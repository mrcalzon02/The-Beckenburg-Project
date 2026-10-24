package org.beckenburg;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
/** Actual bounded fitted assemblies, native pivots and every material atlas. */
public final class HollowCreekFittedTentTest {
 public static void main(String[] args)throws Exception{
  Path reference=Path.of(args.length==0?"development/levels/verification/hollow-creek-fitted-pixels.tsv":args[0]);
  var seen=new HashSet<String>();int pixels=0,triangles=0;
  for(String line:Files.readAllLines(reference)){
   String[] p=line.split("\t");String id=p[0];
   if(!LevelAssets.FITTED_TENTS.contains(id)||!LevelAssets.approved(id))throw new AssertionError("Uncurated fitted tent "+id);
   var model=GlbProps.model(id);
   if(seen.add(id)){
    int n=model.triangles().size();if(n<100||n>=50000)throw new AssertionError("Unchanged asset budget "+id);triangles+=n;
    double low=Double.POSITIVE_INFINITY,high=Double.NEGATIVE_INFINITY;
    for(var t:model.triangles())for(var v:new GlbProps.Vertex[]{t.a(),t.b(),t.c()}){
     if(!Double.isFinite(v.x()+v.y()+v.z()+v.u()+v.v()))throw new AssertionError("Nonfinite "+id);
     low=Math.min(low,v.y());high=Math.max(high,v.y());
     if(Math.abs(v.x())>3||Math.abs(v.z())>3)throw new AssertionError("Fitted native pivot/scale "+id);
    }
    if(low<-.6||high<1||high>3)throw new AssertionError("Fitted native vertical scale "+id);
   }
   var t=model.triangles().get(Integer.parseInt(p[1]));double u=(t.a().u()+t.b().u()+t.c().u())/3,v=(t.a().v()+t.b().v()+t.c().v())/3;
   if(Math.abs(u-Double.parseDouble(p[2]))>1e-8||Math.abs(v-Double.parseDouble(p[3]))>1e-8)throw new AssertionError("Wrong fitted UV channel "+id);
   if(GlbProps.sample(t.material(),u,v)!=Integer.parseInt(p[4],16))throw new AssertionError("Wrong fitted material pixel "+id+" "+p[1]);pixels++;
  }
  if(!seen.equals(LevelAssets.FITTED_TENTS)||pixels<576)throw new AssertionError("Incomplete fitted family coverage");
  System.out.println("PASS8 actual fitted tent GLBs; "+triangles+" triangles; "+pixels+" independent material UV/pixel checks");
 }
}
