package org.beckenburg.editor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Isolated Java 17 terrain-zone module test; no Beckenburg game classes. */
public final class TerrainZoneDocumentTest {
    private static void check(boolean ok,String message){
        if(!ok)throw new AssertionError(message);
    }
    private static void near(double actual,double expected,String message){
        check(Math.abs(actual-expected)<1e-9,message+" actual="+actual+" expected="+expected);
    }
    public static void main(String[] args)throws Exception {
        Path dir=Files.createTempDirectory("terrain-zone-module-");
        try {
            var grid=new TerrainZoneDocument.Grid(3,1,.5,131,158);
            var doc=new TerrainZoneDocument("blackpine",grid);
            var original=doc.snapshot();
            check(doc.zones().isEmpty(),"No default fake loading zones");
            check(doc.brush(TerrainZoneDocument.Brush.RAISE,20,20,2,1,0),
                  "Sculpt makes a real grid edit");
            near(doc.delta(34,38),1,"Brush centre raised exactly one metre");
            check(doc.delta(0,0)==0,"Outside brush unaffected");
            var raised=doc.snapshot();
            doc.brush(TerrainZoneDocument.Brush.LOWER,20,20,2,.5,0);
            near(doc.delta(34,38),.5,"Lower brush applies to same vertex");
            doc.restore(raised);
            near(doc.delta(34,38),1,"Undo-style restore retained edited height");
            doc.brush(TerrainZoneDocument.Brush.FLATTEN,20,20,1,1,0);
            near(doc.delta(34,38),0,"Flatten resets offset");
            doc.restore(raised);
            doc.brush(TerrainZoneDocument.Brush.SMOOTH,20,20,2,1,0);
            check(doc.delta(34,38)<1&&doc.delta(34,38)>0,
                  "Smooth averages prior vertices instead of recalculating in-place");
            doc.restore(raised);
            var zone=new TerrainZoneDocument.Zone("cellarExit",18,0,19,22,3,22,
                    "house",8,0,9,1.2,TerrainZoneDocument.Trigger.ENTER);
            doc.putZone(zone);
            check(doc.crossed(16,1,20,20,1,20)==zone,"Outside-to-inside crossing");
            check(doc.crossed(16,1,20,25,1,20)==zone,"Fast outside-to-outside sweep");
            check(doc.crossed(20,1,20,21,1,20)==null,"No repeated entry from inside");
            check(doc.crossed(16,5,20,20,5,20)==null,"Vertical extent enforced");
            check(doc.crossed(16,1,16,18,1,17)==null,"Near miss not a crossing");
            var manual=new TerrainZoneDocument.Zone("interactOnly",1,0,1,3,3,3,
                    "castle",3,0,3,0,TerrainZoneDocument.Trigger.INTERACT);
            doc.putZone(manual);
            check(!manual.entered(0,1,2,2,1,2),"Interact zone cannot auto-trigger");
            Path path=dir.resolve("blackpine.tzmap");
            doc.save(path);
            byte[] once=Files.readAllBytes(path);
            doc.save(path);
            check(java.util.Arrays.equals(once,Files.readAllBytes(path)),
                  "Deterministic, atomic saved representation");
            var loaded=TerrainZoneDocument.load(path,"blackpine",grid);
            near(loaded.delta(34,38),1,"Saved height offset round-trips");
            check(loaded.zones().equals(List.of(zone,manual)),"Zone targets, volumes and spawn retained");
            check(loaded.crossed(16,1,20,25,1,20)!=null,"Loaded crossing works");
            check(loaded.removeZone("cellarExit"),"Zone removal");
            loaded.restore(original);
            near(loaded.delta(34,38),0,"Restore removes edits");
            check(loaded.zones().isEmpty(),"Restore removes created zones");
            boolean rejected=false;
            try {TerrainZoneDocument.load(path,"wrong-map",grid);}
            catch(java.io.IOException expected){rejected=true;}
            check(rejected,"Wrong level cannot silently import data");
            rejected=false;
            try {doc.setDelta(2,2,Double.NaN);}
            catch(IllegalArgumentException expected){rejected=true;}
            check(rejected,"Nonfinite height rejected");
            rejected=false;
            try {new TerrainZoneDocument.Zone("bad",1,0,1,0,3,2,"house",1,0,1,0,
                  TerrainZoneDocument.Trigger.ENTER);}
            catch(IllegalArgumentException expected){rejected=true;}
            check(rejected,"Reversed trigger bounds rejected");
            Files.write(path,new byte[]{0,1,2});
            rejected=false;
            try{TerrainZoneDocument.load(path,"blackpine",grid);}
            catch(java.io.IOException expected){rejected=true;}
            check(rejected,"Corrupt document rejected, not silently replaced");
            System.out.println("PASS terrain brushes, saved height offsets, zone sweeps, undo snapshots and safe file IO");
        } finally {
            try(var stream=Files.walk(dir)){
                stream.sorted(java.util.Comparator.reverseOrder()).forEach(p->{
                    try{Files.deleteIfExists(p);}catch(java.io.IOException ignored){}
                });
            }
        }
    }
}
