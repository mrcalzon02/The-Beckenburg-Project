package org.beckenburg;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Embodied investigator opposing a human-controlled ghost. Perception is limited
 * to delivered audible SOUND events; no ghost type, anchor or exact ghost position
 * enters this controller. Movement uses the human Hunter's swept collision API.
 */
public final class GhostPracticeHunterAi {
    private final HouseMap map;
    private final Random random;
    private LevelDefinition.Vec position;
    private int heading, target=-1, lastHeard=-10000, soundPan, alert, health=100;
    private int orderTick, lastScan=-10000, lastCounter=-10000, dangerTick=-10000, sightTick=-10000;
    private int stress,totalScans,searchUntil=-1;
    private LevelDefinition.Vec audibleSearchPoint;
    // These are tentative room-level suspicions from the Hunter's own
    // instrument readings and auditory impressions, never the true anchor.
    private final int[] suspicion=new int[HouseMap.ROOM_COUNT];
    private final EquipmentLoadout inventory=new EquipmentLoadout();
    public record FieldNote(int tick,int room,EquipmentLoadout.Item tool,String reading){
        public FieldNote{
            if(tick<0||room<0||tool==null||reading==null||reading.isBlank())
                throw new IllegalArgumentException("invalid field observation");
        }
    }
    private final java.util.ArrayDeque<FieldNote> observations=new java.util.ArrayDeque<>();
    /** Short local utterances are emitted from real character locations.
     * They are delivered only to nearby listeners, never as a global mind-reading feed.
     */
    public record Utterance(int tick,int room,double x,double y,double z,String words){
        public Utterance{
            if(tick<0||room<0||words==null||words.isBlank())
                throw new IllegalArgumentException("invalid spoken observation");
        }
    }
    private final ArrayDeque<Utterance> speech=new ArrayDeque<>();
    private int lastDangerSpeech=-10000,lastSuspicionSpeech=-10000,lastVisualSpeech=-10000;
    private String intent="Surveying the room";
    private void speak(int tick,String words){
        if(room()<0||room()>=HouseMap.EXTRACTION_ROOM)return;
        if(speech.size()==24)speech.removeFirst();
        speech.addLast(new Utterance(tick,room(),position.x(),position.y(),position.z(),words));
    }
    public List<Utterance> drainSpeech(){
        List<Utterance> spoken=List.copyOf(speech);
        speech.clear();
        return spoken;
    }

