package org.beckenburg.editor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

/** Real source PNG, cutout and multilevel collision-query acceptance. */
public final class LayeredTerrainQueriesTest {
    private static void check(boolean actual,String why){
        if(!actual)throw new AssertionError(why);
    }
    public static void main(String[] args)throws Exception{
        Path temp=Files.createTempDirectory("layered-queries-");
        try{
            var world=StitchedHeightmapWorld.create(temp,
                new StitchedHeightmapWorld.Settings(16,1,0,0,0,2,123L,3));
            world.generateRegion(0,0,2,1);
            var topo=AdaptiveTerrainTopology.open(world);
            var agent=new LayeredTerrainQueries.Agent(.15,1.65,35,.25);
            var surface=new LayeredTerrainQueries(world,topo,List.of(),List.of());
            check(surface.canStand("surface",13.5,8.5,agent),
                "Unmodified stitched PNG gives ground support");
            check(surface.canTraverse("surface",15.3,9.5,16.7,9.5,agent),
                "Walking across stitched tile seam succeeds");
            topo.punch(new AdaptiveTerrainTopology.Node(15,9,0,0,0));
            check(surface.support("surface",15.5,9.5)==null,
                "A punched cell has no surface collision, not a zero-height floor");
            check(!surface.canTraverse("surface",14.5,9.5,16.5,9.5,agent),
                "No movement shortcut can bridge the punched hole");

            // Independent layers do not snap to the upper terrain at same X/Z.
            var lower=new LayeredTerrainQueries.Floor("sewer",12,7,20,12,-4,0,0);
            var upper=new LayeredTerrainQueries.Floor("bunker",12,7,20,12,-1,0,0);
            var layered=new LayeredTerrainQueries(world,topo,List.of(lower,upper),List.of());
            check(layered.support("sewer",15.5,9.5).y()==-4,
                "Sewer query retains explicit lower floor despite terrain hole");
            check(layered.support("bunker",15.5,9.5).y()==-1,
                "Bunker query retains separate upper underground floor");
            check(layered.stack(15.5,9.5).size()==2,
                "Cutout removes surface only, never lower stacked layers");
            check(layered.canStand("sewer",15.5,9.5,agent),
                "Sewer is independently walkable under other layers");
            var tall=new LayeredTerrainQueries.Agent(.15,3.25,35,.25);
            check(layered.canStand("bunker",15.5,9.5,tall),
                "A real cutout removes the overhead source-terrain ceiling");
            check(!layered.canStand("bunker",14.5,9.5,tall),
                "Intact terrain above bunker constrains headroom");
            var wall=new LayeredTerrainQueries.Solid("sewer-wall",
                17,-4,8,17.35,-1.6,11);
            var blocked=new LayeredTerrainQueries(world,topo,List.of(lower,upper),
                List.of(wall));
            check(!blocked.canTraverse("sewer",15,9.5,19,9.5,agent),
                "Real solid wall blocks a sewer path");
            check(blocked.canTraverse("sewer",14,7.5,16,7.5,agent),
                "Different unobstructed sewer corridor remains open");
            topo.fill(new AdaptiveTerrainTopology.Node(15,9,0,0,0));
            check(layered.stack(15.5,9.5).size()==3,
                "Filling hole restores upper layer without changing cave floors");
            check(layered.topologyRevision()>1,
                "Topology edits expose revision for incremental nav invalidation");
            check(world.verifySeams().isEmpty(),"Source PNG seams preserved");
            System.out.println("PASS physical-layer queries: holes, stitched seams, stacked floors, collision, headroom");
        }finally{
            try(var paths=Files.walk(temp)){
                for(var p:paths.sorted(Comparator.reverseOrder()).toList())
                    Files.deleteIfExists(p);
            }
        }
    }
}
