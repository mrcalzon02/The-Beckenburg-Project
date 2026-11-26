package org.beckenburg;
import org.beckenburg.LevelDefinition.*;
import java.util.List;

/** Sparse, authored shrub pockets. Foliage detail settings never change availability. */
public final class OutdoorHiding {
    private OutdoorHiding(){}
    public static boolean available(LevelDefinition level,Socket s,int density){
        if(density<0||density>100)throw new IllegalArgumentException("hiding density");
        return s!=null&&s.type().equals("outdoor-hide")&&HidingBudget.available(level,s,density);
    }
    public static Socket containing(LevelDefinition level,double x,double y,double z,int density){
        for(Socket s:enabled(level,density)){
            if(!s.type().equals("outdoor-hide")||!level.room(s.room()).outside())continue;
            if(footprint(level,s).contains(x,y,z))return s;
        }
        return null;
    }
    static boolean blocks(LevelDefinition level,Vec from,Vec to,int density){
        for(Socket s:enabled(level,density))
            if(s.type().equals("outdoor-hide")&&level.room(s.room()).outside()&&footprint(level,s).crosses(from,to))return true;
        return false;
    }
    private static List<Socket> enabled(LevelDefinition level,int density){
        int allowance=HidingBudget.allowance(density);
        if(allowance==0)return List.of();
        var candidates=HidingBudget.candidates(level);
        return candidates.subList(0,Math.min(allowance,candidates.size()));
    }
    static SafeAreaBarrier.Volume footprint(LevelDefinition level,Socket socket){
        double x=socket.x(), y=socket.y(), z=socket.z();
        if(HollowCreekLevel.is(level)){
            for(var refuge:NativeRefuges.INSTANCES){
                var bounds=refuge.bounds();
                if(x<bounds[0]||x>bounds[3]||z<bounds[2]||z>bounds[5])continue;
                double radius=Math.min(Math.min(x-bounds[0],bounds[3]-x),Math.min(z-bounds[2],bounds[5]-z));
                double height=bounds[4]-y;
                if(radius>0&&height>0)return new SafeAreaBarrier.Volume(x,y,z,radius,height*.9,height);
            }
        }else{
            for(var prop:level.props()){
                if(!prop.mesh().equals("level_shrub_refuge")||Math.hypot(prop.x()-x,prop.z()-z)>=.2)continue;
                var bounds=SceneRenderer.castleBounds(prop.mesh());
                double radius=Math.min(bounds[3],bounds[5])*prop.scale()/2;
                double height=Math.max(1.15,prop.y()+(bounds[1]+bounds[4])*prop.scale()-y);
                return new SafeAreaBarrier.Volume(x,y,z,radius,height*.9,height);
            }
        }
        return new SafeAreaBarrier.Volume(x,y,z,.36,.70,1.15);
    }
    private static final class NativeRefuges {
        private static final List<HollowCreekScene.Instance> INSTANCES=HollowCreekScene.instances().stream()
            .filter(instance->instance.object().startsWith("Proposed authored refuge ")).toList();
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