    public GhostPracticeHunterAi(HouseMap map,int room,long seed) {
        this.map=Objects.requireNonNull(map);
        if(room<0||room>=HouseMap.EXTRACTION_ROOM)throw new IllegalArgumentException("hunter room");
        position=map.spawn(room);
        if(!map.traversable(position.x(),position.y(),position.z()))
            throw new IllegalArgumentException("unwalkable hunter starting position");
        random=new Random(seed^0x4649454c444149L);
    }
    public LevelDefinition.Vec position(){return position;}
    public int room(){return map.roomAt(position.x(),position.y(),position.z());}
    public int heading(){return heading;}
    public int health(){return health;}
    public int alert(){return alert;}
    public int stress(){return stress;}
    public String intent(){return intent;}
    public int instrumentCount(){return totalScans;}
    public List<FieldNote> observations(){return List.copyOf(observations);}
    public int suspectedRoom(){
        int best=-1,score=2;
        for(int i=0;i<HouseMap.EXTRACTION_ROOM;i++)
            if(suspicion[i]>score){best=i;score=suspicion[i];}
        return best;
    }
    public int suspicion(int room){
        if(room<0||room>=HouseMap.ROOM_COUNT)throw new IllegalArgumentException("room");
        return suspicion[room];
    }
    public boolean pursuingSound(int tick){
        return audibleSearchPoint!=null&&tick<=searchUntil;
    }
    public EquipmentLoadout.Item equipped(){return inventory.equipped();}
    /**
     * The field worker decides when to use an actually issued tool. The world,
     * not this AI, must supply the resulting raw measurement.
     */
    public EquipmentLoadout.Item toolDue(int tick){
        if(health<=0||tick<0||tick-lastScan<65||room()<0
                ||room()>=HouseMap.EXTRACTION_ROOM)return null;
        return (totalScans%2==0)
            ?EquipmentLoadout.Item.EMF:EquipmentLoadout.Item.THERMOMETER;
    }
    public void observed(int tick,EquipmentLoadout.Item tool,String value){
        if(tool==null||value==null||toolDue(tick)!=tool)
            throw new IllegalArgumentException("no authorized tool reading due");
        inventory.pickup(tool);
        lastScan=tick;
        if(observations.size()==48)observations.removeFirst();
        int site=room();
        observations.addLast(new FieldNote(tick,site,tool,value));
        totalScans++;
        // All field announcements are based on this particular measured value,
        // without silently treating high EMF or cold air as confirmed evidence.
        speak(tick,tool==EquipmentLoadout.Item.EMF?
            value+" at "+map.roomName(site)+". Not yet confirmed.":
            "Thermometer "+value+" at "+map.roomName(site)+".");
        int beforeSuspicion=suspicion[site];
        // Interpret only observed numeric values. Neither Ghost type nor the
        // true anchor is available to this AI. Abnormal weather can mislead it.
        try{
            if(tool==EquipmentLoadout.Item.EMF && value.startsWith("EMF ")){
                int slash=value.indexOf('/',4);
                if(slash>4){
                    int level=Integer.parseInt(value.substring(4,slash));
                    if(level>=8)suspicion[site]=Math.min(40,suspicion[site]+3);
                    else if(level>=6)suspicion[site]=Math.min(40,suspicion[site]+1);
                }
            }else if(tool==EquipmentLoadout.Item.THERMOMETER&&value.startsWith("Air ")){
                int unit=value.indexOf('°');
                if(unit>4){
                    double degrees=Double.parseDouble(value.substring(4,unit));
                    if(degrees<=0||degrees>=33)
                        suspicion[site]=Math.min(40,suspicion[site]+3);
                }
            }
        }catch(NumberFormatException unreadable){
            // Record the imperfect instrument text; never manufacture a clue.
        }
        if(suspicion[site]>beforeSuspicion&&
                (lastSuspicionSpeech<0||tick-lastSuspicionSpeech>=95)){
            lastSuspicionSpeech=tick;
            speak(tick,"Possible activity in "+map.roomName(site)+". More checks needed.");
        }
        intent="Recording "+tool.label.toLowerCase(java.util.Locale.ROOT)+" measurements";
    }
    /** Only after actual visual confirmation (checked by session world geometry). */
    public void spottedManifestation(int tick){
        if(tick<0)return;
        sightTick=tick;dangerTick=tick;
        alert=Math.min(100,alert+25);
        intent="Visible manifestation; preparing protection";
        if(tick-lastVisualSpeech>=80){
            lastVisualSpeech=tick;
            speak(tick,"I saw something. Preparing protection!");
        }
    }
    public HunterCounters.Kind counterDue(int tick,HunterCounters stock){
        if(stock==null||health<=0||tick<321||tick-lastCounter<85||room()<0
                ||room()>=HouseMap.EXTRACTION_ROOM)return null;
        if(tick-sightTick<=12&&stock.charges(0,HunterCounters.Kind.CROSS)>0)
            return HunterCounters.Kind.CROSS;
        if((stress>=35||health<=65||tick-dangerTick<=15)
                &&stock.charges(0,HunterCounters.Kind.WARD)>0)
            return HunterCounters.Kind.WARD;
        return null;
    }
    public void counterDeployed(HunterCounters.Kind kind,int tick){
        lastCounter=tick;
        intent=kind==HunterCounters.Kind.CROSS?
            "Cross deployed against visible apparition":"Protective ward deployed";
        speak(tick,kind==HunterCounters.Kind.CROSS?
            "Cross deployed! Keep back.":"Ward down. Holding my ground.");
    }
    public void frightened(int fear){
        if(fear<=0)return;
        stress=Math.min(100,stress+fear);
        if(stress>=55){alert=Math.min(100,alert+20);intent="Retreating from supernatural pressure";
            target=-1;audibleSearchPoint=null;
            if(lastDangerSpeech<0){lastDangerSpeech=0;speak(lastHeard>=0?lastHeard:0,
                "Pressure is getting bad. I need to regroup.");}
        }
    }
    public SceneRenderer.RemoteBody sprite(){
        return new SceneRenderer.RemoteBody(position.x(),position.y(),position.z(),
            0,heading,"observer");
    }
    public LanWorld.Body listener(){
        return new LanWorld.Body(position.x(),position.y(),position.z(),heading,room());
    }
    /** Only the actual projected audio packet crosses this sensory boundary. */
    public void hear(LanSound.CueEvent heard){
        if(heard==null)return;
        lastHeard=heard.tick();soundPan=heard.pan();
        if(heard.cue().tier()==LanSound.Tier.DANGEROUS
                ||heard.cue().tier()==LanSound.Tier.SIGNATURE
                ||heard.cue()==LanSound.Cue.MANIFEST)dangerTick=heard.tick();
        alert=Math.min(100,alert+Math.max(3,heard.volume()/7));
        intent="Investigating an audible disturbance";
        if(heard.tick()-lastDangerSpeech>=45){
            if(heard.cue().tier()==LanSound.Tier.DANGEROUS||
                    heard.cue().tier()==LanSound.Tier.SIGNATURE||
                    heard.cue()==LanSound.Cue.MANIFEST){
                lastDangerSpeech=heard.tick();
                speak(heard.tick(),"Did you hear that? "+heard.cue().caption()+".");
            }
        }
        if(room()>=0&&room()<HouseMap.EXTRACTION_ROOM){
            // Loud event perceived *in this room*: first inspect the local
            // bearing. Pan is coarse and has NO source distance or coordinates.
            if(heard.volume()>=24&&stress<55){
                double aim=Math.toRadians(heading)-
                    Math.asin(Math.max(-1,Math.min(1,soundPan/100d)));
                for(double distance:new double[]{2.5,1.75,1.0,.6}){
                    double x=position.x()+Math.sin(aim)*distance;
                    double z=position.z()+Math.cos(aim)*distance;
                    if(map.roomAt(x,position.y(),z)==room()
                            &&map.traversable(x,position.y(),z)
                            &&map.clearLineOfSight(position.x(),position.y()+1.3,position.z(),
                                x,position.y()+1.3,z)){
                        audibleSearchPoint=new LevelDefinition.Vec(x,position.y(),z);
                        searchUntil=heard.tick()+90;target=-1;break;
                    }
                }
            }
            if(heard.volume()>=75&&heard.cue().tier()!=LanSound.Tier.AMBIENT)
                suspicion[room()]=Math.min(40,suspicion[room()]+1);
        }
    }
    public void wounded(int damage){
        if(damage<=0)return;
        health=Math.max(0,health-damage);
        alert=Math.min(100,alert+25);
        dangerTick=Math.max(dangerTick,lastHeard);
        intent="Injured; seeking an exit";
        speak(Math.max(0,lastHeard),"I've been hit! Need cover!");
        // Injuries do not reveal the ghost; any retreat still uses walkable rooms.
        target=-1;audibleSearchPoint=null;
    }
    public void advance(int tick){
        if(health==0)return;
        if(tick<321)return; // tutorial warning/attack window stays intact.
        if(lastHeard<tick-120)alert=Math.max(0,alert-1);
        int here=room();
        if(here<0||here==HouseMap.EXTRACTION_ROOM){intent="Route obstructed";return;}
        if(audibleSearchPoint!=null){
            if(tick<=searchUntil&&Math.hypot(
                    position.x()-audibleSearchPoint.x(),
                    position.z()-audibleSearchPoint.z())>.38){
                intent="Checking an audible direction";
                walkToward(audibleSearchPoint.x(),audibleSearchPoint.z());
                return;
            }
            audibleSearchPoint=null;target=-1;
        }
        if(target<0||target==here||tick-orderTick>280){
            List<Integer> destinations=map.ghostExits(here);
            if(destinations.isEmpty()){intent="Holding position";return;}
            // Return to rooms with strong *observed* readings where possible,
            // otherwise keep patrolling. Suspicions can be wrong.
            int bestScore=2;
            int favored=-1;
            for(int next:destinations)if(suspicion[next]>bestScore){
                bestScore=suspicion[next];favored=next;
            }
            target=favored>=0?favored:destinations.get(random.nextInt(destinations.size()));
            orderTick=tick;
            intent=favored>=0?"Rechecking anomalous readings":
                alert>20?"Checking nearby disturbance":"Sweeping adjacent rooms";
        }
        LevelDefinition.Portal portal=null;
        for(var p:map.level().portalsFrom(here))
            if(p.joins(here,target)&&!p.kind().equals("window")){portal=p;break;}
        if(portal==null){target=-1;return;}
        double range=Math.hypot(position.x()-portal.x(),position.z()-portal.z());
        if(portal.door()&&!map.doorOpen(portal.id())&&range<1.10)
            map.openDoorBetweenRooms(here,target);
        double gx=portal.x(),gz=portal.z();
        if(range<1.25){
            if(portal.alongX()){
                double direction=Math.signum(map.centerZ(target)-portal.z());
                gz+=.85*(direction==0?1:direction);
            }else{
                double direction=Math.signum(map.centerX(target)-portal.x());
                gx+=.85*(direction==0?1:direction);
            }
        }
        walkToward(gx,gz);
    }
    private void walkToward(double gx,double gz){
        double dx=gx-position.x(),dz=gz-position.z(),length=Math.hypot(dx,dz);
        if(length<.01)return;
        double angle=Math.atan2(dx,dz),stride=Math.min(.19,length);
        heading=Math.floorMod((int)Math.round(Math.toDegrees(angle)),360);
        for(double adjust:new double[]{0,.35,-.35,.70,-.70,1.15,-1.15}){
            var step=map.resolveMove(position,position.x()+Math.sin(angle+adjust)*stride,
                position.z()+Math.cos(angle+adjust)*stride);
            if(step!=null&&Math.hypot(step.x()-position.x(),step.z()-position.z())>.02){
                position=step;
                break;
            }
        }
    }
}
