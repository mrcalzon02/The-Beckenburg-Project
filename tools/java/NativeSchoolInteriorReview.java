
package org.beckenburg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.beckenburg.LevelDefinition.Dock;
import org.beckenburg.LevelDefinition.Portal;
import org.beckenburg.LevelDefinition.Room;
import org.beckenburg.LevelDefinition.Socket;
import org.beckenburg.LevelDefinition.Vec;

/** Replays the exact native School leaves and routes through the production game kernel. */
public final class NativeSchoolInteriorReview {
    static double d(String[] row,int i){return Double.parseDouble(row[i]);}
    static Vec point(Portal p,double normal,double tangent,double h){
        return new Vec(p.x()+(p.alongX()?tangent:normal),p.y()+h,p.z()+(p.alongX()?normal:tangent));
    }
    static boolean sweep(AuthoredSitePhysics physics, NativeInvestigationDoor.Spec s) {
        var p=s.portal();
        double baseline=(s.frameFrom()+s.frameTo())/2-(p.alongX()?p.z():p.x());
        for(int side:s.leaves()==1?new int[]{s.hingeSide()}:new int[]{-1,1}){
            for(int a=1;a<=18;a++){
                double angle=Math.PI*a/36;
                for(double fraction:new double[]{.15,.35,.55,.75,.94}){
                    double length=p.width()/s.leaves()*fraction;
                    double tangent=side*(p.width()/2-length*Math.cos(angle));
                    double normal=baseline+s.swingDirection()*length*Math.sin(angle);
                    for(double t:new double[]{0,-.033,.033}){
                        Vec foot=point(p,normal,tangent+t,0);
                        var floor=physics.support(foot.x(),foot.z(),foot.y(),.08);
                        if(floor==null||Math.abs(floor.y()-foot.y())>.045)return false;
                        if(!physics.clearSight(new Vec(foot.x(),foot.y()+.08,foot.z()),
                            new Vec(foot.x(),foot.y()+s.clearHeight()-.07,foot.z())))return false;
                    }
                }
            }
        }
        return true;
    }
    private static void require(boolean pass, String label) {
        if (!pass) throw new AssertionError(label);
    }

    private static boolean crosses(HouseMap map, Vec a, Vec b, double height) {
        Vec at=a;
        for (int i=1;i<=20;i++) {
            at=map.resolveMove(at,a.x()+(b.x()-a.x())*i/20,a.z()+(b.z()-a.z())*i/20,height);
            if (at==null) return false;
        }
        return Math.abs(at.y()-b.y())<.05;
    }

    private static void audit(HouseMap map, NativeInvestigationDoor.Spec spec, boolean open) {
        var p=spec.portal();
        for (double lane:new double[]{-.18,0,.18}) {
            var a=point(p,-.68,lane,0);
            var b=point(p,.68,lane,0);
            if(lane==0) {
                require(Set.of(spec.roomA(),spec.roomB()).equals(Set.of(
                    map.nativeInvestigation().spaceAt(a.x(),a.y()+1,a.z()).id(),map.nativeInvestigation().spaceAt(b.x(),b.y()+1,b.z()).id())),
                    "Architectural owner "+spec.frameId());
                int loss=map.architecturalSoundLoss(a.x(),a.y(),a.z(),b.x(),b.y(),b.z());
                require(loss==(open?1:3),"Native acoustic leak "+spec.frameId()+" "+loss);
            }
            for (double h:new double[]{HunterPosture.STANDING_HEIGHT,HunterPosture.CROUCH_HEIGHT}) {
                require(map.traversable(a.x(),a.y(),a.z(),h)&&map.traversable(b.x(),b.y(),b.z(),h),
                    "Obstructed receiving floor "+spec.frameId());
                require(crosses(map,a,b,h)==open&&crosses(map,b,a,h)==open,
                    "Physical crossing "+spec.frameId()+" lane "+lane+" open "+open);
            }
            for (double h:new double[]{.35,1.2,1.8}) {
                var from=point(p,-.68,lane,h);
                var to=point(p,.68,lane,h);
                require(map.clearLineOfSight(from.x(),from.y(),from.z(),to.x(),to.y(),to.z())==open,"Sight "+spec.frameId());
                require(map.clearObjectPath(from.x(),from.y(),from.z(),to.x(),to.y(),to.z(),.035,.035)==open,
                    "Object path "+spec.frameId());
            }
        }
        if (!open) {
            var eye=point(p,-.84,0,0);
            double yaw=p.alongX()?0:Math.PI/2;
            var actor=new LooseClutter.Observer(0,eye.x(),eye.y(),eye.z(),yaw,-.11,true);
            var target=InteractionFocus.pick(map,actor,List.of());
            require(target!=null&&target.kind()==InteractionFocus.Kind.DOOR&&target.id().equals(Integer.toString(p.id())),
                "Closed handle aim "+spec.frameId());
        }
    }

