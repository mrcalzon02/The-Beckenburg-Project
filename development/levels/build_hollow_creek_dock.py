"""Additive dock/shore and campsite detail checkpoint; preserves earlier scenes.
Native metre coordinates, original reusable assets, no runtime map unlock.
"""
import bpy,sys,math,json,hashlib
from pathlib import Path
from mathutils import Vector
from mathutils.bvhtree import BVHTree
ROOT=Path(__file__).resolve().parents[2];sys.path[:0]=[str(ROOT/'development/levels'),str(ROOT/'development/assets')]
import rebuild_support as s
import build_lived_assets as b
import hollow_creek_organic as organic
import hollow_creek_ground as old
OUT=ROOT/'development/levels/hollow-creek';PRE=ROOT/'development/levels/previews/hollow-creek'

def shed():
 m=b.init();p=[]
 # 90mm continuous timber walls and a fully closed pitched roof/soffit.
 p.append(s.box('Service floor',(0,0,.045),(2,2.4,.09),m['oak']))
 for x in [-.955,.955]:
  p.append(s.box('Vent wall lower panel',(x,0,.775),(.09,2.4,1.37),m['pine']))
  p.append(s.box('Vent wall header',(x,0,2.07),(.09,2.4,.20),m['pine']))
  for y,w in [(-.50,1.4),(1.05,.3)]:p.append(s.box('Vent wall side panel',(x,y,1.715),(.09,w,.51),m['pine']))
 p.append(s.box('Solid rear wall',(0,1.155,1.13),(1.82,.09,2.08),m['pine']))
 for x,w in [(-.665,.49),(.855,.11)]:p.append(s.box('Front door jamb wall',(x,-1.155,1.13),(w,.09,2.08),m['pine']))
 p.append(s.box('Door head',(0.34,-1.155,2.12),(1.02,.09,.10),m['pine']))
 # Door opening -0.42..0.80: useful clear 1.22m, no decorative door across it.
 p.append(s.box('Open door against inside left wall',(-.89,-.48,1.08),(.05,1.18,1.96),m['blue']))
 for y in [-1.16,1.16]:
  p.append(s.mesh('Closed gable', [(-1,y,2.17),(1,y,2.17),(0,y,2.72),(-1,y+.045,2.17),(1,y+.045,2.17),(0,y+.045,2.72)],[(0,2,1),(3,4,5),(0,1,4,3),(1,2,5,4),(2,0,3,5)],m['pine']))
 for side in [-1,1]:
  vv=[(0,-1.34,2.72),(side*1.16,-1.34,2.10),(side*1.16,1.34,2.10),(0,1.34,2.72)];vv +=[(x,y,z-.12) for x,y,z in vv]
  p.append(s.mesh('120mm closed roof slope',vv,[(0,1,2,3),(7,6,5,4),(0,4,5,1),(1,5,6,2),(2,6,7,3),(3,7,4,0)],m['iron']))
 for x in [-.97,.97]:
  for y in [-1.18,1.18]:p.append(s.box('Corner structural post',(x,y,1.04),(.10,.10,2.08),m['oak']))
 for x in [-1.007,1.007]:
  for z in [1.62,1.70,1.78,1.86]:p.append(s.box('Vent louvre',(x,.55,z),(.04,.6,.035),m['steel']))
 # Actual outdoor weatherproof board, handle, supported conduit and exhaust.
 p.append(s.box('External reachable distribution board',(1.05,-.30,1.28),(.12,.48,.62),m['enamel'],.018))
 p.append(s.box('Main breaker handle',(1.125,-.30,1.30),(.045,.08,.19),m['red']))
 p.append(s.rod('Board continuous conduit',(1.02,-.30,.99),(1.02,-.30,.13),.025,m['steel'],8))
 p.append(s.rod('Exhaust bulkhead collar',(-.995,.62,.73),(-1.08,.62,.73),.10,m['steel'],12))
 p.append(s.rod('External exhaust rise',(-1.08,.62,.73),(-1.08,.62,2.30),.044,m['iron'],10))
 p.append(s.box('Exhaust rain hood',(-1.08,.62,2.33),(.24,.24,.045),m['iron']))
 b.export('level_camp_dock_shed',p,'campground',view=(5,-7,4))

