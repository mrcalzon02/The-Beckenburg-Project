package org.beckenburg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import org.beckenburg.LevelDefinition.Vec;

final class GreywaterNativeReview {
    private static final HouseMap.Site SITE = HouseMap.Site.GREYWATER;

    private GreywaterNativeReview() {}

    public static void main(String[] args) throws Exception {
        var entry = MissionSites.byId("WLD-13");
        require(entry.location() == SiteCatalog.Location.GREYWATER,
            "The Greywater mission identity changed");
        require(MissionSites.campaign().get(21).equals(entry), "Greywater campaign ordinal shifted");
        require(entry.playable() && SiteCatalog.playable(SITE)
            && SiteCatalog.mapFor(entry.location()) == SITE, "Solo and LAN disagree on Greywater");
        var map = new HouseMap(SITE);
        var replica = HouseMap.forNetworkRevision(SITE, map.level().revision());
        require(map.valid() && RitualSites.valid(map) && map.nativeInvestigation() == replica.nativeInvestigation(),
            "Greywater map, rites or network authority differ");
        var out = Path.of(args[0]);
        Files.createDirectories(out);
        var report = audit(map, replica);
        if (List.of(args).contains("--gpu")) {
            var desktop = Class.forName("org.beckenburg.gpu.GpuDesktop");
            gpu(desktop, "startGpu");
            try { capture(map, out, report); }
            finally { gpu(desktop, "stopReviewGpu"); }
        } else {
            capture(map, out, report);
        }
        Files.write(out.resolve("review.tsv"), report);
        report.forEach(System.out::println);
    }

    private static List<String> audit(HouseMap map, HouseMap replica) throws Exception {
        var scene = map.nativeInvestigation();
        var report = new ArrayList<>(List.of("check\tresult", "revision\t" + map.level().revision(),
            "collision_sha256\t" + scene.physics().sha256()));
        var failures = new ArrayList<String>();
        for (var door : scene.doors()) {
            var frame = door.spec();
            try {
                require(NativeSchoolInteriorReview.sweep(scene.physics(), frame), "The leaf swing intersects construction");
                NativeSchoolInteriorReview.audit(map, frame, false);
                map.toggleDoor(frame.portal().id(), System.nanoTime() - 2_000_000_000L);
                replica.applyDoorSnapshot(map.doorSnapshot());
                NativeSchoolInteriorReview.audit(map, frame, true);
                NativeSchoolInteriorReview.audit(replica, frame, true);
            } catch (AssertionError bad) {
                failures.add(frame.frameId() + " " + bad.getMessage());
                if (!map.doorOpen(frame.portal().id())) map.toggleDoor(frame.portal().id(), System.nanoTime() - 2_000_000_000L);
            }
        }
        var routes = new LinkedHashMap<String, List<Vec>>();
        try (var in = GreywaterNativeReview.class.getResourceAsStream("/org/beckenburg/sites/wld-13/runtime-input.tsv")) {
            for (var row : WharfNativeLevel.rows(in)) {
                if (!row[0].equals("POINT")) continue;
                routes.computeIfAbsent(row[1], key -> new ArrayList<>()).add(new Vec(
                    Double.parseDouble(row[2]), Double.parseDouble(row[3]), Double.parseDouble(row[4])));
            }
        }
        int passes = 0;
        for (var route : routes.entrySet()) {
            var reverse = new ArrayList<>(route.getValue());
            Collections.reverse(reverse);
            for (var path : List.of(route.getValue(), reverse)) {
                for (double height : new double[]{HunterPosture.STANDING_HEIGHT, HunterPosture.CROUCH_HEIGHT}) {
                    try {
                        NativeSchoolInteriorReview.walk(map, path, height, route.getKey());
                        passes++;
                    } catch (AssertionError blocked) {
                        failures.add(blocked.getMessage());
                    }
                }
            }
        }
        require(failures.isEmpty(), String.join("\n", failures));
        require(passes == 76 && scene.doors().size() == 16, "Incomplete route or door replay");
        report.add("physical_door_states\t16 fitted doors; standing and crouched crossing, object, sight, aim and acoustics");
        report.add("route_replays\t" + passes);
        report.add("owner_spawns\t14 native-supported regions and the expedition van");
        for (double posture : new double[]{HunterPosture.STANDING_HEIGHT, HunterPosture.CROUCH_HEIGHT}) {
            require(map.resolveMove(new Vec(29, 3, 41), 30.45, 41, posture) == null,
                "The mezzanine's outer guard must stop a lateral step");
        }
        report.add("mezzanine_guard\tThe built rail stops a lateral step in both postures");
        var control = map.level().socket("switch", 2);
        var light = map.level().socket("light", 2);
        require(control != null && light != null, "Missing switched boiler lamp");
        var roomLights = new RoomLighting();
        require(roomLights.aimed(map, control.x(), 0, control.z() + 1.15, Math.PI) == 2,
            "The fitted boiler switch cannot be reached from the service aisle");
        var fixtures = new FixtureLighting(map);
        fixtures.frame(roomLights, 0);
        double powered = fixtures.sample(2, light.x(), light.y() - .65, light.z()).energy();
        roomLights.toggle(2);
        fixtures.frame(roomLights, 0);
        double switchedOff = fixtures.sample(2, light.x(), light.y() - .65, light.z()).energy();
        roomLights.toggle(2);
        roomLights.setSourceRunning(false);
        fixtures.frame(roomLights, 0);
        double noSupply = fixtures.sample(2, light.x(), light.y() - .65, light.z()).energy();
        require(powered > 2 * switchedOff && Math.abs(switchedOff - noSupply) < .001,
            "Boiler light must respond to both the local switch and the power supply");
        report.add("boiler_light\tAimable switch, powered lamp " + powered + ", switched off "
            + switchedOff + ", no supply " + noSupply);
        return report;
    }

