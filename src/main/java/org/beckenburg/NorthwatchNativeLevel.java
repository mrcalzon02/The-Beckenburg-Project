package org.beckenburg;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.beckenburg.LevelDefinition.Dock;
import org.beckenburg.LevelDefinition.Portal;
import org.beckenburg.LevelDefinition.Prop;
import org.beckenburg.LevelDefinition.Room;
import org.beckenburg.LevelDefinition.Socket;
import org.beckenburg.LevelDefinition.Vec;

final class NorthwatchNativeLevel {
    static final String TITLE="Northwatch Research Station";
    static final String REVIEW_PROPERTY="beckenburg.review.northwatchNative";
    private static final String ID="wld-08";
    private static final String DIRECTORY="/org/beckenburg/sites/"+ID+"/";

    record Story(String id,int owner,Vec feet,Vec contact) {}
    record Source(LevelDefinition level,NativeInvestigationScene scene,Map<String,List<Vec>> routes,List<Story> stories) {}
    private NorthwatchNativeLevel() {}
    private static final class Loaded {
        static final Source SOURCE=load();
    }
    static Source packaged(){return Loaded.SOURCE;}
    static NativeInvestigationScene sceneFor(LevelDefinition level){
        var source=packaged();
        if(!level.equals(source.level()))throw new IllegalArgumentException("Northwatch requires the exact authored revision");
        return source.scene();
    }

    private static Source load(){
        try(var identity=resource("source-hashes.tsv");var runtime=resource("runtime-input.tsv");
            var schedule=resource("door-schedule.tsv");var model=resource("station-with-furnishings.glb");
            var physics=resource("station-with-furnishings.hcs2")){
            var hashes=WharfNativeLevel.rows(identity);
            require(hashes.size()==1&&hashes.get(0).length==3&&hashes.get(0)[0].equals("SOURCE"),"Missing Northwatch source hashes");
            var source=read(WharfNativeLevel.rows(runtime),WharfNativeLevel.rows(schedule),model,hashes.get(0)[1],physics,hashes.get(0)[2]);
            require(source.scene().doors().size()==16&&source.stories().size()==2&&HidingBudget.candidates(source.level()).size()==3,
                "Incomplete Northwatch furnishings, doors or refuges");
            require(source.level().sockets().stream().filter(s->s.type().equals("switch")).count()==12
                &&source.level().sockets().stream().filter(s->s.type().equals(HauntedFixtures.NATIVE_BASIN_SOCKET)).count()==2
                &&source.level().socket("generator",5)!=null&&source.level().socket("breaker",5)!=null,"Incomplete Northwatch utilities");
            return source;
        }catch(IOException error){
            throw new IllegalStateException("Cannot load "+ID,error);
        }
    }

    private static InputStream resource(String name)throws IOException {
        var in=NorthwatchNativeLevel.class.getResourceAsStream(DIRECTORY+name);
        if(in==null)throw new IOException("Missing "+DIRECTORY+name);
        return in;
    }

