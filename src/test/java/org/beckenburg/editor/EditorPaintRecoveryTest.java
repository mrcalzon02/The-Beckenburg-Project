package org.beckenburg.editor;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Stop a real paint writer at each durable boundary and reopen in the parent JVM. */
public final class EditorPaintRecoveryTest {
    private static void check(boolean ok,String text){if(!ok)throw new AssertionError(text);}
    private static EditorTopologyProject create(Path dir)throws Exception{
        Path glb=Path.of("development/quality/evidence/g2-textured-roundtrip-2026-11-07/edited.glb");
        var p=EditorTopologyProject.fromGlb("box.glb",Files.readAllBytes(glb),dir.resolve("original.btop"));
        p.save();
        return p;
    }
    private static void stroke(EditorTopologyProject p,int pixel,int color){
        p.applyPreview(p.previewPencil(p.snapshot().faces().get(10).id(),List.of(pixel),color));
    }
    private static void child(Path dir,EditorDocumentStore.Checkpoint stop)throws Exception{
        var project=create(dir);
        stroke(project,1,0xffff00ff);
        var writer=new EditorPaintRecovery(project,r->{throw new AssertionError("Crash boundary missed");},point->{
            if(point==stop)Runtime.getRuntime().halt(37);
        });
        Files.writeString(dir.resolve("draft-name"),writer.file().toString());
        writer.request(project.observe(),null);
    }
    private static void crash(Path dir,EditorDocumentStore.Checkpoint point)throws Exception{
        Files.createDirectories(dir);
        var process=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin","java").toString(),
            "-Xmx128m","-Duser.home="+dir,"-cp",System.getProperty("java.class.path"),
            EditorPaintRecoveryTest.class.getName(),dir.toString(),point.name()).inheritIO().start();
        try {
            check(process.waitFor(45,TimeUnit.SECONDS) && process.exitValue()==37,"Paint worker did not halt at "+point);
        }finally{if(process.isAlive())process.destroyForcibly();}
        Path draft=Path.of(Files.readString(dir.resolve("draft-name")));
        check(Files.exists(EditorDocumentStore.journal(draft)),"Expected a durable interrupted journal");
        var original=EditorTopologyProject.open(dir.resolve("original.btop"));
        var initial=original.snapshot();
        stroke(original,1,0xffff00ff);
        var restored=EditorPaintRecovery.openCopy(draft,dir.resolve("restored.btop"));
        check(restored.snapshot().equals(original.snapshot()),"Recovered wrong native paint state");
        check(EditorTopologyProject.open(dir.resolve("original.btop")).snapshot().equals(initial),"Manual save changed");
        check(!Files.exists(EditorDocumentStore.journal(draft)),"Recovery journal not consumed");
    }
    private static void competing(Path dir)throws Exception{
        Files.createDirectories(dir);
        var project=create(dir);
        var events=new LinkedBlockingQueue<EditorPaintRecovery.Result>();
        var writer=new EditorPaintRecovery(project,events::add);
        stroke(project,1,0xffff00ff);
        writer.request(project.observe(),null);
        var result=events.poll(20,TimeUnit.SECONDS);
        check(result!=null && result.error()==null,"First recovery checkpoint failed");
        byte[] external=Files.readAllBytes(project.documentPath());
        Files.write(writer.file(),external);
        stroke(project,2,0xff0000ff);
        writer.request(project.observe(),null);
        result=events.poll(20,TimeUnit.SECONDS);
        check(result!=null && result.error()!=null && Arrays.equals(external,Files.readAllBytes(writer.file())),
            "Autosave overwrote an externally changed draft");
    }
    private static void coalescing(Path dir)throws Exception{
        Files.createDirectories(dir);
        var project=create(dir);
        var clean=project.snapshot();
        var events=new LinkedBlockingQueue<EditorPaintRecovery.Result>();
        var durable=new CountDownLatch(1);
        var proceed=new CountDownLatch(1);
        var writer=new EditorPaintRecovery(project,events::add,point->{
            if(point==EditorDocumentStore.Checkpoint.JOURNAL_DURABLE){
                durable.countDown();
                try{
                    if(!proceed.await(15,TimeUnit.SECONDS))throw new java.io.IOException("Writer was not released");
                }catch(InterruptedException interrupted){Thread.currentThread().interrupt();throw new java.io.IOException(interrupted);}
            }
        });
        stroke(project,1,0xffff00ff);
        long first=project.revision();
        writer.request(project.observe(),null);
        try{
            check(durable.await(15,TimeUnit.SECONDS),"Writer never reached its journal");
            stroke(project,2,0xff0000ff);
            writer.request(project.observe(),null);
            project.restore(clean);
            writer.request(project.observe(),null);
        }finally{proceed.countDown();}
        var a=events.poll(20,TimeUnit.SECONDS);
        var b=events.poll(20,TimeUnit.SECONDS);
        check(a!=null && b!=null && a.error()==null && b.error()==null && a.revision()==first &&
            b.revision()==project.revision(),"Expected first and latest coalesced checkpoints");
        check(EditorTopologyProject.open(writer.file()).snapshot().equals(clean),"Recovery resurrected undone paint");
    }
    public static void main(String[] args)throws Exception{
        if(args.length==2){child(Path.of(args[0]),EditorDocumentStore.Checkpoint.valueOf(args[1]));return;}
        Path root=Files.createTempDirectory("paint-crash-");
        String home=System.getProperty("user.home");System.setProperty("user.home",root.toString());
        try {
            for(var point:EditorDocumentStore.Checkpoint.values())crash(root.resolve(point.name()),point);
            competing(root.resolve("competing"));
            coalescing(root.resolve("coalescing"));
            System.out.println("PASS native paint autosave: two real JVM halts, journal replay, separate writable copy, unchanged manual file, coalesced undo and competing-write refusal");
        }finally{
            System.setProperty("user.home",home);
            try(var paths=Files.walk(root)){for(var p:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}
        }
    }
}
