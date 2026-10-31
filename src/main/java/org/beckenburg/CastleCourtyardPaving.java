package org.beckenburg;

import java.awt.image.BufferedImage;

/** A worn open-air stone courtyard, not interior brick or a uniform floor tile.
 * Paving is generated deterministically at PSX resolution, independent of
 * source-model UVs or original art. The repeatable diffuse texture is a
 * 4-metre patch of uneven flagstones, eroded joints, dirt and sparse moss.
 * Existing structural deck/collision geometry is deliberately unchanged.
 */
public final class CastleCourtyardPaving {
    private CastleCourtyardPaving(){}
    public static final int SIZE=192;
    private static int material=-1;
    public static synchronized int material(){
        if(material<0)material=GlbProps.surfaceMaterial("castle/open-courtyard/aged-flagstone",
            image());
        return material;
    }
    private static int hash(int x,int y,int seed){
        int n=x*0x1f123bb5+y*0x5f356495+seed*0x2c9277b5;
        n^=n>>>16;n*=0x7feb352d;n^=n>>>15;n*=0x846ca68b;n^=n>>>16;
        return n;
    }
    private static int clamp(int x){return Math.max(0,Math.min(255,x));}
    private static int rgb(int r,int g,int b){
        return (clamp(r)<<16)|(clamp(g)<<8)|clamp(b);
    }
    /** Four-metre seamless section of weathered broad slate slabs. No brick bond.
     * Staggered paving joints vary in both axes and remain clear at PSX scale.
     */
    public static BufferedImage image(){
        BufferedImage out=new BufferedImage(SIZE,SIZE,BufferedImage.TYPE_INT_RGB);
        int[] pixels=new int[SIZE*SIZE];
        // 4 courses of wide slate, alternating the offset so joints do not
        // form one uniform square grid. Every patch has different wear.
        final int rows=4,rowHeight=SIZE/rows;
        for(int y=0;y<SIZE;y++)for(int x=0;x<SIZE;x++){
            int row=y/rowHeight,localY=y%rowHeight;
            int shift=(row%2==0?0:SIZE/8),ax=(x+shift)%SIZE;
            int col=ax/(SIZE/4),localX=ax%(SIZE/4);
            // Unevenly broken seam edges, not straight painted brick lines.
            int irregular=((hash(col,row,73)&7)-3);
            int edge=Math.min(localX,SIZE/4-1-localX);
            int lip=Math.min(localY,rowHeight-1-localY);
            boolean grout=edge<2||lip<2+(irregular>2?1:0);
            int stone=hash(col,row,11);
            int base=102+((stone>>>12)&27)-14;
            int grain=(hash(x,y,3)&15)-8;
            int mottling=(hash(x/6+col*13,y/6+row*17,99)&31)-16;
            int crumbled=(hash(x/3,y/3,17)&31)==0?10:0;
            int r=base+grain+mottling/2-crumbled;
            int g=base+1+grain+mottling/2-crumbled;
            int b=base-3+grain+mottling/2-crumbled;
            if(grout){r=49+grain/2;g=48+grain/2;b=43+grain/2;}
            // Moss concentrates in shaded gaps, worn corners and drain grooves.
            boolean moss=(grout&&(hash(x/4,y/4,45)&15)<5)
                ||((hash(x/8,y/8,52)&255)<8&&edge<7);
            if(moss){r=47+grain/2;g=66+grain/2;b=44+grain/2;}
            // Soft dirt/wet-darkened patches create useful visual landmarks.
            if(!grout&&(hash(x/17,y/17,21)&31)==0){
                r-=10;g-=8;b-=7;
            }
            pixels[y*SIZE+x]=rgb(r,g,b);
        }
        out.setRGB(0,0,SIZE,SIZE,pixels,0,SIZE);
        return out;
    }
}
