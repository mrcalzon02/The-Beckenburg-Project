package org.beckenburg;

import java.io.*;
import java.security.*;
import java.util.*;
import java.util.function.IntPredicate;
import org.beckenburg.LevelDefinition.Collider;
import org.beckenburg.LevelDefinition.Vec;

/** Bounded HCS2 native geometry queries. Does not unlock or synthesize a level.
 * Walking support, physical solids and optical solids have distinct meanings;
 * invisible containment OBBs are consulted only by actor movement.
 */
public final class AuthoredSitePhysics {
    public static final int SUPPORT=1,OPTICAL=2,BODY=4;
    public static final double MAX_GRADE=.20,RADIUS=.22,MAX_STEP=.25;
    private static final int MAX_BYTES=32_000_000,MAX_TRIANGLES=500_000,MAX_PARTS=16384,MAX_REFS=8_000_000;
    private static final double CELL=2,CONTACT=.005;
    public record Triangle(Vec a,Vec b,Vec c,int flags,int component){}
    public record Support(double y,double grade,int triangle){}
    private record Part(String name,boolean closed,int first,int count,double[] bounds){}
    private final float[] xyz;
    private final byte[] flags;
    private final int[] owner;
    private final Part[] parts;
    private final Map<Long,int[]> bins;
    private final List<Collider> blockers;
    private final String sha256;

