package org.beckenburg;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Contained teaching panel for a physical annex worktable. */
public final class CastleTrainingPanel extends JPanel {
    private final CastleTraining.Lesson lesson;
    private final JLabel progress=new JLabel();
    private final JTextArea feedback=new JTextArea(3,50);
    private final List<JButton> identify=new ArrayList<>(),install=new ArrayList<>();
    private final JButton ward=PsxHorrorUi.button("REHEARSE ANCHOR WARD PREPARATION");
    public CastleTrainingPanel(CastleTraining.Bay bay,Runnable close){
        super(new BorderLayout(12,12));lesson=new CastleTraining.Lesson(bay);setName("castle.training.panel");
        setOpaque(false);setBorder(BorderFactory.createEmptyBorder(16,16,16,16));
        JPanel heading=new JPanel(new GridLayout(0,1,0,8));heading.setOpaque(false);
        JLabel title=new JLabel(bay.title());title.setForeground(new Color(245,217,166));title.setFont(new Font(Font.MONOSPACED,Font.BOLD,22));heading.add(title);
        progress.setForeground(new Color(192,218,220));heading.add(progress);add(heading,BorderLayout.NORTH);
        JPanel body=new JPanel();body.setOpaque(false);body.setLayout(new BoxLayout(body,BoxLayout.Y_AXIS));
        var example=bay.example();
        JTextArea card=new JTextArea("TRAINING PROFILE: "+readable(example)+"\n\nEMF: "+readable(example.emf())+"   THERMAL: "+readable(example.thermal())+"   VOICE: "+readable(example.voice())+"\n\nChoose its rite, rehearse anchor ward preparation, then select its installation. This is a headquarters lesson.",7,55);
        style(card);body.add(card);body.add(Box.createVerticalStrut(12));
        JPanel choices=new JPanel(new GridLayout(1,3,8,0));choices.setOpaque(false);
        for(var rite:GhostIdentity.Rite.values()){
            var b=PsxHorrorUi.button(rite.name());b.setName("castle.training.rite."+rite);b.addActionListener(e->reply(lesson.identify(rite)));identify.add(b);choices.add(b);
        }
        body.add(choices);body.add(Box.createVerticalStrut(12));ward.setName("castle.training.ward");ward.addActionListener(e->reply(lesson.ward()));body.add(ward);body.add(Box.createVerticalStrut(12));
        JPanel sites=new JPanel(new GridLayout(1,3,8,0));sites.setOpaque(false);
        for(var rite:GhostIdentity.Rite.values()){
            var b=PsxHorrorUi.button(RitualSites.installationName(rite));b.setName("castle.training.installation."+rite);b.addActionListener(e->reply(lesson.installation(rite)));install.add(b);sites.add(b);
        }
        body.add(sites);body.add(Box.createVerticalStrut(16));style(feedback);feedback.setName("castle.training.feedback");body.add(feedback);
        add(body,BorderLayout.CENTER);JPanel footer=new JPanel(new GridLayout(1,2,10,0));footer.setOpaque(false);
        var reset=PsxHorrorUi.button("RESTART LESSON");reset.setName("castle.training.reset");reset.addActionListener(e->{lesson.reset();feedback.setText("");refresh();});footer.add(reset);
        var back=PsxHorrorUi.button("RETURN TO CASTLE");back.setName("castle.training.return");back.addActionListener(e->close.run());footer.add(back);add(footer,BorderLayout.SOUTH);refresh();
    }
    private static void style(JTextArea text){text.setEditable(false);text.setLineWrap(true);text.setWrapStyleWord(true);text.setOpaque(false);text.setForeground(new Color(220,224,210));text.setFont(new Font(Font.MONOSPACED,Font.PLAIN,14));}
    private static String readable(Enum<?> value){return value.name().replace('_',' ');}
    private void reply(CastleTraining.Reply r){feedback.setText(r.message());feedback.setForeground(r.accepted()?new Color(180,228,190):new Color(248,176,150));refresh();}
    private void refresh(){
        progress.setText(switch(lesson.step()){case IDENTIFY->"1 / 3   IDENTIFY THE REQUIRED RITE";case WARD->"2 / 3   PREPARE THE ANCHOR WARD";case INSTALLATION->"3 / 3   CHOOSE THE INSTALLATION";case COMPLETE->"LESSON COMPLETE";});
        identify.forEach(b->b.setEnabled(lesson.step()==CastleTraining.Step.IDENTIFY));ward.setEnabled(lesson.step()==CastleTraining.Step.WARD);install.forEach(b->b.setEnabled(lesson.step()==CastleTraining.Step.INSTALLATION));
    }
    CastleTraining.Step step(){return lesson.step();}
}
