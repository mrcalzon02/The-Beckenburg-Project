"""Original tent/rolled bedding assets and additive terrain-fitted camp placements.
Measured against the existing .66 x 1.86m bedroll and authored crouch approaches.
Never flattens a zone, changes prior sources, or unlocks the runtime map.
"""
import bpy,bmesh,sys,json,math,random,hashlib
from pathlib import Path
from mathutils import Vector
from mathutils.bvhtree import BVHTree
ROOT=Path(__file__).resolve().parents[2];sys.path[:0]=[str(ROOT/'development/levels'),str(ROOT/'development/assets')]
import rebuild_support as s
import build_lived_assets as b
OUT=ROOT/'development/levels/hollow-creek';PRE=ROOT/'development/levels/previews/hollow-creek';CLEANUP=[]
def cloth(name,color):
 m=s.material(name,color,'paint');node=next(n for n in m.node_tree.nodes if n.type=='TEX_IMAGE');im=bpy.data.images.new(name+' fine woven diffuse',width=256,height=256);rng=random.Random(name);pixels=[]
 for y in range(256):
  for x in range(256):
   weave=.012*math.sin(x*math.pi/2)*math.sin(y*math.pi/2);f=1+weave+rng.uniform(-.018,.018)+.018*math.sin(x*.04+y*.07);pixels.extend([c*f for c in color]+[1])
 im.pixels.foreach_set(pixels);im.pack();node.image=im;node.interpolation='Linear';return m
def canvas(name,verts,faces,material,parts,thickness=.008):
 o=s.mesh(name,verts,faces,material);bm=bmesh.new();bm.from_mesh(o.data);loose=[v for v in bm.verts if not v.link_faces];CLEANUP.append(dict(component=name,unreferenced_vertices_removed=len(loose)));bmesh.ops.delete(bm,geom=loose,context='VERTS');bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=.000001);bm.to_mesh(o.data);bm.free();o.data.update();mod=o.modifiers.new('Real 8mm canvas shell','SOLIDIFY');mod.thickness=thickness;mod.offset=0;bpy.context.view_layer.objects.active=o;bpy.ops.object.modifier_apply(modifier=mod.name)
 for poly in o.data.polygons:poly.use_smooth=True
 parts.append(o);return o
def ridge():
 m=b.init();fabric=cloth('Weathered olive tent canvas',(.28,.32,.17));floor=cloth('Tent waterproof groundsheet',(.09,.11,.085));seam=cloth('Canvas reinforced seams',(.21,.24,.13));p=[]
 p.append(s.box('Groundsheet physical floor',(0,0,.022),(2.60,3.40,.012),floor))
 for side in [-1,1]:
  vv=[];ff=[]
  for j in range(13):
   y=-1.7+j*3.4/12;ridge=1.90-.045*math.cos(y*math.pi/3.4)
   for k in range(6):
    t=k/5;x=side*t*1.3;z=ridge*(1-t)+.18*t-.035*math.sin(t*math.pi);vv.append((x,y,z))
   if j:
    for k in range(5):a=(j-1)*6+k;ff.append((a,a+1,a+7,a+6))
  canvas('Catenary canvas side '+str(side),vv,ff,fabric,p)
  canvas('Ground skirt '+str(side),[(side*1.3,-1.7,.02),(side*1.3,1.7,.02),(side*1.3,1.7,.18),(side*1.3,-1.7,.18)],[(0,1,2,3)],fabric,p)
  for y in [-1.65,1.65]:p.append(s.rod('A-frame support leaving doorway clear',(side*1.20,y,.025),(0,y,1.90),.021,m['steel'],10))
  for y in [-1.7,1.7]:
   start=(side*1.26,y,.25);end=(side*1.92,y*1.22,.025);p.append(s.rod('Supported corner guyline',start,end,.007,m['cloth'],6));p.append(s.rod('Grounded guy peg',(end[0],end[1],-.10),(end[0]-.035,end[1]-.02,.09),.012,m['steel'],8))
 p.append(s.rod('Connected ridge pole',(0,-1.70,1.90),(0,1.70,1.90),.022,m['steel'],10))
 canvas('Closed rear canvas',[(-1.3,1.7,.02),(1.3,1.7,.02),(1.3,1.7,.18),(0,1.7,1.90),(-1.3,1.7,.18)],[(0,1,2,3,4)],fabric,p)
 for side in [-1,1]:canvas('Tied-open front flap '+str(side),[(side*1.3,-1.7,.02),(side*.45,-1.7,.02),(side*.45,-1.7,1.20),(0,-1.7,1.90),(side*1.3,-1.7,.18)],[(0,1,2,3,4)],fabric,p)
 canvas('Door canopy header',[(-.45,-1.7,1.20),(.45,-1.7,1.20),(0,-1.7,1.90)],[(0,1,2)],seam,p)
 for side in [-1,1]:
  p.append(s.rod('Door seam zipper',(side*.45,-1.711,.04),(side*.45,-1.711,1.20),.006,m['black'],6))
  p.append(s.rod('Tied flap toggle',(side*.81,-1.73,.58),(side*.86,-1.73,.62),.012,m['oak'],6))
 b.export('level_camp_tent_ridge',p,'campground',view=(5,-7,3.8))
