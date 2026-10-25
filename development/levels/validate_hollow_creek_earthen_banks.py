"""Actual earth-bank support, closed geometry, facilities and export checks.
The user authorized a shape change; do not reuse material-only geometry claims.
"""
import bpy,bmesh,json,hashlib,struct,array,collections,math
from pathlib import Path
from mathutils import Vector,Quaternion
from mathutils.bvhtree import BVHTree
ROOT=Path(__file__).resolve().parents[2];OUT=ROOT/'development/levels/hollow-creek';r=json.loads((OUT/'grounded-berm.json').read_text(encoding='utf-8'));name='level_camp_boundary_banks'
assert hashlib.sha256((ROOT/r['scene']).read_bytes()).hexdigest()==r['scene_sha256']
def digest(o):
 h=hashlib.sha256();h.update(struct.pack('>16d',*(v for row in o.matrix_world for v in row)))
 for data,field,code,n in [(o.data.vertices,'co','f',3),(o.data.loops,'vertex_index','i',1),(o.data.polygons,'material_index','i',1)]:
  a=array.array(code,[0])*(len(data)*n);data.foreach_get(field,a);h.update(a.tobytes())
 for uv in o.data.uv_layers:
  a=array.array('f',[0])*(len(uv.data)*2);uv.data.foreach_get('uv',a);h.update(uv.name.encode());h.update(a.tobytes())
 h.update(bytes(p.use_smooth for p in o.data.polygons));h.update(repr([m.name for m in o.data.materials]).encode());return h.hexdigest()
foliage_name='level_camp_boundary_logfall'
bpy.ops.wm.open_mainfile(filepath=str(ROOT/r['source']));before={o.name:digest(o) for o in bpy.context.scene.objects if o.type=='MESH' and o.name not in [name,foliage_name]}
old_foliage=bpy.data.objects[foliage_name];old_positions=[v.co.copy() for v in old_foliage.data.vertices];old_members={g.name:{v.index for v in old_foliage.data.vertices if any(a.group==g.index and a.weight>.5 for a in v.groups)} for g in old_foliage.vertex_groups}
old_loops=[l.vertex_index for l in old_foliage.data.loops];old_uvs={u.name:[tuple(v.uv) for v in u.data] for u in old_foliage.data.uv_layers}
old_bank=bpy.data.objects[name];old_bank.data.calc_loop_triangles();old_soil=BVHTree.FromPolygons([old_bank.matrix_world@v.co for v in old_bank.data.vertices],[tuple(t.vertices) for t in old_bank.data.loop_triangles if old_bank.data.polygons[t.polygon_index].material_index==0],all_triangles=True)
bpy.ops.wm.open_mainfile(filepath=str(ROOT/r['scene']));scene=bpy.context.scene;current={o.name:o for o in scene.objects if o.type=='MESH' and o.name not in [name,foliage_name]};assert before.keys()==current.keys()
for key,o in current.items():assert before[key]==digest(o),('Changed existing terrain/model/van/bedding/UV',key)
bank=bpy.data.objects[name];bank.data.calc_loop_triangles();bm=bmesh.new();bm.from_mesh(bank.data);assert all(len(e.link_faces)==2 for e in bm.edges),'Open bank/outcrop shell';bm.free()
def tree(o,ids=None):
 o.data.calc_loop_triangles();return BVHTree.FromPolygons([o.matrix_world@v.co for v in o.data.vertices],[tuple(t.vertices) for t in o.data.loop_triangles if ids is None or all(i in ids for i in t.vertices)],all_triangles=True)
ground_objects=[bpy.data.objects['level_camp_terrain_amenities']]+[o for o in scene.objects if o.name.startswith(('Continuous west forest lakeshore return','Continuous east forest lakeshore return'))]
grounds=[tree(o) for o in ground_objects]
def hit(t,x,y):return t.ray_cast(Vector((x,y,40)),Vector((0,0,-1)),100)[0]
def ground(x,y):
 p=[hit(t,x,y) for t in grounds];p=[v.z for v in p if v is not None];assert p,(x,y);return max(p)
