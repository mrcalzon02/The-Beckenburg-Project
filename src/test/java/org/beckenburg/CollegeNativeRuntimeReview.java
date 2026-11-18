package org.beckenburg;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import javax.imageio.ImageIO;
import org.beckenburg.LevelDefinition.*;

public final class CollegeNativeRuntimeReview {
    record Route(String id, List<Vec> points){}
    private record Aperture(String id, String a, String b, Portal portal){}
    record Campus(HouseMap map, List<Aperture> apertures, List<Route> routes, List<Vec> arrival){}
    private static final Path PACKET=Path.of("development/levels/rebuild/college-ground-pass/college");

    private static double n(String[] row,int at){return Double.parseDouble(row[at]);}
    private static Vec point(String[] row,int at){return new Vec(n(row,at),n(row,at+1),n(row,at+2));}
    private static void check(boolean valid,String reason){if(!valid)throw new AssertionError(reason);}

    static Campus load(Path manifest)throws IOException{
        return load(manifest, PACKET.resolve("campus-grounds.glb"), PACKET.resolve("campus-grounds.hcs2"));
    }

    static Campus load(Path manifest, Path glb, Path hcs2)throws IOException{
        var rows=Files.readAllLines(manifest).stream().map(line->line.split("\t",-1)).toList();
        var spaces=new ArrayList<NativeInvestigationScene.Space>();
        var stairs=new HashSet<String>();
        var byId=new HashMap<String,NativeInvestigationScene.Space>();
        for(var row:rows){
            if(row[0].equals("SPACE")){
                var space=new NativeInvestigationScene.Space(row[1],Integer.parseInt(row[2]),n(row,3),n(row,4),n(row,5),n(row,6),
                    n(row,7),n(row,8),row[9].equals("1"),row[10].equals("1"));
                spaces.add(space);
                byId.put(space.id(),space);
            }else if(row[0].equals("TREAD"))stairs.add(row[1]);
        }
        var source=rows.get(0);
        check(source[0].equals("SOURCE"),"Missing source packet");
        var dockRow=rows.get(1);
        check(dockRow[0].equals("DOCK"),"Missing canonical dock");
        var dock=new Dock(n(dockRow,1),n(dockRow,2),n(dockRow,3),n(dockRow,4));
        var van=new VanStaging(new VanStaging.Placement(dock.x(),dock.y(),dock.z(),dock.yaw()));
        var sill=van.world(0,VanStaging.FRONT);
        var rooms=new ArrayList<Room>();
        var portals=new ArrayList<Portal>();
        var apertures=new ArrayList<Aperture>();
        var frames=new ArrayList<NativeInvestigationDoor.Spec>();
        var routes=new ArrayList<Route>();
        var arrival=new ArrayList<Vec>();
        var sockets=new ArrayList<Socket>();
        for(var row:rows)if(row[0].equals("PORTAL"))addAperture(row,byId,portals,apertures,frames);
        portals.add(new Portal(portals.size(),13,14,sill.x(),van.floorY(),sill.z(),VanStaging.RAMP_HALF_WIDTH*2,
            false,"trail",1));
        for(var row:rows)if(row[0].equals("LOCAL_DOOR"))addAperture(row,byId,portals,apertures,frames);
        NativeInvestigationScene nativeScene;
        try(var model=Files.newInputStream(glb);
            var collision=Files.newInputStream(hcs2)){
            nativeScene=NativeInvestigationScene.read(model,source[1],collision,source[2],stairs,spaces,frames);
        }
        for(var row:rows)switch(row[0]){
            case "ANCHOR" -> {
                var space=byId.get(row[3]);
                var p=point(row,4);
                var hit=nativeScene.physics().support(p.x(),p.z(),p.y(),.08);
                check(hit!=null,"Unsupported spawn reservation "+row[3]);
                rooms.add(new Room(Integer.parseInt(row[1]),row[2],space.x0(),space.z0(),space.x1(),space.z1(),
                    hit.y(),space.height(),space.outside()?"native-outdoor":"native-indoor","hard",
                    new Vec(p.x(),hit.y(),p.z())));
            }
            case "SOCKET" -> sockets.add(new Socket(row[1],row[2],Integer.parseInt(row[3]),
                n(row,4),n(row,5),n(row,6),n(row,7),row[8]));
            case "RITUAL" -> {
                var p=point(row,3);
                var hit=nativeScene.physics().support(p.x(),p.z(),p.y(),.20);
                check(hit!=null,"Unsupported ritual reservation "+row[1]);
                sockets.add(new Socket("campus-ritual-"+row[1],"ritual",Integer.parseInt(row[2]),
                    p.x(),hit.y(),p.z(),0,row[1]));
            }
            case "ARRIVAL" -> {
                for(int start=1;start<row.length;start+=3)arrival.add(point(row,start));
            }
            case "ROUTE" -> {
                var points=new ArrayList<Vec>();
                for(int start=3;start<row.length;start+=3)points.add(point(row,start));
                routes.add(new Route(row[1],List.copyOf(points)));
            }
            default -> {}
        }
        rooms.add(new Room(14,"Expedition van",dock.x()-4,dock.z()-1,dock.x()+4,dock.z()+1,
            van.floorY(),VanStaging.INTERIOR_HEIGHT,"van","metal",new Vec(dock.x(),van.floorY(),dock.z())));
        sockets.add(new Socket("college-scene-hash","native_scene",13,dock.x(),dock.y(),dock.z(),0,nativeScene.revision()));
        var level=new LevelDefinition("college","authored-campus-candidate","Beckenburg College",
            rooms,portals,List.of(),List.of(),List.of(),sockets,List.of(),dock,"college");
        return new Campus(new HouseMap(HouseMap.Site.COLLEGE,level,null,nativeScene),apertures,routes,List.copyOf(arrival));
    }

