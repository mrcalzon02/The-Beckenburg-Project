package org.beckenburg;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.beckenburg.LevelDefinition.Dock;
import org.beckenburg.LevelDefinition.Portal;
import org.beckenburg.LevelDefinition.Room;
import org.beckenburg.LevelDefinition.Socket;
import org.beckenburg.LevelDefinition.Vec;

/** Loads the same authored College packet for packaged play and source review. */
final class CollegeNativeLevel {
    private static final String ROOT = "/org/beckenburg/sites/";
    static final String REVIEW_PROPERTY = "beckenburg.review.collegeNative";
    private static final String SCENE_BINDING = "college-scene-hash";

    record Aperture(String id, String a, String b, Portal portal) {}
    record Source(LevelDefinition definition, NativeInvestigationScene scene, List<Aperture> apertures) {}

    private CollegeNativeLevel() {}

    static boolean reviewEnabled() {
        return Boolean.getBoolean(REVIEW_PROPERTY);
    }

    static LevelDefinition definition() {
        return Packaged.SOURCE.definition();
    }

    static NativeInvestigationScene sceneFor(HouseMap.Site site, LevelDefinition definition) {
        if (site != HouseMap.Site.COLLEGE) return null;
        var binding = definition.socket("native_scene", 13);
        if (binding == null || !binding.id().equals(SCENE_BINDING)) return null;
        var packaged = Packaged.SOURCE;
        if (!packaged.definition().equals(definition))
            throw new IllegalArgumentException("College structure requires its matching authored definition");
        return packaged.scene();
    }

    private static final class Packaged {
        private static final Source SOURCE = load();

        private static Source load() {
            try (var manifest = resource("college-runtime.tsv");
                 var model = resource("college-services.glb");
                 var collision = resource("college-services.hcs2")) {
                return read(manifest, model, collision);
            } catch (IOException failure) {
                throw new IllegalStateException("Cannot load the packaged College reconstruction", failure);
            }
        }
    }

    private static InputStream resource(String name) throws IOException {
        var data = CollegeNativeLevel.class.getResourceAsStream(ROOT + name);
        if (data == null) throw new IOException("Missing College resource " + name);
        return data;
    }

    static Source read(InputStream manifest, InputStream model, InputStream collision) throws IOException {
        try (var reader = new BufferedReader(new InputStreamReader(manifest, StandardCharsets.UTF_8))) {
            var rows = new ArrayList<String[]>();
            for (String line; (line = reader.readLine()) != null;) {
                // Review guides may describe trips between physical thresholds. They create no runtime geometry or links.
                if (line.startsWith("ROUTE\t") || line.startsWith("ARRIVAL\t")) continue;
                rows.add(line.split("\t", -1));
            }
            return decode(rows, model, collision);
        } catch (IllegalArgumentException | IndexOutOfBoundsException malformed) {
            throw new IOException("Invalid College runtime metadata", malformed);
        }
    }

