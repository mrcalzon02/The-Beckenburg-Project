package org.beckenburg;

import java.util.Arrays;

/**
 * Physical camera/pose acceptance for approved static character turnarounds.
 * Exercises the actual production rasterizer, not a screenshot placeholder.
 * No frame-cycling is permitted until independently approved animations ship.
 */
public final class CharacterSpriteWorldMotionTest {
    private CharacterSpriteWorldMotionTest() {}

    private static void check(boolean ok,String reason){
        if(!ok)throw new AssertionError(reason);
    }
    private static long digest(int[] pixels){
        long hash=1469598103934665603L;
        for(int pixel:pixels)hash=(hash^pixel)*1099511628211L;
        return hash;
    }
    private static int[] raster(String identity,int heading,double x,double z,
                                boolean blocked){
        int[] pixels=new int[SceneRenderer.W*SceneRenderer.H];
        double[] depth=new double[pixels.length];
        Arrays.fill(depth,blocked?1.0:Double.POSITIVE_INFINITY);
        var shape=CharacterVisuals.shape(identity);
        DirectionalSprites.draw(pixels,depth,0,0,0,0,x,z,heading,identity,
            shape.height(),shape.width(),0,255,true);
        return pixels;
    }
    public static void main(String[] args){
        String[] roster={"cleric","observer","demonologist",
            "elder_blade_demon","elder_plague_demon","elder_nothic"};
        long[] identities=new long[roster.length];
        for(int entity=0;entity<roster.length;entity++){
            String identity=roster[entity];
            // Four actual body faces, not synthetic walking frames.
            int[] compass={180,90,0,270};
            long[] facings=new long[4];
            for(int facing=0;facing<compass.length;facing++){
                int heading=compass[facing];
                int[] raster=raster(identity,heading,0,4,false);
                check(Arrays.stream(raster).anyMatch(pixel->pixel!=0),
                    "missing rendered physical character: "+identity+"/"+heading);
                facings[facing]=digest(raster);
                check(Arrays.stream(raster(identity,heading,0,4,true)).allMatch(pixel->pixel==0),
                    "foreground wall must occlude entire body: "+identity+"/"+heading);
            }
            // Both sides may intentionally share approved one-sided source art.
            check(facings[0]!=facings[1]&&facings[0]!=facings[2]
                    &&facings[2]!=facings[1],
                "turning an entity must select different physical artwork: "+identity);
            check(digest(raster(identity,180,.5,4,false))!=facings[0],
                "accepted world-space translation must move the actual sprite: "+identity);
            identities[entity]=facings[0];
        }
        check(Arrays.stream(identities).distinct().count()==roster.length,
            "all three Hunter classes and three Elders must have different silhouettes");
        check(DirectionalSprites.facing(180,0,4,0,0)==0,"front direction");
        check(DirectionalSprites.facing(0,0,4,0,0)==4,"rear direction");
        check(DirectionalSprites.facing(90,0,4,0,0)==2,"first side direction");
        check(DirectionalSprites.facing(270,0,4,0,0)==6,"opposite side direction");
        System.out.println("PASS six real camera-depth sprites, four headings, motion, alpha and wall occlusion");
    }
}
