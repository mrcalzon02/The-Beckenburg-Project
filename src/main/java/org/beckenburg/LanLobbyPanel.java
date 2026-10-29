package org.beckenburg;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

/** Compact LAN lobby and mode launcher; both hunter and ghost inhabit host-owned 3D views. */
public final class LanLobbyPanel extends JPanel implements AutoCloseable {
    private final JPasswordField ip=hbnet.NetworkPanel.masked("127.0.0.1",13),port=hbnet.NetworkPanel.masked("27717",5),name=hbnet.NetworkPanel.masked("Investigator",11);
    private final JPasswordField serverPin=hbnet.NetworkPanel.masked("",20);
    private final JCheckBox secure=new JCheckBox("TLS (verified fingerprint)");
    private hbnet.LocalProfile localNetwork;
    private hbnet.Hbnet.Client activeSecure;
    private final JComboBox<String> role=new JComboBox<>(new String[]{"Hunter","Ghost"});
    private final JComboBox<CharacterVisuals.Hunter> hunterAppearance=
        new JComboBox<>(CharacterVisuals.Hunter.values());
    private final JPasswordField roomPassword=new JPasswordField(9);
    private final JToggleButton lobbyLock=new JToggleButton("Lock joins");
    private final JButton hostInfo=new JButton("Host info");
    private final LobbyGhostAvatar.Preview avatar=new LobbyGhostAvatar.Preview();
    private LobbyGhostAvatar.Look localLook=LobbyGhostAvatar.random(
        java.util.concurrent.ThreadLocalRandom.current());
    private LobbyGhostAvatar.Look remoteLook;
    private boolean ghostJoined;
    private final JSpinner partySize=new JSpinner(new SpinnerNumberModel(1,1,6,1));
    private final JComboBox<HouseMap.Site> siteChoice=
        new JComboBox<>(java.util.Arrays.stream(HouseMap.Site.values())
            .filter(SiteCatalog::playable).toArray(HouseMap.Site[]::new));
    private HouseMap.Site selectedSite=HouseMap.Site.HOUSE;
    private final JButton tuneContract=new JButton("Contract options");
    private CustomContract custom=CustomContract.standard(3);
    private final JTextArea log=new JTextArea(12,52);
    private final JTextField lobbyChat=new JTextField(30);
    private final JButton sendLobbyChat=new JButton("Send");
    private final JLabel status=new JLabel("Not connected");
    private final JSpinner target=new JSpinner(new SpinnerNumberModel(4,0,HouseMap.ROOM_COUNT-1,1));
    private final JComboBox<GhostDomain.Action> action=new JComboBox<>(GhostDomain.Action.values());
    private final JComboBox<LanCase.Tool> instrument=new JComboBox<>(LanCase.Tool.values());
    private final JComboBox<GhostIdentity.Type> hypothesis=new JComboBox<>(GhostIdentity.Type.values());
    private final JButton identify=new JButton("Identify"),banish=new JButton("Banish"),absolve=new JButton("Absolve");
    private final JButton scan=new JButton("Scan"),ward=new JButton("Ward"),cleanse=new JButton("Ritual"),extract=new JButton("Extract");
    private final JLabel caseStatus=new JLabel("Case: not started"),teamStatus=new JLabel("Team: awaiting session");
    private LanHost hosted;
    private LanClient connection;
    private LanClient.ResumeTicket resumeTicket;
    private String resumeAddress;
    private int resumePort;
    private final JButton reconnect=new JButton("Reconnect");
    private int myRoom=HouseMap.EXTRACTION_ROOM;
    private boolean busy,missionRunning,worldMode;
    private boolean localReady,serverReady,pendingStart,startCommandSent,waitingReadyAck;
    private long connectionGeneration;
    private LanWorldPanel worldView;
    private LanGhostPanel ghostView;
    private JPanel fieldContents;
    private final JButton enter3D=new JButton("Enter 3D World");
    private final JButton ready=new JButton("READY"),start=new JButton("START ROUND"),move=new JButton("Move Room"),use=new JButton("Ghost Action");