    private static Vec walk(HouseMap map,List<Vec> route,double height,String label) {
        Vec at=route.get(0);
        require(map.traversable(at.x(),at.y(),at.z(),height),"Unsupported start "+label+" "+at);
        for(int j=1;j<route.size();j++){
            var start=at;
            var end=route.get(j);
            int steps=Math.max(1,(int)Math.ceil(Math.hypot(end.x()-start.x(),end.z()-start.z())/.13));
            for(int k=1;k<=steps;k++){
                double t=(double)k/steps;
                var next=map.resolveMove(at,start.x()+(end.x()-start.x())*t,start.z()+(end.z()-start.z())*t,height);
                require(next!=null,"Movement lost at "+label+" near "+at+" to "+end);
                require(map.roomAt(next.x(),next.y(),next.z())>=0,"Route loses room ownership "+label+" at "+next);
                at=next;
            }
            require(Math.abs(at.y()-end.y())<.05,"Wrong receiving height "+label+" "+at+" versus "+end);
        }
        return at;
    }

    private static void replay(HouseMap map,List<String[]> rows,List<String> report) {
        var van=map.van();
        var cargo=van.world(0,VanStaging.FRONT+.45);
        var arrival=new ArrayList<Vec>();
        arrival.add(new Vec(cargo.x(),van.floorY(),cargo.z()));
        for(var row:rows)if(row[0].equals("POINT"))arrival.add(new Vec(d(row,1),d(row,2),d(row,3)));
        int count=0;
        for(double posture:new double[]{HunterPosture.STANDING_HEIGHT,HunterPosture.CROUCH_HEIGHT}){
            walk(map,arrival,posture,"van to foyer");
            var outbound=new ArrayList<>(arrival);
            Collections.reverse(outbound);
            walk(map,outbound,posture,"foyer to van");
            var reached=new HashMap<String,Vec>();
            reached.put("stage",arrival.get(1));
            for(var row:rows)if(row[0].equals("TRANSIT")){
                var source=reached.get(row[1]);
                require(source!=null,"Unconnected arrival guide "+row[1]);
                var route=new ArrayList<Vec>();
                route.add(source);
                for(int i=6;i<row.length;i+=3)route.add(new Vec(d(row,i),d(row,i+1),d(row,i+2)));
                String label=row[1]+" to "+row[2]+" posture "+posture;
                var next=walk(map,route,posture,label);
                var owner=map.nativeInvestigation().spaceAt(next.x(),next.y(),next.z());
                require(owner!=null&&owner.id().equals(row[2])&&owner.accessible(),"Wrong receiving room "+label);
                reached.put(row[2],next);
                var reversed=new ArrayList<>(route);
                Collections.reverse(reversed);
                reversed.set(0,next);
                walk(map,reversed,posture,"return: "+label);
                count+=2;
            }
            require(reached.size()==40,"Wrong reachable search count "+reached.size());
        }
        report.add("contiguous_cargo_to_foyer_both_ways_and_postures\tPASS");
        report.add("space_link_direction_replays\t"+count);
    }

