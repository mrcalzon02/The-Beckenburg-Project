package org.beckenburg;

import org.beckenburg.LevelDefinition.Vec;

/** Exact collision query budgets and diagonal corner parity for visual camera motion. */
public final class LanCameraCollisionTest {
    private static void check(boolean condition,String message){
        if(!condition)throw new AssertionError(message);
    }
    public static void main(String[] args){
        var from=new LanVisualMotion.Pose(1,2,3);
        int[] calls={0};
        LanCameraCollision.Sweep free=(start,x,z)->{
            calls[0]++;
            check(start.equals(new Vec(1,2,3)),"Every sweep starts at the host-owned pose");
            return new Vec(x,2.04,z);
        };
        var north=LanCameraCollision.resolve(from,1,3.31,free);
        check(calls[0]==1&&north!=null&&north.y()==2.04,
            "Straight Z movement needs one swept collision, keeping floor elevation");
        calls[0]=0;
        var east=LanCameraCollision.resolve(from,1.31,3,free);
        check(calls[0]==1&&east!=null&&east.x()==1.31,
            "Straight X movement needs one swept collision");
        calls[0]=0;
        var diagonal=LanCameraCollision.resolve(from,1.31,3.31,free);
        check(calls[0]==3&&diagonal!=null,
            "True diagonal checks direct, X-only, Z-only swept paths");
        calls[0]=0;
        var corner=LanCameraCollision.resolve(from,1.31,3.31,(start,x,z)->{
            calls[0]++;
            if(x==1.31&&z==3)return null;
            return new Vec(x,2,z);
        });
        check(calls[0]==2&&corner==null,
            "A blocked independent axis still prevents diagonal corner cutting");
        calls[0]=0;
        var blocked=LanCameraCollision.resolve(from,1,3.31,(start,x,z)->{
            calls[0]++;return null;
        });
        check(calls[0]==1&&blocked==null,
            "Directly blocked movement never proceeds to extra sweeps");
        System.out.println("PASS camera collision: 1 cardinal vs 3 diagonal sweeps, host-pose anchor, blocked corner and elevation preserved");
    }
}
