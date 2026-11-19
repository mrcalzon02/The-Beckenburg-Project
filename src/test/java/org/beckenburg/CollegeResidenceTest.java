package org.beckenburg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import org.beckenburg.LevelDefinition.Vec;

/** Real native bedroom partitions and their supported everyday use at the college. */
public final class CollegeResidenceTest {
    private static final double FLOOR = 3.6;
    private static final double[] HEIGHTS = {HunterPosture.STANDING_HEIGHT, HunterPosture.CROUCH_HEIGHT};

    private record Grid(int x, int z) {
        static Grid rounded(Vec at) {
            return new Grid((int)Math.round(at.x() * 5), (int)Math.round(at.z() * 5));
        }
        Vec at() {
            return new Vec(x / 5.0, FLOOR, z / 5.0);
        }
    }

    private static void require(boolean valid, String detail) {
        if (!valid) throw new AssertionError(detail);
    }

    private static Vec walk(HouseMap map, Vec from, Vec end, double height) {
        int steps = Math.max(1, (int)Math.ceil(Math.hypot(end.x()-from.x(), end.z()-from.z()) / .12));
        Vec feet = from;
        for (int i = 1; i <= steps && feet != null; i++) {
            double t = i / (double)steps;
            feet = map.resolveMove(feet, from.x()+(end.x()-from.x())*t, from.z()+(end.z()-from.z())*t, height);
        }
        return feet;
    }

    private static HashSet<Grid> walkableRoom(HouseMap map, NativeInvestigationScene.Space room,
                                              Vec start, double height) {
        var origin = Grid.rounded(start);
        var reached = new HashSet<Grid>();
        var frontier = new ArrayDeque<Grid>();
        require(map.traversable(origin.at().x(), FLOOR, origin.at().z(), height), "No receiving floor " + room.id());
        frontier.add(origin);
        reached.add(origin);
        while (!frontier.isEmpty()) {
            var here = frontier.removeFirst();
            for (var next : List.of(new Grid(here.x()-1, here.z()), new Grid(here.x()+1, here.z()),
                                    new Grid(here.x(), here.z()-1), new Grid(here.x(), here.z()+1))) {
                var end = next.at();
                if (!room.contains(end.x(), FLOOR, end.z()) || reached.contains(next)) continue;
                var landed = map.resolveMove(here.at(), end.x(), end.z(), height);
                if (landed == null || Math.abs(landed.y()-FLOOR) > .025) continue;
                reached.add(next);
                frontier.addLast(next);
            }
        }
        return reached;
    }

    private static void sealedRoom(HouseMap map, NativeInvestigationScene.Space room, double boundary, double center,
                                   int inward) {
        require(!room.accessible(), room.id() + " unexpectedly became searchable");
        double hall = boundary - inward*.85;
        double inside = boundary + inward*.85;
        for (double lane : new double[]{-.59, -.35, 0, .35, .59}) {
            double z = center + lane;
            var start = new Vec(hall, FLOOR, z);
            var end = new Vec(inside, FLOOR, z);
            require(map.roomAt(hall, FLOOR, z) == 9 && map.roomAt(inside, FLOOR, z) == -1,
                "Incorrect search ownership at fixed leaf " + room.id());
            for (double posture : HEIGHTS)
                require(map.traversable(hall, FLOOR, z, posture) && walk(map, start, end, posture) == null,
                    "Fixed bedroom leaf admitted a hunter " + room.id());
            for (double y : new double[]{.21, .95, 1.62, 2.16}) {
                require(!map.clearLineOfSight(hall, FLOOR+y, z, inside, FLOOR+y, z),
                    "Visible seam in fixed bedroom leaf " + room.id());
                require(!map.clearObjectPath(hall, FLOOR+y, z, inside, FLOOR+y, z, .035, .035),
                    "Item escaped through fixed bedroom leaf " + room.id());
            }
        }
    }

