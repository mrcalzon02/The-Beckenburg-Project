package org.beckenburg;

import java.util.*;

/** Direct GPU vertex RGB writes must preserve the original sampled float bits. */
public final class PreparedLightWriteTest {
    private static final int[] TINTS={0,0xffffff,0x010203,0x80aaff,0xff8040,0x123456};
    private static void check(boolean yes,String message){if(!yes)throw new AssertionError(message);}
    private record Saved(FixtureLighting.VertexProbe probe,int tint,float[] vertices){}
    private static List<Saved> save(List<FixtureLighting.VertexProbe> probes){
        var result=new ArrayList<Saved>();
        for(var probe:probes)for(int tint:TINTS){
            float[] vertices=new float[24];Arrays.fill(vertices,-17.25f);probe.writeTinted(vertices,8,tint);
            result.add(new Saved(probe,tint,vertices));
        }
        return result;
    }
    private static void compareCopies(List<Saved> saved){
        for(var state:saved){
            float[] original=state.vertices,checkpoint=original.clone(),expected=original.clone();
            state.probe.writeTinted(expected,8,state.tint);
            float[] result=state.probe.copyTintedIfChanged(original,original,8,state.tint);
            check(Arrays.equals(checkpoint,original),"A published source array must never be modified");
            check(Arrays.equals(expected,result),"Lazy copy must preserve exact current vertex data");
            check((result!=original)==!Arrays.equals(expected,original),"Clone only when final float colors change");
            check(state.probe.copyTintedIfChanged(original,result,8,state.tint)==result,"Repeated identical writes retain their existing array");
        }
    }
    private static void compare(List<FixtureLighting.VertexProbe> probes){
        for(var probe:probes)for(int tint:TINTS){
            var rgb=probe.sample();
            float[] expected={(float)(Math.max(.03,rgb.r())*((tint>>16)&255)/255.0),
                (float)(Math.max(.03,rgb.g())*((tint>>8)&255)/255.0),
                (float)(Math.max(.03,rgb.b())*(tint&255)/255.0)};
            float[] target=new float[24];Arrays.fill(target,-17.25f);
            probe.writeTinted(target,8,tint);
            for(int i=0;i<target.length;i++){
                float want=i>=13&&i<=15?expected[i-13]:-17.25f;
                check(Float.floatToRawIntBits(target[i])==Float.floatToRawIntBits(want),
                    "Exact tinted RGB bits; preserve neighboring position/UV and vertices at slot "+i);
            }
        }
    }
    private static List<FixtureLighting.VertexProbe> probes(FixtureLighting lighting){
        var result=new ArrayList<FixtureLighting.VertexProbe>();
        int glow=GlbProps.televisionMaterial(5000,0,0);var random=new Random(991);
        for(var source:lighting.sources().subList(0,Math.min(12,lighting.sources().size()))){
            for(int i=0;i<8;i++){
                double x=source.x()+(i==0?0:random.nextDouble()*3-1.5);
                double y=source.y()+(i==0?0:random.nextDouble()*2-1);
                double z=source.z()+(i==0?0:random.nextDouble()*3-1.5);
                result.add(lighting.prepareVertex(source.room(),i%2==0?glow:MaterialAtlas.STONE,x,y,z,0,0,0,false));
                for(var n:new double[][]{{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}})
                    result.add(lighting.prepareVertex(source.room(),i%2==0?glow:MaterialAtlas.STONE,x,y,z,n[0],n[1],n[2],true));
            }
        }
        return result;
    }
    public static void main(String[] args){
        var castle=new CastleHubMap();var light=new FixtureLighting(castle);light.castleFrame(true,25);
        var prepared=probes(light);check(!prepared.isEmpty(),"Actual authored light probes required");
        var retained=save(prepared);
        for(int tick:new int[]{25,26,31,70,120}){light.castleFrame(true,tick);compare(prepared);compareCopies(retained);}
        light.castleFrame(false,120);compare(prepared);compareCopies(retained);
        castle.toggleDoor(CastleDoorSlots.all().stream().filter(s->s.gate()).findFirst().orElseThrow().id());
        light.castleFrame(true,121);compare(probes(light));
        for(var site:HouseMap.Site.values()){
            var map=new HouseMap(site);var field=new FixtureLighting(map);var circuits=new RoomLighting();field.frame(circuits,25);
            var samples=probes(field);compare(samples);
            var originals=save(samples);
            for(var fixture:map.fixtures().states())map.fixtures().activate(fixture.id(),fixture.room(),26);
            field.frame(circuits,26);compare(samples);compareCopies(originals);
            circuits.setBreaker(false);field.frame(circuits,31);compare(samples);compareCopies(originals);
            circuits.setBreaker(true);field.frame(circuits,70);compare(samples);compareCopies(originals);
        }
        System.out.println("PASS exact direct/sample RGB float bits, lazy immutable copies, all tints/normals, emission, candle/TV changes, blackout, doors and all map types");
    }
}