    private static void capture(HouseMap map, Path output, List<String> report) throws Exception {
        capture(map, new Vec(32, 0, 74.5), new Vec(32, 1.2, 81), output.resolve("greywater-van.png"));
        capture(map, new Vec(36, 0, 53), new Vec(24, 3, 40), output.resolve("greywater-process-hall.png"));
        capture(map, new Vec(27, 3, 41), new Vec(36, 1.2, 41), output.resolve("greywater-gallery.png"));
        capture(map, new Vec(-11.8, 0, 53), new Vec(-8, 1, 53), output.resolve("greywater-bunk-door.png"));
        NativeSchoolInteriorReview.auditWalkInCover(map, output, report, "greywater");
        var gpu = GpuWorldRenderer.active();
        report.add("renderer\t" + (gpu == null ? "Software" : gpu.rendererDescription()));
    }

    private static void capture(HouseMap map, Vec feet, Vec aim, Path output) throws Exception {
        require(map.traversable(feet.x(), feet.y(), feet.z()), "Unsupported review stance " + feet);
        var view = new SceneRenderer();
        view.setRenderSize(960, 640);
        view.setPsxPreset(PsxPipeline.Preset.OFF);
        view.x = feet.x();
        view.z = feet.z();
        view.eyeHeight = feet.y() + HunterPosture.STANDING_EYE;
        view.yaw = Math.atan2(aim.x() - feet.x(), aim.z() - feet.z());
        view.pitch = Math.atan2(aim.y() - view.eyeHeight, Math.hypot(aim.x() - feet.x(), aim.z() - feet.z()));
        view.lamp = true;
        view.setLampHardware(EquipmentTiers.Tier.III, false);
        view.roomLights = new RoomLighting();
        NativeSchoolInteriorReview.drawFrame(view, map, output);
    }

    private static void gpu(Class<?> type, String name) throws Exception {
        var method = type.getDeclaredMethod(name);
        method.setAccessible(true);
        method.invoke(null);
    }

    private static void require(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
}
