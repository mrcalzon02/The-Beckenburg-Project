package org.beckenburg;

import java.util.*;

/** Direct GPU vertex RGB writes must preserve the original sampled float bits. */
public final class PreparedLightWriteTest {
    private static final int[] TINTS={0,0xffffff,0x010203,0x80aaff,0xff8040,0x123456};
    private static void check(boolean yes,String message){if(!yes)throw new AssertionError(message);}
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
        for(int tick:new int[]{25,26,31,70,120}){light.castleFrame(true,tick);compare(prepared);}
        light.castleFrame(false,120);compare(prepared);
        castle.toggleDoor(CastleDoorSlots.all().stream().filter(s->s.gate()).findFirst().orElseThrow().id());
        light.castleFrame(true,121);compare(probes(light));
        for(var site:HouseMap.Site.values()){
            var map=new HouseMap(site);var field=new FixtureLighting(map);var circuits=new RoomLighting();field.frame(circuits,25);
            var samples=probes(field);compare(samples);
            for(var fixture:map.fixtures().states())map.fixtures().activate(fixture.id(),fixture.room(),26);
            field.frame(circuits,26);compare(samples);
            circuits.setBreaker(false);field.frame(circuits,31);compare(samples);
            circuits.setBreaker(true);field.frame(circuits,70);compare(samples);
        }
        System.out.println("PASS exact direct/sample RGB float bits, all tints/normals, emission, candle/TV changes, blackout, doors and all map types");
    }
}
