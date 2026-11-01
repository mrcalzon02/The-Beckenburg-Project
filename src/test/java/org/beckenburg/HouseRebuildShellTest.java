package org.beckenburg;

import java.util.List;

/** Detects rendered legacy envelopes that do not belong to the authored pilot.
 * The van and ground are excluded; all elevated house-shell faces must fit the
 * real room union plus measured roof/eave allowance, rather than a site enum.
 */
public final class HouseRebuildShellTest {
    public static void main(String[] args)throws Exception{
        var map=HouseRebuild.graybox();
        if(LevelEnclosure.setting(map.level().id())!=LevelEnclosure.Setting.SUBURBAN)
            throw new AssertionError("The family House must not inherit an urban service-yard backdrop");
        var indoor=map.level().rooms().stream().filter(room->!room.outside()).toList();
        double x0=indoor.stream().mapToDouble(room->room.x0()).min().orElseThrow();
        double x1=indoor.stream().mapToDouble(room->room.x1()).max().orElseThrow();
        double z0=indoor.stream().mapToDouble(room->room.z0()).min().orElseThrow();
        double z1=indoor.stream().mapToDouble(room->room.z1()).max().orElseThrow();
        var method=SceneRenderer.class.getDeclaredMethod("buildLevelShell",HouseMap.class);
        method.setAccessible(true);
        List<?> faces=(List<?>)method.invoke(null,map);
        int elevated=0;
        for(Object face:faces){
            int owner=((Number)LevelSceneSource.read(face,"room")).intValue();
            if(owner==14)continue;
            for(String corner:List.of("a","b","c")){
                Object point=LevelSceneSource.read(face,corner);
                double y=((Number)LevelSceneSource.read(point,"y")).doubleValue();
                if(y<=.6)continue;
                double x=((Number)LevelSceneSource.read(point,"x")).doubleValue();
                double z=((Number)LevelSceneSource.read(point,"z")).doubleValue();
                if(x<x0-.5||x>x1+.5||z<z0-.5||z>z1+.5)
                    throw new AssertionError("Rendered house shell exceeds its authored footprint: "+x+","+y+","+z);
                elevated++;
            }
        }
        if(elevated<100)throw new AssertionError("Missing rendered house shell");
        System.out.println("PASS rebuilt House shell follows authored coordinates: "+elevated+" elevated vertices");
    }
}
