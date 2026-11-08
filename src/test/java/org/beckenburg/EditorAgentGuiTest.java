package org.beckenburg;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
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
    private record Access(String url,String bearer){}
    private static final HttpClient CLIENT=HttpClient.newHttpClient();
    private static Access openAccess(DeveloperLevelEditor editor)throws Exception{
        var access=new AtomicReference<Access>();
        SwingUtilities.invokeAndWait(()->{
            Timer driver=new Timer(50,event->{
                for(Window window:Window.getWindows())if(window instanceof JDialog dialog && dialog.isShowing()){
                    var pane=component(dialog,JOptionPane.class);if(pane==null)continue;
                    if(dialog.getTitle().equals("Scene verification")){
                        var choices=component(dialog,JComboBox.class);
                        choices.setSelectedItem("START LOCAL OBSERVATION");
                        pane.setInputValue("START LOCAL OBSERVATION");pane.setValue(JOptionPane.OK_OPTION);
                    }else if(dialog.getTitle().equals("Read-only access to this editor scene")){
                        var lines=component(dialog,JTextArea.class).getText().split("\n");
                        access.set(new Access(lines[0].substring("Address  ".length()),lines[1].substring("Authorization: ".length())));
                        pane.setValue(JOptionPane.OK_OPTION);
                    }
                }
            });
            driver.start();try{button(editor,"VERIFY SCENE").doClick();}finally{driver.stop();}
        });
        check(access.get()!=null,"Actual editor displayed local session credentials");return access.get();
    }
    private static HttpResponse<byte[]> request(Access access,String path,String method,String auth)throws Exception{
        var request=HttpRequest.newBuilder(URI.create(access.url()+path)).timeout(java.time.Duration.ofSeconds(30))
            .method(method,HttpRequest.BodyPublishers.noBody());
        if(auth!=null)request.header("Authorization",auth);
        return CLIENT.send(request.build(),HttpResponse.BodyHandlers.ofByteArray());
    }
    private static long revision(Access access)throws Exception{
        var reply=request(access,"/summary","GET",access.bearer());
        check(reply.statusCode()==200,"Observe active editor summary");
        return reply.headers().firstValueAsLong("X-Editor-Revision").orElseThrow();
    }
    private static void verifyObservation(DeveloperLevelEditor editor,Path output,Access access)throws Exception{
        long rev=revision(access);String node="/node?id=prop-0&revision=";
        check(request(access,"/summary","GET",null).statusCode()==401,"Anonymous observation denied");
        check(request(access,"/summary","GET","Bearer invalid").statusCode()==401,"Incorrect token denied");
        check(request(access,"/edit","POST",access.bearer()).statusCode()==405,"Mutation method denied");
        var browser=HttpRequest.newBuilder(URI.create(access.url()+"/summary")).header("Origin","https://example.invalid")
            .header("Authorization",access.bearer()).GET().build();
        check(CLIENT.send(browser,HttpResponse.BodyHandlers.discarding()).statusCode()==403,"Browser origin denied");
        var before=request(access,node+rev,"GET",access.bearer());
        check(before.statusCode()==200,"Read native unsaved prop");
        var expected=new AtomicReference<String>();
        var movedX=new AtomicReference<Double>();
        SwingUtilities.invokeAndWait(()->{
            try{expected.set(EditorAgentCli.json(editor.agentService().node(rev,"prop-0")));
                var hierarchy=DeveloperLevelEditor.class.getDeclaredField("hierarchy");hierarchy.setAccessible(true);
                ((JList<?>)hierarchy.get(editor)).setSelectedIndex(0);
                var xField=DeveloperLevelEditor.class.getDeclaredField("x");xField.setAccessible(true);
                var x=(JSpinner)xField.get(editor);
                movedX.set(editor.agentService().node(rev,"prop-0").result().x()+.25);
                x.setValue(movedX.get());
            }catch(Exception failure){throw new RuntimeException(failure);}
        });
        check(expected.get().equals(new String(before.body(),StandardCharsets.UTF_8)),"Read exact unsaved native row");
        check(request(access,node+rev,"GET",access.bearer()).statusCode()==409,"Human inspector move invalidates revision");
        long changed=revision(access);
        var after=request(access,node+changed,"GET",access.bearer());
        SwingUtilities.invokeAndWait(()->{
            try{var observed=editor.agentService().node(changed,"prop-0");
                check(observed.result().x()==movedX.get(),"Manual X move reached native state");
                expected.set(EditorAgentCli.json(observed));}
            catch(Exception failure){throw new RuntimeException(failure);}
        });
        check(after.statusCode()==200 && expected.get().equals(new String(after.body(),StandardCharsets.UTF_8)),"Read native row after human move");
        var batch=request(access,"/nodes?revision="+changed+"&id=prop-1&id=prop-0","GET",access.bearer());
        SwingUtilities.invokeAndWait(()->{
            try{expected.set(EditorAgentCli.json(editor.agentService().nodes(changed,java.util.List.of("prop-1","prop-0"))));}
            catch(Exception failure){throw new RuntimeException(failure);}
        });
        check(batch.statusCode()==200 && expected.get().equals(new String(batch.body(),StandardCharsets.UTF_8)),"Ordered sparse observation");
        var changes=request(access,"/changes?revision="+changed+"&offset=0&limit=1","GET",access.bearer());
        check(changes.statusCode()==200 && new String(changes.body(),StandardCharsets.UTF_8).contains("\"prop-0\""),"Expose human move metadata");
        check(request(access,"/test?revision="+changed+"&id=mission-runtime-parity","GET",access.bearer()).statusCode()==200,"Read focused diagnostic");
        var capture=request(access,"/capture?revision="+changed,"GET",access.bearer());
        var image=javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(capture.body()));
        check(capture.statusCode()==200 && image!=null && image.getWidth()==560 && image.getHeight()==350,"Real renderer capture");
        Files.write(output.resolve("local-observation-house.png"),capture.body());
    }
    public static void main(String[] args)throws Exception{
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
                    try{castle.inspect();throw new AssertionError("Old Castle service remains active");}
                    catch(IllegalStateException expected){}
                }catch(Exception failure){throw new RuntimeException(failure);}
            });
            var access=openAccess(editor.get());
            verifyObservation(editor.get(),output,access);
            SwingUtilities.invokeAndWait(()->{
                try{
                    var open=DeveloperLevelEditor.class.getDeclaredMethod("openMission",HouseMap.Site.class);
                    open.setAccessible(true);open.invoke(editor.get(),HouseMap.Site.HOUSE);
                }catch(Exception failure){throw new RuntimeException(failure);}
            });
            try{request(access,"/summary","GET",access.bearer());throw new AssertionError("Old scene session is reachable");}
            catch(java.io.IOException expected){}
            var next=openAccess(editor.get());
            check(request(next,"/summary","GET",access.bearer()).statusCode()==401,"Old scene token refused on new session");
            SwingUtilities.invokeAndWait(()->window.get().dispose());
            SwingUtilities.invokeAndWait(()->{});
            try{request(next,"/summary","GET",next.bearer());throw new AssertionError("Closed editor is reachable");}
            catch(java.io.IOException expected){}
        }finally{
            SwingUtilities.invokeAndWait(()->{if(window.get()!=null)window.get().dispose();});
            SwingUtilities.invokeAndWait(()->{
                if(editor.get()!=null){try{editor.get().agentService();throw new AssertionError("Closed editor exposed");}
                catch(IllegalStateException expected){}}
            });
            try(var files=Files.walk(home)){files.sorted(java.util.Comparator.reverseOrder()).forEach(p->{try{Files.deleteIfExists(p);}catch(Exception ignored){}});}
        }
        System.out.println("PASS real Windows editor: mission/Castle recovery and history, authenticated loopback observation of an unsaved inspector move, PNG capture, scene replacement and listener shutdown");
    }
}
