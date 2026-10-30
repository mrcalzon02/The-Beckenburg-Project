package org.beckenburg;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;

/** Only portable warehouse stock receives shelf plaques; one batched atlas/material. */
public final class CastleShelfLabels {
    private CastleShelfLabels(){}
    public static final double WIDTH=.60,HEIGHT=.19;
    private static final int COLUMNS=8,TILE_WIDTH=256,TILE_HEIGHT=96;
    public record Tile(int material,double u0,double v0,double u1,double v1){}
    private record Key(String id,EquipmentTiers.Tier tier){}
    private static Map<Key,Tile> tiles;
    private static volatile int labelMaterial=-1;
    /** Let the GPU smooth only thin plaque lettering; scenery stays PSX-nearest. */
    public static boolean isLabelMaterial(int material){return labelMaterial>=0&&material==labelMaterial;}
    public static String title(CastleWarehouse.Exhibit item){return item.id().replace('_',' ');}
    public static synchronized Tile tile(CastleWarehouse.Exhibit item){
        if(tiles==null)build();
        Tile result=tiles.get(new Key(item.id(),item.tier()));
        if(result==null)throw new IllegalArgumentException("Unknown shelf exhibit");
        return result;
    }
    private static void build(){
        var exhibits=CastleWarehouse.exhibits();
        int width=COLUMNS*TILE_WIDTH,height=((exhibits.size()+COLUMNS-1)/COLUMNS)*TILE_HEIGHT;
        var image=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
        var g=image.createGraphics();
        g.setColor(new Color(14,20,24));g.fillRect(0,0,width,height);
        g.setFont(new Font(Font.MONOSPACED,Font.BOLD,28));
        for(int i=0;i<exhibits.size();i++){
            int x=i%COLUMNS*TILE_WIDTH,y=i/COLUMNS*TILE_HEIGHT;
            var item=exhibits.get(i);String text=title(item);
            g.setColor(new Color(113,125,129));g.drawRect(x+1,y+1,TILE_WIDTH-3,TILE_HEIGHT-3);
            g.setColor(new Color(250,240,214));
            // Fit measured text width, preserving every word of the real item name.
            var lines=new ArrayList<String>();String line="";
            for(String word:text.split(" ")){
                String next=line.isEmpty()?word:line+" "+word;
                if(!line.isEmpty()&&g.getFontMetrics().stringWidth(next)>TILE_WIDTH-16){lines.add(line);line=word;}
                else line=next;
            }
            if(!line.isEmpty())lines.add(line);
            int lineHeight=28;
            for(int n=0;n<lines.size();n++)g.drawString(lines.get(n),x+8,y+30+n*lineHeight);
            g.setColor(item.tier()==EquipmentTiers.Tier.III?new Color(163,225,249):new Color(255,220,137));
            g.drawString("TIER "+item.tier().name(),x+8,y+85);
        }
        g.dispose();
        int material=GlbProps.surfaceMaterial("Castle retail label atlas v2",image);
        labelMaterial=material;
        var result=new HashMap<Key,Tile>();
        for(int i=0;i<exhibits.size();i++){
            int x=i%COLUMNS*TILE_WIDTH,y=i/COLUMNS*TILE_HEIGHT;
            var item=exhibits.get(i);
            result.put(new Key(item.id(),item.tier()),new Tile(material,
                (double)x/width,1.0-(double)(y+TILE_HEIGHT)/height,
                (double)(x+TILE_WIDTH)/width,1.0-(double)y/height));
        }
        tiles=Map.copyOf(result);
    }
}
