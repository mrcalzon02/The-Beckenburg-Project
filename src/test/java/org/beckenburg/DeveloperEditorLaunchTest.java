package org.beckenburg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarFile;

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
        String source=Files.readString(Path.of("src/main/java/org/beckenburg/InvestigationGame.java"));
        check(source.contains("JButton editor=button(\"DEVELOPER LEVEL EDITOR\")"),
              "Main menu button");
        check(source.contains("\"OPEN LEVEL EDITOR\""),"Options dialog launch action");
        String ship=Files.readString(Path.of(".github/workflows/ship-installer.yml"));
        check(ship.contains("dist\\Beckenburg\\tools\\LevelEditorKit-Java17.jar"),
              "Installer includes reusable editor files");
        System.out.println("PASS game menu, Options, --editor, Windows shortcut, shipped JAR and kit");
    }
}
