package org.beckenburg.editor;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Bounded Java 17 terrain-height DELTAS and loading-zone authoring document.
 * The host game's renderer/physics must consume these values; this model does
 * not run a second preview renderer or silently dispatch gameplay missions.
 */
public final class TerrainZoneDocument {
    public static final int VERSION=1;
    private static final int MAGIC=0x545a4d50; // TZMP
    private static final int MAX_NODES=65_536, MAX_ZONES=256;
    public enum Brush { RAISE, LOWER, SMOOTH, FLATTEN }
    public enum Trigger { ENTER, INTERACT }

    public record Grid(double x0,double z0,double step,int columns,int rows) {
        public Grid {
            if(!Double.isFinite(x0)||!Double.isFinite(z0)||!Double.isFinite(step)
                ||step<=0||step>8||columns<2||rows<2
                ||(long)columns*rows>MAX_NODES)
                throw new IllegalArgumentException("Invalid terrain grid");
        }
        public double x1(){return x0+(columns-1)*step;}
        public double z1(){return z0+(rows-1)*step;}
        public boolean contains(double x,double z){
            return Double.isFinite(x)&&Double.isFinite(z)&&x>=x0&&z>=z0&&x<=x1()&&z<=z1();
        }
    }
    /** Bounds are world metres, Y is FEET elevation; destination yaw is radians. */
    public record Zone(String id,double x0,double y0,double z0,double x1,double y1,double z1,
                       String destination,double spawnX,double spawnY,double spawnZ,
                       double spawnYaw,Trigger trigger) {
        public Zone {
            if(id==null||!id.matches("[A-Za-z0-9_-]{1,48}")||destination==null
                ||!destination.matches("[A-Za-z0-9_-]{1,48}")||trigger==null)
                throw new IllegalArgumentException("Invalid loading-zone identity");
            for(double d:new double[]{x0,y0,z0,x1,y1,z1,spawnX,spawnY,spawnZ,spawnYaw})
                if(!Double.isFinite(d)||Math.abs(d)>100_000)
                    throw new IllegalArgumentException("Non-finite/out-of-range zone");
            if(x1-x0<.25||z1-z0<.25||y1-y0<.25)
                throw new IllegalArgumentException("Loading-zone bounds too small or reversed");
        }
        public boolean contains(double x,double y,double z){
            return x>=x0&&x<=x1&&y>=y0&&y<=y1&&z>=z0&&z<=z1;
        }
        /** Segment-vs-box sweep also catches fast outside-to-outside crossings. */
        public boolean entered(double ax,double ay,double az,double bx,double by,double bz){
            if(trigger!=Trigger.ENTER||contains(ax,ay,az))return false;
            if(!Double.isFinite(ax+ay+az+bx+by+bz))return false;
            double[] origin={ax,ay,az},dir={bx-ax,by-ay,bz-az};
            double[] lo={x0,y0,z0},hi={x1,y1,z1};
            double near=0,far=1;
            for(int axis=0;axis<3;axis++){
                if(Math.abs(dir[axis])<1e-12){
                    if(origin[axis]<lo[axis]||origin[axis]>hi[axis])return false;
                }else{
                    double a=(lo[axis]-origin[axis])/dir[axis];
                    double b=(hi[axis]-origin[axis])/dir[axis];
                    near=Math.max(near,Math.min(a,b));
                    far=Math.min(far,Math.max(a,b));
                    if(near>far)return false;
                }
            }
            return far>=0&&near<=1;
        }
    }
    public record Snapshot(double[] deltas,List<Zone> zones) {
        public Snapshot {
            deltas=deltas.clone();
            zones=List.copyOf(zones);
        }
        @Override public double[] deltas(){return deltas.clone();}
    }
    private final String level;
    private final Grid grid;
    private final double[] deltas;
    private final LinkedHashMap<String,Zone> zones=new LinkedHashMap<>();
    private long revision=1;

