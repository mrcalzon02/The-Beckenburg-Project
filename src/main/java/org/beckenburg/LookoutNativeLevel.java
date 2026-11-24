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

/** Exact authored Lookout source selected by the ordinary gameplay entry points. */
final class LookoutNativeLevel {
    static final String REVIEW_PROPERTY="beckenburg.review.lookoutNative";
    record Source(LevelDefinition level,NativeInvestigationScene scene,List<NativeInvestigationDoor.Spec> doors){}

    private LookoutNativeLevel(){}

    static LevelDefinition definition(){return Packaged.SOURCE.level();}

    static NativeInvestigationScene sceneFor(LevelDefinition level){
        var source=Packaged.SOURCE;
        if(!source.level().equals(level))throw new IllegalArgumentException("Lookout geometry requires its matching authored definition");
        return source.scene();
    }

    private static final class Packaged {
        private static final Source SOURCE=load();

        private static Source load(){
            try(var arrival=resource("arrival.tsv");var mission=resource("mission.tsv");
                var schedule=resource("door-survey.tsv");var model=resource("power.glb");
                var collision=resource("power.hcs2")){
                var source=read(rows(arrival),rows(schedule),rows(mission),model,collision);
                var level=source.level();
                if(source.doors().size()!=7||level.portals().size()!=16
                        ||level.socket("breaker",13)==null||level.socket("generator",13)==null
                        ||HidingBudget.candidates(level).size()!=3)
                    throw new IOException("Incomplete powered Lookout packet");
                return source;
            }catch(IOException problem){
                throw new IllegalStateException("Cannot load Widow's Peak Fire Lookout",problem);
            }
        }
    }

    private static InputStream resource(String name)throws IOException{
        var stream=LookoutNativeLevel.class.getResourceAsStream("/org/beckenburg/sites/lookout-"+name);
        if(stream==null)throw new IOException("Missing Lookout resource "+name);
        return stream;
    }

    private static List<String[]> rows(InputStream source)throws IOException{
        var reader=new BufferedReader(new InputStreamReader(source,StandardCharsets.UTF_8));
        var rows=new ArrayList<String[]>();
        for(String line;(line=reader.readLine())!=null;)rows.add(line.split("\t",-1));
        return rows;
    }

    static Source read(List<String[]> arrival,List<String[]> schedule,List<String[]> mission,
            InputStream model,InputStream collision)throws IOException{
        if(arrival.size()<2||!arrival.get(0)[0].equals("SOURCE")||!arrival.get(1)[0].equals("DOCK"))
            throw new IOException("Lookout manifest needs source hashes and the canonical dock");
        var source=arrival.get(0);
        var vanRow=arrival.get(1);
        var dock=new Dock(d(vanRow,1),d(vanRow,2),d(vanRow,3),d(vanRow,4));
        var spaces=new ArrayList<NativeInvestigationScene.Space>();
        var treads=new HashSet<String>();
        var sockets=new ArrayList<Socket>();
        for(var row:arrival)switch(row[0]){
            case "SPACE" -> spaces.add(new NativeInvestigationScene.Space(row[1],Integer.parseInt(row[2]),
                d(row,3),d(row,4),d(row,5),d(row,6),d(row,7),d(row,8),
                Boolean.parseBoolean(row[9]),Boolean.parseBoolean(row[10])));
            case "TREAD" -> treads.add(row[1]);
            case "RITUAL" -> sockets.add(new Socket("lookout-ritual-"+row[1],"ritual",Integer.parseInt(row[2]),
                d(row,3),d(row,4),d(row,5),0,row[1]));
            case "SERVICE" -> sockets.add(new Socket(row[1],row[2],Integer.parseInt(row[3]),
                d(row,4),d(row,5),d(row,6),d(row,7),row[8]));
            case "COVER" -> sockets.add(new Socket(row[1],"hide",Integer.parseInt(row[2]),
                d(row,3),d(row,4),d(row,5),d(row,6),"walk-in-cupboard"));
            case "SOURCE","DOCK","POINT" -> {}
            default -> throw new IOException("Unknown Lookout arrival record "+row[0]);
        }
        var frames=new ArrayList<NativeInvestigationDoor.Spec>();
        for(var row:schedule){
            var portal=new Portal(Integer.parseInt(row[0]),Integer.parseInt(row[4]),Integer.parseInt(row[5]),
                d(row,6),d(row,7),d(row,8),d(row,9),row[10].equals("z"),"door",1);
            frames.add(new NativeInvestigationDoor.Spec(row[1],row[2],row[3],portal,d(row,11),d(row,12),d(row,13),
                Integer.parseInt(row[14]),Integer.parseInt(row[15]),Integer.parseInt(row[16]),row[17]));
        }
        var scene=NativeInvestigationScene.read(model,source[2],collision,source[4],treads,spaces,frames);
        var portals=new ArrayList<>(frames.stream().map(NativeInvestigationDoor.Spec::portal).toList());
        var rooms=owners(scene,dock,mission,portals);
        sockets.add(new Socket("lookout-scene-hash","native_scene",13,dock.x(),dock.y(),dock.z(),0,scene.revision()));
        var level=new LevelDefinition("wld-04",scene.revision(),MissionSites.title("LOOKOUT"),rooms,portals,
            List.of(),List.of(),List.of(),sockets,List.of(),dock,"woodland");
        return new Source(level,scene,List.copyOf(frames));
    }

