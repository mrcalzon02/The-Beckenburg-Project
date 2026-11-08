package org.beckenburg.editor;

import java.util.ArrayDeque;
import java.util.Objects;

/** Project-independent bounded transactional undo/redo.
 * Failed commands restore their before-state instead of leaving half an edit.
 */
public final class EditorHistory<S> {
    private final EditorProject<S> project;
    private record Entry<S>(S state,long bytes) {}
    private final ArrayDeque<Entry<S>> undo=new ArrayDeque<>(),redo=new ArrayDeque<>();
    private final int capacity;
    private final long budget;
    private final java.util.function.ToLongBiFunction<S,S> cost;
    private long retainedBytes;
    public EditorHistory(EditorProject<S> project,int capacity){
        this(project,capacity,Long.MAX_VALUE,null);
    }
    /** Cost is measured on each successful state transition and follows it between undo and redo. */
    public EditorHistory(EditorProject<S> project,int capacity,long budget,java.util.function.ToLongBiFunction<S,S> cost){
        this.project=Objects.requireNonNull(project);
        if(capacity<1 || budget<1)throw new IllegalArgumentException("History capacity and budget must be positive");
        if(budget!=Long.MAX_VALUE && cost==null)throw new IllegalArgumentException("A finite undo budget requires a cost function");
        this.capacity=capacity;
        this.budget=budget;
        this.cost=cost;
    }
    public void execute(Runnable command){
        Objects.requireNonNull(command);
        S before=project.snapshot();
        long bytes;
        try{
            command.run();
            bytes=cost==null?0:cost.applyAsLong(before,project.snapshot());
            if(bytes<0 || bytes>budget)throw new IllegalArgumentException("Edit exceeds undo byte budget");
        }
        catch(RuntimeException|Error failure){rollback(before,failure);throw failure;}
        for(var entry:redo)retainedBytes-=entry.bytes();
        redo.clear();
        while(undo.size()>=capacity || retainedBytes>budget-bytes)retainedBytes-=undo.removeLast().bytes();
        undo.push(new Entry<>(before,bytes));
        retainedBytes+=bytes;
    }
    public long retainedBytes(){return retainedBytes;}
    public boolean canUndo(){return !undo.isEmpty();}
    public boolean isFor(EditorProject<?> candidate){return project==candidate;}
    public boolean canRedo(){return !redo.isEmpty();}
    private void rollback(S before,Throwable failure){
        try{project.restore(before);}catch(RuntimeException|Error rollbackFailure){if(rollbackFailure!=failure)failure.addSuppressed(rollbackFailure);}
    }
    public boolean undo(){
        if(undo.isEmpty())return false;
        S before=project.snapshot();
        var entry=undo.peek();
        try{project.restore(entry.state());}catch(RuntimeException|Error failure){rollback(before,failure);throw failure;}
        undo.pop();redo.push(new Entry<>(before,entry.bytes()));return true;
    }
    public boolean redo(){
        if(redo.isEmpty())return false;
        S before=project.snapshot();
        var entry=redo.peek();
        try{project.restore(entry.state());}catch(RuntimeException|Error failure){rollback(before,failure);throw failure;}
        redo.pop();undo.push(new Entry<>(before,entry.bytes()));return true;
    }
}
