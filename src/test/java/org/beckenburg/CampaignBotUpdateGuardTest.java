package org.beckenburg;

/** One malformed frame cannot permanently terminate campaign AI scheduling. */
public final class CampaignBotUpdateGuardTest {
    private static void ok(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    public static void main(String[] args){
        var guard=new CampaignBotUpdateGuard();
        final int[] progress={0};
        guard.run(()->{throw new IllegalArgumentException("malformed input");});
        guard.run(()->progress[0]++);
        ok(guard.failures()==1&&progress[0]==1,
            "a subsequent hire decision must run after an isolated exception");
        ok(guard.lastFailureType().equals("IllegalArgumentException"),
            "last fault class remains locally diagnosable");
        for(int i=0;i<105;i++)guard.run(()->{throw new IllegalStateException();});
        ok(guard.failures()==106&&guard.lastFailureType().equals("IllegalStateException"),
            "repeating faults are tracked without killing scheduler");
        guard.run(()->progress[0]++);
        ok(progress[0]==2,"normal decisions resume after repeated exceptions");
        System.out.println("PASS: campaign AI scheduler liveness and bounded local fault counters");
    }
}
