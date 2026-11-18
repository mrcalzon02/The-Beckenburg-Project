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
        check(renderer.frameDiagnostics().faces()>500&&renderer.frameDiagnostics().frustumFaces()>35,"Native geometry missing in frame");
        var frame=renderer.image();
        check(ImageIO.write(frame,"png",file.toFile()),"PNG encoder unavailable");
        return frame.getRGB(0,0,840,560,null,0,840);
    }

    public static void main(String[] args)throws Exception{
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
        Files.write(Path.of(args[2]),List.of(
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
    }
}
