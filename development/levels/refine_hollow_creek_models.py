"""Second original construction/weathering pass. Clean checkpoints stay immutable.
Actual geometry details, curved-surface normals and packed fine diffuse maps.
"""
import bpy,bmesh,sys,math,random,json,hashlib
from pathlib import Path
from mathutils import Vector
from mathutils.bvhtree import BVHTree
ROOT=Path(__file__).resolve().parents[2];sys.path[:0]=[str(ROOT/'development/levels'),str(ROOT/'development/assets')]
import rebuild_support as s
import build_lived_assets as b
OUT=ROOT/'development/levels/hollow-creek';PRE=ROOT/'development/levels/previews/hollow-creek'
ids=['level_camp_dock_shed','level_camp_dock_utilities','level_camp_rowboat','level_camp_folding_chair','level_camp_cooler','level_camp_portable_toilet_open','level_camp_portable_toilet_closed','level_camp_refuse_bin','level_camp_heron_memorial','level_camp_pavilion_fireplace','level_camp_shrub_decorative_a','level_camp_shrub_decorative_b','level_camp_creek_engineering','level_camp_tent_ridge','level_camp_tent_dome','level_camp_bedroll_rolled','level_trace_generator','level_camp_pavilion','level_camp_picnic_table','level_camp_fire_ring']
protected={}
for id in ids:
 for file in [ROOT/f'assets/runtime/models/{id}.glb',ROOT/f'development/levels/blender/{id}.blend',ROOT/f'development/levels/previews/{id}.png']:
  if file.suffix=='.png' and not file.exists():continue
  protected[file.relative_to(ROOT).as_posix()]=hashlib.sha256(file.read_bytes()).hexdigest()
source=OUT/'hollow-creek-amenities.blend';protected[source.relative_to(ROOT).as_posix()]=hashlib.sha256(source.read_bytes()).hexdigest();records=[]

