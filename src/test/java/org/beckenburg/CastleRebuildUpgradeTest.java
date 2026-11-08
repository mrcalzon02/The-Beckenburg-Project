package org.beckenburg;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import org.beckenburg.editor.*;

/** Actual historical defaults, native three-way edits, undo/save and rejected conflicts. */
public final class CastleRebuildUpgradeTest {
    private static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
    private static byte[] original(String name)throws Exception{try(var in=CastleRebuildUpgradeTest.class.getResourceAsStream("/org/beckenburg/sites/castle-baseline-"+name+".properties")){return Objects.requireNonNull(in).readAllBytes();}}
    private static Properties properties(byte[] bytes)throws Exception{var p=new Properties();try(var in=new ByteArrayInputStream(bytes)){p.load(in);}return p;}
    private static void write(Path path,Properties p)throws Exception{try(var out=Files.newOutputStream(path)){p.store(out,"Actual historical Castle with explicit author edits");}}
    private static String key(Properties p,String id){for(int i=0;i<Integer.parseInt(p.getProperty("props.count"));i++)if(id.equals(p.getProperty("props."+i+".id")))return "props."+i+".";throw new AssertionError("Missing historical fixture "+id);}
    private static CastleLevelEdits custom(Path path,boolean structureV1)throws Exception{
        var p=properties(original("original"));p.setProperty(key(p,"ground6")+"x","16.0");
        if(structureV1){p.setProperty("structure.layout","1");p.remove("surfaces.count");}
        write(path,p);byte[] before=Files.readAllBytes(path);var scene=new CastleLevelEdits(path);
        check(scene.snapshot().rebuildLayout()==0,"Custom scene silently marked rebuilt");
        check(scene.props().stream().anyMatch(a->a.id().equals("ground6")&&a.x()==16),"Authored original chair lost during load");
        if(!structureV1)check(Arrays.equals(before,Files.readAllBytes(path)),"Custom old scene rewritten automatically");
        return scene;
    }
    public static void main(String[] args)throws Exception{
        Path home=Files.createTempDirectory("castle-rebuild-upgrade-");System.setProperty("user.home",home.toString());
        var scene=CastleLevelEdits.current();var canonical=scene.snapshot();
        for(String name:List.of("original","before-finish")){
            Path file=home.resolve(name+".properties");byte[] bytes=original(name);Files.write(file,bytes);var upgraded=new CastleLevelEdits(file);
            check(canonical.equals(upgraded.snapshot()),"Unedited actual "+name+" default did not receive complete rebuild");
            check(Arrays.equals(bytes,Files.readAllBytes(file.resolveSibling(file.getFileName()+".before-first-principles.properties"))),"Exact original bytes not preserved");
            check(Arrays.equals(bytes,Files.readAllBytes(EditorDocumentStore.previous(file))),"Automatic rebuild not journaled");
            check(new CastleLevelEdits(file).snapshot().equals(canonical),"Automatic rebuild not persistent/idempotent");
        }
        var old=custom(home.resolve("custom.properties"),false);old.removeProp("ground0");old.paintFloor(2,2,MaterialAtlas.STONE);old.setNavBlocked(3,12,true);old.setSpawn(13.5,19,1.25);old.setStation(CastleHubMap.Station.OPTIONS,24.8,23);
        old.put(new CastleLevelEdits.Prop("personal-crate","wooden_crate_1",13,0,8,1,0,.3600001,.2520001,false));
        old.put(new CastleLevelEdits.Surface("personal-plinth",CastleLevelEdits.Kind.WALL,30,15,30.5,15,0,.3,MaterialAtlas.STONE));
        var lamp=old.lamps().stream().filter(l->l.mount()==CastleHubDecor.Mount.WALL).findFirst().orElseThrow();
        old.put(new CastleLevelEdits.Lamp(lamp.id(),lamp.on(),lamp.off(),lamp.x(),lamp.y(),lamp.z(),lamp.scale(),lamp.yaw(),lamp.mount(),lamp.radius(),lamp.power()+.1));
        old.save();var saved=old.snapshot();check(new CastleLevelEdits(old.path()).snapshot().equals(saved),"Ordinary custom save/load loses content or version");
        scene.restore(saved);scene.save();byte[] disk=Files.readAllBytes(scene.path());
        var project=new CastleEditorProject(scene);var history=new EditorHistory<CastleLevelEdits.Snapshot>(project,60);var renderer=new SceneRenderer();
        var agent=new EditorAgentService<>(new CastleEditorAgent(project,new CastleHubMap(),renderer,()->{}),history);
        check(agent.inspect().operations().contains("rebuild-castle"),"Actual editor service omits human rebuild operation");
        agent.transact(scene.revision(),"rebuild-castle",Map.of("baseline","ORIGINAL"));var result=scene.snapshot();
        check(result.rebuildLayout()==1&&CastleGround.active()&&CastleUpper.active()&&CastleCellar.active(),"Actual native structure not upgraded");
        check(scene.props().stream().anyMatch(p->p.id().equals("ground6")&&p.x()==16)&&scene.props().stream().noneMatch(p->p.id().equals("ground0")),"Edited/deleted source fixtures not retained");
        check(result.props().stream().anyMatch(p->p.id().equals("personal-crate"))&&result.surfaces().containsAll(saved.surfaces().stream().filter(s->s.id().equals("personal-plinth")).toList()),"Personal additions lost");
        check(result.floorPaint().equals(saved.floorPaint())&&result.navBlocked().equals(saved.navBlocked())&&Arrays.equals(result.stations().get("OPTIONS"),saved.stations().get("OPTIONS"))&&result.spawnYaw()==1.25,"Paint/navigation/station/spawn edits lost");
        check(result.lamps().stream().anyMatch(l->l.id().equals(lamp.id())&&l.power()==lamp.power()+.1),"Edited light lost");
        check(Arrays.equals(disk,Files.readAllBytes(scene.path())),"Agent rebuild saved implicitly");
        agent.undo(scene.revision());check(saved.equals(scene.snapshot()),"Shared undo did not restore complete original native scene/version");
        agent.redo(scene.revision());check(result.equals(scene.snapshot()),"Shared redo lost upgraded native scene");agent.save(scene.revision());
        check(Arrays.equals(disk,Files.readAllBytes(scene.path().resolveSibling(scene.path().getFileName()+".before-first-principles.properties"))),"Explicit saved rebuild did not preserve original document bytes");
        check(new CastleLevelEdits(scene.path()).snapshot().equals(result),"Upgraded custom scene not persistent");
        if(args.length>0){
            Path out=Path.of(args[0]);Files.createDirectories(out);renderer.setRenderSize(1000,625);renderer.lamp=false;var map=new CastleHubMap();
            double[][] views={{15.5,7.7,0,-1.451,-.4},{39.2,12.7,0,.312,-.1},{-1,23,0,Math.atan2(-1,-4),-.25},{33,27.6,-3,0,-.05}};
            String[] names={"contract","museum","annex","cellar"};
            for(int i=0;i<views.length;i++){var v=views[i];check(map.traversable(v[0],v[1],v[2]),"Upgraded native review camera blocked "+names[i]);renderer.x=v[0];renderer.z=v[1];renderer.eyeHeight=v[2]+1.62;renderer.yaw=v[3];renderer.pitch=v[4];check(agent.capture(scene.revision(),out.resolve("upgraded-custom-"+names[i]+"-runtime.png")).result().renderer().equals("SceneRenderer.drawCastle"),"Synthetic upgrade capture");}
        }
        scene.removeProp("finish-keeper-statue");scene.save();var deleted=scene.snapshot();scene.reopen();check(deleted.equals(scene.snapshot()),"Current-version deletion resurrected");
        agent.transact(scene.revision(),"rebuild-castle",Map.of("baseline","ORIGINAL"));check(deleted.equals(scene.snapshot()),"Repeated rebuild resurrects a deleted current fitting");
        // A real saved custom crate at a newly partitioned wall must block the upgrade, never disappear.
        var conflicting=custom(home.resolve("conflicting.properties"),false);
        conflicting.put(new CastleLevelEdits.Prop("personal-wall-crate","wooden_crate_1",23.5,0,3,1,0,.36,.252,false));
        scene.restore(conflicting.snapshot());scene.save();var before=scene.snapshot();disk=Files.readAllBytes(scene.path());
        try{agent.transact(scene.revision(),"rebuild-castle",Map.of("baseline","ORIGINAL"));throw new AssertionError("Wall intersection accepted");}catch(IllegalArgumentException expected){check(expected.getMessage().contains("personal-wall-crate"),"Conflict does not identify retained object");}
        check(before.equals(scene.snapshot())&&Arrays.equals(disk,Files.readAllBytes(scene.path())),"Rejected rebuild mutated native scene/disk");
        // The new keeper must not intersect a retained source object at its authored coordinates.
        var colliding=custom(home.resolve("collision.properties"),false);colliding.put(new CastleLevelEdits.Prop("personal-exhibit-crate","wooden_crate_1",40.2,0,15.8,1,0,.36,.252,false));
        scene.restore(colliding.snapshot());before=scene.snapshot();
        try{agent.transact(scene.revision(),"rebuild-castle",Map.of("baseline","ORIGINAL"));throw new AssertionError("New exhibit intersects retained crate");}catch(IllegalArgumentException expected){check(expected.getMessage().contains("personal-exhibit-crate"),"Model conflict omitted actual object ID");}
        check(before.equals(scene.snapshot()),"Model conflict discarded retained source object");
        var swallowed=custom(home.resolve("swallowed.properties"),false);swallowed.put(new CastleLevelEdits.Surface("personal-book-plinth",CastleLevelEdits.Kind.FLOOR,40.05,15.65,40.35,15.95,.08,.08,MaterialAtlas.STONE));swallowed.put(new CastleLevelEdits.Prop("personal-floor-book","level_loose_book_worn",40.2,.08,15.8,1,0,.081,.114,false));scene.restore(swallowed.snapshot());before=scene.snapshot();
        try{agent.transact(scene.revision(),"rebuild-castle",Map.of("baseline","ORIGINAL"));throw new AssertionError("New stone plinth swallowed retained floor book");}catch(IllegalArgumentException expected){check(expected.getMessage().contains("personal-floor-book"),"Contained-object conflict omitted its actual ID");}
        check(before.equals(scene.snapshot()),"Containment conflict discarded retained book");
        var duplicate=custom(home.resolve("duplicate.properties"),false);
        var target=canonical.props().stream().filter(p->p.id().startsWith("finish-")&&p.mesh().equals("wooden_crate_1")).findFirst().orElseThrow();
        duplicate.put(new CastleLevelEdits.Prop("personal-duplicate-crate",target.mesh(),target.x(),target.y(),target.z(),target.scale(),target.yaw(),target.halfX(),target.halfZ(),target.upper()));
        scene.restore(duplicate.snapshot());before=scene.snapshot();
        try{agent.transact(scene.revision(),"rebuild-castle",Map.of("baseline","ORIGINAL"));throw new AssertionError("Coincident new fitting accepted");}catch(IllegalArgumentException expected){check(expected.getMessage().contains("personal-duplicate-crate")&&expected.getMessage().contains("duplicates new fitting"),"Duplicate conflict omitted actual object ID or geometry check");}
        check(before.equals(scene.snapshot()),"Duplicate conflict discarded original scene");
        // A custom object supported by the old small dining table cannot silently float after refit.
        var pBefore=properties(original("before-finish"));pBefore.setProperty(key(pBefore,"ground6")+"x","16.0");Path supported=home.resolve("supported.properties");write(supported,pBefore);var tableScene=new CastleLevelEdits(supported);
        tableScene.put(new CastleLevelEdits.Prop("personal-table-book","level_loose_book_worn",5.1,.602,24.9,1,0,.081,.114,false));scene.restore(tableScene.snapshot());before=scene.snapshot();
        try{agent.transact(scene.revision(),"rebuild-castle",Map.of("baseline","BEFORE_FINISH"));throw new AssertionError("Retained book floats after parent table refit");}catch(IllegalArgumentException expected){check(expected.getMessage().contains("personal-table-book")&&expected.getMessage().contains("support"),"Support conflict not identified");}
        check(before.equals(scene.snapshot()),"Support conflict discarded original table/book scene");
        var v1=custom(home.resolve("authored-v1.properties"),true);check(v1.snapshot().props().stream().anyMatch(p->p.id().equals("ground6")&&p.x()==16),"Structure-v1 reset discards valid authored fields");
        Path partial=home.resolve("partial-v1.properties");Files.writeString(partial,"format=1\nlamps.count=1\nlamps.0.id=authored-light\n");byte[] partialBytes=Files.readAllBytes(partial);
        try{new CastleLevelEdits(partial);throw new AssertionError("Partial authored file silently reset");}catch(IllegalStateException expected){}check(Arrays.equals(partialBytes,Files.readAllBytes(partial)),"Partial authored file overwritten");
        Path future=home.resolve("future.properties");var p=properties(original("original"));p.setProperty("rebuild.layout","2");write(future,p);byte[] futureBytes=Files.readAllBytes(future);
        try{new CastleLevelEdits(future);throw new AssertionError("Future rebuild schema accepted");}catch(IllegalStateException expected){}check(Arrays.equals(futureBytes,Files.readAllBytes(future)),"Future version was overwritten");
        scene.restore(canonical);
        System.out.println("PASS actual original/pre-furnishing automatic rebuilds + exact backups; custom props/deletions/lights/geometry/paint/navigation/stations/spawn; shared native transaction/undo/redo/explicit save; conflict rollback; current deletion retention; authored-v1 and future-version preservation");
    }
}
