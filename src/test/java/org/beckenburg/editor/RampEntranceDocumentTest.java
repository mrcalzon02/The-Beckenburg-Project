package org.beckenburg.editor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Functional acceptance for sloped, surface-contiguous entrances. */
public final class RampEntranceDocumentTest {
    private static void check(boolean good,String why){if(!good)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        Path folder=Files.createTempDirectory("cave-ramp-");
        try{
            var world=StitchedHeightmapWorld.create(folder,
                new StitchedHeightmapWorld.Settings(16,1,0,0,-.01,.01,8041L,4));
            world.generateRegion(0,0,1,1);
            var topo=AdaptiveTerrainTopology.open(world);
            // Author an actual source opening only after the intact mouth cell.
            // This prevents an invisible ground plane over a descending ramp.
            for(int x=2;x<=9;x++)for(int z=4;z<=5;z++)
                topo.punch(new AdaptiveTerrainTopology.Node(x,z,0,0,0));
            topo.save();
            var geometry=LayeredTerrainDocument.open(world);
            geometry.addFloor(new LayeredTerrainQueries.Floor("bunker",
                8,3,13,7,-2,0,0));
            geometry.save();
            double lip=world.height(1,5);
            var ramp=new LayeredTerrainQueries.Ramp("bunker-ramp","bunker-entrance",
                "bunker",LayeredTerrainQueries.Ramp.Direction.EAST,
                1,4,9,6,lip,-2,0,0);
            var document=RampEntranceDocument.open(world);
            document.add(ramp);document.save();
            var loaded=RampEntranceDocument.open(world);
            check(loaded.snapshot().equals(document.snapshot()),
                "Ramp geometry, ID, source/destination and tangents roundtrip to disk");
            var physical=new LayeredTerrainQueries(world,topo,
                geometry.floors(),geometry.solids(),loaded.ramps());
            check(physical.layers().contains("bunker-entrance"),
                "Ramp has a distinct physical support and navigation layer");
            check(Math.abs(physical.support("bunker-entrance",1.3,5).y()
                -ramp.height(1.3,5))<1e-7,
                "Navigation and collision query sample the exact same ramp surface");
            check(ramp.height(1.0,5)==lip&&ramp.height(9.0,5)==-2,
                "Ramp profile reaches exact mouth and interior elevations");
            double step=.001;
            check(Math.abs((ramp.height(1+step,5)-ramp.height(1,5))/step)<.01,
                "Zero-grade lip is tangent-smooth, no abrupt entrance kink");
            check(Math.abs((ramp.height(9,5)-ramp.height(9-step,5))/step)<.01,
                "Ramp flattens smoothly onto bunker floor");
            for(var tri:ramp.triangles(40))for(int j=0;j<9;j+=3)
                check(Math.abs(tri[j+1]-ramp.height(tri[j],tri[j+2]))<1e-8,
                    "Preview render triangles share collision support height exactly");
            var agent=new LayeredTerrainQueries.Agent(.10,1.65,42,.24);
            var problems=RampEntranceDocument.audit(world,topo,geometry,ramp,agent);
            check(problems.isEmpty(),"Fully carved smooth ramp has no grade, gap or ceiling errors: "+problems);
            check(physical.canTraverse("bunker-entrance",1.3,5,8.7,5,agent),
                "A real agent can physically traverse the entire eased downhill slope");
            var profile=new AdaptiveNavigationRegions.Profile(agent,false,false);
            var areas=List.of(
                new AdaptiveNavigationRegions.Area("surface",.4,4.25,1.7,5.75),
                new AdaptiveNavigationRegions.Area("bunker-entrance",1,4.25,9,5.75),
                new AdaptiveNavigationRegions.Area("bunker",8,4.25,12,5.75));
            var graph=AdaptiveNavigationRegions.build(physical,profile,
                areas,loaded.connections(),.25);
            check(!graph.route("surface",.8,5,"bunker",11,5).isEmpty(),
                "Walking agent routes through ramp surface without ladder or teleport");
            check(graph.route("surface",.8,5,"bunker-entrance",8,5).stream()
                    .anyMatch(t->t.arriveVia()==AdaptiveNavigationRegions.Connection.RAMP),
                "Route preserves explicit continuous ramp connection marker");
            // A noncontiguous ramp exit must be physically rejected, not silently
            // accepted as a vertical teleport into the underground floor.
            var wrong=new LayeredTerrainQueries.Ramp("wrong","wrong-entrance",
                "bunker",LayeredTerrainQueries.Ramp.Direction.EAST,
                1,4,9,6,lip,-4,0,0);
            var mismatched=new LayeredTerrainQueries(world,topo,
                geometry.floors(),geometry.solids(),List.of(wrong));
            var badGraph=AdaptiveNavigationRegions.build(mismatched,profile,
                List.of(areas.get(0),
                    new AdaptiveNavigationRegions.Area("wrong-entrance",1,4.25,9,5.75),
                    areas.get(2)),List.of(
                    new AdaptiveNavigationRegions.Transition("wrong-entrance","bunker",
                        8.7,5,8.7,5,AdaptiveNavigationRegions.Connection.RAMP)),.25);
            check(badGraph.route("wrong-entrance",7,5,"bunker",11,5).isEmpty(),
                "Non-contiguous ramp links never become AI teleport edges");
            var uncarved=AdaptiveTerrainTopology.open(world);
            uncarved.fill(new AdaptiveTerrainTopology.Node(4,4,0,0,0));
            check(!RampEntranceDocument.audit(world,uncarved,geometry,ramp,agent).isEmpty(),
                "Audit catches filled-in terrain that would cap the cave entrance");
            var before=loaded.snapshot();
            loaded.remove(ramp);loaded.restore(before);
            check(loaded.snapshot().equals(before),"Ramp authoring undo snapshot restores profile");
            check(world.verifySeams().isEmpty(),
                "Ramp authoring does not corrupt stitched source PNG seams");
            System.out.println("PASS smooth ramp: C1 tangents, collision-triangle agreement, "+
                "real downhill traversal, physically continuous links, opening audit and persistence");
        }finally{
            try(var files=Files.walk(folder)){
                for(var p:files.sorted(Comparator.reverseOrder()).toList())
                    Files.deleteIfExists(p);
            }
        }
    }
}