def weather(material,cache):
 if material.name in cache:return cache[material.name]
 m=material.copy();m.name='Fine weathered '+material.name;m.use_nodes=True;shader=next(n for n in m.node_tree.nodes if n.type=='BSDF_PRINCIPLED');tex=next((n for n in m.node_tree.nodes if n.type=='TEX_IMAGE'),None)
 if tex and tex.image:
  raw=list(tex.image.pixels);base=[sum(raw[c::4])/max(1,len(raw)//4) for c in range(3)]
 else:base=list(shader.inputs['Base Color'].default_value[:3]);tex=m.node_tree.nodes.new('ShaderNodeTexImage');m.node_tree.links.new(tex.outputs['Color'],shader.inputs['Base Color'])
 name=material.name.lower();wood=any(k in name for k in ['oak','pine','timber','wood','crossing']);metal=any(k in name for k in ['iron','steel','brass','bronze','rust']);cloth=any(k in name for k in ['cloth','canvas','woven','quilt','groundsheet']);rng=random.Random(material.name+' refined');pix=[]
 coarse=[[rng.uniform(-1,1) for x in range(9)] for y in range(9)];small=[[rng.uniform(-1,1) for x in range(33)] for y in range(33)]
 def noise(grid,x,y,step):
  ix=int(x/step);iy=int(y/step);u=x/step-ix;v=y/step-iy;u=u*u*(3-2*u);v=v*v*(3-2*v);return (grid[iy][ix]*(1-u)+grid[iy][ix+1]*u)*(1-v)+(grid[iy+1][ix]*(1-u)+grid[iy+1][ix+1]*u)*v
 for y in range(256):
  for x in range(256):
   mottling=.10*noise(coarse,x,y,32)+.035*noise(small,x,y,8);fine=rng.uniform(-.022,.022);edge=min(x,y,255-x,255-y);wear=.10*(1-edge/13) if edge<13 else 0
   grain=.026*math.sin(x*.28+math.sin(y*.053)*1.2) if wood else .010*math.sin(x*math.pi/2)*math.sin(y*math.pi/2) if cloth else 0
   factor=1+mottling+fine+grain+wear;color=[max(0,min(1,c*factor)) for c in base]
   if metal and rng.random()<.016:color=[c*.78+.012*t for c,t in zip(color,[1,.55,.26])]
   pix.extend(color+[1])
 im=bpy.data.images.new('Original 256px fine wear '+material.name,width=256,height=256);im.pixels.foreach_set(pix);im.pack();tex.image=im;tex.interpolation='Linear';shader.inputs['Roughness'].default_value=.72 if metal else .91;cache[material.name]=m;return m

def smooth(obj,bevel=True):
 bpy.context.view_layer.objects.active=obj;bpy.ops.object.select_all(action='DESELECT');obj.select_set(True)
 if bevel:
  mod=obj.modifiers.new('Small three-segment construction edge radii','BEVEL');mod.width=.006;mod.segments=3;mod.limit_method='ANGLE';mod.angle_limit=1.25
  bpy.ops.object.modifier_apply(modifier=mod.name)
 # Preserve flat panels; soften cylinder circumference and bevel strips.
 bm=bmesh.new();bm.from_mesh(obj.data)
 for f in bm.faces:f.smooth=max(abs(c) for c in f.normal)<.99999
 for e in bm.edges:e.smooth=len(e.link_faces)==2 and e.calc_face_angle(0)<math.radians(48)
 bm.to_mesh(obj.data);bm.free();obj.data.update()

for id in ids:
 s.reset()
 with bpy.data.libraries.load(str(ROOT/f'development/levels/blender/{id}.blend'),link=False) as (src,dst):dst.objects=[id]
 obj=dst.objects[0];bpy.context.scene.collection.objects.link(obj);obj.data=obj.data.copy();cache={}
 for i,m in enumerate(list(obj.data.materials)):obj.data.materials[i]=weather(m,cache)
 metal=s.material('Refined small hardware',(.22,.235,.22),'metal');dark=s.material('Fine recessed seams',(.055,.065,.057),'paint');wood=s.material('Fine weathered construction timber',(.27,.21,.13),'wood');stone=s.material('Fine weathered course faces',(.33,.32,.285),'paint');parts=[obj];features=[]
 def box(name,p,size,mat=metal,bevel=0):
  o=s.box(name,p,size,mat,bevel);parts.append(o);return o
 def rod(name,a,c,r=.009,mat=metal,sides=12):
  o=s.rod(name,a,c,r,mat,sides);parts.append(o);return o
 def sphere(name,p,size,mat):
  bpy.ops.mesh.primitive_uv_sphere_add(segments=20,ring_count=12,location=p);o=bpy.context.object;o.name=name;o.scale=size;bpy.ops.object.transform_apply(location=False,rotation=False,scale=True);o.data.materials.append(mat);s.uv_project(o);parts.append(o);return o
 if id=='level_camp_dock_shed':
  for side in [-1,1]:
   for j in range(15):
    y=-1.10+j*.155
    for low,high in ([(.10,1.46),(1.97,2.16)] if .2<y<.9 else [(.10,2.16)]):
     box('Individual weathered side boarding',(side*1.006,y,(low+high)/2),(.025,.143,high-low),wood)
     for h in [low+.10,high-.10]:rod('Embedded side-board nail',(side*1.018,y,h),(side*1.023,y,h),.006,metal,8)
  for j in range(12):box('Rear individual board',(-.86+j*.156,1.207,1.13),(.145,.025,2.06),wood)
  for x in [-.70,.70]:
   for y in [-1.20,1.20]:rod('Roof fixing cap',(x,y,2.72-abs(x)*.62/1.16+.002),(x,y,2.72-abs(x)*.62/1.16+.014),.016,metal,10)
  features+=['Individual boarding, open vent gaps, embedded fixing heads and roof fasteners']
 elif id=='level_camp_rowboat':
  # The clean draft used coplanar inner/outer end caps. Rebuild the actual
  # 50mm end walls, remove redundant curved cap strips, and close the top rim.
  bm=bmesh.new();bm.from_mesh(obj.data);layer=bm.verts.layers.deform.active;gid=next(g.index for g in obj.vertex_groups if g.name.startswith('Continuous double-skin hull'));hull=sorted([v for v in bm.verts if gid in v[layer]],key=lambda v:v.index);assert len(hull)==650
  endfaces=[f for f in bm.faces if all(gid in v[layer] for v in f.verts) and all(abs(abs(v.co.y)-1.75)<.00001 for v in f.verts)];bmesh.ops.delete(bm,geom=endfaces,context='FACES_ONLY')
  for j in [0,24]:
   outer=hull[j*13:j*13+13];inner=hull[325+j*13:325+j*13+13]
   for v in inner:v.co.y+=.05 if j==0 else -.05
   bm.faces.new(outer);bm.faces.new(inner[::-1]);bm.faces.new([outer[0],outer[-1],inner[-1],inner[0]])
  bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces));bm.to_mesh(obj.data);bm.free();obj.data.update()
  for side in [-1,1]:
   rod('Actual oarlock socket',(side*.79,0,.53),(side*.79,0,.66),.021,metal)
   for y in [-.95,0,.95]:rod('Thwart fastening',(side*.50,y,.52),(side*.50,y,.55),.014,metal,10)
  # Fine external rubbing strakes follow the actual hull profile.
  for side in [-1,1]:
   for band in [.35,.63]:
    prev=None
    for j in range(41):
     t=-1+2*j/40;width=.12+.68*math.sin((t+1)*math.pi/2)**.72;a=band*math.pi/2;point=(side*(width*math.sin(a)+.012),t*1.75,.12+.47*(1-math.cos(a))+.14*abs(t)**3)
     if prev:rod('Hull rubbing strake',prev,point,.012,wood,8)
     prev=point
  features+=['Curved rubbing strakes, proper oarlocks and thwart fasteners']
 elif id=='level_camp_folding_chair':
  for x in [-.28,.28]:
   rod('Folding pivot bolt',(x-.025,0,.24),(x+.025,0,.24),.025,metal)
   for y in [-.23,.23]:rod('Fabric-to-frame fixing',(x-.02,y,.47),(x+.02,y,.47),.015,metal,10)
  features+=['Folding pivots, frame fixings, finer woven seat/back weathering']
 elif id=='level_camp_cooler':
  rod('Cooler drain plug',(.302,.05,.08),(.321,.05,.08),.026,metal)
  for x in [-.34,.34]:rod('Carry-handle rubber grip',(x,-.075,.31),(x,.075,.31),.018,dark)
  for x in [-.22,.22]:rod('Lid hinge pin',(x-.055,.21,.407),(x+.055,.21,.407),.012,metal)
  features+=['Drain plug, hinge pins, separate grips and lid seam wear']
 elif id.startswith('level_camp_portable_toilet_'):
  vv=[];ff=[]
  for y in [-.795,.795]:
   for j in range(17):x=-.72+j*.09;vv.append((x,y,2.35+.12*(1-(x/.72)**2)))
  vv +=[(x,y,z-.045) for x,y,z in vv];n=34
  for j in range(16):ff.extend([(j,j+1,j+18,j+17),(n+j+17,n+j+18,n+j+1,n+j),(j,n+j,n+j+1,j+1),(j+17,j+18,n+j+18,n+j+17)])
  ff.extend([(0,17,51,34),(16,50,67,33)]);parts.append(s.mesh('Curved moulded sanitary roof',vv,ff,weather(s.material('Moulded sanitary rain cap',(.62,.63,.55),'paint'),cache)))
  extra=[]
  for x in [-.47,.47]:
   for z in [.23,2.0]:
    o=s.rod('Door panel fixing',(x,-.808,z),(x,-.82,z),.011,metal,10);extra.append(o)
  for z in [1.03,1.19]:extra.append(s.rod('Real pull-handle standoff',(.38,-.81,z),(.38,-.87,z),.013,metal,10))
  extra.append(s.rod('Rounded door pull',(.38,-.87,1.03),(.38,-.87,1.19),.015,metal,12))
  if id.endswith('_open'):
   pivot=Vector((-.51,-.77,0));angle=-math.pi*.53
   for o in extra:
    bpy.context.view_layer.objects.active=o;bpy.ops.object.select_all(action='DESELECT');o.select_set(True);bpy.ops.object.transform_apply(location=True,rotation=True,scale=True)
    for v in o.data.vertices:q=v.co-pivot;v.co=pivot+Vector((q.x*math.cos(angle)-q.y*math.sin(angle),q.x*math.sin(angle)+q.y*math.cos(angle),q.z))
  parts+=extra;features+=['Curved moulded cap, panel fixings and a real rounded pull handle']
 elif id=='level_camp_refuse_bin':
  for x in [-.26,.26]:rod('Refuse lid hinge',(x-.06,.40,1.07),(x+.06,.40,1.07),.022,metal)
  rod('Moulded lid pull',(-.18,-.43,1.12),(.18,-.43,1.12),.025,dark)
  for x in [-.29,.29]:
   for side in [-1,1]:rod('Wheel hub bolt',(x+side*.046,.28,.10),(x+side*.058,.28,.10),.028,metal)
  features+=['Hinged lid hardware, moulded grip and axle/hub details']
 elif id=='level_camp_heron_memorial':
  sphere('Smooth bronze heron head',(0,-.17,2.89),(.085,.13,.09),obj.data.materials[-1])
  for side in [-1,1]:sphere('Heron inset eye',(side*.074,-.225,2.91),(.009,.014,.009),dark)
  for x in [-.25,.25]:
   for z in [.67,.93]:rod('Recessed plaque fixing',(x,-.46,z),(x,-.465,z),.012,metal,10)
  features+=['Smooth curved bronze surfaces, head/eyes and plaque fasteners']
 elif id=='level_camp_pavilion_fireplace':
  wood_groups={g.index for g in obj.vertex_groups if g.name.startswith('Cold split firewood')}
  for v in obj.data.vertices:
   if any(g.group in wood_groups for g in v.groups):v.co.z+=.225-v.co.x*.02/1.3
  bm=bmesh.new();bm.from_mesh(obj.data);front=[f for f in bm.faces if f.normal.y<-.99 and all(abs(v.co.y-.95)<.00001 for v in f.verts) and 2.4<f.calc_center_median().z<5.67];bmesh.ops.delete(bm,geom=front,context='FACES_ONLY');bm.to_mesh(obj.data);bm.free()
  box('Continuous recessed front mortar',(0,.982,4.03),(1.40,.026,3.26),dark)
  for side in [-1,1]:
   for j in range(6):
    z=.275+j*.285;box('Individual fireplace cheek course',(side*1.15,-.722,z),(.485,.045,.265),stone,.008)
    for k in range(3):box('Side masonry course',(side*1.415,-.42+k*.55,z),(.045,.525,.265),stone,.008)
  # Maintain the eave clearance: add rear/side flue courses, never push its
  # already measured front face into the timber roof edge.
  for j in range(11):
   z=2.56+j*.28;box('Rear flue stone course',(0,1.862,z),(1.37,.025,.26),stone,.006)
   for x in [-.35,.35]:box('Recessed front flue stone course',(x,.965,z),(.68,.03,.26),stone,.004)
   for side in [-1,1]:box('Flue side stone course',(side*.713,1.40,z),(.025,.875,.26),stone,.006)
  for x in [-.65,.65]:
   for y in [-.45,.50]:rod('Fire grate support leg',(x,y,.245),(x,y,.48),.018,metal)
  features+=['Real separate masonry courses, mortar gaps, flue courses and grate legs']
 elif 'shrub_decorative' in id:
  # Connect individual leaves to real twig geometry, not floating quads.
  segments=[]
  for g in obj.vertex_groups:
   if not g.name.startswith('Decorative woody stem'):continue
   vertices=[v.co.copy() for v in obj.data.vertices if any(a.group==g.index for a in v.groups)]
   if len(vertices)==14:segments.append((sum(vertices[:7],Vector())/7,sum(vertices[7:],Vector())/7))
  for g in obj.vertex_groups:
   if not g.name.startswith('Individual ordinary shrub leaves'):continue
   vertices=[v.co.copy() for v in obj.data.vertices if any(a.group==g.index for a in v.groups)]
   for i in range(0,len(vertices)-4,5):
    root=vertices[i];options=[]
    for a,c in segments:axis=c-a;t=max(0,min(1,(root-a).dot(axis)/axis.length_squared));q=a+axis*t;options.append(((q-root).length,q))
    if options:
     distance,q=min(options,key=lambda x:x[0])
     if distance>.018:rod('Attached fine leaf petiole',q,root,.0028,wood,5)
  features+=['Attached leaf petioles, smooth woody stems and fine leaf variation']
 elif id=='level_camp_creek_engineering':
  a=json.loads((OUT/'amenities.json').read_text(encoding='utf-8'));cx,cy,cz=a['creek']['culvert_center']
  for side in [-1,1]:
   for j in range(7):box('Headwall individual stone course',(cx+side*1.23,-cz+.145,-2.10+j*.29),(.535,.035,.268),stone,.007)
  features+=['Individual headwall stone courses, smoother barrel, finer exposed stone breakup']
 elif id=='level_trace_generator':
  for x in [-.66,.66]:
   for y in [-.32,.32]:rod('Generator skid anchor',(x,y,.04),(x,y,.13),.024,metal,10)
  rod('Generator tank filler neck',(-.25,0,1.04),(-.25,0,1.10),.055,metal)
  rod('Generator filler cap',(-.25,0,1.10),(-.25,0,1.13),.073,metal,16)
  for j in range(9):box('Alternator fine cooling rib',(.46,-.25+j*.061,.45),(.42,.016,.025),metal)
  for y in [-.13,.13]:rod('Control box fastening',(.25,y,.64),(.27,y,.64),.015,metal,10)
  features+=['Skid anchors, fuel filler, cooling ribs and control fasteners']
 elif id=='level_camp_pavilion':
  for x in [-6,-4,-2,2,4,6]:rod('Roof standing seam',(x,-7,5.60-abs(x)*2/7+.015),(x,7,5.60-abs(x)*2/7+.015),.013,metal,8)
  box('Pavilion entry nameboard',(0,-6.98,2.7),(2.2,.055,.28),wood,.008)
  bpy.ops.object.text_add(location=(-.88,-7.017,2.63));o=bpy.context.object;o.data.body='HOLLOW CREEK';o.data.size=.18;o.data.extrude=.001;o.rotation_euler.x=math.pi/2;bpy.ops.object.convert(target='MESH');o=bpy.context.object;o.name='Raised pavilion lettering';o.data.materials.append(stone);s.uv_project(o);parts.append(o)
  features+=['Supported nameboard/lettering, roof seams and finer timber/roof aging']
 elif id=='level_camp_picnic_table':
  for x in [-.5,.5]:
   for y in [-.35,.35]:rod('Picnic-table countersunk bolt',(x,y,.76),(x,y,.77),.014,metal,10)
  features+=['Countersunk construction fixings and finer timber grain/weathering']
 elif id.startswith('level_camp_tent_'):
  features+=['Fine woven canvas wear, seam/hem tone, smooth poles; floor/canvas thickness preserved']
 elif id=='level_camp_bedroll_rolled':features+=['Finer quilt/weave, strap grime and smoothly shaded roll']
 elif id=='level_camp_fire_ring':
  char_groups={g.index for g in obj.vertex_groups if g.name.startswith('Cold charred timber')}
  for v in obj.data.vertices:
   if any(g.group in char_groups for g in v.groups):v.co.z-=.013
  features+=['Finer soot/oxidation, softened metal edge and charred timber supported on ash bed']
 else:features+=['Finer physical edge radii and material wear']
 # Preserve thin canvas, leafy planes and long authored cable layouts.
 result=s.join(parts,id+'_refined');bpy.ops.object.transform_apply(location=True,rotation=True,scale=True)
 smooth(result,bevel=not ('tent_' in id or 'shrub_' in id or id in ['level_camp_dock_utilities','level_camp_rowboat','level_camp_bedroll_rolled','level_camp_fire_ring']))
 result.data.calc_loop_triangles();assert len(result.data.loop_triangles)<50000,(id,len(result.data.loop_triangles))
 view=(5,-7,4) if id not in ['level_camp_pavilion','level_camp_pavilion_fireplace','level_camp_creek_engineering','level_camp_dock_utilities'] else (18,-22,14)
 if id=='level_camp_creek_engineering':view=(20,25,15)
 b.export(id+'_refined',[result],'campground-refinement',view=view);r=b.LOG[-1];r['clean_source']=id;r['features']=features;records.append(r)

