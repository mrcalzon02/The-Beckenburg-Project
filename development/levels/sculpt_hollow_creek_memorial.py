"""Continuous cast-bird prototype and real supported plaque lettering.
Use --publish only after prototype visual/topology review. Parent remains intact.
"""
import bpy,bmesh,math,sys,json,hashlib,random
from pathlib import Path
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[2];sys.path[:0]=[str(ROOT/'development/levels'),str(ROOT/'development/assets')]
import rebuild_support as s
import build_lived_assets as b
prototype='--publish' not in sys.argv;id='level_camp_heron_memorial_organic';parent=ROOT/'development/levels/blender/level_camp_heron_memorial_authored.blend'
protected=hashlib.sha256(parent.read_bytes()).hexdigest();s.reset()
with bpy.data.libraries.load(str(parent),link=False) as (src,dst):dst.objects=['level_camp_heron_memorial_authored']
base=dst.objects[0];bpy.context.scene.collection.objects.link(base);base.data=base.data.copy()
bird_ids={g.index for g in base.vertex_groups if g.name.startswith(('Heron ','Curved heron neck','Layered bronze wing feather','Smooth bronze heron head'))}
bm=bmesh.new();bm.from_mesh(base.data);deform=bm.verts.layers.deform.active;bmesh.ops.delete(bm,geom=[v for v in bm.verts if any(i in v[deform] for i in bird_ids)],context='VERTS');bm.to_mesh(base.data);bm.free()
bronze=s.material('Organic cast memorial bronze',(.13,.20,.165),'metal');dark=s.material('Recessed organic bird eye',(.024,.033,.027),'metal');parts=[]
def sphere(name,p,scale):
 bpy.ops.mesh.primitive_uv_sphere_add(segments=32,ring_count=20,location=p);o=bpy.context.object;o.name=name;o.scale=scale;bpy.ops.object.transform_apply(location=False,rotation=False,scale=True);o.data.materials.append(bronze);parts.append(o);return o
def tube(name,points,radii,sides=16):
 vv=[];ff=[]
 for i,p in enumerate(points):
  p=Vector(p);tangent=Vector(points[min(i+1,len(points)-1)])-Vector(points[max(i-1,0)]);tangent.normalize();a=Vector((1,0,0))
  if abs(a.dot(tangent))>.9:a=Vector((0,1,0))
  a=(a-tangent*a.dot(tangent)).normalized();c=tangent.cross(a).normalized()
  for j in range(sides):angle=j*math.tau/sides;vv.append(p+radii[i]*(a*math.cos(angle)+c*math.sin(angle)))
  if i:
   for j in range(sides):k=(i-1)*sides+j;ff.append((k,(i-1)*sides+(j+1)%sides,i*sides+(j+1)%sides,i*sides+j))
 ff +=[tuple(range(sides-1,-1,-1)),tuple((len(points)-1)*sides+j for j in range(sides))]
 o=s.mesh(name,vv,ff,bronze);parts.append(o);return o
def catmull(control,count):
 out=[]
 for i in range(count):
  t=i/(count-1)*(len(control)-1);k=min(len(control)-2,int(t));u=t-k;p0=Vector(control[max(0,k-1)]);p1=Vector(control[k]);p2=Vector(control[k+1]);p3=Vector(control[min(len(control)-1,k+2)])
  out.append(.5*((2*p1)+(-p0+p2)*u+(2*p0-5*p1+4*p2-p3)*u*u+(-p0+3*p1-3*p2+p3)*u**3))
 return out
