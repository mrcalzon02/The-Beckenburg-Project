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

/** Loads the shared native room, door and electrical schedule format from a game archive. */
final class PackagedInvestigationLevel {
    record Profile(String id, String title, String stem, Dock dock, String ritualPrefix,
                   String material, String acoustics, String sealedSpace, String waterResource) {}
    record Source(LevelDefinition level, NativeInvestigationScene scene) {}

    private PackagedInvestigationLevel() {}

    static Source load(Profile profile) {
        String stem = profile.stem();
        try (var manifest = resource(profile, "source-hashes.tsv");
             var model = resource(profile, stem + ".glb");
             var physics = resource(profile, stem + ".hcs2")) {
            Map<String, String> hashes = new LinkedHashMap<>();
            for (var row : WharfNativeLevel.rows(manifest)) {
                check(row.length == 2 && row[1].matches("[0-9a-f]{64}"), "Invalid native resource identity");
                check(hashes.putIfAbsent(row[0], row[1]) == null, "Duplicate native resource identity " + row[0]);
            }
            var expected = new HashSet<>(List.of(stem + ".glb", stem + ".hcs2",
                "runtime-input.tsv", "door-schedule.tsv", "electrical-schedule.tsv"));
            if (profile.waterResource() != null) expected.add(profile.waterResource());
            check(hashes.keySet().equals(expected), "Incomplete native resource identity for " + profile.id());
            var water = WorldWater.defaults(profile.id());
            if (profile.waterResource() != null) {
                try (var stream = verified(profile, profile.waterResource(), hashes)) {
                    water = WaterDocument.read(stream);
                }
            }
            return read(profile, WharfNativeLevel.rows(verified(profile, "runtime-input.tsv", hashes)),
                WharfNativeLevel.rows(verified(profile, "door-schedule.tsv", hashes)),
                WharfNativeLevel.rows(verified(profile, "electrical-schedule.tsv", hashes)),
                water, model, hashes.get(stem + ".glb"), physics, hashes.get(stem + ".hcs2"));
        } catch (IOException error) {
            throw new IllegalStateException("Cannot load " + profile.id(), error);
        }
    }

    private static InputStream resource(Profile profile, String name) throws IOException {
        String entry = "/org/beckenburg/sites/" + profile.id() + "/" + name;
        var stream = PackagedInvestigationLevel.class.getResourceAsStream(entry);
        if (stream == null) throw new IOException("Missing " + entry);
        return stream;
    }

