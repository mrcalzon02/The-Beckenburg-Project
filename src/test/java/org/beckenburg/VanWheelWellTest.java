package org.beckenburg;

/** Actual imported tyre bounds and fitted wheel housings, independent of camera angle. */
public final class VanWheelWellTest {
    static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        int protruding=0;
        for(var triangle:GlbProps.model("ggbot_car08_van_shell").triangles()){
            if(!SceneRenderer.vanExteriorTriangle(triangle))continue;
            for(var v:new GlbProps.Vertex[]{triangle.a(),triangle.b(),triangle.c()}){
                var p=SceneRenderer.vanExteriorLocal(v.x(),v.y(),v.z());
                if(p[1]<=VanStaging.FLOOR_Y||p[1]>=1.1||p[2]>=VanStaging.REAR
                        ||Math.abs(p[0])>=VanStaging.INTERIOR_HALF_WIDTH)continue;
                protruding++;
                check(Math.abs(p[0])>VanInteriorLayout.WHEEL_WELL_INNER_X
                    &&p[1]<VanInteriorLayout.WHEEL_WELL_TOP
                    &&p[2]>VanInteriorLayout.WHEEL_WELL_FRONT&&p[2]<VanInteriorLayout.WHEEL_WELL_REAR,
                    "every donor tyre vertex protruding into cargo must be enclosed");
            }
        }
        check(protruding>20,"test must exercise actual rear tyre intrusions");
        var method=SceneRenderer.class.getDeclaredMethod("buildVanModel",java.util.List.class,
            VanStaging.class,java.util.Set.class,HouseMap.class);method.setAccessible(true);
        var faces=new java.util.ArrayList<>();
        method.invoke(new SceneRenderer(),faces,CastleSurroundings.courtyardVan(),java.util.Set.of(),null);
        // Check actual built metal caps, rather than trusting declared bounds.
        int caps=0;
        for(var face:faces){
            var material=face.getClass().getDeclaredMethod("material");material.setAccessible(true);
            if((int)material.invoke(face)!=MaterialAtlas.IRON)continue;
            boolean cap=true;
            double x0=1e9,x1=-1e9,y0=1e9,y1=-1e9,z0=1e9,z1=-1e9;
            for(String corner:new String[]{"a","b","c"}){
                var accessor=face.getClass().getDeclaredMethod(corner);accessor.setAccessible(true);
                var point=accessor.invoke(face);var y=point.getClass().getDeclaredMethod("y");y.setAccessible(true);
                cap&=Math.abs((double)y.invoke(point)-VanInteriorLayout.WHEEL_WELL_TOP)<1e-8;
                var x=point.getClass().getDeclaredMethod("x");x.setAccessible(true);
                var z=point.getClass().getDeclaredMethod("z");z.setAccessible(true);
                var local=CastleSurroundings.courtyardVan().local((double)x.invoke(point),(double)z.invoke(point));
                x0=Math.min(x0,local.x());x1=Math.max(x1,local.x());
                z0=Math.min(z0,local.z());z1=Math.max(z1,local.z());
                y0=Math.min(y0,(double)y.invoke(point));y1=Math.max(y1,(double)y.invoke(point));
            }
            if(cap)caps++;
            boolean deck=y0>=VanStaging.FLOOR_Y-.13-1e-8&&y1<=VanStaging.FLOOR_Y+1e-8;
            boolean wheelSpan=z0<VanInteriorLayout.WHEEL_WELL_REAR-1e-8&&z1>VanInteriorLayout.WHEEL_WELL_FRONT+1e-8;
            check(!(deck&&wheelSpan&&(x0< -VanInteriorLayout.WHEEL_WELL_INNER_X-1e-8
                ||x1>VanInteriorLayout.WHEEL_WELL_INNER_X+1e-8)),
                "deck or wheel-well bottom must not cut across exterior wheel barrels");
        }
        check(caps==4,"two wheel wells must each have two actual closed top triangles");
        for(double yaw:new double[]{0,Math.PI/2,-.4})for(double height:new double[]{0,1.5}){
            var van=new VanStaging(new VanStaging.Placement(32,height,78,yaw));
            for(double side:new double[]{-1,1}){
                var aisle=van.world(0,-.4);var wheel=van.world(side*.82,-.4);
                check(!van.clearSight(aisle.x(),height+.80,aisle.z(),wheel.x(),height+.80,wheel.z()),
                    "wheel well must occlude light at every rotated/elevated placement");
                check(van.collides(wheel.x(),wheel.z(),.18),"existing fitted storage collision encloses wheel well");
                check(!van.collides(aisle.x(),aisle.z(),.18),"wheel wells preserve central aisle");
            }
        }
        VanRackStockTest.main(args);
        System.out.println("PASS real donor tyres enclosed, closed 3D caps, light/collision/aisle, full six-player stock");
    }
}
