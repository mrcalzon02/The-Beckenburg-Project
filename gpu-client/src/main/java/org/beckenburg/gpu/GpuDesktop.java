package org.beckenburg.gpu;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import org.beckenburg.GpuWorldRenderer;
import org.beckenburg.HouseMap;
import org.beckenburg.InvestigationGame;
import org.beckenburg.RoomLighting;
import org.beckenburg.SceneRenderer;
import org.beckenburg.CastleHubMap;
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
 }
 private static GpuWorldRenderer.Result capture(SceneRenderer renderer,boolean castle)
    throws Exception{
  int width=640,height=360;
  renderer.setRenderSize(width,height);
  if(castle){
   renderer.x=14;renderer.z=26;renderer.yaw=-.12;
   renderer.eyeHeight=1.62;
   renderer.drawCastle(new CastleHubMap(),0,true);
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
   if(result!=null&&result.width()==width&&result.height()==height){
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
 private static void smoke()throws Exception{
  SceneRenderer first=new SceneRenderer();
  screenshot(capture(first,false),"investigation-house.png");
  // Wait for a NEW GPU scene, rather than reusing the first capture.
  SceneRenderer second=new SceneRenderer();
  second.setRenderSize(640,360);
  var previous=ENGINE.latest();
  second.x=14;second.z=26;second.eyeHeight=1.62;
  second.drawCastle(new CastleHubMap(),0,true);
  long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(25);
  while(System.nanoTime()<deadline&&ENGINE.latest()==previous)
   Thread.sleep(40);
  var castle=ENGINE.latest();
  if(castle==previous||castle==null)
   throw new IllegalStateException("Castle geometry did not reach OpenGL");
  screenshot(castle,"castle-headquarters.png");
  System.out.println("PASS Beckenburg OpenGL 3.3 real house and real castle framebuffers");
 }
 public static void main(String[] args)throws Exception{
  boolean smoke=java.util.Arrays.asList(args).contains("--gpu-smoke");
  startGpu();
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
  // Simulation and all Swing menus continue on their tested engine path.
  InvestigationGame.main(args);
 }
}
