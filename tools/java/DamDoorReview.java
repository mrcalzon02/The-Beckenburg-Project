package org.beckenburg;

import dev.mrcalzon.water.WaterDocument;
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

/** Exercises operating Dam leaves in the shared game, including live client movement. */
public final class DamDoorReview {
    private static final double[] POSTURES={HunterPosture.STANDING_HEIGHT,HunterPosture.CROUCH_HEIGHT};
    private record Scene(LevelDefinition level,NativeInvestigationScene nativeScene,
                         List<NativeInvestigationDoor.Spec> doors,Map<String,List<Vec>> paths) {
        HouseMap map(){return new HouseMap(HouseMap.Site.USER,level,null,nativeScene);}
    }
    private DamDoorReview(){}

    public static void main(String[] args)throws Exception {
        var folder=Path.of(args[0]);
        var waterPath=Path.of(args[1]);
        boolean gpu=List.of(args).contains("--gpu");
        if(!gpu){review(folder,waterPath,false);return;}
        var desktop=Class.forName("org.beckenburg.gpu.GpuDesktop");
        var start=desktop.getDeclaredMethod("startGpu");
        var stop=desktop.getDeclaredMethod("stopReviewGpu");
        start.setAccessible(true);
        stop.setAccessible(true);
        start.invoke(null);
        try{review(folder,waterPath,true);}finally{stop.invoke(null);}
    }

    private static Scene read(Path folder,Path waterPath)throws Exception {
        var rooms=new Room[HouseMap.ROOM_COUNT];
        var spaces=new ArrayList<NativeInvestigationScene.Space>();
        var stairs=new HashSet<String>();
        var specs=new ArrayList<NativeInvestigationDoor.Spec>();
        var sockets=new ArrayList<Socket>();
        var portals=new ArrayList<Portal>();
        var paths=new LinkedHashMap<String,List<Vec>>();
        for(String line:Files.readAllLines(folder.resolve("runtime-input.tsv"))){
            var p=line.split("\t");
            switch(p[0]){
                case "ROOM","SPACE" -> {
                    var s=new NativeInvestigationScene.Space(p[1],n(p,2),d(p,3),d(p,4),d(p,5),
                        d(p,6),d(p,7),d(p,8),Boolean.parseBoolean(p[9]),!p[1].equals("dam-closed-channel"));
                    spaces.add(s);
                    if(p[0].equals("ROOM")){
                        rooms[s.owner()]=new Room(s.owner(),p[10],s.x0(),s.z0(),s.x1(),s.z1(),
                            s.floor(),s.height(),s.outside()?"native-outdoor":"metal",
                            s.outside()?"outdoor":"concrete",point(p,11));
                    }
                }
                case "STEP" -> stairs.add(p[1]);
                case "RITUAL" -> sockets.add(new Socket("dam-"+p[1],"ritual",n(p,2),d(p,3),d(p,4),d(p,5),0,p[1]));
                case "OPEN" -> portals.add(new Portal(n(p,1),n(p,2),n(p,3),d(p,4),d(p,5),d(p,6),
                    d(p,7),p[8].equals("z"),p[9],1));
                case "POINT" -> paths.computeIfAbsent(p[1],id->new ArrayList<>()).add(point(p,2));
                default -> throw new IllegalArgumentException("Unhandled Dam runtime row "+line);
            }
        }
        for(String line:Files.readAllLines(folder.resolve("door-schedule.tsv"))){
            var p=line.split("\t");
            var portal=new Portal(n(p,0),n(p,4),n(p,5),d(p,6),d(p,7),d(p,8),d(p,9),p[10].equals("z"),"door",1);
            portals.add(portal);
            specs.add(new NativeInvestigationDoor.Spec(p[1],p[2],p[3],portal,d(p,11),d(p,12),d(p,13),
                n(p,14),n(p,15),n(p,16),p[17]));
        }
        var dock=new Dock(36,0,82,0);
        var van=new VanStaging(new VanStaging.Placement(dock.x(),dock.y(),dock.z(),dock.yaw()));
        rooms[14]=new Room(14,"Expedition van",34.74,79,37.26,84.4,van.floorY(),VanStaging.INTERIOR_HEIGHT,
            "van","metal",new Vec(36,van.floorY(),82));
        var glb=folder.resolve("door-fitted-dam.glb");
        var collision=folder.resolve("door-fitted-dam.hcs2");
        NativeInvestigationScene scene;
        try(var model=Files.newInputStream(glb);var physics=Files.newInputStream(collision)){
            scene=NativeInvestigationScene.read(model,sha(glb),physics,sha(collision),stairs,spaces,specs);
        }
        var water=WaterDocument.read(Files.newInputStream(waterPath));
        var level=new LevelDefinition("wld-09",scene.revision(),"Blackwater Hydroelectric Dam",
            List.of(rooms),portals,List.of(),List.of(),List.of(),sockets,List.of(),dock,"highland",water);
        return new Scene(level,scene,List.copyOf(specs),paths);
    }

