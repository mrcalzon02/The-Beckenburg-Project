package org.beckenburg;
/** Physical navigation acceptance through the editor's shared runtime swept-capsule audit. */
public final class AuthoredLevelReachabilityTest {
 public static void main(String[] args){
  boolean blackpine=args.length>0&&args[0].equals("BLACKPINE");
  boolean rebuild=args.length>0&&args[0].equals("HOUSE_REBUILD");
  HouseMap.Site[] sites=blackpine||rebuild?new HouseMap.Site[]{HouseMap.Site.HOUSE}:args.length==0?new HouseMap.Site[]{HouseMap.Site.HOUSE,HouseMap.Site.FARMHOUSE,HouseMap.Site.SCHOOL}:new HouseMap.Site[]{HouseMap.Site.valueOf(args[0])};
  for(HouseMap.Site site:sites){
   HouseMap map=rebuild?HouseRebuild.graybox():blackpine?BlackpineLevel.map():new HouseMap(site);
   var result=LevelTraversalAudit.inspect(map);
   System.out.println(result.level()+" reached elevations "+result.lowest()+".."+result.highest());
   for(var finding:result.findings())System.out.println(finding.code()+" "+site+" room="+finding.room()+" "+finding.detail());
   if(!result.passed())throw new AssertionError(site+" "+result.findings().size()+" unreachable goals, visited="+result.positions());
   System.out.println("PASS "+result.level()+": swept van route reaches all "+map.level().rooms().size()+" zones and all functional sockets, "+result.positions()+" positions");
  }
 }
}
