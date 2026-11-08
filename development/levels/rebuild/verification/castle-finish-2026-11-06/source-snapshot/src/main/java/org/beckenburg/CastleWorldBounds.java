package org.beckenburg;

/** Signed Castle authoring extent. Legacy nonnegative cell IDs remain byte-for-byte
 * stable; the west annex occupies appended IDs instead of aliasing old paint/nav. */
public final class CastleWorldBounds {
    private CastleWorldBounds(){}
    public static final int MIN_X=-18,MAX_X=44,MIN_Z=0,MAX_Z=36;
    public static final double MIN_Y=-3.5;
    public static final int LEGACY_CELLS=MAX_X*MAX_Z;
    public static final int CELL_COUNT=(MAX_X-MIN_X)*MAX_Z;
    public static boolean spawn(double x,double z,double yaw){return Double.isFinite(x+z+yaw)&&x>=MIN_X+1&&x<=MAX_X-2&&z>=MIN_Z+1&&z<=MAX_Z-2;}
    public static boolean cell(int x,int z){return x>=MIN_X&&x<MAX_X&&z>=MIN_Z&&z<MAX_Z;}
    public static int cellKey(int x,int z){
        if(!cell(x,z))throw new IllegalArgumentException("Tile outside castle");
        return x>=0?z*MAX_X+x:LEGACY_CELLS+z*(-MIN_X)+(-x-1);
    }
    public static boolean cellKeyValid(int key){return key>=0&&key<CELL_COUNT;}
    public static int cellX(int key){
        requireKey(key);return key<LEGACY_CELLS?key%MAX_X:-1-(key-LEGACY_CELLS)%(-MIN_X);
    }
    public static int cellZ(int key){
        requireKey(key);return key<LEGACY_CELLS?key/MAX_X:(key-LEGACY_CELLS)/(-MIN_X);
    }
    private static void requireKey(int key){if(!cellKeyValid(key))throw new IllegalArgumentException("Invalid Castle cell ID");}
}
