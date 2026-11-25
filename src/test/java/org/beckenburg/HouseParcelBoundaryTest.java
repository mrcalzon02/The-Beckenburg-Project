package org.beckenburg;

import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;
import org.beckenburg.LevelDefinition.Vec;

/** Physical continuity and render parity for the placed, uneven House perimeter. */
public final class HouseParcelBoundaryTest {
    private HouseParcelBoundaryTest() { }

    private static void check(boolean yes, String message) {
        if (!yes) throw new AssertionError(message);
    }

    private static Vec rendererPoint(Object face, String vertex) throws Exception {
        var point = LevelSceneSource.read(face, vertex);
        return new Vec(((Number)LevelSceneSource.read(point, "x")).doubleValue(),
            ((Number)LevelSceneSource.read(point, "y")).doubleValue(),
            ((Number)LevelSceneSource.read(point, "z")).doubleValue());
    }

    private static void checkRenderer(HouseMap map, HouseParcelBoundary.Geometry geometry) throws Exception {
        var method = SceneRenderer.class.getDeclaredMethod("enclosureScenery", LevelDefinition.class);
        method.setAccessible(true);
        var scene = (List<?>)method.invoke(new SceneRenderer(), map.level());
        int groundFaces = map.worldSurface().triangleCount();
        var mesh = geometry.model().triangles();
        var canonical = GlbProps.model(HouseParcelBoundary.MODEL).triangles();
        for (int i = 0; i < mesh.size(); i++) {
            var rendered = scene.get(groundFaces + i);
            var collision = geometry.physics().triangle(i);
            check(rendererPoint(rendered, "a").equals(collision.a())
                && rendererPoint(rendered, "b").equals(collision.b())
                && rendererPoint(rendered, "c").equals(collision.c()),
                "Rendered House fence differs from actual physical triangle " + i);
            var source = canonical.get(i % canonical.size());
            var triangle = mesh.get(i);
            check(source.a().u() == triangle.a().u() && source.a().v() == triangle.a().v()
                && source.b().u() == triangle.b().u() && source.b().v() == triangle.b().v()
                && source.c().u() == triangle.c().u() && source.c().v() == triangle.c().v()
                && source.material() == ((Number)LevelSceneSource.read(rendered, "material")).intValue(),
                "Canonical fence surfacing changed at triangle " + i);
        }
    }

