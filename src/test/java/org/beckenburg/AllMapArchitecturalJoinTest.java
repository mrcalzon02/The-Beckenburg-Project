package org.beckenburg;
/** Dense receiver-side boundary probes on each built indoor map, not full visual acceptance. */
public final class AllMapArchitecturalJoinTest {
    public static void main(String[] args){int total=0;
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
            total+=count;System.out.println("PASS "+site+" "+count+" closed wall/floor/corner boundary rays");
        }
        System.out.println("PASS total"+total+" receiver-side architectural probes; native maps require separate physical-mesh review");
    }
    private static String ArraysText(double[] p){return java.util.Arrays.toString(p);}
}
