"""26 authored faces: recessed Castle fireplace, surface detail in one original atlas.

Met46.20a,b informs broad moulded surround, backed firebox and borne overmantel.
No museum photograph is used as a texture. Owner budget:20-30 authored faces.
"""
from pathlib import Path
import bpy,bmesh,math,json,hashlib,importlib.util
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[2];OUT=ROOT/'development/assets/castle-museum'
PRE=OUT/'previews/fireplace-rebuild';PRE.mkdir(parents=True,exist_ok=True)
bpy.ops.wm.read_factory_settings(use_empty=True)
# The owner requested image-generation rather than procedural/PIL artwork.
# Preserve the generated master and pack a512px game-budget derivative.
texture_path=OUT/'textures/fireplace-imagegen-master.png'
image=bpy.data.images.load(str(texture_path));image.name='Castle fireplace / generated original limestone atlas'
image.scale(512,512);image.filepath_raw=str(OUT/'textures/fireplace-runtime-atlas.png');image.file_format='PNG';image.save();image.pack()
mat=bpy.data.materials.new('Limestone / repaired ashlar / soot / textured iron fireback');mat.use_nodes=True
shader=mat.node_tree.nodes.get('Principled BSDF');shader.inputs['Roughness'].default_value=.92
tex=mat.node_tree.nodes.new('ShaderNodeTexImage');tex.image=image;tex.interpolation='Closest'
mat.node_tree.links.new(tex.outputs['Color'],shader.inputs['Base Color'])
verts=[];faces=[];uvs=[]
def face(points,tile=0,uv=None):
 start=len(verts);verts.extend(points);faces.append(tuple(range(start,start+len(points))))
 if uv is None:
  normal=(Vector(points[1])-Vector(points[0])).cross(Vector(points[2])-Vector(points[0]));axis=max(range(3),key=lambda a:abs(normal[a]));axes=[a for a in range(3) if a!=axis]
  lo=[min(p[a] for p in points) for a in axes];hi=[max(p[a] for p in points) for a in axes]
  uv=[tuple((p[a]-lo[i])/max(1e-6,hi[i]-lo[i]) for i,a in enumerate(axes)) for p in points]
 if tile==3:
  span_u=(hi[0]-lo[0])/2.8;span_v=(hi[1]-lo[1])/2.85
  uv=[(u*span_u,v*span_v) for u,v in uv]
 uvs.extend([((tile%2+.015+u*.97)/2,(tile//2+.015+v*.97)/2) for u,v in uv])
def box(cx,cy,cz,sx,sy,sz,tile):
 p=[(cx+a*sx/2,cy+b*sy/2,cz+c*sz/2) for a,b,c in [(-1,-1,-1),(1,-1,-1),(1,1,-1),(-1,1,-1),(-1,-1,1),(1,-1,1),(1,1,1),(-1,1,1)]]
 for indices in [(0,3,2,1),(4,5,6,7),(0,1,5,4),(1,2,6,5),(2,3,7,6),(3,0,4,7)]:face([p[i] for i in indices],tile)
def front(points,tile=0):
 face([(x,-.35,z) for x,z in points],tile,[((x+1.4)/2.8,(z-.14)/1.81) for x,z in points] if tile==0 else None)
box(0,-.10,.07,3.12,1.14,.14,3) # six hearth faces; real floor support
front([(-1.4,.14),(-1,.14),(-1,1.95),(-1.4,1.95)])
front([(1,.14),(1.4,.14),(1.4,1.95),(1,1.95)])
front([(-1,1.48),(-.76,1.74),(.76,1.74),(1,1.48),(1,1.95),(-1,1.95)])
front([(-1.4,1.95),(1.4,1.95),(1.4,2.85),(-1.4,2.85)],1)
front_path=[(-1,1.48),(-.76,1.74),(.76,1.74),(1,1.48)]
back_path=[(-.85,1.48),(-.65,1.74),(.65,1.74),(.85,1.48)]
for i in range(3):
 a,b=front_path[i:i+2];c,d=back_path[i:i+2]
 face([(a[0],-.35,a[1]),(b[0],-.35,b[1]),(d[0],.28,d[1]),(c[0],.28,c[1])],2,[(.02,.75),(.12,.75),(.12,.98),(.02,.98)])
for sign in [-1,1]:
 face([(sign,-.35,.14),(sign*.85,.28,.14),(sign*.85,.28,1.48),(sign,-.35,1.48)],2,[(.02,0),(.12,0),(.12,1),(.02,1)])
face([(-.85,.28,.14),(.85,.28,.14),(.85,.28,1.48),(.65,.28,1.74),(-.65,.28,1.74),(-.85,.28,1.48)],2)
for sign in [-1,1]:face([(sign*1.4,-.35,.14),(sign*1.4,.35,.14),(sign*1.4,.35,2.85),(sign*1.4,-.35,2.85)],3)
face([(-1.4,.35,.14),(1.4,.35,.14),(1.4,.35,2.85),(-1.4,.35,2.85)],3)
face([(-1.4,-.35,2.85),(1.4,-.35,2.85),(1.4,.35,2.85),(-1.4,.35,2.85)],3)
box(0,-.05,1.99,2.96,.84,.10,3) # borne projecting mantel: six faces
assert len(faces)==26,len(faces)
mesh=bpy.data.meshes.new('26-face recessed limestone fireplace');mesh.from_pydata(verts,[],faces);mesh.uv_layers.new();mesh.materials.append(mat)
for poly in mesh.polygons:
 for loop in poly.loop_indices:mesh.uv_layers.active.data[loop].uv=uvs[mesh.loops[loop].vertex_index]
obj=bpy.data.objects.new('Castle fireplace / real backed opening and supported mantel',mesh);bpy.context.collection.objects.link(obj)
bpy.context.view_layer.objects.active=obj;obj.select_set(True)
bm=bmesh.new();bm.from_mesh(mesh);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=.000001);bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces));bm.to_mesh(mesh);bm.free();mesh.calc_loop_triangles()
spec=importlib.util.spec_from_file_location('contact',Path(__file__).with_name('castle_armour_lowpoly.py'));contact=importlib.util.module_from_spec(spec);spec.loader.exec_module(contact)
contact.audit_contacts(mesh,'fireplace-source')
assert len(mesh.polygons)==26
bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'great_fireplace.blend'))
path=OUT/'exports/level_castle_great_fireplace.glb';bpy.ops.export_scene.gltf(filepath=str(path),export_format='GLB',export_yup=True)
bpy.ops.object.select_all(action='SELECT');bpy.ops.object.delete(use_global=False);bpy.ops.import_scene.gltf(filepath=str(path))
obj=next(o for o in bpy.context.scene.objects if o.type=='MESH');copy=obj.data.copy()
bm=bmesh.new();bm.from_mesh(copy);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=.000001);bm.to_mesh(copy);bm.free();copy.calc_loop_triangles()
report=contact.audit_contacts(copy,'fireplace-export',hashlib.sha256(path.read_bytes()).hexdigest());report.update(authored_faces=26,status='GEOMETRY_VERIFIED_RENDER_REVIEW_PENDING',dimensions_metres=list(obj.dimensions),reference='https://www.metmuseum.org/art/collection/search/471241',scope='Original26-face restored castle fireplace; real backed/splayed opening, hearth, borne mantel; ornament/masonry/iron/soot in one512px atlas. Cold retained fitting, no live fire or working flue claimed.')
(OUT/'fireplace-review.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
scene=bpy.context.scene;scene.render.engine='CYCLES';scene.cycles.samples=20;scene.render.resolution_x=900;scene.render.resolution_y=900;scene.render.resolution_percentage=100
scene.world=bpy.data.worlds.new('Fireplace review world');scene.world.color=(.16,.16,.16)
bpy.ops.mesh.primitive_plane_add(size=200,location=(0,0,-.005))
for pos,power,size in [((1,-4,5),650,4),((-3,2,4),400,3)]:
 bpy.ops.object.light_add(type='AREA',location=pos);bpy.context.object.data.energy=power;bpy.context.object.data.size=size
bpy.ops.object.camera_add();cam=bpy.context.object;cam.data.type='ORTHO';cam.data.ortho_scale=3.9;scene.camera=cam
for name,pos in [('front',(0,-5,2.0)),('quarter',(3,-5,3)),('side',(5,-1,2.2)),('rear',(3,5,3))]:
 cam.location=pos;cam.rotation_euler=(Vector((0,0,1.4))-cam.location).to_track_quat('-Z','Y').to_euler();scene.render.filepath=str(PRE/(name+'.png'));bpy.ops.render.render(write_still=True)
print('PASS fireplace26 authored faces;',report['triangle_count'],'exported triangles; actual source/export support; four reimported views require review')
