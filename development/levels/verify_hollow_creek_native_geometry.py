"""Independent current native triangle/closure audit and runtime query oracles.
Do not use Java support/LOS math to manufacture expected results.
"""
import bpy,json,struct,hashlib,math,collections,random
from pathlib import Path
from mathutils import Vector
from mathutils.bvhtree import BVHTree
ROOT=Path(__file__).resolve().parents[2];OUT=ROOT/'development/levels/hollow-creek';report=json.loads((ROOT/'development/levels/verification/hollow-creek-physics-export.json').read_text(encoding='utf-8'))
source=ROOT/report['source'];assert hashlib.sha256(source.read_bytes()).hexdigest()==report['source_sha256'];data=(ROOT/report['binary']).read_bytes();assert hashlib.sha256(data).hexdigest()==report['binary_sha256']
offset=0
def take(fmt):
 global offset
 n=struct.calcsize(fmt);v=struct.unpack_from(fmt,data,offset);offset+=n;return v
magic,nparts,ntri=take('>III');assert magic==0x48435332 and nparts==report['components'] and ntri==report['triangles'];parts=[];actual=collections.Counter();closed=0
def canonical(v):
 i=min(range(3),key=lambda i:v[i]);return tuple(v[i:]+v[:i])
for _ in range(nparts):
 n,=take('>H');name=data[offset:offset+n].decode('utf-8');offset+=n;volume,count=take('>BI');edges=collections.Counter();winding=collections.Counter();component=[]
 for j in range(count):
  values=take('>9fB');points=[tuple(values[i:i+3]) for i in [0,3,6]];actual[canonical(points)]+=1;component.append((points,values[-1]))
  for a,c in zip(points,points[1:]+points[:1]):key=tuple(sorted((a,c)));edges[key]+=1;winding[key]+=1 if a<c else -1
 if volume:
  assert all(v%2==0 and winding[k]==0 for k,v in edges.items()),('Closed component has unbalanced source edges',name);closed+=1
 parts.append((name,volume,component))
assert offset==len(data) and sum(len(p[2]) for p in parts)==ntri
bpy.ops.wm.open_mainfile(filepath=str(source));objects=[bpy.data.objects[r['object']] for r in report['objects']];names={o.name for o in objects}
required={'level_camp_terrain_amenities','level_camp_pavilion','level_camp_boundary_banks','level_camp_boundary_fence','level_camp_boundary_gate','dock-foundation','creek-crossing-foundation','west-pitched-ridge','east-pitched-dome'}|{'Additional pitched tent '+str(i) for i in range(1,7)};assert required<=names
expected=collections.Counter();vertices=[];faces=[];floor_vertices=[];floor_faces=[]
def intended(name):return any(t in name.lower() for t in ['groundsheet','physical floor','slab closed solid','service floor','raised cubicle floor','recent sleeping mat'])
for o in objects:
 o.data.calc_loop_triangles();vv=[o.matrix_world@v.co for v in o.data.vertices];base=len(vertices);vertices+=vv
 ids={g.index for g in o.vertex_groups if intended(g.name)};floor={v.index for v in o.data.vertices if any(a.group in ids and a.weight>.5 for a in v.groups)}
 entire=o.name in ['level_camp_terrain_amenities','dock-foundation','creek-crossing-foundation'] or o.name.startswith('Toilet supported entry ramp ') or o.name.endswith('laid-bedding')
 for t in o.data.loop_triangles:
  p=[vv[i] for i in t.vertices];normal=(p[1]-p[0]).cross(p[2]-p[0])
  if normal.length<1e-10:continue
  expected[canonical([(v.x,v.z,-v.y) for v in p])]+=1;faces.append(tuple(base+i for i in t.vertices))
  if (entire or all(i in floor for i in t.vertices)) and normal.z>0 and math.hypot(normal.x,normal.y)/normal.z<=.20+1e-5:floor_faces.append(tuple(base+i for i in t.vertices))
