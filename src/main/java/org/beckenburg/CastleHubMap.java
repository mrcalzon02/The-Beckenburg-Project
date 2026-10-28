package org.beckenburg;

/** Authored Beckenburg castle ground floor. All workstations have physical positions. */
public final class CastleHubMap {
    public static final int WIDTH=44, HEIGHT=36;
    private final SoundDocument sounds=SoundDocument.load("castle");
    public SoundDocument sounds(){return sounds;}
    private final dev.mrcalzon.water.WaterDocument water=WorldWater.load("castle");
    public dev.mrcalzon.water.WaterDocument water(){return water;}
    private final char[][] grid=new char[HEIGHT][WIDTH];
    /** All navigable ground cells, including the entrance apron. Immutable so
     * the renderer never needs to rediscover the map or drop distant tiles. */
    public record GroundCell(int x,int z){}
    private final java.util.List<GroundCell> groundCells;
    public java.util.List<GroundCell> groundCells(){return groundCells;}
    private final java.util.Set<String> openDoors=new java.util.HashSet<>();
    private long doorRevision;
    public long doorRevision(){return doorRevision;}
    /** Gate and door state belongs to the playable map, not a freestanding mesh. */
    public boolean doorOpen(String slotId){CastleDoorSlots.require(slotId);return openDoors.contains(slotId);}
    public boolean toggleDoor(String slotId){
        CastleDoorSlots.require(slotId);
        if(CastleLevelEdits.current().doorType(slotId)==CastleDoorSlots.Type.EMPTY)
            throw new IllegalArgumentException("Opening has no installed door");
        if(!openDoors.add(slotId))openDoors.remove(slotId);
        doorRevision++;
        return openDoors.contains(slotId);
    }
    public CastleDoorSlots.Slot aimedDoor(double x,double z,double yaw){return aimedDoor(x,0,z,yaw);}
    public CastleDoorSlots.Slot aimedDoor(double x,double elevation,double z,double yaw){
        if(!Double.isFinite(x+z+yaw))return null;
        CastleDoorSlots.Slot nearest=null;double best=0;
        for(var slot:CastleDoorSlots.all()){
            if(Math.abs(elevation-slot.bottom())>.6||CastleLevelEdits.current().doorType(slot.id())==CastleDoorSlots.Type.EMPTY)continue;
            double dx=slot.centerX()-x,dz=slot.centerZ()-z,d=Math.hypot(dx,dz);
            if(d<.35||d>2.8)continue;
            double facing=(Math.sin(yaw)*dx+Math.cos(yaw)*dz)/d;
            if(facing>.72&&facing>best){best=facing;nearest=slot;}
        }
        return nearest;
    }
    private boolean closedLeafBlocks(double x,double y,double z,double r){
        for(var slot:CastleDoorSlots.all()){
            var type=CastleLevelEdits.current().doorType(slot.id());
            if(type==CastleDoorSlots.Type.WOOD){if(CastleTimberDoors.blocks(slot,doorOpen(slot.id()),x,y,z,r))return true;}
            else if(type!=CastleDoorSlots.Type.EMPTY&&!doorOpen(slot.id())&&CastleDoorSlots.leafBlocks(slot,x,y,z,r))return true;
        }
        return false;
    }
    private final java.util.Set<String> darkCircuits=new java.util.HashSet<>();
    public boolean lightOn(double x,double y,double z){return !darkCircuits.contains(CastleRooms.circuit(x,y,z));}
    public boolean toggleCircuit(CastleHubDecor.Switch sw){
        String id=CastleRooms.circuit(sw.x(),sw.y(),sw.z());
        if(!darkCircuits.add(id))darkCircuits.remove(id);
        return !darkCircuits.contains(id);
    }
    public enum Station {
        ARMORY_UV(5.2,4.5,"QUARTERMASTER / EQUIPMENT STORE"),
        ARMORY_SALT(5.2,8.0,"QUARTERMASTER / SUPPLY STORE"),
        ROLE(23.0,6.0,"HUNTER WARDROBE / CHANGE ROLE"),
        CONTRACT(13.5,6.8,"CAMPAIGN CONTRACT BOARD"),
        EXPEDITION(13.5,16.6,"EXPEDITION VAN / START CASE"),
        MULTIPLAYER(22.5,16.6,"NETWORK DESK / HOST OR JOIN"),
        SPECTRAL(5.0,16.6,"SPECTRAL PRACTICE"),
        ARCHIVE(36.0,11.8,"MATTHIS ROOK / COMMON ARCHIVE");
        public final double x,z;public final String label;
        Station(double x,double z,String label){this.x=x;this.z=z;this.label=label;}
    }
    /** Small, authored real GLB furnishings with matching walkable collision footprints. */
    public record Furnishing(String mesh,double x,double z,double scale,double yaw,
                             double halfWidth,double halfDepth) { }
    public static java.util.List<Furnishing> furnishings(){
        var all=new java.util.ArrayList<>(CastleLevelEdits.current().furnishings(false));
        all.addAll(CastleArchive.furnishings());return java.util.List.copyOf(all);
    }
    /** Articulated H-plan with real hall passages, two connected eastern service
     * wings, a covered north block and an open south courtyard.
     * Purchasing stations retain their physically reachable world coordinates.
     */
    public CastleHubMap(){
        for(char[] row:grid)java.util.Arrays.fill(row,'#');
        for(int z=0;z<HEIGHT;z++)for(int x=0;x<WIDTH;x++)
            if(CastleRooms.ground(x+.5,z+.5))grid[z][x]='.';
        var cells=new java.util.ArrayList<GroundCell>();
        for(int z=0;z<HEIGHT;z++)for(int x=0;x<WIDTH;x++)if(!solid(x,z))cells.add(new GroundCell(x,z));
        groundCells=java.util.List.copyOf(cells);

    }
    private void carve(int x0,int z0,int x1,int z1){
        for(int z=z0;z<=z1;z++)for(int x=x0;x<=x1;x++)grid[z][x]='.';
    }
    /** Courtyard paving is open to the sky; both lateral wings remain indoors. */
    public boolean outside(double x,double z){
        var room=CastleRooms.at(x,0,z);return room==null||room.outside();
    }
    /** One source of truth for displayed flooring and physical footfall Foley. */
    public int floorMaterial(double x,double z){
        if(!Double.isFinite(x)||!Double.isFinite(z)||
                solid((int)Math.floor(x),(int)Math.floor(z)))
            throw new IllegalArgumentException("floor location must be navigable");
        var room=CastleRooms.at(x,0,z);
        int nativeMaterial=room==null?MaterialAtlas.STONE:room.material();
        return CastleLevelEdits.current().floorMaterial((int)Math.floor(x),
            (int)Math.floor(z),nativeMaterial);
    }
    public LanSound.Cue footstep(double x,double z){
        return footstep(x,z,0);
    }
    public LanSound.Cue footstep(double x,double z,double elevation){
        if(elevation>2.0&&CastleHubArchitecturalPlan.upperWalkable(x,z))
            return z<=10.7?LanSound.Cue.STEP_WOOD:LanSound.Cue.STEP_STONE;
        if(CastleHubArchitecturalPlan.onStairs(x,z))
            return LanSound.Cue.STEP_STONE;
        return switch(floorMaterial(x,z)){
            case MaterialAtlas.CARPET->LanSound.Cue.STEP_CARPET;
            case MaterialAtlas.PARQUET,MaterialAtlas.AGED_WOOD->LanSound.Cue.STEP_WOOD;
            default->LanSound.Cue.STEP_STONE;
        };
    }

