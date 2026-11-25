package org.beckenburg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import org.beckenburg.LevelDefinition.Vec;
import org.beckenburg.NativeSchoolInteriorReview.Peer;

/** Plays the archived Northwatch revision through the normal named host constructors. */
final class NorthwatchNamedReview {
    private NorthwatchNamedReview() {}

    public static void main(String[] args)throws Exception {
        boolean gpu=List.of(args).contains("--gpu");
        var output=Path.of(args[0]);
        if(!gpu){review(output,false);return;}
        var desktop=Class.forName("org.beckenburg.gpu.GpuDesktop");
        var start=desktop.getDeclaredMethod("startGpu");
        var stop=desktop.getDeclaredMethod("stopReviewGpu");
        start.setAccessible(true);
        stop.setAccessible(true);
        start.invoke(null);
        try{review(output,true);}
        finally{stop.invoke(null);}
    }

    private static void review(Path output,boolean gpu)throws Exception {
        var report=new ArrayList<String>(List.of("check\tresult"));
        var entry=MissionSites.byId("WLD-08");
        var location=entry.location();
        require(entry.playable()&&List.of(SiteCatalog.soloLocations()).contains(location)
              &&SiteCatalog.mapFor(location)==HouseMap.Site.NORTHWATCH&&SiteCatalog.playable(HouseMap.Site.NORTHWATCH),
              "Named solo/LAN entries must use the same authored scene");
        var campaign=MissionSites.campaign().stream().map(MissionSites.Entry::id).toList();
        require(campaign.lastIndexOf("WLD-08")==16&&campaign.get(15).equals("WLD-07")&&campaign.get(17).equals("WLD-09"),
              "Saved campaign ordinals moved");
        for(String file:List.of("station-with-furnishings.glb","station-with-furnishings.hcs2",
                "runtime-input.tsv","door-schedule.tsv","source-hashes.tsv")){
            var resource=NorthwatchNativeLevel.class.getResource("/org/beckenburg/sites/wld-08/"+file);
            require(resource!=null&&resource.getProtocol().equals("jar"),"Named site payload is outside the review JAR "+file);
        }
        var source=NorthwatchNativeLevel.packaged();
        var map=new HouseMap(SiteCatalog.mapFor(location));
        var network=HouseMap.forNetworkRevision(map.site(),map.level().revision());
        require(map.level().equals(source.level())&&network.level().equals(map.level())&&map.valid()&&RitualSites.valid(map),
                "Game catalog, room graph or rites drifted from the archive");
        require(map.fixtures().states().size()==2&&HidingBudget.candidates(map.level()).size()==3,
                "Archived wash basins or refuges disappeared");
        for(int i=0;i<HouseMap.INDOOR_ROOMS;i++)require(map.level().socket("switch",i)!=null,"Missing circuit "+i);
        try{
            HouseMap.forNetworkRevision(map.site(),"wrong-revision");
            throw new AssertionError("Network accepted a different revision");
        }catch(IllegalArgumentException expected){
            require(expected.getMessage().equals("incompatible level revision"),expected.getMessage());
        }
        report.add("packaged_native_scene\t"+source.scene().revision());
        report.add("named_network_revision\t"+map.level().revision());
        report.add("renderer\t"+(gpu?GpuWorldRenderer.active().rendererDescription():"Software"));
        report.add("named_gate_catalog_campaign_and_exact_revision\tPASS");
        Path captures=output.resolve(gpu?"captures/opengl":"captures");
        Files.createDirectories(captures);
        capture(map,new Vec(37.7,0,72.8),new Vec(32,1.25,75.75),captures.resolve("northwatch-named-arrival.png"));
        capture(map,new Vec(27.8,0,30.8),new Vec(26,1.35,26),captures.resolve("northwatch-named-forecourt.png"));
        if(!gpu){
            solo(map,report);
            namedFieldAndCamera(map,source,report);
            for(var type:List.of(GhostIdentity.Type.POLTERGEIST,GhostIdentity.Type.SPECTER,GhostIdentity.Type.SPIRIT))
                rite(map,type,report);
        }
        Files.writeString(output.resolve(gpu?"named-opengl.tsv":"named-software.tsv"),String.join("\n",report)+"\n");
        report.forEach(System.out::println);
    }

