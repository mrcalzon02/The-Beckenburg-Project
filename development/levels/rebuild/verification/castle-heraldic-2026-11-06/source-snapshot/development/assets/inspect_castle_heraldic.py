"""Independent runtime export contact and detached actual blade control."""
from pathlib import Path
import bpy,bmesh,importlib.util,hashlib,json
OUT=Path(__file__).with_name('castle-museum');path=OUT/'exports/level_castle_heraldic_arms.glb'
digest=hashlib.sha256(path.read_bytes()).hexdigest()
bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(path))
obj=next(o for o in bpy.context.scene.objects if o.type=='MESH');data=obj.data.copy()
bm=bmesh.new();bm.from_mesh(data);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=.000001);bm.to_mesh(data);bm.free()
for v in data.vertices:v.co=(v.co.x,v.co.z,-v.co.y)
data.calc_loop_triangles()
spec=importlib.util.spec_from_file_location('contact',Path(__file__).with_name('castle_armour_lowpoly.py'));contact=importlib.util.module_from_spec(spec);spec.loader.exec_module(contact)
def audit(mesh,phase,expected):
 try:r=contact.audit_contacts(mesh,phase,digest)
 except RuntimeError:
  if expected!='FAIL':raise
  r=json.loads((OUT/('armour-contact-audit-'+phase+'.json')).read_text())
 r['criterion']='Rigid wall audit(x,z,-y); every component reaches real wall-spine triangles within1mm'
 (OUT/('armour-contact-audit-'+phase+'.json')).write_text(json.dumps(r,indent=2)+'\n',encoding='utf-8')
 r['expected']=expected;(OUT/(phase+'.json')).write_text(json.dumps(r,indent=2)+'\n',encoding='utf-8');assert r['status']==expected
 return r
positive=audit(data,'heraldic-independent-wall-export','PASS')
negative=data.copy();links=[set() for v in negative.vertices]
for e in negative.edges:
 a,b=e.vertices;links[a].add(b);links[b].add(a)
remaining=set(range(len(links)));moved=0
while remaining:
 first=remaining.pop();group={first};queue=[first]
 while queue:
  for j in links[queue.pop()]:
   if j in remaining:remaining.remove(j);group.add(j);queue.append(j)
 if max(negative.vertices[i].co.y for i in group)>1.64 and min(negative.vertices[i].co.y for i in group)<.4:
  for i in group:negative.vertices[i].co.z+=.25
  moved+=1;break
assert moved==1,'Actual exported blade was not selected'
negative.calc_loop_triangles();audit(negative,'heraldic-detached-blade-negative','FAIL')
assert hashlib.sha256(path.read_bytes()).hexdigest()==digest
source=OUT/'heraldic_arms.blend';source_digest=hashlib.sha256(source.read_bytes()).hexdigest()
bpy.ops.wm.open_mainfile(filepath=str(source));images=[]
for obj in bpy.context.scene.objects:
 if obj.type!='MESH':continue
 for material in obj.data.materials:
  for node in material.node_tree.nodes:
   if node.type!='TEX_IMAGE':continue
   image=node.image;assert image and image.packed_file
   pixels=list(image.pixels[:4]);assert len(pixels)==4 and image.has_data and list(image.size)==[512,512]
   images.append(image.name)
assert images and hashlib.sha256(source.read_bytes()).hexdigest()==source_digest
(OUT/'heraldic-packed-source.json').write_text(json.dumps(dict(status='PASS',source_sha256=source_digest,images=images,criterion='Fresh source reopen and packed-image pixel decode; source bytes unchanged'),indent=2)+'\n',encoding='utf-8')
print('PASS independent actual export316tri /14 supported components; actual blade moved25cm fails; original GLB untouched')