def boat():
 m=b.init();p=[];vv=[];ff=[];stations=25;sections=13
 # Continuous external and internal hull joined at gunwale: physical 45mm shell.
 for inner in [False,True]:
  for j in range(stations):
   t=-1+2*j/(stations-1);width=.12+.68*math.sin((t+1)*math.pi/2)**.72
   for k in range(sections):
    a=-math.pi/2+k*math.pi/(sections-1);x=width*math.sin(a);z=.12+.47*(1-math.cos(a))+.14*abs(t)**3
    if inner:x*=.94;z+=.045
    vv.append((x,t*1.75,z))
 n=stations*sections
 for layer in range(2):
  off=layer*n
  for j in range(stations-1):
   for k in range(sections-1):
    a=off+j*sections+k;face=(a,a+1,a+sections+1,a+sections);ff.append(face if layer==0 else face[::-1])
 for j in range(stations-1):
  for k in [0,sections-1]:a=j*sections+k;c=(j+1)*sections+k;ff.append((a,c,c+n,a+n))
 for j in [0,stations-1]:
  for k in range(sections-1):a=j*sections+k;ff.append((a,a+n,a+n+1,a+1))
  outer=tuple(j*sections+k for k in range(sections));inner=tuple(n+j*sections+k for k in range(sections))
  ff.extend([outer if j==0 else outer[::-1],inner[::-1] if j==0 else inner])
 p.append(s.mesh('Continuous double-skin hull and gunwale',vv,ff,m['blue']))
 for y in [-.95,0,.95]:
  width=.12+.68*math.sin((y/1.75+1)*math.pi/2)**.72
  p.append(s.box('Supported rowing thwart',(0,y,.51),(width*1.92,.24,.055),m['pine']))
  for x in [-width*.77,width*.77]:p.append(s.box('Thwart side bracket',(x,y,.43),(.07,.18,.16),m['oak']))
 for x in [-.33,.33]:
  p.append(s.rod('Stowed oar shaft',(x,-1.1,.57),(x,1.15,.57),.022,m['pine'],10))
  p.append(s.box('Stowed oar blade',(x,1.32,.57),(.16,.42,.035),m['pine'],.018))
 for y in [-1.67,1.67]:p+=b.ring('Mooring ring',(0,y,.62),.07,.012,m['steel'],axis='Z',segments=12)
 b.export('level_camp_rowboat',p,'campground',view=(4,-6,4))

def chair():
 m=b.init();p=[]
 for x in [-.28,.28]:
  for sign in [-1,1]:p.append(s.rod('Crossed folding leg',(x,sign*.31,0),(x,-sign*.27,.48),.018,m['steel'],10))
  p.append(s.rod('Back support',(x,.25,.42),(x,.32,.91),.018,m['steel'],10))
  p.append(s.rod('Arm support',(x,-.24,.38),(x,-.24,.65),.016,m['steel'],8))
  p.append(s.box('Fabric arm rest',(x,0,.65),(.085,.54,.035),m['green'],.012))
 p.append(s.box('Tensioned fabric seat',(0,0,.47),(.54,.53,.025),m['green']))
 back=s.box('Fabric backrest',(0,.29,.72),(.54,.027,.35),m['green']);back.rotation_euler.x=-.14;p.append(back)
 b.export('level_camp_folding_chair',p,'campground',view=(2,-3,2))

def cooler():
 m=b.init();p=[s.box('Insulated cooler body',(0,0,.20),(.62,.38,.40),m['blue'],.035),s.box('Separate fitted lid',(0,0,.425),(.65,.41,.065),m['enamel'],.025)]
 for x in [-.34,.34]:p.append(s.box('Side carry handle',(x,0,.30),(.05,.20,.045),m['steel'],.012))
 p.append(s.box('Front latch',(0,-.21,.36),(.07,.028,.15),m['steel'],.008))
 b.export('level_camp_cooler',p,'campground',view=(2,-3,2))

