package org.beckenburg;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.beckenburg.LevelDefinition.Vec;
import org.beckenburg.NativeSchoolInteriorReview.Peer;

final class NorthwatchFurnishingReview {
    record Record(String id,int owner,Vec feet,Vec contact) {}
    private NorthwatchFurnishingReview() {}

    static void audit(HouseMap map,Map<String,List<Vec>> routes,List<Record> stories,Path captures,
                      List<String> report,boolean gpu)throws Exception {
        require(stories.size()==2,"Both station documents must bind to their fitted papers");
        for(var document:stories) {
            var path=routes.get(routeKey(document));
            require(path.get(path.size()-1).equals(document.feet()),"Route stops before the reading stance");
            require(map.roomAt(document.feet().x(),document.feet().y(),document.feet().z())==document.owner(),
                    "Reading stance in wrong room for "+document.id());
            var target=focus(map,document,document.feet(),HunterPosture.STANDING_EYE);
            require(target.name().equals(SiteStories.document(map.level().id(),document.id()).title()),
                    "The readable overlay and aimed prompt use different records");
            focus(map,document,document.feet(),HunterPosture.CROUCH_EYE);
            capture(map,document,captures,gpu);
            report.add(document.id()+"_standing_and_crouched_focus\tPASS");
        }
    }

    static Vec readShed(HouseMap map,Peer reader,Vec at,Map<String,List<Vec>> routes,List<Record> stories,
                        List<String> report)throws Exception {
        Vec resume=at;
        at=NorthwatchDoorRuntime.walk(reader,at,new Vec(11.9,0,11.03),new Vec(11,0,13));
        at=read(map,reader,at,find(stories,5),routes,report);
        return NorthwatchDoorRuntime.walk(reader,at,new Vec(5.1,0,16),new Vec(7,0,16),
                new Vec(11,0,16),new Vec(11,0,13),new Vec(11.9,0,11.03),resume);
    }

    static Vec readRecords(HouseMap map,Peer reader,Vec at,Map<String,List<Vec>> routes,List<Record> stories,
                           List<String> report)throws Exception {
        Vec resume=at;
        at=NorthwatchDoorRuntime.walk(reader,at,new Vec(33.44,0,18.4));
        at=read(map,reader,at,find(stories,4),routes,report);
        return NorthwatchDoorRuntime.walk(reader,at,new Vec(38,0,16.10),new Vec(38,0,19),
                new Vec(33.44,0,18.4),resume);
    }

    private static Vec read(HouseMap map,Peer reader,Vec at,Record story,Map<String,List<Vec>> routes,
                            List<String> report)throws Exception {
        for(var point:routes.get(routeKey(story)))at=CemeteryRefugeReview.walk(reader,0,at,point);
        var target=focus(map,story,at,HunterPosture.STANDING_EYE);
        double yaw=yaw(story,at);
        reader.client.turn(Math.floorMod((int)Math.round(Math.toDegrees(yaw)),360));
        reader.client.interact(target,yaw,pitch(story,at,HunterPosture.STANDING_EYE));
        reader.awaitExact("INTERACTED|INTERFACE|STORY_"+story.id());
        reader.awaitExact("ACCEPT|INTERACT|"+target.name());
        report.add(story.id()+"_live_reading\tPASS real approach and host-authorized read");
        return at;
    }

    private static void capture(HouseMap map,Record story,Path captures,boolean gpu)throws Exception {
        var render=new SceneRenderer();
        render.setRenderSize(880,580);
        render.setPsxPreset(PsxPipeline.Preset.OFF);
        render.roomLights=new RoomLighting();
        Vec at=story.feet();
        render.x=at.x();
        render.z=at.z();
        render.eyeHeight=at.y()+HunterPosture.STANDING_EYE;
        render.yaw=yaw(story,at);
        render.pitch=pitch(story,at,HunterPosture.STANDING_EYE);
        render.lamp=true;
        render.setLampHardware(EquipmentTiers.Tier.III,false);
        NativeSchoolInteriorReview.drawFrame(render,map,captures.resolve("northwatch-desk-"+story.id()+".png"));
        if(!gpu){
            var image=new BufferedImage(880,580,BufferedImage.TYPE_INT_RGB);
            var canvas=image.createGraphics();
            try{SiteStories.draw(canvas,880,580,map,story.id());}
            finally{canvas.dispose();}
            require(ImageIO.write(image,"png",captures.resolve("northwatch-record-"+story.id()+".png").toFile()),
                    "No record PNG encoder");
        }
    }

    private static InteractionFocus.Target focus(HouseMap map,Record story,Vec feet,double eye) {
        var look=new LooseClutter.Observer(0,feet.x(),feet.y(),feet.z(),yaw(story,feet),pitch(story,feet,eye),true,eye);
        var target=InteractionFocus.pick(map,look,List.of());
        require(target!=null&&target.kind()==InteractionFocus.Kind.INTERFACE&&target.id().equals("STORY_"+story.id()),
                "Reading aim missed "+story.id()+" at "+feet+" / "+target);
        return target;
    }

    private static String routeKey(Record story) {
        return "Northwatch-switch-"+story.id().toLowerCase(java.util.Locale.ROOT).replace('_','-');
    }

    private static Record find(List<Record> stories,int owner) {
        return stories.stream().filter(story->story.owner()==owner).findFirst().orElseThrow();
    }

    private static double yaw(Record story,Vec at) {
        return Math.atan2(story.contact().x()-at.x(),story.contact().z()-at.z());
    }

    private static double pitch(Record story,Vec at,double eye) {
        double range=Math.hypot(story.contact().x()-at.x(),story.contact().z()-at.z());
        return Math.atan2(story.contact().y()-at.y()-eye,range);
    }

    private static void require(boolean pass,String message) {
        if(!pass)throw new AssertionError(message);
    }
}
