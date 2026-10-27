package org.beckenburg;

import java.util.Objects;
import java.util.prefs.Preferences;

/** Engine-native PSX image treatment; external GLSL/Godot shaders cannot run in Swing. */
public final class PsxPipeline {
    private PsxPipeline(){}
    private static final String KEY="psxPreset";
    private static final int[] BAYER={0,8,2,10,12,4,14,6,3,11,1,9,15,7,13,5};
    public enum Preset {
        OFF("Off (original textures)",0,0,8,0),
        SOFT("Soft (subtle PSX)",.5,.25,6,.5),
        CLASSIC("Classic (PS1)",1,.70,5,1),
        GRITTY("Gritty (heavy PSX)",2,1,4,1.3);
        private final String title;
        final double snap,affine,dither;
        final int bits;
        Preset(String title,double snap,double affine,int bits,double dither){
            this.title=title;this.snap=snap;this.affine=affine;this.bits=bits;this.dither=dither;
        }
        @Override public String toString(){return title;}
    }
    public static Preset selected(){
        String value=Preferences.userNodeForPackage(PsxPipeline.class).get(KEY,Preset.CLASSIC.name());
        try{return Preset.valueOf(value);}catch(IllegalArgumentException ex){return Preset.CLASSIC;}
    }
    public static void choose(Preset preset){
        Preferences.userNodeForPackage(PsxPipeline.class).put(KEY,Objects.requireNonNull(preset).name());
    }
    /** Snap projected vertices, not world coordinates, before barycentric rasterization. */
    public static double snap(double screenCoordinate,Preset preset){
        Objects.requireNonNull(preset);
        if(preset.snap==0)return screenCoordinate;
        return Math.rint(screenCoordinate/preset.snap)*preset.snap;
    }
    /** Blend precise UVs with period-style affine UVs. Keep near-plane clipping intact. */
    public static double uv(double perspective,double affine,Preset preset){
        Objects.requireNonNull(preset);
        return perspective+(affine-perspective)*preset.affine;
    }
    // The ordered bias has only 16 phases and each input component has 256
    // values. Retain the exact old rounding result instead of repeating three
    // floating-point quantizations for every pixel of every gameplay frame.
    private static final int[][] CHANNELS=new int[Preset.values().length][];
    static {
        for(var preset:Preset.values()){
            int[] table=new int[16*256];int levels=(1<<preset.bits)-1;
            for(int phase=0;phase<16;phase++){
                double bias=(BAYER[phase]-7.5)/16.0*(255.0/levels)*preset.dither;
                for(int value=0;value<256;value++)table[phase*256+value]=channel(value,bias,levels);
            }
            CHANNELS[preset.ordinal()]=table;
        }
    }
    /** Ordered RGB quantization after world sprites, before the sharp Swing HUD. */
    public static void finishFrame(int[] rgb,int width,int height,Preset preset){
        Objects.requireNonNull(rgb);Objects.requireNonNull(preset);
        if(width<=0||height<=0||rgb.length!=width*height)
            throw new IllegalArgumentException("invalid PSX framebuffer");
        if(preset==Preset.OFF)return;
        int[] table=CHANNELS[preset.ordinal()];
        for(int y=0;y<height;y++)for(int x=0;x<width;x++){
            int i=y*width+x,c=rgb[i];
            int phase=((x&3)+((y&3)<<2))*256;
            int red=table[phase+((c>>>16)&255)];
            int green=table[phase+((c>>>8)&255)];
            int blue=table[phase+(c&255)];
            rgb[i]=(red<<16)|(green<<8)|blue;
        }
    }
    private static int channel(int component,double bias,int levels){
        double adjusted=Math.max(0,Math.min(255,component+bias));
        return (int)Math.round(Math.round(adjusted*levels/255.0)*255.0/levels);
    }
}
