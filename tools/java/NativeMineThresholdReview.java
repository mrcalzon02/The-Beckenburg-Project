package org.beckenburg;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import org.beckenburg.LevelDefinition.Dock;
import org.beckenburg.LevelDefinition.Portal;
import org.beckenburg.LevelDefinition.Room;
import org.beckenburg.LevelDefinition.Socket;
import org.beckenburg.LevelDefinition.Vec;
import org.beckenburg.NativeSchoolInteriorReview.Peer;

/** Physically replays the Mine's surfaces, doors and level changes in the shared game world. */
public final class NativeMineThresholdReview {
    private NativeMineThresholdReview(){}

    public static void main(String[] args)throws Exception{
        if(!List.of(args).contains("--gpu")){review(args);return;}
        var desktop=Class.forName("org.beckenburg.gpu.GpuDesktop");
        var start=desktop.getDeclaredMethod("startGpu");
        var stop=desktop.getDeclaredMethod("stopReviewGpu");
        start.setAccessible(true);
        stop.setAccessible(true);
        start.invoke(null);
        try{review(args);}finally{stop.invoke(null);}
    }

    private static void review(String[] args)throws Exception{
        var packet=Path.of(args[0]);
        var arrival=rows(packet.resolve("arrival.tsv"));
        var source=arrival.get(0);
        var staging=arrival.get(1);
        var dock=new Dock(d(staging,1),d(staging,2),d(staging,3),d(staging,4));
        var spaces=new ArrayList<NativeInvestigationScene.Space>();
        var treads=new HashSet<String>();
        var sockets=new ArrayList<Socket>();
        var routes=new LinkedHashMap<String,List<Vec>>();
        for(var row:arrival)switch(row[0]){
            case "SPACE" -> spaces.add(new NativeInvestigationScene.Space(row[1],Integer.parseInt(row[2]),
                d(row,3),d(row,4),d(row,5),d(row,6),d(row,7),d(row,8),
                Boolean.parseBoolean(row[9]),Boolean.parseBoolean(row[10])));
            case "RITUAL" -> sockets.add(new Socket("mine-"+row[1],"ritual",Integer.parseInt(row[2]),
                d(row,3),d(row,4),d(row,5),0,row[1]));
            case "TREAD" -> treads.add(row[1]);
            case "POINT" -> routes.computeIfAbsent(row[1],id->new ArrayList<>()).add(point(row,2));
            case "SOURCE","DOCK" -> {}
            default -> throw new IOException("Unknown Mine field "+row[0]);
        }
        var doors=new ArrayList<NativeInvestigationDoor.Spec>();
        for(var row:rows(packet.resolve("door-survey.tsv"))){
            var portal=new Portal(Integer.parseInt(row[0]),Integer.parseInt(row[4]),Integer.parseInt(row[5]),
                d(row,6),d(row,7),d(row,8),d(row,9),row[10].equals("z"),"door",1);
            doors.add(new NativeInvestigationDoor.Spec(row[1],row[2],row[3],portal,d(row,11),d(row,12),d(row,13),
                Integer.parseInt(row[14]),Integer.parseInt(row[15]),Integer.parseInt(row[16]),row[17]));
        }
        NativeInvestigationScene scene;
        try(var model=Files.newInputStream(Path.of(source[1]));var physics=Files.newInputStream(Path.of(source[3]))){
            scene=NativeInvestigationScene.read(model,source[2],physics,source[4],treads,spaces,doors);
        }
        var portals=new ArrayList<>(doors.stream().map(NativeInvestigationDoor.Spec::portal).toList());
        var rooms=bind(scene,dock,rows(packet.resolve("mission.tsv")),portals);
        var level=new LevelDefinition("wld-05",scene.revision(),"Dead Man's Claim",rooms,portals,
            List.of(),List.of(),List.of(),sockets,List.of(),dock,"industrial");
        var map=new HouseMap(HouseMap.Site.USER,level,null,scene);
        var guest=new HouseMap(HouseMap.Site.USER,level,null,scene);
        require(map.valid(),"Disconnected Mine case owners");
        require(RitualSites.valid(map),"Unsupported Mine ritual position");
        boolean gpu=List.of(args).contains("--gpu");
        var captures=packet.resolve(gpu?"captures/opengl":"captures");
        Files.createDirectories(captures);
        var errors=new ArrayList<String>();
        for(var door:doors){
            try{
                require(NativeSchoolInteriorReview.sweep(scene.physics(),door),"Blocked leaf swing "+door.frameId());
                NativeSchoolInteriorReview.audit(map,door,false);
                if(door.portal().id()>=705)
                    NativeSchoolInteriorReview.render(map,door.portal(),captures.resolve("mine-"+door.portal().id()+"-closed.png"),1);
                map.toggleDoor(door.portal().id(),System.nanoTime()-2_000_000_000L);
                guest.applyDoorSnapshot(map.doorSnapshot());
                NativeSchoolInteriorReview.audit(map,door,true);
                NativeSchoolInteriorReview.audit(guest,door,true);
                if(door.portal().id()>=705)
                    NativeSchoolInteriorReview.render(map,door.portal(),captures.resolve("mine-"+door.portal().id()+"-open.png"),1);
            }catch(AssertionError problem){
                errors.add(problem.getMessage());
                if(!map.doorOpen(door.portal().id()))map.toggleDoor(door.portal().id(),System.nanoTime()-2_000_000_000L);
            }
        }

        var van=map.van();
        var cargo=van.world(0,0);
        var lip=van.world(0,VanStaging.FRONT);
        var deck=new Vec(cargo.x(),van.floorY(),cargo.z());
        var ramp=new Vec(lip.x(),van.floorY(),lip.z());
        var arrivalGuide=new ArrayList<>(List.of(deck,ramp));
        arrivalGuide.addAll(routes.get("L01"));
        routes.put("L01",arrivalGuide);
        for(var route:routes.entrySet()){
            try{walkBoth(map,route.getValue(),route.getKey());}
            catch(AssertionError problem){errors.add(problem.getMessage());}
        }
        require(errors.isEmpty(),String.join("\n",errors));
        var loop=new ArrayList<Vec>();
        join(loop,routes.get("L01"),false);
        join(loop,routes.get("L06"),true);
        for(String id:List.of("L09","L10","L18","L17","L06"))join(loop,routes.get(id),false);
        join(loop,routes.get("L01"),true);
        walkBoth(map,loop,"cargo to haul and ventilation return");
        var evidence=new ArrayList<>(List.of("measure\tresult","native_scene_revision\t"+scene.revision(),
            "staged_case_revision\t"+level.revision(),"supported_case_owners\t14 supported owners and actual cargo van",
            "fitted_thresholds\t8 fitted sets, four standing/crouched body tests per state, sight, aim, sound and object behavior",
            "physical_routes\t"+routes.size()+" routes with exact native standing/crouched replay in both directions"));
        evidence.add("joined_mine_loop\tFour complete walks from cargo through haul and vent stairs, then back to the cargo deck");
        for(var site:RitualSites.all(map))
            evidence.add("mine_"+site.rite().name().toLowerCase(java.util.Locale.ROOT)+"\t"+site.room()+" "+site.x()+" "+site.y()+" "+site.z());
        var former=Path.of("development/levels/rebuild/mine-yard-pass/wld-05/mine-yard.hcs2");
        AuthoredSitePhysics previous;
        try(var raw=Files.newInputStream(former)){
            previous=AuthoredSitePhysics.read(raw,"892154e00cb1f7186e283a13a82e47d00febbddc0101f3f9060be4654919d94c",List.of());
        }
        for(int id:new int[]{706,707}){
            var p=doors.stream().filter(candidate->candidate.portal().id()==id).findFirst().orElseThrow().portal();
            int samples=0;
            for(double lane:new double[]{-.54,0,.54})for(double depth:new double[]{.28,.46,.68,.86,.96}){
                double x=p.x()+(p.alongX()?lane:-depth),z=p.z()+(p.alongX()?-depth:lane);
                var support=scene.physics().support(x,z,0,.025);
                require(support!=null&&Math.abs(support.y())<.002&&map.traversable(x,0,z),
                    "Mine stair-side landing is missing at "+x+","+z);
                samples++;
            }
            double oldX=p.x()-(p.alongX()?0:.96),oldZ=p.z()-(p.alongX()?.96:0);
            var old=previous.support(oldX,oldZ,0,.025);
            require(old==null||Math.abs(old.y())>.025,"Former Mine export already had the new level platform "+id);
            evidence.add("shaft_landing_"+id+"\t"+samples+" level, standing capsule supports; former face has no level support at .96 m past the sill");

        }
        capture(map,new Vec(30.4,0,75),-.30,captures.resolve("mine-roadhead-and-van.png"));
        capture(map,map.spawn(4),0,captures.resolve("mine-lower-haul-stair.png"));
        if(List.of(args).contains("--lan"))live(level,scene,evidence);
        NativeSchoolInteriorReview.exportTriangles(scene,Path.of(args[1]));
        var output=packet.resolve("verification");
        Files.createDirectories(output);
        Files.write(output.resolve(gpu?"opengl.tsv":"native-review.tsv"),evidence);
        evidence.forEach(System.out::println);
    }

