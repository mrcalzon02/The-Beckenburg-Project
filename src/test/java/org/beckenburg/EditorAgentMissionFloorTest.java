package org.beckenburg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.beckenburg.LevelDefinition.Room;
import org.beckenburg.editor.EditorAgentService;
import org.beckenburg.editor.EditorHistory;
import org.beckenburg.editor.RuntimeSceneDocument;

/**
 * Authoritative upper-storey deck-depth edit: original Room floor + new
 * underside/cut edges, native swept-capsule/optical collision, saved BSCN,
 * human/agent shared history and real fixed-camera rendered pixels.
 */
public final class EditorAgentMissionFloorTest {
    private EditorAgentMissionFloorTest(){}
    private static final double DEPTH=.35,BODY=.90;
    private record Probe(int room,double x,double z,Room lower,List<double[]> cameras){}
    private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    private static void rejects(Runnable action){
        try{action.run();}catch(IllegalArgumentException|IllegalStateException expected){return;}
        throw new AssertionError("Invalid native floor-depth edit was accepted");
    }
    private static Map<String,String> depth(int room,double value){
        return Map.of("room",Integer.toString(room),"depth",Double.toString(value));
    }
    private static List<double[]> viewCandidates(HouseMap map,Room lower,double x,double z){
        var out=new ArrayList<double[]>();
        for(double[] d:new double[][]{{-1.5,0},{1.5,0},{0,-1.5},{0,1.5},{-2.5,0},{0,2.5}}){
            double cx=x+d[0],cz=z+d[1];
            if(!lower.contains(cx,lower.y(),cz)||!map.traversable(cx,lower.y(),cz))continue;
            out.add(new double[]{cx,cz});
            if(out.size()>=4)break;
        }
        return out;
    }
    private static Probe findProbe(HouseMap game){
        var level=game.level();
        for(var upper:level.rooms()){
            if(upper.outside()||upper.y()<1||DeckSlabs.thickness(level,upper)>.001)continue;
            for(var lower:level.rooms()){
                if(lower.outside()||lower.y()>=upper.y()-1||
                    lower.y()+lower.height()<upper.y()-.18)continue;
                double left=Math.max(upper.x0(),lower.x0())+1.1;
                double right=Math.min(upper.x1(),lower.x1())-1.1;
                double front=Math.max(upper.z0(),lower.z0())+1.1;
                double back=Math.min(upper.z1(),lower.z1())-1.1;
                for(double x=left;x<right;x+=1.15)for(double z=front;z<back;z+=1.15){
                    if(level.floorVoid(x,upper.y(),z)||!DeckSlabs.solid(level,upper,x,z)
                        ||!game.traversable(x,upper.y(),z)||!game.traversable(x,lower.y(),z))continue;
                    double y=upper.y()-DEPTH-BODY+.07;
                    if(!game.airborneClear(x,y,z,BODY))continue;
                    // The short vertical ray must be clear in the legacy floor
                    // before authoring the new underside.
                    if(!game.clearLineOfSight(x,upper.y()-DEPTH-.13,z,
                            x,upper.y()-DEPTH+.11,z))continue;
                    var cameras=viewCandidates(game,lower,x,z);
                    if(!cameras.isEmpty())return new Probe(upper.id(),x,z,lower,cameras);
                }
            }
        }
        throw new AssertionError("No actual School upper-deck/under-room free probe exists");
    }
    private static void aim(SceneRenderer renderer,Probe p,double[] view,double top){
        renderer.x=view[0];renderer.z=view[1];
        renderer.eyeHeight=p.lower().y()+1.62;
        renderer.yaw=Math.atan2(p.x()-renderer.x,p.z()-renderer.z);
        renderer.pitch=Math.atan2(top-DEPTH/2-renderer.eyeHeight,
            Math.hypot(p.x()-renderer.x,p.z()-renderer.z));
    }
    public static void main(String[] args)throws Exception{
        var home=Files.createTempDirectory("agent-native-upper-floor-");
        String previous=System.getProperty("user.home");
        System.setProperty("user.home",home.toString());
        try{
            var site=HouseMap.Site.SCHOOL;
            var session=new LevelAuthoringSession(site);
            var map=new HouseMap(site,session.level());
            var probe=findProbe(map);
            var upper=session.level().room(probe.room());
            var renderer=new SceneRenderer();renderer.setRenderSize(320,200);
            var agent=new EditorAgentService<>(new LevelEditorAgent(session,
                ()->new HouseMap(site,session.level()),renderer,()->{}),
                new EditorHistory<>(session,60));
            check(agent.summary().operations().contains("set-floor-depth"),
                "Native mission agent exposes bounded upper-floor thickness command");
            check(agent.summary().tests().contains("mission-deck-catalog"),
                "Agent can inspect native deck identities and floor thicknesses");
            long initial=session.revision();
            var original=session.snapshot();
            var beforeRoute=LevelTraversalAudit.inspect(map);
            Path proof=Path.of("build","editor-agent-evidence","mission-floor");
            Files.createDirectories(proof);
            for(int i=0;i<probe.cameras().size();i++){
                aim(renderer,probe,probe.cameras().get(i),upper.y());
                agent.capture(initial,proof.resolve("before-"+i+".png"));
            }
            agent.transact(initial,"set-floor-depth",depth(probe.room(),DEPTH));
            String nodeId="beckenburg-floor-depth-"+probe.room();
            check(session.floorDepths().get(probe.room())==DEPTH,
                "Real native authoring session changed upper floor thickness");
            var node=agent.node(session.revision(),nodeId).result();
            check(node.kind().equals("FLOOR_SLAB")&&Math.abs(node.y()-upper.y())<1e-8,
                "Stable structural floor node visible in the native hierarchy");
            check(agent.properties(session.revision(),nodeId).result().fields().get("depth").equals(DEPTH),
                "Native floor depth is inspectable through property API");
            var scene=new HouseMap(site,session.level());
            var part=scene.level().room(probe.room());
            check(DeckSlabs.thickness(scene.level(),part)==DEPTH,
                "Real mission runtime consumes the exact authored slab thickness");
            check(DeckSlabs.cells(scene.level(),part).stream().anyMatch(c->
                Math.abs(c.top()-upper.y())<1e-8&&
                Math.abs(c.bottom()-(upper.y()-DEPTH))<1e-8),
                "Renderer's actual deck cells have physical top, underside and exposed edges");
            check(scene.traversable(probe.x(),upper.y(),probe.z()),
                "The original upper walking surface remains traversable");
            double below=upper.y()-DEPTH-BODY+.07;
            check(!scene.airborneClear(probe.x(),below,probe.z(),BODY),
                "Actual game body collision blocks the newly deepened underside");
            check(!scene.clearLineOfSight(probe.x(),upper.y()-DEPTH-.13,probe.z(),
                probe.x(),upper.y()-DEPTH+.11,probe.z()),
                "Actual game optical ray is occluded by slab geometry");
            var afterRoute=LevelTraversalAudit.inspect(scene);
            var baseline=new java.util.HashSet<String>();
            for(var problem:beforeRoute.findings())baseline.add(problem.code()+":"+problem.room());
            for(var problem:afterRoute.findings())
                check(baseline.contains(problem.code()+":"+problem.room()),
                    "Upper deck cannot introduce new blocked gameplay access: "+problem);
            boolean visible=false;
            for(int i=0;i<probe.cameras().size();i++){
                aim(renderer,probe,probe.cameras().get(i),upper.y());
                Path before=proof.resolve("before-"+i+".png"),after=proof.resolve("after-"+i+".png");
                agent.capture(session.revision(),after);
                if(!Arrays.equals(Files.readAllBytes(before),Files.readAllBytes(after))){
                    Files.copy(before,proof.resolve("before.png"),StandardCopyOption.REPLACE_EXISTING);
                    Files.copy(after,proof.resolve("after.png"),StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("Native deck underside renderer diff from camera "+i);
                    visible=true;break;
                }
            }
            check(visible,"Real fixed-camera game render must reveal the new underside");
            var authored=session.snapshot();
            rejects(()->agent.transact(session.revision(),"set-floor-depth",depth(1,DEPTH)));
            rejects(()->agent.transact(session.revision(),"set-floor-depth",depth(probe.room(),.60)));
            rejects(()->agent.transact(session.revision(),"set-floor-depth",depth(probe.room(),Double.NaN)));
            rejects(()->agent.transact(session.revision(),"set-floor-depth",depth(999,DEPTH)));
            check(authored.equals(session.snapshot()),"Rejected slab edits preserve exact native scene");
            rejects(()->agent.transact(session.revision(),List.of(
                new EditorAgentService.Command("set-floor-depth",depth(probe.room(),0)),
                new EditorAgentService.Command("set-floor-depth",depth(1,DEPTH)))));
            check(authored.equals(session.snapshot()),"Second invalid command rolls back a batch without loss");
            agent.save(session.revision());
            check(agent.verifyPosition(session.revision(),nodeId,node.x(),node.y(),node.z())
                .result().diskState()==EditorAgentService.DiskState.MATCHES,
                "Native BSCN floor depth save matches exact live scene");
            var persisted=session.snapshot();
            agent.reopen(session.revision());
            check(session.snapshot().equals(persisted),"Floor thickness survives native save/reopen");
            var savedGame=new HouseMap(site);
            check(DeckSlabs.thickness(savedGame.level(),savedGame.level().room(probe.room()))==DEPTH
                &&!savedGame.airborneClear(probe.x(),below,probe.z(),BODY),
                "Independent actual game load consumes the saved physical slab");
            check(RuntimeSceneDocument.load(session.documentPath()).find(nodeId)!=null,
                "Versioned native BSCN retains stable floor-slab component ID");
            agent.transact(session.revision(),"set-floor-depth",depth(probe.room(),0));
            check(session.floorDepths().isEmpty()&&new HouseMap(site,session.level())
                .airborneClear(probe.x(),below,probe.z(),BODY),
                "Revert removes authored depth and restores original open air");
            agent.undo(session.revision());
            check(session.floorDepths().get(probe.room())==DEPTH,
                "Shared Undo restores physical slab thickness and node identity");
            agent.redo(session.revision());
            check(session.floorDepths().isEmpty(),"Shared Redo restores source floor");
            System.out.println("PASS native mission upper-floor slab: depth/soffit renderer, body and optical parity, route safety, stable IDs, typed command, rollback, BSCN save/reopen, independent game load, undo/redo");
        }finally{
            System.setProperty("user.home",previous);
            try(var paths=Files.walk(home)){
                paths.sorted(java.util.Comparator.reverseOrder()).forEach(p->{
                    try{Files.deleteIfExists(p);}catch(Exception cleanup){}
                });
            }
        }
    }
}