sphere('Sculpted chest and back',(0,.07,2.13),(.22,.29,.33));sphere('Sculpted tapered tail',(0,.265,1.995),(.12,.24,.13))
neck=catmull([(0,-.035,2.12),(0,-.16,2.29),(0,-.035,2.47),(0,.045,2.64),(0,-.03,2.80),(0,-.175,2.87)],65)
tube('Continuous curved S neck',neck,[.070-.018*math.sin(i/64*math.pi) for i in range(65)])
sphere('Integrated heron skull',(0,-.175,2.87),(.084,.13,.077));tube('Integrated tapered bill',[(0,-.27,2.86),(0,-.40,2.85),(0,-.62,2.825)],[.040,.026,.003])
for side in [-1,1]:
 leg=catmull([(side*.105,.015,1.285),(side*.105,-.015,1.53),(side*.105,.035,1.77),(side*.105,.035,1.96)],24)
 tube('Organic leg and ankle',leg,[.022+.006*i/23 for i in range(24)])
 for toe in [-1,0,1]:tube('Splayed heron toe',[(side*.105,.015,1.287),(side*.105+toe*.032,-.07,1.283),(side*.105+toe*.060,-.16+abs(toe)*.025,1.279)],[.021,.014,.006],12)
 tube('Rear supporting toe',[(side*.105,.015,1.287),(side*.105,.11,1.279)],[.020,.007],12)
 # Continuous solid wing plates with a scalloped feather edge, not rod fans.
 vv=[];ff=[];nu=48;nv=10
 for layer in [-1,1]:
  for i in range(nu+1):
   u=i/nu
   for j in range(nv+1):
    v=-1+2*j/nv
    x=side*(.08+.91*u);y=.13+.25*u+v*(.13*(1-u)+.03*u);z=2.20+.22*math.sin(u*math.pi*.8)-.12*u+.025*(1-v*v)
    z+=.006*math.cos(u*14*math.pi)*max(0,v)*(1-u);z+=layer*(.035*(1-u)+.012*u);vv.append((x,y,z))
  offset=0 if layer==-1 else (nu+1)*(nv+1)
  for i in range(nu):
   for j in range(nv):k=offset+i*(nv+1)+j;face=(k,k+1,k+nv+2,k+nv+1);ff.append(face if layer==1 else face[::-1])
 n=(nu+1)*(nv+1)
 for i in range(nu):
  for j in [0,nv]:a=i*(nv+1)+j;c=(i+1)*(nv+1)+j;ff.append((a,c,c+n,a+n))
 for i in [0,nu]:
  for j in range(nv):a=i*(nv+1)+j;ff.append((a,a+1,a+1+n,a+n))
 wing=s.mesh('Sculpted continuous wing',vv,ff,bronze);parts.append(wing)
 # Flattened overlapping flight-feather blades are fused into the casting;
 # their rounded free tips and ridges retain avian anatomy instead of bat webs.
 for feather in range(8):
  u=.16+.105*feather;root=Vector((side*(.08+.91*u),.13+.25*u,2.20+.22*math.sin(u*math.pi*.8)-.12*u))
  tip=Vector((side*(.38+.095*feather),.53+.035*math.sin(feather*.45),root.z-.08))
  axis=(tip-root).normalized();across=Vector((-axis.y,axis.x,0)).normalized();vv=[];ff=[];rings=18;sides=12
  for i in range(rings):
   t=i/(rings-1);center=root.lerp(tip,t);center.z+=.025*math.sin(t*math.pi)
   width=.052*math.sin((.08+.91*t)*math.pi)**.62+.004;depth=.013+.004*math.sin(t*math.pi)
   for j in range(sides):a=j*math.tau/sides;vv.append(center+across*width*math.cos(a)+Vector((0,0,depth*math.sin(a))))
   if i:
    for j in range(sides):k=(i-1)*sides+j;ff.append((k,(i-1)*sides+(j+1)%sides,i*sides+(j+1)%sides,i*sides+j))
  ff +=[tuple(range(sides-1,-1,-1)),tuple((rings-1)*sides+j for j in range(sides))];parts.append(s.mesh('Sculpted flattened flight feather',vv,ff,bronze))
bird=s.join(parts,'Organic continuous cast bird');bpy.ops.object.transform_apply(location=True,rotation=True,scale=True)
bm=bmesh.new();bm.from_mesh(bird.data);bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces));bm.to_mesh(bird.data);bm.free()
remesh=bird.modifiers.new('Fuse the casting into one organic surface','REMESH');remesh.mode='VOXEL';remesh.voxel_size=.009;remesh.use_smooth_shade=True
bpy.context.view_layer.objects.active=bird;bpy.ops.object.modifier_apply(modifier=remesh.name)
smooth=bird.modifiers.new('Sculpted surface relaxation','SMOOTH');smooth.factor=.55;smooth.iterations=3;bpy.ops.object.modifier_apply(modifier=smooth.name)
bird.data.calc_loop_triangles();before=len(bird.data.loop_triangles)
if before>24000:
 dec=bird.modifiers.new('Bounded cast surface','DECIMATE');dec.ratio=24000/before;dec.use_collapse_triangulate=True;bpy.ops.object.modifier_apply(modifier=dec.name)
