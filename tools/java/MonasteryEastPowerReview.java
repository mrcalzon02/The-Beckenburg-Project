package org.beckenburg;

import org.beckenburg.LevelDefinition.Socket;
import org.beckenburg.LevelDefinition.Vec;

import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Checks mounted eastern circuits with the normal game scene and LAN host. */
public final class MonasteryEastPowerReview {
    private MonasteryEastPowerReview() {}

    public static void main(String[] args) throws Exception {
        var packet=Path.of(args[0]);
        if (!List.of(args).contains("--gpu")) {
            review(packet,false);
            return;
        }
        var desktop=Class.forName("org.beckenburg.gpu.GpuDesktop");
        var start=desktop.getDeclaredMethod("startGpu");
        var stop=desktop.getDeclaredMethod("stopReviewGpu");
        start.setAccessible(true);
        stop.setAccessible(true);
        start.invoke(null);
        try { review(packet,true); } finally { stop.invoke(null); }
    }

    private static void review(Path packet,boolean gpu) throws Exception {
        var data=MonasteryPowerReview.read(packet,"monastery-with-east-distribution");
        var map=data.map();
        require(map.valid()&&RitualSites.valid(map),"Installed circuits disturbed ownership or rites");
        var report=new ArrayList<>(List.of("check\tresult","native_scene_revision\t"+data.nativeScene().revision(),
            "collision_sha256\t"+data.nativeScene().physics().sha256(),"level_revision\t"+data.level().revision(),
            "renderer\t"+(gpu?GpuWorldRenderer.active().rendererDescription():"Software")));
        for (var door:data.nativeScene().doors()) {
            var spec=door.spec();
            require(NativeSchoolInteriorReview.sweep(data.nativeScene().physics(),spec),"Conduit obstructed "+spec.frameId());
            NativeSchoolInteriorReview.audit(map,spec,false);
            map.toggleDoor(spec.portal().id(),System.nanoTime()-2_000_000_000L);
            NativeSchoolInteriorReview.audit(map,spec,true);
        }
        int walks=0;
        for (var route:data.paths().entrySet()) {
            var back=new ArrayList<>(route.getValue());
            Collections.reverse(back);
            for (double posture:new double[]{HunterPosture.STANDING_HEIGHT,HunterPosture.CROUCH_HEIGHT}) {
                NativeSchoolInteriorReview.walk(map,route.getValue(),posture,route.getKey());
                NativeSchoolInteriorReview.walk(map,back,posture,route.getKey()+" reverse");
                walks+=2;
            }
        }
        report.add("original_route_replays\t"+walks);
        report.add("retained_doors\t"+data.nativeScene().doors().size());
        var lighting=new RoomLighting();
        var fixture=new FixtureLighting(map);
        var lamps=map.level().sockets().stream().filter(s->s.type().equals("light")).toList();
        require(lamps.size()==12,"Expected twelve retained and added lights");
        for (var lamp:lamps) {
            double on=energy(fixture,lighting,lamp);
            lighting.setSourceRunning(false);
            double noSource=energy(fixture,lighting,lamp);
            lighting.setSourceRunning(true);
            lighting.setBreaker(false);
            double tripped=energy(fixture,lighting,lamp);
            lighting.setBreaker(true);
            lighting.toggle(lamp.room());
            double switched=energy(fixture,lighting,lamp);
            require(on>tripped+.06 && Math.abs(tripped-noSource)<.0001 && Math.abs(tripped-switched)<.0001,
                lamp.id()+" did not follow its switch and main supply "+on+" "+tripped+" "+switched);
            lighting.toggle(lamp.room());
            report.add(lamp.id()+"\t"+on+" supplied / "+tripped+" isolated");
        }
        var captures=packet.resolve(gpu?"captures/opengl":"captures/software");
        Files.createDirectories(captures);
        witness(map,lighting,captures,4,"refectory",new Vec(59,0,40),new Vec(63.8,2.7,45.2),report);
        witness(map,lighting,captures,2,"east-cloister",new Vec(47,0,32.8),new Vec(49.8,2.35,36.2),report);
        NativeSchoolInteriorReview.auditHiding(map,report);
        if (!gpu) MonasteryPowerReview.live(data,report);
        Files.write(packet.resolve(gpu?"east-power-opengl.tsv":"east-power-software.tsv"),report);
        report.forEach(System.out::println);
    }

    private static double energy(FixtureLighting fixture,RoomLighting lighting,Socket lamp) {
        fixture.frame(lighting,0);
        double x=lamp.x()+switch(lamp.room()) { case 11 -> .64; case 6,2 -> -.64; default -> 0; };
        return fixture.sample(lamp.room(),x,lamp.y()-.44,lamp.z()).energy();
    }

    private static void witness(HouseMap map,RoomLighting lighting,Path dir,int owner,String label,Vec at,Vec target,
                                List<String> report) throws Exception {
        require(map.traversable(at.x(),at.y(),at.z()),"Camera lacks standing floor "+at);
        var supplied=dir.resolve(label+"-supplied.png");
        var isolated=dir.resolve(label+"-isolated.png");
        capture(map,lighting,at,target,supplied);
        lighting.toggle(owner);
        capture(map,lighting,at,target,isolated);
        lighting.toggle(owner);
        var a=ImageIO.read(supplied.toFile());
        var b=ImageIO.read(isolated.toFile());
        int change=0;
        long gain=0;
        for (int y=0;y<a.getHeight();y++) for (int x=0;x<a.getWidth();x++) {
            int on=a.getRGB(x,y),off=b.getRGB(x,y);
            if (on!=off) change++;
            for (int sh=0;sh<24;sh+=8) gain+=((on>>sh)&255)-((off>>sh)&255);
        }
        require(change>12000&&gain>0,"Missing fixture contrast in "+label);
        report.add(label+"_visible_light_response\t"+change+" changed pixels");
    }

    private static void capture(HouseMap map,RoomLighting lights,Vec at,Vec target,Path file) throws Exception {
        var r=new SceneRenderer();
        r.setRenderSize(920,620);
        r.setPsxPreset(PsxPipeline.Preset.OFF);
        r.roomLights=lights;
        r.x=at.x();
        r.z=at.z();
        r.eyeHeight=at.y()+HunterPosture.STANDING_EYE;
        r.yaw=Math.atan2(target.x()-at.x(),target.z()-at.z());
        r.pitch=Math.atan2(target.y()-r.eyeHeight,Math.hypot(target.x()-at.x(),target.z()-at.z()));
        r.weatherModule().setTimeOfDay(.08);
        NativeSchoolInteriorReview.drawFrame(r,map,file);
    }

    private static void require(boolean ok,String message) {
        if (!ok) throw new AssertionError(message);
    }
}
