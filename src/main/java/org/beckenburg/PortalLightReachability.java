package org.beckenburg;

import java.util.List;
import java.util.Objects;
import org.beckenburg.LevelDefinition.Room;

/**
 * Cached conservative doorway connectivity for static light sources. A room
 * whose portal component is disconnected cannot receive direct illumination
 * from the source room. Every edge in that source's actual travel route
 * must intersect its physical light sphere and genuinely hard-edged beam:
 * touching the first door does NOT illuminate the entire connected graph.
 * Per-source doorway masks are computed only when authored topology changes.
 *
 * Real ray tracing is retained for reachable openings, boundary vertices,
 * uncertain source placement, stair/vertical holes, native maps and exterior
 * contacts; this never manufactures a virtual light or modifies the shader.
 *
 * A HouseMap owns authored topology. Castle currently does not expose its
 * architectural regions as room IDs; therefore Castle continues through the
 * exact same FixtureLighting backend without applying this unsafe shortcut.
 */
final class PortalLightReachability {
    private static final double EDGE_MARGIN=LevelDefinition.WALL_HALF_THICKNESS+.10;
    private final HouseMap map;
    private final List<FixtureLighting.Source> sources;
    private final boolean[] safeSource;
    private final long[] connected;
    /** Per-light rooms reached through at least one aperture the source can
     * physically touch. This is NOT one world-wide connected-room mask. */
    private final long[] sourceReachable;
    private int rebuilds,sourceConfined,sourceBlockedConnectedRooms;
    private String lastDoors,lastWindows;

