package org.beckenburg.editor;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.*;

/** Session-scoped access to one open editor. Every service call runs on its owner thread. */
public final class EditorObservationBridge implements AutoCloseable {
    public enum Scope { OBSERVE, EDIT, EDIT_AND_SAVE }
    private final Scope scope;
    private final Executor owner;
    private final Set<EditorAgentService.Capability> capabilities;
    private final boolean selection;
    private static final int MAX_BYTES=4*1024*1024;
    private final HttpServer server;
    private final ExecutorService worker;
    private final EditorAgentService<?> service;
    private final String token;
    private volatile boolean closed;

    /** Construct on the owner thread and dispatch all later requests through that host's executor. */
    public EditorObservationBridge(EditorAgentService<?> service,Scope scope,Executor owner)throws IOException{
        this.service=Objects.requireNonNull(service);this.scope=Objects.requireNonNull(scope);this.owner=Objects.requireNonNull(owner);
        capabilities=Set.copyOf(service.summary().capabilities());
        selection=service.supportsSelection();
        var secret=new byte[32];new SecureRandom().nextBytes(secret);
        token=Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
        server=HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"),0),8);
        worker=new ThreadPoolExecutor(1,1,0,TimeUnit.SECONDS,new ArrayBlockingQueue<>(8),task->{
            var thread=new Thread(task,"Editor observation");thread.setDaemon(true);return thread;
        });
        server.createContext("/",this::handle);server.setExecutor(worker);server.start();
    }
    public String url(){return "http://127.0.0.1:"+server.getAddress().getPort();}
    public String token(){return token;}
    @Override public void close(){closed=true;server.stop(0);worker.shutdownNow();}

