package org.beckenburg;

/** Deterministic 20ms render / 110ms host-clock and collision regression checks. */
public final class LanVisualMotionTest {
    private static void check(boolean yes, String message) {if(!yes)throw new AssertionError(message);}
    private static final LanVisualMotion.Collision FREE=(p,x,z)->new LanVisualMotion.Pose(x,p.y(),z);
    private static long ms(int ms) {return ms*1_000_000L;}
    public static void main(String[] args) {
        LanVisualMotion motion=new LanVisualMotion();
        long start=ms(1000);
        motion.accept(new LanVisualMotion.Pose(2,0,3),start);
        double lastX=2;
        for(int t=0;t<=100;t+=20){
            var p=motion.sample(start+ms(t),.020,1,0,FREE);
            check(p.x()>=lastX-1e-8,"jitter before first host step: "+p.x()+" < "+lastX);
            check(p.x()<=2.37,"unbounded prediction before first step");
            lastX=p.x();
        }
        motion.accept(new LanVisualMotion.Pose(2.31,0,3),start+ms(110));
        var follow=motion.sample(start+ms(120),.020,1,0,FREE);
        check(follow.x()>lastX,"authoritative step must not jump camera backwards");
        check(follow.x()-lastX<.10,"no discrete 31cm host jump should reach the camera");
        double newest=follow.x();
        for(int t=140;t<=300;t+=20){
            var next=motion.sample(start+ms(t),.020,1,0,FREE);
            check(next.x()>=newest-1e-8,"rejected/delayed host must not oscillate");
            check(next.x()<=2.31+.31*1.15+1e-8,"stale host must cap prediction to one step");
            newest=next.x();
        }
        var stop=motion.sample(start+ms(320),.02,0,0,FREE);
        check(Math.abs(stop.x()-newest)<.025,"key release must not immediately rubber-band");
        for(int t=340;t<=650;t+=20)stop=motion.sample(start+ms(t),.02,0,0,FREE);
        check(Math.abs(stop.x()-2.31)<.01,"host authority restored after stopping");
        motion.reset();
        motion.accept(new LanVisualMotion.Pose(2,0,3),start);
        LanVisualMotion.Collision WALL=(p,x,z)->x>2.12?null:new LanVisualMotion.Pose(x,p.y(),z);
        for(int t=0;t<=240;t+=20){
            var p=motion.sample(start+ms(t),.02,1,0,WALL);
            check(p.x()<=2.12,"collision wall must not be visually crossed");
        }
        motion.accept(new LanVisualMotion.Pose(17,2,19),start+ms(250));
        var teleport=motion.sample(start+ms(250),0,0,0,FREE);
        check(teleport.x()==17&&teleport.y()==2&&teleport.z()==19,"teleport/recovery snaps immediately");
        motion.reset();
        motion.accept(new LanVisualMotion.Pose(2,0,3),start);
        for(int t=0;t<=60;t+=20)motion.sample(start+ms(t),.02,1,0,FREE);
        motion.accept(new LanVisualMotion.Pose(2,0,3),start+ms(70)); // repeated TURN broadcast
        var repeated=motion.sample(start+ms(80),.02,1,0,FREE);
        check(repeated.x()>2.08,"duplicate authoritative pose must not reset prediction clock");
        // A player holding W while turning the mouse by 20 degrees per render
        // frame must keep predicting between real host BODY updates. The old
        // cosine .97 restart threshold reset every one of these frames and
        // made the camera remain motionless while the server kept walking.
        motion.reset();
        motion.accept(new LanVisualMotion.Pose(2,0,3),start);
        LanVisualMotion.Pose turning=null;
        for(int frameIndex=0;frameIndex<=5;frameIndex++){
            double bearing=Math.toRadians(frameIndex*20);
            turning=motion.sample(start+ms(frameIndex*20),.02,
                Math.cos(bearing),Math.sin(bearing),FREE);
        }
        double turnDistance=Math.hypot(turning.x()-2,turning.z()-3);
        check(turnDistance>.12&&turnDistance<=.37,
            "mouse steering must not restart visual prediction every frame");
        // A real instantaneous movement-key reversal is different from
        // rotating a held key: it should restart the prediction to avoid
        // carrying an obsolete velocity into the opposite direction.
        double opposite=Math.toRadians(280);
        var reversed=motion.sample(start+ms(120),.02,
            Math.cos(opposite),Math.sin(opposite),FREE);
        check(Math.hypot(reversed.x()-2,reversed.z()-3)<turnDistance,
            "sharp opposing input must restart the bounded visual epoch");

        // The user opens a journal mid-step. Authoritative input is neutral
        // and camera extrapolation must immediately stop without using a
        // stale held-key direction when the journal closes.
        motion.reset();
        motion.accept(new LanVisualMotion.Pose(2,0,3),start);
        var initialIntent=motion.sample(start,.02,1,0,FREE);
        check(initialIntent.x()==2,"new input starts at the host pose, not a fabricated step");
        var midStep=motion.sample(start+ms(80),.02,1,0,FREE);
        check(midStep.x()>2.05,"test must start with a real predicted camera step");
        motion.suspend();
        var paused=motion.sample(start+ms(100),.02,0,0,FREE);
        check(paused.x()==2&&paused.y()==0&&paused.z()==3,
            "menu/focus suspension must discard predicted travel immediately");
        motion.accept(new LanVisualMotion.Pose(2.12,0,3),start+ms(110));
        motion.suspend();
        var hostCorrection=motion.sample(start+ms(130),.02,0,0,FREE);
        check(hostCorrection.x()==2.12,
            "suspended camera uses newer host position without inventing movement");
        var freshIntent=motion.sample(start+ms(140),.02,1,0,FREE);
        check(freshIntent.x()==2.12,"re-entering gameplay must start a fresh prediction epoch");
        var movingAgain=motion.sample(start+ms(160),.02,1,0,FREE);
        check(movingAgain.x()>freshIntent.x(),"movement resumes promptly on new held input");

        motion.reset();
        motion.accept(new LanVisualMotion.Pose(2,0,3),start);
        int[] sweeps={0};
        LanVisualMotion.Collision counted=(p,x,z)->{
            sweeps[0]++;
            return new LanVisualMotion.Pose(x,p.y(),z);
        };
        for(int render=0;render<=100;render+=20)
            motion.sample(start+ms(render),.02,1,0,counted);
        check(sweeps[0]==1,"same heading and host pose must reuse one collision sweep");
        motion.invalidateCollision();
        motion.sample(start+ms(120),.02,1,0,counted);
        check(sweeps[0]==2,"door or barrier changes must invalidate the swept segment");
        System.out.println("PASS: bounded responsive render-clock prediction, stop, correction, collision, teleport, duplicate packets, swept-cache reuse");
    }
}
