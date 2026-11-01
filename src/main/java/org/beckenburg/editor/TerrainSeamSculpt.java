package org.beckenburg.editor;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import javax.imageio.ImageIO;

/**
 * Non-destructive cross-tile seam sculpt session. Stores unique global grid
 * vertex overrides, never per-tile edge copies that can silently diverge.
 *
 * A session can be saved/reopened without modifying authoritative PNG files.
 * Explicit bake expands the shared vertices into every owner tile and passes
 * the entire set through StitchedHeightmapWorld.importTilesBatch().
 *
 * The current batch importer supports at most nine tiles. The session never
 * claims to be a live game renderer or multi-world collaboration protocol.
 */
public final class TerrainSeamSculpt {
    private static final int MAGIC=0x53534350,VERSION=1,MAX_EDITS=250000;
    public record Vertex(int x,int z) {}
    public record Delta(Vertex vertex,double before,double after) {}
    private final StitchedHeightmapWorld world;
    private final int centerX,centerZ,n;
    private final LinkedHashMap<StitchedHeightmapWorld.Tile,String> source=new LinkedHashMap<>();
    private final TreeMap<Long,Double> override=new TreeMap<>();
    private final TreeMap<Long,Double> pending=new TreeMap<>();
    private final ArrayDeque<List<Delta>> undo=new ArrayDeque<>(),redo=new ArrayDeque<>();
    private boolean dirty;
    private final Path path;

