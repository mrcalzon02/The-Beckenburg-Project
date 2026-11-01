package org.beckenburg.editor;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.CRC32;
import javax.imageio.ImageIO;

/**
 * Portable, non-destructive terrain authoring on the authoritative 16-bit tile grid.
 * A workbench file is NOT a runtime renderer or a replacement for a stitched world.
 * Border vertices are protected until a multi-tile transaction is available.
 */
public final class TerrainLayerStack {
    private static final int MAGIC=0x544C5354, VERSION=1, MAX_LAYERS=12;
    public enum Brush { RAISE, LOWER, SMOOTH, FLATTEN, REVERT, TRENCH, TERRACE }
    public enum FlattenLimit { BOTH, RAISE_ONLY, LOWER_ONLY }
    public enum Noise { PERLIN, SIMPLEX, WORLEY, RIDGED, BILLOW, DOMAIN_WARP }
    public enum Material { GRASS, ROCK, DIRT, SAND, MUD, SNOW, GRAVEL, RIVERBED }
    public record Point(double x,double z,double elevation) {}
    /** Inclusive grid-space bounds for localized terrain solvers. */
    public record Region(int x0,int z0,int x1,int z1) {
        public Region {
            if(x0>x1||z0>z1)throw new IllegalArgumentException("Invalid erosion region");
        }
        public boolean contains(int x,int z){
            return x>=x0&&x<=x1&&z>=z0&&z<=z1;
        }
        public static Region around(double cx,double cz,double radius,int tileCells){
            if(!Double.isFinite(cx)||!Double.isFinite(cz)
                ||!Double.isFinite(radius)||radius<=0||tileCells<2)
                throw new IllegalArgumentException("Invalid erosion selection");
            int x0=Math.max(1,(int)Math.floor(cx-radius)),
                z0=Math.max(1,(int)Math.floor(cz-radius));
            int x1=Math.min(tileCells-1,(int)Math.ceil(cx+radius)),
                z1=Math.min(tileCells-1,(int)Math.ceil(cz+radius));
            if(x0>x1||z0>z1)
                throw new IllegalArgumentException("Erosion region does not intersect tile");
            return new Region(x0,z0,x1,z1);
        }
    }
    public record LayerInfo(String name,boolean enabled,double opacity) {}
    private static final class Layer {
        String name; boolean enabled=true; float opacity=1;
        final float[] delta;
        Layer(String name,int pixels){this.name=name;delta=new float[pixels];}
    }
    private record Change(Layer layer,int[] indices,float[] before,float[] after) {}
    private final StitchedHeightmapWorld world;
    private final int tx,tz,n,side,pixels;
    private final double step,min,max;
    private final int[] source;
    private long sourceCrc;
    private final ArrayList<Layer> layers=new ArrayList<>();
    private final byte[] material;
    private final BitSet frozen;
    private final Deque<Change> undo=new ArrayDeque<>(),redo=new ArrayDeque<>();
    private final TreeMap<Integer,Float> pending=new TreeMap<>();
    private int active;
    private boolean dirty;

