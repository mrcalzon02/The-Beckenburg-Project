package org.beckenburg;

import java.awt.Component;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.BorderLayout;
import java.util.Objects;
import javax.swing.*;

/** Universal field pause/leave control: never requires physically finding VAN. */
public final class InGamePauseMenu {
    private InGamePauseMenu(){}
    public enum Action { RESUME, FIELD_SETTINGS, RETURN_TO_LOBBY, EXIT_GAME }
    public static String[] options(){
        return new String[]{"Resume", "Field Settings (Controls / Graphics)", "Return to Lobby", "Exit Game"};
    }
    /** Selection itself must be testable without showing a Swing dialog. */
    static Action actionForSelection(int index){
        return switch(index){
            case 1 -> Action.FIELD_SETTINGS;
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

    /** Two immediately usable field tabs; the complete six-tab options screen
     * remains accessible in Castle for display mode, lighting and audio. */
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
        view.add(PsxMenuScreen.label("Controls / changes take effect after APPLY"),BorderLayout.NORTH);
        view.add(fields,BorderLayout.CENTER);
        view.add(PsxMenuScreen.label("The host and other hunters keep moving while this menu is open."),
            BorderLayout.SOUTH);
        view.setPreferredSize(new Dimension(480,180));
        return new ControlsForm(view,sensitivity,invertY);
    }

    record FieldSettingsForm(JPanel view,JTabbedPane tabs,ControlsForm mouse,
                             JComboBox<GraphicsOptions.Detail> detail,
                             JComboBox<GraphicsOptions.Upscale> upscale,
                             JComboBox<GraphicsOptions.Antialias> antialias,
                             JSlider fieldOfView){}
    private static JPanel settingsRow(String text,JComponent input){
        JPanel row=new JPanel(new BorderLayout(12,0));
        row.setOpaque(false);
        row.add(PsxMenuScreen.label(text),BorderLayout.WEST);
        row.add(input,BorderLayout.CENTER);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE,43));
        return row;
    }
    static FieldSettingsForm fieldSettingsForm(MousePreferences.Settings mouse,
                                               GraphicsOptions graphics){
        Objects.requireNonNull(mouse);
        Objects.requireNonNull(graphics);
        ControlsForm controls=controlsForm(mouse);
        JComboBox<GraphicsOptions.Detail> detail=
            new JComboBox<>(GraphicsOptions.Detail.values());
        detail.setName("pause.graphics.detail");
        detail.setSelectedItem(graphics.detail());
        detail.getAccessibleContext().setAccessibleName("Internal rendering detail");

        JComboBox<GraphicsOptions.Upscale> upscale=
            new JComboBox<>(GraphicsOptions.Upscale.values());
        upscale.setName("pause.graphics.upscale");
        upscale.setSelectedItem(graphics.upscale());

        JComboBox<GraphicsOptions.Antialias> aa=
            new JComboBox<>(GraphicsOptions.Antialias.values());
        aa.setName("pause.graphics.antialias");
        aa.setSelectedItem(graphics.antialias());

        JSlider fieldOfView=new JSlider(50,120,graphics.fieldOfView());
        fieldOfView.setName("pause.graphics.fieldOfView");
        fieldOfView.setMajorTickSpacing(10);
        fieldOfView.setPaintTicks(true);
        fieldOfView.setOpaque(false);
        fieldOfView.setForeground(new Color(237,225,207));
        JLabel fovLabel=PsxMenuScreen.label("Field of view / "+graphics.fieldOfView()+" deg");
        fieldOfView.addChangeListener(e->
            fovLabel.setText("Field of view / "+fieldOfView.getValue()+" deg"));

        JPanel graphicsPanel=new JPanel();
        graphicsPanel.setName("pause.graphics");
        graphicsPanel.setOpaque(false);
        graphicsPanel.setLayout(new BoxLayout(graphicsPanel,BoxLayout.Y_AXIS));
        graphicsPanel.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        graphicsPanel.add(settingsRow("Internal rendering resolution",detail));
        graphicsPanel.add(Box.createVerticalStrut(8));
        graphicsPanel.add(settingsRow("Image upscaling",upscale));
        graphicsPanel.add(Box.createVerticalStrut(8));
        graphicsPanel.add(settingsRow("Edge antialiasing",aa));
        graphicsPanel.add(Box.createVerticalStrut(8));
        graphicsPanel.add(settingsRow("Field of view",fieldOfView));
        graphicsPanel.add(Box.createVerticalStrut(10));
        graphicsPanel.add(PsxMenuScreen.label(
            "Higher detail may reduce FPS. Display/window modes remain unchanged."));

        JTabbedPane tabs=new JTabbedPane(JTabbedPane.TOP,JTabbedPane.SCROLL_TAB_LAYOUT);
        tabs.setName("pause.settings.tabs");
        tabs.addTab("CONTROLS",PsxMenuScreen.scroll(controls.view()));
        tabs.addTab("GRAPHICS",PsxMenuScreen.scroll(graphicsPanel));
        tabs.getAccessibleContext().setAccessibleName("Field settings categories");
        PsxHorrorUi.styleTabs(tabs);
        tabs.setPreferredSize(new Dimension(548,270));

        JPanel view=new JPanel(new BorderLayout(8,10));
        view.setName("pause.settings");
        view.setOpaque(false);
        view.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        view.add(PsxMenuScreen.label("FIELD SETTINGS / APPLY saves both tabs"),BorderLayout.NORTH);
        view.add(tabs,BorderLayout.CENTER);
        view.add(PsxMenuScreen.label(
            "Live investigation continues; the full settings menu remains at Castle."),
            BorderLayout.SOUTH);
        view.setPreferredSize(new Dimension(574,337));
        return new FieldSettingsForm(view,tabs,controls,detail,upscale,aa,fieldOfView);
    }
    /** Copy only live-safe rendering parameters, never window or display mode. */
    static GraphicsOptions selectedGraphics(GraphicsOptions before,FieldSettingsForm form){
        return new GraphicsOptions(before.mode(),before.display(),
            (GraphicsOptions.Detail)form.detail().getSelectedItem(),
            before.aspect(),(GraphicsOptions.Upscale)form.upscale().getSelectedItem(),
            (GraphicsOptions.Antialias)form.antialias().getSelectedItem(),
            form.fieldOfView().getValue(),before.brightness(),before.contrast(),
            before.scanlines(),before.vignette(),before.grain(),before.chromatic());
    }
    static MousePreferences.Settings selectedMouse(FieldSettingsForm form){
        return new MousePreferences.Settings(form.mouse().sensitivity().getValue(),
            form.mouse().invertY().isSelected());
    }
    private static void fieldSettings(Component parent){
        GraphicsOptions currentGraphics=GraphicsOptions.current();
        MousePreferences.Settings currentMouse=MousePreferences.current();
        FieldSettingsForm form=fieldSettingsForm(currentMouse,currentGraphics);
        int selected=GameDialogs.showOptionDialog(parent,form.view(),
            "Field / Controls and Graphics",GameDialogs.DEFAULT_OPTION,GameDialogs.PLAIN_MESSAGE,
            null,new String[]{"APPLY","CANCEL"},"APPLY");
        if(selected!=0)return;
        GraphicsOptions updatedGraphics=selectedGraphics(currentGraphics,form);
        MousePreferences.Settings updatedMouse=selectedMouse(form);
        // Cancel is a pure no-op; apply only changed values to prevent
        // unnecessary renderer configuration and preference writes.
        if(!updatedGraphics.equals(currentGraphics))updatedGraphics.save();
        if(!updatedMouse.equals(currentMouse))MousePreferences.save(updatedMouse);
    }
    public static void execute(Component parent){
        Action requested=choose(parent);
        if(requested==Action.FIELD_SETTINGS){
            fieldSettings(parent);
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
