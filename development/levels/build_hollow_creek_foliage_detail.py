"""Refined original foliage, supplemental trunk/crown billboards, and near planting.
Billboard textures are renders of our native crown meshes, not external images.
Existing tree placements remain, refined in this derived scene; cards supplement.
"""
import bpy,sys,json,math,random,hashlib
from pathlib import Path
from mathutils import Vector
from mathutils.bvhtree import BVHTree
ROOT=Path(__file__).resolve().parents[2];sys.path[:0]=[str(ROOT/'development/levels'),str(ROOT/'development/assets')]
import rebuild_support as s
import hollow_creek_organic as organic
OUT=ROOT/'development/levels/hollow-creek';PRE=ROOT/'development/levels/previews/hollow-creek';TEX=OUT/'textures';TEX.mkdir(exist_ok=True)
source=OUT/'hollow-creek-forest.blend';source_hash=hashlib.sha256(source.read_bytes()).hexdigest();d=json.loads((OUT/'organic-terrain.json').read_text(encoding='utf-8'));forest=json.loads((OUT/'forest.json').read_text(encoding='utf-8'))
def texture(name,color,kind):
 m=bpy.data.materials.new(name);m.use_nodes=True;p=m.node_tree.nodes['Principled BSDF'];p.inputs['Roughness'].default_value=.94
 image=bpy.data.images.new(name+' original diffuse',width=256,height=256);pixels=[];rng=random.Random(name)
 for y in range(256):
  for x in range(256):
   if kind=='bark':f=1+.08*math.sin(x*.31+math.sin(y*.025)*.7)+.035*math.sin(x*.91+y*.018)+rng.uniform(-.025,.025)
   else:f=1+.08*math.sin(x*.17+y*.23)*math.sin(y*.11-x*.07)+rng.uniform(-.035,.035)
   pixels.extend([c*f for c in color]+[1])
 image.pixels.foreach_set(pixels);image.pack();node=m.node_tree.nodes.new('ShaderNodeTexImage');node.image=image;node.interpolation='Linear';m.node_tree.links.new(node.outputs['Color'],p.inputs['Base Color']);return m
