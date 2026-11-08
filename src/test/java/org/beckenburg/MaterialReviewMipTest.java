package org.beckenburg;

import java.awt.image.BufferedImage;

/** Exercise the largest supported native material with a bounded review cache. */
public final class MaterialReviewMipTest {
    private static void check(boolean valid,String message){if(!valid)throw new AssertionError(message);}
    public static void main(String[] args){
        final int size=2048;
        var source=new BufferedImage(size,size,BufferedImage.TYPE_INT_ARGB);
        var cache=new MaterialReviewMip.Cache();
        cache.level(0,source,4,true);
        long full=cache.filteredTexels;
        check(full==1_392_640,"Full mip count");
        source.setRGB(0,0,0xffffffff);
        source.setRGB(size-1,size-1,0xff0088ff);
        cache.level(0,source,1,true);
        // Two disjoint source texels each affect a single texel at every level.
        check(cache.filteredTexels-full==8,"Distant edits refiltered the space between them");
        for(int level=1;level<=4;level++){
            var actual=cache.level(0,source,level,true);
            var rebuilt=MaterialReviewMip.level(source,level,true);
            int width=actual.getWidth();
            for(int y=0;y<width;y++)for(int x=0;x<width;x++)
                check(actual.getRGB(x,y)==rebuilt.getRGB(x,y),"Localized mip differs from full texture");
        }
        check(cache.filteredTexels==full+8,"Repeated observations refiltered unchanged levels");
        cache.level(1,source,4,true); // Both chains cannot fit within this window's cache.
        long afterEviction=cache.filteredTexels;
        cache.level(0,source,4,true);
        check(cache.filteredTexels-afterEviction==full,"Evicted chain was not regenerated");
        var opaque=cache.level(0,source,4,false);
        check((opaque.getRGB(64,64)>>>24)==255,"Changing alpha semantics must rebuild");
        System.out.println("PASS 2048-square RGBA review: 8 dirty mip texels versus "+full+
            " for full regeneration; exact levels, bounded eviction and alpha rebind");
    }
}
