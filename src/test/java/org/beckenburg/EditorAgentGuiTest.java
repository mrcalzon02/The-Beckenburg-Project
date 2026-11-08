package org.beckenburg;

import org.beckenburg.editor.EditorAgentProtocol;

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
    static <T> T component(Container parent,Class<T> type){
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
    private static void verifyMismatchThroughMenu(DeveloperLevelEditor editor){
        var report=new AtomicReference<String>();
        Timer driver=new Timer(50,event->{
            for(Window window:Window.getWindows())if(window instanceof JDialog dialog && dialog.isShowing()){
                var pane=component(dialog,JOptionPane.class);if(pane==null)continue;
                switch(dialog.getTitle()){
                    case "Scene verification" -> {
                        component(dialog,JComboBox.class).setSelectedItem("VERIFY POSITION");
                        pane.setInputValue("VERIFY POSITION");pane.setValue(JOptionPane.OK_OPTION);
                    }
                    case "Verify position" -> {
                        component(dialog,JTextField.class).setText("123456");pane.setValue(JOptionPane.OK_OPTION);
                    }
                    case "Scene evidence" -> {report.set(component(dialog,JTextArea.class).getText());pane.setValue(JOptionPane.OK_OPTION);}
                    case "Scene verification failed" -> pane.setValue(JOptionPane.OK_OPTION);
                }
            }
        });
        driver.start();try{button(editor,"VERIFY SCENE").doClick();}finally{driver.stop();}
        check(report.get()!=null && report.get().contains("Position matches: false") &&
            report.get().contains("Saved document: DIFFERS"),"Human position proof reported mismatch and unsaved document");
    }
    private static Object member(Object instance,String name)throws Exception{
        var field=instance.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(instance);
    }
    private static void verifyWalkabilityMenu(DeveloperLevelEditor editor,CastleLevelEdits.Prop prop){
        var report=new AtomicReference<String>();
        Timer driver=new Timer(50,event->{
            for(Window window:Window.getWindows())if(window instanceof JDialog dialog&&dialog.isShowing()){
                var pane=component(dialog,JOptionPane.class);if(pane==null)continue;
                switch(dialog.getTitle()){
                    case "Scene verification" -> {
                        component(dialog,JComboBox.class).setSelectedItem("CHECK WALKABILITY");
                        pane.setInputValue("CHECK WALKABILITY");pane.setValue(JOptionPane.OK_OPTION);
                    }
                    case "Check walkability" -> {
                        Object[] fields=(Object[])pane.getMessage();
                        ((JTextField)fields[2]).setText(Double.toString(prop.x()));
                        ((JTextField)fields[4]).setText(Double.toString(prop.y()));
                        ((JTextField)fields[6]).setText(Double.toString(prop.z()));
                        pane.setValue(JOptionPane.OK_OPTION);
                    }
                    case "Scene evidence" -> {report.set(component(dialog,JTextArea.class).getText());pane.setValue(JOptionPane.OK_OPTION);}
                    case "Scene verification failed" -> pane.setValue(JOptionPane.OK_OPTION);
                }
            }
        });
        driver.start();try{button(editor,"VERIFY SCENE").doClick();}finally{driver.stop();}
        check(report.get()!=null&&report.get().contains("Blocked or unsupported"),"Human collision query missed the upper furnishing");
    }
    private static void verifyPlaytest(DeveloperLevelEditor editor,HouseMap missionMap,Path output)throws Exception{
        var service=editor.agentService();var before=service.inspect();
        var hierarchy=(JList<?>)member(editor,"hierarchy");hierarchy.setSelectedIndex(0);
        byte[] saved=Files.readAllBytes(before.document());
        var camera=(SceneRenderer)member(editor,"renderer");
        var selection=member(editor,"chosen");check(selection!=null,"Human selection before playtest");
        double[] view={camera.x,camera.z,camera.yaw,camera.pitch,camera.eyeHeight};
        boolean castle=missionMap==null;
        Object dirty=member(editor,castle?"castleDirty":"missionDirty");
        var error=new AtomicReference<Throwable>();var played=new java.util.concurrent.atomic.AtomicBoolean();
        boolean[] turnStarted={false};
        long start=System.nanoTime();
        Timer driver=new Timer(100,event->{
            for(Window w:Window.getWindows())if(w instanceof JDialog dialog && dialog.isShowing()){
                try{
                    if(dialog.getTitle().equals("Playtest failed"))throw new AssertionError("Playtest initialization failed");
                    JPanel game=castle?component(dialog,CastleHubPanel.class):component(dialog,GhostPracticePanel.class);
                    if(game==null)continue;
                    if(System.nanoTime()-start>60_000_000_000L)throw new AssertionError("Playtest did not advance");
                    try{service.summary();throw new AssertionError("Editor service accepted access while playing");}
                    catch(IllegalStateException expected){}
                    if(castle){
                        var hub=(CastleHubPanel)game;var render=(SceneRenderer)member(hub,"renderer");
                        double yaw=((CastleLevelEdits.Snapshot)before.state()).spawnYaw();
                        if(!turnStarted[0]){
                            check(render.yaw==yaw,"Castle gameplay started at the unsaved authored yaw");
                            hub.keyPressed(new java.awt.event.KeyEvent(hub,java.awt.event.KeyEvent.KEY_PRESSED,0,0,java.awt.event.KeyEvent.VK_LEFT,' '));
                            turnStarted[0]=true;
                            return;
                        }
                        if(render.yaw==yaw)return;
                        var profile=(ProfileStore)member(hub,"profile");profile.save();
                        check(!Files.exists(ProfileStore.defaultPath()),"Castle playtest wrote the player profile");
                    }else{
                        var practice=(GhostPracticePanel)game;var playable=(HouseMap)member(practice,"map");
                        check(playable!=missionMap && playable.level()==missionMap.level(),"Playtest must consume the reviewed runtime definition");
                        if(practice.session().tickCount()==0)return;
                        var eye=(SceneRenderer)member(practice,"renderer");
                        double expectedEye=playable.floorY(practice.session().ghostRoom())+1.62;
                        check(Math.abs(eye.eyeHeight-expectedEye)<1e-6,"Practice camera height "+eye.eyeHeight+" differs from authored room eye height "+expectedEye);
                    }
                    var image=new java.awt.image.BufferedImage(game.getWidth(),game.getHeight(),java.awt.image.BufferedImage.TYPE_INT_RGB);
                    var g=image.createGraphics();try{game.paint(g);}finally{g.dispose();}
                    javax.imageio.ImageIO.write(image,"PNG",output.resolve(castle?"castle-playtest.png":"house-playtest.png").toFile());
                    played.set(true);button(dialog,"Return to editor").doClick();
                }catch(Throwable failure){error.set(failure);dialog.dispose();}
            }
        });
        driver.start();try{button(editor,"PLAYTEST").doClick();}finally{driver.stop();}
        if(error.get()!=null)throw new AssertionError("Playtest failed",error.get());
        var after=service.inspect();
        check(played.get() && before.revision()==after.revision() && before.state().equals(after.state()),"Playtest changed authoritative document or revision");
        check(java.util.Arrays.equals(saved,Files.readAllBytes(after.document())),"Playtest changed saved level");
        check(selection==member(editor,"chosen") && dirty.equals(member(editor,castle?"castleDirty":"missionDirty")),"Playtest changed selection or dirty state");
        check(java.util.Arrays.equals(view,new double[]{camera.x,camera.z,camera.yaw,camera.pitch,camera.eyeHeight}),"Playtest changed editor camera");
    }
    private static void verifyPalette(DeveloperLevelEditor editor,boolean mission)throws Exception{
        var palette=DeveloperLevelEditor.class.getDeclaredField("assetPalette");palette.setAccessible(true);
        var visible=component((Container)palette.get(editor),JList.class).getModel();
        check(visible.getSize()>0,"Palette displays runtime assets");
        var service=editor.agentService();long rev=service.summary().revision();
        String fingerprint=null;
        for(int offset=0;offset<visible.getSize();offset+=64){
            var page=service.assets(rev,offset,64).result();
            check(page.total()==visible.getSize() && page.offset()==offset,"Palette matches native catalog size and position");
            check(fingerprint==null || fingerprint.equals(page.catalogFingerprint()),"Stable catalog identity across pages");
            fingerprint=page.catalogFingerprint();
            for(int i=0;i<page.assets().size();i++){
                var row=page.assets().get(i);
                check(row.equals(visible.getElementAt(offset+i)),"Service asset differs from actual editor palette");
                if(mission)check(row.category().equals("Models"),"Mission has a Castle-only asset category");
            }
        }
    }
    private static final HttpClient CLIENT=HttpClient.newHttpClient();
    private static Access openAccess(DeveloperLevelEditor editor)throws Exception{
        return openAccess(editor,"observe");
    }
    private static Access openAccess(DeveloperLevelEditor editor,String scope)throws Exception{
        boolean edit=!scope.equals("observe");
        var access=new AtomicReference<Access>();
        SwingUtilities.invokeAndWait(()->{
            Timer driver=new Timer(50,event->{
                for(Window window:Window.getWindows())if(window instanceof JDialog dialog && dialog.isShowing()){
                    var pane=component(dialog,JOptionPane.class);if(pane==null)continue;
                    if(dialog.getTitle().equals("Scene verification")){
                        var choices=component(dialog,JComboBox.class);
                        String action=edit?"START LOCAL EDITING":"START LOCAL OBSERVATION";
                        choices.setSelectedItem(action);
                        pane.setInputValue(action);pane.setValue(JOptionPane.OK_OPTION);
                    }else if(dialog.getTitle().equals("Local editing access")){
                        component(dialog,JCheckBox.class).setSelected(scope.equals("save"));pane.setValue(JOptionPane.OK_OPTION);
                    }else if(dialog.getTitle().equals(edit?"Editing access to this editor scene":"Read-only access to this editor scene")){
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
    private static void verifyMcp(Access access,String scope)throws Exception{
        String python=System.getenv("EDITOR_MCP_PYTHON");
        if(python==null)return;
        var process=new ProcessBuilder(python,"tools/verify_editor_mcp.py",scope).inheritIO();
        process.environment().put("EDITOR_AGENT_URL",access.url());
        process.environment().put("EDITOR_AGENT_TOKEN",access.bearer().substring("Bearer ".length()));
        var child=process.start();
        try{
            check(child.waitFor(90,java.util.concurrent.TimeUnit.SECONDS) && child.exitValue()==0,"MCP client acceptance: "+scope);
        }finally{if(child.isAlive())child.destroyForcibly();}
    }
    private static void verifyHumanCellarDoor(DeveloperLevelEditor editor){
        String slot="portal-v-3700-2200";
        var map=new CastleHubMap();
        check(map.traversable(37,22,-3),"Empty lower aperture must be passable");
        var tabs=component(editor,JTabbedPane.class);
        int old=tabs.getSelectedIndex();tabs.setSelectedIndex(tabs.indexOfTab("Paint & nav"));
        Timer driver=new Timer(50,e->{
            for(Window window:Window.getWindows())if(window instanceof JDialog dialog&&dialog.isShowing()&&dialog.getTitle().equals("Castle door sockets")){
                var pane=component(dialog,JOptionPane.class);var fields=(Object[])pane.getMessage();
                ((JComboBox<?>)fields[1]).setSelectedItem(slot);
                ((JComboBox<?>)fields[4]).setSelectedItem(CastleDoorSlots.Type.WOOD);
                pane.setValue(JOptionPane.OK_OPTION);
            }
        });
        driver.start();try{button(editor,"DOOR SOCKETS…").doClick();}finally{driver.stop();tabs.setSelectedIndex(old);}
        check(CastleLevelEdits.current().doorType(slot)==CastleDoorSlots.Type.WOOD&&!map.traversable(37,22,-3),"Human door installation must block the game body");
        check(!FixtureLighting.castleRayClear(map,36,-1.6,22,38,-1.6,22),"Closed lower door must occlude the game ray");
        map.toggleDoor(slot);
        check(map.traversable(37,22,-3)&&FixtureLighting.castleRayClear(map,36,-1.6,22,38,-1.6,22),"Opened leaf must clear body and ray");
        button(editor,"UNDO").doClick();
        check(CastleLevelEdits.current().doorType(slot)==CastleDoorSlots.Type.EMPTY,"Human undo must remove the mounted leaf");
    }
    static void verifyCastleMcpRepair(DeveloperLevelEditor editor)throws Exception{
        SwingUtilities.invokeAndWait(()->{
            verifyHumanCellarDoor(editor);
            var service=editor.agentService();var rev=service.summary().revision();
            var spawn=service.node(rev,"start").result();
            service.transact(rev,"put-surface",Map.of("id","mcp-test-divider","kind","WALL",
                "x0",String.valueOf(spawn.x()-1),"z0",String.valueOf(spawn.z()),"x1",String.valueOf(spawn.x()+1),"z1",String.valueOf(spawn.z()),
                "base","0","height","2","material","0"));
        });
        var writable=openAccess(editor,"save");verifyMcp(writable,"save");
        SwingUtilities.invokeAndWait(()->{
            var prop=CastleLevelEdits.current().propById("mcp-upper-lodging-chair");
            check(prop!=null&&prop.upper()&&prop.y()==CastleHubArchitecturalPlan.GROUND_CEILING,"Agent furnishing is on the actual upper floor");
            var map=new CastleHubMap();
            check(!map.traversable(prop.x(),prop.z(),prop.y())&&map.traversable(prop.x(),prop.z()+.85,prop.y()),"Game collision and nearby aisle");
            verifyWalkabilityMenu(editor,prop);
            var undo=button(editor,"UNDO");undo.doClick();
            check(CastleLevelEdits.current().propById(prop.id())==null&&map.traversable(prop.x(),prop.z(),prop.y()),"One human undo removed native furnishing and collider");
            check(CastleLevelEdits.current().surfaceById("mcp-test-divider")!=null,"Human undo restored the agent-removed wall");
            undo.doClick();undo.doClick();
            check(CastleLevelEdits.current().surfaceById("mcp-test-divider")==null,"Fixture edits reversed through shared history");
        });
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
        check(request(access,"/playtest?revision="+rev,"POST",access.bearer()).statusCode()==405,"Observation scope cannot start gameplay");
        var browser=HttpRequest.newBuilder(URI.create(access.url()+"/summary")).header("Origin","https://example.invalid")
            .header("Authorization",access.bearer()).GET().build();
        check(CLIENT.send(browser,HttpResponse.BodyHandlers.discarding()).statusCode()==403,"Browser origin denied");
        var before=request(access,node+rev,"GET",access.bearer());
        check(before.statusCode()==200,"Read native unsaved prop");
        var expected=new AtomicReference<String>();
        var movedX=new AtomicReference<Double>();
        SwingUtilities.invokeAndWait(()->{
            try{expected.set(EditorAgentProtocol.json(editor.agentService().node(rev,"prop-0")));
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
                expected.set(EditorAgentProtocol.json(observed));}
            catch(Exception failure){throw new RuntimeException(failure);}
        });
        check(after.statusCode()==200 && expected.get().equals(new String(after.body(),StandardCharsets.UTF_8)),"Read native row after human move");
        var target=new AtomicReference<org.beckenburg.editor.EditorProject.Node>();
        SwingUtilities.invokeAndWait(()->target.set(editor.agentService().node(changed,"prop-0").result()));
        var p=target.get();
        var match=request(access,"/verify-position?revision="+changed+"&id=prop-0&x="+p.x()+"&y="+p.y()+"&z="+p.z(),"GET",access.bearer());
        var proof=new String(match.body(),StandardCharsets.UTF_8);
        check(match.statusCode()==200 && proof.contains("\"verified\":true") && proof.contains("\"committed\":true") &&
            proof.contains("\"diskState\":\"DIFFERS\""),"Prove human move independently of saved native file");
        SwingUtilities.invokeAndWait(()->verifyMismatchThroughMenu(editor));
        var batch=request(access,"/nodes?revision="+changed+"&id=prop-1&id=prop-0","GET",access.bearer());
        SwingUtilities.invokeAndWait(()->{
            try{expected.set(EditorAgentProtocol.json(editor.agentService().nodes(changed,java.util.List.of("prop-1","prop-0"))));}
            catch(Exception failure){throw new RuntimeException(failure);}
        });
        check(batch.statusCode()==200 && expected.get().equals(new String(batch.body(),StandardCharsets.UTF_8)),"Ordered sparse observation");
        var changes=request(access,"/changes?revision="+changed+"&offset=0&limit=1","GET",access.bearer());
        check(changes.statusCode()==200 && new String(changes.body(),StandardCharsets.UTF_8).contains("\"prop-0\""),"Expose human move metadata");
        check(request(access,"/test?revision="+changed+"&id=mission-runtime-parity","GET",access.bearer()).statusCode()==200,"Read focused diagnostic");
        var assets=request(access,"/assets?revision="+changed+"&offset=0&limit=2","GET",access.bearer());
        SwingUtilities.invokeAndWait(()->{
            try{expected.set(EditorAgentProtocol.json(editor.agentService().assets(changed,0,2)));}
            catch(Exception failure){throw new RuntimeException(failure);}
        });
        check(assets.statusCode()==200 && expected.get().equals(new String(assets.body(),StandardCharsets.UTF_8)),"Return exact native asset page over HTTP");
        var capture=request(access,"/capture?revision="+changed,"GET",access.bearer());
        var image=javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(capture.body()));
        check(capture.statusCode()==200 && image!=null && image.getWidth()==560 && image.getHeight()==350,"Real renderer capture");
        Files.write(output.resolve("local-observation-house.png"),capture.body());
    }
    public static void main(String[] args)throws Exception{
        var backgroundFailure=new AtomicReference<Throwable>();var priorHandler=Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread,failure)->{backgroundFailure.compareAndSet(null,failure);failure.printStackTrace();});
        Path output=Path.of(args.length==0?"build/editor-agent-evidence":args[0]).toAbsolutePath();Files.createDirectories(output);
        Path home=Files.createTempDirectory("editor-agent-gui-");
        System.setProperty("user.home",home.toString());
        AtomicReference<JFrame> window=new AtomicReference<>();
        AtomicReference<DeveloperLevelEditor> editor=new AtomicReference<>();
        var joined=new java.util.concurrent.CountDownLatch(1);
        try(var host=new LanHost("127.0.0.1",0,GhostIdentity.Type.SPIRIT);
            var peer=new LanClient("127.0.0.1",host.port(),false,"Observer",line->{if(line.startsWith("SITE|"))joined.countDown();})){
            check(joined.await(10,java.util.concurrent.TimeUnit.SECONDS) && host.connected()==1,"Real multiplayer peer joined independent host");
            var hostBefore=host.devAdminSnapshot();
            SwingUtilities.invokeAndWait(()->{
                DeveloperLevelEditor.open(null);
                for(Window w:Window.getWindows())if(w instanceof JFrame f&&f.getContentPane() instanceof DeveloperLevelEditor e){
                    window.set(f);editor.set(e);break;
                }
                check(editor.get()!=null,"Actual human editor opened");
            });
            if(System.getenv("EDITOR_MCP_PYTHON")!=null)verifyCastleMcpRepair(editor.get());
            SwingUtilities.invokeAndWait(()->{
                try{
                    var e=editor.get();var castle=e.agentService();var state=castle.inspect();
                    verifyPalette(e,false);
                    castle.capture(state.revision(),output.resolve("castle.png"));
                    double originalYaw=CastleLevelEdits.current().spawn()[2];
                    castle.save(state.revision());
                    castle.transact(castle.inspect().revision(),"set-spawn",Map.of("x","13.5","z","19","yaw","1.125"));
                    castle.save(castle.inspect().revision());
                    Files.writeString(castle.inspect().document(),"format=invalid");
                    var corrupt=castle.verifyPosition(castle.summary().revision(),"start",13.5,0,19).result();
                    check(corrupt.verified() && corrupt.diskState()==org.beckenburg.editor.EditorAgentService.DiskState.UNREADABLE &&
                        Files.readString(castle.inspect().document()).equals("format=invalid"),"Corrupt Castle readback must leave disk untouched");
                    recoverThroughMenu(e);
                    check(CastleLevelEdits.current().spawn()[2]==originalYaw,"Human Castle recovery uses same singleton");
                    button(e,"UNDO").doClick();
                    check(CastleLevelEdits.current().spawn()[2]==1.125,"Human undo restores pre-recovery Castle edits");
                    verifyPlaytest(e,null,output);
                    button(e,"REDO").doClick();
                    check(CastleLevelEdits.current().spawn()[2]==originalYaw,"Playtest preserved Castle redo");
                    button(e,"UNDO").doClick();
                    check(CastleLevelEdits.current().spawn()[2]==1.125,"Playtest preserved Castle undo");
                    var open=DeveloperLevelEditor.class.getDeclaredMethod("openMission",HouseMap.Site.class);
                    open.setAccessible(true);open.invoke(e,HouseMap.Site.HOUSE);
                    var finish=DeveloperLevelEditor.class.getDeclaredMethod("finishSceneOpen");finish.setAccessible(true);finish.invoke(e);
                    verifyPalette(e,true);
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
                    verifyPlaytest(e,(HouseMap)editedMap,output);
                    var undo=button(e,"UNDO");check(undo!=null,"Human undo button exists");undo.doClick();
                    check(service.inspect().hierarchy().get(0).x()==node.x(),"Human undo reverses agent edit");
                    var redo=button(e,"REDO");redo.doClick();
                    check(service.inspect().hierarchy().get(0).x()==node.x()+.125,"Human redo restores agent edit");
                    service.save(service.inspect().revision());
                    service.reopen(service.inspect().revision());
                    check(service.test(service.inspect().revision(),"mission-runtime-parity").result().stream().allMatch(c->c.passed()),
                        "Real editor's HouseMap has same reopened props and colliders");
                    service.capture(service.inspect().revision(),output.resolve("house.png"));
                    Files.writeString(output.resolve("house-inspection.json"),EditorAgentProtocol.json(service.inspect()));
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
                    Files.writeString(output.resolve("house-rebuilt-inspection.json"),EditorAgentProtocol.json(service.inspect()));
                    try{castle.inspect();throw new AssertionError("Old Castle service remains active");}
                    catch(IllegalStateException expected){}
                }catch(Exception failure){throw new RuntimeException(failure);}
            });
            var access=openAccess(editor.get());
            check(host.connected()==1 && hostBefore.equals(host.devAdminSnapshot()),"Playtests changed independent LAN host state");
            verifyObservation(editor.get(),output,access);
            verifyMcp(access,"observe");
            SwingUtilities.invokeAndWait(()->{
                try{
                    var open=DeveloperLevelEditor.class.getDeclaredMethod("openMission",HouseMap.Site.class);
                    open.setAccessible(true);open.invoke(editor.get(),HouseMap.Site.HOUSE);
                }catch(Exception failure){throw new RuntimeException(failure);}
            });
            try{request(access,"/summary","GET",access.bearer());throw new AssertionError("Old scene session is reachable");}
            catch(java.io.IOException expected){}
            var next=openAccess(editor.get(),"edit");
            check(request(next,"/save?revision="+revision(next),"POST",next.bearer()).statusCode()==403,"Edit access does not grant native save");
            check(request(next,"/reopen?revision="+revision(next),"POST",next.bearer()).statusCode()==403,"Edit access does not grant native reopen");
            verifyMcp(next,"edit");
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
            Thread.setDefaultUncaughtExceptionHandler(priorHandler);
        }
        if(backgroundFailure.get()!=null)throw new AssertionError("Native editor background thread failed",backgroundFailure.get());
        System.out.println("PASS real Windows editor: isolated Castle and House gameplay, exact unsaved-state return, unaffected connected LAN host, native history, authenticated observation and renderer capture");
    }
}
