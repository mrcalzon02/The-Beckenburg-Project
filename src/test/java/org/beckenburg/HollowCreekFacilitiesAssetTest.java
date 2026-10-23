package org.beckenburg;
/** Reusable GLBs must load in the game at native metre scale with floor pivots. */
public final class HollowCreekFacilitiesAssetTest {
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void main(String[] args){
        for(String id:new String[]{"level_camp_picnic_table","level_camp_fire_ring"}){
            check(LevelAssets.approved(id),"approved reusable asset "+id);var model=GlbProps.model(id);
            double lo=999,hi=-999,x0=999,x1=-999;
            for(var t:model.triangles())for(var v:new GlbProps.Vertex[]{t.a(),t.b(),t.c()}){
                check(Double.isFinite(v.x()+v.y()+v.z()+v.u()+v.v()),"finite mesh/UV");
                lo=Math.min(lo,v.y());hi=Math.max(hi,v.y());x0=Math.min(x0,v.x());x1=Math.max(x1,v.x());
            }
            check(Math.abs(lo)<1e-5,"floor pivot "+id);
            if(id.endsWith("picnic_table")){check(Math.abs(hi-.76)<1e-5&&Math.abs(x1-x0-2.44)<1e-5,"human-scale table, not a stretched mesh");check(model.triangles().size()==1136,"reviewed table geometry");}
            else{check(hi>.38&&hi<.41&&x1-x0>1&&x1-x0<1.1,"native fire-ring size");check(model.triangles().size()==1760,"reviewed grate/ring geometry");}
        }
        System.out.println("PASS two reusable campground GLBs, approved IDs, native floor pivots, metre bounds and reviewed geometry");
    }
}
