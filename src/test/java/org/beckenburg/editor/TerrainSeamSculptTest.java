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

            // Existing terrain-layer authoring data is now rebased IN the same
            // journal transaction; the layer itself must remain byte-for-byte
            // identical after its source fingerprint fields.
            var protectedLayer=new TerrainLayerStack(disk,0,0);
            protectedLayer.addLayer("Preserve this author's work");
            protectedLayer.selectLayer(0);
            protectedLayer.brush(TerrainLayerStack.Brush.RAISE,13,13,2,7,0,0,1);
            protectedLayer.finishStroke();
            protectedLayer.paintLayerMask(13,13,3,0,.7);
            protectedLayer.finishStroke();
            protectedLayer.paintMaterial(13,13,2,TerrainLayerStack.Material.MUD,90);
            protectedLayer.finishStroke();
            protectedLayer.freezeRect(12,12,14,14,true);
            protectedLayer.save();
            double oldMask=protectedLayer.layerMask(0,13,13);
            double oldDelta=protectedLayer.height(13,13)-
                (disk.settings().minElevation()+
                 (disk.settings().maxElevation()-disk.settings().minElevation())*
                 disk.sample(0,0,13,13)/65535.0);
            int oldMaterial=protectedLayer.material(13,13);
            boolean oldFrozen=protectedLayer.frozen(13,13);
            var originalBytes=Files.readAllBytes(protectedLayer.path());
            noDuplicate.raise(32,32,2,5);noDuplicate.finishStroke();
            noDuplicate.bake();
            int secondBake=disk.sample(0,0,32,32);
            check(secondBake!=baked,"A second seam bake updates source with authored layers");
            check(disk.verifySeams().isEmpty(),"Rebased seam bake preserves all shared edges");
            var reloaded=new TerrainLayerStack(disk,0,0);
            near(reloaded.layerMask(0,13,13),oldMask,"Rebased sculpt mask preserved");
            near(reloaded.height(13,13)-
                (disk.settings().minElevation()+
                 (disk.settings().maxElevation()-disk.settings().minElevation())*
                 disk.sample(0,0,13,13)/65535.0),oldDelta,
                "Layer height delta preserved when source moves");
            check(reloaded.material(13,13)==oldMaterial&&
                reloaded.frozen(13,13)==oldFrozen,"Paint and protection data retained");
            var changedBytes=Files.readAllBytes(protectedLayer.path());
            check(changedBytes.length==originalBytes.length,"No layer document truncation");
            for(int i=0;i<originalBytes.length;i++)
                if(i<12||i>=20)
                    check(originalBytes[i]==changedBytes[i],
                        "Rebase may only update source checksum bytes");
            boolean staleRejected=false;
            try{protectedLayer.save();}
            catch(java.io.IOException correct){staleRejected=true;}
            check(staleRejected,"Editor open before source rebase cannot overwrite newer data");

            var discardable=new TerrainSeamSculpt(disk,0,0);
            discardable.raise(32,32,2,3);discardable.finishStroke();
            discardable.save();
            check(Files.exists(discardable.path()),"Unbaked seam edit remains resumable");
            discardable.discard();
            check(!Files.exists(discardable.path())&&discardable.changedVertices()==0,
                "Discard clears editing session but never modifies source");
            check(disk.sample(0,0,32,32)==secondBake,
                "Discard preserves last authoritative baked height");
            check(new TerrainSeamSculpt(disk,0,0).changedVertices()==0,
                "Reopening discarded session starts clean");
            System.out.println("PASS shared 4-tile sculpt, undo/redo, save/readback, journaled bake, coordinated layer rebase and discard");
        }finally{
            try(var walk=Files.walk(root)){
                for(Path p:walk.sorted(Comparator.reverseOrder()).toList())
                    Files.deleteIfExists(p);
            }
        }
    }
}
