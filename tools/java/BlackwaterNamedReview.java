package org.beckenburg;

import org.beckenburg.LevelDefinition.Socket;
import org.beckenburg.LevelDefinition.Vec;
import org.beckenburg.NativeSchoolInteriorReview.Peer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/** Replays named production constructors using only the resources embedded in the ordinary game archive. */
final class BlackwaterNamedReview {
    private static final String RESOURCE = "/org/beckenburg/sites/wld-09/";

    private BlackwaterNamedReview() {}

    public static void main(String[] args) throws Exception {
        Path stage = Path.of(args[0]);
        if (!List.of(args).contains("--gpu")) {
            review(stage, false);
            return;
        }
        var desktop = Class.forName("org.beckenburg.gpu.GpuDesktop");
        var start = desktop.getDeclaredMethod("startGpu");
        var stop = desktop.getDeclaredMethod("stopReviewGpu");
        start.setAccessible(true);
        stop.setAccessible(true);
        start.invoke(null);
        try {
            review(stage, true);
        } finally {
            stop.invoke(null);
        }
    }

    private static void review(Path stage, boolean gpu) throws Exception {
        var entry = MissionSites.byId("WLD-09");
        var location = entry.location();
        require(entry.playable() && List.of(SiteCatalog.soloLocations()).contains(location)
            && SiteCatalog.mapFor(location) == HouseMap.Site.BLACKWATER && SiteCatalog.playable(HouseMap.Site.BLACKWATER),
            "Blackwater is missing from the named multiplayer or solo entries");
        var campaign = MissionSites.campaign().stream().map(MissionSites.Entry::id).toList();
        require(campaign.indexOf("WLD-09") == 17 && campaign.get(16).equals("WLD-08") && campaign.get(18).equals("WLD-10"),
            "Published campaign position changed");
        for (String file : List.of("dam-with-ventilation.glb", "dam-with-ventilation.hcs2", "runtime-input.tsv",
                                  "door-schedule.tsv", "electrical-schedule.tsv", "blackwater.water", "source-hashes.tsv")) {
            var resource = BlackwaterNativeLevel.class.getResource(RESOURCE + file);
            require(resource != null && resource.getProtocol().equals("jar"), "Review resource is outside the game archive " + file);
        }
        var source = BlackwaterNativeLevel.packaged();
        var map = new HouseMap(SiteCatalog.mapFor(location));
        var network = HouseMap.forNetworkRevision(map.site(), map.level().revision());
        require(map.level().equals(source.level()) && map.nativeInvestigation() == source.scene()
            && network.nativeInvestigation() == source.scene() && map.valid() && RitualSites.valid(map),
            "Named construction does not share the archived case authority");
        try {
            HouseMap.forNetworkRevision(map.site(), "stale-source");
            throw new AssertionError("Named client accepted a mismatched map revision");
        } catch (IllegalArgumentException expected) {
            require(expected.getMessage().equals("incompatible level revision"), expected.getMessage());
        }
        require(source.scene().doors().size() == 14 && HidingBudget.candidates(map.level()).size() == 3
            && map.level().water().placements().size() == 2
            && map.level().sockets().stream().filter(s -> s.type().equals("switch")).count() == 2,
            "Packaged case dropped door, refuge, water, or circuit authority");

        var fixtures = new HouseMap(map.site());
        int testedDoors = 0;
        for (var door : source.scene().doors()) {
            NativeSchoolInteriorReview.audit(fixtures, door.spec(), false);
            fixtures.toggleDoor(door.spec().portal().id(), System.nanoTime() - 2_000_000_000L);
            NativeSchoolInteriorReview.audit(fixtures, door.spec(), true);
            testedDoors++;
        }
        var captures = stage.resolve(gpu ? "captures/opengl" : "captures/software");
        Files.createDirectories(captures);
        capture(map, new Vec(42, 0, 86.3), new Vec(36, 1.1, 81.7), captures.resolve("blackwater-named-arrival.png"));
        capture(map, new Vec(21.4, 0, 52.3), new Vec(17, 1.30, 47.89), captures.resolve("blackwater-named-crest.png"));
        capture(map, new Vec(42.55, -6, 13.3), new Vec(42.20, -4.52, 8.17),
            captures.resolve("blackwater-named-switchgear.png"));
        var report = new ArrayList<>(List.of("check\tresult",
            "packaged_native_scene\t" + source.scene().revision(), "named_network_revision\t" + map.level().revision(),
            "collision_sha256\t" + source.scene().physics().sha256(), "water_revision\t" + map.level().water().revision(),
            "renderer\t" + (gpu ? GpuWorldRenderer.active().rendererDescription() : "Software"),
            "review_gate_archive_campaign_stale_revision\tPASS", "retained_fitted_doors_both_states\t" + testedDoors));
        if (!gpu) {
            solo(map, report);
            live(map, source.scene(), report);
        }
        Files.write(stage.resolve(gpu ? "named-opengl.tsv" : "named-software.tsv"), report);
        report.forEach(System.out::println);
    }

