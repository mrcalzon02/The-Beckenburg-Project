package org.beckenburg;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/** Local, atomic, versioned profile persistence. No executable deserialization. */
public final class ProfileStore {
    private final Path file;
    private int currency, experience;
    private final java.util.EnumMap<InvestigationSession.Role,Integer> disciplineXp=
        new java.util.EnumMap<>(InvestigationSession.Role.class);
    private final ArrayList<CampaignArchive.CaseFile> archive=new ArrayList<>();
    public int disciplineExperience(InvestigationSession.Role role){
        return disciplineXp.getOrDefault(java.util.Objects.requireNonNull(role),0);
    }
    public int level(InvestigationSession.Role role){
        return HunterProgression.level(disciplineExperience(role));
    }
    public java.util.List<CampaignArchive.CaseFile> archives(){return java.util.List.copyOf(archive);}
    public static int soloXp(InvestigationSession.Outcome outcome){
        return switch(outcome){case CLEANSED->100;case RETREATED->20;
            case LOST->0;case ACTIVE->throw new IllegalStateException("case still active");};
    }
    // Optional schema-v1 campaign fields preserve all existing save files.
    private boolean assistantHired;
    private long campaignSerial;
    private long storySeed;
    private int storyCheckpoint;
    /** 0-based index of the next unbeaten story chapter; 22 means complete. */
    public int storyCheckpoint(){return storyCheckpoint;}
    public boolean hasCampaignStory(){return storySeed!=0;}
    public long storySeed(){return storySeed;}
    public boolean chapterUnlocked(int stage){
        return stage>=0&&stage<CampaignStory.LENGTH&&stage<=storyCheckpoint;
    }
    /** The 22 assignments are generated ONCE, then pinned to this save forever. */
    public CampaignStory.Chapter campaignChapter(int stage){
        if(!hasCampaignStory()||!chapterUnlocked(stage))
            throw new IllegalStateException("Chapter not unlocked for this campaign");
        return CampaignStory.chapter(storySeed,stage);
    }
    public CampaignStory.Chapter dispatchStory(int stage)throws IOException{
        if(blocked)throw new IOException("Cannot dispatch after failed profile load");
        if(!chapterUnlocked(stage))throw new IllegalArgumentException("Chapter locked");
        var entry=CampaignBoardPanel.catalog().get(stage);
        if(!entry.playable())throw new IllegalArgumentException("Unique level geometry not yet available");
        if(!hasCampaignStory()){
            long seed;
            do{seed=new java.security.SecureRandom().nextLong();}while(seed==0);
            storySeed=seed;
            try{save();}catch(IOException problem){storySeed=0;throw problem;}
        }
        return campaignChapter(stage);
    }
    /** Only an actual successful mission advances the host-owned story. */
    public boolean settleStory(InvestigationSession session,int stage)throws IOException{
        if(!hasCampaignStory()||!chapterUnlocked(stage))
            throw new IllegalArgumentException("Chapter is not unlocked");
        var chapter=CampaignStory.chapter(storySeed,stage);
        if(!chapter.built()||session.map().site()!=SiteCatalog.mapFor(chapter.map())
            ||session.ghostVisualType()!=chapter.ghost())
            throw new IllegalArgumentException("Campaign receipt disagrees with the saved assignment");
        return addPayout(session,chapter.map(),stage);
    }
    private final java.util.EnumMap<SiteCatalog.Location,Integer> siteVisits=
        new java.util.EnumMap<>(SiteCatalog.Location.class);
    private final java.util.EnumMap<SiteCatalog.Location,Integer> siteClears=
        new java.util.EnumMap<>(SiteCatalog.Location.class);
    public static final int ASSISTANT_HIRE_COST=180;
    public boolean assistantHired(){return assistantHired;}
    public int visits(SiteCatalog.Location site){return siteVisits.getOrDefault(site,0);}
    public int clears(SiteCatalog.Location site){return siteClears.getOrDefault(site,0);}
    /** A single paid recruit arrives with their own issued thermometer. */
    public boolean hireAssistant()throws IOException{
        if(blocked)throw new IOException("Profile load failed; recruitment blocked");
        if(assistantHired||currency<ASSISTANT_HIRE_COST)return false;
        currency-=ASSISTANT_HIRE_COST;assistantHired=true;
        try{save();}catch(IOException e){currency+=ASSISTANT_HIRE_COST;assistantHired=false;throw e;}
        return true;
    }
    /** Durable dispatch serial: even restarts cannot silently repeat campaign seeds. */
    public long nextCampaignSeed(SiteCatalog.Location site)throws IOException{
        SiteCatalog.requirePlayable(site);
        if(blocked)throw new IOException("Profile load failed; contract dispatch blocked");
        long previous=campaignSerial;
        if(previous==Long.MAX_VALUE)throw new IOException("Campaign contract serial exhausted");
        campaignSerial++;
        try{save();}catch(IOException e){campaignSerial=previous;throw e;}
        return (campaignSerial*0x9E3779B97F4A7C15L)^((long)site.ordinal()<<43);
    }
    private final Set<String> awarded=new HashSet<>();
    private boolean blocked;
    private final int[][] medicines=new int[2][3];
    // Legacy starter instruments are issued; optional instruments must be purchased.
    private final java.util.EnumSet<EquipmentLoadout.Item> purchasedEquipment=
        java.util.EnumSet.of(EquipmentLoadout.Item.EMF,EquipmentLoadout.Item.THERMOMETER,
            EquipmentLoadout.Item.SPIRIT_BOX,EquipmentLoadout.Item.PHOTO_CAMERA);
    public boolean ownsEquipment(EquipmentLoadout.Item item){return purchasedEquipment.contains(item);}
    public java.util.Set<EquipmentLoadout.Item> equipment(){return java.util.Set.copyOf(purchasedEquipment);}
    public static int equipmentPrice(EquipmentLoadout.Item item){
        return switch(item){
            case EMF,THERMOMETER,SPIRIT_BOX,PHOTO_CAMERA->0;
            case UV_LAMP->150;case SALT->80;
        };
    }
    public boolean purchaseEquipment(EquipmentLoadout.Item item)throws IOException{
        if(blocked)throw new IOException("Profile cannot be modified after failed load");
        if(item==null||purchasedEquipment.contains(item))return false;
        int cost=equipmentPrice(item);
        if(cost<=0||currency<cost)return false;
        currency-=cost;purchasedEquipment.add(item);
        try{save();}catch(IOException e){currency+=cost;purchasedEquipment.remove(item);throw e;}
        return true;
    }
    public int supplyCount(FieldSupplies.Kind kind,FieldSupplies.Tier tier){
        return medicines[kind.ordinal()][tier.ordinal()];
    }
    public FieldSupplies.Tier strongest(FieldSupplies.Kind kind){
        for(int t=FieldSupplies.Tier.values().length-1;t>=0;t--)
            if(medicines[kind.ordinal()][t]>0)return FieldSupplies.Tier.values()[t];
        return null;
    }
    public boolean purchase(FieldSupplies.Kind kind,FieldSupplies.Tier tier)throws IOException{
        if(blocked)throw new IOException("Profile cannot be modified after failed load");
        if(currency<tier.price||supplyCount(kind,tier)>=4)return false;
        currency-=tier.price;medicines[kind.ordinal()][tier.ordinal()]++;
        try{save();}catch(IOException e){
            currency+=tier.price;medicines[kind.ordinal()][tier.ordinal()]--;throw e;
        }
        return true;
    }
    public boolean consume(FieldSupplies.Kind kind,FieldSupplies.Tier tier)throws IOException{
        if(blocked)throw new IOException("Profile cannot be modified after failed load");
        if(supplyCount(kind,tier)==0)return false;
        medicines[kind.ordinal()][tier.ordinal()]--;
        try{save();}catch(IOException e){medicines[kind.ordinal()][tier.ordinal()]++;throw e;}
        return true;
    }
    public ProfileStore(Path file) { this.file=file; }
    public int currency() { return currency; }
    public int experience() { return experience; }
    public static Path defaultPath() { return Path.of(System.getProperty("user.home"),".beckenburg","profile.properties"); }
    /** Ordinary LAN progression is NOT the host's single-player campaign wallet. */
    public static Path multiplayerPath(){
        return defaultPath().resolveSibling("multiplayer.properties");
    }
    public void load() throws IOException {
        if(!Files.exists(file))return;
        Properties p=new Properties();
        try(InputStream input=Files.newInputStream(file)){p.load(input);}
        blocked=true;
        if(!"1".equals(p.getProperty("schema")))throw new IOException("Unsupported profile schema");
        try{
            int money=Integer.parseInt(p.getProperty("currency"));
            int xp=Integer.parseInt(p.getProperty("experience"));
            if(money<0||xp<0)throw new NumberFormatException("negative balance");
            currency=money;experience=xp;
            disciplineXp.clear();
            for(InvestigationSession.Role role:InvestigationSession.Role.values()){
                int value=Integer.parseInt(p.getProperty("hunter.xp."+role.name(),"0"));
                if(value<0)throw new NumberFormatException("negative Hunter experience");
                disciplineXp.put(role,value);
            }
            archive.clear();archive.addAll(CampaignArchive.load(p));
            assistantHired=Boolean.parseBoolean(p.getProperty("campaign.assistant","false"));
            campaignSerial=Long.parseLong(p.getProperty("campaign.serial","0"));
            storySeed=Long.parseLong(p.getProperty("campaign.story.seed","0"));
            storyCheckpoint=Integer.parseInt(p.getProperty("campaign.story.next","0"));
            if(campaignSerial<0||storyCheckpoint<0||storyCheckpoint>CampaignStory.LENGTH
                ||(storyCheckpoint>0&&storySeed==0))
                throw new NumberFormatException("invalid campaign progress");
            siteVisits.clear();siteClears.clear();
            for(SiteCatalog.Location site:SiteCatalog.Location.values()){
                int visits=Integer.parseInt(p.getProperty("campaign.visits."+site.name(),"0"));
                int clears=Integer.parseInt(p.getProperty("campaign.clears."+site.name(),"0"));
                if(visits<0||clears<0||clears>visits||visits>1000000)
                    throw new NumberFormatException("invalid campaign results for "+site);
                siteVisits.put(site,visits);siteClears.put(site,clears);
            }
            for(FieldSupplies.Kind kind:FieldSupplies.Kind.values())
                for(FieldSupplies.Tier tier:FieldSupplies.Tier.values()){
                    String key="supply."+kind+"."+tier;
                    int count=Integer.parseInt(p.getProperty(key,"0"));
                    if(count<0||count>4)throw new NumberFormatException("invalid medicine stock");
                    medicines[kind.ordinal()][tier.ordinal()]=count;
                }
            for(EquipmentLoadout.Item item:EquipmentLoadout.Item.values()) {
                if(equipmentPrice(item)<=0)continue;
                // Persist enum identifiers, never a user-facing display label.
                // Earlier profiles used Item.toString() (e.g. "UV lamp").
                String stableKey="equipment."+item.name();
                String legacyKey="equipment."+item.toString();
                if(Boolean.parseBoolean(p.getProperty(stableKey,p.getProperty(legacyKey,"false"))))
                    purchasedEquipment.add(item);
            }
            String receipts=p.getProperty("awarded","");
            for(String receipt:receipts.split(",")) if(!receipt.isBlank()) {
                if(!receipt.matches("[0-9a-f\\-]{36}"))throw new NumberFormatException("invalid receipt");
                awarded.add(receipt);
            }
            blocked=false;
        }catch(IllegalArgumentException ex){throw new IOException("Invalid profile values",ex);}
    }
    /** Network case receipt is settled by the authoritative host, saved locally once.
     * Keep the same receipt set as solo payouts, using the host's UUID.
     */
    public boolean addNetworkPayout(java.util.UUID caseId,int credits,int xp)throws IOException {
        java.util.Objects.requireNonNull(caseId);
        if(credits<0||credits>100000||xp<0||xp>10000)
            throw new IllegalArgumentException("invalid network payout");
        if(blocked)throw new IOException("Profile load failed; payouts blocked.");
        String receipt=caseId.toString();
        if(awarded.contains(receipt))return false;
        int previousCredits=currency,previousExperience=experience;
        currency=Math.addExact(currency,credits);
        experience=Math.addExact(experience,xp);
        awarded.add(receipt);
        try{save();}catch(IOException ex){
            currency=previousCredits;experience=previousExperience;awarded.remove(receipt);throw ex;
        }
        return true;
    }
    public boolean addPayout(InvestigationSession session) throws IOException {
        return addPayout(session,null);
    }
    /** Campaign outcome and payout settle in ONE idempotent saved receipt. */
    public boolean addPayout(InvestigationSession session,SiteCatalog.Location campaignSite) throws IOException {
        return addPayout(session,campaignSite,-1);
    }
    private boolean addPayout(InvestigationSession session,SiteCatalog.Location campaignSite,
            int storyChapter) throws IOException {
        if(campaignSite!=null)SiteCatalog.requirePlayable(campaignSite);
        if(session.outcome()==InvestigationSession.Outcome.ACTIVE)throw new IllegalStateException("mission unresolved");
        if(blocked)throw new IOException("Profile load failed; payouts blocked until recovery.");
        if(awarded.contains(session.missionId()))return false;
        int previousCheckpoint=storyCheckpoint;
        int oldMoney=currency,oldXp=experience;
        int oldRoleXp=disciplineExperience(session.role());
        var oldArchive=new ArrayList<>(archive);
        int gainedXp=soloXp(session.outcome());
        int beforeLevel=level(session.role());
        int oldVisits=campaignSite==null?0:visits(campaignSite);
        int oldClears=campaignSite==null?0:clears(campaignSite);
        // Precompute all potentially throwing values before changing any
        // in-memory balance. The file save then commits the whole case receipt.
        int newCredits=Math.addExact(currency,session.payout());
        int newGlobalXp=Math.addExact(experience,gainedXp);
        int newRoleXp=Math.addExact(oldRoleXp,gainedXp);
        int newVisits=campaignSite==null?0:Math.addExact(oldVisits,1);
        int newClears=campaignSite==null?0:
            Math.addExact(oldClears,session.outcome()==InvestigationSession.Outcome.CLEANSED?1:0);
        CampaignArchive.CaseFile record=campaignSite==null?null:
            CampaignArchive.from(session,campaignSite,gainedXp,beforeLevel,
                HunterProgression.level(newRoleXp));
        currency=newCredits;experience=newGlobalXp;
        disciplineXp.put(session.role(),newRoleXp);
        if(storyChapter==storyCheckpoint&&
            session.outcome()==InvestigationSession.Outcome.CLEANSED)
            storyCheckpoint++;
        awarded.add(session.missionId());
        if(campaignSite!=null){
            if(archive.size()==CampaignArchive.LIMIT)archive.remove(0);
            archive.add(record);
            siteVisits.put(campaignSite,newVisits);
            siteClears.put(campaignSite,newClears);
        }
        try{save();}catch(IOException ex){
            currency=oldMoney;experience=oldXp;awarded.remove(session.missionId());
            storyCheckpoint=previousCheckpoint;
            disciplineXp.put(session.role(),oldRoleXp);
            archive.clear();archive.addAll(oldArchive);
            if(campaignSite!=null){siteVisits.put(campaignSite,oldVisits);siteClears.put(campaignSite,oldClears);}
            throw ex;
        }
        return true;
    }
    /**
     * Import a user-owned local progression file without changing the machine's
     * installation identity. Validate the staged bytes before replacing the
     * active profile; leave a versioned backup of the prior save.
     */
    public void importFrom(Path source) throws IOException {
        if (source == null) throw new IOException("No imported save selected");
        Path target = file.toAbsolutePath().normalize();
        Path selected = source.toAbsolutePath().normalize();
        if (selected.equals(target)) throw new IOException("Selected file is already the active save");
        if (!Files.isRegularFile(selected) || Files.size(selected) > 262144)
            throw new IOException("Imported file must be a readable Beckenburg save under 256 KiB");
        Path parent = target.getParent();
        if (parent == null) throw new IOException("Missing save directory");
        Files.createDirectories(parent);
        Path staged = Files.createTempFile(parent, "profile-import-", ".tmp");
        try {
            Files.copy(selected, staged, StandardCopyOption.REPLACE_EXISTING);
            ProfileStore candidate = new ProfileStore(staged);
            candidate.load(); // validate the actual staged bytes before touching the active save
            if (Files.exists(target)) {
                Path backup = parent.resolve("profile-before-import-" + java.util.UUID.randomUUID() + ".properties");
                Files.copy(target, backup);
            }
            try { Files.move(staged, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(staged, target, StandardCopyOption.REPLACE_EXISTING);
            }
            // Candidate was already validated, so in-memory state can be updated without reopening.
            currency = candidate.currency;
            experience = candidate.experience;
            assistantHired = candidate.assistantHired;
            disciplineXp.clear();disciplineXp.putAll(candidate.disciplineXp);
            archive.clear();archive.addAll(candidate.archive);
            campaignSerial = candidate.campaignSerial;
            storySeed = candidate.storySeed;
            storyCheckpoint = candidate.storyCheckpoint;
            siteVisits.clear();siteVisits.putAll(candidate.siteVisits);
            siteClears.clear();siteClears.putAll(candidate.siteClears);
            blocked = false; // successful valid import can recover a previously corrupt profile
            awarded.clear(); awarded.addAll(candidate.awarded);
            purchasedEquipment.clear(); purchasedEquipment.addAll(candidate.purchasedEquipment);
            for (int k = 0; k < medicines.length; k++)
                System.arraycopy(candidate.medicines[k], 0, medicines[k], 0, medicines[k].length);
        } finally { Files.deleteIfExists(staged); }
    }

    public void save() throws IOException {
        if(blocked) throw new IOException("Previous profile load failed; save blocked to preserve existing data.");
        Path parent=file.toAbsolutePath().getParent();
        if(parent==null)throw new IOException("Missing save directory");
        Files.createDirectories(parent);
        Path tmp=Files.createTempFile(parent,"profile-",".tmp");
        try{
            Properties p=new Properties();p.setProperty("schema","1");
            p.setProperty("campaign.assistant",Boolean.toString(assistantHired));
            p.setProperty("campaign.serial",Long.toString(campaignSerial));
            p.setProperty("campaign.story.seed",Long.toString(storySeed));
            p.setProperty("campaign.story.next",Integer.toString(storyCheckpoint));
            for(SiteCatalog.Location site:SiteCatalog.Location.values()){
                p.setProperty("campaign.visits."+site.name(),Integer.toString(visits(site)));
                p.setProperty("campaign.clears."+site.name(),Integer.toString(clears(site)));
            }
            for(InvestigationSession.Role role:InvestigationSession.Role.values())
                p.setProperty("hunter.xp."+role.name(),Integer.toString(disciplineExperience(role)));
            CampaignArchive.save(p,archive);
            p.setProperty("currency",Integer.toString(currency));p.setProperty("experience",Integer.toString(experience));
            p.setProperty("awarded",String.join(",",new ArrayList<>(awarded)));
            for(EquipmentLoadout.Item item:EquipmentLoadout.Item.values())
                if(equipmentPrice(item)>0)
                    p.setProperty("equipment."+item.name(),Boolean.toString(ownsEquipment(item)));
            for(FieldSupplies.Kind kind:FieldSupplies.Kind.values())
                for(FieldSupplies.Tier tier:FieldSupplies.Tier.values())
                    p.setProperty("supply."+kind+"."+tier,
                        Integer.toString(supplyCount(kind,tier)));
            try(OutputStream out=Files.newOutputStream(tmp)){p.store(out,"Beckenburg local profile v1");}
            try{Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
            catch(java.nio.file.AtomicMoveNotSupportedException e){Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING);}
        }finally{Files.deleteIfExists(tmp);}
    }
}