package org.beckenburg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.imageio.ImageIO;
import org.beckenburg.LevelDefinition.Portal;
import org.beckenburg.LevelDefinition.Vec;

/** Source-backed partitions and leaves through ordinary native movement and the real LAN host. */
public final class CollegeNativeDoorsTest {
    private static final double[] POSTURES = {HunterPosture.STANDING_HEIGHT,HunterPosture.CROUCH_HEIGHT};
    private static final double[] LANES = {-.23,0,.23};

    private static void check(boolean ok, String reason) {
        if(!ok)throw new AssertionError(reason);
    }

    private static Vec at(Portal p, double normal, double tangent, double y) {
        return p.alongX() ? new Vec(p.x()+tangent,y,p.z()+normal) : new Vec(p.x()+normal,y,p.z()+tangent);
    }

    private static Vec landing(HouseMap map, Portal p, double normal, double tangent) {
        var at=at(p,normal,tangent,p.y());
        Double support=map.supportNear(at.x(),at.z(),at.y());
        check(support!=null,"Unsupported receiving floor at portal "+p.id()+" "+at);
        return new Vec(at.x(),support,at.z());
    }

    private static boolean walk(HouseMap map, Vec from, Vec to, double bodyHeight) {
        int steps=Math.max(1,(int)Math.ceil(Math.hypot(to.x()-from.x(),to.z()-from.z())/.075));
        var feet=from;
        for(int i=1;i<=steps;i++){
            double t=i/(double)steps;
            feet=map.resolveMove(feet,from.x()+(to.x()-from.x())*t,from.z()+(to.z()-from.z())*t,bodyHeight);
            if(feet==null)return false;
        }
        return Math.abs(feet.y()-to.y())<.10;
    }

    private static boolean sight(HouseMap map, Vec a, Vec b, double y) {
        return map.clearLineOfSight(a.x(),y,a.z(),b.x(),y,b.z());
    }

    private static boolean objectPath(HouseMap map, Vec a, Vec b, double y) {
        return map.clearObjectPath(a.x(),y,a.z(),b.x(),y,b.z(),.035,.035);
    }

    private static void checkedState(HouseMap map, NativeInvestigationDoor door, boolean open) {
        var p=door.spec().portal();
        var name=door.spec().frameId();
        check(map.doorOpen(p.id())==open,"Unexpected leaf state "+name);
        for(double lane:LANES) {
            var a=landing(map,p,-.76,lane);
            var b=landing(map,p,.76,lane);
            for(double height:POSTURES) {
                check(map.traversable(a.x(),a.y(),a.z(),height)&&map.traversable(b.x(),b.y(),b.z(),height),
                    name+" receiving floor blocked at lane "+lane+" open="+open);
                check(walk(map,a,b,height)==open&&walk(map,b,a,height)==open,
                    name+" traversal does not match leaf state at lane "+lane+" open="+open);
            }
            for(double height:new double[]{.30,1.16,door.spec().clearHeight()-.11}){
                check(sight(map,a,b,p.y()+height)==open,
                    name+" sight does not match the leaf at lane "+lane+" height="+height+" open="+open);
                check(objectPath(map,a,b,p.y()+height)==open,
                    name+" object sweep does not match the leaf at lane "+lane+" height="+height+" open="+open);
            }
        }
        for(double sign:new double[]{-1,1}) {
            double edge=sign*(p.width()/2+.025);
            for(double height:new double[]{.30,1.22,door.spec().clearHeight()-.08}) {
                var a=at(p,-.44,edge,p.y()+height);
                var b=at(p,.44,edge,p.y()+height);
                check(!sight(map,a,b,a.y()),name+" sight leak around the jamb at "+a);
            }
        }
    }

    private static int acoustic(HouseMap map, Portal p) {
        var a=landing(map,p,-.76,0);
        var b=landing(map,p,.76,0);
        return LanAcoustics.physicalCostAt(map,map.roomAt(a.x(),a.y(),a.z()),a,map.roomAt(b.x(),b.y(),b.z()),b);
    }

