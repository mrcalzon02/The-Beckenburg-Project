"""First additive Hollow Creek facility kit. Original CC0, metre floor pivots.
Preserves the existing layout and rough-shell files; adds a separate native fit-out.
Blender --background --factory-startup --python-exit-code 1 --python this.py
"""
import bpy,json,sys,math,hashlib
from pathlib import Path
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[2]
sys.path[:0]=[str(ROOT/'development/levels'),str(ROOT/'development/assets')]
import build_lived_assets as b
import rebuild_support as s
OUT=ROOT/'development/levels/hollow-creek';OUT.mkdir(exist_ok=True)
PRE=ROOT/'development/levels/previews/hollow-creek';PRE.mkdir(exist_ok=True)
ROUGH=ROOT/'development/levels/blockouts/wld-02/rough-shell.blend'
LAYOUT=ROOT/'development/levels/layouts/wld-02/layout.json'
original={str(p.relative_to(ROOT)):hashlib.sha256(p.read_bytes()).hexdigest() for p in [ROUGH,LAYOUT,*sorted((ROOT/'development/levels/blockouts').glob('*/rough-shell.glb'))]}

def picnic_table():
    m=b.init();p=[]
    for y in [-.32,-.16,0,.16,.32]:p.append(s.box('Tabletop slat',(0,y,.74),(2.44,.15,.04),m['pine'],.003))
    for side in [-1,1]:
        for offset in [-.0725,.0725]:p.append(s.box('Bench slat',(0,side*.66+offset,.43),(1.80,.135,.04),m['oak'],.003))
    for x in [-.5,.5]:
        p.append(s.box('Tabletop bearing rail',(x,0,.68),(.12,.86,.08),m['oak']))
        p.append(s.box('Bench bearing crossmember',(x,0,.382),(.12,1.58,.056),m['oak']))
        for side in [-1,1]:
            p.append(s.box('Ground-bearing foot shoe',(x,side*.70,.03),(.18,.20,.06),m['oak']))
            p.append(s.rod('Splayed support leg',(x,side*.70,.06),(x,side*.28,.70),.055,m['oak'],6))
            for height,y in [(.39,side*.49),(.67,side*.30)]:
                p.append(s.rod('Coach bolt',(x-.07,y,height),(x+.07,y,height),.016,m['steel'],8))
                p.append(s.rod('Bearing washer',(x+.061,y,height),(x+.073,y,height),.027,m['steel'],10))
    p.append(s.box('Central longitudinal stretcher',(0,0,.34),(1.0,.10,.10),m['oak']))
    for side in [-1,1]:p.append(s.rod('Frame triangulation',(0,0,.35),(side*.5,0,.68),.035,m['oak'],6))
    b.export('level_camp_picnic_table',p,'campground',view=(3,-4,2.5))

def fire_ring():
    m=b.init();p=[];verts=[];faces=[];count=32
    for height in [0,.35]:
        for radius in [.48,.51]:
            for i in range(count):a=i*math.tau/count;verts.append((radius*math.cos(a),radius*math.sin(a),height))
    for i in range(count):
        j=(i+1)%count
        faces.extend([(i,j,count+j,count+i),(2*count+i,3*count+i,3*count+j,2*count+j),(count+i,count+j,3*count+j,3*count+i),(i,2*count+i,2*count+j,j)])
    mesh=bpy.data.meshes.new('Open ring wall');mesh.from_pydata(verts,[],faces);mesh.update();obj=bpy.data.objects.new('Open steel fire ring',mesh);bpy.context.collection.objects.link(obj);mesh.materials.append(m['rust']);s.uv_project(obj);p.append(obj)
    p+=b.ring('Rolled upper ring edge',(0,0,.35),.505,.015,m['steel'],axis='Z',segments=32)
    p.append(s.rod('Cold ash bed',(0,0,0),(0,0,.018),.455,m['black'],32))
    for angle in [0,math.pi/2,math.pi,3*math.pi/2]:
        x=.475*math.cos(angle);y=.475*math.sin(angle)
        lug=s.box('Grate support lug',(x,y,.35),(.14,.06,.04),m['iron']);lug.rotation_euler.z=angle;p.append(lug)
    p+=b.ring('Grate perimeter',(0,0,.38),.46,.012,m['iron'],axis='Z',segments=24)
    for x in [-.36,-.24,-.12,0,.12,.24,.36]:
        length=math.sqrt(.46*.46-x*x);p.append(s.rod('Cooking grate bar',(x,-length,.38),(x,length,.38),.009,m['iron'],6))
    for x,y,yaw in [(-.12,0,.35),(.15,.09,-.5),(.03,-.14,1.2)]:
        dx=.28*math.cos(yaw);dy=.28*math.sin(yaw);p.append(s.rod('Cold charred timber',(x-dx,y-dy,.075),(x+dx,y+dy,.075),.045,m['black'],7))
    b.export('level_camp_fire_ring',p,'campground',view=(2,-3,2.3))

