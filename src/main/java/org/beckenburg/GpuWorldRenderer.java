package org.beckenburg;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Game-owned backend contract. The Swing HUD, simulation, evidence and LAN
 * authority are independent of the rasterizer. A real OpenGL implementation
 * is installed at startup by the separately packaged GPU entry point.
 */
public interface GpuWorldRenderer {
    /** Interleaved world/camera-space x,y,z, UV and RGB.
     * resourceId=0 preserves the legacy per-frame batch contract.
     * A positive ID identifies a scene-owned resident VBO; revision changes
     * are the caller's explicit guarantee that changed vertex/light data
     * must be uploaded. A draw-only resident descriptor carries no array.
     * Never use an unchanged revision after editing geometry or lighting. */
    record Surface(int material,float[] vertices,long resourceId,long revision,int vertexCount) {
        public Surface(int material,float[] vertices){
            this(material,Objects.requireNonNull(vertices),0,0,vertices.length/8);
        }
        public Surface(int material,float[] vertices,long resourceId,long revision){
            this(material,Objects.requireNonNull(vertices),resourceId,revision,vertices.length/8);
        }
        public static Surface drawResident(int material,long resourceId,long revision,int vertexCount){
            return new Surface(material,new float[0],resourceId,revision,vertexCount);
        }
        public Surface {
            Objects.requireNonNull(vertices);
            if(vertices.length%24!=0||resourceId<0||revision<0
                ||vertexCount<0||vertexCount%3!=0
                ||(resourceId==0&&(revision!=0||vertexCount!=vertices.length/8))
                ||(resourceId>0&&(revision==0||vertexCount==0
                    ||(vertices.length>0&&vertexCount!=vertices.length/8))))
                throw new IllegalArgumentException("Invalid GPU triangle batch or residency descriptor");
        }
    }
    /**
     * World-space sources carry a camera transform for vertex-shader execution.
     * Keeping camera uniforms separate from vertex data is the first step
     * toward persistent per-level VBOs; legacy camera-space callers retain
     * their existing eight-argument constructor.
     */
    record Frame(long sceneId,long frameId,int width,int height,float focal,float vertexSnap,
                 float affineWarp,List<Surface> surfaces,boolean worldSpace,
                 float cameraX,float cameraY,float cameraZ,float yaw,float pitch,
                 boolean lampOn,float lampIntensity,float lampReach,float lampWidth,boolean flameLamp) {
        public Frame(long sceneId,long frameId,int width,int height,float focal,
                     float vertexSnap,float affineWarp,List<Surface> surfaces) {
            this(sceneId,frameId,width,height,focal,vertexSnap,affineWarp,
                surfaces,false,0,0,0,0,0,false,0,1,1,false);
        }
        public Frame(long sceneId,long frameId,int width,int height,float focal,
                     float vertexSnap,float affineWarp,List<Surface> surfaces,
                     boolean worldSpace,float cameraX,float cameraY,float cameraZ,
                     float yaw,float pitch){
            this(sceneId,frameId,width,height,focal,vertexSnap,affineWarp,
                surfaces,worldSpace,cameraX,cameraY,cameraZ,yaw,pitch,
                false,0,1,1,false);
        }
        public Frame {
            if(sceneId<=0||frameId<=0||width<160||height<120||!Float.isFinite(focal)||focal<=0
                ||!Float.isFinite(vertexSnap)||vertexSnap<0
                ||!Float.isFinite(affineWarp)||affineWarp<0||affineWarp>1
                ||surfaces==null||!Float.isFinite(cameraX)||!Float.isFinite(cameraY)
                ||!Float.isFinite(cameraZ)||!Float.isFinite(yaw)||!Float.isFinite(pitch)
                ||Math.abs(pitch)>Math.PI/2+0.01
                ||!Float.isFinite(lampIntensity)||lampIntensity<0
                ||!Float.isFinite(lampReach)||lampReach<=0
                ||!Float.isFinite(lampWidth)||lampWidth<=0)
                throw new IllegalArgumentException("Bad GPU scene frame");
            surfaces=List.copyOf(surfaces);
        }
    }
    /** ARGB with transparent clear pixels; linearized camera depth in metres. */
    record Result(long sceneId,long frameId,int width,int height,int[] argb,float[] depth) {
        public Result {
            if(sceneId<=0||frameId<=0||width<160||height<120
                ||argb==null||depth==null||argb.length!=width*height||depth.length!=argb.length)
                throw new IllegalArgumentException("Invalid GPU readback");
        }
    }
    /** Unique identities prevent one scene renderer from accepting another's
     * stale color/depth readback when changing maps or returning to the castle. */
    java.util.concurrent.atomic.AtomicLong NEXT_ID=new java.util.concurrent.atomic.AtomicLong();
    static long nextId(){return NEXT_ID.incrementAndGet();}
    static boolean belongsTo(Result result,long sceneId,int width,int height){
        return result!=null&&result.sceneId()==sceneId
            &&result.width()==width&&result.height()==height;
    }
    AtomicReference<GpuWorldRenderer> INSTALLED=new AtomicReference<>();
    static void install(GpuWorldRenderer backend) {
        if(!INSTALLED.compareAndSet(null,Objects.requireNonNull(backend)))
            throw new IllegalStateException("A GPU backend is already installed");
    }
    static GpuWorldRenderer active() {return INSTALLED.get();}
    boolean ready();
    /** Nonblocking: newest authoritative scene supersedes an unrendered scene. */
    void submit(Frame frame);
    /** Last completed hardware framebuffer; can be null during initial startup. */
    Result latest();
    String rendererDescription();
}
