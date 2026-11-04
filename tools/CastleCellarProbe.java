package org.beckenburg;
import java.nio.file.*;
public class CastleCellarProbe {public static void main(String[] args)throws Exception {
System.setProperty("user.home",Files.createTempDirectory("castle-cellar-probe-").toString());var map=new CastleHubMap();var s=CastleLevelEdits.current();double old=0;
for(double z=25;z>17.5;z-=.125){double y=CastleHubArchitecturalPlan.elevation(40,z,old);System.out.println(z+" y="+y+" walk="+map.traversable(40,z,y)+" wall="+s.wallBlocks(40,y,z,.21)+" slab="+CastleRooms.slabBodyBlocks(40,y,z,.21,1.8)+" world="+map.worldSurface().bodyBlocked(new LevelDefinition.Vec(40,y,z),1.8,.21)+" support="+s.floorSupports(40,z+.21,y));old=y;}
}}