groups={g.index for g in bank.vertex_groups if g.name.startswith('Earthen bank ')};soil_ids={v.index for v in bank.data.vertices if any(a.group in groups and a.weight>.5 for a in v.groups)};soil=tree(bank,soil_ids)
toe_probes=0;width=[];relief=[]
actual_soil_vertices={tuple(bank.data.vertices[i].co) for i in soil_ids}
for path in r['stations']:
 for row in path['rows']:
  assert all(tuple(p) in actual_soil_vertices for p in row['profile']),'Source section metadata is not actual bank geometry'
  width.append(row['inside_m']+row['outside_m']);relief.append(row['peak_z']-row['ground_z'])
  assert len(row['profile'])==15 and row['inside_m']>=1 and row['outside_m']>=1.6
  for j in [0,11]:
   x,y,z=row['profile'][j];error=z-ground(x,y);assert abs(error+.06)<.00002,('Unsupported earth-bank toe',error);toe_probes+=1
assert max(width)-min(width)>1.5 and max(relief)-min(relief)>.7,'Repeated uniform bank section'
foliage=bpy.data.objects[foliage_name];assert old_loops==[l.vertex_index for l in foliage.data.loops]
for key,values in old_uvs.items():assert values==[tuple(v.uv) for v in foliage.data.uv_layers[key].data]
expected=[p.copy() for p in old_positions];moved=set();rigid_error=0
for refit in r['crest_refits']:
 indices=set.union(*(old_members[g] for g in refit['groups']));assert not (moved&indices),'Assembly groups overlap';moved.update(indices)
 for i in indices:
  if refit['kind']=='rigid-log':expected[i]=Vector(refit['new_axis'][0])+Quaternion(refit['quaternion'])@(old_positions[i]-Vector(refit['old_axis'][0]))
  else:expected[i]=old_positions[i]+Vector(refit['translation'])
for i,p in enumerate(expected):
 error=(p-foliage.data.vertices[i].co).length;assert error<.00001,('Crest assembly distorted/disconnected',i,error);rigid_error=max(rigid_error,error)
log_contacts=[];plant_contacts=[]
before_contacts=[]
for refit in [v for v in r['crest_refits'] if v['kind']=='rigid-log']:
 a,c=[Vector(v) for v in refit['old_axis']];axis=(c-a).normalized();length=(c-a).length;points=[old_positions[i] for i in old_members[refit['name']]]
 for end in [0,1]:
  bearing=[p for p in points if ((p-a).dot(axis)/length<.12 if end==0 else (p-a).dot(axis)/length>.88)];new_gaps=[];prior_gaps=[]
  for p in bearing:
   prior=hit(old_soil,p.x,p.y);now=hit(soil,p.x,p.y)
   if prior is not None:prior_gaps.append(p.z-prior.z)
   if now is not None:new_gaps.append(p.z-now.z)
  before_contacts.append(dict(name=refit['name'],end=end,actual_original_body_min_gap_over_original_soil_m=min(prior_gaps) if prior_gaps else None,actual_original_body_min_gap_over_new_soil_m=min(new_gaps) if new_gaps else None))
(ROOT/'development/quality/evidence/DEV-084-actual-crest-bearing-before-after.json').write_text(json.dumps(dict(source=r['source'],source_sha256=r['source_sha256'],new_soil_scene=r['scene'],new_soil_scene_sha256=r['scene_sha256'],before_refit_actual_body_contacts=before_contacts,note='Diagnostic uses actual original end-region geometry, not metadata bottoms or a cap/axis ray; passing refitted contacts are in the current bank verification report.'),indent=2)+'\n',encoding='utf-8')
for record in r['crest_log_contacts']:
 group=foliage.vertex_groups[record['group']].index;points=[foliage.matrix_world@v.co for v in foliage.data.vertices if any(a.group==group and a.weight>.5 for a in v.groups)];a,c=[Vector(v) for v in record['axis']];axis=(c-a).normalized();length=(c-a).length
 for end in [0,1]:
  bearing=[p for p in points if ((p-a).dot(axis)/length<.12 if end==0 else (p-a).dot(axis)/length>.88)];assert bearing
  gaps=[]
  for p in bearing:
   g=hit(soil,p.x,p.y);assert g is not None;gaps.append(p.z-g.z)
  gap=min(gaps);log_contacts.append(dict(name=record['name'],end=end,actual_bearing_vertex_probes=len(gaps),bottom_minus_soil_m=gap));assert -.20<gap<.025,('Floating/buried actual crest log',record['name'],end,gap)
