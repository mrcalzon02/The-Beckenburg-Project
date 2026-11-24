package org.beckenburg;

import org.beckenburg.LevelDefinition.Dock;
import org.beckenburg.LevelDefinition.Portal;
import org.beckenburg.LevelDefinition.Room;
import org.beckenburg.LevelDefinition.Socket;
import org.beckenburg.LevelDefinition.Vec;
import org.beckenburg.NativeSchoolInteriorReview.Peer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Runs the Monastery’s original geometry with its newly fitted dynamic doors. */
public final class MonasteryDoorReview {
    private record Scene(LevelDefinition level, NativeInvestigationScene nativeScene, Map<String,List<Vec>> paths) {
        HouseMap map(){return new HouseMap(HouseMap.Site.USER,level,null,nativeScene);}
    }

    private MonasteryDoorReview() {}

    public static void main(String[] args)throws Exception {
        var packet=Path.of(args[0]);
        var geometry=Path.of(args[1]);
        if(!List.of(args).contains("--gpu")){
            review(packet,geometry,false);
            return;
        }
        var desktop=Class.forName("org.beckenburg.gpu.GpuDesktop");
        var start=desktop.getDeclaredMethod("startGpu");
        var stop=desktop.getDeclaredMethod("stopReviewGpu");
        start.setAccessible(true);
        stop.setAccessible(true);
        start.invoke(null);
        try{review(packet,geometry,true);}finally{stop.invoke(null);}
    }

    private static Scene read(Path packet,Path geometry)throws Exception {
        var rooms=new Room[HouseMap.ROOM_COUNT];
        var spaces=new ArrayList<NativeInvestigationScene.Space>();
        var portals=new ArrayList<Portal>();
        var sockets=new ArrayList<Socket>();
        var specs=new ArrayList<NativeInvestigationDoor.Spec>();
        var stairs=new HashSet<String>();
        var paths=new LinkedHashMap<String,List<Vec>>();
        for(var line:Files.readAllLines(packet.resolve("runtime-input.tsv"))){
            var p=line.split("\t");
            switch(p[0]){
                case "ROOM","SPACE" -> {
                    var s=new NativeInvestigationScene.Space(p[1],n(p,2),d(p,3),d(p,4),d(p,5),d(p,6),
                        d(p,7),d(p,8),Boolean.parseBoolean(p[9]),true);
                    spaces.add(s);
                    if(p[0].equals("ROOM")){
                        require(rooms[s.owner()]==null,"Duplicate site owner "+s.owner());
                        rooms[s.owner()]=new Room(s.owner(),p[10],s.x0(),s.z0(),s.x1(),s.z1(),
                            s.floor(),s.height(),s.outside()?"native-outdoor":"stone",
                            s.outside()?"outdoor":"stone",point(p,11));
                    }
                }
                case "STEP" -> stairs.add(p[1]);
                case "RITUAL" -> sockets.add(new Socket("monastery-"+p[1],"ritual",n(p,2),d(p,3),d(p,4),d(p,5),0,p[1]));
                case "OPEN" -> portals.add(new Portal(n(p,1),n(p,2),n(p,3),d(p,4),d(p,5),d(p,6),
                    d(p,7),p[8].equals("z"),p[9],1));
                case "POINT" -> paths.computeIfAbsent(p[1],id->new ArrayList<>()).add(point(p,2));
                default -> throw new IllegalArgumentException("Unrecognized Monastery runtime row "+line);
            }
        }
        for(var line:Files.readAllLines(packet.resolve("door-schedule.tsv"))){
            var p=line.split("\t");
            var portal=new Portal(n(p,0),n(p,4),n(p,5),d(p,6),d(p,7),d(p,8),d(p,9),p[10].equals("z"),"door",1);
            portals.add(portal);
            specs.add(new NativeInvestigationDoor.Spec(p[1],p[2],p[3],portal,d(p,11),d(p,12),d(p,13),
                n(p,14),n(p,15),n(p,16),p[17]));
        }
        var dock=new Dock(32,0,80,0);
        var van=new VanStaging(new VanStaging.Placement(dock.x(),dock.y(),dock.z(),dock.yaw()));
        rooms[14]=new Room(14,"Expedition van",30.74,77,33.26,82.4,van.floorY(),VanStaging.INTERIOR_HEIGHT,
            "van","metal",new Vec(32,van.floorY(),80));
        var model=geometry.resolve("monastery-cloister.glb");
        var collision=geometry.resolve("monastery-cloister.hcs2");
        NativeInvestigationScene scene;
        try(var src=Files.newInputStream(model);var shapes=Files.newInputStream(collision)){
            scene=NativeInvestigationScene.read(src,sha(model),shapes,sha(collision),stairs,spaces,specs);
        }
        var level=new LevelDefinition("wld-10",scene.revision(),"Monastery of Ashen Bells",
            List.of(rooms),portals,List.of(),List.of(),List.of(),sockets,List.of(),dock,"highland",
            WorldWater.defaults("wld-10"));
        return new Scene(level,scene,paths);
    }

