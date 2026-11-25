package org.beckenburg;
import java.util.*;
public final class FixtureSoundscapeTest {
 static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
 public static void main(String[]args){
  Set<Integer> hashes=new HashSet<>();for(var kind:FixtureSoundscape.Sound.values()){
   byte[] full=FixtureSoundscape.render(kind,0,32000,0,100),head=FixtureSoundscape.render(kind,0,7000,0,100),tail=FixtureSoundscape.render(kind,7000,25000,0,100);
   check(Arrays.equals(Arrays.copyOfRange(full,0,head.length),head)&&Arrays.equals(Arrays.copyOfRange(full,head.length,full.length),tail),"loop phase restarts "+kind);hashes.add(Arrays.hashCode(full));int peak=0;
   for(int n=0;n<FixtureSoundscape.frames(kind);n++){int s=FixtureSoundscape.sample(kind,n);peak=Math.max(peak,Math.abs(s));check(s%256==0,"not quantized original PCM");}check(peak>3000&&peak<30000,"silent or clipped loop "+kind);
  }check(hashes.size()==FixtureSoundscape.Sound.values().length,"device loops are not distinct");
  var gate=new LanAudio.FixtureGate();gate.state(1,true);gate.state(2,true);int first=gate.generation(1),second=gate.generation(2);
  byte[] a=FixtureSoundscape.render(FixtureSoundscape.Sound.TELEPHONE,0,512,0,70),b=FixtureSoundscape.render(FixtureSoundscape.Sound.RADIO,0,512,0,70);
  gate.state(1,false);byte[] mixed=LanAudio.mixFixtureBlock(new byte[][]{a,b},new int[]{1,2},new int[]{first,second},gate);check(Arrays.equals(mixed,b),"stopping telephone cuts unrelated radio or leaves ringing");
  gate.state(1,true);mixed=LanAudio.mixFixtureBlock(new byte[][]{a,b},new int[]{1,2},new int[]{first,second},gate);check(Arrays.equals(mixed,b),"off/on resurrects stale queued voice");
  gate.state(2,false);check(Arrays.equals(LanAudio.mixFixtureBlock(new byte[][]{a,b},new int[]{1,2},new int[]{first,second},gate),new byte[2048]),"off sources leak audio into next mixer block");
  check(Arrays.equals(LanAudio.mixFixtureBlock(new byte[][]{b},new int[]{-1},new int[]{0},gate),b),"fixture mute cuts unrelated game sound");
  var map=Arrays.stream(AuthoredTestSupport.SITES).map(HouseMap::new)
    .filter(site->site.fixtures().states().stream().anyMatch(f->f.kind()==HauntedFixtures.Kind.PHONE))
    .findFirst().orElseThrow(()->new AssertionError("No authored telephone for the sound lifetime check"));
  var f=map.fixtures().states().stream().filter(s->s.kind()==HauntedFixtures.Kind.PHONE).findFirst().orElseThrow();
  var operator=HauntedFixturesTest.operator(map,f);
  map.fixtures().toggle(f.id(),operator,1);map.fixtures().advance(10000);check(map.fixtures().state(f.id()).active(),"player-started sound times out");
  map.fixtures().toggle(f.id(),operator,10001);check(!map.fixtures().state(f.id()).active(),"player cannot stop own sound");
  map.fixtures().activate(f.id(),f.room(),10002);map.fixtures().advance(10203);check(!map.fixtures().state(f.id()).active(),"ghost activation lost bounded duration");
  checkTowerBell();
  System.out.println("PASS distinct fixture audio, continuous phases, source-specific cancellation and physical Monastery bell");
 }
 private static void checkTowerBell(){
  var map=new HouseMap(HouseMap.Site.MONASTERY);
  var fixture=map.fixtures().states().stream()
      .filter(f->f.kind()==HauntedFixtures.Kind.TOWER_BELL).findFirst().orElseThrow();
  double z=fixture.z()-1.06,y=6;
  var observer=new LooseClutter.Observer(0,fixture.x(),y,z,0,Math.atan2(fixture.y()-y-1.63,1.06),true,1.63);
  check(map.traversable(observer.x(),observer.y(),observer.z())&&map.roomAt(observer.x(),y,z)==7,
      "Bell rope has a real standing approach");
  var aim=InteractionFocus.pick(map,observer,List.of());
  check(aim!=null&&aim.kind()==InteractionFocus.Kind.FIXTURE&&aim.id().equals(""+fixture.id()),
      "Aim must hit the actual pull rope");
  map.fixtures().power(false);
  map.fixtures().toggleFocused(fixture.id(),observer,5);
  var struck=map.fixtures().advance(6);
  check(struck.size()==1&&struck.get(0).cue()==LanSound.Cue.TOWER_BELL,"Mechanical bell rings in a blackout");
  var audible=LanSound.project(map,new LanWorld.Body(fixture.x(),y,z,0,7),7,
      fixture.x(),fixture.y(),fixture.z(),struck.get(0).cue(),6);
  check(audible!=null&&audible.volume()>0,"Bell strike produces located sound in its tower");
  var first=map.fixtures().state(fixture.id());
  map.fixtures().toggleFocused(fixture.id(),observer,12);
  check(map.fixtures().state(fixture.id()).until()==first.until(),"A ringing bell cannot be endlessly retriggered");
  map.fixtures().advance(47);
  check(!map.fixtures().state(fixture.id()).active(),"A manual strike dies away");
  map.fixtures().witness(new GhostDomain.Resolution(true,"",GhostDomain.Action.DISTURB,7,40,0,0,false),48,fixture.id());
  check(map.fixtures().advance(48).stream().anyMatch(sound->sound.cue()==LanSound.Cue.TOWER_BELL),
      "A ghost disturbance rings the same physical bell");
  var sound=FixtureSoundscape.Sound.TOWER_BELL;
  int peak=0,tail=0;
  for(int frame=0;frame<FixtureSoundscape.frames(sound);frame++){
   int sample=Math.abs(FixtureSoundscape.sample(sound,frame));
   peak=Math.max(peak,sample);
   if(frame>=FixtureSoundscape.RATE*4)tail=Math.max(tail,sample);
  }
  check(peak>3000&&tail<peak/20,"The original bronze ring has a decaying tail");
 }

}
