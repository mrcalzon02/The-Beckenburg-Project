"""Rebuild Lookout landscape architecture from datums and fixed stair bays."""
from pathlib import Path
import sys,json,math,copy,struct,io
import bpy,bmesh
from mathutils import Vector
from mathutils.bvhtree import BVHTree
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
from planar_structure import plate
from build_roster_foundations import material,prism
from build_roster_floor_closures import difference
from append_static_glb import append
from compose_room_terrain import split_native
from review_roster_macroterrain import records,sha
from export_roster_collision import face_keys,native_faces
from build_deliberate_entrances import write
from build_architectural_doorways import frame_template
B=ROOT/'development/levels/rebuild';O=B/'service-ground-bay-pass';out=O/'wld-04';out.mkdir(parents=True,exist_ok=True)
design=json.loads((O/'program/wld-04/design.json').read_text());row=design['source'];world=ROOT/design['world'];data=json.loads((ROOT/row['layout']).read_text());before=copy.deepcopy(data);meta=json.loads((ROOT/row['construction']).read_text())
bpy.ops.wm.read_factory_settings(use_empty=True);bpy.context.preferences.filepaths.save_version=0;bpy.ops.import_scene.gltf(filepath=str(ROOT/row['glb']));old=[o for o in bpy.context.scene.objects if o.type=='MESH'];removed=set();built=[];steps=[];flights=[];receivers=[]
selected={'L03','L04','L06','L09','L10','L11'}
for o in old:
 role=o.get('role','');link=o.get('link','')
 if role=='placed source-library furnishing':continue
 if link in selected and any(k in role for k in ['stair','landing','guard','support','waist','stringer']):removed.add(o.name)
 if role.startswith('rough route support') or role=='structural-arrival-end-bearing':removed.add(o.name)
terrain=next(o for o in old if o.get('role','').startswith('saved editor terrain'));removed.add(terrain.name);mats=list(terrain.data.materials)
verts=[];faces=[]
for path in sorted((world/'scene-package/meshes').glob('site*.obj')):
 offset=len(verts)
 for line in path.read_text().splitlines():
  a=line.split()
  if a and a[0]=='v':x,y,z=map(float,a[1:]);verts.append((x,-z,y))
  elif a and a[0]=='f':faces.append(tuple(offset+int(i.split('/')[0])-1 for i in a[1:]))
mesh=bpy.data.meshes.new('Saved geographic terrace terrain');mesh.from_pydata(verts,[],faces);mesh.update();ground=bpy.data.objects.new('Saved editor geographic Lookout terraces',mesh);bpy.context.collection.objects.link(ground);ground['role']='saved editor terrain / architectural geographic terraces';ground['saved_world']=design['world']
for mt in mats:mesh.materials.append(mt)
uv=mesh.uv_layers.new(name='World metre terrain UV')
for p in mesh.polygons:
 p.material_index=1 if len(mats)>1 and p.normal.z<.93 else 0
 for i in p.loop_indices:v=mesh.vertices[mesh.loops[i].vertex_index].co;uv.data[i].uv=(v.x/8,v.y/8)
assert face_keys(records(ground))==face_keys(native_faces(world/'terrain-physics.hcs2'))
tree=BVHTree.FromPolygons(verts,faces)
def soil(x,z):
 hit=tree.ray_cast(Vector((x,-z,40)),Vector((0,0,-1)),100)[0];assert hit is not None,('No real soil receiver',x,z);return hit.z
concrete=material('Lookout geographic stair foundation',(.36,.36,.33));steel=material('Lookout fitted landscape steel stair',(.27,.29,.27))
def rect(a,b,c,d):return [(a,b),(c,b),(c,d),(a,d)]
def slab(name,poly,y,depth,role,mt=concrete):
 made=plate('Lookout terrace '+name,[poly],y,depth,mt) or []
 for o in made:o['role']=role;built.append(o)
 return made
