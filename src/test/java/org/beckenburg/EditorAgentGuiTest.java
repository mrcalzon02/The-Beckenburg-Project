package org.beckenburg;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/** Real Windows editor, native viewport, human Undo button and active mission
 * service. Reflection only chooses the fixture scene without modal input.
 */
public final class EditorAgentGuiTest {
    private static JButton button(Container parent,String text){
        for(Component c:parent.getComponents()){
            if(c instanceof JButton b&&b.getText().equals(text))return b;
            if(c instanceof Container nested){var found=button(nested,text);if(found!=null)return found;}
        }
        return null;
    }
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static <T> T component(Container parent,Class<T> type){
        for(var child:parent.getComponents()){
            if(type.isInstance(child))return type.cast(child);
            if(child instanceof Container nested){T found=component(nested,type);if(found!=null)return found;}
        }
        return null;
    }
    /** Drive the actual human verification menu and recovery confirmation. */
    private static void recoverThroughMenu(DeveloperLevelEditor editor){
        var stage=new java.util.concurrent.atomic.AtomicInteger();
        var failure=new AtomicReference<String>();long started=System.nanoTime();
        Timer driver=new Timer(50,event->{
            for(Window window:Window.getWindows())if(window instanceof JDialog dialog&&dialog.isShowing()){
                var pane=component(dialog,JOptionPane.class);if(pane==null)continue;
                if(stage.get()==0&&dialog.getTitle().equals("Scene verification")){
                    var choice=component(dialog,JComboBox.class);
                    if(choice==null){failure.set("Human recovery chooser missing");dialog.dispose();return;}
                    choice.setSelectedItem("RECOVER PREVIOUS SAVE");
                    pane.setInputValue("RECOVER PREVIOUS SAVE");stage.incrementAndGet();pane.setValue(JOptionPane.OK_OPTION);
                }else if(stage.get()==1&&dialog.getTitle().equals("Recover saved scene")){
                    stage.incrementAndGet();pane.setValue(JOptionPane.OK_OPTION);
                }else if(stage.get()==2&&dialog.getTitle().equals("Scene evidence")){
                    stage.incrementAndGet();pane.setValue(JOptionPane.OK_OPTION);
                }else if(dialog.getTitle().equals("Scene verification failed")){
                    failure.set("Human recovery failed");pane.setValue(JOptionPane.OK_OPTION);
                }
                if(System.nanoTime()-started>15_000_000_000L){failure.set("Human recovery dialog timed out");dialog.dispose();}
            }
        });
        driver.start();try{button(editor,"VERIFY SCENE").doClick();}finally{driver.stop();}
        check(failure.get()==null&&stage.get()==3,"Actual human recovery menu completed: "+failure.get());
    }
    public static void main(String[] args)throws Exception{
        var backgroundFailure=new AtomicReference<Throwable>();var priorHandler=Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread,failure)->{backgroundFailure.compareAndSet(null,failure);failure.printStackTrace();});
        Path output=Path.of(args.length==0?"build/editor-agent-evidence":args[0]).toAbsolutePath();Files.createDirectories(output);
        Path home=Files.createTempDirectory("editor-agent-gui-");
        System.setProperty("user.home",home.toString());
        AtomicReference<JFrame> window=new AtomicReference<>();
        AtomicReference<DeveloperLevelEditor> editor=new AtomicReference<>();
        try{
            SwingUtilities.invokeAndWait(()->{
                DeveloperLevelEditor.open(null);
                for(Window w:Window.getWindows())if(w instanceof JFrame f&&f.getContentPane() instanceof DeveloperLevelEditor e){
                    window.set(f);editor.set(e);break;
                }
                check(editor.get()!=null,"Actual human editor opened");
            });
            SwingUtilities.invokeAndWait(()->{
                try{
                    var e=editor.get();var castle=e.agentService();var state=castle.inspect();
                    castle.capture(state.revision(),output.resolve("castle.png"));
                    double originalYaw=CastleLevelEdits.current().spawn()[2];
                    castle.save(state.revision());
                    castle.transact(castle.inspect().revision(),"set-spawn",Map.of("x","13.5","z","19","yaw","1.125"));
                    castle.save(castle.inspect().revision());
                    Files.writeString(castle.inspect().document(),"format=invalid");
                    recoverThroughMenu(e);
                    check(CastleLevelEdits.current().spawn()[2]==originalYaw,"Human Castle recovery uses same singleton");
                    button(e,"UNDO").doClick();
                    check(CastleLevelEdits.current().spawn()[2]==1.125,"Human undo restores pre-recovery Castle edits");
                    var open=DeveloperLevelEditor.class.getDeclaredMethod("openMission",HouseMap.Site.class);
                    open.setAccessible(true);open.invoke(e,HouseMap.Site.HOUSE);
                    var fresh=e.agentService();
                    check(fresh.inspect().state() instanceof LevelAuthoringSession.Snapshot snapshot
                        &&UserLevelLibrary.HOUSE_REBUILD.equals(snapshot.architecture())&&snapshot.props().size()==45,
                        "Actual fresh human editor opens furnished rebuilt architecture");
                    check(!Files.exists(LevelAuthoringSession.path(HouseMap.Site.HOUSE)),"Opening fresh human editor does not implicitly save");
                    fresh.capture(fresh.inspect().revision(),output.resolve("house-fresh-default.png"));
                    fresh.save(fresh.inspect().revision());
                    check(new HouseMap(HouseMap.Site.HOUSE).level().id().equals("briar-rebuild"),"Actual fresh human save loads in gameplay");
                    // The remaining scenario explicitly exercises a historical
                    // document's recovery and human rebuild/undo workflow.
                    LevelAuthoringSession.saveProps(LevelAuthoringSession.path(HouseMap.Site.HOUSE),"beckenburg/house",LevelPlans.legacyHouseSource().props());
                    open.invoke(e,HouseMap.Site.HOUSE);
                    var service=e.agentService();var before=service.inspect();var node=before.hierarchy().get(0);
                    var currentMap=DeveloperLevelEditor.class.getDeclaredMethod("currentMissionMap");currentMap.setAccessible(true);
                    Object initialMap=currentMap.invoke(e);
                    check(initialMap==currentMap.invoke(e),"Unchanged native revision reuses actual map");
                    service.save(before.revision());
                    service.transact(before.revision(),"move-prop",Map.of("id",node.id(),
                        "x",Double.toString(node.x()+.125),"y",Double.toString(node.y()),"z",Double.toString(node.z())));
                    check(service.inspect().hierarchy().get(0).x()==node.x()+.125,"Agent edit visible in active human editor");
                    Object editedMap=currentMap.invoke(e);
                    check(initialMap!=editedMap&&editedMap==currentMap.invoke(e),"Committed native change invalidates map exactly until revision is consumed");
                    check(((HouseMap)editedMap).level().props().get(0).x()==node.x()+.125,"Cache rebuild consumes authoritative native props");
                    var undo=button(e,"UNDO");check(undo!=null,"Human undo button exists");undo.doClick();
                    check(service.inspect().hierarchy().get(0).x()==node.x(),"Human undo reverses agent edit");
                    var redo=button(e,"REDO");redo.doClick();
                    check(service.inspect().hierarchy().get(0).x()==node.x()+.125,"Human redo restores agent edit");
                    service.save(service.inspect().revision());
                    service.reopen(service.inspect().revision());
                    check(service.test(service.inspect().revision(),"mission-runtime-parity").result().stream().allMatch(c->c.passed()),
                        "Real editor's HouseMap has same reopened props and colliders");
                    service.capture(service.inspect().revision(),output.resolve("house.png"));
                    Files.writeString(output.resolve("house-inspection.json"),EditorAgentCli.json(service.inspect()));
                    Files.writeString(service.inspect().document(),"corrupt fixture document");
                    recoverThroughMenu(e);
                    check(service.inspect().hierarchy().get(0).x()==node.x(),"Human recovery restores previous real saved prop");
                    undo.doClick();
                    check(service.inspect().hierarchy().get(0).x()==node.x()+.125,"Human undo restores pre-recovery unsaved edit");
                    service.transact(service.inspect().revision(),"move-prop",Map.of("id",node.id(),"x",Double.toString(node.x()),
                        "y",Double.toString(node.y()),"z",Double.toString(node.z())));
                    check(service.inspect().operations().contains("rebuild-house"),"Actual human House service exposes native rebuild");
                    service.transact(service.inspect().revision(),"rebuild-house",Map.of("baseline","ORIGINAL"));
                    check(((HouseMap)currentMap.invoke(e)).level().room(5).y()==2.8,"Human viewport consumes rebuilt authoritative upper storey");
                    undo.doClick();check(((HouseMap)currentMap.invoke(e)).level().room(5).y()==3,"Human Undo restores original architecture");
                    redo.doClick();check(((HouseMap)currentMap.invoke(e)).level().room(5).y()==2.8,"Human Redo restores rebuilt architecture");
                    service.save(service.inspect().revision());service.reopen(service.inspect().revision());
                    check(new HouseMap(HouseMap.Site.HOUSE).level().room(9).y()==-2.4,"Saved human editor rebuild becomes actual built-in House source");
                    check(service.test(service.inspect().revision(),"mission-runtime-parity").result().stream().allMatch(c->c.passed()),"Human rebuilt native source retains actual multipart physics");
                    var rendererField=DeveloperLevelEditor.class.getDeclaredField("renderer");rendererField.setAccessible(true);
                    var nativeRenderer=(SceneRenderer)rendererField.get(e);nativeRenderer.lamp=true;
                    nativeRenderer.x=16;nativeRenderer.z=12.4;nativeRenderer.eyeHeight=1.62;
                    nativeRenderer.yaw=Math.atan2(14.9-16,14.1925-12.4);nativeRenderer.pitch=Math.atan2(.85-1.62,Math.hypot(14.9-16,14.1925-12.4));
                    service.capture(service.inspect().revision(),output.resolve("windows-editor-rebuilt-kitchen-runtime.png"));
                    Files.writeString(output.resolve("house-rebuilt-inspection.json"),EditorAgentCli.json(service.inspect()));
                    try{castle.inspect();throw new AssertionError("Old Castle service remains active");}
                    catch(IllegalStateException expected){}
                }catch(Exception failure){throw new RuntimeException(failure);}
            });
        }finally{
            SwingUtilities.invokeAndWait(()->{if(window.get()!=null)window.get().dispose();});
            SwingUtilities.invokeAndWait(()->{
                if(editor.get()!=null){try{editor.get().agentService();throw new AssertionError("Closed editor exposed");}
                catch(IllegalStateException expected){}}
            });
            try(var files=Files.walk(home)){files.sorted(java.util.Comparator.reverseOrder()).forEach(p->{try{Files.deleteIfExists(p);}catch(Exception ignored){}});}
            Thread.setDefaultUncaughtExceptionHandler(priorHandler);
        }
        if(backgroundFailure.get()!=null)throw new AssertionError("Native editor background thread failed",backgroundFailure.get());
        System.out.println("PASS real Windows editor: mission/Castle service and recovery, human House architecture rebuild/undo/redo/save/reopen, actual recovery menu, runtime parity, native rebuilt capture and stale/closed scene rejection");
    }
}
