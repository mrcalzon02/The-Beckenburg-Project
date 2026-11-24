package org.beckenburg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import org.beckenburg.LevelDefinition.Dock;
import org.beckenburg.LevelDefinition.Portal;
import org.beckenburg.LevelDefinition.Room;
import org.beckenburg.LevelDefinition.Socket;
import org.beckenburg.LevelDefinition.Vec;
import org.beckenburg.editor.DefaultPlayerMotion;

/** Verifies the actual Northwatch exports, old cutouts, physical perimeter, and retained circulation. */
public final class NorthwatchBoundaryReview {
    private static final double[] POSTURES={HunterPosture.STANDING_HEIGHT,HunterPosture.CROUCH_HEIGHT};

    private NorthwatchBoundaryReview() {}

    public static void main(String[] args)throws Exception {
        boolean gpu=List.of(args).contains("--gpu");
        if(!gpu){review(args,false);return;}
        var desktop=Class.forName("org.beckenburg.gpu.GpuDesktop");
        var start=desktop.getDeclaredMethod("startGpu");
        var stop=desktop.getDeclaredMethod("stopReviewGpu");
        start.setAccessible(true);
        stop.setAccessible(true);
        start.invoke(null);
        try { review(args,true); }
        finally { stop.invoke(null); }
    }

