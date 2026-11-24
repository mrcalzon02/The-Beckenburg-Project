package org.beckenburg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.beckenburg.LevelDefinition.Socket;
import org.beckenburg.LevelDefinition.Vec;
import org.beckenburg.NativeSchoolInteriorReview.Peer;

/** Bounded Lookout case actions on the named packaged map. */
public final class NativeLookoutCaseReview {
    private NativeLookoutCaseReview(){}

    public static void main(String[] args)throws Exception{
        var map=new HouseMap(HouseMap.Site.LOOKOUT);
        require(SiteCatalog.Location.LOOKOUT.playable(),"Enable the named Lookout package review");
        var output=Path.of(args[0]);
        Files.createDirectories(output.getParent());
        var report=new ArrayList<String>();
        report.add("measure\tresult");
        report.add("packaged_revision\t"+map.level().revision());
        documentsAndCamera(map,report);
        retreat(map,report);
        for(var type:List.of(GhostIdentity.Type.POLTERGEIST,GhostIdentity.Type.SPECTER,GhostIdentity.Type.SPIRIT))
            rite(map,type,report);
        Files.write(output,report);
        report.forEach(System.out::println);
    }

    private static void documentsAndCamera(HouseMap map,List<String> report)throws Exception{
        var file=SiteCaseFiles.forSite(map.level().id());
        require(file.reference().equals("BB / WLD-04")&&file.name().equals(map.level().title())
            &&file.conditions().stream().anyMatch(line->line.contains("roadhead shelter")),"Lookout used a generic briefing");
        try(var host=host(GhostIdentity.Type.SPIRIT);
            var ranger=new Peer(host,false,"Ranger room");
            var cabin=new Peer(host,false,"Observation cabin");
            var van=new Peer(host,false,"Camera observer");
            var ghost=new Peer(host,true,"Spirit")){
            start(map,ranger,List.of(ranger,cabin,van,ghost),true);
            var at=arrive(map,ranger,0,4);
            for(int n=0;n<13;n++)at=NativeSchoolInteriorReview.step(ranger,0,-100,0);
            for(int n=0;n<16;n++)at=NativeSchoolInteriorReview.step(ranger,0,0,-100);
            read(map,ranger,0,at,"TRANSMISSION");
            var rangerFeet=at;
            var tall=arrive(map,cabin,1,10);
            for(int n=0;n<5;n++)tall=NativeSchoolInteriorReview.step(cabin,1,-100,0);
            for(int n=0;n<4;n++)tall=NativeSchoolInteriorReview.step(cabin,1,0,-100);
            read(map,cabin,1,tall,"SIGHTING_LOG");

            var target=map.spawn(4);
            int heading=heading(target.x()-at.x(),target.z()-at.z());
            var camera=new LanVideoCameras.Camera(0,4,at.x(),at.y(),at.z(),heading,EquipmentTiers.Tier.I);
            require(LanCameraSight.sees(map,camera,new LanGhostSpatial.Pose(4,target.x(),target.y(),target.z(),0),
                true,GhostIdentity.Type.SPIRIT,"a"),"Camera couldn't see a target on the ranger floor");
            require(!LanCameraSight.sees(map,camera,new LanGhostSpatial.Pose(10,tall.x(),tall.y(),tall.z(),0),
                true,GhostIdentity.Type.SPIRIT,"a"),"Camera saw through the tower floor and intervening walls");
            ranger.client.turn(heading);
            ranger.client.useHeldEquipment();
            String placed=ranger.await("CAMERA|PLACE|0|4|");
            ranger.awaitExact("CAMERA|POWER|0|1");
            ranger.await("ACCEPT|GEARUSE|VIDEO_CAMERA|");
            van.client.view3D();
            van.awaitExact("ACCEPT|VIEW3D");
            van.awaitExact(placed);
            van.awaitExact("CAMERA|POWER|0|1");
            ghost.client.viewGhost3D();
            ghost.awaitExact("ACCEPT|GVIEW3D");
            ghost.await("GPOSE|");
            NativeSchoolInteriorReview.ghostTrip(host,van,ghost);

            var ticket=ranger.client.resumeTicket();
            require(ticket!=null,"No reconnect seat ticket");
            ((java.net.Socket)NativeSchoolInteriorReview.field(ranger.client,"socket")).close();
            cabin.await("PEER|LOST|0");
            try(var restored=new AuthoredLevelNetworkTest.Peer(host.port(),ticket)){
                restored.await("INFO|mission resumed");
                restored.client.view3D();
                restored.awaitExact("ACCEPT|VIEW3D");
                restored.awaitExact("SITE|LOOKOUT|"+map.level().revision());
                restored.awaitExact(placed);
                restored.awaitExact("CAMERA|POWER|0|1");
                restored.awaitExact("LIGHTS|111111111111|0");
                restored.awaitExact("SOURCE|1");
                var feet=NativeSchoolInteriorReview.body(restored.await("BODY|0|"));
                require(feet.equals(rangerFeet),"Reconnect lost the ranger's supported pose "+feet);
            }
            report.add("lookout_records\tBoth physical authored documents opened from ordinary field positions; exact named briefing returned");
            report.add("lookout_camera\tAimed video camera mounted on ranger floor, valid local sight and blocked cross-level sight, proper van feed snapshot");
            report.add("lookout_reconnect\tDirect ticket resume preserved exact supported body, source running, breaker open and active camera feed");
        }
    }

