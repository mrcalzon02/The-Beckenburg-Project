package org.beckenburg.editor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

/** Generated physical mesh acceptance, independent from renderer screenshots. */
public final class TerrainEntranceMeshTest {
    private static void check(boolean yes,String reason){
        if(!yes)throw new AssertionError(reason);
    }
    private static double up(EditorMeshPipeline.Triangle t){
        var a=t.a();var b=t.b();var c=t.c();
        return (b.z()-a.z())*(c.x()-a.x())
            -(b.x()-a.x())*(c.z()-a.z());
    }
    public static void main(String[] args)throws Exception{
        Path dir=Files.createTempDirectory("beckenburg-real-entrance-mesh-");
        try{
            var world=StitchedHeightmapWorld.create(dir,
                new StitchedHeightmapWorld.Settings(16,1,0,0,-.01,.01,9112L,4));
            world.generateRegion(0,0,2,1);
            var seams=EntranceSeamDocument.open(world);
            seams.addEdge(new EntranceSeamDocument.EdgeCut("cave-cut",
                EntranceSeamDocument.Method.EXTRA_SEAM,"bunker",4.5,4,4.5,8));
            seams.addEdge(new EntranceSeamDocument.EdgeCut("tile-deweld",
                EntranceSeamDocument.Method.TILE_DESTITCH,"bunker",16,4,16,8));
            seams.addTab(new EntranceSeamDocument.DetachedTab(
                "approach-tab","entrance-tab","bunker",4.5,4,6,6));
            seams.save();
            var topology=AdaptiveTerrainTopology.open(world);
            EntranceSeamTopology.apply(world,topology,seams);
            for(int x=6;x<12;x++)for(int z=4;z<6;z++)
                topology.punch(new AdaptiveTerrainTopology.Node(x,z,0,0,0));
            topology.save();
            var underground=LayeredTerrainDocument.open(world);
            underground.addFloor(new LayeredTerrainQueries.Floor(
                "bunker",12,3,15,7,-2,0,0));
            underground.save();
            var entrances=RampEntranceDocument.open(world);
            var ramp=new LayeredTerrainQueries.Ramp("bunker-mouth","mouth-ramp",
                "bunker",LayeredTerrainQueries.Ramp.Direction.EAST,
                4.7,4,12.7,6,world.height(5,5),-2,0,0);
            entrances.add(ramp);entrances.save();
            var generated=TerrainEntranceMesh.build(world,
                new TerrainEntranceMesh.Bounds(2,2,20,9));
            check(generated.sourceLeaves()>100,"Mesh uses actual subdivided heightmap leaves");
            check(generated.rampTriangles()>0,"Continuous ramp is emitted as genuine 3D triangles");
            check(generated.tabTriangles()>0,"Method 3 preserved detached tab triangles");
            check(generated.exposedWallSegments()>0,
                "Source cutouts adjacent to real ramp emit visible cave cheek walls");
            var tris=generated.mesh().triangles();
            check(tris.size()<TerrainEntranceMesh.MAX_TRIANGLES,"Export remains bounded");
            var parts=new HashSet<String>();
            for(var t:tris){
                parts.add(t.part());
                if(t.part().startsWith("ramp_")){
                    for(var v:new EditorMeshPipeline.Vertex[]{t.a(),t.b(),t.c()})
                        check(Math.abs(v.y()-ramp.height(v.x(),v.z()))<1e-8,
                            "Rendered ramp triangle must share actual collision height");
                }
                if(t.part().startsWith("surface")||t.part().startsWith("tab_"))
                    for(var v:new EditorMeshPipeline.Vertex[]{t.a(),t.b(),t.c()})
                        check(Math.abs(v.y()-world.height(v.x(),v.z()))<1e-7,
                            "Actual source surface and tab geometry preserve pixel-derived height");
                if(t.part().startsWith("surface")||t.part().startsWith("ramp_")
                    ||t.part().startsWith("tab_")||t.part().startsWith("floor_"))
                    check(up(t)>0,"Visible upward terrain and floor triangles must face the sky");
            }
            check(parts.contains("tab_approach-tab")
                &&parts.contains("ramp_bunker-mouth")
                &&parts.contains("floor_bunker")
                &&parts.contains("rock_edge_bunker-mouth"),
                "Parts must preserve tab, ramp, floor and actual carved rock edge identity: "+parts);
            check(parts.stream().anyMatch(x->x.contains("tile-deweld_a"))
                &&parts.stream().anyMatch(x->x.contains("tile-deweld_b")),
                "Method 2 existing tile border must keep two separately grouped sides");
            check(parts.stream().anyMatch(x->x.contains("cave-cut_a"))
                &&parts.stream().anyMatch(x->x.contains("cave-cut_b")),
                "Method 1 interior seam must keep two separately grouped sides");
            var exported=dir.resolve("inspection.obj");
            TerrainEntranceMesh.writeObj(generated.mesh(),exported);
            var read=EditorMeshPipeline.read(exported);
            check(read.triangles().size()==tris.size(),"OBJ roundtrip must preserve face count");
            check(read.triangles().stream().map(EditorMeshPipeline.Triangle::part)
                .collect(java.util.stream.Collectors.toSet()).equals(parts),
                "OBJ roundtrip must preserve all authored part names");
            long vertexLines=Files.lines(exported).filter(x->x.startsWith("v ")).count();
            check(vertexLines==tris.size()*3L,
                "Every face must own its own vertices, without automatic de-stitch re-weld");
            var sparse=StitchedHeightmapWorld.create(dir.resolve("sparse"),
                new StitchedHeightmapWorld.Settings(16,1,0,0,-.01,.01,171L,4));
            sparse.generateRegion(0,0,1,1);
            var sparseResult=TerrainEntranceMesh.build(sparse,
                new TerrainEntranceMesh.Bounds(13,13,18,18));
            check(sparseResult.mesh().triangles().size()>0,
                "Mesh must export outer border of a sparse tile without inventing neighbors");
            check(world.verifySeams().isEmpty(),
                "Mesh export must not modify or corrupt the 16-bit stitched source tiles");
            System.out.println("PASS entrance mesh: physical heights, adaptive edge conformity, "+
                "separate seam/tab part identities, cave walls, OBJ readback, sparse tile border");
        }finally{
            try(var paths=Files.walk(dir)){
                for(var p:paths.sorted(Comparator.reverseOrder()).toList())
                    Files.deleteIfExists(p);
            }
        }
    }
}
