package org.beckenburg;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import dev.mrcalzon.water.WaterDocument;
import org.beckenburg.LevelDefinition.Dock;
import org.beckenburg.LevelDefinition.Portal;
import org.beckenburg.LevelDefinition.Room;
import org.beckenburg.LevelDefinition.Socket;
import org.beckenburg.LevelDefinition.Vec;

/** Authored Dead Man's Claim package, shared by ordinary selection and native case review. */
final class MineNativeLevel {
    static final String REVIEW_PROPERTY="beckenburg.review.mineNative";
    record Source(LevelDefinition level,NativeInvestigationScene scene,List<NativeInvestigationDoor.Spec> doors){}

    private MineNativeLevel(){}

    static LevelDefinition definition(){return Packaged.SOURCE.level();}

    static NativeInvestigationScene sceneFor(LevelDefinition level){
        var source=Packaged.SOURCE;
        if(!source.level().equals(level))throw new IllegalArgumentException("Mine scene needs its exact authored case");
        return source.scene();
    }

    private static final class Packaged {
        private static final Source SOURCE=load();

        private static Source load(){
            try(var manifest=resource("arrival.tsv");var schedule=resource("door-survey.tsv");
                var mission=resource("mission.tsv");var model=resource("case.glb");var nativeModel=resource("case.hcs2");
                var water=resource("sump.water")){
                var source=read(rows(manifest),rows(schedule),rows(mission),model,nativeModel,WaterDocument.read(water));
                var level=source.level();
                require(source.doors().size()==8&&level.portals().size()==18
                    &&level.sockets().stream().filter(s->s.type().equals("switch")).count()==12
                    &&level.sockets().stream().filter(s->s.type().equals("light")).count()==13
                    &&level.socket("generator",13)!=null&&level.socket("breaker",13)!=null
                    &&HidingBudget.candidates(level).size()==3&&level.props().size()==2
                    &&level.water().placements().size()==1,"Incomplete authored Mine case");
                return source;
            }catch(IOException problem){throw new IllegalStateException("Cannot load Dead Man's Claim",problem);}
        }
    }

    private static InputStream resource(String name)throws IOException{
        var stream=MineNativeLevel.class.getResourceAsStream("/org/beckenburg/sites/mine-"+name);
        if(stream==null)throw new IOException("Missing Mine resource "+name);
        return stream;
    }

    private static List<String[]> rows(InputStream source)throws IOException{
        var reader=new BufferedReader(new InputStreamReader(source,StandardCharsets.UTF_8));
        var rows=new ArrayList<String[]>();
        for(String line;(line=reader.readLine())!=null;)rows.add(line.split("\\t",-1));
        return rows;
    }

    static Source read(List<String[]> arrival,List<String[]> schedule,List<String[]> mission,
            InputStream model,InputStream physics,WaterDocument water)throws IOException{
        require(arrival.size()>=2&&arrival.get(0)[0].equals("SOURCE")&&arrival.get(1)[0].equals("DOCK"),
            "Mine manifest requires source hashes and an authored dock");
        var source=arrival.get(0);
        var staging=arrival.get(1);
        var dock=new Dock(d(staging,1),d(staging,2),d(staging,3),d(staging,4));
        var spaces=new ArrayList<NativeInvestigationScene.Space>();
        var treads=new HashSet<String>();
        var sockets=new ArrayList<Socket>();
        var props=new ArrayList<LevelDefinition.Prop>();
        for(var row:arrival)switch(row[0]){
            case "SPACE" -> spaces.add(new NativeInvestigationScene.Space(row[1],Integer.parseInt(row[2]),
                d(row,3),d(row,4),d(row,5),d(row,6),d(row,7),d(row,8),
                Boolean.parseBoolean(row[9]),Boolean.parseBoolean(row[10])));
            case "RITUAL" -> sockets.add(new Socket("mine-"+row[1],"ritual",Integer.parseInt(row[2]),
                d(row,3),d(row,4),d(row,5),0,row[1]));
            case "TREAD" -> treads.add(row[1]);
            case "COVER" -> sockets.add(new Socket(row[1],"hide",Integer.parseInt(row[2]),
                d(row,3),d(row,4),d(row,5),d(row,6),"walk-in-cupboard"));
            case "SERVICE" -> sockets.add(new Socket(row[1],row[2],Integer.parseInt(row[3]),
                d(row,4),d(row,5),d(row,6),d(row,7),row[8]));
            case "STORY" -> {
                int owner=Integer.parseInt(row[3]);
                var at=point(row,5);
                props.add(new LevelDefinition.Prop(row[4],owner,at.x(),at.y(),at.z(),d(row,9),d(row,8),0,0,.05,"bench"));
                sockets.add(new Socket(row[1],"story",owner,at.x(),at.y(),at.z(),d(row,8),row[2]));
            }
            case "SOURCE","DOCK","POINT" -> {}
            default -> throw new IOException("Unknown Mine field "+row[0]);
        }
        var doors=new ArrayList<NativeInvestigationDoor.Spec>();
        for(var row:schedule){
            var portal=new Portal(Integer.parseInt(row[0]),Integer.parseInt(row[4]),Integer.parseInt(row[5]),
                d(row,6),d(row,7),d(row,8),d(row,9),row[10].equals("z"),"door",1);
            doors.add(new NativeInvestigationDoor.Spec(row[1],row[2],row[3],portal,d(row,11),d(row,12),d(row,13),
                Integer.parseInt(row[14]),Integer.parseInt(row[15]),Integer.parseInt(row[16]),row[17]));
        }
        var scene=NativeInvestigationScene.read(model,source[2],physics,source[4],treads,spaces,doors);
        var portals=new ArrayList<>(doors.stream().map(NativeInvestigationDoor.Spec::portal).toList());
        var rooms=bind(scene,dock,mission,portals);
        var level=new LevelDefinition("wld-05",scene.revision(),"Dead Man's Claim",rooms,portals,
            List.of(),List.of(),props,sockets,List.of(),dock,"industrial",water);
        return new Source(level,scene,List.copyOf(doors));
    }

