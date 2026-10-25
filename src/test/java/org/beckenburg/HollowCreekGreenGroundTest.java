package org.beckenburg;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
/** Actual new bank and rigidly refitted crest exports with original PNG pixels. */
public final class HollowCreekGreenGroundTest {
 public static void main(String[] args)throws Exception{
  var seen=new HashSet<String>();int samples=0,triangles=0;
  for(String line:Files.readAllLines(Path.of("development/levels/verification/hollow-creek-green-pixels.tsv"))){
   var p=line.split("\t");String id=p[0];
   if(!LevelAssets.GREEN_GROUND.contains(id)||!LevelAssets.approved(id))throw new AssertionError("Uncurated bank "+id);
   var model=GlbProps.model(id);
   if(seen.add(id)){
    if(model.triangles().isEmpty()||model.triangles().size()>=50000)throw new AssertionError("Original loader budget "+id);
    triangles+=model.triangles().size();
    for(var t:model.triangles())for(var v:new GlbProps.Vertex[]{t.a(),t.b(),t.c()})if(!Double.isFinite(v.x()+v.y()+v.z()+v.u()+v.v()))throw new AssertionError("Nonfinite bank "+id);
   }
   var t=model.triangles().get(Integer.parseInt(p[1]));double u=(t.a().u()+t.b().u()+t.c().u())/3,v=(t.a().v()+t.b().v()+t.c().v())/3;
   if(Math.abs(u-Double.parseDouble(p[2]))>1e-8||Math.abs(v-Double.parseDouble(p[3]))>1e-8)throw new AssertionError("Berm original UV channel "+id);
   if(GlbProps.sample(t.material(),u,v)!=Integer.parseInt(p[4],16))throw new AssertionError("Berm source PNG pixel "+id+" "+p[1]);samples++;
  }
  if(!seen.equals(LevelAssets.GREEN_GROUND)||samples<1152)throw new AssertionError("Incomplete boundary source coverage");
  System.out.println("PASS actual green grass ground/bank/shore GLB; "+triangles+" triangles; "+samples+" independent UV/PNG pixel checks");
 }
}
