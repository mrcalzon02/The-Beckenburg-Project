"""Saved triangle checks for campsite support, crouch access and prior routes.
Native asset/placement acceptance, not gameplay collision or map unlock.
"""
import bpy,json,math,hashlib,sys
from pathlib import Path
from mathutils import Vector
from mathutils.bvhtree import BVHTree
ROOT=Path(__file__).resolve().parents[2];OUT=ROOT/'development/levels/hollow-creek';sys.path.insert(0,str(ROOT/'development/levels'))
import hollow_creek_organic as organic
r=json.loads((OUT/'tents.json').read_text(encoding='utf-8'));d=json.loads((OUT/'organic-terrain.json').read_text(encoding='utf-8'))
bpy.ops.wm.open_mainfile(filepath=str(OUT/'hollow-creek-tents.blend'))
def tree(objects,group=None):
 vv=[];ff=[]
 for o in objects:
  o.data.calc_loop_triangles();base=len(vv);vv.extend(o.matrix_world@v.co for v in o.data.vertices)
  selected=None
  if group:
   ids={g.index for g in o.vertex_groups if group(g.name)};selected={v.index for v in o.data.vertices if any(a.group in ids for a in v.groups)}
  ff.extend(tuple(base+i for i in t.vertices) for t in o.data.loop_triangles if selected is None or all(i in selected for i in t.vertices))
 assert ff,('No matching actual faces',group)
 return BVHTree.FromPolygons(vv,ff,all_triangles=True)
terrain=tree([bpy.data.objects['level_camp_terrain_organic']]);objects=[bpy.data.objects[p['native_object']] for p in r['placements']];all_new=tree(objects)
approach_obstacles=tree(objects+[o for o in bpy.context.scene.objects if o.type=='MESH' and o.name.startswith('level_camp_boundary_')])
def ground(x,z):
 p=terrain.ray_cast(Vector((x,-z,10)),Vector((0,0,-1)),30)[0];assert p is not None,(x,z);return p.z
findings=[];route_samples=0
for link in d['routes']:
 for a,b in zip(link['points'],link['points'][1:]):
  dx=b[0]-a[0];dz=b[2]-a[2];length=math.hypot(dx,dz);count=max(1,math.ceil(length/.25))
  for i in range(count+1):
   t=i/count
   for side in [-1,0,1]:
    off=side*(link['width']/2+.22);x=a[0]+dx*t-dz/(length or 1)*off;z=a[2]+dz*t+dx/(length or 1)*off;y=a[1]+(b[1]-a[1])*t
    for height in [.28,.65,1.42]:
     route_samples+=1
     if all_new.find_nearest(Vector((x,-z,y+height)),.23)[0] is not None:findings.append(dict(kind='new model encroaches protected route',link=link['id'],point=[x,y+height,z]))
for rite in d['rituals']:
 x,y,z=rite['point']
 for i in range(11):
  for j in range(11):
   if all_new.find_nearest(Vector((x-2.2+i*.44,-(z-2.2+j*.44),y+.3)),.23)[0] is not None:findings.append(dict(kind='ritual footprint obstruction',rite=rite['rite']))
approach_samples=0;approach_surface_samples=0;maximum_approach_grade=0
for route in r['approaches']:
 for a,b in zip(route['points'],route['points'][1:]):
  distance=math.hypot(b[0]-a[0],b[2]-a[2]);count=max(1,math.ceil(distance/.10))
  for i in range(count+1):
   t=i/count
   for side in [-.3,0,.3]:
    dx=b[0]-a[0];dz=b[2]-a[2];x=a[0]+dx*t-dz/distance*side;z=a[2]+dz*t+dx/distance*side;y=ground(x,z)
    for level in [.26,.65,1.02,1.56]:
     approach_samples+=1
     if approach_obstacles.find_nearest(Vector((x,-z,y+level)),.22)[0] is not None:findings.append(dict(kind='standing tent approach blocked',id=route['id'],point=[x,y+level,z]))
