package org.beckenburg;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.beckenburg.LevelDefinition.Vec;

/** Two field instruments illuminate one Wraith. Each TCP client sees only its own view. */
public final class LanProjectedLightWatchTest {
    private static final String DOTS="DOTS|ILLUMINATE|", LASER="LASER|HIT|";

    private static void check(boolean condition,String message){
        if(!condition)throw new AssertionError(message);
    }

    private static final class Player implements AutoCloseable {
        final LanClient client;
        final List<String> visual=new CopyOnWriteArrayList<>();
        final LinkedBlockingQueue<String> commands=new LinkedBlockingQueue<>();

        Player(LanHost host,String name,boolean ghost,boolean owner)throws Exception{
            client=new LanClient("127.0.0.1",host.port(),ghost,name,"",owner?host.localOwnerCredential():"",this::receive);
        }

        void receive(String frame){
            if(frame.startsWith(DOTS)||frame.startsWith(LASER)||frame.startsWith("SENSOR|READ|")
                    ||frame.startsWith("CAMGHOST|")||frame.startsWith("CAMERA|"))visual.add(frame);
            if(frame.startsWith("ACCEPT|")||frame.startsWith("REJECT|")||frame.startsWith("LOBBY|")
                    ||frame.startsWith("INFO|mission started"))commands.add(frame);
        }

        String await(String prefix)throws Exception{
            long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
            while(System.nanoTime()<end){
                var frame=commands.poll(50,TimeUnit.MILLISECONDS);
                if(frame==null)continue;
                check(!frame.startsWith("REJECT|"),client.seat()+" rejected "+prefix+" with "+frame);
                if(frame.startsWith(prefix))return frame;
            }
            throw new AssertionError(client.seat()+" missing "+prefix);
        }

        void equip(int slot,String tool)throws Exception{
            client.equipAt(slot,tool);
            await("ACCEPT|GEAREQUIP|"+slot+"|"+tool);
        }

        void use(String tool)throws Exception{
            client.useHeldEquipment();
            await("ACCEPT|GEARUSE|"+tool+"|");
        }

        void watch(int owner)throws Exception{
            client.watchCamera(owner);
            await(owner<0?"ACCEPT|WATCH|NONE":"ACCEPT|WATCH|CAMERA|"+owner);
        }

        // Acknowledgment on the same TCP stream drains all earlier observation packets.
        void sync()throws Exception{
            if(client.seat().equals("GHOST")){
                client.viewGhost3D();
                await("ACCEPT|GVIEW3D");
            }else{
                client.journal();
                await("ACCEPT|JOURNAL");
            }
        }

        List<String> since(int start,String prefix){
            return visual.stream().skip(start).filter(frame->frame.startsWith(prefix)).toList();
        }

        String observation(int start,String prefix)throws Exception{
            long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);
            while(System.nanoTime()<end){
                var matches=since(start,prefix);
                if(!matches.isEmpty())return matches.get(0);
                Thread.sleep(8);
            }
            throw new AssertionError(client.seat()+" missing "+prefix+" after "+start+" in "+visual);
        }

        void patterns(int start,boolean remote)throws Exception{
            var dots=LanProtocol.parts(observation(start,DOTS));
            var laser=LanProtocol.parts(observation(start,LASER));
            check(dots.size()==(remote?8:7)&&laser.size()==(remote?9:8),"Wrong optical source: "+dots+" "+laser);
            if(remote)check(dots.get(7).equals("0")&&laser.get(8).equals("0"),"Wrong camera owner");
        }

        void noPatterns(int start)throws Exception{
            sync();
            check(since(start,DOTS).isEmpty()&&since(start,LASER).isEmpty(),
                client.seat()+" received a projected-light contact outside its view");
        }