    private static void retreat(HouseMap map,List<String> report)throws Exception{
        try(var host=host(GhostIdentity.Type.SPIRIT);
            var leader=new Peer(host,false,"Ramp crew");
            var outside=new Peer(host,false,"Roadhead crew");
            var observer=new Peer(host,false,"Cargo crew");
            var ghost=new Peer(host,true,"Ghost")){
            start(map,leader,List.of(leader,outside,observer,ghost),false);
            for(var peer:List.of(leader,outside)){
                peer.client.view3D();
                peer.awaitExact("ACCEPT|VIEW3D");
            }
            Vec lead=null,trail=null;
            for(int n=0;n<13;n++){
                lead=NativeSchoolInteriorReview.step(leader,0,0,-100);
                trail=NativeSchoolInteriorReview.step(outside,1,0,-100);
            }
            require(lead!=null&&trail!=null&&map.roomAt(lead.x(),lead.y(),lead.z())==13
                &&map.roomAt(trail.x(),trail.y(),trail.z())==13,"Both ramp operators must reach the actual roadhead");
            leader.client.vanDoor();
            leader.awaitExact("REJECT|van door button out of reach");
            for(int n=0;n<13;n++)lead=NativeSchoolInteriorReview.step(leader,0,0,100);
            require(map.roomAt(lead.x(),lead.y(),lead.z())==14,"Ramp operator missed the cargo floor on return");
            leader.client.vanDoor();
            leader.awaitExact("ACCEPT|VANDOOR|CLOSED");
            observer.awaitExact("EXTRACTION|0|1");
            observer.awaitExact("EXTRACTION|1|0");
            observer.awaitExact("EXTRACTION|2|1");
            observer.await("RESULT|RETREATED");
            require(!host.running(),"Van retreat did not settle the packaged case");
            report.add("lookout_retreat\tPhysical roadhead rejection, cargo return and correct three-hunter extraction roster");
        }
    }

    private static void rite(HouseMap map,GhostIdentity.Type type,List<String> report)throws Exception{
        var rite=type.rite();
        var place=RitualSites.site(map,rite);
        try(var host=host(type);
            var anchor=new Peer(host,false,"Anchor investigator");
            var remote=new Peer(host,false,"Van evidence");
            var witness=new Peer(host,false,"Ritual guard");
            var ghost=new Peer(host,true,"Ghost")){
            start(map,anchor,List.of(anchor,remote,witness,ghost),false);
            arrive(map,anchor,0,4);
            remote.client.view3D();
            remote.awaitExact("ACCEPT|VIEW3D");
            if(type==GhostIdentity.Type.POLTERGEIST){
                ghost.client.ghost(GhostDomain.Action.DISTURB,4);
                ghost.await("ACCEPT|GHOST|DISTURB|");
                anchor.client.scan(LanCase.Tool.EMF);
                int near=emf(anchor.await("ACCEPT|SCAN|READING EMF in 4"));
                remote.client.scan(LanCase.Tool.EMF);
                int far=emf(remote.await("ACCEPT|SCAN|READING EMF in 14"));
                require(near>=8&&far<=7,"Evidence didn't discriminate the actual ranger floor from the van "+near+" / "+far);
                report.add("lookout_spatial_emf\tRanger floor "+near+"/10; remote van "+far+"/10 after a real ghost disturbance");
            }
            anchor.client.identify(type);
            anchor.await("ACCEPT|IDENTIFY|");
            anchor.client.ward();
            anchor.await("ACCEPT|WARD|ANCHOR WARD PREPARED");
            sendRite(remote.client,rite);
            String op=switch(rite){case BANISHMENT->"BANISH";case CLEANSING->"CLEANSE";case ABSOLUTION->"ABSOLVE";};
            remote.awaitExact("REJECT|"+op+"|go to the correct ritual installation");
            var at=arrive(map,witness,2,place.room());
            require(place.near(new LanWorld.Body(at.x(),at.y(),at.z(),0,map.roomAt(at.x(),at.y(),at.z()))),
                "The "+rite+" operator missed their physical mounting");
            sendRite(witness.client,rite);
            witness.await("ACCEPT|"+op+"|RITUAL STARTED");
            remote.awaitExact("RITUAL|STARTED|"+rite);
            String result=switch(rite){case BANISHMENT->"BANISHED";case CLEANSING->"CLEANSED";case ABSOLUTION->"ABSOLVED";};
            for(var peer:List.of(anchor,remote,witness,ghost))peer.await("RESULT|"+result,8500);
            require(!host.running(),"Accepted Lookout rite failed to settle");
            report.add("lookout_"+rite.name().toLowerCase(java.util.Locale.ROOT)
                +"\tVan refusal, anchor ward, "+map.roomName(place.room())+" installation and "+result+" on four peers");
        }
    }

