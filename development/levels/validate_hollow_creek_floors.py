"""Independent dense actual-surface, closed-depth, UV and exported mesh audit."""
import bpy,bmesh,json,math,hashlib,array,struct,collections
from pathlib import Path
from mathutils import Vector
from mathutils.bvhtree import BVHTree
ROOT=Path(__file__).resolve().parents[2];OUT=ROOT/'development/levels/hollow-creek'
r=json.loads((OUT/'fitted-floors.json').read_text(encoding='utf-8'))
assert hashlib.sha256((ROOT/r['scene']).read_bytes()).hexdigest()==r['scene_sha256'],'Stale scene manifest; finish generation before verification'

def selected(o):
 ids={g.index for g in o.vertex_groups if 'groundsheet' in g.name.lower() or 'physical floor' in g.name.lower()}
 return {v.index for v in o.data.vertices if any(a.group in ids and a.weight>.5 for a in v.groups)}
def triangles(o,indices=None,world=True):
 o.data.calc_loop_triangles();vv=[o.matrix_world@v.co if world else v.co.copy() for v in o.data.vertices]
 tt=[t for t in o.data.loop_triangles if indices is None or all(i in indices for i in t.vertices)]
 return vv,tt
def tree(o,indices=None,world=True):
 vv,tt=triangles(o,indices,world);return BVHTree.FromPolygons(vv,[tuple(t.vertices) for t in tt],all_triangles=True),vv,tt
def digest(o):
 h=hashlib.sha256();h.update(struct.pack('>16d',*(c for row in o.matrix_world for c in row)))
 for data,field,code,n in [(o.data.vertices,'co','f',3),(o.data.loops,'vertex_index','i',1),(o.data.polygons,'material_index','i',1)]:
  values=array.array(code,[0])*(len(data)*n);data.foreach_get(field,values);h.update(values.tobytes())
 h.update(bytes(p.use_smooth for p in o.data.polygons))
 for layer in o.data.uv_layers:
  values=array.array('f',[0])*(len(layer.data)*2);layer.data.foreach_get('uv',values);h.update(layer.name.encode());h.update(values.tobytes())
 return h.hexdigest()
def parts(o):
 floor=selected(o);rows=[];o.data.calc_loop_triangles()
 for t in o.data.loop_triangles:
  if all(i in floor for i in t.vertices):continue
  p=o.data.polygons[t.polygon_index]
  ring=[]
  for loop in t.loops:
   co=tuple(o.data.vertices[o.data.loops[loop].vertex_index].co);uv=tuple(tuple(layer.data[loop].uv) for layer in o.data.uv_layers)
   ring.append((co,uv))
  k=min(range(len(ring)),key=lambda i:ring[i]);rows.append((p.material_index,p.use_smooth,tuple(ring[k:]+ring[:k])))
 return sorted(rows)
def cross(a,c):return a[0]*c[1]-a[1]*c[0]
def inside(x,y,hull,margin=.00001):
 for a,c in zip(hull,hull[1:]+hull[:1]):
  edge=(c[0]-a[0],c[1]-a[1])
  if cross(edge,(x-a[0],y-a[1]))<margin*math.hypot(*edge):return False
 return True

bpy.ops.wm.open_mainfile(filepath=str(ROOT/r['source']));changed={p['name'] for p in r['instances']}
old={o.name:(parts(o) if o.name in changed else digest(o)) for o in bpy.context.scene.objects if o.type=='MESH'}
poses={o.name:(tuple(o.location),tuple(o.rotation_euler),tuple(o.scale)) for o in bpy.context.scene.objects if o.type=='MESH'}
old_uv={}
for name in changed:
 o=bpy.data.objects[name];floor=selected(o);vv,tt=triangles(o,floor);top=[t for t in tt if (vv[t.vertices[1]]-vv[t.vertices[0]]).cross(vv[t.vertices[2]]-vv[t.vertices[0]]).z>0]
 bt=BVHTree.FromPolygons(vv,[tuple(t.vertices) for t in top],all_triangles=True)
 old_uv[name]=(bt,[(tuple(vv[i] for i in t.vertices),tuple(tuple(o.data.uv_layers.active.data[loop].uv) for loop in t.loops)) for t in top])

bpy.ops.wm.open_mainfile(filepath=str(ROOT/r['scene']));scene=bpy.context.scene;terrain,_,_=tree(bpy.data.objects['level_camp_terrain_amenities'])
assert old.keys()=={o.name for o in scene.objects if o.type=='MESH'}
checks=[];total_probes=0;max_error=0;uv_error=0;floor_by_name={};roof_floor_probes=0
for o in scene.objects:
 if o.type!='MESH':continue
 assert poses[o.name]==(tuple(o.location),tuple(o.rotation_euler),tuple(o.scale)),('Pose changed',o.name)
 if o.name not in changed:assert old[o.name]==digest(o),('Unrelated mesh/UV changed',o.name)
