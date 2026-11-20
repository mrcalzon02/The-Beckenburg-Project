package org.beckenburg;

/**
 * Privacy-bounded, once-per-recording hardware and build provenance.
 * No usernames, home paths, hostnames, tokens or network requests are read.
 * Rendering and simulation cannot be affected by this snapshot.
 */
final class FrameCaptureProvenance {
    record Snapshot(String sourceRevision,String renderer,String os,String javaVersion,
                    String detail,String display,String aspect,String upscale,
                    String antialias,String psxPreset,int fieldOfView,
                    int presentationTargetHz) {
        Snapshot {
            sourceRevision=clean(sourceRevision);
            renderer=clean(renderer);
            os=clean(os);
            javaVersion=clean(javaVersion);
            detail=clean(detail);
            display=clean(display);
            aspect=clean(aspect);
            upscale=clean(upscale);
            antialias=clean(antialias);
            psxPreset=clean(psxPreset);
        }
    }
    private FrameCaptureProvenance(){}

    /** Defend the line-based CSV metadata against arbitrary driver text. */
    static String clean(String value){
        if(value==null||value.isBlank())return "unavailable";
        StringBuilder out=new StringBuilder(Math.min(192,value.length()));
        for(int i=0;i<value.length()&&out.length()<192;i++){
            char c=value.charAt(i);
            out.append(c>=32&&c!=127&&c!='='&&c!='#'&&c!='\\'&&c!=','?c:' ');
        }
        return out.toString().trim();
    }
    static Snapshot unavailable(){
        return new Snapshot("unknown","unknown","unknown","unknown",
            "unknown","unknown","unknown","unknown","unknown","unknown",0,0);
    }
    static Snapshot current(){
        GraphicsOptions options=GraphicsOptions.current();
        GpuWorldRenderer backend=GpuWorldRenderer.active();
        String renderer="software";
        if(backend!=null){
            try{renderer=backend.ready()?backend.rendererDescription():"GPU initializing";}
            catch(RuntimeException unavailable){renderer="GPU unavailable";}
        }
        String os=System.getProperty("os.name","unknown")+" "+
            System.getProperty("os.version","unknown")+" / "+
            System.getProperty("os.arch","unknown");
        return new Snapshot(BeckenburgDiagnostics.sourceRevision(),renderer,os,
            System.getProperty("java.version","unknown"),options.detail().name(),
            options.display().name(),options.aspect().name(),options.upscale().name(),
            options.antialias().name(),PsxPipeline.selected().name(),
            options.fieldOfView(),PresentationRate.current().hz());
    }
}
