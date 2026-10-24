package org.beckenburg;
/** Load actual authored exports through the game's unchanged asset parser. */
public final class HollowCreekDockAssetsTest {
 public static void main(String[] args){
  int total=0;
  for(String id:new String[]{"level_camp_portable_toilet_open","level_camp_portable_toilet_closed","level_camp_refuse_bin","level_camp_heron_memorial","level_camp_pavilion_fireplace","level_camp_shrub_decorative_a","level_camp_shrub_decorative_b","level_camp_terrain_amenities","level_camp_creek_engineering","level_camp_dock_shed","level_camp_rowboat","level_camp_folding_chair","level_camp_cooler","level_camp_terrain_shore","level_camp_dock_utilities"}){
   if(!LevelAssets.approved(id))throw new AssertionError("Missing curated approval "+id);
   var model=GlbProps.model(id);int n=model.triangles().size();
   if(n<100||n>=50000)throw new AssertionError("Triangle budget "+id+": "+n);total+=n;
   for(var t:model.triangles())for(var v:new GlbProps.Vertex[]{t.a(),t.b(),t.c()})if(!Double.isFinite(v.x()+v.y()+v.z()+v.u()+v.v()))throw new AssertionError("Nonfinite authored geometry/UV "+id);
  }
  System.out.println("PASS fifteen dock/shore/camp exports: "+total+" triangles, finite native geometry/UVs and unchanged parser budgets");
 }
}
