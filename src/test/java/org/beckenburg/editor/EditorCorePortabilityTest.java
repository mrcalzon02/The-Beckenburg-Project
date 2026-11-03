package org.beckenburg.editor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Standalone project with no Beckenburg classes, proving reusable editor API. */
public final class EditorCorePortabilityTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static final class OtherGame implements EditorProject<Integer>{
        private int state=4;
        private final Path file;
        OtherGame(Path file){this.file=file;}
        public String id(){return "another-game";}
        public String title(){return "Other Game";}
        public Path documentPath(){return file;}
        public List<Node> hierarchy(){return List.of(new Node("room","FLOOR","Room",2,0,5));}
        public List<Asset> assets(){
            return List.of(new Asset("assets://models/other-game-room","Models","Other Room"));
        }
        public Integer snapshot(){return state;}
        public void restore(Integer value){state=value;}
        public void save()throws IOException{Files.writeString(file,Integer.toString(state));}
    }
    private static void mustReject(Runnable invalid){
        boolean caught=false;
        try{invalid.run();}catch(IllegalArgumentException expected){caught=true;}
        check(caught,"Unsafe or invalid project asset URI should be rejected");
    }
    public static void main(String[] args)throws Exception{
        Path folder=Files.createTempDirectory("portable-editor-");
        try{
            OtherGame project=new OtherGame(folder.resolve("map.txt"));
            check(project.hierarchy().size()==1,"Generic project scene nodes");
            check(project.assets().get(0).uri().equals("assets://models/other-game-room"),
                "Project-controlled asset catalog");
            var history=new EditorHistory<>(project,3);
            history.execute(()->project.state=8);
            history.execute(()->project.state=9);
            check(history.undo()&&project.state==8,"Generic undo");
            check(history.redo()&&project.state==9,"Generic redo");
            var events=new java.util.ArrayList<EditorChange>();
            var service=new EditorAgentService<>(new EditorAgentService.Host<Integer>(){
                public EditorProject<Integer> project(){return project;}
                public long revision(){return project.state;}
                public void checkAccess(){}
                public java.util.Set<EditorAgentService.Capability> capabilities(){
                    return java.util.EnumSet.complementOf(java.util.EnumSet.of(EditorAgentService.Capability.CAPTURE,EditorAgentService.Capability.RECOVER_PREVIOUS));
                }
                public List<String> operations(){return List.of("increment");}
                public void apply(String operation,java.util.Map<String,String> arguments){project.state++;}
                public EditorChange.Details changes(Integer before,Integer after){throw new IllegalStateException("Metadata unavailable");}
                public void validate(){if(project.state<0)throw new IllegalArgumentException("Negative state");}
                public void reopen()throws IOException{project.state=Integer.parseInt(Files.readString(project.documentPath()));}
                public EditorAgentService.Capture capture(Path destination){throw new UnsupportedOperationException("No renderer");}
                public List<String> tests(){return List.of("positive");}
                public List<EditorAgentService.Check> test(String id){return List.of(new EditorAgentService.Check("POSITIVE",project.state>0,"Project state"));}
            },history,events::add,()->{});
            service.transact(9,"increment",java.util.Map.of());
            check(project.state==10&&service.inspect().state()==10,"Portable service edits host-owned data");
            check(events.size()==1&&events.get(0).details().fullScene()&&events.get(0).revision()==10,
                "Metadata failure conservatively invalidates committed project, without concealing mutation");
            service.undo(10);check(project.state==9,"Agent and human share portable history");
            boolean unavailable=false;
            try{service.capture(9,folder.resolve("fake.png"));}catch(UnsupportedOperationException expected){unavailable=true;}
            check(unavailable&&!Files.exists(folder.resolve("fake.png")),"Host without renderer cannot fabricate capture");
            boolean failed=false;
            try{history.execute(()->{project.state=999;throw new IllegalStateException("bad");});}
            catch(IllegalStateException expected){failed=true;}
            check(failed&&project.state==9,"Failed operation is rolled back");
            project.save();
            check(Files.readString(project.documentPath()).equals("9"),"Adapter owns serialization");
            Path root=Files.createDirectory(folder.resolve("textures"));
            Files.writeString(root.resolve("stone.txt"),"brick");
            var registry=new EditorAssets.Registry().register("assets",EditorAssets.filesystem(root));
            try(var stream=registry.open("assets://stone.txt")){
                check(new String(stream.readAllBytes()).equals("brick"),"External loose asset provider");
            }
            mustReject(()->EditorAssets.validateUri("assets://../secrets"));
            mustReject(()->EditorAssets.validateUri("assets:///absolute"));
            mustReject(()->EditorAssets.validateUri("https://example.com/hi?x=y"));
            check(EditorTool.values().length>=11,"Portable architecture tools");
            System.out.println("PASS portable editor project adapters, history, asset routing and tools");
        }finally{
            try(var stream=Files.walk(folder)){stream.sorted(java.util.Comparator.reverseOrder()).forEach(p->{
                try{Files.deleteIfExists(p);}catch(IOException ignored){}
            });}
        }
    }
}
