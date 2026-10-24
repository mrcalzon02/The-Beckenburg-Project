package org.beckenburg;
import org.beckenburg.LevelDefinition.*;

/** Sparse, authored shrub pockets. Foliage detail settings never change availability. */
public final class OutdoorHiding {
    private OutdoorHiding(){}
    public static boolean available(LevelDefinition level,Socket s,int density){
        if(density<0||density>100)throw new IllegalArgumentException("hiding density");
        return s!=null&&s.type().equals("outdoor-hide")&&HidingBudget.available(level,s,density);
    }
    public static Socket containing(LevelDefinition level,double x,double y,double z,int density){
        for(Socket s:level.sockets())if(available(level,s,density)&&level.room(s.room()).outside()
                &&Math.abs(y-s.y())<.12&&Math.hypot(x-s.x(),z-s.z())<.36)return s;
        return null;
    }
    static void populate(LevelPlans.B b){
        switch(b.id){
            case "house" -> add(b,12,8.8,6.8);
            case "farmhouse" -> add(b,12,8.5,7);
            case "school" -> add(b,12,7.7,6.8);
            case "blackpine" -> {add(b,12,9,30);add(b,12,50,12);add(b,12,34,56);}
            default -> { }
        }
    }
    private static void add(LevelPlans.B b,int room,double x,double z){
        b.socket("outdoor-hide",room,x,b.rooms.get(room).y(),z,0,"shrub-pocket");
        // Leaves are penetrable cover. Never manufacture an impassable box around a bush.
        b.floorProp("level_shrub_refuge",room,x,z,1,0,0,0,1.45);
    }
}
