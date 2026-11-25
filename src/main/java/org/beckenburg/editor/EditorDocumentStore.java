package org.beckenburg.editor;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;

/** Bounded persistence of authoritative document bytes, not a scene model.
 * A forced write-ahead journal can finish an interrupted atomic replacement.
 * Recovery refuses an externally changed target. One prior valid, distinct
 * document is retained for an explicit human recovery operation.
 */
public final class EditorDocumentStore {
    public static final int MAX_BYTES=8*1024*1024;
    private static final int MAGIC=0x454a4e4c,VERSION=1;
    @FunctionalInterface public interface Writer {void write(OutputStream output)throws IOException;}
    @FunctionalInterface public interface Validator {void validate(Path file)throws IOException;}
    private interface Action<T>{T run()throws IOException;}
    private record Journal(boolean existed,byte[] before,byte[] after,byte[] payload){}
    enum Checkpoint { JOURNAL_DURABLE, DOCUMENT_REPLACED }
    @FunctionalInterface interface Observer {void reached(Checkpoint checkpoint)throws IOException;}
    private static final Observer NO_OBSERVER=checkpoint->{};
    private EditorDocumentStore(){}
    public static Path previous(Path target){return sidecar(normalize(target),".previous");}
    public static Path journal(Path target){return sidecar(normalize(target),".journal");}
    private static Path normalize(Path path){return Objects.requireNonNull(path).toAbsolutePath().normalize();}
    private static Path sidecar(Path target,String suffix){return target.resolveSibling("."+target.getFileName()+".editor"+suffix);}
    private static String temporaryPrefix(Path target){return "."+target.getFileName()+".editor-write-";}
    private static void cleanupStaging(Path target)throws IOException{
        var prefixes=java.util.List.of(temporaryPrefix(target),temporaryPrefix(journal(target)),temporaryPrefix(previous(target)));
        try(var entries=Files.newDirectoryStream(target.getParent())){
            for(Path entry:entries){
                String name=entry.getFileName().toString();
                if(name.endsWith(".tmp")&&prefixes.stream().anyMatch(name::startsWith)){
                    regularOrMissing(entry);Files.deleteIfExists(entry);
                }
            }
        }
    }
    private static void regularOrMissing(Path path)throws IOException{
        if(Files.exists(path,LinkOption.NOFOLLOW_LINKS)
            &&(!Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS)||Files.isSymbolicLink(path)))
            throw new IOException("Editor document path is not a regular file: "+path);
    }
    private static <T>T locked(Path target,Action<T> action)throws IOException{
        Path lock=sidecar(target,".lock");regularOrMissing(target);regularOrMissing(lock);
        try(var channel=FileChannel.open(lock,StandardOpenOption.CREATE,StandardOpenOption.WRITE)){
            try(var held=channel.tryLock()){
                if(held==null)throw new IOException("Editor document is busy: "+target);
                return action.run();
            }catch(OverlappingFileLockException busy){throw new IOException("Editor document is busy: "+target,busy);}
        }
    }
    private static byte[] read(Path path,int limit)throws IOException{
        regularOrMissing(path);
        if(Files.size(path)>limit)throw new IOException("Editor document exceeds byte budget: "+path);
        try(var input=Files.newInputStream(path)){
            byte[] bytes=input.readNBytes(limit+1);
            if(bytes.length>limit)throw new IOException("Editor document grew beyond byte budget");
            return bytes;
        }
    }
    /** Exact bounded document bytes for codecs which must bind a decoded state to its revision. */
    public static byte[] readDocument(Path path)throws IOException{return read(normalize(path),MAX_BYTES);}
    public static String fingerprint(byte[] bytes){return java.util.HexFormat.of().formatHex(digest(bytes));}
    private static byte[] digest(byte[] bytes){
        try{return MessageDigest.getInstance("SHA-256").digest(bytes);}
        catch(NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
    }
    private static boolean matches(Path target,boolean existed,byte[] hash)throws IOException{
        regularOrMissing(target);
        if(!Files.exists(target))return !existed;
        return existed&&MessageDigest.isEqual(digest(read(target,MAX_BYTES)),hash);
    }
    /** The callback cannot exceed the on-disk budget, even before readback. */
    private static Path stage(Path target,Writer writer,int budget)throws IOException{
        Path temp=Files.createTempFile(target.getParent(),temporaryPrefix(target),".tmp");
        try{
            try(var channel=FileChannel.open(temp,StandardOpenOption.WRITE);
                var output=java.nio.channels.Channels.newOutputStream(channel)){
                var bounded=new FilterOutputStream(output){
                    int count;
                    private void reserve(int amount)throws IOException{
                        if(amount<0||amount>budget-count)throw new IOException("Editor output exceeds byte budget");
                        count+=amount;
                    }
                    public void write(int value)throws IOException{reserve(1);out.write(value);}
                    public void write(byte[] bytes,int offset,int length)throws IOException{reserve(length);out.write(bytes,offset,length);}
                    // Store owns the channel; codecs may close their wrappers.
                    public void close()throws IOException{flush();}
                };
                writer.write(bounded);bounded.flush();channel.force(true);
            }
            return temp;
        }catch(IOException|RuntimeException|Error failure){Files.deleteIfExists(temp);throw failure;}
    }
    private static void replace(Path staged,Path destination)throws IOException{
        regularOrMissing(destination);
        // Fail explicitly on providers without atomic replacement.
        Files.move(staged,destination,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
    }
    private static void installBytes(Path target,byte[] bytes)throws IOException{
        Path temp=stage(target,out->out.write(bytes),MAX_BYTES);
        try{replace(temp,target);}finally{Files.deleteIfExists(temp);}
    }
    public static void save(Path target,Writer writer,Validator validator)throws IOException{
        save(target,writer,validator,validator);
    }
    public static void save(Path destination,Writer writer,Validator format,Validator prepared)throws IOException{
        save(destination,writer,format,prepared,NO_OBSERVER);
    }
    // Package-only fault injection: tests halt a real child process at durable
    // filesystem boundaries. No process-control hook is exposed to transports.
    static void save(Path destination,Writer writer,Validator format,Validator prepared,Observer observer)throws IOException{
        Path target=normalize(destination);Objects.requireNonNull(writer);Objects.requireNonNull(format);Objects.requireNonNull(prepared);
        Files.createDirectories(target.getParent());
        locked(target,()->{recoverLocked(target,format);cleanupStaging(target);saveLocked(target,writer,format,prepared,observer);return null;});
    }
    /** Compare the observed disk revision inside the writer lock, after staging.
     * A null fingerprint requires a previously missing document. Format validation
     * controls backup eligibility; it is deliberately separate from this guard.
     */
    public static void saveIfUnchanged(Path destination,Writer writer,Validator format,Validator prepared,String expectedSha256)throws IOException{
        saveIfUnchanged(destination,writer,format,prepared,expectedSha256,NO_OBSERVER);
    }
    static void saveIfUnchanged(Path destination,Writer writer,Validator format,Validator prepared,String expectedSha256,Observer observer)throws IOException{
        Path target=normalize(destination);Objects.requireNonNull(writer);Objects.requireNonNull(format);Objects.requireNonNull(prepared);
        if(expectedSha256!=null&&!expectedSha256.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("Invalid document fingerprint");
        Files.createDirectories(target.getParent());
        locked(target,()->{recoverLocked(target,format);cleanupStaging(target);saveLocked(target,writer,format,prepared,observer,expectedSha256,true);return null;});
    }
    private static void saveLocked(Path target,Writer writer,Validator format,Validator prepared,Observer observer)throws IOException{
        saveLocked(target,writer,format,prepared,observer,null,false);
    }
    private static void saveLocked(Path target,Writer writer,Validator format,Validator prepared,Observer observer,String expectedSha256,boolean guarded)throws IOException{
        Path temp=stage(target,writer,MAX_BYTES);
        Path log=journal(target);
        boolean existed=Files.exists(target);
        byte[] before=new byte[32];
        boolean published=false;
        try{
            prepared.validate(temp);
            byte[] next=read(temp,MAX_BYTES),old=existed?read(target,MAX_BYTES):new byte[0];
            before=digest(old);
            if(guarded&&(existed?(expectedSha256==null||!expectedSha256.equals(java.util.HexFormat.of().formatHex(before))):expectedSha256!=null))
                throw new IOException("Editor document changed on disk; reopen before saving: "+target);
            if(existed&&Arrays.equals(old,next))return; // Keep the last DISTINCT good backup.
            if(existed){
                boolean valid=true;
                try{format.validate(target);}catch(IOException invalid){valid=false;}
                if(valid)installBytes(previous(target),old);
            }
            byte[] expectedBefore=before;
            Path stagedLog=stage(log,output->{
                var data=new DataOutputStream(output);
                data.writeInt(MAGIC);data.writeInt(VERSION);data.writeUTF(target.toString());
                data.writeBoolean(existed);data.write(expectedBefore);data.write(digest(next));
                data.writeInt(next.length);data.write(next);data.flush();
            },MAX_BYTES+16384);
            try{replace(stagedLog,log);published=true;}finally{Files.deleteIfExists(stagedLog);}
            observer.reached(Checkpoint.JOURNAL_DURABLE);
            if(!matches(target,existed,before))throw new IOException("Document changed during save; journal preserved: "+target);
            replace(temp,target);
            observer.reached(Checkpoint.DOCUMENT_REPLACED);
            // Installed bytes are authoritative. A leftover journal is safe to
            // clean at next load, so cleanup failure cannot report a false save failure.
            try{Files.deleteIfExists(log);}catch(IOException cleanup){/* replay recognizes committed hash */}
        }catch(IOException|RuntimeException|Error failure){
            if(published){
                try{if(matches(target,existed,before))Files.deleteIfExists(log);}
                catch(IOException cleanup){failure.addSuppressed(cleanup);}
            }
            throw failure;
        }finally{Files.deleteIfExists(temp);}
    }
    /** Normal reopening only replays a checksummed pending save when the target
     * still has its exact prior bytes (or confirms an already committed save). */
    public static boolean recoverPending(Path destination,Validator validator)throws IOException{
        Path target=normalize(destination);Objects.requireNonNull(validator);
        regularOrMissing(journal(target));
        if(!Files.exists(journal(target)))return false;
        return locked(target,()->{boolean recovered=recoverLocked(target,validator);cleanupStaging(target);return recovered;});
    }
    private static Journal readJournal(Path target)throws IOException{
        byte[] bytes=read(journal(target),MAX_BYTES+16384);
        try(var data=new DataInputStream(new ByteArrayInputStream(bytes))){
            if(data.readInt()!=MAGIC||data.readInt()!=VERSION||!Path.of(data.readUTF()).equals(target))
                throw new IOException("Editor save journal identity/version mismatch");
            boolean existed=data.readBoolean();byte[] before=data.readNBytes(32),after=data.readNBytes(32);
            int size=data.readInt();
            if(before.length!=32||after.length!=32||size<0||size>MAX_BYTES)throw new IOException("Invalid editor journal size");
            byte[] payload=data.readNBytes(size);
            if(payload.length!=size||data.read()!=-1||!MessageDigest.isEqual(after,digest(payload)))
                throw new IOException("Editor journal checksum/truncation failure");
            return new Journal(existed,before,after,payload);
        }catch(IllegalArgumentException malformed){throw new IOException("Invalid editor journal",malformed);}
    }
    private static boolean recoverLocked(Path target,Validator validator)throws IOException{
        Path log=journal(target);regularOrMissing(log);
        if(!Files.exists(log))return false;
        var entry=readJournal(target);
        if(matches(target,true,entry.after())){
            validator.validate(target);Files.deleteIfExists(log);return true;
        }
        if(!matches(target,entry.existed(),entry.before()))
            throw new IOException("Document differs from pending save; preserved document and journal: "+target);
        Path temp=stage(target,out->out.write(entry.payload()),MAX_BYTES);
        try{
            validator.validate(temp);
            if(!matches(target,entry.existed(),entry.before()))throw new IOException("Document changed during recovery");
            replace(temp,target);Files.deleteIfExists(log);return true;
        }finally{Files.deleteIfExists(temp);}
    }
    /** Explicit recovery, never an automatic fallback from corrupt scene data.
     * Caller validates ownership/schema BEFORE bytes replace the real document. */
    public static void restorePrevious(Path destination,Validator validator)throws IOException{
        restorePrevious(destination,validator,validator);
    }
    public static void restorePrevious(Path destination,Validator format,Validator validator)throws IOException{
        Path target=normalize(destination);Objects.requireNonNull(validator);
        locked(target,()->{
            recoverLocked(target,format);
            cleanupStaging(target);
            Path previous=previous(target);validator.validate(previous);
            byte[] bytes=read(previous,MAX_BYTES);
            saveLocked(target,out->out.write(bytes),format,validator,NO_OBSERVER);return null;
        });
    }
}
