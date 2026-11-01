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
                    var open=DeveloperLevelEditor.class.getDeclaredMethod("openMission",HouseMap.Site.class);
                    open.setAccessible(true);open.invoke(e,HouseMap.Site.HOUSE);
                    var service=e.agentService();var before=service.inspect();var node=before.hierarchy().get(0);
                    service.transact(before.revision(),"move-prop",Map.of("id",node.id(),
                        "x",Double.toString(node.x()+.125),"y",Double.toString(node.y()),"z",Double.toString(node.z())));
                    check(service.inspect().hierarchy().get(0).x()==node.x()+.125,"Agent edit visible in active human editor");
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
        }
        System.out.println("PASS real Windows editor: mission service, human undo/redo, save/reopen, runtime parity, Castle/House PNGs and stale/closed scene rejection");
    }
}