    public TerrainSeamSculpt(StitchedHeightmapWorld world,int x,int z)throws IOException{
        this.world=Objects.requireNonNull(world);this.centerX=x;this.centerZ=z;
        this.n=world.settings().tileCells();
        if(((long)x-1)*n<Integer.MIN_VALUE||
            ((long)x+2)*n>Integer.MAX_VALUE||
            ((long)z-1)*n<Integer.MIN_VALUE||
            ((long)z+2)*n>Integer.MAX_VALUE)
            throw new IllegalArgumentException(
                "Selected seam window exceeds 32-bit global grid coordinates");
        this.path=world.directory().resolve("terrain-seam-sessions")
            .resolve("center_"+x+"_"+z+".bin");
        for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++){
            int tx=x+dx,tz=z+dz;
            if(world.hasTile(tx,tz)){
                StitchedHeightmapWorld.Tile tile=new StitchedHeightmapWorld.Tile(tx,tz);
                source.put(tile,fingerprint(world.tilePath(tx,tz)));
            }
        }
        if(!world.hasTile(x,z))throw new IOException("Center tile must exist");
        if(source.size()<2)throw new IOException(
            "Cross-tile sculpt needs at least two adjacent existing tiles");
        verifyLoadedTileSeams();
        load();
    }
    /**
     * Validate ONLY the loaded 3x3 editing region. Large worlds may contain
     * thousands of other tiles unrelated to this seam stroke.
     */
    private void verifyLoadedTileSeams()throws IOException{
        for(var key:source.keySet()){
            int tx=key.x(),tz=key.z();
            var right=new StitchedHeightmapWorld.Tile(tx+1,tz);
            var bottom=new StitchedHeightmapWorld.Tile(tx,tz+1);
            var diagonal=new StitchedHeightmapWorld.Tile(tx+1,tz+1);
            var otherDiagonal=new StitchedHeightmapWorld.Tile(tx-1,tz+1);
            if(source.containsKey(right))
                for(int i=0;i<=n;i++)
                    if(world.sample(tx,tz,n,i)!=world.sample(tx+1,tz,0,i))
                        throw new IOException("Pre-existing east-west seam mismatch at "+key);
            if(source.containsKey(bottom))
                for(int i=0;i<=n;i++)
                    if(world.sample(tx,tz,i,n)!=world.sample(tx,tz+1,i,0))
                        throw new IOException("Pre-existing north-south seam mismatch at "+key);
            if(source.containsKey(diagonal)&&
                world.sample(tx,tz,n,n)!=world.sample(tx+1,tz+1,0,0))
                throw new IOException("Pre-existing diagonal seam mismatch at "+key);
            if(source.containsKey(otherDiagonal)&&
                world.sample(tx,tz,0,n)!=world.sample(tx-1,tz+1,n,0))
                throw new IOException("Pre-existing diagonal seam mismatch at "+key);
        }
    }
    public int tileCells(){return n;}
    public int centerX(){return centerX;}
    public int centerZ(){return centerZ;}
    public boolean dirty(){return dirty;}
    public int changedVertices(){return override.size();}
    public Path path(){return path;}
    public Set<StitchedHeightmapWorld.Tile> tiles(){return Set.copyOf(source.keySet());}
    private static long key(int x,int z){return ((long)x<<32)|(z&0xffffffffL);}
    private static Vertex vertex(long key){return new Vertex((int)(key>>32),(int)key);}
    private static String fingerprint(Path path)throws IOException {
        try{
            MessageDigest digest=MessageDigest.getInstance("SHA-256");
            try(var input=Files.newInputStream(path)){
                byte[] buffer=new byte[8192];int count;
                while((count=input.read(buffer))!=-1)
                    if(count>0)digest.update(buffer,0,count);
            }
            return HexFormat.of().formatHex(digest.digest());
        }catch(NoSuchAlgorithmException impossible){
            throw new IllegalStateException("JDK is missing SHA-256",impossible);
        }
    }
    private List<StitchedHeightmapWorld.Tile> owners(int gx,int gz){
        int tx=Math.floorDiv(gx,n),tz=Math.floorDiv(gz,n);
        int[] xs=gx%n==0?new int[]{tx-1,tx}:new int[]{tx};
        int[] zs=gz%n==0?new int[]{tz-1,tz}:new int[]{tz};
        var result=new ArrayList<StitchedHeightmapWorld.Tile>(4);
        for(int z:zs)for(int x:xs)
            result.add(new StitchedHeightmapWorld.Tile(x,z));
        return result;
    }
    /** Ownership requires ALL four adjacent tiles for an internal corner. */
    public boolean editable(int gx,int gz){
        var owners=owners(gx,gz);
        if(owners.isEmpty())return false;
        for(var tile:owners)if(!source.containsKey(tile))return false;
        return true;
    }
    private double base(int gx,int gz)throws IOException{
        var first=owners(gx,gz).get(0);
        int localX=gx-first.x()*n,localZ=gz-first.z()*n;
        return world.settings().minElevation()+
            (world.settings().maxElevation()-world.settings().minElevation())*
            world.sample(first.x(),first.z(),localX,localZ)/65535.0;
    }
    public double height(int gx,int gz)throws IOException{
        if(!editable(gx,gz))throw new IllegalArgumentException(
            "Vertex outside available seam-safe tile ownership");
        return override.getOrDefault(key(gx,gz),base(gx,gz));
    }
    /** A stroke may include multiple dabs, combined as one sparse undo action. */
    public void raise(double gx,double gz,double radius,double metres)throws IOException{
        if(!Double.isFinite(gx)||!Double.isFinite(gz)||!Double.isFinite(radius)
            ||!Double.isFinite(metres)||radius<=0||radius>n||Math.abs(metres)>10000)
            throw new IllegalArgumentException("Invalid shared-seam brush");
        int x0=(int)Math.floor(gx-radius),x1=(int)Math.ceil(gx+radius);
        int z0=(int)Math.floor(gz-radius),z1=(int)Math.ceil(gz+radius);
        // Validate every touched sample before making any mutation.
        TreeMap<Long,Double> nextValues=new TreeMap<>();
        TreeMap<Long,Double> previousValues=new TreeMap<>();
        for(int z=z0;z<=z1;z++)for(int x=x0;x<=x1;x++){
            double d=Math.hypot(x-gx,z-gz);
            if(d>=radius||!editable(x,z))continue;
            long k=key(x,z);
            double previous=height(x,z);
            double next=previous+metres*(1-d/radius);
            double min=world.settings().minElevation(),max=world.settings().maxElevation();
            if(next<min||next>max)throw new IOException(
                "Seam brush would exceed 16-bit terrain height range");
            nextValues.put(k,next);
            previousValues.put(k,previous);
        }
        int newKeys=0;
        for(long k:nextValues.keySet())if(!override.containsKey(k))newKeys++;
        if(override.size()+newKeys>MAX_EDITS)
            throw new IOException("Maximum seam session vertex budget exceeded");
        for(var entry:nextValues.entrySet()){
            long k=entry.getKey();
            pending.putIfAbsent(k,previousValues.get(k));
            override.put(k,entry.getValue());
        }
        if(!nextValues.isEmpty())dirty=true;
    }
    public void finishStroke(){
        if(pending.isEmpty())return;
        List<Delta> changes=new ArrayList<>(pending.size());
        for(var e:pending.entrySet()){
            var v=vertex(e.getKey());
            changes.add(new Delta(v,e.getValue(),override.get(e.getKey())));
        }
        undo.push(List.copyOf(changes));redo.clear();pending.clear();
    }
    private void apply(List<Delta> changes,boolean forward)throws IOException{
        for(Delta d:changes){
            long k=key(d.vertex.x(),d.vertex.z());
            double v=forward?d.after():d.before();
            if(!editable(d.vertex.x(),d.vertex.z()))
                throw new IOException("Source ownership changed during undo");
            var point=d.vertex();
            double baseline=base(point.x(),point.z());
            if(Math.abs(v-baseline)<1e-9)override.remove(k);
            else override.put(k,v);
        }
        dirty=true;
    }
    public boolean undo()throws IOException{
        finishStroke();
        if(undo.isEmpty())return false;
        var changes=undo.pop();apply(changes,false);redo.push(changes);return true;
    }
    public boolean redo()throws IOException{
        finishStroke();
        if(redo.isEmpty())return false;
        var changes=redo.pop();apply(changes,true);undo.push(changes);return true;
    }
    private void verifySource()throws IOException{
        for(var entry:source.entrySet()){
            Path tile=world.tilePath(entry.getKey().x(),entry.getKey().z());
            if(!Files.isRegularFile(tile)||!fingerprint(tile).equals(entry.getValue()))
                throw new IOException("Source heightmap changed since seam session opened: "+tile);
        }
    }
    private void load()throws IOException{
        if(!Files.isRegularFile(path))return;
        if(Files.size(path)>MAX_EDITS*24L+65536)
            throw new IOException("Oversized seam editing session");
        try(var in=new DataInputStream(new BufferedInputStream(Files.newInputStream(path)))){
            if(in.readInt()!=MAGIC||in.readInt()!=VERSION||
                in.readInt()!=centerX||in.readInt()!=centerZ||in.readInt()!=n)
                throw new IOException("Incompatible seam editing session");
            int count=in.readInt();
            if(count!=source.size())throw new IOException("Tile coverage changed since editing");
            for(int i=0;i<count;i++){
                var tile=new StitchedHeightmapWorld.Tile(in.readInt(),in.readInt());
                String hash=in.readUTF();
                if(!hash.equals(source.get(tile)))
                    throw new IOException("Source changed since session was saved: "+tile);
            }
            int edits=in.readInt();
            if(edits<0||edits>MAX_EDITS)throw new IOException("Invalid seam edit count");
            for(int i=0;i<edits;i++){
                int x=in.readInt(),z=in.readInt();double value=in.readDouble();
                if(!editable(x,z)||!Double.isFinite(value)
                    ||value<world.settings().minElevation()
                    ||value>world.settings().maxElevation())
                    throw new IOException("Invalid saved cross-tile sculpt point");
                if(override.putIfAbsent(key(x,z),value)!=null)
                    throw new IOException("Duplicate saved seam vertex");
            }
            if(in.read()!=-1)throw new IOException("Trailing seam editing data");
        }
    }
    public void save()throws IOException{
        finishStroke();verifySource();Files.createDirectories(path.getParent());
        Path tmp=Files.createTempFile(path.getParent(),".seam-sculpt-",".tmp");
        try{
            try(var out=new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(tmp)))){
                out.writeInt(MAGIC);out.writeInt(VERSION);
                out.writeInt(centerX);out.writeInt(centerZ);out.writeInt(n);
                out.writeInt(source.size());
                for(var entry:source.entrySet()){
                    out.writeInt(entry.getKey().x());out.writeInt(entry.getKey().z());
                    out.writeUTF(entry.getValue());
                }
                out.writeInt(override.size());
                for(var entry:override.entrySet()){
                    var v=vertex(entry.getKey());
                    out.writeInt(v.x());out.writeInt(v.z());out.writeDouble(entry.getValue());
                }
            }
            Files.move(tmp,path,StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING);
            dirty=false;
        }finally{Files.deleteIfExists(tmp);}
    }
    /**
     * Explicitly abandon an authored seam session without touching source
     * PNGs. Caller must obtain confirmation before discarding local work.
     */
    public void discard()throws IOException{
        Files.deleteIfExists(path);
        override.clear();pending.clear();undo.clear();redo.clear();
        dirty=false;
    }
    /**
     * Explicit source bake. Quantization happens ONCE per unique global vertex;
     * every owner receives the exact same R16 sample. Existing layered authoring
     * documents are protected against invalidation rather than silently erased.
     */
    public void bake()throws IOException{
        finishStroke();verifySource();
        if(override.isEmpty())return;
        Map<StitchedHeightmapWorld.Tile,int[]> output=new LinkedHashMap<>();
        for(var e:override.entrySet()){
            var v=vertex(e.getKey());
            for(var tile:owners(v.x(),v.z())){
                Path layer=world.directory().resolve("terrain-layers").resolve(
                    "tile_"+tile.x()+"_"+tile.z()+".bin");
                if(Files.exists(layer))throw new IOException(
                    "A layered terrain document depends on this source tile: "+layer+
                    ". Export/rebase that document before baking cross-tile seams.");
                if(!output.containsKey(tile)){
                    int[] pixels=new int[(n+1)*(n+1)];
                    for(int z=0;z<=n;z++)for(int x=0;x<=n;x++)
                        pixels[z*(n+1)+x]=world.sample(tile.x(),tile.z(),x,z);
                    output.put(tile,pixels);
                }
            }
        }
        if(output.size()>9)throw new IOException("Cross-tile bake exceeds nine-tile journal limit");
        double min=world.settings().minElevation(),max=world.settings().maxElevation();
        for(var entry:override.entrySet()){
            var v=vertex(entry.getKey());
            double raw=(entry.getValue()-min)*65535/(max-min);
            if(!Double.isFinite(raw)||raw<0||raw>65535)
                throw new IOException("Seam edit is outside R16 height range");
            int quantized=(int)Math.round(raw);
            for(var tile:owners(v.x(),v.z())){
                int x=v.x()-tile.x()*n,z=v.z()-tile.z()*n;
                output.get(tile)[z*(n+1)+x]=quantized;
            }
        }
        Path staging=Files.createTempDirectory(world.directory(),".sculpt-bake-");
        try{
            Map<StitchedHeightmapWorld.Tile,Path> files=new LinkedHashMap<>();
            for(var entry:output.entrySet()){
                BufferedImage image=new BufferedImage(n+1,n+1,
                    BufferedImage.TYPE_USHORT_GRAY);
                int[] pixels=entry.getValue();
                for(int z=0;z<=n;z++)for(int x=0;x<=n;x++)
                    image.getRaster().setSample(x,z,0,pixels[z*(n+1)+x]);
                Path file=staging.resolve("tile_"+entry.getKey().x()+"_"+
                    entry.getKey().z()+".png");
                if(!ImageIO.write(image,"png",file.toFile()))
                    throw new IOException("No 16-bit PNG writer available");
                files.put(entry.getKey(),file);
            }
            world.importTilesBatch(files,true);
            override.clear();undo.clear();redo.clear();pending.clear();
            // Session metadata is stale after source changes; the old file must
            // not silently apply a second copy of the same sculpt operation.
            Files.deleteIfExists(path);
            dirty=false;
        }finally{
            try(var walk=Files.walk(staging)){
                for(Path p:walk.sorted(Comparator.reverseOrder()).toList())
                    Files.deleteIfExists(p);
            }
        }
    }
}