    private static void addAperture(String[] row, HashMap<String,NativeInvestigationScene.Space> spaces,
                                    List<Portal> portals, List<Aperture> apertures, List<NativeInvestigationDoor.Spec> frames){
        var a=spaces.get(row[2]);
        var b=spaces.get(row[3]);
        var p=point(row,4);
        boolean fitted=row[9].equals("door");
        check(a.accessible()&&b.accessible()&&(fitted||a.owner()!=b.owner()),"Invalid aperture endpoints "+row[1]);
        var portal=new Portal(portals.size(),a.owner(),b.owner(),p.x(),p.y(),p.z(),n(row,7),
            row[8].equals("z"),row[9],1);
        portals.add(portal);
        apertures.add(new Aperture(row[1],a.id(),b.id(),portal));
        if(fitted)frames.add(new NativeInvestigationDoor.Spec(row[1],a.id(),b.id(),portal,n(row,10),
            n(row,11),n(row,12),Integer.parseInt(row[13]),Integer.parseInt(row[14]),Integer.parseInt(row[15]),row[16]));
    }

    private static Vec walk(HouseMap map,Vec from,Vec to){
        return walk(map,from,to,HunterPosture.STANDING_HEIGHT);
    }

    private static Vec walk(HouseMap map,Vec from,Vec to,double height){
        int steps=Math.max(1,(int)Math.ceil(Math.hypot(to.x()-from.x(),to.z()-from.z())/.34));
        Vec feet=from;
        for(int i=1;i<=steps;i++){
            double t=i/(double)steps;
            feet=map.resolveMove(feet,from.x()+(to.x()-from.x())*t,from.z()+(to.z()-from.z())*t,height);
            if(feet==null)return null;
        }
        return feet;
    }


