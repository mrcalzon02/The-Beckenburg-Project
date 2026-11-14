package org.beckenburg;

import org.beckenburg.editor.EditorAgentProtocol;

import org.beckenburg.editor.EditorAgentService;
import org.beckenburg.editor.EditorHistory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** Actual native mission and Castle adapters, not a shadow test scene. */
public final class EditorAgentServiceTest {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static void rejects(Runnable action){
        try{action.run();}catch(IllegalArgumentException|IllegalStateException expected){return;}
        throw new AssertionError("Expected rejected operation");
    }
    public static void main(String[] args)throws Exception{
        Path home=Files.createTempDirectory("editor-agent-");
        String previous=System.getProperty("user.home");System.setProperty("user.home",home.toString());
        try{
            Thread owner=Thread.currentThread();
            Runnable access=()->{if(Thread.currentThread()!=owner)throw new IllegalStateException("Wrong owner");};
            var session=new LevelAuthoringSession(HouseMap.Site.HOUSE);
            var history=new EditorHistory<>(session,60);
            var renderer=new SceneRenderer();renderer.setRenderSize(320,200);
            var host=new LevelEditorAgent(session,()->new HouseMap(HouseMap.Site.HOUSE,session.level()),renderer,access);
            var service=new EditorAgentService<>(host,history);
            int editable=0;while(HouseAuthoringAttachments.required(session.sourceLevel(),session.prop(editable)))editable++;
            final int index=editable;
            var original=session.prop(index);long initial=service.inspect().revision();
            var move=Map.of("id",session.nodeId(index),"x",Double.toString(original.x()+.125),
                "y",Double.toString(original.y()),"z",Double.toString(original.z()));
            service.transact(initial,"move-prop",move);
            check(session.prop(index).x()==original.x()+.125,"Agent modified actual active authoring session");
            check(service.inspect().state().props().equals(session.props()),"Unsaved readback is exact");
            rejects(()->service.transact(initial,"move-prop",move));
            check(service.test(session.revision(),"mission-runtime-parity").result().stream().allMatch(EditorAgentService.Check::passed),
                "Native HouseMap rendering props and collision match edited scene");
            service.undo(session.revision());check(session.prop(index).equals(original),"Shared undo restores native prop");
            service.redo(session.revision());var moved=session.prop(index);
            history.execute(()->session.put(index,original));
            service.undo(session.revision());check(session.prop(index).equals(moved),"Human and agent use same history");
            var invalid=Map.of("id",session.nodeId(index),"x","NaN","y","0","z","0");
            rejects(()->service.transact(session.revision(),List.of(
                new EditorAgentService.Command("move-prop",move),new EditorAgentService.Command("move-prop",invalid))));
            check(session.prop(index).equals(moved),"Mid-batch failure restores every edit");
            rejects(()->service.transact(session.revision(),"shell",Map.of()));
            var beforeCancel=session.snapshot();
            var polls=new java.util.concurrent.atomic.AtomicInteger();
            var cancellationMove=new java.util.LinkedHashMap<>(move);
            cancellationMove.put("x",Double.toString(original.x()+.5));
            try{
                service.transact(session.revision(),List.of(new EditorAgentService.Command("move-prop",cancellationMove),
                    new EditorAgentService.Command("move-prop",move)),()->polls.incrementAndGet()==2);
                throw new AssertionError("Cancellation ignored");
            }catch(java.util.concurrent.CancellationException expected){}
            check(session.snapshot().equals(beforeCancel),"Cancellation after first native edit restores full scene identity and props");
            service.undo(session.revision());check(session.prop(index).equals(original),"Cancelled batch adds no undo entry and preserves earlier history");
            service.redo(session.revision());check(session.prop(index).equals(moved),"Earlier redo remains valid after cancelled batch");
            Thread.currentThread().interrupt();
            try{
                service.transact(session.revision(),List.of(new EditorAgentService.Command("move-prop",move)));
                throw new AssertionError("Owner interruption ignored");
            }catch(java.util.concurrent.CancellationException expected){}
            finally{Thread.interrupted();}
            check(session.snapshot().equals(beforeCancel),"Interrupted owner cannot leave partial edits");
            var unrelated=new LevelAuthoringSession(HouseMap.Site.SCHOOL);
            rejects(()->new EditorAgentService<>(host,new EditorHistory<>(unrelated,2)));
            service.save(session.revision());
            check(new HouseMap(HouseMap.Site.HOUSE).level().props().get(index).equals(moved),"Saved data consumed by real game load");
            service.edit(()->session.put(index,original));service.reopen(session.revision());
            check(session.prop(index).equals(moved),"Reopen loads authoritative persisted state into same session");
            service.undo(session.revision());check(session.prop(index).equals(original),"Reopen is undoable");
            Files.writeString(session.documentPath(),"corrupt document");
            try{service.reopen(session.revision());throw new AssertionError("Corrupt file accepted");}
            catch(java.io.IOException expected){}
            check(session.prop(index).equals(original),"Bad reopen preserves unsaved state");
            var spawn=session.level().room(0).spawn();renderer.x=spawn.x();renderer.z=spawn.z();renderer.eyeHeight=spawn.y()+1.62;
            var capture=service.capture(session.revision(),home.resolve("mission.png"));
            var image=javax.imageio.ImageIO.read(capture.result().file().toFile());
            check(image.getWidth()==320&&image.getHeight()==200,"Capture is a real rendered PNG");
            check(capture.revision()==session.revision(),"Capture includes exact scene revision");
            var denied=new java.util.concurrent.atomic.AtomicBoolean();
            Thread other=new Thread(()->{try{service.inspect();}catch(IllegalStateException expected){denied.set(true);}});
            other.start();other.join();check(denied.get(),"Off-thread scene access rejected");
            var castle=CastleLevelEdits.current();var project=new CastleEditorProject(castle);
            var castleService=new EditorAgentService<>(new CastleEditorAgent(project,new CastleHubMap(),renderer,access),new EditorHistory<>(project,60));
            double[] start=castle.spawn();
            castleService.transact(castle.revision(),"set-spawn",Map.of("x","13.5","z","19","yaw","1.5"));
            check(CastleLevelEdits.current().spawn()[2]==1.5,"Castle adapter mutates runtime singleton");
            castleService.undo(castle.revision());check(castle.spawn()[2]==start[2],"Castle undo shares live scene");
            castleService.save(castle.revision());castle.setSpawn(13.5,19,1.25);castleService.reopen(castle.revision());
            check(castle.spawn()[2]==start[2],"Castle reload into same singleton");
            Files.writeString(castle.path(),"format=invalid");
            try{castleService.reopen(castle.revision());throw new AssertionError("Bad Castle accepted");}catch(java.io.IOException expected){}
            check(castle.spawn()[2]==start[2],"Corrupt Castle reload preserves state");
            var responses=new java.io.ByteArrayOutputStream();
            int code=EditorAgentProtocol.run(service,new java.io.BufferedReader(new java.io.StringReader("inspect\nunknown\t"+session.revision()+"\n")),
                new java.io.PrintStream(responses, true,java.nio.charset.StandardCharsets.UTF_8));
            check(code==1&&responses.toString(java.nio.charset.StandardCharsets.UTF_8).contains("\"ok\":false"),"CLI returns structured failures and failing exit status");
            var consumed=new java.util.concurrent.atomic.AtomicInteger();var beforeProtocol=session.snapshot();
            var excessive=new java.io.Reader(){
                public int read(char[] chars,int offset,int length)throws java.io.IOException{
                    int total=consumed.addAndGet(length);if(total>=32768)throw new java.io.IOException("Reader guard: unbounded line buffering");
                    java.util.Arrays.fill(chars,offset,offset+length,'x');return length;
                }
                public void close(){}
            };
            var boundedOutput=new java.io.ByteArrayOutputStream();
            int bounded=EditorAgentProtocol.run(service,new java.io.BufferedReader(excessive),new java.io.PrintStream(boundedOutput,true,java.nio.charset.StandardCharsets.UTF_8));
            check(bounded==1&&consumed.get()<32768&&boundedOutput.toString(java.nio.charset.StandardCharsets.UTF_8).contains("protocol")
                &&session.snapshot().equals(beforeProtocol),"Request limit applies before unbounded buffering or any native edit");
            System.out.println("PASS native mission/Castle service: stale rejection, shared undo, batch rollback, save/game load/reopen, renderer capture, owner thread and CLI");
        }finally{
            System.setProperty("user.home",previous);
            try(var files=Files.walk(home)){files.sorted(java.util.Comparator.reverseOrder()).forEach(p->{try{Files.deleteIfExists(p);}catch(Exception ignored){}});}
        }
    }
}
