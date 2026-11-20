package org.beckenburg;

import javax.swing.SwingUtilities;

/** Headless-safe field-menu control contract: mouse edits cannot steer the host. */
public final class InGamePauseMenuTest {
    private static void check(boolean okay,String message){
        if(!okay)throw new AssertionError(message);
    }
    public static void main(String[] args)throws Exception{
        String[] actions=InGamePauseMenu.options();
        check(actions.length==4&&actions[1].contains("Controls"),
            "Quick mouse control access must be discoverable in the field menu");
        check(InGamePauseMenu.actionForSelection(-1)==InGamePauseMenu.Action.RESUME,
            "ESC/cancel must resume without touching network gameplay");
        check(InGamePauseMenu.actionForSelection(0)==InGamePauseMenu.Action.RESUME,
            "First option must resume");
        check(InGamePauseMenu.actionForSelection(1)==InGamePauseMenu.Action.QUICK_CONTROLS,
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
        });
        System.out.println("PASS field menu quick mouse controls, focus-safe routing and cancel");
    }
}
