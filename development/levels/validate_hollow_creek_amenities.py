"""Reopen native expansion; check actual faces, support, routes and blockers.
Does not substitute for actual player collision, AI, electrical or LAN tests.
"""
import bpy,json,math,hashlib
from pathlib import Path
from mathutils import Vector
from mathutils.bvhtree import BVHTree
ROOT=Path(__file__).resolve().parents[2];OUT=ROOT/'development/levels/hollow-creek'
r=json.loads((OUT/'amenities.json').read_text(encoding='utf-8'));d=json.loads((OUT/'organic-terrain.json').read_text(encoding='utf-8'));bpy.ops.wm.open_mainfile(filepath=str(OUT/'hollow-creek-amenities.blend'));scene=bpy.context.scene
def tree(objects,group=None):
 vv=[];ff=[];owners=[]
 for o in objects:
  o.data.calc_loop_triangles();off=len(vv);vv.extend(o.matrix_world@v.co for v in o.data.vertices);selected=None
  if group:
   ids={g.index for g in o.vertex_groups if group(g.name)};selected={v.index for v in o.data.vertices if any(g.group in ids for g in v.groups)}
  for t in o.data.loop_triangles:
   if selected is None or all(i in selected for i in t.vertices):ff.append(tuple(off+i for i in t.vertices));owners.append(o.name)
 assert ff,'Missing actual component faces';return BVHTree.FromPolygons(vv,ff,all_triangles=True),owners
terrain=tree([bpy.data.objects['level_camp_terrain_amenities']])[0];objects=[bpy.data.objects[p['name']] for p in r['placements']];obstacles,owners=tree(objects+[bpy.data.objects['level_camp_creek_engineering']]);findings=[]
def check(ok,kind,**data):
 if not ok:findings.append(dict(kind=kind,**data))
def height(x,z):
 p=terrain.ray_cast(Vector((x,-z,10)),Vector((0,0,-1)),40)[0];assert p is not None,(x,z);return p.z
route_samples=0
for l in d['routes']:
 for a,b in zip(l['points'],l['points'][1:]):
  dx=b[0]-a[0];dz=b[2]-a[2];length=math.hypot(dx,dz);count=max(1,math.ceil(length/.25))
  for i in range(count+1):
   for side in [-1,0,1]:
    t=i/count;off=side*(l['width']/2+.22);x=a[0]+dx*t-dz/(length or 1)*off;z=a[2]+dz*t+dx/(length or 1)*off;y=a[1]+(b[1]-a[1])*t
    for h in [.28,.65,1.42]:
     route_samples+=1;p,n,ix,dist=obstacles.find_nearest(Vector((x,-z,y+h)),.23);check(p is None,'new detail blocks protected route',link=l['id'],object=owners[ix] if ix is not None else None,point=[x,y+h,z])
ritual_samples=0
for rite in d['rituals']:
 x,y,z=rite['point']
 for i in range(11):
  for j in range(11):ritual_samples+=1;check(obstacles.find_nearest(Vector((x-2.2+i*.44,-(z-2.2+j*.44),y+.3)),.23)[0] is None,'new detail blocks ritual',rite=rite['rite'])
tent_floor=0;doors=0;bedding=0
tents=[p for p in r['placements'] if p['name'].startswith('Additional pitched tent ')];check(len(tents)==6,'six additional tents required')
for index,p in enumerate(tents):
 o=bpy.data.objects[p['name']];floor=tree([o],lambda n:'groundsheet' in n.lower() or 'physical floor' in n.lower())[0];body=tree([o])[0];cs=math.cos(p['yaw']);sn=math.sin(p['yaw'])
 def world(lx,ly):return p['x']+cs*lx-sn*ly,p['z']-sn*lx-cs*ly
 for dx in [-.7,0,.7]:
  for dy in [-.7,0,.7]:
   x,z=world(dx,dy);g=height(x,z);hit=floor.ray_cast(Vector((x,-z,g+.2)),Vector((0,0,-1)),.4)[0];tent_floor+=1;check(hit is not None and abs(hit.z-g-.028)<.006,'tent floor contact',name=o.name,x=x,z=z)
 for i in range(41):
  x,z=world(0,-2.3+i*2.5/40);g=height(x,z)+.028
  for h in [.24,.38,.52,.66,.83]:doors+=1;check(body.find_nearest(Vector((x,-z,g+h)),.22)[0] is None,'crouch tent entry blocked',name=o.name,x=x,z=z,h=h)
 roll=bpy.data.objects['Additional camp rolled bedding '+str(index)];rolltree=tree([roll])[0]
 for dx in [-.20,0,.20]:
  x=p['x']+.55+dx*cs;z=p['z']-dx*sn;g=height(x,z);a=floor.ray_cast(Vector((x,-z,g+.3)),Vector((0,0,-1)),.5)[0];b=rolltree.ray_cast(Vector((x,-z,g-.1)),Vector((0,0,1)),.7)[0];bedding+=1;check(a is not None and b is not None and -.006<b.z-a.z<.014,'additional roll support',name=roll.name,x=x,z=z)
