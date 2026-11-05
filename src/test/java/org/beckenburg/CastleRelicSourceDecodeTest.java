package org.beckenburg;

import java.util.HashSet;
import java.util.Set;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Independent native glTF decoder contract for source-only Castle relic art.
 * Never changes the approved production registry or gameplay save.
 */
public final class CastleRelicSourceDecodeTest {
    private CastleRelicSourceDecodeTest(){}
    private static final String[] ARTIFACTS={
        "level_blockout_relic_mourning_locket",
        "level_blockout_relic_votive_bell",
        "level_blockout_relic_bronze_astrolabe",
        "level_blockout_relic_sealed_urn",
        "level_blockout_relic_black_mirror",
        "level_blockout_relic_bound_folio",
        "level_blockout_relic_reliquary_box",
        "level_blockout_relic_pilgrim_lantern",
        "level_blockout_relic_ossuary_keys",
        "level_blockout_relic_censer",
        "level_blockout_relic_burial_mask",
        "level_blockout_relic_pilgrim_chalice"
    };
    private static final String[] SUPPORTS={
        "level_blockout_relic_containment",
        "level_blockout_relic_padded_tray",
        "level_blockout_relic_locking_showcase",
        "level_blockout_relic_barred_shelf",
        "level_blockout_relic_inventory_placard",
        "level_blockout_relic_repair_stand",
        "level_blockout_relic_open_guard"
    };
    private static final String[] CRYPTS={
        "level_blockout_sarcophagus_plain",
        "level_blockout_sarcophagus_inscribed",
        "level_blockout_sarcophagus_effigy"
    };
    private static void check(boolean condition,String message){
        if(!condition)throw new AssertionError(message);
    }
    public static void main(String[] ignored)throws Exception {
        var source=Path.of("development","levels","primitive-props-v1","models");
        var catalog=DevAssetReviewCatalog.entries();
        Set<String> ids=new HashSet<>();
        int triangles=0,materials=0,readable=0;
        for(var collection:new String[][]{ARTIFACTS,SUPPORTS,CRYPTS})
            for(String id:collection){
                check(ids.add(id),"Duplicate physical relic/case/crypt mesh "+id);
                check(Files.isRegularFile(source.resolve(id+".glb")),
                    "Individual model was not generated "+id);
                check(!LevelAssets.approved(id)&&!GlbProps.availableModels().contains(id),
                    "Unapproved relic/crypt art leaked into production registry "+id);
                var entry=new DevAssetReviewCatalog.Entry(DevAssetReviewCatalog.Kind.SOURCE_ONLY,id);
                check(catalog.contains(entry),"Source-only asset invisible in DEV room "+id);
                var model=DevAssetReviewCatalog.sourceModel(id);
                var audit=DevAssetReviewCatalog.audit(entry);
                check(audit.loaded()&&audit.triangles()>24&&audit.materials()>=2,
                    "Model failed actual native GLB/PNG decode "+id+" "+audit);
                check(audit.textureSamples()>0,"Actual color sampler couldn't load "+id);
                check(Double.isFinite(audit.minY())&&Double.isFinite(audit.maxY())
                    &&audit.maxY()>audit.minY()&&audit.minY()>=-.001,
                    "Off-ground/reversed model bounds "+id);
                int withArea=0;
                for(var tri:model.triangles()){
                    var a=tri.a();var b=tri.b();var c=tri.c();
                    double uvArea=(b.u()-a.u())*(c.v()-a.v())-
                                  (c.u()-a.u())*(b.v()-a.v());
                    if(Double.isFinite(uvArea)&&Math.abs(uvArea)>1e-8)withArea++;
                }
                check(withArea>=model.triangles().size()*.95,
                    "Most texture UV triangles are still flat/degenerate "+id+
                    " "+withArea+"/"+model.triangles().size());
                triangles+=audit.triangles();materials+=audit.materials();
                readable++;
            }
        check(ARTIFACTS.length==12&&SUPPORTS.length==7&&CRYPTS.length==3,
            "Expected 12 distinct relic objects, 7 cases/fixtures and 3 sarcophagi");
        check(ids.size()==22,"Missing physical GLBs in vault/crypt family");
        System.out.println("PASS actual game GLB decoder: "+readable+
            " source-only relic, support and sarcophagus meshes; "+
            triangles+" triangles, "+materials+" material bindings, real UV0 and PNG sampling.");
    }
}
