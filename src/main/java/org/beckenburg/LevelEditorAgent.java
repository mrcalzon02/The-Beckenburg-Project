package org.beckenburg;

import org.beckenburg.editor.EditorAgentService;
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
public final class LevelEditorAgent implements EditorAgentService.Host<LevelAuthoringSession.Snapshot> {
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
    public LevelAuthoringSession project(){return session;}
    public long revision(){return session.revision();}
    public void checkAccess(){access.run();}
    public java.util.Set<EditorAgentService.Capability> capabilities(){return java.util.Set.of(EditorAgentService.Capability.values());}
    public List<String> operations(){return List.of("move-prop");}
    public void apply(String operation,Map<String,String> args){
        if(!operation.equals("move-prop")||!args.keySet().equals(java.util.Set.of("id","x","y","z")))
            throw new IllegalArgumentException("Expected move-prop with id, x, y, z");
        String id=args.get("id");
        int index=session.indexOf(id);
        var p=session.prop(index);
        session.put(index,new Prop(p.mesh(),p.room(),Double.parseDouble(args.get("x")),
            Double.parseDouble(args.get("y")),Double.parseDouble(args.get("z")),
            p.scale(),p.yaw(),p.halfX(),p.halfZ(),p.height(),p.support()));
    }
    public void validate(){session.validate();}
    public void reopen()throws IOException{session.reopen();}
    public EditorAgentService.Capture capture(Path destination)throws IOException{
        renderer.drawPractice(map.get(),List.of(),0);
        var image=renderer.image();
        Path file=destination.toAbsolutePath().normalize();
        if(!ImageIO.write(image,"png",file.toFile()))throw new IOException("PNG writer unavailable");
        return new EditorAgentService.Capture(file,image.getWidth(),image.getHeight(),"SceneRenderer.drawPractice");
    }
    public List<String> tests(){return List.of("mission-runtime-parity");}
    public List<EditorAgentService.Check> test(String id){
        if(!id.equals("mission-runtime-parity"))throw new IllegalArgumentException("Unsupported test: "+id);
        var level=map.get().level();
        boolean props=level.props().equals(session.props());
        boolean collision=session.props().stream().filter(p->p.halfX()>0&&p.halfZ()>0&&p.height()>0)
            .allMatch(p->level.colliders().contains(LevelPlans.propCollider(p)));
        return List.of(new EditorAgentService.Check("NATIVE_PROP_PARITY",props,"HouseMap props equal current unsaved authoring props"),
            new EditorAgentService.Check("NATIVE_COLLIDER_PARITY",collision,"Native collider list contains each colliding authored prop"));
    }
}
