package org.beckenburg;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.function.*;

/** One tier-by-tier equipment library shared by campaign and multiplayer. */
public class EquipmentCatalogPanel extends JPanel {
    public record Offer(String id,EquipmentTiers.Tier tier,FieldSupplies.Kind medicine){
        public String title(){return medicine==FieldSupplies.Kind.HEALTH?"Health supplies":EquipmentRegistry.get(id).name();}
        public int price(){return medicine==null?EquipmentTiers.stage(id,tier).creditCost():FieldSupplies.Tier.values()[tier.ordinal()].price;}
        public boolean functional(){return medicine!=null||FieldEquipment.functional(id);}
        public String icon(){return id.toLowerCase(Locale.ROOT);}
    }
    public static List<Offer> catalog(){
        List<Offer> offers=new ArrayList<>();
        for(var item:EquipmentRegistry.all())for(var tier:EquipmentTiers.Tier.values())
            offers.add(new Offer(item.id(),tier,item.id().equals("SANITY_MEDICATION")?FieldSupplies.Kind.SANITY:null));
        for(var tier:EquipmentTiers.Tier.values())offers.add(new Offer("HEALTH_SUPPLIES",tier,FieldSupplies.Kind.HEALTH));
        return List.copyOf(offers);
    }
    public static java.util.Set<String> representedEquipmentIds(){
        return EquipmentRegistry.all().stream().map(EquipmentRegistry.Tool::id).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
    protected final JList<Offer> offers=new JList<>(catalog().toArray(Offer[]::new));
    private final JTextArea details=new JTextArea();
    private final JLabel preview=new JLabel("",SwingConstants.CENTER){
        @Override protected void paintComponent(Graphics g){
            if(!(getIcon() instanceof ImageIcon icon)){super.paintComponent(g);return;}
            int size=Math.max(1,Math.min(getWidth()-8,getHeight()-8));
            g.drawImage(icon.getImage(),(getWidth()-size)/2,(getHeight()-size)/2,size,size,null);
        }
    },balance=PsxMenuScreen.label("Connect to load inventory");
    private final JLabel notice=PsxMenuScreen.label(" ");
    private final JButton buy=PsxHorrorUi.button("BUY"),equip=PsxHorrorUi.button("EQUIP"),sell=PsxHorrorUi.button("SELL ONE / 50%");
    private final JComboBox<Integer> slot=new JComboBox<>(new Integer[]{1,2,3,4});
    private final Map<String,EquipmentTiers.Tier> owned=new HashMap<>();
    private final String[] carried=new String[4];private final int[] medicines=new int[6];
    private int credits;private boolean connected,campaign;
    private ProfileStore profile;private Runnable changed;
    private final Consumer<EquipmentRackDialog.Choice> gearRequest;
    private final BiConsumer<FieldSupplies.Kind,FieldSupplies.Tier> supplyRequest;
    public EquipmentCatalogPanel(ProfileStore profile,Runnable changed){
        this((Consumer<EquipmentRackDialog.Choice>)null,null);this.profile=Objects.requireNonNull(profile);this.changed=Objects.requireNonNull(changed);
        campaign=true;refresh();
    }
    public EquipmentCatalogPanel(Consumer<EquipmentRackDialog.Choice> gearRequest,
            BiConsumer<FieldSupplies.Kind,FieldSupplies.Tier> supplyRequest){
        this.gearRequest=gearRequest;this.supplyRequest=supplyRequest;
        setName("equipment.catalog");setLayout(new BorderLayout(8,8));setOpaque(false);
        offers.setName("equipment.catalog.items");offers.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);offers.setFixedCellHeight(68);
        offers.setCellRenderer(new DefaultListCellRenderer(){
            public Component getListCellRendererComponent(JList<?> list,Object value,int index,boolean selected,boolean focus){
                JLabel cell=(JLabel)super.getListCellRendererComponent(list,value,index,selected,focus);
                Offer offer=(Offer)value;cell.setIcon(ItemIconAtlas.getTier(offer.icon(),offer.tier(),58));cell.setIconTextGap(9);
                cell.setText("<html>"+offer.title()+" / T"+(offer.tier().ordinal()+1)+"<br><small>£"+offer.price()+" / "+state(offer)+"</small></html>");return cell;
            }
        });
        preview.setName("equipment.catalog.preview");preview.setMinimumSize(new Dimension(120,100));preview.setPreferredSize(new Dimension(230,230));
        details.setEditable(false);details.setLineWrap(true);details.setWrapStyleWord(true);details.setMargin(new Insets(12,12,12,12));
        details.setName("equipment.catalog.details");
        JScrollPane text=new JScrollPane(details);text.setMinimumSize(new Dimension(180,100));
        JSplitPane description=new JSplitPane(JSplitPane.VERTICAL_SPLIT,preview,text);description.setResizeWeight(.55);description.setBorder(null);
        JScrollPane list=new JScrollPane(offers);list.setMinimumSize(new Dimension(220,100));
        list.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        JSplitPane split=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,list,description);split.setResizeWeight(.5);split.setDividerLocation(340);
        add(balance,BorderLayout.NORTH);add(split,BorderLayout.CENTER);
        JPanel footer=new JPanel(new BorderLayout(0,5));footer.setOpaque(false);
        JPanel controls=new JPanel(new FlowLayout(FlowLayout.LEFT,6,2));controls.setOpaque(false);
        buy.setName("equipment.catalog.buy");sell.setName("equipment.catalog.sell");equip.setName("equipment.catalog.equip");
        controls.add(buy);controls.add(sell);controls.add(PsxMenuScreen.label("Slot"));controls.add(slot);controls.add(equip);
        footer.add(controls,BorderLayout.NORTH);footer.add(notice,BorderLayout.SOUTH);add(footer,BorderLayout.SOUTH);
        buy.addActionListener(e->buySelected());sell.addActionListener(e->sellSelected());equip.addActionListener(e->equipSelected());
        offers.addListSelectionListener(e->{if(!e.getValueIsAdjusting())refresh();});
        offers.setSelectedIndex(0);refresh();PsxMenuScreen.theme(this);
    }
    private String state(Offer offer){
        if(!offer.functional())return "UNAVAILABLE";
        if(offer.medicine()!=null)return "STOCK "+medicines[offer.medicine().ordinal()*3+offer.tier().ordinal()]+"/4";
        var tier=owned.get(offer.id());return tier==null?"NOT OWNED":tier==offer.tier()?"OWNED":
            tier.ordinal()>offer.tier().ordinal()?"UPGRADED TO T"+(tier.ordinal()+1):"UPGRADE";
    }
    public void setCampaign(boolean value){campaign=value;refresh();}
    public void disconnect(){connected=false;owned.clear();refresh();}
    public void notice(String value){notice.setText(value.replace('|',' '));}
    public void receive(String frame){
        var p=LanProtocol.parts(frame);
        try{
            if(p.get(0).equals("SUPPLY")&&p.size()==8){credits=LanProtocol.integer(p.get(1),0,100000);
                for(int i=0;i<6;i++)medicines[i]=LanProtocol.integer(p.get(i+2),0,4);connected=true;}
            if(p.get(0).equals("GEARSTATE")&&p.size()==7){credits=LanProtocol.integer(p.get(1),0,100000);
                for(int i=0;i<4;i++)carried[i]=p.get(i+3).split(":")[0];connected=true;}
            if(p.get(0).equals("GEAROWNED")){for(int i=1;i<p.size();i++){
                var pair=p.get(i).split(":");owned.put(pair[0],EquipmentTiers.Tier.valueOf(pair[1]));}connected=true;}
            refresh();
        }catch(IllegalArgumentException invalid){notice("Inventory snapshot rejected");}
    }
    public boolean buySelected(){
        Offer offer=offers.getSelectedValue();if(offer==null||!buy.isEnabled())return false;
        if(profile==null){
            if(offer.medicine()!=null)supplyRequest.accept(offer.medicine(),FieldSupplies.Tier.values()[offer.tier().ordinal()]);
            else gearRequest.accept(new EquipmentRackDialog.Choice(EquipmentRackDialog.Action.UPGRADE,offer.id(),0,offer.tier()));
            notice("Purchase requested / awaiting host");return true;
        }
        try{
            boolean bought=offer.medicine()==null?profile.purchaseCatalogGear(offer.id(),offer.tier()):
                profile.purchase(offer.medicine(),FieldSupplies.Tier.values()[offer.tier().ordinal()]);
            refresh();notice(bought?"PURCHASE SAVED":"Purchase unavailable / funds or stock limit");if(bought)changed.run();return bought;
        }catch(IOException failed){notice("NOT SAVED: "+failed.getMessage());return false;}
    }
    public boolean sellSelected(){
        Offer offer=offers.getSelectedValue();if(profile==null||offer==null||!sell.isEnabled())return false;
        try{boolean sold=profile.sellSupply(offer.medicine(),FieldSupplies.Tier.values()[offer.tier().ordinal()]);
            refresh();if(sold){changed.run();notice("SALE SAVED");}return sold;
        }catch(IOException failed){notice("NOT SAVED: "+failed.getMessage());return false;}
    }
    public boolean equipSelected(){
        Offer offer=offers.getSelectedValue();if(offer==null||!equip.isEnabled())return false;
        if(profile==null){gearRequest.accept(new EquipmentRackDialog.Choice(EquipmentRackDialog.Action.EQUIP,offer.id(),slot.getSelectedIndex(),null));
            notice("Loadout requested / awaiting host");return true;}
        try{profile.equipCatalog(slot.getSelectedIndex(),offer.id());refresh();changed.run();notice("LOADOUT SAVED");return true;}
        catch(IOException|IllegalArgumentException failed){notice("NOT SAVED: "+failed.getMessage());return false;}
    }
    public void refresh(){
        if(profile!=null){credits=profile.currency();owned.clear();owned.putAll(profile.catalogEquipment());
            System.arraycopy(profile.catalogLoadout(),0,carried,0,4);
            for(var k:FieldSupplies.Kind.values())for(var t:FieldSupplies.Tier.values())medicines[k.ordinal()*3+t.ordinal()]=profile.supplyCount(k,t);connected=true;}
        balance.setText((campaign?"CAMPAIGN":"MULTIPLAYER")+" EQUIPMENT / "+(connected?"£"+credits:"CONNECT TO LOAD INVENTORY"));
        Offer offer=offers.getSelectedValue();buy.setEnabled(false);equip.setEnabled(false);sell.setEnabled(false);if(offer==null)return;
        var tier=owned.get(offer.id());
        boolean next=tier==null?offer.tier()==EquipmentTiers.Tier.I:offer.tier().ordinal()==tier.ordinal()+1;
        boolean stock=offer.medicine()!=null&&medicines[offer.medicine().ordinal()*3+offer.tier().ordinal()]<4;
        buy.setEnabled(connected&&offer.functional()&&credits>=offer.price()&&(offer.medicine()!=null?stock:next)&&(profile!=null||!campaign));
        equip.setEnabled(connected&&offer.medicine()==null&&tier==offer.tier());
        sell.setEnabled(profile!=null&&offer.medicine()!=null&&!state(offer).equals("STOCK 0/4"));
        buy.setText("BUY T"+(offer.tier().ordinal()+1)+" / £"+offer.price());
        preview.setIcon(ItemIconAtlas.getTier(offer.icon(),offer.tier(),220));
        String copy=offer.medicine()!=null?"Restores "+FieldSupplies.Tier.values()[offer.tier().ordinal()].restore(offer.medicine())+" "+offer.medicine().name().toLowerCase(Locale.ROOT):
            EquipmentStoreDescriptions.describe(offer.id(),offer.tier()).details();
        details.setText(offer.title()+" / TIER "+(offer.tier().ordinal()+1)+"\n"+state(offer)+" / £"+offer.price()+"\n\n"+copy+
            "\n\nYOUR LOADOUT\n"+String.join("\n",Arrays.stream(carried).map(id->id==null?"Awaiting inventory":EquipmentRegistry.get(id).name()).toList())+
            (campaign?"\n\nCampaign inventory belongs to the host. Mission guests keep their own saves.":"\n\nMultiplayer contract inventory. Career earnings are saved separately.")+
            (!offer.functional()?"\n\nThis item is listed here but is not available for purchase until its field behavior is implemented.":""));
        details.setCaretPosition(0);offers.repaint();
    }
}
