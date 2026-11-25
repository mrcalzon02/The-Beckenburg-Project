"""Architectural lower escape bay inside the Dam's existing saved excavation.

The room/floor/opening schedule is fixed before navigation. No terrain is cut,
no route extrusion is used, and the original turbine-floor route wounds close.
"""
from pathlib import Path
import sys,json,copy,math
import bpy,bmesh
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'tools'))
from planar_structure import plate
from build_roster_floor_closures import difference
from build_roster_foundations import material,prism
from build_circulation_construction import native_write
from compose_room_terrain import split_native
from append_static_glb import append,read
from review_roster_macroterrain import records,sha
from export_roster_collision import face_keys
from check_roster_decks import read_parts
from build_deliberate_entrances import write
B=ROOT/'development/levels/rebuild'
OUT=B/'service-ground-bay-pass/wld-09'
BASE=B/'household-sacred-bay-pass/roster.json'
RAIL=B.parent/'primitive-props-v1/models/level_blockout_guardrail.glb'

def rect(a,b,c,d):return [(a,b),(c,b),(c,d),(a,d)]
def bounds(o):
 p=[o.matrix_world@v.co for v in o.data.vertices]
 return [min(v.x for v in p),min(-v.y for v in p),max(v.x for v in p),max(-v.y for v in p),min(v.z for v in p),max(v.z for v in p)]

