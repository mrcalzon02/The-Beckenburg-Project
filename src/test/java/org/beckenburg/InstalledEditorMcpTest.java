package org.beckenburg;

import java.awt.Window;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.*;

/** Test-only JVM agent. Drive the normal --editor launch and menus inside the installed executable. */
public final class InstalledEditorMcpTest {
    public static void premain(String unused){
        new Thread(()->{
            try{
                var exe=Path.of(ProcessHandle.current().info().command().orElseThrow()).toRealPath();
                var install=Path.of(System.getenv("EDITOR_MCP_SCRIPT")).toRealPath().getParent().getParent();
                var jar=Path.of(DeveloperLevelEditor.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath();
                if(!exe.equals(install.resolve("Beckenburg.exe").toRealPath()) ||
                    !jar.equals(install.resolve("app/beckenburg-game.jar").toRealPath()))throw new AssertionError("Wrong installed host");
                var editor=new AtomicReference<DeveloperLevelEditor>();
                long end=System.nanoTime()+90_000_000_000L;
                while(editor.get()==null && System.nanoTime()<end){
                    SwingUtilities.invokeAndWait(()->{
                        for(Window w:Window.getWindows())if(w instanceof JFrame frame && frame.isShowing() &&
                            frame.getContentPane() instanceof DeveloperLevelEditor active)editor.set(active);
                    });
                    if(editor.get()==null)Thread.sleep(100);
                }
                if(editor.get()==null)throw new AssertionError("Installed editor never opened");
                if(System.getenv("EDITOR_MCP_PYTHON")==null)throw new AssertionError("MCP client required");
                EditorAgentGuiTest.verifyCastleMcpRepair(editor.get());
                SwingUtilities.invokeAndWait(()->SwingUtilities.getWindowAncestor(editor.get()).dispose());
                Files.writeString(Path.of("build/editor-agent-evidence/installed-mcp.txt"),
                    "PASS " + exe + "\nRuntime " + jar + "\nAdapter " + System.getenv("EDITOR_MCP_SCRIPT") + "\nSource " + System.getenv("GITHUB_SHA"));
                System.exit(0);
            }catch(Throwable failure){failure.printStackTrace();System.exit(1);}
        },"installed-editor-mcp-test").start();
    }
}
