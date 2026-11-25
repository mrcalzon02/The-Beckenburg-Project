package org.beckenburg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.beckenburg.LevelDefinition.Vec;
import org.beckenburg.NativeSchoolInteriorReview.Peer;

/** Bounded camera, evidence, rite and extraction scenarios on the actual named Mine package. */
public final class NativeMineCasePlayReview {
    private NativeMineCasePlayReview(){}

    public static void main(String[] args)throws Exception{
        var entry=MissionSites.byId("WLD-05");
        require(entry.playable()&&entry.location()==SiteCatalog.Location.MINE,"Mine missing from ordinary dispatch");
        var map=new HouseMap(HouseMap.Site.MINE);
        var report=new ArrayList<String>();
        report.add("measure\tresult");
        report.add("packaged_revision\t"+map.level().revision());
        camera(map,report);
        retreat(map,report);
        for(var type:List.of(GhostIdentity.Type.POLTERGEIST,GhostIdentity.Type.SPECTER,GhostIdentity.Type.SPIRIT))
            rite(map,type,report);
        var output=Path.of(args[0]);
        Files.createDirectories(output.getParent());
        Files.write(output,report);
        report.forEach(System.out::println);
    }

    private static void camera(HouseMap map,List<String> report)throws Exception{
        try(var host=host(GhostIdentity.Type.SPIRIT);
            var investigator=new Peer(host,false,"Mine field camera");
            var second=new Peer(host,false,"Mine working crew");
            var observer=new Peer(host,false,"Mine van monitor");
            var ghost=new Peer(host,true,"Spirit")){
            start(map,investigator,List.of(investigator,second,observer,ghost),true);
            var at=arrive(map,investigator,0,1);
            investigator.client.turn(180);
            var test=new LanVideoCameras.Camera(0,1,at.x(),at.y(),at.z(),180,EquipmentTiers.Tier.I);
            require(LanCameraSight.sees(map,test,new LanGhostSpatial.Pose(1,47,0,51,0),true,GhostIdentity.Type.SPIRIT,"a"),
                "Assay camera could not see the same-room target");
            require(!LanCameraSight.sees(map,test,new LanGhostSpatial.Pose(6,47,-6,51,0),true,GhostIdentity.Type.SPIRIT,"a"),
                "Assay camera saw an underground target through the slab");
            investigator.client.useHeldEquipment();
            String placed=investigator.await("CAMERA|PLACE|0|1|");
            investigator.awaitExact("CAMERA|POWER|0|1");
            investigator.await("ACCEPT|GEARUSE|VIDEO_CAMERA|");
            observer.client.view3D();
            observer.awaitExact("ACCEPT|VIEW3D");
            observer.awaitExact(placed);
            observer.awaitExact("CAMERA|POWER|0|1");
            report.add("mine_camera\tReal floor-mounted assay camera, local visibility, blocked cross-storey sight, and van feed replication");
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
            report.add("mine_retreat\tPhysical roadhead rejection, cargo return and correct three-hunter extraction roster");
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
                require(near>=8&&far<=7,"Evidence didn't discriminate the actual haul floor from the van "+near+" / "+far);
                report.add("mine_spatial_emf\tHaul floor "+near+"/10; remote van "+far+"/10 after a real ghost disturbance");
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
            require(!host.running(),"Accepted Mine rite failed to settle");
            report.add("mine_"+rite.name().toLowerCase(java.util.Locale.ROOT)
                +"\tVan refusal, anchor ward, "+map.roomName(place.room())+" installation and "+result+" on four peers");
        }
    }

    private static LanHost host(GhostIdentity.Type type)throws Exception{
        return new LanHost("127.0.0.1",0,type,CustomContract.standard(3),false,false,HouseMap.Site.MINE);
    }

    private static void start(HouseMap map,Peer leader,List<Peer> peers,boolean videoCamera)throws Exception{
        for(var peer:peers){
            peer.awaitExact("SITE|MINE|"+map.level().revision());
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
