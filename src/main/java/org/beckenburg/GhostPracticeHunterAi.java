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
    private int orderTick;
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
    public String intent(){return intent;}
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
