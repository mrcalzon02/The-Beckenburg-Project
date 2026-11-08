package org.beckenburg;

import java.nio.file.*;
import java.util.*;

/** Fresh installs use the rebuild; historical persisted coordinates stay historical. */
public final class HouseDefaultPromotionTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        String previous=System.getProperty("user.home");Path home=Files.createTempDirectory("house-default-promotion-");
        System.setProperty("user.home",home.toString());
        try{
            var site=HouseMap.Site.HOUSE;var target=HouseRebuildFurnishings.source();
            Path document=LevelAuthoringSession.path(site);
            var game=new HouseMap(site);var editor=new LevelAuthoringSession(site);
            check(game.level().rooms().equals(target.rooms())&&game.level().props().equals(target.props()),"Fresh actual gameplay must use furnished rebuild");
            check(game.level().colliders().equals(target.colliders())&&game.hasGenerator(),"Fresh runtime shares authored collisions and actual power source");
            check(editor.sourceLevel().rooms().equals(target.rooms())&&editor.level().props().equals(target.props()),"Fresh editor and gameplay share architecture");
            check(!Files.exists(document),"Opening fresh gameplay/editor must not implicitly write a native file");
            check(new HouseEncounter(game).phase()==HouseEncounter.Phase.UNNOTICED,"Fresh actual site enables its authored encounter");
            editor.save();byte[] saved=Files.readAllBytes(document);
            check(new LevelAuthoringSession(site).snapshot().equals(editor.snapshot()),"Fresh saved metadata and stable prop identities reopen exactly");
            check(new HouseMap(site).level().revision().equals(editor.level().revision()),"Saved editor and actual game retain the same network revision");
            check(Arrays.equals(saved,Files.readAllBytes(document)),"Gameplay opening cannot mutate saved native bytes");
            HouseRebuildPowerTest.main(new String[]{"SAVED_HOUSE"});
            var legacy=LevelPlans.legacyHouseSource();
            // Manufacture an actual historical versionless document, never an implicit fallback.
            LevelAuthoringSession.saveProps(document,"beckenburg/house",legacy.props());
            byte[] old=Files.readAllBytes(document);
            check(new HouseMap(site).level().rooms().equals(legacy.rooms()),"Versionless built-in House keeps old storey coordinates");
            check(new LevelAuthoringSession(site).sourceLevel().rooms().equals(legacy.rooms()),"Historical editor stays on its saved architecture");
            check(Arrays.equals(old,Files.readAllBytes(document)),"Historical gameplay/editor opening preserves exact native bytes");
            UserLevelLibrary.create("FreshRebuiltHouse",site);
            var custom=new HouseMap(HouseMap.Site.USER);
            check(custom.level().rooms().equals(target.rooms())&&custom.level().props().equals(target.props()),"New House templates use the rebuilt furnished source");
            check(new HouseEncounter(custom).phase()==HouseEncounter.Phase.DISABLED,"Custom templates do not force the location-specific encounter");
            System.out.println("PASS fresh default/editor/save/network revision and real4TCP/solo source power; historical native bytes and new rebuilt custom template preserved");
        }finally{
            System.setProperty("user.home",previous);
            try(var files=Files.walk(home)){for(Path file:files.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(file);}
        }
    }
}
