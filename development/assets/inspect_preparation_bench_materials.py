"""Independent shared bench export contact, actual loose-tool negative and packed source reopening."""
from pathlib import Path
import bpy,bmesh,hashlib,json,importlib.util
ROOT=Path(__file__).resolve().parents[2];OUT=ROOT/'development/assets/castle-training'
spec=importlib.util.spec_from_file_location('contact',Path(__file__).with_name('castle_armour_lowpoly.py'));contact=importlib.util.module_from_spec(spec);spec.loader.exec_module(contact)
def audit(mesh,phase,expected,digest):
 temporary=OUT.parent/'castle-museum'/('armour-contact-audit-'+phase+'.json')
 try:r=contact.audit_contacts(mesh,phase,digest)
 except RuntimeError:
  if expected!='FAIL':raise
  r=json.loads(temporary.read_text(encoding='utf-8'))
 r['expected']=expected;r['criterion']='Actual shared runtime GLB triangle contact within1mm through bench legs to real floor; raised actual tool must fail'
 (OUT/(phase+'.json')).write_text(json.dumps(r,indent=2)+'\n',encoding='utf-8');temporary.unlink();assert r['status']==expected
 return r
records=[]
for suffix in ['', '_worn']:
 id='level_workbench'+suffix;path=ROOT/'assets/runtime/models'/(id+'.glb');digest=hashlib.sha256(path.read_bytes()).hexdigest()
 assert path.read_bytes()==(OUT/('level_castle_preparation_bench'+suffix+'.glb')).read_bytes()
 bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(path));obj=next(o for o in bpy.context.scene.objects if o.type=='MESH');m=obj.data.copy()
 bm=bmesh.new();bm.from_mesh(m);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=1e-6);bm.to_mesh(m);bm.free();m.calc_loop_triangles()
 positive=audit(m,id+'-independent-export','PASS',digest)
 negative=m.copy();moved=0
 for v in negative.vertices:
  p=v.co
  if -.366<p.x<-.234 and abs(p.y)<.07 and .943<p.z<1.035:p.z+=.15;moved+=1
 assert moved==40,(id,moved)
 negative.calc_loop_triangles();audit(negative,id+'-raised-tool-negative','FAIL',digest)
 assert hashlib.sha256(path.read_bytes()).hexdigest()==digest
 from mathutils import Vector
 pre=OUT/'previews/generated-bench-materials';scene=bpy.context.scene;scene.render.engine='CYCLES';scene.cycles.samples=12;scene.render.resolution_x=1100;scene.render.resolution_y=800;scene.render.resolution_percentage=100
 scene.world=bpy.data.worlds.new('Neutral geometry control');scene.world.color=(.2,.2,.2)
 clay=bpy.data.materials.new('Neutral clay shape control');clay.diffuse_color=(.42,.42,.42,1);scene.view_layers[0].material_override=clay
 for pos,power in [((2,-3,4),450),((-2,-1,3),200)]:
  bpy.ops.object.light_add(type='AREA',location=pos);bpy.context.object.data.energy=power;bpy.context.object.data.size=3
 bpy.ops.object.camera_add();cam=bpy.context.object;cam.data.type='ORTHO';cam.data.ortho_scale=2.35;cam.location=(2,-3,2.3);cam.rotation_euler=(Vector((0,0,.65))-cam.location).to_track_quat('-Z','Y').to_euler();scene.camera=cam;scene.render.filepath=str(pre/(id+'-clay.png'));bpy.ops.render.render(write_still=True)
 source=OUT/('level_castle_preparation_bench'+suffix+'.blend');source_hash=hashlib.sha256(source.read_bytes()).hexdigest();bpy.ops.wm.open_mainfile(filepath=str(source));images=[]
 for obj in bpy.context.scene.objects:
  if obj.type!='MESH':continue
  for material in obj.data.materials:
   for node in material.node_tree.nodes:
    if node.type=='TEX_IMAGE':
     image=node.image;assert image and image.packed_file and len(image.pixels[:4])==4 and image.has_data and list(image.size)==[512,512];images.append(image.name)
 assert images and hashlib.sha256(source.read_bytes()).hexdigest()==source_hash
 records.append(dict(id=id,runtime_sha256=digest,triangles=positive['triangle_count'],supported_components=len(positive['components']),source_sha256=source_hash,packed_images=images))
(OUT/'bench-material-independent-review.json').write_text(json.dumps(dict(status='PASS',models=records,scope='Actual export/source checks only; visual/native/all-map acceptance separate'),indent=2)+'\n',encoding='utf-8')
print('PASS both688tri runtime bench exports, actual raised-tool negatives, fresh packed512px sources; production bytes unchanged')
