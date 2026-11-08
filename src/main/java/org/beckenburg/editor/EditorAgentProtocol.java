package org.beckenburg.editor;

import java.io.BufferedReader;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Bounded UTF-8 line protocol for a trusted headless host that supplies the real service and owner thread. */
public final class EditorAgentProtocol {
    private EditorAgentProtocol(){}
    private static String readRequest(BufferedReader input)throws java.io.IOException{
        var line=new StringBuilder();
        for(int value;(value=input.read())!=-1;){
            if(value=='\n')return line.toString();
            if(value=='\r'){
                input.mark(1);int next=input.read();if(next!=-1&&next!='\n')input.reset();return line.toString();
            }
            if(line.length()==16384)throw new java.io.IOException("Request exceeds 16384 UTF-16 code units");
            line.append((char)value);
        }
        return line.length()==0?null:line.toString();
    }
    private static EditorAgentService.Command parseEdit(String[] fields, int start, int end) {
        if (start >= end || fields[start].isEmpty()) throw new IllegalArgumentException("Expected edit operation");
        Map<String,String> arguments = new LinkedHashMap<>();
        for (int i = start + 1; i < end; i++) {
            int split = fields[i].indexOf('=');
            if (split < 1 || arguments.putIfAbsent(fields[i].substring(0, split), fields[i].substring(split + 1)) != null) {
                throw new IllegalArgumentException("Expected unique key=value arguments");
            }
        }
        return new EditorAgentService.Command(fields[start], arguments);
    }

    static java.util.List<EditorAgentService.Command> parseBatch(String[] fields,int start) {
        var commands = new java.util.ArrayList<EditorAgentService.Command>();
        for (int i = start; i <= fields.length; i++) {
            if (i == fields.length || fields[i].equals("|")) {
                if (commands.size() == 64) throw new IllegalArgumentException("Batch exceeds 64 operations");
                commands.add(parseEdit(fields, start, i));
                start = i + 1;
            }
        }
        return commands;
    }
    public static int run(EditorAgentService<?> service,BufferedReader requests,PrintStream responses)throws Exception{
        boolean failed=false;
        for(;;){
            String line;
            try{line=readRequest(requests);}
            catch(java.io.IOException failure){
                responses.println(json(Map.of("ok",false,"phase","protocol","error",failure.toString())));responses.flush();return 1;
            }
            if(line==null)break;
            try{
                String[] parts=line.split("\t",-1);String command=parts[0];Object result;
                if(command.equals("revision")){
                    if(parts.length!=1)throw new IllegalArgumentException("revision takes no arguments");
                    result=service.sceneRevision();
                }else if(command.equals("summary")){
                    if(parts.length!=1)throw new IllegalArgumentException("summary takes no arguments");
                    result=service.summary();
                }else if(command.equals("inspect")){
                    if(parts.length!=1)throw new IllegalArgumentException("inspect takes no arguments");
                    result=service.inspect();
                }else{
                    if(parts.length<2)throw new IllegalArgumentException("Expected scene revision");
                    long revision=Long.parseLong(parts[1]);
                    int expected=switch(command){case "edit","batch","nodes"->-1;case "capture","test","node"->3;case "hierarchy","changes","assets"->4;case "verify-position"->6;default->2;};
                    if((expected>=0&&parts.length!=expected)||(expected<0&&parts.length<3))
                        throw new IllegalArgumentException("Wrong argument count");
                    result=switch(command){
                        case "edit" -> service.transact(revision, java.util.List.of(parseEdit(parts, 2, parts.length)));
                        case "batch" -> service.transact(revision, parseBatch(parts,2));
                        case "node" -> service.node(revision,parts[2]);
                        case "selection" -> service.selection(revision);
                        case "verify-position" -> service.verifyPosition(revision,parts[2],Double.parseDouble(parts[3]),Double.parseDouble(parts[4]),Double.parseDouble(parts[5]));
                        case "nodes" -> service.nodes(revision,java.util.Arrays.asList(parts).subList(2,parts.length));
                        case "assets" -> service.assets(revision,Integer.parseInt(parts[2]),Integer.parseInt(parts[3]));
                        case "hierarchy" -> service.hierarchy(revision,Integer.parseInt(parts[2]),Integer.parseInt(parts[3]));
                        case "changes" -> service.changes(revision,Integer.parseInt(parts[2]),Integer.parseInt(parts[3]));
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
    public static String json(Object value)throws ReflectiveOperationException{
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