assert expected==actual,('Current source/binary triangles differ',sum((expected-actual).values()),sum((actual-expected).values()))
opaque=BVHTree.FromPolygons(vertices,faces,all_triangles=True);walking=BVHTree.FromPolygons(vertices,floor_faces,all_triangles=True);terrain=bpy.data.objects['level_camp_terrain_amenities'];terrain.data.calc_loop_triangles();ground=BVHTree.FromPolygons([terrain.matrix_world@v.co for v in terrain.data.vertices],[tuple(t.vertices) for t in terrain.data.loop_triangles],all_triangles=True)
lines=[];samples=[];expected_route_probes=0
d=json.loads((OUT/'organic-terrain.json').read_text(encoding='utf-8'))
for route in d['routes']:
 for a,c in zip(route['points'],route['points'][1:]):
  length=math.hypot(c[0]-a[0],c[2]-a[2]);steps=max(1,math.ceil(length/.3))
  for i in range(steps+1):
   expected_route_probes+=1
   t=i/steps;x=a[0]+(c[0]-a[0])*t;z=a[2]+(c[2]-a[2])*t
   hit=walking.ray_cast(Vector((x,-z,12)),Vector((0,0,-1)),30)[0]
   assert hit is not None,('Authored route has no walkable source triangle',route['id'],x,z)
   lines.append(f'S\t{x:.12g}\t{z:.12g}\t{hit.z:.12g}\t{route["id"]}');samples.append(Vector((x,hit.z+1.2,z)))
# Compare the actual fitted floor, not its roof or the old rough room elevation.
for name in ['west-pitched-ridge','east-pitched-dome']+['Additional pitched tent '+str(i) for i in range(1,7)]:
 o=bpy.data.objects[name]
 for dx in [-.45,0,.45]:
  for dy in [-.5,0,.5]:
   p=o.matrix_world@Vector((dx,dy,0));hit=walking.ray_cast(Vector((p.x,p.y,5)),Vector((0,0,-1)),20)[0];assert hit is not None;lines.append(f'S\t{p.x:.12g}\t{-p.y:.12g}\t{hit.z:.12g}\t{name}')
 tree=BVHTree.FromPolygons([o.matrix_world@v.co for v in o.data.vertices],[tuple(t.vertices) for t in o.data.loop_triangles],all_triangles=True);p=o.matrix_world@Vector((0,0,5));roof=tree.ray_cast(p,Vector((0,0,-1)),10)[0];assert roof is not None;lines.append(f'N\t{roof.x:.12g}\t{-roof.y:.12g}\t{roof.z:.12g}\t{name} roof is not a floor')
rng=random.Random(251025)
for i in range(450):
 a=rng.choice(samples);c=rng.choice(samples)
 if (a-c).length>39:continue
 start=Vector((a.x,-a.z,a.y));end=Vector((c.x,-c.z,c.y));delta=end-start;length=delta.length
 if length<.02:continue
 hit=opaque.ray_cast(start+delta.normalized()*.0001,delta.normalized(),max(0,length-.0002))[0];clear=hit is None
 lines.append('L\t'+'\t'.join(f'{v:.12g}' for p in [a,c] for v in p)+f'\t{int(clear)}')
# Deep bank interiors must be rejected even if no capsule surface touches a face.
bank_packet=json.loads((OUT/'grounded-berm.json').read_text(encoding='utf-8'));interiors=0
for path in bank_packet['stations']:
 for row in path['rows'][::7]:
  if row['peak_z']-row['ground_z']<1.8 or row['inside_m']<2.4:continue
  x,y=row['center'];g=ground.ray_cast(Vector((x,y,10)),Vector((0,0,-1)),30)[0]
  if g is None:continue
  lines.append(f'B\t{x:.12g}\t{g.z+.15:.12g}\t{-y:.12g}\t1.05');interiors+=1
assert len(samples)>500 and interiors>=10
file=ROOT/'development/levels/verification/hollow-creek-native-queries.tsv';file.write_text('\n'.join(lines)+'\n',encoding='utf-8')
result=dict(status='pass-current-native-triangle-and-closure-audit',source=report['source'],source_sha256=report['source_sha256'],binary_sha256=report['binary_sha256'],exact_raw_native_triangles=ntri,balanced_closed_components=closed,required_native_objects=sorted(required),oracle_counts=dict(collections.Counter(line[0] for line in lines)),oracle=file.relative_to(ROOT).as_posix(),gameplay_or_level_unlock=False)
(ROOT/'development/levels/verification/hollow-creek-native-source.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8');print('NATIVE QUERY ORACLE',result['oracle_counts'],ntri,'exact actual source triangles',flush=True)
