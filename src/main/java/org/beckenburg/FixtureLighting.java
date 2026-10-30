package org.beckenburg;
import java.util.*;
import dev.mrcalzon.lighting.LightingModule;
/** Metre-space coloured emission and architectural occlusion, independent of the camera. */
public final class FixtureLighting {
 public enum Kind { FROSTED_DOME,BARE_BULB,CANDLE_CHANDELIER,TABLE_LAMP,FLUORESCENT,VAN_DIFFUSER,WALL_SCONCE,TELEVISION,FIRELIGHT }
 public record RGB(double r,double g,double b){static final RGB ZERO=new RGB(0,0,0);public double energy(){return r+g+b;}}
 public record Source(int id,Kind kind,int room,int fixture,double x,double y,double z,double dx,double dy,double dz,double radius,double power,double red,double green,double blue){}
 private record Key(int room,long x,long y,long z){}
 // Exact geometry and orientation bits avoid reusing a normal probe across
 // opposite sides of a thin wall or across differently facing surfaces.
 private record NormalKey(int room,long x,long y,long z,long nx,long ny,long nz){}
 private record Shadow(int source,int receiver,long x,long y,long z){}
 private record DistantCell(int room,long x,long y,long z){}

 private record DistantProbe(double x,double y,double z,RGB light,long revision){}
 private record Cell(int x,int y,int z){}
 private record Lit(RGB rgb,long revision){}
 private final LightingModule.Settings options=LightingModule.current();
 private final double cellSize=options.cellMetres();
 private final Map<Cell,List<Source>> influence=new HashMap<>();
 private final Map<Cell,Long> changedCells=new HashMap<>();
 private final Map<Cell,Long> occluderCells=new HashMap<>();
 private final Map<LevelDefinition.Portal,Boolean> portalStates=new HashMap<>();
 private boolean portalStateInitialized;
 private int lastHidingDensity=-1;
 private long generation,invalidatedCells,consideredSources;
 private static <K,V> Map<K,V> bounded(int limit){
  return new LinkedHashMap<K,V>(2048,.75f,true){
   @Override protected boolean removeEldestEntry(Map.Entry<K,V> eldest){
    return size()>limit;
   }
  };
 }
 private Cell cell(double x,double y,double z){
  return new Cell((int)Math.floor(x/cellSize),
                  (int)Math.floor(y/cellSize),(int)Math.floor(z/cellSize));
 }
 private long revision(double x,double y,double z){
  return changedCells.getOrDefault(cell(x,y,z),0L);
 }
 /** Sphere-of-influence preselection only. Actual wall raycasts stay mandatory. */
 private void eachInfluenceCell(Source source,java.util.function.Consumer<Cell> use){
  int xa=(int)Math.floor((source.x-source.radius)/cellSize);
  int xb=(int)Math.floor((source.x+source.radius)/cellSize);
  int ya=(int)Math.floor((source.y-source.radius)/cellSize);
  int yb=(int)Math.floor((source.y+source.radius)/cellSize);
  int za=(int)Math.floor((source.z-source.radius)/cellSize);
  int zb=(int)Math.floor((source.z+source.radius)/cellSize);
  for(int xx=xa;xx<=xb;xx++)for(int yy=ya;yy<=yb;yy++)
   for(int zz=za;zz<=zb;zz++)use.accept(new Cell(xx,yy,zz));
 }
 private void invalidate(Source source){
  long version=++generation;
  eachInfluenceCell(source,key->{changedCells.put(key,version);invalidatedCells++;});
 }
 private List<Source> candidates(double x,double y,double z){
  return influence.getOrDefault(cell(x,y,z),List.of());
 }
 private final Map<DistantCell,DistantProbe> distant=bounded(options.cacheLimit(15000));
 private long lodQueries,lodHits,lodVisibilityChecks;
 private final HouseMap map;private final CastleHubMap castle;
 private final List<Source> sources=new ArrayList<>();
 private final Map<Key,Lit> colours=bounded(options.cacheLimit(150000));
 private final Map<NormalKey,Lit> normalColours=bounded(options.cacheLimit(100000));
 private final Map<Object,Boolean> sight=bounded(options.cacheLimit(200000));
 /** Scene-owned immutable receiver visibility survives cosmetic intensity
  * changes. Door/topology revisions invalidate it independently of brightness. */
 public static final class StationaryShadows {
  private final FixtureLighting owner;
  private final Map<Object,Boolean> values=new HashMap<>();
  private long epoch=-1;
  private StationaryShadows(FixtureLighting owner){this.owner=owner;}
  public void release(){owner.stationaryShadowEntries-=values.size();values.clear();epoch=-1;}
 }

