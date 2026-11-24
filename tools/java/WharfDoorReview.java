package org.beckenburg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

import dev.mrcalzon.water.WaterDocument;
import org.beckenburg.LevelDefinition.Dock;
import org.beckenburg.LevelDefinition.Portal;
import org.beckenburg.LevelDefinition.Room;
import org.beckenburg.LevelDefinition.Socket;
import org.beckenburg.LevelDefinition.Vec;

/** Checks the inspected native Wharf frames using real open and closed leaves. */
public final class WharfDoorReview {
    private record Anchor(int id,String space,Vec at) {}

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
        var spaces=new ArrayList<NativeInvestigationScene.Space>();
        var titles=new java.util.HashMap<String,String>();
        var anchors=new ArrayList<Anchor>();
        var steps=new HashSet<String>();
        var sockets=new ArrayList<Socket>();
        var portals=new ArrayList<Portal>();
        var paths=new LinkedHashMap<String,List<Vec>>();
        for (String line:Files.readAllLines(folder.resolve("room-layout.tsv"))) {
            var r=line.split("\t");
            switch(r[0]) {
                case "SPACE" -> {
                    spaces.add(new NativeInvestigationScene.Space(r[1],id(r,2),d(r,3),d(r,4),d(r,5),d(r,6),
                        d(r,7),d(r,8),Boolean.parseBoolean(r[9]),true));
                    titles.put(r[1],r[10]);
                }
                case "TREAD" -> steps.add(r[1]);
                case "ANCHOR" -> anchors.add(new Anchor(id(r,1),r[2],vec(r,3)));
                case "COVER" -> sockets.add(new Socket(r[1],"hide",id(r,2),d(r,3),d(r,4),d(r,5),d(r,6),"walk-in-cupboard"));
                case "RITUAL" -> sockets.add(new Socket("wharf-"+r[1],"ritual",id(r,2),d(r,3),d(r,4),d(r,5),0,r[1]));
                case "POINT" -> paths.computeIfAbsent(r[1],ignored->new ArrayList<>()).add(vec(r,2));
                case "TRANSITION" -> portals.add(new Portal(id(r,1),id(r,3),id(r,4),d(r,5),d(r,6),d(r,7),
                        d(r,9),r[8].equals("z"),r[10],1));
                default -> throw new IllegalArgumentException("Invalid Wharf row "+r[0]);
            }
        }
        var openings=List.copyOf(portals);
        var leaves=new ArrayList<NativeInvestigationDoor.Spec>();
        for (String line:Files.readAllLines(folder.resolve("door-schedule.tsv"))) {
            var r=line.split("\t");
            var p=new Portal(id(r,0),id(r,4),id(r,5),d(r,6),d(r,7),d(r,8),d(r,9),r[10].equals("z"),"door",1);
            portals.add(p);
            leaves.add(new NativeInvestigationDoor.Spec(r[1],r[2],r[3],p,d(r,11),d(r,12),d(r,13),
                      id(r,14),id(r,15),id(r,16),r[17]));
        }
        var dock=new Dock(32,0,74,0);
        var van=new VanStaging(new VanStaging.Placement(dock.x(),dock.y(),dock.z(),dock.yaw()));
        var lip=van.world(0,VanStaging.FRONT);
        portals.add(new Portal(880,13,14,lip.x(),van.floorY(),lip.z(),VanStaging.RAMP_HALF_WIDTH*2,true,"trail",1));
        NativeInvestigationScene scene;
        WaterDocument water;
        try(var glb=Files.newInputStream(Path.of(args[1]));var physics=Files.newInputStream(Path.of(args[3]));
            var wet=Files.newInputStream(Path.of(args[5]))) {
            scene=NativeInvestigationScene.read(glb,args[2],physics,args[4],steps,spaces,leaves);
            water=WaterDocument.read(wet);
        }
        var rooms=new Room[15];
        for(var a:anchors) {
            var at=a.at();
            var space=scene.spaceAt(at.x(),at.y(),at.z());
            require(space!=null&&space.id().equals(a.space())&&space.owner()==a.id()
                &&scene.physics().traversable(at,HunterPosture.STANDING_HEIGHT),"Invalid Wharf anchor "+a+": "+space);
            rooms[a.id()]=new Room(a.id(),titles.get(a.space()),space.x0(),space.z0(),space.x1(),space.z1(),
                space.floor(),space.height(),space.outside()?"native-outdoor":"wood",
                space.outside()?"outdoor":"timber",at);
        }
        rooms[14]=new Room(14,"Expedition van",30.74,71,33.26,76.4,van.floorY(),VanStaging.INTERIOR_HEIGHT,
                           "van","metal",new Vec(32,van.floorY(),74));
        var level=new LevelDefinition("wld-06",scene.revision(),"Drowned Fisherman's Wharf",List.of(rooms),portals,
                List.of(),List.of(),List.of(),sockets,List.of(),dock,"coast",water);
        var host=new HouseMap(HouseMap.Site.USER,level,null,scene);
        var client=new HouseMap(HouseMap.Site.USER,level,null,scene);
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
                points.add(0,rooms[14].spawn());
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
        if(cover){
            var evidence=new ArrayList<String>();
            NativeSchoolInteriorReview.auditWalkInCover(host,captures,evidence,"wharf");
            if(!gpu)auditLiveCover(level,scene,evidence);
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
            "fitted_frames\t"+leaves.size(),"reachable_owners\t"+rooms.length,
            "physical_walks_both_postures_and_directions\t"+paths.size(),
            "native_door_motion_body_optical_acoustic_and_snapshot\tPASS",
            "physically_checked_open_portals\t"+openings.size(),
            "four_peer_tcp_cottage_door\t"+(gpu?"covered by software TCP run":"PASS"),
            "renderer\t"+(gpu?GpuWorldRenderer.active().rendererDescription():"Software"))+"\n");
    }

    private static void auditLiveCover(LevelDefinition level,NativeInvestigationScene scene,List<String> report)
            throws Exception {
        var map=new HouseMap(HouseMap.Site.USER,level,null,scene);
        map.configureHiding(100);
        var full=new CustomContract(3,100,150,100,100,100,100,100,CustomContract.Sprint.LIMITED,100,true,true);
        try(var host=new LanHost("127.0.0.1",0,GhostIdentity.Type.SPIRIT,full,map);
            var one=new NativeSchoolInteriorReview.Peer(host,false,"West Cottage");
            var two=new NativeSchoolInteriorReview.Peer(host,false,"East Cottage");
            var three=new NativeSchoolInteriorReview.Peer(host,false,"Net Shed");
            var ghost=new NativeSchoolInteriorReview.Peer(host,true,"Wharf Ghost")) {
            var hunters=List.of(one,two,three);
            for(var peer:List.of(one,two,three,ghost)){
                peer.awaitExact("SITE|USER|"+level.revision());
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

    private static Vec vec(String[] r,int i){return new Vec(d(r,i),d(r,i+1),d(r,i+2));}
    private static double d(String[] r,int i){return Double.parseDouble(r[i]);}
    private static int id(String[] r,int i){return Integer.parseInt(r[i]);}
    private static void require(boolean v,String context){if(!v)throw new AssertionError(context);}
}
