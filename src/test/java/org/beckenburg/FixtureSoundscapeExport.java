package org.beckenburg;
import java.nio.file.*;import javax.sound.sampled.*;import java.io.*;import java.security.*;
/** Original CC0 export, independent of the user's volume preference. */
public final class FixtureSoundscapeExport {
 public static void main(String[]args)throws Exception{
  Path out=Path.of("development/levels/event-audio");Files.createDirectories(out);StringBuilder manifest=new StringBuilder("{\n  \"license\":\"CC0-1.0\",\n  \"provenance\":\"Original deterministic oscillators, noise and newly composed melodies. No third-party audio.\",\n  \"sample_rate\":16000,\n  \"files\":[\n");boolean comma=false;
  for(var sound:FixtureSoundscape.Sound.values()){
   int frames=FixtureSoundscape.frames(sound);byte[] mono=new byte[frames*2];for(int n=0;n<frames;n++){short s=FixtureSoundscape.sample(sound,n);mono[n*2]=(byte)s;mono[n*2+1]=(byte)(s>>8);}
   Path file=out.resolve("original_"+sound.name().toLowerCase(java.util.Locale.ROOT)+".wav");
   try(var stream=new AudioInputStream(new ByteArrayInputStream(mono),new AudioFormat(16000,16,1,true,false),frames)){AudioSystem.write(stream,AudioFileFormat.Type.WAVE,file.toFile());}
   String hash=java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));if(comma)manifest.append(",\n");comma=true;
   manifest.append("    {\"file\":\"").append(file.getFileName()).append("\",\"seconds\":").append(frames/16000).append(",\"sha256\":\"").append(hash).append("\"}");System.out.println("CC0_SYNTH "+file.getFileName());
  }Files.writeString(out.resolve("original-sound-manifest.json"),manifest.append("\n  ]\n}\n"));
 }
}
