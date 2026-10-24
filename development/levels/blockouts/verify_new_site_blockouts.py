"""Independent native/GLB inspection for six selected rough proposals; never rebuild old maps.
Run with Blender --background --factory-startup --python-exit-code 1 --python this_file.
"""
from pathlib import Path
import sys,json,math,hashlib
import bpy
from mathutils import Vector
from mathutils.bvhtree import BVHTree
ROOT=Path(__file__).resolve().parents[3];LEVELS=ROOT/'development/levels';BLOCKS=LEVELS/'blockouts'
sys.path.insert(0,str(LEVELS/'layouts'))
from validate_layouts import validate
KEYS=['wld-14','wld-15','wld-16','wld-17','wld-18','wld-07-mortuary']
def tree(objects):
 vv=[];ff=[]
 for o in objects:
  m=o.data;m.calc_loop_triangles();base=len(vv);vv.extend(o.matrix_world@v.co for v in m.vertices);ff.extend(tuple(base+i for i in t.vertices) for t in m.loop_triangles)
 return BVHTree.FromPolygons(vv,ff,all_triangles=True)
def finite(mesh):return all(math.isfinite(v) for p in mesh.vertices for v in p.co)
results=[];reports=[]
for key in KEYS:
 p=(LEVELS/'layout-expansions' if key.endswith('mortuary') else LEVELS/'layouts')/key/'layout.json';data=json.loads(p.read_text(encoding='utf-8'));count=validate(data)
 folder=BLOCKS/key;report=json.loads((folder/'implementation.json').read_text(encoding='utf-8'));reports.append(report)
 bpy.ops.wm.open_mainfile(filepath=str(folder/'rough-shell.blend'));scene=bpy.context.scene
 assert scene['source_sha256']==hashlib.sha256(p.read_bytes()).hexdigest()
 objs=[o for o in scene.objects if o.type=='MESH'];assert all(finite(o.data) for o in objs)
 floors=[o for o in objs if o.get('zone') and o.name.startswith('Floor ')]
 for o in floors:
  zs=[(o.matrix_world@v.co).z for v in o.data.vertices];assert abs(max(zs)-min(zs)-.16)<.0001,(key,o.name)
 terrain=next(o for o in objs if o.name.startswith('Authored '));tt=tree([terrain]);ranges=[v.co.z for v in terrain.data.vertices]
 assert max(ranges)-min(ranges)>.2
 assert all(p.normal.z>0 for p in terrain.data.polygons),(key,'downward terrain')
 # All shaped upper floors are physically annular; centre rays must pass their void.
 shaped=[]
 for zone in data['zones']:
  if not zone.get('shape'):continue
  parts=[o for o in floors if o.get('zone')==zone['id']];assert parts
  st=tree(parts);a,b,c,d=zone['bounds'];cx=(a+c)/2;cz=(b+d)/2;y=zone['y']
  if zone.get('inner_radius') or y>0:
   hit=st.ray_cast(Vector((cx,-cz,y+.4)),Vector((0,0,-1)),.8)[0];assert hit is None,(key,zone['id'],'filled central void')
  rad=(zone['radius']+zone.get('inner_radius',1 if y>0 else 0))/2
  hit,normal,_,_=st.ray_cast(Vector((cx+rad,-cz,y+.4)),Vector((0,0,-1)),.8)
  assert hit and abs(hit.z-y)<.0001 and normal.z>.99,(key,zone['id'],'floor face')
  shaped.append(zone['id'])
 seams=[]
 if key=='wld-14':
  zs=[z for z in data['zones'] if z['id'].startswith('tower')]
  for lo,hi in zip(zs,zs[1:]):assert abs(lo['y']+lo['height']-hi['y'])<1e-6;seams.append([lo['id'],hi['id']])
 if key=='wld-18':
  shaft=next(o for o in objs if o.name.startswith('EMPTY SHAFT'));hit=tree([shaft]).ray_cast(Vector((32,-28,2)),Vector((0,0,-1)),24)[0];assert hit and abs(hit.z+16)<.001
  assert tt.ray_cast(Vector((32,-28,3)),Vector((0,0,-1)),30)[0] is None,'terrain capped empty silo'
 diagnostics=[];samples=0
 outside={z['id'] for z in data['zones'] if z['kind']=='outside' and z['y']>=0 and not z.get('shape')}
 for l in data['links']:
  if l['kind']!='level' or not {l['a'],l['b']}<=outside:continue
  for p0,p1 in zip(l['points'],l['points'][1:]):
   dx=p1[0]-p0[0];dz=p1[2]-p0[2];dist=math.hypot(dx,dz)
   if dist<.1:continue
   for i in range(math.ceil(dist)+1):
    t=i/math.ceil(dist);x=p0[0]+dx*t;z=p0[2]+dz*t;y=p0[1];samples+=1
    hit,n,_,_=tt.ray_cast(Vector((x,-z,y+20)),Vector((0,0,-1)),60)
    if not hit or abs(hit.z-y)>.22 or n.z<.98:diagnostics.append(dict(link=l['id'],point=[x,y,z],terrain_y=hit.z if hit else None,normal_z=n.z if n else None,note='Diagnostic only; decks/route strips may support this point. Actual surface ownership and swept path remain open.'))
 # Keep a second actual native cutaway for stacked rooms; no new false 'runtime' label.
 scene.world=bpy.data.worlds.new('Neutral native review');scene.world.use_nodes=True;scene.world.node_tree.nodes['Background'].inputs[1].default_value=.75
 bpy.ops.object.light_add(type='SUN');bpy.context.object.rotation_euler=(.5,-.4,-.5);bpy.context.object.data.energy=2
 a,b,c,d=data['site_bounds'];span=max(c-a,d-b);center=Vector(((a+c)/2,-(b+d)/2,0))
 bpy.ops.object.camera_add(location=center+Vector((span*.85,-span*.95,span*1.2)));cam=bpy.context.object;cam.rotation_euler=(center-cam.location).to_track_quat('-Z','Y').to_euler();cam.data.type='ORTHO';cam.data.ortho_scale=span*1.5;scene.camera=cam
 low=min(z['y'] for z in data['zones']);layer=low if low<0 else max(z['y'] for z in data['zones'])
 for o in list(scene.objects):
  if o.type not in ('MESH','FONT'):continue
  zone=next((z for z in data['zones'] if z['id']==o.get('zone')),None)
  # Hide terrain, diagram marks, and upper surfaces. Retain relevant lower shell and routes.
  if o==terrain or o.name.startswith(('WATER','EMPTY SHAFT','Pointed retired')) or o.type=='FONT':o.hide_render=True
  elif o.type=='MESH':
   bottom=min((o.matrix_world@v.co).z for v in o.data.vertices)
   if bottom>layer+.12:o.hide_render=True
 scene.render.engine='CYCLES';scene.cycles.samples=4;scene.cycles.use_denoising=True;scene.render.resolution_x=960;scene.render.resolution_y=720;scene.render.resolution_percentage=100;scene.view_settings.view_transform='Standard';scene.render.image_settings.file_format='PNG';scene.render.filepath=str(folder/'layer-study-native-fitout.png');bpy.ops.render.render(write_still=True)
 # Independently import actual exported GLB, rather than trusting export success.
 expected_triangles=sum(len(o.data.loop_triangles) for o in objs)
 bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(folder/'rough-shell.glb'))
 loaded=[o for o in bpy.context.scene.objects if o.type=='MESH'];triangles=0
 for o in loaded:o.data.calc_loop_triangles();assert finite(o.data);triangles+=len(o.data.loop_triangles)
 assert loaded and triangles>0 and all(len(o.data.uv_layers)>0 for o in loaded)
 assert all(math.isfinite(v) for o in loaded for layer in o.data.uv_layers for loop in layer.data for v in loop.uv)
 results.append(dict(id=data['id'],planning_checks=count,native_meshes=len(objs),floor_parts_160mm=len(floors),terrain_range=[min(ranges),max(ranges)],terrain_upward_faces=True,shaped_floor_voids=shaped,tower_wall_joins=seams,glb_meshes=len(loaded),glb_triangles=triangles,glb_uvs_finite_positions=True,outdoor_terrain_diagnostic_samples=samples,outdoor_terrain_findings=diagnostics,limitations=['Native coarse terrain and shell only; no gameplay/collision unlock.','Colored supports are diagram reservations, not engineered stairs/gangways.','Terrain diagnostics do not account for route strips/deck surfaces or establish walkability.','Lighting joins, watertight hull, guards, furnishings and runtime acceptance pending.']))
 print('NATIVE_VERIFIED',data['id'],len(loaded),triangles,flush=True)
old=json.loads((LEVELS/'verification/new-location-planning.json').read_text(encoding='utf-8'))
for rel,h in old['protected_originals'].items():assert hashlib.sha256((ROOT/rel).read_bytes()).hexdigest()==h,rel
original=json.loads((LEVELS/'layouts/wld-07/layout.json').read_text(encoding='utf-8'));expanded=json.loads((LEVELS/'layout-expansions/wld-07-mortuary/layout.json').read_text(encoding='utf-8'))
for k in ['zones','links','rituals']:
 assert expanded[k][:len(original[k])]==original[k],k
out=dict(status='native rough proposal checks pass; runtime unaccepted',date='2026-10-23',maps=results,protected_original_count=len(old['protected_originals']),original_cemetery_spaces_links_rites_unchanged=True)
(LEVELS/'verification/new-location-native.json').write_text(json.dumps(out,indent=2)+'\n',encoding='utf-8')
(BLOCKS/'selected-manifest.json').write_text(json.dumps(dict(status='six selected additional proposals',maps=reports),indent=2)+'\n',encoding='utf-8')
print('ALL_SIX_NATIVE_CHECKED; protected original files:',len(old['protected_originals']),flush=True)