# Derived site revision. Keep all existing transforms and semantic identities.
bpy.ops.wm.open_mainfile(filepath=str(source));bpy.context.preferences.filepaths.save_version=0;scene=bpy.context.scene;a=json.loads((OUT/'amenities.json').read_text(encoding='utf-8'));dock=json.loads((OUT/'dock-details.json').read_text(encoding='utf-8'));early=json.loads((OUT/'tents.json').read_text(encoding='utf-8'));mapping={r['clean_source']:r for r in records};templates={}
for r in records:
 with bpy.data.libraries.load(str(ROOT/r['editable']),link=False) as (src,dst):dst.objects=[r['id']]
 templates[r['clean_source']]=dst.objects[0]
terrain=bpy.data.objects['level_camp_terrain_amenities'];ground=BVHTree.FromPolygons([terrain.matrix_world@v.co for v in terrain.data.vertices],[tuple(p.vertices) for p in terrain.data.polygons])
def height(x,z):
 p=ground.ray_cast(Vector((x,-z,10)),Vector((0,0,-1)),40)[0];assert p is not None,(x,z);return p.z
placements=[];by={p['name']:p for p in dock['placements']+a['placements']};by.update({p['native_object']:dict(p,name=p['native_object']) for p in early['placements']})
by.update({id:dict(name=id,asset=id) for id in ['level_camp_pavilion','level_camp_creek_engineering','level_camp_dock_utilities','level_camp_fire_ring','level_camp_picnic_table'] if bpy.data.objects.get(id)})
# Earlier facility objects use semantic names rather than their asset IDs.
for o in list(scene.objects):
 if o.type=='MESH' and o.get('facility_id'):
  asset='level_camp_fire_ring' if 'fire' in o.name else 'level_camp_picnic_table';by[o.name]=dict(name=o.name,asset=asset)
