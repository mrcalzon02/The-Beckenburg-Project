package org.beckenburg;
import java.util.*;
public final class FixtureSoundscapeTest {
 static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
 public static void main(String[]args){
  Set<Integer> hashes=new HashSet<>();for(var kind:FixtureSoundscape.Sound.values()){
   byte[] full=FixtureSoundscape.render(kind,0,32000,0,100),head=FixtureSoundscape.render(kind,0,7000,0,100),tail=FixtureSoundscape.render(kind,7000,25000,0,100);
   check(Arrays.equals(Arrays.copyOfRange(full,0,head.length),head)&&Arrays.equals(Arrays.copyOfRange(full,head.length,full.length),tail),"loop phase restarts "+kind);hashes.add(Arrays.hashCode(full));int peak=0;
   for(int n=0;n<FixtureSoundscape.frames(kind);n++){int s=FixtureSoundscape.sample(kind,n);peak=Math.max(peak,Math.abs(s));check(s%256==0,"not quantized original PCM");}check(peak>3000&&peak<30000,"silent or clipped loop "+kind);
  }check(hashes.size()==9,"device loops are not distinct");
  var gate=new LanAudio.FixtureGate();gate.state(1,true);gate.state(2,true);int first=gate.generation(1),second=gate.generation(2);
  byte[] a=FixtureSoundscape.render(FixtureSoundscape.Sound.TELEPHONE,0,512,0,70),b=FixtureSoundscape.render(FixtureSoundscape.Sound.RADIO,0,512,0,70);
  gate.state(1,false);byte[] mixed=LanAudio.mixFixtureBlock(new byte[][]{a,b},new int[]{1,2},new int[]{first,second},gate);check(Arrays.equals(mixed,b),"stopping telephone cuts unrelated radio or leaves ringing");
  gate.state(1,true);mixed=LanAudio.mixFixtureBlock(new byte[][]{a,b},new int[]{1,2},new int[]{first,second},gate);check(Arrays.equals(mixed,b),"off/on resurrects stale queued voice");
  gate.state(2,false);check(Arrays.equals(LanAudio.mixFixtureBlock(new byte[][]{a,b},new int[]{1,2},new int[]{first,second},gate),new byte[2048]),"off sources leak audio into next mixer block");
  check(Arrays.equals(LanAudio.mixFixtureBlock(new byte[][]{b},new int[]{-1},new int[]{0},gate),b),"fixture mute cuts unrelated game sound");
  var map=new HouseMap();var f=map.fixtures().states().stream().filter(s->s.kind()==HauntedFixtures.Kind.PHONE).findFirst().orElseThrow();var operator=HauntedFixturesTest.operator(map,f);
  map.fixtures().toggle(f.id(),operator,1);map.fixtures().advance(10000);check(map.fixtures().state(f.id()).active(),"player-started sound times out");
  map.fixtures().toggle(f.id(),operator,10001);check(!map.fixtures().state(f.id()).active(),"player cannot stop own sound");
  map.fixtures().activate(f.id(),f.room(),10002);map.fixtures().advance(10203);check(!map.fixtures().state(f.id()).active(),"ghost activation lost bounded duration");
  System.out.println("PASS nine distinct original quantized loops, continuous phases, source-specific PCM cancellation, stale voice rejection, independent sounds and manual/ghost lifetimes");
 }
}
