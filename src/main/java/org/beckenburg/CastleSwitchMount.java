package org.beckenburg;

import java.util.List;

/** Source-fidelity wall switch fitting: no artificial backing strap and no
 * inherited lamp scale. All rotations derive from the actual supporting wall. */
final class CastleSwitchMount {
    private CastleSwitchMount(){}
    private static final String MESH="light_switch_1";
    static final double MAX_WIDTH=.16,MAX_HEIGHT=.17,MAX_DEPTH=.055;

    static CastleHubDecor.Light fit(CastleHubDecor.Switch sw,
             List<CastleHubArchitecturalPlan.Wall> walls){
        double[] bounds=SceneRenderer.castleBounds(MESH);
        if(bounds[3]<=0||bounds[4]<=0||bounds[5]<=0)
            throw new IllegalStateException("Degenerate imported light switch "+MESH);
        // Uniform source scale maintains all original triangle proportions, UVs
        // and materials. No world-axis stretching of the switch's face plate.
        double scale=Math.min(.85,Math.min(MAX_WIDTH/bounds[3],
                Math.min(MAX_HEIGHT/bounds[4],MAX_DEPTH/bounds[5])));
        double nearest=Double.POSITIVE_INFINITY;
        double yaw=sw.yaw();
        boolean supported=false;
        for(var wall:walls){
            if(wall.hasDoor()||sw.y()<wall.bottom()+.04||sw.y()>wall.top()-.04)
                continue;
            double px=wall.horizontal()
                ?Math.max(Math.min(wall.x0(),wall.x1()),Math.min(sw.x(),Math.max(wall.x0(),wall.x1())))
                :wall.x0();
            double pz=wall.horizontal()?wall.z0()
                :Math.max(Math.min(wall.z0(),wall.z1()),Math.min(sw.z(),Math.max(wall.z0(),wall.z1())));
            double dx=px-sw.x(),dz=pz-sw.z();
            double distance=Math.hypot(dx,dz);
            if(distance<.06||distance>.65)continue;
            // Prefer the physically closest support; break ties toward the
            // author-set face, but never preserve a backward-facing model.
            double aligned=(dx*Math.sin(sw.yaw())+dz*Math.cos(sw.yaw()))/distance;
            double score=distance+.025*(1-aligned);
            if(score<nearest){nearest=score;yaw=Math.atan2(dx,dz);supported=true;}
        }
        if(!supported)return null; // Deleted support wall: no floating switch.
        var fixture=new CastleHubDecor.Light(MESH,MESH,
            sw.x(),sw.y(),sw.z(),scale,yaw,CastleHubDecor.Mount.WALL);
        if(!CastleWallLampMount.resolve(fixture,walls).supported())return null;
        return fixture;
    }
}
