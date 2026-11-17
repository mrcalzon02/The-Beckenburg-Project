package org.beckenburg.editor;

import com.sun.management.ThreadMXBean;
import java.awt.image.BufferedImage;
import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.beckenburg.editor.EditorTopologyProject.*;

/** Diagnostic only: actual Material Editor canvas repaint, with no timing release gate.
 * Run in fresh JVMs with the same classes/resources, changing only the workspace class.
 * Source construction, observe/list layout, allocation counters and hashes are outside timing.
 */
public final class EditorUvPaintProfile {
    private static State grid(boolean tiled) {
        int columns=300,rows=200;
        var seed=EditorTopologyProject.fromMesh(new EditorMeshPipeline.Mesh("paint",List.of(),0),
            "0".repeat(64),Path.of("paint-profile.btop")).snapshot();
        var vertices=new ArrayList<Vertex>();
        var faces=new ArrayList<Face>();
        for(int y=0;y<=rows;y++)for(int x=0;x<=columns;x++)vertices.add(new Vertex(vertices.size(),"grid",x,y,0));
        for(int y=0;y<rows;y++)for(int x=0;x<columns;x++) {
            int a=y*(columns+1)+x,b=a+1,c=a+columns+2,d=c-1;
            double u=x/(tiled?1.:columns),v=y/(tiled?1.:rows);
            double du=1/(tiled?1.:columns),dv=1/(tiled?1.:rows);
            faces.add(new Face(faces.size(),"grid",new Corner(a,u,v),new Corner(b,u+du,v),new Corner(c,u+du,v+dv)));
            faces.add(new Face(faces.size(),"grid",new Corner(a,u,v),new Corner(c,u+du,v+dv),new Corner(d,u,v+dv)));
        }
        // Pins and seams exercise the same projected endpoints in a second pass.
        var f=faces.get(60300);
        faces.set(60300,new Face(f.id(),f.part(),new Corner(f.a().vertex(),f.a().u(),f.a().v(),null,true),f.b(),f.c(),f.material(),7));
        return new State(seed.sceneId(),seed.sourceFingerprint(),"paint",0,vertices,faces);
    }

