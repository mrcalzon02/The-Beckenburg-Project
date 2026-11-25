package org.beckenburg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.beckenburg.LevelDefinition.Vec;
import org.beckenburg.NativeSchoolInteriorReview.Peer;

/** Walks the ordinary mission from cargo across the Marsh to the chapel rite. */
final class MourningMarshInvestigationReview {
    private static final HouseMap.Site SITE=HouseMap.Site.MOURNING_MARSH;
    private static final GhostIdentity.Type TYPE=GhostIdentity.Type.SPIRIT;

    private MourningMarshInvestigationReview(){}

    public static void main(String[] args)throws Exception{
        System.setProperty(MourningMarshNativeLevel.REVIEW_PROPERTY,"true");
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
            var scout=new Peer(host,false,"Marsh investigator");
            var witness=new Peer(host,false,"Marsh witness");
            var ghost=new Peer(host,true,"Marsh Spirit")){
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
            at=walk(scout,at,new Vec(32,0,76),new Vec(32,0,72),new Vec(39.98,0,72),
                new Vec(39.98,0,70.17),new Vec(39.98,0,65.6),new Vec(39.98,1.2,61.75),new Vec(40,1.2,59));
            require(room(map,at)==9&&Math.abs(at.y()-1.2)<.03,"The real levee stair missed the south deck "+at);
            pass(report,"levee_stair","The cargo ramp and supported seven-riser levee stair reached the +1.20 m deck");

            at=walk(scout,at,new Vec(50,1.2,59),new Vec(50,1.2,58));
            at=open(map,replica,scout,witness,at,986);
            at=walk(scout,at,new Vec(58,1.2,58),new Vec(58,1.2,59),new Vec(58,1.2,58),
                new Vec(50,1.2,58),new Vec(50,1.2,46));
            at=open(map,replica,scout,witness,at,981);
            at=walk(scout,at,new Vec(59,1.2,46),new Vec(61.5,1.2,49));
            at=refuge(map,scout,at,1,report);
            at=walk(scout,at,new Vec(61.5,1.2,49),new Vec(59,1.2,46),new Vec(50,1.2,46),
                new Vec(50,1.2,38),new Vec(50,1.2,25));
            at=open(map,replica,scout,witness,at,985);
            at=walk(scout,at,new Vec(58,1.2,25),new Vec(50,1.2,25));
            at=open(map,replica,scout,witness,at,984);
            at=walk(scout,at,new Vec(40,1.2,25));
            require(room(map,at)==4,"The warehouse goods door missed the anchor owner "+at);
            ghost.client.ghost(GhostDomain.Action.WHISPER,4);
            ghost.await("ACCEPT|GHOST|WHISPER|");
            scout.client.scan(LanCase.Tool.EMF);
            scout.await("ACCEPT|SCAN|READING EMF in 4");
            var observation=witness.awaitMatching(frame->frame.startsWith("CASEFILE|")&&frame.contains("|EMF|"),
                "warehouse EMF observation",5000);
            require(LanEvidenceJournal.Entry.decode(observation).finding()==LanEvidenceJournal.Finding.OBSERVED,
                "The witness received no actual EMF observation");
            scout.client.identify(TYPE);
            scout.await("ACCEPT|IDENTIFY|ENTITY IDENTIFIED SPIRIT");
            witness.awaitExact("IDENTIFIED|SPIRIT|ABSOLUTION");
            scout.client.ward();
            scout.awaitExact("ACCEPT|WARD|ANCHOR WARD PREPARED");
            scout.client.absolve();
            scout.await("REJECT|ABSOLVE|go to the correct ritual installation");
            pass(report,"warehouse_anchor","Warehouse evidence replicated, and the host accepted the anchor ward and refused the remote rite");

            at=walk(scout,at,new Vec(33.4,1.2,25));
            at=open(map,replica,scout,witness,at,983);
            at=walk(scout,at,new Vec(30,1.2,25),new Vec(30,1.2,38),new Vec(30,1.2,46));
            at=open(map,replica,scout,witness,at,980);
            at=walk(scout,at,new Vec(20,1.2,46),new Vec(17,1.2,49));
            at=refuge(map,scout,at,0,report);
            at=walk(scout,at,new Vec(17,1.2,49),new Vec(20,1.2,46),new Vec(30,1.2,46),
                new Vec(30,1.2,38),new Vec(30,1.2,25));
            at=open(map,replica,scout,witness,at,982);
            at=walk(scout,at,new Vec(20,1.2,25),new Vec(15.5,1.2,21));
            at=refuge(map,scout,at,2,report);
            at=walk(scout,at,new Vec(15.5,1.2,21),new Vec(20,1.2,25));
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
                "The host did not settle the chapel investigation");
            pass(report,"chapel_rite","ABSOLVED reported to all three peers; "+result.credits()+" credits and "+result.xp()+" XP");
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
