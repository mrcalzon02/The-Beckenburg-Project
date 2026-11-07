package org.beckenburg;

import java.util.*;

/** Real cross-family renders retire inactive worlds while retaining destination preparation. */
public final class EditorSceneRetentionTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static Object field(SceneRenderer renderer,String name)throws Exception{
        var f=SceneRenderer.class.getDeclaredField(name);f.setAccessible(true);return f.get(renderer);
    }
    private static int[] pixels(SceneRenderer renderer){var image=renderer.image();return image.getRGB(0,0,image.getWidth(),image.getHeight(),null,0,image.getWidth());}
    public static void main(String[] args)throws Exception{
        var scene=new SceneRenderer();scene.setRenderSize(320,200);scene.lamp=true;
        var castle=new CastleHubMap();var house=HouseRebuildFurnishings.map();
        scene.x=16;scene.z=12.4;scene.eyeHeight=1.62;scene.yaw=Math.atan2(-1.1,1.7925);scene.pitch=-.3;
        scene.drawCastle(castle,0,false);
        check(!((List<?>)field(scene,"castleStaticGeometry")).isEmpty(),"Actual Castle source rendered");
        scene.prepareStaticScene(house);var prepared=field(scene,"preparedShellFaces");
        scene.drawPractice(house,List.of(),0);var before=pixels(scene);
        check(field(scene,"preparedShellFaces")==prepared,"First House frame preserves exact preloaded destination shell");
        prepared=null; // The test must not itself keep the retired source geometry alive.
        check(((List<?>)field(scene,"castleStaticGeometry")).isEmpty()&&((Map<?,?>)field(scene,"castleAssemblies")).isEmpty()
            &&((List<?>)field(scene,"castleActiveAssemblies")).isEmpty()&&field(scene,"castleGeometryMap")==null
            &&field(scene,"castleEnvironment")==null,"House must not retain inactive Castle CPU faces/owners/environment");
        scene.drawCastle(castle,0,false);
        check(((List<?>)field(scene,"preparedShellFaces")).isEmpty()&&field(scene,"sceneryInstances")==null
            &&((Map<?,?>)field(scene,"preparedScenery")).isEmpty(),"Castle must not retain inactive House CPU geometry");
        scene.drawPractice(house,List.of(),0);
        check(Arrays.equals(before,pixels(scene)),"Retiring inactive worlds must preserve every actual rendered House pixel");
        System.out.println("PASS actual Castle/House scene retirement, destination preload reuse, rebuilt return and exact runtime pixel parity");
    }
}