floor_samples=0;door_samples=0;bed_samples=0
for p in r['placements'][:2]:
 o=bpy.data.objects[p['native_object']];floor=tree([o],lambda n:'groundsheet' in n.lower() or 'physical floor' in n.lower())
 for dx in [-.8,-.4,0,.4,.8]:
  for dz in [-.7,0,.7]:
   x=p['x']+dx;z=p['z']+dz;g=ground(x,z);hit,normal,_,_=floor.ray_cast(Vector((x,-z,g+.2)),Vector((0,0,-1)),.4);floor_samples+=1
   if hit is None or abs(hit.z-g-.028)>.006 or normal.z<.9:findings.append(dict(kind='tent floor unsupported',id=p['id'],x=x,z=z,ground=g,hit=list(hit) if hit else None))
 # True current posture: 1.05 m capsule with .22m radius, plus 20mm foot clearance.
 tent_and_bedding=tree([o]+[bpy.data.objects[b['native_object']] for b in r['placements'][2:] if b['id'].startswith('west' if p['id'].startswith('west') else 'east')])
 for i in range(41):
  z=p['z']+2.3-i*2.6/40;x=p['x'];floor_y=ground(x,z)+.028
  for level in [.24,.38,.52,.66,.83]:
   door_samples+=1;hit=tent_and_bedding.find_nearest(Vector((x,-z,floor_y+level)),.22)[0]
   if hit is not None:findings.append(dict(kind='crouch approach blocked',id=p['id'],point=[x,floor_y+level,z],nearest=list(hit)))
for p in r['placements'][:2]:
 o=bpy.data.objects[p['native_object']];check=tree([o])
 for dx in [-.18,0,.18]:
  for dz in [1.5,1.65,1.8]:
   x=p['x']+dx;z=p['z']+dz;y=ground(x,z)+.028
   for level in [.24,.38,.52,.66,.83]:
    door_samples+=1
    if check.find_nearest(Vector((x,-z,y+level)),.22)[0] is not None:findings.append(dict(kind='lateral crouch doorway clearance blocked',id=p['id'],point=[x,y+level,z]))
for route in r['approaches']:
 for a,b in zip(route['points'],route['points'][1:]):
  dx=b[0]-a[0];dz=b[2]-a[2];length=math.hypot(dx,dz);count=max(1,math.ceil(length/.2))
  for i in range(count+1):
   t=i/count
   for side in [-.55,0,.55]:
    x=a[0]+dx*t-dz/length*side;z=a[2]+dz*t+dx/length*side;hit,normal,_,_=terrain.ray_cast(Vector((x,-z,10)),Vector((0,0,-1)),30);approach_surface_samples+=1
    if hit is None:findings.append(dict(kind='unsupported approach tread',id=route['id'],point=[x,z]));continue
    grade=math.hypot(normal.x,normal.y)/normal.z;maximum_approach_grade=max(maximum_approach_grade,grade)
    if grade>.20:findings.append(dict(kind='approach grade too steep',id=route['id'],point=[x,hit.z,z],grade=grade))
