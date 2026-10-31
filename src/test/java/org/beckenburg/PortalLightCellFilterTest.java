package org.beckenburg;

import java.util.Random;

/** Pure geometric negative controls and conservative sampled-ray parity.
 * This test needs only the production helper, not an OpenGL context. */
public final class PortalLightCellFilterTest {
    private static void check(boolean value,String message){
        if(!value)throw new AssertionError(message);
    }
    private static boolean crossing(double sx,double sy,double sz,
            double tx,double ty,double tz,double px,double py,double pz,
            double width,boolean alongX,boolean window){
        double start=alongX?sz:sx,end=alongX?tz:tx,plane=alongX?pz:px;
        if(Math.abs(end-start)<1e-9)return false;
        double t=(plane-start)/(end-start);
        if(t<0||t>1)return false;
        double lateral=(alongX?sx:sz)+t*((alongX?tx:tz)-(alongX?sx:sz));
        double height=sy+t*(ty-sy);
        return Math.abs(lateral-(alongX?px:pz))<=width*.5
                &&height>=py+(window?.8:-.25)
                &&height<=py+(window?2.55:2.65);
    }
    public static void main(String[] args){
        check(!PortalLightCellFilter.mayCross(-3,1,0,-2,0,0,1,0,0,0,1,false,false),
                "same-side receiver cannot cross the door");
        // The source's own cell is NOT automatically dependent on a
        // distant doorway. This prevents a redundant resident-light rebuild.
        check(!PortalLightCellFilter.mayCross(-3.2,1,0,-4,1,0,1,0,0,0,1,false,false),
                "source cell entirely before a door must retain its occlusion revision");
        check(!PortalLightCellFilter.mayCross(0,1,-3.2,0,1,-4,1,0,0,0,1,true,false),
                "along-X source cell before a door must retain its occlusion revision");
        check(PortalLightCellFilter.mayCross(-.1,1,0,-1,1,0,1,0,0,0,1,false,false),
                "source cell near the door plane must remain conservatively invalidated");
        check(PortalLightCellFilter.mayCross(0,1,-.1,0,1,-1,1,0,0,0,1,true,false),
                "near-plane along-X source cell must remain conservative");
        check(!PortalLightCellFilter.mayCross(-3,1,0,2,0,12,1,0,0,0,1,false,false),
                "off-axis receiver cannot cross the door");
        check(!PortalLightCellFilter.mayCross(-3,5,0,2,5,0,1,0,0,0,1,false,false),
                "high receiver cannot cross the doorway");
        check(PortalLightCellFilter.mayCross(-3,1,0,2,1,0,1,0,0,0,1,false,false),
                "through-door receiver must refresh");
        check(PortalLightCellFilter.mayCross(3,1,0,-3,1,0,1,0,0,0,1,false,false),
                "reverse through-door receiver must refresh");
        check(PortalLightCellFilter.mayCross(0,1,-3,0,1,2,1,0,0,0,1,true,false),
                "along-X doorway must refresh");
        check(PortalLightCellFilter.mayCross(-3,1,0,0,1,0,1,0,0,0,1,false,false),
                "receiver cell touching aperture plane must remain conservative");
        check(PortalLightCellFilter.mayCross(0,1,0,2,1,0,1,0,0,0,1,false,false),
                "source on aperture plane must remain conservative");
        Random random=new Random(20261030L);
        int crossingSamples=0;
        for(int i=0;i<3000;i++){
            boolean alongX=random.nextBoolean(),window=random.nextBoolean();
            double sx=random.nextDouble()*20-10,sy=random.nextDouble()*8-2,sz=random.nextDouble()*20-10;
            double px=random.nextDouble()*8-4,py=random.nextDouble()*3-1,pz=random.nextDouble()*8-4;
            double width=.6+random.nextDouble()*3,cellSize=.5+random.nextDouble()*2;
            int ix=random.nextInt(15)-7,iy=random.nextInt(7)-2,iz=random.nextInt(15)-7;
            boolean maybe=PortalLightCellFilter.mayCross(sx,sy,sz,ix,iy,iz,cellSize,px,py,pz,width,alongX,window);
            for(int a=0;a<3;a++)for(int b=0;b<3;b++)for(int c=0;c<3;c++){
                double tx=(ix+a*.5)*cellSize,ty=(iy+b*.5)*cellSize,tz=(iz+c*.5)*cellSize;
                if(crossing(sx,sy,sz,tx,ty,tz,px,py,pz,width,alongX,window)){
                    crossingSamples++;
                    check(maybe,"false negative: a sampled ray crosses the aperture (case "+i+")");
                }
            }
        }
        check(crossingSamples>100,"insufficient real crossing rays in deterministic sample");
        System.out.println("PASS portal-cell conservative filter: 12 directed cases and 3000 seeded cells; crossing rays="+crossingSamples);
    }
}