    private static void render(HouseMap map, Portal p, Path target) throws Exception {
        var renderer=new SceneRenderer();
        renderer.setRenderSize(880,580);
        renderer.setPsxPreset(PsxPipeline.Preset.OFF);
        renderer.lamp=true;
        renderer.setLampHardware(EquipmentTiers.Tier.III,false);
        Vec camera=point(p,-1.45,-.22,0);
        if(!map.traversable(camera.x(),camera.y(),camera.z())) camera=point(p,-.82,0,0);
        require(map.traversable(camera.x(),camera.y(),camera.z()),"Unsupported review camera "+p.id());
        renderer.x=camera.x();
        renderer.z=camera.z();
        renderer.eyeHeight=camera.y()+HunterPosture.STANDING_EYE;
        renderer.yaw=Math.atan2(p.x()-camera.x(),p.z()-camera.z());
        renderer.pitch=-.10;
        renderer.drawNetwork(map,List.of(),-1,0);
        var gpu=GpuWorldRenderer.active();
        if(gpu!=null){
            require(renderer.awaitSubmittedGpuPrewarm(30_000_000_000L,()->false),"No School GPU frame");
            renderer.pinCompletedGpuFrame(gpu.latest());
            renderer.drawNetwork(map,List.of(),-1,0);
            require(renderer.frameDiagnostics().gpuReadbackAvailable(),"Missing School GPU readback");
        }
        require(javax.imageio.ImageIO.write(renderer.image(),"png",target.toFile()),"PNG encoder unavailable");
    }

    public static void main(String[] args)throws Exception{
        if(!List.of(args).contains("--gpu")){review(args);return;}
        var launcher=Class.forName("org.beckenburg.gpu.GpuDesktop");
        var start=launcher.getDeclaredMethod("startGpu");
        var stop=launcher.getDeclaredMethod("stopReviewGpu");
        start.setAccessible(true);
        stop.setAccessible(true);
        start.invoke(null);
        try{review(args);}finally{stop.invoke(null);}
    }

    private static void exportTriangles(NativeInvestigationScene scene, Path file)throws Exception {
        try(var out=Files.newBufferedWriter(file)){
            for(var door:scene.doors())for(boolean open:new boolean[]{false,true}){
                for(var triangle:door.pose(open).model().triangles()){
                    out.write(door.spec().portal().id()+"\t"+(open?"open":"closed"));
                    for(var vertex:List.of(triangle.a(),triangle.b(),triangle.c()))
                        out.write("\t"+vertex.x()+"\t"+vertex.y()+"\t"+vertex.z());
                    out.newLine();
                }
            }
        }
    }

    private static List<Room> missionRooms(List<String[]> records, NativeInvestigationScene scene,
            Dock dock, List<Portal> portals) {
        var anchors=new Room[HouseMap.ROOM_COUNT];
        for(var row:records) {
            if(row[0].equals("TRANSITION")){
                var portal=new Portal(Integer.parseInt(row[1]),Integer.parseInt(row[3]),Integer.parseInt(row[4]),
                    d(row,5),d(row,6),d(row,7),d(row,8),row[9].equals("z"),row[10],1);
                var support=scene.physics().support(portal.x(),portal.z(),portal.y(),.03);
                require(support!=null,"No physical floor or tread under mission transition "+row[2]);
                portals.add(portal);
                continue;
            }
            require(row[0].equals("ANCHOR"),"Unknown School mission row "+row[0]);
            int id=Integer.parseInt(row[1]);
            var feet=new Vec(d(row,3),d(row,4),d(row,5));
            var room=scene.spaceAt(feet.x(),feet.y(),feet.z());
            require(anchors[id]==null&&room!=null&&room.id().equals(row[2])&&room.owner()==id&&room.accessible()
                &&scene.physics().traversable(feet,HunterPosture.STANDING_HEIGHT),"Bad School anchor "+row[2]);
            anchors[id]=new Room(id,row[2],room.x0(),room.z0(),room.x1(),room.z1(),room.floor(),room.height(),
                room.outside()?"ground":"stone",room.outside()?"outdoor":"masonry",feet);
        }
        double vanY=dock.y()+VanStaging.FLOOR_Y;
        var van=new VanStaging(new VanStaging.Placement(dock.x(),dock.y(),dock.z(),dock.yaw()));
        var lip=van.world(0,VanStaging.FRONT);
        portals.add(new Portal(424,13,14,lip.x(),vanY,lip.z(),VanStaging.RAMP_HALF_WIDTH*2,
            Math.abs(Math.sin(dock.yaw()))<.5,"trail",1));
        anchors[14]=new Room(14,"Expedition van",dock.x()-4,dock.z()-1.3,dock.x()+4,dock.z()+1.3,
            vanY,VanStaging.INTERIOR_HEIGHT,"van","metal",new Vec(dock.x(),vanY,dock.z()));
        return List.of(anchors);
    }

