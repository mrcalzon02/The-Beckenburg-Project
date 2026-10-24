package org.beckenburg;
/** Actual world-coordinate exports must load without changing engine budgets. */
public final class HollowCreekBoundaryAssetsTest {
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 public static void main(String[] args){
  int total=0;
  for(String id:new String[]{"level_camp_pine_refined_a","level_camp_pine_refined_b","level_camp_fern_refined","level_camp_terrain_organic","level_camp_boundary_banks","level_camp_boundary_logfall","level_camp_boundary_fence","level_camp_boundary_gate"}){
   check(LevelAssets.approved(id),"curated asset "+id);var mesh=GlbProps.model(id);int count=mesh.triangles().size();check(count>20&&count<50000,"native triangle budget "+id);total+=count;
   double x0=999,x1=-999,z0=999,z1=-999,y0=999;
   for(var t:mesh.triangles())for(var v:new GlbProps.Vertex[]{t.a(),t.b(),t.c()}){
    check(Double.isFinite(v.x()+v.y()+v.z()+v.u()+v.v()),"finite native geometry/UV "+id);x0=Math.min(x0,v.x());x1=Math.max(x1,v.x());z0=Math.min(z0,v.z());z1=Math.max(z1,v.z());y0=Math.min(y0,v.y());
   }
   if(id.contains("pine_refined")){check(x1-x0>2&&x1-x0<6&&z1-z0>2&&z1-z0<6&&Math.abs(y0)<.01,"refined pine local floor pivot/metres");}
   else if(id.endsWith("fern_refined")){check(x1-x0>.8&&x1-x0<2&&z1-z0>.8&&z1-z0<2&&y0>=0&&y0<.05,"fern native bounds/floor pivot");}
   else if(id.endsWith("gate")){check(x0>=27.8&&x1<=36.2&&z0>84.7&&z1<85.3,"gate beyond canonical van at authored roadhead");check(y0>1,"gate fitted to elevated ground");}
   else if(id.endsWith("fence")){check(x0>23&&x1<41&&z0>=73.8&&z1<85.3,"service returns follow narrowed arrival neck");}
   else {check(x1-x0>25&&z1-z0>25,"world-scale/site origin retained "+id);}
   if(id.endsWith("organic"))check(y0< -3,"carved lake bed retained");
  }
  System.out.println("PASS eight actual Hollow Creek GLBs: "+total+" triangles, finite UVs, native site coordinates and unchanged per-model budgets");
 }
}