for row in r['instances']:
 o=bpy.data.objects[row['name']];assert old[o.name]==parts(o),('Nonfloor shape/material/UV changed',o.name)
 floor=selected(o);bt,vv,tt=tree(o,floor);bm=bmesh.new();bm.from_mesh(o.data);bm.verts.ensure_lookup_table()
 floor_by_name[o.name]=bt
 faces={f for f in bm.faces if all(v.index in floor for v in f.verts)};edges={e for f in faces for e in f.edges}
 bad=sum(sum(f in faces for f in e.link_faces)!=2 for e in edges);assert bad==0,('Floor is not a closed two-face-per-edge shell',o.name,bad);bm.free()
 hull=row['footprint_native_xy'];xmin=min(p[0] for p in hull);xmax=max(p[0] for p in hull);ymin=min(p[1] for p in hull);ymax=max(p[1] for p in hull)
 probes=0;error=0;depth_error=0
 nx=math.ceil((xmax-xmin)/.035);ny=math.ceil((ymax-ymin)/.035)
 for i in range(nx+1):
  for j in range(ny+1):
   x=xmin+(xmax-xmin)*(i+.173)/(nx+1);y=ymin+(ymax-ymin)*(j+.319)/(ny+1)
   if not inside(x,y,hull,.001):continue
   g=terrain.ray_cast(Vector((x,y,10)),Vector((0,0,-1)),30)[0];top=bt.ray_cast(Vector((x,y,10)),Vector((0,0,-1)),30)[0];bottom=bt.ray_cast(Vector((x,y,-10)),Vector((0,0,1)),30)[0]
   assert g is not None and top is not None and bottom is not None,('Missing terrain/floor skin',o.name,x,y)
   e=max(abs(top.z-g.z-.028),abs(bottom.z-g.z-.016));de=abs(top.z-bottom.z-.012)
   assert e<.00005 and de<.00005,('Wrong actual terrain fit/depth',o.name,e,de)
   probes+=1;error=max(error,e);depth_error=max(depth_error,de)
 # UV comparison interpolates the previous actual top triangles at each new
 # triangle centroid; no producer affine-coefficient shortcut is reused here.
 previous,encoded=old_uv[o.name];uv_probes=0;local_uv_error=0;top_area=0
 for t in tt:
  a,c,d=[vv[i] for i in t.vertices];normal=(c-a).cross(d-a)
  if normal.z<=1e-12:continue
  top_area+=normal.z*.5;p=(a+c+d)/3
  hit,_,index,_=previous.ray_cast(p+Vector((0,0,1)),Vector((0,0,-1)),2);assert hit is not None,('Source UV surface missing',o.name)
  points,values=encoded[index];aa,cc,dd=points;ac=cc-aa;ad=dd-aa;ap=hit-aa;den=cross(ac,ad)
  u=cross(ap,ad)/den;v=cross(ac,ap)/den
  expected=[values[0][k]*(1-u-v)+values[1][k]*u+values[2][k]*v for k in [0,1]]
  actual=[sum(o.data.uv_layers.active.data[loop].uv[k] for loop in t.loops)/3 for k in [0,1]]
  gap=max(abs(a-b) for a,b in zip(expected,actual));assert gap<3e-5,('Source top UV changed',o.name,gap)
  local_uv_error=max(local_uv_error,gap);uv_probes+=1
 expected_area=abs(sum(cross(a,c) for a,c in zip(hull,hull[1:]+hull[:1])))*.5
 assert abs(top_area-expected_area)<.0001,('Footprint coverage changed',o.name,top_area,expected_area)
 gids={g.index for g in o.vertex_groups if 'Catenary canvas side' in g.name or 'Sewn multi-panel dome canopy' in g.name};roof_ids={v.index for v in o.data.vertices if any(a.group in gids and a.weight>.5 for a in v.groups)};roof,_,_=tree(o,roof_ids)
 for x in [-.3,0,.3]:
  for y in [-.45,0,.45]:
   p=o.matrix_world@Vector((x,y,5));a=roof.ray_cast(p,Vector((0,0,-1)),10)[0];c=bt.ray_cast(p,Vector((0,0,-1)),10)[0]
   assert a is not None and c is not None and 1.25<a.z-c.z<2.1,('Missing roof or misplaced floor',o.name);roof_floor_probes+=1
 checks.append(dict(name=o.name,closed_floor=True,actual_grid_probes=probes,max_support_offset_error_m=error,max_vertical_depth_error_m=depth_error,top_uv_probes=uv_probes,max_top_uv_error=local_uv_error,footprint_area_m2=expected_area,actual_top_area_m2=top_area,new_floor_triangles=row['new_floor_triangles']))
 total_probes+=probes;max_error=max(max_error,error);uv_error=max(uv_error,local_uv_error)

