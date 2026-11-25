package org.beckenburg.editor;

import java.util.*;
import static org.beckenburg.editor.EditorTopologyProject.*;

/** Fixed-boundary harmonic relaxation of one connected, consistently oriented UV chart. */
final class EditorUvRelax {
    private final List<Face> faces;
    private final int[] island,root;
    private final boolean[] fixed;
    private final double[] x,y,weight,area;
    private final int width,height;

    EditorUvRelax(State state,EditorMeshTopology graph,int[] island,int width,int height){
        this.faces=state.faces();this.island=island;this.width=width;this.height=height;
        int count=island.length*3;
        var chart=new EditorUvChart(state,graph,island);root=chart.root;fixed=chart.boundary;
        x=new double[count];y=new double[count];weight=new double[count];area=new double[island.length];
        for(int i=0;i<count;i++){
            var face=faces.get(island[i/3]);
            var c=switch(i%3){case 0->face.a();case 1->face.b();default->face.c();};
            if(c.pinned())fixed[root[i]]=true;
        }
        boolean interior=false;
        for(int i=0;i<count;i++)if(!fixed[root[i]]){interior=true;break;}
        if(!interior)throw new IllegalArgumentException("This UV island has no interior vertices to relax");
        for(int i=0;i<count;i++){
            var f=faces.get(island[i/3]);var c=switch(i%3){case 0->f.a();case 1->f.b();default->f.c();};
            x[root[i]]=width*c.u();y[root[i]]=height*c.v();
            int edge=island[i/3]*3+i%3;
            var a=graph.vertices().get(graph.edges().get(edge).origin());
            var b=graph.vertices().get(graph.edges().get(graph.edges().get(edge).next()).origin());
            var p=graph.vertices().get(graph.edges().get(graph.edges().get(edge).previous()).origin());
            double ax=a.x()-p.x(),ay=a.y()-p.y(),az=a.z()-p.z(),bx=b.x()-p.x(),by=b.y()-p.y(),bz=b.z()-p.z();
            double cross=Math.hypot(Math.hypot(ay*bz-az*by,az*bx-ax*bz),ax*by-ay*bx);
            weight[i]=(ax*bx+ay*by+az*bz)/cross;
            if(!Double.isFinite(weight[i]))throw new IllegalArgumentException("Island contains degenerate geometry");
        }
        for(int i=0;i<area.length;i++){
            area[i]=signed(i,x,y);
            if(!Double.isFinite(area[i]) || area[i]==0 || Math.signum(area[i])!=Math.signum(area[0]))
                throw new IllegalArgumentException("Interior relaxation requires noncollapsed, consistently wound UVs");
        }
    }
    private static int next(int i){return i/3*3+(i%3+1)%3;}
    private double signed(int face,double[] u,double[] v){
        int a=root[3*face],b=root[3*face+1],c=root[3*face+2];
        return (u[b]-u[a])*(v[c]-v[a])-(v[b]-v[a])*(u[c]-u[a]);
    }
    private double energy(double[] u,double[] v){
        double result=0;
        for(int i=0;i<root.length;i++){
            int a=root[i],b=root[next(i)];double dx=u[b]-u[a],dy=v[b]-v[a];
            result+=weight[i]*(dx*dx+dy*dy);
        }
        return result;
    }
    private boolean valid(double[] u,double[] v){
        for(int i=0;i<island.length;i++){
            double ratio=signed(i,u,v)/area[i];
            if(!Double.isFinite(ratio)||ratio<=1e-8)return false;
        }
        return true;
    }
    List<Face> solve(){
        int n=root.length;
        double[] dx=new double[n],dy=new double[n],diagonal=new double[n],u=new double[n],v=new double[n];
        for(int i=0;i<n;i++){double w=Math.abs(weight[i]);diagonal[root[i]]+=w;diagonal[root[next(i)]]+=w;}
        double initial=energy(x,y),current=initial;
        if(!Double.isFinite(initial) || initial<=0)throw new IllegalArgumentException("Invalid island relaxation energy");
        for(int iteration=0;iteration<32;iteration++){
            if(Thread.currentThread().isInterrupted())throw new java.util.concurrent.CancellationException("UV relaxation interrupted");
            Arrays.fill(dx,0);Arrays.fill(dy,0);
            for(int i=0;i<n;i++){
                int a=root[i],b=root[next(i)];
                double du=weight[i]*(x[b]-x[a]),dv=weight[i]*(y[b]-y[a]);
                dx[a]+=du;dx[b]-=du;dy[a]+=dv;dy[b]-=dv;
            }
            for(int i=0;i<n;i++)if(fixed[i] || diagonal[i]==0){dx[i]=0;dy[i]=0;}
                else{dx[i]/=diagonal[i];dy[i]/=diagonal[i];}
            boolean advanced=false;
            for(double step=1;step>=1d/1024;step*=.5){
                for(int i=0;i<n;i++){u[i]=x[i]+step*dx[i];v[i]=y[i]+step*dy[i];}
                double candidate=energy(u,v);
                if(candidate<current*(1-1e-12) && valid(u,v)){
                    System.arraycopy(u,0,x,0,n);System.arraycopy(v,0,y,0,n);
                    current=candidate;advanced=true;break;
                }
            }
            if(!advanced)break;
        }
        if(current>=initial*(1-1e-10))throw new IllegalArgumentException("No interior stretch improvement with this boundary");
        // Recheck the values that can actually be stored in native corner UVs.
        for(int i=0;i<n;i++){
            if(fixed[i])continue;
            x[i]=(x[i]/width)*width;
            y[i]=(y[i]/height)*height;
        }
        if(!valid(x,y) || !(energy(x,y)<initial*(1-1e-10)))
            throw new IllegalArgumentException("Interior relaxation exceeds UV precision");
        var output=new ArrayList<>(faces);
        for(int i=0;i<island.length;i++){
            var f=faces.get(island[i]);var corners=new Corner[]{f.a(),f.b(),f.c()};
            boolean changed=false;
            for(int k=0;k<3;k++){
                int j=root[i*3+k];var c=corners[k];
                if(fixed[j])continue;
                double nu=x[j]/width,nv=y[j]/height;
                if(!Double.isFinite(nu)||!Double.isFinite(nv)||Math.abs(nu)>1e9||Math.abs(nv)>1e9)
                    throw new IllegalArgumentException("Relaxed UV exceeds native coordinate budget");
                if(nu!=c.u() || nv!=c.v()){corners[k]=new Corner(c.vertex(),nu,nv,c.normal());changed=true;}
            }
            if(changed)output.set(island[i],f.withCorners(corners[0],corners[1],corners[2]));
        }
        return output;
    }
}