    private void handle(HttpExchange exchange)throws IOException{
        try(exchange){
            var headers=exchange.getRequestHeaders();
            String auth=headers.getFirst("Authorization");
            if(auth==null || !MessageDigest.isEqual(("Bearer "+token).getBytes(StandardCharsets.UTF_8),auth.getBytes(StandardCharsets.UTF_8))){
                error(exchange,401,"Bearer token required");return;
            }
            if(!("127.0.0.1:"+server.getAddress().getPort()).equals(headers.getFirst("Host")) || headers.containsKey("Origin")){
                error(exchange,403,"Local client required");return;
            }
            boolean write=exchange.getRequestMethod().equals("POST");
            if(!exchange.getRequestMethod().equals("GET") && !(write && scope!=Scope.OBSERVE)){
                error(exchange,405,"Method unavailable in this session");return;
            }
            String path=exchange.getRequestURI().getRawPath();
            if(write && !writeRoutes().contains(path)){error(exchange,403,"Action outside session scope");return;}
            String length=headers.getFirst("Content-Length");
            if(exchange.getRequestURI().toASCIIString().length()>4096 || headers.containsKey("Transfer-Encoding") ||
                (length!=null && (!length.matches("[0-9]{1,5}") || Integer.parseInt(length)>(write?16384:0)))){
                error(exchange,400,"Invalid observation request");return;
            }
            Reply reply;
            try{
                var uri=exchange.getRequestURI();
                byte[] body=write?exchange.getRequestBody().readNBytes(16385):new byte[0];
                if(body.length>16384)throw new IllegalArgumentException();
                var params=parameters(uri.getRawQuery(),uri.getRawPath().equals("/nodes"));
                var task=new FutureTask<>(()->{
                    if(closed)throw new IllegalStateException("Observation session closed");
                    return write?mutate(path,params.fields,body):observe(path,params);
                });
                owner.execute(task);
                try{reply=write?task.get():task.get(30,TimeUnit.SECONDS);}
                finally{task.cancel(false);}
            }catch(ExecutionException failure){
                var cause=failure.getCause();
                if(cause instanceof IllegalStateException)error(exchange,409,"Editor revision or session changed");
                else if(cause instanceof IllegalArgumentException)error(exchange,400,"Invalid observation request");
                else error(exchange,500,"Observation failed");
                return;
            }catch(IllegalArgumentException invalid){error(exchange,400,"Invalid observation request");return;}
            catch(TimeoutException timeout){error(exchange,503,"Editor busy");return;}
            catch(InterruptedException interrupted){Thread.currentThread().interrupt();return;}
            if(reply.bytes.length>MAX_BYTES){error(exchange,413,"Observation exceeds response limit");return;}
            if(reply.revision!=null)exchange.getResponseHeaders().set("X-Editor-Revision",reply.revision.toString());
            send(exchange,200,reply.type,reply.bytes);
        }
    }
    private record Query(Map<String,String> fields,List<String> ids){}
    private static Query parameters(String query,boolean multi){
        var result=new HashMap<String,String>();
        var ids=new ArrayList<String>();
        if(query!=null)for(String pair:query.split("&",-1)){
            int i=pair.indexOf('=');
            if(i<=0)throw new IllegalArgumentException();
            String key=pair.substring(0,i),value=URLDecoder.decode(pair.substring(i+1),StandardCharsets.UTF_8);
            if(multi && key.equals("id"))ids.add(value);
            else if(result.put(key,value)!=null)throw new IllegalArgumentException();
        }
        return new Query(result,ids);
    }
    private record Reply(String type,byte[] bytes,Long revision){}
    private List<String> writeRoutes(){
        if(scope==Scope.OBSERVE)return List.of();
        var routes=new ArrayList<String>();
        if(service.supportsPlaytest())routes.add("/playtest");
        if(capabilities.contains(EditorAgentService.Capability.TRANSACT))routes.add("/edit");
        if(capabilities.contains(EditorAgentService.Capability.UNDO))routes.add("/undo");
        if(capabilities.contains(EditorAgentService.Capability.REDO))routes.add("/redo");
        if(scope==Scope.EDIT_AND_SAVE){
            if(capabilities.contains(EditorAgentService.Capability.SAVE))routes.add("/save");
            if(capabilities.contains(EditorAgentService.Capability.REOPEN))routes.add("/reopen");
        }
        return routes;
    }
    private List<String> readRoutes(){
        var routes=new ArrayList<>(List.of("/summary","/revision","/hierarchy","/node","/nodes","/changes","/assets","/verify-position"));
        if(selection)routes.add("/selection");
        if(capabilities.contains(EditorAgentService.Capability.TEST))routes.add("/test");
        if(capabilities.contains(EditorAgentService.Capability.CAPTURE))routes.add("/capture");
        return routes;
    }
    private Reply mutate(String path,Map<String,String> args,byte[] body)throws Exception{
        requireKeys(args,"revision");long revision=Long.parseLong(args.get("revision"));
        if(!path.equals("/edit") && body.length!=0)throw new IllegalArgumentException();
        EditorAgentService.Evidence<?> result;
        switch(path){
            case "/playtest" -> {return image(revision,true);}
            case "/edit" -> {
                String batch=StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(body)).toString();
                if(batch.contains("\r")||batch.contains("\n"))throw new IllegalArgumentException();
                result=service.transact(revision,EditorAgentProtocol.parseBatch(batch.split("\t",-1),0));
            }
            case "/undo" -> result=service.undo(revision);
            case "/redo" -> result=service.redo(revision);
            case "/save" -> result=service.save(revision);
            case "/reopen" -> result=service.reopen(revision);
            default -> throw new IllegalArgumentException();
        }
        return json(result,result.revision());
    }
    private Reply observe(String path,Query query)throws Exception{
        var args=query.fields;
        if(path.equals("/summary") && args.isEmpty()){
            var summary=service.summary();
            return json(Map.of("sceneId",summary.sceneId(),"revision",summary.revision(),
                "tests",capabilities.contains(EditorAgentService.Capability.TEST)?summary.tests():List.of(),
                "routes",readRoutes(),
                "writeRoutes",writeRoutes(),"operations",writeRoutes().contains("/edit")?service.operationParameters():Map.of()),summary.revision());
        }
        var current=service.sceneRevision();
        if(path.equals("/revision")){
            requireKeys(args);
            return json(current,current.revision());
        }
        long revision=Long.parseLong(args.remove("revision"));
        if(revision!=current.revision())throw new IllegalStateException("Stale revision");
        Object value;
        switch(path){
            case "/selection" -> {requireKeys(args);if(!selection)throw new IllegalArgumentException();value=service.selection(revision);}
            case "/node" -> {requireKeys(args,"id");value=service.node(revision,args.get("id"));}
            case "/verify-position" -> {
                requireKeys(args,"id","x","y","z");
                value=service.verifyPosition(revision,args.get("id"),Double.parseDouble(args.get("x")),Double.parseDouble(args.get("y")),Double.parseDouble(args.get("z")));
            }
            case "/nodes" -> {requireKeys(args);value=service.nodes(revision,query.ids);}
            case "/changes" -> {requireKeys(args,"offset","limit");value=service.changes(revision,Integer.parseInt(args.get("offset")),Integer.parseInt(args.get("limit")));}
            case "/assets" -> {requireKeys(args,"offset","limit");value=service.assets(revision,Integer.parseInt(args.get("offset")),Integer.parseInt(args.get("limit")));}
            case "/hierarchy" -> {requireKeys(args,"offset","limit");value=service.hierarchy(revision,Integer.parseInt(args.get("offset")),Integer.parseInt(args.get("limit")));}
            case "/test" -> {requireKeys(args,"id");value=service.test(revision,args.get("id"));}
            case "/capture" -> {
                requireKeys(args);
                return image(revision,false);
            }
            default -> throw new IllegalArgumentException("Unknown observation");
        }
        return json(value,revision);
    }
    private static void requireKeys(Map<String,String> args,String... keys){
        if(!args.keySet().equals(Set.of(keys)))throw new IllegalArgumentException();
    }
    private Reply image(long revision,boolean gameplay)throws IOException{
        Path file=Files.createTempFile("editor-observation-",".png");
        try{
            if(gameplay)service.playtest(revision,file);else service.capture(revision,file);
            if(Files.size(file)>MAX_BYTES)throw new IOException("Capture exceeds limit");
            return new Reply("image/png",Files.readAllBytes(file),revision);
        }finally{Files.deleteIfExists(file);}
    }
    private static Reply json(Object value,long revision)throws ReflectiveOperationException{
        return new Reply("application/json",EditorAgentProtocol.json(value).getBytes(StandardCharsets.UTF_8),revision);
    }
    private static void error(HttpExchange exchange,int status,String message)throws IOException{
        send(exchange,status,"application/json",("{\"error\":\""+message+"\"}").getBytes(StandardCharsets.UTF_8));
    }
    private static void send(HttpExchange exchange,int status,String type,byte[] bytes)throws IOException{
        exchange.getResponseHeaders().set("Content-Type",type);
        exchange.getResponseHeaders().set("Cache-Control","no-store");
        exchange.getResponseHeaders().set("X-Content-Type-Options","nosniff");
        exchange.sendResponseHeaders(status,bytes.length);exchange.getResponseBody().write(bytes);
    }
}
