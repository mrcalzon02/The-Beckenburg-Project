package org.beckenburg;
import java.util.*;
import java.util.concurrent.*;
/** Real sockets verify revision negotiation, XYZ snapshots and private ghost projection. */
public final class AuthoredLevelNetworkTest {
 static class Peer implements AutoCloseable {
  final BlockingQueue<String> inbox=new LinkedBlockingQueue<>();final LanClient client;
  Peer(int port,boolean ghost,String name)throws Exception{client=new LanClient("127.0.0.1",port,ghost,name,inbox::offer);}
  Peer(int port,LanClient.ResumeTicket ticket)throws Exception{client=new LanClient("127.0.0.1",port,ticket,inbox::offer);}
  String await(String prefix)throws Exception{return await(frame->frame.startsWith(prefix),prefix);}
  String awaitExact(String expected)throws Exception{return await(expected::equals,expected);}
  private String await(java.util.function.Predicate<String> matches,String description)throws Exception{
   long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
   while(System.nanoTime()<end){
    String frame=inbox.poll(50,TimeUnit.MILLISECONDS);
    if(frame==null)continue;
    if(client.seat().equals("GHOST"))assertPublic(frame);
    if(matches.test(frame))return frame;
   }
   throw new AssertionError("missing "+description);
  }
  public void close(){client.close();}
 }
 public static void main(String[] args)throws Exception{
  for(HouseMap.Site site:new HouseMap.Site[]{HouseMap.Site.HOUSE,HouseMap.Site.FARMHOUSE,HouseMap.Site.SCHOOL}){
   HouseMap map=new HouseMap(site);
   try(LanHost host=new LanHost("127.0.0.1",0,GhostIdentity.Type.SPIRIT,CustomContract.standard(3),false,false,site);
       Peer a=new Peer(host.port(),false,"Cleric");Peer b=new Peer(host.port(),false,"Observer");Peer c=new Peer(host.port(),false,"Demonologist");Peer ghost=new Peer(host.port(),true,"Spirit")){
    check(a.await("SITE|").equals("SITE|"+site+"|"+map.level().revision()),"site revision");
    a.client.ready();b.client.ready();c.client.ready();ghost.client.ready();
    a.await("LOBBY|3|HUMAN|4|WAITING");a.client.start();a.await("INFO|mission started");
    int target=map.floorY(6)>0?6:8;
    List<Integer> path=path(map,14,target);for(int room:path){a.client.move(room);a.await("ACCEPT|MOVE|"+room);}
    a.client.view3D();a.await("ACCEPT|VIEW3D");
    String body=a.await("BODY|0|");var fields=LanProtocol.parts(body);check(fields.size()==7,"XYZ body width");
    double x=Double.parseDouble(fields.get(2))/1000,y=Double.parseDouble(fields.get(3))/1000,z=Double.parseDouble(fields.get(4))/1000;
    check(Math.abs(y-map.floorY(target))<.001&&y>0&&map.roomAt(x,y,z)==target,"upper storey host transform");
    a.await("DOORS|");a.await("WINDOWS|");
    if(site!=HouseMap.Site.SCHOOL) verifyWindow(site,map,a,b);
    a.client.scan(LanCase.Tool.EMF);a.await("ACCEPT|SCAN");
    a.client.journal();a.await("ACCEPT|JOURNAL");
    ghost.client.viewGhost3D();ghost.await("ACCEPT|GVIEW3D");ghost.await("SITE|");
    check(ghost.await("HIDECONFIG|").equals("HIDECONFIG|"+CustomContract.standard(3).hidingSpots()),"ghost sees the same physical hiding-cupboard layout");
    String pose=ghost.await("GPOSE|");check(LanProtocol.parts(pose).size()==7,"XYZ ghost pose");
    ghost.client.journal();ghost.await("REJECT|");
    for(String message:ghost.inbox)assertPublic(message);
    check(host.running(),"host alive after height snapshots");
    System.out.println("PASS "+site+": real host/client XYZ, map revision, door/window resync and ghost privacy");
   }
  }
 }
 private static void verifyWindow(HouseMap.Site site,HouseMap map,Peer hunter,Peer observer)throws Exception {
  boolean house=site==HouseMap.Site.HOUSE;
  // Briar House's current upstairs landing reaches the south leaf. The
  // farmhouse retains its measured west-window approach.
  int expectedWindow=house?23:18;
  int steps=house?6:9;
  hunter.client.window();
  String distant=hunter.await("REJECT|");
  check(distant.contains("no reachable window"),"upper spawn should be outside window reach: "+distant);
  String body=null;
  for(int i=0;i<steps;i++){
   Thread.sleep(125);
   hunter.client.step(house?0:-100,house?100:0);
   hunter.await("ACCEPT|STEP");
   body=hunter.await("BODY|0|");
  }
  var fields=LanProtocol.parts(body);
  double x=Double.parseDouble(fields.get(2))/1000;
  double y=Double.parseDouble(fields.get(3))/1000;
  double z=Double.parseDouble(fields.get(4))/1000;
  var window=map.nearestWindow(x,y,z);
  check(window!=null&&window.id()==expectedWindow&&map.traversable(x,y,z),"Host body missed measured window "+body);
  double dx=window.x()-x,dz=window.z()-z;
  var view=new LooseClutter.Observer(0,x,y,z,Math.atan2(dx,dz),
   Math.atan2(window.y()+1.58-y-HunterPosture.STANDING_EYE,Math.hypot(dx,dz)),true);
  var focus=InteractionFocus.pick(map,view,List.of());
  check(focus!=null&&focus.kind()==InteractionFocus.Kind.WINDOW&&focus.id().equals(Integer.toString(expectedWindow)),
   "Measured standing pose cannot aim at the fitted leaf");
  hunter.client.window();
  hunter.await("ACCEPT|WINDOW");
  String state="WINDOWS|"+expectedWindow;
  hunter.awaitExact(state);
  observer.awaitExact(state);
  HouseMap remote=new HouseMap(site);
  remote.applyWindowSnapshot(state.substring(8));
  check(remote.windowOpen(expectedWindow),"Remote did not restore the measured window");
  Thread.sleep(370);
  hunter.client.window();
  hunter.await("ACCEPT|WINDOW");
  hunter.awaitExact("WINDOWS|");
  observer.awaitExact("WINDOWS|");
  remote.applyWindowSnapshot("");
  check(!remote.windowOpen(expectedWindow),"Remote did not restore the closed window");
  System.out.printf(java.util.Locale.US,"WINDOW %s id=%d approach=(%.2f, %.2f, %.2f) separate peer open/close%n",
   site,expectedWindow,x,y,z);
 }
 static void assertPublic(String frame){
  for(String prefix:new String[]{"BODY|","TEAMMAP|","PROGRESS|","EVIDENCE|","APPARITION|","MOTION|","SOUND|","CASEFILE|","GEARSTATE|","RECORDER|","RECAUDIO|","BOOK|","DOTS|","CAMERA|","COUNTER|"})
   check(!frame.startsWith(prefix),"private hunter state leaked: "+prefix);
 }
 static List<Integer> path(HouseMap map,int from,int to){Map<Integer,Integer> parent=new HashMap<>();ArrayDeque<Integer> q=new ArrayDeque<>();q.add(from);parent.put(from,-1);while(!q.isEmpty()){int r=q.remove();if(r==to)break;for(var p:map.level().portalsFrom(r))if(!p.kind().equals("window")){int n=p.a()==r?p.b():p.a();if(!parent.containsKey(n)){parent.put(n,r);q.add(n);}}}List<Integer> out=new ArrayList<>();for(int r=to;r!=from;r=parent.get(r))out.add(r);Collections.reverse(out);return out;}
 static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
}
