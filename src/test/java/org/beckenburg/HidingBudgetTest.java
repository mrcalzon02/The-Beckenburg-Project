package org.beckenburg;
import java.util.*;
/** Shared combined cap and exact density counts on every currently built map. */
public final class HidingBudgetTest {
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 public static void main(String[] args)throws Exception{
  try(var user=AuthoredTestSupport.userFixture()){
  var maps=new ArrayList<HouseMap>();
  for(var site:HouseMap.Site.values())maps.add(user.map(site));
  maps.add(new HouseMap(HouseMap.Site.HOUSE,BlackpineLevel.build()));
  for(var physical:maps){
   var level=physical.level();
   check(HidingBudget.candidates(level).size()==HidingBudget.MAXIMUM,"Insufficient authored candidates "+level.id());
   physical.configureHiding(100);
   for(var socket:HidingBudget.candidates(level))if(socket.type().equals("hide")){
    check(physical.traversable(socket.x(),socket.y(),socket.z()),"Hiding pocket support/body fit "+level.id()+" "+socket.room());
    var approach=LevelClosets.world(socket,0,0,1.10);
    check(physical.traversable(approach.x(),approach.y(),approach.z()),"Cupboard approach body fit "+level.id()+" "+socket.room());
   }
   Set<String> previous=new HashSet<>();
   for(int density=0;density<=100;density++){
    Set<String> selected=new HashSet<>();
    for(var socket:level.sockets()){
     boolean available=socket.type().equals("outdoor-hide")?OutdoorHiding.available(level,socket,density)
        :socket.type().equals("hide")&&LevelClosets.available(level,socket.room(),density);
     if(available)selected.add(socket.id());
    }
    check(selected.size()==HidingBudget.allowance(density),"Unequal combined availability "+level.id()+" at "+density+": "+selected);
    check(selected.containsAll(previous),"Availability must be monotonic "+level.id());previous=selected;
   }
   check(HidingBudget.allowance(0)==0,"Highest difficulty can remove all effective hiding");
  }
  for(int bad:new int[]{-1,101}){boolean rejected=false;try{HidingBudget.allowance(bad);}catch(IllegalArgumentException e){rejected=true;}check(rejected,"Invalid density");}
  System.out.println("PASS combined indoor/outdoor maximum "+HidingBudget.MAXIMUM+" on "+maps.size()+" built locations, all 101 density settings, zero-cover and monotonic selection");
  }
 }
}
