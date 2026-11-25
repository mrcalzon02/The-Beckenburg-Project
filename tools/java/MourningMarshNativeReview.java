package org.beckenburg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import org.beckenburg.LevelDefinition.Vec;

final class MourningMarshNativeReview {
    private static final HouseMap.Site SITE = HouseMap.Site.MOURNING_MARSH;

    private MourningMarshNativeReview() {}

    public static void main(String[] args) throws Exception {
        var entry = MissionSites.byId("WLD-11");
        require(entry.location() == SiteCatalog.Location.MOURNING_MARSH && !entry.playable(),
            "The native Mourning Marsh review gate or mission identity changed");
        require(MissionSites.campaign().get(19).equals(entry), "Mourning Marsh campaign ordinal shifted");
        System.setProperty(MourningMarshNativeLevel.REVIEW_PROPERTY, "true");
        require(entry.playable() && SiteCatalog.playable(SITE)
            && SiteCatalog.mapFor(entry.location()) == SITE, "Solo and LAN disagree on Mourning Marsh");
        var map = new HouseMap(SITE);
        var replica = HouseMap.forNetworkRevision(SITE, map.level().revision());
        require(map.valid() && RitualSites.valid(map) && map.nativeInvestigation() == replica.nativeInvestigation(),
            "Mourning Marsh map, rites or network authority differ");
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
        var approach = new Vec(40.06, 0, 72.001);
        require(map.traversable(approach.x(), approach.y(), approach.z())
            && map.roomAt(approach.x(), approach.y(), approach.z()) == 12,
            "A normal hunter step at the supported causeway turn lost room ownership");
        report.add("causeway_turn\tThe supported outside edge retains the causeway owner at normal step length");
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
        try (var in = MourningMarshNativeReview.class.getResourceAsStream("/org/beckenburg/sites/wld-11/runtime-input.tsv")) {
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
        require(passes == 56 && scene.doors().size() == 7, "Incomplete route or door replay");
        report.add("physical_door_states\t7 fitted doors; standing and crouched crossing, object, sight, aim and acoustics");
        report.add("route_replays\t" + passes);
        report.add("owner_spawns\t14 native-supported regions and the expedition van");
        for (double posture : new double[]{HunterPosture.STANDING_HEIGHT, HunterPosture.CROUCH_HEIGHT}) {
            require(map.resolveMove(new Vec(34, 1.2, 57), 34, 55.5, posture) == null,
                "The wet side of the south crossing lacks a physical barrier");
            require(map.resolveMove(new Vec(30.7, 1.2, 49), 32.2, 49, posture) == null,
                "The west boardwalk no longer guards the gap between the decks");
            require(map.resolveMove(new Vec(34, 1.2, -4.8), 34, -6.3, posture) == null,
                "The ferry end no longer guards the drop");
        }
        require(!map.clearLineOfSight(34, 1.67, 56.5, 34, 1.67, 55.85)
            && !map.clearLineOfSight(31.6, 1.67, 49, 32.2, 1.67, 49)
            && !map.clearLineOfSight(34, 1.67, -5.50, 34, 1.67, -6.20),
            "The fitted rails must also provide the actual optical barriers");
        report.add("deck_guards\t3 supported edge sweeps in each posture stopped on the actual rails");
        return report;
    }

    private static void capture(HouseMap map, Path output, List<String> report) throws Exception {
        capture(map, new Vec(32, 0, 74.5), new Vec(32, 1.2, 81), output.resolve("marsh-van-causeway.png"));
        capture(map, new Vec(39.98, 0, 67.1), new Vec(40, 2, 59), output.resolve("marsh-levee-stair.png"));
        capture(map, new Vec(30, 1.2, 27), new Vec(26, 2.2, 25), output.resolve("marsh-chapel-door.png"));
        capture(map, new Vec(40, 1.2, 59), new Vec(29, 2.2, 55), output.resolve("marsh-crossing.png"));
        NativeSchoolInteriorReview.auditWalkInCover(map, output, report, "mourning_marsh");
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
