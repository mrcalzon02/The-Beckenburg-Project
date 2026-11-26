package org.beckenburg;

import java.util.*;

/** One original environmental sequence, independent of ghost mechanics and case evidence. */
final class HouseEncounter {
    static final int EARLIEST=100,NOTICE_TICKS=20,EMPTY_TICKS=15,TIMEOUT=600;
    private static final String TRAIN="wooden-train";
    enum Phase { DISABLED,UNNOTICED,NOTICED,TURNED,ABORTED }
    record Sound(int room,double x,double y,double z,LanSound.Cue cue){}
    private final HouseMap map;private final LooseClutter.State anchor;private final int room;
    private Phase phase;private int owner=-1,noticedAt=-1,lastTick=-1,dwell,empty;
    HouseEncounter(HouseMap map){
        this.map=Objects.requireNonNull(map);anchor=map.clutter().authoredItem(TRAIN);
        var source=map.level().sockets().stream().filter(s->s.id().equals(TRAIN)&&s.type().equals("loose-object")).findFirst().orElse(null);
        room=source==null?-1:source.room();
        phase=map.site()==HouseMap.Site.HOUSE&&map.level().id().equals("briar-rebuild")&&anchor!=null
            &&anchor.kind()==LooseClutter.Kind.TOY&&source!=null?Phase.UNNOTICED:Phase.DISABLED;
    }
    Phase phase(){return phase;}
    void cancel(){if(phase==Phase.UNNOTICED||phase==Phase.NOTICED)phase=Phase.ABORTED;}
    private boolean unchanged(LooseClutter.State toy){return toy!=null&&toy.id()==anchor.id()&&toy.holder()==-1
        &&Math.hypot(toy.x()-anchor.x(),toy.z()-anchor.z())<.015&&Math.abs(toy.y()-anchor.y())<.02
        &&Math.abs(toy.yaw()-anchor.yaw())<.025&&Math.abs(toy.roll())<.025;}
    /** Conservative full-object visibility: even a visible bounding corner blocks the change. */
    boolean visible(LooseClutter.Observer observer,LooseClutter.State toy){
        if(map.clearLineOfSight(observer.x(),observer.eyeY(),observer.z(),toy.x(),toy.y(),toy.z()))return true;
        var b=SceneRenderer.castleBounds(toy.kind().mesh());double c=Math.cos(toy.yaw()),s=Math.sin(toy.yaw());
        for(double x:new double[]{b[0]-b[3]/2,b[0]+b[3]/2})for(double z:new double[]{b[2]-b[5]/2,b[2]+b[5]/2})for(double y:new double[]{b[1],b[1]+b[4]})
            if(map.clearLineOfSight(observer.x(),observer.eyeY(),observer.z(),toy.x()+x*c-z*s,toy.y()-toy.kind().half+y,toy.z()+x*s+z*c))return true;
        return false;
    }
    Sound advance(int tick,List<LooseClutter.Observer> connectedLivingHunters){
        if(tick<=lastTick)return null;lastTick=tick;
        if(phase==Phase.DISABLED||phase==Phase.TURNED||phase==Phase.ABORTED)return null;
        var toy=map.clutter().authoredItem(TRAIN);
        if(!unchanged(toy)){phase=Phase.ABORTED;return null;}
        var observers=List.copyOf(connectedLivingHunters);
        if(phase==Phase.UNNOTICED){
            if(tick<EARLIEST)return null;
            var candidate=observers.stream().filter(o->o.exposed()&&o.seat()>=0&&o.seat()<6
                &&map.roomAt(o.x(),o.y(),o.z())==room&&Math.hypot(o.x()-toy.x(),o.z()-toy.z())<=2.3&&visible(o,toy))
                .min(Comparator.comparingInt(LooseClutter.Observer::seat)).orElse(null);
            if(candidate==null){owner=-1;dwell=0;return null;}
            if(owner!=candidate.seat()){owner=candidate.seat();dwell=0;}
            if(++dwell>=NOTICE_TICKS){phase=Phase.NOTICED;noticedAt=tick;}
            return null;
        }
        if(tick-noticedAt>TIMEOUT||observers.stream().noneMatch(o->o.seat()==owner)){phase=Phase.ABORTED;return null;}
        if(observers.stream().anyMatch(o->map.roomAt(o.x(),o.y(),o.z())==room||visible(o,toy))){empty=0;return null;}
        if(++empty<EMPTY_TICKS)return null;
        if(!map.clutter().turnRestingAuthoredToy(TRAIN,anchor,Math.PI/2)){phase=Phase.ABORTED;return null;}
        phase=Phase.TURNED;return new Sound(room,toy.x(),toy.y(),toy.z(),LanSound.Cue.TOY_WINDUP);
    }
}
