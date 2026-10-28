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
    /** A single inventory, not a purchasable mini-catalog beside a fake library.
     * The source-equipped LAN inventory is visible, but only real solo goods may
     * debit the local Castle account until the solo dispatch handles them. */
    private record Row(Offer offer,EquipmentRegistry.Tool tool){
        String title(){return offer!=null?offer.title():tool.name();}
        @Override public String toString(){return title();}
        String iconKey(){
            if(offer!=null&&offer.equipment()!=null)return ItemIconAtlas.key(offer.equipment());
            if(offer!=null)return ItemIconAtlas.key(offer.kind());
            return ItemIconAtlas.key(tool);
        }
        String purpose(){
            return tool!=null?tool.purpose().name():"SUPPLIES";
        }
    }
    private static List<Row> fullCatalog(){
        List<Row> result=new ArrayList<>();
        // Keep the actual purchasable catalog at the front of the ordinary
        // Castle interface, followed by all remaining authored equipment.
        for(Offer offer:catalog()){
            String id=offer.equipment()==null?
                (offer.kind()==FieldSupplies.Kind.HEALTH?null:"SANITY_MEDICATION"):
                switch(offer.equipment()){
                    case EMF -> "EMF_READER";
                    case THERMOMETER -> "THERMOMETER";
                    case SPIRIT_BOX -> "SPIRIT_BOX";
                    case UV_LAMP -> "UV_LIGHT";
                    case PHOTO_CAMERA -> "PHOTO_CAMERA";
                    case SALT -> "SALT";
                };
            result.add(new Row(offer,id==null?null:EquipmentRegistry.get(id)));
        }
        for(EquipmentRegistry.Tool tool:EquipmentRegistry.all()){
            if(tool.id().equals("UV_LIGHT")||tool.id().equals("SALT")
                    ||tool.id().equals("SANITY_MEDICATION"))continue;
            result.add(new Row(null,tool));
        }
        return List.copyOf(result);
    }
    private final DefaultListModel<Row> inventory=new DefaultListModel<>();
    private final JList<Row> offers=new JList<>(inventory);
    private final JComboBox<String> filter=new JComboBox<>(
        new String[]{"ALL GEAR","SOLO AVAILABLE","LAN EQUIPMENT",
            "EVIDENCE","SURVEY","COUNTER","RITUAL","MONITOR","CONSUMABLE"});

    private final JTextArea details=new JTextArea();
    private final JLabel balance=new JLabel();
    private final JButton purchase=new JButton("BUY SELECTED");
    private final JButton sell=new JButton("SELL ONE / 50%");
    private final JLabel preview=new JLabel("",SwingConstants.CENTER);
    private final List<JComboBox<EquipmentLoadout.Item>> slots=new ArrayList<>();
    private final JLabel preparedStatus=new JLabel("Next mission loadout");
    public CampaignStorePanel(ProfileStore profile,Runnable afterPurchase){
        this.profile=Objects.requireNonNull(profile);
        this.afterPurchase=Objects.requireNonNull(afterPurchase);
        setName("campaign.store"); // legacy component ID, shared by Castle and campaign
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
        filter.setName("castle.store.filter");
        filter.setBackground(new Color(30,37,45));
        filter.setForeground(PAPER);
        filter.setFont(new Font(Font.MONOSPACED,Font.BOLD,12));
        filter.addActionListener(e->updateFilter());
        updateFilter();
        offers.setCellRenderer(new DefaultListCellRenderer(){
            @Override public Component getListCellRendererComponent(JList<?> list,Object value,
                    int index,boolean selected,boolean focused){
                JLabel cell=(JLabel)super.getListCellRendererComponent(list,value,index,selected,focused);
                if(value instanceof Row row){
                    cell.setIcon(ItemIconAtlas.get(row.iconKey(),48));
                    cell.setIconTextGap(10);
                    cell.setText("<html>"+row.title()+"<br><small>"+
                        availability(row)+"</small></html>");
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

        JPanel catalogPane=PsxHorrorUi.panel(new BorderLayout(5,7));
        catalogPane.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));
        JPanel toolbar=new JPanel(new BorderLayout(7,0));
        toolbar.setOpaque(false);
        JLabel heading=new JLabel("CASTLE EQUIPMENT / 42 TOOL FAMILIES");
        heading.setForeground(PAPER);
        heading.setFont(new Font(Font.MONOSPACED,Font.BOLD,12));
        toolbar.add(heading,BorderLayout.NORTH);
        toolbar.add(filter,BorderLayout.CENTER);
        catalogPane.add(toolbar,BorderLayout.NORTH);
        catalogPane.add(new JScrollPane(offers),BorderLayout.CENTER);
        JSplitPane split=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            catalogPane,description);
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
    private void updateFilter(){
        Row selected=offers.getSelectedValue();
        String selectedFilter=(String)filter.getSelectedItem();
        inventory.clear();
        for(Row row:fullCatalog()){
            boolean solo=row.offer()!=null||soloStarter(row.tool());
            boolean lan=row.tool()!=null&&FieldEquipment.functional(row.tool().id());
            String purpose=row.purpose();
            boolean included=switch(selectedFilter==null?"ALL GEAR":selectedFilter){
                case "SOLO AVAILABLE" -> solo;
                case "LAN EQUIPMENT" -> lan;
                case "EVIDENCE","SURVEY","COUNTER","RITUAL","MONITOR","CONSUMABLE"
                    -> selectedFilter.equals(purpose)
                       ||selectedFilter.equals("CONSUMABLE")&&row.offer()!=null&&row.offer().kind()!=null;
                default -> true;
            };
            if(included)inventory.addElement(row);
        }
        int found=-1;
        if(selected!=null)for(int i=0;i<inventory.size();i++)
            if(selected.equals(inventory.get(i))){found=i;break;}
        if(found>=0)offers.setSelectedIndex(found);
        else if(inventory.size()>0)offers.setSelectedIndex(0);
        refresh();
    }
    private static boolean soloStarter(EquipmentRegistry.Tool tool){
        return tool!=null&&switch(tool.id()){
            case "EMF_READER","THERMOMETER","SPIRIT_BOX","PHOTO_CAMERA" -> true;
            default -> false;
        };
    }
    private String availability(Row row){
        Offer offer=row.offer();
        if(offer!=null){
            if(offer.equipment()!=null)
                return "SOLO / "+(profile.ownsEquipment(offer.equipment())?"OWNED":"£"+offer.cost()+" / BUY");
            return "SOLO / £"+offer.cost()+" / STOCK "+
                profile.supplyCount(offer.kind(),offer.tier())+"/4";
        }
        var tool=row.tool();
        if(soloStarter(tool))return "SOLO / ISSUED STARTER / OWNED";
        return tool.origin()==EquipmentRegistry.Origin.TRUCK?"VAN FIXTURE / NOT CARRIED":
            FieldEquipment.functional(tool.id())?"LAN FIELD READY / SOLO NOT YET WIRED":
            "NOT AVAILABLE IN CURRENT GAME MODE";
    }
    public boolean buySelected(){
        Row row=offers.getSelectedValue();
        Offer offer=row==null?null:row.offer();
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
        Row row=offers.getSelectedValue();
        Offer offer=row==null?null:row.offer();
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
        Row row=offers.getSelectedValue();
        Offer offer=row==null?null:row.offer();
        balance.setText("CASTLE FUNDS  £"+profile.currency());
        offers.repaint();
        if(row==null){
            purchase.setEnabled(false);sell.setEnabled(false);
            preview.setIcon(null);details.setText("Select equipment.");return;
        }
        if(offer==null){
            purchase.setEnabled(false);sell.setEnabled(false);
            preview.setIcon(ItemIconAtlas.get(row.iconKey(),96));
            EquipmentRegistry.Tool tool=row.tool();
            boolean lan=FieldEquipment.functional(tool.id());
            var tier=EquipmentStoreDescriptions.describe(tool.id(),EquipmentTiers.Tier.I);
            details.setText(tool.name()+"\n\n"+availability(row)+
                "\n\n"+tier.effect()+"\n"+tier.specificationLine()+
                (tool.origin()==EquipmentRegistry.Origin.TRUCK?
                    "\n\nFIXED VAN EQUIPMENT / NOT A PORTABLE PURCHASE":
                    "\n\nTIER I  £"+tier.price()+" / TIER II £"+
                    EquipmentTiers.stage(tool.id(),EquipmentTiers.Tier.II).creditCost()+
                    " / TIER III £"+EquipmentTiers.stage(tool.id(),EquipmentTiers.Tier.III).creditCost())+
                "\n\n"+(soloStarter(tool)?
                    "Issued as standard Castle equipment and available in the "+
                    "solo loadout. Starter gear does not require repurchase.":
                    lan?
                    "The multiplayer host supports this instrument. Castle solo "+
                    "missions still use a separate six-tool loadout. This shop "+
                    "does not charge your Castle account for equipment the "+
                    "solo expedition cannot equip.":
                    "Fixed van hardware or unfinished solo equipment. "+
                    "No purchase is offered until real deployment is connected."));
            details.setCaretPosition(0);
            return;
        }
        preview.setIcon(ItemIconAtlas.get(row.iconKey(),96));
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
