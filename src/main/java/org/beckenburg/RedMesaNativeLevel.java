package org.beckenburg;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.beckenburg.LevelDefinition.Dock;
import org.beckenburg.LevelDefinition.Portal;
import org.beckenburg.LevelDefinition.Room;
import org.beckenburg.LevelDefinition.Socket;
import org.beckenburg.LevelDefinition.Vec;

/** Exact Red Mesa review exports for the ordinary mission and network entries. */
final class RedMesaNativeLevel {
    static final String REVIEW_PROPERTY = "beckenburg.review.redMesaNative";
    private static final String ID = "wld-03";
    private static final String DIRECTORY = "/org/beckenburg/sites/" + ID + "/";

    private record Source(LevelDefinition level, NativeInvestigationScene scene) {}

    private RedMesaNativeLevel() {}

    private static final class Packaged {
        private static final Source SOURCE = load();
    }

    static LevelDefinition definition() {
        return Packaged.SOURCE.level();
    }

    static NativeInvestigationScene sceneFor(LevelDefinition level) {
        var source = Packaged.SOURCE;
        if (!source.level().equals(level)) throw new IllegalArgumentException("Red Mesa requires its matching native definition");
        return source.scene();
    }

    private static Source load() {
        try (var layout = resource("runtime-input.tsv"); var schedule = resource("door-schedule.tsv");
             var model = resource("red-mesa-review.glb"); var physics = resource("red-mesa-review.hcs2")) {
            return read(WharfNativeLevel.rows(layout), WharfNativeLevel.rows(schedule), model, physics);
        } catch (IOException bad) {
            throw new IllegalStateException("Cannot load Red Mesa", bad);
        }
    }

    private static InputStream resource(String name) throws IOException {
        var stream = RedMesaNativeLevel.class.getResourceAsStream(DIRECTORY + name);
        if (stream == null) throw new IOException("Missing " + DIRECTORY + name);
        return stream;
    }

    private static Source read(List<String[]> layout, List<String[]> schedule, InputStream model,
                               InputStream collision) throws IOException {
        var hashes = layout.get(0);
        var staging = layout.get(1);
        if (!hashes[0].equals("SOURCE") || !staging[0].equals("DOCK"))
            throw new IOException("Red Mesa packet lacks source hashes or a dock");
        var dock = new Dock(d(staging, 1), d(staging, 2), d(staging, 3), d(staging, 4));
        var spaces = new ArrayList<NativeInvestigationScene.Space>();
        var rooms = new Room[HouseMap.ROOM_COUNT];
        var portals = new ArrayList<Portal>();
        var sockets = new ArrayList<Socket>();
        var treads = new HashSet<String>();
        for (var row : layout) {
            switch (row[0]) {
                case "ROOM", "SPACE" -> {
                    int owner = n(row, 2);
                    boolean outside = Boolean.parseBoolean(row[9]);
                    var space = new NativeInvestigationScene.Space(row[1], owner, d(row, 3), d(row, 4),
                        d(row, 5), d(row, 6), d(row, 7), d(row, 8), outside, true);
                    spaces.add(space);
                    if (row[0].equals("ROOM")) {
                        if (rooms[owner] != null) throw new IOException("Duplicate Red Mesa owner " + owner);
                        rooms[owner] = new Room(owner, row[10], space.x0(), space.z0(), space.x1(), space.z1(),
                            space.floor(), space.height(), outside ? "native-outdoor" : "stone",
                            outside ? "outdoor" : "stone", point(row, 11));
                    }
                }
                case "OPEN" -> portals.add(new Portal(n(row, 1), n(row, 2), n(row, 3), d(row, 4), d(row, 5),
                    d(row, 6), d(row, 7), row[8].equals("z"), row[9], 1));
                case "TREAD" -> treads.add(row[1]);
                case "RITUAL" -> sockets.add(new Socket("red-mesa-" + row[1], "ritual", n(row, 2), d(row, 3),
                    d(row, 4), d(row, 5), 0, row[1]));
                case "COVER" -> sockets.add(new Socket(row[1], "hide", n(row, 2), d(row, 3), d(row, 4),
                    d(row, 5), d(row, 6), "walk-in-cupboard"));
                case "SOURCE", "DOCK", "POINT" -> { }
                default -> throw new IOException("Unknown Red Mesa runtime record " + row[0]);
            }
        }
        var frames = new ArrayList<NativeInvestigationDoor.Spec>();
        for (var row : schedule) {
            var portal = new Portal(n(row, 0), n(row, 4), n(row, 5), d(row, 6), d(row, 7), d(row, 8),
                d(row, 9), row[10].equals("z"), "door", 1);
            frames.add(new NativeInvestigationDoor.Spec(row[1], row[2], row[3], portal, d(row, 11),
                d(row, 12), d(row, 13), n(row, 14), n(row, 15), n(row, 16), row[17]));
            portals.add(portal);
        }
        var scene = NativeInvestigationScene.read(model, hashes[1], collision, hashes[2], treads, spaces, frames);
        var van = new VanStaging(new VanStaging.Placement(dock.x(), dock.y(), dock.z(), dock.yaw()));
        var lip = van.world(0, VanStaging.FRONT);
        rooms[14] = new Room(14, "Expedition van", dock.x() - 1.26, dock.z() - 3, dock.x() + 1.26,
            dock.z() + 2.4, van.floorY(), VanStaging.INTERIOR_HEIGHT, "van", "metal",
            new Vec(dock.x(), van.floorY(), dock.z()));
        portals.add(new Portal(969, 13, 14, lip.x(), van.floorY(), lip.z(), VanStaging.RAMP_HALF_WIDTH * 2,
            true, "trail", 1));
        for (int i = 0; i < rooms.length; i++) {
            var room = rooms[i];
            if (room == null) throw new IOException("Missing Red Mesa owner " + i);
            if (i < 14 && (scene.ownerAt(room.spawn().x(), room.spawn().y(), room.spawn().z()) != i
                || !scene.physics().traversable(room.spawn(), HunterPosture.STANDING_HEIGHT)))
                throw new IOException("Unsupported Red Mesa spawn " + i + " at " + room.spawn());
        }
        var level = new LevelDefinition(ID, scene.revision(), MissionSites.byId("WLD-03").title(),
            List.of(rooms), portals, List.of(), List.of(), List.of(), sockets, List.of(), dock, "canyon");
        return new Source(level, scene);
    }

    private static double d(String[] row, int i) { return Double.parseDouble(row[i]); }
    private static int n(String[] row, int i) { return Integer.parseInt(row[i]); }
    private static Vec point(String[] row, int i) { return new Vec(d(row, i), d(row, i + 1), d(row, i + 2)); }
}
