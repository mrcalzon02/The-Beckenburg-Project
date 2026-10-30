package org.beckenburg;

/** Conservative source-to-receiver-cell aperture test for door shadow invalidation.
 * A false result proves that no segment to the cell can cross the aperture;
 * uncertain, near-plane and straddling cases deliberately return true. */
final class PortalLightCellFilter {
    private static final double MARGIN = .35;
    private PortalLightCellFilter() {}

    static boolean mayCross(double sx,double sy,double sz,
            int ix,int iy,int iz,double cellSize,
            double px,double py,double pz,double width,boolean alongX,boolean window) {
        double x0=ix*cellSize,x1=x0+cellSize;
        double y0=iy*cellSize,y1=y0+cellSize;
        double z0=iz*cellSize,z1=z0+cellSize;
        double sourceNormal=alongX?sz:sx;
        double plane=alongX?pz:px;
        double near=alongX?z0:x0,far=alongX?z1:x1;
        // Same-side rays cannot reach the aperture. A thick frame or a
        // receiver cell straddling the plane is deliberately ambiguous.
        if(sourceNormal<plane-MARGIN&&far<plane-MARGIN)return false;
        if(sourceNormal>plane+MARGIN&&near>plane+MARGIN)return false;
        if(Math.abs(sourceNormal-plane)<=MARGIN
                ||near<=plane+MARGIN&&far>=plane-MARGIN)return true;

        double minAlong=Double.POSITIVE_INFINITY,maxAlong=Double.NEGATIVE_INFINITY;
        double minHeight=Double.POSITIVE_INFINITY,maxHeight=Double.NEGATIVE_INFINITY;
        for(double x:new double[]{x0,x1})for(double y:new double[]{y0,y1})
                for(double z:new double[]{z0,z1}){
            double targetNormal=alongX?z:x;
            double t=(plane-sourceNormal)/(targetNormal-sourceNormal);
            if(t<0||t>1)continue;
            double along=(alongX?sx:sz)+t*((alongX?x:z)-(alongX?sx:sz));
            double height=sy+t*(y-sy);
            minAlong=Math.min(minAlong,along);maxAlong=Math.max(maxAlong,along);
            minHeight=Math.min(minHeight,height);maxHeight=Math.max(maxHeight,height);
        }
        if(minAlong==Double.POSITIVE_INFINITY)return true;
        double centre=alongX?px:pz;
        double low=py+(window?.8:-.25)-MARGIN;
        double high=py+(window?2.55:2.65)+MARGIN;
        return minAlong<=centre+width*.5+MARGIN
                &&maxAlong>=centre-width*.5-MARGIN
                &&minHeight<=high&&maxHeight>=low;
    }
}
