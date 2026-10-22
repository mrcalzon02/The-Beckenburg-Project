package org.beckenburg;

import java.util.*;

/**
 * Host-owned 22-chapter Castle Beckenburg story, entirely separate from free-play.
 * The opening 13 stages visit every original archetype once; six of ten Ancient
 * variants are drawn from the campaign seed for escalating middle chapters.
 * The final three Elders are fixed in canonical order. Unbuilt maps stay locked.
 */
public final class CampaignStory {
    public static final int LENGTH=22;
    private static final GhostIdentity.Type[] ELDERS={
        GhostIdentity.Type.ELDER_BLADE_DEMON,
        GhostIdentity.Type.ELDER_PLAGUE_DEMON,
        GhostIdentity.Type.ELDER_NOTHIC
    };
    public record Chapter(int ordinal,String name,String region,String history,
                          SiteCatalog.Location map,GhostIdentity.Type ghost){
        public Chapter{
            if(ordinal<0||ordinal>=LENGTH||name==null||name.isBlank()||
                history==null||history.isBlank()||ghost==null)
                throw new IllegalArgumentException("invalid campaign chapter");
        }
        public boolean built(){return map!=null&&map.playable();}
        public boolean elder(){return ghost.elder();}
        public String arrival(){
            return name+". "+history;
        }
        public String firstSurvey(){
            return "The Lore Master asks you to document the building's history and listen before attempting a rite.";
        }
        public String escalation(){
            return elder()?"The final chapter demands unusually careful preparation. Protect the team.":
                ordinal>=13?"Older accounts suggest a dangerous haunting. Keep the team together.":
                "Do not mistake a single disturbance for proof. Cross-check your instruments.";
        }
    }
    private CampaignStory(){}
    public static List<Chapter> generate(long savedSeed){
        var entries=CampaignBoardPanel.catalog();
        if(entries.size()!=LENGTH)throw new IllegalStateException("Story site roster must contain 22 unique locations");
        List<GhostIdentity.Type> originals=new ArrayList<>();
        List<GhostIdentity.Type> ancients=new ArrayList<>();
        for(var type:GhostIdentity.Type.values()){
            if(type.elder())continue;
            if(type.ancient())ancients.add(type);
            else originals.add(type);
        }
        if(originals.size()!=13||ancients.size()!=10)
            throw new IllegalStateException("Unexpected original/ancient roster size");
        Random random=new Random(savedSeed^0x4245434B43414D50L);
        Collections.shuffle(originals,random);
        Collections.shuffle(ancients,random);
        List<Chapter> chapters=new ArrayList<>(LENGTH);
        for(int i=0;i<LENGTH;i++){
            var entry=entries.get(i);
            GhostIdentity.Type ghost=i<13?originals.get(i):
                i<19?ancients.get(i-13):ELDERS[i-19];
            chapters.add(new Chapter(i,entry.name(),entry.region(),entry.dossier(),entry.map(),ghost));
        }
        return List.copyOf(chapters);
    }
    /** Same level, same map, same ghost, same anchor on retries and after reload. */
    public static long missionSeed(long savedSeed,int ordinal){
        if(ordinal<0||ordinal>=LENGTH)throw new IllegalArgumentException("invalid chapter");
        long x=savedSeed+0x9e3779b97f4a7c15L*(ordinal+1);
        x=(x^(x>>>30))*0xbf58476d1ce4e5b9L;
        x=(x^(x>>>27))*0x94d049bb133111ebL;
        return x^(x>>>31);
    }
    public static Chapter chapter(long seed,int ordinal){
        if(ordinal<0||ordinal>=LENGTH)throw new IllegalArgumentException("invalid chapter");
        return generate(seed).get(ordinal);
    }
    public static int stageFor(SiteCatalog.Location site){
        Objects.requireNonNull(site);
        for(var entry:CampaignBoardPanel.catalog())
            if(entry.map()==site)return CampaignBoardPanel.catalog().indexOf(entry);
        throw new IllegalArgumentException("Unknown campaign site: "+site);
    }
    /** Free play rarity is independent of the campaign's guaranteed boss sequence. */
    public static GhostIdentity.Type rollFreePlay(long seed){
        Random random=new Random(seed^0x47484f5354524f4cL);
        int chance=random.nextInt(10000);
        List<GhostIdentity.Type> pool=new ArrayList<>();
        for(var type:GhostIdentity.Type.values()){
            if(chance<15){if(type.elder())pool.add(type);}
            else if(chance<1450){if(type.ancient())pool.add(type);}
            else if(!type.elder()&&!type.ancient())pool.add(type);
        }
        return pool.get(random.nextInt(pool.size()));
    }
}
