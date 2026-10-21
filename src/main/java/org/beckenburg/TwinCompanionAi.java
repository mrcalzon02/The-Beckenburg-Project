package org.beckenburg;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import org.beckenburg.LevelDefinition.Portal;

/**
 * Host-simulated second Twin. This is a genuine AI actor, NOT a billboard
 * offset from the player. It owns a persistent position, a goal, hearing memory,
 * locomotion, and occasional shared-budget actions. Its sister remains the only
 * human-controlled ghost seat, case identity, energy reservoir and ritual target.
 *
 * Simulation is 10 Hz. No duplicate combat executor or second damage source.
 */
public final class TwinCompanionAi {
    public enum Intent { PATROL, INVESTIGATE, TRAVEL, REST }
    private final HouseMap map;
    private final Random random;
    private LanGhostSpatial.Pose pose;
    private Intent intent=Intent.PATROL;
    private double targetX,targetZ;
    private int targetRoom,goalUntil,nextGoalTick,lastStepTick=-1,lastActionTick=-1000;
    private int lastNoiseTick=-1000,noiseRoom=-1;
    private double noiseX,noiseZ;
    private int lastActivityTick=-1000;
    private boolean moved;

    public TwinCompanionAi(HouseMap map,LanGhostSpatial.Pose primary,long seed){
        this.map=Objects.requireNonNull(map);
        Objects.requireNonNull(primary);
        random=new Random(seed);
        pose=TwinGhostPair.position(map,primary,0).get(1).pose();
        targetRoom=pose.room();
        targetX=pose.x();
        targetZ=pose.z();
    }

    public LanGhostSpatial.Pose pose(){return pose;}
    public Intent intent(){return intent;}
    public boolean moved(){return moved;}
    public int lastActivityTick(){return lastActivityTick;}

    /** A real in-world acoustic event, not perpetual access to hunter locations. */
    public void heard(int room,double x,double z,LanGhostPursuit.Sound sound,int tick,MissionWeather.State weather){
        Objects.requireNonNull(sound);
        if(tick<0||!Double.isFinite(x)||!Double.isFinite(z)||room<0
                ||room>=HouseMap.EXTRACTION_ROOM)return;
        if(!LanAcoustics.audible(map,pose.room(),room,sound,Objects.requireNonNull(weather)))return;
        lastNoiseTick=tick;
        noiseRoom=room;
        noiseX=x;noiseZ=z;
        nextGoalTick=Math.min(nextGoalTick,tick);
    }

    /** Host tick makes concrete independent movement decisions and applies collisions. */
    public void advance(int tick,LanGhostSpatial.Pose sister){
        if(tick<0)throw new IllegalArgumentException("negative tick");
        Objects.requireNonNull(sister);
        moved=false;
        if(tick<=lastStepTick)return;
        lastStepTick=tick;
        if(tick>=nextGoalTick||tick>=goalUntil||stuck()){
            chooseGoal(tick,sister);
        }
        double dx=targetX-pose.x(),dz=targetZ-pose.z();
        double distance=Math.hypot(dx,dz);
        if(distance<.23){
            if(targetRoom!=pose.room()&&map.ghostExits(pose.room()).contains(targetRoom)){
                // A door-connected transition uses the authored portal. The AI
                // does not teleport to the far room's spawn or clip through walls.
                var target=map.level().portalsFrom(pose.room()).stream()
                    .filter(p->p.joins(pose.room(),targetRoom)).findFirst();
                if(target.isPresent()){
                    Portal p=target.get();
                    double tx=map.centerX(targetRoom)-p.x();
                    double tz=map.centerZ(targetRoom)-p.z();
                    double length=Math.hypot(tx,tz);
                    if(length>.01){
                        targetX=p.x()+tx/length*.75;
                        targetZ=p.z()+tz/length*.75;
                    }
                }
                targetRoom=pose.room();
                goalUntil=tick+35;
                return;
            }
            nextGoalTick=tick;
            return;
        }
        double amount=Math.min(.17,distance);
        int heading=Math.floorMod((int)Math.round(Math.toDegrees(Math.atan2(dx,dz))),360);
        double nx=pose.x()+amount*dx/distance;
        double nz=pose.z()+amount*dz/distance;
        int room=map.roomAt(nx,pose.y(),nz);
        if(room>=0&&room<HouseMap.EXTRACTION_ROOM
                &&(room==pose.room()||map.ghostExits(pose.room()).contains(room))
                &&map.traversable(nx,pose.y(),nz)
                &&map.clearLineOfSight(pose.x(),pose.y()+.8,pose.z(),
                    nx,pose.y()+.8,nz)){
            pose=new LanGhostSpatial.Pose(room,nx,map.floorY(room),nz,heading);
            moved=true;
        }else{
            // No wall-phasing, clipping, or arbitrary AI repositioning.
            nextGoalTick=tick;
            goalUntil=tick;
        }
    }