    private static void audit(CollegeNativeRuntimeReview.Campus campus) {
        var map=campus.map();
        var doors=map.nativeInvestigation().doors();
        check(doors.size()==18,"Fitted leaf count");
        check(map.level().portals().get(15).joins(13,14),"Van portal ID was changed by local doors");
        int local=0;
        for(var door:doors) {
            var spec=door.spec();
            var p=spec.portal();
            var a=landing(map,p,-.76,0);
            var b=landing(map,p,.76,0);
            String near=map.nativeInvestigation().spaceAt(a.x(),a.y(),a.z()).id();
            String far=map.nativeInvestigation().spaceAt(b.x(),b.y(),b.z()).id();
            check(List.of(near,far).containsAll(List.of(spec.roomA(),spec.roomB())),
                "Fitted tabs lead to the wrong physical rooms: "+spec.frameId()+" "+near+"/"+far);
            if(p.a()==p.b()){
                local++;
                check(!map.adjacentRooms(p.a()).contains(p.a()),"Child door added a graph self-loop");
            }
            checkedState(map,door,false);
            int closed=acoustic(map,p);
            map.toggleDoor(p.id(),System.nanoTime()-2_000_000_000L);
            checkedState(map,door,true);
            int open=acoustic(map,p);
            check(closed<Integer.MAX_VALUE&&open<closed,"Acoustic loss mismatch at "+spec.frameId()+" "+closed+"/"+open);
            System.out.printf("FITTED %s id=%d owners=%d/%d acoustic=%d->%d%n",spec.frameId(),p.id(),p.a(),p.b(),closed,open);
        }
        CollegeNativeRuntimeReview.audit(campus);
        CollegeNativeRuntimeReview.replayRoutes(campus,HunterPosture.CROUCH_HEIGHT);
        joinedArrivals(campus);
        System.out.printf("PASS %d fitted doors, %d local doors, 432 mover sweeps, 324 sight, 324 small-object and 216 jamb rays; 88 route directions in both postures%n",
            doors.size(),local);
    }

    private static void append(List<Vec> route, List<Vec> segment) {
        var previous=route.get(route.size()-1);
        var next=segment.get(0);
        check(Math.hypot(previous.x()-next.x(),previous.z()-next.z())<.015&&Math.abs(previous.y()-next.y())<.03,
            "Discontinuous guides: "+previous+" to "+next);
        route.addAll(segment.subList(1,segment.size()));
    }

    private static List<Vec> cargoStart(HouseMap map) {
        var van=map.van();
        var seat=van.spawn(0);
        var aisle=van.world(0,-1.1);
        var toe=van.world(0,VanStaging.FRONT-VanStaging.RAMP_LENGTH);
        return new ArrayList<>(List.of(new Vec(seat.x(),van.floorY(),seat.z()),
            new Vec(aisle.x(),van.floorY(),aisle.z()),new Vec(toe.x(),van.placement().y(),toe.z())));
    }

    private static void appendLinks(List<Vec> journey, CollegeNativeRuntimeReview.Campus campus, String... links) {
        for(String key:links) {
            var route=campus.routes().stream().filter(r->r.id().equals(key.replace("-",""))).findFirst().orElseThrow();
            var points=new ArrayList<>(route.points());
            if(key.startsWith("-"))Collections.reverse(points);
            append(journey,points);
        }
    }

    private static Vec traverse(HouseMap map, List<Vec> points, double height) {
        var feet=points.get(0);
        for(int i=1;i<points.size();i++){
            var target=points.get(i);
            var from=feet;
            int steps=Math.max(1,(int)Math.ceil(Math.hypot(target.x()-from.x(),target.z()-from.z())/.19));
            for(int j=1;j<=steps;j++){
                double ratio=j/(double)steps;
                feet=map.resolveMove(feet,from.x()+ratio*(target.x()-from.x()),from.z()+ratio*(target.z()-from.z()),height);
                if(feet==null)return null;
            }
            if(Math.abs(feet.y()-target.y())>.22)return null;
        }
        return feet;
    }