    private static void reached(HouseMap map,List<Vec> points,String label,double height){
        var feet=points.get(0);
        var support=map.nativeInvestigation().physics().support(feet.x(),feet.z(),feet.y(),.08);
        check(support!=null,"Missing route start "+label);
        feet=new Vec(feet.x(),support.y(),feet.z());
        check(map.traversable(feet.x(),feet.y(),feet.z(),height),"Blocked route start "+label);
        for(int i=1;i<points.size();i++){
            var to=points.get(i);
            var arrival=walk(map,feet,to,height);
            check(arrival!=null,"Blocked "+label+" at leg "+i+" "+feet+" to "+to);
            check(Math.abs(arrival.y()-to.y())<.22,"Incorrect storey "+label+" "+arrival+" vs "+to);
            feet=arrival;
        }
    }

    /** Probe the facing edges of eligible and context rooms, independent of route guides. */
    private static void auditContext(HouseMap map){
        var spaces=map.nativeInvestigation().spaces();
        int seams=0,samples=0,bodyLeaks=0,opticalLeaks=0;
        for(var open:spaces){
            if(!open.accessible()||open.outside())continue;
            for(var context:spaces){
                if(context.accessible()||context.outside()||Math.abs(open.floor()-context.floor())>.001)continue;
                for(boolean alongX:new boolean[]{true,false}){
                    double a0=alongX?open.z0():open.x0(),a1=alongX?open.z1():open.x1();
                    double b0=alongX?context.z0():context.x0(),b1=alongX?context.z1():context.x1();
                    double low=alongX?Math.max(open.x0(),context.x0()):Math.max(open.z0(),context.z0());
                    double high=alongX?Math.min(open.x1(),context.x1()):Math.min(open.z1(),context.z1());
                    if(high-low<1)continue;
                    double edge,sign;
                    if(Math.abs(a1-b0)<.001){edge=a1;sign=1;}
                    else if(Math.abs(a0-b1)<.001){edge=a0;sign=-1;}
                    else continue;
                    seams++;
                    int body=0,optical=0;
                    for(double along=low+.5;along<high-.49;along+=.25){
                        double y=open.floor();
                        double from=edge-sign*.58,to=edge+sign*.58;
                        var p=alongX?new Vec(along,y,from):new Vec(from,y,along);
                        var q=alongX?new Vec(along,y,to):new Vec(to,y,along);
                        if(!map.traversable(p.x(),y,p.z()))continue;
                        if(!context.id().equals(map.architecturalRoomAt(q.x(),y+1.25,q.z())))continue;
                        samples++;
                        if(walk(map,p,q)!=null)body++;
                        if(map.clearLineOfSight(p.x(),y+1.25,p.z(),q.x(),y+1.25,q.z()))optical++;
                    }
                    bodyLeaks+=body;
                    opticalLeaks+=optical;
                    if(body>0||optical>0)System.out.printf("CONTEXT_OPEN %s %s %s %.3f %.3f %.3f body=%d optical=%d%n",
                        open.id(),context.id(),alongX?"z":"x",edge,low,high,body,optical);
                }
            }
        }
        System.out.printf("CONTEXT seams=%d samples=%d body_open=%d optical_open=%d%n",seams,samples,bodyLeaks,opticalLeaks);
        check(samples>1000&&bodyLeaks==0&&opticalLeaks==0,"Same-level context seal regression");
    }

