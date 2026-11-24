package org.beckenburg;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import org.beckenburg.LevelDefinition.Vec;
import org.beckenburg.CemeteryNativeLevel.Story;
import org.beckenburg.NativeSchoolInteriorReview.Peer;

/** Confirms the placed registers can be approached and read through the game. */
final class CemeteryFurnishingsReview {

    private CemeteryFurnishingsReview() {}

    static void audit(HouseMap map,NativeInvestigationScene scene,List<Story> stories,Path captures,
                      List<String> report,boolean annex,boolean gpu)throws Exception {
        require(stories.size()==(annex?2:1),"Incorrect number of physical case records");
        var archive=map.level().socket("ritual",6);
        require(archive!=null,"Lost the archive ritual station");
        for(int x=-4;x<=4;x++) for(int z=-4;z<=4;z++) {
            for(double height:new double[]{HunterPosture.STANDING_HEIGHT,HunterPosture.CROUCH_HEIGHT}) {
                require(map.traversable(archive.x()+x*.50,archive.y(),archive.z()+z*.50,height),
                    "Furnishings blocked the archive ritual square at offset "+x+" / "+z);
            }
        }
        report.add("archive_ritual_clearance\t81 supported positions in both postures");
        if(annex) {
            for(double height:new double[]{HunterPosture.STANDING_HEIGHT,HunterPosture.CROUCH_HEIGHT}) {
                NativeSchoolInteriorReview.walk(map,List.of(map.spawn(9),new Vec(66.8,0,43.4)),
                    height,"viewing-room aisle between the seats and bier");
                NativeSchoolInteriorReview.walk(map,List.of(new Vec(78,0,45),new Vec(79.3,0,45.2)),
                    height,"preparation table working side");
                NativeSchoolInteriorReview.walk(map,List.of(map.spawn(11),new Vec(81.1,0,54.6)),
                    height,"cold drawer approach");
                NativeSchoolInteriorReview.walk(map,List.of(map.spawn(10),new Vec(68.15,0,71.8)),
                    height,"receiving trolley approach");
            }
            report.add("mortuary_furnished_working_aisles\tPASS viewing, preparation, cold storage and receiving");
        }
        for(var story:stories) {
            var route=List.of(map.spawn(story.owner()),story.reading());
            for(double height:new double[]{HunterPosture.STANDING_HEIGHT,HunterPosture.CROUCH_HEIGHT}) {
                NativeSchoolInteriorReview.walk(map,route,height,story.id());
                NativeSchoolInteriorReview.walk(map,List.of(story.reading(),map.spawn(story.owner())),height,story.id()+" return");
            }
            focus(map,0,story,story.reading());
            capture(map,story,captures);
            report.add("standing_aim_"+story.id()+"\tPASS");
        }
        if(!gpu)live(map.site(),map.level(),scene,stories,report);
    }

    private static void capture(HouseMap map,Story story,Path captures)throws Exception {
        var solver=new SceneRenderer();
        solver.setRenderSize(880,580);
        solver.setPsxPreset(PsxPipeline.Preset.OFF);
        solver.roomLights=new RoomLighting();
        var at=story.reading();
        solver.x=at.x();
        solver.z=at.z();
        solver.eyeHeight=at.y()+HunterPosture.STANDING_EYE;
        solver.yaw=yaw(story,at);
        solver.pitch=pitch(story,at);
        solver.lamp=true;
        solver.setLampHardware(EquipmentTiers.Tier.III,false);
        NativeSchoolInteriorReview.drawFrame(solver,map,captures.resolve("cemetery-desk-"+story.id()+".png"));
        var sheet=new BufferedImage(880,580,BufferedImage.TYPE_INT_RGB);
        var graphics=sheet.createGraphics();
        try { SiteStories.draw(graphics,880,580,map,story.id()); }
        finally { graphics.dispose(); }
        require(ImageIO.write(sheet,"png",captures.resolve("cemetery-record-"+story.id()+".png").toFile()),
                "No Cemetery record image encoder");
    }

    private static void live(HouseMap.Site site,LevelDefinition level,NativeInvestigationScene scene,List<Story> stories,
                             List<String> report)throws Exception {
        var map=site==HouseMap.Site.USER?new HouseMap(site,level,null,scene):HouseMap.forNetworkRevision(site,level.revision());
        try(var host=new LanHost("127.0.0.1",0,GhostIdentity.Type.SPIRIT,CustomContract.standard(3),map);
            var archive=new Peer(host,false,"Memorial reader");
            var reception=new Peer(host,false,"Registry reader");
            var witness=new Peer(host,false,"Cemetery witness");
            var ghost=new Peer(host,true,"Cemetery ghost")) {
            for(var peer:List.of(archive,reception,witness,ghost)) {
                peer.awaitExact("SITE|"+map.site()+"|"+level.revision());
                peer.client.ready();
            }
            archive.awaitExact("LOBBY|3|HUMAN|4|WAITING");
            archive.client.start();
            archive.await("INFO|mission started");
            var readers=List.of(archive,reception);
            for(int seat=0;seat<stories.size();seat++) {
                var peer=readers.get(seat);
                var story=stories.get(seat);
                for(int room:NativeSchoolInteriorReview.missionRoute(map,story.owner())) {
                    peer.client.move(room);
                    peer.awaitExact("ACCEPT|MOVE|"+room);
                }
                peer.client.view3D();
                peer.awaitExact("ACCEPT|VIEW3D");
                var at=NativeSchoolInteriorReview.body(peer.await("BODY|"+seat+"|"));
                at=CemeteryRefugeReview.walk(peer,seat,at,story.reading());
                var target=focus(map,seat,story,at);
                double yaw=yaw(story,at);
                peer.client.turn(Math.floorMod((int)Math.round(Math.toDegrees(yaw)),360));
                peer.client.interact(target,yaw,pitch(story,at));
                peer.awaitExact("INTERACTED|INTERFACE|STORY_"+story.id());
                peer.awaitExact("ACCEPT|INTERACT|"+target.name());
                report.add("four_peer_record_"+story.id()+"\tPASS");
            }
        }
    }

    private static InteractionFocus.Target focus(HouseMap map,int seat,Story story,Vec at) {
        var observer=new LooseClutter.Observer(seat,at.x(),at.y(),at.z(),yaw(story,at),pitch(story,at),true);
        var aim=InteractionFocus.pick(map,observer,List.of());
        require(aim!=null&&aim.kind()==InteractionFocus.Kind.INTERFACE&&aim.id().equals("STORY_"+story.id()),
                "Expected reachable physical register "+story.id()+" at "+at+" / "+aim);
        return aim;
    }

    private static double yaw(Story story,Vec at) {
        return Math.atan2(story.contact().x()-at.x(),story.contact().z()-at.z());
    }

    private static double pitch(Story story,Vec at) {
        double distance=Math.hypot(story.contact().x()-at.x(),story.contact().z()-at.z());
        return Math.atan2(story.contact().y()-at.y()-HunterPosture.STANDING_EYE,distance);
    }

    private static void require(boolean test,String message) {
        if(!test)throw new AssertionError(message);
    }
}
