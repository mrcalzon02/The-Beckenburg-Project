package org.beckenburg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.beckenburg.NativeSchoolInteriorReview.Peer;

/** Bounded case actions on the same packaged Asylum used by the geometry review. */
public final class NativeAsylumCaseReview {
    private NativeAsylumCaseReview(){}

    public static void main(String[] args)throws Exception{
        System.setProperty(AsylumNativeLevel.REVIEW_PROPERTY,"true");
        var map=new HouseMap(HouseMap.Site.ASYLUM);
        var report=new ArrayList<String>();
        report.add("measure\tresult");
        report.add("packaged_revision\t"+map.level().revision());
        ritual(map,report);
        recovery(map,true,report);
        recovery(map,false,report);
        Files.write(Path.of(args[0]),report);
        report.forEach(System.out::println);
    }

    private static void require(boolean condition,String label){
        if(!condition)throw new AssertionError(label);
    }

    private static void start(HouseMap map,Peer leader,List<Peer> peers)throws Exception{
        for(var peer:peers){
            peer.awaitExact("SITE|ASYLUM|"+map.level().revision());
            peer.client.ready();
        }
        leader.awaitExact("LOBBY|3|HUMAN|4|WAITING");
        leader.client.start();
        leader.await("INFO|mission started");
    }

    private static int level(String reading){
        int at=reading.lastIndexOf("EMF ")+4;
        return Integer.parseInt(reading.substring(at,reading.indexOf('/',at)));
    }

    private static void ritual(HouseMap map,List<String> report)throws Exception{
        var contract=CustomContract.standard(3);
        try(var host=new LanHost("127.0.0.1",0,GhostIdentity.Type.POLTERGEIST,contract,false,false,HouseMap.Site.ASYLUM);
            var close=new Peer(host,false,"Close");
            var remote=new Peer(host,false,"Remote");
            var witness=new Peer(host,false,"Witness");
            var ghost=new Peer(host,true,"Ghost")){
            start(map,close,List.of(close,remote,witness,ghost));
            for(int room:NativeSchoolInteriorReview.missionRoute(map,4)){
                close.client.move(room);
                close.awaitExact("ACCEPT|MOVE|"+room);
            }
            close.client.view3D();
            close.awaitExact("ACCEPT|VIEW3D");
            var at=NativeSchoolInteriorReview.body(close.await("BODY|0|"));
            if(!at.equals(map.spawn(4)))throw new AssertionError("The physical sensor missed the ward floor");
            remote.client.view3D();
            remote.awaitExact("ACCEPT|VIEW3D");
            var other=NativeSchoolInteriorReview.body(remote.await("BODY|1|"));
            if(map.roomAt(other.x(),other.y(),other.z())!=14)throw new AssertionError("The remote sensor missed the cargo body");
            ghost.client.ghost(GhostDomain.Action.DISTURB,4);
            ghost.await("ACCEPT|GHOST|DISTURB|");
            close.client.scan(LanCase.Tool.EMF);
            var near=close.await("ACCEPT|SCAN|READING EMF in 4");
            remote.client.scan(LanCase.Tool.EMF);
            var far=remote.await("ACCEPT|SCAN|READING EMF in 14");
            int signal=level(near),baseline=level(far);
            if(signal<8||baseline>7)throw new AssertionError(near+" / "+far);
            close.client.identify(GhostIdentity.Type.POLTERGEIST);
            close.await("ACCEPT|IDENTIFY|");
            close.client.ward();
            close.await("ACCEPT|WARD|ANCHOR WARD PREPARED");
            close.client.banish();
            close.awaitExact("REJECT|BANISH|go to the correct ritual installation");
            var site=RitualSites.site(map,GhostIdentity.Rite.BANISHMENT);
            for(int room:NativeSchoolInteriorReview.missionRoute(map,site.room())){
                witness.client.move(room);
                witness.awaitExact("ACCEPT|MOVE|"+room);
            }
            witness.client.view3D();
            witness.awaitExact("ACCEPT|VIEW3D");
            var feet=NativeSchoolInteriorReview.body(witness.await("BODY|2|"));
            if(!site.near(new LanWorld.Body(feet.x(),feet.y(),feet.z(),0,map.roomAt(feet.x(),feet.y(),feet.z()))))
                throw new AssertionError("Party did not reach the installed chapel ritual "+feet);
            witness.client.banish();
            witness.await("ACCEPT|BANISH|RITUAL STARTED");
            close.awaitExact("RITUAL|STARTED|BANISHMENT");
            close.await("RESULT|BANISHED",8000);
            for(var peer:List.of(remote,witness,ghost))peer.await("RESULT|BANISHED");
            if(host.running())throw new AssertionError("Accepted guarded rite did not settle");

            report.add("ward_spatial_evidence\tWard EMF "+signal+"/10, remote van "+baseline+"/10");
            report.add("guarded_chapel_banishment\tCorrect identification and ward; wrong-room rejection; BANISHED on four peers");
        }
    }

