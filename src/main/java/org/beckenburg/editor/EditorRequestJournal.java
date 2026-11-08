package org.beckenburg.editor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.TreeMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.LongSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

/** Owner-thread receipts for one protocol session. Retired sequences can never execute again. */
final class EditorRequestJournal<T> {
    static final class Conflict extends IllegalStateException {
        Conflict(String message){super(message);}
    }
    private record Entry<T>(String digest,T result,int size){}
    record Event(long event,String time,String action,List<String> operations,Long sequence,Long expectedRevision,
                 Long observedRevision,String outcome,String digest){}
    record Page(long firstAvailable,long lastEvent,List<Event> events){}
    private final TreeMap<Long,Entry<T>> receipts=new TreeMap<>();
    private final ToIntFunction<T> size;
    private final int limit,byteLimit;
    private long retiredThrough,used;
    private final ArrayDeque<Event> events=new ArrayDeque<>();
    private long lastEvent;

    EditorRequestJournal(ToIntFunction<T> size){this(size,64,4*1024*1024);}
    EditorRequestJournal(ToIntFunction<T> size,int limit,int byteLimit){
        this.size=size;this.limit=limit;this.byteLimit=byteLimit;
    }
    T execute(long sequence,String digest,Supplier<T> operation){
        if(sequence<=0)throw new IllegalArgumentException("Expected a positive request sequence");
        if(sequence<=retiredThrough)throw new Conflict("Request sequence retired");
        var prior=receipts.get(sequence);
        if(prior!=null){
            if(!prior.digest.equals(digest))throw new Conflict("Request sequence has different arguments");
            return prior.result;
        }
        T result=operation.get();
        int bytes=size.applyAsInt(result);
        receipts.put(sequence,new Entry<>(digest,result,bytes));used+=bytes;
        while(receipts.size()>limit||used>byteLimit){
            var oldest=receipts.pollFirstEntry();
            retiredThrough=oldest.getKey();used-=oldest.getValue().size;
        }
        return result;
    }
    T track(String action,List<String> operations,Long sequence,Long expected,String digest,Supplier<T> operation,
            Predicate<T> success,LongSupplier revision){
        boolean repeated=sequence!=null&&receipts.containsKey(sequence);
        String outcome="refused";
        try{
            T value=sequence==null?operation.get():execute(sequence,digest,operation);
            if(success.test(value))outcome=repeated?"replayed":"applied";
            else if(repeated)outcome="replayed-refusal";
            return value;
        }catch(Conflict conflict){outcome="sequence-conflict";throw conflict;}
        finally{
            Long observed;
            try{observed=revision.getAsLong();}catch(RuntimeException unavailable){observed=null;}
            if(events.size()==128)events.removeFirst();
            events.addLast(new Event(++lastEvent,java.time.Instant.now().toString(),action,List.copyOf(operations),sequence,expected,observed,outcome,digest));
        }
    }
    Page page(long after,int limit){
        if(after<0||limit<1||limit>64)throw new IllegalArgumentException("Expected nonnegative event cursor and 1 to 64 events");
        var page=new ArrayList<Event>();
        for(var event:events)if(event.event>after){page.add(event);if(page.size()==limit)break;}
        return new Page(events.isEmpty()?0:events.getFirst().event,lastEvent,List.copyOf(page));
    }
    static Long revisionField(String value){
        try{return Long.parseLong(value);}catch(NumberFormatException invalid){return null;}
    }
    static List<String> operationNames(String[] fields,int first,Set<String> allowed){
        var names=new ArrayList<String>();
        for(int i=first;i<fields.length&&names.size()<64;i++)if(i==first||fields[i-1].equals("|"))
            names.add(allowed.contains(fields[i])?fields[i]:"unsupported");
        return names;
    }
    static String digest(String header,byte[] body){
        try{
            var hash=MessageDigest.getInstance("SHA-256");
            hash.update(header.getBytes(StandardCharsets.UTF_8));hash.update((byte)'\n');
            return HexFormat.of().formatHex(hash.digest(body));
        }catch(NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
    }
}