    public TerrainZoneDocument(String level,Grid grid){
        if(level==null||!level.matches("[A-Za-z0-9_-]{1,48}"))
            throw new IllegalArgumentException("Invalid level ID");
        this.level=level;
        this.grid=Objects.requireNonNull(grid);
        deltas=new double[grid.columns()*grid.rows()];
    }
    public String level(){return level;}
    public Grid grid(){return grid;}
    public long revision(){return revision;}
    public List<Zone> zones(){return List.copyOf(zones.values());}
    public double delta(int ix,int iz){return deltas[index(ix,iz)];}
    private int index(int ix,int iz){
        if(ix<0||ix>=grid.columns()||iz<0||iz>=grid.rows())
            throw new IndexOutOfBoundsException("Terrain vertex");
        return iz*grid.columns()+ix;
    }
    public Snapshot snapshot(){return new Snapshot(deltas,zones());}
    public void restore(Snapshot state){
        Objects.requireNonNull(state);
        double[] values=state.deltas();
        if(values.length!=deltas.length)throw new IllegalArgumentException("Wrong terrain grid snapshot");
        for(double value:values)if(!Double.isFinite(value)||Math.abs(value)>20)
            throw new IllegalArgumentException("Unsafe terrain snapshot");
        if(state.zones().size()>MAX_ZONES)throw new IllegalArgumentException("Too many zones");
        Map<String,Zone> unique=new LinkedHashMap<>();
        for(Zone zone:state.zones())if(unique.putIfAbsent(zone.id(),zone)!=null)
            throw new IllegalArgumentException("Duplicate loading-zone ID");
        System.arraycopy(values,0,deltas,0,values.length);
        zones.clear();zones.putAll(unique);
        revision++;
    }
    public void setDelta(int ix,int iz,double value){
        if(!Double.isFinite(value)||Math.abs(value)>20)
            throw new IllegalArgumentException("Terrain delta outside +/-20 m");
        int index=index(ix,iz);
        if(deltas[index]!=value){deltas[index]=value;revision++;}
    }
    /** Brush stroke is the caller's single undo transaction, not one per vertex. */
    public boolean brush(Brush mode,double x,double z,double radius,double strength,double flattenTo){
        Objects.requireNonNull(mode);
        if(!Double.isFinite(x)||!Double.isFinite(z)||!Double.isFinite(radius)
            ||!Double.isFinite(strength)||!Double.isFinite(flattenTo)
            ||radius<=0||radius>64||strength<0||strength>10||Math.abs(flattenTo)>20)
            throw new IllegalArgumentException("Invalid brush arguments");
        if(strength==0)return false;
        double[] before=deltas.clone();
        boolean changed=false;
        int xMin=Math.max(0,(int)Math.floor((x-radius-grid.x0())/grid.step()));
        int xMax=Math.min(grid.columns()-1,(int)Math.ceil((x+radius-grid.x0())/grid.step()));
        int zMin=Math.max(0,(int)Math.floor((z-radius-grid.z0())/grid.step()));
        int zMax=Math.min(grid.rows()-1,(int)Math.ceil((z+radius-grid.z0())/grid.step()));
        for(int iz=zMin;iz<=zMax;iz++)for(int ix=xMin;ix<=xMax;ix++){
            double distance=Math.hypot(grid.x0()+ix*grid.step()-x,
                                       grid.z0()+iz*grid.step()-z);
            if(distance>radius)continue;
            double factor=strength*(1-distance/radius);
            int k=index(ix,iz);
            double value=before[k];
            double next=switch(mode){
                case RAISE -> value+factor;
                case LOWER -> value-factor;
                case FLATTEN -> value+(flattenTo-value)*Math.min(1,factor);
                case SMOOTH -> {
                    double sum=0;int count=0;
                    for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++){
                        int nx=ix+dx,nz=iz+dz;
                        if(nx>=0&&nz>=0&&nx<grid.columns()&&nz<grid.rows()){
                            sum+=before[index(nx,nz)];count++;
                        }
                    }
                    yield value+(sum/count-value)*Math.min(1,factor);
                }
            };
            next=Math.max(-20,Math.min(20,next));
            if(deltas[k]!=next){deltas[k]=next;changed=true;}
        }
        if(changed)revision++;
        return changed;
    }
    public void putZone(Zone zone){
        Objects.requireNonNull(zone);
        if(zones.size()>=MAX_ZONES&&!zones.containsKey(zone.id()))
            throw new IllegalArgumentException("Loading-zone limit reached");
        zones.put(zone.id(),zone);revision++;
    }
    public boolean removeZone(String id){
        boolean changed=zones.remove(id)!=null;
        if(changed)revision++;
        return changed;
    }
    public Zone crossed(double ax,double ay,double az,double bx,double by,double bz){
        for(Zone zone:zones.values())if(zone.entered(ax,ay,az,bx,by,bz))return zone;
        return null;
    }
    public static Path defaultPath(String level){
        if(level==null||!level.matches("[A-Za-z0-9_-]{1,48}"))
            throw new IllegalArgumentException("Unsafe map name");
        return Path.of(System.getProperty("user.home"),".beckenburg","editor",
                       "terrain-zones",level+".tzmap");
    }
    public void save(Path path)throws IOException {
        Objects.requireNonNull(path);
        Path absolute=path.toAbsolutePath().normalize();
        Files.createDirectories(absolute.getParent());
        Path temp=Files.createTempFile(absolute.getParent(),".tzmap-",".tmp");
        try{
            try(var out=new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(temp)))){
                out.writeInt(MAGIC);out.writeInt(VERSION);out.writeUTF(level);
                out.writeDouble(grid.x0());out.writeDouble(grid.z0());
                out.writeDouble(grid.step());out.writeInt(grid.columns());out.writeInt(grid.rows());
                for(double delta:deltas)out.writeDouble(delta);
                out.writeInt(zones.size());
                for(Zone zone:zones.values()){
                    out.writeUTF(zone.id());out.writeDouble(zone.x0());out.writeDouble(zone.y0());
                    out.writeDouble(zone.z0());out.writeDouble(zone.x1());out.writeDouble(zone.y1());
                    out.writeDouble(zone.z1());out.writeUTF(zone.destination());
                    out.writeDouble(zone.spawnX());out.writeDouble(zone.spawnY());
                    out.writeDouble(zone.spawnZ());out.writeDouble(zone.spawnYaw());
                    out.writeUTF(zone.trigger().name());
                }
            }
            Files.move(temp,absolute,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
        }finally{Files.deleteIfExists(temp);}
    }
    /** Save/load never mutates an existing document on failed validation. */
    public static TerrainZoneDocument load(Path path,String expectedLevel,Grid expectedGrid)
            throws IOException {
        if(!Files.isRegularFile(path))return new TerrainZoneDocument(expectedLevel,expectedGrid);
        if(Files.size(path)>2_000_000)throw new IOException("Terrain-zone document too large");
        try(var in=new DataInputStream(new BufferedInputStream(Files.newInputStream(path)))){
            if(in.readInt()!=MAGIC||in.readInt()!=VERSION)
                throw new IOException("Unsupported terrain-zone document format");
            String level=in.readUTF();
            Grid grid=new Grid(in.readDouble(),in.readDouble(),in.readDouble(),
                               in.readInt(),in.readInt());
            if(!expectedLevel.equals(level)||!expectedGrid.equals(grid))
                throw new IOException("Terrain-zone map/grid mismatch: "+level);
            TerrainZoneDocument result=new TerrainZoneDocument(level,grid);
            for(int i=0;i<result.deltas.length;i++){
                double delta=in.readDouble();
                if(!Double.isFinite(delta)||Math.abs(delta)>20)
                    throw new IOException("Invalid terrain height at "+i);
                result.deltas[i]=delta;
            }
            int count=in.readInt();
            if(count<0||count>MAX_ZONES)throw new IOException("Invalid loading-zone count");
            for(int i=0;i<count;i++){
                Zone zone=new Zone(in.readUTF(),in.readDouble(),in.readDouble(),in.readDouble(),
                    in.readDouble(),in.readDouble(),in.readDouble(),in.readUTF(),
                    in.readDouble(),in.readDouble(),in.readDouble(),in.readDouble(),
                    Trigger.valueOf(in.readUTF()));
                if(result.zones.putIfAbsent(zone.id(),zone)!=null)
                    throw new IOException("Duplicate loading-zone ID");
            }
            if(in.read()!=-1)throw new IOException("Unexpected trailing terrain data");
            return result;
        }catch(IllegalArgumentException problem){
            throw new IOException("Invalid terrain-zone document: "+problem.getMessage(),problem);
        }
    }
}
