package org.beckenburg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import javax.imageio.ImageIO;

import org.beckenburg.LevelDefinition.Socket;
import org.beckenburg.LevelDefinition.Vec;
import org.beckenburg.NativeSchoolInteriorReview.Peer;

/** Commission the auxiliary circuits against the exact fitted scene and existing LAN protocol. */
public final class DamPowerReview {
    private static final Vec PLANT_AISLE=new Vec(45,-6,15);
    private static final Vec TURBINE_AISLE=new Vec(50,-6,35);
    private static final int PLANT=10;
    private static final int TURBINE=3;

    private DamPowerReview() {}

    public static void main(String[] args)throws Exception {
        var packet=Path.of(args[0]);
        var water=Path.of(args[1]);
        if(!List.of(args).contains("--gpu")) {
            review(packet,water,false);
            return;
        }
        var desktop=Class.forName("org.beckenburg.gpu.GpuDesktop");
        var start=desktop.getDeclaredMethod("startGpu");
        var stop=desktop.getDeclaredMethod("stopReviewGpu");
        start.setAccessible(true);
        stop.setAccessible(true);
        start.invoke(null);
        try{review(packet,water,true);}finally{stop.invoke(null);}
    }

    static DamDoorReview.Scene read(Path packet,Path water)throws Exception {
        var base=DamDoorReview.read(packet,water,"dam-with-power");
        var level=base.level();
        var sockets=new ArrayList<>(level.sockets());
        for(String row:Files.readAllLines(packet.resolve("electrical-schedule.tsv"))) {
            var p=row.split("\t");
            require(p.length==8,"Invalid mounted electrical socket "+row);
            sockets.add(new Socket(p[0],p[1],Integer.parseInt(p[2]),Double.parseDouble(p[3]),
                Double.parseDouble(p[4]),Double.parseDouble(p[5]),Double.parseDouble(p[6]),p[7]));
        }
        var bound=new LevelDefinition(level.id(),base.nativeScene().revision(),level.title(),level.rooms(),
            level.portals(),level.stairs(),level.voids(),level.props(),sockets,level.colliders(),
            level.dock(),level.ambience(),level.water(),level.sounds());
        return new DamDoorReview.Scene(bound,base.nativeScene(),base.doors(),base.paths());
    }

    private static void review(Path packet,Path water,boolean gpu)throws Exception {
        var data=read(packet,water);
        var map=data.map();
        require(map.valid()&&RitualSites.valid(map),"Power placement invalidated Dam spawns or rites");
        var captures=packet.resolve(gpu?"captures/opengl":"captures/software");
        Files.createDirectories(captures);
        var report=new ArrayList<>(List.of("check\tresult",
            "native_scene_revision\t"+data.nativeScene().revision(),
            "collision_sha256\t"+data.nativeScene().physics().sha256(),
            "level_revision\t"+data.level().revision(),"water_revision\t"+data.level().water().revision(),
            "renderer\t"+(gpu?GpuWorldRenderer.active().rendererDescription():"Software")));
        for(var frame:data.doors()) {
            require(NativeSchoolInteriorReview.sweep(data.nativeScene().physics(),frame),"Utility blocked "+frame.frameId());
            NativeSchoolInteriorReview.audit(map,frame,false);
            map.toggleDoor(frame.portal().id(),System.nanoTime()-2_000_000_000L);
            NativeSchoolInteriorReview.audit(map,frame,true);
        }
        require(data.doors().size()==14,"Dam door set changed");
        report.add("retained_fitted_doors\t14");
        var refugeAudit=Path.of("build","dam-power-refuge-audit",gpu?"opengl":"software");
        Files.createDirectories(refugeAudit);
        NativeSchoolInteriorReview.auditWalkInCover(map,refugeAudit,report,"dam",java.util.Map.of(
            1,List.of(new Vec(12.3,0,30.8)),4,List.of(new Vec(49.8,-6,57.4)),10,List.of(new Vec(40.35,-6,12.25))));
        auditControls(data,map,report);
        auditCircuits(map,captures,report);
        if(!gpu&&!Boolean.getBoolean("beckenburg.dam.power.skipLive"))live(data,report);
        Files.write(packet.resolve(gpu?"power-opengl.tsv":"power-software.tsv"),report);
        report.forEach(System.out::println);
    }

    private static Vec operator(Socket s){return new Vec(s.x(),-6,s.z()+1.18);}

