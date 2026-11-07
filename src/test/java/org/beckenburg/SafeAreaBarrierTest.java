package org.beckenburg;

import java.util.*;
import org.beckenburg.LevelDefinition.Vec;

public final class SafeAreaBarrierTest {
    static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
    static void reject(Runnable action){try{action.run();}catch(IllegalArgumentException expected){
        check(expected.getMessage().contains("safe-area"),"must reject for actual barrier, not unrelated collision: "+expected.getMessage());return;
    }throw new AssertionError("protected move accepted");}
    static void pose(LanGhostSpatial ghost,int room,Vec p)throws Exception{
        for(String field:new String[]{"x","y","z","room"}){var f=LanGhostSpatial.class.getDeclaredField(field);f.setAccessible(true);
            if(field.equals("room"))f.setInt(ghost,room);else f.setDouble(ghost,field.equals("x")?p.x():field.equals("y")?p.y():p.z());}
    }
    public static void main(String[] args)throws Exception{
        var castle=SafeAreaBarrier.castle();
        for(var room:CastleRooms.ALL)for(double x:new double[]{room.x0(),room.x1()})for(double z:new double[]{room.z0(),room.z1()})
            check(castle.contains(x,room.floor()+1.62,z),"whole Castle protected: "+room.id());
        for(var room:CastleAnnex.SPACES)check(castle.contains(room.x0(),1.62,room.z0())&&castle.contains(room.x1(),1.62,room.z1()),"training annex enclosed");
        var castleVan=CastleSurroundings.courtyardVan();check(castle.contains(castleVan.placement().x(),1,castleVan.placement().z()),"Castle van is already inside headquarters sanctuary");
        for(double yaw:new double[]{0,Math.PI/2,-.6})for(double y:new double[]{0,2,-1}){
            var van=new VanStaging(new VanStaging.Placement(32,y,48,yaw));var a=SafeAreaBarrier.van(van);
            for(double z:new double[]{VanStaging.FRONT-VanStaging.RAMP_LENGTH,VanStaging.REAR+VanStaging.CAB_LENGTH}){
                var p=van.world(0,z);check(a.contains(p.x(),y+.1,p.z()),"dome includes vehicle and loading ramp at every placement");
            }
            var roof=van.world(0,0);check(a.contains(roof.x(),y+VanStaging.ROOF_Y,roof.z()),"dome clears van roof");
            check(!a.contains(a.x(),y+a.height()+.01,a.z()),"dome has a real upper boundary");
            check(a.crosses(new Vec(a.x()-8,y+.8,a.z()),new Vec(a.x()+8,y+.8,a.z())),"long ghost step cannot tunnel");
            check(!a.crosses(new Vec(a.x()-8,y+.8,a.z()+6),new Vec(a.x()+8,y+.8,a.z()+6)),"outside path stays unblocked");
        }
        HouseMap map=new HouseMap();var at=map.spawn(4);var ghost=new LanGhostSpatial(map,4);
        var ward=SafeAreaBarrier.ward(at.x(),at.y(),at.z());map.setTemporarySafeAreas(List.of(ward));
        var safeSpawn=SafeAreaBarrier.ghostSpawn(map,4);check(safeSpawn!=null&&!SafeAreaBarrier.protectedAt(map,safeSpawn.x(),safeSpawn.y(),safeSpawn.z()),"room drift must not spawn inside a ward");
        ghost.syncRoom(3);ghost.syncRoom(4);check(!SafeAreaBarrier.protectedAt(map,ghost.pose().x(),ghost.pose().y(),ghost.pose().z()),"actual spectral sync uses unprotected support");
        // A small test ward on a clear authored route exercises the actual host movement method.
        map.setTemporarySafeAreas(List.of());var r=map.level().room(4);boolean exercised=false;
        for(double x=r.x0()+.5;x<r.x1()-.5&&!exercised;x+=.25)for(double z=r.z0()+.5;z<r.z1()-.5&&!exercised;z+=.25){
            var start=new Vec(x,at.y(),z);var end=map.resolveMove(start,x+.31,z);
            if(end==null||end.x()-x<.25||map.roomAt(x,at.y(),z)!=4||map.roomAt(end.x(),end.y(),end.z())!=4)continue;
            map.setTemporarySafeAreas(List.of(new SafeAreaBarrier.Volume(x+.4,at.y(),z,.20,1.65,2.5)));
            pose(ghost,4,start);reject(()->ghost.step(100,0,1));
            check(Math.abs(ghost.pose().x()-x)<1e-8,"rejected ghost keeps accepted pose");exercised=true;
        }check(exercised,"real authored clear path fixture exists");
        map.setTemporarySafeAreas(List.of(ward));var world=new LanWorld(map);world.setRoom(0,4);
        check(!LanSpatialCombat.canStrike(map,world,new LanGhostSpatial.Pose(4,at.x(),at.y(),at.z(),0),0),"blue safe area blocks real spatial damage");
        var receive=new SafeAreaBarrier.Receiver();var clientMap=new HouseMap();var frames=SafeAreaBarrier.wardFrames(map,30);
        for(int i=0;i<frames.size()-1;i++){receive.accept(clientMap,LanProtocol.parts(frames.get(i)));check(clientMap.temporarySafeAreas().isEmpty(),"partial network snapshot cannot activate haze");}
        receive.accept(clientMap,LanProtocol.parts(frames.get(frames.size()-1)));check(clientMap.temporarySafeAreas().size()==1,"atomic public ward snapshot");
        map.setTemporarySafeAreas(List.of());for(String f:SafeAreaBarrier.wardFrames(map,31))receive.accept(clientMap,LanProtocol.parts(f));
        check(clientMap.temporarySafeAreas().isEmpty(),"expiry clears ghost-side haze");
        for(String f:frames)receive.accept(clientMap,LanProtocol.parts(f));check(clientMap.temporarySafeAreas().isEmpty(),"stale snapshots cannot resurrect safe areas");
        var counters=new HunterCounters();var domain=new GhostDomain(map,4);var body=new LanWorld.Body(at.x(),at.y(),at.z(),0,4);
        counters.deploy(0,HunterCounters.Kind.WARD,body,map,20);
        counters.advance(map,GhostIdentity.Type.SPIRIT,domain,ghost.pose(),21);check(map.temporarySafeAreas().isEmpty(),"airborne ward is not safe yet");
        for(int tick=22;tick<=32;tick++)counters.advance(map,GhostIdentity.Type.SPIRIT,domain,ghost.pose(),tick);
        check(map.temporarySafeAreas().size()==1,"settled ward becomes actual safe area");
        check(SafeAreaBarrier.visibleWards(map,counters.snapshot(32).stream().map(m->new SceneRenderer.WorldMark(m.x(),m.z(),m.height(),m.kind())).toList()).size()==1,"rendered ward matches authoritative area");
        counters.advance(map,GhostIdentity.Type.SPIRIT,domain,ghost.pose(),146);check(map.temporarySafeAreas().isEmpty(),"expired ward stops protection");
        var upper=map.spawn(6);var upperWard=SafeAreaBarrier.settledWard(map,new SceneRenderer.WorldMark(upper.x(),upper.z(),upper.y()+.045,HunterCounters.Kind.WARD));
        check(upperWard!=null&&Math.abs(upperWard.y()-upper.y())<1e-8,"ward haze and seal use actual upstairs support");
        try(var fixture=AuthoredTestSupport.userFixture()){
            for(var site:HouseMap.Site.values()){var m=fixture.map(site);for(int room=0;room<m.interiorRoomCount();room++){
                var p=SafeAreaBarrier.ghostSpawn(m,room);check(p!=null&&!SafeAreaBarrier.protectedAt(m,p.x(),p.y(),p.z()),"safe valid ghost spawn "+site+" "+room);
            }}
        }
        System.out.println("PASS van dome/ramp/roof, swept host rejection, ward lifecycle, damage protection, atomic public snapshots and all-site ghost spawns");
    }
}
