package org.beckenburg;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/** Playable solo modes plus an explicitly limited LAN room-simulation test harness. */
public final class InvestigationGame {
    private final JFrame window=new JFrame("The Beckenburg Project — Expedition Alpha");
    private final JPanel cards=new JPanel(new CardLayout());
    private final JPanel menu=PsxHorrorUi.surface(new BorderLayout());
    private final java.util.prefs.Preferences displayPrefs=java.util.prefs.Preferences.userNodeForPackage(InvestigationGame.class);
    private final ProfileStore profile=new ProfileStore(ProfileStore.defaultPath());
    private BeckenburgInstallIdentity.Identity installation;
    private final JLabel account=new JLabel();
    private JComboBox<InvestigationSession.Role> rolePicker;
    private JComboBox<CharacterVisuals.Hunter> assistantRolePicker;
    private JComboBox<SiteCatalog.Location> locationPicker;
    private JComboBox<SiteCatalog.Conditions> conditionsPicker;
    private JComboBox<MissionWeather.Mode> weatherModePicker;
    private JComboBox<EvidenceSignals.Weather> weatherPicker;
    private JComboBox<Integer> weatherSeverityPicker;
    private GamePanel game;
    private GhostPracticePanel ghostPractice;
    private CastleHubPanel castle;
    private CampaignBoardPanel campaign;
    private LanLobbyPanel networkLobby;
    private String currentScreen="menu";
    private JPanel optionsScreen;
    private javax.swing.Timer displayRevert;
    private int pendingHires;
    private int caseNumber;
    private JLabel startupStatus;

    private void bootPhase(String message){
        BeckenburgDiagnostics.phase(message);
        if(startupStatus!=null)SwingUtilities.invokeLater(()->startupStatus.setText(message));
    }
    private void startupFailure(Throwable reason){
        Throwable cause=reason instanceof java.util.concurrent.ExecutionException
            &&reason.getCause()!=null?reason.getCause():reason;
        BeckenburgDiagnostics.fail("Loading screen / "+BeckenburgDiagnostics.stageName(),cause);
        if(startupStatus!=null)startupStatus.setText("STARTUP FAILED — open Diagnostics for the crash report.");
        BeckenburgDiagnostics.failureScreen(window,"Unable to initialize Beckenburg",cause);
        // Keep the loading window and Diagnostics button available instead of
        // disposing the only route to the failure evidence.
    }

