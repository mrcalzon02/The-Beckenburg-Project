package org.beckenburg;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/** CPU conversion of completed RGBA/depth transfers; never owns GL state. */
public final class GpuReadback {
    private static final float NEAR=.065f,FAR=180f;
    private GpuReadback(){}
    public static void copy(ByteBuffer rgba,FloatBuffer sourceDepth,int width,int height,
            int[] argb,float[] depth,boolean packed){
        int count=Math.multiplyExact(width,height);
        if(width<=0||height<=0||rgba.limit()<Math.multiplyExact(count,4)
                ||sourceDepth.limit()<count||argb.length!=count||depth.length!=count
                ||packed&&rgba.order()!=ByteOrder.BIG_ENDIAN)
            throw new IllegalArgumentException("Invalid readback buffers/order");
        for(int row=0;row<height;row++){
            int sourceRow=(height-row-1)*width,destinationRow=row*width;
            for(int col=0;col<width;col++){
                int src=sourceRow+col,dst=destinationRow+col,i=src*4;
                if(packed)argb[dst]=Integer.rotateRight(rgba.getInt(i),8);
                else{
                    int r=rgba.get(i)&255,g=rgba.get(i+1)&255;
                    int b=rgba.get(i+2)&255,a=rgba.get(i+3)&255;
                    argb[dst]=(a<<24)|(r<<16)|(g<<8)|b;
                }
                float d=sourceDepth.get(src);
                depth[dst]=d>=.999999f?Float.POSITIVE_INFINITY:
                    (float)(2*NEAR*FAR/(FAR+NEAR-(2*d-1)*(FAR-NEAR)));
            }
        }
    }
}
