package org.beckenburg;

import org.beckenburg.LevelDefinition.*;
import org.beckenburg.editor.EditorProject;
import org.beckenburg.editor.InMemoryRuntimeScene;
import org.beckenburg.editor.RuntimeScene;
import org.beckenburg.editor.RuntimeSceneDocument;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Mission-site authoring adapter. The editor and LevelPlans both consume the
 * same versioned document. A saved prop position is used by game rendering,
 * traversability, sight and interaction collision on the next level load.
 * Unlike a separate scene preview, this adapter mutates the LevelDefinition
 * that HouseMap owns.
 */
public final class LevelAuthoringSession implements EditorProject<LevelAuthoringSession.Snapshot> {
    private static final String COMPONENT = "beckenburg.level.prop";
    private static final String PORTAL_COMPONENT = "beckenburg.level.door-width";
    private static final String PORTAL_PREFIX = "beckenburg-door-width-";
    // A bounded native architectural partition is source geometry, not a prop.
    // LevelDefinition consumes it as one physical collider and one render socket.
    private static final String WALL_PREFIX = MissionStructuralWall.NODE_PREFIX;
    private static final String FLOOR_PREFIX = "beckenburg-floor-depth-";
    private static final String FLOOR_COMPONENT = "beckenburg.level.floor-depth";
    private static final String STAIR_PREFIX = "beckenburg-stair-landing-";
    private static final String STAIR_COMPONENT = "beckenburg.level.stair-landing";
    private final String documentId;
    private final Path document;
    private LevelDefinition source;
    private final boolean builtInHouse;
    private final ArrayList<Prop> props = new ArrayList<>();
    private final ArrayList<String> propIds = new ArrayList<>();
    /** Overrides only existing source doorway widths; no second structural scene. */
    private final java.util.LinkedHashMap<Integer,Double> doorWidths = new java.util.LinkedHashMap<>();
    private final ArrayList<MissionStructuralWall> walls = new ArrayList<>();
    /** Per-room authored deck depth; built-in source slabs are retained unless overridden. */
    private final java.util.LinkedHashMap<Integer,Double> floorDepths = new java.util.LinkedHashMap<>();
    /** Source stair terminal cut extensions, without new synthetic rooms. */
    private final java.util.LinkedHashMap<Integer,Double> stairExtensions = new java.util.LinkedHashMap<>();
    private long revision=1;
    private String diskFingerprint;
    /** Identity is authoring metadata paired with the native props, not another scene. */
    public record Snapshot(String sceneId,List<String> ids,List<Prop> props,String architecture,
                           Map<Integer,Double> doorWidths,List<MissionStructuralWall> walls,
                           Map<Integer,Double> floorDepths,Map<Integer,Double> stairExtensions){
        public Snapshot(String sceneId,List<String> ids,List<Prop> props){
            this(sceneId,ids,props,null,Map.of(),List.of(),Map.of(),Map.of());
        }
        public Snapshot(String sceneId,List<String> ids,List<Prop> props,String architecture){
            this(sceneId,ids,props,architecture,Map.of(),List.of(),Map.of(),Map.of());
        }
        public Snapshot(String sceneId,List<String> ids,List<Prop> props,String architecture,
                        Map<Integer,Double> doorWidths){
            this(sceneId,ids,props,architecture,doorWidths,List.of(),Map.of(),Map.of());
        }
        public Snapshot(String sceneId,List<String> ids,List<Prop> props,String architecture,
                        Map<Integer,Double> doorWidths,List<MissionStructuralWall> walls){
            this(sceneId,ids,props,architecture,doorWidths,walls,Map.of(),Map.of());
        }
        public Snapshot(String sceneId,List<String> ids,List<Prop> props,String architecture,
                        Map<Integer,Double> doorWidths,List<MissionStructuralWall> walls,
                        Map<Integer,Double> floorDepths){
            this(sceneId,ids,props,architecture,doorWidths,walls,floorDepths,Map.of());
        }
        public Snapshot{
            stairExtensions=Map.copyOf(Objects.requireNonNull(stairExtensions));
            if(stairExtensions.size()>24||stairExtensions.entrySet().stream().anyMatch(e->
                e.getKey()==null||e.getKey()<0||e.getValue()==null||
                !Double.isFinite(e.getValue())||e.getValue()<.01||e.getValue()>.16))
                throw new IllegalArgumentException("Invalid stair-terminal openings");
            floorDepths=Map.copyOf(Objects.requireNonNull(floorDepths));
            if(floorDepths.size()>12||floorDepths.entrySet().stream().anyMatch(entry->
                entry.getKey()==null||entry.getKey()<0||entry.getValue()==null||
                !Double.isFinite(entry.getValue())||entry.getValue()<.12||entry.getValue()>.45))
                throw new IllegalArgumentException("Invalid native floor thickness overrides");
            walls=Objects.requireNonNull(walls).stream()
                .sorted(java.util.Comparator.comparing(MissionStructuralWall::id)).toList();
            if(walls.size()>256||walls.stream().map(MissionStructuralWall::id).distinct().count()!=walls.size())
                throw new IllegalArgumentException("Duplicate or excessive structural wall IDs");
            doorWidths=Map.copyOf(Objects.requireNonNull(doorWidths));
            if(doorWidths.size()>128||doorWidths.entrySet().stream().anyMatch(entry->
                entry.getKey()==null||entry.getKey()<0||entry.getValue()==null||
                !Double.isFinite(entry.getValue())||entry.getValue()<.75||entry.getValue()>2.8))
                throw new IllegalArgumentException("Invalid native doorway widths");
            Objects.requireNonNull(sceneId);ids=List.copyOf(ids);props=List.copyOf(props);
            if(sceneId.isBlank()||ids.size()!=props.size()||ids.size()>4096
                ||new java.util.HashSet<>(ids).size()!=ids.size()
                ||ids.stream().anyMatch(id->id.isBlank()||id.length()>128))
                throw new IllegalArgumentException("Invalid mission identity snapshot");
            if(architecture!=null){
                HouseSourceVersions.validate(architecture);
                if(!sceneId.equals("beckenburg/house")||ids.contains(HouseSourceVersions.NODE))
                    throw new IllegalArgumentException("House source metadata belongs to another mission");
            }
        }
    }

