package org.beckenburg;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;

/** Private local transport; solo uses the exact multiplayer host, gear and viewport. */
final class LocalInvestigation implements AutoCloseable {
    private final LanHost host;
    private final LanClient client;
    private final ProfileStore profile;
    private final boolean campaign;
    private final List<String> buffered=new ArrayList<>();
    private final LinkedBlockingQueue<String> startup=new LinkedBlockingQueue<>();
    private LanWorldPanel view;
    private boolean closed,started;
    private IOException saveFailure;
    private ContractRewards.Settlement pendingPayout;

    LocalInvestigation(ProfileStore profile,SiteCatalog.Location location,CharacterVisuals.Hunter hunter,
            MissionWeather.Mode mode,EvidenceSignals.Weather weather,int intensity,
            CampaignStory.Chapter chapter,int hires)throws Exception {
        this.profile=profile;campaign=chapter!=null;
        SiteCatalog.requirePlayable(location);
        if(chapter!=null&&chapter.map()!=location)throw new IllegalArgumentException("Campaign site mismatch");
        var contract=new CustomContract(1+hires,100,150,100,100,100,100,100,
            CustomContract.Sprint.LIMITED,70,true,true,mode,weather,intensity);
        var type=chapter==null?CampaignStory.rollFreePlay(System.nanoTime()):chapter.ghost();
        host=new LanHost("127.0.0.1",0,type,contract,false,true,SiteCatalog.mapFor(location));
        LanClient joining=null;
        try{
            if(campaign)host.configureCampaign(profile,chapter);else host.configureFreePlay(profile);
            String password=UUID.randomUUID().toString();host.setLobbyPassword(password);
            joining=new LanClient("127.0.0.1",host.port(),false,"Investigator",password,
                host.localOwnerCredential(),this::receive);
            client=joining;
            client.selectVisual(hunter);client.ready();
            // Commands share an ordered stream; bots require the ready acknowledgment first.
            await("OWNREADY|YES");
            if(campaign&&hires>0)host.prepareCampaignBots(hires);
            client.start();await("INFO|mission started");
            client.view3D();await("ACCEPT|VIEW3D");
            synchronized(this){started=true;startup.clear();}
        }catch(Exception failure){if(joining!=null)joining.close();host.close();throw failure;}
    }
    private void await(String prefix)throws Exception {
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(15);
        while(System.nanoTime()<deadline){
            String event=startup.poll(100,TimeUnit.MILLISECONDS);
            if(event==null)continue;
            if(event.startsWith(prefix))return;
            if(event.startsWith("REJECT|")||event.startsWith("ERROR|"))throw new IOException(event);
        }
        throw new IOException("Local investigation did not acknowledge "+prefix);
    }
    private void receive(String event){
        synchronized(this){
            if(closed)return;
            if(!started)startup.offer(event);
            if(event.startsWith("PAYOUT|")&&!campaign){
                try{pendingPayout=ContractRewards.Settlement.decode(event);
                    retrySave();
                }catch(IOException failure){saveFailure=failure;}
            }
            if(view==null){buffered.add(event);return;}
        }
        SwingUtilities.invokeLater(()->{synchronized(this){if(!closed&&view!=null)view.onEvent(event);}});
    }
    LanWorldPanel openView(){
        if(!SwingUtilities.isEventDispatchThread())throw new IllegalStateException("EDT required");
        synchronized(this){
            if(closed)throw new IllegalStateException("Investigation closed");
            if(view==null){
                view=new LanWorldPanel(client);view.setCampaignInventory(true);
                for(String event:buffered)view.onEvent(event);
                buffered.clear();
            }
            return view;
        }
    }
    LanClient client(){return client;}
    boolean running(){return host.running();}
    synchronized void retrySave()throws IOException {
        if(campaign){host.retryCampaignSettlement();return;}
        if(pendingPayout!=null){
            profile.addNetworkPayout(pendingPayout.id(),pendingPayout.credits(),pendingPayout.xp());
            pendingPayout=null;saveFailure=null;
        }
    }
    IOException saveFailure(){synchronized(this){return saveFailure;}}
    @Override public void close(){
        synchronized(this){if(closed)return;closed=true;buffered.clear();}
        client.close();host.close();
    }
}