# Entire ground-floor plates replace the obsolete diagonal floor cuts in all
# three existing buildings. Deliberate doors in retained walls stay authoritative.
for zone,foot in [('hut',[4,14,20,26]),('ruin',[40,14,54,30]),('store',[40,30,50,40])]:
 floor_mat=next(o.data.materials[0] for o in old if o.get('role')=='floor' and o.get('zone')==zone)
 for o in old:
  if o.name in removed or o.get('role')=='placed source-library furnishing':continue
  if o.get('role') not in ['floor','structural-stair-landing']:continue
  vs=[o.matrix_world@v.co for v in o.data.vertices];lo=min(v.z for v in vs);hi=max(v.z for v in vs)
  if abs(hi-6)>.01 or hi-lo>.4:continue
  if max(v.x for v in vs)<=foot[0] or min(v.x for v in vs)>=foot[2] or max(-v.y for v in vs)<=foot[1] or min(-v.y for v in vs)>=foot[3]:continue
  outside=[]
  for p in o.data.polygons:
   ps=[o.matrix_world@o.data.vertices[i].co for i in p.vertices]
   if all(abs(v.z-hi)<.001 for v in ps):outside.extend(difference([(v.x,-v.y) for v in ps],rect(*foot)))
  removed.add(o.name)
  for i,p in enumerate(outside):slab('retained plate outside '+zone+' '+str(len(built)),p,hi,hi-lo,'retained existing floor outside replaced room',o.data.materials[0])
 for o in slab(zone+' complete ground floor closes obsolete stair cuts',rect(*foot),6,.30,'complete building ground floor',floor_mat):o['zone']=zone
# Replace the whole store south face: its old route-shaped cut was not a door.
# The stair arrives on a 1.5m level porch before this normal framed entrance.
for o in old:
 if 'Wall store S' in o.name or 'Header store S' in o.name:removed.add(o.name)
for label,a,c,lo,hi in [('west pier',40,43.9,6,9),('east pier',46.1,50,6,9),('lintel',43.9,46.1,8.45,9)]:slab('store south door '+label,rect(a,39.88,c,40.12),hi,hi-lo,'complete architectural wall')
frame=frame_template();frame.name='Lookout terrace store south normal indexed door'
for v in frame.data.vertices:v.co=(45-v.co.x*1.8,-40-v.co.y*.24/.18,6+v.co.z*2.3/2.125)
frame['role']='indexed architectural doorway';frame['source_asset']='level_blockout_doorway';built.append(frame)
door=dict(id='WLD-04-store-south-normal',axis='z',boundary=40,along=45,sill_y=6,clear_width=1.8,clear_height=2.3,frame_depth=.24,frame_span=[39.88,40.12],rooms=['store'])
data['authored_doorways']=data.get('authored_doorways',[])+[door]
def bearing(name,a,b,c,d,y,wall_connection=False,walking=True):
 hits=[];wall_receivers=[]
 for x in [a,(a+c)/2,c]:
  for z in [b,(b+d)/2,d]:
   hit=tree.ray_cast(Vector((x,-z,40)),Vector((0,0,-1)),100)[0]
   if hit is None:
    assert wall_connection,('No real soil receiver',name,x,z)
    wall_receivers.append([x,z]);continue
   hits.append(dict(x=x,z=z,soil_y=hit.z))
 assert len(hits)>=6,(name,'Insufficient soil bearing')
 lo=min(p['soil_y'] for p in hits)-.3;assert lo<y
 receivers.append(dict(name=name,soil=hits,footing_bottom=lo,top=y,existing_wall_connection_at=wall_receivers))
 made=slab(name,rect(a,b,c,d),y,y-lo,'structural-stair-landing' if walking else 'soil-founded buried entrance footing')
 # These are the real tread receiving landings and door porches. Bind them
 # with the treads so the existing production stair solver handles their
 # shallow soil-to-landing nosings; terrain/road areas never enter this set.
 if walking:steps.extend(o.name for o in made)
 return made
def beam(name,p,q):
 a=Vector((p[0],-p[2],p[1]));b=Vector((q[0],-q[2],q[1]));bpy.ops.mesh.primitive_cube_add(size=1,location=(a+b)/2);o=bpy.context.object;o.name='Lookout terrace '+name;o.rotation_euler=(b-a).to_track_quat('Z','Y').to_euler();o.scale=(.16,.28,(b-a).length);bpy.ops.object.transform_apply(location=True,rotation=True,scale=True);o.data.materials.append(steel);o['role']='abutment-bearing inclined stair stringer';built.append(o)
