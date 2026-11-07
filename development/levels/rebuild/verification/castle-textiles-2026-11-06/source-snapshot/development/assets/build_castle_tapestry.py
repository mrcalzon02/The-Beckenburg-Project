"""Original160-triangle Castle tapestry with real wall mounts and generated weave.

Place this builder at development/assets/build_castle_tapestry.py in the target
checkout. Museum reference informs material/hierarchy, never supplies artwork.
"""
from pathlib import Path
import bpy,bmesh,math,json,hashlib,importlib.util
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[2];OUT=ROOT/'development/assets/castle-museum'
PRE=OUT/'previews/tapestry-rebuild';PRE.mkdir(parents=True,exist_ok=True)
bpy.ops.wm.read_factory_settings(use_empty=True)
image=bpy.data.images.load(str(OUT/'textures/tapestry-imagegen-master.png'))
image.name='Original Castle and key / woven wall hanging';image.scale(512,768)
image.filepath_raw=str(OUT/'textures/tapestry-runtime.png');image.file_format='PNG';image.save();image.pack()
metal_image=bpy.data.images.load(str(OUT/'textures/armour-runtime-atlas.png'));metal_image.pack()
def material(name,img):
 m=bpy.data.materials.new(name);m.use_nodes=True
 t=m.node_tree.nodes.new('ShaderNodeTexImage');t.image=img;t.interpolation='Closest'
 s=m.node_tree.nodes.get('Principled BSDF');s.inputs['Roughness'].default_value=.9
 m.node_tree.links.new(t.outputs['Color'],s.inputs['Base Color']);return m
cloth=material('Original generated woven tapestry',image)
iron=material('Reused authored forged steel / support hardware',metal_image)
def mesh(name,verts,faces,mat,uv=None):
 m=bpy.data.meshes.new(name);m.from_pydata(verts,[],faces);m.update();m.uv_layers.new();m.materials.append(mat)
 o=bpy.data.objects.new(name,m);bpy.context.collection.objects.link(o)
 for poly in m.polygons:
  for loop in poly.loop_indices:
   i=m.loops[loop].vertex_index
   u,v=uv[i] if uv else ((i%4)/3,((i//4)%4)/3)
   if mat==iron:u,v=.02+u*.20,.02+v*.25
   m.uv_layers.active.data[loop].uv=u,v
 return o
def box(name,centre,size,mat):
 verts=[tuple(centre[a]+p[a]*size[a]/2 for a in range(3)) for p in [(-1,-1,-1),(1,-1,-1),(1,1,-1),(-1,1,-1),(-1,-1,1),(1,-1,1),(1,1,1),(-1,1,1)]]
 return mesh(name,verts,[(0,3,2,1),(4,5,6,7),(0,1,5,4),(1,2,6,5),(2,3,7,6),(3,0,4,7)],mat,[(.005+.99*((v[0]-centre[0])/size[0]+.5),.97+.025*((v[2]-centre[2])/size[2]+.5)) for v in verts] if mat==cloth else None)
# A closed cloth shell, six shallow folds across a single contiguous image.
n=6;verts=[];uv=[]
for side in [0,.006]:
 for row,z in enumerate([.12,1.18,2.24]):
  for i in range(n+1):
   u=i/n;offset=0 if row==2 else .018*math.cos(u*math.pi*6)
   verts.append(((u-.5)*1.4,-.08+offset+side,z));uv.append((u,(z-.12)/2.12))
size=3*(n+1);faces=[]
for side in [0,size]:
 for row in range(2):
  for col in range(n):
   a=side+row*(n+1)+col;faces.append((a,a+1,a+n+2,a+n+1))
border=list(range(n+1))+[2*n+1,3*n+2]+list(range(3*n+1,2*n+1,-1))+[n+1]
assert len(border)==2*n+4
for a,b in zip(border,border[1:]+border[:1]):faces.append((a,b,b+size,a+size))
obj=mesh('Continuous reinforced hanging with shallow folds',verts,faces,cloth,uv)
# Full-width sleeve is structural, not unrelated tabs floating above the cloth.
box('Reinforced sewn hanging sleeve',(0,-.08,2.255),(1.40,.048,.05),cloth)
for x in [-.70,.70]:
 box('Wall bearing plate',(x,-.006,2.25),(.06,.012,.16),iron)
 box('Rail bracket arm',(x,-.05,2.25),(.030,.10,.040),iron)
verts=[];uv=[]
for x in [-.82,.82]:
 for i in range(6):
  a=i*math.pi/3;verts.append((x,-.08+.025*math.cos(a),2.255+.025*math.sin(a)));uv.append((i/5,(x+.82)/1.64))
faces=[tuple(range(5,-1,-1)),tuple(range(6,12))]+[(i,(i+1)%6,(i+1)%6+6,i+6) for i in range(6)]
mesh('Continuous six-sided support rail',verts,faces,iron,uv)
bpy.ops.object.select_all(action='SELECT');bpy.context.view_layer.objects.active=obj;bpy.ops.object.join()
obj=bpy.context.object;bpy.ops.object.transform_apply(location=True,rotation=True,scale=True)
bm=bmesh.new();bm.from_mesh(obj.data);bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces));bm.to_mesh(obj.data);bm.free();obj.data.calc_loop_triangles()
assert len(obj.data.loop_triangles)<=180,len(obj.data.loop_triangles)
spec=importlib.util.spec_from_file_location('contact',Path(__file__).with_name('castle_armour_lowpoly.py'));contact=importlib.util.module_from_spec(spec);spec.loader.exec_module(contact)
def mounted_audit(data,phase,digest=None):
 copy=data.copy();bm=bmesh.new();bm.from_mesh(copy);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=.000001);bm.to_mesh(copy);bm.free()
 assert max(v.co.y for v in copy.vertices)<.00001,'Hardware penetrates wall'
 # Rigid audit coordinate change: wall planeY=0 becomes support planeZ=0.
 # Distances and triangle contact are unchanged. No cloth bottom is a root.
 for v in copy.vertices:v.co=(v.co.x,v.co.z,-v.co.y)
 copy.calc_loop_triangles();r=contact.audit_contacts(copy,phase,digest)
 r['criterion']='Every component reaches actual wall bearing plates through triangle contact within1mm; rigid audit coordinates(x,z,-y), wallY=0 becomes supportZ=0'
 r['physical_mount_plane']='Blender localY=0; only rear wall-plate surfaces touch it'
 (OUT/('tapestry-contact-'+phase+'.json')).write_text(json.dumps(r,indent=2)+'\n',encoding='utf-8');return r
