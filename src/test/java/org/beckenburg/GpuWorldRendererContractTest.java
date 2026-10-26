package org.beckenburg;
import java.util.List;
/** Headless contract test: no OpenGL test doubles masquerade as a GPU context. */
public final class GpuWorldRendererContractTest {
 private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
 public static void main(String[] ignored){
  check(GpuWorldRenderer.active()==null,"Plain Java runtime does not falsely claim a GPU");
  boolean denied=false;
  try{new GpuWorldRenderer.Surface(1,new float[9]);}catch(IllegalArgumentException expected){denied=true;}
  check(denied,"GPU vertices must contain complete triangle groups");
  var triangle=new GpuWorldRenderer.Surface(-1,new float[24]);
  var f=new GpuWorldRenderer.Frame(9,10,640,360,275.4f,1f,.7f,List.of(triangle));
  check(f.surfaces().size()==1&&f.width()==640,"Frame preserves real geometry and display size");
  check(!f.worldSpace(),"Legacy callers still receive camera-space geometry");
  var world=new GpuWorldRenderer.Frame(9,11,640,360,275.4f,1f,.7f,
      List.of(triangle),true,13f,2.5f,44f,-.4f,-.15f);
  check(world.worldSpace()&&world.cameraX()==13f&&world.cameraY()==2.5f
          &&world.cameraZ()==44f&&world.yaw()==-.4f&&world.pitch()==-.15f,
      "World-space vertices and explicit GPU camera uniforms must survive submit contract");
  var lit=new GpuWorldRenderer.Frame(9,12,640,360,275.4f,1f,.7f,
      List.of(triangle),true,13f,2.5f,44f,-.4f,-.15f,
      true,.72f,7.5f,.44f,false);
  check(lit.lampOn()&&lit.lampIntensity()==.72f&&lit.lampReach()==7.5f
      &&lit.lampWidth()==.44f&&!lit.flameLamp(),
      "GPU flashlight settings survive the world-space scene bridge");
  check(!world.lampOn()&&!f.lampOn(),
      "Legacy GPU calls must not invent flashlight illumination");
  denied=false;
  try{new GpuWorldRenderer.Frame(9,11,640,360,275.4f,1f,.7f,
      List.of(triangle),true,Float.NaN,0,0,0,0);}
  catch(IllegalArgumentException expected){denied=true;}
  check(denied,"GPU world camera may not carry non-finite positions");
  denied=false;
  try{new GpuWorldRenderer.Result(9,10,640,360,new int[100],new float[100]);}
  catch(IllegalArgumentException expected){denied=true;}
  check(denied,"GPU readbacks must match actual framebuffer dimensions");
  var result=new GpuWorldRenderer.Result(9,10,640,360,new int[640*360],new float[640*360]);
  check(GpuWorldRenderer.belongsTo(result,9,640,360),
      "GPU result binds to its actual scene identity");
  check(!GpuWorldRenderer.belongsTo(result,11,640,360),
      "an old house framebuffer must never paint the new castle HUD");
  check(!GpuWorldRenderer.belongsTo(result,9,320,200),
      "a resized GPU readback must never overwrite the new viewport");
  denied=false;
  try{new GpuWorldRenderer.Frame(0,10,640,360,275.4f,1f,.7f,List.of(triangle));}
  catch(IllegalArgumentException expected){denied=true;}
  check(denied,"invalid scene identities cannot be submitted");
  System.out.println("PASS GPU generation identity, viewport isolation and frame bridge");
 }
}
