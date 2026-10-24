"""User-directed destinations, more camps, masonry hearth and creek engineering.
Additive native fitout; previous accepted scene and runtime source assets intact.
"""
import bpy,sys,json,math,random,hashlib,bmesh
from pathlib import Path
from mathutils import Vector
from mathutils.bvhtree import BVHTree
ROOT=Path(__file__).resolve().parents[2];sys.path[:0]=[str(ROOT/'development/levels'),str(ROOT/'development/assets')]
import rebuild_support as s
import build_lived_assets as b
import hollow_creek_organic as organic
import hollow_creek_ground as old
OUT=ROOT/'development/levels/hollow-creek';PRE=ROOT/'development/levels/previews/hollow-creek'
def cubicle(opened):
 m=b.init();plastic=s.material('Muted sanitary moulded shell',(.15,.28,.26),'paint');p=[s.box('Raised cubicle floor',(0,0,.055),(1.35,1.5,.11),m['black'],.02)]
 for x in [-.65,.65]:
  p.append(s.box('Solid moulded wall',(x,0,1.14),(.05,1.5,2.17),plastic,.015))
  for y in [-.60,-.3,0,.3,.60]:p.append(s.box('Moulded wall stiffener',(x*1.035,y,1.08),(.025,.04,1.85),plastic,.008))
 p.append(s.box('Cubicle back',(0,.72,1.14),(1.3,.06,2.17),plastic,.015));p.append(s.box('Closed pitched-cap roof',(0,0,2.26),(1.44,1.59,.16),m['enamel'],.04))
 for x in [-.585,.585]:p.append(s.box('Door frame jamb',(x,-.72,1.15),(.13,.08,2.12),plastic,.012))
 p.append(s.box('Door frame head',(0,-.72,2.14),(1.1,.08,.15),plastic))
 door=[s.box('Physical door leaf',(0,-.77,1.10),(1.02,.065,1.98),plastic,.018),s.box('Door handle',(.38,-.825,1.12),(.055,.045,.18),m['steel'],.008),s.box('Occupancy plate',(.30,-.81,1.49),(.12,.025,.10),m['ivory'])]
 for z in [.40,1.1,1.8]:door.append(s.box('Door hinge',(-.51,-.81,z),(.07,.055,.10),m['steel']))
 if opened:
  pivot=Vector((-.51,-.77,0));angle=-math.pi*.53
  for o in door:
   bpy.context.view_layer.objects.active=o;o.select_set(True);bpy.ops.object.transform_apply(location=True,rotation=True,scale=True);o.select_set(False)
   for v in o.data.vertices:
    q=v.co-pivot;v.co=pivot+Vector((q.x*math.cos(angle)-q.y*math.sin(angle),q.x*math.sin(angle)+q.y*math.cos(angle),q.z))
 p+=door
 p.append(s.box('Actual waste reservoir',(0,.34,.31),(.9,.69,.40),plastic,.06))
 p+=b.ring('Actual open toilet seat',(0,.20,.54),.25,.045,m['ivory'],axis='Z',segments=24)
 p.append(s.box('Toilet seat hinge',(0,.48,.55),(.40,.07,.04),m['ivory']))
 p.append(s.box('Paper dispenser',(-.56,.10,.86),(.13,.18,.19),m['enamel'],.02))
 p.append(s.rod('Waste vent pipe',(.48,.53,.45),(.48,.53,2.48),.048,m['black'],12));p.append(s.box('Vent rain cap',(.48,.53,2.50),(.20,.20,.04),m['black']))
 b.export('level_camp_portable_toilet_'+('open' if opened else 'closed'),p,'campground',view=(4,-6,3.5))
def bin_model():
 m=b.init();p=[s.box('Large refuse bin body',(0,0,.57),(.74,.80,1.00),m['green'],.045),s.box('Closed hinged refuse lid',(0,0,1.105),(.80,.86,.09),m['black'],.03)]
 for x in [-.29,.29]:p.append(s.rod('Grounded refuse wheel',(x-.045,.28,.10),(x+.045,.28,.10),.10,m['black'],14))
 p.append(s.rod('Rear handle',(-.30,.45,.96),(.30,.45,.96),.028,m['steel'],10))
 for x in [-.25,.25]:p.append(s.box('Front stabilising foot',(x,-.26,.055),(.12,.12,.11),m['black']))
 p.append(s.box('Refuse label plate',(0,-.407,.72),(.32,.015,.20),m['enamel']))
 b.export('level_camp_refuse_bin',p,'campground',view=(3,-4,2.5))
