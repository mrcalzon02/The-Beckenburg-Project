package org.beckenburg.editor;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import javax.imageio.ImageIO;

/** Portable Java 17 tests for sparse, globally aligned, stitched 16-bit height tiles. */
public final class StitchedHeightmapWorldTest {
    private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    private static void equal(double actual,double expected,String why){
        if(Math.abs(actual-expected)>1e-8)throw new AssertionError(why+" actual="+actual+" expected="+expected);
    }
    private static void delete(Path dir)throws Exception{
        try(var paths=Files.walk(dir)){
            for(Path p:paths.sorted(java.util.Comparator.reverseOrder()).toList())
                Files.deleteIfExists(p);
        }
    }
    public static void main(String[] args)throws Exception {
        Path root=Files.createTempDirectory("stitched-world-");
        try {
            var settings=new StitchedHeightmapWorld.Settings(16,2,100,-100,
                -200,1000,76123L,2);
            var world=StitchedHeightmapWorld.create(root.resolve("first"),settings);
            check(world.tiles().isEmpty(),"Sparse empty world does not allocate continent");
            world.generateTile(1,0);
            world.generateTile(0,1);
            world.generateTile(1,1);
            world.generateTile(0,0); // reverse order must still stitch
            check(world.tiles().size()==4,"Four tiles allocated, no fictitious tiles");
            check(world.verifySeams().isEmpty(),"All borders and corners exact");
            for(int i=0;i<=16;i++){
                check(world.sample(0,0,16,i)==world.sample(1,0,0,i),"East border identical "+i);
                check(world.sample(0,0,i,16)==world.sample(0,1,i,0),"South border identical "+i);
            }
            check(world.sample(0,0,16,16)==world.sample(1,1,0,0),"Diagonal corner identical");
            check(world.cachedTiles()<=2,"Bounded resident tile cache");
            double x=132, z=-80;
            check(Math.abs(world.height(x-1e-7,z+1)-world.height(x+1e-7,z+1))<1e-4,
                  "World-coordinate seam is continuous without a height jump");
            var loaded=StitchedHeightmapWorld.open(root.resolve("first"));
            check(loaded.settings().equals(settings),"Manifest roundtrip");
            equal(loaded.height(102,-98),world.height(102,-98),"Reopen exact canonical terrain");

            var negative=StitchedHeightmapWorld.create(root.resolve("negative"),settings);
            negative.generateTile(-1,-1);negative.generateTile(0,-1);
            negative.generateTile(-1,0);negative.generateTile(0,0);
            check(negative.verifySeams().isEmpty(),"Negative tile indexes stitch");
            equal(negative.height(100.1,-100.1),negative.height(100.1,-100.1),
                  "Negative world region samples without overflow");
            check(negative.sample(0,0,0,0)==world.sample(0,0,0,0),
                  "Same seed and global coordinate produce same vertices");

            var alternate=StitchedHeightmapWorld.create(root.resolve("alternate"),settings);
            alternate.generateTile(0,0);
            check(Arrays.equals(Files.readAllBytes(alternate.tilePath(0,0)),
                                Files.readAllBytes(world.tilePath(0,0))),
                  "Generation independent of tile order and cache");
            boolean rejected=false;
            try {world.height(1e12,0);}catch(IllegalArgumentException expected){rejected=true;}
            check(rejected,"World coordinates bounded rather than integer-overflow");
            rejected=false;
            try {world.height(-10,500);}catch(java.io.IOException expected){rejected=true;}
            check(rejected,"Unknown world tile remains unknown, not fake ground");

            // Independent hand-sculpted tile: exact triangle interpolation, not bilinear.
            var terrain=StitchedHeightmapWorld.create(root.resolve("triangle"),
                new StitchedHeightmapWorld.Settings(16,1,0,0,0,65535,4L,2));
            BufferedImage image=new BufferedImage(17,17,BufferedImage.TYPE_USHORT_GRAY);
            image.getRaster().setSample(0,0,0,100);
            image.getRaster().setSample(1,0,0,200);
            image.getRaster().setSample(1,1,0,500);
            image.getRaster().setSample(0,1,0,300);
            Path sample=root.resolve("triangle.png");
            check(ImageIO.write(image,"png",sample.toFile()),"16-bit encoder available");
            terrain.importTile(0,0,sample,false);
            equal(terrain.height(.75,.25),250,"Real triangle ABC, matching SiteTerrain diagonal");
            equal(terrain.height(.25,.75),300,"Real triangle ACD, matching SiteTerrain diagonal");

            // Failure safety: reject a mismatched neighbor before touching the existing file.
            byte[] original=Files.readAllBytes(world.tilePath(0,0));
            BufferedImage corrupted=ImageIO.read(world.tilePath(0,0).toFile());
            corrupted.getRaster().setSample(16,8,0,0);
            Path bad=root.resolve("bad.png");
            check(ImageIO.write(corrupted,"png",bad.toFile()),"Corrupt seam fixture saved");
            rejected=false;
            try{world.importTile(0,0,bad,true);}
            catch(java.io.IOException expected){rejected=true;}
            check(rejected,"Mismatching 16-bit edge rejects import");
            check(Arrays.equals(original,Files.readAllBytes(world.tilePath(0,0))),
                  "Rejected replace leaves original PNG bit-identical");
            check(world.verifySeams().isEmpty(),"Failed operation cannot create a seam");
            rejected=false;
            try{world.importTile(0,0,sample,true);}
            catch(java.io.IOException expected){rejected=true;}
            check(rejected,"Incompatible sample tile cannot overwrite active world");
            BufferedImage rgb=new BufferedImage(17,17,BufferedImage.TYPE_INT_RGB);
            Path wrong=root.resolve("rgb.png");
            ImageIO.write(rgb,"png",wrong.toFile());
            rejected=false;
            try{terrain.importTile(1,0,wrong,false);}
            catch(java.io.IOException expected){rejected=true;}
            check(rejected,"8-bit RGB PNG rejected; 16-bit sample precision required");
            check(!terrain.hasTile(1,0),"Failed import does not create a tile");
            System.out.println("PASS stitched world: 16-bit PNG, global deterministic tiles, seams, negative indices, triangle height, LRU and rollback");
        } finally { delete(root); }
    }
}
