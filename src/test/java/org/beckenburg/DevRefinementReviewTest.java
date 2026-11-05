package org.beckenburg;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Source-review contract for the additive map refinement models.
 * Not a gameplay registry, level, renderer or material-art acceptance test.
 */
public final class DevRefinementReviewTest {
    private DevRefinementReviewTest(){}

    public static void main(String[] args) throws Exception {
        List<String> ids=List.of(
            "level_blockout_refined_castle_open_crypt_niche",
            "level_blockout_refined_castle_sarcophagus_bier",
            "level_blockout_refined_castle_candelabrum",
            "level_blockout_refined_house_iron_stove",
            "level_blockout_refined_school_coat_rail",
            "level_blockout_refined_mortuary_instrument_cart",
            "level_blockout_refined_rail_mechanical_semaphore",
            "level_blockout_refined_lighthouse_fresnel_cage",
            "level_blockout_refined_mine_open_ore_cart",
            "level_blockout_refined_sawmill_guarded_blade_table",
            "level_blockout_refined_cannery_suspended_hooks",
            "level_blockout_refined_prison_visitation_grille",
            "level_blockout_refined_camp_timber_washstand",
            "level_blockout_refined_wharf_mooring_bollard",
            "level_blockout_refined_battery_vent_cage");
        var entries=DevAssetReviewCatalog.entries();
        Set<String> expected=new HashSet<>(ids);
        if(expected.size()!=15)throw new AssertionError("Repeated source refinement ID");
        for(int i=1;i<=3;i++)
            expected.add("level_blockout_kit_refined_p0"+i);
        for(String id:expected){
            long count=entries.stream().filter(e->e.id().equals(id)
                &&e.kind()==DevAssetReviewCatalog.Kind.SOURCE_ONLY).count();
            if(count!=1)throw new AssertionError("Source-only showroom registration: "+id+" count "+count);
            if(LevelAssets.approved(id)||GlbProps.availableModels().contains(id))
                throw new AssertionError("Unreviewed source model was promoted to runtime: "+id);
            var glb=DevAssetReviewCatalog.sourceModel(id);
            if(glb.triangles().isEmpty())
                throw new AssertionError("Game GLB decoder found no triangles: "+id);
            var a=DevAssetReviewCatalog.audit(
                new DevAssetReviewCatalog.Entry(DevAssetReviewCatalog.Kind.SOURCE_ONLY,id));
            if(!a.loaded()||!a.state().equals("REVIEW_REGISTRATION"))
                throw new AssertionError("Refined asset must decode and remain SOURCE_ONLY: "
                    +id+" "+a.state()+" "+a.details());
            if(ids.contains(id)&&Math.abs(a.minY())>.025)
                throw new AssertionError("Refined fixture is not ground-aligned: "+id+" "+a.minY());
        }
        System.out.println("PASS: 15 detailed source GLBs and 3 true-scale contact rooms decode "+
            "in the game importer and remain unapproved showroom-only entries");
    }
}