def dome():
 m=b.init();fabric=cloth('Muted ochre dome canvas',(.40,.30,.16));floor=cloth('Dome groundsheet',(.085,.10,.09));fv=[];fs=[];ns=64
 for height in [.016,.028]:
  for i in range(ns):a=i/ns*math.tau;fv.append((1.29*math.cos(a),1.52*math.sin(a),height))
 fs=[tuple(range(ns-1,-1,-1)),tuple(range(ns,2*ns))]+[(i,(i+1)%ns,(i+1)%ns+ns,i+ns) for i in range(ns)]
 p=[s.mesh('Dome inset oval groundsheet',fv,fs,floor)];vv=[];ff=[];ns=32;nr=12
 for j in range(nr+1):
  latitude=j/nr*math.pi/2
  for i in range(ns):
   angle=i/ns*math.tau;vv.append((1.40*math.cos(latitude)*math.cos(angle),1.65*math.cos(latitude)*math.sin(angle),.03+1.65*math.sin(latitude)))
 for j in range(nr):
  for i in range(ns):
   angle=(i+.5)/ns*math.tau;front=abs(math.atan2(math.sin(angle+math.pi/2),math.cos(angle+math.pi/2)))
   if front<.40 and j<7:continue
   ff.append((j*ns+i,j*ns+(i+1)%ns,(j+1)*ns+(i+1)%ns,(j+1)*ns+i))
 canvas('Sewn multi-panel dome canopy',vv,ff,fabric,p)
 for angle in [math.pi/4,3*math.pi/4]:
  prev=None
  for i in range(33):
   t=i/32*math.pi;x=1.43*math.cos(t)*math.cos(angle);y=1.68*math.cos(t)*math.sin(angle);z=.03+1.68*math.sin(t);point=(x,y,z)
   if prev:p.append(s.rod('Attached segmented flexible tent pole',prev,point,.012,m['steel'],8))
   prev=point
 for i in range(8):
  angle=i*math.tau/8;x=1.4*math.cos(angle);y=1.65*math.sin(angle);a=(x,y,.08);end=(x*1.22,y*1.18,.02)
  if y< -1.3 and abs(x)<.6:continue
  p.append(s.rod('Dome perimeter guy',a,end,.006,m['cloth'],6));p.append(s.rod('Dome grounded peg',(end[0],end[1],-.09),(end[0],end[1],.07),.012,m['steel'],8))
 b.export('level_camp_tent_dome',p,'campground',view=(5,-7,3.8))