    private final java.util.function.Consumer<JComponent> enterWorld;
    private final Runnable back;
    private final CampaignLaunch campaignLaunch;
    private JPanel lobbyContents;
    private NetworkEquipmentPanel equipmentPage;
    private final JTabbedPane pages=new JTabbedPane(JTabbedPane.TOP);
    private boolean campaignGuest;
    public record CampaignLaunch(ProfileStore profile,CampaignStory.Chapter chapter,int bots,boolean coop,CharacterVisuals.Hunter hunter){
        public CampaignLaunch(ProfileStore profile,CampaignStory.Chapter chapter,int bots){this(profile,chapter,bots,true);}
        public CampaignLaunch(ProfileStore profile,CampaignStory.Chapter chapter,int bots,boolean coop){this(profile,chapter,bots,coop,CharacterVisuals.Hunter.OBSERVER);}
    }
    /** Open the exact multiplayer catalog used by the lobby; purchases still require host snapshots. */
    void focusEquipmentInventory(){pages.setSelectedComponent(equipmentPage);}
    public LanLobbyPanel(){this(null,null,null);}
    public LanLobbyPanel(java.util.function.Consumer<JComponent> enterWorld,Runnable back,
            CampaignLaunch campaignLaunch){
        this.enterWorld=enterWorld==null?this::showLocalPage:enterWorld;this.back=back;this.campaignLaunch=campaignLaunch;
        setName(campaignLaunch==null?"multiplayer.lobby":"campaign.coop.lobby");

        // The standard lobby gets the same original-asset skin as the main menu.
        for(JButton button:new JButton[]{hostInfo,tuneContract,sendLobbyChat,reconnect,enter3D,ready,start,
                move,use,identify,banish,absolve,scan,ward,cleanse,extract})
            PsxHorrorUi.styleButton(button);
        for(JTextField field:new JTextField[]{ip,port,name,roomPassword,lobbyChat})
            PsxHorrorUi.styleTextField(field);
        setLayout(new BorderLayout());
        setBackground(new Color(15,19,25));
        JPanel connectionFields=new JPanel(new GridLayout(0,4,8,5));
        connectionFields.setBorder(BorderFactory.createEmptyBorder(10,12,6,12));
        String[] fieldLabels={"Server address","Port","Player name","Seat","Hunters","Room password","Location","Hunter appearance"};
        JComponent[] fields={ip,port,name,role,partySize,roomPassword,siteChoice,hunterAppearance};
        for(int i=0;i<fields.length;i++){connectionFields.add(new JLabel(fieldLabels[i]));connectionFields.add(fields[i]);}
        JButton host=PsxHorrorUi.button("Host LAN"),join=PsxHorrorUi.button("Join by IP");
        JPanel connectActions=new JPanel(new GridLayout(0,3,7,6));
        for(JButton button:new JButton[]{host,join,reconnect,tuneContract,hostInfo})connectActions.add(button);
        connectActions.add(lobbyLock);
        hunterAppearance.setToolTipText("Approved directional appearance, visible to other investigators");
        hunterAppearance.setSelectedItem(CharacterVisuals.Hunter.OBSERVER);
        role.addActionListener(e->hunterAppearance.setEnabled(role.getSelectedIndex()==0
                &&(connection==null||!missionRunning)));
        hunterAppearance.addActionListener(e->{
            if(role.getSelectedIndex()==0)
                avatar.showHunter((CharacterVisuals.Hunter)hunterAppearance.getSelectedItem());
            if(connection!=null&&"GHOST".equals(connection.seat())==false&&!missionRunning)
                action(()->connection.selectVisual(
                    (CharacterVisuals.Hunter)hunterAppearance.getSelectedItem()));
        });
        siteChoice.setRenderer(new DefaultListCellRenderer(){
            @Override public Component getListCellRendererComponent(JList<?> list,Object value,int index,boolean selected,boolean focus){
                super.getListCellRendererComponent(list,value,index,selected,focus);
                if(value instanceof HouseMap.Site site)setText(MissionSites.title(site.name()));return this;
            }
        });
        siteChoice.setToolTipText("The LAN host chooses the actual shared investigation map");
        lobbyLock.setEnabled(false);hostInfo.setEnabled(false);
        JPanel info=new JPanel(new GridLayout(3,1,0,4));
        info.setBorder(BorderFactory.createEmptyBorder(7,12,10,12));
        status.setFont(status.getFont().deriveFont(Font.BOLD));
        info.add(status);info.add(caseStatus);info.add(teamStatus);
        JPanel connectionTop=new JPanel(new BorderLayout(5,0));
        connectionTop.add(connectionFields,BorderLayout.NORTH);
        connectionTop.add(connectActions,BorderLayout.CENTER);
        connectionTop.add(info,BorderLayout.SOUTH);

        log.setDocument(new javax.swing.text.PlainDocument(){
            @Override public void insertString(int offset,String value,javax.swing.text.AttributeSet attrs)throws javax.swing.text.BadLocationException{
                super.insertString(offset,hbnet.Privacy.enabled?"Private session update (Stream Safe)\n":value,attrs);
            }
        });
        log.setEditable(false);log.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12));
        log.setLineWrap(true);log.setWrapStyleWord(true);
        log.setMargin(new Insets(12,12,12,12));
        JPanel lobbyCenter=new JPanel(new BorderLayout(12,0));
        lobbyCenter.setBorder(BorderFactory.createEmptyBorder(0,12,0,12));
        lobbyCenter.add(new JScrollPane(log),BorderLayout.CENTER);
        // Reserve full-body portrait space; scroll the lobby instead of shrinking the art.
        avatar.setPreferredSize(new Dimension(208,300));
        lobbyCenter.setPreferredSize(new Dimension(640,310));
        lobbyCenter.add(avatar,BorderLayout.EAST);
        JPanel launchActions=new JPanel(new FlowLayout(FlowLayout.LEFT,8,4));
        launchActions.add(ready);launchActions.add(start);launchActions.add(enter3D);
        enter3D.setToolTipText("Available when a connected match has started");
        JPanel chatRow=new JPanel(new BorderLayout(6,0));
        chatRow.add(lobbyChat,BorderLayout.CENTER);chatRow.add(sendLobbyChat,BorderLayout.EAST);
        JPanel lobbyBottom=new JPanel(new BorderLayout(5,5));
        lobbyBottom.setBorder(BorderFactory.createEmptyBorder(8,12,12,12));
        lobbyBottom.add(launchActions,BorderLayout.NORTH);
        lobbyBottom.add(chatRow,BorderLayout.SOUTH);
        JPanel lobbyPage=PsxHorrorUi.panel(new BorderLayout(0,7));
        lobbyPage.add(connectionTop,BorderLayout.NORTH);
        lobbyPage.add(lobbyCenter,BorderLayout.CENTER);
        lobbyPage.add(lobbyBottom,BorderLayout.SOUTH);

        // The advanced command console is not the ordinary multiplayer lobby.
        // Keep every existing command listener, but present the test tools where
        // they can be found without crowding hosting, readiness, and chat.
        JPanel hunterTools=new JPanel(new BorderLayout(0,9));
        hunterTools.setBorder(BorderFactory.createTitledBorder("Investigator commands"));
        JPanel hunterGrid=new JPanel(new GridLayout(0,2,9,9));
        hunterGrid.add(new JLabel("Instrument"));hunterGrid.add(instrument);
        for(JButton button:new JButton[]{scan,ward,cleanse,banish,absolve,extract})
            hunterGrid.add(button);
        hunterGrid.add(new JLabel("Ghost hypothesis"));hunterGrid.add(hypothesis);
        hunterGrid.add(identify);
        hunterTools.add(hunterGrid,BorderLayout.NORTH);
        JPanel ghostTools=new JPanel(new BorderLayout(0,9));
        ghostTools.setBorder(BorderFactory.createTitledBorder("Spatial & spectral commands"));
        JPanel ghostGrid=new JPanel(new GridLayout(0,2,9,9));
        ghostGrid.add(new JLabel("Target room"));ghostGrid.add(target);
        ghostGrid.add(move);ghostGrid.add(new JLabel("Move hunter (test)"));
        ghostGrid.add(new JLabel("Ghost ability"));ghostGrid.add(action);
        ghostGrid.add(use);ghostGrid.add(new JLabel("Ghost seat only"));
        ghostTools.add(ghostGrid,BorderLayout.NORTH);
        JPanel toolColumns=new JPanel(new GridLayout(1,2,14,0));
        toolColumns.setBorder(BorderFactory.createEmptyBorder(10,12,10,12));
        toolColumns.add(hunterTools);toolColumns.add(ghostTools);
        JPanel fieldPage=PsxHorrorUi.panel(new BorderLayout());
        JLabel toolNote=new JLabel("ALPHA FIELD CONSOLE  /  Network simulation commands; enter the 3D world from the Lobby tab.");
        toolNote.setBorder(BorderFactory.createEmptyBorder(12,12,3,12));
        fieldPage.add(toolNote,BorderLayout.NORTH);
        fieldPage.add(new JScrollPane(toolColumns),BorderLayout.CENTER);
        pages.setName("network.lobby.tabs");
        pages.addTab("Lobby & connection",PsxMenuScreen.scroll(lobbyPage));
        try{
            localNetwork=new hbnet.LocalProfile(java.nio.file.Path.of(System.getProperty("hbnet.profile",System.getProperty("user.home")+"/.beckenburg/network")));
            hbnet.NetworkPanel network=new hbnet.NetworkPanel(localNetwork,s->{ip.setText(s.host());port.setText(Integer.toString(s.port()));serverPin.setText(s.pin());secure.setSelected(true);connect(false);});
            network.onPrivacyChange(()->{log.setText("");repaint();});
            pages.addTab("Saved Servers & Friends",network);
        }catch(Exception unavailable){pages.addTab("Network profile",new JLabel("Local profile needs recovery (NET-20)"));}
        connectionFields.add(secure);connectionFields.add(serverPin);
        pages.addTab("Field controls (alpha)",fieldPage);
        PsxHorrorUi.styleTabs(pages);
        equipmentPage=new NetworkEquipmentPanel(choice->action(()->{
            if(choice.action()==EquipmentRackDialog.Action.UPGRADE)connection.buyEquipment(choice.id(),choice.next());
            else if(choice.action()==EquipmentRackDialog.Action.EQUIP)connection.equipAt(choice.slot(),choice.id());
        }), (kind,tier)->action(()->connection.purchaseSupply(kind,tier)));
        pages.addTab("EQUIPMENT & SUPPLIES",equipmentPage);
        JButton retrySave=PsxHorrorUi.button("RETRY CAMPAIGN SAVE");
        retrySave.addActionListener(e->{try{if(hosted!=null&&hosted.retryCampaignSettlement())log.append("Campaign result saved.\n");}
            catch(IOException failed){log.append("Campaign save failed: "+failed.getMessage()+"\n");}});
        if(campaignLaunch!=null)launchActions.add(retrySave);
        lobbyContents=PsxMenuScreen.screen("network.lobby.content",
            campaignLaunch==null?"MULTIPLAYER LOBBY":campaignLaunch.coop()?"CAMPAIGN / CO-OP LOBBY":"CAMPAIGN / SOLO LOBBY",
            campaignLaunch==null?"Independent contract inventory / host or join a room":
                "Guests assist your story / host owns progress and campaign funds",pages);
        if(back!=null){
            JButton leave=PsxHorrorUi.button("LEAVE LOBBY");leave.setName("network.back");
            leave.addActionListener(e->leaveLobby());
            lobbyContents.add(leave,BorderLayout.SOUTH);
        }
        add(lobbyContents,BorderLayout.CENTER);
        if(campaignLaunch!=null){
            hunterAppearance.setSelectedItem(campaignLaunch.hunter());
            role.setSelectedIndex(0);role.setEnabled(false);
            siteChoice.setSelectedItem(SiteCatalog.mapFor(campaignLaunch.chapter().map()));
            siteChoice.setEnabled(false);tuneContract.setEnabled(false);
            partySize.setValue(campaignLaunch.coop()?6:1+campaignLaunch.bots());partySize.setEnabled(false);
            if(!campaignLaunch.coop()){
                join.setVisible(false);host.setText("PREPARE SOLO TEAM");port.setText("0");ip.setText("127.0.0.1");
                roomPassword.setText(java.util.UUID.randomUUID().toString());roomPassword.setVisible(false);
            }
        }
        avatar.showHunter((CharacterVisuals.Hunter)hunterAppearance.getSelectedItem());
        role.addActionListener(e->{
            if(connection!=null)return; // Seat/role is fixed once the handshake succeeds.
            if(role.getSelectedIndex()==1){
                localLook=LobbyGhostAvatar.random(java.util.concurrent.ThreadLocalRandom.current());
                avatar.showGhost(localLook,false);
            }else avatar.showHunter((CharacterVisuals.Hunter)hunterAppearance.getSelectedItem());
        });
        host.addActionListener(e->connect(true));join.addActionListener(e->connect(false));
        hostInfo.addActionListener(e->{
            if(hosted==null)return;
            JTextArea diagnostic=new JTextArea("Private host diagnostics: "+hosted.connected()+" connected; "+hosted.reservedSeats()+" reserved. Endpoints hidden. Inspect operator configuration privately.",11,60);
            diagnostic.setEditable(false);diagnostic.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12));
            diagnostic.setCaretPosition(0);
            GameDialogs.showMessageDialog(this,new JScrollPane(diagnostic),
                "LAN host connectivity — local addresses, not public reachability",GameDialogs.INFORMATION_MESSAGE);
        });
        lobbyLock.addActionListener(e->{
            if(hosted==null){lobbyLock.setSelected(false);return;}
            hosted.setLobbyLocked(lobbyLock.isSelected());
            log.append(lobbyLock.isSelected()?"Lobby locked to new joins (existing reservations may reconnect).\n":
                "Lobby open to eligible LAN joins.\n");
        });
        reconnect.addActionListener(e->reconnect());
        reconnect.setEnabled(false);
        sendLobbyChat.addActionListener(e->sendLobbyMessage());
        lobbyChat.addActionListener(e->sendLobbyMessage());
        tuneContract.addActionListener(e->configureCustomContract());
        ready.addActionListener(e->{
            if(connection==null||missionRunning)return;
            localReady=!localReady;
            serverReady=false;
            send("READY");
            ready.setText(localReady?"UNREADY":"READY");
        });
        start.addActionListener(e->requestStartRound());
        move.addActionListener(e->action(() -> connection.move((int)target.getValue())));
        use.addActionListener(e->action(() -> connection.ghost((GhostDomain.Action)action.getSelectedItem(),(int)target.getValue())));
        scan.addActionListener(e->action(() -> connection.scan((LanCase.Tool)instrument.getSelectedItem())));
        ward.addActionListener(e->action(() -> connection.ward()));
        cleanse.addActionListener(e->action(() -> connection.cleanse()));
        banish.addActionListener(e->action(() -> connection.banish()));
        absolve.addActionListener(e->action(() -> connection.absolve()));
        identify.addActionListener(e->action(() -> connection.identify((GhostIdentity.Type)hypothesis.getSelectedItem())));
        extract.addActionListener(e->action(() -> connection.extract()));
        enter3D.addActionListener(e->open3D());
        setCommandEnabled(false);setMinimumSize(new Dimension(740,490));
        if(campaignLaunch==null)restoreLocalResumeOffer();
        // Solo campaign is one-click dispatch: host, ready, start and enter the scene.
        if(campaignLaunch!=null&&!campaignLaunch.coop())
            SwingUtilities.invokeLater(this::requestStartRound);
    }
    private void requestStartRound(){
        if(missionRunning){open3D();return;}
        if(connection==null){
            if(campaignLaunch!=null&&!campaignLaunch.coop()){
                pendingStart=true;
                status.setText("Creating local investigation...");
                if(!busy)connect(true);
            }else status.setText("Host or join a LAN room before starting.");
            return;
        }
        if(hosted==null){
            status.setText("Only the hosting player can start a round.");
            return;
        }
        if(startCommandSent)return;
        pendingStart=true;
        status.setText("Preparing investigation...");
        if(!localReady){
            localReady=true;
            serverReady=false;
            waitingReadyAck=true;
            send("READY");
            ready.setText("UNREADY");
            return;
        }
        if(!serverReady){waitingReadyAck=true;return;}
        dispatchStartRound();
    }
    /** Only prepare the campaign's real hosted party after readiness reached its host. */
    private void dispatchStartRound(){
        if(!pendingStart||startCommandSent||hosted==null||connection==null)return;
        startCommandSent=true;
        start.setEnabled(false);
        if(campaignLaunch==null){send("START");return;}
        final LanHost hostToStart=hosted;
        new SwingWorker<Void,Void>(){
            @Override protected Void doInBackground()throws Exception{
                hostToStart.prepareCampaignBots(campaignLaunch.bots());
                return null;
            }
            @Override protected void done(){
                try{
                    get();
                    if(connection==null||hosted!=hostToStart)throw new IOException("Host disconnected");
                    send("START");
                }catch(Exception ex){
                    pendingStart=false;startCommandSent=false;waitingReadyAck=false;
                    Throwable reason=ex.getCause()==null?ex:ex.getCause();
                    String detail=reason.getMessage()==null?reason.toString():reason.getMessage();
                    status.setText("Round not started: "+detail);
                    log.append("Team preparation failed: "+detail+"\n");
                    setCommandEnabled(connection!=null);
                }
            }
        }.execute();
    }
    private void showLocalPage(JComponent active){
        removeAll();add(active==this?lobbyContents:active,BorderLayout.CENTER);revalidate();repaint();
    }
    public void leaveLobby(){
        try{if(hosted!=null)hosted.retryCampaignSettlement();}
        catch(IOException failed){GameDialogs.showMessageDialog(this,"Save failed: "+failed.getMessage()+"\nRetry the campaign save before leaving.","Campaign result not saved",GameDialogs.ERROR_MESSAGE);return;}
        close();if(back!=null)back.run();
    }
    @Override public void close(){
        connectionGeneration++;busy=false;
        closeWorld();
        if(connection!=null){clearSavedResume();connection.close();connection=null;}
        if(hosted!=null){hosted.close();hosted=null;}
    }
    private void closeWorld(){
        if(worldView!=null){worldView.close();worldView=null;}
        if(ghostView!=null){ghostView.close();ghostView=null;}
        fieldContents=null;
    }
    /** Host lifetime is independent of its local player's transport socket.
     * Callable without constructing Swing windows for headless LAN regression tests.
     */
    static boolean keepRunningLocalHost(LanHost host,LanClient.ResumeTicket ticket){
        return host!=null && host.running() && ticket!=null;
    }
    private void restoreLocalResumeOffer(){
        try {
            var saved=LanResumeStore.load(LanResumeStore.defaultPath());
            if(saved.isEmpty())return;
            LanResumeStore.Saved last=saved.get();
            resumeAddress=last.address();resumePort=last.port();
            ip.setText(resumeAddress);port.setText(Integer.toString(resumePort));
            role.setSelectedIndex("GHOST".equals(last.seat())?1:0);
            resumeTicket=new LanClient.ResumeTicket(last.seat(),last.token(),last.sequence());
            reconnect.setEnabled(true);
            status.setText("Previous LAN seat may be recoverable — Reconnect");
            log.append("Saved recovery ticket found. The original host must still be running and keeping your seat.\n");
        }catch(IOException ex){
            log.append("Stored reconnect ticket could not be read; original file was preserved: "+"Operation unavailable (NET-04)"+"\n");
        }
    }
    private boolean saveResume(LanClient client){
        if(hosted!=null||activeSecure!=null)return false; // Secure runtime recovery retains the verified transport; never persist an insecure downgrade.
        LanClient.ResumeTicket ticket=client.resumeTicket();
        if(ticket==null)return false;
        try {
            LanResumeStore.save(LanResumeStore.defaultPath(),client.address(),client.port(),
                    ticket.seat(),ticket.token(),ticket.nextSequence());
            return true;
        }catch(IOException ex){
            log.append("Session recovery file unavailable: "+"Operation unavailable (NET-04)"+"\n");
            return false;
        }
    }
    private void clearSavedResume(){
        try { LanResumeStore.clear(LanResumeStore.defaultPath()); }
        catch(IOException ex){ log.append("Cannot clear stale recovery ticket: "+"Operation unavailable (NET-04)"+"\n"); }
    }
    private static JSpinner spin(int value,int low,int high,int step){
        return new JSpinner(new SpinnerNumberModel(value,low,high,step));
    }
    /** Only effective rules are editable; unsupported Phasmophobia-inspired controls are not sold as complete. */
    private void configureCustomContract(){
        JPanel form=new JPanel(new GridLayout(0,2,5,5));
        var walk=spin(custom.playerSpeed(),50,150,25);
        var run=spin(custom.sprintSpeed(),100,200,25);
        var ghost=spin(custom.ghostSpeed(),50,150,25);
        var hunterEndurance=spin(custom.hunterSprintSeconds(),1,30,1);
        var ghostEndurance=spin(custom.ghostSprintSeconds(),1,20,1);
        var recharge=spin(custom.energyRecovery(),50,200,25);
        var sanity=spin(custom.startingSanity(),0,100,25);
        var drain=spin(custom.sanityDrain(),0,200,25);
        var events=spin(custom.eventFrequency(),50,200,25);
        var hiding=spin(custom.hidingSpots(),0,100,10);
        var hardEvidence=new JComboBox<>(HardEvidenceQuota.Level.values());
        hardEvidence.setSelectedItem(custom.evidenceLevel());
        var weatherMode=new JComboBox<>(MissionWeather.Mode.values());
        weatherMode.setSelectedItem(custom.weatherMode());
        var weatherChoice=new JComboBox<>(MissionWeather.selectableWeather());
        weatherChoice.setRenderer(new DefaultListCellRenderer(){
            @Override public Component getListCellRendererComponent(JList<?> list,Object value,
                    int index,boolean selected,boolean focus){
                JLabel label=(JLabel)super.getListCellRendererComponent(
                    list,value,index,selected,focus);
                if(value instanceof EvidenceSignals.Weather weather)
                    label.setText(MissionWeather.displayName(weather));
                return label;
            }
        });
        weatherChoice.setSelectedItem(custom.fixedWeather());
        var weatherIntensity=spin(custom.weatherIntensity(),1,3,1);
        var sprint=new JComboBox<>(CustomContract.Sprint.values());
        sprint.setSelectedItem(custom.sprint());
        var sanMonitor=new JCheckBox("Sanity display",custom.sanityMonitor());
        var actMonitor=new JCheckBox("Activity display",custom.activityMonitor());
        java.util.List<Object[]> rows=java.util.List.of(
            new Object[]{"Walking speed %",walk},
            new Object[]{"Sprint multiplier %",run},
            new Object[]{"Sprinting",sprint},
            new Object[]{"Hunter sprint endurance (sec)",hunterEndurance},
            new Object[]{"Ghost burst endurance (sec)",ghostEndurance},
            new Object[]{"Ghost speed %",ghost},
            new Object[]{"Ghost energy recharge %",recharge},
            new Object[]{"Starting sanity %",sanity},
            new Object[]{"Sanity loss rate %",drain},
            new Object[]{"Ghost events frequency %",events},
            new Object[]{"Available hiding places %",hiding},
            new Object[]{"Hard evidence",hardEvidence},
            new Object[]{"Weather scheduling",weatherMode},
            new Object[]{"Starting / fixed weather",weatherChoice},
            new Object[]{"Weather severity (1-3)",weatherIntensity},
            new Object[]{"Van sanity display",sanMonitor},
            new Object[]{"Van activity display",actMonitor}
        );
        for(Object[] row:rows){form.add(new JLabel((String)row[0]));form.add((JComponent)row[1]);}
        JScrollPane scroll=new JScrollPane(form);
        scroll.setPreferredSize(new Dimension(440,325));
        int answer=GameDialogs.showConfirmDialog(this,scroll,
            "CUSTOM CONTRACT / IMPLEMENTED CONTROLS",
            GameDialogs.OK_CANCEL_OPTION,GameDialogs.PLAIN_MESSAGE);
        if(answer==GameDialogs.OK_OPTION){
            custom=new CustomContract((int)partySize.getValue(),
                (int)walk.getValue(),(int)run.getValue(),(int)ghost.getValue(),
                (int)recharge.getValue(),(int)drain.getValue(),(int)sanity.getValue(),
                (int)events.getValue(),(CustomContract.Sprint)sprint.getSelectedItem(),
                (int)hiding.getValue(),sanMonitor.isSelected(),actMonitor.isSelected(),
                (MissionWeather.Mode)weatherMode.getSelectedItem(),
                (EvidenceSignals.Weather)weatherChoice.getSelectedItem(),
                (int)weatherIntensity.getValue(),
                (int)hunterEndurance.getValue(),(int)ghostEndurance.getValue(),
                (HardEvidenceQuota.Level)hardEvidence.getSelectedItem());
            log.append("Custom settings armed for next hosted case, "+custom.hunters()+
                " hunters / difficulty +"+custom.partyDifficulty()+
                " / evidence "+custom.evidenceLevel()+
                " / weather "+custom.weatherMode()+" "+custom.fixedWeather()+
                " intensity "+custom.weatherIntensity()+".\n");
        }
    }
    private void sendLobbyMessage(){
        if(connection==null){log.append("Connect before using lobby chat.\n");return;}
        try{
            connection.lobbyChat(lobbyChat.getText());
            lobbyChat.setText("");
        }catch(IOException|IllegalArgumentException ex){
            log.append("Lobby chat: "+"Operation unavailable (NET-04)"+"\n");
        }
    }
    @FunctionalInterface private interface Command {void send()throws IOException;}
    private void action(Command command){try{if(connection==null)throw new IOException("not connected");command.send();}
        catch(Exception ex){log.append("Send failed: "+"Operation unavailable (NET-04)"+"\n");}}
    private void connect(boolean localHost){
        if(busy||connection!=null)return;
        if(hosted!=null){
            log.append("Your LAN host is still running. Reconnect its reserved seat before starting another room; closing this window ends hosting.\n");
            return;
        }
        busy=true;
        long generation=++connectionGeneration;
        status.setText("Connecting...");
        String hostname=ip.getText(),nickname=name.getText();String p=port.getText();
        boolean encrypted=secure.isSelected();String expectedPin=new String(serverPin.getPassword());
        String requestedPassword=new String(roomPassword.getPassword());
        boolean ghost=campaignLaunch==null&&role.getSelectedIndex()==1;
        HouseMap.Site requestedSite=(HouseMap.Site)siteChoice.getSelectedItem();
        // Capture the GUI state before the background socket connection begins.
        CustomContract request=new CustomContract((int)partySize.getValue(),
            custom.playerSpeed(),custom.sprintSpeed(),custom.ghostSpeed(),
            custom.energyRecovery(),custom.sanityDrain(),custom.startingSanity(),
            custom.eventFrequency(),custom.sprint(),custom.hidingSpots(),
            custom.sanityMonitor(),custom.activityMonitor(),
            custom.weatherMode(),custom.fixedWeather(),custom.weatherIntensity(),
            custom.hunterSprintSeconds(),custom.ghostSprintSeconds(),
            custom.evidenceLevel());
        Thread task=new Thread(()->{
            LanHost newHost=null;
            try{
                int portNumber=LanProtocol.integer(p,localHost?0:1,65535);
                hbnet.Hbnet.Client tls=null;
                hbnet.Hbnet.Server tlsHost=null;
                if(encrypted){
                    if(localNetwork==null)throw hbnet.Hbnet.failure();
                    if(localHost)tlsHost=hbnet.Hbnet.Server.load(java.nio.file.Path.of(System.getProperty("hbnet.server","server-network")));
                    tls=new hbnet.Hbnet.Client(localHost?tlsHost.fingerprint:expectedPin,"beckenburg",localNetwork.identity,requestedPassword.toCharArray());
                }
                if(localHost){
                    var types=GhostIdentity.Type.values();
                    var chosen=campaignLaunch==null?types[java.util.concurrent.ThreadLocalRandom.current().nextInt(types.length)]:campaignLaunch.chapter().ghost();
                    newHost=encrypted?new LanHost("0.0.0.0",portNumber,chosen,request,ghost,requestedSite,tlsHost):new LanHost(campaignLaunch!=null&&!campaignLaunch.coop()?"127.0.0.1":"0.0.0.0",portNumber,chosen,request,ghost,true,requestedSite);
                    if(campaignLaunch!=null)newHost.configureCampaign(campaignLaunch.profile(),campaignLaunch.chapter());
                    if(!encrypted)newHost.setLobbyPassword(requestedPassword);portNumber=newHost.port();
                }
                // A remote guest cannot race the local player to H1 or ghost-owner seat.
                String localProof=newHost==null?"":newHost.localOwnerCredential();
                java.util.function.Consumer<String> events=frame->SwingUtilities.invokeLater(()->{if(generation==connectionGeneration)receive(frame);});
                LanClient client=encrypted?new LanClient(localHost?"127.0.0.1":hostname,portNumber,ghost,nickname,localProof,events,tls):new LanClient(localHost?"127.0.0.1":hostname,portNumber,ghost,nickname,requestedPassword,localProof,events);
                activeSecure=tls;
                final LanHost own=newHost;
                SwingUtilities.invokeLater(()->{
                    if(generation!=connectionGeneration){
                        client.close();
                        if(own!=null)own.close();
                        return; // Stale connection completed after EOF or a replacement request.
                    }
                    hosted=own;connection=client;busy=false;
                    action(client::equipmentState);
                    if(!"GHOST".equals(client.seat()))
                        action(()->client.selectVisual((CharacterVisuals.Hunter)hunterAppearance.getSelectedItem()));
                    hunterAppearance.setEnabled(!"GHOST".equals(client.seat()));
                    resumeTicket=null;reconnect.setEnabled(false);
                    if(hosted!=null)clearSavedResume();
                    else saveResume(client);
                    roomPassword.setEnabled(false);
                    siteChoice.setEnabled(false);
                    lobbyLock.setEnabled(hosted!=null);hostInfo.setEnabled(hosted!=null);
                    role.setEnabled(false);
                    status.setText("Seat "+client.seat()+" / "+MissionSites.title(selectedSite.name()));
                    if("GHOST".equals(client.seat()))avatar.showGhost(
                        remoteLook!=null?remoteLook:localLook,remoteLook!=null);
                    else avatar.showHunter((CharacterVisuals.Hunter)hunterAppearance.getSelectedItem());
                    setCommandEnabled(true);log.append("Connected. Ready when your team is assembled.\n");
                    if(pendingStart)SwingUtilities.invokeLater(this::requestStartRound);
                });
            }catch(Exception ex){
                if(newHost!=null)newHost.close();
                String msg="Connection unavailable (NET-04)";
                SwingUtilities.invokeLater(()->{
                    if(generation!=connectionGeneration)return;
                    busy=false;pendingStart=false;waitingReadyAck=false;
                    status.setText("Connection failed");
                    log.append(LanNetworkDiagnostics.explain(ex)+"\nDetail: "+msg+"\n");
                });
            }
        },"beckenburg-connect");task.setDaemon(true);task.start();
    }
    private void reconnect(){
        if(busy||connection!=null||resumeTicket==null)return;
        LanClient.ResumeTicket ticket=resumeTicket;
        final String address=resumeAddress;final int reconnectPort=resumePort;
        busy=true;reconnect.setEnabled(false);status.setText("Reconnecting to original seat...");
        long generation=++connectionGeneration;
        Thread task=new Thread(()->{
            try{
                java.util.function.Consumer<String> restoreEvents=
                    frame->SwingUtilities.invokeLater(()->{
                        if(generation==connectionGeneration)receive(frame);
                    });
                LanClient restored=activeSecure==null?new LanClient(address,reconnectPort,ticket,restoreEvents):new LanClient(address,reconnectPort,ticket,restoreEvents,activeSecure);
                SwingUtilities.invokeLater(()->{
                    if(generation!=connectionGeneration){restored.close();return;}
                    connection=restored;resumeTicket=null;busy=false;
                    if(hosted!=null){
                        // The local host lives in this same JVM, so the rotated token
                        // is retained by the connected client for its lifetime.
                        // No stale on-disk guest ticket is written for a listen host.
                        try{restored.acknowledgeStoredResume();}
                        catch(IOException ex){
                            log.append("Local host reconnect acknowledgement unavailable: "+"Operation unavailable (NET-04)"+"\n");
                        }
                    }else if(saveResume(restored)){
                        // Only retire the old host capability AFTER the replacement
                        // has been durably persisted for process-restart recovery.
                        try{restored.acknowledgeStoredResume();}
                        catch(IOException ex){
                            log.append("Recovery acknowledgement unavailable: "+"Operation unavailable (NET-04)"+"\n");
                        }
                    }else{
                        log.append("Old recovery token remains valid during this match if the connection drops again.\n");
                    }
                    missionRunning=true;worldMode=false;
                    role.setSelectedIndex("GHOST".equals(restored.seat())?1:0);
                    role.setEnabled(false);reconnect.setEnabled(false);
                    hunterAppearance.setEnabled(false);
                    roomPassword.setEnabled(false);
                    lobbyLock.setEnabled(hosted!=null);
                    if(hosted!=null)lobbyLock.setSelected(hosted.lobbyLocked());
                    hostInfo.setEnabled(hosted!=null);
                    status.setText("Seat "+restored.seat()+" restored - enter 3D world");
                    caseStatus.setText("Case: mission resumed");
                    setCommandEnabled(true);
                    log.append("Reconnected to the same mission. Reopen 3D view to restore live audio and scene.\n");
                });
            }catch(Exception ex){
                SwingUtilities.invokeLater(()->{
                    if(generation!=connectionGeneration)return;
                    busy=false;reconnect.setEnabled(true);
                    if("Operation unavailable (NET-04)"!=null&&"Operation unavailable (NET-04)".contains("reconnect seat expired")){
                        resumeTicket=null;reconnect.setEnabled(false);clearSavedResume();
                    }
                    status.setText("Reconnect unavailable");
                    log.append("Reconnect failed: "+"Operation unavailable (NET-04)"+"\n");
                });
            }
        },"beckenburg-resume");
        task.setDaemon(true);task.start();
    }
    private void open3D(){
        if(connection==null||!missionRunning)return;
        if(fieldContents!=null){enterWorld.accept(fieldContents);
            JComponent active=worldView!=null?worldView:ghostView;if(active!=null)active.requestFocusInWindow();return;}
        boolean ghost="GHOST".equals(connection.seat());
        try{if(ghost)connection.viewGhost3D();else connection.view3D();}
        catch(IOException ex){log.append("3D start failed: "+"Operation unavailable (NET-04)"+"\n");return;}
        worldMode=true;setCommandEnabled(true);
        JComponent active;
        if(ghost){ghostView=new LanGhostPanel(connection);active=ghostView;}
        else {worldView=new LanWorldPanel(connection);worldView.setCampaignInventory(campaignGuest||campaignLaunch!=null);active=worldView;}
        JPanel field=new JPanel(new BorderLayout());fieldContents=field;
        JButton returnLobby=PsxHorrorUi.button("RETURN TO CASTLE LOBBY");
        Runnable returnToLobby=()->{closeWorld();worldMode=false;enterWorld.accept(this);};
        field.putClientProperty("beckenburg.returnToLobby",returnToLobby);
        returnLobby.addActionListener(e->returnToLobby.run());
        field.add(active,BorderLayout.CENTER);field.add(returnLobby,BorderLayout.SOUTH);
        enterWorld.accept(field);SwingUtilities.invokeLater(active::requestFocusInWindow);
        // The host itself now replays the role-filtered casefile, notes, equipment,
        // progress and 3D world on VIEW3D; do not race or duplicate a second journal reset.
        // Subsequent frames update this viewport.
    }
    private void receive(String event){
        if(event.equals("INFO|network connection ended")){
            // This event is a transport loss, NOT necessarily the death of the
            // authoritative listen-host process. Keep a running local host alive.
            connectionGeneration++;
            // Capture the host-issued per-seat reconnect capability before closing
            // the old socket. It is not the local installation/player profile ID.
            resumeTicket=connection==null?null:connection.resumeTicket();
            if(connection!=null){
                resumeAddress=connection.address();resumePort=connection.port();
                saveResume(connection);
            }
            boolean localHostSurvives=keepRunningLocalHost(hosted,resumeTicket);
            if(hosted!=null&&!localHostSurvives)resumeTicket=null;
            reconnect.setEnabled(resumeTicket!=null);
            missionRunning=false;worldMode=false;busy=false;
            pendingStart=false;startCommandSent=false;waitingReadyAck=false;
            localReady=false;serverReady=false;ready.setText("READY");
            fieldContents=null;
            if(connection!=null){connection.close();connection=null;}
            // An unexpected local TCP EOF does not imply authoritative host death.
            // Destroying the host here would disconnect every remaining investigator.
            if(hosted!=null&&!localHostSurvives){hosted.close();hosted=null;}
            myRoom=HouseMap.EXTRACTION_ROOM;
            role.setEnabled(!localHostSurvives);ghostJoined=false;remoteLook=null;
            hunterAppearance.setEnabled(!localHostSurvives&&role.getSelectedIndex()==0);
            roomPassword.setEnabled(hosted==null);
            siteChoice.setEnabled(hosted==null);
            lobbyLock.setEnabled(localHostSurvives);
            lobbyLock.setSelected(localHostSurvives&&hosted.lobbyLocked());
            hostInfo.setEnabled(localHostSurvives);
            if(role.getSelectedIndex()==1)avatar.showGhost(localLook,false);
            else avatar.showHunter((CharacterVisuals.Hunter)hunterAppearance.getSelectedItem());
            status.setText(localHostSurvives
                ?"Local host still running — reconnect your reserved seat"
                :"Disconnected — host or server connection lost");
            caseStatus.setText(localHostSurvives?"Case: active on local host":"Case: connection ended");
            teamStatus.setText("Team: disconnected");
            setCommandEnabled(false);equipmentPage.disconnect();closeWorld();
            log.append(resumeTicket==null?"Connection ended. You can host or join a fresh match.\n":
                "Connection interrupted. Reconnect to the same mission while the host reserves your seat.\n");
            return;
        }
        if(worldView!=null)worldView.onEvent(event);
        if(ghostView!=null)ghostView.onEvent(event);
        if(event.startsWith("REJECT|")||event.startsWith("ERROR|")||event.startsWith("ACCEPT|"))equipmentPage.notice(event);
        if(event.startsWith("REJECT|")&&pendingStart){
            pendingStart=false;startCommandSent=false;waitingReadyAck=false;
            status.setText("Round not started: "+event.substring("REJECT|".length()));
            setCommandEnabled(connection!=null);
        }
        if(event.startsWith("OWNREADY|")){
            var fields=LanProtocol.parts(event);
            if(fields.size()==2){
                serverReady="YES".equals(fields.get(1));
                localReady=serverReady;
                ready.setText(serverReady?"UNREADY":"READY");
                if(serverReady&&waitingReadyAck&&pendingStart){
                    waitingReadyAck=false;
                    dispatchStartRound();
                }
            }
            return;
        }
        if(event.startsWith("SITE|")){
            var parts=LanProtocol.parts(event);
            if(parts.size()==3){
                try{
                    selectedSite=HouseMap.Site.valueOf(parts.get(1));
                    if(!new HouseMap(selectedSite).level().revision().equals(parts.get(2)))throw new IllegalArgumentException("incompatible level revision");
                    siteChoice.setSelectedItem(selectedSite);

                }catch(IllegalArgumentException rejected){
                    log.append("Unsupported location version received.\n");
                }
            }
            return;
        }
        if(event.startsWith("MODE|CAMPAIGN")){
            campaignGuest=true;equipmentPage.setCampaign(true);
            log.append("CAMPAIGN GUEST SESSION / story and rewards belong to the host.\n");return;
        }
        if(event.startsWith("SUPPLY|")||event.startsWith("GEARSTATE|")||event.startsWith("GEAROWNED|"))
            equipmentPage.receive(event);
        if(event.startsWith("PAYOUT|")){
            try{
                ContractRewards.Settlement settlement=ContractRewards.Settlement.decode(event);
                if(!campaignGuest&&campaignLaunch==null&&connection!=null&&!"GHOST".equals(connection.seat())){
                    ProfileStore wallet=new ProfileStore(ProfileStore.multiplayerPath());
                    wallet.load();
                    boolean applied=wallet.addNetworkPayout(settlement.id(),
                        settlement.credits(),settlement.xp());
                    log.append("CASE SETTLEMENT / "+
                        (settlement.victory()?"RITUAL VICTORY":"ENTITY UNRESOLVED")+
                        " / "+(settlement.identified()?"IDENTIFIED":"UNIDENTIFIED")+
                        " / "+(applied?"PAID":"ALREADY RECORDED")+
                        " £"+settlement.credits()+" +"+settlement.xp()+" XP"+
                        " / OPTIONAL "+settlement.optionalDone()+
                        " / DAILY "+settlement.dailyDone()+"\n");
                }
            }catch(IOException|IllegalArgumentException|ArithmeticException ex){
                log.append("Payout could not be saved locally: "+"Operation unavailable (NET-04)"+"\n");
            }
            log.setCaretPosition(log.getDocument().getLength());
            return;
        }
        if(event.startsWith("OBJECTIVES|")||event.startsWith("TASK|DONE|")
                ||event.startsWith("EVIDENCELEVEL|")){
            log.append(event.replace('|',' ')+"\n");
            return;
        }
        if(event.startsWith("HUNT|"))return; // Forwarded to active 3D view; suppress 1Hz lobby log spam.
        if(event.startsWith("LOBBYCHAT|")){
            var line=LanProtocol.parts(event);
            if(line.size()==4){
                log.append("["+line.get(1)+" / "+line.get(2)+"] "+line.get(3)+"\n");
                log.setCaretPosition(log.getDocument().getLength());
            }
            return;
        }
        if(event.startsWith("INFO|mission started")){
            missionRunning=true;pendingStart=false;startCommandSent=false;waitingReadyAck=false;
            setCommandEnabled(true);
            // START ROUND enters the actual game: no second Enter 3D World click.
            SwingUtilities.invokeLater(()->{if(connection!=null&&missionRunning)open3D();});
        }
        if(event.startsWith("RESULT|")||event.startsWith("INFO|match stopped")){missionRunning=false;setCommandEnabled(true);}
        if(event.startsWith("LOBBYAVATAR|")){
            var parts=LanProtocol.parts(event);
            if(parts.size()==2&&parts.get(1).equals("NONE")){
                ghostJoined=false;remoteLook=null;
                if(connection==null&&role.getSelectedIndex()==1)avatar.showGhost(localLook,false);
                else avatar.showHunter((CharacterVisuals.Hunter)hunterAppearance.getSelectedItem());
            }else{
                remoteLook=LobbyGhostAvatar.Look.decode(event);
                ghostJoined=true;
                if(role.getSelectedIndex()==1)avatar.showGhost(remoteLook,true);
                else avatar.showHunter((CharacterVisuals.Hunter)hunterAppearance.getSelectedItem());
            }
        }else if(event.startsWith("STATE|")){
            var p=LanProtocol.parts(event);if(p.size()>=4)try{myRoom=Integer.parseInt(p.get(2));}catch(NumberFormatException ignored){}
            status.setText("Room: "+new HouseMap(selectedSite).roomName(myRoom)+
                "  |  tick "+p.get(1)+"  |  energy "+p.get(4));
        }else if(event.startsWith("TEAMMAP|")){
            var parts=LanProtocol.parts(event);
            if(parts.size()==7){
                HouseMap layout=new HouseMap(selectedSite);
                StringBuilder summary=new StringBuilder("Hunters");
                for(int i=1;i<=6;i++)summary.append(" H").append(i).append(':')
                        .append(teamRoom(layout,parts.get(i)));
                teamStatus.setText(summary.toString());
            }
        }else if(event.startsWith("PARTY|")){
            var parts=LanProtocol.parts(event);
            if(parts.size()==3)status.setText("Party "+parts.get(1)+"/6  /  difficulty +"+parts.get(2));
        }else if(event.startsWith("PROGRESS|")){
            var fields=LanProtocol.parts(event);
            if(fields.size()==8)caseStatus.setText("Health "+fields.get(1)+"  Fear "+fields.get(2)+"  Wards "+fields.get(3)+"  Evidence "+fields.get(4)+"/3  Circle "+fields.get(5)+"  Ritual "+fields.get(6)+"  "+fields.get(7));
        }else{log.append(event+"\n");log.setCaretPosition(log.getDocument().getLength());}
    }
    private static String teamRoom(HouseMap map,String raw){
        int room=LanProtocol.integer(raw,-1,HouseMap.ROOM_COUNT-1);
        return room<0?"DISCONNECTED":map.roomName(room);
    }
    private void setCommandEnabled(boolean available){
        lobbyChat.setEnabled(available&&!missionRunning);
        sendLobbyChat.setEnabled(available&&!missionRunning);
        ready.setEnabled(available&&!missionRunning);
        // Production GUI hosts explicitly claim H1 or GHOST at creation.
        // A remote guest occupying H1 in a ghost-hosted room is NOT the owner.
        start.setEnabled(available&&!missionRunning&&hosted!=null&&connection!=null&&
            ("H1".equals(connection.seat())||"GHOST".equals(connection.seat())));
        move.setEnabled(available&&!worldMode&&connection!=null&&!"GHOST".equals(connection.seat()));
        use.setEnabled(available&&connection!=null&&"GHOST".equals(connection.seat()));
        boolean hunter=available&&connection!=null&&!"GHOST".equals(connection.seat());
        scan.setEnabled(hunter);ward.setEnabled(hunter);cleanse.setEnabled(hunter);
        banish.setEnabled(hunter);absolve.setEnabled(hunter);
        identify.setEnabled(hunter);hypothesis.setEnabled(hunter);
        extract.setEnabled(hunter);
        instrument.setEnabled(hunter);
        enter3D.setEnabled(available&&missionRunning&&connection!=null);
    }
    private void send(String frame){try{if(connection==null)throw new IOException("not connected");connection.send(frame);}
        catch(Exception e){log.append("Send failed: "+"Operation unavailable (NET-04)"+"\n");}}
}
