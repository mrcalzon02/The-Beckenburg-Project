package org.beckenburg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import org.beckenburg.editor.EditorAgentService;
import org.beckenburg.editor.EditorHistory;
import org.beckenburg.editor.RuntimeSceneDocument;

/**
 * Real School construction, renderer, collision, persistent native BSCN and
 * independently loaded game. A room only counts when all four solid walls,
 * one operable door and the ceiling exist in the actual game.
 */
public final class EditorAgentArchitecturalRoomTest {
    private EditorAgentArchitecturalRoomTest(){}
    private record Site(double x0,double z0,double x1,double z1){}
    private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    private static void rejects(Runnable action){
        try{action.run();}catch(IllegalArgumentException|IllegalStateException expected){return;}
        throw new AssertionError("Unsafe incomplete native room edit was accepted");
    }
    private static Map<String,String> room(Site p){
        return Map.of("id","gym-store","room","6","x0",Double.toString(p.x0()),
            "z0",Double.toString(p.z0()),"x1",Double.toString(p.x1()),
            "z1",Double.toString(p.z1()),"height","2.35",
            "material",Integer.toString(LevelMaterials.PLASTER),"doorSide","S");
    }
    private static Site findRoom(LevelAuthoringSession session,HouseMap map){
        var gym=map.level().room(6);
        // Bounded authentic Gymnasium construction, not a synthetic room map.
        for(double x=35.3;x<42.3;x+=.65)
            for(double z=9.0;z<19.0;z+=.65){
                var site=new Site(x,z,x+2.65,z+2.65);
                double cx=(site.x0()+site.x1())*.5,cz=site.z1()+1.5;
                if(!gym.contains(cx,gym.y(),cz)||!map.traversable(cx,gym.y(),cz))
                    continue;
                try{
                    session.buildRoom("gym-store",6,site.x0(),site.z0(),
                        site.x1(),site.z1(),2.35,LevelMaterials.PLASTER,"S");
                    session.removeRoom(MissionArchitecturalRoom.NODE_STEM+"gym-store");
                    return site;
                }catch(IllegalArgumentException rejected){
                    check(session.architecturalRooms().isEmpty(),
                        "Rejected candidate cannot leave partial construction in scene");
                }
            }
        throw new AssertionError("School Gymnasium has no valid floor for an authored child room");
    }
    private static void aim(SceneRenderer renderer,Site site,double floor){
        renderer.x=(site.x0()+site.x1())*.5;
        renderer.z=site.z1()+1.65;
        renderer.eyeHeight=floor+1.62;
        renderer.yaw=Math.PI;
        renderer.pitch=-.08;
    }
    public static void main(String[] args)throws Exception {
        Path home=Files.createTempDirectory("beckenburg-architectural-room-");
        String old=System.getProperty("user.home");
        System.setProperty("user.home",home.toString());
        try {
            var site=HouseMap.Site.SCHOOL;
            var session=new LevelAuthoringSession(site);
            var preMap=new HouseMap(site,session.level());
            var area=findRoom(session,preMap);
            var renderer=new SceneRenderer();
            renderer.setRenderSize(360,225);
            aim(renderer,area,0);
            var service=new EditorAgentService<>(
                new LevelEditorAgent(session,
                    ()->new HouseMap(site,session.level()),renderer,()->{}),
                new EditorHistory<>(session,32));
            check(service.summary().operations().containsAll(List.of("build-room","remove-room")),
                "The real mission editor advertises atomic room construction and deletion");
            var baseline=session.snapshot();
            long revision=session.revision();
            var captures=Path.of("build","editor-agent-evidence","architectural-room");
            Files.createDirectories(captures);
            service.capture(revision,captures.resolve("school-before.png"));

            service.transact(revision,"build-room",room(area));
            check(session.walls().size()==4 && session.architecturalRooms().size()==1,
                "One native agent transaction builds exactly four real wall members");
            var child=session.architecturalRooms().get(0);
            String node=child.nodeId();
            check(child.zone()==6&&child.doorSide().equals("S")&&
                child.id().equals("gym-store"),"Stable architectural identity retains its parent gameplay zone");
            var hierarchy=service.node(session.revision(),node).result();
            check(hierarchy.kind().equals("ARCH_ROOM"),"Native scene exposes a first-class child-room node");
            check(service.properties(session.revision(),node).result().fields()
                .get("physicalWallCount").equals(4),"Agent reads actual child construction");
            var level=session.level();
            var map=new HouseMap(site,level);
            check(map.roomAt(child.cx(),child.base(),child.cz())==6 &&
                "gym-store".equals(map.architecturalRoomAt(child.cx(),child.base(),child.cz())),
                "Actual game reports stable physical child-room identity without changing its multiplayer gameplay zone");
            check(level.sockets().stream().anyMatch(s->
                s.id().equals(MissionArchitecturalRoom.ROOF_STEM+"gym-store")&&
                    s.type().equals(MissionArchitecturalRoom.ROOF_TYPE)),
                "Game renderer owns an actual textured, opaque ceiling socket");
            check(level.colliders().stream().anyMatch(c->
                LevelPlans.sameBody(c,child.roofBody())),
                "Game physics includes the exact solid ceiling body");
            check(!map.airborneClear(child.cx(),child.base()+2.11,child.cz(),.90),
                "The child ceiling blocks the real swept body above head height");
            check(map.traversable(child.cx(),child.base(),child.cz()),
                "Standing player is supported inside the constructed room");
            check(service.test(session.revision(),"mission-architectural-rooms").result()
                .stream().allMatch(EditorAgentService.Check::passed),
                "Native diagnostic proves wall/door/ceiling/runtime identity parity");

            var door=level.portals().stream().filter(p->p.id()==child.doorPortalId())
                .findFirst().orElseThrow();
            double dx=door.x(),dz=door.z();
            check(door.a()==6&&door.b()==6&&door.door()&&door.alongX(),
                "Real game doorway preserves its original gameplay protocol zone");
            check(!map.traversable(dx,door.y(),dz),"Real closed door blocks standing capsule");
            map.toggleDoor(door.id());
            check(map.traversable(dx,door.y(),dz),"Open door permits player through the authored passage");
            var remote=new HouseMap(site,level);
            remote.applyDoorSnapshot(map.doorSnapshot());
            check(remote.doorOpen(door.id())&&remote.traversable(dx,door.y(),dz),
                "Network snapshot replicates the exact live authoring door");
            aim(renderer,area,0);
            service.capture(session.revision(),captures.resolve("school-built.png"));
            check(!Arrays.equals(Files.readAllBytes(captures.resolve("school-before.png")),
                Files.readAllBytes(captures.resolve("school-built.png"))),
                "Actual fixed-camera renderer displays newly constructed walls/ceiling/door");
            var constructed=session.snapshot();
            rejects(()->service.transact(session.revision(),"build-room",room(area)));
            rejects(()->service.transact(session.revision(),"remove-wall",
                Map.of("id",MissionStructuralWall.NODE_PREFIX+"archroom-gym-store-N")));
            rejects(()->service.transact(session.revision(),"set-wall-door",
                Map.of("id",MissionStructuralWall.NODE_PREFIX+"archroom-gym-store-S",
                    "state","REMOVE")));
            check(session.snapshot().equals(constructed),
                "Duplicate rooms, partial demolition and orphan door edits are all transactionally rejected");

            service.undo(session.revision());
            check(session.snapshot().equals(baseline)&&session.architecturalRooms().isEmpty(),
                "Single undo removes all physical wall and ceiling geometry");
            service.redo(session.revision());
            check(session.snapshot().equals(constructed),
                "Single redo rebuilds the entire room with stable wall/door IDs");
            service.save(session.revision());
            service.reopen(session.revision());
            check(session.snapshot().equals(constructed),
                "The authoritative native document reloads all four wall members and ceiling ownership");
            var independent=new HouseMap(site);
            check("gym-store".equals(independent.architecturalRoomAt(child.cx(),child.base(),child.cz())) &&
                independent.level().colliders().stream().anyMatch(c->
                    LevelPlans.sameBody(c,child.roofBody())) &&
                !independent.traversable(dx,door.y(),dz),
                "Independently loaded actual game reads the same room, roof and shut door");
            var savedFile=RuntimeSceneDocument.load(session.documentPath());
            for(var segment:session.walls())
                check(savedFile.find(MissionStructuralWall.NODE_PREFIX+segment.id())!=null,
                    "Each constructed physical wall is persisted in the native BSCN");
            service.transact(session.revision(),"remove-room",Map.of("id",node));
            check(session.architecturalRooms().isEmpty() &&
                session.walls().isEmpty() &&
                session.level().portals().stream().noneMatch(p->p.id()==door.id()) &&
                session.level().sockets().stream().noneMatch(s->s.type().equals(MissionArchitecturalRoom.ROOF_TYPE)),
                "Removal deletes the entire native room, roof and portal without orphaning game objects");
            service.undo(session.revision());
            check(session.snapshot().equals(constructed),"Undo of demolition restores exact complete room");
            System.out.println("PASS real School child room: 4 thick native walls, actual door, solid ceiling, stable architectural identity, standing/airborne collision, renderer captures, network snapshot, atomic rollback, BSCN independent game load, undo/redo");
        }finally{
            System.setProperty("user.home",old);
            try(var stream=Files.walk(home)){
                for(var path:stream.sorted(Comparator.reverseOrder()).toList())
                    try{Files.deleteIfExists(path);}catch(Exception ignored){}
            }
        }
    }
}
