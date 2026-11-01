package org.beckenburg.editor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Portable physical railway mesh authoring. Emits actual ballast, two elevated
 * rails and individual sleepers on a persisted railway spline. No placeholder
 * surfaces, preassigned textures, game renderer, or Beckenburg scene dependency.
 *
 * Positions are world metres, Y-up, matching TerrainEntranceMesh. This is
 * geometry only: no train simulation, collision, bridge, switch or tunnel.
 */
public final class TerrainRailMesh {
    private static final int MAX_FACES=TerrainEntranceMesh.MAX_TRIANGLES;
    public record Profile(double gauge,double ballastTopWidth,double ballastBottomWidth,
                          double ballastDepth,double sleeperLength,double sleeperWidth,
                          double sleeperHeight,double sleeperSpacing,
                          double railHeadWidth,double railHeight,double minimumRadius) {
        public Profile {
            if(!Double.isFinite(gauge+ballastTopWidth+ballastBottomWidth+
                ballastDepth+sleeperLength+sleeperWidth+sleeperHeight+
                sleeperSpacing+railHeadWidth+railHeight+minimumRadius)
                ||gauge<.3||gauge>3||ballastTopWidth<=gauge
                ||ballastBottomWidth<ballastTopWidth
                ||ballastDepth<=0||ballastDepth>5
                ||sleeperLength<gauge+.15||sleeperLength>ballastTopWidth
                ||sleeperWidth<=.05||sleeperWidth>.6
                ||sleeperHeight<=.04||sleeperHeight>.4
                ||sleeperSpacing<.3||sleeperSpacing>3
                ||railHeadWidth<.025||railHeadWidth>.3
                ||railHeight<.05||railHeight>.5||minimumRadius<0)
                throw new IllegalArgumentException("Invalid railway cross section");
        }
        public static Profile standard(){
            return new Profile(1.435,2.65,3.35,.40,2.45,.23,.15,.65,
                .075,.18,5);
        }
    }
    public record Output(EditorMeshPipeline.Mesh mesh,int sleepers,double lengthMetres,
                         double minimumMeasuredRadiusMetres) {}
    private record Centre(double x,double y,double z,double nx,double nz) {}
    private final ArrayList<EditorMeshPipeline.Triangle> faces=new ArrayList<>();

    private TerrainRailMesh(){}

    private static double[] p(double x,double y,double z){return new double[]{x,y,z};}
    private void triangle(String part,double[] a,double[] b,double[] c){
        if(faces.size()>=MAX_FACES)
            throw new IllegalArgumentException(
                "Railway OBJ exceeds 16,000 triangles; split the alignment into shorter export sections");
        double ux=b[0]-a[0],uy=b[1]-a[1],uz=b[2]-a[2];
        double vx=c[0]-a[0],vy=c[1]-a[1],vz=c[2]-a[2];
        double crossX=uy*vz-uz*vy,crossY=uz*vx-ux*vz,crossZ=ux*vy-uy*vx;
        if(crossX*crossX+crossY*crossY+crossZ*crossZ<1e-15)return;
        var va=new EditorMeshPipeline.Vertex(a[0],a[1],a[2],a[0]*.2,a[2]*.2);
        var vb=new EditorMeshPipeline.Vertex(b[0],b[1],b[2],b[0]*.2,b[2]*.2);
        var vc=new EditorMeshPipeline.Vertex(c[0],c[1],c[2],c[0]*.2,c[2]*.2);
        faces.add(new EditorMeshPipeline.Triangle(part,va,vb,vc));
    }
    private void quad(String part,double[] a,double[] b,double[] c,double[] d){
        // Cross sections are emitted with the centreline's left normal
        // (-tangentZ,+tangentX). Their original A-B-C winding pointed INTO
        // the ballast/sleeper/rail prism: top faces pointed down and left
        // walls pointed right. Reverse each triangle so render back-face
        // culling and walkable support use the actual outward normals.
        triangle(part,a,c,b);triangle(part,a,d,c);
    }
    private static double[] cross(Centre c,double offset,double y){
        return p(c.x()+c.nx()*offset,c.y()+y,c.z()+c.nz()*offset);
    }
    /** Transverse rectangular or trapezoidal extrusions along centreline. */
    private void prismSegment(String part,Centre a,Centre b,
                              double centerOffset,double halfTop,double halfBottom,
                              double base,double top){
        double[] al=cross(a,centerOffset-halfTop,top),
                 ar=cross(a,centerOffset+halfTop,top),
                 bl=cross(b,centerOffset-halfTop,top),
                 br=cross(b,centerOffset+halfTop,top),
                 abl=cross(a,centerOffset-halfBottom,base),
                 abr=cross(a,centerOffset+halfBottom,base),
                 bbl=cross(b,centerOffset-halfBottom,base),
                 bbr=cross(b,centerOffset+halfBottom,base);
        quad(part,al,bl,br,ar);
        quad(part,abl,abr,bbr,bbl);
        quad(part,abl,bbl,bl,al);
        quad(part,ar,br,bbr,abr);
        quad(part,al,ar,abr,abl);
        quad(part,bbl,bbr,br,bl);
    }
    private void sleeper(Centre c,double tangentX,double tangentZ,Profile profile){
        double half=profile.sleeperLength()*.5;
        double across=profile.sleeperWidth()*.5;
        Centre a=new Centre(c.x()-tangentX*across,c.y(),c.z()-tangentZ*across,c.nx(),c.nz());
        Centre b=new Centre(c.x()+tangentX*across,c.y(),c.z()+tangentZ*across,c.nx(),c.nz());
        prismSegment("rail_sleepers",a,b,0,half,half,.03,.03+profile.sleeperHeight());
    }
    private static Centre centre(TerrainLayerStack.Point point,
                                 StitchedHeightmapWorld.Settings world,
                                 double nx,double nz){
        double step=world.metresPerCell();
        return new Centre(world.originX()+point.x()*step,point.elevation(),
            world.originZ()+point.z()*step,nx,nz);
    }

