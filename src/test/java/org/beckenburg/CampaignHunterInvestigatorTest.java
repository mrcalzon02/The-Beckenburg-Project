package org.beckenburg;

/** Controlled observation/decision cases. The live TCP scenario separately checks actual travel. */
public final class CampaignHunterInvestigatorTest {
    private static void require(boolean condition,String why){
        if(!condition)throw new AssertionError(why);
    }
    private static void reading(CampaignHunterInvestigator ai,int tick,int room,String tool,String words){
        ai.accept(new LanEvidenceJournal.Entry(tick,0,room,tool,LanEvidenceJournal.Finding.OBSERVED,words).frame());
    }
    private static LanWorld.Body body(HouseMap map,int room){
        var p=map.spawn(room);return new LanWorld.Body(p.x(),p.y(),p.z(),0,room);
    }
    private static void ritual(HouseMap map,GhostIdentity.Type type){
        int anchor=4;
        var ai=new CampaignHunterInvestigator(map,1);ai.assignSeat(0);
        var mission=new LanCase(anchor,map);mission.configure(type,true);
        var witness=new GhostHauntingFolio(1);
        ai.accept(witness.choose(GhostDomain.Action.SIGNATURE,type,anchor,10).frame());
        require(ai.hypothesis()==null,"A signature alone cannot replace fieldwork");
        ai.accept("EVIDENCELEVEL|MEDIUM|3");
        if(type==GhostIdentity.Type.ANCIENT_DEMON)ai.accept("FORM|FIGURE|GREEN|SPIRIT");
        require(ai.hypothesis()==null,"Reading a signature's tint cannot identify its species");
        reading(ai,11,1,"THERMOMETER","Air 19.0?C / 66.2?F");
        switch(type){
            case SPIRIT->{
                reading(ai,12,anchor,"SPIRIT_BOX",SpiritBoxResponses.observation(SpiritBoxResponses.Phrase.HERE));
                reading(ai,13,anchor,"THERMOMETER","Air -4.0?C / 24.8?F");
                reading(ai,14,anchor,"OBJECT_IMPRINT_SCANNER","Surface contact pattern 4/10 / imprint visible");
            }
            case MIMIC->{
                reading(ai,12,anchor,"EMF_READER","EMF 9/10");
                reading(ai,13,anchor,"THERMOMETER","Air 41.0?C / 105.8?F");
                reading(ai,14,anchor,"OBJECT_IMPRINT_SCANNER","Surface contact pattern 4/10 / imprint visible");
            }
            case ANCIENT_DEMON->{
                reading(ai,12,anchor,"RESONANCE_TUNER","Peak harmonic 128 Hz / 32 dB");
                reading(ai,13,anchor,"BAROMETRIC_PROBE","Differential pressure -8 hPa / localized pulse");
                reading(ai,14,anchor,"EMF_READER","EMF 9/10");
            }
            default->throw new AssertionError(type);
        }
        require(ai.hypothesis()==type,"Deduced "+type+" from public observations");
        boolean identified=false,ward=false;
        var site=RitualSites.site(map,type.rite());
        for(int tick=15;tick<215;tick++){
            if(tick==158)ai.accept(witness.choose(GhostDomain.Action.WHISPER,type,anchor,tick).frame());
            var in=ai.decide(tick,body(map,anchor),null);
            if(in.action()==CampaignGuestBot.Action.IDENTIFY){
                var report=mission.identify(0,in.identifiedType(),tick);require(report.accepted(),report.message());
                identified=true;ai.accept("IDENTIFIED|"+type+"|"+type.rite());
            }else if(in.action()==CampaignGuestBot.Action.WARD){
                require(tick>=158,"A single roaming encounter must not burn a ward");
                var report=mission.ward(0,anchor);require(report.accepted(),report.message());
                ward=mission.wardPrepared();ai.accept("ACCEPT|WARD|"+report.message());
            }
            if(identified&&ward)break;
        }
        require(identified&&ward,"Identify and ward before performing the rite");
        var feet=new LanWorld.Body(site.x(),site.y(),site.z(),0,site.room());
        boolean performing=false;
        for(int tick=215;tick<255;tick++){
            var in=ai.decide(tick,feet,null);
            if(in.action()==CampaignGuestBot.Action.RITE){
                var verdict=mission.performRite(0,in.rite(),feet.room(),site.near(feet));
                require(verdict.accepted(),verdict.message());performing=true;
                ai.accept("RITUAL|STARTED|"+in.rite());break;
            }
        }
        require(performing,"Correct rite at the installation for "+type);
        int[] defenders={site.room(),-1,-1,-1,-1,-1};
        for(int i=0;i<mission.requiredRitualTicks();i++)mission.advance(defenders);
        require(mission.outcome()!=LanCase.Outcome.ACTIVE,"Rite completes while circle is defended");
        System.out.println(type+" -> "+type.rite()+" -> "+mission.outcome());
    }
    private static void windup(HouseMap map){
        LanWorld.Body initial=body(map,0);
        LanWorld.Body hunter=initial;
        int room=hunter.room();
        var ai=new CampaignHunterInvestigator(map,1);ai.assignSeat(1);
        ai.decide(100,hunter,null);
        // Select a legal open side at the actual room spawn for this test map.
        double angle=0;
        while(angle<2*Math.PI){
            double gx=hunter.x()+1.8*Math.sin(angle),gz=hunter.z()+1.8*Math.cos(angle);
            if(map.traversable(gx,hunter.y(),gz)&&map.roomAt(gx,hunter.y(),gz)==room
                    &&map.resolveMove(new LevelDefinition.Vec(hunter.x(),hunter.y(),hunter.z()),
                    hunter.x()-.31*Math.sin(angle),hunter.z()-.31*Math.cos(angle))!=null)break;
            angle+=Math.PI/4;
        }
        require(angle<2*Math.PI,"Find a fair wind-up position");
        double gx=hunter.x()+1.8*Math.sin(angle),gz=hunter.z()+1.8*Math.cos(angle);
        String sight="APPARITION|VISIBLE|"+Math.round(gx*1000)+"|"+Math.round(hunter.y()*1000)+"|"+Math.round(gz*1000);
        ai.accept(sight+"|100");ai.accept("THREAT|START|8|1");
        for(int tick=101;tick<=108;tick++){
            var step=ai.decide(tick,hunter,null);
            if(step.action()!=CampaignGuestBot.Action.WALK)continue;
            double size=.31/Math.hypot(step.dx(),step.dz());
            var next=map.resolveMove(new LevelDefinition.Vec(hunter.x(),hunter.y(),hunter.z()),
                hunter.x()+step.dx()*size,hunter.z()+step.dz()*size);
            require(next!=null,"Evading hunter attempted an obstructed step");
            hunter=new LanWorld.Body(next.x(),next.y(),next.z(),0,map.roomAt(next.x(),next.y(),next.z()));
        }
        require(Math.hypot(hunter.x()-gx,hunter.z()-gz)>1.8,"Targeted hire increased distance from the perceived attacker");
        ai.accept("THREAT|EVADED|1");
        ai.accept("CHAT|RADIO|0|109|H2 hold");
        ai.decide(109,initial,null);
        ai.accept("COUNTERAMMO|1|2|3");ai.accept(sight+"|110");ai.accept("THREAT|START|8|0");
        var cover=ai.decide(110,initial,null);
        require(cover.action()==CampaignGuestBot.Action.CROSS,"Stationary hire protects an attacked teammate");
        require(Math.abs(Math.floorMod(cover.heading()-(int)Math.round(Math.toDegrees(angle))+180,360)-180)<2,
            "Cross aimed at visible apparition");
        ai.accept("THREAT|EVADED|0");ai.accept("COUNTERAMMO|0|2|3");ai.accept("THREAT|START|8|1");
        require(ai.decide(111,initial,null).action()==CampaignGuestBot.Action.WAIT,"Hold and finite stock respected");
    }
    private static void abandonedRoomLead(HouseMap map)throws ReflectiveOperationException{
        var ai=new CampaignHunterInvestigator(map,1);ai.assignSeat(0);
        var goal=CampaignHunterInvestigator.class.getDeclaredField("searchRoom");goal.setAccessible(true);
        ai.accept("PROGRESS|100|0|0|0|0|0|ACTIVE");
        reading(ai,10,4,"EMF_READER","EMF 8/10");reading(ai,120,4,"EMF_READER","EMF 8/10");
        var hall=body(map,4);var foyer=body(map,0);
        for(int tick=125;tick<=700;tick++)ai.decide(tick,hall,null);
        int next=goal.getInt(ai);
        require(next>=0&&next!=4,"The completed hall survey chose a new patrol room");
        ai.decide(701,foyer,null);
        require(goal.getInt(ai)==next,"Old readings reversed an unswept-room patrol without a fresh local observation");
        ai.accept(new LanEvidenceJournal.Entry(702,1,4,"EMF_READER",LanEvidenceJournal.Finding.OBSERVED,"EMF 8/10").frame());
        ai.decide(703,foyer,null);
        require(goal.getInt(ai)==4,"A new positive after the survey must recall the investigator");
        ai.accept("CHAT|RADIO|1|704|Defensive ward only in "+map.roomName(4)+".");
        require(ai.suspectedRoom()!=4&&goal.getInt(ai)!=4,"A disproved home room must retire its active pursuit");
        ai.accept("CHAT|RADIO|1|705|H1 search 4");
        var expected=new HunterFieldNavigation(map).to(720,foyer,4,map.centerX(4),map.centerZ(4),1);
        require(expected.action()==CampaignGuestBot.Action.WALK||expected.action()==CampaignGuestBot.Action.DOOR,
            "A route must exist for the directed-room control");
        boolean followed=false;
        for(int tick=706;tick<=725;tick++){
            var movement=ai.decide(tick,foyer,null);
            if(movement.action()==expected.action()&&movement.dx()==expected.dx()&&movement.dz()==expected.dz()){followed=true;break;}
        }
        require(followed,"A player can still order a search of a room excluded as the home");
    }
    private static void memorialLectern(int origin){
        var map=new HouseMap();var world=new LanWorld(map);
        if(origin!=HouseMap.EXTRACTION_ROOM)world.setRoom(0,origin);
        var mission=new LanCase(4,map);mission.configure(GhostIdentity.Type.SPIRIT,true);
        require(mission.ward(0,4).accepted()&&mission.identify(0,GhostIdentity.Type.SPIRIT,100).accepted(),
            "Set up the real ward and Spirit hypothesis");
        var investigator=new CampaignHunterInvestigator(map,1);investigator.assignSeat(0);
        investigator.accept("IDENTIFIED|SPIRIT|ABSOLUTION");
        investigator.accept("PROGRESS|100|0|1|0|1|0|ACTIVE");
        var site=RitualSites.site(map,GhostIdentity.Rite.ABSOLUTION);
        int doors=0;
        for(int tick=5;tick<=1500;tick+=5){
            var body=world.body(0);
            var action=investigator.decide(tick,body,null);
            switch(action.action()){
                case WALK -> {
                    if(action.heading()>=0)world.turn(0,action.heading());
                    world.move(0,action.dx(),action.dz(),tick);
                }
                case DOOR -> {if(world.door(0,tick))doors++;}
                case RITE -> {
                    require(doors>0&&site.near(body)&&action.rite()==GhostIdentity.Rite.ABSOLUTION,
                        "Approach the physical Memorial Lectern and select its correct rite");
                    require(mission.performRite(0,action.rite(),body.room(),site.near(body)).accepted(),
                        "The host must accept the rite at the actual arrival pose");
                    System.out.println("PASS "+map.roomName(origin)+" to Memorial Lectern: Absolution accepted at tick "+tick);
                    return;
                }
                default -> { }
            }
        }
        throw new AssertionError("No Absolution from "+map.roomName(origin)+"; stopped at "+world.body(0));
    }
    private static void directedCamera(HouseMap map)throws ReflectiveOperationException{
        var hire=new CampaignHunterInvestigator(map,2);
        hire.assignSeat(1);
        var gearField=CampaignHunterInvestigator.class.getDeclaredField("equipment");
        gearField.setAccessible(true);
        var equipment=(HunterDiagnosticEquipment)gearField.get(hire);
        equipment.belt[0]="VIDEO_CAMERA";
        hire.accept("GEAROWNED|VIDEO_CAMERA");
        String assigned=map.roomName(6);
        hire.accept("CHAT|RADIO|0|10|H2 search "+assigned);
        hire.accept("CHAT|RADIO|0|11|H2 test video camera");
        var feet=body(map,6);
        // Acknowledging the human's new order is legitimate, but the NEXT
        // action must use the owned camera in the exact directed room, not
        // abandon that assignment and wander until incidental haunt evidence.
        var acknowledge=hire.decide(20,feet,null);
        require(acknowledge.action()==CampaignGuestBot.Action.RADIO,
            "H2 must acknowledge direct camera request by radio");
        var use=hire.decide(21,feet,null);
        require(use.action()==CampaignGuestBot.Action.SAMPLE&&use.slot()==0,
            "H2 must deploy an already-owned video camera in the assigned room without a speculative ghost lead; got "+use);
        System.out.println("PASS directed room remains active through human-requested camera placement");
    }
    public static void main(String[] args)throws ReflectiveOperationException{
        var map=new HouseMap();directedCamera(map);windup(map);abandonedRoomLead(map);memorialLectern(HouseMap.EXTRACTION_ROOM);memorialLectern(4);
        var ai=new CampaignHunterInvestigator(map,1);ai.assignSeat(1);
        var home=body(map,0);
        reading(ai,1,2,"THERMOMETER","Air -5.5?C / 22.1?F");
        reading(ai,26,3,"THERMOMETER","Air -5.0?C / 23.0?F");
        require(ai.suspectedRoom()<0&&ai.hypothesis()==null,"Uniform freezing conditions are ambiguous");
        ai.accept("CHAT|RADIO|0|27|H3 hold");
        ai.accept("CHAT|RADIO|0|28|H2 hold");
        var ack=ai.decide(29,home,null);
        require(ack.action()==CampaignGuestBot.Action.RADIO&&ack.radio().equals("Holding position."),"Acknowledge own radio order");
        require(ai.decide(30,home,null).action()==CampaignGuestBot.Action.WAIT,"Honor hold order");
        ai.accept("CHAT|RADIO|2|31|H2 follow");
        require(ai.directorSeat()==2,"Follow the issuing teammate");
        ai.accept("CHAT|RADIO|0|40|team search");
        ai.accept("EVIDENCELEVEL|MEDIUM|3");
        reading(ai,70,1,"THERMOMETER","Air 19.0?C / 66.2?F");
        reading(ai,90,4,"THERMOMETER","Air -3.0?C / 26.6?F");
        reading(ai,120,4,"SPIRIT_BOX","Vocal fragment heard: HELP ME");
        require(ai.hypothesis()==null,"Two of three families leave competing suspects");
        reading(ai,150,4,"OBJECT_IMPRINT_SCANNER","Surface contact pattern 3/10 / imprint visible");
        require(ai.hypothesis()==GhostIdentity.Type.SPIRIT&&ai.suspectedRoom()==4,
            "Measured families plus room comparison identify the spirit without seeing its form");
        ritual(map,GhostIdentity.Type.SPIRIT);ritual(map,GhostIdentity.Type.ANCIENT_DEMON);
        ritual(map,GhostIdentity.Type.MIMIC);
        System.out.println("PASS hunter radio, weather ambiguity, public deductions, anchor ward and all three rites");
    }
}
