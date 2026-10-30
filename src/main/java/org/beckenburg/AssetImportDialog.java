package org.beckenburg;

import org.beckenburg.editor.EditorGlbWriter;
import org.beckenburg.editor.EditorMeshPipeline;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Live asset import dock/dialog for the actual Beckenburg editor.
 * Preview-only reduction never mutates the source; only a confirmed publish
 * registers a derived GLB for the existing game rasterizer.
 */
public final class AssetImportDialog {
    private final JFrame parent;
    private final Consumer<String> published;
    private final JDialog dialog;
    private final JSlider slider=new JSlider(0,12,0);
    private final JComboBox<String> preset=new JComboBox<>(new String[]{"PSX Prop / 100 triangles","PSX Character / 800 triangles","PSX Close-up / 1200 triangles","PSX World Kit / 4000 triangles","Modern / no style budget"});
    private final JTextArea details=new JTextArea(10,40);
    private final JLabel pathLabel=new JLabel("Choose an OBJ, STL or embedded-texture GLB");
    private final JButton apply=new JButton("PUBLISH TO EDITOR");
    private final JButton choose=new JButton("CHOOSE MODEL");
    private final JPanel preview=new JPanel(){
        @Override public void paintComponent(Graphics g){
            super.paintComponent(g);
            Graphics2D p=(Graphics2D)g.create();
            p.setColor(new Color(20,24,29));p.fillRect(0,0,getWidth(),getHeight());
            if(source!=null)draw(p,source.triangles(),0,getWidth()/2,"SOURCE");
            if(result!=null)draw(p,result.optimized().triangles(),getWidth()/2,getWidth()-getWidth()/2,"PREVIEW");
            p.dispose();
        }
    };
    private Path path;
    private EditorMeshPipeline.Mesh source;
    private EditorMeshPipeline.Result result;
    private int revision;
    private boolean busy;
    private volatile long sourceTextureBytes;
    private AssetImportDialog(JFrame parent,Consumer<String> published){
        this.parent=parent;this.published=published;
        dialog=new JDialog(parent,"Asset Import / PSX Regression Lab",false);
        JPanel content=new JPanel(new BorderLayout(5,5));
        JPanel head=new JPanel(new BorderLayout(5,4));
        head.add(pathLabel,BorderLayout.CENTER);
        choose.addActionListener(e->openSource());
        head.add(choose,BorderLayout.EAST);
        slider.setPaintTicks(true);slider.setMajorTickSpacing(2);slider.setPaintLabels(true);
        slider.setToolTipText("Level 0 preserves source geometry; 12 intentionally collapses small parts. This is not a triangle-budget target.");
        slider.addChangeListener(e->{if(!slider.getValueIsAdjusting()&&source!=null)recalculate();});
        preset.setToolTipText("Advisory visual/style budget only; never a hard release gate");
        preset.addActionListener(e->{if(source!=null&&result!=null)updateReport();});
        JPanel controls=new JPanel(new BorderLayout(3,3));
        controls.add(new JLabel("Geometry regression / vertex clustering (0 = original)"),BorderLayout.NORTH);
        controls.add(slider,BorderLayout.CENTER);
        JButton reset=new JButton("RESET");
        reset.addActionListener(e->slider.setValue(0));
        controls.add(reset,BorderLayout.EAST);
        controls.add(preset,BorderLayout.SOUTH);
        JPanel top=new JPanel(new BorderLayout(4,5));
        top.add(head,BorderLayout.NORTH);top.add(controls,BorderLayout.SOUTH);
        content.add(top,BorderLayout.NORTH);
        preview.setPreferredSize(new Dimension(780,310));
        JPanel middle=new JPanel(new BorderLayout(4,4));
        middle.add(preview,BorderLayout.CENTER);
        details.setEditable(false);details.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12));
        middle.add(new JScrollPane(details),BorderLayout.SOUTH);
        content.add(middle,BorderLayout.CENTER);
        JPanel footer=new JPanel(new FlowLayout(FlowLayout.RIGHT,4,4));
        JButton close=new JButton("CLOSE");close.addActionListener(e->dialog.dispose());
        apply.setEnabled(false);apply.addActionListener(e->publish());
        footer.add(new JLabel("Original retained; source and derivative are stored separately"));
        footer.add(apply);footer.add(close);
        content.add(footer,BorderLayout.SOUTH);
        content.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));
        dialog.setContentPane(content);dialog.setSize(910,680);
        dialog.setLocationRelativeTo(parent);
    }
    public static void show(JFrame parent,Consumer<String> published){
        new AssetImportDialog(parent,published).dialog.setVisible(true);
    }
    private void openSource(){
        JFileChooser file=new JFileChooser();
        file.setDialogTitle("Import a mesh source (OBJ, STL, GLB)");
        file.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "Mesh files (*.obj, *.stl, *.glb)","obj","stl","glb"));
        if(file.showOpenDialog(dialog)!=JFileChooser.APPROVE_OPTION)return;
        Path candidate=file.getSelectedFile().toPath();
        busy=true;choose.setEnabled(false);apply.setEnabled(false);
        pathLabel.setText("Reading "+candidate.getFileName()+"...");
        new SwingWorker<EditorMeshPipeline.Mesh,Void>(){
            @Override protected EditorMeshPipeline.Mesh doInBackground()throws Exception{
                String extension=candidate.getFileName().toString().toLowerCase(Locale.ROOT);
                if(extension.endsWith(".glb")){
                    if(Files.size(candidate)>4_000_000)throw new IOException("Current game GLB decoder limit is 4 MiB");
                    byte[] bytes=Files.readAllBytes(candidate);
                    var decoded=GlbProps.decode(bytes);
                    java.util.Set<Integer> mats=new java.util.HashSet<>();
                    long texBytes=0;
                    for(var triangle:decoded.triangles())mats.add(triangle.material());
                    for(int material:mats)
                        texBytes+=(long)GlbProps.textureWidth(material)*GlbProps.textureHeight(material)*4;
                    sourceTextureBytes=texBytes;
                    List<EditorMeshPipeline.Triangle> all=new ArrayList<>();
                    for(var triangle:decoded.triangles()){
                        var a=triangle.a();var b=triangle.b();var c=triangle.c();
                        all.add(new EditorMeshPipeline.Triangle("GLB assembly",
                            new EditorMeshPipeline.Vertex(a.x(),a.y(),a.z(),a.u(),a.v()),
                            new EditorMeshPipeline.Vertex(b.x(),b.y(),b.z(),b.u(),b.v()),
                            new EditorMeshPipeline.Vertex(c.x(),c.y(),c.z(),c.u(),c.v())));
                    }
                    return new EditorMeshPipeline.Mesh(candidate.getFileName().toString(),all,bytes.length);
                }
                sourceTextureBytes=0;
                return EditorMeshPipeline.read(candidate);
            }
            @Override protected void done(){
                choose.setEnabled(true);busy=false;
                try{
                    source=get();path=candidate;slider.setValue(0);
                    pathLabel.setText(candidate.getFileName()+" | "+source.triangles().size()+" source triangles");
                    recalculate();
                }catch(Exception e){
                    source=null;result=null;path=null;apply.setEnabled(false);
                    details.setText("IMPORT ERROR: "+rootMessage(e));
                    pathLabel.setText("Unable to read "+candidate.getFileName());
                    preview.repaint();
                }
            }
        }.execute();
    }
    private void recalculate(){
        if(source==null||busy)return;
        final EditorMeshPipeline.Mesh mesh=source;
        final int level=slider.getValue(),request=++revision;
        apply.setEnabled(false);details.setText("Computing regression level "+level+"...");
        new SwingWorker<EditorMeshPipeline.Result,Void>(){
            @Override protected EditorMeshPipeline.Result doInBackground(){
                return EditorMeshPipeline.regress(mesh,level);
            }
            @Override protected void done(){
                if(request!=revision||mesh!=source)return;
                try{
                    result=get();
                    int after=result.parts().stream().mapToInt(EditorMeshPipeline.PartStats::after).sum();
                    StringBuilder report=new StringBuilder();
                    report.append("PSX look preset: ").append(preset.getSelectedItem())
                          .append(" (advisory; the game is not running on real PS1 hardware)\\n");
                    int[] limits={100,800,1200,4000,Integer.MAX_VALUE};
                    int max=limits[preset.getSelectedIndex()];
                    if(after>max)report.append("STYLE WARNING: ").append(after)
                        .append(" triangles exceeds the chosen ").append(max)
                        .append("-triangle visual target.\\n");
                    if(sourceTextureBytes>512*1024)report.append("TEXTURE MEMORY WARNING: approx ")
                        .append(sourceTextureBytes/1024)
                        .append(" KiB decoded GLB RGBA texture pixels (excludes renderer/cache copies).\\n");
                    report.append("Source: ").append(mesh.name()).append("  /  ").append(mesh.sourceBytes()).append(" bytes")
                          .append("\nGeometry: ").append(mesh.triangles().size()).append(" -> ").append(after)
                          .append(" triangles  |  estimated CPU triangle storage: ")
                          .append(result.sourceMemoryBytes()/1024).append(" -> ")
                          .append(result.optimizedMemoryBytes()/1024).append(" KiB\n");
                    for(var part:result.parts())report.append(part.overRegression()?"!! ":"   ")
                        .append(part.name()).append(": ").append(part.before()).append(" -> ")
                        .append(part.after()).append(" triangles, ").append(part.verticesBefore())
                        .append(" -> ").append(part.verticesAfter()).append(" unique positions\n");
                    for(String warning:result.warnings())report.append("WARNING: ").append(warning).append("\n");
                    if(mesh.name().toLowerCase(Locale.ROOT).endsWith(".glb")&&level>0)
                        report.append("WARNING: GLB material and named-node identities are not carried into regenerated fallback textures.\n");
                    if(mesh.name().toLowerCase(Locale.ROOT).endsWith(".obj"))
                        report.append("NOTE: OBJ source MTL/texture links are not yet converted; uses a generated low-res fallback atlas.\n");
                    if(mesh.name().toLowerCase(Locale.ROOT).endsWith(".stl"))
                        report.append("NOTE: STL contains no UV/material atlas; uses a generated low-res fallback atlas.\n");
                    details.setText(report.toString());details.setCaretPosition(0);
                    apply.setEnabled(after>0);
                    preview.repaint();
                }catch(Exception err){details.setText("REGRESSION ERROR: "+rootMessage(err));apply.setEnabled(false);}
            }
        }.execute();
    }
    /** Update budget commentary without rerunning expensive vertex clustering. */
    private void updateReport(){
        if(source==null||result==null)return;
        // Result is immutable; update the already visible budget line if preset changes
        // by recalculating in the background, not on the GUI paint thread.
        recalculate();
    }

    private void publish(){
        if(source==null||result==null||path==null||busy)return;
        final int remaining=result.parts().stream().mapToInt(EditorMeshPipeline.PartStats::after).sum();
        if(remaining==0)return;
        boolean lossyMaterial=source.name().toLowerCase(Locale.ROOT).endsWith(".glb")&&result.level()>0;
        if(result.overRegressed()||lossyMaterial){
            String msg=(result.overRegressed()?
                "OVER-REGRESSION: one or more named parts have collapsed.\n":"")+
                (lossyMaterial?"Original GLB material/part identity will be replaced by a fallback atlas.\n":"")+
                "The untouched source will be preserved. Publish this destructive-looking variant anyway?";
            if(JOptionPane.showConfirmDialog(dialog,msg,"Explicit lossy import warning",
                JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE)!=JOptionPane.YES_OPTION)return;
        }
        final Path original=path;final EditorMeshPipeline.Result chosen=result;
        choose.setEnabled(false);apply.setEnabled(false);busy=true;
        details.append("\nPublishing validated GLB derivative...");
        new SwingWorker<String,Void>(){
            @Override protected String doInBackground()throws Exception{
                byte[] output=chosen.level()==0&&original.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".glb")
                    ?Files.readAllBytes(original):EditorGlbWriter.encode(chosen.optimized());
                // Use the exact decoder that live gameplay will use. No mock preview-only mesh.
                GlbProps.decode(output);
                int after=chosen.parts().stream().mapToInt(EditorMeshPipeline.PartStats::after).sum();
                return ImportedAssetLibrary.publish(original,output,chosen.level(),
                    chosen.source().triangles().size(),after,String.join(" | ",chosen.warnings()));
            }
            @Override protected void done(){
                choose.setEnabled(true);busy=false;
                try{
                    String id=get();published.accept(id);
                    details.append("\nPUBLISHED "+id+" to "+ImportedAssetLibrary.root()+
                        "\nSelect the model in the editor and place it with the PROP tool.");
                }catch(Exception failure){
                    details.append("\nPUBLISH FAILED: "+rootMessage(failure));
                    apply.setEnabled(true);
                }
            }
        }.execute();
    }
    private static String rootMessage(Throwable failure){
        while(failure.getCause()!=null)failure=failure.getCause();
        return String.valueOf(failure.getMessage());
    }
    /** Both panels use the SOURCE bounds: shrinking topology cannot trick the eye
     * by zooming the remaining stump to fill the frame. */
    private void draw(Graphics2D g,List<EditorMeshPipeline.Triangle> tris,int left,int width,String title){
        List<EditorMeshPipeline.Triangle> reference=source.triangles();
        double xmin=Double.POSITIVE_INFINITY,ymin=xmin,xmax=Double.NEGATIVE_INFINITY,ymax=xmax;
        for(var t:reference)for(var v:new EditorMeshPipeline.Vertex[]{t.a(),t.b(),t.c()}){
            double ax=v.x()*.83-v.z()*.55,ay=-(v.y()*.95+v.x()*.24+v.z()*.35);
            xmin=Math.min(xmin,ax);xmax=Math.max(xmax,ax);
            ymin=Math.min(ymin,ay);ymax=Math.max(ymax,ay);
        }
        double scale=Math.min((width-26)/Math.max(1e-6,xmax-xmin),
                              (getPreviewHeight()-36)/Math.max(1e-6,ymax-ymin));
        double centerX=(xmin+xmax)*.5,centerY=(ymin+ymax)*.5;
        g.setColor(new Color(181,206,213));
        g.drawString(title+"  /  "+tris.size()+" triangles",left+10,18);
        int skip=Math.max(1,tris.size()/12000);
        g.setColor(new Color(113,168,188,120));
        for(int i=0;i<tris.size();i+=skip){
            var t=tris.get(i);
            int[] xs=new int[3],ys=new int[3];int k=0;
            for(var v:new EditorMeshPipeline.Vertex[]{t.a(),t.b(),t.c()}){
                double ax=v.x()*.83-v.z()*.55,ay=-(v.y()*.95+v.x()*.24+v.z()*.35);
                xs[k]=left+width/2+(int)Math.round((ax-centerX)*scale);
                ys[k]=getPreviewHeight()/2+(int)Math.round((ay-centerY)*scale);k++;
            }
            g.drawPolygon(xs,ys,3);
        }
        if(result!=null&&title.equals("PREVIEW")){
            java.util.Map<String,double[]> centroids=new java.util.LinkedHashMap<>();
            for(var part:result.parts())if(part.after()==0){
                double[] accum=new double[4];
                for(var t:reference)if(t.part().equals(part.name()))
                    for(var v:new EditorMeshPipeline.Vertex[]{t.a(),t.b(),t.c()}){
                        accum[0]+=v.x();accum[1]+=v.y();accum[2]+=v.z();accum[3]++;
                    }
                if(accum[3]>0)centroids.put(part.name(),accum);
            }
            g.setColor(new Color(244,92,81));
            for(var part:centroids.entrySet()){
                double[] c=part.getValue();
                double a=c[0]/c[3],b=c[1]/c[3],d=c[2]/c[3];
                double ax=a*.83-d*.55,ay=-(b*.95+a*.24+d*.35);
                int px=left+width/2+(int)Math.round((ax-centerX)*scale);
                int py=getPreviewHeight()/2+(int)Math.round((ay-centerY)*scale);
                g.fillOval(px-4,py-4,8,8);
                g.drawString(part.getKey()+" — COLLAPSED",px+7,py-5);
            }
            if(tris.isEmpty()){
                g.setColor(Color.ORANGE);g.drawString("No drawable triangles remain",left+10,35);
            }
        }
    }
    private int getPreviewHeight(){return Math.max(180,preview.getHeight());}
}
