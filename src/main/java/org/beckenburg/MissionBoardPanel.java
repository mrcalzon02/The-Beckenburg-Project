package org.beckenburg;

import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

/** Picture carousel for every lore-named site. Browsing never unlocks a map. */
public final class MissionBoardPanel extends JPanel {
    private static final Color PAPER=new Color(232,222,198),INK=new Color(39,45,43);
    private final JComboBox<MissionSites.Entry> sites=new JComboBox<>(MissionSites.all().toArray(MissionSites.Entry[]::new));
    private final JLabel title=new JLabel(),region=new JLabel(),status=new JLabel(),position=new JLabel();
    private final JTextArea description=new JTextArea();
    private final Photo photo=new Photo();
    private SwingWorker<BufferedImage,Void> loader;
    private Consumer<MissionSites.Entry> change=e->{};
    private int generation;
    private static final java.util.Map<String,BufferedImage> CACHE=java.util.Collections.synchronizedMap(new java.util.LinkedHashMap<>(8,.75f,true){
        @Override protected boolean removeEldestEntry(java.util.Map.Entry<String,BufferedImage> e){return size()>4;}
    });
    public MissionBoardPanel(){
        super(new BorderLayout(12,12));setBackground(new Color(43,49,43));setBorder(BorderFactory.createEmptyBorder(16,16,16,16));
        setPreferredSize(new Dimension(820,510));setName("mission.board");
        var heading=new JLabel("BECKENBURG / MISSION SELECTION");heading.setForeground(PAPER);heading.setFont(new Font(Font.SANS_SERIF,Font.BOLD,18));
        add(heading,BorderLayout.NORTH);
        var content=new JPanel(new BorderLayout(14,14));content.setOpaque(false);
        photo.setPreferredSize(new Dimension(500,330));photo.getAccessibleContext().setAccessibleName("Selected mission site picture");
        content.add(photo,BorderLayout.CENTER);
        var dossier=new JPanel();dossier.setLayout(new BoxLayout(dossier,BoxLayout.Y_AXIS));dossier.setBackground(PAPER);dossier.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));dossier.setPreferredSize(new Dimension(255,330));
        title.setFont(new Font(Font.SANS_SERIF,Font.BOLD,20));title.setForeground(INK);
        region.setForeground(INK);status.setForeground(new Color(105,55,30));
        description.setEditable(false);description.setFocusable(false);description.setLineWrap(true);description.setWrapStyleWord(true);description.setOpaque(false);description.setForeground(INK);description.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,14));
        for(var c:new JComponent[]{title,region,status,description}){c.setAlignmentX(LEFT_ALIGNMENT);dossier.add(c);dossier.add(Box.createVerticalStrut(12));}
        content.add(dossier,BorderLayout.EAST);add(content,BorderLayout.CENTER);
        var bottom=new JPanel(new BorderLayout(8,8));bottom.setOpaque(false);
        var row=new JPanel(new BorderLayout(8,0));row.setOpaque(false);
        JButton previous=new JButton("← PREVIOUS"),next=new JButton("NEXT →");previous.setName("mission.previous");next.setName("mission.next");sites.setName("mission.site");
        row.add(previous,BorderLayout.WEST);row.add(sites,BorderLayout.CENTER);row.add(next,BorderLayout.EAST);bottom.add(row,BorderLayout.CENTER);
        position.setForeground(PAPER);bottom.add(position,BorderLayout.SOUTH);add(bottom,BorderLayout.SOUTH);
        previous.addActionListener(e->step(-1));next.addActionListener(e->step(1));sites.addActionListener(e->refresh());
        bind("LEFT",-1);bind("RIGHT",1);
        sites.setSelectedItem(MissionSites.byId("HOUSE"));refresh();
    }
    private void bind(String key,int direction){String name="mission."+key;getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(key),name);getActionMap().put(name,new AbstractAction(){public void actionPerformed(ActionEvent e){step(direction);}});}
    public void onSelection(Consumer<MissionSites.Entry> listener){change=java.util.Objects.requireNonNull(listener);change.accept(selected());}
    public MissionSites.Entry selected(){return (MissionSites.Entry)sites.getSelectedItem();}
    public void select(String id){sites.setSelectedItem(MissionSites.byId(id));}
    public void step(int delta){sites.setSelectedIndex(Math.floorMod(sites.getSelectedIndex()+delta,sites.getItemCount()));}
    public void close(){generation++;if(loader!=null)loader.cancel(true);}
    public boolean imageReady(){return photo.image!=null;}
    private void refresh(){
        var entry=selected();if(entry==null)return;
        title.setText("<html>"+escape(entry.title())+"</html>");region.setText("<html>"+escape(entry.region())+"<br>"+escape(entry.scale())+"</html>");
        status.setText("<html>"+escape(entry.status())+"</html>");description.setText(entry.description());
        position.setText("SITE "+(sites.getSelectedIndex()+1)+" / "+sites.getItemCount()+"   ·   Arrow keys or Previous / Next to browse");
        photo.caption=entry.imageCaption();photo.image=CACHE.get(entry.imageKey());photo.repaint();change.accept(entry);
        int request=++generation;if(loader!=null)loader.cancel(true);
        if(photo.image!=null)return;
        loader=new SwingWorker<>(){
            @Override protected BufferedImage doInBackground()throws Exception{
                try(var in=MissionBoardPanel.class.getResourceAsStream("/org/beckenburg/mission-board/"+entry.imageKey()+".png")){
                    if(in==null)return null;var im=ImageIO.read(in);if(im!=null)CACHE.put(entry.imageKey(),im);return im;
                }
            }
            @Override protected void done(){if(request!=generation||isCancelled())return;try{photo.image=get();}catch(Exception unavailable){photo.image=null;}photo.repaint();}
        };loader.execute();
    }
    private static String escape(String s){return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");}
    private static final class Photo extends JPanel {
        BufferedImage image;String caption="STATIC PLACEHOLDER";
        Photo(){setBackground(new Color(19,25,27));setBorder(BorderFactory.createLineBorder(PAPER,8));}
        @Override protected void paintComponent(Graphics original){
            super.paintComponent(original);Graphics2D g=(Graphics2D)original.create();
            int w=getWidth()-24,h=getHeight()-58;
            if(image!=null){double scale=Math.min(w/(double)image.getWidth(),h/(double)image.getHeight());int iw=(int)(image.getWidth()*scale),ih=(int)(image.getHeight()*scale);g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);g.drawImage(image,(getWidth()-iw)/2,12+(h-ih)/2,iw,ih,null);}
            else{g.setColor(PAPER);g.drawString("Site picture loading / unavailable",24,Math.max(40,h/2));}
            g.setColor(PAPER);g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,12));g.drawString(caption,18,getHeight()-20);g.dispose();
        }
    }
}
