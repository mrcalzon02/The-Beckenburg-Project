package org.beckenburg.editor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

/** Functional acceptance for permanent nav trims, real flood and blocked transition gates. */
public final class AdaptiveNavMeshEditsTest {
    private static void check(boolean condition,String message){
        if(!condition)throw new AssertionError(message);
    }
    private static AdaptiveNavigationRegions.Graph graph(StitchedHeightmapWorld world,
        AdaptiveTerrainTopology topo,AdaptiveNavMeshEdits doc,
        List<AdaptiveNavigationRegions.Area> areas)throws Exception {
        var query=new LayeredTerrainQueries(world,topo,List.of(),List.of());
        var profile=new AdaptiveNavigationRegions.Profile(
            new LayeredTerrainQueries.Agent(.15,1.65,35,.25),false,false);
        return AdaptiveNavigationRegions.build(query,profile,areas,List.of(),doc,.25);
    }
    public static void main(String[] args)throws Exception {
        Path dir=Files.createTempDirectory("nav-authoring-");
        try{
            var world=StitchedHeightmapWorld.create(dir,
                new StitchedHeightmapWorld.Settings(16,1,0,0,0,2,991L,4));
            world.generateRegion(0,0,1,1);
            var topology=AdaptiveTerrainTopology.open(world);
            var edits=AdaptiveNavMeshEdits.open(world);
            var areas=List.of(
                new AdaptiveNavigationRegions.Area("surface",2,2,5,8),
                new AdaptiveNavigationRegions.Area("surface",5,2,8,8));
            var bare=graph(world,topology,edits,areas);
            check(bare.regions().size()==2,"Both flat regions initially generated");
            check(!bare.route("surface",3.5,5,"surface",6.5,5).isEmpty(),
                "Connected geometry originally has a valid route");
            edits.addSeed(new AdaptiveNavMeshEdits.Seed("surface",3.5,5));
            edits.addBarrier(new AdaptiveNavMeshEdits.Barrier("surface",5,2,5,8));
            edits.setContinuous(false);
            edits.save();
            check(edits.snapshot().equals(AdaptiveNavMeshEdits.open(world).snapshot()),
                "Seeds and blocked-door edges survive real atomic disk save/readback");
            check(bare.stale(),"Authored mask changes invalidate old paths");
            var pruned=graph(world,topology,AdaptiveNavMeshEdits.open(world),areas);
            check(pruned.regions().size()==1,
                "Flood from inside seed stops at explicitly authored level-transition gate");
            check(pruned.candidates().regions().size()==2
                    &&pruned.candidates().connections(0).isEmpty(),
                "Both sides remain selectable in editor, but blocked gateway has no traversable link");
            check(pruned.route("surface",3.5,5,"surface",6.5,5).isEmpty(),
                "Open geometry past transition door is not navigable");
            var revision=edits.revision();
            var prior=edits.snapshot();
            edits.removeBarrier(edits.barriers().get(0));
            check(edits.revision()>revision,"Gate edits invalidate graph generation");
            var regrown=graph(world,topology,edits,areas);
            check(regrown.regions().size()==2,
                "Removing blocked gate deliberately restores physically reachable regions");
            edits.addExclusion(new AdaptiveNavMeshEdits.Exclusion("surface",5,2,8,8));
            var trimmed=graph(world,topology,edits,areas);
            check(trimmed.regions().size()==1,
                "Permanent authored trim prevents flood from regrowing forbidden section");
            edits.save();
            var reload=AdaptiveNavMeshEdits.open(world);
            check(graph(world,topology,reload,areas).regions().size()==1,
                "Trim remains enforced after save/reload and a new flood pulse");
            reload.restore(prior);
            check(graph(world,topology,reload,areas).regions().size()==1,
                "Undo snapshot restores the gate and removes subsequent trim");
            reload.setContinuous(true);
            reload.save();
            check(AdaptiveNavMeshEdits.open(world).continuous(),
                "Continuous/manual mode survives editor restart");
            check(world.verifySeams().isEmpty(),"Navigation edits never modify heightmap seams");
            check(Files.isRegularFile(world.tilePath(0,0)),
                "Original 16-bit heightmap remains intact");

            // Exclusions across a physical surface must cause local refinement;
            // entire coarse regions must never survive with trimmed interiors.
            var coarse=List.of(new AdaptiveNavigationRegions.Area("surface",2,2,8,8));
            var cut=new AdaptiveNavMeshEdits.Exclusion("surface",4,4,6,6);
            reload.addExclusion(cut);
            var refined=graph(world,topology,reload,coarse);
            check(refined.regions().size()>1,
                "Sub-area trim refines coarse nav graph instead of ignoring exclusion");
            for(var region:refined.regions())
                check(!cut.overlaps(region.x0(),region.z0(),region.x1(),region.z1()),
                    "No retained region overlaps forbidden authored exclusion");
            System.out.println("PASS nav masks: manual/continuous persistence, seeded flood, "+
                "blocked transition, regrowth prevention, undo, coarse-area trim");
        } finally {
            try(var files=Files.walk(dir))
                {for(var p:files.sorted(Comparator.reverseOrder()).toList())
                    Files.deleteIfExists(p);}
        }
    }
}
