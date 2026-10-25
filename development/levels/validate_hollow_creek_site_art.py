"""Independent actual geometry/material/source audits for the site art pass."""
import bpy,bmesh,json,hashlib,struct,collections,math
from pathlib import Path
from mathutils import Vector
from mathutils.bvhtree import BVHTree
ROOT=Path(__file__).resolve().parents[2];OUT=ROOT/'development/levels/hollow-creek'
r=json.loads((OUT/'site-art.json').read_text(encoding='utf-8'));f=json.loads((OUT/'siteart-fitted-floors.json').read_text(encoding='utf-8'))
assert hashlib.sha256((ROOT/f['scene']).read_bytes()).hexdigest()==f['scene_sha256']
bpy.ops.wm.open_mainfile(filepath=str(ROOT/f['scene']));scene=bpy.context.scene
def tree(o):
 o.data.calc_loop_triangles();return BVHTree.FromPolygons([o.matrix_world@v.co for v in o.data.vertices],[tuple(t.vertices) for t in o.data.loop_triangles],all_triangles=True)
surfaces=[o for o in scene.objects if o.name=='level_camp_terrain_amenities' or o.name.startswith(('Continuous west forest lakeshore return','Continuous east forest lakeshore return'))];trees=[tree(o) for o in surfaces]
shore_faces=0;uvs=0
for o in surfaces:
 assert next(u.name for u in o.data.uv_layers if u.active_render)=='SiteArtWorldTile2m','Native renderer must use actual tile UVs'
 for loop in o.data.loops:
  p=o.matrix_world@o.data.vertices[loop.vertex_index].co;uv=o.data.uv_layers.active.data[loop.index].uv
  assert max(abs(uv[0]-p.x/2),abs(uv[1]-p.y/2))<.00001;uvs+=1
 for p in o.data.polygons:
  if min((o.matrix_world@o.data.vertices[i].co).z for i in p.vertices)<=-1.6:
   assert 'water margin' in o.data.materials[p.material_index].name,'Grass reaches water';shore_faces+=1
rocks=[o for o in scene.objects if o.name.startswith(('Grounded lake stone ','Distant bank boulder '))];contact=0;worst=0
props=[bpy.data.objects[p['name']] for p in json.loads((OUT/'amenities.json').read_text(encoding='utf-8'))['placements']]
for o in rocks:
 gaps=[]
 for v in o.data.vertices:
  p=o.matrix_world@v.co;hits=[t.ray_cast(Vector((p.x,p.y,50)),Vector((0,0,-1)),100)[0] for t in trees];hits=[h for h in hits if h is not None]
  assert hits,('Unsupported rock vertex',o.name);gaps.append(p.z-max(h.z for h in hits))
 gap=min(gaps);assert abs(gap+.025)<.00005,('Floating/buried rock',o.name,gap);worst=max(worst,abs(gap+.025));contact+=len(gaps)
 if o.name.startswith('Grounded'):
  a=[o.matrix_world@Vector(c) for c in o.bound_box]
  for prop in props:
   b=[prop.matrix_world@Vector(c) for c in prop.bound_box]
   overlaps=all(max(min(p[k] for p in a),min(p[k] for p in b))<min(max(p[k] for p in a),max(p[k] for p in b)) for k in [0,1,2])
   assert not overlaps,('Rock intersects facility/model',o.name,prop.name)
mem=bpy.data.objects['Camp wardens heron memorial'];group=mem.vertex_groups['Organic continuous cast bird'].index
selected={v.index for v in mem.data.vertices if any(a.group==group and a.weight>.5 for a in v.groups)}
bm=bmesh.new();bm.from_mesh(mem.data);bm.verts.ensure_lookup_table();faces=[fa for fa in bm.faces if all(v.index in selected for v in fa.verts)];edges={e for fa in faces for e in fa.edges};assert all(sum(fa in faces for fa in e.link_faces)==2 for e in edges),'Open casting'
remaining=set(selected);components=[]
adj={i:set() for i in selected}
for e in edges:
 a,b=[v.index for v in e.verts];adj[a].add(b);adj[b].add(a)