amenities=json.loads((OUT/'amenities.json').read_text(encoding='utf-8'));extra=[p['name'] for p in amenities['placements'] if p['name'].startswith('Additional pitched tent ')]
bag_contacts=0
for name,parent in [('west-rolled-bedding','west-pitched-ridge')]+[('Additional camp rolled bedding '+str(i),tent) for i,tent in enumerate(extra)]:
 o=bpy.data.objects[name];bag,_,_=tree(o)
 for x in [-.2,0,.2]:
  low=o.matrix_world@Vector((x,0,-.2));high=o.matrix_world@Vector((x,0,.6));a=bag.ray_cast(low,Vector((0,0,1)),1)[0];c=floor_by_name[parent].ray_cast(high,Vector((0,0,-1)),1)[0]
  assert a is not None and c is not None and -.006<a.z-c.z<.014,('Bedding lost actual new-floor support',name,x);bag_contacts+=1

# Exact raw oriented triangles are stronger than float BVH nearest-face probes,
# which can misreport distances on the original extremely thin canvas faces.
def canonical(tri):
 i=min(range(3),key=lambda i:tri[i]);return tuple(tri[i:]+tri[:i])
def raw_triangles(data):
 length=struct.unpack_from('<I',data,12)[0];g=json.loads(data[20:20+length]);start=28+length
 assert len(g['nodes'])==1 and all(k not in g['nodes'][0] for k in ['matrix','rotation','translation','scale']),'Fitted asset unexpectedly moved its native pivot'
 def value(accessor,index,count):
  a=g['accessors'][accessor];view=g['bufferViews'][a['bufferView']];code,size={5126:('f',4),5125:('I',4),5123:('H',2),5121:('B',1)}[a['componentType']]
  return struct.unpack_from('<'+code*count,data,start+view.get('byteOffset',0)+a.get('byteOffset',0)+index*view.get('byteStride',size*count))
 result=collections.Counter()
 for mesh in g['meshes']:
  for p in mesh['primitives']:
   for i in range(0,g['accessors'][p['indices']]['count'],3):
    tri=[value(p['attributes']['POSITION'],value(p['indices'],i+j,1)[0],3) for j in range(3)];result[canonical(tri)]+=1
 return result
imports=[]
for a in r['assets']:
 bpy.ops.wm.open_mainfile(filepath=str(ROOT/a['editable']));o=bpy.data.objects[a['id']];o.data.calc_loop_triangles()
 expected=collections.Counter(canonical([(o.data.vertices[i].co.x,o.data.vertices[i].co.z,-o.data.vertices[i].co.y) for i in t.vertices]) for t in o.data.loop_triangles)
 bpy.ops.wm.read_factory_settings(use_empty=True);file=ROOT/f"assets/runtime/models/{a['id']}.glb";bpy.ops.import_scene.gltf(filepath=str(file));points=[];faces=[]
 for o in bpy.context.scene.objects:
  if o.type!='MESH':continue
  v,t=triangles(o);offset=len(points);points+=v;faces +=[tuple(i+offset for i in tri.vertices) for tri in t]
  assert o.data.uv_layers and all(math.isfinite(c) for layer in o.data.uv_layers for v in layer.data for c in v.uv)
 assert len(faces)==a['triangles'] and len(faces)<50000 and file.stat().st_size<4000000
 actual=raw_triangles(file.read_bytes());assert expected==actual,('Raw source/GLB surfaces differ',a['id'],sum((expected-actual).values()),sum((actual-expected).values()))
 assert hashlib.sha256(file.read_bytes()).hexdigest()==a['sha256']
 imports.append(dict(id=a['id'],triangles=len(faces),bytes=file.stat().st_size,exact_raw_oriented_triangle_identity=True,native_pivot_preserved=True))
for p,h in r['protected_sources'].items():assert hashlib.sha256((ROOT/p).read_bytes()).hexdigest()==h,p
report=dict(status='pass-native-floor-and-eight-export-only',floor_count=len(checks),old_floor_triangles=r['old_floor_triangles'],new_floor_triangles=r['new_floor_triangles'],dense_actual_floor_probes=total_probes,max_terrain_offset_error_m=max_error,max_source_top_uv_error=uv_error,unchanged_other_meshes=len(old)-len(changed),all_nonfloor_parts_and_poses_unchanged=True,positive_roof_floor_rays=roof_floor_probes,bedding_floor_contact_rays=bag_contacts,floors=checks,actual_glb_reimports=imports,scene_sha256=r['scene_sha256'],runtime_fps_or_gameplay_acceptance=False)
(ROOT/'development/levels/verification/hollow-creek-fitted-floors.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
print('PASS8 closed terrain-clipped floors;',total_probes,'actual dense probes;8 exact raw source/GLB oriented triangle sets; no nonfloor rendered-triangle/pose changes',flush=True)
