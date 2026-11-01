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
    public java.util.Set<EditorAgentService.Capability> capabilities(){return java.util.Set.of(EditorAgentService.Capability.values());}
    public List<String> operations(){return List.of("set-spawn");}
    public void apply(String operation, Map<String,String> args){
        if(!operation.equals("set-spawn")||!args.keySet().equals(java.util.Set.of("x","z","yaw")))
            throw new IllegalArgumentException("Expected set-spawn with x, z, yaw (radians)");
        scene.setSpawn(Double.parseDouble(args.get("x")),Double.parseDouble(args.get("z")),Double.parseDouble(args.get("yaw")));
    }
    public void validate(){
        for(var node:project.hierarchy())
            if(!Double.isFinite(node.x())||!Double.isFinite(node.y())||!Double.isFinite(node.z()))
                throw new IllegalArgumentException("Non-finite scene node: "+node.id());
    }
    public void reopen()throws IOException{scene.reopen();}
    public EditorAgentService.Capture capture(Path destination)throws IOException{
        // Render now. Never return the previous timer frame or a synthetic preview.
        renderer.drawCastle(map,0,true);
        var image=renderer.image();
        Path file=destination.toAbsolutePath().normalize();
        if(!ImageIO.write(image,"png",file.toFile()))throw new IOException("PNG writer unavailable");
        return new EditorAgentService.Capture(file,image.getWidth(),image.getHeight(),"SceneRenderer.drawCastle");
    }
    public List<String> tests(){return List.of("castle-scene-audit");}
    public List<EditorAgentService.Check> test(String id){
        if(!id.equals("castle-scene-audit"))throw new IllegalArgumentException("Unsupported test: "+id);
        var findings=CastleEditorAudit.inspect(scene);
        if(findings.isEmpty())return List.of(new EditorAgentService.Check("CASTLE_SCENE_AUDIT",true,"No advisory findings"));
        return findings.stream().map(f->new EditorAgentService.Check(f.code(),false,
            f.kind()+"/"+f.id()+": "+f.message())).toList();
    }
}
