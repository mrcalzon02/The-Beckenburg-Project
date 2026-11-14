package org.beckenburg;

/** Boundary and replay prerequisites for a spectral viewpoint with the same DRIFT gates as AI. */
public final class LanGhostSpatialTest {
    private static void ok(boolean v,String s){if(!v)throw new AssertionError(s);}
    private static void fails(Runnable f,String message){try{f.run();throw new AssertionError("allowed "+message);}catch(IllegalArgumentException expected){}}
    public static void main(String[] args){
        HouseMap map=new HouseMap();LanGhostSpatial view=new LanGhostSpatial(map,4);
        ok(view.pose().room()==4&&map.roomAt(view.pose().x(),view.pose().z())==4,"initial pose");
        ok(view.listening(0),"Stationary ghost initially hears the physical world");
        fails(()->new LanGhostSpatial(map,HouseMap.EXTRACTION_ROOM),"van ghost spawn");
        fails(()->view.step(0,0,1),"zero movement");
        fails(()->view.step(999,0,1),"oversized movement");
        double before=view.pose().x();
        ok(view.step(100,0,2)&&view.pose().x()>before,"local spectral position updated");
        ok(view.movementGateMetres()>=.035-1e-9&&view.movementGateMetres()<=.075+1e-9,
            "Host movement tolerance stays within 5.5cm +/-2cm");
        ok(!LanGhostSpatial.significantMotion(.018,.030,.055)
                &&LanGhostSpatial.significantMotion(.018,.085,.055),
            "Single noisy displacement is tolerated, cumulative movement is not");
        int settledAt=2+view.listeningSettleTicks();
        ok(view.listeningSettleTicks()>=3&&view.listeningSettleTicks()<=5,
            "Listen settle duration stays within four ticks +/- one");
        ok(!view.listening(settledAt-1)&&view.listening(settledAt),
            "Real spectral movement mutes until host-selected settle threshold");
        view.turn(90);
        ok(view.listening(settledAt),"Turning in place does not count as movement");
        fails(()->view.step(100,0,2),"same-tick speed bypass");
        fails(()->view.turn(-1),"invalid orientation");
        for(int i=3;i<45;i++){try{view.step(100,0,i);}catch(IllegalArgumentException ignored){}}
        ok(view.pose().room()==4,"spectral stepping may not change rooms");
        fails(()->view.syncRoom(HouseMap.EXTRACTION_ROOM),"safe van ingress");
        view.syncRoom(1);
        ok(view.pose().room()==1&&Math.abs(view.pose().x()-map.centerX(1))<.001,"legitimate drift resets camera");
        ok(view.frame(45).startsWith("GPOSE|1|"),"bounded spectral packet");
        view.markTravel(45);
        ok(!view.listening(45)&&view.listening(45+view.listeningSettleTicks()),
            "Room DRIFT resets listening until the varied host settle threshold");
        fails(()->view.markTravel(-1),"invalid travel tick");
        System.out.println("PASS: spectral pose bounded to room, fixed tick, safe van denied, drift resets pose");
    }
}
