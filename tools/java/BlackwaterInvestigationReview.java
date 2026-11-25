package org.beckenburg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.beckenburg.LevelDefinition.Vec;
import org.beckenburg.NativeSchoolInteriorReview.Peer;

/** Follows one packaged Blackwater investigation from the cargo deck to settlement. */
final class BlackwaterInvestigationReview {
    private static final HouseMap.Site SITE=HouseMap.Site.BLACKWATER;
    private static final GhostIdentity.Type TYPE=GhostIdentity.Type.SHADE;

    private BlackwaterInvestigationReview(){}

    public static void main(String[] args)throws Exception{
        System.setProperty(BlackwaterNativeLevel.REVIEW_PROPERTY,"true");
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
            var scout=new Peer(host,false,"Dam investigator");
            var witness=new Peer(host,false,"Dam case witness");
            var ghost=new Peer(host,true,"Dam Shade")){
            var map=(HouseMap)NativeSchoolInteriorReview.field(host,"map");
            require(map.nativeInvestigation()==named.nativeInvestigation(),"The host selected a different native dam");
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
            at=walk(scout,at,new Vec(36,0,69),new Vec(40,0,69),new Vec(40,0,52),new Vec(31,0,52),new Vec(17,0,52));
            at=open(map,replica,scout,witness,at,870);
            at=walk(scout,at,new Vec(17,0,41));
            at=open(map,replica,scout,witness,at,871);
            at=walk(scout,at,new Vec(17,0,28),new Vec(12.3,0,30.8));
            at=refuge(map,scout,at,1,report);

            at=walk(scout,at,new Vec(12.3,0,30.8),new Vec(17,0,28),new Vec(17,0,41),new Vec(17,0,52),
                new Vec(31,0,52));
            at=open(map,replica,scout,witness,at,873);
            at=walk(scout,at,new Vec(31,0,46.5),new Vec(31,0,45.23),new Vec(31,-3,35.7),
                new Vec(31,-3,34.3),new Vec(31,-6,24.78),new Vec(31,-6,23.5),
                new Vec(34.7,-6,23.5),new Vec(34.7,-6,35));
            at=open(map,replica,scout,witness,at,874);
            at=walk(scout,at,new Vec(50,-6,35));
            require(room(map,at)==3&&Math.abs(at.y()+6)<.03,"West stair missed the turbine floor "+at);
            pass(report,"crest_to_turbine","Cargo ramp, two office doors and west stair entrance and exit verified with replicated leaves");
            at=walk(scout,at,new Vec(45,-6,23.2));
            at=open(map,replica,scout,witness,at,880);
            at=walk(scout,at,new Vec(45,-6,15),new Vec(40.35,-6,12.25));
            at=refuge(map,scout,at,10,report);
            at=walk(scout,at,new Vec(40.35,-6,12.25),new Vec(45,-6,15),new Vec(45,-6,23.2),new Vec(50,-6,35),
                new Vec(52,-6,35),new Vec(52,-6,43),new Vec(45,-6,43));
            at=open(map,replica,scout,witness,at,875);
            at=walk(scout,at,new Vec(45,-6,55),new Vec(49.8,-6,57.4));
            at=refuge(map,scout,at,4,report);
            at=walk(scout,at,new Vec(49.8,-6,57.4),new Vec(45,-6,55));
            require(room(map,at)==4,"Workshop door missed the case anchor "+at);
            scout.client.scan(LanCase.Tool.EMF);
            scout.await("ACCEPT|SCAN|READING EMF");
            witness.awaitMatching(frame->frame.startsWith("CASEFILE|")&&frame.contains("|EMF|"),
                "workshop EMF observation",5000);
            scout.client.identify(TYPE);
            scout.await("ACCEPT|IDENTIFY|ENTITY IDENTIFIED SHADE");
            witness.awaitExact("IDENTIFIED|SHADE|CLEANSING");
            scout.client.ward();
            scout.await("ACCEPT|WARD|ANCHOR WARD PREPARED");
            scout.client.cleanse();
            scout.await("REJECT|CLEANSE|go to the correct ritual installation");
            pass(report,"anchor","Workshop EMF reached the witness; host accepted the ward and required the lower ritual installation");

            // The east switchback links the turbine floor to the lower gallery via its own landing.
            at=walk(scout,at,new Vec(45,-6,43),new Vec(52,-6,43),new Vec(52,-6,35),new Vec(62.5,-6,35),
                new Vec(62.5,-6,36.19),new Vec(62.5,-8,40.22),new Vec(62.5,-8,40.88),
                new Vec(60.3,-8,40.88),new Vec(60.3,-8,40.22),new Vec(60.3,-10,36.19),
                new Vec(60.3,-10,35),new Vec(57,-10,35));
            at=open(map,replica,scout,witness,at,882);
            at=walk(scout,at,new Vec(57,-10,29),
                new Vec(50,-10,29),new Vec(50,-10,27),new Vec(43,-10,30.8));
            require(Math.abs(at.y()+10)<.03,"East switchback missed the lower gallery "+at);
            at=open(map,replica,scout,witness,at,881);
            at=walk(scout,at,new Vec(43,-10,40));
            require(RitualSites.site(map,TYPE.rite()).near(new LanWorld.Body(at.x(),at.y(),at.z(),0,room(map,at))),
                "The lower valve installation is unreachable "+at);
            pass(report,"turbine_to_valve","East switchback crossed its -8 m landing and reached the -10 m gallery and fitted vault door");

            scout.client.cleanse();
            scout.await("ACCEPT|CLEANSE|RITUAL STARTED");
            witness.awaitExact("RITUAL|STARTED|CLEANSING");
            scout.await("RESULT|CLEANSED",15000);
            witness.awaitExact("RESULT|CLEANSED");
            ghost.awaitExact("RESULT|CLEANSED");
            var result=ContractRewards.Settlement.decode(scout.await("PAYOUT|"));
            require(result.outcome()==LanCase.Outcome.CLEANSED&&result.credits()>0&&result.xp()>0,
                "The host did not settle the cleansing investigation");
            pass(report,"settlement","CLEANSED reported to all three peers; "+result.credits()+" credits and "+result.xp()+" XP");
        }
    }

    private static Vec refuge(HouseMap map,Peer scout,Vec at,int owner,List<String> report)throws Exception{
        var socket=map.level().socket("hide",owner);
        var outside=LevelClosets.world(socket,0,0,1.24);
        var front=LevelClosets.world(socket,0,0,.22);
        var back=LevelClosets.world(socket,0,0,-.27);
        at=walk(scout,at,outside);
        require(!protectedAt(map,at),"Refuge protected the exterior "+socket.id());
        for(boolean crouched:new boolean[]{false,true}){
            if(crouched){
                scout.client.crouch(true);
                scout.await("POSTURE|0|1|");
            }
            at=walk(scout,at,front);
            require(protectedAt(map,at),"Walk-in protection did not begin across the threshold "+socket.id()+" "+at);
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
