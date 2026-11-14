package org.beckenburg;

/** Shared-world host movement model, isolated from the legacy room-only demonstration.
 * Peers submit direction impulses; only this class owns collision and transforms.
 */
public final class LanWorld {
    public record Body(double x,double y,double z,int heading,int room) {
        public Body(double x,double z,int heading,int room){this(x,0,z,heading,room);}
    }
    private static final double SPEED=.31;
    private final HouseMap map;
    private final org.beckenburg.editor.DefaultPlayerMotion[] jumps=new org.beckenburg.editor.DefaultPlayerMotion[6];
    private final double[] momentumX=new double[6],momentumZ=new double[6];
    private final boolean[] requestedCrouch=new boolean[6];
    private int lastPhysicsTick=-1;
    private final int[] jumpStartedTick=new int[6];
    private org.beckenburg.editor.DefaultPlayerMotion.Collision jumpCollision(int seat){
        return new org.beckenburg.editor.DefaultPlayerMotion.Collision(){
            public boolean clear(double x,double y,double z,double height){
                return (!exitSealed||body(seat).room()==HouseMap.EXTRACTION_ROOM
                    ||map.roomAt(x,y,z)!=HouseMap.EXTRACTION_ROOM)&&map.airborneClear(x,y,z,height);
            }
            public double landing(double x,double z,double from,double to){return map.jumpLanding(x,z,from,to);}
        };
    }
    public boolean airborne(int seat){seat(seat);return jumps[seat].airborne();}
    public void jump(int seat,int tick){
        seat(seat);
        if(hiding[seat])throw new IllegalArgumentException("leave hiding before jumping");
        var b=body(seat);
        if(!map.traversable(b.x(),b.y(),b.z(),HunterPosture.height(crouched[seat])))
            throw new IllegalArgumentException("jump requires grounded feet");
        boolean moving=tick-lastTick[seat]<=1;
        jumps[seat].jump(b.x(),b.y(),b.z(),moving?momentumX[seat]:0,moving?momentumZ[seat]:0,
            HunterPosture.CROUCH_HEIGHT,jumpCollision(seat));
        ys[seat]=jumps[seat].y();crouched[seat]=true;jumpStartedTick[seat]=tick;
    }
    /** Host clock owns gravity even if the client sends no more packets. */
    public boolean advanceMotion(int tick){
        if(tick<=lastPhysicsTick)return false;
        lastPhysicsTick=tick;boolean changed=false;
        for(int i=0;i<6;i++)if(jumps[i].active()){
            // A malformed/custom scene must not trap a hunter in a void or contact forever.
            if(tick-jumpStartedTick[i]>30){setRoom(i,safeRooms[i]);changed=true;continue;}
            jumps[i].advance(.1,HunterPosture.CROUCH_HEIGHT,jumpCollision(i));
            xs[i]=jumps[i].x();ys[i]=jumps[i].y();zs[i]=jumps[i].z();changed=true;
            if(!jumps[i].airborne())momentumX[i]=momentumZ[i]=0;
            if(!jumps[i].active())crouched[i]=requestedCrouch[i]||!HunterPosture.fits(map,new LevelDefinition.Vec(xs[i],ys[i],zs[i]),false);
        }
        return changed;
    }
    private final double[] xs=new double[6],ys=new double[6],zs=new double[6];
    private final int[] headings={270,270,270,270,270,270};
    private final int[] lastTick={-1,-1,-1,-1,-1,-1};
    private final int[] safeRooms={14,14,14,14,14,14};
    public void recoverBelowGround(){
        for(int i=0;i<6;i++){
            if(jumps[i].active())continue;
            var old=new LevelDefinition.Vec(xs[i],ys[i],zs[i]);var at=GroundRecovery.recover(map,old,safeRooms[i],HunterPosture.height(crouched[i]));
            if(!at.equals(old)){xs[i]=at.x();ys[i]=at.y();zs[i]=at.z();hiding[i]=false;shrubHiding[i]=false;}
            int r=map.roomAt(xs[i],ys[i],zs[i]);if(r>=0&&map.traversable(xs[i],ys[i],zs[i],HunterPosture.height(crouched[i])))safeRooms[i]=r;
        }
    }
    private final int[] lastDoorTick={-3,-3,-3,-3,-3,-3};
    private double playerSpeed=1.0,sprintSpeed=1.5;
    private int hidingDensity=70;
    private final boolean[] hiding=new boolean[6],crouched=new boolean[6],shrubHiding=new boolean[6];
    public boolean crouched(int seat){seat(seat);return crouched[seat];}
    public boolean posture(int seat,boolean low){
        seat(seat);if(jumps[seat].active()){requestedCrouch[seat]=low;return true;}var b=body(seat);
        if(!HunterPosture.fits(map,new LevelDefinition.Vec(b.x(),b.y(),b.z()),low))throw new IllegalArgumentException("not enough headroom to stand");
        requestedCrouch[seat]=low;crouched[seat]=low;if(!low&&shrubHiding[seat]){hiding[seat]=false;shrubHiding[seat]=false;}return low;
    }
    public String postureFrame(int seat,int tick){return "POSTURE|"+seat+"|"+(crouched(seat)?1:0)+"|"+tick;}
    public void revealOutdoorNear(double x,double y,double z){
        for(int i=0;i<6;i++)if(shrubHiding[i]&&Math.abs(ys[i]-y)<1.6&&Math.hypot(xs[i]-x,zs[i]-z)<2.4){hiding[i]=false;shrubHiding[i]=false;}
    }
    private boolean exitSealed;
    /** The investigation/van boundary is sealed only while a true Frenzy is active. */
    public void setExitSealed(boolean sealed){exitSealed=sealed;}
    public boolean exitSealed(){return exitSealed;}
    public boolean hidden(int seat){seat(seat);if(shrubHiding[seat]&&(!crouched[seat]||OutdoorHiding.containing(map.level(),xs[seat],ys[seat],zs[seat],hidingDensity)==null)){hiding[seat]=false;shrubHiding[seat]=false;}return hiding[seat];}
    public boolean toggleHide(int seat){
        seat(seat);
        if(hidden(seat)){hiding[seat]=false;shrubHiding[seat]=false;return false;}
        if(crouched[seat]&&OutdoorHiding.containing(map.level(),xs[seat],ys[seat],zs[seat],hidingDensity)!=null){hiding[seat]=true;shrubHiding[seat]=true;return true;}
        var pocket=map.hidingSpot(body(seat).room(),hidingDensity);
        if(pocket==null||!LevelClosets.inside(map.level().socket("hide",body(seat).room()),xs[seat],ys[seat],zs[seat]))
            throw new IllegalArgumentException("no hiding place within reach");
        hiding[seat]=true;return true;
    }
    private CustomContract.Sprint sprint=CustomContract.Sprint.LIMITED;
    private final int[] stamina={40,40,40,40,40,40};
    private final int[] lastSprintTick={-1000,-1000,-1000,-1000,-1000,-1000};
    private int maxStamina=40;
    public void configure(CustomContract c){
        playerSpeed=c.playerSpeed()/100.0;sprintSpeed=c.sprintSpeed()/100.0;sprint=c.sprint();
        maxStamina=c.sprint()==CustomContract.Sprint.DISABLED?0:c.hunterSprintSeconds()*10;
        java.util.Arrays.fill(stamina,maxStamina);
        java.util.Arrays.fill(lastSprintTick,-1000);
        hidingDensity=c.hidingSpots();
        map.configureHiding(hidingDensity);
    }
    public LanWorld(HouseMap map){
        this.map=java.util.Objects.requireNonNull(map);
        for(int i=0;i<6;i++){
            jumps[i]=new org.beckenburg.editor.DefaultPlayerMotion();
            setRoom(i,HouseMap.EXTRACTION_ROOM);
            var pad=map.van().spawn(i);xs[i]=pad.x();zs[i]=pad.z(); // Two columns, three rows, in the van's local frame.
            if(!map.traversable(xs[i],ys[i],zs[i]))throw new IllegalStateException("invalid van spawn pad");
        }
    }
    private static void seat(int id){if(id<0||id>=6)throw new IllegalArgumentException("hunter seat required");}
    /** Room-only clients are synchronized to room centers; world-mode never uses this teleporter. */
    public void setRoom(int seat,int room){
        seat(seat);
        if(room<0||room>=HouseMap.ROOM_COUNT)throw new IllegalArgumentException("invalid room");
        jumps[seat].reset();requestedCrouch[seat]=false;momentumX[seat]=momentumZ[seat]=0;
        var spawn=map.spawn(room);xs[seat]=spawn.x();ys[seat]=spawn.y();zs[seat]=spawn.z();safeRooms[seat]=room;hiding[seat]=false;shrubHiding[seat]=false;crouched[seat]=false;
    }
    public Body body(int seat){seat(seat);return new Body(xs[seat],ys[seat],zs[seat],headings[seat],map.roomAt(xs[seat],ys[seat],zs[seat]));}
    /** A stationary player recharges on the same host 10 Hz clock as movement. */
    public void advanceStamina(int tick){
        recoverBelowGround();
        if(tick<0)throw new IllegalArgumentException("invalid tick");
        if(sprint!=CustomContract.Sprint.LIMITED)return;
        for(int i=0;i<6;i++)
            if(tick-lastSprintTick[i]>3&&tick%2==0)
                stamina[i]=Math.min(maxStamina,stamina[i]+1);
    }
    public int stamina(int seat){seat(seat);return stamina[seat];}
    public int staminaCapacity(){return maxStamina;}
    public String staminaFrame(int seat,int tick){
        seat(seat);
        return "STAMINA|"+stamina[seat]+"|"+maxStamina+"|"
            +(tick==lastSprintTick[seat]?1:0);
    }
    public boolean move(int seat,int dx,int dz,int tick){return move(seat,dx,dz,tick,false);}
    public boolean move(int seat,int dx,int dz,int tick,boolean wantsSprint){
        seat(seat);
        if(dx< -100||dx>100||dz< -100||dz>100||(dx==0&&dz==0))throw new IllegalArgumentException("invalid motion vector");
        if(tick<=lastTick[seat])throw new IllegalArgumentException("movement rate exceeded");
        lastTick[seat]=tick; // failed collisions and airborne input consume the allowance.
        if(jumps[seat].active())return false; // takeoff momentum owns airborne travel.
        double magnitude=Math.hypot(dx,dz);
        boolean running=wantsSprint&&!crouched[seat]&&sprint!=CustomContract.Sprint.DISABLED
            &&(sprint==CustomContract.Sprint.UNLIMITED||stamina[seat]>0);
        double speed=SPEED*playerSpeed*HunterPosture.speed(crouched[seat])*(running?sprintSpeed:1);
        double x=xs[seat]+speed*dx/magnitude,z=zs[seat]+speed*dz/magnitude;
        var destination=map.resolveMove(new LevelDefinition.Vec(xs[seat],ys[seat],zs[seat]),x,z,HunterPosture.height(crouched[seat]));
        if(destination==null)throw new IllegalArgumentException("blocked by world collision");
        if(exitSealed && body(seat).room()!=HouseMap.EXTRACTION_ROOM
                &&map.roomAt(destination.x(),destination.y(),destination.z())==HouseMap.EXTRACTION_ROOM)
            throw new IllegalArgumentException("frenzy sealed the van extraction threshold");
        // Forbid diagonal corner cutting; each axis must independently clear the collision radius.
        if(map.resolveMove(new LevelDefinition.Vec(xs[seat],ys[seat],zs[seat]),x,zs[seat],HunterPosture.height(crouched[seat]))==null
            ||map.resolveMove(new LevelDefinition.Vec(xs[seat],ys[seat],zs[seat]),xs[seat],z,HunterPosture.height(crouched[seat]))==null)
            throw new IllegalArgumentException("blocked by world collision");
        momentumX[seat]=(destination.x()-xs[seat])*10;momentumZ[seat]=(destination.z()-zs[seat])*10;
        xs[seat]=destination.x();ys[seat]=destination.y();zs[seat]=destination.z();hiding[seat]=false;shrubHiding[seat]=false;
        if(running){
            lastSprintTick[seat]=tick;
            if(sprint==CustomContract.Sprint.LIMITED)stamina[seat]--;
        }
        return true;
    }
    public void turn(int seat,int heading){seat(seat);if(heading<0||heading>=360)throw new IllegalArgumentException("invalid heading");headings[seat]=heading;}
    public boolean door(int seat,int tick){
        seat(seat);
        if(tick-lastDoorTick[seat]<3)throw new IllegalArgumentException("door interaction cooling down");
        if(!map.toggleNearestDoor(xs[seat],ys[seat],zs[seat]))return false;
        for(int i=0;i<6;i++)if(!map.traversable(xs[i],ys[i],zs[i])){
            // Never close the door through a player's physical capsule.
            map.toggleNearestDoor(xs[seat],ys[seat],zs[seat]);
            throw new IllegalArgumentException("door occupied");
        }
        lastDoorTick[seat]=tick;return true;
    }
    public boolean window(int seat,int tick){
        seat(seat);
        if(tick-lastDoorTick[seat]<3)throw new IllegalArgumentException("window interaction cooling down");
        if(!map.toggleNearestWindow(xs[seat],ys[seat],zs[seat]))return false;
        lastDoorTick[seat]=tick;return true;
    }
    public String bodyFrame(int seat,int tick){
        Body b=body(seat);
        return "BODY|"+seat+"|"+Math.round(b.x()*1000)+"|"+Math.round(b.y()*1000)+"|"+Math.round(b.z()*1000)+"|"+b.heading()+"|"+tick;
    }
}
