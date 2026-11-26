package org.beckenburg;

/** Real room-threshold material and open/closed-door regression tests. */
public final class LanAcousticsTest {
    static void check(boolean pass, String why) {if(!pass)throw new AssertionError(why);}
    private static void checkCargoShutter(){
        var map=new HouseMap(HouseMap.Site.MONASTERY);
        var van=map.van();
        var outside=van.world(0,VanStaging.FRONT-1.65);
        var inside=van.world(0,VanStaging.FRONT+.60);
        double outsideY=map.groundHeight(outside.x(),outside.z(),13);
        var listener=new LanWorld.Body(inside.x(),van.floorY(),inside.z(),0,HouseMap.EXTRACTION_ROOM);
        check(map.roomAt(outside.x(),outsideY,outside.z())==13,"Expected the Monastery's real apron");
        var step=LanSound.project(map,listener,13,outside.x(),outsideY,outside.z(),LanSound.Cue.STEP,10);
        check(step!=null,"An open cargo shutter admits nearby apron footsteps");
        var openMotor=LanSound.project(map,listener,13,outside.x(),outsideY,outside.z(),LanSound.Cue.VAN_DOOR,10);
        van.setCargoDoorClosed(true);
        check(LanSound.project(map,listener,13,outside.x(),outsideY,outside.z(),LanSound.Cue.STEP,10)==null,
            "A shut cargo door suppresses nearby quiet footsteps");
        var closedMotor=LanSound.project(map,listener,13,outside.x(),outsideY,outside.z(),LanSound.Cue.VAN_DOOR,10);
        check(openMotor!=null&&closedMotor!=null&&closedMotor.volume()<openMotor.volume(),
            "Loud machinery still carries through the shutter with lower gain");
        van.setCargoDoorClosed(false);
        check(LanSound.project(map,listener,13,outside.x(),outsideY,outside.z(),LanSound.Cue.STEP,10).equals(step),
            "Reopening the same shutter restores transmission");
        check(LanAcoustics.cost(map,13,HouseMap.EXTRACTION_ROOM)==Integer.MAX_VALUE,
            "The van remains acoustically excluded from ghost tracking");
    }
    public static void main(String[] args) {
        HouseMap map=new HouseMap();
        check(LanAcoustics.cost(map,4,4)==0,"co-location has no sound loss");
        check(LanAcoustics.cost(map,3,4)==3,"closed partition must attenuate sound");
        check(!LanAcoustics.audible(map,3,4,LanGhostPursuit.Sound.FOOTSTEP),
                "quiet footsteps must not pierce a closed door");
        check(LanAcoustics.audible(map,3,4,LanGhostPursuit.Sound.EQUIPMENT),
                "loud evidence tool may be heard through one closed door");
        var door=AuthoredTestSupport.door(map,3,4);var p=AuthoredTestSupport.side(map,door,3,.7);
        check(map.toggleNearestDoor(p.x(),p.y(),p.z()),"open the actual hall/dining door");
        check(map.doorOpen(door.id()),"the intended portal opened");
        check(LanAcoustics.cost(map,3,4)==1,"open door must transmit footsteps");
        check(LanAcoustics.audible(map,3,4,LanGhostPursuit.Sound.FOOTSTEP),
                "quiet footsteps are now audible through the open threshold");
        check(map.toggleNearestDoor(p.x(),p.y(),p.z()),"close threshold");
        check(LanAcoustics.cost(map,3,4)==3,"closing reestablishes attenuation");
        check(LanAcoustics.cost(map,7,10)>=4,"stone cellar absorbs more sound than normal doors");
        check(LanAcoustics.cost(map,3,10)>LanAcoustics.cost(map,3,4),
                "distance and masonry affect room-to-room audibility");
        check(LanAcoustics.cost(map,12,1)>=2,
            "rear garden sound crosses the physical drawing-room doorway");
        check(LanAcoustics.cost(map,13,14)==Integer.MAX_VALUE,"van safe zone must never be acoustically tracked");
        check(!LanAcoustics.audible(map,4,HouseMap.EXTRACTION_ROOM,LanGhostPursuit.Sound.RITUAL),
                "no sound reveals a hunter inside the safe van");
        check(LanAcoustics.edgeCost(map,7,10)==Integer.MAX_VALUE,
            "nonadjacent cellar-stair shortcut rejected");
        checkCargoShutter();
        System.out.println("PASS: door-dependent, cargo shutter, masonry and exterior acoustic attenuation");
    }
}