def build():
 OUT.mkdir(parents=True,exist_ok=True)
 row=next(x for x in json.loads(BASE.read_text())['active_sources'] if x['id']=='WLD-09')
 data=json.loads((ROOT/row['layout']).read_text());before=copy.deepcopy(data)
 meta=json.loads((ROOT/row['construction']).read_text());world=ROOT/meta['saved_world']
 worldhash={p.relative_to(ROOT).as_posix():sha(p) for p in world.rglob('*') if p.is_file()}
 # Deliberately allocated lower room bay; all datums checked as one section.
 plan=dict(bay=[59.2,34.6,63.6,41.8],well=[59.2,36.4,63.6,41.8],
  floors=[-10,-8,-6],lanes=[60.3,62.5],flight_z=[36.4,40],width=1.8,
  risers_per_flight=12,turn_landing=[59.4,40,63.4,41.8],
  upper_arrival=[61.6,34.6,63.4,36.4],existing_gallery_well=[54.9,23.3,57.1,30.5],
  saved_terrain='Reuse existing architectural room excavation; no new holes or height edits',
  spillway='OPEN: existing proposed z58 corridor intersects unpunched saved soil; no shortcut built')
 bpy.ops.wm.read_factory_settings(use_empty=True);bpy.context.preferences.filepaths.save_version=0
 bpy.ops.import_scene.gltf(filepath=str(ROOT/row['glb']))
 old=[o for o in bpy.context.scene.objects if o.type=='MESH'];removed=set();built=[];steps=[]
 concrete=material('Dam authored escape bay concrete',(.39,.40,.38))
 steel=material('Dam authored escape bay steel',(.25,.28,.27))
 def slab(name,poly,y,depth,role='architectural bay floor',mt=None):
  made=plate('WLD-09 service bay '+name,poly,y,depth,mt or concrete) or []
  for o in made:o['role']=role;built.append(o)
  return made
 def box(name,a,b,c,d,lo,hi,role='bay bearing connected to lower room slab'):
  return slab(name,[rect(a,b,c,d)],hi,hi-lo,role)
 def floor(name,foot,y,depth,cuts):
  polys=[foot]
  for cut in cuts:polys=[q for p in polys for q in difference(p,cut)]
  return slab(name,polys,y,depth,'complete room slab with designed rectangular stair bay')
 # Retire the whole obsolete slab assemblage. Preserve every library prop.
 for o in old:
  if o.get('role')=='placed source-library furnishing':continue
  if o.get('zone')=='turbine' and o.get('role')=='floor':removed.add(o.name)
  if 'closed old return escape roof' in o.name:removed.add(o.name)
  if o.get('link')=='L18' and any(k in o.get('role','') for k in ['stair','landing','support','guard','waist']):removed.add(o.name)
 well=rect(*plan['well']);gallerywell=rect(*plan['existing_gallery_well'])
 floor('complete turbine floor',rect(36,22,64,48),-6,.30,[well,gallerywell])
 floor('complete lower return roof',rect(50,32,64,48),-7,.30,[well])
 # Outer cheeks are continuous bearing walls tied to the existing lower floor.
 box('west stair cheek',59.2,36.4,59.4,41.8,-10.3,-6.3)
 box('east stair cheek',63.4,36.4,63.6,41.8,-10.3,-6.3)
 box('south end retaining return',59.2,41.8,63.6,42,-10.3,-6.3)
 slab('supported middle landing',[rect(*plan['turn_landing'])],-8,.30,'wall-bearing middle landing')
 box('middle landing transverse beam',59.2,40.1,63.6,40.4,-8.6,-8.3)
 box('middle landing founded central pier',61.2,40.1,61.6,40.4,-10.3,-8.3)
 # Wall-supported reinforced flights; separate straight lanes, same stairway.
 for flight,(x,z0,z1,y0,y1) in enumerate([(60.3,36.4,40,-10,-8),(62.5,40,36.4,-8,-6)]):
  poly=rect(x-.9,min(z0,z1),x+.9,max(z0,z1))
  # A closed inclined waist has exactly the same bearing endpoints as treads.
  bottom=[y0+(y1-y0)*(z-z0)/(z1-z0)-.4 for _,z in poly]
  top=[v+.2 for v in bottom]
  vs=[(px,-pz,h) for layer in [bottom,top] for (px,pz),h in zip(poly,layer)]
  fs=[(3,2,1,0),(4,5,6,7),(0,1,5,4),(1,2,6,5),(2,3,7,6),(3,0,4,7)]
  mesh=bpy.data.meshes.new('closed reinforced waist');mesh.from_pydata(vs,[],fs);mesh.materials.append(concrete)
  o=bpy.data.objects.new('WLD-09 service bay inclined waist '+str(flight),mesh);bpy.context.collection.objects.link(o);o['role']='continuous wall-bearing stair waist';o['link']='L18';built.append(o)
  for i in range(12):
   za,zb=z0+(z1-z0)*i/12,z0+(z1-z0)*(i+1)/12;poly=rect(x-.9,min(za,zb),x+.9,max(za,zb))
   bottom=[y0+(y1-y0)*(z-z0)/(z1-z0)-.2 for _,z in poly]
   o=prism('WLD-09 service bay L18 tread '+str(flight*12+i),poly,y0+(y1-y0)*(i+1)/12,bottom,concrete)
   o['role']='structural-stair-step';o['link']='L18';built.append(o);steps.append(o.name)
 # The well is rimmed by structural slab edges; indexed guards mount on them.
 before_import=set(bpy.context.scene.objects);bpy.ops.import_scene.gltf(filepath=str(RAIL))
 rail=next(o for o in set(bpy.context.scene.objects)-before_import if o.type=='MESH').data.copy()
 for o in set(bpy.context.scene.objects)-before_import:bpy.data.objects.remove(o,do_unlink=True)
 def guard(name,p,q):
  run=math.hypot(q[0]-p[0],q[2]-p[2]);ux,uz=(q[0]-p[0])/run,(q[2]-p[2])/run;n=math.ceil(run/2)
  for i in range(n):
   mesh=rail.copy()
   for v in mesh.vertices:
    t=(v.co.x+1)*run/n/2+i*run/n;off=v.co.y
    v.co=(p[0]+ux*t-uz*off,-(p[2]+uz*t+ux*off),p[1]+(q[1]-p[1])*t/run+v.co.z)
   o=bpy.data.objects.new('WLD-09 service bay '+name+' guard '+str(i),mesh);bpy.context.collection.objects.link(o)
   o['role']='indexed structural stair bay guard';o['source_asset']='level_blockout_guardrail';built.append(o)
 for x in [59.47,61.13]:guard('lower flight '+str(x),[x,-10,36.4],[x,-8,40])
 for x in [61.67,63.33]:guard('upper flight '+str(x),[x,-8,40],[x,-6,36.4])
 for p,q in [([59.47,-8,41.7],[63.33,-8,41.7]),([59.47,-8,40],[59.47,-8,41.7]),([63.33,-8,40],[63.33,-8,41.7]),([59.13,-6,36.3],[61.4,-6,36.3]),([59.13,-6,36.3],[59.13,-6,42.1]),([59.13,-6,42.1],[63.7,-6,42.1]),([63.7,-6,42.1],[63.7,-6,36.3])]:guard('supported landing rim',p,q)
 # Navigation now describes the built stair and the existing east hall doorway.
 by={l['id']:l for l in data['links']}
 lower=[[57,-10,40],[57,-10,35],[60.3,-10,35],[60.3,-10,36.4],[60.3,-8,40],[60.3,-8,40.9],[62.5,-8,40.9],[62.5,-8,40],[62.5,-6,36.4],[62.5,-6,35]]
 primary=by['L11']['points'][1:]
 by['L18']['points']=lower+primary
 by['L18']['construction_status']='AUTHORED_LOWER_U_BAY_JOINED_TO_EXISTING_MAIN_EAST_HALL'
 by['L18']['construction']='One reserved lower return/turbine bay; two flights meet at a supported middle landing. Upper arrival uses the existing actual east-hall doorway and main stair.'
 data['architectural_service_bay']=plan
 assert sum(z.get('player_access')!='SEALED_CONTEXT' for z in data['zones'])<=40
 quarantine=[dict(name=o.name,role=o.get('role'),zone=o.get('zone'),bounds=bounds(o)) for o in old if o.name in removed]
 write(OUT/'quarantine.json',dict(source=row,reason='Complete turbine floor/return roof replaced by coordinated designed stair wells; old imaginary emergency guide retained here only',meshes=quarantine,old_emergency_guide=next(l for l in before['links'] if l['id']=='L18')))
 for o in old:
  if o.name in removed:bpy.data.objects.remove(o,do_unlink=True)
 for o in built:
  bm=bmesh.new();bm.from_mesh(o.data);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=1e-6);bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces))
  assert all(e.is_manifold for e in bm.edges) and bm.calc_volume(signed=True)>0,o.name
  bm.to_mesh(o.data);bm.free()
 write(OUT/'authored-layout.json',data)
 bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'authored-service-bay.blend'),compress=True)
 bpy.ops.object.select_all(action='DESELECT')
 for o in built:o.select_set(True)
 add=ROOT/'build/dam-service-bay-addition.glb';bpy.ops.export_scene.gltf(filepath=str(add),export_format='GLB',export_extras=True,use_selection=True)
 append(ROOT/row['glb'],add,OUT/'authored-service-bay.glb',remove_mesh_names=sorted(removed))
 native_write(ROOT/row['native'],OUT/'authored-service-bay.hcs2',removed,built)
 mapping=copy.deepcopy(meta.get('source_segment_map',{}));mapping['L18']={'3':0,'7':2,'13':3,'15':4}
 added=[v for v in meta.get('interface_added_flights',[]) if v['link']!='L18']
 for segment,layout_segment in [(3,13),(4,15)]:added.append(dict(link='L18',segment=segment,layout_segment=layout_segment,points=by['L18']['points'][layout_segment:layout_segment+2]))
 meta.update(status='ARCHITECTURAL_LOWER_SERVICE_BAY_NATIVE_CANDIDATE_RUNTIME_OPEN',source_glb=row['glb'],source_glb_sha256=sha(ROOT/row['glb']),source_physics=row['native'],source_physics_sha256=sha(ROOT/row['native']),before_layout=row['layout'],before_layout_sha256=sha(ROOT/row['layout']),before_segment_map=meta.get('source_segment_map',{}),source_segment_map=mapping,before_stair_components=meta['stair_components'],stair_components=[n for n in meta['stair_components'] if n.split(':',1)[-1] not in removed]+steps,interface_added_flights=added,layout=(OUT/'authored-layout.json').relative_to(ROOT).as_posix(),layout_sha256=sha(OUT/'authored-layout.json'),removed_source_meshes=sorted(removed),new_interface_parts=[o.name for o in built],architectural_plan=plan,builder='tools/build_dam_service_bay.py',builder_sha256=sha(Path(__file__)),glb_sha256=sha(OUT/'authored-service-bay.glb'),physics_sha256=sha(OUT/'authored-service-bay.hcs2'),blend_sha256=sha(OUT/'authored-service-bay.blend'),unchanged_saved_world_files=worldhash)
 write(OUT/'construction.json',meta)
 entry=dict(id='WLD-09',construction=(OUT/'construction.json').relative_to(ROOT).as_posix(),layout=meta['layout'],glb=(OUT/'authored-service-bay.glb').relative_to(ROOT).as_posix(),native=(OUT/'authored-service-bay.hcs2').relative_to(ROOT).as_posix(),blend=(OUT/'authored-service-bay.blend').relative_to(ROOT).as_posix(),glb_sha256=meta['glb_sha256'],native_sha256=meta['physics_sha256'])
 write(OUT/'source-row.json',entry)
 print('DAM_SERVICE_BAY_BUILT',len(removed),'removed',len(built),'new parts',flush=True)

