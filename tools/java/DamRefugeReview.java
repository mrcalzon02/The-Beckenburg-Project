package org.beckenburg;

import org.beckenburg.LevelDefinition.Socket;
import org.beckenburg.LevelDefinition.Vec;
import org.beckenburg.NativeSchoolInteriorReview.Peer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Reviews complete Dam refuge volumes, normal travel and live host authority. */
public final class DamRefugeReview {
    private static final List<Integer> OWNERS=List.of(1,4,10);
    private static final Map<Integer,List<Vec>> APPROACHES=Map.of(
        1,List.of(new Vec(12.3,0,30.8)),
        4,List.of(new Vec(49.8,-6,57.4)),
        10,List.of(new Vec(40.35,-6,12.25)));
    private static final GhostIdentity.Type GHOST=GhostIdentity.Type.SPIRIT;

    private DamRefugeReview(){}

    public static void main(String[] args)throws Exception {
        var packet=Path.of(args[0]);
        var water=Path.of(args[1]);
        if(!List.of(args).contains("--gpu")){review(packet,water,false);return;}
        var desktop=Class.forName("org.beckenburg.gpu.GpuDesktop");
        var start=desktop.getDeclaredMethod("startGpu");
        var stop=desktop.getDeclaredMethod("stopReviewGpu");
        start.setAccessible(true);
        stop.setAccessible(true);
        start.invoke(null);
        try{review(packet,water,true);}finally{stop.invoke(null);}
    }

    private static void review(Path packet,Path water,boolean gpu)throws Exception {
        var data=DamDoorReview.read(packet,water,"dam-with-refuges");
        var map=data.map();
        require(map.valid()&&RitualSites.valid(map),"Refuge placement invalidated Dam spawns or rites");
        var report=new ArrayList<>(List.of("check\tresult",
            "native_scene_revision\t"+data.nativeScene().revision(),
            "collision_sha256\t"+data.nativeScene().physics().sha256(),
            "level_revision\t"+data.level().revision(),"water_revision\t"+data.level().water().revision(),
            "renderer\t"+(gpu?GpuWorldRenderer.active().rendererDescription():"Software")));
        var captures=packet.resolve(gpu?"captures/opengl":"captures/software");
        Files.createDirectories(captures);
        require(data.doors().size()==14,"A fitted Dam door was lost");
        for(var frame:data.doors()){
            require(NativeSchoolInteriorReview.sweep(data.nativeScene().physics(),frame),"Refuge obstructed "+frame.frameId());
            NativeSchoolInteriorReview.audit(map,frame,false);
            map.toggleDoor(frame.portal().id(),System.nanoTime()-2_000_000_000L);
            require(map.doorOpen(frame.portal().id()),"Door could not open "+frame.frameId());
            NativeSchoolInteriorReview.audit(map,frame,true);
        }
        report.add("retained_fitted_doors\t"+data.doors().size());
        NativeSchoolInteriorReview.auditWalkInCover(map,captures,report,"dam",APPROACHES);
        NativeSchoolInteriorReview.auditHiding(map,report);
        map.configureHiding(100);
        int volumePoints=0;
        for(var pocket:HidingBudget.candidates(map.level())){
            for(int x=-7;x<=7;x++)for(int z=-5;z<=5;z++)for(double h:new double[]{0,.42,.88,1.42,2.01}){
                var p=LevelClosets.world(pocket,x*.1,h,z*.1);
                require(protectedAt(map,p),pocket.id()+" interior volume gap "+p);
                volumePoints++;
            }
            for(double h:new double[]{0,.42,.88,1.42,2.01}){
                for(double[] outside:new double[][]{{-.82,0},{.82,0},{0,-.58},{0,.66}}){
                    require(!protectedAt(map,LevelClosets.world(pocket,outside[0],h,outside[1])),
                        pocket.id()+" protection extends beyond the enclosure");
                }
            }
        }
        report.add("protected_samples_across_full_width_depth_and_height\t"+volumePoints);
        report.add("physical_volume_m\t1.58 x 1.14 x 2.02");
        cargoRoutes(data,map,report);
        var disabled=map.level().socket("hide",4);
        map.configureHiding(0);
        require(NativeSchoolInteriorReview.captureStanding(map,LevelClosets.world(disabled,0,0,-.27),
            -disabled.yaw(),captures.resolve("dam-workshop-disabled.png"))==0,"Disabled refuge drew an active cue");
        report.add("disabled_refuge_cue_pixels\t0");
        if(!gpu){
            live(data,report);
            zeroCover(data,report);
        }
        Files.writeString(packet.resolve(gpu?"refuge-opengl.tsv":"refuge-software.tsv"),String.join("\n",report)+"\n");
        report.forEach(System.out::println);
    }