    private AuthoredSitePhysics(float[] xyz,byte[] flags,int[] owner,Part[] parts,List<Collider> blockers,String sha)throws IOException{
        this.xyz=xyz;this.flags=flags;this.owner=owner;this.parts=parts;this.blockers=List.copyOf(blockers);sha256=sha;
        for(var b:blockers)if(!finite(b.x(),b.y(),b.z(),b.halfX(),b.halfZ(),b.height(),b.yaw())||b.halfX()<=0||b.halfZ()<=0||b.height()<=0)throw new IOException("Invalid movement-only blocker");
        long refs=0;
        for(int i=0;i<flags.length;i++){
            int n=i*9;long dx=cell(Math.max(xyz[n],Math.max(xyz[n+3],xyz[n+6])))-cell(Math.min(xyz[n],Math.min(xyz[n+3],xyz[n+6])))+1;
            long dz=cell(Math.max(xyz[n+2],Math.max(xyz[n+5],xyz[n+8])))-cell(Math.min(xyz[n+2],Math.min(xyz[n+5],xyz[n+8])))+1;
            refs+=dx*dz;if(refs>MAX_REFS)throw new IOException("Native geometry spatial budget exceeded");
        }
        var lists=new HashMap<Long,ArrayList<Integer>>();
        for(int i=0;i<flags.length;i++){
            int n=i*9;int ax=cell(Math.min(xyz[n],Math.min(xyz[n+3],xyz[n+6]))),bx=cell(Math.max(xyz[n],Math.max(xyz[n+3],xyz[n+6])));
            int az=cell(Math.min(xyz[n+2],Math.min(xyz[n+5],xyz[n+8]))),bz=cell(Math.max(xyz[n+2],Math.max(xyz[n+5],xyz[n+8])));
            for(int x=ax;x<=bx;x++)for(int z=az;z<=bz;z++)lists.computeIfAbsent(key(x,z),k->new ArrayList<>()).add(i);
        }
        var result=new HashMap<Long,int[]>();for(var e:lists.entrySet())result.put(e.getKey(),e.getValue().stream().mapToInt(Integer::intValue).toArray());bins=Map.copyOf(result);
    }
    public static AuthoredSitePhysics read(InputStream source,String expectedSha,List<Collider> blockers)throws IOException{
        Objects.requireNonNull(source);Objects.requireNonNull(blockers);
        byte[] data=source.readNBytes(MAX_BYTES+1);if(data.length>MAX_BYTES)throw new IOException("Native geometry byte budget exceeded");
        String sha;
        try{sha=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));}catch(NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
        if(expectedSha==null||!sha.equals(expectedSha))throw new IOException("Native geometry source hash mismatch");
        try(var in=new DataInputStream(new ByteArrayInputStream(data))){
            if(in.readInt()!=0x48435332)throw new IOException("Unsupported native geometry version");
            int count=in.readInt(),triangles=in.readInt();
            if(count<1||count>MAX_PARTS||triangles<1||triangles>MAX_TRIANGLES||data.length<12L+37L*triangles+7L*count)throw new IOException("Invalid native geometry counts");
            float[] xyz=new float[triangles*9];byte[] flags=new byte[triangles];int[] owner=new int[triangles];Part[] parts=new Part[count];int cursor=0;
            for(int p=0;p<count;p++){
                int length=in.readUnsignedShort();if(length<1||length>256)throw new IOException("Invalid component name");
                byte[] text=in.readNBytes(length);if(text.length!=length)throw new EOFException();String name=java.nio.charset.StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(text)).toString();
                int closed=in.readUnsignedByte(),n=in.readInt();if(closed>1||n<1||n>triangles-cursor)throw new IOException("Invalid component size/closure");
                double[] bounds={Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY};
                int first=cursor;
                for(int j=0;j<n;j++,cursor++){
                    for(int k=0;k<9;k++){
                        float v=in.readFloat();if(!Float.isFinite(v)||Math.abs(v)>=256)throw new IOException("Nonfinite/out-of-domain native vertex");xyz[cursor*9+k]=v;
                        int a=k%3;bounds[a]=Math.min(bounds[a],v);bounds[a+3]=Math.max(bounds[a+3],v);
                    }
                    int f=in.readUnsignedByte();if(f==0||(f&~7)!=0||((f&SUPPORT)!=0&&(f&BODY)==0)||closed==1&&(f&BODY)==0)throw new IOException("Invalid native surface flags");
                    flags[cursor]=(byte)f;owner[cursor]=p;
                    Vec a=vertex(xyz,cursor,0),b=vertex(xyz,cursor,1),c=vertex(xyz,cursor,2);if(norm2(cross(sub(b,a),sub(c,a)))<1e-24)throw new IOException("Degenerate native triangle");
                }
                parts[p]=new Part(name,closed==1,first,n,bounds);
            }
            if(cursor!=triangles||in.read()!=-1)throw new IOException("Truncated/trailing native geometry records");
            return new AuthoredSitePhysics(xyz,flags,owner,parts,blockers,sha);
        }catch(EOFException truncated){throw new IOException("Truncated native geometry",truncated);}
    }
    private static AuthoredSitePhysics hollowCreek;
    public static synchronized AuthoredSitePhysics hollowCreek()throws IOException{
        if(hollowCreek!=null)return hollowCreek;
        try(var in=AuthoredSitePhysics.class.getResourceAsStream(HollowCreekContract.RESOURCE)){
            if(in==null)throw new IOException("Missing current Hollow Creek geometry");return hollowCreek=read(in,HollowCreekContract.SHA256,HollowCreekContract.blockers());
        }
    }
    public String sha256(){return sha256;}
    public int triangleCount(){return flags.length;}
    public int componentCount(){return parts.length;}
    public int movementBlockerCount(){return blockers.size();}
    public Triangle triangle(int index){if(index<0||index>=flags.length)throw new IndexOutOfBoundsException();return new Triangle(v(index,0),v(index,1),v(index,2),flags[index],owner[index]);}
    public String componentName(int component){return parts[component].name();}
    private static int cell(double v){return (int)Math.floor(v/CELL);}
    private static long key(int x,int z){return ((long)x<<32)^(z&0xffffffffL);}
    private Vec v(int t,int n){return vertex(xyz,t,n);}
    private static Vec vertex(float[] values,int t,int n){int i=t*9+n*3;return new Vec(values[i],values[i+1],values[i+2]);}
    private static boolean finite(double... values){for(double v:values)if(!Double.isFinite(v))return false;return true;}
    private static boolean point(Vec p){return p!=null&&finite(p.x(),p.y(),p.z())&&Math.max(Math.abs(p.x()),Math.max(Math.abs(p.y()),Math.abs(p.z())))<256;}
    private static Vec add(Vec a,Vec b){return new Vec(a.x()+b.x(),a.y()+b.y(),a.z()+b.z());}
    private static Vec sub(Vec a,Vec b){return new Vec(a.x()-b.x(),a.y()-b.y(),a.z()-b.z());}
    private static Vec mul(Vec a,double n){return new Vec(a.x()*n,a.y()*n,a.z()*n);}
    private static double dot(Vec a,Vec b){return a.x()*b.x()+a.y()*b.y()+a.z()*b.z();}
    private static double norm2(Vec a){return dot(a,a);}
    private static Vec cross(Vec a,Vec b){return new Vec(a.y()*b.z()-a.z()*b.y(),a.z()*b.x()-a.x()*b.z(),a.x()*b.y()-a.y()*b.x());}
    private static double clamp(double t){return Math.max(0,Math.min(1,t));}

    /** Highest intended walking triangle close to previous feet Y; never a fake flat pad. */
    public Support support(double x,double z,double previousY,double maxDelta){
        if(!finite(x,z,previousY,maxDelta)||maxDelta<0||maxDelta>4||Math.abs(x)>=256||Math.abs(z)>=256)return null;
        int[] list=bins.get(key(cell(x),cell(z)));if(list==null)return null;Support best=null;
        for(int i:list){
            if((flags[i]&SUPPORT)==0)continue;Vec a=v(i,0),b=v(i,1),c=v(i,2);double abx=b.x()-a.x(),abz=b.z()-a.z(),acx=c.x()-a.x(),acz=c.z()-a.z(),den=abx*acz-abz*acx;
            if(Math.abs(den)<1e-14)continue;double dx=x-a.x(),dz=z-a.z(),u=(dx*acz-dz*acx)/den,w=(abx*dz-abz*dx)/den;
            if(u< -1e-7||w< -1e-7||u+w>1+1e-7)continue;
            Vec normal=cross(sub(b,a),sub(c,a));if(normal.y()<=0)continue;double grade=Math.hypot(normal.x(),normal.z())/normal.y();if(grade>MAX_GRADE+1e-5)continue;
            double y=a.y()+u*(b.y()-a.y())+w*(c.y()-a.y());
            if(Math.abs(y-previousY)<=maxDelta+1e-6&&(best==null||y>best.y()))best=new Support(y,grade,i);
        }return best;
    }
    /** Exact two-sided segment/triangle intersection, returned as a segment fraction. */
    private double ray(int i,Vec a,Vec delta){
        Vec p=v(i,0),e1=sub(v(i,1),p),e2=sub(v(i,2),p),h=cross(delta,e2);double det=dot(e1,h);
        if(Math.abs(det)<1e-14)return Double.NaN;double inv=1/det;Vec s=sub(a,p);double u=dot(s,h)*inv;if(u< -1e-8||u>1+1e-8)return Double.NaN;
        Vec q=cross(s,e1);double w=dot(delta,q)*inv;if(w< -1e-8||u+w>1+1e-8)return Double.NaN;double t=dot(e2,q)*inv;return t>=0&&t<=1?t:Double.NaN;
    }
    private boolean rayCells(Vec a,Vec b,IntPredicate visitor){
        int x=cell(a.x()),z=cell(a.z()),endX=cell(b.x()),endZ=cell(b.z());double dx=b.x()-a.x(),dz=b.z()-a.z();int sx=dx>0?1:dx<0?-1:0,sz=dz>0?1:dz<0?-1:0;
        double tx=sx==0?Double.POSITIVE_INFINITY:((sx>0?(x+1)*CELL:x*CELL)-a.x())/dx,tz=sz==0?Double.POSITIVE_INFINITY:((sz>0?(z+1)*CELL:z*CELL)-a.z())/dz;
        double stepX=sx==0?Double.POSITIVE_INFINITY:CELL/Math.abs(dx),stepZ=sz==0?Double.POSITIVE_INFINITY:CELL/Math.abs(dz);
        for(int step=0;step<1028;step++){
            int[] list=bins.get(key(x,z));if(list!=null)for(int i:list)if(!visitor.test(i))return false;
            if(x==endX&&z==endZ)return true;
            // A negative-going segment ending on a grid plane belongs to the
            // floor-selected endpoint cell, not the cell beyond t=1. Roundoff
            // at a simultaneous X/Z crossing must never overrun that endpoint.
            if(Math.min(tx,tz)>=1-1e-12)return true;
            if(tx<tz){x+=sx;tx+=stepX;}else if(tz<tx){z+=sz;tz+=stepZ;}else{x+=sx;z+=sz;tx+=stepX;tz+=stepZ;}
        }throw new IllegalStateException("Bounded native ray grid did not terminate");
    }
    public boolean clearSight(Vec a,Vec b){
        if(!point(a)||!point(b)||norm2(sub(b,a))>128*128)return false;Vec d=sub(b,a);if(norm2(d)<1e-16)return true;
        return rayCells(a,b,i->{if((flags[i]&OPTICAL)==0)return true;double t=ray(i,a,d);return !Double.isFinite(t)||t<=1e-6||t>=1-1e-6;});
    }
    /** Per closed component parity preserves hollow rooms and overlapping solids. */
    private boolean insideSolid(Vec p){
        for(int part=0;part<parts.length;part++){
            Part c=parts[part];double[] box=c.bounds();if(!c.closed()||p.x()<=box[0]||p.y()<=box[1]||p.z()<=box[2]||p.x()>=box[3]||p.y()>=box[4]||p.z()>=box[5])continue;
            double length=Math.sqrt((box[3]-box[0])*(box[3]-box[0])+(box[4]-box[1])*(box[4]-box[1])+(box[5]-box[2])*(box[5]-box[2]))+1;
            Vec d=mul(new Vec(.918719,.341389,.199237),length),end=add(p,d);var hits=new ArrayList<Double>();final int target=part;
            rayCells(p,end,i->{if(owner[i]==target){double t=ray(i,p,d);if(Double.isFinite(t)&&t>1e-8)hits.add(t);}return true;});
            hits.sort(Double::compare);int count=0;double last=-1;for(double t:hits)if(t-last>1e-7){count++;last=t;}if((count&1)==1)return true;
        }return false;
    }
    private static double pointTriangle(Vec p,Vec a,Vec b,Vec c){
        Vec ab=sub(b,a),ac=sub(c,a),ap=sub(p,a);double d1=dot(ab,ap),d2=dot(ac,ap);if(d1<=0&&d2<=0)return norm2(ap);
        Vec bp=sub(p,b);double d3=dot(ab,bp),d4=dot(ac,bp);if(d3>=0&&d4<=d3)return norm2(bp);double vc=d1*d4-d3*d2;
        if(vc<=0&&d1>=0&&d3<=0)return norm2(sub(p,add(a,mul(ab,d1/(d1-d3)))));
        Vec cp=sub(p,c);double d5=dot(ab,cp),d6=dot(ac,cp);if(d6>=0&&d5<=d6)return norm2(cp);double vb=d5*d2-d1*d6;
        if(vb<=0&&d2>=0&&d6<=0)return norm2(sub(p,add(a,mul(ac,d2/(d2-d6)))));double va=d3*d6-d5*d4;
        if(va<=0&&d4-d3>=0&&d5-d6>=0)return norm2(sub(p,add(b,mul(sub(c,b),(d4-d3)/(d4-d3+d5-d6)))));
        double den=1/(va+vb+vc);return norm2(sub(p,add(a,add(mul(ab,vb*den),mul(ac,vc*den)))));
    }
    private static double segments(Vec p,Vec q,Vec a,Vec b){
        Vec d1=sub(q,p),d2=sub(b,a),r=sub(p,a);double x=norm2(d1),e=norm2(d2),f=dot(d2,r),s,t;
        if(x<=1e-20&&e<=1e-20)return norm2(r);
        if(x<=1e-20){s=0;t=clamp(f/e);}else{
            double c=dot(d1,r);if(e<=1e-20){t=0;s=clamp(-c/x);}else{
                double k=dot(d1,d2),den=x*e-k*k;s=den>1e-14*x*e?clamp((k*f-c*e)/den):0;t=(k*s+f)/e;
                if(t<0){t=0;s=clamp(-c/x);}else if(t>1){t=1;s=clamp((k-c)/x);}
            }
        }return norm2(sub(add(p,mul(d1,s)),add(a,mul(d2,t))));
    }
    private double capsuleTriangle(Vec p,Vec q,int i){
        double t=ray(i,p,sub(q,p));if(Double.isFinite(t))return 0;Vec a=v(i,0),b=v(i,1),c=v(i,2);
        return Math.min(Math.min(pointTriangle(p,a,b,c),pointTriangle(q,a,b,c)),Math.min(segments(p,q,a,b),Math.min(segments(p,q,b,c),segments(p,q,c,a))));
    }
    public boolean bodyBlocked(Vec feet,double height,double radius){
        if(!point(feet)||!finite(height,radius)||radius<.01||radius>.4||height<radius*2||height>2.5)return true;
        Vec a=add(feet,new Vec(0,radius,0)),b=add(feet,new Vec(0,height-radius,0));double limit=(radius-CONTACT)*(radius-CONTACT);
        for(Collider c:blockers){
            double dx=feet.x()-c.x(),dz=feet.z()-c.z(),cs=Math.cos(c.yaw()),sn=Math.sin(c.yaw());
            double x=Math.max(0,Math.abs(dx*cs+dz*sn)-c.halfX()),z=Math.max(0,Math.abs(-dx*sn+dz*cs)-c.halfZ()),y=Math.max(0,Math.max(c.y()-b.y(),a.y()-c.y()-c.height()));
            if(x*x+y*y+z*z<limit)return true;
        }
        for(int x=cell(feet.x()-radius);x<=cell(feet.x()+radius);x++)for(int z=cell(feet.z()-radius);z<=cell(feet.z()+radius);z++){
            int[] list=bins.get(key(x,z));if(list==null)continue;
            for(int i:list)if((flags[i]&BODY)!=0&&capsuleTriangle(a,b,i)<limit)return true;
        }return insideSolid(add(feet,new Vec(0,height/2,0)));
    }
    public boolean traversable(Vec feet,double height){
        if(!point(feet)||support(feet.x(),feet.z(),feet.y(),.006)==null||bodyBlocked(feet,height,RADIUS))return false;
        for(double[] d:new double[][]{{RADIUS,0},{-RADIUS,0},{0,RADIUS},{0,-RADIUS}})if(support(feet.x()+d[0],feet.z()+d[1],feet.y(),.08)==null)return false;
        return true;
    }
    public Vec resolveMove(Vec from,double x,double z,double height){
        if(!point(from)||!finite(x,z)||!traversable(from,height))return null;double distance=Math.hypot(x-from.x(),z-from.z());if(distance>2)return null;
        int steps=Math.max(1,(int)Math.ceil(distance/.05));Vec at=from;
        for(int i=1;i<=steps;i++){
            double nx=from.x()+(x-from.x())*i/steps,nz=from.z()+(z-from.z())*i/steps;Support s=support(nx,nz,at.y(),MAX_STEP);
            if(s==null)return null;Vec next=new Vec(nx,s.y(),nz);if(!traversable(next,height))return null;at=next;
        }return at;
    }
}
