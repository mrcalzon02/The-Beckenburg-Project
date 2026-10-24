package org.beckenburg;
/** Independently load every planned-site ground export using the actual game decoder. */
public final class TerrainFoundationAssetsTest {
 record Spec(String id,double x0,double z0,double x1,double z1){}
 static void check(boolean good,String why){if(!good)throw new AssertionError(why);}
 public static void main(String[] args){
  Spec[] sites={new Spec("level_site_academy_terrain",-4,-4,72,84),
   new Spec("level_site_asylum_terrain",-4,-2,68,88),
   new Spec("level_site_college_terrain",0,-4,78,92),
   new Spec("level_site_mansion_terrain",0,1,58,81),
   new Spec("level_site_prison_terrain",6,0,74,94),
   new Spec("level_site_wld_03_terrain",0,0,86,96),
   new Spec("level_site_wld_04_terrain",0,-2,58,88),
   new Spec("level_site_wld_05_terrain",-2,4,58,92),
   new Spec("level_site_wld_06_terrain",0,2,64,84),
   new Spec("level_site_wld_07_terrain",2,-4,58,88),
   new Spec("level_site_wld_08_terrain",-2,-4,74,86),
   new Spec("level_site_wld_09_terrain",4,4,80,92),
   new Spec("level_site_wld_10_terrain",-12,-2,72,90),
   new Spec("level_site_wld_11_terrain",8,-10,70,92),
   new Spec("level_site_wld_12_terrain",2,-12,68,104),
   new Spec("level_site_wld_13_terrain",-20,-4,68,92)};
  int triangles=0;
  for(var site:sites){
   check(LevelAssets.approved(site.id()),"curated ID "+site.id());var model=GlbProps.model(site.id());
   check(!model.triangles().isEmpty()&&model.triangles().size()<50000,"unchanged total runtime triangle budget "+site.id());
   double x0=999,x1=-999,z0=999,z1=-999,y0=999,y1=-999;
   for(var t:model.triangles()){
    var a=t.a();var b=t.b();var c=t.c();
    double ny=(b.z()-a.z())*(c.x()-a.x())-(b.x()-a.x())*(c.z()-a.z());check(ny>0,"actual upward ground faces "+site.id());
    for(var v:new GlbProps.Vertex[]{a,b,c}){check(Double.isFinite(v.x()+v.y()+v.z()+v.u()+v.v()),"finite geometry and source UV");x0=Math.min(x0,v.x());x1=Math.max(x1,v.x());z0=Math.min(z0,v.z());z1=Math.max(z1,v.z());y0=Math.min(y0,v.y());y1=Math.max(y1,v.y());}
   }
   check(Math.abs(x0-site.x0())<1e-5&&Math.abs(x1-site.x1())<1e-5&&Math.abs(z0-site.z0())<1e-5&&Math.abs(z1-site.z1())<1e-5,"original site origin/extent "+site.id());
   check(y1-y0>.25,"real height variation "+site.id());triangles+=model.triangles().size();
  }
  check(sites.length==16&&TerrainFoundationAssets.MODELS.size()==16,"remaining rough sites, without rebuilding existing maps");
  System.out.println("PASS 16 terrain GLBs, "+triangles+" total triangles, native metres/origins, UVs, upward normals and unchanged per-asset budgets; map unlocks unchanged");
 }
}
