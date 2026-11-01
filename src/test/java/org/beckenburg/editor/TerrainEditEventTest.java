package org.beckenburg.editor;

import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/** Verifies portable bounded change notifications and stroke completion timing. */
public final class TerrainEditEventTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        Path root=Files.createTempDirectory("terrain-change-events");
        try{
            var world=StitchedHeightmapWorld.create(root.resolve("map"),
                new StitchedHeightmapWorld.Settings(32,2,0,0,-100,400,8723L,4));
            world.generateTile(0,0);
            var terrain=new TerrainLayerStack(world,0,0);
            List<TerrainLayerStack.EditEvent> events=new ArrayList<>();
            terrain.addChangeListener(events::add);
            terrain.brush(TerrainLayerStack.Brush.RAISE,10,10,2,4,0,0,1);
            terrain.brush(TerrainLayerStack.Brush.RAISE,11,10,2,4,0,0,1);
            check(events.isEmpty(),"No costly mid-stroke geometry rebuild notification");
            terrain.finishStroke();
            check(events.size()==1,"Two brush dabs emit one transaction event");
            var stroke=events.get(0);
            check(stroke.domains().equals(
                java.util.Set.of(TerrainLayerStack.EditDomain.HEIGHT)),
                "Sculpt advertises height dependency only");
            check(stroke.x0()<=10&&stroke.x1()>=11
                &&stroke.z0()<=10&&stroke.z1()>=10,
                "Height dirty bounds cover actual brush extent");
            check(stroke.x0()>0&&stroke.x1()<32,
                "Localized stroke does not dirty full terrain tile");

            terrain.paintMaterial(10,10,1.5,TerrainLayerStack.Material.MUD,90);
            terrain.paintMaterial(12,10,1.5,TerrainLayerStack.Material.MUD,90);
            check(events.size()==1,"No mid-material-drag notifications");
            terrain.finishStroke();
            check(events.size()==2&&events.get(1).domains().contains(
                TerrainLayerStack.EditDomain.MATERIAL),
                "Painted materials emit a single material region event");
            terrain.freezeRect(10,10,12,12,true);
            check(events.size()==3&&events.get(2).domains().contains(
                TerrainLayerStack.EditDomain.PROTECTION),
                "Freeze operation emits protection region event");
            long latest=terrain.editRevision();
            check(latest==3&&events.get(2).revision()==3,
                "Edit revision is monotonically updated");
            terrain.undo();
            check(events.size()==4&&events.get(3).revision()==4,
                "Undo also signals dependents");
            check(events.get(3).domains().equals(
                java.util.Set.of(TerrainLayerStack.EditDomain.PROTECTION)),
                "Undo retains affected subsystem classification");
            terrain.addLayer("Extra");
            check(events.size()==5&&events.get(4).domains().contains(
                TerrainLayerStack.EditDomain.LAYER_CONFIGURATION),
                "New height layer invalidates dependent terrain");
            terrain.configureLayer(1,false,.5);
            check(events.size()==6,"Layer settings invalidate dependent terrain");
            terrain.save();
            check(events.size()==6,"Saving unchanged geometry does not cause rebuild");
            check(world.verifySeams().isEmpty(),"Notifications cannot mutate source");
            System.out.println("PASS edit-region events, per-stroke batching, undo domains and revision tracking");
        }finally{
            try(var paths=Files.walk(root)){
                for(Path path:paths.sorted(java.util.Comparator.reverseOrder()).toList())
                    Files.deleteIfExists(path);
            }
        }
    }
}
