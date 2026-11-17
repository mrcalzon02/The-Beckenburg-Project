package org.beckenburg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.beckenburg.GhostFieldGuide.Clue;
import org.beckenburg.GhostIdentity.Type;
import org.beckenburg.HardEvidenceQuota.Family;
import org.beckenburg.HardEvidenceQuota.Level;

/** Public guide and positive observations only. No host case identity, case seed or private enabled set. */
final class InvestigationDiagnostics {
    record Reading(Family family,Clue clue){}
    enum Test {
        EMF("EMF_READER",Family.EMF), AIR("THERMOMETER",Family.TEMPERATURE),
        BOX("SPIRIT_BOX",Family.SPIRIT_BOX), UV("UV_LIGHT",Family.UV),
        BOOK("GHOST_WRITING_BOOK",Family.WRITING), DOTS("DOTS_PROJECTOR",Family.DOTS),
        CAMERA("VIDEO_CAMERA",Family.ORBS), SWAB("ECTOPLASM_SAMPLER",Family.ECTOPLASM),
        RESONANCE("RESONANCE_TUNER",Family.RESONANCE), SEISMIC("SEISMIC_RECORDER",Family.SEISMIC),
        PRESSURE("BAROMETRIC_PROBE",Family.ATMOSPHERIC), PRISM("SPECTRAL_PRISM",Family.SHADOW),
        PLATE("OBJECT_IMPRINT_SCANNER",Family.IMPRINT), LASER("LASER_GRID",Family.LASER);
        final String tool;
        final Family family;
        Test(String tool,Family family){this.tool=tool;this.family=family;}
        String available(Collection<String> tools){
            if(tools.contains(tool))return tool;
            return this==RESONANCE&&tools.contains("SPECTRAL_FREQUENCY_ANALYZER")
                ?"SPECTRAL_FREQUENCY_ANALYZER":null;
        }
    }
    private record Profile(Set<Family> easy,Set<Family> medium,List<Reading> readings){}
    private static final EnumMap<Type,Profile> PROFILES=new EnumMap<>(Type.class);
    static{
        for(Type type:Type.values()){
            var row=GhostEvidenceMatrix.forGhost(type);
            var easy=GhostEvidenceMatrix.hardEvidenceFor(type);
            List<Reading> readings=new ArrayList<>();
            for(Family family:easy){
                switch(family){
                    case EMF->{
                        // 2–7 is also ordinary background. Repeated readings in that band cannot exclude a ghost.
                        if(row.emf()!=Clue.EMF_LOW_MID)readings.add(new Reading(family,row.emf()));
                    }
                    case TEMPERATURE->{
                        if(row.temperature()!=Clue.TEMPERATURE_NORMAL)readings.add(new Reading(family,row.temperature()));
                    }
                    case WRITING->{for(var style:row.writing())readings.add(new Reading(family,switch(style){
                        case CHILD_DRAWINGS->Clue.WRITING_CHILD;
                        case FRANTIC_SCRAWLS->Clue.WRITING_SCRAWL;
                        case RITUAL_CIRCLES->Clue.WRITING_RITUAL;
                    }));}
                    default->readings.add(new Reading(family,null));
                }
            }
            PROFILES.put(type,new Profile(Set.copyOf(easy),Set.copyOf(HardEvidenceQuota.signature(type)),List.copyOf(readings)));
        }
    }
    private InvestigationDiagnostics(){}
    private static Set<Family> supported(Type type,Level level){
        var p=PROFILES.get(type);return level==Level.EASY?p.easy:p.medium;
    }
    static List<Reading> possibleReadings(Type type,Level level){
        var p=PROFILES.get(type);
        return level==Level.EASY?p.readings:p.readings.stream().filter(r->p.medium.contains(r.family())).toList();
    }
    private static Family clueFamily(Clue clue){
        return switch(clue){
            case EMF_OVER_SIX,EMF_SUPPRESSION->Family.EMF;
            case EXTREME_COLD,EXTREME_HEAT->Family.TEMPERATURE;
            case SPIRIT_REPLY->Family.SPIRIT_BOX;
            case GHOST_WRITING,WRITING_CHILD,WRITING_SCRAWL,WRITING_RITUAL->Family.WRITING;
            case DOTS_PATTERN->Family.DOTS;
            case UV_RESIDUE->Family.UV;
            case SPIRIT_ORBS->Family.ORBS;
            default->null;
        };
    }
    static List<Type> candidates(FieldJournal notes,Set<Family> positives,Level level,Boolean ancientTint){
        var families=EnumSet.noneOf(Family.class);
        families.addAll(positives);
        List<Clue> clues=new ArrayList<>();
        for(Clue clue:GhostFieldGuide.activeClues()){
            if(notes.status(clue)!=FieldJournal.Status.SEEN||clue==Clue.EMF_LOW_MID)continue;
            Family family=clueFamily(clue);
            if(family!=null)families.add(family);
            if(clue.exclusionSafe||clue==Clue.DOTS_PATTERN)clues.add(clue);
        }
        if(level==Level.DEATH_WISH&&families.size()>1)return List.of();
        List<Type> remaining=new ArrayList<>();
        for(Type type:Type.values()){
            if(ancientTint!=null&&ancientTint!=type.ancient())continue;
            if(!supported(type,level).containsAll(families))continue;
            if(clues.stream().allMatch(clue->GhostFieldGuide.possible(type,clue)))remaining.add(type);
        }
        return List.copyOf(remaining);
    }
    static Type sufficientToIdentify(FieldJournal notes,Set<Family> positives,Level level,Boolean ancientTint){
        var remaining=candidates(notes,positives,level,ancientTint);
        return remaining.size()==1?remaining.get(0):null;
    }
    /** Rank a test by how many current alternatives a genuine positive would eliminate.
     * Non-detections score zero. Writing styles are alternative observable outcomes of one book.
     */
    static String recommend(List<Type> candidates,Set<Family> positives,Level level,Collection<String> availableTools){
        if(candidates.size()<2||(level==Level.DEATH_WISH&&!positives.isEmpty()))return null;
        String best=null;double bestScore=0;
        for(Test test:Test.values()){
            String tool=test.available(availableTools);
            if(tool==null||positives.contains(test.family))continue;
            var counts=new java.util.HashMap<Reading,Integer>();
            for(Type type:candidates){
                if(!supported(type,level).contains(test.family))continue;
                for(Reading r:PROFILES.get(type).readings)if(r.family()==test.family)counts.merge(r,1,Integer::sum);
            }
            double score=0;
            for(Type type:candidates){
                if(!supported(type,level).contains(test.family))continue;
                int outcomes=0,eliminations=0;
                for(Reading r:PROFILES.get(type).readings)if(r.family()==test.family){
                    outcomes++;eliminations+=candidates.size()-counts.get(r);
                }
                if(outcomes>0)score+=(double)eliminations/outcomes;
            }
            if(score>bestScore){bestScore=score;best=tool;}
        }
        return best;
    }
}
