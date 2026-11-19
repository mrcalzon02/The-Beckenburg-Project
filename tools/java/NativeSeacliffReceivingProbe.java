package org.beckenburg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import javax.imageio.ImageIO;
import org.beckenburg.LevelDefinition.Dock;
import org.beckenburg.LevelDefinition.Portal;
import org.beckenburg.LevelDefinition.Room;
import org.beckenburg.LevelDefinition.Socket;
import org.beckenburg.LevelDefinition.Vec;

/** Exercise the exact Battery journey through the shared production game kernel. */
public final class NativeSeacliffReceivingProbe {
    private static double d(String[] row,int index){return Double.parseDouble(row[index]);}
    private static Vec point(String[] row,int index){return new Vec(d(row,index),d(row,index+1),d(row,index+2));}
    private static void check(boolean valid,String why){if(!valid)throw new AssertionError(why);}

    private static String walk(HouseMap map,List<Vec> route,double height){
        Vec at=route.get(0);
        if(!map.traversable(at.x(),at.y(),at.z(),height))return "unsupported start "+at;
        for(int i=1;i<route.size();i++){
            Vec start=at,end=route.get(i);
            int steps=Math.max(1,(int)Math.ceil(Math.hypot(end.x()-at.x(),end.z()-at.z())/.13));
            for(int n=1;n<=steps;n++){
                double t=(double)n/steps;
                double x=start.x()+t*(end.x()-start.x()),z=start.z()+t*(end.z()-start.z());
                Vec next=map.resolveMove(at,x,z,height);
                if(next==null)return "blocked segment "+i+" at "+at+" toward "+x+","+z;
                at=next;
            }
            if(Math.abs(at.y()-end.y())>.045)return "wrong receiving height "+at+" for "+end;
        }
        return "";
    }

    private static void cross(HouseMap map,List<Vec> path,double height,String label){
        String failure=walk(map,path,height);
        check(failure.isEmpty(),label+" "+failure);
    }

    private static int doorState(HouseMap map,Portal p,boolean open){
        double x=p.x(),y=p.y(),z=p.z();
        Vec gallery=new Vec(x,y,z+.72),bay=new Vec(x,y,z-.72);
        check(map.roomAt(x,y,gallery.z())==p.a()&&map.roomAt(x,y,bay.z())==p.b(),"Threshold room ownership");
        check(map.doorOpen(p.id())==open,"Incorrect fitted door pose");
        for(double h:new double[]{HunterPosture.STANDING_HEIGHT,HunterPosture.CROUCH_HEIGHT}){
            check(walk(map,List.of(gallery,bay),h).isEmpty()==open,"Inbound door state");
            check(walk(map,List.of(bay,gallery),h).isEmpty()==open,"Outbound door state");
        }
        for(double height:new double[]{.3,1.24,2.14})
            check(map.clearLineOfSight(x,y+height,gallery.z(),x,y+height,bay.z())==open,"Sight through fitted aperture");
        check(!map.clearLineOfSight(x+1.1,y+1.2,gallery.z(),x+1.1,y+1.2,bay.z()),"Light leak beside frame");
        check(map.clearObjectPath(x,y+1,gallery.z(),x,y+1,bay.z(),.06,.06)==open,"Object path through fitted aperture");
        check(map.clearInteractionSight(x,y+1.2,gallery.z(),x,y+1.2,bay.z(),p.id()),"Leaf interaction occluded");
        return LanAcoustics.physicalCostAt(map,p.a(),gallery,p.b(),bay);
    }

