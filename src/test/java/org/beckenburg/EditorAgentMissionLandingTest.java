package org.beckenburg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.beckenburg.LevelDefinition.*;
import org.beckenburg.editor.EditorAgentService;
import org.beckenburg.editor.EditorHistory;
import org.beckenburg.editor.RuntimeSceneDocument;

/**
 * Native source stair opening + deep upper floor: real rendered void/rim,
 * player support, full traversal and protected BSCN/undo. This previously
 * failed the School upper locker corridor as UNREACHABLE_ROOM room 7.
 */
public final class EditorAgentMissionLandingTest {
    private EditorAgentMissionLandingTest(){}
    private static final double EXTENSION=.16,DEPTH=.35;
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static void rejects(Runnable run){
        try{run.run();}catch(IllegalArgumentException|IllegalStateException expected){return;}
        throw new AssertionError("An unsafe native stair or slab command was accepted");
    }
    private static Map<String,String> opening(int id,double extension){
        return Map.of("stairId",Integer.toString(id),"extension",Double.toString(extension));
    }
    private static Map<String,String> slab(int room,double depth){
        return Map.of("room",Integer.toString(room),"depth",Double.toString(depth));
    }
    private static void aim(SceneRenderer renderer,Stair stair,int view){
        double dx=stair.x1()-stair.x0(),dz=stair.z1()-stair.z0();
        double len=Math.hypot(dx,dz),tx=dx/len,tz=dz/len;
        switch(view){
            case 0 -> {
                renderer.x=stair.x1()+tx*1.35;
                renderer.z=stair.z1()+tz*1.35;
                renderer.eyeHeight=stair.y1()+1.62;
            }
            case 1 -> {
                renderer.x=stair.x1()+tx*.75+.48;
                renderer.z=stair.z1()+tz*.75;
                renderer.eyeHeight=stair.y1()+1.30;
            }
            case 2 -> {
                renderer.x=stair.x0()+dx*.76;
                renderer.z=stair.z0()+dz*.76;
                renderer.eyeHeight=stair.y0()+(stair.y1()-stair.y0())*.76+1.50;
            }
            default -> {
                renderer.x=stair.x1()+tx*2.1-.45;
                renderer.z=stair.z1()+tz*2.1;
                renderer.eyeHeight=stair.y1()+1.5;
            }
        }
        renderer.yaw=Math.atan2(stair.x1()-renderer.x,stair.z1()-renderer.z);
        renderer.pitch=Math.atan2(stair.y1()-.22-renderer.eyeHeight,
            Math.max(.1,Math.hypot(stair.x1()-renderer.x,stair.z1()-renderer.z)));
    }
    private static void checkRoutes(LevelTraversalAudit.Result original,
                                    LevelTraversalAudit.Result edited){
        var codes=new HashSet<String>();
        for(var f:original.findings())codes.add(f.code()+":"+f.room());
        for(var f:edited.findings())check(codes.contains(f.code()+":"+f.room()),
            "Native stair terminal must not introduce blocked room or socket: "+f);
    }
    public static void main(String[] args)throws Exception{
        var home=Files.createTempDirectory("beckenburg-landing-native-");
        String previous=System.getProperty("user.home");
        System.setProperty("user.home",home.toString());
        try{
            var site=HouseMap.Site.SCHOOL;
            var session=new LevelAuthoringSession(site);
            var source=session.sourceLevel();
            var stair=source.stairs().stream().filter(s->s.y1()>s.y0()&&s.b()==7)
                .findFirst().orElseThrow();
            var upper=source.room(stair.b());
            var beforeGame=new HouseMap(site,session.level());
            var routesBefore=LevelTraversalAudit.inspect(beforeGame);
            var renderer=new SceneRenderer();renderer.setRenderSize(320,200);
            var agent=new EditorAgentService<>(new LevelEditorAgent(session,
                ()->new HouseMap(site,session.level()),renderer,()->{}),
                new EditorHistory<>(session,60));
            check(agent.summary().operations().containsAll(List.of(
                "extend-stair-opening","set-floor-depth")),
                "Agent exposes safe native upper stair opening and floor commands");
            check(agent.summary().tests().contains("mission-stair-catalog"),
                "Native source stair IDs are discoverable without hardcoding");
            check(agent.test(session.revision(),"mission-stair-catalog").result().stream()
                .anyMatch(c->c.code().equals("NATIVE_STAIR_"+stair.id())),
                "The live editor enumerates the correct School staircase");
            var initial=session.snapshot();
            rejects(()->agent.transact(session.revision(),"set-floor-depth",
                slab(upper.id(),DEPTH)));
            check(initial.equals(session.snapshot()),"Deep corridor floor is refused without its cut");
            Path evidence=Path.of("build","editor-agent-evidence","mission-stair-landing");
            Files.createDirectories(evidence);
            long initialRev=session.revision();
            for(int i=0;i<4;i++){
                aim(renderer,stair,i);
                agent.capture(initialRev,evidence.resolve("before-"+i+".png"));
            }
            var oldVoid=source.voids().stream()
                .filter(v->Math.abs(v.y()-stair.y1())<.025
                    &&stair.contains((v.x0()+v.x1())/2,(v.z0()+v.z1())/2,-.2))
                .findFirst().orElseThrow();
            agent.transact(session.revision(),"extend-stair-opening",
                opening(stair.id(),EXTENSION));
            String nodeId="beckenburg-stair-landing-"+stair.id();
            check(agent.node(session.revision(),nodeId).result().kind().equals("STAIR_OPENING"),
                "Extended source stair has a stable native scene identity");
            check(agent.properties(session.revision(),nodeId).result().fields()
                .get("extension").equals(EXTENSION),"Native stair terminal is inspectable");
            var opened=session.level();
            var newVoid=opened.voids().stream().filter(v->
                Math.abs(v.y()-stair.y1())<.025&&
                stair.contains((v.x0()+v.x1())/2,(v.z0()+v.z1())/2,-.2))
                .findFirst().orElseThrow();
            check(newVoid.z1()>oldVoid.z1()+.20&&
                Math.abs(newVoid.z1()-(stair.z1()+EXTENSION))<1e-8,
                "Actual deck void, rather than a renderer-only hole, reaches past the final riser");
            check(opened.guards().size()==source.guards().size(),
                "Existing authored side and entrance guards are retained");
            check(!DeckSlabs.solid(opened,opened.room(upper.id()),stair.x1(),stair.z1()+.10),
                "Native upper landing contains a real floor aperture at the last tread");
            agent.transact(session.revision(),"set-floor-depth",slab(upper.id(),DEPTH));
            check(session.floorDepths().get(upper.id())==DEPTH,
                "First-class opening allows the formerly rejected thick upper School deck");
            var finished=new HouseMap(site,session.level());
            check(DeckSlabs.thickness(finished.level(),finished.level().room(upper.id()))==DEPTH,
                "Actual School physics and renderer consume the same deep slab");
            checkRoutes(routesBefore,LevelTraversalAudit.inspect(finished));
            boolean visible=false;
            for(int i=0;i<4;i++){
                aim(renderer,stair,i);
                Path before=evidence.resolve("before-"+i+".png");
                Path after=evidence.resolve("after-"+i+".png");
                agent.capture(session.revision(),after);
                if(!Arrays.equals(Files.readAllBytes(before),Files.readAllBytes(after))){
                    Files.copy(before,evidence.resolve("before.png"),StandardCopyOption.REPLACE_EXISTING);
                    Files.copy(after,evidence.resolve("after.png"),StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("Native School stair and slab render difference camera "+i);
                    visible=true;break;
                }
            }
            check(visible,"Real fixed-camera game renderer must show the joined stair/slab construction");
            var completed=session.snapshot();
            rejects(()->agent.transact(session.revision(),"extend-stair-opening",
                opening(stair.id(),0)));
            rejects(()->agent.transact(session.revision(),"extend-stair-opening",
                opening(stair.id(),.25)));
            rejects(()->agent.transact(session.revision(),"extend-stair-opening",
                opening(99999,EXTENSION)));
            check(completed.equals(session.snapshot()),
                "A dependent slab cannot lose its stair cut, and invalid extensions are atomic");
            rejects(()->agent.transact(session.revision(),List.of(
                new EditorAgentService.Command("set-floor-depth",slab(upper.id(),0)),
                new EditorAgentService.Command("extend-stair-opening",opening(stair.id(),.50)))));
            check(completed.equals(session.snapshot()),"Broken compound edit restores complete authored stair and slab");
            agent.undo(session.revision());
            check(!session.floorDepths().containsKey(upper.id()),
                "Undo removes physical slab but preserves stair cut");
            agent.undo(session.revision());
            check(session.stairExtensions().isEmpty(),"Undo restores legacy source stair opening");
            agent.redo(session.revision());
            agent.redo(session.revision());
            check(session.snapshot().equals(completed),
                "Shared redo restores exactly the reviewed native architecture");
            agent.save(session.revision());
            var saved=session.snapshot();
            agent.reopen(session.revision());
            check(saved.equals(session.snapshot()),"Full native stair and slab survive save/reopen");
            var game=new HouseMap(site);
            check(game.level().floorVoid(stair.x1(),stair.y1(),stair.z1()+.10),
                "Independently launched real game consumes the saved stair aperture");
            checkRoutes(routesBefore,LevelTraversalAudit.inspect(game));
            check(RuntimeSceneDocument.load(session.documentPath()).find(nodeId)!=null,
                "Native BSCN stores the stable stair opening component");
            System.out.println("PASS native mission School terminal landing and deep upper deck: physical aperture, guarded join, full route preservation, renderer, independent saved game, rollback, undo/redo");
        }finally{
            System.setProperty("user.home",previous);
            try(var files=Files.walk(home)){
                files.sorted(java.util.Comparator.reverseOrder()).forEach(p->{
                    try{Files.deleteIfExists(p);}catch(Exception ignored){}
                });
            }
        }
    }
}
