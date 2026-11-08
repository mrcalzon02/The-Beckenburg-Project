package org.beckenburg;

import java.util.ArrayList;
import org.beckenburg.LevelDefinition.*;

/** Measured major-fixture fit pilot. The empty graybox and dispatched House remain separate. */
public final class HouseRebuildFittings {
    private HouseRebuildFittings(){}
    public static HouseMap map(){return new HouseMap(HouseMap.Site.HOUSE,source());}
    public static LevelDefinition source(){
        var base=HouseRebuild.source();
        var props=new ArrayList<>(base.props());
        var solids=new ArrayList<>(base.colliders());
        var portals=new ArrayList<>(base.portals());
        // A full-size cooking run requires a deliberately positioned dining entrance.
        for(int i=0;i<portals.size();i++){
            var p=portals.get(i);
            if(p.door()&&p.joins(2,3))portals.set(i,new Portal(p.id(),p.a(),p.b(),p.x(),p.y(),10.85,p.width(),p.alongX(),p.kind(),p.loss()));
        }
        fit(base,props,solids,"level_kitchen_counter",2,17.565,12.95,Math.PI/2);
        fit(base,props,solids,"level_bathtub",8,21.13,11.5,Math.PI/2);
        fit(base,props,solids,"toilet_mp_1",8,18.47,10.65,0);
        fit(base,props,solids,"level_bath_basin",8,19.85,10.34,0);
        fit(base,props,solids,"level_laundry_washer",10,19,10.49,0);
        return new LevelDefinition(base.id(),"first-principles-fixture-fit-v1",base.title().replace("graybox","fixture fit"),
            base.rooms(),portals,base.stairs(),base.voids(),props,base.sockets(),solids,base.dock(),base.ambience(),base.water(),base.sounds());
    }
    private static void fit(LevelDefinition base,ArrayList<Prop> props,ArrayList<Collider> solids,
            String mesh,int room,double cx,double cz,double yaw){
        double[] b=SceneRenderer.castleBounds(mesh);
        double c=Math.cos(yaw),s=Math.sin(yaw),floor=base.floorY(room);
        // Preserve the source GLB pivot, UVs, proportions and physical metre scale.
        props.add(new Prop(mesh,room,cx-(b[0]*c-b[2]*s),floor-b[1],cz-(b[0]*s+b[2]*c),1,yaw,b[3]/2,b[5]/2,b[4],"floor"));
        solids.add(new Collider(cx,floor,cz,b[3]/2,b[5]/2,b[4],yaw));
    }
}