assets=[];textures=[]
for variant,seed in [('a',23),('b',71)]:
 s.reset();rng=random.Random(seed);bark=texture('Refined pine bark '+variant,(.24,.17,.10),'bark');green=texture('Refined pine needles '+variant,(.105,.205,.085),'needles');parts=[];crown=[]
 profile=[(0,.31),(.2,.23),(1,.18),(2.5,.14),(4,.105),(6,.065),(7.5,.025),(8.05,.008)];trunk=s.lathe('Tapered root flare trunk',profile,bark,12);parts.append(trunk)
 for i in range(72):
  h=1.3+i*.09+rng.uniform(-.10,.10);angle=i*2.39996+rng.uniform(-.24,.24);radius=max(.25,2.6*(1-h/8.5))*rng.uniform(.85,1.2);tip=Vector((math.cos(angle)*radius,math.sin(angle)*radius,h+.23+rng.uniform(-.14,.2)));base=Vector((0,0,h));branch=s.rod('Needle-bearing attached branch',base,tip,.035,bark,6,end_radius=.014);parts.append(branch);crown.append(branch)
  for j in [0,1]:
   center=base.lerp(tip,.55 if j==0 else .92)+Vector((0,0,.12));axis=Vector((math.cos(angle),math.sin(angle),.20));side=Vector((-math.sin(angle),math.cos(angle),.08));v=[];f=[]
   # Fine attached needle-bearing sprays replace smooth ellipsoid canopy lumps.
   for k in range(9):
    along=k/8;root=center+axis*(along-.45)*(.68 if j==0 else .48);spread=(.35 if j==0 else .25)*math.sin((along*.8+.1)*math.pi)
    for sign in [-1,1]:
     end=root+side*spread*sign+axis*.15+Vector((0,0,.035));width=.025+(.018 if j==0 else .012)*(1-along);n=len(v)
     v.extend([tuple(root),tuple(root+Vector((0,0,width))),tuple(end),tuple(root-Vector((0,0,width)))])
     f.extend([(n,n+1,n+2),(n,n+2,n+3),(n+2,n+1,n),(n+3,n+2,n)])
   o=s.mesh('Attached fine needle spray',v,f,green);parts.append(o);crown.append(o)
 # Transparent orthographic crown bake; lower trunk is intentionally absent.
 trunk.hide_render=True;scene=bpy.context.scene;scene.render.engine='CYCLES';scene.cycles.samples=12;scene.cycles.use_denoising=True;scene.render.film_transparent=True;scene.render.resolution_x=768;scene.render.resolution_y=768;scene.render.resolution_percentage=100;scene.render.image_settings.file_format='PNG';scene.render.image_settings.color_mode='RGBA'
 scene.world=bpy.data.worlds.new('Crown bake ambient');scene.world.use_nodes=True;scene.world.node_tree.nodes['Background'].inputs[0].default_value=(.6,.65,.7,1);scene.world.node_tree.nodes['Background'].inputs[1].default_value=.9
 bpy.ops.object.light_add(type='AREA',location=(2,-7,10));bpy.context.object.data.energy=650;bpy.context.object.data.size=8;bpy.context.object.rotation_euler=(Vector((0,0,4))-bpy.context.object.location).to_track_quat('-Z','Y').to_euler()
 bpy.ops.object.camera_add(location=(0,-16,4));cam=bpy.context.object;cam.rotation_euler=(Vector((0,0,4))-cam.location).to_track_quat('-Z','Y').to_euler();cam.data.type='ORTHO';cam.data.ortho_scale=8.8;scene.camera=cam;file=TEX/f'pine-crown-{variant}.png';scene.render.filepath=str(file);bpy.ops.render.render(write_still=True);textures.append(str(file.relative_to(ROOT)).replace('\\','/'));trunk.hide_render=False
 name='level_camp_pine_refined_'+variant;obj=s.join(parts,name);bpy.ops.object.transform_apply(location=True,rotation=True,scale=True);obj.data.calc_loop_triangles();assert len(obj.data.loop_triangles)<50000;obj['license']='CC0-1.0';obj['source']='Original attached branch geometry and 256px procedural diffuse; crown bake generated from this mesh'
 model=ROOT/f'assets/runtime/models/{name}.glb';bpy.ops.export_scene.gltf(filepath=str(model),export_format='GLB',use_selection=True,export_yup=True,export_extras=True);editable=ROOT/f'development/levels/blender/{name}.blend';bpy.data.libraries.write(str(editable),{obj},compress=True);assets.append(dict(id=name,triangles=len(obj.data.loop_triangles),editable=str(editable.relative_to(ROOT)).replace('\\','/'),sha256=hashlib.sha256(model.read_bytes()).hexdigest()))
# Original close foliage: pinnate fern fronds, not opaque cone/bush substitutes.
s.reset();leaf=texture('Fern subtle green',(.16,.28,.09),'needles');stem=texture('Fern stalk',(.18,.23,.075),'bark');parts=[]
for i in range(11):
 angle=i*math.tau/11;length=.62+.16*math.sin(i*2.1);tip=Vector((math.cos(angle)*length,math.sin(angle)*length,.38+.17*math.sin(i*.9)**2));root=Vector((0,0,.025));parts.append(s.rod('Fern attached rachis',root,tip,.008,stem,5));side=Vector((-math.sin(angle),math.cos(angle),0))
 for j in range(1,9):
  t=j/9;center=root.lerp(tip,t);spread=.14*math.sin(t*math.pi)**.65
  for sign in [-1,1]:
   end=center+side*spread*sign+Vector((math.cos(angle)*.06,math.sin(angle)*.06,.025));width=.025*(1-t*.6);vv=[tuple(center-side*.005),tuple(center+Vector((0,0,width))),tuple(end),tuple(center-Vector((0,0,width)))];parts.append(s.mesh('Fern tapered leaflet',vv,[(0,1,2),(0,2,3),(2,1,0),(3,2,0)],leaf))
