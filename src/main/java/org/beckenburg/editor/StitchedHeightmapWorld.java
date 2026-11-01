package org.beckenburg.editor;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

/**
 * Sparse, 16-bit, world-coordinate stitched terrain heightmaps (Java 17).
 * N+1 pixels per side supply N cells. All tiles use the SAME elevation datum.
 * This is the authoritative SOURCE sampler, not a render-LOD or game adapter.
 */
public final class StitchedHeightmapWorld {
    public static final String FORMAT="beckenburg-stitched-heightmap-v1";
    private static final Pattern TILE=Pattern.compile("tile_(-?[0-9]+)_(-?[0-9]+)\\.png");
    private static final long MAX_PNG_BYTES=32L*1024*1024;
    public record Settings(int tileCells,double metresPerCell,double originX,double originZ,
                           double minElevation,double maxElevation,long seed,int cacheTiles) {
        public Settings {
            if(tileCells<16||tileCells>1024||Integer.bitCount(tileCells)!=1
                ||!Double.isFinite(metresPerCell)||metresPerCell<.05||metresPerCell>100
                ||!Double.isFinite(originX)||!Double.isFinite(originZ)
                ||Math.abs(originX)>100_000_000||Math.abs(originZ)>100_000_000
                ||!Double.isFinite(minElevation)||!Double.isFinite(maxElevation)
                ||maxElevation<=minElevation||maxElevation-minElevation>100_000
                ||cacheTiles<1||cacheTiles>32)
                throw new IllegalArgumentException("Invalid stitched-world settings");
        }
        public double tileWidthMetres(){return tileCells*metresPerCell;}
    }
    public record Tile(int x,int z){}
    public record SeamError(Tile a,Tile b,String edge,int sample,int aValue,int bValue){}
    private final Path root;
    private final Settings settings;
    private final Map<Tile,int[]> cache;
    private long diskReads;
    private StitchedHeightmapWorld(Path root,Settings settings) {
        this.root=root.toAbsolutePath().normalize();
        this.settings=settings;
        cache=new LinkedHashMap<>(settings.cacheTiles()+1,.75f,true) {
            @Override protected boolean removeEldestEntry(Map.Entry<Tile,int[]> e) {
                return size()>StitchedHeightmapWorld.this.settings.cacheTiles();
            }
        };
    }
    public static StitchedHeightmapWorld create(Path root,Settings settings)throws IOException {
        Objects.requireNonNull(root);Objects.requireNonNull(settings);
        Path dir=root.toAbsolutePath().normalize();
        if(Files.exists(dir.resolve("world.properties")))
            throw new IOException("Stitched world already exists");
        Files.createDirectories(dir);
        StitchedHeightmapWorld world=new StitchedHeightmapWorld(dir,settings);
        world.writeManifest();
        return world;
    }
    public static StitchedHeightmapWorld open(Path root)throws IOException {
        Path dir=Objects.requireNonNull(root).toAbsolutePath().normalize();
        Properties props=new Properties();
        try(var in=Files.newInputStream(dir.resolve("world.properties"))){props.load(in);}
        // Recover any interrupted multi-tile replacement before a reader can
        // observe a half-installed heightmap region.
        recoverPendingBatch(dir);
        if(!FORMAT.equals(props.getProperty("format")))
            throw new IOException("Unknown stitched-world format");
        try {
            return new StitchedHeightmapWorld(dir,new Settings(
                Integer.parseInt(props.getProperty("tileCells")),
                Double.parseDouble(props.getProperty("metresPerCell")),
                Double.parseDouble(props.getProperty("originX")),
                Double.parseDouble(props.getProperty("originZ")),
                Double.parseDouble(props.getProperty("minElevation")),
                Double.parseDouble(props.getProperty("maxElevation")),
                Long.parseLong(props.getProperty("seed")),
                Integer.parseInt(props.getProperty("cacheTiles"))));
        }catch(RuntimeException bad){throw new IOException("Invalid stitched-world metadata",bad);}
    }
    private void writeManifest()throws IOException {
        String s="format="+FORMAT+"\n"
            +"tileCells="+settings.tileCells()+"\n"
            +"metresPerCell="+settings.metresPerCell()+"\n"
            +"originX="+settings.originX()+"\n"
            +"originZ="+settings.originZ()+"\n"
            +"minElevation="+settings.minElevation()+"\n"
            +"maxElevation="+settings.maxElevation()+"\n"
            +"seed="+settings.seed()+"\n"
            +"cacheTiles="+settings.cacheTiles()+"\n";
        Path temp=Files.createTempFile(root,".world-",".tmp");
        try{
            Files.writeString(temp,s);
            Files.move(temp,root.resolve("world.properties"),
                StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
        }finally{Files.deleteIfExists(temp);}
    }
    public Settings settings(){return settings;}
    public Path directory(){return root;}
    public synchronized int cachedTiles(){return cache.size();}
    public synchronized long diskReads(){return diskReads;}
    private static void validIndex(int x,int z){
        if(Math.abs((long)x)>1_000_000||Math.abs((long)z)>1_000_000)
            throw new IllegalArgumentException("World tile index out of bounds");
    }
    public Path tilePath(int x,int z){
        validIndex(x,z);
        return root.resolve("tile_"+x+"_"+z+".png");
    }
    public boolean hasTile(int x,int z){return Files.isRegularFile(tilePath(x,z));}
    public List<Tile> tiles()throws IOException {
        List<Tile> result=new ArrayList<>();
        try(var entries=Files.list(root)){
            for(Path p:entries.filter(Files::isRegularFile).toList()){
                Matcher m=TILE.matcher(p.getFileName().toString());
                if(m.matches())try{
                    int x=Integer.parseInt(m.group(1)),z=Integer.parseInt(m.group(2));
                    validIndex(x,z);result.add(new Tile(x,z));
                }catch(IllegalArgumentException ignored){}
            }
        }
        result.sort(Comparator.comparingInt(Tile::z).thenComparingInt(Tile::x));
        return List.copyOf(result);
    }
    private int[] decode(Path path)throws IOException {
        if(!Files.isRegularFile(path)||Files.size(path)>MAX_PNG_BYTES)
            throw new IOException("Missing/oversized heightmap PNG: "+path.getFileName());
        BufferedImage image=ImageIO.read(path.toFile());
        int size=settings.tileCells()+1;
        if(image==null||image.getWidth()!=size||image.getHeight()!=size)
            throw new IOException("Heightmap dimensions must be "+size+"x"+size);
        Raster raster=image.getRaster();
        if(raster.getNumBands()!=1||raster.getSampleModel().getSampleSize(0)!=16
            ||raster.getDataBuffer().getDataType()!=DataBuffer.TYPE_USHORT)
            throw new IOException("Must use single-channel 16-bit grayscale PNG");
        int[] values=new int[size*size];
        for(int z=0;z<size;z++)for(int x=0;x<size;x++)
            values[z*size+x]=raster.getSample(x,z,0);
        return values;
    }
    private synchronized int[] tile(int x,int z)throws IOException {
        Tile key=new Tile(x,z);
        int[] cached=cache.get(key);
        if(cached!=null)return cached;
        int[] loaded=decode(tilePath(x,z));
        cache.put(key,loaded);diskReads++;
        return loaded;
    }
    private int at(int[] data,int x,int z){
        return data[z*(settings.tileCells()+1)+x];
    }
    private void checkAdjacent(int x,int z,int[] values)throws IOException {
        checkAdjacent(x,z,values,Map.of());
    }
    private void checkAdjacent(int x,int z,int[] values,Map<Tile,int[]> candidates)
            throws IOException {
        int n=settings.tileCells();
        for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++){
            if(dx==0&&dz==0)continue;
            long xx=(long)x+dx,zz=(long)z+dz;
            if(Math.abs(xx)>1_000_000||Math.abs(zz)>1_000_000)continue;
            int tx=(int)xx,tz=(int)zz;
            Tile neighborKey=new Tile(tx,tz);
            int[] replacement=candidates.get(neighborKey);
            if(replacement==null&&!hasTile(tx,tz))continue;
            int[] neighbor=replacement!=null?replacement:tile(tx,tz);
            if(dx!=0&&dz!=0){
                int a=at(values,dx<0?0:n,dz<0?0:n);
                int b=at(neighbor,dx<0?n:0,dz<0?n:0);
                if(a!=b)throw new IOException("Corner seam mismatch: "+x+","+z+" vs "+tx+","+tz);
            }else for(int i=0;i<=n;i++){
                int a=dx!=0?at(values,dx<0?0:n,i):at(values,i,dz<0?0:n);
                int b=dx!=0?at(neighbor,dx<0?n:0,i):at(neighbor,i,dz<0?n:0);
                if(a!=b)throw new IOException("Edge seam mismatch sample "+i+
                    ": "+x+","+z+" vs "+tx+","+tz);
            }
        }
    }
    /** Import validates entire PNG and every present neighbor BEFORE replacement. */
    public synchronized void importTile(int x,int z,Path source,boolean replace)throws IOException {
        validIndex(x,z);Objects.requireNonNull(source);
        Path destination=tilePath(x,z);
        if(Files.exists(destination)&&!replace)
            throw new IOException("Tile already exists; explicit replace required");
        int[] values=decode(source);
        checkAdjacent(x,z,values);
        Path tmp=Files.createTempFile(root,".tile-",".png");
        try{
            Files.copy(source,tmp,StandardCopyOption.REPLACE_EXISTING);
            Files.move(tmp,destination,StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING);
            cache.remove(new Tile(x,z));
        }finally{Files.deleteIfExists(tmp);}
    }
    private static final String BATCH_FOLDER=".pending-heightmap-batch";
    /**
     * Batch import of up to nine mutually stitched tiles. All staged source
     * pixels are decoded and cross-validated before ANY authoritative PNG is
     * changed. The journal restores original files on next open after a crash.
     * This is not a promise of cross-process atomic visibility during a write.
     */
    public synchronized void importTilesBatch(Map<Tile,Path> inputs,boolean replace)
            throws IOException {
        Objects.requireNonNull(inputs);
        if(inputs.isEmpty()||inputs.size()>9)
            throw new IllegalArgumentException("Batch must contain 1..9 tiles");
        Path journal=root.resolve(BATCH_FOLDER);
        // Prevent two instances from committing/recovering the same journal.
        try(var channel=FileChannel.open(root.resolve(".heightmap-batch-lock"),
                    StandardOpenOption.CREATE,StandardOpenOption.WRITE);
            var lock=channel.lock()){
            recoverPendingBatchUnlocked(root);
            Map<Tile,int[]> candidates=new LinkedHashMap<>();
            List<Tile> order=new ArrayList<>(inputs.keySet());
            order.sort(Comparator.comparingInt(Tile::z).thenComparingInt(Tile::x));
            for(Tile key:order){
                if(key==null)throw new IllegalArgumentException("Null tile coordinate");
                validIndex(key.x(),key.z());
                Path path=Objects.requireNonNull(inputs.get(key));
                if(hasTile(key.x(),key.z())&&!replace)
                    throw new IOException("Existing tile requires explicit replacement: "+key);
                candidates.put(key,decode(path));
            }
            for(var entry:candidates.entrySet())
                checkAdjacent(entry.getKey().x(),entry.getKey().z(),
                    entry.getValue(),candidates);
            Files.createDirectory(journal);
            boolean manifestReady=false;
            try{
                Properties manifest=new Properties();
                manifest.setProperty("format","heightmap-batch-v1");
                manifest.setProperty("count",Integer.toString(order.size()));
                for(int i=0;i<order.size();i++){
                    Tile key=order.get(i);
                    Path destination=tilePath(key.x(),key.z());
                    String name="tile_"+key.x()+"_"+key.z()+".png";
                    Path staged=journal.resolve("new_"+name);
                    Files.copy(inputs.get(key),staged);
                    // Re-decoding staged bytes prevents a changing input file
                    // from silently invalidating the earlier source preflight.
                    if(!java.util.Arrays.equals(decode(staged),candidates.get(key)))
                        throw new IOException("Staged tile differs from preflight: "+name);
                    boolean existed=Files.exists(destination);
                    if(existed)Files.copy(destination,journal.resolve("old_"+name));
                    manifest.setProperty("entry."+i,
                        key.x()+","+key.z()+","+existed);
                }
                Path config=journal.resolve("manifest.tmp");
                try(var output=Files.newOutputStream(config)){manifest.store(output,
                    "Stitched heightmap batch rollback manifest");}
                Files.move(config,journal.resolve("manifest.properties"),
                    StandardCopyOption.ATOMIC_MOVE);
                manifestReady=true;
                for(Tile key:order){
                    String name="tile_"+key.x()+"_"+key.z()+".png";
                    Files.move(journal.resolve("new_"+name),
                        tilePath(key.x(),key.z()),StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
                    cache.remove(key);
                }
                Path success=journal.resolve("committed.tmp");
                Files.writeString(success,"complete");
                Files.move(success,journal.resolve("COMMITTED"),
                    StandardCopyOption.ATOMIC_MOVE);
                clearBatchJournal(journal);
            }catch(IOException|RuntimeException failure){
                try{
                    if(manifestReady)recoverPendingBatchUnlocked(root);
                    else clearBatchJournal(journal);
                }catch(IOException rollback){
                    failure.addSuppressed(rollback);
                }
                for(Tile key:order)cache.remove(key);
                throw failure;
            }
        }
    }
    private static void clearBatchJournal(Path folder)throws IOException{
        if(!Files.exists(folder))return;
        try(var entries=Files.walk(folder)){
            for(Path path:entries.sorted(Comparator.reverseOrder()).toList())
                Files.deleteIfExists(path);
        }
    }
    private static void recoverPendingBatch(Path directory)throws IOException {
        if(!Files.exists(directory.resolve(BATCH_FOLDER)))return;
        try(var channel=FileChannel.open(directory.resolve(".heightmap-batch-lock"),
                    StandardOpenOption.CREATE,StandardOpenOption.WRITE);
            var lock=channel.lock()){
            recoverPendingBatchUnlocked(directory);
        }
    }
    private static void recoverPendingBatchUnlocked(Path directory)throws IOException{
        Path folder=directory.resolve(BATCH_FOLDER);
        if(!Files.exists(folder))return;
        if(Files.exists(folder.resolve("COMMITTED"))){
            clearBatchJournal(folder);return;
        }
        Path manifestPath=folder.resolve("manifest.properties");
        if(!Files.exists(manifestPath)){
            // No authoritative file could have been modified before the
            // manifest was published, so discard the incomplete staging.
            clearBatchJournal(folder);return;
        }
        Properties manifest=new Properties();
        try(var input=Files.newInputStream(manifestPath)){manifest.load(input);}
        if(!"heightmap-batch-v1".equals(manifest.getProperty("format")))
            throw new IOException("Unknown pending heightmap transaction; manual recovery needed");
        int count;
        try{count=Integer.parseInt(manifest.getProperty("count"));}
        catch(RuntimeException problem){throw new IOException("Invalid pending batch count",problem);}
        if(count<1||count>9)throw new IOException("Invalid pending batch entry count");
        // First ensure all backups are present. Never partially restore a
        // journal that has already lost one of its required originals.
        List<Path> destinations=new ArrayList<>();
        List<Path> backups=new ArrayList<>();
        List<Boolean> existed=new ArrayList<>();
        for(int i=0;i<count;i++){
            String line=manifest.getProperty("entry."+i,"");
            if(!line.matches("-?[0-9]+,-?[0-9]+,(true|false)"))
                throw new IOException("Invalid pending batch manifest entry");
            String[] values=line.split(",");
            int x,z;
            try{x=Integer.parseInt(values[0]);z=Integer.parseInt(values[1]);}
            catch(NumberFormatException bad){throw new IOException("Invalid batch tile index",bad);}
            validIndex(x,z);
            String name="tile_"+x+"_"+z+".png";
            Path destination=directory.resolve(name);
            Path backup=folder.resolve("old_"+name);
            boolean prior=Boolean.parseBoolean(values[2]);
            if(prior&&!Files.isRegularFile(backup))
                throw new IOException("Pending batch original missing for "+name);
            destinations.add(destination);backups.add(backup);existed.add(prior);
        }
        for(int i=0;i<count;i++){
            if(existed.get(i)){
                Path temp=Files.createTempFile(directory,".recovery-",".png");
                try{
                    Files.copy(backups.get(i),temp,StandardCopyOption.REPLACE_EXISTING);
                    Files.move(temp,destinations.get(i),StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
                }finally{Files.deleteIfExists(temp);}
            }else Files.deleteIfExists(destinations.get(i));
        }
        clearBatchJournal(folder);
    }

    private static long mix(long v){
        v=(v^(v>>>30))*0xbf58476d1ce4e5b9L;
        v=(v^(v>>>27))*0x94d049bb133111ebL;
        return v^(v>>>31);
    }
    private double vertexNoise(long gx,long gz){
        long h=mix(settings.seed()^mix(gx*0x9e3779b97f4a7c15L)
                               ^mix(gz*0xc2b2ae3d27d4eb4fL));
        return (h>>>11)*0x1.0p-53;
    }
    private double noise(long gx,long gz,int interval){
        long ix=Math.floorDiv(gx,interval),iz=Math.floorDiv(gz,interval);
        double fx=Math.floorMod(gx,interval)/(double)interval;
        double fz=Math.floorMod(gz,interval)/(double)interval;
        fx=fx*fx*(3-2*fx);fz=fz*fz*(3-2*fz);
        double a=vertexNoise(ix,iz),b=vertexNoise(ix+1,iz);
        double c=vertexNoise(ix+1,iz+1),d=vertexNoise(ix,iz+1);
        return (a+(b-a)*fx)*(1-fz)+(d+(c-d)*fx)*fz;
    }
    /** Global integer-vertex addressing makes generation order seam-independent. */
    public synchronized void generateTile(int x,int z)throws IOException {
        validIndex(x,z);
        if(hasTile(x,z))throw new IOException("Generated tile already exists");
        int n=settings.tileCells()+1;
        BufferedImage image=new BufferedImage(n,n,BufferedImage.TYPE_USHORT_GRAY);
        for(int row=0;row<n;row++)for(int col=0;col<n;col++){
            long gx=(long)x*settings.tileCells()+col;
            long gz=(long)z*settings.tileCells()+row;
            double h=.46*noise(gx,gz,256)+.30*noise(gx,gz,128)
                     +.16*noise(gx,gz,64)+.08*noise(gx,gz,32);
            int value=(int)Math.round(65535*(.12+.76*h));
            image.getRaster().setSample(col,row,0,value);
        }
        Path temp=Files.createTempFile(root,".generated-",".png");
        try{
            if(!ImageIO.write(image,"png",temp.toFile()))
                throw new IOException("16-bit PNG encoder missing");
            importTile(x,z,temp,false);
        }finally{Files.deleteIfExists(temp);}
    }

    /**
     * Create a small contiguous region without overwriting existing source images.
     * An ordinary failure rolls back every tile created by this invocation. No
     * crash/power-loss atomicity is promised for multiple individual PNG files.
     */
    public synchronized List<Tile> generateRegion(int startX,int startZ,int width,int depth)
            throws IOException {
        if(width<1||depth<1||width>3||depth>3)
            throw new IllegalArgumentException("Region must be 1..3 tiles per axis");
        List<Tile> requested=new ArrayList<>();
        for(int z=0;z<depth;z++)for(int x=0;x<width;x++){
            long tx=(long)startX+x,tz=(long)startZ+z;
            if(tx>Integer.MAX_VALUE||tz>Integer.MAX_VALUE)
                throw new IllegalArgumentException("Tile index overflow");
            int ix=(int)tx,iz=(int)tz;
            validIndex(ix,iz);
            if(hasTile(ix,iz))
                throw new IOException("Region contains an existing tile: "+ix+","+iz);
            requested.add(new Tile(ix,iz));
        }
        List<Tile> created=new ArrayList<>();
        try {
            for(Tile t:requested){
                generateTile(t.x(),t.z());
                created.add(t);
            }
        }catch(IOException|RuntimeException failure){
            for(int i=created.size()-1;i>=0;i--){
                Tile t=created.get(i);
                try {
                    Files.deleteIfExists(tilePath(t.x(),t.z()));
                    cache.remove(t);
                }catch(IOException rollbackFailure){
                    failure.addSuppressed(rollbackFailure);
                }
            }
            throw failure;
        }
        return List.copyOf(created);
    }

    public double metres(int sample){
        if(sample<0||sample>65535)throw new IllegalArgumentException("Invalid height sample");
        return settings.minElevation()+
            (settings.maxElevation()-settings.minElevation())*sample/65535.0;
    }
    public int sample(int tx,int tz,int x,int z)throws IOException {
        int n=settings.tileCells();
        if(x<0||x>n||z<0||z>n)throw new IllegalArgumentException("Grid coordinates");
        return at(tile(tx,tz),x,z);
    }
    /** Ground = exact diagonal-triangle interpolation, NOT bilinear averaging. */
    public double height(double worldX,double worldZ)throws IOException {
        if(!Double.isFinite(worldX)||!Double.isFinite(worldZ))
            throw new IllegalArgumentException("Non-finite world point");
        double size=settings.tileWidthMetres();
        double gx=(worldX-settings.originX())/size;
        double gz=(worldZ-settings.originZ())/size;
        if(gx< -1_000_000||gx>=1_000_001||gz< -1_000_000||gz>=1_000_001)
            throw new IllegalArgumentException("World point outside supported tile space");
        int tx=(int)Math.floor(gx),tz=(int)Math.floor(gz);
        int n=settings.tileCells();
        double lx=(gx-tx)*n,lz=(gz-tz)*n;
        int ix=Math.min(n-1,(int)Math.floor(lx)),iz=Math.min(n-1,(int)Math.floor(lz));
        double fx=lx-ix,fz=lz-iz;
        int[] t=tile(tx,tz);
        double a=metres(at(t,ix,iz)),b=metres(at(t,ix+1,iz));
        double c=metres(at(t,ix+1,iz+1)),d=metres(at(t,ix,iz+1));
        return fz<=fx ? a+(b-a)*fx+(c-b)*fz
                      : a+(c-d)*fx+(d-a)*fz;
    }
    /** O(total loaded tiles) explicit inspection; no silent seam blending. */
    public synchronized List<SeamError> verifySeams()throws IOException {
        List<SeamError> errors=new ArrayList<>();
        int n=settings.tileCells();
        for(Tile k:tiles()){
            int[] first=tile(k.x(),k.z());
            for(int axis=0;axis<2;axis++){
                int nx=k.x()+(axis==0?1:0),nz=k.z()+(axis==1?1:0);
                if(!hasTile(nx,nz))continue;
                int[] second=tile(nx,nz);
                for(int i=0;i<=n;i++){
                    int a=axis==0?at(first,n,i):at(first,i,n);
                    int b=axis==0?at(second,0,i):at(second,i,0);
                    if(a!=b)errors.add(new SeamError(k,new Tile(nx,nz),
                        axis==0?"east":"south",i,a,b));
                }
            }
        }
        return List.copyOf(errors);
    }
}