    private static InputStream verified(Profile profile, String name, Map<String, String> hashes) throws IOException {
        try (var stream = resource(profile, name)) {
            byte[] bytes = stream.readNBytes(2_000_001);
            check(bytes.length <= 2_000_000, "Oversized native schedule " + name);
            var actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
            check(actual.equals(hashes.get(name)), "Native resource hash mismatch " + name);
            return new ByteArrayInputStream(bytes);
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

    private static Source read(Profile profile, List<String[]> layout, List<String[]> schedule, List<String[]> electrical,
                               WaterDocument water, InputStream model, String modelHash,
                               InputStream collision, String collisionHash) throws IOException {
        var spaces = new ArrayList<NativeInvestigationScene.Space>();
        var rooms = new Room[HouseMap.ROOM_COUNT];
        var treads = new HashSet<String>();
        var grates = new HashSet<String>();
        var doors = new ArrayList<NativeInvestigationDoor.Spec>();
        var portals = new ArrayList<Portal>();
        var sockets = new ArrayList<Socket>();
        var looseSupports = new LinkedHashMap<Socket,String>();
        var looseIds = new HashSet<String>();
        for (var row : layout) {
            switch (row[0]) {
                case "ROOM", "SPACE" -> {
                    var space = new NativeInvestigationScene.Space(row[1], integer(row, 2), number(row, 3), number(row, 4),
                        number(row, 5), number(row, 6), number(row, 7), number(row, 8), Boolean.parseBoolean(row[9]),
                        !row[1].equals(profile.sealedSpace()));
                    spaces.add(space);
                    if (row[0].equals("ROOM")) {
                        check(rooms[space.owner()] == null, "Duplicate native room owner " + space.owner());
                        rooms[space.owner()] = new Room(space.owner(), row[10], space.x0(), space.z0(), space.x1(),
                            space.z1(), space.floor(), space.height(), space.outside() ? "native-outdoor" : profile.material(),
                            space.outside() ? "outdoor" : profile.acoustics(), point(row, 11));
                    }
                }
                case "STEP" -> treads.add(row[1]);
                case "GRATE" -> check(row.length == 2 && grates.add(row[1]), "Invalid native walking grate");
                case "RITUAL" -> sockets.add(new Socket(profile.ritualPrefix() + row[1], "ritual", integer(row, 2),
                    number(row, 3), number(row, 4), number(row, 5), 0, row[1]));
                case "COVER" -> sockets.add(new Socket(row[1], "hide", integer(row, 2),
                    number(row, 3), number(row, 4), number(row, 5), number(row, 6), "walk-in-cupboard"));
                case "STORY" -> {
                    check(row.length == 12, "Invalid native story placement");
                    SiteStories.document(profile.id(), row[2]);
                    sockets.add(new Socket(row[1], "native_story", integer(row, 3),
                        number(row, 5), number(row, 6), number(row, 7), number(row, 8), row[2]));
                }
                case "LOOSE" -> {
                    check(row.length==9 && looseIds.add(row[1]) && looseIds.size()<=120,"Invalid native belonging");
                    int owner=integer(row,2);
                    var kind=LooseClutter.Kind.valueOf(row[3]);
                    var at=point(row,5);
                    double yaw=number(row,8);
                    check(owner>=0 && owner<HouseMap.ROOM_COUNT && Double.isFinite(yaw),"Invalid native belonging placement");
                    SiteStories.nativeBelonging(profile.id(),row[1]);
                    var socket=new Socket(row[1],"native_loose",owner,at.x(),at.y(),at.z(),yaw,kind.name());
                    sockets.add(socket);
                    looseSupports.put(socket,row[4]);
                }
                case "OPEN" -> portals.add(new Portal(integer(row, 1), integer(row, 2), integer(row, 3), number(row, 4),
                    number(row, 5), number(row, 6), number(row, 7), row[8].equals("z"), row[9], 1));
                case "POINT" -> { /* Survey paths are review input. Geometry governs movement. */ }
                default -> throw new IOException("Unknown native layout row " + row[0]);
            }
        }
        for (var row : electrical) {
            check(row.length == 8, "Invalid native electrical fitting");
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
        var dock = profile.dock();
        var van = new VanStaging(new VanStaging.Placement(dock.x(), dock.y(), dock.z(), dock.yaw()));
        rooms[14] = new Room(14, "Expedition van", dock.x() - 1.26, dock.z() - 3, dock.x() + 1.26,
            dock.z() + 2.4, van.floorY(), VanStaging.INTERIOR_HEIGHT, "van", "metal",
            new Vec(dock.x(), van.floorY(), dock.z()));
        for (int i = 0; i < rooms.length; i++) check(rooms[i] != null, "Missing native room owner " + i);
        var scene = NativeInvestigationScene.read(model, modelHash, collision, collisionHash, treads, spaces, doors, grates);
        var nativeComponents = new HashSet<String>();
        for (int i = 0; i < scene.physics().componentCount(); i++) nativeComponents.add(scene.physics().componentName(i));
        for (var socket : sockets) if (socket.type().equals("native_story"))
            check(nativeComponents.contains(socket.id()), "Missing native readable component " + socket.id());
        for (var entry : looseSupports.entrySet()) {
            var item=entry.getKey();
            var bearing=scene.physics().support(item.x(),item.z(),item.y(),.006);
            check(bearing!=null && scene.physics().componentName(scene.physics().triangle(bearing.triangle()).component())
                .equals(entry.getValue()),"Missing physical support for "+item.id());
            check(scene.spaces().stream().anyMatch(s->s.accessible() && s.owner()==item.room()
                && s.contains(item.x(),item.y(),item.z())),"Belonging outside its authored space "+item.id());
        }
        var level = new LevelDefinition(profile.id(), scene.revision(), profile.title(), List.of(rooms), portals, List.of(),
            List.of(), List.of(), sockets, List.of(), dock, "highland", water);
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
