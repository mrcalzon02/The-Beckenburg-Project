package org.beckenburg;

import dev.mrcalzon.water.WaterDocument;
import org.beckenburg.LevelDefinition.Dock;
import org.beckenburg.LevelDefinition.Portal;
import org.beckenburg.LevelDefinition.Room;
import org.beckenburg.LevelDefinition.Socket;
import org.beckenburg.LevelDefinition.Vec;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Packaged authority for the Blackwater review site. */
final class BlackwaterNativeLevel {
    static final String TITLE = "Blackwater Hydroelectric Dam";
    static final String REVIEW_PROPERTY = "beckenburg.review.blackwaterNative";
    private static final String ID = "wld-09";
    private static final String DIRECTORY = "/org/beckenburg/sites/" + ID + "/";

    record Source(LevelDefinition level, NativeInvestigationScene scene) {}

    private BlackwaterNativeLevel() {}

    private static final class Loaded {
        private static final Source SOURCE = load();
    }

    static Source packaged() {
        return Loaded.SOURCE;
    }

    static NativeInvestigationScene sceneFor(LevelDefinition level) {
        var source = packaged();
        if (!source.level().equals(level)) {
            throw new IllegalArgumentException("Blackwater requires the exact authored revision");
        }
        return source.scene();
    }

    private static Source load() {
        try (var manifest = resource("source-hashes.tsv");
             var model = resource("dam-with-ventilation.glb");
             var physics = resource("dam-with-ventilation.hcs2")) {
            Map<String, String> hashes = new LinkedHashMap<>();
            for (var row : WharfNativeLevel.rows(manifest)) {
                check(row.length == 2 && row[1].matches("[0-9a-f]{64}"), "Invalid Blackwater resource identity");
                check(hashes.putIfAbsent(row[0], row[1]) == null, "Duplicate Blackwater resource identity " + row[0]);
            }
            check(hashes.keySet().equals(java.util.Set.of("dam-with-ventilation.glb", "dam-with-ventilation.hcs2",
                "runtime-input.tsv", "door-schedule.tsv", "electrical-schedule.tsv", "blackwater.water")),
                "Incomplete Blackwater resource identity");
            return read(WharfNativeLevel.rows(verified("runtime-input.tsv", hashes)),
                WharfNativeLevel.rows(verified("door-schedule.tsv", hashes)),
                WharfNativeLevel.rows(verified("electrical-schedule.tsv", hashes)),
                WaterDocument.read(verified("blackwater.water", hashes)),
                model, hashes.get("dam-with-ventilation.glb"), physics, hashes.get("dam-with-ventilation.hcs2"));
        } catch (IOException error) {
            throw new IllegalStateException("Cannot load " + ID, error);
        }
    }

    private static InputStream resource(String name) throws IOException {
        var stream = BlackwaterNativeLevel.class.getResourceAsStream(DIRECTORY + name);
        if (stream == null) throw new IOException("Missing " + DIRECTORY + name);
        return stream;
    }

