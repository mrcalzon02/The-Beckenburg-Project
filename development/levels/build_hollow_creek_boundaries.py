"""Add terrain-fitted land boundaries to a new derived Hollow Creek scene.
No engine, original terrain, map layout, prior facility or van geometry is rewritten.
The lake/water side remains explicitly open pending water-entry/containment work.
"""
import bpy,bmesh,sys,json,math,hashlib,random
from pathlib import Path
from mathutils import Vector
from mathutils.bvhtree import BVHTree
ROOT=Path(__file__).resolve().parents[2];sys.path[:0]=[str(ROOT/'development/levels'),str(ROOT/'development/assets')]
import rebuild_support as s
OUT=ROOT/'development/levels/hollow-creek';PRE=ROOT/'development/levels/previews/hollow-creek'
source=OUT/'hollow-creek-organic.blend';contract=json.loads((OUT/'organic-terrain.json').read_text(encoding='utf-8'))
protected_paths=[source,OUT/'organic-terrain.json',OUT/'organic-assets.json',OUT/'hollow-creek-terrain.blend',OUT/'terrain.json',OUT/'terrain-assets.json',OUT/'hollow-creek-pavilion.blend',OUT/'facilities.json',ROOT/'assets/runtime/models/level_camp_terrain.glb',ROOT/'development/levels/layouts/wld-02/layout.json',*sorted((ROOT/'development/levels/blockouts').glob('*/rough-shell.glb'))]
protected={p.relative_to(ROOT).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in protected_paths}
bpy.ops.wm.open_mainfile(filepath=str(source));bpy.context.preferences.filepaths.save_version=0
scene=bpy.context.scene;terrain=bpy.data.objects['level_camp_terrain_organic']
ground=BVHTree.FromPolygons([terrain.matrix_world@v.co for v in terrain.data.vertices],[tuple(p.vertices) for p in terrain.data.polygons])
prior={o.name:dict(matrix=[list(row) for row in o.matrix_world],vertices=len(o.data.vertices),sha256=hashlib.sha256(b''.join(__import__('struct').pack('<fff',*v.co) for v in o.data.vertices)).hexdigest()) for o in scene.objects if o.type=='MESH'}
def height(x,z):
 p=ground.ray_cast(Vector((x,-z,20)),Vector((0,0,-1)),40)[0]
 if p is None:raise ValueError(('Boundary must stay on existing native ground',x,z))
 return p.z
rock=s.material('Boundary restrained weathered stone',(.25,.27,.22),'paint')
soil=s.material('Boundary dark rooted earth',(.19,.22,.13),'paint')
bark=s.material('Boundary natural dark bark',(.20,.145,.09),'paint')
wood=s.material('Boundary faded timber',(.30,.25,.17),'paint')
iron=s.material('Boundary gate iron',(.13,.145,.14),'metal')
leaves=s.material('Boundary foliage restrained green',(.14,.23,.095),'paint')
# Retain material identity while removing coarse 64px checker breakup.
for material in [rock,soil,bark,wood,iron,leaves]:
 shader=material.node_tree.nodes['Principled BSDF'];node=next(n for n in material.node_tree.nodes if n.type=='TEX_IMAGE');base_image=node.image
 sample=list(base_image.pixels[:4]);rng=random.Random(material.name+'-fine-patina');im=bpy.data.images.new(material.name+' fine diffuse',width=256,height=256);pixels=[]
 for y in range(256):
  for x in range(256):
   f=1+rng.uniform(-.035,.035)+.018*math.sin(x*.13+y*.19)*math.sin(y*.07)
   pixels.extend([max(0,min(1,c*f)) for c in sample[:3]]+[1])
 im.pixels.foreach_set(pixels);im.pack();node.image=im;node.interpolation='Linear'
families={key:[] for key in ['banks','logfall','fence','gate']};footings=[];stations=[];log_contacts=[]
paths=contract['boundary_paths']
def xyz(x,z,y):return (x,-z,y)
def bank_top(x,z):
 t=max(0,min(1,(z-74)/4));t=t*t*(3-2*t)
 return height(x,z)+((1-t)*(1.88+.23*math.sin(z*.21+x*.18))+t*.78)