for fn in [shed,boat,chair,cooler]:fn()
assets=list(b.LOG[-4:]);source=OUT/'hollow-creek-tents.blend'
protected={p.relative_to(ROOT).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in [source,ROOT/'assets/runtime/models/level_trace_generator.glb',OUT/'organic-terrain.json']}
bpy.ops.wm.open_mainfile(filepath=str(source));bpy.context.preferences.filepaths.save_version=0;scene=bpy.context.scene
d=json.loads((OUT/'organic-terrain.json').read_text(encoding='utf-8'));terrain=bpy.data.objects['level_camp_terrain_organic'];terrain.data=terrain.data.copy()
# Repair only missing cells in the coast-to-land union. Existing route vertices,
# UVs and triangle positions remain bit-identical; no campsite pad is levelled.
coast=[[-4,-16],[64,-16],[64,8],[59,17],[58,17],[56.5,9],[54.8,6],[45,2],[34,1],[23,3],[14,8],[5,12],[-4,12]]
vv=[tuple(v.co) for v in terrain.data.vertices];ff=[tuple(p.vertices) for p in terrain.data.polygons];old_faces=len(ff);existing={tuple(sorted(f)) for f in ff};nx=len(d['xs'])
for j in range(len(d['zs'])-1):
 for i in range(nx-1):
  x=(d['xs'][i]+d['xs'][i+1])/2;z=(d['zs'][j]+d['zs'][j+1])/2
  # The entire narrow coast strip joins the forest apron underneath. Culling
  # against the old land polygon here reintroduced the same diagonal notch.
  if not (-4<x<64 and z<18):continue
  a=j*nx+i
  for f in [(a,a+nx+1,a+1),(a,a+nx,a+nx+1)]:
   if tuple(sorted(f)) not in existing:ff.append(f)
oldmesh=terrain.data;mesh=bpy.data.meshes.new('Continuous repaired shore terrain');mesh.from_pydata(vv,[],ff);mesh.update()
for m in oldmesh.materials:mesh.materials.append(m)
uv=mesh.uv_layers.new(name='Preserved site UV')
for p in mesh.polygons:
 p.material_index=oldmesh.polygons[p.index].material_index if p.index<old_faces else (1 if p.center.x>=30 else 0)
 for li in p.loop_indices:
  v=mesh.vertices[mesh.loops[li].vertex_index].co;uv.data[li].uv=((v.x-d['bounds'][0])/(d['bounds'][2]-d['bounds'][0]),(v.y+d['bounds'][3])/(d['bounds'][3]-d['bounds'][1]))
terrain.data=mesh
# Continue both exact forest-apron end rows into submerged beds. At the land end
# each vertex is shared spatially with the existing apron; at lake end below water.
shore_strips=[];seam_contract={};shore_ground=BVHTree.FromPolygons([terrain.matrix_world@v.co for v in terrain.data.vertices],[tuple(p.vertices) for p in terrain.data.polygons])
for side in ['west','east']:
 apron=bpy.data.objects['Non-playable '+side+' forest terrain apron'];row=[apron.matrix_world@v.co for v in (list(apron.data.vertices)[:7] if side=='west' else list(apron.data.vertices)[-7:])];v=[];f=[]
 if side=='west':
  # The playable grid already owns x>=-4. Continue only the outer apron;
  # overlapping a long linear ramp here made a projecting triangular ledge.
  edge=row[2].lerp(row[3],(-4-row[2].x)/(row[3].x-row[2].x));row=[edge]+row[3:]
 width=len(row);seam_contract[side]=[list(p) for p in row]
 for j in range(49):
  t=j/48
  for k,p in enumerate(row):
   y=p.y+(45+p.z)*t;linear=p.z*(1-t)-4.2*t;target=organic.height(p.x,-y,d['routes']);w=old.smooth(t/.12);h=linear*(1-w)+target*w
   if side=='west' and k==0 and t>0:
    hit=shore_ground.ray_cast(Vector((p.x,y,10)),Vector((0,0,-1)),40)[0]
    if hit is not None:h=hit.z
   v.append((p.x,y,h))
  if j:
   for i in range(width-1):
    a=(j-1)*width+i;faces=[(a,a+width,a+width+1),(a,a+width+1,a+1)];f.extend(faces if side=='west' else [q[::-1] for q in faces])
 o=s.mesh('Continuous '+side+' forest lakeshore return',v,f,apron.data.materials[0]);o['role']='Scenery shoreline seam closure, not accepted playable containment';shore_strips.append(o.name)
