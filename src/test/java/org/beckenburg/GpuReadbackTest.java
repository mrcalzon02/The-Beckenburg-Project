package org.beckenburg;

import java.nio.*;
import java.util.*;

/** Functional readback contract: channels, alpha, row orientation and linear depth. */
public final class GpuReadbackTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static void verify(int w,int h,boolean direct){
        int count=w*h;var bytes=(direct?ByteBuffer.allocateDirect(count*4):ByteBuffer.allocate(count*4)).order(ByteOrder.BIG_ENDIAN);
        var sourceDepth=FloatBuffer.allocate(count);var random=new Random(771+w+h);
        float[] samples={0,.1f,.5f,.999998f,.999999f,1,Float.NaN};
        for(int i=0;i<count;i++){
            // Includes every alpha value, transparent color and unsigned channels.
            int rgba=(random.nextInt()&0xffffff00)|(i&255);bytes.putInt(i*4,rgba);
            sourceDepth.put(i,i%11==0?samples[i%samples.length]:random.nextFloat());
        }
        bytes.position(Math.min(7,bytes.limit()));sourceDepth.position(Math.min(2,count));
        int[] expected=new int[count];float[] expectedDepth=new float[count];
        for(int row=0;row<h;row++)for(int col=0;col<w;col++){
            int src=(h-row-1)*w+col,dst=row*w+col,i=src*4;
            expected[dst]=((bytes.get(i+3)&255)<<24)|((bytes.get(i)&255)<<16)|((bytes.get(i+1)&255)<<8)|(bytes.get(i+2)&255);
            float d=sourceDepth.get(src),near=.065f,far=180f;
            expectedDepth[dst]=d>=.999999f?Float.POSITIVE_INFINITY:(float)(2*near*far/(far+near-(2*d-1)*(far-near)));
        }
        for(boolean packed:new boolean[]{false,true}){
            int[] argb=new int[count];float[] depth=new float[count];
            GpuReadback.copy(bytes,sourceDepth,w,h,argb,depth,packed);
            check(Arrays.equals(expected,argb),"Exact RGBA-to-ARGB and row flip at "+w+"x"+h+" packed="+packed);
            for(int i=0;i<count;i++)check(Float.floatToIntBits(expectedDepth[i])==Float.floatToIntBits(depth[i]),"Exact linear depth including infinity/NaN");
            check(bytes.position()==Math.min(7,bytes.limit())&&sourceDepth.position()==Math.min(2,count),"Absolute reads preserve buffer positions");
        }
        bytes.order(ByteOrder.LITTLE_ENDIAN);
        var argb=new int[count];var depth=new float[count];GpuReadback.copy(bytes,sourceDepth,w,h,argb,depth,false);
        check(Arrays.equals(expected,argb),"Byte-wise fallback is native-order independent");
        boolean rejected=false;
        try{GpuReadback.copy(bytes,sourceDepth,w,h,argb,depth,true);}catch(IllegalArgumentException expectedError){rejected=true;}
        check(rejected,"Packed reads require explicit RGBA byte order");
    }
    public static void main(String[] args){
        for(var size:new int[][]{{1,1},{7,5},{160,120},{640,400},{1280,800},{1920,600}})
            for(boolean direct:new boolean[]{false,true})verify(size[0],size[1],direct);
        boolean rejected=false;
        try{GpuReadback.copy(ByteBuffer.allocate(4),FloatBuffer.allocate(1),2,2,new int[4],new float[4],false);}
        catch(IllegalArgumentException expected){rejected=true;}
        check(rejected,"Invalid framebuffer dimensions cannot read stale data");
        System.out.println("PASS packed/legacy readback: exact color, alpha, vertical flip, linear depth, native order, buffer position and dimensions");
    }
}
