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
    public enum DiskState { MATCHES, DIFFERS, MISSING, UNREADABLE, UNSUPPORTED }
    public record PositionProof(EditorProject.Node actual,boolean verified,boolean committed,DiskState diskState){}
    public record Selection(String kind,String id,Map<String,String> properties){
        public Selection { properties=Map.copyOf(properties); }
    }
    /** Implement only in a host that can read the active human selection on its owner thread. */
    public interface SelectionSource { Selection selection(); }
    public interface WalkingCollision { boolean walkable(double x,double y,double z); }
    public interface NodeFocus { void focus(EditorProject.Node node); }
    public record Walkability(double x,double y,double z,boolean walkable){}
    public record Inspection<S>(String sceneId, long revision, Path document,
                                S state, List<EditorProject.Node> hierarchy,
                                Set<Capability> capabilities, List<String> operations, List<String> tests) {}
    public record Capture(Path file, int width, int height, String renderer) {}
    @FunctionalInterface public interface Playtest { Capture run(Path destination)throws IOException; }
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
        default Map<String,Map<String,String>> operationParameters(){return Map.of();}
        void apply(String operation, Map<String,String> arguments);
        void validate();
        /** Derive metadata from actual native snapshots. Unknown hosts invalidate all dependencies. */
        default EditorChange.Details changes(S before,S after){return EditorChange.Details.full();}
        default DiskState compareSaved(S live)throws IOException{return DiskState.UNSUPPORTED;}
        default void save()throws IOException{project().save();}
        void reopen() throws IOException;
        default void didReopen(){}
        default void recoverPrevious()throws IOException{throw new UnsupportedOperationException("Previous-save recovery unavailable");}
        Capture capture(Path destination) throws IOException;
        List<String> tests();
        List<Check> test(String id);
    }
    private final Host<S> host;
    private final EditorHistory<S> history;
    private final java.util.function.Consumer<EditorChange> changed;
    private final Runnable saved;
    private EditorChange lastChange;
    private final Playtest playtest;
    public EditorAgentService(Host<S> host, EditorHistory<S> history) {
        this(host,history,()->{},()->{});
    }
    public EditorAgentService(Host<S> host, EditorHistory<S> history, Runnable changed, Runnable saved) {
        this(host,history,notification(changed),saved);
    }
    private static java.util.function.Consumer<EditorChange> notification(Runnable changed){
        Objects.requireNonNull(changed);return event->changed.run();
    }
    /** Trusted human host notification, called once after a committed transaction.
     * Consumers must not throw or perform reentrant edits. No event is emitted for rollback.
     */
    public EditorAgentService(Host<S> host, EditorHistory<S> history,
                              java.util.function.Consumer<EditorChange> changed, Runnable saved) {
        this(host,history,changed,saved,null);
    }
    public EditorAgentService(Host<S> host, EditorHistory<S> history,
                              java.util.function.Consumer<EditorChange> changed,Runnable saved,Playtest playtest){
        this.host=Objects.requireNonNull(host);this.history=Objects.requireNonNull(history);
        this.playtest=playtest;
        this.changed=Objects.requireNonNull(changed);this.saved=Objects.requireNonNull(saved);
        if(!history.isFor(host.project()))throw new IllegalArgumentException("History belongs to another scene");
    }
    private void publish(S before,long revision){
        EditorChange.Details details;
        // Metadata failure cannot conceal a committed native mutation or undo.
        try{details=Objects.requireNonNull(host.changes(before,host.project().snapshot()));}
        catch(RuntimeException failure){details=EditorChange.Details.full();}
        lastChange = new EditorChange(host.project().id(),host.project().documentPath(),revision,host.revision(),details);
        changed.accept(lastChange);
    }
    public Inspection<S> inspect() {
        capable(Capability.INSPECT);
        var p=host.project();
        return new Inspection<>(p.id(),host.revision(),p.documentPath(),p.snapshot(),
            List.copyOf(p.hierarchy()),Set.copyOf(host.capabilities()),List.copyOf(host.operations()),List.copyOf(host.tests()));
    }
    public record Summary(String sceneId, long revision, Set<Capability> capabilities,
                          List<String> operations, List<String> tests) {}
    public record SceneRevision(String sceneId,long revision){}
    public SceneRevision sceneRevision(){
        capable(Capability.INSPECT);
        return new SceneRevision(host.project().id(),host.revision());
    }
    public boolean supportsSelection(){capable(Capability.INSPECT);return host instanceof SelectionSource;}
    public boolean supportsWalkability(){capable(Capability.INSPECT);return host instanceof WalkingCollision;}
    public boolean supportsFocus(){capable(Capability.INSPECT);return host instanceof NodeFocus;}
    public Evidence<EditorProject.Node> focus(long expected,String id){
        var node=node(expected,id);
        if(!(host instanceof NodeFocus view))throw new UnsupportedOperationException("Host cannot focus hierarchy nodes");
        view.focus(node.result());require(expected);return node;
    }
    public Evidence<Walkability> walkability(long expected,double x,double y,double z){
        capable(Capability.INSPECT);require(expected);
        if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z))throw new IllegalArgumentException("Expected finite foot coordinates");
        if(host instanceof WalkingCollision runtime)return evidence(new Walkability(x,y,z,runtime.walkable(x,y,z)));
        throw new UnsupportedOperationException("Host does not provide walking collision");
    }
    public Evidence<Selection> selection(long expected){
        capable(Capability.INSPECT);require(expected);
        if(host instanceof SelectionSource source)return evidence(source.selection());
        throw new UnsupportedOperationException("Host does not expose a selection");
    }
    public record NodePage(int total, int offset, List<EditorProject.Node> nodes) {}
    public record AssetPage(int total,int offset,String catalogFingerprint,List<EditorProject.Asset> assets){}
    public record ChangePage(long beforeRevision, boolean fullScene, int total, int offset,
                             List<EditorChange.Node> nodes) {}

    /** Read the host's latest commit notification, including human edits on the shared service. */
    public Evidence<ChangePage> changes(long expected, int offset, int limit) {
        capable(Capability.INSPECT);
        require(expected);
        if (lastChange == null || lastChange.revision() != expected || !lastChange.sceneId().equals(host.project().id()))
            throw new IllegalStateException("No committed change available for this scene revision");
        var details = lastChange.details();
        var nodes = details.nodes();
        int end = pageEnd(nodes.size(),offset,limit);
        return evidence(new ChangePage(lastChange.beforeRevision(), details.fullScene(), nodes.size(), offset,
            List.copyOf(nodes.subList(offset,end))));
    }

    private static int pageEnd(int size, int offset, int limit) {
        if (offset < 0 || offset > size || limit < 1 || limit > 64)
            throw new IllegalArgumentException("Expected a valid page offset and limit 1 to 64");
        return offset + Math.min(limit,size-offset);
    }

    public Summary summary() {
        capable(Capability.INSPECT);
        return new Summary(host.project().id(), host.revision(), Set.copyOf(host.capabilities()),
                List.copyOf(host.operations()), List.copyOf(host.tests()));
    }
    public Map<String,Map<String,String>> operationParameters(){
        capable(Capability.INSPECT);
        return host.operationParameters();
    }

    public Evidence<NodePage> hierarchy(long expected, int offset, int limit) {
        capable(Capability.INSPECT);
        require(expected);
        if (offset < 0 || limit < 1 || limit > 64) throw new IllegalArgumentException("Expected offset >= 0 and limit 1 to 64");
        var project = host.project();
        var page = project.hierarchyPage(offset,limit);
        return new Evidence<>(project.id(), expected,
                new NodePage(page.total(), page.offset(), page.nodes()));
    }

    public Evidence<EditorProject.Node> node(long expected, String id) {
        capable(Capability.INSPECT);
        require(expected);
        var project = host.project();
        return new Evidence<>(project.id(), expected, project.node(id));
    }
    /** Explicit proof request. Disk comparison reads the published native document without reopening the editor. */
    public Evidence<PositionProof> verifyPosition(long expected,String id,double x,double y,double z){
        capable(Capability.INSPECT);require(expected);
        if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z))throw new IllegalArgumentException("Expected finite native coordinates");
        var project=host.project();var actual=project.node(id);
        boolean verified=actual.x()==x && actual.y()==y && actual.z()==z;
        boolean committed=lastChange!=null && lastChange.revision()==expected && lastChange.sceneId().equals(project.id()) &&
            lastChange.details().nodes().stream().anyMatch(n->n.id().equals(id) && n.kind()!=EditorChange.Kind.REMOVED);
        DiskState disk;
        try{disk=host.compareSaved(project.snapshot());}
        catch(java.nio.file.NoSuchFileException missing){disk=DiskState.MISSING;}
        catch(IOException invalid){disk=DiskState.UNREADABLE;}
        return evidence(new PositionProof(actual,verified,committed,disk));
    }
    /** Use the active project's human palette source. Imports can change it between scene edits. */
    public Evidence<AssetPage> assets(long expected,int offset,int limit){
        capable(Capability.INSPECT);
        require(expected);
        var catalog=host.project().assets();
        int end=pageEnd(catalog.size(),offset,limit);
        var signature=new StringBuilder();
        for(var asset:catalog){
            for(var field:List.of(asset.uri(),asset.category(),asset.label()))
                signature.append(field.length()).append(':').append(field);
        }
        return evidence(new AssetPage(catalog.size(),offset,EditorChange.fingerprint(signature.toString()),List.copyOf(catalog.subList(offset,end))));
    }
    /** Resolve a bounded selection against one native revision, preserving caller order. */
    public Evidence<List<EditorProject.Node>> nodes(long expected, List<String> ids){
        capable(Capability.INSPECT);
        require(expected);
        if(ids.isEmpty() || ids.size()>64)throw new IllegalArgumentException("Expected 1 to 64 hierarchy node IDs");
        var project=host.project();
        return evidence(ids.stream().map(project::node).toList());
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
        S before=host.project().snapshot();long revision=host.revision();
        history.execute(()->{operation.run();host.validate();});
        publish(before,revision);
    }
    public Evidence<Boolean> undo(long expected) {
        require(expected);capable(Capability.UNDO);S before=host.project().snapshot();
        boolean ok=history.undo();if(ok)publish(before,expected);return evidence(ok);
    }
    public Evidence<Boolean> redo(long expected) {
        require(expected);capable(Capability.REDO);S before=host.project().snapshot();
        boolean ok=history.redo();if(ok)publish(before,expected);return evidence(ok);
    }
    public Evidence<Path> save(long expected) throws IOException {
        require(expected);capable(Capability.SAVE);host.validate();host.save();saved.run();return evidence(host.project().documentPath());
    }
    /** Reopen is undoable; malformed files cannot replace the live scene. */
    public Evidence<Void> reopen(long expected) throws IOException {
        require(expected);
        capable(Capability.REOPEN);
        try {
            edit(()->{try{host.reopen();}catch(IOException failure){throw new java.io.UncheckedIOException(failure);}});
        } catch(java.io.UncheckedIOException failure) {throw failure.getCause();}
        host.didReopen();
        return evidence(null);
    }
    public Evidence<Capture> capture(long expected, Path destination) throws IOException {
        require(expected);capable(Capability.CAPTURE);return evidence(host.capture(destination));
    }
    public boolean supportsPlaytest(){return playtest!=null;}
    public Evidence<Capture> playtest(long expected,Path destination)throws IOException{
        require(expected);
        if(playtest==null)throw new UnsupportedOperationException("Host does not provide gameplay playtest");
        var result=playtest.run(destination);
        require(expected);
        return evidence(result);
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
