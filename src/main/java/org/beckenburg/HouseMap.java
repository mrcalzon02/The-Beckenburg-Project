package org.beckenburg;

import java.util.*;
import org.beckenburg.LevelDefinition.*;

/** Authored metre-based mission geometry and authoritative physical queries. */
public final class HouseMap {
    public static final int COLS=3,ROWS=5,INDOOR_ROOMS=12,ROOM_COUNT=15;
    public static final int WIDTH=128,HEIGHT=128,GRAND_WIDTH=128,GRAND_HEIGHT=128,EXTRACTION_ROOM=14;
    public enum Site { HOUSE,FARMHOUSE,SCHOOL,MANSION,PRISON,ASYLUM,ACADEMY,COLLEGE,CHURCH,HOLLOW_CREEK }
    private final Site site;
    private final LevelDefinition level;
    private final SiteTerrain terrain;
    private final AuthoredSitePhysics nativeSite;
    public boolean nativeSite(){return nativeSite!=null;}
    public SiteTerrain terrain(){return terrain;}
    private LooseClutter clutter;
    private HauntedFixtures fixtures;
    public synchronized HauntedFixtures fixtures(){if(fixtures==null)fixtures=new HauntedFixtures(this);return fixtures;}
    public synchronized LooseClutter clutter(){if(clutter==null)clutter=new LooseClutter(this);return clutter;}
    private final List<Guard> guards;
    private int hidingDensity=70;
    private List<Collider> closetColliders=List.of();
    public int hidingDensity(){return hidingDensity;}
    public void configureHiding(int density){
        if(density<0||density>100)throw new IllegalArgumentException("hiding density");
        hidingDensity=density;closetColliders=LevelClosets.colliders(level,density);
    }
    private final Set<Integer> openDoors=new HashSet<>(),openWindows=new HashSet<>();
    /** Door poses are visual only: authoritative collision, sound and LAN snapshots
     * continue to use openDoors. No geometry/lightmap is rebuilt for each step. */
    static final long DOOR_SWING_NANOS=450_000_000L;
    private record DoorMotion(double from,double target,long startedAt){}
    private final Map<Integer,DoorMotion> doorMotions=new HashMap<>();
    /** Continuous 0=closed, 1=open door pose; smoothstep at a constant 450ms.
     * The clock overload is package-private for deterministic regression tests. */
    public double doorOpenFraction(int id){return doorOpenFraction(id,System.nanoTime());}
    double doorOpenFraction(int id,long now){
        DoorMotion motion=doorMotions.get(id);
        if(motion==null)return doorOpen(id)?1.0:0.0;
        long elapsed=Math.max(0L,now-motion.startedAt());
        if(elapsed>=DOOR_SWING_NANOS){
            doorMotions.remove(id);
            return motion.target();
        }
        double t=elapsed/(double)DOOR_SWING_NANOS;
        double eased=t*t*(3-2*t);
        return motion.from()+(motion.target()-motion.from())*eased;
    }
    /** An in-flight pose is transient; completed poses alone enter retained VBO caches. */
    public boolean doorMoving(int id){
        doorOpenFraction(id);
        return doorMotions.containsKey(id);
    }
    private boolean setDoorTarget(int id,boolean open,long now){
        if(doorOpen(id)==open)return false; // unchanged host snapshots never restart motion
        double from=doorOpenFraction(id,now);
        if(open)openDoors.add(id);else openDoors.remove(id);
        double target=open?1.0:0.0;
        if(Math.abs(from-target)<1e-6)doorMotions.remove(id);
        else doorMotions.put(id,new DoorMotion(from,target,now));
        return true;
    }
    private final VanStaging van;
    public HouseMap(){this(Site.HOUSE);}
    public HouseMap(Site site){
        this(site,LevelPlans.build(site));
    }
    HouseMap(Site site,LevelDefinition definition){
        this.site=Objects.requireNonNull(site);level=Objects.requireNonNull(definition);guards=level.guards();
        terrain=SiteTerrain.of(level);
        nativeSite=HollowCreekLevel.is(level)?HollowCreekLevel.physics():null;
        Dock d=level.dock();van=new VanStaging(new VanStaging.Placement(d.x(),d.y(),d.z(),d.yaw()));
        // Fixture anchors must be fixed before door state or difficulty can change.
        // Author access against every potential cupboard, then restore the default.
        configureHiding(100);fixtures=new HauntedFixtures(this);configureHiding(70);
    }
    public Site site(){return site;}
    public LevelDefinition level(){return level;}
    public boolean authoredArchitecture(){return true;}
    public VanStaging van(){return van;}
    public boolean sheltered(double x,double eyeY,double z){if(nativeSite!=null)return !nativeSite.clearSight(new Vec(x,eyeY+.03,z),new Vec(x,eyeY+10,z))||!van.clearSight(x,eyeY+.03,z,x,eyeY+10,z);int room=roomAt(x,eyeY-1.3,z);return room>=0&&!outside(room);}
    public int width(){return (int)Math.ceil(level.rooms().stream().mapToDouble(Room::x1).max().orElse(100))+3;}
    public int height(){return (int)Math.ceil(level.rooms().stream().mapToDouble(Room::z1).max().orElse(100))+3;}
    public int cellWidth(){return 8;}
    public int cellHeight(){return 6;}
    public double ceilingHeight(){return level.room(0).height();}
    public int roomCount(){return ROOM_COUNT;}
    public int interiorRoomCount(){return INDOOR_ROOMS;}
    public boolean outside(int room){return room>=0&&room<15&&level.room(room).outside();}
    public double floorY(int room){checkRoom(room);return level.floorY(room);}
    public Vec spawn(int room){checkRoom(room);return level.room(room).spawn();}
    public double centerX(int room){return spawn(room).x();}
    public double centerZ(int room){return spawn(room).z();}
    public String roomName(int room){return room<0||room>=15?"OUTSIDE":level.room(room).name();}
    public String roomName(double x,double z){return roomName(roomAt(x,z));}
    public int roomAt(double x,double y,double z){
        if(!finite(x,y,z))return -1;
        if(nativeSite!=null){var local=van.local(x,z);if(Math.abs(local.x())<VanStaging.INTERIOR_HALF_WIDTH&&local.z()>=VanStaging.FRONT&&local.z()<VanStaging.REAR&&y>=van.floorY()-.22&&y<van.placement().y()+VanStaging.ROOF_Y)return 14;var support=nativeSite.support(x,z,y,4);return support==null?-1:HollowCreekLevel.owner(x,z);}
        for(Stair s:level.stairs())if(s.contains(x,z,0)&&Math.abs(y-s.yAt(x,z))<.25)
            return s.t(x,z)<.5?s.a():s.b();
        Room owner=null;
        for(Room r:level.rooms())if(r.contains(x,y,z)&&(owner==null||r.y()>owner.y()+.01||Math.abs(r.y()-owner.y())<.01&&(r.x1()-r.x0())*(r.z1()-r.z0())<(owner.x1()-owner.x0())*(owner.z1()-owner.z0())))owner=r;
        if(owner!=null)return owner.id();
        for(Portal p:level.portals())if(!p.kind().equals("stair")&&Math.abs(y-p.y())<.25&&inPortal(p,x,z,0))return p.a();
        return -1;
    }
    /** Legacy ground-plane adapter; authoritative callers must supply feet Y. */
    public int roomAt(double x,double z){
        var cargo=van.local(x,z);if(Math.abs(cargo.x())<VanStaging.INTERIOR_HALF_WIDTH&&cargo.z()>=VanStaging.FRONT&&cargo.z()<VanStaging.REAR)return 14;
        if(nativeSite!=null){var support=nativeSite.support(x,z,0,4);return support==null?-1:HollowCreekLevel.owner(x,z);}
        if(terrain!=null){int graded=roomAt(x,groundHeight(x,z,12),z);if(graded>=0)return graded;}
        int room=roomAt(x,0,z);if(room>=0)return room;
        for(Room r:level.rooms())if(r.contains(x,r.y(),z))return r.id();return -1;
    }
    private static void checkRoom(int room){if(room<0||room>=15)throw new IllegalArgumentException("invalid room");}
    private static boolean finite(double... values){for(double v:values)if(!Double.isFinite(v))return false;return true;}
    private boolean inPortal(Portal p,double x,double z,double radius){
        if(p.a()==p.b())return p.alongX()?Math.abs(x-p.x())<=p.width()/2-radius&&Math.abs(z-p.z())<=.20:Math.abs(z-p.z())<=p.width()/2-radius&&Math.abs(x-p.x())<=.20;
        Room a=level.room(p.a()),b=level.room(p.b());
        if(p.alongX())return Math.abs(x-p.x())<=p.width()/2-radius
            &&z>=Math.min(a.z1(),b.z1())-.20&&z<=Math.max(a.z0(),b.z0())+.20;
        return Math.abs(z-p.z())<=p.width()/2-radius
            &&x>=Math.min(a.x1(),b.x1())-.20&&x<=Math.max(a.x0(),b.x0())+.20;
    }
    public boolean doorOpen(int id){return openDoors.contains(id);}
    public boolean doorOpen(int x,int z){return level.portals().stream().anyMatch(p->(int)p.x()==x&&(int)p.z()==z&&doorOpen(p.id()));}
    public String doorSnapshot(){return openDoors.stream().sorted().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));}
    public void applyDoorSnapshot(String encoded){
        if(encoded==null||encoded.length()>4096)throw new IllegalArgumentException("invalid doors");
        Set<Integer> ids=new HashSet<>();
        if(!encoded.isEmpty())for(String token:encoded.split(",",-1)){
            if(!token.matches("[0-9]{1,6}"))throw new IllegalArgumentException("invalid door id");
            int id=Integer.parseInt(token);
            if(level.portals().stream().noneMatch(p->p.id()==id&&p.door())||!ids.add(id))throw new IllegalArgumentException("invalid door id");
        }
        long now=System.nanoTime();
        for(int id:new HashSet<>(openDoors))if(!ids.contains(id))setDoorTarget(id,false,now);
        for(int id:ids)setDoorTarget(id,true,now);
    }
    public boolean windowOpen(int id){return openWindows.contains(id);}
    public String windowSnapshot(){return openWindows.stream().sorted().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));}
    public void applyWindowSnapshot(String encoded){
        if(encoded==null||encoded.length()>4096)throw new IllegalArgumentException("invalid windows");
        Set<Integer> next=new HashSet<>();
        if(!encoded.isEmpty())for(String token:encoded.split(",",-1)){
            if(!token.matches("[0-9]{1,6}"))throw new IllegalArgumentException("invalid window id");
            int id=Integer.parseInt(token);
            if(level.portals().stream().noneMatch(p->p.id()==id&&p.kind().equals("window"))||!next.add(id))throw new IllegalArgumentException("invalid window id");
        }
        openWindows.clear();openWindows.addAll(next);
    }
    public Portal nearestWindow(double x,double y,double z){
        if(!finite(x,y,z))return null;Portal best=null;double distance=1.85;
        for(Portal p:level.portals())if(p.kind().equals("window")&&Math.abs(p.y()-y)<.65){
            double d=Math.hypot(x-p.x(),z-p.z());if(d<distance){best=p;distance=d;}
        }
        return best;
    }
    /** Exact portal selected by the centre ray; no second nearest-object search. */
    public void toggleDoor(int id){toggleDoor(id,System.nanoTime());}
    void toggleDoor(int id,long now){
        if(level.portals().stream().noneMatch(p->p.id()==id&&p.door()))throw new IllegalArgumentException("invalid door");
        setDoorTarget(id,!doorOpen(id),now);
    }
    public void toggleWindow(int id){
        if(level.portals().stream().noneMatch(p->p.id()==id&&p.kind().equals("window")))throw new IllegalArgumentException("invalid window");
        if(!openWindows.add(id))openWindows.remove(id);
    }
    public boolean toggleNearestWindow(double x,double y,double z){
        Portal p=nearestWindow(x,y,z);if(p==null)return false;
        if(!openWindows.add(p.id()))openWindows.remove(p.id());return true;
    }
    public boolean toggleNearestDoor(double x,double y,double z){
        Portal best=null;double distance=1.65;
        for(Portal p:level.portals())if(p.door()&&Math.abs(p.y()-y)<.65){double d=Math.hypot(x-p.x(),z-p.z());if(d<distance){best=p;distance=d;}}
        if(best==null)return false;toggleDoor(best.id());return true;
    }
    public boolean toggleNearestDoor(double x,double z){int r=roomAt(x,z);return r>=0&&toggleNearestDoor(x,floorY(r),z);}
    public boolean openDoorBetweenRooms(int from,int to){
        boolean changed=false;long now=System.nanoTime();
        for(Portal p:level.portals())if(p.joins(from,to)&&p.door())changed|=setDoorTarget(p.id(),true,now);
        return changed;
    }
    public List<Integer> adjacentRooms(int room){checkRoom(room);return level.portalsFrom(room).stream().filter(p->!p.kind().equals("window")).map(p->p.a()==room?p.b():p.a()).filter(next->next!=room).distinct().toList();}
    public List<Integer> ghostExits(int room){return room==14?List.of():adjacentRooms(room).stream().filter(r->r!=14).toList();}
    public int[] doorBetweenRooms(int a,int b){for(Portal p:level.portals())if(p.joins(a,b)&&p.door())return new int[]{(int)p.x(),(int)p.z()};return null;}
    public boolean doorwayAlongX(int x,int z){return level.portals().stream().anyMatch(p->(int)p.x()==x&&(int)p.z()==z&&!p.alongX());}
    public VanStaging.Position hidingSpot(int room,int density){
        if(density<0||density>100)throw new IllegalArgumentException("hiding density");
        if(!LevelClosets.available(level,room,density))return null;
        Socket s=level.socket("hide",room);return s==null?null:new VanStaging.Position(s.x(),s.z());
    }
    private boolean supported(double x,double y,double z,double radius){
        Double vanY=van.supportHeight(x,z);if(vanY!=null)return Math.abs(y-vanY)<.10;
        for(Stair s:level.stairs())if(s.contains(x,z,radius)&&Math.abs(y-s.yAt(x,z))<.24)return true;
        for(Room r:level.rooms())if(Math.abs(y-surface(r,x,z))<.08&&x>=r.x0()&&x<=r.x1()&&z>=r.z0()&&z<=r.z1()&&!level.floorVoid(x,r.y(),z))return true;
        for(Portal p:level.portals())if(!p.kind().equals("stair")&&!p.kind().equals("window")&&Math.abs(y-p.y())<.06&&inPortal(p,x,z,radius))return true;
        return false;
    }
    private static boolean boxHit(double x,double y,double z,double radius,double bx,double by,double bz,double hx,double hz,double height,double yaw){
        return boxHit(x,y,z,radius,bx,by,bz,hx,hz,height,yaw,HunterPosture.STANDING_HEIGHT);
    }
    private static boolean boxHit(double x,double y,double z,double radius,double bx,double by,double bz,double hx,double hz,double height,double yaw,double bodyHeight){
        if(y+bodyHeight<=by+.02||y>=by+height-.02)return false;
        double dx=x-bx,dz=z-bz,c=Math.cos(yaw),s=Math.sin(yaw);
        return Math.abs(dx*c+dz*s)<hx+radius&&Math.abs(-dx*s+dz*c)<hz+radius;
    }
    private boolean wallOpening(Room room,boolean edgeAlongX,double normal,double tangent,double y,double radius,boolean ray,int selectedPortal,double bodyHeight){
        for(Portal p:level.portalsFrom(room.id())){
            if(p.kind().equals("stair"))continue;
            if(p.alongX()!=edgeAlongX)continue;
            double plane=edgeAlongX?p.z():p.x(),mid=edgeAlongX?p.x():p.z();
            if(Math.abs(plane-normal)>.55||Math.abs(tangent-mid)>p.width()/2-radius)continue;
            if(p.kind().equals("window")){
                if(ray&&y>p.y()+1.0&&y<p.y()+2.35)return true;
            }else if(y>=p.y()-.10&&y+(ray?0:bodyHeight)<=p.y()+2.45
                    &&(!p.door()||doorOpen(p.id())||p.id()==selectedPortal))return true;
        }
        for(Stair stair:level.stairs())if((stair.a()==room.id()||stair.b()==room.id())
                &&stair.contains(edgeAlongX?tangent:normal,edgeAlongX?normal:tangent,radius)
                &&y>=stair.yAt(edgeAlongX?tangent:normal,edgeAlongX?normal:tangent)-.12
                    -(radius+LevelDefinition.WALL_HALF_THICKNESS)*Math.abs(stair.y1()-stair.y0())/Math.hypot(stair.x1()-stair.x0(),stair.z1()-stair.z0())
                &&y<=stair.yAt(edgeAlongX?tangent:normal,edgeAlongX?normal:tangent)+2.4)return true;
        return false;
    }
    /** Every indoor perimeter remains solid even when exterior support overlaps it. */
    private boolean wallBlocked(double x,double y,double z,double radius,boolean ray){return wallBlocked(x,y,z,radius,ray,-1);}
    private boolean wallBlocked(double x,double y,double z,double radius,boolean ray,int selectedPortal){return wallBlocked(x,y,z,radius,ray,selectedPortal,HunterPosture.STANDING_HEIGHT);}
    private boolean wallBlocked(double x,double y,double z,double radius,boolean ray,int selectedPortal,double bodyHeight){
        for(Room r:level.rooms()){
            if(r.outside()||y+(ray?0:bodyHeight)<=r.y()+.03||y>=r.y()+r.height())continue;
            if(r.name().toLowerCase().contains("loft")&&y>=r.y()+1.0)continue;
            if(x>=r.x0()-radius&&x<=r.x1()+radius){
                if(Math.abs(z-r.z0())<radius+LevelDefinition.WALL_HALF_THICKNESS+.005&&!wallOpening(r,true,r.z0(),x,y,radius,ray,selectedPortal,bodyHeight))return true;
                if(Math.abs(z-r.z1())<radius+LevelDefinition.WALL_HALF_THICKNESS+.005&&!wallOpening(r,true,r.z1(),x,y,radius,ray,selectedPortal,bodyHeight))return true;
            }
            if(z>=r.z0()-radius&&z<=r.z1()+radius){
                if(Math.abs(x-r.x0())<radius+LevelDefinition.WALL_HALF_THICKNESS+.005&&!wallOpening(r,false,r.x0(),z,y,radius,ray,selectedPortal,bodyHeight))return true;
                if(Math.abs(x-r.x1())<radius+LevelDefinition.WALL_HALF_THICKNESS+.005&&!wallOpening(r,false,r.x1(),z,y,radius,ray,selectedPortal,bodyHeight))return true;
            }
        }
        return false;
    }
    public boolean traversable(double x,double y,double z){return traversable(x,y,z,HunterPosture.STANDING_HEIGHT);}
    public boolean traversable(double x,double y,double z,double bodyHeight){
        if(!finite(x,y,z,bodyHeight)||bodyHeight<.9||bodyHeight>2.0)return false;double radius=.22;
        if(nativeSite!=null){Double sy=van.supportHeight(x,z);boolean blocked=van.collides(x,z,radius)&&y+bodyHeight>van.placement().y()&&y<van.placement().y()+VanStaging.ROOF_Y;
            if(sy!=null)return Math.abs(y-sy)<.08&&!blocked&&!nativeSite.bodyBlocked(new Vec(x,y,z),bodyHeight,radius);
            if(WorldWater.module(level.water()).bodies().stream().anyMatch(w->y<w.y()-.01&&w.wet(x,z)))return false;
            return !blocked&&nativeSite.traversable(new Vec(x,y,z),bodyHeight);}
        if(!supported(x,y,z,radius)||wallBlocked(x,y,z,radius,false,-1,bodyHeight))return false;
        for(double[] d:new double[][]{{-radius,0},{radius,0},{0,-radius},{0,radius}})
            if(!supported(x+d[0],y,z+d[1],0)&&!(terrain!=null&&terrain.playable(x+d[0],z+d[1])&&!terrain.indoors(x+d[0],z+d[1])&&Math.abs(y-terrain.height(x+d[0],z+d[1]))<.20))return false;
        for(Portal p:level.portals())if(p.door()&&!doorOpen(p.id())&&boxHit(x,y,z,radius,p.x(),p.y(),p.z(),p.alongX()?p.width()/2:.10,p.alongX()?.10:p.width()/2,2.4,0,bodyHeight))return false;
        for(Collider p:level.colliders())if(boxHit(x,y,z,radius,p.x(),p.y(),p.z(),p.halfX(),p.halfZ(),p.height(),p.yaw(),bodyHeight)&&!DeckSlabs.landingContact(level,p,x,y,z))return false;
        for(Collider p:closetColliders)if(boxHit(x,y,z,radius,p.x(),p.y(),p.z(),p.halfX(),p.halfZ(),p.height(),p.yaw(),bodyHeight))return false;
        for(Guard g:guards)if(boxHit(x,y,z,radius,(g.x0()+g.x1())/2,g.y(),(g.z0()+g.z1())/2,
            Math.max(.035,Math.abs(g.x1()-g.x0())/2),Math.max(.035,Math.abs(g.z1()-g.z0())/2),1.0,0,bodyHeight))return false;
        if(Math.abs(y-level.dock().y())<1.5&&(van.collides(x,z,radius)||VanArrivalStreet.collision(van,x,z,radius)))return false;
        // A higher deck is a solid ceiling unless its authored stair opening removes it.
        for(Room r:level.rooms())if(r.y()>y+.12&&r.y()<y+bodyHeight+.04&&x>r.x0()-radius&&x<r.x1()+radius&&z>r.z0()-radius&&z<r.z1()+radius&&!level.floorVoid(x,r.y(),z))return false;
        for(Room r:level.rooms())if(!r.outside()&&x>r.x0()&&x<r.x1()&&z>r.z0()&&z<r.z1()&&y>=r.y()-.05&&r.y()+r.height()>y+.12&&y+bodyHeight>r.y()+r.height()-.03&&!level.floorVoid(x,r.y()+r.height(),z))return false;
        return true;
    }
    public boolean traversable(double x,double z){if(nativeSite!=null){double y=groundHeight(x,z,roomAt(x,z));return traversable(x,y,z);}Double deck=van.supportHeight(x,z);int r=roomAt(x,z);return r>=0&&traversable(x,deck==null?surface(level.room(r),x,z):deck,z);}
    private double surface(Room r,double x,double z){return terrain!=null&&r.outside()&&r.id()!=14?terrain.height(x,z):r.y();}
    /** Physical support for entities: preserve a room's lower storey underneath an overlapping loft. */
    public double groundHeight(double x,double z,int roomHint){
        if(nativeSite!=null){Double deck=van.supportHeight(x,z);if(deck!=null)return deck;var support=nativeSite.support(x,z,roomHint>=0&&roomHint<15?level.floorY(roomHint):0,4);return support==null?Double.NaN:support.y();}
        // Stair surfaces must win over the nominal room floor even on ordinary
        // (non-terrain) maps. The former early return flattened every stair
        // query to its room's ground plane, including cellar approaches.
        Double vanY=van.supportHeight(x,z);if(vanY!=null)return vanY;
        for(var stair:level.stairs())if((stair.a()==roomHint||stair.b()==roomHint)&&stair.contains(x,z,0))return stair.yAt(x,z);
        if(roomHint>=0&&roomHint<12){var r=level.room(roomHint);if(x>=r.x0()&&x<=r.x1()&&z>=r.z0()&&z<=r.z1()){
            for(var s:level.stairs())if((s.a()==roomHint||s.b()==roomHint)&&s.contains(x,z,0))return s.yAt(x,z);
            return r.y();
        }}
        Room lowest=null;for(var r:level.rooms())if(!r.outside()&&x>=r.x0()&&x<=r.x1()&&z>=r.z0()&&z<=r.z1()&&(lowest==null||r.y()<lowest.y()))lowest=r;
        if(lowest!=null)return lowest.y();
        return terrain!=null?terrain.height(x,z):roomHint>=0&&roomHint<15?level.room(roomHint).y():0;
    }
    private Double supportNear(double x,double z,double previousY){
        Double vanY=van.supportHeight(x,z);if(vanY!=null)return Math.abs(vanY-previousY)<.251?vanY:null;
        if(nativeSite!=null){var s=nativeSite.support(x,z,previousY,.251);return s==null?null:s.y();}
        Double best=null;double delta=.251;
        for(Stair s:level.stairs())if(s.contains(x,z,.22)){double sy=s.yAt(x,z),d=Math.abs(sy-previousY);if(d<delta){best=sy;delta=d;}}
        // A ramp owns its deck opening; prefer it over the overlapping lower room.
        if(best!=null)return best;
        for(Room r:level.rooms())if(x>=r.x0()&&x<=r.x1()&&z>=r.z0()&&z<=r.z1()&&!level.floorVoid(x,r.y(),z)){double sy=surface(r,x,z),d=Math.abs(sy-previousY);if(d<delta){best=sy;delta=d;}}
        for(Portal p:level.portals())if(!p.kind().equals("stair")&&!p.kind().equals("window")&&inPortal(p,x,z,.22)){double d=Math.abs(p.y()-previousY);if(d<delta){best=p.y();delta=d;}}
        return best;
    }
    /** Swept horizontal input; elevation comes solely from contiguous authored support. */
    public Vec resolveMove(Vec from,double x,double z){return resolveMove(from,x,z,HunterPosture.STANDING_HEIGHT);}
    public Vec resolveMove(Vec from,double x,double z,double bodyHeight){
        if(from==null||!finite(from.x(),from.y(),from.z(),x,z))return null;
        double distance=Math.hypot(x-from.x(),z-from.z());if(distance>2)return null;
        int steps=Math.max(1,(int)Math.ceil(distance/.055));Vec at=from;
        for(int i=1;i<=steps;i++){
            double nx=from.x()+(x-from.x())*i/steps,nz=from.z()+(z-from.z())*i/steps;
            Double y=supportNear(nx,nz,at.y());if(y==null||!traversable(nx,y,nz,bodyHeight))return null;at=new Vec(nx,y,nz);
        }
        return at;
    }
    /** Small-body sweep, including its radius, ceiling and furnishing contact. */
    public boolean clearObjectPath(double ax,double ay,double az,double bx,double by,double bz,double radius,double half){
        if(!finite(ax,ay,az,bx,by,bz,radius,half)||radius<0||radius>.3||half<0||half>.4)return false;
        if(nativeSite!=null){for(double[] d:new double[][]{{0,0,0},{radius,0,0},{-radius,0,0},{0,half,0},{0,-half,0},{0,0,radius},{0,0,-radius}})if(!nativeSite.clearSight(new Vec(ax+d[0],ay+d[1],az+d[2]),new Vec(bx+d[0],by+d[1],bz+d[2])))return false;return true;}
        int steps=Math.max(1,(int)Math.ceil(Math.sqrt((bx-ax)*(bx-ax)+(by-ay)*(by-ay)+(bz-az)*(bz-az))/.025));
        for(int n=1;n<=steps;n++){
            double t=n/(double)steps,x=ax+(bx-ax)*t,y=ay+(by-ay)*t,z=az+(bz-az)*t;
            if(wallBlocked(x,y+half*.5,z,radius,true))return false;
            boolean volume=false;for(Room r:level.rooms())if(r.contains(x,y,z)){volume=true;break;}
            if(!volume)for(Portal p:level.portals())if(y>=p.y()&&y<p.y()+2.4&&inPortal(p,x,z,radius))volume=true;
            if(!volume)return false;
            for(Room r:level.rooms())if(!r.outside()&&x>r.x0()&&x<r.x1()&&z>r.z0()&&z<r.z1()){
                double ceiling=r.y()+r.height();if(ay<ceiling&&y+half>ceiling&&!level.floorVoid(x,ceiling,z))return false;
            }
            for(Collider c:level.colliders())if(y+half>c.y()+.003&&y-half<c.y()+c.height()-.003
                    &&boxHit(x,y-1,z,radius,c.x(),c.y(),c.z(),c.halfX(),c.halfZ(),c.height(),c.yaw()))return false;
            for(Collider c:closetColliders)if(y+half>c.y()&&y-half<c.y()+c.height()
                    &&boxHit(x,y-1,z,radius,c.x(),c.y(),c.z(),c.halfX(),c.halfZ(),c.height(),c.yaw()))return false;
            for(Guard g:guards)if(y+half>g.y()&&y-half<g.y()+1&&boxHit(x,y-1,z,radius,(g.x0()+g.x1())/2,g.y(),(g.z0()+g.z1())/2,Math.max(.035,Math.abs(g.x1()-g.x0())/2),Math.max(.035,Math.abs(g.z1()-g.z0())/2),1,0))return false;
        }return true;
    }
    public boolean clearLineOfSight(double ax,double ay,double az,double bx,double by,double bz){return clearInteractionSight(ax,ay,az,bx,by,bz,-1);}
    /** A door face can be reached through its own conservative collision thickness.
     * All other doors, walls, storeys and furnishing still occlude the interaction ray. */
    public boolean clearInteractionSight(double ax,double ay,double az,double bx,double by,double bz,int selectedPortal){
        if(!finite(ax,ay,az,bx,by,bz))return false;
        if(nativeSite!=null)return nativeSite.clearSight(new Vec(ax,ay,az),new Vec(bx,by,bz))&&van.clearSight(ax,ay,az,bx,by,bz);
        double distance=Math.sqrt((bx-ax)*(bx-ax)+(by-ay)*(by-ay)+(bz-az)*(bz-az));if(distance>40)return false;if(distance<.01)return true;
        int steps=(int)Math.ceil(distance/.075);
        for(int i=1;i<steps;i++){
            double t=i/(double)steps,x=ax+(bx-ax)*t,y=ay+(by-ay)*t,z=az+(bz-az)*t;
            boolean inside=false;
            for(Room r:level.rooms())if(r.contains(x,y,z)){inside=true;break;}
            for(Portal p:level.portals())if(!p.kind().equals("stair")&&y>=p.y()&&y<p.y()+2.4&&inPortal(p,x,z,0)){inside=true;if(p.door()&&!doorOpen(p.id())&&p.id()!=selectedPortal)return false;}
            if(!inside||wallBlocked(x,y,z,.015,true,selectedPortal))return false;
            for(Room r:level.rooms())if((Math.abs(y-surface(r,x,z))<.06||terrain!=null&&r.outside()&&r.id()!=14&&y<terrain.height(x,z))&&x>=r.x0()&&x<=r.x1()&&z>=r.z0()&&z<=r.z1()&&!level.floorVoid(x,r.y(),z))return false;
            for(Collider p:level.colliders())if(y>p.y()&&y<p.y()+p.height()&&boxHit(x,y-1,z,0,p.x(),p.y(),p.z(),p.halfX(),p.halfZ(),p.height(),p.yaw()))return false;
            for(Collider p:closetColliders)if(y>p.y()&&y<p.y()+p.height()&&boxHit(x,y-1,z,0,p.x(),p.y(),p.z(),p.halfX(),p.halfZ(),p.height(),p.yaw()))return false;
        }
        return true;
    }
    public boolean clearLineOfSight(double ax,double az,double bx,double bz){int a=roomAt(ax,az),b=roomAt(bx,bz);return a>=0&&b>=0&&clearLineOfSight(ax,floorY(a)+1.3,az,bx,floorY(b)+1.3,bz);}
    /** Legacy map-overlay sampling; production rendering reads level surfaces directly. */
    public char tile(int x,int z){
        for(Portal p:level.portals())if(p.door()&&(int)p.x()==x&&(int)p.z()==z)return 'D';
        int room=roomAt(x+.5,0,z+.5);return room<0?'#':outside(room)?'o':'.';
    }
    public boolean solid(int x,int z){char t=tile(x,z);return t=='#'||t=='D'&&!doorOpen(x,z);}
    public boolean valid(){
        Set<Integer> seen=new HashSet<>();ArrayDeque<Integer> queue=new ArrayDeque<>();queue.add(14);
        while(!queue.isEmpty()){int r=queue.remove();if(seen.add(r))queue.addAll(adjacentRooms(r));}
        if(seen.size()!=15)return false;
        for(Room r:level.rooms())if(!traversable(r.spawn().x(),r.spawn().y(),r.spawn().z()))return false;
        return true;
    }
}
