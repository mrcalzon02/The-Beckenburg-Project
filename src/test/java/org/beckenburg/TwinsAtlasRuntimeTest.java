package org.beckenburg;

import java.awt.image.BufferedImage;

/**
 * Advisory art regression against the real shipped nine-part v3 atlas.
 * A source PNG test alone cannot catch packaging/decoder regressions.
 */
public final class TwinsAtlasRuntimeTest {
    private TwinsAtlasRuntimeTest(){}

    private static void check(boolean okay,String message){
        if(!okay)throw new AssertionError(message);
    }

    private static int visible(BufferedImage img,int x0,int y0,int x1,int y1){
        int n=0;
        for(int y=y0;y<y1;y++)for(int x=x0;x<x1;x++){
            if((img.getRGB(x,y)>>>24)>=24)n++;
        }
        return n;
    }

    public static void main(String[] args){
        check(GhostSpriteAtlas.installed(),"Shipped v3 archive not installed");
        check(GhostSpriteAtlas.supports("twins"),"Original Twins ghost absent");
        String[] facing={"front","front_right","right","back_right",
            "back","back_left","left","front_left"};
        for(int f=0;f<8;f++){
            BufferedImage img=GhostSpriteAtlas.frame("twins",f,"idle",0);
            check(img!=null&&img.getWidth()==64&&img.getHeight()==112,
                "Original Twins directional atlas size changed: "+facing[f]);
            check(visible(img,0,0,64,112)>1000,
                "Two original source figures damaged: "+facing[f]);
        }
        for(int f:new int[]{1,2,6,7}){
            BufferedImage img=GhostSpriteAtlas.frame("twins",f,"idle",0);
            int a=f==1||f==2?0:46;
            int b=f==1||f==2?18:64;
            check(visible(img,a,35,b,97)==0,
                "Disconnected alpha stick survives "+facing[f]);
        }
        String[] states={"idle","walk","sprint","attack","signature",
            "manifest","recover","dissipate"};
        for(int f=0;f<8;f++)for(String state:states)for(int step=0;step<4;step++){
            BufferedImage img=GhostSpriteAtlas.frame("twins",f,state,step);
            check(img!=null&&img.getWidth()==64&&img.getHeight()==112,
                "Twins ghost animation sheet corrupt "+facing[f]+"/"+state+"/"+step);
        }
        check(GhostSpriteAtlas.frame("spirit",0,"idle",0)!=null,
            "Unrelated source ghost atlas altered");
        System.out.println("PASS original Twins game decoder: 8 facings, 256 cells, "
            +"four detached alpha strips cleared without lost figures");
    }
}
