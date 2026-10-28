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
        return root;
    }

    public static JScrollPane scroll(JComponent view){
        JScrollPane scroll=new JScrollPane(view,ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        return scroll;
    }

    public static JLabel label(String value){
        JLabel text=new JLabel(value);
        text.setForeground(new Color(231,220,194));
        text.setFont(new Font(Font.MONOSPACED,Font.PLAIN,13));
        return text;
    }
}