    public TerrainLayerStack(StitchedHeightmapWorld world,int tx,int tz)throws IOException{
        this.world=Objects.requireNonNull(world);
        if(!world.hasTile(tx,tz))throw new IOException("Generate or import the selected tile first");
        this.tx=tx;this.tz=tz;n=world.settings().tileCells();side=n+1;pixels=side*side;
        step=world.settings().metresPerCell();min=world.settings().minElevation();
        max=world.settings().maxElevation();
        source=new int[pixels];
        for(int z=0;z<side;z++)for(int x=0;x<side;x++)source[id(x,z)]=world.sample(tx,tz,x,z);
        sourceCrc=crc(source);
        material=new byte[pixels];frozen=new BitSet(pixels);
        layers.add(new Layer("Sculpt",pixels));
        load();
    }
    private int id(int x,int z){return z*side+x;}
    private static long crc(int[] data){
        CRC32 c=new CRC32();
        for(int v:data){c.update(v&255);c.update(v>>>8);}
        return c.getValue();
    }
    public StitchedHeightmapWorld worldForWorkbench(){return world;}
    public int cells(){return n;}
    public int tileX(){return tx;}
    public int tileZ(){return tz;}
    public double metresPerCell(){return step;}
    public boolean dirty(){return dirty;}
    public int layerCount(){return layers.size();}
    public List<LayerInfo> layers(){
        var result=new ArrayList<LayerInfo>();
        for(Layer l:layers)result.add(new LayerInfo(l.name,l.enabled,l.opacity));
        return List.copyOf(result);
    }
    public int activeLayer(){return active;}
    public void selectLayer(int index){layer(index);finishStroke();active=index;}
    private Layer layer(int i){
        if(i<0||i>=layers.size())throw new IllegalArgumentException("Layer index");
        return layers.get(i);
    }
    public int addLayer(String name){
        finishStroke();
        if(layers.size()>=MAX_LAYERS)throw new IllegalStateException("Maximum 12 terrain layers");
        if(name==null||name.isBlank()||name.length()>64)throw new IllegalArgumentException("Layer name");
        layers.add(new Layer(name,pixels));active=layers.size()-1;dirty=true;return active;
    }
    public void configureLayer(int i,boolean enabled,double opacity){
        finishStroke();
        if(!Double.isFinite(opacity)||opacity<0||opacity>1)
            throw new IllegalArgumentException("Opacity must be between 0 and 1");
        Layer l=layer(i);l.enabled=enabled;l.opacity=(float)opacity;dirty=true;
    }
    private double sourceMetres(int i){return min+(max-min)*source[i]/65535.0;}
    private double composed(int i){
        double h=sourceMetres(i);
        for(Layer l:layers)if(l.enabled)h+=l.delta[i]*l.opacity;
        return h;
    }
    public double height(int x,int z){bounds(x,z);return composed(id(x,z));}
    public int material(int x,int z){bounds(x,z);return material[id(x,z)]&255;}
    public boolean frozen(int x,int z){bounds(x,z);return frozen.get(id(x,z));}
    private void bounds(int x,int z){
        if(x<0||z<0||x>n||z>n)throw new IllegalArgumentException("Outside tile");
    }
    private boolean editable(int x,int z){
        return x>0&&z>0&&x<n&&z<n&&!frozen.get(id(x,z));
    }
    public void freezeRect(int x0,int z0,int x1,int z1,boolean freeze){
        finishStroke();
        for(int z=Math.max(0,z0);z<=Math.min(n,z1);z++)
            for(int x=Math.max(0,x0);x<=Math.min(n,x1);x++)
                frozen.set(id(x,z),freeze);
        dirty=true;
    }
    public void lockMaterial(Material type,boolean lock){
        finishStroke();
        for(int i=0;i<pixels;i++)if((material[i]&255)==type.ordinal())frozen.set(i,lock);
        dirty=true;
    }
    public void paintMaterial(double cx,double cz,double radius,Material type,double maxSlope){
        if(radius<=0||!Double.isFinite(radius)||!Double.isFinite(maxSlope))
            throw new IllegalArgumentException("Invalid material brush");
        int x0=Math.max(1,(int)Math.floor(cx-radius)),x1=Math.min(n-1,(int)Math.ceil(cx+radius));
        int z0=Math.max(1,(int)Math.floor(cz-radius)),z1=Math.min(n-1,(int)Math.ceil(cz+radius));
        for(int z=z0;z<=z1;z++)for(int x=x0;x<=x1;x++){
            if(!editable(x,z)||Math.hypot(x-cx,z-cz)>radius)continue;
            if(slope(x,z)<=maxSlope)material[id(x,z)]=(byte)type.ordinal();
        }
        dirty=true;
    }
    public double slope(int x,int z){
        bounds(x,z);
        int ax=Math.max(0,x-1),bx=Math.min(n,x+1);
        int az=Math.max(0,z-1),bz=Math.min(n,z+1);
        double dx=(height(bx,z)-height(ax,z))/((bx-ax)*step);
        double dz=(height(x,bz)-height(x,az))/((bz-az)*step);
        return Math.toDegrees(Math.atan(Math.hypot(dx,dz)));
    }
    public void autoBiome(double waterLine,double snowLine,double cliffDegrees){
        finishStroke();
        for(int z=1;z<n;z++)for(int x=1;x<n;x++){
            if(!editable(x,z))continue;
            double h=height(x,z),s=slope(x,z);
            Material m=s>cliffDegrees?Material.ROCK:
                h<waterLine?Material.RIVERBED:h>snowLine?Material.SNOW:
                s>22?Material.GRAVEL:Material.GRASS;
            material[id(x,z)]=(byte)m.ordinal();
        }
        dirty=true;
    }
    /** A stroke records only modified float samples; no full-tile undo copies. */
    private void write(int x,int z,double target){
        if(!editable(x,z))return;
        Layer l=layer(active);
        if(!l.enabled||l.opacity<=0)throw new IllegalStateException("Select an enabled layer with opacity > 0");
        int i=id(x,z);
        if(!Double.isFinite(target))throw new IllegalArgumentException("Non-finite edit");
        float old=l.delta[i];
        float next=(float)(old+(target-composed(i))/l.opacity);
        if(!Float.isFinite(next)||old==next)return;
        pending.putIfAbsent(i,old);l.delta[i]=next;dirty=true;
    }
    public void finishStroke(){
        if(pending.isEmpty())return;
        Layer l=layer(active);int count=pending.size(),k=0;
        int[] ids=new int[count];float[] a=new float[count],b=new float[count];
        for(var e:pending.entrySet()){int i=e.getKey();ids[k]=i;a[k]=e.getValue();b[k]=l.delta[i];k++;}
        undo.push(new Change(l,ids,a,b));redo.clear();pending.clear();
    }
    public boolean undo(){
        finishStroke();if(undo.isEmpty())return false;
        Change c=undo.pop();for(int i=0;i<c.indices.length;i++)c.layer.delta[c.indices[i]]=c.before[i];
        redo.push(c);dirty=true;return true;
    }
    public boolean redo(){
        finishStroke();if(redo.isEmpty())return false;
        Change c=redo.pop();for(int i=0;i<c.indices.length;i++)c.layer.delta[c.indices[i]]=c.after[i];
        undo.push(c);dirty=true;return true;
    }
    private static double falloff(double d,double r,double inner,int curve){
        if(d>=r)return 0;
        if(d<=inner*r)return 1;
        double t=1-(d-inner*r)/(r*(1-inner));
        return switch(curve){
            case 1 -> t*t*(3-2*t);
            case 2 -> t*t*t*t;
            default -> t;
        };
    }
    public void brush(Brush mode,double cx,double cz,double radius,double strength,
                      double target,double inner,int curve){
        brush(mode,cx,cz,radius,strength,target,inner,curve,FlattenLimit.BOTH);
    }
    public void brush(Brush mode,double cx,double cz,double radius,double strength,
                      double target,double inner,int curve,FlattenLimit direction){
        brushPrecision(mode,cx,cz,new TerrainBrushProfile.Settings(
            radius,strength,target,inner,curve,1,0,
            -Double.MAX_VALUE,Double.MAX_VALUE,90,direction),null);
    }
    /**
     * Masked, rotated brush with explicit height/slope exclusions. Opacity is
     * applied at every editable vertex and does not change the source heightmap.
     * All dabs within a stroke share the existing sparse undo transaction.
     */
    public void brushPrecision(Brush mode,double cx,double cz,
                               TerrainBrushProfile.Settings settings,
                               TerrainBrushProfile.Alpha alpha){
        Objects.requireNonNull(mode);Objects.requireNonNull(settings);
        double radius=settings.radius(),strength=settings.strength(),
            target=settings.target(),inner=settings.inner();
        int curve=settings.curve();
        if(!Double.isFinite(cx)||!Double.isFinite(cz)||radius>n)
            throw new IllegalArgumentException("Brush outside supported tile scale");
        int x0=Math.max(1,(int)Math.floor(cx-radius)),x1=Math.min(n-1,(int)Math.ceil(cx+radius));
        int z0=Math.max(1,(int)Math.floor(cz-radius)),z1=Math.min(n-1,(int)Math.ceil(cz+radius));
        for(int z=z0;z<=z1;z++)for(int x=x0;x<=x1;x++){
            if(!editable(x,z))continue;
            double h=height(x,z);
            if(h<settings.minElevation()||h>settings.maxElevation()
                ||slope(x,z)>settings.maxSlopeDegrees())continue;
            double f=falloff(Math.hypot(x-cx,z-cz),radius,inner,curve);
            if(alpha!=null)f*=alpha.sample((x-cx)/radius,
                (z-cz)/radius,settings.rotationDegrees());
            if(f<=0)continue;
            double a=Math.min(1,Math.abs(strength)*f),dest=h;
            switch(mode){
                case RAISE -> dest=h+strength*f;
                case LOWER -> dest=h-Math.abs(strength)*f;
                case FLATTEN -> {
                    if(settings.flattenLimit()==FlattenLimit.RAISE_ONLY&&target<=h)break;
                    if(settings.flattenLimit()==FlattenLimit.LOWER_ONLY&&target>=h)break;
                    dest=h+(target-h)*a;
                }
                case REVERT -> dest=h+(sourceMetres(id(x,z))-h)*a;
                case TRENCH -> dest=h-Math.abs(strength)*f;
                case TERRACE -> {
                    double unit=Math.max(.01,Math.abs(target));
                    dest=h+(Math.rint(h/unit)*unit-h)*a;
                }
                case SMOOTH -> {
                    double avg=(height(x-1,z)+height(x+1,z)
                        +height(x,z-1)+height(x,z+1))*.25;
                    dest=h+(avg-h)*a;
                }
            }
            write(x,z,dest);
        }
    }
    private static long mix(long v){
        v=(v^(v>>>30))*0xbf58476d1ce4e5b9L;
        v=(v^(v>>>27))*0x94d049bb133111ebL;return v^(v>>>31);
    }
    private static double grad(int x,int z,long seed,double dx,double dz){
        long h=mix(seed^mix(x*0x9e3779b97f4a7c15L)^mix(z*0xc2b2ae3d27d4eb4fL));
        double angle=(h>>>11)*0x1.0p-53*Math.PI*2;
        return Math.cos(angle)*dx+Math.sin(angle)*dz;
    }
    private static double perlin(double x,double z,long seed){
        int ix=(int)Math.floor(x),iz=(int)Math.floor(z);
        double fx=x-ix,fz=z-iz,u=fx*fx*fx*(fx*(fx*6-15)+10),v=fz*fz*fz*(fz*(fz*6-15)+10);
        double a=grad(ix,iz,seed,fx,fz),b=grad(ix+1,iz,seed,fx-1,fz);
        double c=grad(ix,iz+1,seed,fx,fz-1),d=grad(ix+1,iz+1,seed,fx-1,fz-1);
        return (a+(b-a)*u)*(1-v)+(c+(d-c)*u)*v;
    }
    private static double simplex(double x,double y,long seed){
        double f=(x+y)*.3660254037844386;int i=(int)Math.floor(x+f),j=(int)Math.floor(y+f);
        double g=(i+j)*.2113248654051871,x0=x-(i-g),y0=y-(j-g);
        int i1=x0>y0?1:0,j1=x0>y0?0:1;
        double x1=x0-i1+.2113248654051871,y1=y0-j1+.2113248654051871;
        double x2=x0-1+.4226497308103742,y2=y0-1+.4226497308103742;
        double[] xx={x0,x1,x2},yy={y0,y1,y2};int[] ix={i,i+i1,i+1},iy={j,j+j1,j+1};
        double sum=0;
        for(int k=0;k<3;k++){double t=.5-xx[k]*xx[k]-yy[k]*yy[k];
            if(t>0){t*=t;sum+=t*t*grad(ix[k],iy[k],seed,xx[k],yy[k]);}}
        return 70*sum;
    }
    private static double worley(double x,double z,long seed){
        int ix=(int)Math.floor(x),iz=(int)Math.floor(z);double best=2;
        for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++){
            long a=mix(seed^mix(ix+dx)),b=mix(seed^mix(iz+dz)^a);
            double px=ix+dx+(a>>>11)*0x1.0p-53,pz=iz+dz+(b>>>11)*0x1.0p-53;
            best=Math.min(best,Math.hypot(x-px,z-pz));
        }
        return 1-Math.min(1,best);
    }
    private static double signal(Noise type,double x,double z,long seed){
        return switch(type){
            case PERLIN -> perlin(x,z,seed);
            case SIMPLEX -> simplex(x,z,seed);
            case WORLEY -> worley(x,z,seed)*2-1;
            case RIDGED -> (1-Math.abs(perlin(x,z,seed)*1.5))*2-1;
            case BILLOW -> 2*Math.abs(perlin(x,z,seed)*1.5)-1;
            case DOMAIN_WARP -> perlin(x+perlin(x*.7,z*.7,seed+71)*2,
                z+perlin(x*.7+17,z*.7,seed+89)*2,seed);
        };
    }
    private double fractal(Noise type,long seed,double cycles,int octaves,double gx,double gz){
        double value=0,weight=1,total=0;
        for(int o=0;o<octaves;o++){
            double freq=cycles*Math.pow(2,o)/n;
            value+=weight*signal(type,gx*freq,gz*freq,seed+o*9973L);
            total+=weight;weight*=.5;
        }
        return value/total;
    }
    private static void noiseParameters(double cycles,double amplitude,int octaves){
        if(!Double.isFinite(cycles)||cycles<=0||cycles>512
            ||!Double.isFinite(amplitude)||Math.abs(amplitude)>100000
            ||octaves<1||octaves>8)throw new IllegalArgumentException("Noise parameters");
    }
    public double minimumElevation(){return min;}
    public double maximumElevation(){return max;}
    /** Low-resolution preview is read-only and never touches a layer or source PNG. */
    public double[][] previewNoise(Noise type,long seed,double cycles,double amplitude,
                                   int octaves,int resolution){
        noiseParameters(cycles,amplitude,octaves);
        if(resolution<2||resolution>256)throw new IllegalArgumentException("Preview size");
        double[][] preview=new double[resolution][resolution];
        for(int z=0;z<resolution;z++)for(int x=0;x<resolution;x++){
            int sx=x*n/(resolution-1),sz=z*n/(resolution-1);
            double gx=(long)tx*n+sx,gz=(long)tz*n+sz;
            preview[z][x]=height(sx,sz)+amplitude*fractal(type,seed,cycles,octaves,gx,gz);
        }
        return preview;
    }
    /** Deterministic absolute global coordinates prevent tile-dependent noise seams. */
    public void noise(Noise type,long seed,double cyclesPerTile,double amplitude,int octaves){
        noiseParameters(cyclesPerTile,amplitude,octaves);
        finishStroke();
        for(int z=1;z<n;z++)for(int x=1;x<n;x++){
            if(!editable(x,z))continue;
            double gx=(long)tx*n+x,gz=(long)tz*n+z;
            write(x,z,height(x,z)+
                amplitude*fractal(type,seed,cyclesPerTile,octaves,gx,gz));
        }
        finishStroke();
    }
    /** Bilinear height stamp, soft edge, with explicit local datum matching. */
    public void stamp(float[][] heights,double cx,double cz,double radius,double strength){
        if(heights==null||heights.length<2||heights[0].length<2||radius<=0
            ||!Double.isFinite(strength))throw new IllegalArgumentException("Stamp");
        int w=heights[0].length;
        for(float[] row:heights)if(row.length!=w)throw new IllegalArgumentException("Ragged stamp");
        double midpoint=heights[heights.length/2][w/2];
        for(int z=Math.max(1,(int)(cz-radius));z<=Math.min(n-1,(int)(cz+radius));z++)
            for(int x=Math.max(1,(int)(cx-radius));x<=Math.min(n-1,(int)(cx+radius));x++){
                if(!editable(x,z))continue;
                double dx=(x-cx)/radius,dz=(z-cz)/radius;
                if(Math.abs(dx)>1||Math.abs(dz)>1)continue;
                double sx=(dx+1)*.5*(w-1),sz=(dz+1)*.5*(heights.length-1);
                int ix=(int)Math.floor(sx),iz=(int)Math.floor(sz);
                int jx=Math.min(ix+1,w-1),jz=Math.min(iz+1,heights.length-1);
                double fx=sx-ix,fz=sz-iz;
                double h=(heights[iz][ix]*(1-fx)+heights[iz][jx]*fx)*(1-fz)
                    +(heights[jz][ix]*(1-fx)+heights[jz][jx]*fx)*fz;
                double edge=Math.pow(Math.max(0,1-Math.max(Math.abs(dx),Math.abs(dz))),2);
                write(x,z,height(x,z)+(h-midpoint)*strength*edge);
            }
        finishStroke();
    }
    /** Grade a railway, road, or river polyline; points are local grid positions. */
    public void gradePath(List<Point> points,double halfWidth,double shoulder){
        if(points==null||points.size()<2||points.size()>256||halfWidth<=0||shoulder<0)
            throw new IllegalArgumentException("Path parameters");
        finishStroke();
        for(int z=1;z<n;z++)for(int x=1;x<n;x++){
            if(!editable(x,z))continue;
            double best=Double.POSITIVE_INFINITY,elevation=0;
            for(int k=1;k<points.size();k++){
                Point a=points.get(k-1),b=points.get(k);
                double dx=b.x-a.x,dz=b.z-a.z,ls=dx*dx+dz*dz;
                if(ls<1e-9)continue;
                double t=Math.max(0,Math.min(1,((x-a.x)*dx+(z-a.z)*dz)/ls));
                double distance=Math.hypot(x-a.x-t*dx,z-a.z-t*dz);
                if(distance<best){best=distance;elevation=a.elevation+(b.elevation-a.elevation)*t;}
            }
            if(best>=halfWidth+shoulder)continue;
            double blend=best<=halfWidth?1:shoulder==0?0:
                falloff(best-halfWidth,shoulder,0,1);
            write(x,z,height(x,z)+(elevation-height(x,z))*blend);
        }
        finishStroke();
    }
    private Region validatedRegion(Region region){
        Objects.requireNonNull(region);
        if(region.x0()<1||region.z0()<1||region.x1()>=n||region.z1()>=n)
            throw new IllegalArgumentException("Erosion region must be inside source tile seam");
        return region;
    }
    /** Talus relaxation in the selected tile; frozen pixels and seam vertices never move. */
    public void thermal(int iterations,double talusDegrees,double rate){
        thermal(iterations,talusDegrees,rate,new Region(1,1,n-1,n-1));
    }
    public void thermal(int iterations,double talusDegrees,double rate,Region region){
        region=validatedRegion(region);
        if(iterations<1||iterations>100||talusDegrees<=0||talusDegrees>=89
            ||rate<=0||rate>.25)throw new IllegalArgumentException("Thermal erosion parameters");
        finishStroke();
        double talus=Math.tan(Math.toRadians(talusDegrees))*step;
        for(int pass=0;pass<iterations;pass++){
            float[] flow=new float[pixels];
            for(int z=region.z0();z<=region.z1();z++)for(int x=region.x0();x<=region.x1();x++){
                if(!editable(x,z))continue;
                int i=id(x,z);double h=composed(i);
                int[] xs={x-1,x+1,x,x},zs={z,z,z-1,z+1};
                for(int d=0;d<4;d++){
                    if(!region.contains(xs[d],zs[d])||!editable(xs[d],zs[d]))continue;
                    int neighbor=id(xs[d],zs[d]);
                    double excess=h-composed(neighbor)-talus;
                    if(excess>0){float move=(float)(excess*rate*.25);
                        flow[i]-=move;flow[neighbor]+=move;}
                }
            }
            for(int z=region.z0();z<=region.z1();z++)for(int x=region.x0();x<=region.x1();x++){
                int i=id(x,z);if(flow[i]!=0)write(x,z,composed(i)+flow[i]);
            }
        }
        finishStroke();
    }
    /** Seeded localized droplet erosion with water loss and sediment deposition. */
    public void hydraulic(int droplets,int lifetime,long seed,double erode,double deposit){
        hydraulic(droplets,lifetime,seed,erode,deposit,new Region(1,1,n-1,n-1));
    }
    public void hydraulic(int droplets,int lifetime,long seed,double erode,
                          double deposit,Region region){
        region=validatedRegion(region);
        if(droplets<1||droplets>200000||lifetime<1||lifetime>200
            ||erode<=0||erode>1||deposit<=0||deposit>1)
            throw new IllegalArgumentException("Hydraulic erosion parameters");
        finishStroke();Random random=new Random(seed);
        for(int d=0;d<droplets;d++){
            int x=region.x0()+random.nextInt(region.x1()-region.x0()+1),
                z=region.z0()+random.nextInt(region.z1()-region.z0()+1);
            double sediment=0,water=1;
            for(int stepNo=0;stepNo<lifetime&&water>.01;stepNo++){
                if(!editable(x,z))break;
                double h=height(x,z),lowest=h;int nx=x,nz=z;
                for(int[] off:new int[][]{{-1,0},{1,0},{0,-1},{0,1}}){
                    int xx=x+off[0],zz=z+off[1];
                    if(region.contains(xx,zz)&&editable(xx,zz)&&height(xx,zz)<lowest){
                        lowest=height(xx,zz);nx=xx;nz=zz;
                    }
                }
                if(nx==x&&nz==z){write(x,z,h+sediment*deposit);break;}
                double capacity=(h-lowest)*water*2;
                if(sediment>capacity){double amount=(sediment-capacity)*deposit;
                    write(x,z,h+amount);sediment-=amount;}
                else{double amount=Math.min(h-lowest,(capacity-sediment)*erode);
                    write(x,z,h-amount);sediment+=amount;}
                x=nx;z=nz;water*=.97;
            }
        }
        finishStroke();
    }
    /** Conservative directional soil transport; repeat to simulate dune migration. */
    public void wind(int iterations,int dx,int dz,double amount){
        wind(iterations,dx,dz,amount,new Region(1,1,n-1,n-1));
    }
    public void wind(int iterations,int dx,int dz,double amount,Region region){
        region=validatedRegion(region);
        if(iterations<1||iterations>100||Math.abs(dx)>1||Math.abs(dz)>1
            ||(dx==0&&dz==0)||amount<=0||amount>2)
            throw new IllegalArgumentException("Wind parameters");
        finishStroke();
        for(int pass=0;pass<iterations;pass++){
            float[] flow=new float[pixels];
            for(int z=region.z0();z<=region.z1();z++)for(int x=region.x0();x<=region.x1();x++){
                int nx=x+dx,nz=z+dz;
                if(!editable(x,z)||!region.contains(nx,nz)||!editable(nx,nz))continue;
                double slope=Math.max(0,height(x,z)-height(nx,nz));
                float moved=(float)(Math.min(amount,.02+slope*.02));
                flow[id(x,z)]-=moved;flow[id(nx,nz)]+=moved;
            }
            for(int z=region.z0();z<=region.z1();z++)for(int x=region.x0();x<=region.x1();x++)
                if(flow[id(x,z)]!=0&&editable(x,z))
                    write(x,z,height(x,z)+flow[id(x,z)]);
        }
        finishStroke();
    }
    public Path path(){
        return world.directory().resolve("terrain-layers").resolve("tile_"+tx+"_"+tz+".bin");
    }
    private void load()throws IOException {
        if(!Files.isRegularFile(path()))return;
        if(Files.size(path())>MAX_LAYERS*(long)pixels*4+pixels*2L+32768)
            throw new IOException("Oversized terrain authoring document");
        try(var in=new DataInputStream(new BufferedInputStream(Files.newInputStream(path())))){
            if(in.readInt()!=MAGIC||in.readInt()!=VERSION||in.readInt()!=n
                ||in.readLong()!=sourceCrc)throw new IOException("Incompatible or stale terrain layers");
            int count=in.readInt();
            if(count<1||count>MAX_LAYERS)throw new IOException("Invalid layer count");
            layers.clear();
            for(int k=0;k<count;k++){
                String name=in.readUTF();if(name.isBlank()||name.length()>64)
                    throw new IOException("Invalid layer name");
                Layer l=new Layer(name,pixels);l.enabled=in.readBoolean();l.opacity=in.readFloat();
                if(!Float.isFinite(l.opacity)||l.opacity<0||l.opacity>1)
                    throw new IOException("Invalid layer opacity");
                for(int i=0;i<pixels;i++){
                    l.delta[i]=in.readFloat();
                    if(!Float.isFinite(l.delta[i]))throw new IOException("Non-finite terrain data");
                }
                layers.add(l);
            }
            active=in.readInt();if(active<0||active>=count)throw new IOException("Invalid active layer");
            in.readFully(material);
            for(byte b:material)if((b&255)>=Material.values().length)
                throw new IOException("Unknown material ID");
            int size=in.readInt();if(size<0||size>pixels)throw new IOException("Invalid freeze mask");
            for(int k=0;k<size;k++){int index=in.readInt();
                if(index<0||index>=pixels)throw new IOException("Invalid freeze vertex");
                frozen.set(index);
            }
            if(in.read()!=-1)throw new IOException("Unexpected data after terrain document");
        }
    }
    public void save()throws IOException {
        finishStroke();
        Files.createDirectories(path().getParent());
        Path temp=Files.createTempFile(path().getParent(),".terrain-",".tmp");
        try{
            try(var out=new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(temp)))){
                out.writeInt(MAGIC);out.writeInt(VERSION);out.writeInt(n);out.writeLong(sourceCrc);
                out.writeInt(layers.size());
                for(Layer l:layers){
                    out.writeUTF(l.name);out.writeBoolean(l.enabled);out.writeFloat(l.opacity);
                    for(float v:l.delta)out.writeFloat(v);
                }
                out.writeInt(active);out.write(material);
                out.writeInt(frozen.cardinality());
                for(int i=frozen.nextSetBit(0);i>=0;i=frozen.nextSetBit(i+1))out.writeInt(i);
            }
            Files.move(temp,path(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
            dirty=false;
        }finally{Files.deleteIfExists(temp);}
    }
    /**
     * Explicit irreversible source bake. A single tile cannot own shared seams:
     * reject modifications on any border rather than corrupt adjacent terrain.
     */
    public void bake()throws IOException {
        finishStroke();
        int[] updated=new int[pixels];
        for(int z=0;z<side;z++)for(int x=0;x<side;x++){
            int i=id(x,z);double h=composed(i);
            double raw=(h-min)*65535.0/(max-min);
            if(raw<0||raw>65535||!Double.isFinite(raw))
                throw new IOException("Bake height exceeds 16-bit elevation range");
            updated[i]=(int)Math.round(raw);
            if((x==0||x==n||z==0||z==n)&&updated[i]!=source[i])
                throw new IOException("Source seam changed; multi-tile bake required");
        }
        int[] current=new int[pixels];
        for(int z=0;z<side;z++)for(int x=0;x<side;x++)current[id(x,z)]=world.sample(tx,tz,x,z);
        if(crc(current)!=sourceCrc)throw new IOException("Source changed since editing; reopen tile");
        BufferedImage png=new BufferedImage(side,side,BufferedImage.TYPE_USHORT_GRAY);
        for(int z=0;z<side;z++)for(int x=0;x<side;x++)
            png.getRaster().setSample(x,z,0,updated[id(x,z)]);
        Path temp=Files.createTempFile(world.directory(),".bake-",".png");
        try{
            if(!ImageIO.write(png,"png",temp.toFile()))throw new IOException("PNG writer missing");
            world.importTile(tx,tz,temp,true);
        }finally{Files.deleteIfExists(temp);}
        System.arraycopy(updated,0,source,0,pixels);
        sourceCrc=crc(updated);
        for(Layer l:layers)Arrays.fill(l.delta,0);
        undo.clear();redo.clear();pending.clear();dirty=true;
        // Preserve material IDs, frozen geometry and layer configuration. Only
        // height deltas are rebased against the new authoritative PNG.
        save();
    }
}