for name,p in by.items():
 id=p['asset']
 if id not in templates:continue
 o=bpy.data.objects.get(name)
 if o is None:continue
 o.vertex_groups.clear();o.data=templates[id].data.copy();o['refinement_source']=mapping[id]['id'];is_tent='tent_' in id;conform=is_tent or 'shrub_decorative' in id
 if is_tent:
  o.location=(p['x'],-p['z'],height(p['x'],p['z']));o.rotation_euler.z=p['yaw'];o.scale=(1,1,1)
  bm=bmesh.new();bm.from_mesh(o.data);deform=bm.verts.layers.deform.active;ids2={g.index for g in templates[id].vertex_groups if 'groundsheet' in g.name.lower() or 'physical floor' in g.name.lower()};faces=[f for f in bm.faces if all(any(i in v[deform] for i in ids2) for v in f.verts)];bmesh.ops.triangulate(bm,faces=faces);faces=[f for f in bm.faces if all(any(i in v[deform] for i in ids2) for v in f.verts)];bmesh.ops.subdivide_edges(bm,edges=list({e for f in faces for e in f.edges}),cuts=20,use_grid_fill=True);bm.to_mesh(o.data);bm.free()
 # Replace vertex-group definitions together with mesh data: source indices
 # remain authoritative for contact and fixture/roof verification.
 for g in templates[id].vertex_groups:
  if o.vertex_groups.get(g.name) is None:o.vertex_groups.new(name=g.name)
 bpy.context.view_layer.update()
 if conform:
  for v in o.data.vertices:
   w=o.matrix_world@v.co;v.co.z+=(height(w.x,-w.y)-o.location.z)/o.scale.z
 elif id=='level_camp_folding_chair':
  for v in o.data.vertices:
   if v.co.z<.025:w=o.matrix_world@v.co;v.co.z+=height(w.x,-w.y)-o.location.z
 elif id=='level_camp_bedroll_rolled':
  # Earlier site-fit rolls retain their actual terrain/floor contact.
  o.location=(p['x'],-p['z'],height(p['x'],p['z'])+.028);o.rotation_euler.z=p['yaw']
  for v in o.data.vertices:w=o.matrix_world@v.co;v.co.z+=height(w.x,-w.y)-height(p['x'],p['z'])
 placements.append(dict(name=name,clean_source=id,refined_source=mapping[id]['id']))
