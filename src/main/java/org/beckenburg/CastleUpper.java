package org.beckenburg;

import java.util.*;

/** Native upper-west construction, six source-size lodging beds and supported household services. */
public final class CastleUpper {
    private CastleUpper(){}
    public static final int LIGHT_COUNT=24;
    private static long activeRevision=Long.MIN_VALUE;
    private static boolean active;
    public static boolean active(){
        var scene=CastleLevelEdits.current();
        if(activeRevision!=scene.revision()){
            active=scene.surfaces().stream().anyMatch(s->s.id().startsWith("upper-floor-")
                &&s.kind()==CastleLevelEdits.Kind.FLOOR&&s.base()==3.4);
            activeRevision=scene.revision();
        }
        return active;
    }
    private static final Set<String> ROOM_IDS=CastleUpperLayout.ROOMS.stream().map(CastleRooms.Room::id).filter(id->!id.equals("library")).collect(java.util.stream.Collectors.toUnmodifiableSet());
    public static boolean isRoom(String id){return ROOM_IDS.contains(id);}
    public static boolean privateDoor(CastleDoorSlots.Slot slot){
        if(slot.bottom()!=3.4)return false;
        return slot.x0()==7.5&&slot.x1()==7.5&&(slot.centerZ()==4||slot.centerZ()==15||slot.centerZ()==22||slot.centerZ()==29)
            ||slot.horizontal()&&slot.centerZ()==7&&slot.centerX()==2.5;
    }
    public static final List<CastleHubDecor.Switch> SWITCHES=switches();
    private static List<CastleHubDecor.Switch> switches(){
        var out=new ArrayList<CastleHubDecor.Switch>();
        out.add(new CastleHubDecor.Switch(7.73,4.6,6.5,-Math.PI/2));
        for(double z:new double[]{16.5,23.5,30.5})out.add(new CastleHubDecor.Switch(7.27,4.6,z,Math.PI/2));
        out.add(new CastleHubDecor.Switch(7.27,4.6,5.5,Math.PI/2));
        out.add(new CastleHubDecor.Switch(3.77,4.6,9.1,Math.PI/2));
        out.add(new CastleHubDecor.Switch(10.73,4.6,13,-Math.PI/2));
        return List.copyOf(out);
    }
    private static void prop(Map<String,CastleLevelEdits.Prop> props,String id,String mesh,double x,double z,double yaw){
        var b=SceneRenderer.castleBounds(mesh);double c=Math.abs(Math.cos(yaw)),s=Math.abs(Math.sin(yaw));
        props.put(id,new CastleLevelEdits.Prop(id,mesh,x,3.4,z,1,yaw,(b[3]*c+b[5]*s)/2,(b[3]*s+b[5]*c)/2,true));
    }
    private static void lamp(Map<String,CastleLevelEdits.Lamp> lamps,String room,int i,double x,double z){
        String id="upper-light-"+room+"-"+i;
        lamps.put(id,new CastleLevelEdits.Lamp(id,"ceiling_lamp_1_on","ceiling_lamp_1_off",x,6.8-CastleRooms.SLAB,z,
            .7,0,CastleHubDecor.Mount.CEILING,7,3.4));
    }
    private static void wallLamp(Map<String,CastleLevelEdits.Lamp> lamps,int i,double x,double y,double z,double yaw,double radius,double power){
        String id="upper-light-ramparts-"+i;
        lamps.put(id,new CastleLevelEdits.Lamp(id,"lamp_3_on","lamp_3_off",x,y,z,.45,yaw,CastleHubDecor.Mount.WALL,radius,power));
    }
    private static void beam(Map<String,CastleLevelEdits.Surface> surfaces,String id,double x0,double x1,double z){
        surfaces.put(id,new CastleLevelEdits.Surface(id,CastleLevelEdits.Kind.WALL,x0,z,x1,z,6.22,.36,MaterialAtlas.AGED_WOOD));
    }
    public static void installDefaults(Map<String,CastleLevelEdits.Surface> surfaces,Map<String,CastleLevelEdits.Prop> props,
                                        Map<String,CastleLevelEdits.Lamp> lamps){
        for(var r:CastleUpperLayout.ROOMS)if(CastleUpperLayout.WEST_IDS.contains(r.id())){
            String id="upper-floor-"+r.id();
            surfaces.put(id,new CastleLevelEdits.Surface(id,CastleLevelEdits.Kind.FLOOR,r.x0(),r.z0(),r.x1(),r.z1(),3.4,CastleRooms.SLAB,r.material()));
            id="upper-ceiling-"+r.id();
            surfaces.put(id,new CastleLevelEdits.Surface(id,CastleLevelEdits.Kind.FLOOR,r.x0(),r.z0(),r.x1(),r.z1(),6.8,CastleRooms.SLAB,MaterialAtlas.CEILING));
            if(r.id().startsWith("lodging-")){
                double middle=(r.z0()+r.z1())/2;
                prop(props,"upper-wardrobe-"+r.id(),"wardrobe_mp_1",6.65,r.z0()+1.1,Math.PI/2);
                prop(props,"upper-desk-"+r.id(),"level_student_desk",1.75,middle,-Math.PI/2);
                prop(props,"upper-chair-"+r.id(),"chair_mp_5",2.7,middle,Math.PI/2);
                beam(surfaces,"upper-beam-"+r.id(),1,7.5,middle);
                lamp(lamps,r.id(),0,4.5,r.z0()+2);lamp(lamps,r.id(),1,4.5,r.z0()+5);
            }
        }
        int n=0;
        for(var bed:CastleUpperLayout.BEDS){
            prop(props,"upper-bed-"+n,"bed_5",bed.x(),bed.z(),0);
            prop(props,"upper-bedside-"+n++,"bedside_table_1",2,bed.z(),0);
        }
        surfaces.put("upper-floor-north-west-link",new CastleLevelEdits.Surface("upper-floor-north-west-link",CastleLevelEdits.Kind.FLOOR,
            10.5,11.5,12.5,12,3.4,CastleRooms.SLAB,MaterialAtlas.STONE));
        n=0;for(double z:new double[]{3,7,11,15,19,23,27,31})lamp(lamps,"res-hall",n++,9,z);
        n=0;for(double z:new double[]{6,12,18,24,30})beam(surfaces,"upper-beam-hall-"+n++,7.5,10.5,z);
        beam(surfaces,"upper-beam-wash",1,7.5,4);
        prop(props,"upper-shower-west","level_bath_shower_worn",2.3,2,0);
        prop(props,"upper-shower-east","level_bath_shower_worn",5.6,2,0);
        prop(props,"upper-basin-west","level_bath_basin",2.2,5.8,Math.PI);
        prop(props,"upper-basin-east","level_bath_basin",5.8,5.8,Math.PI);
        prop(props,"upper-wc-toilet","toilet_mp_1",2.6,10,Math.PI);
        prop(props,"upper-wc-basin","level_bath_basin",1.65,8.5,-Math.PI/2);
        n=0;
        for(double z:new double[]{16,24,30})wallLamp(lamps,n++,10.73,5.2,z,-Math.PI/2,8,3.4);
        for(double z:new double[]{14,28}){
            String id="upper-rampart-pier-"+n;
            surfaces.put(id,new CastleLevelEdits.Surface(id,CastleLevelEdits.Kind.WALL,20.95,z-.1,20.95,z+.1,3.4,2.3,MaterialAtlas.IRON));
            wallLamp(lamps,n++,20.72,4.9,z,Math.PI/2,8,3.4);
        }
        surfaces.put("upper-rampart-pier-south",new CastleLevelEdits.Surface("upper-rampart-pier-south",CastleLevelEdits.Kind.WALL,15,32.75,15.2,32.75,3.4,2.3,MaterialAtlas.IRON));
        wallLamp(lamps,n++,15.1,4.9,32.52,0,8,3.4);
        wallLamp(lamps,n,17.5,6.3,11.73,Math.PI,10,4);
        lamp(lamps,"upper-wash",0,3,3);lamp(lamps,"upper-wash",1,5.5,5.5);lamp(lamps,"upper-wc",0,2.5,9.2);
    }
    public static String lampRoom(String id){
        if(id.startsWith("upper-light-ramparts-"))return "ramparts";
        for(var r:CastleUpperLayout.ROOMS)if(id.startsWith("upper-light-"+r.id()+"-"))return r.id();
        return null;
    }
}
