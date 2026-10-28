package org.beckenburg;

/** Source switches change RGB without rebuilding geometry or retracing fixed rays. */
public final class StationaryFixtureProbeTest {
    static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
    public static void main(String[] args){
        var map=new HouseMap();var circuits=new RoomLighting();var field=new FixtureLighting(map);
        var source=field.sources().stream().filter(s->s.room()>=0&&s.room()<12&&
            s.kind()==FixtureLighting.Kind.FROSTED_DOME).findFirst().orElseThrow();
        circuits.acceptSnapshot("000000000000");field.frame(circuits,0);
        var probe=field.staticProbe(source.room(),source.x(),source.y()-.7,source.z(),0,0,0);
        var dark=probe.sample();check(field.frameStats().shadowRays()>0,"off fixtures retain their actual source-to-receiver visibility");
        circuits.toggle(source.room());field.frame(circuits,0);var lit=probe.sample();
        check(lit.energy()>dark.energy(),"actual room circuit changes stationary illumination");
        check(field.frameStats().shadowRays()==0,"turning on a stationary fixture cannot retrace unchanged rays");
        circuits.toggle(source.room());field.frame(circuits,0);check(probe.sample().equals(dark),"switch-off restores exact stationary RGB");
        check(field.frameStats().shadowRays()==0,"switch-off also retains visibility weights");
        circuits.toggle(source.room());field.frame(circuits,0);
        var up=field.staticProbe(source.room(),source.x(),source.y()-.7,source.z(),0,1,0).sample();
        var down=field.staticProbe(source.room(),source.x(),source.y()-.7,source.z(),0,-1,0).sample();
        check(up.energy()>down.energy(),"opposite authored normals retain different light-facing sides");
        var door=map.level().portals().stream().filter(p->p.door()).findFirst().orElseThrow();map.toggleDoor(door.id());
        field.frame(circuits,0);probe.sample();check(field.frameStats().shadowRays()>0,"changed door invalidates actual occluder samples");
        field.frame(circuits,0);probe.sample();check(field.frameStats().shadowRays()==0,"stable door does not retrace");
        System.out.println("PASS source-independent stationary visibility, real switched RGB, preserved normals and occluder revision");
    }
}
