package org.beckenburg;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/** Real TCP: a deployed camera displays Shade/Specter at the van, not to the naked eye. */
public final class LanCameraOnlyGhostIntegrationTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static final class Peer implements AutoCloseable {
        final BlockingQueue<String> inbox=new LinkedBlockingQueue<>();
        final LanClient client;
        Peer(int port,boolean ghost,String name)throws Exception{
            client=new LanClient("127.0.0.1",port,ghost,name,inbox::offer);
        }
        String await(String prefix,int seconds)throws Exception{
            long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(seconds);
            while(System.nanoTime()<until){
                String value=inbox.poll(80,TimeUnit.MILLISECONDS);
                if(value!=null&&value.startsWith("REJECT|"))
                    throw new AssertionError(client.seat()+" command rejected: "+value);
                if(value!=null&&value.startsWith(prefix))return value;
            }
            throw new AssertionError(client.seat()+" missing "+prefix);
        }
        @Override public void close(){client.close();}
    }
    public static void main(String[] args)throws Exception{
        for(var species:new GhostIdentity.Type[]{
                GhostIdentity.Type.SHADE,GhostIdentity.Type.SPECTER}){
            try(var host=new LanHost("127.0.0.1",0,species,CustomContract.standard(2));
                var van=new Peer(host.port(),false,"Camera operator");
                var field=new Peer(host.port(),false,"Field observer");
                var ghost=new Peer(host.port(),true,"Camera-only apparition")){
                van.client.ready();field.client.ready();ghost.client.ready();
                van.await("LOBBY|2|HUMAN|3|WAITING",12);
                van.client.start();van.await("INFO|mission started",12);
                van.client.view3D();van.await("ACCEPT|VIEW3D",8);
                var map=(HouseMap)LooseClutterNetworkTest.field(host,"map");
                var world=(LanWorld)LooseClutterNetworkTest.field(host,"world");
                var mutex=LooseClutterNetworkTest.field(host,"mutex");
                var rack=map.van().location(VanStaging.Station.EQUIPMENT);
                var vanPosition=new LevelDefinition.Vec(rack.x(),VanStaging.FLOOR_Y,rack.z());
                synchronized(mutex){AuthoredTestSupport.place(map,world,0,vanPosition);}
                van.client.equipAt(0,"VIDEO_CAMERA");
                van.await("ACCEPT|GEAREQUIP|0|VIDEO_CAMERA",8);
                var target=map.spawn(4);
                LevelDefinition.Vec station=null;
                double[][] offsets={{0,1.2},{0,-1.2},{1.2,0},{-1.2,0},
                                    {1.3,1.3},{-1.3,-1.3},{.8,.8},{-.8,-.8}};
                for(double[] offset:offsets){
                    double px=target.x()+offset[0],pz=target.z()+offset[1];
                    if(map.roomAt(px,target.y(),pz)==4
                        &&map.traversable(px,target.y(),pz)
                        &&map.clearLineOfSight(px,target.y()+1.62,pz,
                            target.x(),target.y()+1.0,target.z())){
                        station=new LevelDefinition.Vec(px,target.y(),pz);
                        break;
                    }
                }
                check(station!=null,"House must have a real unobstructed camera placement");
                int heading=Math.floorMod((int)Math.round(Math.toDegrees(
                    Math.atan2(target.x()-station.x(),target.z()-station.z()))),360);
                synchronized(mutex){AuthoredTestSupport.place(map,world,0,station);}
                van.client.turn(heading);
                van.client.useHeldEquipment();
                van.await("CAMERA|PLACE|0|4|",8);
                van.await("ACCEPT|GEARUSE|VIDEO_CAMERA|",8);
                // The actual remote camera now stays at the station. The viewing
                // investigator returns to the van; a second Hunter stays onsite.
                synchronized(mutex){AuthoredTestSupport.place(map,world,0,vanPosition);}
                for(int room:new int[]{11,8,5,4}){
                    field.client.move(room);
                    field.await("ACCEPT|MOVE|"+room,8);
                }
                field.client.view3D();field.await("ACCEPT|VIEW3D",8);
                ghost.client.viewGhost3D();ghost.await("ACCEPT|GVIEW3D",8);
                int tick=0;
                while(tick<82){
                    String[] state=ghost.await("STATE|",8).split("\\|");
                    tick=Integer.parseInt(state[1]);
                }
                field.inbox.clear();
                ghost.client.ghost(GhostDomain.Action.MANIFEST,4);
                ghost.await("ACCEPT|GHOST|MANIFEST",8);
                String cameraFrame=van.await("CAMGHOST|0|a|VISIBLE|",10);
                check(cameraFrame.contains("|"+species.name().toLowerCase(java.util.Locale.ROOT)+"|"),
                    "Remote van receiver must show correct "+species+" figure");
                check(field.await("APPARITION|HIDDEN|",8).startsWith("APPARITION|HIDDEN|"),
                    "A camera-only species must remain invisible to direct Hunter vision");
                check(host.running(),"True remote apparition must not stop multiplayer match");
                System.out.println("PASS "+species+": deployed live camera, real van receiver, direct sight distinction");
            }
        }
    }
}
