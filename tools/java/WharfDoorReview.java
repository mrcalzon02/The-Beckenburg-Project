package org.beckenburg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dev.mrcalzon.water.WaterDocument;
import org.beckenburg.LevelDefinition.Portal;
import org.beckenburg.LevelDefinition.Vec;

/** Checks the inspected native Wharf frames using real open and closed leaves. */
public final class WharfDoorReview {
    public static void main(String[] args) throws Exception {
        if (!List.of(args).contains("--gpu")) {
            review(args);
            return;
        }
        var type=Class.forName("org.beckenburg.gpu.GpuDesktop");
        var start=type.getDeclaredMethod("startGpu");
        var stop=type.getDeclaredMethod("stopReviewGpu");
        start.setAccessible(true);
        stop.setAccessible(true);
        start.invoke(null);
        try {review(args);} finally {stop.invoke(null);}
    }

    private static void review(String[] args) throws Exception {
        Path folder=Path.of(args[0]);
        WharfNativeLevel.Source source;
        try (var layout = Files.newInputStream(folder.resolve("room-layout.tsv"));
             var doors = Files.newInputStream(folder.resolve("door-schedule.tsv"));
             var glb = Files.newInputStream(Path.of(args[1]));
             var physics = Files.newInputStream(Path.of(args[3]));
             var water = Files.newInputStream(Path.of(args[5]))) {
            source = WharfNativeLevel.read(WharfNativeLevel.rows(layout), WharfNativeLevel.rows(doors),
                glb, args[2], physics, args[4], WaterDocument.read(water));
        }
        var level = source.level();
        var scene = source.scene();
        var openings = source.openings();
        var leaves = source.doors();
        var paths = source.paths();
        var switchFeet = source.switchFeet();
        boolean named = !source.stories().isEmpty();
        var site = named ? HouseMap.Site.WHARF : HouseMap.Site.USER;
        if (named) WharfCaseChecks.auditDispatch(level, scene);
        var host = named ? new HouseMap(site) : new HouseMap(site, level, null, scene);
        var client = named ? new HouseMap(site) : new HouseMap(site, level, null, scene);
        var dock = level.dock();
        var van = new VanStaging(new VanStaging.Placement(dock.x(), dock.y(), dock.z(), dock.yaw()));
        var lip = van.world(0, VanStaging.FRONT);
        require(host.valid()&&RitualSites.valid(host),"Wharf room graph or ritual reservation is invalid");
        for (var opening:openings) auditOpening(host,opening);
        for (var frame:leaves) {
            require(NativeSchoolInteriorReview.sweep(scene.physics(),frame),"Unsupported swing "+frame.frameId());
            NativeSchoolInteriorReview.audit(host,frame,false);
            host.toggleDoor(frame.portal().id(),System.nanoTime()-2_000_000_000L);
            client.applyDoorSnapshot(host.doorSnapshot());
            NativeSchoolInteriorReview.audit(host,frame,true);
            NativeSchoolInteriorReview.audit(client,frame,true);
            System.out.println("PASS Wharf leaf and acoustic state "+frame.frameId());
        }
        for (var route:paths.entrySet()) {
            var points=new ArrayList<>(route.getValue());
            if (route.getKey().equals("L01")) {
                points.add(0,new Vec(lip.x(),van.floorY(),lip.z()));
                points.add(0,level.room(14).spawn());
            }
            for(double h:new double[]{HunterPosture.STANDING_HEIGHT,HunterPosture.CROUCH_HEIGHT}) {
                NativeSchoolInteriorReview.walk(host,points,h,route.getKey());
                Collections.reverse(points);
                NativeSchoolInteriorReview.walk(host,points,h,route.getKey()+" return");
                Collections.reverse(points);
            }
        }
        boolean gpu=GpuWorldRenderer.active()!=null;
        boolean cover=!HidingBudget.candidates(level).isEmpty();
        if (!gpu&&!cover) auditLive(level,scene);
        var captures=folder.resolve(gpu?"captures/opengl":"captures");
        Files.createDirectories(captures);
        if(host.hasGenerator()){
            var evidence=new ArrayList<String>();
            evidence.add("scene_revision\t"+scene.revision());
            evidence.add("renderer\t"+(gpu?GpuWorldRenderer.active().rendererDescription():"Software"));
            evidence.add("route_and_door_replays\t"+paths.size()+" paths and "+leaves.size()+" doors");
            WharfPowerChecks.audit(host,captures,evidence);
            if(!switchFeet.isEmpty())WharfPowerChecks.auditCircuits(host,switchFeet,captures,evidence);
            if(!gpu){
                WharfPowerChecks.live(named ? new HouseMap(site) : new HouseMap(site,level,null,scene),evidence);
                if(!switchFeet.isEmpty())WharfPowerChecks.liveCircuits(named ? new HouseMap(site) : new HouseMap(site,level,null,scene),switchFeet,evidence);
            }
            if (named) {
                NativeSchoolInteriorReview.auditWalkInCover(host, captures, evidence, "wharf");
                WharfCaseChecks.audit(host, source.stories(), captures, evidence);
                if (!gpu) {
                    auditLiveCover(new HouseMap(site), evidence);
                    WharfCaseChecks.live(level, source.stories(), evidence);
                    WharfCaseChecks.fieldActions(level, evidence);
                }
            }
            Files.writeString(folder.resolve(named ? (gpu ? "named-opengl.tsv" : "named-software.tsv")
                                                   : (gpu ? "power-opengl.tsv" : "power-software.tsv")),
                              String.join("\n",evidence)+"\n");
            evidence.forEach(System.out::println);
            return;
        }
        if(cover){
            var evidence=new ArrayList<String>();
            NativeSchoolInteriorReview.auditWalkInCover(host,captures,evidence,"wharf");
            if(!gpu)auditLiveCover(new HouseMap(site,level,null,scene),evidence);
            Files.writeString(folder.resolve(gpu?"cover-opengl.tsv":"cover-software.tsv"),
                              String.join("\n",evidence)+"\n");
            evidence.forEach(System.out::println);
            return;
        }
        for(var frame:leaves) {
            var p=frame.portal();
            if(!List.of(810,814,817).contains(p.id()))continue;
            int side=frame.swingDirection()>0?-1:1;
            NativeSchoolInteriorReview.render(host,p,captures.resolve("wharf-"+p.id()+"-open.png"),side);
            host.toggleDoor(p.id(),System.nanoTime()-2_000_000_000L);
            NativeSchoolInteriorReview.render(host,p,captures.resolve("wharf-"+p.id()+"-closed.png"),side);
        }
        var evidence=folder.resolve("verification");
        Files.createDirectories(evidence);
        Files.writeString(evidence.resolve(gpu?"opengl.tsv":"software.tsv"),String.join("\n",
            "check\tresult","native_scene_revision\t"+scene.revision(),"stage_revision\t"+level.revision(),
            "fitted_frames\t"+leaves.size(),"reachable_owners\t"+level.rooms().size(),
            "physical_walks_both_postures_and_directions\t"+paths.size(),
            "native_door_motion_body_optical_acoustic_and_snapshot\tPASS",
            "physically_checked_open_portals\t"+openings.size(),
            "four_peer_tcp_cottage_door\t"+(gpu?"covered by software TCP run":"PASS"),
            "renderer\t"+(gpu?GpuWorldRenderer.active().rendererDescription():"Software"))+"\n");
    }

