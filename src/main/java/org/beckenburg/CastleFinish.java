package org.beckenburg;

import java.util.*;

/** Final household and collection-store furnishings; native props own every pose. */
public final class CastleFinish {
    private CastleFinish(){}
    public record Fitting(String id,String mesh,double x,double y,double z,double yaw,String room,String purpose){}
    public static final List<Fitting> FITTINGS=List.of(
        new Fitting("finish-vault-case-west","display_cabinet_mp_2",30.5,-3,33.25,Math.PI,"vault","Retained museum collection storage; no invented captured trophies"),
        new Fitting("finish-vault-case-east","display_cabinet_mp_2",35,-3,33.25,Math.PI,"vault","Conservation collection cabinet"),
        new Fitting("finish-vault-shelf","shelf_mp_6",29.1,-3,30.8,-Math.PI/2,"vault","Low conservation supply shelf"),
        new Fitting("finish-vault-trunk","level_lived_trunk",29.5,-3,27.5,0,"vault","Closed collection transport chest"),
        new Fitting("finish-vault-crate","wooden_crate_1",35.5,-3,27.2,0,"vault","Supported restoration packing crate"),
        new Fitting("finish-vault-table","dinner_table_1",33,-3,30,0,"vault","Clear accession and conservation work surface"),
        new Fitting("finish-kitchen-sideboard","level_lived_sideboard",5.7,0,31.7,Math.PI/2,"kitchen","Household serving and food storage"),
        new Fitting("finish-kitchen-trunk","level_lived_trunk",5.6,0,33.1,0,"kitchen","Closed household provisions; entry switch approach remains clear"),
        new Fitting("finish-dining-chair-north","chair_mp_5",4.55,0,23.4,0,"common","Common dining seat; clear table approach beside chair"),
        new Fitting("finish-dining-chair-south","chair_mp_5",5.1,0,25.6,Math.PI,"common","Common dining seat"),
        new Fitting("finish-dining-chair-west","chair_mp_5",3.75,0,24.5,-Math.PI/2,"common","Common dining seat"),
        new Fitting("finish-dining-chair-east","chair_mp_5",6.45,0,24.5,Math.PI/2,"common","Common dining seat"),
        new Fitting("finish-lodging-trunk-1","level_lived_trunk",6.6,3.4,17.5,0,"lodging-1","Resident travelling chest at end of lodging"),
        new Fitting("finish-lodging-trunk-2","level_lived_trunk",6.6,3.4,24.5,0,"lodging-2","Resident travelling chest at end of lodging"),
        new Fitting("finish-lodging-trunk-3","level_lived_trunk",6.6,3.4,31.5,0,"lodging-3","Resident travelling chest at end of lodging"),
        new Fitting("finish-keeper-statue","level_castle_keeper_statue",40.2,0,15.8,Math.PI,"archive","Retained limestone keeper with codex; continuous carving repaired during restoration"),
        new Fitting("finish-keeper-barrier","level_castle_museum_rope",40.2,0,14.7,0,"archive","Collection barrier; central archive remains open"));
    public static void installDefaults(Map<String,CastleLevelEdits.Prop> props){
        // Refit the retained small common-room table for four dining places.
        for(var e:new ArrayList<>(props.entrySet())){
            var p=e.getValue();
            if(p.mesh().equals("dinner_table_1")&&Math.abs(p.x()-5.1)<.01&&Math.abs(p.z()-24.9)<.01)
                put(props,p.id(),p.mesh(),5.1,0,24.5,0);
        }
        for(var f:FITTINGS)put(props,f.id(),f.mesh(),f.x(),f.y(),f.z(),f.yaw());
    }
    private static void put(Map<String,CastleLevelEdits.Prop> props,String id,String mesh,double x,double y,double z,double yaw){
        var b=SceneRenderer.castleBounds(mesh);double c=Math.abs(Math.cos(yaw)),s=Math.abs(Math.sin(yaw));
        props.put(id,new CastleLevelEdits.Prop(id,mesh,x,y,z,1,yaw,(b[3]*c+b[5]*s)/2,(b[3]*s+b[5]*c)/2,y>=3.4));
    }
}
