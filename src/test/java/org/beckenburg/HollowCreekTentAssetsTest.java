package org.beckenburg;
/** Real campsite exports: retained metre pivots, finite UVs and bounded asset costs. */
public final class HollowCreekTentAssetsTest {
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 public static void main(String[] args){
  int total=0;
  for(String id:new String[]{"level_camp_tent_ridge","level_camp_tent_dome","level_camp_bedroll_rolled"}){
   check(LevelAssets.approved(id),"curated original campsite asset "+id);var model=GlbProps.model(id);int n=model.triangles().size();check(n>100&&n<50000,"unchanged triangle budget "+id);total+=n;
   double x0=999,x1=-999,y0=999,y1=-999,z0=999,z1=-999;
   for(var t:model.triangles())for(var p:new GlbProps.Vertex[]{t.a(),t.b(),t.c()}){
    check(Double.isFinite(p.x()+p.y()+p.z()+p.u()+p.v()),"finite geometry and original UVs "+id);x0=Math.min(x0,p.x());x1=Math.max(x1,p.x());y0=Math.min(y0,p.y());y1=Math.max(y1,p.y());z0=Math.min(z0,p.z());z1=Math.max(z1,p.z());
   }
   check(Math.abs((x0+x1)/2)<.06,"native lateral floor pivot "+id);
   if(id.endsWith("dome"))check(z0> -2&&z0< -1.9&&z1>1.5&&z1<1.56,"actual asymmetric guy/canopy bounds; no fake recentering");
   else check(Math.abs((z0+z1)/2)<.06,"authored local floor origin "+id);
   if(id.endsWith("rolled")){check(x1-x0>.6&&x1-x0<.75&&z1-z0>.3&&z1-z0<.4,"human-scale rolled sleeping bag");check(y0>=-.005&&y1<.4,"roll is supported at floor pivot");}
   else{check(x1-x0>2.7&&x1-x0<4.2&&z1-z0>3.3&&z1-z0<4.4,"two-person tent plus actual guy footprint");check(y0>=-.12&&y0<0&&y1>1.6&&y1<2,"ground pegs and tent head height remain in metres");}
  }
  System.out.println("PASS three original campsite GLBs: "+total+" triangles, human-scale native pivots, intentional ground pegs and finite UVs");
 }
}
