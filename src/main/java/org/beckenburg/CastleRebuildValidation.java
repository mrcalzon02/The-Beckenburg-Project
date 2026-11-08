package org.beckenburg;

import java.util.*;

/** Validate the merged native scene itself before an editor rebuild transaction commits. */
final class CastleRebuildValidation {
    private CastleRebuildValidation(){}
    /** Shared source-triangle check; callers provide their own world/pivot transform. */
    static boolean modelConflict(List<LevelDefinition.Vec[]> first,List<LevelDefinition.Vec[]> second){
        var a=new ArrayList<Tri>();var b=new ArrayList<Tri>();
        for(var pair:List.of(Map.entry(first,a),Map.entry(second,b)))for(var face:pair.getKey()){
            var x=face[0];var y=face[1];var z=face[2];pair.getValue().add(new Tri(new V(x.x(),x.y(),x.z()),new V(y.x(),y.y(),y.z()),new V(z.x(),z.y(),z.z())));
        }
        if(geometryKey(a).equals(geometryKey(b)))return true;
        for(int axis=0;axis<3;axis++){
            double amin=Double.POSITIVE_INFINITY,amax=Double.NEGATIVE_INFINITY,bmin=amin,bmax=amax;
            for(var t:first)for(var v:t){double p=axis==0?v.x():axis==1?v.y():v.z();amin=Math.min(amin,p);amax=Math.max(amax,p);}
            for(var t:second)for(var v:t){double p=axis==0?v.x():axis==1?v.y():v.z();bmin=Math.min(bmin,p);bmax=Math.max(bmax,p);}
            if(amax<=bmin+.002||bmax<=amin+.002)return false;
        }
        for(var t:a)for(var u:b)if(overlap(t,u))return true;
        return contained(a,closedSolids(b))||contained(b,closedSolids(a));
    }
    private record Cell(int x,int y,int z){}
    private static Cell key(LevelDefinition.Vec p){return new Cell((int)Math.round(p.x()*8),(int)Math.round(p.y()*10000),(int)Math.round(p.z()*8));}
    private static void require(boolean value,String message){if(!value)throw new IllegalArgumentException("Castle rebuild conflict: "+message);}
    static void validate(CastleLevelEdits scene,CastleRebuildUpgrade.Plan plan,List<CastleHubArchitecturalPlan.Wall> priorWalls){
        validateRetained(scene,plan,priorWalls);
        var map=new CastleHubMap();
        for(var slot:CastleDoorSlots.all())if(scene.doorType(slot.id())!=CastleDoorSlots.Type.EMPTY)map.toggleDoor(slot.id());
        double[] spawn=scene.spawn();require(map.traversable(spawn[0],spawn[1],0),"saved spawn is unsupported or obstructed");
        var start=new LevelDefinition.Vec(spawn[0],0,spawn[1]);var seen=new HashSet<Cell>();var queue=new ArrayDeque<LevelDefinition.Vec>();seen.add(key(start));queue.add(start);
        var rooms=new HashSet<String>();var stations=EnumSet.noneOf(CastleHubMap.Station.class);
        while(!queue.isEmpty()){
            var p=queue.removeFirst();var room=CastleRooms.at(p.x(),p.y(),p.z());if(room!=null)rooms.add(room.id());
            if(Math.abs(p.y())<.01)for(var station:CastleHubMap.Station.values())if(!stations.contains(station)){
                var at=scene.station(station);double dx=at[0]-p.x(),dz=at[1]-p.z();
                if(dx*dx+dz*dz<12&&map.aimed(p.x(),p.z(),Math.atan2(dx,dz))==station)stations.add(station);
            }
            for(double[] d:new double[][]{{.125,0},{-.125,0},{0,.125},{0,-.125}}){
                double x=p.x()+d[0],z=p.z()+d[1],y=CastleHubArchitecturalPlan.elevation(x,z,p.y());var next=new LevelDefinition.Vec(x,y,z);var k=key(next);
                if(!seen.contains(k)&&CastleHubPanel.safeElevationTransition(p.y(),y,x,z)&&map.traversable(x,z,y)){seen.add(k);queue.add(next);}
            }
            require(seen.size()<400000,"navigation exceeds the bounded Castle envelope");
        }
        var expected=new HashSet<String>();for(var r:CastleRooms.ALL)expected.add(r.id());for(var r:CastleAnnex.SPACES)expected.add(r.id());for(var r:CastleCellarLayout.ROOMS)expected.add(r.id());
        var seals=new HashSet<>(CastleGroundLayout.SEALED);seals.addAll(CastleUpperLayout.SEALED);expected.removeAll(seals);
        for(String id:expected)require(rooms.contains(id),"saved edit disconnects room "+id);
        for(String id:seals)require(!rooms.contains(id),"saved edit opens sealed context "+id);
        require(expected.size()==39&&seals.size()==3,"authoritative Castle search programme changed");
        require(stations.size()==CastleHubMap.Station.values().length,"saved edit obstructs a station approach: "+stations);
    }
    private record V(double x,double y,double z){}
    private record Tri(V a,V b,V c){}
    private static V sub(V a,V b){return new V(a.x-b.x,a.y-b.y,a.z-b.z);}
    private static V cross(V a,V b){return new V(a.y*b.z-a.z*b.y,a.z*b.x-a.x*b.z,a.x*b.y-a.y*b.x);}
    private static double dot(V a,V b){return a.x*b.x+a.y*b.y+a.z*b.z;}
    private static V world(CastleLevelEdits.Prop p,GlbProps.Vertex v,double[] b){
        double x=(v.x()-b[0])*p.scale(),z=(v.z()-b[2])*p.scale();return new V(p.x()+x*Math.cos(p.yaw())-z*Math.sin(p.yaw()),p.y()+(v.y()-b[1])*p.scale(),p.z()+x*Math.sin(p.yaw())+z*Math.cos(p.yaw()));
    }
    private static List<Tri> triangles(CastleLevelEdits.Prop p){
        var result=new ArrayList<Tri>();var b=SceneRenderer.castleBounds(p.mesh());
        for(var t:GlbProps.model(p.mesh()).triangles())result.add(new Tri(world(p,t.a(),b),world(p,t.b(),b),world(p,t.c(),b)));return result;
    }
    private static boolean crosses(V a,V b,CastleHubArchitecturalPlan.Wall w){
        if(w.hasDoor())return false;double h=CastleDoorSlots.MASONRY_THICKNESS/2-.002;
        double x0=Math.min(w.x0(),w.x1())-(w.horizontal()?0:h),x1=Math.max(w.x0(),w.x1())+(w.horizontal()?0:h);
        double z0=Math.min(w.z0(),w.z1())-(w.horizontal()?h:0),z1=Math.max(w.z0(),w.z1())+(w.horizontal()?h:0);
        boolean inside=a.x>x0+.002&&a.x<x1-.002&&a.z>z0+.002&&a.z<z1-.002&&a.y>w.bottom()+.002&&a.y<w.top()-.002;
        return inside||CastleDoorSlots.crossesBox(a.x,a.y,a.z,b.x,b.y,b.z,x0,w.bottom()+.002,z0,x1,w.top()-.002,z1);
    }
    private static boolean floor(CastleLevelEdits scene,V p){
        if(Math.abs(p.y)<.002&&CastleRooms.ground(p.x,p.z)&&!scene.floorOpening(p.x,p.z,0))return true;
        return scene.surfaces().stream().anyMatch(s->s.kind()==CastleLevelEdits.Kind.FLOOR&&Math.abs(s.base()-p.y)<.002&&p.x>=s.x0()&&p.x<=s.x1()&&p.z>=s.z0()&&p.z<=s.z1()&&!scene.floorOpening(p.x,p.z,s.base()));
    }
    private static boolean onTriangle(V p,Tri t){
        double ax=t.b.x-t.a.x,az=t.b.z-t.a.z,bx=t.c.x-t.a.x,bz=t.c.z-t.a.z,den=ax*bz-az*bx;if(Math.abs(den)<1e-10)return false;
        double dx=p.x-t.a.x,dz=p.z-t.a.z,u=(dx*bz-dz*bx)/den,v=(ax*dz-az*dx)/den;
        return u>=-1e-6&&v>=-1e-6&&u+v<=1+1e-6&&Math.abs(t.a.y+u*(t.b.y-t.a.y)+v*(t.c.y-t.a.y)-p.y)<.002;
    }
    private static boolean wallContact(V p,CastleHubArchitecturalPlan.Wall w){
        if(w.hasDoor()||p.y<w.bottom()||p.y>w.top())return false;double h=CastleDoorSlots.MASONRY_THICKNESS/2;
        return w.horizontal()?p.x>=Math.min(w.x0(),w.x1())&&p.x<=Math.max(w.x0(),w.x1())&&Math.abs(Math.abs(p.z-w.z0())-h)<.002:
            p.z>=Math.min(w.z0(),w.z1())&&p.z<=Math.max(w.z0(),w.z1())&&Math.abs(Math.abs(p.x-w.x0())-h)<.002;
    }
    private static boolean supported(CastleLevelEdits scene,CastleLevelEdits.Prop prop,List<Tri> tris,Map<String,List<Tri>> geometry){
        for(var t:tris)for(var v:List.of(t.a,t.b,t.c))if(scene.walls().stream().anyMatch(w->wallContact(v,w)))return true;
        var samples=new HashSet<V>();for(var t:tris){for(var v:List.of(t.a,t.b,t.c))if(Math.abs(v.y-prop.y())<.0005)samples.add(v);
            if(Math.abs(t.a.y-prop.y())<.0005&&Math.abs(t.b.y-prop.y())<.0005&&Math.abs(t.c.y-prop.y())<.0005)samples.add(new V((t.a.x+t.b.x+t.c.x)/3,prop.y(),(t.a.z+t.b.z+t.c.z)/3));}
        int contact=0;for(var p:samples){
            boolean touches=floor(scene,p);
            if(!touches)for(var parent:scene.props())if(!parent.id().equals(prop.id())&&parent.y()<=prop.y()){
                if(geometry.computeIfAbsent(parent.id(),id->triangles(parent)).stream().anyMatch(t->onTriangle(p,t))){touches=true;break;}
            }
            if(touches&&++contact>=Math.min(3,samples.size()))return true;
        }return false;
    }
    /** Proper edge crossing; coplanar/contact-only faces are allowed. */
    private static boolean edgeHit(V a,V b,Tri t){
        var d=sub(b,a);var e1=sub(t.b,t.a);var e2=sub(t.c,t.a);var h=cross(d,e2);double den=dot(e1,h);if(Math.abs(den)<1e-10)return false;
        double inverse=1/den;var s=sub(a,t.a);double u=inverse*dot(s,h);if(u<1e-6||u>1-1e-6)return false;
        var q=cross(s,e1);double v=inverse*dot(d,q);if(v<1e-6||u+v>1-1e-6)return false;
        double f=inverse*dot(e2,q);return f>1e-5&&f<1-1e-5;
    }
    private static boolean overlap(Tri a,Tri b){
        return edgeHit(a.a,a.b,b)||edgeHit(a.b,a.c,b)||edgeHit(a.c,a.a,b)||edgeHit(b.a,b.b,a)||edgeHit(b.b,b.c,a)||edgeHit(b.c,b.a,a);
    }
    private record Weld(long x,long y,long z){}
    private static Weld weld(V p){return new Weld(Math.round(p.x*1000000),Math.round(p.y*1000000),Math.round(p.z*1000000));}
    private static int root(int[] parent,int i){while(parent[i]!=i){parent[i]=parent[parent[i]];i=parent[i];}return i;}
    private static List<List<Tri>> closedSolids(List<Tri> mesh){
        var vertices=new HashMap<Weld,Integer>();var owner=new HashMap<Integer,Integer>();int[] parent=new int[mesh.size()];var ids=new ArrayList<int[]>();
        for(int i=0;i<mesh.size();i++){
            parent[i]=i;var t=mesh.get(i);int[] face=new int[3];int n=0;
            for(var p:List.of(t.a,t.b,t.c)){
                int v=vertices.computeIfAbsent(weld(p),k->vertices.size());face[n++]=v;Integer prior=owner.putIfAbsent(v,i);
                if(prior!=null)parent[root(parent,i)]=root(parent,prior);
            }ids.add(face);
        }
        var groups=new HashMap<Integer,List<Integer>>();for(int i=0;i<mesh.size();i++)groups.computeIfAbsent(root(parent,i),k->new ArrayList<>()).add(i);
        var result=new ArrayList<List<Tri>>();
        for(var group:groups.values()){
            var balance=new HashMap<Long,Integer>();
            for(int i:group){int[] f=ids.get(i);for(int edge=0;edge<3;edge++){
                int a=f[edge],b=f[(edge+1)%3];if(a==b)continue;long key=((long)Math.min(a,b)<<32)|(Math.max(a,b)&0xffffffffL);balance.merge(key,a<b?1:-1,Integer::sum);
            }}
            // An oriented closed chain can contain several overlapping solid parts.
            // Open labels, curtains and glass sheets do not acquire a solid volume.
            if(!balance.isEmpty()&&balance.values().stream().allMatch(n->n==0))result.add(group.stream().map(mesh::get).toList());
        }return result;
    }
    private static boolean onSurface(V p,Tri t){
        var a=sub(t.b,t.a);var b=sub(t.c,t.a);var q=sub(p,t.a);var n=cross(a,b);double length=Math.sqrt(dot(n,n));if(length<1e-10||Math.abs(dot(q,n)/length)>.002)return false;
        double aa=dot(a,a),ab=dot(a,b),bb=dot(b,b),qa=dot(q,a),qb=dot(q,b),den=aa*bb-ab*ab;if(Math.abs(den)<1e-16)return false;
        double u=(bb*qa-ab*qb)/den,v=(aa*qb-ab*qa)/den;return u>=-1e-6&&v>=-1e-6&&u+v<=1+1e-6;
    }
    private static boolean inside(V p,List<Tri> solid){
        double angle=0;
        for(var t:solid){
            if(onSurface(p,t))return false;
            var a=sub(t.a,p);var b=sub(t.b,p);var c=sub(t.c,p);double la=Math.sqrt(dot(a,a)),lb=Math.sqrt(dot(b,b)),lc=Math.sqrt(dot(c,c));
            angle+=2*Math.atan2(dot(a,cross(b,c)),la*lb*lc+dot(a,b)*lc+dot(b,c)*la+dot(c,a)*lb);
        }return Math.abs(angle)>2*Math.PI;
    }
    private static Set<Set<Weld>> geometryKey(List<Tri> mesh){
        var key=new HashSet<Set<Weld>>();for(var t:mesh){var triangle=new HashSet<Weld>();triangle.add(weld(t.a));triangle.add(weld(t.b));triangle.add(weld(t.c));key.add(triangle);}return key;
    }
    private static boolean contained(List<Tri> child,List<List<Tri>> solids){
        for(var t:child){var p=new V((t.a.x+t.b.x+t.c.x)/3,(t.a.y+t.b.y+t.c.y)/3,(t.a.z+t.b.z+t.c.z)/3);for(var solid:solids)if(inside(p,solid))return true;}return false;
    }
    private static boolean near(CastleLevelEdits.Prop a,CastleLevelEdits.Prop b){
        var x=SceneRenderer.castleBounds(a.mesh());var y=SceneRenderer.castleBounds(b.mesh());
        double ar=Math.hypot(x[3],x[5])*a.scale()/2,br=Math.hypot(y[3],y[5])*b.scale()/2;
        return Math.hypot(a.x()-b.x(),a.z()-b.z())<ar+br&&a.y()<b.y()+y[4]*b.scale()-.002&&b.y()<a.y()+x[4]*a.scale()-.002;
    }
    private static List<Double> ceilingPlanes(List<CastleLevelEdits.Surface> surfaces,CastleLevelEdits.Lamp lamp){
        var planes=new ArrayList<Double>();for(var s:surfaces)if((s.kind()==CastleLevelEdits.Kind.FLOOR||s.kind()==CastleLevelEdits.Kind.ROOF)&&lamp.x()>=s.x0()&&lamp.x()<=s.x1()&&lamp.z()>=s.z0()&&lamp.z()<=s.z1()){
            double h=s.base()-(s.kind()==CastleLevelEdits.Kind.FLOOR?s.height():CastleRooms.SLAB);if(h>=lamp.y()-.002&&h-lamp.y()<1)planes.add(h);
        }return planes;
    }
    private static void validateRetained(CastleLevelEdits scene,CastleRebuildUpgrade.Plan plan,List<CastleHubArchitecturalPlan.Wall> priorWalls){
        var custom=new HashSet<String>();for(String item:plan.preserved())if(item.startsWith("prop:")&&!item.endsWith(" (deleted)"))custom.add(item.substring(5));
        var changedWalls=scene.walls().stream().filter(w->!priorWalls.contains(w)&&!w.hasDoor()).toList();var geometry=new HashMap<String,List<Tri>>();var solids=new HashMap<String,List<List<Tri>>>();
        var old=new HashMap<String,CastleLevelEdits.Prop>();for(var p:plan.before().props())old.put(p.id(),p);
        for(var p:scene.props())if(custom.contains(p.id())){
            var mesh=geometry.computeIfAbsent(p.id(),id->triangles(p));
            for(var t:mesh)for(var w:changedWalls)require(!crosses(t.a,t.b,w)&&!crosses(t.b,t.c,w)&&!crosses(t.c,t.a,w),"retained prop "+p.id()+" intersects rebuilt masonry");
            require(supported(scene,p,mesh,geometry),"retained prop "+p.id()+" lost its real support");
            for(var other:scene.props())if(!custom.contains(other.id())&&!other.equals(old.get(other.id()))&&near(p,other)){
                var otherMesh=geometry.computeIfAbsent(other.id(),id->triangles(other));
                require(!geometryKey(mesh).equals(geometryKey(otherMesh)),"retained prop "+p.id()+" duplicates new fitting "+other.id());
                for(var t:mesh)for(var u:otherMesh)require(!overlap(t,u),"retained prop "+p.id()+" intersects new fitting "+other.id());
                require(!contained(mesh,solids.computeIfAbsent(other.id(),id->closedSolids(otherMesh)))&&!contained(otherMesh,solids.computeIfAbsent(p.id(),id->closedSolids(mesh))),"retained prop "+p.id()+" is contained by new fitting "+other.id());
            }
        }
        for(var lamp:scene.lamps())if(plan.preserved().contains("lamp:"+lamp.id())){
            if(lamp.mount()==CastleHubDecor.Mount.WALL)require(CastleWallLampMount.resolve(lamp.fixture(),scene.walls()).supported(),"retained lamp "+lamp.id()+" lost its wall mount");
            else{
                var was=ceilingPlanes(plan.before().surfaces(),lamp);var now=ceilingPlanes(scene.surfaces(),lamp);
                require(!now.isEmpty()&&(was.isEmpty()||was.stream().anyMatch(a->now.stream().anyMatch(b->Math.abs(a-b)<.002))),"retained lamp "+lamp.id()+" lost its ceiling support");
            }
        }
    }
}
