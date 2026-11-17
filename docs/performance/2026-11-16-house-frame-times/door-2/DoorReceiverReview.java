package org.beckenburg;

import java.lang.reflect.Field;
import java.util.HexFormat;
import java.util.List;
import java.nio.ByteBuffer;

/** Exercise the actual retained-world vertex builder on the archived doorway
 * boundary. Baseline/candidate output bits can be compared for the same order. */
public final class DoorReceiverReview {
    static Field field(Class<?> type, String name) throws Exception {
        var f=type.getDeclaredField(name);f.setAccessible(true);return f;
    }
    public static void main(String[] args) throws Exception {
        var view=new SceneRenderer();
        var map=new HouseMap(HouseMap.Site.HOUSE);
        var point=Class.forName("org.beckenburg.SceneRenderer$Point");
        var floats=Class.forName("org.beckenburg.SceneRenderer$GpuFloats");
        var pointCtor=point.getDeclaredConstructor(double.class,double.class,double.class);
        pointCtor.setAccessible(true);
        var bufferCtor=floats.getDeclaredConstructor();
        bufferCtor.setAccessible(true);
        var append=SceneRenderer.class.getDeclaredMethod("appendGpuVertex",floats,point,double.class,double.class,int.class,int.class,point,int.class);
        append.setAccessible(true);
        var fieldLighting=field(SceneRenderer.class,"fixtureLighting");
        field(SceneRenderer.class,"preparingWorldProbes").setBoolean(view,true);
        var a=pointCtor.newInstance(16.125,1.9375,22.5625);
        var b=pointCtor.newInstance(16.11049899,1.94557621,22.54674685);
        boolean inspectMemo = List.of(args).contains("--memo");
        boolean expectExact = List.of(args).contains("--require-exact");
        int mismatches = 0;
        for(boolean reverse:List.of(false,true)){
            map = new HouseMap(HouseMap.Site.HOUSE);
            var light=new FixtureLighting(map);
            var shadows=light.newStationaryShadows();
            var points=reverse?List.of(b,a,a,b):List.of(a,b,b,a);
            for(int i=0;i<4;i++){
                if(i>0)map.toggleDoor(2);
                light.frame(null,0);
                fieldLighting.set(view,light);
                var previous=light.useStationaryShadows(shadows,light.regionOccluderVersion(15,1,21,18,3,24));
                var buffer=bufferCtor.newInstance();
                try {
                    for(var p : points) append.invoke(view,buffer,p,0.0,0.0,LevelMaterials.WOOD,1,null,0xffffff);
                } finally {
                    light.restoreStationaryShadows(previous);
                }
                var data=(float[])field(floats,"values").get(buffer);
                var actual=ByteBuffer.allocate(4*3*Float.BYTES);
                var expected=ByteBuffer.allocate(actual.capacity());
                for(int v=0;v<4;v++){
                    var fresh=new FixtureLighting(map);
                    fresh.frame(null,0);
                    var coords=points.get(v)==a?new double[]{16.125,1.9375,22.5625}:
                        new double[]{16.11049899,1.94557621,22.54674685};
                    var rgb=fresh.prepareVertex(1,LevelMaterials.WOOD,coords[0],coords[1],coords[2],0,0,0,false).sample();
                    for(float c:new float[]{(float)rgb.r(),(float)rgb.g(),(float)rgb.b()})
                        expected.putInt(Float.floatToRawIntBits(c));
                    for(int c=5;c<8;c++)actual.putInt(Float.floatToRawIntBits(data[v*8+c]));
                }
                if(inspectMemo){
                    var probes=(java.util.Map<?,?>)field(floats,"receiverProbes").get(buffer);
                    if(probes.size()!=2||probes.values().stream().distinct().count()!=2)
                        throw new AssertionError("Distinct doorway receivers lost");
                }
                boolean match=java.util.Arrays.equals(actual.array(),expected.array());
                if(!match)mismatches++;
                System.out.println(i+","+map.doorOpen(2)+","+reverse+","+match+","+
                    HexFormat.of().formatHex(actual.array())+","+HexFormat.of().formatHex(expected.array()));
            }
            shadows.release();
        }
        if(expectExact && mismatches!=0)throw new AssertionError("Fresh receiver mismatch in "+mismatches+" batches");
    }
}