before_rail=set(bpy.context.scene.objects);bpy.ops.import_scene.gltf(filepath=str(B.parent/'primitive-props-v1/models/level_blockout_guardrail.glb'));rail=next(o for o in set(bpy.context.scene.objects)-before_rail if o.type=='MESH').data.copy()
for o in set(bpy.context.scene.objects)-before_rail:bpy.data.objects.remove(o,do_unlink=True)
def guard(name,p,q):
 run=math.hypot(q[0]-p[0],q[2]-p[2]);ux,uz=(q[0]-p[0])/run,(q[2]-p[2])/run;n=math.ceil(run/2)
 for i in range(n):
  mesh=rail.copy()
  for v in mesh.vertices:
   t=(v.co.x+1)*run/n/2+i*run/n;off=v.co.y;v.co=(p[0]+ux*t-uz*off,-(p[2]+uz*t+ux*off),p[1]+(q[1]-p[1])*t/run+v.co.z)
  o=bpy.data.objects.new('Lookout terrace '+name+' guard '+str(i),mesh);bpy.context.collection.objects.link(o);o['role']='indexed stringer or landing mounted guard';o['source_asset']='level_blockout_guardrail';built.append(o)
def flight(link,x,z0,z1,y0,y1,n,width=2):
 assert (y1-y0)/n<=.181 and abs(z1-z0)/n>=.25
 for i in range(n):
  za=z0+(z1-z0)*i/n;zb=z0+(z1-z0)*(i+1)/n;poly=rect(x-width/2,min(za,zb),x+width/2,max(za,zb));bottom=[y0+(y1-y0)*(z-z0)/(z1-z0)-.22 for _,z in poly]
  o=prism('Lookout terrace '+link+' tread '+str(i),poly,y0+(y1-y0)*(i+1)/n,bottom,steel);o['role']='structural-stair-step';o['link']=link;built.append(o);steps.append(o.name)
 for dx in [-width/2,width/2]:beam(link+' founded stringer '+str(dx),[x+dx,y0-.2,z0],[x+dx,y1-.2,z1]);guard(link+' flight '+str(dx),[x+dx,y0,z0],[x+dx,y1,z1])
 flights.append(dict(link=link,low=[x,y0,z0],high=[x,y1,z1],width=width,risers=n))
# The four separated bays follow two real hillside stair routes. Alternate
# guidance shares those stairs; no fifth diagonal shortcut is constructed.
for link,x,z0,z1,y0,y1,n in [('L03',20,58,53.2,0,3,17),('L04',28,36,31.2,3,5.65,15),('L09',45,46.3,41.5,3,6,17),('L10',43,58,53.2,0,3,17)]:
 bearing(link+' lower abutment',x-1.25,z0,x+1.25,z0+1.5,y0);bearing(link+' upper landing',x-1.25,z1-1.5,x+1.25,z1,y1);flight(link,x,z0,z1,y0,y1,n)
 # Outer side of the upper landing, leaving normal fore/aft circulation.
 for dx in [-1.18,1.18]:guard(link+' receiving side '+str(dx),[x+dx,y1,z1-1.4],[x+dx,y1,z1-.05])