    private static final class Peer implements AutoCloseable {
        private final java.util.concurrent.BlockingQueue<String> input=new java.util.concurrent.LinkedBlockingQueue<>();
        final LanClient client;

        Peer(LanHost host,boolean ghost,String name)throws Exception{
            client=new LanClient("127.0.0.1",host.port(),ghost,name,input::offer);
        }

        String await(String prefix)throws Exception{
            long until=System.nanoTime()+5_000_000_000L;
            while(System.nanoTime()<until){
                String row=input.poll(100,java.util.concurrent.TimeUnit.MILLISECONDS);
                if(row!=null&&(row.startsWith(prefix)||row.startsWith("REJECT|"))){
                    require(row.startsWith(prefix),"Expected "+prefix+" but host sent "+row);
                    return row;
                }
            }
            throw new AssertionError("Missing live TCP frame "+prefix);
        }

        @Override public void close(){client.close();}
    }

    private static Vec body(String row){
        var fields=LanProtocol.parts(row);
        return new Vec(Double.parseDouble(fields.get(2))/1000,Double.parseDouble(fields.get(3))/1000,
            Double.parseDouble(fields.get(4))/1000);
    }

    private static Vec step(Peer peer,int seat,int x,int z)throws Exception{
        Thread.sleep(125);
        peer.client.step(x,z);
        peer.await("ACCEPT|STEP");
        return body(peer.await("BODY|"+seat+"|"));
    }

    private static List<Integer> missionRoute(HouseMap map,int end){
        var queue=new java.util.ArrayDeque<Integer>();
        var predecessor=new HashMap<Integer,Integer>();
        int start=HouseMap.EXTRACTION_ROOM;
        queue.add(start);
        predecessor.put(start,start);
        while(!queue.isEmpty()){
            int room=queue.remove();
            for(int neighbor:map.adjacentRooms(room))if(!predecessor.containsKey(neighbor)){
                predecessor.put(neighbor,room);
                queue.add(neighbor);
            }
        }
        require(predecessor.containsKey(end),"Assembly owner is disconnected");
        var route=new ArrayList<Integer>();
        for(int room=end;room!=start;room=predecessor.get(room))route.add(room);
        Collections.reverse(route);
        return route;
    }

    private static void stagedLan(HouseMap map)throws Exception{
        try(var host=new LanHost("127.0.0.1",0,GhostIdentity.Type.SPIRIT,CustomContract.standard(3),map);
            var scout=new Peer(host,false,"Scout");
            var walker=new Peer(host,false,"Hunter");
            var observer=new Peer(host,false,"Observer");
            var ghost=new Peer(host,true,"Spirit")){
            String site="SITE|SCHOOL|"+map.level().revision();
            for(var peer:List.of(scout,walker,observer,ghost)){
                require(peer.await("SITE|").equals(site),"Peer disagreed with the School revision");
                peer.client.ready();
            }
            scout.await("LOBBY|3|HUMAN|4|WAITING");
            scout.client.start();
            scout.await("INFO|mission started");
            scout.client.view3D();
            scout.await("ACCEPT|VIEW3D");
            Vec deck=body(scout.await("BODY|0|"));
            var feet=deck;
            for(int i=0;i<9;i++) feet=step(scout,0,-100,0);
            require(map.roomAt(deck.x(),deck.y(),deck.z())==14&&map.roomAt(feet.x(),feet.y(),feet.z())==13
                &&Math.abs(feet.y())<.005,"Real School arrival missed the forecourt "+feet);
            for(int room:missionRoute(map,10)){
                walker.client.move(room);
                require(walker.await("ACCEPT|MOVE|").equals("ACCEPT|MOVE|"+room),"Wrong room-level arrival");
            }
            walker.client.view3D();
            walker.await("ACCEPT|VIEW3D");
            feet=body(walker.await("BODY|1|"));
            require(feet.equals(map.spawn(10)),"Wrong supported assembly start "+feet);
            for(int i=0;i<10;i++) feet=step(walker,1,0,100);
            for(int i=0;i<18;i++) feet=step(walker,1,-100,0);
            Thread.sleep(125);
            walker.client.step(-100,0);
            require(walker.await("REJECT|").contains("blocked by world collision"),"Closed assembly leaf leaked");
            double yaw=-Math.PI/2;
            var eye=new LooseClutter.Observer(1,feet.x(),feet.y(),feet.z(),yaw,0,true);
            var focus=InteractionFocus.pick(map,eye,List.of());
            require(focus!=null&&focus.kind()==InteractionFocus.Kind.DOOR&&focus.id().equals("420"),"No assembly door aim at "+feet);
            walker.client.turn(270);
            walker.client.interact(focus,yaw,0);
            walker.await("ACCEPT|INTERACT|Open door");
            require(scout.await("DOORS|420").equals("DOORS|420"),"Peer missed assembly door state");
            for(int i=0;i<4;i++) feet=step(walker,1,-100,0);
            require("upper-lobby".equals(map.architecturalRoomAt(feet.x(),feet.y()+1,feet.z())),
                "Host did not cross the real upper assembly door "+feet);
            ghost.client.viewGhost3D();
            ghost.await("ACCEPT|GVIEW3D");
            require(ghost.await("SITE|").equals(site),"Ghost missed the exact native source");
            ghost.await("GPOSE|");
            System.out.println("PASS actual staged four-peer School TCP case, forecourt and fitted assembly doorway "+feet);
        }
    }

