"""Reopen and independently validate actual pavilion triangles and native placement."""
import bpy,bmesh,json,hashlib,math
from pathlib import Path
from mathutils import Vector
from mathutils.bvhtree import BVHTree
ROOT=Path(__file__).resolve().parents[2];OUT=ROOT/'development/levels/hollow-creek'
d=json.loads((OUT/'pavilion.json').read_text(encoding='utf-8'))
bpy.ops.wm.open_mainfile(filepath=str(ROOT/d['asset']['editable']))
obj=bpy.data.objects[d['asset']['id']];obj.data.calc_loop_triangles()
assert len(obj.data.loop_triangles)==d['asset']['triangles'];assert obj.data.uv_layers
assert all(im.packed_file for im in bpy.data.images if im.source=='FILE')
assert hashlib.sha256((ROOT/f"assets/runtime/models/{obj.name}.glb").read_bytes()).hexdigest()==d['asset']['sha256']
def part(name):
 group=obj.vertex_groups[name].index
 ids={v.index for v in obj.data.vertices if any(g.group==group and g.weight>.5 for g in v.groups)}
 faces=[tuple(f.vertices) for f in obj.data.polygons if all(i in ids for i in f.vertices)]
 vertices=[v.co.copy() for v in obj.data.vertices]
 return BVHTree.FromPolygons(vertices,faces,all_triangles=False),[vertices[i] for i in ids],ids,faces
cache={}
def data(name):
 if name not in cache:cache[name]=part(name)
 return cache[name]
for a,b in d['contacts']:
 ta,va,_,_=data(a);tb,vb,_,_=data(b)
 if not ta.overlap(tb):
  gap=min([ta.find_nearest(v)[3] for v in vb]+[tb.find_nearest(v)[3] for v in va])
  assert gap<.003,(a,b,'unsupported actual meshes',gap)
# Both enclosing architectural solids have no boundary or non-manifold edges.
for name in ['Slab closed solid','Roof closed solid']:
 tree,vertices,ids,faces=data(name);edges={}
 for face in faces:
  for i,j in zip(face,face[1:]+face[:1]):
   key=tuple(sorted((i,j)));edges[key]=edges.get(key,0)+1
 assert all(count==2 for count in edges.values()),name
roof=data('Roof closed solid')[0];samples=[]
for x in [-6.9,-5,-2,-.01,.01,2,5,6.9]:
 for y in [-6.9,0,6.9]:
  upper,un,_,_=roof.ray_cast(Vector((x,y,8)),Vector((0,0,-1)))
  lower,ln,_,_=roof.ray_cast(Vector((x,y,1)),Vector((0,0,1)))
  assert upper is not None and lower is not None and un.z>0 and ln.z<0
  assert abs(upper.z-lower.z-.12)<1e-5,(x,y,upper,lower)
  samples.append([x,y,round(upper.z-lower.z,5)])
slab=data('Slab closed solid')[0]
for x,y in [(-6.9,-6.9),(0,0),(6.9,6.9)]:
 top=slab.ray_cast(Vector((x,y,1)),Vector((0,0,-1)))[0]
 bottom=slab.ray_cast(Vector((x,y,-1)),Vector((0,0,1)))[0]
 assert abs(top.z-.18)<1e-5 and abs(bottom.z)<1e-5
# Per-part physical support checks above must pass before scene placement review.
bpy.ops.wm.open_mainfile(filepath=str(OUT/'hollow-creek-pavilion.blend'))
placed=bpy.data.objects[d['asset']['id']];assert (placed.location-Vector((32,-42,-.18))).length<1e-6
assert not any(o.name.startswith(('Wall pavilion','Header pavilion','Floor pavilion')) for o in bpy.context.scene.objects)
# Four apron rectangles and structural slab cover the original zone without coplanar overlaps.
rects=[(25,35,39,49)]
for o in bpy.context.scene.objects:
 if o.name.startswith('Pavilion apron '):
  assert abs(o.location.z+o.dimensions.z/2)<1e-6
  rects.append((o.location.x-o.dimensions.x/2,-o.location.y-o.dimensions.y/2,o.location.x+o.dimensions.x/2,-o.location.y+o.dimensions.y/2))
assert len(rects)==5
for ix in range(32):
 for iz in range(32):
  x,z=24+(ix+.5)*.5,34+(iz+.5)*.5
  assert sum(a<x<c and b<z<e for a,b,c,e in rects)==1,(x,z,'floor gap/overlap')
# All existing facility vertices and transforms remain identical to the previous additive scene.
prior=json.loads((OUT/'facilities.json').read_text(encoding='utf-8'))
current={p['id']:[tuple(v.co) for v in bpy.data.objects[p['native_object']].data.vertices] for p in prior['placements']}
for p in prior['placements']:
 o=bpy.data.objects[p['native_object']];assert (o.location-Vector((p['x'],-p['z'],p['y']))).length<1e-6
# Posts stay outside full-width planning corridors plus hunter radius.
layout=json.loads((ROOT/'development/levels/layouts/wld-02/layout.json').read_text(encoding='utf-8'))
def point_distance(p,a,b):
 dx=b[0]-a[0];dz=b[1]-a[1];n=dx*dx+dz*dz;t=max(0,min(1,((p[0]-a[0])*dx+(p[1]-a[1])*dz)/n)) if n else 0
 return math.hypot(p[0]-a[0]-dx*t,p[1]-a[1]-dz*t)
clearance=999
for post in d['posts']:
 center=(32+post['x'],42+post['z'])
 for link in layout['links']:
  for a,b in zip(link['points'],link['points'][1:]):
   # Circumscribed post-foot disk is conservative for the actual square shoe.
   gap=point_distance(center,(a[0],a[2]),(b[0],b[2]))-link['width']/2-.22-math.sqrt(2)*.22
   assert gap>.03,(post['id'],link['id'],gap);clearance=min(clearance,gap)
 # Keep central ritual reservation and table footprints separate from posts.
 assert not (abs(center[0]-32)<1.2+.22 and abs(center[1]-42)<1.2+.22)
 for p in prior['placements']:
  asset=next(a for a in prior['assets'] if a['id']==p['model']);lo,hi=asset['bounds']
  assert min(center[0]+.22,p['x']+hi[0])<=max(center[0]-.22,p['x']+lo[0]) or min(center[1]+.22,p['z']+hi[2])<=max(center[1]-.22,p['z']+lo[2])
bpy.ops.wm.open_mainfile(filepath=str(OUT/'hollow-creek-facilities.blend'))
for p in prior['placements']:assert current[p['id']]==[tuple(v.co) for v in bpy.data.objects[p['native_object']].data.vertices]
for path,digest in d['protected_sources'].items():assert hashlib.sha256((ROOT/path).read_bytes()).hexdigest()==digest,path
report=dict(status='pass-native-architecture-only',triangles=d['asset']['triangles'],actual_mesh_contacts=len(d['contacts']),roof_thickness_samples=samples,closed_roof_and_slab=True,standing_y=0,apron_cover_samples=1024,minimum_conservative_planning_route_clearance=round(clearance,4),prior_facilities_exact_vertices_and_transforms=True,protected_sources_unchanged=True,scene_sha256=hashlib.sha256((OUT/'hollow-creek-pavilion.blend').read_bytes()).hexdigest(),limitations=d['limitations'])
(ROOT/'development/levels/verification/hollow-creek-pavilion.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
print('PASS actual mesh support contacts, closed 120mm roof/180mm slab, 1024 gap-free floor samples, route/table clearances and preserved sources',flush=True)
