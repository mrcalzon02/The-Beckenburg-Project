package org.beckenburg;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import dev.mrcalzon.lighting.LightingModule;
import java.util.HashSet;
import java.util.Set;

/** Solo and co-op launch the shared host-authoritative investigation runtime. */
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
    private JComboBox<MissionWeather.Mode> weatherModePicker;
    private JComboBox<EvidenceSignals.Weather> weatherPicker;
    private JComboBox<Integer> weatherSeverityPicker;
    private LocalInvestigation localInvestigation;
    private LanWorldPanel localWorld;
    private long localLaunchGeneration;
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
    private boolean shuttingDown;

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
        // All close routes must stop the hidden non-daemon OpenGL worker too.
        window.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        window.addWindowListener(new WindowAdapter(){
            @Override public void windowClosing(WindowEvent event){shutdown();}
        });
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
                    showCastle();
                    BeckenburgDiagnostics.ready();
                    if(!warnings.isBlank())GameDialogs.showMessageDialog(window,
                        warnings+"Existing files were preserved.", "Local startup warnings",GameDialogs.WARNING_MESSAGE);
                    BeckenburgDiagnostics.phase("Checking update channel");
                    BeckenburgUpdatesUI.checkOnStartup(window);
                    BeckenburgDiagnostics.phase("Castle home available");
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
                @Override public void multiplayerInventory(){openMultiplayerInventory();}
                @Override public void spectral(){InvestigationGame.this.beginGhost();}
                @Override public void menu(){castle.openPauseMenu();}
                @Override public void options(){openOptions();}
                @Override public void extras(){showExtras();}
                @Override public void exit(){confirmQuit();}
                @Override public void exitFromGate(){shutdown();}
                @Override public void campaign(){showCampaign();}
                @Override public void cycleRole(){rolePicker.setSelectedIndex((rolePicker.getSelectedIndex()+1)%rolePicker.getItemCount());}
                @Override public void cycleWeather(){weatherPicker.setSelectedIndex((weatherPicker.getSelectedIndex()+1)%weatherPicker.getItemCount());}
                @Override public String role(){return rolePicker.getSelectedItem().toString();}
                @Override public String weather(){return MissionWeather.displayName((EvidenceSignals.Weather)weatherPicker.getSelectedItem());}
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
            @Override public void menu(){showCastle();}
            @Override public void castle(){showCastle();}
            @Override public void tutorial(){showCastleOrientation();}
        });
        // The contract board is a workstation in the retained Castle scene.
        if(castle!=null){
            showCastle();
            castle.openService("CAMPAIGN CONTRACT BOARD",campaign,this::showCastle);
        }else page("campaign",campaign);
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
    private void openMultiplayerInventory(){
        openNetwork(null);
        networkLobby.focusEquipmentInventory();
    }
    private void showNetworkInCastle(){
        if(networkLobby==null)return;
        if(castle!=null){
            if(!"castle".equals(currentScreen))showCastle();
            castle.openService("MULTIPLAYER / HOST OR JOIN",networkLobby,this::showCastle);
            SwingUtilities.invokeLater(networkLobby::requestFocusInWindow);
        }else page("network",networkLobby);
    }
    private void openNetwork(LanLobbyPanel.CampaignLaunch launch){
        final String returnScreen=currentScreen;
        if(networkLobby!=null)networkLobby.close();
        networkLobby=new LanLobbyPanel(active->{
            if(active==networkLobby)showNetworkInCastle();
            else if(active instanceof JPanel panel)page("network.field",panel);
        },()->{
            if(launch!=null)showCampaign();
            else showCastle();
        },launch);
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
        if(castle!=null)castle.closeService();
        for(Component child:cards.getComponents())if(name.equals(child.getName()))cards.remove(child);
        page.setName(name);cards.add(page,name);showCard(name);
    }
    /** Workstation menu pages remain inside Castle, with the Castle scene as their backdrop. */
    private void castlePage(String name,JPanel page){
        if(castle!=null&&"castle".equals(currentScreen)){
            castle.openMenuPanel(name,page,this::showCastle);
            SwingUtilities.invokeLater(page::requestFocusInWindow);
        }else page(name,page);
    }
    private void returnFromMenu(String screen){
        if("castle".equals(screen))showCastle();
        else showCard(screen);
    }
    private static boolean stopOnExit(String resource,Runnable cleanup){
        try{cleanup.run();return true;}
        catch(Throwable failure){
            BeckenburgDiagnostics.fail("Desktop shutdown / "+resource,failure);
            return false;
        }
    }
    private void shutdown(){
        if(shuttingDown)return;
        shuttingDown=true;
        int status=0;
        // Each component gets its own cleanup attempt; one broken network or
        // audio service must never prevent the process exit from completing.
        if(networkLobby!=null){LanLobbyPanel closing=networkLobby;networkLobby=null;
            if(!stopOnExit("multiplayer lobby",closing::close))status=1;}
        if(!stopOnExit("investigation",this::closeLocalInvestigation))status=1;
        if(ghostPractice!=null){GhostPracticePanel closing=ghostPractice;ghostPractice=null;
            if(!stopOnExit("ghost practice",closing::close))status=1;}
        if(castle!=null){CastleHubPanel closing=castle;castle=null;
            if(!stopOnExit("castle lobby",closing::close))status=1;}
        if(displayRevert!=null){displayRevert.stop();displayRevert=null;}
        if(!stopOnExit("game window",window::dispose))status=1;
        // Off the Swing EDT: libGDX must dispose its native GLFW context on
        // the GL worker before Java terminates. The GPU join is time-bounded.
        DesktopShutdown.requestExit(status);
    }
    private void confirmQuit(){
        if(GameDialogs.showConfirmDialog(window,"Leave Castle Beckenburg and exit the game?",
                "Exit Game",GameDialogs.YES_NO_OPTION,GameDialogs.QUESTION_MESSAGE)
                ==GameDialogs.YES_OPTION)shutdown();
    }
    private JButton nav(String name,String label,Runnable action){
        JButton button=button(label);button.setName(name);button.addActionListener(e->action.run());return button;
    }
    /** Headless-testable action binding for the same editor launcher used by --editor. */
    static JButton editorExtrasRoute(JButton styledButton,Runnable launchEditor){
        java.util.Objects.requireNonNull(styledButton,"styledButton");
        java.util.Objects.requireNonNull(launchEditor,"launchEditor");
        styledButton.setName("extras.editor");
        styledButton.addActionListener(e->launchEditor.run());
        return styledButton;
    }
    private void buildMenu(){
        menu.removeAll();menu.setName("menu");
        if(rolePicker==null){
            rolePicker=new JComboBox<>(InvestigationSession.Role.values());
            assistantRolePicker=new JComboBox<>(CharacterVisuals.Hunter.values());
            assistantRolePicker.setSelectedItem(CharacterVisuals.Hunter.OBSERVER);
            locationPicker=new JComboBox<>(SiteCatalog.Location.values());
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
        routes.add(nav("start.exit","EXIT",this::shutdown));
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
        list.add(editorExtrasRoute(button("DEVELOPER LEVEL EDITOR"),()->openDeveloperEditor(window)));
        JTextArea credits=new JTextArea("PSX Horror UI Free by heyheythere / CC BY 4.0\nhttps://heyheythere.itch.io/psx-horror-ui-free");
        credits.setEditable(false);list.add(credits);
        castlePage("extras",PsxMenuScreen.screen("extras","EXTRAS & SUPPORT","Game tools and credits",
            PsxMenuScreen.scroll(list),nav("extras.back","BACK",this::showCastle)));
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
        LightingModule.Settings previousLighting=LightingModule.current();
        JComboBox<LightingModule.Preset> lightingPreset=select(
            LightingModule.Preset.values(),previousLighting.preset());
        JSpinner ambientLight=new JSpinner(new SpinnerNumberModel(previousLighting.ambientPercent(),50,150,5));
        JSpinner sourceLight=new JSpinner(new SpinnerNumberModel(previousLighting.sourcePercent(),50,150,5));
        JSpinner lightReach=new JSpinner(new SpinnerNumberModel(previousLighting.reachPercent(),65,140,5));
        JSpinner diffusionEdge=new JSpinner(new SpinnerNumberModel(previousLighting.diffusionPercent(),5,100,5));
        JSpinner lightFlicker=new JSpinner(new SpinnerNumberModel(previousLighting.flickerPercent(),0,200,5));
        JSpinner lightingCache=new JSpinner(new SpinnerNumberModel(previousLighting.cacheBudgetPercent(),50,150,10));
        JComboBox<Integer> lightingCells=select(new Integer[]{2,4,8},previousLighting.cellMetres());
        JCheckBox distantLightLod=new JCheckBox("Use distance LOD outdoors",previousLighting.distantOutdoorLod());
        JCheckBox changedOnlyLights=new JCheckBox("Update only changed vertex colours",previousLighting.changedOnlyVertices());
        JCheckBox lightDependencies=new JCheckBox("Reuse unchanged source dependencies",previousLighting.dependencyReuse());
        // Presets populate real settings; touching an individual parameter
        // promotes it to Custom without applying until the user presses APPLY.
        final boolean[] loadingLightPreset={false};
        Runnable markLightingCustom=()->{
            if(!loadingLightPreset[0])lightingPreset.setSelectedItem(LightingModule.Preset.CUSTOM);
        };
        for(JSpinner control:new JSpinner[]{ambientLight,sourceLight,lightReach,diffusionEdge,lightFlicker,lightingCache})
            control.addChangeListener(e->markLightingCustom.run());
        lightingCells.addActionListener(e->markLightingCustom.run());
        for(JCheckBox control:new JCheckBox[]{distantLightLod,changedOnlyLights,lightDependencies})
            control.addActionListener(e->markLightingCustom.run());
        lightingPreset.addActionListener(e->{
            var preset=(LightingModule.Preset)lightingPreset.getSelectedItem();
            if(preset==null||preset==LightingModule.Preset.CUSTOM||loadingLightPreset[0])return;
            var chosen=LightingModule.preset(preset);
            loadingLightPreset[0]=true;
            try{
                ambientLight.setValue(chosen.ambientPercent());
                sourceLight.setValue(chosen.sourcePercent());
                lightReach.setValue(chosen.reachPercent());
                diffusionEdge.setValue(chosen.diffusionPercent());
                lightFlicker.setValue(chosen.flickerPercent());
                lightingCache.setValue(chosen.cacheBudgetPercent());
                lightingCells.setSelectedItem(chosen.cellMetres());
                distantLightLod.setSelected(chosen.distantOutdoorLod());
                changedOnlyLights.setSelected(chosen.changedOnlyVertices());
                lightDependencies.setSelected(chosen.dependencyReuse());
            }finally{loadingLightPreset[0]=false;}
        });
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
        JSlider environmentVolume=new JSlider(0,100,EnvironmentAudio.volume());
        environmentVolume.setMajorTickSpacing(25);environmentVolume.setPaintTicks(true);
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
        setting(grid,"Environment volume",environmentVolume);
        setting(grid,"Effects volume (voice separate)",effectsVolume);
        setting(grid,"LIGHTING — Rendering only",new JLabel("Game rules and ghost perception unchanged"));
        setting(grid,"Lighting preset",lightingPreset);
        setting(grid,"Ambient fill (%)",ambientLight);
        setting(grid,"World fixture strength (%)",sourceLight);
        setting(grid,"Fixture reach (%)",lightReach);
        setting(grid,"Light falloff edge (% of reach)",diffusionEdge);
        setting(grid,"Cosmetic candle / TV flicker (%)",lightFlicker);
        setting(grid,"Lighting cache allowance (%)",lightingCache);
        setting(grid,"Lighting invalidation cell (metres)",lightingCells);
        setting(grid,"Outdoor probe detail",distantLightLod);
        setting(grid,"Changed vertex uploads",changedOnlyLights);
        setting(grid,"Source-dependency cache",lightDependencies);
        JScrollPane scroll=new JScrollPane(grid,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setPreferredSize(new Dimension(610,Math.min(540,grid.getPreferredSize().height+18)));
        JLabel status=PsxMenuScreen.label("Changes are applied only when you choose APPLY.");
        JButton apply=button("APPLY OPTIONS"),back=button("BACK");
        back.addActionListener(e->returnFromMenu(returnScreen));
        JPanel content=transparentPanel(new BorderLayout(0,8));
        content.add(PsxMenuScreen.scroll(grid),BorderLayout.CENTER);content.add(status,BorderLayout.SOUTH);
        optionsScreen=PsxMenuScreen.screen("options","OPTIONS","Display / sound / accessibility",content,apply,back);
        castlePage("options",optionsScreen);
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
        LightingModule.Settings chosenLighting=new LightingModule.Settings(
            (LightingModule.Preset)lightingPreset.getSelectedItem(),
            (Integer)ambientLight.getValue(),(Integer)sourceLight.getValue(),
            (Integer)lightReach.getValue(),(Integer)diffusionEdge.getValue(),
            (Integer)lightFlicker.getValue(),
            (Integer)lightingCache.getValue(),(Integer)lightingCells.getSelectedItem(),
            distantLightLod.isSelected(),changedOnlyLights.isSelected(),lightDependencies.isSelected());
        Runnable save=()->{
        chosen.save();
        LightingModule.apply(chosenLighting);
        PsxPipeline.choose((PsxPipeline.Preset)psx.getSelectedItem());
        displayPrefs.putBoolean("largeMenuText",large.isSelected());
        displayPrefs.putBoolean("highContrast",highContrast.isSelected());
        SoundtrackAudio.volume(musicVolume.getValue());
        EnvironmentAudio.volume(environmentVolume.getValue());
        LanAudio.effectsVolume(effectsVolume.getValue());
        buildMenu();menu.revalidate();menu.repaint();PsxMenuScreen.theme(optionsScreen);status.setText("OPTIONS SAVED");
        };
        boolean windowChanged=previous.mode()!=chosen.mode()||previous.display()!=chosen.display();
        if(!windowChanged){save.run();return;}
        try{chosen.applyWindow(window);}catch(RuntimeException failure){
            previous.applyWindow(window);status.setText("Display change failed: "+failure.getMessage());return;
        }
        JButton keep=button("KEEP DISPLAY"),revert=button("REVERT");
        Runnable restore=()->{displayRevert.stop();previous.applyWindow(window);castlePage("options",optionsScreen);};
        displayRevert=new javax.swing.Timer(15000,e->restore.run());displayRevert.setRepeats(false);
        keep.addActionListener(e->{displayRevert.stop();save.run();castlePage("options",optionsScreen);});
        revert.addActionListener(e->restore.run());
        castlePage("display.confirm",PsxMenuScreen.screen("display.confirm","KEEP THIS DISPLAY?",
            "Reverts automatically after 15 seconds",PsxMenuScreen.label("Choose KEEP DISPLAY to save this mode."),keep,revert));
        displayRevert.start();
        });
    }
    private void showControlsError(String title,Exception error){
        page("notice",PsxMenuScreen.screen("notice",title,error.getMessage(),
            PsxMenuScreen.label("Your existing save was preserved."),nav("notice.back","BACK",this::showCampaign)));
    }
    private void showControls(){
        JTextArea help=new JTextArea(GameControls.help());
        help.setEditable(false);help.setLineWrap(true);help.setWrapStyleWord(true);
        help.setFont(new Font(Font.MONOSPACED,Font.PLAIN,13));
        help.setMargin(new Insets(12,12,12,12));
        String previous=currentScreen;
        castlePage("controls",PsxMenuScreen.screen("controls","FIELD CONTROLS","Keyboard and mouse",
            PsxMenuScreen.scroll(help),nav("controls.back","BACK",()->
                {if("castle".equals(previous))showExtras();else showCard(previous);})));
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
    /** Contract controls keep their native height even when the tab viewport is tall.
     * GridLayout previously stretched six combo boxes to fill the whole panel,
     * exposing the light Swing tab background behind pale labels. */
    static JPanel soloContractForm(JComponent site,JComponent role,
            JComponent schedule,JComponent weather,JComponent severity){
        JPanel form=transparentPanel(new GridBagLayout());
        form.setName("solo.contract.form");
        form.setBorder(BorderFactory.createEmptyBorder(16,18,12,18));
        contractSetting(form,0,"Investigation location",site);
        contractSetting(form,1,"Hunter discipline",role);
        contractSetting(form,2,"Weather schedule",schedule);
        contractSetting(form,3,"Starting weather",weather);
        contractSetting(form,4,"Weather severity",severity);
        GridBagConstraints space=new GridBagConstraints();
        space.gridx=0;space.gridy=5;space.gridwidth=2;
        space.weighty=1;space.fill=GridBagConstraints.VERTICAL;
        form.add(Box.createVerticalGlue(),space);
        return form;
    }
    private static void contractSetting(JPanel form,int row,String title,JComponent control){
        JLabel label=PsxMenuScreen.label(title);
        label.setForeground(new Color(240,230,208));
        label.setHorizontalAlignment(SwingConstants.LEFT);
        GridBagConstraints name=new GridBagConstraints();
        name.gridx=0;name.gridy=row;name.anchor=GridBagConstraints.WEST;
        name.insets=new Insets(0,0,10,16);
        form.add(label,name);
        Dimension natural=control.getPreferredSize();
        control.setPreferredSize(new Dimension(Math.max(245,natural.width),Math.max(34,natural.height)));
        GridBagConstraints value=new GridBagConstraints();
        value.gridx=1;value.gridy=row;value.weightx=1;
        value.fill=GridBagConstraints.HORIZONTAL;value.anchor=GridBagConstraints.NORTHWEST;
        value.insets=new Insets(0,0,10,0);
        form.add(control,value);
    }
    /** Free play is never silently dispatched to the menu's previous site.
     * Campaign dispatch continues to use its fixed story site and progression. */
    private void begin(){
        JComboBox<SiteCatalog.Location> site=new JComboBox<>(java.util.Arrays.stream(SiteCatalog.Location.values())
            .filter(SiteCatalog.Location::playable).toArray(SiteCatalog.Location[]::new));
        site.setSelectedItem(locationPicker.getSelectedItem());
        JPanel form=soloContractForm(site,rolePicker,weatherModePicker,
            weatherPicker,weatherSeverityPicker);
        JTabbedPane tabs=new JTabbedPane();PsxHorrorUi.styleTabs(tabs);
        tabs.addTab("CONTRACT",PsxMenuScreen.scroll(form));
        tabs.addTab("EQUIPMENT",new CampaignStorePanel(profile,this::refreshAccount));
        JButton deploy=nav("solo.deploy","START ROUND",()->{
            locationPicker.setSelectedItem(site.getSelectedItem());begin(false,false,0,null);
        });
        castlePage("solo",PsxMenuScreen.screen("solo","SOLO CONTRACT LOBBY",
            "Castle equipment / free-play case / campaign story advances from Campaign",tabs,
            nav("solo.back","BACK",this::showCastle),deploy));
    }
    private void begin(boolean campaignMode,boolean withAssistant,long seed,
            CampaignStory.Chapter chapter){
        closeLocalInvestigation();
        if(ghostPractice!=null){ghostPractice.close();cards.remove(ghostPractice);ghostPractice=null;}
        SiteCatalog.Location location=(SiteCatalog.Location)locationPicker.getSelectedItem();
        SiteCatalog.requirePlayable(location);
        CharacterVisuals.Hunter hunter=CharacterVisuals.Hunter.valueOf(((InvestigationSession.Role)rolePicker.getSelectedItem()).name());
        MissionWeather.Mode mode=(MissionWeather.Mode)weatherModePicker.getSelectedItem();
        EvidenceSignals.Weather weather=(EvidenceSignals.Weather)weatherPicker.getSelectedItem();
        int intensity=(Integer)weatherSeverityPicker.getSelectedItem();
        int requestedHires=campaignMode&&withAssistant?pendingHires:0;
        long generation=++localLaunchGeneration;
        page("solo.loading",PsxMenuScreen.screen("solo.loading","PREPARING INVESTIGATION",
            location.label,new JLabel("Preparing the shared game simulation..."),
            nav("solo.cancel","CANCEL",this::returnHome)));
        new SwingWorker<LocalInvestigation,Void>(){
            @Override protected LocalInvestigation doInBackground()throws Exception{
                return new LocalInvestigation(profile,location,hunter,mode,weather,intensity,
                    campaignMode?chapter:null,requestedHires);
            }
            @Override protected void done(){
                try{
                    LocalInvestigation ready=get();
                    if(generation!=localLaunchGeneration||!window.isDisplayable()){ready.close();return;}
                    localInvestigation=ready;
                    localWorld=ready.openView();
                    localWorld.putClientProperty("beckenburg.returnToLobby",(Runnable)InvestigationGame.this::returnHome);
                    page("play",localWorld);
                    SwingUtilities.invokeLater(localWorld::requestFocusInWindow);
                }catch(Exception failure){
                    if(generation==localLaunchGeneration){closeLocalInvestigation();showControlsError("Investigation launch",failure);showCastle();}
                }
            }
        }.execute();
    }
    private void closeLocalInvestigation(){
        localLaunchGeneration++;
        if(localWorld!=null){localWorld.close();cards.remove(localWorld);localWorld=null;}
        if(localInvestigation!=null){localInvestigation.close();localInvestigation=null;}
    }
    private void beginGhost(){
        if(ghostPractice!=null){ghostPractice.close();cards.remove(ghostPractice);}
        ghostPractice=new GhostPracticePanel(this::returnHome);
        cards.add(ghostPractice,"ghost");
        ((CardLayout)cards.getLayout()).show(cards,"ghost");
        SwingUtilities.invokeLater(ghostPractice::requestFocusInWindow);
    }
    private void returnHome(){
        if(localInvestigation!=null)try{localInvestigation.retrySave();}
        catch(IOException failure){showControlsError("Case payout could not be saved; retry returning to Castle",failure);return;}
        closeLocalInvestigation();
        if(ghostPractice!=null){ghostPractice.close();cards.remove(ghostPractice);ghostPractice=null;}
        refreshAccount();
        showCastle();
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
        // Opt-in developer command line only. No admin menu entry or normal
        // multiplayer spectator seat; works from the packaged Windows EXE.
        if(java.util.Arrays.asList(args).contains("--dev-ai-match")){
            try{DevMatchSession.main(args);}
            catch(Exception failure){
                BeckenburgDiagnostics.fail("Developer AI match startup",failure);
                failure.printStackTrace(System.err);
                System.exit(1);
            }
            return;
        }
        // Hidden, explicit developer asset room. Never expose it as an
        // ordinary case or multiplayer map and never alter campaign state.
        if(java.util.Arrays.asList(args).contains("--dev-room")){
            try{DevAssetShowroom.main(args);}
            catch(Exception failure){
                BeckenburgDiagnostics.fail("Developer asset review room",failure);
                failure.printStackTrace(System.err);
                System.exit(1);
            }
            return;
        }
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
