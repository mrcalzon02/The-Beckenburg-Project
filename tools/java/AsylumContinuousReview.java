package org.beckenburg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.beckenburg.LevelDefinition.Vec;
import org.beckenburg.NativeSchoolInteriorReview.Peer;

/** One St. Dymphna case followed continuously from the van and back through the hospital. */
final class AsylumContinuousReview {
    private static final HouseMap.Site SITE=HouseMap.Site.ASYLUM;
    private static final GhostIdentity.Type TYPE=GhostIdentity.Type.SPIRIT;

    private AsylumContinuousReview(){}

    public static void main(String[] args)throws Exception{
        System.setProperty(AsylumNativeLevel.REVIEW_PROPERTY,"true");
        var map=new HouseMap(SITE);
        var report=new ArrayList<String>();
        report.add("named_revision\t"+map.level().revision());
        review(map,report);
        Files.write(Path.of(args[0]),report);
    }

    private static void review(HouseMap map,List<String> report)throws Exception{
        var replica=HouseMap.forNetworkRevision(SITE,map.level().revision());
        var contract=new CustomContract(2,100,150,100,100,100,100,100,CustomContract.Sprint.LIMITED,100,true,true);
        try(var host=new LanHost("127.0.0.1",0,TYPE,contract,false,false,SITE);
            var scout=new Peer(host,false,"Asylum ward investigator");
            var witness=new Peer(host,false,"Asylum case witness");
            var ghost=new Peer(host,true,"Asylum Spirit")){
            var authority=(HouseMap)NativeSchoolInteriorReview.field(host,"map");
            require(authority.nativeInvestigation()==map.nativeInvestigation(),"Host used a different native hospital");
            for(var peer:List.of(scout,witness,ghost)){
                peer.awaitExact("SITE|"+SITE+"|"+map.level().revision());
                peer.client.ready();
            }
            scout.awaitExact("LOBBY|2|HUMAN|3|WAITING");
            scout.client.start();
            scout.await("INFO|mission started");
            scout.client.view3D();
            scout.awaitExact("ACCEPT|VIEW3D");
            var at=NativeSchoolInteriorReview.body(scout.await("BODY|0|"));
            require(room(authority,at)==14,"Hunter did not start on the cargo deck");
            for(int i=0;i<9;i++)at=NativeSchoolInteriorReview.step(scout,0,-100,0);
            at=walk(scout,at,new Vec(27.772,0,76),new Vec(27.772,0,72),new Vec(32,0,72),
                new Vec(32,0,64),new Vec(30,0,64));
            at=open(map,replica,scout,witness,at,470);
            at=walk(scout,at,new Vec(30,0,54),new Vec(32,0,52));
            at=open(map,replica,scout,witness,at,471);
            at=walk(scout,at,new Vec(39.5,0,52));
            at=open(map,replica,scout,witness,at,472);
            at=walk(scout,at,new Vec(39.5,0,56),new Vec(41,0,56));
            at=refuge(authority,scout,at,8,report);
            pass(report,"admission","Real cargo ramp, admission door, interview door and records door");
            at=walk(scout,at,new Vec(41,0,56),new Vec(39.5,0,56),new Vec(39.5,0,52),
                new Vec(32,0,52),new Vec(27.5,0,54));
            at=open(map,replica,scout,witness,at,473);
            at=walk(scout,at,new Vec(27.5,0,45));
            at=open(map,replica,scout,witness,at,477);
            at=walk(scout,at,new Vec(18,0,45),new Vec(18,0,46),new Vec(12.1,0,46));
            at=refuge(authority,scout,at,2,report);
            at=walk(scout,at,new Vec(12.1,0,46),new Vec(18,0,46),new Vec(18,0,42));
            at=open(map,replica,scout,witness,at,474);
            require(room(authority,at)==12,"Court doors did not lead to the supported patient court");
            at=walk(scout,at,new Vec(18,0,34.5),new Vec(18,0,42),new Vec(18,0,45),new Vec(27.5,0,45),
                new Vec(27.5,0,31),new Vec(27.5,0,28.10),new Vec(30.25,0,28.10),new Vec(30.25,0,21.1),new Vec(32,0,21.1));
            at=walk(scout,at,new Vec(32,3.4,27.9));
            require(room(authority,at)==7 && Math.abs(at.y()-3.4)<.03,"Main flight missed the upper ward floor "+at);
            at=walk(scout,at,new Vec(32,0,21.1));
            require(room(authority,at)==0 && Math.abs(at.y())<.03,"Return down the main flight missed the lower floor "+at);
            pass(report,"court_and_main_stair","Day-room doors crossed into the court and back; main flight reached +3.4 m and returned");

            at=walk(scout,at,new Vec(30.25,0,21.1),new Vec(30.25,0,28.10),new Vec(27.5,0,28.10),
                new Vec(27.5,0,16),new Vec(32,0,16),new Vec(32,0,18),new Vec(44.5,0,18),new Vec(54,0,18),new Vec(54.5,0,18),
                new Vec(55,0,18.5),new Vec(55,0,19.5),new Vec(56.5,0,19.5),new Vec(57,0,19),
                new Vec(57,-3,13.1),new Vec(55.5,-3,13.1),new Vec(55.5,-3,18),new Vec(55.5,-3,24),new Vec(55.5,-3,37),
                new Vec(54,-3,38),new Vec(44.5,-3,38),new Vec(40,-3,38),new Vec(40,-3,36),new Vec(40,-3,30));
            require(room(authority,at)==11 && Math.abs(at.y()+3)<.03,"Cellar flight missed the usable plant floor "+at);
            at=walk(scout,at,new Vec(42.5,-3,29),new Vec(43.6,-3,27));
            at=refuge(authority,scout,at,11,report);
            at=walk(scout,at,new Vec(43.6,-3,27),new Vec(42.5,-3,29),new Vec(40,-3,30),
                new Vec(40,-3,36),new Vec(40,-3,38),new Vec(44.5,-3,38),new Vec(54,-3,38),
                new Vec(55.5,-3,37),new Vec(55.5,-3,24),new Vec(55.5,-3,18),new Vec(55.5,-3,13.1),new Vec(57,-3,13.1),
                new Vec(57,0,19),new Vec(56.5,0,19.5),new Vec(55,0,19.5),new Vec(55,0,18.5),
                new Vec(54.5,0,18),new Vec(54,0,18),new Vec(44.5,0,18),new Vec(32,0,18),new Vec(32,0,16),new Vec(27.5,0,16));
            require(room(authority,at)==0 && Math.abs(at.y())<.03,"The cellar ascent did not return to the service level");
            pass(report,"service_stair","The physical cellar flight reached −3 m and returned to the ground service corridor");

            at=walk(scout,at,new Vec(27.5,0,20.5));
            at=open(map,replica,scout,witness,at,476);
            at=walk(scout,at,new Vec(18,0,20.5));
            require(room(authority,at)==4,"The ward door did not reach the true anchor");
            scout.client.scan(LanCase.Tool.EMF);
            scout.await("ACCEPT|SCAN|READING EMF");
            witness.awaitMatching(frame->frame.startsWith("CASEFILE|")&&frame.contains("|EMF|"),"ward EMF observation",5000);
            scout.client.identify(TYPE);
            scout.await("ACCEPT|IDENTIFY|ENTITY HYPOTHESIS RECORDED SPIRIT");
            scout.client.ward();
            scout.await("ACCEPT|WARD|ANCHOR WARD PREPARED");
            scout.client.absolve();
            scout.await("REJECT|ABSOLVE|go to the correct ritual installation");
            at=walk(scout,at,new Vec(27.5,0,20.5),new Vec(27.5,0,31),new Vec(27.5,0,45),new Vec(18,0,45));
            require(RitualSites.site(authority,TYPE.rite()).near(new LanWorld.Body(at.x(),at.y(),at.z(),0,room(authority,at))),
                "The day room lectern is unreachable");
            scout.client.absolve();
            scout.await("ACCEPT|ABSOLVE|RITUAL STARTED");
            witness.awaitExact("RITUAL|STARTED|ABSOLUTION");
            scout.await("RESULT|ABSOLVED",15000);
            witness.awaitExact("RESULT|ABSOLVED");
            ghost.awaitExact("RESULT|ABSOLVED");
            var result=ContractRewards.Settlement.decode(scout.await("PAYOUT|"));
            require(result.outcome()==LanCase.Outcome.ABSOLVED&&result.credits()>0&&result.xp()>0,
                "The host did not settle the resolved ward investigation");
            pass(report,"ward_and_absolution","Physical anchor ward and day-room rite yielded ABSOLVED and "
                +result.credits()+" credits / "+result.xp()+" XP");
        }
    }

