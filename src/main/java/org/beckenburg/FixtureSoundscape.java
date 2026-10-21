package org.beckenburg;
/** Original deterministic PSX-inspired device loops; no sampled songs or third-party recordings. */
public final class FixtureSoundscape {
 private FixtureSoundscape(){}
 public static final int RATE=16000;
 public enum Sound { TELEPHONE,BEDSIDE_BELL,OLD_FIRE_GONG,SMOKE_ALARM,RADIO,BOOM_BOX,RUNNING_WATER,WASHER,TV_STATIC }
 public static Sound sound(HauntedFixtures.Kind kind){return switch(kind.name()){
  case "PHONE"->Sound.TELEPHONE;case "ALARM_CLOCK"->Sound.BEDSIDE_BELL;case "FIRE_ALARM"->Sound.OLD_FIRE_GONG;case "SMOKE_ALARM"->Sound.SMOKE_ALARM;
  case "RADIO"->Sound.RADIO;case "BOOM_BOX"->Sound.BOOM_BOX;case "WASHING_MACHINE"->Sound.WASHER;case "TELEVISION"->Sound.TV_STATIC;default->Sound.RUNNING_WATER;};}
 public static int frames(Sound kind){return RATE*switch(kind){case TELEPHONE->4;case RADIO,BOOM_BOX,WASHER->8;case SMOKE_ALARM->3;default->2;};}
 private static final int[] RADIO_NOTES={60,64,67,71,69,64,62,67,65,69,72,69,67,64,62,59},BOOM_NOTES={48,55,60,58,53,60,65,63,55,62,67,65,53,60,58,55};
 private static double noise(long frame){long h=frame*0x9e3779b97f4a7c15L+0x4245434bL;h^=h>>>29;h*=0xbf58476d1ce4e5b9L;return ((h>>>40)&0xffffff)/8388607.5-1;}
 private static double sine(double hz,double time){return Math.sin(Math.PI*2*hz*time);}
 private static double triangle(double hz,double time){return 2/Math.PI*Math.asin(sine(hz,time));}
 private static double envelope(double time,double length){return Math.min(1,Math.min(time/.008,(length-time)/.008));}
 public static short sample(Sound kind,long frame){
  long n=Math.floorMod(frame,frames(kind));double t=n/(double)RATE,v=0;
  switch(kind){
   case TELEPHONE -> {double beat=t%1.0;if(t<2&&beat<.68)v=(sine(425,t)*.22+sine(655,t)*.16)*(0.45+0.55*Math.abs(sine(22,t)))*envelope(beat,.68);}
   case BEDSIDE_BELL -> {double a=t%.11;v=(sine(1650,t)*.19+sine(2267,t)*.13+sine(3401,t)*.07)*Math.exp(-a*17);}
   case OLD_FIRE_GONG -> {double a=t%.15;v=(sine(718,t)*.21+sine(1133,t)*.13+sine(1977,t)*.07)*Math.exp(-a*11);}
   case SMOKE_ALARM -> {double beat=t%.5;if(t<1.5&&beat<.28)v=(triangle(2850,t)*.30+triangle(3100,t)*.09)*envelope(beat,.28);}
   case RADIO,BOOM_BOX -> {
    // Two newly composed eight-bar phrases. These are not transcriptions of existing songs.
    int[] melody=kind==Sound.RADIO?RADIO_NOTES:BOOM_NOTES;
    double step=.25,phase=t%step,hz=440*Math.pow(2,(melody[(int)(t/step)%melody.length]-69)/12.0);
    v=triangle(hz,t)*.17*Math.exp(-phase*4)+triangle(hz/2,t)*.045;
    double drum=t%.5;v+=sine(65-45*drum,drum)*Math.exp(-drum*38)*(kind==Sound.BOOM_BOX?.19:.06);
    if(t%.5>.25)v+=noise(n/3)*Math.exp(-(t%.5-.25)*55)*.035;
    v+=noise(n/4)*(kind==Sound.RADIO?.012:.003);
   }
   case RUNNING_WATER -> {v=(noise(n/2)*.19+noise(n/5)*.12+noise(n/13)*.06)*(0.82+.18*sine(3,t));double a=t%.31;v+=sine(1150,a)*Math.exp(-a*50)*.07;}
   case TV_STATIC -> {v=(noise(n)*.17+noise(n/3)*.06)*(0.91+.09*sine(50,t));}
   case WASHER -> {v=(sine(58,t)*.11+sine(117,t)*.035)*(0.65+.35*sine(1.15,t))+noise(n/7)*.025;double a=t%.73;v+=sine(83,a)*Math.exp(-a*26)*.11;}
  }
  return (short)(Math.round(Math.max(-.72,Math.min(.72,v))*120)*256);
 }
 /** Stereo spatialization happens after the original mono sound; no hidden coordinates are needed. */
 public static byte[] render(Sound kind,long first,int count,int pan,int volume){
  if(count<0||count>RATE*30||pan< -100||pan>100||volume<0||volume>100)throw new IllegalArgumentException("bounded fixture PCM required");
  byte[] pcm=new byte[count*4];double p=pan/100.0,l=Math.sqrt((1-p)/2)*volume/100d,r=Math.sqrt((1+p)/2)*volume/100d;
  for(int i=0;i<count;i++){int s=sample(kind,first+i),left=(int)Math.round(s*l),right=(int)Math.round(s*r),at=i*4;pcm[at]=(byte)left;pcm[at+1]=(byte)(left>>8);pcm[at+2]=(byte)right;pcm[at+3]=(byte)(right>>8);}return pcm;
 }
}
