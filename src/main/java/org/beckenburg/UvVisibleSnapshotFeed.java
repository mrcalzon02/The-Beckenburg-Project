package org.beckenburg;

import java.util.List;
import java.util.Objects;

/**
 * The visual UV decal feed is one complete ordered snapshot per changed set.
 * Actual UV evidence collection and specimen lifetime remain host authoritative.
 * Each peer owns a separate cache; never shares private traces between seats.
 */
final class UvVisibleSnapshotFeed {
    private List<UvEvidenceMarks.Mark> last;
    private long sent,unchanged;
    boolean changed(List<UvEvidenceMarks.Mark> current){
        Objects.requireNonNull(current);
        if(last!=null&&last.equals(current)){
            unchanged++;return false;
        }
        last=List.copyOf(current);
        sent++;
        return true;
    }
    /** A newly constructed viewport requires a complete snapshot, even empty. */
    void invalidate(){last=null;}
    long sends(){return sent;}
    long skipped(){return unchanged;}
}
