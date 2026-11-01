package org.beckenburg.editor;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Independent, persistent road/rail/river design splines, expressed in GLOBAL
 * heightmap sample coordinates, not a Beckenburg scene or game object.
 * Applying an alignment modifies a selected non-destructive terrain layer.
 * Track meshes, switches, rolling stock and runtime collision are separate adapters.
 */
public final class TerrainInfrastructureSplines {
    private static final int MAGIC=0x5453504C,VERSION=1,MAX=512;
    public enum Kind { RAIL, ROAD, RIVER }
    public record Alignment(String id,Kind kind,List<TerrainLayerStack.Point> controls,
                            double halfWidth,double shoulder,double maximumGradePercent) {
        public Alignment {
            if(id==null||!id.matches("[A-Za-z0-9_-]{1,64}")||kind==null
                ||controls==null||controls.size()<2||controls.size()>256
                ||!Double.isFinite(halfWidth)||halfWidth<=0||halfWidth>512
                ||!Double.isFinite(shoulder)||shoulder<0||shoulder>512
                ||!Double.isFinite(maximumGradePercent)||maximumGradePercent<=0
                ||maximumGradePercent>1000)
                throw new IllegalArgumentException("Invalid infrastructure spline");
            controls=List.copyOf(controls);
            for(TerrainLayerStack.Point p:controls){
                if(p==null||!Double.isFinite(p.x())||!Double.isFinite(p.z())
                    ||!Double.isFinite(p.elevation())||Math.abs(p.x())>1_000_000_000
                    ||Math.abs(p.z())>1_000_000_000)
                    throw new IllegalArgumentException("Invalid spline control");
            }
        }
    }
    private final Path file;
    private final LinkedHashMap<String,Alignment> alignments=new LinkedHashMap<>();
    private TerrainInfrastructureSplines(Path file){this.file=file;}
    public static TerrainInfrastructureSplines open(StitchedHeightmapWorld world)throws IOException{
        Objects.requireNonNull(world);
        var result=new TerrainInfrastructureSplines(
            world.directory().resolve("terrain-infrastructure-splines.bin"));
        if(!Files.isRegularFile(result.file))return result;
        if(Files.size(result.file)>4_000_000)throw new IOException("Oversized spline document");
        try(var in=new DataInputStream(new BufferedInputStream(Files.newInputStream(result.file)))){
            if(in.readInt()!=MAGIC||in.readInt()!=VERSION)
                throw new IOException("Unsupported infrastructure spline format");
            int count=in.readInt();
            if(count<0||count>MAX)throw new IOException("Invalid spline count");
            for(int k=0;k<count;k++){
                String id=in.readUTF();int index=in.readInt();
                if(index<0||index>=Kind.values().length)
                    throw new IOException("Unknown infrastructure profile");
                Kind kind=Kind.values()[index];
                double width=in.readDouble(),shoulder=in.readDouble(),grade=in.readDouble();
                int points=in.readInt();if(points<2||points>256)
                    throw new IOException("Invalid control count");
                List<TerrainLayerStack.Point> controls=new ArrayList<>();
                for(int j=0;j<points;j++)controls.add(
                    new TerrainLayerStack.Point(in.readDouble(),in.readDouble(),in.readDouble()));
                Alignment a;
                try{a=new Alignment(id,kind,controls,width,shoulder,grade);}
                catch(IllegalArgumentException ex){throw new IOException("Invalid spline entry",ex);}
                if(result.alignments.putIfAbsent(id,a)!=null)
                    throw new IOException("Duplicate infrastructure spline ID");
            }
            if(in.read()!=-1)throw new IOException("Trailing infrastructure spline data");
        }
        return result;
    }
    public Path path(){return file;}
    public List<Alignment> list(){return List.copyOf(alignments.values());}
    public void put(Alignment a){
        Objects.requireNonNull(a);
        if(!alignments.containsKey(a.id())&&alignments.size()>=MAX)
            throw new IllegalStateException("Infrastructure spline limit");
        alignments.put(a.id(),a);
    }
    public boolean remove(String id){return alignments.remove(id)!=null;}
    public void save()throws IOException{
        Files.createDirectories(file.getParent());
        Path temp=Files.createTempFile(file.getParent(),".splines-",".tmp");
        try{
            try(var out=new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(temp)))){
                out.writeInt(MAGIC);out.writeInt(VERSION);out.writeInt(alignments.size());
                for(Alignment a:alignments.values()){
                    out.writeUTF(a.id());out.writeInt(a.kind().ordinal());
                    out.writeDouble(a.halfWidth());out.writeDouble(a.shoulder());
                    out.writeDouble(a.maximumGradePercent());
                    out.writeInt(a.controls().size());
                    for(var p:a.controls()){
                        out.writeDouble(p.x());out.writeDouble(p.z());out.writeDouble(p.elevation());
                    }
                }
            }
            Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
        }finally{Files.deleteIfExists(temp);}
    }
    private static double catmull(double p0,double p1,double p2,double p3,double t){
        return .5*((2*p1)+(-p0+p2)*t+
            (2*p0-5*p1+4*p2-p3)*t*t+(-p0+3*p1-3*p2+p3)*t*t*t);
    }
    /** Dense, smooth centerline; endpoint positions are retained exactly. */
    public static List<TerrainLayerStack.Point> evaluate(Alignment a,double metresPerSample){
        if(metresPerSample<=0||!Double.isFinite(metresPerSample))
            throw new IllegalArgumentException("Sample spacing");
        var p=a.controls();
        var result=new ArrayList<TerrainLayerStack.Point>();
        for(int segment=0;segment<p.size()-1;segment++){
            var v0=p.get(Math.max(0,segment-1));
            var v1=p.get(segment);
            var v2=p.get(segment+1);
            var v3=p.get(Math.min(p.size()-1,segment+2));
            double distance=Math.hypot(v2.x()-v1.x(),v2.z()-v1.z());
            int steps=Math.max(1,Math.min(256,(int)Math.ceil(distance/1.5)));
            for(int i=0;i<steps;i++){
                double t=i/(double)steps;
                result.add(new TerrainLayerStack.Point(
                    catmull(v0.x(),v1.x(),v2.x(),v3.x(),t),
                    catmull(v0.z(),v1.z(),v2.z(),v3.z(),t),
                    catmull(v0.elevation(),v1.elevation(),v2.elevation(),v3.elevation(),t)));
            }
        }
        result.add(p.get(p.size()-1));
        if(result.size()>50000)throw new IllegalArgumentException("Too many spline samples");
        // Rail/road centerline grade is a functional geometry constraint, not a visual test.
        if(a.kind()!=Kind.RIVER){
            for(int i=1;i<result.size();i++){
                var b=result.get(i);
                var previous=result.get(i-1);
                double horizontal=Math.hypot(b.x()-previous.x(),b.z()-previous.z())*
                    metresPerSample;
                double grade=horizontal==0?Double.POSITIVE_INFINITY:
                    Math.abs(b.elevation()-previous.elevation())/horizontal*100;
                if(grade>a.maximumGradePercent()+1e-5)
                    throw new IllegalArgumentException(a.id()+
                        " exceeds "+a.maximumGradePercent()+"% allowed grade at sample "+i+
                        " ("+Math.round(grade*100)/100.0+"%)");
            }
        }
        return List.copyOf(result);
    }
    /** Fit subsequent control elevations to a maximum incline while keeping the start datum. */
    public static List<TerrainLayerStack.Point> limitGrade(List<TerrainLayerStack.Point> controls,
                           double metresPerSample,double percent){
        if(controls==null||controls.size()<2||metresPerSample<=0||percent<=0)
            throw new IllegalArgumentException("Invalid grade controls");
        var result=new ArrayList<TerrainLayerStack.Point>();
        result.add(controls.get(0));
        for(int i=1;i<controls.size();i++){
            var prev=result.get(result.size()-1);
            var next=controls.get(i);
            double run=Math.hypot(next.x()-prev.x(),next.z()-prev.z())*metresPerSample;
            double rise=run*percent/100;
            double clamped=Math.max(prev.elevation()-rise,
                Math.min(prev.elevation()+rise,next.elevation()));
            result.add(new TerrainLayerStack.Point(next.x(),next.z(),clamped));
        }
        return List.copyOf(result);
    }
    /** Existing paths are independent; applying one is explicit and editable. */
    public void apply(TerrainLayerStack editor,Alignment spline){
        Objects.requireNonNull(editor);Objects.requireNonNull(spline);
        var points=evaluate(spline,editor.metresPerCell());
        var local=new ArrayList<TerrainLayerStack.Point>();
        for(var p:points)local.add(new TerrainLayerStack.Point(
            p.x()-(long)editor.tileX()*editor.cells(),
            p.z()-(long)editor.tileZ()*editor.cells(),p.elevation()));
        editor.gradePath(local,spline.halfWidth(),spline.shoulder());
        TerrainLayerStack.Material material=switch(spline.kind()){
            case RAIL -> TerrainLayerStack.Material.GRAVEL;
            case ROAD -> TerrainLayerStack.Material.DIRT;
            case RIVER -> TerrainLayerStack.Material.RIVERBED;
        };
        for(var p:local)editor.paintMaterial(p.x(),p.z(),
            spline.halfWidth(),material,90);
    }
}
