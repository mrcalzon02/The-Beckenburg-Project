package org.beckenburg;

import javax.swing.*;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicInteger;

/** One catalog, two persistently separate ownership scopes and physical access routes. */
public final class CastleInventoryWorkstationTest {
    private static void check(boolean result,String label){
        if(!result)throw new AssertionError(label);
    }
    public static void main(String[] args)throws Exception{
        ProfileStore profile=new ProfileStore(
            Files.createTempDirectory("castle-inventory-routing").resolve("campaign.properties"));
        AtomicInteger multiplayerInventory=new AtomicInteger(), genericNetwork=new AtomicInteger();
        Throwable[] failure={null};
        SwingUtilities.invokeAndWait(()->{
            CastleHubPanel castle=null;
            try(LanLobbyPanel lobby=new LanLobbyPanel()){
                castle=new CastleHubPanel(profile,new CastleHubPanel.Actions(){
                    public void begin(){}public void network(){genericNetwork.incrementAndGet();}
                    public void multiplayerInventory(){multiplayerInventory.incrementAndGet();}
                    public void spectral(){}public void menu(){}public void campaign(){}
                    public void cycleRole(){}public void cycleWeather(){}
                    public String role(){return "OBSERVER";}
                    public String weather(){return "Normal";}
                });
                check(!castle.quartermasterOpen(),"Castle store starts closed");
                castle.openWarehouseInventory();
                check(multiplayerInventory.get()==1&&genericNetwork.get()==0,
                    "Warehouse selects dedicated multiplayer inventory route, not general network or campaign");
                check(!castle.quartermasterOpen(),"Warehouse does not open campaign Quartermaster");
                lobby.focusEquipmentInventory();
                JTabbedPane tabs=CampaignMenuWorkflowTest.named(lobby,"network.lobby.tabs",JTabbedPane.class);
                NetworkEquipmentPanel network=CampaignMenuWorkflowTest.named(lobby,"network.equipment",NetworkEquipmentPanel.class);
                check(tabs!=null&&network!=null&&tabs.getSelectedComponent()==network,
                    "Multiplayer route focuses the actual host-authoritative equipment tab");
                JButton networkBuy=CampaignMenuWorkflowTest.named(network,"equipment.catalog.buy",JButton.class);
                check(networkBuy!=null&&!networkBuy.isEnabled(),
                    "Unconnected multiplayer workstation cannot charge local campaign wallet");

                castle.openQuartermaster();
                check(castle.quartermasterOpen(),"Campaign Quartermaster opens within Castle");
                CampaignStorePanel campaign=CampaignMenuWorkflowTest.named(castle,"campaign.store",CampaignStorePanel.class);
                check(campaign!=null,"Rear supply wardrobe uses campaign-backed catalog");
                JList<?> soloItems=CampaignMenuWorkflowTest.named(campaign,"equipment.catalog.items",JList.class);
                JList<?> multiItems=CampaignMenuWorkflowTest.named(network,"equipment.catalog.items",JList.class);
                check(soloItems!=null&&multiItems!=null
                    &&soloItems.getModel().getSize()==multiItems.getModel().getSize(),
                    "Both workstations expose the identical item-tier catalog");
                for(int i=0;i<soloItems.getModel().getSize();i++)
                    check(soloItems.getModel().getElementAt(i).equals(multiItems.getModel().getElementAt(i)),
                        "Item tiers and order must not differ between inventories");
                check(profile.currency()==0,"Opening multiplayer catalog never changes campaign balance");
            }catch(Throwable error){failure[0]=error;}
            finally{if(castle!=null)castle.close();}
        });
        if(failure[0]!=null)throw new AssertionError(failure[0]);
        System.out.println("PASS Castle warehouse multiplayer scope, campaign Quartermaster and common catalog");
    }
}
