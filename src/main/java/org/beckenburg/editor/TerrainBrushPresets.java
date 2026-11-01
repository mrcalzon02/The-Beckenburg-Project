package org.beckenburg.editor;

import java.io.*;
import java.nio.file.*;
import java.util.*;
/** Versioned, self-contained brush presets. No game renderer or external image paths. */
public final class TerrainBrushPresets {
    private static final int MAGIC=0x54425052,VERSION=1,MAX_SIDE=2048;
    private final Path directory;
    public record Preset(TerrainLayerStack.Brush brush,
                         TerrainBrushProfile.Settings settings,
                         TerrainBrushProfile.Alpha alpha) {
        public Preset {
            Objects.requireNonNull(brush);Objects.requireNonNull(settings);
        }
    }
    public TerrainBrushPresets(StitchedHeightmapWorld world){
        directory=Objects.requireNonNull(world).directory().resolve("terrain-brush-presets");
    }
    private Path path(String id){
        if(id==null||!id.matches("[A-Za-z0-9_-]{1,64}"))
            throw new IllegalArgumentException("Preset name: letters, digits, - or _ (1..64)");
        return directory.resolve(id+".bin");
    }
    public List<String> list()throws IOException{
        if(!Files.isDirectory(directory))return List.of();
        try(var stream=Files.list(directory)){
            var names=new ArrayList<String>();
            for(Path p:stream.filter(Files::isRegularFile).toList()){
                String name=p.getFileName().toString();
                if(name.endsWith(".bin")&&name.substring(0,name.length()-4)
                    .matches("[A-Za-z0-9_-]{1,64}"))
                    names.add(name.substring(0,name.length()-4));
            }
            names.sort(String::compareTo);
            return List.copyOf(names);
        }
    }
    public void save(String name,Preset preset,boolean replace)throws IOException{
        Objects.requireNonNull(preset);
        Path file=path(name);
        Files.createDirectories(directory);
        if(Files.exists(file)&&!replace)
            throw new IOException("Preset already exists; confirm replacement first");
        Path temp=Files.createTempFile(directory,".brush-",".tmp");
        try{
            try(var out=new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(temp)))){
                out.writeInt(MAGIC);out.writeInt(VERSION);out.writeInt(preset.brush().ordinal());
                var s=preset.settings();
                out.writeDouble(s.radius());out.writeDouble(s.strength());
                out.writeDouble(s.target());out.writeDouble(s.inner());
                out.writeInt(s.curve());out.writeDouble(s.spacing());
                out.writeDouble(s.rotationDegrees());out.writeDouble(s.minElevation());
                out.writeDouble(s.maxElevation());out.writeDouble(s.maxSlopeDegrees());
                out.writeInt(s.flattenLimit().ordinal());
                if(preset.alpha()==null){out.writeInt(0);out.writeInt(0);}
                else {
                    float[][] values=preset.alpha().snapshot();
                    out.writeInt(preset.alpha().width());
                    out.writeInt(preset.alpha().height());
                    for(float[] row:values)for(float v:row)out.writeFloat(v);
                }
            }
            if(!replace&&Files.exists(file))throw new IOException("Preset created concurrently");
            Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING);
        }finally{Files.deleteIfExists(temp);}
    }
    public Preset open(String name)throws IOException{
        Path file=path(name);
        if(!Files.isRegularFile(file)||Files.size(file)>4L*MAX_SIDE*MAX_SIDE+4096)
            throw new IOException("Preset missing or too large");
        try(var in=new DataInputStream(new BufferedInputStream(Files.newInputStream(file)))){
            if(in.readInt()!=MAGIC||in.readInt()!=VERSION)
                throw new IOException("Unrecognized brush preset format");
            int brush=in.readInt();
            if(brush<0||brush>=TerrainLayerStack.Brush.values().length)
                throw new IOException("Invalid brush preset tool");
            double radius=in.readDouble(),strength=in.readDouble(),target=in.readDouble(),
                inner=in.readDouble();
            int curve=in.readInt();
            double spacing=in.readDouble(),rotation=in.readDouble(),
                min=in.readDouble(),max=in.readDouble(),slope=in.readDouble();
            int direction=in.readInt();
            if(direction<0||direction>=TerrainLayerStack.FlattenLimit.values().length)
                throw new IOException("Invalid brush preset flatten mode");
            TerrainBrushProfile.Settings settings;
            try{settings=new TerrainBrushProfile.Settings(radius,strength,target,inner,
                curve,spacing,rotation,min,max,slope,
                TerrainLayerStack.FlattenLimit.values()[direction]);}
            catch(IllegalArgumentException e){throw new IOException("Invalid brush settings",e);}
            int width=in.readInt(),height=in.readInt();
            if(width<0||height<0||width>MAX_SIDE||height>MAX_SIDE
                ||(width==0)!=(height==0)||(width!=0&&(width<2||height<2)))
                throw new IOException("Invalid alpha dimensions");
            TerrainBrushProfile.Alpha alpha=null;
            if(width!=0){
                float[][] pixels=new float[height][width];
                for(int z=0;z<height;z++)for(int x=0;x<width;x++)pixels[z][x]=in.readFloat();
                try{alpha=new TerrainBrushProfile.Alpha(pixels);}
                catch(IllegalArgumentException e){throw new IOException("Invalid alpha image",e);}
            }
            if(in.read()!=-1)throw new IOException("Trailing preset data");
            return new Preset(TerrainLayerStack.Brush.values()[brush],settings,alpha);
        }
    }
    public boolean delete(String name)throws IOException{return Files.deleteIfExists(path(name));}
}
