package org.beckenburg;

import java.util.Set;

/** Curated architectural scenery, separate from owned investigation equipment. */
public final class LevelAssets {
    private LevelAssets(){}
    public static final Set<String> AUTHORED_SURFACES=Set.of(
        "level_camp_dock_shed_authored","level_camp_rowboat_authored","level_camp_folding_chair_authored",
        "level_camp_cooler_authored","level_camp_portable_toilet_open_authored","level_camp_portable_toilet_closed_authored",
        "level_camp_refuse_bin_authored","level_camp_heron_memorial_authored","level_camp_pavilion_fireplace_authored",
        "level_camp_bedroll_rolled_authored","level_trace_generator_authored","level_camp_pavilion_authored",
        "level_camp_picnic_table_authored","level_camp_fire_ring_authored");
    public static final Set<String> MODELS=Set.of(
        "bed_4","bed_5","bed_1","bed_2","bedside_table_1","toilet_mp_1","carpet_mp_1",
        "photo_frame_mp_1","painting_1","clock_1","wooden_crate_1","pipe_1","book_mp_2","plate_mp_1",
        "lamp_1_on","lamp_1_off","lamp_2_on","lamp_2_off","lamp_5_on","lamp_5_off",
        "ceiling_lamp_2_on","ceiling_lamp_2_off","ceiling_lamp_mp_1_on","ceiling_lamp_mp_1_off",
        "level_kitchen_counter","level_bath_basin","level_bathtub","level_domestic_light",
        "level_wall_sconce","level_utility_light","level_fireplace","level_bedding_double","level_bedding_single","level_table_lamp",
        "level_camp_dock_shed_refined","level_camp_dock_utilities_refined","level_camp_rowboat_refined","level_camp_folding_chair_refined","level_camp_cooler_refined","level_camp_portable_toilet_open_refined","level_camp_portable_toilet_closed_refined","level_camp_refuse_bin_refined","level_camp_heron_memorial_refined","level_camp_pavilion_fireplace_refined","level_camp_shrub_decorative_a_refined","level_camp_shrub_decorative_b_refined","level_camp_creek_engineering_refined","level_camp_tent_ridge_refined","level_camp_tent_dome_refined","level_camp_bedroll_rolled_refined","level_trace_generator_refined","level_camp_pavilion_refined","level_camp_picnic_table_refined","level_camp_fire_ring_refined","level_camp_portable_toilet_open","level_camp_portable_toilet_closed","level_camp_refuse_bin","level_camp_heron_memorial","level_camp_pavilion_fireplace","level_camp_shrub_decorative_a","level_camp_shrub_decorative_b","level_camp_terrain_amenities","level_camp_creek_engineering","level_camp_dock_shed","level_camp_rowboat","level_camp_folding_chair","level_camp_cooler","level_camp_terrain_shore","level_camp_dock_utilities","level_camp_tent_ridge","level_camp_tent_dome","level_camp_bedroll_rolled","level_camp_pine_refined_a","level_camp_pine_refined_b","level_camp_fern_refined","level_camp_terrain_organic","level_camp_boundary_banks","level_camp_boundary_logfall","level_camp_boundary_fence","level_camp_boundary_gate","level_camp_terrain","level_camp_pavilion","level_camp_picnic_table","level_camp_fire_ring","level_mill_suspension","level_shrub_refuge","level_hay_bale","level_stall","level_corn","level_blackboard","level_student_desk","level_lockers","level_basketball_hoop");
    public static boolean approved(String id){return id.equals("level_npc_matthis_rook_proxy")||AUTHORED_SURFACES.contains(id)||MODELS.contains(id)||TerrainFoundationAssets.approved(id)||BlackpineDressing.ASSETS.contains(id)||BlackpineStoryDressing.ASSETS.contains(id)||BlackpineProductionAssets.ASSETS.contains(id)||LivedAssets.approved(id);}
}
