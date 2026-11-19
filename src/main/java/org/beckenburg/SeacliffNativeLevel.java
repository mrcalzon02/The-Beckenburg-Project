package org.beckenburg;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.beckenburg.LevelDefinition.Dock;
import org.beckenburg.LevelDefinition.Portal;
import org.beckenburg.LevelDefinition.Room;
import org.beckenburg.LevelDefinition.Socket;
import org.beckenburg.LevelDefinition.Vec;

/** Loads the surveyed Battery packet for source review and ordinary packaged play. */
final class SeacliffNativeLevel {
    static final String TITLE = "Seacliff Battery";
    static final String REVIEW_PROPERTY = "beckenburg.review.seacliff";
    private static final String ROOT = "/org/beckenburg/sites/";

    record Source(LevelDefinition definition, NativeInvestigationScene scene,
                  List<NativeInvestigationDoor.Spec> doors, int transitionCount) {}

    private SeacliffNativeLevel() {}

    static LevelDefinition definition() {
        return Packaged.SOURCE.definition();
    }

    static NativeInvestigationScene sceneFor(LevelDefinition definition) {
        var source = Packaged.SOURCE;
        if (!source.definition().equals(definition))
            throw new IllegalArgumentException("Seacliff structure requires its matching authored definition");
        return source.scene();
    }

    private static final class Packaged {
        private static final Source SOURCE = load();

        private static Source load() {
            try (var manifest = resource("seacliff-runtime.tsv");
                 var model = resource("seacliff-native.glb");
                 var collision = resource("seacliff-native.hcs2")) {
                var source = read(manifest, model, collision);
                if (source.transitionCount() == 0)
                    throw new IOException("Packaged Seacliff needs its surveyed stair and van transitions");
                return source;
            } catch (IOException failure) {
                throw new IllegalStateException("Cannot load the packaged Seacliff Battery", failure);
            }
        }
    }

    private static InputStream resource(String name) throws IOException {
        var stream = SeacliffNativeLevel.class.getResourceAsStream(ROOT + name);
        if (stream == null) throw new IOException("Missing Seacliff resource " + name);
        return stream;
    }

    static Source read(InputStream manifest, InputStream model, InputStream collision) throws IOException {
        try (var reader = new BufferedReader(new InputStreamReader(manifest, StandardCharsets.UTF_8))) {
            var rows = new ArrayList<String[]>();
            for (String line; (line = reader.readLine()) != null;)
                rows.add(line.split("\t", -1));
            return decode(rows, model, collision);
        } catch (IllegalArgumentException | IndexOutOfBoundsException | java.util.NoSuchElementException malformed) {
            throw new IOException("Invalid Seacliff runtime metadata", malformed);
        }
    }

