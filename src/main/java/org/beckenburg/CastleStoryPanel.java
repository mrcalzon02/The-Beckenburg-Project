package org.beckenburg;

import javax.swing.*;
import java.awt.*;

/** Readable collection text inside the existing Castle viewport. */
final class CastleStoryPanel extends JPanel {
    CastleStoryPanel(CastleStory.Record record,Runnable back){
        super(new BorderLayout(18,18));setName("castle.story.panel");setOpaque(false);setBorder(BorderFactory.createEmptyBorder(22,26,22,26));
        var title=new JLabel(record.title());title.setName("castle.story.title");title.setFont(new Font(Font.SERIF,Font.BOLD,27));title.setForeground(new Color(235,215,174));add(title,BorderLayout.NORTH);
        var text=new JTextArea(String.join("\n\n",record.paragraphs()));text.setName("castle.story.text");text.setEditable(false);text.setLineWrap(true);text.setWrapStyleWord(true);text.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,19));text.setForeground(new Color(224,226,216));text.setBackground(new Color(23,31,29));text.setBorder(BorderFactory.createEmptyBorder(18,18,18,18));
        var scroll=new JScrollPane(text);scroll.setBorder(BorderFactory.createEmptyBorder());add(scroll,BorderLayout.CENTER);
        var close=new JButton("RETURN TO THE CASTLE");close.setName("castle.story.close");close.setFont(new Font(Font.SANS_SERIF,Font.BOLD,17));close.addActionListener(e->back.run());add(close,BorderLayout.SOUTH);
    }
}
