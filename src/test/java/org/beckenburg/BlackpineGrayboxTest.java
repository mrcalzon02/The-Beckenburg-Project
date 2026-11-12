package org.beckenburg;
import java.util.*;
import org.beckenburg.LevelDefinition.*;

/** Check the first real camp shell, dimensions and physical approaches, not just room counts. */
public final class BlackpineGrayboxTest {
    private static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
    public static void main(String[] args){
        var map=BlackpineLevel.map();map.configureHiding(100);var l=map.level();
        for(var p:l.portals())if(p.door())map.toggleDoor(p.id());
        check(l.title().equals("Blackpine Logging Camp"),"distinct real level definition");
        check(l.room(6).x1()-l.room(6).x0()==6&&l.room(6).z1()-l.room(6).z0()==8,"cabin remains normal 6 by 8 metres");
        check(l.room(0).x1()-l.room(0).x0()==12&&l.room(0).z1()-l.room(0).z0()==18,"modest working mill scale");
        check(l.sockets().stream().filter(s->s.type().equals("outdoor-hide")).count()==3,"only three authored outdoor refuges across the camp");
        check(l.sockets().stream().noneMatch(s->s.type().equals("hide")),"no automatic cupboard in every building");
        check(l.sockets().stream().filter(s->s.type().equals("ritual")).map(Socket::room).distinct().count()==3,"three distinct defended rite rooms");
        check(map.valid(),"physical spawns and graph are valid");
        for(var portal:l.portals())if(portal.door()){
            var a=AuthoredTestSupport.side(map,portal,portal.a(),.5);var b=AuthoredTestSupport.side(map,portal,portal.b(),.5);
            check(map.resolveMove(a,b.x(),b.z())!=null,"real open threshold traversable: "+portal.id());
            map.toggleDoor(portal.id());check(map.resolveMove(a,b.x(),b.z())==null,"closed threshold blocks: "+portal.id());map.toggleDoor(portal.id());
        }
        var stair=l.stairs().get(0);Vec at=new Vec(stair.x0(),stair.y0(),stair.z0());
        for(int i=1;i<=120;i++){double t=i/120.;at=map.resolveMove(at,stair.x0()+(stair.x1()-stair.x0())*t,stair.z0()+(stair.z1()-stair.z0())*t);check(at!=null,"mill stair continuously supports ascent "+i);}
        check(Math.abs(at.y()-l.room(11).y())<.02,"actual upper deck elevation reached");
        // A body near the same deck lip but outside the authored stair may
        // NOT exploit the terminal-riser contact exception to enter the loft.
        double nearLanding=stair.z1()-.25,beside=stair.x1()+.65;
        // The actual rendered upper deck now has an architectural opening
        // above the final rising treads, not merely a collision waiver.
        check(l.floorVoid(stair.x1(),stair.y1(),nearLanding),
            "rising mill stair owns a true floor opening through the final riser");
        check(l.floorVoid(stair.x1(),stair.y1(),stair.z1()),
            "ramp endpoint remains under the actual stair aperture");
        check(!l.floorVoid(stair.x1(),stair.y1(),stair.z1()+.25),
            "solid inspection loft landing remains intact beyond the staircase");
        double contactZ=stair.z0()+(stair.z1()-stair.z0())*110/120.;
        double contactY=stair.yAt(stair.x0(),contactZ);
        check(!DeckSlabs.blocksBody(l,stair.x0(),contactY,contactZ,
                HunterPosture.STANDING_HEIGHT,.22),
            "step 110 capsule does not prematurely hit solid loft slab");
        double beneathDeck=stair.y1()-.125;
        check(DeckSlabs.blocksBody(l,beside,beneathDeck,nearLanding,
                HunterPosture.STANDING_HEIGHT,.22),
            "off-stair capsule remains blocked by real upper-deck structure");
        check(!DeckSlabs.blocksRay(l,stair.x1(),stair.y1()-.08,nearLanding),
            "actual stair opening remains optically open, not a decorative hole");
        // Near the final tread centre, the body's front touches intact deck
        // before the sole reaches the upper Y. Support and void adjacency both
        // authorize this, whereas the off-stair negative remains blocked.
        double lastRampZ=stair.z0()+(stair.z1()-stair.z0())*118/120.;
        double lastRampY=stair.yAt(stair.x0(),lastRampZ);
        check(!DeckSlabs.blocksBody(l,stair.x0(),lastRampY,lastRampZ,
                HunterPosture.STANDING_HEIGHT,.22),
            "supported final riser is clear across the deck aperture lip");
        for(int i=119;i>=0;i--){double t=i/120.;at=map.resolveMove(at,stair.x0()+(stair.x1()-stair.x0())*t,stair.z0()+(stair.z1()-stair.z0())*t);check(at!=null,"mill stair continuously supports descent "+i);}
        System.out.println("PASS: Blackpine dimensions, sparse refuge, physical doors, spawns, three rites and supported mill stairs");
    }
}
