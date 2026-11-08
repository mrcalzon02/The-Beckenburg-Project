"""Measured connections to unchanged shipped boiler terminals; original steel/red/blue maps."""
from pathlib import Path
import bpy,bmesh,math,json,hashlib
from mathutils import Vector
from mathutils.geometry import intersect_ray_tri
ROOT=Path(__file__).resolve().parents[2];OUT=ROOT/'development/assets/castle-services';OUT.mkdir(parents=True,exist_ok=True)
SOURCE=ROOT/'assets/runtime/models/level_boiler_modern.glb';digest=hashlib.sha256(SOURCE.read_bytes()).hexdigest()
bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(SOURCE))
original=next(o for o in bpy.context.scene.objects if o.type=='MESH')
# Repair only the independently demonstrated loose source components. Keep the
# library original untouched and preserve each vertex's UV/material assignment.
data=original.data;bm=bmesh.new();bm.from_mesh(data);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=1e-6);bm.to_mesh(data);bm.free();data.calc_loop_triangles()
links=[set() for v in data.vertices]
for e in data.edges:
 a,b=e.vertices;links[a].add(b);links[b].add(a)
remaining=set(range(len(links)));groups=[]
while remaining:
 first=remaining.pop();group={first};queue=[first]
 while queue:
  for j in links[queue.pop()]:
   if j in remaining:remaining.remove(j);group.add(j);queue.append(j)
 groups.append(group)
failed=json.loads((OUT/'rejected-original-boiler-contact.json').read_text(encoding='utf-8'))
bad=[c['bounds'] for c in failed['components'] if not c['supported']];repairs=[]
for group in groups:
 lo=[min(data.vertices[i].co[k] for i in group) for k in range(3)];hi=[max(data.vertices[i].co[k] for i in group) for k in range(3)]
 if not any(max(abs(lo[k]-b[0][k])+abs(hi[k]-b[1][k]) for k in range(3))<1e-5 for b in bad):continue
 origin=Vector(((lo[0]+hi[0])/2,-2,(lo[2]+hi[2])/2));hits=[]
 for tri in data.loop_triangles:
  if any(i in group for i in tri.vertices):continue
  a,b,c=[data.vertices[i].co for i in tri.vertices];hit=intersect_ray_tri(a,b,c,Vector((0,1,0)),origin,True)
  if hit is not None:hits.append(hit.y)
 assert hits,'No actual supporting source triangle'
 delta=min(hits)+.001-hi[1];assert 0<delta<.06,(lo,hi,delta)
 for i in group:data.vertices[i].co.y+=delta
 repairs.append(dict(old_bounds=[lo,hi],translation_y=delta,support_y=min(hits)))
assert len(repairs)==len(bad),(len(repairs),len(bad))
for m in original.data.materials:
 for n in m.node_tree.nodes:
  if n.type=='TEX_IMAGE' and not n.image.packed_file:n.image.pack()
bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'plant_boiler.blend'))
bpy.ops.export_scene.gltf(filepath=str(OUT/'level_castle_plant_boiler.glb'),export_format='GLB',export_yup=True)
(OUT/'boiler-attachment-repairs.json').write_text(json.dumps(dict(status='REPAIRED_VARIANT_PENDING_EXPORT_CONTACT_AUDIT',original_sha256=digest,repairs=repairs,criterion='Each loose component translated only along depth to actual supporting source triangles plus1mm intersection; source library unchanged'),indent=2)+'\n',encoding='utf-8')
materials={m.name:m for m in original.data.materials}
def tube(name,a,b,radius,material):
 a,b=Vector(a),Vector(b);axis=(b-a).normalized();u=axis.cross(Vector((1,0,0))).normalized();v=axis.cross(u).normalized()
 verts=[tuple(p+radius*(u*math.cos(i*math.pi/4)+v*math.sin(i*math.pi/4))) for p in [a,b] for i in range(8)]
 faces=[tuple(range(7,-1,-1)),tuple(range(8,16))]+[(i,(i+1)%8,(i+1)%8+8,i+8) for i in range(8)]
 mesh=bpy.data.meshes.new(name);mesh.from_pydata(verts,[],faces);mesh.update();mesh.uv_layers.new();mesh.materials.append(material)
 for poly in mesh.polygons:
  for k,loop in enumerate(poly.loop_indices):mesh.uv_layers.active.data[loop].uv=[(.1,.1),(.9,.1),(.9,.9),(.1,.9)][k%4]
 obj=bpy.data.objects.new(name,mesh);bpy.context.collection.objects.link(obj);return obj
parts=[tube('Hot return sleeve meets exact source terminal and east masonry',(-.29,.489,2.30),(-.29,.525,2.30),.047,materials['Rebuild Original red']),
       tube('Cold feed sleeve meets exact source terminal and east masonry',(.29,.489,2.30),(.29,.525,2.30),.047,materials['Rebuild Original blue']),
       tube('Sealed flue riser from source collar to actual ceiling underside',(0,.17,2.079),(0,.17,2.78),.14,materials['Rebuild Original steel'])]
original.select_set(False)
for obj in parts:obj.select_set(True)
bpy.context.view_layer.objects.active=parts[0];bpy.ops.object.join();obj=bpy.context.object
bm=bmesh.new();bm.from_mesh(obj.data);bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces));bm.to_mesh(obj.data);bm.free()
bpy.data.objects.remove(original,do_unlink=True)
for m in obj.data.materials:
 for node in m.node_tree.nodes:
  if node.type=='TEX_IMAGE':
   image=node.image
   if not image.packed_file:image.pack()
   assert image.packed_file
obj.data.calc_loop_triangles();assert len(obj.data.loop_triangles)==84
bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'plant_connections.blend'))
path=OUT/'level_castle_plant_connections.glb';bpy.ops.export_scene.gltf(filepath=str(path),export_format='GLB',export_yup=True)
assert hashlib.sha256(SOURCE.read_bytes()).hexdigest()==digest
r=dict(status='EXPORTED_MEASURED_CONNECTIONS_NATIVE_REVIEW_PENDING',triangles=84,unchanged_boiler_sha256=digest,connections_sha256=hashlib.sha256(path.read_bytes()).hexdigest(),source_axes='Blender X,Y,Z; shipped boiler base0, back terminalY=.49, rear wallY=.525, ceiling undersideZ=2.78',terminals=[dict(point=[-.29,.49,2.30],target=[-.29,.525,2.30]),dict(point=[.29,.49,2.30],target=[.29,.525,2.30]),dict(point=[0,.17,2.08],target=[0,.17,2.78])],scope='Original GLB, pivots, UVs and textures unchanged. Separate three measured closed sleeves. No new heating simulation or electrical breaker logic.')
(OUT/'plant-connections-review.json').write_text(json.dumps(r,indent=2)+'\n',encoding='utf-8')
print('PASS84-triangle measured hot/cold wall sleeves and flue; original boiler unchanged')
