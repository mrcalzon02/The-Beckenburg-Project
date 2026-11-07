package org.beckenburg;
public final class VanSpawnSweepControl {
 public static void main(String[] args)throws Exception{
  System.setProperty("user.home",java.nio.file.Files.createTempDirectory("van-yaw-control-").toString());
  var map=new HouseMap();var p=map.van().placement();double y=map.floorY(HouseMap.EXTRACTION_ROOM);
  System.out.println("actual spawn "+p+" floor="+y+" standing="+map.traversable(p.x(),y,p.z(),HunterPosture.STANDING_HEIGHT));
  int blocked=0;for(int angle=0;angle<360;angle++){
   double yaw=Math.toRadians(angle);boolean valid=false;
   for(int i=0;i<4;i++){double a=yaw+i*Math.PI/2;var next=map.resolveMove(new LevelDefinition.Vec(p.x(),y,p.z()),p.x()+Math.sin(a)*.6,p.z()+Math.cos(a)*.6,HunterPosture.STANDING_HEIGHT);if(next!=null&&Math.hypot(next.x()-p.x(),next.z()-p.z())>.5)valid=true;}
   if(!valid){if(blocked++<10)System.out.println("four-key route unavailable at actual van spawn yaw="+angle);}
  }System.out.println("no four-key route at "+blocked+"/360 headings");
 }
}
