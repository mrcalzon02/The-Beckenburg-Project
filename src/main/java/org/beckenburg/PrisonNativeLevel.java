package org.beckenburg;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.beckenburg.LevelDefinition.Dock;
import org.beckenburg.LevelDefinition.Portal;
import org.beckenburg.LevelDefinition.Room;
import org.beckenburg.LevelDefinition.Socket;
import org.beckenburg.LevelDefinition.Vec;

/** Binds the surveyed Prison case to its exact structural, mechanical and service exports. */
final class PrisonNativeLevel {
    static final String REVIEW_PROPERTY="beckenburg.review.prisonNative";
    private static final String BINDING="prison-scene-hash";

    record Source(LevelDefinition definition, NativeInvestigationScene scene,
                  List<NativeInvestigationDoor.Spec> swingingDoors) {}

    record GateSource(List<String[]> rows,InputStream panel) {}

    private PrisonNativeLevel(){}

    static boolean reviewEnabled(){
        return Boolean.getBoolean(REVIEW_PROPERTY);
    }

    static LevelDefinition definition(){
        return Packaged.SOURCE.definition();
    }

    static NativeInvestigationScene sceneFor(LevelDefinition definition){
        var binding=definition.socket("native_scene",13);
        if(binding==null||!binding.id().equals(BINDING))return null;
        var source=Packaged.SOURCE;
        if(!source.definition().equals(definition))
            throw new IllegalArgumentException("Prison geometry requires its matching authored definition");
        return source.scene();
    }

    private static final class Packaged {
        private static final Source SOURCE=load();

        private static Source load(){
            try(var arrival=resource("arrival.tsv");
                var doors=resource("door-survey.tsv");
                var mission=resource("mission.tsv");
                var gate=resource("gate.tsv");
                var vehicle=resource("vehicle-gate.tsv");
                var services=resource("services.tsv");
                var model=resource("vehicle-sally.glb");
                var collision=resource("vehicle-sally.hcs2");
                var leaf=resource("sally-moving-panel.glb");
                var vehicleLeaf=resource("vehicle-moving-panel.glb")){
                var gates=List.of(new GateSource(rows(gate),leaf),new GateSource(rows(vehicle),vehicleLeaf));
                var source=read(rows(arrival),rows(doors),rows(mission),gates,rows(services),model,collision);
                var level=source.definition();
                if(source.scene().doors().size()!=4||level.portals().size()!=15
                    ||level.socket("breaker",11)==null||HidingBudget.candidates(level).size()!=3)
                    throw new IOException("Incomplete Prison review packet");
                return source;
            }catch(IOException failure){
                throw new IllegalStateException("Cannot load the packaged Prison review",failure);
            }
        }
    }

    private static InputStream resource(String name)throws IOException{
        var data=PrisonNativeLevel.class.getResourceAsStream("/org/beckenburg/sites/prison-"+name);
        if(data==null)throw new IOException("Missing Prison resource "+name);
        return data;
    }

    private static List<String[]> rows(InputStream input)throws IOException{
        var reader=new BufferedReader(new InputStreamReader(input,StandardCharsets.UTF_8));
        var rows=new ArrayList<String[]>();
        for(String line;(line=reader.readLine())!=null;)rows.add(line.split("\t",-1));
        return rows;
    }