def statue():
 m=b.init();stone=s.material('Memorial fine sandstone',(.36,.35,.31),'paint');bronze=s.material('Patinated memorial bronze',(.16,.24,.20),'metal');p=[]
 for z,width,depth in [(.09,1.45,.18),(.25,1.17,.14),(.75,.86,.86),(1.22,1.08,.10)]:p.append(s.box('Memorial stone plinth',(0,0,z),(width,width,depth),stone,.025))
 p.append(s.box('Memorial inscription plaque',(0,-.445,.80),(.58,.026,.35),m['brass'],.012))
 # Original carved heron, actual slender legs, curved neck, beak and feathered wings.
 for x in [-.11,.11]:
  p.append(s.rod('Heron slender leg',(x,0,1.27),(x,.03,1.95),.026,bronze,10))
  p.append(s.rod('Heron grounded foot',(x,-.12,1.285),(x,.13,1.285),.027,bronze,8))
 body=s.lathe('Heron bronze body',[(1.84,.08),(1.94,.23),(2.17,.20),(2.30,.07)],bronze,20);p.append(body)
 neck=[(0,-.05,2.17),(0,-.15,2.39),(0,.02,2.58),(0,-.04,2.80),(0,-.17,2.89)]
 p+=s.tube('Curved heron neck',neck,.062,bronze,10);p.append(s.rod('Heron long bill',(0,-.17,2.89),(0,-.52,2.86),.055,bronze,10,end_radius=.004))
 for side in [-1,1]:
  for i in range(9):
   a=(side*.10,.02,2.11);tip=(side*(.55+i*.065),.10+i*.045,2.40-i*.04);p.append(s.rod('Layered bronze wing feather',a,tip,.045,bronze,8,end_radius=.012))
 b.export('level_camp_heron_memorial',p,'campground',view=(4,-6,4))
def fireplace():
 m=b.init();stone=s.material('Pavilion masonry fine stone',(.30,.29,.26),'paint');p=[]
 p.append(s.box('Fireproof projecting hearth',(0,-.2,.12),(3.0,3.7,.24),stone,.025))
 for x in [-1.15,1.15]:p.append(s.box('Massive masonry fireplace cheek',(x,.15,1.02),(.50,1.7,1.8),stone,.025))
 p.append(s.box('Solid firebox back',(0,.91,1.0),(1.8,.20,1.75),m['black']))
 p.append(s.box('Lintel and mantel',(0,.15,1.99),(3.0,2.0,.30),stone,.025))
 # Rear flue sits beyond pavilion eave, avoiding a false uncut roof penetration.
 p.append(s.box('Smoke gathering rear hood',(0,.83,2.28),(1.8,1.6,.30),stone,.025))
 for x in [-.58,.58]:p.append(s.box('Rear flue side',(x,1.40,4.03),(.24,.90,3.26),stone))
 for y in [1.07,1.73]:p.append(s.box('Rear flue front and back',(0,y,4.03),(.92,.24,3.26),stone))
 p.append(s.box('Flue weather cap',(0,1.40,5.80),(1.65,1.45,.15),stone,.02))
 for x in [-.56,.56]:p.append(s.rod('Cap support',(x,1.4,5.60),(x,1.4,5.75),.045,m['iron'],8))
 for y in [-.25,.2,.60]:p.append(s.rod('Cold split firewood',(-.65,y,.36),(.65,y+.06,.38),.10,m['oak'],10))
 for x in [-.7,-.35,0,.35,.7]:p.append(s.rod('Supported fire grate',(x,-.55,.48),(x,.63,.48),.019,m['iron'],8))
 b.export('level_camp_pavilion_fireplace',p,'campground',view=(6,-8,6))
