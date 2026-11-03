package org.beckenburg;

import java.util.Arrays;

public final class HunterSpriteAnimationTest {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static SceneRenderer.RemoteBody body(double x,boolean crouch){
        return new SceneRenderer.RemoteBody(x,0,4,0,180,"cleric",crouch);
    }
    public static void main(String[] args){
        var tracker=new HunterSpriteAnimation.Tracker();
        check(tracker.sample(body(0,false),0)==HunterSpriteAnimation.Pose.IDLE,"first snapshot is idle");
        var a=tracker.sample(body(.1,false),.1);
        var b=tracker.sample(body(.4,false),.2);
        check(a==HunterSpriteAnimation.Pose.WALK_A&&b==HunterSpriteAnimation.Pose.WALK_B,"received movement advances gait");
        check(tracker.sample(body(.4,false),.5)==HunterSpriteAnimation.Pose.IDLE,"stationary snapshots stop walking");
        check(tracker.sample(body(.4,true),.6)==HunterSpriteAnimation.Pose.CROUCH,"host crouch selects authored pose");
        check(tracker.sample(body(8,false),.7)==HunterSpriteAnimation.Pose.IDLE,"teleport does not animate walking");
        check(tracker.sample(body(8,false),.1)==HunterSpriteAnimation.Pose.IDLE,"clock reset clears gait");
        for(String identity:CharacterVisuals.HUNTERS)for(int facing:new int[]{0,2,4,5}){
            var walkA=HunterSpriteAnimation.frame(identity,facing,HunterSpriteAnimation.Pose.WALK_A);
            var walkB=HunterSpriteAnimation.frame(identity,facing,HunterSpriteAnimation.Pose.WALK_B);
            var crouch=HunterSpriteAnimation.frame(identity,facing,HunterSpriteAnimation.Pose.CROUCH);
            check(!Arrays.equals(walkA.getRGB(0,0,128,224,null,0,128),walkB.getRGB(0,0,128,224,null,0,128)),"distinct walking frames "+identity);
            check(!Arrays.equals(walkA.getRGB(0,0,128,224,null,0,128),crouch.getRGB(0,0,128,224,null,0,128)),"authored crouch "+identity);
        }
        for(String identity:CharacterVisuals.HUNTERS)for(var pose:HunterSpriteAnimation.Pose.values()){
            int[] pixels=new int[SceneRenderer.W*SceneRenderer.H];
            double[] depths=new double[pixels.length];
            var body=new SceneRenderer.RemoteBody(0,0,4,0,180,identity,pose==HunterSpriteAnimation.Pose.CROUCH);
            Arrays.fill(depths,Double.POSITIVE_INFINITY);
            DirectionalSprites.drawHunter(pixels,depths,0,0,0,0,body,pose,0,true,SceneRenderer.W,SceneRenderer.H,153);
            check(Arrays.stream(pixels).anyMatch(p->p!=0),"real pose raster "+identity+pose);
            Arrays.fill(pixels,0);Arrays.fill(depths,1);
            DirectionalSprites.drawHunter(pixels,depths,0,0,0,0,body,pose,0,true,SceneRenderer.W,SceneRenderer.H,153);
            check(Arrays.stream(pixels).allMatch(p->p==0),"wall occludes animated hunter "+identity+pose);
        }
        System.out.println("PASS hunter pose atlas, received movement, stop, crouch, teleport, clock reset and depth occlusion");
    }
}
