package org.beckenburg;

import java.nio.file.*;
import java.util.*;
import org.beckenburg.LevelDefinition.*;
import org.beckenburg.editor.RuntimeSceneDocument;

/** Live fresh dispatch uses the rebuilt House; previous authored files stay pinned. */
public final class HouseDefaultPromotionTest {
    private HouseDefaultPromotionTest(){}
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        String previous=System.getProperty("user.home");
        Path home=Files.createTempDirectory("briar-house-default-");
        System.setProperty("user.home",home.toString());
        try{
            Path saved=LevelAuthoringSession.path(HouseMap.Site.HOUSE);
            check(!Files.exists(saved),"Fresh fixture has a saved House");
            LevelDefinition source=HouseRebuildFurnishings.source();
            HouseMap live=new HouseMap();
            LevelDefinition now=live.level();
            check("briar-rebuild".equals(now.id()),"Fresh House still dispatches the original");
            check(now.revision().equals(source.revision()),"Live and accepted source network revisions differ");
            check(now.rooms().equals(source.rooms())&&now.portals().equals(source.portals())
                &&now.stairs().equals(source.stairs())&&now.voids().equals(source.voids())
                &&now.props().equals(source.props())&&now.sockets().equals(source.sockets())
                &&now.colliders().equals(source.colliders())&&now.dock().equals(source.dock()),
                "Live House lost constructed rooms, physical geometry, fittings or van");
            check(live.envelope()!=null&&live.services()!=null&&live.worldSurface()!=null,
                "House lacks constructed shell, services or terrain");
            check(live.hasGenerator()&&now.sockets().stream().anyMatch(s->s.type().equals("breaker")),
                "House missing independent source/breaker");
            check(now.rooms().stream().filter(r->!r.outside()).map(Room::y)
                .collect(java.util.stream.Collectors.toSet()).equals(Set.of(-2.4,0.0,2.8)),
                "House still uses old floor elevations");
            check(now.stairs().size()==2&&now.voids().size()>=2
                &&now.stairs().stream().anyMatch(s->s.a()==9&&s.b()==2),
                "House missing new upper/cellar stairs or floor openings");
            check(now.sockets().stream().filter(s->s.type().equals("ritual")).count()==3,
                "House lost three ritual reservations");
            check(now.sockets().stream().filter(s->s.type().equals("hide")).count()==HidingBudget.MAXIMUM,
                "House hiding candidates differ from shared limit");
            check(now.sockets().stream().filter(s->s.type().equals("story")).count()==3
                &&live.clutter().states().size()==8,
                "House lost family records or physical belongings");
            check(SiteCaseFiles.forSite(now.id()).reference().equals("BB / RES-017"),
                "House lost its canonical case file");
            check(live.valid()&&RitualSites.valid(live),"Live House mission and ritual invalid");
            for(Room room:now.rooms()){
                Vec spawn=room.spawn();
                check(live.roomAt(spawn.x(),spawn.y(),spawn.z())==room.id()
                    &&live.traversable(spawn.x(),spawn.y(),spawn.z()),
                    "Blocked or misowned source spawn "+room.id());
            }
            var routes=LevelTraversalAudit.inspect(live);
            check(routes.passed(),"Physical van/room/storey routes failed "+routes.findings());
            HouseRebuildPowerTest.approach(live,InteractionFocus.Kind.GENERATOR);
            HouseRebuildPowerTest.approach(live,InteractionFocus.Kind.BREAKER);
            var editor=new LevelAuthoringSession(HouseMap.Site.HOUSE);
            check(UserLevelLibrary.HOUSE_REBUILD.equals(editor.snapshot().architecture())
                &&editor.level().revision().equals(now.revision()),
                "Editor and game disagree on default built-in House");
            editor.save();
            check("briar-rebuild".equals(new HouseMap().level().id())
                &&UserLevelLibrary.HOUSE_REBUILD.equals(
                    HouseSourceVersions.generation(RuntimeSceneDocument.load(saved))),
                "Fresh saved built-in House fails to reopen as rebuilt");
            // Versionless previous native files must never be silently migrated.
            LevelAuthoringSession.saveProps(saved,"beckenburg/house",
                LevelPlans.legacyHouseSource().props());
            byte[] historical=Files.readAllBytes(saved);
            HouseMap archived=new HouseMap();
            check("house".equals(archived.level().id())
                &&archived.level().rooms().equals(LevelPlans.legacyHouseSource().rooms())
                &&archived.level().props().equals(LevelPlans.legacyHouseSource().props()),
                "Saved historical House lost its independent original geometry");
            check(UserLevelLibrary.HOUSE_ORIGINAL.equals(
                new LevelAuthoringSession(HouseMap.Site.HOUSE).snapshot().architecture())
                &&Arrays.equals(historical,Files.readAllBytes(saved)),
                "Opening historical House altered native bytes or architecture");
            System.out.println("PASS rebuilt Briar House fresh default, full sourced geometry, "
                +"navigation, power controls, rituals, lore, native editor and historical save isolation");
        }finally{
            System.setProperty("user.home",previous);
            try(var files=Files.walk(home)){
                for(Path path:files.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);
            }
        }
    }
}
