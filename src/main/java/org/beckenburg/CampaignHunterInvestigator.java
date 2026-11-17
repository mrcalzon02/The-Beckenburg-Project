package org.beckenburg;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Pattern;

import static org.beckenburg.CampaignGuestBot.Action;
import static org.beckenburg.CampaignGuestBot.Input;

/** Shared hired/developer hunter. Knows the floor plan and delivered hunter observations. */
final class CampaignHunterInvestigator {
    private enum Order { SEARCH, FOLLOW, HOLD, ROOM }
    private static final int SURVEY_TICKS=180;
    private static final Pattern EMF=Pattern.compile("EMF (\\d+)/10");
    private static final Pattern AIR=Pattern.compile("Air (-?\\d+(?:\\.\\d+)?)");
    private final HouseMap map;
    private final int hireNumber;
    private final HunterFieldNavigation navigation;
    private final int rooms=HouseMap.INDOOR_ROOMS;
    private final int[] deferredUntil=new int[rooms],lastSurvey=new int[rooms],lastSignal=new int[rooms],signals=new int[rooms],
        lastHaunt=new int[rooms],haunts=new int[rooms],samples=new int[rooms],
        ownSampleTick=new int[rooms],thermalTick=new int[rooms],
        episodes=new int[rooms],lastActivity=new int[rooms], thermalExtremes=new int[rooms],
        lastExtreme=new int[rooms],eventTick=new int[rooms];
    private final String[] eventInstrument=new String[rooms];
    private final double[] air=new double[rooms];
    private final boolean[] triedWard=new boolean[rooms];
    private final Map<String,int[]> latestReadings=new HashMap<>();
    private final HunterDiagnosticEquipment equipment=new HunterDiagnosticEquipment();
    private final String[] belt=equipment.belt;
    private List<GhostIdentity.Type> candidates=List.of(GhostIdentity.Type.values());
    private final EnumSet<HardEvidenceQuota.Family> detected=EnumSet.noneOf(HardEvidenceQuota.Family.class);
    private HardEvidenceQuota.Level evidenceLevel=HardEvidenceQuota.Level.EASY;
    private final FieldJournal teamNotes=new FieldJournal();
    private final ArrayDeque<String> radio=new ArrayDeque<>();
    private Order order=Order.SEARCH;
    private int seat=-1,leader,orderedRoom=-1,searchRoom=-1,lastRoom=-1,arrived=-1,
        lastDecision=-1,lastRadio=-1000,lastSample=-1000,nextSlot,
        observationCount,wardCharges=2,lastSweepReport=-1000,lastReportedRoom=-1,wardRoom=-1,confirmedRoom=-1,lastRite=-1000,lastGuess=-1000;
    private boolean wardPrepared,ritualActive,hasMeasured;
    private GhostIdentity.Type signature,identified,attempted,candidate;
    private boolean deductionDirty=true;
    private Boolean green;
    private int trackedGoal=-1,progressTick,lastHauntingRoom=-1,lastAnsweredEvent=-1;
    private double progressX,progressZ,apparitionX,apparitionZ;
    private int apparitionTick=-1000,apparitionRoom=-1,crossCharges,threatTarget=-1,impactTick,lastCross=-1000;
    private boolean thrownForThreat,atCameraRelay;
    CampaignHunterInvestigator(HouseMap map,int hireNumber){
        this.map=map;this.hireNumber=hireNumber;this.navigation=new HunterFieldNavigation(map);
        Arrays.fill(lastSurvey,-10000);Arrays.fill(lastSignal,-10000);Arrays.fill(lastHaunt,-10000);
        Arrays.fill(lastActivity,-10000);Arrays.fill(lastExtreme,-10000);Arrays.fill(eventTick,-10000);
        Arrays.fill(air,Double.NaN);Arrays.fill(ownSampleTick,-1);Arrays.fill(thermalTick,-10000);
    }
    synchronized void assignSeat(int seat){this.seat=seat;}
    synchronized int seat(){return seat;}
    synchronized int directorSeat(){return leader;}
    synchronized int observationCount(){return observationCount;}
    synchronized GhostIdentity.Type hypothesis(){return identified!=null?identified:deduction();}
    synchronized int suspectedRoom(){
        if(confirmedRoom>=0)return confirmedRoom;
        int best=-1,bestScore=0;
        for(int room=0;room<rooms;room++){
            if(triedWard[room]||samples[room]==0||(deferredUntil[room]>0&&lastDecision<deferredUntil[room]))continue;
            int thermal=relativeThermal(room)?4:0;
            int score=Math.min(4,signals[room])*2+Math.min(4,haunts[room])*2+thermal;
            if(score<4||score<=bestScore)continue;
            best=room;bestScore=score;
        }
        return best;
    }
    private void encounter(int room,int tick){
        if(tick-lastActivity[room]>=100)episodes[room]++;
        lastActivity[room]=Math.max(lastActivity[room],tick);
    }
    private boolean readyToWard(int room){
        return episodes[room]>=2&&(signals[room]>0||haunts[room]>=2)
            ||thermalExtremes[room]>=2&&(haunts[room]>0||signals[room]>=3);
    }
    private boolean relativeThermal(int room){
        double local=air[room];
        if(!(local<0||local>=32.23))return false;
        for(int other=0;other<rooms;other++)if(Math.abs(thermalTick[other]-thermalTick[room])<=350
                &&air[other]>6&&air[other]<29&&Math.abs(air[other]-local)>=8)return true;
        return false;
    }
    /** Only frames already delivered to this investigator enter its notebook. */
    synchronized void accept(String frame){
        equipment.frame(frame,seat);
        if(frame.startsWith("APPARITION|VISIBLE|")){
            var p=LanProtocol.parts(frame);
            apparitionX=Integer.parseInt(p.get(2))/1000d;double y=Integer.parseInt(p.get(3))/1000d;
            apparitionZ=Integer.parseInt(p.get(4))/1000d;apparitionTick=Integer.parseInt(p.get(5));
            apparitionRoom=map.roomAt(apparitionX,y,apparitionZ);
        }else if(frame.startsWith("THREAT|START|")){
            var p=LanProtocol.parts(frame);int target=Integer.parseInt(p.get(3));
            if(target!=threatTarget||lastDecision>=impactTick)thrownForThreat=false;
            threatTarget=target;impactTick=Math.max(lastDecision,apparitionTick)+Integer.parseInt(p.get(2))+1;
        }else if(frame.startsWith("THREAT|EVADED|")||frame.startsWith("THREAT|HIT|")){
            if(Integer.parseInt(LanProtocol.parts(frame).get(2))==threatTarget)threatTarget=-1;
        }else if(frame.startsWith("COUNTERAMMO|")){
            crossCharges=Integer.parseInt(LanProtocol.parts(frame).get(1));
        }
        if(frame.startsWith("CAMORB|")&&atCameraRelay&&equipment.watchingCamera(lastDecision)){
            var parts=LanProtocol.parts(frame);
            if(Integer.parseInt(parts.get(1))==seat&&detected.add(HardEvidenceQuota.Family.ORBS)){
                hasMeasured=true;deductionDirty=true;announce("Orb on my live van camera.");
            }
        }
        if(frame.startsWith("DOTS|ILLUMINATE|")||frame.startsWith("LASER|HIT|")){
            var parts=LanProtocol.parts(frame);boolean dots=parts.get(0).equals("DOTS");
            int room=Integer.parseInt(parts.get(dots?2:3)),tick=Integer.parseInt(parts.get(dots?3:7));
            HardEvidenceQuota.Family family=dots?HardEvidenceQuota.Family.DOTS:HardEvidenceQuota.Family.LASER;
            if(room<rooms&&detected.add(family)){
                hasMeasured=true;deductionDirty=true;encounter(room,tick);signals[room]++;
                announce(map.roomName(room)+(dots?": D.O.T.S. passage.":": laser grid disrupted."));
            }
        }
        if(frame.startsWith("CASEFILE|")&&!frame.equals("CASEFILE|RESET")){
            measure(LanEvidenceJournal.Entry.decode(frame));
        }else if(frame.startsWith("HAUNT|")){
            var occurrence=GhostHauntingFolio.decode(frame);int room=occurrence.room();
            if(room<rooms){
                int observed=occurrence.startTick();
                if(lastHauntingRoom>=0&&lastHauntingRoom!=room&&observed-lastActivity[room]<100)episodes[room]++;
                encounter(room,observed);lastHauntingRoom=room;eventTick[room]=observed;
                eventInstrument[room]=switch(occurrence.entry().action()){
                    case WHISPER,SIGNATURE -> "SPIRIT_BOX";
                    case MANIFEST,STRIKE,DISTURB -> "EMF_READER";
                    default -> null;
                };
            }
            if(room<rooms&&occurrence.startTick()-lastHaunt[room]>=55){
                lastHaunt[room]=occurrence.startTick();haunts[room]++;
            }
            if(occurrence.entry().signatureType()!=null&&signature!=occurrence.entry().signatureType()){
                deductionDirty=true;
                signature=occurrence.entry().signatureType();
                announce("Witnessed "+occurrence.entry().title()+" in "+map.roomName(room)+".");
            }
        }else if(frame.startsWith("EVIDENCELEVEL|")){
            deductionDirty=true;
            evidenceLevel=HardEvidenceQuota.Level.valueOf(LanProtocol.parts(frame).get(1));
        }else if(frame.startsWith("FORM|FIGURE|")){
            // The visible signature sprite's tint distinguishes Ancient kin.
            // Never use the sprite asset's species identifier as an answer.
            boolean ancientTint=frame.startsWith("FORM|FIGURE|GREEN|");
            if(green==null||green!=ancientTint){green=ancientTint;deductionDirty=true;}
        }else if(frame.startsWith("NOTE|")){
            teamNotes.accept(FieldJournal.Note.decode(frame));deductionDirty=true;
        }else if(frame.startsWith("IDENTIFIED|")){
            identified=GhostIdentity.Type.valueOf(LanProtocol.parts(frame).get(1));
        }else if(frame.startsWith("PROGRESS|")){
            var p=LanProtocol.parts(frame);wardPrepared=p.get(5).equals("1");wardCharges=Integer.parseInt(p.get(3));
        }else if(frame.startsWith("RITUAL|DISPROVED")){
            identified=null;ritualActive=false;deductionDirty=true;
            announce("The rite failed. Rechecking our evidence.");
        }else if(frame.startsWith("RITUAL|")){
            ritualActive=frame.startsWith("RITUAL|STARTED")||frame.startsWith("RITUAL|RESUMED");
        }else if(frame.startsWith("ACCEPT|WARD|")&&wardRoom>=0){
            wardPrepared=frame.contains("ANCHOR WARD PREPARED");
            if(wardPrepared)confirmedRoom=wardRoom;
            String report=wardPrepared?"Anchor ward set in ":"Defensive ward only in ";
            announce(report+map.roomName(wardRoom)+".");
            wardRoom=-1;
        }else if(frame.startsWith("REJECT|WARD|")){
            wardRoom=-1;
        }else if(frame.startsWith("CHAT|RADIO|")){
            var line=LanChat.Entry.decode(frame);
            if(line.seat()!=seat)equipment.claimed(line,seat);order(line);
        }
    }
    private void measure(LanEvidenceJournal.Entry e){
        int room=e.room();if(room>=rooms)return;
        // VIEW3D and the casefile UI can replay historic observations.
        int[] latest=latestReadings.computeIfAbsent(e.tool(),k->new int[6]);
        if(e.tick()+1<=latest[e.seat()])return;
        latest[e.seat()]=e.tick()+1;equipment.observed(e,seat);
        String value=e.reading();observationCount++;samples[room]++;
        if(e.seat()==seat)ownSampleTick[room]=e.tick();
        var emf=EMF.matcher(value);var temp=AIR.matcher(value);
        GhostFieldGuide.Clue clue=null;
        HardEvidenceQuota.Family family=null;
        if((e.tool().equals("EMF")||e.tool().equals("EMF_READER"))&&emf.find()){
            int level=Integer.parseInt(emf.group(1));hasMeasured=true;
            if(level>=8)clue=GhostFieldGuide.Clue.EMF_OVER_SIX;
            else if(level==0)clue=GhostFieldGuide.Clue.EMF_SUPPRESSION;
            if(clue!=null)family=HardEvidenceQuota.Family.EMF;
        }else if(e.tool().equals("THERMOMETER")&&temp.find()){
            hasMeasured=true;air[room]=Double.parseDouble(temp.group(1));thermalTick[room]=e.tick();
            if(relativeThermal(room)){
                clue=air[room]<0?GhostFieldGuide.Clue.EXTREME_COLD:GhostFieldGuide.Clue.EXTREME_HEAT;
                family=HardEvidenceQuota.Family.TEMPERATURE;
                if(e.tick()-lastExtreme[room]>=80){lastExtreme[room]=e.tick();thermalExtremes[room]++;}
            }
        }else if(e.tool().equals("SPIRIT_BOX")){
            hasMeasured=true;
            if(value.contains("Vocal fragment heard")){clue=GhostFieldGuide.Clue.SPIRIT_REPLY;family=HardEvidenceQuota.Family.SPIRIT_BOX;}
        }else if((e.tool().equals("UV_LIGHT")||e.tool().equals("UV_LAMP"))&&
                (value.contains("genuine surface trace")||value.contains("fresh surface trace"))){
            hasMeasured=true;clue=GhostFieldGuide.Clue.UV_RESIDUE;family=HardEvidenceQuota.Family.UV;
        }else if(e.tool().equals("GHOST_WRITING_BOOK")){
            if(value.contains("Childlike figures"))clue=GhostFieldGuide.Clue.WRITING_CHILD;
            if(value.contains("crossed-out words"))clue=GhostFieldGuide.Clue.WRITING_SCRAWL;
            if(value.contains("ritual geometry"))clue=GhostFieldGuide.Clue.WRITING_RITUAL;
            if(clue!=null)family=HardEvidenceQuota.Family.WRITING;
        }else if(AdditionalEvidence.phenomenon(e.tool())!=null&&
                (value.contains("glassy film")||value.contains("Peak harmonic")||value.contains("Floor impulse")
                ||value.contains("localized pulse")||value.contains("Silhouette area")
                ||value.contains("imprint visible")||value.contains("offset observed"))){
            family=AdditionalEvidence.family(AdditionalEvidence.phenomenon(e.tool()));
        }
        if(clue!=null&&teamNotes.status(clue)!=FieldJournal.Status.SEEN)deductionDirty=true;
        if(clue!=null)teamNotes.set(clue,FieldJournal.Status.SEEN,e.seat(),e.tick());
        if(family!=null){hasMeasured=true;if(detected.add(family))deductionDirty=true;encounter(room,e.tick());}
        if(family!=null&&e.tick()-lastSignal[room]>=25){
            lastSignal[room]=e.tick();signals[room]++;
            if(e.seat()==seat){
                int prefix=value.indexOf(" - ");
                announce(map.roomName(room)+": "+(prefix<0?value:value.substring(prefix+3)));
            }
        }
    }
    private GhostIdentity.Type deduction(){
        if(deductionDirty){
            deductionDirty=false;
            candidates=InvestigationDiagnostics.candidates(teamNotes,detected,evidenceLevel,green);
            candidate=InvestigationDiagnostics.sufficientToIdentify(teamNotes,detected,evidenceLevel,green);
        }
        return hasMeasured?candidate:null;
    }
    private void order(LanChat.Entry line){
        if(line.seat()==seat)return;
        for(int room=0;room<rooms;room++){
            if(line.message().equals("Defensive ward only in "+map.roomName(room)+"."))triedWard[room]=true;
            if(wardPrepared&&line.message().equals("Anchor ward set in "+map.roomName(room)+"."))confirmedRoom=room;
        }
        String words=line.message().strip().toLowerCase(Locale.ROOT);
        String prefix=words.startsWith("team ")?"team ":"h"+(seat+1)+" ";
        if(!words.startsWith(prefix))return;
        String request=words.substring(prefix.length());
        if(request.startsWith("test ")){
            leader=line.seat();order=Order.SEARCH;radio.clear();
            announce(equipment.orderTest(request.substring(5),line.tick()));return;
        }
        if(request.equals("follow"))order=Order.FOLLOW;
        else if(request.equals("hold"))order=Order.HOLD;
        else if(request.equals("search")){order=Order.SEARCH;searchRoom=-1;}
        else if(request.startsWith("search ")){
            String location=request.substring(7).strip();
            int found=-1;
            for(int i=0;i<rooms;i++)if(location.equals(Integer.toString(i))
                    ||location.equals(map.roomName(i).toLowerCase(Locale.ROOT))){found=i;break;}
            if(found<0){announce("Unknown search room: "+location);return;}
            orderedRoom=found;order=Order.ROOM;
        }else return;
        leader=line.seat();navigation.reset();radio.clear();
        announce(switch(order){
            case FOLLOW -> "Following H"+(leader+1)+".";
            case HOLD -> "Holding position.";
            case ROOM -> "Searching "+map.roomName(orderedRoom)+".";
            case SEARCH -> "Resuming room sweep.";
        });
    }
    private void announce(String text){
        String clean=text.replace("°"," deg ").replaceAll("[^ -~]","").replace('|','/').strip();
        if(clean.isEmpty())return;
        clean=clean.substring(0,Math.min(LanChat.MAX_CHARS,clean.length()));
        if(radio.size()<4&&!radio.contains(clean))radio.addLast(clean);
    }
    synchronized Input decide(int tick,LanWorld.Body self,LanWorld.Body orderLeader){
        Input action=chooseAction(tick,self,orderLeader);
        return action.action()==Action.WALK?action.facingFrom(self.heading()):action;
    }
    private Input defend(int tick,LanWorld.Body self){
        if(threatTarget<0||tick>impactTick)return null;
        boolean targeted=threatTarget==seat;
        boolean seen=apparitionRoom==self.room()&&tick-apparitionTick<=15;
        double dx=apparitionX-self.x(),dz=apparitionZ-self.z();
        double distance=Math.hypot(dx,dz);
        double bearing=seen?Math.atan2(dx,dz):Math.toRadians(self.heading());
        // Hold and ritual guard orders keep their position. A nearby observer can
        // also throw an issued cross to interrupt a strike on a human teammate.
        boolean guarding=order==Order.HOLD||ritualActive;
        boolean canThrow=seen&&distance<=3.4&&crossCharges>0&&!thrownForThreat&&tick-lastCross>=6;
        if(canThrow&&(!targeted||guarding||distance<1.35)){
            thrownForThreat=true;lastCross=tick;
            return Input.cross(Math.floorMod((int)Math.round(Math.toDegrees(bearing)),360));
        }
        if(targeted&&!guarding){
            var escape=navigation.evade(self,bearing+Math.PI);
            if(escape!=null)return escape;
            if(canThrow){
                thrownForThreat=true;lastCross=tick;
                return Input.cross(Math.floorMod((int)Math.round(Math.toDegrees(bearing)),360));
            }
        }
        return null;
    }
    private Input chooseAction(int tick,LanWorld.Body self,LanWorld.Body orderLeader){
        if(self==null||self.room()<0||lastDecision==tick)return Input.simple(Action.WAIT);
        lastDecision=tick;
        int here=self.room();
        atCameraRelay=here==HouseMap.EXTRACTION_ROOM&&map.van().stationAt(self.x(),self.z())==VanStaging.Station.RELAY;
        if(here!=lastRoom){lastRoom=here;arrived=tick;}
        Input defense=defend(tick,self);
        if(defense!=null)return defense;
        if(!radio.isEmpty()&&tick-lastRadio>=12){lastRadio=tick;return Input.radio(radio.removeFirst());}
        int source=suspectedRoom();
        if(source>=0&&source!=lastReportedRoom){
            lastReportedRoom=source;announce("Possible home room: "+map.roomName(source)+". Checking the readings.");
        }
        if(here<rooms&&!ritualActive&&tick-lastSample>=9&&eventInstrument[here]!=null
                &&eventTick[here]>lastAnsweredEvent&&tick-eventTick[here]<=15){
            lastAnsweredEvent=eventTick[here];
            for(int i=0;i<belt.length;i++)if(eventInstrument[here].equals(belt[i])){
                lastSample=tick;return Input.sample(i);
            }
        }
        if(here<rooms&&tick-lastSample>=48+hireNumber%7&&!ritualActive){
            String focus=equipment.nextTool();
            if(source==here&&focus!=null&&equipment.slot(focus)>=0&&equipment.sampleable(focus,true)&&nextSlot%2==0){
                lastSample=tick;nextSlot++;return Input.sample(equipment.slot(focus));
            }
            for(int i=0;i<belt.length;i++){
                int index=Math.floorMod(nextSlot++,belt.length);String tool=belt[index];
                if(equipment.sampleable(tool,source==here)){
                    lastSample=tick;return Input.sample(index);
                }
            }
        }
        if(order==Order.HOLD)return Input.simple(Action.WAIT);
        if(order==Order.FOLLOW)
            return orderLeader==null?Input.simple(Action.WAIT):navigation.to(tick,self,orderLeader.room(),orderLeader.x(),orderLeader.y(),orderLeader.z(),1.45);
        if(order==Order.ROOM){
            if(here!=orderedRoom||tick-arrived<SURVEY_TICKS||ownSampleTick[here]<arrived)
                return navigation.to(tick,self,orderedRoom,map.centerX(orderedRoom),map.centerZ(orderedRoom),1.0);
            announce("Sweep complete in "+map.roomName(orderedRoom)+". Resuming search.");
            order=Order.SEARCH;lastSurvey[here]=tick;searchRoom=-1;
        }
        GhostIdentity.Type candidate=deduction();
        if(identified==null&&candidate!=null&&candidate!=attempted&&tick-lastGuess>=35){
            attempted=candidate;lastGuess=tick;
            announce("Evidence points to "+candidate.name().replace('_',' ')+".");
            return Input.identify(candidate);
        }
        if(identified==null&&candidate==null){
            String request=equipment.request(tick,seat,candidates,detected,evidenceLevel,
                source>=0||!detected.isEmpty()||signature!=null||observationCount>=15);
            if(request!=null)announce(request);
            Input fetch=equipment.fetch(tick,self,map,navigation);
            if(fetch!=null)return fetch;
        }
        if(wardCharges>0&&!wardPrepared&&source>=0&&here==source&&wardRoom<0&&readyToWard(source)){
            triedWard[source]=true;wardRoom=source;return Input.simple(Action.WARD);
        }
        if(identified==null&&wardPrepared&&confirmedRoom>=0){
            return navigation.to(tick,self,confirmedRoom,map.centerX(confirmedRoom),map.centerZ(confirmedRoom),1.0);
        }
        if(identified!=null&&wardPrepared){
            var site=RitualSites.site(map,identified.rite());
            if(site.near(self)){
                if(!ritualActive&&tick-lastRite>=50){
                    lastRite=tick;announce("At "+site.name()+". Guard the circle.");
                    return Input.rite(identified.rite());
                }
                return Input.simple(Action.WAIT);
            }
            return navigation.to(tick,self,site.room(),site.x(),site.y(),site.z(),1.85);
        }
        if(source>=0&&!wardPrepared&&(readyToWard(source)||tick-lastSurvey[source]>=280))
            searchRoom=source;
        if(searchRoom==here&&tick-arrived>=SURVEY_TICKS){
            if(tick-lastSweepReport>=250&&ownSampleTick[here]>=arrived){
                lastSweepReport=tick;announce("Finished sampling "+map.roomName(here)+".");
            }
            lastSurvey[here]=tick;searchRoom=-1;
        }
        if(searchRoom>=0&&searchRoom!=here&&trackedGoal==searchRoom&&tick-progressTick>=95){
            // Authored colliders or another closed route can make a room inaccessible.
            // Keep searching elsewhere and allow a later approach.
            deferredUntil[searchRoom]=tick+400;searchRoom=-1;navigation.reset();
        }
        if(searchRoom<0)searchRoom=chooseRoom(here,tick);
        if(trackedGoal!=searchRoom||Math.hypot(self.x()-progressX,self.z()-progressZ)>=.9){
            progressTick=tick;progressX=self.x();progressZ=self.z();trackedGoal=searchRoom;
        }
        return navigation.to(tick,self,searchRoom,map.centerX(searchRoom),map.centerZ(searchRoom),1.15);
    }
    private int chooseRoom(int here,int tick){
        int chosen=0,best=Integer.MAX_VALUE;
        int[] distance=navigation.distances(here);
        for(int room=0;room<rooms;room++){
            if(distance[room]<0||map.outside(room))continue;
            int rank=(tick<deferredUntil[room]?20000:0)+lastSurvey[room]+distance[room]*16+Math.floorMod(room*7+hireNumber*11,23);
            if(rank<best){best=rank;chosen=room;}
        }
        return chosen;
    }
}