def decorative_shrub(variant):
 m=b.init();rng=random.Random(370+variant);bark=s.material('Decorative shrub fine bark',(.19,.15,.10),'paint');greens=[s.material('Ordinary broadleaf '+str(k),c,'paint') for k,c in enumerate([(.12,.24,.08),(.17,.29,.10),(.20,.32,.12)])];p=[]
 for i in range(35):
  a=i*2.39996;radius=rng.uniform(.65,1.22);h=rng.uniform(.95,1.80)+variant*.15;base=Vector((rng.uniform(-.17,.17),rng.uniform(-.17,.17),0));tip=Vector((radius*math.cos(a),radius*math.sin(a),h));p.append(s.rod('Decorative woody stem',base,tip,.025,bark,7,end_radius=.008))
  vv=[];ff=[]
  for j in range(25):
   t=rng.uniform(.42,1);c=base.lerp(tip,t)+Vector((rng.uniform(-.24,.24),rng.uniform(-.24,.24),rng.uniform(-.13,.13)));a2=rng.random()*math.tau;axis=Vector((math.cos(a2),math.sin(a2),rng.uniform(-.25,.4)))*rng.uniform(.12,.20);side=Vector((-math.sin(a2),math.cos(a2),.3))*rng.uniform(.045,.075);n=len(vv);vv.extend([tuple(c-axis*.45),tuple(c+side),tuple(c+axis),tuple(c-side),tuple(c+Vector((0,0,.025)))]);ff.extend([(n,n+1,n+4),(n+1,n+2,n+4),(n+2,n+3,n+4),(n+3,n,n+4)])
  p.append(s.mesh('Individual ordinary shrub leaves',vv,ff,greens[i%3]))
 b.export('level_camp_shrub_decorative_'+('a' if variant==0 else 'b'),p,'campground',view=(4,-5,3))
for fn in [lambda:cubicle(True),lambda:cubicle(False),bin_model,statue,fireplace,lambda:decorative_shrub(0),lambda:decorative_shrub(1)]:fn()
assets=b.LOG[-7:];source=OUT/'hollow-creek-dock.blend';protected={source.relative_to(ROOT).as_posix():hashlib.sha256(source.read_bytes()).hexdigest()};bpy.ops.wm.open_mainfile(filepath=str(source));bpy.context.preferences.filepaths.save_version=0;scene=bpy.context.scene;d=json.loads((OUT/'organic-terrain.json').read_text(encoding='utf-8'))
terrain=bpy.data.objects['level_camp_terrain_shore'];ground=BVHTree.FromPolygons([terrain.matrix_world@v.co for v in terrain.data.vertices],[tuple(p.vertices) for p in terrain.data.polygons])
def height(x,z):
 p=ground.ray_cast(Vector((x,-z,10)),Vector((0,0,-1)),40)[0];assert p is not None,(x,z);return p.z
placements=[];supports=[]
def place(id,name,x,z,y,yaw=0,conform=False,scale=1):
 with bpy.data.libraries.load(str(ROOT/f'development/levels/blender/{id}.blend'),link=False) as (src,dst):dst.objects=[id]
 o=dst.objects[0];scene.collection.objects.link(o);o.name=name;o.data=o.data.copy();o.location=(x,-z,y);o.rotation_euler.z=yaw;o.scale=(scale,scale,scale)
 if conform and 'tent_' in id:
  bm=bmesh.new();bm.from_mesh(o.data);deform=bm.verts.layers.deform.active;ids={g.index for g in o.vertex_groups if 'groundsheet' in g.name.lower() or 'physical floor' in g.name.lower()};faces=[f for f in bm.faces if all(any(i in v[deform] for i in ids) for v in f.verts)];bmesh.ops.triangulate(bm,faces=faces);faces=[f for f in bm.faces if all(any(i in v[deform] for i in ids) for v in f.verts)];edges=list({e for f in faces for e in f.edges});bmesh.ops.subdivide_edges(bm,edges=edges,cuts=20,use_grid_fill=True);bm.to_mesh(o.data);bm.free()
 if conform:
  for v in o.data.vertices:
   wx=x+scale*(math.cos(yaw)*v.co.x-math.sin(yaw)*v.co.y);wz=z-scale*(math.sin(yaw)*v.co.x+math.cos(yaw)*v.co.y);v.co.z+=(height(wx,wz)-y)/scale
 placements.append(dict(asset=id,name=name,x=x,z=z,y=y,yaw=yaw,scale=scale,terrain_fitted=conform));return o
stone=s.material('Site footing stone',(.30,.29,.25),'paint');wood=s.material('Site supported tread timber',(.23,.18,.10),'wood')
def footings(name,x,z,y,dx,dz):
 for sx in [-dx,dx]:
  for sz in [-dz,dz]:
   g=height(x+sx,z+sz);o=s.box(name+' support',(x+sx,-z-sz,(g+y)/2),(.22,.22,max(.02,y-g)+.02),stone);supports.append(dict(name=o.name,x=x+sx,z=z+sz,ground=g,top=y))
