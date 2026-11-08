package org.beckenburg;

import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

/** Real TCP -> normal world-panel packet handling -> complete gameplay scene.
 * Initial camera/actor positions are fixtures; all tested movement/posture is host accepted.
 */
public final class LanHunterAnimationGameplayTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static Object field(Object object,String name)throws Exception{
        var f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);
    }
    private static final class Peer implements AutoCloseable {
        final List<String> frames=new CopyOnWriteArrayList<>();
        final LanClient client;
        int cursor;
        LanWorldPanel panel;
        Peer(int port,String name)throws Exception{client=new LanClient("127.0.0.1",port,false,name,frames::add);}
        int mark(){return frames.size();}
        String await(int start,String prefix)throws Exception{
            long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);
            while(System.nanoTime()<until){
                for(int i=start;i<frames.size();i++)if(frames.get(i).startsWith(prefix))return frames.get(i);
                Thread.sleep(10);
            }
            var snapshot=List.copyOf(frames);throw new AssertionError("Missing "+prefix+"; tail="+snapshot.subList(Math.max(0,snapshot.size()-15),snapshot.size()));
        }
        void feed()throws Exception{
            var snapshot=List.copyOf(frames);List<String> pending=snapshot.subList(cursor,snapshot.size());cursor=snapshot.size();
            SwingUtilities.invokeAndWait(()->{for(String packet:pending)panel.onEvent(packet);});
        }
        SceneRenderer renderer()throws Exception{return (SceneRenderer)field(panel,"renderer");}
        public void close(){
            try{if(panel!=null)SwingUtilities.invokeAndWait(panel::close);}catch(Exception e){throw new RuntimeException(e);}
            finally{client.close();}
        }
    }
    private static HunterSpriteAnimation.Pose render(Peer viewer,int seat,Path capture)throws Exception{
        viewer.feed();
        var renderer=viewer.renderer();
        @SuppressWarnings("unchecked")
        var bodies=(java.util.Map<Integer,SceneRenderer.RemoteBody>)field(viewer.panel,"peers");
        var body=bodies.get(seat);
        if(body!=null)renderer.yaw=Math.atan2(body.x()-renderer.x,body.z()-renderer.z);
        SwingUtilities.invokeAndWait(viewer.panel::renderWorld);
        if(capture!=null)ImageIO.write(renderer.image(),"PNG",capture.toFile());
        return renderer.hunterPose(seat);
    }
    private static void press(Peer actor,int key)throws Exception{
        actor.feed();
        SwingUtilities.invokeAndWait(()->{
            actor.panel.keyPressed(new KeyEvent(actor.panel,KeyEvent.KEY_PRESSED,0,0,key,KeyEvent.CHAR_UNDEFINED));
            actor.panel.keyReleased(new KeyEvent(actor.panel,KeyEvent.KEY_RELEASED,0,0,key,KeyEvent.CHAR_UNDEFINED));
        });
    }
    /** Exercise every real host tick while keeping slow raster/file IO out of
     * simulation time. Never forge a position, pose, panel clock or packet. */
    private static void advance(LanHost host,Peer[] peers,int count)throws Exception{
        var tick=LanHost.class.getDeclaredMethod("tickSafely");tick.setAccessible(true);
        for(int step=0;step<count;step++){
            int[] marks=new int[peers.length];for(int i=0;i<peers.length;i++)marks[i]=peers[i].mark();
            int now;synchronized(field(host,"mutex")){int before=(int)field(host,"tick");tick.invoke(host);now=(int)field(host,"tick");check(now==before+1&&host.running(),"Actual host tick must advance once without simulation failure");}
            // Periodic state/world snapshots are emitted every fifth tick.
            // STATE precedes the world batch. Wait through its final hunter's
            // actual posture row, so no previous body is rendered by accident.
            if(now%5==0)for(int i=0;i<peers.length;i++)peers[i].await(marks[i],"POSTURE|2|0|"+now);
        }
    }
    public static void main(String[] args)throws Exception{
        boolean gpu=List.of(args).contains("--gpu");
        java.lang.reflect.Method stop=null;
        if(gpu){
            var launcher=Class.forName("org.beckenburg.gpu.GpuDesktop");
            var start=launcher.getDeclaredMethod("startGpu");start.setAccessible(true);start.invoke(null);
            stop=launcher.getDeclaredMethod("stopReviewGpu");stop.setAccessible(true);
        }
        try{run(gpu);}finally{if(stop!=null)stop.invoke(null);}
    }
    private static void run(boolean gpu)throws Exception{
        Path captures=Path.of("build/e2e/hunter-animation-gameplay"+(gpu?"-gpu":""));Files.createDirectories(captures);
        try(var host=new LanHost("127.0.0.1",0);
            var cleric=new Peer(host.port(),"Cleric");
            var observer=new Peer(host.port(),"Observer");
            var demonologist=new Peer(host.port(),"Demonologist")){
            Peer[] peers={cleric,observer,demonologist};
            var roles=CharacterVisuals.Hunter.values();
            for(int i=0;i<peers.length;i++){peers[i].client.selectVisual(roles[i]);peers[i].client.ready();}
            cleric.await(0,"LOBBY|3|AI|3|WAITING");
            cleric.client.start();cleric.await(0,"INFO|mission started");
            for(Peer peer:peers){int mark=peer.mark();peer.client.view3D();peer.await(mark,"ACCEPT|VIEW3D");}
            // A cold software render can take seconds on a shared CI worker.
            // Let the in-flight real tick finish, then advance its actual method
            // explicitly; otherwise each render can cross the gait reset gap.
            var clock=(java.util.concurrent.ScheduledExecutorService)field(host,"clock");
            clock.shutdown();check(clock.awaitTermination(5,TimeUnit.SECONDS),"Host clock must quiesce before deterministic acceptance ticks");
            var map=(HouseMap)field(host,"map");var world=(LanWorld)field(host,"world");Object mutex=field(host,"mutex");
            for(Peer peer:peers){
                SwingUtilities.invokeAndWait(()->{peer.panel=new LanWorldPanel(peer.client);});
                ((javax.swing.Timer)field(peer.panel,"timer")).stop();peer.feed();
                // Skip the opening using the same key path as the player.
                press(peer,KeyEvent.VK_SPACE);
            }
            advance(host,peers,1);
            var fixture=AuthoredTestSupport.flatCross(map,2.4).at();
            double x=fixture.x(),z=fixture.z(),y=fixture.y();
            var initial=new LanWorld.Body[]{world.body(0),world.body(1),world.body(2)};
            for(int seat=0;seat<3;seat++){
                Peer actor=peers[seat],viewer=peers[(seat+1)%3];
                String role=roles[seat].name().toLowerCase(java.util.Locale.ROOT);
                synchronized(mutex){
                    for(int j=0;j<3;j++)AuthoredTestSupport.place(map,world,j,
                        new LevelDefinition.Vec(initial[j].x(),initial[j].y(),initial[j].z()));
                    AuthoredTestSupport.place(map,world,seat,new LevelDefinition.Vec(x,y,z));
                    AuthoredTestSupport.place(map,world,(seat+1)%3,new LevelDefinition.Vec(x,y,z-2.4));
                }
                int mark=viewer.mark();actor.client.turn(180);
                viewer.await(mark,"POSTURE|"+seat+"|");viewer.feed();
                var camera=viewer.renderer();camera.yaw=0;camera.pitch=-.04;
                check(render(viewer,seat,null)==HunterSpriteAnimation.Pose.IDLE,"new actor begins idle "+role);
                var poses=EnumSet.noneOf(HunterSpriteAnimation.Pose.class);
                for(int step=0;step<4;step++){
                    advance(host,peers,1);
                    int received=viewer.mark(),accepted=actor.mark();actor.client.step(100,0);
                    actor.await(accepted,"ACCEPT|STEP");viewer.await(received,"POSTURE|"+seat+"|");
                    var pose=render(viewer,seat,null);boolean first=poses.add(pose);
                    if(first&&(pose==HunterSpriteAnimation.Pose.WALK_A||pose==HunterSpriteAnimation.Pose.WALK_B))
                        ImageIO.write(camera.image(),"PNG",captures.resolve(role+"-"+pose.name().toLowerCase(java.util.Locale.ROOT)+".png").toFile());
                    // Deliberately reproduce a slow render/capture interval. Its
                    // wall duration must not invent missing simulation samples.
                    if(step==0)Thread.sleep(1100);
                }
                check(poses.contains(HunterSpriteAnimation.Pose.WALK_A)&&poses.contains(HunterSpriteAnimation.Pose.WALK_B),
                    "real accepted STEP packets must render both walk frames "+role+poses);
                // Stop sending movement and receive a real stationary periodic
                // snapshot. STEP(0,0) is an invalid host motion vector, not stop.
                advance(host,peers,3);int remainder=(int)field(host,"tick")%5;
                if(remainder!=0)advance(host,peers,5-remainder);
                check(render(viewer,seat,captures.resolve(role+"-idle.png"))==HunterSpriteAnimation.Pose.IDLE,"stop returns to idle "+role);
                if(gpu)check(camera.frameDiagnostics().gpu(),"gameplay must use actual GPU world renderer");
                int low=viewer.mark();press(actor,KeyEvent.VK_CONTROL);viewer.await(low,"POSTURE|"+seat+"|1|");
                check(render(viewer,seat,captures.resolve(role+"-crouch.png"))==HunterSpriteAnimation.Pose.CROUCH,"Ctrl selects crouch artwork "+role);
                int up=viewer.mark();press(actor,KeyEvent.VK_CONTROL);viewer.await(up,"POSTURE|"+seat+"|0|");
                check(render(viewer,seat,null)==HunterSpriteAnimation.Pose.IDLE,"Ctrl restores idle "+role);
                // Scene replacement must not inherit this seat's prior walk stride.
                camera.drawNetwork(new HouseMap(),List.of(new SceneRenderer.RemoteBody(x+.1,y,z,seat,180,role)),
                    (SceneRenderer.Apparition)null,(int)field(viewer.panel,"tick")*.1+.1);
                check(camera.hunterPose(seat)==HunterSpriteAnimation.Pose.IDLE,"new scene resets gait "+role);
                System.out.println("PASS gameplay "+role+": TCP STEP -> WALK_A/WALK_B; stop -> IDLE; Ctrl -> CROUCH -> IDLE; scene reset");
            }
            int low=cleric.mark();press(demonologist,KeyEvent.VK_CONTROL);cleric.await(low,"POSTURE|2|1|");
            check(render(cleric,2,null)==HunterSpriteAnimation.Pose.CROUCH,"departure control starts crouched");
            int gone=cleric.mark();demonologist.client.close();cleric.await(gone,"PEER|LEFT|2");cleric.feed();
            check(cleric.renderer().hunterPose(2)==HunterSpriteAnimation.Pose.IDLE,"departed seat forgets pose");
            System.out.println("PASS real TCP and live gameplay panel hunter animation; captures="+captures);
        }
    }
}
