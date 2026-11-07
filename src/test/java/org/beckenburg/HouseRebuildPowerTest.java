package org.beckenburg;
import java.util.*;
import org.beckenburg.LevelDefinition.Vec;

/** Actual authored control mesh, circuit lighting and real TCP source/breaker authority. */
public final class HouseRebuildPowerTest {
    static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    static LooseClutter.Observer approach(HouseMap map,InteractionFocus.Kind kind){
        var socket=map.level().sockets().stream().filter(s->s.type().equals(kind==InteractionFocus.Kind.GENERATOR?"generator":"breaker")).findFirst().orElseThrow();
        double tx=socket.x(),ty=socket.y(),tz=socket.z();
        if(kind==InteractionFocus.Kind.GENERATOR){var b=SceneRenderer.castleBounds("button_12");tx+=b[0]*.30;ty+=(b[1]+b[4]/2)*.30;tz+=(b[2]+b[5]/2)*.30;}
        for(double radius:new double[]{.65,.85,1.05,1.3,1.6})for(int i=0;i<96;i++){
            double angle=i*Math.PI/48,x=tx+Math.sin(angle)*radius,z=tz+Math.cos(angle)*radius,y=map.groundHeight(x,z,socket.room());
            if(map.roomAt(x,y,z)!=socket.room()||!map.traversable(x,y,z))continue;
            double yaw=Math.atan2(tx-x,tz-z),pitch=Math.atan2(ty-y-HunterPosture.STANDING_EYE,Math.hypot(tx-x,tz-z));
            var observer=new LooseClutter.Observer(0,x,y,z,yaw,pitch,true);var target=InteractionFocus.pick(map,observer,List.of());
            if(target!=null&&target.kind()==kind&&target.id().equals(socket.id()))return observer;
        }throw new AssertionError("No supported visible control approach: "+kind);
    }
    static void interact(LanHost host,HouseMap map,LanWorld world,AuthoredLevelNetworkTest.Peer peer,LooseClutter.Observer observer)throws Exception{
        Object mutex=LanSafeAreaBarrierTest.field(host,"mutex");int ready;
        synchronized(mutex){ready=(int)LanSafeAreaBarrierTest.field(host,"tick")+4;}
        long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
        for(;;){synchronized(mutex){if((int)LanSafeAreaBarrierTest.field(host,"tick")>=ready){AuthoredTestSupport.place(map,world,0,new Vec(observer.x(),observer.y(),observer.z()));break;}}
            if(System.nanoTime()>deadline)throw new AssertionError("Host clock did not advance to interaction cooldown");Thread.sleep(10);}

        var target=InteractionFocus.pick(map,observer,List.of());check(target!=null,"Control disappeared");
        peer.client.interact(target,observer.yaw(),observer.pitch());
    }
    public static void main(String[] args)throws Exception{
        var map=args.length>0&&args[0].equals("FURNISHINGS")?HouseRebuildFurnishings.map():args.length>0&&args[0].equals("SERVICES")?HouseRebuildServices.map():args.length>0&&args[0].equals("FITOUT")?HouseRebuildFitout.map():HouseRebuildFunctions.map();check(map.hasGenerator()&&!map.nativeSite(),"Authored source capability cannot depend on native terrain");
        check(!new HouseMap().hasGenerator(),"Legacy grid House does not advertise an invented generator");
        var starter=approach(map,InteractionFocus.Kind.GENERATOR);var breaker=approach(map,InteractionFocus.Kind.BREAKER);
        var lights=new RoomLighting();var field=new FixtureLighting(map);var room=map.level().room(1);double x=room.spawn().x(),z=room.spawn().z();
        field.frame(lights,0);double on=field.sample(1,x,.9,z).energy();lights.setSourceRunning(false);field.frame(lights,1);double off=field.sample(1,x,.9,z).energy();
        check(on>off&&lights.breaker(),"Stopped source must darken actual connected lights and preserve breaker");
        lights.setBreaker(false);lights.setSourceRunning(true);check(!lights.supplied(),"Restart cannot repair breaker");
        lights.setBreaker(true);check(lights.supplied(),"Ordinary breaker restores connected supply");
        try(var host=new LanHost("127.0.0.1",0,GhostIdentity.Type.SPIRIT,CustomContract.standard(3),map);
            var a=new AuthoredLevelNetworkTest.Peer(host.port(),false,"Cleric");
            var b=new AuthoredLevelNetworkTest.Peer(host.port(),false,"Observer");
            var c=new AuthoredLevelNetworkTest.Peer(host.port(),false,"Scholar");
            var ghost=new AuthoredLevelNetworkTest.Peer(host.port(),true,"Spirit")){
            a.client.ready();b.client.ready();c.client.ready();ghost.client.ready();a.await("LOBBY|3|HUMAN|4|WAITING");a.client.start();a.await("INFO|mission started");
            for(int next:AuthoredLevelNetworkTest.path(map,14,13)){a.client.move(next);a.await("ACCEPT|MOVE|"+next);}
            a.client.view3D();a.await("ACCEPT|VIEW3D");a.await("SOURCE|1");
            var world=(LanWorld)LanSafeAreaBarrierTest.field(host,"world");var authority=(RoomLighting)LanSafeAreaBarrierTest.field(host,"lights");
            interact(host,map,world,a,starter);b.await("SOURCE|0");a.await("ACCEPT|INTERACT|");check(!authority.sourceRunning()&&authority.breaker(),"Physical host source stop");
            interact(host,map,world,a,breaker);a.await("ACCEPT|INTERACT|");check(!authority.breaker(),"Ordinary breaker trips independently");
            interact(host,map,world,a,starter);b.await("SOURCE|1");a.await("ACCEPT|INTERACT|");check(authority.sourceRunning()&&!authority.breaker()&&!authority.supplied(),"Network restart preserves tripped breaker");
            interact(host,map,world,a,breaker);a.await("ACCEPT|INTERACT|");check(authority.supplied(),"Physical network breaker reset");
            a.client.view3D();a.await("ACCEPT|VIEW3D");a.await("SOURCE|1");check(authority.supplied(),"Re-entering view preserves authoritative source");
            var target=InteractionFocus.pick(map,starter,List.of());ghost.client.interact(target,starter.yaw(),starter.pitch());ghost.await("REJECT|");check(authority.supplied(),"Ghost cannot submit hunter generator control");
            AuthoredTestSupport.place(map,world,0,map.spawn(13));Thread.sleep(250);a.client.interact(target,starter.yaw(),starter.pitch());a.await("REJECT|");check(authority.supplied(),"Distant forged generator interaction rejected");
            check(host.running(),"Electrical controls leave case running");
        }
        System.out.println("PASS actual supported source/panel aim; connected light energy "+on+" -> "+off+";4TCP peers source/breaker independence,view-resync,ghost/distance rejection. Ghost-specific trip-event audit remains open.");
    }
}
