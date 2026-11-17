package org.beckenburg;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Investigator-side belt and rack knowledge. Reads the same private inventory as its human seat. */
final class HunterDiagnosticEquipment {
    static final List<String> FIELD_TOOLS=List.of("UV_LIGHT","GHOST_WRITING_BOOK","DOTS_PROJECTOR","VIDEO_CAMERA",
        "ECTOPLASM_SAMPLER","OBJECT_IMPRINT_SCANNER","BAROMETRIC_PROBE","RESONANCE_TUNER","SPECTRAL_PRISM",
        "SEISMIC_RECORDER","LASER_GRID","SPECTRAL_FREQUENCY_ANALYZER","EMF_READER","THERMOMETER","SPIRIT_BOX");
    private final Set<String> owned=new HashSet<>();
    final String[] belt=new String[FieldEquipment.SLOTS];
    private final Map<String,Integer> tested=new HashMap<>(),claimed=new HashMap<>();
    private final Set<String> deployed=new HashSet<>();
    private String needed,requested,claim;
    private int nextReview,lastSwap=-1000,lastServiced=-1000,cameraWatchUntil=-1,fetchSince=-1;
    private boolean service;
    private int activeSlot;
    String held(){return belt[activeSlot];}
    private int lastUnresolvedCount=-1;
    boolean ownsCamera(){return owned.contains("VIDEO_CAMERA")||slot("VIDEO_CAMERA")>=0;}
    boolean needsService(){return service;}
    void requestCamera(int tick){if(!"VIDEO_CAMERA".equals(needed))orderTest("video camera",tick);}
    void endCameraWatch(){cameraWatchUntil=-1;}
    int slot(String tool){return Arrays.asList(belt).indexOf(tool);}
    void frame(String frame,int seat){
        if(frame.startsWith("GEAROWNED|")){
            var p=LanProtocol.parts(frame);
            for(int i=1;i<p.size();i++)owned.add(p.get(i).split(":",2)[0]);
        }else if(frame.startsWith("GEARSTATE|")){
            var p=LanProtocol.parts(frame);
            activeSlot=Integer.parseInt(p.get(2));
            for(int i=0;i<belt.length;i++)belt[i]=p.get(3+i).split(":",2)[0];
            if(needed!=null&&slot(needed)>=0){needed=null;fetchSince=-1;}
        }else if(frame.startsWith("REJECT|collection kit empty")||frame.startsWith("REJECT|battery empty")||frame.startsWith("REJECT|camera battery empty")){
            service=true;
        }else if(frame.startsWith("ACCEPT|GEARREFILL"))service=false;
        else if(frame.startsWith("DOTS|PLACE|"+seat+"|"))deployed.add("DOTS_PROJECTOR");
        else if(frame.equals("DOTS|CLEAR|"+seat))deployed.remove("DOTS_PROJECTOR");
        else if(frame.equals("CAMERA|RESET"))deployed.remove("VIDEO_CAMERA");
        else if(frame.startsWith("CAMERA|PLACE|"+seat+"|"))deployed.add("VIDEO_CAMERA");
        else if(frame.equals("CAMERA|CLEAR|"+seat))deployed.remove("VIDEO_CAMERA");
        else if(frame.startsWith("SENSOR|PLACE|")){
            var p=LanProtocol.parts(frame);
            if(Integer.parseInt(p.get(3))==seat&&p.get(4).equals("LASER_GRID"))deployed.add("LASER_GRID");
        }
    }
    void observed(LanEvidenceJournal.Entry sample,int seat){
        if(sample.seat()==seat&&sample.reading().contains("stations active")&&sample.tool().equals("LASER_GRID"))
            deployed.add("LASER_GRID");
        if(sample.seat()==seat&&sample.tool().equals("VIDEO_CAMERA")&&sample.reading().contains("live feed"))
            {deployed.add("VIDEO_CAMERA");cameraWatchUntil=sample.tick()+650;}
        tested.put(sample.tool(),sample.tick());
    }
    void claimed(LanChat.Entry line,int ownSeat){
        for(String tool:FIELD_TOOLS)if(line.message().equals("I'll test "+name(tool)+".")){
            claimed.put(tool,line.tick());
            if(tool.equals(claim)&&line.seat()<ownSeat){
                claim=null;if(tool.equals(needed))needed=null;
            }
        }
    }
    boolean sampleable(String tool,boolean atSource){
        if(tool==null||!FIELD_TOOLS.contains(tool)||deployed.contains(tool))return false;
        return switch(tool){
            case "DOTS_PROJECTOR","VIDEO_CAMERA","LASER_GRID","GHOST_WRITING_BOOK",
                "SEISMIC_RECORDER","ECTOPLASM_SAMPLER","OBJECT_IMPRINT_SCANNER" -> atSource;
            default -> true;
        };
    }
    boolean watchingCamera(int tick){return tick<cameraWatchUntil&&deployed.contains("VIDEO_CAMERA");}
    String orderTest(String words,int tick){
        String id=words.strip().toUpperCase(java.util.Locale.ROOT).replace(' ','_');
        if(!FIELD_TOOLS.contains(id))return "Unrecognized diagnostic tool.";
        if(!owned.contains(id))return "I don't have "+name(id)+". Can someone take that reading?";
        requested=null;claim=id;nextReview=tick+650;
        needed=slot(id)<0?id:null;fetchSince=-1;
        return "I'll test "+name(id)+".";
    }
    String nextTool(){return claim;}
    String request(int tick,int seat,List<GhostIdentity.Type> candidates,Set<HardEvidenceQuota.Family> positives,
                   HardEvidenceQuota.Level level,boolean lead){
        if(!lead||tick<nextReview||needed!=null||candidates.isEmpty())return null;
        nextReview=tick+165+seat*7;
        // A clean reading remains a lead to revisit. Give another instrument a turn.
        List<String> available=FIELD_TOOLS.stream().filter(owned::contains)
            .filter(t->tick-claimed.getOrDefault(t,-10000)>650)
            .filter(t->tick-tested.getOrDefault(t,-10000)>160).toList();
        String useful=InvestigationDiagnostics.recommend(candidates,positives,level,available);
        if(useful!=null){
            if(slot(useful)<0){needed=useful;claim=useful;fetchSince=-1;return "I'll test "+name(useful)+".";}
            if(!useful.equals(claim)){claim=useful;return "I'll test "+name(useful)+".";}
            return null;
        }
        String missing=InvestigationDiagnostics.recommend(candidates,positives,level,FIELD_TOOLS.stream()
            .filter(t->!owned.contains(t)&&tick-claimed.getOrDefault(t,-10000)>650).toList());
        if(missing!=null&&!missing.equals(requested)){
            requested=missing;return candidates.size()+" candidates. Can someone test "+name(missing)+"?";
        }
        if(missing==null&&level==HardEvidenceQuota.Level.DEATH_WISH&&!positives.isEmpty()
                &&candidates.size()>1&&lastUnresolvedCount!=candidates.size()){
            lastUnresolvedCount=candidates.size();
            return candidates.size()+" candidates share our hard evidence. Check behavioral observations.";
        }
        return null;
    }
    private static String name(String tool){return tool.toLowerCase(java.util.Locale.ROOT).replace('_',' ');}
    CampaignGuestBot.Input fetch(int tick,LanWorld.Body self,HouseMap map,HunterFieldNavigation navigation){
        if(needed==null&&!service&&!watchingCamera(tick))return null;
        if(fetchSince<0)fetchSince=tick;
        if(needed!=null&&tick-fetchSince>650){needed=null;claim=null;fetchSince=-1;return null;}
        var van=map.van();
        if(needed==null&&!service&&watchingCamera(tick)){
            var monitor=van.location(VanStaging.Station.RELAY);
            return navigation.to(tick,self,HouseMap.EXTRACTION_ROOM,monitor.x(),van.floorY(),monitor.z(),.3);
        }
        if(self.room()!=HouseMap.EXTRACTION_ROOM||van.stationAt(self.x(),self.z())!=VanStaging.Station.EQUIPMENT){
            var rack=van.location(VanStaging.Station.EQUIPMENT);
            return navigation.to(tick,self,HouseMap.EXTRACTION_ROOM,rack.x(),van.floorY(),rack.z(),.11);
        }
        if(service&&tick-lastServiced>=40){lastServiced=tick;return CampaignGuestBot.Input.simple(CampaignGuestBot.Action.SERVICE);}
        if(needed==null||tick-lastSwap<40)return CampaignGuestBot.Input.simple(CampaignGuestBot.Action.WAIT);
        // Stabilize the survey kit and rotate the specialist instrument in its last slot.
        lastSwap=tick;
        return CampaignGuestBot.Input.equip(belt.length-1,needed);
    }
}
