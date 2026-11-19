package org.beckenburg;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Random;

/** Exact conservative frustum parity across camera changes, resolutions and near planes.
 * Reference is the original per-face Math.sin/cos/array loop, not an optimized copy. */
public final class FrustumMathParityTest {
    private static final double NEAR=.065;
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static boolean reference(double[][] p,double cameraX,double cameraY,double cameraZ,
                                     double yaw,double pitch,double focal,int width,int height){
        double cy=Math.cos(yaw),sy=Math.sin(yaw),cp=Math.cos(pitch),sp=Math.sin(pitch);
        double hx=width*.5/focal+.035,hy=height*.5/focal+.035;
        int common=31;
        for(double[] v:p){
            double dx=v[0]-cameraX,dz=v[2]-cameraZ,dy=v[1]-cameraY;
            double xx=-dx*cy+dz*sy,forward=dx*sy+dz*cy;
            double yy=dy*cp-forward*sp,depth=dy*sp+forward*cp;
            int code=(depth<NEAR?1:0)|(xx>depth*hx?2:0)|
                (xx< -depth*hx?4:0)|(yy>depth*hy?8:0)|(yy< -depth*hy?16:0);
            common&=code;
            if(common==0)return true;
        }
        return false;
    }
    public static void main(String[] args)throws Exception{
        Class<?> point=Class.forName("org.beckenburg.SceneRenderer$Point");
        Class<?> face=Class.forName("org.beckenburg.SceneRenderer$Face");
        Constructor<?> p=point.getDeclaredConstructor(double.class,double.class,double.class);
        p.setAccessible(true);
        Constructor<?> f=face.getDeclaredConstructor(point,point,point,int.class,int.class,
            double.class,double.class,double.class,double.class,double.class,double.class);
        f.setAccessible(true);
        Method classify=SceneRenderer.class.getDeclaredMethod("faceInFrustum",face);
        classify.setAccessible(true);
        Field focalField=SceneRenderer.class.getDeclaredField("focal");
        focalField.setAccessible(true);
        SceneRenderer renderer=new SceneRenderer();
        Random random=new Random(0x4f50544cL);
        int compared=0;
        for(int scene=0;scene<36;scene++){
            int width=scene%3==0?320:scene%3==1?640:960;
            int height=scene%3==0?200:scene%3==1?360:540;
            renderer.setRenderSize(width,height);
            renderer.setFieldOfView(50+random.nextInt(71));
            renderer.x=random.nextDouble()*80-40;
            renderer.z=random.nextDouble()*80-40;
            renderer.eyeHeight=random.nextDouble()*9-3;
            renderer.yaw=(random.nextDouble()*2-1)*Math.PI;
            renderer.pitch=(random.nextDouble()*2-1)*1.04;
            double focal=focalField.getDouble(renderer);
            for(int n=0;n<360;n++){
                double[][] vertices=new double[3][3];
                for(int k=0;k<3;k++){
                    // Include back-facing, clipping, distant, grazing and
                    // short triangles. Culling must remain conservative.
                    double radius=n%7==0?.10:n%7==1?1:n%7==2?20:100;
                    vertices[k][0]=renderer.x+(random.nextDouble()*2-1)*radius;
                    vertices[k][1]=renderer.eyeHeight+(random.nextDouble()*2-1)*radius;
                    vertices[k][2]=renderer.z+(random.nextDouble()*2-1)*radius;
                }
                Object triangle=f.newInstance(p.newInstance(vertices[0][0],vertices[0][1],vertices[0][2]),
                    p.newInstance(vertices[1][0],vertices[1][1],vertices[1][2]),
                    p.newInstance(vertices[2][0],vertices[2][1],vertices[2][2]),
                    0xffffff,LevelMaterials.STONE,0d,0d,1d,0d,0d,1d);
                boolean expected=reference(vertices,renderer.x,renderer.eyeHeight,renderer.z,
                    renderer.yaw,renderer.pitch,focal,width,height);
                boolean actual=(boolean)classify.invoke(renderer,triangle);
                check(actual==expected,"Face frustum changed for scene="+scene+" triangle="+n);
                compared++;
            }
        }
        System.out.println("PASS: "+compared+
            " exact per-face culling results across FOV, yaw/pitch, resize and near-plane changes");
    }
}
