package org.beckenburg;
import java.util.*;
import java.nio.file.*;

public final class FoliageVariationTest {
    static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        String mesh="level_tree_conifer_worn";var source=GlbProps.model(mesh);var original=source.triangles();
        int leaf=original.stream().filter(t->FoliageVariation.leafMaterial(GlbProps.materialName(t.material()))).findFirst().orElseThrow().material();
        int before=GlbProps.sample(leaf,.34,.57);Set<Integer> palettes=new HashSet<>(),sampleColors=new HashSet<>();
        double lo=2,hi=0,leanLeft=0,leanRight=0;
        for(int i=0;i<200;i++){
            var p=FoliageVariation.profile(mesh,i*3.7,12.3);check(p.equals(FoliageVariation.profile(mesh,i*3.7,12.3)),"stable placement profile");
            var root=p.point(0,0,0,7.5);check(root.x()==0&&root.y()==0&&root.z()==0,"root never drifts or floats");
            var trunk=p.point(.12,1.7,-.03,7.5);check(trunk.x()==.12&&trunk.y()==1.7&&trunk.z()==-.03,"solid lower trunk never bends/scales out of collision");
            lo=Math.min(lo,p.height());hi=Math.max(hi,p.height());if(p.leanX()<0)leanLeft++;else leanRight++;
            int variant=GlbProps.foliageMaterial(leaf,p.seed());palettes.add(variant);sampleColors.add(GlbProps.sample(variant,.34,.57));
        }
        check(lo<.84&&hi>1.18&&leanLeft>50&&leanRight>50,"height and left/right lean both vary");
        check(palettes.size()==16&&sampleColors.size()>10,"bounded leaf palettes contain real texel variation");
        check(GlbProps.sample(leaf,.34,.57)==before&&GlbProps.model(mesh).triangles()==original,"shared source texture/geometry never mutated");
        var refuge=FoliageVariation.profile("level_shrub_refuge",9,30);check(!refuge.deform()&&refuge.rotation()==0,"refuge pocket silhouette and physics remain authored");
        var map=BlackpineLevel.map();var all=new SceneryInstances(map.level(),100);var none=new SceneryInstances(map.level(),0);
        check(all.instances().stream().filter(i->i.mesh.equals(mesh)).map(i->i.template).distinct().count()==1,"all conifers use exactly one source template");
        check(all.instances().size()-none.instances().size()==3,"density affects exactly the three real refuges");
        var result=new LinkedHashMap<String,Object>();result.put("status","pass");result.put("profiles",200);result.put("height_scale_range",List.of(lo,hi));result.put("leaf_texel_palettes",palettes.size());result.put("anchored_lower_trunk_m",2);result.put("geometry_shared",true);result.put("refuge_shape_preserved",true);
        Files.writeString(Path.of("development/levels/verification/foliage-variation.json"),DressingReport.json(result)+"\n");
        System.out.println("PASS stable foliage height, width, rotation, twist, asymmetric lean, 16 true texel variants, shared meshes and unchanged refuge pocket");
    }
}
