package org.beckenburg;
/** These short end segments used to be discarded by the source-facing shadow bias. */
public final class LightJoinOcclusionTest {
    static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
    static boolean old(HouseMap m,double ax,double ay,double az,double x,double y,double z){
        double dx=x-ax,dy=y-ay,dz=z-az,d=Math.sqrt(dx*dx+dy*dy+dz*dz),b=Math.min(.12,d*.2)/Math.max(.001,d);
        return m.clearLineOfSight(ax,ay,az,x-dx*b,y-dy*b,z-dz*b);
    }
    public static void main(String[] args){
        var map=BlackpineLevel.map();double fy=map.floorY(0),upper=map.floorY(11);
        check(old(map,48.5,fy+2.855,35,48.5,upper,35),"baseline floor leak reproduced");
        check(!ArchitecturalLightOcclusion.visible(map,11,48.5,fy+2.855,35,48.5,upper,35),"underloft light cannot illuminate upper deck through its receiving endpoint");
        check(old(map,39,fy+1.3,23,40.08,fy+1.3,23),"baseline back-wall leak reproduced");
        check(!ArchitecturalLightOcclusion.visible(map,0,39,fy+1.3,23,40.08,fy+1.3,23),"outside source cannot light inner wall face through its join");
        for(double height:new double[]{.061,.12,1.3,5.94})
            check(!ArchitecturalLightOcclusion.visible(map,0,39,fy+height,23,40.08,fy+height,23),"wall and floor/ceiling joins remain sealed at "+height);
        var door=map.level().portals().stream().filter(p->p.door()&&p.joins(0,12)&&p.x()==40).findFirst().orElseThrow();
        check(!ArchitecturalLightOcclusion.visible(map,0,39,fy+1.3,25,41,fy+1.3,25),"closed door blocks light");
        map.toggleDoor(door.id());
        check(ArchitecturalLightOcclusion.visible(map,0,39,fy+1.3,25,41,fy+1.3,25),"open door remains a real light aperture");
        check(ArchitecturalLightOcclusion.architecturalClear(map,50.5,upper-.25,30.5,50.5,upper+.25,30.5),"authored stair void remains open");
        System.out.println("PASS reproduced old wall/floor endpoint leaks; sealed receiving-side wall, floor and ceiling joins; genuine door and stair apertures retained");
    }
}
