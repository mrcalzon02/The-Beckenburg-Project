package org.beckenburg;
/** Native-coordinate ground export must preserve site scale, beds and upward-facing terrain. */
public final class HollowCreekTerrainAssetTest {
 static void check(boolean good,String why){if(!good)throw new AssertionError(why);}
 public static void main(String[] args){
  check(LevelAssets.approved("level_camp_terrain"),"approved terrain export");var model=GlbProps.model("level_camp_terrain");
  check(model.triangles().size()>30000&&model.triangles().size()<50000,"actual native ground triangle count");double x0=999,x1=-999,z0=999,z1=-999,y0=999,y1=-999;int upward=0;
  for(var t:model.triangles()){
   var a=t.a();var b=t.b();var c=t.c();
   double ny=(b.z()-a.z())*(c.x()-a.x())-(b.x()-a.x())*(c.z()-a.z());if(ny>0)upward++;
   for(var v:new GlbProps.Vertex[]{a,b,c}){check(Double.isFinite(v.x()+v.y()+v.z()+v.u()+v.v()),"finite geometry and source UV");x0=Math.min(x0,v.x());x1=Math.max(x1,v.x());z0=Math.min(z0,v.z());z1=Math.max(z1,v.z());y0=Math.min(y0,v.y());y1=Math.max(y1,v.y());}
  }
  check(upward==model.triangles().size(),"terrain faces actual upward side");
  check(Math.abs(x0+4)<1e-5&&Math.abs(x1-64)<1e-5&&Math.abs(z0+16)<1e-5&&Math.abs(z1-90)<1e-5,"world-site origin retained without floor normalization");
  check(y0< -1.7&&y1>.2,"lake/creek beds and graded land exported");
  System.out.println("PASS terrain within unchanged runtime triangle budget, real site scale/origin, upward normals, negative water beds and finite UVs");
 }
}
