package org.beckenburg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.beckenburg.LevelDefinition.Vec;
import org.beckenburg.NativeSchoolInteriorReview.Peer;

/** Completes a named Monastery case with real 3D movement and independent TCP peers. */
final class MonasteryInvestigationReview {
    private static final HouseMap.Site SITE=HouseMap.Site.MONASTERY;
    private static final GhostIdentity.Type TYPE=GhostIdentity.Type.WRAITH;
    private static final Vec BELL_FOOT=new Vec(52.97,0,24.5);

    private MonasteryInvestigationReview(){}

    public static void main(String[] args)throws Exception{
        System.setProperty(MonasteryNativeLevel.REVIEW_PROPERTY,"true");
        var report=new ArrayList<String>();
        var map=new HouseMap(SITE);
        report.add("named_revision\t"+map.level().revision());
        review(map,report);
        Files.write(Path.of(args[0]),report);
    }

    private static void review(HouseMap map,List<String> report)throws Exception{
        var replica=HouseMap.forNetworkRevision(SITE,map.level().revision());
        var paths=new LinkedHashMap<String,List<Vec>>();
        try(var stream=MonasteryNativeLevel.class.getResourceAsStream("/org/beckenburg/sites/wld-10/runtime-input.tsv")){
            for(var row:WharfNativeLevel.rows(stream)){
                if(row[0].equals("POINT"))paths.computeIfAbsent(row[1],key->new ArrayList<>()).add(
                    new Vec(Double.parseDouble(row[2]),Double.parseDouble(row[3]),Double.parseDouble(row[4])));
            }
        }
        var contract=new CustomContract(2,100,150,100,100,100,100,100,CustomContract.Sprint.LIMITED,100,true,true);
        try(var host=new LanHost("127.0.0.1",0,TYPE,contract,false,false,SITE);
            var scout=new Peer(host,false,"Monastery investigator");
            var witness=new Peer(host,false,"Monastery case witness");
            var ghost=new Peer(host,true,"Monastery Wraith")){
            var authority=(HouseMap)NativeSchoolInteriorReview.field(host,"map");

            require(authority.nativeInvestigation()==map.nativeInvestigation(),"Host mounted a different collision scene");
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
            require(authority.roomAt(at.x(),at.y(),at.z())==14,"The hunter did not start on the cargo deck");
            for(int i=0;i<15;i++)at=NativeSchoolInteriorReview.step(scout,0,0,-100);
            at=walk(scout,at,new Vec(32,0,68));
            at=open(map,replica,scout,witness,at,930);
            at=walk(scout,at,new Vec(32,0,59));
            at=open(map,replica,scout,witness,at,931);
            pass(report,"arrival","Walked off the van deck, aimed at both gatehouse leaves and crossed their actual thresholds");

            at=walk(scout,at,new Vec(32,0,53),new Vec(20,0,53),new Vec(11,0,53),new Vec(11,0,45),new Vec(11,0,36));
            at=open(map,replica,scout,witness,at,941);
            at=walk(scout,at,new Vec(5.2,0,36),new Vec(0,0,36),new Vec(-3,0,39));
            at=refuge(authority,scout,at,5,report);
            at=walk(scout,at,new Vec(-3,0,39),new Vec(0,0,36),new Vec(6.45,0,36),new Vec(11,0,36),
                new Vec(11,0,24),new Vec(11,0,17),new Vec(18,0,17));
            at=open(map,replica,scout,witness,at,938);
            at=walk(scout,at,new Vec(18,0,14.2),new Vec(18,0,9),new Vec(22,0,7));
            at=refuge(authority,scout,at,3,report);
            at=walk(scout,at,new Vec(22,0,7),new Vec(18,0,9),new Vec(18,0,14.2),new Vec(18,0,17),
                new Vec(36,0,17),new Vec(47,0,17),new Vec(47,0,26),new Vec(47,0,40));
            at=open(map,replica,scout,witness,at,944);
            at=walk(scout,at,new Vec(59,0,40));
            require(authority.roomAt(at.x(),at.y(),at.z())==4,"Host did not put the hunter in the refectory anchor");

            ghost.client.ghost(GhostDomain.Action.WHISPER,4);
            ghost.await("ACCEPT|GHOST|WHISPER|");
            for(var tool:LanCase.Tool.values()){
                scout.client.scan(tool);
                var reading=scout.await("ACCEPT|SCAN|READING "+tool);
                var line=witness.awaitMatching(frame->frame.startsWith("CASEFILE|")&&frame.contains("|"+tool+"|"),
                    "casefile observation "+tool,5000);
                require(LanEvidenceJournal.Entry.decode(line).finding()==LanEvidenceJournal.Finding.OBSERVED,
                    "The host treated a raw reading as automatically confirmed evidence");
                pass(report,"measured_"+tool,reading);
            }
            scout.client.identify(TYPE);
            scout.await("ACCEPT|IDENTIFY|ENTITY HYPOTHESIS RECORDED "+TYPE);
            witness.awaitExact("IDENTIFIED|WRAITH|BANISHMENT");
            scout.client.ward();
            scout.await("ACCEPT|WARD|ANCHOR WARD PREPARED");
            pass(report,"identification_and_anchor","Explicit hunter identification and a real anchor ward in the refectory");

            at=walk(scout,at,new Vec(51.4,0,40),new Vec(47,0,40),new Vec(47,0,26));
            at=open(map,replica,scout,witness,at,942);
            at=walk(scout,at,BELL_FOOT);
            var ascent=paths.get("L16");
            at=walk(scout,at,ascent.toArray(Vec[]::new));
            require(Math.abs(at.y()-6)<.03 && authority.roomAt(at.x(),at.y(),at.z())==7,"Both bell flights failed to reach the upper gallery");
            var descent=new ArrayList<>(ascent);
            java.util.Collections.reverse(descent);
            at=walk(scout,at,descent.toArray(Vec[]::new));
            require(Math.abs(at.y())<.03,"Bell descent did not recover the ground-floor datum");
            pass(report,"bell_stair","Both flights traversed to +6 m and back to ground");
            at=walk(scout,at,new Vec(51.4,0,26),new Vec(47,0,26),new Vec(47,0,17),new Vec(29,0,17));
            at=walk(scout,at,paths.get("L19").toArray(Vec[]::new));
            require(Math.abs(at.y()+3)<.03 && authority.roomAt(at.x(),at.y(),at.z())==8,"Lower stair did not reach the actual catacombs");
            scout.client.banish();
            scout.await("REJECT|BANISH|go to the correct ritual installation");
            at=walk(scout,at,new Vec(21.25,-3,17.2),new Vec(18,-3,17.2));
            at=open(map,replica,scout,witness,at,946);
            at=walk(scout,at,new Vec(18,-3,13.8),new Vec(18,-3,9),new Vec(23,-3,7));
            at=refuge(authority,scout,at,9,report);
            at=walk(scout,at,new Vec(23,-3,7),new Vec(18,-3,9));
            var site=RitualSites.site(authority,TYPE.rite());
            require(site.near(new LanWorld.Body(at.x(),at.y(),at.z(),0,authority.roomAt(at.x(),at.y(),at.z()))),
                "Ritual stance does not meet the authored room, elevation and reach");
            scout.client.banish();
            scout.await("ACCEPT|BANISH|RITUAL STARTED");
            witness.awaitExact("RITUAL|STARTED|BANISHMENT");
            require(host.running(),"Host resolved the ritual before it ran");
            scout.await("RESULT|BANISHED",15000);
            witness.awaitExact("RESULT|BANISHED");
            ghost.awaitExact("RESULT|BANISHED");
            require(!host.running(),"The host remained active after the result");
            String payout=scout.awaitMatching(frame->frame.startsWith("REWARD|")||frame.startsWith("SETTLEMENT|")
                ||frame.startsWith("PAYOUT|"),"completed round payout",5000);
            pass(report,"banishment_and_settlement","Guarded the lower circle until all three peers received BANISHED. "+payout);
            scout.client.scan(LanCase.Tool.EMF);
            scout.await("REJECT|");
            pass(report,"closed_case","A new scan after settlement was rejected");
        }
    }

