package org.beckenburg;

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
import javax.swing.SwingUtilities;
import org.beckenburg.editor.EditorAgentService;

/** Optional read-only access to one open editor. Every observation is made on its owner thread. */
final class EditorObservationBridge implements AutoCloseable {
    private static final int MAX_BYTES=4*1024*1024;
    private final HttpServer server;
    private final ExecutorService worker;
    private final EditorAgentService<?> service;
    private final String token;
    private volatile boolean closed;

    EditorObservationBridge(EditorAgentService<?> service)throws IOException{
        this.service=service;
        var secret=new byte[32];new SecureRandom().nextBytes(secret);
        token=Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
        server=HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"),0),8);
        worker=new ThreadPoolExecutor(1,1,0,TimeUnit.SECONDS,new ArrayBlockingQueue<>(8),task->{
            var thread=new Thread(task,"Editor observation");thread.setDaemon(true);return thread;
        });
        server.createContext("/",this::handle);server.setExecutor(worker);server.start();
    }
    String url(){return "http://127.0.0.1:"+server.getAddress().getPort();}
    String token(){return token;}
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
            if(!exchange.getRequestMethod().equals("GET")){error(exchange,405,"Only GET is supported");return;}
            String length=headers.getFirst("Content-Length");
            if(exchange.getRequestURI().toASCIIString().length()>4096 || headers.containsKey("Transfer-Encoding") ||
                (length!=null && !length.equals("0"))){
                error(exchange,400,"Invalid observation request");return;
            }
            Reply reply;
            try{
                var uri=exchange.getRequestURI();
                var params=parameters(uri.getRawQuery(),uri.getRawPath().equals("/nodes"));
                var task=new FutureTask<>(()->{
                    if(closed)throw new IllegalStateException("Observation session closed");
                    return observe(uri.getRawPath(),params);
                });
                SwingUtilities.invokeLater(task);
                try{reply=task.get(30,TimeUnit.SECONDS);}
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
    private Reply observe(String path,Query query)throws Exception{
        var summary=service.summary();
        var args=query.fields;
        if(path.equals("/summary") && args.isEmpty())
            return json(Map.of("sceneId",summary.sceneId(),"revision",summary.revision(),"tests",summary.tests(),
                "routes",List.of("/summary","/hierarchy","/node","/nodes","/changes","/assets","/test","/capture")),summary.revision());
        long revision=Long.parseLong(args.remove("revision"));
        if(revision!=summary.revision())throw new IllegalStateException("Stale revision");
        Object value;
        switch(path){
            case "/node" -> {requireKeys(args,"id");value=service.node(revision,args.get("id"));}
            case "/nodes" -> {requireKeys(args);value=service.nodes(revision,query.ids);}
            case "/changes" -> {requireKeys(args,"offset","limit");value=service.changes(revision,Integer.parseInt(args.get("offset")),Integer.parseInt(args.get("limit")));}
            case "/assets" -> {requireKeys(args,"offset","limit");value=service.assets(revision,Integer.parseInt(args.get("offset")),Integer.parseInt(args.get("limit")));}
            case "/hierarchy" -> {requireKeys(args,"offset","limit");value=service.hierarchy(revision,Integer.parseInt(args.get("offset")),Integer.parseInt(args.get("limit")));}
            case "/test" -> {requireKeys(args,"id");value=service.test(revision,args.get("id"));}
            case "/capture" -> {
                requireKeys(args);
                Path file=Files.createTempFile("editor-observation-",".png");
                try{
                    service.capture(revision,file);
                    if(Files.size(file)>MAX_BYTES)throw new IOException("Capture exceeds limit");
                    return new Reply("image/png",Files.readAllBytes(file),revision);
                }finally{Files.deleteIfExists(file);}
            }
            default -> throw new IllegalArgumentException("Unknown observation");
        }
        return json(value,revision);
    }
    private static void requireKeys(Map<String,String> args,String... keys){
        if(!args.keySet().equals(Set.of(keys)))throw new IllegalArgumentException();
    }
    private static Reply json(Object value,long revision)throws ReflectiveOperationException{
        return new Reply("application/json",EditorAgentCli.json(value).getBytes(StandardCharsets.UTF_8),revision);
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