# Level porches are solid founded objects at each existing door; their shallow
# steps meet the lower clearing. They do not break any walls or floors.
bearing('hut east level porch',20,18.8,21.8,21.2,6)
bearing('hut south level porch',10.8,26,13.2,27.8,6)
bearing('ruin west level porch',38.2,21,39.5,23,6)
bearing('store south level porch',43.75,40,46.25,41.5,6,wall_connection=True)
# Use the same real closed step section for the south hut entrance.
flight('HUT-SOUTH',12,28.6,27.8,5.65,6,2,2.4)
def xentry(link,xa,xb,z):
 for i in range(2):
  a=xa+(xb-xa)*i/2;b=xa+(xb-xa)*(i+1)/2;poly=rect(min(a,b),z-1,max(a,b),z+1);bottom=[5.65+.35*(x-xa)/(xb-xa)-.22 for x,_ in poly];o=prism('Lookout terrace '+link+' tread '+str(i),poly,5.65+.35*(i+1)/2,bottom,concrete);o['role']='structural-stair-step';o['link']=link;built.append(o);steps.append(o.name)
 # These short masonry entry steps bear across their full footprint in soil.
 bearing(link+' grounded receiving bed',min(xa,xb),z-1,max(xa,xb),z+1,5.45,walking=False)
 flights.append(dict(link=link,low=[xa,5.65,z],high=[xb,6,z],width=2,risers=2))
xentry('HUT-EAST',22.6,21.8,20);xentry('RUIN-WEST',37.4,38.2,22)
by={l['id']:l for l in data['links']};guides={
'L02':[[28,0,66],[14,0,60]],
'L03':[[14,0,60],[20,0,60],[20,0,58],[20,3,53.2],[20,3,50],[28,3,50],[28,3,36]],
'L04':[[28,3,36],[28,5.65,31.2],[28,5.65,28.8],[30,5.65,22]],
'L05':[[30,5.65,22],[24,5.65,22],[24,5.65,20],[22.6,5.65,20],[21.8,6,20],[12,6,20]],
'L06':[[12,6,20],[21.8,6,20],[22.6,5.65,20],[24,5.65,20],[24,5.65,22],[30,5.65,22],[28,5.65,28.8],[28,5.65,31.2],[28,3,36]],
'L07':[[30,5.65,22],[37.4,5.65,22],[38.2,6,22],[47,6,22]],
'L08':[[47,6,22],[45,6,22],[45,6,35]],
'L09':[[45,6,35],[45,6,41.5],[45,3,46.3],[45,3,47.1],[43,3,47.1],[43,3,51]],
'L10':[[43,3,51],[43,3,53.2],[43,0,58],[43,0,62],[28,0,62],[28,0,66]],
'L11':[[43,3,51],[43,3,47.1],[45,3,47.1],[45,3,46.3],[45,6,41.5],[45,6,35],[45,6,22],[38.2,6,22],[37.4,5.65,22],[30,5.65,22]],
'L12':[[30,5.65,22],[30.8,5.65,18],[30.8,5.650079727172852,16.8]]}
for key,points in guides.items():by[key]['points']=points
mapping=copy.deepcopy(meta.get('source_segment_map',{}));mapping.update({'L03':{'2':0},'L04':{'0':0},'L06':{'1':1,'7':0},'L09':{'1':0},'L10':{'1':0},'L11':{'3':0,'7':1},'L05':{'3':0},'L07':{'1':0}})
new_declarations=copy.deepcopy(meta.get('interface_added_flights',[]))
for link,i in [('L05',3),('L06',1),('L07',1),('L11',7)]:new_declarations.append(dict(link=link,segment=mapping[link][str(i)],layout_segment=i,points=by[link]['points'][i:i+2]))
for z in data['zones']:
 if z['id']=='lower':z['label_point']=[14,0,60]
 if z['id']=='clearing':z['y']=5.65;z['label_point']=[30,5.65,22]
 if z['id']=='bend':z['label_point']=[28,3,36]
 if z['id']=='return':z['label_point']=[43,3,51]
for o in old:
 if o.name in removed:bpy.data.objects.remove(o,do_unlink=True)
for o in built:
 bm=bmesh.new();bm.from_mesh(o.data);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=1e-6);bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces));assert all(e.is_manifold for e in bm.edges) and bm.calc_volume(signed=True)>0,o.name;bm.to_mesh(o.data);bm.free()