    private static void cargoRoutes(DamDoorReview.Scene data,HouseMap map,List<String> report){
        var van=map.van();
        var center=van.world(0,0);
        var toe=van.world(0,VanStaging.FRONT);
        int directions=0;
        for(int room:OWNERS){
            var path=new ArrayList<>(List.of(new Vec(center.x(),van.floorY(),center.z()),
                new Vec(toe.x(),van.floorY(),toe.z()),new Vec(36,0,75.45),new Vec(36,0,69)));
            join(path,data.paths().get("L02"),false);
            if(room==1){
                join(path,data.paths().get("L03"),false);
                join(path,data.paths().get("L04"),false);
            }else{
                join(path,data.paths().get("L06"),true);
                join(path,data.paths().get("L07"),false);
                join(path,data.paths().get(room==4?"L08":"L13"),false);
            }
            var pocket=map.level().socket("hide",room);
            path.addAll(APPROACHES.get(room));
            path.add(LevelClosets.world(pocket,0,0,1.32));
            path.add(LevelClosets.world(pocket,0,0,-.27));
            var reverse=new ArrayList<>(path);
            Collections.reverse(reverse);
            for(double height:new double[]{HunterPosture.STANDING_HEIGHT,HunterPosture.CROUCH_HEIGHT}){
                NativeSchoolInteriorReview.walk(map,path,height,pocket.id()+" from cargo");
                NativeSchoolInteriorReview.walk(map,reverse,height,pocket.id()+" cargo return");
                directions+=2;
            }
        }
        report.add("full_cargo_to_refuge_and_return_direction_replays\t"+directions);
    }

    private static void join(List<Vec> route,List<Vec> segment,boolean reverse){
        var points=new ArrayList<>(segment);
        if(reverse)Collections.reverse(points);
        require(points.get(0).equals(route.get(route.size()-1)),"Dam route did not join at the physical receiving floor");
        route.addAll(points.subList(1,points.size()));
    }

    private static CustomContract contract(int hunters,int cover){
        return new CustomContract(hunters,100,150,100,100,100,100,100,
            CustomContract.Sprint.LIMITED,cover,true,true);
    }

    private static void live(DamDoorReview.Scene data,List<String> report)throws Exception {
        var map=data.map();
        try(var host=new LanHost("127.0.0.1",0,GHOST,contract(3,100),map);
            var office=new Peer(host,false,"Records investigator");
            var workshop=new Peer(host,false,"Workshop investigator");
            var plant=new Peer(host,false,"Switchgear investigator");
            var ghost=new Peer(host,true,"Dam spectral witness")){
            var hunters=List.of(office,workshop,plant);
            start(data,office,List.of(office,workshop,plant,ghost),3);
            for(int seat=0;seat<hunters.size();seat++){
                var peer=hunters.get(seat);
                int owner=OWNERS.get(seat);
                var pocket=map.level().socket("hide",owner);
                var at=approach(map,peer,seat,pocket);
                require(!protectedAt(map,at),"Hunter gained cover before crossing "+pocket.id());
                if(owner==4){
                    peer.client.crouch(true);
                    peer.awaitExact("HIDING|0");
                }
                at=CemeteryRefugeReview.walk(peer,seat,at,LevelClosets.world(pocket,0,0,.30));
                require(protectedAt(map,at),"Entrance did not grant automatic protection "+pocket.id());
                at=walkToCentre(peer,seat,at,pocket);
                require(protectedAt(map,at),"Back of live refuge was exposed "+pocket.id());
                peer.client.hide();
                peer.awaitExact("HIDING|1");
                peer.client.hide();
                peer.awaitExact("HIDING|0");
                at=CemeteryRefugeReview.walk(peer,seat,at,LevelClosets.world(pocket,0,0,1.32));
                require(!protectedAt(map,at),"Hunter remained protected outside "+pocket.id());
                peer.client.hide();
                peer.awaitExact("REJECT|no hiding place within reach");
                report.add(pocket.id()+"_live_entry_hide_reveal_exit\tPASS");
            }
            spectralThreshold(map,ghost,true);
            report.add("live_spectral_workshop_threshold\tPASS");
        }
    }

    private static void zeroCover(DamDoorReview.Scene data,List<String> report)throws Exception {
        var map=data.map();
        try(var host=new LanHost("127.0.0.1",0,GHOST,contract(1,0),map);
            var investigator=new Peer(host,false,"Zero cover investigator");
            var ghost=new Peer(host,true,"Zero cover spectral")){
            start(data,investigator,List.of(investigator,ghost),1);
            var pocket=map.level().socket("hide",4);
            var at=approach(map,investigator,0,pocket);
            at=CemeteryRefugeReview.walk(investigator,0,at,LevelClosets.world(pocket,0,0,.30));
            at=walkToCentre(investigator,0,at,pocket);
            require(!protectedAt(map,at),"Zero-cover contract granted protection");
            investigator.client.hide();
            investigator.awaitExact("REJECT|no hiding place within reach");
            spectralThreshold(map,ghost,false);
            report.add("live_zero_density_same_walkable_shell_without_cover\tPASS");
        }
    }

