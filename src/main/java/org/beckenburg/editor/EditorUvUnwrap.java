package org.beckenburg.editor;

import java.util.*;
import static org.beckenburg.editor.EditorTopologyProject.*;

/** Least-squares conformal map of one cut disk, in texture pixels. */
final class EditorUvUnwrap {
    record Result(List<Face> faces,String angleReport){}
    private final State state;
    private final EditorMeshTopology graph;
    private final int[] island;
    private final EditorUvChart chart;
    private final int width,height;
    private final boolean[] fixed;
    private final double[] real,imag,diagonal,uv;
    private final double orientation,offsetU,offsetV;

    EditorUvUnwrap(State state,EditorMeshTopology graph,int[] island,int width,int height){
        this.state=state;this.graph=graph;this.island=island;this.width=width;this.height=height;
        Arrays.sort(island);
        chart=new EditorUvChart(state,graph,island);
        int n=chart.root.length;
        fixed=new boolean[n];real=new double[n];imag=new double[n];diagonal=new double[n];uv=new double[2*n];
        int vertices=0,boundaries=0;
        double signed=0;
        int[] degree=new int[n];
        for(int i=0;i<n;i++){
            var f=state.faces().get(island[i/3]);var c=corner(f,i%3);int j=chart.root[i];
            uv[2*j]=c.u()*width;uv[2*j+1]=c.v()*height;
            fixed[j]|=c.pinned();
            if(i==j)vertices++;
            if(chart.cut[i]){degree[j]++;degree[chart.root[EditorUvChart.next(i)]]++;boundaries++;}
            if(i%3==0)signed+=(f.b().u()-f.a().u())*(f.c().v()-f.a().v())-(f.b().v()-f.a().v())*(f.c().u()-f.a().u());
        }
        if(boundaries==0 || 2*vertices-(n+boundaries)+2*island.length!=2)
            throw new IllegalArgumentException("Mark seams to make the selected chart a disk");
        for(int count:degree)if(count!=0 && count!=2)throw new IllegalArgumentException("Repair the branching chart boundary");
        orientation=signed<0?-1:1;
        for(int i=0;i<n;i+=3)setCoefficients(i);
        anchor();
        int first=0;while(!fixed[first])first++;
        offsetU=uv[2*first];offsetV=uv[2*first+1];
        for(int i=0;i<n;i++){uv[2*i]-=offsetU;uv[2*i+1]-=offsetV;}
        initializeFromPlane(first);
    }
    private static Corner corner(Face f,int k){return k==0?f.a():k==1?f.b():f.c();}
    private EditorMeshTopology.Vertex point(int i){
        return graph.vertices().get(graph.edges().get(3*island[i/3]+i%3).origin());
    }
    // The complex edge opposite each corner, divided by sqrt(twice the 3D area),
    // gives the two Cauchy-Riemann residuals for this triangle.
    private void setCoefficients(int i){
        var a=point(i);var b=point(i+1);var c=point(i+2);
        double bx=b.x()-a.x(),by=b.y()-a.y(),bz=b.z()-a.z(),cx=c.x()-a.x(),cy=c.y()-a.y(),cz=c.z()-a.z();
        double length=Math.hypot(Math.hypot(bx,by),bz);
        double area=Math.hypot(Math.hypot(by*cz-bz*cy,bz*cx-bx*cz),bx*cy-by*cx);
        double projection=(bx*cx+by*cy+bz*cz)/length,altitude=orientation*area/length;
        if(!Double.isFinite(projection+altitude+area)||area<=1e-14*length*Math.hypot(Math.hypot(cx,cy),cz))
            throw new IllegalArgumentException("Degenerate triangle cannot be unwrapped");
        double weight=1/Math.sqrt(area);
        real[i]=(projection-length)*weight;real[i+1]=-projection*weight;real[i+2]=length*weight;
        imag[i]=altitude*weight;imag[i+1]=-imag[i];
        for(int k=i;k<i+3;k++)diagonal[chart.root[k]]+=real[k]*real[k]+imag[k]*imag[k];
    }
    private int farthestBoundary(int from){
        var a=point(from);double best=0;int selected=-1;
        for(int i=0;i<fixed.length;i++)if(chart.boundary[i]){
            var b=point(i);double distance=Math.hypot(Math.hypot(a.x()-b.x(),a.y()-b.y()),a.z()-b.z());
            if(distance>best){selected=i;best=distance;}
        }
        if(selected<0)throw new IllegalArgumentException("Chart has no distinct boundary anchors");
        return selected;
    }
    private void anchor(){
        int first=-1,count=0;
        for(int i=0;i<fixed.length;i++)if(fixed[i]){
            if(first<0)first=i;
            count++;
        }
        if(count>=2){
            for(int i=0;i<fixed.length;i++)if(fixed[i] && (uv[2*i]!=uv[2*first]||uv[2*i+1]!=uv[2*first+1]))return;
            throw new IllegalArgumentException("Pinned UVs need two distinct positions");
        }
        if(first<0){
            int start=0;while(!chart.boundary[start])start++;
            first=farthestBoundary(start);
        }
        int second=farthestBoundary(first);
        fixed[first]=true;fixed[second]=true;
        if(uv[2*first]==uv[2*second] && uv[2*first+1]==uv[2*second+1])uv[2*second]=uv[2*first]+width;
    }
    /** A similarity projection is the exact solution for a flat chart with compatible pins. */
    private void initializeFromPlane(int first){
        var a=point(first);int second=-1;double length=0;
        for(int i=0;i<fixed.length;i++)if(fixed[i] && (uv[2*i]!=0 || uv[2*i+1]!=0)){
            var b=point(i);double distance=Math.hypot(Math.hypot(b.x()-a.x(),b.y()-a.y()),b.z()-a.z());
            if(distance>length){second=i;length=distance;}
        }
        if(second<0)return;
        var b=point(second);
        double ex=(b.x()-a.x())/length,ey=(b.y()-a.y())/length,ez=(b.z()-a.z())/length;
        for(int k=0;k<chart.root.length;k+=3){
            var p=point(k);var q=point(k+1);var r=point(k+2);
            double dx=q.x()-p.x(),dy=q.y()-p.y(),dz=q.z()-p.z(),fx=r.x()-p.x(),fy=r.y()-p.y(),fz=r.z()-p.z();
            double nx=dy*fz-dz*fy,ny=dz*fx-dx*fz,nz=dx*fy-dy*fx;
            double bx=ny*ez-nz*ey,by=nz*ex-nx*ez,bz=nx*ey-ny*ex;
            double size=Math.hypot(Math.hypot(bx,by),bz);
            if(size<=1e-10*Math.hypot(Math.hypot(nx,ny),nz))continue;
            var guess=uv.clone();
            for(int i=0;i<fixed.length;i++)if(chart.root[i]==i && !fixed[i]){
                var v=point(i);
                double x=v.x()-a.x(),y=v.y()-a.y(),z=v.z()-a.z();
                double along=(ex*x+ey*y+ez*z)/length,across=orientation*(bx*x+by*y+bz*z)/size/length;
                guess[2*i]=uv[2*second]*along-uv[2*second+1]*across;
                guess[2*i+1]=uv[2*second+1]*along+uv[2*second]*across;
            }
            if(normal(guess,null)<normal(uv,null))System.arraycopy(guess,0,uv,0,uv.length);
            return;
        }
    }
    // Multiply by A^T A without constructing a dense matrix.
    private double normal(double[] values,double[] result){
        if(result!=null)Arrays.fill(result,0);
        double energy=0;
        for(int i=0;i<chart.root.length;i+=3){
            double x=0,y=0;
            for(int k=i;k<i+3;k++){
                int j=2*chart.root[k];x+=real[k]*values[j]-imag[k]*values[j+1];
                y+=imag[k]*values[j]+real[k]*values[j+1];
            }
            energy+=x*x+y*y;
            if(result==null)continue;
            for(int k=i;k<i+3;k++){
                int j=chart.root[k];if(fixed[j])continue;
                result[2*j]+=real[k]*x+imag[k]*y;result[2*j+1]+=-imag[k]*x+real[k]*y;
            }
        }
        return energy;
    }
    private double norm(double[] residual){
        double sum=0;
        for(int i=0;i<fixed.length;i++)if(chart.root[i]==i && !fixed[i])
            sum+=(residual[2*i]*residual[2*i]+residual[2*i+1]*residual[2*i+1])/diagonal[i];
        return sum;
    }
    private void optimize(){
        var residual=new double[uv.length];var direction=new double[uv.length];var product=new double[uv.length];
        normal(uv,residual);
        for(int j=0;j<uv.length;j++){
            residual[j]=-residual[j];if(diagonal[j/2]>0)direction[j]=residual[j]/diagonal[j/2];
        }
        double current=norm(residual),magnitude=0;
        for(int i=0;i<fixed.length;i++)magnitude+=diagonal[i]*(uv[2*i]*uv[2*i]+uv[2*i+1]*uv[2*i+1]);
        double tolerance=Math.max(current*1e-18,magnitude*1e-26);
        int steps=Math.min(2048,60_000_000/chart.root.length);
        for(int step=0;step<steps;step++){
            if(current<=tolerance && Double.isFinite(current))return;
            if(Thread.currentThread().isInterrupted())throw new java.util.concurrent.CancellationException("UV unwrap interrupted");
            normal(direction,product);
            double denom=0;for(int j=0;j<uv.length;j++)denom+=direction[j]*product[j];
            if(!(denom>0) || !Double.isFinite(current/denom))break;
            double alpha=current/denom;
            for(int j=0;j<uv.length;j++){uv[j]+=alpha*direction[j];residual[j]-=alpha*product[j];}
            double next=norm(residual);
            if(next<=tolerance && Double.isFinite(next)){
                normal(uv,product);
                if(norm(product)<=16*tolerance)return;
                break;
            }
            for(int j=0;j<uv.length;j++)if(diagonal[j/2]>0)
                direction[j]=residual[j]/diagonal[j/2]+(next/current)*direction[j];
            current=next;
        }
        throw new IllegalArgumentException("Unwrap did not converge; add seams or adjust pins");
    }
    private record Segment(int a,int b,double x0,double x1,double y0,double y1){}
    private double turn(int a,int b,int c){
        return (uv[2*b]-uv[2*a])*(uv[2*c+1]-uv[2*a+1])-(uv[2*b+1]-uv[2*a+1])*(uv[2*c]-uv[2*a]);
    }
    private void validateLayout(){
        var boundary=new ArrayList<Segment>();
        for(int i=0;i<chart.root.length;i++){
            int a=chart.root[i],b=chart.root[EditorUvChart.next(i)];
            if(i%3==0){
                int c=chart.root[i+2];
                double e1=Math.hypot(uv[2*b]-uv[2*a],uv[2*b+1]-uv[2*a+1]);
                double e2=Math.hypot(uv[2*c]-uv[2*a],uv[2*c+1]-uv[2*a+1]);
                if(!(orientation*turn(a,b,c)>1e-12*e1*e2))
                    throw new IllegalArgumentException("Unwrap folds or collapses a triangle; adjust seams or pins");
            }
            if(chart.cut[i])boundary.add(new Segment(a,b,Math.min(uv[2*a],uv[2*b]),Math.max(uv[2*a],uv[2*b]),
                Math.min(uv[2*a+1],uv[2*b+1]),Math.max(uv[2*a+1],uv[2*b+1])));
        }
        // Positive triangle orientation and a simple boundary give an injective disk parameterization.
        boundary.sort(Comparator.comparingDouble(Segment::x0));
        int budget=16_000_000;
        for(int i=0;i<boundary.size();i++){
            var s=boundary.get(i);
            for(int j=i+1;j<boundary.size();j++){
                var t=boundary.get(j);if(t.x0>s.x1)break;
                if(--budget==0)throw new IllegalArgumentException("Chart boundary exceeds overlap-check budget");
                if(t.y0>s.y1 || t.y1<s.y0)continue;
                int common=(s.a==t.a||s.a==t.b)?s.a:(s.b==t.a||s.b==t.b)?s.b:-1;
                if(common>=0){
                    int p=s.a==common?s.b:s.a,q=t.a==common?t.b:t.a;
                    double dot=(uv[2*p]-uv[2*common])*(uv[2*q]-uv[2*common])+
                        (uv[2*p+1]-uv[2*common+1])*(uv[2*q+1]-uv[2*common+1]);
                    if(turn(common,p,q)==0 && dot>0)throw new IllegalArgumentException("Unwrapped boundary doubles back; adjust seams or pins");
                    continue;
                }
                if(Math.signum(turn(s.a,s.b,t.a))*Math.signum(turn(s.a,s.b,t.b))<=0 &&
                    Math.signum(turn(t.a,t.b,s.a))*Math.signum(turn(t.a,t.b,s.b))<=0)
                    throw new IllegalArgumentException("Unwrapped boundary overlaps; adjust seams or pins");
            }
        }
    }
    private String angleError(List<Face> faces){
        double sum=0;
        for(int i=0;i<island.length;i++){
            var f=faces.get(island[i]);
            for(int k=0;k<3;k++){
                int j=(k+1)%3,l=(k+2)%3;
                var a=corner(f,k);var b=corner(f,j);var c=corner(f,l);
                double bx=(b.u()-a.u())*width,by=(b.v()-a.v())*height,cx=(c.u()-a.u())*width,cy=(c.v()-a.v())*height;
                double cross=bx*cy-by*cx;
                if(cross==0)return "undefined";
                double imageAngle=Math.atan2(Math.abs(cross),bx*cx+by*cy);
                int m=3*i+j,n=3*i+l;
                double spaceAngle=Math.atan2(Math.abs(real[m]*imag[n]-imag[m]*real[n]),-real[m]*real[n]-imag[m]*imag[n]);
                double difference=imageAngle-spaceAngle;sum+=difference*difference;
            }
        }
        return String.format(Locale.ROOT,"%.2f°",Math.toDegrees(Math.sqrt(sum/(3*island.length))));
    }
    private void preserveCuts(List<Face> faces){
        for(int i=0;i<chart.cut.length;i++)if(chart.cut[i]){
            int edgeIndex=island[i/3]*3+i%3;
            var edge=graph.edges().get(edgeIndex);int twin=edge.twin();
            if(twin<0 || !edge.uvSeam())continue;
            var face=faces.get(island[i/3]);var adjacent=faces.get(twin/3);
            if(face.marked(i%3)||adjacent.marked(twin%3)||!face.material().equals(adjacent.material()))continue;
            var a=corner(face,i%3);var b=corner(face,(i+1)%3);
            var otherA=corner(adjacent,(twin+1)%3);var otherB=corner(adjacent,twin%3);
            if(a.u()==otherA.u() && a.v()==otherA.v() && b.u()==otherB.u() && b.v()==otherB.v())
                throw new IllegalArgumentException("Unwrap would join an existing UV seam; mark that edge to retain the cut");
        }
    }
    Result solve(){
        optimize();
        for(int i=0;i<uv.length;i++){
            int dimension=i%2==0?width:height;
            uv[i]=(uv[i]+(i%2==0?offsetU:offsetV))/dimension;
            if(!Double.isFinite(uv[i])||Math.abs(uv[i])>1e9)throw new IllegalArgumentException("Unwrap exceeds native UV precision");
        }
        for(int i=0;i<chart.root.length;i++){
            var c=corner(state.faces().get(island[i/3]),i%3);
            if(c.pinned()){int j=2*chart.root[i];uv[j]=c.u();uv[j+1]=c.v();}
        }
        validateLayout();
        var faces=new ArrayList<>(state.faces());boolean changed=false;
        for(int i=0;i<island.length;i++){
            var f=faces.get(island[i]);var corners=new Corner[]{f.a(),f.b(),f.c()};boolean moved=false;
            for(int k=0;k<3;k++){
                int j=2*chart.root[3*i+k];var c=corners[k];
                if(uv[j]!=c.u() || uv[j+1]!=c.v()){
                    corners[k]=new Corner(c.vertex(),uv[j],uv[j+1],c.normal(),c.pinned());moved=true;
                }
            }
            if(moved){faces.set(island[i],f.withCorners(corners[0],corners[1],corners[2]));changed=true;}
        }
        if(!changed)throw new IllegalArgumentException("Chart already has the fitted UV coordinates");
        preserveCuts(faces);
        return new Result(faces,"RMS angle error "+angleError(state.faces())+" → "+angleError(faces));
    }
}
