package org.beckenburg;

import org.beckenburg.LevelDefinition.Vec;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

/** Verifies the sloping liners and open pipes against the saved native architecture. */
public final class MonasteryValleyReview {
    private record Profile(Vec left, Vec center, Vec right) {}
    private record Outfall(String valley, String side, Vec top, Vec bottom, Vec bed) {}

    private MonasteryValleyReview() {}

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
        for (var name : List.of("runtime-input.tsv", "door-schedule.tsv", "handrail-schedule.tsv")) {
            require(Arrays.equals(Files.readAllBytes(prior.resolve(name)), Files.readAllBytes(packet.resolve(name))),
                "Drainage altered authored entries, treads or room routes " + name);
        }
        var before = MonasteryDoorReview.read(prior, prior, "monastery-with-handrails").nativeScene().physics();
        var data = MonasteryDoorReview.read(packet, packet, "monastery-with-roof-valleys");
        var physics = data.nativeScene().physics();
        var map = data.map();
        require(map.valid() && RitualSites.valid(map), "Drainage changed room, spawn or ritual validity");
        var profiles = new LinkedHashMap<String, List<Profile>>();
        var outfalls = new ArrayList<Outfall>();
        for (var line : Files.readAllLines(packet.resolve("valley-schedule.tsv"))) {
            var p = line.split("\t");
            if (p[0].equals("PROFILE")) {
                profiles.computeIfAbsent(p[1], key -> new ArrayList<>()).add(new Profile(vec(p, 2), vec(p, 5), vec(p, 8)));
            } else {
                require(p[0].equals("OUTFALL"), "Unexpected valley schedule row " + line);
                outfalls.add(new Outfall(p[1], p[2], new Vec(num(p, 3), num(p, 6), num(p, 4)),
                    new Vec(num(p, 3), num(p, 5), num(p, 4)), vec(p, 7)));
            }
        }
        require(profiles.size() == 4 && outfalls.size() == 7, "Valley or outfall omitted");
        int lining = 0;
        int laps = 0;
        for (var entry : profiles.entrySet()) {
            var sections = entry.getValue();
            var peak = entry.getKey().equals("refectory") ? sections.getFirst().center()
                : Collections.max(sections, java.util.Comparator.comparingDouble(p -> p.center().y())).center();
            // Every surveyed station has an uninterrupted fall towards its assigned terminal.
            for (int i = 1; i < sections.size(); i++) {
                var a = sections.get(i - 1);
                var b = sections.get(i);
                var center = mix(a.center(), b.center(), .5);
                double chord = Math.hypot(a.center().x() - b.center().x(), a.center().z() - b.center().z());
                double fall = b.center().y() - a.center().y();
                double expected = center.x() < peak.x() || center.z() < peak.z() ? .0046 : -.0046;
                require(Math.abs(fall / chord - expected) < .00001, "Flat spot or adverse valley fall " + center);
                require(before.clearSight(up(center, .012), up(center, -.012))
                    && !physics.clearSight(up(center, .012), up(center, -.012)), "Liner missing at " + center);
                lining++;
                for (var lap : List.of(mix(a.left(), b.left(), .5), mix(a.right(), b.right(), .5))) {
                    require(lap.y() - center.y() > .055, "Crossfall reverses near lapped slate " + lap);
                    require(!physics.clearSight(up(lap, .032), up(lap, -.06)), "Air gap at lapped slate " + lap);
                    laps++;
                }
            }
        }
        int openings = 0;
        int shoes = 0;
        int beds = 0;
        var captures = packet.resolve(gpu ? "captures/opengl" : "captures/software");
        Files.createDirectories(captures);
        for (var outfall : outfalls) {
            var top = outfall.top();
            var bottom = outfall.bottom();
            for (double[] offset : new double[][]{{0,0},{-.035,0},{.035,0},{0,-.035},{0,.035}}) {
                require(physics.clearSight(new Vec(top.x() + offset[0], top.y() - .018, top.z() + offset[1]),
                    new Vec(bottom.x() + offset[0], bottom.y() + .018, bottom.z() + offset[1])),
                    "Physical downpipe bore is blocked at " + outfall);
                openings++;
            }
            var mid = mix(top, bottom, .5);
            var outside = outfall.bed();
            double dx = (outside.x() - mid.x()) / .29;
            double dz = (outside.z() - mid.z()) / .29;
            require(before.clearSight(mid, new Vec(mid.x() + .14*dx, mid.y(), mid.z() + .14*dz))
                && !physics.clearSight(mid, new Vec(mid.x() + .14*dx, mid.y(), mid.z() + .14*dz)),
                "Native collision lost the pipe shell " + outfall);
            var sections = profiles.get(outfall.valley());
            var near = horizontal(top, sections.getFirst().center()) < horizontal(top, sections.getLast().center())
                ? sections.getFirst().center() : sections.getLast().center();
            for (double t : new double[]{.17,.38,.59,.8}) {
                var shoe = mix(near, up(top, .012), t);
                require(!physics.clearSight(up(shoe, .015), up(shoe, -.018)), "Export lost falling inlet shoe " + shoe);
                shoes++;
            }
            var bed = new Vec(outside.x() - .115*dz, outside.y(), outside.z() + .115*dx);
            require(physics.clearSight(up(bed, .02), up(bed, -.014))
                && !physics.clearSight(up(bed, -.020), up(bed, -.04)), "Soakaway is closed or missing its gravel floor " + bed);
            beds++;
            if (outfall.side().equals("east") || outfall.valley().equals("refectory") ||
                    outfall.valley().equals("dormitory") && outfall.side().equals("south")) {
                double sx = bed.x() + dx*2.65;
                double sz = bed.z() + dz*2.65;
                var support = before.support(sx, sz, bed.y(), 1.5);
                require(support != null, "No supported exterior inspection at " + bed);
                capture(map, new Vec(sx, support.y(), sz), new Vec(top.x(), 1.37, top.z()),
                    captures.resolve(outfall.valley().replace(' ', '-') + "-" + outfall.side() + "-outfall.png"));
            }
        }
        for (var door : data.nativeScene().doors()) {
            var frame = door.spec();
            require(NativeSchoolInteriorReview.sweep(physics, frame), "Drainage obstructs a door sweep " + frame.frameId());
            NativeSchoolInteriorReview.audit(map, frame, false);
            map.toggleDoor(frame.portal().id(), System.nanoTime() - 2_000_000_000L);
            NativeSchoolInteriorReview.audit(map, frame, true);
        }
        int routes = 0;
        for (var entry : data.paths().entrySet()) {
            var reverse = new ArrayList<>(entry.getValue());
            Collections.reverse(reverse);
            for (double height : new double[]{HunterPosture.STANDING_HEIGHT, HunterPosture.CROUCH_HEIGHT}) {
                NativeSchoolInteriorReview.walk(map, entry.getValue(), height, entry.getKey());
                NativeSchoolInteriorReview.walk(map, reverse, height, entry.getKey() + " reverse");
                routes += 2;
            }
        }
        var report = new ArrayList<>(List.of("check\tresult", "native_scene_revision\t" + data.nativeScene().revision(),
            "level_revision\t" + data.level().revision(), "collision_sha256\t" + physics.sha256(),
            "graded_physical_liner_spans\t" + lining, "lapped_slate_edge_checks\t" + laps,
            "open_vertical_bore_rays\t" + openings, "native_inlet_shoe_samples\t" + shoes,
            "open_soakaway_beds\t" + beds, "original_route_replays\t" + routes,
            "retained_fitted_doors\t" + data.nativeScene().doors().size()));
        NativeSchoolInteriorReview.auditHiding(map, report);
        if (!gpu) MonasteryDoorReview.live(data, report);
        Files.write(packet.resolve(gpu ? "valleys-opengl.tsv" : "valleys-software.tsv"), report);
        report.forEach(System.out::println);
    }

    private static double horizontal(Vec a, Vec b) {
        return Math.hypot(a.x() - b.x(), a.z() - b.z());
    }

    private static Vec mix(Vec a, Vec b, double t) {
        return new Vec(a.x() + (b.x() - a.x())*t, a.y() + (b.y() - a.y())*t, a.z() + (b.z() - a.z())*t);
    }

    private static Vec up(Vec a, double dy) { return new Vec(a.x(), a.y() + dy, a.z()); }

    private static Vec vec(String[] p, int i) { return new Vec(num(p, i), num(p, i + 1), num(p, i + 2)); }

    private static double num(String[] p, int index) { return Double.parseDouble(p[index]); }

    private static void capture(HouseMap map, Vec feet, Vec target, Path image) throws Exception {
        var renderer = new SceneRenderer();
        renderer.setRenderSize(960, 640);
        renderer.setPsxPreset(PsxPipeline.Preset.OFF);
        renderer.x = feet.x();
        renderer.z = feet.z();
        renderer.eyeHeight = feet.y() + HunterPosture.STANDING_EYE;
        renderer.yaw = Math.atan2(target.x() - feet.x(), target.z() - feet.z());
        renderer.pitch = Math.atan2(target.y() - renderer.eyeHeight, horizontal(target, feet));
        renderer.weatherModule().setTimeOfDay(.5);
        renderer.lamp = true;
        renderer.setLampHardware(EquipmentTiers.Tier.III, false);
        NativeSchoolInteriorReview.drawFrame(renderer, map, image);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
