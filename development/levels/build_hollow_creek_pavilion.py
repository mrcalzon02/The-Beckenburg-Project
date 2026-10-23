"""Additive pavilion architecture pass. Does not rewrite any existing map/blockout."""
import bpy,bmesh,json,sys,math,hashlib
from pathlib import Path
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[2]
sys.path[:0]=[str(ROOT/'development/levels'),str(ROOT/'development/assets')]
import build_lived_assets as b
import rebuild_support as s
OUT=ROOT/'development/levels/hollow-creek';PRE=ROOT/'development/levels/previews/hollow-creek'
prior=json.loads((OUT/'facilities.json').read_text(encoding='utf-8'))
protected=[OUT/'hollow-creek-facilities.blend',OUT/'facilities.json',*[ROOT/path for path in prior['source_hashes']],*[ROOT/a['editable'] for a in prior['assets']],*[ROOT/f"assets/runtime/models/{a['id']}.glb" for a in prior['assets']]]
source_hashes={p.relative_to(ROOT).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in protected}
m=b.init();parts=[];contacts=[]
concrete=s.material('Camp pavilion concrete',(.30,.31,.285),'paint')
roofmat=s.material('Camp pavilion green roof',(.10,.15,.12),'paint')
def box(name,center,size,material):
 obj=s.box(name,center,size,material);parts.append(obj);return obj
def rod(name,a,c,r,material):
 obj=s.rod(name,a,c,r,material,8);parts.append(obj);return obj
def contact(a,c):contacts.append([a,c])
box('Slab closed solid',(0,0,.09),(14,14,.18),concrete)
colliders=[]
for x in [-6,6]:
 for y in [-6,0,6]:
  key=f'{x}_{y}';shoe='Post shoe '+key;post='Timber post '+key
  box(shoe,(x,y,.25),(.44,.44,.14),m['iron']);box(post,(x,y,1.79),(.30,.30,2.98),m['oak'])
  contact('Slab closed solid',shoe);contact(shoe,post)
  colliders.append(dict(id=post,x=x,z=-y,width=.44,depth=.44,bottom=0,top=3.28))
  for side in [-1,1]:
   rod('Shoe bolt '+key+str(side),(x-.24,y+side*.10,.27),(x+.24,y+side*.10,.27),.014,m['steel'])
  for direction in [-1,1]:
   if abs(y+direction*.8)>6.1:continue
   brace='Longitudinal knee brace '+key+str(direction)
   rod(brace,(x,y,2.58),(x,y+direction*.80,3.36),.075,m['oak'])
   contact(post,brace);contact(brace,'Eave bearing '+str(x))
 box('Eave bearing '+str(x),(x,0,3.43),(.30,12.6,.30),m['oak'])
 for y in [-6,0,6]:contact('Timber post '+f'{x}_{y}','Eave bearing '+str(x))
box('Ridge bearing',(0,0,5.21),(.25,12.6,.30),m['oak'])
for y in [-6,0,6]:
 tie='Tie beam '+str(y);king='King post '+str(y)
 box(tie,(0,y,3.38),(12.30,.24,.20),m['oak']);box(king,(0,y,4.27),(.22,.22,1.58),m['oak'])
 contact(tie,king);contact(king,'Ridge bearing')
 for side in [-1,1]:
  contact(tie,'Eave bearing '+str(side*6))
  name='Truss diagonal '+str(y)+'_'+str(side)
  rod(name,(side*5.8,y,3.43),(0,y,5.20),.085,m['oak']);contact(tie,name);contact(name,'Ridge bearing')
# Every rafter has a real eave and ridge bearing and contacts the closed soffit.
for index in range(17):
 y=-6.8+index*.85
 for side in [-1,1]:
  name='Rafter '+str(index)+'_'+str(side)
  rod(name,(0,y,5.36),(side*7,y,3.36),.12,m['pine'])
  if abs(y)<=6.3:
   contact(name,'Eave bearing '+str(side*6));contact(name,'Ridge bearing')
  else:
   # Overhanging end rafters are carried by longitudinal purlins below.
   contact(name,'Purlin '+str(side))
# Purlins support the end overhang and intersect all rafters.
for side in [-1,1]:
 box('Purlin '+str(side),(side*3.5,0,4.27),(.20,14,.22),m['oak'])
 contact('Purlin '+str(side),'Truss diagonal 0_'+str(side))
