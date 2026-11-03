package org.beckenburg;

import org.beckenburg.editor.*;
import org.beckenburg.LevelDefinition.Vec;
import java.nio.file.*;
import java.util.*;

/** Saved editor hole, independent filled-world negative and actual Castle renderer parity. */
public final class CastleWorldTerrainTest {
    private static void require(boolean v,String message){if(!v)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        System.setProperty("user.home",Files.createTempDirectory("castle-terrain-profile-").toString());
        var world=StitchedHeightmapWorld.open(Path.of("development/levels/rebuild/castle/terrain-world"));
        var topo=AdaptiveTerrainTopology.open(world);
        require(topo.cutoutCount()==120,"Saved Castle hole missing");
        byte[] source=Files.readAllBytes(world.tilePath(0,0));
        var result=TerrainFootprintCutout.punch(topo,new TerrainEntranceMesh.Bounds(28.5,17.5,43,34));
        require(result.newSplits()==0&&result.newCutouts()==0,"Hole authoring not idempotent");
        require(Arrays.equals(source,Files.readAllBytes(world.tilePath(0,0))),"Punch modified source elevation");
        var scene=new TerrainScenePackage(world).openScene();
        require(TerrainScenePackage.inspect(world,scene).isEmpty(),"Saved editor package stale");
        var exported=TerrainSceneNativePhysics.load(world,scene);
        var map=new CastleHubMap();var terrain=map.worldSurface();
        var project=new CastleEditorProject(CastleLevelEdits.current());
        var agent=new CastleEditorAgent(project,map,new SceneRenderer(),()->{});
        var service=new EditorAgentService<>(agent,new EditorHistory<>(project,60));
        require(service.test(agent.revision(),"castle-world-terrain").result().stream().allMatch(EditorAgentService.Check::passed),"Actual editor service terrain check failed");
        require(exported.sourceSha256().equals(terrain.sha256()),"Packaged Castle terrain differs from editor");
        for(double x=28.75;x<43;x+=.5)for(double z=17.75;z<34;z+=.5){
            require(terrain.clearSight(new Vec(x,.1,z),new Vec(x,-1,z)),"World face caps cellar at "+x+","+z);
            require(terrain.support(x,z,0,4)==null,"Invisible support caps cellar");
            require(!terrain.bodyBlocked(new Vec(x,-1,z),1.8,.1),"Body cannot pass punched terrain");
        }
        require(!terrain.clearSight(new Vec(25,.1,20),new Vec(25,-1,20)),"Uncut world floor disappeared");
        require(terrain.support(25,20,0,4)!=null,"Uncut terrain lost support");
        require(terrain.bodyBlocked(new Vec(25,-1,20),1.8,.21),"Uncut terrain lost body collision");
        // Actual editor API builds the negative control; no synthetic blocking rectangle.
        Path control=Files.createTempDirectory("castle-filled-world-");
        var filled=StitchedHeightmapWorld.create(control,world.settings());
        filled.importTile(0,0,world.tilePath(0,0),false);
        var filledScene=new TerrainScenePackage(filled).openScene();
        TerrainScenePackage.publish(filled,filledScene,"castle",new TerrainEntranceMesh.Bounds(-24,-8,48,40));
        var negative=TerrainSceneNativePhysics.load(filled,filledScene).physics();
        require(!negative.clearSight(new Vec(40,.1,22),new Vec(40,-1,22)),"Filled-world optical negative did not block");
        require(negative.bodyBlocked(new Vec(40,-1,22),1.8,.21),"Filled-world body negative did not block");
        require(negative.support(40,22,0,4)!=null,"Filled-world support negative missing");
        var method=SceneRenderer.class.getDeclaredMethod("castleSurroundings");method.setAccessible(true);
        var faces=(List<?>)method.invoke(new SceneRenderer());
        int count=0;
        for(Object f:faces){
            int mat=((Number)LevelSceneSource.read(f,"material")).intValue();
            if(mat!=MaterialAtlas.FOLIAGE)continue;
            double cx=0,cz=0;boolean local=true;
            for(String corner:List.of("a","b","c")){
                Object p=LevelSceneSource.read(f,corner);
                double x=((Number)LevelSceneSource.read(p,"x")).doubleValue();
                double z=((Number)LevelSceneSource.read(p,"z")).doubleValue();
                local&=x>=-24-1e-6&&x<=48+1e-6&&z>=-8-1e-6&&z<=40+1e-6;cx+=x/3;cz+=z/3;
            }
            if(!local)continue;
            count++;
            require(!(cx>28.5+1e-6&&cx<43-1e-6&&cz>17.5+1e-6&&cz<34-1e-6),"Rendered terrain caps cellar");
        }
        require(count==terrain.triangleCount(),"Duplicate/missing procedural terrain in Castle: "+count+" vs "+terrain.triangleCount());
        require(map.traversable(13,19),"Castle spawn lost ground support");
        System.out.println("PASS Castle persisted editor hole:120 cut leaves, "+count+" shared render/body/optical/support faces; whole-footprint passage and actual filled-world negatives. Architectural stair/void is covered separately by CastleCellarConstructionTest.");
    }
}
