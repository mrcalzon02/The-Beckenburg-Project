package org.beckenburg;

import javax.swing.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Persistent settlement, character growth, bounded case archive and real Castle shop. */
public final class CampaignProgressionAndStoreTest {
    private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    private static <T extends java.awt.Component> T named(java.awt.Container root,
            String name,Class<T> type){
        for(var c:root.getComponents()){
            if(type.isInstance(c)&&name.equals(c.getName()))return type.cast(c);
            if(c instanceof java.awt.Container nested){
                T found=named(nested,name,type);if(found!=null)return found;
            }
        }
        return null;
    }
    private static int index(String id,FieldSupplies.Kind kind){
        var list=EquipmentCatalogPanel.catalog();
        for(int i=0;i<list.size();i++)if(list.get(i).id().equals(id)&&list.get(i).tier()==EquipmentTiers.Tier.I)return i;
        throw new AssertionError("Catalog item missing: "+id);
    }
    public static void main(String[] args)throws Exception{
        Path folder=Files.createTempDirectory("beckenburg-depot-archive");
        Path file=folder.resolve("campaign.properties");
        try{
            var profile=new ProfileStore(file);
            check(profile.level(InvestigationSession.Role.OBSERVER)==1,
                "New Hunter begins at level one");
            check(HunterProgression.threshold(2)==120&&HunterProgression.level(120)==2,
                "Progression thresholds are deterministic and attainable");
            check(HunterProgression.maxHealth(2)==102
                &&HunterProgression.extraWards(6)==1,
                "Levels grant actual field capacity and eventual ward upgrades");
            profile.addNetworkPayout(UUID.randomUUID(),6000,0);
            AtomicInteger changes=new AtomicInteger();
            SwingUtilities.invokeAndWait(()->{
                CampaignStorePanel store=new CampaignStorePanel(profile,changes::incrementAndGet);
                JList<?> goods=named(store,"equipment.catalog.items",JList.class);
                JButton buy=named(store,"equipment.catalog.buy",JButton.class);
                check(goods!=null&&buy!=null&&CampaignStorePanel.catalog().size()==129,
                    "Single compact quartermaster has permanent and consumable purchases");
                goods.setSelectedIndex(index("UV_LIGHT",null));
                check(buy.isEnabled(),"Unlocked permanent upgrade available");
                buy.doClick();
                check(profile.ownsEquipment(EquipmentLoadout.Item.UV_LAMP),
                    "Quartermaster grants actual persistent UV ownership");
                check(!buy.isEnabled()&&!store.buySelected(),"Permanent gear cannot be purchased twice");
                goods.setSelectedIndex(index("HEALTH_SUPPLIES",FieldSupplies.Kind.HEALTH));buy.doClick();
                check(profile.supplyCount(FieldSupplies.Kind.HEALTH,FieldSupplies.Tier.BASIC)==1,
                    "Quartermaster sells real finite health supplies");
                check(changes.get()==2,"Account UI is refreshed exactly once per successful purchase");
            });
            check(profile.currency()==6000-ProfileStore.equipmentPrice(
                EquipmentLoadout.Item.UV_LAMP)-FieldSupplies.Tier.BASIC.price,
                "All purchases debit one shared Castle currency account");
            // One unit of real persisted Castle medicine stock can be sold back.
            SwingUtilities.invokeAndWait(()->{
                CampaignStorePanel store=new CampaignStorePanel(profile,changes::incrementAndGet);
                JList<?> goods=named(store,"equipment.catalog.items",JList.class);
                JButton sell=named(store,"equipment.catalog.sell",JButton.class);
                check(sell!=null&&!sell.isEnabled(),
                    "Never resell permanent equipment or starter gear");
                goods.setSelectedIndex(index("HEALTH_SUPPLIES",FieldSupplies.Kind.HEALTH));
                check(sell.isEnabled(),"Only actual positive medicine stock can be sold");
                sell.doClick();
                check(profile.supplyCount(FieldSupplies.Kind.HEALTH,FieldSupplies.Tier.BASIC)==0,
                    "One resale actually removes one stock unit");
                check(!sell.isEnabled()&&!store.sellSelected(),
                    "An empty stock row cannot be sold again for free credits");
            });
            check(profile.currency()==6000-ProfileStore.equipmentPrice(
                    EquipmentLoadout.Item.UV_LAMP)-FieldSupplies.Tier.BASIC.price/2,
                "Resale refunds only half the price and never duplicates funds");
            ProfileStore stockReload=new ProfileStore(file);stockReload.load();
            check(stockReload.currency()==profile.currency()&&
                    stockReload.supplyCount(FieldSupplies.Kind.HEALTH,FieldSupplies.Tier.BASIC)==0,
                "Stock resale and its exact refund survive a profile reload");
            var map=new HouseMap(HouseMap.Site.HOUSE);
            for(int i=0;i<7;i++){
                var caseSession=new InvestigationSession(map,InvestigationSession.Role.OBSERVER,911+i,
                    profile.level(InvestigationSession.Role.OBSERVER));
                var extraction=map.van().spawn(0);
                check(caseSession.extract(extraction.x(),VanStaging.FLOOR_Y,extraction.z()),
                    "Case must physically return to the extraction van");
                int original=profile.currency();
                check(profile.addPayout(caseSession,SiteCatalog.Location.HOUSE),"Unique case settled");
                check(profile.currency()==original+caseSession.payout(),
                    "Actual round earnings reach the account");
                check(!profile.addPayout(caseSession,SiteCatalog.Location.HOUSE),
                    "Duplicate settlement cannot duplicate credits, XP or archive records");
            }
            check(profile.level(InvestigationSession.Role.OBSERVER)==2
                &&profile.level(InvestigationSession.Role.CLERIC)==1,
                "Class XP advances only the Hunter who actually worked the case");
            check(profile.disciplineExperience(InvestigationSession.Role.OBSERVER)==140,
                "Seven legitimate partial cases award the correct non-victory XP");
            check(profile.visits(SiteCatalog.Location.HOUSE)==7
                &&profile.clears(SiteCatalog.Location.HOUSE)==0
                &&profile.archives().size()==7,
                "Each actual retreat is filed as visited, not a cleared haunting");
            var archived=profile.archives().get(6);
            check(archived.startLevel()==2&&archived.finalLevel()==2
                &&archived.fullReport().contains("FIELD JOURNAL")
                &&archived.credits()>=0,
                "End of round archives preserve class, level, income, status and real journal");
            var stronger=new InvestigationSession(map,InvestigationSession.Role.OBSERVER,88,
                profile.level(InvestigationSession.Role.OBSERVER));
            check(stronger.health()==102&&stronger.maxHealth()==102,
                "Level up actually affects the next playable Hunter mission");
            var veteran=new InvestigationSession(map,InvestigationSession.Role.CLERIC,88,6);
            check(veteran.wards()==4&&veteran.health()==110,
                "Veteran Cleric receives exactly one real additional defensive ward");
            ProfileStore recovered=new ProfileStore(file);recovered.load();
            check(recovered.level(InvestigationSession.Role.OBSERVER)==2
                &&recovered.ownsEquipment(EquipmentLoadout.Item.UV_LAMP)
                &&recovered.archives().size()==7&&recovered.visits(SiteCatalog.Location.HOUSE)==7,
                "Level, purchased gear, case rewards and archives survive real reload");
            SwingUtilities.invokeAndWait(()->{
                CampaignArchivePanel archive=new CampaignArchivePanel(recovered);
                JList<?> records=named(archive,"campaign.archive.list",JList.class);
                JTextArea report=named(archive,"campaign.archive.report",JTextArea.class);
                check(records!=null&&records.getModel().getSize()==7&&report!=null,
                    "Castle case archive displays all settled dossier records");
                check(report.getText().contains("COMPENSATION"),
                    "Selected dossier shows earnings and actual outcome");
            });
            for(int i=7;i<35;i++){
                var round=new InvestigationSession(map,InvestigationSession.Role.CLERIC,500+i);
                var van=map.van().spawn(0);
                round.extract(van.x(),VanStaging.FLOOR_Y,van.z());
                recovered.addPayout(round,SiteCatalog.Location.HOUSE);
            }
            check(recovered.archives().size()==CampaignArchive.LIMIT,
                "History is bounded but does not overflow the profile");
            var finalLoad=new ProfileStore(file);finalLoad.load();
            check(finalLoad.archives().size()==CampaignArchive.LIMIT
                &&finalLoad.archives().get(0).discipline()==InvestigationSession.Role.OBSERVER,
                "Oldest archive rolls off without damaging newer records");
            System.out.println("PASS campaign: unified quartermaster, role levels, field perks, real payout and 32 persisted reports");
        }finally{Files.deleteIfExists(file);Files.deleteIfExists(folder);}
    }
}