    private static Vec refuge(HouseMap authority,Peer scout,Vec at,int owner,List<String> report)throws Exception{
        var pocket=authority.level().socket("hide",owner);
        var outside=LevelClosets.world(pocket,0,0,1.24);
        var front=LevelClosets.world(pocket,0,0,.22);
        var back=LevelClosets.world(pocket,0,0,-.27);
        at=walk(scout,at,outside);
        require(!protectedAt(authority,at),"Protection extended into the aisle at "+pocket.id());
        at=walk(scout,at,front);
        require(protectedAt(authority,at),"Standing entry did not receive protection at "+pocket.id());
        scout.client.hide();
        scout.awaitExact("HIDING|1");
        scout.client.hide();
        scout.awaitExact("HIDING|0");
        at=walk(scout,at,outside);
        require(!protectedAt(authority,at),"Standing exit retained protection at "+pocket.id());

        scout.client.crouch(true);
        scout.await("POSTURE|0|1|");
        at=walk(scout,at,front,back);
        require(protectedAt(authority,at)&&protectedAt(authority,new Vec(at.x(),at.y()+1.05,at.z())),
            "Crouched hunter reached unprotected interior at "+pocket.id());
        require(SafeAreaBarrier.blocks(authority,outside,at),"Refuge admitted a crossing spectral sweep");
        scout.client.hide();
        scout.awaitExact("HIDING|1");
        scout.client.hide();
        scout.awaitExact("HIDING|0");
        at=walk(scout,at,front,outside);
        require(!protectedAt(authority,at),"Crouched exit retained protection at "+pocket.id());
        scout.client.crouch(false);
        scout.await("POSTURE|0|0|");
        pass(report,pocket.id(),"Standing and crouched TCP entry, concealment, reveal and exit. Protection from threshold to rear and spectral sweep exclusion");
        return at;
    }

    private static boolean protectedAt(HouseMap map,Vec at){return SafeAreaBarrier.protectedAt(map,at.x(),at.y(),at.z());}
    private static Vec open(HouseMap map,HouseMap replica,Peer scout,Peer witness,Vec at,int id)throws Exception{
        return NorthwatchDoorRuntime.open(map,replica,map.nativeInvestigation(),scout,witness,at,id);
    }
    private static Vec walk(Peer scout,Vec at,Vec... along)throws Exception{return NorthwatchDoorRuntime.walk(scout,at,along);}
    private static void pass(List<String> report,String check,String detail){
        String line=check+"\tPASS "+detail;
        report.add(line);
        System.out.println(line);
    }
    private static void require(boolean pass,String reason){if(!pass)throw new AssertionError(reason);}
}