# Terrain retains all exact vertex positions and cuts; only native visual normals
# soften triangle shading. Physics normals/grade checks remain triangle-based.
removed_empty_groups=0
for o in scene.objects:
 if o.type!='MESH' or not o.get('refinement_source'):continue
 used={g.group for v in o.data.vertices for g in v.groups if g.weight>0}
 for g in list(o.vertex_groups)[::-1]:
  if g.index not in used:o.vertex_groups.remove(g);removed_empty_groups+=1
for poly in terrain.data.polygons:poly.use_smooth=True
scene['production_status']='Second native construction/smoothing/weathering pass; runtime gates remain open';scene.cycles.samples=24;scene.render.resolution_x=1600;scene.render.resolution_y=1066
fill=bpy.data.objects.get('Boundary inspection fill only')
def camera(name,pos,target,lens=32):
 scene.camera.location=pos;scene.camera.rotation_euler=(Vector(target)-scene.camera.location).to_track_quat('-Z','Y').to_euler();scene.camera.data.lens=lens
 if fill:fill.location=pos;fill.rotation_euler=scene.camera.rotation_euler
 scene.render.filepath=str(PRE/(name+'.png'));bpy.ops.render.render(write_still=True)
bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'hollow-creek-refined.blend'),compress=True)
camera('refined-expanded-site-native-fitout',(105,-125,100),(31,-42,1),40)
camera('refined-hearth-native-fitout',(33,-43,2.4),(25.9,-46.1,1.6),28)
camera('refined-culvert-native-fitout',(44,-21,.7),(a['creek']['culvert_center'][0],-25.5,-.7),34)
camera('refined-toilets-native-fitout',(20,-25,1.8),(12.8,-17.4,.0),30)
camera('refined-dock-native-fitout',(36,-.5,3),(27,6.8,.0),32)
camera('refined-memorial-native-fitout',(10,-57,2.7),(5,-50.7,1.8),34)
for p,h in protected.items():assert hashlib.sha256((ROOT/p).read_bytes()).hexdigest()==h,p
(OUT/'refined-models.json').write_text(json.dumps(dict(status='native refinement; independent validation pending',assets=records,replaced_instances=placements,removed_empty_group_definitions=removed_empty_groups,protected_sources=protected,limitations=a['limitations']+['This is a second detail/weathering iteration, not final human art acceptance.']),indent=2)+'\n',encoding='utf-8')
print('BUILT',len(records),'separate refined original assets;',len(placements),'native instance replacements; clean sources preserved',flush=True)