    static void audit(Campus campus){
        var map=campus.map();
        check(!new HouseMap(HouseMap.Site.COLLEGE).nativeSite(),"Candidate replaced public College");
        check(map.nativeSite()&&map.valid(),"Candidate graph or spawn poses");
        check(map.nativeInvestigation().spaces().stream().filter(NativeInvestigationScene.Space::accessible).count()==40,"Search budget");
        check(RitualSites.valid(map),"Ritual reservations");
        var navigation=GhostAiNavigationAudit.inspect(map,0);
        check(navigation.inspectedRooms()==14&&navigation.findings().isEmpty(),"Ghost navigation graph "+navigation.findings());
        System.out.printf("GHOST_GRAPH rooms=%d directed_edges=%d%n",navigation.inspectedRooms(),navigation.links().size());
        int passage=LanAcoustics.physicalCostAt(map,0,new Vec(22,0,44),5,new Vec(13,0,44));
        check(passage<Integer.MAX_VALUE,"No host acoustic route through the lecture opening");
        check(LanAcoustics.physicalCostAt(map,0,new Vec(22,3.6,44),5,new Vec(13,0,44))==Integer.MAX_VALUE,
            "Host acoustic route accepted a mismatched source volume");
        for(int owner=0;owner<15;owner++){
            var point=map.spawn(owner);
            check(map.roomAt(point.x(),point.y(),point.z())==owner,"Spawn owner "+owner);
        }
        check("teaching-hall".equals(map.architecturalRoomAt(22,1.2,31))
            &&"upper-teaching".equals(map.architecturalRoomAt(22,4.8,31))
            &&"west-tunnel".equals(map.architecturalRoomAt(22,-1.8,31)),"Stacked room ownership");
        for(var aperture:campus.apertures()){
            var portal=aperture.portal();
            int loss=LanAcoustics.physicalEdgeCost(map,portal.a(),portal.b());
            check(loss<Integer.MAX_VALUE,"Acoustics lacks "+aperture.id());
            if(!portal.kind().equals("opening"))continue;
            double x=portal.x(),y=portal.y(),z=portal.z(),dx=portal.alongX()?0:.7,dz=portal.alongX()?.7:0;
            check(map.clearLineOfSight(x-dx,y+1.3,z-dz,x+dx,y+1.3,z+dz),"Occluded opening "+aperture.id());
            var from=new Vec(x-dx,y,z-dz);var to=new Vec(x+dx,y,z+dz);
            check(walk(map,from,to)!=null&&walk(map,to,from)!=null,"Movement through opening "+aperture.id());
            var source=map.nativeInvestigation().spaceAt(from.x(),y,from.z());
            var target=map.nativeInvestigation().spaceAt(to.x(),y,to.z());
            check(source!=null&&target!=null&&List.of(source.id(),target.id()).containsAll(List.of(aperture.a(),aperture.b())),
                "Wrong spaces at aperture "+aperture.id()+" "+source+" "+target);
        }
        check(!map.clearLineOfSight(20.8,1.3,60,20.8,1.3,64),"Teaching exterior wall optical control");
        check(walk(map,new Vec(20.8,0,60),new Vec(20.8,0,64))==null,"Teaching wall movement control");
        check(!map.clearObjectPath(20.8,1.3,60,20.8,1.3,64,.05,.05),"Teaching wall object-sweep control");

        var van=map.van();
        var seat=van.spawn(0);
        var ramp=van.world(0,VanStaging.FRONT-VanStaging.RAMP_LENGTH-.1);
        var aisle=van.world(0,-1.1);
        var start=new Vec(seat.x(),van.floorY(),seat.z());
        var center=new Vec(aisle.x(),van.floorY(),aisle.z());
        var ground=new Vec(ramp.x(),van.placement().y(),ramp.z());
        check(walk(map,start,center)!=null&&walk(map,center,ground)!=null&&walk(map,ground,center)!=null,
            "Canonical cargo ramp fails normal movement");
        van.setCargoDoorClosed(true);
        check(walk(map,center,ground)==null,"Closed van shutter permits escape");
        van.setCargoDoorClosed(false);
        check(LanAcoustics.edgeCost(map,13,14)==Integer.MAX_VALUE,"Van ghost hearing isolation");
        auditContext(map);
        replayRoutes(campus,HunterPosture.STANDING_HEIGHT);
        System.out.printf("PASS spaces=40 owners=15 runtime_route_directions=%d aperture_pairs=%d rituals=3 collision_sha=%s scene_sha=%s%n",
            2*campus.routes().size(),campus.apertures().size(),map.nativeInvestigation().physics().sha256(),map.nativeInvestigation().revision());
    }

