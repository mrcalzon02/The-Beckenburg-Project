package org.beckenburg;
/** Actual second-pass GLBs through the unchanged runtime parser. */
public final class HollowCreekRefinementAssetsTest {
 public static void main(String[] args){int total=0;
  for(String id:new String[]{"level_camp_dock_shed_refined","level_camp_dock_utilities_refined","level_camp_rowboat_refined","level_camp_folding_chair_refined","level_camp_cooler_refined","level_camp_portable_toilet_open_refined","level_camp_portable_toilet_closed_refined","level_camp_refuse_bin_refined","level_camp_heron_memorial_refined","level_camp_pavilion_fireplace_refined","level_camp_shrub_decorative_a_refined","level_camp_shrub_decorative_b_refined","level_camp_creek_engineering_refined","level_camp_tent_ridge_refined","level_camp_tent_dome_refined","level_camp_bedroll_rolled_refined","level_trace_generator_refined","level_camp_pavilion_refined","level_camp_picnic_table_refined","level_camp_fire_ring_refined"}){
   if(!LevelAssets.approved(id))throw new AssertionError("Missing curated refinement "+id);
   var model=GlbProps.model(id);int n=model.triangles().size();if(n<100||n>=50000)throw new AssertionError("Unchanged triangle cap "+id+" "+n);total+=n;
   for(var t:model.triangles())for(var v:new GlbProps.Vertex[]{t.a(),t.b(),t.c()})if(!Double.isFinite(v.x()+v.y()+v.z()+v.u()+v.v()))throw new AssertionError("Nonfinite geometry/UV "+id);
  }
  System.out.println("PASS20 second-pass GLBs: "+total+" triangles, finite geometry/UVs, unchanged parser and per-asset budgets");
 }
}