    private static void review(String[] args,boolean gpu)throws Exception {
        Path path=Path.of(args[0]);
        boolean fitted=List.of(args).contains("--doors");
        Path boundary=fitted?Path.of(args[5]):path;
        var spaces=new ArrayList<NativeInvestigationScene.Space>();
        var rooms=new Room[HouseMap.ROOM_COUNT];
        var sockets=new ArrayList<Socket>();
        var treads=new LinkedHashSet<String>();
        var routes=new LinkedHashMap<String,List<Vec>>();
        var arrivals=new ArrayList<Vec>();
        var controls=new ArrayList<Vec>();
        var footings=new ArrayList<String[]>();
        var portals=new ArrayList<Portal>();
        var frames=new ArrayList<NativeInvestigationDoor.Spec>();
        for(String line:Files.readAllLines(path.resolve("runtime-input.tsv"))) {
            var row=line.split("\\t");
            switch(row[0]) {
                case "SPACE" -> {
                    boolean outside=Boolean.parseBoolean(row[9]);
                    int owner=Integer.parseInt(row[2]);
                    var zone=new NativeInvestigationScene.Space(row[1],owner,d(row,3),d(row,4),d(row,5),d(row,6),
                        d(row,7),d(row,8),outside,true);
                    spaces.add(zone);
                    if(rooms[owner]==null) {
                        Vec feet=point(row,11);
                        rooms[owner]=new Room(owner,row[10],zone.x0(),zone.z0(),zone.x1(),zone.z1(),
                            zone.floor(),zone.height(),outside?"native-outdoor":"metal",outside?"outdoor":"metal",feet);
                    }
                }
                case "OPEN" -> portals.add(new Portal(Integer.parseInt(row[1]),Integer.parseInt(row[2]),
                    Integer.parseInt(row[3]),d(row,4),d(row,5),d(row,6),d(row,8),row[7].equals("z"),row[9],1));
                case "TREAD" -> treads.add(row[1]);
                case "RITUAL" -> sockets.add(new Socket("northwatch-"+row[1],"ritual",Integer.parseInt(row[2]),
                    d(row,3),d(row,4),d(row,5),0,row[1]));
                case "POINT" -> routes.computeIfAbsent(row[1],ignored->new ArrayList<>()).add(point(row,2));
                case "ARRIVAL" -> arrivals.add(point(row,1));
                case "RIM_CONTROL" -> controls.add(point(row,1));
                default -> throw new AssertionError("Unexpected site review row "+row[0]);
            }
        }
        for(String line:Files.readAllLines(boundary.resolve("boundary-stations.tsv")))footings.add(line.split("\\t"));
        if(fitted) {
            for(String line:Files.readAllLines(path.resolve("door-schedule.tsv"))) {
                var r=line.split("\\t");
                var p=new Portal(n(r,0),n(r,4),n(r,5),d(r,6),d(r,7),d(r,8),d(r,9),r[10].equals("z"),"door",1);
                portals.add(p);
                frames.add(new NativeInvestigationDoor.Spec(r[1],r[2],r[3],p,d(r,11),d(r,12),d(r,13),
                    n(r,14),n(r,15),n(r,16),r[17]));
            }
        }
        NativeInvestigationScene scene;
        AuthoredSitePhysics prior;
        try(var model=Files.newInputStream(boundary.resolve("bounded-station.glb"));
            var collision=Files.newInputStream(boundary.resolve("bounded-station.hcs2"));
            var original=Files.newInputStream(Path.of(args[3]))) {
            scene=NativeInvestigationScene.read(model,args[1],collision,args[2],treads,spaces,frames);
            prior=AuthoredSitePhysics.read(original,args[4],List.of(),treads);
        }

        var dock=new Dock(32,0,76,0);
        var van=new VanStaging(new VanStaging.Placement(dock.x(),dock.y(),dock.z(),dock.yaw()));
        rooms[14]=new Room(14,"Expedition van",30.74,73,33.26,78.4,van.floorY(),VanStaging.INTERIOR_HEIGHT,
            "van","metal",new Vec(32,van.floorY(),76));
        if(fitted) {
            var toe=van.world(0,VanStaging.FRONT);
            portals.add(new Portal(905,13,14,toe.x(),van.floorY(),toe.z(),VanStaging.RAMP_HALF_WIDTH*2,
                true,"trail",1));
        }
        var level=new LevelDefinition("wld-08",scene.revision(),"Northwatch Research Station",
            List.of(rooms),portals,List.of(),List.of(),List.of(),sockets,List.of(),dock,"polar");
        var map=new HouseMap(HouseMap.Site.USER,level,null,scene);
        require(RitualSites.valid(map),"New perimeter affected a ritual approach or supported floor");

        var captures=path.resolve(gpu?"captures/opengl":"captures");
        Files.createDirectories(captures);
        if(fitted) {
            require(map.valid(),"The 15 native owners are disconnected or an anchor is obstructed");
            var replica=new HouseMap(HouseMap.Site.USER,level,null,scene);
            NorthwatchDoorRuntime.review(map,replica,frames,captures);
        }
        var physics=scene.physics();
        int pastEdge=0;
        for(int i=0;i<controls.size();i+=2) {
            var from=controls.get(i);
            var beyond=controls.get(i+1);
            var origin=prior.support(from.x(),from.z(),from.y(),1);
            require(origin!=null&&prior.support(beyond.x(),beyond.z(),beyond.y(),1)==null,
                    "Incorrect reference edge control "+from+" / "+beyond);
            require(prior.clearSight(new Vec(from.x(),origin.y()+1.58,from.z()),
                                     new Vec(beyond.x(),origin.y()+1.58,beyond.z())),
                    "The original terrain edge was already visible");
            require(physics.support(beyond.x(),beyond.z(),beyond.y(),2)!=null,
                    "New exterior did not continue beyond the original rim "+beyond);
            pastEdge++;
        }

        double apex=DefaultPlayerMotion.TAKEOFF_SPEED*DefaultPlayerMotion.TAKEOFF_SPEED/(2*DefaultPlayerMotion.GRAVITY);
        int bodies=0,jumps=0,optic=0,supported=0;
        for(var row:footings) {
            double x=d(row,1),z=d(row,2),nx=d(row,3),nz=d(row,4),y=d(row,5);
            var finish=new Vec(x,y,z);
            for(double height:POSTURES) {
                require(physics.bodyBlocked(finish,height,.22),"Open perimeter at "+finish);
                bodies++;
                require(physics.bodyBlocked(new Vec(x,y+apex,z),height,.22),"Perimeter jump gap at "+finish);
                jumps++;
            }
            var inner=physics.support(x-nx*.95,z-nz*.95,y,1.15);
            var outer=physics.support(x+nx*.95,z+nz*.95,y,1.15);
            if(inner!=null&&outer!=null) {
                var initial=new Vec(x-nx*.95,inner.y(),z-nz*.95);
                for(double height:POSTURES) {
                    if(!physics.traversable(initial,height))continue;
                    require(physics.resolveMove(initial,x+nx*.95,z+nz*.95,height)==null,
                        "Supported crossing penetrated physical barrier "+finish);
                    supported++;
                }
            }
            require(!physics.clearSight(new Vec(x-nx*.70,y+HunterPosture.STANDING_EYE,z-nz*.70),
                                         new Vec(x+nx*.70,y+HunterPosture.STANDING_EYE,z+nz*.70)),
                    "Open standing-height seam at "+finish);
            optic++;
        }
        require(supported>700,"Insufficient two-sided supported negative crossing controls");
        require(pastEdge==3,"Missing the original three edge controls");

        var surveyed=new ArrayList<String>(List.of("route\tdeclared_y\tsupported_y\tx\tz"));
        for(var entry:routes.entrySet()) {
            if(entry.getKey().equals("L01")||entry.getKey().equals("J01"))continue;
            entry.setValue(surveyDatums(prior,physics,entry.getValue(),entry.getKey(),surveyed));
        }
        var cargoJourney=new ArrayList<>(routes.get("J01"));
        cargoJourney.remove(0);
        routes.put("J01",surveyDatums(prior,physics,cargoJourney,"J01",surveyed));
        Files.writeString(path.resolve("retained-route-datums.tsv"),String.join("\n",surveyed)+"\n");
        var lip=van.world(0,VanStaging.FRONT);
        var vanRoute=new ArrayList<Vec>();
        vanRoute.add(rooms[14].spawn());
        vanRoute.add(new Vec(lip.x(),van.floorY(),lip.z()));
        vanRoute.addAll(arrivals);
        var wholeJourney=new ArrayList<>(vanRoute.subList(0,2));
        wholeJourney.addAll(routes.get("J01"));
        routes.put("J01",wholeJourney);
        for(double height:POSTURES) {
            replay(map,vanRoute,height,"Van to existing forecourt");
            for(var entry:routes.entrySet()) {
                // L01 was checked from the actual raised van deck in vanRoute.
                if(entry.getKey().equals("L01"))continue;
                replay(map,entry.getValue(),height,entry.getKey());
            }
        }
        require(map.roomAt(32,van.floorY(),76)==14,"Van launch position changed");

        if(!fitted) {
            render(map,physics,new Vec(37,0,79.5),new Vec(32,1.3,85.2),"road-gate-from-apron",captures);
            render(map,physics,new Vec(32,-.5,96),new Vec(32,1.2,84),"continued-lane-and-station",captures);
            render(map,physics,new Vec(57,0,5.0),new Vec(60,1.1,-.4),"array-edge-screen",captures);
        }
        var rows=new ArrayList<>(List.of("check\tresult","scene_revision\t"+scene.revision(),
            "renderer\t"+(gpu?GpuWorldRenderer.active().rendererDescription():"Software"),
            "body_contacts_at_perimeter\t"+bodies,"jump_height_contacts\t"+jumps,
            "standing_optical_contacts\t"+optic,"supported_crossings_blocked\t"+supported,
            "original_invisible_edges_with_exterior_ground\t"+pastEdge,
            "canonical_ramp_retreat_both_postures\tPASS",
            "measured_tread_datums\t"+(surveyed.size()-1),
            "walked_authored_links_and_journeys_both_postures\t"+routes.size()));
        if(fitted) {
            rows.add("working_doors\t"+frames.size());
            rows.add("connected_supported_owners\t"+level.rooms().size());
            rows.add("optics_object_passage_room_acoustics_and_replica_state\tPASS");
            NorthwatchDoorRuntime.transits(map,portals);
            if(!gpu) {
                NorthwatchDoorRuntime.live(level,scene);
                rows.add("four_peer_van_to_generator_two_aimed_doors\tPASS");
            }
        }
        String stem=fitted?"doors":"boundary";
        Files.writeString(path.resolve(stem+(gpu?"-opengl.tsv":"-software.tsv")),String.join("\n",rows)+"\n");
        rows.forEach(System.out::println);
    }

