package org.beckenburg;
import java.nio.file.Files;
import java.util.HashSet;

/** One authoritative door assembly per wall opening, world interactions and exact shadows. */
public final class CastlePortalSlotTest {
 private static void check(boolean yes,String message){if(!yes)throw new AssertionError(message);}
 public static void main(String[] ignored)throws Exception{
  var map=new CastleHubMap();
  var slots=CastleDoorSlots.all();
  check(slots.size()>5,"every authored masonry opening has a stable socket");
  var ids=new HashSet<String>();
  for(var slot:slots){
   check(ids.add(slot.id()),"no duplicate physical doorway ID: "+slot.id());
   check(slot.width()>2,"archways are genuine traversable openings");
  }
  var gate=CastleDoorSlots.require("courtyard-main-gate");
  check(gate.gate()&&gate.width()>4.5&&gate.horizontal()&&gate.z0()==34,
    "courtyard gateway has one dedicated wide portcullis socket");
  check(!map.doorOpen(gate.id()),"gate starts lowered");
  check(!map.traversable(15.5,34),"closed gate blocks player movement");
  check(!FixtureLighting.castleRayClear(map,15.5,1.5,33.3,15.5,1.5,34.7),
    "lowered portcullis blocks straight-through lighting");
  check(map.aimedDoor(15.5,32.5,0)!=null,
    "portcullis can be operated through actual camera facing from courtyard");
  check(map.toggleDoor(gate.id()),"portcullis opens");
  check(map.traversable(15.5,34),"raised portcullis physically clears the gate aperture");
  check(FixtureLighting.castleRayClear(map,15.5,1.5,33.3,15.5,1.5,34.7),
    "raised portcullis clears light through the same aperture");
  check(!map.traversable(12,34),"solid gatehouse masonry remains a collision blocker");
  check(!FixtureLighting.castleRayClear(map,12,1.5,33.3,12,1.5,34.7),
    "thick gatehouse shoulder still stops lighting");
  var folder=Files.createTempDirectory("castle-doors-");
  try{
   var path=folder.resolve("castle.properties");
   var scene=new CastleLevelEdits(path);
   check(scene.doorType(gate.id())==CastleDoorSlots.Type.PORTCULLIS,
     "default castle scene has exactly one installed gate assembly");
   boolean duplicate=false;
   try{scene.installDoor(gate.id(),CastleDoorSlots.Type.PORTCULLIS);}
   catch(IllegalStateException correct){duplicate=true;}
   check(duplicate,"second door on an occupied socket is forbidden");
   var arch=slots.stream().filter(s->!s.gate()).findFirst().orElseThrow();
   check(scene.doorType(arch.id())==CastleDoorSlots.Type.EMPTY,
     "an archway has no secretly assigned duplicate leaf");
   scene.installDoor(arch.id(),CastleDoorSlots.Type.WOOD);
   duplicate=false;
   try{scene.installDoor(arch.id(),CastleDoorSlots.Type.WOOD);}
   catch(IllegalStateException correct){duplicate=true;}
   check(duplicate,"interior door slot rejects a second door too");
   scene.save();
   var restored=new CastleLevelEdits(path);
   check(restored.doorType(arch.id())==CastleDoorSlots.Type.WOOD,
     "wooden door ownership survives a complete scene reload");
   check(restored.doorType(gate.id())==CastleDoorSlots.Type.PORTCULLIS,
     "gate assignment survives a complete scene reload");
   restored.removeDoor(arch.id());restored.save();
   check(new CastleLevelEdits(path).doorType(arch.id())==CastleDoorSlots.Type.EMPTY,
     "removing a door leaves one clear archway without recreating the leaf");
  }finally{
   try(var walk=Files.walk(folder)){
    for(var file:walk.sorted(java.util.Comparator.reverseOrder()).toList())
     Files.deleteIfExists(file);
   }
  }
  System.out.println("PASS one door per socket, physical gate toggle, thick-wall occlusion and saved occupancy");
 }
}