support_checks=0;max_ramp_grade=0
for p in r['supports']:
 o=bpy.data.objects[p['name']];bt=tree([o])[0]
 if p.get('kind')=='ramp':
  for i in range(17):
   for dx in [-.4,0,.4]:
    z=p['front']+(p['back']-p['front'])*(i+.01)/16.02;point,n,_,_=bt.ray_cast(Vector((p['x']+dx,-z,10)),Vector((0,0,-1)),40);support_checks+=1;check(point is not None,'missing toilet ramp tread',name=o.name)
    if point is not None:
     grade=math.hypot(n.x,n.y)/n.z;max_ramp_grade=max(max_ramp_grade,grade);check(grade<=.20,'toilet entry ramp too steep',name=o.name,grade=grade)
 else:
  low=min((o.matrix_world@v.co).z for v in o.data.vertices);support_checks+=1;check(low<=height(p['x'],p['z'])+.015,'floating foundation support',name=o.name)
# Native barrier coverage tested against actual saved closed meshes, not names.
barrier_tree=tree([bpy.data.objects[b['id']] for b in r['blockers']])[0];barrier_rays=0
for b in r['blockers']:
 a,c=b['a'],b['b'];dx=c[0]-a[0];dz=c[1]-a[1];length=math.hypot(dx,dz)
 if length<.001:continue
 for t in [.05,.25,.5,.75,.95]:
  x=a[0]+dx*t;z=a[1]+dz*t;y=(b['bottom']+b['top'])/2;normal=Vector((-dz/length,-dx/length,0));origin=Vector((x,-z,y))+normal*.6;hit=barrier_tree.ray_cast(origin,-normal,1.2)[0];barrier_rays+=1;check(hit is not None,'native barrier ray gap',id=b['id'],t=t)
for b in r['blockers']:check(bpy.data.objects[b['id']].hide_render and b['top']>b['bottom'],'barrier hidden/height contract',id=b['id'])
# Standing routes cross real bridge and dock entrances without invisible blocks.
for x in [43+i*.2 for i in range(51)]:
 for z in [12.1,13,13.9]:check(barrier_tree.find_nearest(Vector((x,-z,.1)),.22)[0] is None,'native barrier blocks bridge',x=x,z=z)
for z in [2+i*.1 for i in range(41)]:check(barrier_tree.find_nearest(Vector((28,-z,-.2)),.22)[0] is None,'native barrier blocks dock entrance',z=z)
# Culvert centre bore is a real aperture; grate deliberately closes traversal.
cx,cy,cz=r['creek']['culvert_center'];engineering=bpy.data.objects['level_camp_creek_engineering'];barrel=tree([engineering],lambda n:'Open thick culvert barrel' in n)[0]
check(barrel.ray_cast(Vector((cx,-cz+.5,cy)),Vector((0,-1,0)),3.4)[0] is None,'culvert bore accidentally capped');check(r['creek']['terrain_faces_cut']>0,'buried culvert did not cut actual terrain skin')
culvert_sight_rays=0
for dx in [-.4,0,.4]:
 for h in [-.2,0,.3]:
  culvert_sight_rays+=1;check(terrain.ray_cast(Vector((cx+dx,-cz+.15,cy+h)),Vector((0,-1,0)),2.8)[0] is None,'terrain still plugs visible culvert bore',dx=dx,h=h)
check(bpy.data.objects.get('Creek water boundary') is None,'coplanar lake/creek water overlap remains')
check(len(r['proposed_refuges'])==r['shared_hiding_maximum']==3,'shared native hiding reservation budget')
check(all(p['asset'].startswith('level_camp_shrub_decorative_') for p in r['large_decorative_shrubs']),'ordinary planting reuses hiding shrub')
for p,h in r['protected_sources'].items():check(hashlib.sha256((ROOT/p).read_bytes()).hexdigest()==h,'prior source changed',path=p)
report=dict(status='pass-native-only' if not findings else 'refinement-required',protected_route_samples=route_samples,ritual_samples=ritual_samples,new_tent_floor_contacts=tent_floor,new_tent_crouch_samples=doors,new_roll_contacts=bedding,foundation_ramp_checks=support_checks,max_toilet_ramp_grade=max_ramp_grade,barrier_volumes=len(r['blockers']),actual_barrier_rays=barrier_rays,culvert_terrain_sight_rays=culvert_sight_rays,large_shrubs=len(r['large_decorative_shrubs']),total_pitched_tents=r['total_pitched_tents'],findings=findings,blocker_runtime_status=r['blocker_runtime_status'],limitations=r['limitations'])
path=ROOT/'development/levels/verification/hollow-creek-amenities.json';path.write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8');assert not findings,('Native expansion failed',len(findings),'see',path)
imports=[]
for a in r['assets']:
 file=ROOT/f"assets/runtime/models/{a['id']}.glb";assert hashlib.sha256(file.read_bytes()).hexdigest()==a['sha256'];bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(file));n=0
 for o in [o for o in bpy.context.scene.objects if o.type=='MESH']:
  o.data.calc_loop_triangles();n+=len(o.data.loop_triangles);assert o.data.uv_layers;assert all(math.isfinite(c) for v in o.data.vertices for c in v.co);assert all(math.isfinite(c) for l in o.data.uv_layers for v in l.data for c in v.uv)
 assert n==a['triangles'] and n<50000 and file.stat().st_size<4000000;imports.append(dict(id=a['id'],triangles=n,bytes=file.stat().st_size))
report['actual_glb_reimports']=imports;path.write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8');print('PASS',json.dumps({k:v for k,v in report.items() if isinstance(v,(int,float,str))}),flush=True)
