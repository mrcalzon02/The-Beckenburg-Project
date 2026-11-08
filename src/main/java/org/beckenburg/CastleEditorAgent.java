package org.beckenburg;

import org.beckenburg.editor.EditorAgentService;
import org.beckenburg.editor.EditorChange;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

/** Adapter for the same singleton consumed by CastleHubMap and SceneRenderer. */
public final class CastleEditorAgent implements EditorAgentService.Host<CastleLevelEdits.Snapshot> {
    private final CastleLevelEdits scene=CastleLevelEdits.current();
    private final CastleEditorProject project;
    private final CastleHubMap map;
    private final SceneRenderer renderer;
    private final Runnable access;
    public CastleEditorAgent(CastleEditorProject project, CastleHubMap map,
                             SceneRenderer renderer, Runnable access) {
        if(project.scene()!=scene)throw new IllegalArgumentException("Castle runtime requires its authoritative singleton");
        this.project=project;this.map=java.util.Objects.requireNonNull(map);
        this.renderer=java.util.Objects.requireNonNull(renderer);this.access=java.util.Objects.requireNonNull(access);
    }
    public CastleEditorProject project(){return project;}
    public long revision(){return scene.revision();}
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
    public List<String> operations(){return List.of("set-spawn","put-surface","remove-surface");}
    public void apply(String operation, Map<String,String> args){
        switch(operation){
            case "set-spawn" -> {
                exact(args,"x","z","yaw");
                scene.setSpawn(number(args,"x"),number(args,"z"),number(args,"yaw"));
            }
            case "put-surface" -> {
                exact(args,"id","kind","x0","z0","x1","z1","base","height","material");
                scene.put(new CastleLevelEdits.Surface(args.get("id"),CastleLevelEdits.Kind.valueOf(args.get("kind")),
                    number(args,"x0"),number(args,"z0"),number(args,"x1"),number(args,"z1"),
                    number(args,"base"),number(args,"height"),Integer.parseInt(args.get("material"))));
            }
            case "remove-surface" -> {
                exact(args,"id");
                if(scene.surfaces().stream().noneMatch(s->s.id().equals(args.get("id"))))
                    throw new IllegalArgumentException("Unknown native Castle surface");
                scene.removeSurface(args.get("id"));
            }
            default -> throw new IllegalArgumentException("Unsupported native Castle operation: "+operation);
        }
    }
    private static double number(Map<String,String> args,String key){return Double.parseDouble(args.get(key));}
    private static void exact(Map<String,String> args,String...keys){
        if(!args.keySet().equals(java.util.Set.of(keys)))throw new IllegalArgumentException("Expected fields: "+String.join(", ",keys));
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
    public List<String> tests(){return List.of("castle-scene-audit","castle-world-terrain");}
    public List<EditorAgentService.Check> test(String id){
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
}
