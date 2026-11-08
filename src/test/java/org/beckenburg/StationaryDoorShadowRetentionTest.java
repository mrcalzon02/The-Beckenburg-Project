package org.beckenburg;

import dev.mrcalzon.lighting.LightModel;

/** Regress source-wide cache eviction on door changes while preserving exact
 * fresh physical-ray lighting for the current authoritative door state. */
public final class StationaryDoorShadowRetentionTest {
 private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
 public static void main(String[] args){
  var map=new HouseMap(HouseMap.Site.HOUSE);
  var circuits=new RoomLighting();var light=new FixtureLighting(map);
  light.frame(circuits,0);
  FixtureLighting.Source chosen=null;LevelDefinition.Portal door=null;
  for(var p:map.level().portals())if(p.door()){
   for(var source:light.sources()){
    double side=Math.abs(p.alongX()?source.z()-p.z():source.x()-p.x());
    if(source.room()<0||source.room()>=12||side<.45)continue;
    if(!LightModel.portalInfluences(source.x(),source.y(),source.z(),source.radius(),
            p.x(),p.y(),p.z(),p.width(),p.alongX()))continue;
    chosen=source;door=p;break;
   }
   if(chosen!=null)break;
  }
  check(door!=null,"No real House door with a contributing same-side fixture");
  var shadows=light.newStationaryShadows();
  double rx=chosen.x()+(door.alongX()?.11:Math.copySign(.30,chosen.x()-door.x()));
  double rz=chosen.z()+(door.alongX()?Math.copySign(.30,chosen.z()-door.z()):.11);
  double ry=chosen.y()-.20;int room=chosen.room(),id=chosen.id();
  double ax=door.x()-2,ay=door.y()-.8,az=door.z()-2;
  double bx=door.x()+2,by=door.y()+3.2,bz=door.z()+2;
  long before=light.regionOccluderVersion(ax,ay,az,bx,by,bz);
  var prev=light.useStationaryShadows(shadows,before);
  try{
   for(int i=0;i<5;i++)light.prepareVertex(room,MaterialAtlas.STONE,
       rx+i*.08,ry,rz,0,0,0,false);
  }finally{light.restoreStationaryShadows(prev);}
  int entries=shadows.entriesForSource(id);
  check(entries>=3,"Expected same-side rays from fixture "+id+", found "+entries);
  map.toggleDoor(door.id());light.frame(circuits,1);
  long after=light.regionOccluderVersion(ax,ay,az,bx,by,bz);
  check(after>before,"Opening the door must advance nearby region revision");
  prev=light.useStationaryShadows(shadows,after);
  try{
   int retained=shadows.entriesForSource(id);
   check(retained>0,"Door discarded every same-side shadow of contributing lamp "+id);
   var warm=light.prepareVertex(room,MaterialAtlas.STONE,rx,ry,rz,0,0,0,false).sample();
   var fresh=new FixtureLighting(map);fresh.frame(circuits,1);
   var clean=fresh.prepareVertex(room,MaterialAtlas.STONE,rx,ry,rz,0,0,0,false).sample();
   check(Math.abs(warm.r()-clean.r())<1e-6
       &&Math.abs(warm.g()-clean.g())<1e-6
       &&Math.abs(warm.b()-clean.b())<1e-6,
       "Door shadow reuse differs from fresh physical light rays: "+warm+" vs "+clean);
   System.out.println("PASS House door "+door.id()+": retained "+retained+
       "/"+entries+" contributing-lamp rays on the same side, fresh-cache parity");
  }finally{light.restoreStationaryShadows(prev);shadows.release();}
 }
}
