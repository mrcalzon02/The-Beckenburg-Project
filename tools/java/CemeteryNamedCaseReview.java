package org.beckenburg;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import org.beckenburg.LevelDefinition.Vec;
import org.beckenburg.NativeSchoolInteriorReview.Peer;

/** Exercises the named case through the regular packaged solo and LAN selectors. */
final class CemeteryNamedCaseReview {
    private CemeteryNamedCaseReview() {}

    static void audit(HouseMap map,CemeteryNativeLevel.Source source,List<String> report,boolean gpu)
            throws Exception {
        String id=map.level().id();
        String property=CemeteryNativeLevel.REVIEW_PROPERTY;
        String before=System.getProperty(property);
        try {
            System.clearProperty(property);
            require(!MissionSites.byId("WLD-07").playable()&&!MissionSites.byId("WLD-07-MORTUARY").playable(),
                "Cemetery variants escaped their review gate");
        } finally {
            if(before!=null)System.setProperty(property,before);
        }
        var location=MissionSites.byId(id.toUpperCase(java.util.Locale.ROOT)).location();
        require(location!=null&&List.of(SiteCatalog.soloLocations()).contains(location)&&SiteCatalog.mapFor(location)==map.site()
            &&MissionSites.title(map.site().name()).equals(map.level().title()),"Named Cemetery selection is unavailable");
        var replica=HouseMap.forNetworkRevision(map.site(),map.level().revision());
        require(replica.level().equals(source.level())&&replica.nativeInvestigation().revision().equals(source.scene().revision()),
            "Network selected the wrong Cemetery revision");
        require(MissionSites.campaign().stream().noneMatch(e->e.id().equals("WLD-07-MORTUARY")),
            "Additive mortuary inserted a campaign chapter");
        if(!gpu) {
            solo(map.site(),map.level().revision(),report);
            camera(map,report);
            replay(replica,source.stories().get(0));
            report.add("normal_named_van_to_archive	PASS ramp, three aimed doors, west stair and reading");
            for(var type:List.of(GhostIdentity.Type.POLTERGEIST,GhostIdentity.Type.SPECTER,GhostIdentity.Type.SPIRIT))
                rite(map,type,report);
        }
        report.add("named_solo_and_network_revision_match	PASS");
    }

    private static void solo(HouseMap.Site site,String revision,List<String> report)throws Exception {
        var frames=new LinkedBlockingQueue<String>();
        try(var host=new LanHost("127.0.0.1",0,GhostIdentity.Type.SPIRIT,CustomContract.standard(1),false,true,site);
            var hunter=new LanClient("127.0.0.1",host.port(),false,"Solo investigator","",
                host.localOwnerCredential(),frames::offer)) {
            await(frames,"SITE|"+site+"|"+revision);
            hunter.ready();
            await(frames,"LOBBY|1|AI|1|WAITING");
            hunter.start();
            await(frames,"INFO|mission started");
            hunter.view3D();
            await(frames,"ACCEPT|VIEW3D");
            var map=HouseMap.forNetworkRevision(site,revision);
            Vec at=soloBody(frames);
            require(map.roomAt(at.x(),at.y(),at.z())==14,"Solo did not start inside the shared van");
            for(int i=0;i<15;i++)at=soloStep(hunter,frames,-100);
            require(map.roomAt(at.x(),at.y(),at.z())==13,"Solo could not descend the actual ramp");
            for(int i=0;i<15;i++)at=soloStep(hunter,frames,100);
            require(map.roomAt(at.x(),at.y(),at.z())==14,"Solo return missed the cargo floor");
            hunter.vanDoor();
            await(frames,"ACCEPT|VANDOOR|CLOSED");
            await(frames,"RESULT|RETREATED");
        }
        report.add("solo_ai_named_van_retreat	PASS");
    }

    private static Vec soloStep(LanClient hunter,LinkedBlockingQueue<String> frames,int dz)throws Exception {
        Thread.sleep(125);
        hunter.step(0,dz);
        await(frames,"ACCEPT|STEP");
        return soloBody(frames);
    }

