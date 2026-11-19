package org.beckenburg;

/** Host-side network projection only. Never changes the actual hearing gate. */
final class GhostListeningFeed {
    private Boolean last;
    /** 10Hz host snapshots remain mandatory; 50Hz updates only carry changes. */
    boolean send(boolean listening,boolean heartbeat){
        boolean deliver=heartbeat||last==null||last!=listening;
        if(deliver)last=listening;
        return deliver;
    }
}
