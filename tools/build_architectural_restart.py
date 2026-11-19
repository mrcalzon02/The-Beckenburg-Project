"""Bounded replacement of rejected tower geometry and Dam workshop envelope.
All plans below are architectural bays/datums, authored before movement guides.
"""
from pathlib import Path
import sys,json,math,copy
import bpy,bmesh
from mathutils import Vector
from mathutils.bvhtree import BVHTree
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'tools'))
from planar_structure import plate
from build_roster_foundations import material,prism
from build_circulation_construction import native_write
from append_static_glb import append
from compose_room_terrain import split_native
from review_roster_macroterrain import records,sha
from export_roster_collision import face_keys
from build_architectural_doorways import frame_template
from build_deliberate_entrances import write
B=ROOT/'development/levels/rebuild';O=B/'architectural-restart-pass'
BASE=B/'dam-contained-stair-pass/roster.json'
def rect(a,b,c,d):return [(a,b),(c,b),(c,d),(a,d)]
def bounds(o):
 p=[o.matrix_world@v.co for v in o.data.vertices]
 return [min(v.x for v in p),min(-v.y for v in p),max(v.x for v in p),max(-v.y for v in p),min(v.z for v in p),max(v.z for v in p)]
def build(row):
 site=row['id'];out=O/site.lower();out.mkdir(parents=True,exist_ok=True)
 meta=json.loads((ROOT/row['construction']).read_text());data=json.loads((ROOT/row['layout']).read_text());before=copy.deepcopy(data)
 bpy.ops.wm.read_factory_settings(use_empty=True);bpy.context.preferences.filepaths.save_version=0
 bpy.ops.import_scene.gltf(filepath=str(ROOT/row['glb']));old=[o for o in bpy.context.scene.objects if o.type=='MESH'];removed=set();built=[];steps=[];doors=[];flights=[];receivers=[]
 concrete=material(site+' architectural concrete',(.39,.40,.37));steel=material(site+' weathered tower steel',(.24,.28,.27));wallmat=material(site+' retained envelope finish',(.45,.48,.47))
 def slab(name,poly,y,depth,role,mt=None):
  made=plate(site+' restart '+name,[poly],y,depth,mt or concrete) or []
  for o in made:o['role']=role;built.append(o)
  return made
 def box(name,a,b,c,d,lo,hi,role='architectural bearing',mt=None):return slab(name,rect(a,b,c,d),hi,hi-lo,role,mt)
 def beam(name,p,q,width=.18,depth=.24):
  v1=Vector((p[0],-p[2],p[1]));v2=Vector((q[0],-q[2],q[1]));bpy.ops.mesh.primitive_cube_add(size=1,location=(v1+v2)/2);o=bpy.context.object;o.name=site+' restart '+name;o.rotation_euler=(v2-v1).to_track_quat('Z','Y').to_euler();o.scale=(width,depth,(v2-v1).length);bpy.ops.object.transform_apply(location=True,rotation=True,scale=True);o.data.materials.append(steel);o['role']='connected structural frame';built.append(o)
 railbefore=set(bpy.context.scene.objects);bpy.ops.import_scene.gltf(filepath=str(B.parent/'primitive-props-v1/models/level_blockout_guardrail.glb'));rail=next(o for o in set(bpy.context.scene.objects)-railbefore if o.type=='MESH').data.copy()
 for o in set(bpy.context.scene.objects)-railbefore:bpy.data.objects.remove(o,do_unlink=True)
 def guard(name,p,q):
  run=math.hypot(q[0]-p[0],q[2]-p[2]);ux,uz=(q[0]-p[0])/run,(q[2]-p[2])/run;n=math.ceil(run/2)
  for i in range(n):
   mesh=rail.copy()
   for v in mesh.vertices:
    t=(v.co.x+1)*run/n/2+i*run/n;off=v.co.y;v.co=(p[0]+ux*t-uz*off,-(p[2]+uz*t+ux*off),p[1]+(q[1]-p[1])*t/run+v.co.z)
   o=bpy.data.objects.new(site+' restart '+name+' rail '+str(i),mesh);bpy.context.collection.objects.link(o);o['role']='indexed frame-mounted guard';o['source_asset']='level_blockout_guardrail';built.append(o)
 def flight(link,x,z0,z1,y0,y1,n,width=1.8):
  run=abs(z1-z0);assert (y1-y0)/n<=.181 and run/n>=.25
  # The reserved lane determines geometry; no route sweep or capsule extrusion.
  for i in range(n):
   za,zb=z0+(z1-z0)*i/n,z0+(z1-z0)*(i+1)/n
   poly=rect(x-width/2,min(za,zb),x+width/2,max(za,zb));bottom=[y0+(y1-y0)*(z-z0)/(z1-z0)-.20 for _,z in poly]
   o=prism(site+' restart '+link+' tread '+str(len(steps)),poly,y0+(y1-y0)*(i+1)/n,bottom,steel);o['role']='structural-stair-step';o['link']=link;built.append(o);steps.append(o.name)
  for off in [-width/2,width/2]:
   beam(link+' stringer '+str(len(built)),[x+off,y0-.21,z0],[x+off,y1-.21,z1],.14,.30);guard(link+' flight '+str(len(built)),[x+off,y0,z0],[x+off,y1,z1])
  flights.append(dict(link=link,low=[x,y0,z0],high=[x,y1,z1],width=width,risers=n))
 def wall_face(name,axis,fixed,start,end,lo,hi,apertures,thick=.24):
  cursor=start
  for j,(along,width,sill,head,kind) in enumerate(apertures):
   left,right=along-width/2,along+width/2
   assert cursor<=left<right<=end and lo<=sill<head<=hi
   pieces=[(cursor,left,lo,hi),(left,right,lo,sill),(left,right,head,hi)]
   for k,(a,b,l,h) in enumerate(pieces):
    if b-a>1e-5 and h-l>1e-5:
     foot=rect(a,fixed-thick/2,b,fixed+thick/2) if axis=='z' else rect(fixed-thick/2,a,fixed+thick/2,b)
     slab(name+' pier '+str(j)+' '+str(k),foot,h,h-l,'complete architectural wall',wallmat)
   if kind=='door':
    # Indexed jamb/head template, clear aperture dimensions independent of guidance.
    p=dict(id=site+'-restart-'+name,axis=axis,boundary=fixed,along=along,sill_y=sill,clear_width=width-.19,clear_height=head-sill-.15,frame_span=[fixed-thick/2,fixed+thick/2],frame_depth=thick)
    t=frame_template();t.name=p['id']+' indexed frame'
    for v in t.data.vertices:
     u=v.co.x*p['clear_width'];normal=v.co.y*thick/.18;y=sill+v.co.z*p['clear_height']/2.125
     v.co=(along-u,-fixed-normal,y) if axis=='z' else (fixed+normal,-along-u,y)
    t['role']='indexed architectural doorway';t['source_asset']='level_blockout_doorway';built.append(t);doors.append(p)
   cursor=right
  if end-cursor>1e-5:
   poly=rect(cursor,fixed-thick/2,end,fixed+thick/2) if axis=='z' else rect(fixed-thick/2,cursor,fixed+thick/2,end)
   slab(name+' end pier',poly,hi,hi-lo,'complete architectural wall',wallmat)
 by={l['id']:l for l in data['links']}
 if site=='WLD-04':
  # One tower frame, two repeated switchback storeys, and a separate level cab.
  plan=dict(frame_bounds=[24,2,36,16],base_y=6,landings=[8.25,10.5,12.75,15],lanes=[28.2,30.8],flight_z=[10,13.9],landing_z=[[8.2,10],[13.9,16]],cab=[24,2,30,8],deck=[30,2,36,8])
  for o in old:
   role=o.get('role','');zone=o.get('zone');bb=bounds(o)
   if role=='placed source-library furnishing':continue
   if zone in ['cab','deck','landing','upper-turn-platform']:removed.add(o.name)
   if o.get('link') in ['L13','L14','L14B']:removed.add(o.name)
   if o.name.startswith(('L12 support','L15 support')) or 'Header cab' in o.name:removed.add(o.name)
   if role.startswith('rough route support') and bb[0]<36 and bb[2]>24 and bb[1]<24 and bb[3]>16:removed.add(o.name)
   if 'guard' in role and bb[0]>23 and bb[2]<37 and bb[1]<17 and bb[4]>6:removed.add(o.name)
  terrain=next(o for o in old if o.get('role','').startswith('saved editor terrain'));tree=BVHTree.FromPolygons([terrain.matrix_world@v.co for v in terrain.data.vertices],[list(p.vertices) for p in terrain.data.polygons])
  def soil(x,z):
   h=tree.ray_cast(Vector((x,-z,30)),Vector((0,0,-1)),60)[0];assert h is not None;return h.z
  # Footings and base are embedded in the real saved terrain; it stays unchanged.
  heights=[soil(x,z) for x in [24,27,30,33,36] for z in [2,5,8,11,14,16]];assert max(heights)<5.98
  box('continuous grounded tower plinth',24,2,36,16,min(heights)-.45,6,'soil-embedded tower foundation')
  low=soil(30.8,16.8);flight('L13',30.8,16.8,16,low,6,2,2.0)
  for x in [24.2,29.8,35.8]:
   for z in [2.2,7.8]:box('cab frame column '+str((x,z)),x-.14,z-.14,x+.14,z+.14,5.1,14.8,mt=steel);receivers.append(dict(x=x,z=z,soil_y=soil(x,z),foundation_bottom=min(heights)-.45))
  for x in [24.2,27.05,32]:
   for z in [8.35,15.75]:box('stair frame column '+str((x,z)),x-.12,z-.12,x+.12,z+.12,5.1,14.8,mt=steel)
  for y in [8.25,10.5,12.75]:
   za,zb=(8.2,10) if y in [8.25,12.75] else (13.9,16)
   slab('complete turning landing '+str(y),rect(27.3,za,31.7,zb),y,.20,'frame-supported turning landing',steel)
   for z in [za+.15,zb-.15]:box('landing crossbeam '+str((y,z)),26.93,z-.10,32.12,z+.10,y-.45,y-.20,mt=steel)
   guard('landing exposed end '+str(y),[27.36,y,za+.06 if za<10 else zb-.06],[31.64,y,za+.06 if za<10 else zb-.06])
   for x in [27.36,31.64]:guard('landing outer cheek '+str((x,y)),[x,y,za+.06],[x,y,zb-.06])
  flight('L13',30.8,13.9,10,6,8.25,13);flight('L13',28.2,10,13.9,8.25,10.5,13)
  flight('L14',30.8,13.9,10,10.5,12.75,13);flight('L14B',28.2,10,13.9,12.75,15,13)
  # Top floor follows building areas and the landing; it never spans the other flight.
  for name,poly in [('cab and observation deck',rect(24,2,36,8)),('west level access gallery',rect(24,8,27.3,16)),('top stair arrival landing',rect(27.3,13.9,29.1,16))]:slab(name,poly,15,.20,'complete supported top floor',steel)
  for z in [2.2,7.8]:box('cab transverse girder '+str(z),24.06,z-.12,35.94,z+.12,14.45,14.8,mt=steel)
  for x in [24.2,27.05]:box('west gallery girder '+str(x),x-.12,7.8,x+.12,15.9,14.45,14.8,mt=steel)
  box('arrival landing girder',26.93,15.65,29.1,15.9,14.45,14.8,mt=steel)
  # Diagonal bracing lies on frame elevations outside stair and walking bays.
  for x in [24.2,32]:
   for za,zb in [(8.35,15.75)]:
    for lo,hi in [(6,10.2),(10.2,14.45)]:beam('stair frame diagonal', [x,lo,za],[x,hi,zb],.12,.16)
  for z in [2.2,7.8]:
   for xa,xb in [(24.2,29.8),(29.8,35.8)]:beam('cab frame diagonal',[xa,6,z],[xb,14.45,z],.14,.18)
  wall_face('cab north','z',2.1,24,30,15,18,[(27,4.4,16,17.4,'window')],.20)
  wall_face('cab south','z',7.9,24,30,15,18,[(25.5,1.44,15,17.25,'door')],.20)
  wall_face('cab west','x',24.1,2.2,7.8,15,18,[(5,4,16,17.4,'window')],.20)
  wall_face('cab east','x',29.9,2.2,7.8,15,18,[(5,1.44,15,17.25,'door')],.20)
  slab('cab closed roof',rect(23.8,1.8,30.2,8.2),18.2,.20,'coherent enclosed cab roof',wallmat)
  for p,q in [([30.1,15,2.1],[35.9,15,2.1]),([35.9,15,2.1],[35.9,15,7.9]),([30.1,15,7.9],[35.9,15,7.9]),([24.1,15,8.1],[24.1,15,15.9]),([24.1,15,15.9],[29,15,15.9]),([27.2,15,8.1],[27.2,15,13.84]),([29,15,13.96],[29,15,15.9])]:guard('top perimeter '+str(len(built)),p,q)
  # Movement is authored only after complete structural areas exist.
  by['L12']['points']=[[30,low,22],[30.8,low,18],[30.8,low,16.8]]
  by['L13']['points']=[[30.8,low,16.8],[30.8,6,16],[30.8,6,15],[30.8,6,13.9],[30.8,8.25,10],[30.8,8.25,9.1],[28.2,8.25,9.1],[28.2,8.25,10],[28.2,10.5,13.9],[28.2,10.5,15],[29.5,10.5,15]]
  by['L14']['points']=[[29.5,10.5,15],[30.8,10.5,15],[30.8,10.5,13.9],[30.8,12.75,10],[30.8,12.75,9.1],[28.2,12.75,9.1]]
  by['L14B']['points']=[[28.2,12.75,9.1],[28.2,12.75,10],[28.2,15,13.9],[28.2,15,15],[25.5,15,15],[25.5,15,9],[25.5,15,7],[27,15,5]]
  by['L15']['points']=[[27,15,5],[33,15,5]]
  mapping=copy.deepcopy(meta.get('source_segment_map',{}));mapping.update({'L13':{'0':2,'3':0,'7':1},'L14':{'2':2},'L14B':{'1':2}})
  added=[dict(link='L13',segment=2,layout_segment=0,points=by['L13']['points'][0:2]),dict(link='L13',segment=1,layout_segment=7,points=by['L13']['points'][7:9])]
  for z in data['zones']:
   if z['id']=='foot':z.update(bounds=[24,2,36,16],label_point=[30.8,6,15])
   if z['id']=='landing':z.update(bounds=[27.3,13.9,31.7,16],label_point=[29.5,10.5,15])
   if z['id']=='upper-turn-platform':z.update(bounds=[27.3,8.2,31.7,10],label_point=[28.2,12.75,9.1])
 else:
  plan=dict(workshop=[36,48,54,62],floor=-6,roof=-3,wall_depth=.24,door_center=[45,-6,48],door_clear=[1.61,2.10],spillway_and_emergency='OPEN_UNBUILT')
  for o in old:
   role=o.get('role','');name=o.name
   if (o.get('zone')=='workshop' and role in ['rough shell wall','ceiling-cap']) or 'Header workshop' in name or 'Wall workshop' in name or 'ceiling-cap workshop' in name:removed.add(name)
   if 'Wall turbine S' in name or 'Header turbine S' in name:removed.add(name)
  slab('workshop complete roof closes obsolete diagonal wound',rect(36,48,54,62),-3,.30,'complete workshop enclosure roof')
  wall_face('shared turbine workshop north','z',48,36,64,-6,2,[(45,1.8,-6,-3.75,'door')])
  wall_face('workshop south','z',61.88,36,54,-6,-3,[])
  wall_face('workshop west','x',36,48.12,61.76,-6,-3,[])
  wall_face('workshop east','x',54,48.12,61.76,-6,-3,[])
  by['L08']['points']=[[50,-6,35],[52,-6,35],[52,-6,43],[45,-6,43],[45,-6,48],[45,-6,55]]
  # Preserve the real stair ascent; its lower approach now follows the actual door.
  oldapproach=before['links'][next(i for i,l in enumerate(before['links']) if l['id']=='L08')]['points']
  original=by['L09']['points'];ascent=original[len(oldapproach):];by['L09']['points']=list(reversed(by['L08']['points']))+ascent
  original12=by['L12']['points'];junction=next(i for i,p in enumerate(original12) if i>7 and p==[45,-6,55]);by['L12']['points']=original12[:9]+by['L08']['points'][1:]+original12[junction+1:]
  mapping=copy.deepcopy(meta.get('source_segment_map',{}));delta=len(by['L08']['points'])-len(oldapproach);mapping['L09']={str(int(k)+delta):v for k,v in mapping['L09'].items()};added=[]
 # Preserve originals and quarantine by explicit names, without copying masters.
 quarantine=[dict(name=o.name,role=o.get('role'),zone=o.get('zone'),link=o.get('link'),bounds=bounds(o)) for o in old if o.name in removed]
 write(out/'quarantine.json',dict(source=row,reason='Owner rejected generated architecture; original source preserved unchanged',meshes=quarantine))
 for o in old:
  if o.name in removed:bpy.data.objects.remove(o,do_unlink=True)
 for o in built:
  bm=bmesh.new();bm.from_mesh(o.data);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=1e-6);bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces));assert all(e.is_manifold for e in bm.edges) and bm.calc_volume(signed=True)>0,o.name;bm.to_mesh(o.data);bm.free()
 data['authored_doorways']=data.get('authored_doorways',[])+doors;assert sum(z.get('player_access')!='SEALED_CONTEXT' for z in data['zones'])<=40
 layout=out/'authored-layout.json';write(layout,data);bpy.ops.wm.save_as_mainfile(filepath=str(out/'reconstructed.blend'),compress=True)
 bpy.ops.object.select_all(action='DESELECT')
 for o in built:o.select_set(True)
 addition=ROOT/'build/restart-addition.glb';bpy.ops.export_scene.gltf(filepath=str(addition),export_format='GLB',export_extras=True,use_selection=True)
 append(ROOT/row['glb'],addition,out/'reconstructed.glb',remove_mesh_names=sorted(removed));native_write(ROOT/row['native'],out/'reconstructed.hcs2',removed,built)
 nativeold=split_native(ROOT/row['native']);nativenew=split_native(out/'reconstructed.hcs2');kept=[p for p in nativeold if p[0].split(':',1)[-1] not in removed];assert nativenew[:len(kept)]==kept
 meta.update(status='BOUNDED_ARCHITECTURAL_RESTART_RUNTIME_OPEN',source_glb=row['glb'],source_glb_sha256=sha(ROOT/row['glb']),source_physics=row['native'],source_physics_sha256=sha(ROOT/row['native']),before_layout=row['layout'],before_layout_sha256=sha(ROOT/row['layout']),before_segment_map=meta.get('source_segment_map',{}),source_segment_map=mapping,before_stair_components=meta['stair_components'],stair_components=[n for n in meta['stair_components'] if n.split(':',1)[-1] not in removed]+steps,interface_added_flights=added,layout=layout.relative_to(ROOT).as_posix(),layout_sha256=sha(layout),glb_sha256=sha(out/'reconstructed.glb'),physics_sha256=sha(out/'reconstructed.hcs2'),removed_source_meshes=sorted(removed),new_interface_parts=[o.name for o in built],doorways=meta.get('doorways',[])+doors,architectural_plan=plan,foundation_receivers=receivers,flights=flights,builder='tools/build_architectural_restart.py',builder_sha256=sha(Path(__file__)),blend_sha256=sha(out/'reconstructed.blend'))
 write(out/'construction.json',meta)
 print('RECONSTRUCTED',site,'removed',len(removed),'built',len(built),flush=True)
 return dict(id=site,construction=(out/'construction.json').relative_to(ROOT).as_posix(),layout=layout.relative_to(ROOT).as_posix(),glb=(out/'reconstructed.glb').relative_to(ROOT).as_posix(),native=(out/'reconstructed.hcs2').relative_to(ROOT).as_posix(),blend=(out/'reconstructed.blend').relative_to(ROOT).as_posix(),glb_sha256=meta['glb_sha256'],native_sha256=meta['physics_sha256'])
def main():
 roster=json.loads(BASE.read_text());new={}
 for site in ['WLD-04','WLD-09']:new[site]=build(next(v for v in roster['active_sources'] if v['id']==site))
 roster['active_sources']=[new.get(v['id'],v) for v in roster['active_sources']];roster['baseline']=BASE.relative_to(ROOT).as_posix();write(O/'roster.json',roster)
if __name__=='__main__':main()