def add_mesh(name,vv,ff,mat,family,role):
 o=s.mesh(name,vv,ff,mat);bm=bmesh.new();bm.from_mesh(o.data);bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces));bm.to_mesh(o.data);bm.free();o['role']=role;o['boundary_family']=family;families[family].append(o);return o
def box(name,pos,size,mat,family,angle=0,role='Supported boundary component'):
 o=s.box(name,pos,size,mat);o.rotation_euler.z=angle;o['role']=role;o['boundary_family']=family;families[family].append(o);return o
def rod(name,a,b,r,mat,family,sides=8):
 o=s.rod(name,a,b,r,mat,sides);o['role']='Grounded/supported boundary component';o['boundary_family']=family;families[family].append(o);return o
for path in paths:
 points=[]
 for a,b in zip(path['points'],path['points'][1:]):
  length=math.dist(a,b);n=math.ceil(length/.5)
  points.extend([[a[0]+(b[0]-a[0])*i/n,a[1]+(b[1]-a[1])*i/n] for i in range(n)])
 points.append(path['points'][-1]);vv=[];ff=[];profiles=[]
 for i,(x,z) in enumerate(points):
  before=points[max(0,i-1)];after=points[min(len(points)-1,i+1)];dx=after[0]-before[0];dz=after[1]-before[1];length=math.hypot(dx,dz);nx=-dz/length;nz=dx/length
  top=bank_top(x,z);profile=[]
  for off,crest in [(-1.0,False),(-.2,True),(.7,True),(1.6,False)]:
   px=x+nx*off;pz=z+nz*off;surface=height(px,pz);y=(top if off<0 else top-.12) if crest else surface-.20
   profile.append(xyz(px,pz,y));vv.append(profile[-1])
   if not crest:footings.append(dict(path=path['id'],x=px,z=pz,bottom=y,ground=surface))
  profiles.append(profile);stations.append(dict(path=path['id'],x=x,z=z,nx=nx,nz=nz,ground=height(x,z),top=top))
  if i:
   k=(i-1)*4
   for j in range(4):ff.append((k+j,k+(j+1)%4,k+4+(j+1)%4,k+4+j))
 ff.extend([(3,2,1,0),tuple(range(len(vv)-4,len(vv)))]);core=add_mesh(path['id']+' grounded bank',vv,ff,soil,'banks','Continuous closed rooted bank; native collider candidate, runtime integration pending')
 # Rock faces are part of the bank, never hovering props. Dressing remains sparse.
 for i in range(0,len(points)-4,7):
  x,z=points[i];x2,z2=points[min(i+4,len(points)-1)];mid=((x+x2)/2,(z+z2)/2)
  if z>77:continue
  top=min(bank_top(x,z),bank_top(x2,z2));floor=min(height(x,z),height(x2,z2))
  bpy.ops.mesh.primitive_ico_sphere_add(subdivisions=2,radius=1,location=xyz(*mid,(top+floor)/2))
  o=bpy.context.object;o.name=path['id']+' embedded boulder '+str(i);o.dimensions=(max(.6,math.dist([x,z],[x2,z2])),1.25,top-floor+.3);o.rotation_euler.z=math.atan2(-(z2-z),x2-x);bpy.ops.object.transform_apply(location=False,rotation=False,scale=True);o.data.materials.append(rock);s.uv_project(o);o['role']='Embedded weathered rock in rooted bank'
  for poly in o.data.polygons:poly.use_smooth=True
  families['banks'].append(o)
 # Irregular logfall remains visibly supported on the bank crest.
 for i in range(4,len(points)-6,11):
  x,z=points[i];x2,z2=points[i+4]
  if z>74 or z<16:continue
  a=xyz(x,z,bank_top(x,z)+.16);b=xyz(x2,z2,bank_top(x2,z2)+.16)
  name=path['id']+' crest log '+str(i);rod(name,a,b,.25,bark,'logfall',10)
  log_contacts.append(dict(name=name,ends=[[x,z,a[2]-.25],[x2,z2,b[2]-.25]],expected_embed=.09))
  # Short branch forks join the log itself, not a floating root plate.
  for sign in [-1,1]:
   t=.38 if sign<0 else .7;aa=Vector(a).lerp(Vector(b),t);bb=aa+Vector((sign*.30,sign*.25,.22));rod(name+' branch '+str(sign),aa,bb,.07,bark,'logfall',6)
 # Grounded woody growth interrupts the long bank silhouette; no hiding grant.
 for i in range(2,len(points)-3,13):
  x,z=points[i]
  if z>76 or z<15:continue
  top=bank_top(x,z);base=Vector(xyz(x,z,top-.16));tip=base+Vector((.06,.04,.85))
  rod(path['id']+' rooted scrub stem '+str(i),base,tip,.065,bark,'logfall',6)
  for j,(dx,dz,dyy,rad) in enumerate([(-.42,0,.48,.63),(.37,.15,.62,.72),(0,-.26,.98,.65)]):
   center=base+Vector((dx,-dz,dyy));rod('Scrub branch contact',base+Vector((0,0,.25)),center,.035,bark,'logfall',6)
   bpy.ops.mesh.primitive_ico_sphere_add(subdivisions=2,radius=rad,location=center)
   o=bpy.context.object;o.name='Supported boundary scrub crown';o.scale=(1,.85,1.06);bpy.ops.object.transform_apply(location=False,rotation=False,scale=True);o.data.materials.append(leaves);s.uv_project(o);o['role']='Woody boundary vegetation supported by rooted branches; no gameplay refuge flag'
   for poly in o.data.polygons:poly.use_smooth=True
   families['logfall'].append(o)
 # Board-on-board service returns: closed lower bank plus supported palings.
 for a,b in zip(path['points'],path['points'][1:]):
  if max(a[1],b[1])<=74:continue
  a=list(a);b=list(b)
  if a[1]<74:
   t=(74-a[1])/(b[1]-a[1]);a=[a[0]+t*(b[0]-a[0]),74]
  if b[1]<74:
   t=(74-b[1])/(a[1]-b[1]);b=[b[0]+t*(a[0]-b[0]),74]
  dx=b[0]-a[0];dz=b[1]-a[1];length=math.hypot(dx,dz);angle=math.atan2(-dz,dx);n=math.ceil(length/2)
  for j in range(n+1):
   t=j/n;x=a[0]+dx*t;z=a[1]+dz*t;y=height(x,z)
   box('Fence grounded post',xyz(x,z,y+1.05),(.16,.16,2.5),wood,'fence',angle)
   footings.append(dict(path='fence-post',x=x,z=z,bottom=y-.20,ground=y))
  for h in [.95,1.8]:
   rod('Fence supported rail',xyz(*a,height(*a)+h),xyz(*b,height(*b)+h),.065,wood,'fence',6)
  n=math.ceil(length/.22)
  for j in range(n):
   t=(j+.5)/n;x=a[0]+dx*t;z=a[1]+dz*t;y=height(x,z)
   box('Fence overlapping paling',xyz(x,z,y+1.43),(length/n+.04,.09,1.72),wood,'fence',angle)
