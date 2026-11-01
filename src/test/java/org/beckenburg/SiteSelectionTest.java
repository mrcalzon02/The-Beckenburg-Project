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
        finally{System.setProperty("user.home",previousHome);Files.deleteIfExists(isolatedHome);}
    }
    private static void checkCatalog(){
        Set<SiteCatalog.Location> builtins=EnumSet.of(SiteCatalog.Location.HOUSE,
            SiteCatalog.Location.FARMHOUSE,SiteCatalog.Location.SCHOOL,SiteCatalog.Location.MANSION,
            SiteCatalog.Location.PRISON,SiteCatalog.Location.ASYLUM,SiteCatalog.Location.ACADEMY,
            SiteCatalog.Location.COLLEGE,SiteCatalog.Location.CHURCH);
        ok(SiteCatalog.Location.values().length==builtins.size()+1,
            "nine built-in families and the separate USER authoring slot remain indexed");
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
                SceneRenderer renderer=new SceneRenderer();
                renderer.draw(map,solo,0);
                ok(java.util.Arrays.stream(renderer.pixels()).distinct().count()>20,
                    "shipped source models and walls render for "+location);
            }else{
                try{SiteCatalog.requirePlayable(location);
                    throw new AssertionError("unbuilt level claimed playable");}
                catch(IllegalArgumentException expected){}
            }
        }
        ok(ready==builtins.size(),"every current built-in is checked");
        // The multiplayer map roster includes authoring-only prototypes that
        // are not yet entries in the nine-site public mission catalogue.
        // Iterating it must NEVER break lobby construction or expose a
        // prototype merely because its HouseMap enum value exists.
        int multiplayerReady=0;
        for(HouseMap.Site candidate:HouseMap.Site.values()){
            boolean selectable=SiteCatalog.playable(candidate);
            if(selectable)multiplayerReady++;
            ok(selectable==builtins.stream().anyMatch(location->location.name().equals(candidate.name())),
                "multiplayer availability must match playable authored catalogue: "+candidate);
        }
        ok(multiplayerReady==ready && !SiteCatalog.playable(HouseMap.Site.HOLLOW_CREEK),
            "Hollow Creek prototype cannot crash the lobby or be dispatched as complete");
        ok(!SiteCatalog.playable(HouseMap.Site.USER)&&UserLevelLibrary.activeId()==null,
            "USER is unavailable without an explicitly selected authored project");
        ok(!SiteCatalog.Conditions.BLACKOUT.initiallyPowered(),"blackout starts powerless");
        ok(SiteCatalog.Conditions.FOGBOUND.fogMultiplier()>SiteCatalog.Conditions.STANDARD.fogMultiplier(),
            "weather modifies 3D environment");
        System.out.println("PASS: nine built-in dispatch sources with real headless rendering; unselected USER and Hollow Creek remain unavailable");
    }
}
