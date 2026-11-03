package org.beckenburg;

import org.beckenburg.editor.EditorMeshPipeline;
import org.beckenburg.editor.RuntimeScene;
import org.beckenburg.editor.StitchedHeightmapWorld;
import org.beckenburg.editor.TerrainRailScenePackage;
import org.beckenburg.editor.TerrainScenePackage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;

/**
 * Host adapter for the portable scene's REAL published OBJ geometry. Bakes the
 * same triangles into the existing, authoritative HCS2 gameplay physics path:
 * no invented collision boxes, parallel physics solver, or phantom nav surface.
 *
 * This is an opt-in import for bounded game areas, not automatic conversion of
 * every arbitrarily large terrain world into a 15-zone investigation level.
 */
public final class TerrainSceneNativePhysics {
    private TerrainSceneNativePhysics(){}
    public record Loaded(AuthoredSitePhysics physics,String sourceSha256,
                         int triangles,int supportTriangles,int parts) {}
    private record Surface(String owner,EditorMeshPipeline.Triangle triangle,int flags) {}
    private static final int SUPPORT=AuthoredSitePhysics.SUPPORT,
        OPTICAL=AuthoredSitePhysics.OPTICAL,BODY=AuthoredSitePhysics.BODY;
    private static final int TRIANGLE_LIMIT=350_000,PART_LIMIT=15_000;
    private static String hash(byte[] contents){
        try{return HexFormat.of().formatHex(
            MessageDigest.getInstance("SHA-256").digest(contents));
        }catch(NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    }
    private static boolean support(EditorMeshPipeline.Triangle t,boolean rail){
        // A steel railhead and transverse sleeper are physical obstructions,
        // not invented full-width walkable navigation platforms.
        if(rail&&!t.part().equals("rail_ballast"))return false;
        var a=t.a();var b=t.b();var c=t.c();
        double ux=b.x()-a.x(),uy=b.y()-a.y(),uz=b.z()-a.z();
        double vx=c.x()-a.x(),vy=c.y()-a.y(),vz=c.z()-a.z();
        double nx=uy*vz-uz*vy,ny=uz*vx-ux*vz,nz=ux*vy-uy*vx;
        return ny>1e-8&&Math.hypot(nx,nz)/ny<=AuthoredSitePhysics.MAX_GRADE+1e-7;
    }
    private static void checked(EditorMeshPipeline.Triangle t)throws IOException{
        for(var v:new EditorMeshPipeline.Vertex[]{t.a(),t.b(),t.c()}){
            if(!Double.isFinite(v.x())||!Double.isFinite(v.y())||!Double.isFinite(v.z())
                ||Math.abs(v.x())>=256||Math.abs(v.y())>=256||Math.abs(v.z())>=256)
                throw new IOException("Authored scene outside bounded HCS2 physics domain (-256,+256 metres)");
        }
    }
    public static Loaded load(StitchedHeightmapWorld world,RuntimeScene scene)
            throws IOException{
        byte[] encoded=encode(world,scene);
        String checksum=hash(encoded);
        var runtime=AuthoredSitePhysics.read(new ByteArrayInputStream(encoded),checksum,List.of());
        int walk=0;
        for(int i=0;i<runtime.triangleCount();i++)if((runtime.triangle(i).flags()&SUPPORT)!=0)walk++;
        return new Loaded(runtime,checksum,runtime.triangleCount(),walk,runtime.componentCount());
    }
    /** Exact bytes consumed by the runtime; also used by the packaged-site exporter. */
    public static byte[] encode(StitchedHeightmapWorld world,RuntimeScene scene)throws IOException{
        Objects.requireNonNull(world);Objects.requireNonNull(scene);
        var issues=new ArrayList<String>(TerrainScenePackage.inspect(world,scene));
        issues.addAll(TerrainRailScenePackage.inspect(world,scene));
        if(!issues.isEmpty())throw new IOException("Published scene integrity failed: "+issues);
        // HCS2 uses per-component contiguous triangles. A group key includes
        // the node ID so two rail routes cannot share material/closed-volume
        // ownership accidentally.
        var groups=new LinkedHashMap<String,List<Surface>>();
        int total=0,walk=0;
        for(var node:scene.nodes())for(var component:node.components()){
            boolean rail=component.type().equals(TerrainRailScenePackage.COMPONENT);
            boolean terrain=component.type().equals(TerrainScenePackage.COMPONENT);
            if(!rail&&!terrain)continue;
            Path mesh=world.directory().resolve(TerrainScenePackage.FOLDER)
                .resolve("meshes").resolve(rail
                    ?"rail-"+component.properties().get("route")+".obj"
                    :component.properties().get("chunk")+".obj");
            // inspect() checked the owning URI and SHA before ANY file read.
            var source=EditorMeshPipeline.read(mesh);
            for(var t:source.triangles()){
                checked(t);
                boolean canWalk=support(t,rail);
                int flags=BODY|OPTICAL|(canWalk?SUPPORT:0);
                String group=node.id()+":"+t.part();
                var list=groups.computeIfAbsent(group,unused->new ArrayList<>());
                list.add(new Surface(group,t,flags));
                total++;if(canWalk)walk++;
                if(total>TRIANGLE_LIMIT||groups.size()>PART_LIMIT)
                    throw new IOException("Native scene physics export exceeds bounded geometry budget");
            }
        }
        if(total==0)throw new IOException("No verified terrain or railway meshes in scene");
        if(walk==0)throw new IOException("Published scene has no upward walking support triangles");
        byte[] encoded;
        try(var buffer=new ByteArrayOutputStream();var out=new DataOutputStream(buffer)){
            out.writeInt(0x48435332);out.writeInt(groups.size());out.writeInt(total);
            for(var group:groups.entrySet()){
                byte[] name=group.getKey().getBytes(StandardCharsets.UTF_8);
                if(name.length==0||name.length>256)
                    throw new IOException("Authored scene part name exceeds native physics limit");
                out.writeShort(name.length);out.write(name);
                // The saved mesh may contain disconnected extrusions; unlike
                // an audited closed solid, it must not use parity-filled
                // invisible collision interiors.
                out.writeByte(0);out.writeInt(group.getValue().size());
                for(var surface:group.getValue()){
                    var t=surface.triangle();
                    for(var v:new EditorMeshPipeline.Vertex[]{t.a(),t.b(),t.c()}){
                        out.writeFloat((float)v.x());out.writeFloat((float)v.y());out.writeFloat((float)v.z());
                    }
                    out.writeByte(surface.flags());
                }
            }
            out.flush();
            if(buffer.size()>30_000_000)
                throw new IOException("Published scene exceeds HCS2 runtime byte budget");
            encoded=buffer.toByteArray();
        }
        return encoded;
    }
}
