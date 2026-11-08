package org.beckenburg.editor;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

/** Atomic OBJ/MTL/PNG artifact. Derived standard views must agree with canonical native material data. */
public final class EditorObjBundle {
    private static final int MAGIC=0x424d4154;
    private static final String FALLBACK="_native_fallback";
    private EditorObjBundle(){}
    public static void write(EditorTopologyProject.State state,OutputStream output)throws IOException{
        var aliases=new LinkedHashMap<String,String>();for(var m:state.materials())aliases.put(m.key(),m.key());aliases.put("",FALLBACK);
        var mesh=new ByteArrayOutputStream();EditorObjWriter.write(state,mesh,aliases,"materials.mtl");
        var entries=assets(state.materials(),state.faces().stream().anyMatch(f->f.material().isEmpty()));entries.put("mesh.obj",mesh.toByteArray());
        // Finish the archive without closing the document store's owned stream.
        var zip=new ZipOutputStream(output,StandardCharsets.UTF_8);for(var entry:entries.entrySet()){var item=new ZipEntry(entry.getKey());item.setTime(0);zip.putNextEntry(item);zip.write(entry.getValue());zip.closeEntry();}zip.finish();zip.flush();
    }
    private static Map<String,byte[]> assets(List<EditorMeshMaterial> materials,boolean fallback)throws IOException{
        var entries=new TreeMap<String,byte[]>();var binary=new ByteArrayOutputStream();var data=new DataOutputStream(binary);data.writeInt(MAGIC);int version=EditorMeshMaterial.hasStyles(materials)?3:EditorMeshMaterial.hasPaint(materials)?2:1;data.writeInt(version);EditorMeshMaterial.writeTable(data,materials);if(version>=2)EditorMeshMaterial.writePaint(data,materials);if(version>=3)EditorMeshMaterial.writeStyles(data,materials);data.flush();entries.put("materials.bin",binary.toByteArray());
        var definitions=new ArrayList<>(materials);if(fallback)definitions.add(EditorMeshMaterial.fallback());var mtl=new StringBuilder("# Native diffuse material views. Original PNG and provenance retained in materials.bin.\n");
        for(var m:definitions){String key=m.key().isEmpty()?FALLBACK:m.key();var color=m.color();mtl.append("newmtl ").append(key).append("\nKd ").append(color.r()).append(' ').append(color.g()).append(' ').append(color.b()).append("\nd ").append(m.alpha()==EditorMeshMaterial.Alpha.BLEND?color.a():1).append('\n');
            if(m.wrapS()!=m.wrapT()||m.wrapS()==33648)throw new IOException("Wavefront MTL cannot represent mirrored or differing axis wraps; retain native BTOP/GLB");
            for(var p:m.sourceProperties().entrySet())if(Set.of("Ka","Ks","Ns","Ni","illum","Ke").contains(p.getKey()))mtl.append(p.getKey()).append(' ').append(p.getValue()).append('\n');
            mtl.append("map_Kd -clamp ").append(m.wrapS()==33071?"on":"off").append(" textures/").append(key).append(".png\n\n");entries.put("textures/"+key+".png",standardTexture(m));
        }
        entries.put("materials.mtl",mtl.toString().getBytes(StandardCharsets.UTF_8));return entries;
    }
    private static byte[] standardTexture(EditorMeshMaterial m)throws IOException{
        if(m.origin()==EditorMeshMaterial.Origin.BOTTOM_LEFT&&m.alpha()==EditorMeshMaterial.Alpha.BLEND)return m.png();
        var original=ImageIO.read(new ByteArrayInputStream(m.png()));var image=new BufferedImage(m.width(),m.height(),BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<m.height();y++)for(int x=0;x<m.width();x++){int pixel=original.getRGB(x,m.origin()==EditorMeshMaterial.Origin.TOP_LEFT?m.height()-y-1:y);int alpha=pixel>>>24;if(m.alpha()==EditorMeshMaterial.Alpha.OPAQUE)alpha=255;else if(m.alpha()==EditorMeshMaterial.Alpha.MASK)alpha=alpha*m.color().a()/255>=m.cutoff()?255:0;image.setRGB(x,y,(alpha<<24)|(pixel&0xffffff));}
        var out=new ByteArrayOutputStream();ImageIO.write(image,"png",out);return out.toByteArray();
    }
    public static EditorMeshPipeline.Mesh read(String name,byte[] archive)throws IOException{
        if(archive.length==0||archive.length>EditorDocumentStore.MAX_BYTES)throw new IOException("OBJ bundle byte budget exceeded");var entries=new TreeMap<String,byte[]>();int total=0;
        try(var zip=new ZipInputStream(new ByteArrayInputStream(archive),StandardCharsets.UTF_8)){
            for(ZipEntry entry;(entry=zip.getNextEntry())!=null;){String path=entry.getName();if(entry.isDirectory()||entries.size()>=68||!path.matches("(?:mesh\\.obj|materials\\.(?:bin|mtl)|textures/[A-Za-z0-9_.-]{1,96}\\.png)"))throw new IOException("Invalid OBJ bundle entry");byte[] bytes=zip.readNBytes(EditorDocumentStore.MAX_BYTES-total+1);total+=bytes.length;if(total>EditorDocumentStore.MAX_BYTES||entries.putIfAbsent(path,bytes)!=null)throw new IOException("OBJ bundle duplicate/expanded byte budget exceeded");}
        }
        byte[] definitions=entries.get("materials.bin"),obj=entries.get("mesh.obj");if(definitions==null||obj==null)throw new IOException("OBJ bundle lacks canonical materials or geometry");
        List<EditorMeshMaterial> materials;try(var in=new DataInputStream(new ByteArrayInputStream(definitions))){if(in.readInt()!=MAGIC)throw new IOException("Invalid material bundle");int version=in.readInt();if(version<1||version>3)throw new IOException("Unsupported material bundle schema");try{materials=EditorMeshMaterial.readTable(in);if(version>=2)materials=EditorMeshMaterial.readPaint(in,materials);if(version>=3)materials=EditorMeshMaterial.readStyles(in,materials);}catch(IllegalArgumentException invalid){throw new IOException("Invalid native bundle materials",invalid);}if(in.read()!=-1)throw new IOException("Trailing material data");}
        var aliases=new HashMap<String,String>();for(var m:materials)aliases.put(m.key(),m.key());aliases.put(FALLBACK,"");String text=new String(obj,StandardCharsets.UTF_8);
        for(String line:text.split("\\R"))if(line.strip().startsWith("mtllib")&&!line.strip().equals("mtllib materials.mtl"))throw new IOException("Foreign OBJ bundle material library");
        var mesh=EditorMeshPipeline.obj(name,text,obj.length,materials,aliases);var expected=assets(materials,mesh.triangles().stream().anyMatch(t->t.material().isEmpty()));expected.put("mesh.obj",obj);
        if(!entries.keySet().equals(expected.keySet()))throw new IOException("Unexpected/missing OBJ material bundle views");for(var entry:expected.entrySet())if(!Arrays.equals(entry.getValue(),entries.get(entry.getKey())))throw new IOException("OBJ material view disagrees with canonical data: "+entry.getKey());return mesh;
    }
    public static void validate(EditorTopologyProject.State state,byte[] archive)throws IOException{EditorObjWriter.validate(state,read("export.obj.zip",archive));}
}
