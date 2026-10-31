package org.beckenburg;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/** Authoritative haunting folio shared by solo and network simulation. */
public final class GhostHauntingFolio {
    public enum Tier { AMBIENT, MINOR, MODERATE, MAJOR, DANGEROUS, SIGNATURE }
    public enum Visual { VEIL, SHADOW, STATIC, CHILL, PULSE, DISTORT }
    public record Entry(String id,String title,GhostDomain.Action action,Tier tier,
                        Visual visual,LanSound.Cue cue,int duration,
                        GhostIdentity.Type signatureType) {
        public Entry {
            if(id==null||!id.matches("[a-z_]{4,42}")||title==null||title.isBlank()
                ||action==null||tier==null||visual==null||cue==null
                ||duration<8||duration>100
                ||(action==GhostDomain.Action.SIGNATURE)!=(signatureType!=null)
                ||(tier==Tier.SIGNATURE)!=(signatureType!=null))
                throw new IllegalArgumentException("invalid haunting entry");
        }
    }
    public record Occurrence(Entry entry,int room,int startTick){
        public Occurrence {
            Objects.requireNonNull(entry);
            if(room<0||room>=HouseMap.ROOM_COUNT||startTick<0)
                throw new IllegalArgumentException("invalid haunting");
        }
        public int untilTick(){return startTick+entry.duration();}
        public boolean active(int tick,int localRoom){
            return room==localRoom&&tick>=startTick&&tick<untilTick();
        }
        public String frame(){return "HAUNT|"+entry.id()+"|"+room+"|"+startTick;}
    }
    private static final String DATA = """
corridor_exhale|Corridor Exhale|DRIFT|AMBIENT|VEIL|GHOST_DRIFT|22|
passing_shadow|Passing Shadow|DRIFT|AMBIENT|SHADOW|GHOST_DRIFT|19|
threshold_tremor|Threshold Tremor|DRIFT|AMBIENT|PULSE|GHOST_DRIFT|19|
airless_hall|Airless Hall|DRIFT|AMBIENT|CHILL|WHISPER|25|
distant_presence|Distant Presence|DRIFT|AMBIENT|STATIC|WHISPER|22|
wall_murmur|Wall Murmur|WHISPER|MINOR|VEIL|WHISPER|32|
shoulder_breath|Shoulder Breath|WHISPER|MINOR|SHADOW|WHISPER|28|
broken_sentence|Broken Sentence|WHISPER|MINOR|STATIC|WHISPER|30|
winter_breath|Winter Breath|WHISPER|MINOR|CHILL|WHISPER|31|
hushed_warning|Hushed Warning|WHISPER|MINOR|PULSE|WHISPER|29|
hollow_voice|Hollow Voice|WHISPER|MINOR|DISTORT|WHISPER|30|
doorway_sigh|Doorway Sigh|WHISPER|MINOR|VEIL|HORROR_STING_A|26|
lightning_static|Lightning Static|DISTURB|MODERATE|STATIC|DISTURB|42|
lamp_convulsion|Lamp Convulsion|DISTURB|MODERATE|PULSE|DISTURB|40|
bent_perspective|Bent Perspective|DISTURB|MODERATE|DISTORT|DISTURB|37|
window_shadow|Window Shadow|DISTURB|MODERATE|SHADOW|DISTURB|39|
floor_chill|Floor Chill|DISTURB|MODERATE|CHILL|DISTURB|40|
stairwell_mist|Stairwell Mist|DISTURB|MODERATE|VEIL|DISTURB|44|
radio_crawl|Radio Crawl|DISTURB|MODERATE|STATIC|HORROR_STING_B|33|
corner_darkening|Corner Darkening|DISTURB|MODERATE|SHADOW|HORROR_STING_C|41|
doorframe_figure|Doorframe Figure|MANIFEST|MAJOR|SHADOW|MANIFEST|54|
shrouded_witness|Shrouded Witness|MANIFEST|MAJOR|VEIL|MANIFEST|56|
bent_silhouette|Bent Silhouette|MANIFEST|MAJOR|DISTORT|MANIFEST|51|
frozen_silhouette|Frozen Silhouette|MANIFEST|MAJOR|CHILL|MANIFEST|58|
flicker_walker|Flicker Walker|MANIFEST|MAJOR|STATIC|MANIFEST|53|
silent_vigil|Silent Vigil|MANIFEST|MAJOR|PULSE|MANIFEST|57|
mist_crossing|Mist Crossing|MANIFEST|MAJOR|VEIL|HORROR_STING_A|50|
blindspot_watcher|Blind-Spot Watcher|MANIFEST|MAJOR|SHADOW|HORROR_STING_C|52|
pressure_wave|Pressure Wave|STRIKE|DANGEROUS|PULSE|STRIKE|67|
vision_collapse|Vision Collapse|STRIKE|DANGEROUS|DISTORT|STRIKE|67|
grasping_darkness|Grasping Darkness|STRIKE|DANGEROUS|SHADOW|STRIKE|70|
rending_static|Rending Static|STRIKE|DANGEROUS|STATIC|HORROR_STING_B|62|
death_chill|Death Chill|STRIKE|DANGEROUS|CHILL|STRIKE|68|
suffocating_veil|Suffocating Veil|STRIKE|DANGEROUS|VEIL|TENSION_BUILD|70|
bond_shattering|Bond Shattering|SIGNATURE|SIGNATURE|PULSE|SIGNATURE|95|POSSESSIVE
violent_cascade|Violent Cascade|SIGNATURE|SIGNATURE|DISTORT|SIGNATURE|95|POLTERGEIST
infernal_apparition|Infernal Apparition|SIGNATURE|SIGNATURE|SHADOW|SIGNATURE|95|DEMON
voice_in_flesh|Voice in the Flesh|SIGNATURE|SIGNATURE|STATIC|SIGNATURE|95|YOKAI
widows_lament|Widow's Lament|SIGNATURE|SIGNATURE|CHILL|SIGNATURE|95|BANSHEE
through_wall|Through the Wall|SIGNATURE|SIGNATURE|VEIL|SIGNATURE|95|WRAITH
camera_witness|Camera Witness|SIGNATURE|SIGNATURE|SHADOW|SIGNATURE|95|SHADE
lens_borne_figure|Lens-Borne Figure|SIGNATURE|SIGNATURE|STATIC|SIGNATURE|95|SPECTER
quietus_revelation|Quietus Revelation|SIGNATURE|SIGNATURE|PULSE|SIGNATURE|95|SPIRIT
glacial_revelation|Glacial Revelation|SIGNATURE|SIGNATURE|CHILL|SIGNATURE|95|HANTU
pact_bearer|Pact Bearer|SIGNATURE|SIGNATURE|DISTORT|SIGNATURE|95|DEVIL
borrowed_visage|Borrowed Visage|SIGNATURE|SIGNATURE|SHADOW|SIGNATURE|95|MIMIC
paired_revelation|Paired Revelation|SIGNATURE|SIGNATURE|VEIL|SIGNATURE|95|TWINS
executioners_coming|Executioner's Coming|SIGNATURE|SIGNATURE|PULSE|SIGNATURE|95|ELDER_BLADE_DEMON
plague_procession|Plague Procession|SIGNATURE|SIGNATURE|VEIL|SIGNATURE|95|ELDER_PLAGUE_DEMON
unblinking_judgment|Unblinking Judgment|SIGNATURE|SIGNATURE|DISTORT|SIGNATURE|95|ELDER_NOTHIC
        """;
    private static final List<Entry> ALL=load();
    private static final Map<String,Entry> INDEX=index();
    private static List<Entry> load() {
        List<Entry> out=new ArrayList<>();
        for(String line:DATA.strip().split("\\R")){
            String[] p=line.strip().split("\\|",-1);
            if(p.length!=8)throw new IllegalStateException("invalid haunting folio row: "+line);
            out.add(new Entry(p[0],p[1],GhostDomain.Action.valueOf(p[2]),
                Tier.valueOf(p[3]),Visual.valueOf(p[4]),LanSound.Cue.valueOf(p[5]),
                Integer.parseInt(p[6]),p[7].isBlank()?null:GhostIdentity.Type.valueOf(p[7])));
        }
        return List.copyOf(out);
    }
    private static Map<String,Entry> index(){
        Map<String,Entry> index=new HashMap<>();
        for(Entry entry:ALL)
            if(index.putIfAbsent(entry.id(),entry)!=null)
                throw new IllegalStateException("duplicate haunting event ID "+entry.id());
        for(GhostIdentity.Type type:GhostIdentity.Type.values())
            if(ALL.stream().noneMatch(e->e.signatureType()==type.baseType()))
                throw new IllegalStateException("missing signature appearance "+type);
        return Map.copyOf(index);
    }
    private final Random random;
    private final EnumMap<GhostDomain.Action,String> previous=
        new EnumMap<>(GhostDomain.Action.class);
    public GhostHauntingFolio(long seed){random=new Random(seed);}
    public static List<Entry> entries(){return ALL;}
    public static Entry entry(String id){return INDEX.get(id);}
    public Occurrence choose(GhostDomain.Action action,GhostIdentity.Type species,int room,int tick){
        Objects.requireNonNull(action);Objects.requireNonNull(species);
        List<Entry> matching=new ArrayList<>();
        for(Entry entry:ALL)
            if(entry.action()==action &&(action!=GhostDomain.Action.SIGNATURE
                    ||entry.signatureType()==species.baseType()))
                matching.add(entry);
        if(matching.isEmpty())return null;
        int choice=random.nextInt(matching.size());
        if(matching.size()>1&&matching.get(choice).id().equals(previous.get(action)))
            choice=(choice+1+random.nextInt(matching.size()-1))%matching.size();
        Entry selected=matching.get(choice);
        previous.put(action,selected.id());
        return new Occurrence(selected,room,tick);
    }
    public static Occurrence decode(String frame){
        List<String> p=LanProtocol.parts(frame);
        if(p.size()!=4||!p.get(0).equals("HAUNT"))throw new IllegalArgumentException("invalid haunt");
        Entry entry=INDEX.get(p.get(1));
        if(entry==null)throw new IllegalArgumentException("unknown haunt");
        return new Occurrence(entry,LanProtocol.integer(p.get(2),0,HouseMap.ROOM_COUNT-1),
            LanProtocol.integer(p.get(3),0,99999999));
    }

    /** One animated, event-scoped spectral texture layer, never screen bars.
     * When several hauntings overlap, render the strongest active effect once:
     * avoid stacking multiple full-screen passes and hiding the investigation.
     */
    public static void paint(int[] pixels,int width,int height,List<Occurrence> events,int room,int tick){
        if(room<0||events==null||events.isEmpty())return;
        Occurrence strongest=null;
        double peak=0;
        for(Occurrence event:events){
            if(event==null||!event.active(tick,room))continue;
            int priority=switch(event.entry().tier()){
                case AMBIENT->1;case MINOR->2;case MODERATE->3;
                case MAJOR->4;case DANGEROUS->5;case SIGNATURE->6;
            };
            double weight=priority*Math.sin(Math.PI*(tick-event.startTick())
                /event.entry().duration());
            if(weight>peak){peak=weight;strongest=event;}
        }
        if(strongest==null)return;
        GhostHauntingChoreography.Score score=
            GhostHauntingChoreography.forEvent(strongest.entry().id());
        HauntingScreenFx.paint(pixels,width,height,strongest,tick,
            score==null?null:score.motif());
    }

}
