package org.beckenburg;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
/** Actual new portable site assets, texture UV origin and external PNG samples. */
public final class HollowCreekSiteArtTest {
 public static void main(String[] args)throws Exception{
  var seen=new HashSet<String>();int pixels=0;
  for(String line:Files.readAllLines(Path.of("development/levels/verification/hollow-creek-siteart-pixels.tsv"))){
   String[] p=line.split("\t");String id=p[0];
   if(!LevelAssets.SITE_ART.contains(id)||!LevelAssets.approved(id))throw new AssertionError("Uncurated site art "+id);
   var model=GlbProps.model(id);seen.add(id);
   if(model.triangles().isEmpty()||model.triangles().size()>=50000)throw new AssertionError("Asset budget "+id);
   var t=model.triangles().get(Integer.parseInt(p[1]));double u=(t.a().u()+t.b().u()+t.c().u())/3,v=(t.a().v()+t.b().v()+t.c().v())/3;
   if(Math.abs(u-Double.parseDouble(p[2]))>1e-8||Math.abs(v-Double.parseDouble(p[3]))>1e-8)throw new AssertionError("UV channel "+id);
   if(GlbProps.sample(t.material(),u,v)!=Integer.parseInt(p[4],16))throw new AssertionError("Material pixel "+id+" "+p[1]);pixels++;
  }
  var expected=new HashSet<>(LevelAssets.SITE_ART);expected.remove("level_camp_memorial_uv_tracks");
  if(!seen.equals(expected)||pixels<1000)throw new AssertionError("Incomplete portable site family coverage");
  var marks=GlbProps.model("level_camp_memorial_uv_tracks");if(marks.triangles().size()!=60)throw new AssertionError("Five surface-mounted cube carriers");
  var materials=new HashSet<Integer>();for(var t:marks.triangles())materials.add(t.material());
  int transparent=0,footprint=0;for(int material:materials){
   if(GlbProps.sampleAlpha(material,.5,.55)==0)transparent++;
   else if(GlbProps.sampleAlpha(material,.02,.02)==0)footprint++;
  }
  if(transparent!=1||footprint!=1)throw new AssertionError("Outward mask and invisible carrier alpha");
  System.out.println("PASS14 site assets; "+pixels+" independent UV/material pixel checks; five separate cosmetic cube carriers");
 }
}
