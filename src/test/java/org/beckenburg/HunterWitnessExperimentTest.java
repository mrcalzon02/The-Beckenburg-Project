package org.beckenburg;

import java.util.ArrayList;
import java.util.List;

import static org.beckenburg.CampaignGuestBot.Action;
import static org.beckenburg.GhostFieldGuide.FigureView;

public final class HunterWitnessExperimentTest {
    private static void check(boolean condition,String why){if(!condition)throw new AssertionError(why);}
    private static LanWorld.Body body(HouseMap map,int room,int heading){
        var p=map.spawn(room);
        return new LanWorld.Body(p.x(),p.y(),p.z(),heading,room);
    }
    private static LanWorld.Body monitor(HouseMap map){
        var van=map.van();var p=van.location(VanStaging.Station.RELAY);
        var body=new LanWorld.Body(p.x(),van.floorY(),p.z(),0,HouseMap.EXTRACTION_ROOM);
        return new LanWorld.Body(body.x(),body.y(),body.z(),new HunterCameraAttention(map).screenHeading(body),body.room());
    }
    private static HunterWitnessExperiment plan(HouseMap map,HunterDiagnosticEquipment equipment){
        return new HunterWitnessExperiment(map,equipment,new HunterFieldNavigation(map),new HunterCameraAttention(map));
    }
    private static void setupAndRetest(HouseMap map){
        var reports=new ArrayList<String>();
        var equipment=new HunterDiagnosticEquipment();
        equipment.frame("GEAROWNED|VIDEO_CAMERA:I",1);
        var test=plan(map,equipment);
        var here=body(map,0,0);
        var van=monitor(map);
        test.frame("CAMERA|RESET");
        test.frame("TEAMMAP|14|0|-1|-1|-1|-1");
        check(test.command("empty 0",10,1,reports::add),"numeric room order");
        test.act(11,1,here,reports::add);
        test.act(41,1,here,reports::add);
        check(!test.observing(FigureView.VIDEO,0,0),"Unplaced van camera");
        var rack=map.van().location(VanStaging.Station.EQUIPMENT);
        var swap=test.act(50,1,new LanWorld.Body(rack.x(),map.van().floorY(),rack.z(),0,HouseMap.EXTRACTION_ROOM),reports::add);
        check(swap.action()==Action.EQUIP&&swap.equipment().equals("VIDEO_CAMERA"),"Fetch the owned instrument at the van rack");
        equipment.frame("GEARSTATE|0|0|EMF_READER:I|NONE:NONE|NONE:NONE|VIDEO_CAMERA:I",1);
        check(test.act(61,1,here,reports::add).action()==Action.SAMPLE,"Held video camera physically placed");
        test.frame(new LanVideoCameras.Camera(1,0,here.x(),here.y(),here.z(),0,EquipmentTiers.Tier.I).frame());
        test.frame("CAMERA|POWER|1|1");
        test.act(65,1,here,reports::add);
        check(!test.watching(1,van),"Installer still in target room cannot read the van monitor");
        check(test.act(67,1,van,reports::add).action()==Action.WATCH,"Operator must select the screen feed");
        check(!test.watching(1,van),"Request without active feed is insufficient");
        test.frame("ACCEPT|WATCH|CAMERA|1");
        test.act(68,1,van,reports::add);
        check(test.watching(1,van)&&!test.watching(2,van)&&test.observing(FigureView.VIDEO,0,0),"One live feed on an empty room");
        test.frame("TEAMMAP|0|14|-1|-1|-1|-1");
        test.act(300,1,van,reports::add);
        check(!test.observing(FigureView.VIDEO,0,0),"A human entering interrupts the empty trial");
        test.frame("HUNTER|DOWN|0");
        test.act(301,1,van,reports::add);
        test.act(560,1,van,reports::add);
        check(test.active(),"Headcount interruption restarts the entire observation interval");
        test.act(561,1,van,reports::add);
        check(!test.active()&&reports.stream().anyMatch(s->s.contains("Inconclusive")),"Silence remains inconclusive");
        check(test.command("solo 0",570,1,reports::add),"lone direct test can run");
        test.frame("CAMERA|RESET");
        test.act(601,1,here,reports::add);
        check(test.observing(FigureView.DIRECT,0,1)&&!test.watching(1,van),"One living hire can directly watch alone");
        test.stop(620);
        test.frame("TEAMMAP|14|0|-1|-1|-1|-1");
        var candidates=List.of(GhostIdentity.Type.SHADE,GhostIdentity.Type.BANSHEE);
        check(test.propose(3100,1,0,candidates)!=null,"Can propose a discriminating test from public candidates");
        check(test.propose(3300,1,0,List.of(GhostIdentity.Type.DEMON,GhostIdentity.Type.ANCIENT_DEMON))==null,
            "No proposed test when occupancy cannot distinguish the remaining profiles");
    }
    private static void groupStaffing(HouseMap map){
        var reports=new ArrayList<String>();
        var gear=new HunterDiagnosticEquipment();
        gear.frame("GEARSTATE|0|2|EMF_READER:I|NONE:NONE|VIDEO_CAMERA:I|NONE:NONE",1);
        var test=plan(map,gear);
        var here=body(map,0,0);var van=monitor(map);
        test.frame("TEAMMAP|14|14|0|-1|-1|-1");
        test.command("group 0",10,1,reports::add);
        test.radio(new LanChat.Entry(LanChat.Channel.RADIO,2,12,"Joining watch group 0 at 10."),1,true,reports::add);
        test.act(41,1,van,reports::add);
        check(!test.observing(FigureView.DIRECT,0,2),"An intended group on paper does not count");
        test.act(50,1,here,reports::add);
        check(test.observing(FigureView.DIRECT,0,2),"Two volunteers can witness a group apparition without a third operator");
        test.frame("TEAMMAP|0|0|0|-1|-1|-1");
        test.frame(new LanVideoCameras.Camera(2,0,here.x(),here.y(),here.z(),0,EquipmentTiers.Tier.I).frame());
        test.frame("CAMERA|POWER|2|1");
        test.act(51,1,here,reports::add);
        check(!test.watching(2,van),"When the human joins, the designated camera operator still has to walk to the van");
        test.act(64,1,van,reports::add);
        test.frame("ACCEPT|WATCH|CAMERA|2");
        test.act(65,1,van,reports::add);
        check(test.watching(2,van)&&test.observing(FigureView.VIDEO,0,2),"A human and a field hire physically sustain the camera test");
        test.frame("CAMERA|POWER|2|0");
        check(!test.watching(2,van),"Power loss cuts off that live feed immediately");
        test.stop(67);
        test.radio(new LanChat.Entry(LanChat.Channel.RADIO,2,1600,"Suggest watch empty 0"),1,false,reports::add);
        check(!test.active(),"Automatic proposals cannot override a directed hold");
    }
    private static String camera(String prefix,int owner,int tick,int count){
        return prefix+"|"+owner+"|a|VISIBLE|100|0|100|0|BASE|unknown|"+tick+"|0|"+count;
    }
    private static void positiveReceipts(HouseMap map){
        var ai=new CampaignHunterInvestigator(map,1);ai.assignSeat(1);
        var here=body(map,0,0);var van=monitor(map);
        ai.accept("GEARSTATE|0|0|VIDEO_CAMERA:I|PHOTO_CAMERA:I|NONE:NONE|NONE:NONE");
        ai.accept("TEAMMAP|14|14|-1|-1|-1|-1");
        ai.accept(new LanVideoCameras.Camera(2,0,here.x(),here.y(),here.z(),0,EquipmentTiers.Tier.I).frame());
        ai.accept("CAMERA|POWER|2|1");
        ai.accept("CHAT|RADIO|0|10|team watch empty 0");
        var away=new LanWorld.Body(van.x(),van.y(),van.z(),(van.heading()+180)%360,van.room());
        ai.decide(11,away,null);
        check(ai.decide(42,away,null).action()==Action.TURN,"Physically attend to the camera screen at the relay");
        ai.accept(camera("CAMGHOST",2,42,0));
        check(ai.decide(43,away,null).action()!=Action.NOTE,"Being at the relay facing away yields no evidence");
        check(ai.decide(44,van,null).action()==Action.WATCH,"Choose this particular feed");
        ai.accept(camera("CAMGHOST",2,44,0));
        check(ai.decide(45,van,null).action()!=Action.NOTE,"Unacknowledged subscription cannot create a notebook entry");
        ai.accept("ACCEPT|WATCH|CAMERA|2");
        ai.decide(46,van,null);
        ai.accept(camera("CAMGHOST",3,46,0));
        check(ai.decide(47,van,null).action()!=Action.NOTE,"Unselected camera cannot create a notebook entry");
        ai.accept("CAMGHOST|2|a|HIDDEN|47");
        check(ai.decide(48,van,null).action()!=Action.NOTE,"Blank video cannot exclude a candidate");
        ai.accept(camera("CAMGHOST",2,48,0));
        var note=ai.decide(49,van,null);
        check(note.action()==Action.NOTE&&note.equipment().equals("REMOTE_EMPTY"),"Share actual occupied-count receipt on the authenticated journal");
        ai.accept("CHAT|RADIO|0|50|H2 hold");
        ai.decide(51,here,null);
        int x=(int)Math.round(here.x()*1000),y=(int)Math.round(here.y()*1000),z=(int)Math.round(here.z()*1000);
        ai.accept("APPARITION|VISIBLE|"+x+"|"+y+"|"+(z-1500)+"|52|0|2");
        check(ai.decide(52,here,null).action()!=Action.NOTE,"A body behind the investigator creates no sighting");
        ai.accept("APPARITION|VISIBLE|"+x+"|"+y+"|"+(z+1500)+"|53|0|2");
        note=ai.decide(53,here,null);
        check(note.action()==Action.NOTE&&note.equipment().equals("EYE_GROUP"),"An in-view figure and two live bodies creates an eye witness note");
        ai.accept("GEARSTATE|0|1|VIDEO_CAMERA:I|PHOTO_CAMERA:I|NONE:NONE|NONE:NONE");
        ai.accept(camera("LENSGHOST",1,60,1));ai.accept(camera("SHUTTERGHOST",1,60,1));
        var inattentive=ai.decide(61,here,null);
        check(inattentive.action()!=Action.NOTE&&inattentive.action()!=Action.SHUTTER,"An equipped camera and an unsolicited print are both ignored");
        ai.accept("CHAT|RADIO|0|70|team watch solo 0");
        int raised=-1;
        for(int tick=71;tick<175;tick++){
            var action=ai.decide(tick,here,null);
            if(action.action()==Action.WATCH&&action.slot()==HunterCameraAttention.LENS){raised=tick;break;}
        }
        check(raised>=0,"A field witness deliberately raises the equipped lens");
        ai.accept("ACCEPT|WATCH|LENS");
        ai.accept(camera("LENSGHOST",1,raised,1));
        check(ai.decide(raised+1,here,null).action()==Action.SHUTTER,"A hunter looking through the raised viewfinder takes a shot");
        ai.accept(camera("SHUTTERGHOST",1,raised+1,1));
        note=ai.decide(raised+2,here,null);
        check(note.action()==Action.NOTE&&note.equipment().equals("PHOTO_SOLO"),"A personally exposed print records its actual medium and headcount");
    }
    private static void monitorPatterns(HouseMap map){
        var ai=new CampaignHunterInvestigator(map,1);ai.assignSeat(1);
        var room=body(map,0,0);var van=monitor(map);
        var away=new LanWorld.Body(van.x(),van.y(),van.z(),(van.heading()+180)%360,van.room());
        ai.accept("TEAMMAP|14|14|-1|-1|-1|-1");
        ai.accept("GEAROWNED|VIDEO_CAMERA:I");
        ai.accept(new LanVideoCameras.Camera(2,0,room.x(),room.y(),room.z(),0,EquipmentTiers.Tier.I).frame());
        ai.accept("CAMERA|POWER|2|1");
        ai.decide(1,van,null);
        ai.accept("CAMORB|2|0|0|0|2");
        ai.accept("DOTS|ILLUMINATE|0|2|1000|1000|0|2");ai.accept("LASER|HIT|0|0|1.0|1.3|1.0|2|2");
        ai.accept("CHAT|RADIO|0|10|team watch empty 0");
        ai.decide(11,van,null);ai.decide(42,van,null);ai.decide(43,van,null);
        ai.accept("ACCEPT|WATCH|CAMERA|2");ai.decide(44,van,null);
        ai.accept("CAMORB|3|0|0|0|45");
        ai.accept("DOTS|ILLUMINATE|0|45|1000|1000|0|3");ai.accept("LASER|HIT|0|0|1.0|1.3|1.0|45|3");
        // The server may already have sent a frame while this observer was turning.
        ai.accept("BODY|1|"+Math.round(away.x()*1000)+"|"+Math.round(away.y()*1000)+"|"+Math.round(away.z()*1000)+"|"+away.heading()+"|46");
        ai.accept("CAMORB|2|0|0|0|46");
        ai.accept("DOTS|ILLUMINATE|0|46|1000|1000|0|2");ai.accept("LASER|HIT|0|0|1.0|1.3|1.0|46|2");
        var reports=new ArrayList<String>();
        for(int tick=47;tick<95;tick++){
            var in=ai.decide(tick,van,null);
            if(in.action()==Action.WATCH)ai.accept("ACCEPT|WATCH|CAMERA|2");
            if(in.action()==Action.RADIO)reports.add(in.radio());
        }
        check(reports.stream().noneMatch(s->s.contains("Orb on")||s.contains("D.O.T.S. passage")||s.contains("laser grid disrupted")),
            "No investigator report from unattended, wrong, or turned-away screens");
        ai.accept("CAMORB|2|0|0|0|95");
        ai.accept("DOTS|ILLUMINATE|0|95|1000|1000|0|2");ai.accept("LASER|HIT|0|0|1.0|1.3|1.0|95|2");
        reports.clear();
        for(int tick=96;tick<153;tick++){
            var in=ai.decide(tick,van,null);
            if(in.action()==Action.RADIO)reports.add(in.radio());
        }
        check(reports.stream().anyMatch(s->s.contains("Orb on"))&&reports.stream().anyMatch(s->s.contains("D.O.T.S. passage"))
            &&reports.stream().anyMatch(s->s.contains("laser grid disrupted")),"Investigator reports the three personally observed live feed events");
    }
    public static void main(String[] args){
        var map=new HouseMap();
        setupAndRetest(map);groupStaffing(map);positiveReceipts(map);monitorPatterns(map);
        System.out.println("PASS staffed witness tests, positive journal receipts, occupied-room interruption and inconclusive blank video");
    }
}
