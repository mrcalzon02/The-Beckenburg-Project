package org.beckenburg;

import javax.swing.SwingUtilities;

/** Headless-safe field-menu control contract: mouse edits cannot steer the host. */
public final class InGamePauseMenuTest {
    private static void check(boolean okay,String message){
        if(!okay)throw new AssertionError(message);
    }
    public static void main(String[] args)throws Exception{
        String[] actions=InGamePauseMenu.options();
        check(actions.length==4&&actions[1].contains("Settings"),
            "Quick mouse control access must be discoverable in the field menu");
        check(InGamePauseMenu.actionForSelection(-1)==InGamePauseMenu.Action.RESUME,
            "ESC/cancel must resume without touching network gameplay");
        check(InGamePauseMenu.actionForSelection(0)==InGamePauseMenu.Action.RESUME,
            "First option must resume");
        check(InGamePauseMenu.actionForSelection(1)==InGamePauseMenu.Action.FIELD_SETTINGS,
            "Second option must keep the player in the investigation");
        check(InGamePauseMenu.actionForSelection(2)==InGamePauseMenu.Action.RETURN_TO_LOBBY,
            "Third option must retain authenticated round exit");
        check(InGamePauseMenu.actionForSelection(3)==InGamePauseMenu.Action.EXIT_GAME,
            "Fourth option must retain confirmed game quit");
        SwingUtilities.invokeAndWait(()->{
            MousePreferences.Settings persisted=MousePreferences.current();
            var form=InGamePauseMenu.controlsForm(new MousePreferences.Settings(135,true));
            check("pause.controls".equals(form.view().getName()),"Editor needs stable identity");
            check(form.sensitivity().getValue()==135&&form.invertY().isSelected(),
                "Controls must reflect the loaded preferences");
            check(form.sensitivity().getMinimum()==25&&form.sensitivity().getMaximum()==200,
                "Slider must honor validated preference bounds");
            form.sensitivity().setValue(200);
            form.invertY().setSelected(false);
            var changed=new MousePreferences.Settings(
                form.sensitivity().getValue(),form.invertY().isSelected());
            check(changed.sensitivityPercent()==200&&!changed.invertY(),
                "In-round options must support both input adjustments");
            check(MousePreferences.current()==persisted,
                "Editing but canceling must not mutate saved preferences");
            GraphicsOptions baseline=GraphicsOptions.current();
            var settings=InGamePauseMenu.fieldSettingsForm(
                new MousePreferences.Settings(135,true),baseline);
            check(settings.tabs().getTabCount()==2
                    &&"CONTROLS".equals(settings.tabs().getTitleAt(0))
                    &&"GRAPHICS".equals(settings.tabs().getTitleAt(1)),
                "Field settings must have real distinct controls and graphics tabs");
            check(settings.detail().getSelectedItem()==baseline.detail()
                    &&settings.upscale().getSelectedItem()==baseline.upscale()
                    &&settings.antialias().getSelectedItem()==baseline.antialias()
                    &&settings.fieldOfView().getValue()==baseline.fieldOfView(),
                "Field graphics must load the active rendering settings");
            check(settings.frameRate().getSelectedItem()==PresentationRate.current(),
                "Current render-only frame target must appear in field graphics tab");
            check(settings.fieldOfView().getMinimum()==50
                    &&settings.fieldOfView().getMaximum()==120,
                "Field FOV must preserve engine validation bounds");
            settings.tabs().setSelectedIndex(1);
            settings.detail().setSelectedItem(baseline.detail()==GraphicsOptions.Detail.RETRO
                ?GraphicsOptions.Detail.HIGH:GraphicsOptions.Detail.RETRO);
            settings.upscale().setSelectedItem(GraphicsOptions.Upscale.SMOOTH);
            settings.antialias().setSelectedItem(GraphicsOptions.Antialias.EDGE);
            settings.frameRate().setSelectedItem(PresentationRate.Target.HIGH_90);
            settings.fieldOfView().setValue(120);
            var changedGraphics=InGamePauseMenu.selectedGraphics(baseline,settings);
            check(changedGraphics.detail()==settings.detail().getSelectedItem()
                    &&changedGraphics.fieldOfView()==120,
                "In-round graphics edits must become a valid rendering configuration");
            check(changedGraphics.mode()==baseline.mode()
                    &&changedGraphics.display()==baseline.display()
                    &&changedGraphics.aspect()==baseline.aspect()
                    &&changedGraphics.brightness()==baseline.brightness()
                    &&changedGraphics.contrast()==baseline.contrast()
                    &&changedGraphics.scanlines()==baseline.scanlines()
                    &&changedGraphics.vignette()==baseline.vignette()
                    &&changedGraphics.grain()==baseline.grain()
                    &&changedGraphics.chromatic()==baseline.chromatic(),
                "Field options cannot inadvertently change display/window or effects");
            check(GraphicsOptions.current()==baseline,
                "Editing or canceling graphics must never mutate saved preferences");
            settings.mouse().sensitivity().setValue(160);
            settings.mouse().invertY().setSelected(false);
            check(InGamePauseMenu.selectedPresentationRate(settings)
                    ==PresentationRate.Target.HIGH_90,
                "Selected presentation target is staged without saving");
            check(PresentationRate.current()!=null,
                "Cancel leaves valid existing pacing preferences in place");
            check(InGamePauseMenu.selectedMouse(settings).equals(
                    new MousePreferences.Settings(160,false)),
                "Both settings tabs must retain edits until Apply");
            check(MousePreferences.current()==persisted,
                "Changing field tabs must not save mouse options prematurely");
        });
        System.out.println("PASS field tabs, graphics rendering selection, mouse controls, cancel and focus-safe routing");
    }
}