    private static int[] capture(SceneRenderer renderer,HouseMap map,Path file,Vec at,double yaw,double pitch)throws Exception{
        check(map.traversable(at.x(),at.y(),at.z()),"Unsupported standing view "+at);
        renderer.x=at.x();renderer.z=at.z();renderer.eyeHeight=at.y()+HunterPosture.STANDING_EYE;
        renderer.yaw=yaw;renderer.pitch=pitch;
        renderer.drawNetwork(map,List.of(),-1,0);
        var gpu=GpuWorldRenderer.active();
        if(gpu!=null){
            check(renderer.awaitSubmittedGpuPrewarm(30_000_000_000L,()->false),"No native OpenGL frame");
            renderer.pinCompletedGpuFrame(gpu.latest());
            renderer.drawNetwork(map,List.of(),-1,0);
            check(renderer.frameDiagnostics().gpuReadbackAvailable(),"Missing exact hardware readback");
        }
        check(renderer.frameDiagnostics().faces()>500&&renderer.frameDiagnostics().frustumFaces()>35,"Native geometry missing in frame");
        var frame=renderer.image();
        check(ImageIO.write(frame,"png",file.toFile()),"PNG encoder unavailable");
        return frame.getRGB(0,0,840,560,null,0,840);
    }

    private static LooseClutter.Observer aim(HouseMap map, Socket s, InteractionFocus.Kind kind) {
        for (double distance : new double[]{.65,.85,1.10,1.35,1.60})
            for (double tangent : new double[]{0,-.30,.30}) {
                double x=s.x()-distance, z=s.z()+tangent;
                double y=map.level().room(s.room()).y();
                if(!map.traversable(x,y,z)||map.roomAt(x,y,z)!=s.room())continue;
                double yaw=Math.atan2(s.x()-x,s.z()-z);
                double targetHeight=s.y()+(kind==InteractionFocus.Kind.GENERATOR?.10:0);
                double pitch=Math.atan2(targetHeight-y-HunterPosture.STANDING_EYE,Math.hypot(s.x()-x,s.z()-z));
                var viewer=new LooseClutter.Observer(0,x,y,z,yaw,pitch,true);
                var picked=InteractionFocus.pick(map,viewer,List.of());
                String id=kind==InteractionFocus.Kind.SWITCH?Integer.toString(s.room()):s.id();
                if(picked!=null&&picked.kind()==kind&&picked.id().equals(id))return viewer;
            }
        throw new AssertionError("No supported aim on "+s);
    }

    private static double light(FixtureLighting field, Socket s) {
        return field.sampleNormal(s.room(),s.x(),s.y()-.65,s.z(),0,1,0).energy();
    }

