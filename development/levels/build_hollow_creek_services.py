"""Supported pavilion utility kit; reuses original utility fixtures without rewriting them."""
import bpy,json,sys,hashlib
from pathlib import Path
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[2];sys.path[:0]=[str(ROOT/'development/levels'),str(ROOT/'development/assets')]
import build_lived_assets as b
import rebuild_support as s
OUT=ROOT/'development/levels/hollow-creek';PRE=ROOT/'development/levels/previews/hollow-creek'
paths=[OUT/'hollow-creek-pavilion.blend',OUT/'pavilion.json',ROOT/'development/levels/blender/level_utility_light.blend',ROOT/'assets/runtime/models/level_utility_light.glb']
protected={p.relative_to(ROOT).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in paths}
m=b.init();parts=[];contacts=[]
def box(name,position,size,mat):
 obj=s.box(name,position,size,mat);parts.append(obj);return obj
def rod(name,a,c,r,mat):
 obj=s.rod(name,a,c,r,mat,8);parts.append(obj);return obj
# Blender X is world X; local Y is minus world Z. Origin remains pavilion floor centre.
# Mount back = local X-5.85, the inside face of the 300mm west post.
box('Switch backplate',(-5.835,0,1.35),(.03,.22,.32),m['iron'])
box('Weatherproof switch enclosure',(-5.785,0,1.35),(.07,.19,.27),m['enamel'])
box('Switch rocker',(-5.742,0,1.36),(.016,.07,.10),m['black'])
for y in [-.075,.075]:
 for z in [1.25,1.45]:rod('Switch screw',(-5.735,y,z),(-5.72,y,z),.008,m['steel'])
rod('Top cable gland',(-5.785,0,1.485),(-5.785,0,1.53),.025,m['iron'])
rod('Post conduit',(-5.785,0,1.52),(-5.785,0,3.075),.014,m['iron'])
for z in [1.65,2.25,2.85]:
 box('Post conduit saddle '+str(z),(-5.82,0,z),(.06,.07,.035),m['steel'])
 contacts += [['Post conduit','Post conduit saddle '+str(z)]]
# Joints are real connected segments, supported by the underside of tie/longitudinal timber.
rod('Tie conduit',(-5.785,0,3.075),(-.69,0,3.075),.014,m['iron'])
# The 3 fixtures are supported directly at the original tie-beam underside Y3.10.
# Along-Z trunk sits at X=-0.69 to clear the fixture housings rather than pass through them.
rod('Longitudinal trunk',(-.69,-6,3.075),(-.69,6,3.075),.014,m['iron'])
for y in [-6,0,6]:
 box('Fixture junction '+str(y),(-.69,y,3.075),(.10,.10,.05),m['iron'])
 rod('Fixture feed '+str(y),(-.69,y,3.075),(-.55,y,3.075),.014,m['iron'])
 for x in [-4,-2]:
  if y==0:box('Tie conduit saddle '+str(x),(x,0,3.09),(.06,.06,.025),m['steel'])
# Dedicated narrow timber bearer supports the long trunk between existing trusses.
# This is visible authored support, not a floating twelve-metre pipe.
box('Conduit timber bearer',(-.69,0,3.145),(.16,12.1,.09),m['oak'])
contacts += [['Post conduit','Top cable gland'],['Post conduit','Tie conduit'],['Tie conduit','Longitudinal trunk'],['Longitudinal trunk','Conduit timber bearer']]
b.export('level_camp_pavilion_services',parts,'campground',view=(13,-18,10))
asset=b.LOG[-1];asset['mount']='Pavilion floor-centre origin; switch back local X=-5.85, Y1.35; fixture mounts local X0, Y3.10, Z -6/0/6. Do not floor-normalize.'
bpy.ops.wm.open_mainfile(filepath=str(OUT/'hollow-creek-pavilion.blend'));bpy.context.preferences.filepaths.save_version=0
with bpy.data.libraries.load(str(ROOT/asset['editable']),link=False) as (incoming,outgoing):outgoing.objects=[asset['id']]
kit=outgoing.objects[0];bpy.context.scene.collection.objects.link(kit);kit.location=(32,-42,0);kit['role']='supported utility kit; runtime wiring pending'
with bpy.data.libraries.load(str(ROOT/'development/levels/blender/level_utility_light.blend'),link=False) as (incoming,outgoing):outgoing.objects=['level_utility_light']
template=outgoing.objects[0];fixtures=[]
for z in [36,42,48]:
 obj=template.copy();obj.data=template.data.copy();bpy.context.scene.collection.objects.link(obj);obj.name='Pavilion utility fixture '+str(z);obj.location=(32,-z,3.10);obj['circuit']='pavilion-preview';obj['role']='source-preserved utility fixture'
 fixtures.append(dict(id=obj.name,model='level_utility_light',x=32,y=3.10,z=z,support='Tie beam '+str(42-z)))
 scene=bpy.context.scene
 bpy.ops.object.light_add(type='AREA',location=(32,-z,2.935));light=bpy.context.object;light.name='Preview emitter '+str(z);light.data.energy=180;light.data.shape='RECTANGLE';light.data.size=1.05;light.data.size_y=.12;light['role']='native preview emitter; runtime lighting not accepted';light['circuit']='pavilion-preview'
scene=bpy.context.scene;scene['production_status']='Native pavilion mounted utilities; runtime circuit integration pending'
scene.render.engine='CYCLES';scene.cycles.samples=32;scene.cycles.use_denoising=True;scene.render.resolution_x=1120;scene.render.resolution_y=700;scene.render.resolution_percentage=100;scene.render.image_settings.file_format='PNG'
scene.camera.location=(32,-46,1.62);scene.camera.rotation_euler=(Vector((32,-37,2.0))-scene.camera.location).to_track_quat('-Z','Y').to_euler();scene.camera.data.lens=23
# Ordinary inspection image: existing labeled inspection fill follows camera.
fill=bpy.data.objects.get('Inspection-only camera fill')
if fill:fill.location=scene.camera.location;fill.rotation_euler=scene.camera.rotation_euler;fill.data.energy=600
scene.render.filepath=str(PRE/'pavilion-mounted-utilities-native-fitout.png');bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'hollow-creek-services.blend'),compress=True);bpy.ops.render.render(write_still=True)
# Matched night captures use only preview fixtures/world, never the inspection fill.
if fill:fill.data.energy=0
for light in [o for o in scene.objects if o.type=='LIGHT' and o.data.type=='SUN']:light.data.energy=0
scene.world.node_tree.nodes['Background'].inputs[1].default_value=.025
for state in ['on','off']:
 for light in [o for o in scene.objects if o.get('circuit')=='pavilion-preview' and o.type=='LIGHT']:light.data.energy=180 if state=='on' else 0
 scene.render.filepath=str(PRE/('pavilion-native-circuit-'+state+'.png'));bpy.ops.render.render(write_still=True)
report=dict(status='native-mounted-utilities-only',asset=asset,fixtures=fixtures,contacts=contacts,placement=dict(x=32,y=0,z=42),protected_sources=protected,preview_only=True,limitations=['No runtime circuit/interaction or multiplayer integration yet.','Native Cycles emitter previews do not certify game renderer light separation.','Circuit feed/breaker ownership will connect to authored ranger services during full-map integration.'])
(OUT/'services.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
for path,digest in protected.items():assert hashlib.sha256((ROOT/path).read_bytes()).hexdigest()==digest,path
print('PASS supported utility kit and three source-preserved fixtures; paired native circuit previews',flush=True)
