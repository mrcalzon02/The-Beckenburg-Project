import java.nio.file.*;
import java.util.*;
import org.beckenburg.AuthoredSitePhysics;
import org.beckenburg.LevelDefinition.Vec;
/** Horizontal facade optical queries in the unchanged production native kernel. */
public final class RebuildFacadeOpticalProbe {
 public static void main(String[] args)throws Exception {
  Map<String,AuthoredSitePhysics[]> maps=new LinkedHashMap<>();Map<String,int[]> results=new LinkedHashMap<>();
  for(String line:Files.readAllLines(Path.of(args[0]))){
   String[] p=line.split("\t");if(p.length!=11)throw new IllegalArgumentException("roof input columns");
   if(!maps.containsKey(p[0])){var pair=new AuthoredSitePhysics[2];try(var in=Files.newInputStream(Path.of(p[1]))){pair[0]=AuthoredSitePhysics.read(in,p[2],List.of());}try(var in=Files.newInputStream(Path.of(p[3]))){pair[1]=AuthoredSitePhysics.read(in,p[4],List.of());}maps.put(p[0],pair);results.put(p[0],new int[3]);}
   double x=Double.parseDouble(p[5]),y=Double.parseDouble(p[6]),z=Double.parseDouble(p[7]);var a=new Vec(x,y,z);var b=new Vec(Double.parseDouble(p[8]),Double.parseDouble(p[9]),Double.parseDouble(p[10]));var pair=maps.get(p[0]);var r=results.get(p[0]);r[0]++;if(pair[0].clearSight(a,b))r[1]++;if(pair[1].clearSight(a,b))r[2]++;
  }
  var out=new ArrayList<String>();out.add("site\tprobes\tbefore_clear\tafter_clear");
  for(var e:results.entrySet()){var r=e.getValue();out.add(e.getKey()+"\t"+r[0]+"\t"+r[1]+"\t"+r[2]);if(r[2]!=0||r[1]==0)throw new AssertionError(e.getKey()+" missing optical closure/control "+Arrays.toString(r));}
  Files.write(Path.of(args[1]),out);out.forEach(System.out::println);
 }
}
