package org.beckenburg;
public final class CrouchCameraTest {
 private static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
 private static double after(int fps,double seconds){var c=new CrouchCamera();for(int i=0;i<Math.round(fps*seconds);i++)c.advance(true,1.0/fps);return c.eye();}
 public static void main(String[] args){
  var c=new CrouchCamera();double first=c.advance(true,1.0/60);
  check(first<HunterPosture.STANDING_EYE&&first>HunterPosture.CROUCH_EYE+.5,"first frame moves slightly, not a full crouch snap");
  check(Math.abs(after(30,.5)-after(60,.5))<1e-10&&Math.abs(after(60,.5)-after(144,.5))<1e-10,"same easing at different frame rates");
  for(int i=0;i<100;i++){double before=c.eye();c.advance(true,1.0/60);check(c.eye()<=before&&c.eye()>=HunterPosture.CROUCH_EYE,"crouch converges without overshoot");}
  check(c.settled(true)&&c.eye()==HunterPosture.CROUCH_EYE,"exact settled crouch");
  for(int i=0;i<100;i++){double before=c.eye();c.advance(false,1.0/60);check(c.eye()>=before&&c.eye()<=HunterPosture.STANDING_EYE,"stand converges without overshoot");}
  check(c.settled(false),"settled standing");
  for(int i=0;i<120;i++){double before=c.eye();c.advance(i%12<6,1.0/60);check(Math.abs(c.eye()-before)<.09,"rapid direction reversal remains continuous");check(c.eye()>=HunterPosture.CROUCH_EYE&&c.eye()<=HunterPosture.STANDING_EYE,"reversal remains within eye bounds");}
  double before=c.eye();check(c.advance(true,Double.NaN)==before&&c.advance(false,-1)==before,"invalid elapsed time never corrupts camera");
  c.reset(false);check(c.eye()==HunterPosture.STANDING_EYE&&c.settled(false),"scene reset clears in-flight smoothing");
  System.out.println("PASS crouch/stand easing: gradual motion, frame-rate independence, reversal continuity, bounds and reset");
 }
}
