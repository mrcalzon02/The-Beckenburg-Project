package org.beckenburg.editor;

import java.nio.file.*;

/** Two open workbenches must never silently overwrite each other's source-backed documents. */
public final class TerrainLayerConcurrentSaveTest {
    private static void check(boolean condition,String why){
        if(!condition)throw new AssertionError(why);
    }
    public static void main(String[] args)throws Exception{
        Path root=Files.createTempDirectory("terrain-open-editor-race");
        try {
            var world=StitchedHeightmapWorld.create(root.resolve("world"),
                new StitchedHeightmapWorld.Settings(32,2,0,0,-100,600,825L,4));
            world.generateTile(0,0);
            var first=new TerrainLayerStack(world,0,0);
            var second=new TerrainLayerStack(world,0,0);
            first.addLayer("Author one");first.save();
            boolean refused=false;
            try{second.save();}
            catch(java.io.IOException expected){refused=true;}
            check(refused,"A stale editor cannot overwrite a newly created layer file");
            var reopened=new TerrainLayerStack(world,0,0);
            check(reopened.layerCount()==2,"Author one's layers survive stale save attempt");
            var competing=new TerrainLayerStack(world,0,0);
            reopened.paintMaterial(12,12,2,TerrainLayerStack.Material.MUD,90);
            reopened.finishStroke();reopened.save();
            competing.paintMaterial(16,16,2,TerrainLayerStack.Material.SNOW,90);
            competing.finishStroke();
            refused=false;
            try{competing.save();}
            catch(java.io.IOException expected){refused=true;}
            check(refused,"Stale editor cannot overwrite a modified saved document");
            check(new TerrainLayerStack(world,0,0).material(12,12)==
                TerrainLayerStack.Material.MUD.ordinal(),
                "Most recent authored material remains intact");
            int original=world.sample(0,0,16,16);
            refused=false;
            try{competing.bake();}
            catch(java.io.IOException expected){refused=true;}
            check(refused,"Stale editor must not source-bake before checking disk revision");
            check(world.sample(0,0,16,16)==original,
                "Rejected stale bake leaves authoritative PNG unmodified");
            check(world.verifySeams().isEmpty(),"No incorrect source changes");
            System.out.println("PASS stale source-layer save and bake protection with two open editors");
        }finally{
            try(var walk=Files.walk(root)){
                for(Path p:walk.sorted(java.util.Comparator.reverseOrder()).toList())
                    Files.deleteIfExists(p);
            }
        }
    }
}
