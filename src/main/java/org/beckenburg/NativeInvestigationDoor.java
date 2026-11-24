package org.beckenburg;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.beckenburg.LevelDefinition.Portal;

/** The fitted portal, canonical leaf mesh and its two authoritative physical poses. */
record NativeInvestigationDoor(Spec spec, Pose closedPose, Pose openPose, double slideTravel) {
    static final String ASSET = "door_1_loose";
    private static final double SOURCE_HEIGHT = 2.1;
    private static final double SOURCE_HALF_DEPTH = .03;

    record Spec(String frameId, String roomA, String roomB, Portal portal, double frameFrom,
                double frameTo, double clearHeight, int leaves, int hingeSide, int swingDirection,
                String leafSourceSha256) {
        Spec {
            if (frameId.isBlank() || roomA.isBlank() || roomB.isBlank() || roomA.equals(roomB)
                    || !portal.kind().equals("door") || portal.id() < 0
                    || !leafSourceSha256.matches("[0-9a-f]{64}")
                    || !Double.isFinite(portal.x()+portal.y()+portal.z()+portal.width()
                            +portal.loss()+frameFrom+frameTo+clearHeight)
                    || !(frameFrom < frameTo) || portal.width() < .6 || portal.width() > 3
                    || clearHeight < 1.8 || clearHeight > 3.2
                    || (leaves != 1 && leaves != 2) || Math.abs(hingeSide) != 1 || Math.abs(swingDirection) != 1)
                throw new IllegalArgumentException("Invalid native door frame " + frameId);
            double boundary = portal.alongX() ? portal.z() : portal.x();
            if (boundary < frameFrom-1e-6 || boundary > frameTo+1e-6)
                throw new IllegalArgumentException("Portal is outside its fitted frame " + frameId);
        }
    }

    record Pose(GlbProps.Model model, AuthoredSitePhysics physics) { }

    static NativeInvestigationDoor fit(Spec spec, GlbProps.Model source) throws IOException {
        return new NativeInvestigationDoor(spec, pose(spec,source,false), pose(spec,source,true), 0);
    }

    static NativeInvestigationDoor slide(Spec spec, GlbProps.Model closed, double travel) throws IOException {
        var portal=spec.portal();
        double x=portal.alongX()?travel:0;
        double z=portal.alongX()?0:travel;
        var open=new GlbProps.Model(closed.triangles().stream().map(face->new GlbProps.Triangle(
            offset(face.a(),x,z),offset(face.b(),x,z),offset(face.c(),x,z),face.material())).toList());
        return new NativeInvestigationDoor(spec,
            new Pose(closed,AuthoredSitePhysics.meshObstacles(spec.frameId(),closed)),
            new Pose(open,AuthoredSitePhysics.meshObstacles(spec.frameId(),open)),travel);
    }

    private static GlbProps.Vertex offset(GlbProps.Vertex v,double x,double z){
        return new GlbProps.Vertex((float)(v.x()+x),v.y(),(float)(v.z()+z),v.u(),v.v(),v.nx(),v.ny(),v.nz());
    }

    boolean sliding(){
        return slideTravel!=0;
    }

    Pose pose(boolean open) {
        return open ? openPose : closedPose;
    }

    private static Pose pose(Spec spec, GlbProps.Model source, boolean open) throws IOException {
        var portal = spec.portal();
        var faces = new ArrayList<GlbProps.Triangle>();
        double frameCenter = (spec.frameFrom()+spec.frameTo())/2;
        double width = portal.width()/spec.leaves();
        double height = spec.clearHeight()/SOURCE_HEIGHT;
        boolean along = portal.alongX();
        for (int side : spec.leaves()==1 ? new int[]{spec.hingeSide()} : new int[]{-1,1}) {
            double x = along ? portal.x()+side*portal.width()/2 : frameCenter;
            double z = along ? frameCenter : portal.z()+side*portal.width()/2;
            // The asset's -X runs from its hinge to its latch. Its +Z
            // panel runs from 0 to .06, with the midpoint on the hinge plane.
            double cos = open ? (along ? 0 : -spec.swingDirection()) : (along ? side : 0);
            double sin = open ? (along ? -spec.swingDirection() : 0) : (along ? 0 : side);
            for (var face : source.triangles()) {
                faces.add(new GlbProps.Triangle(
                        vertex(face.a(),x,portal.y(),z,width,height,cos,sin),
                        vertex(face.b(),x,portal.y(),z,width,height,cos,sin),
                        vertex(face.c(),x,portal.y(),z,width,height,cos,sin),face.material()));
            }
        }
        var model = new GlbProps.Model(List.copyOf(faces));
        return new Pose(model,AuthoredSitePhysics.meshObstacles(spec.frameId(),model));
    }

    private static GlbProps.Vertex vertex(GlbProps.Vertex v, double x, double y, double z,
                                           double width, double height, double cos, double sin) {
        double localX = v.x()*width, localZ = v.z()-SOURCE_HALF_DEPTH;
        double nx = v.nx()/width, ny = v.ny()/height, nz = v.nz();
        double length = Math.sqrt(nx*nx+ny*ny+nz*nz);
        if (length > 0) {
            nx /= length;
            ny /= length;
            nz /= length;
        }
        // The positions rendered on both states are the exact float vertices
        // loaded into the native collision and optical grid.
        return new GlbProps.Vertex((float)(x+localX*cos-localZ*sin), (float)(y+v.y()*height),
                (float)(z+localX*sin+localZ*cos),v.u(),v.v(),nx*cos-nz*sin,ny,nx*sin+nz*cos);
    }
}
