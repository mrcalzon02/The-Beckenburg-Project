package org.beckenburg;

import java.util.List;

/**
 * Shared coordinates and paging for the isolated multi-storey DEV review room.
 * Not a gameplay map, collision volume, scene-save format or asset approval list.
 *
 * Each wing has three physical 4.4m storeys and four sequential six-stand bays
 * per storey. Only the nearby meshes need to be decoded for a given camera.
 */
public final class DevAssetGalleryLayout {
    private DevAssetGalleryLayout(){}
    public static final int SLOTS_PER_BAY=6;
    public static final int BAYS_PER_FLOOR=4;
    public static final int FLOORS=3;
    public static final int SLOTS_PER_FLOOR=SLOTS_PER_BAY*BAYS_PER_FLOOR;
    public static final int SLOTS_PER_WING=SLOTS_PER_FLOOR*FLOORS;
    public static final double FLOOR_HEIGHT_M=4.4;
    public static final double BAY_DEPTH_M=16.0;
    public static final double MODEL_RADIUS_M=19.0;

    public record Address(int wing,int floor,int bay,int slot){
        public Address {
            if(wing<0||floor<0||floor>=FLOORS||bay<0||bay>=BAYS_PER_FLOOR||
                slot<0||slot>=SLOTS_PER_BAY)
                throw new IllegalArgumentException("Invalid DEV gallery address");
        }
        public int absoluteIndex(){
            return wing*SLOTS_PER_WING+floor*SLOTS_PER_FLOOR+
                bay*SLOTS_PER_BAY+slot;
        }
        public int localIndex(){
            return floor*SLOTS_PER_FLOOR+bay*SLOTS_PER_BAY+slot;
        }
    }

    public static Address address(int absoluteIndex){
        if(absoluteIndex<0)throw new IllegalArgumentException("Negative gallery index");
        int local=absoluteIndex%SLOTS_PER_WING;
        return new Address(absoluteIndex/SLOTS_PER_WING,
            local/SLOTS_PER_FLOOR,
            (local%SLOTS_PER_FLOOR)/SLOTS_PER_BAY,local%SLOTS_PER_BAY);
    }
    public static int clampFloor(int floor){return Math.max(0,Math.min(FLOORS-1,floor));}
    public static int clampBay(int bay){return Math.max(0,Math.min(BAYS_PER_FLOOR-1,bay));}
    public static int floorAtEye(double y){
        if(!Double.isFinite(y))throw new IllegalArgumentException("Nonfinite gallery eye");
        return clampFloor((int)Math.floor(Math.max(0,y)/FLOOR_HEIGHT_M));
    }
    public static int bayAtZ(double z){
        if(!Double.isFinite(z))throw new IllegalArgumentException("Nonfinite gallery position");
        // Entering a bay starts five metres before its first exhibit row.
        // The boundary at Z=10.75 lies AFTER bay 1's rear stand (10.5)
        // and BEFORE bay 2's entrance (11.0); do not mislabel arrival.
        return clampBay((int)Math.floor((z+5.25)/BAY_DEPTH_M));
    }
    public static double floorY(int floor){return clampFloor(floor)*FLOOR_HEIGHT_M;}
    public static double standX(int slot){
        if(slot<0||slot>=SLOTS_PER_BAY)throw new IllegalArgumentException("stand");
        return (slot%3-1)*4.6;
    }
    public static double standZ(int bay,int slot){
        if(bay<0||bay>=BAYS_PER_FLOOR||slot<0||slot>=SLOTS_PER_BAY)
            throw new IllegalArgumentException("stand");
        return (slot/3==0?4.5:10.5)+bay*BAY_DEPTH_M;
    }
    public static double eyeY(int floor){return floorY(floor)+1.65;}
    /** Stage camera starts six metres before its first exhibit row,
     * with four metres of clear space behind the previous bay's rear row.
     * The prior -5m origin put bay >=2 cameras only 0.5m from the
     * preceding row, inside wide 3D machinery (full-screen model occlusion).
     */
    public static double bayEntranceZ(int bay){
        return -1.5+clampBay(bay)*BAY_DEPTH_M;
    }
    public static <T> List<T> wing(List<T> items,int index){
        if(index<0)throw new IllegalArgumentException("wing");
        long start=(long)index*SLOTS_PER_WING;
        if(start>=items.size())return List.of();
        int lo=(int)start;
        return List.copyOf(items.subList(lo,Math.min(items.size(),lo+SLOTS_PER_WING)));
    }
    public static <T> List<T> bay(List<T> items,int floor,int bay){
        int start=clampFloor(floor)*SLOTS_PER_FLOOR+clampBay(bay)*SLOTS_PER_BAY;
        if(start>=items.size())return List.of();
        return List.copyOf(items.subList(start,Math.min(items.size(),start+SLOTS_PER_BAY)));
    }
}