        @Override public void close(){client.close();}
    }

    private static void checkPanel(Player operator,String dots,String laser)throws Exception{
        var placement=operator.observation(0,"CAMERA|PLACE|0|");
        var power=operator.observation(0,"CAMERA|POWER|0|1");
        LanWorldGuiTest.onEdt(()->{
            var panel=new LanWorldPanel(operator.client);
            try{
                panel.onEvent(placement);
                panel.onEvent(power);
                panel.updateCameraAttention(0);
                panel.onEvent(operator.await("ACCEPT|WATCH|CAMERA|0"));
                panel.onEvent(dots);panel.onEvent(laser);
                var direct=(SceneRenderer)LooseClutterNetworkTest.field(panel,"renderer");
                var monitor=(SceneRenderer)LooseClutterNetworkTest.field(panel,"videoRenderer");
                check(monitor.dotsPassRoom==4&&!monitor.laserContacts.isEmpty(),"Monitor lost the selected feed's light patterns");
                check(direct.dotsPassRoom<0&&direct.laserContacts.isEmpty(),"Remote light patterns leaked to direct vision");
                panel.focusLost(new java.awt.event.FocusEvent(panel,java.awt.event.FocusEvent.FOCUS_LOST));
                panel.onEvent(dots);panel.onEvent(laser);
                check(monitor.dotsPassRoom<0&&monitor.laserContacts.isEmpty(),"Delayed patterns repopulated a closed feed");
                panel.updateCameraAttention(0);  // Reopen before either pending server reply reaches the UI.
                panel.onEvent(dots);panel.onEvent(laser);
                panel.onEvent(operator.await("ACCEPT|WATCH|NONE"));
                panel.onEvent(dots);panel.onEvent(laser);
                check(monitor.dotsPassRoom<0&&monitor.laserContacts.isEmpty(),"Previous watch replayed while renewing its feed");
                panel.onEvent(operator.await("ACCEPT|WATCH|CAMERA|0"));
            }finally{panel.close();}
            return null;
        });
        operator.await("ACCEPT|WATCH|NONE");
    }

    public static void main(String[] args)throws Exception{
        // Both light-based evidence families are available to this Wraith at this difficulty.
        // The seeded House case places its home in the stair hall, room 4.
        try(var host=LanHost.investigation("127.0.0.1",0,GhostIdentity.Type.WRAITH,CustomContract.standard(3),
                false,HouseMap.Site.HOUSE,null,2,65);
            var van=new Player(host,"Receiver",false,true);
            var dots=new Player(host,"Projector",false,false);
            var laser=new Player(host,"Grid",false,false);
            var ghost=new Player(host,"Wraith",true,false)){
            for(var p:List.of(van,dots,laser,ghost))p.client.ready();
            van.await("LOBBY|3|HUMAN|4|WAITING");
            van.client.start();
            van.await("INFO|mission started");
            for(var p:List.of(van,dots,laser)){
                p.client.view3D();
                p.await("ACCEPT|VIEW3D");
            }
            ghost.client.viewGhost3D();
            ghost.await("ACCEPT|GVIEW3D");

            var lock=LooseClutterNetworkTest.field(host,"mutex");
            var map=(HouseMap)LooseClutterNetworkTest.field(host,"map");
            var world=(LanWorld)LooseClutterNetworkTest.field(host,"world");
            var rack=map.van().location(VanStaging.Station.EQUIPMENT);
            synchronized(lock){
                for(int i=0;i<3;i++)AuthoredTestSupport.place(map,world,i,new Vec(rack.x(),VanStaging.FLOOR_Y,rack.z()));
            }
            van.equip(0,"VIDEO_CAMERA");
            dots.equip(1,"DOTS_PROJECTOR");
            laser.equip(1,"LASER_GRID");

            var subject=map.spawn(4);
            synchronized(lock){
                AuthoredTestSupport.place(map,world,0,new Vec(subject.x(),subject.y(),subject.z()-2.55));
                for(int i=1;i<3;i++)AuthoredTestSupport.place(map,world,i,new Vec(subject.x(),subject.y(),subject.z()-1.31));
            }
            for(var field:List.of(dots,laser)){
                field.client.step(0,100);
                field.await("ACCEPT|STEP");
                field.client.turn(0);
            }
            van.client.turn(0);
            van.use("VIDEO_CAMERA");
            var relay=map.van().location(VanStaging.Station.RELAY);
            int screenHeading;
            synchronized(lock){
                AuthoredTestSupport.place(map,world,0,new Vec(relay.x(),VanStaging.FLOOR_Y,relay.z()));
                screenHeading=new HunterCameraAttention(map).screenHeading(world.body(0));
            }
            van.client.turn(screenHeading);
            van.watch(0);

            LanTestGhostActions.manifest(host,dots.client,dots::await,ghost.client,ghost::await);
            van.observation(0,"CAMGHOST|0|a|VISIBLE|");
            van.noPatterns(0);
            check(van.since(0,"SENSOR|READ|").isEmpty(),"Video alone produced an instrument reading");

            van.watch(-1);
            dots.use("DOTS_PROJECTOR");
            laser.use("LASER_GRID");
            laser.client.turn(180);
            laser.sync();
            int localStart=dots.visual.size(),remoteStart=van.visual.size(),otherStart=laser.visual.size();
            dots.patterns(localStart,false);
            van.noPatterns(remoteStart);
            laser.noPatterns(otherStart);

            van.watch(0);
            remoteStart=van.visual.size();
            van.patterns(remoteStart,true);
            String remoteDots=van.observation(remoteStart,DOTS),remoteLaser=van.observation(remoteStart,LASER);

            van.watch(-1);
            dots.client.turn(180);
            dots.sync();
            laser.client.turn(0);
            laser.sync();
            localStart=dots.visual.size();remoteStart=van.visual.size();otherStart=laser.visual.size();
            laser.patterns(otherStart,false);
            van.noPatterns(remoteStart);
            dots.noPatterns(localStart);
            ghost.noPatterns(0);
            checkPanel(van,remoteDots,remoteLaser);
            System.out.println("PASS real Wraith, powered D.O.T.S. and laser, facing field viewers, selected van feed,"
                +" turned-away and idle controls, and camera-alone sensor negative");
        }
    }
}
