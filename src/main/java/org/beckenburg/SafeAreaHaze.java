package org.beckenburg;

import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.function.IntUnaryOperator;
import java.util.stream.IntStream;

/** Analytic, translucent world-space fog shell shared by CPU and GPU frames.
 * Reads completed scene depth, including characters; never writes opaque depth.
 * Lower wall reaches head height and joins a soft elliptical dome over the roof. */
public final class SafeAreaHaze {
    private SafeAreaHaze(){}
    // Reuse bit-identical horizontal projection values in moving views.
    // Opt-out retains the old per-pixel division for physical A/B.
    private static final boolean COLUMN_CACHE_ENABLED=
        !Boolean.getBoolean("beckenburg.haze.columnCache.disabled");
    /** A renderer owns the range and opacity of its first sanctuary. Range is tied to
     * the exact projection, while opacity also depends on the current animation clock. */
    static final class RayCache {
        private record View(int width,int height,double focal,double x,double eye,double z,
                            double yaw,double pitch,SafeAreaBarrier.Volume area) {}
        private View last;
        private double lastClock;
        private double[] ranges;
        private double[] alphas;
        private double[] horizontal;
        private int horizontalWidth=-1;
        private double horizontalFocal=Double.NaN;
        /** Exact horizontal camera coordinates; safe across rows and scene-depth updates. */
        double[] horizontal(int width,double focal){
            if(horizontal==null||horizontalWidth!=width||Double.doubleToLongBits(horizontalFocal)!=Double.doubleToLongBits(focal)){
                horizontal=new double[width];
                for(int col=0;col<width;col++)horizontal[col]=(col+.5-width*.5)/focal;
                horizontalWidth=width;
                horizontalFocal=focal;
            }
            return horizontal;
        }
        private boolean sameView;
        private boolean sameClock;
        private boolean previousRepeated;
        private boolean recorded;

        private boolean begin(View view, double clock, int pixels) {
            boolean repeats=view.equals(last);
            sameView=repeats && recorded;
            sameClock=sameView && Double.doubleToLongBits(clock)==Double.doubleToLongBits(lastClock);
            // A continuously changing camera uses the direct raster. A held view
            // also primes the first frame at the next step's new position.
            recorded=repeats || previousRepeated;
            previousRepeated=repeats;
            last=view;
            lastClock=clock;
            if(!recorded)return false;
            if(ranges==null || ranges.length<pixels){
                int capacity=ranges==null?pixels:Math.min(view.width()*view.height(),Math.max(pixels,ranges.length*2));
                ranges=new double[capacity];
                alphas=new double[capacity];
            }
            return true;
        }

    }

    public static int draw(int[] pixels,double[] depths,int width,int height,double focal,
            double x,double eye,double z,double yaw,double pitch,double clock,List<SafeAreaBarrier.Volume> areas){
        return draw(pixels,depths,width,height,focal,x,eye,z,yaw,pitch,clock,areas,null);
    }

