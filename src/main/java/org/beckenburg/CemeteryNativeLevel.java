package org.beckenburg;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import org.beckenburg.LevelDefinition.Dock;
import org.beckenburg.LevelDefinition.Portal;
import org.beckenburg.LevelDefinition.Prop;
import org.beckenburg.LevelDefinition.Room;
import org.beckenburg.LevelDefinition.Socket;
import org.beckenburg.LevelDefinition.Vec;

/** Shared exact-source loader for the Cemetery and its additive mortuary variant. */
final class CemeteryNativeLevel {
    static final String TITLE = "Saint Bartholomew's Forgotten Cemetery";
    static final String ANNEX_TITLE = "Saint Bartholomew's Cemetery and Vale Mortuary";
    static final String REVIEW_PROPERTY = "beckenburg.review.cemeteryNative";

    record Story(String id,int owner,Vec reading,Vec contact) {}
    private record Anchor(int owner,String room,Vec feet) {}
    record Source(LevelDefinition level,NativeInvestigationScene scene,List<NativeInvestigationDoor.Spec> doors,
                  List<Portal> openings,Map<String,List<Vec>> routes,Map<Integer,Vec> switchFeet,List<Story> stories) {}

    private CemeteryNativeLevel() {}

    static Source packaged(boolean annex) {
        return annex ? Mortuary.SOURCE : Cemetery.SOURCE;
    }

    static NativeInvestigationScene sceneFor(boolean annex,LevelDefinition level) {
        var source=packaged(annex);
        if(!level.equals(source.level()))throw new IllegalArgumentException("Cemetery requires the exact authored variant");
        return source.scene();
    }

    private static final class Cemetery {
        private static final Source SOURCE=load(false);
    }

    private static final class Mortuary {
        private static final Source SOURCE=load(true);
    }

    private static Source load(boolean annex) {
        String id=annex?"wld-07-mortuary":"wld-07";
        try(var layout=resource(id,"room-layout.tsv");var schedule=resource(id,"door-schedule.tsv");
            var model=resource(id,"furnished-grounds.glb");var physics=resource(id,"furnished-grounds.hcs2")) {
            var rows=WharfNativeLevel.rows(layout);
            require(!rows.isEmpty(),"Missing Cemetery room schedule");
            var header=rows.get(0);
            require(header.length==3&&header[0].equals("SOURCE"),"Missing exact Cemetery source hashes");
            var source=read(annex,rows,WharfNativeLevel.rows(schedule),model,header[1],physics,header[2]);
            var level=source.level();
            require(source.doors().size()==(annex?17:7)&&source.stories().size()==(annex?2:1)
                &&source.switchFeet().size()==(annex?11:7)&&HidingBudget.candidates(level).size()==3
                &&level.socket("breaker",12)!=null&&level.socket("generator",12)!=null,
                "Incomplete Cemetery variant "+id);
            return source;
        } catch(IOException failure) {
            throw new IllegalStateException("Cannot load "+id,failure);
        }
    }

    private static InputStream resource(String id,String name)throws IOException {
        var stream=CemeteryNativeLevel.class.getResourceAsStream("/org/beckenburg/sites/"+id+"/"+name);
        if(stream==null)throw new IOException("Missing Cemetery resource "+id+"/"+name);
        return stream;
    }