def roll():
 m=b.init();fabric=cloth('Rolled sleeping quilt',(.18,.25,.29));strap=cloth('Bedroll woven carry straps',(.10,.12,.10));p=[]
 o=s.lathe('Soft rolled sleeping bag',[(-.32,.12),(-.30,.16),(-.22,.167),(.22,.167),(.30,.16),(.32,.12)],fabric,32)
 for v in o.data.vertices:x,y,z=v.co;v.co=(z,y,x+.17)
 s.uv_project(o)
 for poly in o.data.polygons:poly.use_smooth=len(poly.vertices)==4
 p.append(o)
 for x in [-.21,.21]:
  vv=[];ff=[]
  for i in range(33):
   a=i/32*math.tau
   for xx in [x-.018,x+.018]:vv.append((xx,.17*math.sin(a),.17+.17*math.cos(a)))
   if i:k=(i-1)*2;ff.append((k,k+1,k+3,k+2))
  canvas('Wrapped carry webbing',vv,ff,strap,p,thickness=.002);p.append(s.box('Strap buckle',(x,-.174,.17),(.045,.018,.065),m['steel'],.005))
 for x in [-.322,.322]:
  prev=None
  for i in range(81):
   t=i/80;a=t*math.pi*5;radius=.025+.092*t;point=(x,radius*math.sin(a),.17+radius*math.cos(a))
   if prev:p.append(s.rod('Visible rolled quilt edge',prev,point,.0025,m['cloth'],5))
   prev=point
 b.export('level_camp_bedroll_rolled',p,'campground',view=(2,-3,2))
for fn in [ridge,dome,roll]:fn()
assets=b.LOG[-3:];source=OUT/'hollow-creek-foliage-detail.blend';protected={str(source.relative_to(ROOT)).replace('\\','/'):hashlib.sha256(source.read_bytes()).hexdigest(), 'assets/runtime/models/level_trace_bedroll.glb':hashlib.sha256((ROOT/'assets/runtime/models/level_trace_bedroll.glb').read_bytes()).hexdigest()}
bpy.ops.wm.open_mainfile(filepath=str(source));bpy.context.preferences.filepaths.save_version=0;scene=bpy.context.scene
terrain=bpy.data.objects['level_camp_terrain_organic'];ground=BVHTree.FromPolygons([terrain.matrix_world@v.co for v in terrain.data.vertices],[tuple(p.vertices) for p in terrain.data.polygons])
def height(x,z):
 p=ground.ray_cast(Vector((x,-z,10)),Vector((0,0,-1)),30)[0];assert p is not None,(x,z);return p.z
placements=[];removed=[]
approach_xz=[dict(id='west-pitched-ridge',points=[[14.0, 37.0], [11.0, 30.2], [6.6, 30.2], [6.2, 30.8], [6.2, 35.0], [6.6, 35.4], [8.6, 35.2], [8.8, 35.1]]),dict(id='east-pitched-dome',points=[[50,37],[48.4,39.5],[48.4,45.5],[51,45.1]])]
def place(asset,name,x,z,yaw=0,lift=0):
 with bpy.data.libraries.load(str(ROOT/asset['editable']),link=False) as (incoming,outgoing):outgoing.objects=[asset['id']]
 obj=outgoing.objects[0];scene.collection.objects.link(obj);obj.name=name;obj.data=obj.data.copy();base=height(x,z);cs=math.cos(yaw);sn=math.sin(yaw)
 for v in obj.data.vertices:
  lx,ly,lz=v.co;wx=x+cs*lx+sn*ly;wz=z+sn*lx-cs*ly;v.co=(wx,-wz,lz+height(wx,wz)+lift)
 obj.location=(0,0,0);obj['role']='Terrain-fitted campsite model; runtime integration pending';obj['campsite_id']=name
 placements.append(dict(id=name,asset=asset['id'],x=x,z=z,y=base+lift,yaw=yaw,lift=lift,native_object=obj.name,terrain_fitted=True));return obj
