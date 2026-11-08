package org.beckenburg;

import java.awt.image.BufferedImage;

/** Area-filtered base-color levels for the Material Editor's explicit mip inspection. */
final class MaterialReviewMip {
    private static final double[] LINEAR=new double[256];
    static {
        for(int i=0;i<256;i++){
            double v=i/255.0;
            LINEAR[i]=v<=.04045?v/12.92:Math.pow((v+.055)/1.055,2.4);
        }
    }
    static BufferedImage level(BufferedImage source,int level,boolean alpha){
        if(level<0||level>4)throw new IllegalArgumentException("Invalid preview mip");
        for(int i=0;i<level&&(source.getWidth()>1||source.getHeight()>1);i++)source=reduce(source,alpha);
        return source;
    }
    private static BufferedImage reduce(BufferedImage source,boolean alpha){
        int w=source.getWidth(),h=source.getHeight(),nx=Math.max(1,w/2),ny=Math.max(1,h/2);
        var result=new BufferedImage(nx,ny,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<ny;y++)for(int x=0;x<nx;x++){
            double left=x*(double)w/nx,right=(x+1)*(double)w/nx;
            double top=y*(double)h/ny,bottom=(y+1)*(double)h/ny;
            double a=0,r=0,g=0,b=0;
            for(int j=(int)top;j<Math.ceil(bottom);j++)for(int i=(int)left;i<Math.ceil(right);i++){
                int pixel=source.getRGB(i,j);
                double area=(Math.min(i+1,right)-Math.max(i,left))*(Math.min(j+1,bottom)-Math.max(j,top));
                double weight=area*(alpha?(pixel>>>24)/255.0:1);
                a+=weight;r+=weight*LINEAR[(pixel>>>16)&255];g+=weight*LINEAR[(pixel>>>8)&255];b+=weight*LINEAR[pixel&255];
            }
            if(a>0){
                int coverage=(int)Math.round(255*a/((right-left)*(bottom-top)));
                result.setRGB(x,y,(coverage<<24)|(encode(r/a)<<16)|(encode(g/a)<<8)|encode(b/a));
            }
        }
        return result;
    }
    private static int encode(double linear){
        double value=linear<=.0031308?12.92*linear:1.055*Math.pow(linear,1/2.4)-.055;
        return (int)Math.round(255*Math.min(1,value));
    }
}
