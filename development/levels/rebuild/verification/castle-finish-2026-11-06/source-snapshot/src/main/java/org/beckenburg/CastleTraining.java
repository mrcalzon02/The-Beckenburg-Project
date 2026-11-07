package org.beckenburg;

import java.util.List;
import java.util.Map;

/** Peaceful annex lessons bound to native fixtures, never a live investigation. */
public final class CastleTraining {
    private CastleTraining(){}
    public record Bay(String room,GhostIdentity.Rite rite,GhostIdentity.Type example,double x){
        public Bay{java.util.Objects.requireNonNull(room);java.util.Objects.requireNonNull(rite);java.util.Objects.requireNonNull(example);if(example.rite()!=rite||!Double.isFinite(x))throw new IllegalArgumentException("Invalid training profile");}
        public String tableId(){return "training-table-"+room;}
        public String markerId(){return "training-marker-"+room;}
        public String title(){return rite.name()+" PRACTICE";}
    }
    public static final List<Bay> BAYS=List.of(
        new Bay("train-b",GhostIdentity.Rite.BANISHMENT,GhostIdentity.Type.POLTERGEIST,-14),
        new Bay("train-c",GhostIdentity.Rite.CLEANSING,GhostIdentity.Type.SHADE,-8),
        new Bay("train-a",GhostIdentity.Rite.ABSOLUTION,GhostIdentity.Type.SPIRIT,-2));
    private static void prop(Map<String,CastleLevelEdits.Prop> props,String id,String mesh,double x,double z){
        double[] b=SceneRenderer.castleBounds(mesh);
        props.put(id,new CastleLevelEdits.Prop(id,mesh,x,0,z,1,0,b[3]/2,b[5]/2,false));
    }
    public static void installDefaults(Map<String,CastleLevelEdits.Prop> props){
        for(var bay:BAYS){
            prop(props,bay.tableId(),"level_workbench",bay.x(),16.3);
            prop(props,bay.markerId(),"level_castle_training_"+bay.rite().name().toLowerCase(java.util.Locale.ROOT),bay.x(),20);
        }
    }
    public static Bay aimed(CastleHubMap map,double x,double feet,double z,double yaw){
        if(map==null||Math.abs(feet)>.3||!Double.isFinite(x+feet+z+yaw)||!map.traversable(x,z,feet))return null;
        var here=CastleRooms.at(x,feet,z);if(here==null)return null;
        var props=CastleLevelEdits.current().props();
        for(var bay:BAYS){
            if(!here.id().equals(bay.room()))continue;
            var table=props.stream().filter(p->p.id().equals(bay.tableId())).findFirst().orElse(null);
            var marker=props.stream().filter(p->p.id().equals(bay.markerId())).findFirst().orElse(null);
            if(table==null||marker==null||Math.abs(table.y())>.01||Math.abs(marker.y())>.01)continue;
            var tableRoom=CastleRooms.at(table.x(),0,table.z());var markerRoom=CastleRooms.at(marker.x(),0,marker.z());
            if(tableRoom==null||markerRoom==null||!tableRoom.id().equals(bay.room())||!markerRoom.id().equals(bay.room()))continue;
            double dx=table.x()-x,dz=table.z()-z,d=Math.hypot(dx,dz);
            if(d<.65||d>2.6||(dx*Math.sin(yaw)+dz*Math.cos(yaw))/d<.88)continue;
            double targetY=table.y()+SceneRenderer.castleBounds(table.mesh())[4]*table.scale()+.08;
            if(FixtureLighting.castleRayClear(map,x,feet+1.62,z,table.x(),targetY,table.z()))return bay;
        }
        return null;
    }
    public enum Step { IDENTIFY, WARD, INSTALLATION, COMPLETE }
    public record Reply(boolean accepted,String message){}
    /** Local knowledge rehearsal only. No profile, inventory, host or case reference exists. */
    public static final class Lesson {
        private final Bay bay;private Step step=Step.IDENTIFY;
        public Lesson(Bay bay){this.bay=java.util.Objects.requireNonNull(bay);}
        public Bay bay(){return bay;}public Step step(){return step;}
        public void reset(){step=Step.IDENTIFY;}
        public Reply identify(GhostIdentity.Rite rite){
            if(step!=Step.IDENTIFY)return new Reply(false,"Start with the profile card.");
            if(rite!=bay.example().rite())return new Reply(false,"Review the profile card and choose its required rite.");
            step=Step.WARD;return new Reply(true,"Correct. Prepare the anchor ward before beginning the rite.");
        }
        public Reply ward(){
            if(step!=Step.WARD)return new Reply(false,"Identify the required rite first.");
            step=Step.INSTALLATION;return new Reply(true,"Ward preparation rehearsed. Choose the correct installation.");
        }
        public Reply installation(GhostIdentity.Rite rite){
            if(step!=Step.INSTALLATION)return new Reply(false,"Prepare the anchor ward first.");
            if(rite!=bay.rite())return new Reply(false,"Choose the installation for this rite.");
            step=Step.COMPLETE;return new Reply(true,"Rehearsal complete. During a case, remain at the prepared installation and protect the team.");
        }
    }
}
