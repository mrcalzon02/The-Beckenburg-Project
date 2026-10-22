package org.beckenburg;

import java.nio.file.Files;

/** Real saved scene round-trip: placing objects/lights, moving stations,
 * painting surfaces, setting spawn and modifying collision navigation. */
public final class DeveloperLevelEditorDataTest {
 private static void check(boolean ok,String detail){if(!ok)throw new AssertionError(detail);}
 public static void main(String[] args)throws Exception{
  var dir=Files.createTempDirectory("beckenburg-editor-test-");
  try{
   var file=dir.resolve("castle.properties");
   var edits=new CastleLevelEdits(file);
   check(edits.props().size()>25,"Initial castle furniture must not collapse into one object");
   check(edits.lamps().size()>12,"All authored castle lamps must be exposed");
   check(GlbProps.availableModels().contains("sofa_1"),"Editor includes real textured GLBs");
   check(GlbProps.availableModels().contains("blender_emf_reader_2"),
       "The complete licensed equipment asset list is available");
   edits.put(new CastleLevelEdits.Prop("testBench","sofa_1",7,0,23,.72,.3,.7,.35,false));
   edits.put(new CastleLevelEdits.Lamp("extra","ceiling_lamp_1_on","ceiling_lamp_1_off",
       5,3.38,17.5,.8,0,CastleHubDecor.Mount.CEILING,12,1.8));
   edits.paintFloor(4,17,MaterialAtlas.BRICK);
   edits.paintWall(0,MaterialAtlas.AGED_WOOD);
   edits.setNavBlocked(3,17,true);
   edits.setSpawn(13.5,19,1.5);
   edits.setStation(CastleHubMap.Station.MULTIPLAYER,23,17);
   edits.save();
   var copy=new CastleLevelEdits(file);
   check(copy.props().stream().anyMatch(v->v.id().equals("testBench")&&v.mesh().equals("sofa_1")),
       "Placed object survives game restart");
   check(copy.lamps().stream().anyMatch(v->v.id().equals("extra")&&v.radius()==12),
       "Placed shadow-casting fixture survives");
   check(copy.floorMaterial(4,17,0)==MaterialAtlas.BRICK,"Saved 3D floor texture");
   check(copy.wallMaterial(0,0)==MaterialAtlas.AGED_WOOD,"Saved wall material");
   check(copy.navBlocked(3,17),"Saved navigation blocker");
   check(copy.station(CastleHubMap.Station.MULTIPLAYER)[0]==23,
       "Station geometry/interaction socket move persists");
   check(copy.spawn()[2]==1.5,"Player spawn yaw persists");
   check(copy.furnishings(false).stream().anyMatch(v->v.mesh().equals("sofa_1")&&v.x()==7),
       "Saved furniture writes back to real game collision list");
   copy.removeProp("testBench");copy.save();
   check(new CastleLevelEdits(file).props().stream().noneMatch(v->v.id().equals("testBench")),
       "Deleting props persists");
   System.out.println("PASS real 3D level scene persistence, lights, material assignments, spawn and nav");
  }finally{
   try(var paths=Files.walk(dir)){paths.sorted(java.util.Comparator.reverseOrder()).forEach(p->{
    try{Files.deleteIfExists(p);}catch(Exception ignored){}
   });}
  }
 }
}
