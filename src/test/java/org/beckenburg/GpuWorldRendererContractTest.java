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
