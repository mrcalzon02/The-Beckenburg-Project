package org.beckenburg;

import java.nio.file.*;
import java.util.*;
import org.beckenburg.LevelDefinition.Prop;

/** Actual saved legacy and rebuilt projects must retain different architectural authorities. */
public final class HouseTemplateCompatibilityTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static Map<LevelDefinition.Collider,Long> physicalBodies(LevelDefinition level){
        return level.colliders().stream().collect(java.util.stream.Collectors.groupingBy(
            body->body,java.util.stream.Collectors.counting()));
    }
    private static Properties metadata(String id)throws Exception{
        var result=new Properties();
        try(var in=Files.newInputStream(UserLevelLibrary.folder(id).resolve("template.properties"))){result.load(in);}
        return result;
    }
    private static void writeMetadata(String id,Properties data)throws Exception{
        try(var out=Files.newOutputStream(UserLevelLibrary.folder(id).resolve("template.properties"))){data.store(out,"Compatibility fixture");}
    }
    public static void main(String[] args)throws Exception{
        String previous=System.getProperty("user.home");
        Path home=Files.createTempDirectory("house-template-compatibility-");
        System.setProperty("user.home",home.toString());
        try{
            var legacy=LevelPlans.legacyHouseSource();var rebuilt=HouseRebuildFurnishings.source();
            check(HouseRebuildServices.source().revision().equals("first-principles-heating-v1-1d5d95258b105737"),"Fridge relocation must preserve accepted underlying construction coordinates");
            check(legacy.rooms().get(5).y()==3&&rebuilt.rooms().get(5).y()==2.8,"Independent old/new floor reference");
            check(legacy.rooms().get(9).y()==-3&&rebuilt.rooms().get(9).y()==-2.4,"Independent old/new cellar reference");
            UserLevelLibrary.create("BeforeRebuild",HouseMap.Site.HOUSE,UserLevelLibrary.HOUSE_ORIGINAL);
            check(UserLevelLibrary.HOUSE_ORIGINAL.equals(metadata("BeforeRebuild").getProperty("architecture")),"New legacy project pins architectural generation");
            var oldMeta=metadata("BeforeRebuild");oldMeta.remove("architecture");writeMetadata("BeforeRebuild",oldMeta);
            var oldSession=new LevelAuthoringSession(HouseMap.Site.USER);
            String first=oldSession.nodeId(0);Prop p=oldSession.prop(0);
            Prop moved=new Prop(p.mesh(),p.room(),p.x()+.125,p.y(),p.z(),p.scale(),p.yaw(),p.halfX(),p.halfZ(),p.height(),p.support());
            oldSession.put(0,moved);oldSession.remove(oldSession.size()-1);oldSession.save();
            byte[] oldScene=Files.readAllBytes(oldSession.documentPath());
            Path oldMetadata=UserLevelLibrary.folder("BeforeRebuild").resolve("template.properties");
            byte[] oldBytes=Files.readAllBytes(oldMetadata);

            UserLevelLibrary.create("RebuiltHouse",HouseMap.Site.HOUSE,UserLevelLibrary.HOUSE_REBUILD);
            var fresh=new LevelAuthoringSession(HouseMap.Site.USER);
            check(fresh.sourceLevel().rooms().equals(rebuilt.rooms()),"Actual editor uses rebuilt architecture");
            check(fresh.sourceLevel().stairs().equals(rebuilt.stairs())&&fresh.sourceLevel().voids().equals(rebuilt.voids()),"Actual rebuilt floor openings and continuous stairs retained");
            check(fresh.props().equals(rebuilt.props()),"Actual furnished source loaded");
            var game=new HouseMap(HouseMap.Site.USER);
            check(game.level().rooms().equals(rebuilt.rooms())&&game.level().sockets().equals(rebuilt.sockets()),"Playable USER map uses same authored rooms and functional sockets");
            // Native composition groups edited prop bodies after fixed geometry.
            // Compare every body's coordinates AND multiplicity, independent of list order.
            check(physicalBodies(game.level()).equals(physicalBodies(rebuilt)),"Runtime source retains every physical body and multiplicity");
            check(game.level().props().stream().anyMatch(prop->prop.mesh().equals("level_house_fridge_worn")),"Runtime receives approved furnishing");
            String revision=game.level().revision();fresh.save();fresh.reopen();
            check(new HouseMap(HouseMap.Site.USER).level().revision().equals(revision),"Stable network identity across native save/reopen");
            int fridge=-1;for(int i=0;i<fresh.size();i++)if(fresh.prop(i).mesh().equals("level_house_fridge_worn"))fridge=i;
            check(fridge>=0,"Actual cold-storage fixture exists");
            var originalFridge=fresh.prop(fridge);
            var renderer=new SceneRenderer();
            var agent=new LevelEditorAgent(fresh,()->new HouseMap(HouseMap.Site.USER,fresh.level()),renderer,()->{});
            var service=new org.beckenburg.editor.EditorAgentService<>(agent,new org.beckenburg.editor.EditorHistory<>(fresh,20));
            service.transact(fresh.revision(),"move-prop",Map.of("id",fresh.nodeId(fridge),"x",Double.toString(originalFridge.x()-.125),
                "y",Double.toString(originalFridge.y()),"z",Double.toString(originalFridge.z())));
            var beforeBodies=new ArrayList<>(rebuilt.colliders());
            var fridgeBody=beforeBodies.stream().filter(body->Math.abs(body.x()-14.9)<1e-8&&Math.abs(body.z()-14.1925)<1e-8&&Math.abs(body.y())<1e-8).findFirst().orElseThrow();
            beforeBodies.remove(fridgeBody);var afterBodies=new ArrayList<>(fresh.level().colliders());
            for(var body:beforeBodies)check(afterBodies.remove(body),"Editing fridge must retain every other exact authored body, including generator parts");
            check(afterBodies.size()==1,"Move must replace exactly one body without duplicate generic boxes");
            var movedBody=afterBodies.get(0);
            check(Math.abs(movedBody.x()-(fridgeBody.x()-.125))<1e-8&&Math.abs(movedBody.y()-fridgeBody.y())<1e-8
                &&Math.abs(movedBody.z()-fridgeBody.z())<1e-8&&movedBody.halfX()==fridgeBody.halfX()
                &&movedBody.halfZ()==fridgeBody.halfZ()&&movedBody.height()==fridgeBody.height(),"Moved physics follows native origin and retained source dimensions");
            check(agent.test("mission-runtime-parity").stream().allMatch(check->check.passed()),"Editor Agent Services validate actual rebuilt native/runtime geometry");
            fresh.save();fresh.reopen();
            check(new HouseMap(HouseMap.Site.USER).level().props().equals(fresh.props()),"Edited rebuilt furniture persists into playable map");
            if(args.length>0){
                var out=Path.of(args[0]);Files.createDirectories(out);
                var editedMap=new HouseMap(HouseMap.Site.USER,fresh.level());double floor=editedMap.groundHeight(16,12.4,2);
                check(editedMap.traversable(16,floor,12.4)&&editedMap.roomAt(16,floor,12.4)==2,"Actual custom-map review camera remains in kitchen");
                renderer.setRenderSize(1000,625);renderer.lamp=true;renderer.roomLights=new RoomLighting();
                renderer.x=16;renderer.z=12.4;renderer.eyeHeight=floor+1.62;
                renderer.yaw=Math.atan2(14.775-16,14.1925-12.4);renderer.pitch=Math.atan2(.85-1.62,Math.hypot(14.775-16,14.1925-12.4));
                check(service.capture(fresh.revision(),out.resolve("edited-custom-house-kitchen-runtime.png")).result().renderer().equals("SceneRenderer.drawPractice"),"Capture uses actual Editor Agent Services native viewport");
            }
            byte[] newScene=Files.readAllBytes(fresh.documentPath());

            UserLevelLibrary.activate("BeforeRebuild");oldSession.reopen();
            check(oldSession.sourceLevel().rooms().equals(legacy.rooms()),"Versionless historical project retains original architecture");
            check(oldSession.nodeId(0).equals(first)&&oldSession.prop(0).equals(moved)&&oldSession.size()==legacy.props().size()-1,"Legacy stable identity, move and deletion survive source switching");
            check(new HouseMap(HouseMap.Site.USER).level().props().equals(oldSession.props()),"Legacy gameplay uses exact retained authored edits");
            check(Arrays.equals(oldScene,Files.readAllBytes(oldSession.documentPath()))&&Arrays.equals(oldBytes,Files.readAllBytes(oldMetadata)),"Opening legacy project must not rewrite original bytes");
            check(Arrays.equals(newScene,Files.readAllBytes(fresh.documentPath())),"Switching old source cannot mutate rebuilt project");

            UserLevelLibrary.activate("RebuiltHouse");var meta=metadata("RebuiltHouse");meta.setProperty("architecture","house-rebuild-v999");writeMetadata("RebuiltHouse",meta);
            byte[] unsupported=Files.readAllBytes(UserLevelLibrary.folder("RebuiltHouse").resolve("template.properties"));
            try{new LevelAuthoringSession(HouseMap.Site.USER);throw new AssertionError("Future source silently substituted");}catch(IllegalStateException expected){}
            check(Arrays.equals(unsupported,Files.readAllBytes(UserLevelLibrary.folder("RebuiltHouse").resolve("template.properties")))&&Arrays.equals(newScene,Files.readAllBytes(fresh.documentPath())),"Rejected future source preserves files");
            try{UserLevelLibrary.create("InvalidVersion",HouseMap.Site.HOUSE,"future");throw new AssertionError("Invalid new source accepted");}catch(IllegalArgumentException expected){}
            check(!Files.exists(UserLevelLibrary.folder("InvalidVersion")),"Invalid source creates no partial project");
            try{UserLevelLibrary.templateSource(HouseMap.Site.SCHOOL,UserLevelLibrary.HOUSE_REBUILD);throw new AssertionError("Cross-site source accepted");}catch(IllegalArgumentException expected){}
            System.out.println("PASS pinned House architectural generations: native legacy edits/identity/bytes, actual rebuilt USER geometry/sockets/colliders/network revision, isolated saves and unknown-version rejection");
        }finally{
            System.setProperty("user.home",previous);
            try(var paths=Files.walk(home)){for(Path path:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}
        }
    }
}
