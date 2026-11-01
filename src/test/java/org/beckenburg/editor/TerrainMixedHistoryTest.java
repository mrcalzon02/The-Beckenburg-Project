package org.beckenburg.editor;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Regression: terrain history must include material and protection changes,
 * not just height deltas. Standalone Java 17, no game or renderer dependencies.
 */
public final class TerrainMixedHistoryTest {
    private static void check(boolean condition,String why){
        if(!condition)throw new AssertionError(why);
    }
    public static void main(String[] args)throws Exception {
        Path root=Files.createTempDirectory("terrain-mixed-undo");
        try {
            var world=StitchedHeightmapWorld.create(root.resolve("map"),
                new StitchedHeightmapWorld.Settings(32,2,0,0,-150,500,8249L,4));
            world.generateRegion(0,0,2,1);
            var t=new TerrainLayerStack(world,0,0);
            int source=world.sample(0,0,12,12);
            check(t.material(12,12)==TerrainLayerStack.Material.GRASS.ordinal(),
                "Default surface material must be known");
            t.paintMaterial(12,12,1.2,TerrainLayerStack.Material.MUD,90);
            t.paintMaterial(17,12,1.2,TerrainLayerStack.Material.ROCK,90);
            t.finishStroke();
            check(t.material(12,12)==TerrainLayerStack.Material.MUD.ordinal(),
                "First part of painted drag applies");
            check(t.material(17,12)==TerrainLayerStack.Material.ROCK.ordinal(),
                "Second part of painted drag applies");
            check(t.undo(),"One undo covers the entire painted drag");
            check(t.material(12,12)==0&&t.material(17,12)==0,
                "Material paint undo restores both independent dabs");
            check(t.redo(),"One redo restores painted drag");
            check(t.material(12,12)==TerrainLayerStack.Material.MUD.ordinal()
                &&t.material(17,12)==TerrainLayerStack.Material.ROCK.ordinal(),
                "Redo restores material IDs");

            t.freezeRect(10,10,14,14,true);
            check(t.frozen(12,12),"Freeze rectangle works");
            check(t.undo()&&!t.frozen(12,12),"Freeze is independently undoable");
            check(t.redo()&&t.frozen(12,12),"Freeze redo works");
            t.freezeRect(10,10,14,14,false);
            check(!t.frozen(12,12),"Unfreeze works");
            check(t.undo()&&t.frozen(12,12),"Unfreeze is undoable");
            check(t.redo()&&!t.frozen(12,12),"Unfreeze redo works");

            t.lockMaterial(TerrainLayerStack.Material.MUD,true);
            check(t.frozen(12,12)&&!t.frozen(17,12),
                "Material-specific protection only locks matching vertices");
            check(t.undo()&&!t.frozen(12,12),
                "Material protection lock is undoable");
            check(t.redo()&&t.frozen(12,12),
                "Material protection redo works");
            t.lockMaterial(TerrainLayerStack.Material.MUD,false);
            check(!t.frozen(12,12),"Material protection unlock works");

            t.addLayer("Independent noise");
            t.configureLayer(1,false,.25);
            check(!t.layers().get(1).enabled()&&t.layers().get(1).opacity()==.25,
                "Layer visibility and opacity changed");
            check(t.undo()&&t.layers().get(1).enabled()
                &&t.layers().get(1).opacity()==1.0,
                "Layer configuration has sparse undo history");
            check(t.redo()&&!t.layers().get(1).enabled()
                &&t.layers().get(1).opacity()==.25,
                "Layer configuration has redo history");
            t.configureLayer(1,true,1);
            t.selectLayer(0);
            double before=t.height(12,12);
            t.brush(TerrainLayerStack.Brush.RAISE,12,12,2,4,0,.2,1);
            t.finishStroke();
            check(t.height(12,12)>before,"Sculpt after mixed edit history works");
            check(t.undo()&&Math.abs(t.height(12,12)-before)<.0005,
                "Height history still works following material and freeze edits");
            check(t.redo()&&t.height(12,12)>before,
                "Height redo still works following mixed edits");
            check(world.sample(0,0,12,12)==source,
                "All authoring history remains non-destructive");
            check(world.verifySeams().isEmpty(),
                "All history operations preserve source seam vertices");

            t.save();
            var reopened=new TerrainLayerStack(world,0,0);
            check(reopened.material(12,12)==TerrainLayerStack.Material.MUD.ordinal()
                &&reopened.material(17,12)==TerrainLayerStack.Material.ROCK.ordinal(),
                "Saved surface state survives reopening");
            check(!reopened.frozen(12,12),
                "Saved protection state survives reopening");
            System.out.println("PASS combined height, material, freeze and layer settings undo/redo; source unchanged");
        }finally{
            try(var stream=Files.walk(root)){
                for(Path p:stream.sorted(java.util.Comparator.reverseOrder()).toList())
                    Files.deleteIfExists(p);
            }
        }
    }
}
