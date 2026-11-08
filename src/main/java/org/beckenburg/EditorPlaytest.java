package org.beckenburg;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.awt.image.BufferedImage;
import java.util.function.BooleanSupplier;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.beckenburg.editor.EditorAgentService;

/** Modal human playtest using the game's own local panels and session-owned gameplay state. */
final class EditorPlaytest {
    static EditorAgentService.Capture run(JFrame owner,HouseMap activeMission,Path capture)throws IOException{
        var dialog=new JDialog(owner,activeMission==null?"Castle playtest":"Mission spectral playtest",true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        var back=new JButton("Return to editor");back.addActionListener(e->dialog.dispose());
        dialog.add(back,BorderLayout.SOUTH);
        java.nio.file.Path profileFile=null;
        Runnable stop=null;
        Timer observer=null;
        var result=new java.util.concurrent.atomic.AtomicReference<EditorAgentService.Capture>();
        var error=new java.util.concurrent.atomic.AtomicReference<IOException>();
        try{
            JPanel game;
            BooleanSupplier advanced;
            if(activeMission==null){
                profileFile=Files.createTempFile("beckenburg-playtest-",".properties");
                var hub=new CastleHubPanel(new ProfileStore(profileFile),new PlaytestActions(dialog));
                stop=hub::close;game=hub;
                advanced=()->hub.gameplaySeconds()>0;
            }else{
                var playable=new HouseMap(activeMission.site(),activeMission.level());
                var practice=new GhostPracticePanel(playable,dialog::dispose,"EDITOR");
                stop=practice::close;game=practice;
                advanced=()->practice.session().tickCount()>0;
            }
            game.setPreferredSize(new Dimension(880,550));dialog.add(game,BorderLayout.CENTER);
            dialog.addWindowListener(new WindowAdapter(){
                @Override public void windowOpened(WindowEvent event){game.requestFocusInWindow();}
            });
            dialog.pack();dialog.setLocationRelativeTo(owner);
            if(capture!=null){
                long deadline=System.nanoTime()+20_000_000_000L;
                observer=new Timer(50,e->{
                    try{
                        if(!game.isShowing()||!advanced.getAsBoolean()){
                            if(System.nanoTime()<deadline)return;
                            throw new IOException("Gameplay did not advance before capture");
                        }
                        var frame=new BufferedImage(game.getWidth(),game.getHeight(),BufferedImage.TYPE_INT_RGB);
                        var g=frame.createGraphics();try{game.paint(g);}finally{g.dispose();}
                        if(!ImageIO.write(frame,"png",capture.toFile()))throw new IOException("PNG writer unavailable");
                        result.set(new EditorAgentService.Capture(capture,frame.getWidth(),frame.getHeight(),game.getClass().getSimpleName()));
                    }catch(IOException|RuntimeException failure){error.set(new IOException("Gameplay capture failed",failure));}
                    ((Timer)e.getSource()).stop();dialog.dispose();
                });
                observer.start();
            }
            dialog.setVisible(true);
            if(error.get()!=null)throw error.get();
            if(capture!=null&&result.get()==null)throw new IOException("Playtest closed before gameplay capture");
            return result.get();
        }finally{
            if(observer!=null)observer.stop();
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
