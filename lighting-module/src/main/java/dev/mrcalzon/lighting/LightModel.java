package dev.mrcalzon.lighting;

/**
 * Pure lighting mathematics shared by the field and Castle renderers.
 * No authored maps, graphics drivers, network simulation or Swing dependency.
 * Source visibility is still decided by the game's exact wall/door ray tests.
 */
public final class LightModel {
    private LightModel() {}
    public enum Beam { FLUORESCENT, VAN_DIFFUSER, TELEVISION, TABLE_LAMP, FROSTED_DOME, OMNIDIRECTIONAL }

    private static double smooth(double lo,double hi,double value){
        double t=Math.max(0,Math.min(1,(value-lo)/(hi-lo)));
        return t*t*(3-2*t);
    }
    public static double angular(Beam beam,double dot){
        if(beam==null||!Double.isFinite(dot))
            throw new IllegalArgumentException("beam / direction");
        return switch(beam){
            case FLUORESCENT -> smooth(.48,.72,dot);
            case VAN_DIFFUSER -> .35+.65*smooth(-.12,.45,dot);
            case TELEVISION -> Math.pow(Math.max(0,dot),2.2);
            case TABLE_LAMP -> .07+.86*smooth(.32,.76,dot)+.24*smooth(.70,.96,-dot);
            case FROSTED_DOME -> .18+.82*smooth(-.08,.42,dot);
            case OMNIDIRECTIONAL -> 1;
        };
    }
    /** Physically masked source-to-receiver contribution, excluding game state factor. */
    public static double attenuation(double sourcePower,double angle,double fade,
                                     double distanceSquared,double diffuse){
        return sourcePower*angle*fade*(.45+.55*fade)/(1+.13*distanceSquared)*diffuse;
    }
    /** Cosmetic waveform only; never changes an authoritative interference state. */
    public static double flicker(double phase,double fraction){
        return 1-.06*fraction*(1-Math.sin(phase));
    }
    /**
     * Conservative intersection: a changed doorway only touches sources whose
     * light volume might cross its bounded aperture. Actual shadows still use
     * per-source real geometry LOS, never this bounding-box approximation.
     */
    public static boolean portalInfluences(double lightX,double lightY,double lightZ,
            double radius,double portalX,double portalY,double portalZ,
            double width,boolean alongX){
        double half=width/2+.35;
        double dx=Math.max(0,Math.abs(lightX-portalX)-(alongX?half:.35));
        double dz=Math.max(0,Math.abs(lightZ-portalZ)-(alongX?.35:half));
        double dy=Math.max(0,Math.abs(lightY-(portalY+1.15))-1.15);
        return dx*dx+dy*dy+dz*dz<=radius*radius;
    }
}
