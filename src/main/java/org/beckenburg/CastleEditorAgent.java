package org.beckenburg;

import org.beckenburg.editor.EditorAgentService;
import org.beckenburg.editor.EditorChange;
import org.beckenburg.editor.EditorProject;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

/** Adapter for the same singleton consumed by CastleHubMap and SceneRenderer. */
public final class CastleEditorAgent implements EditorAgentService.Host<CastleLevelEdits.Snapshot>,EditorAgentService.WalkingCollision,EditorAgentService.NodeFocus,EditorAgentService.PropertySource {
    private final CastleLevelEdits scene=CastleLevelEdits.current();
    private final CastleEditorProject project;
    private final CastleHubMap map;
    private final SceneRenderer renderer;
    private final Runnable access;
    private final java.util.function.Consumer<EditorProject.Node> select;
    public CastleEditorAgent(CastleEditorProject project, CastleHubMap map,
                             SceneRenderer renderer, Runnable access) {
        this(project,map,renderer,access,node->{});
    }
    CastleEditorAgent(CastleEditorProject project,CastleHubMap map,SceneRenderer renderer,Runnable access,
                      java.util.function.Consumer<EditorProject.Node> select){
        if(project.scene()!=scene)throw new IllegalArgumentException("Castle runtime requires its authoritative singleton");
        this.project=project;this.map=java.util.Objects.requireNonNull(map);
        this.renderer=java.util.Objects.requireNonNull(renderer);this.access=java.util.Objects.requireNonNull(access);
        this.select=java.util.Objects.requireNonNull(select);
    }
    public CastleEditorProject project(){return project;}
    public long revision(){return scene.revision();}
    public EditorAgentService.DiskState compareSaved(CastleLevelEdits.Snapshot live)throws IOException{
        return scene.matchesSaved(live)?EditorAgentService.DiskState.MATCHES:EditorAgentService.DiskState.DIFFERS;
    }
    private record Station(double x,double z) {}
    private record Spawn(double x,double z,double yaw) {}
    private static Spawn spawn(CastleLevelEdits.Snapshot s) { return new Spawn(s.spawnX(),s.spawnZ(),s.spawnYaw()); }

