package org.beckenburg;

import org.beckenburg.LevelDefinition.Vec;
import org.beckenburg.NativeSchoolInteriorReview.Peer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Exercises the full walk-in envelopes and existing architectural approaches. */
public final class MonasteryRefugeReview {
    private static final List<Integer> OWNERS = List.of(3, 5, 9);
    private static final Map<Integer, List<Vec>> APPROACHES = Map.of(
        3, List.of(new Vec(22, 0, 7)),
        5, List.of(new Vec(-3, 0, 39)),
        9, List.of(new Vec(23, -3, 7)));
    private static final Map<Integer, List<String>> ROUTES = Map.of(
        3, List.of("L01", "L02", "L03", "L04", "L05", "L11"),
        5, List.of("L01", "L02", "L03", "L04", "L10"),
        9, List.of("L01", "L02", "L03", "L04", "L05", "L19", "L20"));

    private MonasteryRefugeReview() {}

    public static void main(String[] args) throws Exception {
        var packet = Path.of(args[0]);
        if (!List.of(args).contains("--gpu")) {
            review(packet, false);
            return;
        }
        var desktop = Class.forName("org.beckenburg.gpu.GpuDesktop");
        var start = desktop.getDeclaredMethod("startGpu");
        var stop = desktop.getDeclaredMethod("stopReviewGpu");
        start.setAccessible(true);
        stop.setAccessible(true);
        start.invoke(null);
        try {
            review(packet, true);
        } finally {
            stop.invoke(null);
        }
    }

    private static void review(Path packet, boolean gpu) throws Exception {
        review(packet, gpu, MonasteryDoorReview.read(packet, packet, "monastery-with-refuges"));
    }

    static void review(Path packet, boolean gpu, MonasteryDoorReview.Scene data) throws Exception {
        var map = data.map();
        require(map.valid() && RitualSites.valid(map), "Monastery spawn or ritual ownership changed");
        var report = new ArrayList<>(List.of("check\tresult",
            "native_scene_revision\t" + data.nativeScene().revision(),
            "level_revision\t" + data.level().revision(),
            "collision_sha256\t" + data.nativeScene().physics().sha256(),
            "renderer\t" + (gpu ? GpuWorldRenderer.active().rendererDescription() : "Software")));
        var captures = packet.resolve(gpu ? "captures/opengl" : "captures/software");
        Files.createDirectories(captures);
        require(data.nativeScene().doors().size() == 13, "Missing fitted door");
        for (var door : data.nativeScene().doors()) {
            var frame = door.spec();
            require(NativeSchoolInteriorReview.sweep(data.nativeScene().physics(), frame),
                "Cupboard obstructed " + frame.frameId());
            NativeSchoolInteriorReview.audit(map, frame, false);
            map.toggleDoor(frame.portal().id(), System.nanoTime() - 2_000_000_000L);
            NativeSchoolInteriorReview.audit(map, frame, true);
        }
        report.add("retained_door_sweeps\t13");
        int originalWalks = 0;
        for (var entry : data.paths().entrySet()) {
            walkBothWays(map, entry.getValue(), entry.getKey());
            originalWalks += 4;
        }
        report.add("original_route_replays\t" + originalWalks);
        NativeSchoolInteriorReview.auditWalkInCover(map, captures, report, "monastery", APPROACHES);
        NativeSchoolInteriorReview.auditHiding(map, report);
        map.configureHiding(100);
        int samples = 0;
        int boundarySamples = 0;
        for (var pocket : HidingBudget.candidates(map.level())) {
            for (double height : new double[]{0, .42, .88, 1.42, 2.01}) {
                for (int x = -7; x <= 7; x++) {
                    for (int z = -5; z <= 5; z++) {
                        var at = LevelClosets.world(pocket, x * .1, height, z * .1);
                        require(protectedAt(map, at), pocket.id() + " exposed interior at " + at);
                        samples++;
                    }
                }
                for (double x : new double[]{-.78, .78}) {
                    for (double z : new double[]{-.53, .59}) {
                        var at = LevelClosets.world(pocket, x, height, z);
                        require(protectedAt(map, at), pocket.id() + " exposed near a corner at " + at);
                        boundarySamples++;
                    }
                }
                for (double[] offset : new double[][]{{-.83, 0}, {.83, 0}, {0, -.58}, {0, .65}}) {
                    var at = LevelClosets.world(pocket, offset[0], height, offset[1]);
                    require(!protectedAt(map, at), pocket.id() + " protected beyond the enclosure at " + at);
                }
            }
        }
        report.add("samples_across_full_protected_volume\t" + samples);
        report.add("samples_within_one_centimeter_of_four_interior_corners\t" + boundarySamples);
        int fullWalks = 0;
        int disabledPixels = 0;
        for (int owner : OWNERS) {
            var pocket = map.level().socket("hide", owner);
            var route = cargoRoute(data, map, owner);
            route.add(LevelClosets.world(pocket, 0, 0, .30));
            route.add(LevelClosets.world(pocket, 0, 0, -.27));
            walkBothWays(map, route, pocket.id() + " cargo");
            fullWalks += 4;
            map.configureHiding(0);
            disabledPixels += NativeSchoolInteriorReview.captureStanding(map, LevelClosets.world(pocket, 0, 0, -.27),
                -pocket.yaw(), captures.resolve(pocket.id() + "-disabled.png"));
            map.configureHiding(100);
        }
        require(disabledPixels == 0, "Inactive refuges drew sanctuary energy");
        report.add("cargo_arrival_and_return_replays\t" + fullWalks);
        report.add("disabled_refuge_cue_pixels\t" + disabledPixels);
        if (!gpu) {
            live(data, 100, report);
            live(data, 0, report);
        }
        Files.write(packet.resolve(gpu ? "refuges-opengl.tsv" : "refuges-software.tsv"), report);
        report.forEach(System.out::println);
    }

