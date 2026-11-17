package org.beckenburg;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import org.beckenburg.CampaignGuestBot.Action;
import org.beckenburg.CampaignGuestBot.Input;
import org.beckenburg.GhostFieldGuide.FigureView;

/** A hunter's radio-coordinated room test. All positions, volunteers and feeds come from its own socket. */
final class HunterWitnessExperiment {
    enum Formation {
        EMPTY(0), SOLO(1), GROUP(2);
        final int minimum;
        Formation(int minimum){this.minimum=minimum;}
        boolean fits(int count){return this==GROUP?count>=2:count==minimum;}
        String word(){return name().toLowerCase(Locale.ROOT);}
    }
    private static final int VOLUNTEER_TICKS=30, WATCH_TICKS=260, SETUP_LIMIT=1450;
    private final HouseMap map;
    private final HunterCameraAttention attention;
    private final HunterDiagnosticEquipment gear;
    private final HunterFieldNavigation nav;
    private final int[] rooms=new int[6];
    private final boolean[] down=new boolean[6],crew=new boolean[6],bringsCamera=new boolean[6],powered=new boolean[6];
    private final LanVideoCameras.Camera[] cameras=new LanVideoCameras.Camera[6];
    private final int[][] lastTest=new int[HouseMap.INDOOR_ROOMS][Formation.values().length];
    private Formation mode;
    private int room,start,lastFinish=-1000,lastUse=-1000,watchSince=-1,selectedFeed=-1,setupHand=-1,ownSeat=-1,lastTurn=-1000;
    private int lastSelect=-1000;
    private boolean atMonitor,readyReported,fieldObserver,usingVideo,rolesReported;
    HunterWitnessExperiment(HouseMap map,HunterDiagnosticEquipment gear,HunterFieldNavigation nav,HunterCameraAttention attention){
        this.attention=attention;this.map=map;this.gear=gear;this.nav=nav;
        Arrays.fill(rooms,-1);
        for(int[] tests:lastTest)Arrays.fill(tests,-10000);
    }
    boolean active(){return mode!=null;}
    void stop(int tick){
        if(!active())return;
        mode=null;lastFinish=tick;watchSince=-1;selectedFeed=-1;atMonitor=false;nav.reset();
        gear.endCameraWatch();attention.stop();
    }
    void frame(String frame){
        attention.frame(frame);
        if(frame.startsWith("TEAMMAP|")){
            var p=LanProtocol.parts(frame);
            for(int s=0;s<rooms.length;s++)rooms[s]=Integer.parseInt(p.get(s+1));
        }else if(frame.startsWith("PEER|LEFT|")){
            int s=Integer.parseInt(LanProtocol.parts(frame).get(2));rooms[s]=-1;crew[s]=false;
        }else if(frame.startsWith("HUNTER|DOWN|")){
            down[Integer.parseInt(LanProtocol.parts(frame).get(2))]=true;
        }else if(frame.equals("CAMERA|RESET")){
            Arrays.fill(cameras,null);Arrays.fill(powered,false);selectedFeed=-1;
        }else if(frame.startsWith("CAMERA|")){
            var p=LanProtocol.parts(frame);
            int s=Integer.parseInt(p.get(2));
            switch(p.get(1)){
                case "PLACE" -> {
                    cameras[s]=new LanVideoCameras.Camera(s,Integer.parseInt(p.get(3)),Integer.parseInt(p.get(4))/1000d,
                        Integer.parseInt(p.get(5))/1000d,Integer.parseInt(p.get(6))/1000d,Integer.parseInt(p.get(7)),
                        EquipmentTiers.Tier.valueOf(p.get(8)));
                    powered[s]=false;
                }
                case "POWER" -> powered[s]=p.get(3).equals("1");
                case "CLEAR" -> {cameras[s]=null;powered[s]=false;}
            }
        }
    }
    void positioned(int seat,int room,boolean onRelay){
        rooms[seat]=room;
        if(seat==ownSeat)atMonitor=seat==setupHand&&onRelay;
    }
    boolean command(String words,int tick,int seat,Consumer<String> radio){
        String[] args=words.split(" ",2);
        if(args.length!=2)return false;
        Formation wanted;
        try{wanted=Formation.valueOf(args[0].toUpperCase(Locale.ROOT));}
        catch(IllegalArgumentException unknown){return false;}
        int where=-1;
        for(int r=0;r<HouseMap.INDOOR_ROOMS;r++)
            if(args[1].equals(""+r)||map.roomName(r).equalsIgnoreCase(args[1])){where=r;break;}
        if(where<0||map.outside(where))return false;
        gear.endCameraWatch();attention.stop();ownSeat=seat;mode=wanted;room=where;start=tick;lastTest[room][mode.ordinal()]=tick;
        Arrays.fill(crew,false);Arrays.fill(bringsCamera,false);
        crew[seat]=true;bringsCamera[seat]=gear.ownsCamera();selectedFeed=-1;setupHand=-1;
        watchSince=-1;readyReported=false;rolesReported=false;atMonitor=false;fieldObserver=false;usingVideo=false;nav.reset();
        radio.accept("Joining watch "+token()+(bringsCamera[seat]?" with camera.":"."));
        return true;
    }
    private String token(){return mode.word()+" "+room+" at "+start;}
    void radio(LanChat.Entry line,int seat,boolean autonomous,Consumer<String> say){
        String words=line.message();
        if(words.startsWith("Suggest watch ")&&!active()&&autonomous&&line.tick()-lastFinish>=800){
            command(words.substring(14),line.tick(),seat,say);
        }else if(active()){
            String joining="Joining watch "+token();
            if(words.equals(joining+".")||words.equals(joining+" with camera.")){
                crew[line.seat()]=true;bringsCamera[line.seat()]=words.endsWith(" with camera.");
            }else if(crew[line.seat()]&&words.equals("Leaving watch "+token()+".")){
                crew[line.seat()]=false;
            }else if(crew[line.seat()]&&words.equals("Finished watch "+token()+".")){
                stop(line.tick());
            }
        }
    }
    String withdrawal(){return "Leaving watch "+token()+".";}
    String propose(int tick,int seat,int source,List<GhostIdentity.Type> candidates){
        if(source<0||candidates.size()<2||tick<1100+seat*19||tick-lastFinish<800)return null;
        int available=0;
        for(int s=0;s<rooms.length;s++)if(rooms[s]>=0&&!down[s])available++;
        Formation best=null;
        int oldest=Integer.MAX_VALUE;
        for(Formation test:Formation.values()){
            boolean video=gear.ownsCamera()||feed(source)>=0;
            if(test==Formation.EMPTY&&(!video||available==0)||available<test.minimum)continue;
            FigureView view=video&&available>test.minimum?FigureView.VIDEO:FigureView.DIRECT;
            var clue=GhostFieldGuide.figureClue(view,test.minimum);
            long compatible=candidates.stream().filter(type->GhostFieldGuide.possible(type,clue)).count();
            int last=lastTest[source][test.ordinal()];
            if(compatible>0&&compatible<candidates.size()&&last<oldest&&tick-last>=2400){
                best=test;oldest=last;
            }
        }
        if(best==null)return null;
        lastTest[source][best.ordinal()]=tick;
        return "Suggest watch "+best.word()+" "+source;
    }
    private int feed(int where){
        for(int s=0;s<cameras.length;s++)if(cameras[s]!=null&&powered[s]&&cameras[s].room()==where)return s;
        return -1;
    }
    private int count(){
        int result=0;
        for(int s=0;s<rooms.length;s++)if(!down[s]&&rooms[s]==room)result++;
        return result;
    }
    LanVideoCameras.Camera camera(int owner){return cameras[owner];}
    int cameraRoom(int owner){return cameras[owner]==null||!powered[owner]?-1:cameras[owner].room();}
    boolean watching(int owner,LanWorld.Body b){return active()&&selectedFeed==owner&&powered[owner]&&attention.remote(owner,b);}
    boolean observing(FigureView view,int subject,int occupants){
        if(!active()||subject!=room||!mode.fits(occupants)||watchSince<0)return false;
        return view==FigureView.VIDEO?usingVideo&&atMonitor:!usingVideo&&fieldObserver;
    }
    void finish(int tick,Consumer<String> say){
        say.accept("Finished watch "+token()+".");
        stop(tick);
    }
    Input act(int tick,int seat,LanWorld.Body self,Consumer<String> say){
        rooms[seat]=self.room();
        if(tick-start<VOLUNTEER_TICKS)return Input.simple(Action.WAIT);
        int unassignedInside=0,team=0,lead=-1,operator=-1;
        selectedFeed=feed(room);
        for(int s=0;s<crew.length;s++){
            if(down[s]||rooms[s]<0)continue;
            if(!crew[s]){if(rooms[s]==room)unassignedInside++;continue;}
            team++;if(lead<0)lead=s;
            if(operator<0&&bringsCamera[s])operator=s;
        }
        if(operator<0&&selectedFeed>=0)operator=lead;
        // A field witness and a receiver operator are different physical bodies.
        usingVideo=operator>=0&&(mode==Formation.EMPTY||team+unassignedInside>mode.minimum);
        int assignedOperator=usingVideo?operator:-1;
        if(setupHand!=assignedOperator){rolesReported=false;readyReported=false;watchSince=-1;}
        setupHand=assignedOperator;
        int wanted=Math.max(0,mode.minimum-unassignedInside);
        fieldObserver=false;
        for(int s=0;s<crew.length&&wanted>0;s++){
            if(!crew[s]||down[s]||rooms[s]<0||s==setupHand)continue;
            if(s==seat)fieldObserver=true;
            wanted--;
        }
        boolean canWatch=usingVideo||mode!=Formation.EMPTY;
        if(!canWatch||tick-start>=SETUP_LIMIT&&watchSince<0){
            if(seat==lead)say.accept(map.roomName(room)+(canWatch?" watch timed out.":" needs a live video camera for an empty-room test."));
            if(seat==lead)finish(tick,say);else stop(tick);
            return Input.simple(Action.WAIT);
        }
        if(!rolesReported){
            rolesReported=true;
            if(seat==lead){
                String direction=switch(mode){
                    case EMPTY -> "Everyone clear the room.";
                    case SOLO -> "Keep exactly one hunter inside.";
                    case GROUP -> "Keep at least two hunters inside.";
                };
                say.accept(map.roomName(room)+" watch: "+direction+(usingVideo?" H"+(setupHand+1)+" on video.":""));
            }
        }
        boolean operatorHere=seat==setupHand;
        atMonitor=operatorHere&&self.room()==HouseMap.EXTRACTION_ROOM
            &&map.van().stationAt(self.x(),self.z())==VanStaging.Station.RELAY;
        boolean localObserver=usingVideo?selectedFeed>=0&&watching(selectedFeed,self):fieldObserver&&self.room()==room;
        if(localObserver&&mode.fits(count())){
            if(watchSince<0)watchSince=tick;
            if(!readyReported){
                readyReported=true;say.accept("Watching "+map.roomName(room)+" "+mode.word()+(usingVideo?" on van video.":" in person."));
            }
            if(tick-watchSince>=WATCH_TICKS){
                say.accept(map.roomName(room)+" "+mode.word()+" watch: no figure seen. Inconclusive.");
                finish(tick,say);return Input.simple(Action.WAIT);
            }
        }else watchSince=-1;
        if(operatorHere){
            if(selectedFeed<0)return prepareCamera(tick,seat,self);
            if(atMonitor)return attention.screen(tick,selectedFeed,self);
            var relay=map.van().location(VanStaging.Station.RELAY);
            return nav.to(tick,self,HouseMap.EXTRACTION_ROOM,relay.x(),map.van().floorY(),relay.z(),.11);
        }
        if(fieldObserver){
            if(self.room()==room){
                int lens=gear.slot("PHOTO_CAMERA");
                if(lens<0&&cameras[seat]==null)lens=gear.slot("VIDEO_CAMERA");
                // Alternate field observation with deliberate, raised viewfinder checks.
                boolean examineLens=lens>=0&&(tick-start)%120>=60;
                if(!examineLens)attention.stop();
                if(examineLens&&!gear.belt[lens].equals(gear.held())&&tick-lastSelect>=18){
                    lastSelect=tick;return Input.select(lens);
                }
                if(tick-lastTurn>=24){
                    lastTurn=tick;return Input.turn((self.heading()+80)%360);
                }
                if(examineLens&&gear.belt[lens].equals(gear.held()))return attention.select(tick,HunterCameraAttention.LENS);
                return Input.simple(Action.WAIT);
            }
            return nav.to(tick,self,room,map.centerX(room),map.centerZ(room),1);
        }
        if(self.room()!=room)return Input.simple(Action.WAIT);
        var van=map.van();var outside=van.location(VanStaging.Station.BRIEFING);
        return nav.to(tick,self,HouseMap.EXTRACTION_ROOM,outside.x(),van.floorY(),outside.z(),.3);
    }
    private Input prepareCamera(int tick,int seat,LanWorld.Body self){
        int slot=gear.slot("VIDEO_CAMERA");
        if(slot<0||gear.needsService()){
            if(slot<0)gear.requestCamera(tick);
            Input fetch=gear.fetch(tick,self,map,nav,cameras[seat]);
            if(fetch!=null)return fetch;
        }
        if(slot<0)return Input.simple(Action.WAIT);
        var old=cameras[seat];
        if(old!=null)return gear.retrieveCamera(tick,self,nav,old,slot);
        if(self.room()!=room)return nav.to(tick,self,room,map.centerX(room),map.centerZ(room),1);
        if(tick-lastUse<18)return Input.simple(Action.WAIT);
        lastUse=tick;
        int heading=Math.floorMod((int)Math.round(Math.toDegrees(Math.atan2(map.centerX(room)-self.x(),
            map.centerZ(room)-self.z()))),360);
        return Input.sampleFacing(slot,heading);
    }
}