    private static void auditLiveCover(HouseMap map,List<String> report) throws Exception {
        var level=map.level();
        map.configureHiding(100);
        var full=new CustomContract(3,100,150,100,100,100,100,100,CustomContract.Sprint.LIMITED,100,true,true);
        try(var host=new LanHost("127.0.0.1",0,GhostIdentity.Type.SPIRIT,full,map);
            var one=new NativeSchoolInteriorReview.Peer(host,false,"West Cottage");
            var two=new NativeSchoolInteriorReview.Peer(host,false,"East Cottage");
            var three=new NativeSchoolInteriorReview.Peer(host,false,"Net Shed");
            var ghost=new NativeSchoolInteriorReview.Peer(host,true,"Wharf Ghost")) {
            var hunters=List.of(one,two,three);
            for(var peer:List.of(one,two,three,ghost)){
                peer.awaitExact("SITE|"+map.site()+"|"+level.revision());
                peer.client.ready();
            }
            one.awaitExact("LOBBY|3|HUMAN|4|WAITING");
            one.client.start();
            one.await("INFO|mission started");
            var owners=List.of(0,1,3);
            for(int seat=0;seat<hunters.size();seat++){
                var hunter=hunters.get(seat);
                int owner=owners.get(seat);
                var cover=level.socket("hide",owner);
                for(int room:NativeSchoolInteriorReview.missionRoute(map,owner)){
                    hunter.client.move(room);
                    hunter.awaitExact("ACCEPT|MOVE|"+room);
                }
                hunter.client.view3D();
                hunter.awaitExact("ACCEPT|VIEW3D");
                var feet=NativeSchoolInteriorReview.body(hunter.await("BODY|"+seat+"|"));
                require(feet.equals(map.spawn(owner)),"Live hunter missed refuge room "+owner);
                var approach=LevelClosets.world(cover,0,0,1.18);
                var interior=LevelClosets.world(cover,0,0,.22);
                feet=walkCoverPeer(hunter,seat,feet,approach);
                require(!SafeAreaBarrier.protectedAt(map,feet.x(),feet.y(),feet.z()),"Protection before the physical doorway");
                feet=walkCoverPeer(hunter,seat,feet,interior);
                require(SafeAreaBarrier.protectedAt(map,feet.x(),feet.y(),feet.z()),"No protection inside refuge");
                hunter.client.hide();
                hunter.awaitExact("HIDING|1");
                hunter.client.hide();
                hunter.awaitExact("HIDING|0");
                feet=walkCoverPeer(hunter,seat,feet,approach);
                require(!SafeAreaBarrier.protectedAt(map,feet.x(),feet.y(),feet.z()),"Protection after exiting");
                report.add(cover.id()+"_live_entry_hide_reveal_exit\tPASS");
            }
        }
    }

