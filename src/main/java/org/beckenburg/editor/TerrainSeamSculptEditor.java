package org.beckenburg.editor;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;
import javax.swing.*;

/**
 * Functional two-dimensional inspection and editing UI for global seam vertices.
 * Displays real source height samples + unbaked cross-tile overrides. Never a
 * fake in-game 3D render. Uses the same world and batch importer as source edit.
 */
public final class TerrainSeamSculptEditor extends JPanel {
    private final JFrame frame;
    private final StitchedHeightmapWorld world;
    private final int centerX,centerZ,n;
    private final TerrainCanvas canvas=new TerrainCanvas();
    private final JLabel status=new JLabel("Non-destructive seam-authoring session");
    private final JSpinner radius=new JSpinner(new SpinnerNumberModel(4,1,64,1));
    private final JSpinner amount=new JSpinner(new SpinnerNumberModel(3.0,.25,250.0,.25));
    private final JCheckBox lower=new JCheckBox("LOWER",false);
    private TerrainSeamSculpt session;
    private BufferedImage preview;
    private int cursorX,cursorZ;
    private boolean painting,busy;
    private TerrainBrushProfile.Stroke stroke;

    public static JFrame open(Window owner,StitchedHeightmapWorld world,int tx,int tz)
            throws IOException {
        var f=new JFrame("Level Editor Kit — Multi-Tile Seam Sculpt");
        var editor=new TerrainSeamSculptEditor(f,world,tx,tz);
        f.setContentPane(editor);
        f.setSize(970,775);f.setMinimumSize(new Dimension(700,535));
        f.setLocationRelativeTo(owner);
        f.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        f.addWindowListener(new WindowAdapter(){
            @Override public void windowClosing(WindowEvent event){
                if(editor.busy)return;
                if(editor.session.dirty()){
                    int answer=JOptionPane.showConfirmDialog(f,
                        "Uncommitted seam-layer edits are not saved. Close without saving?",
                        "Unsaved seam session",JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                    if(answer!=JOptionPane.YES_OPTION)return;
                }
                f.dispose();
            }
        });
        f.setVisible(true);
        return f;
    }
    private TerrainSeamSculptEditor(JFrame frame,StitchedHeightmapWorld world,int tx,int tz)
            throws IOException{
        super(new BorderLayout(5,5));
        this.frame=Objects.requireNonNull(frame);
        this.world=Objects.requireNonNull(world);
        centerX=tx;centerZ=tz;n=world.settings().tileCells();
        ((SpinnerNumberModel)radius.getModel()).setMaximum(Math.min(n,64));
        session=new TerrainSeamSculpt(world,tx,tz);
        setBorder(BorderFactory.createEmptyBorder(7,7,7,7));
        JPanel toolbar=new JPanel(new FlowLayout(FlowLayout.LEFT,5,3));
        toolbar.add(new JLabel("Radius (grid)"));toolbar.add(radius);
        toolbar.add(new JLabel("Height / dab (metres)"));toolbar.add(amount);toolbar.add(lower);
        JButton undo=new JButton("UNDO"),redo=new JButton("REDO"),
            save=new JButton("SAVE SESSION"),discard=new JButton("DISCARD SESSION..."),
            bake=new JButton("BAKE SHARED TILES...");
        toolbar.add(undo);toolbar.add(redo);toolbar.add(save);
        toolbar.add(discard);toolbar.add(bake);
        add(toolbar,BorderLayout.NORTH);
        add(canvas,BorderLayout.CENTER);
        JPanel footer=new JPanel(new GridLayout(3,1));
        footer.add(new JLabel("2D R16 source + layered seam edits. Draw across adjoining tiles. Gray areas lack full vertex ownership."));
        footer.add(new JLabel("SAVE retains an editable authoring session. BAKE explicitly replaces source tiles together, with rollback."));
        footer.add(status);
        add(footer,BorderLayout.SOUTH);
        undo.addActionListener(e->{
            if(busy)return;
            try{if(session.undo())refresh("Undid shared-vertex stroke");}
            catch(IOException|RuntimeException failure){error(failure);}
        });
        redo.addActionListener(e->{
            if(busy)return;
            try{if(session.redo())refresh("Redid shared-vertex stroke");}
            catch(IOException|RuntimeException failure){error(failure);}
        });
        save.addActionListener(e->background("Saving seam authoring session",()->session.save()));
        discard.addActionListener(e->{
            if(busy)return;
            if(JOptionPane.showConfirmDialog(frame,
                "Discard ALL saved and unsaved seam edits in this viewport? "+
                "Authoritative heightmap source PNGs are not affected.",
                "Discard cross-tile sculpting session",JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE)!=JOptionPane.YES_OPTION)return;
            background("Discarding authored seam session",session::discard);
        });
        bake.addActionListener(e->{
            if(busy)return;
            if(session.changedVertices()==0){refresh("No edited vertices to bake");return;}
            int answer=JOptionPane.showConfirmDialog(frame,
                "Commit the shared seam edits into authoritative R16 PNG tiles? "+
                "This operation modifies all affected owner tiles together. "+
                "Layered terrain documents referencing those source tiles are protected.",
                "Explicit multi-tile source bake",JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
            if(answer!=JOptionPane.YES_OPTION)return;
            background("Committing shared-seam batch",()->{
                session.bake();
                session=new TerrainSeamSculpt(world,centerX,centerZ);
            });
        });
        refresh("Loaded "+session.tiles().size()+" existing tiles around "+
            tx+","+tz+"; edited vertices "+session.changedVertices());
    }
    @FunctionalInterface private interface Job {void run()throws Exception;}
    private void background(String label,Job job){
        if(busy)return;
        busy=true;status.setText(label+"...");
        new SwingWorker<Void,Void>(){
            @Override protected Void doInBackground()throws Exception{job.run();return null;}
            @Override protected void done(){
                busy=false;
                try{
                    get();refresh(label+" completed");
                }catch(InterruptedException error){
                    Thread.currentThread().interrupt();TerrainSeamSculptEditor.this.error(error);
                }catch(java.util.concurrent.ExecutionException error){
                    TerrainSeamSculptEditor.this.error(error);
                }
            }
        }.execute();
    }
    private void error(Throwable error){
        while(error.getCause()!=null)error=error.getCause();
        status.setText("FAILED: "+error.getMessage());
        JOptionPane.showMessageDialog(frame,error.getMessage(),
            "Cross-tile terrain operation failed",JOptionPane.ERROR_MESSAGE);
    }
    private void refresh(String message){
        preview=renderPreview();
        status.setText(message+" | "+session.changedVertices()+" edited vertices"+
            (session.dirty()?" | UNSAVED":" | SAVED"));
        canvas.repaint();
    }
    private BufferedImage renderPreview(){
        // Fixed maximum preview complexity: very large tiles never require
        // a full-resolution GUI framebuffer or per-pixel world recomputation.
        final int size=129;
        BufferedImage result=new BufferedImage(size,size,BufferedImage.TYPE_INT_RGB);
        long originX=((long)centerX-1)*n,originZ=((long)centerZ-1)*n;
        for(int z=0;z<size;z++)for(int x=0;x<size;x++){
            long gx=originX+Math.round(x*3.0*n/(size-1)),
                gz=originZ+Math.round(z*3.0*n/(size-1));
            if(gx<Integer.MIN_VALUE||gx>Integer.MAX_VALUE||
                gz<Integer.MIN_VALUE||gz>Integer.MAX_VALUE||
                !session.editable((int)gx,(int)gz)){
                result.setRGB(x,z,0x30363b);continue;
            }
            try{
                double height=session.height((int)gx,(int)gz);
                double f=(height-world.settings().minElevation())/
                    (world.settings().maxElevation()-world.settings().minElevation());
                int grey=(int)Math.max(0,Math.min(255,f*255));
                result.setRGB(x,z,(grey<<16)|(grey<<8)|grey);
            }catch(IOException|RuntimeException failure){
                result.setRGB(x,z,0x9a2222);
            }
        }
        return result;
    }
    private final class TerrainCanvas extends JPanel {
        TerrainCanvas(){
            setBackground(new Color(20,24,29));
            MouseAdapter listener=new MouseAdapter(){
                private int x(MouseEvent event){
                    return (int)(((long)centerX-1)*n+
                        Math.round(event.getX()*3.0*n/Math.max(1,getWidth()-1)));
                }
                private int z(MouseEvent event){
                    return (int)(((long)centerZ-1)*n+
                        Math.round(event.getY()*3.0*n/Math.max(1,getHeight()-1)));
                }
                @Override public void mousePressed(MouseEvent event){
                    if(busy||!SwingUtilities.isLeftMouseButton(event))return;
                    cursorX=x(event);cursorZ=z(event);painting=true;
                    double metres=((Number)amount.getValue()).doubleValue()*
                        (lower.isSelected()?-1:1);
                    double r=((Number)radius.getValue()).doubleValue();
                    stroke=new TerrainBrushProfile.Stroke(1.0,(gx,gz)->{
                        try{session.raise(gx,gz,r,metres);}
                        catch(IOException err){throw new IllegalStateException(err);}
                    });
                    try{stroke.begin(cursorX,cursorZ);refresh("Sculpting shared terrain vertices");}
                    catch(RuntimeException failure){
                        painting=false;stroke=null;session.finishStroke();error(failure);
                    }
                }
                @Override public void mouseDragged(MouseEvent event){
                    if(!painting||stroke==null)return;
                    cursorX=x(event);cursorZ=z(event);
                    try{stroke.move(cursorX,cursorZ);refresh("Sculpting shared terrain vertices");}
                    catch(RuntimeException failure){
                        painting=false;stroke.end();stroke=null;session.finishStroke();
                        error(failure);
                    }
                }
                @Override public void mouseReleased(MouseEvent event){
                    if(!painting)return;
                    painting=false;
                    if(stroke!=null){stroke.end();stroke=null;}
                    session.finishStroke();refresh("Completed one undoable cross-tile stroke");
                }
            };
            addMouseListener(listener);addMouseMotionListener(listener);
        }
        @Override protected void paintComponent(Graphics g){
            super.paintComponent(g);
            if(preview!=null)
                g.drawImage(preview,0,0,getWidth(),getHeight(),null);
            Graphics2D p=(Graphics2D)g.create();
            try{
                p.setColor(new Color(68,205,215,180));
                for(int tile=1;tile<=2;tile++){
                    int x=Math.round(tile*getWidth()/3f);
                    int y=Math.round(tile*getHeight()/3f);
                    p.drawLine(x,0,x,getHeight());p.drawLine(0,y,getWidth(),y);
                }
                if(painting){
                    double radiusCells=((Number)radius.getValue()).doubleValue();
                    double ox=((long)centerX-1)*n,oz=((long)centerZ-1)*n;
                    int px=(int)((cursorX-ox)*getWidth()/(3.0*n)),
                        pz=(int)((cursorZ-oz)*getHeight()/(3.0*n));
                    int rx=(int)(radiusCells*getWidth()/(3.0*n)),
                        rz=(int)(radiusCells*getHeight()/(3.0*n));
                    p.setColor(Color.ORANGE);
                    p.drawOval(px-rx,pz-rz,rx*2,rz*2);
                }
            }finally{p.dispose();}
        }
    }
}
