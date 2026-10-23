package org.beckenburg;
/** Frustum rejection must be conservative: no false omission of near doorway
 * edges, but fully behind-camera props must never load their expensive GLBs.
 */
public final class RendererVisibilityTest {
 private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
 public static void main(String[] ignored){
  SceneRenderer renderer=new SceneRenderer();
  renderer.x=0;renderer.z=0;renderer.yaw=0;renderer.pitch=0;
  check(renderer.objectInView(0,8,1,24),"front room remains in view");
  check(renderer.objectInView(3.0,8,2,24),"doorway on edge remains visible");
  check(renderer.objectInView(0,-.3,2,24),"overlapping near-plane prop remains");
  check(!renderer.objectInView(0,-8,1,24),"entirely behind camera excluded");
  check(!renderer.objectInView(30,10,1,24),"outside horizontal field excluded");
  check(!renderer.objectInView(0,100,1,24),"distant dynamic prop excluded");
  renderer.yaw=Math.PI;
  check(renderer.objectInView(0,-8,1,24),"turning around reveals prior culled room");
  check(!renderer.objectInView(0,8,1,24),"turning around culls the prior front");
  System.out.println("PASS camera-direction culling preserves near apertures and rotation");
 }
}
