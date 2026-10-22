package org.beckenburg;

/** Focused regressions from the 2026-10-22 player-reported Castle Beckenburg defects. */
public final class CastleLiveRepairTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void main(String[] args){
        CastleHubMap map=new CastleHubMap();
        check(map.valid(),"Castle navigation retains all reachable stations");
        check(CastleHubArchitecturalPlan.roofedAt(5,20),"west castle wing roofed");
        check(CastleHubArchitecturalPlan.roofedAt(23,20),"operations wing roofed");
        check(CastleHubArchitecturalPlan.roofedAt(14,5),"upper library roofed");
        check(!CastleHubArchitecturalPlan.roofedAt(13.5,19.3),
            "open castle courtyard must remain under sky");
        check(CastleHubArchitecturalPlan.roofs().size()>=4,
            "roof must be authored by wing rather than decorative fragments");
        check(CastleHubArchitecturalPlan.groundCeilingHeight(14.5,5.5)
                <CastleHubArchitecturalPlan.GROUND_CEILING,
            "Upper library deck must not z-fight downstairs ceiling");
        check(CastleHubArchitecturalPlan.groundCeilingHeight(23,5.5)
                ==CastleHubArchitecturalPlan.GROUND_CEILING,
            "Rooms without an upstairs deck retain original ceiling height");
        double stair=CastleHubArchitecturalPlan.stairHeight(17.5,27);
        check(stair>.1&&map.traversable(17.5,27,stair),
            "Stair walking surface remains traversable");
        check(!map.traversable(16.05,27,stair),
            "Stair side must have collision");
        check(!map.traversable(16.04,15,3.4),
            "Raised gallery side must have player-radius collision");
        check(map.traversable(17.5,15,3.4),
            "Raised gallery center stays navigable");
        check(map.traversable(17.5,22.0,3.4),
            "Stair landing remains traversable when adjoining upper gallery");
        check(map.traversable(17.5,21.9,3.4),
            "Gallery transition retains clearance after stair ascent");
        check(CastleHubMap.Station.ARMORY_UV.label.contains("QUARTERMASTER")
                && CastleHubMap.Station.ARMORY_SALT.label.contains("QUARTERMASTER"),
            "Castle armoury stations must advertise accessible full shop");
        check(CampaignStorePanel.catalog().stream().anyMatch(
                offer->offer.equipment()==EquipmentLoadout.Item.UV_LAMP),
            "Full shop includes purchasable UV equipment");
        check(CampaignStorePanel.catalog().stream().anyMatch(
                offer->offer.equipment()==EquipmentLoadout.Item.SALT),
            "Full shop includes purchasable salt");
        check(CampaignStorePanel.catalog().stream().anyMatch(
                offer->offer.kind()!=null),
            "Full shop includes consumable supplies");
        for(var item:CastleHubMap.furnishings())
            check(Double.isFinite(item.x())&&Double.isFinite(item.z()),
                "Furnishing has valid placement");
        System.out.println("PASS castle live repairs: deck separation, stair/rail collision, quartermaster");
    }
}
