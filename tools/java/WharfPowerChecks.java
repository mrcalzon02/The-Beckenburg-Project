package org.beckenburg;

import java.nio.file.Path;
import java.util.List;

import org.beckenburg.LevelDefinition.Socket;
import org.beckenburg.LevelDefinition.Vec;
import org.beckenburg.NativeSchoolInteriorReview.Peer;

/** Tests the Wharf service equipment in the shared staged game. */
final class WharfPowerChecks {
    private WharfPowerChecks() {}

    static void audit(HouseMap map,Path captures,List<String> report)throws Exception {
        var generator=map.level().socket("generator",12);
        var breaker=RoomLighting.breaker(map);
        var light=map.level().socket("light",12);
        require(generator!=null&&breaker.room()==12&&light!=null,"Incomplete Wharf service bindings");
        for(var control:List.of(generator,breaker)){
            Vec target=operator(control);
            for(double h:new double[]{HunterPosture.STANDING_HEIGHT,HunterPosture.CROUCH_HEIGHT})
                NativeSchoolInteriorReview.walk(map,List.of(new Vec(37.5,.018,57.65),target),h,
                    control.id()+" operating stance");
            require(focus(map,target,control).id().equals(control.id()),"Aiming missed "+control.id());
        }
        var lights=new RoomLighting();
        var scene=new FixtureLighting(map);
        double[] level=new double[3];
        for(int state=0;state<3;state++){
            lights.setSourceRunning(state!=1);
            lights.setBreaker(state!=2);
            scene.frame(lights,0);
            level[state]=scene.sample(light.room(),light.x(),light.y()-.65,light.z()).energy();
        }
        require(Math.abs(level[1]-level[2])<.001&&level[0]>2*level[1],"Fitted lamp did not drop to the ambient baseline");
        report.add("supported_load_energy\t"+level[0]+" supplied, "+level[1]+" stopped, "+level[2]+" tripped");
        for(boolean on:new boolean[]{true,false}){
            lights.setBreaker(on);
            var renderer=new SceneRenderer();
            renderer.setRenderSize(880,580);
            renderer.setPsxPreset(PsxPipeline.Preset.OFF);
            renderer.roomLights=lights;
            renderer.x=37.5;
            renderer.z=59.2;
            renderer.eyeHeight=HunterPosture.STANDING_EYE;
            renderer.yaw=Math.PI;
            renderer.pitch=-.09;
            NativeSchoolInteriorReview.drawFrame(renderer,map,
                captures.resolve(on?"wharf-service-powered.png":"wharf-service-blackout.png"));
        }
    }

