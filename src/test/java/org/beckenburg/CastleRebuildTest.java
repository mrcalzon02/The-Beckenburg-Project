package org.beckenburg;
import java.nio.file.*;
import java.util.*;

/** Physical, source-mesh and lighting acceptance for the room-based HQ rebuild. */
public final class CastleRebuildTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        var map=new CastleHubMap();
        check(map.valid(),"editor footprint remains connected");
        for(var slot:CastleDoorSlots.all())if(CastleLevelEdits.current().doorType(slot.id())!=CastleDoorSlots.Type.EMPTY)map.toggleDoor(slot.id());
        // Body-radius movement on a 250mm lattice, using actual walls, props and doors.
        final int width=176,height=144;var seen=new boolean[width*height];var queue=new ArrayDeque<Integer>();
        int start=77*width+54;seen[start]=true;queue.add(start);
        while(!queue.isEmpty()){
            int key=queue.removeFirst(),ix=key%width,iz=key/width;
            for(var d:List.of(new int[]{1,0},new int[]{-1,0},new int[]{0,1},new int[]{0,-1})){
                int nx=ix+d[0],nz=iz+d[1];if(nx<0||nz<0||nx>=width||nz>=height)continue;
                int next=nz*width+nx;if(seen[next]||!map.traversable(nx*.25,nz*.25))continue;
                if(!map.traversable((ix+nx)*.125,(iz+nz)*.125))continue;
                seen[next]=true;queue.add(next);
            }
        }
        for(var station:CastleHubMap.Station.values()){
            boolean reachable=false;
            for(int iz=0;iz<height&&!reachable;iz++)for(int ix=0;ix<width;ix++)if(seen[iz*width+ix]){
                double x=ix*.25,z=iz*.25,yaw=Math.atan2(station.x-x,station.z-z);
                if(map.aimed(x,z,yaw)==station){reachable=true;break;}
            }
            check(reachable,"physical approach to "+station);
            if(station!=CastleHubMap.Station.ARCHIVE)check(!map.traversable(station.x,station.z),"solid service fixture "+station);
        }
        for(var room:CastleRooms.ALL)if(room.floor()==0){
            boolean reachable=false;for(int i=0;i<seen.length;i++)if(seen[i]&&room.contains((i%width)*.25,(i/width)*.25)){reachable=true;break;}
            check(reachable,"supported route to "+room.name());
        }
        double elevation=0;
        for(double z=32.2;z>=21.8;z-=.05){elevation=CastleHubArchitecturalPlan.elevation(17.5,z,elevation);check(map.traversable(17.5,z,elevation),"stair ascent "+z);}
        for(double z=21.8;z>=5;z-=.05)check(map.traversable(17.5,z,3.4),"library approach "+z);
        elevation=3.4;for(double z=21.8;z<=32.2;z+=.05){elevation=CastleHubArchitecturalPlan.elevation(17.5,z,elevation);check(map.traversable(17.5,z,elevation),"stair descent "+z);}
        check(elevation<.1,"descent reaches ground");
        // Unioned slabs contain neither coplanar overlaps nor unclosed ends.
        var cells=CastleRooms.slabCells(CastleLevelEdits.current().decks());
        for(int i=0;i<cells.size();i++)for(int j=i+1;j<cells.size();j++){
            var a=cells.get(i);var b=cells.get(j);if(a.top()!=b.top())continue;
            check(!(Math.min(a.x1(),b.x1())-Math.max(a.x0(),b.x0())>.0001&&Math.min(a.z1(),b.z1())-Math.max(a.z0(),b.z0())>.0001),"overlapping slab tops");
        }
        check(CastleRooms.SLAB>=.20,"structural floor depth");
        var exhibits=CastleWarehouse.exhibits();
        check(exhibits.size()==129,"all 43 catalog families have three visible tier rows");
        var ids=new HashSet<String>();
        for(var e:exhibits){
            check(ids.add(e.id()+"/"+e.tier()),"duplicate display");
            check(e.mesh().equals(EquipmentModelCatalog.model(e.id(),e.tier())),"actual tier source "+e.id());
            var b=SceneRenderer.castleBounds(e.mesh());
            check(b[3]*e.scale()<=.551&&b[5]*e.scale()<=.501&&b[4]*e.scale()<=.621,"source mesh fits shelf bay "+e.id());
            check(Math.abs(e.y()-CastleWarehouse.SHELVES[e.tier().ordinal()])<.0001,"supported item bottom");
            var rack=CastleWarehouse.racks().get(e.rack());var stand=rack.world((e.column()-1)*.66,1.15);
            check(map.traversable(stand[0],stand[1]),"usable browsing aisle "+e.id()+" "+Arrays.toString(stand));
        }
        var shelf=SceneRenderer.castleBounds(CastleWarehouse.SHELF);
        check(Math.abs(shelf[3]-CastleWarehouse.WIDTH)<.002&&Math.abs(shelf[5]-CastleWarehouse.DEPTH)<.002,"native open shelf bounds match collision");
        for(var light:CastleHubDecor.LIGHTS)check(light.attached(map),"real lamp support "+light);
        for(var sw:CastleHubDecor.SWITCHES){
            check(CastleHubArchitecturalPlan.mountingWall(sw.x(),sw.y(),sw.z(),sw.yaw()),"switch mounting "+sw);
            double x=sw.x()-Math.sin(sw.yaw())*.8,z=sw.z()-Math.cos(sw.yaw())*.8,feet=sw.y()>4?3.4:0;
            check(map.traversable(x,z,feet),"reachable physical switch "+sw);
            check(CastleHubDecor.aimed(map,x,feet,z,sw.yaw())!=null,"operable physical switch");
        }
        var lighting=new FixtureLighting(map);lighting.castleFrame(true,20);
        for(var room:CastleRooms.ALL)if(!room.id().equals("gate")){
            double x=(room.x0()+room.x1())*.5,z=(room.z0()+room.z1())*.5,y=room.floor()+1;
            var lit=lighting.sample(0,x,y,z);lighting.castleFrame(false,20);var dark=lighting.sample(0,x,y,z);
            check(lit.energy()>dark.energy()+.08,"usable real lighting in "+room.name()+": "+lit.energy()+" vs "+dark.energy());
            lighting.castleFrame(true,20);
        }
        var sw=CastleHubDecor.SWITCHES.get(0);check(!map.toggleCircuit(sw),"armory circuit off");
        check(!map.lightOn(5,2,5)&&map.lightOn(35,2,25),"switch controls only its room");map.toggleCircuit(sw);
        check(!FixtureLighting.castleRayClear(map,5,1.5,3,13,1.5,3),"thick partition shadow");
        check(!FixtureLighting.castleRayClear(map,14,2,5,14,5,5),"solid upper floor shadow");
        System.out.println("PASS rebuilt Castle: physical room/station routes, continuous stairs, slab union,129 supported tier displays, switches and room lighting");
    }
}
