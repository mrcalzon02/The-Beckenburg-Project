package org.beckenburg;

/** Repeated movement inputs cannot saturate the EDT with duplicate hearing status. */
public final class GhostListeningFeedTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void main(String[] args){
        GhostListeningFeed feed=new GhostListeningFeed();
        check(feed.send(true,true),"initial host snapshot");
        for(int i=0;i<40;i++)check(!feed.send(true,false),"steady 50Hz state is redundant");
        check(feed.send(false,false),"actual movement must immediately mute hearing");
        for(int i=0;i<40;i++)check(!feed.send(false,false),"moving ghost stays muted without spam");
        check(feed.send(false,true),"10Hz host snapshot is never suppressed");
        check(feed.send(true,false),"stationary listening transition must reach ghost");
        check(feed.send(true,true),"host heartbeat remains authoritative");
        System.out.println("PASS: ghost listening 50Hz change-only projection and 10Hz authoritative heartbeat");
    }
}
