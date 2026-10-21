package org.beckenburg;
import java.util.*;
import org.beckenburg.LevelDefinition.*;
/** Explicit plumbing/support coordinates, before decorative furniture consumes the wall bays. */
final class DomesticFitout {
 private DomesticFitout(){}
 static void populate(LevelPlans.B b){switch(b.id){case "house"->house(b);case "farmhouse"->farm(b);case "school"->school(b);default->{}}}
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
 /** A real partition has matching rendered dimensions and collision, including door headers. */
 static void partition(LevelPlans.B b,int room,double x0,double z0,double x1,double z1,double height){
  double length=Math.hypot(x1-x0,z1-z0),angle=Math.atan2(z1-z0,x1-x0),x=(x0+x1)/2,z=(z0+z1)/2,y=b.rooms.get(room).y();
  b.socket("partition",room,x,y,z,-angle,length+":"+height);b.colliders.add(new Collider(x,y,z,length/2,.06,height,angle));
 }
 static void header(LevelPlans.B b,int room,double x,double z,double width,boolean alongX,double height){
  double y=b.rooms.get(room).y();b.socket("partition",room,x,y+2.30,z,alongX?0:-Math.PI/2,width+":"+(height-2.30));b.colliders.add(new Collider(x,y+2.30,z,alongX?width/2:.06,alongX?.06:width/2,height-2.30,0));
 }
 static void floorFinish(LevelPlans.B b,int room,double x,double z,double width,double depth){b.socket("floor_finish",room,x,b.rooms.get(room).y(),z,0,width+":"+depth);}
 static void toilet(LevelPlans.B b,int room,double x,double z,double scale,double yaw){double y=b.rooms.get(room).y();b.props.add(new Prop("toilet_mp_1",room,x,y+.235*scale,z,scale,yaw,.29*scale,.45*scale,.80*scale,"floor-plumbing"));}
 static void basin(LevelPlans.B b,int room,double x,double z,double scale,double yaw){b.props.add(new Prop("level_bath_basin",room,x,b.rooms.get(room).y(),z,scale,yaw,.325*scale,.25*scale,.86*scale,"floor-plumbing"));}
 static void shower(LevelPlans.B b,int room,double x,double z,double yaw){
  double y=b.rooms.get(room).y();b.props.add(new Prop("level_bath_shower",room,x,y-.0825,z,1,yaw,0,0,2.2,"recessed-shower"));
  // The tray is recessed so its real standing surface meets the room floor exactly.
  b.socket("floor_cutout",room,x,y,z,0,".94:.89");
  for(double[] wall:new double[][]{{0,-.442,.48,.025},{.464,0,.025,.445}}){double wx=x+wall[0]*Math.cos(yaw)-wall[1]*Math.sin(yaw),wz=z+wall[0]*Math.sin(yaw)+wall[1]*Math.cos(yaw);b.colliders.add(new Collider(wx,y,wz,wall[2],wall[3],2.12,yaw));}
 }
 static void farm(LevelPlans.B b){
  b.socket("light",10,37,5.975,12.5,0,"bare-bulb");
  // Northern landing bay becomes a private washroom, leaving the stair/bedroom route to the west.
  b.props.removeIf(p->p.room()==5&&p.x()>22.45&&p.z()<15.6);
  partition(b,5,22.6,12.08,22.6,15.4,3);partition(b,5,22.6,15.4,22.8,15.4,3);partition(b,5,23.8,15.4,25.92,15.4,3);header(b,5,23.3,15.4,1,true,3);b.door(5,5,23.3,3,15.4,1,true);
  floorFinish(b,5,24.3,13.7,3.4,3.4);b.socket("service_reservation",5,24.3,3,13.7,0,"3.4:3.4");
  basin(b,5,23.25,12.48,1,0);toilet(b,5,25.2,12.58,1,0);shower(b,5,25.3,14.85,Math.PI);
  wallKit(b,"level_bathroom_wall_set",5,23.25,12.085,0);add(b,"level_laundry_hamper",5,23.0,14.05,0,.235,.20,.60);add(b,"level_bathroom_caddy",5,24.55,12.6,0,.22,.18,.70);
  b.socket("device",5,24.3,5.3,12.085,0,"SMOKE_ALARM");
  b.socket("device",5,22.685,4.20,14.65,-Math.PI/2,"LIGHT_SWITCH");String controller=b.sockets.get(b.sockets.size()-1).id();b.socket("light",5,24.3,5.975,13.7,0,"domestic@"+controller);
  add(b,"level_laundry_washer",11,33.6,24.5,0,.32,.34,.88);wallKit(b,"level_laundry_shelf",11,33.6,24.085,0);
  b.props.add(new Prop("level_yard_clothesline",12,8.5,0,7.5,1,0,0,0,2,"grounded-yard-furniture"));
  for(double x:new double[]{6.7,10.3})b.colliders.add(new Collider(x,0,7.5,.045,.045,1.96,0));
 }
 static void school(LevelPlans.B b){
  // Keep the broad west side of the upper corridor as the circulation route.
  b.props.removeIf(p->p.room()==7&&p.x()>24.5&&p.z()<28);
  for(int side=0;side<2;side++){
   double start=side==0?13:20.5,end=start+6.7,doorZ=start+1.05;int room=7;
   partition(b,room,24.8,start,27.92,start,3.6);partition(b,room,24.8,end,27.92,end,3.6);
   partition(b,room,24.8,start,24.8,doorZ-.475,3.6);partition(b,room,24.8,doorZ+.475,24.8,end,3.6);header(b,room,24.8,doorZ,.95,false,3.6);b.door(room,room,24.8,3.6,doorZ,.95,false);
   floorFinish(b,room,26.4,(start+end)/2,3.2,6.7);b.socket("service_reservation",room,26.4,3.6,(start+end)/2,0,"3.2:6.7");
   // One infant-height and one full-height handwash station; the native model remains uniformly scaled.
   basin(b,room,25.5,start+.40,.72,0);basin(b,room,26.8,start+.40,1,0);
   b.props.add(new Prop("level_bathroom_wall_set",room,25.5,3.6,start+.065,.72,0,0,0,1.5,"wall"));wallKit(b,"level_bathroom_wall_set",room,26.8,start+.065,0);
   for(int stall=0;stall<3;stall++){
    double z0=start+2+stall*1.45,z1=z0+1.45,z=(z0+z1)/2;
    partition(b,room,26.1,z0,27.92,z0,2.4);
    if(stall==2)partition(b,room,26.1,z1,27.92,z1,2.4);
    partition(b,room,26.1,z0,26.1,z-.4,2.4);partition(b,room,26.1,z+.4,26.1,z1,2.4);header(b,room,26.1,z,.8,false,2.4);b.door(room,room,26.1,3.6,z,.8,false);
    toilet(b,room,27.42,z,stall==0?.72:1,Math.PI/2);
   }
   if(side==0){b.props.add(new Prop("level_school_urinal",room,27.915,3.6,start+1.15,.75,Math.PI/2,0,0,1.0,"wall"));partition(b,room,27.15,start+.70,27.15,start+1.65,1.5);}
   b.socket("device",room,25.05,5.95,start+.065,0,"SMOKE_ALARM");
   b.socket("device",room,25.0,4.80,start+.085,0,"LIGHT_SWITCH");String controller=b.sockets.get(b.sockets.size()-1).id();
   b.socket("light",room,26.4,7.175,start+1.0,0,"utility@"+controller);b.socket("light",room,27.0,7.175,start+4.5,0,"utility@"+controller);
   b.socket("place_label",room,24.73,6.25,doorZ,-Math.PI/2,side==0?"MENS WASHROOM":"WOMENS WASHROOM");
  }
  add(b,"level_laundry_hamper",5,33.2,19.2,0,.235,.20,.60);
 }

}

