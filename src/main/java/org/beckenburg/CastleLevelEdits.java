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
        props.clear();lamps.clear();stations.clear();floorPaint.clear();
        wallPaint.clear();navBlocked.clear();
        int n=0;
        for(var f:CastleHubDecor.furnishings())
            props.put("ground"+n,fromFurnishing("ground"+n++,f,false,0));
        n=0;
        for(var f:CastleHubDecor.UPPER_FURNISHINGS)
            props.put("upper"+n,fromFurnishing("upper"+n++,f,true,
                CastleHubArchitecturalPlan.GROUND_CEILING));
        n=0;
        for(var f:CastleHubDecor.LIGHTS){
            String id="fixture"+n++;
            boolean chandelier=f.on().contains("ceiling_lamp_4");
            lamps.put(id,new Lamp(id,f.on(),f.off(),f.x(),f.y(),f.z(),
                f.scale(),f.yaw(),f.mount(),chandelier?10.5:9.8,
                chandelier?3.2:2.5));
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
        Objects.requireNonNull(light);if(lamps.size()>=96&&!lamps.containsKey(light.id()))
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
        checkMaterial(material);wallPaint.put(index,material);revision++;
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
            if(count<0||count>96)throw new IllegalArgumentException("Too many lights");
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
