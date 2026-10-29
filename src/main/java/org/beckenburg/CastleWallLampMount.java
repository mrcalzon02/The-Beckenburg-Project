package org.beckenburg;

import java.util.List;

/**
 * Fits imported wall-light geometry against the visible face of the authored
 * masonry rather than treating a GLB's arbitrary centre as its wall pivot.
 * Scene/save coordinates remain unchanged; only the rendered mesh is fitted.
 */
final class CastleWallLampMount {
    private CastleWallLampMount() {}
    static final double CLEARANCE = .018;
    private static final double PLATE_OFFSET = .025;

    record Pose(double x,double z,double wallX,double wallZ,
                double plateX,double plateZ,boolean supported) {}

    static Pose resolve(CastleHubDecor.Light fixture,
                        List<CastleHubArchitecturalPlan.Wall> walls) {
        Pose unchanged=new Pose(fixture.x(),fixture.z(),Double.NaN,Double.NaN,
                fixture.x(),fixture.z(),false);
        if(fixture.mount()!=CastleHubDecor.Mount.WALL)return unchanged;
        double fx=Math.sin(fixture.yaw()),fz=Math.cos(fixture.yaw());
        double nearest=Double.POSITIVE_INFINITY,wx=0,wz=0;
        for(var wall:walls){
            if(wall.hasDoor()||fixture.y()<wall.bottom()+.04
                    ||fixture.y()>wall.top()-.04)continue;
            double px=wall.horizontal()?
                    Math.max(wall.x0(),Math.min(fixture.x(),wall.x1())):wall.x0();
            double pz=wall.horizontal()?wall.z0():
                    Math.max(wall.z0(),Math.min(fixture.z(),wall.z1()));
            double dx=px-fixture.x(),dz=pz-fixture.z();
            double distance=Math.hypot(dx,dz);
            if(distance<.06||distance>.65
                    ||dx*fx+dz*fz<distance*.85)continue;
            if(distance<nearest){nearest=distance;wx=px;wz=pz;}
        }
        if(!Double.isFinite(nearest))return unchanged;

        // Evaluate both power states, so switching a lamp never changes its pose.
        double reach=Math.max(rearExtent(fixture,fixture.on()),
                              rearExtent(fixture,fixture.off()));
        double half=CastleDoorSlots.MASONRY_THICKNESS*.5;
        // The rearmost imported triangle is at least CLEARANCE in front of
        // the actual masonry face, not merely in front of its centerline.
        double move=reach+half+CLEARANCE-nearest;
        return new Pose(fixture.x()-fx*move,fixture.z()-fz*move,wx,wz,
                wx-fx*(half+PLATE_OFFSET),wz-fz*(half+PLATE_OFFSET),true);
    }

    private static double rearExtent(CastleHubDecor.Light fixture,String mesh){
        double minX=Double.POSITIVE_INFINITY,minZ=minX;
        double maxX=Double.NEGATIVE_INFINITY,maxZ=maxX;
        for(var triangle:GlbProps.model(mesh).triangles())
            for(var vertex:new GlbProps.Vertex[]{triangle.a(),triangle.b(),triangle.c()}){
                minX=Math.min(minX,vertex.x());maxX=Math.max(maxX,vertex.x());
                minZ=Math.min(minZ,vertex.z());maxZ=Math.max(maxZ,vertex.z());
            }
        if(!Double.isFinite(minX+maxX+minZ+maxZ))
            throw new IllegalStateException("Unbounded wall fixture "+mesh);
        double cx=(minX+maxX)*.5,cz=(minZ+maxZ)*.5;
        double cy=Math.cos(fixture.yaw()),sy=Math.sin(fixture.yaw());
        double fx=Math.sin(fixture.yaw()),fz=Math.cos(fixture.yaw());
        double rear=Double.NEGATIVE_INFINITY;
        for(double x:new double[]{minX,maxX})
            for(double z:new double[]{minZ,maxZ}){
                double dx=(x-cx)*fixture.scale(),dz=(z-cz)*fixture.scale();
                rear=Math.max(rear,(dx*cy-dz*sy)*fx+(dx*sy+dz*cy)*fz);
            }
        return rear;
    }
}
