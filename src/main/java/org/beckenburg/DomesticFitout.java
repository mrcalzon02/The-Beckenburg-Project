package org.beckenburg;
import java.util.*;
import org.beckenburg.LevelDefinition.*;
/** Explicit plumbing/support coordinates, before decorative furniture consumes the wall bays. */
final class DomesticFitout {
 private DomesticFitout(){}
 static void populate(LevelPlans.B b){if(b.id.equals("house"))house(b);}
 static void add(LevelPlans.B b,String mesh,int room,double x,double z,double yaw,double hx,double hz,double height){b.floorProp(mesh,room,x,z,1,yaw,hx,hz,height);}
 static void wallKit(LevelPlans.B b,String mesh,int room,double x,double z,double yaw){b.props.add(new Prop(mesh,room,x,b.rooms.get(room).y(),z,1,yaw,0,0,2.1,"wall"));}
 static void house(LevelPlans.B b){
  // A real north-facing bath against the south wall; its tap and shower wall stays face the support.
  for(int i=0;i<b.props.size();i++){var p=b.props.get(i);if(p.room()==8&&p.mesh().equals("level_bathtub")){
   var tub=new Prop(p.mesh(),p.room(),22.9,p.y(),14.4,p.scale(),Math.PI,p.halfX(),p.halfZ(),p.height(),p.support());b.props.set(i,tub);
   b.props.add(new Prop("level_bath_shower_rail",8,tub.x(),tub.y(),tub.z(),1,tub.yaw(),0,0,2.15,"level_bathtub"));break;
  }}
  wallKit(b,"level_bathroom_wall_set",8,23.915,11.5,Math.PI/2);
  add(b,"level_laundry_hamper",8,18.55,13.6,0,.235,.20,.60);
  add(b,"level_bathroom_caddy",8,19.85,11.25,0,.22,.18,.70);
  add(b,"level_laundry_washer",10,23.50,13.7,Math.PI/2,.32,.34,.88);
  wallKit(b,"level_laundry_shelf",10,23.915,13.7,Math.PI/2);
  add(b,"level_laundry_hamper",10,23.50,12.55,0,.235,.20,.60);
 }
}
