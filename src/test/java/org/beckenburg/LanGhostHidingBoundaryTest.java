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
        for(int density=0;density<=100;density++){
            map.configureHiding(density);
            for(var socket:map.level().sockets())if(socket.type().equals("hide")||socket.type().equals("outdoor-hide")){
                boolean available=socket.type().equals("hide")?LevelClosets.available(map.level(),socket.room(),density):OutdoorHiding.available(map.level(),socket,density);
                check(LanGhostSpatial.hidingPocketAt(map,socket.room(),socket.x(),socket.y(),socket.z())==available,
                    "spectral cover must match actual budgeted/visible pocket "+density+" / "+socket.id());
                if(density==100&&available){if(socket.type().equals("hide"))closets++;else shrubs++;}
            }
        }
        check(closets>0&&shrubs>0,"both indoor and outdoor concealment require protection");
        map.configureHiding(100);
        check(!LanGhostSpatial.hidingPocketAt(map,4,ghost.pose().x(),ghost.pose().y(),ghost.pose().z()),
                "normal navigable spectral territory remains accessible");
        System.out.println("PASS: ghost blocked from van and authored hiding pockets without hunter-state leaks");
    }
}