    private static void review(Path folder,Path water,boolean gpu)throws Exception {
        var data=read(folder,water);
        var map=data.map();
        var replica=data.map();
        var captures=folder.resolve(gpu?"captures/opengl":"captures/software");
        Files.createDirectories(captures);
        var report=new ArrayList<>(List.of("check\tresult",
            "native_scene_revision\t"+data.nativeScene().revision(),
            "collision_sha256\t"+data.nativeScene().physics().sha256(),
            "level_revision\t"+data.level().revision(),
            "water_revision\t"+data.level().water().revision(),
            "renderer\t"+(gpu?GpuWorldRenderer.active().rendererDescription():"Software")));
        require(data.doors().size()==14,"Unexpected fitted Dam door count");
        require(map.valid(),"Dam ownership or spawns are disconnected "+data.level().rooms().stream().filter(r->!map.traversable(r.spawn().x(),r.spawn().y(),r.spawn().z())).map(Room::id).toList());
        require(RitualSites.valid(map),"Dam ritual reservations are disconnected "+RitualSites.all(map));
        for(var frame:data.doors()){
            var p=frame.portal();
            require(NativeSchoolInteriorReview.sweep(data.nativeScene().physics(),frame),
                "The Dam leaf hits an existing surface or unsupported floor "+frame.frameId());
            NativeSchoolInteriorReview.audit(map,frame,false);
            String view=switch(p.id()){
                case 870 -> "crest-control";
                case 876 -> "turbine-east";
                case 879 -> "spillway";
                case 881 -> "gallery-valve";
                default -> null;
            };
            if(view!=null)NativeSchoolInteriorReview.render(map,p,captures.resolve(view+"-closed.png"),-frame.swingDirection());
            map.toggleDoor(p.id(),System.nanoTime()-2_000_000_000L);
            require(map.doorOpen(p.id()),"Door toggle was not accepted "+p.id());
            replica.applyDoorSnapshot(map.doorSnapshot());
            NativeSchoolInteriorReview.audit(map,frame,true);
            NativeSchoolInteriorReview.audit(replica,frame,true);
            if(view!=null)NativeSchoolInteriorReview.render(map,p,captures.resolve(view+"-open.png"),-frame.swingDirection());
            report.add("door_"+p.id()+"\tPASS "+frame.frameId()+" fitted swing, body, optical, objects and acoustics");
            System.out.println(report.get(report.size()-1));
        }
        int bypass=0;
        for(double x:new double[]{43.82,44.02,44.17})for(double y:new double[]{-9.65,-8.8,-8.03}){
            require(!map.clearLineOfSight(x,y,31.52,x,y,32.49),"Restored valve jamb left a gap");
            bypass++;
        }
        int routes=0;
        for(var link:data.paths().entrySet()){
            var backward=new ArrayList<>(link.getValue());
            Collections.reverse(backward);
            for(double height:POSTURES){
                NativeSchoolInteriorReview.walk(map,link.getValue(),height,"Dam "+link.getKey());
                NativeSchoolInteriorReview.walk(map,backward,height,"Dam reverse "+link.getKey());
            }
            routes++;
        }
        var van=map.van();
        var inside=van.world(0,0);
        var toe=van.world(0,VanStaging.FRONT);
        var cargoRoute=List.of(new Vec(inside.x(),van.floorY(),inside.z()),
            new Vec(toe.x(),van.floorY(),toe.z()),new Vec(36,0,75.45),new Vec(36,0,69));
        for(double height:POSTURES){
            NativeSchoolInteriorReview.walk(map,cargoRoute,height,"canonical cargo ramp");
            var backward=new ArrayList<>(cargoRoute);
            Collections.reverse(backward);
            NativeSchoolInteriorReview.walk(map,backward,height,"canonical cargo ramp return");
        }
        report.add("fitted_operating_doorways\t"+data.doors().size());
        report.add("valve_jamb_bypass_rays_blocked\t"+bypass);
        report.add("owned_routes_both_ways_and_postures\t"+routes);
        report.add("cargo_retreat_both_ways_and_postures\tPASS");
        for(int room=0;room<map.roomCount();room++){
            var start=map.spawn(room);
            require(map.roomAt(start.x(),start.y(),start.z())==room
                 &&map.traversable(start.x(),start.y(),start.z()),"Unsupported room spawn "+room+" "+start);
        }
        report.add("owned_supported_spawns\t"+map.roomCount());
        if(!gpu)live(data,report);
        Files.writeString(folder.resolve(gpu?"door-opengl.tsv":"door-software.tsv"),String.join("\n",report)+"\n");
        report.forEach(System.out::println);
    }

