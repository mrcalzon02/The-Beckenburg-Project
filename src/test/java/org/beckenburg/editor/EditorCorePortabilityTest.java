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
        private long revision;
        private final Path file;
        OtherGame(Path file){this.file=file;}
        public String id(){return "another-game";}
        public String title(){return "Other Game";}
        public Path documentPath(){return file;}
        public List<Node> hierarchy(){return List.of(new Node("room","FLOOR","Room",state,0,5));}
        public List<Asset> assets(){
            return List.of(new Asset("assets://models/other-game-room","Models","Other Room"));
        }
        public Integer snapshot(){return state;}
        public void restore(Integer value){state=value;revision++;}
        public void save()throws IOException{Files.writeString(file,Integer.toString(state));}
    }
    private static void mustReject(Runnable invalid){
        boolean caught=false;
        try{invalid.run();}catch(IllegalArgumentException expected){caught=true;}
        check(caught,"Unsafe or invalid project asset URI should be rejected");
    }
    public static void main(String[] args)throws Exception{
        verifyReceiptRetention();
        Path folder=Files.createTempDirectory("portable-editor-");
        try{
            OtherGame project=new OtherGame(folder.resolve("map.txt"));
            check(project.hierarchy().size()==1,"Generic project scene nodes");
            check(project.assets().get(0).uri().equals("assets://models/other-game-room"),
                "Project-controlled asset catalog");
            var history=new EditorHistory<>(project,3);
            history.execute(()->project.restore(8));
            history.execute(()->project.restore(9));
            check(history.undo()&&project.state==8,"Generic undo");
            check(history.redo()&&project.state==9,"Generic redo");
            var events=new java.util.ArrayList<EditorChange>();
            var owner=new java.util.concurrent.atomic.AtomicReference<>(Thread.currentThread());
            var service=new EditorAgentService<>(new EditorAgentService.Host<Integer>(){
                public EditorProject<Integer> project(){return project;}
                public long revision(){return project.revision;}
                public void checkAccess(){if(Thread.currentThread()!=owner.get())throw new IllegalStateException("Wrong project thread");}
                public java.util.Set<EditorAgentService.Capability> capabilities(){
                    return java.util.EnumSet.complementOf(java.util.EnumSet.of(EditorAgentService.Capability.CAPTURE,EditorAgentService.Capability.RECOVER_PREVIOUS));
                }
                public List<String> operations(){return List.of("increment");}
                public java.util.Map<String,java.util.Map<String,String>> operationParameters(){return java.util.Map.of("increment",java.util.Map.of());}
                public void apply(String operation,java.util.Map<String,String> arguments){project.restore(project.state+1);}
                public EditorChange.Details changes(Integer before,Integer after){throw new IllegalStateException("Metadata unavailable");}
                public void validate(){if(project.state<0)throw new IllegalArgumentException("Negative state");}
                public void reopen()throws IOException{project.restore(Integer.parseInt(Files.readString(project.documentPath())));}
                public EditorAgentService.Capture capture(Path destination){throw new UnsupportedOperationException("No renderer");}
                public List<String> tests(){return List.of("positive");}
                public List<EditorAgentService.Check> test(String id){return List.of(new EditorAgentService.Check("POSITIVE",project.state>0,"Project state"));}
            },history,events::add,()->{});
            service.transact(project.revision,"increment",java.util.Map.of());
            check(project.state==10&&service.inspect().state()==10,"Portable service edits host-owned data");
            check(events.size()==1&&events.get(0).details().fullScene()&&events.get(0).revision()==project.revision,
                "Metadata failure conservatively invalidates committed project, without concealing mutation");
            service.undo(project.revision);check(project.state==9,"Agent and human share portable history");
            boolean unavailable=false;
            try{service.capture(project.revision,folder.resolve("fake.png"));}catch(UnsupportedOperationException expected){unavailable=true;}
            check(unavailable&&!Files.exists(folder.resolve("fake.png")),"Host without renderer cannot fabricate capture");
            boolean failed=false;
            try{history.execute(()->{project.state=999;throw new IllegalStateException("bad");});}
            catch(IllegalStateException expected){failed=true;}
            check(failed&&project.state==9,"Failed operation is rolled back");
            project.save();
            check(Files.readString(project.documentPath()).equals("9"),"Adapter owns serialization");
            verifyTransports(service,project,owner);
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
    private static void verifyTransports(EditorAgentService<Integer> service,OtherGame project,
            java.util.concurrent.atomic.AtomicReference<Thread> owner)throws Exception{
        long rev=project.revision;
        String requests="edit\t"+rev+"\tincrement\nnode\t"+(rev+1)+"\troom\nsave\t"+(rev+1)+"\nundo\t"+(rev+1)+
            "\nreopen\t"+(rev+2)+"\nnode\t"+rev+"\troom\n";
        var output=new java.io.ByteArrayOutputStream();
        int code=EditorAgentProtocol.run(service,new java.io.BufferedReader(new java.io.StringReader(requests)),
            new java.io.PrintStream(output,true,java.nio.charset.StandardCharsets.UTF_8));
        String[] lines=output.toString(java.nio.charset.StandardCharsets.UTF_8).split("\\R");
        check(code==1 && lines.length==6 && lines[1].contains("\"x\":10.0") && lines[5].contains("Stale scene revision"),"Shared headless wire readback and stale refusal");
        check(project.state==10 && Files.readString(project.file).equals("10"),"Portable protocol reopened saved state");
        var executor=java.util.concurrent.Executors.newSingleThreadExecutor();
        try(var bridge=executor.submit(()->{
            owner.set(Thread.currentThread());
            return new EditorObservationBridge(service,EditorObservationBridge.Scope.EDIT,executor);
        }).get()){
            var client=java.net.http.HttpClient.newHttpClient();
            var request=java.net.http.HttpRequest.newBuilder().header("Authorization","Bearer "+bridge.token());
            var summary=client.send(request.uri(java.net.URI.create(bridge.url()+"/summary")).GET().build(),
                java.net.http.HttpResponse.BodyHandlers.ofString());
            check(summary.statusCode()==200 && !summary.body().contains("/capture") && !summary.body().contains("/save"),"Unsupported capture and unauthorized save absent");
            var edited=client.send(request.uri(java.net.URI.create(bridge.url()+"/edit?revision="+project.revision))
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString("increment")).build(),java.net.http.HttpResponse.BodyHandlers.ofString());
            check(edited.statusCode()==200 && project.state==11,"Same native object edited through reusable loopback transport");
            String python=System.getenv("EDITOR_MCP_PYTHON");
            if(python!=null){
                var clientProcess=new ProcessBuilder(python,"tools/verify_editor_mcp.py","portable").inheritIO();
                clientProcess.environment().put("EDITOR_AGENT_URL",bridge.url());
                clientProcess.environment().put("EDITOR_AGENT_TOKEN",bridge.token());
                var process=clientProcess.start();
                try{
                    check(process.waitFor(60,java.util.concurrent.TimeUnit.SECONDS) && process.exitValue()==0 && project.state==11,
                        "Independent MCP client used this project and its history");
                }finally{if(process.isAlive())process.destroyForcibly();}
            }
        }finally{executor.shutdownNow();owner.set(Thread.currentThread());}
    }
    private static void verifyReceiptRetention(){
        int[] calls={0};
        var receipts=new EditorRequestJournal<String>(String::length,2,5);
        for(int sequence:new int[]{2,1,3}){
            String response=receipts.execute(sequence,"test-"+sequence,()->{calls[0]++;return "ok";});
            check(response.equals("ok"),"Accepted out-of-order sequence in the active window");
        }
        check(receipts.execute(2,"test-2",()->"wrong").equals("ok"),"Retained exact receipt");
        try{receipts.execute(1,"test-1",()->"wrong");throw new AssertionError("Retired sequence repeated");}
        catch(EditorRequestJournal.Conflict expected){}
        receipts.execute(4,"large",()->{calls[0]++;return "oversized";});
        try{receipts.execute(4,"large",()->"wrong");throw new AssertionError("Evicted large receipt repeated");}
        catch(EditorRequestJournal.Conflict expected){}
        check(calls[0]==4,"Only original calls executed");
    }
}