    private static void capture(HouseMap map,Vec feet,Vec target,Path out)throws Exception {
        var view=new SceneRenderer();
        view.setRenderSize(960,640);
        view.setPsxPreset(PsxPipeline.Preset.OFF);
        require(map.traversable(feet.x(),feet.y(),feet.z(),HunterPosture.STANDING_HEIGHT),
                "Named capture must use a supported walking surface");
        view.x=feet.x();
        view.z=feet.z();
        view.eyeHeight=feet.y()+HunterPosture.STANDING_EYE;
        view.yaw=Math.atan2(target.x()-feet.x(),target.z()-feet.z());
        view.pitch=Math.atan2(target.y()-view.eyeHeight,Math.hypot(target.x()-feet.x(),target.z()-feet.z()));
        view.lamp=true;
        view.setLampHardware(EquipmentTiers.Tier.III,false);
        view.roomLights=new RoomLighting();
        NativeSchoolInteriorReview.drawFrame(view,map,out);
    }

    private static void solo(HouseMap map,List<String> report)throws Exception {
        var frames=new LinkedBlockingQueue<String>();
        try(var host=new LanHost("127.0.0.1",0,GhostIdentity.Type.SPIRIT,CustomContract.standard(1),false,true,map.site());
            var hunter=new LanClient("127.0.0.1",host.port(),false,"Northwatch solo","",host.localOwnerCredential(),frames::offer)){
            WharfPowerChecks.await(frames,"SITE|NORTHWATCH|"+map.level().revision());
            hunter.ready();
            WharfPowerChecks.await(frames,"LOBBY|1|AI|1|WAITING");
            hunter.start();
            WharfPowerChecks.await(frames,"INFO|mission started");
            hunter.view3D();
            WharfPowerChecks.await(frames,"ACCEPT|VIEW3D");
            Vec at=NativeSchoolInteriorReview.body(awaitBody(frames));
            require(map.roomAt(at.x(),at.y(),at.z())==14,"Named solo missed cargo spawn");
            for(int i=0;i<15;i++)at=soloStep(hunter,frames,-100);
            require(map.roomAt(at.x(),at.y(),at.z())==13,"Named solo ramp missed apron");
            for(int i=0;i<15;i++)at=soloStep(hunter,frames,100);
            require(map.roomAt(at.x(),at.y(),at.z())==14,"Named solo return missed the cargo floor");
            hunter.vanDoor();
            WharfPowerChecks.await(frames,"ACCEPT|VANDOOR|CLOSED");
            WharfPowerChecks.await(frames,"RESULT|RETREATED");
        }
        report.add("named_solo_ai_cargo_ramp_and_retreat\tPASS");
    }

    private static Vec soloStep(LanClient client,LinkedBlockingQueue<String> frames,int dz)throws Exception {
        for(int attempt=0;attempt<3;attempt++){
            Thread.sleep(attempt==0?135:200);
            client.step(0,dz);
            long deadline=System.nanoTime()+5_000_000_000L;
            while(System.nanoTime()<deadline){
                String frame=frames.poll(100,java.util.concurrent.TimeUnit.MILLISECONDS);
                if(frame==null)continue;
                if(frame.equals("ACCEPT|STEP"))return NativeSchoolInteriorReview.body(awaitBody(frames));
                if(frame.equals("REJECT|movement rate exceeded"))break;
                require(!frame.startsWith("REJECT|"),"Solo step rejected "+frame);
            }
        }
        throw new AssertionError("Solo movement tick did not advance");
    }

    private static String awaitBody(LinkedBlockingQueue<String> frames)throws Exception {
        long stop=System.nanoTime()+5_000_000_000L;
        while(System.nanoTime()<stop){
            var row=frames.poll(100,java.util.concurrent.TimeUnit.MILLISECONDS);
            if(row!=null&&row.startsWith("BODY|0|"))return row;
        }
        throw new AssertionError("Missing real solo body");
    }