    static void replayRoutes(Campus campus,double height){
        var map=campus.map();
        var failures=new ArrayList<String>();
        for(var route:campus.routes()){
            var reversed=new ArrayList<>(route.points());
            java.util.Collections.reverse(reversed);
            try{reached(map,route.points(),route.id()+" forward",height);}
            catch(AssertionError failure){failures.add(failure.getMessage());}
            try{reached(map,reversed,route.id()+" reverse",height);}
            catch(AssertionError failure){failures.add(failure.getMessage());}
        }
        failures.forEach(System.out::println);
        check(failures.isEmpty(),"Native gameplay route failures "+failures.size()+"/"+2*campus.routes().size());
    }

    private static void capture(HouseMap map,Path out)throws IOException{
        Files.createDirectories(out);
        var scene=new SceneRenderer();
        scene.setRenderSize(720,450);
        scene.setPsxPreset(PsxPipeline.Preset.OFF);
        scene.lamp=true;
        scene.setLampHardware(EquipmentTiers.Tier.III,false);
        String[] views={"arrival","teaching-entry","cafe-entry","library-issue","north-stair","cross-tunnel"};
        double[][] cameras={{26,0,92,32,96},{24,0,65,24,60},{38,0,58,45,58},
            {31,0,24,31,18},{64,0,32,65,37},{33,-3,40,42,40}};
        for(int i=0;i<views.length;i++){
            var c=cameras[i];
            var support=map.nativeInvestigation().physics().support(c[0],c[2],c[1],.08);
            check(support!=null&&map.traversable(c[0],support.y(),c[2]),"Unsupported camera "+views[i]);
            scene.x=c[0];scene.eyeHeight=support.y()+HunterPosture.STANDING_EYE;scene.z=c[2];
            scene.yaw=Math.atan2(c[3]-c[0],c[4]-c[2]);scene.pitch=-.04;
            scene.drawNetwork(map,List.of(),-1,0);
            if(GpuWorldRenderer.active()!=null){
                check(scene.awaitSubmittedGpuPrewarm(30_000_000_000L,()->false),"No GL result");
                scene.pinCompletedGpuFrame(GpuWorldRenderer.active().latest());
                scene.drawNetwork(map,List.of(),-1,0);
                check(scene.frameDiagnostics().gpu()&&scene.frameDiagnostics().gpuReadbackAvailable(),"Missing actual GL readback");
            }
            check(scene.frameDiagnostics().faces()>300&&scene.frameDiagnostics().frustumFaces()>50,"Empty frame "+views[i]);
            check(scene.atmosphereFrame().opaqueMetres()<=24,"Missing shared fog");
            ImageIO.write(scene.image(),"png",out.resolve(views[i]+".png").toFile());
            System.out.printf("FRAME %s %s room=%d visible=%d depth=%d%n",views[i],
                GpuWorldRenderer.active()==null?"software":"OpenGL",scene.frameDiagnostics().room(),
                scene.frameDiagnostics().frustumFaces(),scene.frameDiagnostics().depthRejectedFaces());
        }
    }

    public static void main(String[] args)throws Exception{
        var campus=load(Path.of(args[0]));
        audit(campus);
        if(args.length<2)return;
        java.lang.reflect.Method stop=null;
        if(List.of(args).contains("--gpu")){
            var launcher=Class.forName("org.beckenburg.gpu.GpuDesktop");
            var start=launcher.getDeclaredMethod("startGpu");
            start.setAccessible(true);
            start.invoke(null);
            stop=launcher.getDeclaredMethod("stopReviewGpu");
            stop.setAccessible(true);
        }
        try{capture(campus.map(),Path.of(args[1]));}
        finally{if(stop!=null)stop.invoke(null);}
    }
}