    private static void review(String[] args)throws Exception{
        var rows=Files.readAllLines(Path.of(args[0])).stream().map(line->line.split("\t",-1)).toList();
        var source=rows.get(0);
        int missionArg=List.of(args).indexOf("--mission");
        var mission=missionArg<0?List.<String[]>of():Files.readAllLines(Path.of(args[missionArg+1])).stream()
            .map(line->line.split("\t",-1)).toList();
        var rooms=new ArrayList<NativeInvestigationScene.Space>();
        var stairs=new HashSet<String>();
        for(var r:rows)switch(r[0]){
            case "SPACE" -> rooms.add(new NativeInvestigationScene.Space(r[1],Integer.parseInt(r[2]),
                d(r,3),d(r,4),d(r,5),d(r,6),d(r,7),d(r,8),Boolean.parseBoolean(r[9]),Boolean.parseBoolean(r[10])));
            case "TREAD" -> stairs.add(r[1]);
            default -> {}
        }
        var schedule=Files.readAllLines(Path.of(args[1])).stream().map(line->line.split("\t",-1)).toList();
        var specs=new ArrayList<NativeInvestigationDoor.Spec>();
        var sockets=new ArrayList<Socket>();
        for(var f:schedule){
            if(f[0].equals("RITUAL")){
                sockets.add(new Socket("school-ritual-"+f[1],"ritual",Integer.parseInt(f[2]),d(f,3),d(f,4),d(f,5),0,f[1]));
                continue;
            }
            var p=new Portal(Integer.parseInt(f[0]),Integer.parseInt(f[4]),Integer.parseInt(f[5]),
                d(f,6),d(f,7),d(f,8),d(f,9),f[10].equals("z"),"door",1);
            specs.add(new NativeInvestigationDoor.Spec(f[1],f[2],f[3],p,d(f,11),d(f,12),d(f,13),
                Integer.parseInt(f[14]),Integer.parseInt(f[15]),Integer.parseInt(f[16]),f[17]));
        }
        NativeInvestigationScene scene;
        try(var mesh=Files.newInputStream(Path.of(source[1]));var col=Files.newInputStream(Path.of(source[3]))){
            scene=NativeInvestigationScene.read(mesh,source[2],col,source[4],stairs,rooms,specs);
        }
        var dock=rows.get(1);
        var existing=LevelPlans.buildSource(HouseMap.Site.SCHOOL);
        var placement=new Dock(d(dock,1),d(dock,2),d(dock,3),d(dock,4));
        var portals=new ArrayList<>(specs.stream().map(NativeInvestigationDoor.Spec::portal).toList());
        var anchors=mission.isEmpty()?existing.rooms():missionRooms(mission,scene,placement,portals);
        var definition=new LevelDefinition("school",scene.revision(),"Ravenshade fitted thresholds",
            anchors,portals,List.of(),List.of(),List.of(),sockets,List.of(),placement,
            existing.ambience(),existing.water(),existing.sounds());
        var host=new HouseMap(HouseMap.Site.SCHOOL,definition,null,scene);
        var guest=new HouseMap(HouseMap.Site.SCHOOL,definition,null,scene);
        require(specs.size()==(mission.isEmpty()?17:20),"Missing a fitted School frame");
        if(!mission.isEmpty()){
            require(host.valid()&&guest.valid(),"The connected School does not have fifteen valid anchors");
            for(var room:anchors){
                Vec feet=room.spawn();
                require(host.roomAt(feet.x(),feet.y(),feet.z())==room.id(),"Wrong spawn zone "+room);
            }
            var outside=host.van().world(0,VanStaging.FRONT-VanStaging.RAMP_LENGTH-.6);
            var ground=new Vec(outside.x(),placement.y(),outside.z());
            require(host.roomAt(ground.x(),ground.y(),ground.z())==13,"The forecourt incorrectly inherits the van owner");
        }
        require(RitualSites.valid(host),"Existing School ritual installations lost their supported room ownership");
        var output=Path.of(args[3]);
        Files.createDirectories(output);
        var report=new ArrayList<String>();
        report.add("measure\tresult");
        report.add("native_scene_revision\t"+scene.revision());
        report.add("retained_ritual_anchors\t3 supported and correctly owned");
        for(var spec:specs){
            require(sweep(scene.physics(),spec),"Leaf sweeps an unsupported or solid area "+spec.frameId());
            audit(host,spec,false);
            int id=spec.portal().id();
            boolean capture=mission.isEmpty()?id==401||id==404||id==413||id==414||id==417:id>=418;
            if(capture)render(host,spec.portal(),output.resolve("school-frame-"+id+"-closed.png"));
            host.toggleDoor(id,System.nanoTime()-2_000_000_000L);
            guest.applyDoorSnapshot(host.doorSnapshot());
            audit(host,spec,true);
            audit(guest,spec,true);
            if(capture)render(host,spec.portal(),output.resolve("school-frame-"+id+"-open.png"));
            host.toggleDoor(id,System.nanoTime()-2_000_000_000L);
            guest.applyDoorSnapshot(host.doorSnapshot());
            audit(guest,spec,false);
            report.add(spec.frameId()+"\t"+id+" "+spec.roomA()+" / "+spec.roomB()+" PASS fitted swing, body, sight, objects, aim and acoustics");
            System.out.println(report.get(report.size()-1));
        }
        for(var spec:specs)host.toggleDoor(spec.portal().id(),System.nanoTime()-2_000_000_000L);
        for(var spec:specs)audit(host,spec,true);
        report.add("all_open_crossing\tPASS");
        replay(host,rows,report);
        if(!mission.isEmpty()){
            report.add("connected_mission_owners\t15 supported spawns and four measured transitions");
            report.add("cargo_owner_receipt\tVan interior 14, forecourt 13");
            var physics=scene.physics();
            int closure=0;
            for(double z:new double[]{24.15,25,27,29.5,30.5,31.9,34.1,35.7})
                for(double y:new double[]{3.82,4.9,5.8,6.3,7.6}){
                    require(!physics.clearSight(new Vec(29.55,y,z),new Vec(30.45,y,z)),
                        "Rebuilt assembly partition has an open seam at "+y+","+z);
                    closure++;
                }
            require(!physics.clearSight(new Vec(29.55,6.21,33),new Vec(30.45,6.21,33)),
                "Rebuilt assembly head does not close above the leaf");
            report.add("assembly_wall_and_head_closure\t"+(closure+1)+" solid rays");
        }
        report.add("spaces\t"+rooms.stream().filter(NativeInvestigationScene.Space::accessible).count());
        if(List.of(args).contains("--lan")){
            require(!mission.isEmpty(),"The staged TCP replay needs the surveyed mission graph");
            stagedLan(new HouseMap(HouseMap.Site.SCHOOL,definition,null,scene));
            report.add("staged_tcp_case\tFour peers, physical cargo departure and new assembly door crossing");
        }
        Files.write(Path.of(args[2]),report);
        if(args.length>4&&!args[4].startsWith("--")) exportTriangles(scene,Path.of(args[4]));
    }
}
