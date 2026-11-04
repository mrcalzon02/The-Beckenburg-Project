package org.beckenburg;
import java.nio.file.*;
import javax.imageio.ImageIO;
/** Candidate source review through the production GLB/UV/software rasterizer. */
public final class CastleMuseumAssetReview {
 public static void main(String[] args)throws Exception{
  String id=args.length==0?"level_castle_armour_display":args[0];
  var file=Path.of("development/assets/castle-museum/exports/"+id+".glb");
  byte[] bytes=Files.readAllBytes(file);var model=GlbProps.decode(bytes);
  if(model.triangles().size()!=(id.endsWith("_label")?86:id.endsWith("_rope")?264:1036))throw new AssertionError("Measured armour budget changed");
  var folder=Path.of("development/assets/castle-museum/previews/runtime-material");Files.createDirectories(folder);
  var r=new SceneRenderer();r.setRenderSize(800,800);
  for(int i=0;i<4;i++){
   GlbProps.renderNativeMaterialReview(bytes,r,i*Math.PI/2);
   var depthField=SceneRenderer.class.getDeclaredField("depths");depthField.setAccessible(true);double[] depths=(double[])depthField.get(r);int finite=0;int holes=0;for(int y=345;y<380;y++)for(int x=390;x<410;x++){
    int expected=((x/16+y/16)&1)==0?0x41474f:0x343a42;
    if((r.image().getRGB(x,y)&0xffffff)==expected){holes++;if(Double.isFinite(depths[y*800+x]))finite++;}
   }
   System.out.println("view="+i+" backgroundInsideCentralTorso="+holes+" coveredColorMatches="+finite+" preset="+r.psxPreset());
   ImageIO.write(r.image(),"png",folder.resolve(id+"-source-runtime-"+i+".png").toFile());
  }
  System.out.println("PASS actual source "+id+" / "+model.triangles().size()+" triangles; four production material/UV rasterizer views, no runtime approval or placement implied");
 }
}
