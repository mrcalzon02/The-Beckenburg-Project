"""Actual original boiler + supplemental GLB contact, broken-terminal negative, source reopen."""
from pathlib import Path
import bpy,bmesh,json,hashlib,importlib.util
ROOT=Path(__file__).resolve().parents[2];OUT=ROOT/'development/assets/castle-services'
source=OUT/'level_castle_plant_boiler.glb';connections=OUT/'level_castle_plant_connections.glb'
digests={p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in [source,connections]}
bpy.ops.wm.read_factory_settings(use_empty=True)
for path in [source,connections]:bpy.ops.import_scene.gltf(filepath=str(path))
parts=[o for o in bpy.context.scene.objects if o.type=='MESH']
bpy.ops.object.select_all(action='DESELECT')
for o in parts:o.select_set(True)
bpy.context.view_layer.objects.active=parts[0];bpy.ops.object.join();data=bpy.context.object.data.copy()
bm=bmesh.new();bm.from_mesh(data);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=1e-6);bm.to_mesh(data);bm.free();data.calc_loop_triangles()
spec=importlib.util.spec_from_file_location('contact',Path(__file__).with_name('castle_armour_lowpoly.py'));contact=importlib.util.module_from_spec(spec);spec.loader.exec_module(contact)
def audit(mesh,phase,expected):
 temporary=ROOT/'development/assets/castle-museum'/('armour-contact-audit-'+phase+'.json')
 try:r=contact.audit_contacts(mesh,phase,digests[connections.name])
 except RuntimeError:
  if expected!='FAIL':raise
  r=json.loads(temporary.read_text(encoding='utf-8'))
 r['criterion']='Actual source+connections triangle contact within1mm from boiler floor plinth; no bounding-box/shared-name substitute'
 r['expected']=expected;(OUT/(phase+'.json')).write_text(json.dumps(r,indent=2)+'\n',encoding='utf-8')
 temporary.unlink();assert r['status']==expected
 return r
audit(data,'plant-connected-export','PASS')
negative=data.copy();moved=0
for v in negative.vertices:
 p=v.co
 if -.34<p.x<-.24 and p.y>.4885 and 2.25<p.z<2.35:p.x+=.25;moved+=1
assert moved>=16,'Actual sleeve geometry was not selected'
negative.calc_loop_triangles();audit(negative,'plant-broken-feed-negative','FAIL')
from mathutils import Vector
pre=OUT/'previews/plant-assembly';pre.mkdir(parents=True,exist_ok=True)
scene=bpy.context.scene;scene.render.engine='CYCLES';scene.cycles.samples=16
scene.render.resolution_x=800;scene.render.resolution_y=1000;scene.render.resolution_percentage=100
scene.world=bpy.data.worlds.new('Neutral plant assembly inspection');scene.world.color=(.2,.2,.2)
for pos,power in [((2,-4,5),500),((-3,-1,3),250)]:
 bpy.ops.object.light_add(type='AREA',location=pos);bpy.context.object.data.energy=power;bpy.context.object.data.size=3
bpy.ops.object.camera_add();cam=bpy.context.object;cam.data.type='ORTHO';cam.data.ortho_scale=3.15;scene.camera=cam
for name,pos in [('front',(0,-4,1.8)),('quarter',(2,-4,2.5)),('rear',(0,4,1.8)),('side',(4,-.1,1.8))]:
 cam.location=pos;cam.rotation_euler=(Vector((0,0,1.4))-cam.location).to_track_quat('-Z','Y').to_euler();scene.render.filepath=str(pre/(name+'.png'));bpy.ops.render.render(write_still=True)
clay=bpy.data.materials.new('Neutral clay contact inspection');clay.diffuse_color=(.42,.42,.42,1);scene.view_layers[0].material_override=clay
cam.location=(2,-4,2.5);cam.rotation_euler=(Vector((0,0,1.4))-cam.location).to_track_quat('-Z','Y').to_euler();scene.render.filepath=str(pre/'quarter-clay.png');bpy.ops.render.render(write_still=True)
sources=[]
for name in ['plant_connections','plant_boiler']:
 path=OUT/(name+'.blend');before=hashlib.sha256(path.read_bytes()).hexdigest();bpy.ops.wm.open_mainfile(filepath=str(path));images=[]
 for obj in bpy.context.scene.objects:
  if obj.type!='MESH':continue
  for material in obj.data.materials:
   for node in material.node_tree.nodes:
    if node.type=='TEX_IMAGE':
     image=node.image;assert image and image.packed_file and len(image.pixels[:4])==4 and image.has_data;images.append(image.name)
 assert images and hashlib.sha256(path.read_bytes()).hexdigest()==before
 sources.append(dict(source=name+'.blend',sha256=before,images=images))
for p in [source,connections]:assert hashlib.sha256(p.read_bytes()).hexdigest()==digests[p.name]
(OUT/'plant-packed-source.json').write_text(json.dumps(dict(status='PASS',sources=sources,unchanged_exports=digests),indent=2)+'\n',encoding='utf-8')
print('PASS repaired boiler variant + supplemental export contact; moved feed fails; both packed sources freshly reopen; GLBs unchanged by inspection')