    static void solo(HouseMap map, List<String> report) throws Exception {
        var frames = new LinkedBlockingQueue<String>();
        try (var host = new LanHost("127.0.0.1", 0, GhostIdentity.Type.SPIRIT,
                CustomContract.standard(1), false, true, map.site());
             var client = new LanClient("127.0.0.1", host.port(), false, map.site() + " solo", "",
                 host.localOwnerCredential(), frames::offer)) {
            WharfPowerChecks.await(frames, "SITE|" + map.site() + "|" + map.level().revision());
            client.ready();
            WharfPowerChecks.await(frames, "LOBBY|1|AI|1|WAITING");
            client.start();
            WharfPowerChecks.await(frames, "INFO|mission started");
            client.view3D();
            WharfPowerChecks.await(frames, "ACCEPT|VIEW3D");
            Vec feet = awaitBody(frames);
            require(map.roomAt(feet.x(), feet.y(), feet.z()) == 14, "Blackwater solo spawned outside cargo");
            for (int i = 0; i < 15; i++) feet = soloStep(client, frames, -100);
            require(map.roomAt(feet.x(), feet.y(), feet.z()) == 13, "Blackwater solo missed the arrival apron");
            for (int i = 0; i < 15; i++) feet = soloStep(client, frames, 100);
            require(map.roomAt(feet.x(), feet.y(), feet.z()) == 14, "Blackwater solo return missed cargo");
            client.vanDoor();
            WharfPowerChecks.await(frames, "ACCEPT|VANDOOR|CLOSED");
            WharfPowerChecks.await(frames, "RESULT|RETREATED");
        }
        report.add("named_solo_ai_cargo_apron_and_retreat\tPASS");
    }

    private static Vec soloStep(LanClient client, LinkedBlockingQueue<String> frames, int dz) throws Exception {
        for (int attempt = 0; attempt < 3; attempt++) {
            Thread.sleep(attempt == 0 ? 140 : 200);
            client.step(0, dz);
            long deadline = System.nanoTime() + 5_000_000_000L;
            while (System.nanoTime() < deadline) {
                String frame = frames.poll(100, TimeUnit.MILLISECONDS);
                if (frame == null) continue;
                if (frame.equals("ACCEPT|STEP")) return awaitBody(frames);
                if (frame.equals("REJECT|movement rate exceeded")) break;
                require(!frame.startsWith("REJECT|"), "Solo step rejected " + frame);
            }
        }
        throw new AssertionError("No named solo movement response");
    }

    private static Vec awaitBody(LinkedBlockingQueue<String> frames) throws Exception {
        long deadline = System.nanoTime() + 5_000_000_000L;
        while (System.nanoTime() < deadline) {
            var frame = frames.poll(100, TimeUnit.MILLISECONDS);
            if (frame != null && frame.startsWith("BODY|0|")) return NativeSchoolInteriorReview.body(frame);
        }
        throw new AssertionError("No named solo world body");
    }