    private static void walkBothWays(HouseMap map, List<Vec> route, String name) {
        var back = new ArrayList<>(route);
        Collections.reverse(back);
        for (double height : new double[]{HunterPosture.STANDING_HEIGHT, HunterPosture.CROUCH_HEIGHT}) {
            NativeSchoolInteriorReview.walk(map, route, height, name);
            NativeSchoolInteriorReview.walk(map, back, height, name + " return");
        }
    }

    private static ArrayList<Vec> cargoRoute(MonasteryDoorReview.Scene data, HouseMap map, int owner) {
        var van = map.van();
        var center = van.world(0, 0);
        var toe = van.world(0, VanStaging.FRONT);
        var route = new ArrayList<>(List.of(new Vec(center.x(), van.floorY(), center.z()),
            new Vec(toe.x(), van.floorY(), toe.z())));
        for (String link : ROUTES.get(owner)) {
            var points = data.paths().get(link);
            var last = route.getLast();
            boolean sameKnot = last.equals(points.getFirst());
            require(sameKnot || link.equals("L01"), "Broken physical route join at " + link);
            route.addAll(points.subList(sameKnot ? 1 : 0, points.size()));
        }
        route.addAll(APPROACHES.get(owner));
        route.add(LevelClosets.world(map.level().socket("hide", owner), 0, 0, 1.32));
        return route;
    }

    private static void live(MonasteryDoorReview.Scene data, int density, List<String> report) throws Exception {
        var map = data.map();
        var contract = new CustomContract(3, 100, 150, 100, 100, 100, 100, 100,
            CustomContract.Sprint.LIMITED, density, true, true);
        try (var host = new LanHost("127.0.0.1", 0, GhostIdentity.Type.SPIRIT, contract, map);
             var archive = new Peer(host, false, "Archive investigator");
             var dorm = new Peer(host, false, "Dormitory investigator");
             var chamber = new Peer(host, false, "Memorial investigator");
             var ghost = new Peer(host, true, "Spectral witness")) {
            for (var peer : List.of(archive, dorm, chamber, ghost)) {
                peer.awaitExact("SITE|USER|" + data.level().revision());
                peer.client.ready();
            }
            archive.awaitExact("LOBBY|3|HUMAN|4|WAITING");
            archive.client.start();
            archive.await("INFO|mission started");
            var hunters = List.of(archive, dorm, chamber);
            for (int seat = 0; seat < hunters.size(); seat++) {
                var hunter = hunters.get(seat);
                int owner = OWNERS.get(seat);
                var pocket = map.level().socket("hide", owner);
                for (int destination : NativeSchoolInteriorReview.missionRoute(map, owner)) {
                    hunter.client.move(destination);
                    hunter.awaitExact("ACCEPT|MOVE|" + destination);
                }
                hunter.client.view3D();
                hunter.awaitExact("ACCEPT|VIEW3D");
                var at = NativeSchoolInteriorReview.body(hunter.await("BODY|" + seat + "|"));
                for (var point : APPROACHES.get(owner)) {
                    at = CemeteryRefugeReview.walk(hunter, seat, at, point);
                }
                var outside = LevelClosets.world(pocket, 0, 0, 1.32);
                var centre = LevelClosets.world(pocket, 0, 0, 0);
                at = CemeteryRefugeReview.walk(hunter, seat, at, outside);
                require(!protectedAt(map, at), "Protection before crossing " + pocket.id());
                at = CemeteryRefugeReview.walk(hunter, seat, at, centre);
                require(protectedAt(map, at) == (density == 100), "Wrong host protection " + pocket.id());
                hunter.client.hide();
                if (density == 100) {
                    hunter.awaitExact("HIDING|1");
                    hunter.client.hide();
                    hunter.awaitExact("HIDING|0");
                } else {
                    hunter.awaitExact("REJECT|no hiding place within reach");
                }
                at = CemeteryRefugeReview.walk(hunter, seat, at, outside);
                require(!protectedAt(map, at), "Protection remained after exit " + pocket.id());
                report.add(pocket.id() + "_live_density_" + density + "\tPASS");
            }
        }
    }

    private static boolean protectedAt(HouseMap map, Vec p) {
        return SafeAreaBarrier.protectedAt(map, p.x(), p.y(), p.z());
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
