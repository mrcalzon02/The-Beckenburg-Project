package org.beckenburg;
import java.util.*;
import org.beckenburg.LevelDefinition.Vec;

/** Real ghost/hunter sockets prove public haze lifecycle and host movement rejection. */
public final class LanSafeAreaBarrierTest {
    static Object field(Object object,String key)throws Exception{var f=object.getClass().getDeclaredField(key);f.setAccessible(true);return f.get(object);}
    static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        try(var host=new LanHost("127.0.0.1",0);
            var a=new AuthoredLevelNetworkTest.Peer(host.port(),false,"Cleric");
            var b=new AuthoredLevelNetworkTest.Peer(host.port(),false,"Observer");
            var c=new AuthoredLevelNetworkTest.Peer(host.port(),false,"Scholar");
            var g=new AuthoredLevelNetworkTest.Peer(host.port(),true,"Spirit")){
            a.client.ready();b.client.ready();c.client.ready();g.client.ready();
            a.await("LOBBY|3|HUMAN|4|WAITING");a.client.start();a.await("INFO|mission started");
            var map=(HouseMap)field(host,"map");
            for(int room:AuthoredLevelNetworkTest.path(map,14,4)){a.client.move(room);a.await("ACCEPT|MOVE|"+room);}
            a.client.view3D();a.await("ACCEPT|VIEW3D");
            g.client.viewGhost3D();g.await("ACCEPT|GVIEW3D");g.await("SAFEEND|");
            a.client.deploy(HunterCounters.Kind.WARD);a.await("ACCEPT|DEPLOY|WARD");
            var fields=LanProtocol.parts(g.await("SAFEAREA|"));
            check(fields.size()==4,"only public barrier coordinates reach ghost");
            g.await("SAFEEND|");check(map.temporarySafeAreas().size()==1,"host and ghost agree landed ward is live");
            var ward=map.temporarySafeAreas().get(0);var ghost=(LanGhostSpatial)field(host,"ghostPose");
            boolean exercised=false;
            for(int i=0;i<120&&!exercised;i++){
                double angle=i*Math.PI*2/120,dx=Math.cos(angle),dz=Math.sin(angle);
                var start=new Vec(ward.x()+dx*(ward.radius()+.12),ward.y(),ward.z()+dz*(ward.radius()+.12));
                var finish=map.resolveMove(start,start.x()-dx*.31,start.z()-dz*.31);
                if(finish==null||map.roomAt(start.x(),start.y(),start.z())!=4||map.roomAt(finish.x(),finish.y(),finish.z())!=4
                    ||!map.traversable(start.x(),start.y(),start.z())||Math.hypot(finish.x()-start.x(),finish.z()-start.z())<.25
                    ||LanGhostSpatial.hidingPocketAt(map,4,finish.x(),finish.y(),finish.z()))continue;
                SafeAreaBarrierTest.pose(ghost,4,start);Thread.sleep(130);
                g.client.ghostStep((int)Math.round(-dx*100),(int)Math.round(-dz*100));
                check(g.await("REJECT|").contains("safe-area"),"real host rejects crossing the blue ward");
                check(Math.hypot(ghost.pose().x()-start.x(),ghost.pose().z()-start.z())<.01,"rejection preserves spectral pose");exercised=true;
            }
            check(exercised,"authored approach to active ward exists");
            // Re-entering/reconnecting the ghost view gets a complete authoritative snapshot.
            g.client.viewGhost3D();g.await("ACCEPT|GVIEW3D");g.await("SAFEAREA|");g.await("SAFEEND|");
            check(host.running(),"barrier rejection leaves mission running");
            System.out.println("PASS 4 TCP peers: real thrown ward, public landed haze, host crossing rejection and view re-entry snapshot; no private hunter frames");
        }
    }
}
