package org.beckenburg;

/** No guessed pattern may become a verified clue or forge the host's ghost identity. */
public final class FieldJournalTest {
    private static void check(boolean good,String msg){if(!good)throw new AssertionError(msg);}
    private static void witnessNotes(){
        var crossCheck=new FieldJournal();
        for(int count:new int[]{0,1,2}){
            var clue=GhostFieldGuide.figureClue(GhostFieldGuide.FigureView.VIDEO,count);
            crossCheck.set(clue,FieldJournal.Status.SEEN,0,count);
        }
        var candidates=GhostFieldGuide.shortlist(crossCheck);
        for(var type:GhostIdentity.Type.values()){
            boolean restricted=switch(type.baseType()){
                case SHADE,BANSHEE,POLTERGEIST -> true;
                default -> false;
            };
            check(candidates.contains(type)!=restricted,"cross-check all three headcounts for "+type);
        }
        for(var view:GhostFieldGuide.FigureView.values())for(int count=0;count<=6;count++){
            if(count==0&&view==GhostFieldGuide.FigureView.DIRECT)continue;
            var clue=GhostFieldGuide.figureClue(view,count);
            var journal=new FieldJournal();
            journal.set(clue,FieldJournal.Status.UNSURE,0,1);
            check(GhostFieldGuide.shortlist(journal).size()==26,"unverified headcount never rules out a lineage");
            journal.set(clue,FieldJournal.Status.SEEN,0,2);
            var possible=GhostFieldGuide.shortlist(journal);
            check(possible.contains(GhostIdentity.Type.SPIRIT),"an unrestricted visible figure always remains possible");
            for(var type:GhostIdentity.Type.values()){
                boolean camera=view!=GhostFieldGuide.FigureView.DIRECT;
                check(possible.contains(type)==GhostTraits.figureAllowed(type,camera,count),"guide disagrees with host on "+type+" / "+view+" / "+count);
            }
            check(InvestigationDiagnostics.candidates(journal,java.util.Set.of(),HardEvidenceQuota.Level.DEATH_WISH,null)
                .equals(possible),"a witnessed figure is behavioral information, independent of hard-evidence quota");
        }
    }
    public static void main(String[] args){
        witnessNotes();
        FieldJournal book=new FieldJournal();
        check(GhostFieldGuide.shortlist(book).size()==GhostIdentity.Type.values().length,"all types initially possible");
        book.set(GhostFieldGuide.Clue.FLOATING,FieldJournal.Status.SEEN,0,9);
        book.set(GhostFieldGuide.Clue.DOOR_TO_TRAVEL,FieldJournal.Status.SEEN,1,12);
        var candidates=GhostFieldGuide.shortlist(book);
        check(candidates.contains(GhostIdentity.Type.BANSHEE),"floating door user includes Banshee");
        check(!candidates.contains(GhostIdentity.Type.WRAITH),"wall-phasing Wraith excluded by door-bound observation");
        book.set(GhostFieldGuide.Clue.PHASE_WALL,FieldJournal.Status.SEEN,2,18);
        check(GhostFieldGuide.shortlist(book).isEmpty(),"mutually exclusive clues are flagged, not silently reconciled");
        book.set(GhostFieldGuide.Clue.DOOR_TO_TRAVEL,FieldJournal.Status.UNKNOWN,1,19);
        check(GhostFieldGuide.shortlist(book).contains(GhostIdentity.Type.WRAITH),"corrected notes restore Wraith");
        book.clear();
        book.set(GhostFieldGuide.Clue.SOLO_ATTACK,FieldJournal.Status.SEEN,1,29);
        book.set(GhostFieldGuide.Clue.PASSIVE_DRAIN,FieldJournal.Status.SEEN,1,32);
        check(GhostFieldGuide.shortlist(book).size()==GhostIdentity.Type.values().length,"circumstantial patterns never hard eliminate");
        check(GhostFieldGuide.ordered(book).get(0)==GhostIdentity.Type.DEVIL,
              "isolated attack plus feeding favors Devil in live species profiles");
        FieldJournal.Note n=new FieldJournal.Note(GhostFieldGuide.Clue.PHASE_WALL,FieldJournal.Status.SEEN,2,77);
        check(FieldJournal.Note.decode(n.frame()).equals(n),"bounded shared note roundtrip");
        for(GhostIdentity.Type t:GhostIdentity.Type.values())check(!GhostFieldGuide.reference(t).isBlank(),"reference entry "+t);
        System.out.println("PASS: 26 identities at all supported living hunter counts, three media, uncertain notes and combined positive sightings");
        System.out.println("PASS: complete casebook roster, hard exclusions, behavioral ranking and shared note frames");
    }
}