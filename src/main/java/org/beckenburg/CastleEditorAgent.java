package org.beckenburg;

import org.beckenburg.editor.EditorAgentService;
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
    public void checkAccess(){access.run();}
    public java.util.Set<EditorAgentService.Capability> capabilities(){
        return java.util.EnumSet.allOf(EditorAgentService.Capability.class);
    }
    public List<String> operations(){return List.of("set-spawn","put-surface","remove-surface","rebuild-castle");}
    public void apply(String operation, Map<String,String> args){
        switch(operation){
            case "rebuild-castle" -> {
                exact(args,"baseline");
                CastleRebuildUpgrade.apply(scene,CastleRebuildUpgrade.Baseline.valueOf(args.get("baseline")));
            }
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