 private long stationaryShadowEntries;
 private StationaryShadows activeShadows;
 public StationaryShadows newStationaryShadows(){return new StationaryShadows(this);}
 public StationaryShadows useStationaryShadows(StationaryShadows next){
  return useStationaryShadows(next,occluderEpoch);
 }
 /** A retained chunk owns its own occluder revision; a distant door cannot
  * discard unrelated baked shadow probes. */
 public StationaryShadows useStationaryShadows(StationaryShadows next,long regionEpoch){
  var previous=activeShadows;
  if(next!=null){
   if(next.owner!=this)throw new IllegalArgumentException("Shadows belong to a different lighting scene");
   if(next.epoch!=regionEpoch){
    stationaryShadowEntries-=next.values.size();next.values.clear();next.epoch=regionEpoch;
   }
  }
  activeShadows=next;return previous;
 }
 /** Restoring the caller's active cache is not a new occlusion pass. */
 void restoreStationaryShadows(StationaryShadows previous){activeShadows=previous;}
 private Boolean cachedShadow(Object key){
  Boolean found=activeShadows==null?null:activeShadows.values.get(key);
  return found==null?sight.get(key):found;
 }
 private void rememberShadow(Object key,boolean visible){
  if(activeShadows!=null&&stationaryShadowEntries<options.cacheLimit(1_000_000)){
   if(activeShadows.values.put(key,visible)==null)stationaryShadowEntries++;
  }else sight.put(key,visible);
 }
 private record NormalShadow(int source,NormalKey receiver){}
 private String geometry="",state="";private double[] factors=new double[0];
 private long occluderEpoch;
 private final BitSet knownEmissionMaterials=new BitSet(),emissionMaterials=new BitSet();
 /** Authoritative revision of the lighting cells touched by an immutable
  * geometry range. Unrelated room-light changes retain prepared GPU colours;
  * changed doors/windows always invalidate their previous visibility. */
 private long regionRevision(Map<Cell,Long> updates,
                             double ax,double ay,double az,double bx,double by,double bz){
  int x0=(int)Math.floor(ax/cellSize),x1=(int)Math.floor(bx/cellSize);
  int y0=(int)Math.floor(ay/cellSize),y1=(int)Math.floor(by/cellSize);
  int z0=(int)Math.floor(az/cellSize),z1=(int)Math.floor(bz/cellSize);
  long newest=0;
  long volume=(long)(x1-x0+1)*(y1-y0+1)*(z1-z0+1);
  if(volume>updates.size()){
   for(var entry:updates.entrySet()){
    var c=entry.getKey();
    if(c.x>=x0&&c.x<=x1&&c.y>=y0&&c.y<=y1&&c.z>=z0&&c.z<=z1)
     newest=Math.max(newest,entry.getValue());
   }
  }else for(int xx=x0;xx<=x1;xx++)for(int yy=y0;yy<=y1;yy++)
   for(int zz=z0;zz<=z1;zz++)
    newest=Math.max(newest,updates.getOrDefault(new Cell(xx,yy,zz),0L));
  return Math.max(newest,occluderEpoch);
 }
 public long regionVersion(double ax,double ay,double az,double bx,double by,double bz){
  return regionRevision(changedCells,ax,ay,az,bx,by,bz);
 }
 /** Only receiver chunks touched by a changed door's contributing lights
  * require new visibility rays and rebuilt resident vertices. */
 public long regionOccluderVersion(double ax,double ay,double az,double bx,double by,double bz){
  return regionRevision(occluderCells,ax,ay,az,bx,by,bz);
 }
 /** Fast unchanged-frame gate before consulting individual spatial regions. */
 public long frameRevision(){return generation;}
 public long occluderRevision(){return occluderEpoch;}
 private record Weighted(Source source,double weight){}
 private static final Weighted[] NO_WEIGHTS=new Weighted[0];
 private static final Source[] NO_EMISSION=new Source[0];
 private record Probe(RGB ambient,Weighted[] contributions,boolean normal){
  RGB sample(double[] factors){
   double r=ambient.r,g=ambient.g,b=ambient.b;
   for(var part:contributions){
    double f=factors[part.source.id],w=part.weight*f;
    if(f<.001||w<(normal?.001:.002))continue;
    r+=w*part.source.red;g+=w*part.source.green;b+=w*part.source.blue;
   }
   return new RGB(Math.min(1.6,r),Math.min(1.6,g),Math.min(1.6,b));
  }
 }
 /** Immutable visibility and attenuation, with live source factors. Cosmetic
  * flicker and real switches require only RGB evaluation, never new rays or
  * tessellation. The owning chunk rebuilds this on an occluder revision. */
 public final class VertexProbe {
  private final Probe base,normal;
  private final Source[] emissive;
  private VertexProbe(Probe base,Probe normal,Source[] emissive){this.base=base;this.normal=normal;this.emissive=emissive;}
  private FixtureLighting owner(){return FixtureLighting.this;}
  public RGB sample(){
   RGB rgb=base.sample(factors),glow=RGB.ZERO;
   for(var source:emissive)if(factors[source.id]>.01){glow=new RGB(source.red*1.35,source.green*1.35,source.blue*1.35);break;}
   double r=Math.max(rgb.r,glow.r),g=Math.max(rgb.g,glow.g),b=Math.max(rgb.b,glow.b);
   if(normal!=null){var n=normal.sample(factors);r=Math.max(0,r+n.r-rgb.r);g=Math.max(0,g+n.g-rgb.g);b=Math.max(0,b+n.b-rgb.b);}
   return new RGB(r,g,b);
  }
  /** Same evaluation order/clamps as sample(), writing only the RGB slots
   * of an eight-float world vertex. No temporary RGBs or iterators. */
  public void writeTinted(float[] target,int offset,int tint){
   writeTinted(target,null,offset,tint);
  }
  public boolean constant(){return base.contributions.length==0
      &&(normal==null||normal.contributions.length==0)&&emissive.length==0;}
  /** Preserve published arrays; clone only on the first actual color change. */
  public float[] copyTintedIfChanged(float[] original,float[] target,int offset,int tint){
   if(constant())return target;
   return writeTinted(target,original,offset,tint);
  }
  private float[] store(float[] target,float[] original,int offset,int tint,double r,double g,double b){
   float rr=(float)(Math.max(.03,r)*((tint>>16)&255)/255.0);
   float gg=(float)(Math.max(.03,g)*((tint>>8)&255)/255.0);
   float bb=(float)(Math.max(.03,b)*(tint&255)/255.0);
   if(original!=null){
    if(Float.floatToRawIntBits(target[offset+5])==Float.floatToRawIntBits(rr)
       &&Float.floatToRawIntBits(target[offset+6])==Float.floatToRawIntBits(gg)
       &&Float.floatToRawIntBits(target[offset+7])==Float.floatToRawIntBits(bb))return target;
    if(target==original)target=original.clone();
   }
   target[offset+5]=rr;target[offset+6]=gg;target[offset+7]=bb;
   return target;
  }
  private float[] writeTinted(float[] target,float[] original,int offset,int tint){
   if(!directVertexWrite){
    var rgb=sample();
    return store(target,original,offset,tint,rgb.r,rgb.g,rgb.b);
   }
   double br=base.ambient.r,bg=base.ambient.g,bb=base.ambient.b;
   for(var part:base.contributions){
    double f=factors[part.source.id],w=part.weight*f;
    if(f<.001||w<(base.normal?.001:.002))continue;
    br+=w*part.source.red;bg+=w*part.source.green;bb+=w*part.source.blue;
   }
   br=Math.min(1.6,br);bg=Math.min(1.6,bg);bb=Math.min(1.6,bb);
   double r=br,g=bg,b=bb;
   for(var source:emissive)if(factors[source.id]>.01){
    r=Math.max(r,source.red*1.35);g=Math.max(g,source.green*1.35);b=Math.max(b,source.blue*1.35);break;
   }
   if(normal!=null){
    double nr=normal.ambient.r,ng=normal.ambient.g,nb=normal.ambient.b;
    for(var part:normal.contributions){
     double f=factors[part.source.id],w=part.weight*f;
     if(f<.001||w<(normal.normal?.001:.002))continue;
     nr+=w*part.source.red;ng+=w*part.source.green;nb+=w*part.source.blue;
    }
    r=Math.max(0,r+Math.min(1.6,nr)-br);g=Math.max(0,g+Math.min(1.6,ng)-bg);b=Math.max(0,b+Math.min(1.6,nb)-bb);
   }
   return store(target,original,offset,tint,r,g,b);
  }
 }
 /** Exact factor snapshot for the sources that can affect one prepared batch.
  * Visibility/topology invalidation remains the owning chunk's responsibility.
  * Raw bits preserve even changes smaller than the spatial invalidation threshold. */
 final class ProbeDependencies {
  private final int[] ids;
  private final long[] bits;
  private ProbeDependencies(BitSet sources){ids=sources.stream().toArray();bits=new long[ids.length];capture();}
  boolean matchesCurrent(){
   for(int i=0;i<ids.length;i++)if(bits[i]!=Double.doubleToRawLongBits(factors[ids[i]]))return false;
   return true;
  }
  void capture(){for(int i=0;i<ids.length;i++)bits[i]=Double.doubleToRawLongBits(factors[ids[i]]);}
 }
 ProbeDependencies dependencies(List<VertexProbe> probes){
  BitSet ids=new BitSet();
  for(var probe:probes){
   if(probe.owner()!=this)throw new IllegalArgumentException("Probes belong to a different lighting scene");
   for(var part:probe.base.contributions)ids.set(part.source.id);
   if(probe.normal!=null)for(var part:probe.normal.contributions)ids.set(part.source.id);
   for(var source:probe.emissive)ids.set(source.id);
  }
  return new ProbeDependencies(ids);
 }
 private final boolean directVertexWrite=!Boolean.getBoolean("beckenburg.lighting.directWrite.disabled");
 private Probe prepareProbe(int room,double x,double y,double z,double nx,double ny,double nz,boolean normal){
  boolean exterior=room<0||room>=12||map!=null&&map.outside(room);
  double hemi=.65+.35*Math.max(0,ny);
  double ambientScale=options.ambientScale();
  RGB ambient=normal?new RGB(.27*hemi*ambientScale,.29*hemi*ambientScale,.34*hemi*ambientScale):
      new RGB((exterior?.27:castle!=null?.125:.055)*ambientScale,
              (exterior?.29:castle!=null?.129:.059)*ambientScale,
              (exterior?.34:castle!=null?.14:.073)*ambientScale);
  var parts=new ArrayList<Weighted>();
  for(var source:candidates(x,y,z)){
   double dx=x-source.x,dy=y-source.y,dz=z-source.z,d2=dx*dx+dy*dy+dz*dz;
   if(d2>source.radius*source.radius||normal&&d2<1e-8)continue;
   double d=Math.sqrt(d2),angle=!normal&&d<.03?1:angular(source.kind,(dx*source.dx+dy*source.dy+dz*source.dz)/d);
   double diffuse=normal?Math.max(0,(-nx*dx-ny*dy-nz*dz)/Math.max(.001,d)):1;
   double fade=1-d/source.radius,w=source.power*angle*fade*(.45+.55*fade)/(1+.13*d2)*diffuse;
   if(w<(normal?.001:.002)||angle<.001||diffuse<.001)continue;
   Object key=normal?new NormalShadow(source.id,new NormalKey(room,Double.doubleToLongBits(x),Double.doubleToLongBits(y),Double.doubleToLongBits(z),Double.doubleToLongBits(nx),Double.doubleToLongBits(ny),Double.doubleToLongBits(nz))):
       new Shadow(source.id,room,Math.round(x*16),Math.round(y*16),Math.round(z*16));
   Boolean visible=cachedShadow(key);
   if(visible==null){
    double back=Math.min(.12,d*.20)/Math.max(.001,d);
    double rx=normal?x+nx*.025:x-dx*back,ry=normal?y+ny*.025:y-dy*back,rz=normal?z+nz*.025:z-dz*back;
    if(normal)normalRays++;else shadowRays++;
    visible=map!=null&&!map.nativeSite()?ArchitecturalLightOcclusion.visible(map,room,source.x,source.y,source.z,normal?rx:x,normal?ry:y,normal?rz:z):clear(source.x,source.y,source.z,rx,ry,rz);
    rememberShadow(key,visible);
   }
   if(visible)parts.add(new Weighted(source,w));
  }
  return new Probe(ambient,parts.isEmpty()?NO_WEIGHTS:parts.toArray(Weighted[]::new),normal);
 }
 public VertexProbe prepareVertex(int room,int material,double x,double y,double z,double nx,double ny,double nz,boolean normal){
  Probe base=prepareProbe(room,x,y,z,0,0,0,false);
  Probe directional=normal?prepareProbe(room,x,y,z,nx,ny,nz,true):null;
  var emit=new ArrayList<Source>();
  if(emittingMaterial(material))for(var s:candidates(x,y,z))
   if(Math.abs(x-s.x)<.4&&Math.abs(y-s.y)<.28&&Math.abs(z-s.z)<.4)emit.add(s);
  return new VertexProbe(base,directional,emit.isEmpty()?NO_EMISSION:emit.toArray(Source[]::new));
 }
 /** Per-render CPU work counters; shadow tests are expensive architectural rays. */
 public record FrameStats(long samples,long cacheHits,long shadowRays,
                          long normalSamples,long normalRays,int lights,int cachedColours,
                          long lodQueries,long lodHits,long lodVisibilityChecks,
                          long consideredSources,long invalidatedCells,long normalCacheHits){}
 private long samples,cacheHits,shadowRays,normalSamples,normalRays,normalCacheHits;
 public FrameStats frameStats(){return new FrameStats(samples,cacheHits,shadowRays,
     normalSamples,normalRays,sources.size(),colours.size(),
     lodQueries,lodHits,lodVisibilityChecks,consideredSources,invalidatedCells,normalCacheHits);}
 private void resetStatistics(){samples=cacheHits=shadowRays=normalSamples=normalRays=normalCacheHits=0;
  lodQueries=lodHits=lodVisibilityChecks=0;
  consideredSources=invalidatedCells=0;}
 private long castleDoorRevision=-1;
 public FixtureLighting(HouseMap map){this.map=map;this.castle=null;
  for(var s:map.level().sockets())if(s.type().equals("light")){
   int control=controller(s);
   if(s.style().equals("native-string"))add(Kind.FROSTED_DOME,s.room(),control,s.x(),s.y(),s.z(),0,-1,0,6,1.45,1,.76,.42);
   else if(s.style().equals("bare-bulb"))add(Kind.BARE_BULB,s.room(),0,s.x(),s.y()-.565,s.z(),0,-1,0,8,4.5,1,.86,.65);
   else if(s.style().equals("candle-chandelier")){for(int i=0;i<6;i++){double a=i*Math.PI/3;add(Kind.CANDLE_CHANDELIER,s.room(),0,s.x()+.4*Math.cos(a),s.y()-.515,s.z()+.4*Math.sin(a),0,-1,0,5.8,.55,1,.66,.31);}}
   else if(s.style().contains("utility")||s.style().contains("industrial")){for(double dx:new double[]{-.34,.34})add(Kind.FLUORESCENT,s.room(),control,s.x()+dx,s.y()-.145,s.z(),0,-1,0,9,2.8,.84,.94,1);}
   else if(s.style().contains("sconce")||s.style().contains("porch")){double yaw=-s.yaw();add(Kind.WALL_SCONCE,s.room(),0,s.x()-Math.sin(yaw)*.24,s.y()+.16,s.z()+Math.cos(yaw)*.24,0,-1,0,5,2.4,1,.78,.48);}
   else add(Kind.FROSTED_DOME,s.room(),control,s.x(),s.y()-.16,s.z(),0,-1,0,7,3.4,1,.88,.72);
  }
  for(var f:map.fixtures().states())if(f.kind()==HauntedFixtures.Kind.TABLE_LAMP)add(Kind.TABLE_LAMP,f.room(),f.id(),f.x(),f.y()+.345,f.z(),0,-1,0,3.6,2.0,1,.72,.40);
  for(var f:map.fixtures().states())if(f.kind().name().equals("TELEVISION")){double front=.345*f.scale();add(Kind.TELEVISION,f.room(),f.id(),f.x()-Math.sin(f.yaw())*front,f.y()+.32*f.scale(),f.z()+Math.cos(f.yaw())*front,-Math.sin(f.yaw()),0,Math.cos(f.yaw()),4.4,3.4,.91,.96,1);}
  for(var light:VanInteriorLayout.CEILING_LIGHTS)for(double xx:new double[]{-.18,.18}){
   var pos=map.van().world(xx,light.z());
   add(Kind.VAN_DIFFUSER,14,0,pos.x(),map.van().placement().y()+light.mountY()-.065,pos.z(),0,-1,0,3.8,.95,.92,.97,1);
  }
  factors=new double[sources.size()];
 }
 private int controller(LevelDefinition.Socket socket){int at=socket.style().indexOf('@');if(at<0)return 0;String key=socket.style().substring(at+1);var plate=map.level().sockets().stream().filter(s->s.id().equals(key)).findFirst().orElseThrow();return map.fixtures().states().stream().filter(f->f.kind()==HauntedFixtures.Kind.LIGHT_SWITCH&&Math.hypot(f.x()-plate.x(),f.z()-plate.z())<.001).findFirst().orElseThrow().id();}
 public FixtureLighting(CastleHubMap castle){map=null;this.castle=Objects.requireNonNull(castle);
  for(var edited:CastleLevelEdits.current().lamps()){var light=edited.fixture();
   var bounds=VanInteriorLayout.bounds(light.on());
   double y=light.y()-(light.mount()==CastleHubDecor.Mount.CEILING
       ?(bounds.y1()-bounds.y0())*light.scale()*.55:0);
   Kind kind=light.on().contains("ceiling_lamp_4")?Kind.CANDLE_CHANDELIER
       :light.mount()==CastleHubDecor.Mount.WALL?Kind.WALL_SCONCE:Kind.FROSTED_DOME;
   // Use the wall-fitted position for the emitter as well as the GLB.
   // Emission inside the 42 cm stone otherwise shadows its own room.
   var mount=CastleWallLampMount.resolve(light,CastleLevelEdits.current().walls());
   add(kind,0,0,mount.x(),y,mount.z(),0,-1,0,edited.radius(),edited.power(),
       1,kind==Kind.CANDLE_CHANDELIER?.69:.83,kind==Kind.CANDLE_CHANDELIER?.34:.57);
  }factors=new double[sources.size()];
 }
 public void castleFrame(boolean on,int tick){
  resetStatistics();
  // Dynamic gate leaves change real shadow occlusion even when the clock and
  // circuits have not advanced. Retain expensive geometry caches otherwise.
  if(castle!=null&&castle.doorRevision()!=castleDoorRevision){
   castleDoorRevision=castle.doorRevision();occluderEpoch=++generation;
   sight.clear();colours.clear();normalColours.clear();distant.clear();
  }
  int cosmeticTick=(tick/6)*6;
  for(var source:sources){
   double next=!on||!castle.lightOn(source.x,source.y,source.z)?0:source.kind==Kind.CANDLE_CHANDELIER
       ?(1-.06*options.flickerScale()*(1-Math.sin(cosmeticTick*.3+source.id))):1;
   if(Math.round(next*1000)!=Math.round(factors[source.id]*1000))invalidate(source);
   factors[source.id]=next;
  }
 }
 /** Unified shadow decision for all live light emitters. Investigations use
  * HouseMap's existing 3D doors, colliders and multi-storey visibility.
  * Castle uses its authored solid wall segments, decks and room ceilings.
  * The caller never falls back to unoccluded brightness. */
 boolean clear(double ax,double ay,double az,double bx,double by,double bz){
  if(map!=null)return map.clearLineOfSight(ax,ay,az,bx,by,bz);
  return castleRayClear(castle,ax,ay,az,bx,by,bz);
 }
 static boolean castleRayClear(CastleHubMap map,double ax,double ay,double az,double bx,double by,double bz){
  if(map==null||!Double.isFinite(ax+ay+az+bx+by+bz))return false;
  final double EPS=0.0001;
  for(var slot:CastleDoorSlots.all()){
   var type=CastleLevelEdits.current().doorType(slot.id());
   if(type==CastleDoorSlots.Type.WOOD){if(CastleTimberDoors.crosses(slot,map.doorOpen(slot.id()),ax,ay,az,bx,by,bz))return false;}
   else if(type!=CastleDoorSlots.Type.EMPTY&&!map.doorOpen(slot.id())&&CastleDoorSlots.crossesLeaf(slot,ax,ay,az,bx,by,bz))return false;
  }
  for(var wall:CastleLevelEdits.current().walls()){
   if(wall.hasDoor())continue; // markers denote OPENINGS, not solid faces
   if(CastleDoorSlots.crossesWall(wall,ax,ay,az,bx,by,bz))return false;
   double da=wall.horizontal()?az-wall.z0():ax-wall.x0();
   double db=wall.horizontal()?bz-wall.z0():bx-wall.x0();
   if(Math.abs(db-da)<EPS)continue;
   double t=-da/(db-da);
   if(t<=EPS||t>=1-EPS)continue;
   double y=ay+(by-ay)*t;
   double along=wall.horizontal()?ax+(bx-ax)*t:az+(bz-az)*t;
   double lo=wall.horizontal()?Math.min(wall.x0(),wall.x1()):Math.min(wall.z0(),wall.z1());
   double hi=wall.horizontal()?Math.max(wall.x0(),wall.x1()):Math.max(wall.z0(),wall.z1());
   if(along>lo-EPS&&along<hi+EPS&&y>wall.bottom()+EPS&&y<wall.top()-EPS)
    return false;
  }
  // The same structural slabs supply visible tops, soffits, edges and occlusion.
  for(var slab:CastleRooms.structuralDecks())
   if(CastleDoorSlots.crossesBox(ax,ay,az,bx,by,bz,slab.x0(),slab.height()-CastleRooms.SLAB,slab.z0(),
       slab.x1(),slab.height(),slab.z1()))return false;
  // Sloping roof faces cast shadows too. Trace the signed elevation
  // against each left/right planar roof face, not a fake flat ceiling.
  for(var roof:CastleLevelEdits.current().roofs()){
   double dx=bx-ax,dy=by-ay,dz=bz-az;
   double middle=(roof.west()+roof.east())*.5;
   for(int side=0;side<2;side++){
    double slope=(side==0?roof.rise():-roof.rise())/
        ((roof.east()-roof.west())*.5);
    double lower=(side==0?roof.west():middle);
    double roofAtLower=roof.eave()+(side==0?0:roof.rise());
    double denom=dy-slope*dx;
    if(Math.abs(denom)<EPS)continue;
    double t=(roofAtLower+slope*(ax-lower)-ay)/denom;
    if(t<=EPS||t>=1-EPS)continue;
    double x=ax+dx*t,z=az+dz*t;
    if(x>=lower+EPS&&x<=(side==0?middle:roof.east())-EPS
       &&z>=roof.north()+EPS&&z<=roof.south()-EPS)return false;
   }
  }
  // Downstairs ceilings and roofs are also opaque to light: the old path
  // missed these planes and illuminated downstairs surfaces from above.
  for(double ceiling:new double[]{CastleHubArchitecturalPlan.GROUND_CEILING-CastleRooms.SLAB,
                                  CastleHubArchitecturalPlan.GROUND_CEILING,
                                  CastleHubArchitecturalPlan.UPPER_CEILING}){
   if((ay-ceiling)*(by-ceiling)>=0||Math.abs(by-ay)<EPS)continue;
   double t=(ceiling-ay)/(by-ay);
   if(t<=EPS||t>=1-EPS)continue;
   double x=ax+(bx-ax)*t,z=az+(bz-az)*t;
   if(x<=0||x>=CastleHubMap.WIDTH||z<=0||z>=CastleHubMap.HEIGHT
      ||map.solid((int)Math.floor(x),(int)Math.floor(z))
      ||map.outside(x,z)||CastleHubArchitecturalPlan.onStairs(x,z))continue;
   if(ceiling==CastleHubArchitecturalPlan.UPPER_CEILING){
    if(CastleHubArchitecturalPlan.upperWalkable(x,z))return false;
   }else if(Math.abs(CastleHubArchitecturalPlan.groundCeilingHeight(x,z)-ceiling)<EPS)
    return false;
  }
  return true;
 }
 private void add(Kind k,int room,int fixture,double x,double y,double z,double dx,double dy,double dz,double radius,double power,double r,double g,double b){
  var source=new Source(sources.size(),k,room,fixture,x,y,z,dx,dy,dz,
      radius*options.reachScale(),power*options.sourceScale(),r,g,b);
  sources.add(source);
  eachInfluenceCell(source,key->influence.computeIfAbsent(key,ignored->new ArrayList<>()).add(source));
 }
 public List<Source> sources(){return List.copyOf(sources);}
 /** Opening a single portal changes only lights whose radius intersects it.
  * Mark their entire influence cells: rays can reach receivers far from the
  * actual door, so testing just the door cell would leak light through walls. */
 private void invalidateOccludingPortal(LevelDefinition.Portal portal){
  long revision=++generation;
  double half=portal.width()/2+.35;
  for(var source:sources){
   double dx=Math.max(0,Math.abs(source.x-portal.x())-(portal.alongX()?half:.35));
   double dz=Math.max(0,Math.abs(source.z-portal.z())-(portal.alongX()?.35:half));
   double dy=Math.max(0,Math.abs(source.y-(portal.y()+1.15))-1.15);
   if(dx*dx+dy*dy+dz*dz>source.radius*source.radius)continue;
   eachInfluenceCell(source,key->{
    changedCells.put(key,revision);occluderCells.put(key,revision);invalidatedCells++;
   });
  }
 }
 public void frame(RoomLighting circuits,int tick){
  resetStatistics();
  String nextGeometry=map.doorSnapshot()+":"+map.windowSnapshot()+":"+map.hidingDensity();
  if(!geometry.equals(nextGeometry)){
   boolean full=!portalStateInitialized||lastHidingDensity!=map.hidingDensity();
   int changedPortals=0;
   for(var portal:map.level().portals()){
    if(!portal.door()&&!portal.kind().equals("window"))continue;
    boolean open=portal.door()?map.doorOpen(portal.id()):map.windowOpen(portal.id());
    Boolean old=portalStates.put(portal,open);
    if(old==null&&portalStateInitialized)full=true;
    else if(old!=null&&old.booleanValue()!=open){
     invalidateOccludingPortal(portal);changedPortals++;
    }
   }
   // Unknown architectural changes (including hiding layouts) remain global.
   if(changedPortals==0)full=true;
   if(full){
    occluderEpoch=++generation;
    sight.clear();colours.clear();normalColours.clear();distant.clear();
   }else{
    // Shared short-lived ray results cannot know which doorway blocked them.
    // Cached colours and per-chunk stationary shadows DO know their regions.
    sight.clear();
   }
   lastHidingDensity=map.hidingDensity();portalStateInitialized=true;geometry=nextGeometry;
  }
  int cosmeticTick=(tick/6)*6;
  for(var source:sources){double f;
   if(source.room==14)f=1;
   else if(source.kind==Kind.CANDLE_CHANDELIER)f=1-.06*options.flickerScale()*(1-Math.sin(cosmeticTick*.37+source.id*2.1));
   else if(source.kind==Kind.TABLE_LAMP)f=map.fixtures().state(source.fixture).active()&&(circuits==null||circuits.supplied())?1:0;
   else if(source.kind==Kind.TELEVISION)f=map.fixtures().state(source.fixture).active()&&(circuits==null||circuits.supplied())?(1-.06*options.flickerScale()*(1-Math.sin(cosmeticTick*1.7))):0;
   else f=circuits==null?1:Math.max(0,Math.min(1,(circuits.brightness(source.room,tick)-.085)/.865));
   if(source.fixture>0&&source.kind!=Kind.TELEVISION&&source.kind!=Kind.TABLE_LAMP&&!map.fixtures().state(source.fixture).active())f=0;
   // True circuit, fixture and blackout changes are immediate. Subtle TV
   // and candle intensity variation updates every six simulation ticks.
   if(Math.round(f*1000)!=Math.round(factors[source.id]*1000))invalidate(source);
   factors[source.id]=f;
  }
  // Keep previous lighting outside changed light volumes; bounded LRU
  // eviction is incremental instead of emptying every cache on one frame.
 }
 private static double smooth(double lo,double hi,double value){double t=Math.max(0,Math.min(1,(value-lo)/(hi-lo)));return t*t*(3-2*t);}
 /** Source-to-surface cone, not a screen-space flashlight-shaped tint. */
 public static double angular(Kind kind,double dot){return switch(kind){
  case FLUORESCENT->smooth(.48,.72,dot);case VAN_DIFFUSER->.35+.65*smooth(-.12,.45,dot);case TELEVISION->Math.pow(Math.max(0,dot),2.2);
  case TABLE_LAMP->.07+.86*smooth(.32,.76,dot)+.24*smooth(.70,.96,-dot);
  case FROSTED_DOME->.18+.82*smooth(-.08,.42,dot);default->1;};}
 /**
  * Far-away EXTERIOR probe LOD only. Near/on-building surfaces preserve the
  * full 1/16m cached shadow tests. A coarse cell is reused only if the
  * authoritative 3D collision/visibility query confirms the SAME visible
  * side of geometry; this prevents lights crossing door and wall boundaries.
  * The one short probe visibility check replaces many source-to-point rays.
  */
 public RGB sampleDistanceLod(int room,double x,double y,double z,double viewDistance){
  if(!options.distantOutdoorLod()||map==null||room<0||!map.outside(room)||viewDistance<18
      ||!Double.isFinite(viewDistance))return sample(room,x,y,z);
  lodQueries++;
  var key=new DistantCell(room,Math.round(x*2),Math.round(y*2),Math.round(z*2));
  var cached=distant.get(key);
  if(cached!=null){
   lodVisibilityChecks++;
   if(cached.revision()==revision(x,y,z)
      &&map.clearLineOfSight(cached.x,cached.y,cached.z,x,y,z)){
    lodHits++;return cached.light;
   }
  }
  RGB result=sample(room,x,y,z);
  distant.put(key,new DistantProbe(x,y,z,result,revision(x,y,z)));
  return result;
 }
 public RGB sample(int room,double x,double y,double z){
  samples++;
  Key key=new Key(room,Math.round(x*16),Math.round(y*16),Math.round(z*16));
  long version=revision(x,y,z);
  Lit cached=colours.get(key);
  if(cached!=null&&cached.revision()==version){cacheHits++;return cached.rgb();}
  boolean exterior=room<0||room>=12||map!=null&&map.outside(room);
  double ambientScale=options.ambientScale();
  double r=(exterior?.27:(castle!=null?.125:.055))*ambientScale,
      g=(exterior?.29:(castle!=null?.129:.059))*ambientScale,
      b=(exterior?.34:(castle!=null?.14:.073))*ambientScale;
  for(var source:candidates(x,y,z)){consideredSources++;double factor=factors[source.id];if(factor<.001)continue;
   double dx=x-source.x,dy=y-source.y,dz=z-source.z,d2=dx*dx+dy*dy+dz*dz;if(d2>source.radius*source.radius)continue;double d=Math.sqrt(d2);
   double angle=d<.03?1:angular(source.kind,(dx*source.dx+dy*source.dy+dz*source.dz)/d);if(angle<.001)continue;
   double fade=1-d/source.radius,weight=source.power*factor*angle*fade*(.45+.55*fade)/(1+.13*d2);if(weight<.002)continue;
   Shadow shadow=new Shadow(source.id,room,key.x,key.y,key.z);Boolean visible=cachedShadow(shadow);
   if(visible==null){double back=Math.min(.12,d*.20)/Math.max(.001,d);shadowRays++;
    // Check architecture to the receiving face before applying prop self-shadow
    // bias. The bias must never pull an inner-wall endpoint across its wall.
    visible=map!=null&&!map.nativeSite()
        ?ArchitecturalLightOcclusion.visible(map,room,source.x,source.y,source.z,x,y,z)
        :clear(source.x,source.y,source.z,x-dx*back,y-dy*back,z-dz*back);
    rememberShadow(shadow,visible);}
   if(visible){r+=weight*source.red;g+=weight*source.green;b+=weight*source.blue;}
  }
  RGB result=new RGB(Math.min(1.6,r),Math.min(1.6,g),Math.min(1.6,b));
  colours.put(key,new Lit(result,version));return result;
 }
 /** Directional diffuse for audited source normals. Existing un-audited
  * assets retain the legacy sampler until their normal orientation is reviewed. */
 public RGB sampleNormal(int room,double x,double y,double z,double nx,double ny,double nz){
  normalSamples++;
  NormalKey key=new NormalKey(room,Double.doubleToLongBits(x),
      Double.doubleToLongBits(y),Double.doubleToLongBits(z),
      Double.doubleToLongBits(nx),Double.doubleToLongBits(ny),
      Double.doubleToLongBits(nz));
  long version=revision(x,y,z);
  Lit cached=normalColours.get(key);
  if(cached!=null&&cached.revision()==version){
   normalCacheHits++;return cached.rgb();
  }
  double hemi=.65+.35*Math.max(0,ny),ambientScale=options.ambientScale(),
      r=.27*hemi*ambientScale,g=.29*hemi*ambientScale,b=.34*hemi*ambientScale;
  for(var source:candidates(x,y,z)){consideredSources++;double factor=factors[source.id];if(factor<.001)continue;double dx=source.x-x,dy=source.y-y,dz=source.z-z,d2=dx*dx+dy*dy+dz*dz;if(d2>source.radius*source.radius||d2<1e-8)continue;double d=Math.sqrt(d2),diffuse=Math.max(0,(nx*dx+ny*dy+nz*dz)/d);if(diffuse<.001)continue;
   double angle=angular(source.kind,(-dx*source.dx-dy*source.dy-dz*source.dz)/d),fade=1-d/source.radius,weight=source.power*factor*angle*fade*(.45+.55*fade)/(1+.13*d2)*diffuse;
   if(weight>.001){
    var shadow=new NormalShadow(source.id,key);Boolean visible=cachedShadow(shadow);
    if(visible==null){normalRays++;
    visible=map!=null&&!map.nativeSite()
        ?ArchitecturalLightOcclusion.visible(map,room,source.x,source.y,source.z,x+nx*.025,y+ny*.025,z+nz*.025)
        :clear(source.x,source.y,source.z,x+nx*.025,y+ny*.025,z+nz*.025);
    rememberShadow(shadow,visible);}
    if(visible){
     r+=weight*source.red;g+=weight*source.green;b+=weight*source.blue;
    }
   }
  }
  RGB result=new RGB(Math.min(1.6,r),Math.min(1.6,g),Math.min(1.6,b));
  normalColours.put(key,new Lit(result,version));
  return result;
 }
 private boolean emittingMaterial(int material){
  if(material<0||material==GlbProps.VAN_MAP_MATERIAL)return false;
  if(!knownEmissionMaterials.get(material)){
   // Reserved/sentinel IDs are not dense texture indices. In particular the
   // map CRT uses MAX_VALUE-2 and must never size a BitSet to that value.
   if(material>=GlbProps.TEXTURE_BASE&&!GlbProps.registeredTexture(material))return false;
   String name=GlbProps.materialName(material).toLowerCase(Locale.ROOT);
   if(name.contains("glass")||name.contains("bulb")||name.contains("flame")||name.startsWith("television"))emissionMaterials.set(material);
   knownEmissionMaterials.set(material);
  }
  return emissionMaterials.get(material);
 }
 public RGB emission(int material,double x,double y,double z){
  // LCD texels illuminate themselves without becoming extra room light sources.
  if(material==GlbProps.VAN_MAP_MATERIAL)return new RGB(1.0,1.0,1.0);
  if(material>=GlbProps.TEXTURE_BASE && GlbProps.registeredTexture(material)
      &&GlbProps.materialName(material).startsWith("van live screen "))
      return new RGB(1.0,1.0,1.0);
  if(!emittingMaterial(material))return RGB.ZERO;
  for(var s:candidates(x,y,z))if(factors[s.id]>.01&&Math.abs(x-s.x)<.4&&Math.abs(y-s.y)<.28&&Math.abs(z-s.z)<.4)return new RGB(s.red*1.35,s.green*1.35,s.blue*1.35);
  return RGB.ZERO;
 }
}
