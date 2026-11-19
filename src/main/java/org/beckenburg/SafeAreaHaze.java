package org.beckenburg;

import java.util.List;

/** Analytic, translucent world-space fog shell shared by CPU and GPU frames.
 * Reads completed scene depth, including characters; never writes opaque depth.
 * Lower wall reaches head height and joins a soft elliptical dome over the roof. */
public final class SafeAreaHaze {
    private SafeAreaHaze(){}
    public static int draw(int[] pixels,double[] depths,int width,int height,double focal,
            double x,double eye,double z,double yaw,double pitch,double clock,List<SafeAreaBarrier.Volume> areas){
        return drawShells(pixels,depths,width,height,focal,x,eye,z,yaw,pitch,clock,
            areas.stream().map(a->(Shell)new Dome(a)).toList());
    }
    /** Usable authored cover reuses the sanctuary material at its authored cover location. */
    public static int drawHiding(int[] pixels,double[] depths,int width,int height,double focal,
            double x,double eye,double z,double yaw,double pitch,double clock,HouseMap map){
        var shells=new java.util.ArrayList<Shell>();
        for(var socket:HidingBudget.candidates(map.level()))if(HidingBudget.available(map.level(),socket,map.hidingDensity())){
            if(socket.type().equals("hide")&&LevelClosets.available(map.level(),socket.room(),map.hidingDensity()))
                shells.add(new Pocket(new SafeAreaBarrier.Volume(socket.x(),socket.y(),socket.z(),Math.hypot(.35,.31),1.65,2.02),socket.yaw(),.35,.31));
            else if(socket.type().equals("outdoor-hide")&&OutdoorHiding.available(map.level(),socket,map.hidingDensity())){
                // Mark the actual refuge asset so opaque leaves cannot hide the cue.
                // Eligibility and concealment still use the authored standing/crouch pocket.
                double radius=.36,coverHeight=1.15,shoulder=.70;
                for(var prop:map.level().props())if(prop.mesh().equals("level_shrub_refuge")&&Math.hypot(prop.x()-socket.x(),prop.z()-socket.z())<.2){
                    var b=SceneRenderer.castleBounds(prop.mesh());radius=Math.hypot(b[3]/2,b[5]/2)*prop.scale()+.04;
                    coverHeight=Math.max(1.15,prop.y()+b[1]+b[4]*prop.scale()-socket.y()+.12);shoulder=coverHeight*.90;
                }
                shells.add(new Dome(new SafeAreaBarrier.Volume(socket.x(),socket.y(),socket.z(),radius,shoulder,coverHeight)));
            }
        }
        return drawShells(pixels,depths,width,height,focal,x,eye,z,yaw,pitch,clock,shells);
    }
    private interface Shell {
        SafeAreaBarrier.Volume material();
        double hit(double ox,double oy,double oz,double dx,double dy,double dz);
    }
    private record Dome(SafeAreaBarrier.Volume material) implements Shell {
        public double hit(double ox,double oy,double oz,double dx,double dy,double dz){return surface(material,ox,oy,oz,dx,dy,dz);}
    }
    private record Pocket(SafeAreaBarrier.Volume material,double cosine,double sine,double halfX,double halfZ) implements Shell {
        Pocket(SafeAreaBarrier.Volume material,double yaw,double halfX,double halfZ){this(material,Math.cos(yaw),Math.sin(yaw),halfX,halfZ);}
        public double hit(double ox,double oy,double oz,double dx,double dy,double dz){
            double lx=ox*cosine+oz*sine,lz=-ox*sine+oz*cosine,ldx=dx*cosine+dz*sine,ldz=-dx*sine+dz*cosine;
            double near=Double.NEGATIVE_INFINITY,far=Double.POSITIVE_INFINITY;
            for(int i=0;i<3;i++){
                double origin=i==0?lx:i==1?oy:lz,direction=i==0?ldx:i==1?dy:ldz,low=i==0?-halfX:i==1?0:-halfZ,high=i==0?halfX:i==1?material.height():halfZ;
                if(Math.abs(direction)<1e-12){if(origin<low||origin>high)return Double.POSITIVE_INFINITY;}
                else{double a=(low-origin)/direction,b=(high-origin)/direction;near=Math.max(near,Math.min(a,b));far=Math.min(far,Math.max(a,b));if(far<near)return Double.POSITIVE_INFINITY;}
            }
            return near>.035?near:far>.035?far:Double.POSITIVE_INFINITY;
        }
    }
    private static int drawShells(int[] pixels,double[] depths,int width,int height,double focal,
            double x,double eye,double z,double yaw,double pitch,double clock,List<? extends Shell> shells){
        if(!Double.isFinite(clock))throw new IllegalArgumentException("finite barrier animation clock required");
        int changed=0;double cy=Math.cos(yaw),sy=Math.sin(yaw),cp=Math.cos(pitch),sp=Math.sin(pitch);
        for(var shell:shells){var a=shell.material();
            if(Math.hypot(x-a.x(),z-a.z())>70+a.radius())continue;
            double ox=x-a.x(),oz=z-a.z();
            // Perspective bounds keep distant/small wards from shading the whole screen.
            double minX=width,minY=height,maxX=0,maxY=0,minZ=Double.POSITIVE_INFINITY,maxZ=-Double.POSITIVE_INFINITY;
            for(int corner=0;corner<8;corner++){
                double wx=a.x()+((corner&1)==0?-a.radius():a.radius())-x;
                double wy=a.y()+((corner&2)==0?0:a.height())-eye;
                double wz=a.z()+((corner&4)==0?-a.radius():a.radius())-z;
                double xx=-wx*cy+wz*sy,zz=wx*sy+wz*cy,yy=wy*cp-zz*sp,depth=wy*sp+zz*cp;
                minZ=Math.min(minZ,depth);maxZ=Math.max(maxZ,depth);
                if(depth>.035){double px=width*.5+xx/depth*focal,py=height*.5-yy/depth*focal;
                    minX=Math.min(minX,px);maxX=Math.max(maxX,px);minY=Math.min(minY,py);maxY=Math.max(maxY,py);}
            }
            if(maxZ<=.035)continue;
            int x0=minZ<=.035?0:Math.max(0,(int)Math.floor(minX)-1),x1=minZ<=.035?width-1:Math.min(width-1,(int)Math.ceil(maxX)+1);
            int y0=minZ<=.035?0:Math.max(0,(int)Math.floor(minY)-1),y1=minZ<=.035?height-1:Math.min(height-1,(int)Math.ceil(maxY)+1);
            for(int row=y0;row<=y1;row++){
                double v=(height*.5-row-.5)/focal,dy=v*cp+sp,forward=cp-v*sp;
                for(int col=x0;col<=x1;col++){
                    double u=(col+.5-width*.5)/focal,dx=-u*cy+forward*sy,dz=u*sy+forward*cy;
                    double hit=shell.hit(ox,eye-a.y(),oz,dx,dy,dz);
                    int at=row*width+col;
                    if(hit<.035||hit>=depths[at]-.008)continue;
                    double wx=x+dx*hit,wy=eye+dy*hit-a.y(),wz=z+dz*hit;
                    double alpha=opacity(a,wx,wy,wz,clock);
                    int blue=LevelAtmosphere.shadePixel(0xa9dfff,hit,col,row);int old=pixels[at];double keep=1-alpha;
                    int r=(int)(((old>>16)&255)*keep+((blue>>16)&255)*alpha);
                    int g=(int)(((old>>8)&255)*keep+((blue>>8)&255)*alpha);
                    int b=(int)((old&255)*keep+(blue&255)*alpha);
                    pixels[at]=(r<<16)|(g<<8)|b;changed++;
                }
            }
        }return changed;
    }
    /** Continuous ethereal material, including across the wall/dome join.
     * The fade begins below head height and reaches almost clear at the crown. */
    static double opacity(SafeAreaBarrier.Volume a,double wx,double localY,double wz,double clock){
        double rise=Math.max(0,Math.min(1,localY/a.shoulder()));
        double smooth=rise*rise*(3-2*rise),fade=1-.78*smooth;
        if(localY>a.shoulder()){
            rise=Math.max(0,Math.min(1,(localY-a.shoulder())/(a.height()-a.shoulder())));
            smooth=rise*rise*(3-2*rise);fade=.002+.218*(1-smooth)*(1-smooth);
        }
        // Broad clouds slide through finer, curling wisps; coordinates remain world-anchored.
        double drift=Math.sin(wx*.73+wz*.45-localY*.85+clock*.43);
        double broad=.5+.5*Math.sin(wx*1.15+wz*.62+localY*1.4-clock*.68+drift*.9);
        double wisps=Math.pow(.5+.5*Math.sin(wz*2.2-wx*.8-localY*2.7+clock*1.06+drift),3);
        double mist=.055+.15*broad+.055*wisps;
        return fade*mist+.045*Math.exp(-Math.max(0,localY)*4);
    }
    /** Ray parameter equals camera-forward depth, matching the production buffer. */
    static double surface(SafeAreaBarrier.Volume a,double ox,double oy,double oz,double dx,double dy,double dz){
        double best=Double.POSITIVE_INFINITY;
        double aa=dx*dx+dz*dz,bb=ox*dx+oz*dz,cc=ox*ox+oz*oz-a.radius()*a.radius();
        double d=bb*bb-aa*cc;
        if(aa>1e-12&&d>=0){double root=Math.sqrt(d),t=(-bb-root)/aa,y=oy+dy*t;
            if(t>.035&&y>=0&&y<=a.shoulder())best=t;
            t=(-bb+root)/aa;y=oy+dy*t;if(t>.035&&y>=0&&y<=a.shoulder())best=Math.min(best,t);
        }
        double cap=a.height()-a.shoulder(),rr=a.radius()*a.radius(),hh=cap*cap;
        aa=(dx*dx+dz*dz)/rr+dy*dy/hh;bb=(ox*dx+oz*dz)/rr+(oy-a.shoulder())*dy/hh;
        cc=(ox*ox+oz*oz)/rr+(oy-a.shoulder())*(oy-a.shoulder())/hh-1;d=bb*bb-aa*cc;
        if(aa>1e-12&&d>=0){double root=Math.sqrt(d),t=(-bb-root)/aa,y=oy+dy*t;
            if(t>.035&&y>=a.shoulder()&&y<=a.height())best=Math.min(best,t);
            t=(-bb+root)/aa;y=oy+dy*t;if(t>.035&&y>=a.shoulder()&&y<=a.height())best=Math.min(best,t);
        }return best;
    }
}
