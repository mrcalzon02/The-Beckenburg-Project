package org.beckenburg;

import java.io.IOException;
import java.util.concurrent.*;
import java.util.function.Supplier;

/** A hired hunter submits the same bounded inputs as a human, without ghost knowledge. */
final class CampaignGuestBot implements AutoCloseable {
    record Input(boolean running,int tick,int dx,int dz,boolean door,boolean scan){}
    private final LanClient client;
    private final ScheduledExecutorService clock;
    private boolean entered;
    private int lastScan=-1000;
    CampaignGuestBot(int port,String name,String password,Supplier<Input> inputs)throws IOException{
        client=new LanClient("127.0.0.1",port,false,name,password,"",frame->{});
        clock=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"campaign-hire");t.setDaemon(true);return t;});
        clock.scheduleWithFixedDelay(()->{
            try{
                Input input=inputs.get();if(!input.running())return;
                if(!entered){client.view3D();entered=true;return;}
                if(input.door())client.door();
                if(input.dx()!=0||input.dz()!=0)client.step(input.dx(),input.dz());
                if(input.scan()&&input.tick()-lastScan>=100){
                    client.scan(LanCase.Tool.THERMOMETER);lastScan=input.tick();
                }
            }catch(IOException|IllegalArgumentException ignored){/* Host rejects blocked/cooling inputs. */}
        },100,150,TimeUnit.MILLISECONDS);
    }
    int seat(){return Integer.parseInt(client.seat().substring(1))-1;}
    @Override public void close(){clock.shutdownNow();client.close();}
}
