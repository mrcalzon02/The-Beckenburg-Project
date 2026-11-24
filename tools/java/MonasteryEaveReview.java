package org.beckenburg;

import org.beckenburg.LevelDefinition.Vec;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

/** Reviews eave gutter falls, open outlets and the approach widths they preserve. */
public final class MonasteryEaveReview {
    private record Channel(Vec apron, Vec floor, Vec rim) {}
    private record Outlet(String run, String name, Vec shaftTop, Vec shaftBottom, Vec bed, Vec inlet, Vec nozzle) {}

    private MonasteryEaveReview() {}

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
        for (String name : List.of("runtime-input.tsv", "door-schedule.tsv", "handrail-schedule.tsv",
                "valley-schedule.tsv", "flashing-schedule.tsv")) {
            require(Arrays.equals(Files.readAllBytes(packet.resolve(name)), Files.readAllBytes(prior.resolve(name))),
                "Eave additions changed an established map schedule " + name);
        }
        var before = MonasteryDoorReview.read(prior, prior, "monastery-with-roof-flashings").nativeScene().physics();
        var data = MonasteryDoorReview.read(packet, packet, "monastery-with-eave-gutters");
        var physics = data.nativeScene().physics();
        var map = data.map();
        require(map.valid() && RitualSites.valid(map), "Changed room, arrival or rite validity");
        var gutters = new LinkedHashMap<String, List<Channel>>();
        var drains = new ArrayList<Outlet>();
        var laps = new ArrayList<Vec>();
        for (String row : Files.readAllLines(packet.resolve("eave-schedule.tsv"))) {
            var p = row.split("\t");
            switch (p[0]) {
                case "GUTTER" -> gutters.computeIfAbsent(p[1], k -> new ArrayList<>())
                    .add(new Channel(vec(p,4),vec(p,10),vec(p,13)));
                case "OUTLET" -> drains.add(new Outlet(p[1],p[1] + " " + p[2], new Vec(d(p,3),d(p,6),d(p,4)),
                    new Vec(d(p,3),d(p,5),d(p,4)), vec(p,7), vec(p,10), vec(p,13)));
                case "LAP" -> laps.add(mix(vec(p,4),vec(p,7),.53));
                default -> throw new IllegalArgumentException(row);
            }
        }
        require(gutters.size()==5 && drains.size()==10 && laps.size()==29, "Missing roof unit or outlet");
        int channelSpans = 0;
        int outletNotches = 0;
        for (var run : gutters.entrySet()) {
            var profiles = run.getValue();
            for (int i=1;i<profiles.size();i++) {
                var a=profiles.get(i-1);
                var b=profiles.get(i);
                var floor=mix(a.floor(),b.floor(),.4);
                var lap=mix(a.apron(),b.apron(),.4);
                double span=horizontal(a.floor(),b.floor());
                require(Math.abs(Math.abs(a.floor().y()-b.floor().y()) / span - .0045) < .000002,
                    "Gutter segment has no grade to its outlet " + run.getKey() + " " + floor);
                require(before.clearSight(up(floor,.012),up(floor,-.014))
                    && !physics.clearSight(up(floor,.012),up(floor,-.014)), "Native gutter lost its floor " + floor);
                require(!physics.clearSight(up(lap,.018),up(lap,-.05)), "Slate apron lap is unsupported " + lap);
                channelSpans++;
            }
            long lows = profiles.stream().filter(p -> Math.abs(p.floor().y()-p.rim().y())<.000001).count();
            require(lows==2, "Expected two open lateral gutter notches " + run.getKey());
            outletNotches += (int)lows;
        }
        int isolatedLap = 0;
        for (int i=1;i<laps.size();i++) {
            var at=mix(laps.get(i-1),laps.get(i),.4);
            require(!physics.clearSight(up(at,.018),up(at,-.018)), "Archiveâ€“vestry lap missing at " + at);
            if (before.clearSight(up(at,.018),up(at,-.018))) isolatedLap++;
        }
        require(isolatedLap >= 20, "Existing slate was mistaken for the new archiveâ€“vestry step");
        int bores = 0, shoes = 0;
        for (var drain : drains) {
            var top=drain.shaftTop();
            var bottom=drain.shaftBottom();
            var notch=gutters.get(drain.run()).stream()
                .min(java.util.Comparator.comparingDouble(p -> horizontal(p.rim(),drain.inlet())))
                .orElseThrow();
            require(horizontal(notch.rim(),drain.inlet())<.001
                && physics.clearSight(up(notch.floor(),.012),up(drain.inlet(),.012)),
                "Native wall at the opening between gutter and outlet " + drain.name());
            for (double[] p : new double[][]{{0,0},{-.032,0},{.032,0},{0,-.032},{0,.032}}) {
                require(physics.clearSight(new Vec(top.x()+p[0],top.y()-.014,top.z()+p[1]),
                    new Vec(bottom.x()+p[0],bottom.y()+.014,bottom.z()+p[1])), "Pipe bore closed " + drain.name());
                bores++;
            }
            require(drain.inlet().y()>drain.nozzle().y() && drain.nozzle().y()>top.y(), "Adverse inlet fall " + drain.name());
            for (double t : new double[]{.18,.39,.61,.82}) {
                var shoe=mix(drain.inlet(),drain.nozzle(),t);
                require(!physics.clearSight(up(shoe,.015),up(shoe,-.018))
                        && physics.clearSight(up(shoe,.039),up(shoe,.018)),
                    "Inlet shoe missing or blocked " + drain.name() + " at " + shoe);
                shoes++;
            }
            var bed=drain.bed();
            double dx=(bed.x()-top.x())/.30, dz=(bed.z()-top.z())/.30;
            var gravel=new Vec(bed.x()-.11*dz,bed.y(),bed.z()+.11*dx);
            require(physics.clearSight(up(gravel,.023),up(gravel,-.016))
                && !physics.clearSight(up(gravel,-.020),up(gravel,-.041)), "Blocked gravel soakaway " + drain.name());
        }
        for (var door : data.nativeScene().doors()) {
            var frame=door.spec();
            require(NativeSchoolInteriorReview.sweep(physics,frame),"Gutter or drain obstructed " + frame.frameId());
            NativeSchoolInteriorReview.audit(map,frame,false);
            map.toggleDoor(frame.portal().id(),System.nanoTime()-2_000_000_000L);
            NativeSchoolInteriorReview.audit(map,frame,true);
        }
        int paths=0;
        for (var path : data.paths().entrySet()) {
            var reverse=new ArrayList<>(path.getValue());
            Collections.reverse(reverse);
            for (double posture : new double[]{HunterPosture.STANDING_HEIGHT,HunterPosture.CROUCH_HEIGHT}) {
                NativeSchoolInteriorReview.walk(map,path.getValue(),posture,path.getKey());
                NativeSchoolInteriorReview.walk(map,reverse,posture,path.getKey()+" reverse");
                paths+=2;
            }
        }
        var captures=packet.resolve(gpu ? "captures/opengl" : "captures/software");
        Files.createDirectories(captures);
        capture(map,new Vec(35.1,0,65.3),new Vec(33,2.62,62),captures.resolve("public-gate-eave.png"));
        capture(map,new Vec(27.9,0,25.55),new Vec(28.6,2.55,22),captures.resolve("north-herb-court-eave.png"));
        capture(map,new Vec(28.9,0,45.8),new Vec(28.9,2.55,50),captures.resolve("south-herb-court-eave.png"));
        var report=new ArrayList<>(List.of("check\tresult","native_scene_revision\t"+data.nativeScene().revision(),
            "level_revision\t"+data.level().revision(),"collision_sha256\t"+physics.sha256(),
            "renderer\t"+(gpu ? GpuWorldRenderer.active().rendererDescription() : "Software"),
            "native_graded_gutter_spans\t"+channelSpans,"open_gutter_notches\t"+outletNotches,
            "open_vertical_bore_rays\t"+bores,"unobstructed_inlet_shoe_samples\t"+shoes,
            "open_gravel_soakaways\t"+drains.size(),"native_archive_vestry_step_lap_spans\t"+(laps.size()-1),
            "isolated_new_archive_vestry_step_lap_spans\t"+isolatedLap,
            "retained_doors\t"+data.nativeScene().doors().size(),"original_route_replays\t"+paths));
        NativeSchoolInteriorReview.auditHiding(map,report);
        if (!gpu) MonasteryDoorReview.live(data,report);
        Files.write(packet.resolve(gpu ? "eaves-opengl.tsv" : "eaves-software.tsv"),report);
        report.forEach(System.out::println);
    }

    private static double horizontal(Vec a,Vec b) {
        return Math.hypot(a.x()-b.x(),a.z()-b.z());
    }

    private static Vec mix(Vec a,Vec b,double t) {
        return new Vec(a.x()+(b.x()-a.x())*t,a.y()+(b.y()-a.y())*t,a.z()+(b.z()-a.z())*t);
    }

    private static Vec up(Vec a,double h) { return new Vec(a.x(),a.y()+h,a.z()); }

    private static double d(String[] p,int i) { return Double.parseDouble(p[i]); }

    private static Vec vec(String[] p,int i) { return new Vec(d(p,i),d(p,i+1),d(p,i+2)); }

    private static void capture(HouseMap map,Vec feet,Vec target,Path image) throws Exception {
        require(map.traversable(feet.x(),feet.y(),feet.z()),"Standing camera lacks an actual floor " + feet);
        var renderer=new SceneRenderer();
        renderer.setRenderSize(960,640);
        renderer.setPsxPreset(PsxPipeline.Preset.OFF);
        renderer.x=feet.x();
        renderer.z=feet.z();
        renderer.eyeHeight=feet.y()+HunterPosture.STANDING_EYE;
        renderer.yaw=Math.atan2(target.x()-feet.x(),target.z()-feet.z());
        renderer.pitch=Math.atan2(target.y()-renderer.eyeHeight,horizontal(target,feet));
        renderer.weatherModule().setTimeOfDay(.5);
        renderer.lamp=true;
        renderer.setLampHardware(EquipmentTiers.Tier.III,false);
        NativeSchoolInteriorReview.drawFrame(renderer,map,image);
    }

    private static void require(boolean ok,String failure) {
        if (!ok) throw new AssertionError(failure);
    }
}