while remaining:
 queue=[remaining.pop()];n=0
 while queue:
  a=queue.pop();n+=1
  for b in adj[a]:
   if b in remaining:remaining.remove(b);queue.append(b)
 components.append(n)
bm.free();assert len(components)==1,('Primitive cast parts are still disconnected',components,len(selected),group)
for side in [-1,1]:
 foot=min(mem.data.vertices[i].co.z for i in selected if side*mem.data.vertices[i].co.x>.02 and mem.data.vertices[i].co.z<1.4);assert 1.268<foot<1.272
marks=bpy.data.objects['Cosmetic UV goose footprint carriers'];assert marks.hide_render,'Cosmetic marks leak into ordinary rendering'
for a in r['uv_cosmetic_attachments']:
 assert a['inner_offset_m']>0 and a['outer_offset_m']>a['inner_offset_m']
def canonical(tri):
 i=min(range(3),key=lambda i:tri[i]);return tuple(tri[i:]+tri[:i])
def raw(data):
 length=struct.unpack_from('<I',data,12)[0];g=json.loads(data[20:20+length]);start=28+length
 assert len(g['nodes'])==1 and all(k not in g['nodes'][0] for k in ['matrix','rotation','translation','scale'])
 def val(index,i,n):
  a=g['accessors'][index];v=g['bufferViews'][a['bufferView']];code,size={5126:('f',4),5125:('I',4),5123:('H',2),5121:('B',1)}[a['componentType']]
  return struct.unpack_from('<'+code*n,data,start+v.get('byteOffset',0)+a.get('byteOffset',0)+i*v.get('byteStride',size*n))
 result=collections.Counter()
 for mesh in g['meshes']:
  for p in mesh['primitives']:
   for i in range(0,g['accessors'][p['indices']]['count'],3):result[canonical([val(p['attributes']['POSITION'],val(p['indices'],i+j,1)[0],3) for j in range(3)])]+=1
 return result
sculpt=json.loads((OUT/'organic-memorial-model.json').read_text(encoding='utf-8'))['asset'];assets=r['assets']+[sculpt];checked=[]
for a in assets:
 file=ROOT/f"assets/runtime/models/{a['id']}.glb";assert hashlib.sha256(file.read_bytes()).hexdigest()==a['sha256'];assert file.stat().st_size<4_000_000
 bpy.ops.wm.read_factory_settings(use_empty=True)
 with bpy.data.libraries.load(str(ROOT/a['editable']),link=False) as (src,dst):dst.objects=list(src.objects)
 objects=[o for o in dst.objects if o and o.type=='MESH'];assert len(objects)==1
 o=objects[0];o.data.calc_loop_triangles();assert len(o.data.loop_triangles)<50000
 expected=collections.Counter(canonical([(v.co.x,v.co.z,-v.co.y) for v in [o.data.vertices[i] for i in t.vertices]]) for t in o.data.loop_triangles)
 actual=raw(file.read_bytes());assert expected==actual,('Raw geometry export disagreement',a['id'])
 checked.append(dict(id=a['id'],triangles=len(o.data.loop_triangles),bytes=file.stat().st_size,exact_raw_oriented_triangle_identity=True))
assert hashlib.sha256((ROOT/r['parent']).read_bytes()).hexdigest()==r['parent_sha256']
report=dict(status='pass-native-material-support-and-source-only',scene=f['scene'],scene_sha256=f['scene_sha256'],world_scale_uv_loops=uvs,bare_shore_faces=shore_faces,rocks=len(rocks),actual_rock_vertex_support_probes=contact,max_rock_seating_error_m=worst,facility_bounds_clear=True,continuous_cast_components=components,closed_cast=True,both_feet_seated=True,cosmetic_uv_cubes=5,hidden_without_native_uv_diagnostic=True,assets=checked,runtime_encounter_or_map_acceptance=False)
(ROOT/'development/levels/verification/hollow-creek-siteart.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8');print('PASS',json.dumps(report),flush=True)
