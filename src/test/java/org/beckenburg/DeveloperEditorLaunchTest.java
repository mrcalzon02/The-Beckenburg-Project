package org.beckenburg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarFile;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JButton;

/** Checks packaged launchers and game-menu/Options routes to the real editor. */
public final class DeveloperEditorLaunchTest {
    private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        check(InvestigationGame.editorRequested(new String[]{"--editor"}),"Editor shortcut flag");
        check(InvestigationGame.editorRequested(new String[]{"--extra","--editor"}),"Editor flag after other args");
        check(!InvestigationGame.editorRequested(new String[]{}),"Game menu default");
        check(!InvestigationGame.editorRequested(new String[]{"--diagnostics"}),"Diagnostics unchanged");
        try(var jar=new JarFile("build/beckenburg-game.jar")){
            for(String name:new String[]{
                "org/beckenburg/DeveloperLevelEditor.class",
                "org/beckenburg/CastleEditorProject.class",
                "org/beckenburg/editor/EditorProject.class",
                "org/beckenburg/editor/EditorPalettePanel.class",
                "org/beckenburg/InvestigationGame.class"
            })check(jar.getEntry(name)!=null,"Game JAR missing "+name);
        }
        String installer=Files.readString(Path.of("installer/beckenburg.iss"));
        check(installer.contains("Beckenburg Level Editor")
              &&installer.contains("Parameters: \"--editor\""),
              "Start Menu shortcut launches editor mode");
        // Exercise the real named Extras editor route as a Swing action, without opening a
        // top-level window in headless CI. A missing listener, duplicate listener, or
        // wrong component identity must fail before any source-level contract checks.
        AtomicInteger launches=new AtomicInteger();
        JButton editor=InvestigationGame.editorExtrasRoute(
            new JButton("DEVELOPER LEVEL EDITOR"),launches::incrementAndGet);
        check("extras.editor".equals(editor.getName()),"Editor route identity");
        check("DEVELOPER LEVEL EDITOR".equals(editor.getText()),"Editor route label");
        check(editor.isEnabled(),"Editor route enabled");
        editor.doClick();
        check(launches.get()==1,"Editor route invokes exactly one launcher callback");
        editor.doClick();
        check(launches.get()==2,"Editor route remains usable after first click");
        String source=Files.readString(Path.of("src/main/java/org/beckenburg/InvestigationGame.java"));
        check(source.contains("routes.add(nav(\"start.extras\",\"EXTRAS & SUPPORT\",this::showExtras))"),
              "Main menu opens the Extras screen");
        check(source.contains("list.add(editorExtrasRoute(button(\"DEVELOPER LEVEL EDITOR\"),()->openDeveloperEditor(window)))"),
              "Extras editor action uses the packaged editor launcher");
        check(source.contains("DeveloperLevelEditor.open(parent)"),
              "Editor launcher calls the real editor");
        check(source.contains("routes.add(nav(\"start.options\",\"OPTIONS\",this::openOptions))")
              &&source.contains("optionsScreen=PsxMenuScreen.screen(\"options\""),
              "Main menu opens the real Options screen");
        String ship=Files.readString(Path.of(".github/workflows/ship-installer.yml"));
        check(ship.contains("dist\\Beckenburg\\tools\\LevelEditorKit-Java17.jar"),
              "Installer includes reusable editor files");
        System.out.println("PASS named Extras editor click, Options route, --editor, Windows shortcut, shipped JAR and kit");
    }
}