    private static void namedFieldAndCamera(HouseMap map,NorthwatchNativeLevel.Source source,List<String> report)throws Exception {
        try(var host=new LanHost("127.0.0.1",0,GhostIdentity.Type.SPIRIT,CustomContract.standard(3),false,false,map.site());
            var lead=new Peer(host,false,"Records investigator");
            var observer=new Peer(host,false,"Northwatch van feed");
            var crew=new Peer(host,false,"Northwatch crew");
            var ghost=new Peer(host,true,"Northwatch ghost")){
            var live=(HouseMap)NativeSchoolInteriorReview.field(host,"map");
            var replica=HouseMap.forNetworkRevision(map.site(),map.level().revision());
            prepare(map,List.of(lead,observer,crew,ghost));
            lead.client.equipAt(0,"VIDEO_CAMERA");
            lead.awaitExact("ACCEPT|GEAREQUIP|0|VIDEO_CAMERA");
            depart(lead);
            lead.client.view3D();
            lead.awaitExact("ACCEPT|VIEW3D");
            var at=NativeSchoolInteriorReview.body(lead.await("BODY|0|"));
            require(live.roomAt(at.x(),at.y(),at.z())==14,"Named LAN missed van spawn");
            for(int i=0;i<15;i++)at=NativeSchoolInteriorReview.step(lead,0,0,-100);
            at=NorthwatchDoorRuntime.walk(lead,at,new Vec(32,0,63),new Vec(32,0,56),new Vec(32,0,41),new Vec(26,0,41));
            at=NorthwatchDoorRuntime.open(live,replica,source.scene(),lead,observer,at,845);
            at=NorthwatchDoorRuntime.walk(lead,at,new Vec(26,0,19),new Vec(31,0,19));
            at=NorthwatchDoorRuntime.open(live,replica,source.scene(),lead,observer,at,846);
            at=NorthwatchDoorRuntime.walk(lead,at,new Vec(38,0,19));
            require(live.roomAt(at.x(),at.y(),at.z())==4,"Named route missed records room");

            var view=new LanVideoCameras.Camera(0,0,at.x(),at.y(),at.z(),90,EquipmentTiers.Tier.I);
            var inside=new LanGhostSpatial.Pose(4,at.x()+2,at.y(),at.z(),0);
            int outer=live.roomAt(44.8,0,at.z());
            var outside=new LanGhostSpatial.Pose(outer,44.8,0,at.z(),0);
            require(outer!=4&&outer>=0&&LanCameraSight.sees(live,view,inside,true,GhostIdentity.Type.SPIRIT,"a")
                    &&!LanCameraSight.sees(live,view,outside,true,GhostIdentity.Type.SPIRIT,"a"),
                    "Records wall did not separate aligned camera targets");
            lead.client.turn(90);
            lead.client.useHeldEquipment();
            String placed=lead.await("CAMERA|PLACE|0|4|");
            lead.awaitExact("CAMERA|POWER|0|1");
            lead.await("ACCEPT|GEARUSE|VIDEO_CAMERA|");
            observer.awaitExact(placed);
            observer.awaitExact("CAMERA|POWER|0|1");
            report.add("named_records_camera_and_van_feed\tPASS real camera placement, wall occlusion and replicated feed");

            var story=source.stories().stream().filter(s->s.id().equals("WINTER_RELAY_LOG")).findFirst().orElseThrow();
            at=NorthwatchDoorRuntime.walk(lead,at,new Vec(38,0,16.1),story.feet());
            double dx=story.contact().x()-at.x(),dz=story.contact().z()-at.z();
            double yaw=Math.atan2(dx,dz),pitch=Math.atan2(story.contact().y()-at.y()-HunterPosture.STANDING_EYE,Math.hypot(dx,dz));
            var focus=InteractionFocus.pick(live,new LooseClutter.Observer(0,at.x(),at.y(),at.z(),yaw,pitch,true),List.of());
            require(focus!=null&&focus.kind()==InteractionFocus.Kind.INTERFACE&&focus.id().equals("STORY_"+story.id()),
                "Named record aim missed "+focus);
            lead.client.turn(Math.floorMod((int)Math.round(Math.toDegrees(yaw)),360));
            lead.client.interact(focus,yaw,pitch);
            lead.awaitExact("INTERACTED|INTERFACE|STORY_"+story.id());
            lead.awaitExact("ACCEPT|INTERACT|"+focus.name());
            report.add("named_van_to_relay_log\tPASS actual ramp, two aimed doors, records desk and reading");
        }
    }