    private static void capture(HouseMap map,Vec at,double yaw,Path target)throws Exception{
        require(map.traversable(at.x(),at.y(),at.z()),"Unsupported standing Mine capture "+at);
        var renderer=new SceneRenderer();
        renderer.setRenderSize(880,580);
        renderer.setPsxPreset(PsxPipeline.Preset.OFF);
        renderer.x=at.x();renderer.z=at.z();renderer.eyeHeight=at.y()+HunterPosture.STANDING_EYE;
        renderer.yaw=yaw;renderer.pitch=-.06;renderer.lamp=true;
        renderer.setLampHardware(EquipmentTiers.Tier.III,false);
        NativeSchoolInteriorReview.drawFrame(renderer,map,target);
    }

    private static void join(List<Vec> target,List<Vec> segment,boolean reverse){
        var use=new ArrayList<>(segment);
        if(reverse)Collections.reverse(use);
        require(target.isEmpty()||target.get(target.size()-1).equals(use.get(0)),"Joined Mine route lost its actual threshold");
        target.addAll(target.isEmpty()?use:use.subList(1,use.size()));
    }

    private static void live(LevelDefinition level,NativeInvestigationScene scene,List<String> report)throws Exception{
        var map=new HouseMap(HouseMap.Site.USER,level,null,scene);
        var clientMap=new HouseMap(HouseMap.Site.USER,level,null,scene);
        try(var host=new LanHost("127.0.0.1",0,GhostIdentity.Type.SPIRIT,CustomContract.standard(3),map);
            var scout=new Peer(host,false,"Roadhead");
            var haul=new Peer(host,false,"Haul");
            var vent=new Peer(host,false,"Ventilation");
            var ghost=new Peer(host,true,"Ghost")){
            for(var peer:List.of(scout,haul,vent,ghost)){
                peer.awaitExact("SITE|USER|"+level.revision());
                peer.client.ready();
            }
            scout.awaitExact("LOBBY|3|HUMAN|4|WAITING");
            scout.client.start();
            scout.await("INFO|mission started");
            scout.client.view3D();
            scout.awaitExact("ACCEPT|VIEW3D");
            var at=NativeSchoolInteriorReview.body(scout.await("BODY|0|"));
            require(map.roomAt(at.x(),at.y(),at.z())==14,"Scout did not start on the loaded cargo deck");
            for(int n=0;n<13;n++)at=NativeSchoolInteriorReview.step(scout,0,0,-100);
            require(map.roomAt(at.x(),at.y(),at.z())==13&&Math.abs(at.y())<.025,"Scout missed the roadhead");
            for(int room:NativeSchoolInteriorReview.missionRoute(map,3)){
                haul.client.move(room);
                haul.awaitExact("ACCEPT|MOVE|"+room);
            }
            haul.client.view3D();
            haul.awaitExact("ACCEPT|VIEW3D");
            at=NativeSchoolInteriorReview.body(haul.await("BODY|1|"));
            require(at.equals(map.spawn(3)),"Haul peer missed the shared headhouse floor");
            for(int n=0;n<5;n++)at=NativeSchoolInteriorReview.step(haul,1,-100,0);
            for(int n=0;n<8;n++)at=NativeSchoolInteriorReview.step(haul,1,0,-100);
            refused(haul,0,-100);
            open(clientMap,haul,1,at,Math.PI,706);
            scout.awaitExact("DOORS|706");
            for(int n=0;n<5;n++)at=NativeSchoolInteriorReview.step(haul,1,0,-100);
            require(map.roomAt(at.x(),at.y(),at.z())==4&&Math.abs(at.y())<.02,"Haul peer crossed past the level receiving landing "+at);
            for(int n=0;n<11;n++)at=NativeSchoolInteriorReview.step(haul,1,0,-100);
            require(at.y()<-1.2,"Haul peer could not descend the actual new treads "+at);
            for(int n=0;n<16;n++)at=NativeSchoolInteriorReview.step(haul,1,0,100);
            require(map.roomAt(at.x(),at.y(),at.z())==3,"Haul peer missed the return to the headhouse");

            clientMap.applyDoorSnapshot("706");
            for(int room:NativeSchoolInteriorReview.missionRoute(map,3)){
                vent.client.move(room);
                vent.awaitExact("ACCEPT|MOVE|"+room);
            }
            vent.client.view3D();
            vent.awaitExact("ACCEPT|VIEW3D");
            at=NativeSchoolInteriorReview.body(vent.await("BODY|2|"));
            require(at.equals(map.spawn(3)),"Ventilation peer missed the shared landing");
            for(int n=0;n<10;n++)at=NativeSchoolInteriorReview.step(vent,2,-100,0);
            refused(vent,-100,0);
            open(clientMap,vent,2,at,-Math.PI/2,707);
            scout.awaitExact("DOORS|706,707");
            for(int n=0;n<4;n++)at=NativeSchoolInteriorReview.step(vent,2,-100,0);
            require(map.roomAt(at.x(),at.y(),at.z())==10&&Math.abs(at.y())<.02,"Vent peer missed the level receiving landing "+at);
            for(int n=0;n<10;n++)at=NativeSchoolInteriorReview.step(vent,2,-100,0);
            require(at.y()<-1.1,"Vent peer could not descend the actual new treads "+at);
            for(int n=0;n<14;n++)at=NativeSchoolInteriorReview.step(vent,2,100,0);
            require(map.roomAt(at.x(),at.y(),at.z())==3,"Vent peer missed the return to the headhouse");
        }
        report.add("staged_tcp_mine\tFour peers, real cargo ramp, both closed stair doors reject crossing, both aimed opens replicate, level receiving landings followed by descent and return");
    }