    private boolean stuck(){
        return !map.traversable(pose.x(),pose.y(),pose.z());
    }
    private void chooseGoal(int tick,LanGhostSpatial.Pose sister){
        boolean heard=tick-lastNoiseTick<70 && noiseRoom>=0;
        if(heard&&noiseRoom==pose.room()){
            intent=Intent.INVESTIGATE;
            targetRoom=pose.room();
            targetX=noiseX;targetZ=noiseZ;
            goalUntil=tick+35;
        }else if(heard&&map.ghostExits(pose.room()).contains(noiseRoom)){
            // A real passage into another room, independent of human sister.
            var doors=map.level().portalsFrom(pose.room()).stream()
                .filter(p->p.joins(pose.room(),noiseRoom)).toList();
            if(!doors.isEmpty()){
                Portal p=doors.get(0);
                if(p.door())map.openDoorBetweenRooms(pose.room(),noiseRoom);
                intent=Intent.TRAVEL;targetRoom=noiseRoom;
                targetX=p.x();targetZ=p.z();
                goalUntil=tick+80;nextGoalTick=tick+35;
                return;
            }
            intent=Intent.PATROL;
            pickLocalGoal(tick,sister);
        }else{
            // Wandering is independent of the human-controlled Twin's movement.
            intent=Intent.PATROL;
            pickLocalGoal(tick,sister);
        }
        nextGoalTick=tick+15;
    }

    private void pickLocalGoal(int tick,LanGhostSpatial.Pose sister){
        for(int tries=0;tries<18;tries++){
            double x=pose.x()+(random.nextDouble()-.5)*7;
            double z=pose.z()+(random.nextDouble()-.5)*7;
            if(map.roomAt(x,pose.y(),z)==pose.room()
                    &&map.traversable(x,pose.y(),z)
                    &&map.clearLineOfSight(pose.x(),pose.y()+.9,pose.z(),x,pose.y()+.9,z)){
                targetX=x;targetZ=z;targetRoom=pose.room();goalUntil=tick+25+random.nextInt(25);
                return;
            }
        }
        // Stay valid rather than jump into another sister's body or a wall.
        intent=Intent.REST;targetX=pose.x();targetZ=pose.z();targetRoom=pose.room();
        goalUntil=tick+15;
    }

    /**
     * Occasional independent haunting. Shared GhostDomain validates cost,
     * cooldown, energy and room; only harmless/disturbing events are chosen.
     * A separate Twin NEVER doubles a single STRIKE or damage resolution.
     */
    public GhostDomain.Action chooseActivity(GhostDomain.Observation shared,int tick){
        if(shared==null||tick-lastActionTick<75||!moved&&intent!=Intent.INVESTIGATE
                ||pose.room()!=shared.room())return null;
        var legal=shared.legalActions();
        GhostDomain.Action action=null;
        if(tick-lastNoiseTick<45&&legal.contains(GhostDomain.Action.DISTURB))
            action=GhostDomain.Action.DISTURB;
        else if(tick%29==0&&legal.contains(GhostDomain.Action.WHISPER))
            action=GhostDomain.Action.WHISPER;
        if(action!=null){lastActionTick=tick;lastActivityTick=tick;}
        return action;
    }

    public String playerFrame(boolean ancient,int tick){
        var p=pose;
        return "GCOMPANION|"+Math.round(p.x()*1000)+"|"+Math.round(p.y()*1000)
            +"|"+Math.round(p.z()*1000)+"|"+p.heading()+"|"+(ancient?1:0)+"|"+tick;
    }
}