    PortalLightReachability(HouseMap map,List<FixtureLighting.Source> sources){
        this.map=Objects.requireNonNull(map);
        this.sources=List.copyOf(sources);
        int count=map.roomCount();
        safeSource=new boolean[sources.size()];
        sourceReachable=new long[sources.size()];
        connected=new long[count];
        for(var source:sources){
            int room=source.room();
            if(source.id()<0||source.id()>=safeSource.length||!interior(room))continue;
            Room r=map.level().room(room);
            safeSource[source.id()]=inside(r,source.x(),source.y(),source.z(),true);
        }
        refresh();
    }
    private boolean interior(int id){
        return id>=0&&id<map.roomCount()&&!map.level().room(id).outside();
    }
    private static boolean inside(Room r,double x,double y,double z,boolean emitter){
        return Double.isFinite(x+y+z)
            &&x>r.x0()+EDGE_MARGIN&&x<r.x1()-EDGE_MARGIN
            &&z>r.z0()+EDGE_MARGIN&&z<r.z1()-EDGE_MARGIN
            &&y>r.y()+(emitter?.02:.10)
            &&y<r.y()+r.height()+(emitter?.12:-.10);
    }
    /** Cheap conservative overlap of a physical radius with the entire
     * doorway opening. It is deliberately not a centre-of-door sample:
     * a narrow beam or small light can enter near a jamb or lintel.
     *
     * Stairs have vertical void/ramp geometry instead of a simple rectangular
     * doorway, so they are never rejected on this incomplete bound. */
    private static boolean touchesOpticalAperture(FixtureLighting.Source light,
                                                   LevelDefinition.Portal p){
        if(p.kind().equals("stair"))return true;
        double x=p.x(),z=p.z();
        boolean window=p.kind().equals("window");
        double lo=p.y()+(window?1.0:-.10);
        double hi=p.y()+(window?2.35:2.45);
        double halfWidth=p.width()*.5+.05;
        double normalHalf=LevelDefinition.WALL_HALF_THICKNESS+.35;
        double heightHalf=(hi-lo)*.5+.05,midY=(lo+hi)*.5;
        double along=p.alongX()?light.x()-x:light.z()-z;
        double normal=p.alongX()?light.z()-z:light.x()-x;
        double side=Math.max(0,Math.abs(along)-halfWidth);
        double depth=Math.max(0,Math.abs(normal)-normalHalf);
        double vertical=Math.max(0,Math.abs(light.y()-midY)-heightHalf);
        double radius=light.radius()+1e-6;
        if(side*side+depth*depth+vertical*vertical>radius*radius)return false;
        // Beam types with nonzero backward/diffuse emission have no hard cone.
        // Only fluorescent and TV beams can be completely dark by direction.
        if(light.kind()!=FixtureLighting.Kind.FLUORESCENT
                &&light.kind()!=FixtureLighting.Kind.TELEVISION)return true;
        double dx=x-light.x(),dy=midY-light.y(),dz=z-light.z();
        double d2=dx*dx+dy*dy+dz*dz;
        double bound2=halfWidth*halfWidth+normalHalf*normalHalf
                +heightHalf*heightHalf;
        if(d2<=bound2+1e-6)return true; // source inside aperture bounding sphere
        double fx=light.dx(),fy=light.dy(),fz=light.dz();
        double direction2=fx*fx+fy*fy+fz*fz;
        if(direction2<.98||direction2>1.02)return true; // unknown beam calibration
        double cos=(fx*dx+fy*dy+fz*dz)/Math.sqrt(direction2*d2);
        cos=Math.max(-1,Math.min(1,cos));
        double sinA=Math.sqrt(bound2/d2);
        double threshold=light.kind()==FixtureLighting.Kind.FLUORESCENT?.48:0;
        // Max possible cos(theta) of any ray into the bounding sphere.
        if(cos>=threshold)return true;
        double best=cos*Math.sqrt(1-sinA*sinA)
                +Math.sqrt(Math.max(0,1-cos*cos))*sinA;
        return best>threshold-1e-7;
    }
    /** Overlapping regions (including vertically stacked rooms) may have
     * special floor/void/child-room ownership; leave them to exact rays. */
    private static boolean overlaps(Room a,Room b){
        return Math.max(a.x0(),b.x0())<Math.min(a.x1(),b.x1())
            &&Math.max(a.z0(),b.z0())<Math.min(a.z1(),b.z1());
    }
    /** Rebuild once per actual door/window snapshot change, not every frame.
     * Conservatively connect all outdoor zones: open exterior paths can link
     * indoor rooms through outside space even if their graph IDs differ. */
    void refresh(){
        String doors=map.doorSnapshot(),windows=map.windowSnapshot();
        if(doors.equals(lastDoors)&&windows.equals(lastWindows))return;
        lastDoors=doors;lastWindows=windows;rebuilds++;
        int n=connected.length;
        for(int i=0;i<n;i++)connected[i]=n>63?-1L:1L<<i;
        if(n>63){
            java.util.Arrays.fill(sourceReachable,-1L);
            sourceConfined=0;
            return;
        }
        for(int a=0;a<n;a++)if(map.level().room(a).outside())
            for(int b=0;b<n;b++)if(map.level().room(b).outside())
                connect(a,b);
        for(var p:map.level().portals()){
            int a=p.a(),b=p.b();
            if(a<0||b<0||a>=n||b>=n)continue;
            // Closed windows remain optically open in the current exact
            // ArchitecturalLightOcclusion implementation. Do not darken them.
            if(DoorwayTransmission.opticalTransmission(map,p,a,b)>0)connect(a,b);
        }
        // Stable small graph, transitive closure without per-ray work.
        for(int via=0;via<n;via++)
            for(int from=0;from<n;from++)
                if((connected[from]&(1L<<via))!=0)
                    connected[from]|=connected[via];
        // Source-specific bounded flood: an optical path must touch EVERY
        // opening along the route with this SAME original emitter. The global
        // connected graph is too generous: lamp A might reach door 1 but not
        // door 2, even when those doors connect the entire building.
        // This work is performed once per real topology revision, never in
        // the per-frame source-to-receiver rasterizer.
        sourceConfined=sourceBlockedConnectedRooms=0;
        for(var source:sources){
            int id=source.id(),from=source.room();
            if(id<0||id>=sourceReachable.length||!safeSource[id]){
                if(id>=0&&id<sourceReachable.length)sourceReachable[id]=-1L;
                continue; // unknown emitter ownership: retain exact rays
            }
            long reachable=1L<<from,frontier=reachable;
            while(frontier!=0){
                int current=Long.numberOfTrailingZeros(frontier);
                frontier&=frontier-1;
                // Adjacent outdoor zones are not separated by indoor walls.
                // Keep every external connection as a conservative bypass;
                // all indoor re-entries still require an actual in-range
                // optical aperture from the ORIGINAL light.
                if(map.level().room(current).outside()){
                    for(int other=0;other<n;other++){
                        if(!map.level().room(other).outside())continue;
                        long bit=1L<<other;
                        if((reachable&bit)!=0)continue;
                        reachable|=bit;frontier|=bit;
                    }
                }
                for(var portal:map.lightPortals(current)){
                    if(portal.a()!=current&&portal.b()!=current)continue;
                    int other=portal.a()==current?portal.b():portal.a();
                    if(other<0||other>=n||other==current)continue;
                    long bit=1L<<other;
                    if((reachable&bit)!=0)continue;
                    if(DoorwayTransmission.opticalTransmission(map,portal,current,other)<=0)
                        continue;
                    if(!touchesOpticalAperture(source,portal))continue;
                    reachable|=bit;frontier|=bit;
                }
            }
            sourceReachable[id]=reachable;
            sourceBlockedConnectedRooms+=Long.bitCount(connected[from]&~reachable);
            if(reachable==(1L<<from))sourceConfined++;
        }
    }
    private void connect(int a,int b){
        connected[a]|=1L<<b;
        connected[b]|=1L<<a;
    }
    /** True only when rejecting illumination is geometrically conservative.
     * Every uncertain case goes to the original full architectural raycast. */
    boolean disconnected(FixtureLighting.Source source,int receiverRoom,
                         double x,double y,double z){
        if(source==null||!interior(source.room())||!interior(receiverRoom)
                ||source.room()==receiverRoom||source.id()<0
                ||source.id()>=safeSource.length
                ||!safeSource[source.id()])return false;
        Room from=map.level().room(source.room()),to=map.level().room(receiverRoom);
        // Check the receiver's authored volume before ever eliminating a
        // candidate. Near join edges, overlap and ambiguous ownership use the
        // legacy source-to-surface ray and preserve original illumination.
        if(overlaps(from,to)||!inside(to,x,y,z,false))return false;
        return (sourceReachable[source.id()]&(1L<<receiverRoom))==0;
    }
    int rebuilds(){return rebuilds;}
    /** Count of safe authored emitters that cannot reach any transmissive
     * opening (and therefore never need an inter-room light ray). */
    int sourceConfined(){return sourceConfined;}
    /** Light/receiver-room pairs that the globally-connected graph would
     * have admitted, but per-source radius/cone propagation can reject. */
    int sourceBlockedConnectedRooms(){return sourceBlockedConnectedRooms;}
    boolean sourceCanReach(FixtureLighting.Source source,int room){
        return source!=null&&source.id()>=0&&source.id()<sourceReachable.length
            &&room>=0&&room<connected.length
            &&(sourceReachable[source.id()]&(1L<<room))!=0;
    }
    boolean connected(int a,int b){
        return a>=0&&b>=0&&a<connected.length&&b<connected.length
            &&(connected[a]&(1L<<b))!=0;
    }
}
