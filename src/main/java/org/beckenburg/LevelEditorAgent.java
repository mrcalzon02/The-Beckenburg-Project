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
public final class LevelEditorAgent implements EditorAgentService.Host<LevelAuthoringSession.Snapshot>,EditorAgentService.WalkingCollision {
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
    public boolean walkable(double x,double y,double z){return map.get().traversable(x,y,z);}
    public EditorAgentService.DiskState compareSaved(LevelAuthoringSession.Snapshot live)throws IOException{
        return session.matchesSaved(live)?EditorAgentService.DiskState.MATCHES:EditorAgentService.DiskState.DIFFERS;
    }
    public void checkAccess(){access.run();}
    public java.util.Set<EditorAgentService.Capability> capabilities(){return java.util.Set.of(EditorAgentService.Capability.values());}
    public List<String> operations(){return List.of("move-prop");}
    private static final Map<String,String> MOVE_FIELDS=Map.of("id","string","x","number","y","number","z","number");
    public Map<String,Map<String,String>> operationParameters(){return Map.of("move-prop",MOVE_FIELDS);}
    public void apply(String operation,Map<String,String> args){
        if(!operation.equals("move-prop")||!args.keySet().equals(MOVE_FIELDS.keySet()))
            throw new IllegalArgumentException("Expected move-prop with id, x, y, z");
        String id=args.get("id");
        int index=session.indexOf(id);
        var p=session.prop(index);
        session.put(index,new Prop(p.mesh(),p.room(),Double.parseDouble(args.get("x")),
            Double.parseDouble(args.get("y")),Double.parseDouble(args.get("z")),
            p.scale(),p.yaw(),p.halfX(),p.halfZ(),p.height(),p.support()));
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
                if(prop.halfX()>0&&prop.halfZ()>0){
                    var collider=LevelPlans.propCollider(prop);
                    double c=Math.abs(Math.cos(collider.yaw())),s=Math.abs(Math.sin(collider.yaw()));
                    double rx=c*collider.halfX()+s*collider.halfZ(),rz=s*collider.halfX()+c*collider.halfZ();
                    var region=new EditorChange.Bounds(collider.x()-rx,collider.y(),collider.z()-rz,
                        collider.x()+rx,collider.y()+collider.height(),collider.z()+rz);
                    bounds=bounds==null?region:bounds.union(region);
                }
            }
        }
        boolean reordered=!before.ids().equals(after.ids());
        return new EditorChange.Details(nodes,assets,nodes.isEmpty()&&!reordered?java.util.Set.of():java.util.Set.of(EditorChange.Dependency.values()),bounds,reordered);
    }
    public void validate(){session.validate();}
    public void reopen()throws IOException{session.reopen();}
    public void recoverPrevious()throws IOException{session.recoverPrevious();}
    public EditorAgentService.Capture capture(Path destination)throws IOException{
        renderer.drawPractice(map.get(),List.of(),0);
        var image=renderer.image();
        Path file=destination.toAbsolutePath().normalize();
        if(!ImageIO.write(image,"png",file.toFile()))throw new IOException("PNG writer unavailable");
        return new EditorAgentService.Capture(file,image.getWidth(),image.getHeight(),"SceneRenderer.drawPractice");
    }
    public List<String> tests(){return List.of("mission-runtime-parity","mission-open-routes");}
    public List<EditorAgentService.Check> test(String id){
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
        boolean collision=session.props().stream().filter(p->p.halfX()>0&&p.halfZ()>0&&p.height()>0)
            .allMatch(p->level.colliders().contains(LevelPlans.propCollider(p)));
        return List.of(new EditorAgentService.Check("NATIVE_PROP_PARITY",props,"HouseMap props equal current unsaved authoring props"),
            new EditorAgentService.Check("NATIVE_COLLIDER_PARITY",collision,"Native collider list contains each colliding authored prop"));
    }
}