    private static void review(Path packet,Path geometry,boolean gpu)throws Exception {
        var data=read(packet,geometry);
        var map=data.map();
        var replica=data.map();
        var captures=packet.resolve(gpu?"captures/opengl":"captures/software");
        Files.createDirectories(captures);
        var report=new ArrayList<>(List.of("check\tresult","native_scene_revision\t"+data.nativeScene().revision(),
            "level_revision\t"+data.level().revision(),"collision_sha256\t"+data.nativeScene().physics().sha256(),
            "renderer\t"+(gpu?GpuWorldRenderer.active().rendererDescription():"Software")));
        require(map.valid()&&RitualSites.valid(map),"Monastery staged room/spawn/ritual graph is invalid");
        for(var door:data.nativeScene().doors()){
            var frame=door.spec();
            var portal=frame.portal();
            require(NativeSchoolInteriorReview.sweep(data.nativeScene().physics(),frame),
                "Door sweep contacts a nearby wall or landing "+frame.frameId());
            NativeSchoolInteriorReview.audit(map,frame,false);
            String image=switch(portal.id()){
                case 930 -> "public-gate";
                case 933 -> "garden-door";
                case 942 -> "bell-tower-entry";
                case 946 -> "memorial-chamber";
                default -> null;
            };
            if(image!=null)NativeSchoolInteriorReview.render(map,portal,captures.resolve(image+"-closed.png"),-frame.swingDirection());
            map.toggleDoor(portal.id(),System.nanoTime()-2_000_000_000L);
            replica.applyDoorSnapshot(map.doorSnapshot());
            NativeSchoolInteriorReview.audit(map,frame,true);
            NativeSchoolInteriorReview.audit(replica,frame,true);
            if(image!=null)NativeSchoolInteriorReview.render(map,portal,captures.resolve(image+"-open.png"),-frame.swingDirection());
            report.add("door_"+portal.id()+"\tPASS "+frame.frameId()+" closed, open, replicated, acoustic and optical");
            System.out.println(report.get(report.size()-1));
        }
        require(data.nativeScene().doors().size()==13,"Missing Monastery operating threshold");
        int directions=0;
        for(var path:data.paths().entrySet()){
            var back=new ArrayList<>(path.getValue());
            Collections.reverse(back);
            for(double posture:new double[]{HunterPosture.STANDING_HEIGHT,HunterPosture.CROUCH_HEIGHT}){
                NativeSchoolInteriorReview.walk(map,path.getValue(),posture,path.getKey());
                NativeSchoolInteriorReview.walk(map,back,posture,path.getKey()+" reverse");
                directions+=2;
            }
        }
        report.add("directional_route_replays\t"+directions);
        if(!gpu&&!Boolean.getBoolean("beckenburg.monastery.skipLive"))live(data,report);
        Files.write(packet.resolve(gpu?"doors-opengl.tsv":"doors-software.tsv"),report);
        report.forEach(System.out::println);
    }

