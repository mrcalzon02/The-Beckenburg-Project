package org.beckenburg.editor;

import java.util.*;
import static org.beckenburg.editor.EditorTopologyProject.*;

/** Extend a projected stroke into unused atlas texels closer to that stroke than to other sampled surfaces. */
final class EditorPaintPadding {
    private final EditorMeshMaterial material;
    private final int w,h;
    private int work=16_000_000;
    EditorPaintPadding(EditorMeshMaterial material){this.material=material;w=material.width();h=material.height();}

    Collection<Integer> expand(List<Face> faces,Set<Integer> seeds,int radius){
        if(radius<0||radius>16)throw new IllegalArgumentException("Projection padding must be 0 to 16 texels");
        if(radius==0)return seeds;
        var ink=new BitSet(w*h);for(int p:seeds)ink.set(p);
        var guard=new BitSet(w*h);byte[] inkDistance=rings(ink,2*radius,guard);
        var occupied=new BitSet(w*h);
        for(var face:faces)if(face.material().equals(material.key()))cover(face,guard,occupied);
        occupied.andNot(ink);
        var offsets=new ArrayList<Offset>();
        for(int y=-radius;y<=radius;y++)for(int x=-radius;x<=radius;x++){
            int d=x*x+y*y;if(d>0&&d<=radius*radius)offsets.add(new Offset(x,y,d));
        }
        offsets.sort(Comparator.comparingInt(Offset::squared));
        var result=new ArrayList<>(seeds);
        for(int p=guard.nextSetBit(0);p>=0;p=guard.nextSetBit(p+1)){
            int distance=inkDistance[p];
            if(distance<2 || distance>radius+1 || occupied.get(p) || !closestToInk(p,offsets,ink,occupied))continue;
            if(result.size()>=65536)throw new IllegalArgumentException("Paint and padding exceed 65536 texels");
            result.add(p);
        }
        return result;
    }
    private record Offset(int x,int y,int squared){}
    private boolean closestToInk(int pixel,List<Offset> offsets,BitSet ink,BitSet occupied){
        int closest=Integer.MAX_VALUE;
        for(var offset:offsets){
            if(offset.squared>closest)break;
            budget();int x=pixel%w+offset.x,y=pixel/w+offset.y;
            if(x<0||x>=w){if(material.wrapS()!=10497)continue;x=Math.floorMod(x,w);}
            if(y<0||y>=h){if(material.wrapT()!=10497)continue;y=Math.floorMod(y,h);}
            int n=y*w+x;if(occupied.get(n))return false;
            if(ink.get(n))closest=offset.squared;
        }
        return closest!=Integer.MAX_VALUE;
    }
    // Eight-connected rings bound the candidate region. The final distance and ties are Euclidean.
    private byte[] rings(BitSet sources,int radius,BitSet seen){
        byte[] distance=new byte[w*h];
        BitSet front=(BitSet)sources.clone(),next=new BitSet(w*h);
        seen.or(front);
        for(int d=0;d<=radius && !front.isEmpty();d++){
            for(int p=front.nextSetBit(0);p>=0;p=front.nextSetBit(p+1)){
                budget();distance[p]=(byte)(d+1);
                if(d==radius)continue;
                int x=p%w,y=p/w;
                for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++){
                    int nx=x+dx,ny=y+dy;
                    if(nx<0||nx>=w){if(material.wrapS()!=10497)continue;nx=nx<0?w-1:0;}
                    if(ny<0||ny>=h){if(material.wrapT()!=10497)continue;ny=ny<0?h-1:0;}
                    int n=ny*w+nx;
                    if(!seen.get(n)){seen.set(n);next.set(n);}
                }
            }
            var swap=front;front=next;next=swap;next.clear();
        }
        return distance;
    }
    private void budget(){
        if(--work<0)throw new IllegalArgumentException("Projection padding exceeds the UV coverage budget");
        if((work&4095)==0 && Thread.currentThread().isInterrupted())throw new java.util.concurrent.CancellationException("Paint padding interrupted");
    }
    private static int tile(double coordinate,int wrap,boolean first){
        if(wrap==33071)return 0;
        if(Math.abs(coordinate)>1e9)throw new IllegalArgumentException("UV tile exceeds projection padding range");
        int n=(int)Math.floor(coordinate);
        return first && coordinate==n?n-1:n;
    }
    private void cover(Face f,BitSet guard,BitSet occupied){
        var corners=new Corner[]{f.a(),f.b(),f.c()};
        double minU=Math.min(f.a().u(),Math.min(f.b().u(),f.c().u())),maxU=Math.max(f.a().u(),Math.max(f.b().u(),f.c().u()));
        double minV=Math.min(f.a().v(),Math.min(f.b().v(),f.c().v())),maxV=Math.max(f.a().v(),Math.max(f.b().v(),f.c().v()));
        int u0=tile(minU,material.wrapS(),true),u1=tile(maxU,material.wrapS(),false);
        int v0=tile(minV,material.wrapT(),true),v1=tile(maxV,material.wrapT(),false);
        if((long)(u1-u0+1)*(v1-v0+1)>work)throw new IllegalArgumentException("Too many repeated UV tiles for projection padding");
        double[] x=new double[3],y=new double[3];
        for(int v=v0;v<=v1;v++)for(int u=u0;u<=u1;u++){
            budget();var tile=new EditorUvTile(u,v);
            for(int k=0;k<3;k++){x[k]=w*tile.imageU(corners[k].u(),material);y[k]=h*tile.imageV(corners[k].v(),material);}
            coverTile(x,y,guard,occupied);
        }
    }
    private static int clip(double pixel,int extent){return (int)Math.max(0,Math.min(extent-1,pixel));}
    private void coverTile(double[] x,double[] y,BitSet guard,BitSet occupied){
        double minX=Math.min(x[0],Math.min(x[1],x[2])),maxX=Math.max(x[0],Math.max(x[1],x[2]));
        double minY=Math.min(y[0],Math.min(y[1],y[2])),maxY=Math.max(y[0],Math.max(y[1],y[2]));
        int left=clip(Math.ceil(minX)-1,w),right=clip(Math.floor(maxX),w);
        int bottom=clip(Math.floor(maxY),h);
        for(int row=clip(Math.ceil(minY)-1,h);row<=bottom;row++){
            budget();int end=row*w+right;
            for(int p=guard.nextSetBit(row*w+left);p>=0&&p<=end;p=guard.nextSetBit(p+1)){
                if(occupied.get(p))continue;
                budget();int col=p%w;
                double x0=col==0&&material.wrapS()==33071?Math.min(col,minX):col;
                double x1=col==w-1&&material.wrapS()==33071?Math.max(col+1,maxX):col+1;
                double y0=row==0&&material.wrapT()==33071?Math.min(row,minY):row;
                double y1=row==h-1&&material.wrapT()==33071?Math.max(row+1,maxY):row+1;
                if(x0<=maxX && x1>=minX && y0<=maxY && y1>=minY && intersects(x,y,x0,x1,y0,y1))occupied.set(p);
            }
        }
    }
    // Test the texel footprint against all three edge normals as well as the bounding box above.
    // This includes thin and collapsed UVs that cover no texel center but can be sampled by the renderer.
    private static boolean intersects(double[] x,double[] y,double left,double right,double top,double bottom){
        for(int i=0;i<3;i++){
            int j=(i+1)%3,k=(i+2)%3;double nx=y[i]-y[j],ny=x[j]-x[i];
            double third=nx*(x[k]-x[i])+ny*(y[k]-y[i]);
            double xa=nx*(left-x[i]),xb=nx*(right-x[i]),ya=ny*(top-y[i]),yb=ny*(bottom-y[i]);
            double lo=Math.min(xa,xb)+Math.min(ya,yb),hi=Math.max(xa,xb)+Math.max(ya,yb);
            double tolerance=1e-12*Math.max(1,Math.max(Math.abs(third),Math.max(Math.abs(lo),Math.abs(hi))));
            if(lo>Math.max(0,third)+tolerance || hi<Math.min(0,third)-tolerance)return false;
        }
        return true;
    }
}
