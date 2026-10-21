package org.beckenburg;

/** Stable visual variation from an authored placement, never from frame time or client preferences. */
public final class FoliageVariation {
    private FoliageVariation(){}
    public static boolean foliage(String mesh){return mesh.startsWith("level_tree_")||mesh.equals("level_shrub_refuge")
        ||mesh.equals("level_woodland_grass")||mesh.equals("level_woodland_fern")||mesh.equals("level_woodland_flowers")||mesh.equals("level_woodland_scrub");}
    public static boolean leafMaterial(String name){String s=name.toLowerCase(java.util.Locale.ROOT);return s.contains("green")||s.contains("leaf")||s.contains("dry")||s.contains("petal")||s.contains("purple");}
    public static long seed(String mesh,double x,double z){return mix(mesh.hashCode()^Long.rotateLeft(Double.doubleToLongBits(x),19)^Double.doubleToLongBits(z));}
    private static long mix(long x){x=(x^(x>>>30))*0xbf58476d1ce4e5b9L;x=(x^(x>>>27))*0x94d049bb133111ebL;return x^(x>>>31);}
    private static double unit(long seed,int salt){return (mix(seed+salt*0x9e3779b97f4a7c15L)>>>11)*0x1.0p-53;}
    public record Profile(long seed,double anchor,double height,double widthX,double widthZ,double leanX,double leanZ,double twist,double asymmetry,double rotation,boolean deform){
        public LevelDefinition.Vec point(double x,double y,double z,double modelHeight){
            if(!deform||y<=anchor)return new LevelDefinition.Vec(x,y,z);
            double f=Math.max(0,Math.min(1,(y-anchor)/Math.max(.01,modelHeight-anchor))),bend=f*f;
            double angle=twist*f,c=Math.cos(angle),s=Math.sin(angle);
            double xx=x*(1+(widthX-1)*f)*(1+asymmetry*Math.tanh(x)*f),zz=z*(1+(widthZ-1)*f);
            return new LevelDefinition.Vec(xx*c-zz*s+leanX*modelHeight*bend,anchor+(y-anchor)*height,xx*s+zz*c+leanZ*modelHeight*bend);
        }
    }
    public static Profile profile(String mesh,double x,double z){
        long seed=seed(mesh,x,z);boolean tree=mesh.startsWith("level_tree_");
        // The first two metres of a solid trunk stay unchanged; physical capsules still meet the source bark.
        // Refuge foliage retains its authored hollow pocket and height; only its leaf texels vary.
        return new Profile(seed,tree?2.0:0,.80+unit(seed,1)*.43,.79+unit(seed,2)*.43,.79+unit(seed,3)*.43,
            (unit(seed,4)-.5)*.16,(unit(seed,5)-.5)*.16,(unit(seed,6)-.5)*.38,(unit(seed,7)-.5)*.19,
            foliage(mesh)&&!mesh.equals("level_shrub_refuge")?unit(seed,8)*Math.PI*2:0,foliage(mesh)&&!mesh.equals("level_shrub_refuge"));
    }
}