    static void live(LevelDefinition level,NativeInvestigationScene scene,List<String> report)throws Exception {
        var map=new HouseMap(HouseMap.Site.USER,level,null,scene);
        var generator=level.socket("generator",12);
        var breaker=RoomLighting.breaker(map);
        try(var host=new LanHost("127.0.0.1",0,GhostIdentity.Type.SPIRIT,CustomContract.standard(3),map);
            var player=new Peer(host,false,"Wharf Electrician");
            var witness=new Peer(host,false,"Wharf Witness");
            var teammate=new Peer(host,false,"Wharf Crew");
            var ghost=new Peer(host,true,"Wharf Ghost")) {
            for(var peer:List.of(player,witness,teammate,ghost)){
                peer.awaitExact("SITE|USER|"+level.revision());
                peer.client.ready();
            }
            player.awaitExact("LOBBY|3|HUMAN|4|WAITING");
            player.client.start();
            player.await("INFO|mission started");
            player.client.view3D();
            player.awaitExact("ACCEPT|VIEW3D");
            Vec at=NativeSchoolInteriorReview.body(player.await("BODY|0|"));
            require(map.roomAt(at.x(),at.y(),at.z())==14,"Live start missed the van");
            ghost.client.viewGhost3D();
            ghost.awaitExact("ACCEPT|GVIEW3D");
            ghost.awaitExact("SITE|USER|"+level.revision());
            ghost.await("GPOSE|");
            NativeSchoolInteriorReview.ghostTrip(host,witness,ghost);
            for(int i=0;i<15;i++)at=NativeSchoolInteriorReview.step(player,0,0,-100);
            at=walk(player,at,32,61);
            at=walk(player,at,37.5,61);
            at=walk(player,at,37.5,57.65);
            at=walk(player,at,generator.x(),generator.z()+1.18);
            var lights=(RoomLighting)NativeSchoolInteriorReview.field(host,"lights");
            use(map,player,at,generator);
            witness.awaitExact("SOURCE|0");
            require(!lights.supplied()&&!lights.breaker()&&!lights.sourceRunning(),"Stop did not preserve trip");
            Thread.sleep(400);
            use(map,player,at,generator);
            witness.awaitExact("SOURCE|1");
            require(!lights.supplied()&&!lights.breaker()&&lights.sourceRunning(),"Restart reset the breaker");
            var ticket=teammate.client.resumeTicket();
            require(ticket!=null,"Missing Wharf reconnect ticket");
            ((java.net.Socket)NativeSchoolInteriorReview.field(teammate.client,"socket")).close();
            witness.await("PEER|LOST|2");
            var frames=new java.util.concurrent.LinkedBlockingQueue<String>();
            try(var resumed=new LanClient("127.0.0.1",host.port(),ticket,frames::offer)){
                await(frames,"INFO|mission resumed");
                resumed.view3D();
                await(frames,"ACCEPT|VIEW3D");
                await(frames,"SITE|USER|"+level.revision());
                await(frames,"LIGHTS|111111111111|0");
                await(frames,"SOURCE|1");
            }
            at=walk(player,at,37.5,57.1);
            at=walk(player,at,breaker.x(),breaker.z()+1.18);
            use(map,player,at,breaker);
            witness.awaitExact("LIGHTS|111111111111|1");
            require(lights.supplied(),"Aimed breaker reset did not restore the supplied load");
        }
        report.add("four_peer_power\tPASS ghost trip, physical arrival and controls, source stop/restart, tripped reconnect, breaker reset");
    }

    private static void await(java.util.concurrent.BlockingQueue<String> frames,String prefix)throws Exception{
        long stop=System.nanoTime()+5_000_000_000L;
        while(System.nanoTime()<stop){
            var frame=frames.poll(100,java.util.concurrent.TimeUnit.MILLISECONDS);
            if(frame!=null&&frame.startsWith(prefix))return;
        }
        throw new AssertionError("Wharf reconnect did not receive "+prefix);
    }

    private static Vec operator(Socket s){return new Vec(s.x(),.018,s.z()+1.18);}

    private static InteractionFocus.Target focus(HouseMap map,Vec feet,Socket s){
        double dx=s.x()-feet.x(),dz=s.z()-feet.z(),yaw=Math.atan2(dx,dz);
        double pitch=Math.atan2(s.y()-feet.y()-HunterPosture.STANDING_EYE,Math.hypot(dx,dz));
        var observer=new LooseClutter.Observer(0,feet.x(),feet.y(),feet.z(),yaw,pitch,true);
        var result=InteractionFocus.pick(map,observer,List.of());
        require(result!=null,"No service-control focus from "+feet);
        return result;
    }

    private static void use(HouseMap map,Peer peer,Vec feet,Socket s)throws Exception{
        var target=focus(map,feet,s);
        require(target.id().equals(s.id()),"Wrong live service focus "+target+" for "+s.id());
        double dx=s.x()-feet.x(),dz=s.z()-feet.z(),yaw=Math.atan2(dx,dz);
        double pitch=Math.atan2(s.y()-feet.y()-HunterPosture.STANDING_EYE,Math.hypot(dx,dz));
        peer.client.turn(Math.floorMod((int)Math.round(Math.toDegrees(yaw)),360));
        peer.client.interact(target,yaw,pitch);
        peer.awaitExact("ACCEPT|INTERACT|"+target.name());
    }

    private static Vec walk(Peer peer,Vec at,double x,double z)throws Exception {
        for(int i=0;i<60;i++){
            double dx=x-at.x(),dz=z-at.z(),length=Math.hypot(dx,dz);
            if(length<.165)return at;
            double scale=Math.min(100/length,310);
            at=NativeSchoolInteriorReview.step(peer,0,(int)Math.round(dx*scale),(int)Math.round(dz*scale));
        }
        throw new AssertionError("Unreachable service approach from "+at+" to "+x+", "+z);
    }

    private static void require(boolean ok,String reason){if(!ok)throw new AssertionError(reason);}
}
