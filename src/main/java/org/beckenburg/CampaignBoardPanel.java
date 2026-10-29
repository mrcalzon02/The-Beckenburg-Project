package org.beckenburg;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Objects;

/** Compact campaign contract browser. Only genuine SiteCatalog maps can deploy. */
public final class CampaignBoardPanel extends JPanel {
    public interface Actions {
        void deploy(SiteCatalog.Location location,boolean withAssistant);
        void castle();
        void tutorial();
        default void menu(){castle();}
        default void deployTeam(SiteCatalog.Location location,int hires,boolean coop){deploy(location,hires>0);}
        default void discipline(InvestigationSession.Role role){}
        default InvestigationSession.Role currentDiscipline(){return InvestigationSession.Role.CLERIC;}
    }
    public record Entry(String name,String region,String dossier,SiteCatalog.Location map) {
        public boolean playable(){return map!=null&&map.playable();}
        @Override public String toString(){return name+(playable()?"  /  AVAILABLE":"  /  PLANNED");}
    }
    public static List<Entry> catalog(){
        return MissionSites.campaign().stream()
            .map(e->new Entry(e.title(),e.region(),e.description(),e.location())).toList();
    }
    private final JList<Entry> contracts=new JList<>();
    private final JLabel status=new JLabel();
    private final JTextArea dossier=new JTextArea();
    private final MissionBoardPanel sitePictures=new MissionBoardPanel();
    private final JButton deploy=button("START ROUND");
    private final JButton recruit=button("HIRE FIELD ASSISTANT  /  £"+ProfileStore.ASSISTANT_HIRE_COST);
    private final JCheckBox bringAssistant=new JCheckBox("Include booked hunters");
    private final JSpinner hires=new JSpinner(new SpinnerNumberModel(0,0,5,1));
    private final JComboBox<String> mode=new JComboBox<>(new String[]{"SOLO","CO-OP / HOST CAMPAIGN"});
    private final JLabel roster=PsxMenuScreen.label(" ");
    private final JLabel account=new JLabel();
    private final JPanel pages=new JPanel(new CardLayout());
    private final CampaignStorePanel depot;
    private final CampaignArchivePanel caseArchive;
    private final ProfileStore profile;
    private final Actions actions;
    private static final Color PAPER=new Color(230,219,193);
    private static JButton button(String name){
        JButton b=PsxHorrorUi.button(name);
        b.setFont(new Font(Font.SANS_SERIF,Font.BOLD,13));
        b.setBackground(new Color(46,56,63));b.setForeground(PAPER);
        b.setFocusPainted(true);return b;
    }
    public CampaignBoardPanel(ProfileStore profile,Actions actions){
        this.profile=Objects.requireNonNull(profile);
        this.actions=Objects.requireNonNull(actions);
        setLayout(new BorderLayout(8,8));
        setBackground(new Color(13,17,24));
        setBorder(BorderFactory.createEmptyBorder(14,18,14,18));
        setName("campaign.lobby");
        JLabel title=new JLabel("CAMPAIGN LOBBY");
        title.setFont(new Font(Font.SERIF,Font.BOLD,24));title.setForeground(PAPER);
        account.setForeground(new Color(160,215,184));
        JPanel header=new JPanel(new BorderLayout());header.setOpaque(false);
        header.add(title,BorderLayout.NORTH);header.add(account,BorderLayout.SOUTH);
        add(header,BorderLayout.NORTH);
        DefaultListModel<Entry> entries=new DefaultListModel<>();
        for(Entry entry:catalog())entries.addElement(entry);
        contracts.setModel(entries);contracts.setName("campaign.contracts");
        contracts.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        contracts.setBackground(new Color(27,34,42));contracts.setForeground(PAPER);
        contracts.setSelectionBackground(new Color(73,81,74));
        contracts.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12));
        JScrollPane scroll=new JScrollPane(contracts);
        scroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(92,91,78)),"REGIONAL CONTRACTS"));
        scroll.setPreferredSize(new Dimension(340,420));scroll.setMinimumSize(new Dimension(180,100));
        dossier.setEditable(false);dossier.setFocusable(false);dossier.setLineWrap(true);
        dossier.setWrapStyleWord(true);dossier.setMargin(new Insets(12,12,12,12));
        dossier.setBackground(new Color(27,34,42));dossier.setForeground(PAPER);
        dossier.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,14));
        JScrollPane details=new JScrollPane(dossier);
        details.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(92,91,78)),"LOCATION DOSSIER"));
        JSplitPane split=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,scroll,details);
        details.setMinimumSize(new Dimension(180,100));
        split.setResizeWeight(.52);split.setBorder(null);
        depot=new CampaignStorePanel(profile,this::refreshLedger);
        caseArchive=new CampaignArchivePanel(profile);
        pages.add(split,"contracts");pages.add(depot,"depot");pages.add(caseArchive,"archive");
        pages.add(sitePictures,"sites");
        sitePictures.onSelection(site->{
            int index=-1;
            for(int i=0;i<entries.size();i++)if(entries.get(i).name().equals(site.title())){index=i;break;}
            if(index<0)contracts.clearSelection();else contracts.setSelectedIndex(index);
        });
        pages.setBackground(new Color(13,17,24));
        add(pages,BorderLayout.CENTER);
        JPanel footer=new JPanel(new BorderLayout(8,6));footer.setOpaque(false);
        status.setForeground(PAPER);footer.add(status,BorderLayout.NORTH);
        JPanel controls=new JPanel(new FlowLayout(FlowLayout.LEFT,7,0));controls.setOpaque(false);
        JButton tutorial=button("CASTLE ORIENTATION");
        JButton back=button("RETURN TO CASTLE");
        deploy.setName("campaign.deploy");
        recruit.setName("campaign.recruit");
        bringAssistant.setName("campaign.assistant");
        bringAssistant.setOpaque(false);
        bringAssistant.setForeground(PAPER);
        bringAssistant.setSelected(profile.missionHires()>0);
        hires.setValue(profile.missionHires());
        hires.setName("campaign.hires");mode.setName("campaign.mode");
        JButton contractsTab=button("CONTRACTS");
        JButton shopTab=button("SUPPLY DEPOT");
        JButton archiveTab=button("CASE ARCHIVE");
        JButton picturesTab=button("SITE PICTURES");
        picturesTab.setName("campaign.view.sites");
        contractsTab.setName("campaign.view.contracts");
        shopTab.setName("campaign.view.store");
        archiveTab.setName("campaign.view.archive");
        JPanel team=new JPanel(new BorderLayout(8,12));team.setOpaque(false);
        JPanel booking=new JPanel(new GridLayout(0,2,10,14));booking.setOpaque(false);
        booking.add(PsxMenuScreen.label("Play mode"));booking.add(mode);
        JComboBox<InvestigationSession.Role> discipline=new JComboBox<>(InvestigationSession.Role.values());
        discipline.setSelectedItem(actions.currentDiscipline());
        discipline.setName("campaign.discipline");
        discipline.addActionListener(e->actions.discipline((InvestigationSession.Role)discipline.getSelectedItem()));
        booking.add(PsxMenuScreen.label("Your discipline"));booking.add(discipline);
        booking.add(PsxMenuScreen.label("Hire hunters / £"+ProfileStore.ASSISTANT_HIRE_COST+" each mission"));booking.add(hires);
        recruit.setText("UPDATE MISSION HIRES");booking.add(recruit);booking.add(bringAssistant);
        team.add(booking,BorderLayout.NORTH);
        JTextArea rules=new JTextArea("TEAM CAPACITY: SIX HUNTERS\n\nYou occupy one slot. Up to five human guests or hired AI hunters fill the others.\n\nCo-op guests help with your saved chapter. The host owns campaign funds, equipment, rewards and story progress. Guest campaign saves stay unchanged.\n\nHires last for one mission. Update the booking to zero to refund it before deployment. Human guests take priority; displaced hires are refunded at dispatch.\n\nChoose a contract, manage campaign equipment at Supply Depot, then deploy.");
        rules.setEditable(false);rules.setLineWrap(true);rules.setWrapStyleWord(true);
        rules.setBackground(new Color(27,34,42));rules.setForeground(PAPER);rules.setMargin(new Insets(14,14,14,14));
        team.add(new JScrollPane(rules),BorderLayout.CENTER);team.add(roster,BorderLayout.SOUTH);
        pages.add(team,"team");
        JButton teamTab=button("TEAM / SOLO & CO-OP");teamTab.setName("campaign.view.team");
        teamTab.addActionListener(e->((CardLayout)pages.getLayout()).show(pages,"team"));
        JPanel navigation=new JPanel(new GridLayout(2,3,6,6));navigation.setOpaque(false);
        for(JButton b:new JButton[]{contractsTab,teamTab,shopTab,archiveTab,picturesTab,tutorial})navigation.add(b);
        header.add(navigation,BorderLayout.CENTER);
        JButton menu=button("MAIN MENU");menu.setName("campaign.back");menu.addActionListener(e->actions.menu());
        controls.add(back);controls.add(menu);controls.add(deploy);
        contractsTab.addActionListener(event->((CardLayout)pages.getLayout()).show(pages,"contracts"));
        picturesTab.addActionListener(event->((CardLayout)pages.getLayout()).show(pages,"sites"));
        shopTab.addActionListener(event->{
            depot.refresh();((CardLayout)pages.getLayout()).show(pages,"depot");
        });
        archiveTab.addActionListener(event->{
            caseArchive.refresh();((CardLayout)pages.getLayout()).show(pages,"archive");
        });
        footer.add(controls,BorderLayout.SOUTH);add(footer,BorderLayout.SOUTH);
        contracts.addListSelectionListener(event->{if(!event.getValueIsAdjusting())updateSelection();});
        deploy.addActionListener(event->{
            Entry entry=contracts.getSelectedValue();
            int stage=contracts.getSelectedIndex();
            if(entry!=null&&entry.playable()&&profile.chapterUnlocked(stage))
                actions.deployTeam(entry.map(),bringAssistant.isSelected()?profile.missionHires():0,mode.getSelectedIndex()==1);
        });
        recruit.addActionListener(event->{
            try{
                if(profile.reserveMissionHunters((int)hires.getValue())){
                    bringAssistant.setSelected(profile.missionHires()>0);
                    refreshLedger();
                }else status.setText("Insufficient campaign funds for this booking.");
            }catch(java.io.IOException failed){
                status.setText("Recruitment was not saved: "+failed.getMessage());
            }
        });
        tutorial.addActionListener(event->actions.tutorial());
        back.addActionListener(event->actions.castle());
        refreshLedger();
        contracts.setSelectedIndex(0);
        PsxMenuScreen.theme(this);
    }
    public void refreshLedger(){
        account.setText("£"+profile.currency()+"  /  XP "+profile.experience()+
            "  /  CLERIC "+profile.level(InvestigationSession.Role.CLERIC)+
            "  OBSERVER "+profile.level(InvestigationSession.Role.OBSERVER)+
            "  DEMONOLOGIST "+profile.level(InvestigationSession.Role.DEMONOLOGIST)+
            "  /  "+profile.missionHires()+" mission hires reserved");
        recruit.setEnabled(true);
        bringAssistant.setEnabled(profile.missionHires()>0);
        roster.setText("1 HOST + UP TO "+profile.missionHires()+" HIRES / GUESTS REPLACE HIRES / SIX TOTAL");
        updateSelection();
    }
    private void updateSelection(){
        Entry entry=contracts.getSelectedValue();
        int stage=contracts.getSelectedIndex();
        boolean unlocked=profile.chapterUnlocked(stage);
        boolean built=entry!=null&&entry.playable();
        boolean ready=unlocked&&built;
        deploy.setEnabled(ready);
        if(entry==null){dossier.setText("");status.setText("SELECT A CHAPTER");return;}
        String pictureId=MissionSites.campaign().get(stage).id();
        if(!sitePictures.selected().id().equals(pictureId))sitePictures.select(pictureId);
        String progress=entry.map()==null?"": "\n\nVISITS   "+profile.visits(entry.map())+
            "   /   CLEARED   "+profile.clears(entry.map());
        String gate=!unlocked?"Finish chapter "+profile.storyCheckpoint()+" first.":
            !built?"Unique level geometry is still in production; no substitute map can launch.":
            stage<profile.storyCheckpoint()?"Previously completed. Replay for income and equipment.": 
            "Current story chapter. Victory unlocks the next location.";
        dossier.setText("CHAPTER "+(stage+1)+" / "+CampaignStory.LENGTH+
            "\n"+entry.name()+"\nREGION   "+entry.region()+
            progress+"\n\nLORE MASTER'S BRIEFING\n"+entry.dossier()+
            "\n\n"+gate+
            "\n\nNormal multiplayer contracts have their own ghost and inventory.");
        dossier.setCaretPosition(0);
        status.setText(ready?(stage<profile.storyCheckpoint()?
            "REPLAY AVAILABLE / HOST REWARDS":"STORY AVAILABLE / SUCCESS REQUIRED"):
            unlocked?"LEVEL IN PRODUCTION / CANNOT DEPLOY":
            "LOCKED / COMPLETE PREVIOUS STORY CHAPTER");
    }
}
