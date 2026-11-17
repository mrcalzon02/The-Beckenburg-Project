package org.beckenburg;

import org.beckenburg.CampaignGuestBot.Action;
import org.beckenburg.CampaignGuestBot.Input;

/** Private visual attention of a single hunter. Subscription acknowledgments never establish physical line of sight. */
final class HunterCameraAttention {
    static final int NONE=-1,LENS=-2;
    private final VanStaging van;
    private final VanStaging.Position screen;
    private int requested=NONE,confirmed=NONE,lastRequest=-1000;
    private boolean release;
    HunterCameraAttention(HouseMap map){
        van=map.van();
        var display=VanInteriorLayout.FIXTURES.stream().filter(f->f.purpose().equals("camera feed")).findFirst().orElseThrow();
        screen=van.world(display.localX(),display.localZ());
    }
    int screenHeading(LanWorld.Body b){
        return Math.floorMod((int)Math.round(Math.toDegrees(Math.atan2(screen.x()-b.x(),screen.z()-b.z()))),360);
    }
    boolean facingScreen(LanWorld.Body b){
        return b!=null&&b.room()==HouseMap.EXTRACTION_ROOM&&van.stationAt(b.x(),b.z())==VanStaging.Station.RELAY
            &&Math.abs(Math.floorMod(screenHeading(b)-b.heading()+180,360)-180)<=15;
    }
    void frame(String f){
        if(!f.startsWith("ACCEPT|WATCH|"))return;
        var p=LanProtocol.parts(f);
        int received=switch(p.get(2)){
            case "CAMERA" -> Integer.parseInt(p.get(3));
            case "LENS" -> LENS;
            default -> NONE;
        };
        if(received==requested)confirmed=received;
    }
    boolean remote(int owner,LanWorld.Body self){
        return confirmed==owner&&requested==owner&&owner>=0&&facingScreen(self);
    }
    boolean lens(){return requested==LENS&&confirmed==LENS;}
    boolean lookingAtDevice(){return requested!=NONE;}
    void stop(){
        if(requested!=NONE)release=true;
        requested=NONE;confirmed=NONE;
    }
    Input select(int tick,int target){
        if(requested!=target||confirmed!=target&&tick-lastRequest>=30){
            requested=target;confirmed=NONE;lastRequest=tick;release=false;
            return Input.watch(target);
        }
        return Input.simple(Action.WAIT);
    }
    Input screen(int tick,int owner,LanWorld.Body self){
        if(!facingScreen(self))return Input.turn(screenHeading(self));
        return select(tick,owner);
    }
    Input apply(Input input){
        switch(input.action()){
            case WATCH, WAIT, RADIO, SHUTTER -> { }
            default -> stop();
        }
        if(release){release=false;return input.releasingView();}
        return input;
    }
}
