package org.beckenburg.editor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

/**
 * Human-only view of actual 16-bit source height tiles.
 * No simulated 3D frame, agent endpoint, gameplay streaming or implicit overwrites.
 * Changes are committed as individual source PNGs immediately.
 */
public final class StitchedWorldEditor extends JPanel {
    private final JFrame frame;
    private final JLabel status=new JLabel("Create or open a stitched source world.");
    private final JSpinner tx=new JSpinner(new SpinnerNumberModel(0,-999999,999999,1));
    private final JSpinner tz=new JSpinner(new SpinnerNumberModel(0,-999999,999999,1));
    private final JPanel preview=new TilePreview();
    private final Map<StitchedHeightmapWorld.Tile,BufferedImage> thumbnails=new HashMap<>();
    private StitchedHeightmapWorld world;
    private int left,top,side;

    public static JFrame open(Window owner){
        JFrame f=new JFrame("Beckenburg — Stitched 16-bit World (source editor)");
        f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        f.setContentPane(new StitchedWorldEditor(f));
        f.setMinimumSize(new Dimension(800,500));
        f.setSize(1030,680);
        f.setLocationRelativeTo(owner);
        f.setVisible(true);
        return f;
    }
    private StitchedWorldEditor(JFrame frame){
        super(new BorderLayout(6,6));
        this.frame=frame;
        setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        JPanel commands=new JPanel(new FlowLayout(FlowLayout.LEFT,5,3));
        addButton(commands,"NEW",this::createWorld);
        addButton(commands,"OPEN",this::openWorld);
        commands.add(new JLabel("Center tile X"));commands.add(tx);
        commands.add(new JLabel("Z"));commands.add(tz);
        addButton(commands,"GENERATE TILE",()->generate(false));
        addButton(commands,"GENERATE 3x3",()->generate(true));
        addButton(commands,"IMPORT PNG",this::importTile);
        addButton(commands,"EXPORT PNG",this::exportTile);
        addButton(commands,"VERIFY SEAMS",this::verify);
        addButton(commands,"ADAPTIVE TOPOLOGY",this::adaptiveTopology);
        add(commands,BorderLayout.NORTH);
        preview.setPreferredSize(new Dimension(520,500));
        preview.setBackground(new Color(25,30,35));
        add(preview,BorderLayout.CENTER);
        JLabel hint=new JLabel("2D 16-bit source heights only. Click an adjacent tile to recenter. "+
            "PNG writes save immediately; sculpt/undo and runtime rendering are not connected yet.");
        JPanel footer=new JPanel(new GridLayout(2,1,2,2));
        footer.add(hint);footer.add(status);
        add(footer,BorderLayout.SOUTH);
        tx.addChangeListener(e->refresh());
        tz.addChangeListener(e->refresh());
        refresh();
    }
    private void addButton(JPanel into,String name,Runnable action){
        JButton b=new JButton(name);
        b.addActionListener(e->action.run());
        into.add(b);
    }
    private int x(){return ((Number)tx.getValue()).intValue();}
    private int z(){return ((Number)tz.getValue()).intValue();}
    private Path worldsRoot(){
        return Path.of(System.getProperty("user.home"),".beckenburg","editor","stitched-worlds");
    }
    private static JTextField field(String value){return new JTextField(value,12);}
    private void createWorld(){
        JTextField id=field("new-world"),cells=field("256"),step=field("2.0"),
            min=field("-200"),max=field("1000"),seed=field("76123");
        JPanel form=new JPanel(new GridLayout(0,2,5,5));
        for(var item:new Object[][]{
            {"World folder ID",id},{"Cells per tile (power of 2)",cells},
            {"Metres per sample",step},{"Minimum elevation (m)",min},
            {"Maximum elevation (m)",max},{"Generation seed",seed}}){
            form.add(new JLabel((String)item[0]));form.add((JTextField)item[1]);
        }
        int choice=JOptionPane.showConfirmDialog(frame,form,"Create stitched source world",
            JOptionPane.OK_CANCEL_OPTION);
        if(choice!=JOptionPane.OK_OPTION)return;
        try{
            String name=id.getText().trim();
            if(!name.matches("[A-Za-z0-9_-]{1,48}"))
                throw new IllegalArgumentException("Use a simple 1..48 character world ID");
            StitchedHeightmapWorld.Settings settings=new StitchedHeightmapWorld.Settings(
                Integer.parseInt(cells.getText().trim()),
                Double.parseDouble(step.getText().trim()),0,0,
                Double.parseDouble(min.getText().trim()),
                Double.parseDouble(max.getText().trim()),
                Long.parseLong(seed.getText().trim()),12);
            world=StitchedHeightmapWorld.create(worldsRoot().resolve(name),settings);
            tx.setValue(0);tz.setValue(0);refresh();
        }catch(IOException|RuntimeException failure){error("Cannot create world",failure);}
    }
    private void openWorld(){
        JFileChooser picker=new JFileChooser(worldsRoot().toFile());
        picker.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if(picker.showOpenDialog(frame)!=JFileChooser.APPROVE_OPTION)return;
        try{
            StitchedHeightmapWorld opened=StitchedHeightmapWorld.open(picker.getSelectedFile().toPath());
            world=opened;tx.setValue(0);tz.setValue(0);refresh();
        }catch(IOException|RuntimeException failure){error("Cannot open world",failure);}
    }
    private void generate(boolean region){
        if(!ready())return;
        try{
            if(region)world.generateRegion(x()-1,z()-1,3,3);
            else world.generateTile(x(),z());
            refresh();
        }catch(IOException|RuntimeException failure){error("Generation failed; no existing tile overwritten",failure);}
    }
    private void importTile(){
        if(!ready())return;
        JFileChooser picker=new JFileChooser();
        if(picker.showOpenDialog(frame)!=JFileChooser.APPROVE_OPTION)return;
        try{
            // Import cannot overwrite a previously authored tile from this UI.
            world.importTile(x(),z(),picker.getSelectedFile().toPath(),false);
            refresh();
        }catch(IOException|RuntimeException failure){error("16-bit PNG import rejected",failure);}
    }
    private void exportTile(){
        if(!ready())return;
        if(!world.hasTile(x(),z())){status.setText("No tile at selected coordinate.");return;}
        JFileChooser picker=new JFileChooser("tile_"+x()+"_"+z()+".png");
        if(picker.showSaveDialog(frame)!=JFileChooser.APPROVE_OPTION)return;
        Path target=picker.getSelectedFile().toPath();
        if(Files.exists(target)&&JOptionPane.showConfirmDialog(frame,
                "Replace the chosen export file?", "Confirm export overwrite",
                JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        try{
            Files.copy(world.tilePath(x(),z()),target,StandardCopyOption.REPLACE_EXISTING);
            status.setText("Exported identical 16-bit source PNG to "+target);
        }catch(IOException failure){error("PNG export failed",failure);}
    }
    private void adaptiveTopology(){
        if(!ready())return;
        try{
            AdaptiveTopologyEditor.open(frame,world,x(),z());
        }catch(IOException|RuntimeException failure){
            error("Adaptive topology editor failed to open",failure);
        }
    }
    private void verify(){
        if(!ready())return;
        try{
            var errors=world.verifySeams();
            status.setText(errors.isEmpty()?"No mismatched borders among present source tiles.":
                errors.size()+" SOURCE SEAM ERRORS; first: "+errors.get(0));
        }catch(IOException|RuntimeException failure){error("Seam inspection failed",failure);}
    }
    private boolean ready(){
        if(world!=null)return true;
        status.setText("Create or open a world first.");return false;
    }
    private void error(String title,Exception failure){
        status.setText(title+": "+failure.getMessage());
        JOptionPane.showMessageDialog(frame,failure.getMessage(),title,JOptionPane.ERROR_MESSAGE);
        refresh();
    }
    private void refresh(){
        thumbnails.clear();
        if(world!=null){
            int n=world.settings().tileCells();
            for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++){
                int ix=x()+dx,iz=z()+dz;
                if(!world.hasTile(ix,iz))continue;
                BufferedImage thumb=new BufferedImage(36,36,BufferedImage.TYPE_INT_RGB);
                try{
                    for(int py=0;py<36;py++)for(int px=0;px<36;px++){
                        int q=world.sample(ix,iz,px*n/35,py*n/35);
                        int tone=(int)Math.round(q*255.0/65535);
                        thumb.setRGB(px,py,new Color(tone,tone,tone).getRGB());
                    }
                    thumbnails.put(new StitchedHeightmapWorld.Tile(ix,iz),thumb);
                }catch(IOException|RuntimeException failure){
                    status.setText("Tile read error "+ix+","+iz+": "+failure.getMessage());
                }
            }
            var settings=world.settings();
            status.setText("SOURCE: "+world.directory().getFileName()+
                " | tile "+x()+","+z()+" | "+settings.tileCells()+" cells × "+
                settings.metresPerCell()+"m | "+thumbnails.size()+
                " resident thumbnails | decoded cache "+world.cachedTiles()+"/"+
                settings.cacheTiles()+" | file changes saved immediately");
        }
        preview.repaint();
    }
    private final class TilePreview extends JPanel {
        TilePreview(){
            addMouseListener(new MouseAdapter(){
                @Override public void mouseClicked(MouseEvent e){
                    if(world==null||side<=0)return;
                    int col=(e.getX()-left)/side,row=(e.getY()-top)/side;
                    if(e.getX()<left||e.getY()<top||col<0||row<0||col>=3||row>=3)return;
                    int nx=x()+col-1,nz=z()+row-1;
                    if(Math.abs((long)nx)>999999||Math.abs((long)nz)>999999)return;
                    tx.setValue(nx);tz.setValue(nz);
                }
            });
        }
        @Override protected void paintComponent(Graphics g){
            super.paintComponent(g);
            Graphics2D out=(Graphics2D)g.create();
            side=Math.max(1,Math.min((getWidth()-30)/3,(getHeight()-50)/3));
            left=(getWidth()-3*side)/2;top=(getHeight()-3*side)/2;
            for(int dz=0;dz<3;dz++)for(int dx=0;dx<3;dx++){
                int gx=left+dx*side,gz=top+dz*side;
                var key=new StitchedHeightmapWorld.Tile(x()+dx-1,z()+dz-1);
                var bitmap=thumbnails.get(key);
                if(bitmap!=null)out.drawImage(bitmap,gx,gz,side,side,null);
                else{out.setColor(new Color(50,56,62));out.fillRect(gx,gz,side,side);}
                out.setColor(dx==1&&dz==1?Color.ORANGE:Color.GRAY);
                out.drawRect(gx,gz,side,side);
                out.setColor(Color.WHITE);
                out.drawString(key.x()+", "+key.z()+(bitmap==null?" (empty)":""),gx+8,gz+18);
            }
            out.setColor(Color.WHITE);
            out.drawString("SOURCE HEIGHTMAP TILE OVERVIEW — NOT A RUNTIME SCREENSHOT",12,20);
            out.dispose();
        }
    }
}
