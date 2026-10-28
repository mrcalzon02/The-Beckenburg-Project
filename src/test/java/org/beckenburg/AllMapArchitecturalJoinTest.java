package org.beckenburg;
/** Dense receiver-side boundary probes on each built indoor map, not full visual acceptance. */
public final class AllMapArchitecturalJoinTest {
    public static void main(String[] args){int total=0,open=0;
        for(var site:HouseMap.Site.values()){
            var map=new HouseMap(site);if(map.nativeSite())continue;int count=0;
            for(var r:map.level().rooms())if(!r.outside()){
                for(double t:new double[]{.05,.2,.5,.8,.95})for(double h:new double[]{.061,.12,r.height()-.061}){
                    double y=r.y()+h,x=r.x0()+(r.x1()-r.x0())*t,z=r.z0()+(r.z1()-r.z0())*t;
                    double[][] rays={{r.x0()-.5,y,z,r.x0()+.08,y,z},{r.x1()+.5,y,z,r.x1()-.08,y,z},{x,y,r.z0()-.5,x,y,r.z0()+.08},{x,y,r.z1()+.5,x,y,r.z1()-.08}};
                    for(var p:rays){if(ArchitecturalLightOcclusion.visible(map,r.id(),p[0],p[1],p[2],p[3],p[4],p[5]))throw new AssertionError("Boundary join leak "+site+" room"+r.id()+" "+ArraysText(p));count++;}
                }
                for(double t:new double[]{.2,.5,.8}){double x=r.x0()+(r.x1()-r.x0())*t,z=r.z0()+(r.z1()-r.z0())*t;if(map.level().floorVoid(x,r.y(),z))continue;if(ArchitecturalLightOcclusion.architecturalClear(map,x,r.y()-.2,z,x,r.y()+.2,z))throw new AssertionError("Floor join leak "+site+" room"+r.id());count++;}
            }
            int openings=stairApertures(map);open+=openings;
            total+=count;System.out.println("PASS "+site+" "+count+" closed wall/floor/corner boundary rays and "+openings+" actual clear stair rays");
        }
        if(open<1000)throw new AssertionError("Insufficient real stair-aperture coverage");
        System.out.println("PASS total"+total+" closed and "+open+" open stair probes; native maps require separate physical-mesh review");
    }
    private static String ArraysText(double[] p){return java.util.Arrays.toString(p);}
    private static int stairApertures(HouseMap map){int count=0;
        for(var s:map.level().stairs())for(double t:new double[]{.1,.25,.5,.75,.9})
            for(double h:new double[]{.3,1.3,1.6})for(double d:new double[]{.15,.4,.8})for(boolean axis:new boolean[]{true,false}){
                double x=s.x0()+(s.x1()-s.x0())*t,z=s.z0()+(s.z1()-s.z0())*t,y=s.y0()+(s.y1()-s.y0())*t+h;
                double ax=x-(axis?d:0),az=z-(axis?0:d),bx=x+(axis?d:0),bz=z+(axis?0:d);
                if(!map.clearLineOfSight(ax,y,az,bx,y,bz))continue;
                if(!ArchitecturalLightOcclusion.architecturalClear(map,ax,y,az,bx,y,bz))throw new AssertionError("Real clear staircase was made opaque: "+map.site()+" "+s);
                count++;
            }
        return count;
    }
}
