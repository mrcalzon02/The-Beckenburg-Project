package org.beckenburg;

import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.beckenburg.editor.*;
import org.beckenburg.LevelDefinition.Vec;

/** Native spawn-to-cellar routes, actual filled-world negative, clear floor/headroom and physical fixtures. */
public final class CastleCellarConstructionTest {
    private static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
    private static String key(double x,double y,double z){return Math.round(x*8)+":"+Math.round(y*10000)+":"+Math.round(z*8);}
    public static void main(String[] args)throws Exception{
        System.setProperty("user.home",Files.createTempDirectory("castle-cellar-").toString());
        var scene=CastleLevelEdits.current();var map=new CastleHubMap();
        check(CastleCellar.active(),"Fresh native cellar absent");
        var q=new ArrayDeque<Vec>();var seen=new HashSet<String>();double[] spawn=scene.spawn();
        q.add(new Vec(spawn[0],0,spawn[1]));seen.add(key(spawn[0],0,spawn[1]));
        var reached=new HashSet<String>();
        while(!q.isEmpty()){
            var p=q.removeFirst();
            for(var room:CastleCellarLayout.ROOMS)if(p.y()< -2.95&&room.contains(p.x(),p.z()))reached.add(room.id());
            for(var r:CastleAnnex.SPACES)if(Math.abs(p.y())<.01&&p.x()>r.x0()&&p.x()<r.x1()&&p.z()>r.z0()&&p.z()<r.z1())reached.add(r.id());
            for(double[] delta:new double[][]{{.125,0},{-.125,0},{0,.125},{0,-.125}}){
                double x=p.x()+delta[0],z=p.z()+delta[1];
                double y=CastleHubArchitecturalPlan.elevation(x,z,p.y());String k=key(x,y,z);
                if(!seen.contains(k)&&CastleHubPanel.safeElevationTransition(p.y(),y,x,z)&&map.traversable(x,z,y)){
                    seen.add(k);q.add(new Vec(x,y,z));
                }
            }
            check(seen.size()<400000,"Unbounded native physical route flood");
        }
        for(var r:CastleCellarLayout.ROOMS)check(reached.contains(r.id()),"Unreachable actual cellar room "+r.id()+" / "+reached);
        for(var r:CastleAnnex.SPACES)check(reached.contains(r.id()),"Annex route lost "+r.id());
        for(double z=CastleCellarLayout.LOW_Z+.02;z<CastleCellarLayout.HIGH_Z;z+=.04){
            double y=CastleCellar.stairHeight(z);
            check(map.traversable(40,z,y),"Flight obstructed at "+z+" / "+y);
            check(!map.traversable(39.15,z,y),"Stair cheek/guard not physical");
            check(!CastleRooms.slabBodyBlocks(40,y,z,.21,1.8),"Floor soffit caps flight headroom");
        }
        double walkY=0;
        for(int i=0;i<=256;i++){
            double z=24.8-i*.025,next=CastleHubArchitecturalPlan.elevation(40,z,walkY);
            check(CastleHubPanel.safeElevationTransition(walkY,next,40,z)&&map.traversable(40,z,next),"Continuous descending body sweep blocked at "+z);
            walkY=next;
        }
        check(Math.abs(walkY+3)<1e-7,"Descent misses actual lower landing");
        for(int i=0;i<=256;i++){
            double z=18.4+i*.025,next=CastleHubArchitecturalPlan.elevation(40,z,walkY);
            check(CastleHubPanel.safeElevationTransition(walkY,next,40,z)&&map.traversable(40,z,next),"Continuous ascending body sweep blocked at "+z);
            walkY=next;
        }
        check(Math.abs(walkY)<1e-7,"Ascent misses actual upper landing");
        check(!map.traversable(40,21),"Ground navigation bridges the stair void");
        check(!CastleHubPanel.safeElevationTransition(0,-3,38,22),"Ground-to-cellar teleport permitted");
        check(!CastleHubPanel.safeElevationTransition(-3,0,38,22),"Cellar-to-ground teleport permitted");
        check(CastleRooms.slabBodyBlocks(35,-.8,22,.21,1.8),"Retained ground slab no longer blocks heads");
        check(!FixtureLighting.castleRayClear(map,35,.2,22,35,-1,22),"Retained ground slab lost optical occlusion");
        check(FixtureLighting.castleRayClear(map,40,.1,22.1,40,-1,22.1),"Architectural/world floor still caps the flight");
        check(!FixtureLighting.castleRayClear(map,40,-2.9,22.1,40,-2.5,22.1),"Filled stair mass leaks light from below");
        for(var c:CastleRooms.slabCells(CastleRooms.structuralDecks()))if(c.top()==0)
            check(!CastleCellar.opening((c.x0()+c.x1())/2,(c.z0()+c.z1())/2),"Rendered structural ground cell caps the opening");

        var world=StitchedHeightmapWorld.open(Path.of("development/levels/rebuild/castle/terrain-world"));
        var filled=StitchedHeightmapWorld.create(Files.createTempDirectory("castle-cellar-filled-"),world.settings());
        filled.importTile(0,0,world.tilePath(0,0),false);
        var filledScene=new TerrainScenePackage(filled).openScene();
        TerrainScenePackage.publish(filled,filledScene,"castle",new TerrainEntranceMesh.Bounds(-24,-8,48,40));
        var blocked=new CastleHubMap(TerrainSceneNativePhysics.load(filled,filledScene).physics());
        double crossing=CastleCellar.stairHeight(22.1);
        check(map.traversable(40,22.1,crossing),"Punched terrain blocks real stair body");
        check(!blocked.traversable(40,22.1,crossing),"Actual runtime ignores filled world floor");
        check(!FixtureLighting.castleRayClear(blocked,40,.1,22.1,40,-1,22.1),"Actual lighting ignores filled world floor");
        var door=CastleDoorSlots.require("portal-v-3700-2200");
        check(door.bottom()==-3&&Math.abs(door.clearHeight()-2.4)<1e-7,"Lower portal has wrong native elevation/headroom");
        scene.installDoor(door.id(),CastleDoorSlots.Type.WOOD);
        check(!map.traversable(37,22,-3)&&!FixtureLighting.castleRayClear(map,36,-1.6,22,38,-1.6,22),"Closed lower leaf fails body/optical collision");
        check(map.aimedDoor(35.5,-3,22,Math.PI/2)==door,"Lower door cannot be aimed from its actual floor");
        map.toggleDoor(door.id());
        check(map.traversable(37,22,-3)&&FixtureLighting.castleRayClear(map,36,-1.6,22,38,-1.6,22),"Opened lower leaf blocks its aperture");
        scene.removeDoor(door.id());
        check(map.traversable(29.95,29.65,-3),"Ground warehouse rack creates phantom cellar collision");
        scene.put(new CastleLevelEdits.Prop("cellar-prop-control","chair_mp_5",31,-3,22.2,1,0,.4,.4,false));
        check(!map.traversable(31,22.2,-3),"Cellar furniture has no lower body collision");
        check(map.traversable(31,22.2),"Cellar furniture blocks ground store");
        scene.put(new CastleLevelEdits.Prop("cellar-prop-control","chair_mp_5",31,-3,22.2,5,0,.4,.4,false));
        check(!map.traversable(31,22.2),"Actual tall lower mesh extending upstairs is ignored");
        scene.removeProp("cellar-prop-control");
        var lighting=new FixtureLighting(map);
        for(var lamp:scene.lamps())if(lamp.id().startsWith("cellar-light-")){
            check(lamp.fixture().attached(map),"Unsupported cellar lamp "+lamp.id());
            if(lamp.mount()==CastleHubDecor.Mount.CEILING)check(!scene.wallBlocks(lamp.x(),lamp.y()-.1,lamp.z(),.43),"Cellar fixture intersects masonry");
            double px=lamp.id().contains("stair-")?40:lamp.x(),py=lamp.id().contains("stair-")?CastleCellar.stairHeight(lamp.z())+1.3:-1.4;
            lighting.castleFrame(false,10);double dark=lighting.sample(0,px,py,lamp.z()).energy();
            lighting.castleFrame(true,10);double lit=lighting.sample(0,px,py,lamp.z()).energy();
            check(lit>dark+.2,"Cellar lamp fails to illuminate actual lower room: "+lamp.id()+" / "+dark+" -> "+lit);
        }
        scene.paintFloor(32,30,MaterialAtlas.CARPET);
        check(map.footstep(32.5,30.5,-3)==LanSound.Cue.STEP_STONE,"Ground floor paint leaks into cellar footfall");
        var sw=CastleHubDecor.SWITCHES.stream().filter(s->s.y()<0).findFirst().orElseThrow();
        check(CastleHubDecor.aimed(map,29.6,-3,24.5,-Math.PI/2)==sw,"Real lower switch unreachable/occluded");
        check(map.lightOn(33,-1.4,30),"Cellar circuit starts off");map.toggleCircuit(sw);
        check(!map.lightOn(33,-1.4,30)&&map.lightOn(35,1.4,25),"Cellar switch controls the wrong elevation");map.toggleCircuit(sw);
        var project=new CastleEditorProject(scene);
        var agent=new CastleEditorAgent(project,map,new SceneRenderer(),()->{});
        var service=new EditorAgentService<>(agent,new EditorHistory<>(project,60));
        service.transact(scene.revision(),"remove-surface",Map.of("id","cellar-stair-opening"));
        check(!map.traversable(40,22.1,crossing),"Removing native architectural opening leaves phantom headroom");
        check(!FixtureLighting.castleRayClear(map,40,.1,22.1,40,-1,22.1),"Removing native opening leaves phantom optical void");
        check(map.worldSurface().clearSight(new Vec(40,.1,22.1),new Vec(40,-1,22.1)),"Architectural edit modified editor world terrain");
        service.undo(scene.revision());check(map.traversable(40,22.1,crossing),"Actual service undo failed to restore stair opening");
        service.redo(scene.revision());check(!map.traversable(40,22.1,crossing),"Actual service redo failed to close opening");
        service.undo(scene.revision());
        var snapshot=scene.snapshot();scene.save();scene.reopen();check(snapshot.equals(scene.snapshot()),"Native cellar save/reopen differs");
        check(map.traversable(40,22.1,crossing),"Reopened cellar stair unsupported");
        if(args.length>0){
            Path folder=Path.of(args[0]);Files.createDirectories(folder);
            var renderer=new SceneRenderer();renderer.setRenderSize(1000,625);renderer.lamp=true;
            renderer.x=40;renderer.z=24.8;renderer.eyeHeight=1.62;renderer.yaw=Math.PI;renderer.pitch=-.45;
            renderer.drawCastle(map,0,true);ImageIO.write(renderer.image(),"png",folder.resolve("cellar-stair-runtime.png").toFile());
            renderer.x=34.5;renderer.z=22;renderer.eyeHeight=-1.38;renderer.yaw=Math.PI/2;renderer.pitch=-.03;
            renderer.drawCastle(map,0,true);ImageIO.write(renderer.image(),"png",folder.resolve("cellar-hall-runtime.png").toFile());
        }
        System.out.println("PASS Castle cellar construction: "+seen.size()+" physical positions from native spawn reach all4 cellar and4 annex rooms; continuous18-riser flight, true deck/world openings, body/head/optical guards, filled-world negatives, separated furniture/circuits and exact native reopen.");
    }
}
