package org.beckenburg.editor;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;
import java.util.function.Consumer;

/** One independently writable recovery document per open paint session. */
public final class EditorPaintRecovery {
    public record Result(long revision,Path file,String error) {}
    private record Request(EditorTopologyProject.State state,long revision,EditorModelWorkspace workspace) {}
    private final EditorTopologyProject owner;
    private final Path file;
    private final Consumer<Result> notify;
    private final EditorDocumentStore.Observer observer;
    private Request pending;
    private boolean running;
    private long latest=-1;
    private String fingerprint;

    public static Path directory(){
        return Path.of(System.getProperty("user.home"),".beckenburg","editor","paint-recovery").toAbsolutePath().normalize();
    }
    public EditorPaintRecovery(EditorTopologyProject project,Consumer<Result> notify){
        this(project,notify,checkpoint->{});
    }
    EditorPaintRecovery(EditorTopologyProject project,Consumer<Result> notify,EditorDocumentStore.Observer observer){
        owner=project;
        this.observer=observer;
        this.notify=java.util.Objects.requireNonNull(notify);
        String name=project.title().replaceAll("[^A-Za-z0-9_-]","_");
        if(name.length()>48)name=name.substring(0,48);
        file=directory().resolve("paint-"+name+"-"+UUID.randomUUID()+".btop");
    }
    public Path file(){return file;}
    public boolean owns(EditorTopologyProject project){return owner==project;}
    public synchronized void request(EditorTopologyProject.Observation observation,EditorModelWorkspace workspace){
        if(!owner.isCurrent(observation) || observation.revision()<=latest)return;
        latest=observation.revision();
        pending=new Request(observation.snapshot(),latest,workspace);
        if(running)return;
        running=true;
        Thread worker=new Thread(this::drain,"Material recovery writer");
        worker.setDaemon(false);
        worker.start();
    }
    private void drain(){
        for(;;){
            Request request;
            synchronized(this){
                request=pending;pending=null;
                if(request==null){running=false;return;}
            }
            String error=null;
            try{
                var view=request.workspace()==null?null:request.workspace().forState(request.state());
                byte[] bytes=EditorTopologyProject.snapshotBytes(request.state(),view);
                EditorDocumentStore.saveIfUnchanged(file,out->out.write(bytes),EditorPaintRecovery::validate,
                    path->{
                        var state=EditorTopologyProject.decode(EditorDocumentStore.readDocument(path));
                        if(!state.equals(request.state()))throw new IOException("Paint recovery state differs from the reviewed edit");
                    },fingerprint,observer);
                fingerprint=EditorDocumentStore.fingerprint(bytes);
            }catch(IOException|RuntimeException failure){error=failure.getMessage()==null?failure.toString():failure.getMessage();}
            notify.accept(new Result(request.revision(),file,error));
        }
    }
    private static void validate(Path path)throws IOException{
        EditorTopologyProject.decode(EditorDocumentStore.readDocument(path));
    }
    /** Replays a pending recovery journal, then opens a separate writable native document. */
    public static EditorTopologyProject openCopy(Path recovery,Path destination)throws IOException{
        Path source=recovery.toAbsolutePath().normalize();
        if(source.equals(destination.toAbsolutePath().normalize()))throw new IOException("Choose a new document for the recovered copy");
        var draft=EditorTopologyProject.open(source);
        byte[] bytes=draft.snapshotBytes();
        EditorDocumentStore.saveIfUnchanged(destination,out->out.write(bytes),EditorPaintRecovery::validate,
            EditorPaintRecovery::validate,null);
        return EditorTopologyProject.open(destination);
    }
}
