package org.beckenburg;

import org.beckenburg.LevelDefinition.Vec;

/** Real host movement and its next tick must agree on already valid support. */
public final class GroundRecoverySupportParityTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void main(String[] args){
        var map=new HouseMap();var world=new LanWorld(map);world.configure(CustomContract.standard(3));world.setRoom(0,4);
        var start=world.body(0);
        for(int direction:new int[]{100,-100})for(int step=0;step<7;step++){
            int tick=(direction==100?10:40)+step*2;
            world.move(0,direction,0,tick);var accepted=world.body(0);
            check(map.traversable(accepted.x(),accepted.y(),accepted.z()),"Accepted move is supported by native collision");
            world.advanceStamina(tick+1);
            check(world.body(0).equals(accepted),"Ground recovery changed an accepted native movement at "+accepted+" into "+world.body(0));
        }
        var returned=world.body(0);
        check(Math.abs(returned.y()-start.y())<.01&&Math.hypot(returned.x()-start.x(),returned.z()-start.z())<.01,
            "Real host round trip preserves the original floor and position");
        var underStair=new Vec(22.24,0,18);
        check(map.traversable(underStair.x(),underStair.y(),underStair.z()),"Existing collision permits this lower standing surface");
        check(map.groundHeight(underStair.x(),underStair.z(),4)>underStair.y()+1,"Reproducer contains a different nominal stair support above feet");
        check(GroundRecovery.recover(map,underStair,4,HunterPosture.STANDING_HEIGHT).equals(underStair),
            "Recovery uses the same position-relative support as walking, not a higher nominal stair");
        for(double depth:new double[]{.06,10}){
            var fixed=GroundRecovery.recover(map,new Vec(start.x(),start.y()-depth,start.z()),4,HunterPosture.STANDING_HEIGHT);
            check(map.traversable(fixed.x(),fixed.y(),fixed.z())&&fixed.y()>=start.y()-.02,"Actual penetration still recovers to valid physical support");
        }
        for(var site:AuthoredTestSupport.SITES){
            var other=new HouseMap(site);
            for(var room:other.level().rooms()){
                var pose=room.spawn();
                check(GroundRecovery.recover(other,pose,room.id(),HunterPosture.STANDING_HEIGHT).equals(pose),
                    "Valid authored spawn changed floor on "+site+"/"+room.id());
            }
        }
        System.out.println("PASS native support parity: accepted host moves survive next tick, stair overlap preserves lower support, round trip and real penetration recovery");
    }
}
