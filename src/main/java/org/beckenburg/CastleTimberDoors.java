package org.beckenburg;
import java.util.*;
/** One fitted timber assembly; visible leaves, collision and light rays share hinges. */
final class CastleTimberDoors {
    record Leaf(double x,double y,double z,double width,double height,double angle){
        static final double THICKNESS=.08;
        double[] point(double u,double h,double v){return new double[]{x+u*Math.cos(angle)-v*Math.sin(angle),y+h,z+u*Math.sin(angle)+v*Math.cos(angle)};}
        double[] local(double px,double py,double pz){double dx=px-x,dz=pz-z;return new double[]{dx*Math.cos(angle)+dz*Math.sin(angle),py-y,-dx*Math.sin(angle)+dz*Math.cos(angle)};}
    }
    static List<Leaf> leaves(CastleDoorSlots.Slot slot,boolean open){
        double a=slot.horizontal()?0:Math.PI/2,w=(slot.width()-.16)/2+.004;
        double x=slot.x0()+(slot.horizontal()?.08:0),z=slot.z0()+(slot.horizontal()?0:.08);
        var left=new Leaf(x,slot.bottom()+.04,z,w,slot.clearHeight()-.12,a+(open?Math.PI/2:0));
        var right=new Leaf(slot.x1()-(slot.horizontal()?.08:0),slot.bottom()+.04,slot.z1()-(slot.horizontal()?0:.08),w,slot.clearHeight()-.12,a+Math.PI-(open?Math.PI/2:0));
        return List.of(left,right);
    }
    static boolean blocks(CastleDoorSlots.Slot slot,boolean open,double x,double y,double z,double radius){for(var leaf:leaves(slot,open)){var p=leaf.local(x,y,z);if(p[1]<leaf.height()&&p[1]+1.55>0&&p[0]>=-radius&&p[0]<=leaf.width()+radius&&Math.abs(p[2])<=Leaf.THICKNESS/2+radius)return true;}return false;}
    static boolean crosses(CastleDoorSlots.Slot slot,boolean open,double ax,double ay,double az,double bx,double by,double bz){for(var leaf:leaves(slot,open)){var a=leaf.local(ax,ay,az);var b=leaf.local(bx,by,bz);if(CastleDoorSlots.crossesBox(a[0],a[1],a[2],b[0],b[1],b[2],0,0,-Leaf.THICKNESS/2,leaf.width(),leaf.height(),Leaf.THICKNESS/2))return true;}return false;}
}
