package org.beckenburg.editor;

import java.util.*;
import static org.beckenburg.editor.EditorTopologyProject.*;

/** Deterministic shelf packing of one material's native half-edge UV islands. */
final class EditorUvPack {
    record Result(List<Face> faces,int islands,double scale) {}
    private static final class Island {
        double minU=Double.POSITIVE_INFINITY,minV=minU,maxU=-minU,maxV=-minU,x,y;
        long first=Long.MAX_VALUE;
        boolean fixed;
        void include(Corner c){
            fixed|=c.pinned();
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
        var budget=new Budget();
        var fixed=islands.stream().filter(i->i.fixed).toList();
        var moving=islands.stream().filter(i->!i.fixed).toList();
        var bins=available(fixed,width,height,padding,budget);
        if(moving.isEmpty())throw new IllegalArgumentException("All material islands are pinned");
        double scale=1;
        if(!keepDensity){
            double hi=Double.POSITIVE_INFINITY;
            for(var i:moving)hi=Math.min(hi,Math.min((width-2d*padding)/((i.maxU-i.minU)*width),
                (height-2d*padding)/((i.maxV-i.minV)*height)));
            if(!Double.isFinite(hi) || hi<=0)throw new IllegalArgumentException("UV extent exceeds packing range");
            double lo=0;
            for(int step=0;step<52;step++){
                double mid=lo+(hi-lo)*.5;
                if(layout(moving,bins,width,height,padding,mid,budget))lo=mid;else hi=mid;
            }
            scale=lo;
            if(Math.abs(scale-1)<1e-12 && layout(moving,bins,width,height,padding,1,budget))scale=1;
        }
        if(!(scale>0) || !layout(moving,bins,width,height,padding,scale,budget))
            throw new IllegalArgumentException("Islands do not fit with this padding and density");
        var faces=new ArrayList<>(state.faces());
        for(int i=0;i<chart.length;i++){
            var island=chart[i];if(island==null || island.fixed)continue;
            var f=faces.get(i);
            var next=f.withCorners(place(f.a(),island,scale,width,height),place(f.b(),island,scale,width,height),
                place(f.c(),island,scale,width,height));
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
    private record Rect(double x0,double y0,double x1,double y1) {}
    private static final class Budget {
        private int remaining=16_000_000;
        void use(){if(--remaining<0)throw new IllegalArgumentException("Pinned atlas exceeds packing work budget");}
    }
    private static List<Rect> available(List<Island> fixed,int width,int height,int p,Budget budget){
        List<Rect> bins=List.of(new Rect(p,p,width-p,height-p));
        var occupied=new ArrayList<Rect>();
        for(var f:fixed){
            var r=new Rect(f.minU*width,f.minV*height,f.maxU*width,f.maxV*height);
            if(r.x0<p || r.y0<p || r.x1>width-p || r.y1>height-p)
                throw new IllegalArgumentException("Pinned island lies outside the requested atlas border");
            for(var old:occupied){
                budget.use();
                if(r.x0<old.x1+p && old.x0<r.x1+p && r.y0<old.y1+p && old.y0<r.y1+p)
                    throw new IllegalArgumentException("Pinned islands overlap or violate the requested gutter");
            }
            occupied.add(r);
            var remaining=new ArrayList<Rect>();
            for(var b:bins){
                budget.use();
                double x0=Math.max(r.x0-p,b.x0),x1=Math.min(r.x1+p,b.x1);
                double y0=Math.max(r.y0-p,b.y0),y1=Math.min(r.y1+p,b.y1);
                if(x0>=x1 || y0>=y1){remaining.add(b);continue;}
                if(b.y0<y0)remaining.add(new Rect(b.x0,b.y0,b.x1,y0));
                if(b.y1>y1)remaining.add(new Rect(b.x0,y1,b.x1,b.y1));
                double mid0=y0+(b.y0<y0?p:0),mid1=y1-(b.y1>y1?p:0);
                if(mid0<mid1 && b.x0<x0)remaining.add(new Rect(b.x0,mid0,x0,mid1));
                if(mid0<mid1 && b.x1>x1)remaining.add(new Rect(x1,mid0,b.x1,mid1));
            }
            bins=remaining;
        }
        return bins;
    }
    private static boolean layout(List<Island> islands,List<Rect> bins,int width,int height,int padding,double scale,Budget budget){
        var x=new double[bins.size()];var y=new double[bins.size()];var row=new double[bins.size()];
        for(int k=0;k<bins.size();k++){x[k]=bins.get(k).x0;y[k]=bins.get(k).y0;}
        for(var i:islands){
            double w=(i.maxU-i.minU)*width*scale,h=(i.maxV-i.minV)*height*scale;
            boolean placed=false;
            for(int k=0;k<bins.size();k++){
                budget.use();
                var bin=bins.get(k);if(w>bin.x1-bin.x0)continue;
                double px=x[k],py=y[k],shelf=row[k];
                if(px+w>bin.x1){px=bin.x0;py+=shelf+padding;shelf=0;}
                if(py+h>bin.y1)continue;
                i.x=px;i.y=py;x[k]=px+w+padding;y[k]=py;row[k]=Math.max(shelf,h);placed=true;break;
            }
            if(!placed)return false;
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
