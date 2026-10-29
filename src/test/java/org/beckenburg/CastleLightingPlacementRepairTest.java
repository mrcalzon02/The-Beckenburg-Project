package org.beckenburg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Regression for live castle lighting, physical occlusion and furniture-supported props. */
public final class CastleLightingPlacementRepairTest {
    private CastleLightingPlacementRepairTest(){}
    private static void check(boolean condition,String why){
        if(!condition)throw new AssertionError(why);
    }
    private static void brightness(FixtureLighting lighting,CastleHubMap map,
                                   double x,double y,double z,String room){
        lighting.castleFrame(false,10);
        double dark=lighting.sample(0,x,y,z).energy();
        lighting.castleFrame(true,10);
        double lit=lighting.sample(0,x,y,z).energy();
        check(lit>dark+.20,room+" must receive real switchable light: "+dark+" -> "+lit);
    }
    public static void main(String[] args)throws Exception {
        CastleHubMap map=new CastleHubMap();
        var lamps=CastleHubDecor.LIGHTS;
        check(lamps.size()>=50,"room-by-room rebuild coverage reaches the actual castle");
        long upper=lamps.stream().filter(l->l.y()>6).count();
        check(upper>=2,"both upper-library lights hang from the upper ceiling");
        for(var fixture:lamps){
            check(fixture.attached(map),"unattached/roofless lamp "+fixture.x()+","+fixture.z()+","+fixture.y());
            if(fixture.mount()==CastleHubDecor.Mount.CEILING)
                check(!map.outside(fixture.x(),fixture.z()),
                    "a ceiling-mounted fitting cannot float above the uncovered courtyard");
        }
        // Regression: the old 20 cm centreline offset is inside 42 cm
        // masonry. Check every imported vertex of both on/off models against
        // the *visible face* of its actual supporting wall.
        for(var fixture:lamps){
            if(fixture.mount()!=CastleHubDecor.Mount.WALL)continue;
            var pose=CastleWallLampMount.resolve(fixture,CastleHubArchitecturalPlan.walls());
            check(pose.supported(),"sconce lacks real masonry support at "
                +fixture.x()+","+fixture.z());
            for(String mesh:java.util.List.of(fixture.on(),fixture.off())){
                var bounds=SceneRenderer.castleBounds(mesh);
                double cx=bounds[0],cz=bounds[2],cy=Math.cos(fixture.yaw());
                double sy=Math.sin(fixture.yaw());
                double towardX=Math.sin(fixture.yaw()),towardZ=Math.cos(fixture.yaw());
                double minClearance=Double.POSITIVE_INFINITY;
                for(var t:GlbProps.model(mesh).triangles())
                    for(var v:new GlbProps.Vertex[]{t.a(),t.b(),t.c()}){
                        double dx=(v.x()-cx)*fixture.scale(),dz=(v.z()-cz)*fixture.scale();
                        double x=pose.x()+dx*cy-dz*sy,z=pose.z()+dx*sy+dz*cy;
                        double fromWall=(pose.wallX()-x)*towardX
                                        +(pose.wallZ()-z)*towardZ;
                        minClearance=Math.min(minClearance,
                            fromWall-CastleDoorSlots.MASONRY_THICKNESS*.5);
                    }
                check(minClearance>=CastleWallLampMount.CLEARANCE-.00001,
                    "imported "+mesh+" intersects castle masonry: "+minClearance
                    +" at "+fixture.x()+","+fixture.z());
            }
            check(Math.abs((pose.wallX()-pose.plateX())*Math.sin(fixture.yaw())
                           +(pose.wallZ()-pose.plateZ())*Math.cos(fixture.yaw())
                           -(CastleDoorSlots.MASONRY_THICKNESS*.5+.025))<.00001,
                "mounting plate must be fitted to the masonry face");
        }
        var lighting=new FixtureLighting(map);
        check(lighting.sources().size()==lamps.size(),"all fittings feed the game light engine");
        brightness(lighting,map,5.2,1.5,29.0,"west practice wing");
        brightness(lighting,map,35.0,1.5,21.7,"east service wing");
        brightness(lighting,map,24.0,1.5,26.5,"multiplayer operations wing");
        brightness(lighting,map,14.5,5.3,5.4,"upper library");

        check(!FixtureLighting.castleRayClear(map,9.8,1.6,3.0,11.4,1.6,3.0),
            "solid dividing wall must block illumination");
        check(FixtureLighting.castleRayClear(map,9.8,1.6,6.5,11.4,1.6,6.5),
            "open masonry doorway must allow light through");
        check(!FixtureLighting.castleRayClear(map,9.8,3.0,6.5,11.4,3.0,6.5),
            "solid door lintel must block high rays");
        check(!FixtureLighting.castleRayClear(map,14,2.0,5,14,5.0,5),
            "gallery floor and ceiling prevent cross-storey bleed");
        check(!FixtureLighting.castleRayClear(map,5,3.9,5,5,6.0,5),
            "west wing pitched roof blocks upward and downward rays");
        check(FixtureLighting.castleRayClear(map,13.5,2,25,13.5,5,25),
            "open courtyard does not acquire an imaginary light-blocking ceiling");

        var folder=Files.createTempDirectory("castle-light-migration");
        try{
            Path file=folder.resolve("castle.properties");
            var fresh=new CastleLevelEdits(file);
            check(fresh.lamps().size()==lamps.size(),"new scene receives extra lamp instances");
            // Simulate an authentic older document with only its original 17
            // fixture rows and no lighting version. Do not wipe other content.
            fresh.save();
            var older=new Properties();
            try(var input=Files.newInputStream(file)){older.load(input);}
            older.remove("lighting.layout");
            older.setProperty("lamps.count","17");
            try(var output=Files.newOutputStream(file)){older.store(output,"legacy castle");}
            var migrated=new CastleLevelEdits(file);
            check(migrated.lamps().size()==lamps.size(),"existing scene receives current room coverage");
            migrated.removeLamp("lighting2_0");
            migrated.save();
            var reloaded=new CastleLevelEdits(file);
            check(reloaded.lamps().size()==lamps.size()-1
                &&reloaded.lamps().stream().noneMatch(l->l.id().equals("lighting2_0")),
                "removing an upgraded lamp persists; no unwanted resurrection");

            var parent=reloaded.props().stream().filter(p->p.id().startsWith("ground")
                &&Math.abs(p.x()-13.5)<.01&&Math.abs(p.z()-3.4)<.01)
                .findFirst().orElseThrow();
            var book=CastleHubDecor.placedAccents(reloaded).stream()
                .filter(a->a.mesh().equals("open_book_mp_1")&&Math.abs(a.z()-3.4)<.05)
                .findFirst().orElseThrow();
            check(Math.abs(book.y()-(parent.y()+
                CastleHubDecor.meshHeight(parent.mesh())*parent.scale()+.008))<.00001,
                "book must rest on real table mesh height");
            reloaded.put(new CastleLevelEdits.Prop(parent.id(),parent.mesh(),
                parent.x()+.6,parent.y(),parent.z()+.5,parent.scale(),parent.yaw(),
                parent.halfX(),parent.halfZ(),parent.upper()));
            check(CastleHubDecor.placedAccents(reloaded).stream().anyMatch(a->
                a.mesh().equals("open_book_mp_1")
                &&Math.abs(a.x()-parent.x()-.6)<.001
                &&Math.abs(a.z()-parent.z()-.5)<.001),
                "decorations follow the relocated editor prop");
            reloaded.removeProp(parent.id());
            check(CastleHubDecor.placedAccents(reloaded).stream().noneMatch(a->
                a.mesh().equals("open_book_mp_1")
                &&Math.abs(a.x()-parent.x()-.6)<.001),
                "deleted tables cannot leave floating decoration");
        }finally{
            try(var walk=Files.walk(folder)){
                for(var file:walk.sorted(java.util.Comparator.reverseOrder()).toList())
                    Files.deleteIfExists(file);
            }
        }
        System.out.println("PASS rebuilt real fittings, switch-off, shadow occlusion, existing-scene migration and grounded props");
    }
}
