package org.beckenburg;

/** All channel inputs, all dither phases, all presets retain original output. */
public final class PsxQuantizationParityTest {
    static final int[] BAYER={0,8,2,10,12,4,14,6,3,11,1,9,15,7,13,5};
    static int original(int channel,double bias,int levels){
        double adjusted=Math.max(0,Math.min(255,channel+bias));
        return (int)Math.round(Math.round(adjusted*levels/255.0)*255.0/levels);
    }
    public static void main(String[] args){
        for(var preset:PsxPipeline.Preset.values())for(int value=0;value<256;value++){
            var frame=new int[4*4];int input=(value<<16)|((255-value)<<8)|((value*73)&255);
            java.util.Arrays.fill(frame,input);PsxPipeline.finishFrame(frame,4,4,preset);
            for(int phase=0;phase<16;phase++){
                int expected=input;
                if(preset!=PsxPipeline.Preset.OFF){
                    int levels=(1<<preset.bits)-1;double bias=(BAYER[phase]-7.5)/16.0*(255.0/levels)*preset.dither;
                    expected=0;for(int shift:new int[]{0,8,16})expected|=original((input>>>shift)&255,bias,levels)<<shift;
                }
                if(frame[phase]!=expected)throw new AssertionError("PSX output changed: "+preset+" input="+value+" phase="+phase);
            }
        }
        System.out.println("PASS exhaustive original PSX quantization parity: all 256 components, 16 phases and four presets");
    }
}