# One watertight cross-section: no coplanar roof planes or open ridge seam.
section=[(-7,3.60),(0,5.60),(7,3.60),(7,3.48),(0,5.48),(-7,3.48)]
vertices=[(x,y,z) for y in [-7,7] for x,z in section]
faces=[tuple(range(5,-1,-1)),tuple(range(6,12))]+[(i,(i+1)%6,(i+1)%6+6,i+6) for i in range(6)]
mesh=bpy.data.meshes.new('Closed roof cross section');mesh.from_pydata(vertices,[],faces);mesh.update()
roof=bpy.data.objects.new('Roof closed solid',mesh);bpy.context.collection.objects.link(roof);mesh.materials.append(roofmat)
bm=bmesh.new();bm.from_mesh(mesh);bmesh.ops.recalc_face_normals(bm,faces=bm.faces);bm.to_mesh(mesh);bm.free();s.uv_project(roof);parts.append(roof)
for index in range(17):
 for side in [-1,1]:contact('Rafter '+str(index)+'_'+str(side),'Roof closed solid')
# Raised ridge cap is physical weathering detail, not black/gray texture striping.
rod('Ridge weather cap',(0,-7.02,5.61),(0,7.02,5.61),.075,m['iron'])
# Edge fascia closes visible rafter ends; connections overlap within timber joints.
for side in [-1,1]:box('Eave fascia '+str(side),(side*7,0,3.43),(.10,14,.26),m['oak'])
b.export('level_camp_pavilion',parts,'campground',view=(18,-22,14))
asset=b.LOG[-1]
# Open prior additive scene; replace only the pavilion placeholder in this new copy.
bpy.ops.wm.open_mainfile(filepath=str(OUT/'hollow-creek-facilities.blend'));bpy.context.preferences.filepaths.save_version=0
removed=[]
for obj in list(bpy.context.scene.objects):
 if obj.name.startswith(('Floor pavilion','Wall pavilion','Header pavilion')) or obj.name=='pavilion Dining pavilion':
  removed.append(obj.name);bpy.data.objects.remove(obj,do_unlink=True)
with bpy.data.libraries.load(str(ROOT/asset['editable']),link=False) as (incoming,outgoing):outgoing.objects=[asset['id']]
obj=outgoing.objects[0];bpy.context.scene.collection.objects.link(obj);obj.location=(32,-42,-.18);obj['zone']='pavilion';obj['role']='refined open shelter; runtime integration pending'
concrete=s.material('Camp pavilion concrete',(.30,.31,.285),'paint')
# Non-overlapping apron strips retain the original 16m-zone standing Y=0.
for name,center,size in [('West',(24.5,-42,-.08),(1,16,.16)),('East',(39.5,-42,-.08),(1,16,.16)),('North',(32,-34.5,-.08),(14,1,.16)),('South',(32,-49.5,-.08),(14,1,.16))]:
 ground=s.box('Pavilion apron '+name,center,size,concrete);ground['zone']='pavilion';ground['role']='non-overlapping apron'
scene=bpy.context.scene;scene['production_status']='Open pavilion architecture native pass; full map integration pending'
scene.render.engine='CYCLES';scene.cycles.samples=24;scene.cycles.use_denoising=True
scene.render.resolution_x=1120;scene.render.resolution_y=700;scene.render.resolution_percentage=100;scene.render.image_settings.file_format='PNG'
bpy.ops.object.light_add(type='AREA');inspection=bpy.context.object;inspection.name='Inspection-only camera fill';inspection.data.energy=1800;inspection.data.shape='DISK';inspection.data.size=5;inspection['role']='authoring inspection light; not a gameplay fixture'
def camera(name,position,target,lens):
 scene.camera.location=position;scene.camera.rotation_euler=(Vector(target)-scene.camera.location).to_track_quat('-Z','Y').to_euler();scene.camera.data.type='PERSP';scene.camera.data.lens=lens
 inspection.location=position;inspection.rotation_euler=scene.camera.rotation_euler
 scene.render.filepath=str(PRE/(name+'.png'))
camera('pavilion-structure-native-fitout',(46,-57,10),(32,-42,2),34)
bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'hollow-creek-pavilion.blend'),compress=True);bpy.ops.render.render(write_still=True)
camera('pavilion-under-roof-native-fitout',(32,-47,1.62),(32,-37,2.2),22);bpy.ops.render.render(write_still=True)
camera('pavilion-join-native-fitout',(27,-37,2.2),(26,-36,3.3),28);bpy.ops.render.render(write_still=True)
report=dict(status='native-pavilion-architecture-only',asset=asset,placement=dict(x=32,y=-.18,z=42),standing_y=0,roof_vertical_depth=.12,posts=colliders,contacts=contacts,replaced_in_derived_scene=removed,protected_sources=source_hashes,limitations=['No runtime map/collision/AI/LAN integration or unlock.','Native structural contact and passage checks are not engineering certification.','Pavilion utilities, detail dressing and full site terrain remain next gates.'])
(OUT/'pavilion.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
for path,digest in source_hashes.items():assert hashlib.sha256((ROOT/path).read_bytes()).hexdigest()==digest,path
print('PASS open pavilion authored; prior facilities/layouts/blockouts unchanged',flush=True)