    static void verify(HouseMap map) throws Exception {
        var geometry = HouseParcelBoundary.geometry(map.level());
        check(geometry != null, "Rebuilt parcel is missing shared fence geometry");
        var panels = map.level().sockets().stream().filter(HouseParcelBoundary::primaryPanel).toList();
        var edges = HouseRebuildLandscape.EDGE;
        int expected = 0;
        int moves = 0;
        int joins = 0;
        int physicalPoses = 0;
        int cutThrough = 0;
        int boardRays = 0;
        int objectGaps = 0;
        int blockedObjects = 0;
        double tallestBaseGap = Double.NEGATIVE_INFINITY;
        double shortestPanel = Double.POSITIVE_INFINITY;
        var eyeLevels = new double[] {.30, HunterPosture.CROUCH_EYE, HunterPosture.STANDING_EYE, 1.83, 1.92};
        var postures = new double[] {HunterPosture.STANDING_HEIGHT, HunterPosture.CROUCH_HEIGHT};
        for (int i = 0; i < edges.length; i++) {
            double[] from = edges[i], to = edges[(i + 1) % edges.length];
            double length = Math.hypot(to[0] - from[0], to[1] - from[1]);
            double dx = (to[0] - from[0]) / length, dz = (to[1] - from[1]) / length;
            int count = (int)Math.ceil(length / 2);
            double panelWidth = length / count;
            for (int j = 0; j < count; j++) {
                String id = "parcel-" + i + "-" + j;
                var panel = panels.stream().filter(p -> p.id().equals(id)).findFirst().orElseThrow();
                double middle = (j + .5) * panelWidth;
                check(Math.abs(panel.x() - from[0] - dx * middle) < 1e-7
                    && Math.abs(panel.z() - from[1] - dz * middle) < 1e-7
                    && Math.abs(HouseParcelBoundary.panelLength(panel) - panelWidth) < 1e-7
                    && Math.abs(Math.cos(panel.yaw()) - dx) < 1e-7
                    && Math.abs(-Math.sin(panel.yaw()) - dz) < 1e-7, "Displaced fence panel " + id);
                check(map.level().colliders().stream().noneMatch(c -> c.x() == panel.x() && c.y() == panel.y()
                    && c.z() == panel.z() && c.yaw() == -panel.yaw()
                    && Math.abs(c.halfX() - panelWidth / 2 - .012) < 1e-7), "Opaque box over the rendered slats " + id);
                expected++;
                for (int k = 0; k <= 32; k++) {
                    double offset = (k / 32.0 - .5) * panelWidth;
                    double x = panel.x() + offset * dx, z = panel.z() + offset * dz;
                    var ground = map.worldSurface().support(x, z, 0, 1);
                    check(ground != null, "Fence segment has no saved terrain " + id);
                    tallestBaseGap = Math.max(tallestBaseGap, panel.y() - ground.y());
                    shortestPanel = Math.min(shortestPanel, panel.y() + 1.96 - ground.y());
                    check(panel.y() <= ground.y() + .005, "Visible fence floats " + id);
                    for (double height : postures) {
                        for (double rise : new double[] {0, .32, .70, 1.08}) {
                            check(geometry.physics().bodyBlocked(new Vec(x, ground.y() + rise, z), height, .22),
                                "Physical body gap at " + id + " sample " + k + " raise " + rise);
                            physicalPoses++;
                        }
                    }
                    if (k == 0) joins++;
                    check(!geometry.physics().bodyBlocked(new Vec(x, panel.y() + 2.04, z),
                        HunterPosture.CROUCH_HEIGHT, .22), "Invisible wall above the timber tops " + id);
                    for (double height : eyeLevels) {
                        double y = ground.y() + height;
                        var a = new Vec(x + .4*dz, y, z - .4*dx);
                        var b = new Vec(x - .4*dz, y, z + .4*dx);
                        boolean meshClear = geometry.physics().clearSight(a, b);
                        boolean runtimeClear = map.clearLineOfSight(a.x(), y, a.z(), b.x(), y, b.z());
                        if (meshClear && runtimeClear) {
                            cutThrough++;
                            if (height < 1.7 && map.clearObjectPath(a.x(), y, a.z(), b.x(), y, b.z(), .004, .004)
                                && !map.clearObjectPath(a.x(), y, a.z(), b.x(), y, b.z(), .07, .07)) objectGaps++;
                        } else if (!meshClear) {
                            check(!runtimeClear, "Game sight passes through visible timber " + id);
                            boardRays++;
                            if (height == .30) {
                                check(!map.clearObjectPath(a.x(), y, a.z(), b.x(), y, b.z(), .05, .05),
                                    "Small object passes through timber " + id);
                                blockedObjects++;
                            }
                        }
                    }
                    if (k % 8 == 0 && k > 0 && k < 32) {
                        double sign = HouseRebuildLandscape.inside(x + .70*dz, z - .70*dx) ? 1 : -1;
                        for (double height : postures) {
                            Vec start = null;
                            for (double offsetIn : new double[] {.31, .38, .50, .70, .90, 1.15, 1.40}) {
                                double sx = x + sign*offsetIn*dz, sz = z - sign*offsetIn*dx;
                                var inner = map.worldSurface().support(sx, sz, 0, 1);
                                if (inner == null || !HouseRebuildLandscape.inside(sx, sz)
                                    || !map.traversable(sx, inner.y(), sz, height)) continue;
                                start = new Vec(sx, inner.y(), sz);
                                break;
                            }
                            check(start != null, "Unverified interior approach to " + id + " sample " + k);
                            check(map.resolveMove(start, x - sign*.52*dz, z + sign*.52*dx, height) == null,
                                "Mover crosses the actual segmented fence " + id);
                            moves++;
                        }
                    }
                }
            }
        }
        check(panels.size() == expected && joins == expected, "Incomplete or duplicate perimeter");
        check(physicalPoses > 0 && boardRays > 0 && cutThrough > 0 && objectGaps > 0 && blockedObjects > 0,
            "Missing inclusive timber, sight and object controls");
        checkRenderer(map, geometry);
        System.out.printf(Locale.ROOT,
            "PASS House fence: %d panels, %d joined seams, %d real triangles, %d supported mover sweeps, "
            + "%d capsule contacts, %d visible gaps, %d timber rays, %d filtered object gaps, %d object stops, "
            + "max base gap %.4f m, minimum post height %.4f m%n",
            panels.size(), joins, geometry.physics().triangleCount(), moves, physicalPoses, cutThrough, boardRays,
            objectGaps, blockedObjects, tallestBaseGap, shortestPanel);
    }

