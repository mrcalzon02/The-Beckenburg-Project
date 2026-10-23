package org.beckenburg;
import java.awt.image.BufferedImage;
/** Generated terrain/foliage slots must retain current-main UV metadata alignment. */
public final class MergedMaterialMetadataTest {
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void main(String[] args){
        var pixels=new BufferedImage(4,4,BufferedImage.TYPE_INT_RGB);
        for(int y=0;y<4;y++)for(int x=0;x<4;x++)pixels.setRGB(x,y,y==0?0xff0000:0x00ff00);
        int ground=GlbProps.surfaceMaterial("merge-metadata-probe",pixels);
        check(GlbProps.sample(ground,.1,.99)==0xff0000,"terrain slot has explicit source UV orientation");
        int leaf=GlbProps.foliageMaterial(ground,19);
        check(GlbProps.sample(leaf,.1,.99)!=GlbProps.sample(leaf,.1,.01),"foliage slot samples inherited UV orientation");
        check(GlbProps.availableModels().contains("level_work_circular_headrig"),"merged models remain available in live editor");
        var model=GlbProps.model("level_work_circular_headrig");
        for(var triangle:model.triangles())GlbProps.sample(triangle.material(),triangle.a().u(),triangle.a().v());
        int television=GlbProps.televisionMaterial(0,0,0);GlbProps.sample(television,.5,.5);
        check(GlbProps.surfaceMaterial("merge-metadata-probe",pixels)==ground,"surface material identity is stable");
        System.out.println("PASS generated terrain/foliage, subsequent GLB and TV material metadata remain aligned");
    }
}
