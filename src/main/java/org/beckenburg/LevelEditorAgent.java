package org.beckenburg;

import org.beckenburg.editor.EditorAgentService;
import org.beckenburg.editor.EditorChange;
import org.beckenburg.LevelDefinition.Prop;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import javax.imageio.ImageIO;

/** Mission adapter: reads/writes the active authoring session; the supplied
 * HouseMap is the same native game map used by the human editor viewport.
 */
public final class LevelEditorAgent implements EditorAgentService.Host<LevelAuthoringSession.Snapshot>,EditorAgentService.WalkingCollision,EditorAgentService.PropertySource,EditorAgentService.NodeFocus {
    private final LevelAuthoringSession session;
    private final Supplier<HouseMap> map;
    private final SceneRenderer renderer;
    private final Runnable access;
    public LevelEditorAgent(LevelAuthoringSession session, Supplier<HouseMap> map,
                            SceneRenderer renderer, Runnable access){
        this.session=java.util.Objects.requireNonNull(session);
        this.map=java.util.Objects.requireNonNull(map);
        this.renderer=java.util.Objects.requireNonNull(renderer);
        this.access=java.util.Objects.requireNonNull(access);
    }
    /** Aim the authoritative renderer from a traversable room point, without editing geometry. */
    public void focus(org.beckenburg.editor.EditorProject.Node node){
        int roomId=node.kind().equals("STAIR_OPENING")
            ?session.sourceLevel().stairs().stream()
                .filter(q->("beckenburg-stair-landing-"+q.id()).equals(node.id()))
                .findFirst().orElseThrow().b()
            :node.kind().equals("FLOOR_SLAB")
                ?Integer.parseInt(node.id().substring("beckenburg-floor-depth-".length()))
            :node.kind().equals("WALL")?session.walls().stream()
                .filter(w->(MissionStructuralWall.NODE_PREFIX+w.id()).equals(node.id()))
                .findFirst().orElseThrow().room()
            :session.prop(session.indexOf(node.id())).room();
        var room=session.level().room(roomId);
        var spawn=room.spawn();
        double dx=node.x()-spawn.x(), dz=node.z()-spawn.z();
        double length=Math.hypot(dx,dz);
        double fraction=length>2.6?(length-2.6)/length:0;
        double px=spawn.x()+dx*fraction, pz=spawn.z()+dz*fraction;
        if(!map.get().traversable(px,spawn.y(),pz)){
            px=spawn.x();pz=spawn.z();
        }
        renderer.x=px;renderer.z=pz;renderer.eyeHeight=spawn.y()+1.62;
        renderer.yaw=Math.atan2(node.x()-px,node.z()-pz);
        renderer.pitch=-.12;
    }
    public LevelAuthoringSession project(){return session;}
    public long revision(){return session.revision();}
    public boolean walkable(double x,double y,double z){return map.get().traversable(x,y,z);}
    public Map<String,Object> properties(org.beckenburg.editor.EditorProject.Node node){
        if(node.kind().equals("STAIR_OPENING")){
            int id=Integer.parseInt(node.id().substring("beckenburg-stair-landing-".length()));
            var stair=session.sourceLevel().stairs().stream().filter(q->q.id()==id)
                .findFirst().orElseThrow();
            return Map.of("stairId",id,
                "extension",session.stairExtensions().getOrDefault(id,0.0),
                "upperRoom",stair.b(),"topY",stair.y1(),
                "terminalX",stair.x1(),"terminalZ",stair.z1());
        }
        if(node.kind().equals("FLOOR_SLAB")){
            int room=Integer.parseInt(node.id().substring("beckenburg-floor-depth-".length()));
            var r=session.sourceLevel().room(room);
            double depth=session.floorDepths().getOrDefault(room,0.0);
            return Map.of("room",room,"depth",depth,"top",r.y(),
                "underside",r.y()-depth,"x0",r.x0(),"z0",r.z0(),"x1",r.x1(),"z1",r.z1());
        }
        if(node.kind().equals("WALL")){
            var wall=session.walls().stream().filter(w->
                (MissionStructuralWall.NODE_PREFIX+w.id()).equals(node.id())).findFirst().orElseThrow();
            var fields=new java.util.LinkedHashMap<String,Object>(Map.of(
                "room",wall.room(),"x0",wall.x0(),"z0",wall.z0(),
                "x1",wall.x1(),"z1",wall.z1(),"base",wall.base(),
                "height",wall.height(),"thickness",wall.thickness(),"material",wall.material()));
            fields.put("openingOffset",wall.cutOffset());
            fields.put("openingWidth",wall.cutWidth());
            fields.put("openingClearHeight",wall.cutHeight());
            return Map.copyOf(fields);
        }
        var p=session.prop(session.indexOf(node.id()));
        var fields=new java.util.LinkedHashMap<String,Object>(Map.of("mesh",p.mesh(),"room",p.room(),"scale",p.scale(),
            "yawDegrees",Math.toDegrees(p.yaw()),"collisionX",p.halfX(),"collisionZ",p.halfZ(),"height",p.height()));
        if(p.support()!=null)fields.put("support",p.support());
        return fields;
    }
    public EditorAgentService.DiskState compareSaved(LevelAuthoringSession.Snapshot live)throws IOException{
        return session.matchesSaved(live)?EditorAgentService.DiskState.MATCHES:EditorAgentService.DiskState.DIFFERS;
    }
    public void checkAccess(){access.run();}
    public java.util.Set<EditorAgentService.Capability> capabilities(){return java.util.Set.of(EditorAgentService.Capability.values());}
    /**
     * Mission command parity: the human level editor already edits these exact
     * LevelAuthoringSession props. This typed interface admits only loose floor
     * objects for new placements/duplicates; protected source assemblies cannot
     * be torn apart by an untrusted planner.
     */
    public List<String> operations(){
        var ops=new java.util.ArrayList<>(List.of("move-prop","place-prop","transform-prop",
            "duplicate-prop","remove-prop"));
        if(!HollowCreekLevel.is(session.sourceLevel())){
            ops.add("resize-door");
            ops.add("place-wall");
            ops.add("extend-wall");
            ops.add("set-wall-opening");
            ops.add("remove-wall");
            ops.add("set-floor-depth");
            ops.add("extend-stair-opening");
        }
        if(session.canRebuildHouse())ops.add("rebuild-house");
        return List.copyOf(ops);
    }
    private static final Map<String,String> MOVE_FIELDS=Map.of("id","string","x","number","y","number","z","number");
    private static final Map<String,String> PLACE_FIELDS=Map.of("mesh","string","room","integer",
        "x","number","y","number","z","number","scale","number","yawDegrees","number");
    private static final Map<String,String> TRANSFORM_FIELDS=Map.of("id","string","x","number",
        "y","number","z","number","scale","number","yawDegrees","number");
    private static final Map<String,String> DUPLICATE_FIELDS=MOVE_FIELDS;
    private static final Map<String,String> REMOVE_FIELDS=Map.of("id","string");
    private static final Map<String,String> DOOR_FIELDS=Map.of("id","integer","width","number");
    private static final Map<String,String> OPEN_WALL_FIELDS=Map.of(
        "id","string","offset","number","width","number","clearHeight","number");
    private static final Map<String,String> EXTEND_WALL_FIELDS=Map.of("id","string","anchor","string",
        "endpoint","string","turn","string","length","number");
    private static final Map<String,String> FLOOR_FIELDS=Map.of("room","integer","depth","number");
    private static final Map<String,String> STAIR_FIELDS=Map.of("stairId","integer","extension","number");
    private static final Map<String,String> WALL_FIELDS=Map.ofEntries(
        Map.entry("id","string"),Map.entry("room","integer"),Map.entry("x0","number"),
        Map.entry("z0","number"),Map.entry("x1","number"),Map.entry("z1","number"),
        Map.entry("base","number"),Map.entry("height","number"),Map.entry("thickness","number"),
        Map.entry("material","integer"));
    public Map<String,Map<String,String>> operationParameters(){
        var result=new java.util.LinkedHashMap<String,Map<String,String>>();
        result.put("move-prop",MOVE_FIELDS);
        result.put("place-prop",PLACE_FIELDS);
        result.put("transform-prop",TRANSFORM_FIELDS);
        result.put("duplicate-prop",DUPLICATE_FIELDS);
        result.put("remove-prop",REMOVE_FIELDS);
        if(!HollowCreekLevel.is(session.sourceLevel())){
            result.put("resize-door",DOOR_FIELDS);
            result.put("place-wall",WALL_FIELDS);
            result.put("extend-wall",EXTEND_WALL_FIELDS);
            result.put("set-wall-opening",OPEN_WALL_FIELDS);
            result.put("remove-wall",REMOVE_FIELDS);
            result.put("set-floor-depth",FLOOR_FIELDS);
            result.put("extend-stair-opening",STAIR_FIELDS);
        }
        if(session.canRebuildHouse())result.put("rebuild-house",Map.of("baseline","ORIGINAL"));
        return Map.copyOf(result);
    }
    private static double number(Map<String,String> args,String name){
        double value=Double.parseDouble(args.get(name));
        if(!Double.isFinite(value))throw new IllegalArgumentException("Non-finite mission "+name);
        return value;
    }
    private static double yaw(Map<String,String> args){
        double degrees=number(args,"yawDegrees");
        if(Math.abs(degrees)>3600)throw new IllegalArgumentException("Mission yaw exceeds ten turns");
        return Math.toRadians(degrees);
    }
    private static void floorOnly(Prop prop){
        if(!"floor".equals(prop.support()))throw new IllegalArgumentException(
            "Only loose floor-supported props may use this operation");
    }
    private void safeFloorPlacement(Prop prop,String ignoreId,boolean newObject){
        floorOnly(prop);
        var level=session.sourceLevel();
        if(prop.room()<0||prop.room()>=level.rooms().size())
            throw new IllegalArgumentException("Unknown mission room");
        var room=level.room(prop.room());
        if(!room.contains(prop.x(),prop.y(),prop.z())||Math.abs(prop.y()-room.y())>.02)
            throw new IllegalArgumentException("Floor prop must belong to a physical room at its standing height");
        double c=Math.abs(Math.cos(prop.yaw())),s=Math.abs(Math.sin(prop.yaw()));
        double rx=c*prop.halfX()+s*prop.halfZ(),rz=s*prop.halfX()+c*prop.halfZ();
        double edge=.08;
        if(prop.x()-rx<room.x0()+edge||prop.x()+rx>room.x1()-edge||
            prop.z()-rz<room.z0()+edge||prop.z()+rz>room.z1()-edge)
            throw new IllegalArgumentException("Prop footprint crosses its assigned room boundary");
        for(int i=0;i<session.size();i++){
            if(ignoreId!=null&&ignoreId.equals(session.nodeId(i)))continue;
            var other=session.prop(i);
            if(other.room()!=prop.room()||other.halfX()<=0||other.halfZ()<=0)continue;
            double oc=Math.abs(Math.cos(other.yaw())),os=Math.abs(Math.sin(other.yaw()));
            double ox=oc*other.halfX()+os*other.halfZ(),oz=os*other.halfX()+oc*other.halfZ();
            if(prop.x()-rx<other.x()+ox&&prop.x()+rx>other.x()-ox
                &&prop.z()-rz<other.z()+oz&&prop.z()+rz>other.z()-oz
                &&prop.y()<other.y()+other.height()&&other.y()<prop.y()+prop.height())
                throw new IllegalArgumentException("Prop footprint intersects another native object");
        }
        if(newObject&&!map.get().traversable(prop.x(),prop.y(),prop.z()))
            throw new IllegalArgumentException("Floor prop has no clear native standing position");
    }
    private static boolean approvedLooseMesh(LevelDefinition level,String mesh){
        if(mesh==null||!GlbProps.availableModels().contains(mesh))return false;
        // Do not create apparent stand-alone copies of authored service devices.
        return level.props().stream().noneMatch(p->p.mesh().equals(mesh)
            &&HouseAuthoringAttachments.required(level,p));
    }
    private Prop looseProp(String mesh,int room,double x,double y,double z,double scale,double yaw){
        if(!approvedLooseMesh(session.sourceLevel(),mesh))
            throw new IllegalArgumentException("Unapproved or source-attached mission mesh");
        if(!Double.isFinite(scale)||scale<=0||scale>10)throw new IllegalArgumentException("Invalid prop scale");
        double[] bounds=SceneRenderer.castleBounds(mesh);
        var prop=new Prop(mesh,room,x,y,z,scale,yaw,
            Math.max(0,bounds[3]*scale/2),Math.max(0,bounds[5]*scale/2),
            Math.max(0,bounds[4]*scale),"floor");
        safeFloorPlacement(prop,null,true);
        return prop;
    }
    public void apply(String operation,Map<String,String> args){
        if(operation.equals("rebuild-house")){
            if(!args.equals(Map.of("baseline","ORIGINAL")))throw new IllegalArgumentException("Expected rebuild-house baseline ORIGINAL");
            session.rebuildHouse();return;
        }
        switch(operation){
            case "move-prop" -> {
                if(!args.keySet().equals(MOVE_FIELDS.keySet()))throw new IllegalArgumentException("Expected id, x, y, z");
                int index=session.indexOf(args.get("id"));
                var p=session.prop(index);
                session.put(index,new Prop(p.mesh(),p.room(),number(args,"x"),number(args,"y"),number(args,"z"),
                    p.scale(),p.yaw(),p.halfX(),p.halfZ(),p.height(),p.support()));
            }
            case "place-prop" -> {
                if(!args.keySet().equals(PLACE_FIELDS.keySet()))throw new IllegalArgumentException("Expected native place-prop fields");
                var prop=looseProp(args.get("mesh"),Integer.parseInt(args.get("room")),
                    number(args,"x"),number(args,"y"),number(args,"z"),number(args,"scale"),yaw(args));
                session.add(prop);
            }
            case "transform-prop" -> {
                if(!args.keySet().equals(TRANSFORM_FIELDS.keySet()))throw new IllegalArgumentException("Expected native transform-prop fields");
                int index=session.indexOf(args.get("id"));var old=session.prop(index);floorOnly(old);
                double scale=number(args,"scale"),ratio=scale/old.scale();
                var candidate=new Prop(old.mesh(),old.room(),number(args,"x"),number(args,"y"),number(args,"z"),
                    scale,yaw(args),old.halfX()*ratio,old.halfZ()*ratio,old.height()*ratio,old.support());
                safeFloorPlacement(candidate,args.get("id"),false);
                session.put(index,candidate);
            }
            case "duplicate-prop" -> {
                if(!args.keySet().equals(DUPLICATE_FIELDS.keySet()))throw new IllegalArgumentException("Expected id, x, y, z");
                var old=session.prop(session.indexOf(args.get("id")));floorOnly(old);
                if(HouseAuthoringAttachments.required(session.sourceLevel(),old))
                    throw new IllegalArgumentException("Cannot duplicate an authored service/story assembly");
                var duplicate=new Prop(old.mesh(),old.room(),number(args,"x"),number(args,"y"),number(args,"z"),
                    old.scale(),old.yaw(),old.halfX(),old.halfZ(),old.height(),old.support());
                safeFloorPlacement(duplicate,null,true);
                session.add(duplicate);
            }
            case "remove-prop" -> {
                if(!args.keySet().equals(REMOVE_FIELDS.keySet()))throw new IllegalArgumentException("Expected exact native prop id");
                session.remove(session.indexOf(args.get("id")));
            }
            case "resize-door" -> {
                if(!args.keySet().equals(DOOR_FIELDS.keySet()))
                    throw new IllegalArgumentException("Expected native portal id and clear opening width");
                session.resizeDoor(Integer.parseInt(args.get("id")),number(args,"width"));
            }
            case "place-wall" -> {
                if(!args.keySet().equals(WALL_FIELDS.keySet()))
                    throw new IllegalArgumentException("Expected bounded native partition geometry");
                session.placeWall(new MissionStructuralWall(args.get("id"),
                    Integer.parseInt(args.get("room")),number(args,"x0"),number(args,"z0"),
                    number(args,"x1"),number(args,"z1"),number(args,"base"),
                    number(args,"height"),number(args,"thickness"),Integer.parseInt(args.get("material"))));
            }
            case "extend-wall" -> {
                if(!args.keySet().equals(EXTEND_WALL_FIELDS.keySet()))
                    throw new IllegalArgumentException("Expected new wall ID, existing wall anchor, START/END, LEFT/RIGHT/STRAIGHT and length");
                var parent=session.walls().stream().filter(w->
                    (MissionStructuralWall.NODE_PREFIX+w.id()).equals(args.get("anchor")))
                    .findFirst().orElseThrow(()->new IllegalArgumentException("Unknown native wall anchor"));
                boolean start=switch(args.get("endpoint")){
                    case "START" -> true;
                    case "END" -> false;
                    default -> throw new IllegalArgumentException("Endpoint must be START or END");
                };
                double length=number(args,"length");
                if(length<.75||length>4)
                    throw new IllegalArgumentException("Wall extension must be 0.75..4m");
                double dx=parent.x1()-parent.x0(),dz=parent.z1()-parent.z0();
                double span=Math.hypot(dx,dz);
                double ux=dx/span,uz=dz/span;
                if(start){ux=-ux;uz=-uz;}
                double vx,vz;
                switch(args.get("turn")){
                    case "STRAIGHT" -> {vx=ux;vz=uz;}
                    case "LEFT" -> {vx=-uz;vz=ux;}
                    case "RIGHT" -> {vx=uz;vz=-ux;}
                    default -> throw new IllegalArgumentException("Turn must be LEFT, RIGHT or STRAIGHT");
                }
                double x=start?parent.x0():parent.x1(),z=start?parent.z0():parent.z1();
                // Endpoints inherit all source geometry attributes, so their
                // native collider seams share an EXACT anchor, not planner-
                // guessed tolerances or a second hidden coordinate frame.
                session.placeWall(new MissionStructuralWall(args.get("id"),parent.room(),
                    x,z,x+vx*length,z+vz*length,parent.base(),parent.height(),
                    parent.thickness(),parent.material()));
            }
            case "set-wall-opening" -> {
                if(!args.keySet().equals(OPEN_WALL_FIELDS.keySet()))
                    throw new IllegalArgumentException("Expected native wall ID and offset, width, clearHeight");
                session.setWallOpening(args.get("id"),number(args,"offset"),
                    number(args,"width"),number(args,"clearHeight"));
            }
            case "remove-wall" -> {
                if(!args.keySet().equals(REMOVE_FIELDS.keySet()))
                    throw new IllegalArgumentException("Expected exact native structural wall node id");
                session.removeWall(args.get("id"));
            }
            case "set-floor-depth" -> {
                if(!args.keySet().equals(FLOOR_FIELDS.keySet()))
                    throw new IllegalArgumentException("Expected real upper-floor room and thickness");
                session.setFloorDepth(Integer.parseInt(args.get("room")),number(args,"depth"));
            }
            case "extend-stair-opening" -> {
                if(!args.keySet().equals(STAIR_FIELDS.keySet()))
                    throw new IllegalArgumentException("Expected source stair ID and terminal extension");
                session.extendStairOpening(Integer.parseInt(args.get("stairId")),number(args,"extension"));
            }
            default -> throw new IllegalArgumentException("Unsupported mission operation: "+operation);
        }
    }
    public EditorChange.Details changes(LevelAuthoringSession.Snapshot before,LevelAuthoringSession.Snapshot after){
        if(!session.id().equals(before.sceneId())||!session.id().equals(after.sceneId()))
            throw new IllegalArgumentException("Change belongs to another mission");
        var old=new java.util.LinkedHashMap<String,Prop>();var next=new java.util.LinkedHashMap<String,Prop>();
        for(int i=0;i<before.ids().size();i++)old.put(before.ids().get(i),before.props().get(i));
        for(int i=0;i<after.ids().size();i++)next.put(after.ids().get(i),after.props().get(i));
        var ids=new java.util.LinkedHashSet<>(old.keySet());ids.addAll(next.keySet());
        var nodes=new java.util.ArrayList<EditorChange.Node>();var assets=new java.util.HashSet<String>();
        EditorChange.Bounds bounds=null;
        for(String id:ids){
            Prop a=old.get(id),b=next.get(id);if(java.util.Objects.equals(a,b))continue;
            nodes.add(new EditorChange.Node(id,a==null?EditorChange.Kind.ADDED:b==null?EditorChange.Kind.REMOVED:EditorChange.Kind.UPDATED,
                a==null?null:EditorChange.fingerprint("mission-prop/v1:"+a),b==null?null:EditorChange.fingerprint("mission-prop/v1:"+b)));
            for(Prop prop:new Prop[]{a,b})if(prop!=null){
                assets.add("assets://models/"+prop.mesh());
                for(var collider:LevelPlans.propColliders(session.sourceLevel(),prop)){
                    double c=Math.abs(Math.cos(collider.yaw())),s=Math.abs(Math.sin(collider.yaw()));
                    double rx=c*collider.halfX()+s*collider.halfZ(),rz=s*collider.halfX()+c*collider.halfZ();
                    var region=new EditorChange.Bounds(collider.x()-rx,collider.y(),collider.z()-rz,
                        collider.x()+rx,collider.y()+collider.height(),collider.z()+rz);
                    bounds=bounds==null?region:bounds.union(region);
                }
            }
        }
        // Wall changes have stable structural node IDs and physical bounds.
        // They must invalidate actual collision, room visibility and renderer,
        // not merely alter a metadata fingerprint.
        var previousWalls=new java.util.LinkedHashMap<String,MissionStructuralWall>();
        var currentWalls=new java.util.LinkedHashMap<String,MissionStructuralWall>();
        for(var wall:before.walls())previousWalls.put(wall.id(),wall);
        for(var wall:after.walls())currentWalls.put(wall.id(),wall);
        var wallIds=new java.util.LinkedHashSet<>(previousWalls.keySet());
        wallIds.addAll(currentWalls.keySet());
        for(var id:wallIds){
            var a=previousWalls.get(id);var b=currentWalls.get(id);
            if(java.util.Objects.equals(a,b))continue;
            nodes.add(new EditorChange.Node(MissionStructuralWall.NODE_PREFIX+id,
                a==null?EditorChange.Kind.ADDED:b==null?EditorChange.Kind.REMOVED:EditorChange.Kind.UPDATED,
                a==null?null:EditorChange.fingerprint("native-wall/v1:"+a),
                b==null?null:EditorChange.fingerprint("native-wall/v1:"+b)));
            for(var wall:new MissionStructuralWall[]{a,b})if(wall!=null){
                var region=new EditorChange.Bounds(Math.min(wall.x0(),wall.x1())-wall.thickness()/2,
                    wall.base(),Math.min(wall.z0(),wall.z1())-wall.thickness()/2,
                    Math.max(wall.x0(),wall.x1())+wall.thickness()/2,wall.base()+wall.height(),
                    Math.max(wall.z0(),wall.z1())+wall.thickness()/2);
                bounds=bounds==null?region:bounds.union(region);
            }
        }
        // Native slab changes invalidate the same rendered floor, underside,
        // optical rays and swept body volume; always report physical bounds.
        var floorRooms=new java.util.TreeSet<Integer>();
        floorRooms.addAll(before.floorDepths().keySet());
        floorRooms.addAll(after.floorDepths().keySet());
        for(int room:floorRooms){
            Double prev=before.floorDepths().get(room),nextDepth=after.floorDepths().get(room);
            if(java.util.Objects.equals(prev,nextDepth))continue;
            String id="beckenburg-floor-depth-"+room;
            nodes.add(new EditorChange.Node(id,
                prev==null?EditorChange.Kind.ADDED:nextDepth==null?
                    EditorChange.Kind.REMOVED:EditorChange.Kind.UPDATED,
                prev==null?null:EditorChange.fingerprint("native-floor-depth/v1:"+room+":"+prev),
                nextDepth==null?null:EditorChange.fingerprint("native-floor-depth/v1:"+room+":"+nextDepth)));
            var r=session.sourceLevel().room(room);
            double depth=Math.max(prev==null?0:prev,nextDepth==null?0:nextDepth);
            var region=new EditorChange.Bounds(r.x0(),r.y()-depth,r.z0(),
                r.x1(),r.y(),r.z1());
            bounds=bounds==null?region:bounds.union(region);
        }
        var stairIds=new java.util.TreeSet<Integer>();
        stairIds.addAll(before.stairExtensions().keySet());
        stairIds.addAll(after.stairExtensions().keySet());
        for(int id:stairIds){
            Double a=before.stairExtensions().get(id),b=after.stairExtensions().get(id);
            if(java.util.Objects.equals(a,b))continue;
            String nativeId="beckenburg-stair-landing-"+id;
            nodes.add(new EditorChange.Node(nativeId,
                a==null?EditorChange.Kind.ADDED:b==null?EditorChange.Kind.REMOVED:EditorChange.Kind.UPDATED,
                a==null?null:EditorChange.fingerprint("native-stair-opening/v1:"+id+":"+a),
                b==null?null:EditorChange.fingerprint("native-stair-opening/v1:"+id+":"+b)));
            var stair=session.sourceLevel().stairs().stream().filter(x->x.id()==id).findFirst()
                .orElseThrow();
            double margin=stair.width()/2+.25;
            var region=new EditorChange.Bounds(
                Math.min(stair.x0(),stair.x1())-margin,stair.y1()-.45,
                Math.min(stair.z0(),stair.z1())-margin,
                Math.max(stair.x0(),stair.x1())+margin,stair.y1()+.03,
                Math.max(stair.z0(),stair.z1())+margin);
            bounds=bounds==null?region:bounds.union(region);
        }
        boolean reordered=!before.ids().equals(after.ids())||!java.util.Objects.equals(before.architecture(),after.architecture())
            ||!before.doorWidths().equals(after.doorWidths())||
                !before.floorDepths().equals(after.floorDepths())||
                !before.stairExtensions().equals(after.stairExtensions());
        return new EditorChange.Details(nodes,assets,nodes.isEmpty()&&!reordered?java.util.Set.of():java.util.Set.of(EditorChange.Dependency.values()),bounds,reordered);
    }
    public void validate(){session.validate();}
    public void reopen()throws IOException{session.reopen();}
    public void recoverPrevious()throws IOException{session.recoverPrevious();}
    public EditorAgentService.Capture capture(Path destination)throws IOException{
        // Same actual mission renderer as the live editor, exact frame without stale GPU readback.
        renderer.renderExactFrame(()->renderer.drawPractice(map.get(),List.of(),0));
        var image=renderer.image();
        Path file=destination.toAbsolutePath().normalize();
        if(!ImageIO.write(image,"png",file.toFile()))throw new IOException("PNG writer unavailable");
        return new EditorAgentService.Capture(file,image.getWidth(),image.getHeight(),"SceneRenderer.drawPractice");
    }
    public List<String> tests(){return List.of("mission-runtime-parity","mission-open-routes",
        "mission-door-catalog","mission-deck-catalog","mission-stair-catalog");}
    public List<EditorAgentService.Check> test(String id){
        if(id.equals("mission-stair-catalog")){
            var nativeLevel=session.level();
            return nativeLevel.stairs().stream().map(stair->new EditorAgentService.Check(
                "NATIVE_STAIR_"+stair.id(),true,
                "from="+stair.a()+" to="+stair.b()+" terminal="+stair.x1()+","+
                    stair.y1()+","+stair.z1()+" extension="+
                    session.stairExtensions().getOrDefault(stair.id(),0.0)+
                    " actualVoids="+nativeLevel.voids().size())).toList();
        }
        if(id.equals("mission-deck-catalog")){
            var current=session.level();
            return current.rooms().stream().filter(r->!r.outside()&&r.y()>.05)
                .map(r->new EditorAgentService.Check("NATIVE_UPPER_DECK_"+r.id(),true,
                    "room="+r.id()+" elevation="+r.y()+" depth="+DeckSlabs.thickness(current,r)+
                        " floorVoidCount="+current.voids().size())).toList();
        }
        if(id.equals("mission-door-catalog")){
            // Read-only discovery of the source-backed architectural door IDs.
            // All metadata comes from the same LevelDefinition the game uses.
            return session.level().portals().stream().filter(LevelDefinition.Portal::door).map(p->
                new EditorAgentService.Check("NATIVE_DOOR_"+p.id(),true,
                    "id="+p.id()+" rooms="+p.a()+","+p.b()+" xyz="+p.x()+","+p.y()+","+p.z()+
                        " width="+p.width()+" alongX="+p.alongX()+" kind="+p.kind())).toList();
        }
        if(id.equals("mission-open-routes")){
            var result=LevelTraversalAudit.inspect(map.get());
            var checks=new java.util.ArrayList<EditorAgentService.Check>();
            checks.add(new EditorAgentService.Check("OPEN_ROUTE_REACHABILITY",result.passed(),
                result.level()+" "+result.revision()+": "+result.positions()+" swept positions, Y"+result.lowest()+".."+result.highest()
                    +"; open doors only, live state unchanged"));
            for(var finding:result.findings())checks.add(new EditorAgentService.Check(finding.code(),false,
                "room "+finding.room()+": "+finding.detail()));
            return List.copyOf(checks);
        }
        if(!id.equals("mission-runtime-parity"))throw new IllegalArgumentException("Unsupported test: "+id);
        var level=map.get().level();
        boolean props=level.props().equals(session.props());
        var available=new java.util.ArrayList<>(level.colliders());boolean collision=true;
        for(var prop:session.props())for(var expected:LevelPlans.propColliders(session.sourceLevel(),prop)){
            int found=-1;for(int i=0;i<available.size();i++)if(LevelPlans.sameBody(available.get(i),expected)){found=i;break;}
            if(found<0)collision=false;else available.remove(found);
        }
        return List.of(new EditorAgentService.Check("NATIVE_PROP_PARITY",props,"HouseMap props equal current unsaved authoring props"),
            new EditorAgentService.Check("NATIVE_COLLIDER_PARITY",collision,"Native collider list contains each colliding authored prop"));
    }
}