fern=s.join(parts,'level_camp_fern_refined');bpy.ops.object.transform_apply(location=True,rotation=True,scale=True);fern.data.calc_loop_triangles();fern['license']='CC0-1.0';model=ROOT/'assets/runtime/models/level_camp_fern_refined.glb';bpy.ops.export_scene.gltf(filepath=str(model),export_format='GLB',use_selection=True,export_yup=True,export_extras=True);editable=ROOT/'development/levels/blender/level_camp_fern_refined.blend';bpy.data.libraries.write(str(editable),{fern},compress=True);assets.append(dict(id=fern.name,triangles=len(fern.data.loop_triangles),editable=str(editable.relative_to(ROOT)).replace('\\','/'),sha256=hashlib.sha256(model.read_bytes()).hexdigest()))
bpy.ops.wm.open_mainfile(filepath=str(source));scene=bpy.context.scene;templates={}
for a in assets:
 with bpy.data.libraries.load(str(ROOT/a['editable']),link=False) as (incoming,outgoing):outgoing.objects=[a['id']]
 templates[a['id']]=outgoing.objects[0]
refined=0
for i,p in enumerate(forest['placements']):
 if p['kind']!='conifer':continue
 obj=bpy.data.objects[p['name']];template=templates['level_camp_pine_refined_'+('a' if i%2==0 else 'b')];obj.data=template.data;obj['source_refinement']='Original refined pine derivative, original placement retained';refined+=1
def bvh(objects):
 vv=[];ff=[]
 for o in objects:
  o.data.calc_loop_triangles();offset=len(vv);vv.extend(o.matrix_world@v.co for v in o.data.vertices);ff.extend(tuple(offset+i for i in f.vertices) for f in o.data.loop_triangles)
 return BVHTree.FromPolygons(vv,ff,all_triangles=True)
apron=bvh([o for o in scene.objects if o.name.startswith('Non-playable ') and o.type=='MESH']);ground=bvh([bpy.data.objects['level_camp_terrain_organic']]);rng=random.Random(503);billboards=[];planting=[]
materials=[]
for variant in ['a','b']:
 m=bpy.data.materials.new('Supplemental alpha crown '+variant);m.use_nodes=True;nodes=m.node_tree.nodes;nodes.clear();out=nodes.new('ShaderNodeOutputMaterial');mix=nodes.new('ShaderNodeMixShader');transparent=nodes.new('ShaderNodeBsdfTransparent');diffuse=nodes.new('ShaderNodeBsdfDiffuse');image=nodes.new('ShaderNodeTexImage');image.image=bpy.data.images.load(str(TEX/f'pine-crown-{variant}.png'));image.image.pack();image.interpolation='Linear';m.node_tree.links.new(image.outputs['Color'],diffuse.inputs['Color']);m.node_tree.links.new(image.outputs['Alpha'],mix.inputs[0]);m.node_tree.links.new(transparent.outputs[0],mix.inputs[1]);m.node_tree.links.new(diffuse.outputs[0],mix.inputs[2]);m.node_tree.links.new(mix.outputs[0],out.inputs['Surface']);materials.append(m)
trunkmat=s.material('Supplemental trunk bark',(.20,.15,.09),'paint')
for p in forest['placements']:
 if p['layer']=='near':continue
 for j in [-1,1]:
  x=p['x']+rng.uniform(3,6)*j;z=p['z']+rng.uniform(-4,4)
  if organic.inside(x,z) or z<10 or any(math.hypot(x-q['x'],z-q['z'])<2.5 for q in forest['placements']+billboards):continue
  hit=apron.ray_cast(Vector((x,-z,40)),Vector((0,0,-1)),100)[0]
  if hit is None:continue
  scale=rng.uniform(1.3,1.95);height=8.8*scale;y=hit.z-.08
  trunk=s.rod('Supplemental billboard solid trunk',(x,-z,y),(x+.06,-z,y+7.5*scale),.15*scale,trunkmat,8,end_radius=.035*scale);trunk['role']='3D trunk for supplemental distant foliage; scenery only'
  card=s.mesh('Supplemental camera-facing crown',[(x-4.4*scale,-z,y-.4*scale),(x+4.4*scale,-z,y-.4*scale),(x+4.4*scale,-z,y+8.4*scale),(x-4.4*scale,-z,y+8.4*scale)],[(0,1,2,3)],materials[len(billboards)%2]);card.location=(x,-z,y)
  for v in card.data.vertices:v.co-=Vector((x,-z,y))
  uv=card.data.uv_layers.active
  for poly in card.data.polygons:
   for li,co in zip(poly.loop_indices,[(0,0),(1,0),(1,1),(0,1)]):uv.data[li].uv=co
  constraint=card.constraints.new('LOCKED_TRACK');constraint.target=scene.camera;constraint.track_axis='TRACK_NEGATIVE_Y';constraint.lock_axis='LOCK_Z';card['role']='Supplemental alpha crown only; existing 3D trees retained; native camera-facing test, runtime pending';billboards.append(dict(x=x,z=z,ground=hit.z,trunk=trunk.name,card=card.name,scale=scale))
