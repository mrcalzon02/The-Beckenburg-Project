package org.beckenburg;

import java.awt.Component;
import java.awt.Container;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import javax.swing.*;

/** Browsing real packaged pictures cannot reorder or bypass saved campaign gates. */
public final class MissionBoardIntegrationTest {
    static void check(boolean value,String reason){if(!value)throw new AssertionError(reason);}
    static <T extends Component> T named(Container root,String name,Class<T> type){
        for(Component c:root.getComponents()){
            if(type.isInstance(c)&&name.equals(c.getName()))return type.cast(c);
            if(c instanceof Container child){T found=named(child,name,type);if(found!=null)return found;}
        }return null;
    }
    public static void main(String[] args)throws Exception{
        var expected=List.of(SiteCatalog.Location.HOUSE,SiteCatalog.Location.SCHOOL,
            SiteCatalog.Location.FARMHOUSE,SiteCatalog.Location.MANSION,SiteCatalog.Location.PRISON,
            SiteCatalog.Location.ASYLUM,SiteCatalog.Location.ACADEMY,SiteCatalog.Location.COLLEGE,SiteCatalog.Location.CHURCH);
        var catalog=CampaignBoardPanel.catalog();
        check(catalog.size()==22,"all saved campaign ordinals retained");
        for(int i=0;i<expected.size();i++)check(catalog.get(i).map()==expected.get(i),"saved chapter map retained at "+i);
        for(int i=9;i<22;i++)check(catalog.get(i).map()==null&&!catalog.get(i).playable(),"remote concepts remain locked");
        for(var site:HouseMap.Site.values())check(!MissionSites.title(site.name()).isBlank(),"lobby can label every authoring enum");
        check(!SiteCatalog.playable(HouseMap.Site.HOLLOW_CREEK),"lore alias does not unlock prototype geometry");
        for(var entry:MissionSites.all())try(var in=MissionBoardPanel.class.getResourceAsStream(
                "/org/beckenburg/mission-board/"+entry.imageKey()+".png")){
            check(in!=null,"packaged site picture missing: "+entry.id());
            var image=ImageIO.read(in);check(image!=null&&image.getWidth()>0&&image.getHeight()>0,"picture decodes: "+entry.id());
        }
        var directory=Files.createTempDirectory("beckenburg-mission-pictures");
        var launches=new AtomicInteger();var profile=new ProfileStore(directory.resolve("career.properties"));
        MissionBoardPanel[] picture={null};
        SwingUtilities.invokeAndWait(()->{
            var board=new CampaignBoardPanel(profile,new CampaignBoardPanel.Actions(){
                public void deploy(SiteCatalog.Location map,boolean assistant){check(map==SiteCatalog.Location.HOUSE,"only unlocked House dispatches");launches.incrementAndGet();}
                public void castle(){} public void tutorial(){}
            });
            picture[0]=named(board,"mission.board",MissionBoardPanel.class);
            var tab=named(board,"campaign.view.sites",JButton.class);
            var dispatch=named(board,"campaign.deploy",JButton.class);
            check(picture[0]!=null&&tab!=null,"pictures reachable from actual campaign UI");tab.doClick();
            check(picture[0].isVisible(),"site pictures card opens");
            picture[0].select("SCHOOL");check(!dispatch.isEnabled(),"browsing locked School cannot bypass story checkpoint");
            picture[0].select("WLD-02");check(!dispatch.isEnabled(),"browsing remote concept cannot dispatch");
            picture[0].select("CASTLE");check(!dispatch.isEnabled(),"headquarters cannot become an investigation");
            picture[0].select("HOUSE");check(dispatch.isEnabled(),"unlocked House remains dispatchable");
            dispatch.doClick();check(launches.get()==1,"real campaign dispatch action retained");
        });
        long deadline=System.nanoTime()+5_000_000_000L;boolean[] ready={false};
        while(!ready[0]&&System.nanoTime()<deadline){
            SwingUtilities.invokeAndWait(()->ready[0]=picture[0].imageReady());
            if(!ready[0])Thread.sleep(20);
        }
        check(ready[0],"actual asynchronous picture loader completes");
        SwingUtilities.invokeAndWait(()->picture[0].close());
        Files.deleteIfExists(directory.resolve("career.properties"));Files.deleteIfExists(directory);
        System.out.println("PASS packaged pictures, async UI load, saved chapter order, prototype privacy and actual locked/unlocked dispatch");
    }
}
