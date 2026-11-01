package org.beckenburg.editor;

import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

/**
 * Verifies coordinated crash recovery for authored layers and source PNGs.
 * Heightmap rollback and layer rollback are the same transaction, not two
 * independent best-effort saves.
 */
public final class StitchedHeightmapLayerRebaseTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static Path image(StitchedHeightmapWorld w,int x,int z,Path file,
                              int offsetX,int offsetZ,int delta)throws Exception {
        int n=w.settings().tileCells();
        BufferedImage img=new BufferedImage(n+1,n+1,BufferedImage.TYPE_USHORT_GRAY);
        for(int row=0;row<=n;row++)for(int col=0;col<=n;col++){
            int old=w.sample(x,z,col,row);
            if(col==offsetX&&row==offsetZ)old+=delta;
            img.getRaster().setSample(col,row,0,old);
        }
        if(!ImageIO.write(img,"png",file.toFile()))
            throw new AssertionError("Missing R16 PNG writer");
        return file;
    }
    public static void main(String[] args)throws Exception{
        Path dir=Files.createTempDirectory("layer-rebase-journal");
        try{
            StitchedHeightmapWorld world=StitchedHeightmapWorld.create(dir.resolve("world"),
                new StitchedHeightmapWorld.Settings(32,2,0,0,-200,900,6423L,4));
            world.generateTile(0,0);
            var editor=new TerrainLayerStack(world,0,0);
            editor.brush(TerrainLayerStack.Brush.RAISE,14,14,3,4,0,0,1);
            editor.finishStroke();
            editor.paintLayerMask(14,14,3,0,.6);editor.finishStroke();
            editor.save();
            Path layer=editor.path(),source=world.tilePath(0,0);
            byte[] originalLayer=Files.readAllBytes(layer);
            byte[] originalPNG=Files.readAllBytes(source);
            int originalSample=world.sample(0,0,14,14);
            var key=new StitchedHeightmapWorld.Tile(0,0);
            Path candidate=image(world,0,0,dir.resolve("candidate.png"),14,14,5);
            boolean rejected=false;
            try{world.importTilesBatch(Map.of(key,candidate),true);}
            catch(java.io.IOException correct){rejected=true;}
            check(rejected,"Ordinary tile importer rejects source replacement with saved layers");
            check(Arrays.equals(originalLayer,Files.readAllBytes(layer))&&
                Arrays.equals(originalPNG,Files.readAllBytes(source)),
                "Rejected batch cannot mutate either original file");

            // Simulate a crash AFTER replacing both the PNG and layer document
            // but BEFORE publishing the COMMITTED transaction marker.
            Path journal=world.directory().resolve(".pending-heightmap-batch");
            Files.createDirectories(journal);
            Files.copy(source,journal.resolve("old_tile_0_0.png"));
            Files.copy(layer,journal.resolve("old_layer_tile_0_0.bin"));
            Files.copy(candidate,source,StandardCopyOption.REPLACE_EXISTING);
            Files.write(layer,new byte[]{2,4,6,8});
            Properties manifest=new Properties();
            manifest.setProperty("format","heightmap-batch-v2");
            manifest.setProperty("count","1");
            manifest.setProperty("entry.0","0,0,true");
            manifest.setProperty("layerCount","1");
            manifest.setProperty("layer.0","0,0");
            try(var out=Files.newOutputStream(journal.resolve("manifest.properties"))){
                manifest.store(out,"simulated interrupted coordinated rebase");
            }
            var recovered=StitchedHeightmapWorld.open(world.directory());
            check(recovered.sample(0,0,14,14)==originalSample,
                "Recovery restores authoritative R16 PNG");
            check(Arrays.equals(originalPNG,Files.readAllBytes(source)),
                "Recovery restores exact source file bytes");
            check(Arrays.equals(originalLayer,Files.readAllBytes(layer)),
                "Recovery also restores exact layer bytes");
            check(!Files.exists(journal),"Recovery removes v2 transaction journal");
            var restored=new TerrainLayerStack(recovered,0,0);
            check(restored.layerMask(0,14,14)<1,
                "Recovered document still contains authored layer mask");

            // Simulate lost original layer backup. Recovery must refuse
            // to partially roll back the source without the authored file.
            Files.createDirectories(journal);
            Files.copy(source,journal.resolve("old_tile_0_0.png"));
            try(var out=Files.newOutputStream(journal.resolve("manifest.properties"))){
                manifest.store(out,"missing layer backup");
            }
            boolean blocked=false;
            try{StitchedHeightmapWorld.open(world.directory());}
            catch(java.io.IOException correct){blocked=true;}
            check(blocked,"Incomplete authored rollback refuses partial recovery");
            check(Files.exists(journal),"Recovery journal retained for diagnosis");
            Files.deleteIfExists(journal.resolve("manifest.properties"));
            Files.deleteIfExists(journal.resolve("old_tile_0_0.png"));
            Files.deleteIfExists(journal);
            check(recovered.verifySeams().isEmpty(),"Untouched terrain still has safe seams");
            System.out.println("PASS opt-in coordinated rebase, v2 interrupted transaction recovery, and backup safety");
        }finally{
            try(var walk=Files.walk(dir)){
                for(Path path:walk.sorted(Comparator.reverseOrder()).toList())
                    Files.deleteIfExists(path);
            }
        }
    }
}
