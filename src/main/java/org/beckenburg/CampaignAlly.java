package org.beckenburg;

import java.util.*;

/**
 * First campaign field assistant. Uses authored portal routing, swept Hunter
 * collision, actual feet elevation and a separately issued thermometer.
 * No ghost state is available to its decisions; observed air is always raw.
 */
public final class CampaignAlly {
    public enum Order { FOLLOW, HOLD, INVESTIGATE }
    private final HouseMap map;
    private final InvestigationSession caseSession;
    private LevelDefinition.Vec position;
    private Order order=Order.FOLLOW;
    private int destination=-1,heading=0,lastReading=-10000,lastAutoScan=-10000;
    private int lastSoundReport=-10000,totalScans;
    private String report="Awaiting field orders.";
    private final EquipmentLoadout instruments=new EquipmentLoadout();
    public record Observation(int tick,int room,EquipmentLoadout.Item instrument,
                              String reading,boolean unusual){}
    private final ArrayDeque<Observation> observations=new ArrayDeque<>();
    private final ArrayDeque<String> outboundChat=new ArrayDeque<>();
    private final int[] roomSuspicion=new int[HouseMap.ROOM_COUNT];
    public CampaignAlly(HouseMap map,InvestigationSession session){
        this.map=Objects.requireNonNull(map);
        caseSession=Objects.requireNonNull(session);
        var spawn=map.van().spawn(1);
        position=new LevelDefinition.Vec(spawn.x(),map.floorY(HouseMap.EXTRACTION_ROOM),spawn.z());
        if(!map.traversable(position.x(),position.y(),position.z()))
            throw new IllegalStateException("Assistant has no valid starting position");
    }
    public LevelDefinition.Vec position(){return position;}
    public int room(){return map.roomAt(position.x(),position.y(),position.z());}
    public Order order(){return order;}
    public String report(){return report;}
    public List<Observation> observations(){return List.copyOf(observations);}
    public int totalScans(){return totalScans;}
    public int suspectedRoom(){
        int room=-1,weight=0;
        for(int i=0;i<HouseMap.EXTRACTION_ROOM;i++)if(roomSuspicion[i]>weight){
            weight=roomSuspicion[i];room=i;
        }
        return weight>=3?room:-1;
    }
    public List<String> drainChat(){
        var messages=List.copyOf(outboundChat);outboundChat.clear();return messages;
    }
    private void announce(String words){
        if(outboundChat.size()==16)outboundChat.removeFirst();
        outboundChat.addLast(words);
    }
    private boolean recordReading(EquipmentLoadout.Item item,String raw){
        if(raw==null)return false;
        int slot=item==EquipmentLoadout.Item.EMF?0:1;
        if(instruments.at(slot)!=item)throw new IllegalStateException("Missing issued instrument");
        instruments.select(slot);
        int area=room();
        boolean unusual=false;
        if(item==EquipmentLoadout.Item.THERMOMETER){
            int degrees=raw.indexOf('°');
            if(degrees>4)try{
                double c=Double.parseDouble(raw.substring(4,degrees));
                unusual=c<=0||c>=33;
            }catch(NumberFormatException ignored){}
        }else if(item==EquipmentLoadout.Item.EMF){
            int slash=raw.indexOf('/');
            if(slash>4)try{
                unusual=Integer.parseInt(raw.substring(4,slash))>=8;
            }catch(NumberFormatException ignored){}
        }
        if(unusual&&area>=0&&area<HouseMap.EXTRACTION_ROOM)
            roomSuspicion[area]=Math.min(40,roomSuspicion[area]+3);
        if(observations.size()==48)observations.removeFirst();
        observations.addLast(new Observation(caseSession.ticks(),area,item,raw,unusual));
        totalScans++;
        lastReading=caseSession.ticks();
        String where=map.roomName(area);
        announce(item.label+": "+raw+" / "+where+
            (unusual?" (unusual, unverified)":""));
        if(unusual)announce("Possible activity around "+where+". Need another reading.");
        return true;
    }
    /** Hearing uses only the exact SOUND cue a human ear could receive.
     * The companion never sees the Ghost controller or automatically confirms clues.
     */
    public LanWorld.Body listener(){
        return new LanWorld.Body(position.x(),position.y(),position.z(),heading,room());
    }
    public void hear(LanSound.CueEvent sound){
        if(sound==null)return;
        String direction=sound.pan()>25?"right":sound.pan()<-25?"left":"ahead";
        boolean danger=sound.cue().tier()==LanSound.Tier.DANGEROUS
            ||sound.cue().tier()==LanSound.Tier.SIGNATURE;
        if(danger&&order==Order.INVESTIGATE){
            order=Order.FOLLOW;destination=-1;
        }
        report="T"+sound.tick()+" / "+map.roomName(room())+
            " / heard "+sound.cue().caption().toLowerCase(java.util.Locale.ROOT)+
            " ("+direction+")"+(danger?" / regrouping":"");
        if(sound.tick()-lastSoundReport>=55 || danger&&sound.tick()>lastSoundReport+8){
            lastSoundReport=sound.tick();
            announce("Heard "+sound.cue().caption()+" to my "+direction+
                " near "+map.roomName(room())+(danger?". Regrouping.":"."));
        }
    }
    public int destination(){return destination;}
    public void follow(){order=Order.FOLLOW;destination=-1;report="Following investigator."; }
    public void hold(){order=Order.HOLD;destination=-1;report="Holding at "+map.roomName(room())+"."; }
    public void investigate(int zone){
        if(zone<0||zone>=HouseMap.ROOM_COUNT)
            throw new IllegalArgumentException("Unknown investigation zone");
        order=Order.INVESTIGATE;destination=zone;
        report="Heading to "+map.roomName(zone)+".";
    }
    /** Independent AI measurements never mark the player's journal. */
    public String readThermometer(){
        if(caseSession.ticks()-lastReading<30){
            report="Thermometer is still settling.";return report;
        }
        String raw=caseSession.rawTemperatureAt(position.x(),position.y(),position.z());
        report=raw==null?"Cannot measure air from here.":
            "T"+caseSession.ticks()+" / "+map.roomName(room())+" / "+raw;
        if(raw!=null)recordReading(EquipmentLoadout.Item.THERMOMETER,raw);
        return report;
    }
    public String readEmf(){
        if(caseSession.ticks()-lastReading<30){
            report="EMF reader is still settling.";return report;
        }
        String raw=caseSession.rawEmfAt(position.x(),position.y(),position.z());
        report=raw==null?"Cannot sample the field from here.":
            "T"+caseSession.ticks()+" / "+map.roomName(room())+" / "+raw;
        if(raw!=null)recordReading(EquipmentLoadout.Item.EMF,raw);
        return report;
    }
    public String latestEvidence(){
        if(observations.isEmpty())return "No field measurements yet.";
        var last=observations.peekLast();
        return last.instrument().label+" / "+map.roomName(last.room())+" / "+
            last.reading()+(last.unusual()?" / unusual, unverified":"");
    }
    public String suspicionReport(){
        int probable=suspectedRoom();
        return probable<0?"No strong location hypothesis yet.":
            "Possible activity in "+map.roomName(probable)+". Unconfirmed.";
    }
    /** Callouts are actual nearby speech, not remote magic orders. */
    public String onCallout(TeamCallout.Ping ping,LanWorld.Body player){
        if(ping==null||player==null||ping.sender()!=0||
            !LanChat.localVisible(map,player,listener()))return null;
        switch(ping.phrase()){
            case FOLLOW,HERE,HELP -> {
                follow();return "Copy. Following you.";
            }
            case HOLD -> {
                hold();return "Holding here.";
            }
            case THERE,EVIDENCE -> {
                investigate(ping.zone());
                return "Checking "+map.roomName(ping.zone())+" with my instruments.";
            }
            case DANGER,DONT_GO -> {
                follow();return "Understood. Regrouping.";
            }
        }
        throw new AssertionError(ping.phrase());
    }
    /** Typed field messages use the exact LAN chat hearing/radio channels. */
    public String onChat(String raw,LanChat.Channel channel,LanWorld.Body player){
        String message=LanChat.clean(raw).toLowerCase(Locale.ROOT);
        if(channel==LanChat.Channel.LOCAL&&
            !LanChat.localVisible(map,player,listener()))return null;
        if(message.contains("follow")||message.contains("regroup")
                ||message.equals("help")||message.equals("come here")){
            follow();return "Copy. Following you.";
        }
        if(message.equals("hold")||message.equals("stay")){
            hold();return "Holding position.";
        }
        if(message.startsWith("check ")||message.startsWith("investigate ")){
            String token=message.substring(message.indexOf(' ')+1).strip();
            try{
                int target=Integer.parseInt(token);
                if(target>=0&&target<HouseMap.EXTRACTION_ROOM){
                    investigate(target);return "Moving to "+map.roomName(target)+".";
                }
            }catch(NumberFormatException ignored){}
            return "Send a valid room number, e.g. check 4.";
        }
        if(message.contains("emf"))return readEmf();
        if(message.contains("temperature")||message.contains("thermometer"))return readThermometer();
        if(message.contains("evidence")||message.contains("reading"))return latestEvidence();
        if(message.contains("suspect")||message.contains("hypothesis")
                ||message.contains("ghost")||message.contains("think"))return suspicionReport();
        if(message.contains("status")||message.contains("where")||message.contains("report"))
            return "At "+map.roomName(room())+" / "+order+" / "+
                totalScans+" measurements. "+suspicionReport();
        return "Copy. Try: follow, hold, check 4, emf, temperature, evidence, suspect.";
    }
    private List<Integer> route(int from,int to){
        if(from==to)return List.of(from);
        if(from<0||to<0)return List.of();
        int[] parent=new int[HouseMap.ROOM_COUNT];Arrays.fill(parent,-1);
        ArrayDeque<Integer> queue=new ArrayDeque<>();queue.add(from);parent[from]=from;
        while(!queue.isEmpty()&&parent[to]==-1){
            int here=queue.remove();
            for(int next:map.adjacentRooms(here))if(parent[next]==-1){
                parent[next]=here;queue.add(next);
            }
        }
        if(parent[to]==-1)return List.of();
        ArrayList<Integer> path=new ArrayList<>();
        for(int at=to;at!=from;at=parent[at])path.add(at);
        path.add(from);Collections.reverse(path);return path;
    }
    /** Ten steps per second, like the solo simulation clock. */
    public void advance(double playerX,double playerY,double playerZ){
        if(caseSession.outcome()!=InvestigationSession.Outcome.ACTIVE)return;
        int tick=caseSession.ticks();
        if(room()>=0&&room()<HouseMap.EXTRACTION_ROOM&&
                tick>=lastAutoScan+90&&tick>=lastReading+30){
            lastAutoScan=tick;
            if(totalScans%2==0)readEmf();else readThermometer();
        }
        if(order==Order.HOLD)return;
        int targetRoom=order==Order.FOLLOW?map.roomAt(playerX,playerY,playerZ):destination;
        if(targetRoom<0)return;
        int here=room();
        if(here<0){report="Navigation blocked; waiting for a route.";return;}
        double goalX=order==Order.FOLLOW?playerX:map.centerX(targetRoom);
        double goalZ=order==Order.FOLLOW?playerZ:map.centerZ(targetRoom);
        if(here!=targetRoom){
            var path=route(here,targetRoom);
            if(path.size()<2){report="No reachable route to "+map.roomName(targetRoom)+".";return;}
            int next=path.get(1);
            LevelDefinition.Portal portal=null;
            for(var p:map.level().portalsFrom(here))
                if(!p.kind().equals("window")&&p.joins(here,next)){portal=p;break;}
            if(portal==null)return;
            double atDoor=Math.hypot(position.x()-portal.x(),position.z()-portal.z());
            if(portal.door()&&!map.doorOpen(portal.id())&&atDoor<=1.10)
                map.openDoorBetweenRooms(here,next);
            // Portal width lies ALONG its tangent, not toward the centre of an
            // often enormous outdoor room. A diagonal centre vector would walk
            // off the narrow van ramp and collide with the cargo door frame.
            // Approach the portal at its centre, then cross perpendicular to it.
            double beyond=atDoor<1.30?.85:0;
            if(portal.joins(HouseMap.EXTRACTION_ROOM,13)){
                // The outdoor room centre is NOT necessarily outside the van:
                // large farmhouse and school aprons may centre behind the cargo
                // threshold. Use the actual dock orientation, not room centroids.
                var centreline=map.van().world(0,VanStaging.FRONT+
                    (next==HouseMap.EXTRACTION_ROOM?beyond:-beyond));
                goalX=centreline.x();goalZ=centreline.z();
            }else if(portal.alongX()){
                goalX=portal.x();
                goalZ=portal.z()+Math.signum(map.centerZ(next)-portal.z())*beyond;
            }else{
                goalX=portal.x()+Math.signum(map.centerX(next)-portal.x())*beyond;
                goalZ=portal.z();
            }
        }else if(here!=HouseMap.EXTRACTION_ROOM&&
                targetRoom!=HouseMap.EXTRACTION_ROOM&&
                position.y()>.10&&map.van().supportHeight(position.x(),position.z())!=null){
            // After crossing the exterior room boundary, the companion is
            // still physically on the descending cargo ramp. Finish that
            // descent along its clear centreline before pursuing room goals.
            var bottom=map.van().world(0,VanStaging.FRONT-VanStaging.RAMP_LENGTH-.14);
            goalX=bottom.x();goalZ=bottom.z();
        }else if(Math.hypot(position.x()-goalX,position.z()-goalZ)
                <(order==Order.FOLLOW?1.55:.65)){
            if(order==Order.INVESTIGATE){
                readThermometer();order=Order.HOLD;destination=-1;
            }
            return;
        }
        double dx=goalX-position.x(),dz=goalZ-position.z(),distance=Math.hypot(dx,dz);
        if(distance<.00001)return;
        double speed=Math.min(.21,distance),headingRad=Math.atan2(dx,dz);
        heading=Math.floorMod((int)Math.round(Math.toDegrees(headingRad)),360);
        // No coordinate jump: a companion must move using the same swept
        // collision/elevation function that controls human solo movement.
        LevelDefinition.Vec candidate=null;
        for(double correction:new double[]{0,.40,-.40,.85,-.85,1.25,-1.25}){
            double angle=headingRad+correction;
            var next=map.resolveMove(position,position.x()+Math.sin(angle)*speed,
                position.z()+Math.cos(angle)*speed);
            if(next!=null&&Math.hypot(next.x()-position.x(),next.z()-position.z())>.035){
                candidate=next;break;
            }
        }
        if(candidate!=null)position=candidate;
    }
    public SceneRenderer.RemoteBody sprite(){
        return new SceneRenderer.RemoteBody(position.x(),position.y(),position.z(),
            1,heading,"observer");
    }
}