segments=[(a,b,l['width']) for l in d['routes'] for a,b in zip(l['points'],l['points'][1:])]
for attempt in range(2400):
 if len(planting)>=150:break
 x=rng.uniform(3,58);z=rng.uniform(10,69);radius=.6
 if not organic.inside(x,z) or 24<x<40 and 34<z<50:continue
 if any(organic.segment_distance(x,z,[a[0],a[2]],[b[0],b[2]])<w/2+.75+radius for a,b,w in segments):continue
 if any(math.hypot(x-r['point'][0],z-r['point'][2])<3.4+radius for r in d['rituals']):continue
 if any(math.hypot((x-p['center'][0])/p['radii'][0],(z-p['center'][1])/p['radii'][1])<1.25 for p in organic.ELLIPSES):continue
 if any(math.hypot(x-p['x'],z-p['z'])<.8 for p in planting):continue
 hit=ground.ray_cast(Vector((x,-z,10)),Vector((0,0,-1)),30)[0]
 if hit is None or hit.z<d['water_y']+.25:continue
 obj=templates['level_camp_fern_refined'].copy();obj.data=templates['level_camp_fern_refined'].data;scene.collection.objects.link(obj);obj.name='Interior grounded fern '+str(len(planting));obj.location=(x,-z,hit.z-.025);obj.rotation_euler.z=rng.random()*math.tau;scale=rng.uniform(.55,1.0);obj.scale=(scale,scale,scale);obj['role']='Decorative close foliage; no gameplay hiding grant';planting.append(dict(name=obj.name,x=x,z=z,ground=hit.z,scale=scale,radius=.8*scale))
scene.render.engine='CYCLES';scene.cycles.samples=16;scene.cycles.use_denoising=True;scene.render.film_transparent=False;scene.render.resolution_x=1200;scene.render.resolution_y=800;scene.render.resolution_percentage=100;scene.render.image_settings.file_format='PNG';scene.render.image_settings.color_mode='RGBA';scene['production_status']='Refined native foliage: existing trees plus supplemental alpha crowns and protected close planting; runtime pending'
fill=bpy.data.objects.get('Boundary inspection fill only')
def camera(name,pos,target,lens):
 scene.camera.location=pos;scene.camera.rotation_euler=(Vector(target)-scene.camera.location).to_track_quat('-Z','Y').to_euler();scene.camera.data.lens=lens
 if fill:fill.location=pos;fill.rotation_euler=scene.camera.rotation_euler
 scene.render.filepath=str(PRE/(name+'.png'));bpy.ops.render.render(write_still=True)
bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'hollow-creek-foliage-detail.blend'),compress=True)
camera('foliage-detail-trail-native-fitout',(32,-64,2.5),(20,-40,1.5),24)
camera('foliage-detail-berm-native-fitout',(8,-37,1.7),(1,-40,4),23)
camera('foliage-detail-near-native-fitout',(19,-29,.9),(14,-28,.1),32)
camera('foliage-detail-site-native-fitout',(105,-125,100),(31,-42,1),40)
camera('foliage-detail-lake-native-fitout',(32,-14,.32),(30,10,.2),24)
report=dict(status='native art refinement only; runtime alpha/LOD integration pending',assets=assets,original_tree_count=len(forest['placements']),refined_conifers=refined,billboards=billboards,near_planting=planting,texture_sources=textures,source_scene_sha256=source_hash,source_scene=str(source.relative_to(ROOT)).replace('\\','/'),limitations=['Native locked-track alpha crowns are supplemental, not replacements for 3D trees.','CPU/GPU cutout sorting, photographs and runtime billboard/LOD/performance acceptance remain required.','Decorative close ferns do not grant hiding; dense authored refuge pockets remain a separate requirement.','Remaining tents/sanitary/dock/utility and park-detail work follows the recorded detail pass.'])
(OUT/'foliage-detail.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8');assert hashlib.sha256(source.read_bytes()).hexdigest()==source_hash
print('BUILT refined trees',refined,'+ supplemental crowns',len(billboards),'+ protected near ferns',len(planting),flush=True)
