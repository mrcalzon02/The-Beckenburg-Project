"""Independently reopen actual saved dock scene and six actual GLB exports."""
import bpy,json,math,hashlib
from pathlib import Path
from mathutils import Vector
from mathutils.bvhtree import BVHTree
ROOT=Path(__file__).resolve().parents[2];OUT=ROOT/'development/levels/hollow-creek'
r=json.loads((OUT/'dock-details.json').read_text(encoding='utf-8'));d=json.loads((OUT/'organic-terrain.json').read_text(encoding='utf-8'))
bpy.ops.wm.open_mainfile(filepath=str(OUT/'hollow-creek-dock.blend'))
def tree(objects,group=None):
 vv=[];ff=[]
 for o in objects:
  o.data.calc_loop_triangles();n=len(vv);vv.extend(o.matrix_world@v.co for v in o.data.vertices);selected=None
  if group:
   ids={g.index for g in o.vertex_groups if group(g.name)};selected={v.index for v in o.data.vertices if any(g.group in ids for g in v.groups)}
  ff.extend(tuple(n+i for i in t.vertices) for t in o.data.loop_triangles if selected is None or all(i in selected for i in t.vertices))
 assert ff,'Missing actual component triangles';return BVHTree.FromPolygons(vv,ff,all_triangles=True)
scene=bpy.context.scene;terrain=bpy.data.objects['level_camp_terrain_shore'];ground=tree([terrain]);new=[bpy.data.objects[p['name']] for p in r['placements']];obstacles=tree(new+[bpy.data.objects['level_camp_dock_utilities']]);findings=[]
def check(ok,kind,**data):
 if not ok:findings.append(dict(kind=kind,**data))
def height(x,z):
 p=ground.ray_cast(Vector((x,-z,10)),Vector((0,0,-1)),40)[0];assert p is not None,(x,z);return p.z
# All retained original terrain triangles are unchanged and continuous across the
# repaired cell strip. This is a surface check, not accepted shore containment.
shore_samples=0
for i in range(273):
 for j in range(133):
  x=-3.9+i*.248;z=-15.9+j*.25
  p=ground.ray_cast(Vector((x,-z,10)),Vector((0,0,-1)),40)[0];shore_samples+=1;check(p is not None,'shore cell hole',x=x,z=z)
seams=0
for side in ['west','east']:
 row=r['shore_seam_vertices'][side];width=len(row);strip=bpy.data.objects['Continuous '+side+' forest lakeshore return'];v=[strip.matrix_world@v.co for v in strip.data.vertices]
 for a,c in zip(row,v[:width]):seams+=1;check((Vector(a)-c).length<.00001,'forest coastal seam mismatch',side=side)
 check(all(p.z<d['water_y']-.5 for p in v[-width:]),'shore strip does not terminate below water',side=side)
body_samples=0
for l in d['routes']:
 for a,b in zip(l['points'],l['points'][1:]):
  dx=b[0]-a[0];dz=b[2]-a[2];dist=math.hypot(dx,dz);n=max(1,math.ceil(dist/.25))
  for i in range(n+1):
   for side in [-1,0,1]:
    t=i/n;off=side*(l['width']/2+.22);x=a[0]+t*dx-dz/(dist or 1)*off;z=a[2]+t*dz+dx/(dist or 1)*off;y=a[1]+t*(b[1]-a[1])
    for h in [.28,.65,1.42]:body_samples+=1;check(obstacles.find_nearest(Vector((x,-z,y+h)),.23)[0] is None,'new furniture blocks protected route',link=l['id'],point=[x,y+h,z])
ritual_samples=0
for rite in d['rituals']:
 x,y,z=rite['point']
 for i in range(11):
  for j in range(11):ritual_samples+=1;check(obstacles.find_nearest(Vector((x-2.2+i*.44,-(z-2.2+j*.44),y+.3)),.23)[0] is None,'new furniture blocks ritual',rite=rite['rite'])
# Exact deck top supports corners of the shed floor and the feet of the generator.
deck=tree([bpy.data.objects['dock-foundation']]);shed=bpy.data.objects['Dock boat-service shed'];shedfloor=tree([shed],lambda n:'Service floor' in n);contacts=0
for x in [25.75,26.65,27.55]:
 for z in [-8.8,-7.7,-6.6]:
  p=deck.ray_cast(Vector((x,-z,0)),Vector((0,0,-1)),5)[0];q=shedfloor.ray_cast(Vector((x,-z,-2)),Vector((0,0,1)),3)[0];contacts+=1;check(p is not None and q is not None and abs(p.z-q.z)<.002,'shed floor support gap',x=x,z=z)
