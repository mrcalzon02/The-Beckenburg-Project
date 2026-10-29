package org.beckenburg;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;

/** Branching character conversation INSIDE the Castle, separate from the desk.
 * The multiplayer guest version is deliberately read-only; only the host owns
 * quest and relationship progression.
 */
public final class CastleDialoguePanel extends JPanel {
    private final CastleRelations.Resident resident;
    private final ProfileStore owner;
    private CastleRelations.Context snapshot;
    private int guestTrust;
    private final Runnable trade,readArchive;
    private final JLabel title=new JLabel();
    private final JLabel standing=new JLabel();
    private final JTextArea speech=new JTextArea();
    private final JPanel choices=new JPanel(new GridLayout(0,1,4,5));
    private CastleRelations.Topic current=CastleRelations.Topic.GREETING;
    public CastleDialoguePanel(ProfileStore profile,CastleRelations.Resident who,
            Runnable trade,Runnable readArchive){
        this(who,Objects.requireNonNull(profile),null,0,trade,readArchive);
    }
    public CastleDialoguePanel(CastleRelations.Resident who,
            CastleRelations.Context hostContext,int hostTrust){
        this(who,null,hostContext,hostTrust,null,null);
    }
    private CastleDialoguePanel(CastleRelations.Resident who,ProfileStore owner,
            CastleRelations.Context snapshot,int trust,Runnable trade,Runnable readArchive){
        this.resident=Objects.requireNonNull(who);
        this.owner=owner;this.snapshot=snapshot;this.guestTrust=trust;
        this.trade=trade;this.readArchive=readArchive;
        setName("castle.dialogue."+who.name());
        setLayout(new BorderLayout(10,8));
        setBorder(BorderFactory.createEmptyBorder(13,14,13,14));
        setBackground(new Color(17,23,30));
        JPanel header=new JPanel(new GridLayout(2,1,0,2));
        header.setOpaque(false);
        title.setName("castle.dialogue.title");
        title.setForeground(new Color(238,215,177));
        title.setFont(new Font(Font.MONOSPACED,Font.BOLD,16));
        standing.setName("castle.dialogue.standing");
        standing.setForeground(new Color(153,219,189));
        header.add(title);header.add(standing);
        add(header,BorderLayout.NORTH);
        speech.setName("castle.dialogue.speech");
        speech.setEditable(false);speech.setFocusable(false);
        speech.setLineWrap(true);speech.setWrapStyleWord(true);
        speech.setFont(new Font(Font.MONOSPACED,Font.PLAIN,15));
        speech.setForeground(new Color(225,226,216));
        speech.setBackground(new Color(24,30,39));
        speech.setMargin(new Insets(18,19,14,19));
        add(new JScrollPane(speech),BorderLayout.CENTER);
        choices.setName("castle.dialogue.choices");
        choices.setBackground(new Color(17,23,30));
        JScrollPane scroll=new JScrollPane(choices);
        scroll.setPreferredSize(new Dimension(320,220));
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        add(scroll,BorderLayout.SOUTH);
        select(CastleRelations.Topic.GREETING);
    }
    public CastleRelations.Resident resident(){return resident;}
    public CastleRelations.Topic topic(){return current;}
    public String spoken(){return speech.getText();}
    public CastleRelations.Context context(){
        return owner==null?snapshot:CastleRelations.context(owner);
    }
    public void refreshGuest(CastleRelations.Context context,int trust){
        if(owner!=null)throw new IllegalStateException("Owner dialogues use their live profile");
        if(trust<0||trust>100)throw new IllegalArgumentException("invalid relationship snapshot");
        this.snapshot=Objects.requireNonNull(context);guestTrust=trust;
        select(CastleRelations.Topic.GREETING);
    }
    public void select(CastleRelations.Topic topic){
        Objects.requireNonNull(topic);
        if(owner==null&&snapshot==null)throw new IllegalStateException("No host story snapshot");
        CastleRelations.Context progress=context();
        int trust=owner==null?guestTrust:owner.relationship(resident);
        // Never change a guest's campaign; on the host record only the first
        // conversation on this chapter and roll it back if persistence fails.
        if(owner!=null)try{
            owner.recordDialogue(resident,topic);
            trust=owner.relationship(resident);
        }catch(IOException problem){
            speech.setText("The Castle chronicle could not be saved: "+problem.getMessage());
            return;
        }
        current=topic;
        var page=CastleRelations.page(resident,progress,trust,topic);
        title.setText(page.heading());
        standing.setText("RELATIONSHIP "+CastleRelations.rank(progress,trust)+"/5"+
            "  /  "+(owner==null?"HOST'S CAMPAIGN / VIEW ONLY":"TRUST "+trust+" / 100"));
        speech.setText(page.spoken());speech.setCaretPosition(0);
        choices.removeAll();
        if(topic==CastleRelations.Topic.FAREWELL){
            addChoice("Speak again",CastleRelations.Topic.GREETING);
        }else for(var choice:page.choices())addChoice(choice.label(),choice.destination());
        if(owner!=null&&(topic==CastleRelations.Topic.GREETING||topic==CastleRelations.Topic.QUEST)){
            if(trade!=null){
                JButton button=PsxHorrorUi.button("Ask to see the quartermaster's supplies");
                button.setName("castle.dialogue.trade");
                button.addActionListener(e->trade.run());choices.add(button);
            }
            if(readArchive!=null){
                JButton button=PsxHorrorUi.button("Consult the Common Archive");
                button.setName("castle.dialogue.archive");
                button.addActionListener(e->readArchive.run());choices.add(button);
            }
        }
        choices.revalidate();choices.repaint();
    }
    private void addChoice(String label,CastleRelations.Topic topic){
        JButton button=PsxHorrorUi.button(label);
        button.setName("castle.dialogue.choice."+topic.name());
        button.addActionListener(e->select(topic));
        choices.add(button);
    }
}
