package org.beckenburg;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

/** Castle Beckenburg case archive from actually settled, persisted campaign missions. */
public final class CampaignArchivePanel extends JPanel {
    private final ProfileStore profile;
    private final DefaultListModel<CampaignArchive.CaseFile> entries=new DefaultListModel<>();
    private final JList<CampaignArchive.CaseFile> cases=new JList<>(entries);
    private final JTextArea report=new JTextArea();
    public CampaignArchivePanel(ProfileStore profile){
        this.profile=Objects.requireNonNull(profile);
        setName("campaign.archive");setLayout(new BorderLayout(6,6));
        setBackground(new Color(16,22,30));
        cases.setName("campaign.archive.list");
        cases.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        cases.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12));
        cases.setBackground(new Color(28,34,42));cases.setForeground(new Color(230,219,193));
        cases.setCellRenderer((list,value,index,selected,focus)->{
            JLabel line=new JLabel(value.label());
            line.setOpaque(true);line.setBorder(BorderFactory.createEmptyBorder(6,8,6,8));
            line.setBackground(selected?new Color(80,91,80):new Color(28,34,42));
            line.setForeground(new Color(230,219,193));return line;
        });
        report.setName("campaign.archive.report");
        report.setEditable(false);report.setFocusable(false);report.setLineWrap(true);
        report.setWrapStyleWord(true);report.setBackground(new Color(28,34,42));
        report.setForeground(new Color(230,219,193));report.setMargin(new Insets(12,12,12,12));
        report.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12));
        JSplitPane split=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            new JScrollPane(cases),new JScrollPane(report));
        split.setResizeWeight(.42);split.setBorder(null);
        add(split,BorderLayout.CENTER);
        cases.addListSelectionListener(e->{if(!e.getValueIsAdjusting())select();});
        refresh();
    }
    public void refresh(){
        String selected=cases.getSelectedValue()==null?null:cases.getSelectedValue().id();
        entries.clear();
        var history=profile.archives();
        for(int i=history.size()-1;i>=0;i--)entries.addElement(history.get(i));
        int position=0;
        if(selected!=null)for(int i=0;i<entries.size();i++)
            if(selected.equals(entries.get(i).id())){position=i;break;}
        if(!entries.isEmpty())cases.setSelectedIndex(position);
        else report.setText("No completed campaign cases yet.\nReturn from an investigation to file its report.");
    }
    private void select(){
        var record=cases.getSelectedValue();
        report.setText(record==null?"No case selected.":record.fullReport());
        report.setCaretPosition(0);
    }
}
