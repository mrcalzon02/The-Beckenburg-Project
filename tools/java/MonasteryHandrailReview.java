package org.beckenburg;

import org.beckenburg.LevelDefinition.Vec;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Compares the installed handrails against the prior shared render and collision scene. */
public final class MonasteryHandrailReview {
    private record View(String name, Vec feet, Vec target) {}

    private static final List<View> VIEWS = List.of(
        new View("bell-lower-flight", new Vec(55, 0, 17.08), new Vec(55, 2.35, 21.85)),
        new View("bell-upper-flight", new Vec(57.04, 3, 24.48), new Vec(57.04, 5.55, 20.03)),
        new View("bell-turning-landing", new Vec(55.1, 3, 23.89), new Vec(57.15, 4.02, 24.65)),
        new View("west-catacomb-flight", new Vec(11.95, 0, 19), new Vec(16.48, -.86, 19)),
        new View("east-catacomb-flight", new Vec(25.04, -3, 19), new Vec(29.32, -.62, 19)));

    private MonasteryHandrailReview() {}

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
        for (String name : List.of("runtime-input.tsv", "door-schedule.tsv")) {
            require(Arrays.equals(Files.readAllBytes(packet.resolve(name)), Files.readAllBytes(prior.resolve(name))),
                "Rail work changed the authored travel or door authority " + name);
        }
        var before = MonasteryDoorReview.read(prior, prior, "monastery-with-refuges").nativeScene().physics();
        var data = MonasteryDoorReview.read(packet, packet, "monastery-with-handrails");
        var physics = data.nativeScene().physics();
        var map = data.map();
        require(map.valid() && RitualSites.valid(map), "Stair hardware invalidated the rooms or rites");
        int physicalRails = 0;
        var schedule = Files.readAllLines(packet.resolve("handrail-schedule.tsv"));
        var guide = schedule.stream().filter(s -> s.startsWith("RAIL\t")).toList();
        var flights = schedule.stream().filter(s -> s.startsWith("FLIGHT\t")).toList();
        require(flights.size() == 4 && flights.size() + guide.size() == schedule.size(),
            "Expected the four surveyed stair flights");
        require(guide.size() == 11, "Expected four sided flights and three landing rails");
        for (String row : guide) {
            var cells = row.split("\t");
            require(cells[0].equals("RAIL"), "Unknown handrail schedule entry " + row);
            var a = vec(cells, 4);
            var b = vec(cells, 7);
            for (double sample : new double[]{.17, .27, .48, .59, .81}) {
                var mid = new Vec(a.x() + (b.x() - a.x()) * sample, a.y() + (b.y() - a.y()) * sample,
                    a.z() + (b.z() - a.z()) * sample);
                boolean axisX = cells[2].equals("x");
                double sign = Double.parseDouble(cells[3]);
                var inside = offset(mid, axisX, sign * .067);
                var outside = offset(mid, axisX, -sign * .053);
                require(before.clearSight(inside, outside) && !physics.clearSight(inside, outside),
                    "Native physics missed the isolated handrail " + cells[1] + " at " + mid);
                var clearance = offset(mid, axisX, sign * .12);
                require(physics.clearSight(clearance, inside), "Unexpected protrusion into the aisle " + cells[1]);
                physicalRails++;
            }
        }

        int treadSamples = 0;
        for (String row : flights) {
            var p = row.split("\t");
            boolean axisX = p[2].equals("x");
            double center = Double.parseDouble(p[3]);
            double start = Double.parseDouble(p[4]);
            double end = Double.parseDouble(p[5]);
            double low = Double.parseDouble(p[6]);
            double high = Double.parseDouble(p[7]);
            int risers = Integer.parseInt(p[8]);
            for (int i = 0; i < risers; i++) {
                double t = (i + .5) / risers;
                double along = start + (end - start) * t;
                double pitch = low + (high - low) * t;
                for (double side : new double[]{center - .30, center, center + .30}) {
                    double x = axisX ? along : side;
                    double z = axisX ? side : along;
                    var floor = physics.support(x, z, pitch, .21);
                    var oldFloor = before.support(x, z, pitch, .21);
                    require(floor != null && oldFloor != null && Math.abs(floor.y() - oldFloor.y()) < .001,
                        "Tread lost its original datum " + p[1] + " at " + x + ", " + z);
                    for (double body : new double[]{HunterPosture.STANDING_HEIGHT, HunterPosture.CROUCH_HEIGHT}) {
                        require(map.traversable(x, floor.y(), z, body), "Stair lost usable width " + p[1] + " at " + x + ", " + z);
                        treadSamples++;
                    }
                }
            }
        }
        int stairPasses = 0;
        for (String id : List.of("L16", "L19", "L21")) {
            var points = data.paths().get(id);
            var reverse = new ArrayList<>(points);
            Collections.reverse(reverse);
            for (double height : new double[]{HunterPosture.STANDING_HEIGHT, HunterPosture.CROUCH_HEIGHT}) {
                NativeSchoolInteriorReview.walk(map, points, height, id + " with rails");
                NativeSchoolInteriorReview.walk(map, reverse, height, id + " with rails reverse");
                stairPasses += 2;
            }
        }
        var captures = packet.resolve(gpu ? "captures/opengl" : "captures/software");
        Files.createDirectories(captures);
        for (var view : VIEWS) capture(map, view, captures.resolve(view.name() + ".png"));
        MonasteryRefugeReview.review(packet, gpu, data);
        var report = new ArrayList<>(List.of("check\tresult", "native_scene_revision\t" + data.nativeScene().revision(),
            "collision_sha256\t" + physics.sha256(), "level_revision\t" + data.level().revision(),
            "isolated_collision_probes\t" + physicalRails, "two_posture_vertical_route_replays\t" + stairPasses,
            "unchanged_tread_width_posture_samples\t" + treadSamples,
            "supported_standing_views\t" + VIEWS.size()));
        if (!gpu) MonasteryDoorReview.live(data, report);
        Files.write(packet.resolve(gpu ? "handrails-opengl.tsv" : "handrails-software.tsv"), report);
        report.forEach(System.out::println);
    }

    private static Vec offset(Vec at, boolean axisX, double offset) {
        return axisX ? new Vec(at.x(), at.y(), at.z() + offset) : new Vec(at.x() + offset, at.y(), at.z());
    }

    private static Vec vec(String[] cells, int index) {
        return new Vec(Double.parseDouble(cells[index]), Double.parseDouble(cells[index + 1]),
            Double.parseDouble(cells[index + 2]));
    }

    private static void capture(HouseMap map, View view, Path image) throws Exception {
        var feet = view.feet();
        var target = view.target();
        require(map.traversable(feet.x(), feet.y(), feet.z()), "Unsupported standing view " + view.name());
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
        NativeSchoolInteriorReview.drawFrame(renderer, map, image);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
