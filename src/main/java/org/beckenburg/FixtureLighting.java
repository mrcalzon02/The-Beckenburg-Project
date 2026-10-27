package org.beckenburg;
import java.util.*;
/** Metre-space coloured emission and architectural occlusion, independent of the camera. */
public final class FixtureLighting {
 public enum Kind { FROSTED_DOME,BARE_BULB,CANDLE_CHANDELIER,TABLE_LAMP,FLUORESCENT,WALL_SCONCE,TELEVISION,FIRELIGHT }
 public record RGB(double r,double g,double b){static final RGB ZERO=new RGB(0,0,0);public double energy(){return r+g+b;}}
 public record Source(int id,Kind kind,int room,int fixture,double x,double y,double z,double dx,double dy,double dz,double radius,double power,double red,double green,double blue){}
 private record Key(int room,long x,long y,long z){}
 private record Contribution(int source,double weight){}
 private record Shadow(int source,int room,long x,long y,long z){}
 private final HouseMap map;private final List<Source> sources=new ArrayList<>();private final Map<Key,RGB> colours=new HashMap<>();private final Map<Key,List<Contribution>> contributions=new HashMap<>();private final Map<Integer,Boolean> luminousMaterials=new HashMap<>();private final Map<Shadow,Boolean> sight=new HashMap<>();
 private String geometry="",state="";private double[] factors=new double[0];
 public FixtureLighting(HouseMap map){this.map=map;
  for(var s:map.level().sockets())if(s.type().equals("light")){
   int control=controller(s);
   if(s.style().equals("bare-bulb"))add(Kind.BARE_BULB,s.room(),0,s.x(),s.y()-.565,s.z(),0,-1,0,8,4.5,1,.86,.65);
   else if(s.style().equals("candle-chandelier")){for(int i=0;i<6;i++){double a=i*Math.PI/3;add(Kind.CANDLE_CHANDELIER,s.room(),0,s.x()+.4*Math.cos(a),s.y()-.515,s.z()+.4*Math.sin(a),0,-1,0,5.8,.55,1,.66,.31);}}
   else if(s.style().contains("utility")||s.style().contains("industrial")){for(double dx:new double[]{-.34,.34})add(Kind.FLUORESCENT,s.room(),control,s.x()+dx,s.y()-.145,s.z(),0,-1,0,9,2.8,.84,.94,1);}
   else if(s.style().contains("sconce")||s.style().contains("porch")){double yaw=-s.yaw();add(Kind.WALL_SCONCE,s.room(),0,s.x()-Math.sin(yaw)*.24,s.y()+.16,s.z()+Math.cos(yaw)*.24,0,-1,0,5,2.4,1,.78,.48);}
   else add(Kind.FROSTED_DOME,s.room(),control,s.x(),s.y()-.16,s.z(),0,-1,0,7,3.4,1,.88,.72);
  }
  for(var f:map.fixtures().states())if(f.kind()==HauntedFixtures.Kind.TABLE_LAMP)add(Kind.TABLE_LAMP,f.room(),f.id(),f.x(),f.y()+.345,f.z(),0,-1,0,3.6,2.0,1,.72,.40);
  for(var f:map.fixtures().states())if(f.kind().name().equals("TELEVISION")){double front=.345*f.scale();add(Kind.TELEVISION,f.room(),f.id(),f.x()-Math.sin(f.yaw())*front,f.y()+.32*f.scale(),f.z()+Math.cos(f.yaw())*front,-Math.sin(f.yaw()),0,Math.cos(f.yaw()),4.4,3.4,.91,.96,1);}
  for(double zz:new double[]{-1.1,.9}){var pos=map.van().world(0,zz);add(Kind.FLUORESCENT,14,0,pos.x(),VanInteriorLayout.ROOF_Y-.10,pos.z(),0,-1,0,3.6,2.2,.9,.95,1);}
  factors=new double[sources.size()];
 }
 private int controller(LevelDefinition.Socket socket){int at=socket.style().indexOf('@');if(at<0)return 0;String key=socket.style().substring(at+1);var plate=map.level().sockets().stream().filter(s->s.id().equals(key)).findFirst().orElseThrow();return map.fixtures().states().stream().filter(f->f.kind()==HauntedFixtures.Kind.LIGHT_SWITCH&&Math.hypot(f.x()-plate.x(),f.z()-plate.z())<.001).findFirst().orElseThrow().id();}
 public FixtureLighting(CastleHubMap castle){map=null;
  for(var light:CastleHubDecor.LIGHTS){var bounds=VanInteriorLayout.bounds(light.on());double y=light.y()-(light.mount()==CastleHubDecor.Mount.CEILING?(bounds.y1()-bounds.y0())*light.scale()*.55:0);
   Kind kind=light.on().contains("ceiling_lamp_4")?Kind.CANDLE_CHANDELIER:light.mount()==CastleHubDecor.Mount.WALL?Kind.WALL_SCONCE:Kind.FROSTED_DOME;
   add(kind,0,0,light.x(),y,light.z(),0,-1,0,kind==Kind.CANDLE_CHANDELIER?7.2:6.2,kind==Kind.CANDLE_CHANDELIER?4.2:3.0,1,kind==Kind.CANDLE_CHANDELIER?.69:.83,kind==Kind.CANDLE_CHANDELIER?.34:.57);
  }factors=new double[sources.size()];
 }
 public void castleFrame(boolean on,int tick){String stamp=on+":"+(tick/3);if(!state.equals(stamp)){colours.clear();state=stamp;}for(var source:sources)factors[source.id]=source.kind==Kind.CANDLE_CHANDELIER?.94+.06*Math.sin(tick*.3+source.id):on?1:0;}
 private boolean clear(double ax,double ay,double az,double bx,double by,double bz){
  if(map!=null)return map.clearLineOfSight(ax,ay,az,bx,by,bz);
  for(var wall:CastleHubArchitecturalPlan.walls())if(!wall.hasDoor()){
   double da=wall.horizontal()?az-wall.z0():ax-wall.x0(),db=wall.horizontal()?bz-wall.z0():bx-wall.x0();if(da*db>=0||Math.abs(da-db)<.00001)continue;
   double t=da/(da-db),y=ay+(by-ay)*t,along=wall.horizontal()?ax+(bx-ax)*t:az+(bz-az)*t,lo=wall.horizontal()?Math.min(wall.x0(),wall.x1()):Math.min(wall.z0(),wall.z1()),hi=wall.horizontal()?Math.max(wall.x0(),wall.x1()):Math.max(wall.z0(),wall.z1());if(along>lo-.02&&along<hi+.02&&y>wall.bottom()&&y<wall.top())return false;
  }
  for(var deck:CastleHubArchitecturalPlan.decks())if((ay-deck.height())*(by-deck.height())<0){double t=(deck.height()-ay)/(by-ay),x=ax+(bx-ax)*t,z=az+(bz-az)*t;if(x>deck.x0()&&x<deck.x1()&&z>deck.z0()&&z<deck.z1()&&!CastleHubArchitecturalPlan.onStairs(x,z))return false;}
  return true;
 }
 private void add(Kind k,int room,int fixture,double x,double y,double z,double dx,double dy,double dz,double radius,double power,double r,double g,double b){sources.add(new Source(sources.size(),k,room,fixture,x,y,z,dx,dy,dz,radius,power,r,g,b));}
 public List<Source> sources(){return List.copyOf(sources);}
 public void frame(RoomLighting circuits,int tick){
  String nextGeometry=map.doorSnapshot()+"|"+map.windowSnapshot()+"|"+map.hidingDensity();if(!geometry.equals(nextGeometry)){geometry=nextGeometry;sight.clear();colours.clear();contributions.clear();}
  StringBuilder stamp=new StringBuilder();for(var source:sources){double f;
   if(source.room==14)f=1;
   else if(source.kind==Kind.CANDLE_CHANDELIER)f=.94+.06*Math.sin(tick*.37+source.id*2.1);
   else if(source.kind==Kind.TABLE_LAMP)f=map.fixtures().state(source.fixture).active()&&(circuits==null||circuits.breaker())?1:0;
   else if(source.kind==Kind.TELEVISION)f=map.fixtures().state(source.fixture).active()&&(circuits==null||circuits.breaker())?(.94+.06*Math.sin(tick*1.7)):0;
   else f=circuits==null?1:Math.max(0,Math.min(1,(circuits.brightness(source.room,tick)-.085)/.865));
   if(source.fixture>0&&source.kind!=Kind.TELEVISION&&source.kind!=Kind.TABLE_LAMP&&!map.fixtures().state(source.fixture).active())f=0;
   factors[source.id]=f;stamp.append(Math.round(f*1000)).append(',');
  }
  if(!state.contentEquals(stamp)){state=stamp.toString();colours.clear();}
  if(contributions.size()>200000){contributions.clear();sight.clear();}if(colours.size()>150000)colours.clear();
 }
 private static double smooth(double lo,double hi,double value){double t=Math.max(0,Math.min(1,(value-lo)/(hi-lo)));return t*t*(3-2*t);}
 /** Source-to-surface cone, not a screen-space flashlight-shaped tint. */
 public static double angular(Kind kind,double dot){return switch(kind){
  case FLUORESCENT->smooth(.48,.72,dot);case TELEVISION->Math.pow(Math.max(0,dot),2.2);
  case TABLE_LAMP->.07+.86*smooth(.32,.76,dot)+.24*smooth(.70,.96,-dot);
  case FROSTED_DOME->.18+.82*smooth(-.08,.42,dot);default->1;};}
 public RGB sample(int room,double x,double y,double z){
  Key key=new Key(room,Math.round(x*1024),Math.round(y*1024),Math.round(z*1024));RGB cached=colours.get(key);if(cached!=null)return cached;
  double r=(room<0||room>=12)?.31:.055,g=(room<0||room>=12)?.34:.059,b=(room<0||room>=12)?.41:.073;
  List<Contribution> parts=contributions.get(key);
  if(parts==null){var found=new ArrayList<Contribution>();
   for(var source:sources){
    double dx=x-source.x,dy=y-source.y,dz=z-source.z,d2=dx*dx+dy*dy+dz*dz;if(d2>source.radius*source.radius)continue;double d=Math.sqrt(d2);
    double angle=d<.03?1:angular(source.kind,(dx*source.dx+dy*source.dy+dz*source.dz)/d);if(angle<.001)continue;
    double fade=1-d/source.radius,weight=source.power*angle*fade*fade/(1+.13*d2);if(weight<.002)continue;
    Shadow shadow=new Shadow(source.id,room,key.x,key.y,key.z);Boolean visible=sight.get(shadow);
    if(visible==null){visible=map==null?clear(source.x,source.y,source.z,x,y,z):ArchitecturalLightOcclusion.visible(map,room,source.x,source.y,source.z,x,y,z);sight.put(shadow,visible);}
    if(visible)found.add(new Contribution(source.id,weight));
   }parts=List.copyOf(found);contributions.put(key,parts);
  }
  for(var part:parts){double weight=part.weight*factors[part.source];if(weight<.002)continue;var source=sources.get(part.source);r+=weight*source.red;g+=weight*source.green;b+=weight*source.blue;}
  RGB result=new RGB(Math.min(1.6,r),Math.min(1.6,g),Math.min(1.6,b));colours.put(key,result);return result;
 }
 public RGB emission(int material,double x,double y,double z){
  if(!luminousMaterials.computeIfAbsent(material,id->{String name=GlbProps.materialName(id).toLowerCase(Locale.ROOT);return name.contains("glass")||name.contains("flame")||name.startsWith("television");}))return RGB.ZERO;
  for(var s:sources)if(factors[s.id]>.01&&Math.abs(x-s.x)<.4&&Math.abs(y-s.y)<.28&&Math.abs(z-s.z)<.4)return new RGB(s.red*1.35,s.green*1.35,s.blue*1.35);
  return RGB.ZERO;
 }
}
