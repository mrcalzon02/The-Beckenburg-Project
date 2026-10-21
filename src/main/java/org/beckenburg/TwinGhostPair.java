package org.beckenburg;

import java.util.List;
import java.util.Objects;

/**
 * One supernatural identity, TWO server-positioned Twin actors. Member B is a
 * bounded follower/echo with an independently resolved physical room and facing.
 * No second GhostDomain, energy bar, action scheduler, rite or damage channel.
 */
public final class TwinGhostPair {
    private TwinGhostPair() {}

    public record Member(String id,LanGhostSpatial.Pose pose) {
        public Member {
            if(!id.equals("a")&&!id.equals("b"))throw new IllegalArgumentException("invalid Twin");
            Objects.requireNonNull(pose);
        }
        public String appearance(boolean ancient){return (ancient?"ancient_":"")+"twins_"+id;}
    }

    public static List<Member> position(HouseMap map,LanGhostSpatial.Pose primary,int tick) {
        Objects.requireNonNull(map);Objects.requireNonNull(primary);
        if(tick<0)throw new IllegalArgumentException("negative tick");
        double heading=Math.toRadians(primary.heading());
        // The paired echo shifts smoothly as the single host pose travels.
        // It can cross an accessible neighboring room when near an open doorway,
        // but never jump through a solid wall, a closed door or the safe van.
        double side=Math.cos(heading),forward=Math.sin(heading);
        double drift=Math.sin(tick*.035)*.42;
        double[][] offsets={
            {side*(1.8+drift),-forward*(1.8+drift)},
            {-side*1.65,forward*1.65},
            {side*.9,-forward*.9},
            {-side*.85,forward*.85}
        };
        LanGhostSpatial.Pose echo=null;
        for(double[] offset:offsets) {
            double xx=primary.x()+offset[0],zz=primary.z()+offset[1];
            int room=map.roomAt(xx,primary.y(),zz);
            if(room<0||room>=HouseMap.EXTRACTION_ROOM||!map.traversable(xx,primary.y(),zz))continue;
            if(!map.clearLineOfSight(primary.x(),primary.y()+1.1,primary.z(),
                    xx,primary.y()+1.1,zz))continue;
            echo=new LanGhostSpatial.Pose(room,xx,primary.y(),zz,
                Math.floorMod(primary.heading()+20,360));
            break;
        }
        if(echo==null) {
            // Never invent a geometry-penetrating second body.
            // A visible overlap is preferable to an unearned wall teleport.
            echo=new LanGhostSpatial.Pose(primary.room(),primary.x(),primary.y(),primary.z(),
                primary.heading());
        }
        return List.of(new Member("a",primary),new Member("b",echo));
    }

    public static String wire(Member member,int tick,boolean visible,boolean ancient) {
        if(tick<0)throw new IllegalArgumentException("negative tick");
        if(!visible)return "TWIN|"+member.id()+"|HIDDEN|"+tick;
        LanGhostSpatial.Pose p=member.pose();
        return "TWIN|"+member.id()+"|VISIBLE|"+Math.round(p.x()*1000)+
            "|"+Math.round(p.y()*1000)+"|"+Math.round(p.z()*1000)+
            "|"+p.heading()+"|"+tick+"|"+(ancient?"GREEN":"BASE");
    }
}
