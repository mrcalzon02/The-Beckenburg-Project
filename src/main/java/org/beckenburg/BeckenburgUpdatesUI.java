package org.beckenburg;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.prefs.Preferences;
import javax.swing.JButton;
import javax.swing.JCheckBox;

import javax.swing.JFrame;
import javax.swing.JLabel;

import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/** Non-blocking, opt-in install flow. Checks are read-only and never interrupt play. */
public final class BeckenburgUpdatesUI {
    private static final Preferences PREFS=Preferences.userNodeForPackage(BeckenburgUpdatesUI.class);
    private static volatile BeckenburgUpdates.Release pending;
    public static String availableVersion(){return pending==null?"":pending.version();}
    private static final long DAY=86400000L;
    private BeckenburgUpdatesUI() { }
    public static void checkOnStartup(JFrame parent){
        if(GraphicsEnvironment.isHeadless()||!PREFS.getBoolean("check_updates",true))return;
        long now=System.currentTimeMillis();
        if(now-PREFS.getLong("last_update_check",0L)<DAY){
            BeckenburgDeveloperTrace.update("auto-skip","daily throttle active");return;
        }
        PREFS.putLong("last_update_check",now);
        BeckenburgDeveloperTrace.update("auto-start","installed="+BeckenburgUpdates.installedVersion());
        Thread worker=new Thread(()->{
            try{
                var release=BeckenburgUpdates.check(BeckenburgUpdates.installedVersion());
                BeckenburgDeveloperTrace.update("auto-result",release==null?"up-to-date":"available="+release.version());
                if(release!=null){
                    BeckenburgDiagnostics.record("UPDATES found "+release.version()+" from "+release.tag());
                    SwingUtilities.invokeLater(()->{
                    pending=release;parent.getRootPane().putClientProperty("beckenburg.update.available",release.version());
                    });
                }
            }catch(Exception offline){ // Offline updates must not interrupt a playable game.
                BeckenburgDiagnostics.record("UPDATES automatic check unavailable: "+offline);
            }
        },"Beckenburg-update-check");worker.setDaemon(true);worker.start();
    }
    public static void open(JFrame parent){
        UpdatePanel panel=new UpdatePanel(parent,pending);
        try{GameDialogs.showMessageDialog(parent,panel,"BECKENBURG UPDATES",GameDialogs.PLAIN_MESSAGE);}
        finally{panel.dispose();}
    }

