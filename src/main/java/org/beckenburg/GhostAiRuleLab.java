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
    private final JComboBox<HouseMap.Site> site=new JComboBox<>(HouseMap.Site.values());
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
        top.add(new JLabel("Nav site"));top.add(site);
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
        JButton audit=new JButton("AUDIT ROSTER");
        audit.setToolTipText("Inspect every canonical species, variant link, and presentation safeguard");
        audit.addActionListener(e->auditRoster());
        controls.add(audit);
        JButton nav=new JButton("AUDIT NAV");
        nav.setToolTipText("Inspect real room exits, van boundary and acoustic propagation on a fresh authored-site map");
        nav.addActionListener(e->auditNavigation());
        controls.add(nav);
        JButton export=new JButton("EXPORT RULES JSON");
        export.setToolTipText("Write a deterministic, source-derived NPC rule snapshot without session data");
        export.addActionListener(e->exportRulebook());
        controls.add(export);
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
    private void auditRoster(){
        StringBuilder out=new StringBuilder("NPC / AI CANONICAL ROSTER AUDIT\\n");
        out.append("Species definitions: ").append(book.all().size()).append("\\n");
        out.append("All rules loaded from current GhostIdentity/GhostTraits/GhostBehavior/")
            .append("GhostFrenzy/GhostEvidenceMatrix.\\n\\n");
        var findings=book.inspect();
        if(findings.isEmpty())out.append("No structural warnings detected.\\n");
        else for(var finding:findings)
            out.append(finding.code()).append(" / ").append(finding.subject())
                .append(" / ").append(finding.detail()).append("\\n");
        out.append("\\nNotes describe constraints; they do not imply game errors.\\n");
        out.append("A source rulebook audit does not replace multiplayer pursuit or live collision testing.");
        test.setText(out.toString());test.setCaretPosition(0);
        caption.setText("Completed developer-only source rule audit; no active session accessed");
    }
    private void auditNavigation(){
        final HouseMap.Site chosen=(HouseMap.Site)site.getSelectedItem();
        if(chosen==null)return;
        caption.setText("Auditing actual "+chosen+" portal/door and hearing topology on a fresh sandbox map...");
        new SwingWorker<String,Void>(){
            @Override protected String doInBackground(){
                HouseMap map=new HouseMap(chosen);
                var report=GhostAiNavigationAudit.inspect(map,4);
                StringBuilder out=new StringBuilder("CANONICAL AI NAVIGATION / ACOUSTIC AUDIT\\n");
                out.append("Site: ").append(report.site())
                    .append(" / origin: ").append(report.origin())
                    .append(" / rooms: ").append(report.inspectedRooms()).append("\\n");
                out.append("Directed ghost portal links: ").append(report.links().size()).append("\\n");
                out.append("Unreachable rooms from 4: ").append(report.unreachable()).append("\\n\\n");
                for(var issue:report.findings())
                    out.append("DIAGNOSTIC / ").append(issue.code()).append(" / room ")
                        .append(issue.room()).append(" / ").append(issue.detail()).append("\\n");
                if(report.findings().isEmpty())out.append("No topological issues found.\\n");
                out.append("\\nGhost portal exits (door may need to be opened to physically pass):\\n");
                for(int room=0;room<report.inspectedRooms();room++){
                    int current=room;
                    out.append(room).append(" -> ").append(map.ghostExits(room))
                        .append(" / route to origin ").append(GhostAiNavigationAudit.path(map,current,4)).append("\\n");
                }
                out.append("\\nActual acoustic losses per directed portal (MAX = disconnected):\\n");
                for(var edge:report.links())
                    out.append(edge.from()).append(" -> ").append(edge.to())
                        .append(": ").append(edge.acousticLoss()).append("\\n");
                out.append("\\nAuthoring map audit only. No actual doors opened, live actors moved or LAN state read.");
                return out.toString();
            }
            @Override protected void done(){
                try{test.setText(get());test.setCaretPosition(0);
                    caption.setText("Read-only source map audit complete; see diagnostic warnings, not release blockers.");
                }catch(Exception ex){
                    Throwable why=ex;while(why.getCause()!=null)why=why.getCause();
                    test.setText("NAV AUDIT ERROR: "+why.getMessage());
                    caption.setText("Map diagnostics failed; no live scene was touched.");
                }
            }
        }.execute();
    }
    private void exportRulebook(){
        javax.swing.JFileChooser files=new javax.swing.JFileChooser();
        files.setSelectedFile(new java.io.File("beckenburg-npc-rules.json"));
        if(files.showSaveDialog(dialog)!=javax.swing.JFileChooser.APPROVE_OPTION)return;
        try{
            org.beckenburg.editor.ai.NpcRuleExport.save(book,files.getSelectedFile().toPath());
            caption.setText("Exported source-derived NPC rulebook: "+files.getSelectedFile());
        }catch(java.io.IOException error){
            javax.swing.JOptionPane.showMessageDialog(dialog,error.getMessage(),
                "NPC rule export failed",javax.swing.JOptionPane.ERROR_MESSAGE);
        }
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
