package org.beckenburg;

import java.awt.Component;


/** Universal field pause/leave control: never requires physically finding VAN. */
public final class InGamePauseMenu {
    private InGamePauseMenu(){}
    public enum Action { RESUME, RETURN_TO_LOBBY, EXIT_GAME }
    public static String[] options(){return new String[]{"Resume", "Return to Lobby", "Exit Game"};}
    public static boolean confirmExit(Component parent){
        return GameDialogs.showConfirmDialog(parent,
            "Exit Beckenburg now? An active investigation will not receive a payout.",
            "Exit Game",GameDialogs.YES_NO_OPTION,GameDialogs.WARNING_MESSAGE)
            ==GameDialogs.YES_OPTION;
    }
    public static Action choose(Component parent){
        int selection=GameDialogs.showOptionDialog(parent,
            "Investigation paused", "Beckenburg / Pause",
            GameDialogs.DEFAULT_OPTION,GameDialogs.QUESTION_MESSAGE,
            null,options(),options()[0]);
        return switch(selection){
            case 1 -> Action.RETURN_TO_LOBBY;
            case 2 -> confirmExit(parent)?Action.EXIT_GAME:Action.RESUME;
            default -> Action.RESUME;
        };
    }
    public static void execute(Component parent){
        Action requested=choose(parent);
        if(requested==Action.EXIT_GAME)System.exit(0);
        if(requested==Action.RETURN_TO_LOBBY){
            for(Component at=parent;at!=null;at=at.getParent())if(at instanceof javax.swing.JComponent component
                    &&component.getClientProperty("beckenburg.returnToLobby") instanceof Runnable back){back.run();return;}
            java.awt.Window window=javax.swing.SwingUtilities.getWindowAncestor(parent);
            if(window!=null)window.dispose();
        }else if(parent!=null)parent.requestFocusInWindow();
    }
}
