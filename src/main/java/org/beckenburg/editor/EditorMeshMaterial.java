package org.beckenburg.editor;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

/** Immutable material data owned by the native model, never a mutable runtime-cache handle. */
public final class EditorMeshMaterial {
    public enum Alpha { OPAQUE, MASK, BLEND }
    public enum Origin { BOTTOM_LEFT, TOP_LEFT }
    public record Color(double r,double g,double b,double a){public Color{for(double c:new double[]{r,g,b,a})if(!Double.isFinite(c)||c<0||c>1)throw new IllegalArgumentException("Invalid material color factor");}}
    private final String key,name;
    private final byte[] png;
    private final int width,height,wrapS,wrapT;
    private final Color color;
    private final Alpha alpha;
    private final double cutoff;
    private final Origin origin;
    private final Map<String,String> sourceProperties;
    public EditorMeshMaterial(String key,String name,byte[] png,Color color,int wrapS,int wrapT,Alpha alpha,double cutoff,Origin origin,Map<String,String> properties)throws IOException{
        if(key==null||!key.isEmpty()&&!key.matches("[A-Za-z0-9][A-Za-z0-9_.-]{0,95}")||name==null||name.isBlank()||name.length()>512||name.chars().anyMatch(Character::isISOControl))throw new IOException("Invalid material identity");
        if(png==null||png.length<33||png.length>4_000_000||!Arrays.equals(Arrays.copyOf(png,8),new byte[]{(byte)137,80,78,71,13,10,26,10})||ByteBuffer.wrap(png).getInt(8)!=13||ByteBuffer.wrap(png).getInt(12)!=0x49484452)throw new IOException("Material requires a bounded original PNG");
        int w=ByteBuffer.wrap(png).getInt(16),h=ByteBuffer.wrap(png).getInt(20);if(w<2||h<2||w>2048||h>2048)throw new IOException("Material PNG dimensions exceed actual runtime limits");
        var image=ImageIO.read(new ByteArrayInputStream(png));if(image==null||image.getWidth()!=w||image.getHeight()!=h)throw new IOException("Invalid PNG material image");
        if(!wrap(wrapS)||!wrap(wrapT)||!Double.isFinite(cutoff)||cutoff<0||cutoff>1)throw new IOException("Invalid material sampler/alpha cutoff");
        var props=new TreeMap<String,String>();if(properties==null||properties.size()>32)throw new IOException("Material property budget exceeded");
        for(var e:properties.entrySet()){String k=e.getKey(),v=e.getValue();if(k==null||!k.matches("[A-Za-z0-9_.-]{1,64}")||v==null||v.length()>4096||v.chars().anyMatch(c->Character.isISOControl(c)&&c!='\t'))throw new IOException("Invalid material provenance property");props.put(k,v);}
        this.key=key;this.name=name;this.png=png.clone();width=w;height=h;this.color=Objects.requireNonNull(color);this.alpha=Objects.requireNonNull(alpha);this.origin=Objects.requireNonNull(origin);this.wrapS=wrapS;this.wrapT=wrapT;this.cutoff=cutoff;sourceProperties=Collections.unmodifiableMap(props);
    }
    private static boolean wrap(int value){return value==10497||value==33071||value==33648;}
    public String key(){return key;}public String name(){return name;}public byte[] png(){return png.clone();}public int width(){return width;}public int height(){return height;}public int wrapS(){return wrapS;}public int wrapT(){return wrapT;}public Color color(){return color;}public Alpha alpha(){return alpha;}public double cutoff(){return cutoff;}public Origin origin(){return origin;}public Map<String,String> sourceProperties(){return sourceProperties;}
    @Override public String toString(){return name;}
    @Override public boolean equals(Object o){return o instanceof EditorMeshMaterial m&&key.equals(m.key)&&name.equals(m.name)&&Arrays.equals(png,m.png)&&color.equals(m.color)&&wrapS==m.wrapS&&wrapT==m.wrapT&&alpha==m.alpha&&cutoff==m.cutoff&&origin==m.origin&&sourceProperties.equals(m.sourceProperties);}
    @Override public int hashCode(){return Objects.hash(key,name,Arrays.hashCode(png),color,wrapS,wrapT,alpha,cutoff,origin,sourceProperties);}
    public static void validateTable(List<EditorMeshMaterial> materials){
        if(materials.size()>64)throw new IllegalArgumentException("Material count budget exceeded");var keys=new HashSet<String>();long bytes=0,pixels=0,props=0;
        for(var m:materials){if(m==null||m.key.isEmpty()||!keys.add(m.key))throw new IllegalArgumentException("Duplicate/missing material identity");bytes+=m.png.length;pixels+=(long)m.width*m.height;for(var value:m.sourceProperties.values())props+=value.length();}
        if(bytes>4_000_000||pixels>16_777_216||props>262144)throw new IllegalArgumentException("Native material memory budget exceeded");
    }
    static void writeTable(DataOutputStream out,List<EditorMeshMaterial> values)throws IOException{
        out.writeInt(values.size());for(var m:values){out.writeUTF(m.key);out.writeUTF(m.name);out.writeInt(m.png.length);out.write(m.png);out.writeDouble(m.color.r());out.writeDouble(m.color.g());out.writeDouble(m.color.b());out.writeDouble(m.color.a());out.writeInt(m.wrapS);out.writeInt(m.wrapT);out.writeUTF(m.alpha.name());out.writeDouble(m.cutoff);out.writeUTF(m.origin.name());out.writeInt(m.sourceProperties.size());for(var p:m.sourceProperties.entrySet()){out.writeUTF(p.getKey());out.writeUTF(p.getValue());}}
    }
    static List<EditorMeshMaterial> readTable(DataInputStream in)throws IOException{
        int count=in.readInt();if(count<0||count>64)throw new IOException("Invalid material count");var values=new ArrayList<EditorMeshMaterial>();long bytes=0,pixels=0;
        for(int i=0;i<count;i++){String key=in.readUTF(),name=in.readUTF();int size=in.readInt();if(size<33||size>4_000_000||(bytes+=size)>4_000_000)throw new IOException("Material image byte budget exceeded");byte[] png=in.readNBytes(size);if(png.length!=size)throw new EOFException("Truncated material PNG");int width=ByteBuffer.wrap(png).getInt(16),height=ByteBuffer.wrap(png).getInt(20);if(width<2||height<2||width>2048||height>2048||(pixels+=(long)width*height)>16_777_216)throw new IOException("Material decoded pixel budget exceeded");var color=new Color(in.readDouble(),in.readDouble(),in.readDouble(),in.readDouble());int s=in.readInt(),t=in.readInt();var alpha=Alpha.valueOf(in.readUTF());double cutoff=in.readDouble();var origin=Origin.valueOf(in.readUTF());int n=in.readInt();if(n<0||n>32)throw new IOException("Material property budget exceeded");var props=new LinkedHashMap<String,String>();for(int j=0;j<n;j++)if(props.putIfAbsent(in.readUTF(),in.readUTF())!=null)throw new IOException("Duplicate material property");values.add(new EditorMeshMaterial(key,name,png,color,s,t,alpha,cutoff,origin,props));}
        validateTable(values);return List.copyOf(values);
    }
    public static EditorMeshMaterial fromPng(String name,byte[] png,int s,int t,Alpha alpha,Origin origin)throws IOException{
        String suffix=name+"\n"+EditorMeshPipeline.sourceFingerprint(png)+s+":"+t+":"+alpha+":"+origin;
        String key="png-"+EditorMeshPipeline.sourceFingerprint(suffix.getBytes(java.nio.charset.StandardCharsets.UTF_8)).substring(0,24);
        return new EditorMeshMaterial(key,name,png,new Color(1,1,1,1),s,t,alpha,.5,origin,Map.of("sourcePngSha256",EditorMeshPipeline.sourceFingerprint(png)));
    }
    public static EditorMeshMaterial fallback(){return Fallback.VALUE;}
    private static final class Fallback{static final EditorMeshMaterial VALUE=create();private static EditorMeshMaterial create(){try{var image=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);for(int y=0;y<16;y++)for(int x=0;x<16;x++)image.setRGB(x,y,((x/4+y/4)&1)==0?0xff969c9d:0xff606d74);var bytes=new ByteArrayOutputStream();ImageIO.write(image,"png",bytes);return new EditorMeshMaterial("","Generated preview fallback",bytes.toByteArray(),new Color(1,1,1,1),10497,10497,Alpha.OPAQUE,.5,Origin.BOTTOM_LEFT,Map.of());}catch(IOException bad){throw new ExceptionInInitializerError(bad);}}}
}
