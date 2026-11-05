package org.beckenburg;

import java.util.List;

/**
 * Decorative Castle door reveals are measured from the same authored portal
 * marker and load-bearing lintel as collision/lighting. Never extend past the
 * opening or through the upper floor to fake a thick door frame.
 */
final class CastlePortalTrim {
    private CastlePortalTrim(){}
    record Beam(double x,double y,double z,double width,double height,
                double depth,int colour){
        Beam {
            if(!Double.isFinite(x+y+z+width+height+depth)
                    ||width<=0||height<=0||depth<=0)
                throw new IllegalArgumentException("Invalid Castle portal trim beam");
        }
        double top(){return y+height;}
    }
    static List<Beam> beams(CastleHubArchitecturalPlan.Wall portal,
                           List<CastleHubArchitecturalPlan.Wall> walls){
        if(portal==null||!portal.hasDoor())
            throw new IllegalArgumentException("Expected actual portal marker");
        double start=portal.horizontal()?portal.x0():portal.z0();
        double end=portal.horizontal()?portal.x1():portal.z1();
        double head=CastleHubArchitecturalPlan.portalHead(portal,walls);
        double foot=portal.bottom();
        if(end-start<=.10||head-foot<=.20)return List.of();
        // The opening spans from start..end and foot..head. Both jambs
        // are INSIDE it; the trim header stops below the solid stone lintel.
        double trim=Math.min(.15,(end-start)*.10,(head-foot)*.20);
        double rise=head-foot-trim;
        if(portal.horizontal()){
            double z=portal.z0();
            return List.of(
                new Beam(start,foot,z-.16,trim,rise,.32,0x786c5b),
                new Beam(end-trim,foot,z-.16,trim,rise,.32,0x786c5b),
                new Beam(start,head-trim,z-.18,end-start,trim,.36,0x9b8970)
            );
        }
        double x=portal.x0();
        return List.of(
            new Beam(x-.16,foot,start,.32,rise,trim,0x786c5b),
            new Beam(x-.16,foot,end-trim,.32,rise,trim,0x786c5b),
            new Beam(x-.18,head-trim,start,.36,trim,end-start,0x9b8970)
        );
    }
}
