package org.beckenburg;

/** Handheld Spirit Box is an active measurement, not a speech-capable station. */
public final class HandheldSpiritBoxEvidenceTest {
    private HandheldSpiritBoxEvidenceTest(){}
    private static void ok(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    public static void main(String[] args){
        var casefile=new LanCase(4);
        var quiet=casefile.scanMeasured(0,LanCase.Tool.SPIRIT_BOX,4,4,100,-1,0);
        ok(quiet.accepted()&&quiet.message().contains("READING SPIRIT_BOX")
            &&quiet.message().contains("Radio static; no intelligible voice"),
            "Pressing handheld Spirit Box must produce a real silent measurement");
        var inRange=casefile.scanMeasured(0,LanCase.Tool.SPIRIT_BOX,4,4,101,4,8);
        ok(inRange.accepted(),"Only unattended station should reject speech measurements");
        ok(inRange.message().contains("Radio static; no intelligible voice")
            &&!inRange.message().contains("Vocal fragment heard"),
            "Generic event strength and room proximity cannot attest to actual played speech");
        var otherRoom=casefile.scanMeasured(0,LanCase.Tool.SPIRIT_BOX,3,4,102,4,8);
        ok(otherRoom.accepted()&&otherRoom.message().contains("Radio static; no intelligible voice"),
            "Same real event from the wrong room must not produce a response");
        var station=casefile.stationMeasured(LanCase.Tool.SPIRIT_BOX,4,4,103,4,8);
        ok(!station.accepted(),"Unattended station cannot sample ghost speech");
        System.out.println("PASS: accepted handheld raw static, no fabricated reply from generic events, wrong-room negatives and station separation");
    }
}
