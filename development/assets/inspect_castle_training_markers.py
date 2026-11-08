"""Independently inspect actual native floor-inlay exports and packed editable sources."""
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
 r['criterion']='Every actual floor-inlay component bears directly on floorZ=0 within1mm; raised actual ring must fail';r['expected']=expected
 (OUT/(phase+'.json')).write_text(json.dumps(r,indent=2)+'\n',encoding='utf-8');temporary.unlink();assert r['status']==expected
 return r
records=[]
for i,rite in enumerate(['banishment','cleansing','absolution']):
 name='level_castle_training_'+rite;path=ROOT/'assets/runtime/models'/(name+'.glb');digest=hashlib.sha256(path.read_bytes()).hexdigest();assert path.read_bytes()==(OUT/(name+'.glb')).read_bytes()
 bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(path));obj=next(o for o in bpy.context.scene.objects if o.type=='MESH');m=obj.data.copy()
 bm=bmesh.new();bm.from_mesh(m);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=1e-6);bm.to_mesh(m);bm.free();m.calc_loop_triangles()
 assert len(m.loop_triangles)==196+12*i and min(v.co.z for v in m.vertices)>=-1e-6 and max(v.co.z for v in m.vertices)<.009
 r=audit(m,name+'-floor-export','PASS',digest);negative=m.copy();moved=0
 for v in negative.vertices:
  if v.co.x*v.co.x+v.co.y*v.co.y>.69**2:v.co.z+=.15;moved+=1
 assert moved==80,moved
 negative.calc_loop_triangles();audit(negative,name+'-raised-ring-negative','FAIL',digest)
 source=OUT/(name+'.blend');before=hashlib.sha256(source.read_bytes()).hexdigest();bpy.ops.wm.open_mainfile(filepath=str(source));images=[]
 for obj in bpy.context.scene.objects:
  if obj.type!='MESH':continue
  for material in obj.data.materials:
   for node in material.node_tree.nodes:
    if node.type=='TEX_IMAGE':
     image=node.image;assert image and image.packed_file and len(image.pixels[:4])==4 and image.has_data;images.append(dict(name=image.name,size=list(image.size)))
 assert images and hashlib.sha256(source.read_bytes()).hexdigest()==before and hashlib.sha256(path.read_bytes()).hexdigest()==digest
 records.append(dict(id=name,triangles=r['triangle_count'],floor_components=len(r['components']),runtime_sha256=digest,source_sha256=before,packed_images=images))
(OUT/'training-marker-independent-review.json').write_text(json.dumps(dict(status='PASS',models=records,scope='Actual GLB floor contact/raised-ring controls/fresh packed sources; native visual and gameplay acceptance separate'),indent=2)+'\n',encoding='utf-8')
print('PASS3 actual floor-inlay GLBs196/208/220tri, raised-ring negatives, fresh packed sources and unchanged exports')