def review():
 meta=json.loads((OUT/'construction.json').read_text());glb=OUT/'authored-service-bay.glb';native=OUT/'authored-service-bay.hcs2'
 assert sha(glb)==meta['glb_sha256'] and sha(native)==meta['physics_sha256']
 assert all(sha(ROOT/p)==h for p,h in meta['unchanged_saved_world_files'].items())
 a,blob=read(ROOT/meta['source_glb']);b,after=read(glb);assert after[:len(blob)]==blob
 for key in ['meshes','accessors','bufferViews','images','textures','materials']:assert b.get(key,[])[:len(a.get(key,[]))]==a.get(key,[])
 removed=set(meta['removed_source_meshes'])
 for i,node in enumerate(a['nodes']):
  expected=dict(node)
  if node.get('name') in removed:expected.pop('mesh',None)
  assert b['nodes'][i]==expected
 old=split_native(ROOT/meta['source_physics']);new=split_native(native);keep=[p for p in old if p[0].split(':',1)[-1] not in removed];assert new[:len(keep)]==keep
 bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(glb));objs={o.name:o for o in bpy.context.scene.objects if o.type=='MESH'};parts=read_parts(native)
 for name in meta['new_interface_parts']:
  o=objs[name];assert face_keys(records(o))==face_keys(parts[name][1]),name
  bm=bmesh.new();bm.from_mesh(o.data);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=1e-6);assert all(e.is_manifold for e in bm.edges) and bm.calc_volume(signed=True)>0,name;bm.free()
 cap=OUT/'captures';cap.mkdir(exist_ok=True)
 sc=bpy.context.scene;sc.render.engine='BLENDER_WORKBENCH';sc.display.shading.color_type='MATERIAL';sc.display.shading.show_cavity=True;sc.render.resolution_x=1400;sc.render.resolution_y=1050;sc.render.resolution_percentage=100;sc.view_settings.view_transform='Standard';sc.world=bpy.data.worlds.new('Dam service architecture review')
 bpy.ops.object.camera_add();cam=bpy.context.object;sc.camera=cam;proof=[]
 for name,position,target,kind,scale in [
  ('lower-standing',(60.3,-34.5,-8.38),(60.3,-39.7,-8.4),'PERSP',23),
  ('upper-standing',(62.5,-33,-4.38),(62.5,-39,-6.8),'PERSP',23),
  ('bay-structural-section',(53,-31,-.5),(61.3,-38.5,-8),'ORTHO',14)]:
  hidden=[]
  for o in objs.values():
   bb=bounds(o);o.hide_render=False
   if name=='bay-structural-section':o.hide_render='terrain' in o.get('role','') or bb[4]>=-3.31 or ('return roof' in o.name) or ('complete turbine floor' in o.name) or ('west stair cheek' in o.name)
   if o.hide_render:hidden.append(o.name)
  cam.data.type=kind;cam.data.lens=scale;cam.data.ortho_scale=scale;cam.data.clip_start=.02;cam.data.clip_end=300;cam.location=position;cam.rotation_euler=(Vector(target)-cam.location).to_track_quat('-Z','Y').to_euler();path=cap/(name+'.png');sc.render.filepath=str(path);bpy.ops.render.render(write_still=True)
  proof.append(dict(path=path.relative_to(ROOT).as_posix(),sha256=sha(path),type='Native actual exported source; structural section hides listed terrain/overhead layers; no runtime acceptance',hidden=hidden))
 write(OUT/'captures.json',proof)
 write(OUT/'export-verification.json',dict(closed_positive_new_solids=len(meta['new_interface_parts']),retained_native_records=len(keep),new_treads=24,source_buffer_asset_arrays_and_nodes_exact=True,native_export_triangles_exact=True,saved_terrain_holes_and_layers_unchanged=True,runtime_acceptance=False))
 print('DAM_SERVICE_EXPORT_REVIEW_PASS',flush=True)

if __name__=='__main__':
 args=sys.argv[sys.argv.index('--')+1:] if '--' in sys.argv else []
 if '--review-only' not in args:build()
 review()
