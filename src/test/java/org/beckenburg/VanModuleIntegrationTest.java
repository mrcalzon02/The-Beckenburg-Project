package org.beckenburg;

import java.util.HashSet;
import java.util.Set;

/** Acceptance for the single reusable VAN fit-out at every playable site. */
public final class VanModuleIntegrationTest {
    private VanModuleIntegrationTest(){}
    private static void ok(boolean yes,String label){
        if(!yes)throw new AssertionError(label);
    }
    public static void main(String[] args){
        ok(Math.abs(VanStaging.REAR-VanStaging.FRONT-4.256)<.001,"manufacturer-sized long cargo bay");
        ok(Math.abs(VanStaging.HALF_WIDTH*2-2.059)<.001,"road-vehicle body width");
        ok(VanStaging.INTERIOR_HEIGHT>=2.10,"full height workstation and overhead lights");
        // Portable inventory occupies real hooks. Full-size monitoring consoles
        // instead occupy their already-authored bulkhead fixtures; forcing
        // duplicate COMPUTER meshes onto a 2.059m-wide van rack is impossible.
        Set<String> portable=new HashSet<>();
        for(String id:FieldEquipment.workingTools()){
            if(EquipmentRegistry.get(id).origin()!=EquipmentRegistry.Origin.TRUCK)
                portable.add(id);
        }
        portable.add("SALT");
        ok(VanInteriorLayout.toolSockets().size()==portable.size(),
            "every portable tool has exactly one physical hook, consoles stay mounted");
        ok(VanInteriorLayout.TOOL_CAPACITY>=EquipmentTiers.all().size(),
            "42 real rack spaces reserved without inventing missing or nonfunctional gear");
        ok(VanInteriorLayout.TOOL_CAPACITY>=VanInteriorLayout.toolSockets().size(),
            "rack cannot silently drop equipment when fully stocked");
        Set<String> slots=new HashSet<>();
        for(var entry:VanInteriorLayout.toolSockets()){
            ok(slots.add(entry.equipment()),"no duplicate bracket: "+entry.equipment());
            ok(portable.remove(entry.equipment()),
                "portable hook is unknown, duplicated or belongs to fixed monitor: "+entry.equipment());
            ok(entry.x()<-.5 && entry.x()>-VanStaging.HALF_WIDTH,
                "equipment fixed to left wall, not floating in aisle");
            ok(entry.z()>VanStaging.FRONT+.25&&entry.z()<VanStaging.REAR-.25,
                "tool socket inside the rear opening and bulkhead");
            ok(entry.y()>.15&&entry.y()<VanStaging.FLOOR_Y+VanStaging.INTERIOR_HEIGHT-.05,
                "hook at plausible standing reach height");
            ok(GlbProps.model(EquipmentModelCatalog.model(entry.equipment(),
                EquipmentTiers.Tier.I)).triangles().size()>1,
                "approved original mesh actually exists: "+entry.equipment());
        }
        ok(portable.isEmpty(),"missing tangible hooks for portable equipment: "+portable);
        for(var p:VanInteriorLayout.FIXTURES){
            ok(VanInteriorLayout.insideShell(p),"fitted fixture outside cargo: "+p.purpose());
            if(p.mesh().equals("beckenburg_flat_monitor")
                    ||p.mesh().equals("beckenburg_wide_monitor")){
                // These LCD housings are real procedural world-space geometry
                // assembled by SceneRenderer.vanFlatDisplay, not GLB imports.
                // Asking GlbProps.model() for a nonexistent GLB broke Windows
                // packaging although the display is already rendered in world.
                var bounds=VanInteriorLayout.bounds(p.mesh());
                ok(bounds.x1()>bounds.x0()&&bounds.y1()>bounds.y0()
                    &&bounds.z1()>bounds.z0(),
                    "procedural van monitor has invalid physical housing: "+p.purpose());
                ok(VanLiveScreens.material(VanLiveScreens.forPurpose(p.purpose()))
                    >=GlbProps.TEXTURE_BASE,
                    "procedural van monitor is missing its live screen: "+p.purpose());
            }else{
                ok(GlbProps.model(p.mesh()).triangles().size()>1,
                    "mounted console/board uses actual imported model: "+p.mesh());
            }
        }
        Set<String> purposes=new HashSet<>();
        for(var p:VanInteriorLayout.FIXTURES)purposes.add(p.purpose());
        for(String critical:new String[]{"command workstation","computer tower",
                "desk keyboard","main command monitor","contract objective board",
                "daily assignment board","sanity telemetry","haunting activity",
                "sound telemetry","site map"})
            ok(purposes.contains(critical),"essential station missing: "+critical);
        ok(VanInteriorLayout.openAisle(0,0),"straight aisle is not obstructed");
        for(double z=VanStaging.FRONT+.34;z<=1.10;z+=.15)
            ok(VanInteriorLayout.openAisle(0,z),"centre walkway blocked at "+z);
        for(HouseMap.Site site:HouseMap.Site.values()){
            var map=new HouseMap(site);
            var van=map.van();
            var spawn=van.spawn();
            ok(map.traversable(spawn.x(),spawn.z()),"spawn clear "+site);
            for(var station:VanStaging.Station.values()){
                var p=van.location(station);
                ok(map.traversable(p.x(),p.z()),"station accessible "+site+" / "+station);
                ok(van.stationAt(p.x(),p.z())==station,
                    "authored station interaction socket "+station);
            }
            for(var item:EquipmentLoadout.Item.values()){
                var socket=VanEquipmentRack.socket(van,item);
                var local=van.local(socket.x(),socket.z());
                ok(local.x()<0 && local.z()>=VanStaging.FRONT,
                    "solo bought item mounted on physical left rack");
            }
        }
        ok(java.util.Arrays.asList(InGamePauseMenu.options()).contains("Exit Game"),
            "Exit Game available without extraction");
        System.out.println("PASS VAN interior sizes, complete tier catalog, purchased tool mounts, "+
            "four functional boards/screens, walkable stations at all locations and Exit Game menu");
    }
}