    public boolean valid(){
        java.util.HashSet<Integer> seen=new java.util.HashSet<>();
        java.util.ArrayDeque<int[]> queue=new java.util.ArrayDeque<>();
        queue.add(new int[]{13,19});
        while(!queue.isEmpty()){
            int[] point=queue.removeFirst();
            int x=point[0],z=point[1],key=z*WIDTH+x;
            if(x<0||x>=WIDTH||z<0||z>=HEIGHT||solid(x,z)||!seen.add(key))continue;
            queue.add(new int[]{x+1,z});queue.add(new int[]{x-1,z});
            queue.add(new int[]{x,z+1});queue.add(new int[]{x,z-1});
        }
        for(int z=0;z<HEIGHT;z++)for(int x=0;x<WIDTH;x++)
            if(!solid(x,z)&&!seen.contains(z*WIDTH+x))return false;
        for(Station station:Station.values())
            if(solid((int)station.x,(int)station.z))return false;
        return true;
    }
    public char tile(int x,int z){return x<0||z<0||x>=WIDTH||z>=HEIGHT?'#':grid[z][x];}
    public boolean solid(int x,int z){return tile(x,z)=='#';}

    public boolean traversable(double x,double z){return groundTraversable(x,z,0);}
    private boolean groundTraversable(double x,double z,double elevation){
        if(CastleHubArchitecturalPlan.onStairs(x,z)&&elevation+.10<CastleHubArchitecturalPlan.stairHeight(x,z))return false;
        if(!Double.isFinite(x)||!Double.isFinite(z))return false;
        double r=.21;
        if(!CastleRooms.ground(x-r,z-r)||!CastleRooms.ground(x+r,z-r)
            ||!CastleRooms.ground(x-r,z+r)||!CastleRooms.ground(x+r,z+r))return false;
        var edits=CastleLevelEdits.current();
        if(edits.navBlocked((int)Math.floor(x),(int)Math.floor(z))
            ||edits.wallBlocks(x,elevation,z,r)
            ||CastleSurroundings.courtyardVanBlocks(x,z,r)
            ||CastleWarehouse.blocks(x,z,r)
            ||CastleServiceFixtures.blocks(x,z,r)
            ||closedLeafBlocks(x,elevation,z,r))return false;
        for(Furnishing item:furnishings())
            if(Math.abs(x-item.x())<item.halfWidth()+r &&
               Math.abs(z-item.z())<item.halfDepth()+r)return false;
        return true;
    }
    /** True three-dimensional walk path: landings and upper corridors ignore
     * solid ground-wall cells only when physically above their lintels. */
    public boolean traversable(double x,double z,double elevation){
        if(!Double.isFinite(elevation)||elevation<0)return false;
        double radius=.21;
        if(CastleHubArchitecturalPlan.onStairs(x,z) && elevation>.1
                && (x<16+radius||x>19-radius))return false;
        if(elevation>2.0){
            if(CastleHubArchitecturalPlan.upperWalkable(x,z)){
                if(!CastleHubArchitecturalPlan.upperClearance(x,z,radius)
                    ||CastleRooms.guardBlocks(x,z,radius)
                    ||CastleLevelEdits.current().wallBlocks(x,elevation,z,radius)
                    ||closedLeafBlocks(x,elevation,z,radius))
                    return false;
                for(Furnishing item:CastleLevelEdits.current().furnishings(true))
                    if(Math.abs(x-item.x())<item.halfWidth()+radius&&
                        Math.abs(z-item.z())<item.halfDepth()+radius)return false;
                return true;
            }
            // Elevated space is not ground navigation. Only the authored
            // staircase can support a body more than 2m above the floor.
            if(!CastleHubArchitecturalPlan.onStairs(x,z)||
                Math.abs(elevation-CastleHubArchitecturalPlan.stairHeight(x,z))>.55)
                return false;
        }
        return groundTraversable(x,z,elevation);
    }
    /** Interaction uses actual position and camera orientation, not a free-floating button. */
    public Station aimed(double playerX,double playerZ,double yaw){
        if(!traversable(playerX,playerZ)||!Double.isFinite(yaw))return null;
        Station nearest=null;double best=-1;
        for(Station s:Station.values()){
            double[] location=CastleLevelEdits.current().station(s);
            double dx=location[0]-playerX,dz=location[1]-playerZ,d=Math.hypot(dx,dz);
            if(d>.5&&d<=2.75){
                double forward=(dx*Math.sin(yaw)+dz*Math.cos(yaw))/d;
                if(forward>.77&&forward>best&&FixtureLighting.castleRayClear(this,playerX,1.62,playerZ,location[0],.9,location[1])){best=forward;nearest=s;}
            }
        }
        return nearest;
    }
}