    private static List<Room> owners(NativeInvestigationScene scene,Dock dock,List<String[]> rows,List<Portal> portals)
            throws IOException{
        var result=new Room[HouseMap.ROOM_COUNT];
        for(var row:rows)switch(row[0]){
            case "ANCHOR" -> {
                int id=Integer.parseInt(row[1]);
                var at=point(row,3);
                var space=scene.spaceAt(at.x(),at.y(),at.z());
                if(space==null||space.owner()!=id||!space.id().equals(row[2])
                        ||!scene.physics().traversable(at,HunterPosture.STANDING_HEIGHT))
                    throw new IOException("Unsupported Lookout owner "+row[2]+" at "+at+" / "+space);
                result[id]=new Room(id,space.id(),space.x0(),space.z0(),space.x1(),space.z1(),space.floor(),space.height(),
                    space.outside()?"native-outdoor":"wood",space.outside()?"outdoor":"timber",at);
            }
            case "TRANSITION" -> {
                var at=point(row,4);
                var floor=scene.physics().support(at.x(),at.z(),at.y(),.07);
                if(floor==null||Math.abs(floor.y()-at.y())>.06)
                    throw new IOException("Unsupported Lookout transition "+row[1]+" "+at);
                int a=Integer.parseInt(row[2]),b=Integer.parseInt(row[3]);
                boolean along=row[8].equals("z");
                if(row[9].equals("trail")){
                    double dx=along?0:.68,dz=along?.68:0;
                    int near=scene.ownerAt(at.x()-dx,at.y(),at.z()-dz);
                    int far=scene.ownerAt(at.x()+dx,at.y(),at.z()+dz);
                    if(!(a==near&&b==far||a==far&&b==near))
                        throw new IOException("No Lookout owner interface at "+row[1]+": "+near+" and "+far);
                }
                portals.add(new Portal(Integer.parseInt(row[1]),a,b,at.x(),at.y(),at.z(),d(row,7),along,row[9],1));
            }
            default -> throw new IOException("Unknown Lookout mission record "+row[0]);
        }
        var van=new VanStaging(new VanStaging.Placement(dock.x(),dock.y(),dock.z(),dock.yaw()));
        var lip=van.world(0,VanStaging.FRONT);
        double y=van.floorY();
        portals.add(new Portal(619,13,14,lip.x(),y,lip.z(),VanStaging.RAMP_HALF_WIDTH*2,true,"trail",1));
        result[14]=new Room(14,"Expedition van",dock.x()-1.26,dock.z()-3,dock.x()+1.26,dock.z()+2.4,
            y,VanStaging.INTERIOR_HEIGHT,"van","metal",new Vec(dock.x(),y,dock.z()));
        return List.of(result);
    }

    private static double d(String[] row,int column){return Double.parseDouble(row[column]);}
    private static Vec point(String[] row,int column){return new Vec(d(row,column),d(row,column+1),d(row,column+2));}
}
