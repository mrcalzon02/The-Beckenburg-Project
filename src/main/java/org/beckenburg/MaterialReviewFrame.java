package org.beckenburg;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.beckenburg.editor.EditorMeshPipeline;

/** Visible native face and sampled source texel recorded by the material review rasterizer. */
final class MaterialReviewFrame {
    final BufferedImage image;
    private final long[] face;
    private final int[] texel;
    private long outlineId=-1;
    private BufferedImage outline;
    private final Map<Long,String> bindings=new HashMap<>();
    final boolean sourceTexels;
    MaterialReviewFrame(BufferedImage image,boolean sourceTexels){
        this.sourceTexels=sourceTexels;
        this.image=image;int n=Math.multiplyExact(image.getWidth(),image.getHeight());
        face=new long[n];texel=new int[n];Arrays.fill(face,-1);Arrays.fill(texel,-1);
    }
    void record(int pixel,long id,int address){face[pixel]=id;texel[pixel]=sourceTexels?address:-1;}
    int pixel(int x,int y){
        return x<0||y<0||x>=image.getWidth()||y>=image.getHeight()?-1:y*image.getWidth()+x;
    }
    long face(int pixel){return pixel<0?-1:face[pixel];}
    int texel(int pixel){return pixel<0?-1:texel[pixel];}
    void bindVisible(EditorMeshPipeline.Mesh source){
        long previous=-1;
        for(long id:face){
            if(id>=0 && id!=previous)bindings.putIfAbsent(id,"");
            previous=id;
        }
        for(int i=0;i<source.faceIds().size();i++){
            long id=source.faceIds().get(i);
            if(bindings.containsKey(id))bindings.put(id,source.triangles().get(i).material());
        }
    }
    boolean boundTo(long id,String key){return key.equals(bindings.get(id));}
    BufferedImage outline(long id){
        if(outline!=null && outlineId==id)return outline;
        int w=image.getWidth(),h=image.getHeight();outline=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){
            int i=y*w+x;
            if(face[i]==id && (x==0||y==0||x==w-1||y==h-1||face[i-1]!=id||face[i+1]!=id||face[i-w]!=id||face[i+w]!=id))
                outline.setRGB(x,y,0xffffdb50);
        }
        outlineId=id;return outline;
    }
}