    private static int walkGuide(HouseMap map, Vec start, List<Vec> points, double height) {
        check(map.traversable(start.x(), start.y(), start.z(), height), "Unsupported door guide start " + start);
        Vec feet = start;
        int poses = 0;
        for (Vec target : points) {
            Vec origin = feet;
            int steps = (int)Math.ceil(Math.hypot(target.x() - origin.x(), target.z() - origin.z()) / .08);
            for (int step = 1; step <= steps; step++) {
                double fraction = step / (double)steps;
                Vec next = map.resolveMove(feet, origin.x() + fraction * (target.x() - origin.x()),
                    origin.z() + fraction * (target.z() - origin.z()), height);
                if (next == null) return poses;
                check(map.traversable(next.x(), next.y(), next.z(), height), "Door guide landed on invalid support");
                feet = next;
                poses++;
            }
        }
        return poses;
    }

    private static void verifyDoorGuides(HouseMap map) {
        var toe = map.van().world(0, VanStaging.FRONT - VanStaging.RAMP_LENGTH);
        var road = new Vec(toe.x(), map.groundHeight(toe.x(), toe.z(), 13), toe.z());
        var turning = new Vec(17, 0, 32);
        var verge = new Vec(17, 0, 28);
        var garden = new Vec(19.8, 0, 24);
        var vestibule = new Vec(19.8, 0, 19.8);
        var rearGarden = new Vec(16.5, map.groundHeight(16.5, 7, 12), 7);
        var kitchen = new Vec(16.5, 0, 10.7);
        var arrival = List.of(turning, verge, garden, vestibule);
        var drive = List.of(garden, verge, turning, road);
        double[] heights = {HunterPosture.STANDING_HEIGHT, HunterPosture.CROUCH_HEIGHT};
        for (double height : heights) {
            int front = walkGuide(map, road, arrival, height);
            int rear = walkGuide(map, rearGarden, List.of(kitchen), height);
            check(front >= 166 && front < 176, "Closed front door should stop the supported porch approach");
            check(rear >= 31 && rear < 39, "Closed rear door should stop the supported garden approach");
        }
        long settled = System.nanoTime() - 2_000_000_000L;
        map.toggleDoor(14, settled);
        map.toggleDoor(15, settled);
        for (double height : heights) {
            check(walkGuide(map, road, arrival, height) == 183 && walkGuide(map, vestibule, drive, height) == 183,
                "The supported path must reach the front vestibule and return to the van ramp");
            check(walkGuide(map, rearGarden, List.of(kitchen), height) == 47
                && walkGuide(map, kitchen, List.of(rearGarden), height) == 47,
                "The supported path must reach the kitchen and return to the rear garden");
        }
        map.toggleDoor(14, settled);
        map.toggleDoor(15, settled);
        System.out.println("PASS actual front and rear door guides in both directions and postures: 920 moves, four closed-door controls");
    }

    private static void capture(HouseMap map, Path output, String name, double x, double z, double targetX, double targetZ)
        throws Exception {
        double floor = map.groundHeight(x, z, map.roomAt(x, z));
        check(map.traversable(x, floor, z), "Standing camera lacks site support at " + name);
        var renderer = new SceneRenderer();
        renderer.setRenderSize(1120, 700);
        renderer.lamp = true;
        renderer.setLampHardware(EquipmentTiers.Tier.III, false);
        renderer.weatherModule().setTimeOfDay(.52);
        renderer.weatherModule().pauseClock(true);
        renderer.x = x;
        renderer.z = z;
        renderer.eyeHeight = floor + HunterPosture.STANDING_EYE;
        renderer.yaw = Math.atan2(targetX - x, targetZ - z);
        renderer.pitch = -.10;
        renderer.drawNetwork(map, List.of(), -1, 0);
        check(ImageIO.write(renderer.image(), "png", output.resolve(name + ".png").toFile()), "PNG capture failed");
    }

    public static void main(String[] args) throws Exception {
        var map = new HouseMap(HouseMap.Site.HOUSE, HouseSourceVersions.source(UserLevelLibrary.HOUSE_REBUILD));
        verify(map);
        verifyDoorGuides(map);
        if (args.length > 0) {
            Path output = Path.of(args[0]);
            Files.createDirectories(output);
            capture(map, output, "house-rear-service-return", 9.4, 5.8, 8.2, 4.7);
            capture(map, output, "house-west-bend", 8.5, 13.2, 6, 11);
            capture(map, output, "house-carriageway-south-return", 17.7, 36.5, 20.0, 39.4);
            capture(map, output, "house-van-and-south-boundary", 17.7, 35.8, 22.0, 34.3);
        }
    }
}
