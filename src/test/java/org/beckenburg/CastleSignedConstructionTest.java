package org.beckenburg;
import java.nio.file.*;import java.util.*;
/** Real native authoring and runtime support in the annex/cellar bounds. */
public final class CastleSignedConstructionTest {
 static void check(boolean value,String reason){if(!value)throw new AssertionError(reason);}
 public static void main(String[] args)throws Exception{
  Path home=Files.createTempDirectory("castle-signed-");String old=System.getProperty("user.home");
  System.setProperty("user.home",home.toString());
  try{
   var scene=CastleLevelEdits.current();var before=scene.snapshot();
   var map=new CastleHubMap();check(map.solid(-2,28),"Absent annex floor is solid before native edit");
   var renderer=new SceneRenderer();renderer.setRenderSize(800,500);renderer.x=-3;renderer.z=28;renderer.eyeHeight=1.62;renderer.yaw=-Math.PI/2;renderer.pitch=-.25;renderer.lamp=true;
   var project=new CastleEditorProject(scene);
   var service=new org.beckenburg.editor.EditorAgentService<>(new CastleEditorAgent(project,map,renderer,()->{}),new org.beckenburg.editor.EditorHistory<>(project,60));
   service.transact(scene.revision(),"put-surface",Map.of("id","annex-floor","kind","FLOOR","x0","-17","z0","26","x1","1","z1","30","base","0","height",".22","material",Integer.toString(MaterialAtlas.STONE)));
   check(!map.solid(-2,28),"Agent transaction changes actual runtime support");
   service.undo(scene.revision());check(map.solid(-2,28),"Shared agent undo removes actual support");
   service.redo(scene.revision());check(!map.solid(-2,28),"Shared agent redo restores actual support");
   scene.put(new CastleLevelEdits.Surface("cellar-floor",CastleLevelEdits.Kind.FLOOR,28.5,17.5,43,34,-3,.22,MaterialAtlas.BRICK));
   scene.put(new CastleLevelEdits.Prop("cellar-basin","level_bath_basin",30,-3,20,1,0,.33,.25,false));
   check(map.floorMaterial(-2,28)==MaterialAtlas.STONE,"Ground renderer/footfall material follows actual native floor");
   check(!map.solid(-2,28)&&map.traversable(-2,28),"Native annex floor supports actual body outside original raster");
   check(!map.traversable(-17.1,28)&&!map.traversable(-2,30.1),"A world-bound extension cannot create unsupported walkable ground");
   scene.paintFloor(43,0,MaterialAtlas.PARQUET);scene.paintFloor(-1,1,MaterialAtlas.BRICK);
   check(scene.floorMaterial(43,0,-1)==MaterialAtlas.PARQUET&&scene.floorMaterial(-1,1,-1)==MaterialAtlas.BRICK,"Negative cell must not alias legacy43/0");
   scene.setNavBlocked(-2,28,true);check(!map.traversable(-2,28),"Actual annex nav block participates in body collision");scene.setNavBlocked(-2,28,false);
   scene.put(new CastleLevelEdits.Surface("annex-wall",CastleLevelEdits.Kind.WALL,-5,26,-5,30,0,3.4,MaterialAtlas.STONE));
   check(!map.traversable(-5,28)&&map.traversable(-3,28),"Actual native wall blocks west-annex movement");
   var moved=CastleSurfaceTransforms.drag(scene.surfaces().stream().filter(s->s.id().equals("annex-wall")).findFirst().orElseThrow(),-5,28,-1,0,false);
   check(moved.x0()==-6,"Human editor drag retains signed coordinates");
   check(scene.floorSupports(30,20,-3),"Native cellar floor remains at actual negative elevation");
   if(args.length>0){Path capture=Path.of(args[0]).toAbsolutePath();Files.createDirectories(capture.getParent());service.capture(scene.revision(),capture);}
   var stable=scene.snapshot();
   try{service.transact(scene.revision(),"put-surface",Map.of("id","out-of-world","kind","FLOOR","x0","-100","z0","26","x1","1","z1","30","base","0","height",".22","material","0"));throw new AssertionError("Invalid native bounds accepted");}
   catch(IllegalArgumentException expected){}
   check(scene.snapshot().equals(stable),"Invalid geometry transaction preserves native scene");
   service.save(scene.revision());var expected=scene.snapshot();var reopened=new CastleLevelEdits(scene.path());
   check(reopened.snapshot().equals(expected),"Signed floor/prop/paint/nav roundtrip is exact");
   check(reopened.floorMaterial(43,0,-1)==MaterialAtlas.PARQUET&&reopened.floorMaterial(-1,1,-1)==MaterialAtlas.BRICK,"Reopen preserves legacy/annex paint separately");
   for(int key=0;key<CastleWorldBounds.CELL_COUNT;key++){
    int x=CastleWorldBounds.cellX(key),z=CastleWorldBounds.cellZ(key);
    check(CastleWorldBounds.cellKey(x,z)==key,"Stable cell encoding must be bijective");
    if(x>=0)check(key==z*44+x,"Legacy positive IDs cannot change");
   }
   scene.restore(before);check(map.solid(-2,28)&&!map.traversable(-2,28),"Native restore removes real annex support from existing runtime map");check(scene.snapshot().equals(before),"Restore preserves original native scene");
   System.out.println("PASS Castle signed construction: real annex support/wall/nav collision; negative cellar source; no legacy paint alias; exact save/reopen/history and all2232 cell IDs");
  }finally{System.setProperty("user.home",old);try(var files=Files.walk(home)){for(Path p:files.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
 }
}
