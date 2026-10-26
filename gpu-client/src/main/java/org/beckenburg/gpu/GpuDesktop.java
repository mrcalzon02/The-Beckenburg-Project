package org.beckenburg.gpu;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import org.beckenburg.GpuWorldRenderer;
import org.beckenburg.HouseMap;
import org.beckenburg.InvestigationGame;
import org.beckenburg.RoomLighting;
import org.beckenburg.SceneRenderer;
import org.beckenburg.LevelOpeningFlyover;
import org.beckenburg.CastleHubMap;
import org.beckenburg.BeckenburgDiagnostics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;

/** Installed GPU-first Beckenburg launcher.
 * The GLFW context stays hidden while Swing displays the established
 * controls/HUD; every world framebuffer comes from the OpenGL 3.3 worker.
 */
public final class GpuDesktop {
 private GpuDesktop(){}
 private static Thread gpuWorker;
 private static final OpenGlWorldRenderer ENGINE=new OpenGlWorldRenderer();
 private static void startGpu() throws Exception {
  var config=new Lwjgl3ApplicationConfiguration();
  config.setTitle("Beckenburg / OpenGL 3.3 world renderer");
  config.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.GL30,3,3);
  config.setWindowedMode(640,400);
  config.setInitialVisible(false);
  config.setResizable(false);
  config.useVsync(false);
  config.setForegroundFPS(60);
  Thread worker=new Thread(()->{
   try{new Lwjgl3Application(ENGINE,config);}
   catch(Throwable failed){
    ENGINE.initialization().completeExceptionally(failed);
    System.err.println("FATAL OpenGL context: "+failed);
    failed.printStackTrace();
   }
  },"beckenburg-opengl");
  gpuWorker=worker;
  worker.setDaemon(false);
  worker.start();
  String driver=ENGINE.initialization().get(30,TimeUnit.SECONDS);
  System.err.println("Beckenburg GPU initialized: "+driver);
  GpuWorldRenderer.install(ENGINE);
  BeckenburgDiagnostics.record("ACTIVE_RENDERER GPU "+driver+
      " / scene triangles OpenGL, UI Swing");
 }
 private static void stopReviewGpu() throws InterruptedException {
  // Dispose resources and destroy GLFW on its owning worker before JVM exit.
  // Abrupt System.exit while that native worker is live crashes this driver.
  if(com.badlogic.gdx.Gdx.app!=null)com.badlogic.gdx.Gdx.app.exit();
  if(gpuWorker!=null){gpuWorker.join(15000);if(gpuWorker.isAlive())throw new IllegalStateException("Review GPU worker did not stop");}
 }
 private static CastleHubMap placeCastleCamera(SceneRenderer renderer){
  CastleHubMap castle=new CastleHubMap();
  // The original smoke camera was inside the courier van's wheel.
  // Require an actual collision-clear position looking toward masonry.
  double[][] locations={{24,22},{24,18},{22,24},{15.5,18},{25,28},{5.5,9}};
  for(double[] spot:locations){
   if(!castle.traversable(spot[0],spot[1]))continue;
   renderer.x=spot[0];renderer.z=spot[1];renderer.yaw=-Math.PI/2;
   renderer.eyeHeight=1.62;
   return castle;
  }
  throw new IllegalStateException("Castle visual proof cannot find an accessible camera");
 }
 private static GpuWorldRenderer.Result capture(SceneRenderer renderer,boolean castle)
    throws Exception{
  int width=640,height=360;
  renderer.setRenderSize(width,height);
  if(castle){
   renderer.drawCastle(placeCastleCamera(renderer),0,true);
  }else{
   var map=new HouseMap(HouseMap.Site.HOUSE);
   renderer.x=map.centerX(4);
   renderer.z=map.centerZ(4);
   renderer.yaw=.2;renderer.eyeHeight=map.floorY(4)+1.62;
   renderer.roomLights=new RoomLighting();
   renderer.drawNetwork(map,List.of(),-1,0);
  }
  long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(25);
  while(System.nanoTime()<deadline){
   var result=ENGINE.latest();
   if(result!=null&&result.frameId()>=renderer.submittedGpuFrameId()
       &&result.width()==width&&result.height()==height){
    int visible=0;
    for(int argb:result.argb())if((argb>>>24)>127)visible++;
    if(visible>400)return result;
   }
   Thread.sleep(40);
  }
  throw new IllegalStateException("OpenGL scene did not produce an actual visible framebuffer");
 }
 private static void screenshot(GpuWorldRenderer.Result frame,String filename)throws Exception {
  BufferedImage image=new BufferedImage(frame.width(),frame.height(),
      BufferedImage.TYPE_INT_ARGB);
  image.setRGB(0,0,frame.width(),frame.height(),frame.argb(),0,frame.width());
  File out=new File("build/e2e/gpu/"+filename);
  out.getParentFile().mkdirs();
  if(!ImageIO.write(image,"png",out))
   throw new IllegalStateException("PNG encoder missing");
  System.out.println("GPU CAPTURE "+out+" ("+out.length()+" bytes)");
 }
 /** Deterministic movement/rotation audit: scene generation is independent
  * from graphics thread, and every requested view must return its own frame ID.
  * Real geometry and lighting are used, not artificial triangle test objects.
  */
 private static void rotationSmoke()throws Exception{
  var house=new HouseMap(HouseMap.Site.HOUSE);
  var renderer=new SceneRenderer();
  renderer.roomLights=new RoomLighting();
  renderer.setRenderSize(640,360);
  renderer.x=house.centerX(4);renderer.z=house.centerZ(4);
  renderer.eyeHeight=house.floorY(4)+1.62;
  long totalNanos=0,maxNanos=0;
  for(int direction=0;direction<6;direction++){
   renderer.yaw=direction*Math.PI/3.0;
   long start=System.nanoTime();
   renderer.drawNetwork(house,List.of(),-1,direction*.5);
   long expected=renderer.submittedGpuFrameId();
   long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(24);
   GpuWorldRenderer.Result completed=null;
   while(System.nanoTime()<deadline){
    var candidate=ENGINE.latest();
    if(candidate!=null&&candidate.frameId()==expected){
     completed=candidate;break;
    }
    Thread.sleep(25);
   }
   if(completed==null)throw new AssertionError(
       "GPU request for house rotation "+direction+" never completed, frame "+expected);
   int visible=0;
   for(int px:completed.argb())if((px>>>24)>127)visible++;
   if(visible<250)throw new AssertionError(
       "House rotation "+direction+" produced empty GPU geometry");
   long elapsed=System.nanoTime()-start;
   totalNanos+=elapsed;maxNanos=Math.max(maxNanos,elapsed);
  }
  System.out.println("GPU ROTATION PASS 6 real house directions, fresh frame identity each; "+
      "average_ms="+totalNanos/6/1_000_000+
      " worst_ms="+maxNanos/1_000_000);
 }
 private static GpuWorldRenderer.Result awaitGpu(SceneRenderer renderer,
      String stage)throws Exception{
  long expected=renderer.submittedGpuFrameId();
  if(expected<=0)throw new AssertionError("No GPU submission: "+stage);
  long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(30);
  while(System.nanoTime()<until){
   var found=ENGINE.latest();
   if(found!=null&&found.frameId()==expected){
    int visible=0;
    for(int rgba:found.argb())if((rgba>>>24)>127)visible++;
    if(visible<250)throw new AssertionError("Empty GPU frame: "+stage+
        " visible="+visible);
    return found;
   }
   Thread.sleep(35);
  }
  throw new AssertionError("Timed out waiting for real GPU frame "+stage);
 }
 /** Actual GPU aerial flight must render on every currently playable site. */
 private static void flyoverSmoke()throws Exception{
  for(var site:new HouseMap.Site[]{
      HouseMap.Site.HOUSE,HouseMap.Site.FARMHOUSE,HouseMap.Site.SCHOOL}){
   var map=new HouseMap(site);
   var spawn=map.van().spawn(0);
   var end=new LevelOpeningFlyover.Pose(spawn.x(),map.van().floorY()+1.62,
       spawn.z(),-.5f,-.09f);
   var flight=new LevelOpeningFlyover(map,end);
   SceneRenderer camera=new SceneRenderer();
   camera.setRenderSize(640,360);
   camera.roomLights=new RoomLighting();
   double[] times=site==HouseMap.Site.HOUSE
       ?new double[]{0,.39,.78,.94}:new double[]{0,.75,.94};
   for(int index=0;index<times.length;index++){
    flight.pose(times[index]).apply(camera);
    camera.drawPractice(map,List.of(),times[index]*7.5);
    var result=awaitGpu(camera,site+" opening "+index);
    screenshot(result,String.format(java.util.Locale.ROOT,
        "level-opening-%s-%02d.png",
        site.name().toLowerCase(java.util.Locale.ROOT),index));
    System.out.println("GPU OPENING PASS "+site+" frame="+index+
        " t="+times[index]+" submitted="+result.frameId());
   }
   if(!flight.pose(1).equals(end))throw new AssertionError(
       "Final aerial frame fails exact Hunter van handoff "+site);
  }
 }
 /**
  * Measure outstanding CPU lighting while a real GPU context is active.
  * Observe and report cost; do NOT claim GPU lighting because raster passes.
  */
 private static void lightingAudit()throws Exception{
  var map=new HouseMap(HouseMap.Site.HOUSE);
  for(int room:new int[]{14,13,12,0,4}){
   var camera=new SceneRenderer();
   camera.setRenderSize(640,360);
   camera.roomLights=new RoomLighting();
   var eye=map.spawn(room);
   var target=map.spawn(room==4?0:room==0?4:0);
   camera.x=eye.x();camera.z=eye.z();
   camera.eyeHeight=eye.y()+1.62;
   camera.yaw=Math.atan2(target.x()-eye.x(),target.z()-eye.z());
   camera.pitch=-.075;
   long started=System.nanoTime();
   camera.drawNetwork(map,List.of(),-1,0);
   var frame=camera.frameDiagnostics();
   if(frame==null||!frame.gpu())throw new AssertionError(
       "GPU not active in lighting benchmark at room "+room);
   var delivered=awaitGpu(camera,"lighting audit room "+room);
   long elapsed=System.nanoTime()-started;
   System.out.printf(java.util.Locale.ROOT,
       "GPU LIGHTING AUDIT room=%d cpu_build_ms=%.2f cpu_gpu_geometry_ms=%.2f "+
       "cpu_shadow_rays=%d cpu_light_samples=%d cpu_normal_rays=%d "+
       "submitted_gpu_triangles=%d end_to_end_ms=%.2f frame_id=%d%n",
       room,frame.sceneBuildNs()/1e6,frame.geometryNs()/1e6,
       frame.lighting().shadowRays(),frame.lighting().samples(),
       frame.lighting().normalRays(),frame.submittedTriangles(),
       elapsed/1e6,delivered.frameId());
  }
  System.out.println("PASS five real GPU viewport lighting audits; remaining CPU cost explicitly reported");
 }
 /**
  * Real world scene, two frames with identical geometry and CPU fixture
  * lighting. Only the GPU flashlight uniform changes. This must update the
  * framebuffer without retransmitting unchanged world VBOs.
  */
 private static void flashlightAndResidentMeshSmoke()throws Exception{
  var map=new HouseMap(HouseMap.Site.HOUSE);
  var eye=map.spawn(0);
  var target=map.spawn(4);
  var camera=new SceneRenderer();
  camera.setRenderSize(640,360);
  camera.roomLights=new RoomLighting();
  camera.x=eye.x();camera.z=eye.z();
  camera.eyeHeight=eye.y()+1.62;
  camera.yaw=Math.atan2(target.x()-eye.x(),target.z()-eye.z());
  camera.pitch=-.07;
  camera.lamp=false;
  camera.drawNetwork(map,List.of(),-1,0);
  var off=awaitGpu(camera,"House with GPU flashlight disabled");
  var firstUpload=ENGINE.uploadStats();
  screenshot(off,"house-torch-off.png");
  camera.lamp=true;
  camera.drawNetwork(map,List.of(),-1,0);
  var on=awaitGpu(camera,"House with GPU flashlight enabled");
  var secondUpload=ENGINE.uploadStats();
  screenshot(on,"house-torch-on.png");
  int changed=0,brighter=0;
  for(int i=0;i<on.argb().length;i++){
   int dark=off.argb()[i],bright=on.argb()[i];
   if((dark>>>24)<128||(bright>>>24)<128)continue;
   if((dark&0xffffff)!=(bright&0xffffff))changed++;
   int dv=((bright>>16)&255)-((dark>>16)&255)
       +((bright>>8)&255)-((dark>>8)&255)
       +(bright&255)-(dark&255);
   if(dv>8)brighter++;
  }
  System.out.println("GPU FLASHLIGHT image_changed_pixels="+changed+
      " brighter_pixels="+brighter+
      " initial_uploaded_bytes="+firstUpload.uploadedBytes()+
      " repeat_uploaded_bytes="+secondUpload.uploadedBytes()+
      " repeat_reused_bytes="+secondUpload.reusedBytes()+
      " repeated_batches="+secondUpload.reusedBatches());
  if(changed<80||brighter<60)throw new AssertionError(
      "GPU flashlight did not visibly illuminate the real House scene");
  if(secondUpload.sceneId()!=firstUpload.sceneId()||
      secondUpload.reusedBatches()==0||secondUpload.reusedBytes()<=0)
   throw new AssertionError(
       "Identical authored House VBOs were unexpectedly reuploaded when only GPU torch changed");
  System.out.println("PASS OpenGL per-fragment flashlight and retained unchanged House VBO uploads");
 }
 private static void smoke()throws Exception{
  SceneRenderer first=new SceneRenderer();
  screenshot(capture(first,false),"investigation-house.png");
  // Wait for a NEW GPU scene, rather than reusing the first capture.
  SceneRenderer second=new SceneRenderer();
  second.setRenderSize(640,360);
  var previous=ENGINE.latest();
  CastleHubMap castleMap=placeCastleCamera(second);
  second.drawCastle(castleMap,0,true);
  long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(25);
  while(System.nanoTime()<deadline&&ENGINE.latest()==previous)
   Thread.sleep(40);
  var castle=ENGINE.latest();
  if(castle==previous||castle==null||
      castle.frameId()<second.submittedGpuFrameId())
   throw new IllegalStateException("Fresh castle geometry did not reach OpenGL");
  int castlePixels=0;
  for(int rgba:castle.argb())if((rgba>>>24)>127)castlePixels++;
  if(castlePixels<8000)
   throw new IllegalStateException("Castle frame does not show enough visible masonry: "+castlePixels);
  screenshot(castle,"castle-headquarters.png");
  rotationSmoke();
  flyoverSmoke();
  lightingAudit();
  flashlightAndResidentMeshSmoke();
  System.out.println("PASS Beckenburg OpenGL 3.3 real House/Castle, flyovers, flashlight and VBO reuse");
 }
 public static void main(String[] args)throws Exception{
  BeckenburgDiagnostics.install();
  var options=java.util.Arrays.asList(args);
  if(options.contains("--software")||options.contains("--diagnostics")
      ||options.contains("--editor")){
   BeckenburgDiagnostics.record("ACTIVE_RENDERER software (explicit alternate entry)");
   InvestigationGame.main(java.util.Arrays.stream(args)
       .filter(a->!a.equals("--software")).toArray(String[]::new));
   return;
  }
  boolean smoke=options.contains("--gpu-smoke");
  boolean uiSmoke=options.contains("--gpu-ui-smoke");
  if(smoke&&uiSmoke)throw new IllegalArgumentException("Choose one GPU smoke mode");
  try{startGpu();}catch(Exception failure){
   BeckenburgDiagnostics.fail("Required GPU renderer initialization",failure);
   throw failure;
  }
  if(options.contains("--water-module-editor")){
   javax.swing.SwingUtilities.invokeLater(()->{var window=org.beckenburg.EnvironmentModuleEditor.open(null,System.getProperty("beckenburg.water.editor.map","hollow-creek"));window.addWindowListener(new java.awt.event.WindowAdapter(){public void windowClosed(java.awt.event.WindowEvent e){if(com.badlogic.gdx.Gdx.app!=null)com.badlogic.gdx.Gdx.app.exit();}});});return;
  }
  if(options.contains("--hollow-creek-review")){try{HollowCreekRuntimeReview.execute(ENGINE);}finally{stopReviewGpu();}return;}
  if(smoke){
   try{
    smoke();
    System.exit(0);
   }catch(Throwable failure){
    System.err.println("GPU SMOKE FAILED: "+failure);
    failure.printStackTrace();
    System.exit(1);
   }
   return;
  }
  if(uiSmoke){
   try{GpuUiSmoke.execute(ENGINE);System.exit(0);}
   catch(Throwable failure){
    System.err.println("GPU UI SMOKE FAILED: "+failure);
    failure.printStackTrace();
    System.exit(1);
   }
   return;
  }
  // Simulation and all Swing menus continue on their tested engine path.
  InvestigationGame.main(args);
 }
}
