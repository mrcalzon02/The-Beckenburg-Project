package org.beckenburg;
import java.nio.file.*;
/** Reproduce misleading inspection domains without changing the production renderer. */
public final class ProbeDomainControl {
 public static void main(String[] args)throws Exception{
  System.setProperty("user.home",Files.createTempDirectory("museum-probe-control-").toString());
  var scene=CastleLevelEdits.current();double x=31.5,z=5.5;
  boolean nativeOnly=scene.floorSupports(x,z,0),shared=CastleRooms.supported(CastleRooms.slabCells(CastleRooms.structuralDecks()),x,z,0);
  if(nativeOnly||!shared)throw new AssertionError("Expected actual archive floor to belong to shared structural authority");
  var renderer=new SceneRenderer();renderer.setRenderSize(800,800);
  GlbProps.renderNativeMaterialReview(Files.readAllBytes(Path.of("assets/runtime/models/level_castle_armour_display.glb")),renderer,Math.PI);
  var f=SceneRenderer.class.getDeclaredField("depths");f.setAccessible(true);double[] depth=(double[])f.get(renderer);int overbroad=0,torso=0;
  for(int y=345;y<400;y++)for(int q=390;q<410;q++)if(!Double.isFinite(depth[y*800+q])){overbroad++;if(y<380)torso++;}
  if(overbroad==0||torso!=0)throw new AssertionError("Control must distinguish intentional thigh-guard gap from closed torso");
  System.out.println("PASS diagnostic domain controls: native-only-floor="+nativeOnly+" shared-actual-floor="+shared+"; overbroad ROI includes "+overbroad+" intentional-gap pixels, true torso has "+torso+" uncovered pixels. No production renderer change.");
 }
}
