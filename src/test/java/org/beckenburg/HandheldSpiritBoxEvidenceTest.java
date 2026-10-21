package org.beckenburg;

/** Handheld Spirit Box is an active measurement, not a speech-capable station. */
public final class HandheldSpiritBoxEvidenceTest {
    private HandheldSpiritBoxEvidenceTest(){}
    private static void ok(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    public static void main(String[] args){
        var casefile=new LanCase(4);
        var quiet=casefile.scanMeasured(0,LanCase.Tool.SPIRIT_BOX,4,4,100,-1,0);
        ok(quiet.accepted()&&quiet.message().contains("READING SPIRIT_BOX")
            &&quiet.message().contains("No response"),
            "Pressing handheld Spirit Box must produce a real silent measurement");
        var inRange=casefile.scanMeasured(0,LanCase.Tool.SPIRIT_BOX,4,4,101,4,8);
        ok(inRange.accepted(),"Only unattended station should reject speech measurements");
        if(casefile.allowsEvidence(HardEvidenceQuota.Family.SPIRIT_BOX))
            ok(inRange.message().contains("Voice response detected"),
                "Real in-range voiced ghost event must produce its allowed response");
        else
            ok(inRange.message().contains("No response"),
                "Withheld hard evidence must not create a positive voice reading");
        var otherRoom=casefile.scanMeasured(0,LanCase.Tool.SPIRIT_BOX,3,4,102,4,8);
        ok(otherRoom.accepted()&&otherRoom.message().contains("No response"),
            "Same real event from the wrong room must not produce a response");
        var station=casefile.stationMeasured(LanCase.Tool.SPIRIT_BOX,4,4,103,4,8);
        ok(!station.accepted(),"Unattended station cannot sample ghost speech");
        System.out.println("PASS: handheld voice sampling, event-linked detection, silent negatives and station separation");
    }
}