    private static Vec soloBody(LinkedBlockingQueue<String> frames)throws Exception {
        return NativeSchoolInteriorReview.body(await(frames,"BODY|0|"));
    }

    private static String await(LinkedBlockingQueue<String> frames,String prefix)throws Exception {
        long stop=System.nanoTime()+5_000_000_000L;
        while(System.nanoTime()<stop) {
            String frame=frames.poll(100,java.util.concurrent.TimeUnit.MILLISECONDS);
            if(frame==null)continue;
            if(frame.startsWith(prefix))return frame;
            if(frame.startsWith("REJECT|"))throw new AssertionError("Expected "+prefix+" but got "+frame);
        }
        throw new AssertionError("Solo host did not send "+prefix);
    }

    private static void camera(HouseMap map,List<String> report)throws Exception {
        var fresh=HouseMap.forNetworkRevision(map.site(),map.level().revision());
        try(var host=new LanHost("127.0.0.1",0,GhostIdentity.Type.SPIRIT,CustomContract.standard(3),false,false,map.site());
            var field=new Peer(host,false,"Chapel camera");
            var van=new Peer(host,false,"Van camera observer");
            var witness=new Peer(host,false,"Witness");
            var ghost=new Peer(host,true,"Ghost")) {
            for(var peer:List.of(field,van,witness,ghost)) {
                peer.awaitExact("SITE|"+map.site()+"|"+map.level().revision());
                peer.client.ready();
            }
            field.awaitExact("LOBBY|3|HUMAN|4|WAITING");
            field.client.equipAt(0,"VIDEO_CAMERA");
            field.awaitExact("ACCEPT|GEAREQUIP|0|VIDEO_CAMERA");
            field.client.start();
            field.await("INFO|mission started");
            for(int room:NativeSchoolInteriorReview.missionRoute(map,0)) {
                field.client.move(room);
                field.awaitExact("ACCEPT|MOVE|"+room);
            }
            field.client.view3D();
            field.awaitExact("ACCEPT|VIEW3D");
            var feet=NativeSchoolInteriorReview.body(field.await("BODY|0|"));
            feet=CemeteryRefugeReview.walk(field,0,feet,new Vec(22.0,0,47));
            var camera=new LanVideoCameras.Camera(0,0,feet.x(),feet.y(),feet.z(),90,EquipmentTiers.Tier.I);
            var inside=new LanGhostSpatial.Pose(0,23.17,0,feet.z(),0);
            int outsideOwner=fresh.roomAt(25,0,feet.z());
            var outside=new LanGhostSpatial.Pose(outsideOwner,25,0,feet.z(),0);
            require(outsideOwner>=0&&outsideOwner!=0&&LanCameraSight.sees(fresh,camera,inside,true,GhostIdentity.Type.SPIRIT,"a")
                &&!LanCameraSight.sees(fresh,camera,outside,true,GhostIdentity.Type.SPIRIT,"a"),
                "Chapel wall did not separate two targets along the camera axis");
            field.client.turn(90);
            field.client.useHeldEquipment();
            String placed=field.await("CAMERA|PLACE|0|0|");
            field.awaitExact("CAMERA|POWER|0|1");
            field.await("ACCEPT|GEARUSE|VIDEO_CAMERA|");
            van.awaitExact(placed);
            van.awaitExact("CAMERA|POWER|0|1");
        }
        report.add("named_chapel_camera	PASS physical placement, blocked exterior view, shared van feed");
    }

