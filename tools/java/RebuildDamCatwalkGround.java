import org.beckenburg.LevelDefinition.Vec;
import org.beckenburg.editor.AdaptiveTerrainTopology;
import org.beckenburg.editor.StitchedHeightmapWorld;
import org.beckenburg.editor.TerrainEntranceMesh;
import org.beckenburg.TerrainSceneNativePhysics;
import org.beckenburg.editor.TerrainScenePackage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

/** Restores obsolete exterior excavation outside the retained Dam foundations. */
public final class RebuildDamCatwalkGround {
    private static final List<TerrainEntranceMesh.Bounds> RETIRED = List.of(
        new TerrainEntranceMesh.Bounds(72.75,27,75.25,43),
        new TerrainEntranceMesh.Bounds(70.5,34,71,35),
        new TerrainEntranceMesh.Bounds(70.5,35,71.25,48));
    private static final String RETIRED_ROW = "72.75\t27.0\t75.25\t43.0\tstair-L18";
    private static final List<String> SAVED_INPUTS = List.of("world.properties","tile_0_0.png",
        "terrain-layers/tile_0_0.bin","adaptive-topology.bin","scene-package/scene.bin");
    private record RestoredLeaf(Vec probe,double area) {}
    private record Chunk(String name,TerrainEntranceMesh.Bounds bounds) {}

    private RebuildDamCatwalkGround() {}

    public static void main(String[] args) throws Exception {
        Path source=Path.of(args[0]);
        Path target=Path.of(args[1]);
        require(!Files.exists(target),"Use a fresh saved-world destination");
        for(String name:SAVED_INPUTS){
            var output=target.resolve(name);
            Files.createDirectories(output.getParent());
            Files.copy(source.resolve(name),output);
        }

        var authored=new ArrayList<>(Files.readAllLines(source.resolve("authored-cuts.tsv")));
        require(authored.remove(RETIRED_ROW)&&!authored.contains(RETIRED_ROW),"Expected exactly one retired cut");
        String previousGalleryCut="68.75\t35.0\t71.25\t55.0\tstair-L12";
        int gallery=authored.indexOf(previousGalleryCut);
        require(gallery>=0&&gallery==authored.lastIndexOf(previousGalleryCut),"Expected existing east hall and gallery cut");
        authored.remove(gallery);
        authored.add(gallery,"68.75\t35.0\t70.5\t48.0\teast-hall-retained-basement");
        authored.add(gallery+1,"68.75\t48.0\t71.25\t55.0\tgallery-retained-basement");
        Files.writeString(target.resolve("authored-cuts.tsv"),String.join("\n",authored)+"\n");

        var original=StitchedHeightmapWorld.open(source);
        var oldScene=new TerrainScenePackage(original).openScene();
        var oldPhysics=TerrainSceneNativePhysics.load(original,oldScene).physics();
        var world=StitchedHeightmapWorld.open(target);
        var topology=AdaptiveTerrainTopology.open(world);
        var before=topology.snapshot();
        var filled=new ArrayList<RestoredLeaf>();
        for(var leaf:before.cutouts())restore(topology,leaf,filled);
        double area=filled.stream().mapToDouble(RestoredLeaf::area).sum();
        require(Math.abs(area-50.25)<1e-9,"Expected to restore the complete exterior cut footprints");
        topology.save();

        var disk=StitchedHeightmapWorld.open(target);
        var saved=AdaptiveTerrainTopology.open(disk);
        require(saved.snapshot().equals(topology.snapshot()),"Reopened topology differs from published topology");
        require(new HashSet<>(saved.splitNodes()).containsAll(before.splits()),"Prior terrain splits disappeared");
        var scene=new TerrainScenePackage(disk).openScene();
        var chunks=new ArrayList<Chunk>();
        for(var node:scene.nodes())for(var component:node.components()){
            if(!component.type().equals(TerrainScenePackage.COMPONENT))continue;
            var p=component.properties();
            chunks.add(new Chunk(p.get("chunk"),new TerrainEntranceMesh.Bounds(
                Double.parseDouble(p.get("boundsX0")),Double.parseDouble(p.get("boundsZ0")),
                Double.parseDouble(p.get("boundsX1")),Double.parseDouble(p.get("boundsZ1")))));
        }
        require(chunks.size()==4,"Expected all four Dam terrain chunks");
        for(var chunk:chunks)TerrainScenePackage.publish(disk,scene,chunk.name(),chunk.bounds());
        var published=new TerrainScenePackage(disk).openScene();
        require(TerrainScenePackage.inspect(disk,published).isEmpty(),"Reopened scene has stale terrain");
        var physics=TerrainSceneNativePhysics.load(disk,published).physics();
        Files.write(target.resolve("terrain-physics.hcs2"),TerrainSceneNativePhysics.encode(disk,published));

        for(var leaf:filled){
            var p=leaf.probe();
            require(oldPhysics.clearSight(new Vec(p.x(),20,p.z()),new Vec(p.x(),-20,p.z())),
                "Retired cut had terrain before restoration");
            require(!physics.clearSight(new Vec(p.x(),20,p.z()),new Vec(p.x(),-20,p.z())),
                "Restored catwalk ground missed a native surface probe");
        }
        for(String line:authored){
            var col=line.split("\t");
            if(col.length<4)continue;
            double x=(Double.parseDouble(col[0])+Double.parseDouble(col[2]))/2;
            double z=(Double.parseDouble(col[1])+Double.parseDouble(col[3]))/2;
            require(physics.clearSight(new Vec(x,20,z),new Vec(x,-20,z)),"Remaining authored cut filled "+line);
        }
        for(String name:List.of("tile_0_0.png","terrain-layers/tile_0_0.bin"))
            require(Files.mismatch(source.resolve(name),target.resolve(name))==-1,"Saved grade changed "+name);

        Files.writeString(target.resolve("restoration.json"),String.format(Locale.ROOT,
            """
            {
              "area_m2": %.2f,
              "restored_leaf_probes": %d,
              "retained_cut_leaves": %d,
              "retained_authored_cut_controls": %d,
              "published_chunks": %d,
              "reopened_native_surface": true,
              "source_heights_unchanged": true
            }
            """,area,filled.size(),saved.cutoutCount(),authored.size(),chunks.size()));
        System.out.printf("PASS restored %.2f mÂ² at %d native controls; retained %d cut leaves%n",
            area,filled.size(),saved.cutoutCount());
    }