    static Source read(List<String[]> rows,List<String[]> schedule,List<String[]> records,List<GateSource> gates,
            List<String[]> services,InputStream model,InputStream collision)throws IOException{
        if(rows.size()<2||!rows.get(0)[0].equals("SOURCE")||!rows.get(1)[0].equals("DOCK"))
            throw new IOException("Prison manifest needs source hashes and a dock");
        var source=rows.get(0);
        var staging=rows.get(1);
        var dock=new Dock(d(staging,1),d(staging,2),d(staging,3),d(staging,4));
        var spaces=new ArrayList<NativeInvestigationScene.Space>();
        var sockets=new ArrayList<Socket>();
        for(var row:rows)switch(row[0]){
            case "SPACE" -> spaces.add(new NativeInvestigationScene.Space(row[1],Integer.parseInt(row[2]),d(row,3),
                d(row,4),d(row,5),d(row,6),d(row,7),d(row,8),Boolean.parseBoolean(row[9]),Boolean.parseBoolean(row[10])));
            case "RITUAL" -> sockets.add(new Socket("prison-ritual-"+row[1],"ritual",Integer.parseInt(row[2]),
                d(row,3),d(row,4),d(row,5),0,row[1]));
            case "SOURCE", "DOCK", "POINT" -> {}
            default -> throw new IOException("Unknown Prison circulation row "+row[0]);
        }
        var specs=new ArrayList<NativeInvestigationDoor.Spec>();
        for(var f:schedule){
            var portal=new Portal(Integer.parseInt(f[0]),Integer.parseInt(f[4]),Integer.parseInt(f[5]),d(f,6),d(f,7),d(f,8),
                d(f,9),f[10].equals("z"),"door",1);
            specs.add(new NativeInvestigationDoor.Spec(f[1],f[2],f[3],portal,d(f,11),d(f,12),d(f,13),
                Integer.parseInt(f[14]),Integer.parseInt(f[15]),Integer.parseInt(f[16]),f[17]));
        }
        var stairParts=records.stream().filter(r->r[0].equals("TREAD")).map(r->r[1])
            .collect(java.util.stream.Collectors.toSet());
        var scene=NativeInvestigationScene.read(model,source[2],collision,source[4],stairParts,spaces,specs);
        for(var gate:gates){
            var gateRows=gate.rows();
            if(gateRows.size()!=3||gate.panel()==null)
                throw new IOException("A Prison gate needs its moving leaf and two releases");
            var f=gateRows.get(0);
            var portal=new Portal(Integer.parseInt(f[0]),Integer.parseInt(f[4]),Integer.parseInt(f[5]),
                d(f,6),d(f,7),d(f,8),d(f,9),f[10].equals("z"),"door",1);
            var spec=new NativeInvestigationDoor.Spec(f[1],f[2],f[3],portal,d(f,11),d(f,12),d(f,13),1,1,1,f[15]);
            scene=scene.withSlidingDoor(spec,gate.panel(),d(f,16));
            var owners=new java.util.HashSet<Integer>();
            for(var r:gateRows.subList(1,gateRows.size())){
                int owner=Integer.parseInt(r[1]);
                if(!r[6].equals(f[0])||(owner!=portal.a()&&owner!=portal.b())||!owners.add(owner))
                    throw new IOException("Invalid release ownership for "+spec.frameId());
                sockets.add(new Socket(r[0],"gate_release",owner,d(r,2),d(r,3),d(r,4),d(r,5),r[6]));
            }
        }
        for(var r:services)
            sockets.add(new Socket(r[0],r[1],Integer.parseInt(r[2]),d(r,3),d(r,4),d(r,5),d(r,6),r[7]));
        var existing=LevelPlans.buildSource(HouseMap.Site.PRISON);
        var portals=new ArrayList<>(scene.doors().stream().map(door->door.spec().portal()).toList());
        var anchors=records.isEmpty()?existing.rooms():bind(scene,dock,records,portals);
        if(!records.isEmpty())sockets.add(new Socket(BINDING,"native_scene",13,dock.x(),dock.y(),dock.z(),0,scene.revision()));
        var definition=new LevelDefinition("prison",scene.revision(),existing.title(),anchors,
            portals,List.of(),List.of(),List.of(),
            sockets,List.of(),dock,existing.ambience(),existing.water(),existing.sounds());
        return new Source(definition,scene,List.copyOf(specs));
    }

    private static double d(String[] row,int i){
        return Double.parseDouble(row[i]);
    }

    private static Vec xyz(String[] row,int start){
        return new Vec(d(row,start),d(row,start+1),d(row,start+2));
    }

    private static List<Room> bind(NativeInvestigationScene scene,Dock dock,List<String[]> records,List<Portal> portals)throws IOException{
        var rooms=new Room[HouseMap.ROOM_COUNT];
        var physics=scene.physics();
        for(var row:records)switch(row[0]){
            case "ANCHOR" -> {
                int owner=Integer.parseInt(row[1]);
                var feet=xyz(row,3);
                var room=scene.spaceAt(feet.x(),feet.y(),feet.z());
                if(rooms[owner]!=null||room==null||room.owner()!=owner||!room.id().equals(row[2])||!room.accessible()
                    ||!physics.traversable(feet,HunterPosture.STANDING_HEIGHT))
                    throw new IOException("Invalid Prison anchor "+row[2]+" "+feet);
                rooms[owner]=new Room(owner,room.id(),room.x0(),room.z0(),room.x1(),room.z1(),room.floor(),room.height(),
                    room.outside()?"ground":"stone",room.outside()?"outdoor":"masonry",feet);
            }
            case "TRANSITION" -> {
                var at=xyz(row,5);
                var support=physics.support(at.x(),at.z(),at.y(),.06);
                if(support==null||Math.abs(support.y()-at.y())>.05)
                    throw new IOException("No existing support for Prison transition "+row[2]+" "+at);
                portals.add(new Portal(Integer.parseInt(row[1]),Integer.parseInt(row[3]),Integer.parseInt(row[4]),
                    at.x(),at.y(),at.z(),Double.parseDouble(row[8]),row[9].equals("z"),row[10],1));
            }
            default -> {}
        }
        var van=new VanStaging(new VanStaging.Placement(dock.x(),dock.y(),dock.z(),dock.yaw()));
        double y=dock.y()+VanStaging.FLOOR_Y;
        var lip=van.world(0,VanStaging.FRONT);
        portals.add(new Portal(464,13,14,lip.x(),y,lip.z(),VanStaging.RAMP_HALF_WIDTH*2,
            Math.abs(Math.sin(dock.yaw()))<.5,"trail",1));
        rooms[14]=new Room(14,"Expedition van",dock.x()-4,dock.z()-1.3,dock.x()+4,dock.z()+1.3,
            y,VanStaging.INTERIOR_HEIGHT,"van","metal",new Vec(dock.x(),y,dock.z()));
        for(int id=0;id<rooms.length;id++)if(rooms[id]==null)throw new IOException("Missing Prison owner "+id);
        return List.of(rooms);
    }

}
