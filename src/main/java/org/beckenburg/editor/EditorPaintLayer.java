package org.beckenburg.editor;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

/** Immutable sparse base-color overlay. A transaction copies each touched tile once. */
final class EditorPaintLayer {
    private static final int SIDE=16, COUNT=SIDE*SIDE;
    static final int MAX_TILES=2048;
    static final EditorPaintLayer EMPTY=new EditorPaintLayer(Map.of());
    private final Map<Integer,int[]> tiles;

    private EditorPaintLayer(Map<Integer,int[]> tiles){this.tiles=Collections.unmodifiableMap(tiles);}
    int size(){return tiles.size();}
    long deltaBytes(EditorPaintLayer after) {
        if(this==after)return 0;
        int changed=0;
        for(var entry:tiles.entrySet())if(entry.getValue()!=after.tiles.get(entry.getKey()))changed++;
        for(var entry:after.tiles.entrySet())if(entry.getValue()!=tiles.get(entry.getKey()))changed++;
        return (long)changed*COUNT*Integer.BYTES;
    }
    EditorPaintLayer stroke(int width,int height,Collection<Integer> texels,int argb) {
        var changed=new HashMap<Integer,int[]>();
        int columns=(width+SIDE-1)/SIDE;
        for (int index:texels) {
            if (index<0 || index>=width*height) throw new IllegalArgumentException("Paint texel outside material");
            int x=index%width,y=index/width,key=(y/SIDE)*columns+x/SIDE;
            int[] tile=changed.computeIfAbsent(key,k->tiles.containsKey(k)?tiles.get(k).clone():new int[COUNT]);
            tile[(y%SIDE)*SIDE+x%SIDE]=argb;
        }
        var next=new TreeMap<>(tiles);
        for (var item:changed.entrySet()) {
            var tile=item.getValue();
            if (Arrays.equals(tile,tiles.get(item.getKey()))) continue;
            if (Arrays.stream(tile).allMatch(p->p==0)) next.remove(item.getKey());
            else next.put(item.getKey(),tile);
        }
        if (next.size()>MAX_TILES) throw new IllegalArgumentException("Material paint tile budget exceeded");
        return next.equals(tiles)?this:new EditorPaintLayer(next);
    }
    void draw(BufferedImage image,EditorMeshMaterial.PaintStyle style) {
        int columns=(image.getWidth()+SIDE-1)/SIDE;
        for (var entry:tiles.entrySet()) {
            int x0=(entry.getKey()%columns)*SIDE,y0=(entry.getKey()/columns)*SIDE;
            for (int y=0;y<SIDE && y+y0<image.getHeight();y++) for(int x=0;x<SIDE && x+x0<image.getWidth();x++){
                int color=entry.getValue()[y*SIDE+x];
                if (color!=0) image.setRGB(x0+x,y0+y,composite(image.getRGB(x0+x,y0+y),color,style));
            }
        }
    }
    private static int composite(int base,int ink,EditorMeshMaterial.PaintStyle style) {
        double opacity=style.opacity()/100d,backdrop=(base>>>24)/255d;
        double alpha=opacity+backdrop*(1-opacity);
        if(alpha==0)return 0;
        int result=(int)Math.round(alpha*255)<<24;
        for(int shift=0;shift<=16;shift+=8){
            int b=(base>>>shift)&255,s=(ink>>>shift)&255;
            double blended=switch(style.blend()){
                case NORMAL -> s;
                case ADD -> Math.min(255,b+s);
                case MULTIPLY -> b*s/255d;
                case SCREEN -> 255-(255-b)*(255-s)/255d;
            };
            double value=((1-opacity)*backdrop*b + opacity*((1-backdrop)*s+backdrop*blended))/alpha;
            result|=(int)Math.round(value)<<shift;
        }
        return result;
    }
    void write(DataOutputStream out)throws IOException {
        out.writeInt(tiles.size());
        for (var item:tiles.entrySet()){out.writeInt(item.getKey());for(int p:item.getValue())out.writeInt(p);}
    }
    static EditorPaintLayer read(DataInputStream in,int width,int height,int remaining)throws IOException {
        int columns=(width+SIDE-1)/SIDE, rows=(height+SIDE-1)/SIDE;
        int count=in.readInt();
        if(count<0 || count>Math.min(remaining,columns*rows))throw new IOException("Invalid paint tile count");
        var tiles=new TreeMap<Integer,int[]>();
        for(int i=0;i<count;i++){
            int key=in.readInt();
            if(key<0 || key>=columns*rows || tiles.containsKey(key))throw new IOException("Invalid paint tile key");
            var tile=new int[COUNT];
            boolean occupied=false;
            for(int j=0;j<COUNT;j++){
                int color=in.readInt();tile[j]=color;
                if(color==0)continue;
                if((color>>>24)!=255 || (key%columns)*SIDE+j%SIDE>=width || (key/columns)*SIDE+j/SIDE>=height)
                    throw new IOException("Invalid pencil tile texel");
                occupied=true;
            }
            if(!occupied)throw new IOException("Empty paint tile");
            tiles.put(key,tile);
        }
        return count==0?EMPTY:new EditorPaintLayer(tiles);
    }
    @Override public boolean equals(Object other) {
        if(this==other)return true;
        if(!(other instanceof EditorPaintLayer layer) || tiles.size()!=layer.tiles.size())return false;
        for(var item:tiles.entrySet())if(!Arrays.equals(item.getValue(),layer.tiles.get(item.getKey())))return false;
        return true;
    }
    @Override public int hashCode(){
        int hash=0;
        for(var item:tiles.entrySet())hash+=item.getKey()^Arrays.hashCode(item.getValue());
        return hash;
    }
}
