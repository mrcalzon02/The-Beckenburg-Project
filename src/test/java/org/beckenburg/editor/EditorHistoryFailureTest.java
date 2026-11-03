package org.beckenburg.editor;

import java.nio.file.Path;
import java.util.List;

/** A host restore can fail; failure must not consume undo/redo or hide its cause. */
public final class EditorHistoryFailureTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static final class Project implements EditorProject<Integer>{
        int state=4;boolean fail;
        public String id(){return "restore-failure-contract";}
        public String title(){return id();}
        public Path documentPath(){return Path.of("unused-editor-history-fixture").toAbsolutePath();}
        public List<Node> hierarchy(){return List.of();}
        public List<Asset> assets(){return List.of();}
        public Integer snapshot(){return state;}
        public void restore(Integer value){if(fail){fail=false;state=999;throw new IllegalStateException("Host restore temporarily unavailable");}state=value;}
        public void save(){}
    }
    public static void main(String[] args){
        var project=new Project();var history=new EditorHistory<>(project,3);history.execute(()->project.state=5);
        project.fail=true;
        try{history.undo();throw new AssertionError("Restore failure hidden");}catch(IllegalStateException expected){}
        check(project.state==5&&history.canUndo()&&!history.canRedo(),"Failed undo rolls back partial host state and retains original history stacks");
        check(history.undo()&&project.state==4,"Retry uses retained undo target");project.fail=true;
        try{history.redo();throw new AssertionError("Redo restore failure hidden");}catch(IllegalStateException expected){}
        check(project.state==4&&!history.canUndo()&&history.canRedo(),"Failed redo preserves both stacks and source state");
        check(history.redo()&&project.state==5,"Retry uses retained redo target");
        var original=new IllegalArgumentException("Original command failure");
        try{history.execute(()->{project.state=6;project.fail=true;throw original;});throw new AssertionError("Command failure hidden");}
        catch(IllegalArgumentException failure){check(failure==original&&failure.getSuppressed().length==1,"Rollback failure remains attached to original command error");}
        check(history.canUndo()&&!history.canRedo(),"Failed command cannot push or discard history");
        System.out.println("PASS editor history failures: partial restore rollback, retained undo/redo targets, retry and original/suppressed failure provenance");
    }
}
