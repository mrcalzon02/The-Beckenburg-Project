package org.beckenburg.editor;

import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Proves two tiles can change their shared height seam as one validated batch. */
public final class StitchedHeightmapBatchTest {
    private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    private static BufferedImage tile(StitchedHeightmapWorld w,int x,int z)throws Exception{
        int n=w.settings().tileCells();
        BufferedImage image=new BufferedImage(n+1,n+1,BufferedImage.TYPE_USHORT_GRAY);
        for(int row=0;row<=n;row++)for(int col=0;col<=n;col++)
            image.getRaster().setSample(col,row,0,w.sample(x,z,col,row));
        return image;
    }
    private static Path save(BufferedImage image,Path file)throws Exception{
        if(!ImageIO.write(image,"png",file.toFile()))
            throw new AssertionError("JDK PNG writer missing");
        return file;
    }
    public static void main(String[] args)throws Exception{
        Path root=Files.createTempDirectory("tile-seam-batch");
        try{
            var settings=new StitchedHeightmapWorld.Settings(32,2,0,0,-200,1000,4512L,4);
            var w=StitchedHeightmapWorld.create(root.resolve("world"),settings);
            w.generateRegion(0,0,2,1);
            int before=w.sample(0,0,32,15);
            check(before==w.sample(1,0,0,15),"Starting tiles have exact seam");
            BufferedImage first=tile(w,0,0),second=tile(w,1,0);
            for(int z=0;z<=32;z++){
                int updated=first.getRaster().getSample(32,z,0)+11;
                first.getRaster().setSample(32,z,0,updated);
                second.getRaster().setSample(0,z,0,updated);
            }
            Path a=save(first,root.resolve("first.png"));
            Path b=save(second,root.resolve("second.png"));
            boolean invalid=false;
            try{w.importTile(0,0,a,true);}
            catch(java.io.IOException expected){invalid=true;}
            check(invalid&&w.sample(0,0,32,15)==before,
                "Single-tile seam change is rightly forbidden");

            invalid=false;
            try{w.importTilesBatch(Map.of(new StitchedHeightmapWorld.Tile(0,0),a),true);}
            catch(java.io.IOException expected){invalid=true;}
            check(invalid&&w.sample(0,0,32,15)==before,
                "Incomplete batch fails preflight without changing source");

            w.importTilesBatch(Map.of(new StitchedHeightmapWorld.Tile(0,0),a,
                new StitchedHeightmapWorld.Tile(1,0),b),true);
            check(w.sample(0,0,32,15)==before+11,"First tile got new shared seam");
            check(w.sample(1,0,0,15)==before+11,"Second tile got identical seam");
            check(w.verifySeams().isEmpty(),"Complete batch is stitched");
            check(!Files.exists(w.directory().resolve(".pending-heightmap-batch")),
                "Successful commit clears transaction journal");
            var reopened=StitchedHeightmapWorld.open(w.directory());
            check(reopened.sample(0,0,32,15)==before+11,
                "Batch commit is durable after reopen");

            // Simulate power loss after the first authoritative file replacement,
            // with a persisted manifest but no COMMITTED marker.
            Path journal=w.directory().resolve(".pending-heightmap-batch");
            Files.createDirectories(journal);
            Files.copy(w.tilePath(0,0),journal.resolve("old_tile_0_0.png"));
            BufferedImage corrupted=tile(w,0,0);
            int previous=corrupted.getRaster().getSample(10,10,0);
            corrupted.getRaster().setSample(10,10,0,previous+7);
            save(corrupted,w.tilePath(0,0));
            Properties manifest=new Properties();
            manifest.setProperty("format","heightmap-batch-v1");
            manifest.setProperty("count","1");
            manifest.setProperty("entry.0","0,0,true");
            try(var out=Files.newOutputStream(journal.resolve("manifest.properties"))){
                manifest.store(out,"simulated interrupted commit");
            }
            var recovered=StitchedHeightmapWorld.open(w.directory());
            check(recovered.sample(0,0,10,10)==previous,
                "Opening an interrupted batch restores original source pixels");
            check(!Files.exists(journal),"Recovery removes completed rollback journal");
            check(recovered.verifySeams().isEmpty(),"Recovery restores matched seams");

            // Simulate interrupted preflight without any published manifest.
            Files.createDirectories(journal);
            Files.writeString(journal.resolve("orphan.tmp"),"staged but never installed");
            StitchedHeightmapWorld.open(w.directory());
            check(!Files.exists(journal),"Orphan staging without manifest is discarded");
            System.out.println("PASS seam-aware two-tile imports, preflight, durability, simulated crash rollback");
        }finally{
            try(var walk=Files.walk(root)){
                for(Path p:walk.sorted(Comparator.reverseOrder()).toList())
                    Files.deleteIfExists(p);
            }
        }
    }
}
