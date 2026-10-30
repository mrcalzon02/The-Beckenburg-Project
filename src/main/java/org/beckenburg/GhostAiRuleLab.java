package org.beckenburg;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import org.beckenburg.editor.ai.NpcRuleBook;
import org.beckenburg.editor.ai.NpcDecisionWorkbench;

/** Developer-only viewer over real ghost rules; no shared live game state is changed. */
public final class GhostAiRuleLab {
    private final NpcRuleBook book=GhostAiRuleAdapter.read();
    private final JDialog dialog;
    private final JComboBox<GhostIdentity.Type> species=
        new JComboBox<>(GhostIdentity.Type.values());
    private final JSpinner tick=new JSpinner(new SpinnerNumberModel(180,0,1000,10));
    private final JCheckBox hears=new JCheckBox("Simulate a host-audible hunter in room",true);
    private final JCheckBox victim=new JCheckBox("Host-verified spatial victim (preview only)");
    private final JTextArea rules=new JTextArea(22,44),test=new JTextArea(22,44);
    private final JLabel caption=new JLabel("Rulebook comes from live Java source. Simulator never mutates the active game.");
    private final JButton run=new JButton("SIMULATE LEGAL ACTIONS");
    private GhostAiRuleLab(JFrame parent){
        dialog=new JDialog(parent,"NPC / AI RULES — Developer Inspection Lab",false);
        JPanel root=new JPanel(new BorderLayout(5,5));
        JPanel top=new JPanel(new FlowLayout(FlowLayout.LEFT,5,4));
        top.add(new JLabel("Species"));top.add(species);
        top.add(new JLabel("Sandbox tick (10 Hz)"));top.add(tick);
        root.add(top,BorderLayout.NORTH);
        JPanel reports=new JPanel(new GridLayout(1,2,5,5));
        rules.setEditable(false);rules.setLineWrap(true);rules.setWrapStyleWord(true);
        test.setEditable(false);test.setLineWrap(true);test.setWrapStyleWord(true);
        rules.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12));
        test.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12));
        reports.add(new JScrollPane(rules));reports.add(new JScrollPane(test));
        root.add(reports,BorderLayout.CENTER);
        JPanel footer=new JPanel(new BorderLayout(4,4));
        JPanel controls=new JPanel(new FlowLayout(FlowLayout.LEFT,4,3));
        controls.add(hears);controls.add(victim);
        run.addActionListener(e->simulate());controls.add(run);
        JButton close=new JButton("CLOSE");close.addActionListener(e->dialog.dispose());
        controls.add(close);
        footer.add(controls,BorderLayout.NORTH);
        footer.add(caption,BorderLayout.SOUTH);
        root.add(footer,BorderLayout.SOUTH);
        root.setBorder(javax.swing.BorderFactory.createEmptyBorder(6,6,6,6));
        species.addActionListener(e->showRule());
        dialog.setContentPane(root);dialog.setSize(1120,685);
        dialog.setLocationRelativeTo(parent);
        showRule();
    }
    public static void show(JFrame parent){new GhostAiRuleLab(parent).dialog.setVisible(true);}
    private void showRule(){
        GhostIdentity.Type selected=(GhostIdentity.Type)species.getSelectedItem();
        if(selected==null)return;
        rules.setText("CANONICAL SPECIES RULES\n\n"+book.explain(selected.name())+
            "\n\nSOURCE OWNERS\n"+
            "GhostIdentity / GhostTraits / GhostBehavior / GhostFrenzy\n"+
            "GhostEvidenceMatrix / GhostDomain (legality)\n"+
            "Ancient variants inherit base mechanical movement / cost biases.\n"+
            "Evidence entries are POSSIBILITIES, never confirmed sightings.\n\n"+
            "Read-only, source-derived rule browser. Not a rule editor or AI dispatcher.");
        rules.setCaretPosition(0);
        test.setText("Press SIMULATE LEGAL ACTIONS to create a fresh isolated GhostDomain\n"+
            "and inspect actual host legal-actions at the chosen tick.\n\n"+
            "No network client, shared world, player location or private journal is accessed.\n"+
            "A checkmark is a synthetic test precondition, not proof of an in-game victim.");
    }
    private void simulate(){
        final GhostIdentity.Type selected=(GhostIdentity.Type)species.getSelectedItem();
        if(selected==null)return;
        final int requested=((Number)tick.getValue()).intValue();
        final boolean audible=hears.isSelected(),contact=victim.isSelected();
        run.setEnabled(false);caption.setText("Inspecting isolated 10 Hz host rule simulation...");
        new SwingWorker<String,Void>(){
            @Override protected String doInBackground(){
                var source=GhostAiRuleAdapter.sandbox(selected,requested,audible);
                var snapshot=GhostAiRuleAdapter.from(source);
                var rule=book.get(selected.name());
                var decision=NpcDecisionWorkbench.propose(rule,snapshot,contact);
                StringBuilder out=new StringBuilder();
                out.append("ISOLATED HOST-LAWFUL ACTION PREVIEW\n");
                out.append("Tick: ").append(source.tick())
                    .append(" / room ").append(source.room())
                    .append(" / energy ").append(source.energy()).append("\n");
                out.append("Hears hunter (host-derived): ").append(source.hearsHunter())
                    .append(" / manifested ").append(source.manifested()).append("\n");
                out.append("Physical portal exits: ").append(source.exits()).append("\n");
                out.append("Host legal actions: ").append(source.legalActions()).append("\n");
                out.append("Preferred behavior action: ").append(rule.favoredAction()).append("\n");
                out.append("Host-verified strike contact (synthetic input): ").append(contact).append("\n\n");
                out.append("ADVISORY PROPOSAL: ").append(decision.action()).append(" -> ")
                    .append(decision.targetRoom()).append("\n");
                out.append(decision.reason()).append("\n\n");
                out.append("INDIVIDUAL ACTION EXPLANATIONS\n");
                for(GhostDomain.Action action:GhostDomain.Action.values()){
                    int target=action==GhostDomain.Action.DRIFT&&!source.exits().isEmpty()
                        ?source.exits().get(0):source.room();
                    var diagnostic=NpcDecisionWorkbench.inspect(rule,snapshot,action.name(),target,contact);
                    out.append(action).append(": ").append(diagnostic.eligible()?"LEGAL ":"DENIED ")
                        .append(diagnostic.reason()).append("\n");
                }
                out.append("\nTHIS IS NOT THE LIVE GHOST AI: no commands dispatched.\n");
                out.append("No synthetic tick may overwrite real host cooldown, hearing, room or evidence.");
                return out.toString();
            }
            @Override protected void done(){
                run.setEnabled(true);
                try{test.setText(get());test.setCaretPosition(0);
                    caption.setText("Completed read-only, host-derived legality analysis. Source remains authoritative.");
                }catch(Exception e){
                    Throwable failure=e;
                    while(failure.getCause()!=null)failure=failure.getCause();
                    test.setText("TEST SANDBOX FAILED: "+failure.getMessage());
                    caption.setText("No game state changed; inspect diagnostic failure.");
                }
            }
        }.execute();
    }
}
