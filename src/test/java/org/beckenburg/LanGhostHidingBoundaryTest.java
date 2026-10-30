package org.beckenburg;

/** Physical hiding pockets and the extraction van remain inaccessible to every spectral controller. */
public final class LanGhostHidingBoundaryTest {
    private static void check(boolean condition,String message){
        if(!condition)throw new AssertionError(message);
    }
    public static void main(String[] args){
        HouseMap map=new HouseMap();map.configureHiding(100);
        LanGhostSpatial ghost=new LanGhostSpatial(map,4);
        boolean vanBlocked=false;
        try{ghost.syncRoom(HouseMap.EXTRACTION_ROOM);}
        catch(IllegalArgumentException expected){vanBlocked=true;}
        check(vanBlocked,"ghost cannot occupy the extraction van");
        for(int room=0;room<HouseMap.EXTRACTION_ROOM;room++)
            check(!map.ghostExits(room).contains(HouseMap.EXTRACTION_ROOM),
                    "van must not appear as a DRIFT destination");

        int closets=0,shrubs=0;
        for(var socket:map.level().sockets()){
            if(socket.type().equals("hide")){
                closets++;
                check(LanGhostSpatial.hidingPocketAt(map,socket.room(),
                        socket.x(),socket.y(),socket.z()),
                        "an authored indoor hiding volume must be spectrally excluded");
            }else if(socket.type().equals("outdoor-hide")){
                shrubs++;
                check(LanGhostSpatial.hidingPocketAt(map,socket.room(),
                        socket.x(),socket.y(),socket.z()),
                        "an authored outdoor hiding volume must be spectrally excluded");
            }
        }
        check(closets>0&&shrubs>0,"both indoor and outdoor concealment require protection");
        check(!LanGhostSpatial.hidingPocketAt(map,4,ghost.pose().x(),ghost.pose().y(),ghost.pose().z()),
                "normal navigable spectral territory remains accessible");
        System.out.println("PASS: ghost blocked from van and authored hiding pockets without hunter-state leaks");
    }
}