    private static Vec walkCoverPeer(NativeSchoolInteriorReview.Peer peer,int seat,Vec at,Vec target)throws Exception {
        for(int step=0;step<60;step++){
            double dx=target.x()-at.x(),dz=target.z()-at.z(),distance=Math.hypot(dx,dz);
            if(distance<.165)return at;
            double scale=Math.min(100/distance,310);
            at=NativeSchoolInteriorReview.step(peer,seat,(int)Math.round(dx*scale),(int)Math.round(dz*scale));
        }
        throw new AssertionError("Wharf hunter cannot reach "+target+" from "+at);
    }

    private static void auditOpening(HouseMap map,Portal p) {
        Vec a=NativeSchoolInteriorReview.point(p,-.42,0,0);
        Vec b=NativeSchoolInteriorReview.point(p,.42,0,0);
        int one=map.roomAt(a.x(),a.y(),a.z());
        int other=map.roomAt(b.x(),b.y(),b.z());
        require(p.joins(one,other),"Unowned portal "+p.id()+" "+a+" / "+one+" "+b+" / "+other);
        for (double h:new double[]{HunterPosture.STANDING_HEIGHT,HunterPosture.CROUCH_HEIGHT}) {
            NativeSchoolInteriorReview.walk(map,List.of(a,b),h,"open Wharf portal "+p.id());
            NativeSchoolInteriorReview.walk(map,List.of(b,a),h,"reverse open Wharf portal "+p.id());
        }
    }

    private static void auditLive(LevelDefinition level,NativeInvestigationScene scene)throws Exception {
        var map=new HouseMap(HouseMap.Site.USER,level,null,scene);
        var witnessMap=new HouseMap(HouseMap.Site.USER,level,null,scene);
        try(var host=new LanHost("127.0.0.1",0,GhostIdentity.Type.SPIRIT,CustomContract.standard(3),map);
            var hunter=new NativeSchoolInteriorReview.Peer(host,false,"Wharf Scout");
            var witness=new NativeSchoolInteriorReview.Peer(host,false,"Wharf Witness");
            var teammate=new NativeSchoolInteriorReview.Peer(host,false,"Wharf Crew");
            var ghost=new NativeSchoolInteriorReview.Peer(host,true,"Wharf Ghost")) {
            for(var peer:List.of(hunter,witness,teammate,ghost)) {
                peer.awaitExact("SITE|USER|"+level.revision());
                peer.client.ready();
            }
            hunter.awaitExact("LOBBY|3|HUMAN|4|WAITING");
            hunter.client.start();
            hunter.await("INFO|mission started");
            for(int room:NativeSchoolInteriorReview.missionRoute(map,12)) {
                hunter.client.move(room);
                hunter.awaitExact("ACCEPT|MOVE|"+room);
            }
            hunter.client.view3D();
            hunter.awaitExact("ACCEPT|VIEW3D");
            Vec feet=NativeSchoolInteriorReview.body(hunter.await("BODY|0|"));
            require(feet.equals(map.spawn(12)),"Live Wharf hunter missed the shore street");
            for(int i=0;i<65;i++)feet=NativeSchoolInteriorReview.step(hunter,0,-100,0);
            for(int i=0;i<22;i++)feet=NativeSchoolInteriorReview.step(hunter,0,0,-100);
            Thread.sleep(130);
            hunter.client.step(0,-100);
            hunter.awaitExact("REJECT|blocked by world collision");
            feet=NativeSchoolInteriorReview.step(hunter,0,0,100);
            var observer=new LooseClutter.Observer(0,feet.x(),feet.y(),feet.z(),Math.PI,-.11,true);
            var target=InteractionFocus.pick(witnessMap,observer,List.of());
            require(target!=null&&target.kind()==InteractionFocus.Kind.DOOR&&target.id().equals("810"),
                    "Live aim missed the west cottage door at "+feet);
            hunter.client.turn(180);
            hunter.client.interact(target,Math.PI,-.11);
            hunter.awaitExact("ACCEPT|INTERACT|Open door");
            var state=witness.awaitExact("DOORS|810");
            witnessMap.applyDoorSnapshot(state.substring("DOORS|".length()));
            require(witnessMap.doorOpen(810),"Witness missed the fitted cottage leaf state");
            for(int i=0;i<4;i++)feet=NativeSchoolInteriorReview.step(hunter,0,0,-100);
            require(map.roomAt(feet.x(),feet.y(),feet.z())==0,"Live hunter did not cross into the cottage");
        }
    }

    private static void require(boolean v,String context){if(!v)throw new AssertionError(context);}
}
