package org.beckenburg;
import java.util.*;
import javax.swing.SwingUtilities;
public final class HunterReactionsNetworkTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static Object field(Object o,String n)throws Exception{return LooseClutterNetworkTest.field(o,n);}
 public static void main(String[] args)throws Exception{
  try(var host=new LanHost("127.0.0.1",0,GhostIdentity.Type.SPIRIT);
      var a=new AuthoredLevelNetworkTest.Peer(host.port(),false,"Cleric");
      var b=new AuthoredLevelNetworkTest.Peer(host.port(),false,"Observer");
      var c=new AuthoredLevelNetworkTest.Peer(host.port(),false,"Demonologist");
      var g=new AuthoredLevelNetworkTest.Peer(host.port(),true,"Spirit")){
   a.client.ready();b.client.ready();c.client.ready();g.client.ready();a.await("LOBBY|3|HUMAN|4|WAITING");a.client.start();a.await("INFO|mission started");
   a.client.view3D();a.await("ACCEPT|VIEW3D");b.client.view3D();b.await("ACCEPT|VIEW3D");g.client.viewGhost3D();g.await("ACCEPT|GVIEW3D");
   var map=(HouseMap)field(host,"map");var world=(LanWorld)field(host,"world");var mission=(LanCase)field(host,"mission");
   var room=map.level().room(4);LevelDefinition.Vec at=null;
   outer:for(double x=room.x0()+.6;x<room.x1()-.6;x+=.3)for(double z=room.z0()+.6;z<room.z1()-.6;z+=.3)if(map.traversable(x,room.y(),z)){at=new LevelDefinition.Vec(x,room.y(),z);break outer;}
   check(at!=null,"damage fixture on actual accessible floor");
   var effect=LanHost.class.getDeclaredMethod("distributeEffect",GhostDomain.Resolution.class,boolean[].class);effect.setAccessible(true);
   boolean[] victims=new boolean[6];victims[0]=true;
   synchronized(field(host,"mutex")){
    AuthoredTestSupport.place(map,world,0,at);((int[])field(host,"hunterRooms"))[0]=4;
    effect.invoke(host,new GhostDomain.Resolution(true,"fixture hit",GhostDomain.Action.STRIKE,4,50,0,20,true),victims);
   }
   String hit=b.await("HUNTER|HIT|0|");a.await("HUNTER|HIT|0|");check(mission.health(0)<100&&mission.alive(0),"pain requires actual nonlethal health loss");
   b.inbox.clear();
   synchronized(field(host,"mutex")){effect.invoke(host,new GhostDomain.Resolution(true,"no victims",GhostDomain.Action.STRIKE,4,50,0,20,true),new boolean[6]);}
   check(b.inbox.stream().noneMatch(s->s.startsWith("HUNTER|HIT|")),"no pain broadcast for zero applied damage");
   final LanWorldPanel[] panel=new LanWorldPanel[1];SwingUtilities.invokeAndWait(()->panel[0]=new LanWorldPanel(b.client));
   try{
    SwingUtilities.invokeAndWait(()->panel[0].onEvent(hit));var renderer=(SceneRenderer)field(panel[0],"renderer");
    double event=Double.parseDouble(LanProtocol.parts(hit).get(3))*.1;
    check(renderer.hunterPainVisible(0,event+.1)&&!renderer.hunterPainVisible(0,event+.5),"real TCP packet drives bounded pain pose");
    synchronized(field(host,"mutex")){effect.invoke(host,new GhostDomain.Resolution(true,"fatal fixture hit",GhostDomain.Action.STRIKE,4,50,0,500,true),victims);}
    String down=b.await("HUNTER|DOWN|0");SwingUtilities.invokeAndWait(()->panel[0].onEvent(down));
    check(!mission.alive(0)&&!renderer.hunterPainVisible(0,event+.1),"host-confirmed down replaces pain");
    SwingUtilities.invokeAndWait(()->{
     try{var f=LanWorldPanel.class.getDeclaredField("feedback");f.setAccessible(true);f.set(panel[0],"current interaction feedback");panel[0].onEvent(down);
      check(f.get(panel[0]).equals("current interaction feedback"),"repeated down snapshots must not spam feedback");
     }catch(ReflectiveOperationException e){throw new AssertionError(e);}
    });
    b.inbox.clear();var broadcast=LanHost.class.getDeclaredMethod("broadcastWorld");broadcast.setAccessible(true);synchronized(field(host,"mutex")){broadcast.invoke(host);}
    b.await("BODY|0|");b.await("HUNTER|DOWN|0");
    SwingUtilities.invokeAndWait(()->panel[0].onEvent("HUNTER|DOWN|5"));
    check(((Set<?>)field(renderer,"downedHunters")).contains(5),"sixth hunter can render a fallen body");
    for(String lens:new String[]{"videoRenderer","photoRenderer"})check(((Set<?>)field(field(panel[0],lens),"downedHunters")).contains(0),"reaction shared by camera lenses");
    check(g.inbox.stream().noneMatch(s->s.startsWith("HUNTER|HIT|")||s.startsWith("HUNTER|DOWN|")),"human ghost never receives private hunter status");
    SwingUtilities.invokeAndWait(()->panel[0].onEvent("PEER|LEFT|0"));check(!((Set<?>)field(renderer,"downedHunters")).contains(0),"departed seat clears body state");
    SwingUtilities.invokeAndWait(()->panel[0].onEvent("SITE|HOUSE|"+map.level().revision()));check(((Set<?>)field(renderer,"downedHunters")).isEmpty(),"site replacement clears old bodies");
   }finally{SwingUtilities.invokeAndWait(panel[0]::close);}
   System.out.println("PASS real TCP applied damage -> timed pain -> fallen body, persistent dead BODY snapshots, six-seat support, lens parity and ghost privacy");
  }
 }
}