    private static void rite(HouseMap map,GhostIdentity.Type type,List<String> report)throws Exception {
        var rite=type.rite();
        var place=RitualSites.site(map,rite);
        try(var host=new LanHost("127.0.0.1",0,type,CustomContract.standard(3),false,false,map.site());
            var investigator=new Peer(host,false,"Investigator");
            var remote=new Peer(host,false,"Van observer");
            var operator=new Peer(host,false,"Ritual operator");
            var ghost=new Peer(host,true,"Ghost")) {
            var peers=List.of(investigator,remote,operator,ghost);
            for(var peer:peers) {
                peer.awaitExact("SITE|"+map.site()+"|"+map.level().revision());
                peer.client.ready();
            }
            investigator.awaitExact("LOBBY|3|HUMAN|4|WAITING");
            investigator.client.start();
            investigator.await("INFO|mission started");
            for(int owner:NativeSchoolInteriorReview.missionRoute(map,4)) {
                investigator.client.move(owner);
                investigator.awaitExact("ACCEPT|MOVE|"+owner);
            }
            if(type==GhostIdentity.Type.POLTERGEIST) {
                ghost.client.ghost(GhostDomain.Action.DISTURB,4);
                ghost.await("ACCEPT|GHOST|DISTURB|");
                investigator.client.scan(LanCase.Tool.EMF);
                int local=emf(investigator.await("ACCEPT|SCAN|READING EMF in 4"));
                remote.client.scan(LanCase.Tool.EMF);
                int van=emf(remote.await("ACCEPT|SCAN|READING EMF in 14"));
                require(local>=8&&van<=7,"Pavilion disturbance appeared as evidence in the van");
                report.add("named_pavilion_emf	"+local+"/10 local, "+van+"/10 in van");
            }
            investigator.client.identify(type);
            investigator.await("ACCEPT|IDENTIFY|");
            investigator.client.ward();
            investigator.await("ACCEPT|WARD|ANCHOR WARD PREPARED");
            String command=switch(rite) {
                case BANISHMENT -> "BANISH";
                case CLEANSING -> "CLEANSE";
                case ABSOLUTION -> "ABSOLVE";
            };
            send(remote.client,rite);
            remote.awaitExact("REJECT|"+command+"|go to the correct ritual installation");
            for(int owner:NativeSchoolInteriorReview.missionRoute(map,place.room())) {
                operator.client.move(owner);
                operator.awaitExact("ACCEPT|MOVE|"+owner);
            }
            operator.client.view3D();
            operator.awaitExact("ACCEPT|VIEW3D");
            Vec at=NativeSchoolInteriorReview.body(operator.await("BODY|2|"));
            at=CemeteryRefugeReview.walk(operator,2,at,new Vec(place.x(),place.y(),place.z()));
            require(place.near(new LanWorld.Body(at.x(),at.y(),at.z(),0,map.roomAt(at.x(),at.y(),at.z()))),
                "Operator did not reach the actual rite station");
            send(operator.client,rite);
            operator.await("ACCEPT|"+command+"|RITUAL STARTED");
            remote.awaitExact("RITUAL|STARTED|"+rite);
            String result=switch(rite) {
                case BANISHMENT -> "BANISHED";
                case CLEANSING -> "CLEANSED";
                case ABSOLUTION -> "ABSOLVED";
            };
            for(var peer:peers)peer.await("RESULT|"+result,8500);
        }
        report.add("named_rite_"+rite+"	PASS actual installation, van refusal and result on all four peers");
    }

    private static void send(LanClient client,GhostIdentity.Rite rite)throws Exception {
        switch(rite) {
            case BANISHMENT -> client.banish();
            case CLEANSING -> client.cleanse();
            case ABSOLUTION -> client.absolve();
        }
    }

    private static int emf(String reading) {
        int start=reading.lastIndexOf("EMF ")+4;
        return Integer.parseInt(reading.substring(start,reading.indexOf('/',start)));
    }

