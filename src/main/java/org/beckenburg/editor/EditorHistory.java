package org.beckenburg.editor;

import java.util.ArrayDeque;
import java.util.Objects;

/** Project-independent bounded transactional undo/redo.
 * Failed commands restore their before-state instead of leaving half an edit.
 */
public final class EditorHistory<S> {
    private final EditorProject<S> project;
    private final ArrayDeque<S> undo=new ArrayDeque<>(),redo=new ArrayDeque<>();
    private final int capacity;
    public EditorHistory(EditorProject<S> project,int capacity){
        this.project=Objects.requireNonNull(project);
        if(capacity<1)throw new IllegalArgumentException("History must be positive");
        this.capacity=capacity;
    }
    public void execute(Runnable command){
        Objects.requireNonNull(command);
        S before=project.snapshot();
        try{command.run();}
        catch(RuntimeException|Error failure){rollback(before,failure);throw failure;}
        undo.push(before);
        if(undo.size()>capacity)undo.removeLast();
        redo.clear();
    }
    public boolean canUndo(){return !undo.isEmpty();}
    public boolean isFor(EditorProject<?> candidate){return project==candidate;}
    public boolean canRedo(){return !redo.isEmpty();}
    private void rollback(S before,Throwable failure){
        try{project.restore(before);}catch(RuntimeException|Error rollbackFailure){if(rollbackFailure!=failure)failure.addSuppressed(rollbackFailure);}
    }
    public boolean undo(){
        if(undo.isEmpty())return false;
        S before=project.snapshot();
        try{project.restore(undo.peek());}catch(RuntimeException|Error failure){rollback(before,failure);throw failure;}
        undo.pop();redo.push(before);return true;
    }
    public boolean redo(){
        if(redo.isEmpty())return false;
        S before=project.snapshot();
        try{project.restore(redo.peek());}catch(RuntimeException|Error failure){rollback(before,failure);throw failure;}
        redo.pop();undo.push(before);return true;
    }
}
