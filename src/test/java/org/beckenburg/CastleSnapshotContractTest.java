package org.beckenburg;

import java.nio.file.*;
import java.util.*;

/** Snapshot data cannot become a mutable, misleading substitute for native state. */
public final class CastleSnapshotContractTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        if(args.length>0){
            Path unrelated=Path.of(System.getProperty("user.home"),".beckenburg","editor","castle.properties");byte[] before=Files.readAllBytes(unrelated);
            var explicit=new CastleLevelEdits(Path.of(args[0]));explicit.save();
            check(Arrays.equals(before,Files.readAllBytes(unrelated)),"Explicit codec cannot read/migrate unrelated default scene");
            try{CastleLevelEdits.current();throw new AssertionError("Corrupt actual default scene silently hidden");}catch(ExceptionInInitializerError expected){}
            check(new CastleLevelEdits(explicit.path()).snapshot().equals(explicit.snapshot()),"Explicit codecs remain usable after unrelated runtime failure");return;
        }
        Path home=Files.createTempDirectory("castle-snapshot-contract-");
        try{
            Path unrelated=home.resolve("other-profile/.beckenburg/editor/castle.properties");Files.createDirectories(unrelated.getParent());Files.writeString(unrelated,"format=invalid");
            var child=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin",System.getProperty("os.name").startsWith("Windows")?"java.exe":"java").toString(),"-Duser.home="+home.resolve("other-profile"),"-Djava.awt.headless=true","-Djava.util.prefs.PreferencesFactory=org.beckenburg.VerificationPreferencesFactory","-ea","-cp",System.getProperty("java.class.path"),
                CastleSnapshotContractTest.class.getName(),home.resolve("explicit-codec.properties").toString()).inheritIO().start();
            if(!child.waitFor(30,java.util.concurrent.TimeUnit.SECONDS)){child.destroyForcibly();throw new AssertionError("Isolated codec child hung");}
            check(child.exitValue()==0,"Codec scope remains independent from corrupt default runtime profile");
            var scene=new CastleLevelEdits(home.resolve("castle.properties"));var original=scene.snapshot();
            check(original.equals(scene.snapshot())&&original.hashCode()==scene.snapshot().hashCode(),"Equal native state has structural snapshot equality and hash");
            var arrays=new LinkedHashMap<>(original.stations());var props=new ArrayList<>(original.props());
            var copy=new CastleLevelEdits.Snapshot(props,original.lamps(),original.surfaces(),arrays,original.floorPaint(),original.wallPaint(),original.navBlocked(),original.doors(),original.spawnX(),original.spawnZ(),original.spawnYaw());
            var station=CastleHubMap.Station.values()[0];arrays.get(station.name())[0]+=1;props.clear();
            check(copy.equals(original),"Constructor defensively owns caller collections and coordinate arrays");
            copy.stations().get(station.name())[0]+=2;
            check(copy.equals(original)&&scene.snapshot().equals(original),"Accessor mutation cannot change retained snapshot or actual Castle");
            try{copy.stations().clear();throw new AssertionError("Mutable station map");}catch(UnsupportedOperationException expected){}
            try{copy.props().clear();throw new AssertionError("Mutable props");}catch(UnsupportedOperationException expected){}
            double[] point=scene.station(station);scene.setStation(station,point[0]+.1,point[1]);
            check(!scene.snapshot().equals(original),"Actual native station edit changes snapshot value");
            scene.restore(copy);check(scene.snapshot().equals(original),"Restoration consumes preserved native snapshot values");scene.save();
            check(new CastleLevelEdits(scene.path()).snapshot().equals(original),"Native save/reopen preserves structural snapshot value");
            scene.setSpawn(original.spawnX(),original.spawnZ(),original.spawnYaw()+.125);scene.save();
            var beforeDoor=scene.snapshot();byte[] beforeDoorBytes=Files.readAllBytes(scene.path());
            byte[] priorDoorBytes=Files.readAllBytes(org.beckenburg.editor.EditorDocumentStore.previous(scene.path()));
            var firstDoor=CastleDoorSlots.all().stream().filter(slot->scene.doorType(slot.id())!=CastleDoorSlots.Type.EMPTY).findFirst().orElseThrow();
            var type=scene.doorType(firstDoor.id());scene.removeDoor(firstDoor.id());scene.installDoor(firstDoor.id(),type);
            check(scene.snapshot().equals(beforeDoor),"Door insertion order does not alter native scene value");scene.save();
            check(Arrays.equals(beforeDoorBytes,Files.readAllBytes(scene.path()))&&Arrays.equals(priorDoorBytes,Files.readAllBytes(org.beckenburg.editor.EditorDocumentStore.previous(scene.path()))),
                "Equivalent door remove/reinstall cannot rotate meaningful save history");
            scene.restore(original);scene.save();
            try{
                new CastleLevelEdits.Snapshot(original.props(),original.lamps(),original.surfaces(),Map.of(),original.floorPaint(),original.wallPaint(),original.navBlocked(),original.doors(),original.spawnX(),original.spawnZ(),original.spawnYaw());
                throw new AssertionError("Incomplete snapshot accepted");
            }catch(IllegalArgumentException expected){}
            byte[] valid=Files.readAllBytes(scene.path());var properties=new Properties();try(var in=Files.newInputStream(scene.path())){properties.load(in);}
            properties.setProperty("station."+station.name()+".x","NaN");
            Path backup=org.beckenburg.editor.EditorDocumentStore.previous(scene.path());
            try(var out=Files.newOutputStream(backup)){properties.store(out,"Invalid recovery source fixture");}
            try{scene.recoverPrevious();throw new AssertionError("Nonfinite station backup installed");}catch(java.io.IOException expected){}
            check(scene.snapshot().equals(original)&&Arrays.equals(valid,Files.readAllBytes(scene.path())),"Invalid station backup rejected before replacing actual native source");
            properties.setProperty("station."+station.name()+".x","999");
            try(var out=Files.newOutputStream(scene.path())){properties.store(out,"Out-of-world station fixture");}
            byte[] invalid=Files.readAllBytes(scene.path());
            try{scene.reopen();throw new AssertionError("Out-of-world station accepted");}catch(java.io.IOException expected){}
            check(scene.snapshot().equals(original)&&Arrays.equals(invalid,Files.readAllBytes(scene.path())),"Human station bounds also apply to native decode; failed reload preserves both source and live state");
            System.out.println("PASS Castle snapshot contract: owned collections/arrays, immutable accessors, structural equality/hash and exact native restore/save/reopen and isolated default-profile ownership");
        }finally{try(var paths=Files.walk(home)){for(var p:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
    }
}
