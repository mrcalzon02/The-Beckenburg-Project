package org.beckenburg;

import java.nio.file.*;
import java.util.*;
import java.awt.image.BufferedImage;
import org.beckenburg.editor.*;

/** One immutable source bitmap can back distinct real GLB materials without merging behavior. */
public final class SourceImageSharingTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    @SuppressWarnings("unchecked") private static BufferedImage image(int material)throws Exception{
        var field=GlbProps.class.getDeclaredField("textures");field.setAccessible(true);
        return ((List<BufferedImage>)field.get(null)).get(material-GlbProps.TEXTURE_BASE);
    }
    public static void main(String[] args)throws Exception{
        Path home=Files.createTempDirectory("shared-source-images-");
        try{
            byte[] png=org.beckenburg.editor.EditorMeshMaterialTest.png();
            var one=new EditorMeshMaterial("repeat","Repeat source",png,new EditorMeshMaterial.Color(1,1,1,1),10497,10497,EditorMeshMaterial.Alpha.BLEND,.5,EditorMeshMaterial.Origin.TOP_LEFT,Map.of());
            var two=new EditorMeshMaterial("clamp","Clamp tinted source",png,new EditorMeshMaterial.Color(.5,.25,1,.5),33071,33071,EditorMeshMaterial.Alpha.MASK,.7,EditorMeshMaterial.Origin.TOP_LEFT,Map.of());
            String obj="v 0 0 0\nv 1 0 0\nv 0 1 0\nv 1 1 0\nvt 0 0\nvt 1 0\nvt 0 1\nf 1/1 2/2 3/3\nf 3/1 2/2 4/3\n";
            var mesh=EditorMeshPipeline.read("shared.obj",obj.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            var project=EditorTopologyProject.fromMesh(mesh,"a".repeat(64),home.resolve("shared.btop"));
            project.applyPreview(project.previewBindMaterial(0,one));project.applyPreview(project.previewBindMaterial(1,two));
            var decoded=GlbProps.decode(EditorGlbWriter.encode(project.mesh()));
            int first=decoded.triangles().get(0).material(),second=decoded.triangles().get(1).material();
            check(first!=second&&image(first)==image(second),"Distinct native materials must share only their immutable source bitmap");
            check(GlbProps.sample(first,1.25,.25)==0xff0000&&GlbProps.sample(second,1.25,.25)==0x004000,"Repeat/clamp and per-material tint remain independent");
            check(GlbProps.sampleAlpha(first,1.25,.25)==255&&GlbProps.sampleAlpha(second,1.25,.25)==64,"Original alpha and material opacity remain independent");
            check(GlbProps.gltfBlended(first)&&GlbProps.gltfMaskCutoff(second)==.7,"BLEND/MASK metadata is not merged");
            var expected=javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(png));
            for(int y=0;y<4;y++)for(int x=0;x<4;x++)check(image(first).getRGB(x,y)==expected.getRGB(x,y),"Every original RGBA texel retained");
            var genuine=GlbProps.model("level_house_fridge_worn");int nativeMat=genuine.triangles().get(0).material();
            var imported=GlbProps.decode(Files.readAllBytes(Path.of("assets/runtime/models/level_house_fridge_worn.glb")));int importedMat=imported.triangles().get(0).material();
            check(image(nativeMat)==image(importedMat),"Actual native1254pixel fridge atlas is not decoded twice");
            var atlas=javax.imageio.ImageIO.read(Path.of("development/levels/model-production/house-fridge/atlas-v1.png").toFile());
            for(int y=0;y<atlas.getHeight();y++)for(int x=0;x<atlas.getWidth();x++)check(image(nativeMat).getRGB(x,y)==atlas.getRGB(x,y),"Original fridge atlas RGBA and resolution unchanged");
            int live=GlbProps.liveSurfaceMaterial("van live screen sharing-test",64,64);GlbProps.updateLiveSurface(live,new BufferedImage(64,64,BufferedImage.TYPE_INT_RGB));
            check(GlbProps.sample(first,1.25,.25)==0xff0000,"Updating a live monitor cannot mutate shared source artwork");
            System.out.println("PASS shared immutable source PNGs, distinct material IDs/tint/wrap/alpha modes, exact full atlas RGBA/resolution and independent live surfaces");
        }finally{try(var paths=Files.walk(home)){for(Path p:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
    }
}
