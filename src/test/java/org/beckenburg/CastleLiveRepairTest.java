package org.beckenburg;

/** Focused regressions from the 2026-10-22 player-reported Castle Beckenburg defects. */
public final class CastleLiveRepairTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void main(String[] args){
        CastleHubMap map=new CastleHubMap();
        check(map.valid(),"Castle navigation retains all reachable stations");
        var parked=CastleSurroundings.parkedVehicles();
        check(parked.size()==3,"One decorative van and two PSX parked cars");
        check(parked.stream().filter(v->v.mesh().equals("ggbot_car08_van_shell")).count()==1,
            "Castle must display one non-operational van");
        check(parked.stream().filter(v->v.mesh().equals("ggbot_car01")||
                v.mesh().equals("ggbot_car05")).count()==2,
            "Castle road must display two original PSX car models");
        for(var v:parked){
            if(v.mesh().equals("ggbot_car08_van_shell")){
                check(v.z()>18&&v.z()<33&&v.x()>10&&v.x()<20,
                    "Project van must visibly occupy the actual central courtyard");
                check(!map.traversable(v.x(),v.z()),"Parked van must block walk-through");
            }else{
                check(v.z()>34.4,"Two PSX cars remain on exterior access road");
                check(!map.traversable(v.x(),v.z()),"Roadside cars are not player stations");
            }
            var model=GlbProps.model(v.mesh());
            check(model.triangles().size()>100,"Source-backed PSX model "+v.mesh());
            check(model.triangles().stream().anyMatch(t->
                t.material()>=GlbProps.TEXTURE_BASE),
                "Original texture atlas required for "+v.mesh());
        }
        for(String psxCar:new String[]{"ggbot_car01","ggbot_car05"}){
            var model=GlbProps.model(psxCar);
            java.util.Set<Integer> samples=new java.util.HashSet<>();
            for(var tri:model.triangles()){
                if(tri.material()<GlbProps.TEXTURE_BASE)continue;
                double u=(tri.a().u()+tri.b().u()+tri.c().u())/3;
                double v=(tri.a().v()+tri.b().v()+tri.c().v())/3;
                samples.add(GlbProps.sample(tri.material(),u,v));
            }
            check(samples.size()>12,
                "Actual varied PSX atlas must replace dull placeholder: "+psxCar);
        }
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
                offer->offer.id().equals("UV_LIGHT")),
            "Full shop includes purchasable UV equipment");
        check(CampaignStorePanel.catalog().stream().anyMatch(
                offer->offer.id().equals("SALT")),
            "Full shop includes purchasable salt");
        check(CampaignStorePanel.catalog().stream().anyMatch(
                offer->offer.medicine()!=null),
            "Full shop includes consumable supplies");
        for(var item:CastleHubMap.furnishings())
            check(Double.isFinite(item.x())&&Double.isFinite(item.z()),
                "Furnishing has valid placement");
        System.out.println("PASS castle live repairs: deck separation, stair/rail collision, quartermaster");
    }
}
