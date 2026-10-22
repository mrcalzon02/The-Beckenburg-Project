package org.beckenburg;

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
    private int stress;
    private final EquipmentLoadout inventory=new EquipmentLoadout();
    public record FieldNote(int tick,int room,EquipmentLoadout.Item tool,String reading){
        public FieldNote{
            if(tick<0||room<0||tool==null||reading==null||reading.isBlank())
                throw new IllegalArgumentException("invalid field observation");
        }
    }
    private final java.util.ArrayDeque<FieldNote> observations=new java.util.ArrayDeque<>();
    private String intent="Surveying the room";

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
    public int instrumentCount(){return observations.size();}
    public List<FieldNote> observations(){return List.copyOf(observations);}
    public EquipmentLoadout.Item equipped(){return inventory.equipped();}
    /**
     * The field worker decides when to use an actually issued tool. The world,
     * not this AI, must supply the resulting raw measurement.
     */
    public EquipmentLoadout.Item toolDue(int tick){
        if(health<=0||tick<0||tick-lastScan<65||room()<0
                ||room()>=HouseMap.EXTRACTION_ROOM)return null;
        return (observations.size()%2==0)
            ?EquipmentLoadout.Item.EMF:EquipmentLoadout.Item.THERMOMETER;
    }
    public void observed(int tick,EquipmentLoadout.Item tool,String value){
        if(tool==null||value==null||toolDue(tick)!=tool)
            throw new IllegalArgumentException("no authorized tool reading due");
        inventory.pickup(tool);
        lastScan=tick;
        if(observations.size()==48)observations.removeFirst();
        observations.addLast(new FieldNote(tick,room(),tool,value));
        intent="Recording "+tool.label.toLowerCase(java.util.Locale.ROOT)+" measurements";
    }
    /** Only after actual visual confirmation (checked by session world geometry). */
    public void spottedManifestation(int tick){
        if(tick<0)return;
        sightTick=tick;dangerTick=tick;
        alert=Math.min(100,alert+25);
        intent="Visible manifestation; preparing protection";
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
    }
    public void frightened(int fear){
        if(fear<=0)return;
        stress=Math.min(100,stress+fear);
        if(stress>=55){alert=Math.min(100,alert+20);intent="Retreating from supernatural pressure";
            target=-1;}
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
        if(target<0&&room()>=0&&room()<HouseMap.EXTRACTION_ROOM){
            // Guess a route from a coarse stereo bearing, not secret ghost coords.
            double aim=Math.toRadians(heading)-Math.asin(Math.max(-1,Math.min(1,soundPan/100d)));
            double dx=Math.sin(aim),dz=Math.cos(aim);
            double best=-2;
            for(int next:map.ghostExits(room())){
                for(var p:map.level().portalsFrom(room()))if(p.joins(room(),next)&&!p.kind().equals("window")){
                    double vx=p.x()-position.x(),vz=p.z()-position.z(),distance=Math.hypot(vx,vz);
                    if(distance<.01)continue;
                    double score=(vx*dx+vz*dz)/distance;
                    if(score>best){best=score;target=next;orderTick=heard.tick();}
                }
            }
        }
    }
    public void wounded(int damage){
        if(damage<=0)return;
        health=Math.max(0,health-damage);
        alert=Math.min(100,alert+25);
        dangerTick=Math.max(dangerTick,lastHeard);
        intent="Injured; seeking an exit";
        // Injuries do not reveal the ghost; any retreat still uses walkable rooms.
        target=-1;
    }
    public void advance(int tick){
        if(health==0)return;
        if(tick<321)return; // tutorial warning/attack window stays intact.
        if(lastHeard<tick-120)alert=Math.max(0,alert-1);
        int here=room();
        if(here<0||here==HouseMap.EXTRACTION_ROOM){intent="Route obstructed";return;}
        if(target<0||target==here||tick-orderTick>280){
            List<Integer> destinations=map.ghostExits(here);
            if(destinations.isEmpty()){intent="Holding position";return;}
            target=destinations.get(random.nextInt(destinations.size()));
            orderTick=tick;
            intent=alert>20?"Checking nearby disturbance":"Sweeping adjacent rooms";
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
