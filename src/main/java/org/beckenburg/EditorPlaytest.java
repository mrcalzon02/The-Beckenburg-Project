package org.beckenburg;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Files;
import javax.swing.*;

/** Modal human playtest using the game's own local panels and session-owned gameplay state. */
final class EditorPlaytest {
    static void run(JFrame owner,HouseMap activeMission)throws IOException{
        var dialog=new JDialog(owner,activeMission==null?"Castle playtest":"Mission spectral playtest",true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        var back=new JButton("Return to editor");back.addActionListener(e->dialog.dispose());
        dialog.add(back,BorderLayout.SOUTH);
        java.nio.file.Path profileFile=null;
        Runnable stop=null;
        try{
            JPanel game;
            if(activeMission==null){
                profileFile=Files.createTempFile("beckenburg-playtest-",".properties");
                var hub=new CastleHubPanel(new ProfileStore(profileFile),new PlaytestActions(dialog));
                stop=hub::close;game=hub;
            }else{
                var playable=new HouseMap(activeMission.site(),activeMission.level());
                var practice=new GhostPracticePanel(playable,dialog::dispose,"EDITOR");
                stop=practice::close;game=practice;
            }
            game.setPreferredSize(new Dimension(880,550));dialog.add(game,BorderLayout.CENTER);
            dialog.addWindowListener(new WindowAdapter(){
                @Override public void windowOpened(WindowEvent event){game.requestFocusInWindow();}
            });
            dialog.pack();dialog.setLocationRelativeTo(owner);dialog.setVisible(true);
        }finally{
            try{if(stop!=null)stop.run();}
            finally{dialog.dispose();if(profileFile!=null)Files.deleteIfExists(profileFile);}
        }
    }
    /** Leaving local Castle gameplay returns to the editor. No production menu or network session is opened. */
    private static final class PlaytestActions implements CastleHubPanel.Actions{
        private final JDialog dialog;
        private int role;
        PlaytestActions(JDialog dialog){this.dialog=dialog;}
        public void begin(){dialog.dispose();}
        public void network(){dialog.dispose();}
        public void spectral(){dialog.dispose();}
        public void menu(){dialog.dispose();}
        public void campaign(){dialog.dispose();}
        public void cycleRole(){role=(role+1)%InvestigationSession.Role.values().length;}
        public String role(){return InvestigationSession.Role.values()[role].name();}
        public void cycleWeather(){}
        public String weather(){return "CLEAR";}
    }
}