    private static final class UpdatePanel extends JPanel {
        private final JLabel status=new JLabel("Checks only Beckenburg release tags on GitHub.");
        private final JTextArea notes=new JTextArea(8,48);
        private final JButton check=new JButton("Check now");
        private final JButton action=new JButton("Download installer");
        private final JProgressBar progress=new JProgressBar(0,100);
        private BeckenburgUpdates.Release offered;
        private BeckenburgUpdates.StagedInstaller staged;
        private boolean busy;
        private boolean closed;
        UpdatePanel(JFrame parent,BeckenburgUpdates.Release found){
            super(new BorderLayout());
            JPanel panel=new JPanel(new BorderLayout(8,8));
            panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(12,12,12,12));
            panel.add(new JLabel("Installed: Beckenburg "+BeckenburgUpdates.installedVersion()),BorderLayout.NORTH);
            notes.setEditable(false);notes.setLineWrap(true);notes.setWrapStyleWord(true);
            notes.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12));
            panel.add(new JScrollPane(notes),BorderLayout.CENTER);
            JPanel bottom=new JPanel(new BorderLayout(3,3));
            bottom.add(status,BorderLayout.NORTH);bottom.add(progress,BorderLayout.CENTER);
            JPanel buttons=new JPanel(new FlowLayout(FlowLayout.RIGHT,4,2));
            JCheckBox auto=new JCheckBox("Check daily",PREFS.getBoolean("check_updates",true));
            auto.addActionListener(e->PREFS.putBoolean("check_updates",auto.isSelected()));
            buttons.add(auto);
            check.addActionListener(e->check());buttons.add(check);
            action.addActionListener(e->act());buttons.add(action);
            JButton later=new JButton("Not now");later.addActionListener(e->dispose());buttons.add(later);
            bottom.add(buttons,BorderLayout.SOUTH);panel.add(bottom,BorderLayout.SOUTH);
            add(panel,BorderLayout.CENTER);PsxMenuScreen.theme(this);
            apply(found);
            if(found==null)check();

        }
        private void dispose(){closed=true;busy=true;GameDialogs.close(this);}
        private void apply(BeckenburgUpdates.Release result){
            offered=result;staged=null;
            notes.setText(result==null?"Beckenburg has its own version stream: beckenburg-vX.Y.Z.\n\n"+
                "Updates are downloaded and SHA-256 verified before any installer can run.\n"+
                "Your saved profile remains in your user directory.":
                "Version "+result.version()+"\n\n"+result.notes());
            status.setText(result==null?"No update selected.":"Beckenburg "+result.version()+" available.");
            action.setText("Download installer");action.setEnabled(result!=null);
        }
        private void state(boolean working){
            busy=working;check.setEnabled(!working);action.setEnabled(!working&&offered!=null);
        }
        private void check(){
            if(busy){BeckenburgDeveloperTrace.update("manual-skip","check already running");return;}
            BeckenburgDeveloperTrace.update("manual-start","installed="+BeckenburgUpdates.installedVersion());
            state(true);status.setText("Checking Beckenburg releases…");
            progress.setValue(0);
            Thread worker=new Thread(()->{
                try{
                    BeckenburgDiagnostics.record("UPDATES manual check requested");
                    var newest=BeckenburgUpdates.check(BeckenburgUpdates.installedVersion());
                    BeckenburgDeveloperTrace.update("manual-result",newest==null?"up-to-date":"available="+newest.version());
                    SwingUtilities.invokeLater(()->{
                        if(closed)return;
                        apply(newest);if(newest==null)status.setText("No newer complete Beckenburg release.");
                        state(false);
                    });
                }catch(Exception failure){
                    BeckenburgDiagnostics.record("UPDATES manual check failed: "+failure);
                    SwingUtilities.invokeLater(()->{
                        if(closed)return;
                        status.setText("Update check unavailable: "+failure.getMessage());state(false);
                    });
                }
            },"Beckenburg-manual-update-check");worker.setDaemon(true);worker.start();
        }
        private void act(){
            if(busy||offered==null)return;
            if(staged!=null){
                if(!System.getProperty("os.name","").toLowerCase(java.util.Locale.ROOT).contains("win")){
                    status.setText("Windows installer requires Windows.");return;
                }
                int consent=GameDialogs.showConfirmDialog(this,
                    "Close Beckenburg and hand the verified setup to a separate Windows helper? Installation will start only after the game has exited.",
                    "Install Beckenburg update",GameDialogs.YES_NO_OPTION,GameDialogs.WARNING_MESSAGE);
                if(consent!=GameDialogs.YES_OPTION)return;
                try{
                    BeckenburgDiagnostics.record("UPDATES handoff to SHA-256 verified installer "+offered.tag()
                        +" file="+staged.path().getFileName()+" (user confirmed)");
                    BeckenburgDeveloperTrace.update("installer-handoff-start","tag="+offered.tag());
                    BeckenburgInstallerHandoff.start(staged);
                    dispose(); // Independent Windows helper waits for the game to exit.
                    System.exit(0);
                }catch(Exception failure){
                    BeckenburgDiagnostics.fail("Installer process handoff",failure);
                    status.setText("Handoff failed; setup remains in "+BeckenburgUpdates.updateDirectory());
                }
                return;
            }
            BeckenburgUpdates.Release chosen=offered;
            state(true);status.setText("Downloading and verifying installer…");
            Thread worker=new Thread(()->{
                try{
                    BeckenburgDeveloperTrace.update("download-start","tag="+chosen.tag()+" size="+chosen.size());
                    BeckenburgDiagnostics.record("UPDATES downloading "+chosen.tag()+
                        " expected SHA-256 "+chosen.digest());
                    BeckenburgUpdates.StagedInstaller verified=BeckenburgUpdates.stageVerified(chosen,BeckenburgUpdates.updateDirectory(),
                        percent->SwingUtilities.invokeLater(()->progress.setValue(percent)));
                    BeckenburgDeveloperTrace.update("download-verified","tag="+chosen.tag()+" sha256-ok");
                    BeckenburgDiagnostics.record("UPDATES verified installer "+chosen.tag()+
                        " path="+verified.path().getFileName());
                    SwingUtilities.invokeLater(()->{
                        if(closed)return;
                        staged=verified;state(false);action.setText("Close game and install");
                        status.setText("Verified setup stored outside game. Installation starts after exit.");
                    });
                }catch(Exception failure){
                    BeckenburgDiagnostics.fail("Update download / SHA-256 verification",failure);
                    SwingUtilities.invokeLater(()->{
                        if(closed)return;
                        state(false);status.setText("Download failed, game unchanged: "+failure.getMessage());
                    });
                }
            },"Beckenburg-update-download");worker.setDaemon(true);worker.start();
        }
    }
}
