package org.beckenburg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.HashSet;
import org.beckenburg.LevelDefinition.Dock;
import org.beckenburg.LevelDefinition.Portal;
import org.beckenburg.LevelDefinition.Room;
import org.beckenburg.LevelDefinition.Socket;
import org.beckenburg.LevelDefinition.Vec;
import org.beckenburg.NativeSchoolInteriorReview.Peer;

/** Exact staged Cemetery and Mortuary door, room, movement, acoustic and LAN checks. */
public final class CemeteryDoorReview {
    private static final double[] POSTURES={HunterPosture.STANDING_HEIGHT,HunterPosture.CROUCH_HEIGHT};
    private record Anchor(int owner,String room,Vec feet) {}

    private CemeteryDoorReview() {}

    public static void main(String[] args) throws Exception {
        boolean gpu=List.of(args).contains("--gpu");
        if(!gpu){review(args,false);return;}
        var launcher=Class.forName("org.beckenburg.gpu.GpuDesktop");
        var start=launcher.getDeclaredMethod("startGpu");
        var stop=launcher.getDeclaredMethod("stopReviewGpu");
        start.setAccessible(true);
        stop.setAccessible(true);
        start.invoke(null);
        try {review(args,true);} finally {stop.invoke(null);}
    }

    private static void review(String[] args,boolean gpu)throws Exception {
        Path folder=Path.of(args[0]);
        boolean annex=folder.getFileName().toString().endsWith("mortuary");
        var spaces=new ArrayList<NativeInvestigationScene.Space>();
        var titles=new LinkedHashMap<String,String>();
        var anchors=new ArrayList<Anchor>();
        var sockets=new ArrayList<Socket>();
        var switchFeet=new LinkedHashMap<Integer,Vec>();
        var stairs=new HashSet<String>();
        var openings=new ArrayList<Portal>();
        var routes=new LinkedHashMap<String,List<Vec>>();
        for(String line:Files.readAllLines(folder.resolve("room-layout.tsv"))) {
            var r=line.split("\t");
            switch(r[0]){
                case "SPACE" -> {
                    spaces.add(new NativeInvestigationScene.Space(r[1],n(r,2),d(r,3),d(r,4),d(r,5),d(r,6),
                        d(r,7),d(r,8),Boolean.parseBoolean(r[9]),true));
                    titles.put(r[1],r[10]);
                }
                case "ANCHOR" -> anchors.add(new Anchor(n(r,1),r[2],point(r,3)));
                case "SERVICE" -> sockets.add(new Socket(r[1],r[2],n(r,3),d(r,4),d(r,5),d(r,6),d(r,7),r[8]));
                case "COVER" -> sockets.add(new Socket(r[1],"hide",n(r,2),
                    d(r,3),d(r,4),d(r,5),d(r,6),"walk-in-cupboard"));
                case "SWITCH_FEET" -> require(switchFeet.put(n(r,1),point(r,2))==null,"Duplicate switch stance");
                case "TREAD" -> stairs.add(r[1]);
                case "RITUAL" -> sockets.add(new Socket("cemetery-"+r[1],"ritual",n(r,2),
                    d(r,3),d(r,4),d(r,5),0,r[1]));
                case "POINT" -> routes.computeIfAbsent(r[1],ignored->new ArrayList<>()).add(point(r,2));
                case "OPEN" -> openings.add(new Portal(n(r,1),n(r,2),n(r,3),d(r,4),d(r,5),d(r,6),
                    d(r,8),r[7].equals("z"),r[9],1));
                default -> throw new IllegalArgumentException("Unknown cemetery room row "+r[0]);
            }
        }
        var frames=new ArrayList<NativeInvestigationDoor.Spec>();
        var portals=new ArrayList<>(openings);
        for(String line:Files.readAllLines(folder.resolve("door-schedule.tsv"))){
            var r=line.split("\t");
            var portal=new Portal(n(r,0),n(r,4),n(r,5),d(r,6),d(r,7),d(r,8),d(r,9),r[10].equals("z"),"door",1);
            portals.add(portal);
            frames.add(new NativeInvestigationDoor.Spec(r[1],r[2],r[3],portal,d(r,11),d(r,12),d(r,13),
                n(r,14),n(r,15),n(r,16),r[17]));
        }
        NativeInvestigationScene scene;
        try(var model=Files.newInputStream(Path.of(args[1]));var collision=Files.newInputStream(Path.of(args[3]))) {
            scene=NativeInvestigationScene.read(model,args[2],collision,args[4],stairs,spaces,frames);
        }
        var rooms=new Room[HouseMap.ROOM_COUNT];
        for(var anchor:anchors){
            var at=anchor.feet();
            var zone=scene.spaceAt(at.x(),at.y(),at.z());
            require(zone!=null&&zone.id().equals(anchor.room())&&zone.owner()==anchor.owner(),
                "Incorrect cemetery anchor "+anchor+" / "+zone);
            rooms[anchor.owner()]=new Room(anchor.owner(),titles.get(zone.id()),zone.x0(),zone.z0(),zone.x1(),
                zone.z1(),zone.floor(),zone.height(),zone.outside()?"native-outdoor":"stone",
                zone.outside()?"outdoor":"stone",at);
        }
        var dock=new Dock(28,0,78,0);
        var van=new VanStaging(new VanStaging.Placement(dock.x(),dock.y(),dock.z(),dock.yaw()));
        var lip=van.world(0,VanStaging.FRONT);
        rooms[14]=new Room(14,"Expedition van",26.74,75,29.26,80.4,van.floorY(),VanStaging.INTERIOR_HEIGHT,
            "van","metal",new Vec(28,van.floorY(),78));
        portals.add(new Portal(920,13,14,lip.x(),van.floorY(),lip.z(),VanStaging.RAMP_HALF_WIDTH*2,true,"trail",1));
        var id=annex?"wld-07-mortuary":"wld-07";
        var level=new LevelDefinition(id,scene.revision(),annex?"Saint Bartholomew's Cemetery and Vale Mortuary":
            "Saint Bartholomew's Forgotten Cemetery",List.of(rooms),portals,List.of(),List.of(),List.of(),sockets,
            List.of(),dock,"woodland");
        var map=new HouseMap(HouseMap.Site.USER,level,null,scene);
        var witness=new HouseMap(HouseMap.Site.USER,level,null,scene);
        require(map.valid()&&RitualSites.valid(map),"Cemetery room graph, anchors or ritual sites failed");
        for(var opening:openings) {
            if(opening.kind().equals("stair")) continue;
            var a=NativeSchoolInteriorReview.point(opening,-.48,0,0);
            var b=NativeSchoolInteriorReview.point(opening,.48,0,0);
            require(opening.joins(map.roomAt(a.x(),a.y(),a.z()),map.roomAt(b.x(),b.y(),b.z())),
                "Open connection does not join its two physical rooms "+opening+" reached "+
                map.roomAt(a.x(),a.y(),a.z())+" and "+map.roomAt(b.x(),b.y(),b.z()));
            for(double height:POSTURES){
                NativeSchoolInteriorReview.walk(map,List.of(a,b),height,"Open connection "+opening.id());
                NativeSchoolInteriorReview.walk(map,List.of(b,a),height,"Open connection return "+opening.id());
            }
        }

        boolean cover=!HidingBudget.candidates(level).isEmpty();
        boolean powered=map.hasGenerator();
        var captures=folder.resolve(gpu?"captures/opengl":"captures");
        Files.createDirectories(captures);
        for(var frame:frames){
            var portal=frame.portal();
            require(NativeSchoolInteriorReview.sweep(scene.physics(),frame),"Unsupported operating swing "+frame.frameId());
            NativeSchoolInteriorReview.audit(map,frame,false);
            String label=cover?null:captureName(frame.frameId());
            if(label!=null) NativeSchoolInteriorReview.render(map,portal,captures.resolve(label+"-closed.png"),-frame.swingDirection());
            map.toggleDoor(portal.id(),System.nanoTime()-2_000_000_000L);
            witness.applyDoorSnapshot(map.doorSnapshot());
            NativeSchoolInteriorReview.audit(map,frame,true);
            NativeSchoolInteriorReview.audit(witness,frame,true);
            if(label!=null) NativeSchoolInteriorReview.render(map,portal,captures.resolve(label+"-open.png"),-frame.swingDirection());
            System.out.println("PASS fitted leaf and acoustics "+id+" "+frame.frameId());
        }
        var arrival=routes.get("L01");
        arrival.set(0,new Vec(lip.x(),van.floorY(),lip.z()));
        arrival.add(0,rooms[14].spawn());
        for(var route:routes.entrySet()){
            var reversed=new ArrayList<>(route.getValue());
            Collections.reverse(reversed);
            for(double height:POSTURES) {
                NativeSchoolInteriorReview.walk(map,route.getValue(),height,route.getKey());
                NativeSchoolInteriorReview.walk(map,reversed,height,route.getKey()+" return");
            }
        }
        if(!gpu&&!cover) {
            var frame=frames.stream().filter(f->f.frameId().equals(annex?"cold storage and preparation":"cottage S 1"))
                .findFirst().orElseThrow();
            liveDoor(level,scene,frame,annex);
        }
        var report=new ArrayList<>(List.of("check\tresult","native_scene_revision\t"+scene.revision(),"stage_revision\t"+level.revision(),
            "actual_fitted_doors\t"+frames.size(),"bound_native_room_volumes\t"+spaces.size(),
            "connected_stable_owners\t"+rooms.length,"retained_paths_both_ways_and_postures\t"+routes.size(),
            "physical_open_transitions\t"+(openings.size()-2),
            "standing_crouched_optical_object_acoustics_and_snapshots\tPASS"));
        if(!cover) report.add("four_peer_tcp_door_interaction\t"+(gpu?"covered by software run":"PASS"));
        report.add("renderer\t"+(gpu?GpuWorldRenderer.active().rendererDescription():"Software"));
        String prefix=!switchFeet.isEmpty()?"distribution":powered?"power":cover?"refuges":"doors";
        if(powered) {
            NativeSchoolInteriorReview.auditHiding(map,report);
            CemeteryPowerReview.audit(map,level,scene,routes.get("caretaker-service"),captures,report,gpu);
            if(!switchFeet.isEmpty()) {
                CemeteryDistributionReview.audit(map,scene,switchFeet,captures,report,annex,gpu);
            }
        } else if(cover) {
            CemeteryRefugeReview.audit(map,level,scene,captures,report,annex,gpu);
        }
        Files.writeString(folder.resolve(prefix+(gpu?"-opengl.tsv":"-software.tsv")),String.join("\n",report)+"\n");
        report.forEach(System.out::println);
    }

