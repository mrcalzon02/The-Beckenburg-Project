package org.beckenburg;
/** Campaign funds and stock use the same equipment library as multiplayer. */
public final class CampaignStorePanel extends EquipmentCatalogPanel {
    public CampaignStorePanel(ProfileStore profile,Runnable afterPurchase){super(profile,afterPurchase);setName("campaign.store");}
}
