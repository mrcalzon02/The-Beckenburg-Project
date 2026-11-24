
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

    static void audit(HouseMap map, NativeInvestigationDoor.Spec spec, boolean open) {
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

    static Vec walk(HouseMap map,List<Vec> route,double height,String label) {
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
        render(map,p,target,-1);
    }

    static void render(HouseMap map, Portal p, Path target,int side) throws Exception {
        var renderer=new SceneRenderer();
        renderer.setRenderSize(880,580);
        renderer.setPsxPreset(PsxPipeline.Preset.OFF);
        renderer.lamp=true;
        renderer.setLampHardware(EquipmentTiers.Tier.III,false);
        Vec camera=point(p,side*1.45,-.22,0);
        if(!map.traversable(camera.x(),camera.y(),camera.z())) camera=point(p,side*.82,0,0);
        require(map.traversable(camera.x(),camera.y(),camera.z()),"Unsupported review camera "+p.id());
        renderer.x=camera.x();
        renderer.z=camera.z();
        renderer.eyeHeight=camera.y()+HunterPosture.STANDING_EYE;
        renderer.yaw=Math.atan2(p.x()-camera.x(),p.z()-camera.z());
        renderer.pitch=-.10;
        drawFrame(renderer,map,target);
    }

    static void drawFrame(SceneRenderer renderer,HouseMap map,Path target)throws Exception{
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

    static void exportTriangles(NativeInvestigationScene scene, Path file)throws Exception {
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

    static final class Peer implements AutoCloseable {
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

    static Vec body(String row){
        var fields=LanProtocol.parts(row);
        return new Vec(Double.parseDouble(fields.get(2))/1000,Double.parseDouble(fields.get(3))/1000,
            Double.parseDouble(fields.get(4))/1000);
    }

    static Vec step(Peer peer,int seat,int x,int z)throws Exception{
        Thread.sleep(125);
        peer.client.step(x,z);
        peer.await("ACCEPT|STEP");
        return body(peer.await("BODY|"+seat+"|"));
    }

    static List<Integer> missionRoute(HouseMap map,int end){
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
        require(predecessor.containsKey(end),"Mission owner is disconnected");
        var route=new ArrayList<Integer>();
        for(int room=end;room!=start;room=predecessor.get(room))route.add(room);
        Collections.reverse(route);
        return route;
    }

    private static void replayLan(HouseMap map,boolean packaged)throws Exception{
        try(var host=packaged?new LanHost("127.0.0.1",0,GhostIdentity.Type.SPIRIT,CustomContract.standard(3),false,false,HouseMap.Site.SCHOOL)
                :new LanHost("127.0.0.1",0,GhostIdentity.Type.SPIRIT,CustomContract.standard(3),map);
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
            if(map.level().socket("breaker",11)!=null) stagedBreaker(host,scout,observer,ghost,map);
            System.out.println("PASS actual "+(packaged?"packaged":"staged")+" four-peer School TCP case, forecourt and fitted assembly doorway "+feet);
        }
    }

    private static void auditSolo(LevelDefinition expected)throws Exception{
        var directory=Files.createTempDirectory("beckenburg-school-solo-review");
        var profile=directory.resolve("career.properties");
        try(var local=new LocalInvestigation(new ProfileStore(profile),SiteCatalog.Location.SCHOOL,
                CharacterVisuals.Hunter.OBSERVER,MissionWeather.Mode.FIXED,EvidenceSignals.Weather.CLEAR,1,null,0)){
            var host=(LanHost)field(local,"host");
            var map=(HouseMap)field(host,"map");
            var player=((LanWorld)field(host,"world")).body(0);
            require(local.running()&&map.level().equals(expected)
                &&map.roomAt(player.x(),player.y(),player.z())==HouseMap.EXTRACTION_ROOM,
                "Normal solo selection missed the Ravenshade source or supported van spawn");
        }finally{
            Files.deleteIfExists(profile);
            Files.deleteIfExists(directory);
        }
    }

    static Object field(Object target,String name)throws Exception{
        var declared=target.getClass().getDeclaredField(name);
        declared.setAccessible(true);
        return declared.get(target);
    }

    private static void stagedBreaker(LanHost host,Peer scout,Peer observer,Peer ghost,HouseMap map)throws Exception{
        var mutex=field(host,"mutex");
        int room;
        synchronized(mutex){
            room=((GhostDomain)field(host,"ghost")).room();
            long seed=0;
            while(!new GhostHauntingFolio(seed).choose(GhostDomain.Action.DISTURB,GhostIdentity.Type.SPIRIT,room,1)
                    .entry().id().equals("lightning_static"))seed++;
            var folio=field(host,"hauntingFolio");
            ((java.util.Random)field(folio,"random")).setSeed(seed);
        }
        ghost.client.ghost(GhostDomain.Action.DISTURB,room);
        ghost.await("ACCEPT|GHOST|DISTURB");
        scout.await("LIGHTS|111111111111|0");
        require(!((RoomLighting)field(host,"lights")).supplied(),"Live School disturbance did not cut power");
        for(int zone:missionRoute(map,11)){
            observer.client.move(zone);
            require(observer.await("ACCEPT|MOVE|").equals("ACCEPT|MOVE|"+zone),"Wrong route to School plant");
        }
        observer.client.view3D();
        observer.await("ACCEPT|VIEW3D");
        require(body(observer.await("BODY|2|")).equals(map.spawn(11)),"Wrong supported plant spawn");
        Vec at=step(observer,2,-100,0);
        var panel=RoomLighting.breaker(map);
        double dx=panel.x()-at.x(),dz=panel.z()-at.z();
        double yaw=Math.atan2(dx,dz);
        double pitch=Math.atan2(panel.y()-at.y()-HunterPosture.STANDING_EYE,Math.hypot(dx,dz));
        var focus=InteractionFocus.pick(map,new LooseClutter.Observer(2,at.x(),at.y(),at.z(),yaw,pitch,true),List.of());
        require(focus!=null&&focus.kind()==InteractionFocus.Kind.BREAKER,"Plant reset does not target the actual board from "+at);
        observer.client.turn((int)Math.floorMod(Math.round(Math.toDegrees(yaw)),360));
        observer.client.interact(focus,yaw,pitch);
        observer.await("ACCEPT|INTERACT");
        scout.await("LIGHTS|111111111111|1");
        require(((RoomLighting)field(host,"lights")).supplied(),"Live School board reset did not restore power");
        System.out.println("PASS live School ghost trip and aimed breaker reset " +at);
    }

    private static double fixtureEnergy(FixtureLighting lights, Socket load){
        return lights.sampleNormal(load.room(),load.x(),load.y()-.55,load.z(),0,1,0).energy();
    }

    private static void auditHiding(HouseMap map,List<String> report){
        var hiding=HidingBudget.candidates(map.level());
        if(hiding.isEmpty())return;
        require(hiding.size()==HidingBudget.MAXIMUM,"Wrong physical School hiding budget");
        var solo=new InvestigationSession(map,InvestigationSession.Role.OBSERVER,101);
        for(var cabinet:hiding){
            var pocket=LevelClosets.world(cabinet,0,0,0);
            var entrance=LevelClosets.world(cabinet,0,0,1.10);
            var behind=LevelClosets.world(cabinet,0,1.25,-1.05);
            for(int density:new int[]{0,100}){
                map.configureHiding(density);
                for(double posture:new double[]{HunterPosture.STANDING_HEIGHT,HunterPosture.CROUCH_HEIGHT}){
                    walk(map,List.of(entrance,pocket),posture,cabinet.id());
                    walk(map,List.of(pocket,entrance),posture,cabinet.id()+" return");
                }
                require(map.roomAt(pocket.x(),pocket.y(),pocket.z())==cabinet.room(),"Wrong cupboard owner");
                require(!map.clearLineOfSight(pocket.x(),pocket.y()+1.25,pocket.z(),behind.x(),behind.y(),behind.z()),
                    "Cupboard back does not block sight "+cabinet.id());
                require(solo.toggleHide(pocket.x(),pocket.y(),pocket.z())==(density==100),"Wrong difficulty cover "+cabinet.id());
                solo.leaveHiding();
            }
        }
        map.configureHiding(70);
        report.add("founded_cupboards\tThree physical pockets and zero cover at maximum difficulty");
    }

    private static void auditPower(HouseMap map,Path output,List<String> report)throws Exception{
        var state=new RoomLighting();
        var lighting=new FixtureLighting(map);
        var loads=map.level().sockets().stream().filter(s->s.type().equals("light")).toList();
        require(loads.size()==26,"Missing circuit fittings");
        require(map.fixtures().states().stream().filter(f->f.kind()==HauntedFixtures.Kind.FIRE_ALARM).count()==12,
            "Missing physically authored fire alarms");
        lighting.frame(state,0);
        var supplied=loads.stream().mapToDouble(s->fixtureEnergy(lighting,s)).toArray();
        for(int room=0;room<12;room++){
            var socket=map.level().socket("switch",room);
            require(socket!=null,"Missing mounted room switch "+room);
            require(aimControl(map,socket,InteractionFocus.Kind.SWITCH)!=null,"Cannot aim at the installed switch "+socket);
            state.toggle(room);
            lighting.frame(state,room*2+1);
            int affected=0;
            for(int i=0;i<loads.size();i++){
                double difference=supplied[i]-fixtureEnergy(lighting,loads.get(i));
                if(loads.get(i).room()==room){
                    require(difference>.018,"Own circuit did not affect "+loads.get(i).id()+" loss "+difference);
                    affected++;
                }else require(Math.abs(difference)<.01,"Switch leaked into the next circuit "+loads.get(i).id());
            }
            require(affected>0,"Circuit without a fitted load "+room);
            state.toggle(room);
        }
        var panel=RoomLighting.breaker(map);
        require(aimControl(map,panel,InteractionFocus.Kind.BREAKER)!=null,"Cannot aim at the plant panel");
        var lit=serviceFrame(map,state,output.resolve("school-plant-powered.png"));
        state.setBreaker(false);
        lighting.frame(state,26);
        for(int i=0;i<loads.size();i++)
            require(supplied[i]-fixtureEnergy(lighting,loads.get(i))>.018,"Blackout missed "+loads.get(i).id());
        var dark=serviceFrame(map,state,output.resolve("school-plant-blackout.png"));
        int pixels=0;
        for(int i=0;i<lit.length;i++)if(lit[i]!=dark[i])pixels++;
        require(pixels>1000,"No visible School blackout");
        report.add("plant_breaker_and_independent_room_switches\t12 controls and 26 fitted loads");
        report.add("global_breaker_blackout\t26 extinguished loads and "+pixels+" altered pixels");
    }

    private static LooseClutter.Observer aimControl(HouseMap map,Socket socket,InteractionFocus.Kind kind){
        var room=map.level().room(socket.room());
        String target=kind==InteractionFocus.Kind.BREAKER?socket.id():Integer.toString(socket.room());
        for(double radius:new double[]{.72,.94,1.16,1.38,1.6})
            for(int i=0;i<16;i++){
                double theta=i*Math.PI/8;
                double x=socket.x()+radius*Math.sin(theta),z=socket.z()+radius*Math.cos(theta);
                double y=room.y();
                if(!map.traversable(x,y,z)||map.roomAt(x,y,z)!=room.id())continue;
                double yaw=Math.atan2(socket.x()-x,socket.z()-z);
                double pitch=Math.atan2(socket.y()-y-HunterPosture.STANDING_EYE,radius);
                var eye=new LooseClutter.Observer(0,x,y,z,yaw,pitch,true);
                var focus=InteractionFocus.pick(map,eye,List.of());
                if(focus!=null&&focus.kind()==kind&&focus.id().equals(target))return eye;
            }
        return null;
    }

    private static int[] serviceFrame(HouseMap map,RoomLighting power,Path path)throws Exception{
        var renderer=new SceneRenderer();
        renderer.setRenderSize(880,580);
        renderer.setPsxPreset(PsxPipeline.Preset.OFF);
        renderer.roomLights=power;
        renderer.lamp=false;
        Vec camera=new Vec(25.64,-3,16.43);
        require(map.traversable(camera.x(),camera.y(),camera.z()),"Unsupported plant view");
        renderer.x=camera.x();
        renderer.z=camera.z();
        renderer.eyeHeight=camera.y()+HunterPosture.STANDING_EYE;
        renderer.yaw=-Math.PI/2;
        renderer.pitch=-.10;
        drawFrame(renderer,map,path);
        return renderer.image().getRGB(0,0,880,580,null,0,880);
    }

    private static void review(String[] args)throws Exception{
        var rows=Files.readAllLines(Path.of(args[0])).stream().map(line->line.split("\t",-1)).toList();
        int missionArg=List.of(args).indexOf("--mission");
        var mission=missionArg<0?List.<String[]>of():Files.readAllLines(Path.of(args[missionArg+1])).stream()
            .map(line->line.split("\t",-1)).toList();
        var schedule=Files.readAllLines(Path.of(args[1])).stream().map(line->line.split("\t",-1)).toList();
        var packet=SchoolNativeLevel.read(rows,schedule,mission);
        var scene=packet.scene();
        var definition=packet.definition();
        var specs=packet.doors();
        var anchors=definition.rooms();
        var placement=definition.dock();
        boolean packaged=List.of(args).contains("--packaged");
        if(packaged){
            require(!new HouseMap(HouseMap.Site.SCHOOL).level().equals(definition),"School review must be opt-in");
            System.setProperty(SchoolNativeLevel.REVIEW_PROPERTY,"true");
        }
        var host=packaged?new HouseMap(HouseMap.Site.SCHOOL):new HouseMap(HouseMap.Site.SCHOOL,definition,null,scene);
        var guest=packaged?HouseMap.forNetworkRevision(HouseMap.Site.SCHOOL,definition.revision())
            :new HouseMap(HouseMap.Site.SCHOOL,definition,null,scene);
        if(packaged){
            require(host.level().equals(definition)&&guest.level().equals(definition),
                "Packaged School differs from the surveyed source");
            auditSolo(definition);
        }
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
        if(packaged)report.add("packaged_solo_case\tNormal School selection and supported cargo spawn");
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
        report.add("spaces\t"+rows.stream().filter(row->row[0].equals("SPACE")&&Boolean.parseBoolean(row[10])).count());
        auditHiding(host,report);
        if(host.level().socket("breaker",11)!=null) auditPower(host,output,report);
        if(List.of(args).contains("--lan")){
            require(!mission.isEmpty(),"The staged TCP replay needs the surveyed mission graph");
            replayLan(packaged?new HouseMap(HouseMap.Site.SCHOOL):new HouseMap(HouseMap.Site.SCHOOL,definition,null,scene),packaged);
            report.add((packaged?"packaged":"staged")+"_tcp_case\tFour peers, physical cargo departure and new assembly door crossing");
            if(host.level().socket("breaker",11)!=null)report.add((packaged?"packaged":"staged")+"_tcp_power\tHost ghost trip and physically aimed breaker reset");
        }
        Files.write(Path.of(args[2]),report);
        if(args.length>4&&!args[4].startsWith("--")) exportTriangles(scene,Path.of(args[4]));
    }
}