    private static void recovery(HouseMap map,boolean escort,List<String> report)throws Exception{
        try(var host=new LanHost("127.0.0.1",0,GhostIdentity.Type.SPIRIT,
                    CustomContract.standard(3),false,false,HouseMap.Site.ASYLUM);
            var leader=new Peer(host,false,"Lead");
            var stranded=new Peer(host,false,"Stranded");
            var observer=new Peer(host,false,"Van observer");
            var ghost=new Peer(host,true,"Ghost")){
            var peers=List.of(leader,stranded,observer,ghost);
            start(map,leader,peers);
            for(var peer:List.of(leader,stranded)){
                peer.client.view3D();
                peer.awaitExact("ACCEPT|VIEW3D");
            }
            var outside=NativeSchoolInteriorReview.body(stranded.await("BODY|1|"));
            for(int i=0;i<10;i++)outside=NativeSchoolInteriorReview.step(stranded,1,-100,0);
            require(map.roomAt(outside.x(),outside.y(),outside.z())==13&&Math.abs(outside.y())<.005,"Teammate missed the real forecourt");
            stranded.client.vanDoor();
            stranded.awaitExact("REJECT|van door button out of reach");
            var rover=NativeSchoolInteriorReview.body(leader.await("BODY|0|"));
            if(escort){
                for(int i=0;i<10;i++)rover=NativeSchoolInteriorReview.step(leader,0,-100,0);
                require(map.roomAt(rover.x(),rover.y(),rover.z())==13&&Math.abs(rover.y()-outside.y())<.01
                    &&Math.hypot(rover.x()-outside.x(),rover.z()-outside.z())<1.6,
                    "Leader missed the supported teammate rendezvous "+rover+" / "+outside);
                for(int i=0;i<10;i++){
                    outside=NativeSchoolInteriorReview.step(stranded,1,100,0);
                    rover=NativeSchoolInteriorReview.step(leader,0,100,0);
                }
                require(map.roomAt(outside.x(),outside.y(),outside.z())==14,"Escorted teammate missed the cargo floor");
            }
            require(map.roomAt(rover.x(),rover.y(),rover.z())==14&&map.van().nearDoorButton(rover.x(),rover.z()),
                "Leader missed the actual cargo control");
            leader.client.vanDoor();
            leader.awaitExact("ACCEPT|VANDOOR|CLOSED");
            observer.awaitExact("EXTRACTION|0|1");
            observer.awaitExact("EXTRACTION|1|"+(escort?1:0));
            observer.awaitExact("EXTRACTION|2|1");
            observer.await("RESULT|RETREATED");
            require(!host.running(),"Physical van control did not settle the roster");
            report.add(escort?"team_forecourt_recovery\tTwo physical hunters met on the forecourt and returned aboard"
                :"outside_hunter_extraction_negative\tCargo control refused outdoors; host marked outside hunter left behind");
        }
    }
}
