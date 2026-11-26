package org.beckenburg;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.mrcalzon.water.WaterDocument;
import org.beckenburg.LevelDefinition.Dock;
import org.beckenburg.LevelDefinition.Portal;
import org.beckenburg.LevelDefinition.Prop;
import org.beckenburg.LevelDefinition.Room;
import org.beckenburg.LevelDefinition.Socket;
import org.beckenburg.LevelDefinition.Vec;

/** The authored Wharf scene and room contract used by both review and mission dispatch. */
final class WharfNativeLevel {
    static final String TITLE = "Drowned Fisherman's Wharf";
    static final String REVIEW_PROPERTY = "beckenburg.review.wharfNative";

    record Source(LevelDefinition level, NativeInvestigationScene scene,
                  List<NativeInvestigationDoor.Spec> doors, List<Portal> openings,
                  Map<String, List<Vec>> paths, Map<Integer, Vec> switchFeet, List<Story> stories) {}

    record Story(String document, int owner, Vec feet, Vec aim) {}

    private record Anchor(int owner, String space, Vec feet) {}

    private WharfNativeLevel() {}

    static LevelDefinition definition() {
        return Packaged.SOURCE.level();
    }

    static NativeInvestigationScene sceneFor(LevelDefinition level) {
        var source = Packaged.SOURCE;
        if (!source.level().equals(level)) throw new IllegalArgumentException("Wharf requires its exact authored case");
        return source.scene();
    }

    private static final class Packaged {
        private static final Source SOURCE = load();

        private static Source load() {
            try (var layout = resource("room-layout.tsv"); var doors = resource("door-schedule.tsv");
                 var model = resource("case.glb"); var physics = resource("case.hcs2");
                 var water = resource("tide.water")) {
                var rows = rows(layout);
                var header = rows.get(0);
                require(header.length == 3 && header[0].equals("SOURCE"), "Missing exact Wharf source hashes");
                var source = read(rows, rows(doors), model, header[1], physics, header[2], WaterDocument.read(water));
                var level = source.level();
                require(source.stories().size() == 2 && source.doors().size() == 8 && source.openings().size() == 10
                    && source.switchFeet().size() == 6 && HidingBudget.candidates(level).size() == 3
                    && level.socket("breaker", 12) != null && level.socket("generator", 12) != null
                    && !level.water().placements().isEmpty(), "Incomplete authored Wharf case");
                return source;
            } catch (IOException problem) {
                throw new IllegalStateException("Cannot load " + TITLE, problem);
            }
        }
    }

    private static InputStream resource(String name) throws IOException {
        var stream = WharfNativeLevel.class.getResourceAsStream("/org/beckenburg/sites/wharf-" + name);
        if (stream == null) throw new IOException("Missing Wharf resource " + name);
        return stream;
    }

    static List<String[]> rows(InputStream source) throws IOException {
        var reader = new BufferedReader(new InputStreamReader(source, StandardCharsets.UTF_8));
        var rows = new ArrayList<String[]>();
        for (String line; (line = reader.readLine()) != null;) rows.add(line.split("\\t", -1));
        return rows;
    }

