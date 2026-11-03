package org.beckenburg;

/** Rear shell/door datum, original donor texture corners and real hinge pivots. */
public final class VanRearFitTest {
    static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        var rear=SceneRenderer.vanExteriorLocal(VanBodyGeometry.SOURCE_HALF_WIDTH,2.32,VanBodyGeometry.SOURCE_REAR);
        check(Math.abs(rear[0]-VanStaging.HALF_WIDTH)<1e-8&&Math.abs(rear[1]-VanStaging.ROOF_Y)<1e-8
            &&Math.abs(rear[2]-VanStaging.FRONT)<1e-8,"rear sheet metal and hinge frame must share the actual donor opening");
        var source=GlbProps.model("ggbot_car08_van_shell");int material=source.triangles().get(0).material();
        var colors=new java.util.HashSet<Integer>();
        for(boolean left:new boolean[]{true,false}){
            var door=VanBodyGeometry.door(left);check(door.size()==3,"original three triangles per donor door");
            for(var triangle:door)for(var corner:triangle){
                check(corner.u()>=.7886&&corner.u()<=.9751&&corner.v()>=.0263&&corner.v()<=.4125,
                    "door UVs must stay inside the original rear-panel island, not side or wheel artwork");
                var closed=VanBodyGeometry.doorPoint(corner,left,true,-.015);
                check(Math.abs(closed[2]-(VanStaging.FRONT-.015))<1e-8,"closed door lies on the rear frame");
                colors.add(GlbProps.sample(material,corner.u(),corner.v()));
            }
            // The source's top corner keeps its original UV, matching the retained shell cap.
            check(source.triangles().stream().flatMap(t->java.util.stream.Stream.of(t.a(),t.b(),t.c()))
                .anyMatch(v->Math.abs(v.u()-.957541)<1e-5&&Math.abs(v.v()-.026421)<1e-5),"source rear-cap UV reference exists");
            double side=left?-1:1;
            var hinge=new VanBodyGeometry.Corner(side*VanBodyGeometry.SOURCE_HALF_WIDTH,1,0,0);
            var shut=VanBodyGeometry.doorPoint(hinge,left,true,0);var open=VanBodyGeometry.doorPoint(hinge,left,false,0);
            check(Math.abs(shut[0]-open[0])<1e-8&&Math.abs(shut[2]-open[2])<1e-8,"hinge remains attached through door rotation");
            var centre=new VanBodyGeometry.Corner(0,1,0,0);
            check(VanBodyGeometry.doorPoint(centre,left,false,0)[2]<VanStaging.FRONT-.9,"door opens outward beyond rear jamb");
            var method=SceneRenderer.class.getDeclaredMethod("cargoDoorLeaf",java.util.List.class,VanStaging.class,boolean.class);
            method.setAccessible(true);var built=new java.util.ArrayList<>();
            var van=new VanStaging(new VanStaging.Placement(0,0,0));van.setCargoDoorClosed(true);
            method.invoke(null,built,van,left);int painted=0;
            for(var face:built){
                var accessor=face.getClass().getDeclaredMethod("material");accessor.setAccessible(true);
                if((int)accessor.invoke(face)!=material)continue;
                var original=door.get(painted++);int n=0;
                for(String key:new String[]{"au","av","bu","bv","cu","cv"}){
                    accessor=face.getClass().getDeclaredMethod(key);accessor.setAccessible(true);
                    double expected=n%2==0?original[n/2].u():original[n/2].v();
                    check(Math.abs((double)accessor.invoke(face)-expected)<1e-8,"rendered donor door retains every original UV corner");n++;
                }
            }
            check(painted==3,"production leaf must render all three original donor triangles");
        }
        check(colors.size()>3,"donor door atlas must retain painted details");
        System.out.println("PASS shared rear-opening datum, source rear-door UV island, textured details and attached outward hinges");
    }
}