    private static void joinedArrivals(CollegeNativeRuntimeReview.Campus campus) {
        var map=campus.map();
        var maintenance=cargoStart(map);
        append(maintenance,campus.arrival());
        appendLinks(maintenance,campus,"L04","L11","-L84","-L68","L67");
        var services=maintenance.get(maintenance.size()-1);
        check(map.roomAt(services.x(),services.y(),services.z())==11&&services.y()==-3,"Service arrival chose the wrong datum");
        var library=cargoStart(map);
        appendLinks(library,campus,"L01","L02","L17","L19","L81","L51","L52","L55","L56","L88","L57");
        var residence=library.get(library.size()-1);
        check(map.roomAt(residence.x(),residence.y(),residence.z())==9&&residence.y()==3.6,"Upper arrival chose the wrong datum");
        for(var journey:List.of(maintenance,library)){
            var reverse=new ArrayList<>(journey);
            Collections.reverse(reverse);
            for(double height:POSTURES)
                check(traverse(map,journey,height)!=null&&traverse(map,reverse,height)!=null,"Continuous campus journey failed");
        }
        System.out.println("PASS continuous van-to-maintenance and van-to-upper-residence journeys in both postures and directions");
    }

    private static LooseClutter.Observer aim(HouseMap map, NativeInvestigationDoor door, boolean open) {
        var spec=door.spec();
        var p=spec.portal();
        for(double side:new double[]{-spec.swingDirection(),spec.swingDirection()})
            for(double dist:new double[]{.80,1.05,1.31})
                for(double drift:new double[]{-.26,0,.26}) {
                    var start=at(p,side*dist,drift,p.y());
                    Double y=map.supportNear(start.x(),start.z(),start.y());
                    if(y==null||!map.traversable(start.x(),y,start.z()))continue;
                    var face=open?at(p,spec.swingDirection()*p.width()/spec.leaves()*.54,
                        spec.hingeSide()*p.width()/2,p.y()+1.40):at(p,0,p.width()*.20,p.y()+1.40);
                    double dx=face.x()-start.x(),dz=face.z()-start.z();
                    var viewer=new LooseClutter.Observer(0,start.x(),y,start.z(),Math.atan2(dx,dz),
                        Math.atan2(face.y()-y-HunterPosture.STANDING_EYE,Math.hypot(dx,dz)),true);
                    var target=InteractionFocus.pick(map,viewer,List.of());
                    if(target!=null&&target.kind()==InteractionFocus.Kind.DOOR&&target.id().equals(Integer.toString(p.id())))
                        return viewer;
                }
        throw new AssertionError("No supported aim on the actual leaf "+spec.frameId()+" open="+open);
    }

    private static void peers(HouseMap map, HouseMap replica) throws Exception {
        map.applyDoorSnapshot("");
        try(var host=new LanHost("127.0.0.1",0,GhostIdentity.Type.SPIRIT,CustomContract.standard(3),map);
            var owner=new AuthoredLevelNetworkTest.Peer(host.port(),false,"Surveyor");
            var receiver=new AuthoredLevelNetworkTest.Peer(host.port(),false,"Observer");
            var hunter=new AuthoredLevelNetworkTest.Peer(host.port(),false,"Investigator");
            var ghost=new AuthoredLevelNetworkTest.Peer(host.port(),true,"Spirit")) {
            owner.await("SITE|COLLEGE|"+replica.level().revision());
            owner.client.ready();receiver.client.ready();hunter.client.ready();ghost.client.ready();
            owner.await("LOBBY|3|HUMAN|4|WAITING");
            owner.client.start();owner.await("INFO|mission started");
            owner.client.view3D();owner.await("ACCEPT|VIEW3D");
            receiver.client.view3D();receiver.await("ACCEPT|VIEW3D");receiver.await("DOORS|");
            var world=(LanWorld)LanSafeAreaBarrierTest.field(host,"world");
            for(var fitted:map.nativeInvestigation().doors()) {
                int id=fitted.spec().portal().id();
                for(boolean wasOpen:new boolean[]{false,true}) {
                    var viewer=aim(map,fitted,wasOpen);
                    HouseRebuildPowerTest.interact(host,map,world,owner,viewer);
                    owner.await("ACCEPT|INTERACT|"+(wasOpen?"Close door":"Open door"));
                    String expected="DOORS|"+(wasOpen?"":id);
                    String snapshot=receiver.await("DOORS|");
                    for(int attempt=0;attempt<20&&!snapshot.equals(expected);attempt++)
                        snapshot=receiver.await("DOORS|");
                    check(snapshot.equals(expected),"Wrong received leaf state "+snapshot);
                    replica.applyDoorSnapshot(snapshot.substring(6));
                    check(replica.doorOpen(id)!=wasOpen&&map.doorOpen(id)!=wasOpen,"Leaf state differs on peers");
                }
                System.out.println("REPLICATED "+fitted.spec().frameId());
            }
        }
        System.out.println("PASS 36 real host door actions broadcast to a second native scene");
    }

