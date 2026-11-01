package org.beckenburg;

import java.io.IOException;
import java.nio.file.*;

/** Explicit authored USER source for standalone diagnostic/test processes.
 * Never selects or edits the developer's actual project. Call before rendering;
 * the temporary home is restored before the returned immutable map is used.
 */
final class ReviewMapFixtures {
    private ReviewMapFixtures(){}
    static HouseMap userLevel(Path evidenceRoot)throws IOException{
        Files.createDirectories(evidenceRoot);
        Path home=Files.createTempDirectory(evidenceRoot,"user-fixture-home-").toAbsolutePath();
        String previous=System.getProperty("user.home");
        try{
            System.setProperty("user.home",home.toString());
            if(UserLevelLibrary.activeId()!=null)throw new AssertionError("Isolated authoring profile must start empty");
            try{
                new HouseMap(HouseMap.Site.USER);
                throw new AssertionError("Unselected USER must not silently use a built-in map");
            }catch(IllegalStateException expected){
                if(!expected.getMessage().contains("No authored user level selected"))throw expected;
            }
            UserLevelLibrary.create("ReviewAuthoredHouse",HouseMap.Site.HOUSE);
            var map=new HouseMap(HouseMap.Site.USER);
            if(map.site()!=HouseMap.Site.USER||!map.level().revision().contains("custom-ReviewAuthoredHouse")
                ||!Files.isRegularFile(UserLevelLibrary.activeDocument()))
                throw new AssertionError("Review must use an actual persisted authored USER source");
            return map;
        }finally{System.setProperty("user.home",previous);}
    }
}
