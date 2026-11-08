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
    private record WireReply(String line,boolean failed){}
    static String errorCode(Throwable failure){
        if(failure instanceof EditorRequestJournal.Conflict)return "SEQUENCE_CONFLICT";
        if(failure instanceof EditorAgentService.StaleRevision)return "STALE_REVISION";
        if(failure instanceof UnsupportedOperationException)return "UNSUPPORTED_CAPABILITY";
        if(failure instanceof IllegalArgumentException)return "INVALID_ARGUMENT";
        if(failure instanceof IllegalStateException)return "INVALID_STATE";
        if(failure instanceof java.io.IOException||failure instanceof java.io.UncheckedIOException)return "IO_ERROR";
        return "INTERNAL";
    }
    private static WireReply refusal(Exception failure){
        try{return new WireReply(json(Map.of("ok",false,"code",errorCode(failure),"error",failure.toString())),true);}
        catch(ReflectiveOperationException impossible){throw new AssertionError(impossible);}
    }
    private static WireReply dispatch(EditorAgentService<?> service,String line){
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
                int expected=switch(command){case "edit","batch","nodes"->-1;case "capture","playtest","test","node","focus"->3;case "hierarchy","changes","assets"->4;case "walkability"->5;case "verify-position"->6;default->2;};
                if((expected>=0&&parts.length!=expected)||(expected<0&&parts.length<3))
                    throw new IllegalArgumentException("Wrong argument count");
                result=switch(command){
                    case "edit" -> service.transact(revision, java.util.List.of(parseEdit(parts, 2, parts.length)));
                    case "batch" -> service.transact(revision, parseBatch(parts,2));
                    case "node" -> service.node(revision,parts[2]);
                    case "focus" -> service.focus(revision,parts[2]);
                    case "selection" -> service.selection(revision);
                    case "walkability" -> service.walkability(revision,Double.parseDouble(parts[2]),Double.parseDouble(parts[3]),Double.parseDouble(parts[4]));
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
                    case "playtest" -> service.playtest(revision,Path.of(parts[2]));
                    case "test" -> service.test(revision,parts[2]);
                    default -> throw new IllegalArgumentException("Unknown command: "+command);
                };
            }
            boolean failed=result instanceof EditorAgentService.Evidence<?> e && e.result() instanceof java.util.List<?> list
                && list.stream().anyMatch(item->item instanceof EditorAgentService.Check check&&!check.passed());
            return new WireReply(json(Map.of("ok",true,"result",result)),failed);
        }catch(Exception failure){return refusal(failure);}
    }
    private static final java.util.Set<String> ACTIONS=java.util.Set.of("edit","batch","undo","redo","save","reopen","recover","focus","playtest");
    private static WireReply handle(EditorAgentService<?> service,EditorRequestJournal<WireReply> journal,java.util.Set<String> known,String line){
        try{
            Long sequence=null;String request=line;
            if(line.startsWith("once-v1\t")){
                String[] envelope=line.split("\t",3);
                if(envelope.length!=3)throw new IllegalArgumentException("Expected once-v1, sequence and action");
                sequence=Long.parseLong(envelope[1]);request=envelope[2];
            }
            int split=request.indexOf('\t');String action=split<0?request:request.substring(0,split);
            if(!ACTIONS.contains(action)){
                if(sequence!=null)throw new IllegalArgumentException("Request sequence requires an editor action");
                if(!action.equals("actions"))return dispatch(service,request);
                var parts=request.split("\t",-1);
                if(parts.length!=4)throw new IllegalArgumentException("Expected actions, revision, after, limit");
                var current=service.sceneRevision();
                if(Long.parseLong(parts[1])!=current.revision())throw new EditorAgentService.StaleRevision();
                var page=journal.page(Long.parseLong(parts[2]),Integer.parseInt(parts[3]));
                return new WireReply(json(Map.of("ok",true,"result",new EditorAgentService.Evidence<>(current.sceneId(),current.revision(),page))),false);
            }
            var fields=request.split("\t",-1);
            var names=action.equals("edit")||action.equals("batch")?EditorRequestJournal.operationNames(fields,2,known):java.util.List.<String>of();
            String nativeRequest=request;
            return journal.track(action,names,sequence,fields.length<2?null:EditorRequestJournal.revisionField(fields[1]),
                EditorRequestJournal.digest(request,new byte[0]),()->dispatch(service,nativeRequest),reply->!reply.failed,
                ()->service.sceneRevision().revision());
        }catch(Exception failure){return refusal(failure);}
    }
    public static int run(EditorAgentService<?> service,BufferedReader requests,PrintStream responses)throws Exception{
        boolean failed=false;
        var journal=new EditorRequestJournal<WireReply>(reply->reply.line.getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
        java.util.Set<String> known;
        try{known=java.util.Set.copyOf(service.summary().operations());}catch(RuntimeException unavailable){known=java.util.Set.of();}
        for(;;){
            String line;
            try{line=readRequest(requests);}
            catch(java.io.IOException failure){
                responses.println(json(Map.of("ok",false,"phase","protocol","error",failure.toString())));responses.flush();return 1;
            }
            if(line==null)break;
            var reply=handle(service,journal,known,line);
            failed|=reply.failed;responses.println(reply.line);responses.flush();
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