by={a['id']:a for a in assets};west=place(by['level_camp_tent_ridge'],'west-pitched-ridge',8.8,32.8);east=place(by['level_camp_tent_dome'],'east-pitched-dome',51,43)
def point_segment(x,z,a,b):
 dx=b[0]-a[0];dz=b[1]-a[1];t=max(0,min(1,((x-a[0])*dx+(z-a[1])*dz)/(dx*dx+dz*dz or 1)));return math.hypot(x-a[0]-dx*t,z-a[1]-dz*t)
# Remove only decorative ferns from the newly occupied pads/entrances in this copy.
for obj in list(scene.objects):
 if not obj.name.startswith('Interior grounded fern '):continue
 x,z=obj.location.x,-obj.location.y
 if any(abs(x-p['x'])<2.8 and abs(z-p['z'])<3.1 for p in placements) or any(point_segment(x,z,a,b)<1.3 for route in approach_xz for a,b in zip(route['points'],route['points'][1:])):removed.append(obj.name);bpy.data.objects.remove(obj,do_unlink=True)
place(by['level_camp_bedroll_rolled'],'west-rolled-bedding',7.9,33.4,.15,lift=.028)
# Existing laid-out bedding is reused with original UV/materials and floor pivot.
for name,x,z in [('west-laid-bedding',9.5,32.4),('east-laid-bedding',51.64,43.05)]:
 obj=place(dict(id='level_trace_bedroll',editable='development/levels/blender/level_trace_bedroll.blend'),name,x,z,lift=.028)
 obj['role']='Reused existing sleeping mat fitted to actual tent-floor support; original UV/materials retained'
scene['production_status']='Native pitched tents and bedding; geometry/circulation acceptance pending';scene.render.engine='CYCLES';scene.cycles.samples=20;scene.cycles.use_denoising=True;scene.render.resolution_x=1200;scene.render.resolution_y=800;scene.render.resolution_percentage=100;scene.render.image_settings.file_format='PNG'
fill=bpy.data.objects.get('Boundary inspection fill only')
def camera(name,pos,target,lens):
 scene.camera.location=pos;scene.camera.rotation_euler=(Vector(target)-scene.camera.location).to_track_quat('-Z','Y').to_euler();scene.camera.data.lens=lens
 if fill:fill.location=pos;fill.rotation_euler=scene.camera.rotation_euler
 scene.render.filepath=str(PRE/(name+'.png'));bpy.ops.render.render(write_still=True)
bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'hollow-creek-tents.blend'),compress=True)
camera('tents-west-native-fitout',(13,-40,2),(8.8,-32.8,.7),30)
camera('tents-east-native-fitout',(46,-49,2),(51,-43,.6),32)
camera('tents-site-native-fitout',(105,-125,100),(31,-42,1),40)
report=dict(status='native tent/bedding construction; acceptance pending',assets=assets,placements=placements,source_geometry_cleanup=CLEANUP,removed_decorative_ferns=removed,source_hashes=protected,approaches=[dict(id=a['id'],points=[[x,height(x,z),z] for x,z in a['points']],status='Native supported walking approach, texture wear and runtime navigation integration pending') for a in approach_xz],door_reservations=[dict(id='west-pitched-ridge',center=[8.8,height(8.8,35.1),35.1],width=.90,height=1.20,posture='crouched; not a standing doorway'),dict(id='east-pitched-dome',center=[51,height(51,45.1),45.1],width=1.0,height=1.2,posture='crouched')],limitations=['No runtime tent room/collision/AI/circuit or map unlock.','Canvas is an explicit thin shell; cloth transmission differs from opaque building light isolation.','Tent entry, anchors, floor and bedding support require actual native acceptance before dressing more.','Portable toilets, camp furniture, park details and dock/utilities/rowboat remain in the recorded detail queue.'])
(OUT/'tents.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
for path,h in protected.items():assert hashlib.sha256((ROOT/path).read_bytes()).hexdigest()==h,path
print('BUILT',len(assets),'original campsite assets;',len(placements),'placements; retained source scene and existing bedroll',flush=True)
