package org.beckenburg.editor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

/** Three entrance topology methods from the editor's authored diagram. */
public final class EntranceSeamDocumentTest {
    private static void check(boolean ok,String reason){
        if(!ok)throw new AssertionError(reason);
    }
    public static void main(String[] args)throws Exception{
        Path dir=Files.createTempDirectory("three-entrance-methods-");
        try{
            var world=StitchedHeightmapWorld.create(dir,
                new StitchedHeightmapWorld.Settings(16,1,0,0,-.01,.01,911L,4));
            world.generateRegion(0,0,2,1);
            var topology=AdaptiveTerrainTopology.open(world);
            var sourceBefore=world.height(5,5);
            var author=EntranceSeamDocument.open(world);
            var method1=new EntranceSeamDocument.EdgeCut("extra-cut-1",
                EntranceSeamDocument.Method.EXTRA_SEAM,"bunker",4.5,4,4.5,8);
            var method2=new EntranceSeamDocument.EdgeCut("tile-unweld-1",
                EntranceSeamDocument.Method.TILE_DESTITCH,"bunker",16,4,16,8);
            author.addEdge(method1);
            author.addEdge(method2);
            boolean missingSeam=false;
            try{author.addTab(new EntranceSeamDocument.DetachedTab(
                "wrong-tab","wrong-layer","wrong-destination",4.5,4,6,6));}
            catch(IllegalArgumentException expected){missingSeam=true;}
            check(missingSeam,"Method 3 must be anchored to a matching Method 1 cut");
            var method3=new EntranceSeamDocument.DetachedTab(
                "independent-tab","cave-tab","bunker",4.5,4,6,6);
            author.addTab(method3);
            author.save();
            var saved=EntranceSeamDocument.open(world);
            check(author.snapshot().equals(saved.snapshot()),
                "All three seam operations must survive binary readback");
            var applied=EntranceSeamTopology.apply(world,topology,saved);
            check(applied.newSplits()>0,"Method 1 must cause real adaptive leaf subdivision");
            check(applied.newCutouts()>0,"Method 3 must punch physical surface leaves");
            check(AdaptiveTerrainTopology.open(world).snapshot().equals(topology.snapshot()),
                "Conformed leaves/punches must survive reload");
            // A leaf segment at x=4.5 must now coincide with actual leaf
            // boundaries, not cross the middle of an unsplit source leaf.
            boolean conform=true;
            for(var leaf:topology.leaves(4,5)){
                double x0=leaf.x0()/64.0,x1=leaf.x1()/64.0;
                if(x0+1e-7<4.5&&x1-1e-7>4.5)conform=false;
            }
            check(conform,"New seam at x=4.5 is a genuine conformal terrain edge");
            var queries=new LayeredTerrainQueries(world,topology,
                List.of(),List.of(),List.of(),saved);
            var agent=new LayeredTerrainQueries.Agent(.10,1.65,42,.24);
            check(queries.support("surface",5,5)==null,
                "Punched Method 3 patch is absent from surrounding surface");
            check(Math.abs(queries.support("cave-tab",5,5).y()-sourceBefore)<1e-7,
                "Detached tab retains identical source height without losing terrain");
            check(queries.canStand("cave-tab",5,5,agent),
                "Detached patch is a physical walkable support, not only a nav marker");
            check(!queries.canTraverse("surface",3,5,7,5,agent),
                "Method 1 surface edge must not reconnect across the authored seam");
            check(!queries.canTraverse("surface",15,5,17,5,agent),
                "Method 2 must disconnect the original stitched tile border");
            check(queries.canStand("surface",15,5,agent)
                &&queries.canStand("surface",17,5,agent),
                "Destitching only changes edge adjacency, not either tile's terrain");
            var graph=AdaptiveNavigationRegions.build(queries,
                new AdaptiveNavigationRegions.Profile(agent,false,false),
                List.of(new AdaptiveNavigationRegions.Area("surface",14,4.5,18,7.5)),
                List.of(),.25);
            check(graph.route("surface",15.5,5,"surface",16.5,5).isEmpty(),
                "Adaptive region graph cannot route AI over a de-stitched tile border");
            check(queries.layers().contains("cave-tab"),
                "Detached tab has independent named navigation-layer identity");
            var ramp=new LayeredTerrainQueries.Ramp("ramp-1","ramp-layer","bunker",
                LayeredTerrainQueries.Ramp.Direction.EAST,4.7,4,12.7,6,
                world.height(5,5),-2,0,0);
            var ramps=RampEntranceDocument.open(world);
            ramps.add(ramp);
            var connections=saved.rampConnections(ramps);
            check(connections.size()==2
                &&connections.get(0).from().equals("cave-tab")
                &&connections.get(0).to().equals("ramp-layer"),
                "Ramp mouth stitches to detached tab, not the outer surface");
            check(Math.abs(world.height(5,5)-sourceBefore)<1e-9
                &&world.verifySeams().isEmpty(),
                "Adaptive seam work must never corrupt source 16-bit heightmap");
            var prior=saved.snapshot();
            saved.removeTab(method3);
            saved.restore(prior);
            check(saved.snapshot().equals(prior),"Authoring undo restores detached tab");
            var again=EntranceSeamTopology.apply(world,topology,saved);
            check(again.newSplits()==0&&again.newCutouts()==0,
                "Reapplying a conformed entrance is idempotent");
            System.out.println("PASS three entrance methods: actual seam splits, tile "+
                "destitch, detached punched tab, AI blocking, ramp tab link and readback");
        }finally{
            try(var paths=Files.walk(dir)){
                for(var p:paths.sorted(Comparator.reverseOrder()).toList())
                    Files.deleteIfExists(p);
            }
        }
    }
}