    private static void start(DamDoorReview.Scene data,Peer leader,List<Peer> peers,int hunters)throws Exception{
        for(var peer:peers){
            peer.awaitExact("SITE|USER|"+data.level().revision());
            peer.client.ready();
        }
        leader.awaitExact("LOBBY|"+hunters+"|HUMAN|"+(hunters+1)+"|WAITING");
        leader.client.start();
        leader.await("INFO|mission started");
    }

    private static Vec approach(HouseMap map,Peer peer,int seat,Socket pocket)throws Exception{
        for(int owner:NativeSchoolInteriorReview.missionRoute(map,pocket.room())){
            peer.client.move(owner);
            peer.awaitExact("ACCEPT|MOVE|"+owner);
        }
        peer.client.view3D();
        peer.awaitExact("ACCEPT|VIEW3D");
        var at=NativeSchoolInteriorReview.body(peer.await("BODY|"+seat+"|"));
        for(var p:APPROACHES.get(pocket.room()))at=CemeteryRefugeReview.walk(peer,seat,at,p);
        return CemeteryRefugeReview.walk(peer,seat,at,LevelClosets.world(pocket,0,0,1.32));
    }

    private static Vec walkToCentre(Peer peer,int seat,Vec from,Socket pocket)throws Exception{
        // The live host normalizes every nonzero step vector to full effort.
        // Step squarely toward the back and stop in the central floor band.
        var at=from;
        int dx=(int)Math.round(100*Math.sin(pocket.yaw()));
        int dz=(int)Math.round(-100*Math.cos(pocket.yaw()));
        for(int i=0;i<4&&localDepth(pocket,at)>.023;i++)
            at=NativeSchoolInteriorReview.step(peer,seat,dx,dz);
        require(localDepth(pocket,at)<=.023,"Hunter missed the refuge centre "+pocket.id()+" "+at);
        return at;
    }

    private static double localDepth(Socket pocket,Vec at){
        return -(at.x()-pocket.x())*Math.sin(pocket.yaw())+(at.z()-pocket.z())*Math.cos(pocket.yaw());
    }

    private static void spectralThreshold(HouseMap map,Peer ghost,boolean enabled)throws Exception{
        ghost.client.viewGhost3D();
        ghost.awaitExact("ACCEPT|GVIEW3D");
        var at=NativeSchoolInteriorReview.body(ghost.await("GPOSE|4|"));
        var pocket=map.level().socket("hide",4);
        var approach=APPROACHES.get(4).get(0);
        for(int i=0;i<32&&distance(at,approach)>.175;i++){
            double factor=100/distance(at,approach);
            at=ghostStep(ghost,(int)Math.round((approach.x()-at.x())*factor),
                (int)Math.round((approach.z()-at.z())*factor));
        }
        require(distance(at,approach)<=.175,"Spectral control missed the clear workshop aisle "+at);
        var threshold=LevelClosets.world(pocket,0,0,LevelClosets.ENTRY_Z);
        double step=.31*GhostTraits.movementPercent(GHOST)/100;
        for(int i=0;i<12&&at.z()+step<threshold.z()+.006;i++)at=ghostStep(ghost,0,100);
        require(at.z()<threshold.z()&&at.z()+step>threshold.z(),"Spectral control missed the refuge threshold "+at);
        if(enabled){
            Thread.sleep(125);
            ghost.client.ghostStep(0,100);
            ghost.awaitExact("REJECT|spectral hiding pocket is inaccessible");
        }else{
            at=ghostStep(ghost,0,100);
            require(at.z()>threshold.z()&&!protectedAt(map,at),"Disabled refuge blocked the same spectral crossing");
        }
    }

    private static Vec ghostStep(Peer ghost,int dx,int dz)throws Exception {
        Thread.sleep(125);
        ghost.client.ghostStep(dx,dz);
        ghost.awaitExact("ACCEPT|GSTEP");
        return NativeSchoolInteriorReview.body(ghost.await("GPOSE|4|"));
    }

    private static double distance(Vec a,Vec b){return Math.hypot(a.x()-b.x(),a.z()-b.z());}
    private static boolean protectedAt(HouseMap map,Vec p){return SafeAreaBarrier.protectedAt(map,p.x(),p.y(),p.z());}
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
}