    private static void auditControls(DamDoorReview.Scene data,HouseMap map,List<String> report) {
        var van=map.van();
        var center=van.world(0,0);
        var toe=van.world(0,VanStaging.FRONT);
        var path=new ArrayList<>(List.of(new Vec(center.x(),van.floorY(),center.z()),
            new Vec(toe.x(),van.floorY(),toe.z()),new Vec(36,0,75.45)));
        path.addAll(data.paths().get("L02"));
        var west=new ArrayList<>(data.paths().get("L06"));
        Collections.reverse(west);
        path.addAll(west.subList(1,west.size()));
        path.addAll(data.paths().get("L07").subList(1,data.paths().get("L07").size()));
        int routes=0;
        for(var s:List.of(map.level().socket("generator",PLANT),RoomLighting.breaker(map),
                          map.level().socket("switch",PLANT),map.level().socket("switch",TURBINE))) {
            var route=new ArrayList<>(path);
            if(s.room()==PLANT){
                route.addAll(data.paths().get("L13").subList(1,data.paths().get("L13").size()));
                route.add(new Vec(s.x(),-6,12));
            }else route.add(new Vec(s.x(),-6,25));
            var feet=operator(s);
            route.add(feet);
            var back=new ArrayList<>(route);
            Collections.reverse(back);
            require(map.roomAt(feet.x(),feet.y(),feet.z())==s.room(),"Control approach missed its room: "+s);
            var aim=WharfPowerChecks.focus(map,feet,s);
            require(aim.id().equals(s.type().equals("switch")?Integer.toString(s.room()):s.id()),
                "Control aim missed "+s.id()+" / "+aim);
            for(double height:new double[]{HunterPosture.STANDING_HEIGHT,HunterPosture.CROUCH_HEIGHT}){
                NativeSchoolInteriorReview.walk(map,route,height,s.id()+" from van");
                NativeSchoolInteriorReview.walk(map,back,height,s.id()+" to van");
                routes+=2;
            }
        }
        report.add("van_to_controls_and_back_both_postures\t"+routes);
    }

    private static void auditCircuits(HouseMap map,Path captures,List<String> report)throws Exception {
        var lamps=map.level().sockets().stream().filter(s->s.type().equals("light")).toList();
        require(lamps.size()==4,"Unexpected auxiliary load count");
        var lighting=new RoomLighting();
        var fixtures=new FixtureLighting(map);
        for(var lamp:lamps) {
            double on=energy(fixtures,lighting,lamp);
            lighting.setSourceRunning(false);
            double stopped=energy(fixtures,lighting,lamp);
            lighting.setSourceRunning(true);
            lighting.setBreaker(false);
            double tripped=energy(fixtures,lighting,lamp);
            lighting.setBreaker(true);
            lighting.toggle(lamp.room());
            double switched=energy(fixtures,lighting,lamp);
            int other=lamp.room()==PLANT?TURBINE:PLANT;
            require(lighting.on(other),"Room switch altered circuit "+other);
            lighting.toggle(lamp.room());
            require(on>stopped*1.55&&Math.abs(stopped-tripped)<.001&&Math.abs(tripped-switched)<.001,
                lamp.id()+" load mismatch: "+on+" / "+stopped+" / "+tripped+" / "+switched);
            report.add(lamp.id()+"\t"+on+" supplied / "+stopped+" stopped / "+tripped+" tripped / "+switched+" switch open");
        }
        renderPair(map,lighting,captures,report,"switchgear-supply",new Vec(45,-6,15.75),new Vec(45,-4.88,8.5),false,PLANT);
        renderPair(map,lighting,captures,report,"turbine-lighting",new Vec(55,-6,28.8),new Vec(55,-3.8,22.1),true,TURBINE);
        lighting.setBreaker(false);
        render(map,lighting,captures.resolve("generator-inspection.png"),new Vec(42.2,-6,12.4),new Vec(41,-5.4,9.15),true);
        render(map,lighting,captures.resolve("distribution-inspection.png"),new Vec(50,-6,12.2),new Vec(48.6,-4.55,8.33),true);
        lighting.setBreaker(true);
    }

    private static double energy(FixtureLighting fixtures,RoomLighting lighting,Socket lamp) {
        fixtures.frame(lighting,0);
        var p=lamp.room()==PLANT?new Vec(lamp.x(),lamp.y()-.65,lamp.z()):new Vec(lamp.x(),lamp.y()-.33,lamp.z()+.75);
        return fixtures.sample(lamp.room(),p.x(),p.y(),p.z()).energy();
    }

