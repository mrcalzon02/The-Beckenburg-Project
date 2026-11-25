package org.beckenburg.editor;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

/** Bottom-to-top authoring order. Layer IDs survive renames, movement and serialization. */
final class EditorPaintStack {
    static final int LIMIT=16;
    record Layer(String id,String name,EditorMeshMaterial.PaintStyle style,EditorPaintLayer pixels){
        Layer{
            if(id==null||!id.matches("[a-zA-Z0-9-]{1,36}")||name==null||name.isBlank()||name.length()>80||
                name.chars().anyMatch(Character::isISOControl))throw new IllegalArgumentException("Invalid paint layer identity");
            Objects.requireNonNull(style);Objects.requireNonNull(pixels);
        }
    }
    static final EditorPaintStack EMPTY=legacy(EditorPaintLayer.EMPTY,EditorMeshMaterial.PaintStyle.DEFAULT);
    final List<Layer> layers;
    private EditorPaintStack(List<Layer> layers){this.layers=List.copyOf(layers);}
    static EditorPaintStack legacy(EditorPaintLayer paint,EditorMeshMaterial.PaintStyle style){
        return new EditorPaintStack(List.of(new Layer("paint","Paint",style,paint)));
    }
    boolean legacy(){var first=layers.get(0);return layers.size()==1 && first.id().equals("paint") && first.name().equals("Paint");}
    int size(){int count=0;for(var layer:layers)count+=layer.pixels().size();return count;}
    Layer layer(String id){return layers.stream().filter(l->l.id().equals(id)).findFirst()
        .orElseThrow(()->new IllegalArgumentException("Unknown paint layer"));}
    private EditorPaintStack replace(Layer old,Layer next){
        if(old.equals(next))return this;
        var list=new ArrayList<>(layers);list.set(list.indexOf(old),next);return new EditorPaintStack(list);
    }
    EditorPaintStack pencil(String id,int width,int height,Collection<Integer> texels,int color){
        var old=layer(id);
        var pixels=old.pixels().stroke(width,height,texels,color);
        return pixels==old.pixels()?this:replace(old,new Layer(id,old.name(),old.style(),pixels));
    }
    EditorPaintStack configure(String id,String name,EditorMeshMaterial.PaintStyle style){
        var old=layer(id);return replace(old,new Layer(id,name.strip(),style,old.pixels()));
    }
    EditorPaintStack add(String above,String name){
        if(layers.size()>=LIMIT)throw new IllegalArgumentException("Material already has 16 paint layers");
        var list=new ArrayList<>(layers);
        list.add(list.indexOf(layer(above))+1,new Layer(UUID.randomUUID().toString(),name.strip(),
            EditorMeshMaterial.PaintStyle.DEFAULT,EditorPaintLayer.EMPTY));
        return new EditorPaintStack(list);
    }
    EditorPaintStack move(String id,int step){
        var list=new ArrayList<>(layers);
        int index=list.indexOf(layer(id));
        if(Math.abs(step)!=1 || index+step<0 || index+step>=list.size())throw new IllegalArgumentException("Layer cannot move farther");
        Collections.swap(list,index,index+step);return new EditorPaintStack(list);
    }
    EditorPaintStack remove(String id){
        if(layers.size()==1)throw new IllegalArgumentException("Keep at least one paint layer");
        var list=new ArrayList<>(layers);list.remove(layer(id));return new EditorPaintStack(list);
    }
    void draw(BufferedImage image){for(var l:layers)if(l.style().opacity()>0)l.pixels().draw(image,l.style());}
    boolean visible(){return layers.stream().anyMatch(l->l.pixels().size()>0 && l.style().opacity()>0);}
    long deltaBytes(EditorPaintStack next){
        if(this==next)return 0;
        var remaining=new HashMap<String,EditorPaintLayer>();
        for(var l:layers)remaining.put(l.id(),l.pixels());
        long bytes=0;
        for(var l:next.layers){
            var old=remaining.remove(l.id());
            bytes+=(old==null?EditorPaintLayer.EMPTY:old).deltaBytes(l.pixels());
        }
        for(var old:remaining.values())bytes+=old.deltaBytes(EditorPaintLayer.EMPTY);
        return bytes;
    }
    void write(DataOutputStream out)throws IOException{
        out.writeInt(layers.size());
        for(var l:layers){
            out.writeUTF(l.id());out.writeUTF(l.name());out.writeUTF(l.style().blend().name());out.writeByte(l.style().opacity());
            l.pixels().write(out);
        }
    }
    static EditorPaintStack read(DataInputStream in,int width,int height,int remaining)throws IOException{
        int n=in.readInt();
        if(n<1||n>LIMIT)throw new IOException("Invalid native paint layer count");
        var list=new ArrayList<Layer>();var ids=new HashSet<String>();
        for(int i=0;i<n;i++){
            String id=in.readUTF(),name=in.readUTF();
            if(!ids.add(id))throw new IOException("Duplicate paint layer ID");
            var style=new EditorMeshMaterial.PaintStyle(EditorMeshMaterial.Blend.valueOf(in.readUTF()),in.readUnsignedByte());
            var pixels=EditorPaintLayer.read(in,width,height,remaining);remaining-=pixels.size();
            list.add(new Layer(id,name,style,pixels));
        }
        return new EditorPaintStack(list);
    }
    @Override public boolean equals(Object other){return other instanceof EditorPaintStack p && layers.equals(p.layers);}
    @Override public int hashCode(){return layers.hashCode();}
}