    private static void replay(HouseMap map, CemeteryNativeLevel.Story story)throws Exception {
        try(var host=new LanHost("127.0.0.1",0,GhostIdentity.Type.SPIRIT,CustomContract.standard(3),false,false,map.site());
            var leader=new Peer(host,false,"Van departure");
            var witness=new Peer(host,false,"Archive witness");
            var crew=new Peer(host,false,"Route crew");
            var ghost=new Peer(host,true,"Ghost")) {
            for(var peer:List.of(leader,witness,crew,ghost)){
                peer.awaitExact("SITE|"+map.site()+"|"+map.level().revision());
                peer.client.ready();
            }
            leader.awaitExact("LOBBY|3|HUMAN|4|WAITING");
            leader.client.start();
            leader.await("INFO|mission started");
            leader.client.view3D();
            leader.awaitExact("ACCEPT|VIEW3D");
            Vec feet=NativeSchoolInteriorReview.body(leader.await("BODY|0|"));
            require(map.roomAt(feet.x(),feet.y(),feet.z())==14,"Wrong van spawn");
            for(int i=0;i<15;i++)feet=NativeSchoolInteriorReview.step(leader,0,0,-100);
            feet=walk(leader,feet,new Vec(28,0,72),new Vec(28,0,66),new Vec(23,0,62),new Vec(15,0,62));
            feet=open(map,leader,feet,witness,810,15,56);
            feet=walk(leader,feet,new Vec(15,0,47));
            feet=open(map,leader,feet,witness,811,15,38);
            feet=walk(leader,feet,new Vec(15,0,29),new Vec(11.5,0,24),new Vec(7.5,0,23));
            feet=open(map,leader,feet,witness,814,7.5,20);
            feet=walk(leader,feet,new Vec(7.5,0,16),new Vec(8.5,0,16),new Vec(14.2,-3,16),new Vec(28,-3,16));
            require(map.roomAt(feet.x(),feet.y(),feet.z())==5&&Math.abs(feet.y()+3)<.05,
                    "Stair did not reach the actual vault floor "+feet);
            feet=walk(leader,feet,new Vec(22,-3,12),new Vec(22,-3,6),story.reading());
            require(map.roomAt(feet.x(),feet.y(),feet.z())==6,"Physical approach missed the archive "+feet);
            var at=story.contact();
            double dx=at.x()-feet.x(),dz=at.z()-feet.z();
            double yaw=Math.atan2(dx,dz),pitch=Math.atan2(at.y()-feet.y()-HunterPosture.STANDING_EYE,Math.hypot(dx,dz));
            aim(map,leader,feet,InteractionFocus.Kind.INTERFACE,"STORY_MEMORIAL_REGISTER",yaw,pitch);
            leader.awaitExact("INTERACTED|INTERFACE|STORY_MEMORIAL_REGISTER");
            leader.awaitExact("ACCEPT|INTERACT|Memorial register");
            System.out.println("PASS "+map.level().id()+" van ramp, three aimed doors, west stair, ossuary floor, archive and actual reading");
        }
    }

    private static Vec open(HouseMap map,Peer peer,Vec feet,Peer witness,int id,double x,double z)throws Exception {
        feet=walk(peer,feet,new Vec(x,0,z+.89));
        require(!map.doorOpen(id),"Expected closed passage "+id);
        double yaw=Math.atan2(x-feet.x(),z-feet.z());
        aim(map,peer,feet,InteractionFocus.Kind.DOOR,Integer.toString(id),yaw,-.10);
        peer.awaitExact("ACCEPT|INTERACT|Open door");
        var frame=witness.await("DOORS|");
        while(!List.of(frame.substring(6).split(",")).contains(Integer.toString(id)))frame=witness.await("DOORS|");
        map.applyDoorSnapshot(frame.substring(6));
        require(map.doorOpen(id),"Client snapshot did not open passage "+id);
        return walk(peer,feet,new Vec(x,0,z-.90));
    }

    private static void aim(HouseMap map,Peer peer,Vec feet,InteractionFocus.Kind kind,String id,
                            double yaw,double pitch)throws Exception {
        var target=InteractionFocus.pick(map,new LooseClutter.Observer(0,feet.x(),feet.y(),feet.z(),yaw,pitch,true),List.of());
        require(target!=null&&target.kind()==kind&&target.id().equals(id),"Physical aim missed "+id+" at "+feet+" / "+target);
        peer.client.turn(Math.floorMod((int)Math.round(Math.toDegrees(yaw)),360));
        peer.client.interact(target,yaw,pitch);
    }

    private static Vec walk(Peer peer,Vec feet,Vec... points)throws Exception {
        for(var point:points)feet=CemeteryRefugeReview.walk(peer,0,feet,point);
        return feet;
    }

    private static void require(boolean pass,String message) {
        if(!pass)throw new AssertionError(message);
    }
}
