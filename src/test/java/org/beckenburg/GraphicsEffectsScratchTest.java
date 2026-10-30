package org.beckenburg;

import java.util.Arrays;

/** Non-blocking performance diagnostic: scratch reuse preserves exact pixels. */
public final class GraphicsEffectsScratchTest {
    private static void check(boolean ok,String reason){
        if(!ok)throw new AssertionError(reason);
    }
    private static GraphicsOptions options(GraphicsOptions.Antialias aa,
            boolean chromatic,boolean extra){
        return new GraphicsOptions(GraphicsOptions.Mode.WINDOWED,
            GraphicsOptions.DisplaySize.NATIVE,GraphicsOptions.Detail.RETRO,
            GraphicsOptions.Aspect.AUTO,GraphicsOptions.Upscale.CRISP,
            aa,93,extra?11:0,extra?-7:0,extra,extra,extra,chromatic);
    }
    public static void main(String[] args){
        for(int[] size:new int[][]{{7,7},{320,200},{640,360}}){
            int width=size[0],height=size[1];
            int[] original=new int[width*height];
            for(int i=0;i<original.length;i++)
                original[i]=((i*47+23)&255)<<16|((i*11+192)&255)<<8|((i*29+13)&255);
            int[] scratch=new int[original.length];
            for(var aa:GraphicsOptions.Antialias.values())
                for(boolean chromatic:new boolean[]{false,true})
                    for(boolean extra:new boolean[]{false,true}){
                        var settings=options(aa,chromatic,extra);
                        int[] baseline=original.clone(),reused=original.clone();
                        GraphicsEffects.apply(baseline,width,height,settings,2.75);
                        GraphicsEffects.apply(reused,width,height,settings,2.75,scratch);
                        check(Arrays.equals(baseline,reused),
                            "Reusable scratch changed pixels for "+aa
                            +" chromatic="+chromatic+" effects="+extra);
                        // Reuse without clearing: input must overwrite all required
                        // source pixels rather than blending stale prior frames.
                        int[] next=original.clone();
                        for(int i=0;i<next.length;i++)next[i]^=0x251309;
                        int[] expected=next.clone();
                        GraphicsEffects.apply(expected,width,height,settings,7.25);
                        GraphicsEffects.apply(next,width,height,settings,7.25,scratch);
                        check(Arrays.equals(expected,next),"Scratch retained previous frame");
                    }
            // Scratch aliasing and undersizing must still fall back safely.
            var aa=options(GraphicsOptions.Antialias.EDGE,true,true);
            int[] reference=original.clone(),aliased=original.clone(),small=original.clone();
            GraphicsEffects.apply(reference,width,height,aa,0.5);
            GraphicsEffects.apply(aliased,width,height,aa,0.5,aliased);
            GraphicsEffects.apply(small,width,height,aa,0.5,new int[1]);
            check(Arrays.equals(reference,aliased)&&Arrays.equals(reference,small),
                "Invalid scratch buffers must use equivalent safe fallback");
        }
        System.out.println("PASS pixel-identical reusable AA/chromatic scratch at three resolutions");
    }
}