    private InvestigationGame(){
        bootPhase("Opening game window");
        window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        if(UiBranding.desktopIcon()!=null)window.setIconImage(UiBranding.desktopIcon());
        window.setSize(1060,725);
        window.setMinimumSize(new Dimension(760,510));
        window.setLocationRelativeTo(null);
        window.setContentPane(loadingScreen());
        window.setVisible(true);
        bootPhase("Applying display preferences");
        GraphicsOptions.current().applyWindow(window);
        window.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_F11,0),"toggleFullscreen");
        window.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,InputEvent.ALT_DOWN_MASK),"toggleFullscreen");
        window.getRootPane().getActionMap().put("toggleFullscreen",new AbstractAction(){
            @Override public void actionPerformed(ActionEvent event){toggleFullscreen();}
        });
        // Do local disk I/O away from the event dispatch thread. The splash reports actual
        // initialization work; it is never a simulated progress bar or forced delay.
        new SwingWorker<String,Void>(){
            @Override protected String doInBackground(){
                StringBuilder warnings=new StringBuilder();
                bootPhase("Reading installation identity");
                try{installation=BeckenburgInstallIdentity.loadOrCreate(ProfileStore.defaultPath().getParent());}
                catch(IOException ex){warnings.append("Installation identity: ").append(ex.getMessage()).append("\n");}
                bootPhase("Loading local profile");
                try{profile.load();if(!java.nio.file.Files.exists(ProfileStore.defaultPath()))profile.save();}
                catch(IOException ex){warnings.append("Local profile: ").append(ex.getMessage()).append("\n");}
                return warnings.toString();
            }
            @Override protected void done(){
                try{
                    String warnings=get();
                    bootPhase("Building investigation menu and PSX UI");
                    buildMenu();
                    cards.add(menu,"menu");
                    window.setContentPane(cards);
                    window.revalidate();window.repaint();
                    showMenu();
                    BeckenburgDiagnostics.ready();
                    if(!warnings.isBlank())GameDialogs.showMessageDialog(window,
                        warnings+"Existing files were preserved.", "Local startup warnings",GameDialogs.WARNING_MESSAGE);
                    BeckenburgDiagnostics.phase("Checking update channel");
                    BeckenburgUpdatesUI.checkOnStartup(window);
                    BeckenburgDiagnostics.phase("Menu available");
                }catch(Throwable ex){
                    startupFailure(ex);
                }
            }
        }.execute();
    }
    private JPanel loadingScreen(){
        // Deliberately plain Swing: diagnostics must work even if the PSX UI
        // archive, its decoding, or its custom paint components are broken.
        JPanel surface=new JPanel(new BorderLayout()){
            @Override protected void paintComponent(Graphics g){
                super.paintComponent(g);
                var castleCapture=UiBranding.splash();
                if(castleCapture!=null){
                    g.drawImage(castleCapture,0,0,getWidth(),getHeight(),null);
                    g.setColor(new Color(7,11,16,135));g.fillRect(0,0,getWidth(),getHeight());
                }
            }
        };
        surface.setBackground(new Color(9,13,18));
        JPanel center=new JPanel(new GridBagLayout());
        center.setOpaque(false);
        JLabel text=new JLabel("<html><div style='text-align:center'>"+
            "<span style='font-size:34px;color:#e5d4b1'>THE BECKENBURG PROJECT</span><br><br>"+
            "<span style='font-size:13px;color:#bcc2bb'>Reading local records and preparing expedition services</span>"+
            "</div></html>",SwingConstants.CENTER);
        center.add(text);
        surface.add(center,BorderLayout.CENTER);
        startupStatus=new JLabel("Preparing local records",SwingConstants.CENTER);
        startupStatus.setForeground(new Color(208,214,201));
        startupStatus.setFont(new Font(Font.MONOSPACED,Font.PLAIN,13));
        JProgressBar loading=new JProgressBar();
        loading.setIndeterminate(true);
        loading.setBorder(BorderFactory.createEmptyBorder(4,0,4,0));
        loading.setForeground(new Color(158,185,160));
        loading.setBackground(new Color(28,34,40));
        JPanel loadingRow=new JPanel(new BorderLayout());
        loadingRow.setOpaque(false);
        loadingRow.setBorder(BorderFactory.createEmptyBorder(0,70,65,70));
        loadingRow.add(startupStatus,BorderLayout.NORTH);
        loadingRow.add(loading,BorderLayout.CENTER);
        JButton diagnostics=new JButton("DIAGNOSTICS");
        diagnostics.addActionListener(e->BeckenburgDiagnostics.show(window));
        loadingRow.add(diagnostics,BorderLayout.SOUTH);
        surface.add(loadingRow,BorderLayout.SOUTH);
        return surface;
    }
    private void showCastle(){
        BeckenburgDiagnostics.phase("Entering Castle Beckenburg");
        try{
            if(castle==null){
            castle=new CastleHubPanel(profile,new CastleHubPanel.Actions(){
                @Override public void begin(){InvestigationGame.this.begin();}
                @Override public void network(){openLan();}
                @Override public void spectral(){InvestigationGame.this.beginGhost();}
                @Override public void menu(){showMenu();}
                @Override public void campaign(){showCampaign();}
                @Override public void cycleRole(){rolePicker.setSelectedIndex((rolePicker.getSelectedIndex()+1)%rolePicker.getItemCount());}
                @Override public void cycleWeather(){conditionsPicker.setSelectedIndex((conditionsPicker.getSelectedIndex()+1)%conditionsPicker.getItemCount());}
                @Override public String role(){return rolePicker.getSelectedItem().toString();}
                @Override public String weather(){return conditionsPicker.getSelectedItem().toString();}
            });
                cards.add(castle,"castle");
            }
            castle.closeService();showCard("castle");
            SwingUtilities.invokeLater(castle::requestFocusInWindow);
        }catch(RuntimeException ex){
            BeckenburgDiagnostics.fail("Castle construction / rendering",ex);
            // Castle art must not prevent access to settings, multiplayer or the main menu.
            showMenu();
            GameDialogs.showMessageDialog(window,"Castle assets could not be loaded: "+ex.getMessage(),
                "Castle unavailable",GameDialogs.WARNING_MESSAGE);
        }
    }
    private void showCampaign(){
        // Campaign is a separate mode, not a floating service menu in free-play Castle.
        campaign=null;
        campaign=new CampaignBoardPanel(profile,new CampaignBoardPanel.Actions(){
            @Override public void deploy(SiteCatalog.Location location,boolean withAssistant){
                beginCampaign(location,withAssistant);
            }
            @Override public void discipline(InvestigationSession.Role selected){rolePicker.setSelectedItem(selected);}
            @Override public InvestigationSession.Role currentDiscipline(){return (InvestigationSession.Role)rolePicker.getSelectedItem();}
            @Override public void deployTeam(SiteCatalog.Location location,int hires,boolean coop){
                pendingHires=hires;
                try{
                    var chapter=profile.dispatchStory(CampaignStory.stageFor(location));
                    openNetwork(new LanLobbyPanel.CampaignLaunch(profile,chapter,hires,coop,
                        CharacterVisuals.Hunter.valueOf(((InvestigationSession.Role)rolePicker.getSelectedItem()).name())));
                }catch(IOException failed){showControlsError("Campaign dispatch",failed);}
            }
            @Override public void menu(){showMenu();}
            @Override public void castle(){showCastle();}
            @Override public void tutorial(){showCastleOrientation();}
        });
        // Show one full-window menu surface; do not stack a menu over Castle HUD.
        page("campaign",campaign);
        SwingUtilities.invokeLater(campaign::requestFocusInWindow);
    }
    private void showCastleOrientation(){
        showCastle();
        if(castle!=null&&castle.isShowing())castle.startOrientation();
    }
    private void showMenu(){
        refreshAccount();
        showCard("menu");
    }
    private void openLan(){openNetwork(null);}
    private void showNetworkInCastle(){
        // The network lobby replaces the previous page inside the same game window.
        if(networkLobby!=null)page("network",networkLobby);
    }
    private void openNetwork(LanLobbyPanel.CampaignLaunch launch){
        if(networkLobby!=null)networkLobby.close();
        networkLobby=new LanLobbyPanel(active->{
            if(active==networkLobby)showNetworkInCastle();
            else if(active instanceof JPanel panel)page("network.field",panel);
        },()->{if(launch==null)showCastle();else showCampaign();},launch);
        networkLobby.setName("network");showNetworkInCastle();
    }
    private Color ink(){return displayPrefs.getBoolean("highContrast",false)?Color.WHITE:new Color(223,210,183);}
    private int menuFont(int base){return base;}
    private JLabel menuLabel(String text,int size){
        JLabel l=new JLabel(text);l.setForeground(ink());
        l.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,menuFont(size)));return l;
    }
    private static JPanel transparentPanel(LayoutManager layout){JPanel p=new JPanel(layout);p.setOpaque(false);return p;}
    private static JPanel transparentPanel(){JPanel p=new JPanel();p.setOpaque(false);return p;}
    private void showCard(String name){
        currentScreen=name;((CardLayout)cards.getLayout()).show(cards,name);
        cards.revalidate();cards.repaint();
    }
    private void page(String name,JPanel page){
        for(Component child:cards.getComponents())if(name.equals(child.getName()))cards.remove(child);
        page.setName(name);cards.add(page,name);showCard(name);
    }
    private JButton nav(String name,String label,Runnable action){
        JButton button=button(label);button.setName(name);button.addActionListener(e->action.run());return button;
    }
    private void buildMenu(){
        menu.removeAll();menu.setName("menu");
        if(rolePicker==null){
            rolePicker=new JComboBox<>(InvestigationSession.Role.values());
            assistantRolePicker=new JComboBox<>(CharacterVisuals.Hunter.values());
            assistantRolePicker.setSelectedItem(CharacterVisuals.Hunter.OBSERVER);
            locationPicker=new JComboBox<>(SiteCatalog.Location.values());
            conditionsPicker=new JComboBox<>(SiteCatalog.Conditions.values());
            weatherModePicker=new JComboBox<>(MissionWeather.Mode.values());
            weatherPicker=new JComboBox<>(EvidenceSignals.Weather.values());
            weatherSeverityPicker=new JComboBox<>(new Integer[]{1,2,3});
        }
        JPanel routes=transparentPanel(new GridLayout(0,1,0,10));
        routes.add(nav("start.campaign","CAMPAIGN",this::showCampaign));
        routes.add(nav("start.multiplayer","HOST / JOIN MULTIPLAYER",this::openLan));
        routes.add(nav("start.solo","START SOLO INVESTIGATION",this::begin));
        routes.add(nav("start.castle","ENTER CASTLE",this::showCastle));
        routes.add(nav("start.options","OPTIONS",this::openOptions));
        routes.add(nav("start.extras","EXTRAS & SUPPORT",this::showExtras));
        routes.add(nav("start.exit","EXIT",()->{if(networkLobby!=null)networkLobby.close();window.dispose();}));
        JPanel centered=transparentPanel(new GridBagLayout());centered.add(routes);
        account.setForeground(new Color(181,218,189));
        JPanel body=transparentPanel(new BorderLayout(12,12));
        body.add(PsxMenuScreen.scroll(centered),BorderLayout.CENTER);body.add(account,BorderLayout.SOUTH);
        menu.add(PsxMenuScreen.screen("start.menu","THE BECKENBURG PROJECT",
            "Choose a mode / prepare your team and equipment in its lobby",body),BorderLayout.CENTER);
        refreshAccount();
    }
    private void showExtras(){
        JPanel list=transparentPanel(new GridLayout(0,1,0,9));
        list.add(nav("extras.practice","GHOST PRACTICE",this::beginGhost));
        list.add(nav("extras.controls","FIELD CONTROLS",this::showControls));
        list.add(nav("extras.import","IMPORT CAMPAIGN SAVE",this::importLocalSave));
        list.add(nav("extras.diagnostics","DIAGNOSTICS",()->BeckenburgDiagnostics.show(window)));
        list.add(nav("extras.updates","UPDATES",()->BeckenburgUpdatesUI.open(window)));
        list.add(nav("extras.editor","DEVELOPER LEVEL EDITOR",()->openDeveloperEditor(window)));
        JTextArea credits=new JTextArea("PSX Horror UI Free by heyheythere / CC BY 4.0\nhttps://heyheythere.itch.io/psx-horror-ui-free");
        credits.setEditable(false);list.add(credits);
        page("extras",PsxMenuScreen.screen("extras","EXTRAS & SUPPORT","Game tools and credits",
            PsxMenuScreen.scroll(list),nav("extras.back","BACK",this::showMenu)));
    }
    /** F11 / Alt+Enter: guaranteed windowed escape even after a failed fullscreen choice. */
    private void toggleFullscreen(){
        GraphicsOptions old=GraphicsOptions.current();
        GraphicsOptions.Mode mode=old.mode()==GraphicsOptions.Mode.WINDOWED
            ?GraphicsOptions.Mode.BORDERLESS_FULLSCREEN:GraphicsOptions.Mode.WINDOWED;
        GraphicsOptions alternate=new GraphicsOptions(mode,old.display(),old.detail(),
            old.aspect(),old.upscale(),old.antialias(),old.fieldOfView(),
            old.brightness(),old.contrast(),old.scanlines(),old.vignette(),
            old.grain(),old.chromatic());
        try{alternate.applyWindow(window);alternate.save();}
        catch(RuntimeException failure){
            try{old.applyWindow(window);}catch(RuntimeException ignored){}
            GameDialogs.showMessageDialog(window,failure.getMessage(),
                "Display change failed",GameDialogs.ERROR_MESSAGE);
        }
    }
    private static <T> JComboBox<T> select(T[] items,T selected){
        JComboBox<T> combo=new JComboBox<>(items);combo.setSelectedItem(selected);return combo;
    }
    private static void setting(JPanel grid,String text,JComponent control){
        JLabel label=new JLabel(text);label.setHorizontalAlignment(SwingConstants.RIGHT);
        grid.add(label);grid.add(control);
    }
    private void openOptions(){
        final String returnScreen=currentScreen;
        GraphicsOptions old=GraphicsOptions.current();
        JComboBox<GraphicsOptions.Mode> mode=select(GraphicsOptions.Mode.values(),old.mode());
        JComboBox<GraphicsOptions.DisplaySize> desktop=select(GraphicsOptions.DisplaySize.values(),old.display());
        JComboBox<GraphicsOptions.Detail> detail=select(GraphicsOptions.Detail.values(),old.detail());
        JComboBox<GraphicsOptions.Aspect> aspect=select(GraphicsOptions.Aspect.values(),old.aspect());
        JComboBox<GraphicsOptions.Upscale> upscale=select(GraphicsOptions.Upscale.values(),old.upscale());
        JComboBox<GraphicsOptions.Antialias> aa=select(GraphicsOptions.Antialias.values(),old.antialias());
        JComboBox<PsxPipeline.Preset> psx=select(PsxPipeline.Preset.values(),PsxPipeline.selected());
        JSpinner fov=new JSpinner(new SpinnerNumberModel(old.fieldOfView(),50,120,1));
        JSpinner brightness=new JSpinner(new SpinnerNumberModel(old.brightness(),-40,40,1));
        JSpinner contrast=new JSpinner(new SpinnerNumberModel(old.contrast(),-40,40,1));
        JCheckBox scanlines=new JCheckBox("Scanlines",old.scanlines());
        JCheckBox vignette=new JCheckBox("Vignette",old.vignette());
        JCheckBox grain=new JCheckBox("Film grain",old.grain());
        JCheckBox chromatic=new JCheckBox("Chromatic aberration",old.chromatic());
        JCheckBox large=new JCheckBox("Larger menu text",displayPrefs.getBoolean("largeMenuText",false));
        JCheckBox highContrast=new JCheckBox("High contrast menu",displayPrefs.getBoolean("highContrast",false));
        JSlider musicVolume=new JSlider(0,100,SoundtrackAudio.volume());
        musicVolume.setMajorTickSpacing(25);musicVolume.setPaintTicks(true);
        JSlider effectsVolume=new JSlider(0,100,LanAudio.effectsVolume());
        effectsVolume.setMajorTickSpacing(25);effectsVolume.setPaintTicks(true);
        JPanel grid=new JPanel(new GridLayout(0,2,7,7));
        grid.setBorder(BorderFactory.createEmptyBorder(9,9,9,9));
        setting(grid,"Display mode",mode);
        setting(grid,"Window size (fullscreen uses native)",desktop);
        setting(grid,"Real 3D rendering height",detail);
        setting(grid,"Aspect ratio",aspect);
        setting(grid,"Upscaling filter",upscale);
        setting(grid,"Anti-aliasing",aa);
        setting(grid,"PSX shader treatment",psx);
        setting(grid,"Field of view (degrees)",fov);
        setting(grid,"Brightness",brightness);
        setting(grid,"Contrast",contrast);
        setting(grid,"Atmospheric effects",new JLabel("Optional CPU post-processing"));
        grid.add(scanlines);grid.add(vignette);
        grid.add(grain);grid.add(chromatic);
        grid.add(large);grid.add(highContrast);
        setting(grid,"Music volume",musicVolume);
        setting(grid,"Effects volume (voice separate)",effectsVolume);
        JScrollPane scroll=new JScrollPane(grid,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setPreferredSize(new Dimension(610,Math.min(540,grid.getPreferredSize().height+18)));
        JLabel status=PsxMenuScreen.label("Changes are applied only when you choose APPLY.");
        JButton apply=button("APPLY OPTIONS"),back=button("BACK");
        back.addActionListener(e->showCard(returnScreen));
        JPanel content=transparentPanel(new BorderLayout(0,8));
        content.add(PsxMenuScreen.scroll(grid),BorderLayout.CENTER);content.add(status,BorderLayout.SOUTH);
        optionsScreen=PsxMenuScreen.screen("options","OPTIONS","Display / sound / accessibility",content,apply,back);
        page("options",optionsScreen);
        apply.addActionListener(event->{
        GraphicsOptions previous=GraphicsOptions.current();
        GraphicsOptions chosen=new GraphicsOptions(
            (GraphicsOptions.Mode)mode.getSelectedItem(),
            (GraphicsOptions.DisplaySize)desktop.getSelectedItem(),
            (GraphicsOptions.Detail)detail.getSelectedItem(),
            (GraphicsOptions.Aspect)aspect.getSelectedItem(),
            (GraphicsOptions.Upscale)upscale.getSelectedItem(),
            (GraphicsOptions.Antialias)aa.getSelectedItem(),
            (Integer)fov.getValue(),(Integer)brightness.getValue(),(Integer)contrast.getValue(),
            scanlines.isSelected(),vignette.isSelected(),grain.isSelected(),chromatic.isSelected());
        Runnable save=()->{
        chosen.save();
        PsxPipeline.choose((PsxPipeline.Preset)psx.getSelectedItem());
        displayPrefs.putBoolean("largeMenuText",large.isSelected());
        displayPrefs.putBoolean("highContrast",highContrast.isSelected());
        SoundtrackAudio.volume(musicVolume.getValue());
        LanAudio.effectsVolume(effectsVolume.getValue());
        buildMenu();menu.revalidate();menu.repaint();PsxMenuScreen.theme(optionsScreen);status.setText("OPTIONS SAVED");
        };
        boolean windowChanged=previous.mode()!=chosen.mode()||previous.display()!=chosen.display();
        if(!windowChanged){save.run();return;}
        try{chosen.applyWindow(window);}catch(RuntimeException failure){
            previous.applyWindow(window);status.setText("Display change failed: "+failure.getMessage());return;
        }
        JButton keep=button("KEEP DISPLAY"),revert=button("REVERT");
        Runnable restore=()->{displayRevert.stop();previous.applyWindow(window);showCard("options");};
        displayRevert=new javax.swing.Timer(15000,e->restore.run());displayRevert.setRepeats(false);
        keep.addActionListener(e->{displayRevert.stop();save.run();showCard("options");});
        revert.addActionListener(e->restore.run());
        page("display.confirm",PsxMenuScreen.screen("display.confirm","KEEP THIS DISPLAY?",
            "Reverts automatically after 15 seconds",PsxMenuScreen.label("Choose KEEP DISPLAY to save this mode."),keep,revert));
        displayRevert.start();
        });
    }
    private void showControlsError(String title,Exception error){
        page("notice",PsxMenuScreen.screen("notice",title,error.getMessage(),
            PsxMenuScreen.label("Your existing save was preserved."),nav("notice.back","BACK",this::showCampaign)));
    }
    private void showControls(){
        JTextArea help=new JTextArea(
            "CASTLE / SOLO\nWASD move  |  Shift sprint  |  Ctrl crouch  |  Click mouse-look  |  F light  |  E interact\n"+
            "1–4 select gear  |  R use  |  C ward  |  B ritual  |  J journal  |  M map\n"+
            "Q (hold) callouts  |  L hide in cupboard / emerge  |  X extract  |  Escape pause or return to menu\n\n"+
            "LOOSE OBJECTS (solo and LAN)\nF5 pick up/drop within 2m | ] turn in hand | F12 throw along your view\nDescriptions appear while examining; poltergeist projectiles injure on contact. E toggles aimed phones, alarms, radios and taps.\n\n"+
            "GHOST PRACTICE\n[ / ] change room  |  Space drift  |  1–4 ghost actions\n\n"+
            "MULTIPLAYER\nHost or join by IP, ready up, then use Enter 3D World when the case starts.\n"+
            "The Field Controls tab contains alpha simulation tools, not ordinary lobby controls.");
        help.setEditable(false);help.setLineWrap(true);help.setWrapStyleWord(true);
        help.setFont(new Font(Font.MONOSPACED,Font.PLAIN,13));
        help.setMargin(new Insets(12,12,12,12));
        String previous=currentScreen;
        page("controls",PsxMenuScreen.screen("controls","FIELD CONTROLS","Keyboard and mouse",
            PsxMenuScreen.scroll(help),nav("controls.back","BACK",()->showCard(previous))));
    }
    private JButton button(String s){
        JButton b=PsxHorrorUi.button(s);
        b.setFont(new Font(Font.SANS_SERIF,Font.BOLD,menuFont(13)));
        b.setForeground(ink());b.setBackground(new Color(48,58,66));
        b.setFocusPainted(true);
        b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(104,100,79)),
            BorderFactory.createEmptyBorder(8,12,8,12)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setMaximumSize(new Dimension(390,44));
        return b;
    }
    private void refreshAccount(){account.setText("CASTLE LEDGER  £"+profile.currency()+"   XP "+profile.experience()
            +(installation==null?"":"   PLAYER "+installation.shortPlayerId()));}
    private void importLocalSave(){
        JFileChooser chooser=new JFileChooser();
        chooser.setDialogTitle("Import a Beckenburg local progression save");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Beckenburg profile (*.properties)","properties"));
        if(chooser.showOpenDialog(window)!=JFileChooser.APPROVE_OPTION)return;
        int answer=GameDialogs.showConfirmDialog(window,
            "Import this save? Your current progress will be backed up automatically. Your installation ID will not change.",
            "Import local save",GameDialogs.OK_CANCEL_OPTION,GameDialogs.WARNING_MESSAGE);
        if(answer!=GameDialogs.OK_OPTION)return;
        try{
            profile.importFrom(chooser.getSelectedFile().toPath());
            refreshAccount();if(castle!=null)castle.repaint();
            GameDialogs.showMessageDialog(window,"Local progress imported. The previous save was backed up; installation identity is unchanged.",
                "Save imported",GameDialogs.INFORMATION_MESSAGE);
        }catch(IOException ex){GameDialogs.showMessageDialog(window,
            "Import refused; your existing progress was not intentionally overwritten: "+ex.getMessage(),
            "Import failed",GameDialogs.ERROR_MESSAGE);}
    }
    private void beginCampaign(SiteCatalog.Location location,boolean withAssistant){
        SiteCatalog.requirePlayable(location);
        if(pendingHires>profile.missionHires())throw new IllegalStateException("Hunters not reserved");
        try{
            int stage=CampaignStory.stageFor(location);
            CampaignStory.Chapter chapter=profile.dispatchStory(stage);
            long seed=CampaignStory.missionSeed(profile.storySeed(),stage);
            locationPicker.setSelectedItem(location);
            begin(true,withAssistant,seed,chapter);
        }catch(IOException failed){
            GameDialogs.showMessageDialog(window,"Campaign dispatch could not be saved: "+failed.getMessage(),
                "Contract unavailable",GameDialogs.ERROR_MESSAGE);
        }
    }
    /** Free play is never silently dispatched to the menu's previous site.
     * Campaign dispatch continues to use its fixed story site and progression. */
    private void begin(){
        JPanel form=transparentPanel(new GridLayout(0,2,8,12));
        JComboBox<SiteCatalog.Location> site=new JComboBox<>(java.util.Arrays.stream(SiteCatalog.Location.values())
            .filter(SiteCatalog.Location::playable).toArray(SiteCatalog.Location[]::new));
        site.setSelectedItem(locationPicker.getSelectedItem());
        setting(form,"Investigation location",site);setting(form,"Hunter discipline",rolePicker);
        setting(form,"Conditions",conditionsPicker);setting(form,"Weather schedule",weatherModePicker);
        setting(form,"Starting weather",weatherPicker);setting(form,"Weather severity",weatherSeverityPicker);
        JTabbedPane tabs=new JTabbedPane();PsxHorrorUi.styleTabs(tabs);
        tabs.addTab("CONTRACT",PsxMenuScreen.scroll(form));
        tabs.addTab("EQUIPMENT",new CampaignStorePanel(profile,this::refreshAccount));
        JButton deploy=nav("solo.deploy","START ROUND",()->{
            locationPicker.setSelectedItem(site.getSelectedItem());begin(false,false,0,null);
        });
        page("solo",PsxMenuScreen.screen("solo","SOLO CONTRACT LOBBY",
            "Castle equipment / free-play case / campaign story advances from Campaign",tabs,
            nav("solo.back","BACK",this::showMenu),deploy));
    }
    private void begin(boolean campaignMode,boolean withAssistant,long seed,
            CampaignStory.Chapter chapter){
        BeckenburgDiagnostics.record("CASE REQUEST campaign="+campaignMode+" assistant="+withAssistant+" seed="+seed);
        // Close the old case before starting the next trace: close() disarms
        // its watchdog, and must never disarm the newly requested case.
        if(game!=null){game.close();cards.remove(game);game=null;}
        if(ghostPractice!=null){ghostPractice.close();cards.remove(ghostPractice);ghostPractice=null;}
        BeckenburgDeveloperTrace.caseStart("campaign="+campaignMode+" assistant="+withAssistant+" seed="+seed);
        BeckenburgDeveloperTrace.caseStage("previous panels closed");
        caseNumber++;
        SiteCatalog.Location location=(SiteCatalog.Location)locationPicker.getSelectedItem();
        SiteCatalog.requirePlayable(location);
        BeckenburgDeveloperTrace.caseStage("constructing GamePanel "+location);
        game=new GamePanel((InvestigationSession.Role)rolePicker.getSelectedItem(),caseNumber,
            location,(SiteCatalog.Conditions)conditionsPicker.getSelectedItem(),
            (MissionWeather.Mode)weatherModePicker.getSelectedItem(),
            (EvidenceSignals.Weather)weatherPicker.getSelectedItem(),
            (Integer)weatherSeverityPicker.getSelectedItem(),campaignMode,withAssistant,seed,
            chapter,(CharacterVisuals.Hunter)assistantRolePicker.getSelectedItem());
        if(campaignMode){try{profile.consumeMissionHunters(withAssistant?pendingHires:0);}
            catch(IOException failed){game.close();game=null;showControlsError("Team dispatch",failed);return;}}
        BeckenburgDeveloperTrace.caseStage("GamePanel constructor completed");
        cards.add(game,"play");((CardLayout)cards.getLayout()).show(cards,"play");
        BeckenburgDeveloperTrace.caseStage("case view displayed");
        SwingUtilities.invokeLater(()->{
            game.beginRendering();
            game.requestFocusInWindow();
        });
    }
    private void beginGhost(){
        if(game!=null){game.close();cards.remove(game);game=null;}
        if(ghostPractice!=null){ghostPractice.close();cards.remove(ghostPractice);}
        ghostPractice=new GhostPracticePanel(this::returnHome);
        cards.add(ghostPractice,"ghost");
        ((CardLayout)cards.getLayout()).show(cards,"ghost");
        SwingUtilities.invokeLater(ghostPractice::requestFocusInWindow);
    }
    private void returnHome(){
        if(game!=null){game.close();cards.remove(game);game=null;}
        if(ghostPractice!=null){ghostPractice.close();cards.remove(ghostPractice);ghostPractice=null;}
        refreshAccount();
        showMenu();
    }
    private final class GamePanel extends JPanel implements KeyListener,FocusListener {
        private final Set<Integer> down=new HashSet<>();
        private final HouseMap map;
        private final SceneRenderer scene=new SceneRenderer();
        private final RoomLighting lights=new RoomLighting();
        private final InvestigationSession session;
        private final boolean campaignMode;
        private final CampaignStory.Chapter chapter;
        private final CampaignAlly assistant;
        private final java.util.List<CampaignAlly> assistants=new java.util.ArrayList<>();
        private String chapterNarration="";
        private int chapterNarrationUntil;
        private boolean narratedArrival,narratedSurvey,narratedEscalation;
        private final FieldTeamChat teamChat=new FieldTeamChat();
        private final JTextField chatInput=new JTextField();
        private LanChat.Channel chatChannel=LanChat.Channel.LOCAL;
        private int chatVisibleUntil;
        private final javax.swing.Timer timer;
        private final EquipmentLoadout loadout=new EquipmentLoadout();
        private final SiteCatalog.Location location;
        private final SiteCatalog.Conditions conditions;
        private boolean powerOn;
        private boolean mapShown,journalShown,archiveShown,claimSaved,wheelOpen,helpVisible;
        private int soloArchiveIndex;
        private final MouseLook mouseLook;
        private final TeamCallout.Board callouts;
        private double wheelDx,wheelDy;
        private TeamCallout.Ping lastPing;
        private String vanNotice="";
        private int vanNoticeExpires;
        private String storyReading="";
        private boolean briefingShown,interactionHeld;private int mapFloor;
        private final SiteCaseFiles.Reader assignmentReader=new SiteCaseFiles.Reader(),storyReader=new SiteCaseFiles.Reader();
        private int useHeldUntilTick;
        private final VanTelemetry monitor=new VanTelemetry(20261017L);
        private final AtmosphereAudio atmosphere=new AtmosphereAudio(AtmosphereAudio.Scene.FIELD);
        private final LevelSoundscape buildingSoundscape;
        private final java.util.ArrayDeque<Integer> activityHistory=new java.util.ArrayDeque<>();
        private int frameCount;
        private final DebugPerformanceHud perfHud=new DebugPerformanceHud();
        private LevelOpeningCinematic opening;
        private volatile boolean initialFrameReady;
        private SwingWorker<Void,Void> firstFrameWorker;
        void beginRendering(){
            if(firstFrameWorker!=null)return;
            final int width=Math.max(760,getWidth());
            final int height=Math.max(510,getHeight());
            BeckenburgDeveloperTrace.caseStage("building initial sized scene");
            firstFrameWorker=new SwingWorker<>(){
                @Override protected Void doInBackground(){
                    GraphicsOptions.current().configure(scene,width,height,true);
                    scene.draw(map,session,0,
                        assistants.stream().map(CampaignAlly::sprite).toList());
                    return null;
                }
                @Override protected void done(){
                    if(isCancelled())return;
                    try{
                        get();
                        initialFrameReady=true;
                        BeckenburgDeveloperTrace.caseStage("initial scene complete");
                        opening=new LevelOpeningCinematic(map,scene,getWidth(),getHeight());
                        BeckenburgDeveloperTrace.caseStage("opening aerial approach");
                        last=System.nanoTime();
                        timer.start();
                        // A running intro is not yet a responsive gameplay frame.
                        // The first successful hunter render marks the active stage.
                        repaint();
                    }catch(Exception failed){
                        Throwable cause=failed.getCause()==null?failed:failed.getCause();
                        BeckenburgDiagnostics.fail("Initial investigation renderer",cause);
                        BeckenburgDeveloperTrace.caseStop();
                        BeckenburgDiagnostics.failureScreen(InvestigationGame.this.window,
                            "Investigation scene could not render",cause);
                    }
                }
            };
            firstFrameWorker.execute();
        }
        private long last=System.nanoTime();
        private double accumulator;
        private int lastSupportedRoom=HouseMap.EXTRACTION_ROOM;
        GamePanel(InvestigationSession.Role role,int ordinal,SiteCatalog.Location site,
                SiteCatalog.Conditions mode,MissionWeather.Mode weatherMode,
                EvidenceSignals.Weather selectedWeather,int severity,
                boolean campaignMode,boolean withAssistant,long campaignSeed,
                CampaignStory.Chapter chapter,CharacterVisuals.Hunter allyClass){
            SiteCatalog.requirePlayable(site);
            if(campaignMode!=(chapter!=null))
                throw new IllegalArgumentException("Campaign assignment required only for story");
            this.campaignMode=campaignMode;
            this.chapter=chapter;
            BeckenburgDeveloperTrace.caseStage("loading map geometry");
            map=new HouseMap(SiteCatalog.mapFor(site));
            BeckenburgDeveloperTrace.caseStage("initializing soundscape");
            buildingSoundscape=new LevelSoundscape(map);
            callouts=new TeamCallout.Board(map);
            location=site;conditions=java.util.Objects.requireNonNull(mode);
            scene.x=map.van().placement().x();scene.z=map.van().placement().z();
            scene.eyeHeight=map.floorY(HouseMap.EXTRACTION_ROOM)+1.62;
            powerOn=conditions.initiallyPowered();
            syncEnvironment();
            setFocusable(true);
            setLayout(null);
            chatInput.setName("campaign.chat");
            chatInput.setFont(new Font(Font.MONOSPACED,Font.PLAIN,13));
            chatInput.setBackground(new Color(15,20,28));
            chatInput.setForeground(new Color(224,230,215));
            chatInput.setCaretColor(new Color(246,211,162));
            chatInput.setVisible(false);
            chatInput.addActionListener(e->submitTeamChat());
            chatInput.addKeyListener(new KeyAdapter(){
                @Override public void keyPressed(KeyEvent e){
                    if(e.getKeyCode()==KeyEvent.VK_ESCAPE){closeTeamChat();e.consume();}
                }
            });
            add(chatInput);
            mouseLook=new MouseLook(this,(dx,dy)->{
                if(wheelOpen){wheelDelta(dx,dy);}
                else MouseLook.rotate(scene,dx,dy);
            },()->opening==null&&!mapShown&&!journalShown&&!briefingShown
                &&!helpVisible&&!chatInput.isVisible());
            addKeyListener(this);addMouseMotionListener(mouseLook);addMouseListener(mouseLook);
            addFocusListener(mouseLook);addFocusListener(this);
            // Reproducible case seed, varies by session, no silent random site conversion.
            long roundSeed=campaignMode?campaignSeed:20261015L+ordinal;
            GhostIdentity.Type assigned=campaignMode?chapter.ghost():
                CampaignStory.rollFreePlay(roundSeed);
            BeckenburgDeveloperTrace.caseStage("initializing ghost and investigation session");
            session=new InvestigationSession(map,role,roundSeed,profile.level(role),assigned);
            session.configureWeather(weatherMode,selectedWeather,severity);
            int hires=campaignMode&&withAssistant?pendingHires:0;
            session.configureParty(1+hires);
            for(int i=0;i<hires;i++)assistants.add(new CampaignAlly(map,session,allyClass,i+1));
            assistant=assistants.isEmpty()?null:assistants.get(0);
            session.connectLighting(lights);
            atmosphere.setModule(scene.weatherModule());
            scene.setWeather(session.weather());
            atmosphere.setWeather(session.weather());
            scene.roomLights=lights;
            scene.roomLightTick=0;
            scene.rackStock=profile.equipment();
            // Real carried gear originates at the Castle quartermaster, not
            // an unconditional new set of default instruments on each mission.
            loadout.replaceAtRack(profile.preparedLoadout());
            scene.rackInventory=VanRackStock.solo(profile,loadout);
            // No implicit 320x200 frame before the sized game viewport exists.
            // House geometry/first-frame lighting run off the Swing event thread:
            // a slow scene must not freeze the entire window and prevent escape.
            timer=new javax.swing.Timer(33,e->frame());
            BeckenburgDeveloperTrace.caseStage("GamePanel ready for sized initial render");
        }
        void close(){timer.stop();if(firstFrameWorker!=null)firstFrameWorker.cancel(true);
            BeckenburgDeveloperTrace.caseStop();
            if(opening!=null){opening.close();opening=null;}
            down.clear();wheelOpen=false;mouseLook.close();atmosphere.close();buildingSoundscape.close();GameSfx.clearFixtures();}
        @Override public void doLayout(){
            super.doLayout();
            int w=Math.min(485,Math.max(150,getWidth()-24));
            chatInput.setBounds(Math.max(12,getWidth()-w-12),
                Math.max(35,getHeight()-39),w,27);
        }
        private void openTeamChat(LanChat.Channel channel){
            if(wheelOpen||mapShown||journalShown||briefingShown||helpVisible)return;
            chatChannel=channel;down.clear();mouseLook.release();
            chatInput.setText("");chatInput.setVisible(true);
            chatInput.requestFocusInWindow();repaint();
        }
        private void closeTeamChat(){
            chatInput.setVisible(false);chatInput.setText("");
            down.clear();requestFocusInWindow();repaint();
        }
        private LanWorld.Body playerSpeaker(){
            return new LanWorld.Body(scene.x,feetY(),scene.z,
                Math.floorMod((int)Math.round(Math.toDegrees(scene.yaw)),360),currentRoom());
        }
        private void postTeamMessage(LanChat.Channel channel,int seat,String message){
            teamChat.post(channel,seat,session.ticks(),message);
            chatVisibleUntil=session.ticks()+110;
        }
        private void collectAssistantChat(){
            if(assistant==null)return;
            for(int i=0;i<assistants.size();i++)for(String words:assistants.get(i).drainChat())
                postTeamMessage(LanChat.Channel.RADIO,i+1,words);
        }
        private void submitTeamChat(){
            try{
                String clean=LanChat.clean(chatInput.getText());
                postTeamMessage(chatChannel,0,clean);
                for(var assistant:assistants){
                    String reply=assistant.onChat(clean,chatChannel,playerSpeaker());
                    if(reply!=null)postTeamMessage(chatChannel,1,reply);
                }
                collectAssistantChat();
                closeTeamChat();
            }catch(IllegalArgumentException rejected){
                showVanNotice("CHAT: "+rejected.getMessage());
                chatInput.requestFocusInWindow();
            }
        }
        private double feetY(){return scene.eyeHeight-HunterPosture.eye(session.crouched());}
        private int currentRoom(){return map.roomAt(scene.x,feetY(),scene.z);}
        private boolean atVanFloor(){return Math.abs(feetY()-map.floorY(HouseMap.EXTRACTION_ROOM))<.25;}
        private EquipmentLoadout.Item aimedRack(){return atVanFloor()
            ?VanEquipmentRack.aimed(map.van(),scene.x,scene.z,scene.yaw,profile.equipment()):null;}
        private VanStaging.Station aimedStation(){return atVanFloor()?map.van().stationAt(scene.x,scene.z):null;}
        private void syncEnvironment(){
            scene.environmentLight=conditions.lightMultiplier()*(map.nativeSite()?1.0:powerOn?1.0:.48);
            scene.fogDensity=conditions.fogMultiplier();
            scene.environmentPowerOn=powerOn;
            lights.setBreaker(powerOn);map.fixtures().power(lights.supplied());
        }
        private boolean nearBreaker(){
            return RoomLighting.breakerAimed(map,scene.x,feetY(),scene.z,scene.yaw);
        }
        private void buyMedicine(){
            timer.stop();down.clear();mouseLook.release();
            String[] names=new String[6];int at=0;
            for(FieldSupplies.Kind kind:FieldSupplies.Kind.values())
                for(FieldSupplies.Tier tier:FieldSupplies.Tier.values())
                    names[at++]=kind+" / "+tier.name()+" / £"+tier.price+" / +"+tier.restore(kind);
            Object chosen=GameDialogs.showInputDialog(window,"ACCOUNT £"+profile.currency()+
                "  /  Purchase field medicine","VAN SUPPLY COUNTER",
                GameDialogs.PLAIN_MESSAGE,null,names,names[0]);
            if(chosen!=null)for(int i=0;i<names.length;i++)if(names[i].equals(chosen)){
                FieldSupplies.Kind kind=FieldSupplies.Kind.values()[i/3];
                FieldSupplies.Tier tier=FieldSupplies.Tier.values()[i%3];
                try{showVanNotice(profile.purchase(kind,tier)?
                    "Purchased "+kind+" "+tier+".":"Insufficient credits or stock capacity.");}
                catch(IOException error){showVanNotice("PURCHASE FAILED: "+error.getMessage());}
                break;
            }
            last=System.nanoTime();timer.start();requestFocusInWindow();
        }
        private void useMedicine(FieldSupplies.Kind kind){
            FieldSupplies.Tier tier=profile.strongest(kind);
            if(tier==null){showVanNotice("No purchased "+kind+" medicine. Buy at the van rack.");return;}
            if(session.outcome()!=InvestigationSession.Outcome.ACTIVE||
                    (kind==FieldSupplies.Kind.HEALTH&&session.health()==100)||
                    (kind==FieldSupplies.Kind.SANITY&&session.stress()==0)){
                showVanNotice("No treatment necessary or mission inactive.");return;
            }
            try{
                if(profile.consume(kind,tier)){
                    session.useMedicine(kind,tier.restore(kind));
                    GameSfx.play(LanSound.Cue.POTION);
                    showVanNotice(kind+" "+tier+" used. Supplies deducted from the saved inventory.");
                }
            }catch(IOException error){showVanNotice("TREATMENT FAILED: "+error.getMessage());}
        }
        /** The E key picks up a physically visible instrument; no loadout popup. */
        private void takeRackItem(){
            EquipmentLoadout.Item aimed=aimedRack();
            if(aimed==null){showVanNotice("Look directly at an instrument on the van rack, then press E.");return;}
            loadout.pickup(aimed);
            GameSfx.play(LanSound.Cue.PICKUP);
            showVanNotice("PICKED UP: "+aimed.label+".  Current slot "+(loadout.activeSlot()+1));
        }
        private boolean insideVan(){
            var p=map.van().local(scene.x,scene.z);
            return currentRoom()==HouseMap.EXTRACTION_ROOM &&
                Math.abs(p.x())<VanStaging.HALF_WIDTH-.20 &&
                p.z()>=VanStaging.FRONT && p.z()<VanStaging.REAR-.2;
        }
        private void frame(){
            long diagnosticFrameStart=System.nanoTime();
            if(opening!=null){
                if(opening.complete()){
                    opening.close();opening=null;
                    last=System.nanoTime();
                    BeckenburgDeveloperTrace.caseStage("opening handoff to hunter");
                }else{
                    opening.update(getWidth(),getHeight());
                    repaint();
                    BeckenburgDeveloperTrace.frame(System.nanoTime()-diagnosticFrameStart);
                    return; // intro never advances AI/evidence or moves player
                }
            }
            var prior=new LevelDefinition.Vec(scene.x,feetY(),scene.z);
            var supported=GroundRecovery.recover(map,prior,lastSupportedRoom,HunterPosture.height(session.crouched()));
            if(!supported.equals(prior)){scene.x=supported.x();scene.z=supported.z();scene.eyeHeight=supported.y()+HunterPosture.eye(session.crouched());}
            int safe=map.roomAt(scene.x,feetY(),scene.z);if(safe>=0&&map.traversable(scene.x,feetY(),scene.z,HunterPosture.height(session.crouched())))lastSupportedRoom=safe;
            atmosphere.setActive(isShowing());
            buildingSoundscape.setActive(isShowing());
            int outdoorRoom=currentRoom();
            buildingSoundscape.setContext(outdoorRoom,lights.breaker());buildingSoundscape.listener(scene.x,scene.eyeHeight,scene.z,scene.yaw,lights.breaker());
            atmosphere.setOutside(outdoorRoom>=0&&map.outside(outdoorRoom));
            long now=System.nanoTime();double dt=Math.max(0,Math.min(.075,(now-last)/1e9));last=now;
            double forward=wheelOpen||chatInput.isVisible()?0:
                (down.contains(KeyEvent.VK_W)?1:0)-(down.contains(KeyEvent.VK_S)?1:0);
            double strafe=wheelOpen||chatInput.isVisible()?0:
                (down.contains(KeyEvent.VK_D)?1:0)-(down.contains(KeyEvent.VK_A)?1:0);
            if(!wheelOpen && down.contains(KeyEvent.VK_LEFT))scene.yaw+=dt*1.8;
            if(!wheelOpen && down.contains(KeyEvent.VK_RIGHT))scene.yaw-=dt*1.8;
            if(!wheelOpen && down.contains(KeyEvent.VK_UP))scene.pitch=Math.min(1.05,scene.pitch+dt*.9);
            if(!wheelOpen && down.contains(KeyEvent.VK_DOWN))scene.pitch=Math.max(-1.05,scene.pitch-dt*.9);
            double length=Math.hypot(forward,strafe);
            if(length>0 && (session.outcome()==InvestigationSession.Outcome.ACTIVE || session.outcome()==InvestigationSession.Outcome.CLEANSED)){
                double priorX=scene.x,priorZ=scene.z;
                forward/=length;strafe/=length;
                double speed=session.crouched()?2.4*HunterPosture.CROUCH_SPEED:down.contains(KeyEvent.VK_SHIFT)?3.6:2.4;
                double dx=(Math.sin(scene.yaw)*forward-Math.cos(scene.yaw)*strafe)*dt*speed;
                double dz=(Math.cos(scene.yaw)*forward+Math.sin(scene.yaw)*strafe)*dt*speed;
                var moved=map.resolveMove(new LevelDefinition.Vec(scene.x,feetY(),scene.z),scene.x+dx,scene.z+dz,HunterPosture.height(session.crouched()));
                if(moved==null){
                    moved=map.resolveMove(new LevelDefinition.Vec(scene.x,feetY(),scene.z),scene.x+dx,scene.z,HunterPosture.height(session.crouched()));
                    if(moved==null)moved=map.resolveMove(new LevelDefinition.Vec(scene.x,feetY(),scene.z),scene.x,scene.z+dz,HunterPosture.height(session.crouched()));
                }
                if(moved!=null){session.leaveHiding();scene.x=moved.x();scene.z=moved.z();scene.eyeHeight=moved.y()+HunterPosture.eye(session.crouched());}
                if(Math.hypot(scene.x-priorX,scene.z-priorZ)>.0001)
                    GameSfx.play(LanSound.stepCue(map,currentRoom()));
            }
            accumulator+=dt;
            while(accumulator>=.1){
                session.clutterLook(scene.yaw,scene.pitch);
                session.tick(scene.x,feetY(),scene.z,scene.lamp);
                if(powerOn!=lights.breaker()){powerOn=lights.breaker();syncEnvironment();showVanNotice(powerOn?"Breaker restored":"Main breaker tripped");}
                if(campaignMode)advanceChapterStory();
                var listener=new LanWorld.Body(scene.x,feetY(),scene.z,
                    Math.floorMod((int)Math.round(Math.toDegrees(scene.yaw)),360),
                    currentRoom());
                for(var cue:session.drainWorldSounds()){
                    GameSfx.playAt(map,listener,cue.room(),cue.x(),cue.y(),cue.z(),
                        cue.cue(),cue.tick(),session.weather());
                    for(var assistant:assistants){
                        var heard=LanSound.project(map,assistant.listener(),cue.room(),
                            cue.x(),cue.y(),cue.z(),cue.cue(),cue.tick());
                        assistant.hear(LanSound.weatherMask(heard,session.weather(),
                            map.outside(cue.room())||map.outside(assistant.room())));
                    }
                }
                for(var beat:session.drainHauntSounds()){
                    GameSfx.playHauntingAt(map,listener,beat,session.weather());
                    for(var assistant:assistants){
                        var score=GhostHauntingChoreography.forEvent(beat.id());
                        var cue=score.beats().get(beat.beat()).cue();
                        var heard=LanSound.project(map,assistant.listener(),
                            beat.room(),beat.x(),beat.y(),beat.z(),cue,beat.tick());
                        assistant.hear(LanSound.weatherMask(heard,session.weather(),
                            map.outside(beat.room())||map.outside(assistant.room())));
                    }
                }
                for(var assistant:assistants){
                    assistant.advance(scene.x,feetY(),scene.z);
                    collectAssistantChat();
                }
                if(!map.clutter().drainNoises().isEmpty())GameSfx.play(LanSound.Cue.IMPACT);
                int activity=monitor.sample(session.ticks(),session.ghostEnergy());
                if(session.ticks()%12==0){
                    activityHistory.addLast(activity);
                    if(activityHistory.size()>32)activityHistory.removeFirst();
                }
                accumulator-=.1;
            }
            scene.roomLightTick=session.ticks();
            scene.setWeather(session.weather());
            atmosphere.setWeather(session.weather());
            GraphicsOptions.current().configure(scene,getWidth(),getHeight(),true);
            scene.rackInventory=VanRackStock.solo(profile,loadout);
            scene.draw(map,session,frameCount++/30.0,
                assistants.stream().map(CampaignAlly::sprite).toList());
            if(frameCount==1)BeckenburgDeveloperTrace.caseStage("active");
            perfHud.sample(System.nanoTime()-diagnosticFrameStart,scene,map,assistants.size(),"SOLO");
            repaint();
            if(session.outcome()==InvestigationSession.Outcome.LOST){
                timer.stop();endCase("You were overcome. The case remains open.");
            }
            BeckenburgDeveloperTrace.frame(System.nanoTime()-diagnosticFrameStart);
        }
        private void endCase(String details){
            if(claimSaved)return;
            int previousCredits=profile.currency(),previousXp=profile.disciplineExperience(session.role());
            int previousLevel=profile.level(session.role());
            String result=details+"\nLOCATION  "+location.label+
                "\nMISSION  "+session.outcome()+
                "\nEARNINGS  £"+session.payout()+
                "\nEXPERIENCE  +"+ProfileStore.soloXp(session.outcome())+
                "\nMEASURED INSTRUMENTS  "+session.evidenceCount()+
                "\nCASE DURATION  "+(session.ticks()/600)+"m "+(session.ticks()/10%60)+"s";
            try{
                boolean recorded=campaignMode?profile.settleStory(session,chapter.ordinal()):
                    profile.addPayout(session);
                if(recorded){
                    claimSaved=true;
                    result+="\n\nCASTLE FUNDS  £"+previousCredits+" -> £"+profile.currency()+
                        "\n"+session.role()+"  LEVEL "+previousLevel+" -> "+
                        profile.level(session.role())+
                        "\nCLASS XP  "+previousXp+" -> "+profile.disciplineExperience(session.role())+
                        (campaignMode?"\nCASE FILED IN CASTLE ARCHIVES"+
                            "\nCAMPAIGN CHAPTERS COMPLETE  "+profile.storyCheckpoint()+
                            " / "+CampaignStory.LENGTH:"");
                }else{
                    claimSaved=true;result+="\nAlready settled (duplicate payout refused).";
                }
            }catch(IOException | ArithmeticException ex){
                GameDialogs.showMessageDialog(window,
                    "Case could not be saved: "+ex.getMessage()+
                    "\nReturn to the extraction screen to retry. No duplicate rewards will be issued.",
                    "Campaign save failed",GameDialogs.ERROR_MESSAGE);
                return;
            }
            GameDialogs.showMessageDialog(window,result,"EXPEDITION DEBRIEF",GameDialogs.INFORMATION_MESSAGE);
            SwingUtilities.invokeLater(()->{
                InvestigationGame.this.returnHome();
                if(campaignMode)InvestigationGame.this.showCampaign();
            });
        }
        private void pause(){
            timer.stop();down.clear();mouseLook.release();wheelOpen=false;briefingShown=false;
            BeckenburgDeveloperTrace.caseStage("paused");
            Object[] choices={"Resume", "Retreat & return", "Controls", "Exit Game"};
            int choice=GameDialogs.showOptionDialog(window,"Expedition paused", "Field Pause",
                GameDialogs.DEFAULT_OPTION,GameDialogs.QUESTION_MESSAGE,null,choices,choices[0]);
            if(choice==1){session.extract(scene.x,feetY(),scene.z); // extraction location still validated
                if(session.outcome()==InvestigationSession.Outcome.ACTIVE){
                    // Leaving a running mission from elsewhere is a failed withdrawal, not a free payout.
                    GameDialogs.showMessageDialog(window,"Return to the entry marker to extract safely.","Not at extraction",GameDialogs.INFORMATION_MESSAGE);
                }else{endCase("Expedition ended.");return;}
            }
            if(choice==2) GameDialogs.showMessageDialog(window,"WASD / click to mouse-look • 1/2/3 gear • R scan • C ward • B ritual • E door • Q hold communication wheel • M map • J journal • X extract","Controls",GameDialogs.INFORMATION_MESSAGE);
            if(choice==3&&InGamePauseMenu.confirmExit(window)){
                close(); // never reward an abandoned investigation
                System.exit(0);
                return;
            }
            last=System.nanoTime();timer.start();
            BeckenburgDeveloperTrace.caseStage("active");
            requestFocusInWindow();
        }
        @Override public void keyPressed(KeyEvent e){
            if(chatInput.isVisible())return;
            int key=e.getKeyCode();
            if(DebugPerformanceHud.hotkey(e)){repaint();return;}
            if(opening!=null){
                if(key==KeyEvent.VK_SPACE||key==KeyEvent.VK_ENTER
                    ||key==KeyEvent.VK_ESCAPE){
                    opening.close();opening=null;last=System.nanoTime();
                    BeckenburgDeveloperTrace.caseStage("opening skipped to hunter");
                    down.clear();
                }
                repaint();return;
            }
            if(down.contains(key))return;
            if(key==KeyEvent.VK_ENTER){openTeamChat(LanChat.Channel.LOCAL);return;}
            if(key==KeyEvent.VK_F2){openTeamChat(LanChat.Channel.RADIO);return;}
            if(key==KeyEvent.VK_E){if(interactionHeld)return;interactionHeld=true;}
            if(key==KeyEvent.VK_F1){
                helpVisible=!helpVisible;
                if(helpVisible){down.clear();mouseLook.release();wheelOpen=false;}
                repaint();return;
            }
            if(helpVisible){if(key==KeyEvent.VK_ESCAPE){helpVisible=false;repaint();}return;}
            if(key==KeyEvent.VK_ESCAPE){if(mapShown||journalShown||briefingShown){mapShown=journalShown=briefingShown=false;down.clear();repaint();return;}if(wheelOpen){wheelOpen=false;down.remove(KeyEvent.VK_Q);repaint();}else pause();return;}
            if(briefingShown){if(key==KeyEvent.VK_E)briefingShown=false;else (storyReading.isEmpty()?assignmentReader:storyReader).key(key);down.clear();repaint();return;}
            if(mapShown){
                if(key==KeyEvent.VK_PAGE_UP)mapFloor++;else if(key==KeyEvent.VK_PAGE_DOWN)mapFloor--;else if(key==KeyEvent.VK_M||key==KeyEvent.VK_E)mapShown=false;
                down.clear();repaint();return;
            }
            if(journalShown){
                if(key==KeyEvent.VK_TAB){archiveShown=!archiveShown;down.clear();repaint();return;}
                if(archiveShown&&key==KeyEvent.VK_LEFT){
                    soloArchiveIndex=Math.floorMod(soloArchiveIndex-1,GhostIdentity.Type.values().length);
                }else if(archiveShown&&key==KeyEvent.VK_RIGHT){
                    soloArchiveIndex=(soloArchiveIndex+1)%GhostIdentity.Type.values().length;
                }else if(key==KeyEvent.VK_J||key==KeyEvent.VK_E)journalShown=false;
                down.clear();repaint();return;
            }
            if(key==KeyEvent.VK_Q){
                wheelOpen=true;wheelDx=0;wheelDy=0;
                down.add(key);repaint();return;
            }
            if(wheelOpen){ // Arrow keys also select callouts without a mouse.
                if(key==KeyEvent.VK_LEFT)wheelDx=-75;
                if(key==KeyEvent.VK_RIGHT)wheelDx=75;
                if(key==KeyEvent.VK_UP)wheelDy=-75;
                if(key==KeyEvent.VK_DOWN)wheelDy=75;
                down.add(key);repaint();return;
            }
            if(key>=KeyEvent.VK_1&&key<=KeyEvent.VK_4){
                loadout.select(key-KeyEvent.VK_1);
                GameSfx.play(LanSound.Cue.SELECT);
            }
            if(key==KeyEvent.VK_U&&assistant!=null)for(var assistant:assistants){
                if(assistant.order()==CampaignAlly.Order.FOLLOW)assistant.hold();
                else assistant.follow();
                showVanNotice("Assistant: "+assistant.report());
                postTeamMessage(LanChat.Channel.LOCAL,1,assistant.report());
            }
            if(key==KeyEvent.VK_I&&assistant!=null)for(var assistant:assistants){
                showVanNotice(assistant.readThermometer());
                collectAssistantChat();
            }
            if(key==KeyEvent.VK_P&&assistant!=null){
                orderAssistantZone();
            }
            if(key==KeyEvent.VK_F){
                scene.lamp=!scene.lamp;
                GameSfx.play(scene.lamp?LanSound.Cue.DEVICE_ON:LanSound.Cue.DEVICE_OFF);
            }
            if(key==KeyEvent.VK_CONTROL){double feet=feetY();if(session.posture(!session.crouched(),scene.x,feet,scene.z))scene.eyeHeight=feet+HunterPosture.eye(session.crouched());}
            if(key==KeyEvent.VK_L)session.toggleHide(scene.x,feetY(),scene.z);
            if(key==KeyEvent.VK_F5||key==KeyEvent.VK_F12||key==KeyEvent.VK_CLOSE_BRACKET){
                var o=new LooseClutter.Observer(0,scene.x,feetY(),scene.z,scene.yaw,scene.pitch,true,HunterPosture.eye(session.crouched()));
                int id=map.clutter().held(0);var aimed=map.clutter().aimed(o);if(id<0&&aimed!=null)id=aimed.id();
                if(id>0)try{session.clutterLook(scene.yaw,scene.pitch);session.clutterAction(key==KeyEvent.VK_F12?"THROW":key==KeyEvent.VK_CLOSE_BRACKET?"ROTATE":map.clutter().held(0)>=0?"DROP":"TAKE",id,scene.x,feetY(),scene.z);}
                catch(IllegalArgumentException ex){showVanNotice(ex.getMessage());}
            }
            if(key==KeyEvent.VK_E){if(briefingShown){briefingShown=false;repaint();return;}interactFocused();down.add(key);repaint();return;}
            if(key==KeyEvent.VK_R){
                useHeldUntilTick=session.ticks()+4;
                session.useEquipment(loadout,scene.x,feetY(),scene.z,scene.yaw);
                GameSfx.play(loadout.equipped()==EquipmentLoadout.Item.PHOTO_CAMERA
                    ?LanSound.Cue.CAMERA:LanSound.Cue.SCANNER);
            }
            if(key==KeyEvent.VK_C){
                session.placeWard(scene.x,feetY(),scene.z);
                GameSfx.play(LanSound.Cue.WARD);
            }
            if(key==KeyEvent.VK_5||key==KeyEvent.VK_6){
                HunterDiscipline.Ability ability=key==KeyEvent.VK_5
                    ?HunterDiscipline.primary(CharacterVisuals.Hunter.valueOf(session.role().name()))
                    :HunterDiscipline.secondary(CharacterVisuals.Hunter.valueOf(session.role().name()));
                var outcome=session.classAbility(ability,scene.x,feetY(),scene.z);
                showVanNotice(outcome.message());
                if(outcome.accepted())GameSfx.play(ability==HunterDiscipline.Ability.SURVEY
                    ?LanSound.Cue.SCANNER:LanSound.Cue.WARD);
            }
            if(key==KeyEvent.VK_Z)session.deployCounter(HunterCounters.Kind.CROSS,scene.x,feetY(),scene.z,scene.yaw);
            if(key==KeyEvent.VK_G)session.deployCounter(HunterCounters.Kind.WARD,scene.x,feetY(),scene.z,scene.yaw);
            if(key==KeyEvent.VK_H)session.deployCounter(HunterCounters.Kind.SALT,scene.x,feetY(),scene.z,scene.yaw);
            if(key==KeyEvent.VK_B)session.cleanse(scene.x,feetY(),scene.z);
            if(key==KeyEvent.VK_T)useMedicine(FieldSupplies.Kind.HEALTH);
            if(key==KeyEvent.VK_Y)useMedicine(FieldSupplies.Kind.SANITY);
            if(key==KeyEvent.VK_J){journalShown=!journalShown;down.clear();mouseLook.release();GameSfx.play(LanSound.Cue.BOOK);}
            if(key==KeyEvent.VK_M){mapShown=!mapShown;down.clear();mouseLook.release();GameSfx.play(LanSound.Cue.BOOK);}
            if(key==KeyEvent.VK_X && session.extract(scene.x,feetY(),scene.z)) {timer.stop();endCase("Extraction processed.");return;}
            down.add(key);repaint();
        }
        private void orderAssistantZone(){
            timer.stop();down.clear();mouseLook.release();
            String[] zones=new String[HouseMap.ROOM_COUNT];
            for(int i=0;i<zones.length;i++)zones[i]=i+"  /  "+map.roomName(i);
            Object selected=GameDialogs.showInputDialog(window,
                "Assistant destination","FIELD ORDER",GameDialogs.PLAIN_MESSAGE,
                null,zones,zones[currentRoom()<0?0:currentRoom()]);
            if(selected!=null){
                int room=Integer.parseInt(selected.toString().split("  /  ")[0]);
                for(var ally:assistants)ally.investigate(room);
                showVanNotice(assistant.report());
            }
            last=System.nanoTime();timer.start();requestFocusInWindow();
        }
        private InteractionFocus.Target focusTarget(){
            return InteractionFocus.pick(map,new LooseClutter.Observer(0,scene.x,feetY(),scene.z,scene.yaw,scene.pitch,!session.hidden()&&session.health()>0,HunterPosture.eye(session.crouched())),VanRackStock.solo(profile,loadout));
        }
        private void interactFocused(){
            if(journalShown||mapShown||briefingShown||wheelOpen)return;
            var target=focusTarget();if(target==null)return;
            try{switch(target.kind()){
                case CLUTTER -> map.clutter().takeFocused(Integer.parseInt(target.id()),new LooseClutter.Observer(0,scene.x,feetY(),scene.z,scene.yaw,scene.pitch,true,HunterPosture.eye(session.crouched())),session.ticks());
                case FIXTURE -> {int id=Integer.parseInt(target.id());map.fixtures().toggleFocused(id,new LooseClutter.Observer(0,scene.x,feetY(),scene.z,scene.yaw,scene.pitch,true,HunterPosture.eye(session.crouched())),session.ticks());GameSfx.fixtureState(map.fixtures().state(id));}
                case RACK -> {
                    session.rechargeUvAtVan(scene.x,feetY(),scene.z);
                    String id=target.id().split(":")[0];
                    if(id.equals("HEALTH_SUPPLIES")||id.equals("SANITY_MEDICATION")){
                        var kind=id.equals("HEALTH_SUPPLIES")?FieldSupplies.Kind.HEALTH:FieldSupplies.Kind.SANITY;
                        var tier=FieldSupplies.Tier.values()[EquipmentTiers.Tier.valueOf(target.id().split(":")[1]).ordinal()];
                        if((kind==FieldSupplies.Kind.HEALTH&&session.health()==100)||(kind==FieldSupplies.Kind.SANITY&&session.stress()==0))throw new IllegalArgumentException("No treatment necessary.");
                        if(profile.consume(kind,tier))session.useMedicine(kind,tier.restore(kind));
                    }else {
                        var item=EquipmentLoadout.Item.valueOf(id.equals("EMF_READER")?"EMF":id.equals("UV_LIGHT")?"UV_LAMP":id);
                        loadout.pickup(item);
                    }
                }
                case INTERFACE -> {assignmentReader.reset();storyReader.reset();storyReading=target.id().startsWith("STORY_")?target.id().substring(6):"";if(target.id().equals("MAP"))mapShown=true;else if(target.id().equals("COMPUTER"))journalShown=true;else briefingShown=true;down.clear();mouseLook.release();}
                case SWITCH -> lights.toggle(Integer.parseInt(target.id()));
                case GENERATOR -> {lights.setSourceRunning(!lights.sourceRunning());map.fixtures().power(lights.supplied());syncEnvironment();}
                case BREAKER -> {powerOn=!powerOn;map.fixtures().power(lights.supplied());syncEnvironment();}
                case DOOR -> {int id=Integer.parseInt(target.id());map.toggleDoor(id);if(!map.traversable(scene.x,feetY(),scene.z)){map.toggleDoor(id);throw new IllegalArgumentException("Doorway occupied.");}}
                case WINDOW -> map.toggleWindow(Integer.parseInt(target.id()));
                case VAN_DOOR -> {if(map.van().toggleCargoDoor()&&!map.traversable(scene.x,feetY(),scene.z)){map.van().toggleCargoDoor();throw new IllegalArgumentException("Van doorway occupied.");}}
            }
            GameSfx.play(target.kind()==InteractionFocus.Kind.CLUTTER||target.kind()==InteractionFocus.Kind.RACK?LanSound.Cue.PICKUP:LanSound.Cue.SWITCH);
            showVanNotice(target.kind()==InteractionFocus.Kind.GENERATOR?(lights.sourceRunning()?"Generator running":"Generator stopped"):target.name());
            }catch(IllegalArgumentException|IOException ex){showVanNotice(ex.getMessage());}
        }
        private void showVanNotice(String text){vanNotice=text;vanNoticeExpires=session.ticks()+85;}
        private void chapterLine(String words){
            chapterNarration="LORE MASTER  /  "+words;
            chapterNarrationUntil=session.ticks()+115;
        }
        private void advanceChapterStory(){
            if(!narratedArrival){
                narratedArrival=true;
                chapterLine(chapter.arrival());
            }else if(!narratedSurvey&&session.ticks()>115&&
                    currentRoom()>=0&&currentRoom()<HouseMap.INDOOR_ROOMS){
                narratedSurvey=true;
                chapterLine(chapter.firstSurvey());
            }else if(narratedSurvey&&!narratedEscalation&&session.ticks()>390){
                narratedEscalation=true;
                chapterLine(chapter.escalation());
            }
        }
        private void drawChapterNarration(Graphics2D g,int width,int height){
            if(!campaignMode||chapterNarrationUntil<=session.ticks())return;
            int boxW=Math.min(width-26,690);
            int x=(width-boxW)/2,y=Math.max(126,Math.min(height-135,210));
            g.setColor(new Color(12,19,27,235));
            g.fillRoundRect(x,y,boxW,62,8,8);
            g.setFont(new Font(Font.MONOSPACED,Font.BOLD,12));
            g.setColor(new Color(227,206,159));
            String words=chapterNarration;
            int split=Math.min(words.length(),Math.max(24,(boxW-24)/8));
            if(split<words.length()){
                int space=words.lastIndexOf(' ',split);
                if(space>16)split=space;
            }
            g.drawString(words.substring(0,split),x+11,y+25);
            if(split<words.length()){
                String tail=words.substring(split).strip();
                int limit=Math.max(12,(boxW-36)/8);
                if(tail.length()>limit)tail=tail.substring(0,limit-3)+"...";
                g.drawString(tail,x+11,y+44);
            }
        }
        private void useVan(VanStaging.Station station){
            switch(station){
                case BRIEFING -> {
                    storyReading="";assignmentReader.reset();briefingShown=!briefingShown;
                    showVanNotice(briefingShown?"CASE BOARD OPEN — current role, objectives and progress":"Case board closed.");
                }
                case EQUIPMENT -> takeRackItem();
                case RELAY -> showVanNotice("The monitor stays visible at the right while you are in the van.");
                case EXTRACTION -> {
                    if(session.extract(scene.x,feetY(),scene.z)){timer.stop();endCase("Van extraction confirmed.");}
                    else showVanNotice("Move to the extraction marker inside the van.");
                }
            }
        }
        @Override public void keyReleased(KeyEvent e){
            if(e.getKeyCode()==KeyEvent.VK_E)interactionHeld=false;
            down.remove(e.getKeyCode());
            if(e.getKeyCode()==KeyEvent.VK_Q && wheelOpen){
                wheelOpen=false;
                TeamCallout.Phrase choice=TeamCallout.choose(wheelDx,wheelDy);
                if(choice!=null){
                    double[] where=choice==TeamCallout.Phrase.HERE || choice==TeamCallout.Phrase.FOLLOW ||
                        choice==TeamCallout.Phrase.HELP || choice==TeamCallout.Phrase.HOLD ?
                        new double[]{scene.x,scene.z} : TeamCallout.aim(map,scene.x,feetY(),scene.z,scene.yaw,12);
                    lastPing=callouts.submit(new TeamCallout.Request(0,choice,scene.x,scene.z,where[0],where[1],session.ticks()),feetY(),feetY());
                    if(lastPing!=null){
                        postTeamMessage(LanChat.Channel.LOCAL,0,choice.spoken);
                        for(var assistant:assistants){
                            String reply=assistant.onCallout(lastPing,playerSpeaker());
                            if(reply!=null)postTeamMessage(LanChat.Channel.LOCAL,1,reply);
                        }
                    }
                }
                repaint();
            }
        }
        @Override public void keyTyped(KeyEvent e){}
        private void wheelDelta(int dx,int dy){
            wheelDx=Math.max(-125,Math.min(125,wheelDx+dx));
            wheelDy=Math.max(-125,Math.min(125,wheelDy+dy));
            repaint();
        }
        @Override public void focusGained(FocusEvent e){last=System.nanoTime();}
        @Override public void focusLost(FocusEvent e){interactionHeld=false;down.clear();mouseLook.release();wheelOpen=false;}
        @Override protected void paintComponent(Graphics graphics){super.paintComponent(graphics);
            Graphics2D g=(Graphics2D)graphics.create();
            try{
                g.setColor(Color.BLACK);g.fillRect(0,0,getWidth(),getHeight());
                int stageH=getHeight(),width=getWidth();
                if(!initialFrameReady){
                    g.setFont(new Font(Font.MONOSPACED,Font.BOLD,16));
                    g.setColor(new Color(208,214,202));
                    g.drawString("PREPARING INVESTIGATION / "+location.label,
                        25,Math.max(45,stageH/2));
                    return;
                }
                if(opening!=null){
                    opening.draw(g,width,stageH);
                    perfHud.draw(g,width,stageH);
                    return;
                }
                GraphicsOptions.current().draw(g,scene.image(),width,stageH);
                if(map.clutter().held(0)<0)HeldToolView.draw(g,width,stageH,loadout.equipped().name(),session.ticks(),
                    session.ticks()<=useHeldUntilTick,scene.lamp,session.deviceReading(loadout.equipped().name()));
                g.setFont(new Font(Font.MONOSPACED,Font.BOLD,13));
                g.setColor(new Color(224,218,197));g.drawString(map.clutter().held(0)>=0?"F5 DROP / F12 THROW / ] EXAMINE":"",12,stageH-124);
                g.setColor(new Color(14,19,25,222));
                g.fillRoundRect(12,10,Math.min(width-24,590),71,9,9);
                g.setColor(new Color(242,212,156));
                g.drawString("BECKENBURG / "+location.label.toUpperCase()+" / "+session.role(),24,30);
                g.setColor(new Color(207,220,216));
                g.drawString(map.roomName(currentRoom())+" / "+conditions.label.toUpperCase(),24,48);
                String status=session.statusLine();
                if(status.length()>76)status=status.substring(0,73)+"...";
                g.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12));
                g.drawString(status,24,66);
                g.setColor(new Color(232,225,195));g.drawString("+",width/2-4,stageH/2+4);
                String msg=session.message();
                if(!msg.isBlank()){
                    if(msg.length()>75)msg=msg.substring(0,72)+"...";
                    g.setColor(new Color(14,19,25,228));
                    g.fillRoundRect(12,88,Math.min(width-24,610),30,7,7);
                    g.setColor(new Color(182,218,205));g.drawString(msg,23,108);
                }
                if(assistant!=null){
                    g.setColor(new Color(13,20,27,224));
                    g.fillRoundRect(12,157,Math.min(width-24,685),39,7,7);
                    g.setColor(new Color(169,224,193));
                    g.drawString("ALLIES "+assistants.size()+" / "+assistant.order()+"  /  "+map.roomName(assistant.room())+
                        " / "+assistant.discipline()+"  /  U follow/hold  I thermometer  P destination",23,173);
                    g.setColor(new Color(210,218,202));
                    String observed=assistant.report();
                    g.drawString(observed.substring(0,Math.min(88,observed.length())),23,190);
                }
                drawTeamChat(g,width,stageH);
                drawChapterNarration(g,width,stageH);
                if(lastPing!=null && callouts.visible(session.ticks()).contains(lastPing)){
                    g.setColor(new Color(238,196,109));
                    g.drawString("CALLOUT: "+lastPing.phrase().spoken+" ["+map.roomName(lastPing.zone())+"]",23,139);
                }
                int gearY=stageH-112;
                g.setColor(new Color(13,20,27,222));g.fillRoundRect(12,gearY,346,100,9,9);
                g.setColor(new Color(236,206,155));
                g.drawString("EQUIPMENT / 1-4 SELECT / R USE / F1 HELP",23,gearY+20);
                for(int i=0;i<EquipmentLoadout.SLOTS;i++){
                    String item=loadout.at(i).label.toUpperCase();
                    if(item.length()>17)item=item.substring(0,14)+"...";
                    g.setColor(i==loadout.activeSlot()?new Color(166,238,194):new Color(194,207,211));
                    g.drawString((i+1)+"  "+item,23+(i%2)*167,gearY+45+(i/2)*27);
                }
                g.setColor(new Color(13,19,25,215));g.fillRoundRect(Math.max(12,width-126),12,114,30,8,8);
                g.setColor(new Color(225,219,200));g.drawString("ESC PAUSE",Math.max(23,width-116),32);
                if(!journalShown&&!mapShown&&!briefingShown&&!wheelOpen&&!helpVisible)InteractionFocus.draw(g,width,stageH,focusTarget());
                if(vanNoticeExpires>session.ticks()){
                    g.setColor(new Color(160,218,216));
                    g.drawString(vanNotice.substring(0,Math.min(vanNotice.length(),78)),24,Math.max(146,stageH-155));
                }
                // Always-on instrument sidebar while physically inside the van.
                if(insideVan())drawVanMeters(g);
                if(briefingShown)drawBriefing(g);
                if(wheelOpen) drawCalloutWheel(g,stageH);
                if(mapShown)drawMap(g);
                if(journalShown)drawJournal(g);
                if(helpVisible){
                    int pw=Math.min(width-36,570),ph=245,x=(width-pw)/2,y=(stageH-ph)/2;
                    g.setColor(new Color(12,17,27,248));g.fillRoundRect(x,y,pw,ph,12,12);
                    g.setColor(new Color(228,206,161));g.setFont(new Font(Font.MONOSPACED,Font.BOLD,14));
                    g.drawString("BECKENBURG / FIELD CONTROLS",x+18,y+29);
                    g.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12));
                    String[] hints={
                        "WASD move / SHIFT sprint / CLICK mouse-look",
                        "E interact / F flashlight / 1-4 select gear / R use",
                        "C ward / B rite / 5-6 class skills / Z cross / G ward / H salt",
                        "T health medicine / Y sanity medicine",
                        "J journal / M map / Q hold team callout wheel",
                        "X extract at van / ESC pause / F1 close help"
                    };
                    int iy=y+60;g.setColor(new Color(225,228,221));
                    for(String hint:hints){g.drawString(hint,x+18,iy);iy+=27;}
                }
                perfHud.draw(g,getWidth(),getHeight());
            }finally{g.dispose();}
        }
        private void drawTeamChat(Graphics2D g,int width,int height){
            if(teamChat.messages().isEmpty()&&!chatInput.isVisible())return;
            if(!chatInput.isVisible()&&session.ticks()>chatVisibleUntil)return;
            int viewWidth=Math.min(510,Math.max(175,width-24)),x=Math.max(12,width-viewWidth-12);
            int rows=chatInput.isVisible()?5:3;
            var messages=teamChat.messages();
            int first=Math.max(0,messages.size()-rows);
            int shown=messages.size()-first;
            int y=Math.max(185,height-65-shown*19-(chatInput.isVisible()?36:8));
            int boxHeight=24+shown*19+(chatInput.isVisible()?35:0);
            g.setColor(new Color(12,18,26,226));
            g.fillRoundRect(x,y,viewWidth,boxHeight,7,7);
            g.setFont(new Font(Font.MONOSPACED,Font.BOLD,12));
            g.setColor(new Color(235,209,157));
            g.drawString("FIELD CHAT / "+(chatInput.isVisible()?chatChannel+" / ESC CANCEL":
                "ENTER LOCAL  F2 RADIO"),x+10,y+17);
            g.setFont(new Font(Font.MONOSPACED,Font.PLAIN,11));
            for(int i=first;i<messages.size();i++){
                LanChat.Entry entry=messages.get(i);
                String line=(entry.channel()==LanChat.Channel.RADIO?"[R] ":"[L] ")+
                    (entry.seat()==0?"YOU: ":"ALLY: ")+entry.message();
                int chars=Math.max(12,(viewWidth-22)/7);
                if(line.length()>chars)line=line.substring(0,chars-3)+"...";
                g.setColor(entry.seat()==0?new Color(205,221,229):
                    new Color(161,226,187));
                g.drawString(line,x+10,y+38+(i-first)*19);
            }
        }
        private void drawMap(Graphics2D g){SiteMapView.draw(g,getWidth(),getHeight(),map,mapFloor,scene.x,feetY(),scene.z);}
        private void drawCalloutWheel(Graphics2D g,int stageH){
            int cx=getWidth()/2,cy=stageH/2;
            g.setColor(new Color(7,11,17,218));g.fillOval(cx-148,cy-148,296,296);
            g.setColor(new Color(176,158,121));g.drawOval(cx-146,cy-146,292,292);
            for(TeamCallout.Phrase phrase:TeamCallout.Phrase.values()){
                double angle=switch(phrase){
                    case THERE -> 0; case DANGER -> Math.PI/4; case HOLD -> Math.PI/2;
                    case DONT_GO -> 3*Math.PI/4; case HERE -> Math.PI;
                    case FOLLOW -> 5*Math.PI/4; case EVIDENCE -> 3*Math.PI/2;
                    case HELP -> 7*Math.PI/4;
                };
                TeamCallout.Phrase chosen=TeamCallout.choose(wheelDx,wheelDy);
                int tx=cx+(int)(Math.cos(angle)*108),ty=cy-(int)(Math.sin(angle)*108);
                g.setColor(chosen==phrase?new Color(255,206,124):new Color(195,196,211));
                g.setFont(new Font(Font.MONOSPACED,Font.BOLD,12));
                FontMetrics fm=g.getFontMetrics();g.drawString(phrase.label,tx-fm.stringWidth(phrase.label)/2,ty+5);
            }
            g.setColor(new Color(233,212,172));g.drawString("RELEASE Q",cx-34,cy+3);
            g.drawString("to call out",cx-38,cy+19);
            g.setColor(new Color(244,179,102));
            g.fillOval(cx+(int)wheelDx-5,cy+(int)wheelDy-5,10,10);
        }
        private void drawVanMeters(Graphics2D g){
            int w=Math.min(226,Math.max(154,getWidth()/4)),x=getWidth()-w-12,y=70;
            g.setColor(new Color(12,18,27,198));g.fillRoundRect(x,y,w,177,7,7);
            g.setFont(new Font(Font.MONOSPACED,Font.BOLD,12));
            g.setColor(new Color(230,209,165));g.drawString("VAN TELEMETRY",x+10,y+21);
            int sane=Math.max(0,100-session.stress());
            g.setFont(new Font(Font.MONOSPACED,Font.PLAIN,11));
            g.setColor(new Color(196,217,213));g.drawString("SANITY "+sane+"%",x+10,y+42);
            g.setColor(new Color(46,58,62));g.fillRect(x+10,y+48,w-20,9);
            g.setColor(sane<35?new Color(203,113,117):new Color(128,192,161));
            g.fillRect(x+10,y+48,(w-20)*sane/100,9);
            int activity=monitor.sample(session.ticks(),session.ghostEnergy());
            g.setColor(new Color(196,217,213));g.drawString("ACTIVITY ~"+activity+"/10",x+10,y+77);
            int priorX=-1,priorY=-1,index=0,size=activityHistory.size();
            g.setColor(new Color(126,198,178));
            for(int value:activityHistory){
                int px=x+10+(size<=1?0:index*(w-20)/(size-1));
                int py=y+151-value*5;
                if(priorX>=0)g.drawLine(priorX,priorY,px,py);
                priorX=px;priorY=py;index++;
            }
            g.setColor(new Color(174,183,194));g.drawString("UNCERTAIN / DELAYED",x+10,y+167);
        }
        private void drawBriefing(Graphics2D g){
            if(!storyReading.isEmpty()){SiteStories.draw(g,getWidth(),getHeight()-82,map,storyReading,storyReader);return;}
            assignmentReader.draw(g,getWidth(),getHeight()-82,map,java.util.List.of("FIELD STATUS: "+session.evidenceCount()+" verified readings; "+session.wards()+" wards; health "+session.health()+". Return to the van after cleansing."));
        }
        private void drawJournal(Graphics2D g){
            int boxW=Math.min(490,getWidth()-50),x=getWidth()-boxW-18,y=70,boxH=Math.min(getHeight()-165,395);
            g.setColor(new Color(18,21,29,240));g.fillRoundRect(x,y,boxW,boxH,10,10);
            g.setColor(new Color(223,201,156));
            if(archiveShown){
                g.drawString("GHOST ARCHIVE / TAB: CASE NOTES",x+15,y+27);
                GhostArchiveScreen.draw(g,x,y,boxW,boxH,soloArchiveIndex);
                return;
            }
            g.drawString("CASE JOURNAL / "+session.evidenceCount()+" VERIFIED / TAB: ARCHIVE",x+15,y+27);
            g.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12));
            for(InvestigationSession.Instrument t:InvestigationSession.Instrument.values()){
                y+=22;g.setColor(session.hasEvidence(t)?new Color(127,220,172):new Color(194,173,135));
                g.drawString((session.hasEvidence(t)?"[X] ":"[ ] ")+t.name().replace('_',' '),x+18,y+25);
            }
            y+=35;int j=0;
            for(String entry:session.journal().subList(Math.max(0,session.journal().size()-8),session.journal().size())){
                if(j++>=8)break;
                g.setColor(new Color(186,193,204));g.drawString(entry.substring(0,Math.min(59,entry.length())),x+16,y+j*21);
            }
        }
    }
    /** The packaged Windows shortcut and the in-game menus share this launcher. */
    static boolean editorRequested(String[] args){
        return args!=null&&java.util.Arrays.asList(args).contains("--editor");
    }
    private static void openDeveloperEditor(Window parent){
        try{DeveloperLevelEditor.open(parent);}
        catch(Throwable failure){
            BeckenburgDiagnostics.fail("Developer level editor startup",failure);
            BeckenburgDiagnostics.failureScreen(parent,"Level editor startup failed",failure);
        }
    }
    public static void main(String[] args){
        BeckenburgDiagnostics.install();
        if(GraphicsEnvironment.isHeadless()){
            BeckenburgDiagnostics.record("Headless runtime; GUI unavailable");
            System.err.println("A graphical Windows/Linux desktop is required for the Swing client.");
            return;
        }
        if(java.util.Arrays.asList(args).contains("--diagnostics")){
            SwingUtilities.invokeLater(()->BeckenburgDiagnostics.show(null));
            return;
        }
        if(editorRequested(args)){
            SwingUtilities.invokeLater(()->openDeveloperEditor(null));
            return;
        }
        SwingUtilities.invokeLater(()->{
            try{new InvestigationGame();}
            catch(Throwable failure){
                BeckenburgDiagnostics.fail("Game initialization before loading screen",failure);
                BeckenburgDiagnostics.failureScreen(null,"Beckenburg startup failed",failure);
            }
        });
    }
}
