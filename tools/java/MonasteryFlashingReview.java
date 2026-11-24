package org.beckenburg;

import org.beckenburg.LevelDefinition.Vec;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

/** Verifies fitted roof joints, the gutter bridge and the retained playable scene. */
public final class MonasteryFlashingReview {
    private record Section(Vec edge, Vec foot, Vec top) {}

    private MonasteryFlashingReview() {}

    public static void main(String[] args) throws Exception {
        var packet = Path.of(args[0]);
        var prior = Path.of(args[1]);
        if (!List.of(args).contains("--gpu")) {
            review(packet, prior, false);
            return;
        }
        var desktop = Class.forName("org.beckenburg.gpu.GpuDesktop");
        var start = desktop.getDeclaredMethod("startGpu");
        var stop = desktop.getDeclaredMethod("stopReviewGpu");
        start.setAccessible(true);
        stop.setAccessible(true);
        start.invoke(null);
        try {
            review(packet, prior, true);
        } finally {
            stop.invoke(null);
        }
    }

    private static void review(Path packet, Path prior, boolean gpu) throws Exception {
        for (String name : List.of("runtime-input.tsv", "door-schedule.tsv", "handrail-schedule.tsv", "valley-schedule.tsv")) {
            require(Arrays.equals(Files.readAllBytes(packet.resolve(name)), Files.readAllBytes(prior.resolve(name))),
                "Roof flashings changed an existing schedule " + name);
        }
        var before = MonasteryDoorReview.read(prior, prior, "monastery-with-roof-valleys").nativeScene().physics();
        var data = MonasteryDoorReview.read(packet, packet, "monastery-with-roof-flashings");
        var physics = data.nativeScene().physics();
        var map = data.map();
        require(map.valid() && RitualSites.valid(map), "Roof work changed room or rite validity");
        var joints = new LinkedHashMap<String, List<Section>>();
        for (String row : Files.readAllLines(packet.resolve("flashing-schedule.tsv"))) {
            var p = row.split("\t");
            joints.computeIfAbsent(p[0], k -> new ArrayList<>()).add(new Section(vec(p, 4), vec(p, 7), vec(p, 10)));
        }
        require(joints.size() == 7, "Missing measured tower or gable join");
        var report = new ArrayList<>(List.of("check\tresult", "native_scene_revision\t" + data.nativeScene().revision(),
            "level_revision\t" + data.level().revision(), "collision_sha256\t" + physics.sha256(),
            "renderer\t" + (gpu ? GpuWorldRenderer.active().rendererDescription() : "Software")));
        int aprons = 0;
        int knees = 0;
        for (var joint : joints.entrySet()) {
            var list = joint.getValue();
            int distinctFoot = 0;
            for (int i = 1; i < list.size(); i++) {
                var a = list.get(i - 1);
                var b = list.get(i);
                var edge = mix(a.edge(), b.edge(), .5);
                var foot = mix(a.foot(), b.foot(), .5);
                var face = mix(a.top(), b.top(), .5);
                var apron = mix(edge, foot, .58);
                require(!physics.clearSight(up(apron, .023), up(apron, -.025)),
                    "Native flashing apron is open " + joint.getKey() + " " + apron);
                aprons++;
                // The rising fold is intersected before the ray reaches the
                // receiving wall. At a shallow end lap the upper slate may also
                // intercept, so separately count rays unique to the new metal.
                var fold = mix(foot, face, .23);
                double dx = face.x() - foot.x(), dz = face.z() - foot.z();
                double span = Math.hypot(dx, dz);
                var p = new Vec(fold.x() - .011*dx/span, fold.y(), fold.z() - .011*dz/span);
                var q = new Vec(fold.x() + .006*dx/span, fold.y(), fold.z() + .006*dz/span);
                if (before.clearSight(p, q) && !physics.clearSight(p, q)) {
                    distinctFoot++;
                    knees++;
                }
                if (joint.getKey().equals("tower west eave")) {
                    require(a.foot().y() > b.foot().y(), "West tower gutter reversed its fall");
                }
            }
            require(distinctFoot >= 4, "Flashing did not produce a separately measurable fold " + joint.getKey());
            report.add(joint.getKey().replace(' ', '_') + "_isolated_folds\t" + distinctFoot);
        }
        var apronEnd = joints.get("tower west eave").getLast().foot();
        var valleyHandoff = new Vec(49.995, 3.377, 30.22);
        require(apronEnd.y() > valleyHandoff.y(), "Tower outlet rises at the refectory valley");
        int bridgeRays = 0;
        for (double t : new double[]{.22,.44,.66,.83}) {
            var at = mix(apronEnd, valleyHandoff, t);
            require(before.clearSight(up(at, .0018), up(at, -.0018))
                && !physics.clearSight(up(at, .0018), up(at, -.0018))
                && physics.clearSight(up(at, .042), up(at, .019)),
                "Tower outlet bridge is absent or obstructed " + at);
            bridgeRays++;
        }
        for (var door : data.nativeScene().doors()) {
            var spec = door.spec();
            require(NativeSchoolInteriorReview.sweep(physics, spec), "Flashing obstructs a door " + spec.frameId());
            NativeSchoolInteriorReview.audit(map, spec, false);
            map.toggleDoor(spec.portal().id(), System.nanoTime() - 2_000_000_000L);
            NativeSchoolInteriorReview.audit(map, spec, true);
        }
        int directionReplays = 0;
        for (var route : data.paths().entrySet()) {
            var back = new ArrayList<>(route.getValue());
            Collections.reverse(back);
            for (double posture : new double[]{HunterPosture.STANDING_HEIGHT, HunterPosture.CROUCH_HEIGHT}) {
                NativeSchoolInteriorReview.walk(map, route.getValue(), posture, route.getKey());
                NativeSchoolInteriorReview.walk(map, back, posture, route.getKey() + " reverse");
                directionReplays += 2;
            }
        }
        var vaultViews = packet.resolve(gpu ? "captures/opengl" : "captures/software");
        Files.createDirectories(vaultViews);
        capture(map, new Vec(66.35,0,24.03), new Vec(56.6,5.88,30), vaultViews.resolve("tower-refectory-step-from-service-court.png"));
        capture(map, new Vec(16.3,0,27.4), new Vec(11.2,3.42,22), vaultViews.resolve("north-west-roof-step-from-herb-court.png"));
        NativeSchoolInteriorReview.auditHiding(map, report);
        if (!gpu) MonasteryDoorReview.live(data, report);
        report.addAll(List.of("native_apron_spans\t" + aprons, "distinct_native_fold_probes\t" + knees,
            "tower_to_refectory_bridge_probes\t" + bridgeRays, "original_route_replays\t" + directionReplays,
            "retained_doors\t" + data.nativeScene().doors().size()));
        Files.write(packet.resolve(gpu ? "flashings-opengl.tsv" : "flashings-software.tsv"), report);
        report.forEach(System.out::println);
    }