terrain_parts=[p for p in split_native(ROOT/row['native']) if p[0].startswith('terrain-')];assert terrain_parts
layout=out/'authored-layout.json';world_files={p.relative_to(ROOT).as_posix():sha(p) for p in world.rglob('*') if p.is_file() and not p.name.endswith('.lock')};data['current_saved_editor_terrain']=dict(world=design['world'],files_sha256=world_files,source_world=design['source_world'],source_height_unchanged=True,original_holes_unchanged=True);write(layout,data)
bpy.ops.wm.save_as_mainfile(filepath=str(out/'authored-service-bay.blend'),compress=True);bpy.ops.object.select_all(action='DESELECT');ground.select_set(True)
for o in built:o.select_set(True)
addition=ROOT/'build/lookout-terraces-addition.glb';bpy.ops.export_scene.gltf(filepath=str(addition),export_format='GLB',export_extras=True,use_selection=True);append(ROOT/row['glb'],addition,out/'authored-service-bay.glb',remove_mesh_names=sorted(removed))
oldparts=split_native(ROOT/row['native']);keep=[p for p in oldparts if not p[0].startswith('terrain-') and p[0].split(':',1)[-1] not in removed];groundbytes=(world/'terrain-physics.hcs2').read_bytes();magic,count,total=struct.unpack_from('>III',groundbytes);payload=b'';triangles=0
for o in built:
 ts=records(o);name=o.name.encode();payload+=struct.pack('>H',len(name))+name+struct.pack('>BI',1,len(ts))+b''.join(struct.pack('>9fB',*xyz,flags) for xyz,flags in ts);triangles+=len(ts)
native=out/'authored-service-bay.hcs2';native.write_bytes(struct.pack('>III',magic,len(keep)+count+len(built),sum(p[1] for p in keep)+total+triangles)+b''.join(p[2] for p in keep)+groundbytes[12:]+payload);assert split_native(native)[:len(keep)]==keep
write(out/'quarantine.json',dict(source=row,removed_meshes=sorted(removed),removed_native_terrain=[p[0] for p in terrain_parts],reason='Remove diagonal outdoor stair masses, route-derived landing shards and study strips; original sources retained. Alternate links now use the two actual hillside stair routes.'))
meta.update(status='GEOGRAPHIC_TERRACES_AND_SUPPORTED_STAIR_BAYS_NATIVE_REVIEW_OPEN',source_glb=row['glb'],source_glb_sha256=sha(ROOT/row['glb']),source_physics=row['native'],source_physics_sha256=sha(ROOT/row['native']),before_layout=row['layout'],before_layout_sha256=sha(ROOT/row['layout']),before_segment_map=meta.get('source_segment_map',{}),source_segment_map=mapping,before_stair_components=meta['stair_components'],stair_components=[s for s in meta['stair_components'] if s.split(':',1)[-1] not in removed]+steps,interface_added_flights=new_declarations,layout=layout.relative_to(ROOT).as_posix(),layout_sha256=sha(layout),glb_sha256=sha(out/'authored-service-bay.glb'),physics_sha256=sha(native),blend_sha256=sha(out/'authored-service-bay.blend'),removed_source_meshes=sorted(removed),new_interface_parts=[o.name for o in built],terrain_mesh=ground.name,saved_world=design['world'],saved_world_files_sha256=world_files,source_terrain=design['source_world'],flights=flights,foundation_receivers=receivers,architectural_plan=dict(ground_datums=[0,3,5.65],building_floors=6,tower_base=6,flights=flights,alternate_routes_share_stairs={'L06':'L04','L11':'L09'},terrain_program='program/wld-04/design.json'),builder='tools/build_lookout_terraces.py',builder_sha256=sha(Path(__file__)),retained_native_records=len(keep));write(out/'construction.json',meta)
write(out/'source-row.json',dict(id='WLD-04',construction=(out/'construction.json').relative_to(ROOT).as_posix(),layout=layout.relative_to(ROOT).as_posix(),glb=(out/'authored-service-bay.glb').relative_to(ROOT).as_posix(),native=native.relative_to(ROOT).as_posix(),blend=(out/'authored-service-bay.blend').relative_to(ROOT).as_posix(),glb_sha256=meta['glb_sha256'],native_sha256=meta['physics_sha256']))
print('LOOKOUT_ARCHITECTURE',len(removed),'retired',len(built),'new solids',sum(o.get('role')=='structural-stair-step' for o in built),'treads',len(steps),'stair collision components',flush=True)
