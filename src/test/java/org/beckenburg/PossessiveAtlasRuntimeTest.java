package org.beckenburg;

import java.awt.image.BufferedImage;

/** Advisory visual integrity check for the source-backed Possessive alpha repair.
 * Exercises the same packed v3 ghost art decoder used by the actual game.
 * Do NOT treat the artistic decision as a release-blocking test.
 */
public final class PossessiveAtlasRuntimeTest {
    private PossessiveAtlasRuntimeTest(){}

    private static void check(boolean pass,String message){
        if(!pass)throw new AssertionError(message);
    }

    private static int alphaPixels(BufferedImage source,int xmin,int xmax,int threshold){
        int count=0;
        for(int y=0;y<source.getHeight();y++)
            for(int x=xmin;x<xmax;x++)
                if((source.getRGB(x,y)>>>24)>=threshold)count++;
        return count;
    }

    public static void main(String[] args){
        check(GhostSpriteAtlas.installed(),"Shipped ghost atlas not installed");
        check(GhostSpriteAtlas.supports("possessive"),"Possessive source identity missing");

        String[] facings={"front","front_right","right","back_right",
            "back","back_left","left","front_left"};
        for(int facing=0;facing<facings.length;facing++){
            BufferedImage frame=GhostSpriteAtlas.frame("possessive",facing,"idle",0);
            check(frame!=null&&frame.getWidth()==64&&frame.getHeight()==112,
                "Wrong source sprite dimensions: "+facings[facing]);
            check(alphaPixels(frame,0,64,24)>1500,
                "Original source silhouette damaged: "+facings[facing]);
        }
        int[] affected={0,3,4,5};
        int[][] detachedBands={{53,64},{0,12},{0,12},{52,64}};
        for(int i=0;i<affected.length;i++){
            int facing=affected[i];
            BufferedImage frame=GhostSpriteAtlas.frame("possessive",facing,"idle",0);
            int residual=alphaPixels(frame,detachedBands[i][0],detachedBands[i][1],24);
            check(residual==0, "Detached alpha line remains in "+
                facings[facing]+": "+residual+" pixels");
        }
        for(int facing=0;facing<8;facing++){
            for(String state:new String[]{"idle","walk","sprint","attack","signature",
                "manifest","recover","dissipate"}){
                for(int step=0;step<4;step++){
                    BufferedImage frame=GhostSpriteAtlas.frame("possessive",facing,state,step);
                    check(frame!=null&&frame.getWidth()==64&&frame.getHeight()==112,
                        "Missing packed animation cell: "+facings[facing]+"/"+state+"/"+step);
                }
            }
        }
        System.out.println("PASS source-preserving Possessive atlas: 8 faces, 256 "+
            "decodable animation cells, 4 detached alpha bands cleared");
    }
}
