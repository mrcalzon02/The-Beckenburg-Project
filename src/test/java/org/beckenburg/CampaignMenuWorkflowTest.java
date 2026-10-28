package org.beckenburg;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.*;

/** Uses real saved transactions and production panels, including minimum-window captures. */
public final class CampaignMenuWorkflowTest {
    static void ok(boolean value,String why){if(!value)throw new AssertionError(why);}
    static <T extends Component>T named(Container root,String name,Class<T> type){
        if(type.isInstance(root)&&name.equals(root.getName()))return type.cast(root);
        for(Component c:root.getComponents()){
            if(type.isInstance(c)&&name.equals(c.getName()))return type.cast(c);
            if(c instanceof Container container){T result=named(container,name,type);if(result!=null)return result;}
        }return null;
    }
    static void layout(Container root){root.doLayout();for(Component c:root.getComponents())if(c instanceof Container nested)layout(nested);}
    static void capture(JComponent panel,String name)throws Exception{
        for(int[] size:new int[][]{{1060,725},{760,510}}){
            panel.setSize(size[0],size[1]);layout(panel);
            BufferedImage picture=new BufferedImage(size[0],size[1],BufferedImage.TYPE_INT_RGB);
            var g=picture.createGraphics();panel.paint(g);g.dispose();
            Path out=Path.of("build/menu-review/captures/"+name+"-"+size[0]+".png");
            Files.createDirectories(out.getParent());ImageIO.write(picture,"png",out.toFile());
        }
    }
    public static void main(String[] args)throws Exception{
        Path folder=Files.createTempDirectory("campaign-menu");var profile=new ProfileStore(folder.resolve("campaign.properties"));
        profile.addNetworkPayout(UUID.randomUUID(),3000,0);
        ok(profile.reserveMissionHunters(5),"Five hire reservations are purchasable");
        ok(profile.currency()==2100&&profile.missionHires()==5,"Charge five actual hires");
        profile.consumeMissionHunters(3);
        ok(profile.currency()==2460&&profile.missionHires()==0,"Two human replacements refund unused hires");
        profile.load();ok(profile.currency()==2460&&profile.missionHires()==0,"Hires consumed durably");
        int beforeGear=profile.currency();
        ok(!profile.purchaseCatalogGear("DOTS_PROJECTOR",EquipmentTiers.Tier.III),"Cannot skip two equipment tiers");
        for(var tier:EquipmentTiers.Tier.values())ok(profile.purchaseCatalogGear("DOTS_PROJECTOR",tier),"Buy actual catalog tier "+tier);
        int fullPrice=0;for(var tier:EquipmentTiers.Tier.values())fullPrice+=EquipmentTiers.stage("DOTS_PROJECTOR",tier).creditCost();
        ok(profile.currency()==beforeGear-fullPrice,"Every tier uses the shared canonical price");
        ok(!profile.purchaseCatalogGear("DOTS_PROJECTOR",EquipmentTiers.Tier.III),"Cannot duplicate a tier purchase");
        profile.equipCatalog(2,"DOTS_PROJECTOR");profile.load();
        ok(profile.catalogEquipment().get("DOTS_PROJECTOR")==EquipmentTiers.Tier.III&&profile.catalogLoadout()[2].equals("DOTS_PROJECTOR"),"Full campaign tier and loadout survive reload");
        FieldEquipment issuedCampaign=new FieldEquipment();issuedCampaign.configureCampaign(profile);
        ok(issuedCampaign.owned("DOTS_PROJECTOR")==EquipmentTiers.Tier.III&&issuedCampaign.at(2).equals("DOTS_PROJECTOR"),"Actual campaign hardware receives saved tier and prepared slot");
        profile.addNetworkPayout(UUID.randomUUID(),3000,0);
        try{profile.reserveMissionHunters(6);throw new AssertionError("Six additional hires accepted");}catch(IllegalArgumentException expected){}
        ProfileStore poor=new ProfileStore(folder.resolve("poor.properties"));
        ok(!poor.reserveMissionHunters(1)&&poor.currency()==0,"No negative campaign balance");
        // Atomic save failure restores both booking and funds.
        Path blocked=folder.resolve("blocked");Files.createDirectory(blocked);
        var broken=new ProfileStore(blocked);boolean refused=false;
        try{broken.reserveMissionHunters(0);}catch(java.io.IOException expected){refused=true;}
        ok(refused&&broken.missionHires()==0&&broken.currency()==0,"Failed save restores reservation state");
        Throwable[] fail={null};
        SwingUtilities.invokeAndWait(()->{try{
            int[] dispatched={-1};boolean[] coop={false};
            CampaignBoardPanel board=new CampaignBoardPanel(profile,new CampaignBoardPanel.Actions(){
                public void deploy(SiteCatalog.Location map,boolean ally){}
                public void deployTeam(SiteCatalog.Location map,int hires,boolean network){dispatched[0]=hires;coop[0]=network;}
                public void castle(){}public void tutorial(){}
            });
            capture(board,"campaign-contracts");
            named(board,"campaign.view.team",JButton.class).doClick();
            named(board,"campaign.hires",JSpinner.class).setValue(5);
            named(board,"campaign.recruit",JButton.class).doClick();
            ok(profile.missionHires()==5,"Real team booking from campaign menu");
            named(board,"campaign.mode",JComboBox.class).setSelectedIndex(1);
            capture(board,"campaign-team");
            named(board,"campaign.deploy",JButton.class).doClick();
            ok(dispatched[0]==5&&coop[0],"Co-op dispatch carries requested hires");
            named(board,"campaign.view.store",JButton.class).doClick();capture(board,"campaign-store");
            NetworkEquipmentPanel network=new NetworkEquipmentPanel(x->{},(k,t)->{});
            FieldEquipment issued=new FieldEquipment();network.receive(new FieldSupplies().frame());
            network.receive(issued.frame(400));for(String frame:issued.inventoryFrames())network.receive(frame);
            JList<?> campaignCatalog=named(board,"equipment.catalog.items",JList.class);
            JList<?> multiplayerCatalog=named(network,"equipment.catalog.items",JList.class);
            ok(campaignCatalog.getModel().getSize()==129&&multiplayerCatalog.getModel().getSize()==129,"Both modes expose the same 129 item-tier entries");
            for(int i=0;i<129;i++)ok(campaignCatalog.getModel().getElementAt(i).equals(multiplayerCatalog.getModel().getElementAt(i)),"Same catalog entry in both modes");
            capture(network,"multiplayer-equipment");
            try(LanLobbyPanel lobby=new LanLobbyPanel()){capture(lobby,"multiplayer-lobby");}
        }catch(Throwable error){fail[0]=error;}});
        if(fail[0]!=null)throw new AssertionError(fail[0]);
        System.out.println("PASS mission reservations, refunds, capacity, save rollback, campaign dispatch and real panel captures");
    }
}