gen=bpy.data.objects['Dock reused generator'];lo=min((gen.matrix_world@v.co).z for v in gen.data.vertices);check(abs(lo-(-1.21))<.001,'generator floor pivot not supported')
chair_contacts=0
for p in r['placements']:
 if not p['asset'].endswith('folding_chair'):continue
 o=bpy.data.objects[p['name']]
 for v in o.data.vertices:
  w=o.matrix_world@v.co
  if v.co.z<.12 and any(abs(w.x-f[0])<.001 and abs(-w.y-f[1])<.001 for f in p['feet']):chair_contacts+=1;check(abs(w.z-height(w.x,-w.y))<.025,'chair leg not fitted',name=o.name)
# Full standing lane along the occupied dock; board reachable outside shed.
dock_samples=0
for i in range(131):
 for x in [28.30,28.95,29.60]:
  z=3.5-i*.10
  for h in [.25,.65,1.2,1.6]:dock_samples+=1;check(obstacles.find_nearest(Vector((x,-z,-1.3+h)),.22)[0] is None,'dock standing lane blocked',x=x,z=z,h=h)
for z in [-7.1,-7.4,-7.7]:
 for h in [.25,.65,1.2,1.6]:check(obstacles.find_nearest(Vector((28.15,-z,-1.3+h)),.22)[0] is None,'external breaker approach blocked',z=z,h=h)
# Real 1.8m standing body through doorway to front generator service space.
for i in range(22):
 z=-4.8-i*.1
 for h in [.25,.65,1.2,1.6]:check(obstacles.find_nearest(Vector((26.99,-z,-1.21+h)),.22)[0] is None,'shed service door approach blocked',z=z,h=h)
utility=bpy.data.objects['level_camp_dock_utilities'];globes=tree([utility],lambda n:'lamp globe' in n)
verts=[utility.matrix_world@v.co for v in utility.data.vertices if any(utility.vertex_groups[g.group].name.startswith('Non-emissive review lamp globe') for g in v.groups)];headroom=min(v.z for v in verts)+1.3;check(headroom>=2.48-.000001,'dock globe too low',headroom=headroom)
boat=bpy.data.objects['West moored rowing boat'];boatverts=[boat.matrix_world@v.co for v in boat.data.vertices];check(min(v.z for v in boatverts)<d['water_y']-.1 and max(v.z for v in boatverts)>d['water_y']+.3,'rowboat waterline fit')
for rel,h in r['protected_sources'].items():check(hashlib.sha256((ROOT/rel).read_bytes()).hexdigest()==h,'prior source changed',path=rel)
report=dict(status='pass-native-dock-detail-only' if not findings else 'refinement-required',shore_surface_samples=shore_samples,exact_forest_seam_vertices=seams,protected_route_body_samples=body_samples,ritual_samples=ritual_samples,shed_deck_contacts=contacts,chair_foot_vertices=chair_contacts,dock_standing_samples=dock_samples,actual_minimum_globe_headroom_m=headroom,findings=findings,limitations=r['limitations'])
file=ROOT/'development/levels/verification/hollow-creek-dock.json';file.write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
assert not findings,('Native dock/detail checks failed',len(findings),'see',file)
imports=[]
for a in r['assets']:
 path=ROOT/f"assets/runtime/models/{a['id']}.glb";assert hashlib.sha256(path.read_bytes()).hexdigest()==a['sha256'];assert path.stat().st_size<4000000;bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(path));tris=0
 for o in [o for o in bpy.context.scene.objects if o.type=='MESH']:
  o.data.calc_loop_triangles();tris+=len(o.data.loop_triangles);assert o.data.uv_layers;assert all(math.isfinite(c) for v in o.data.vertices for c in v.co);assert all(math.isfinite(c) for l in o.data.uv_layers for v in l.data for c in v.uv)
 assert tris==a['triangles'] and tris<50000;(imports.append(dict(id=a['id'],triangles=tris,bytes=path.stat().st_size)))
report['actual_glb_reimports']=imports;file.write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8');print('PASS',json.dumps({k:v for k,v in report.items() if isinstance(v,(int,float,str))}),flush=True)
