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
  var f=new GpuWorldRenderer.Frame(640,360,List.of(triangle));
  check(f.surfaces().size()==1&&f.width()==640,"Frame preserves real geometry and display size");
  denied=false;
  try{new GpuWorldRenderer.Result(640,360,new int[100],new float[100]);}
  catch(IllegalArgumentException expected){denied=true;}
  check(denied,"GPU readbacks must match actual framebuffer dimensions");
  System.out.println("PASS standalone renderer bridge contract");
 }
}