    private static void renderPair(HouseMap map,RoomLighting lighting,Path captures,List<String> report,
                                   String name,Vec feet,Vec to,boolean switchOnly,int room)throws Exception {
        var on=captures.resolve(name+"-on.png");
        var off=captures.resolve(name+"-off.png");
        render(map,lighting,on,feet,to,false);
        if(switchOnly)lighting.toggle(room);else lighting.setBreaker(false);
        render(map,lighting,off,feet,to,false);
        if(switchOnly)lighting.toggle(room);else lighting.setBreaker(true);
        var a=ImageIO.read(on.toFile());
        var b=ImageIO.read(off.toFile());
        int changed=0;
        long brighter=0;
        for(int y=0;y<a.getHeight();y++)for(int x=0;x<a.getWidth();x++) {
            int aa=a.getRGB(x,y),bb=b.getRGB(x,y);
            if(aa!=bb)changed++;
            for(int c=0;c<24;c+=8)brighter+=((aa>>c)&255)-((bb>>c)&255);
        }
        double gain=brighter/(3.0*a.getWidth()*a.getHeight());
        require(changed>15_000&&gain>.22,"No visible electrical contrast: "+name+" / "+changed+" / "+gain);
        report.add(name+"_same_camera_response\t"+changed+" changed pixels / "+gain+" mean RGB");
    }

    private static void render(HouseMap map,RoomLighting lighting,Path image,Vec feet,Vec to,boolean torch)throws Exception {
        var renderer=new SceneRenderer();
        renderer.setRenderSize(880,580);
        renderer.setPsxPreset(PsxPipeline.Preset.OFF);
        renderer.roomLights=lighting;
        renderer.lamp=torch;
        if(torch)renderer.setLampHardware(EquipmentTiers.Tier.values()[2],false);
        renderer.x=feet.x();
        renderer.z=feet.z();
        renderer.eyeHeight=feet.y()+HunterPosture.STANDING_EYE;
        renderer.yaw=Math.atan2(to.x()-feet.x(),to.z()-feet.z());
        renderer.pitch=Math.atan2(to.y()-renderer.eyeHeight,Math.hypot(to.x()-feet.x(),to.z()-feet.z()));
        NativeSchoolInteriorReview.drawFrame(renderer,map,image);
    }

