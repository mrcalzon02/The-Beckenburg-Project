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
    private static final int REFRESH_TICKS=30, RETAIN_TICKS=240, MAX_MARKS=96;
    private final ArrayList<Mark> marks=new ArrayList<>();
    private int nextId;

    void leaveAtSpawn(HouseMap map,int room,int tick){
        if(room<0||room>=HouseMap.EXTRACTION_ROOM)return;
        var pose=SafeAreaBarrier.ghostSpawn(map,room);
        if(pose!=null)leave(map,new LanGhostSpatial.Pose(room,pose.x(),pose.y(),pose.z(),0),tick);
    }
    public void leave(HouseMap map,LanGhostSpatial.Pose ghost,int tick){
        if(ghost.room()<0||ghost.room()>=HouseMap.EXTRACTION_ROOM)return;
        Double floor=map.supportNear(ghost.x(),ghost.z(),ghost.y());
        if(floor==null||map.roomAt(ghost.x(),floor,ghost.z())!=ghost.room()
            ||!map.traversable(ghost.x(),floor,ghost.z())
            ||SafeAreaBarrier.protectedAt(map,ghost.x(),floor,ghost.z()))return;

        marks.removeIf(mark->tick-mark.born()>RETAIN_TICKS);
        double y=floor+UvSurfaceDecal.DEPTH;
        for(int i=marks.size()-1;i>=0;i--){
            var mark=marks.get(i);
            if(mark.room()!=ghost.room()||Math.abs(mark.y()-y)>.14
                ||Math.hypot(mark.x()-ghost.x(),mark.z()-ghost.z())>=.45)continue;
            if(tick-mark.born()<REFRESH_TICKS)return;
            marks.remove(i);
            break;
        }
        marks.add(new Mark(nextId++,ghost.room(),ghost.x(),y,ghost.z(),tick));
        if(marks.size()>MAX_MARKS)marks.remove(0);
    }
    public List<Mark> visible(HouseMap map,LanWorld.Body viewer,EquipmentTiers.Tier tier,int tick,boolean powered){
        return visible(map,viewer,tier,tick,powered,false);
    }
    public List<Mark> visible(HouseMap map,LanWorld.Body viewer,EquipmentTiers.Tier tier,int tick,
                              boolean powered,boolean crouched){
        if(!powered)return List.of();
        double range=HandheldLighting.uv(tier).reach();
        double eye=viewer.y()+HunterPosture.eye(crouched);
        double heading=Math.toRadians(viewer.heading()),sin=Math.sin(heading),cos=Math.cos(heading);
        int age=90+tier.ordinal()*35;
        // The renderer narrows this set to the local beam pitch.
        return marks.stream().filter(mark->{
            double dx=mark.x()-viewer.x(),dz=mark.z()-viewer.z(),dy=mark.y()-eye;
            double distance=Math.hypot(dx,dz);
            return tick>=mark.born()&&tick-mark.born()<=age
                &&distance*distance+dy*dy<range*range
                &&(distance<.1||(dx*sin+dz*cos)/distance>.60)
                &&map.clearLineOfSight(viewer.x(),eye,viewer.z(),mark.x(),mark.y()+.06,mark.z());
        }).toList();
    }
}