for p in r['placements'][2:]:
 obj=bpy.data.objects[p['native_object']];parent=bpy.data.objects['west-pitched-ridge' if p['id'].startswith('west') else 'east-pitched-dome'];floor=tree([parent],lambda n:'groundsheet' in n.lower() or 'physical floor' in n.lower())
 offsets=[[-.21,0],[0,0],[.21,0]] if p['asset'].endswith('rolled') else [[x,z] for x in [-.28,0,.28] for z in [-.82,0,.82]]
 bedtree=tree([obj]);source_mat=tree([obj],lambda n:'Recent sleeping mat' in n) if p['asset']=='level_trace_bedroll' else None
 for dx,dz in offsets:
  cs=math.cos(p['yaw']);sn=math.sin(p['yaw']);x=p['x']+cs*dx-sn*dz;z=p['z']+sn*dx+cs*dz;g=ground(x,z);surface=floor.ray_cast(Vector((x,-z,g+.25)),Vector((0,0,-1)),.5)[0];bed_samples+=1
  if surface is None:findings.append(dict(kind='bedroll extends beyond actual groundsheet',id=p['id'],x=x,z=z));continue
  if source_mat:
   hit=source_mat.ray_cast(Vector((x,-z,g-.1)),Vector((0,0,1)),.4)[0]
   if hit is None or abs(hit.z-surface.z)>.012:findings.append(dict(kind='sleeping mat not supported',id=p['id'],point=[x,g,z],floor=surface.z,underside=hit.z if hit else None))
  else:
   hit=bedtree.ray_cast(Vector((x,-z,g-.1)),Vector((0,0,1)),.5)[0]
   if hit is None or not -.006<hit.z-surface.z<.014:findings.append(dict(kind='rolled bedding not supported',id=p['id'],x=x,z=z,floor=surface.z,underside=hit.z if hit else None))
for rel,h in r['source_hashes'].items():assert hashlib.sha256((ROOT/rel).read_bytes()).hexdigest()==h,rel
out=dict(status='pass-native-campsite-only' if not findings else 'native campsite fit refinement required',route_body_samples=route_samples,standing_approach_samples=approach_samples,approach_surface_samples=approach_surface_samples,max_approach_grade=maximum_approach_grade,tent_floor_samples=floor_samples,crouch_approach_samples=door_samples,bedding_contact_samples=bed_samples,removed_decorative_ferns=r['removed_decorative_ferns'],findings=findings,source_hashes_unchanged=True,limitations=r['limitations'])
path=ROOT/'development/levels/verification/hollow-creek-tents.json';path.write_text(json.dumps(out,indent=2)+'\n',encoding='utf-8')
assert not findings,('Native campsite placement failed',len(findings),'see',path)
shell_checks=[]
for a in r['assets'][:2]:
 bpy.ops.wm.open_mainfile(filepath=str(ROOT/a['editable']));obj=bpy.data.objects[a['id']]
 shell=tree([obj],lambda n:'Catenary canvas' in n or 'Sewn multi-panel dome' in n)
 p,n,_,_=shell.ray_cast(Vector((.6,0,4)),Vector((0,0,-1)),5);assert p is not None
 if n.z<0:n=-n
 q,_,_,_=shell.ray_cast(p-n*.0001,-n,.03);assert q is not None
 thickness=(p-q).length;assert .0075<thickness<.0085,(a['id'],thickness)
 shell_checks.append(dict(id=a['id'],measured_canvas_thickness_m=thickness))
out['actual_canvas_shell_checks']=shell_checks
imports=[]
for a in r['assets']:
 file=ROOT/f"assets/runtime/models/{a['id']}.glb";assert hashlib.sha256(file.read_bytes()).hexdigest()==a['sha256'];assert file.stat().st_size<4_000_000
 bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(file));meshes=[o for o in bpy.context.scene.objects if o.type=='MESH'];triangles=0
 for o in meshes:
  o.data.calc_loop_triangles();triangles+=len(o.data.loop_triangles);assert o.data.uv_layers
  assert all(math.isfinite(v) for p in o.data.vertices for v in p.co)
  assert all(math.isfinite(v) for layer in o.data.uv_layers for p in layer.data for v in p.uv)
 assert triangles==a['triangles'] and triangles<50000
 imports.append(dict(id=a['id'],triangles=triangles,bytes=file.stat().st_size,finite_geometry_and_uvs=True))
out['actual_glb_reimports']=imports;path.write_text(json.dumps(out,indent=2)+'\n',encoding='utf-8')
print('PASS native campsite:',route_samples,'route body samples,',floor_samples,'floor points,',door_samples,'crouch samples,',bed_samples,'bedding contacts; 3 actual GLB reimports',flush=True)
