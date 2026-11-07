package org.beckenburg.editor;

/** Portable default player jump. Units are metres and seconds; adapters own collision.
 * Ground locomotion supplies takeoff velocity. Air input cannot create extra speed.
 */
public final class DefaultPlayerMotion {
    public static final double GRAVITY=18, TAKEOFF_SPEED=4.8, TUCK=.73, LAND_SECONDS=.12;
    public interface Collision {
        boolean clear(double x,double y,double z,double height);
        /** Highest support crossed while descending, or NaN. */
        double landing(double x,double z,double fromY,double toY);
    }
    private double x,y,z,vx,vy,vz,landingTime;
    private boolean airborne;
    public boolean active(){return airborne||landingTime>0;}
    public boolean airborne(){return airborne;}
    public double x(){return x;} public double y(){return y;} public double z(){return z;}
    public void reset(){airborne=false;landingTime=0;vx=vy=vz=0;}
    public void jump(double x,double y,double z,double vx,double vz,double height,Collision collision){
        if(active())throw new IllegalArgumentException("jump requires grounded feet");
        if(!collision.clear(x,y+TUCK,z,height))throw new IllegalArgumentException("not enough headroom to jump");
        this.x=x;this.y=y+TUCK;this.z=z;this.vx=vx;this.vz=vz;vy=TAKEOFF_SPEED;airborne=true;
    }
    /** Small sweeps prevent tunnelling even at the host's 10 Hz update rate. */
    public void advance(double seconds,double height,Collision collision){
        if(!Double.isFinite(seconds)||seconds<0||seconds>.25)throw new IllegalArgumentException("invalid physics step");
        int steps=Math.max(1,(int)Math.ceil(seconds/.01));double dt=seconds/steps;
        for(int i=0;i<steps;i++){
            if(!airborne){landingTime=Math.max(0,landingTime-dt);continue;}
            double nx=x+vx*dt,nz=z+vz*dt;
            if(collision.clear(nx,y,z,height))x=nx;else vx=0;
            if(collision.clear(x,y,nz,height))z=nz;else vz=0;
            vy-=GRAVITY*dt;double next=y+vy*dt;
            double floor=vy<=0?collision.landing(x,z,y,next):Double.NaN;
            if(Double.isFinite(floor)){
                y=floor;airborne=false;landingTime=LAND_SECONDS;vx=vy=vz=0;
            }else if(collision.clear(x,next,z,height))y=next;
            else {if(vy>0)vy=0;else {vx=vz=0;}}
        }
    }
}
