package org.beckenburg;

import org.beckenburg.editor.*;
import org.beckenburg.LevelDefinition.Vec;
import java.nio.file.*;
import java.util.*;

/** Exercises persisted editor cutouts through real runtime physics and rendered faces. */
public final class HouseRebuildTerrainTest {
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        var world=StitchedHeightmapWorld.open(Path.of("development/levels/rebuild/house/terrain-world"));
        var topology=AdaptiveTerrainTopology.open(world);
        require(topology.cutoutCount()==52,"Saved editor cutouts missing");
        byte[] before=Files.readAllBytes(world.tilePath(0,0));
        var snapshot=topology.snapshot();
        var again=TerrainFootprintCutout.punch(topology,new TerrainEntranceMesh.Bounds(12,10,21.6,20.5));
        require(again.newSplits()==0&&again.newCutouts()==0,"Cutout authoring not idempotent");
        try{
            TerrainFootprintCutout.punch(topology,new TerrainEntranceMesh.Bounds(7.013,6.017,11.013,9.017));
            throw new AssertionError("Inexact footprint silently enlarged");
        }catch(IllegalArgumentException expected){require(snapshot.equals(topology.snapshot()),"Failed punch changed terrain");}
        require(Arrays.equals(before,Files.readAllBytes(world.tilePath(0,0))),"Source heights changed");
        var scene=new TerrainScenePackage(world).openScene();
        require(TerrainScenePackage.inspect(world,scene).isEmpty(),"Editor scene integrity failed");
        var exported=TerrainSceneNativePhysics.load(world,scene);
        var map=HouseRebuild.graybox();var punched=map.worldSurface();
        require(punched!=null&&punched.sha256().equals(exported.sourceSha256()),"Runtime differs from saved editor export");
        var a=new Vec(13.1,.2,12.5);var b=new Vec(13.1,-.9,12.5);
        require(punched.clearSight(a,b)&&map.clearLineOfSight(a.x(),a.y(),a.z(),b.x(),b.y(),b.z()),"World floor still caps the cellar stair");
        require(!punched.clearSight(new Vec(7,.2,7),new Vec(7,-1.2,7)),"Outside world floor disappeared");
        double stairY=-2.4+(12.5-10.5)/3.6*2.4;
        require(map.traversable(13.1,stairY,12.5),"Player cannot descend through punched world floor");
        require(punched.support(13.1,12.5,0,4)==null,"Invisible support spans the hole");
        require(punched.support(7,7,0,4)!=null,"Outside terrain has no support");

        // A separate, unpunched editor world is the negative control. No fake plane/box.
        Path control=Files.createTempDirectory("house-filled-terrain-");
        var filledWorld=StitchedHeightmapWorld.create(control,world.settings());
        filledWorld.importTile(0,0,world.tilePath(0,0),false);
        var filledScene=new TerrainScenePackage(filledWorld).openScene();
        TerrainScenePackage.publish(filledWorld,filledScene,"world",new TerrainEntranceMesh.Bounds(-39,-48,86,89));
        var filled=TerrainSceneNativePhysics.load(filledWorld,filledScene).physics();
        // The deliberately invalid filled cellar also prevents automatic House
        // fixture mounting. Use the fixture-free site profile for this control;
        // geometry, stairs, body/sight/object query paths are the same HouseMap.
        var blocked=new HouseMap(HouseMap.Site.MANSION,map.level(),filled);
        require(!blocked.traversable(13.1,stairY,12.5),"Runtime collision ignored filled editor ground");
        require(!blocked.clearLineOfSight(a.x(),a.y(),a.z(),b.x(),b.y(),b.z()),"Runtime sight ignored filled editor ground");
        require(!blocked.clearObjectPath(a.x(),a.y(),a.z(),b.x(),b.y(),b.z(),.03,.03),"Object sweep ignored filled editor ground");
        require(map.clearObjectPath(a.x(),a.y(),a.z(),b.x(),b.y(),b.z(),.03,.03),"Object cannot pass punched floor");

        var method=SceneRenderer.class.getDeclaredMethod("enclosureScenery",LevelDefinition.class);
        method.setAccessible(true);
        var faces=(List<?>)method.invoke(new SceneRenderer(),map.level());
        int rendered=0;
        for(Object face:faces){
            double[] x=new double[3],z=new double[3],y=new double[3];int n=0;
            for(String corner:List.of("a","b","c")){
                Object p=LevelSceneSource.read(face,corner);
                x[n]=((Number)LevelSceneSource.read(p,"x")).doubleValue();
                y[n]=((Number)LevelSceneSource.read(p,"y")).doubleValue();
                z[n++]=((Number)LevelSceneSource.read(p,"z")).doubleValue();
            }
            if(Math.abs(y[0]+.65)>1e-6||Math.abs(y[1]+.65)>1e-6||Math.abs(y[2]+.65)>1e-6)continue;
            rendered++;
            double cx=(x[0]+x[1]+x[2])/3,cz=(z[0]+z[1]+z[2])/3;
            require(!(cx>12+1e-6&&cx<21.6-1e-6&&cz>10+1e-6&&cz<20.5-1e-6),"Rendered world triangle caps basement");
        }
        require(rendered==punched.triangleCount(),"Rendered world faces differ from physical editor faces: "+rendered);
        require(new HouseMap(HouseMap.Site.HOUSE).worldSurface()==null,"Legacy map unexpectedly opted into pilot terrain");
        System.out.println("PASS persisted editor holes: source unchanged, rollback/idempotence, "+rendered
            +" shared render/collision faces, basement player/object/sight passage and filled-ground negatives");
        // Keep the bounded control for diagnosis under the system temporary directory.
    }
}
