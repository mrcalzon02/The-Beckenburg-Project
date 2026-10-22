package org.beckenburg;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Licensed Shared PSX Weather Module v1 media imported from the pinned Drive
 * release. Individual compact archives preserve original WAV and textured GLB
 * bytes; no runtime Drive/network dependency or procedural substitute assets.
 */
public final class WeatherAssetLibrary {
    private WeatherAssetLibrary(){}
    private static final String[] PACKS={
        "audio_00","audio_01","audio_02","audio_03","audio_04","audio_05",
        "atmosphere","cloud_to_ground","fork","horizontal_arc","sheet"};
    private static final Map<String,byte[]> binary=new HashMap<>();
    private static final Map<String,GlbProps.Model> geometry=new HashMap<>();
    private static final Set<String> loaded=new HashSet<>();
    public static final int MESH_COUNT=25,AUDIO_COUNT=12;
    private static final int ENTRY_MAX=1_500_000;
    private static final int ARCHIVE_MAX=3_100_000;
    private static String packFor(String name){
        if(name==null||!name.matches("[a-z0-9_]+\\.(?:wav|glb)"))
            throw new IllegalArgumentException("invalid weather asset");
        if(name.endsWith(".wav")){
            return switch(name){
                case "ambient_fog.wav","ambient_rain.wav"->"audio_00";
                case "ambient_snow.wav","ambient_storm.wav"->"audio_01";
                case "ambient_wind.wav","music_day.wav"->"audio_02";
                case "music_fog.wav","music_night.wav"->"audio_03";
                case "music_snow.wav","music_storm.wav"->"audio_04";
                case "thunder_near.wav","thunder_distant.wav"->"audio_05";
                default->throw new IllegalArgumentException("unapproved weather audio "+name);
            };
        }
        if(name.startsWith("lightning_")){
            String family=name.substring("lightning_".length());
            for(String f:new String[]{"cloud_to_ground","horizontal_arc","fork","sheet"})
                if(family.matches(f+"_frame_0[0-3]"))return f;
            throw new IllegalArgumentException("unapproved lightning frame "+name);
        }
        return switch(name){
            case "weather_cloud_bank.glb","weather_dense_fog_bank.glb",
                "weather_ground_fog_bank.glb","weather_ground_splash_frame_00.glb",
                "weather_ground_splash_frame_01.glb","weather_ground_splash_frame_02.glb",
                "weather_heavy_rain_cell.glb","weather_rain_cell.glb",
                "weather_storm_cloud_bank.glb"->"atmosphere";
            default->throw new IllegalArgumentException("unapproved weather mesh "+name);
        };
    }
    public static synchronized byte[] bytes(String name){
        String pack=packFor(name);
        if(!loaded.contains(pack))loadPack(pack);
        byte[] payload=binary.get(name);
        if(payload==null)throw new IllegalStateException("Missing weather media "+name);
        return payload;
    }
    private static void loadPack(String pack){
        if(!Arrays.asList(PACKS).contains(pack))throw new IllegalArgumentException("pack");
        String resource="/org/beckenburg/weather/"+pack+".zip";
        int total=0;
        try(InputStream input=WeatherAssetLibrary.class.getResourceAsStream(resource)){
            if(input==null)throw new IOException("Missing packaged original weather asset pack "+resource);
            try(ZipInputStream zip=new ZipInputStream(input)){
                ZipEntry entry;
                while((entry=zip.getNextEntry())!=null){
                    if(entry.isDirectory())continue;
                    String name=entry.getName();
                    if(name.contains("/")||!pack.equals(packFor(name))||binary.containsKey(name))
                        throw new IOException("Unsafe or duplicate weather media "+name);
                    ByteArrayOutputStream out=new ByteArrayOutputStream();
                    byte[] chunk=new byte[8192];int n;
                    while((n=zip.read(chunk))!=-1){
                        if(out.size()+n>ENTRY_MAX)throw new IOException("Oversize weather media "+name);
                        out.write(chunk,0,n);
                    }
                    total+=out.size();
                    if(total>ARCHIVE_MAX)throw new IOException("Oversize weather archive "+pack);
                    binary.put(name,out.toByteArray());
                }
            }
            loaded.add(pack);
        }catch(IOException ex){throw new IllegalStateException("Broken Shared PSX Weather media: "+resource,ex);}
    }
    public static synchronized GlbProps.Model mesh(String id){
        String name=id+".glb";
        GlbProps.Model existing=geometry.get(name);
        if(existing!=null)return existing;
        try{
            GlbProps.Model model=GlbProps.decode(bytes(name));
            geometry.put(name,model);
            return model;
        }catch(IOException ex){throw new IllegalStateException("Invalid original weather VFX GLB "+name,ex);}
    }
    public static void verifyPackaged(){
        for(String pack:PACKS)loadAndCheck(pack);
        if(binary.size()!=MESH_COUNT+AUDIO_COUNT)
            throw new IllegalStateException("Weather v1 asset count: "+binary.size());
    }
    private static synchronized void loadAndCheck(String pack){if(!loaded.contains(pack))loadPack(pack);}
}