    private static InputStream verified(String name, Map<String, String> hashes) throws IOException {
        try (var stream = resource(name)) {
            byte[] bytes = stream.readNBytes(2_000_001);
            check(bytes.length <= 2_000_000, "Oversized Blackwater schedule " + name);
            var actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
            check(actual.equals(hashes.get(name)), "Blackwater resource hash mismatch " + name);
            return new ByteArrayInputStream(bytes);
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

    private static Source read(List<String[]> layout, List<String[]> schedule, List<String[]> electrical,
                               WaterDocument water, InputStream model, String modelHash,
                               InputStream collision, String collisionHash) throws IOException {
        var spaces = new ArrayList<NativeInvestigationScene.Space>();
        var rooms = new Room[HouseMap.ROOM_COUNT];
        var treads = new HashSet<String>();
        var doors = new ArrayList<NativeInvestigationDoor.Spec>();
        var portals = new ArrayList<Portal>();
        var sockets = new ArrayList<Socket>();
        for (var row : layout) {
            switch (row[0]) {
                case "ROOM", "SPACE" -> {
                    var space = new NativeInvestigationScene.Space(row[1], integer(row, 2), number(row, 3), number(row, 4),
                        number(row, 5), number(row, 6), number(row, 7), number(row, 8), Boolean.parseBoolean(row[9]),
                        !row[1].equals("dam-closed-channel"));
                    spaces.add(space);
                    if (row[0].equals("ROOM")) {
                        check(rooms[space.owner()] == null, "Duplicate Blackwater room owner " + space.owner());
                        rooms[space.owner()] = new Room(space.owner(), row[10], space.x0(), space.z0(), space.x1(),
                            space.z1(), space.floor(), space.height(), space.outside() ? "native-outdoor" : "metal",
                            space.outside() ? "outdoor" : "concrete", point(row, 11));
                    }
                }
                case "STEP" -> treads.add(row[1]);
                case "RITUAL" -> sockets.add(new Socket("dam-" + row[1], "ritual", integer(row, 2),
                    number(row, 3), number(row, 4), number(row, 5), 0, row[1]));
                case "COVER" -> sockets.add(new Socket(row[1], "hide", integer(row, 2),
                    number(row, 3), number(row, 4), number(row, 5), number(row, 6), "walk-in-cupboard"));
                case "OPEN" -> portals.add(new Portal(integer(row, 1), integer(row, 2), integer(row, 3), number(row, 4),
                    number(row, 5), number(row, 6), number(row, 7), row[8].equals("z"), row[9], 1));
                case "POINT" -> { /* Survey paths are review input. Geometry governs movement. */ }
                default -> throw new IOException("Unknown Blackwater row " + row[0]);
            }
        }
        for (var row : electrical) {
            check(row.length == 8, "Invalid Blackwater electrical fitting");
            sockets.add(new Socket(row[0], row[1], integer(row, 2), number(row, 3),
                number(row, 4), number(row, 5), number(row, 6), row[7]));
        }
        for (var row : schedule) {
            var portal = new Portal(integer(row, 0), integer(row, 4), integer(row, 5), number(row, 6), number(row, 7),
                number(row, 8), number(row, 9), row[10].equals("z"), "door", 1);
            portals.add(portal);
            doors.add(new NativeInvestigationDoor.Spec(row[1], row[2], row[3], portal, number(row, 11), number(row, 12),
                number(row, 13), integer(row, 14), integer(row, 15), integer(row, 16), row[17]));
        }
        var dock = new Dock(36, 0, 82, 0);
        var van = new VanStaging(new VanStaging.Placement(dock.x(), dock.y(), dock.z(), dock.yaw()));
        rooms[14] = new Room(14, "Expedition van", 34.74, 79, 37.26, 84.4, van.floorY(), VanStaging.INTERIOR_HEIGHT,
            "van", "metal", new Vec(36, van.floorY(), 82));
        for (int i = 0; i < rooms.length; i++) check(rooms[i] != null, "Missing Blackwater room owner " + i);
        var scene = NativeInvestigationScene.read(model, modelHash, collision, collisionHash, treads, spaces, doors);
        var level = new LevelDefinition(ID, scene.revision(), TITLE, List.of(rooms), portals, List.of(),
            List.of(), List.of(), sockets, List.of(), dock, "highland", water);
        check(scene.doors().size() == 14 && HidingBudget.candidates(level).size() == 3
            && level.socket("generator", 10) != null && level.socket("breaker", 10) != null
            && water.placements().size() == 2, "Incomplete Blackwater review source");
        return new Source(level, scene);
    }

    private static int integer(String[] row, int field) { return Integer.parseInt(row[field]); }
    private static double number(String[] row, int field) { return Double.parseDouble(row[field]); }
    private static Vec point(String[] row, int field) {
        return new Vec(number(row, field), number(row, field + 1), number(row, field + 2));
    }
    private static void check(boolean okay, String reason) throws IOException {
        if (!okay) throw new IOException(reason);
    }
}
