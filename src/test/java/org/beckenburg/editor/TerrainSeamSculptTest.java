package org.beckenburg.editor;

import java.nio.file.*;
import java.util.*;
/**
 * Portable end-to-end seam sculpt contract: four owner tiles, one authored
 * vertex, save/reopen, explicit batch bake, and layered-document protection.
 */
public final class TerrainSeamSculptTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static void near(double a,double b,String why){
        check(Math.abs(a-b)<.0005,why+": "+a+" vs "+b);
    }
    public static void main(String[] args)throws Exception{
        Path root=Files.createTempDirectory("non-destructive-seam-sculpt");
        try{
            var world=StitchedHeightmapWorld.create(root.resolve("world"),
                new StitchedHeightmapWorld.Settings(32,2,0,0,-200,1200,24177L,8));
            world.generateRegion(0,0,2,2);
            int before=world.sample(0,0,32,32);
            check(before==world.sample(1,0,0,32)
                &&before==world.sample(0,1,32,0)
                &&before==world.sample(1,1,0,0),
                "Four neighboring tiles begin with a shared corner vertex");
            var session=new TerrainSeamSculpt(world,0,0);
            check(session.tiles().size()==4,"All existing adjacent owner tiles loaded");
            check(session.editable(32,32),"Shared corner editable when all four owners exist");
            check(!session.editable(0,0),"Patch exterior is frozen without neighbor");
            double initial=session.height(32,32);
            boolean invalidDabRejected=false;
            try{session.raise(32,32,2,10000);}
            catch(java.io.IOException expected){invalidDabRejected=true;}
            check(invalidDabRejected&&session.changedVertices()==0,
                "Out-of-range brush dab fails without partially changing any vertex");
            session.raise(32,32,3,11);
            session.raise(32.5,32.5,3,3);
            check(session.changedVertices()>1,"Brush includes multiple global samples");
            check(world.sample(0,0,32,32)==before,
                "Non-destructive brush never changes authoritative source");
            double sculpted=session.height(32,32);
            check(sculpted>initial+11,"Seam height changes in authoring session");
            session.finishStroke();
            check(session.undo(),"Combined brush stroke is undoable");
            near(session.height(32,32),initial,"Undo restores original seam sample");
            check(session.redo(),"Combined brush stroke can be redone");
            near(session.height(32,32),sculpted,"Redo restores edit");
            session.save();
            check(Files.isRegularFile(session.path()),"Saved seam-authoring file exists");
            var reopened=new TerrainSeamSculpt(world,0,0);
            near(reopened.height(32,32),sculpted,
                "Saved multi-owner seam stroke survives reopen");
            reopened.bake();
            check(!Files.exists(reopened.path()),"Baked session cannot be replayed");
            int baked=world.sample(0,0,32,32);
            check(baked!=before,"Source changed only through explicit batch bake");
            check(baked==world.sample(1,0,0,32)
                &&baked==world.sample(0,1,32,0)
                &&baked==world.sample(1,1,0,0),
                "All four unique owner positions share exact baked R16 value");
            check(world.verifySeams().isEmpty(),"Entire terrain retains perfect seams");
            var disk=StitchedHeightmapWorld.open(world.directory());
            check(disk.sample(0,0,32,32)==baked,"Batch seam bake survives world reopen");
            var noDuplicate=new TerrainSeamSculpt(disk,0,0);
            check(noDuplicate.changedVertices()==0,"Reopened post-bake session is clean");

            // Authoring documents tied to pre-bake checksums must NOT be
            // silently invalidated by another source-level seam transaction.
            var protectedLayer=new TerrainLayerStack(disk,0,0);
            protectedLayer.addLayer("Preserve this author's work");
            protectedLayer.save();
            noDuplicate.raise(32,32,2,5);noDuplicate.finishStroke();
            boolean blocked=false;
            try{noDuplicate.bake();}
            catch(java.io.IOException expected){blocked=true;}
            check(blocked,"Existing layered authoring document blocks destructive seam bake");
            check(disk.sample(0,0,32,32)==baked,
                "Blocked bake keeps source unchanged");
            check(disk.verifySeams().isEmpty(),"Blocked bake keeps all seams intact");
            noDuplicate.save();
            // A session can still save as non-destructive edits while a layer
            // document blocks committing it into the original PNG files.
            check(Files.exists(noDuplicate.path()),"Unbaked seam edit remains resumable");
            System.out.println("PASS shared 4-tile sculpt, undo/redo, save/readback, journaled bake, layer guard");
        }finally{
            try(var walk=Files.walk(root)){
                for(Path p:walk.sorted(Comparator.reverseOrder()).toList())
                    Files.deleteIfExists(p);
            }
        }
    }
}
