package org.beckenburg;

/** Axis-aligned authoring handles for the live castle surfaces.
 * Floors and roofs remain rectangular; walls remain legal line segments.
 * The reusable editor kit does not depend on these castle geometry rules.
 */
final class CastleSurfaceTransforms {
    private CastleSurfaceTransforms(){}
    private static double clip(double v,double lo,double hi){
        return Math.max(lo,Math.min(hi,v));
    }
    static CastleLevelEdits.Surface drag(CastleLevelEdits.Surface s,
            double pickX,double pickZ,double dx,double dz,boolean vertex){
        if(s==null||!Double.isFinite(pickX+pickZ+dx+dz))
            throw new IllegalArgumentException("Invalid surface drag");
        double x0=s.x0(),x1=s.x1(),z0=s.z0(),z1=s.z1();
        if(s.kind()==CastleLevelEdits.Kind.WALL){
            if(x0==x1){
                if(vertex){
                    if(Math.abs(pickZ-z0)<=Math.abs(pickZ-z1))
                        z0=clip(z0+dz,0,z1-.2);
                    else z1=clip(z1+dz,z0+.2,CastleHubMap.HEIGHT);
                }else x0=x1=clip(x0+dx,0,CastleHubMap.WIDTH);
            }else {
                if(vertex){
                    if(Math.abs(pickX-x0)<=Math.abs(pickX-x1))
                        x0=clip(x0+dx,0,x1-.2);
                    else x1=clip(x1+dx,x0+.2,CastleHubMap.WIDTH);
                }else z0=z1=clip(z0+dz,0,CastleHubMap.HEIGHT);
            }
        }else{
            boolean nearLeft=Math.abs(pickX-x0)<=Math.abs(pickX-x1);
            boolean nearFront=Math.abs(pickZ-z0)<=Math.abs(pickZ-z1);
            boolean editX=vertex||
                Math.min(Math.abs(pickX-x0),Math.abs(pickX-x1))
                <=Math.min(Math.abs(pickZ-z0),Math.abs(pickZ-z1));
            if(editX){
                if(nearLeft)x0=clip(x0+dx,0,x1-.2);
                else x1=clip(x1+dx,x0+.2,CastleHubMap.WIDTH);
            }
            if(vertex||!editX){
                if(nearFront)z0=clip(z0+dz,0,z1-.2);
                else z1=clip(z1+dz,z0+.2,CastleHubMap.HEIGHT);
            }
        }
        return new CastleLevelEdits.Surface(s.id(),s.kind(),x0,z0,x1,z1,
            s.base(),s.height(),s.material());
    }
}
