package org.beckenburg;

import java.util.List;

/** Unchanged UV decals are not continuously retransmitted over an otherwise reliable TCP stream. */
public final class UvVisibleSnapshotFeedTest {
    private static void require(boolean truth,String why){if(!truth)throw new AssertionError(why);}
    public static void main(String[] args){
        var a=new UvVisibleSnapshotFeed();
        var b=new UvVisibleSnapshotFeed();
        var first=new UvEvidenceMarks.Mark(1,4,12.0,0.012,16.0,20);
        var second=new UvEvidenceMarks.Mark(2,4,12.3,0.012,16.2,30);
        require(a.changed(List.of()),"first blank UV state clears an entering viewport");
        for(int tick=3;tick<=300;tick+=3)
            require(!a.changed(List.of()),"blank scenes should not send repeated RESET packets");
        require(a.skipped()==100,"one hundred redundant polling ticks elided");
        require(a.changed(List.of(first)),"new physical UV decal transmitted");
        for(int tick=0;tick<90;tick++)
            require(!a.changed(List.of(first)),"same mark set is not retransmitted");
        require(a.changed(List.of(first,second)),"additional real trace transmitted");
        require(a.changed(List.of(second)),"first mark moving out of cone or expiring is removed");
        require(a.changed(List.of()),"UV power off or all marks expiring clears client view");
        require(!a.changed(List.of()),"stable powered-off state does not flood TCP");
        a.invalidate();
        require(a.changed(List.of()),"reopening 3D view receives fresh RESET");
        require(b.changed(List.of(first)),"a different hunter's UV visibility is independent");
        require(b.changed(List.of()),"different hunter can clear without changing first hunter");
        require(a.sends()==6&&b.sends()==2,"expected bounded packet snapshots");
        System.out.println("PASS: per-seat UV evidence snapshots, expiry/power off and 3D reentry without 300ms spam");
    }
}
