package org.beckenburg.editor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Transport-independent access to a host's actual editor, never a scene store.
 * All calls run on the host's owner thread. The host supplies only operations
 * already supported by its human editor and runtime.
 */
public final class EditorAgentService<S> {
    public enum Capability { INSPECT, TRANSACT, UNDO, REDO, SAVE, REOPEN, RECOVER_PREVIOUS, CAPTURE, TEST }
    public record Check(String code, boolean passed, String detail) {}
    public record Inspection<S>(String sceneId, long revision, Path document,
                                S state, List<EditorProject.Node> hierarchy,
                                Set<Capability> capabilities, List<String> operations, List<String> tests) {}
    public record Capture(Path file, int width, int height, String renderer) {}
    public record Evidence<T>(String sceneId, long revision, T result) {}
    public record Command(String operation, Map<String,String> arguments) {
        public Command {Objects.requireNonNull(operation);arguments=Map.copyOf(arguments);}
    }
    public interface Host<S> {
        EditorProject<S> project();
        long revision();
        void checkAccess();
        Set<Capability> capabilities();
        List<String> operations();
        void apply(String operation, Map<String,String> arguments);
        void validate();
        void reopen() throws IOException;
        default void recoverPrevious()throws IOException{throw new UnsupportedOperationException("Previous-save recovery unavailable");}
        Capture capture(Path destination) throws IOException;
        List<String> tests();
        List<Check> test(String id);
    }
    private final Host<S> host;
    private final EditorHistory<S> history;
    private final Runnable changed,saved;
    public EditorAgentService(Host<S> host, EditorHistory<S> history) {
        this(host,history,()->{},()->{});
    }
    public EditorAgentService(Host<S> host, EditorHistory<S> history, Runnable changed, Runnable saved) {
        this.host=Objects.requireNonNull(host);
        this.history=Objects.requireNonNull(history);
        this.changed=Objects.requireNonNull(changed);this.saved=Objects.requireNonNull(saved);
        if(!history.isFor(host.project()))throw new IllegalArgumentException("History belongs to another scene");
    }
    public Inspection<S> inspect() {
        capable(Capability.INSPECT);
        var p=host.project();
        return new Inspection<>(p.id(),host.revision(),p.documentPath(),p.snapshot(),
            List.copyOf(p.hierarchy()),Set.copyOf(host.capabilities()),List.copyOf(host.operations()),List.copyOf(host.tests()));
    }
    private void capable(Capability capability){
        host.checkAccess();
        if(!host.capabilities().contains(capability))throw new UnsupportedOperationException("Unavailable capability: "+capability);
    }
    private void require(long expected) {
        host.checkAccess();
        if(expected!=host.revision())throw new IllegalStateException("Stale scene revision; inspect again");
    }
    public Evidence<Void> transact(long expected, String operation, Map<String,String> arguments) {
        return transact(expected,List.of(new Command(operation,arguments)));
    }
    /** One bounded batch is one undo step; any invalid command rolls it all back. */
    public Evidence<Void> transact(long expected, List<Command> commands) {
        return transact(expected,commands,()->false);
    }
    /** Cancellation is cooperative between bounded host operations, on the owner
     * thread. A cancelled batch restores the full scene and preserves history. */
    public Evidence<Void> transact(long expected, List<Command> commands,
                                   java.util.function.BooleanSupplier cancelled) {
        require(expected);
        Objects.requireNonNull(cancelled);
        var batch=List.copyOf(commands);
        if(batch.isEmpty()||batch.size()>64)throw new IllegalArgumentException("Expected 1–64 commands");
        for(var command:batch)if(!host.operations().contains(command.operation()))
            throw new IllegalArgumentException("Unsupported operation: "+command.operation());
        edit(()->{
            cancelIfRequested(cancelled);
            for(var command:batch){
                host.apply(command.operation(),command.arguments());
                cancelIfRequested(cancelled);
            }
        });
        return evidence(null);
    }
    private static void cancelIfRequested(java.util.function.BooleanSupplier cancelled){
        if(Thread.currentThread().isInterrupted()||cancelled.getAsBoolean())
            throw new java.util.concurrent.CancellationException("Editor batch cancelled; scene restored");
    }
    /** Human commands use the exact same transaction/history boundary. */
    public void edit(Runnable operation) {
        capable(Capability.TRANSACT);
        history.execute(()->{operation.run();host.validate();});
        changed.run();
    }
    public Evidence<Boolean> undo(long expected) {require(expected);capable(Capability.UNDO);boolean ok=history.undo();if(ok)changed.run();return evidence(ok);}
    public Evidence<Boolean> redo(long expected) {require(expected);capable(Capability.REDO);boolean ok=history.redo();if(ok)changed.run();return evidence(ok);}
    public Evidence<Path> save(long expected) throws IOException {
        require(expected);capable(Capability.SAVE);host.validate();host.project().save();saved.run();return evidence(host.project().documentPath());
    }
    /** Reopen is undoable; malformed files cannot replace the live scene. */
    public Evidence<Void> reopen(long expected) throws IOException {
        require(expected);
        capable(Capability.REOPEN);
        try {
            edit(()->{try{host.reopen();}catch(IOException failure){throw new java.io.UncheckedIOException(failure);}});
        } catch(java.io.UncheckedIOException failure) {throw failure.getCause();}
        return evidence(null);
    }
    public Evidence<Capture> capture(long expected, Path destination) throws IOException {
        require(expected);capable(Capability.CAPTURE);return evidence(host.capture(destination));
    }
    /** Explicit disk recovery, followed by reload into the same live object.
     * Undo restores the prior live edit; it does not rewrite the recovered file. */
    public Evidence<Void> recoverPrevious(long expected)throws IOException{
        require(expected);capable(Capability.RECOVER_PREVIOUS);
        try{
            edit(()->{try{host.recoverPrevious();}catch(IOException failure){throw new java.io.UncheckedIOException(failure);}});
        }catch(java.io.UncheckedIOException failure){throw failure.getCause();}
        return evidence(null);
    }
    public Evidence<List<Check>> test(long expected, String id) {
        require(expected);
        capable(Capability.TEST);
        if(!host.tests().contains(id))throw new IllegalArgumentException("Unsupported test: "+id);
        List<Check> checks;
        try {checks=List.copyOf(host.test(id));}
        catch(RuntimeException failure){checks=List.of(new Check("TEST_ERROR",false,failure.toString()));}
        return evidence(checks);
    }
    private <T> Evidence<T> evidence(T value) {return new Evidence<>(host.project().id(),host.revision(),value);}
}