# Finite lake preview now extends under scenery shoreline returns instead of
# ending at the playable terrain crop. Actual layered water remains a runtime gate.
lake=bpy.data.objects['Lake water boundary'];lake.dimensions=(250,150,.04);lake.location=(20,45,-1.97);bpy.context.view_layer.update()
lake.data=lake.data.copy()
for loop in lake.data.loops:
 p=lake.matrix_world@lake.data.vertices[loop.vertex_index].co;lake.data.uv_layers.active.data[loop.index].uv=(p.x*.04,p.y*.04)
# Export additive repaired world-space terrain; retain older approved source.
terrain.name='level_camp_terrain_shore';bpy.ops.object.select_all(action='DESELECT');terrain.select_set(True);bpy.context.view_layer.objects.active=terrain
path=ROOT/'assets/runtime/models/level_camp_terrain_shore.glb';bpy.ops.export_scene.gltf(filepath=str(path),export_format='GLB',use_selection=True,export_yup=True)
bpy.data.libraries.write(str(ROOT/'development/levels/blender/level_camp_terrain_shore.blend'),{terrain},compress=True);mesh.calc_loop_triangles();assets.append(dict(id=terrain.name,editable='development/levels/blender/level_camp_terrain_shore.blend',triangles=len(mesh.loop_triangles),sha256=hashlib.sha256(path.read_bytes()).hexdigest(),bytes=path.stat().st_size))
ground=BVHTree.FromPolygons([terrain.matrix_world@v.co for v in terrain.data.vertices],[tuple(p.vertices) for p in terrain.data.polygons])
def height(x,z):
 p=ground.ray_cast(Vector((x,-z,10)),Vector((0,0,-1)),40)[0];assert p is not None,(x,z);return p.z
placements=[]
def place(id,name,x,z,y,yaw=0):
 with bpy.data.libraries.load(str(ROOT/f'development/levels/blender/{id}.blend'),link=False) as (src,dst):dst.objects=[id]
 o=dst.objects[0];scene.collection.objects.link(o);o.name=name;o.location=(x,-z,y);o.rotation_euler.z=yaw;o['role']='Native detail placement, runtime integration pending';placements.append(dict(asset=id,name=name,x=x,z=z,y=y,yaw=yaw));return o
# Leave 2.8m of the existing 5m dock completely clear beside the service shed.
place('level_camp_dock_shed','Dock boat-service shed',26.65,-7.7,-1.30)
# Generator source is 1.55 x .88 x1.155m, preserved exactly; fits back service bay.
place('level_trace_generator','Dock reused generator',26.65,-8.13,-1.21)
place('level_camp_rowboat','West moored rowing boat',23.9,-5,-2.25)
m={k:s.material('Dock '+k,c,kind) for k,c,kind in [('wood',(.25,.19,.11),'wood'),('iron',(.10,.12,.11),'metal'),('rope',(.31,.28,.20),'cloth'),('bulb',(.72,.59,.31),'ceramic')]};dockparts=[]
# Deck top -1.30; cable low point +1.32 ->2.62m minimum headroom.
posts=[(30.12,z) for z in [3.4,-2.6,-8.8]]
for x,z in posts:
 dockparts.append(s.box('Dock string structural post',(x,-z,.18),(.13,.13,2.96),m['wood']))
 dockparts.append(s.box('Post bolted foot',(x,-z,-1.26),(.25,.25,.08),m['iron']))
