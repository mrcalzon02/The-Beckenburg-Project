package org.beckenburg;

import java.util.List;
import java.util.Objects;
import org.beckenburg.LevelDefinition.Room;

/**
 * Cached conservative doorway connectivity for static light sources. A room
 * whose portal component is disconnected cannot receive direct illumination
 * from the source room. Real ray tracing is ALWAYS retained for connected
 * rooms, boundary vertices, uncertain source placement, native maps and
 * exterior contacts; this never manufactures a virtual light or modifies
 * the final pixel shader.
 *
 * A HouseMap owns authored topology. Castle currently does not expose its
 * architectural regions as room IDs; therefore Castle continues through the
 * exact same FixtureLighting backend without applying this unsafe shortcut.
 */
final class PortalLightReachability {
    private static final double EDGE_MARGIN=LevelDefinition.WALL_HALF_THICKNESS+.10;
    private final HouseMap map;
    private final boolean[] safeSource;
    private final long[] connected;
    private int rebuilds;
    private String lastDoors,lastWindows;

    PortalLightReachability(HouseMap map,List<FixtureLighting.Source> sources){
        this.map=Objects.requireNonNull(map);
        int count=map.roomCount();
        safeSource=new boolean[sources.size()];
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
        for(int i=0;i<n;i++)connected[i]=1L<<i;
        if(n>63){for(int i=0;i<n;i++)connected[i]=-1L;return;}
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
        if((connected[source.room()]&(1L<<receiverRoom))!=0)return false;
        Room from=map.level().room(source.room()),to=map.level().room(receiverRoom);
        return !overlaps(from,to)&&inside(to,x,y,z,false);
    }
    int rebuilds(){return rebuilds;}
    boolean connected(int a,int b){
        return a>=0&&b>=0&&a<connected.length&&b<connected.length
            &&(connected[a]&(1L<<b))!=0;
    }
}
