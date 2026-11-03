package org.beckenburg;
import java.nio.file.*;import java.util.*;
/** Actual fresh native Castle: arrival flood, real portal leaves, indoor support and save parity. */
public final class CastleTrainingAnnexTest {
 static void check(boolean ok,String reason){if(!ok)throw new AssertionError(reason);}
 static String key(double x,double z){return Math.round(x*8)+":"+Math.round(z*8);}
 public static void main(String[] args)throws Exception{
  Path home=Files.createTempDirectory("castle-annex-");String old=System.getProperty("user.home");System.setProperty("user.home",home.toString());
  try{
   var scene=CastleLevelEdits.current();var map=new CastleHubMap();double[] spawn=scene.spawn();
   var queue=new ArrayDeque<double[]>();var seen=new HashSet<String>();var reached=new ArrayList<double[]>();queue.add(new double[]{spawn[0],spawn[1]});seen.add(key(spawn[0],spawn[1]));
   while(!queue.isEmpty()){
    var p=queue.remove();reached.add(p);
    for(double[] step:new double[][]{{.125,0},{-.125,0},{0,.125},{0,-.125}}){double x=p[0]+step[0],z=p[1]+step[1];if(map.traversable(x,z)&&seen.add(key(x,z)))queue.add(new double[]{x,z});}
    check(seen.size()<250000,"Bounded real Castle flood");
   }
   for(var room:CastleAnnex.SPACES){
    double x=(room.x0()+room.x1())/2,z=(room.z0()+room.z1())/2;
    check(reached.stream().anyMatch(p->Math.hypot(p[0]-x,p[1]-z)<.35),"Actual spawn cannot reach "+room.id());
    check(!map.outside(x,z),"Training room must be indoors: "+room.id());
    check(CastleRooms.circuit(x,1,z).equals("practice"),"Training uses authored practice circuit");
    check(scene.lamps().stream().anyMatch(l->l.id().equals("annex-light-"+room.id())&&Math.abs(l.y()-CastleRooms.mountHeight(x,z,false))<1e-7),"Actual lamp root touches ceiling underside");
   }
   var lighting=new FixtureLighting(map);
   for(var room:CastleAnnex.SPACES){
    double x=(room.x0()+room.x1())/2,z=(room.z0()+room.z1())/2;
    lighting.castleFrame(false,10);double dark=lighting.sample(0,x,1.4,z).energy();
    lighting.castleFrame(true,10);double lit=lighting.sample(0,x,1.4,z).energy();
    check(lit>dark+.2,"Actual training light reaches room "+room.id()+": "+dark+" -> "+lit);
    var lamp=scene.lamps().stream().filter(l->l.id().equals("annex-light-"+room.id())).findFirst().orElseThrow();
    check(lamp.fixture().attached(map),"Annex fixture is attached to real room ceiling");
    check(!scene.wallBlocks(lamp.x(),lamp.y()-.1,lamp.z(),.48),"Fixture mesh remains clear of timber/masonry");
   }
   check(!map.traversable(-11,19)&&!map.traversable(-5,19),"Real solid partitions separate bays");
   var slot=CastleDoorSlots.all().stream().filter(s->s.horizontal()&&Math.abs(s.centerX()+14)<1e-7&&Math.abs(s.centerZ()-24)<1e-7).findFirst().orElseThrow();
   scene.installDoor(slot.id(),CastleDoorSlots.Type.WOOD);
   check(!map.traversable(-14,24),"Installed training leaf blocks actual player");check(!FixtureLighting.castleRayClear(map,-14,1.4,23,-14,1.4,25),"Installed leaf blocks actual light through the same doorway");map.toggleDoor(slot.id());check(map.traversable(-14,24),"Opened training leaf clears actual aperture");check(FixtureLighting.castleRayClear(map,-14,1.4,23,-14,1.4,25),"Opened leaf clears the optical aperture");
   var expected=scene.snapshot();scene.save();check(new CastleLevelEdits(scene.path()).snapshot().equals(expected),"Whole native annex roundtrips exactly");
   if(args.length>0){var r=new SceneRenderer();r.setRenderSize(1000,625);r.x=-8;r.z=27.5;r.eyeHeight=1.62;r.yaw=Math.PI;r.pitch=-.04;r.lamp=false;r.drawCastle(map,0,true);Path out=Path.of(args[0]).toAbsolutePath();Files.createDirectories(out.getParent());javax.imageio.ImageIO.write(r.image(),"png",out.toFile());}
   System.out.println("PASS actual Castle training annex: "+seen.size()+" physical positions from native spawn reach all4 rooms; real walls, closed/open leaf, indoor lighting circuit, supported fixtures and exact save/reopen");
  }finally{System.setProperty("user.home",old);try(var paths=Files.walk(home)){for(Path p:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
 }
}