for refit in [v for v in r['crest_refits'] if v['kind']=='rigid-rooted-scrub']:
 p=Vector(refit['base_before'])+Vector(refit['translation']);g=hit(soil,p.x,p.y);assert g is not None and abs(p.z-g.z+.08)<.00002;plant_contacts.append(refit['name'])
outcrops=[]
for g in [g for g in bank.vertex_groups if g.name.startswith('Bedrock outcrop ')]:
 points=[bank.matrix_world@v.co for v in bank.data.vertices if any(a.group==g.index and a.weight>.5 for a in v.groups)];gaps=[]
 for p in points:
  below=hit(soil,p.x,p.y)
  if below is not None:gaps.append(p.z-below.z)
 assert len(gaps)==len(points),('Bedrock extends off supporting bank',g.name)
 buried=sum(v<-.05 for v in gaps);exposed=sum(v>.05 for v in gaps)
 assert buried>=3 and exposed>=3,('Outcrop lacks buried roots or exposed stone face',g.name,buried,exposed,min(gaps),max(gaps))
 outcrops.append(dict(name=g.name,buried_vertex_probes=buried,exposed_vertex_probes=exposed,deepest_burial_m=min(gaps)))
assert len(outcrops)==len(r['outcrops'])>=8
props=json.loads((OUT/'amenities.json').read_text(encoding='utf-8'))['placements'];props+=[dict(name=n) for n in ['west-pitched-ridge','east-pitched-dome','Camp wardens heron memorial']]
prop_probes=0;intersections=[]
for record in props:
 o=bpy.data.objects[record['name']]
 for v in o.data.vertices:
  p=o.matrix_world@v.co;h=hit(soil,p.x,p.y);prop_probes+=1
  if h is None:continue
  g=ground(p.x,p.y)
  if p.z>g+.015 and h.z>g+.04 and h.z>p.z+.015:intersections.append(dict(model=o.name,point=list(p),bank_z=h.z))
assert not intersections,('New earth covers a facility/model',intersections[:8])
def canonical(tri):
 i=min(range(3),key=lambda i:tri[i]);return tuple(tri[i:]+tri[:i])
a=r['assets'][0];file=ROOT/a['model'];data=file.read_bytes();assert hashlib.sha256(data).hexdigest()==a['sha256'];assert len(data)<4_000_000 and len(bank.data.loop_triangles)<50000
length=struct.unpack_from('<I',data,12)[0];g=json.loads(data[20:20+length]);start=28+length
assert len(g['nodes'])==1 and all(k not in g['nodes'][0] for k in ['matrix','rotation','translation','scale'])
def val(index,i,n):
 acc=g['accessors'][index];view=g['bufferViews'][acc['bufferView']];code,size={5126:('f',4),5125:('I',4),5123:('H',2),5121:('B',1)}[acc['componentType']]
 return struct.unpack_from('<'+code*n,data,start+view.get('byteOffset',0)+acc.get('byteOffset',0)+i*view.get('byteStride',size*n))
expected=collections.Counter(canonical([(bank.data.vertices[i].co.x,bank.data.vertices[i].co.z,-bank.data.vertices[i].co.y) for i in t.vertices]) for t in bank.data.loop_triangles);actual=collections.Counter()
for mesh in g['meshes']:
 for p in mesh['primitives']:
  for i in range(0,g['accessors'][p['indices']]['count'],3):actual[canonical([val(p['attributes']['POSITION'],val(p['indices'],i+j,1)[0],3) for j in range(3)])]+=1
