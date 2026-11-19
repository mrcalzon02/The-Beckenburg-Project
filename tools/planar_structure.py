"""Merge clipped planar patches before adding closed structural thickness."""
import collections
import bpy,bmesh
from mathutils import Vector
from mathutils.geometry import delaunay_2d_cdt


def plate(name,polygons,top,depth,material):
    vertices=[];faces=[]
    for poly in polygons:
        signed=sum(p[0]*q[1]-q[0]*p[1] for p,q in zip(poly,poly[1:]+poly[:1]))
        if signed<0:poly=list(reversed(poly))
        start=len(vertices);vertices.extend(Vector(p) for p in poly);faces.append(tuple(range(start,len(vertices))))
    if not faces:return None
    points,edges,triangles,ov,oe,of=delaunay_2d_cdt(vertices,[],faces,1,.000001,True)
    triangles=[tri for tri,orig in zip(triangles,of) if orig]
    mesh=bpy.data.meshes.new(name+' planar union');mesh.from_pydata([(p.x,p.y,0) for p in points],[],triangles);mesh.update()
    bm=bmesh.new();bm.from_mesh(mesh)
    bmesh.ops.dissolve_limit(bm,angle_limit=.00001,use_dissolve_boundaries=False,verts=list(bm.verts),edges=list(bm.edges))
    bmesh.ops.triangulate(bm,faces=list(bm.faces))
    zero=[f for f in bm.faces if f.calc_area()<1e-12]
    if zero:bmesh.ops.delete(bm,geom=zero,context='FACES_ONLY')
    bm.to_mesh(mesh);bm.free();mesh.update()
    xy=[(v.co.x,v.co.y) for v in mesh.vertices];fs=[tuple(p.vertices) for p in mesh.polygons];boundary=collections.Counter()
    for face in fs:
        for a,b in zip(face,face[1:]+face[:1]):boundary[tuple(sorted((a,b)))]+=1
    assert all(n in [1,2] for n in boundary.values()),'Planar union is not manifold'
    # Regions meeting only at a point cannot share a vertical edge in a
    # manifold solid. Extrude edge-connected planar regions independently.
    edge_faces=collections.defaultdict(list)
    for i,face in enumerate(fs):
        for a,b in zip(face,face[1:]+face[:1]):edge_faces[tuple(sorted((a,b)))].append(i)
    adjacency=collections.defaultdict(set)
    for neighbors in edge_faces.values():
        for i in neighbors:adjacency[i].update(set(neighbors)-{i})
    unseen=set(range(len(fs)));components=[]
    while unseen:
        seed=unseen.pop();component={seed};stack=[seed]
        while stack:
            for other in adjacency[stack.pop()]&unseen:unseen.remove(other);component.add(other);stack.append(other)
        components.append(component)
    objects=[]
    for component in components:
        original_ids=sorted({v for i in component for v in fs[i]});mapping={old:i for i,old in enumerate(original_ids)};localxy=[xy[i] for i in original_ids];localfaces=[tuple(mapping[v] for v in fs[i]) for i in component];bounds=collections.Counter()
        for face in localfaces:
            for a,b in zip(face,face[1:]+face[:1]):bounds[tuple(sorted((a,b)))]+=1
        n=len(localxy);xyz=[(x,-z,y) for y in [top-depth,top] for x,z in localxy]
        solid_faces=[face for face in localfaces]+[tuple(i+n for i in reversed(face)) for face in localfaces]+[(a,b,b+n,a+n) for (a,b),count in bounds.items() if count==1]
        solid=bpy.data.meshes.new(name);solid.from_pydata(xyz,[],solid_faces);solid.update();bm=bmesh.new();bm.from_mesh(solid);bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces))
        assert all(e.is_manifold for e in bm.edges) and bm.calc_volume(signed=True)>0,('Merged plate needs closed outward solids',name,len(component))
        bm.to_mesh(solid);bm.free()
        obj=bpy.data.objects.new(name+' region'+str(len(objects)),solid);bpy.context.scene.collection.objects.link(obj);solid.materials.append(material);uv=solid.uv_layers.new(name='Metre structural UV')
        for face in solid.polygons:
            for li in face.loop_indices:
                p=solid.vertices[solid.loops[li].vertex_index].co;uv.data[li].uv=(p.x,p.y) if abs(face.normal.z)>.5 else (p.x+p.y,p.z)
        objects.append(obj)
    bpy.data.meshes.remove(mesh)
    return objects
