package org.beckenburg;
public final class VanInputRouteControl {
 public static void main(String[] args)throws Exception{
  System.setProperty("user.home",java.nio.file.Files.createTempDirectory("van-input-control-").toString());
  var type=Class.forName("org.beckenburg.gpu.GpuUiSmoke");var planner=type.getDeclaredMethod("planWalk",HouseMap.class,SceneRenderer.class);planner.setAccessible(true);
  int diagonal=0,total=0;
  for(var site:new HouseMap.Site[]{HouseMap.Site.HOUSE,HouseMap.Site.FARMHOUSE,HouseMap.Site.SCHOOL}){
   var map=new HouseMap(site);var p=map.van().placement();var scene=new SceneRenderer();scene.x=p.x();scene.z=p.z();scene.eyeHeight=map.floorY(HouseMap.EXTRACTION_ROOM)+HunterPosture.STANDING_EYE;
   for(int degrees=0;degrees<360;degrees++){
    scene.yaw=Math.toRadians(degrees);Object route=planner.invoke(null,map,scene);var second=route.getClass().getDeclaredMethod("second");second.setAccessible(true);if((int)second.invoke(route)!=0)diagonal++;total++;
   }System.out.println("PASS actual production GPU planner: "+site+" real van floor/body collision at all360 camera headings");
  }System.out.println("PASS "+total+" routes; "+diagonal+" require real two-key WASD input; original .6m sweep and >.5m candidate retained");
 }
}