    private static void live(Scene scene,List<String> report)throws Exception {
        var hostMap=scene.map();
        var replica=scene.map();
        try(var host=new LanHost("127.0.0.1",0,GhostIdentity.Type.SPIRIT,CustomContract.standard(3),hostMap);
            var leader=new Peer(host,false,"Dam field engineer");
            var witness=new Peer(host,false,"Van observer");
            var crew=new Peer(host,false,"Dam crew");
            var ghost=new Peer(host,true,"Dam ghost")){
            for(var peer:List.of(leader,witness,crew,ghost)){
                peer.awaitExact("SITE|USER|"+scene.level().revision());
                peer.client.ready();
            }
            leader.awaitExact("LOBBY|3|HUMAN|4|WAITING");
            leader.client.start();
            leader.await("INFO|mission started");
            leader.client.view3D();
            leader.awaitExact("ACCEPT|VIEW3D");
            var at=NativeSchoolInteriorReview.body(leader.await("BODY|0|"));
            require(hostMap.roomAt(at.x(),at.y(),at.z())==14,"Dam case missed canonical van spawn");
            for(int i=0;i<15;i++)at=NativeSchoolInteriorReview.step(leader,0,0,-100);
            at=NorthwatchDoorRuntime.walk(leader,at,new Vec(36,0,69),new Vec(40,0,69),
                                         new Vec(40,0,52),new Vec(53,0,52),new Vec(64,0,52),new Vec(68,0,52));
            at=NorthwatchDoorRuntime.open(hostMap,replica,scene.nativeScene(),leader,witness,at,877);
            var path=scene.paths().get("L12");
            for(var p:path.subList(0,path.indexOf(new Vec(68,-6,48))))at=CemeteryRefugeReview.walk(leader,0,at,p);
            at=NorthwatchDoorRuntime.open(hostMap,replica,scene.nativeScene(),leader,witness,at,879);
            at=NorthwatchDoorRuntime.walk(leader,at,new Vec(68,-6,50.3),new Vec(70.8,-6,50.3));
            require(hostMap.roomAt(at.x(),at.y(),at.z())==9,"Live case missed the lower spillway floor");
            require(replica.doorOpen(877)&&replica.doorOpen(879),"Witness lost the two physical door states");
            report.add("live_van_to_lower_spillway_with_replicated_leaves\tPASS");
        }
    }

    private static String sha(Path path)throws Exception{
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }
    private static Vec point(String[] p,int offset){return new Vec(d(p,offset),d(p,offset+1),d(p,offset+2));}
    private static int n(String[] p,int field){return Integer.parseInt(p[field]);}
    private static double d(String[] p,int field){return Double.parseDouble(p[field]);}
    private static void require(boolean value,String reason){if(!value)throw new AssertionError(reason);}
}
