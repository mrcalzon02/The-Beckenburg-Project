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
    private final JButton purchase=new JButton("PURCHASE SELECTED");
    public CampaignStorePanel(ProfileStore profile,Runnable afterPurchase){
        this.profile=Objects.requireNonNull(profile);
        this.afterPurchase=Objects.requireNonNull(afterPurchase);
        setName("campaign.store");
        setLayout(new BorderLayout(6,6));
        setBackground(new Color(16,22,30));
        offers.setName("campaign.store.items");
        offers.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        offers.setFont(new Font(Font.MONOSPACED,Font.PLAIN,13));
        offers.setBackground(new Color(28,34,42));offers.setForeground(PAPER);
        offers.setSelectionBackground(new Color(81,89,75));
        details.setName("campaign.store.details");
        details.setEditable(false);details.setFocusable(false);details.setLineWrap(true);
        details.setWrapStyleWord(true);details.setBackground(new Color(28,34,42));
        details.setForeground(PAPER);details.setMargin(new Insets(12,12,12,12));
        JSplitPane split=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            new JScrollPane(offers),new JScrollPane(details));
        split.setBorder(null);split.setResizeWeight(.48);
        add(split,BorderLayout.CENTER);
        JPanel footer=new JPanel(new FlowLayout(FlowLayout.LEFT,8,4));footer.setOpaque(false);
        balance.setForeground(new Color(170,222,186));
        purchase.setName("campaign.store.buy");
        footer.add(purchase);footer.add(balance);add(footer,BorderLayout.SOUTH);
        offers.addListSelectionListener(e->{if(!e.getValueIsAdjusting())refresh();});
        purchase.addActionListener(e->buySelected());
        offers.setSelectedIndex(0);refresh();
    }
    public boolean buySelected(){
        Offer offer=offers.getSelectedValue();
        if(offer==null)return false;
        try{
            boolean bought=offer.equipment()!=null?profile.purchaseEquipment(offer.equipment()):
                profile.purchase(offer.kind(),offer.tier());
            refresh();afterPurchase.run();
            return bought;
        }catch(IOException error){
            details.setText("Purchase could not be saved.\n"+error.getMessage());
            return false;
        }
    }
    public void refresh(){
        Offer offer=offers.getSelectedValue();
        balance.setText("CASTLE FUNDS  £"+profile.currency());
        if(offer==null){purchase.setEnabled(false);details.setText("Select supplies.");return;}
        boolean owned=offer.equipment()!=null&&profile.ownsEquipment(offer.equipment());
        int stock=offer.kind()==null?0:profile.supplyCount(offer.kind(),offer.tier());
        boolean full=offer.kind()!=null&&stock>=4;
        purchase.setEnabled(!owned&&!full&&profile.currency()>=offer.cost());
        details.setText(offer.title()+"\n\nCOST  £"+offer.cost()+
            "\n"+(offer.equipment()!=null?
                (owned?"OWNED  /  available for loadout":"New permanent gear  /  "+offer.equipment().description):
                "IN STOCK  "+stock+" / 4  /  "+offer.tier().restore(offer.kind())+
                (offer.kind()==FieldSupplies.Kind.HEALTH?" health restored":" sanity restored"))+
            "\n\n"+
            (owned?"Already owned.":full?"Stock limit reached.":
                profile.currency()<offer.cost()?"Insufficient funds.":
                "Purchase is charged to the persistent Castle account."));
    }
}