    private static void rite(HouseMap map,GhostIdentity.Type type,List<String> report)throws Exception {
        var rite=type.rite();
        var place=RitualSites.site(map,rite);
        try(var host=new LanHost("127.0.0.1",0,type,CustomContract.standard(3),false,false,map.site());
            var investigator=new Peer(host,false,"Investigator");
            var van=new Peer(host,false,"Van witness");
            var operator=new Peer(host,false,"Rite operator");
            var ghost=new Peer(host,true,"Ghost")){
            var peers=List.of(investigator,van,operator,ghost);
            prepare(map,peers);
            depart(investigator);
            int anchorRoom=4;
            int evidenceRoom=type==GhostIdentity.Type.POLTERGEIST?10:anchorRoom;
            for(int room:NativeSchoolInteriorReview.missionRoute(map,evidenceRoom)){
                investigator.client.move(room);
                investigator.awaitExact("ACCEPT|MOVE|"+room);
            }
            if(type==GhostIdentity.Type.POLTERGEIST){
                ghost.client.ghost(GhostDomain.Action.DRIFT,evidenceRoom);
                ghost.await("ACCEPT|GHOST|DRIFT|");
                ghost.client.ghost(GhostDomain.Action.DISTURB,evidenceRoom);
                ghost.await("ACCEPT|GHOST|DISTURB|");
                investigator.client.scan(LanCase.Tool.EMF);
                int local=emf(investigator.await("ACCEPT|SCAN|READING EMF in "+evidenceRoom));
                van.client.scan(LanCase.Tool.EMF);
                int remote=emf(van.await("ACCEPT|SCAN|READING EMF in 14"));
                require(local>=8&&remote<=7,"Array disturbance leaked to the van "+local+"/"+remote);
                report.add("named_array_emf_locality\t"+local+"/10 at array, "+remote+"/10 in van");
                investigator.client.move(anchorRoom);
                investigator.awaitExact("ACCEPT|MOVE|"+anchorRoom);
            }
            investigator.client.identify(type);
            investigator.await("ACCEPT|IDENTIFY|");
            investigator.client.ward();
            investigator.awaitExact("ACCEPT|WARD|ANCHOR WARD PREPARED");
            String command=switch(rite){
                case BANISHMENT -> "BANISH";
                case CLEANSING -> "CLEANSE";
                case ABSOLUTION -> "ABSOLVE";
            };
            send(van.client,rite);
            van.awaitExact("REJECT|"+command+"|go to the correct ritual installation");
            for(int room:NativeSchoolInteriorReview.missionRoute(map,place.room())){
                operator.client.move(room);
                operator.awaitExact("ACCEPT|MOVE|"+room);
            }
            operator.client.view3D();
            operator.awaitExact("ACCEPT|VIEW3D");
            Vec at=NativeSchoolInteriorReview.body(operator.await("BODY|2|"));
            at=CemeteryRefugeReview.walk(operator,2,at,new Vec(place.x(),place.y(),place.z()));
            require(place.near(new LanWorld.Body(at.x(),at.y(),at.z(),0,map.roomAt(at.x(),at.y(),at.z()))),
                    "Actual rite stance not reached "+rite+" / "+at);
            send(operator.client,rite);
            operator.await("ACCEPT|"+command+"|RITUAL STARTED");
            van.awaitExact("RITUAL|STARTED|"+rite);
            String result=switch(rite){
                case BANISHMENT -> "BANISHED";
                case CLEANSING -> "CLEANSED";
                case ABSOLUTION -> "ABSOLVED";
            };
            for(var peer:peers)peer.await("RESULT|"+result,8500);
            report.add("named_rite_"+rite+"\tPASS fitted installation, van refusal and four-peer result");
        }
    }

    private static void send(LanClient peer,GhostIdentity.Rite rite)throws Exception {
        switch(rite){
            case BANISHMENT -> peer.banish();
            case CLEANSING -> peer.cleanse();
            case ABSOLUTION -> peer.absolve();
        }
    }

    private static int emf(String frame){
        int start=frame.lastIndexOf("EMF ")+4;
        return Integer.parseInt(frame.substring(start,frame.indexOf('/',start)));
    }

    private static void prepare(HouseMap map,List<Peer> peers)throws Exception {
        for(var peer:peers){
            peer.awaitExact("SITE|NORTHWATCH|"+map.level().revision());
            peer.client.ready();
        }
        peers.get(0).awaitExact("LOBBY|3|HUMAN|4|WAITING");
    }

    private static void depart(Peer leader)throws Exception {
        leader.client.start();
        leader.await("INFO|mission started");
    }

    private static void require(boolean condition,String why) {
        if(!condition)throw new AssertionError(why);
    }
}
