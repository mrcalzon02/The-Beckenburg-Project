"""Independent saved-scene support, boundary and protected-corridor checks.
Native land-edge acceptance only; not runtime water/whole-map containment.
"""
import bpy,json,hashlib,math,struct
from pathlib import Path
from mathutils import Vector
from mathutils.bvhtree import BVHTree
ROOT=Path(__file__).resolve().parents[2];OUT=ROOT/'development/levels/hollow-creek'
report=json.loads((OUT/'boundaries.json').read_text(encoding='utf-8'));d=json.loads((OUT/'organic-terrain.json').read_text(encoding='utf-8'))
bpy.ops.wm.open_mainfile(filepath=str(OUT/'hollow-creek-boundaries.blend'))
def tree(objects):
 vv=[];ff=[]
 for obj in objects:
  obj.data.calc_loop_triangles();base=len(vv);vv.extend(obj.matrix_world@v.co for v in obj.data.vertices);ff.extend(tuple(base+i for i in f.vertices) for f in obj.data.loop_triangles)
 return BVHTree.FromPolygons(vv,ff,all_triangles=True)
terrain=tree([bpy.data.objects['level_camp_terrain_organic']]);boundary_objects=[bpy.data.objects[a['id']] for a in report['assets']];body=tree(boundary_objects);banks=tree([bpy.data.objects['level_camp_boundary_banks']])
def ground(x,z):
 p=terrain.ray_cast(Vector((x,-z,20)),Vector((0,0,-1)),40)[0];assert p is not None,(x,z);return p.z
for rel,h in report['protected_sources'].items():assert hashlib.sha256((ROOT/rel).read_bytes()).hexdigest()==h,rel
for name,prior in report['prior_meshes'].items():
 obj=bpy.data.objects[name];assert len(obj.data.vertices)==prior['vertices'];assert all(abs(obj.matrix_world[i][j]-prior['matrix'][i][j])<1e-6 for i in range(4) for j in range(4));assert hashlib.sha256(b''.join(struct.pack('<fff',*v.co) for v in obj.data.vertices)).hexdigest()==prior['sha256'],name
for f in report['footings']:assert -.23<f['bottom']-ground(f['x'],f['z'])<-.17,f
contacts=0
for log in report['log_contacts']:
 for x,z,bottom in log['ends']:
  p=banks.ray_cast(Vector((x,-z,20)),Vector((0,0,-1)),40)[0];assert p is not None and -.6<bottom-p.z<.025,(log['name'],bottom,p);contacts+=1
failures=[];edge_rays=0;sweeps=0
for station in report['stations']:
 x,z=station['x'],station['z'];nx,nz=station['nx'],station['nz'];base=ground(x,z)
 for eye in [.35,.9,1.4]:
  if (station['path']=='west-woodland' and abs(x-5)<1e-6 and abs(z-12)<1e-6) or (station['path']=='east-service' and abs(x-54.8)<1e-6 and abs(z-6)<1e-6):continue # Deliberately open shoreline interfaces; exact end-cap coplanar rays are not land-span closure. Capsule contact is still tested below.
  start=Vector((x-nx*2,-(z-nz*2),base+eye));direction=Vector((nx,-nz,0));hit=body.ray_cast(start,direction,5)[0];edge_rays+=1
  if hit is None:failures.append(dict(kind='unblocked-native-land-ray',path=station['path'],point=[x,base+eye,z],height=eye))
 # Sample an upright capsule travelling laterally through the edge. This tests
 # static geometry contact; it is not the game's movement/terrain step solver.
 stopped=False
 for i in range(51):
  along=-2+i*.1;px=x+nx*along;pz=z+nz*along
  for level in [.28,.65,1.05,1.42]:
   nearest=body.find_nearest(Vector((px,-pz,base+level)),.28)
   if nearest[0] is not None:stopped=True;break
  if stopped:break
 sweeps+=1
 if not stopped:failures.append(dict(kind='unblocked-native-capsule-contact',path=station['path'],point=[x,base,z]))