    static Source read(boolean annex,List<String[]> layout,List<String[]> schedule,InputStream model,String modelHash,
                       InputStream collision,String physicsHash)throws IOException {
        var spaces=new ArrayList<NativeInvestigationScene.Space>();
        var titles=new LinkedHashMap<String,String>();
        var anchors=new ArrayList<Anchor>();
        var sockets=new ArrayList<Socket>();
        var props=new ArrayList<Prop>();
        var stories=new ArrayList<Story>();
        var switchFeet=new LinkedHashMap<Integer,Vec>();
        var stairs=new HashSet<String>();
        var openings=new ArrayList<Portal>();
        var routes=new LinkedHashMap<String,List<Vec>>();
        for(var r:layout) {
            switch(r[0]){
                case "SOURCE" -> require(r[1].equals(modelHash)&&r[2].equals(physicsHash),"Furnishings source mismatch");
                case "STORY" -> {
                    int owner=n(r,3);
                    var at=point(r,5);
                    double yaw=d(r,8);
                    props.add(new Prop(r[4],owner,at.x(),at.y(),at.z(),d(r,9),yaw,0,0,.05,"desk"));
                    sockets.add(new Socket(r[1],"story",owner,at.x(),at.y(),at.z(),yaw,r[2]));
                    stories.add(new Story(r[2],owner,point(r,10),point(r,13)));
                }
                case "SPACE" -> {
                    spaces.add(new NativeInvestigationScene.Space(r[1],n(r,2),d(r,3),d(r,4),d(r,5),d(r,6),
                        d(r,7),d(r,8),Boolean.parseBoolean(r[9]),true));
                    titles.put(r[1],r[10]);
                }
                case "ANCHOR" -> anchors.add(new Anchor(n(r,1),r[2],point(r,3)));
                case "SERVICE" -> sockets.add(new Socket(r[1],r[2],n(r,3),d(r,4),d(r,5),d(r,6),d(r,7),r[8]));
                case "COVER" -> sockets.add(new Socket(r[1],"hide",n(r,2),
                    d(r,3),d(r,4),d(r,5),d(r,6),"walk-in-cupboard"));
                case "SWITCH_FEET" -> require(switchFeet.put(n(r,1),point(r,2))==null,"Duplicate switch stance");
                case "TREAD" -> stairs.add(r[1]);
                case "RITUAL" -> sockets.add(new Socket("cemetery-"+r[1],"ritual",n(r,2),
                    d(r,3),d(r,4),d(r,5),0,r[1]));
                case "POINT" -> routes.computeIfAbsent(r[1],ignored->new ArrayList<>()).add(point(r,2));
                case "OPEN" -> openings.add(new Portal(n(r,1),n(r,2),n(r,3),d(r,4),d(r,5),d(r,6),
                    d(r,8),r[7].equals("z"),r[9],1));
                default -> throw new IllegalArgumentException("Unknown cemetery room row "+r[0]);
            }
        }
        var frames=new ArrayList<NativeInvestigationDoor.Spec>();
        var portals=new ArrayList<>(openings);
        for(var r:schedule) {
            var portal=new Portal(n(r,0),n(r,4),n(r,5),d(r,6),d(r,7),d(r,8),d(r,9),r[10].equals("z"),"door",1);
            portals.add(portal);
            frames.add(new NativeInvestigationDoor.Spec(r[1],r[2],r[3],portal,d(r,11),d(r,12),d(r,13),
                n(r,14),n(r,15),n(r,16),r[17]));
        }
        var scene=NativeInvestigationScene.read(model,modelHash,collision,physicsHash,stairs,spaces,frames);
        var rooms=new Room[HouseMap.ROOM_COUNT];
        for(var anchor:anchors){
            var at=anchor.feet();
            var zone=scene.spaceAt(at.x(),at.y(),at.z());
            require(zone!=null&&zone.id().equals(anchor.room())&&zone.owner()==anchor.owner(),
                "Incorrect cemetery anchor "+anchor+" / "+zone);
            rooms[anchor.owner()]=new Room(anchor.owner(),titles.get(zone.id()),zone.x0(),zone.z0(),zone.x1(),
                zone.z1(),zone.floor(),zone.height(),zone.outside()?"native-outdoor":"stone",
                zone.outside()?"outdoor":"stone",at);
        }
        var dock=new Dock(28,0,78,0);
        var van=new VanStaging(new VanStaging.Placement(dock.x(),dock.y(),dock.z(),dock.yaw()));
        var lip=van.world(0,VanStaging.FRONT);
        rooms[14]=new Room(14,"Expedition van",26.74,75,29.26,80.4,van.floorY(),VanStaging.INTERIOR_HEIGHT,
            "van","metal",new Vec(28,van.floorY(),78));
        portals.add(new Portal(920,13,14,lip.x(),van.floorY(),lip.z(),VanStaging.RAMP_HALF_WIDTH*2,true,"trail",1));
        var id=annex?"wld-07-mortuary":"wld-07";
        var level=new LevelDefinition(id,scene.revision(),annex?ANNEX_TITLE:TITLE,List.of(rooms),portals,List.of(),List.of(),props,sockets,
            List.of(),dock,"woodland");
        routes.replaceAll((key, route)->List.copyOf(route));
        return new Source(level,scene,List.copyOf(frames),List.copyOf(openings),
            java.util.Collections.unmodifiableMap(routes),java.util.Collections.unmodifiableMap(switchFeet),List.copyOf(stories));
    }

    private static int n(String[] r,int i){return Integer.parseInt(r[i]);}
    private static double d(String[] r,int i){return Double.parseDouble(r[i]);}
    private static Vec point(String[] r,int i){return new Vec(d(r,i),d(r,i+1),d(r,i+2));}
    private static void require(boolean pass,String message){if(!pass)throw new AssertionError(message);}
}
