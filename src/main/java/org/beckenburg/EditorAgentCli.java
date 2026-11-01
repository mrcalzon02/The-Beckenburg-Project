package org.beckenburg;

import org.beckenburg.editor.EditorAgentService;
import org.beckenburg.editor.EditorHistory;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Opt-in, process-local headless adapter. No listener, shell or script execution.
 * Tab-separated requests on stdin; one JSON response per line on stdout.
 */
public final class EditorAgentCli {
    private EditorAgentCli(){}
    public static void main(String[] args)throws Exception{
        if(args.length!=2)throw new IllegalArgumentException("Usage: EditorAgentCli CASTLE|HOUSE|SCHOOL|... project-home");
        System.setProperty("user.home",Path.of(args[1]).toAbsolutePath().normalize().toString());
        System.setProperty("java.awt.headless","true");
        PrintStream responses=System.out;
        // Runtime diagnostics must never corrupt the JSON response stream.
        System.setOut(System.err);
        Thread owner=Thread.currentThread();
        Runnable access=()->{if(Thread.currentThread()!=owner)throw new IllegalStateException("Wrong runtime owner thread");};
        SceneRenderer renderer=new SceneRenderer();renderer.setRenderSize(560,350);renderer.lamp=false;
        EditorAgentService<?> service;
        if(args[0].equalsIgnoreCase("CASTLE")){
            var scene=CastleLevelEdits.current();var project=new CastleEditorProject(scene);
            double[] spawn=scene.spawn();renderer.x=spawn[0];renderer.z=spawn[1];renderer.eyeHeight=1.62;
            renderer.yaw=spawn[2];renderer.pitch=-.12;
            service=new EditorAgentService<>(new CastleEditorAgent(project,new CastleHubMap(),renderer,access),
                new EditorHistory<>(project,60));
        }else{
            var site=HouseMap.Site.valueOf(args[0].toUpperCase(java.util.Locale.ROOT));
            var session=new LevelAuthoringSession(site);
            var spawn=session.level().room(0).spawn();
            renderer.x=spawn.x();renderer.z=spawn.z();renderer.eyeHeight=spawn.y()+1.62;renderer.pitch=-.12;
            service=new EditorAgentService<>(new LevelEditorAgent(session,()->new HouseMap(site,session.level()),renderer,access),
                new EditorHistory<>(session,60));
        }
        int code=run(service,new BufferedReader(new InputStreamReader(System.in,StandardCharsets.UTF_8)),responses);
        if(code!=0)System.exit(code);
    }
    static int run(EditorAgentService<?> service,BufferedReader requests,PrintStream responses)throws Exception{
        boolean failed=false;
        for(String line;(line=requests.readLine())!=null;){
            try{
                if(line.length()>16384)throw new IllegalArgumentException("Request too large");
                String[] parts=line.split("\t",-1);String command=parts[0];Object result;
                if(command.equals("inspect")){
                    if(parts.length!=1)throw new IllegalArgumentException("inspect takes no arguments");
                    result=service.inspect();
                }else{
                    if(parts.length<2)throw new IllegalArgumentException("Expected scene revision");
                    long revision=Long.parseLong(parts[1]);
                    int expected=switch(command){case "edit"->-1;case "capture","test"->3;default->2;};
                    if((expected>=0&&parts.length!=expected)||(expected<0&&parts.length<3))
                        throw new IllegalArgumentException("Wrong argument count");
                    result=switch(command){
                        case "edit" -> {
                            Map<String,String> arguments=new LinkedHashMap<>();
                            for(int i=3;i<parts.length;i++){
                                int split=parts[i].indexOf('=');
                                if(split<1||arguments.putIfAbsent(parts[i].substring(0,split),parts[i].substring(split+1))!=null)
                                    throw new IllegalArgumentException("Expected unique key=value arguments");
                            }
                            yield service.transact(revision,parts[2],arguments);
                        }
                        case "undo" -> service.undo(revision);
                        case "redo" -> service.redo(revision);
                        case "save" -> service.save(revision);
                        case "reopen" -> service.reopen(revision);
                        case "recover" -> service.recoverPrevious(revision);
                        case "capture" -> service.capture(revision,Path.of(parts[2]));
                        case "test" -> service.test(revision,parts[2]);
                        default -> throw new IllegalArgumentException("Unknown command: "+command);
                    };
                    if(result instanceof EditorAgentService.Evidence<?> e && e.result() instanceof java.util.List<?> list
                        && list.stream().anyMatch(item->item instanceof EditorAgentService.Check check&&!check.passed()))failed=true;
                }
                responses.println(json(Map.of("ok",true,"result",result)));
            }catch(Exception failure){
                failed=true;responses.println(json(Map.of("ok",false,"error",failure.toString())));
            }
            responses.flush();
        }
        return failed?1:0;
    }
    /** Minimal output-only JSON encoder for data records, not executable input. */
    static String json(Object value)throws ReflectiveOperationException{
        if(value==null)return "null";
        if(value instanceof Path)value=value.toString();
        if(value instanceof Boolean||value instanceof Number)return value.toString();
        if(value instanceof Map<?,?> map){
            var parts=new java.util.ArrayList<String>();
            for(var entry:map.entrySet())parts.add(json(entry.getKey().toString())+":"+json(entry.getValue()));
            return "{"+String.join(",",parts)+"}";
        }
        if(value instanceof Iterable<?> list){
            var parts=new java.util.ArrayList<String>();for(var item:list)parts.add(json(item));
            return "["+String.join(",",parts)+"]";
        }
        if(value.getClass().isArray()){
            var parts=new java.util.ArrayList<String>();
            for(int i=0;i<java.lang.reflect.Array.getLength(value);i++)parts.add(json(java.lang.reflect.Array.get(value,i)));
            return "["+String.join(",",parts)+"]";
        }
        if(value.getClass().isRecord()){
            Map<String,Object> fields=new LinkedHashMap<>();
            for(var field:value.getClass().getRecordComponents())fields.put(field.getName(),field.getAccessor().invoke(value));
            return json(fields);
        }
        String text=value.toString();StringBuilder out=new StringBuilder("\"");
        for(int i=0;i<text.length();i++){
            char c=text.charAt(i);
            if(c=='"'||c=='\\')out.append('\\').append(c);
            else if(c<32)out.append(String.format("\\u%04x",(int)c));
            else out.append(c);
        }
        return out.append('"').toString();
    }
}