    private static Vec refuge(HouseMap map,Peer scout,Vec at,int owner,List<String> report)throws Exception{
        var pocket=map.level().socket("hide",owner);
        var outside=LevelClosets.world(pocket,0,0,1.24);
        var front=LevelClosets.world(pocket,0,0,.22);
        var back=LevelClosets.world(pocket,0,0,-.27);
        at=walk(scout,at,outside);
        require(!protectedAt(map,at),"Safe zone leaked into the corridor "+pocket.id());
        for(boolean crouched:new boolean[]{false,true}){
            if(crouched){
                scout.client.crouch(true);
                scout.await("POSTURE|0|1|");
            }
            at=walk(scout,at,front,back);
            require(protectedAt(map,at)&&SafeAreaBarrier.blocks(map,outside,at),"Missed supported refuge "+pocket.id());
            scout.client.hide();
            scout.awaitExact("HIDING|1");
            scout.client.hide();
            scout.awaitExact("HIDING|0");
            at=walk(scout,at,front,outside);
            require(!protectedAt(map,at),"Protection survived exit "+pocket.id());
        }
        scout.client.crouch(false);
        scout.await("POSTURE|0|0|");
        pass(report,pocket.id(),"Standing and crouched walk-in protection, hide, reveal and exit; spectral sweep excluded");
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
        String line=key+"\tPASS "+value;
        report.add(line);
        System.out.println(line);
    }
    private static void require(boolean ok,String reason){if(!ok)throw new AssertionError(reason);}
}
