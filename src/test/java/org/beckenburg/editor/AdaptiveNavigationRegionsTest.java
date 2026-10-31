package org.beckenburg.editor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

/** Diagnostic region graph; no fake HouseMap movement success. */
public final class AdaptiveNavigationRegionsTest {
    private static void check(boolean ok,String why) {
        if(!ok)throw new AssertionError(why);
    }
    private static AdaptiveNavigationRegions.Graph graph(LayeredTerrainQueries query,
                AdaptiveNavigationRegions.Profile p,
                List<AdaptiveNavigationRegions.Area> regions,
                List<AdaptiveNavigationRegions.Transition> transitions)throws Exception {
        return AdaptiveNavigationRegions.build(query,p,regions,transitions,.25);
    }
    public static void main(String[] args)throws Exception {
        Path tmp=Files.createTempDirectory("adaptive-navigation-");
        try {
            var source=StitchedHeightmapWorld.create(tmp,
                new StitchedHeightmapWorld.Settings(16,1,0,0,0,2,999L,4));
            source.generateRegion(0,0,2,1);
            var topo=AdaptiveTerrainTopology.open(source);
            var hunter=new AdaptiveNavigationRegions.Profile(
                new LayeredTerrainQueries.Agent(.15,1.65,35,.22),false,false);
            var normal=new LayeredTerrainQueries(source,topo,List.of(),List.of());
            var broad=List.of(new AdaptiveNavigationRegions.Area("surface",2,2,10,10));
            var simple=graph(normal,hunter,broad,List.of());
            check(simple.regions().size()==1,
                "A broad physically simple region uses ONE nav node, not display triangles");

            for(int ix=3;ix<9;ix++){
                var root=new AdaptiveTerrainTopology.Node(ix,3,0,0,0);
                topo.split(root);
            }
            var detailed=graph(normal,hunter,broad,List.of());
            check(detailed.regions().size()==1,
                "High source topology density does not inflate a flat navigation graph");
            topo.punch(new AdaptiveTerrainTopology.Node(6,6,0,0,0));
            var around=graph(normal,hunter,broad,List.of());
            check(around.regions().size()>1,
                "A fine physical hole forces local refinement of coarse nav region");
            var aroundHole=around.route("surface",3,6.5,"surface",9,6.5);
            check(aroundHole.size()>2,
                "AI can route around a real hole without crossing its missing surface");
            for(int i=1;i<aroundHole.size();i++){
                var previous=aroundHole.get(i-1);
                var next=aroundHole.get(i);
                check(normal.canTraverse("surface",previous.x(),previous.z(),
                        next.x(),next.z(),hunter.footprint()),
                    "Every emitted same-layer waypoint segment must physically avoid the hole");
            }
            var previous=around.metrics().topologyRevision();
            topo.fill(new AdaptiveTerrainTopology.Node(6,6,0,0,0));
            check(around.stale()&&previous!=topo.revision(),
                "Editing surface invalidates previously generated navigation graph");
            boolean staleRejected=false;
            try{around.route("surface",3,6.5,"surface",9,6.5);}
            catch(IllegalStateException expected){staleRejected=true;}
            check(staleRejected,"Stale routes are never treated as validated");

            // Stacked floors at identical X/Z remain disconnected until an
            // explicitly authored traversal exists (no falling through stone).
            var sewer=new LayeredTerrainQueries.Floor("sewer",2,2,10,10,-4,0,0);
            var bunker=new LayeredTerrainQueries.Floor("bunker",2,2,10,10,-1,0,0);
            var physical=new LayeredTerrainQueries(source,topo,List.of(sewer,bunker),
                List.of());
            var areas=List.of(new AdaptiveNavigationRegions.Area("surface",2,2,10,10),
                new AdaptiveNavigationRegions.Area("bunker",2,2,10,10),
                new AdaptiveNavigationRegions.Area("sewer",2,2,10,10));
            var disconnected=graph(physical,hunter,areas,List.of());
            check(disconnected.regions().size()==3,
                "Three stacked flat surfaces create three independent regions");
            check(disconnected.route("surface",4,4,"sewer",4,4).isEmpty(),
                "Same X/Z is NEVER permission to move through two underground floors");
            var stairs=new AdaptiveNavigationRegions.Transition("surface","bunker",
                4,4,4,4,AdaptiveNavigationRegions.Connection.STAIRS);
            var ladder=new AdaptiveNavigationRegions.Transition("bunker","sewer",
                4,4,4,4,AdaptiveNavigationRegions.Connection.LADDER);
            var restricted=graph(physical,hunter,areas,List.of(stairs,ladder));
            check(restricted.route("surface",4,4,"bunker",4,4).size()>2,
                "Explicit authored stair joins upper and bunker regions");
            check(restricted.route("surface",4,4,"sewer",4,4).isEmpty(),
                "Hunter lacking ladder ability cannot reach deeper sewer");
            var climber=new AdaptiveNavigationRegions.Profile(hunter.footprint(),true,false);
            var permitted=graph(physical,climber,areas,List.of(stairs,ladder));
            check(permitted.route("surface",4,4,"sewer",4,4).size()>3,
                "Ability-gated vertical connector creates authentic multi-layer route");

            var barrier=new LayeredTerrainQueries.Solid("wall",5,0,2,5.5,3,10);
            var obstructed=new LayeredTerrainQueries(source,topo,List.of(),List.of(barrier));
            var blocked=graph(obstructed,hunter,broad,List.of());
            check(blocked.route("surface",3.5,6,"surface",8.5,6).isEmpty(),
                "Full-height solid wall prevents a phantom route across region centres");
            // A single steep source grid vertex lies between broad nav probes.
            // Even without visual refinement, exact source triangles must force
            // local navigation refinement instead of authorizing a flat shortcut.
            var png=javax.imageio.ImageIO.read(source.tilePath(0,0).toFile());
            png.getRaster().setSample(6,6,0,65535);
            Path altered=tmp.resolve("steep-source.png");
            check(javax.imageio.ImageIO.write(png,"png",altered.toFile()),
                "16-bit test PNG encoder available");
            source.importTile(0,0,altered,true);
            var coarseBounds=new LayeredTerrainQueries.Rect(2,2,10,10);
            check(normal.steepSourceWithin(coarseBounds,hunter.footprint()),
                "Steep source vertex is detected even when broad probes might miss it");
            var steep=graph(normal,hunter,broad,List.of());
            check(steep.regions().size()>1,
                "Coarsely rendered heightfield with a narrow ridge gains adaptive navigation detail");
            check(source.verifySeams().isEmpty(),
                "Navigation never modifies original stitched height data");
            System.out.println("PASS adaptive nav: sparse regions, cutout refinement, locked layers, ability links, solid obstacles");
        } finally {
            try(var paths=Files.walk(tmp)){
                for(var p:paths.sorted(Comparator.reverseOrder()).toList())
                    Files.deleteIfExists(p);
            }
        }
    }
}
