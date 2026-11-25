package org.beckenburg;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.beckenburg.LevelDefinition.Dock;
import org.beckenburg.LevelDefinition.Portal;
import org.beckenburg.LevelDefinition.Room;
import org.beckenburg.LevelDefinition.Socket;
import org.beckenburg.LevelDefinition.Vec;

/** Loads the surveyed Ravenshade review packet from the same source in the tool and the packaged game. */
final class SchoolNativeLevel {
    static final String REVIEW_PROPERTY = "beckenburg.review.schoolNative";
    private static final String BINDING = "school-scene-hash";

    record Source(LevelDefinition definition, NativeInvestigationScene scene,
                  List<NativeInvestigationDoor.Spec> doors) {}

    private SchoolNativeLevel() {}

    static boolean reviewEnabled() {
        return Boolean.getBoolean(REVIEW_PROPERTY);
    }

    static LevelDefinition definition() {
        return Packaged.SOURCE.definition();
    }

    static NativeInvestigationScene sceneFor(LevelDefinition definition) {
        var binding=definition.socket("native_scene",13);
        if(binding==null||!binding.id().equals(BINDING))return null;
        var source=Packaged.SOURCE;
        if(!source.definition().equals(definition))
            throw new IllegalArgumentException("Ravenshade structure requires its matching authored definition");
        return source.scene();
    }

    private static final class Packaged {
        private static final Source SOURCE=load();

        private static Source load() {
            try(var circulation=resource("circulation.tsv");
                var doors=resource("door-schedule.tsv");
                var mission=resource("mission.tsv");
                var model=resource("electrical.glb");
                var collision=resource("electrical.hcs2")){
                var source=decode(rows(circulation),rows(doors),rows(mission),model,collision);
                require(source.doors().size()==20&&source.definition().portals().size()==24,
                    "Packaged Ravenshade is missing surveyed doors or transitions");
                return source;
            }catch(IOException failure){
                throw new IllegalStateException("Cannot load the packaged Ravenshade review",failure);
            }
        }
    }

    private static InputStream resource(String name)throws IOException {
        var data=SchoolNativeLevel.class.getResourceAsStream("/org/beckenburg/sites/school-"+name);
        if(data==null)throw new IOException("Missing Ravenshade resource "+name);
        return data;
    }

    private static List<String[]> rows(InputStream stream)throws IOException {
        var reader=new BufferedReader(new InputStreamReader(stream,StandardCharsets.UTF_8));
        var rows=new ArrayList<String[]>();
        for(String line;(line=reader.readLine())!=null;)rows.add(line.split("\t",-1));
        return rows;
    }

    static Source read(List<String[]> rows,List<String[]> schedule,List<String[]> mission)throws IOException {
        var source=rows.get(0);
        try(var model=Files.newInputStream(Path.of(source[1]));var collision=Files.newInputStream(Path.of(source[3]))){
            return decode(rows,schedule,mission,model,collision);
        }
    }

