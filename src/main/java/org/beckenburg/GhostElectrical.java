package org.beckenburg;
/** Accepted ordinary electrical haunts use the same persistent breaker as hunter controls.
 * No species inference, second circuit state, source restart or battery mutation. */
public final class GhostElectrical {
    private GhostElectrical(){}
    public static boolean apply(HouseMap map,RoomLighting lights,GhostDomain.Resolution accepted,
            GhostHauntingFolio.Occurrence event,int tick){
        if(map==null||lights==null||accepted==null||event==null||!accepted.applied()
                ||accepted.action()!=GhostDomain.Action.DISTURB||!event.entry().id().equals("lightning_static")
                ||event.entry().action()!=accepted.action()||event.room()!=accepted.room()||event.startTick()!=tick
                ||accepted.room()<0||accepted.room()>=map.interiorRoomCount()||!lights.supplied()||!lights.on(accepted.room())
                ||map.level().sockets().stream().noneMatch(s->s.type().equals("breaker")))return false;
        lights.setBreaker(false);map.fixtures().power(false);return true;
    }
}