    static int draw(int[] pixels,double[] depths,int width,int height,double focal,
            double x,double eye,double z,double yaw,double pitch,double clock,List<SafeAreaBarrier.Volume> areas,RayCache cache){
        return drawShells(pixels,depths,width,height,focal,x,eye,z,yaw,pitch,clock,
            areas.stream().map(a->(Shell)new Dome(a)).toList(),cache);
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
        return drawShells(pixels,depths,width,height,focal,x,eye,z,yaw,pitch,clock,shells,null);
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
            double x,double eye,double z,double yaw,double pitch,double clock,List<? extends Shell> shells,RayCache cache){
        if(!Double.isFinite(clock))throw new IllegalArgumentException("finite barrier animation clock required");
        int changed=0;double cy=Math.cos(yaw),sy=Math.sin(yaw),cp=Math.cos(pitch),sp=Math.sin(pitch);
        // Only width/focal determine horizontal projection. Camera movement,
        // ray depth, opacity, pixels and all dynamic scene effects remain live.
        double[] horizontal=COLUMN_CACHE_ENABLED&&cache!=null
            ?cache.horizontal(width,focal):null;
        for(var shell:shells){var a=shell.material();
            if(Math.hypot(x-a.x(),z-a.z())>70+a.radius())continue;
            double ox=x-a.x(),oz=z-a.z();
            double oy=eye-a.y(),cap=a.height()-a.shoulder();
            double invRadius=1/a.radius(),invCap=1/cap,capY=oy-a.shoulder();
            boolean insideDome=shell instanceof Dome&&insideDome(ox,capY,oz,invRadius,invCap);
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
            // A near-plane-straddling box normally covers the whole screen. If the
            // eye is outside a side and all screen rays point away from that side,
            // no positive ray can reach the box or the enclosed haze.
            if(minZ<=.035){
                double u=width*.5/focal,v=height*.5/focal,radius=a.radius()+1e-8;
                if(Math.abs(ox)>radius && Math.signum(ox)*cp*sy>Math.abs(cy)*u+Math.abs(sp*sy)*v+1e-10)continue;
                if(Math.abs(oz)>radius && Math.signum(oz)*cp*cy>Math.abs(sy)*u+Math.abs(sp*cy)*v+1e-10)continue;
            }
            int x0=minZ<=.035?0:Math.max(0,(int)Math.floor(minX)-1),x1=minZ<=.035?width-1:Math.min(width-1,(int)Math.ceil(maxX)+1);
            int y0=minZ<=.035?0:Math.max(0,(int)Math.floor(minY)-1),y1=minZ<=.035?height-1:Math.min(height-1,(int)Math.ceil(maxY)+1);
            if(x1<x0||y1<y0)continue;
            int span=x1-x0+1;
            boolean primary=cache!=null&&shell==shells.get(0);
            boolean recording=primary&&cache.begin(new RayCache.View(width,height,focal,x,eye,z,yaw,pitch,a),
                    clock,span*(y1-y0+1));
            double[] ranges=recording?cache.ranges:null;
            double[] alphas=recording?cache.alphas:null;
            boolean sameView=recording&&cache.sameView,sameClock=recording&&cache.sameClock;
            // A large dome needs fresh optical shading on a new clock or view. Each lane
            // owns complete scanlines. Finish this pass before blending a second shell.
            int lanes=primary&&!sameClock&&span*(y1-y0+1)>=96_000
                    ?Math.min(4,Math.min(Runtime.getRuntime().availableProcessors(),ForkJoinPool.getCommonPoolParallelism()+1)):1;
            IntUnaryOperator drawLane=lane->{
                int shaded=0;
                for(int row=y0+lane;row<=y1;row+=lanes){
                    double v=(height*.5-row-.5)/focal,dy=v*cp+sp,forward=cp-v*sp;
                    // Changing poses, secondary shells and callers without a cache use the direct loop.
                    if(ranges==null){
                        for(int col=x0;col<=x1;col++){
                            double u=horizontal==null?(col+.5-width*.5)/focal:horizontal[col],dx=-u*cy+forward*sy,dz=u*sy+forward*cy;
                            int at=row*width+col;
                            double sceneDepth=depths[at];
                            if(insideDome&&sceneDepth>=0&&Double.isFinite(sceneDepth)
                                    &&insideDome(ox+dx*sceneDepth,capY+dy*sceneDepth,
                                        oz+dz*sceneDepth,invRadius,invCap))continue;
                            double hit=shell.hit(ox,oy,oz,dx,dy,dz);
                            if(hit<.035||hit>=sceneDepth-.008)continue;
                            pixels[at]=shade(pixels[at],a,x+dx*hit,eye+dy*hit-a.y(),z+dz*hit,clock);
                            shaded++;
                        }
                        continue;
                    }
                    for(int col=x0;col<=x1;col++){
                        int at=row*width+col,ray=(row-y0)*span+col-x0;
                        double sceneDepth=depths[at];
                        // Positive ranges are analytic hits (infinity is a miss). A negative
                        // range proves the ray remained inside up to that previous scene depth.
                        // Zero means this clock has no opacity yet (the ray may have been
                        // hidden on an earlier draw). Every draw still tests the live depth.
                        if(!sameClock)alphas[ray]=0;
                        double hit=sameView?ranges[ray]:0;
                        if(hit>0 && hit>=sceneDepth-.008)continue;
                        if(hit<0 && sceneDepth>=0 && sceneDepth<=-hit)continue;
                        double alpha=alphas[ray];
                        if(sameClock && alpha>0){
                            pixels[at]=blend(pixels[at],alpha);
                            shaded++;
                            continue;
                        }
                        double u=horizontal==null?(col+.5-width*.5)/focal:horizontal[col],dx=-u*cy+forward*sy,dz=u*sy+forward*cy;
                        if(hit<=0){
                            if(insideDome&&sceneDepth>=0&&Double.isFinite(sceneDepth)
                                    &&insideDome(ox+dx*sceneDepth,capY+dy*sceneDepth,
                                        oz+dz*sceneDepth,invRadius,invCap)){
                                ranges[ray]=-sceneDepth;
                                continue;
                            }
                            hit=shell.hit(ox,oy,oz,dx,dy,dz);
                            ranges[ray]=hit;
                            if(hit<.035||hit>=sceneDepth-.008)continue;
                        }
                        alpha=opacity(a,x+dx*hit,eye+dy*hit-a.y(),z+dz*hit,clock);
                        alphas[ray]=alpha;
                        pixels[at]=blend(pixels[at],alpha);
                        shaded++;
                    }
                }
                return shaded;
            };
            changed+=lanes==1?drawLane.applyAsInt(0):IntStream.range(0,lanes).parallel().map(drawLane).sum();
        }return changed;
    }

    private static int shade(int old,SafeAreaBarrier.Volume area,double x,double y,double z,double clock){
        return blend(old,opacity(area,x,y,z,clock));
    }

    private static int blend(int old,double alpha){
        double keep=1-alpha;
        int r=(int)(((old>>16)&255)*keep+0xa9*alpha);
        int g=(int)(((old>>8)&255)*keep+0xdf*alpha);
        int b=(int)((old&255)*keep+0xff*alpha);
        return (r<<16)|(g<<8)|b;
    }

    private static boolean insideDome(double x,double capY,double z,double invRadius,double invCap){
        double nx=x*invRadius,nz=z*invRadius,ny=Math.max(0,capY)*invCap;
        // Leave the boundary, including round-off at grazing intersections, to the ray test.
        return nx*nx+nz*nz+ny*ny<1-1e-8;
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