    private static Source decode(List<String[]> rows, InputStream model, InputStream collision) throws IOException {
        if (rows.size() < 2 || !rows.get(0)[0].equals("SOURCE") || !rows.get(1)[0].equals("DOCK"))
            throw new IOException("College manifest needs its source hashes and van dock");
        var source = rows.get(0);
        var dockRow = rows.get(1);
        var dock = new Dock(number(dockRow, 1), number(dockRow, 2), number(dockRow, 3), number(dockRow, 4));
        var van = new VanStaging(new VanStaging.Placement(dock.x(), dock.y(), dock.z(), dock.yaw()));

        var spaces = new ArrayList<NativeInvestigationScene.Space>();
        var stairs = new HashSet<String>();
        var byId = new HashMap<String, NativeInvestigationScene.Space>();
        for (var row : rows) {
            switch (row[0]) {
                case "SPACE" -> {
                    var space = new NativeInvestigationScene.Space(row[1], Integer.parseInt(row[2]),
                        number(row, 3), number(row, 4), number(row, 5), number(row, 6), number(row, 7),
                        number(row, 8), row[9].equals("1"), row[10].equals("1"));
                    spaces.add(space);
                    byId.put(space.id(), space);
                }
                case "TREAD" -> stairs.add(row[1]);
                case "SOURCE", "DOCK", "PORTAL", "LOCAL_DOOR", "ANCHOR", "SOCKET", "RITUAL" -> {}
                default -> throw new IOException("Unknown College runtime record " + row[0]);
            }
        }

        var portals = new ArrayList<Portal>();
        var apertures = new ArrayList<Aperture>();
        var frames = new ArrayList<NativeInvestigationDoor.Spec>();
        for (var row : rows)
            if (row[0].equals("PORTAL")) addAperture(row, byId, portals, apertures, frames);
        var sill = van.world(0, VanStaging.FRONT);
        portals.add(new Portal(portals.size(), 13, 14, sill.x(), van.floorY(), sill.z(),
            VanStaging.RAMP_HALF_WIDTH * 2, false, "trail", 1));
        for (var row : rows)
            if (row[0].equals("LOCAL_DOOR")) addAperture(row, byId, portals, apertures, frames);
        var scene = NativeInvestigationScene.read(model, source[1], collision, source[2], stairs, spaces, frames);

        var rooms = new ArrayList<Room>();
        var sockets = new ArrayList<Socket>();
        for (var row : rows) {
            switch (row[0]) {
                case "ANCHOR" -> {
                    var space = byId.get(row[3]);
                    var p = point(row, 4);
                    int owner = Integer.parseInt(row[1]);
                    var hit = scene.physics().support(p.x(), p.z(), p.y(), .08);
                    if (hit == null || space == null || !space.accessible() || space.owner() != owner
                        || scene.ownerAt(p.x(), hit.y(), p.z()) != owner)
                        throw new IOException("Invalid College spawn " + row[3]);
                    rooms.add(new Room(owner, row[2], space.x0(), space.z0(), space.x1(), space.z1(),
                        hit.y(), space.height(), space.outside() ? "native-outdoor" : "native-indoor", "hard",
                        new Vec(p.x(), hit.y(), p.z())));
                }
                case "SOCKET" -> sockets.add(new Socket(row[1], row[2], Integer.parseInt(row[3]),
                    number(row, 4), number(row, 5), number(row, 6), number(row, 7), row[8]));
                case "RITUAL" -> {
                    var p = point(row, 3);
                    int owner = Integer.parseInt(row[2]);
                    var hit = scene.physics().support(p.x(), p.z(), p.y(), .20);
                    if (hit == null || scene.ownerAt(p.x(), hit.y(), p.z()) != owner)
                        throw new IOException("Invalid College ritual position " + row[1]);
                    sockets.add(new Socket("campus-ritual-" + row[1], "ritual", owner, p.x(), hit.y(), p.z(), 0, row[1]));
                }
                default -> {}
            }
        }
        rooms.add(new Room(14, "Expedition van", dock.x() - 4, dock.z() - 1, dock.x() + 4, dock.z() + 1,
            van.floorY(), VanStaging.INTERIOR_HEIGHT, "van", "metal", new Vec(dock.x(), van.floorY(), dock.z())));
        sockets.add(new Socket(SCENE_BINDING, "native_scene", 13, dock.x(), dock.y(), dock.z(), 0, scene.revision()));
        var level = new LevelDefinition("college", "authored-campus-candidate", "Beckenburg College",
            rooms, portals, List.of(), List.of(), List.of(), sockets, List.of(), dock, "college");
        return new Source(level, scene, List.copyOf(apertures));
    }

    private static void addAperture(String[] row, Map<String, NativeInvestigationScene.Space> spaces,
                                    List<Portal> portals, List<Aperture> apertures,
                                    List<NativeInvestigationDoor.Spec> frames) throws IOException {
        var a = spaces.get(row[2]);
        var b = spaces.get(row[3]);
        var p = point(row, 4);
        boolean fitted = row[9].equals("door");
        if (a == null || b == null || !a.accessible() || !b.accessible() || (!fitted && a.owner() == b.owner()))
            throw new IOException("Invalid College aperture endpoints " + row[1]);
        var portal = new Portal(portals.size(), a.owner(), b.owner(), p.x(), p.y(), p.z(),
            number(row, 7), row[8].equals("z"), row[9], 1);
        portals.add(portal);
        apertures.add(new Aperture(row[1], a.id(), b.id(), portal));
        if (fitted) frames.add(new NativeInvestigationDoor.Spec(row[1], a.id(), b.id(), portal, number(row, 10),
            number(row, 11), number(row, 12), Integer.parseInt(row[13]), Integer.parseInt(row[14]),
            Integer.parseInt(row[15]), row[16]));
    }

    private static double number(String[] row, int index) {
        return Double.parseDouble(row[index]);
    }

    private static Vec point(String[] row, int start) {
        return new Vec(number(row, start), number(row, start + 1), number(row, start + 2));
    }
}
