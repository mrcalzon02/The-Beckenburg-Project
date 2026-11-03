package org.beckenburg;

import java.util.Map;
import java.util.List;

/** Source-size domestic/service fixtures with native support and matching world footprints. */
public final class CastleGround {
    private CastleGround(){}
    private static long activeRevision=Long.MIN_VALUE;
    private static boolean active;
    /** A saved older/custom scene without the new partitions retains its original room owners. */
    public static boolean active(){
        var scene=CastleLevelEdits.current();
        if(activeRevision!=scene.revision()){
            active=scene.surfaces().stream().anyMatch(s->s.id().startsWith("ground-wall")&&s.kind()==CastleLevelEdits.Kind.WALL);
            activeRevision=scene.revision();
        }
        return active;
    }
    public static final List<CastleRooms.Room> LEGACY_ROOMS=List.of(
        new CastleRooms.Room("armory","Armory",1,1,10.5,11.5,0,3.4,false,MaterialAtlas.PARQUET),
        new CastleRooms.Room("contracts","Briefing and contracts",10.5,1,19.5,11.5,0,3.4,false,MaterialAtlas.CARPET),
        new CastleRooms.Room("wardrobe","Wardrobe and lounge",19.5,1,28.5,11.5,0,3.4,false,MaterialAtlas.PARQUET),
        new CastleRooms.Room("practice","Practice and common room",1,11.5,10.5,34,0,3.4,false,MaterialAtlas.STONE),
        new CastleRooms.Room("court","Arrival courtyard",10.5,11.5,20.5,34,0,Double.NaN,true,MaterialAtlas.STONE),
        new CastleRooms.Room("operations","Expedition operations",20.5,11.5,28.5,34,0,3.4,false,MaterialAtlas.PARQUET),
        new CastleRooms.Room("archive","Common archive",28.5,4,43,17.5,0,3.4,false,MaterialAtlas.PARQUET),
        new CastleRooms.Room("store","Quartermaster store",28.5,17.5,43,34,0,3.4,false,MaterialAtlas.STONE),
        new CastleRooms.Room("gate","Gate threshold",13.2,34,17.8,36,0,Double.NaN,true,MaterialAtlas.STONE));
    private static void prop(Map<String,CastleLevelEdits.Prop> out,String id,String mesh,
                             double x,double z,double yaw){
        double[] b=SceneRenderer.castleBounds(mesh);double c=Math.abs(Math.cos(yaw)),s=Math.abs(Math.sin(yaw));
        out.put(id,new CastleLevelEdits.Prop(id,mesh,x,0,z,1,yaw,(b[3]*c+b[5]*s)/2,(b[3]*s+b[5]*c)/2,false));
    }
    public static void installDefaults(Map<String,CastleLevelEdits.Prop> props){
        prop(props,"ground-wc-toilet","toilet_mp_1",20.7,1.9,0);
        prop(props,"ground-wc-basin","level_bath_basin",22.85,3.2,Math.PI/2);
        prop(props,"ground-wash-basin-west","level_bath_basin",24.1,3.6,-Math.PI/2);
        prop(props,"ground-wash-basin-east","level_bath_basin",27.8,3.6,Math.PI/2);
        prop(props,"ground-wash-shower-west","level_bath_shower_worn",24.6,2,0);
        prop(props,"ground-wash-shower-east","level_bath_shower_worn",27,2,0);
        prop(props,"ground-kitchen-counter-south","level_kitchen_counter",2.6,33.2,0);
        prop(props,"ground-kitchen-counter-west","level_kitchen_counter",1.65,30.5,Math.PI/2);
        prop(props,"ground-washup-counter","level_kitchen_counter",8.5,30.35,0);
        prop(props,"ground-repair-bench","level_workbench_worn",38.1,18.7,Math.PI/2);
        prop(props,"ground-repair-preparation","level_workbench",41.7,25,0);
        prop(props,"ground-infirmary-bed","bed_5",39.9,29,Math.PI/2);
        prop(props,"ground-infirmary-basin","level_bath_basin",42,27.3,Math.PI/2);
        prop(props,"ground-infirmary-preparation","dinner_table_1",38.3,32.6,0);
    }
}
