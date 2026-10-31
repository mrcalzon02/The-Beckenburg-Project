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
import java.util.List;
import java.util.Objects;

/**
 * Human-authored navigation exclusions, blocked door/transition edges and
 * flood-fill starting points. The file is independent of visual topology:
 * rebuilding a nav graph cannot silently restore an excluded section.
 *
 * Generated polygons/region IDs are deliberately NOT persisted (they change
 * with physical geometry and subdivision). Only stable world-space edits are.
 */
public final class AdaptiveNavMeshEdits {
    public static final int FORMAT_VERSION=1;
    public static final int MAX_ENTRIES=4096;
    private static final int MAGIC=0x424E4156; // BNAV
    private final Path path;
    private final ArrayList<Seed> seeds=new ArrayList<>();
    private final ArrayList<Exclusion> exclusions=new ArrayList<>();
    private final ArrayList<Barrier> barriers=new ArrayList<>();
    private boolean continuous;
    private long revision;

    public record Seed(String layer,double x,double z) {
        public Seed {
            if(layer==null||layer.isBlank()||!finite(x,z))
                throw new IllegalArgumentException("Invalid navigation seed");
        }
    }
    public record Exclusion(String layer,double x0,double z0,double x1,double z1) {
        public Exclusion {
            if(layer==null||layer.isBlank()||!finite(x0,z0,x1,z1)
                ||x1<=x0||z1<=z0)
                throw new IllegalArgumentException("Invalid navigation exclusion");
        }
        public boolean overlaps(double ax,double az,double bx,double bz) {
            return x0<bx&&x1>ax&&z0<bz&&z1>az;
        }
        public boolean covers(double ax,double az,double bx,double bz) {
            return x0<=ax&&x1>=bx&&z0<=az&&z1>=bz;
        }
    }
    /** The segment across a doorway, level transition, or forbidden portal. */
    public record Barrier(String layer,double x0,double z0,double x1,double z1) {
        public Barrier {
            if(layer==null||layer.isBlank()||!finite(x0,z0,x1,z1)
                ||Math.abs(x1-x0)+Math.abs(z1-z0)<1e-7
                ||(Math.abs(x1-x0)>1e-7&&Math.abs(z1-z0)>1e-7))
                throw new IllegalArgumentException("Barrier must be axis-aligned");
        }
        public boolean vertical(){return Math.abs(x0-x1)<1e-7;}
        public boolean spansInterior(double ax,double az,double bx,double bz) {
            if(vertical())return x0>ax+1e-7&&x0<bx-1e-7
                &&Math.max(z0,z1)>az+1e-7&&Math.min(z0,z1)<bz-1e-7;
            return z0>az+1e-7&&z0<bz-1e-7
                &&Math.max(x0,x1)>ax+1e-7&&Math.min(x0,x1)<bx-1e-7;
        }
        /** True only when an actual path crosses the authored blocked segment. */
        public boolean cuts(double ax,double az,double bx,double bz) {
            if(vertical()){
                if((ax-x0)*(bx-x0)>=-1e-12)return false;
                double z=az+(bz-az)*(x0-ax)/(bx-ax);
                return z>=Math.min(z0,z1)-1e-7&&z<=Math.max(z0,z1)+1e-7;
            }
            if((az-z0)*(bz-z0)>=-1e-12)return false;
            double x=ax+(bx-ax)*(z0-az)/(bz-az);
            return x>=Math.min(x0,x1)-1e-7&&x<=Math.max(x0,x1)+1e-7;
        }
    }
    public record Snapshot(List<Seed> seeds,List<Exclusion> exclusions,
                           List<Barrier> barriers,boolean continuous) {
        public Snapshot {
            seeds=List.copyOf(seeds);
            exclusions=List.copyOf(exclusions);
            barriers=List.copyOf(barriers);
        }
    }
    private AdaptiveNavMeshEdits(Path path) {this.path=path;}
    private static boolean finite(double...v){
        for(double d:v)if(!Double.isFinite(d))return false;
        return true;
    }
    public static AdaptiveNavMeshEdits open(StitchedHeightmapWorld world)throws IOException {
        Objects.requireNonNull(world);
        var file=world.directory().resolve("navmesh-authoring.bin");
        var doc=new AdaptiveNavMeshEdits(file);
        if(!Files.exists(file))return doc;
        if(Files.size(file)>1_500_000)throw new IOException("Oversized navmesh authoring document");
        try(var in=new DataInputStream(new BufferedInputStream(Files.newInputStream(file)))){
            if(in.readInt()!=MAGIC||in.readInt()!=FORMAT_VERSION)
                throw new IOException("Invalid nav authoring format");
            doc.continuous=in.readBoolean();
            int seedCount=readCount(in),excludedCount=readCount(in),barrierCount=readCount(in);
            for(int i=0;i<seedCount;i++)
                doc.seeds.add(new Seed(in.readUTF(),in.readDouble(),in.readDouble()));
            for(int i=0;i<excludedCount;i++)
                doc.exclusions.add(new Exclusion(in.readUTF(),in.readDouble(),
                    in.readDouble(),in.readDouble(),in.readDouble()));
            for(int i=0;i<barrierCount;i++)
                doc.barriers.add(new Barrier(in.readUTF(),in.readDouble(),
                    in.readDouble(),in.readDouble(),in.readDouble()));
            if(in.read()!=-1)throw new IOException("Trailing navigation authoring data");
        }catch(IllegalArgumentException invalid){
            throw new IOException("Invalid navigation authoring geometry",invalid);
        }
        return doc;
    }
    private static int readCount(DataInputStream in)throws IOException {
        int n=in.readInt();
        if(n<0||n>MAX_ENTRIES)throw new IOException("Unsafe navigation edit count");
        return n;
    }
    public Path path(){return path;}
    public long revision(){return revision;}
    public boolean continuous(){return continuous;}
    public List<Seed> seeds(){return List.copyOf(seeds);}
    public List<Exclusion> exclusions(){return List.copyOf(exclusions);}
    public List<Barrier> barriers(){return List.copyOf(barriers);}
    public Snapshot snapshot(){return new Snapshot(seeds(),exclusions(),barriers(),continuous);}
    public void restore(Snapshot s){
        Objects.requireNonNull(s);
        if(s.seeds().size()>MAX_ENTRIES||s.exclusions().size()>MAX_ENTRIES
            ||s.barriers().size()>MAX_ENTRIES)
            throw new IllegalArgumentException("Navigation edit budget exceeded");
        seeds.clear();seeds.addAll(s.seeds());
        exclusions.clear();exclusions.addAll(s.exclusions());
        barriers.clear();barriers.addAll(s.barriers());
        continuous=s.continuous();
        revision++;
    }
    public void setContinuous(boolean enabled){
        if(continuous!=enabled){continuous=enabled;revision++;}
    }
    public void addSeed(Seed s){
        Objects.requireNonNull(s);
        if(!seeds.contains(s)){
            if(seeds.size()>=MAX_ENTRIES)throw new IllegalStateException("Seed budget exceeded");
            seeds.add(s);revision++;
        }
    }
    public void addExclusion(Exclusion e){
        Objects.requireNonNull(e);
        if(!exclusions.contains(e)){
            if(exclusions.size()>=MAX_ENTRIES)
                throw new IllegalStateException("Exclusion budget exceeded");
            exclusions.add(e);revision++;
        }
    }
    public void addBarrier(Barrier b){
        Objects.requireNonNull(b);
        if(!barriers.contains(b)){
            if(barriers.size()>=MAX_ENTRIES)throw new IllegalStateException("Barrier budget exceeded");
            barriers.add(b);revision++;
        }
    }
    public boolean removeExclusion(Exclusion e){
        if(exclusions.remove(e)){revision++;return true;}
        return false;
    }
    public boolean removeBarrier(Barrier b){
        if(barriers.remove(b)){revision++;return true;}
        return false;
    }
    public boolean removeSeed(Seed s){
        if(seeds.remove(s)){revision++;return true;}
        return false;
    }
    public boolean excluded(String layer,double x0,double z0,double x1,double z1){
        for(Exclusion e:exclusions)
            if(e.layer().equals(layer)&&e.overlaps(x0,z0,x1,z1))return true;
        return false;
    }
    public boolean needsSplit(String layer,double x0,double z0,double x1,double z1){
        for(Exclusion e:exclusions)if(e.layer().equals(layer)
            &&e.overlaps(x0,z0,x1,z1)&&!e.covers(x0,z0,x1,z1))return true;
        for(Barrier b:barriers)if(b.layer().equals(layer)
            &&b.spansInterior(x0,z0,x1,z1))return true;
        return false;
    }
    public boolean blocks(String layer,double x0,double z0,double x1,double z1){
        for(Barrier b:barriers)
            if(b.layer().equals(layer)&&b.cuts(x0,z0,x1,z1))return true;
        return false;
    }
    /** Atomic persistence. No generated graph can overwrite authored rules. */
    public void save()throws IOException {
        Files.createDirectories(path.getParent());
        Path tmp=Files.createTempFile(path.getParent(),".nav-authoring-",".tmp");
        try{
            try(var out=new DataOutputStream(
                    new BufferedOutputStream(Files.newOutputStream(tmp)))){
                out.writeInt(MAGIC);out.writeInt(FORMAT_VERSION);
                out.writeBoolean(continuous);
                out.writeInt(seeds.size());out.writeInt(exclusions.size());
                out.writeInt(barriers.size());
                for(Seed s:seeds){out.writeUTF(s.layer());out.writeDouble(s.x());out.writeDouble(s.z());}
                for(Exclusion e:exclusions){
                    out.writeUTF(e.layer());out.writeDouble(e.x0());out.writeDouble(e.z0());
                    out.writeDouble(e.x1());out.writeDouble(e.z1());
                }
                for(Barrier b:barriers){
                    out.writeUTF(b.layer());out.writeDouble(b.x0());out.writeDouble(b.z0());
                    out.writeDouble(b.x1());out.writeDouble(b.z1());
                }
            }
            Files.move(tmp,path,StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING);
        }finally{Files.deleteIfExists(tmp);}
    }
}
