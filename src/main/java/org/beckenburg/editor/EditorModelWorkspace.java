package org.beckenburg.editor;

import java.io.*;
import java.util.*;

/** Saved Model Editor view and tool settings. Geometry remains in the native project state. */
public record EditorModelWorkspace(EditorMeshPicking.Projection projection,double yaw,double pitch,double zoom,
                                   double panX,double panY,Map<Field,Double> settings,List<Long> vertices,Long activeVertex,
                                   EditorMeshPicking.Hit hit,Long surfaceFace,boolean edgeGroup) {
    public enum Field {
        TOOL(0,7), MODE(0,2), TRAVERSAL(0,2), AREA(0,1), XRAY(0,1), NORMALS(0,1), COMPARE(0,1),
        SPACE(0,2), TRANSFORM(0,2), AXIS(0,2), PIVOT(0,1), AMOUNT(-36000,1000000,false),
        GRID(0,1000,false), ANGLE(0,360,false), PRIMITIVE(0,4), WIDTH(.0001,10000,false),
        HEIGHT(.0001,10000,false), DEPTH(.0001,10000,false), SEGMENTS(3,128), RINGS(1,64),
        FACE(0,1), FACE_AMOUNT(-10000,10000,false), BEVEL(.000001,10000,false), SLIDE(.001,.999,false),
        REVERSE(0,1), PRESET(0,4), LEVEL(0,12), INSPECTOR(0,1), BROWSERS(0,1), FOCUS(0,1);
        final double min,max;final boolean integral;
        Field(double min,double max){this(min,max,true);}
        Field(double min,double max,boolean integral){this.min=min;this.max=max;this.integral=integral;}
        double initial(){
            return switch(this){
                case AMOUNT -> 90; case GRID -> .05; case ANGLE -> 15;
                case WIDTH,HEIGHT,DEPTH,INSPECTOR -> 1; case SEGMENTS -> 12; case RINGS -> 4;
                case FACE_AMOUNT,SLIDE -> .25; case BEVEL -> .1; default -> 0;
            };
        }
        void check(double value){
            if(!Double.isFinite(value)||value<min||value>max||integral&&value!=Math.rint(value))throw new IllegalArgumentException("Invalid workspace field "+name());
        }
    }
    public EditorModelWorkspace {
        Objects.requireNonNull(projection);
        if(!Double.isFinite(yaw)||Math.abs(yaw)>360||!Double.isFinite(pitch)||Math.abs(pitch)>85||!Double.isFinite(zoom)
                ||zoom<.1||zoom>20||!Double.isFinite(panX)||!Double.isFinite(panY))throw new IllegalArgumentException("Invalid saved camera");
        var complete=new EnumMap<Field,Double>(Field.class);
        for(var field:Field.values())complete.put(field,field.initial());
        settings.forEach((field,value)->{field.check(value);complete.put(field,value);});
        settings=Map.copyOf(complete);
        if(vertices.size()>EditorMeshPipeline.MAX_TRIANGLES*3)throw new IllegalArgumentException("Saved selection exceeds native budget");
        var sorted=new TreeSet<Long>(vertices);
        if(sorted.size()!=vertices.size()||!sorted.isEmpty()&&sorted.first()<0)throw new IllegalArgumentException("Invalid saved vertex selection");
        vertices=List.copyOf(sorted);
        if(activeVertex!=null && !sorted.contains(activeVertex))throw new IllegalArgumentException("Primary vertex is outside the saved selection");
        if(hit!=null && (hit.faceId()<0||hit.vertexId()<0||hit.edgeA()<0||hit.edgeB()<0||hit.edgeA()==hit.edgeB()
                ||!Double.isFinite(hit.depth())||!Double.isFinite(hit.u())||!Double.isFinite(hit.v())))throw new IllegalArgumentException("Invalid saved pick");
        if(surfaceFace!=null && surfaceFace<0 || edgeGroup && (hit==null || settings.getOrDefault(Field.MODE,0d)!=1))throw new IllegalArgumentException("Invalid saved component");
    }

    EditorModelWorkspace forState(EditorTopologyProject.State state) {
        if(vertices.isEmpty()&&hit==null&&surfaceFace==null)return this;
        var known=new HashSet<Long>();for(var vertex:state.vertices())known.add(vertex.id());
        var retained=vertices.stream().filter(known::contains).toList();
        EditorMeshPicking.Hit pick=null;Long surface=null;
        for(var face:state.faces()) {
            if(surfaceFace!=null&&surfaceFace==face.id())surface=surfaceFace;
            if(hit!=null&&face.id()==hit.faceId()){
                var ids=List.of(face.a().vertex(),face.b().vertex(),face.c().vertex());
                if(ids.contains(hit.vertexId())&&ids.contains(hit.edgeA())&&ids.contains(hit.edgeB()))pick=hit;
            }
        }
        if(retained.size()==vertices.size() && pick==hit && Objects.equals(surface,surfaceFace))return this;
        return new EditorModelWorkspace(projection,yaw,pitch,zoom,panX,panY,settings,retained,known.contains(activeVertex)?activeVertex:null,pick,surface,edgeGroup&&pick!=null);
    }

    void write(DataOutputStream out)throws IOException {
        out.writeUTF(projection.name());out.writeDouble(yaw);out.writeDouble(pitch);out.writeDouble(zoom);out.writeDouble(panX);out.writeDouble(panY);
        out.writeInt(settings.size());for(var field:Field.values())if(settings.containsKey(field)){out.writeUTF(field.name());out.writeDouble(settings.get(field));}
        out.writeInt(vertices.size());for(long vertex:vertices)out.writeLong(vertex);out.writeLong(activeVertex==null?-1:activeVertex);
        out.writeBoolean(hit!=null);if(hit!=null){out.writeLong(hit.faceId());out.writeLong(hit.vertexId());out.writeLong(hit.edgeA());out.writeLong(hit.edgeB());out.writeDouble(hit.depth());out.writeDouble(hit.u());out.writeDouble(hit.v());}
        out.writeLong(surfaceFace==null?-1:surfaceFace);out.writeBoolean(edgeGroup);
    }
    private static boolean flag(DataInputStream in)throws IOException {
        int raw=in.readUnsignedByte();if(raw>1)throw new IOException("Invalid workspace flag");return raw==1;
    }
    static EditorModelWorkspace read(DataInputStream in,int vertexCount)throws IOException {
        var projection=EditorMeshPicking.Projection.valueOf(in.readUTF());
        double yaw=in.readDouble(),pitch=in.readDouble(),zoom=in.readDouble(),panX=in.readDouble(),panY=in.readDouble();
        var settings=new EnumMap<Field,Double>(Field.class);int count=in.readInt();
        if(count<0||count>Field.values().length)throw new IOException("Invalid workspace field count");
        for(int i=0;i<count;i++)if(settings.put(Field.valueOf(in.readUTF()),in.readDouble())!=null)throw new IOException("Duplicate workspace field");
        int selected=in.readInt();if(selected<0||selected>vertexCount)throw new IOException("Invalid workspace selection count");
        var ids=new ArrayList<Long>(selected);for(int i=0;i<selected;i++)ids.add(in.readLong());
        long primary=in.readLong();
        var hit=flag(in)?new EditorMeshPicking.Hit(in.readLong(),in.readLong(),in.readLong(),in.readLong(),in.readDouble(),in.readDouble(),in.readDouble()):null;
        long surface=in.readLong();return new EditorModelWorkspace(projection,yaw,pitch,zoom,panX,panY,settings,ids,primary==-1?null:primary,hit,surface==-1?null:surface,flag(in));
    }
}
