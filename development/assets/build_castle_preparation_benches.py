"""Repair measured source tool support for Castle variants; preserve library originals."""
from pathlib import Path
import bpy,bmesh,json,hashlib,importlib.util
from mathutils import Vector
from mathutils.geometry import intersect_ray_tri
ROOT=Path(__file__).resolve().parents[2];OUT=ROOT/'development/assets/castle-training';OUT.mkdir(parents=True,exist_ok=True)
spec=importlib.util.spec_from_file_location('contact',Path(__file__).with_name('castle_armour_lowpoly.py'));contact=importlib.util.module_from_spec(spec);spec.loader.exec_module(contact)
def audit(mesh,phase,digest=None):
 temporary=ROOT/'development/assets/castle-museum'/('armour-contact-audit-'+phase+'.json')
 try:r=contact.audit_contacts(mesh,phase,digest)
 except RuntimeError:r=json.loads(temporary.read_text(encoding='utf-8'))
 r['criterion']='Every actual source/export component reaches table legs/floor through triangle contact within1mm'
 (OUT/(phase+'.json')).write_text(json.dumps(r,indent=2)+'\n',encoding='utf-8');temporary.unlink();return r
for source_id,target in [('level_workbench','level_castle_preparation_bench'),('level_workbench_worn','level_castle_preparation_bench_worn')]:
 source=ROOT/'assets/libraries/industrial/models'/(source_id+'.glb');original_hash=hashlib.sha256(source.read_bytes()).hexdigest()
 bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(source));obj=next(o for o in bpy.context.scene.objects if o.type=='MESH');m=obj.data
 bm=bmesh.new();bm.from_mesh(m);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=1e-6);bm.to_mesh(m);bm.free();m.calc_loop_triangles()
 failed=audit(m,source_id+'-rejected-source',original_hash);assert failed['status']=='FAIL'
 bad=[c['bounds'] for c in failed['components'] if not c['supported']];assert len(bad)==5,len(bad)
 indices=set()
 for v in m.vertices:
  if any(all(b[0][k]-1e-6<=v.co[k]<=b[1][k]+1e-6 for k in range(3)) for b in bad):indices.add(v.index)
 assert indices
 # All five groups belong to the same loose tabletop cluster layer. Query
 # actual top triangles excluding that entire layer, preserving relative parts.
 minimum=min(m.vertices[i].co.z for i in indices);hits=[]
 for b in bad:
  origin=Vector(((b[0][0]+b[1][0])/2,(b[0][1]+b[1][1])/2,3))
  for tri in m.loop_triangles:
   if any(i in indices for i in tri.vertices):continue
   hit=intersect_ray_tri(*(m.vertices[i].co for i in tri.vertices),Vector((0,0,-1)),origin,True)
   if hit is not None:hits.append(hit.z)
 support=max(hits);shift=support-.001-minimum;assert -.06<shift<0,(support,minimum,shift)
 for i in indices:m.vertices[i].co.z+=shift
 m.calc_loop_triangles();fixed=audit(m,target+'-source');assert fixed['status']=='PASS'
 for material in m.materials:
  for node in material.node_tree.nodes:
   if node.type=='TEX_IMAGE' and not node.image.packed_file:node.image.pack()
 bpy.ops.wm.save_as_mainfile(filepath=str(OUT/(target+'.blend')))
 path=OUT/(target+'.glb');bpy.ops.export_scene.gltf(filepath=str(path),export_format='GLB',export_yup=True)
 bpy.ops.object.select_all(action='SELECT');bpy.ops.object.delete(use_global=False);bpy.ops.import_scene.gltf(filepath=str(path));obj=next(o for o in bpy.context.scene.objects if o.type=='MESH');data=obj.data.copy()
 bm=bmesh.new();bm.from_mesh(data);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=1e-6);bm.to_mesh(data);bm.free();data.calc_loop_triangles()
 final=audit(data,target+'-export',hashlib.sha256(path.read_bytes()).hexdigest());assert final['status']=='PASS'
 assert hashlib.sha256(source.read_bytes()).hexdigest()==original_hash
 final.update(status='SCOPED_EXPORTED_SUPPORT_VERIFIED_NATIVE_REVIEW_PENDING',original_sha256=original_hash,cluster_translation_z=shift,actual_support_z=support,source='Original geometry/UV/materials, five-group tabletop cluster translated only vertically; original master untouched')
 (OUT/(target+'-review.json')).write_text(json.dumps(final,indent=2)+'\n',encoding='utf-8')
 print('PASS',target,final['triangle_count'],'triangles; original source failed5 groups; repair shift',shift,'actual support',support)