    private static void refused(Peer peer,int dx,int dz)throws Exception{
        Thread.sleep(125);
        peer.client.step(dx,dz);
        peer.awaitExact("REJECT|blocked by world collision");
    }

    private static void open(HouseMap clientMap,Peer peer,int seat,Vec at,double yaw,int id)throws Exception{
        double pitch=-.11;
        var eye=new LooseClutter.Observer(seat,at.x(),at.y(),at.z(),yaw,pitch,true);
        var focus=InteractionFocus.pick(clientMap,eye,List.of());
        require(focus!=null&&focus.kind()==InteractionFocus.Kind.DOOR&&focus.id().equals(Integer.toString(id)),
            "No live aim on fitted Mine door "+id+": "+at+" / "+focus);
        peer.client.turn(Math.floorMod((int)Math.round(Math.toDegrees(yaw)),360));
        peer.client.interact(focus,yaw,pitch);
        peer.awaitExact("ACCEPT|INTERACT|Open door");
    }

    private static List<Room> bind(NativeInvestigationScene scene,Dock dock,List<String[]> lines,List<Portal> portals){
        var rooms=new Room[HouseMap.ROOM_COUNT];
        for(var row:lines)switch(row[0]){
            case "ANCHOR" -> {
                int id=Integer.parseInt(row[1]);
                var at=point(row,3);
                var zone=scene.spaceAt(at.x(),at.y(),at.z());
                require(zone!=null&&zone.owner()==id&&zone.id().equals(row[2])
                    &&scene.physics().traversable(at,HunterPosture.STANDING_HEIGHT),"Unsupported Mine owner "+row[2]+" "+at+" / "+zone);
                rooms[id]=new Room(id,zone.id(),zone.x0(),zone.z0(),zone.x1(),zone.z1(),zone.floor(),zone.height(),
                    zone.outside()?"native-outdoor":"concrete",zone.outside()?"outdoor":"mine",at);
            }
            case "TRANSITION" -> {
                var at=point(row,4);
                var support=scene.physics().support(at.x(),at.z(),at.y(),.08);
                require(support!=null&&Math.abs(support.y()-at.y())<.07,"Unsupported Mine transition "+row[1]);
                int a=Integer.parseInt(row[2]),b=Integer.parseInt(row[3]);
                boolean along=row[8].equals("z");
                if(!row[9].equals("stair")){
                    double dx=along?0:.65,dz=along?.65:0;
                    int near=scene.ownerAt(at.x()-dx,at.y(),at.z()-dz),far=scene.ownerAt(at.x()+dx,at.y(),at.z()+dz);
                    require(a==near&&b==far||a==far&&b==near,"Mine transition "+row[1]+" touches "+near+" and "+far);
                    require(scene.physics().clearSight(new Vec(at.x()-dx,at.y()+1.2,at.z()-dz),
                        new Vec(at.x()+dx,at.y()+1.2,at.z()+dz)),"Blocked Mine opening "+row[1]);
                }
                portals.add(new Portal(Integer.parseInt(row[1]),a,b,at.x(),at.y(),at.z(),d(row,7),along,row[9],1.0));
            }
            default -> throw new IllegalArgumentException("Unknown Mine mission record "+row[0]);
        }
        var van=new VanStaging(new VanStaging.Placement(dock.x(),dock.y(),dock.z(),dock.yaw()));
        var lip=van.world(0,VanStaging.FRONT);
        double y=van.floorY();
        portals.add(new Portal(719,13,14,lip.x(),y,lip.z(),VanStaging.RAMP_HALF_WIDTH*2,true,"trail",1));
        rooms[14]=new Room(14,"Expedition van",dock.x()-1.26,dock.z()-3,dock.x()+1.26,dock.z()+2.4,
            y,VanStaging.INTERIOR_HEIGHT,"van","metal",new Vec(dock.x(),y,dock.z()));
        return List.of(rooms);
    }

    private static void walkBoth(HouseMap map,List<Vec> points,String id){
        var reverse=new ArrayList<>(points);
        Collections.reverse(reverse);
        for(double height:new double[]{HunterPosture.STANDING_HEIGHT,HunterPosture.CROUCH_HEIGHT}){
            NativeSchoolInteriorReview.walk(map,points,height,"Mine "+id);
            NativeSchoolInteriorReview.walk(map,reverse,height,"Mine reverse "+id);
        }
    }

    private static List<String[]> rows(Path file)throws IOException{
        return Files.readAllLines(file).stream().map(line->line.split("\\t",-1)).toList();
    }

    private static double d(String[] row,int column){return Double.parseDouble(row[column]);}
    private static Vec point(String[] row,int column){return new Vec(d(row,column),d(row,column+1),d(row,column+2));}
    private static void require(boolean ok,String reason){if(!ok)throw new AssertionError(reason);}
}