for a,c in zip(posts,posts[1:]):
 prev=None
 for i in range(25):
  t=i/24;point=(a[0],-(a[1]+(c[1]-a[1])*t),1.66-.34*math.sin(t*math.pi))
  if prev:dockparts.append(s.rod('Supported sagging electrical cable',prev,point,.012,m['iron'],6))
  prev=point
  if i in [4,8,12,16,20]:
   x,y,h=point;dockparts.append(s.rod('Pendant socket',(x,y,h),(x,y,h-.065),.032,m['iron'],8));dockparts.append(s.lathe('Non-emissive review lamp globe',[(h-.14,.015),(h-.11,.055),(h-.065,.032)],m['bulb'],12,location=(x,y,0)))
# Cable terminates physically at shed wall/board-side conduit; switches are native only.
dockparts.append(s.rod('Shed to end-post feed',(27.64,8.8,1.0),(30.12,8.8,1.66),.014,m['iron'],8))
feed=[(27.67,7.4,-.31),(27.67,7.4,.60),(27.67,8.8,.60),(27.64,8.8,1.0)]
for a,c in zip(feed,feed[1:]):dockparts.append(s.rod('Board to overhead feed conduit',a,c,.018,m['iron'],8))
sourcefeed=[(27.35,8.13,-1.15),(27.67,8.13,-1.15),(27.67,7.4,-1.15),(27.67,7.4,-.31)]
for a,c in zip(sourcefeed,sourcefeed[1:]):dockparts.append(s.rod('Generator floor service feed',a,c,.014,m['iron'],8))
for z in [-6.67,-3.33]:
 dockparts.append(s.rod('Mooring line from boat ring to deck cleat',(23.9,-z,-1.63),(25.64,-z,-1.22),.018,m['rope'],8))
 dockparts.append(s.rod('Dock cleat',(25.64,-z-.13,-1.22),(25.64,-z+.13,-1.22),.025,m['iron'],8))
# Additional piles directly below the load-bearing shed corners, down to actual lake bed.
for x in [25.77,27.53]:
 for z in [-8.78,-6.62]:
  bottom=height(x,z)-.30;dockparts.append(s.rod('Shed load-bearing lake pile',(x,-z,bottom),(x,-z,-1.48),.13,m['wood'],12))
dock=s.join(dockparts,'level_camp_dock_utilities');bpy.ops.object.transform_apply(location=True,rotation=True,scale=True);dock.data.calc_loop_triangles();bpy.ops.export_scene.gltf(filepath=str(ROOT/'assets/runtime/models/level_camp_dock_utilities.glb'),export_format='GLB',use_selection=True,export_yup=True);bpy.data.libraries.write(str(ROOT/'development/levels/blender/level_camp_dock_utilities.blend'),{dock},compress=True);assets.append(dict(id=dock.name,triangles=len(dock.data.loop_triangles),editable='development/levels/blender/level_camp_dock_utilities.blend'))
# Fill actual camp clusters, without scattering arbitrary props across route lanes.
for i,(x,z,yaw) in enumerate([(11.9,40.0,.8),(12.3,42.7,1.4),(15.7,30.4,-.5),(18.0,30.0,.5),(48,43.3,1.1),(53.4,42.3,-1.0)]):
 o=place('level_camp_folding_chair','Camp folding chair '+str(i),x,z,height(x,z),yaw);o.data=o.data.copy()
 # Each original leg end fits real terrain; seats remain a single rigid chair.
 base=height(x,z);feet=[]
 for v in o.data.vertices:
  if v.co.z<.025:
   wx=x+math.cos(yaw)*v.co.x-math.sin(yaw)*v.co.y;wz=z-math.sin(yaw)*v.co.x-math.cos(yaw)*v.co.y;v.co.z+=height(wx,wz)-base;feet.append([wx,wz,height(wx,wz)])
 placements[-1]['feet']=feet
