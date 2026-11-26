package org.beckenburg;

/** Soft circular ground decal, projected as a horizontal world-space disk. */
public final class EntityFootShadow {
 private EntityFootShadow(){}
 public static void draw(int[] pixels,double[] depth,double cameraX,double cameraZ,double yaw,double pitch,
        double entityX,double entityZ,double elevation,double radius,int opacity,int width,int height,double focal){
  if(pixels.length!=width*height||depth.length!=pixels.length||radius<=0||opacity<=0
       ||!Double.isFinite(entityX+entityZ+elevation+radius+pitch+yaw))return;
  double dx=entityX-cameraX,dz=entityZ-cameraZ,cos=Math.cos(yaw),sin=Math.sin(yaw);
  double right=-dx*cos+dz*sin,ahead=dx*sin+dz*cos,cy=Math.cos(pitch),sy=Math.sin(pitch);
  double ground=elevation-1.62;
  // The local player disk is normally below the view. Reject it before scanning pixels.
  if(Math.hypot(dx,dz)<radius){
   double up=(height*.5-height+.5)/focal,ray=up*cy+sy;
   if(ray>=0)return;
   double nearest=ground/ray*(cy-up*sy);
   if(nearest>ahead+radius)return;
  }
  // Conservative projected bounds. Disks crossing the near plane use the screen bounds.
  int x0=width-1,x1=0,y0=height-1,y1=0;boolean clipped=false;
  if(ahead+radius<.065||ahead-radius>24)return;
  for(int i=0;i<32;i++){
   double angle=i*Math.PI/16,r=right+Math.cos(angle)*radius,z=ahead+Math.sin(angle)*radius;
   double d=ground*sy+z*cy;
   if(d<.065){clipped=true;continue;}
   double px=width*.5+r/d*focal,py=height*.5-(ground*cy-z*sy)/d*focal;
   x0=Math.min(x0,(int)Math.floor(px)-1);x1=Math.max(x1,(int)Math.ceil(px)+1);
   y0=Math.min(y0,(int)Math.floor(py)-1);y1=Math.max(y1,(int)Math.ceil(py)+1);
  }
  if(clipped){x0=0;x1=width-1;y0=0;y1=height-1;}
  x0=Math.max(0,x0);x1=Math.min(width-1,x1);y0=Math.max(0,y0);y1=Math.min(height-1,y1);
  for(int py=y0;py<=y1;py++){
   double up=(height*.5-(py+.5))/focal,dy=up*cy+sy;
   if(Math.abs(dy)<1e-8)continue;
   double d=ground/dy;
   if(d<.065||d>24)continue;
   double forward=d*(cy-up*sy);
   for(int px=x0;px<=x1;px++){
    double side=(px+.5-width*.5)/focal*d;
    double rr=((side-right)*(side-right)+(forward-ahead)*(forward-ahead))/(radius*radius);
    if(rr>=1)continue;
    int at=py*width+px;
    // A receiver must exist at the ground intersection. Sky, walls and nearer actors reject it.
    if(!Double.isFinite(depth[at])||Math.abs(depth[at]-d)>.04)continue;
    double fade=1-rr;
    int alpha=(int)(Math.min(255,opacity)*fade*fade);
    if(alpha==0)continue;
    int color=pixels[at],keep=255-alpha;
    int shadow=LevelAtmosphere.shadePixel(0,d,px,py);
    pixels[at]=(((((color>>>16)&255)*keep+((shadow>>>16)&255)*alpha)/255)<<16)
        |(((((color>>>8)&255)*keep+((shadow>>>8)&255)*alpha)/255)<<8)
        |(((color&255)*keep+(shadow&255)*alpha)/255);
   }
  }
 }
}
