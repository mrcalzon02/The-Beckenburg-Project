package org.beckenburg;
import java.util.*;
import org.beckenburg.LevelDefinition.*;
/** Small individually committed model-production passes, placed in their real working context. */
public final class BlackpineProductionAssets {
    private BlackpineProductionAssets(){}
    public static final Set<String> ASSETS=Set.of("level_work_bandsaw","level_work_log_carriage","level_work_repair_bench","level_work_crew_footlocker","level_work_hearth_tools","level_work_boot_rack","level_work_survivor_cookbox");
    public static LevelDefinition apply(LevelDefinition l){
        var props=new ArrayList<>(l.props());var colliders=new ArrayList<>(l.colliders());
        add(props,colliders,"level_work_bandsaw",0,42.6,33.8,0,1.48,.74,3.28);
        add(props,colliders,"level_work_log_carriage",0,43.2,30.6,0,.85,1.91,1.14);
        add(props,colliders,"level_work_repair_bench",8,47.0,46.7,0,1.24,.62,1.66);
        add(props,colliders,"level_work_crew_footlocker",4,13.3,24.4,0,.50,.52,.86);
        add(props,colliders,"level_work_crew_footlocker",4,10.4,24.2,.12,.50,.52,.86);
        add(props,colliders,"level_work_hearth_tools",6,24.55,11.35,0,.42,.43,1.07);
        add(props,colliders,"level_work_hearth_tools",2,15.15,45.25,.2,.42,.43,1.07);
        add(props,colliders,"level_work_boot_rack",5,11.50,38.8,Math.PI/2,.71,.25,1.18);
        add(props,colliders,"level_work_survivor_cookbox",7,34.9,9.0,-.08,.44,.32,.93);
        return new LevelDefinition(l.id(),"blackpine-model-passes-v1",l.title(),l.rooms(),l.portals(),l.stairs(),l.voids(),props,l.sockets(),colliders,l.dock(),l.ambience());
    }
    private static void add(List<Prop> props,List<Collider> colliders,String mesh,int room,double x,double z,double yaw,double hx,double hz,double h){
        props.add(new Prop(mesh,room,x,0,z,1,yaw,hx,hz,h,"floor"));colliders.add(new Collider(x,0,z,hx,hz,h,yaw));
    }
}
