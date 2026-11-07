package org.beckenburg;

import java.util.Map;

/** Source-size equipment on actual cellar floor and masonry; native props own collision. */
public final class CastlePlant {
    private CastlePlant(){}
    private static void prop(Map<String,CastleLevelEdits.Prop> out,String id,String mesh,
                             double x,double y,double z,double yaw){
        double[] b=SceneRenderer.castleBounds(mesh);double c=Math.abs(Math.cos(yaw)),s=Math.abs(Math.sin(yaw));
        out.put(id,new CastleLevelEdits.Prop(id,mesh,x,y,z,1,yaw,
            (b[3]*c+b[5]*s)/2,(b[3]*s+b[5]*c)/2,false));
    }
    public static void installDefaults(Map<String,CastleLevelEdits.Prop> props){
        prop(props,"plant-boiler","level_castle_plant_boiler",42.265,-3,28.2,Math.PI/2);
        prop(props,"plant-boiler-connections","level_castle_plant_connections",42.5425,-.921,28.2,Math.PI/2);
        prop(props,"plant-distribution","level_electrical_bank",37.395,-2.75,32.2,-Math.PI/2);
    }
}