picnic_table();fire_ring()
assets=list(b.LOG)
for a in assets:
    a['stage']='native facility kit; runtime placement/collision pending'

bpy.ops.wm.open_mainfile(filepath=str(ROUGH));bpy.context.preferences.filepaths.save_version=0
scene=bpy.context.scene
collection=bpy.data.collections.new('HOLLOW CREEK additive facility pass 01');scene.collection.children.link(collection)
placements=[dict(id='west-table',model='level_camp_picnic_table',zone='camp-a',x=16.45,z=32.3,y=0),dict(id='pavilion-table-west',model='level_camp_picnic_table',zone='pavilion',x=28,z=37,y=0),dict(id='pavilion-table-east',model='level_camp_picnic_table',zone='pavilion',x=36,z=37,y=0),dict(id='west-fire-ring',model='level_camp_fire_ring',zone='camp-a',x=10,z=41.5,y=0)]
templates={}
for a in assets:
    with bpy.data.libraries.load(str(ROOT/a['editable']),link=False) as (incoming,outgoing):outgoing.objects=[a['id']]
    templates[a['id']]=outgoing.objects[0]
for p in placements:
    obj=templates[p['model']].copy();obj.data=templates[p['model']].data.copy();collection.objects.link(obj);obj.name=p['id'];obj.location=(p['x'],-p['z'],p['y']);obj['facility_id']=p['id'];obj['zone']=p['zone'];obj['runtime_status']='not yet integrated'
    p['native_object']=obj.name
# Native reservations are metadata, not invented playable tent/collision geometry.
for name,x,z,hx,hz in [('West tent pad 4.88m square',9.5,32.5,2.44,2.44),('Fire-ring service reservation',10,41.5,1.6,1.6),('Pavilion clear ritual footprint',32,42,1.2,1.2)]:
    obj=bpy.data.objects.new(name,None);collection.objects.link(obj);obj.location=(x,-z,0);obj.empty_display_type='CUBE';obj.empty_display_size=1;obj.scale=(hx,hz,.02);obj['role']='clear-space reservation; not a new runtime model'
scene['production_status']='Hollow Creek facility fit-out pass 01; original rough shell preserved; runtime acceptance pending'
if scene.camera is None:
    bpy.ops.object.camera_add();scene.camera=bpy.context.object
if not any(o.type=='LIGHT' for o in scene.objects):
    bpy.ops.object.light_add(type='SUN');bpy.context.object.rotation_euler=(.35,-.45,-.6);bpy.context.object.data.energy=2.0
if scene.world is None:scene.world=bpy.data.worlds.new('Native inspection daylight')
scene.world.use_nodes=True;scene.world.node_tree.nodes['Background'].inputs[1].default_value=.4
target=Vector((15.0,-33.0,.4));scene.camera.location=(21,-27,4.6);scene.camera.rotation_euler=(target-scene.camera.location).to_track_quat('-Z','Y').to_euler();scene.camera.data.type='PERSP';scene.camera.data.lens=44
scene.render.engine='CYCLES';scene.cycles.samples=16;scene.cycles.use_denoising=True;scene.render.resolution_x=1120;scene.render.resolution_y=700;scene.render.resolution_percentage=100
bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'hollow-creek-facilities.blend'),compress=True)
scene.render.image_settings.file_format='PNG';scene.render.filepath=str(PRE/'west-campsite-native-fitout.png');bpy.ops.render.render(write_still=True)
target=Vector((32,-37,.4));scene.camera.location=(32,-42,8);scene.camera.data.lens=28;scene.camera.rotation_euler=(target-scene.camera.location).to_track_quat('-Z','Y').to_euler();scene.render.filepath=str(PRE/'pavilion-native-fitout.png');bpy.ops.render.render(write_still=True)
report=dict(status='native-facility-fitout-only',source_hashes=original,assets=assets,placements=placements,reservations=[dict(id='tent-pad',x=9.5,z=32.5,width=4.88,depth=4.88),dict(id='ritual',x=32,z=42,width=2.4,depth=2.4)],limitations=['No runtime/collision/AI/LAN map integration or unlock.','Original rough geometry and connector reservations remain provisional.','Tent, bathhouse, dock, bridge and open-pavilion architectural refinement remain pending.'])
(OUT/'facilities.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
for name,digest in original.items():assert hashlib.sha256((ROOT/name).read_bytes()).hexdigest()==digest,name
print('PASS original layouts and 17 rough GLBs unchanged; 2 original models and 4 native placements added',flush=True)
