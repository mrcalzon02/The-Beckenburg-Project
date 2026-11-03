package org.beckenburg.editor;

import java.io.*;
import java.util.*;
import org.beckenburg.editor.EditorMeshPipeline.Normal;

/** Immutable native local-to-world editing frame. Geometry remains canonical native world positions. */
public record EditorMeshFrame(double xx,double xy,double xz,double ox,double yx,double yy,double yz,double oy,double zx,double zy,double zz,double oz) {
    public static final EditorMeshFrame IDENTITY=new EditorMeshFrame(1,0,0,0,0,1,0,0,0,0,1,0);
    public EditorMeshFrame {
        double[] values={xx,xy,xz,ox,yx,yy,yz,oy,zx,zy,zz,oz};for(double value:values)if(!Double.isFinite(value))throw new IllegalArgumentException("Nonfinite native local frame");
        double determinant=xx*(yy*zz-yz*zy)-xy*(yx*zz-yz*zx)+xz*(yx*zy-yy*zx),scale=Math.hypot(Math.hypot(xx,yx),zx)*Math.hypot(Math.hypot(xy,yy),zy)*Math.hypot(Math.hypot(xz,yz),zz);
        if(!Double.isFinite(determinant)||!Double.isFinite(scale)||scale==0||Math.abs(determinant)<=scale*1e-12)throw new IllegalArgumentException("Singular/ill-conditioned native local frame");
        // JSON numbers do not carry a distinct negative-zero value. Canonicalize native frame zeros once.
        xx=xx==0?0:xx;xy=xy==0?0:xy;xz=xz==0?0:xz;ox=ox==0?0:ox;yx=yx==0?0:yx;yy=yy==0?0:yy;yz=yz==0?0:yz;oy=oy==0?0:oy;zx=zx==0?0:zx;zy=zy==0?0:zy;zz=zz==0?0:zz;oz=oz==0?0:oz;
    }
    public static EditorMeshFrame matrix(double[] m){if(m.length!=16||m[12]!=0||m[13]!=0||m[14]!=0||m[15]!=1)throw new IllegalArgumentException("Native frame must be affine");return new EditorMeshFrame(m[0],m[1],m[2],m[3],m[4],m[5],m[6],m[7],m[8],m[9],m[10],m[11]);}
    public double[] values(){return new double[]{xx,xy,xz,ox,yx,yy,yz,oy,zx,zy,zz,oz};}
    public double direction(double x,double y,double z,int axis){return switch(axis){case 0->xx*x+xy*y+xz*z;case 1->yx*x+yy*y+yz*z;case 2->zx*x+zy*y+zz*z;default->throw new IllegalArgumentException("Frame component must be0..2");};}
    public Normal vector(int axis){return switch(axis){case 0->new Normal(xx,yx,zx);case 1->new Normal(xy,yy,zy);case 2->new Normal(xz,yz,zz);default->throw new IllegalArgumentException("Local axis must be0..2");};}
    public Normal axis(int axis){return EditorMeshNormals.unit(vector(axis));}
    public EditorMeshFrame linear(){return new EditorMeshFrame(xx,xy,xz,0,yx,yy,yz,0,zx,zy,zz,0);}
    public EditorMeshFrame inverse(){double d=xx*(yy*zz-yz*zy)-xy*(yx*zz-yz*zx)+xz*(yx*zy-yy*zx);double a=(yy*zz-yz*zy)/d,b=(xz*zy-xy*zz)/d,c=(xy*yz-xz*yy)/d,e=(yz*zx-yx*zz)/d,f=(xx*zz-xz*zx)/d,g=(xz*yx-xx*yz)/d,h=(yx*zy-yy*zx)/d,i=(xy*zx-xx*zy)/d,j=(xx*yy-xy*yx)/d;return new EditorMeshFrame(a,b,c,-(a*ox+b*oy+c*oz),e,f,g,-(e*ox+f*oy+g*oz),h,i,j,-(h*ox+i*oy+j*oz));}
    public EditorMeshFrame multiply(EditorMeshFrame other){double[] a=values(),b=other.values(),out=new double[12];for(int row=0;row<3;row++){for(int col=0;col<3;col++)out[row*4+col]=a[row*4]*b[col]+a[row*4+1]*b[4+col]+a[row*4+2]*b[8+col];out[row*4+3]=a[row*4]*b[3]+a[row*4+1]*b[7]+a[row*4+2]*b[11]+a[row*4+3];}return fromValues(out);}
    public static EditorMeshFrame around(EditorMeshFrame linear,double x,double y,double z){return new EditorMeshFrame(linear.xx,linear.xy,linear.xz,x-linear.direction(x,y,z,0),linear.yx,linear.yy,linear.yz,y-linear.direction(x,y,z,1),linear.zx,linear.zy,linear.zz,z-linear.direction(x,y,z,2));}
    public static EditorMeshFrame translation(double x,double y,double z){return new EditorMeshFrame(1,0,0,x,0,1,0,y,0,0,1,z);}
    public static EditorMeshFrame operation(EditorTopologyProject.Transform operation,int axis,double amount){
        if(operation==EditorTopologyProject.Transform.ROTATE){double c=StrictMath.cos(StrictMath.toRadians(amount)),s=StrictMath.sin(StrictMath.toRadians(amount));return switch(axis){case 0->new EditorMeshFrame(1,0,0,0,0,c,-s,0,0,s,c,0);case 1->new EditorMeshFrame(c,0,s,0,0,1,0,0,-s,0,c,0);case 2->new EditorMeshFrame(c,-s,0,0,s,c,0,0,0,0,1,0);default->throw new IllegalArgumentException("Local axis must be0..2");};}
        return new EditorMeshFrame(operation==EditorTopologyProject.Transform.SCALE_UNIFORM||axis==0?amount:1,0,0,0,0,operation==EditorTopologyProject.Transform.SCALE_UNIFORM||axis==1?amount:1,0,0,0,0,operation==EditorTopologyProject.Transform.SCALE_UNIFORM||axis==2?amount:1,0);
    }
    public Normal normal(Normal n,boolean preserveMagnitude){if(n==null)return null;var inverse=linear().inverse();var value=new Normal(inverse.xx*n.x()+inverse.yx*n.y()+inverse.zx*n.z(),inverse.xy*n.x()+inverse.yy*n.y()+inverse.zy*n.z(),inverse.xz*n.x()+inverse.yz*n.y()+inverse.zz*n.z());var unit=EditorMeshNormals.unit(value);double length=preserveMagnitude?Math.hypot(Math.hypot(n.x(),n.y()),n.z()):1;return new Normal(unit.x()*length,unit.y()*length,unit.z()*length);}
    public static EditorMeshFrame fromValues(double[] v){if(v.length!=12)throw new IllegalArgumentException("Native frame needs12 values");return new EditorMeshFrame(v[0],v[1],v[2],v[3],v[4],v[5],v[6],v[7],v[8],v[9],v[10],v[11]);}
    public static Map<String,EditorMeshFrame> validate(Map<String,EditorMeshFrame> frames,Collection<String> parts){if(frames.size()>EditorMeshPipeline.MAX_TRIANGLES*3)throw new IllegalArgumentException("Native frame count budget");var known=new HashSet<>(parts);for(var e:frames.entrySet())if(!known.contains(e.getKey())||e.getKey().isBlank()||e.getKey().length()>512||e.getValue()==null)throw new IllegalArgumentException("Unknown native frame part");return Map.copyOf(frames);}
    static Map<String,EditorMeshFrame> retain(Map<String,EditorMeshFrame> frames,List<EditorTopologyProject.Vertex> vertices){var names=new HashSet<String>();for(var v:vertices)names.add(v.part());var result=new TreeMap<String,EditorMeshFrame>();for(var e:frames.entrySet())if(names.contains(e.getKey()))result.put(e.getKey(),e.getValue());return result;}
    static void write(DataOutputStream out,Map<String,EditorMeshFrame> frames)throws IOException{out.writeInt(frames.size());for(var e:new TreeMap<>(frames).entrySet()){out.writeUTF(e.getKey());for(double v:e.getValue().values())out.writeDouble(v);}}
    static Map<String,EditorMeshFrame> read(DataInputStream in)throws IOException{int count=in.readInt();if(count<0||count>EditorMeshPipeline.MAX_TRIANGLES*3)throw new IOException("Native frame count budget");var frames=new TreeMap<String,EditorMeshFrame>();for(int i=0;i<count;i++){String part=in.readUTF();double[] v=new double[12];for(int n=0;n<12;n++)v[n]=in.readDouble();if(frames.putIfAbsent(part,fromValues(v))!=null)throw new IOException("Duplicate native frame part");}return Map.copyOf(frames);}
    static EditorMeshFrame json(Object value)throws IOException{var meta=EditorGlbReader.obj(value);if((!(meta.get("version") instanceof Number version)||new java.math.BigDecimal(version.toString()).compareTo(java.math.BigDecimal.ONE)!=0)||!Boolean.TRUE.equals(meta.get("bakedWorldGeometry"))||!meta.keySet().equals(Set.of("version","bakedWorldGeometry","matrix")))throw new IOException("Unsupported native GLB editing frame");var values=EditorGlbReader.arr(meta.get("matrix"));if(values.size()!=12)throw new IOException("Invalid native GLB editing frame matrix");double[] data=new double[12];for(int i=0;i<12;i++)data[i]=((Number)values.get(i)).doubleValue();return fromValues(data);}
    static void attach(Map<String,Object> root,int node,EditorMeshFrame frame){var target=EditorGlbReader.obj(EditorGlbReader.arr(root.get("nodes")).get(node));var extensions=target.get("extensions") instanceof Map<?,?> prior?new LinkedHashMap<>(EditorGlbReader.obj(prior)):new LinkedHashMap<String,Object>();extensions.put("BECK_editor_frame",Map.of("version",1,"bakedWorldGeometry",true,"matrix",Arrays.stream(frame.values()).boxed().toList()));target.put("extensions",extensions);var used=root.get("extensionsUsed") instanceof List<?> prior?new ArrayList<Object>(prior):new ArrayList<Object>();if(!used.contains("BECK_editor_frame"))used.add("BECK_editor_frame");root.put("extensionsUsed",used);}

}
