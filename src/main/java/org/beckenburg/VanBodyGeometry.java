package org.beckenburg;

import java.util.List;

/** Donor Car 08 rear-opening datum and original rear-door UV corners.
 * The shipped shell omitted the two large rear-panel triangles per side.
 * These restore the original Car8.obj faces, not a new atlas projection.
 * Source: pinned August 2023 GGBot archive, Car 08/Car8.obj (OBJ V flipped
 * once to match the shell GLB's glTF/top-left atlas convention). */
final class VanBodyGeometry {
    private VanBodyGeometry(){}
    static final double SOURCE_REAR=-1.982265830, SOURCE_HALF_WIDTH=1.712552309;
    static final double SOURCE_SHOULDER_Y=2.214065313, SOURCE_CROWN_X=1.552118182;
    record Corner(double x,double y,double u,double v){}
    static List<Corner[]> door(boolean left){
        double side=left?-1:1;
        var bottom=new Corner(side*1.712550998,.373940349,.975,.412359);
        var shoulder=new Corner(side*1.712550998,2.214065313,.974996,.047429);
        var crown=new Corner(side*SOURCE_CROWN_X,2.319999218,.957541,.026421);
        var top=new Corner(0,2.319999933,.788676,.026422);
        var centre=new Corner(0,.373938198,.788682,.412362);
        return left?List.of(new Corner[]{bottom,shoulder,top},new Corner[]{top,centre,bottom},new Corner[]{shoulder,crown,top})
            :List.of(new Corner[]{bottom,top,shoulder},new Corner[]{top,bottom,centre},new Corner[]{shoulder,top,crown});
    }
    static double[] doorPoint(Corner v,boolean left,boolean closed,double depth){
        double[] p=SceneRenderer.vanExteriorLocal(v.x(),v.y(),SOURCE_REAR);
        double hinge=left?-VanStaging.HALF_WIDTH:VanStaging.HALF_WIDTH;
        double swing=closed?0:(left?-1:1)*Math.toRadians(99),dx=p[0]-hinge,dz=depth;
        return new double[]{hinge+dx*Math.cos(swing)-dz*Math.sin(swing),p[1],
            VanStaging.FRONT+dx*Math.sin(swing)+dz*Math.cos(swing)};
    }
}
