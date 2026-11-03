package org.beckenburg.editor;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Real filesystem/child-process crash tests for journal replay and conflicts.
 * The fixture is a genuine portable RuntimeScene document, not an agent world.
 */
public final class EditorDocumentStoreTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private interface IO {void run()throws Exception;}
    private static void fails(IO action)throws Exception{
        try{action.run();}catch(IOException expected){return;}
        throw new AssertionError("Expected persistence failure");
    }
    private static InMemoryRuntimeScene scene(String id,double x){
        var scene=new InMemoryRuntimeScene(id);
        scene.replace(new RuntimeScene.Node("lamp","Lamp",null,"lights",
            new RuntimeScene.Transform(x,2,3,0,0,0,1,1,1),List.of()));return scene;
    }
    private static void crash(Path target,Path next,EditorDocumentStore.Checkpoint point)throws Exception{
        String java=Path.of(System.getProperty("java.home"),"bin",System.getProperty("os.name").startsWith("Windows")?"java.exe":"java").toString();
        var process=new ProcessBuilder(java,"-ea","-cp",System.getProperty("java.class.path"),
            EditorDocumentStoreTest.class.getName(),"--crash",target.toString(),next.toString(),point.name()).inheritIO().start();
        if(!process.waitFor(30,TimeUnit.SECONDS)){process.destroyForcibly();throw new AssertionError("Crash worker hung");}
        check(process.exitValue()==73,"Worker halted at requested durable boundary");
    }
    public static void main(String[] args)throws Exception{
        if(args.length>0){
            byte[] bytes=Files.readAllBytes(Path.of(args[2]));
            EditorDocumentStore.save(Path.of(args[1]),out->out.write(bytes),RuntimeSceneDocument::readRaw,
                RuntimeSceneDocument::readRaw,point->{if(point.name().equals(args[3]))Runtime.getRuntime().halt(73);});
            throw new AssertionError("Worker missed checkpoint");
        }
        Path root=Files.createTempDirectory("editor-document-store-");
        try{
            Path next=root.resolve("after.bscn");var after=scene("fixture",9);RuntimeSceneDocument.save(next,after);
            byte[] nextBytes=Files.readAllBytes(next);
            for(var point:EditorDocumentStore.Checkpoint.values())for(boolean existed:new boolean[]{false,true}){
                Path dir=Files.createDirectory(root.resolve(point+"-"+existed));Path target=dir.resolve("level.bscn");
                if(existed)RuntimeSceneDocument.save(target,scene("fixture",1));
                byte[] before=existed?Files.readAllBytes(target):null;
                crash(target,next,point);
                check(Files.isRegularFile(EditorDocumentStore.journal(target)),"Durable journal survives actual process termination");
                if(point==EditorDocumentStore.Checkpoint.JOURNAL_DURABLE)
                    check(existed?Arrays.equals(before,Files.readAllBytes(target)):!Files.exists(target),"Pre-rename crash leaves original document untouched");
                check(RuntimeSceneDocument.load(target).nodes().equals(after.nodes()),"Fresh codec load recovers exact saved nodes after crash");
                check(Arrays.equals(nextBytes,Files.readAllBytes(target)),"Recovery preserves exact authoritative BSCN bytes");
                check(!Files.exists(EditorDocumentStore.journal(target)),"Committed/replayed journal is cleared");
                try(var files=Files.list(dir)){check(files.noneMatch(p->p.toString().endsWith(".tmp")),"Recovery cleans owned crash staging files");}
                if(existed)check(Arrays.equals(before,Files.readAllBytes(EditorDocumentStore.previous(target))),"Prior valid bytes remain recoverable");
            }
            Path conflict=root.resolve("conflict.bscn");RuntimeSceneDocument.save(conflict,scene("fixture",1));
            crash(conflict,next,EditorDocumentStore.Checkpoint.JOURNAL_DURABLE);
            byte[] log=Files.readAllBytes(EditorDocumentStore.journal(conflict));
            byte[] external="external unsaved conflict".getBytes(java.nio.charset.StandardCharsets.UTF_8);Files.write(conflict,external);
            fails(()->RuntimeSceneDocument.load(conflict));
            check(Arrays.equals(external,Files.readAllBytes(conflict))&&Arrays.equals(log,Files.readAllBytes(EditorDocumentStore.journal(conflict))),
                "Replay cannot overwrite external changes or consume conflict evidence");
            Path damaged=root.resolve("damaged-journal.bscn");RuntimeSceneDocument.save(damaged,scene("fixture",1));
            byte[] original=Files.readAllBytes(damaged);crash(damaged,next,EditorDocumentStore.Checkpoint.JOURNAL_DURABLE);
            byte[] corrupt=Files.readAllBytes(EditorDocumentStore.journal(damaged));corrupt[corrupt.length-1]^=1;
            Files.write(EditorDocumentStore.journal(damaged),corrupt);fails(()->RuntimeSceneDocument.load(damaged));
            check(Arrays.equals(original,Files.readAllBytes(damaged)),"Corrupt journal never changes valid document");
            Path foreign=root.resolve("foreign.bscn");RuntimeSceneDocument.save(foreign,scene("fixture",1));
            Files.write(EditorDocumentStore.journal(foreign),log);fails(()->RuntimeSceneDocument.load(foreign));
            check(Arrays.equals(original,Files.readAllBytes(foreign)),"Journal cannot be copied across document paths");
            Path target=root.resolve("recovery.bscn");RuntimeSceneDocument.save(target,scene("fixture",1));
            RuntimeSceneDocument.save(target,after);byte[] prior=Files.readAllBytes(EditorDocumentStore.previous(target));
            RuntimeSceneDocument.save(target,after);
            check(Arrays.equals(prior,Files.readAllBytes(EditorDocumentStore.previous(target))),"No-op save preserves distinct previous version");
            Files.writeString(target,"corrupted current document");fails(()->RuntimeSceneDocument.load(target));
            RuntimeSceneDocument.restorePrevious(target,path->{if(!RuntimeSceneDocument.readRaw(path).sceneId().equals("fixture"))throw new IOException("Wrong scene");});
            check(Arrays.equals(prior,Files.readAllBytes(target)),"Explicit validated recovery restores previous authoritative bytes");
            RuntimeSceneDocument.save(EditorDocumentStore.previous(target),scene("other",3));
            fails(()->RuntimeSceneDocument.restorePrevious(target,path->{if(!RuntimeSceneDocument.readRaw(path).sceneId().equals("fixture"))throw new IOException("Wrong scene");}));
            check(Arrays.equals(prior,Files.readAllBytes(target)),"Foreign backup cannot change owning document");
            fails(()->EditorDocumentStore.save(target,out->{byte[] block=new byte[65536];for(int i=0;i<129;i++)out.write(block);},RuntimeSceneDocument::readRaw));
            check(Arrays.equals(prior,Files.readAllBytes(target))&&!Files.exists(EditorDocumentStore.journal(target)),"Oversized output fails before target or journal change");
            var large=scene("fixture",1);var properties=new java.util.LinkedHashMap<String,String>();
            for(int i=0;i<128;i++)properties.put("p"+i,"x".repeat(3000));
            var component=new RuntimeScene.Component("large",properties);
            for(int i=0;i<30;i++)large.replace(new RuntimeScene.Node("large-"+i,"Large",null,"test",
                RuntimeScene.Transform.identity(),List.of(component)));
            fails(()->RuntimeSceneDocument.save(target,large));
            check(Arrays.equals(prior,Files.readAllBytes(target)),"Actual BSCN encoder stops at storage budget without changing document");
            Path guarded=root.resolve("guarded.bscn");
            EditorDocumentStore.saveIfUnchanged(guarded,out->out.write(nextBytes),RuntimeSceneDocument::readRaw,RuntimeSceneDocument::readRaw,null);
            String observed=EditorMeshPipeline.sourceFingerprint(nextBytes);
            fails(()->EditorDocumentStore.saveIfUnchanged(guarded,out->out.write(prior),RuntimeSceneDocument::readRaw,RuntimeSceneDocument::readRaw,null));
            check(Arrays.equals(nextBytes,Files.readAllBytes(guarded)),"Expected-missing guard cannot overwrite an existing document");
            byte[] lateChange="external change during staging".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            fails(()->EditorDocumentStore.saveIfUnchanged(guarded,out->{out.write(prior);Files.write(guarded,lateChange);},RuntimeSceneDocument::readRaw,RuntimeSceneDocument::readRaw,observed));
            check(Arrays.equals(lateChange,Files.readAllBytes(guarded))&&!Files.exists(EditorDocumentStore.journal(guarded)),"Observed fingerprint rechecked after staging before any source replacement");
            Files.delete(guarded);
            fails(()->EditorDocumentStore.saveIfUnchanged(guarded,out->out.write(prior),RuntimeSceneDocument::readRaw,RuntimeSceneDocument::readRaw,observed));
            check(!Files.exists(guarded),"External deletion is not silently recreated by stale editor");
            Path lock=target.resolveSibling("."+target.getFileName()+".editor.lock");
            try(var channel=FileChannel.open(lock,StandardOpenOption.WRITE);var held=channel.lock()){
                fails(()->RuntimeSceneDocument.save(target,after));
            }
            check(Arrays.equals(prior,Files.readAllBytes(target)),"Concurrent writer lock preserves current document");
            System.out.println("PASS portable document store: actual process crashes before/after rename, exact replay, backup recovery, conflicts, corruption, ownership, output budget and writer lock");
        }finally{
            try(var files=Files.walk(root)){for(Path file:files.sorted(java.util.Comparator.reverseOrder()).toList())Files.deleteIfExists(file);}
        }
    }
}
