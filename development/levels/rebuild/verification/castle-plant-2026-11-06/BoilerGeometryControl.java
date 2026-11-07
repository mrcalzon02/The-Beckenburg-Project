package org.beckenburg;
public class BoilerGeometryControl {
 public static void main(String[] args){for(String id:new String[]{"level_boiler_modern","level_castle_plant_boiler"}){int zero=0;double total=0;for(var t:GlbProps.model(id).triangles()){
  double ax=t.b().x()-t.a().x(),ay=t.b().y()-t.a().y(),az=t.b().z()-t.a().z(),bx=t.c().x()-t.a().x(),by=t.c().y()-t.a().y(),bz=t.c().z()-t.a().z();double x=ay*bz-az*by,y=az*bx-ax*bz,z=ax*by-ay*bx,a=Math.sqrt(x*x+y*y+z*z)/2;
  if(a<1e-12)zero++;total+=a;
 }System.out.println(id+" triangles="+GlbProps.model(id).triangles().size()+" degenerate="+zero+" area="+total);}}
}