    private static void live(DamDoorReview.Scene data,List<String> report)throws Exception {
        var map=data.map();
        var replica=data.map();
        var generator=map.level().socket("generator",PLANT);
        var breaker=RoomLighting.breaker(map);
        var plantSwitch=map.level().socket("switch",PLANT);
        var turbineSwitch=map.level().socket("switch",TURBINE);
        try(var host=new LanHost("127.0.0.1",0,GhostIdentity.Type.SPIRIT,CustomContract.standard(3),map);
            var engineer=new Peer(host,false,"Dam electrician");
            var witness=new Peer(host,false,"Dam control observer");
            var crew=new Peer(host,false,"Dam returning crew");
            var ghost=new Peer(host,true,"Dam spectral witness")) {
            for(var peer:List.of(engineer,witness,crew,ghost)){
                peer.awaitExact("SITE|USER|"+data.level().revision());
                peer.client.ready();
            }
            engineer.awaitExact("LOBBY|3|HUMAN|4|WAITING");
            engineer.client.start();
            engineer.await("INFO|mission started");
            engineer.client.view3D();
            engineer.awaitExact("ACCEPT|VIEW3D");
            var at=NativeSchoolInteriorReview.body(engineer.await("BODY|0|"));
            require(map.roomAt(at.x(),at.y(),at.z())==14,"Live launch missed the van");
            ghost.client.viewGhost3D();
            ghost.awaitExact("ACCEPT|GVIEW3D");
            ghost.await("GPOSE|");
            NativeSchoolInteriorReview.ghostTrip(host,witness,ghost);
            for(int i=0;i<15;i++)at=NativeSchoolInteriorReview.step(engineer,0,0,-100);
            at=NorthwatchDoorRuntime.walk(engineer,at,new Vec(36,0,69),new Vec(40,0,69),
                new Vec(40,0,52),new Vec(31,0,52));
            at=NorthwatchDoorRuntime.open(map,replica,data.nativeScene(),engineer,witness,at,873);
            var stair=data.paths().get("L07");
            for(var p:stair.subList(0,stair.size()-1))at=CemeteryRefugeReview.walk(engineer,0,at,p);
            at=NorthwatchDoorRuntime.open(map,replica,data.nativeScene(),engineer,witness,at,874);
            at=NorthwatchDoorRuntime.walk(engineer,at,TURBINE_AISLE,new Vec(45,-6,23.2));
            at=NorthwatchDoorRuntime.open(map,replica,data.nativeScene(),engineer,witness,at,880);
            at=NorthwatchDoorRuntime.walk(engineer,at,PLANT_AISLE,new Vec(generator.x(),-6,12),operator(generator));

            var lights=(RoomLighting)NativeSchoolInteriorReview.field(host,"lights");
            WharfPowerChecks.use(map,engineer,at,generator);
            witness.awaitExact("SOURCE|0");
            require(!lights.sourceRunning()&&!lights.breaker(),"Stopping the generator disturbed an open breaker");
            Thread.sleep(400);
            WharfPowerChecks.use(map,engineer,at,generator);
            witness.awaitExact("SOURCE|1");
            require(lights.sourceRunning()&&!lights.supplied()&&!lights.breaker(),"Restart reset a ghost trip");
            var ticket=crew.client.resumeTicket();
            require(ticket!=null,"Missing Dam reconnect ticket");
            ((java.net.Socket)NativeSchoolInteriorReview.field(crew.client,"socket")).close();
            witness.await("PEER|LOST|2");
            var frames=new LinkedBlockingQueue<String>();
            try(var resumed=new LanClient("127.0.0.1",host.port(),ticket,frames::offer)){
                WharfPowerChecks.await(frames,"INFO|mission resumed");
                resumed.view3D();
                WharfPowerChecks.await(frames,"ACCEPT|VIEW3D");
                WharfPowerChecks.await(frames,"SITE|USER|"+data.level().revision());
                WharfPowerChecks.await(frames,"LIGHTS|111111111111|0");
                WharfPowerChecks.await(frames,"SOURCE|1");
            }
            at=NorthwatchDoorRuntime.walk(engineer,at,new Vec(generator.x(),-6,12),
                new Vec(breaker.x(),-6,12),operator(breaker));
            WharfPowerChecks.use(map,engineer,at,breaker);
            witness.awaitExact("LIGHTS|111111111111|1");
            require(lights.supplied(),"Aimed breaker reset did not restore the two circuits");
            at=NorthwatchDoorRuntime.walk(engineer,at,operator(plantSwitch));
            operateSwitch(map,engineer,at,plantSwitch);
            witness.awaitExact("LIGHTS|111111111101|1");
            require(!lights.on(PLANT)&&lights.on(TURBINE),"Switchgear switch changed the turbine circuit");
            at=NorthwatchDoorRuntime.walk(engineer,at,PLANT_AISLE,new Vec(45,-6,20.8),new Vec(45,-6,23.35),
                                         new Vec(52,-6,24.3),operator(turbineSwitch));
            operateSwitch(map,engineer,at,turbineSwitch);
            witness.awaitExact("LIGHTS|111011111101|1");
            require(!lights.on(PLANT)&&!lights.on(TURBINE)&&lights.supplied(),"Turbine switch changed the main supply");
        }
        report.add("live_four_peer_auxiliary_power\tPASS van, three physical leaves in blackout, ghost trip, source stop/restart, reconnect, breaker and both switches");
    }

    private static void operateSwitch(HouseMap map,Peer peer,Vec feet,Socket socket)throws Exception {
        var aim=WharfPowerChecks.focus(map,feet,socket);
        require(aim.kind()==InteractionFocus.Kind.SWITCH&&aim.id().equals(Integer.toString(socket.room())),"Aiming missed "+socket.id());
        double dx=socket.x()-feet.x(),dz=socket.z()-feet.z();
        double yaw=Math.atan2(dx,dz);
        double pitch=Math.atan2(socket.y()-feet.y()-HunterPosture.STANDING_EYE,Math.hypot(dx,dz));
        peer.client.turn(Math.floorMod((int)Math.round(Math.toDegrees(yaw)),360));
        peer.client.interact(aim,yaw,pitch);
        peer.awaitExact("ACCEPT|INTERACT|"+aim.name());
    }

    private static void require(boolean pass,String message){if(!pass)throw new AssertionError(message);}
}
