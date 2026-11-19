package org.beckenburg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import javax.imageio.ImageIO;
import org.beckenburg.LevelDefinition.Portal;
import org.beckenburg.LevelDefinition.Socket;
import org.beckenburg.LevelDefinition.Vec;

/** Exercise the exact Battery journey through the shared production game kernel. */
public final class NativeSeacliffReceivingProbe {
    private static double d(String[] row,int index){return Double.parseDouble(row[index]);}
    private static Vec point(String[] row,int index){return new Vec(d(row,index),d(row,index+1),d(row,index+2));}
    private static void check(boolean valid,String why){if(!valid)throw new AssertionError(why);}

    private static String walk(HouseMap map,List<Vec> route,double height){
        return walk(map,route,height,false);
    }

    private static String walk(HouseMap map,List<Vec> route,double height,boolean checkOwnership){
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
                if(checkOwnership&&map.roomAt(next.x(),next.y(),next.z())<0)return "missing room owner at "+next;
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

    private static Vec fittedApproach(Portal p,double side,double lane) {
        return p.alongX()?new Vec(p.x()+lane,p.y(),p.z()+side):new Vec(p.x()+side,p.y(),p.z()+lane);
    }

    private static int checkFittedDoor(HouseMap map, Portal p, boolean open) {
        for(double lane:new double[]{-.23,0,.23}){
            var a=fittedApproach(p,-.84,lane);
            var b=fittedApproach(p,.84,lane);
            check(map.roomAt(a.x(),a.y(),a.z())==p.a()&&map.roomAt(b.x(),b.y(),b.z())==p.b(),
                "Wrong fitted room owners "+p.id());
            for(double height:new double[]{HunterPosture.STANDING_HEIGHT,HunterPosture.CROUCH_HEIGHT}){
                check(map.traversable(a.x(),a.y(),a.z(),height)&&map.traversable(b.x(),b.y(),b.z(),height),
                    "Blocked receiver at leaf "+p.id());
                check(walk(map,List.of(a,b),height).isEmpty()==open
                    &&walk(map,List.of(b,a),height).isEmpty()==open,"Moving leaf collision "+p.id());
            }
            for(double height:new double[]{.3,1.2,2.10}){
                check(map.clearLineOfSight(a.x(),p.y()+height,a.z(),b.x(),p.y()+height,b.z())==open,"Fitted leaf sight "+p.id());
                check(map.clearObjectPath(a.x(),p.y()+height,a.z(),b.x(),p.y()+height,b.z(),.035,.035)==open,
                    "Fitted leaf object motion "+p.id());
            }
        }
        for(double lane:new double[]{-.89,.89}){
            var a=fittedApproach(p,-.84,lane);
            var b=fittedApproach(p,.84,lane);
            check(!map.clearLineOfSight(a.x(),p.y()+1.25,a.z(),b.x(),p.y()+1.25,b.z()),"Frame detached from pier "+p.id());
        }
        if(!open){
            var at=fittedApproach(p,-1,0);
            double yaw=p.alongX()?0:Math.PI/2;
            var observer=new LooseClutter.Observer(0,at.x(),at.y(),at.z(),yaw,-.18,true);
            var target=InteractionFocus.pick(map,observer,List.of());
            check(target!=null&&target.kind()==InteractionFocus.Kind.DOOR&&target.id().equals(Integer.toString(p.id())),
                "Actual closed leaf cannot be aimed at "+p.id());
        }
        return LanAcoustics.physicalCostAt(map,p.a(),fittedApproach(p,-.84,0),p.b(),fittedApproach(p,.84,0));
    }

    private static void closedOldGaps(HouseMap map){
        // Former oblique clearances in the inherited wall fragments.
        for(double[] station:new double[][]{{24,24.44,0},{44,24.44,0},{26.8,26,1},{32,26,1},{41.2,26,1}}){
            double x=station[0],z=station[1],dz=station[2]*.8,dx=.8-dz;
            check(!map.clearLineOfSight(x-dx,1.25,z-dz,x+dx,1.25,z+dz),"Old lower wall gap remains visible");
            check(!map.clearObjectPath(x-dx,1.25,z-dz,x+dx,1.25,z+dz,.035,.035),"Old lower wall passes a small object");
            var a=new Vec(x-dx,0,z-dz);
            var b=new Vec(x+dx,0,z+dz);
            for(double height:new double[]{HunterPosture.STANDING_HEIGHT,HunterPosture.CROUCH_HEIGHT}){
                check(map.traversable(a.x(),0,a.z(),height)&&map.traversable(b.x(),0,b.z(),height),
                    "Former gap test lacks supported approaches");
                check(!walk(map,List.of(a,b),height).isEmpty()&&!walk(map,List.of(b,a),height).isEmpty(),
                    "Old lower wall gap still passes a body");
            }
        }
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

    private static final class Peer implements AutoCloseable {
        private final java.util.concurrent.BlockingQueue<String> pending=new java.util.concurrent.LinkedBlockingQueue<>();
        final LanClient client;
        Peer(LanHost host,boolean ghost,String name)throws Exception{
            client=new LanClient("127.0.0.1",host.port(),ghost,name,pending::offer);
        }
        String waitFor(String prefix)throws Exception{
            long deadline=System.nanoTime()+5_000_000_000L;
            while(System.nanoTime()<deadline){
                var line=pending.poll(80,java.util.concurrent.TimeUnit.MILLISECONDS);
                if(line!=null&&line.startsWith(prefix))return line;
            }
            throw new AssertionError("Missing live frame "+prefix);
        }
        @Override public void close(){client.close();}
    }

    private static Vec receivedPose(String body){
        var row=LanProtocol.parts(body);
        return new Vec(Integer.parseInt(row.get(2))/1000.0,Integer.parseInt(row.get(3))/1000.0,
            Integer.parseInt(row.get(4))/1000.0);
    }

    private static void hostArrival(HouseMap map)throws Exception{
        try(var host=new LanHost("127.0.0.1",0,GhostIdentity.Type.SPIRIT,CustomContract.standard(3),map);
            var first=new Peer(host,false,"Surveyor");
            var second=new Peer(host,false,"Observer");
            var third=new Peer(host,false,"Hunter");
            var ghost=new Peer(host,true,"Spirit")){
            var expected="SITE|"+map.site().name()+"|"+map.level().revision();
            for(var peer:List.of(first,second,third,ghost)){
                check(peer.waitFor("SITE|").equals(expected),"Different staged scene revision on TCP");
                peer.client.ready();
            }
            first.waitFor("LOBBY|3|HUMAN|4|WAITING");
            first.client.start();
            first.waitFor("INFO|mission started");
            first.client.view3D();
            first.waitFor("ACCEPT|VIEW3D");
            var deck=receivedPose(first.waitFor("BODY|0|"));
            check(map.roomAt(deck.x(),deck.y(),deck.z())==14,"Host missed supported cargo floor");
            second.client.view3D();
            second.waitFor("ACCEPT|VIEW3D");
            var outside=deck;
            for(int i=0;i<9;i++){
                Thread.sleep(125);
                first.client.step(0,-100);
                first.waitFor("ACCEPT|STEP");
                outside=receivedPose(first.waitFor("BODY|0|"));
                check(map.traversable(outside.x(),outside.y(),outside.z()),"TCP pose disagrees with authored terrain");
            }
            check(map.roomAt(outside.x(),outside.y(),outside.z())==13&&Math.abs(outside.y())<.01,
                "Host movement missed the Battery forecourt "+outside);
            boolean seen=false;
            for(int i=0;i<50&&!seen;i++)seen=receivedPose(second.waitFor("BODY|0|")).equals(outside);
            check(seen,"Observer missed host's actual ramp traversal");
            ghost.client.viewGhost3D();
            ghost.waitFor("ACCEPT|GVIEW3D");
            check(ghost.waitFor("SITE|").equals(expected),"Ghost received a different site revision");
            ghost.waitFor("GPOSE|");
            System.out.println("Real staged host and four seats: "+deck+" to "+outside);
        }
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
        SeacliffNativeLevel.Source packet;
        try(var manifest=Files.newInputStream(Path.of(args[0]));
            var glb=Files.newInputStream(Path.of(source[1]));
            var hcs=Files.newInputStream(Path.of(source[3]))){
            packet=SeacliffNativeLevel.read(manifest,glb,hcs);
        }
        var level=packet.definition();
        var scene=packet.scene();
        var doors=packet.doors();
        var portal=doors.get(0).portal();
        var staging=level.dock();
        var rooms=level.rooms();
        var routes=new LinkedHashMap<String,List<Vec>>();
        var guides=new LinkedHashMap<String,List<Vec>>();
        for(var row:rows){
            if(row[0].equals("POINT"))routes.computeIfAbsent(row[1],key->new ArrayList<>()).add(point(row,2));
            if(row[0].equals("LINK"))guides.computeIfAbsent(row[1],key->new ArrayList<>()).add(point(row,2));
        }
        var host=new HouseMap(HouseMap.Site.USER,level,null,scene);
        var guest=new HouseMap(HouseMap.Site.USER,level,null,scene);
        if(packet.transitionCount()>0)check(host.valid()&&guest.valid(),"Actual physical transitions did not connect all 15 room owners");
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
            Vec spawn=rooms.get(i).spawn();
            check(host.roomAt(spawn.x(),spawn.y(),spawn.z())==i&&host.traversable(spawn.x(),spawn.y(),spawn.z()),
                "Unsupported native room anchor "+rooms.get(i).name()+" at "+spawn);
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
        long lowerCount=doors.stream().skip(1).filter(d->d.portal().y()==0).count();
        long upperCount=doors.stream().skip(1).filter(d->d.portal().y()==4).count();
        var lowerView=new Vec(29.25,0,28.1);
        var courtyardView=new Vec(26.55,4,37.889);
        if(lowerCount>0){
            capture(renderer,host,directory.resolve("battery-lower-gallery-closed.png"),lowerView,Math.PI,-.055);
            closedOldGaps(host);
        }
        if(upperCount>0)
            capture(renderer,host,directory.resolve("battery-west-courtyard-closed.png"),courtyardView,-Math.PI/2,-.045);
        for(var spec:doors.subList(1,doors.size())){
            var p=spec.portal();
            int closedLoss=checkFittedDoor(host,p,false);
            host.toggleDoor(p.id(),System.nanoTime()-2_000_000_000L);
            guest.applyDoorSnapshot(host.doorSnapshot());
            int openLoss=checkFittedDoor(host,p,true);
            check(checkFittedDoor(guest,p,true)==openLoss&&closedLoss>openLoss,
                "Fitted acoustic and replicated door disagreement "+spec.frameId());
            System.out.println(spec.frameId()+" native door and acoustic "+closedLoss+" -> "+openLoss);
        }
        if(lowerCount>0){
            closedOldGaps(guest);
            capture(renderer,host,directory.resolve("battery-lower-gallery-open.png"),lowerView,Math.PI,-.055);
        }
        if(upperCount>0)
            capture(renderer,host,directory.resolve("battery-west-courtyard-open.png"),courtyardView,-Math.PI/2,-.045);
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
        for(var entry:guides.entrySet()){
            var reverse=new ArrayList<>(entry.getValue());
            Collections.reverse(reverse);
            for(double height:new double[]{HunterPosture.STANDING_HEIGHT,HunterPosture.CROUCH_HEIGHT}){
                boolean measuredOwnership=packet.transitionCount()>0;
                String inbound=walk(host,entry.getValue(),height,measuredOwnership);
                String outbound=walk(host,reverse,height,measuredOwnership);
                check(inbound.isEmpty(),entry.getKey()+" guide forward "+inbound);
                check(outbound.isEmpty(),entry.getKey()+" guide return "+outbound);
            }
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
            "native_room_volumes\t"+scene.spaces().size(),
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
        if(lowerCount>0)report.add("lower_fitted_doors_and_closed_former_gaps\t"+lowerCount);
        if(upperCount>0)report.add("upper_fitted_gallery_and_courtyard_doors\t"+upperCount);
        if(packet.transitionCount()>0)report.add("physical_graph_and_continuous_ownership\t15 owners, "+packet.transitionCount()+" measured stair/ramp transitions");
        if(!guides.isEmpty())report.add("surveyed_guides_both_directions_and_postures\t"+guides.size());
        if(host.level().socket("generator",9)!=null)report.addAll(power(host,renderer,directory));
        if(List.of(args).contains("--lan")){
            // This exercises the exact candidate through TCP before packaging it as a normal site.
            var stagedHost=new HouseMap(HouseMap.Site.USER,level,null,scene);
            hostArrival(stagedHost);
            report.add("staged_host_four_tcp_peers_and_canonical_ramp\tPASS");
        }
        Files.write(Path.of(args[2]),report);
    }
}
