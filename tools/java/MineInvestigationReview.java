package org.beckenburg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.beckenburg.LevelDefinition.Vec;
import org.beckenburg.NativeSchoolInteriorReview.Peer;

/** Walks the packaged Mine from the cargo floor through the shafts to the wash-house rite. */
final class MineInvestigationReview {
    private static final HouseMap.Site SITE=HouseMap.Site.MINE;
    private static final GhostIdentity.Type TYPE=GhostIdentity.Type.SHADE;

    private MineInvestigationReview(){}

    public static void main(String[] args)throws Exception{
        System.setProperty(MineNativeLevel.REVIEW_PROPERTY,"true");
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
            var scout=new Peer(host,false,"Mine investigator");
            var witness=new Peer(host,false,"Mine witness");
            var ghost=new Peer(host,true,"Mine Shade")){
            var map=(HouseMap)NativeSchoolInteriorReview.field(host,"map");
            require(map.nativeInvestigation()==named.nativeInvestigation(),"The host selected a different native mine");
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
            for(int i=0;i<13;i++)at=NativeSchoolInteriorReview.step(scout,0,0,-100);
            at=walk(scout,at,new Vec(28,0,76),new Vec(28,0,71),new Vec(30,0,69),
                new Vec(25,0,64),new Vec(23,0,60),new Vec(23,0,57));
            at=open(map,replica,scout,witness,at,700);
            at=walk(scout,at,new Vec(16,0,57),new Vec(12,0,59),new Vec(7,0,59));
            at=refuge(map,scout,at,0,report);
            at=walk(scout,at,new Vec(7,0,59),new Vec(12,0,59),new Vec(16,0,57),new Vec(23,0,57),
                new Vec(29,0,57),new Vec(37,0,57));
            at=open(map,replica,scout,witness,at,703);
            at=walk(scout,at,new Vec(43,0,57),new Vec(46,0,58),new Vec(49,0,58));
            at=refuge(map,scout,at,1,report);
            at=walk(scout,at,new Vec(49,0,58),new Vec(46,0,58),new Vec(43,0,57),new Vec(37,0,57),
                new Vec(34,0,55.8),new Vec(34,0,46),new Vec(36,0,43),new Vec(38.5,0,43));
            at=open(map,replica,scout,witness,at,704);
            at=walk(scout,at,new Vec(45,0,43),new Vec(47,0,38),new Vec(47,0,34),new Vec(49,0,34));
            at=refuge(map,scout,at,11,report);

            at=walk(scout,at,new Vec(49,0,34),new Vec(47,0,34),new Vec(47,0,38),new Vec(45,0,43),
                new Vec(36,0,43),new Vec(34,0,46),new Vec(34,0,55.8),new Vec(29,0,56.5));
            at=open(map,replica,scout,witness,at,705);
            at=walk(scout,at,new Vec(29,0,52),new Vec(28.35,0,52));
            at=open(map,replica,scout,witness,at,706);
            require(Math.abs(at.y())<.03,"Haul door missed its level receiving landing "+at);
            at=walk(scout,at,new Vec(28.35,0,47.9),new Vec(28.35,-3,41.3),new Vec(28.35,-3,39.9));
            require(Math.abs(at.y()+3)<.03,"Haul flight missed the intermediate landing "+at);
            at=walk(scout,at,new Vec(28.35,-6,32.3),new Vec(28.35,-6,31),new Vec(28,-6,31));
            require(room(map,at)==4&&Math.abs(at.y()+6)<.03,"Haul flight missed the anchor floor "+at);

            ghost.client.ghost(GhostDomain.Action.WHISPER,4);
            ghost.await("ACCEPT|GHOST|WHISPER|");
            scout.client.scan(LanCase.Tool.EMF);
            scout.await("ACCEPT|SCAN|READING EMF in 4");
            var observation=witness.awaitMatching(frame->frame.startsWith("CASEFILE|")&&frame.contains("|EMF|"),
                "haul-floor EMF observation",5000);
            require(LanEvidenceJournal.Entry.decode(observation).finding()==LanEvidenceJournal.Finding.OBSERVED,
                "The witness received no actual EMF observation");
            scout.client.identify(TYPE);
            scout.await("ACCEPT|IDENTIFY|ENTITY IDENTIFIED SHADE");
            witness.awaitExact("IDENTIFIED|SHADE|CLEANSING");
            scout.client.ward();
            scout.awaitExact("ACCEPT|WARD|ANCHOR WARD PREPARED");
            scout.client.cleanse();
            scout.await("REJECT|CLEANSE|go to the correct ritual installation");
            pass(report,"haul_anchor","Headhouse doors, level threshold and two haul flights reached the -6 m anchor; the observed EMF and ward replicated");

            at=walk(scout,at,new Vec(20,-6,31),new Vec(11,-6,31),new Vec(11,-6,42),new Vec(11,-6,52),
                new Vec(11.75,-6,52),new Vec(18.05,-3,52),new Vec(19.75,-3,52));
            require(Math.abs(at.y()+3)<.03,"Ventilation stair missed the intermediate landing "+at);
            at=walk(scout,at,new Vec(25.35,0,52));
            require(Math.abs(at.y())<.03,"Ventilation stair missed the level receiving landing "+at);
            at=open(map,replica,scout,witness,at,707);
            require(room(map,at)==3,"Ventilation stair door missed the headhouse "+at);
            pass(report,"ventilation_return","The eastbound ventilation flights returned from -6 m through a supported landing and operating headhouse door");

            at=walk(scout,at,new Vec(29,0,52),new Vec(29,0,56.5),new Vec(34,0,55.8),new Vec(34,0,46),
                new Vec(36,0,43),new Vec(45,0,43),new Vec(47,0,38));
            require(RitualSites.site(map,TYPE.rite()).near(new LanWorld.Body(at.x(),at.y(),at.z(),0,room(map,at))),
                "The wash-house cleansing installation is unreachable "+at);
            scout.client.cleanse();
            scout.await("ACCEPT|CLEANSE|RITUAL STARTED");
            witness.awaitExact("RITUAL|STARTED|CLEANSING");
            scout.await("RESULT|CLEANSED",15000);
            witness.awaitExact("RESULT|CLEANSED");
            ghost.awaitExact("RESULT|CLEANSED");
            var result=ContractRewards.Settlement.decode(scout.await("PAYOUT|"));
            require(result.outcome()==LanCase.Outcome.CLEANSED&&result.credits()>0&&result.xp()>0,
                "The host did not settle the wash-house investigation");
            pass(report,"wash_house_rite","CLEANSED reported to all three peers; "+result.credits()+" credits and "+result.xp()+" XP");
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