    private static void capture(HouseMap map, Path output) throws Exception {
        Files.createDirectories(output);
        String[] views={"COLLEGE-COURT-teaching-lobby","L06","L14","L52","L67","COLLEGE bay indexed door 4"};
        for(String view:views){
            var door=map.nativeInvestigation().doors().stream().filter(d->d.spec().frameId().equals(view)).findFirst().orElseThrow();
            var p=door.spec().portal();
            map.applyDoorSnapshot("");
            var observer=aim(map,door,false);
            var renderer=new SceneRenderer();
            renderer.setRenderSize(1000,625);
            renderer.setPsxPreset(PsxPipeline.Preset.OFF);
            renderer.roomLights=new RoomLighting();
            renderer.setLampHardware(EquipmentTiers.Tier.III,false);
            renderer.weatherModule().setTimeOfDay(.52);
            renderer.weatherModule().pauseClock(true);
            renderer.lamp=true;
            renderer.x=observer.x();renderer.z=observer.z();renderer.eyeHeight=observer.eyeY();
            renderer.yaw=Math.atan2(p.x()-observer.x(),p.z()-observer.z());
            renderer.pitch=-.10;
            for(boolean open:new boolean[]{false,true}){
                if(open)map.toggleDoor(p.id(),System.nanoTime()-2_000_000_000L);
                renderer.drawNetwork(map,List.of(),-1,0);
                var gpu=GpuWorldRenderer.active();
                if(gpu!=null){
                    check(renderer.awaitSubmittedGpuPrewarm(30_000_000_000L,()->false),"No OpenGL door frame");
                    renderer.pinCompletedGpuFrame(gpu.latest());
                    renderer.drawNetwork(map,List.of(),-1,0);
                    check(renderer.frameDiagnostics().gpuReadbackAvailable(),"No actual OpenGL door readback");
                }
                check(renderer.frameDiagnostics().frustumFaces()>100,"Missing capture geometry");
                check(ImageIO.write(renderer.image(),"png",output.resolve(view.replace(' ','-')+(open?"-open.png":"-closed.png")).toFile()),
                    "Unable to write standing capture of "+view);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Path dir=Path.of(args[0]), manifest=Path.of(args[1]);
        var glb=dir.resolve("college-doors.glb");
        var hcs=dir.resolve("college-doors.hcs2");
        var campus=CollegeNativeRuntimeReview.load(manifest,glb,hcs);
        audit(campus);
        if(List.of(args).contains("--geometry-only"))return;
        peers(campus.map(),CollegeNativeRuntimeReview.load(manifest,glb,hcs).map());
        CollegeNativeElectricalTest.verifyControlsAndPower(campus.map());
        if(args.length>2){
            java.lang.reflect.Method stop=null;
            if(List.of(args).contains("--gpu")){
                var launcher=Class.forName("org.beckenburg.gpu.GpuDesktop");
                var start=launcher.getDeclaredMethod("startGpu");
                start.setAccessible(true);
                start.invoke(null);
                stop=launcher.getDeclaredMethod("stopReviewGpu");
                stop.setAccessible(true);
            }
            try{capture(campus.map(),Path.of(args[2]));}
            finally{if(stop!=null)stop.invoke(null);}
        }
    }
}