    private static List<String> power(HouseMap map, SceneRenderer renderer, Path folder) throws Exception {
        var starter=map.level().socket("generator",9);
        var panel=map.level().socket("breaker",9);
        var power=new RoomLighting();
        var field=new FixtureLighting(map);
        var loads=map.level().sockets().stream().filter(s->s.type().equals("light")).toList();
        check(loads.size()==7,"Seven physically specified load sockets");
        var starterPose=aim(map,starter,InteractionFocus.Kind.GENERATOR);
        var panelPose=aim(map,panel,InteractionFocus.Kind.BREAKER);
        check(RoomLighting.breakerAimed(map,panelPose.x(),panelPose.y(),panelPose.z(),panelPose.yaw()),"Breaker shortcut aim");
        for(int room:new int[]{6,9}) {
            var sw=map.level().socket("switch",room);
            var pose=aim(map,sw,InteractionFocus.Kind.SWITCH);
            check(power.aimed(map,pose.x(),pose.y(),pose.z(),pose.yaw())==room,"Switch owner");
        }
        for(double posture:new double[]{HunterPosture.STANDING_HEIGHT,HunterPosture.CROUCH_HEIGHT}) {
            var middle=new Vec(57.0,0,21.65);
            cross(map,List.of(map.spawn(9),middle,new Vec(57.0,0,starterPose.z()),
                new Vec(starterPose.x(),0,starterPose.z())),posture,"Walk to diesel starter");
            cross(map,List.of(map.spawn(9),middle,new Vec(57.0,0,panelPose.z()),
                new Vec(panelPose.x(),0,panelPose.z())),posture,"Walk to distribution board");
        }
        var stair = new ArrayList<>(List.of(map.spawn(6),new Vec(48,4,22),new Vec(48,4,16),
            new Vec(48.8,4,16),new Vec(56.32,0,16),new Vec(56.95,0,16),new Vec(56.95,0,18),
            new Vec(56,0,19),new Vec(56,0,22),map.spawn(9)));
        for(double posture:new double[]{HunterPosture.STANDING_HEIGHT,HunterPosture.CROUCH_HEIGHT}) {
            cross(map,stair,posture,"Upper switch room to lower plant");
            var returnTrip=new ArrayList<>(stair);
            Collections.reverse(returnTrip);
            cross(map,returnTrip,posture,"Lower plant to upper switch room");
        }
        field.frame(power,0);
        var on=loads.stream().mapToDouble(s->light(field,s)).toArray();
        int tick=0;
        for(int room:new int[]{6,9}){
            power.toggle(room);
            field.frame(power,++tick);
            for(int i=0;i<loads.size();i++) {
                var lamp=loads.get(i);
                check(lamp.room()==room?light(field,lamp)<on[i]-.015:Math.abs(light(field,lamp)-on[i])<.01,
                    "Room switch disagrees with physical load "+lamp.id());
            }
            power.toggle(room);
        }
        power.setSourceRunning(false);
        field.frame(power,++tick);
        check(power.breaker()&&!power.supplied(),"Generator changed breaker state");
        for(int i=0;i<loads.size();i++)
            check(light(field,loads.get(i))<on[i]-.015,"Generator did not darken "+loads.get(i).id());
        power.setBreaker(false);
        power.setSourceRunning(true);
        check(!power.supplied(),"Restart incorrectly repaired the tripped breaker");
        renderer.lamp=false;
        renderer.roomLights=power;
        var view=new Vec(55.1,0,19.15);
        int[] failed=capture(renderer,map,folder.resolve("battery-plant-blackout-runtime.png"),view,1.57,-.02);
        power.setBreaker(true);
        field.frame(power,++tick);
        for(int i=0;i<loads.size();i++)
            check(Math.abs(light(field,loads.get(i))-on[i])<.01,"Reset failed "+loads.get(i).id());
        int[] working=capture(renderer,map,folder.resolve("battery-plant-powered-runtime.png"),view,1.57,-.02);
        renderer.lamp=true;
        capture(renderer,map,folder.resolve("battery-plant-installation-runtime.png"),view,1.57,-.02);
        int difference=0;
        for(int i=0;i<working.length;i++)if(working[i]!=failed[i])difference++;
        check(difference>1000,"The real lighting renderer did not change");
        return List.of("physical_starter_and_breaker_aim\tPASS "+starterPose+" | "+panelPose,
            "physical_switch_aim\tPASS rooms 6 and 9","independent_two_circuits_seven_loads\tPASS",
            "separate_source_and_breaker\tPASS","service_stair_both_directions_and_postures\tPASS",
            "plant_blackout_pixels_changed\t"+difference);
    }

    public static void main(String[] args)throws Exception{
        if(args.length<4||!args[3].equals("--gpu")){
            review(args);
            return;
        }
        var launcher=Class.forName("org.beckenburg.gpu.GpuDesktop");
        var start=launcher.getDeclaredMethod("startGpu");
        var stop=launcher.getDeclaredMethod("stopReviewGpu");
        start.setAccessible(true);
        stop.setAccessible(true);
        start.invoke(null);
        try{review(args);}
        finally{stop.invoke(null);}
    }

