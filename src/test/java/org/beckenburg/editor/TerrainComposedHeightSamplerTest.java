package org.beckenburg.editor;

import java.nio.file.*;
import java.util.*;

/**
 * A sculpted, masked, non-baked layer must affect the SAME physical support
 * heights and authored terrain triangles; publishing must detect changed
 * source-layer documents, not only changed PNGs.
 */
public final class TerrainComposedHeightSamplerTest {
    private static void check(boolean yes,String message){
        if(!yes)throw new AssertionError(message);
    }
    private static void near(double a,double b,String why){
        check(Math.abs(a-b)<1e-7,why+": "+a+" vs "+b);
    }
    public static void main(String[] args)throws Exception{
        Path dir=Files.createTempDirectory("terrain-composed-physical-mesh");
        try{
            var world=StitchedHeightmapWorld.create(dir.resolve("world"),
                new StitchedHeightmapWorld.Settings(16,1,0,0,-200,600,873L,4));
            world.generateRegion(0,0,2,1);
            var area=new TerrainEntranceMesh.Bounds(3,3,12,12);
            double source=world.height(7,7);
            String fingerprintBefore=TerrainScenePackage.sourceFingerprint(world,area);
            var authored=new TerrainLayerStack(world,0,0);
            authored.brush(TerrainLayerStack.Brush.RAISE,7,7,4,20,0,0,1);
            authored.finishStroke();
            authored.save();
            var reference=new TerrainComposedHeightSampler(world);
            check(reference.height(7,7)>source+19,
                "Unbaked sculpt layer raises composed mesh and support datum");
            near(world.height(7,7),source,
                "Unbaked layers never rewrite authoritative 16-bit source");
            var physical=new LayeredTerrainQueries(world,
                AdaptiveTerrainTopology.open(world),List.of(),List.of());
            near(physical.support("surface",7,7).y(),reference.height(7,7),
                "Physical surface query uses composed layer, not raw source height");
            near(physical.groundHeight(7.25,7.75),
                reference.height(7.25,7.75),
                "Fractional physical queries use exact same triangle interpolation");
            var mesh=TerrainEntranceMesh.build(world,area);
            int checked=0,raised=0;
            for(var tri:mesh.mesh().triangles()){
                if(!tri.part().startsWith("surface"))continue;
                for(var vertex:new EditorMeshPipeline.Vertex[]{
                        tri.a(),tri.b(),tri.c()}){
                    near(vertex.y(),reference.height(vertex.x(),vertex.z()),
                        "Real emitted triangle matches authored support height");
                    checked++;
                    if(Math.hypot(vertex.x()-7,vertex.z()-7)<3 &&
                        vertex.y()>world.height(vertex.x(),vertex.z())+.1)raised++;
                }
            }
            check(checked>50&&raised>0,
                "Authored mesh genuinely includes non-baked layer height changes");
            String withLayer=TerrainScenePackage.sourceFingerprint(world,area);
            check(!withLayer.equals(fingerprintBefore),
                "Published scene fingerprint includes new layer authoring document");

            authored.paintLayerMask(7,7,4,0,1);
            authored.finishStroke();authored.save();
            var masked=new TerrainComposedHeightSampler(world);
            near(masked.height(7,7),source,"Zero alpha mask hides sculpt at center");
            near(physical.groundHeight(7,7),source,
                "Existing query instance reloads updated layer data");
            // A new surface query instance always reads the latest layer
            // document. The current query's sampler is intentionally scoped
            // to one build/use session and must be reconstructed after edits.
            var refreshed=new LayeredTerrainQueries(world,
                AdaptiveTerrainTopology.open(world),List.of(),List.of());
            near(refreshed.groundHeight(7,7),source,
                "Reopened query adapter sees latest mask state");
            check(!TerrainScenePackage.sourceFingerprint(world,area).equals(withLayer),
                "A mask-only edit invalidates previously exported mesh packages");
            var remeshed=TerrainEntranceMesh.build(world,area);
            boolean foundCenter=false;
            for(var tri:remeshed.mesh().triangles())
                if(tri.part().startsWith("surface"))
                    for(var v:new EditorMeshPipeline.Vertex[]{tri.a(),tri.b(),tri.c()})
                        if(Math.abs(v.x()-7)<1e-7&&Math.abs(v.z()-7)<1e-7){
                            foundCenter=true;near(v.y(),source,
                                "Remeshing honors masked height at center");
                        }
            check(foundCenter,"Real mesh includes a central source vertex");
            near(world.height(7,7),source,"Authored geometry never mutates source");
            check(world.verifySeams().isEmpty(),"Shared source seams remain intact");
            System.out.println("PASS composed terrain meshes, physical support queries, masked edits and scene fingerprints");
        }finally{
            try(var walk=Files.walk(dir)){
                for(Path path:walk.sorted(Comparator.reverseOrder()).toList())
                    Files.deleteIfExists(path);
            }
        }
    }
}