    private static LanHost host(GhostIdentity.Type type)throws Exception{
        return new LanHost("127.0.0.1",0,type,CustomContract.standard(3),false,false,HouseMap.Site.LOOKOUT);
    }

    private static void start(HouseMap map,Peer leader,List<Peer> peers,boolean videoCamera)throws Exception{
        for(var peer:peers){
            peer.awaitExact("SITE|LOOKOUT|"+map.level().revision());
            peer.client.ready();
        }
        leader.awaitExact("LOBBY|3|HUMAN|4|WAITING");
        if(videoCamera){
            leader.client.equipAt(0,"VIDEO_CAMERA");
            leader.awaitExact("ACCEPT|GEAREQUIP|0|VIDEO_CAMERA");
        }
        leader.client.start();
        leader.await("INFO|mission started");
    }

    private static Vec arrive(HouseMap map,Peer hunter,int seat,int owner)throws Exception{
        for(int room:NativeSchoolInteriorReview.missionRoute(map,owner)){
            hunter.client.move(room);
            hunter.awaitExact("ACCEPT|MOVE|"+room);
        }
        hunter.client.view3D();
        hunter.awaitExact("ACCEPT|VIEW3D");
        var at=NativeSchoolInteriorReview.body(hunter.await("BODY|"+seat+"|"));
        require(at.equals(map.spawn(owner)),"Wrong authored spawn in "+owner+": "+at);
        return at;
    }

    private static void read(HouseMap map,Peer hunter,int seat,Vec at,String doc)throws Exception{
        Socket story=map.level().sockets().stream().filter(s->s.type().equals("story")&&s.style().equals(doc)).findFirst().orElseThrow();
        double dx=story.x()-at.x(),dz=story.z()-at.z();
        double yaw=Math.atan2(dx,dz);
        double pitch=Math.atan2(story.y()+.012-at.y()-HunterPosture.STANDING_EYE,Math.hypot(dx,dz));
        var target=InteractionFocus.pick(map,new LooseClutter.Observer(seat,at.x(),at.y(),at.z(),yaw,pitch,true),List.of());
        require(target!=null&&target.kind()==InteractionFocus.Kind.INTERFACE&&target.id().equals("STORY_"+doc),
            "No aimed physical document "+doc+" from "+at+" / "+target);
        hunter.client.turn(heading(dx,dz));
        hunter.client.interact(target,yaw,pitch);
        hunter.awaitExact("INTERACTED|INTERFACE|STORY_"+doc);
    }

    private static int heading(double dx,double dz){
        return Math.floorMod((int)Math.round(Math.toDegrees(Math.atan2(dx,dz))),360);
    }

    private static int emf(String reading){
        int start=reading.lastIndexOf("EMF ")+4;
        return Integer.parseInt(reading.substring(start,reading.indexOf('/',start)));
    }

    private static void sendRite(LanClient client,GhostIdentity.Rite rite)throws Exception{
        switch(rite){case BANISHMENT->client.banish();case CLEANSING->client.cleanse();case ABSOLUTION->client.absolve();}
    }

    private static void require(boolean condition,String reason){
        if(!condition)throw new AssertionError(reason);
    }
}
