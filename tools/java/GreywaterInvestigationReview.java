package org.beckenburg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.beckenburg.LevelDefinition.Vec;
import org.beckenburg.NativeSchoolInteriorReview.Peer;

/** Walks the ordinary mission from cargo across the Cannery to the mess hall rite. */
final class GreywaterInvestigationReview {
    private static final HouseMap.Site SITE=HouseMap.Site.GREYWATER;
    private static final GhostIdentity.Type TYPE=GhostIdentity.Type.SPIRIT;

    private GreywaterInvestigationReview(){}

    public static void main(String[] args)throws Exception{
        require(SiteCatalog.playable(SITE),"Greywater must be available in the ordinary mission picker");
        var named=new HouseMap(SITE);
        var report=new ArrayList<>(List.of("check\tresult","named_revision\t"+named.level().revision(),
            "collision_sha256\t"+named.nativeInvestigation().physics().sha256()));
        review(named,report);
        Files.write(Path.of(args[0]),report);
    }

    private static void review(HouseMap named,List<String> report)throws Exception{
        var replica=HouseMap.forNetworkRevision(SITE,named.level().revision());
        var contract=new CustomContract(2,100,150,100,100,100,100,100,CustomContract.Sprint.LIMITED,100,true,true);
        try(var host=new LanHost("127.0.0.1",0,TYPE,contract,false,false,SITE);
            var scout=new Peer(host,false,"Cannery investigator");
            var witness=new Peer(host,false,"Cannery witness");
            var ghost=new Peer(host,true,"Cannery Spirit")){
            var map=(HouseMap)NativeSchoolInteriorReview.field(host,"map");
            require(map.nativeInvestigation()==named.nativeInvestigation(),"The host selected a different native settlement");
            for(var peer:List.of(scout,witness,ghost)){
                peer.awaitExact("SITE|"+SITE+"|"+named.level().revision());
                peer.client.ready();
            }
            scout.awaitExact("LOBBY|2|HUMAN|3|WAITING");
            scout.client.start();
            scout.await("INFO|mission started");
            scout.client.view3D();
            scout.awaitExact("ACCEPT|VIEW3D");
            var at=NativeSchoolInteriorReview.body(scout.await("BODY|0|"));
            require(room(map,at)==14,"The case did not begin on the cargo floor");
            for(int i=0;i<15;i++)at=NativeSchoolInteriorReview.step(scout,0,0,-100);
            at=walk(scout,at,new Vec(32,0,76),new Vec(32,0,69),new Vec(36,0,69),new Vec(36,0,57.3));
            at=open(map,replica,scout,witness,at,1002);
            at=walk(scout,at,new Vec(36,0,53),new Vec(27,0,53),new Vec(27,0,51.17),
                new Vec(27,3,42.68),new Vec(27,3,41));
            require(room(map,at)==11&&Math.abs(at.y()-3)<.04,"The built stair missed the gallery "+at);
            at=walk(scout,at,new Vec(27,3,43),new Vec(27,0,51.38),new Vec(27,0,53),
                new Vec(36,0,53),new Vec(36,0,42.35));
            require(room(map,at)==4,"The built stair did not return to processing "+at);
            pass(report,"gallery_stair","Cargo approach, exterior door and the actual switch in floor elevation walked both ways");

            ghost.client.viewGhost3D();
            ghost.awaitExact("ACCEPT|GVIEW3D");
            ghost.await("GPOSE|");
            var pose=((LanGhostSpatial)NativeSchoolInteriorReview.field(host,"ghostPose")).pose();
            require(pose.room()==4&&map.traversable(pose.x(),pose.y(),pose.z()),"No supported ghost spawn in processing "+pose);
            scout.client.turn(Math.floorMod((int)Math.round(Math.toDegrees(Math.atan2(pose.x()-at.x(),pose.z()-at.z()))),360));
            scout.client.selectGearSlot(3);
            scout.client.useHeldEquipment();
            scout.await("GEARFX|0|UV_LIGHT|1");
            for(int i=0;i<4;i++){
                Thread.sleep(125);
                ghost.client.ghostStep(0,-100);
                ghost.awaitExact("ACCEPT|GSTEP");
            }
            var trace=scout.awaitMatching(frame->frame.startsWith("UVTRACE|")&&!frame.equals("UVTRACE|RESET"),
                "local UV mark",10000);
            var mark=UvEvidenceMarks.Mark.decode(LanProtocol.parts(trace));
            require(mark.room()==4&&Math.abs(mark.y()-map.groundHeight(mark.x(),mark.z(),4))<.03,
                "UV mark is not on the processing floor "+mark);
            pass(report,"ghost_and_uv","The Spirit spawned on the processing floor and the equipped UV light revealed its live footprint");
            ghost.client.ghost(GhostDomain.Action.WHISPER,4);
            ghost.await("ACCEPT|GHOST|WHISPER|");
            scout.client.scan(LanCase.Tool.EMF);
            scout.await("ACCEPT|SCAN|READING EMF in 4");
            var observation=witness.awaitMatching(frame->frame.startsWith("CASEFILE|")&&frame.contains("|EMF|"),
                "processing EMF observation",5000);
            require(LanEvidenceJournal.Entry.decode(observation).finding()==LanEvidenceJournal.Finding.OBSERVED,
                "The witness received no actual EMF observation");
            scout.client.identify(TYPE);
            scout.await("ACCEPT|IDENTIFY|ENTITY HYPOTHESIS RECORDED SPIRIT");
            witness.awaitExact("IDENTIFIED|SPIRIT|ABSOLUTION");
            scout.client.ward();
            scout.awaitExact("ACCEPT|WARD|ANCHOR WARD PREPARED");
            scout.client.absolve();
            scout.await("REJECT|ABSOLVE|go to the correct ritual installation");
            pass(report,"processing_anchor","Processing hall evidence replicated, and the host accepted the anchor ward and refused the remote rite");

            at=walk(scout,at,new Vec(36,0,35),new Vec(46.7,0,35));
            at=open(map,replica,scout,witness,at,1004);
            at=walk(scout,at,new Vec(56,0,35),new Vec(59,0,39));
            at=refuge(map,scout,at,1,report);
            at=walk(scout,at,new Vec(59,0,39),new Vec(56,0,35),new Vec(36,0,35),new Vec(25.3,0,35));
            at=open(map,replica,scout,witness,at,1003);

            var generator=map.level().socket("generator",2);
            var breaker=RoomLighting.breaker(map);
            var lights=(RoomLighting)NativeSchoolInteriorReview.field(host,"lights");
            at=walk(scout,at,new Vec(16,0,35),new Vec(generator.x(),0,generator.z()+1.18));
            WharfPowerChecks.use(map,scout,at,generator);
            witness.awaitExact("SOURCE|0");
            require(!lights.sourceRunning()&&!lights.supplied(),"The generator stop left power on");
            Thread.sleep(400);
            WharfPowerChecks.use(map,scout,at,generator);
            witness.awaitExact("SOURCE|1");
            require(lights.supplied(),"The generator restart did not restore power");
            at=walk(scout,at,new Vec(breaker.x(),0,breaker.z()+1.18));
            WharfPowerChecks.use(map,scout,at,breaker);
            require(!lights.breaker()&&!lights.supplied(),"The breaker did not disconnect the supply");
            Thread.sleep(400);
            WharfPowerChecks.use(map,scout,at,breaker);
            require(lights.supplied(),"The boiler breaker did not reconnect the supply");
            pass(report,"boiler_controls","Operated the generator and breaker at their physical panels and witnessed source replication");

            at=walk(scout,at,new Vec(16,0,35),new Vec(16,0,42.7));
            at=open(map,replica,scout,witness,at,1006);
            at=walk(scout,at,new Vec(16,0,50),new Vec(13.5,0,53.5));
            at=refuge(map,scout,at,0,report);
            at=walk(scout,at,new Vec(13.5,0,53.5),new Vec(16,0,50),new Vec(9.4,0,50));
            at=open(map,replica,scout,witness,at,1009);
            at=walk(scout,at,new Vec(0,0,50),new Vec(0,0,53),new Vec(-1.5,0,58));
            at=refuge(map,scout,at,7,report);
            at=walk(scout,at,new Vec(-1.5,0,58),new Vec(0,0,53),new Vec(0,0,45.2));
            at=open(map,replica,scout,witness,at,1013);
            at=walk(scout,at,new Vec(0,0,35));
            require(RitualSites.site(map,TYPE.rite()).near(new LanWorld.Body(at.x(),at.y(),at.z(),0,room(map,at))),
                "The settlement chapel rite is unreachable "+at);
            scout.client.absolve();
            scout.await("ACCEPT|ABSOLVE|RITUAL STARTED");
            witness.awaitExact("RITUAL|STARTED|ABSOLUTION");
            scout.await("RESULT|ABSOLVED",15000);
            witness.awaitExact("RESULT|ABSOLVED");
            ghost.awaitExact("RESULT|ABSOLVED");
            var result=ContractRewards.Settlement.decode(scout.await("PAYOUT|"));
            require(result.outcome()==LanCase.Outcome.ABSOLVED&&result.credits()>0&&result.xp()>0,
                "The host did not settle the mess hall investigation");
            pass(report,"mess_rite","ABSOLVED reported to all three peers; "+result.credits()+" credits and "+result.xp()+" XP");
        }
    }

