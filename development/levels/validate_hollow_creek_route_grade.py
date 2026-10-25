"""Independent local grade/source preservation and raw export audit."""
import bpy,json,hashlib,array,struct,collections,math
from pathlib import Path
from mathutils import Vector
from mathutils.bvhtree import BVHTree
ROOT=Path(__file__).resolve().parents[2];OUT=ROOT/'development/levels/hollow-creek'
r=json.loads((OUT/'route-grade.json').read_text());name='level_camp_terrain_amenities'
def digest(o,positions=True):
 h=hashlib.sha256();h.update(struct.pack('>16d',*(v for row in o.matrix_world for v in row)))
 for data,field,code,n in ([(o.data.vertices,'co','f',3)] if positions else [])+[(o.data.loops,'vertex_index','i',1),(o.data.polygons,'material_index','i',1)]:
  a=array.array(code,[0])*(len(data)*n);data.foreach_get(field,a);h.update(a.tobytes())
 for uv in o.data.uv_layers:
  a=array.array('f',[0])*(len(uv.data)*2);uv.data.foreach_get('uv',a);h.update(uv.name.encode());h.update(a.tobytes())
 h.update(bytes(p.use_smooth for p in o.data.polygons));h.update(repr([m.name for m in o.data.materials]).encode());return h.hexdigest()
for p,key in [('parent','parent_sha256'),('scene','scene_sha256')]:assert hashlib.sha256((ROOT/r[p]).read_bytes()).hexdigest()==r[key]
bpy.ops.wm.open_mainfile(filepath=str(ROOT/r['parent']));before={o.name:digest(o) for o in bpy.context.scene.objects if o.type=='MESH' and o.name!=name};old=bpy.data.objects[name];old.data.calc_loop_triangles();xyz=[tuple(v.co) for v in old.data.vertices];topology=digest(old,False);tri=[tuple(t.vertices) for t in old.data.loop_triangles]
bpy.ops.wm.open_mainfile(filepath=str(ROOT/r['scene']));current={o.name:digest(o) for o in bpy.context.scene.objects if o.type=='MESH' and o.name!=name};assert current==before,'Unrelated mesh geometry/UV/material/pose changed'
o=bpy.data.objects[name];assert digest(o,False)==topology;assert len(o.data.vertices)==len(xyz);changed=[];maximum=0;protected=0;maxgrade=0
for i,(a,v) in enumerate(zip(xyz,o.data.vertices)):
 assert a[:2]==tuple(v.co)[:2],'Plan geometry changed'
 dz=abs(a[2]-v.co.z)
 if dz>1e-9:changed.append(i);maximum=max(maximum,dz);assert 54.3<a[0]<55.8 and -15.8<a[1]<-13.5
assert changed==[p['index'] for p in r['changed_vertices']] and len(changed)==3 and maximum<.013
def grade(points):
 n=(Vector(points[1])-Vector(points[0])).cross(Vector(points[2])-Vector(points[0]));return math.hypot(n.x,n.y)/abs(n.z) if abs(n.z)>1e-12 else None
for ids in tri:
 p=[xyz[i] for i in ids];g=grade(p)
 if g is None:continue
 now=grade([o.data.vertices[i].co for i in ids]);assert now<=max(.19,g)+2e-6,('New steep face',ids,g,now)
 if any(54.3<v[0]<55.8 and -15.8<v[1]<-13.5 for v in p) and min(v[0] for v in p)<=55.30 and max(v[0] for v in p)>=54.70 and min(v[1] for v in p)<=-14 and max(v[1] for v in p)>=-15.3 and g<=.35:
  protected+=1;maxgrade=max(maxgrade,now);assert now<=.190002
o.data.calc_loop_triangles();ground=BVHTree.FromPolygons([o.matrix_world@v.co for v in o.data.vertices],[tuple(t.vertices) for t in o.data.loop_triangles],all_triangles=True)
bank=json.loads((OUT/'grounded-berm.json').read_text());toe=0
for section in bank['stations']:
 for row in section['rows']:
  for j in [0,11]:
   x,y,z=row['profile'][j];hit=ground.ray_cast(Vector((x,y,40)),Vector((0,0,-1)),100)[0]
   if hit is not None:assert abs(z-hit.z+.06)<.00002;toe+=1
def canonical(v):
 i=min(range(3),key=lambda i:v[i]);return tuple(v[i:]+v[:i])
expected=collections.Counter(canonical([(o.data.vertices[i].co.x,o.data.vertices[i].co.z,-o.data.vertices[i].co.y) for i in t.vertices]) for t in o.data.loop_triangles)
a=r['assets'][0];data=(ROOT/a['model']).read_bytes();assert hashlib.sha256(data).hexdigest()==a['sha256'] and len(data)<4000000
n=struct.unpack_from('<I',data,12)[0];g=json.loads(data[20:20+n]);base=28+n
def read(ix):
 ac=g['accessors'][ix];view=g['bufferViews'][ac['bufferView']];code,size={5126:('f',4),5125:('I',4),5123:('H',2)}[ac['componentType']];width={'SCALAR':1,'VEC3':3}[ac['type']];start=base+view.get('byteOffset',0)+ac.get('byteOffset',0)
 return [struct.unpack_from('<'+code*width,data,start+i*view.get('byteStride',width*size)) for i in range(ac['count'])]
actual=collections.Counter()
for mesh in g['meshes']:
 for p in mesh['primitives']:
  vertices=read(p['attributes']['POSITION']);indices=[v[0] for v in read(p['indices'])]
  for i in range(0,len(indices),3):actual[canonical([vertices[j] for j in indices[i:i+3]])]+=1
assert actual==expected and sum(actual.values())==41782,'Raw oriented GLB/native mismatch'
bpy.ops.wm.open_mainfile(filepath=str(ROOT/a['editable']));models=[o for o in bpy.context.scene.objects if o.type=='MESH'];assert len(models)==1 and not bpy.data.libraries and all(im.packed_file for im in bpy.data.images if im.source=='FILE');editable=models[0];editable.data.calc_loop_triangles();assert collections.Counter(canonical([(editable.data.vertices[i].co.x,editable.data.vertices[i].co.z,-editable.data.vertices[i].co.y) for i in t.vertices]) for t in editable.data.loop_triangles)==expected
report=dict(status='pass-local-native-route-grade',source_sha256=r['scene_sha256'],unchanged_other_meshes=len(before),changed_vertices=changed,max_height_change_m=maximum,protected_triangles=protected,max_protected_grade=maxgrade,bank_toe_terrain_probes=toe,terrain_xy_topology_uv_materials_preserved=True,exact_raw_native_export_triangles=41782,isolated_packed_editable_scene=True,world_gameplay_acceptance=False)
(ROOT/'development/levels/verification/hollow-creek-route-grade.json').write_text(json.dumps(report,indent=2)+'\n');print('PASS',json.dumps(report),flush=True)
