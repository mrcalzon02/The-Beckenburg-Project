package org.beckenburg.editor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

/** Owner-thread receipts for one protocol session. Retired sequences can never execute again. */
final class EditorRequestJournal<T> {
    static final class Conflict extends IllegalStateException {
        Conflict(String message){super(message);}
    }
    private record Entry<T>(String digest,T result,int size){}
    private final TreeMap<Long,Entry<T>> receipts=new TreeMap<>();
    private final ToIntFunction<T> size;
    private final int limit,byteLimit;
    private long retiredThrough,used;

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
    static String digest(String header,byte[] body){
        try{
            var hash=MessageDigest.getInstance("SHA-256");
            hash.update(header.getBytes(StandardCharsets.UTF_8));hash.update((byte)'\n');
            return HexFormat.of().formatHex(hash.digest(body));
        }catch(NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
    }
}
