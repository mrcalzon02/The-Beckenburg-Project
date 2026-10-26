package org.beckenburg;

import java.util.Arrays;

/** Player checkmarks filter a fixed ghost roster, never a hidden host species. */
public final class ManualEvidenceJournalTest {
    static void ok(boolean condition,String why){if(!condition)throw new AssertionError(why);}
    public static void main(String[] args){
        FieldJournal journal=new FieldJournal();
        ok(GhostFieldGuide.shortlist(journal).size()==GhostIdentity.Type.values().length,"all ghost identities initially possible");
        var emf=GhostFieldGuide.Clue.EMF_OVER_SIX;
        var zero=GhostFieldGuide.Clue.EMF_SUPPRESSION;
        journal.set(emf,FieldJournal.Status.SEEN,0,1);
        ok(!GhostFieldGuide.remainsPossible(GhostIdentity.Type.SHADE,journal),
           "high EMF manually greys Shade");
        ok(!GhostFieldGuide.remainsPossible(GhostIdentity.Type.SPIRIT,journal)
            &&GhostFieldGuide.remainsPossible(GhostIdentity.Type.DEMON,journal),
            "high EMF excludes low-band Spirit and retains high-band Demon");
        journal.set(emf,FieldJournal.Status.UNKNOWN,0,2);
        ok(GhostFieldGuide.shortlist(journal).size()==GhostIdentity.Type.values().length,
           "unticking restores ghosts without rerunning a scan");
        journal.set(zero,FieldJournal.Status.SEEN,1,3);
        var suppressed=GhostFieldGuide.shortlist(journal);
        ok(suppressed.size()>1 && suppressed.size()<GhostIdentity.Type.values().length
            &&suppressed.contains(GhostIdentity.Type.SHADE)
            &&suppressed.contains(GhostIdentity.Type.SPECTER)
            &&suppressed.contains(GhostIdentity.Type.HANTU),
            "suppressed EMF narrows candidates, never reveals one ghost immediately");
        journal.set(emf,FieldJournal.Status.SEEN,0,4);
        ok(GhostFieldGuide.shortlist(journal).isEmpty(),
            "contradictory ticks show zero possible ghosts without correcting player");
        ok(journal.status(zero)==FieldJournal.Status.SEEN
            &&journal.status(emf)==FieldJournal.Status.SEEN,
            "contradictions never delete the user's conclusions");
        journal.clear();
        journal.set(GhostFieldGuide.Clue.EXTREME_HEAT,FieldJournal.Status.SEEN,2,5);
        ok(!GhostFieldGuide.remainsPossible(GhostIdentity.Type.SPIRIT,journal)
            &&GhostFieldGuide.remainsPossible(GhostIdentity.Type.DEMON,journal),
            "heat checklist uses same polarity as live ghost simulation");
        journal.clear();
        journal.set(GhostFieldGuide.Clue.WRITING_CHILD,FieldJournal.Status.SEEN,2,7);
        ok(GhostFieldGuide.remainsPossible(GhostIdentity.Type.SPIRIT,journal)
           &&!GhostFieldGuide.remainsPossible(GhostIdentity.Type.DEMON,journal),
           "childlike drawings have defined candidate ghost families");
        journal.set(GhostFieldGuide.Clue.WRITING_RITUAL,FieldJournal.Status.SEEN,2,8);
        ok(GhostFieldGuide.remainsPossible(GhostIdentity.Type.POSSESSIVE,journal),
           "multiple writing styles can overlap without magical one-species codes");
        journal.clear();
        journal.set(GhostFieldGuide.Clue.GHOST_WRITING,FieldJournal.Status.SEEN,0,15);
        ok(GhostFieldGuide.remainsPossible(GhostIdentity.Type.SHADE,journal)
            &&GhostFieldGuide.remainsPossible(GhostIdentity.Type.YOKAI,journal)
            &&!GhostFieldGuide.remainsPossible(GhostIdentity.Type.HANTU,journal),
            "generic writing admits real Yokai ritual circles and excludes non-writing Hantu");
        // A generic physical writing mark is one family: evaluate actual
        // production and manual shortlisting for every one of 26 identities.
        for(GhostIdentity.Type type:GhostIdentity.Type.values()){
            boolean writer=GhostEvidenceProfile.availableWriting(type).length>0;
            ok(GhostEvidenceProfile.compatible(type,GhostFieldGuide.Clue.GHOST_WRITING)==writer
                &&GhostFieldGuide.remainsPossible(type,journal)==writer,
                "manual journal filtering agrees with writing producer for "+type);
            boolean cold=GhostEvidenceProfile.compatible(type,GhostFieldGuide.Clue.EXTREME_COLD);
            boolean hot=GhostEvidenceProfile.compatible(type,GhostFieldGuide.Clue.EXTREME_HEAT);
            ok(!(cold&&hot),"no ghost can be inherently freezing and very hot at once: "+type);
        }
        journal.clear();
        journal.set(GhostFieldGuide.Clue.TEMPERATURE_NORMAL,FieldJournal.Status.SEEN,0,20);
        journal.set(GhostFieldGuide.Clue.EMF_NORMAL,FieldJournal.Status.SEEN,0,21);
        ok(GhostFieldGuide.shortlist(journal).size()==GhostIdentity.Type.values().length,
            "ordinary EMF and temperature cannot grey any ghost");
        journal.clear();
        ok(Arrays.stream(GhostFieldGuide.activeClues()).noneMatch(c->c.name().startsWith("COLD_ANCHOR")),
           "rejected synthetic evidence subtypes remain hidden");
        ok(GhostFieldGuide.activeClues().length>=18,
           "manual checklist covers equipment, writing subtypes and behavior");
        System.out.println("PASS: manually ticked evidence greys ghost names; undo/contradiction never verifies");
    }
}
