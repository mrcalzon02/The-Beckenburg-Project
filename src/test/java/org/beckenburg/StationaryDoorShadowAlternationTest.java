package org.beckenburg;

import dev.mrcalzon.lighting.LightModel;

/**
 * Reuse the same immutable receiver rays across an A->B->A House door.
 * A fresh lighting scene supplies the oracle for every door pose.
 */
public final class StationaryDoorShadowAlternationTest {
 private static void ok(boolean yes,String reason){if(!yes)throw new AssertionError(reason);}
 private static void close(FixtureLighting.RGB a,FixtureLighting.RGB b,String label){
  if(Math.abs(a.r()-b.r())>1e-6||Math.abs(a.g()-b.g())>1e-6
      ||Math.abs(a.b()-b.b())>1e-6)
   throw new AssertionError(label+": warm="+a+" fresh="+b);
 }
 public static void main(String[] args){
  var map=new HouseMap(HouseMap.Site.HOUSE);
  var lights=new RoomLighting();
  var field=new FixtureLighting(map);
  field.frame(lights,0);
  LevelDefinition.Portal door=null;FixtureLighting.Source source=null;
  for(var p:map.level().portals())if(p.door()){
   for(var candidate:field.sources()){
    if(candidate.room()<0||candidate.room()>=12)continue;
    if(!LightModel.portalInfluences(candidate.x(),candidate.y(),candidate.z(),
         candidate.radius(),p.x(),p.y(),p.z(),p.width(),p.alongX()))continue;
    // Need a lamp with its emission actually on one side, not within door frame.
    double side=p.alongX()?candidate.z()-p.z():candidate.x()-p.x();
    if(Math.abs(side)<.45)continue;
    door=p;source=candidate;break;
   }
   if(door!=null)break;
  }
  ok(door!=null,"No actual House portal with influencing fixture");
  var cache=field.newStationaryShadows();
  double side=door.alongX()?source.z()-door.z():source.x()-door.x();
  double x=door.x()+(door.alongX()?0:-Math.copySign(.55,side));
  double z=door.z()+(door.alongX()?-Math.copySign(.55,side):0);
  double y=door.y()+1.55;
  int room=map.roomAt(x,y,z);
  ok(room>=0,"Cross-door receiver is not in an authored room");
  double ax=door.x()-3,ay=door.y()-.8,az=door.z()-3,
         bx=door.x()+3,by=door.y()+3.4,bz=door.z()+3;
  long firstRays=-1;
  for(int toggle=0;toggle<5;toggle++){
   field.frame(lights,toggle);
   long revision=field.regionOccluderVersion(ax,ay,az,bx,by,bz);
   var previous=field.useStationaryShadows(cache,revision);
   try{
    var fresh=new FixtureLighting(map);fresh.frame(lights,toggle);
    for(int i=-2;i<=2;i++){
     double tx=x+(door.alongX()?i*.14:0);
     double tz=z+(door.alongX()?0:i*.14);
     close(field.prepareVertex(room,MaterialAtlas.STONE,tx,y,tz,0,0,0,false).sample(),
           fresh.prepareVertex(room,MaterialAtlas.STONE,tx,y,tz,0,0,0,false).sample(),
           "Door pose parity at toggle "+toggle+" offset "+i);
    }
   }finally{field.restoreStationaryShadows(previous);}
   if(toggle==0)firstRays=field.frameStats().shadowRays();
   ok(field.archivedStationaryShadowEntries()<=150000,
       "Door ray archive exceeded bounded memory budget");
   if(toggle<4)map.toggleDoor(door.id());
  }
  ok(firstRays>0,"The test must exercise real architectural light rays");
  ok(field.reusedStationaryDoorShadowRays()>0,
      "A->B->A door should restore actual cached stationary visibility rays");
  cache.release();
  ok(field.archivedStationaryShadowEntries()==0,
      "Closing a scene must release its alternate door-state rays");
  System.out.println("PASS bounded alternate-door stationary rays: restored "+
      field.reusedStationaryDoorShadowRays()+" prior-pose visibility values without stale RGB");
 }
}
