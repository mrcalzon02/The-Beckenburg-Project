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
    private static final int SAMPLE_INTERVAL=10; // One supported footprint per second of travel.
    static final int MAX_VISIBLE=96;
    private final ArrayDeque<Mark> marks=new ArrayDeque<>();
    private int nextId;
    private Mark lastFootfall;

    static int lifetime(EquipmentTiers.Tier tier){
        return (3+tier.ordinal())*600; // 10 Hz host, three / four / five minutes.
    }
    static boolean detectable(Mark mark,EquipmentTiers.Tier tier,int tick){
        return tick>=mark.born()&&tick-mark.born()<=lifetime(tier);
    }
    void leaveAtSpawn(HouseMap map,int room,int tick){
        if(room<0||room>=HouseMap.EXTRACTION_ROOM)return;
        var pose=SafeAreaBarrier.ghostSpawn(map,room);
        if(pose!=null)leave(map,new LanGhostSpatial.Pose(room,pose.x(),pose.y(),pose.z(),0),tick);
    }
    public void leave(HouseMap map,LanGhostSpatial.Pose ghost,int tick){
        while(!marks.isEmpty()&&tick-marks.getFirst().born()>lifetime(EquipmentTiers.Tier.III))
            marks.removeFirst();
        if(ghost.room()<0||ghost.room()>=HouseMap.EXTRACTION_ROOM)return;
        if(lastFootfall!=null){
            if(tick-lastFootfall.born()<SAMPLE_INTERVAL)return;
            if(lastFootfall.room()==ghost.room()
                    &&Math.abs(lastFootfall.y()-ghost.y()-UvSurfaceDecal.DEPTH)<.1
                    &&Math.hypot(lastFootfall.x()-ghost.x(),lastFootfall.z()-ghost.z())<.45)return;
        }
        Double floor=map.supportNear(ghost.x(),ghost.z(),ghost.y());
        if(floor==null||map.roomAt(ghost.x(),floor,ghost.z())!=ghost.room()
                ||!map.traversable(ghost.x(),floor,ghost.z())
                ||SafeAreaBarrier.protectedAt(map,ghost.x(),floor,ghost.z()))return;
        // Retracing a floor contact refreshes that physical spot instead of stacking identical decals.
        double y=floor+UvSurfaceDecal.DEPTH;
        marks.removeIf(mark->mark.room()==ghost.room()&&Math.abs(mark.y()-y)<.1
            &&Math.hypot(mark.x()-ghost.x(),mark.z()-ghost.z())<.30);
        lastFootfall=new Mark(nextId++,ghost.room(),ghost.x(),y,ghost.z(),tick);
        // The cadence and oldest detectable age bound storage at 301 footprints for the whole site.
        marks.addLast(lastFootfall);
    }
    public List<Mark> visible(HouseMap map,LanWorld.Body viewer,EquipmentTiers.Tier tier,int tick,boolean powered){
        return visible(map,viewer,tier,tick,powered,false);
    }
    public List<Mark> visible(HouseMap map,LanWorld.Body viewer,EquipmentTiers.Tier tier,int tick,
                              boolean powered,boolean crouched){
        if(!powered)return List.of();
        double range=HandheldLighting.uv(tier).reach(),eye=viewer.y()+HunterPosture.eye(crouched);
        double heading=Math.toRadians(viewer.heading()),forwardX=Math.sin(heading),forwardZ=Math.cos(heading);
        List<Mark> visible=new ArrayList<>();
        // The renderer narrows this host-visible set to the local beam pitch.
        for(var it=marks.descendingIterator();it.hasNext()&&visible.size()<MAX_VISIBLE;){
            var mark=it.next();
            if(mark.born()>tick)continue;
            if(!detectable(mark,tier,tick))break;
            double dx=mark.x()-viewer.x(),dz=mark.z()-viewer.z(),dy=mark.y()-eye,distance=Math.hypot(dx,dz);
            if(distance*distance+dy*dy<range*range
                    &&(distance<.1||(dx*forwardX+dz*forwardZ)/distance>.60)
                    &&map.clearLineOfSight(viewer.x(),eye,viewer.z(),mark.x(),mark.y()+.06,mark.z()))
                visible.add(mark);
        }
        return List.copyOf(visible);
    }
}