    private static Source decode(List<String[]> rows, InputStream model, InputStream collision) throws IOException {
        if (rows.size() < 3 || !rows.get(0)[0].equals("SOURCE")
            || !rows.get(1)[0].equals("DOCK") || !rows.get(2)[0].equals("FRAME"))
            throw new IOException("Seacliff manifest needs source hashes, a dock and the director frame");
        var source=rows.get(0);
        var dock=rows.get(1);
        var f=rows.get(2);
        var portal=new Portal(Integer.parseInt(f[1]),0,1,d(f,3),d(f,4),d(f,5),d(f,6),true,"door",1);
        var door=new NativeInvestigationDoor.Spec(f[2],"gallery","director-stair",portal,d(f,7),d(f,8),d(f,9),
            2,-1,1,f[10]);
        var rooms=new Room[HouseMap.ROOM_COUNT];
        var spaces=new ArrayList<NativeInvestigationScene.Space>();
        var stairs=new HashSet<String>();
        var sockets=new ArrayList<Socket>();
        var fitted=new ArrayList<String[]>();
        var transitions=new ArrayList<Portal>();
        for(var row:rows.subList(3,rows.size()))switch(row[0]){
            case "SPACE" -> {
                var space=new NativeInvestigationScene.Space(row[1],Integer.parseInt(row[2]),d(row,3),d(row,4),d(row,5),
                    d(row,6),d(row,7),d(row,8),Boolean.parseBoolean(row[9]),true);
                spaces.add(space);
                if(row.length>10 && rooms[space.owner()]!=null)
                    throw new IOException("Duplicate Seacliff room anchor "+space.owner());
                if(row.length>10)rooms[space.owner()]=new Room(space.owner(),space.id(),space.x0(),space.z0(),space.x1(),
                    space.z1(),space.floor(),space.height(),space.outside()?"earth":"stone",
                    space.outside()?"exterior":"masonry",point(row,10));
            }
            case "RITUAL" -> sockets.add(new Socket("battery-ritual-"+row[1],"ritual",Integer.parseInt(row[2]),
                d(row,3),d(row,4),d(row,5),0,row[1]));
            case "TREAD" -> stairs.add(row[1]);
            case "SOCKET" -> sockets.add(new Socket(row[1],row[2],Integer.parseInt(row[3]),
                d(row,4),d(row,5),d(row,6),d(row,7),row[8]));
            case "BAY" -> fitted.add(row);
            case "TRANSITION" -> transitions.add(new Portal(Integer.parseInt(row[1]),Integer.parseInt(row[3]),
                Integer.parseInt(row[4]),d(row,5),d(row,6),d(row,7),d(row,8),row[9].equals("z"),row[10],1));
            // These plotted routes are replayed by the probe. They create no room, portal or physical surface.
            case "POINT", "LINK" -> {}
            default -> throw new IllegalArgumentException(row[0]);
        };
        var doors=new ArrayList<NativeInvestigationDoor.Spec>();
        doors.add(door);
        for(var row:fitted){
            int a=spaces.stream().filter(space->space.id().equals(row[3])).findFirst().orElseThrow().owner();
            int b=spaces.stream().filter(space->space.id().equals(row[4])).findFirst().orElseThrow().owner();
            var p=new Portal(Integer.parseInt(row[1]),a,b,d(row,5),d(row,6),d(row,7),d(row,8),
                row[9].equals("z"),"door",1);
            doors.add(new NativeInvestigationDoor.Spec(row[2],row[3],row[4],p,d(row,10),d(row,11),d(row,12),
                1,Integer.parseInt(row[13]),Integer.parseInt(row[14]),row[15]));
        }
        var scene=NativeInvestigationScene.read(model,source[2],collision,source[4],stairs,spaces,doors);
        var staging=new Dock(d(dock,1),d(dock,2),d(dock,3),d(dock,4));
        var vanDatum=staging.y()+VanStaging.FLOOR_Y;
        rooms[14]=new Room(14,"Van",staging.x()-1.25,staging.z()-2,staging.x()+1.25,staging.z()+3,
            vanDatum,VanStaging.INTERIOR_HEIGHT,"van","metal",new Vec(staging.x(),vanDatum,staging.z()));
        for(int owner=0;owner<HouseMap.EXTRACTION_ROOM;owner++){
            if(rooms[owner]==null)throw new IOException("Missing Seacliff room anchor "+owner);
            var spawn=rooms[owner].spawn();
            if(scene.ownerAt(spawn.x(),spawn.y(),spawn.z())!=owner
                || !scene.physics().traversable(spawn,HunterPosture.STANDING_HEIGHT))
                throw new IOException("Unsupported Seacliff room anchor "+rooms[owner].name());
        }
        var portals=new ArrayList<>(doors.stream().map(NativeInvestigationDoor.Spec::portal).toList());
        portals.addAll(transitions);
        sockets.add(new Socket("seacliff-scene-hash","native_scene",13,staging.x(),staging.y(),staging.z(),
            0,scene.revision()));
        var level=new LevelDefinition("wld-17","authored-battery-candidate",TITLE,List.of(rooms),portals,
            List.of(),List.of(),List.of(),sockets,List.of(),staging,"coast");
        return new Source(level,scene,List.copyOf(doors),transitions.size());
    }

    private static double d(String[] row, int index) {
        return Double.parseDouble(row[index]);
    }

    private static Vec point(String[] row, int start) {
        return new Vec(d(row,start),d(row,start+1),d(row,start+2));
    }
}