for x in [28+i*.1 for i in range(81)]:
 for y in [.35,.9,1.4]:
  hit=body.ray_cast(Vector((x,-83,ground(x,85)+y)),Vector((0,-1,0)),4)[0];edge_rays+=1
  if hit is None:failures.append(dict(kind='unblocked-gate-ray',point=[x,ground(x,85)+y,85]))
clearance_samples=0;clearance_failures=[]
def clear(x,y,z,label):
 global clearance_samples
 for eye in [.32,.8,1.4]:
  nearest=body.find_nearest(Vector((x,-z,y+eye)),.32);clearance_samples+=1
  if nearest[0] is not None:clearance_failures.append(dict(owner=label,point=[x,y+eye,z]))
for link in d['routes']:
 for a,b in zip(link['points'],link['points'][1:]):
  dx,dz=b[0]-a[0],b[2]-a[2];length=math.hypot(dx,dz);n=max(1,math.ceil(length/.3))
  for i in range(n+1):
   t=i/n
   for band in [-1,0,1]:
    off=band*(link['width']/2+.22);x=a[0]+dx*t-dz/(length or 1)*off;z=a[2]+dz*t+dx/(length or 1)*off;y=a[1]+(b[1]-a[1])*t;clear(x,y,z,link['id'])
for rite in d['rituals']:
 x,y,z=rite['point']
 for i in range(11):
  for j in range(11):clear(x-2.2+i*.44,y,z-2.2+j*.44,rite['rite'])
for dx in [-2,0,2]:
 for dz in [-4,0,4]:
  p=d['staging']['point'];clear(p[0]+dx,p[1],p[2]+dz,'canonical van reserve')
out=dict(status='pass-native-land-only' if not failures and not clearance_failures else 'failed-native-boundary-review',edge_ray_samples=edge_rays,capsule_contact_crossings=sweeps,protected_corridor_body_samples=clearance_samples,grounded_footings=len(report['footings']),supported_log_ends=contacts,prior_meshes_unchanged=len(report['prior_meshes']),protected_source_files=len(report['protected_sources']),failures=failures,clearance_failures=clearance_failures,limitations=report['limitations'])
path=ROOT/'development/levels/verification/hollow-creek-boundaries.json';path.write_text(json.dumps(out,indent=2)+'\n',encoding='utf-8')
assert not failures and not clearance_failures,('Native boundary review failed',len(failures),len(clearance_failures),'see',path)
# Reload every real runtime export and check unit scale, finite UVs and budgets.
imports=[]
for asset in report['assets']:
 bpy.ops.wm.read_factory_settings(use_empty=True);file=ROOT/f"assets/runtime/models/{asset['id']}.glb";assert hashlib.sha256(file.read_bytes()).hexdigest()==asset['sha256']
 bpy.ops.import_scene.gltf(filepath=str(file));objects=[o for o in bpy.context.scene.objects if o.type=='MESH'];triangles=0
 for o in objects:
  o.data.calc_loop_triangles();triangles+=len(o.data.loop_triangles);assert o.data.uv_layers
  assert all(math.isfinite(v) for p in o.data.vertices for v in p.co)
  assert all(math.isfinite(v) for uv in o.data.uv_layers for loop in uv.data for v in loop.uv)
 assert triangles==asset['triangles'] and triangles<50000 and file.stat().st_size<4_000_000
 imports.append(dict(id=asset['id'],triangles=triangles,bytes=file.stat().st_size,finite_positions_and_uvs=True))
out['export_reimports']=imports;path.write_text(json.dumps(out,indent=2)+'\n',encoding='utf-8')
print('PASS native land boundaries:',edge_rays,'rays;',sweeps,'capsule contacts;',clearance_samples,'protected body samples;',len(imports),'actual GLB reimports',flush=True)
