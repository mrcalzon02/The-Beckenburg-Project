package org.beckenburg;

import java.nio.file.*;
import java.util.*;

/** Only maps with real navigation and render registries may be offered to solo players. */
public final class SiteSelectionTest {
    private static void ok(boolean b,String why){if(!b)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        // Never read or select the developer's own authored project in a test.
        String previousHome=System.getProperty("user.home");
        Path isolatedHome=Files.createTempDirectory("beckenburg-site-selection-");
        System.setProperty("user.home",isolatedHome.toString());
        try{checkCatalog();}
        finally{
            System.setProperty("user.home",previousHome);
            // Rendering can create preferences in this disposable home. Remove
            // that owned tree instead of masking a catalogue failure with
            // DirectoryNotEmptyException from deleting only its root.
            try(var paths=Files.walk(isolatedHome)){
                for(Path path:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);
            }
        }
    }
    private static SceneRenderer rendererFor(HouseMap map){
        // Renderer defaults are House coordinates, not a valid camera on every
        // authored site. Use a real investigation-room spawn and look toward
        // that room's nearest non-stair opening. Open-air sites may use room 0.
        var room=map.level().rooms().stream()
            .filter(r->!r.outside()&&r.id()!=HouseMap.EXTRACTION_ROOM)
            .findFirst().orElse(map.level().room(0));
        var feet=room.spawn();
        ok(map.roomAt(feet.x(),feet.y(),feet.z())==room.id()
            &&map.traversable(feet.x(),feet.y(),feet.z(),HunterPosture.STANDING_HEIGHT),
            "render camera must use a supported, correctly owned site spawn: "+map.site()+" / "+room.name());
        var opening=map.level().portalsFrom(room.id()).stream()
            .filter(p->!p.kind().equals("stair")&&Math.hypot(p.x()-feet.x(),p.z()-feet.z())>.5)
            .min(Comparator.comparingDouble(p->Math.hypot(p.x()-feet.x(),p.z()-feet.z())))
            .orElse(null);
        // A room without a suitable opening still has its own authored bounds;
        // face its far corner rather than inheriting the House's world yaw.
        double aimX=opening!=null?opening.x():
            (feet.x()-room.x0()>room.x1()-feet.x()?room.x0():room.x1());
        double aimZ=opening!=null?opening.z():
            (feet.z()-room.z0()>room.z1()-feet.z()?room.z0():room.z1());
        double aimY=(opening!=null?opening.y():room.y())+1.2;
        double distance=Math.hypot(aimX-feet.x(),aimZ-feet.z());
        ok(distance>.5&&Double.isFinite(distance),"render view needs a distinct authored target: "+map.site());
        var renderer=new SceneRenderer();
        renderer.x=feet.x();renderer.z=feet.z();
        renderer.playerFeetElevation=feet.y();renderer.eyeHeight=feet.y()+HunterPosture.STANDING_EYE;
        renderer.yaw=Math.atan2(aimX-feet.x(),aimZ-feet.z());
        renderer.pitch=Math.atan2(aimY-renderer.eyeHeight,distance);
        return renderer;
    }
    private static void checkCatalog(){
        Set<SiteCatalog.Location> builtins=EnumSet.of(SiteCatalog.Location.HOUSE,
            SiteCatalog.Location.FARMHOUSE,SiteCatalog.Location.SCHOOL,SiteCatalog.Location.MANSION,
            SiteCatalog.Location.PRISON,SiteCatalog.Location.ASYLUM,SiteCatalog.Location.ACADEMY,
            SiteCatalog.Location.COLLEGE,SiteCatalog.Location.CHURCH,
            SiteCatalog.Location.BLACKPINE,SiteCatalog.Location.HOLLOW_CREEK,
            SiteCatalog.Location.SEACLIFF,SiteCatalog.Location.LOOKOUT,SiteCatalog.Location.MINE,SiteCatalog.Location.WHARF,
            SiteCatalog.Location.CEMETERY,SiteCatalog.Location.MORTUARY,SiteCatalog.Location.NORTHWATCH,
            SiteCatalog.Location.BLACKWATER,
            SiteCatalog.Location.MONASTERY,SiteCatalog.Location.RED_MESA,SiteCatalog.Location.MOURNING_MARSH,
            SiteCatalog.Location.GREYWATER,SiteCatalog.Location.GALLOWS,SiteCatalog.Location.MORROW,SiteCatalog.Location.ELARA,SiteCatalog.Location.PINES,SiteCatalog.Location.CINDER);
        ok(SiteCatalog.soloLocations().length==builtins.size()+1,
            "all playable maps and the separate USER authoring slot remain indexed");
        int ready=0;
        Set<String> sourceIds=new HashSet<>();
        for(SiteCatalog.Location location:SiteCatalog.Location.values()){
            ok(location.playable()==builtins.contains(location),"explicit dispatch roster: "+location);
            if(location.playable()){
                ready++;SiteCatalog.requirePlayable(location);
                HouseMap map=new HouseMap(SiteCatalog.mapFor(location));
                ok(map.site().name().equals(location.name())&&sourceIds.add(map.level().id()),
                    "each dispatch resolves its own site source rather than another map: "+location);
                ok(map.valid()&&RitualSites.valid(map),"playable authored site "+location);
                ok(map.van().placement()!=null,"authentic van staging "+location);
                var solo=new InvestigationSession(map,InvestigationSession.Role.OBSERVER,101);
                SceneRenderer renderer=rendererFor(map);
                renderer.draw(map,solo,0);
                long colours=Arrays.stream(renderer.pixels()).distinct().count();
                long physicalPixels=Arrays.stream(renderer.depths()).filter(Double::isFinite).count();
                ok(colours>20,
                    "shipped source models and walls render for "+location);
                ok(physicalPixels>renderer.pixels().length/20,
                    "site render must contain physical geometry, not just varied sky: "+location);
                System.out.printf(Locale.ROOT,
                    "SITE_RENDER %s room=%d feet=(%.3f,%.3f,%.3f) colours=%d physical_pixels=%d%n",
                    location.name(),map.roomAt(renderer.x,renderer.playerFeetElevation,renderer.z),
                    renderer.x,renderer.playerFeetElevation,renderer.z,colours,physicalPixels);
            }else{
                try{SiteCatalog.requirePlayable(location);
                    throw new AssertionError("unbuilt level claimed playable");}
                catch(IllegalArgumentException expected){}
            }
        }
        ok(ready==builtins.size(),"every current built-in is checked");
        // The multiplayer map roster includes authoring-only prototypes that
        // are not yet available in the public mission catalogue.
        // Iterating it must NEVER break lobby construction or expose a
        // prototype merely because its HouseMap enum value exists.
        int multiplayerReady=0;
        for(HouseMap.Site candidate:HouseMap.Site.values()){
            boolean selectable=SiteCatalog.playable(candidate);
            if(selectable)multiplayerReady++;
            ok(selectable==builtins.stream().anyMatch(location->location.name().equals(candidate.name())),
                "multiplayer availability must match playable authored catalogue: "+candidate);
        }
        ok(multiplayerReady==ready,"the Solo and LAN catalogs agree on built-ins");
        ok(!SiteCatalog.playable(HouseMap.Site.USER)&&UserLevelLibrary.activeId()==null,
            "USER is unavailable without an explicitly selected authored project");
        ok(!SiteCatalog.Conditions.BLACKOUT.initiallyPowered(),"blackout starts powerless");
        ok(SiteCatalog.Conditions.FOGBOUND.fogMultiplier()>SiteCatalog.Conditions.STANDARD.fogMultiplier(),
            "weather modifies 3D environment");
        System.out.println("PASS: all built-in dispatch sources with real headless rendering; unselected USER remains unavailable");
    }
}
