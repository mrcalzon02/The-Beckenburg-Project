package org.beckenburg;

import java.awt.Component;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.BorderLayout;
import javax.swing.*;

/** Universal field pause/leave control: never requires physically finding VAN. */
public final class InGamePauseMenu {
    private InGamePauseMenu(){}
    public enum Action { RESUME, QUICK_CONTROLS, RETURN_TO_LOBBY, EXIT_GAME }
    public static String[] options(){
        return new String[]{"Resume", "Quick Mouse Controls", "Return to Lobby", "Exit Game"};
    }
    /** Selection itself must be testable without showing a Swing dialog. */
    static Action actionForSelection(int index){
        return switch(index){
            case 1 -> Action.QUICK_CONTROLS;
            case 2 -> Action.RETURN_TO_LOBBY;
            case 3 -> Action.EXIT_GAME;
            default -> Action.RESUME;
        };
    }
    public static boolean confirmExit(Component parent){
        return GameDialogs.showConfirmDialog(parent,
            "Exit Beckenburg now? An active investigation will not receive a payout.",
            "Exit Game",GameDialogs.YES_NO_OPTION,GameDialogs.WARNING_MESSAGE)
            ==GameDialogs.YES_OPTION;
    }
    public static Action choose(Component parent){
        int selection=GameDialogs.showOptionDialog(parent,
            "FIELD MENU - the investigation continues while this menu is open.",
            "Beckenburg / Field Menu",GameDialogs.DEFAULT_OPTION,
            GameDialogs.QUESTION_MESSAGE,null,options(),options()[0]);
        Action requested=actionForSelection(selection);
        return requested==Action.EXIT_GAME&&!confirmExit(parent)?Action.RESUME:requested;
    }

    /** Live-round shortcut; the complete six-tab options screen stays in Castle. */
    record ControlsForm(JPanel view,JSlider sensitivity,JCheckBox invertY){}
    static ControlsForm controlsForm(MousePreferences.Settings current){
        JSlider sensitivity=new JSlider(25,200,current.sensitivityPercent());
        sensitivity.setName("pause.controls.sensitivity");
        sensitivity.getAccessibleContext().setAccessibleName("Mouse sensitivity percent");
        sensitivity.setMajorTickSpacing(25);
        sensitivity.setMinorTickSpacing(5);
        sensitivity.setPaintTicks(true);
        sensitivity.setOpaque(false);
        sensitivity.setForeground(new Color(237,225,207));

        JLabel value=PsxMenuScreen.label(current.sensitivityPercent()+"%");
        value.setName("pause.controls.sensitivity.value");
        sensitivity.addChangeListener(e->value.setText(sensitivity.getValue()+"%"));
        JPanel heading=new JPanel(new BorderLayout(8,0));
        heading.setOpaque(false);
        heading.add(PsxMenuScreen.label("Mouse look sensitivity"),BorderLayout.WEST);
        heading.add(value,BorderLayout.EAST);

        JCheckBox invertY=new JCheckBox("Invert vertical mouse look",current.invertY());
        invertY.setName("pause.controls.invertY");
        invertY.setOpaque(false);
        invertY.setForeground(new Color(237,225,207));
        invertY.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,14));

        JPanel fields=new JPanel();
        fields.setOpaque(false);
        fields.setLayout(new BoxLayout(fields,BoxLayout.Y_AXIS));
        fields.add(heading);
        fields.add(Box.createVerticalStrut(8));
        fields.add(sensitivity);
        fields.add(Box.createVerticalStrut(10));
        fields.add(invertY);

        JPanel view=new JPanel(new BorderLayout(8,12));
        view.setName("pause.controls");
        view.setOpaque(false);
        view.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        view.add(PsxMenuScreen.label("Quick controls / changes take effect after APPLY"),BorderLayout.NORTH);
        view.add(fields,BorderLayout.CENTER);
        view.add(PsxMenuScreen.label("The host and other hunters keep moving while this menu is open."),
            BorderLayout.SOUTH);
        view.setPreferredSize(new Dimension(480,180));
        return new ControlsForm(view,sensitivity,invertY);
    }
    private static void quickControls(Component parent){
        ControlsForm form=controlsForm(MousePreferences.current());
        int selected=GameDialogs.showOptionDialog(parent,form.view(),
            "Field / Mouse Controls",GameDialogs.DEFAULT_OPTION,GameDialogs.PLAIN_MESSAGE,
            null,new String[]{"APPLY","CANCEL"},"APPLY");
        if(selected==0)MousePreferences.save(new MousePreferences.Settings(
            form.sensitivity().getValue(),form.invertY().isSelected()));
    }
    public static void execute(Component parent){
        Action requested=choose(parent);
        if(requested==Action.QUICK_CONTROLS){
            quickControls(parent);
            if(parent!=null)parent.requestFocusInWindow();
            return;
        }
        if(requested==Action.EXIT_GAME){
            // Route through the owning desktop window so its host/network,
            // audio and hidden OpenGL worker are all stopped before JVM exit.
            java.awt.Window window=javax.swing.SwingUtilities.getWindowAncestor(parent);
            if(window!=null)window.dispatchEvent(new java.awt.event.WindowEvent(
                window,java.awt.event.WindowEvent.WINDOW_CLOSING));
            else DesktopShutdown.requestExit();
            return;
        }
        if(requested==Action.RETURN_TO_LOBBY){
            for(Component at=parent;at!=null;at=at.getParent())if(at instanceof javax.swing.JComponent component
                    &&component.getClientProperty("beckenburg.returnToLobby") instanceof Runnable back){back.run();return;}
            java.awt.Window window=javax.swing.SwingUtilities.getWindowAncestor(parent);
            if(window!=null)window.dispose();
        }else if(parent!=null)parent.requestFocusInWindow();
    }
}