# Closed roadhead gate lies beyond the van, not across the reviewed cargo route.
for x in [28,36]:
 y=height(x,85);box('Gate buried timber post',xyz(x,85,y+1.12),(.24,.24,2.64),wood,'gate');footings.append(dict(path='gate-post',x=x,z=85,bottom=y-.20,ground=y))
for lo,hi in [(28,32.02),(31.98,36)]:
 n=math.ceil((hi-lo)/.2)
 for j in range(n):
  x=lo+(hi-lo)*(j+.5)/n;y=height(x,85)
  box('Gate closed overlapping board',xyz(x,85,y+1.11),((hi-lo)/n+.025,.12,2.12),wood,'gate')
 for h in [.78,1.72]:rod('Gate continuous rear rail',xyz(lo,85,height(lo,85)+h),xyz(hi,85,height(hi,85)+h),.065,iron,'gate',6)
 rod('Gate diagonal brace',xyz(lo,85,height(lo,85)+.78),xyz(hi,85,height(hi,85)+1.72),.05,iron,'gate',6)
for x in [28.14,35.86]:
 y=height(x,85)
 for h in [.78,1.72]:rod('Gate hinge pin',xyz(x,84.88,y+h-.08),xyz(x,84.88,y+h+.08),.04,iron,'gate',8)