for i,x in enumerate([11.65,13.35]):
 z=17.4;y=max(height(x+dx,z+dz) for dx in [-.68,.68] for dz in [-.75,.75])+.025;place('level_camp_portable_toilet_'+('open' if i==0 else 'closed'),'Beach toilet '+str(i+1),x,z,y);footings('Toilet footing',x,z,y,.53,.60)
 # Actual 1.65m approach ramp, fitted to the existing path at its lower end.
 front=z+.75;back=z+2.40;top=y+.11;v=[(x-.50,-front,top),(x+.50,-front,top),(x+.50,-back,height(x+.5,back)),(x-.50,-back,height(x-.5,back))];v +=[(a,b,c-.10) for a,b,c in v];ramp=s.mesh('Toilet supported entry ramp '+str(i),v,[(0,3,2,1),(4,5,6,7),(0,1,5,4),(1,2,6,5),(2,3,7,6),(3,0,4,7)],wood);supports.append(dict(name=ramp.name,kind='ramp',x=x,front=front,back=back,top=top))
for i,x in enumerate([15.7,16.7]):
 z=19.0;y=max(height(x+dx,z+dz) for dx in [-.4,.4] for dz in [-.43,.43])+.015;place('level_camp_refuse_bin','Beach refuse bin '+str(i+1),x,z,y);footings('Bin fitted support',x,z,y,.25,.26)
x,z=5,50.7;y=max(height(x+dx,z+dz) for dx in [-.73,.73] for dz in [-.73,.73])+.02;memorial_y=y;place('level_camp_heron_memorial','Camp wardens heron memorial',x,z,y);footings('Memorial foundation pier',x,z,y,.52,.52)
place('level_camp_pavilion_fireplace','Pavilion built-in masonry hearth',25.9,46.1,0,math.pi/2)
# Only actual fireplace overhang needs buried engineering supports outside slab.
footings('Hearth external foundation',24.6,46.1,0,.35,1.10)

# Six further pitched tents. Actual terrain-conformed floors, exact original UVs;
# clearance/approach checking remains mandatory, same as previous two tents.
tent_specs=[('ridge',8.5,22,0),('dome',20.2,20.5,0),('ridge',20.4,46,math.pi),('dome',45.3,30.3,0),('ridge',23,52,math.pi),('dome',46.3,61.2,0)]
for i,(kind,x,z,yaw) in enumerate(tent_specs):place('level_camp_tent_'+kind,'Additional pitched tent '+str(i+1),x,z,height(x,z),yaw,conform=True)
# Reused large refuge shrub geometry is decorative here; no hiding grant.
rng=random.Random(241024);shrubs=[]
segments=[(a,c,l['width']) for l in d['routes'] for a,c in zip(l['points'],l['points'][1:])]
for attempt in range(1200):
 if len(shrubs)>=28:break
 x=rng.uniform(18,47);z=rng.uniform(50.2,70);scale=rng.uniform(.85,1.2);radius=1.65*scale
 if not all(organic.inside(x+dx,z+dz) for dx in [-radius,radius] for dz in [-radius,radius]):continue
 if any(organic.segment_distance(x,z,[a[0],a[2]],[c[0],c[2]])<width/2+radius+.60 for a,c,width in segments):continue
 if any(math.hypot(x-q['point'][0],z-q['point'][2])<3+radius for q in d['rituals']):continue
 if any(math.hypot(x-p['x'],z-p['z'])<radius+1.5 for p in shrubs):continue
 if any('tent_' in p['asset'] and math.hypot(x-p['x'],z-p['z'])<radius+3.0 for p in placements):continue
 o=place('level_camp_shrub_decorative_'+('a' if len(shrubs)%2==0 else 'b'),'Approach decorative large shrub '+str(len(shrubs)),x,z,height(x,z),rng.random()*math.tau,conform=True,scale=scale);o['gameplay_hiding']=False;shrubs.append(placements[-1])
refuges=[]
for i,(x,z) in enumerate([(21,63),(42,54),(37,19.5)]):
 o=place('level_shrub_refuge','Proposed authored refuge '+str(i+1),x,z,height(x,z),conform=True);o['gameplay_hiding']='Proposed socket; NOT runtime integrated';refuges.append(placements[-1])
