package org.beckenburg;

import java.util.*;
import org.beckenburg.LevelDefinition.*;

/** Shared indexed source meshes plus small immutable placement records; no retained world triangle copies.
 * Adapts Malefic Veil's template/placement separation to this renderer's real textured depth buffer.
 */
public final class SceneryInstances {
    public static final class Template {
        public final String mesh;private final GlbProps.Vertex[] vertices;private final int[] indices,materialSlots,materials;
        public final double height;
        Template(String mesh){
            this.mesh=mesh;var source=GlbProps.model(mesh);var unique=new LinkedHashMap<GlbProps.Vertex,Integer>();var palette=new LinkedHashMap<Integer,Integer>();
            indices=new int[source.triangles().size()*3];materialSlots=new int[source.triangles().size()];int n=0,t=0;double top=0;
            for(var face:source.triangles()){
                for(var v:List.of(face.a(),face.b(),face.c())){indices[n++]=unique.computeIfAbsent(v,k->unique.size());top=Math.max(top,v.y());}
                materialSlots[t++]=palette.computeIfAbsent(face.material(),k->palette.size());
            }
            vertices=unique.keySet().toArray(GlbProps.Vertex[]::new);materials=palette.keySet().stream().mapToInt(Integer::intValue).toArray();height=top;
        }
        public int vertexCount(){return vertices.length;}public int triangleCount(){return materialSlots.length;}
        public GlbProps.Vertex vertex(int i){return vertices[i];}public int index(int i){return indices[i];}
    }
    public static final class Instance {
        public final Template template;public final String mesh;public final int room;
        public final double x,y,z,sx,sy,sz,yaw,cx,cy,cz,radius;
        public final FoliageVariation.Profile variation;private final int[] materials;
        private final double cosYaw,sinYaw;
        Instance(Template template,int room,double x,double y,double z,double sx,double sy,double sz,double yaw){
            this.template=template;mesh=template.mesh;this.room=room;this.x=x;this.y=y;this.z=z;this.sx=sx;this.sy=sy;this.sz=sz;this.yaw=yaw;
            variation=FoliageVariation.profile(mesh,x,z);materials=template.materials.clone();
            cosYaw=Math.cos(yaw+variation.rotation());sinYaw=Math.sin(yaw+variation.rotation());
            for(int i=0;i<materials.length;i++)if(mesh.equals("tv_mp_1")&&GlbProps.materialName(materials[i]).equals("crt_screen_mp_1"))materials[i]=GlbProps.televisionMaterial(x,y,z);
            for(int i=0;i<materials.length;i++)if(FoliageVariation.foliage(mesh)&&FoliageVariation.leafMaterial(GlbProps.materialName(materials[i])))materials[i]=GlbProps.foliageMaterial(materials[i],variation.seed());
            double x0=Double.POSITIVE_INFINITY,y0=x0,z0=x0,x1=-x0,y1=-x0,z1=-x0;
            for(int i=0;i<template.vertexCount();i++){var p=point(i);x0=Math.min(x0,p.x());y0=Math.min(y0,p.y());z0=Math.min(z0,p.z());x1=Math.max(x1,p.x());y1=Math.max(y1,p.y());z1=Math.max(z1,p.z());}
            cx=(x0+x1)/2;cy=(y0+y1)/2;cz=(z0+z1)/2;radius=Math.sqrt((x1-x0)*(x1-x0)+(y1-y0)*(y1-y0)+(z1-z0)*(z1-z0))/2+.002;
        }
        public Vec point(int i){
            var v=template.vertex(i);var p=variation.point(v.x()*sx,v.y()*sy,v.z()*sz,template.height*sy);
            return new Vec(x+p.x()*cosYaw-p.z()*sinYaw,y+p.y(),z+p.x()*sinYaw+p.z()*cosYaw);
        }
        public int material(int triangle){return materials[template.materialSlots[triangle]];}
        public boolean visible(double cameraX,double eye,double cameraZ,double heading,double pitch,double hx,double hy,double near){
            double dx=cx-cameraX,dy=cy-eye,dz=cz-cameraZ,side=-dx*Math.cos(heading)+dz*Math.sin(heading),forward=dx*Math.sin(heading)+dz*Math.cos(heading);
            double up=dy*Math.cos(pitch)-forward*Math.sin(pitch),depth=dy*Math.sin(pitch)+forward*Math.cos(pitch);
            return depth+radius>=near&&Math.abs(side)<=depth*hx+radius*Math.sqrt(1+hx*hx)&&Math.abs(up)<=depth*hy+radius*Math.sqrt(1+hy*hy);
        }
    }
    private final List<Instance> instances;private final List<Template> templates;
    public SceneryInstances(LevelDefinition level,int hidingDensity){
        var sources=new LinkedHashMap<String,Template>();var placements=new ArrayList<Instance>();
        for(var p:level.props()){
            if(p.mesh().equals("level_shrub_refuge")&&OutdoorHiding.containing(level,p.x(),p.y(),p.z(),hidingDensity)==null)continue;
            add(sources,placements,p.mesh(),p.room(),p.x(),p.y(),p.z(),p.scale(),p.scale(),p.scale(),p.yaw());
        }
        for(var s:level.sockets()){
            if(s.type().equals("landscape_detail")){
                String[] shape=s.style().split("\\|");if(shape.length!=4)throw new IllegalArgumentException("landscape scale contract");
                add(sources,placements,shape[0],s.room(),s.x(),s.y(),s.z(),Double.parseDouble(shape[1]),Double.parseDouble(shape[2]),Double.parseDouble(shape[3]),s.yaw());
            }else if(s.type().equals("landscape_tree"))add(sources,placements,s.style(),s.room(),s.x(),s.y(),s.z(),1,1,1,s.yaw());
        }
        instances=List.copyOf(placements);templates=List.copyOf(sources.values());
    }
    private static void add(Map<String,Template> sources,List<Instance> instances,String mesh,int room,double x,double y,double z,double sx,double sy,double sz,double yaw){
        if(!Double.isFinite(sx+sy+sz+x+y+z+yaw)||sx<=0||sy<=0||sz<=0)throw new IllegalArgumentException("invalid scenery transform");
        instances.add(new Instance(sources.computeIfAbsent(mesh,Template::new),room,x,y,z,sx,sy,sz,yaw));
    }
    public List<Instance> instances(){return instances;}public List<Template> templates(){return templates;}
    public long expandedTriangles(){return instances.stream().mapToLong(i->i.template.triangleCount()).sum();}
    public long sharedTriangles(){return templates.stream().mapToLong(Template::triangleCount).sum();}
}
