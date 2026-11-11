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
                    try{findJoin(session,wall);}
                    catch(AssertionError cannotJoin){continue;}
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
    /** Find a source-safe real L-corner at a terminal, not a crossing. */
    private static MissionStructuralWall findJoin(LevelAuthoringSession session,MissionStructuralWall first){
        for(double endpoint:new double[]{first.x0(),first.x1()})
            for(double direction:new double[]{-1,1})
                for(double length:new double[]{.85,1.20,1.60}){
                    var joined=new MissionStructuralWall("test-corner",first.room(),
                        endpoint,first.z0(),endpoint,first.z0()+direction*length,
                        first.base(),first.height(),first.thickness(),first.material());
                    try{joined.validate(session.sourceLevel(),List.of(first));return joined;}
                    catch(IllegalArgumentException blocked){/* Try another supported native corner. */}
                }
        throw new AssertionError("No supported corner at the candidate School wall");
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
            check(service.summary().operations().containsAll(List.of("place-wall","extend-wall","set-wall-opening","remove-wall")),
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
            // Build a real two-segment L shell in ONE agent transaction, using
            // the source wall's corner as the exact shared world-space vertex.
            // Both solids must be native renderer/collider objects, not a
            // decorative screen mesh or separate agent-only scene.
            var corner=findJoin(session,wall);
            boolean start=Math.abs(corner.x0()-wall.x0())<1e-8;
            boolean goesPositiveZ=corner.z1()>corner.z0();
            String turn=(start==goesPositiveZ)?"RIGHT":"LEFT";
            var exactExtension=Map.of("id",corner.id(),"anchor",nodeId,
                "endpoint",start?"START":"END","turn",turn,
                "length",Double.toString(Math.abs(corner.z1()-corner.z0())));
            service.transact(session.revision(),List.of(
                new EditorAgentService.Command("place-wall",place(wall)),
                new EditorAgentService.Command("extend-wall",exactExtension)));
            check(session.walls().equals(List.of(wall,corner)),
                "Atomic native agent batch creates two physically joined wall segments");
            var nativeJoined=new HouseMap(site,session.level());
            for(var segment:List.of(wall,corner)){
                check(nativeJoined.level().sockets().stream().anyMatch(s->
                    s.id().equals(MissionStructuralWall.NODE_PREFIX+segment.id())&&
                        s.type().equals(MissionStructuralWall.SOCKET_TYPE)),
                    "Each corner segment is a native rendered structural-wall socket");
                check(nativeJoined.level().colliders().stream().anyMatch(c->
                    Math.abs(c.x()-segment.cx())<1e-8&&Math.abs(c.z()-segment.cz())<1e-8
                        &&Math.abs(c.height()-segment.height())<1e-8),
                    "Native game collision contains both continuous wall segments");
                check(!nativeJoined.traversable(segment.cx(),segment.base(),segment.cz()),
                    "Neither wall segment can be walked through");
            }
            var jointState=session.snapshot();
            // Non-welded near misses and crossed interiors are not legitimate
            // joints. The batch must leave live geometry and revision unchanged.
            var gap=new MissionStructuralWall("test-near-miss",wall.room(),
                corner.x0()+.04,corner.z0(),corner.x1()+.04,corner.z1(),
                corner.base(),corner.height(),corner.thickness(),corner.material());
            rejects(()->service.transact(session.revision(),"place-wall",place(gap)));
            var crossing=new MissionStructuralWall("test-cross",wall.room(),
                wall.cx(),wall.z0()-.55,wall.cx(),wall.z0()+.55,
                wall.base(),wall.height(),wall.thickness(),wall.material());
            rejects(()->service.transact(session.revision(),"place-wall",place(crossing)));
            rejects(()->service.transact(session.revision(),"extend-wall",Map.of(
                "id","test-invalid-turn","anchor",nodeId,"endpoint","START",
                "turn","BACKWARD","length","1.2")));
            rejects(()->service.transact(session.revision(),"extend-wall",Map.of(
                "id","test-invalid-span","anchor",nodeId,"endpoint","END",
                "turn","LEFT","length","12")));
            check(session.snapshot().equals(jointState),
                "Misaligned and crossing runs are rejected atomically, preserving the joint");
            // Undo the construction transaction BEFORE reopen, which is
            // itself an undoable editor operation with its own history entry.
            service.undo(session.revision());
            check(session.walls().isEmpty(),"One undo removes the whole two-wall batch");
            service.redo(session.revision());
            check(new java.util.HashSet<>(session.walls()).equals(java.util.Set.of(wall,corner)),
                "One redo restores the complete supported L-corner and stable native IDs");
            aim(renderer,candidate);
            service.capture(session.revision(),evidence.resolve("joined-corner.png"));
            service.save(session.revision());
            service.reopen(session.revision());
            check(session.snapshot().equals(jointState),
                "Joined native wall geometry and stable IDs survive a BSCN save/reopen");
            var reloaded=new HouseMap(site);
            check(!reloaded.traversable(corner.cx(),corner.base(),corner.cz()),
                "Independent game loader still enforces L-corner physics");
            // Real structural aperture: two physical side piers and a lintel,
            // not a fake texture cut or a second level-editor-only scene.
            var cut=Map.of("id",nodeId,"offset","0","width",".8","clearHeight","1.92");
            var beforeCut=session.snapshot();
            service.transact(session.revision(),"set-wall-opening",cut);
            var openWall=session.walls().stream().filter(w->w.id().equals(wall.id()))
                .findFirst().orElseThrow();
            check(openWall.hasOpening()&&openWall.bodies().size()==3,
                "Source wall splits into two actual solid piers and a lintel");
            var openMap=new HouseMap(site,session.level());
            check(openMap.traversable(wall.cx(),wall.base(),wall.cz()),
                "Wall cut frees the true game capsule center");
            check(!openMap.traversable(wall.x0()+.12,wall.base(),wall.z0()),
                "The native solid pier still blocks gameplay traversal");
            check(openMap.level().colliders().stream().filter(c->
                Math.abs(c.z()-wall.cz())<.2&&
                    Math.abs(c.y()-(wall.base()+1.92))<1e-7).anyMatch(c->
                    Math.abs(c.height()-(wall.height()-1.92))<1e-7),
                "Real game collision retains the overhead wall lintel");
            check(service.properties(session.revision(),nodeId).result().fields()
                .get("openingWidth").equals(.8),
                "Native agent inspection reports the actual opening width");
            aim(renderer,candidate);
            service.capture(session.revision(),evidence.resolve("native-aperture.png"));
            check(!Arrays.equals(Files.readAllBytes(evidence.resolve("joined-corner.png")),
                Files.readAllBytes(evidence.resolve("native-aperture.png"))),
                "The actual fixed-camera renderer shows the new physical cutout");
            var opened=session.snapshot();
            rejects(()->service.transact(session.revision(),"set-wall-opening",Map.of(
                "id",nodeId,"offset","0","width","1.29","clearHeight","2.1")));
            rejects(()->service.transact(session.revision(),"set-wall-opening",Map.of(
                "id",nodeId,"offset","0","width","1.2","clearHeight","1.2")));
            check(session.snapshot().equals(opened),
                "Invalid apertures are rejected without altering scene geometry");
            service.save(session.revision());
            service.reopen(session.revision());
            check(session.snapshot().equals(opened),
                "The native wall aperture survives save/reopen with stable IDs");
            var loadedWithOpening=new HouseMap(site);
            check(loadedWithOpening.traversable(wall.cx(),wall.base(),wall.cz()),
                "Independent game load sees the saved physical opening");
            check(RuntimeSceneDocument.load(session.documentPath()).find(nodeId).components()
                .get(0).properties().containsKey("cutWidth"),
                "The versioned scene document stores real aperture dimensions");
            service.transact(session.revision(),"set-wall-opening",Map.of(
                "id",nodeId,"offset","0","width","0","clearHeight","0"));
            check(session.snapshot().equals(beforeCut),
                "Sealing the cut reconstructs the original solid wall");
            service.undo(session.revision());
            check(session.snapshot().equals(opened),"Undo restores the opening and stable wall identity");
            service.redo(session.revision());
            check(session.snapshot().equals(beforeCut),"Redo restores the original solid geometry");
            System.out.println("PASS native mission wall: corner joins, anchored extensions, three-body aperture and lintel, real render, independent game load, validation, persistence and history");
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