    private static Source decode(List<String[]> rows,List<String[]> schedule,List<String[]> mission,
            InputStream model,InputStream collision)throws IOException {
        require(rows.size()>=2&&rows.get(0)[0].equals("SOURCE")&&rows.get(1)[0].equals("DOCK"),
            "Ravenshade manifest needs source hashes and a dock");
        var source=rows.get(0);
        var rooms=new ArrayList<NativeInvestigationScene.Space>();
        var stairs=new HashSet<String>();
        for(var r:rows.subList(1,rows.size()))switch(r[0]){
            case "SPACE" -> rooms.add(new NativeInvestigationScene.Space(r[1],Integer.parseInt(r[2]),
                d(r,3),d(r,4),d(r,5),d(r,6),d(r,7),d(r,8),Boolean.parseBoolean(r[9]),Boolean.parseBoolean(r[10])));
            case "TREAD" -> stairs.add(r[1]);
            case "DOCK", "POINT", "LINK", "TRANSIT" -> {}
            default -> throw new IOException("Unknown Ravenshade circulation row "+r[0]);
        }
        var specs=new ArrayList<NativeInvestigationDoor.Spec>();
        var sockets=new ArrayList<Socket>();
        for(var f:schedule){
            if(f[0].equals("SOCKET")){
                sockets.add(new Socket(f[1],f[2],Integer.parseInt(f[3]),d(f,4),d(f,5),d(f,6),d(f,7),f[8]));
                continue;
            }
            if(f[0].equals("RITUAL")){
                sockets.add(new Socket("school-ritual-"+f[1],"ritual",Integer.parseInt(f[2]),d(f,3),d(f,4),d(f,5),0,f[1]));
                continue;
            }
            var p=new Portal(Integer.parseInt(f[0]),Integer.parseInt(f[4]),Integer.parseInt(f[5]),
                d(f,6),d(f,7),d(f,8),d(f,9),f[10].equals("z"),"door",1);
            specs.add(new NativeInvestigationDoor.Spec(f[1],f[2],f[3],p,d(f,11),d(f,12),d(f,13),
                Integer.parseInt(f[14]),Integer.parseInt(f[15]),Integer.parseInt(f[16]),f[17]));
        }
        var scene=NativeInvestigationScene.read(model,source[2],collision,source[4],stairs,rooms,specs);
        var dock=rows.get(1);
        var placement=new Dock(d(dock,1),d(dock,2),d(dock,3),d(dock,4));
        var legacy=LevelPlans.buildSource(HouseMap.Site.SCHOOL);
        var portals=new ArrayList<>(specs.stream().map(NativeInvestigationDoor.Spec::portal).toList());
        var anchors=mission.isEmpty()?legacy.rooms():missionRooms(mission,scene,placement,portals);
        sockets.add(new Socket(BINDING,"native_scene",13,placement.x(),placement.y(),placement.z(),0,scene.revision()));
        var definition=new LevelDefinition("school",scene.revision(),legacy.title(),anchors,portals,
            List.of(),List.of(),List.of(),sockets,List.of(),placement,legacy.ambience(),legacy.water(),legacy.sounds());
        return new Source(definition,scene,List.copyOf(specs));
    }

    private static double d(String[] row,int index) {
        return Double.parseDouble(row[index]);
    }

    private static void require(boolean condition,String message)throws IOException {
        if(!condition)throw new IOException(message);
    }

    private static List<Room> missionRooms(List<String[]> records, NativeInvestigationScene scene,
            Dock dock, List<Portal> portals) throws IOException {
        var anchors=new Room[HouseMap.ROOM_COUNT];
        for(var row:records) {
            if(row[0].equals("TRANSITION")){
                var portal=new Portal(Integer.parseInt(row[1]),Integer.parseInt(row[3]),Integer.parseInt(row[4]),
                    d(row,5),d(row,6),d(row,7),d(row,8),row[9].equals("z"),row[10],1);
                var support=scene.physics().support(portal.x(),portal.z(),portal.y(),.03);
                require(support!=null,"No physical floor or tread under mission transition "+row[2]);
                portals.add(portal);
                continue;
            }
            require(row[0].equals("ANCHOR"),"Unknown School mission row "+row[0]);
            int id=Integer.parseInt(row[1]);
            var feet=new Vec(d(row,3),d(row,4),d(row,5));
            var room=scene.spaceAt(feet.x(),feet.y(),feet.z());
            require(anchors[id]==null&&room!=null&&room.id().equals(row[2])&&room.owner()==id&&room.accessible()
                &&scene.physics().traversable(feet,HunterPosture.STANDING_HEIGHT),"Bad School anchor "+row[2]);
            anchors[id]=new Room(id,row[2],room.x0(),room.z0(),room.x1(),room.z1(),room.floor(),room.height(),
                room.outside()?"ground":"stone",room.outside()?"outdoor":"masonry",feet);
        }
        double vanY=dock.y()+VanStaging.FLOOR_Y;
        var van=new VanStaging(new VanStaging.Placement(dock.x(),dock.y(),dock.z(),dock.yaw()));
        var lip=van.world(0,VanStaging.FRONT);
        portals.add(new Portal(424,13,14,lip.x(),vanY,lip.z(),VanStaging.RAMP_HALF_WIDTH*2,
            Math.abs(Math.sin(dock.yaw()))<.5,"trail",1));
        anchors[14]=new Room(14,"Expedition van",dock.x()-4,dock.z()-1.3,dock.x()+4,dock.z()+1.3,
            vanY,VanStaging.INTERIOR_HEIGHT,"van","metal",new Vec(dock.x(),vanY,dock.z()));
        for(int id=0;id<anchors.length;id++)
            require(anchors[id]!=null,"Missing School room anchor "+id);
        return List.of(anchors);
    }

}
