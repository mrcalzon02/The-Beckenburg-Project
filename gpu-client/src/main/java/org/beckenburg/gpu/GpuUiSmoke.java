package org.beckenburg.gpu;

import org.beckenburg.GpuWorldRenderer;
import org.beckenburg.CastleHubPanel;
import org.beckenburg.CastleHubMap;
import org.beckenburg.InvestigationGame;
import org.beckenburg.HouseMap;
import org.beckenburg.HunterPosture;
import org.beckenburg.LanWorldPanel;
import org.beckenburg.LanClient;
import org.beckenburg.BeckenburgDiagnostics;
import org.beckenburg.LevelDefinition;
import org.beckenburg.SceneRenderer;
import org.beckenburg.SiteCatalog;
import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/** Live GPU + Swing acceptance: actually selects a map and enters a solo
 * investigation through the installed game's in-world Castle dispatch workstation.
 */
public final class GpuUiSmoke {
 private GpuUiSmoke(){}
 private static final Path OUTPUT=Path.of(System.getProperty(
     "beckenburg.gpu.uiSmoke.output","build/e2e/gpu/ui"));
 @FunctionalInterface private interface Check<T>{T get()throws Exception;}
 private static JButton button(Container owner,String label){
  for(Component c:owner.getComponents()){
   if(c instanceof JButton b&&b.getText().equals(label))return b;
   if(c instanceof Container child){
    JButton b=button(child,label);if(b!=null)return b;
   }
  }
  return null;
 }
 /** Use the production action identity, not mutable presentation text. */
 private static JButton buttonNamed(Container owner,String name){
  for(Component c:owner.getComponents()){
   if(c instanceof JButton b&&name.equals(b.getName()))return b;
   if(c instanceof Container child){
    JButton b=buttonNamed(child,name);if(b!=null)return b;
   }
  }
  return null;
 }
 private static <T> T waitFor(Check<T> check,long seconds)throws Exception{
  long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(seconds);
  FutureTask<T> pending=null;
  while(System.nanoTime()<end){
   // Keep at most one queued check. A blocked EDT must time out rather than
   // hang invokeAndWait forever or accumulate checks behind a slow frame.
   if(pending==null){pending=new FutureTask<>(check::get);SwingUtilities.invokeLater(pending);}
   long left=end-System.nanoTime();
   if(left<=0)break;
   try{
    T result=pending.get(Math.min(left,TimeUnit.MILLISECONDS.toNanos(250)),TimeUnit.NANOSECONDS);
    pending=null;
    if(result!=null)return result;
    Thread.sleep(85);
   }catch(TimeoutException busy){
    // Revisit this same task, never enqueue another one while it is pending.
   }catch(ExecutionException failed){
    throw new IllegalStateException("GPU UI check failed",failed.getCause());
   }
  }
  return null;
 }
 private static <T> T require(Check<T> check,long seconds,String failure)throws Exception{
  T value=waitFor(check,seconds);
  if(value==null)throw new AssertionError(failure);
  return value;
 }
 private static Object field(LanWorldPanel game,String name)throws Exception{
  var field=game.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(game);
 }
 private static JComboBox<?> sitePicker(Container owner){
  for(Component child:owner.getComponents()){
   if(child instanceof JComboBox<?> box&&box.getItemCount()>0
       &&box.getItemAt(0) instanceof SiteCatalog.Location)return box;
   if(child instanceof Container nested){var found=sitePicker(nested);if(found!=null)return found;}
  }
  return null;
 }
 private static void key(JPanel panel,int code,boolean pressed){
  KeyEvent event=new KeyEvent(panel,pressed?KeyEvent.KEY_PRESSED:KeyEvent.KEY_RELEASED,
      System.currentTimeMillis(),0,code,KeyEvent.CHAR_UNDEFINED);
  for(var listener:panel.getKeyListeners()){
   if(pressed)listener.keyPressed(event);else listener.keyReleased(event);
  }
 }
 /** Capture only our own surface, never another application's screen. */
 private static void capture(JFrame window,String name)throws Exception{
  BufferedImage image=require(()->{
   var result=new BufferedImage(window.getWidth(),window.getHeight(),BufferedImage.TYPE_INT_RGB);
   var g=result.createGraphics();try{window.paint(g);}finally{g.dispose();}return result;
  },15,"UI capture blocked: "+name);
  ImageIO.write(image,"png",OUTPUT.resolve(name+".png").toFile());
 }
 private record Walk(int key,int reverse,int second,int secondReverse,double x,double z){}
 private static Walk walk(LanWorldPanel panel)throws Exception{
  SceneRenderer scene=(SceneRenderer)field(panel,"renderer");
  HouseMap map=(HouseMap)field(panel,"map");
  return planWalk(map,scene);
 }
 private static Walk planWalk(HouseMap map,SceneRenderer scene){
  double feet=scene.eyeHeight-HunterPosture.eye(false);
  int[] keys={KeyEvent.VK_W,KeyEvent.VK_S,KeyEvent.VK_A,KeyEvent.VK_D,KeyEvent.VK_W,KeyEvent.VK_W,KeyEvent.VK_S,KeyEvent.VK_S};
  int[] reverse={KeyEvent.VK_S,KeyEvent.VK_W,KeyEvent.VK_D,KeyEvent.VK_A,KeyEvent.VK_S,KeyEvent.VK_S,KeyEvent.VK_W,KeyEvent.VK_W};
  int[] second={0,0,0,0,KeyEvent.VK_A,KeyEvent.VK_D,KeyEvent.VK_A,KeyEvent.VK_D};
  int[] secondReverse={0,0,0,0,KeyEvent.VK_D,KeyEvent.VK_A,KeyEvent.VK_D,KeyEvent.VK_A};
  int[] forward={1,-1,0,0,1,1,-1,-1},strafe={0,0,-1,1,-1,1,-1,1};
  for(int i=0;i<keys.length;i++){
   double length=Math.hypot(forward[i],strafe[i]);
   double dx=(Math.sin(scene.yaw)*forward[i]-Math.cos(scene.yaw)*strafe[i])/length;
   double dz=(Math.cos(scene.yaw)*forward[i]+Math.sin(scene.yaw)*strafe[i])/length;
   var target=map.resolveMove(new LevelDefinition.Vec(scene.x,feet,scene.z),
       scene.x+dx*.6,scene.z+dz*.6,HunterPosture.STANDING_HEIGHT);
   if(target!=null&&Math.hypot(target.x()-scene.x,target.z()-scene.z)>.5)
    return new Walk(keys[i],reverse[i],second[i],secondReverse[i],scene.x,scene.z);
  }
  throw new AssertionError("No short collision-valid WASD route from van spawn: site="+map.site()+" x="+scene.x+" y="+feet+" z="+scene.z+" yaw="+scene.yaw);
 }
 private static double distance(LanWorldPanel panel,Walk route)throws Exception{
  SceneRenderer scene=(SceneRenderer)field(panel,"renderer");return Math.hypot(scene.x-route.x(),scene.z-route.z());
 }
 private static void move(LanWorldPanel panel,Walk route,boolean returning)throws Exception{
  int code=returning?route.reverse():route.key();
  int second=returning?route.secondReverse():route.second();
  try{
   require(()->{key(panel,code,true);if(second!=0)key(panel,second,true);return Boolean.TRUE;},10,"Movement input blocked");
   require(()->(returning?distance(panel,route)<.22:distance(panel,route)>.30)?Boolean.TRUE:null,
       12,"Live WASD movement did not "+(returning?"return toward van spawn":"advance the hunter"));
  }finally{SwingUtilities.invokeLater(()->{key(panel,code,false);if(second!=0)key(panel,second,false);});}
 }
 private static JFrame launcher(){
  for(Window w:Window.getWindows())
   if(w instanceof JFrame f&&f.isShowing()
       &&f.getTitle().startsWith("The Beckenburg Project"))return f;
  return null;
 }
 private static CastleHubPanel castle(Container root){
  for(Component child:root.getComponents()){
   if(child instanceof CastleHubPanel home&&home.isShowing())return home;
   if(child instanceof Container nested){
    var found=castle(nested);if(found!=null)return found;
   }
  }
  return null;
 }
 /** Uses the actual Castle station callback; look-to-use collision is independently tested. */
 private static void station(CastleHubPanel home,CastleHubMap.Station target){
  try{
   var method=CastleHubPanel.class.getDeclaredMethod("use",CastleHubMap.Station.class);
   method.setAccessible(true);
   method.invoke(home,target);
  }catch(ReflectiveOperationException failure){
   throw new IllegalStateException("Castle workstation "+target+" unavailable",failure);
  }
 }
 private static JPanel dispatch(Container root){
  for(Component child:root.getComponents()){
   if(child instanceof JPanel panel&&"solo".equals(panel.getName())&&panel.isShowing())return panel;
   if(child instanceof Container nested){JPanel found=dispatch(nested);if(found!=null)return found;}
  }
  return null;
 }
 private static LanWorldPanel game(Container root){
  for(Component c:root.getComponents()){
   // Solo and multiplayer now share the real host-authoritative LAN viewport.
   // The retired $GamePanel class is no longer created by InvestigationGame.
   if(c instanceof LanWorldPanel panel&&panel.isShowing())return panel;
   if(c instanceof Container inner){
    LanWorldPanel found=game(inner);if(found!=null)return found;
   }
  }
  return null;
 }
 private static boolean firstSceneReady(LanWorldPanel panel)throws Exception{
  // The cinematic and its PreviewStatus are package-private implementation
  // details; the GPU harness lives in org.beckenburg.gpu. Inspect only the
  // real readiness accessor instead of depending on a retired GamePanel field.
  Object opening=field(panel,"opening");
  if(opening==null)return (Integer)field(panel,"frameCount")>0;
  var backend=opening.getClass().getDeclaredMethod("gpuFlight");
  backend.setAccessible(true);
  if(!Boolean.TRUE.equals(backend.invoke(opening)))return true;
  var snapshot=opening.getClass().getDeclaredMethod("previewStatus");
  snapshot.setAccessible(true);
  Object preview=snapshot.invoke(opening);
  var ready=preview.getClass().getDeclaredMethod("firstFrameReady");
  ready.setAccessible(true);
  return Boolean.TRUE.equals(ready.invoke(preview));
 }
 private static void failureEvidence(JFrame window)throws Exception{
  Files.writeString(OUTPUT.resolve("failure-stage.txt"),
      "GPU UI case startup incomplete\n"+
      "diagnostic_phase="+BeckenburgDiagnostics.stageName()+"\n"+
      "visible_castle="+(castle(window.getContentPane())!=null)+"\n"+
      "visible_solo_dispatch="+(dispatch(window.getContentPane())!=null)+"\n"+
      "visible_live_lan_hunter="+(game(window.getContentPane())!=null)+"\n",
      StandardCharsets.UTF_8);
  try{capture(window,"failed-ui");}
  catch(Exception captureFailure){
   Files.writeString(OUTPUT.resolve("capture-failure.txt"),
       BeckenburgDiagnostics.redact(captureFailure.toString())+"\n",StandardCharsets.UTF_8);
  }
 }
 public static void execute(GpuWorldRenderer gpu)throws Exception{
  Files.createDirectories(OUTPUT);
  StringBuilder results=new StringBuilder("site,opening,dispatch_to_hunter_ms,active_frames,walk_metres,outcome,gpu_readback_confirmed\n");
  InvestigationGame.main(new String[0]);
  JFrame window=waitFor(()->{
   JFrame candidate=launcher();
   return candidate!=null&&castle(candidate.getContentPane())!=null?candidate:null;
  },60);
  if(window==null)throw new AssertionError("GPU-loaded Castle home unavailable");
  System.out.println("GPU UI PASS real Castle home loaded");
  boolean passed=false;
  try{
   var sites=System.getProperty("beckenburg.gpu.uiSmoke.sites","HOUSE,FARMHOUSE,SCHOOL").split(",");
   if(sites.length==0)throw new IllegalArgumentException("No GPU UI sites requested");
   for(int index=0;index<sites.length;index++){
    SiteCatalog.Location site=SiteCatalog.Location.valueOf(sites[index].trim());
    SiteCatalog.requirePlayable(site);
    runCase(gpu,window,site,index,index==1,results);
   }
   System.out.println("GPU UI PASS "+sites.length+" real case dispatch / hunter / movement / extraction / menu cycles");
   passed=true;
  }finally{
   Files.writeString(OUTPUT.resolve("case-cycles.csv"),results,StandardCharsets.UTF_8);
   if(!passed)failureEvidence(window);
   SwingUtilities.invokeLater(window::dispose);
  }
 }
 private static void runCase(GpuWorldRenderer gpu,JFrame window,SiteCatalog.Location site,
     int index,boolean naturalOpening,StringBuilder results)throws Exception{
  CastleHubPanel home=require(()->castle(window.getContentPane()),30,
      "Castle home did not return after investigation");
  require(()->{station(home,CastleHubMap.Station.EXPEDITION);return Boolean.TRUE;},
      15,"Castle expedition workstation unavailable");
  JPanel chooser=waitFor(()->dispatch(window.getContentPane()),15);
  if(chooser==null)throw new AssertionError("GPU launcher did not request location selection");
  require(()->{
   var picker=sitePicker(chooser);
   if(picker==null)throw new AssertionError("Solo location selector missing");
   picker.setSelectedItem(site);
   if(picker.getSelectedItem()!=site)throw new AssertionError("Could not select "+site.name());
   return Boolean.TRUE;
  },10,"Map selection blocked");
  // The solo contract lobby exposes the actual action as "solo.deploy"
  // (visible text: "START ROUND"). "DEPLOY" is not a button on this screen:
  // the previous smoke silently clicked nothing and then timed out.
  JButton deploy=waitFor(()->buttonNamed(chooser,"solo.deploy"),5);
  if(deploy==null)throw new AssertionError(
      "Solo contract lobby has no solo.deploy action (START ROUND)");
  long requested=System.nanoTime();
  require(()->{
   if(!deploy.isShowing()||!deploy.isEnabled())throw new AssertionError(
       "Solo contract action is not visible and enabled");
   SwingUtilities.invokeLater(deploy::doClick);return Boolean.TRUE;
  },10,"Solo deploy blocked");
  System.out.println("GPU UI PASS invoked real solo.deploy / START ROUND action");
  LanWorldPanel inGame=waitFor(()->game(window.getContentPane()),55);
  if(inGame==null)throw new AssertionError(
      "Solo deploy did not expose the live LAN hunter viewport: "+site.name());
  require(()->{
   HouseMap active=(HouseMap)field(inGame,"map");
   return active.site().name().equals(site.name())?Boolean.TRUE:null;
  },10,"Hunter received the wrong authored site: "+site.name());
  require(()->field(inGame,"opening")!=null?Boolean.TRUE:null,5,
      "Actual network hunter never entered the authored opening cinematic");
  require(()->firstSceneReady(inGame)?Boolean.TRUE:null,65,
      "First sized GPU case opening frame never became ready: "+site.name());
  if(!naturalOpening)require(()->{
   if(field(inGame,"opening")!=null)key(inGame,KeyEvent.VK_SPACE,true);
   return Boolean.TRUE;
  },10,"Intro skip blocked");
  require(()->field(inGame,"opening")==null&&(Integer)field(inGame,"frameCount")>=3?Boolean.TRUE:null,
      65,"Opening did not hand off to advancing LAN hunter frames: "+site.name());
  long startupMillis=TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-requested);
  require(()->{
   var timing=((SceneRenderer)field(inGame,"renderer")).frameDiagnostics();
   return timing!=null&&timing.gpu()&&timing.gpuReadbackAvailable()?Boolean.TRUE:null;
  },20,"Playable LAN hunter did not use real GPU framebuffer/readback");
  var first=gpu.latest();
  long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(60);
  boolean rendered=false;
  while(System.nanoTime()<deadline){
   var frame=gpu.latest();
   if(frame!=null&&frame!=first){
    int visible=0;for(int argb:frame.argb())if((argb>>>24)>127)visible++;
    if(visible>400){rendered=true;break;}
   }
   Thread.sleep(70);
  }
  if(!rendered)throw new AssertionError(
      "The live investigation never delivered a new OpenGL framebuffer");
  Walk route=require(()->walk(inGame),10,"Could not plan short van movement");
  move(inGame,route,false);
  double walked=require(()->distance(inGame,route),10,"Could not read actual hunter movement");
  capture(window,index+"-"+site.name().toLowerCase()+"-hunter");
  move(inGame,route,true);
  int frames=require(()->(Integer)field(inGame,"frameCount"),10,"Hunter loop stopped");
  // Physical VIEW3D hunters must seal the VAN door; EXTRACT is intentionally
  // rejected for them. Issue the same authenticated VANDOOR packet as the
  // in-world rear-button action and verify the host-owned RESULT.
  require(()->{
   ((LanClient)field(inGame,"client")).vanDoor();
   return Boolean.TRUE;
  },10,"Physical van departure control unavailable");
  String outcome=require(()->{
   String result=(String)field(inGame,"caseResult");
   return result.isBlank()?null:result;
  },20,"Host never acknowledged the real van-door retreat: "+site.name());
  if(!"RETREATED".equals(outcome))
   throw new AssertionError("Expected host-authoritative RETREATED, got "+outcome);
  capture(window,index+"-"+site.name().toLowerCase()+"-result");
  // Invoke the actual menu's registered return handler after host settlement.
  require(()->{
   Object action=inGame.getClientProperty("beckenburg.returnToLobby");
   if(!(action instanceof Runnable back))throw new AssertionError(
       "Finished LAN hunter has no return-to-Castle action");
   back.run();return Boolean.TRUE;
  },15,"Finished case could not use the production return-to-lobby action");
  require(()->{
   var returnedCastle=castle(window.getContentPane());
   return returnedCastle!=null&&returnedCastle.isShowing()&&game(window.getContentPane())==null?Boolean.TRUE:null;
  },30,"Extraction did not return to Castle home");
  results.append(site.name()).append(',').append(naturalOpening?"natural":"skip").append(',')
      .append(startupMillis).append(',').append(frames).append(',').append(walked)
      .append(',').append(outcome).append(',').append("true")
      .append('\n');
  Files.writeString(OUTPUT.resolve("case-cycles.csv"),results);
  System.out.println("GPU UI PASS "+site.name()+" hunter ready="+startupMillis+"ms, walk="+walked+
      "m, frames="+frames+", real extraction="+outcome+", Castle home restored");
 }
}
