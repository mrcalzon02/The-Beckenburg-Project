package org.beckenburg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The opt-in legacy LOS cache preserves sampler output and real door shadows. */
public final class LegacyFixtureVisibilityTest {
    private static final String PROPERTY="beckenburg.fixture.retainVisibility";
    private record Probe(int room,double x,double y,double z,double nx,double ny,double nz,int mode){
        FixtureLighting.RGB sample(FixtureLighting field){
            return mode==1?field.sampleNormal(room,x,y,z,nx,ny,nz)
                :mode==2?field.sampleDistanceLod(room,x,y,z,30):field.sample(room,x,y,z);
        }
    }
    private static void check(boolean value,String why){
        if(!value)throw new AssertionError(why);
    }
    private static FixtureLighting field(HouseMap map,boolean retained){
        System.setProperty(PROPERTY,Boolean.toString(retained));
        return new FixtureLighting(map);
    }
    private static List<FixtureLighting.RGB> sample(FixtureLighting field,List<Probe> probes){
        var result=new ArrayList<FixtureLighting.RGB>();
        for(var probe:probes)result.add(probe.sample(field));
        return List.copyOf(result);
    }
    private static List<FixtureLighting.RGB> parity(FixtureLighting legacy,FixtureLighting retained,
                                                  List<Probe> probes,String event){
        var expected=sample(legacy,probes);var actual=sample(retained,probes);
        check(actual.equals(expected),"Legacy scalar/normal/LOD RGB differs after "+event);
        return actual;
    }
    public static void main(String[] args){
        String previous=System.getProperty(PROPERTY);
        try{
            var map=new HouseMap(HouseMap.Site.HOUSE);var circuits=new RoomLighting();
            circuits.acceptSnapshot("111111111111");
            var legacy=field(map,false);var retained=field(map,true);
            var probes=new ArrayList<Probe>();
            for(var source:legacy.sources()){
                probes.add(new Probe(source.room(),source.x(),source.y()-.7,source.z(),0,0,0,0));
                probes.add(new Probe(source.room(),source.x(),source.y()-.7,source.z(),0,1,0,1));
                probes.add(new Probe(source.room(),source.x(),source.y()-.7,source.z(),0,-1,0,1));
            }
            for(var door:map.level().portals())if(door.door())for(double side:new double[]{-.4,.4}){
                double x=door.x()+(door.alongX()?0:side),y=door.y()+1.2,
                    z=door.z()+(door.alongX()?side:0);
                int room=map.roomAt(x,y,z);
                probes.add(new Probe(room,x,y,z,0,0,0,0));
                probes.add(new Probe(room,x,y,z,0,1,0,1));
                probes.add(new Probe(room,x,y,z,0,-1,0,1));
            }
            // Different exact positions in one scalar cell must keep legacy
            // first-representative reuse; normal sides remain separate.
            var source=legacy.sources().stream().filter(s->s.room()==0).findFirst().orElseThrow();
            double x=Math.rint(source.x()*16)/16,z=Math.rint(source.z()*16)/16,y=source.y()-.7;
            int aliases=probes.size();
            probes.add(new Probe(0,x+.002,y,z,0,0,0,0));
            probes.add(new Probe(0,x+.004,y,z,0,0,0,0));
            var outside=map.spawn(13);
            probes.add(new Probe(13,outside.x(),outside.y()+1.2,outside.z(),0,0,0,2));
            probes.add(new Probe(13,outside.x()+.01,outside.y()+1.2,outside.z(),0,0,0,2));
            legacy.frame(circuits,0);retained.frame(circuits,0);
            var baseline=parity(legacy,retained,probes,"cold sampling");
            check(retained.frameStats().shadowRays()==legacy.frameStats().shadowRays()
                &&retained.frameStats().normalRays()==legacy.frameStats().normalRays(),
                "Retention must not add cold scalar or normal rays");
            check(retained.frameStats().lodHits()==legacy.frameStats().lodHits()
                &&retained.frameStats().lodQueries()==legacy.frameStats().lodQueries(),
                "Retention must preserve legacy distant LOD selection");
            check(baseline.get(1).energy()>baseline.get(2).energy(),
                "Opposite authored normal sides must receive different illumination");
            circuits.setBreaker(false);legacy.frame(circuits,0);retained.frame(circuits,0);
            var dark=parity(legacy,retained,probes,"breaker off");
            check(!dark.equals(baseline),"Breaker must change actual light output");
            circuits.setBreaker(true);legacy.frame(circuits,0);retained.frame(circuits,0);
            check(parity(legacy,retained,probes,"breaker restored").equals(baseline),
                "Restoring power must restore exact legacy RGB");
            check(legacy.frameStats().normalRays()>0&&retained.frameStats().normalRays()==0,
                "Restoring fixture intensity must reuse previously measured normal LOS");
            check(retained.frameStats().shadowRays()==0,
                "Source changes must retain quantized scalar sight");
            long scalarSaved=0,normalSaved=0,retraced=0;int changed=0;
            var last=baseline;
            for(var door:map.level().portals())if(door.door()){
                map.toggleDoor(door.id());legacy.frame(circuits,0);retained.frame(circuits,0);
                // A geometry epoch lets a moving surface choose a different
                // first representative within the same quantized scalar cell.
                Collections.swap(probes,aliases,aliases+1);
                var current=parity(legacy,retained,probes,"door "+door.id());
                for(int i=0;i<aliases;i++)if(!current.get(i).equals(last.get(i)))changed++;
                scalarSaved+=legacy.frameStats().shadowRays()-retained.frameStats().shadowRays();
                normalSaved+=legacy.frameStats().normalRays()-retained.frameStats().normalRays();
                retraced+=retained.frameStats().shadowRays()+retained.frameStats().normalRays();
                last=current;
            }
            check(changed>0&&retraced>0,"Door changes must retrace affected rays and change real illumination");
            check(scalarSaved>0&&normalSaved>0,"Door changes must retain unrelated scalar and normal visibility");
            for(var window:map.level().portals())if(window.kind().equals("window")){
                map.toggleWindow(window.id());legacy.frame(circuits,0);retained.frame(circuits,0);
                parity(legacy,retained,probes,"window "+window.id());
            }
            legacy.frame(circuits,0);retained.frame(circuits,0);
            parity(legacy,retained,probes,"stable doors");
            check(retained.frameStats().shadowRays()+retained.frameStats().normalRays()==0,
                "Stable geometry and intensity must not retrace visibility");
            map.configureHiding(0);legacy.frame(circuits,0);retained.frame(circuits,0);
            parity(legacy,retained,probes,"hiding geometry change");
            check(retained.frameStats().shadowRays()==legacy.frameStats().shadowRays()
                &&retained.frameStats().normalRays()==legacy.frameStats().normalRays(),
                "Nonportal geometry changes must invalidate every retained ray");
            System.out.println("PASS legacy visibility RGB/quantization/normals/LOD parity; saved scalar="+
                scalarSaved+" normal="+normalSaved+" door rays="+retraced+" changed colours="+changed);
        }finally{
            if(previous==null)System.clearProperty(PROPERTY);else System.setProperty(PROPERTY,previous);
        }
    }
}
