package org.beckenburg;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Diagnostics only. Failing to kill or identify is a measured AI-behavior
 * outcome, not a release-blocking failure. No artificial kills or evidence.
 * Input comes from real host-authoritative match state and ordinary clients.
 */
final class DevMatchOutcomeReport {
    private DevMatchOutcomeReport(){}

    static void write(Path root,DevAdminSnapshot state,List<DevMatchClientAi.Stats> stats,
                      int actualSeconds)throws java.io.IOException {
        int siteEntrants=0,totalCommands=0,accepted=0,rejected=0,attacks=0,frenzies=0,
            effectiveAttacks=0,scans=0,identificationAttempts=0,identifiedAccepts=0,
            ritualsAttempted=0,ritesStarted=0;
        for(var bot:stats){
            if(!bot.seat().equals("GHOST")&&bot.enteredSite())siteEntrants++;
            totalCommands+=bot.commands();accepted+=bot.accepted();
            rejected+=bot.rejected();attacks+=bot.attacks();
            frenzies+=bot.frenzyStarts();effectiveAttacks+=bot.effectiveAttacks();
            scans+=bot.goodScans();identificationAttempts+=bot.identifications();
            identifiedAccepts+=bot.correctResponses();ritualsAttempted+=bot.rituals();
            ritesStarted+=bot.ritesStarted();
        }
        int actualDamage=0,hostAttributedDeaths=0;
        StringBuilder hits=new StringBuilder();
        for(var hit:state.combatEvents()){
            actualDamage+=hit.damage();
            if(hit.downed())hostAttributedDeaths++;
            hits.append(hit.tick()).append(',').append(hit.seat()).append(',')
                .append(hit.action()).append(',').append(hit.damage()).append(',')
                .append(hit.downed()).append('\n');
        }
        int incapacitated=0;
        for(var hunter:state.hunters())if(!hunter.alive())incapacitated++;
        boolean correct=state.hunterIdentification()!=null
            &&state.hunterIdentification()==state.trueGhostType();
        boolean ritualCompleted=switch(state.outcome()){
            case CLEANSED,BANISHED,ABSOLVED->true;
            default->false;
        };
        boolean allKilled=state.outcome()==LanCase.Outcome.LOST
            &&incapacitated==6&&hostAttributedDeaths==6;
        String verdict=ritualCompleted?"HUNTERS_RITUAL_VICTORY":
            allKilled?"GHOST_ALL_KILLED":
            state.outcome()==LanCase.Outcome.LOST?"HUNTERS_LOST":
            state.outcome()==LanCase.Outcome.RETREATED?"HUNTERS_RETREATED":
            "UNRESOLVED_WITHIN_BUDGET";
        StringBuilder summary=new StringBuilder();
        summary.append("Beckenburg genuine seven-client match: behavioral outcomes (ADVISORY)\n");
        summary.append("Verdict: ").append(verdict).append("\n");
        summary.append("Authoritative tick: ").append(state.tick())
            .append("; requested seconds: ").append(actualSeconds)
            .append("; final match state: ").append(state.outcome()).append("\n");
        summary.append("Gameplay sockets: ").append(state.connectedPlayers())
            .append("/7; admin seat: 0\n");
        summary.append("Ghost true species: ").append(state.trueGhostType()).append("\n");
        summary.append("Hunter identification: ").append(state.hunterIdentification())
            .append("; correct: ").append(correct)
            .append("; attempts: ").append(identificationAttempts)
            .append("; accepted: ").append(identifiedAccepts).append("\n");
        summary.append("Ritual attempts: ").append(ritualsAttempted)
            .append("; starts: ").append(ritesStarted)
            .append("; completed: ").append(ritualCompleted).append("\n");
        summary.append("Ghost offensive commands attempted: ").append(attacks)
            .append("; frenzies accepted: ").append(frenzies)
            .append("; strikes/frenzy attacks accepted: ").append(effectiveAttacks).append("\n");
        summary.append("Host-applied ghost health damage: ").append(actualDamage)
            .append("; ghost-effect-attributed incapacitations: ").append(hostAttributedDeaths)
            .append("; all downed hunters: ").append(incapacitated).append("/6\n");
        summary.append("Hunter clients that physically entered the investigation site: ")
            .append(siteEntrants).append("/6\\n");
        summary.append("Actual host casefile observations: ")
            .append(state.observedCasefile().size()).append("\n");
        summary.append("Ordinary-client commands: ").append(totalCommands)
            .append("; accepted replies: ").append(accepted)
            .append("; rejected replies: ").append(rejected)
            .append("; accepted scan actions: ").append(scans).append("\n");
        summary.append("NOTE: no kills, identification or rituals are mocked or force-completed.\n");
        summary.append("NOTE: a real combat kill is counted ONLY from host-applied damage;\n");
        summary.append("the host-local combat trace is bounded to the latest 256 damage events.\n");
        summary.append("NOTE: this is early AI. A failure to win is a diagnostic, not a build failure.\n\n");
        for(var bot:stats)summary.append(bot.seat()).append(": commands ")
            .append(bot.commands()).append(" accepted ").append(bot.accepted())
            .append(" rejected ").append(bot.rejected())
            .append(" samples ").append(bot.observations())
            .append(" visitedRooms ").append(bot.roomsVisited())
            .append(" enteredSite ").append(bot.enteredSite())
            .append(" hypothesis ").append(bot.hypothesis())
            .append(" last: ").append(bot.state()).append('\n');
        Files.createDirectories(root);
        Files.writeString(root.resolve("objective-results.txt"),summary.toString(),StandardCharsets.UTF_8);
        Files.writeString(root.resolve("host-combat-events.csv"),
            "tick,seat,ghost_action,actual_hp_damage,downed\n"+hits,StandardCharsets.UTF_8);
        String json="{\n"+
            "  \"verdict\": \""+verdict+"\",\n"+
            "  \"match_outcome\": \""+state.outcome()+"\",\n"+
            "  \"tick\": "+state.tick()+",\n"+
            "  \"players\": "+state.connectedPlayers()+",\n"+
            "  \"admin_seats\": 0,\n"+
            "  \"ghost_type\": \""+state.trueGhostType()+"\",\n"+
            "  \"hunter_identification\": "+(state.hunterIdentification()==null?"null":"\""+state.hunterIdentification()+"\"")+",\n"+
            "  \"correct_identification\": "+correct+",\n"+
            "  \"identification_attempts\": "+identificationAttempts+",\n"+
            "  \"identification_accepted\": "+identifiedAccepts+",\n"+
            "  \"ritual_attempts\": "+ritualsAttempted+",\n"+
            "  \"ritual_starts\": "+ritesStarted+",\n"+
            "  \"ritual_completed\": "+ritualCompleted+",\n"+
            "  \"ghost_attack_commands\": "+attacks+",\n"+
            "  \"frenzies_accepted\": "+frenzies+",\n"+
            "  \"ghost_attacks_accepted\": "+effectiveAttacks+",\n"+
            "  \"actual_ghost_damage\": "+actualDamage+",\n"+
            "  \"ghost_attributed_kills\": "+hostAttributedDeaths+",\n"+
            "  \"incapacitated_hunters\": "+incapacitated+",\n"+
            "  \"site_entered_hunters\": "+siteEntrants+",\n"+
            "  \"accepted_scans\": "+scans+",\n"+
            "  \"casefile_samples\": "+state.observedCasefile().size()+",\n"+
            "  \"client_command_count\": "+totalCommands+",\n"+
            "  \"client_command_accepts\": "+accepted+",\n"+
            "  \"client_command_rejects\": "+rejected+"\n"+
            "}\n";
        Files.writeString(root.resolve("objective-results.json"),json,StandardCharsets.UTF_8);
        System.out.println("DEV MATCH OUTCOME: "+verdict+" / identity correct="+correct+
            " / host-confirmed kills="+hostAttributedDeaths+" / ritual="+ritualCompleted);
    }
}