    private static void review(String[] args)throws Exception{
        var rows=Files.readAllLines(Path.of(args[0])).stream().map(line->line.split("\t",-1)).toList();
        var source=rows.get(0);
        var dock=rows.get(1);
        var f=rows.get(2);
        var portal=new Portal(Integer.parseInt(f[1]),0,1,d(f,3),d(f,4),d(f,5),d(f,6),true,"door",1);
        var door=new NativeInvestigationDoor.Spec(f[2],"gallery","director-stair",portal,d(f,7),d(f,8),d(f,9),
            2,-1,1,f[10]);
        var rooms=new Room[HouseMap.ROOM_COUNT];
        var spaces=new ArrayList<NativeInvestigationScene.Space>();
        var stairs=new HashSet<String>();
        var sockets=new ArrayList<Socket>();
        var routes=new LinkedHashMap<String,List<Vec>>();
        for(var row:rows.subList(3,rows.size()))switch(row[0]){
            case "SPACE" -> {
                var space=new NativeInvestigationScene.Space(row[1],Integer.parseInt(row[2]),d(row,3),d(row,4),d(row,5),
                    d(row,6),d(row,7),d(row,8),Boolean.parseBoolean(row[9]),true);
                spaces.add(space);
                if(row.length>10)rooms[space.owner()]=new Room(space.owner(),space.id(),space.x0(),space.z0(),space.x1(),
                    space.z1(),space.floor(),space.height(),space.outside()?"earth":"stone",
                    space.outside()?"exterior":"masonry",point(row,10));
            }
            case "RITUAL" -> sockets.add(new Socket("battery-ritual-"+row[1],"ritual",Integer.parseInt(row[2]),
                d(row,3),d(row,4),d(row,5),0,row[1]));
            case "TREAD" -> stairs.add(row[1]);
            case "SOCKET" -> sockets.add(new Socket(row[1],row[2],Integer.parseInt(row[3]),
                d(row,4),d(row,5),d(row,6),d(row,7),row[8]));
            case "POINT" -> routes.computeIfAbsent(row[1],key->new ArrayList<>()).add(point(row,2));
            default -> throw new IllegalArgumentException(row[0]);
        };
        NativeInvestigationScene scene;
        try(var glb=Files.newInputStream(Path.of(source[1]));var hcs=Files.newInputStream(Path.of(source[3]))){
            scene=NativeInvestigationScene.read(glb,source[2],hcs,source[4],stairs,spaces,List.of(door));
        }
        var staging=new Dock(d(dock,1),d(dock,2),d(dock,3),d(dock,4));
        rooms[14]=new Room(14,"Van",staging.x()-1.25,staging.z()-2,staging.x()+1.25,staging.z()+3,
            staging.y(),3,"metal","van",new Vec(staging.x(),staging.y(),staging.z()));
        var level=new LevelDefinition("wld-17",scene.revision(),"Seacliff Battery",List.of(rooms),List.of(portal),
            List.of(),List.of(),List.of(),sockets,List.of(),staging,"coast");
        var host=new HouseMap(HouseMap.Site.USER,level,null,scene);
        var guest=new HouseMap(HouseMap.Site.USER,level,null,scene);
        var van=host.van();
        var toe=van.world(0,VanStaging.FRONT-VanStaging.RAMP_LENGTH);
        var deck=van.world(0,VanStaging.FRONT+.45);
        var vanFeet=new Vec(deck.x(),van.floorY(),deck.z());
        for(var route:routes.values()){
            Vec out=route.get(0);
            check(Math.hypot(out.x()-toe.x(),out.z()-toe.z())<1e-6,"Arrival does not use the canonical toe");
            route.add(0,vanFeet);
        }
        for(double across:new double[]{-.38,0,.38}){
            var p=van.world(across,VanStaging.FRONT-VanStaging.RAMP_LENGTH-.25);
            var g=scene.physics().support(p.x(),p.z(),staging.y(),.045);
            check(g!=null&&scene.physics().traversable(new Vec(p.x(),g.y(),p.z()),HunterPosture.STANDING_HEIGHT),
                "Missing rooted forecourt beneath cargo ramp");
        }
        for(int seat=0;seat<6;seat++){
            var start=van.spawn(seat);
            var center=van.world(0,van.local(start.x(),start.z()).z());
            cross(host,List.of(new Vec(start.x(),van.floorY(),start.z()),
                new Vec(center.x(),van.floorY(),center.z()),vanFeet),HunterPosture.STANDING_HEIGHT,"Cargo seat "+seat);
        }
        cross(host,List.of(new Vec(34.2,7,20.3),new Vec(34.2,7,20.72)),
            HunterPosture.STANDING_HEIGHT,"Upper approach to the guard");
        check(!walk(host,List.of(new Vec(34.2,7,20.3),new Vec(34.2,7,21.33)),
            HunterPosture.STANDING_HEIGHT).isEmpty(),"Upper guard does not contain the stair well");
        check(!scene.physics().clearSight(new Vec(34.2,8.6,22),new Vec(34.2,10.5,22)),
            "Director roof admits skylight through the guarded stair opening");
        check(RitualSites.valid(host),"Existing ritual reservations lack supported room ownership");
        for(int i=0;i<14;i++){
            Vec spawn=rooms[i].spawn();
            check(host.roomAt(spawn.x(),spawn.y(),spawn.z())==i&&host.traversable(spawn.x(),spawn.y(),spawn.z()),
                "Unsupported native room anchor "+rooms[i].name()+" at "+spawn);
        }
        int closed=doorState(host,portal,false);
        guest.applyDoorSnapshot(host.doorSnapshot());
        check(doorState(guest,portal,false)==closed,"Guest closed door state");
        Path directory=Path.of(args[1]);
        Files.createDirectories(directory);
        var renderer=new SceneRenderer();
        renderer.setRenderSize(840,560);
        renderer.setPsxPreset(PsxPipeline.Preset.OFF);
        renderer.lamp=true;
        renderer.setLampHardware(EquipmentTiers.Tier.III,false);
        var galleryView=new Vec(30,4,28.65);
        int[] closedFrame=capture(renderer,host,directory.resolve("battery-gallery-closed-runtime.png"),galleryView,Math.PI,-.075);
        host.toggleDoor(portal.id());
        guest.applyDoorSnapshot(host.doorSnapshot());
        int open=doorState(host,portal,true);
        check(doorState(guest,portal,true)==open&&closed!=Integer.MAX_VALUE&&closed>open,"Shared acoustic graph");
        int[] openFrame=capture(renderer,host,directory.resolve("battery-gallery-open-runtime.png"),galleryView,Math.PI,-.075);
        int differences=0;
        for(int i=0;i<openFrame.length;i++)if(openFrame[i]!=closedFrame[i])differences++;
        check(differences>2000,"Shared renderer did not change door pose");
        for(var item:routes.entrySet()){
            var inbound=item.getValue();
            var outbound=new ArrayList<>(inbound);
            Collections.reverse(outbound);
            for(double height:new double[]{HunterPosture.STANDING_HEIGHT,HunterPosture.CROUCH_HEIGHT}){
                cross(host,inbound,height,item.getKey()+" inbound");
                cross(guest,outbound,height,item.getKey()+" return");
            }
            var destination=inbound.get(inbound.size()-1);
            check(host.roomAt(destination.x(),destination.y(),destination.z())==2,"Upper director room ownership");
        }
        var interior=new Vec(30.14,4,22.24);
        capture(renderer,host,directory.resolve("battery-director-stair-runtime.png"),interior,1.57,.22);
        van.setCargoDoorClosed(true);
        check(!walk(host,routes.values().iterator().next(),HunterPosture.STANDING_HEIGHT).isEmpty(),"Closed van hatch");
        van.setCargoDoorClosed(false);
        host.toggleDoor(portal.id());
        guest.applyDoorSnapshot(host.doorSnapshot());
        check(doorState(guest,portal,false)==closed,"Reclosed fitted state");
        var report=new ArrayList<>(List.of(
            "measure\tresult",
            "scene_revision\t"+scene.revision(),
            "native_room_volumes\t"+spaces.size(),
            "van_to_director_west_and_east_standing_crouched\tPASS",
            "director_to_van_west_and_east_standing_crouched\tPASS",
            "canonical_ramp_receipt\tPASS",
            "six_cargo_positions_reach_hatch\tPASS",
            "upper_guard_and_opaque_roof\tPASS",
            "physical_room_anchors\t14",
            "preserved_ritual_socket_ownership\tPASS",
            "closed_cargo_hatch\tBLOCKED",
            "gallery_frame_closed_and_open_body_object_optics\tPASS",
            "local_van_and_director_room_ownership\tPASS",
            "door_state_host_guest_snapshots\tPASS",
            "acoustic_open_cost\t"+open,
            "acoustic_closed_cost\t"+closed,
            "changed_renderer_pixels\t"+differences));
        if(host.level().socket("generator",9)!=null)report.addAll(power(host,renderer,directory));
        Files.write(Path.of(args[2]),report);
    }
}
