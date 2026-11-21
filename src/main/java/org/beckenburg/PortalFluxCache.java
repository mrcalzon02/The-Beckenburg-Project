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
    /** Source-to-door incident visibility is geometry-owned and unchanged
     * by unrelated distant doors or light brightness. Store null as a known
     * blocked result to avoid repeating the expensive architectural ray. */
    private final Map<Long,Transfer> incidentCache=new HashMap<>();
    private final Map<Integer,Portal> portalsById=new HashMap<>();
    private final Map<Integer,Boolean> previousDoorStates=new HashMap<>();
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
    private int previousHidingDensity=-1;
    private int rebuilds,transfers;
    private long incidentRays,incidentCacheHits,receiverRays,receiverHits;
    private static long key(int source,int receiver){
        return ((long)source<<32)|(receiver&0xffffffffL);
    }
    PortalFluxCache(HouseMap map,List<FixtureLighting.Source> sources){
        this.map=java.util.Objects.requireNonNull(map);
        this.sources=List.copyOf(sources);
        for(var portal:map.level().portals())
            portalsById.put(portal.id(),portal);
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
    /** A door change can affect incident visibility at another doorway only
     * when its leaf is in that doorway's source/receiver architectural region.
     * Adjacent or overlapping level volumes are always invalidated; distant
     * unrelated rooms retain their exact source-to-aperture ray. */
    private boolean changedDoorAffects(Portal changed,
            FixtureLighting.Source source,Portal illuminated){
        if(changed.id()==illuminated.id()
                ||changed.a()==illuminated.a()||changed.b()==illuminated.a()
                ||changed.a()==illuminated.b()||changed.b()==illuminated.b())
            return true;
        for(int room:new int[]{illuminated.a(),illuminated.b()}){
            if(!interior(room))return true;
            Room r=map.level().room(room);
            if(changed.x()>=r.x0()-.6&&changed.x()<=r.x1()+.6
                    &&changed.z()>=r.z0()-.6&&changed.z()<=r.z1()+.6
                    &&changed.y()>=r.y()-.6
                    &&changed.y()<=r.y()+r.height()+.6)return true;
        }
        return false;
    }
    private Transfer cachedIncident(FixtureLighting.Source source,
            Portal portal,int receiver){
        long cacheKey=key(source.id(),portal.id());
        if(incidentCache.containsKey(cacheKey)){
            incidentCacheHits++;
            return incidentCache.get(cacheKey); // null is a known blocked ray
        }
        Transfer result=prepare(source,portal,receiver);
        incidentCache.put(cacheKey,result);
        return result;
    }
    void refresh(){
        String doors=map.doorSnapshot(),windows=map.windowSnapshot();
        int hiding=map.hidingDensity();
        if(doors.equals(previousDoors)&&windows.equals(previousWindows)
                &&hiding==previousHidingDensity)return;
        boolean full=previousDoors==null||!windows.equals(previousWindows)
                ||hiding!=previousHidingDensity;
        var changedDoors=new ArrayList<Portal>();
        for(var portal:map.level().portals())if(portal.door()){
            boolean open=map.doorOpen(portal.id());
            Boolean old=previousDoorStates.put(portal.id(),open);
            if(old!=null&&old.booleanValue()!=open)changedDoors.add(portal);
        }
        // A hidden-geometry/window revision changes physical source rays,
        // not just light intensity. Unknown topology deltas fail closed.
        if(full||changedDoors.isEmpty())incidentCache.clear();
        else incidentCache.keySet().removeIf(cacheKey->{
            int sourceId=(int)(cacheKey>>>32);
            int portalId=(int)(long)cacheKey;
            if(sourceId<0||sourceId>=sources.size())return true;
            var portal=portalsById.get(portalId);
            if(portal==null)return true;
            for(var changed:changedDoors)
                if(changedDoorAffects(changed,sources.get(sourceId),portal))
                    return true;
            return false;
        });
        previousDoors=doors;previousWindows=windows;
        previousHidingDensity=hiding;
        rebuilds++;
        // Receiver-side visibility is conservatively forgotten on any real
        // geometry transition; source incidence is the expensive cached part.
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
                Transfer result=cachedIncident(source,portal,to);
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
    /** Minimum segment distance to the REAL rectangular opening, allowing
     * either entry height and any point across its physical width. It is
     * intentionally a lower bound, never a fabricated unobstructed ray. */
    private static double apertureDistance(Portal portal,double x,double y,double z){
        double along=portal.alongX()?x-portal.x():z-portal.z();
        double normal=portal.alongX()?z-portal.z():x-portal.x();
        double sideways=Math.max(0,Math.abs(along)-portal.width()*.5);
        double height=Math.max(0,Math.max(portal.y()-.10-y,
            y-(portal.y()+2.45)));
        double wall=Math.max(0,Math.abs(normal)-INSET);
        return Math.sqrt(wall*wall+sideways*sideways+height*height);
    }
    /** Door changes invalidate every receiver that COULD have been shaded
     * by virtual doorway light, including off-axis surfaces. A receiver
     * beyond every physically possible source-door-receiver path must stay
     * cached, avoiding a full-room shadow/VBO rebuild on local door changes.
     * The existing direct-ray portal filter still runs independently. */
    boolean mayAffectReceiver(FixtureLighting.Source source,Portal portal,
                              double x,double y,double z,double margin){
        if(source==null||portal==null||!eligible(portal)
                ||!Double.isFinite(x+y+z+margin)||margin<0)return false;
        int from=source.room();
        if(from!=portal.a()&&from!=portal.b())return false;
        int to=from==portal.a()?portal.b():portal.a();
        if(!interior(from)||!interior(to)
                ||overlaps(map.level().room(from),map.level().room(to)))return false;
        Room room=map.level().room(to);
        if(x<room.x0()-margin||x>room.x1()+margin
            ||z<room.z0()-margin||z>room.z1()+margin
            ||y<room.y()-margin||y>room.y()+room.height()+margin)return false;
        // Triangle inequality: any two-segment path through ANY point on
        // this aperture is at least the sum of these independent minima.
        // Only reject when even that optimistic path exceeds source reach.
        double first=apertureDistance(portal,source.x(),source.y(),source.z());
        if(first>source.radius()+margin)return false;
        double second=apertureDistance(portal,x,y,z);
        return first+second<=source.radius()+margin;
    }
    int rebuilds(){return rebuilds;}
    int transfers(){return transfers;}
    long incidentRays(){return incidentRays;}
    long incidentCacheHits(){return incidentCacheHits;}
    long receiverRays(){return receiverRays;}
    long receiverHits(){return receiverHits;}
}