    public LevelAuthoringSession(HouseMap.Site site) throws IOException {
        Objects.requireNonNull(site);
        this.builtInHouse=site==HouseMap.Site.HOUSE;
        this.documentId=sceneId(site);this.document=path(site);
        this.source=LevelPlans.buildSource(site);
        var loaded=loadRevision(document,documentId,source);restore(loaded.state());diskFingerprint=loaded.fingerprint();
    }
    public String id(){return documentId;}
    private static String sceneId(HouseMap.Site site){
        return site==HouseMap.Site.USER
            ?"beckenburg/user/"+UserLevelLibrary.activeId()
            :"beckenburg/"+site.name().toLowerCase(Locale.ROOT);
    }
    public String title(){return source.title();}
    public Path documentPath(){return document;}
    public static Path path(HouseMap.Site site){
        if(site==HouseMap.Site.USER)return UserLevelLibrary.activeDocument();
        return Path.of(System.getProperty("user.home"),".beckenburg","editor","levels",
            site.name().toLowerCase(Locale.ROOT)+".bscn").toAbsolutePath().normalize();
    }
    public synchronized List<Prop> props(){return List.copyOf(props);}
    public synchronized int size(){return props.size();}
    public synchronized Prop prop(int index){return props.get(index);}
    public synchronized String nodeId(int index){return propIds.get(index);}
    public synchronized List<MissionStructuralWall> walls(){return List.copyOf(walls);}
    public synchronized Map<Integer,Double> floorDepths(){return Map.copyOf(floorDepths);}
    public synchronized Map<Integer,Double> stairExtensions(){return Map.copyOf(stairExtensions);}
    /** Move a source stair's upper cut through its actual terminal riser.
     * Geometry, guarded edge, renderer and body solver consume one Void. */
    public synchronized void extendStairOpening(int stairId,double extension){
        if(HollowCreekLevel.is(source))
            throw new UnsupportedOperationException("Outdoor stair landing adapter is not implemented");
        if(!Double.isFinite(extension)||extension!=0&&(extension<.01||extension>.16))
            throw new IllegalArgumentException("Stair extension must be 0 or 0.01..0.16m");
        var proposed=new java.util.LinkedHashMap<>(stairExtensions);
        if(extension==0)proposed.remove(stairId);
        else proposed.put(stairId,extension);
        validateStairExtensions(source,proposed);
        validateFloorDepths(source,floorDepths,proposed);
        if(proposed.equals(stairExtensions))return;
        stairExtensions.clear();stairExtensions.putAll(proposed);revision++;
    }
    private static Stair stairById(LevelDefinition source,int id){
        return source.stairs().stream().filter(stair->stair.id()==id).findFirst()
            .orElseThrow(()->new IllegalArgumentException("Unknown native stair ID "+id));
    }
    private static int stairVoidIndex(LevelDefinition source,Stair stair){
        int index=-1;
        for(int i=0;i<source.voids().size();i++){
            var hole=source.voids().get(i);
            if(Math.abs(hole.y()-stair.y1())>.025||
                !stair.contains((hole.x0()+hole.x1())/2,(hole.z0()+hole.z1())/2,-.2))continue;
            if(index!=-1)throw new IllegalArgumentException("Ambiguous native stair opening");
            index=i;
        }
        if(index<0)throw new IllegalArgumentException("No source deck aperture for stair "+stair.id());
        return index;
    }
    private static void validateStairExtensions(LevelDefinition source,Map<Integer,Double> values){
        if(values.size()>24)throw new IllegalArgumentException("Too many native stair openings");
        for(var entry:values.entrySet()){
            Stair stair=stairById(source,entry.getKey());
            double ext=entry.getValue();
            if(!Double.isFinite(ext)||ext<.01||ext>.16||stair.y1()<=stair.y0()+1
                ||stair.y1()<=.05)
                throw new IllegalArgumentException("Only bounded rising upper stair terminals are editable");
            Room upper=source.room(stair.b());
            if(upper.outside()||Math.abs(upper.y()-stair.y1())>.05)
                throw new IllegalArgumentException("Stair endpoint does not belong to an upper room");
            stairVoidIndex(source,stair);
            double length=Math.hypot(stair.x1()-stair.x0(),stair.z1()-stair.z0());
            double xx=stair.x1()+(stair.x1()-stair.x0())/length*ext;
            double zz=stair.z1()+(stair.z1()-stair.z0())/length*ext;
            if(xx<=upper.x0()+.20||xx>=upper.x1()-.20||
                zz<=upper.z0()+.20||zz>=upper.z1()-.20)
                throw new IllegalArgumentException("Terminal cut exceeds authored upstairs room");
        }
    }
    private static List<LevelDefinition.Void> effectiveVoids(LevelDefinition source,Map<Integer,Double> extensions){
        if(extensions.isEmpty())return source.voids();
        var edited=new ArrayList<>(source.voids());
        for(var entry:new java.util.TreeMap<>(extensions).entrySet()){
            Stair stair=stairById(source,entry.getKey());
            int index=stairVoidIndex(source,stair);
            var original=source.voids().get(index);
            double x0=original.x0(),z0=original.z0(),x1=original.x1(),z1=original.z1();
            // The original floor cut ends 0.1m before the final tread.
            // This is a REAL void that can extend at most 0.16m past it.
            if(Math.abs(stair.x1()-stair.x0())>Math.abs(stair.z1()-stair.z0())){
                if(stair.x1()>stair.x0())x1=stair.x1()+entry.getValue();
                else x0=stair.x1()-entry.getValue();
            }else{
                if(stair.z1()>stair.z0())z1=stair.z1()+entry.getValue();
                else z0=stair.z1()-entry.getValue();
            }
            if(x1<=x0||z1<=z0)throw new IllegalArgumentException("Empty stair opening");
            edited.set(index,new LevelDefinition.Void(original.y(),x0,z0,x1,z1));
        }
        return List.copyOf(edited);
    }