    private static void furniture(HouseMap map, NativeInvestigationScene.Space room) {
        boolean west = room.id().startsWith("res-west");
        int inward = west ? -1 : 1;
        double boundary = west ? room.x1() : room.x0();
        double z = room.z0();
        double doorway = z + 3;
        var start = new Vec(boundary + inward*1.25, FLOOR, doorway);
        if (room.accessible()) {
            var hall = new Vec(boundary - inward*1.20, FLOOR, doorway);
            for (double height : HEIGHTS)
                require(walk(map, hall, start, height) != null, "Entrance misses the room floor " + room.id());
        } else sealedRoom(map, room, boundary, doorway, inward);

        double bedX = west ? 45.75 : 58.25;
        double studyX = west ? 48.10 : 55.90;
        double storageX = west ? 43.55 : 60.45;
        double wardA = switch (room.id()) {
            case "res-west-1" -> 4.14;
            case "res-east-3" -> 2.16;
            default -> 1.04;
        };
        var stances = List.of(new Vec(bedX, FLOOR, z+2.98), new Vec(bedX, FLOOR, z+5.03),
            new Vec(studyX, FLOOR, z+2.31), new Vec(studyX, FLOOR, z+5.69),
            new Vec(storageX, FLOOR, z+wardA), new Vec(storageX, FLOOR, z+6.68));
        for (double height : HEIGHTS) {
            var reachable = walkableRoom(map, room, start, height);
            for (var stance : stances) {
                var grid = Grid.rounded(stance);
                require(reachable.contains(grid) && walk(map, grid.at(), stance, height) != null,
                    "Furniture has no usable approach " + room.id() + " " + stance + " height=" + height);
            }
        }
        for (double bedZ : new double[]{z+1.52, z+6.45}) {
            require(walk(map, new Vec(bedX,FLOOR,bedZ-1.22), new Vec(bedX,FLOOR,bedZ+1.22),
                HunterPosture.STANDING_HEIGHT) == null, "Bed failed to stop a walker " + room.id());
            require(!map.clearObjectPath(bedX,FLOOR+.4,bedZ-1.22,bedX,FLOOR+.4,bedZ+1.22,.035,.035),
                "Bed has no physical mattress or frame " + room.id());
        }
        System.out.println("FIT " + room.id() + " " + (room.accessible() ? "working entrance" : "fixed sealed entrance")
            + ", six standing and crouched use approaches, two physical beds");
    }

    private static void capture(HouseMap map, Path output, boolean gpu) throws Exception {
        Files.createDirectories(output);
        java.lang.reflect.Method stop = null;
        if (gpu) {
            var launcher = Class.forName("org.beckenburg.gpu.GpuDesktop");
            var start = launcher.getDeclaredMethod("startGpu");
            start.setAccessible(true);
            stop = launcher.getDeclaredMethod("stopReviewGpu");
            stop.setAccessible(true);
            start.invoke(null);
        }
        try {
            for (var door : map.nativeInvestigation().doors())
                if (door.spec().frameId().equals("res-west-1-door") && map.doorOpen(door.spec().portal().id()))
                    map.toggleDoor(door.spec().portal().id(), System.nanoTime()-2_000_000_000L);
            CollegeContainmentTest.frame(map, output, "west-leaf-closed", new Vec(51.8,FLOOR,37.1),49,36.98,-.04);
            for (var door : map.nativeInvestigation().doors())
                if (door.spec().frameId().equals("res-west-1-door") && !map.doorOpen(door.spec().portal().id()))
                    map.toggleDoor(door.spec().portal().id(), System.nanoTime()-2_000_000_000L);
            CollegeContainmentTest.frame(map, output, "west-leaf-open", new Vec(51.8,FLOOR,37.1),49,36.98,-.04);
            CollegeContainmentTest.frame(map, output, "west-twin", new Vec(48.68,FLOOR,37.7),44,38.7,-.11);
            CollegeContainmentTest.frame(map, output, "east-twin", new Vec(55.23,FLOOR,33.85),60,34.7,-.10);
            CollegeContainmentTest.frame(map, output, "west-study", new Vec(46.37,FLOOR,37.62),48.15,35.0,-.16);
            CollegeContainmentTest.frame(map, output, "fixed-context-leaves", new Vec(52.1,FLOOR,54.8),50.12,51.8,-.04);
        } finally {
            if (stop != null) stop.invoke(null);
        }
    }

    public static void main(String[] args) throws Exception {
        var folder = Path.of(args[0]);
        var manifest = folder.resolve("verification/runtime.tsv");
        var model = folder.resolve("college/college-residence.glb");
        var collision = folder.resolve("college/college-residence.hcs2");
        var campus = CollegeNativeRuntimeReview.load(manifest, model, collision);
        var map = campus.map();
        CollegeNativeDoorsTest.audit(campus, 20);
        for (var room : map.nativeInvestigation().spaces())
            if (room.id().matches("res-(west|east)-[1-4]")) furniture(map, room);
        System.out.println("PASS eight twin rooms, 16 physical beds, 48 use approaches in both postures and six sealed context rooms");
        if (List.of(args).contains("--geometry-only")) return;
        var replica = CollegeNativeRuntimeReview.load(manifest, model, collision);
        CollegeNativeDoorsTest.peers(map, replica.map());
        CollegeNativeElectricalTest.verifyControlsAndPower(map);
        if (args.length > 1) capture(map,Path.of(args[1]),List.of(args).contains("--gpu"));
        System.out.println("RESIDENCE LEVEL " + map.level().revision());
        System.out.println("RESIDENCE SCENE " + map.nativeInvestigation().revision());
    }
}
