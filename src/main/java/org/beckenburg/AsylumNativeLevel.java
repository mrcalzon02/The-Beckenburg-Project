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

/** Binds exact native Asylum geometry, physical thresholds and mounted services for review. */
final class AsylumNativeLevel {
    static final String REVIEW_PROPERTY="beckenburg.review.asylumNative";
    private static final String BINDING="asylum-scene-hash";

    record Source(LevelDefinition definition,NativeInvestigationScene scene,List<NativeInvestigationDoor.Spec> doors) {}

    private AsylumNativeLevel(){}

    static boolean reviewEnabled(){
        return Boolean.getBoolean(REVIEW_PROPERTY);
    }

    static LevelDefinition definition(){
        return Packaged.SOURCE.definition();
    }

    static NativeInvestigationScene sceneFor(LevelDefinition definition){
        var binding=definition.socket("native_scene",13);
        if(binding==null||!binding.id().equals(BINDING))return null;
        var packaged=Packaged.SOURCE;
        if(!packaged.definition().equals(definition))
            throw new IllegalArgumentException("Asylum geometry requires its matching authored definition");
        return packaged.scene();
    }

    private static final class Packaged {
        private static final Source SOURCE=load();

        private static Source load(){
            try(var arrival=resource("arrival.tsv");
                var schedule=resource("door-survey.tsv");
                var mission=resource("mission.tsv");
                var services=resource("services.tsv");
                var model=resource("spine.glb");
                var collision=resource("spine.hcs2")){
                var source=read(rows(arrival),rows(schedule),rows(mission),rows(services),model,collision);
                var level=source.definition();
                if(source.doors().size()!=13||level.portals().size()!=17||level.socket("breaker",11)==null
                        ||HidingBudget.candidates(level).size()!=3)
                    throw new IOException("Incomplete Asylum review packet");
                return source;
            }catch(IOException problem){
                throw new IllegalStateException("Cannot load the packaged Asylum review",problem);
            }
        }
    }

    private static InputStream resource(String name)throws IOException{
        var stream=AsylumNativeLevel.class.getResourceAsStream("/org/beckenburg/sites/asylum-"+name);
        if(stream==null)throw new IOException("Missing Asylum resource "+name);
        return stream;
    }

    private static List<String[]> rows(InputStream input)throws IOException{
        var reader=new BufferedReader(new InputStreamReader(input,StandardCharsets.UTF_8));
        var table=new ArrayList<String[]>();
        for(String line;(line=reader.readLine())!=null;)table.add(line.split("\t",-1));
        return table;
    }

    static Source read(List<String[]> rows,List<String[]> schedule,List<String[]> mission,List<String[]> services,
            InputStream model,InputStream collision)throws IOException{
        if(rows.size()<2||!rows.get(0)[0].equals("SOURCE")||!rows.get(1)[0].equals("DOCK"))
            throw new IOException("Asylum manifest needs source hashes and the canonical dock");
        var source=rows.get(0);
        var placement=rows.get(1);
        var dock=new Dock(d(placement,1),d(placement,2),d(placement,3),d(placement,4));
        var spaces=new ArrayList<NativeInvestigationScene.Space>();
        var stairs=new HashSet<String>();
        var sockets=new ArrayList<Socket>();
        for(var row:rows)switch(row[0]){
            case "SPACE" -> spaces.add(new NativeInvestigationScene.Space(row[1],Integer.parseInt(row[2]),
                d(row,3),d(row,4),d(row,5),d(row,6),d(row,7),d(row,8),Boolean.parseBoolean(row[9]),Boolean.parseBoolean(row[10])));
            case "RITUAL" -> sockets.add(new Socket("asylum-ritual-"+row[1],"ritual",Integer.parseInt(row[2]),
                d(row,3),d(row,4),d(row,5),0,row[1]));
            case "TREAD" -> stairs.add(row[1]);
            case "SOURCE","DOCK","POINT" -> {}
            default -> throw new IOException("Unknown Asylum circulation record "+row[0]);
        }
        var doors=new ArrayList<NativeInvestigationDoor.Spec>();
        for(var f:schedule){
            var portal=new Portal(Integer.parseInt(f[0]),Integer.parseInt(f[4]),Integer.parseInt(f[5]),
                d(f,6),d(f,7),d(f,8),d(f,9),f[10].equals("z"),"door",1);
            doors.add(new NativeInvestigationDoor.Spec(f[1],f[2],f[3],portal,d(f,11),d(f,12),d(f,13),
                Integer.parseInt(f[14]),Integer.parseInt(f[15]),Integer.parseInt(f[16]),f[17]));
        }
        var scene=NativeInvestigationScene.read(model,source[2],collision,source[4],stairs,spaces,doors);
        for(var row:services)
            sockets.add(new Socket(row[0],row[1],Integer.parseInt(row[2]),d(row,3),d(row,4),d(row,5),d(row,6),row[7]));
        var old=LevelPlans.buildSource(HouseMap.Site.ASYLUM);
        boolean connected=!mission.isEmpty();
        var portals=new ArrayList<>(doors.stream().map(NativeInvestigationDoor.Spec::portal).toList());
        var owners=connected?bind(scene,dock,mission,portals):old.rooms();
        if(connected)sockets.add(new Socket(BINDING,"native_scene",13,dock.x(),dock.y(),dock.z(),0,scene.revision()));
        var definition=new LevelDefinition("asylum",scene.revision(),old.title(),owners,
            portals,List.of(),List.of(),List.of(),sockets,List.of(),dock,old.ambience(),old.water(),old.sounds());
        return new Source(definition,scene,List.copyOf(doors));
    }

