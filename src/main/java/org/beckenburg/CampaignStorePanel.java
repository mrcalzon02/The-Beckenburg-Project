package org.beckenburg;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Actual persisted Castle quartermaster purchases, without another currency pool. */
public final class CampaignStorePanel extends JPanel {
    public record Offer(String title,int cost,EquipmentLoadout.Item equipment,
            FieldSupplies.Kind kind,FieldSupplies.Tier tier){
        public Offer{
            if(title==null||cost<1||(equipment==null)==(kind==null)||((kind==null)!=(tier==null)))
                throw new IllegalArgumentException("invalid offer");
        }
        @Override public String toString(){return title+"  /  £"+cost;}
    }
    private static final Color PAPER=new Color(229,220,197);
    public static List<Offer> catalog(){
        List<Offer> list=new ArrayList<>();
        for(EquipmentLoadout.Item item:EquipmentLoadout.Item.values())
            if(ProfileStore.equipmentPrice(item)>0)
                list.add(new Offer(item.label,ProfileStore.equipmentPrice(item),item,null,null));
        for(FieldSupplies.Kind kind:FieldSupplies.Kind.values())
            for(FieldSupplies.Tier tier:FieldSupplies.Tier.values())
                list.add(new Offer(kind.name()+"  "+tier.name(),tier.price,null,kind,tier));
        return List.copyOf(list);
    }
    private final ProfileStore profile;
    private final Runnable afterPurchase;
    private final JList<Offer> offers=new JList<>(catalog().toArray(new Offer[0]));
    private final JTextArea details=new JTextArea();
    private final JLabel balance=new JLabel();
    private final JButton purchase=new JButton("BUY SELECTED");
    private final JButton sell=new JButton("SELL ONE / 50%");
    private final JLabel preview=new JLabel("",SwingConstants.CENTER);
    private final JLabel libraryDetails=new JLabel("Approved source models / reference only");
    private final List<JComboBox<EquipmentLoadout.Item>> slots=new ArrayList<>();
    private final JLabel preparedStatus=new JLabel("Next mission loadout");
    public CampaignStorePanel(ProfileStore profile,Runnable afterPurchase){
        this.profile=Objects.requireNonNull(profile);
        this.afterPurchase=Objects.requireNonNull(afterPurchase);
        setName("campaign.store");
        setLayout(new BorderLayout(6,6));
        setBackground(new Color(16,22,30));
        setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        offers.setName("campaign.store.items");
        offers.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        offers.setFont(new Font(Font.MONOSPACED,Font.PLAIN,13));
        offers.setBackground(new Color(28,34,42));offers.setForeground(PAPER);
        offers.setSelectionBackground(new Color(81,89,75));
        details.setName("campaign.store.details");
        details.setEditable(false);details.setFocusable(false);details.setLineWrap(true);
        details.setWrapStyleWord(true);details.setBackground(new Color(28,34,42));
        details.setForeground(PAPER);details.setMargin(new Insets(12,12,12,12));
        offers.setFixedCellHeight(62);
        offers.setCellRenderer(new DefaultListCellRenderer(){
            @Override public Component getListCellRendererComponent(JList<?> list,Object value,
                    int index,boolean selected,boolean focused){
                JLabel cell=(JLabel)super.getListCellRendererComponent(list,value,index,selected,focused);
                if(value instanceof Offer offer){
                    cell.setIcon(iconFor(offer,48));
                    cell.setIconTextGap(10);
                    String status=offer.equipment()!=null?
                        (profile.ownsEquipment(offer.equipment())?"OWNED":"NOT OWNED"):
                        "STOCK "+profile.supplyCount(offer.kind(),offer.tier())+"/4";
                    cell.setText("<html>"+offer.title()+"<br><small>£"+offer.cost()+
                        "  /  "+status+"</small></html>");
                }
                return cell;
            }
        });
        preview.setName("campaign.store.preview");
        preview.setPreferredSize(new Dimension(118,112));
        preview.setForeground(PAPER);
        JPanel description=new JPanel(new BorderLayout());
        description.setBackground(new Color(28,34,42));
        description.add(preview,BorderLayout.NORTH);
        description.add(new JScrollPane(details),BorderLayout.CENTER);

        JList<EquipmentRegistry.Tool> reference=new JList<>(
            EquipmentRegistry.all().toArray(new EquipmentRegistry.Tool[0]));
        reference.setName("campaign.store.reference");
        reference.setBackground(new Color(28,34,42));reference.setForeground(PAPER);
        reference.setFixedCellHeight(56);
        reference.setCellRenderer(new DefaultListCellRenderer(){
            @Override public Component getListCellRendererComponent(JList<?> list,Object value,
                    int index,boolean selected,boolean focused){
                JLabel cell=(JLabel)super.getListCellRendererComponent(list,value,index,selected,focused);
                if(value instanceof EquipmentRegistry.Tool tool){
                    cell.setText(tool.name());
                    cell.setIcon(ItemIconAtlas.get(ItemIconAtlas.key(tool),44));
                    cell.setIconTextGap(10);
                }
                return cell;
            }
        });
        reference.addListSelectionListener(e->{
            if(!e.getValueIsAdjusting()){
                EquipmentRegistry.Tool tool=reference.getSelectedValue();
                libraryDetails.setText(tool==null?"Equipment model library":
                    "<html><b>"+tool.name()+"</b><br>Approved preview / physical model reference."+
                    "<br>Not a purchasable campaign offer.</html>");
            }
        });
        JPanel library=new JPanel(new BorderLayout());
        library.add(new JScrollPane(reference),BorderLayout.CENTER);
        library.add(libraryDetails,BorderLayout.SOUTH);
        JTabbedPane pages=new JTabbedPane();
        pages.setName("campaign.store.pages");
        PsxHorrorUi.styleTabs(pages);
        pages.addTab("QUARTERMASTER",new JScrollPane(offers));
        pages.addTab("MODEL LIBRARY",library);
        pages.addChangeListener(e->{if(pages.getSelectedIndex()==0)refresh();
            else {purchase.setEnabled(false);sell.setEnabled(false);
                details.setText("MODEL LIBRARY\n\nApproved 3D equipment preview reference.\n"+
                    "Only equipment listed under QUARTERMASTER is currently purchasable.");
                preview.setIcon(null);}
        });
        JSplitPane split=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            pages,description);
        split.setBorder(null);split.setResizeWeight(.48);
        add(split,BorderLayout.CENTER);
        JPanel footer=new JPanel(new FlowLayout(FlowLayout.LEFT,8,4));footer.setOpaque(false);
        balance.setForeground(new Color(170,222,186));
        purchase.setName("campaign.store.buy");
        sell.setName("campaign.store.sell");
        PsxHorrorUi.styleButton(purchase);
        PsxHorrorUi.styleButton(sell);
        footer.add(purchase);footer.add(sell);footer.add(balance);
        JPanel preparation=new JPanel(new FlowLayout(FlowLayout.LEFT,4,3));
        preparation.setOpaque(false);
        preparation.add(new JLabel("EQUIP"));
        for(int i=0;i<EquipmentLoadout.SLOTS;i++){
            JComboBox<EquipmentLoadout.Item> box=new JComboBox<>();
            box.setName("campaign.store.slot."+i);
            box.setToolTipText("Mission instrument slot "+(i+1));
            box.setPreferredSize(new Dimension(96,26));
            slots.add(box);preparation.add(box);
        }
        JButton commit=new JButton("SAVE LOADOUT");
        commit.setName("campaign.store.prepare");
        PsxHorrorUi.styleButton(commit);
        commit.addActionListener(e->prepareSelected());
        preparation.add(commit);
        preparedStatus.setForeground(PAPER);
        footer.add(preparedStatus);
        JPanel bottom=new JPanel(new BorderLayout());bottom.setOpaque(false);
        bottom.add(preparation,BorderLayout.NORTH);
        bottom.add(footer,BorderLayout.SOUTH);add(bottom,BorderLayout.SOUTH);
        refreshPrepared();
        offers.addListSelectionListener(e->{if(!e.getValueIsAdjusting())refresh();});
        purchase.addActionListener(e->buySelected());
        sell.addActionListener(e->sellSelected());
        offers.setSelectedIndex(0);refresh();
    }
    private static Icon iconFor(Offer offer,int pixels){
        String key=offer.equipment()==null?ItemIconAtlas.key(offer.kind()):
            ItemIconAtlas.key(offer.equipment());
        return ItemIconAtlas.get(key,pixels);
    }
    public boolean buySelected(){
        Offer offer=offers.getSelectedValue();
        if(offer==null)return false;
        try{
            boolean bought=offer.equipment()!=null?profile.purchaseEquipment(offer.equipment()):
                profile.purchase(offer.kind(),offer.tier());
            refresh();
            if(bought){refreshPrepared();afterPurchase.run();}
            return bought;
        }catch(IOException error){
            details.setText("Purchase could not be saved.\n"+error.getMessage());
            return false;
        }
    }
    /** Refuses permanent gear resale so active mission loadouts cannot become invalid. */
    public boolean sellSelected(){
        Offer offer=offers.getSelectedValue();
        if(offer==null||offer.kind()==null)return false;
        try{
            boolean sold=profile.sellSupply(offer.kind(),offer.tier());
            refresh();
            if(sold)afterPurchase.run();
            return sold;
        }catch(IOException error){
            details.setText("Sale could not be saved.\n"+error.getMessage());
            return false;
        }
    }
    private void refreshPrepared(){
        EquipmentLoadout.Item[] current=profile.preparedLoadout();
        for(int i=0;i<slots.size();i++){
            JComboBox<EquipmentLoadout.Item> box=slots.get(i);
            box.removeAllItems();
            for(var item:EquipmentLoadout.Item.values())
                if(profile.ownsEquipment(item))box.addItem(item);
            box.setSelectedItem(current[i]);
        }
    }
    public boolean prepareSelected(){
        EquipmentLoadout.Item[] chosen=new EquipmentLoadout.Item[EquipmentLoadout.SLOTS];
        for(int i=0;i<slots.size();i++)chosen[i]=(EquipmentLoadout.Item)slots.get(i).getSelectedItem();
        try{
            profile.prepareLoadout(chosen);
            preparedStatus.setText("SAVED");
            afterPurchase.run();
            return true;
        }catch(IOException|IllegalArgumentException error){
            preparedStatus.setText("NOT SAVED: "+error.getMessage());return false;
        }
    }
    public void refresh(){
        Offer offer=offers.getSelectedValue();
        balance.setText("CASTLE FUNDS  £"+profile.currency());
        offers.repaint();
        if(offer==null){
            purchase.setEnabled(false);sell.setEnabled(false);
            preview.setIcon(null);details.setText("Select supplies.");return;
        }
        preview.setIcon(iconFor(offer,96));
        boolean owned=offer.equipment()!=null&&profile.ownsEquipment(offer.equipment());
        int stock=offer.kind()==null?0:profile.supplyCount(offer.kind(),offer.tier());
        boolean full=offer.kind()!=null&&stock>=4;
        purchase.setEnabled(!owned&&!full&&profile.currency()>=offer.cost());
        sell.setEnabled(offer.kind()!=null&&stock>0);
        details.setText(offer.title()+"\n\nCOST  £"+offer.cost()+
            "\n"+(offer.equipment()!=null?
                (owned?"OWNED  /  available for loadout":"New permanent gear  /  "+offer.equipment().description):
                "IN STOCK  "+stock+" / 4  /  "+offer.tier().restore(offer.kind())+
                (offer.kind()==FieldSupplies.Kind.HEALTH?" health restored":" sanity restored"))+
            "\n\n"+
            (owned?"Already owned. Permanent gear cannot be resold.":full?"Stock limit reached.":
                profile.currency()<offer.cost()?"Insufficient funds.":
                "Purchase is charged to the persistent Castle account.")+
            (offer.kind()!=null?"\n\nSELL ONE: £"+(offer.cost()/2)+
                " refunded, immediately persisted.":""));
    }
}