    /** Temporary comparison of native records in the same resolution order as the human hierarchy. */
    private static Map<String,Object> nodes(CastleLevelEdits.Snapshot state) {
        var values=new java.util.LinkedHashMap<String,Object>();
        for (var surface:state.surfaces()) values.put(surface.id(),surface);
        for (var prop:state.props()) values.putIfAbsent(prop.id(),prop);
        for (var lamp:state.lamps()) values.putIfAbsent(lamp.id(),lamp);
        var stations=state.stations();
        for (var station:CastleHubMap.Station.values()) {
            var p=stations.get(station.name());
            values.putIfAbsent(station.name(),new Station(p[0],p[1]));
        }
        values.putIfAbsent("start",spawn(state));
        return values;
    }
    private static String fingerprint(Object value) {
        return value==null ? null : EditorChange.fingerprint("castle-node/v1:"+value);
    }
    private static void collectAssets(Object value,java.util.Set<String> assets) {
        if (value instanceof CastleLevelEdits.Prop p) assets.add("assets://models/"+p.mesh());
        if (value instanceof CastleLevelEdits.Lamp l) {
            assets.add("assets://models/"+l.on());
            assets.add("assets://models/"+l.off());
        }
    }
    public EditorChange.Details changes(CastleLevelEdits.Snapshot before,CastleLevelEdits.Snapshot after) {
        if (before.sameWorld(after)) {
            var a=spawn(before);
            var b=spawn(after);
            if (a.equals(b)) return new EditorChange.Details(List.of(),java.util.Set.of(),java.util.Set.of(),null,false);
            if (after.surfaces().stream().anyMatch(s->s.id().equals("start")) ||
                after.props().stream().anyMatch(p->p.id().equals("start")) ||
                after.lamps().stream().anyMatch(l->l.id().equals("start"))) return EditorChange.Details.full();
            var node=new EditorChange.Node("start",EditorChange.Kind.UPDATED,fingerprint(a),fingerprint(b));
            return new EditorChange.Details(List.of(node),java.util.Set.of(),java.util.Set.of(EditorChange.Dependency.RENDER),null,false);
        }
        var old=nodes(before);
        var next=nodes(after);
        var ids=new java.util.LinkedHashSet<>(old.keySet());
        ids.addAll(next.keySet());
        var changes=new java.util.ArrayList<EditorChange.Node>();
        var assets=new java.util.HashSet<String>();
        for (String id:ids) {
            var a=old.get(id);
            var b=next.get(id);
            if (java.util.Objects.equals(a,b)) continue;
            var kind=a==null ? EditorChange.Kind.ADDED : b==null ? EditorChange.Kind.REMOVED : EditorChange.Kind.UPDATED;
            changes.add(new EditorChange.Node(id,kind,fingerprint(a),fingerprint(b)));
            collectAssets(a,assets);
            collectAssets(b,assets);
        }
        return new EditorChange.Details(changes,assets,java.util.Set.of(EditorChange.Dependency.values()),null,true);
    }
    public void checkAccess(){access.run();}
    public java.util.Set<EditorAgentService.Capability> capabilities(){
        return java.util.EnumSet.allOf(EditorAgentService.Capability.class);
    }
    public boolean walkable(double x,double y,double z){return map.traversable(x,z,y);}
    public void focus(EditorProject.Node node){
        select.accept(node);
        if(node.kind().equals("PROP")&&positionPropCamera(renderer,map,scene.propById(node.id())))return;
        positionCamera(renderer,node.x(),node.y(),node.z());
    }
    static boolean positionPropCamera(SceneRenderer renderer,CastleHubMap map,CastleLevelEdits.Prop prop){
        if(prop==null)return false;
        double[] bounds=SceneRenderer.castleBounds(prop.mesh());
        double targetY=prop.y()+bounds[4]*prop.scale()*.5;
        double range=Math.max(2,Math.max(bounds[3],Math.max(bounds[4],bounds[5]))*prop.scale()*1.4);
        double eye=prop.y()+1.4;
        for(int i=0;i<8;i++){
            double angle=prop.yaw()+Math.PI*(1+i)/4;
            double x=prop.x()+range*Math.sin(angle),z=prop.z()+range*Math.cos(angle);
            if(!map.traversable(x,z,prop.y())||!FixtureLighting.castleRayClear(map,x,eye,z,prop.x(),targetY,prop.z()))continue;
            renderer.x=x;renderer.z=z;renderer.eyeHeight=eye;
            renderer.yaw=Math.atan2(prop.x()-x,prop.z()-z);
            renderer.pitch=Math.atan2(targetY-eye,range);renderer.setEditorSelection(prop.x(),prop.y(),prop.z());
            return true;
        }
        return false;
    }
    static void positionCamera(SceneRenderer renderer,double x,double y,double z){
        renderer.x=Math.max(CastleWorldBounds.MIN_X+.5,Math.min(CastleHubMap.WIDTH-.5,x-1.8));
        renderer.z=Math.max(.5,Math.min(CastleHubMap.HEIGHT-.5,z-1.8));
        renderer.eyeHeight=Math.max(CastleWorldBounds.MIN_Y+.7,Math.min(10.5,y+1.4));
        renderer.yaw=Math.atan2(x-renderer.x,z-renderer.z);renderer.pitch=-.12;
        renderer.setEditorSelection(x,y,z);
    }
    public Map<String,Object> properties(EditorProject.Node node){
        return switch(node.kind()){
            case "PROP" -> {
                var p=scene.propById(node.id());
                yield Map.of("mesh",p.mesh(),"scale",p.scale(),"yawDegrees",Math.toDegrees(p.yaw()),
                    "collisionX",p.halfX(),"collisionZ",p.halfZ(),"upper",p.upper());
            }
            case "LAMP" -> {
                var l=scene.lampById(node.id());
                yield Map.of("on",l.on(),"off",l.off(),"scale",l.scale(),"yawDegrees",Math.toDegrees(l.yaw()),
                    "mount",l.mount().name(),"radius",l.radius(),"power",l.power());
            }
            case "SPAWN" -> Map.of("yawDegrees",Math.toDegrees(scene.spawn()[2]));
            case "STATION" -> Map.of();
            default -> {
                var s=scene.surfaceById(node.id());
                yield Map.of("x0",s.x0(),"z0",s.z0(),"x1",s.x1(),"z1",s.z1(),"base",s.base(),"height",s.height(),"material",s.material());
            }
        };
    }
    public List<String> operations(){return PARAMETERS.keySet().stream().sorted().toList();}
    private static final Map<String,Map<String,String>> PARAMETERS=Map.ofEntries(
        Map.entry("rebuild-castle",Map.of("baseline",String.join("|",java.util.Arrays.stream(CastleRebuildUpgrade.Baseline.values()).map(Enum::name).toList()))),
        Map.entry("set-spawn",Map.of("x","number","z","number","yaw","number")),
        Map.entry("put-surface",Map.of("id","string","kind",String.join("|",java.util.Arrays.stream(CastleLevelEdits.Kind.values()).map(Enum::name).toList()),
            "x0","number","z0","number","x1","number","z1","number","base","number","height","number","material","integer")),
        Map.entry("remove-surface",Map.of("id","string")),
        Map.entry("place-prop",Map.of("id","string","mesh","string","x","number","y","number","z","number")),
        Map.entry("set-door",Map.of("slot","string","leaf","EMPTY|WOOD|PORTCULLIS")),
        Map.entry("set-nav-cell",Map.of("x","integer","z","integer","state","BLOCKED|CLEAR")),
        Map.entry("place-ceiling-lamp",Map.of("id","string","x","number","floorY","number","z","number","radius","number","power","number")),
        Map.entry("set-prop-field",Map.of("id","string","field","X|Y|Z|SCALE|YAW_DEGREES|COLLISION_X|COLLISION_Z","value","number")),
        Map.entry("set-lamp-field",Map.of("id","string","field","X|Y|Z|SCALE|YAW_DEGREES|RADIUS|POWER","value","number")),
        Map.entry("remove-prop",Map.of("id","string")),Map.entry("remove-lamp",Map.of("id","string")));
    public Map<String,Map<String,String>> operationParameters(){return PARAMETERS;}
    public void apply(String operation, Map<String,String> args){
        var fields=PARAMETERS.get(operation);
        if(fields==null || !args.keySet().equals(fields.keySet()))throw new IllegalArgumentException("Incorrect fields for "+operation);
        switch(operation){
            case "place-ceiling-lamp" -> scene.placeCeilingLamp(args.get("id"),number(args,"x"),number(args,"floorY"),
                number(args,"z"),number(args,"radius"),number(args,"power"));
            case "set-prop-field" -> scene.editProp(requireKind(args.get("id"),"PROP"),fieldChange(args));
            case "set-lamp-field" -> scene.editLamp(requireKind(args.get("id"),"LAMP"),fieldChange(args));
            case "remove-prop" -> scene.removeProp(requireKind(args.get("id"),"PROP"));
            case "remove-lamp" -> scene.removeLamp(requireKind(args.get("id"),"LAMP"));
            case "set-door" -> scene.setSocketLeaf(args.get("slot"),CastleDoorSlots.Type.valueOf(args.get("leaf")));
            case "set-nav-cell" -> {
                boolean blocked=switch(args.get("state")){
                    case "BLOCKED"->true;case "CLEAR"->false;default->throw new IllegalArgumentException("Expected BLOCKED or CLEAR");
                };
                scene.setNavBlocked(Integer.parseInt(args.get("x")),Integer.parseInt(args.get("z")),blocked);
            }
            case "place-prop" -> scene.placePaletteProp(args.get("id"),args.get("mesh"),number(args,"x"),number(args,"y"),number(args,"z"));
            case "rebuild-castle" -> {
                CastleRebuildUpgrade.apply(scene,CastleRebuildUpgrade.Baseline.valueOf(args.get("baseline")));
            }
            case "set-spawn" -> {
                scene.setSpawn(number(args,"x"),number(args,"z"),number(args,"yaw"));
            }
            case "put-surface" -> {
                scene.put(new CastleLevelEdits.Surface(args.get("id"),CastleLevelEdits.Kind.valueOf(args.get("kind")),
                    number(args,"x0"),number(args,"z0"),number(args,"x1"),number(args,"z1"),
                    number(args,"base"),number(args,"height"),Integer.parseInt(args.get("material"))));
            }
            case "remove-surface" -> {
                if(scene.surfaces().stream().noneMatch(s->s.id().equals(args.get("id"))))
                    throw new IllegalArgumentException("Unknown native Castle surface");
                scene.removeSurface(args.get("id"));
            }
            default -> throw new IllegalArgumentException("Unsupported native Castle operation: "+operation);
        }
    }
    private static double number(Map<String,String> args,String key){return Double.parseDouble(args.get(key));}
    private String requireKind(String id,String kind){
        if(!project.node(id).kind().equals(kind))throw new IllegalArgumentException("Expected Castle "+kind);
        return id;
    }
    private static Map<CastleLevelEdits.Field,Double> fieldChange(Map<String,String> args){
        return Map.of(CastleLevelEdits.Field.valueOf(args.get("field")),number(args,"value"));
    }
    public void validate(){
        for(var node:project.hierarchy())
            if(!Double.isFinite(node.x())||!Double.isFinite(node.y())||!Double.isFinite(node.z()))
                throw new IllegalArgumentException("Non-finite scene node: "+node.id());
    }
    public void reopen()throws IOException{scene.reopen();}
    public void recoverPrevious()throws IOException{scene.recoverPrevious();}
    public EditorAgentService.Capture capture(Path destination)throws IOException{
        // Render now. Never return the previous timer frame or a synthetic preview.
        renderer.drawCastle(map,0,true);
        var image=renderer.image();
        Path file=destination.toAbsolutePath().normalize();
        if(!ImageIO.write(image,"png",file.toFile()))throw new IOException("PNG writer unavailable");
        return new EditorAgentService.Capture(file,image.getWidth(),image.getHeight(),"SceneRenderer.drawCastle");
    }
    public List<String> tests(){return List.of("castle-scene-audit","castle-world-terrain","castle-cellar-passage","castle-door-sockets");}
    public List<EditorAgentService.Check> test(String id){
        if(id.equals("castle-cellar-passage"))return List.of(cellarSweep(true),cellarSweep(false));
        if(id.equals("castle-door-sockets"))return CastleDoorSlots.all().stream().filter(CastleDoorSlots::available).map(slot->{
            var leaf=scene.doorType(slot.id());
            return new EditorAgentService.Check("DOOR_SOCKET",leaf==CastleDoorSlots.Type.EMPTY||CastleDoorSlots.canInstall(slot.id(),leaf),
                slot.id()+" at "+slot.centerX()+", "+slot.bottom()+", "+slot.centerZ()+"; mounted "+leaf);
        }).toList();
        if(id.equals("castle-world-terrain")){
            var terrain=map.worldSurface();
            boolean hole=true;
            for(double x=28.75;x<43;x+=.5)for(double z=17.75;z<34;z+=.5)
                hole&=terrain.clearSight(new LevelDefinition.Vec(x,.1,z),new LevelDefinition.Vec(x,-1,z))
                    &&terrain.support(x,z,0,4)==null
                    &&!terrain.bodyBlocked(new LevelDefinition.Vec(x,-1,z),1.8,.1);
            return List.of(new EditorAgentService.Check("CASTLE_WORLD_CELLAR_HOLE",hole,
                "Packaged saved editor terrain "+terrain.sha256()+" / "+terrain.triangleCount()
                +" shared faces; whole-footprint body/optical/support passage only; architectural stair not certified"),
                new EditorAgentService.Check("CASTLE_WORLD_RETAINED_GROUND",
                    terrain.support(25,20,0,4)!=null
                    &&!terrain.clearSight(new LevelDefinition.Vec(25,.1,20),new LevelDefinition.Vec(25,-1,20)),
                    "Unpunched editor ground retains support and optical collision"));
        }
        if(!id.equals("castle-scene-audit"))throw new IllegalArgumentException("Unsupported test: "+id);
        var findings=CastleEditorAudit.inspect(scene);
        if(findings.isEmpty())return List.of(new EditorAgentService.Check("CASTLE_SCENE_AUDIT",true,"No advisory findings"));
        return findings.stream().map(f->new EditorAgentService.Check(f.code(),false,
            f.kind()+"/"+f.id()+": "+f.message())).toList();
    }
    private EditorAgentService.Check cellarSweep(boolean descending){
        double north=CastleCellarLayout.LOW_Z-.8,south=CastleCellarLayout.HIGH_Z+.8,x=CastleCellarLayout.FLIGHT_X;
        double feet=descending?0:CastleCellar.FLOOR;
        String code=descending?"CELLAR_DESCENT":"CELLAR_ASCENT";
        if(!CastleCellar.active())return new EditorAgentService.Check(code,false,"Native cellar floor is missing");
        int steps=(int)Math.ceil((south-north)/.025);
        for(int i=0;i<=steps;i++){
            double distance=(south-north)*i/steps,z=descending?south-distance:north+distance;
            double next=CastleHubArchitecturalPlan.elevation(x,z,feet);
            if(!CastleHubPanel.safeElevationTransition(feet,next,x,z)||!map.traversable(x,z,next))
                return new EditorAgentService.Check(code,false,"Blocked at X "+x+", Y "+next+", Z "+z);
            feet=next;
        }
        double target=descending?CastleCellar.FLOOR:0;
        return new EditorAgentService.Check(code,Math.abs(feet-target)<1e-7,
            (steps+1)+" runtime foot positions; landing elevation "+feet+", expected "+target);
    }
}
