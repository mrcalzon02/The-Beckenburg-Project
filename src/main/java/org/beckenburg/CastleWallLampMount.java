package org.beckenburg;

import java.util.List;

/**
 * Fits a wall light to the supporting stone plane, not to its arbitrary GLB
 * origin or an editor yaw that can leave the housing skewed across masonry.
 * Save coordinates and the original mesh/UVs remain unchanged.
 */
final class CastleWallLampMount {
    private CastleWallLampMount() {}
    static final double CLEARANCE = .018;
    private static final double PLATE_OFFSET = .011;

    record Pose(double x,double z,double wallX,double wallZ,
                double plateX,double plateZ,double yaw,boolean supported) {}

    static Pose resolve(CastleHubDecor.Light fixture,
                        List<CastleHubArchitecturalPlan.Wall> walls) {
        Pose unchanged=new Pose(fixture.x(),fixture.z(),Double.NaN,Double.NaN,
                fixture.x(),fixture.z(),fixture.yaw(),false);
        if(fixture.mount()!=CastleHubDecor.Mount.WALL)return unchanged;

        double requestedX=Math.sin(fixture.yaw()),requestedZ=Math.cos(fixture.yaw());
        double nearest=Double.POSITIVE_INFINITY,wx=0,wz=0,nx=0,nz=0;
        for(var wall:walls){
            if(wall.hasDoor()||fixture.y()<wall.bottom()+.04
                    ||fixture.y()>wall.top()-.04)continue;
            double px=wall.horizontal()?
                    Math.max(Math.min(wall.x0(),wall.x1()),
                        Math.min(fixture.x(),Math.max(wall.x0(),wall.x1()))):wall.x0();
            double pz=wall.horizontal()?wall.z0():
                    Math.max(Math.min(wall.z0(),wall.z1()),
                        Math.min(fixture.z(),Math.max(wall.z0(),wall.z1())));
            double axis=wall.horizontal()?pz-fixture.z():px-fixture.x();
            double distance=Math.hypot(px-fixture.x(),pz-fixture.z());
            if(Math.abs(axis)<.06||distance>.65)continue;
            double wallNX=wall.horizontal()?0:Math.signum(axis);
            double wallNZ=wall.horizontal()?Math.signum(axis):0;
            // Keep the authored intended side, but take the final yaw from the
            // ACTUAL surface. A 15-degree editor error must not cant the lamp.
            if(wallNX*requestedX+wallNZ*requestedZ<.70)continue;
            if(distance<nearest){
                nearest=distance;wx=px;wz=pz;nx=wallNX;nz=wallNZ;
            }
        }
        if(!Double.isFinite(nearest))return unchanged;

        double yaw=Math.atan2(nx,nz);
        // One common source origin for both models prevents power-state jumps.
        // Project actual vertices, not bounding-box corners, onto the wall.
        double rear=Math.max(rearExtent(fixture,fixture.on(),yaw,nx,nz),
                             rearExtent(fixture,fixture.off(),yaw,nx,nz));
        double half=CastleDoorSlots.MASONRY_THICKNESS*.5;
        double setback=half+CLEARANCE+rear;
        return new Pose(wx-nx*setback,wz-nz*setback,wx,wz,
                wx-nx*(half+PLATE_OFFSET),wz-nz*(half+PLATE_OFFSET),
                yaw,true);
    }

    /** Compact metal strip on the actual masonry plane. */
    record Plate(double x,double y,double z,double width,double height,double depth) {}
    static Plate plate(CastleHubDecor.Light fixture,Pose pose){
        double width=.76*fixture.scale(),height=.12*fixture.scale(),depth=.024;
        double y=fixture.y()+.07*fixture.scale()-height/2;
        return Math.abs(Math.sin(pose.yaw()))>.5
            ?new Plate(pose.plateX()-depth/2,y,pose.wallZ()-width/2,depth,height,width)
            :new Plate(pose.wallX()-width/2,y,pose.plateZ()-depth/2,width,height,depth);
    }

    private static double rearExtent(CastleHubDecor.Light fixture,String mesh,
                                      double yaw,double nx,double nz){
        var origin=SceneRenderer.castleBounds(fixture.off());
        double c=Math.cos(yaw),s=Math.sin(yaw),rear=Double.NEGATIVE_INFINITY;
        for(var triangle:GlbProps.model(mesh).triangles())
            for(var vertex:new GlbProps.Vertex[]{triangle.a(),triangle.b(),triangle.c()}){
                double x=(vertex.x()-origin[0])*fixture.scale();
                double z=(vertex.z()-origin[2])*fixture.scale();
                double projected=(x*c-z*s)*nx+(x*s+z*c)*nz;
                rear=Math.max(rear,projected);
            }
        if(!Double.isFinite(rear))
            throw new IllegalStateException("Unbounded wall fixture "+mesh);
        return rear;
    }
}
