package org.beckenburg;

/** Depth-aware distant detail filtering and atmospheric horizon convergence.
 * Applied to the world before precipitation/HUD; nearby objects remain sharp.
 * Samples never cross a foreground depth discontinuity. */
public final class DistanceDetail {
 public static void apply(int[] pixels,double[] depth,int width,int height,int horizon){apply(pixels,depth,width,height,horizon,null);}
 public static void apply(int[] pixels,double[] depth,int width,int height,int[] background){if(background.length!=pixels.length)throw new IllegalArgumentException("Sky frame");apply(pixels,depth,width,height,0,background);}
 private static void apply(int[] pixels,double[] depth,int width,int height,int horizon,int[] background){
  if(pixels.length!=width*height||depth.length!=pixels.length)throw new IllegalArgumentException("Distance detail frame");
  int[] source=pixels.clone();
  for(int y=0;y<height;y++)for(int x=0;x<width;x++){
   int i=y*width+x;double d=depth[i];if(!Double.isFinite(d)||d<=24)continue;
   double amount=Math.min(1,(d-24)/50),fog=1-Math.exp(-Math.pow((d-24)/45,2));int radius=1+(int)(amount*2);
   int r=0,g=0,b=0,count=0;
   for(int oy=-radius;oy<=radius;oy+=radius)for(int ox=-radius;ox<=radius;ox+=radius){
    int sx=x+ox,sy=y+oy;if(sx<0||sx>=width||sy<0||sy>=height)continue;int si=sy*width+sx;
    if(!Double.isFinite(depth[si])||Math.abs(depth[si]-d)>Math.max(2,d*.12))continue;
    int c=source[si];r+=(c>>16)&255;g+=(c>>8)&255;b+=c&255;count++;
   }
   int original=source[i],result=0,target=background==null?horizon:background[i];
   for(int shift:new int[]{16,8,0}){double average=count==0?((original>>shift)&255):(shift==16?r:shift==8?g:b)/(double)count,value=((original>>shift)&255)*(1-amount)+average*amount;result|=(int)Math.round(value*(1-fog)+((target>>shift)&255)*fog)<<shift;}pixels[i]=result;
  }
 }
 private DistanceDetail(){}
}
