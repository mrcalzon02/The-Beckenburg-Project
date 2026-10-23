package org.beckenburg;
import org.beckenburg.LevelDefinition.Vec;

/** Host/local safety net, independent of drawing: recover only below the relevant physical support. */
public final class GroundRecovery {
    private GroundRecovery(){}
    public static Vec recover(HouseMap map,Vec pose,int roomHint,double bodyHeight){
        if(pose!=null&&Double.isFinite(pose.x()+pose.y()+pose.z())){
            int actual=map.roomAt(pose.x(),pose.y(),pose.z());
            if(actual>=0&&map.traversable(pose.x(),pose.y(),pose.z(),bodyHeight)&&Math.abs(pose.y()-map.groundHeight(pose.x(),pose.z(),actual))<.02)return pose;
            double floor=map.groundHeight(pose.x(),pose.z(),roomHint);
            Double vanFloor=map.van().supportHeight(pose.x(),pose.z());if(vanFloor!=null)floor=vanFloor;
            for(var stair:map.level().stairs())if((stair.a()==roomHint||stair.b()==roomHint)&&stair.contains(pose.x(),pose.z(),0))floor=stair.yAt(pose.x(),pose.z());
            if(pose.y()>=floor-.05)return pose;
            if(map.traversable(pose.x(),floor,pose.z(),bodyHeight))return new Vec(pose.x(),floor,pose.z());
            for(double radius=.25;radius<=3;radius+=.25)for(int i=0;i<16;i++){
                double a=i*Math.PI/8,x=pose.x()+Math.cos(a)*radius,z=pose.z()+Math.sin(a)*radius,y=map.groundHeight(x,z,roomHint);
                if(map.traversable(x,y,z,bodyHeight))return new Vec(x,y,z);
            }
        }
        int room=roomHint>=0&&roomHint<15?roomHint:14;var spawn=map.spawn(room);
        if(map.traversable(spawn.x(),spawn.y(),spawn.z(),bodyHeight))return spawn;
        return map.spawn(14);
    }
}
