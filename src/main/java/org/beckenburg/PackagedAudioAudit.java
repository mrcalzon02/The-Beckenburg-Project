package org.beckenburg;
import javax.sound.sampled.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.*;

/** Checks audio inside the assembled game JAR, without sound hardware or network. */
public final class PackagedAudioAudit {
 private PackagedAudioAudit(){}
 private static final String ROOT="/org/beckenburg/";
 private static final AudioFormat EFFECTS=new AudioFormat(16000f,16,2,true,false);
 private static final String[] WAV={
   "ambient_rain","ambient_wind","book","camp_hollow_creek_goose_omen",
   "device_off","device_on","door","dots_motor","drop","electric_hum",
   "equipment","monitor_hum","pickup","purchase","scanner",
   "step_stone_0","step_stone_1","step_stone_2","ui_click"};
 private static final String[] SCORE={
   "castle-candlelit-shadows","ambient-cold-unresolved-drone",
   "ambient-sombra-profunda","ritual-of-ash","ritual-final-strike","hunt-final-surge"};
 private static final String[] WEATHER={
   "audio_00","audio_01","audio_02","audio_03","audio_04","audio_05",
   "atmosphere","cloud_to_ground","fork","horizontal_arc","sheet"};
 private static InputStream get(String name)throws IOException{
   InputStream in=PackagedAudioAudit.class.getResourceAsStream(ROOT+name);
   if(in==null)throw new IOException("Absent packaged audio: "+name);
   return in;
 }
 private static void wav(String name)throws Exception{
   try(InputStream bytes=get("sfx/"+name+".wav");
       AudioInputStream source=AudioSystem.getAudioInputStream(new BufferedInputStream(bytes))){
     if(!AudioSystem.isConversionSupported(EFFECTS,source.getFormat()))
       throw new IOException("Incompatible mixer WAV: "+name);
     try(AudioInputStream pcm=AudioSystem.getAudioInputStream(EFFECTS,source)){
       if(pcm.readNBytes(2048).length!=2048)
         throw new IOException("Truncated packaged WAV: "+name);
     }
   }
 }
 private static InputStream archive(String name)throws IOException{
   InputStream one=PackagedAudioAudit.class.getResourceAsStream(ROOT+"music/"+name+".zip");
   if(one!=null)return one;
   InputStream a=PackagedAudioAudit.class.getResourceAsStream(ROOT+"music/"+name+".zip.0");
   InputStream b=PackagedAudioAudit.class.getResourceAsStream(ROOT+"music/"+name+".zip.1");
   if(a==null||b==null){
     if(a!=null)a.close();if(b!=null)b.close();
     throw new IOException("Incomplete packaged score: "+name);
   }
   return new SequenceInputStream(a,b);
 }
 private static void score(String name)throws Exception{
   try(InputStream input=archive(name);ZipInputStream zip=new ZipInputStream(input)){
     ZipEntry item=zip.getNextEntry();
     if(item==null||!item.getName().equals(name+".wav"))
       throw new IOException("Wrong score ZIP member: "+name);
     byte[] data=zip.readNBytes(8*1024*1024+1);
     if(data.length<50000||data.length>8*1024*1024||zip.getNextEntry()!=null)
       throw new IOException("Invalid packaged score: "+name);
     try(AudioInputStream source=AudioSystem.getAudioInputStream(
         new BufferedInputStream(new ByteArrayInputStream(data)))){
       if(!AudioSystem.isConversionSupported(
           new AudioFormat(22050f,16,1,true,false),source.getFormat()))
         throw new IOException("Incompatible packaged score: "+name);
     }
   }
 }
 private static void layout(String name,int count)throws Exception{
   try(InputStream input=get("sounds/"+name+".sounds")){
     byte[] data=input.readNBytes(100001);
     if(data.length>100000)throw new IOException("Oversized sound layout "+name);
     String[] lines=new String(data,StandardCharsets.UTF_8).split("\\R");
     if(lines.length<2||!lines[0].equals("sounds=1")||
         !lines[1].equals("level="+name))throw new IOException("Wrong layout "+name);
     int observed=0;
     for(int i=2;i<lines.length;i++){
       if(lines[i].isEmpty())continue;
       if(lines[i].split("\\|",-1).length!=18)
         throw new IOException("Bad emitter "+name+" line "+(i+1));
       observed++;
     }
     if(observed!=count)throw new IOException("Missing sound zones for "+name);
   }
 }
 private static void weather(String name)throws Exception{
   try(InputStream input=get("weather/"+name+".zip");
       ZipInputStream zip=new ZipInputStream(input)){
     boolean nonempty=false;ZipEntry item;
     while((item=zip.getNextEntry())!=null)
       if(!item.isDirectory()&&zip.readNBytes(8).length>0){nonempty=true;break;}
     if(!nonempty)throw new IOException("Empty weather sound archive "+name);
   }
 }
 public static void main(String[] args)throws Exception{
   for(String name:WAV)wav(name);
   for(String name:SCORE)score(name);
   layout("castle",10);layout("hollow-creek",3);
   for(String name:WEATHER)weather(name);
   System.out.println("PASS JAR audio: 19 WAVs, six scores, 13 sound zones, 11 weather bundles");
 }
}
