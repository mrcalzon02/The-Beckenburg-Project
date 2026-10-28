package org.beckenburg;
import java.util.*;
/** Exercise the production light sampler, not only the standalone ray helper. */
public final class FixtureJoinIntegrationTest {
    static HouseMap source(HouseMap base,boolean enabled){return source(base,enabled,39);}
    static HouseMap source(HouseMap base,boolean enabled,double sourceX){return source(base,enabled,sourceX,23);}
    static HouseMap source(HouseMap base,boolean enabled,double sourceX,double sourceZ){
        var l=base.level();var sockets=new ArrayList<LevelDefinition.Socket>();
        for(var s:l.sockets())if(!s.type().equals("light"))sockets.add(s);
        if(enabled)sockets.add(new LevelDefinition.Socket("join-reproducer","light",0,sourceX,base.floorY(0)+1.865,sourceZ,0,"bare-bulb"));
        return new HouseMap(base.site(),new LevelDefinition(l.id(),l.revision()+"-join-light-"+enabled,l.title(),l.rooms(),l.portals(),l.stairs(),l.voids(),l.props(),sockets,l.colliders(),l.dock(),l.ambience()));
    }
    public static void main(String[] args){
        var base=BlackpineLevel.map();double fy=base.floorY(0);
        var on=new FixtureLighting(source(base,true));var off=new FixtureLighting(source(base,false));
        on.frame(new RoomLighting(),0);off.frame(new RoomLighting(),0);
        double lit=on.sample(0,40.08,fy+1.3,23).energy(),ambient=off.sample(0,40.08,fy+1.3,23).energy();
        if(Math.abs(lit-ambient)>1e-8)throw new AssertionError("Production sampler leaked exterior light through wall endpoint: "+lit+" versus "+ambient);
        var inside=new FixtureLighting(source(base,true,41));inside.frame(new RoomLighting(),0);
        if(inside.sample(0,40.2,fy+1.3,23).energy()<=off.sample(0,40.2,fy+1.3,23).energy()+.1)throw new AssertionError("Same-side interior light was incorrectly suppressed");
        var doorway=source(base,true,39,25);var door=doorway.level().portals().stream().filter(p->p.door()&&p.joins(0,12)&&p.x()==40).findFirst().orElseThrow();
        var throughDoor=new FixtureLighting(doorway);throughDoor.frame(new RoomLighting(),0);
        double closed=throughDoor.sample(0,40.2,fy+1.3,25).energy();doorway.toggleDoor(door.id());throughDoor.frame(new RoomLighting(),1);
        if(throughDoor.sample(0,40.2,fy+1.3,25).energy()<=closed+.1)throw new AssertionError("Actual open doorway failed to invalidate and transmit light");
        System.out.println("PASS actual FixtureLighting wall endpoint isolation; enabled exterior source cannot light the inner face");
    }
}
