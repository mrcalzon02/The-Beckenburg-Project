package org.beckenburg.editor;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.CRC32;

/** Non-destructive per-layer opacity masks and v1 terrain file migration. */
public final class TerrainLayerMaskTest {
    private static void check(boolean condition,String message){
        if(!condition)throw new AssertionError(message);
    }
    private static void near(double actual,double expected,String message){
        check(Math.abs(actual-expected)<.0005,message+": "+actual+" / "+expected);
    }
    private static long crc(StitchedHeightmapWorld world)throws IOException{
        CRC32 result=new CRC32();
        int n=world.settings().tileCells();
        for(int z=0;z<=n;z++)for(int x=0;x<=n;x++){
            int v=world.sample(0,0,x,z);
            result.update(v&255);result.update(v>>>8);
        }
        return result.getValue();
    }
    public static void main(String[] args)throws Exception{
        Path root=Files.createTempDirectory("terrain-layer-mask");
        try {
            var world=StitchedHeightmapWorld.create(root.resolve("world"),
                new StitchedHeightmapWorld.Settings(32,2,0,0,-200,600,1051L,4));
            world.generateTile(0,0);
            var t=new TerrainLayerStack(world,0,0);
            double baseline=t.height(16,16);
            var events=new ArrayList<TerrainLayerStack.EditEvent>();
            t.addChangeListener(events::add);
            t.brush(TerrainLayerStack.Brush.RAISE,16,16,2,10,0,0,1);
            t.finishStroke();
            double raised=t.height(16,16);
            check(raised>baseline+9,"First layer has real raised geometry");
            t.paintLayerMask(16,16,3,0,1);
            check(events.size()==1,"No per-pixel mask event before completion");
            t.finishStroke();
            check(events.size()==2&&events.get(1).domains().contains(
                TerrainLayerStack.EditDomain.LAYER_MASK),
                "Layer mask event emitted at stroke completion");
            near(t.layerMask(0,16,16),0,"Mask erased at brush center");
            near(t.height(16,16),baseline,
                "Mask zero removes sculpt contribution without changing original delta");
            check(t.undo(),"Undo mask stroke");
            near(t.layerMask(0,16,16),1,"Undo restores layer influence");
            near(t.height(16,16),raised,"Undo restores original sculpted height");
            check(t.redo(),"Redo mask stroke");
            near(t.height(16,16),baseline,"Redo masks sculpted height");
            t.addLayer("Fine detail");
            t.brush(TerrainLayerStack.Brush.RAISE,16,16,2,5,0,0,1);
            t.finishStroke();
            check(t.height(16,16)>baseline+4,
                "Second sculpt layer contributes independently of first layer mask");
            check(t.layerMask(1,16,16)==1,"New layer starts with full influence");
            t.save();
            var reopened=new TerrainLayerStack(world,0,0);
            check(reopened.layerCount()==2,"Versioned layer stack survives save/reopen");
            near(reopened.layerMask(0,16,16),0,"Mask survives save/reopen");
            near(reopened.layerMask(1,16,16),1,"Other layer untouched");
            near(reopened.height(16,16),t.height(16,16),
                "Composite preserves masked sculpt levels");
            check(world.verifySeams().isEmpty(),"Masks do not modify height source seams");

            // Craft a faithful v1 terrain-layer file without mask data to
            // prove backwards loading of existing users' authoring files.
            Path otherRoot=root.resolve("legacy");
            var legacyWorld=StitchedHeightmapWorld.create(otherRoot,
                new StitchedHeightmapWorld.Settings(32,2,0,0,-200,600,1051L,4));
            legacyWorld.generateTile(0,0);
            var legacy=new TerrainLayerStack(legacyWorld,0,0);
            Files.createDirectories(legacy.path().getParent());
            try(var out=new DataOutputStream(new BufferedOutputStream(
                    Files.newOutputStream(legacy.path())))){
                out.writeInt(0x544C5354);out.writeInt(1);
                out.writeInt(32);out.writeLong(crc(legacyWorld));
                out.writeInt(1);out.writeUTF("Legacy Sculpt");
                out.writeBoolean(true);out.writeFloat(1);
                for(int i=0;i<33*33;i++)out.writeFloat(i==16+16*33?12f:0f);
                out.writeInt(0);out.write(new byte[33*33]);out.writeInt(0);
            }
            var restored=new TerrainLayerStack(legacyWorld,0,0);
            check(restored.layers().get(0).name().equals("Legacy Sculpt"),
                "v1 layer name retained");
            near(restored.layerMask(0,16,16),1,
                "Old v1 files upgrade missing layer masks to full influence");
            check(restored.height(16,16)>baseline+11,
                "Legacy height contribution remains active");
            restored.save();
            var migrated=new TerrainLayerStack(legacyWorld,0,0);
            near(migrated.layerMask(0,16,16),1,
                "Migrated v2 format survives write and reopen");
            check(legacyWorld.verifySeams().isEmpty(),
                "Legacy migration leaves source heightmaps intact");
            System.out.println("PASS per-layer masks, history, source protection, v1 compatibility and v2 persistence");
        }finally {
            try(var paths=Files.walk(root)){
                for(Path p:paths.sorted(Comparator.reverseOrder()).toList())
                    Files.deleteIfExists(p);
            }
        }
    }
}
