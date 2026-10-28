package org.beckenburg;

import java.awt.*;
import javax.swing.*;

/**
 * Shared full-window game menus. These are CardLayout children inside the
 * existing Beckenburg JFrame, never modal Swing dialogs or new top-level windows.
 * Every surface and button uses the licensed PSX Horror UI artwork already
 * packaged with the application.
 */
public final class PsxMenuScreen {
    private PsxMenuScreen(){}

    public static JPanel screen(String name,String title,String subtitle,
                                JComponent content,JButton... actions){
        JPanel root=PsxHorrorUi.surface(new BorderLayout(12,12));
        root.setName(name);
        root.setBorder(BorderFactory.createEmptyBorder(16,20,16,20));
        JPanel header=new JPanel(new BorderLayout(10,4));
        header.setOpaque(false);
        JLabel heading=new JLabel(title);
        heading.setFont(new Font(Font.MONOSPACED,Font.BOLD,22));
        heading.setForeground(new Color(241,221,176));
        var emblem=PsxHorrorUi.image("icons/flashlight.png");
        if(emblem!=null){
            JLabel icon=new JLabel(new ImageIcon(emblem));
            icon.setBorder(BorderFactory.createEmptyBorder(0,0,0,7));
            header.add(icon,BorderLayout.WEST);
        }
        header.add(heading,BorderLayout.CENTER);
        JLabel sub=new JLabel(subtitle);
        sub.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12));
        sub.setForeground(new Color(175,201,190));
        header.add(sub,BorderLayout.SOUTH);
        root.add(header,BorderLayout.NORTH);

        JPanel body=PsxHorrorUi.panel(new BorderLayout());
        body.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        body.add(content,BorderLayout.CENTER);
        root.add(body,BorderLayout.CENTER);

        JPanel footer=new JPanel(new FlowLayout(FlowLayout.RIGHT,9,0));
        footer.setOpaque(false);
        for(JButton action:actions){
            PsxHorrorUi.styleButton(action);
            footer.add(action);
        }
        root.add(footer,BorderLayout.SOUTH);
        theme(root);
        return root;
    }

    public static JScrollPane scroll(JComponent view){
        JScrollPane scroll=new JScrollPane(new ScrollBody(view),ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        return scroll;
    }
    private static final class ScrollBody extends JPanel implements Scrollable {
        ScrollBody(JComponent view){super(new BorderLayout());setOpaque(false);add(view,BorderLayout.CENTER);}
        public Dimension getPreferredScrollableViewportSize(){return getPreferredSize();}
        public int getScrollableUnitIncrement(Rectangle r,int orientation,int direction){return 18;}
        public int getScrollableBlockIncrement(Rectangle r,int orientation,int direction){return Math.max(18,r.height-18);}
        public boolean getScrollableTracksViewportWidth(){return true;}
        public boolean getScrollableTracksViewportHeight(){return getParent()!=null&&getPreferredSize().height<getParent().getHeight();}
    }
    /** Theme the real Swing controls as well as their artwork-backed containers. */
    public static void theme(Container root){
        var preferences=java.util.prefs.Preferences.userNodeForPackage(InvestigationGame.class);
        theme(root,preferences.getBoolean("largeMenuText",false),preferences.getBoolean("highContrast",false));
    }
    private static void theme(Container root,boolean large,boolean highContrast){
        Color dark=highContrast?Color.BLACK:new Color(27,34,42),paper=highContrast?Color.WHITE:new Color(235,224,201);
        for(Component child:root.getComponents()){
            if(child instanceof JComponent component&&component.getFont()!=null){
                Font base=(Font)component.getClientProperty("psx.font.base");
                if(base==null){base=component.getFont();component.putClientProperty("psx.font.base",base);}
                component.setFont(base.deriveFont((float)(base.getSize()+(large?3:0))));
            }
            if(child instanceof JPanel panel)panel.setOpaque(false);
            if(child instanceof JLabel label&&(highContrast||label.getForeground() instanceof javax.swing.plaf.UIResource))label.setForeground(paper);
            if(child instanceof JCheckBox check){check.setOpaque(false);check.setForeground(paper);}
            if(child instanceof JComboBox<?> combo){combo.setBackground(dark);combo.setForeground(paper);}
            if(child instanceof JTextField text){text.setBackground(dark);text.setForeground(paper);text.setCaretColor(paper);}
            if(child instanceof JTextArea text){text.setBackground(dark);text.setForeground(paper);}
            if(child instanceof JList<?> list){list.setBackground(dark);list.setForeground(paper);
                list.setSelectionBackground(new Color(76,89,78));list.setSelectionForeground(Color.WHITE);}
            if(child instanceof JScrollPane scroll){scroll.setBackground(dark);scroll.getViewport().setBackground(dark);
                scroll.setBorder(BorderFactory.createLineBorder(new Color(78,85,85)));}
            if(child instanceof JSplitPane split){split.setBackground(dark);split.setBorder(null);}
            if(child instanceof JComponent component&&component.getBorder() instanceof javax.swing.border.TitledBorder border)
                border.setTitleColor(paper);
            if(child instanceof Container nested)theme(nested,large,highContrast);
        }
    }

    public static JLabel label(String value){
        JLabel text=new JLabel(value);
        text.setForeground(new Color(231,220,194));
        text.setFont(new Font(Font.MONOSPACED,Font.PLAIN,13));
        return text;
    }
}
