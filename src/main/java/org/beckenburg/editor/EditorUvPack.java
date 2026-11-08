package org.beckenburg.editor;

import java.util.*;
import static org.beckenburg.editor.EditorTopologyProject.*;

/** Deterministic shelf packing of one material's native half-edge UV islands. */
final class EditorUvPack {
    record Result(List<Face> faces,int islands,double scale) {}
    private static final class Island {
        double minU=Double.POSITIVE_INFINITY,minV=minU,maxU=-minU,maxV=-minU,x,y;
        long first=Long.MAX_VALUE;
        void include(Corner c){
            minU=Math.min(minU,c.u());minV=Math.min(minV,c.v());maxU=Math.max(maxU,c.u());maxV=Math.max(maxV,c.v());
        }
    }
    static Result pack(State state,EditorMeshTopology graph,EditorMeshMaterial material,int padding,boolean keepDensity){
        int width=material.width(),height=material.height();
        if(padding<0 || 2L*padding>=Math.min(width,height))throw new IllegalArgumentException("Padding leaves no texture space");
        int[] queue=new int[state.faces().size()];
        var chart=new Island[queue.length];
        var islands=new ArrayList<Island>();
        for(int start=0;start<chart.length;start++){
            if(chart[start]!=null || !state.faces().get(start).material().equals(material.key()))continue;
            var island=new Island();islands.add(island);
            int count=1;queue[0]=start;chart[start]=island;
            for(int head=0;head<count;head++){
                int index=queue[head];var f=state.faces().get(index);
                double area=area(f);
                if(!Double.isFinite(area)||area==0)throw new IllegalArgumentException("Unwrap collapsed UV faces before packing");
                island.first=Math.min(island.first,f.id());island.include(f.a());island.include(f.b());island.include(f.c());
                for(int i=0;i<3;i++){
                    int other=EditorTopologyProject.uvNeighbor(state,graph,index,i);
                    if(other>=0 && chart[other]==null){chart[other]=island;queue[count++]=other;}
                }
            }
        }
        islands.sort(Comparator.<Island>comparingDouble(i->i.maxV-i.minV).reversed()
            .thenComparing(Comparator.<Island>comparingDouble(i->i.maxU-i.minU).reversed()).thenComparingLong(i->i.first));
        double scale=1;
        if(!keepDensity){
            double hi=Double.POSITIVE_INFINITY;
            for(var i:islands)hi=Math.min(hi,Math.min((width-2d*padding)/((i.maxU-i.minU)*width),
                (height-2d*padding)/((i.maxV-i.minV)*height)));
            if(!Double.isFinite(hi) || hi<=0)throw new IllegalArgumentException("UV extent exceeds packing range");
            double lo=0;
            for(int step=0;step<52;step++){
                double mid=lo+(hi-lo)*.5;
                if(layout(islands,width,height,padding,mid))lo=mid;else hi=mid;
            }
            scale=lo;
            if(Math.abs(scale-1)<1e-12 && layout(islands,width,height,padding,1))scale=1;
        }
        if(!(scale>0) || !layout(islands,width,height,padding,scale))
            throw new IllegalArgumentException("Islands do not fit with this padding and density");
        var faces=new ArrayList<>(state.faces());
        for(int i=0;i<chart.length;i++){
            var island=chart[i];if(island==null)continue;
            var f=faces.get(i);
            var next=new Face(f.id(),f.part(),place(f.a(),island,scale,width,height),place(f.b(),island,scale,width,height),
                place(f.c(),island,scale,width,height),f.material());
            double ratio=area(next)/area(f)/scale/scale;
            if(!Double.isFinite(ratio)||Math.abs(ratio-1)>1e-7)throw new IllegalArgumentException("Packed face exceeds UV precision");
            faces.set(i,next);
        }
        if(padding==0)for(int i=0;i<chart.length;i++)if(chart[i]!=null)for(int j=0;j<3;j++){
            var edge=graph.edges().get(i*3+j);
            if(!edge.uvSeam() || edge.twin()<i*3+j)continue;
            int twin=edge.twin();
            if(chart[twin/3]==null)continue;
            var f=faces.get(i);var g=faces.get(twin/3);
            if(sameUv(corner(f,j),corner(g,(twin%3+1)%3)) && sameUv(corner(f,(j+1)%3),corner(g,twin%3)))
                throw new IllegalArgumentException("Use a positive gutter to preserve the UV seam");
        }
        return new Result(faces,islands.size(),scale);
    }
    private static Corner corner(Face f,int i){return i==0?f.a():i==1?f.b():f.c();}
    private static boolean sameUv(Corner a,Corner b){return a.u()==b.u() && a.v()==b.v();}
    private static boolean layout(List<Island> islands,int width,int height,int padding,double scale){
        double x=padding,y=padding,row=0;
        for(var i:islands){
            double w=(i.maxU-i.minU)*width*scale,h=(i.maxV-i.minV)*height*scale;
            if(w>width-2d*padding)return false;
            if(x+w>width-padding){x=padding;y+=row+padding;row=0;}
            if(y+h>height-padding)return false;
            i.x=x;i.y=y;x+=w+padding;row=Math.max(row,h);
        }
        return true;
    }
    private static Corner place(Corner c,Island i,double scale,int width,int height){
        return new Corner(c.vertex(),i.x/width+(c.u()-i.minU)*scale,i.y/height+(c.v()-i.minV)*scale,c.normal());
    }
    private static double area(Face f){
        return (f.b().u()-f.a().u())*(f.c().v()-f.a().v())-(f.b().v()-f.a().v())*(f.c().u()-f.a().u());
    }
}
