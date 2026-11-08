package org.beckenburg;

import java.awt.image.BufferedImage;
import java.util.BitSet;
import java.util.LinkedHashMap;

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
    /** Derived pixels belong to a single editor window. Each update compares the current decoded GLB. */
    static final class Cache {
        private static final int MAX_TEXELS=8*1024*1024;
        private final LinkedHashMap<Integer,Chain> chains=new LinkedHashMap<>(16,.75f,true);
        private int reserved;
        long filteredTexels;

        BufferedImage level(int slot,BufferedImage source,int level,boolean alpha){
            if(level<1||level>4)throw new IllegalArgumentException("Invalid cached preview mip");
            int w=source.getWidth(),h=source.getHeight();
            var chain=chains.get(slot);
            if(chain==null||chain.w!=w||chain.h!=h||chain.alpha!=alpha){
                if(chain!=null){chains.remove(slot);reserved-=chain.capacity;}
                long capacity=(long)w*h;
                for(int i=0,x=w,y=h;i<4&&(x>1||y>1);i++){
                    x=Math.max(1,x/2);y=Math.max(1,y/2);capacity+=(long)x*y;
                }
                if(capacity>MAX_TEXELS)return MaterialReviewMip.level(source,level,alpha);
                while(reserved+capacity>MAX_TEXELS){
                    var oldest=chains.values().iterator();reserved-=oldest.next().capacity;oldest.remove();
                }
                chain=new Chain(w,h,alpha,(int)capacity);chains.put(slot,chain);reserved+=capacity;
            }
            BitSet changed=chain.updateBase(source);
            var input=source;
            for(int i=0;i<4&&(input.getWidth()>1||input.getHeight()>1);i++){
                var target=chain.levels[i];
                if(i>=level&&target==null)break;
                if(target==null){
                    target=reduce(input,alpha);chain.levels[i]=target;
                    filteredTexels+=(long)target.getWidth()*target.getHeight();
                    changed.clear(); // Higher levels have never been allocated.
                }else if(!changed.isEmpty()){
                    var affected=affected(changed,input.getWidth(),input.getHeight(),target.getWidth(),target.getHeight());
                    changed=new BitSet();
                    for(int p=affected.nextSetBit(0);p>=0;p=affected.nextSetBit(p+1)){
                        int x=p%target.getWidth(),y=p/target.getWidth();
                        int color=filtered(input,x,y,target.getWidth(),target.getHeight(),alpha);
                        if(target.getRGB(x,y)!=color){target.setRGB(x,y,color);changed.set(p);}
                    }
                    filteredTexels+=affected.cardinality();
                }
                input=target;
            }
            var result=chain.levels[level-1];
            return result==null?input:result;
        }
    }
    private static final class Chain {
        final int w,h,capacity;
        final boolean alpha;
        final int[] previous;
        final BufferedImage[] levels=new BufferedImage[4];
        Chain(int w,int h,boolean alpha,int capacity){
            this.w=w;this.h=h;this.alpha=alpha;this.capacity=capacity;previous=new int[w*h];
        }
        BitSet updateBase(BufferedImage source){
            var changed=new BitSet();int[] row=new int[w];
            for(int y=0;y<h;y++){
                source.getRGB(0,y,w,1,row,0,w);
                for(int x=0;x<w;x++)if(previous[y*w+x]!=row[x]){
                    changed.set(y*w+x);previous[y*w+x]=row[x];
                }
            }
            return changed;
        }
    }
    private static BitSet affected(BitSet changed,int w,int h,int nx,int ny){
        var affected=new BitSet(nx*ny);
        for(int p=changed.nextSetBit(0);p>=0;p=changed.nextSetBit(p+1)){
            int x=p%w,y=p/w;
            int firstX=(int)((long)x*nx/w),lastX=(int)(((long)(x+1)*nx-1)/w);
            int firstY=(int)((long)y*ny/h),lastY=(int)(((long)(y+1)*ny-1)/h);
            for(int j=firstY;j<=lastY;j++)affected.set(j*nx+firstX,j*nx+lastX+1);
        }
        return affected;
    }
    private static BufferedImage reduce(BufferedImage source,boolean alpha){
        int w=source.getWidth(),h=source.getHeight(),nx=Math.max(1,w/2),ny=Math.max(1,h/2);
        var result=new BufferedImage(nx,ny,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<ny;y++)for(int x=0;x<nx;x++)
            result.setRGB(x,y,filtered(source,x,y,nx,ny,alpha));
        return result;
    }
    private static int filtered(BufferedImage source,int x,int y,int nx,int ny,boolean alpha){
        int w=source.getWidth(),h=source.getHeight();
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
            return (coverage<<24)|(encode(r/a)<<16)|(encode(g/a)<<8)|encode(b/a);
        }
        return 0;
    }
    private static int encode(double linear){
        double value=linear<=.0031308?12.92*linear:1.055*Math.pow(linear,1/2.4)-.055;
        return (int)Math.round(255*Math.min(1,value));
    }
}
