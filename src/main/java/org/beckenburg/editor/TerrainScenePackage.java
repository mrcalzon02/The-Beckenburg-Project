package org.beckenburg.editor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Bridges physical terrain authoring into a durable RuntimeScene package, using
 * stable scene-node IDs and genuine OBJ geometry. Engine-specific renderers must
 * explicitly adopt the mesh-reference component; this module does not secretly
 * create a second scene, a fake live render or an implicit collision body.
 */
public final class TerrainScenePackage implements EnvironmentModule {
    public static final String COMPONENT="terrain-mesh-asset";
    public static final String SOURCE_COMPONENT="terrain-query-source";
    public static final String FOLDER="scene-package";
    private RuntimeScene attached;
    private final StitchedHeightmapWorld world;

    public record Published(String nodeId,Path geometryFile,Path sceneFile,
                            int triangles,int parts,String checksum) {}

    public TerrainScenePackage(StitchedHeightmapWorld world){
        this.world=Objects.requireNonNull(world);
    }
    public String id(){return "authoring.terrain-scene-package";}
    public String displayName(){return "Terrain scene geometry package";}
    public List<Capability> capabilities(){
        return List.of(Capability.AUTHORING,Capability.SERIALIZATION,Capability.DIAGNOSTICS);
    }
    public boolean ready(){return true;}
    public void attach(RuntimeScene scene){
        Objects.requireNonNull(scene);
        if(attached!=null&&attached!=scene)
            throw new IllegalStateException("Terrain publisher already attached to a scene");
        attached=scene;
    }
    public void detach(){attached=null;}
    public Path root(){return world.directory().resolve(FOLDER);}
    public Path scenePath(){return root().resolve("scene.bin");}
    public static String idForChunk(String chunk){
        if(chunk==null||!chunk.matches("[a-zA-Z0-9_-]{1,64}"))
            throw new IllegalArgumentException("Chunk ID must be 1..64 ASCII letters, digits, _ or -");
        return "terrain-"+chunk;
    }
    public InMemoryRuntimeScene openScene()throws IOException{
        if(Files.exists(scenePath()))return RuntimeSceneDocument.load(scenePath());
        String directory=world.directory().getFileName().toString();
        return new InMemoryRuntimeScene("terrain-world-"+directory);
    }
    public Published publish(String chunk,TerrainEntranceMesh.Bounds bounds)throws IOException{
        if(attached==null)throw new IllegalStateException("Attach to the authoritative scene first");
        return publish(world,attached,chunk,bounds);
    }
    public static Published publish(StitchedHeightmapWorld world,RuntimeScene scene,
                                    String chunk,TerrainEntranceMesh.Bounds bounds)
            throws IOException{
        Objects.requireNonNull(scene);Objects.requireNonNull(world);Objects.requireNonNull(bounds);
        String nodeId=idForChunk(chunk);
        Path root=world.directory().resolve(FOLDER),folder=root.resolve("meshes");
        Path geometry=folder.resolve(chunk+".obj");
        Path scenePath=root.resolve("scene.bin");
        var existing=scene.find(nodeId);
        if(existing!=null&&!existing.components().stream().anyMatch(c->
            c.type().equals(COMPONENT)&&chunk.equals(c.properties().get("chunk"))))
            throw new IOException("Node ID already belongs to another scene object");
        if(Files.exists(geometry)&&existing==null)
            throw new IOException("Refusing to overwrite an OBJ without an owned terrain scene node");
        if(Files.exists(scenePath)){
            var saved=RuntimeSceneDocument.load(scenePath);
            if(!saved.sceneId().equals(scene.sceneId()))
                throw new IOException("Scene package belongs to another scene");
            for(var old:saved.nodes())if(old.components().stream().anyMatch(c->
                c.type().equals(COMPONENT))&&scene.find(old.id())==null)
                throw new IOException("Refusing to drop previously published chunk "+old.id());
        }
        // A failed geometry build must not modify any scene or published file.
        var generated=TerrainEntranceMesh.build(world,bounds);
        Files.createDirectories(folder);
        Path stage=Files.createTempFile(folder,".terrain-chunk-",".obj");
        Path backup=null;
        var before=scene.snapshot();
        boolean newAsset=false,installed=false;
        try{
            TerrainEntranceMesh.writeObj(generated.mesh(),stage);
            var imported=EditorMeshPipeline.read(stage);
            if(imported.triangles().size()!=generated.mesh().triangles().size())
                throw new IOException("Staged authored mesh does not roundtrip");
            String checksum=sha256(stage);
            int parts=(int)generated.mesh().triangles().stream()
                .map(EditorMeshPipeline.Triangle::part).distinct().count();
            String uri="world://"+FOLDER+"/meshes/"+chunk+".obj";
            EditorAssets.validateUri(uri);
            Map<String,String> meshMeta=new LinkedHashMap<>();
            meshMeta.put("chunk",chunk);
            meshMeta.put("uri",uri);
            meshMeta.put("format","obj");
            meshMeta.put("sha256",checksum);
            meshMeta.put("sourceSha256",sourceFingerprint(world,bounds));
            meshMeta.put("triangles",Integer.toString(imported.triangles().size()));
            meshMeta.put("groups",Integer.toString(parts));
            meshMeta.put("materialStatus","unassigned");
            meshMeta.put("rendererStatus","requires-host-adapter");
            for(var stat:List.of(
                "boundsX0="+bounds.x0(),"boundsZ0="+bounds.z0(),
                "boundsX1="+bounds.x1(),"boundsZ1="+bounds.z1())){
                int split=stat.indexOf('=');
                meshMeta.put(stat.substring(0,split),stat.substring(split+1));
            }
            var components=List.of(
                new RuntimeScene.Component(COMPONENT,meshMeta),
                new RuntimeScene.Component(SOURCE_COMPONENT,Map.of(
                    "source","world://",
                    "topology","adaptive-topology.bin",
                    "entrances","entrance-seams.bin",
                    "ramps","ramp-entrances.bin",
                    "layers","layered-geometry.bin",
                    "navmesh","navmesh-authoring.bin",
                    "collisionStatus","query-authoring-only")));
            var node=new RuntimeScene.Node(nodeId,"Authored terrain "+chunk,
                null,"terrain",RuntimeScene.Transform.identity(),components);

            // File backup remains in the same filesystem for atomic rollback;
            // no source terrain/model asset is ever modified by this operation.
            if(Files.exists(geometry)){
                backup=Files.createTempFile(folder,".previous-terrain-",".obj");
                Files.copy(geometry,backup,StandardCopyOption.REPLACE_EXISTING);
            }else newAsset=true;
            Files.move(stage,geometry,StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING);installed=true;
            scene.replace(node);
            if(!node.equals(scene.find(nodeId)))
                throw new IOException("RuntimeScene adapter did not retain published terrain node");
            // Validate the URI with the same provider used by other portable
            // editor modules before saving the durable scene manifest.
            var assets=new EditorAssets.Registry()
                .register("world",EditorAssets.filesystem(world.directory()));
            try(var stream=assets.open(uri)){
                if(stream.read()<0)throw new IOException("Published terrain mesh is empty");
            }
            if(!sha256(geometry).equals(checksum))
                throw new IOException("Published OBJ checksum failed readback");
            RuntimeSceneDocument.save(scenePath,scene);
            return new Published(nodeId,geometry,scenePath,
                imported.triangles().size(),parts,checksum);
        }catch(IOException|RuntimeException failure){
            scene.restore(before);
            if(installed){
                if(backup!=null)
                    Files.move(backup,geometry,StandardCopyOption.REPLACE_EXISTING);
                else if(newAsset)Files.deleteIfExists(geometry);
            }
            if(failure instanceof IOException problem)throw problem;
            throw new IOException("Terrain scene publishing failed",failure);
        }finally{
            Files.deleteIfExists(stage);
            if(backup!=null)Files.deleteIfExists(backup);
        }
    }
    /** Verify an existing scene package without changing it. */
    public static List<String> inspect(StitchedHeightmapWorld world,RuntimeScene scene)
            throws IOException{
        var issues=new java.util.ArrayList<String>();
        for(var node:scene.nodes())for(var c:node.components())
            if(c.type().equals(COMPONENT)){
                String chunk=c.properties().get("chunk"),uri=c.properties().get("uri");
                try{
                    if(!idForChunk(chunk).equals(node.id()))
                        throw new IOException("Mesh node ID differs from stable chunk ID");
                    if(!("world://"+FOLDER+"/meshes/"+chunk+".obj").equals(uri))
                        throw new IOException("Mesh URI is not the expected owned location");
                    Path file=world.directory().resolve(FOLDER).resolve("meshes")
                        .resolve(chunk+".obj");
                    if(!Files.isRegularFile(file))throw new IOException("Mesh asset is missing");
                    if(!sha256(file).equals(c.properties().get("sha256")))
                        throw new IOException("Mesh has changed since publishing");
                    var p=c.properties();
                    var area=new TerrainEntranceMesh.Bounds(
                        Double.parseDouble(p.get("boundsX0")),
                        Double.parseDouble(p.get("boundsZ0")),
                        Double.parseDouble(p.get("boundsX1")),
                        Double.parseDouble(p.get("boundsZ1")));
                    if(!sourceFingerprint(world,area).equals(p.get("sourceSha256")))
                        throw new IOException("Published mesh is STALE: terrain, seams, ramps or layers changed");
                    var mesh=EditorMeshPipeline.read(file);
                    if(mesh.triangles().size()!=
                        Integer.parseInt(c.properties().get("triangles")))
                        throw new IOException("Mesh triangle inventory mismatch");
                }catch(IOException|RuntimeException bad){
                    issues.add(node.id()+": "+bad.getMessage());
                }
            }
        return List.copyOf(issues);
    }
    /**
     * Content fingerprint of the exact source tile span and the versioned
     * authoring documents that drive mesh emission. Conservative invalidation
     * of one document invalidates all derived chunks; no hash is derived from
     * file timestamps or a visual preview.
     */
    public static String sourceFingerprint(StitchedHeightmapWorld world,
            TerrainEntranceMesh.Bounds bounds)throws IOException {
        Objects.requireNonNull(world);Objects.requireNonNull(bounds);
        var settings=world.settings();
        double scale=settings.metresPerCell();
        long sx=(long)Math.floor((bounds.x0()-settings.originX())/scale);
        long ex=(long)Math.ceil((bounds.x1()-settings.originX())/scale)-1;
        long sz=(long)Math.floor((bounds.z0()-settings.originZ())/scale);
        long ez=(long)Math.ceil((bounds.z1()-settings.originZ())/scale)-1;
        if(ex<sx||ez<sz||ex-sx>2048||ez-sz>2048)
            throw new IOException("Source fingerprint region outside terrain mesh budget");
        long x0=Math.floorDiv(sx,settings.tileCells());
        long x1=Math.floorDiv(ex,settings.tileCells());
        long z0=Math.floorDiv(sz,settings.tileCells());
        long z1=Math.floorDiv(ez,settings.tileCells());
        if((x1-x0+1)*(z1-z0+1)>64)
            throw new IOException("Source fingerprint exceeds 64 tile inputs");
        try{
            var hash=MessageDigest.getInstance("SHA-256");
            update(hash,settings.toString());
            for(long z=z0;z<=z1;z++)for(long x=x0;x<=x1;x++){
                update(hash,"tile:"+x+":"+z);
                Path tile=world.tilePath((int)x,(int)z);
                updateFile(hash,tile);
                // The authored mesh and physical support can now directly
                // consume saved non-destructive sculpt layers. These files
                // MUST be fingerprinted to reject stale published geometry.
                update(hash,"terrain-layer:"+x+":"+z);
                updateFile(hash,world.directory().resolve("terrain-layers")
                    .resolve("tile_"+x+"_"+z+".bin"));
            }
            // Managed road/rail/river corridors are procedural inputs, so
            // modifying their saved controls invalidates published geometry.
            update(hash,"terrain-infrastructure-splines.bin");
            updateFile(hash,world.directory().resolve("terrain-infrastructure-splines.bin"));
            for(String source:List.of("adaptive-topology.bin","entrance-seams.bin",
                    "ramp-entrances.bin","layered-geometry.bin")){
                update(hash,source);
                updateFile(hash,world.directory().resolve(source));
            }
            return java.util.HexFormat.of().formatHex(hash.digest());
        }catch(NoSuchAlgorithmException impossible){
            throw new IllegalStateException("JDK missing SHA-256",impossible);
        }
    }
    private static void update(MessageDigest hash,String text){
        hash.update(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        hash.update((byte)0);
    }
    private static void updateFile(MessageDigest hash,Path path)throws IOException{
        if(!Files.exists(path)){update(hash,"absent");return;}
        if(!Files.isRegularFile(path))throw new IOException("Authoring input is not a file: "+path);
        try(var stream=Files.newInputStream(path)){
            byte[] data=new byte[8192];int n;
            while((n=stream.read(data))!=-1)if(n>0)hash.update(data,0,n);
        }
        hash.update((byte)0);
    }
    private static String sha256(Path path)throws IOException{
        try{
            MessageDigest digest=MessageDigest.getInstance("SHA-256");
            try(var stream=Files.newInputStream(path)){
                byte[] buffer=new byte[8192];int n;
                while((n=stream.read(buffer))>=0)if(n>0)digest.update(buffer,0,n);
            }
            return java.util.HexFormat.of().formatHex(digest.digest());
        }catch(NoSuchAlgorithmException impossible){
            throw new IllegalStateException("JDK missing SHA-256",impossible);
        }
    }
}