    private static void restore(AdaptiveTerrainTopology topology,AdaptiveTerrainTopology.Node leaf,
                                List<RestoredLeaf> filled){
        var settings=topology.world().settings();
        double unit=settings.metresPerCell()/AdaptiveTerrainTopology.UNITS_PER_CELL;
        double x0=settings.originX()+leaf.x0()*unit;
        double x1=settings.originX()+leaf.x1()*unit;
        double z0=settings.originZ()+leaf.z0()*unit;
        double z1=settings.originZ()+leaf.z1()*unit;
        for(var bounds:RETIRED){
            if(x1<=bounds.x0()||x0>=bounds.x1()||z1<=bounds.z0()||z0>=bounds.z1())continue;
            if(x0>=bounds.x0()&&x1<=bounds.x1()&&z0>=bounds.z0()&&z1<=bounds.z1()){
                topology.fill(leaf);
                filled.add(new RestoredLeaf(new Vec((x0+x1)/2,0,(z0+z1)/2),(x1-x0)*(z1-z0)));
                return;
            }
            topology.split(leaf);
            for(var child:topology.leaves(leaf.cellX(),leaf.cellZ())){
                if(child.depth()==leaf.depth()+1&&child.parent().equals(leaf))
                    restore(topology,child,filled);
            }
            return;
        }
    }

    private static void require(boolean condition,String reason){
        if(!condition)throw new IllegalStateException(reason);
    }
}
