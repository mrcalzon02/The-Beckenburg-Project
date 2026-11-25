package org.beckenburg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import org.beckenburg.LevelDefinition.Dock;
import org.beckenburg.LevelDefinition.Vec;

/** Replays the staged School arrival through the production native loader and movement kernel. */
public final class NativeSchoolArrivalProbe {
    private static double number(String[] row, int i) {
        return Double.parseDouble(row[i]);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static Vec walk(HouseMap map, List<Vec> waypoints, double height, boolean blockedExpected) {
        Vec feet = waypoints.get(0);
        require(map.traversable(feet.x(), feet.y(), feet.z(), height), "Invalid start " + feet);
        for (int i = 1; i < waypoints.size(); i++) {
            Vec from = feet;
            Vec end = waypoints.get(i);
            int count = Math.max(1, (int) Math.ceil(Math.hypot(end.x()-from.x(), end.z()-from.z()) / .15));
            for (int step = 1; step <= count; step++) {
                double t = (double) step / count;
                double x = from.x() + t * (end.x()-from.x()), z = from.z() + t * (end.z()-from.z());
                var next = map.resolveMove(feet, x, z, height);
                if (next == null && blockedExpected) return feet;
                require(next != null, "Arrival blocked at " + feet + " toward " + x + "," + z);
                feet = next;
            }
        }
        require(!blockedExpected, "Closed cargo door did not stop arrival");
        Vec destination = waypoints.get(waypoints.size()-1);
        require(Math.abs(feet.y()-destination.y()) < .045,
                "Wrong receiving elevation " + feet + " at " + destination);
        return feet;
    }

    private static Vec onDeck(VanStaging van, VanStaging.Position p) {
        return new Vec(p.x(), van.floorY(), p.z());
    }

    public static void main(String[] args) throws Exception {
        var rows = Files.readAllLines(Path.of(args[0])).stream().map(line -> line.split("\t", -1)).toList();
        var source = rows.get(0);
        var dockRow = rows.get(1);
        require(source[0].equals("SOURCE") && dockRow[0].equals("DOCK"), "Invalid arrival proof source");
        var dock = new Dock(number(dockRow,1), number(dockRow,2), number(dockRow,3), number(dockRow,4));
        var stairs = new HashSet<String>();
        var spaces = new ArrayList<NativeInvestigationScene.Space>();
        var arrival = new ArrayList<Vec>();
        for (String[] row : rows) {
            switch (row[0]) {
                case "TREAD" -> stairs.add(row[1]);
                case "SPACE" -> spaces.add(new NativeInvestigationScene.Space(
                        row[1], Integer.parseInt(row[2]), number(row,3), number(row,4),
                        number(row,5), number(row,6), number(row,7), number(row,8),
                        Boolean.parseBoolean(row[9]), Boolean.parseBoolean(row[10])));
                case "POINT" -> arrival.add(new Vec(number(row,1), number(row,2), number(row,3)));
                default -> {}
            }
        }
        NativeInvestigationScene scene;
        try (var glb = Files.newInputStream(Path.of(source[1]));
             var nativeCollision = Files.newInputStream(Path.of(source[3]))) {
            scene = NativeInvestigationScene.read(glb, source[2], nativeCollision, source[4], stairs, spaces);
        }
        // The stable room index comes from the current game. All spatial
        // ownership in this replay comes from the staged scene's surveyed volumes.
        var roomIndex = LevelPlans.buildSource(HouseMap.Site.SCHOOL);
        var review = new LevelDefinition("school", scene.revision(), "Ravenshade staged arrival",
                roomIndex.rooms(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                dock, roomIndex.ambience(), roomIndex.water(), roomIndex.sounds());
        var map = new HouseMap(HouseMap.Site.SCHOOL, review, null, scene);
        var van = map.van();
        var toe = van.world(0, VanStaging.FRONT-VanStaging.RAMP_LENGTH);
        require(Math.hypot(toe.x()-arrival.get(0).x(), toe.z()-arrival.get(0).z()) < 1e-6,
                "Layout arrival origin differs from canonical ramp toe");
        for (double across : new double[]{-.38, -.19, 0, .19, .38}) {
            var p = van.world(across, VanStaging.FRONT-VanStaging.RAMP_LENGTH-.25);
            var ground = scene.physics().support(p.x(), p.z(), dock.y(), .045);
            require(ground != null && Math.abs(ground.y()-dock.y()) < .025
                    && scene.physics().traversable(new Vec(p.x(), ground.y(), p.z()), HunterPosture.STANDING_HEIGHT),
                    "Unsupported toe receipt " + p);
        }

        Vec hatchApproach = onDeck(van, van.world(0, VanStaging.FRONT+.45));
        var full = new ArrayList<Vec>();
        full.add(hatchApproach);
        full.addAll(arrival);
        Vec foyer = full.get(full.size()-1);
        require(map.roomAt(hatchApproach.x(), hatchApproach.y(), hatchApproach.z()) == 14, "Van owner");
        require(map.roomAt(22.5, 0, 40.5) == 13, "Forecourt owner");
        require(map.roomAt(foyer.x(), foyer.y(), foyer.z()) == 0, "Foyer owner");

        var reverse = new ArrayList<>(full);
        Collections.reverse(reverse);
        for (double height : new double[]{HunterPosture.STANDING_HEIGHT, HunterPosture.CROUCH_HEIGHT}) {
            walk(map, full, height, false);
            walk(map, reverse, height, false);
        }
        for (int seat = 0; seat < 6; seat++) {
            var spawn = van.spawn(seat);
            var center = van.world(0, van.local(spawn.x(), spawn.z()).z());
            walk(map, List.of(onDeck(van, spawn), onDeck(van, center), hatchApproach), HunterPosture.STANDING_HEIGHT, false);
        }

        van.setCargoDoorClosed(true);
        var stop = walk(map, full, HunterPosture.STANDING_HEIGHT, true);
        var local = van.local(stop.x(), stop.z());
        require(local.z() >= VanStaging.FRONT && local.z() < VanStaging.FRONT+.42,
                "Closed hatch stopped at the wrong position: " + stop);
        van.setCargoDoorClosed(false);
        walk(map, full, HunterPosture.STANDING_HEIGHT, false);
        var proof = new ArrayList<>(List.of(
                "measure\tresult",
                "decoded_render_triangles\t" + scene.model().triangles().size(),
                "scene_revision\t" + scene.revision(),
                "staged_owner_sequence\t14,13,0",
                "native_ground_receivers\t5",
                "open_cargo_standing_roundtrip\tPASS",
                "open_cargo_crouching_roundtrip\tPASS",
                "spawn_to_hatch_approaches\t6",
                "closed_cargo_hatch\tBLOCKED_AT_HATCH",
                "open_hatch_after_close\tPASS"));
        // Replay a contiguous spanning tree from the already traversed toe.
        // Each destination must own the actual supported body position.
        var reached = new HashMap<String, Vec>();
        reached.put("stage", arrival.get(0));
        require(scene.spaceAt(toe.x(), dock.y(), toe.z()).id().equals("stage"), "Ramp access is outside stage");
        for (String[] row : rows) {
            if (!row[0].equals("TRANSIT")) continue;
            require(reached.containsKey(row[1]) && !reached.containsKey(row[2]), "Disconnected arrival replay");
            var points = new ArrayList<Vec>();
            Vec linked = reached.get(row[1]);
            require(Math.hypot(linked.x()-number(row,3), linked.z()-number(row,5)) < 1e-6
                    && Math.abs(linked.y()-number(row,4)) < .045, "Disconnected landing " + row[1]);
            points.add(linked);
            for (int i = 6; i < row.length; i += 3)
                points.add(new Vec(number(row,i), number(row,i+1), number(row,i+2)));
            Vec at = walk(map, points, HunterPosture.STANDING_HEIGHT, false);
            var volume = scene.spaceAt(at.x(), at.y(), at.z());
            require(volume != null && volume.id().equals(row[2]) && volume.accessible()
                    && map.roomAt(at.x(), at.y(), at.z()) == volume.owner(),
                    "Reached the wrong room from " + row[1] + ": " + row[2] + " at " + at);
            reached.put(row[2], at);
            proof.add("reached_space\t" + volume.id() + "|" + volume.owner() + "|" + at);
        }
        long accessible = spaces.stream().filter(NativeInvestigationScene.Space::accessible).count();
        require(reached.size() == accessible && accessible <= 40, "Reachable authored space count " + reached.size());
        proof.add("reachable_authored_spaces\t" + reached.size());
        Files.write(Path.of(args[1]), proof);
        System.out.println("RAVENSHADE PRODUCTION ARRIVAL REPLAY PASS");
    }
}