    static Source read(List<String[]> layout, List<String[]> schedule, InputStream model, String modelHash,
                       InputStream physics, String physicsHash, WaterDocument water) throws IOException {
        var spaces = new ArrayList<NativeInvestigationScene.Space>();
        var titles = new LinkedHashMap<String, String>();
        var anchors = new ArrayList<Anchor>();
        var treads = new HashSet<String>();
        var sockets = new ArrayList<Socket>();
        var props = new ArrayList<Prop>();
        var stories = new ArrayList<Story>();
        var switchFeet = new LinkedHashMap<Integer, Vec>();
        var openings = new ArrayList<Portal>();
        var paths = new LinkedHashMap<String, List<Vec>>();
        var dock = new Dock(32, 0, 74, 0);
        for (var row : layout) {
            switch (row[0]) {
                case "SOURCE" -> require(row[1].equals(modelHash) && row[2].equals(physicsHash),
                                         "Wharf manifest and supplied hashes differ");
                case "SPACE" -> {
                    spaces.add(new NativeInvestigationScene.Space(row[1], id(row, 2), d(row, 3), d(row, 4),
                        d(row, 5), d(row, 6), d(row, 7), d(row, 8), Boolean.parseBoolean(row[9]), true));
                    titles.put(row[1], row[10]);
                }
                case "TREAD" -> treads.add(row[1]);
                case "ANCHOR" -> anchors.add(new Anchor(id(row, 1), row[2], point(row, 3)));
                case "SWITCH_FEET" -> switchFeet.put(id(row, 1), point(row, 2));
                case "SERVICE" -> sockets.add(new Socket(row[1], row[2], id(row, 3), d(row, 4), d(row, 5),
                                                          d(row, 6), d(row, 7), row[8]));
                case "COVER" -> sockets.add(new Socket(row[1], "hide", id(row, 2), d(row, 3), d(row, 4),
                                                        d(row, 5), d(row, 6), "walk-in-cupboard"));
                case "RITUAL" -> sockets.add(new Socket("wharf-" + row[1], "ritual", id(row, 2), d(row, 3),
                                                        d(row, 4), d(row, 5), 0, row[1]));
                case "POINT" -> paths.computeIfAbsent(row[1], ignored -> new ArrayList<>()).add(point(row, 2));
                case "TRANSITION" -> openings.add(new Portal(id(row, 1), id(row, 3), id(row, 4), d(row, 5),
                    d(row, 6), d(row, 7), d(row, 9), row[8].equals("z"), row[10], 1));
                case "STORY" -> {
                    int owner = id(row, 3);
                    var pivot = point(row, 5);
                    double yaw = d(row, 8);
                    props.add(new Prop(row[4], owner, pivot.x(), pivot.y(), pivot.z(), d(row, 9),
                                       yaw, 0, 0, .05, "bench"));
                    sockets.add(new Socket(row[1], "story", owner, pivot.x(), pivot.y(), pivot.z(), yaw, row[2]));
                    stories.add(new Story(row[2], owner, point(row, 10), point(row, 13)));
                }
                default -> throw new IOException("Unknown Wharf field " + row[0]);
            }
        }

        var doors = new ArrayList<NativeInvestigationDoor.Spec>();
        var portals = new ArrayList<>(openings);
        for (var row : schedule) {
            var portal = new Portal(id(row, 0), id(row, 4), id(row, 5), d(row, 6), d(row, 7), d(row, 8),
                                    d(row, 9), row[10].equals("z"), "door", 1);
            portals.add(portal);
            doors.add(new NativeInvestigationDoor.Spec(row[1], row[2], row[3], portal, d(row, 11), d(row, 12),
                      d(row, 13), id(row, 14), id(row, 15), id(row, 16), row[17]));
        }
        var scene = NativeInvestigationScene.read(model, modelHash, physics, physicsHash, treads, spaces, doors);
        var rooms = new Room[HouseMap.ROOM_COUNT];
        for (var anchor : anchors) {
            var at = anchor.feet();
            var space = scene.spaceAt(at.x(), at.y(), at.z());
            require(space != null && space.id().equals(anchor.space()) && space.owner() == anchor.owner()
                    && scene.physics().traversable(at, HunterPosture.STANDING_HEIGHT),
                    "Unsupported Wharf owner " + anchor + " / " + space);
            rooms[anchor.owner()] = new Room(anchor.owner(), titles.get(anchor.space()), space.x0(), space.z0(),
                space.x1(), space.z1(), space.floor(), space.height(), space.outside() ? "native-outdoor" : "wood",
                space.outside() ? "outdoor" : "timber", at);
        }

        var van = new VanStaging(new VanStaging.Placement(dock.x(), dock.y(), dock.z(), dock.yaw()));
        var lip = van.world(0, VanStaging.FRONT);
        portals.add(new Portal(880, 13, 14, lip.x(), van.floorY(), lip.z(), VanStaging.RAMP_HALF_WIDTH * 2,
                               true, "trail", 1));
        rooms[14] = new Room(14, "Expedition van", dock.x() - 1.26, dock.z() - 3, dock.x() + 1.26, dock.z() + 2.4,
                             van.floorY(), VanStaging.INTERIOR_HEIGHT, "van", "metal", new Vec(dock.x(), van.floorY(), dock.z()));
        var level = new LevelDefinition("wld-06", scene.revision(), TITLE, List.of(rooms), portals,
                                         List.of(), List.of(), props, sockets, List.of(), dock, "coast", water);
        paths.replaceAll((key, path) -> List.copyOf(path));
        return new Source(level, scene, List.copyOf(doors), List.copyOf(openings),
                          java.util.Collections.unmodifiableMap(paths), java.util.Collections.unmodifiableMap(switchFeet), List.copyOf(stories));
    }

    private static void require(boolean valid, String message) throws IOException {
        if (!valid) throw new IOException(message);
    }

    private static int id(String[] row, int i) { return Integer.parseInt(row[i]); }
    private static double d(String[] row, int i) { return Double.parseDouble(row[i]); }
    private static Vec point(String[] row, int i) { return new Vec(d(row, i), d(row, i + 1), d(row, i + 2)); }
}
