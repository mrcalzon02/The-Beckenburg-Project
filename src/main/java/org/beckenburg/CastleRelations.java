package org.beckenburg;

import java.util.*;

/**
 * Castle residents are people, not workstation hitboxes. Every conversation is
 * a bounded, branching story graph derived from the owning campaign's progress.
 * Narrative can warn, speculate or mislead; it never certifies ghost evidence.
 */
public final class CastleRelations {
    private CastleRelations(){}
    public enum Resident {
        MATTHIS_ROOK("Matthis Rook","Lore Master",36.00,10.35),
        ELSBETH_VALE("Elsbeth Vale","Quartermaster",6.35,4.75),
        ROWAN_PIKE("Rowan Pike","Communications",23.75,17.40),
        TOMAS_GREAVES("Tomas Greaves","Expedition Marshal",14.80,16.35),
        MIRELLE_ASH("Sister Mirelle Ash","Ritual Scholar",6.15,17.30);
        public final String name,profession;
        public final double x,z;
        Resident(String name,String profession,double x,double z){
            this.name=name;this.profession=profession;this.x=x;this.z=z;
        }
    }
    public enum Topic { GREETING, BACKGROUND, THE_LAST_CASE, NEXT_LOCATION,
        FIELD_ADVICE, PERSONAL, QUEST, FOLLOW_UP, FAREWELL }
    public record Context(int completed,int hunterLevel){
        public Context{
            if(completed<0||completed>CampaignStory.LENGTH||hunterLevel<1
                ||hunterLevel>HunterProgression.MAX_LEVEL)
                throw new IllegalArgumentException("invalid host campaign snapshot");
        }
        public int next(){return Math.min(completed,CampaignStory.LENGTH-1);}
    }
    public record Choice(String label,Topic destination){
        public Choice{
            if(label==null||label.isBlank()||destination==null)
                throw new IllegalArgumentException("invalid dialogue choice");
        }
    }
    public record Page(String heading,String spoken,List<Choice> choices){
        public Page{
            if(heading==null||spoken==null||spoken.isBlank()||choices==null)
                throw new IllegalArgumentException("invalid dialogue page");
            choices=List.copyOf(choices);
        }
    }
    /** Campaign social snapshot belongs to host; guests cannot write host trust.
     * Network transport sends only progress and relationship scores, not saves.
     */
    public record HostView(Context context,List<Integer> trust){
        public HostView{
            Objects.requireNonNull(context);
            if(trust==null||trust.size()!=Resident.values().length||
                trust.stream().anyMatch(n->n==null||n<0||n>100))
                throw new IllegalArgumentException("invalid social snapshot");
            trust=List.copyOf(trust);
        }
        public int trust(Resident resident){return trust.get(resident.ordinal());}
        public String frame(){
            var fields=new StringJoiner(",");
            for(int score:trust)fields.add(Integer.toString(score));
            return "STORYSOCIAL|"+context.completed()+"|"+context.hunterLevel()+"|"+fields;
        }
    }
    public static HostView hostView(ProfileStore profile){
        var scores=new ArrayList<Integer>();
        for(var npc:Resident.values())scores.add(profile.relationship(npc));
        return new HostView(context(profile),scores);
    }
    public static HostView decodeHostView(String frame){
        var parts=LanProtocol.parts(frame);
        if(parts.size()!=4||!parts.get(0).equals("STORYSOCIAL"))
            throw new IllegalArgumentException("invalid story dialogue frame");
        var progress=new Context(Integer.parseInt(parts.get(1)),Integer.parseInt(parts.get(2)));
        String[] tokens=parts.get(3).split(",",-1);
        if(tokens.length!=Resident.values().length)
            throw new IllegalArgumentException("invalid number of resident scores");
        var scores=new ArrayList<Integer>();
        for(String value:tokens)scores.add(Integer.parseInt(value));
        return new HostView(progress,scores);
    }
    public static Context context(ProfileStore host){
        int best=1;
        for(var role:InvestigationSession.Role.values())
            best=Math.max(best,host.level(role));
        return new Context(host.storyCheckpoint(),best);
    }
    /** A limited relationship tier: mission advancement and class expertise
     * open discussions, while one-time personal exchanges build confidence. */
    public static int rank(Context context,int trust){
        if(trust<0||trust>100)throw new IllegalArgumentException("invalid trust");
        return Math.min(5,1+context.completed()/5+
            (context.hunterLevel()-1)/6+trust/6);
    }
    /** Draws candidate residents from their own positions, not workstation
     * coordinates; the same room and camera bearing are both required.
     */
    public static Resident aimed(CastleHubMap map,double x,double z,double yaw){
        if(map==null||!map.traversable(x,z)||!Double.isFinite(yaw))return null;
        var playerRoom=CastleRooms.at(x,0,z);
        if(playerRoom==null)return null;
        Resident best=null;double bestDistance=Double.MAX_VALUE;
        for(Resident resident:Resident.values()){
            var npcRoom=CastleRooms.at(resident.x,0,resident.z);
            if(npcRoom==null||!npcRoom.id().equals(playerRoom.id()))continue;
            double dx=resident.x-x,dz=resident.z-z,dist=Math.hypot(dx,dz);
            if(dist<.5||dist>3.2)continue;
            if((dx*Math.sin(yaw)+dz*Math.cos(yaw))/dist<.84)continue;
            if(dist<bestDistance){best=resident;bestDistance=dist;}
        }
        return best;
    }
    public static Page page(Resident resident,Context context,int trust,Topic topic){
        Objects.requireNonNull(resident);Objects.requireNonNull(context);
        Objects.requireNonNull(topic);
        int relation=rank(context,trust);
        var order=CampaignBoardPanel.catalog();
        int completed=context.completed();
        var previous=completed==0?null:order.get(completed-1);
        var next=completed==CampaignStory.LENGTH?null:order.get(completed);
        String last=previous==null?"your first expedition":previous.name();
        String upcoming=next==null?"the finished campaign":next.name();
        String region=next==null?"the Castle":next.region();
        String description=next==null?"Every field report is now on file.":next.dossier();
        String prefix=switch(resident){
            case MATTHIS_ROOK->"History is not a verdict. The margins often contradict the official report. ";
            case ELSBETH_VALE->"I would rather mend a coat than identify its wearer in the morgue. ";
            case ROWAN_PIKE->"Signals travel, but not every voice on a line is living. ";
            case TOMAS_GREAVES->"Maps show the road. They rarely show what waits beside it. ";
            case MIRELLE_ASH->"A ritual is a promise backed by evidence, not by courage. ";
        };
        List<Choice> root=List.of(
            new Choice("What do you know of this world?",Topic.BACKGROUND),
            new Choice("What did our last case change?",Topic.THE_LAST_CASE),
            new Choice("Tell me about the next location.",Topic.NEXT_LOCATION),
            new Choice("Give me field advice.",Topic.FIELD_ADVICE),
            new Choice("Any work or leads for me?",Topic.QUEST),
            new Choice("Tell me about yourself.",Topic.PERSONAL),
            new Choice("Leave conversation",Topic.FAREWELL));
        List<Choice> continueTalk=List.of(
            new Choice("Could you explain further?",Topic.FOLLOW_UP),
            new Choice("What should I investigate next?",Topic.NEXT_LOCATION),
            new Choice("Other subjects",Topic.GREETING),
            new Choice("Goodbye",Topic.FAREWELL));
        String speak=switch(topic){
            case GREETING->prefix+(completed==0?
                "There is a great deal to learn before you take your first contract.":
                "I have read the report from "+last+". "+(relation>=3?
                    "You've earned the right to hear what I kept out of the public account.":
                    "Your next assignment will not wait forever."))+
                " Your standing here is "+relation+" of five.";
            case BACKGROUND->switch(resident){
                case MATTHIS_ROOK->"Beckenburg predates our guild. Its oldest accounts name debts no living family admits. Some hauntings are echoes; others make choices. Listen to both, but trust neither.";
                case ELSBETH_VALE->"Hunters built this armory after the first company lost its supplies in a single night. Every item in this room has a price because replacements require actual hands and materials.";
                case ROWAN_PIKE->"We keep field frequencies separate from castle traffic. A voice heard behind a locked wall is not proof of a living speaker. Check the receiver, the distance and the direction.";
                case TOMAS_GREAVES->"The roads out of Beckenburg lead to places people have abandoned for different reasons. Most were not deserted overnight. The order of those departures matters.";
                case MIRELLE_ASH->"There is no universal prayer that will mend every haunting. You must determine which bond can be cleansed and which presence must be driven away.";
            };
            case THE_LAST_CASE->previous==null?
                "No completed campaign report yet. Return after the first successful rite; even a retreat can leave questions behind.":
                "You cleared "+last+". "+switch(resident){
                    case MATTHIS_ROOK->"Its history is now cross-referenced in the archive. The next account begins where this one ends.";
                    case ELSBETH_VALE->"Your surviving equipment tells me as much as the final report. Examine what you actually used before buying replacements.";
                    case ROWAN_PIKE->"I compared the calls with the expedition log. Silence and interference are observations, not proof of a particular ghost.";
                    case TOMAS_GREAVES->"The route is safe for returning teams, but I would not assume the next road uses the same approach.";
                    case MIRELLE_ASH->"A completed rite quiets one place. It does not explain every symptom the team recorded.";
                };
            case NEXT_LOCATION->next==null?
                "The twenty-two chapters are complete. Revisit older cases for funds or consult the archive to compare their histories.":
                "Your next chapter is "+upcoming+" in "+region+". "+description+" "+
                switch(resident){
                    case MATTHIS_ROOK->"Read the site's history before deciding what kind of haunting you expect.";
                    case ELSBETH_VALE->"Pack redundancy. If your primary detector fails, an independent reading may still save the case.";
                    case ROWAN_PIKE->"Check sightlines, walls and radio reach. A signal through a wall is not necessarily a clear view.";
                    case TOMAS_GREAVES->"Know your way back to the van before you move far from it. Mark choke points and doors.";
                    case MIRELLE_ASH->"Gather observations before choosing a rite. Do not commit to an identity from one sign.";
                };
            case FIELD_ADVICE->switch(resident){
                case MATTHIS_ROOK->"Compare a field note with a second independent observation. Old folios are useful precisely because witnesses disagree.";
                case ELSBETH_VALE->"Bring a light source and a backup instrument. Your class level helps endurance, but it will not make a poor measurement true.";
                case ROWAN_PIKE->"A microphone hears around a corner where a camera cannot see. Range, direction and shut doors change the interpretation.";
                case TOMAS_GREAVES->"Never let the party lose track of the extraction route. More teammates bring resources and greater danger.";
                case MIRELLE_ASH->"Wards are for preparation, not a substitute for diagnosis. A wrong ritual wastes the time you should have used to observe.";
            };
            case PERSONAL->relation<2?
                "We have only just met. Finish a few cases and speak with me again; stories need trust as well as time.":
                switch(resident){
                    case MATTHIS_ROOK->"I once erased a name from an official record because I believed it protected the living. The missing name has followed me longer than any ghost.";
                    case ELSBETH_VALE->"I keep the damaged tools of hunters who never came home. Repairs do not make me forget whom they belonged to.";
                    case ROWAN_PIKE->"My first night at the receiver, I answered a call dated thirty years before I was born. I still cannot explain the timestamps.";
                    case TOMAS_GREAVES->"I know the roads so well because I once abandoned a team on one. I returned at dawn. Two of them never did.";
                    case MIRELLE_ASH->"I learned rites from someone who insisted that mercy and banishment are different choices. I wish I had listened earlier.";
                };
            case QUEST->switch(resident){
                case MATTHIS_ROOK->"ARCHIVE LEAD: Read the next location's account and preserve a journal of what you personally witness. It may contradict the version held here.";
                case ELSBETH_VALE->"SUPPLY LEAD: Audit your loadout before departure. A paid replacement and an instrument already in stock should not be confused.";
                case ROWAN_PIKE->"SIGNAL LEAD: Note any interference or camera obstruction at "+upcoming+". A failed transmission can be environmental.";
                case TOMAS_GREAVES->"ROUTE LEAD: Establish an exit path on your first visit to "+upcoming+". A retreat still yields a useful map.";
                case MIRELLE_ASH->"RITUAL LEAD: Compare at least two observations at "+upcoming+" before selecting a rite. A convincing apparition can be deceptive.";
            };
            case FOLLOW_UP->relation<3?
                "I have given you what the records support. Return with more field experience, and I may speak more plainly.":
                prefix+"Consider what we could not confirm at "+last+
                    ". If the next case disagrees with your expectation, document the disagreement rather than forcing the conclusion.";
            case FAREWELL->"Keep your notes. There are no second chances for a detail you never wrote down.";
        };
        List<Choice> choices=topic==Topic.GREETING?root:
            topic==Topic.FAREWELL?List.of():continueTalk;
        return new Page(resident.name+" / "+resident.profession+" / "+
            (completed==CampaignStory.LENGTH?"CAMPAIGN COMPLETE":"CHAPTER "+(completed+1)+"/22"),
            speak,choices);
    }
}
