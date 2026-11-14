package org.beckenburg.editor;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Deterministic Y-up OBJ copy from authoritative native identities; OBJ never becomes hot editing state. */
public final class EditorObjWriter {
    private EditorObjWriter(){}
    private static Map<Long,Integer> indices(EditorTopologyProject.State state)throws IOException{
        var ids=new LinkedHashMap<Long,Integer>();for(var v:state.vertices())ids.put(v.id(),ids.size()+1);
        var used=new HashSet<Long>();for(var f:state.faces()){
            if(!f.part().matches("[^\\s#]+"))throw new IOException("OBJ export requires single-token part names; retain native BTOP for this name");
            used.add(f.a().vertex());used.add(f.b().vertex());used.add(f.c().vertex());
        }
        if(state.faces().isEmpty())throw new IOException("OBJ export needs at least one native face");
        if(used.size()!=state.vertices().size())throw new IOException("OBJ roundtrip needs referenced vertices; explicitly remove unused vertices or retain native BTOP");
        return ids;
    }
    public static void write(EditorTopologyProject.State state,OutputStream output)throws IOException{
        if(!state.materials().isEmpty())throw new IOException("Plain OBJ cannot retain embedded native materials; save BTOP or publish GLB");
        write(state,output,Map.of(),null);
    }
    static void write(EditorTopologyProject.State state,OutputStream output,Map<String,String> aliases,String library)throws IOException{
        var ids=indices(state);var points=new HashMap<Long,EditorTopologyProject.Vertex>();for(var v:state.vertices())points.put(v.id(),v);
        var writer=new BufferedWriter(new OutputStreamWriter(output,StandardCharsets.UTF_8));
        writer.write("# Beckenburg native topology copy: Y up, X/Z horizontal; UVs unmodified\n");
        if(library!=null)writer.write("mtllib "+library+"\n");
        var plainQuads = new HashSet<Integer>();
        if (!state.quads().isEmpty()) {
            var ordered = EditorMeshQuad.ordinals(state.quads(),state.faces().stream().map(EditorTopologyProject.Face::id).toList());
            for (var pair : ordered) {
                int first = Math.toIntExact(pair.first()), second = Math.toIntExact(pair.second());
                if (second == first+1 && representableQuad(state.faces().get(first),state.faces().get(second),points)) plainQuads.add(first);
                else writer.write("# beck_authored_quad_v1 "+first+" "+second+"\n");
            }
        }
        for(var entry:new TreeMap<>(state.frames()).entrySet()){writer.write("# beck_native_frame_v1 "+entry.getKey());for(double value:entry.getValue().values())writer.write(" "+Double.toString(value));writer.write("\n");}
        for(var v:state.vertices())writer.write("v "+v.x()+" "+v.y()+" "+v.z()+"\n");
        for(var f:state.faces())for(var c:List.of(f.a(),f.b(),f.c()))writer.write("vt "+c.u()+" "+c.v()+"\n");
        for(var f:state.faces()){
            var a=vertex(f.a(),points);var b=vertex(f.b(),points);var c=vertex(f.c(),points);var triangle=new EditorMeshPipeline.Triangle(f.part(),a,b,c);var flat=faceNormal(triangle);
            for(var corner:List.of(a,b,c)){
                var n=corner.normal()==null?flat:corner.normal();writer.write("vn "+n.x()+" "+n.y()+" "+n.z()+"\n");
            }
        }
        String part=null,material=null;int face=0;
        while(face<state.faces().size()){
            var f=state.faces().get(face);
            if(!f.part().equals(part)){part=f.part();writer.write("o "+part+"\n");}
            if(library!=null&&!f.material().equals(material)){material=f.material();String alias=aliases.get(material);if(alias==null)throw new IOException("Missing material export alias");writer.write("usemtl "+alias+"\n");}
            writer.write("f");int corner=0;for(var c:List.of(f.a(),f.b(),f.c())){writer.write(" "+ids.get(c.vertex())+"/"+(face*3+corner+1)+"/"+(face*3+corner+1));corner++;}
            if(plainQuads.contains(face)){
                int index=face*3+6;
                writer.write(" "+ids.get(state.faces().get(face+1).c().vertex())+"/"+index+"/"+index);
                face++;
            }
            writer.write("\n");face++;
        }writer.flush();
    }
    private static boolean representableQuad(EditorTopologyProject.Face a, EditorTopologyProject.Face b,
                                              Map<Long,EditorTopologyProject.Vertex> points)throws IOException {
        if (!a.material().equals(b.material()) || !a.a().equals(b.a()) || !a.c().equals(b.b())) return false;
        if (a.a().normal()!=null && a.c().normal()!=null) return true;
        var first = new EditorMeshPipeline.Triangle(a.part(),vertex(a.a(),points),vertex(a.b(),points),vertex(a.c(),points));
        var second = new EditorMeshPipeline.Triangle(b.part(),vertex(b.a(),points),vertex(b.b(),points),vertex(b.c(),points));
        return faceNormal(first).equals(faceNormal(second));
    }
    private static EditorMeshPipeline.Normal faceNormal(EditorMeshPipeline.Triangle t)throws IOException{try{return EditorMeshNormals.face(t);}catch(IllegalArgumentException invalid){throw new IOException("OBJ export needs finite nondegenerate face normals",invalid);}}
    private static EditorMeshPipeline.Vertex vertex(EditorTopologyProject.Corner c,Map<Long,EditorTopologyProject.Vertex> points){var p=points.get(c.vertex());return new EditorMeshPipeline.Vertex(p.x(),p.y(),p.z(),c.u(),c.v(),c.normal());}
    /** Readback uses the same native OBJ decoder as the actual human importer. */
    public static void validate(EditorTopologyProject.State state,byte[] bytes)throws IOException{
        validate(state,EditorMeshPipeline.read("export.obj",bytes));
    }
    static void validate(EditorTopologyProject.State state,EditorMeshPipeline.Mesh parsed)throws IOException{
        var ids=indices(state);
        if(!parsed.quads().equals(EditorMeshQuad.ordinals(state.quads(),state.faces().stream().map(EditorTopologyProject.Face::id).toList())))throw new IOException("OBJ readback lost authored quad boundaries");
        if(!parsed.frames().equals(state.frames()))throw new IOException("OBJ readback local frames differ");
        if(!parsed.materials().equals(state.materials()))throw new IOException("OBJ readback material definitions differ");
        if(parsed.triangles().size()!=state.faces().size())throw new IOException("OBJ readback face count differs");
        var expected=new ArrayList<Long>();for(var f:state.faces())for(var c:List.of(f.a(),f.b(),f.c()))expected.add((long)ids.get(c.vertex())-1);
        if(!parsed.cornerVertexIds().equals(expected))throw new IOException("OBJ readback changed native connectivity");
        var positions=new HashMap<Long,EditorTopologyProject.Vertex>();for(var p:state.vertices())positions.put(p.id(),p);
        for(int i=0;i<state.faces().size();i++){
            var f=state.faces().get(i);var t=parsed.triangles().get(i);if(!f.part().equals(t.part())||!f.material().equals(t.material()))throw new IOException("OBJ readback part/material differs");
            var nativeTriangle=new EditorMeshPipeline.Triangle(f.part(),vertex(f.a(),positions),vertex(f.b(),positions),vertex(f.c(),positions));var flat=faceNormal(nativeTriangle);
            var actual=List.of(t.a(),t.b(),t.c());var corners=List.of(f.a(),f.b(),f.c());
            for(int j=0;j<3;j++){var a=actual.get(j);var c=corners.get(j);var p=positions.get(c.vertex());if(a.x()!=p.x()||a.y()!=p.y()||a.z()!=p.z()||a.u()!=c.u()||a.v()!=c.v())throw new IOException("OBJ readback geometry/UV/winding differs");var expectedNormal=c.normal()==null?flat:c.normal();if(!expectedNormal.equals(a.normal()))throw new IOException("OBJ readback corner normal differs");}
        }
    }
}
