package org.beckenburg;

/** Physical test fixtures use authored metre coordinates and explicit elevation. */
final class AuthoredTestSupport {
    /** Persist a real USER project in an isolated profile, retain every map in
     * all-map tests, and restore the developer profile before assertions run. */
    record UserFixture(java.nio.file.Path folder,HouseMap authored) implements AutoCloseable {
        HouseMap map(HouseMap.Site site){return site==HouseMap.Site.USER?authored:new HouseMap(site);}
        public void close()throws java.io.IOException{
            try(var files=java.nio.file.Files.walk(folder)){
                for(var file:files.sorted(java.util.Comparator.reverseOrder()).toList())java.nio.file.Files.deleteIfExists(file);
            }
        }
    }
    static UserFixture userFixture()throws java.io.IOException{
        var folder=java.nio.file.Files.createTempDirectory("beckenburg-authored-fixture-");
        String home=System.getProperty("user.home"),selected=UserLevelLibrary.activeId();
        try{
            var fixture=new UserFixture(folder,ReviewMapFixtures.userLevel(folder));
            check(home.equals(System.getProperty("user.home"))&&java.util.Objects.equals(selected,UserLevelLibrary.activeId()),
                "Persisted USER fixture must leave developer profile and selection unchanged");
            return fixture;
        }catch(java.io.IOException|RuntimeException|Error failure){
            try(var files=java.nio.file.Files.walk(folder)){
                for(var file:files.sorted(java.util.Comparator.reverseOrder()).toList())java.nio.file.Files.deleteIfExists(file);
            }
            throw failure;
        }
    }
    static final HouseMap.Site[] SITES={HouseMap.Site.HOUSE,HouseMap.Site.FARMHOUSE,HouseMap.Site.SCHOOL};
    static LevelDefinition.Portal door(HouseMap map,int a,int b){return map.level().portals().stream()
        .filter(p->p.door()&&p.joins(a,b)).findFirst().orElseThrow();}
    static LevelDefinition.Vec side(HouseMap map,LevelDefinition.Portal p,int room,double distance){
        var r=map.level().room(room);
        // Exterior support rectangles may underlie the house. Their rectangle
        // centre cannot determine which side of an exterior doorway is outdoors.
        var inside=r.outside()?map.level().room(p.a()==room?p.b():p.a()):r;
        double sign=(p.alongX()?Math.signum((inside.z0()+inside.z1())/2-p.z()):Math.signum((inside.x0()+inside.x1())/2-p.x()))*(r.outside()?-1:1);
        return new LevelDefinition.Vec(p.x()+(p.alongX()?0:sign*distance),p.y(),p.z()+(p.alongX()?sign*distance:0));
    }
    /** Deliberate unit-test fixture placement; movement under test still uses host authority. */
    static void place(HouseMap map,LanWorld world,int seat,LevelDefinition.Vec p){
        if(!map.traversable(p.x(),p.y(),p.z()))throw new AssertionError("Blocked fixture "+p);
        try{
            for(String name:new String[]{"xs","ys","zs"}){
                var f=LanWorld.class.getDeclaredField(name);f.setAccessible(true);
                ((double[])f.get(world))[seat]=name.equals("xs")?p.x():name.equals("ys")?p.y():p.z();
            }
        }catch(ReflectiveOperationException bad){throw new AssertionError(bad);}
    }
    static LanWorld.Body body(HouseMap map,int room){var p=map.spawn(room);return new LanWorld.Body(p.x(),p.y(),p.z(),0,room);}
    static LanGhostSpatial.Pose pose(HouseMap map,int room){var p=map.spawn(room);return new LanGhostSpatial.Pose(room,p.x(),p.y(),p.z(),0);}
    static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
}
