package org.beckenburg;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
/** Curated physical fabric assets and every material through the actual reader. */
public final class HollowCreekTailoredFabricTest {
 public static void main(String[] args)throws Exception{
  var seen=new HashSet<String>();int count=0,total=0;
  for(String line:Files.readAllLines(Path.of(args[0]))){
   String[] p=line.split("\t");String id=p[0];
   if(!LevelAssets.TAILORED_FABRIC.contains(id)||!LevelAssets.approved(id))throw new AssertionError("Uncurated fabric "+id);
   var m=GlbProps.model(id);
   if(seen.add(id)){
    if(m.triangles().size()>=50000)throw new AssertionError("Triangle budget "+id);
    total+=m.triangles().size();
    for(var t:m.triangles())for(var v:new GlbProps.Vertex[]{t.a(),t.b(),t.c()})if(!Double.isFinite(v.x()+v.y()+v.z()+v.u()+v.v()))throw new AssertionError("Nonfinite geometry/UV "+id);
   }
   var t=m.triangles().get(Integer.parseInt(p[1]));double u=(t.a().u()+t.b().u()+t.c().u())/3,v=(t.a().v()+t.b().v()+t.c().v())/3;
   if(Math.abs(u-Double.parseDouble(p[2]))>1e-8||Math.abs(v-Double.parseDouble(p[3]))>1e-8)throw new AssertionError("Wrong UV channel "+id);
   if(GlbProps.sample(t.material(),u,v)!=Integer.parseInt(p[4],16))throw new AssertionError("Wrong atlas orientation/color "+id+" "+p[1]);count++;
  }
  if(!seen.equals(LevelAssets.TAILORED_FABRIC)||count<216)throw new AssertionError("Incomplete fabric coverage");
  System.out.println("PASS3 tailored fabric GLBs; "+total+" triangles; "+count+" independent material UV/pixel samples");
 }
}
