package org.beckenburg;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/** Real TCP: a deployed camera displays Shade/Specter at the van, not to the naked eye. */
public final class LanCameraOnlyGhostIntegrationTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static final class Peer implements AutoCloseable {
        final BlockingQueue<String> inbox=new LinkedBlockingQueue<>();
        final java.util.concurrent.ConcurrentLinkedDeque<String> cameraFrames=
            new java.util.concurrent.ConcurrentLinkedDeque<>();
        final LanClient client;
        Peer(int port,boolean ghost,String name)throws Exception{
            client=new LanClient("127.0.0.1",port,ghost,name,packet->{
                if(packet.startsWith("CAMGHOST|")||packet.startsWith("CAMERA|")){
                    cameraFrames.offer(packet);
                    while(cameraFrames.size()>20)cameraFrames.poll();
                }
                inbox.offer(packet);
            });
        }
        String await(String prefix,int seconds)throws Exception{
            long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(seconds);
            while(System.nanoTime()<until){
                String value=inbox.poll(80,TimeUnit.MILLISECONDS);
                if(value!=null&&value.startsWith("REJECT|"))
                    throw new AssertionError(client.seat()+" command rejected: "+value);
                if(value!=null&&value.startsWith(prefix))return value;
            }
            throw new AssertionError(client.seat()+" missing "+prefix+
                "; recent camera messages="+cameraFrames);
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
                int heading=-1;
                double[][] offsets={{0,1.2},{0,-1.2},{1.2,0},{-1.2,0},
                    {1.3,1.3},{-1.3,-1.3},{.8,.8},{-.8,-.8},
                    {0,2.4},{0,-2.4},{2.4,0},{-2.4,0}};
                var ghostBody=new LanGhostSpatial.Pose(4,target.x(),target.y(),target.z(),0);
                for(double[] offset:offsets){
                    double px=target.x()+offset[0],pz=target.z()+offset[1];
                    int aim=Math.floorMod((int)Math.round(Math.toDegrees(
                        Math.atan2(target.x()-px,target.z()-pz))),360);
                    if(map.roomAt(px,target.y(),pz)==4&&map.traversable(px,target.y(),pz)
                        &&LanCameraSight.sees(map,new LanVideoCameras.Camera(
                            0,4,px,target.y(),pz,aim,EquipmentTiers.Tier.I),
                            ghostBody,true,species,"a")){
                        station=new LevelDefinition.Vec(px,target.y(),pz);
                        heading=aim;
                        break;
                    }
                }
                check(station!=null,
                    "No camera installation has a true frustum and clear sight to the apparition");
                synchronized(mutex){AuthoredTestSupport.place(map,world,0,station);}
                van.client.turn(heading);
                van.client.useHeldEquipment();
                String installed=van.await("CAMERA|PLACE|0|4|",8);
                check(installed.endsWith("|"+heading+"|I"),
                    "Host must preserve deployed camera heading and actual tier: "+installed);
                van.await("CAMERA|POWER|0|1",8);
                van.await("ACCEPT|GEARUSE|VIDEO_CAMERA|",8);
                // The actual remote camera now stays at the station. The viewing
                // investigator returns to the van; a second Hunter stays onsite.
                synchronized(mutex){AuthoredTestSupport.place(map,world,0,vanPosition);}
                check(map.roomAt(vanPosition.x(),vanPosition.y(),vanPosition.z())
                    ==HouseMap.EXTRACTION_ROOM,
                    "Van receiver must be physically inside the extraction room");
                // Follow the current authored level graph; 14 -> 11 is NOT
                // a legal immediate move in the house layout.
                for(int room:AuthoredLevelNetworkTest.path(map,HouseMap.EXTRACTION_ROOM,4)){
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
                field.cameraFrames.removeIf(packet->packet.startsWith("CAMGHOST|"));
                // MANIFEST requires a genuinely audible nearby investigator.
                // Silent room occupancy must NEVER grant secret ghost senses.
                // The prior fixed wait (tick 82) can race simulation ticks and
                // legal ability windows. Exercise actual field EMF interactions,
                // then request manifestation only once the host's authoritative
                // domain reports the ability legal, without bypassing its costs.
                var domain=(GhostDomain)LooseClutterNetworkTest.field(host,"ghost");
                boolean legal=false;
                for(int attempt=0;attempt<12&&!legal;attempt++){
                    field.client.scan(LanCase.Tool.EMF);
                    field.await("ACCEPT|SCAN|",8);
                    synchronized(mutex){
                        legal=domain.observe().legalActions().contains(GhostDomain.Action.MANIFEST);
                    }
                    if(!legal)Thread.sleep(110);
                }
                check(legal,"Real, audible field scans never opened a legal "
                    +"manifestation window: "+domain.observe());
                ghost.client.ghost(GhostDomain.Action.MANIFEST,4);
                ghost.await("ACCEPT|GHOST|MANIFEST",8);
                synchronized(mutex){
                    var mounted=(LanVideoCameras)LooseClutterNetworkTest.field(host,"cameras");
                    var pose=(LanGhostSpatial)LooseClutterNetworkTest.field(host,"ghostPose");
                    check(mounted.at(0)!=null
                        &&LanCameraSight.sees(map,mounted.at(0),pose.pose(),true,species,"a"),
                        "The installed camera must genuinely see the manifested actor");
                }
                String cameraFrame=van.await("CAMGHOST|0|a|VISIBLE|",10);
                check(cameraFrame.contains("|"+GhostManifestationVisual.UNREVEALED+"|")
                    &&!cameraFrame.contains("|"+species.name().toLowerCase(java.util.Locale.ROOT)+"|"),
                    "Non-signature live camera must show a neutral apparition, not "+species+" artwork");
                check(field.await("APPARITION|HIDDEN|",8).startsWith("APPARITION|HIDDEN|"),
                    "A camera-only species must remain invisible to direct Hunter vision");
                Thread.sleep(150); // Drain any queued same-tick packets to the field seat.
                check(field.cameraFrames.stream().noneMatch(x->x.startsWith("CAMGHOST|")),
                    "Offsite investigators must receive no van-only camera captures");
                check(host.running(),"True remote apparition must not stop multiplayer match");
                System.out.println("PASS "+species+": deployed live camera, real van receiver, direct sight distinction");
            }
        }
    }
}
