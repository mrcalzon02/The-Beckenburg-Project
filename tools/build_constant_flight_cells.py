"""Construct measured constant flights and supported rectangular stair cells."""
from pathlib import Path
import sys,json,math,struct
import bpy,bmesh
from mathutils import Vector
from mathutils.bvhtree import BVHTree
ROOT=Path(__file__).resolve().parents[1];sys.path[:0]=[str(ROOT/'tools'),str(ROOT/'development/levels/blockouts')]
from build_roster_foundations import prism,material
from planar_structure import plate
from build_roster_floor_closures import difference
from build_architectural_doorways import frame_template
from append_static_glb import append
from build_circulation_construction import native_write
from compose_room_terrain import split_native
from build_deliberate_entrances import write
from review_roster_macroterrain import records,sha
from export_roster_collision import face_keys,native_faces
BASE=ROOT/'development/levels/rebuild';OUT=BASE/'constant-flight-construction-pass';RAIL=ROOT/'development/levels/primitive-props-v1/models/level_blockout_guardrail.glb'
def rect(a,b,c,d):return [(a,b),(c,b),(c,d),(a,d)]
def bb(o):
 v=[o.matrix_world@p.co for p in o.data.vertices];return min(p.x for p in v),min(-p.y for p in v),max(p.x for p in v),max(-p.y for p in v),min(p.z for p in v),max(p.z for p in v)
FLIGHTS={
 'WLD-08':[('L17',(11,0,35),(22,-2.5,35),14),('L18',(42,-2.5,35),(53,0,35),14),('L20',(11,-2.5,22),(11,0,17),14)],
 'WLD-12':[('L16',(28,0,32.3),(28,-3,28),17),('L17',(31,-3,38),(13,0,38),18)],
 'WLD-18':[('route-08',(6,-4,34),(6,0,48),24)]}
FLOOR_ZONES={'WLD-08':{'lab','dorm','generator','tunnel'},'WLD-12':{'trestle','lower'},'WLD-18':{'exit'}}
WALL_FACES={'WLD-08':{'tunnel':['E'],'tunnel-n':['N']},'WLD-12':{},'WLD-18':{'west':['W'],'exit':['N']}}
WELLS={'WLD-08':[rect(11,34,22.3,36),rect(41.7,34,53,36),rect(9.9,17,12.1,22.3)],'WLD-12':[rect(26.9,28,29.1,32.3),rect(13,37,31,39)],'WLD-18':[rect(4.9,34,7.1,48)]}
def wall_match(o,faces):
 for zone,sides in faces.items():
  for side in sides:
   for prefix in ['Wall ','Header ']:
    needle=prefix+zone+' '+side
    if needle in o.name:
     suffix=o.name.split(needle,1)[1]
     if not suffix or suffix[0] in '. ':return True
 return False

