package org.beckenburg;
/** These short end segments used to be discarded by the source-facing shadow bias. */
public final class LightJoinOcclusionTest {
    static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
    /** Recreate the historical single-plane loft used by this reproducer.
     * Current main added DeckSlabs physical colliders (see BLACKPINE_WORKLIGHTS.md,
     * DEV-001); requiring that corrected map to leak would reject the real fix.
     * Keep both original positive leak assertions on the pre-depth fixture and
     * separately require the current production map to block the floor ray. */
    static HouseMap beforePhysicalDeck(HouseMap current){
        var l=current.level();
        var slabColliders=new java.util.HashSet<LevelDefinition.Collider>();
        for(var r:l.rooms())for(var c:DeckSlabs.cells(l,r))
            slabColliders.add(new LevelDefinition.Collider((c.x0()+c.x1())/2,c.bottom(),
                (c.z0()+c.z1())/2,(c.x1()-c.x0())/2,(c.z1()-c.z0())/2,c.top()-c.bottom(),0));
        return new HouseMap(current.site(),new LevelDefinition(l.id(),l.revision()+"-pre-depth-reproducer",
            l.title(),l.rooms(),l.portals(),l.stairs(),l.voids(),l.props(),
            l.sockets().stream().filter(s->!s.type().equals("deck_slab")).toList(),
            l.colliders().stream().filter(c->slabColliders.stream().noneMatch(s->
                Math.abs(c.x()-s.x())<1e-7&&Math.abs(c.y()-s.y())<1e-7&&
                Math.abs(c.z()-s.z())<1e-7&&Math.abs(c.halfX()-s.halfX())<1e-7&&
                Math.abs(c.halfZ()-s.halfZ())<1e-7&&Math.abs(c.height()-s.height())<1e-7&&
                Math.abs(c.yaw()-s.yaw())<1e-7)).toList(),l.dock(),l.ambience()));
    }
    static boolean old(HouseMap m,double ax,double ay,double az,double x,double y,double z){
        double dx=x-ax,dy=y-ay,dz=z-az,d=Math.sqrt(dx*dx+dy*dy+dz*dz),b=Math.min(.12,d*.2)/Math.max(.001,d);
        return m.clearLineOfSight(ax,ay,az,x-dx*b,y-dy*b,z-dz*b);
    }
    public static void main(String[] args){
        var current=BlackpineLevel.map();var map=beforePhysicalDeck(current);
        double fy=map.floorY(0),upper=map.floorY(11);
        check(!current.clearLineOfSight(48.5,fy+2.855,35,48.5,upper+.15,35),
            "current physical loft depth must block the original floor ray");
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
