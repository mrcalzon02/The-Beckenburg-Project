package org.beckenburg;
import java.nio.file.*;
import java.util.*;
import org.beckenburg.LevelDefinition.*;
/** Independent Blender native-ray fixtures and honest route readiness audit. */
public final class HollowCreekNativePhysicsTest {
 public static void main(String[] args)throws Exception{
  var map=AuthoredSitePhysics.hollowCreek();int support=0,roofs=0,rays=0,volumes=0,clear=0,blocked=0;double maxError=0;
  var routeFailures=new ArrayList<String>();int routeTraversable=0,routeSweeps=0;Vec previous=null;String previousLabel="";
  for(String line:Files.readAllLines(Path.of("development/levels/verification/hollow-creek-native-queries.tsv"))){
   var p=line.split("\t");
   switch(p[0]){
    case "S"->{
     double x=Double.parseDouble(p[1]),z=Double.parseDouble(p[2]),expected=Double.parseDouble(p[3]);var hit=map.support(x,z,expected,.1);
     if(hit==null||Math.abs(hit.y()-expected)>.00005)throw new AssertionError("Native support disagreement "+line+" got "+hit);
     maxError=Math.max(maxError,Math.abs(hit.y()-expected));support++;
     if(p[4].startsWith("L")){
      Vec feet=new Vec(x,hit.y(),z);
      if(map.traversable(feet,HunterPosture.STANDING_HEIGHT))routeTraversable++;
      else routeFailures.add("standing capsule at "+p[4]+" "+feet);
      if(p[4].equals(previousLabel)&&previous!=null&&Math.hypot(x-previous.x(),z-previous.z())<.31&&Math.hypot(x-previous.x(),z-previous.z())>1e-6){
       if(map.resolveMove(previous,x,z,HunterPosture.STANDING_HEIGHT)==null)routeFailures.add("sweep at "+p[4]+" "+previous+" -> "+feet);else routeSweeps++;
      }
      previous=feet;previousLabel=p[4];
     }
    }
    case "N"->{if(map.support(Double.parseDouble(p[1]),Double.parseDouble(p[2]),Double.parseDouble(p[3]),.05)!=null)throw new AssertionError("Canopy became walking support "+line);roofs++;}
    case "L"->{
     Vec a=new Vec(Double.parseDouble(p[1]),Double.parseDouble(p[2]),Double.parseDouble(p[3])),b=new Vec(Double.parseDouble(p[4]),Double.parseDouble(p[5]),Double.parseDouble(p[6]));boolean expected=p[7].equals("1");
     if(map.clearSight(a,b)!=expected)throw new AssertionError("Actual native optical disagreement "+line);
     rays++;if(expected)clear++;else blocked++;
    }
    case "B"->{if(!map.bodyBlocked(new Vec(Double.parseDouble(p[1]),Double.parseDouble(p[2]),Double.parseDouble(p[3])),Double.parseDouble(p[4]),.22))throw new AssertionError("Accepted body inside actual native bank "+line);volumes++;}
    default->throw new AssertionError("Unknown native oracle record");
   }
  }
  if(support<1000||roofs!=8||rays<300||clear==0||blocked==0||volumes<10)throw new AssertionError("Incomplete actual native query coverage");
  for(var zone:HollowCreekContract.zones()){
   var anchor=new Vec(zone.x(),zone.y(),zone.z());var at=map.support(zone.x(),zone.z(),zone.y(),.02);
   if(at==null||Math.abs(at.y()-zone.y())>.00005||!map.traversable(anchor,HunterPosture.STANDING_HEIGHT))throw new AssertionError("Unsafe native zone anchor "+zone.key()+" "+anchor);
  }
  if(!routeFailures.isEmpty())throw new AssertionError("Authored route capsule regression: "+routeFailures);
  System.out.println("PASS "+HollowCreekContract.zones().size()+" actual native standing zone anchors");
  Path out=Path.of("build/hollow-creek-runtime-route-findings.txt");Files.write(out,routeFailures);
  if(Arrays.asList(args).contains("--report")){
   String failures=routeFailures.stream().map(s->"\""+s.replace("\\","\\\\").replace("\"","\\\"")+"\"").collect(java.util.stream.Collectors.joining(","));
   String json="{\n  \"status\":\""+(routeFailures.isEmpty()?"pass-native-query-and-route-component":"query-pass-route-gate-open")+"\",\n  \"binary_sha256\":\""+map.sha256()+"\",\n  \"source_sha256\":\""+HollowCreekContract.SOURCE_SHA256+"\",\n  \"triangles\":"+map.triangleCount()+",\n  \"components\":"+map.componentCount()+",\n  \"support_probes\":"+support+",\n  \"max_support_error_m\":"+maxError+",\n  \"nonwalkable_roofs\":"+roofs+",\n  \"optical_rays\":"+rays+",\n  \"clear_rays\":"+clear+",\n  \"blocked_rays\":"+blocked+",\n  \"actual_bank_interiors\":"+volumes+",\n  \"standing_route_samples\":"+routeTraversable+",\n  \"successful_route_sweeps\":"+routeSweeps+",\n  \"route_findings\":["+failures+"],\n  \"world_renderer_or_mission_unlock\":false\n}\n";
   Files.writeString(Path.of("development/levels/verification/hollow-creek-native-runtime.json"),json);
  }
  System.out.println("PASS native query fidelity: "+support+" support points, "+roofs+" no-roof-support cases, "+rays+" optical rays ("+clear+" clear/"+blocked+" blocked), "+volumes+" actual bank interiors; max support error "+maxError+"m");
  System.out.println("ROUTE READINESS: "+routeTraversable+" standing source-route points; "+routeSweeps+" source-route sweeps pass; "+routeFailures.size()+" unresolved capsule/sweep findings in "+out+". This is not full map acceptance.");
 }
}
