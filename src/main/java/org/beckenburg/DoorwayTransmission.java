package org.beckenburg;

import org.beckenburg.LevelDefinition.Portal;

/**
 * A doorway is one bidirectional transfer surface shared by physical sound
 * and lighting. Each medium obeys its own loss rule; neither can invent a
 * path through a wall or ignore the live door state.
 *
 * Optical transfer is currently a diagnostic candidate, not yet added to
 * SceneRenderer's final lighting buffer: that promotion requires physical
 * frame-time evidence and side-by-side image review. Host sound routing
 * consumes the shared per-portal acoustic rule now.
 */
public final class DoorwayTransmission {
    private DoorwayTransmission(){}

    /** Sound attenuates through shut leaves; visible light does not. */
    public static int soundLoss(HouseMap map,Portal portal,int from,int to){
        if(map==null||portal==null||!portal.joins(from,to)
                ||from<0||to<0||from>=map.roomCount()||to>=map.roomCount())
            return Integer.MAX_VALUE;
        int loss=Math.max(1,(int)Math.ceil(portal.loss()));
        if(portal.door()&&!map.doorOpen(portal.id()))loss+=2;
        if(portal.kind().equals("window")&&!map.windowOpen(portal.id()))loss+=2;
        loss+=Math.max(absorption(map.level().room(from).acoustic()),
                absorption(map.level().room(to).acoustic()));
        if(map.outside(from)!=map.outside(to))loss++;
        return loss;
    }
    private static int absorption(String material){
        if(material==null)return 0;
        String name=material.toLowerCase(java.util.Locale.ROOT);
        return name.contains("carpet")||name.contains("soft")
                ||name.contains("upholster")?1:0;
    }

    /** Reciprocal optical aperture. Shut opaque doors block light; glass still
     * transmits some visible light even though it muffles acoustic signals.
     * Stair openings remain passable; slabs remain authoritative elsewhere. */
    public static double opticalTransmission(HouseMap map,Portal portal,int from,int to){
        if(map==null||portal==null||!portal.joins(from,to)
                ||from<0||to<0||from>=map.roomCount()||to>=map.roomCount())return 0;
        if(portal.door())return map.doorOpen(portal.id())?1:0;
        if(portal.kind().equals("window"))return map.windowOpen(portal.id())?1:.72;
        return 1;
    }
    /** Castle uses the same transfer convention, backed by its own physical
     * door leaf instead of the HouseMap room graph. Doorway IDs are unique. */
    public static double opticalTransmission(CastleHubMap castle,CastleDoorSlots.Slot slot){
        if(castle==null||slot==null||!CastleDoorSlots.available(slot))return 0;
        var kind=CastleLevelEdits.current().doorType(slot.id());
        return kind==CastleDoorSlots.Type.EMPTY||castle.doorOpen(slot.id())?1:0;
    }

    public record LightRGB(double red,double green,double blue){
        public static final LightRGB DARK=new LightRGB(0,0,0);
        public double energy(){return red+green+blue;}
    }

    /** One-hop optical portal proposal: emit light in room A, measure flux at
     * the A-side aperture, and transmit an attenuated directional light from
     * the B-side aperture toward the receiver. Both segments are tested
     * against REAL architecture and prop occlusion. A later GPU implementation
     * can cache incidence once per source+portal+revision, instead of testing
     * each distant source against every pixel in room B.
     *
     * This does not alter live image output and is deliberately limited to
     * adjacent rooms. Multi-hop transport needs an explicit non-revisiting
     * energy budget before promotion, not unbounded recursive virtual bulbs.
     */
    public static LightRGB projectedLight(HouseMap map,Portal portal,
            int from,int to,FixtureLighting.Source source,
            double receiverX,double receiverY,double receiverZ,double factor){
        double transmission=opticalTransmission(map,portal,from,to);
        if(transmission<=0||source==null||source.room()!=from
                ||!Double.isFinite(receiverX+receiverY+receiverZ+factor)
                ||factor<=0||source.radius()<=0)return LightRGB.DARK;
        // Project doorway offsets in the wall-normal axis; each point stays
        // in its own room so a closed/solid jamb cannot be bypassed.
        double normalX=portal.alongX()?0:1,normalZ=portal.alongX()?1:0;
        double signed=(source.x()-portal.x())*normalX
                     +(source.z()-portal.z())*normalZ;
        if(Math.abs(signed)<.001){
            var room=map.level().room(from);
            signed=(room.cx()-portal.x())*normalX+(room.cz()-portal.z())*normalZ;
        }
        if(Math.abs(signed)<.001)return LightRGB.DARK;
        double side=Math.copySign(1,signed);
        double incomingX=portal.x()+normalX*side*.27;
        double incomingZ=portal.z()+normalZ*side*.27;
        double outgoingX=portal.x()-normalX*side*.27;
        double outgoingZ=portal.z()-normalZ*side*.27;
        double apertureY=portal.y()+1.35;
        // Incident light must arrive within its physical radius and cone.
        double dx=incomingX-source.x(),dy=apertureY-source.y(),
               dz=incomingZ-source.z(),d2=dx*dx+dy*dy+dz*dz;
        if(d2>=source.radius()*source.radius()||d2<.000001)return LightRGB.DARK;
        double d=Math.sqrt(d2),cosine=(dx*source.dx()+dy*source.dy()+dz*source.dz())/d;
        double beam=FixtureLighting.angular(source.kind(),cosine);
        if(beam<=0)return LightRGB.DARK;
        if(!ArchitecturalLightOcclusion.visible(map,from,source.x(),source.y(),
                source.z(),incomingX,apertureY,incomingZ))return LightRGB.DARK;
        // The receiving room owns its own furniture and architectural shadows.
        if(!ArchitecturalLightOcclusion.visible(map,to,outgoingX,apertureY,
                outgoingZ,receiverX,receiverY,receiverZ))return LightRGB.DARK;
        double traveled=Math.hypot(Math.hypot(receiverX-outgoingX,receiverZ-outgoingZ),
                receiverY-apertureY);
        if(traveled>=source.radius()-d)return LightRGB.DARK;
        double incidence=dev.mrcalzon.lighting.LightModel.attenuation(
                source.power(),beam,
                dev.mrcalzon.lighting.LightModel.diffusionFade(d,source.radius(),.20),
                d2,1);
        // The transfer aperture absorbs energy; the receiving leg attenuates.
        // Keep energy bounded, never create or multiply luminous sources.
        double spread=1/(1+.15*traveled*traveled);
        double strength=Math.min(1.6,Math.max(0,incidence*factor*transmission*spread*.68));
        return new LightRGB(strength*source.red(),strength*source.green(),
                strength*source.blue());
    }
}