def build(row):
 s=row['source'];site=s['id'];folder=OUT/site.lower();folder.mkdir(parents=True,exist_ok=True);world=ROOT/row['world'];source=ROOT/s['glb'];native=ROOT/s['native'];meta=json.loads((ROOT/s['construction']).read_text());data=json.loads((ROOT/s['layout']).read_text());bpy.ops.wm.read_factory_settings(use_empty=True);bpy.context.preferences.filepaths.save_version=0;bpy.ops.import_scene.gltf(filepath=str(source));old=[o for o in bpy.context.scene.objects if o.type=='MESH'];removed=set();built=[];steps=[];proof=[];selected={f[0] for f in FLIGHTS[site]}
 concrete=material(site+' constant stair concrete',(.39,.40,.38));timber=material(site+' structural service platform timber',(.30,.25,.19));steel=material(site+' stair support steel',(.23,.25,.26));terrain=next(o for o in old if o.get('role','').startswith('saved editor terrain'));terrain_name=terrain.name;oldterrainmat=list(terrain.data.materials);terrain_tree=BVHTree.FromPolygons([terrain.matrix_world@v.co for v in terrain.data.vertices],[tuple(f.vertices) for f in terrain.data.polygons]);terrain_materials=[f.material_index for f in terrain.data.polygons]
 assert face_keys(records(terrain))==face_keys(native_faces(ROOT/row['source_world']/'terrain-physics.hcs2'))
 assert all(sha(ROOT/row['source_world']/p)==h for p,h in row['source_world_files_sha256'].items())
 write(world/'verification.json',dict(status='SAVED_RECTANGULAR_STAIR_PUNCHES_NOT_RUNTIME_ACCEPTANCE',write_verification=json.loads((world/'stair-cell-punch-verification.json').read_text()),physics_sha256=sha(world/'terrain-physics.hcs2')))
 write(world/'source-provenance.json',dict(source_world=row['source_world'],source_files_sha256=row['source_world_files_sha256'],rectangular_construction_cuts=row['cuts'],source_height_and_layers_preserved=True,authoring='Actual saved editor topology punches for structural wells/platform; no route-extrusion surfaces'))
 for o in old:
  role=o.get('role','');zone=o.get('zone');a,b,c,d,low,high=bb(o)
  if o.get('link') in selected or any(l in o.name for l in selected) and any(k in role for k in ['stair','guard','landing','stringer']):removed.add(o.name)
  if zone in FLOOR_ZONES[site] and role=='floor' or wall_match(o,WALL_FACES[site]):removed.add(o.name)
  if role.startswith('rough route support') or role in ['structural-rooted-circulation-bed','structural-arrival-end-bearing']:removed.add(o.name)
  if site=='WLD-08' and high<=.001 and low>=-.5 and a<44 and c>20 and b<36 and d>26 and any(k in role for k in ['cap','floor','closure','roof']):removed.add(o.name)
  if role=='fitted library dock or terrace guard' and any(a<max(p[0] for p in w) and c>min(p[0] for p in w) and b<max(p[1] for p in w) and d>min(p[1] for p in w) for w in WELLS[site]):removed.add(o.name)
 def make(name,polys,top,depth,role,zone=None,mat=None):
  result=plate(site+' '+name,polys,top,depth,mat or concrete) or []
  for o in result:
   o['role']=role
   if zone:o['zone']=zone
   built.append(o)
  return result
 def slab(name,foot,top,depth,cuts=(),zone=None,mat=None):
  polys=[foot]
  for cut in cuts:polys=[q for p in polys for q in difference(p,cut)]
  return make(name,polys,top,depth,'constant-cell structural floor or roof',zone,mat)
 def box(name,a,b,c,d,low,high,role='constant-cell retaining or landing support',mat=None):
  assert high>low,(site,name,low,high)
  return make(name,[rect(a,b,c,d)],high,high-low,role,mat=mat)
 # Complete floors and their independent rectangular openings.
 if site=='WLD-08':
  slab('lab floor with rectangular well',rect(2,26,20,44),0,.30,WELLS[site],'lab');slab('dorm floor with rectangular well',rect(44,26,62,44),0,.30,WELLS[site],'dorm');slab('generator floor with rectangular well',rect(2,8,20,26),0,.30,WELLS[site],'generator');slab('continuous lower tunnel floor',rect(20,26,44,36),-2.5,.30,zone='tunnel');slab('main tunnel roof with rectangular wells',rect(20,26,44,36),0,.30,WELLS[site],zone='tunnel')
  slab('west stair-cell base outside tunnel',rect(11,34,20,36),-2.5,.30);slab('east stair-cell base outside tunnel',rect(44,34,53,36),-2.5,.30);slab('generator stair-cell base north of branch',rect(9.9,17,12.1,18),-2.5,.30)
  for a,c,label in [(9,22.3,'west'),(41.7,55,'east')]:
   for z0,z1 in [(33.75,34),(36,36.25)]:box(label+' straight retaining wall '+str(z0),a,z0,c,z1,-2.8,0)
  for x0,x1 in [(9.65,9.9),(12.1,12.35)]:box('generator straight retaining wall '+str(x0),x0,15,x1,22.3,-2.8,0)
  box('generator branch north wall west pier',2,18,9.65,18.24,-2.5,0);box('generator branch north wall east pier',12.35,18,20,18.24,-2.5,0);box('tunnel east wall to actual stair cell',43.76,26,44,34,-2.5,0)
 elif site=='WLD-12':
  slab('trestle deck with rectangular access',rect(28,22,40,42),0,.25,WELLS[site],'trestle',timber);slab('continuous lower service floor',rect(28,22,34,42),-3,.30,zone='lower');slab('north stair-cell base west of lower walkway',rect(26.9,26,28,34.3),-3,.30);slab('west flight base outside lower walkway',rect(13,37,28,39),-3,.30)
  slab('north full upper landing outside trestle',rect(26.9,32.3,28,34.3),0,.30)
  for x0,x1 in [(26.65,26.9),(29.1,29.35)]:box('north stair retaining wall '+str(x0),x0,28,x1,34.3,-3.3,0)
  box('north lower-landing end wall',26.65,25.75,29.35,26,-3.3,0)
  for z0,z1 in [(36.75,37),(39,39.25)]:box('west stair retaining wall '+str(z0),11,z0,31.25,z1,-3.3,0)
  # A full maintenance platform joins the stair head to the existing return route.
  platform=rect(10,36,16,44.5);slab('west maintenance access platform',platform,0,.25,WELLS[site],mat=timber)
  receivers=[]
  for x,z in [(10.3,44.75),(15.7,44.75)]:
   h=terrain_tree.ray_cast(Vector((x,-z,20)),Vector((0,0,-1)),100)[0];assert h is not None,(site,'Missing pile soil',x,z);bottom=h.z-.35;box('maintenance platform founded pile '+str(len(receivers)),x-.15,z-.15,x+.15,z+.15,bottom,-.25,'native-soil founded platform pile',timber);receivers.append(dict(x=x,z=z,soil_y=h.z,bottom_y=bottom))
  for x in [10.3,15.7]:
   polys=[rect(x-.12,36,x+.12,45)]
   for cut in WELLS[site]:polys=[q for p in polys for q in difference(p,cut)]
   make('maintenance platform bearing '+str(x),polys,-.25,.35,'platform structural bearing',mat=steel)
  box('platform west high-soil retaining parapet',9.75,36,10,40,-3.3,2.3);box('platform west lower parapet',9.75,40,10,44.5,-3.3,1.1);box('platform north retaining parapet',10,35.75,16,36,-3.3,2.3);box('platform intermediate bearing return',10,39,11,39.25,-3.3,-.25)
  proof.append(dict(platform=platform,soil_receivers=receivers,construction='Area service platform on south native-soil-founded piles and continuous bearings into retaining walls; high uphill edge held by real retaining parapets; separate from constant treads'))
 else:
  slab('escape gallery deck with rectangular well',rect(4,42,10,62),0,.30,WELLS[site],'exit');make('escape stair and covered connector common base',[rect(4.9,32,7.1,48),rect(6,32.8,8,35.2)],-4,.30,'constant-cell structural floor or roof')
  box('escape gallery north stair passage west jamb',4,42,4.9,42.24,0,3);box('escape gallery north stair passage east jamb',7.1,42,10,42.24,0,3);box('escape gallery north stair passage lintel',4.9,42,7.1,42.24,2.3,3)
  for x0,x1,label in [(4.65,4.9,'west'),(7.1,7.35,'east')]:box('escape retaining wall '+label,x0,34 if label=='west' else 35.2,x1,50,-4.3,0)
  box('escape lower landing north wall',4.65,31.75,7.35,32,-4.3,0);box('escape retaining header above lower side passage',7.1,32.8,7.35,35.2,-1,0)
  for z0,z1 in [(32.56,32.8),(35.2,35.44)]:box('covered escape connector wall '+str(z0),6 if z0<34 else 7.35,z0,8,z1,-4.3,-1)
  slab('covered lower escape connector roof',rect(6,32.56,8.24,35.44),-1,.25)
  # Two full-depth jambs and real lintels span the shared lower west-room entrance.
  for start,end,label in [(26,32.884,'north'),(35.116,38,'south')]:box('west room entry wall '+label,8,start,8.24,end,-4,-1)
  box('west room entry lintel',8,32.884,8.24,35.116,-4+2.3*2.275/2.125,-1)
  for start,end,label in [(32.8,32.884,'north'),(35.116,35.2,'south')]:box('connector east door pier '+label,7.76,start,8,end,-4,-1)
  box('connector east door lintel',7.76,32.884,8,35.116,-4+2.3*2.275/2.125,-1)
  template=frame_template();o=template.copy();o.data=template.data.copy();o.name='Cinder escape full-depth service frame';bpy.context.collection.objects.link(o)
  for v in o.data.vertices:
   p=template.matrix_world@v.co;v.co=(8+p.y*.48/.18,-34-p.x*1.8,-4+p.z*2.3/2.125)
  o.matrix_world.identity();o['role']='indexed escape-connector doorway';o['source_asset']='level_blockout_doorway';o['portal_id']='WLD-18-west-escape-34';built.append(o);bpy.data.objects.remove(template,do_unlink=True)
  portal=dict(id=o['portal_id'],axis='x',boundary=8,along=34,door_type='service',clear_width=1.8,clear_height=2.3,sill_y=-4,frame_depth=.48,frame_span=[7.76,8.24],frame_center=[8,-4,34],rooms=['west','escape-lower-connector'])
  data['authored_doorways']=data.get('authored_doorways',[])+[portal]
 # Trim only interfering shallow foundations around actual rectangular structural cells.
 for o in old:
  if o.name in removed or not any(k in o.get('role','') for k in ['foundation','bearing']):continue
  a,b,c,d,low,high=bb(o);active=[w for w in WELLS[site] if low<-.001 and high>min(min(f[1][1],f[2][1]) for f in FLIGHTS[site]) and a<max(p[0] for p in w) and c>min(p[0] for p in w) and b<max(p[1] for p in w) and d>min(p[1] for p in w)]
  if not active:continue
  polys=[];o.data.calc_loop_triangles()
  for t in o.data.loop_triangles:
   ps=[o.matrix_world@o.data.vertices[i].co for i in t.vertices]
   if max(abs(p.z-high) for p in ps)>1e-5 or (ps[1]-ps[0]).cross(ps[2]-ps[0]).z<=0:continue
   pieces=[[(p.x,-p.y) for p in ps]]
   for cut in active:pieces=[q for p in pieces for q in difference(p,cut)]
   polys+=pieces
  for clone in plate(site+' old footing trimmed to rectangular well '+o.name,polys,high,high-low,o.data.materials[0]) or []:clone['role']='retained footing outside rectangular stair cell';built.append(clone)
  removed.add(o.name)
 # Cast continuous constant-width steps, independent of the opening outlines.
 for link,p,q,n in FLIGHTS[site]:
  x0,y0,z0=p;x1,y1,z1=q;gross=1.8;lower=min(y0,y1)
  for i in range(n):
   a=i/n;c=(i+1)/n;top=y0+(y1-y0)*(c if y1>y0 else a);xa,xb=x0+(x1-x0)*a,x0+(x1-x0)*c;za,zb=z0+(z1-z0)*a,z0+(z1-z0)*c;foot=rect(min(xa,xb),z0-gross/2,max(xa,xb),z0+gross/2) if x0!=x1 else rect(x0-gross/2,min(za,zb),x0+gross/2,max(za,zb))
   for o in box(link+' constant tread '+str(i),*([min(xa,xb),z0-gross/2,max(xa,xb),z0+gross/2] if x0!=x1 else [x0-gross/2,min(za,zb),x0+gross/2,max(za,zb)]),lower-.16,top,'structural-stair-step'):o['link']=link;steps.append(o.name)
  proof.append(dict(link=link,from_point=p,to_point=q,gross_width=gross,nominal_clear_width=1.5,risers=n,regular_rise=abs(y1-y0)/n,constant_tread_depth=math.hypot(x1-x0,z1-z0)/n))
 # Existing indexed guards bear on constant treads or the full rim/deck structure.
 before=set(bpy.context.scene.objects);bpy.ops.import_scene.gltf(filepath=str(RAIL));template=next(o for o in set(bpy.context.scene.objects)-before if o.type=='MESH').data.copy()
 for o in set(bpy.context.scene.objects)-before:bpy.data.objects.remove(o,do_unlink=True)
 def rail(name,p,q,link=None):
  length=math.hypot(q[0]-p[0],q[2]-p[2]);ux,uz=(q[0]-p[0])/length,(q[2]-p[2])/length;n=math.ceil(length/2)
  for i in range(n):
   mesh=template.copy();o=bpy.data.objects.new(site+' constant-cell guard '+name+' '+str(i),mesh);bpy.context.collection.objects.link(o)
   for v in mesh.vertices:
    t=(v.co.x+1)*length/n/2+i*length/n;off=v.co.y;v.co=(p[0]+ux*t-uz*off,-(p[2]+uz*t+ux*off),p[1]+(q[1]-p[1])*t/length+v.co.z)
   o['role']='indexed supported constant-flight guard';o['source_asset']='level_blockout_guardrail'
   if link:o['link']=link
   built.append(o)
 for link,p,q,n in FLIGHTS[site]:
  ux,uz=(q[0]-p[0])/math.hypot(q[0]-p[0],q[2]-p[2]),(q[2]-p[2])/math.hypot(q[0]-p[0],q[2]-p[2])
  for side in [-.83,.83]:rail(link+' flight '+str(side),(p[0]-uz*side,p[1],p[2]+ux*side),(q[0]-uz*side,q[1],q[2]+ux*side),link)
 if site=='WLD-08':
  for a,c,label in [(11,19.76,'west inside'),(20.04,22.425,'west outside'),(41.575,43.96,'east outside'),(44.24,53,'east inside')]:
   for z in [33.875,36.125]:rail(label+' grade rim '+str(z),(a,0,z),(c,0,z))
  for x in [9.775,12.225]:rail('generator rim '+str(x),(x,0,17),(x,0,22.425))
  for x,label in [(22.425,'west lower slot return'),(41.575,'east lower slot return')]:rail(label,(x,0,33.875),(x,0,36.125))
  rail('generator lower slot return',(9.775,0,22.425),(12.225,0,22.425))
 elif site=='WLD-12':
  for x in [26.775,29.225]:rail('north rim '+str(x),(x,0,28),(x,0,34.3 if x<28 else 32.3))
  for z in [36.875,39.125]:rail('west rim '+str(z),(13,0,z),(31.125,0,z))
  box('north stair lower rim header bearing',26.65,27.75,29.35,28,-.30,0)
  rail('north stair lower slot return',(26.775,0,27.875),(29.225,0,27.875));rail('west lower slot return',(31.125,0,36.875),(31.125,0,39.125))
  rail('platform south edge',(10.14,0,44.36),(15.86,0,44.36));rail('platform east south edge',(15.86,0,39.25),(15.86,0,44.36));rail('platform east north edge',(15.86,0,36.14),(15.86,0,36.75))
 else:
  for x in [4.775,7.225]:
   rail('escape outside rim '+str(x),(x,0,34),(x,0,41.96));rail('escape inside rim '+str(x),(x,0,42.24),(x,0,48))
 # Retain original terrain materials/UV convention while adopting the actual saved punched mesh.
 vs=[];faces=[]
 for p in sorted((world/'scene-package/meshes').glob('site*.obj')):
  offset=len(vs)
  for line in p.read_text().splitlines():
   q=line.split()
   if q and q[0]=='v':x,y,z=map(float,q[1:]);vs.append((x,-z,y))
   elif q and q[0]=='f':faces.append(tuple(offset+int(v.split('/')[0])-1 for v in q[1:]))
 mesh=bpy.data.meshes.new(site+' actual saved stair-cell terrain');mesh.from_pydata(vs,[],faces);mesh.update();ground=bpy.data.objects.new('Saved editor rectangular stair-cell terrain '+site,mesh);bpy.context.collection.objects.link(ground);ground['role']='saved editor terrain / actual rectangular stair-cell punches';ground['saved_world']=row['world']
 for mat in oldterrainmat:mesh.materials.append(mat)
 uv=mesh.uv_layers.new(name='World metre terrain UV')
 for f in mesh.polygons:
  p=sum((mesh.vertices[i].co for i in f.vertices),Vector())/len(f.vertices);hit=terrain_tree.find_nearest(p);f.material_index=terrain_materials[hit[2]] if hit and hit[2] is not None else 0
  for i in f.loop_indices:
   p=mesh.vertices[mesh.loops[i].vertex_index].co;uv.data[i].uv=(p.x/8,p.y/8)
 assert face_keys(records(ground))==face_keys(native_faces(world/'terrain-physics.hcs2'));removed.add(terrain_name)
 for o in old:
  if o.name in removed:bpy.data.objects.remove(o,do_unlink=True)
 for o in built:
  bm=bmesh.new();bm.from_mesh(o.data);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=1e-6);bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces));assert all(e.is_manifold for e in bm.edges) and bm.calc_volume(signed=True)>0,(site,o.name);bm.to_mesh(o.data);bm.free()
 revised={
 'WLD-08':{'L17':[[10,0,35],[11,0,35],[22,-2.5,35],[23.5,-2.5,35],[23.5,-2.5,31],[32,-2.5,31]],'L18':[[32,-2.5,31],[40.5,-2.5,31],[40.5,-2.5,35],[42,-2.5,35],[53,0,35],[54,0,35]],'L20':[[11,-2.5,22],[11,0,17],[11,0,16]]},
 'WLD-12':{'L17':[[31,-3,32],[32.5,-3,32],[32.5,-3,38],[31,-3,38],[13,0,38],[11,0,38],[11,0,42]]},
 'WLD-18':{'route-08':[[14,-4,34],[7.45,-4,34],[7.45,-4,33.15],[6,-4,33.15],[6,-4,34],[6,0,48]]}}[site]
 if site=='WLD-08':
  anchors={(11,0,35):[10,0,35],(53,0,35):[54,0,35],(11,0,17):[11,0,16]}
  for link in data['links']:
   if any(abs(p[1]-q[1])>.01 for p,q in zip(link['points'],link['points'][1:])):continue
   link['points']=[anchors.get(tuple(p),p) for p in link['points']]
   clean=[]
   for point in link['points']:
    if not clean or point!=clean[-1]:clean.append(point)
   link['points']=clean
  data['authored_stair_landing_anchors']={'lab':[10,0,35],'dorm':[54,0,35],'generator':[11,0,16]}
  next(l for l in data['links'] if l['id']=='L15')['points']=[[53,0,10],[53,0,28],[54,0,28],[54,0,35]]
 before_mapping=json.loads(json.dumps(meta.get('source_segment_map',{})));new_mapping=json.loads(json.dumps(before_mapping))
 for link in data['links']:
  if link['id'] not in revised:continue
  old_vertical=next(i for i,(p,q) in enumerate(zip(link['points'],link['points'][1:])) if abs(p[1]-q[1])>.01);points=revised[link['id']];new_vertical=next(i for i,(p,q) in enumerate(zip(points,points[1:])) if abs(p[1]-q[1])>.01);new_mapping[link['id']]={str(new_vertical):before_mapping.get(link['id'],{}).get(str(old_vertical),old_vertical)};link['points']=points
 if site=='WLD-12':data['zones'].append(dict(id='west-maintenance-platform',name='Supported west stair maintenance platform',bounds=[10,36,16,44.5],y=0,height=3,kind='outside',parent='west-return',purpose='Real landing and maintenance access on structural platform',constructed_surface='Native-soil founded platform with bearing walls/steel members'))
 if site=='WLD-18':data['zones'].append(dict(id='escape-lower-connector',name='Covered western escape-stair connector',bounds=[6,32.8,8,35.2],y=-4,height=2.75,kind='circulation',parent='west',purpose='Enclosed side access to the actual lower escape-stair landing'))
 data['constant_stair_cells']=proof;data['authoritative_saved_terrain']=dict(world=row['world'],source_world=row['source_world'],files_sha256={p.relative_to(ROOT).as_posix():sha(p) for p in world.rglob('*') if p.is_file() and not p.name.endswith('.lock')},rectangular_well_cuts=row['cuts'],prior_punches_preserved=True)
 if site=='WLD-18':meta['doorways']=meta.get('doorways',[])+[portal]
 layout=folder/'authored-layout.json';write(layout,data);bpy.ops.wm.save_as_mainfile(filepath=str(folder/'constant-cells.blend'),compress=True);bpy.ops.object.select_all(action='DESELECT')
 for o in built+[ground]:o.select_set(True)
 addition=ROOT/'build'/('constant-cell-addition-'+site+'.glb');bpy.ops.export_scene.gltf(filepath=str(addition),export_format='GLB',export_extras=True,use_selection=True);append(source,addition,folder/'constant-cells.glb',remove_mesh_names=sorted(removed));parts=native_write(native,folder/'constant-cells.hcs2',removed,built);kept=[p for p in split_native(folder/'constant-cells.hcs2') if not p[0].startswith('terrain-')];raw=(world/'terrain-physics.hcs2').read_bytes();magic,count,total=struct.unpack_from('>III',raw);(folder/'constant-cells.hcs2').write_bytes(struct.pack('>III',magic,len(kept)+count,sum(p[1] for p in kept)+total)+b''.join(p[2] for p in kept)+raw[12:])
 ground_name=ground.name
 bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(folder/'constant-cells.glb'));objs={o.name:o for o in bpy.context.scene.objects if o.type=='MESH'}
 for name,ts in parts:assert face_keys(records(objs[name]))==face_keys(ts)
 assert face_keys(records(objs[ground_name]))==face_keys(native_faces(world/'terrain-physics.hcs2'))
 for o in objs.values():
  if o.get('role') in ['enclosed pitched roof','ceiling-cap']:o.hide_render=True
 scene=bpy.context.scene;scene.render.engine='BLENDER_WORKBENCH';scene.display.shading.color_type='MATERIAL';scene.display.shading.show_cavity=True;scene.render.resolution_x=1500;scene.render.resolution_y=1100;scene.render.resolution_percentage=100;scene.world=bpy.data.worlds.new('Constant stair-cell review');bpy.ops.object.camera_add();cam=bpy.context.object;scene.camera=cam;target=Vector((32,-32,-.5)) if site=='WLD-08' else Vector((24,-34,-.5)) if site=='WLD-12' else Vector((8,-42,-1));cam.location=target+Vector((26,-32,45));cam.rotation_euler=(target-cam.location).to_track_quat('-Z','Y').to_euler();cam.data.type='ORTHO';cam.data.ortho_scale=60 if site=='WLD-08' else 43;cap=BASE/'captures/constant-flight-construction-batch'/(site.lower()+'.png');cap.parent.mkdir(parents=True,exist_ok=True);scene.render.filepath=str(cap);bpy.ops.render.render(write_still=True)
 oldsteps=meta['stair_components'];meta.update(status='CONSTANT_STAIR_CELLS_CONSTRUCTED_REMAINING_SITE_WORK_AND_RUNTIME_OPEN',source_contract=s['construction'],source_glb=s['glb'],source_glb_sha256=s['glb_sha256'],source_physics=s['native'],source_physics_sha256=s['native_sha256'],before_layout=s['layout'],before_layout_sha256=sha(ROOT/s['layout']),before_segment_map=before_mapping,source_segment_map=new_mapping,before_stair_components=oldsteps,stair_components=[n for n in oldsteps if n.split(':',1)[-1] not in removed]+steps,layout=layout.relative_to(ROOT).as_posix(),layout_sha256=sha(layout),glb_sha256=sha(folder/'constant-cells.glb'),physics_sha256=sha(folder/'constant-cells.hcs2'),blend_sha256=sha(folder/'constant-cells.blend'),constant_flights=proof,removed_flared_or_conflicting_parts=sorted(removed),saved_world=row['world'],saved_world_files_sha256=data['authoritative_saved_terrain']['files_sha256'],cell_builder='tools/build_constant_flight_cells.py',cell_builder_sha256=sha(Path(__file__)),terrain_punch_builder='tools/java/RebuildStairCellPunches.java',terrain_punch_builder_sha256=sha(ROOT/'tools/java/RebuildStairCellPunches.java'),captures=[dict(path=cap.relative_to(ROOT).as_posix(),sha256=sha(cap),type='Native actual constant stair cells and retaining/landing structure, roofs hidden, no runtime acceptance')]);write(folder/'construction.json',meta);print('CONSTANT_CELLS_CONSTRUCTED',site,len(built),flush=True)
 return dict(id=site,construction=(folder/'construction.json').relative_to(ROOT).as_posix(),layout=layout.relative_to(ROOT).as_posix(),glb=(folder/'constant-cells.glb').relative_to(ROOT).as_posix(),native=(folder/'constant-cells.hcs2').relative_to(ROOT).as_posix(),blend=(folder/'constant-cells.blend').relative_to(ROOT).as_posix(),glb_sha256=meta['glb_sha256'],native_sha256=meta['physics_sha256'])

def main():
 program=json.loads((OUT/'construction-program.json').read_text());rows={r['id']:r for r in program['sites']};r=json.loads((BASE/'stair-cell-construction-pass/roster.json').read_text());active=[build(rows[s['id']]) if s['id'] in rows else s for s in r['active_sources']]
 for s in r['sites']:
  if s['id'] in rows:s['decision']='CONSTANT_FLIGHTS_RECTANGULAR_CELLS_CONSTRUCTED_REMAINING_WORK_OPEN'
 write(OUT/'roster.json',dict(active_sources=active,sites=r['sites'],baseline='development/levels/rebuild/stair-cell-construction-pass/roster.json'));print('CONSTANT_FLIGHT_BATCH_COMPLETE',flush=True)
if __name__=='__main__':main()
