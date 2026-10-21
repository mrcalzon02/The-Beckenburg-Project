package org.beckenburg;
import java.util.*;
import org.beckenburg.LevelDefinition.*;
/** Small individually committed model-production passes, placed in their real working context. */
public final class BlackpineProductionAssets {
    private BlackpineProductionAssets(){}
    public static final Set<String> ASSETS=Set.of("level_work_bandsaw");
    public static LevelDefinition apply(LevelDefinition l){
        var props=new ArrayList<>(l.props());var colliders=new ArrayList<>(l.colliders());
        add(props,colliders,"level_work_bandsaw",0,42.6,33.8,0,1.48,.74,3.28);
        return new LevelDefinition(l.id(),"blackpine-model-passes-v1",l.title(),l.rooms(),l.portals(),l.stairs(),l.voids(),props,l.sockets(),colliders,l.dock(),l.ambience());
    }
    private static void add(List<Prop> props,List<Collider> colliders,String mesh,int room,double x,double z,double yaw,double hx,double hz,double h){
        props.add(new Prop(mesh,room,x,0,z,1,yaw,hx,hz,h,"floor"));colliders.add(new Collider(x,0,z,hx,hz,h,yaw));
    }
}
