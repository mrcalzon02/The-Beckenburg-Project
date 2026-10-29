package org.beckenburg;

import java.awt.image.BufferedImage;

/** Pixel updates have their own revision; static geometry is independent. */
public final class DynamicTextureRevisionTest {
    private static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
    public static void main(String[] args){
        check(!GlbProps.registeredTexture(GlbProps.VAN_MAP_MATERIAL),"Map sentinel is not a dense model texture index");
        check(GlbProps.dynamicTextureRevision(MaterialAtlas.STONE)==-1,"Static material has no live pixel revision");
        int tv=GlbProps.televisionMaterial(1001,2,1001);
        long first=GlbProps.dynamicTextureRevision(tv);GlbProps.updateTelevision(tv,true,10);
        long active=GlbProps.dynamicTextureRevision(tv);check(first!=active,"TV pixel generation changes revision");
        GlbProps.updateTelevision(tv,true,10);check(active==GlbProps.dynamicTextureRevision(tv),"Same TV tick avoids redundant upload");
        GlbProps.updateTelevision(tv,true,11);check(active!=GlbProps.dynamicTextureRevision(tv),"Next actual TV frame is live");
        GlbProps.updateTelevision(tv,false,11);long off=GlbProps.dynamicTextureRevision(tv);
        GlbProps.updateTelevision(tv,false,12);check(off==GlbProps.dynamicTextureRevision(tv),"Powered-off TV pixels stay unchanged");
        long map=GlbProps.dynamicTextureRevision(GlbProps.VAN_MAP_MATERIAL);
        GlbProps.updateVanMap(new BufferedImage(256,256,BufferedImage.TYPE_INT_RGB));
        check(map!=GlbProps.dynamicTextureRevision(GlbProps.VAN_MAP_MATERIAL),"Actual map-image publication changes its texture revision");
        System.out.println("PASS separately revisioned live TV/map pixels and static material independence");
    }
}
