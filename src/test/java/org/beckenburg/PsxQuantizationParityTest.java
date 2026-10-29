package org.beckenburg;

import java.util.*;

/** Exhaust the 256 channel values at every Bayer phase and preset against
 * the original numerical quantizer, then compare complete mixed RGB images. */
public final class PsxQuantizationParityTest {
    private static int quantize(int c,double bias,int levels){
        double adjusted=Math.max(0,Math.min(255,c+bias));
        return (int)Math.round(Math.round(adjusted*levels/255.0)*255.0/levels);
    }
    private static void reference(int[] pixels,int w,int h,PsxPipeline.Preset preset){
        if(preset==PsxPipeline.Preset.OFF)return;
        int[] bayer={0,8,2,10,12,4,14,6,3,11,1,9,15,7,13,5};int levels=(1<<preset.bits)-1;
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){
            int i=y*w+x,c=pixels[i];double bias=(bayer[(x&3)+((y&3)<<2)]-7.5)/16.0*(255.0/levels)*preset.dither;
            pixels[i]=quantize(c>>>16&255,bias,levels)<<16|quantize(c>>>8&255,bias,levels)<<8|quantize(c&255,bias,levels);
        }
    }
    public static void main(String[] args){
        var random=new Random(19458);
        for(var preset:PsxPipeline.Preset.values())for(var size:List.of(new int[]{1024,4},new int[]{317,203},new int[]{640,400})){
            int w=size[0],h=size[1];int[] actual=new int[w*h];
            for(int i=0;i<actual.length;i++){int c=(i/4)&255;actual[i]=w==1024?c<<16|c<<8|c:random.nextInt(1<<24);}
            int[] expected=actual.clone();reference(expected,w,h,preset);PsxPipeline.finishFrame(actual,w,h,preset);
            if(!Arrays.equals(actual,expected))throw new AssertionError("PSX pixel parity: "+preset+" "+w+"x"+h);
        }
        System.out.println("PASS exact PSX quantization pixels across every channel, dither phase, preset and mixed RGB frame");
    }
}
