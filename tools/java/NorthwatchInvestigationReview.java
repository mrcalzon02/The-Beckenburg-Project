package org.beckenburg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.beckenburg.LevelDefinition.Vec;
import org.beckenburg.NativeSchoolInteriorReview.Peer;

/** Replays Northwatch's investigation as one continuous three-dimensional walk. */
final class NorthwatchInvestigationReview {
    private static final HouseMap.Site SITE=HouseMap.Site.NORTHWATCH;
    private static final GhostIdentity.Type TYPE=GhostIdentity.Type.WRAITH;

    private NorthwatchInvestigationReview(){}

    public static void main(String[] args)throws Exception{
        System.setProperty(NorthwatchNativeLevel.REVIEW_PROPERTY,"true");
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
            var scout=new Peer(host,false,"Northwatch investigator");
            var witness=new Peer(host,false,"Northwatch witness");
            var ghost=new Peer(host,true,"Northwatch Wraith")){
            var map=(HouseMap)NativeSchoolInteriorReview.field(host,"map");
            require(map.nativeInvestigation()==named.nativeInvestigation(),"The host selected a different native station");
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
            at=walk(scout,at,new Vec(32,0,63),new Vec(32,0,56),new Vec(32,0,41),new Vec(26,0,41));
            at=open(map,replica,scout,witness,at,845);
            at=walk(scout,at,new Vec(26,0,19),new Vec(31,0,19));
            at=open(map,replica,scout,witness,at,846);
            at=walk(scout,at,new Vec(38,0,19));

            require(room(map,at)==4,"The station route missed the archive anchor "+at);
            ghost.client.ghost(GhostDomain.Action.WHISPER,4);
            ghost.await("ACCEPT|GHOST|WHISPER|");
            scout.client.scan(LanCase.Tool.EMF);
            scout.await("ACCEPT|SCAN|READING EMF in 4");
            var observation=witness.awaitMatching(frame->frame.startsWith("CASEFILE|")&&frame.contains("|EMF|"),
                "archive EMF observation",5000);
            require(LanEvidenceJournal.Entry.decode(observation).finding()==LanEvidenceJournal.Finding.OBSERVED,
                "The witness received no actual EMF observation");
            scout.client.identify(TYPE);
            scout.await("ACCEPT|IDENTIFY|ENTITY IDENTIFIED WRAITH");
            witness.awaitExact("IDENTIFIED|WRAITH|BANISHMENT");
            scout.client.ward();
            scout.awaitExact("ACCEPT|WARD|ANCHOR WARD PREPARED");
            scout.client.banish();
            scout.await("REJECT|BANISH|go to the correct ritual installation");
            pass(report,"radio_and_archive","Cargo ramp, courtyard and two operating doors reached the archive evidence and ward");

            at=walk(scout,at,new Vec(38,0,16),new Vec(42.4,0,16));
            at=open(map,replica,scout,witness,at,848);
            at=walk(scout,at,new Vec(53.5,0,16),new Vec(53.5,0,14),new Vec(61,0,14));
            at=open(map,replica,scout,witness,at,853);
            at=walk(scout,at,new Vec(66,0,14),new Vec(66,0,16.1));
            at=refuge(map,scout,at,9,report);

            at=walk(scout,at,new Vec(66,0,16.1),new Vec(66,0,14),new Vec(53.5,0,14),
                new Vec(53,0,16),new Vec(53,0,24.8));
            at=open(map,replica,scout,witness,at,849);
            at=walk(scout,at,new Vec(53,0,28),new Vec(55,0,28),new Vec(57.7,0,30.9));
            at=refuge(map,scout,at,1,report);
            at=walk(scout,at,new Vec(57.7,0,30.9),new Vec(55,0,30.9),new Vec(55,0,35),new Vec(53,0,35));

            at=walk(scout,at,new Vec(42,-2.5,35),new Vec(40.5,-2.5,35),new Vec(40.5,-2.5,31),new Vec(32,-2.5,31));
            require(room(map,at)==7&&Math.abs(at.y()+2.5)<.03,"Dorm stair missed the heated tunnel "+at);
            at=walk(scout,at,new Vec(23.5,-2.5,31),new Vec(23.5,-2.5,35),new Vec(22,-2.5,35),new Vec(11,0,35));
            require(room(map,at)==0&&Math.abs(at.y())<.03,"West stair missed the laboratory floor "+at);
            pass(report,"heated_service_tunnel","East dorm stair descended to -2.5 m; west stair ascended to the laboratory");

            at=walk(scout,at,new Vec(8,0,35),new Vec(7,0,40.7));
            at=refuge(map,scout,at,0,report);
            at=walk(scout,at,new Vec(7,0,40.7),new Vec(8,0,35),new Vec(11,0,35));
            require(RitualSites.site(map,TYPE.rite()).near(new LanWorld.Body(at.x(),at.y(),at.z(),0,room(map,at))),
                "The laboratory banishment installation is unreachable "+at);

            scout.client.banish();
            scout.await("ACCEPT|BANISH|RITUAL STARTED");
            witness.awaitExact("RITUAL|STARTED|BANISHMENT");
            scout.await("RESULT|BANISHED",15000);
            witness.awaitExact("RESULT|BANISHED");
            ghost.awaitExact("RESULT|BANISHED");
            var result=ContractRewards.Settlement.decode(scout.await("PAYOUT|"));
            require(result.outcome()==LanCase.Outcome.BANISHED&&result.credits()>0&&result.xp()>0,
                "The host did not settle the laboratory investigation");
            pass(report,"laboratory_rite","BANISHED reported to all three peers; "+result.credits()+" credits and "+result.xp()+" XP");
        }
    }

    private static Vec refuge(HouseMap map,Peer scout,Vec at,int owner,List<String> report)throws Exception{
        var socket=map.level().socket("hide",owner);
        var outside=LevelClosets.world(socket,0,0,1.24);
        var front=LevelClosets.world(socket,0,0,.22);
        var back=LevelClosets.world(socket,0,0,-.27);
        at=walk(scout,at,outside);
        for(boolean crouched:new boolean[]{false,true}){
            require(!protectedAt(map,at),"Refuge protected the exterior "+socket.id());
            if(crouched){
                scout.client.crouch(true);
                scout.await("POSTURE|0|1|");
            }
            at=walk(scout,at,front);
            require(protectedAt(map,at),"Refuge missed the entry "+socket.id());
            at=walk(scout,at,back);
            require(protectedAt(map,at)&&SafeAreaBarrier.blocks(map,outside,at),"Refuge missed the rear "+socket.id());
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