for i,(x,z,yaw) in enumerate([(12.0,43.6,.2),(17.0,29.6,.4),(53.0,44.8,-.3)]):
 # Rigid containers get short terrain-fitted feet, not warped lid/body geometry.
 y=max(height(x+dx,z+dz) for dx in [-.38,.38] for dz in [-.32,.32])+.02;place('level_camp_cooler','Camp cooler '+str(i),x,z,y,yaw)
 for dx in [-.23,.23]:
  for dz in [-.13,.13]:
   wx=x+math.cos(yaw)*dx-math.sin(yaw)*dz;wz=z-math.sin(yaw)*dx-math.cos(yaw)*dz;g=height(wx,wz);s.box('Cooler ground shim',(wx,-wz,(g+y)/2),(.10,.10,y-g+.008),m['wood'])
# Only remove decorative fern conflicts from new furnished footprints.
removed=[]
for o in list(scene.objects):
 if o.name.startswith('Interior grounded fern ') and any(abs(o.location.x-p['x'])<.8 and abs(-o.location.y-p['z'])<.8 for p in placements):removed.append(o.name);bpy.data.objects.remove(o,do_unlink=True)
scene['production_status']='Additive shore/dock/camp native detail; runtime and power integration pending';scene.cycles.samples=20;scene.render.resolution_x=1440;scene.render.resolution_y=960
fill=bpy.data.objects.get('Boundary inspection fill only')
def camera(name,pos,target,lens):
 scene.camera.location=pos;scene.camera.rotation_euler=(Vector(target)-scene.camera.location).to_track_quat('-Z','Y').to_euler();scene.camera.data.lens=lens
 if fill:fill.location=pos;fill.rotation_euler=scene.camera.rotation_euler
 scene.render.filepath=str(PRE/(name+'.png'));bpy.ops.render.render(write_still=True)
bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'hollow-creek-dock.blend'),compress=True)
camera('dock-expanded-site-native-fitout',(105,-125,100),(31,-42,1),40)
camera('dock-service-native-fitout',(36,-.5,3),(27,6.8,.0),32)
camera('shore-return-native-fitout',(13,-18,2),(0,-8,-1),26)
camera('camp-furnished-native-fitout',(21,-46,2.4),(12,-39,.1),28)
report=dict(status='native dock and campsite detail; runtime acceptance pending',assets=assets,placements=placements,protected_sources=protected,shore_added_triangles=len(ff)-old_faces,shore_return_objects=shore_strips,dock_clear_lane=[28,30, -10,4],minimum_string_globe_headroom_m=2.48,removed_decorative_ferns=removed,power=dict(source='Dock reused generator',board_local_native=[.52,1.09,1.28],circuit_id='HC-DOCK-01',loads='10 string lamps; currently non-emissive native placeholders',runtime_state='Not integrated; source availability separate from normal host breaker state'),limitations=['No runtime map unlock, collision/AI, water alpha/sorting, circuit state, LAN or performance acceptance.','Native lamp globes are non-emissive; switching and ghost breaker behavior remain required.','Generator is reused intact; exhaust hook-up and combustion ventilation engineering remain open.','Portable toilets, more tent variants, park fixtures, oil lanterns and further camp gear remain queued.'])
for a in assets:
 path=ROOT/f"assets/runtime/models/{a['id']}.glb";a.update(bytes=path.stat().st_size,sha256=hashlib.sha256(path.read_bytes()).hexdigest());assert a['triangles']<50000 and a['bytes']<4000000
report['shore_seam_vertices']=seam_contract;report['power']['board_local_native']=[1.05,-.30,1.28]
(OUT/'dock-details.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
for p,h in protected.items():assert hashlib.sha256((ROOT/p).read_bytes()).hexdigest()==h,p
print('BUILT additive dock and camp details;',len(ff)-old_faces,'repaired coast triangles;',len(placements),'placed models',flush=True)