# Additional gear at sleeping sites, original reusable roll UVs kept intact.
for i,(kind,x,z,yaw) in enumerate(tent_specs):
 o=place('level_camp_bedroll_rolled','Additional camp rolled bedding '+str(i),x+.55,z,height(x+.55,z),yaw,conform=True);o.location.z+=.028;placements[-1]['y']+=.028

# Native creek water presentation. No runtime layered/transparency claim.
water=bpy.data.materials.new('Native creek water readable blue-green');water.use_nodes=True;p=water.node_tree.nodes['Principled BSDF'];p.inputs['Base Color'].default_value=(.055,.20,.215,1);p.inputs['Roughness'].default_value=.25;p.inputs['Coat Weight'].default_value=.25
noise=water.node_tree.nodes.new('ShaderNodeTexNoise');noise.inputs['Scale'].default_value=7;noise.inputs['Detail'].default_value=2;bump=water.node_tree.nodes.new('ShaderNodeBump');bump.inputs['Strength'].default_value=.14;bump.inputs['Distance'].default_value=.025;water.node_tree.links.new(noise.outputs['Fac'],bump.inputs['Height']);water.node_tree.links.new(bump.outputs['Normal'],p.inputs['Normal'])
for name in ['Lake water boundary','Creek water boundary']:o=bpy.data.objects[name];o.data=o.data.copy();o.data.materials.clear();o.data.materials.append(water)
bpy.data.objects.remove(bpy.data.objects['Creek water boundary'],do_unlink=True)
# Culvert: real open bore, stone arch/headwall, uphill buried barrel and debris grate.
creekparts=[];rock=s.material('Creek revetment granite',(.31,.33,.30),'paint');iron=s.material('Culvert iron patina',(.11,.13,.12),'metal');cx=old.creek_center(25.5);cz=25.5;cy=-1.30
vv=[];ff=[];N=32
for y,r in [(0,.82),(-3,.82),(0,.99),(-3,.99)]:
 for i in range(N):a=i/N*math.tau;vv.append((cx+r*math.cos(a),-cz+y,cy+r*math.sin(a)))
for i in range(N):
 j=(i+1)%N;ff.extend([(i,j,N+j,N+i),(2*N+i,3*N+i,3*N+j,2*N+j),(i,2*N+i,2*N+j,j),(N+i,N+j,3*N+j,3*N+i)])
creekparts.append(s.mesh('Open thick culvert barrel',vv,ff,rock))
for side in [-1,1]:
 creekparts.append(s.box('Culvert stone headwall pier',(cx+side*1.23,-cz-.20,-1.21),(.55,.65,2.13),rock,.035))
 for j in range(6):
  a=j/6*math.pi/2+(0 if side==1 else math.pi/2);c=a+math.pi/12;v=[]
  for y in [-cz+.14,-cz-.54]:
   for radius,angle in [(1.03,a),(1.03,c),(1.32,c),(1.32,a)]:v.append((cx+radius*math.cos(angle),y,cy+radius*math.sin(angle)))
  creekparts.append(s.mesh('Stone arch voussoir',v,[(0,1,2,3),(7,6,5,4),(0,4,5,1),(1,5,6,2),(2,6,7,3),(3,7,4,0)],rock))
 for i in range(4):
  x=cx+side*(1.25+i*.45);z=cz-i*.40;g=height(x,z);creekparts.append(s.box('Culvert wing-wall masonry',(x,-z,g+.30),(.58,.65,.75),rock,.045))
for dx in [-.6,-.4,-.2,0,.2,.4,.6]:
 h=math.sqrt(.82**2-dx**2);creekparts.append(s.rod('Culvert recessed debris grate',(cx+dx,-cz-.38,cy-h),(cx+dx,-cz-.38,cy+h),.022,iron,8))
# Cut only the actual buried barrel cavity out of this derived terrain skin.
terrain.data=terrain.data.copy();bm=bmesh.new();bm.from_mesh(terrain.data)
local=[f for f in bm.faces if abs(f.calc_center_median().x-cx)<1.7 and cz-2.3< -f.calc_center_median().y<cz+3.7]
edges=list({e for f in local for e in f.edges});bmesh.ops.subdivide_edges(bm,edges=edges,cuts=10,use_grid_fill=True)
# Grade the actual outlet bed down into the existing wet creek, not a green plug.
for v in bm.verts:
 x,y,z=v.co;station=-y
 if cz-2.3<station<cz+.1 and abs(x-cx)<1.1:
  w=1-old.smooth((abs(x-cx)-.78)/.32);v.co.z=min(z,z*(1-w)+(cy-.96)*w)