    private static String captureName(String frame) {
        return switch(frame) {
            case "cottage S 1" -> "caretaker-cottage";
            case "west-stair S 1" -> "west-ossuary-stair-pavilion";
            case "vehicle receiving door 1" -> "mortuary-receiving";
            case "cold storage and preparation" -> "mortuary-preparation";
            case "viewing and reception" -> "mortuary-viewing";
            default -> null;
        };
    }

    private static void liveDoor(LevelDefinition level,NativeInvestigationScene scene,NativeInvestigationDoor.Spec frame,
                                 boolean annex)throws Exception{
        var map=new HouseMap(HouseMap.Site.USER,level,null,scene);
        var replica=new HouseMap(HouseMap.Site.USER,level,null,scene);
        try(var host=new LanHost("127.0.0.1",0,GhostIdentity.Type.SPIRIT,CustomContract.standard(3),map);
            var scout=new Peer(host,false,"Threshold operator");var other=new Peer(host,false,"Threshold observer");
            var third=new Peer(host,false,"Cemetery crew");var ghost=new Peer(host,true,"Cemetery ghost")){
            for(var peer:List.of(scout,other,third,ghost)){
                peer.awaitExact("SITE|USER|"+level.revision());
                peer.client.ready();
            }
            scout.awaitExact("LOBBY|3|HUMAN|4|WAITING");
            scout.client.start();
            scout.await("INFO|mission started");
            int owner=annex?11:12;
            for(int room:NativeSchoolInteriorReview.missionRoute(map,owner)){
                scout.client.move(room);
                scout.awaitExact("ACCEPT|MOVE|"+room);
            }
            scout.client.view3D();
            scout.awaitExact("ACCEPT|VIEW3D");
            var feet=NativeSchoolInteriorReview.body(scout.await("BODY|0|"));
            require(feet.equals(map.spawn(owner)),"Wrong live owner at door approach");
            var p=frame.portal();
            if(!annex) feet=walkPeer(scout,feet,new Vec(p.x(),0,62));
            feet=walkPeer(scout,feet,NativeSchoolInteriorReview.point(p,.77,0,0));
            require(map.resolveMove(feet,p.x(),p.z()-.48,HunterPosture.STANDING_HEIGHT)==null,"Closed live approach crossed");
            var actor=new LooseClutter.Observer(0,feet.x(),feet.y(),feet.z(),Math.PI,-.11,true);
            var aim=InteractionFocus.pick(map,actor,List.of());
            require(aim!=null&&aim.kind()==InteractionFocus.Kind.DOOR&&aim.id().equals(Integer.toString(p.id())),
                "Live focus missed "+frame.frameId()+" at "+feet+" / "+aim);
            scout.client.turn(180);
            scout.client.interact(aim,Math.PI,-.11);
            scout.awaitExact("ACCEPT|INTERACT|Open door");
            var state=other.awaitExact("DOORS|"+p.id());
            replica.applyDoorSnapshot(state.substring("DOORS|".length()));
            NativeSchoolInteriorReview.audit(replica,frame,true);
            feet=walkPeer(scout,feet,NativeSchoolInteriorReview.point(p,-.77,0,0));
            var crossed=scene.spaceAt(feet.x(),feet.y()+1,feet.z());
            require(crossed!=null&&crossed.id().equals(annex?"mort-prep":"cottage"),"Live hunter missed receiving room");
        }
    }

    private static Vec walkPeer(Peer peer,Vec at,Vec target)throws Exception{
        for(int step=0;step<100;step++){
            double dx=target.x()-at.x(),dz=target.z()-at.z(),distance=Math.hypot(dx,dz);
            if(distance<.19)return at;
            double scale=Math.min(100/distance,310);
            at=NativeSchoolInteriorReview.step(peer,0,(int)Math.round(dx*scale),(int)Math.round(dz*scale));
        }
        throw new AssertionError("Physical live approach blocked at "+at+" toward "+target);
    }

    private static int n(String[] r,int i){return Integer.parseInt(r[i]);}
    private static double d(String[] r,int i){return Double.parseDouble(r[i]);}
    private static Vec point(String[] r,int i){return new Vec(d(r,i),d(r,i+1),d(r,i+2));}
    private static void require(boolean pass,String message){if(!pass)throw new AssertionError(message);}
}
