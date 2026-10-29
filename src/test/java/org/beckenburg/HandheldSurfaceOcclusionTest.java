package org.beckenburg;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/** Exercise the production UV vertex-lighting path against real authored doors. */
public final class HandheldSurfaceOcclusionTest {
    private static void check(boolean good,String why){if(!good)throw new AssertionError(why);}
    private static void field(SceneRenderer scene,String name,Object value)throws Exception{
        var f=SceneRenderer.class.getDeclaredField(name);f.setAccessible(true);f.set(scene,value);
    }
    private static double[] color(SceneRenderer scene,Constructor<?> point,Method lit,
            LevelDefinition.Vec target,int room,boolean uv)throws Exception{
        scene.uvLamp=uv;
        Object vertex=lit.invoke(scene,point.newInstance(target.x(),target.y(),target.z()),0.,0.,MaterialAtlas.STONE,room,false);
        double[] result=new double[3];String[] channels={"r","g","b"};
        for(int i=0;i<3;i++){var read=vertex.getClass().getDeclaredMethod(channels[i]);read.setAccessible(true);result[i]=(double)read.invoke(vertex);}
        return result;
    }
    public static void main(String[] args)throws Exception{
        Class<?> p=Class.forName("org.beckenburg.SceneRenderer$Point");
        var point=p.getDeclaredConstructor(double.class,double.class,double.class);point.setAccessible(true);
        var lit=SceneRenderer.class.getDeclaredMethod("litVertex",p,double.class,double.class,int.class,int.class,boolean.class);lit.setAccessible(true);
        for(var site:AuthoredTestSupport.SITES){
            var map=new HouseMap(site);var door=AuthoredTestSupport.door(map,0,1);
            var from=AuthoredTestSupport.side(map,door,0,.75);var to=AuthoredTestSupport.side(map,door,1,.75);
            var target=new LevelDefinition.Vec(to.x(),to.y()+1.3,to.z());
            var scene=new SceneRenderer();scene.lamp=false;scene.x=from.x();scene.z=from.z();scene.eyeHeight=from.y()+1.3;
            scene.yaw=Math.atan2(target.x()-scene.x,target.z()-scene.z);
            var fixtures=new FixtureLighting(map);fixtures.frame(new RoomLighting(),0);
            field(scene,"lightingMap",map);field(scene,"fixtureLighting",fixtures);
            var dark=color(scene,point,lit,target,1,false);var closed=color(scene,point,lit,target,1,true);
            check(java.util.Arrays.equals(dark,closed),site+": closed real door blocks actual UV contribution");
            check(map.toggleNearestDoor(from.x(),from.y(),from.z())&&map.doorOpen(door.id()),site+": actual door opens");
            var open=color(scene,point,lit,target,1,true);
            check(open[2]>closed[2]+.1,site+": same UV beam reaches surface through the open door");
        }
        System.out.println("PASS actual UV vertex lighting blocked by closed authored doors; open-door positive controls on House/Farmhouse/School");
    }
}
