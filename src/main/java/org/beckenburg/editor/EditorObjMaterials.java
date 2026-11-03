package org.beckenburg.editor;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

/** Capture OBJ dependencies once; the native document owns the resulting immutable materials. */
public final class EditorObjMaterials {
    private EditorObjMaterials(){}
    public record Capture(EditorMeshPipeline.Mesh mesh,String sourceFingerprint,Map<Path,String> dependencies){
        public Capture{dependencies=Map.copyOf(dependencies);}
        public void validateDependencies()throws IOException{
            for(var entry:dependencies.entrySet())if(!EditorMeshPipeline.sourceFingerprint(EditorMeshPipeline.readSource(entry.getKey(),(int)EditorMeshPipeline.MAX_SOURCE_BYTES)).equals(entry.getValue()))throw new IOException("Reviewed material dependency changed: "+entry.getKey());
        }
    }
    public static Capture capture(Path file)throws IOException{return capture(file,file.toAbsolutePath().normalize().getParent());}
    public static Capture capture(Path file,Path assetRoot)throws IOException{
        byte[] source=EditorMeshPipeline.readSource(file,(int)EditorMeshPipeline.MAX_SOURCE_BYTES);String digest=EditorMeshPipeline.sourceFingerprint(source);
        if(file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".obj.zip"))return new Capture(EditorObjBundle.read(file.getFileName().toString(),source),digest,Map.of());
        if(!file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".obj"))return new Capture(EditorMeshPipeline.read(file.getFileName().toString(),source),digest,Map.of());
        var reader=new Reader(assetRoot);String text=new String(source,StandardCharsets.UTF_8);var materials=new ArrayList<EditorMeshMaterial>();var aliases=new LinkedHashMap<String,String>();
        for(String line:text.split("\\R")){String clean=clean(line);if(clean.startsWith("mtllib ")||clean.startsWith("mtllib\t")){
            Path library=reader.resolve(file.toAbsolutePath().normalize().getParent(),clean.substring(6).strip());
            parseLibrary(reader,library,materials,aliases);
        }}
        return new Capture(EditorMeshPipeline.obj(file.getFileName().toString(),text,source.length,materials,aliases),digest,reader.dependencies);
    }
    private static String clean(String value){int i=value.indexOf('#');return (i<0?value:value.substring(0,i)).strip();}
    private static final class Reader{
        final Path root;final Map<Path,String> dependencies=new LinkedHashMap<>();long total;
        Reader(Path root)throws IOException{this.root=root.toRealPath();}
        Path resolve(Path base,String name)throws IOException{
            if(name.isBlank()||name.indexOf('"')>=0||name.indexOf('\0')>=0)throw new IOException("Unsupported material dependency path");
            Path relative=Path.of(name);if(relative.isAbsolute())throw new IOException("Material dependency must be relative to the asset root");
            Path path=base.resolve(relative).normalize().toRealPath();if(!path.startsWith(root))throw new IOException("Material dependency escapes the asset root");return path;
        }
        byte[] read(Path file,int max)throws IOException{
            if(dependencies.size()>=128&&!dependencies.containsKey(file))throw new IOException("Material dependency count exceeded");
            byte[] raw=EditorMeshPipeline.readSource(file,max);if((total+=raw.length)>EditorMeshPipeline.MAX_SOURCE_BYTES)throw new IOException("Material dependency byte budget exceeded");
            String sha=EditorMeshPipeline.sourceFingerprint(raw),prior=dependencies.putIfAbsent(file,sha);if(prior!=null&&!prior.equals(sha))throw new IOException("Material dependency changed during import");return raw;
        }
    }
    private static final class Definition{
        String name,map;double r=1,g=1,b=1,a=1;boolean clamp,opacity;final Map<String,String> properties=new TreeMap<>();
    }
    private static void parseLibrary(Reader reader,Path library,List<EditorMeshMaterial> materials,Map<String,String> aliases)throws IOException{
        Definition d=null;int lineNumber=0;
        try{
            for(String raw:new String(reader.read(library,262144),StandardCharsets.UTF_8).split("\\R")){
                lineNumber++;String line=clean(raw);if(line.isEmpty())continue;String[] pair=line.split("\\s+",2);String command=pair[0],value=pair.length>1?pair[1]:"";
                if(command.equals("newmtl")){if(d!=null)finish(reader,library,d,materials,aliases);d=new Definition();d.name=value;continue;}
                if(d==null)throw new IOException("MTL property before newmtl");
                if(d.properties.putIfAbsent(command,value)!=null)throw new IOException("Duplicate MTL property: "+command);
                switch(command){
                    case "Kd"->{String[] c=value.split("\\s+");if(c.length!=3)throw new IOException("MTL Kd requires RGB values");d.r=component(c[0]);d.g=component(c[1]);d.b=component(c[2]);}
                    case "d","Tr"->{if(d.opacity)throw new IOException("Ambiguous MTL opacity");d.opacity=true;double opacity=component(value);d.a=command.equals("Tr")?1-opacity:opacity;}
                    case "map_Kd"->{if(value.startsWith("-clamp ")){String[] args=value.split("\\s+",3);if(args.length!=3||!Set.of("on","off").contains(args[1]))throw new IOException("Invalid MTL clamp");d.clamp=args[1].equals("on");value=args[2];}if(value.startsWith("-"))throw new IOException("Unsupported MTL texture transform/options");d.map=value;}
                    case "Ka","Ks","Ns","Ni","illum","Ke"->{/* Retain source metadata; current runtime uses diffuse shading. */}
                    default->throw new IOException("Unsupported MTL property: "+command);
                }
            }
            if(d!=null)finish(reader,library,d,materials,aliases);
        }catch(IllegalArgumentException invalid){throw new IOException("Invalid MTL at line "+lineNumber,invalid);}
    }
    private static double component(String value)throws IOException{double d=Double.parseDouble(value);if(!Double.isFinite(d)||d<0||d>1)throw new IOException("MTL color/opacity outside runtime range");return d;}
    private static void finish(Reader reader,Path library,Definition d,List<EditorMeshMaterial> materials,Map<String,String> aliases)throws IOException{
        if(materials.size()>=64||d.name.isBlank()||aliases.containsKey(d.name))throw new IOException("Duplicate/invalid MTL material or count budget exceeded");
        byte[] png;if(d.map!=null)png=reader.read(reader.resolve(library.getParent(),d.map),4_000_000);else{
            var image=new BufferedImage(2,2,BufferedImage.TYPE_INT_ARGB);for(int y=0;y<2;y++)for(int x=0;x<2;x++)image.setRGB(x,y,0xffffffff);var out=new ByteArrayOutputStream();ImageIO.write(image,"png",out);png=out.toByteArray();
        }
        var validated=new EditorMeshMaterial("validation",d.name,png,new EditorMeshMaterial.Color(d.r,d.g,d.b,d.a),10497,10497,EditorMeshMaterial.Alpha.OPAQUE,.5,EditorMeshMaterial.Origin.BOTTOM_LEFT,Map.of());
        if(materials.stream().mapToLong(m->(long)m.width()*m.height()).sum()+(long)validated.width()*validated.height()>16_777_216)throw new IOException("Imported texture pixel budget exceeded");
        var image=ImageIO.read(new ByteArrayInputStream(png));if(image==null)throw new IOException("MTL map_Kd must be a supported PNG");boolean transparent=d.a<1;
        for(int y=0;y<image.getHeight()&&!transparent;y++)for(int x=0;x<image.getWidth();x++)if(image.getRGB(x,y)>>>24!=255){transparent=true;break;}
        String identity=d.name+"\n"+d.properties+"\n"+EditorMeshPipeline.sourceFingerprint(png);String key="mtl-"+EditorMeshPipeline.sourceFingerprint(identity.getBytes(StandardCharsets.UTF_8)).substring(0,24);
        materials.add(new EditorMeshMaterial(key,d.name,png,new EditorMeshMaterial.Color(d.r,d.g,d.b,d.a),d.clamp?33071:10497,d.clamp?33071:10497,transparent?EditorMeshMaterial.Alpha.BLEND:EditorMeshMaterial.Alpha.OPAQUE,.5,EditorMeshMaterial.Origin.BOTTOM_LEFT,d.properties));aliases.put(d.name,key);
        try{EditorMeshMaterial.validateTable(materials);}catch(IllegalArgumentException bad){throw new IOException("Imported material budget exceeded",bad);}
    }
}