    private static Vec mix(Vec a, Vec b, double t) {
        return new Vec(a.x() + (b.x() - a.x()) * t, a.y() + (b.y() - a.y()) * t, a.z() + (b.z() - a.z()) * t);
    }

    private static Vec up(Vec a, double y) { return new Vec(a.x(), a.y() + y, a.z()); }

    private static Vec vec(String[] p, int i) {
        return new Vec(Double.parseDouble(p[i]), Double.parseDouble(p[i + 1]), Double.parseDouble(p[i + 2]));
    }

    private static void capture(HouseMap map, Vec feet, Vec target, Path file) throws Exception {
        require(map.traversable(feet.x(), feet.y(), feet.z()), "Unsupported standing exterior view " + feet);
        var renderer = new SceneRenderer();
        renderer.setRenderSize(960, 640);
        renderer.setPsxPreset(PsxPipeline.Preset.OFF);
        renderer.x = feet.x();
        renderer.z = feet.z();
        renderer.eyeHeight = feet.y() + HunterPosture.STANDING_EYE;
        renderer.yaw = Math.atan2(target.x() - feet.x(), target.z() - feet.z());
        renderer.pitch = Math.atan2(target.y() - renderer.eyeHeight, Math.hypot(target.x() - feet.x(), target.z() - feet.z()));
        renderer.weatherModule().setTimeOfDay(.5);
        renderer.lamp = true;
        renderer.setLampHardware(EquipmentTiers.Tier.III, false);
        NativeSchoolInteriorReview.drawFrame(renderer, map, file);
    }

    private static void require(boolean ok, String error) {
        if (!ok) throw new AssertionError(error);
    }
}
