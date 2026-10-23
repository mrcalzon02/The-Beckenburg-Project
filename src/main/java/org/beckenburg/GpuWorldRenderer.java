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
    /** Interleaved camera-space x,y,z, UV u,v and lit RGB components. */
    record Surface(int material,float[] vertices) {
        public Surface {
            Objects.requireNonNull(vertices);
            if(vertices.length%24!=0)throw new IllegalArgumentException("Invalid GPU triangle batch");
        }
    }
    record Frame(long sceneId,long frameId,int width,int height,float focal,float vertexSnap,float affineWarp,List<Surface> surfaces) {
        public Frame {
            if(sceneId<=0||frameId<=0||width<160||height<120||!Float.isFinite(focal)||focal<=0
                ||!Float.isFinite(vertexSnap)||vertexSnap<0
                ||!Float.isFinite(affineWarp)||affineWarp<0||affineWarp>1
                ||surfaces==null)throw new IllegalArgumentException("Bad GPU scene frame");
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
