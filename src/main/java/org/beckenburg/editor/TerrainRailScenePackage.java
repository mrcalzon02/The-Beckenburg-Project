package org.beckenburg.editor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Publishes saved railway alignments into the SAME durable world scene package
 * used by terrain chunks. Geometry and provenance are real; runtime collision,
 * navigation and material assignment remain explicit responsibilities of a host.
 * No Beckenburg gameplay class is referenced from this portable module.
 */
public final class TerrainRailScenePackage {
    public static final String COMPONENT="terrain-rail-asset";
    private static final String PREFIX="rail-";
    private TerrainRailScenePackage(){}

    public record Published(String nodeId,Path geometryFile,Path sceneFile,
                            int triangles,int sleepers,String checksum) {}

    public static String idForRail(String routeId){
        if(routeId==null||!routeId.matches("[A-Za-z0-9_-]{1,64}"))
            throw new IllegalArgumentException("Invalid saved rail alignment ID");
        return "terrain-rail-"+routeId;
    }
    private static String sha256(Path file)throws IOException{
        try{
            MessageDigest hash=MessageDigest.getInstance("SHA-256");
            try(var in=Files.newInputStream(file)){
                byte[] buf=new byte[8192];int n;
                while((n=in.read(buf))!=-1)if(n>0)hash.update(buf,0,n);
            }
            return HexFormat.of().formatHex(hash.digest());
        }catch(NoSuchAlgorithmException impossible){
            throw new IllegalStateException("SHA-256 unavailable",impossible);
        }
    }
    /** Invalidates published rails if any persisted route or world geometry scale changes. */
    public static String sourceFingerprint(StitchedHeightmapWorld world)throws IOException{
        Objects.requireNonNull(world);
        Path path=world.directory().resolve("terrain-infrastructure-splines.bin");
        if(!Files.isRegularFile(path))throw new IOException("Save the rail alignments first");
        try{
            MessageDigest digest=MessageDigest.getInstance("SHA-256");
            digest.update(world.settings().toString().getBytes(StandardCharsets.UTF_8));
            digest.update(TerrainRailMesh.Profile.standard().toString()
                .getBytes(StandardCharsets.UTF_8));
            try(var in=Files.newInputStream(path)){
                byte[] buf=new byte[8192];int n;
                while((n=in.read(buf))!=-1)if(n>0)digest.update(buf,0,n);
            }
            return HexFormat.of().formatHex(digest.digest());
        }catch(NoSuchAlgorithmException impossible){
            throw new IllegalStateException("SHA-256 unavailable",impossible);
        }
    }
    public static Published publish(StitchedHeightmapWorld world,RuntimeScene scene,
                                    String routeId)throws IOException{
        Objects.requireNonNull(world);Objects.requireNonNull(scene);
        String nodeId=idForRail(routeId);
        var route=TerrainInfrastructureSplines.open(world).list().stream()
            .filter(a->a.id().equals(routeId)).findFirst()
            .orElseThrow(()->new IOException("No saved alignment named "+routeId));
        if(route.kind()!=TerrainInfrastructureSplines.Kind.RAIL)
            throw new IOException("Only RAIL alignments may publish railway geometry");
        Path root=world.directory().resolve(TerrainScenePackage.FOLDER);
        Path folder=root.resolve("meshes");
        Path file=folder.resolve(PREFIX+routeId+".obj");
        Path sceneFile=root.resolve("scene.bin");
        var old=scene.find(nodeId);
        if(old!=null&&!old.components().stream().anyMatch(c->
            c.type().equals(COMPONENT)&&routeId.equals(c.properties().get("route"))))
            throw new IOException("Rail node ID belongs to another object");
        if(Files.exists(file)&&old==null)
            throw new IOException("Refusing to overwrite an unowned existing railway OBJ");
        if(Files.exists(sceneFile)){
            var saved=RuntimeSceneDocument.load(sceneFile);
            if(!saved.sceneId().equals(scene.sceneId()))
                throw new IOException("Scene manifest belongs to another scene");
            // A stale editor must not silently delete or rewrite another editor's
            // saved terrain and rail nodes while publishing this route.
            for(var node:saved.nodes())if(node.components().stream().anyMatch(c->
                    c.type().equals(COMPONENT)||c.type().equals(TerrainScenePackage.COMPONENT))){
                if(!node.equals(scene.find(node.id())))
                    throw new IOException("Saved scene changed; reload before publishing "+node.id());
            }
        }
        // Validate the entire mesh before touching either existing asset or scene.
        var output=TerrainRailMesh.build(world.settings(),route,TerrainRailMesh.Profile.standard());
        Files.createDirectories(folder);
        Path staged=Files.createTempFile(folder,".staged-rail-",".obj");
        Path backup=null;
        var before=scene.snapshot();
        boolean installed=false,wasNew=!Files.exists(file);
        try{
            TerrainEntranceMesh.writeObj(output.mesh(),staged);
            var imported=EditorMeshPipeline.read(staged);
            if(imported.triangles().size()!=output.mesh().triangles().size())
                throw new IOException("Staged railway OBJ failed triangle roundtrip");
            String checksum=sha256(staged);
            String uri="world://"+TerrainScenePackage.FOLDER+"/meshes/"+PREFIX+routeId+".obj";
            EditorAssets.validateUri(uri);
            var p=new LinkedHashMap<String,String>();
            p.put("route",routeId);p.put("uri",uri);p.put("format","obj");
            p.put("sha256",checksum);p.put("sourceSha256",sourceFingerprint(world));
            p.put("triangles",Integer.toString(imported.triangles().size()));
            p.put("sleepers",Integer.toString(output.sleepers()));
            p.put("lengthMetres",Double.toString(output.lengthMetres()));
            p.put("materialStatus","unassigned");p.put("rendererStatus","requires-host-adapter");
            p.put("collisionStatus","requires-host-adapter");
            var node=new RuntimeScene.Node(nodeId,"Authored rail "+routeId,
                null,"infrastructure",RuntimeScene.Transform.identity(),
                List.of(new RuntimeScene.Component(COMPONENT,p)));
            if(!wasNew){
                backup=Files.createTempFile(folder,".previous-rail-",".obj");
                Files.copy(file,backup,StandardCopyOption.REPLACE_EXISTING);
            }
            Files.move(staged,file,StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING);
            installed=true;
            scene.replace(node);
            if(!node.equals(scene.find(nodeId)))
                throw new IOException("Scene adapter rejected authored railway node");
            var assets=new EditorAssets.Registry()
                .register("world",EditorAssets.filesystem(world.directory()));
            try(var stream=assets.open(uri)){
                if(stream.read()<0)throw new IOException("Published rail geometry is empty");
            }
            if(!checksum.equals(sha256(file)))
                throw new IOException("Published railway OBJ checksum mismatch");
            RuntimeSceneDocument.save(sceneFile,scene);
            return new Published(nodeId,file,sceneFile,imported.triangles().size(),
                output.sleepers(),checksum);
        }catch(IOException|RuntimeException failure){
            scene.restore(before);
            if(installed){
                if(backup!=null)Files.move(backup,file,StandardCopyOption.REPLACE_EXISTING);
                else if(wasNew)Files.deleteIfExists(file);
            }
            if(failure instanceof IOException io)throw io;
            throw new IOException("Railway scene publish failed",failure);
        }finally{
            Files.deleteIfExists(staged);
            if(backup!=null)Files.deleteIfExists(backup);
        }
    }
    public static List<String> inspect(StitchedHeightmapWorld world,RuntimeScene scene){
        var problems=new ArrayList<String>();
        for(var node:scene.nodes())for(var component:node.components()){
            if(!component.type().equals(COMPONENT))continue;
            var p=component.properties();
            String route=p.get("route");
            try{
                if(!idForRail(route).equals(node.id()))
                    throw new IOException("Rail ID differs from authored route ID");
                String uri="world://"+TerrainScenePackage.FOLDER+"/meshes/"+PREFIX+route+".obj";
                if(!uri.equals(p.get("uri")))
                    throw new IOException("Rail URI differs from expected owned asset");
                Path geometry=world.directory().resolve(TerrainScenePackage.FOLDER)
                    .resolve("meshes").resolve(PREFIX+route+".obj");
                if(!Files.isRegularFile(geometry))
                    throw new IOException("Railway OBJ is missing");
                if(!sha256(geometry).equals(p.get("sha256")))
                    throw new IOException("Railway OBJ was modified after publishing");
                if(!sourceFingerprint(world).equals(p.get("sourceSha256")))
                    throw new IOException("Published railway is STALE: saved routes or world settings changed");
                var routes=TerrainInfrastructureSplines.open(world).list();
                if(routes.stream().noneMatch(a->a.id().equals(route)
                    &&a.kind()==TerrainInfrastructureSplines.Kind.RAIL))
                    throw new IOException("Published route no longer exists as railway");
                var mesh=EditorMeshPipeline.read(geometry);
                if(mesh.triangles().size()!=Integer.parseInt(p.get("triangles")))
                    throw new IOException("Rail triangle inventory mismatch");
                for(String group:List.of("rail_ballast","rail_left","rail_right","rail_sleepers"))
                    if(mesh.triangles().stream().noneMatch(t->t.part().equals(group)))
                        throw new IOException("Rail missing actual geometry group "+group);
            }catch(IOException|RuntimeException bad){
                problems.add(node.id()+": "+bad.getMessage());
            }
        }
        return List.copyOf(problems);
    }
}
