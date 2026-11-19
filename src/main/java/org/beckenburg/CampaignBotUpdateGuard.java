package org.beckenburg;

import java.util.Objects;

/** Campaign hire loop liveness; no fabricated actions or authority escapes. */
final class CampaignBotUpdateGuard {
    private volatile long failures;
    private volatile String lastFailureType="NONE";
    void run(Runnable update){
        Objects.requireNonNull(update);
        try{update.run();}
        catch(RuntimeException failure){
            long count=++failures;
            lastFailureType=failure.getClass().getSimpleName();
            // Recover from an isolated bad input. Rate-limit recurring faults.
            if(count<=3||count%100==0){
                System.err.println("CAMPAIGN HIRE UPDATE FAULT count="+count+
                    " type="+lastFailureType+
                    " (scheduler remains live; inspect host-local bot trace)");
            }
        }
    }
    long failures(){return failures;}
    String lastFailureType(){return lastFailureType;}
}