    private static void live(Scene data,List<String> report)throws Exception {
        var map=data.map();
        var replica=data.map();
        try(var host=new LanHost("127.0.0.1",0,GhostIdentity.Type.SPIRIT,CustomContract.standard(3),map);
            var lead=new Peer(host,false,"Monastery scout");
            var witness=new Peer(host,false,"Gate observer");
            var crew=new Peer(host,false,"Garden investigator");
            var ghost=new Peer(host,true,"Monastery ghost")) {
            var identity="SITE|USER|"+data.level().revision();
            for(var peer:List.of(lead,witness,crew,ghost)){
                peer.awaitExact(identity);
                peer.client.ready();
            }
            lead.awaitExact("LOBBY|3|HUMAN|4|WAITING");
            lead.client.start();
            lead.await("INFO|mission started");
            lead.client.view3D();
            lead.awaitExact("ACCEPT|VIEW3D");
            var at=NativeSchoolInteriorReview.body(lead.await("BODY|0|"));
            require(map.roomAt(at.x(),at.y(),at.z())==14,"Case did not begin in the actual van");
            for(int i=0;i<15;i++)at=NativeSchoolInteriorReview.step(lead,0,0,-100);
            at=NorthwatchDoorRuntime.walk(lead,at,new Vec(32,0,68));
            at=NorthwatchDoorRuntime.open(map,replica,data.nativeScene(),lead,witness,at,930);
            at=NorthwatchDoorRuntime.walk(lead,at,new Vec(32,0,59));
            at=NorthwatchDoorRuntime.open(map,replica,data.nativeScene(),lead,witness,at,931);
            at=NorthwatchDoorRuntime.walk(lead,at,new Vec(32,0,54.9),new Vec(29,0,53));
            at=NorthwatchDoorRuntime.open(map,replica,data.nativeScene(),lead,witness,at,933);
            at=NorthwatchDoorRuntime.walk(lead,at,new Vec(29,0,36));
            require(map.roomAt(at.x(),at.y(),at.z())==12,"Joined case did not reach the herb garden");
            report.add("four_peer_van_to_herb_court\tPASS public gate, inner gate and garden leaf replicated");
            at=NorthwatchDoorRuntime.walk(lead,at,new Vec(29,0,23.8));
            at=NorthwatchDoorRuntime.open(map,replica,data.nativeScene(),lead,witness,at,936);
            at=NorthwatchDoorRuntime.walk(lead,at,new Vec(29,0,20.98),new Vec(23.6,0,20.98),
                new Vec(23.6,0,17),new Vec(29,0,17));
            for(var knot:data.paths().get("L19"))at=CemeteryRefugeReview.walk(lead,0,at,knot);
            require(map.roomAt(at.x(),at.y(),at.z())==8,"Live stairs did not reach the lower passage");
            at=NorthwatchDoorRuntime.walk(lead,at,new Vec(21.25,-3,17.2),new Vec(18,-3,17.2));
            at=NorthwatchDoorRuntime.open(map,replica,data.nativeScene(),lead,witness,at,946);
            at=NorthwatchDoorRuntime.walk(lead,at,new Vec(18,-3,9));
            require(map.roomAt(at.x(),at.y(),at.z())==9&&replica.doorOpen(946),"Live chamber threshold did not replicate");
            report.add("four_peer_garden_to_lower_memorial_chamber\tPASS existing stairs and five real leaves from cargo");
        }
    }

    private static int n(String[] p,int f){return Integer.parseInt(p[f]);}
    private static double d(String[] p,int f){return Double.parseDouble(p[f]);}
    private static Vec point(String[] p,int f){return new Vec(d(p,f),d(p,f+1),d(p,f+2));}
    private static String sha(Path p)throws Exception{
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(p)));
    }
    private static void require(boolean ok,String reason){if(!ok)throw new AssertionError(reason);}
}