    private static void live(HouseMap named, NativeInvestigationScene scene, List<String> report) throws Exception {
        var contract = new CustomContract(3, 100, 150, 100, 100, 100, 100, 100,
            CustomContract.Sprint.LIMITED, 100, true, true);
        try (var host = new LanHost("127.0.0.1", 0, GhostIdentity.Type.SPIRIT, contract, false, false, named.site());
             var operator = new Peer(host, false, "Dam investigator");
             var witness = new Peer(host, false, "Dam van monitor");
             var crew = new Peer(host, false, "Dam support");
             var ghost = new Peer(host, true, "Dam ghost")) {
            var map = (HouseMap) NativeSchoolInteriorReview.field(host, "map");
            var replica = HouseMap.forNetworkRevision(named.site(), named.level().revision());
            var identity = "SITE|BLACKWATER|" + named.level().revision();
            for (var peer : List.of(operator, witness, crew, ghost)) {
                peer.awaitExact(identity);
                peer.client.ready();
            }
            operator.awaitExact("LOBBY|3|HUMAN|4|WAITING");
            operator.client.equipAt(0, "VIDEO_CAMERA");
            operator.awaitExact("ACCEPT|GEAREQUIP|0|VIDEO_CAMERA");
            operator.client.start();
            operator.await("INFO|mission started");
            operator.client.view3D();
            operator.awaitExact("ACCEPT|VIEW3D");
            Vec at = NativeSchoolInteriorReview.body(operator.await("BODY|0|"));
            require(map.roomAt(at.x(), at.y(), at.z()) == 14, "Named LAN missed cargo");
            ghost.client.viewGhost3D();
            ghost.awaitExact("ACCEPT|GVIEW3D");
            ghost.await("GPOSE|");
            NativeSchoolInteriorReview.ghostTrip(host, witness, ghost);
            for (int i = 0; i < 15; i++) at = NativeSchoolInteriorReview.step(operator, 0, 0, -100);
            at = NorthwatchDoorRuntime.walk(operator, at, new Vec(36, 0, 69), new Vec(40, 0, 69),
                new Vec(40, 0, 52), new Vec(31, 0, 52));
            at = NorthwatchDoorRuntime.open(map, replica, scene, operator, witness, at, 873);
            at = NorthwatchDoorRuntime.walk(operator, at, new Vec(31, 0, 46.5), new Vec(31, 0, 45.23),
                new Vec(31, -3, 35.7), new Vec(31, -3, 34.3), new Vec(31, -6, 24.78), new Vec(31, -6, 23.5),
                new Vec(34.7, -6, 23.5), new Vec(34.7, -6, 35));
            at = NorthwatchDoorRuntime.open(map, replica, scene, operator, witness, at, 874);
            at = NorthwatchDoorRuntime.walk(operator, at, new Vec(50, -6, 35), new Vec(45, -6, 23.2));
            at = NorthwatchDoorRuntime.open(map, replica, scene, operator, witness, at, 880);
            require(map.roomAt(at.x(), at.y(), at.z()) == 10 && replica.doorOpen(873)
                && replica.doorOpen(874) && replica.doorOpen(880), "Named blackout route missed switchgear or replicated leaves");

            var generator = map.level().socket("generator", 10);
            var breaker = RoomLighting.breaker(map);
            var aisle = new Vec(45, -6, 15);
            var lights = (RoomLighting) NativeSchoolInteriorReview.field(host, "lights");
            at = NorthwatchDoorRuntime.walk(operator, at, aisle, new Vec(generator.x(), -6, 12), stance(generator));
            WharfPowerChecks.use(map, operator, at, generator);
            witness.awaitExact("SOURCE|0");
            Thread.sleep(400);
            WharfPowerChecks.use(map, operator, at, generator);
            witness.awaitExact("SOURCE|1");
            require(lights.sourceRunning() && !lights.breaker() && !lights.supplied(), "Restart erased the named ghost trip");
            at = NorthwatchDoorRuntime.walk(operator, at, new Vec(generator.x(), -6, 12),
                new Vec(breaker.x(), -6, 12), stance(breaker));
            WharfPowerChecks.use(map, operator, at, breaker);
            witness.awaitExact("LIGHTS|111111111111|1");
            require(lights.supplied(), "Named breaker did not restore supply");
            report.add("named_four_peer_blackout_three_leaves_and_power_reset\tPASS");

            at = NorthwatchDoorRuntime.walk(operator, at, new Vec(breaker.x(), -6, 12), aisle);
            operator.client.turn(90);
            operator.client.useHeldEquipment();
            String placed = operator.await("CAMERA|PLACE|0|10|");
            operator.awaitExact("CAMERA|POWER|0|1");
            witness.awaitExact(placed);
            witness.awaitExact("CAMERA|POWER|0|1");
            report.add("named_switchgear_camera_and_van_feed\tPASS");

            var refuge = map.level().socket("hide", 10);
            var outside = LevelClosets.world(refuge, 0, 0, 1.32);
            at = NorthwatchDoorRuntime.walk(operator, at, new Vec(40.35, -6, 12.25), outside);
            require(!SafeAreaBarrier.protectedAt(map, at.x(), at.y(), at.z()), "Named refuge protects its exterior");
            at = CemeteryRefugeReview.walk(operator, 0, at, LevelClosets.world(refuge, 0, 0, .30));
            require(SafeAreaBarrier.protectedAt(map, at.x(), at.y(), at.z()), "Named refuge did not protect an entering hunter");
            operator.client.hide();
            operator.awaitExact("HIDING|1");
            operator.client.hide();
            operator.awaitExact("HIDING|0");
            at = CemeteryRefugeReview.walk(operator, 0, at, outside);
            require(!SafeAreaBarrier.protectedAt(map, at.x(), at.y(), at.z()), "Protection stayed active after exit");
            report.add("named_switchgear_refuge_enter_hide_exit\tPASS");
        }
    }

    private static Vec stance(Socket socket) {
        return new Vec(socket.x(), -6, socket.z() + 1.18);
    }

    private static void capture(HouseMap map, Vec feet, Vec target, Path image) throws Exception {
        require(map.traversable(feet.x(), feet.y(), feet.z()), "Named review stance lacks support " + feet);
        var view = new SceneRenderer();
        view.setRenderSize(960, 640);
        view.setPsxPreset(PsxPipeline.Preset.OFF);
        view.x = feet.x();
        view.z = feet.z();
        view.eyeHeight = feet.y() + HunterPosture.STANDING_EYE;
        view.yaw = Math.atan2(target.x() - feet.x(), target.z() - feet.z());
        view.pitch = Math.atan2(target.y() - view.eyeHeight, Math.hypot(target.x() - feet.x(), target.z() - feet.z()));
        view.roomLights = new RoomLighting();
        view.weatherModule().setTimeOfDay(.50);
        view.lamp = true;
        view.setLampHardware(EquipmentTiers.Tier.III, false);
        NativeSchoolInteriorReview.drawFrame(view, map, image);
    }

    private static void require(boolean okay, String reason) {
        if (!okay) throw new AssertionError(reason);
    }
}
