package org.beckenburg.editor;

import java.nio.file.*;
/** Terrain-derived field contract for independent materials, foliage and nav consumers. */
public final class TerrainTopologyFieldsTest {
    private static void check(boolean ok,String message){
        if(!ok)throw new AssertionError(message);
    }
    public static void main(String[] args)throws Exception {
        Path root=Files.createTempDirectory("terrain-topology-fields");
        try {
            var world=StitchedHeightmapWorld.create(root.resolve("source"),
                new StitchedHeightmapWorld.Settings(32,2,0,0,-200,600,5371L,4));
            world.generateRegion(0,0,2,1);
            var t=new TerrainLayerStack(world,0,0);
            var fields=TerrainTopologyFields.calculate(t);
            check(fields.cells()==32,"Topology matches input resolution");
            double accumulationMax=0;
            for(int z=0;z<=32;z++)for(int x=0;x<=32;x++){
                double s=fields.slopeDegrees(x,z),k=fields.curvature(x,z),
                    fx=fields.flowX(x,z),fz=fields.flowZ(x,z),
                    accumulation=fields.accumulation(x,z),m=fields.moisture(x,z);
                check(Double.isFinite(s)&&s>=0&&s<90,"Finite slope");
                check(Double.isFinite(k)&&Double.isFinite(fx)&&Double.isFinite(fz),
                    "Finite curvature and flow proxy");
                check(accumulation>=1&&m>=0&&m<=1,"Nonnegative drainage and moisture");
                accumulationMax=Math.max(accumulationMax,accumulation);
            }
            check(accumulationMax>2,"Drainage accumulates upstream terrain");
            fields.save(t);
            check(Files.isRegularFile(TerrainTopologyFields.path(t)),
                "Versioned derived document exists");
            var loaded=TerrainTopologyFields.open(t);
            check(loaded.fingerprint().equals(fields.fingerprint()),
                "Derived topology fingerprint survives disk");
            check(loaded.accumulation(12,12)==fields.accumulation(12,12),
                "Readback preserves accumulated flow");
            int source=world.sample(0,0,16,16);
            t.paintMaterial(16,16,2,TerrainLayerStack.Material.MUD,90);
            t.finishStroke();
            // Material paint does not affect slope/flow and should not invalidate.
            TerrainTopologyFields.open(t);
            t.brush(TerrainLayerStack.Brush.RAISE,16,16,2,8,0,0,1);
            t.finishStroke();
            boolean invalidated=false;
            try{TerrainTopologyFields.open(t);}
            catch(java.io.IOException expected){invalidated=true;}
            check(invalidated,"Height edit invalidates stale flow/curvature cache");
            check(world.sample(0,0,16,16)==source,
                "No source image modified by derived calculations");
            check(world.verifySeams().isEmpty(),"Source seams preserved");
            var rebuilt=TerrainTopologyFields.calculate(t);
            rebuilt.save(t);
            check(TerrainTopologyFields.open(t).fingerprint().equals(rebuilt.fingerprint()),
                "Explicit rebuild replaces stale derived data");
            System.out.println("PASS derived topology flow, curvature, stale-cache rejection and readback");
        }finally {
            try(var paths=Files.walk(root)){
                for(Path path:paths.sorted(java.util.Comparator.reverseOrder()).toList())
                    Files.deleteIfExists(path);
            }
        }
    }
}