cut=[]
for f in bm.faces:
 p=f.calc_center_median();dx=abs(p.x-cx)
 if cz-.08< -p.y<cz+3.1 and dx<.91 and p.z<cy+math.sqrt(max(0,.91**2-dx**2))+.03:cut.append(f)
bmesh.ops.delete(bm,geom=cut,context='FACES');bm.to_mesh(terrain.data);bm.free();terrain.data.update()
terrain.name='level_camp_terrain_amenities';bpy.ops.object.select_all(action='DESELECT');terrain.select_set(True);bpy.context.view_layer.objects.active=terrain;terrain.data.calc_loop_triangles();bpy.ops.export_scene.gltf(filepath=str(ROOT/'assets/runtime/models/level_camp_terrain_amenities.glb'),export_format='GLB',use_selection=True,export_yup=True);bpy.data.libraries.write(str(ROOT/'development/levels/blender/level_camp_terrain_amenities.blend'),{terrain},compress=True);assets.append(dict(id=terrain.name,triangles=len(terrain.data.loop_triangles),editable='development/levels/blender/level_camp_terrain_amenities.blend'))
revetment=[]
for i in range(49):
 z=2.8+i*.49
 if 11.0<z<15:continue
 for side in [-1,1]:
  x=old.creek_center(z)+side*(1.85+.12*math.sin(i*1.7));g=height(x,z);size=(.63+rng.random()*.17,.62,.48+rng.random()*.17);o=s.box('Terrain-embedded creek revetment stone',(x,-z,g+.05),size,rock,.07);o.rotation_euler.z=rng.uniform(-.3,.3);creekparts.append(o);revetment.append(dict(name=o.name,x=x,z=z,ground=g))
creek=s.join(creekparts,'level_camp_creek_engineering');bpy.ops.object.transform_apply(location=True,rotation=True,scale=True);creek.data.calc_loop_triangles();bpy.ops.export_scene.gltf(filepath=str(ROOT/'assets/runtime/models/level_camp_creek_engineering.glb'),export_format='GLB',use_selection=True,export_yup=True);bpy.data.libraries.write(str(ROOT/'development/levels/blender/level_camp_creek_engineering.blend'),{creek},compress=True);assets.append(dict(id=creek.name,triangles=len(creek.data.loop_triangles),editable='development/levels/blender/level_camp_creek_engineering.blend'))
# Authored, continuous closed blocker meshes, hidden from review render. Runtime
# loader/host integration is explicitly pending; these do not yet stop a player.
blockers=[]
def barrier(id,a,c,bottom=-4,top=4,width=.25):
 dx=c[0]-a[0];dz=c[1]-a[1];length=math.hypot(dx,dz);o=s.box(id,((a[0]+c[0])/2,-(a[1]+c[1])/2,(bottom+top)/2),(length+width,width,top-bottom),iron);o.rotation_euler.z=-math.atan2(dz,dx);o.hide_render=True;o.display_type='WIRE';o['runtime_collision_status']='NOT INTEGRATED';blockers.append(dict(id=id,a=a,b=c,bottom=bottom,top=top,width=width))
for side in [-1,1]:
 for i in range(64):
  a=2+i*.43;c=2+(i+1)*.43;mid=(a+c)/2
  # Under-bridge barrier tops sit below its underside; walking deck stays clear.
  top=-1.20 if 11.1<mid<14.9 else 4
  barrier('Creek bank blocker '+str(side)+' '+str(i),[old.creek_center(a)+side*1.65,a],[old.creek_center(c)+side*1.65,c],top=top)
barrier('Culvert mouth blocker',[cx-1.05,27.2],[cx+1.05,27.2])
for z in [11.5,14.5]:barrier('Bridge side blocker '+str(z),[43,z],[53,z])
for i in range(136):
 a=-4+i*.5;c=a+.5
 if a<30.65 and c>25.35:continue
 if a<51.5 and c>44.5:continue
 def edge(x):
  # Actual retained terrain-to-water contour, measured on the original BVH.
  candidates=[]
  for j in range(65):
   z=j*.20
   if height(max(-3.999,min(63.999,x)),z)>d['water_y']+.08:candidates.append(z)
  return min(candidates) if candidates else 5
 barrier('Lake shore blocker '+str(i),[a,edge(a)],[c,edge(c)])
