package org.beckenburg;

/** Real original GLB material-alpha regression; cosmetic differences are advisory. */
public final class HollowCreekBillboardAlphaRuntimeTest {
    private HollowCreekBillboardAlphaRuntimeTest(){}
    private static void check(boolean ok,String message){
        if(!ok)throw new AssertionError(message);
    }
    public static void main(String[] args){
        int checked=0;
        for(String id:new String[]{"level_hc_native_284_0",
                "level_hc_native_317_0","level_hc_native_350_0"}){
            GlbProps.Model model=GlbProps.model(id);
            check(model.triangles().size()==2,
                "Original two-triangle billboard changed: "+id);
            int material=model.triangles().get(0).material();
            check(GlbProps.gltfBlended(material),
                "Source BLEND material lost in production decoder: "+id);
            check(GlbProps.gltfMaskCutoff(material)<0,
                "Incorrectly promoted native BLEND to MASK: "+id);
            int clear=0,visible=0;
            for(int y=0;y<24;y++)for(int x=0;x<24;x++){
                double u=(x+.5)/24,v=(y+.5)/24;
                int argb=GlbProps.sampleRgba(material,u,v);
                int a=argb>>>24;
                if(a<=3)clear++;
                if(a>=64)visible++;
            }
            check(clear>150&&visible>25,
                "Original canopy alpha lacks transparent background and visible foliage: "+
                id+" clear="+clear+" opaque="+visible);
            check(GlbProps.sampleRgba(material,.001,.001)>>>24<8,
                "Original transparent PNG corner became opaque: "+id);
            checked++;
        }
        var matthis=DevAssetReviewCatalog.entries().stream()
            .filter(e->e.id().equals("level_npc_matthis_rook_proxy"))
            .findFirst().orElseThrow();
        check(matthis.kind()==DevAssetReviewCatalog.Kind.WORLD_MODEL,
            "Approved Matthis model incorrectly classified unapproved");
        System.out.println("PASS original Hollow Creek glTF BLEND transparency: "+
            checked+" samples, verified original alpha, catalog membership");
    }
}