offset=1.269-min(v.co.z for v in bird.data.vertices)
for v in bird.data.vertices:v.co.z+=offset
for poly in bird.data.polygons:poly.use_smooth=True
bird.vertex_groups.clear();g=bird.vertex_groups.new(name='Organic continuous cast bird');g.add(list(range(len(bird.data.vertices))),1,'REPLACE');s.uv_project(bird)
parts=[base,bird];letter=s.material('Engraved dark plaque lettering',(.035,.043,.031),'metal')
from mathutils.bvhtree import BVHTree
bird.data.calc_loop_triangles();cast=BVHTree.FromPolygons([v.co for v in bird.data.vertices],[tuple(t.vertices) for t in bird.data.loop_triangles],all_triangles=True)
for side in [-1,1]:
 hit,normal,_,_=cast.ray_cast(Vector((side*.30,-.228,2.883+offset)),Vector((-side,0,0)),.5);assert hit is not None,'Eye must mount to actual sculpted skull'
 bpy.ops.mesh.primitive_uv_sphere_add(segments=16,ring_count=12,location=hit+normal*.001);eye=bpy.context.object;eye.name='Inset memorial bird eye';eye.scale=(.006,.009,.006);bpy.ops.object.transform_apply(location=False,rotation=False,scale=True);eye.data.materials.append(dark);s.uv_project(eye)
 for p in eye.data.polygons:p.use_smooth=True
 parts.append(eye)
inscription=[('CAMP HOLLOW CREEK',.905,.039),('THE WATCHER',.815,.055),('KEEP THE WATER CLEAN',.744,.028),('LEAVE NO ONE BEHIND',.690,.028)]
for text,z,size in inscription:
 bpy.ops.object.text_add(location=(0,-.45815,z));o=bpy.context.object;o.name='Supported plaque inscription '+text;o.data.body=text;o.data.align_x='CENTER';o.data.size=size;o.data.extrude=.0003;o.rotation_euler.x=math.pi/2;bpy.ops.object.convert(target='MESH');o=bpy.context.object;o.data.materials.append(letter);s.uv_project(o);parts.append(o)
# Preserve all parent UVs and give new geometry the same export-active layer.
for o in parts[1:]:
 original=[tuple(v.uv) for v in o.data.uv_layers.active.data];layer=o.data.uv_layers.new(name='AuthoredWearAtlas')
 for p,uv in zip(layer.data,original):p.uv=uv
 for uv in o.data.uv_layers:uv.active_render=uv.name=='AuthoredWearAtlas'
obj=s.join(parts,id);bpy.ops.object.transform_apply(location=True,rotation=True,scale=True)
assert hashlib.sha256(parent.read_bytes()).hexdigest()==protected
target=ROOT/'build/memorial-organic-prototype';target.mkdir(exist_ok=True)
if prototype:
 bpy.ops.wm.save_as_mainfile(filepath=str(target/(id+'.blend')),compress=True);s.render_family(id,[obj],path=target/(id+'.png'),view=(4,-6,4))
else:
 b.export(id,[obj],'campground-organic-memorial',view=(4,-6,4))
 original=obj.data;obj.data=original.copy()
 for name in [u.name for u in obj.data.uv_layers if u.name!='AuthoredWearAtlas']:obj.data.uv_layers.remove(obj.data.uv_layers[name])
 file=ROOT/f'assets/runtime/models/{id}.glb';bpy.context.view_layer.objects.active=obj;bpy.ops.object.select_all(action='DESELECT');obj.select_set(True);bpy.ops.export_scene.gltf(filepath=str(file),export_format='GLB',use_selection=True,export_yup=True,export_extras=True)
 disposable=obj.data;obj.data=original;bpy.data.meshes.remove(disposable);b.LOG[-1]['sha256']=hashlib.sha256(file.read_bytes()).hexdigest();b.LOG[-1]['bytes']=file.stat().st_size
 (ROOT/'development/levels/hollow-creek/organic-memorial-model.json').write_text(json.dumps(dict(status='source construction; independent checks pending',asset=b.LOG[-1],parent=parent.relative_to(ROOT).as_posix(),parent_sha256=protected,inscription=[p[0] for p in inscription],cast_before_reduction_triangles=before,uv_goose_encounter_status='requested; not implemented by this sculpture export'),indent=2)+'\n',encoding='utf-8')
print('SCULPTED',id,'prototype' if prototype else 'exported','from',before,'cast triangles; parent preserved',flush=True)
