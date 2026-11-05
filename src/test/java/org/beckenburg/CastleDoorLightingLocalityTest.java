package org.beckenburg;

import java.util.Comparator;

/** Release regression: Castle doors must never discard every room's lighting
 * or force every resident world chunk to re-probe its stationary shadows. */
public final class CastleDoorLightingLocalityTest {
 private static void check(boolean ok,String reason){
  if(!ok)throw new AssertionError(reason);
 }
 private static double distanceToDoor(FixtureLighting.Source source,CastleDoorSlots.Slot slot){
  double reach=slot.width()*.5+.45;
  double dx=Math.max(0,Math.abs(source.x()-slot.centerX())-reach);
  double dz=Math.max(0,Math.abs(source.z()-slot.centerZ())-reach);
  double dy=Math.max(0,slot.bottom()-.75-source.y());
  return Math.hypot(Math.hypot(dx,dz),dy);
 }
 private static void nearSamples(FixtureLighting warm,FixtureLighting fresh,
      CastleDoorSlots.Slot slot,int transition){
  for(double offset:new double[]{-1.2,-.45,.45,1.2}){
   double x=slot.centerX()+(slot.horizontal()?0:offset);
   double z=slot.centerZ()+(slot.horizontal()?offset:0);
   double y=slot.bottom()+1.4;
   var existing=warm.sample(0,x,y,z);
   var clean=fresh.sample(0,x,y,z);
   check(Math.abs(existing.r()-clean.r())<1e-6
       &&Math.abs(existing.g()-clean.g())<1e-6
       &&Math.abs(existing.b()-clean.b())<1e-6,
       "Castle door "+slot.id()+" stale shadow at toggle "+transition
       +" offset "+offset+": warm="+existing+" fresh="+clean);
  }
 }
 public static void main(String[] args){
  var map=new CastleHubMap();
  var warm=new FixtureLighting(map);
  warm.castleFrame(true,0);
  var door=CastleDoorSlots.all().stream()
      .filter(CastleDoorSlots::available)
      .filter(slot->CastleLevelEdits.current().doorType(slot.id())!=CastleDoorSlots.Type.EMPTY)
      .filter(slot->warm.sources().stream().anyMatch(source->
          distanceToDoor(source,slot)<=source.radius()))
      .min(Comparator.comparingDouble(slot->warm.sources().stream()
          .mapToDouble(source->distanceToDoor(source,slot)).min()
          .orElse(Double.POSITIVE_INFINITY)))
      .orElseThrow(()->new AssertionError("No populated Castle door near a fixture"));
  long global=warm.occluderRevision();
  long far=warm.regionOccluderVersion(-500,-100,-500,-490,-90,-490);
  for(int toggle=1;toggle<=20;toggle++){
   // Warm the real per-receiver and directional caches before changing state.
   warm.sample(0,door.centerX(),door.bottom()+1.4,door.centerZ());
   warm.sampleNormal(0,door.centerX(),door.bottom()+1.4,door.centerZ(),0,1,0);
   map.toggleDoor(door.id());
   warm.castleFrame(true,0);
   check(warm.occluderRevision()==global,
       "Castle door advanced global occlusion revision on toggle "+toggle);
   check(warm.regionOccluderVersion(-500,-100,-500,-490,-90,-490)==far,
       "Castle door invalidated an unrelated region on toggle "+toggle);
   check(warm.frameStats().invalidatedCells()>0,
       "Castle door did not invalidate its affected receiver cells");
   if(toggle%4==0){
    var fresh=new FixtureLighting(map);
    fresh.castleFrame(true,0);
    nearSamples(warm,fresh,door,toggle);
   }
  }
  System.out.println("PASS 20 alternating Castle door toggles: bounded light invalidation, stable global/remote revisions, fresh-cache lighting parity");
 }
}
