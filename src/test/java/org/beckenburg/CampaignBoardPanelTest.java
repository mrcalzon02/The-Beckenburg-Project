package org.beckenburg;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.*;

/** Contract list never dispatches fictional sites as playable geometry. */
public final class CampaignBoardPanelTest {
    private static void ok(boolean v,String reason){if(!v)throw new AssertionError(reason);}
    private static <T extends java.awt.Component> T named(java.awt.Container root,String name,Class<T> type){
        for(java.awt.Component c:root.getComponents()){
            if(type.isInstance(c)&&name.equals(c.getName()))return type.cast(c);
            if(c instanceof java.awt.Container child){
                T found=named(child,name,type);if(found!=null)return found;
            }
        }return null;
    }
    public static void main(String[] args)throws Exception{
        Path temp=Files.createTempDirectory("beckenburg-campaign-board").resolve("career.properties");
        var profile=new ProfileStore(temp);
        profile.addNetworkPayout(UUID.randomUUID(),ProfileStore.ASSISTANT_HIRE_COST+70,0);
        SwingUtilities.invokeAndWait(()->{
            var data=CampaignBoardPanel.catalog();
            ok(data.size()==22,"nine base families plus thirteen remote concepts");
            ok(data.stream().map(CampaignBoardPanel.Entry::name).distinct().count()==data.size(),"unique names");
            ok(data.stream().filter(CampaignBoardPanel.Entry::playable).count()>=1,"real map exists");
            ok(data.stream().filter(x->x.map()==null).noneMatch(CampaignBoardPanel.Entry::playable),"remote concepts locked");
            AtomicInteger launches=new AtomicInteger();
            java.util.concurrent.atomic.AtomicBoolean deployedAssistant=new java.util.concurrent.atomic.AtomicBoolean();
            CampaignBoardPanel board=new CampaignBoardPanel(profile,
                new CampaignBoardPanel.Actions(){
                    @Override public void deploy(SiteCatalog.Location map,boolean withAssistant){
                        ok(map.playable(),"only authorized geometry");
                        deployedAssistant.set(withAssistant);
                        launches.incrementAndGet();}
                    @Override public void castle(){}
                    @Override public void tutorial(){}
                });
            JList<?> list=named(board,"campaign.contracts",JList.class);
            JButton deploy=named(board,"campaign.deploy",JButton.class);
            JButton recruit=named(board,"campaign.recruit",JButton.class);
            JCheckBox assistant=named(board,"campaign.assistant",JCheckBox.class);
            JButton viewShop=named(board,"campaign.view.store",JButton.class);
            JButton viewArchive=named(board,"campaign.view.archive",JButton.class);
            JButton viewContracts=named(board,"campaign.view.contracts",JButton.class);
            CampaignStorePanel store=named(board,"campaign.store",CampaignStorePanel.class);
            CampaignArchivePanel records=named(board,"campaign.archive",CampaignArchivePanel.class);
            ok(list!=null&&deploy!=null&&recruit!=null&&assistant!=null
                &&viewShop!=null&&viewArchive!=null&&viewContracts!=null
                &&store!=null&&records!=null,
                "The actual campaign board can reach its store and case archives");
            viewShop.doClick();
            ok(store.isVisible()&&!records.isVisible(),
                "Quartermaster is a compact center page, not an extra window");
            viewArchive.doClick();
            ok(records.isVisible()&&!store.isVisible(),
                "Case archives and dossiers use the same navigation area");
            viewContracts.doClick();
            ok(!store.isVisible()&&!records.isVisible(),
                "Player returns directly to playable contracts");
            ok(recruit.isEnabled()&&!assistant.isEnabled(),
                "Owned currency funds a real recruit, not an already-deployed ally");
            named(board,"campaign.hires",JSpinner.class).setValue(1);
            recruit.doClick();
            ok(profile.missionHires()==1&&assistant.isEnabled()&&assistant.isSelected(),
                "Recruit transaction must enable the actual AI slot");
            list.setSelectedIndex(1);
            ok(!deploy.isEnabled(),
                "Built School is still locked until House has been successfully cleared");
            list.setSelectedIndex(9);ok(!deploy.isEnabled(),"Blackpine planned, no deploy");
            deploy.doClick();ok(launches.get()==0,"disabled dispatch blocked");
            list.setSelectedIndex(0);ok(deploy.isEnabled(),"house can deploy");
            deploy.doClick();
            ok(launches.get()==1&&deployedAssistant.get(),
                "Real playable map dispatch includes the hired companion");
        });
        Files.deleteIfExists(temp);Files.deleteIfExists(temp.getParent());
        System.out.println("PASS campaign board has playable contracts, persistent recruit and actual companion dispatch");
    }
}