mounted_audit(obj.data,'tapestry-wall-source')
bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'tapestry.blend'))
path=OUT/'exports/level_castle_tapestry.glb';bpy.ops.export_scene.gltf(filepath=str(path),export_format='GLB',export_yup=True)
bpy.ops.object.select_all(action='SELECT');bpy.ops.object.delete(use_global=False);bpy.ops.import_scene.gltf(filepath=str(path))
obj=next(o for o in bpy.context.scene.objects if o.type=='MESH');report=mounted_audit(obj.data,'tapestry-wall-export',hashlib.sha256(path.read_bytes()).hexdigest())
report.update(status='GEOMETRY_VERIFIED_VISUAL_NATIVE_REVIEW_PENDING',dimensions_metres=list(obj.dimensions),reference='https://www.metmuseum.org/art/collection/search/467642',art='Original built-in image generation; no reference photograph used as texture')
(OUT/'tapestry-review.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
scene=bpy.context.scene;scene.render.engine='CYCLES';scene.cycles.samples=20
scene.render.resolution_x=800;scene.render.resolution_y=1000;scene.render.resolution_percentage=100
scene.world=bpy.data.worlds.new('Tapestry review world');scene.world.color=(.2,.2,.2)
for pos,power in [((1,-3,4),500),((-2,-1,3),250)]:
 bpy.ops.object.light_add(type='AREA',location=pos);bpy.context.object.data.energy=power;bpy.context.object.data.size=3
bpy.ops.object.camera_add();cam=bpy.context.object;cam.data.type='ORTHO';cam.data.ortho_scale=2.8;scene.camera=cam
for name,pos in [('front',(0,-4,1.6)),('quarter',(2,-4,2.1)),('side',(4,-.5,1.7)),('rear',(0,4,1.7))]:
 cam.location=pos;cam.rotation_euler=(Vector((0,-.04,1.25))-cam.location).to_track_quat('-Z','Y').to_euler();scene.render.filepath=str(PRE/(name+'.png'));bpy.ops.render.render(write_still=True)
print('PASS mounted tapestry',report['triangle_count'],'triangles; source/export supports reach real wall plates; four actual exported views await review')
