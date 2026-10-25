package org.beckenburg;
import java.nio.file.*;
import java.util.*;
import org.beckenburg.LevelDefinition.Vec;
/** Actual game HouseMap/LanWorld queries, not a native-component proxy. */
public final class HollowCreekRuntimeTest {
 static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
 public static void main(String[] args)throws Exception{
  var map=new HouseMap(HouseMap.Site.HOLLOW_CREEK);check(map.nativeSite(),"Native world branch");check(map.valid(),"All15 game zone anchors and graph are valid");check(map.level().props().isEmpty(),"No generated replacement room furniture");check(map.level().rooms().stream().allMatch(r->r.outside()),"Open site ownership must not invent interior walls");check(RitualSites.valid(map),"All three actual ritual approaches are supported");
  check(HidingBudget.candidates(map.level()).size()==HidingBudget.MAXIMUM,"Shared maximum three genuine cover models");
  for(var refuge:HidingBudget.candidates(map.level())){check(map.traversable(refuge.x(),refuge.y(),refuge.z(),HunterPosture.CROUCH_HEIGHT),"Supported actual refuge "+refuge.id());check(OutdoorHiding.available(map.level(),refuge,100)&&!OutdoorHiding.available(map.level(),refuge,0),"Highest difficulty removes all cover");}
  check(map.sheltered(32,1.62,42)&&map.sheltered(32,3.62,78),"Actual pavilion/van roof weather shelter");
  var world=new LanWorld(map);for(int i=0;i<6;i++){var b=world.body(i);check(b.room()==14&&Math.abs(b.y()-2)<1e-6&&map.traversable(b.x(),b.y(),b.z()),"Six actual elevated van spawn pads");}
  Vec previous=null;String label="";int points=0,sweeps=0;
  for(String line:Files.readAllLines(Path.of("development/levels/verification/hollow-creek-native-queries.tsv"))){String[] p=line.split("\t");if(!p[0].equals("S")||!p[4].startsWith("L"))continue;double x=Double.parseDouble(p[1]),z=Double.parseDouble(p[2]);Double vanY=map.van().supportHeight(x,z);var at=new Vec(x,vanY==null?Double.parseDouble(p[3]):vanY,z);check(map.traversable(at.x(),at.y(),at.z()),"Game route body "+p[4]+" "+at);check(map.roomAt(at.x(),at.y(),at.z())>=0,"Game route ownership "+p[4]);
   if(label.equals(p[4])&&previous!=null&&Math.hypot(at.x()-previous.x(),at.z()-previous.z())>1e-6){check(map.resolveMove(previous,at.x(),at.z())!=null,"Game movement sweep "+p[4]);sweeps++;}previous=at;label=p[4];points++;
  }
  var water=HollowCreekWater.module().bodies().get(0);check(!water.wet(23.9,-5),"Actual native inner hull must be dry");check(water.wet(22,-5),"Lake next to boat must remain wet");
  check(!map.traversable(23.9,-1.95,-5),"Water beside dock is not a walking floor");
  check(!map.traversable(49,map.groundHeight(49,23,0),23),"Submerged native creek bed cannot be an eligible walking/spawn surface");
  System.out.println("PASS actual Hollow Creek game map:15 zone/graph anchors,3 rituals,6 elevated LAN spawn pads,"+points+" standing route points,"+sweeps+" game movement sweeps, actual dry boat waterline and no water walking");
 }
}
