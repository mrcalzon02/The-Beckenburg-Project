package org.beckenburg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.beckenburg.LevelDefinition.Portal;
import org.beckenburg.LevelDefinition.Room;
import dev.mrcalzon.lighting.LightModel;
import dev.mrcalzon.lighting.LightingModule;

/**
 * Production one-hop portal light field, shared by per-pixel probes and
 * resident GPU vertex preparation. A fixed source illuminates an aperture
 * once per door-topology revision, not once per target vertex. Its outgoing
 * ray is cached by exact receiving point and doorway across all sources.
 *
 * Unknown geometry, overlapping rooms, exterior and multi-hop paths retain
 * the exact direct-source architecture ray rather than guessing.
 * The feature is reversible with beckenburg.lighting.portalFlux.disabled.
 */
final class PortalFluxCache {
    private static final double INSET=.27;
    private static final double ROOM_MARGIN=LevelDefinition.WALL_HALF_THICKNESS+.10;
    private final HouseMap map;
    private final List<FixtureLighting.Source> sources;
    private final double diffusion;
    private final Map<Long,List<Transfer>> routes=new HashMap<>();
    private final Map<ReceiverRay,Boolean> receiverSight=
        new LinkedHashMap<ReceiverRay,Boolean>(4096,.75f,true){
            @Override protected boolean removeEldestEntry(Map.Entry<ReceiverRay,Boolean> eldest){
                return size()>65_536;
            }
        };
    private record Transfer(int portalId,int room,
        double x,double y,double z,double remaining,double strength){}
    private record ReceiverRay(int portal,int room,long x,long y,long z){}
    private String previousDoors,previousWindows;
    private int rebuilds,transfers;
    private long incidentRays,receiverRays,receiverHits;
    private static long key(int source,int receiver){
        return ((long)source<<32)|(receiver&0xffffffffL);
    }
    PortalFluxCache(HouseMap map,List<FixtureLighting.Source> sources){
        this.map=java.util.Objects.requireNonNull(map);
        this.sources=List.copyOf(sources);
        diffusion=LightingModule.current().diffusionFraction();
        refresh();
    }
    private boolean interior(int room){
        return room>=0&&room<map.roomCount()&&!map.level().room(room).outside();
    }
    private static boolean inside(Room room,double x,double y,double z,boolean isSource){
        return Double.isFinite(x+y+z)
            &&x>room.x0()+ROOM_MARGIN&&x<room.x1()-ROOM_MARGIN
            &&z>room.z0()+ROOM_MARGIN&&z<room.z1()-ROOM_MARGIN
            &&y>room.y()+.08&&y<room.y()+room.height()+(isSource?.14:-.10);
    }
    private static boolean overlaps(Room a,Room b){
        // A contained upper room is not a single wall-aperture transfer.
        return Math.max(a.x0(),b.x0())<Math.min(a.x1(),b.x1())
            &&Math.max(a.z0(),b.z0())<Math.min(a.z1(),b.z1());
    }
    private static boolean eligible(Portal p){
        // Windows need refraction/occlusion and stairs need vertical apertures,
        // neither of which may be approximated as a standing-height door.
        return p.door();
    }
    void refresh(){
        String doors=map.doorSnapshot(),windows=map.windowSnapshot();
        if(doors.equals(previousDoors)&&windows.equals(previousWindows))return;
        previousDoors=doors;previousWindows=windows;
        rebuilds++;
        routes.clear();receiverSight.clear();transfers=0;
        for(var portal:map.level().portals()){
            if(!eligible(portal))continue;
            int a=portal.a(),b=portal.b();
            if(!interior(a)||!interior(b)||a==b
                ||overlaps(map.level().room(a),map.level().room(b)))continue;
            if(DoorwayTransmission.opticalTransmission(map,portal,a,b)<=0)continue;
            for(var source:sources){
                int from=source.room();
                if(from!=a&&from!=b)continue;
                int to=from==a?b:a;
                if(!inside(map.level().room(from),source.x(),source.y(),source.z(),true))
                    continue;
                Transfer result=prepare(source,portal,to);
                if(result==null)continue;
                routes.computeIfAbsent(key(source.id(),to),ignored->new ArrayList<>())
                    .add(result);
                transfers++;
            }
        }
    }
    private Transfer prepare(FixtureLighting.Source source,Portal portal,int receiver){
        double nx=portal.alongX()?0:1,nz=portal.alongX()?1:0;
        double side=(source.x()-portal.x())*nx+(source.z()-portal.z())*nz;
        if(Math.abs(side)<.001)return null;
        side=Math.copySign(1,side);
        double ix=portal.x()+nx*side*INSET;
        double iz=portal.z()+nz*side*INSET;
        double ox=portal.x()-nx*side*INSET;
        double oz=portal.z()-nz*side*INSET;
        double py=portal.y()+1.35;
        double dx=ix-source.x(),dy=py-source.y(),dz=iz-source.z();
        double sq=dx*dx+dy*dy+dz*dz;
        double distance=Math.sqrt(sq);
        double remaining=source.radius()-distance-2*INSET;
        if(remaining<=.1||distance<.001)return null;
        double direction=(dx*source.dx()+dy*source.dy()+dz*source.dz())/distance;
        double beam=FixtureLighting.angular(source.kind(),direction);
        if(beam<=0)return null;
        incidentRays++;
        if(!ArchitecturalLightOcclusion.visible(map,source.room(),
                source.x(),source.y(),source.z(),ix,py,iz))return null;
        double fade=LightModel.diffusionFade(distance,source.radius(),diffusion);
        double intensity=LightModel.attenuation(source.power(),beam,fade,sq,1);
        if(intensity<.002)return null;
        return new Transfer(portal.id(),receiver,ox,py,oz,remaining,
            intensity*.68);
    }
    /**
     * NaN means this room/source pair is not eligible for portal transport:
     * preserve the full legacy shadow ray. Finite weights, including zero
     * for an occluded receiver, replace the direct source contribution.
     * One source passing two openings contributes at most the strongest
     * path, avoiding virtual energy multiplication.
     */
    double weight(FixtureLighting.Source source,int room,
                  double x,double y,double z,
                  double nx,double ny,double nz,boolean normal){
        if(source==null||source.room()==room||!interior(room))return Double.NaN;
        List<Transfer> pair=routes.get(key(source.id(),room));
        if(pair==null||pair.isEmpty())return Double.NaN;
        Room destination=map.level().room(room);
        if(!inside(destination,x,y,z,false)
            ||overlaps(map.level().room(source.room()),destination))
            return Double.NaN;
        double maximum=0;
        for(var transfer:pair){
            double dx=x-transfer.x,dy=y-transfer.y,dz=z-transfer.z;
            double sq=dx*dx+dy*dy+dz*dz;
            if(sq>=transfer.remaining*transfer.remaining)continue;
            double distance=Math.sqrt(sq);
            double diffuse=1;
            if(normal){
                if(distance<1e-8)continue;
                diffuse=Math.max(0,
                    (-nx*dx-ny*dy-nz*dz)/distance);
                if(diffuse<.001)continue;
            }
            var receiver=new ReceiverRay(transfer.portalId,room,
                Double.doubleToLongBits(x),Double.doubleToLongBits(y),
                Double.doubleToLongBits(z));
            Boolean sight=receiverSight.get(receiver);
            if(sight==null){
                receiverRays++;
                sight=ArchitecturalLightOcclusion.visible(map,room,
                    transfer.x,transfer.y,transfer.z,x,y,z);
                receiverSight.put(receiver,sight);
            }else receiverHits++;
            if(!sight)continue;
            double spread=1/(1+.15*sq);
            maximum=Math.max(maximum,transfer.strength*spread*diffuse);
        }
        return maximum;
    }
    int rebuilds(){return rebuilds;}
    int transfers(){return transfers;}
    long incidentRays(){return incidentRays;}
    long receiverRays(){return receiverRays;}
    long receiverHits(){return receiverHits;}
}