assert expected==actual,'Actual new bank collider/render geometry differs from export'
bank_bytes=len(data);bank_triangle_count=len(bank.data.loop_triangles);export_checks=[dict(id=a['id'],triangles=bank_triangle_count,bytes=len(data),exact_raw_oriented_triangles=True)];native_sources={a['id']:expected.copy()}
for a in r['assets'][1:]:
 data=(ROOT/a['model']).read_bytes();assert hashlib.sha256(data).hexdigest()==a['sha256'];assert len(data)<4_000_000
 length=struct.unpack_from('<I',data,12)[0];g=json.loads(data[20:20+length]);start=28+length
 assert len(g['nodes'])==1 and all(k not in g['nodes'][0] for k in ['matrix','rotation','translation','scale'])
 foliage.data.calc_loop_triangles();expected=collections.Counter(canonical([(foliage.data.vertices[i].co.x,foliage.data.vertices[i].co.z,-foliage.data.vertices[i].co.y) for i in t.vertices]) for t in foliage.data.loop_triangles);actual=collections.Counter()
 for mesh in g['meshes']:
  for p in mesh['primitives']:
   for i in range(0,g['accessors'][p['indices']]['count'],3):actual[canonical([val(p['attributes']['POSITION'],val(p['indices'],i+j,1)[0],3) for j in range(3)])]+=1
 assert expected==actual and len(foliage.data.loop_triangles)<50000,'Refitted crest source/export mismatch'
 native_sources[a['id']]=expected.copy()
 export_checks.append(dict(id=a['id'],triangles=len(foliage.data.loop_triangles),bytes=len(data),exact_raw_oriented_triangles=True))
editable_checks=[]
for a in r['assets']:
 bpy.ops.wm.open_mainfile(filepath=str(ROOT/a['editable']));o=bpy.data.objects[a['id']];assert len(bpy.context.scene.objects)==1 and bpy.context.scene.objects[0]==o,'Editable source does not open into its isolated model scene'
 assert all(v.library is None for values in [bpy.data.objects,bpy.data.meshes,bpy.data.materials,bpy.data.images] for v in values),'Editable model requires an external linked library'
 o.data.calc_loop_triangles();actual=collections.Counter(canonical([(o.data.vertices[i].co.x,o.data.vertices[i].co.z,-o.data.vertices[i].co.y) for i in t.vertices]) for t in o.data.loop_triangles);assert actual==native_sources[a['id']],'Editable source differs from current native/export'
 images={n.image for m in o.data.materials if m.use_nodes for n in m.node_tree.nodes if n.type=='TEX_IMAGE' and n.image};assert images and all(im.packed_file for im in images),'Unpacked editable model texture'
 editable_checks.append(dict(id=a['id'],directly_openable_isolated_scene=True,actual_native_triangle_identity=True,packed_texture_images=len(images)))
assert hashlib.sha256((ROOT/r['source']).read_bytes()).hexdigest()==r['source_sha256']
report=dict(status='pass-native-earth-bank-only',scene=r['scene'],scene_sha256=r['scene_sha256'],user_authorized_reshaping=True,unchanged_other_mesh_geometry_uv_materials=len(before),closed_bank_and_outcrop_shells=True,actual_toe_terrain_probes=toe_probes,bank_width_range_m=[min(width),max(width)],crest_relief_range_m=[min(relief),max(relief)],actual_crest_log_contacts=log_contacts,rigid_crest_assemblies=len(r['crest_refits']),max_rigid_refit_error_m=rigid_error,rooted_scrub_supports=len(plant_contacts),embedded_solid_bedrock_outcrops=outcrops,facility_vertex_clearance_probes=prop_probes,exact_raw_source_export_triangles=True,triangles=bank_triangle_count,bytes=bank_bytes,exports=export_checks,editable_scene_checks=editable_checks,runtime_movement_or_map_acceptance=False)
(ROOT/'development/levels/verification/hollow-creek-berm.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8');print('PASS',json.dumps(report),flush=True)