    /** Existing upper decks become physical slabs with a genuine underside. */
    public synchronized void setFloorDepth(int roomId,double depth){
        if(HollowCreekLevel.is(source))
            throw new UnsupportedOperationException("Outdoor floor adapter is not implemented");
        if(!Double.isFinite(depth)||depth!=0&&(depth<.12||depth>.45))
            throw new IllegalArgumentException("Native deck thickness must be 0 (revert) or 0.12..0.45 metres");
        var candidate=new java.util.LinkedHashMap<>(floorDepths);
        if(depth==0)candidate.remove(roomId);
        else candidate.put(roomId,depth);
        validateFloorDepths(source,candidate,stairExtensions);
        if(candidate.equals(floorDepths))return;
        floorDepths.clear();floorDepths.putAll(candidate);revision++;
    }
    private static void validateFloorDepths(LevelDefinition source,Map<Integer,Double> values,
                                            Map<Integer,Double> stairExtensions){
        if(values.size()>12)throw new IllegalArgumentException("Too many native floor-depth overrides");
        for(var entry:values.entrySet()){
            int id=entry.getKey();double depth=entry.getValue();
            if(id<0||id>=source.rooms().size()||!Double.isFinite(depth)||depth<.12||depth>.45)
                throw new IllegalArgumentException("Unsupported native floor depth");
            var upper=source.room(id);
            if(upper.outside()||upper.y()<=.05)
                throw new IllegalArgumentException("Floor slabs require an existing upper-storey room");
            // A deep slab must not obstruct the final rising stair step.
            // Only a fully authored source void at this native terminal can
            // permit slab depth: visual-only openings are not sufficient.
            for(Stair stair:source.stairs()){
                if(stair.a()==id&&Math.abs(stair.y0()-upper.y())<.05)
                    throw new IllegalArgumentException("Descending upper landing adapter unavailable");
                if(stair.b()==id&&Math.abs(stair.y1()-upper.y())<.05
                    &&stairExtensions.getOrDefault(stair.id(),0.0)<.15)
                    throw new IllegalArgumentException(
                        "Deep upper deck needs a first-class stair opening with 0.15m terminal clearance");
            }
            double mx=(upper.x0()+upper.x1())/2,mz=(upper.z0()+upper.z1())/2;
            boolean supported=source.rooms().stream().anyMatch(lower->
                lower.id()!=id&&!lower.outside()&&lower.y()<upper.y()-1.0
                &&lower.y()+lower.height()>=upper.y()-.20
                &&mx>lower.x0()&&mx<lower.x1()&&mz>lower.z0()&&mz<lower.z1());
            if(!supported)throw new IllegalArgumentException("Upper deck has no existing structural room beneath it");
        }
    }
    private static List<Socket> effectiveFloorSockets(LevelDefinition source,Map<Integer,Double> depths){
        if(depths.isEmpty())return source.sockets();
        var result=new ArrayList<Socket>();
        for(var socket:source.sockets())
            if(!socket.type().equals("deck_slab")||!depths.containsKey(socket.room()))result.add(socket);
        for(var entry:new java.util.TreeMap<>(depths).entrySet()){
            var room=source.room(entry.getKey());
            result.add(new Socket(FLOOR_PREFIX+entry.getKey(),"deck_slab",room.id(),
                (room.x0()+room.x1())/2,room.y(),(room.z0()+room.z1())/2,
                0,Double.toString(entry.getValue())));
        }
        return List.copyOf(result);
    }
    public synchronized void placeWall(MissionStructuralWall candidate){
        if(HollowCreekLevel.is(source))
            throw new UnsupportedOperationException("Native outdoor structural wall editing unavailable");
        Objects.requireNonNull(candidate);
        if(walls.stream().anyMatch(w->w.id().equals(candidate.id())))
            throw new IllegalArgumentException("Duplicate native structural wall ID");
        var next=new ArrayList<>(walls);
        next.add(candidate);
        validateWalls(source,next,props);
        walls.add(candidate);revision++;
    }
    /** Edit the same native wall node and scene history, never a shadow mesh. */
    public synchronized void setWallOpening(String nodeId,double offset,double width,double clearHeight){
        if(nodeId==null||!nodeId.startsWith(WALL_PREFIX))
            throw new IllegalArgumentException("Expected authored structural wall node");
        String id=nodeId.substring(WALL_PREFIX.length());
        int index=-1;
        for(int i=0;i<walls.size();i++)if(walls.get(i).id().equals(id)){index=i;break;}
        if(index<0)throw new IllegalArgumentException("Unknown native structural wall");
        var next=walls.get(index).withOpening(offset,width,clearHeight);
        if(next.equals(walls.get(index)))return;
        var candidate=new ArrayList<>(walls);
        candidate.set(index,next);
        validateWalls(source,candidate,props);
        walls.set(index,next);
        revision++;
    }
    /** Attach an operable in-game door to an existing bounded wall cut.
     * Its network/interaction portal ID is persisted on the same native wall.
     * This does not create a new investigation zone or substitute a room graph. */
    public synchronized void setWallDoor(String nodeId,boolean installed){
        if(nodeId==null||!nodeId.startsWith(WALL_PREFIX))
            throw new IllegalArgumentException("Expected native wall node");
        String id=nodeId.substring(WALL_PREFIX.length());
        int index=-1;
        for(int i=0;i<walls.size();i++)if(walls.get(i).id().equals(id)){index=i;break;}
        if(index<0)throw new IllegalArgumentException("Unknown native wall");
        var wall=walls.get(index);
        if(installed==wall.hasDoor())return;
        int doorId=-1;
        if(installed){
            if(!wall.hasOpening())
                throw new IllegalArgumentException("Door needs a physical opening before installation");
            var occupied=new java.util.HashSet<Integer>();
            for(var portal:source.portals())occupied.add(portal.id());
            for(var other:walls)if(other.hasDoor())occupied.add(other.doorPortalId());
            doorId=1000;
            while(occupied.contains(doorId)&&doorId<=999999)doorId++;
            if(doorId>999999)throw new IllegalStateException("No available native door portal IDs");
        }
        var next=wall.withDoorPortal(doorId);
        var candidate=new ArrayList<>(walls);
        candidate.set(index,next);
        validateWalls(source,candidate,props);
        walls.set(index,next);revision++;
    }
    public synchronized void removeWall(String nodeId){
        if(nodeId==null||!nodeId.startsWith(WALL_PREFIX))
            throw new IllegalArgumentException("Expected stable structural wall node ID");
        String id=nodeId.substring(WALL_PREFIX.length());
        int index=-1;
        for(int i=0;i<walls.size();i++)if(walls.get(i).id().equals(id)){index=i;break;}
        if(index<0)throw new IllegalArgumentException("Unknown native structural wall");
        walls.remove(index);revision++;
    }
    private static void validateWalls(LevelDefinition source,List<MissionStructuralWall> walls,List<Prop> props){
        var prior=new ArrayList<MissionStructuralWall>();
        var occupied=new java.util.HashSet<Integer>();
        for(var portal:source.portals())occupied.add(portal.id());
        for(var wall:walls){
            wall.validate(source,prior);
            if(wall.hasDoor()&&!occupied.add(wall.doorPortalId()))
                throw new IllegalArgumentException("Duplicate native door portal ID "+wall.doorPortalId());
            for(var p:props)if(p.room()==wall.room()&&p.halfX()>0&&p.halfZ()>0
                &&Math.abs(p.y()-wall.base())<wall.height()+.15
                &&wall.near(p.x(),p.z(),Math.hypot(p.halfX(),p.halfZ())+.28))
                throw new IllegalArgumentException("Wall overlaps an authored source prop");
            prior.add(wall);
        }
    }
    public synchronized int indexOf(String id){
        int index=propIds.indexOf(Objects.requireNonNull(id));
        if(index<0)throw new IllegalArgumentException("Unknown prop ID: "+id);
        return index;
    }
    public synchronized void put(int index,Prop prop){
        checkProp(prop);HouseAuthoringAttachments.change(source,props.get(index),prop,propIds.get(index));props.set(index,prop);revision++;
    }
    public synchronized void add(Prop prop){
        checkProp(prop);
        if(props.size()>=4096)throw new IllegalStateException("Level object limit");
        props.add(prop);propIds.add("prop-"+java.util.UUID.randomUUID());revision++;
    }
    public synchronized void remove(int index){HouseAuthoringAttachments.change(source,props.get(index),null,propIds.get(index));props.remove(index);propIds.remove(index);revision++;}
    public synchronized long revision(){return revision;}
    public synchronized void validate(){
        snapshot();
        for(Prop prop:props)checkProp(prop);
        HouseAuthoringAttachments.validate(source,props);
        validateDoorWidths(source,doorWidths);
        validateWalls(source,walls,props);
        validateStairExtensions(source,stairExtensions);
        validateFloorDepths(source,floorDepths,stairExtensions);
    }
    /** Resize only an existing physical DOOR. Apertures, leaves and player
     * collision all consume the same LevelDefinition portal. */
    public synchronized void resizeDoor(int id,double width){
        if(HollowCreekLevel.is(source))throw new UnsupportedOperationException(
            "Native outdoor site structural door adapter is not implemented");
        var candidate=new java.util.LinkedHashMap<>(doorWidths);
        var original=source.portals().stream().filter(p->p.id()==id&&p.door()).findFirst()
            .orElseThrow(()->new IllegalArgumentException("Unknown native doorway "+id));
        if(!Double.isFinite(width))throw new IllegalArgumentException("Non-finite doorway width");
        if(Math.abs(width-original.width())<1e-10)candidate.remove(id);
        else candidate.put(id,width);
        validateDoorWidths(source,candidate);
        if(candidate.equals(doorWidths))return;
        doorWidths.clear();doorWidths.putAll(candidate);
        revision++;
    }
    private static void validateDoorWidths(LevelDefinition source,Map<Integer,Double> widths){
        for(var entry:widths.entrySet()){
            var p=source.portals().stream().filter(q->q.id()==entry.getKey()&&q.door()).findFirst()
                .orElseThrow(()->new IllegalArgumentException("Door width override lacks native portal "+entry.getKey()));
            double width=entry.getValue();
            if(!Double.isFinite(width)||width<.75||width>2.8)
                throw new IllegalArgumentException("Door width must be 0.75 to 2.8 metres");
            for(int roomId:new int[]{p.a(),p.b()}){
                var room=source.room(roomId);
                double center=p.alongX()?p.x():p.z(),lo=p.alongX()?room.x0():room.z0(),
                    hi=p.alongX()?room.x1():room.z1();
                if(center-width/2<lo+.12||center+width/2>hi-.12)
                    throw new IllegalArgumentException("Door cut intersects room corner "+roomId);
            }
            for(var other:source.portals()){
                if(other.id()==p.id()||other.kind().equals("stair")||
                    other.alongX()!=p.alongX()||Math.abs(other.y()-p.y())>.08||
                    Math.abs((p.alongX()?p.z():p.x())-(p.alongX()?other.z():other.x()))>.1)continue;
                double otherWidth=widths.getOrDefault(other.id(),other.width());
                double separation=Math.abs((p.alongX()?p.x():p.z())-(p.alongX()?other.x():other.z()));
                if(separation<(width+otherWidth)/2+.15)
                    throw new IllegalArgumentException("Door cut intersects adjacent aperture "+other.id());
            }
        }
    }
    private static List<Portal> effectivePortals(LevelDefinition source,Map<Integer,Double> widths){
        if(widths.isEmpty())return source.portals();
        var result=new ArrayList<Portal>();
        for(var p:source.portals()){
            double width=widths.getOrDefault(p.id(),p.width());
            result.add(width==p.width()?p:new Portal(p.id(),p.a(),p.b(),p.x(),p.y(),p.z(),width,p.alongX(),p.kind(),p.loss()));
        }
        return List.copyOf(result);
    }
    public synchronized void reopen()throws IOException{
        var loaded=loadRevision(document,documentId,source);restore(loaded.state());diskFingerprint=loaded.fingerprint();
    }
    public static void recoverSavedDocument(HouseMap.Site site)throws IOException{
        // A corrupt primary cannot select its architecture. The validated
        // recovery candidate carries that authority, including rebuilt House.
        var fallback=site==HouseMap.Site.HOUSE?LevelPlans.legacyHouseSource():LevelPlans.buildSource(site);
        recoverPreviousDocument(path(site),sceneId(site),fallback);
    }
    private static void recoverPreviousDocument(Path document,String documentId,LevelDefinition source)throws IOException{
        RuntimeSceneDocument.restorePrevious(document,path->{
            var candidate=loadState(path,documentId,source);
            var authority=documentId.equals("beckenburg/house")?HouseSourceVersions.source(candidate.architecture()):source;
            try{
                for(var prop:candidate.props())checkProp(prop,authority);
                HouseAuthoringAttachments.validate(authority,candidate.props());
            }catch(IllegalArgumentException invalid){throw new IOException("Invalid recovered native source",invalid);}
        });
    }
    public synchronized void recoverPrevious()throws IOException{recoverPreviousDocument(document,documentId,source);reopen();}
    private void checkProp(Prop p){
        checkProp(p,source);
    }
    private static void checkProp(Prop p,LevelDefinition source){
        Objects.requireNonNull(p);
        if(p.room()<0||p.room()>=source.rooms().size()
            ||p.mesh()==null||!GlbProps.availableModels().contains(p.mesh())
            ||!Double.isFinite(p.x()+p.y()+p.z()+p.scale()+p.yaw()
                +p.halfX()+p.halfZ()+p.height())
            ||p.scale()<=0||p.scale()>10||p.halfX()<0||p.halfZ()<0||p.height()<0)
            throw new IllegalArgumentException("Invalid mission prop");
    }
    public synchronized void resetToSource(){
        var state=sourceState(documentId,source.props());
        restore(new Snapshot(state.sceneId(),state.ids(),state.props(),builtInHouse?HouseSourceVersions.generation(source):null));
    }
    public synchronized boolean canRebuildHouse(){return builtInHouse;}
    public synchronized void rebuildHouse(){
        if(!builtInHouse)throw new IllegalArgumentException("House rebuild requires the built-in House document");
        if(!doorWidths.isEmpty()||!walls.isEmpty()||!floorDepths.isEmpty()||!stairExtensions.isEmpty())
            throw new IllegalStateException(
                "Reset native structural edits before changing the House architectural source");
        if(source.id().equals("briar-rebuild"))return;
        var next=HouseRebuildUpgrade.plan(snapshot());
        // Validate a candidate without changing live source, history, revision or disk.
        HouseRebuildUpgrade.validate(next);
        restore(next.state());
    }
    public synchronized LevelDefinition sourceLevel(){return source;}
    public synchronized Snapshot snapshot(){return new Snapshot(documentId,propIds,props,
        builtInHouse?HouseSourceVersions.generation(source):null,doorWidths,walls,
            floorDepths,stairExtensions);}
    public synchronized void restore(Snapshot snapshot){
        Objects.requireNonNull(snapshot);
        if(!documentId.equals(snapshot.sceneId()))throw new IllegalArgumentException("Snapshot belongs to another mission");
        if(builtInHouse&&snapshot.ids().contains(HouseSourceVersions.NODE))throw new IllegalArgumentException("Reserved House source metadata ID");
        var restoredSource=builtInHouse?HouseSourceVersions.source(snapshot.architecture()==null?UserLevelLibrary.HOUSE_ORIGINAL:snapshot.architecture()):source;
        for(Prop prop:snapshot.props())checkProp(prop,restoredSource);
        validateDoorWidths(restoredSource,snapshot.doorWidths());
        validateWalls(restoredSource,snapshot.walls(),snapshot.props());
        validateStairExtensions(restoredSource,snapshot.stairExtensions());
        validateFloorDepths(restoredSource,snapshot.floorDepths(),snapshot.stairExtensions());
        source=restoredSource;
        props.clear();props.addAll(snapshot.props());propIds.clear();propIds.addAll(snapshot.ids());
        doorWidths.clear();doorWidths.putAll(snapshot.doorWidths());
        walls.clear();walls.addAll(snapshot.walls());
        floorDepths.clear();floorDepths.putAll(snapshot.floorDepths());
        stairExtensions.clear();stairExtensions.putAll(snapshot.stairExtensions());
        revision++;
    }
    public synchronized LevelDefinition level(){
        return compose(source,props,doorWidths,walls,floorDepths,stairExtensions);
    }
    public synchronized List<Node> hierarchy(){
        ArrayList<Node> result=new ArrayList<>();
        for(int i=0;i<props.size();i++) result.add(propNode(i));
        for(var wall:walls)result.add(wallNode(wall));
        for(var entry:new java.util.TreeMap<>(floorDepths).entrySet())
            result.add(floorNode(entry.getKey(),entry.getValue()));
        for(var entry:new java.util.TreeMap<>(stairExtensions).entrySet())
            result.add(stairNode(entry.getKey(),entry.getValue()));
        return List.copyOf(result);
    }
    public synchronized Node node(String id) {
        for(var wall:walls)if((WALL_PREFIX+wall.id()).equals(id))return wallNode(wall);
        for(var entry:floorDepths.entrySet())if((FLOOR_PREFIX+entry.getKey()).equals(id))
            return floorNode(entry.getKey(),entry.getValue());
        for(var entry:stairExtensions.entrySet())if((STAIR_PREFIX+entry.getKey()).equals(id))
            return stairNode(entry.getKey(),entry.getValue());
        return propNode(indexOf(id));
    }
    public synchronized HierarchyPage hierarchyPage(int offset,int limit) {
        var all=hierarchy();
        int end=HierarchyPage.end(all.size(),offset,limit);
        return new HierarchyPage(all.size(),offset,all.subList(offset,end));
    }
    private Node stairNode(int id,double extension){
        var s=stairById(source,id);
        return new Node(STAIR_PREFIX+id,"STAIR_OPENING",
            "Stair terminal opening "+extension+"m / upper room "+s.b(),
            s.x1(),s.y1(),s.z1());
    }
    private Node floorNode(int room,double depth){
        var r=source.room(room);
        return new Node(FLOOR_PREFIX+room,"FLOOR_SLAB",
            "Native upper deck depth "+depth+"m / room "+room,
            (r.x0()+r.x1())/2,r.y(),(r.z0()+r.z1())/2);
    }
    private static Node wallNode(MissionStructuralWall wall){
        return new Node(WALL_PREFIX+wall.id(),"WALL","Native solid partition / room "+wall.room(),
            wall.cx(),wall.base(),wall.cz());
    }
    private Node propNode(int index) {
        var prop = props.get(index);
        return new Node(propIds.get(index),"PROP",prop.mesh()+" / room "+prop.room(),prop.x(),prop.y(),prop.z());
    }
    public List<Asset> assets(){
        ArrayList<Asset> result=new ArrayList<>();
        for(String mesh:GlbProps.availableModels())
            result.add(new Asset("assets://models/"+mesh,"Models",mesh));
        return List.copyOf(result);
    }
    public synchronized void save() throws IOException {
        validate();
        if(builtInHouse&&source.id().equals("briar-rebuild")&&diskFingerprint!=null&&Files.exists(document)){
            byte[] old=org.beckenburg.editor.EditorDocumentStore.readDocument(document);
            if(diskFingerprint.equals(org.beckenburg.editor.EditorDocumentStore.fingerprint(old))
                &&HouseSourceVersions.generation(RuntimeSceneDocument.load(document)).equals(UserLevelLibrary.HOUSE_ORIGINAL)){
                Path backup=document.resolveSibling(document.getFileName()+".before-first-principles.bscn");
                if(!Files.exists(backup))Files.write(backup,old,java.nio.file.StandardOpenOption.CREATE_NEW);
            }
        }
        diskFingerprint=RuntimeSceneDocument.saveIfUnchanged(documentPath(),sceneDocument(snapshot()),diskFingerprint);
    }
    static void saveProps(Path destination,String id,List<Prop> source) throws IOException {
        saveState(destination,sourceState(id,source));
    }
    private static Snapshot sourceState(String id,List<Prop> props){
        var ids=new ArrayList<String>();for(int i=0;i<props.size();i++)ids.add("prop-"+i);
        return new Snapshot(id,ids,props);
    }
    private static void saveState(Path destination,Snapshot state)throws IOException{RuntimeSceneDocument.save(destination,sceneDocument(state));}
    private static InMemoryRuntimeScene sceneDocument(Snapshot state){
        InMemoryRuntimeScene document=new InMemoryRuntimeScene(state.sceneId());
        for(int i=0;i<state.props().size();i++){
            Prop p=state.props().get(i);
            document.replace(new RuntimeScene.Node(state.ids().get(i),p.mesh(),null,"world",
                new RuntimeScene.Transform(p.x(),p.y(),p.z(),0,
                    Math.toDegrees(p.yaw()),0,p.scale(),p.scale(),p.scale()),
                List.of(new RuntimeScene.Component(COMPONENT,Map.of(
                    "mesh",p.mesh(),"room",Integer.toString(p.room()),
                    "yawRad",Double.toString(p.yaw()),
                    "halfX",Double.toString(p.halfX()),
                    "halfZ",Double.toString(p.halfZ()),
                    "height",Double.toString(p.height()),
                    "support",p.support())))));
        }
        if(state.architecture()!=null)document.replace(HouseSourceVersions.node(state.architecture()));
        for(var entry:new java.util.TreeMap<>(state.doorWidths()).entrySet()){
            String portalId=PORTAL_PREFIX+entry.getKey();
            document.replace(new RuntimeScene.Node(portalId,"Native door width",null,"metadata",
                RuntimeScene.Transform.identity(),List.of(new RuntimeScene.Component(PORTAL_COMPONENT,
                    Map.of("portalId",Integer.toString(entry.getKey()),
                        "width",Double.toString(entry.getValue()))))));
        }
        for(var entry:new java.util.TreeMap<>(state.stairExtensions()).entrySet()){
            document.replace(new RuntimeScene.Node(STAIR_PREFIX+entry.getKey(),
                "Native stair landing opening",null,"architecture",RuntimeScene.Transform.identity(),
                List.of(new RuntimeScene.Component(STAIR_COMPONENT,Map.of(
                    "stairId",Integer.toString(entry.getKey()),
                    "extension",Double.toString(entry.getValue()))))));
        }
        for(var entry:new java.util.TreeMap<>(state.floorDepths()).entrySet()){
            document.replace(new RuntimeScene.Node(FLOOR_PREFIX+entry.getKey(),
                "Native floor depth",null,"architecture",RuntimeScene.Transform.identity(),
                List.of(new RuntimeScene.Component(FLOOR_COMPONENT,Map.of(
                    "room",Integer.toString(entry.getKey()),
                    "depth",Double.toString(entry.getValue()))))));
        }
        for(var wall:state.walls()){
            var fields=new java.util.LinkedHashMap<String,String>(Map.of(
                "room",Integer.toString(wall.room()),
                "x0",Double.toString(wall.x0()),"z0",Double.toString(wall.z0()),
                "x1",Double.toString(wall.x1()),"z1",Double.toString(wall.z1()),
                "base",Double.toString(wall.base()),"height",Double.toString(wall.height()),
                "thickness",Double.toString(wall.thickness()),"material",Integer.toString(wall.material())));
            if(wall.hasOpening()){
                fields.put("cutOffset",Double.toString(wall.cutOffset()));
                fields.put("cutWidth",Double.toString(wall.cutWidth()));
                fields.put("cutHeight",Double.toString(wall.cutHeight()));
            }
            if(wall.hasDoor())
                fields.put("doorPortalId",Integer.toString(wall.doorPortalId()));
            document.replace(new RuntimeScene.Node(WALL_PREFIX+wall.id(),"Native structural wall",
                null,"architecture",RuntimeScene.Transform.identity(),
                List.of(new RuntimeScene.Component(MissionStructuralWall.COMPONENT,fields))));
        }
        return document;
    }
    private record Loaded(Snapshot state,String fingerprint){}
    private static Snapshot loadState(Path path,String id,LevelDefinition source)throws IOException{return loadRevision(path,id,source).state();}
    private static Loaded loadRevision(Path path,String id,LevelDefinition source) throws IOException {
        RuntimeSceneDocument.recoverPending(path);
        if(!Files.exists(path)){
            var state=sourceState(id,source.props());
            return new Loaded(new Snapshot(id,state.ids(),state.props(),id.equals("beckenburg/house")?HouseSourceVersions.generation(source):null),null);
        }
        return readStored(path,id,source);
    }
    synchronized boolean matchesSaved(Snapshot live)throws IOException{
        return live.equals(readStored(document,documentId,source).state());
    }
    private static Loaded readStored(Path path,String id,LevelDefinition source)throws IOException{
        var loaded=RuntimeSceneDocument.readStoredRevision(path);var scene=loaded.scene();
        if(!scene.sceneId().equals(id))
            throw new IOException("Level document identity mismatch: "+path);
        ArrayList<Prop> result=new ArrayList<>();
        ArrayList<String> ids=new ArrayList<>();
        var widths=new java.util.LinkedHashMap<Integer,Double>();
        var walls=new ArrayList<MissionStructuralWall>();
        var floorDepths=new java.util.LinkedHashMap<Integer,Double>();
        var stairExtensions=new java.util.LinkedHashMap<Integer,Double>();
        for(var node:scene.nodes()){
            if(node.id().equals(HouseSourceVersions.NODE)&&id.equals("beckenburg/house"))continue;
            if(node.id().startsWith(STAIR_PREFIX)){
                try{
                    if(node.components().size()!=1||!node.components().get(0).type().equals(STAIR_COMPONENT)||
                        !node.name().equals("Native stair landing opening")||
                        !node.layer().equals("architecture")||node.parentId()!=null||
                        !node.transform().equals(RuntimeScene.Transform.identity()))
                        throw new IllegalArgumentException("Unsupported stair landing component");
                    var fields=node.components().get(0).properties();
                    if(!fields.keySet().equals(java.util.Set.of("stairId","extension")))
                        throw new IllegalArgumentException("Unknown stair landing keys");
                    int stairId=Integer.parseInt(fields.get("stairId"));
                    double extension=Double.parseDouble(fields.get("extension"));
                    if(!node.id().equals(STAIR_PREFIX+stairId)||
                        stairExtensions.putIfAbsent(stairId,extension)!=null)
                        throw new IllegalArgumentException("Contradictory stair landing identity");
                }catch(RuntimeException invalid){
                    throw new IOException("Invalid native stair landing "+node.id(),invalid);
                }
                continue;
            }
            if(node.id().startsWith(FLOOR_PREFIX)){
                try{
                    if(node.components().size()!=1||
                        !node.components().get(0).type().equals(FLOOR_COMPONENT)||
                        !node.name().equals("Native floor depth")||
                        !node.layer().equals("architecture")||node.parentId()!=null||
                        !node.transform().equals(RuntimeScene.Transform.identity()))
                        throw new IllegalArgumentException("Unsupported native floor node");
                    var fields=node.components().get(0).properties();
                    if(!fields.keySet().equals(java.util.Set.of("room","depth")))
                        throw new IllegalArgumentException("Unsupported floor-depth properties");
                    int room=Integer.parseInt(fields.get("room"));
                    double depth=Double.parseDouble(fields.get("depth"));
                    if(!node.id().equals(FLOOR_PREFIX+room)||
                        floorDepths.putIfAbsent(room,depth)!=null)
                        throw new IllegalArgumentException("Duplicate/contradictory floor ID");
                }catch(RuntimeException bad){
                    throw new IOException("Invalid native floor-depth "+node.id(),bad);
                }
                continue;
            }
            if(node.id().startsWith(WALL_PREFIX)){
                try{
                    if(node.components().size()!=1||
                        !node.components().get(0).type().equals(MissionStructuralWall.COMPONENT)
                        ||!node.name().equals("Native structural wall")
                        ||!node.layer().equals("architecture")||node.parentId()!=null||
                        !node.transform().equals(RuntimeScene.Transform.identity()))
                        throw new IllegalArgumentException("Unsupported native structural wall record");
                    var props=node.components().get(0).properties();
                    var original=java.util.Set.of("room","x0","z0","x1","z1",
                        "base","height","thickness","material");
                    var apertured=new java.util.HashSet<>(original);
                    apertured.addAll(java.util.Set.of("cutOffset","cutWidth","cutHeight"));
                    var withDoor=new java.util.HashSet<>(apertured);
                    withDoor.add("doorPortalId");
                    boolean hasDoor=props.keySet().equals(withDoor);
                    boolean hasCut=hasDoor||props.keySet().equals(apertured);
                    if(!hasCut&&!props.keySet().equals(original))
                        throw new IllegalArgumentException("Unsupported structural wall properties");
                    var wall=new MissionStructuralWall(node.id().substring(WALL_PREFIX.length()),
                        Integer.parseInt(props.get("room")),Double.parseDouble(props.get("x0")),
                        Double.parseDouble(props.get("z0")),Double.parseDouble(props.get("x1")),
                        Double.parseDouble(props.get("z1")),Double.parseDouble(props.get("base")),
                        Double.parseDouble(props.get("height")),Double.parseDouble(props.get("thickness")),
                        Integer.parseInt(props.get("material")),
                        hasCut?Double.parseDouble(props.get("cutOffset")):0,
                        hasCut?Double.parseDouble(props.get("cutWidth")):0,
                        hasCut?Double.parseDouble(props.get("cutHeight")):0,
                        hasDoor?Integer.parseInt(props.get("doorPortalId")):-1);
                    walls.add(wall);
                }catch(RuntimeException bad){
                    throw new IOException("Invalid native structural wall "+node.id(),bad);
                }
                continue;
            }
            if(node.id().startsWith(PORTAL_PREFIX)){
                try{
                    if(node.components().size()!=1||!node.components().get(0).type().equals(PORTAL_COMPONENT)
                        ||!node.name().equals("Native door width")||!node.layer().equals("metadata")
                        ||node.parentId()!=null||!node.transform().equals(RuntimeScene.Transform.identity()))
                        throw new IllegalArgumentException("Unsupported native door node shape");
                    var data=node.components().get(0).properties();
                    if(!data.keySet().equals(java.util.Set.of("portalId","width")))
                        throw new IllegalArgumentException("Invalid door metadata keys");
                    int portalId=Integer.parseInt(data.get("portalId"));
                    if(!node.id().equals(PORTAL_PREFIX+portalId)||
                        widths.putIfAbsent(portalId,Double.parseDouble(data.get("width")))!=null)
                        throw new IllegalArgumentException("Duplicate or contradictory door identity");
                }catch(RuntimeException malformed){
                    throw new IOException("Invalid native door width "+node.id(),malformed);
                }
                continue;
            }
            if(node.components().size()!=1
                ||!node.components().get(0).type().equals(COMPONENT))
                throw new IOException("Unsupported level node "+node.id());
            try{
                var data=node.components().get(0).properties();
                var required=java.util.Set.of("mesh","room","halfX","halfZ","height","support");
                var allowed=java.util.Set.of("mesh","room","halfX","halfZ","height","support","yawRad");
                if(!data.keySet().containsAll(required)||!allowed.containsAll(data.keySet()))
                    throw new IllegalArgumentException("Unsupported native prop properties");
                if(!node.name().equals(data.get("mesh"))||!node.layer().equals("world"))
                    throw new IllegalArgumentException("Native mission cannot preserve custom node labels/layers");
                var t=node.transform();
                if(data.containsKey("yawRad")){
                    double degrees=Math.toDegrees(Double.parseDouble(data.get("yawRad")));
                    if(!Double.isFinite(degrees)||Math.abs(degrees-t.yaw())>Math.max(1e-10,Math.ulp(t.yaw())*4))
                        throw new IllegalArgumentException("Contradictory native and portable yaw values");
                }
                if(node.parentId()!=null||t.pitch()!=0||t.roll()!=0
                    ||t.scaleX()!=t.scaleY()||t.scaleX()!=t.scaleZ())
                    throw new IllegalArgumentException("Mission runtime supports root props with yaw and uniform scale only");
                Prop prop=new Prop(data.get("mesh"),Integer.parseInt(data.get("room")),
                    t.x(),t.y(),t.z(),t.scaleX(),
                    data.containsKey("yawRad")?Double.parseDouble(data.get("yawRad"))
                        :Math.toRadians(t.yaw()),
                    Double.parseDouble(data.get("halfX")),
                    Double.parseDouble(data.get("halfZ")),
                    Double.parseDouble(data.get("height")),data.get("support"));
                checkProp(prop,source);
                result.add(prop);ids.add(node.id());
            }catch(RuntimeException invalid){
                throw new IOException("Invalid level object "+node.id()+" in "+path,invalid);
            }
        }
        var architecture=id.equals("beckenburg/house")?HouseSourceVersions.generation(scene):null;
        var authority=architecture==null?source:HouseSourceVersions.source(architecture);
        try{
            validateDoorWidths(authority,widths);
            validateWalls(authority,walls,result);
            validateStairExtensions(authority,stairExtensions);
            validateFloorDepths(authority,floorDepths,stairExtensions);
        }catch(IllegalArgumentException invalid){
            throw new IOException("Native structural metadata invalid",invalid);
        }
        return new Loaded(new Snapshot(id,ids,result,architecture,widths,walls,
            floorDepths,stairExtensions),loaded.fingerprint());
    }
    static LevelDefinition apply(HouseMap.Site site,LevelDefinition source){
        try{
            RuntimeSceneDocument.recoverPending(path(site));
            if(!Files.exists(path(site)))return source;
            var state=loadState(path(site),sceneId(site),source);
            var authority=site==HouseMap.Site.HOUSE?HouseSourceVersions.source(state.architecture()):source;
            return compose(authority,state.props(),state.doorWidths(),state.walls(),
                state.floorDepths(),state.stairExtensions());
        }
        catch(IOException bad){
            throw new IllegalStateException("Cannot load authored "+site+" level",bad);
        }
    }
    /**
     * The authored props replace their native source entries, and their
     * matching physical collider bodies are rebuilt from the SAME placement
     * helper used by LevelPlans during initial map construction.
     */
    static LevelDefinition compose(LevelDefinition source,List<Prop> edited){
        return compose(source,edited,Map.of());
    }
    static LevelDefinition compose(LevelDefinition source,List<Prop> edited,Map<Integer,Double> widths){
        return compose(source,edited,widths,List.of());
    }
    static LevelDefinition compose(LevelDefinition source,List<Prop> edited,Map<Integer,Double> widths,
                                   List<MissionStructuralWall> walls){
        return compose(source,edited,widths,walls,Map.of());
    }
    static LevelDefinition compose(LevelDefinition source,List<Prop> edited,Map<Integer,Double> widths,
                                   List<MissionStructuralWall> walls,Map<Integer,Double> floorDepths){
        return compose(source,edited,widths,walls,floorDepths,Map.of());
    }
    static LevelDefinition compose(LevelDefinition source,List<Prop> edited,Map<Integer,Double> widths,
                                   List<MissionStructuralWall> walls,Map<Integer,Double> floorDepths,
                                   Map<Integer,Double> stairExtensions){
        HouseAuthoringAttachments.validate(source,edited);
        validateDoorWidths(source,widths);
        validateWalls(source,walls,edited);
        validateStairExtensions(source,stairExtensions);
        validateFloorDepths(source,floorDepths,stairExtensions);
        if(source.props().equals(edited)&&widths.isEmpty()&&walls.isEmpty()&&floorDepths.isEmpty()
            &&stairExtensions.isEmpty())return source;
        ArrayList<Collider> collision=new ArrayList<>(source.colliders());
        var added=new ArrayList<>(edited);
        for(Prop p:source.props()){
            // Preserve exact source bodies for unchanged objects, including duplicates.
            if(added.remove(p))continue;
            for(var body:LevelPlans.propColliders(source,p)){
                for(int i=0;i<collision.size();i++)if(LevelPlans.sameBody(collision.get(i),body)){
                    collision.remove(i);break;
                }
            }
        }
        for(Prop p:added)collision.addAll(LevelPlans.propColliders(source,p));
        var sockets=new ArrayList<>(effectiveFloorSockets(source,floorDepths));
        var portals=new ArrayList<>(effectivePortals(source,widths));
        for(var wall:walls){
            collision.addAll(wall.bodies());sockets.add(wall.socket());
            if(wall.hasDoor())portals.add(wall.doorPortal());
        }
        return new LevelDefinition(source.id(),source.revision(),source.title(),
            source.rooms(),portals,source.stairs(),effectiveVoids(source,stairExtensions),
            List.copyOf(edited),sockets,collision,source.dock(),
            source.ambience(),source.water(),source.sounds());
    }
}