    static Source read(List<String[]> layout,List<String[]> schedule,InputStream model,String modelHash,
                       InputStream collision,String physicsHash)throws IOException {
        var spaces=new ArrayList<NativeInvestigationScene.Space>();
        var rooms=new Room[HouseMap.ROOM_COUNT];
        var sockets=new ArrayList<Socket>();
        var props=new ArrayList<Prop>();
        var stories=new ArrayList<Story>();
        var portals=new ArrayList<Portal>();
        var treads=new LinkedHashSet<String>();
        var routes=new LinkedHashMap<String,List<Vec>>();
        for(var row:layout){
            switch(row[0]){
                case "SPACE" -> {
                    int owner=n(row,2);
                    boolean outside=Boolean.parseBoolean(row[9]);
                    var zone=new NativeInvestigationScene.Space(row[1],owner,d(row,3),d(row,4),d(row,5),d(row,6),
                        d(row,7),d(row,8),outside,true);
                    spaces.add(zone);
                    if(rooms[owner]==null)
                        rooms[owner]=new Room(owner,row[10],zone.x0(),zone.z0(),zone.x1(),zone.z1(),zone.floor(),zone.height(),
                            outside?"native-outdoor":"metal",outside?"outdoor":"metal",point(row,11));
                }
                case "OPEN" -> portals.add(new Portal(n(row,1),n(row,2),n(row,3),d(row,4),d(row,5),d(row,6),
                    d(row,8),row[7].equals("z"),row[9],1));
                case "TREAD" -> treads.add(row[1]);
                case "COVER" -> sockets.add(new Socket(row[1],"hide",n(row,2),d(row,3),d(row,4),d(row,5),d(row,6),"walk-in-cupboard"));
                case "SERVICE" -> sockets.add(new Socket(row[1],row[2],n(row,3),d(row,4),d(row,5),d(row,6),d(row,7),row[8]));
                case "RITUAL" -> sockets.add(new Socket("northwatch-"+row[1],"ritual",n(row,2),d(row,3),d(row,4),d(row,5),0,row[1]));
                case "STORY" -> {
                    int owner=n(row,3);
                    Vec at=point(row,5);
                    double yaw=d(row,8);
                    props.add(new Prop(row[4],owner,at.x(),at.y(),at.z(),d(row,9),yaw,0,0,.05,"desk"));
                    sockets.add(new Socket(row[1],"story",owner,at.x(),at.y(),at.z(),yaw,row[2]));
                    stories.add(new Story(row[2],owner,point(row,10),point(row,13)));
                    SiteStories.document(ID,row[2]);
                }
                case "POINT" -> routes.computeIfAbsent(row[1],ignored->new ArrayList<>()).add(point(row,2));
                case "RIM_CONTROL","ARRIVAL" -> { /* The probe and marked itinerary are review inputs. */ }
                default -> throw new IOException("Unknown Northwatch row "+row[0]);
            }
        }
        var doors=new ArrayList<NativeInvestigationDoor.Spec>();
        for(var row:schedule){
            var portal=new Portal(n(row,0),n(row,4),n(row,5),d(row,6),d(row,7),d(row,8),d(row,9),row[10].equals("z"),"door",1);
            portals.add(portal);
            doors.add(new NativeInvestigationDoor.Spec(row[1],row[2],row[3],portal,d(row,11),d(row,12),d(row,13),
                n(row,14),n(row,15),n(row,16),row[17]));
        }
        var scene=NativeInvestigationScene.read(model,modelHash,collision,physicsHash,treads,spaces,doors);
        var dock=new Dock(32,0,76,0);
        var van=new VanStaging(new VanStaging.Placement(dock.x(),dock.y(),dock.z(),dock.yaw()));
        var toe=van.world(0,VanStaging.FRONT);
        rooms[14]=new Room(14,"Expedition van",30.74,73,33.26,78.4,van.floorY(),VanStaging.INTERIOR_HEIGHT,
            "van","metal",new Vec(32,van.floorY(),76));
        portals.add(new Portal(905,13,14,toe.x(),van.floorY(),toe.z(),VanStaging.RAMP_HALF_WIDTH*2,true,"trail",1));
        for(int i=0;i<rooms.length;i++)require(rooms[i]!=null,"Missing Northwatch owner "+i);
        var level=new LevelDefinition(ID,scene.revision(),TITLE,List.of(rooms),portals,List.of(),List.of(),props,sockets,List.of(),dock,"polar");
        routes.replaceAll((id,path)->List.copyOf(path));
        return new Source(level,scene,Map.copyOf(routes),List.copyOf(stories));
    }

    private static double d(String[] row,int index){return Double.parseDouble(row[index]);}
    private static int n(String[] row,int index){return Integer.parseInt(row[index]);}
    private static Vec point(String[] row,int index){return new Vec(d(row,index),d(row,index+1),d(row,index+2));}
    private static void require(boolean pass,String why)throws IOException{if(!pass)throw new IOException(why);}
}