barrier('West berm-to-shore return',[5,12],[5,edge(5)])
barrier('East berm-to-shore return',[54.8,6],[54.8,edge(54.8)])
barrier('Dock west shore join',[25,edge(25)],[25.5,3.9])
barrier('Dock east shore join',[30.5,3.9],[31,edge(31)])
barrier('Creek mouth west shore join',[44.5,edge(44.5)],[old.creek_center(2)-1.65,2])
barrier('Creek mouth east shore join',[old.creek_center(2)+1.65,2],[51.5,edge(51.5)])
for x in [25.50,30.50]:barrier('Dock side blocker '+str(x),[x,-10],[x,3.9],bottom=-3,top=4)
barrier('Dock outer end blocker',[25.5,-10],[30.5,-10],bottom=-3,top=4)
# Remove only decorative plants newly occupied by tents, facilities or shrub beds.
removed=[]
for o in list(scene.objects):
 if o.name.startswith('Interior grounded fern ') and any(math.hypot(o.location.x-p['x'],-o.location.y-p['z'])<(2.8 if 'tent_' in p['asset'] else 1.2) for p in placements):removed.append(o.name);bpy.data.objects.remove(o,do_unlink=True)
scene['production_status']='Expanded campground native detail; explicit blocker volumes NOT runtime integrated';scene.cycles.samples=24;scene.render.resolution_x=1600;scene.render.resolution_y=1066
fill=bpy.data.objects.get('Boundary inspection fill only')
def camera(name,pos,target,lens=32):
 scene.camera.location=pos;scene.camera.rotation_euler=(Vector(target)-scene.camera.location).to_track_quat('-Z','Y').to_euler();scene.camera.data.lens=lens
 if fill:fill.location=pos;fill.rotation_euler=scene.camera.rotation_euler
 scene.render.filepath=str(PRE/(name+'.png'));bpy.ops.render.render(write_still=True)
bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'hollow-creek-amenities.blend'),compress=True)
camera('amenities-expanded-site-native-fitout',(105,-125,100),(31,-42,1),40)
camera('amenities-beach-toilets-native-fitout',(20,-25,1.8),(12.8,-17.4,.0),30)
camera('amenities-memorial-native-fitout',(10,-57,2.7),(5,-50.7,1.8),34)
camera('amenities-hearth-native-fitout',(33,-43,2.4),(25.9,-46.1,1.6),28)
camera('amenities-approach-shrubs-native-fitout',(33,-73,3),(30,-56,1.5),28)
camera('amenities-culvert-native-fitout',(44,-21,.7),(cx,-27.5,-.7),34)
report=dict(status='native expansion; independent fitness checks pending',assets=assets,placements=placements,supports=supports,large_decorative_shrubs=shrubs,additional_tents=6,total_pitched_tents=8,creek=dict(culvert_center=[cx,cy,cz],barrel_radius=.82,barrel_length=3,terrain_faces_cut=len(cut),revetment=revetment),blockers=blockers,blocker_runtime_status='NOT INTEGRATED: native authored contract/meshes only; no working player barrier claim',destination_amendments=[dict(old_reservation='store',use='Camp wardens heron memorial and approach',point=[5,memorial_y,50.7]),dict(old_reservation='camper',use='Paired portable toilets and large refuse bins',point=[12.5,0,17.4])],protected_sources=protected,removed_decorative_ferns=removed,limitations=['No runtime map unlock/collision/AI/circuit, host/LAN, photo or performance acceptance.','Invisible blocker volumes are native authoring only; wiring into player/ghost collision and movement tests remains mandatory.','Native water appearance is improved; layered transparency/depth/foam/runtime port remain open.','Masonry hearth is cold; combustion/exhaust and human fire-clearance/art review remain open.'])
for a in assets:
 path=ROOT/f"assets/runtime/models/{a['id']}.glb";a.update(bytes=path.stat().st_size,sha256=hashlib.sha256(path.read_bytes()).hexdigest());assert a['triangles']<50000 and a['bytes']<4000000
report['proposed_refuges']=refuges;report['shared_hiding_maximum']=3
(OUT/'amenities.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
for p,h in protected.items():assert hashlib.sha256((ROOT/p).read_bytes()).hexdigest()==h,p
print('BUILT expanded native campground:',len(placements),'models;',len(shrubs),'large shrubs;',len(blockers),'unintegrated native barrier volumes',flush=True)
