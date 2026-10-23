package org.beckenburg;
/** Actual runtime GLB topology, depth and native metres, independent of authoring metadata. */
public final class HollowCreekPavilionAssetTest {
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void main(String[] args){
        check(LevelAssets.approved("level_camp_pavilion"),"approved architecture kit");
        var model=GlbProps.model("level_camp_pavilion");check(model.triangles().size()==2040,"reviewed source triangle count");
        double min=999,max=-999;int soffit=0,roof=0;double undersideArea=0;
        for(var t:model.triangles()){
            var a=t.a();var b=t.b();var c=t.c();
            for(var v:new GlbProps.Vertex[]{a,b,c}){check(Double.isFinite(v.x()+v.y()+v.z()+v.u()+v.v()),"finite metre geometry and UV");min=Math.min(min,v.y());max=Math.max(max,v.y());}
            double nx=(b.y()-a.y())*(c.z()-a.z())-(b.z()-a.z())*(c.y()-a.y());
            double ny=(b.z()-a.z())*(c.x()-a.x())-(b.x()-a.x())*(c.z()-a.z());
            double nz=(b.x()-a.x())*(c.y()-a.y())-(b.y()-a.y())*(c.x()-a.x());
            double mid=(a.y()+b.y()+c.y())/3;
            if(mid>3.5&&Math.abs(ny)>30){if(ny<0){soffit++;undersideArea+=Math.sqrt(nx*nx+ny*ny+nz*nz)/2;}else roof++;}
        }
        check(Math.abs(min)<1e-6&&max>5.6&&max<5.8,"floor pivot and full-scale pitched roof");
        check(soffit>=4&&roof>=4&&undersideArea>190,"actual downward soffit and upper roof faces exported");
        System.out.println("PASS native-scale pavilion GLB, 2040 triangles, UVs, floor pivot and genuine downward roof underside");
    }
}
