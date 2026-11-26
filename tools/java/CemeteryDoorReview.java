package org.beckenburg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import org.beckenburg.LevelDefinition.Vec;
import org.beckenburg.NativeSchoolInteriorReview.Peer;

/** Exact staged Cemetery and Mortuary door, room, movement, acoustic and LAN checks. */
public final class CemeteryDoorReview {
    private static final double[] POSTURES={HunterPosture.STANDING_HEIGHT,HunterPosture.CROUCH_HEIGHT};

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
        boolean named=List.of(args).contains("--named");
        var runtimeSite=named?(annex?HouseMap.Site.MORTUARY:HouseMap.Site.CEMETERY):HouseMap.Site.USER;
        CemeteryNativeLevel.Source source;
        try(var layout=Files.newInputStream(folder.resolve("room-layout.tsv"));
            var schedule=Files.newInputStream(folder.resolve("door-schedule.tsv"));
            var model=Files.newInputStream(Path.of(args[1]));var physics=Files.newInputStream(Path.of(args[3]))) {
            source=CemeteryNativeLevel.read(annex,WharfNativeLevel.rows(layout),WharfNativeLevel.rows(schedule),
                model,args[2],physics,args[4]);
        }
        if(named) {
            var packaged=CemeteryNativeLevel.packaged(annex);
            require(packaged.level().equals(source.level())&&packaged.scene().revision().equals(source.scene().revision()),
                "Packed Cemetery geometry differs from the staged maps");
            source=packaged;
        }
        var level=source.level();
        var scene=source.scene();
        var frames=source.doors();
        var openings=source.openings();
        var switchFeet=source.switchFeet();
        var stories=source.stories();
        var routes=new LinkedHashMap<>(source.routes());
        var dock=level.dock();
        var van=new VanStaging(new VanStaging.Placement(dock.x(),dock.y(),dock.z(),dock.yaw()));
        var lip=van.world(0,VanStaging.FRONT);
        var id=level.id();
        var map=named?new HouseMap(runtimeSite):new HouseMap(runtimeSite,level,null,scene);
        var witness=named?new HouseMap(runtimeSite):new HouseMap(runtimeSite,level,null,scene);
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
        var arrival=new ArrayList<>(routes.get("L01"));
        arrival.set(0,new Vec(lip.x(),van.floorY(),lip.z()));
        arrival.add(0,level.room(14).spawn());
        routes.put("L01",arrival);
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
            "actual_fitted_doors\t"+frames.size(),"bound_native_room_volumes\t"+countSpaces(folder),
            "connected_stable_owners\t"+level.rooms().size(),"retained_paths_both_ways_and_postures\t"+routes.size(),
            "physical_open_transitions\t"+(openings.size()-2),
            "standing_crouched_optical_object_acoustics_and_snapshots\tPASS"));
        if(!cover) report.add("four_peer_tcp_door_interaction\t"+(gpu?"covered by software run":"PASS"));
        report.add("renderer\t"+(gpu?GpuWorldRenderer.active().rendererDescription():"Software"));
        String prefix=named?"named":!stories.isEmpty()?"furnishings":!switchFeet.isEmpty()?"distribution":powered?"power":cover?"refuges":"doors";
        if(powered) {
            if(named) CemeteryRefugeReview.audit(map,level,scene,captures,report,annex,gpu);
            else NativeSchoolInteriorReview.auditHiding(map,report);
            CemeteryPowerReview.audit(map,level,scene,routes.get("caretaker-service"),captures,report,gpu);
            if(!switchFeet.isEmpty()) {
                CemeteryDistributionReview.audit(map,scene,switchFeet,captures,report,annex,gpu);
            }
        } else if(cover) {
            CemeteryRefugeReview.audit(map,level,scene,captures,report,annex,gpu);
        }
        if(!stories.isEmpty()) CemeteryFurnishingsReview.audit(map,scene,stories,captures,report,annex,gpu);
        if(named) CemeteryNamedCaseReview.audit(map,source,report,gpu);
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

    private static long countSpaces(Path folder)throws Exception {
        try(var rows=Files.lines(folder.resolve("room-layout.tsv"))) {
            return rows.filter(row->row.startsWith("SPACE\t")).count();
        }
    }

    private static void require(boolean pass,String message){if(!pass)throw new AssertionError(message);}
}
