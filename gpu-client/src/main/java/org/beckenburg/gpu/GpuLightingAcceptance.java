package org.beckenburg.gpu;

import org.beckenburg.GpuWorldRenderer;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/** Actual GL pixels for global lighting and recovery after asynchronous eviction. */
final class GpuLightingAcceptance {
    static GpuWorldRenderer.Result frame(OpenGlWorldRenderer engine,long scene,
            List<GpuWorldRenderer.Surface> surfaces,boolean lamp,float scale)throws Exception{
        long id=GpuWorldRenderer.nextId();
        engine.submit(new GpuWorldRenderer.Frame(scene,id,160,120,100,0,0,surfaces,
            false,0,0,0,0,0,lamp,.4f,10,1,false,scale));
        long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);
        while(System.nanoTime()<until){var result=engine.latest();
            if(result!=null&&result.sceneId()==scene&&result.frameId()==id)return result;
            LockSupport.parkNanos(200000);
        }
        throw new AssertionError("GL light/recovery frame did not complete");
    }
    static void verify(OpenGlWorldRenderer engine,Path output)throws Exception{
        var csv=new StringBuilder("kind,full_red,quarter_red,quarter_static_upload_bytes\n");
        long scene=GpuWorldRenderer.nextId();
        for(int kind=0;kind<3;kind++){
            float light=kind==0?.35f:kind==1?.05f:.8f;
            float[] vertices={-1,-1,2,0,0,light,light,light,
                1,-1,2,1,0,light,light,light,0,1,2,.5f,1,light,light,light};
            long resource=GpuWorldRenderer.nextId();
            var upload=new GpuWorldRenderer.Surface(-1,vertices,resource,1);
            var resident=GpuWorldRenderer.Surface.drawResident(-1,resource,1,vertices);
            var full=frame(engine,scene,List.of(upload),kind==1,1);
            var quarter=frame(engine,scene,List.of(resident),kind==1,.25f);
            int index=60*160+80,a=(full.argb()[index]>>16)&255,b=(quarter.argb()[index]>>16)&255;
            long bytes=engine.residentUploadStats().uploadedBytes();
            if(a<10||Math.abs(b-a*.25)>2||bytes!=0||!Arrays.equals(full.depth(),quarter.depth()))
                throw new AssertionError("Global light must scale fixture, flashlight and emission without vertex/depth changes: kind="+kind);
            csv.append(kind).append(',').append(a).append(',').append(b).append(',').append(bytes).append('\n');
            if(kind==0){
                // Dispose this scene between producer-side handle inspection
                // and the next draw, exactly as a foreign CRT viewport can.
                frame(engine,GpuWorldRenderer.nextId(),List.of(),false,1);
                var recovered=frame(engine,scene,List.of(resident),false,1);
                if(!engine.ready()||engine.residentUploadStats().uploadedBytes()!=vertices.length*4L
                    ||!Arrays.equals(full.argb(),recovered.argb()))
                    throw new AssertionError("Evicted draw-only handle did not recover its immutable payload");
            }
        }
        Files.writeString(output.resolve("gpu-light-scale.csv"),csv.toString());
        engine.releaseScene(scene);
        System.out.println("PASS actual GL global fixture/flashlight/emission scaling and evicted resident handle recovery");
    }
}
