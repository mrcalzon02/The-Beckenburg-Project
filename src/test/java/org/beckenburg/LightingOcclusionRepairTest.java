package org.beckenburg;

/** Real 3D shadow regression for the common ghost-investigation/castle lighting path. */
public final class LightingOcclusionRepairTest {
 private static void check(boolean pass,String detail){if(!pass)throw new AssertionError(detail);}
 public static void main(String[] ignored){
  var castle=new CastleHubMap();
  var light=new FixtureLighting(castle);light.castleFrame(true,0);
  check(!FixtureLighting.castleRayClear(castle,9.8,1.5,3.0,11.4,1.5,3.0),
   "solid castle wall MUST cast shadows");
  check(FixtureLighting.castleRayClear(castle,9.8,1.5,6.5,11.4,1.5,6.5),
   "authored open archway must transmit light");
  check(!FixtureLighting.castleRayClear(castle,9.8,3.02,6.5,11.4,3.02,6.5),
   "lintel above archway must cast shadows");
  check(!FixtureLighting.castleRayClear(castle,14,2.0,5,14,4.0,5),
   "library floor/ceiling must block light between storeys");
  check(FixtureLighting.castleRayClear(castle,13.5,2,25,13.5,4.4,25),
   "open courtyard must see the sky");
  check(light.sample(0,5,2,5).energy()>0,
   "enabled castle lighting must produce real illumination");
  var house=new HouseMap(HouseMap.Site.HOUSE);
  var indoors=new FixtureLighting(house);
  // Mission maps retain their dynamic doors and collision ray tests.
  check(!house.clearLineOfSight(-100,1,-100,100,1,100),
   "mission LOS must reject inaccessible external light rays");
  System.out.println("PASS shared 3D lighting occlusion, castle walls, portals, ceiling, courtyard and mission LOS");
 }
}