    private static List<Room> bind(NativeInvestigationScene scene,Dock dock,List<String[]> lines,List<Portal> portals)throws IOException{
        var rooms=new Room[HouseMap.ROOM_COUNT];
        for(var row:lines)switch(row[0]){
            case "ANCHOR" -> {
                int id=Integer.parseInt(row[1]);
                var at=point(row,3);
                var zone=scene.spaceAt(at.x(),at.y(),at.z());
                require(zone!=null&&zone.owner()==id&&zone.id().equals(row[2])
                    &&scene.physics().traversable(at,HunterPosture.STANDING_HEIGHT),"Unsupported Mine owner "+row[2]+" "+at+" / "+zone);
                rooms[id]=new Room(id,zone.id(),zone.x0(),zone.z0(),zone.x1(),zone.z1(),zone.floor(),zone.height(),
                    zone.outside()?"native-outdoor":"concrete",zone.outside()?"outdoor":"mine",at);
            }
            case "TRANSITION" -> {
                var at=point(row,4);
                var support=scene.physics().support(at.x(),at.z(),at.y(),.08);
                require(support!=null&&Math.abs(support.y()-at.y())<.07,"Unsupported Mine transition "+row[1]);
                int a=Integer.parseInt(row[2]),b=Integer.parseInt(row[3]);
                boolean along=row[8].equals("z");
                if(!row[9].equals("stair")){
                    double dx=along?0:.65,dz=along?.65:0;
                    int near=scene.ownerAt(at.x()-dx,at.y(),at.z()-dz),far=scene.ownerAt(at.x()+dx,at.y(),at.z()+dz);
                    require(a==near&&b==far||a==far&&b==near,"Mine transition "+row[1]+" touches "+near+" and "+far);
                    require(scene.physics().clearSight(new Vec(at.x()-dx,at.y()+1.2,at.z()-dz),
                        new Vec(at.x()+dx,at.y()+1.2,at.z()+dz)),"Blocked Mine opening "+row[1]);
                }
                portals.add(new Portal(Integer.parseInt(row[1]),a,b,at.x(),at.y(),at.z(),d(row,7),along,row[9],1.0));
            }
            default -> throw new IllegalArgumentException("Unknown Mine mission record "+row[0]);
        }
        var van=new VanStaging(new VanStaging.Placement(dock.x(),dock.y(),dock.z(),dock.yaw()));
        var lip=van.world(0,VanStaging.FRONT);
        double y=van.floorY();
        portals.add(new Portal(719,13,14,lip.x(),y,lip.z(),VanStaging.RAMP_HALF_WIDTH*2,true,"trail",1));
        rooms[14]=new Room(14,"Expedition van",dock.x()-1.26,dock.z()-3,dock.x()+1.26,dock.z()+2.4,
            y,VanStaging.INTERIOR_HEIGHT,"van","metal",new Vec(dock.x(),y,dock.z()));
        return List.of(rooms);
    }

    private static void require(boolean valid,String message)throws IOException{
        if(!valid)throw new IOException(message);
    }

    private static double d(String[] row,int i){return Double.parseDouble(row[i]);}
    private static Vec point(String[] row,int i){return new Vec(d(row,i),d(row,i+1),d(row,i+2));}
}
