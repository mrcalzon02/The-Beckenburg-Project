package org.beckenburg;

import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.beckenburg.LevelDefinition.*;

/** Actual source/occlusion/ownership/audio/photo negatives for the authored train sequence. */
public final class HouseEncounterTest {
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static LooseClutter.Observer study(HouseMap map,int seat){
        var toy=map.clutter().authoredItem("wooden-train");
        for(double radius:new double[]{1.05,1.35,1.65})for(int i=0;i<96;i++){
            double angle=i*Math.PI/48,x=toy.x()+Math.sin(angle)*radius,z=toy.z()+Math.cos(angle)*radius,y=map.groundHeight(x,z,7);
            if(map.roomAt(x,y,z)!=7||!map.traversable(x,y,z))continue;
            var observer=new LooseClutter.Observer(seat,x,y,z,Math.atan2(toy.x()-x,toy.z()-z),Math.atan2(toy.y()-y-1.62,radius),true);
            var aimed=map.clutter().aimed(observer);if(aimed!=null&&aimed.id()==toy.id())return observer;
        }throw new AssertionError("No actual supported train approach");
    }
    static LooseClutter.Observer away(HouseMap map,int seat){var p=map.spawn(1);return new LooseClutter.Observer(seat,p.x(),p.y(),p.z(),0,0,true);}
    static void notice(HouseEncounter event,LooseClutter.Observer observer){
        for(int tick=HouseEncounter.EARLIEST;tick<HouseEncounter.EARLIEST+HouseEncounter.NOTICE_TICKS;tick++)check(event.advance(tick,List.of(observer))==null,"Notice emits no cue or pose");
        check(event.phase()==HouseEncounter.Phase.NOTICED,"Actual desk visit arms the sequence");
    }
    static HouseEncounter.Sound leave(HouseEncounter event,LooseClutter.Observer observer,int start){
        HouseEncounter.Sound sound=null;for(int tick=start;tick<start+HouseEncounter.EMPTY_TICKS;tick++){
            var next=event.advance(tick,List.of(observer));if(next!=null){check(sound==null,"One cue");sound=next;}
        }return sound;
    }
    private static void render(SceneRenderer renderer,HouseMap map,LooseClutter.Observer observer,Path path)throws Exception{
        renderer.setRenderSize(1000,625);renderer.lamp=true;renderer.roomLights=new RoomLighting();
        renderer.x=observer.x();renderer.z=observer.z();renderer.eyeHeight=observer.eyeY();renderer.yaw=observer.yaw();renderer.pitch=observer.pitch();
        renderer.drawPractice(map,List.of(),0);ImageIO.write(renderer.image(),"png",path.toFile());
    }
    static void support(HouseMap map){
        var toy=map.clutter().authoredItem("wooden-train");var desk=map.level().props().stream().filter(p->p.room()==7&&p.mesh().equals("level_student_desk")).findFirst().orElseThrow();
        double bottom=SceneRenderer.castleBounds(toy.kind().mesh())[1];int feet=0;
        for(var t:GlbProps.model(toy.kind().mesh()).triangles())for(var v:List.of(t.a(),t.b(),t.c()))if(v.y()<bottom+.003){
            double x=toy.x()+v.x()*Math.cos(toy.yaw())-v.z()*Math.sin(toy.yaw()),z=toy.z()+v.x()*Math.sin(toy.yaw())+v.z()*Math.cos(toy.yaw());
            check(Math.abs(HouseRebuildStory.surface(desk,x,z)-(toy.y()-toy.kind().half+v.y()))<.012,"Actual turned source foot loses desk support");feet++;
        }check(feet>0,"Independent native source feet inspected");
    }
    public static void main(String[] args)throws Exception{
        var map=HouseRebuildFurnishings.map();var event=new HouseEncounter(map);var near=study(map,0);var far=away(map,0);
        var before=map.clutter().authoredItem("wooden-train");var colliders=map.level().colliders();var hiding=HidingBudget.candidates(map.level());
        for(int tick=0;tick<HouseEncounter.EARLIEST;tick++)check(event.advance(tick,List.of(near))==null,"No early round trigger");
        check(event.phase()==HouseEncounter.Phase.UNNOTICED,"Early proximity cannot arm");notice(event,near);
        for(int tick=120;tick<140;tick++)check(event.advance(tick,List.of(near))==null,"Occupied room never turns");
        var portal=map.level().portals().stream().filter(p->p.door()&&p.joins(5,7)).findFirst().orElseThrow();if(!map.doorOpen(portal.id()))map.toggleDoor(portal.id());
        var hall=new LooseClutter.Observer(1,18.35,2.8,13.95,0,0,false);
        check(map.traversable(hall.x(),hall.y(),hall.z())&&map.roomAt(hall.x(),hall.y(),hall.z())==5&&event.visible(hall,before),"Actual hall observer can see the train through open doorway");
        for(int tick=140;tick<160;tick++)check(event.advance(tick,List.of(far,hall))==null,"Even a hidden remote-room observer's visible corners block the change");
        check(!event.visible(far,before),"Actual lower-floor wall/deck occludes owner");
        SceneRenderer renderer=null;Path out=null;
        if(args.length>0){out=Path.of(args[0]);Files.createDirectories(out);renderer=new SceneRenderer();render(renderer,map,near,out.resolve("train-before-runtime.png"));}
        var sound=leave(event,far,160);check(sound!=null&&sound.cue()==LanSound.Cue.TOY_WINDUP&&sound.room()==7,"One original authored cue");
        var after=map.clutter().authoredItem("wooden-train");
        check(after.x()==before.x()&&after.y()==before.y()&&after.z()==before.z()&&after.holder()==-1&&Math.abs(after.yaw()-before.yaw()-Math.PI/2)<1e-10,"Rigid supported turn changes yaw only");support(map);
        check(map.level().colliders().equals(colliders)&&HidingBudget.candidates(map.level()).equals(hiding),"No route geometry or extra hiding sockets");
        for(int tick=175;tick<220;tick++)check(event.advance(tick,List.of(near))==null,"Returning observer never replays the sequence");
        check(event.advance(120,List.of(near))==null&&event.phase()==HouseEncounter.Phase.TURNED,"Stale clock cannot reset it");
        if(renderer!=null){
            render(renderer,map,near,out.resolve("train-after-runtime.png"));
            var pcm=LanAudio.synthesize(new LanSound.CueEvent(LanSound.Cue.TOY_WINDUP,0,80,175));
            var format=new javax.sound.sampled.AudioFormat(LanAudio.SAMPLE_RATE,16,2,true,false);
            try(var stream=new javax.sound.sampled.AudioInputStream(new java.io.ByteArrayInputStream(pcm),format,pcm.length/4)){
                javax.sound.sampled.AudioSystem.write(stream,javax.sound.sampled.AudioFileFormat.Type.WAVE,out.resolve("original-wooden-train-windup.wav").toFile());
            }
            var a=ImageIO.read(out.resolve("train-before-runtime.png").toFile());var b=ImageIO.read(out.resolve("train-after-runtime.png").toFile());
            check(!Arrays.equals(a.getRGB(0,0,a.getWidth(),a.getHeight(),null,0,a.getWidth()),b.getRGB(0,0,b.getWidth(),b.getHeight(),null,0,b.getWidth())),"Actual renderer must show the changed source pose");
        }
        for(int mode=0;mode<4;mode++){
            var negative=HouseRebuildFurnishings.map();var controller=new HouseEncounter(negative);var observer=study(negative,0);notice(controller,observer);
            if(mode==0){var toy=negative.clutter().authoredItem("wooden-train");negative.clutter().action("TAKE",toy.id(),observer,120);check(controller.advance(120,List.of(away(negative,0)))==null&&negative.clutter().held(0)==toy.id(),"Script cannot seize a held toy");}
            if(mode==1)controller.advance(120,List.of()); // owner death/disconnect
            if(mode==2)controller.advance(120+HouseEncounter.TIMEOUT,List.of(observer));
            if(mode==3)controller.cancel();
            check(controller.phase()==HouseEncounter.Phase.ABORTED,"Handling/disconnect/timeout/mission end cleanly aborts mode="+mode);
        }
        check(new HouseEncounter(new HouseMap(HouseMap.Site.HOUSE,LevelPlans.legacyHouseSource())).phase()==HouseEncounter.Phase.DISABLED,"Legacy House has no invented event");
        check(new HouseEncounter(new HouseMap(HouseMap.Site.USER,HouseRebuildFurnishings.source())).phase()==HouseEncounter.Phase.DISABLED,"User templates do not acquire compulsory location encounters");
        var fresh=HouseRebuildFurnishings.map();check(new HouseEncounter(fresh).phase()==HouseEncounter.Phase.UNNOTICED&&Math.abs(fresh.clutter().authoredItem("wooden-train").yaw()-before.yaw())<1e-10,"Fresh mission resets original source and controller");
        check(LevelTraversalAudit.inspect(map).passed(),"Turned supported toy cannot jam required routes");
        System.out.println("PASS actual authored train trigger/empty-room/full-object occlusion, one-shot/stale/reset/ownership/death/timeout negatives, independent feet/route/cover and real before-after pose");
    }
}
