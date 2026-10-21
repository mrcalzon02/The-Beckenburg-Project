package org.beckenburg;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/** A Twins identity must remain unknown until a companion is actually observed. */
public final class LanTwinSightingPrivacyTest {
    private static void check(boolean condition,String message){
        if(!condition)throw new AssertionError(message);
    }
    private static final class Player implements AutoCloseable {
        final BlockingQueue<String> inbox=new LinkedBlockingQueue<>();
        final List<String> history=new CopyOnWriteArrayList<>();
        final LanClient client;
        Player(int port,boolean ghost,String name)throws Exception{
            client=new LanClient("127.0.0.1",port,ghost,name,
                s->{history.add(s);inbox.offer(s);});
        }
        String await(String prefix)throws Exception{
            long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(13);
            String lastRejection="";
            while(System.nanoTime()<deadline){
                String msg=inbox.poll(90,TimeUnit.MILLISECONDS);
                if(msg!=null&&msg.startsWith(prefix))return msg;
                if(msg!=null&&(msg.startsWith("REJECT|")||msg.startsWith("INFO|host simulation stopped")))
                    lastRejection=msg;
            }
            throw new AssertionError("Missing "+prefix+" on "+client.seat()
                +" / host response: "+(lastRejection.isEmpty()?"none":lastRejection)
                +" / last packets: "+history.subList(Math.max(0,history.size()-8),history.size()));
        }
        @Override public void close(){client.close();}
    }
    public static void main(String[] args)throws Exception{
        try(var host=new LanHost("127.0.0.1",0,
                    GhostIdentity.Type.TWINS,CustomContract.standard(2));
            var a=new Player(host.port(),false,"A");
            var b=new Player(host.port(),false,"B");
            var g=new Player(host.port(),true,"Linked Ghost")) {
            a.client.ready();b.client.ready();g.client.ready();
            a.await("LOBBY|2|HUMAN|3|WAITING");
            a.client.start();a.await("INFO|mission started");
            // Real authored level topology changes with site revisions. Choose
            // legal adjacent transitions instead of assuming an obsolete route.
            HouseMap level=new HouseMap();
            int start=HouseMap.EXTRACTION_ROOM,target=4;
            int[] parent=new int[HouseMap.ROOM_COUNT];
            java.util.Arrays.fill(parent,-1);
            java.util.ArrayDeque<Integer> open=new java.util.ArrayDeque<>();
            open.add(start);parent[start]=start;
            while(!open.isEmpty()&&parent[target]<0){
                int from=open.removeFirst();
                for(int neighbor:level.adjacentRooms(from))
                    if(neighbor>=0&&neighbor<parent.length&&parent[neighbor]<0){
                        parent[neighbor]=from;open.addLast(neighbor);
                    }
            }
            check(parent[target]>=0,"current site must contain a legal van-to-ghost route");
            java.util.ArrayList<Integer> route=new java.util.ArrayList<>();
            for(int current=target;current!=start;current=parent[current])
                route.add(current);
            java.util.Collections.reverse(route);
            for(int room:route){
                a.client.move(room);a.await("ACCEPT|MOVE|"+room);
            }
            a.client.view3D();a.await("ACCEPT|VIEW3D");
            a.await("APPARITION|HIDDEN");
            b.client.view3D();b.await("ACCEPT|VIEW3D");
            b.await("APPARITION|HIDDEN");
            // At least several host samples while the ghost is not manifested.
            g.client.viewGhost3D();g.await("ACCEPT|GVIEW3D");
            int at=0;
            while(at<18)at=Integer.parseInt(g.await("STATE|").split("\\|")[1]);
            for(Player hunter:List.of(a,b)){
                check(hunter.history.stream().noneMatch(s->s.startsWith("TWIN|")
                        ||s.startsWith("CAMGHOST|")&&s.contains("|b|")),
                    "Never announce a second body or Twins species before real sight");
            }
            check(g.history.stream().noneMatch(s->s.startsWith("APPARITION|")
                ||s.startsWith("TWIN|")||s.startsWith("CAMGHOST|")),
                "Ghost player cannot receive investigator-only sight projection");
            check(host.running(),"Privacy rule cannot interrupt the live match");
            System.out.println("PASS: real TCP Twins case stays anonymous until direct or camera sight");
        }
    }
}
