package org.beckenburg;

import java.util.List;
import java.util.Map;

/** Retained archive-gallery exhibits; native authored props own support and collision. */
public final class CastleMuseum {
    private CastleMuseum(){}
    public record Exhibit(String id,String mesh,double x,double y,double z,double yaw,String story){}
    public static final List<Exhibit> EXHIBITS=List.of(
        new Exhibit("museum-armour-archive","level_castle_armour_display",31.5,0,5.5,0,
            "Retained composite field-harness display, reassembled during west-wing restoration"),
        new Exhibit("museum-armour-caption","level_castle_armour_label",32.3,0,6.0,0,
            "Original collection label retained by the base custodians"),
        new Exhibit("museum-armour-barrier","level_castle_museum_rope",31.5,0,6.45,0,
            "Retained velvet barrier protects the reconstructed museum display without closing the gallery"),
        new Exhibit("heritage-common-fireplace","level_castle_great_fireplace",7.6,0,22.66,0,
            "Restored cold limestone fireplace; retained ashlar, original collection heraldry and capped historic heating context"),
        new Exhibit("heritage-archive-tapestry","level_castle_tapestry",36.5,.55,4.2625,0,
            "Collection hanging conserved with modern support sleeve; original heraldic weave retained above archive circulation"),
        new Exhibit("heritage-restoration-painting","level_castle_restoration_painting",42.75922,1.2,11,Math.PI/2,
            "Retained picture of the western tower before reroofing; worn original frame reused by the archive"),
        new Exhibit("heritage-library-rug","level_castle_library_rug",14.6,3.4,9.2,0,
            "Conserved household carpet returned to the upper reading room as a working furnishing"));
    public static void installDefaults(Map<String,CastleLevelEdits.Prop> props){
        for(var e:EXHIBITS){
            double[] b=SceneRenderer.castleBounds(e.mesh());double c=Math.abs(Math.cos(e.yaw())),s=Math.abs(Math.sin(e.yaw()));
            props.put(e.id(),new CastleLevelEdits.Prop(e.id(),e.mesh(),e.x(),e.y(),e.z(),1,e.yaw(),
                (b[3]*c+b[5]*s)/2,(b[3]*s+b[5]*c)/2,e.y()>=3.4));
        }
    }
}