    private static List<Vec> surveyDatums(AuthoredSitePhysics prior,AuthoredSitePhysics current,List<Vec> points,
                                           String label,List<String> report) {
        var measured=new ArrayList<Vec>();
        for(var p:points){
            var old=prior.support(p.x(),p.z(),p.y(),.24);
            var fitted=current.support(p.x(),p.z(),p.y(),.24);
            require(old!=null&&fitted!=null&&Math.abs(old.y()-fitted.y())<.0001,
                    "Lost exact original support for "+label+" at "+p);
            if(Math.abs(old.y()-p.y())>.05){
                report.add(label+"\t"+p.y()+"\t"+old.y()+"\t"+p.x()+"\t"+p.z());
            }
            measured.add(new Vec(p.x(),old.y(),p.z()));
        }
        return measured;
    }

    private static void replay(HouseMap map,List<Vec> waypoints,double height,String label) {
        NativeSchoolInteriorReview.walk(map,waypoints,height,label);
        var reverse=new ArrayList<>(waypoints);
        Collections.reverse(reverse);
        NativeSchoolInteriorReview.walk(map,reverse,height,label+" return");
    }

    private static void render(HouseMap map,AuthoredSitePhysics physics,Vec at,Vec target,
                               String name,Path captures)throws Exception {
        var floor=physics.support(at.x(),at.z(),at.y(),1.5);
        require(floor!=null&&physics.traversable(new Vec(at.x(),floor.y(),at.z()),HunterPosture.STANDING_HEIGHT),
                "Unsupported standing camera "+name);
        var view=new SceneRenderer();
        view.setRenderSize(960,640);
        view.setPsxPreset(PsxPipeline.Preset.OFF);
        view.x=at.x();
        view.z=at.z();
        view.eyeHeight=floor.y()+HunterPosture.STANDING_EYE;
        view.yaw=Math.atan2(target.x()-at.x(),target.z()-at.z());
        view.pitch=Math.atan2(target.y()-view.eyeHeight,Math.hypot(target.x()-at.x(),target.z()-at.z()));
        view.lamp=true;
        view.setLampHardware(EquipmentTiers.Tier.III,false);
        NativeSchoolInteriorReview.drawFrame(view,map,captures.resolve(name+".png"));
    }

    private static int n(String[] row,int i){return Integer.parseInt(row[i]);}
    private static double d(String[] row,int i){return Double.parseDouble(row[i]);}
    private static Vec point(String[] row,int i){return new Vec(d(row,i),d(row,i+1),d(row,i+2));}
    private static void require(boolean pass,String message){if(!pass)throw new AssertionError(message);}
}
