package org.beckenburg;
import java.util.*;
import org.beckenburg.LevelDefinition.*;

/** The same hollow cupboard, standing pocket and difficulty rule drive all players. */
public final class LevelClosets {
    // The interior begins at the actual entrance. These are the inner faces
    // of the canonical cupboard's sides and back, shared with its cover cue.
    static final double HALF_X=.79, BACK_Z=-.54, ENTRY_Z=.60, HEAD=2.02;
    private static final double FLOOR_TOLERANCE=.15;
    private LevelClosets(){}
    public static boolean available(LevelDefinition level,int room,int density){
        if(density<0||density>100)throw new IllegalArgumentException("hiding density");
        Socket socket=level.socket("hide",room);
        return room>=0&&room<12&&socket!=null&&socket.style().equals("walk-in-cupboard")&&HidingBudget.available(level,socket,density);
    }
    public static Vec world(Socket s,double x,double y,double z){
        double c=Math.cos(s.yaw()),sn=Math.sin(s.yaw());return new Vec(s.x()+x*c-z*sn,s.y()+y,s.z()+x*sn+z*c);
    }
    public static boolean inside(Socket s,double x,double y,double z){
        return s!=null&&Math.abs(y-s.y())<=FLOOR_TOLERANCE&&containsFloorPoint(s,x,z);
    }
    private static boolean containsFloorPoint(Socket s,double x,double z){
        double dx=x-s.x(),dz=z-s.z(),c=Math.cos(s.yaw()),sn=Math.sin(s.yaw());
        double localX=dx*c+dz*sn,localZ=-dx*sn+dz*c;
        return Math.abs(localX)<=HALF_X&&localZ>=BACK_Z&&localZ<=ENTRY_Z;
    }
    static boolean protectedAt(LevelDefinition level,double x,double y,double z,int density){
        for(var socket:HidingBudget.candidates(level))
            if(socket.type().equals("hide")&&HidingBudget.available(level,socket,density)
                    &&y>=socket.y()-FLOOR_TOLERANCE&&y<=socket.y()+HEAD&&containsFloorPoint(socket,x,z))
                return true;
        return false;
    }
    static boolean blocks(LevelDefinition level,Vec from,Vec to,int density){
        for(var socket:HidingBudget.candidates(level))
            if(socket.type().equals("hide")&&HidingBudget.available(level,socket,density)&&crosses(socket,from,to))
                return true;
        return false;
    }
    private static boolean crosses(Socket socket,Vec from,Vec to){
        double c=Math.cos(socket.yaw()),s=Math.sin(socket.yaw());
        double[] a=local(socket,from,c,s),b=local(socket,to,c,s);
        double[] lower={-HALF_X,-FLOOR_TOLERANCE,BACK_Z},upper={HALF_X,HEAD,ENTRY_Z};
        double enter=0,exit=1;
        for(int axis=0;axis<3;axis++){
            double delta=b[axis]-a[axis];
            if(Math.abs(delta)<1e-12){
                if(a[axis]<lower[axis]||a[axis]>upper[axis])return false;
                continue;
            }
            double first=(lower[axis]-a[axis])/delta,second=(upper[axis]-a[axis])/delta;
            enter=Math.max(enter,Math.min(first,second));
            exit=Math.min(exit,Math.max(first,second));
            if(enter>exit)return false;
        }
        return true;
    }
    private static double[] local(Socket socket,Vec point,double c,double s){
        double x=point.x()-socket.x(),z=point.z()-socket.z();
        return new double[]{x*c+z*s,point.y()-socket.y(),-x*s+z*c};
    }
    public static List<Collider> colliders(LevelDefinition level,int density){
        List<Collider> result=new ArrayList<>();
        for(Socket s:level.sockets())if(s.type().equals("hide")&&available(level,s.room(),density)){
            add(result,s,-.82,0,0,.03,.60,2.3);add(result,s,.82,0,0,.03,.60,2.3);
            add(result,s,0,0,-.57,.82,.03,2.3);
            add(result,s,-.62,0,.61,.19,.04,2.02);add(result,s,.62,0,.61,.19,.04,2.02);
        }
        return List.copyOf(result);
    }
    private static void add(List<Collider> result,Socket s,double x,double y,double z,double hx,double hz,double height){
        Vec p=world(s,x,y,z);result.add(new Collider(p.x(),p.y(),p.z(),hx,hz,height,s.yaw()));
    }
}
