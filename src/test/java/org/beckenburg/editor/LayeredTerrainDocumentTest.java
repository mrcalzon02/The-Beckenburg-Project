package org.beckenburg.editor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Real persistence and distinct cave-layer navigation acceptance. */
public final class LayeredTerrainDocumentTest {
    private static void check(boolean valid,String why){
        if(!valid)throw new AssertionError(why);
    }
    private static AdaptiveNavigationRegions.Graph build(StitchedHeightmapWorld source,
        AdaptiveTerrainTopology topology,LayeredTerrainDocument document,boolean canLadder)
            throws Exception{
        var physical=new LayeredTerrainQueries(source,topology,
            document.floors(),document.solids());
        var p=new AdaptiveNavigationRegions.Profile(
            new LayeredTerrainQueries.Agent(.15,1.65,35,.2),canLadder,false);
        var areas=new ArrayList<AdaptiveNavigationRegions.Area>();
        for(String layer:List.of("surface","bunker","sewer"))
            areas.add(new AdaptiveNavigationRegions.Area(layer,2,2,8,8));
        return AdaptiveNavigationRegions.build(physical,p,areas,
            document.links(),.5);
    }
    public static void main(String[] args)throws Exception {
        Path dir=Files.createTempDirectory("layer-authoring-");
        try{
            var source=StitchedHeightmapWorld.create(dir,
                new StitchedHeightmapWorld.Settings(16,1,0,0,0,2,781L,4));
            source.generateRegion(0,0,1,1);
            var topo=AdaptiveTerrainTopology.open(source);
            var authored=LayeredTerrainDocument.open(source);
            var bunker=new LayeredTerrainQueries.Floor("bunker",1,1,9,9,-3,0,0);
            var sewer=new LayeredTerrainQueries.Floor("sewer",1,1,9,9,-6,0,0);
            authored.addFloor(bunker);authored.addFloor(sewer);
            var stairs=new AdaptiveNavigationRegions.Transition(
                "surface","bunker",4,4,4,4,AdaptiveNavigationRegions.Connection.STAIRS);
            var ladder=new AdaptiveNavigationRegions.Transition(
                "bunker","sewer",4,4,4,4,AdaptiveNavigationRegions.Connection.LADDER);
            authored.addLink(stairs);authored.addLink(ladder);
            var physical=new LayeredTerrainQueries(source,topo,
                authored.floors(),authored.solids());
            check(physical.stack(4,4).size()==3,"Three independent authored support layers");
            check(physical.support("sewer",4,4).y()==-6,
                "Deeper floor exists beneath other geometry at same X/Z");
            var ordinary=build(source,topo,authored,false);
            check(ordinary.regions().size()==3,
                "Flat source/underground floors each produce one adaptive region");
            check(!ordinary.route("surface",4,4,"bunker",4,4).isEmpty(),
                "Authored stair links surface to bunker without implicit layer merging");
            check(ordinary.route("surface",4,4,"sewer",4,4).isEmpty(),
                "Ladder-restricted agent cannot reach sewer");
            var climber=build(source,topo,authored,true);
            check(!climber.route("surface",4,4,"sewer",4,4).isEmpty(),
                "Ladder-capable agent can use explicitly authored layer connector");
            var wall=new LayeredTerrainQueries.Solid("sewer-wall",
                5,-6,2,5.4,-3.6,8);
            authored.addSolid(wall);
            check(!new LayeredTerrainQueries(source,topo,authored.floors(),
                    authored.solids()).canTraverse("sewer",3,4,7,4,
                    new LayeredTerrainQueries.Agent(.15,1.65,35,.2)),
                "Authored solid obstructs actual layer query traversability");
            authored.save();
            var read=LayeredTerrainDocument.open(source);
            check(read.snapshot().equals(authored.snapshot()),
                "Binary save and load preserves all authored geometry and links");
            check(read.links().size()==2&&read.solids().size()==1,
                "Solid obstacles and both transition types persist");
            boolean overlapRejected=false;
            try{read.addFloor(new LayeredTerrainQueries.Floor(
                "sewer",2,2,5,5,-7,0,0));}
            catch(IllegalArgumentException expected){overlapRejected=true;}
            check(overlapRejected,"Overlapping floors on same named layer rejected");
            boolean danglingRejected=false;
            try{read.removeFloor(bunker);}
            catch(IllegalArgumentException expected){danglingRejected=true;}
            check(danglingRejected,"Cannot orphan links by deleting referenced floor");
            check(read.snapshot().equals(authored.snapshot()),
                "Rejected authoring mutations cannot corrupt existing document");
            var before=read.snapshot();
            read.removeSolid(wall);
            read.restore(before);
            check(read.snapshot().equals(before),"Snapshot restores solids for editor undo");
            check(source.verifySeams().isEmpty(),"Authoring preserves real height PNG seam integrity");
            System.out.println("PASS layered authoring: persistence, 3 floors, stairs, ladder gates, solids, no orphan links");
        }finally{
            try(var paths=Files.walk(dir)){
                for(var p:paths.sorted(Comparator.reverseOrder()).toList())
                    Files.deleteIfExists(p);
            }
        }
    }
}
