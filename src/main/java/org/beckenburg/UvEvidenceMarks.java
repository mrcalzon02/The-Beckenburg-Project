package org.beckenburg;
import java.util.*;
/** Bounded host-authored residue at actual supported ghost positions. No room-wide invented decals. */
public final class UvEvidenceMarks {
    public record Mark(int id,int room,double x,double y,double z,int born,int normal){
        public Mark(int id,int room,double x,double y,double z,int born){this(id,room,x,y,z,born,2);}
        public Mark {if(id<0||room<0||room>=14||!Double.isFinite(x+y+z)||born<0||Math.abs(normal)<1||Math.abs(normal)>3)throw new IllegalArgumentException("invalid UV mark");}
        public String frame(){return "UVTRACE|"+id+"|"+room+"|"+x+"|"+y+"|"+z+"|"+born+"|"+normal;}
        public static Mark decode(List<String> p){if((p.size()!=7&&p.size()!=8)||!p.get(0).equals("UVTRACE"))throw new IllegalArgumentException("UV trace frame");return new Mark(Integer.parseInt(p.get(1)),Integer.parseInt(p.get(2)),Double.parseDouble(p.get(3)),Double.parseDouble(p.get(4)),Double.parseDouble(p.get(5)),Integer.parseInt(p.get(6)),p.size()==8?Integer.parseInt(p.get(7)):2);}
    }
    private final ArrayList<Mark> marks=new ArrayList<>();private int nextId;
    public void leave(HouseMap map,LanGhostSpatial.Pose ghost,int tick){
        if(ghost.room()<0||ghost.room()>=14)return;
        double floor=map.groundHeight(ghost.x(),ghost.z(),ghost.room());
        if(Math.abs(floor-ghost.y())>.4||!map.traversable(ghost.x(),floor,ghost.z()))return;
        if(!marks.isEmpty()){var last=marks.get(marks.size()-1);if(last.room()==ghost.room()&&Math.hypot(last.x()-ghost.x(),last.z()-ghost.z())<.45)return;}
        marks.add(new Mark(nextId++,ghost.room(),ghost.x(),floor+.012,ghost.z(),tick));
        marks.removeIf(m->tick-m.born()>240);while(marks.size()>96)marks.remove(0);
    }
    public List<Mark> visible(HouseMap map,LanWorld.Body viewer,EquipmentTiers.Tier tier,int tick,boolean powered){
        if(!powered)return List.of();double range=HandheldLighting.uv(tier).reach();
        int age=90+tier.ordinal()*35;double heading=Math.toRadians(viewer.heading());
        return marks.stream().filter(m->{double dx=m.x()-viewer.x(),dz=m.z()-viewer.z(),distance=Math.hypot(dx,dz);
            return tick>=m.born()&&tick-m.born()<=age&&Math.abs(m.y()-viewer.y())<.5
                &&Math.sqrt(distance*distance+1.6*1.6)<range
                &&(distance<.1||(dx*Math.sin(heading)+dz*Math.cos(heading))/distance>.60)
                &&map.clearLineOfSight(viewer.x(),viewer.y()+1.62,viewer.z(),m.x(),m.y()+.06,m.z());}).toList();
    }
}