    private static String hash(BufferedImage image)throws Exception {
        var bytes=ByteBuffer.allocate(image.getWidth()*image.getHeight()*4);
        for(int pixel:image.getRGB(0,0,image.getWidth(),image.getHeight(),null,0,image.getWidth()))bytes.putInt(pixel);
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes.array()));
    }

    private static void run(Path output)throws Exception {
        Files.createDirectories(output);
        var clock=(ThreadMXBean)ManagementFactory.getThreadMXBean();
        if(!clock.isThreadAllocatedMemorySupported()||!clock.isCurrentThreadCpuTimeSupported())
            throw new IllegalStateException("Allocation and CPU clocks required");
        clock.setThreadAllocatedMemoryEnabled(true);clock.setThreadCpuTimeEnabled(true);
        try(var out=Files.newBufferedWriter(output.resolve("paint.csv"));
            var changed=Files.newBufferedWriter(output.resolve("invalidated.csv"))) {
            out.write("case,sample,faces,width,height,wall_ns,cpu_ns,allocated_bytes,argb_sha256\n");
            changed.write("case,operation,faces,wall_ns,cpu_ns,allocated_bytes,argb_sha256\n");
            for(boolean tiled:new boolean[]{false,true}) {
                var state=grid(tiled);
                var view=new EditorMaterialWorkspace(new JPanel(),new JPanel(),new JPanel(),id->{throw new AssertionError("Unexpected edit callback");});
                view.observe(state,60300L,null,false);
                var field=EditorMaterialWorkspace.class.getDeclaredField("canvas");field.setAccessible(true);
                var canvas=(JPanel)field.get(view);canvas.setSize(520,500);
                for(boolean guides:new boolean[]{false,true}) {
                    view.showUvGuides(guides,guides);
                    String name=(tiled?"tiled":"atlas")+(guides?"-guides":"-edges");
                    for(int sample=-6;sample<12;sample++) {
                        var image=new BufferedImage(520,500,BufferedImage.TYPE_INT_ARGB);
                        var graphics=image.createGraphics();
                        long allocated=clock.getCurrentThreadAllocatedBytes(),cpu=clock.getCurrentThreadCpuTime(),start=System.nanoTime();
                        canvas.paint(graphics);
                        long elapsed=System.nanoTime()-start;cpu=clock.getCurrentThreadCpuTime()-cpu;
                        allocated=clock.getCurrentThreadAllocatedBytes()-allocated;
                        graphics.dispose();
                        if(sample<0)continue;
                        out.write(String.format(Locale.ROOT,"%s,%d,%d,520,500,%d,%d,%d,%s%n",name,sample,state.faces().size(),elapsed,cpu,allocated,hash(image)));
                        if(sample==0)ImageIO.write(image,"png",output.resolve(name+".png").toFile());
                    }
                    for(int step=0;step<2;step++){
                        view.observe(state,step==0?60301L:60300L,null,false);
                        var image=new BufferedImage(520,500,BufferedImage.TYPE_INT_ARGB);var graphics=image.createGraphics();
                        long allocated=clock.getCurrentThreadAllocatedBytes(),cpu=clock.getCurrentThreadCpuTime(),start=System.nanoTime();
                        canvas.paint(graphics);
                        long elapsed=System.nanoTime()-start;cpu=clock.getCurrentThreadCpuTime()-cpu;
                        allocated=clock.getCurrentThreadAllocatedBytes()-allocated;graphics.dispose();
                        changed.write(String.format(Locale.ROOT,"%s,%s,%d,%d,%d,%d,%s%n",name,step==0?"select-other":"select-original",
                            state.faces().size(),elapsed,cpu,allocated,hash(image)));
                    }
                }
            }
        }
    }
    private static void changedFrames(Path output)throws Exception {
        Files.createDirectories(output);
        var clock=(ThreadMXBean)ManagementFactory.getThreadMXBean();
        clock.setThreadAllocatedMemoryEnabled(true);clock.setThreadCpuTimeEnabled(true);
        try(var out=Files.newBufferedWriter(output.resolve("changes.csv"))){
            out.write("case,operation,sample,wall_ns,cpu_ns,allocated_bytes,argb_sha256\n");
            for(boolean tiled:new boolean[]{false,true}){
                var base=grid(tiled);
                int id=tiled?0:60300;
                var faces=new ArrayList<>(base.faces());
                var f=faces.get(id);var a=f.a();
                faces.set(id,new Face(f.id(),f.part(),new Corner(a.vertex(),a.u()+.011,a.v()-.007,a.normal(),!a.pinned()),
                    f.b(),f.c(),f.material(),f.seamMask()^1));
                var edited=new State(base.sceneId(),base.sourceFingerprint(),base.name(),0,base.vertices(),faces);
                var view=new EditorMaterialWorkspace(new JPanel(),new JPanel(),new JPanel(),unused->{});
                var field=EditorMaterialWorkspace.class.getDeclaredField("canvas");field.setAccessible(true);
                var canvas=(JPanel)field.get(view);canvas.setSize(520,500);
                for(boolean guides:new boolean[]{false,true}){
                    view.showUvGuides(guides,guides);
                    String name=(tiled?"tiled":"atlas")+(guides?"-guides":"-edges");
                    var image=new BufferedImage(520,500,BufferedImage.TYPE_INT_ARGB);
                    var graphics=image.createGraphics();
                    try{
                        view.observe(base,(long)id,null,false);canvas.paint(graphics);
                        // Every four operations return to exactly the initial document and face.
                        String[] operations={"select","reselect","edit","undo"};
                        for(int cycle=-1;cycle<6;cycle++)for(int step=0;step<operations.length;step++){
                            long allocated=clock.getCurrentThreadAllocatedBytes(),cpu=clock.getCurrentThreadCpuTime(),start=System.nanoTime();
                            view.observe(step==2?edited:base,(long)(step==0?id+1:id),null,step==2);
                            canvas.paint(graphics);
                            long elapsed=System.nanoTime()-start;cpu=clock.getCurrentThreadCpuTime()-cpu;
                            allocated=clock.getCurrentThreadAllocatedBytes()-allocated;
                            if(cycle>=0)out.write(String.format(Locale.ROOT,"%s,%s,%d,%d,%d,%d,%s%n",
                                name,operations[step],cycle,elapsed,cpu,allocated,hash(image)));
                        }
                    }finally{graphics.dispose();}
                }
            }
        }
    }
    public static void main(String[] args)throws Exception {
        boolean changes=args.length==2 && args[0].equals("--changes");
        if(!changes && args.length!=1)throw new IllegalArgumentException("Expected [--changes] output-directory");
        SwingUtilities.invokeAndWait(()->{try{if(changes)changedFrames(Path.of(args[1]));else run(Path.of(args[0]));}catch(Exception e){throw new RuntimeException(e);}});
    }
}
