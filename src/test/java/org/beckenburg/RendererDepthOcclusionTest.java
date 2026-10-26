package org.beckenburg;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Conservative Z-buffer pruning: reject only geometry that cannot win a
 * single real raster depth test. Near-plane and large-area surfaces remain.
 * This does not infer hidden ghost state or modify the network host.
 */
public final class RendererDepthOcclusionTest {
    private static void ok(boolean value,String message){
        if(!value)throw new AssertionError(message);
    }
    public static void main(String[] args)throws Exception{
        SceneRenderer renderer=new SceneRenderer();
        Class<?> pointType=Class.forName("org.beckenburg.SceneRenderer$Point");
        Constructor<?> ctor=pointType.getDeclaredConstructor(double.class,double.class,double.class);
        ctor.setAccessible(true);
        Method occluded=SceneRenderer.class.getDeclaredMethod("fullyDepthOccluded",
            pointType,pointType,pointType);
        occluded.setAccessible(true);
        Object a=ctor.newInstance(-.18,-.13,6.0);
        Object b=ctor.newInstance(.18,-.13,6.0);
        Object c=ctor.newInstance(0.0,.14,6.0);
        Arrays.fill(renderer.depths(),3.0);
        ok(Boolean.TRUE.equals(occluded.invoke(renderer,a,b,c)),
            "triangle wholly behind opaque depth must skip CPU light/raster work");
        Arrays.fill(renderer.depths(),Double.POSITIVE_INFINITY);
        ok(Boolean.FALSE.equals(occluded.invoke(renderer,a,b,c)),
            "fully uncovered triangle must not be omitted");
        Arrays.fill(renderer.depths(),3.0);
        renderer.setDepthEarlyReject(false);
        ok(Boolean.FALSE.equals(occluded.invoke(renderer,a,b,c)),
            "baseline/off switch must preserve full scene lighting");
        renderer.setDepthEarlyReject(true);
        Object near=ctor.newInstance(-.1,-.1,.01);
        ok(Boolean.FALSE.equals(occluded.invoke(renderer,near,b,c)),
            "near-plane-crossing triangles must go through the real clipper");
        Object left=ctor.newInstance(-3.0,-2.0,6.0);
        Object right=ctor.newInstance(3.0,-2.0,6.0);
        Object tall=ctor.newInstance(0,2.5,6.0);
        ok(Boolean.FALSE.equals(occluded.invoke(renderer,left,right,tall)),
            "large projected surfaces must not scan thousands of depth pixels");
        System.out.println("PASS: exact conservative software-Z early rejection, no light probes for fully hidden small triangles");
    }
}
