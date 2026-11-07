package org.beckenburg;

import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.util.function.BooleanSupplier;
import javax.swing.SwingUtilities;

/** Real solo launch, same viewport and TCP host as co-op, including gear and posture. */
public final class LocalInvestigationTest {
    private static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
    private static Object field(Object value,String name)throws Exception{
        var f=value.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(value);
    }
    private static void until(BooleanSupplier condition,String why)throws Exception{
        long end=System.nanoTime()+8_000_000_000L;
        while(System.nanoTime()<end){
            final boolean[] okay={false};SwingUtilities.invokeAndWait(()->okay[0]=condition.getAsBoolean());
            if(okay[0])return;Thread.sleep(25);
        }
        throw new AssertionError(why);
    }
    public static void main(String[] args)throws Exception{
        System.setProperty("beckenburg.audio.mute","true");
        var profile=new ProfileStore(Files.createTempDirectory("shared-solo-").resolve("profile.properties"));
        profile.load();
        try(var solo=new LocalInvestigation(profile,SiteCatalog.Location.HOUSE,CharacterVisuals.Hunter.CLERIC,
                MissionWeather.Mode.FIXED,EvidenceSignals.Weather.CLEAR,1,null,0)){
            check(solo.running(),"one local hunter starts the real host without a lobby");
            final LanWorldPanel[] result={null};SwingUtilities.invokeAndWait(()->result[0]=solo.openView());
            var panel=result[0];
            try{
                LanWorldControlsTest.tap(panel,KeyEvent.VK_SPACE); // skip local flyover
                check(panel.getClass()==LanWorldPanel.class,"solo uses the multiplayer viewport");
                var gear=(String[])field(panel,"equippedGear");
                check(java.util.Arrays.equals(gear,profile.catalogLoadout()),"same host-owned saved loadout");
                LanWorldControlsTest.tap(panel,KeyEvent.VK_2);
                LanWorldControlsTest.tap(panel,GameControls.USE);
                var readings=(java.util.Map<?,?>)field(panel,"deviceReading");
                until(()->String.valueOf(readings.get("THERMOMETER")).contains("Air "),"F produces a real measured host temperature in solo");
                LanWorldControlsTest.tap(panel,GameControls.SWAP);
                until(()->panel.selectedTool()==LanCase.Tool.SPIRIT_BOX,"Q cycles the actual host belt");
                LanWorldControlsTest.tap(panel,GameControls.LIGHT);
                until(()->{try{return (boolean)field(panel,"flashlightOn");}catch(Exception e){throw new RuntimeException(e);}},"T toggles carried flashlight without replacing current tool");
                check(panel.selectedTool()==LanCase.Tool.SPIRIT_BOX,"flashlight leaves selected instrument alone");
                LanWorldControlsTest.tap(panel,GameControls.CROUCH);
                until(()->{try{return (boolean)field(panel,"crouched");}catch(Exception e){throw new RuntimeException(e);}},"Shift crouches via host posture");
                LanWorldControlsTest.tap(panel,KeyEvent.VK_TAB);
                check(panel.casefileOpen(),"Tab journal works in the identical solo view");
                LanWorldControlsTest.tap(panel,KeyEvent.VK_ESCAPE);
                var host=(LanHost)field(solo,"host");
                var mission=(LanCase)field(host,"mission");Object mutex=field(host,"mutex");
                int wards=mission.wards(0);
                SwingUtilities.invokeAndWait(()->panel.fieldAction(6));
                until(()->{synchronized(mutex){return mission.wards(0)==wards-1;}},"solo field action spends one real host ward");
                var map=(HouseMap)field(host,"map");var world=(LanWorld)field(host,"world");
                synchronized(field(host,"mutex")){AuthoredTestSupport.place(map,world,0,map.spawn(4));}
                SwingUtilities.invokeAndWait(()->panel.onEvent(world.bodyFrame(0,0)));
                LanWorldControlsTest.tap(panel,KeyEvent.VK_4);
                LanWorldControlsTest.tap(panel,GameControls.DROP);
                var drops=(LanDroppedGear)field(host,"droppedGear");
                until(()->drops.snapshot().stream().anyMatch(d->d.tool().equals("PHOTO_CAMERA")),"R drops the actual camera, not only EMF");
                var drop=drops.snapshot().stream().filter(d->d.tool().equals("PHOTO_CAMERA")).findFirst().orElseThrow();
                solo.client().pickupDroppedEquipment(drop.id());
                until(()->drops.snapshot().isEmpty(),"same multiplayer pickup restores the dropped camera");
                for(String tool:FieldEquipment.workingTools()){
                    var kit=new FieldEquipment();int existing=-1;
                    for(int i=0;i<FieldEquipment.SLOTS;i++)if(kit.at(i).equals(tool))existing=i;
                    if(existing>=0)kit.selectSlot(existing);else kit.equip(0,tool);
                    var registry=new LanDroppedGear();var item=registry.drop(kit,0,world.body(0),map);
                    check(kit.active().equals(FieldEquipment.EMPTY),tool+" leaves belt on drop");
                    check(LanDroppedGear.Drop.decode(LanProtocol.parts(item.frame())).tool().equals(tool),tool+" physical packet roundtrip");
                    registry.pickup(kit,0,item.id(),world.body(0),map);
                    check(kit.active().equals(tool),tool+" returns to its actual belt slot");
                }

                solo.client().identify((GhostIdentity.Type)field(host,"ghostType"));
                until(()->{synchronized(mutex){return mission.correctlyIdentified();}},"identification uses the shared host");
                synchronized(mutex){AuthoredTestSupport.place(map,world,0,map.spawn(HouseMap.EXTRACTION_ROOM));}
                solo.client().vanDoor();
                until(()->!solo.running(),"physical van departure ends the solo round");
                until(()->{try{return field(panel,"settlement")!=null;}catch(Exception e){throw new RuntimeException(e);}},"same host settlement reaches solo viewport");
                var payout=(ContractRewards.Settlement)field(panel,"settlement");
                check(payout.credits()>0&&profile.currency()==payout.credits(),"real shared-case payout saved to solo wallet");
                solo.retrySave();profile.load();
                check(!profile.addNetworkPayout(payout.id(),payout.credits(),payout.xp()),"solo payout receipt survives reload and prevents duplication");

            }finally{SwingUtilities.invokeAndWait(panel::close);}
        }
        profile.addNetworkPayout(java.util.UUID.randomUUID(),1000,0);
        check(profile.reserveMissionHunters(1),"reserve an actual campaign team member");
        var chapter=profile.dispatchStory(0);
        try(var solo=new LocalInvestigation(profile,chapter.map(),CharacterVisuals.Hunter.CLERIC,
                MissionWeather.Mode.FIXED,EvidenceSignals.Weather.CLEAR,1,chapter,1)){
            check(solo.running(),"campaign solo also starts the same authoritative host");
            check(((LanHost)field(solo,"host")).connected()==2,"hired ally joins that same multiplayer host");
        }
        System.out.println("PASS: free play and campaign solo run the co-op host, viewport, equipment, flashlight, journal and posture");
    }
}