    private static Vec refuge(HouseMap map,Peer scout,Vec at,int owner,List<String> report)throws Exception{
        var socket=map.level().socket("hide",owner);
        var outside=LevelClosets.world(socket,0,0,1.24);
        var front=LevelClosets.world(socket,0,0,.22);
        var back=LevelClosets.world(socket,0,0,-.20);
        at=walk(scout,at,outside);
        for(boolean crouched:new boolean[]{false,true}){
            require(!protectedAt(map,at),"Refuge protected the exterior "+socket.id());
            if(crouched){
                scout.client.crouch(true);
                scout.await("POSTURE|0|1|");
            }
            at=walk(scout,at,front);
            require(protectedAt(map,at),"Refuge missed the entry "+socket.id());
            var entered=at;
            at=walk(scout,at,back);
            require(Math.hypot(at.x()-entered.x(),at.z()-entered.z())>.14
                &&protectedAt(map,at)&&SafeAreaBarrier.blocks(map,outside,at),
                "Refuge missed the deeper interior "+socket.id());
            scout.client.hide();
            scout.awaitExact("HIDING|1");
            scout.client.hide();
            scout.awaitExact("HIDING|0");
            at=walk(scout,at,front,outside);
            require(!protectedAt(map,at),"Refuge protection persisted after exit "+socket.id());
        }
        scout.client.crouch(false);
        scout.await("POSTURE|0|0|");
        pass(report,socket.id(),"Walk-in protection at two interior depths standing and crouched; hide, reveal and exit verified");
        return at;
    }

    private static boolean protectedAt(HouseMap map,Vec at){return SafeAreaBarrier.protectedAt(map,at.x(),at.y(),at.z());}
    private static int room(HouseMap map,Vec at){return map.roomAt(at.x(),at.y(),at.z());}
    private static Vec open(HouseMap map,HouseMap replica,Peer scout,Peer witness,Vec at,int id)throws Exception{
        return NorthwatchDoorRuntime.open(map,replica,map.nativeInvestigation(),scout,witness,at,id);
    }

    private static Vec walk(Peer scout,Vec at,Vec... along)throws Exception{
        for(var target:along){
            try{at=CemeteryRefugeReview.walk(scout,0,at,target);}
            catch(AssertionError blocked){throw new AssertionError("Approach from "+at+" to "+target,blocked);}
        }
        return at;
    }

    private static void pass(List<String> report,String key,String value){
        var line=key+"\tPASS "+value;
        report.add(line);
        System.out.println(line);
    }

    private static void require(boolean ok,String reason){if(!ok)throw new AssertionError(reason);}
}