    private static double d(String[] row,int index){return Double.parseDouble(row[index]);}

    private static Vec point(String[] row,int x){
        return new Vec(Double.parseDouble(row[x]),Double.parseDouble(row[x+1]),Double.parseDouble(row[x+2]));
    }

    private static List<Room> bind(NativeInvestigationScene scene,Dock dock,List<String[]> records,List<Portal> portals)
            throws IOException{
        var owners=new Room[HouseMap.ROOM_COUNT];
        var physics=scene.physics();
        for(var row:records)switch(row[0]){
            case "ANCHOR" -> {
                int owner=Integer.parseInt(row[1]);
                var feet=point(row,3);
                var room=scene.spaceAt(feet.x(),feet.y(),feet.z());
                if(owners[owner]!=null||room==null||room.owner()!=owner||!room.id().equals(row[2])||!room.accessible()
                        ||!physics.traversable(feet,HunterPosture.STANDING_HEIGHT))
                    throw new IOException("Invalid Asylum anchor "+row[2]+" "+feet);
                owners[owner]=new Room(owner,room.id(),room.x0(),room.z0(),room.x1(),room.z1(),room.floor(),room.height(),
                    room.outside()?"ground":"stone",room.outside()?"outdoor":"masonry",feet);
            }
            case "TRANSITION" -> {
                var at=point(row,5);
                int a=Integer.parseInt(row[3]),b=Integer.parseInt(row[4]);
                boolean along=row[9].equals("z");
                var support=physics.support(at.x(),at.z(),at.y(),.055);
                if(support==null||Math.abs(support.y()-at.y())>.05)
                    throw new IOException("No existing support for Asylum transition "+row[2]+" "+at);
                if(row[10].equals("trail")){
                    double dx=along?0:.68,dz=along?.68:0;
                    int near=scene.ownerAt(at.x()-dx,at.y(),at.z()-dz);
                    int far=scene.ownerAt(at.x()+dx,at.y(),at.z()+dz);
                    if(!((a==near&&b==far)||(a==far&&b==near)))
                        throw new IOException("Asylum opening did not separate the named owners "+row[2]+" "+near+"/"+far);
                }
                portals.add(new Portal(Integer.parseInt(row[1]),a,b,at.x(),at.y(),at.z(),
                    Double.parseDouble(row[8]),along,row[10],1));
            }
            default -> throw new IOException("Unknown Asylum mission record "+row[0]);
        }
        var van=new VanStaging(new VanStaging.Placement(dock.x(),dock.y(),dock.z(),dock.yaw()));
        double y=dock.y()+VanStaging.FLOOR_Y;
        var lip=van.world(0,VanStaging.FRONT);
        portals.add(new Portal(486,13,14,lip.x(),y,lip.z(),VanStaging.RAMP_HALF_WIDTH*2,
            Math.abs(Math.sin(dock.yaw()))<.5,"trail",1));
        owners[14]=new Room(14,"Expedition van",dock.x()-4,dock.z()-1.3,dock.x()+4,dock.z()+1.3,
            y,VanStaging.INTERIOR_HEIGHT,"van","metal",new Vec(dock.x(),y,dock.z()));
        for(int id=0;id<owners.length;id++)if(owners[id]==null)throw new IOException("Missing Asylum owner "+id);
        return List.of(owners);
    }

}
