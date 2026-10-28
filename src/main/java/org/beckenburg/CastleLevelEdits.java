package org.beckenburg;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

/** Real, persistent castle geometry/scene authoring data. Game and editor
 * consume exactly the same snapshot; this is not a preview-only asset list.
 * Changes are saved outside the installation and survive software updates.
 */
public final class CastleLevelEdits {
    public record Prop(String id,String mesh,double x,double y,double z,
                       double scale,double yaw,double halfX,double halfZ,boolean upper) {
        public Prop {
            if(!id.matches("[A-Za-z0-9_-]{1,45}")||!modelAllowed(mesh)
                ||!Double.isFinite(x+y+z+scale+yaw+halfX+halfZ)
                ||scale<=0||scale>10||halfX<0||halfZ<0||halfX>10||halfZ>10
                ||x<0||x>CastleHubMap.WIDTH||z<0||z>CastleHubMap.HEIGHT
                ||y<-.5||y>CastleHubArchitecturalPlan.UPPER_CEILING+2)
                throw new IllegalArgumentException("Invalid editable prop "+id);
        }
    }
    public record Lamp(String id,String on,String off,double x,double y,double z,
                       double scale,double yaw,CastleHubDecor.Mount mount,
                       double radius,double power) {
        public Lamp {
            if(!id.matches("[A-Za-z0-9_-]{1,45}")
                ||!modelAllowed(on)||!modelAllowed(off)||mount==null
                ||!Double.isFinite(x+y+z+scale+yaw+radius+power)
                ||x<0||x>CastleHubMap.WIDTH||z<0||z>CastleHubMap.HEIGHT
                ||y<0||y>9||scale<=0||scale>4||radius<1||radius>24
                ||power<0||power>10)
                throw new IllegalArgumentException("Invalid editable lamp "+id);
        }
        public CastleHubDecor.Light fixture(){
            return new CastleHubDecor.Light(on,off,x,y,z,scale,yaw,mount);
        }
    }
    /** Axis-aligned editable architectural solids. Wall is a line extrusion,
     * floor is a horizontal slab, roof is a ridge with editable pitch. */
    public enum Kind { WALL, FLOOR, ROOF }
    public record Surface(String id,Kind kind,double x0,double z0,double x1,double z1,
                          double base,double height,int material) {
        public Surface {
            if(id==null||!id.matches("[A-Za-z0-9_-]{1,45}")||kind==null
               ||!Double.isFinite(x0+z0+x1+z1+base+height)
               ||x0<0||z0<0||x1>CastleHubMap.WIDTH||z1>CastleHubMap.HEIGHT
               ||x1<x0||z1<z0||base<-.5||base>12||height<=0||height>12
               ||material<0||material>=MaterialAtlas.COUNT
               ||(kind==Kind.WALL
                   ? !((x1-x0>.15&&z1==z0)||(z1-z0>.15&&x1==x0))
                   : x1-x0<.2||z1-z0<.2))
                throw new IllegalArgumentException("Invalid architectural surface "+id);
        }
        public double cx(){return (x0+x1)*.5;}
        public double cz(){return (z0+z1)*.5;}
        public double width(){return x1-x0;}
        public double depth(){return z1-z0;}
        public CastleHubArchitecturalPlan.Wall wall(){
            if(kind!=Kind.WALL)throw new IllegalStateException("Not a wall");
            return new CastleHubArchitecturalPlan.Wall(x0,z0,x1,z1,base,base+height,
                material,Double.NaN,0);
        }
        public CastleHubArchitecturalPlan.Deck deck(){
            if(kind!=Kind.FLOOR)throw new IllegalStateException("Not a floor");
            return new CastleHubArchitecturalPlan.Deck(x0,z0,x1,z1,base,material);
        }
        public CastleHubArchitecturalPlan.Roof roof(){
            if(kind!=Kind.ROOF)throw new IllegalStateException("Not a roof");
            return new CastleHubArchitecturalPlan.Roof(x0,x1,z0,z1,base,height,material);
        }
    }
    private final Map<String,Surface> surfaces=new LinkedHashMap<>();
    private final Map<String,CastleDoorSlots.Type> doorAssignments=new LinkedHashMap<>();
    public synchronized CastleDoorSlots.Type doorType(String slotId){
        CastleDoorSlots.require(slotId);
        return doorAssignments.getOrDefault(slotId,CastleDoorSlots.Type.EMPTY);
    }
    /** One and only one mounted leaf in each architectural socket. */
    public synchronized void installDoor(String slotId,CastleDoorSlots.Type type){
        var slot=CastleDoorSlots.require(slotId);
        Objects.requireNonNull(type);
        if(type==CastleDoorSlots.Type.EMPTY)throw new IllegalArgumentException("Use removeDoor to leave an arch");
        if(slot.gate()!=(type==CastleDoorSlots.Type.PORTCULLIS))
            throw new IllegalArgumentException("Door type incompatible with physical socket");
        if(doorType(slotId)!=CastleDoorSlots.Type.EMPTY)
            throw new IllegalStateException("Doorway already occupied: "+slotId);
        doorAssignments.put(slotId,type);revision++;
    }
    public synchronized void removeDoor(String slotId){
        CastleDoorSlots.require(slotId);
        if(doorAssignments.remove(slotId)!=null)revision++;
    }
    private long geometryCacheVersion=-1;
    private List<CastleHubArchitecturalPlan.Wall> cachedWalls=List.of();
    private List<CastleHubArchitecturalPlan.Deck> cachedDecks=List.of();
    private List<CastleHubArchitecturalPlan.Roof> cachedRoofs=List.of();
    public synchronized List<Surface> surfaces(){return List.copyOf(surfaces.values());}
    /** Cached immutable collision/shadow primitives: per-pixel light rays must
     * never rebuild or allocate entire geometry lists while rendering. */
    private void ensureGeometryCache(){
        if(geometryCacheVersion==revision)return;
        List<CastleHubArchitecturalPlan.Wall> wallList=new ArrayList<>();
        List<CastleHubArchitecturalPlan.Deck> deckList=new ArrayList<>();
        List<CastleHubArchitecturalPlan.Roof> roofList=new ArrayList<>();
        for(var surface:surfaces.values())switch(surface.kind()){
            case WALL -> wallList.add(surface.wall());
            case FLOOR -> deckList.add(surface.deck());
            case ROOF -> roofList.add(surface.roof());
        }
        for(var marker:CastleHubArchitecturalPlan.walls())
            if(marker.hasDoor())wallList.add(marker);
        cachedWalls=List.copyOf(wallList);
        cachedDecks=List.copyOf(deckList);
        cachedRoofs=List.copyOf(roofList);
        geometryCacheVersion=revision;
    }
    public synchronized List<CastleHubArchitecturalPlan.Wall> walls(){
        ensureGeometryCache();return cachedWalls;
    }
    public synchronized List<CastleHubArchitecturalPlan.Deck> decks(){
        ensureGeometryCache();return cachedDecks;
    }
    public synchronized List<CastleHubArchitecturalPlan.Roof> roofs(){
        ensureGeometryCache();return cachedRoofs;
    }
    public synchronized void put(Surface surface){
        Objects.requireNonNull(surface);
        if(surfaces.size()>=512&&!surfaces.containsKey(surface.id()))
            throw new IllegalStateException("Castle architectural surface limit");
        surfaces.put(surface.id(),surface);revision++;
    }
    public synchronized void removeSurface(String id){
        if(surfaces.remove(id)!=null)revision++;
    }
    /** Real wall collision shared with game navigation and light blockers. */
    public synchronized boolean wallBlocks(double x,double y,double z,double radius){
        for(Surface s:surfaces.values())if(s.kind()==Kind.WALL &&
            CastleDoorSlots.containsWall(s.wall(),x,y,z,radius))return true;
        return false;
    }
    public synchronized boolean floorSupports(double x,double z,double height){
        for(Surface s:surfaces.values())if(s.kind()==Kind.FLOOR
            &&Math.abs(s.base()-height)<.18&&x>=s.x0()&&x<=s.x1()
            &&z>=s.z0()&&z<=s.z1())return true;
        return false;
    }
    /** Geometry undo is a complete in-memory scene snapshot, not a partial UI undo. */
    public record Snapshot(List<Prop> props,List<Lamp> lamps,List<Surface> surfaces,
        Map<String,double[]> stations,Map<Integer,Integer> floorPaint,
        Map<Integer,Integer> wallPaint,Set<Integer> navBlocked,
        Map<String,CastleDoorSlots.Type> doors,
        double spawnX,double spawnZ,double spawnYaw){}
    public synchronized Snapshot snapshot(){
        Map<String,double[]> coords=new LinkedHashMap<>();
        for(var item:stations.entrySet())coords.put(item.getKey().name(),item.getValue().clone());
        return new Snapshot(List.copyOf(props.values()),List.copyOf(lamps.values()),
            List.copyOf(surfaces.values()),coords,Map.copyOf(floorPaint),
            Map.copyOf(wallPaint),Set.copyOf(navBlocked),
            Map.copyOf(doorAssignments),spawnX,spawnZ,spawnYaw);
    }
    public synchronized void restore(Snapshot snapshot){
        Objects.requireNonNull(snapshot);
        props.clear();for(var p:snapshot.props())props.put(p.id(),p);
        lamps.clear();for(var l:snapshot.lamps())lamps.put(l.id(),l);
        surfaces.clear();for(var s:snapshot.surfaces())surfaces.put(s.id(),s);
        stations.clear();for(var st:CastleHubMap.Station.values())
            stations.put(st,snapshot.stations().get(st.name()).clone());
        floorPaint.clear();floorPaint.putAll(snapshot.floorPaint());
        wallPaint.clear();wallPaint.putAll(snapshot.wallPaint());
        navBlocked.clear();navBlocked.addAll(snapshot.navBlocked());
        doorAssignments.clear();doorAssignments.putAll(snapshot.doors());
        spawnX=snapshot.spawnX();spawnZ=snapshot.spawnZ();spawnYaw=snapshot.spawnYaw();
        revision++;
    }
    private static final CastleLevelEdits DEFAULT=new CastleLevelEdits(defaultPath());
    private final Path path;
    private final Map<String,Prop> props=new LinkedHashMap<>();
    private final Map<String,Lamp> lamps=new LinkedHashMap<>();
    private final EnumMap<CastleHubMap.Station,double[]> stations=
        new EnumMap<>(CastleHubMap.Station.class);
    private final Map<Integer,Integer> floorPaint=new HashMap<>();
    private final Map<Integer,Integer> wallPaint=new HashMap<>();
    private final Set<Integer> navBlocked=new java.util.HashSet<>();
    private double spawnX=13.5,spawnZ=19.3,spawnYaw=Math.PI;
    private long revision=1;
    public static CastleLevelEdits current(){return DEFAULT;}
    private static Path defaultPath(){
        return Path.of(System.getProperty("user.home"),".beckenburg","editor","castle.properties");
    }
    public CastleLevelEdits(Path path) {
        this.path=Objects.requireNonNull(path).toAbsolutePath().normalize();
        defaults();
        if(Files.isRegularFile(path))try{load();}catch(IOException e){
            throw new IllegalStateException("Castle editor overrides cannot load: "+path,e);
        }
    }
    private static boolean modelAllowed(String mesh){
        return mesh!=null&&GlbProps.availableModels().contains(mesh);
    }
    private void defaults(){
        surfaces.clear();doorAssignments.clear();
        for(var slot:CastleDoorSlots.all())
            if(CastleDoorSlots.initial(slot)!=CastleDoorSlots.Type.EMPTY)
                doorAssignments.put(slot.id(),CastleDoorSlots.initial(slot));
        int surfaceId=0;
        for(var wall:CastleHubArchitecturalPlan.walls()){
            int index=surfaceId++;
            if(!wall.hasDoor())surfaces.put("wall"+index,
                new Surface("wall"+index,Kind.WALL,
                    Math.min(wall.x0(),wall.x1()),Math.min(wall.z0(),wall.z1()),
                    Math.max(wall.x0(),wall.x1()),Math.max(wall.z0(),wall.z1()),
                    wall.bottom(),wall.top()-wall.bottom(),wall.material()));
        }
        surfaceId=0;
        for(var deck:CastleHubArchitecturalPlan.decks()){
            String id="floor"+surfaceId++;
            surfaces.put(id,new Surface(id,Kind.FLOOR,
                deck.x0(),deck.z0(),deck.x1(),deck.z1(),deck.height(),CastleRooms.SLAB,deck.material()));
        }
        surfaceId=0;
        for(var roof:CastleHubArchitecturalPlan.roofs()){
            String id="roof"+surfaceId++;
            surfaces.put(id,new Surface(id,Kind.ROOF,roof.west(),roof.north(),
                roof.east(),roof.south(),roof.eave(),roof.rise(),roof.material()));
        }
        props.clear();lamps.clear();stations.clear();floorPaint.clear();
        wallPaint.clear();navBlocked.clear();
        final int LEGACY_LIGHT_COUNT=17;
        int n=0;
        for(var f:CastleHubDecor.furnishings())
            { int id=n++;if(f.x()<28.5||f.z()<17.5)props.put("ground"+id,fromFurnishing("ground"+id,f,false,0)); }
        n=0;
        for(var f:CastleHubDecor.UPPER_FURNISHINGS)
            props.put("upper"+n,fromFurnishing("upper"+n++,f,true,
                CastleHubArchitecturalPlan.GROUND_CEILING));
        n=0;
        for(var f:CastleHubDecor.LIGHTS){
            String id=n<LEGACY_LIGHT_COUNT?"fixture"+n:"lighting2_"+(n-LEGACY_LIGHT_COUNT);
            n++;
            boolean chandelier=f.on().contains("ceiling_lamp_4");
            lamps.put(id,new Lamp(id,f.on(),f.off(),f.x(),f.y(),f.z(),
                f.scale(),f.yaw(),f.mount(),chandelier?10.5:9.8,
                chandelier?4.2:4.0));
        }
        for(var station:CastleHubMap.Station.values())
            stations.put(station,new double[]{station.x,station.z});
        spawnX=13.5;spawnZ=19.3;spawnYaw=Math.PI;
    }
    private static Prop fromFurnishing(String id,CastleHubMap.Furnishing f,
                                       boolean upper,double y){
        return new Prop(id,f.mesh(),f.x(),y,f.z(),f.scale(),f.yaw(),
            f.halfWidth(),f.halfDepth(),upper);
    }
    private static int tile(int x,int z){return z*CastleHubMap.WIDTH+x;}
    public synchronized List<Prop> props(){return List.copyOf(props.values());}
    public synchronized List<Lamp> lamps(){return List.copyOf(lamps.values());}
    public synchronized long revision(){return revision;}
    public synchronized List<CastleHubMap.Furnishing> furnishings(boolean upper){
        List<CastleHubMap.Furnishing> out=new ArrayList<>();
        for(var p:props.values())if(p.upper()==upper)
            out.add(new CastleHubMap.Furnishing(p.mesh(),p.x(),p.z(),p.scale(),
                p.yaw(),p.halfX(),p.halfZ()));
        return List.copyOf(out);
    }
    public synchronized double[] spawn(){return new double[]{spawnX,spawnZ,spawnYaw};}
    public synchronized double[] station(CastleHubMap.Station station){
        return stations.get(station).clone();
    }
    public synchronized void setStation(CastleHubMap.Station station,double x,double z){
        if(!Double.isFinite(x+z)||x<1||z<1||x>42||z>34)
            throw new IllegalArgumentException("Station must be within castle");
        stations.put(Objects.requireNonNull(station),new double[]{x,z});revision++;
    }
    public synchronized void setSpawn(double x,double z,double yaw){
        if(!Double.isFinite(x+z+yaw)||x<1||x>42||z<1||z>34)
            throw new IllegalArgumentException("Invalid playable spawn");
        spawnX=x;spawnZ=z;spawnYaw=yaw;revision++;
    }
    public synchronized void put(Prop prop){
        Objects.requireNonNull(prop);if(props.size()>=512&&!props.containsKey(prop.id()))
            throw new IllegalStateException("Castle prop count exceeded");
        props.put(prop.id(),prop);revision++;
    }
    public synchronized void removeProp(String id){if(props.remove(id)!=null)revision++;}
    public synchronized void put(Lamp light){
        Objects.requireNonNull(light);if(lamps.size()>=192&&!lamps.containsKey(light.id()))
            throw new IllegalStateException("Castle light limit exceeded");
        lamps.put(light.id(),light);revision++;
    }
    public synchronized void removeLamp(String id){if(lamps.remove(id)!=null)revision++;}
    public synchronized int floorMaterial(int x,int z,int fallback){
        return floorPaint.getOrDefault(tile(x,z),fallback);
    }
    public synchronized int wallMaterial(int index,int fallback){
        return wallPaint.getOrDefault(index,fallback);
    }
    public synchronized void paintFloor(int x,int z,int material){
        checkTile(x,z);checkMaterial(material);floorPaint.put(tile(x,z),material);revision++;
    }
    public synchronized void paintWall(int index,int material){
        if(index<0||index>=CastleHubArchitecturalPlan.walls().size())
            throw new IllegalArgumentException("Invalid wall");
        checkMaterial(material);wallPaint.put(index,material);
        String key="wall"+index;
        Surface old=surfaces.get(key);
        if(old!=null)surfaces.put(key,new Surface(old.id(),old.kind(),old.x0(),old.z0(),
            old.x1(),old.z1(),old.base(),old.height(),material));
        revision++;
    }
    public synchronized boolean navBlocked(int x,int z){
        return navBlocked.contains(tile(x,z));
    }
    public synchronized void setNavBlocked(int x,int z,boolean blocked){
        checkTile(x,z);if(blocked)navBlocked.add(tile(x,z));
        else navBlocked.remove(tile(x,z));revision++;
    }
    private static void checkTile(int x,int z){
        if(x<0||x>=CastleHubMap.WIDTH||z<0||z>=CastleHubMap.HEIGHT)
            throw new IllegalArgumentException("Tile outside castle");
    }
    private static void checkMaterial(int material){
        if(material<0||material>=MaterialAtlas.COUNT)
            throw new IllegalArgumentException("Invalid texture tile");
    }
    public synchronized void reset(){defaults();revision++;}
    private static double number(Properties p,String name){
        return Double.parseDouble(Objects.requireNonNull(p.getProperty(name),name));
    }
    private static int integer(Properties p,String name){
        return Integer.parseInt(Objects.requireNonNull(p.getProperty(name),name));
    }
    private void load()throws IOException{
        Properties p=new Properties();
        try(InputStream in=Files.newInputStream(path)){p.load(in);}
        if(!"1".equals(p.getProperty("format")))
            throw new IOException("Unsupported castle editor document version");
        if(Integer.parseInt(p.getProperty("structure.layout","1"))<2){
            Path backup=path.resolveSibling(path.getFileName()+".before-structure-v2.properties");
            if(!Files.exists(backup))Files.copy(path,backup);
            defaults();save();return;
        }
        try{
            Map<String,Prop> incomingProps=new LinkedHashMap<>();
            Map<String,Lamp> incomingLights=new LinkedHashMap<>();
            int count=integer(p,"props.count");
            if(count<0||count>512)throw new IllegalArgumentException("Too many props");
            for(int i=0;i<count;i++){
                String k="props."+i+".";
                String id=p.getProperty(k+"id"),mesh=p.getProperty(k+"mesh");
                var item=new Prop(id,mesh,number(p,k+"x"),number(p,k+"y"),
                    number(p,k+"z"),number(p,k+"scale"),number(p,k+"yaw"),
                    number(p,k+"halfX"),number(p,k+"halfZ"),
                    Boolean.parseBoolean(p.getProperty(k+"upper")));
                if(incomingProps.putIfAbsent(id,item)!=null)
                    throw new IllegalArgumentException("Duplicate prop "+id);
            }
            count=integer(p,"lamps.count");
            if(count<0||count>192)throw new IllegalArgumentException("Too many lights");
            for(int i=0;i<count;i++){
                String k="lamps."+i+".",id=p.getProperty(k+"id");
                var lamp=new Lamp(id,p.getProperty(k+"on"),p.getProperty(k+"off"),
                    number(p,k+"x"),number(p,k+"y"),number(p,k+"z"),
                    number(p,k+"scale"),number(p,k+"yaw"),
                    CastleHubDecor.Mount.valueOf(p.getProperty(k+"mount")),
                    number(p,k+"radius"),number(p,k+"power"));
                if(incomingLights.putIfAbsent(id,lamp)!=null)
                    throw new IllegalArgumentException("Duplicate lamp "+id);
            }
            var newStations=new EnumMap<CastleHubMap.Station,double[]>(CastleHubMap.Station.class);
            for(var st:CastleHubMap.Station.values())
                newStations.put(st,new double[]{number(p,"station."+st+".x"),
                    number(p,"station."+st+".z")});
            var newFloor=new HashMap<Integer,Integer>();var newWalls=new HashMap<Integer,Integer>();
            var newNav=new java.util.HashSet<Integer>();
            for(String key:p.stringPropertyNames()){
                if(key.startsWith("floor.")){
                    int t=Integer.parseInt(key.substring(6)),m=integer(p,key);
                    checkTile(t%CastleHubMap.WIDTH,t/CastleHubMap.WIDTH);
                    checkMaterial(m);newFloor.put(t,m);
                }else if(key.startsWith("wall.")){
                    int w=Integer.parseInt(key.substring(5)),m=integer(p,key);
                    if(w<0||w>=CastleHubArchitecturalPlan.walls().size())
                        throw new IllegalArgumentException("Invalid wall index");
                    checkMaterial(m);newWalls.put(w,m);
                }else if(key.startsWith("nav.")){
                    int t=Integer.parseInt(key.substring(4));
                    checkTile(t%CastleHubMap.WIDTH,t/CastleHubMap.WIDTH);
                    if(Boolean.parseBoolean(p.getProperty(key)))newNav.add(t);
                }
            }
            double sx=number(p,"spawn.x"),sz=number(p,"spawn.z"),syaw=number(p,"spawn.yaw");
            if(!Double.isFinite(sx+sz+syaw)||sx<1||sx>42||sz<1||sz>34)
                throw new IllegalArgumentException("Bad player spawn");
            // Version-1 user saves have no authored geometry list. Keep their
            // original architecture and transfer the old wall-paint overrides.
            Map<String,Surface> parsed=new LinkedHashMap<>();
            if(p.containsKey("surfaces.count")){
                int countSurfaces=integer(p,"surfaces.count");
                if(countSurfaces<0||countSurfaces>512)
                    throw new IllegalArgumentException("Too many scene surfaces");
                for(int index=0;index<countSurfaces;index++){
                    String k="surfaces."+index+".";
                    var face=new Surface(p.getProperty(k+"id"),
                        Kind.valueOf(p.getProperty(k+"kind")),
                        number(p,k+"x0"),number(p,k+"z0"),
                        number(p,k+"x1"),number(p,k+"z1"),
                        number(p,k+"base"),number(p,k+"height"),integer(p,k+"material"));
                    if(parsed.putIfAbsent(face.id(),face)!=null)
                        throw new IllegalArgumentException("Duplicate surface "+face.id());
                }
            }else{
                parsed.putAll(surfaces);
                for(var paint:newWalls.entrySet()){
                    String id="wall"+paint.getKey();
                    var old=parsed.get(id);
                    if(old!=null)parsed.put(id,new Surface(id,old.kind(),
                        old.x0(),old.z0(),old.x1(),old.z1(),old.base(),old.height(),paint.getValue()));
                }
            }
            // Once-only additive lighting upgrade for existing player scenes. Never
            // replace authored positions or re-add a fixture after the player
            // removes it from a version-2 saved scene.
            int lightingVersion=Integer.parseInt(p.getProperty("lighting.layout","1"));
            if(lightingVersion<2){
                for(int i=17;i<CastleHubDecor.LIGHTS.size();i++){
                    var fixture=CastleHubDecor.LIGHTS.get(i);
                    boolean chandelier=fixture.on().contains("ceiling_lamp_4");
                    String id="lighting2_"+(i-17);
                    incomingLights.putIfAbsent(id,new Lamp(id,fixture.on(),fixture.off(),
                        fixture.x(),fixture.y(),fixture.z(),fixture.scale(),fixture.yaw(),
                        fixture.mount(),chandelier?10.5:9.8,chandelier?4.2:4.0));
                }
            }
            // Older authored castle saves receive only newly introduced
            // battlement floors. Once saved, deleted/edited ramparts are
            // respected rather than recreated each launch.
            if(Integer.parseInt(p.getProperty("fortification.layout","1"))<2){
                var decks=CastleHubArchitecturalPlan.decks();
                for(int index=3;index<decks.size();index++){
                    var deck=decks.get(index);
                    String id="rampart2_"+(index-3);
                    parsed.putIfAbsent(id,new Surface(id,Kind.FLOOR,
                        deck.x0(),deck.z0(),deck.x1(),deck.z1(),
                        deck.height(),CastleRooms.SLAB,deck.material()));
                }
            }
            // Relocate one proven wall-penetrating armchair in older castle
            // scenes. Apply only to the exact original pose so editor adjustments
            // remain untouched.
            if(Integer.parseInt(p.getProperty("furniture.layout","1"))<2){
                var chair=incomingProps.get("ground22");
                if(chair!=null&&chair.mesh().equals("armchair_1")
                    &&chair.x()==20.4&&chair.z()==20.0&&chair.scale()==.77){
                    incomingProps.put("ground22",new Prop(chair.id(),chair.mesh(),
                        22.3,chair.y(),chair.z(),chair.scale(),chair.yaw(),
                        chair.halfX(),chair.halfZ(),chair.upper()));
                }
            }
            // Migrate the original continuous west wall to a real, recessed
            // pair of window apertures. Only canonical unchanged geometry is
            // converted; custom walls remain under user control.
            if(Integer.parseInt(p.getProperty("windows.layout","1"))<2){
                String oldId=null;
                for(var entry:parsed.entrySet()){
                    var wall=entry.getValue();
                    if(wall.kind()==Kind.WALL&&wall.x0()==1.0&&wall.x1()==1.0
                        &&wall.z0()==1.0&&wall.z1()==34.0&&wall.base()==0
                        &&wall.height()==CastleHubArchitecturalPlan.GROUND_CEILING){
                        oldId=entry.getKey();break;
                    }
                }
                if(oldId!=null){
                    var previous=parsed.remove(oldId);
                    double[][] cuts={{1,16,0,3.4},{16,17.8,0,1.05},
                        {16,17.8,2.45,3.4},{17.8,26,0,3.4},
                        {26,27.8,0,1.05},{26,27.8,2.45,3.4},
                        {27.8,34,0,3.4}};
                    for(int i=0;i<cuts.length;i++){
                        double[] c=cuts[i];String id="window2-west-"+i;
                        parsed.put(id,new Surface(id,Kind.WALL,
                            1,c[0],1,c[1],c[2],c[3]-c[2],previous.material()));
                    }
                }
            }
            // The old south wall was one solid line across the future gate.
            // Re-cut only the exact unmodified legacy surface. Keep edited
            // player walls intact instead of silently deleting custom geometry.
            if(Integer.parseInt(p.getProperty("gate.layout","1"))<2){
                var former=parsed.get("wall2");
                if(former!=null&&former.kind()==Kind.WALL
                    &&former.x0()==1.0&&former.x1()==43.0
                    &&former.z0()==34.0&&former.z1()==34.0
                    &&former.base()==0&&former.height()==CastleHubArchitecturalPlan.GROUND_CEILING){
                    parsed.remove("wall2");
                    parsed.put("gate-west-masonry",new Surface("gate-west-masonry",Kind.WALL,
                        1,34,13.2,34,0,CastleHubArchitecturalPlan.GROUND_CEILING,former.material()));
                    parsed.put("gate-east-masonry",new Surface("gate-east-masonry",Kind.WALL,
                        17.8,34,43,34,0,CastleHubArchitecturalPlan.GROUND_CEILING,former.material()));
                    parsed.put("gate-header-masonry",new Surface("gate-header-masonry",Kind.WALL,
                        13.2,34,17.8,34,2.95,.45,former.material()));
                }
            }
            Map<String,CastleDoorSlots.Type> loadedDoors=new LinkedHashMap<>();
            if(p.containsKey("doors.count")){
                int total=integer(p,"doors.count");
                if(total<0||total>CastleDoorSlots.all().size())
                    throw new IllegalArgumentException("Invalid number of mounted doors");
                for(int i=0;i<total;i++){
                    String prefix="doors."+i+".";
                    String id=p.getProperty(prefix+"id");
                    var slot=CastleDoorSlots.require(id);
                    CastleDoorSlots.Type type=CastleDoorSlots.Type.valueOf(p.getProperty(prefix+"type"));
                    if(type==CastleDoorSlots.Type.EMPTY
                        ||slot.gate()!=(type==CastleDoorSlots.Type.PORTCULLIS)
                        ||loadedDoors.putIfAbsent(id,type)!=null)
                        throw new IllegalArgumentException("Duplicate or invalid doorway assignment "+id);
                }
            }else{
                for(var slot:CastleDoorSlots.all())
                    if(CastleDoorSlots.initial(slot)!=CastleDoorSlots.Type.EMPTY)
                        loadedDoors.put(slot.id(),CastleDoorSlots.initial(slot));
            }
            surfaces.clear();surfaces.putAll(parsed);
            doorAssignments.clear();doorAssignments.putAll(loadedDoors);
            props.clear();props.putAll(incomingProps);lamps.clear();lamps.putAll(incomingLights);
            stations.clear();stations.putAll(newStations);
            floorPaint.clear();floorPaint.putAll(newFloor);wallPaint.clear();wallPaint.putAll(newWalls);
            navBlocked.clear();navBlocked.addAll(newNav);
            spawnX=sx;spawnZ=sz;spawnYaw=syaw;revision++;
        }catch(RuntimeException e){throw new IOException("Invalid castle editor document: "+path,e);}
    }
    /** Atomic replacement prevents partial save data if the app is interrupted. */
    public synchronized void save()throws IOException{
        Properties p=new Properties();p.setProperty("format","1");
        p.setProperty("structure.layout","2");
        p.setProperty("lighting.layout","2");
        p.setProperty("fortification.layout","2");
        p.setProperty("gate.layout","2");
        p.setProperty("windows.layout","2");
        p.setProperty("furniture.layout","2");
        p.setProperty("doors.count",String.valueOf(doorAssignments.size()));
        int di=0;
        for(var assigned:doorAssignments.entrySet()){
            String prefix="doors."+(di++)+".";
            p.setProperty(prefix+"id",assigned.getKey());
            p.setProperty(prefix+"type",assigned.getValue().name());
        }
        p.setProperty("surfaces.count",""+surfaces.size());
        int surfaceIndex=0;
        for(var surface:surfaces.values()){
            String k="surfaces."+surfaceIndex+++".";
            p.setProperty(k+"id",surface.id());p.setProperty(k+"kind",surface.kind().name());
            p.setProperty(k+"x0",""+surface.x0());p.setProperty(k+"z0",""+surface.z0());
            p.setProperty(k+"x1",""+surface.x1());p.setProperty(k+"z1",""+surface.z1());
            p.setProperty(k+"base",""+surface.base());p.setProperty(k+"height",""+surface.height());
            p.setProperty(k+"material",""+surface.material());
        }
        p.setProperty("props.count",String.valueOf(props.size()));
        int i=0;for(var item:props.values()){
            String k="props."+i+++".";
            p.setProperty(k+"id",item.id());p.setProperty(k+"mesh",item.mesh());
            p.setProperty(k+"x",""+item.x());p.setProperty(k+"y",""+item.y());
            p.setProperty(k+"z",""+item.z());p.setProperty(k+"scale",""+item.scale());
            p.setProperty(k+"yaw",""+item.yaw());p.setProperty(k+"halfX",""+item.halfX());
            p.setProperty(k+"halfZ",""+item.halfZ());p.setProperty(k+"upper",""+item.upper());
        }
        p.setProperty("lamps.count",""+lamps.size());i=0;
        for(var light:lamps.values()){
            String k="lamps."+i+++".";
            p.setProperty(k+"id",light.id());p.setProperty(k+"on",light.on());
            p.setProperty(k+"off",light.off());p.setProperty(k+"x",""+light.x());
            p.setProperty(k+"y",""+light.y());p.setProperty(k+"z",""+light.z());
            p.setProperty(k+"scale",""+light.scale());p.setProperty(k+"yaw",""+light.yaw());
            p.setProperty(k+"mount",light.mount().name());
            p.setProperty(k+"radius",""+light.radius());p.setProperty(k+"power",""+light.power());
        }
        for(var st:stations.entrySet()){
            p.setProperty("station."+st.getKey()+".x",""+st.getValue()[0]);
            p.setProperty("station."+st.getKey()+".z",""+st.getValue()[1]);
        }
        for(var e:floorPaint.entrySet())p.setProperty("floor."+e.getKey(),""+e.getValue());
        for(var e:wallPaint.entrySet())p.setProperty("wall."+e.getKey(),""+e.getValue());
        for(int t:navBlocked)p.setProperty("nav."+t,"true");
        p.setProperty("spawn.x",""+spawnX);p.setProperty("spawn.z",""+spawnZ);
        p.setProperty("spawn.yaw",""+spawnYaw);
        Files.createDirectories(path.getParent());
        Path pending=Files.createTempFile(path.getParent(),"castle-", ".pending");
        try{
            try(OutputStream out=Files.newOutputStream(pending)){p.store(out,"Beckenburg developer castle scene");}
            try{Files.move(pending,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(java.nio.file.AtomicMoveNotSupportedException ex){
                Files.move(pending,path,StandardCopyOption.REPLACE_EXISTING);
            }
        }finally{Files.deleteIfExists(pending);}
    }
    public Path path(){return path;}
}
