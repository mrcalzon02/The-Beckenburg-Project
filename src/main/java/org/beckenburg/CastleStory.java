package org.beckenburg;

import java.util.*;

/** Retained museum records, read from real native exhibits in the protected HQ. */
public final class CastleStory {
    private CastleStory(){}
    public record Record(String title,String mesh,List<String> paragraphs){
        public Record{paragraphs=List.copyOf(paragraphs);}
    }
    private static Record record(String title,String mesh,String... paragraphs){return new Record(title,mesh,List.of(paragraphs));}
    public static final Map<String,Record> RECORDS=Map.of(
        "museum-armour-caption",record("A reconstructed harness","level_castle_armour_label",
            "This display brings surviving armour pieces together as a field harness. The collection label records the reconstruction made for the old museum.",
            "The velvet barrier survived the restoration. The harness remains in the gallery among the working base's records, a remnant of the rooms' earlier public life."),
        "heritage-restoration-painting",record("Before the roof returned","level_castle_restoration_painting",
            "The picture preserves the western tower before reroofing. Its worn frame belongs to the older museum display.",
            "Restoration made selected rooms usable again. The occupied wings received working services; closed sections remain outside the base's everyday circulation. Old fabric, conserved interiors and modern repairs still meet at their boundaries."),
        "heritage-archive-tapestry",record("A hanging saved from storage","level_castle_tapestry",
            "The heraldic hanging survived the museum's neglected years. A modern support sleeve carries the cloth instead of asking the old weave to bear its own weight.",
            "Its position in the working archive keeps the former collection visible among the base's records. Worn fabric and repaired support belong to different periods of the same room."),
        "heritage-common-fireplace",record("The cold hearth","level_castle_great_fireplace",
            "The limestone hearth was conserved as part of the old household interior. The historic flue is capped; this is a cold fireplace.",
            "The working base uses the service plant for heat. The conserved ashlar and heraldry keep the common room's earlier household life visible around the present dining table."),
        "finish-keeper-statue",record("The keeper and the codex","level_castle_keeper_statue",
            "An anonymous stone keeper holds a closed codex. The carving remained with the museum collection through the Castle's decline and partial restoration.",
            "Its repaired display base and retained rope barrier mark the old collection within the working archive. The keeper's closed book echoes the records now handled nearby."),
        "heritage-archive-arms",record("Arms on new supports","level_castle_heraldic_arms",
            "A painted collection shield and crossed swords remain in the archive. Modern steel saddles carry their weight against the old masonry.",
            "The modern supports and the retained display record two periods of work on the same wall. The archive's present custodians continue to care for the old collection.")
    );
    public static Record aimed(CastleHubMap map,double x,double feet,double z,double yaw,double pitch,double eye){
        if(map==null||!Double.isFinite(x+feet+z+yaw+pitch+eye)||eye<.8||eye>1.7||Math.abs(pitch)>1.4||!map.traversable(x,z,feet))return null;
        double dx=Math.sin(yaw)*Math.cos(pitch),dy=Math.sin(pitch),dz=Math.cos(yaw)*Math.cos(pitch),y=feet+eye;
        var hit=CastleObjectRay.nearest(x,y,z,dx,dy,dz,3.2);if(hit==null)return null;
        var record=RECORDS.get(hit.prop().id());if(record==null||!record.mesh().equals(hit.prop().mesh()))return null;
        double d=Math.max(0,hit.distance()-.01);
        return FixtureLighting.castleRayClear(map,x,y,z,x+dx*d,y+dy*d,z+dz*d)?record:null;
    }
}