    public static Output build(StitchedHeightmapWorld.Settings world,
                               TerrainInfrastructureSplines.Alignment route,
                               Profile profile){
        Objects.requireNonNull(world);Objects.requireNonNull(route);
        Objects.requireNonNull(profile);
        if(route.kind()!=TerrainInfrastructureSplines.Kind.RAIL)
            throw new IllegalArgumentException("Track mesh requires a RAIL alignment");
        var samples=TerrainInfrastructureSplines.evaluate(route,world.metresPerCell());
        if(samples.size()<2)throw new IllegalArgumentException("Rail spline has no length");
        if(samples.size()>15_000)throw new IllegalArgumentException("Split exceptionally long rail lines");
        ArrayList<Centre> centers=new ArrayList<>();
        double distance=0,minRadius=Double.POSITIVE_INFINITY;
        double prevTx=0,prevTz=0,prevRun=0;
        for(int i=0;i<samples.size();i++){
            var here=samples.get(i);
            var before=samples.get(Math.max(0,i-1));
            var after=samples.get(Math.min(samples.size()-1,i+1));
            double tx=after.x()-before.x(),tz=after.z()-before.z();
            double tangentLen=Math.hypot(tx,tz);
            if(tangentLen<1e-8)
                throw new IllegalArgumentException("Rail alignment has a zero-length tangent");
            tx/=tangentLen;tz/=tangentLen;
            centers.add(centre(here,world,-tz,tx));
            if(i>0){
                double run=Math.hypot(here.x()-before.x(),
                    here.z()-before.z())*world.metresPerCell();
                distance+=run;
                if(i>1){
                    double angle=Math.acos(Math.max(-1,Math.min(1,prevTx*tx+prevTz*tz)));
                    if(angle>1e-6){
                        double radius=(run+prevRun)*.5/angle;
                        minRadius=Math.min(minRadius,radius);
                    }
                }
                prevRun=run;
            }
            prevTx=tx;prevTz=tz;
        }
        if(distance<profile.sleeperSpacing())
            throw new IllegalArgumentException("Rail line shorter than one sleeper pitch");
        if(minRadius+1e-7<profile.minimumRadius())
            throw new IllegalArgumentException("Rail centreline curvature is tighter than "+
                profile.minimumRadius()+" metre minimum radius");
        TerrainRailMesh maker=new TerrainRailMesh();
        // The bed actually has sloped, finite-depth sides; not a single flat
        // sheet under two cosmetic painted stripes.
        for(int i=1;i<centers.size();i++){
            Centre a=centers.get(i-1),b=centers.get(i);
            maker.prismSegment("rail_ballast",a,b,0,
                profile.ballastTopWidth()*.5,profile.ballastBottomWidth()*.5,
                -profile.ballastDepth(),0);
            for(int side=-1;side<=1;side+=2)
                maker.prismSegment(side<0?"rail_left":"rail_right",a,b,
                    side*profile.gauge()*.5,
                    profile.railHeadWidth()*.5,profile.railHeadWidth()*.5,
                    .03+profile.sleeperHeight(),.03+profile.sleeperHeight()+profile.railHeight());
        }
        int sleepers=0;
        double traversed=0,next=profile.sleeperSpacing()*.5;
        for(int i=1;i<centers.size();i++){
            Centre a=centers.get(i-1),b=centers.get(i);
            double dx=b.x()-a.x(),dz=b.z()-a.z(),segment=Math.hypot(dx,dz);
            if(segment<1e-8)continue;
            while(next<=traversed+segment+1e-8){
                double t=Math.max(0,Math.min(1,(next-traversed)/segment));
                double nx=a.nx()*(1-t)+b.nx()*t,nz=a.nz()*(1-t)+b.nz()*t;
                double len=Math.hypot(nx,nz);
                nx/=len;nz/=len;
                maker.sleeper(new Centre(a.x()+dx*t,a.y()+(b.y()-a.y())*t,
                    a.z()+dz*t,nx,nz),dx/segment,dz/segment,profile);
                sleepers++;next+=profile.sleeperSpacing();
            }
            traversed+=segment;
        }
        return new Output(new EditorMeshPipeline.Mesh(
            "Railway_"+route.id(),maker.faces,0),sleepers,distance,minRadius);
    }
    /** Use the SAME validated OBJ writer/reader as authored entrance geometry. */
    public static Output export(StitchedHeightmapWorld world,
                                TerrainInfrastructureSplines.Alignment route,
                                Profile profile,Path output)throws IOException {
        Objects.requireNonNull(world);Objects.requireNonNull(output);
        Output result=build(world.settings(),route,profile);
        TerrainEntranceMesh.writeObj(result.mesh(),output);
        return result;
    }
}
