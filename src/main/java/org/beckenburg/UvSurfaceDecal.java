package org.beckenburg;
import java.awt.image.BufferedImage;
import java.util.*;
import org.beckenburg.LevelDefinition.Vec;
/** A 12mm surface-attached cube; only its outward face carries the UV texture. */
public final class UvSurfaceDecal {
    private UvSurfaceDecal(){}
    public static final double DEPTH=.012,HALF=.15;
    public static List<Vec> cube(UvEvidenceMarks.Mark m){
        int axis=Math.abs(m.normal())-1;double[] n=new double[3];n[axis]=Math.signum(m.normal());
        double[] u=axis==0?new double[]{0,0,1}:new double[]{1,0,0};
        double[] v={n[1]*u[2]-n[2]*u[1],n[2]*u[0]-n[0]*u[2],n[0]*u[1]-n[1]*u[0]};
        var points=new ArrayList<Vec>();
        for(int layer=0;layer<2;layer++)for(double[] corner:new double[][]{{-1,-1},{1,-1},{1,1},{-1,1}}){
            double back=layer==0?DEPTH:0;
            points.add(new Vec(m.x()-n[0]*back+HALF*(u[0]*corner[0]+v[0]*corner[1]),m.y()-n[1]*back+HALF*(u[1]*corner[0]+v[1]*corner[1]),m.z()-n[2]*back+HALF*(u[2]*corner[0]+v[2]*corner[1])));
        }
        return List.copyOf(points);
    }
    public static BufferedImage texture(){
        var image=new BufferedImage(64,64,BufferedImage.TYPE_INT_ARGB);
        var random=new Random(0x555653504c4154L);
        for(int blot=0;blot<15;blot++){
            double cx=blot==0?32:12+random.nextInt(40),cy=blot==0?32:12+random.nextInt(40),rx=blot==0?11:2+random.nextInt(6),ry=blot==0?15:2+random.nextInt(5);
            for(int y=0;y<64;y++)for(int x=0;x<64;x++)if(Math.pow((x-cx)/rx,2)+Math.pow((y-cy)/ry,2)<1+random.nextDouble()*.25)image.setRGB(x,y,0xff91ffb4);
        }
        return image;
    }
}