rod('Gate centre latch',xyz(31.7,84.9,height(32,85)+1.1),xyz(32.3,84.9,height(32,85)+1.1),.035,iron,'gate',8)
assets=[]
for family,parts in families.items():
 name='level_camp_boundary_'+family;o=s.join(parts,name);bpy.ops.object.transform_apply(location=True,rotation=True,scale=True);o.data.calc_loop_triangles();assert len(o.data.loop_triangles)<50000
 o['role']='Original site-fitted land boundary; native acceptance only';o['license']='CC0-1.0';o['origin_policy']='World-site origin and metre coordinates retained; do not normalize'
 target=ROOT/f'assets/runtime/models/{name}.glb';bpy.ops.export_scene.gltf(filepath=str(target),export_format='GLB',use_selection=True,export_yup=True,export_extras=True);assert target.stat().st_size<4_000_000
 editable=ROOT/f'development/levels/blender/{name}.blend'
 # Export selected family to an editable library; preserve the complete derived scene separately.
 bpy.data.libraries.write(str(editable),{o,*[m for m in o.data.materials if m]},compress=True)
 assets.append(dict(id=name,triangles=len(o.data.loop_triangles),bytes=target.stat().st_size,sha256=hashlib.sha256(target.read_bytes()).hexdigest(),editable=str(editable.relative_to(ROOT)).replace('\\','/'),license='CC0-1.0',source='Original Beckenburg boundary geometry and procedural diffuse; no third-party components'))
scene['production_status']='Additive native land-boundary pass; lake containment and runtime integration remain open'
scene.world.node_tree.nodes['Background'].inputs[0].default_value=(.38,.45,.52,1)
scene.world.node_tree.nodes['Background'].inputs[1].default_value=.7
bpy.ops.object.light_add(type='AREA');inspection=bpy.context.object;inspection.name='Boundary inspection fill only';inspection.data.energy=2200;inspection.data.shape='DISK';inspection.data.size=10;inspection['role']='Native authoring inspection light, not gameplay lighting'
scene.render.engine='CYCLES';scene.cycles.samples=16;scene.cycles.use_denoising=True;scene.render.resolution_x=1200;scene.render.resolution_y=800;scene.render.resolution_percentage=100;scene.render.image_settings.file_format='PNG'
def camera(name,pos,target,lens=35):
 scene.camera.location=pos;scene.camera.rotation_euler=(Vector(target)-scene.camera.location).to_track_quat('-Z','Y').to_euler();scene.camera.data.type='PERSP';scene.camera.data.lens=lens;inspection.location=pos;inspection.rotation_euler=scene.camera.rotation_euler;scene.render.filepath=str(PRE/(name+'.png'));bpy.ops.render.render(write_still=True)
bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'hollow-creek-boundaries.blend'),compress=True)
camera('boundary-site-native-fitout',(100,-115,92),(30,-43,1),44)
camera('boundary-roadhead-native-fitout',(42,-85,6),(32,-77,2.5),32)
camera('boundary-van-exit-native-fitout',(32,-73.8,3.12),(32,-61,1.5),22)
camera('boundary-logfall-native-fitout',(6,-39,1.62),(0,-41,1.2),26)
camera('boundary-shore-vista-native-fitout',(32,-14,.32),(30,10,-.2),25)
report=dict(status='native land-boundary construction; incomplete site containment',assets=assets,paths=paths,stations=stations,footings=footings,log_contacts=log_contacts,gate=dict(x_range=[28,36],z=85,state='closed static native model; articulation/interaction pending'),protected_sources=protected,prior_meshes=prior,limitations=['No runtime level/collision/room/circuit integration or mission unlock.','Two shoreline ends intentionally remain open; water-entry and complete containment need separate engineering and host tests.','No new distant scenery or runtime sky-sector mask; images are native construction reviews.','Fence/gate bodies are static; gate articulation and operational fixtures are unaccepted.'])
(OUT/'boundaries.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
for path,h in protected.items():assert hashlib.sha256((ROOT/path).read_bytes()).hexdigest()==h,path
print('BUILT four original site-fitted boundary exports; source terrain/facilities/layouts unchanged',flush=True)
