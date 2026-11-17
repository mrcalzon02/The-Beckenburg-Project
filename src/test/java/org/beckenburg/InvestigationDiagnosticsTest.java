package org.beckenburg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.beckenburg.GhostIdentity.Type;
import org.beckenburg.HardEvidenceQuota.Family;
import org.beckenburg.HardEvidenceQuota.Level;

/** Positive-only instrument coverage; no free negatives, admin identity, green tint, or behavioral tell. */
public final class InvestigationDiagnosticsTest {
    private record Notebook(FieldJournal notes,EnumSet<Family> families,List<Type> candidates){}
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}

    private static Notebook observe(Type type,Level level,Set<Family> enabled,Set<String> gear){
        FieldJournal notes=new FieldJournal();
        EnumSet<Family> positive=EnumSet.noneOf(Family.class);
        Set<Family> equipped=EnumSet.noneOf(Family.class);
        for(var test:InvestigationDiagnostics.Test.values())if(test.available(gear)!=null)equipped.add(test.family);
        for(var r:InvestigationDiagnostics.possibleReadings(type,level)){
            if(!enabled.contains(r.family())||!equipped.contains(r.family()))continue;
            positive.add(r.family());
            if(r.clue()!=null)notes.set(r.clue(),FieldJournal.Status.SEEN,0,1);
        }
        var possible=InvestigationDiagnostics.candidates(notes,positive,level,null);
        check(possible.contains(type),"Guide excluded the correct "+type+" / "+level+" / "+enabled);
        return new Notebook(notes,positive,possible);
    }
    private static String names(Iterable<?> values){
        List<String> names=new ArrayList<>();
        for(Object value:values)names.add(value.toString());
        return String.join(" ",names);
    }
    private static void sampleTiers(){
        int samples=0;
        for(var test:InvestigationDiagnostics.Test.values()){
            if(test==InvestigationDiagnostics.Test.UV)continue;
            var reader=new FieldEquipment();
            int slot=0;
            for(int i=0;i<FieldEquipment.SLOTS;i++)if(test.tool.equals(reader.at(i)))slot=i;
            reader.equip(slot,test.tool);
            for(var tier:EquipmentTiers.Tier.values()){
                if(tier!=EquipmentTiers.Tier.I)reader.upgrade(test.tool,tier);
                var stage=reader.stage(test.tool);
                if(stage.intervalTicks()==0)continue;
                int tick=1000*(tier.ordinal()+1);
                check(reader.ready(test.tool,tick),"New sample must be ready");
                reader.sampled(test.tool,tick);
                check(!reader.ready(test.tool,tick+stage.intervalTicks()-1)&&reader.ready(test.tool,tick+stage.intervalTicks()),
                    "Host instrument does not enforce stated refresh period for "+stage);
                samples++;
            }
        }
        check(samples>20,"insufficient tested tiered instruments");
    }
    public static void main(String[] args)throws Exception{
        var starting=new FieldEquipment();
        Set<String> starter=new HashSet<>();
        for(int i=0;i<FieldEquipment.SLOTS;i++)starter.add(starting.at(i));
        var campaignStart=new FieldEquipment();
        campaignStart.configureCampaign(new ProfileStore(Path.of("unused-profile-for-hardware-audit")));
        Set<String> campaignBelt=new HashSet<>();
        for(int i=0;i<FieldEquipment.SLOTS;i++)campaignBelt.add(campaignStart.at(i));
        check(campaignBelt.contains("PHOTO_CAMERA")&&!campaignBelt.contains("UV_LIGHT"),"actual fresh campaign belt required");
        Set<String> rack=Set.copyOf(FieldEquipment.workingTools());
        var fresh=new FieldJournal();
        fresh.set(GhostFieldGuide.Clue.EMF_LOW_MID,FieldJournal.Status.SEEN,0,1);
        fresh.set(GhostFieldGuide.Clue.TEMPERATURE_NORMAL,FieldJournal.Status.SEEN,0,1);
        check(InvestigationDiagnostics.candidates(fresh,Set.of(),Level.EASY,null).size()==Type.values().length,
            "normal field and air readings cannot eliminate a ghost");
        check(InvestigationDiagnostics.sufficientToIdentify(fresh,Set.of(),Level.EASY,null)==null,"duplicate baselines cannot identify");
        check(InvestigationDiagnostics.recommend(List.of(Type.DEMON,Type.ANCIENT_DEMON),Set.of(),Level.EASY,
            Set.of("BAROMETRIC_PROBE"))==null,"two identical positive results cannot distinguish the pair");
        check("ECTOPLASM_SAMPLER".equals(InvestigationDiagnostics.recommend(List.of(Type.DEMON,Type.ANCIENT_DEMON),
            Set.of(),Level.EASY,Set.of("ECTOPLASM_SAMPLER","BAROMETRIC_PROBE"))),"advise the discriminating physical sample");
        check(InvestigationDiagnostics.candidates(fresh,Set.of(Family.SHADOW,Family.IMPRINT),Level.DEATH_WISH,null).isEmpty(),
            "single-family difficulty must not validate incompatible notes");
        var photographs=new FieldJournal();
        photographs.set(GhostFieldGuide.Clue.REMOTE_EMPTY,FieldJournal.Status.SEEN,0,10);
        var emptyVideo=InvestigationDiagnostics.candidates(photographs,Set.of(),Level.EASY,null);
        check(emptyVideo.contains(Type.SHADE)&&emptyVideo.contains(Type.SPECTER)&&emptyVideo.contains(Type.SPIRIT)
            &&!emptyVideo.contains(Type.BANSHEE)&&!emptyVideo.contains(Type.POLTERGEIST),
            "empty-room footage is compatible with unrestricted figures and excludes occupied-only figures");
        photographs.set(GhostFieldGuide.Clue.PHOTO_SHAPE,FieldJournal.Status.SEEN,0,11);
        check(InvestigationDiagnostics.candidates(photographs,Set.of(),Level.EASY,null).contains(Type.YOKAI),
            "an orb producer can also manifest visibly on a handheld photo");
        sampleTiers();
        List<String> csv=new ArrayList<>();
        csv.add("difficulty,species,enabled_families,campaign_positive_families,campaign_remaining,campaign_remaining_if_visible_photo,lan_positive_families,lan_remaining,rack_positive_families,rack_remaining,campaign_best_next_rack_tool,lan_best_next_rack_tool,campaign_candidates,lan_candidates,rack_candidates");
        for(Level level:Level.values()){
            int count=0,starterUnique=0,campaignUnique=0,rackUnique=0,starterTotal=0,campaignTotal=0,rackTotal=0;
            for(Type type:Type.values()){
                Set<Family> allowed=level==Level.EASY?GhostEvidenceMatrix.hardEvidenceFor(type):HardEvidenceQuota.signature(type);
                List<Set<Family>> contracts=level==Level.DEATH_WISH?
                    allowed.stream().map(Set::of).toList():List.of(allowed);
                for(var enabled:contracts){
                    count++;
                    var campaign=observe(type,level,enabled,campaignBelt);
                    var base=observe(type,level,enabled,starter);
                    var all=observe(type,level,enabled,rack);
                    campaignTotal+=campaign.candidates.size();
                    if(campaign.candidates.size()==1)campaignUnique++;
                    starterTotal+=base.candidates.size();rackTotal+=all.candidates.size();
                    if(base.candidates.size()==1)starterUnique++;
                    if(all.candidates.size()==1)rackUnique++;
                    String next=InvestigationDiagnostics.recommend(base.candidates,base.families,level,rack);
                    // A person can independently see a figure in an actual photo. The shutter receipt alone proves nothing.
                    campaign.notes.set(GhostFieldGuide.Clue.PHOTO_SHAPE,FieldJournal.Status.SEEN,0,2);
                    int withPhoto=InvestigationDiagnostics.candidates(campaign.notes,campaign.families,level,null).size();
                    String campaignNext=InvestigationDiagnostics.recommend(campaign.candidates,campaign.families,level,rack);
                    csv.add(String.join(",",level.name(),type.name(),names(enabled),names(campaign.families),
                        String.valueOf(campaign.candidates.size()),String.valueOf(withPhoto),names(base.families),
                        String.valueOf(base.candidates.size()),names(all.families),String.valueOf(all.candidates.size()),
                        campaignNext==null?"":campaignNext,next==null?"":next,names(campaign.candidates),names(base.candidates),names(all.candidates)));
                }
            }
            check(count==(level==Level.DEATH_WISH?78:26),"audit must cover every case-family setting");
            check(starterUnique<count/2,"starter belt is solving most contracts without advanced fieldwork");
            if(level!=Level.DEATH_WISH)check(rackUnique>starterUnique&&rackTotal<starterTotal,
                "rack instruments must improve positive discrimination");
            System.out.printf(java.util.Locale.ROOT,"%s: campaign %d/%d / %.2f left, LAN %d/%d / %.2f left, rack %d/%d / %.2f left%n",
                level.name(),campaignUnique,count,campaignTotal/(double)count,starterUnique,count,starterTotal/(double)count,
                rackUnique,count,rackTotal/(double)count);
        }
        if(args.length>0)Files.write(Path.of(args[0]),csv);
        System.out.println("PASS positive-only starter/rack coverage, recommended discriminator, limited-family uncertainty, and instrument tier timing");
    }
}
