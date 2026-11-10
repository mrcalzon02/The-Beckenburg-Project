package org.beckenburg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.beckenburg.editor.EditorAgentService;
import org.beckenburg.editor.EditorHistory;
import org.beckenburg.editor.RuntimeSceneDocument;

/** Tests a real rendered/solid mission partition placed through the same
 * editor/runtime document. No synthetic viewport or shadow collision map. */
public final class EditorAgentMissionWallTest {
    private EditorAgentMissionWallTest(){}
    private record Candidate(MissionStructuralWall wall,double px,double pz){}
    private static void check(boolean ok,String reason){if(!ok)throw new AssertionError(reason);}
    private static void rejects(Runnable command){
        try{command.run();}catch(IllegalArgumentException|IllegalStateException expected){return;}
        throw new AssertionError("Invalid structural edit accepted");
    }
    private static Candidate findWall(LevelAuthoringSession session,HouseMap map){
        for(var room:session.level().rooms()){
            if(room.outside()||room.height()<2.8||room.x1()-room.x0()<5||
                room.z1()-room.z0()<5)continue;
            for(double px=room.x0()+1.6;px<room.x1()-1.6;px+=1.0){
                for(double pz=room.z0()+1.6;pz<room.z1()-1.6;pz+=1.0){
                    var wall=new MissionStructuralWall("test-partition",room.id(),px-.65,pz,
                        px+.65,pz,room.y(),2.2,.2,LevelMaterials.PLASTER);
                    try{wall.validate(session.sourceLevel(),List.of());}
                    catch(IllegalArgumentException rejected){continue;}
                    if(!map.traversable(px,room.y(),pz))continue;
                    for(double side:new double[]{-1.65,1.65}){
                        double cameraZ=pz+side;
                        if(!map.traversable(px,room.y(),cameraZ)||!room.contains(px,room.y(),cameraZ)
                            ||!map.clearLineOfSight(px,room.y()+1.5,cameraZ,
                                px,room.y()+1.15,pz))continue;
                        return new Candidate(wall,px,cameraZ);
                    }
                }
            }
        }
        throw new AssertionError("No native school space for a supported new wall and camera");
    }
    private static Map<String,String> place(MissionStructuralWall w){
        return Map.of("id",w.id(),"room",Integer.toString(w.room()),
            "x0",Double.toString(w.x0()),"z0",Double.toString(w.z0()),
            "x1",Double.toString(w.x1()),"z1",Double.toString(w.z1()),
            "base",Double.toString(w.base()),"height",Double.toString(w.height()),
            "thickness",Double.toString(w.thickness()),"material",Integer.toString(w.material()));
    }
    private static void aim(SceneRenderer renderer,Candidate c){
        var w=c.wall();
        renderer.x=c.px();renderer.z=c.pz();renderer.eyeHeight=w.base()+1.62;
        renderer.yaw=Math.atan2(w.cx()-renderer.x,w.cz()-renderer.z);
        renderer.pitch=Math.atan2(w.base()+1.12-renderer.eyeHeight,
            Math.hypot(w.cx()-renderer.x,w.cz()-renderer.z));
    }
    public static void main(String[] args)throws Exception{
        var temp=Files.createTempDirectory("agent-mission-wall-");
        String previous=System.getProperty("user.home");
        System.setProperty("user.home",temp.toString());
        try {
            var site=HouseMap.Site.SCHOOL;
            var session=new LevelAuthoringSession(site);
            var sourceMap=new HouseMap(site,session.level());
            var candidate=findWall(session,sourceMap);
            var wall=candidate.wall();
            var renderer=new SceneRenderer();renderer.setRenderSize(320,200);
            aim(renderer,candidate);
            var service=new EditorAgentService<>(new LevelEditorAgent(session,
                ()->new HouseMap(site,session.level()),renderer,()->{}),
                new EditorHistory<>(session,60));
            check(service.summary().operations().containsAll(List.of("place-wall","remove-wall")),
                "Native mission agent advertises actual partition construction and deletion");
            long before=session.revision();
            var beforeState=session.snapshot();
            var beforeRoute=LevelTraversalAudit.inspect(sourceMap);
            var evidence=Path.of("build","editor-agent-evidence","mission-wall");
            Files.createDirectories(evidence);
            service.capture(before,evidence.resolve("before.png"));
            service.transact(before,"place-wall",place(wall));
            check(session.walls().size()==1&&session.walls().get(0).equals(wall),
                "Native editor stores the actual wall geometry");
            var nodeId=MissionStructuralWall.NODE_PREFIX+wall.id();
            var node=service.node(session.revision(),nodeId).result();
            check(node.kind().equals("WALL")&&Math.abs(node.x()-wall.cx())<1e-8,
                "Wall has stable discoverable structural hierarchy identity");
            var details=service.properties(session.revision(),nodeId).result();
            check(details.fields().get("material").equals(LevelMaterials.PLASTER),
                "Agent inspects native structural dimensions and material");
            var actual=new HouseMap(site,session.level());
            check(actual.level().sockets().stream().anyMatch(s->s.id().equals(nodeId)&&
                s.type().equals(MissionStructuralWall.SOCKET_TYPE)),
                "Actual game scene receives the authored wall mesh owner");
            check(actual.level().colliders().stream().anyMatch(c->
                Math.abs(c.x()-wall.cx())<1e-8&&Math.abs(c.z()-wall.cz())<1e-8&&
                Math.abs(c.halfX()-wall.halfX())<1e-8&&Math.abs(c.height()-wall.height())<1e-8),
                "Game collision receives exactly the authored solid wall dimensions");
            check(!actual.traversable(wall.cx(),wall.base(),wall.cz()),
                "Player capsule cannot walk through the newly built physical wall");
            check(!actual.clearLineOfSight(candidate.px(),wall.base()+1.5,candidate.pz(),
                wall.cx(),wall.base()+1.15,wall.cz()),
                "New solid wall occludes the game's real visibility ray");
            var afterRoute=LevelTraversalAudit.inspect(actual);
            var priorCodes=new java.util.HashSet<String>();
            for(var finding:beforeRoute.findings())priorCodes.add(finding.code()+":"+finding.room());
            for(var finding:afterRoute.findings())
                check(priorCodes.contains(finding.code()+":"+finding.room()),
                    "Wall must not introduce a new unreachable route or gameplay socket: "+finding);
            aim(renderer,candidate);
            service.capture(session.revision(),evidence.resolve("after.png"));
            check(!Arrays.equals(Files.readAllBytes(evidence.resolve("before.png")),
                Files.readAllBytes(evidence.resolve("after.png"))),
                "Actual fixed-camera game renderer must show the added structural wall");
            var withWall=session.snapshot();
            var overlap=place(new MissionStructuralWall("second",wall.room(),wall.x0(),
                wall.z0()+.04,wall.x1(),wall.z1()+.04,wall.base(),wall.height(),
                wall.thickness(),wall.material()));
            rejects(()->service.transact(session.revision(),"place-wall",overlap));
            check(withWall.equals(session.snapshot()),"Overlapping wall rejected without modifying scene");
            rejects(()->service.transact(session.revision(),List.of(
                new EditorAgentService.Command("remove-wall",Map.of("id",nodeId)),
                new EditorAgentService.Command("place-wall",Map.of("id","invalid-only")))));
            check(withWall.equals(session.snapshot()),"Failed compound batch rolls back removed wall and native ID");
            service.save(session.revision());
            check(service.verifyPosition(session.revision(),nodeId,wall.cx(),wall.base(),wall.cz())
                .result().diskState()==EditorAgentService.DiskState.MATCHES,
                "Saved native structural wall exactly matches live scene");
            var saved=session.snapshot();
            service.reopen(session.revision());
            check(session.snapshot().equals(saved),"Native source wall survives save/reopen without geometry drift");
            var loaded=new HouseMap(site);
            check(loaded.level().sockets().stream().anyMatch(s->s.id().equals(nodeId)),
                "Independent actual game load reads the saved structural partition");
            check(!loaded.traversable(wall.cx(),wall.base(),wall.cz()),
                "Independent loaded game collision still blocks the partition");
            check(RuntimeSceneDocument.load(session.documentPath()).find(nodeId)!=null,
                "BSCN keeps stable versioned wall component metadata");
            service.transact(session.revision(),"remove-wall",Map.of("id",nodeId));
            check(session.walls().isEmpty()&&new HouseMap(site,session.level())
                .traversable(wall.cx(),wall.base(),wall.cz()),"Removing the native wall restores real traversal");
            service.undo(session.revision());
            check(session.walls().equals(List.of(wall)),"Undo restores the same native structural wall ID");
            service.redo(session.revision());
            check(session.walls().isEmpty(),"Redo removes the native wall without touching other geometry");
            System.out.println("PASS native mission wall: solid rendered geometry, actual body/LOS, safe routes, revisions, stable IDs, rejected overlap, batch rollback, BSCN save/reopen, independent game load, undo/redo");
        } finally {
            System.setProperty("user.home",previous);
            try(var paths=Files.walk(temp)){
                paths.sorted(java.util.Comparator.reverseOrder()).forEach(path->{
                    try{Files.deleteIfExists(path);}catch(Exception ignored){}
                });
            }
        }
    }
}
