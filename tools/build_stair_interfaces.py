"""Construct Academy landings, Dam service platforms and lookout flights.
Retains exact unrelated source/native records and actual saved terrain owners.
"""
from pathlib import Path
import sys,json,math,copy
import bpy,bmesh
from mathutils import Vector
from mathutils.bvhtree import BVHTree
ROOT=Path(__file__).resolve().parents[1];sys.path[:0]=[str(ROOT/'tools'),str(ROOT/'development/levels/blockouts')]
from planar_structure import plate
from build_roster_floor_closures import difference
from build_roster_foundations import material,prism
from build_circulation_construction import native_write
from append_static_glb import append
from review_roster_macroterrain import records,sha
from export_roster_collision import face_keys
from compose_room_terrain import split_native
from build_deliberate_entrances import write
from build_architectural_doorways import frame_template
BASE=ROOT/'development/levels/rebuild';OUT=BASE/'stair-interface-construction-pass';BASELINE=BASE/'terrain-ownership-construction-pass/roster.json'
RAIL=ROOT/'development/levels/primitive-props-v1/models/level_blockout_guardrail.glb'
def rect(a,b,c,d):return [(a,b),(c,b),(c,d),(a,d)]
def build(s):
 site=s['id'];folder=OUT/site.lower();folder.mkdir(parents=True,exist_ok=True);meta=json.loads((ROOT/s['construction']).read_text());data=json.loads((ROOT/s['layout']).read_text());old_data=copy.deepcopy(data)
 bpy.ops.wm.read_factory_settings(use_empty=True);bpy.context.preferences.filepaths.save_version=0;bpy.ops.import_scene.gltf(filepath=str(ROOT/s['glb']));original=[o for o in bpy.context.scene.objects if o.type=='MESH'];removed=set();built=[];newstairs=[];proof=[]
 chosen={'ACADEMY':{'L66','L67'},'WLD-09':{'L07','L14'},'WLD-04':{'L14','L14B'}}[site]
 mat=next(o.data.materials[0] for o in original if o.get('link') in chosen and o.get('role')=='structural-stair-step');concrete=material(site+' complete structural landing concrete',(.39,.39,.37));steel=material(site+' founded service platform steel',(.23,.25,.26))
 for o in original:
  if o.get('link') in chosen and any(t in o.get('role','') for t in ['step','stringer','guard']):removed.add(o.name)
  if site=='ACADEMY' and o.get('role')=='floor' and o.get('zone') in ['upper-main-stair','upper-east-stair','east-stair']:removed.add(o.name)
  if site=='ACADEMY' and any(k in o.name for k in ['Wall east-stair E','Header east-stair E']):removed.add(o.name)
  if site=='WLD-09' and o.get('role')=='floor' and o.get('zone')=='west-stair':removed.add(o.name)
 def slab(name,polys,y,depth,role,zone=None,matl=None):
  made=plate(site+' '+name,polys,y,depth,matl or concrete) or []
  for o in made:
   o['role']=role
   if zone:o['zone']=zone
   built.append(o)
  return made
 def floor(name,foot,y,depth,holes=(),zone=None):
  polys=[foot]
  for hole in holes:polys=[q for p in polys for q in difference(p,hole)]
  return slab(name,polys,y,depth,'constructed full landing with rectangular stair opening',zone)
 # Actual closed waist: stepped upper profile, continuously inclined underside.
 # No deep stacked columns occupying the whole volume below a flight.
 def flight(link,low,high,width,n,label):
  dx,dz=high[0]-low[0],high[2]-low[2];run=math.hypot(dx,dz);rise=high[1]-low[1];assert rise>0 and rise/n<=.181 and run/n>=.24
  ux,uz=dx/run,dz/run;profile=[(0,low[1]-.40),(run,high[1]-.40),(run,high[1]-.20),(0,low[1]-.20)]
  vs=[]
  for off in [-width/2,width/2]:
   for t,y in profile:vs.append((low[0]+ux*t-uz*off,-(low[2]+uz*t+ux*off),y))
  count=len(profile);faces=[tuple(range(count-1,-1,-1)),tuple(range(count,count*2))]+[(i,(i+1)%count,(i+1)%count+count,i+count) for i in range(count)]
  mesh=bpy.data.meshes.new(site+' continuous stepped waist');mesh.from_pydata(vs,[],faces);mesh.update();o=bpy.data.objects.new(site+' '+link+' '+label+' continuous stair waist',mesh);bpy.context.collection.objects.link(o);o.data.materials.append(mat);o['role']='continuous load-bearing stair waist';o['link']=link;built.append(o)
  uv=mesh.uv_layers.new(name='Metre-scaled structural face UV')
  for f in mesh.polygons:
   axis=max(range(3),key=lambda k:abs(f.normal[k]));axes=[k for k in range(3) if k!=axis]
   for i in f.loop_indices:p=mesh.vertices[mesh.loops[i].vertex_index].co;uv.data[i].uv=(p[axes[0]],p[axes[1]])
  for i in range(n):
   a,b=i*run/n,(i+1)*run/n;foot=[(low[0]+ux*t-uz*off,low[2]+uz*t+ux*off) for t,off in [(a,-width/2),(b,-width/2),(b,width/2),(a,width/2)]];bottom=[low[1]+rise*t/run-.20 for t in [a,b,b,a]]
   step=prism(site+' '+link+' '+label+' tread'+str(i+1),foot,low[1]+(i+1)*rise/n,bottom,mat);step['role']='structural-stair-step';step['link']=link;built.append(step);newstairs.append(step.name)
  proof.append(dict(link=link,name=o.name,low=low,high=high,gross_width=width,risers=n,riser=rise/n,tread=run/n,waist_depth=.20))
  return (low,high,width)
 flights=[]
 if site=='ACADEMY':
  for link,x,z0,z1 in [('L66',16,44,49.76),('L67',54,47,52.76)]:flights.append(flight(link,[x,0,z0],[x,3.6,z1],1.8,20,'single flight'))
  floor('main upper complete landing',rect(7,42,19,53),3.6,.30,[rect(15.1,43.75,16.9,49.76)],'upper-main-stair')
  floor('east upper complete landing',rect(45,45,57,55),3.6,.30,[rect(53.1,46.75,54.9,52.76)],'upper-east-stair')
  floor('east lower complete room floor',rect(45,45,57,55),0,.30,zone='east-stair')
  next(l for l in data['links'] if l['id']=='L42')['points']=[[51,0,50],[51,0,46],[56,0,46],[56,0,50],[57,0,50],[59,0,50],[59,0,57],[27,0,57],[27,0,49],[27,0,43.5],[32,0,43.5],[32,0,32],[32.5,0,32],[32.5,0,27.5]]
  # A complete flush doorway on the actual east supporting wall.
  for start,end,lo,hi,label in [(45,48.884,0,3.6,'north pier'),(51.116,55,0,3.6,'south pier'),(48.884,51.116,2.3*2.275/2.125,3.6,'lintel')]:slab('east stair doorway '+label,[rect(56.76,start,57,end)],hi,hi-lo,'constructed full-depth stair exit wall','east-stair')
  frame=frame_template();frame.name='ACADEMY east stair fitted indexed doorway'
  for v in frame.data.vertices:
   q=frame.matrix_world@v.co;v.co=(56.88+q.y*.24/.18,-50-q.x*1.8,q.z*2.3/2.125)
  frame.matrix_world.identity();frame['role']='fitted indexed doorway jambs and head';frame['source_asset']='level_blockout_doorway';built.append(frame)
  floor('east stair external exit porch',rect(57,48.5,59,51.5),0,.30)
  portal=dict(id='ACADEMY-east-stair-east-50',axis='x',boundary=57,along=50,clear_width=1.8,clear_height=2.3,sill_y=0,frame_span=[56.76,57],frame_center=[56.88,0,50],frame_depth=.24,rooms=['east-stair'],door_type='service')
  data['authored_doorways']=data.get('authored_doorways',[])+[portal];meta['doorways']=meta.get('doorways',[])+[portal]
 elif site=='WLD-09':
  flights.append(flight('L07',[48.5,-6,35],[42.15,-3,35],1.8,17,'lower flight'))
  flights.append(flight('L07',[40.35,-3,35],[32.5,0,35],1.8,17,'upper flight'))
  floor('west stair real intermediate landing',rect(40.35,34,42.15,36),-3,.32)
  receivers=[o for o in original if o.get('role')=='floor' and o.name not in removed];vs=[];fs=[]
  for obj in receivers:
   offset=len(vs);vs.extend(obj.matrix_world@v.co for v in obj.data.vertices);fs.extend(tuple(offset+i for i in f.vertices) for f in obj.data.polygons)
  floor_tree=BVHTree.FromPolygons(vs,fs)
  for z in [34.25,35.75]:
   x=41.25;hits=[floor_tree.ray_cast(Vector((x+dx,-z-dz,-3.33)),Vector((0,0,-1)),8)[0] for dx,dz in [(-.15,-.15),(.15,-.15),(.15,.15),(-.15,.15)]];assert all(h is not None and abs(h.z+6)<.02 for h in hits),'Intermediate landing pier lacks actual turbine floor receiver'
   slab('mid-landing pier on actual turbine floor '+str(z),[rect(x-.15,z-.15,x+.15,z+.15)],-3.32,2.78,'stair mid-landing floor-founded pier');proof.append(dict(pier=[x,z],receiver_y=[h.z for h in hits],bottom_y=-6.1,top_y=-3.32))
  floor('west complete stair-head floor',rect(26,22,36,48),0,.30,[rect(32.5,34.1,36,35.9)],'west-stair')
  flights.append(flight('L14',[56,-10,23.5],[56,-6,30.5],1.8,23,'open-under service flight'))
  next(l for l in data['links'] if l['id']=='L07')['points']=[[31,0,35],[32.5,0,35],[40.35,-3,35],[42.15,-3,35],[48.5,-6,35],[50,-6,35]]
  next(l for l in data['links'] if l['id']=='L17')['points']=[[57,-10,40],[57,-10,32],[57,-10,29],[50,-10,29],[50,-10,27]]
  # Area platform and abutment returns, with original top landing subtracted.
  floor('east stair side service platform',[*rect(69.2,36.25,73.2,46)],0,.30)
  floor('east crest approach landing',rect(66.8,46,73.2,48),0,.30)
  floor('east crest outside edge return',rect(72,48,73.2,48.2),0,.30)
  floor('east stair-head lateral landing',rect(71.25,33.75,73.2,36.25),0,.30)
  next(l for l in data['links'] if l['id']=='L10')['points']=[[70,0,35],[72,0,35],[72,0,47],[68,0,47],[68,0,52],[40,0,52]]
  terrain=next(o for o in original if o.get('role','').startswith('saved editor terrain'));tree=BVHTree.FromPolygons([terrain.matrix_world@v.co for v in terrain.data.vertices],[tuple(f.vertices) for f in terrain.data.polygons])
  for x,z in [(72.5,38),(72.5,41.6),(72.5,46.5)]:
   hits=[tree.ray_cast(Vector((x+dx,-z-dz,30)),Vector((0,0,-1)),100)[0] for dx,dz in [(-.15,-.15),(.15,-.15),(.15,.15),(-.15,.15)]];assert all(h is not None for h in hits),(site,'Platform receiver missing',x,z)
   bottom=min(min(h.z for h in hits)-.35,-.7);slab('east platform soil-founded pier '+str(x)+' '+str(z),[rect(x-.15,z-.15,x+.15,z+.15)],-.30,-.30-bottom,'native-soil founded landing pier',matl=steel);proof.append(dict(pier=[x,z],soil_y=[h.z for h in hits],bottom_y=bottom))
  slab('east longitudinal platform girder',[rect(72.35,36.25,72.65,47.5)],-.30,.40,'landing structural girder',matl=steel)
  slab('east north transverse platform girder',[rect(69.2,37.85,72.65,38.15)],-.30,.40,'landing structural girder',matl=steel)
  assert any(o.get('role')=='constructed dam crest deck' for o in original),'Missing actual crest bearing'
  slab('west return girder bearing into existing crest slab',[rect(67.15,46,67.45,48.4)],-.30,.40,'girder bearing into retained crest slab',matl=steel)
  proof.append(dict(bearing='Original constructed dam crest deck',contact_bounds=[67.15,48,67.45,48.4],embed_y=[-.6,-.3]))
  slab('east south transverse platform girder',[rect(66.8,47.05,72.65,47.35)],-.30,.40,'landing structural girder',matl=steel)
 else:
  flights.append(flight('L14',[30.8,10.5,4.2],[30.8,12.75,8.1],1.8,13,'lookout upper first flight'))
  flights.append(flight('L14B',[28.2,12.75,8.1],[28.2,15,4.2],1.8,13,'lookout upper second flight'))
 # Fit the actual indexed guard to each new flight; embedded post feet touch
 # the stepped waist. Width leaves the original declared walking clearance.
 old=set(bpy.context.scene.objects);bpy.ops.import_scene.gltf(filepath=str(RAIL));template=next(o for o in set(bpy.context.scene.objects)-old if o.type=='MESH').data.copy()
 for o in set(bpy.context.scene.objects)-old:bpy.data.objects.remove(o,do_unlink=True)
 guard_runs=[(low,high,width/2,side) for low,high,width in flights for side in [-1,1]]
 rims=[]
 if site=='ACADEMY':
  for a,b,c,d in [(15,43.65,17,49.76),(53,46.65,55,52.76)]:rims.extend([([a,3.6,b],[a,3.6,d]),([c,3.6,b],[c,3.6,d]),([a,3.6,b],[c,3.6,b])])
 if site=='WLD-09':
  rims=[([32.5,0,34],[36,0,34]),([32.5,0,36],[36,0,36]),([40.35,-3,34.1],[42.15,-3,34.1]),([40.35,-3,35.9],[42.15,-3,35.9]),([69.3,0,36.25],[69.3,0,46]),([73.1,0,33.85],[73.1,0,47.9]),([71.25,0,33.85],[73.1,0,33.85]),([69.2,0,48.1],[73.1,0,48.1])]
 guard_runs.extend((p,q,0,1) for p,q in rims)
 for low,high,half,side in guard_runs:
  dx,dz=high[0]-low[0],high[2]-low[2];run=math.hypot(dx,dz);ux,uz=dx/run,dz/run;count=math.ceil(run/2)
  for i in range(count):
   o=bpy.data.objects.new(site+' mounted stair guard '+str(len(built)),template.copy());bpy.context.collection.objects.link(o)
   for v in o.data.vertices:
    t=(v.co.x+1)*run/count/2+i*run/count;off=side*(half+v.co.y);v.co=(low[0]+ux*t-uz*off,-(low[2]+uz*t+ux*off),low[1]+(high[1]-low[1])*t/run+v.co.z)
   o['role']='mounted indexed stair edge guard' if half else 'mounted indexed landing or aperture rim guard';o['source_asset']='level_blockout_guardrail';built.append(o)
 for o in original:
  if o.name in removed:bpy.data.objects.remove(o,do_unlink=True)
 for o in built:
  bm=bmesh.new();bm.from_mesh(o.data);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=.000001);bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces));bmesh.ops.triangulate(bm,faces=list(bm.faces));assert all(e.is_manifold for e in bm.edges) and bm.calc_volume(signed=True)>0,(site,o.name,'Not closed positive');bm.to_mesh(o.data);bm.free()
 mapping=copy.deepcopy(meta.get('source_segment_map',{}))
 if site=='WLD-09':mapping['L07']={'1':0,'3':1}
 layout=folder/'authored-layout.json';write(layout,data);bpy.ops.wm.save_as_mainfile(filepath=str(folder/'joined-stairs.blend'),compress=True);bpy.ops.object.select_all(action='DESELECT')
 for o in built:o.select_set(True)
 addition=ROOT/'build'/('stair-interfaces-'+site+'.glb');bpy.ops.export_scene.gltf(filepath=str(addition),export_format='GLB',export_extras=True,use_selection=True);append(ROOT/s['glb'],addition,folder/'joined-stairs.glb',remove_mesh_names=sorted(removed));parts=native_write(ROOT/s['native'],folder/'joined-stairs.hcs2',removed,built)
 before_steps=meta['stair_components'];meta.update(geometry_unchanged=False,status='PHYSICAL_STAIR_INTERFACES_CONSTRUCTED_NATIVE_VERIFICATION_PENDING',source_contract=s['construction'],source_glb=s['glb'],source_glb_sha256=s['glb_sha256'],source_physics=s['native'],source_physics_sha256=s['native_sha256'],before_layout=s['layout'],before_layout_sha256=sha(ROOT/s['layout']),before_segment_map=copy.deepcopy(meta.get('source_segment_map',{})),source_segment_map=mapping,before_stair_components=before_steps,stair_components=[n for n in before_steps if n.split(':',1)[-1] not in removed]+newstairs,removed_source_meshes=sorted(removed),removed_native_parts=[n for n,_,_ in split_native(ROOT/s['native']) if n.split(':',1)[-1] in removed],new_interface_parts=[o.name for o in built],new_parts=len(built),layout=layout.relative_to(ROOT).as_posix(),layout_sha256=sha(layout),glb_sha256=sha(folder/'joined-stairs.glb'),physics_sha256=sha(folder/'joined-stairs.hcs2'),blend_sha256=sha(folder/'joined-stairs.blend'),stair_interfaces=proof,interface_added_flights=([dict(link='L07',segment=1,layout_segment=3,points=[[42.15,-3,35],[48.5,-6,35]])] if site=='WLD-09' else []),interface_builder=Path(__file__).relative_to(ROOT).as_posix(),interface_builder_sha256=sha(Path(__file__)),interface_library=dict(path=RAIL.relative_to(ROOT).as_posix(),sha256=sha(RAIL)))
 write(folder/'construction.json',meta);print('CONSTRUCTED_STAIR_INTERFACES',site,len(built),'new parts',len(removed),'replaced',flush=True)
 return dict(id=site,construction=(folder/'construction.json').relative_to(ROOT).as_posix(),layout=layout.relative_to(ROOT).as_posix(),glb=(folder/'joined-stairs.glb').relative_to(ROOT).as_posix(),native=(folder/'joined-stairs.hcs2').relative_to(ROOT).as_posix(),blend=(folder/'joined-stairs.blend').relative_to(ROOT).as_posix(),glb_sha256=meta['glb_sha256'],native_sha256=meta['physics_sha256'])
def main():
 roster=json.loads(BASELINE.read_text());targets={'ACADEMY','WLD-09','WLD-04'};active=[build(s) if s['id'] in targets else s for s in roster['active_sources']]
 for row in roster['sites']:
  if row['id'] in targets:row['decision']='PHYSICAL_STAIR_LANDING_INTERFACES_CONSTRUCTED_VERIFICATION_PENDING'
 write(OUT/'roster.json',dict(active_sources=active,sites=roster['sites'],baseline=BASELINE.relative_to(ROOT).as_posix()))
if __name__=='__main__':main()
