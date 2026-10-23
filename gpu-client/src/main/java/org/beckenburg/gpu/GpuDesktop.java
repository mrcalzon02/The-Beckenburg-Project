package org.beckenburg.gpu;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import org.beckenburg.GpuWorldRenderer;
import org.beckenburg.HouseMap;
import org.beckenburg.InvestigationGame;
import org.beckenburg.RoomLighting;
import org.beckenburg.SceneRenderer;
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
  worker.setDaemon(false);
  worker.start();
  String driver=ENGINE.initialization().get(30,TimeUnit.SECONDS);
  System.err.println("Beckenburg GPU initialized: "+driver);
  GpuWorldRenderer.install(ENGINE);
  BeckenburgDiagnostics.record("ACTIVE_RENDERER GPU "+driver+
      " / scene triangles OpenGL, UI Swing");
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
  System.out.println("PASS Beckenburg OpenGL 3.3 real house and real castle framebuffers");
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
