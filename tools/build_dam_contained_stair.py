"""A bounded architectural replacement for the Dam room-crossing stair masses."""
from pathlib import Path
import sys,json,math,struct,copy
import bpy,bmesh
from mathutils import Vector
from mathutils.bvhtree import BVHTree
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
from planar_structure import plate
from build_roster_floor_closures import difference
from build_roster_foundations import material,prism
from build_architectural_doorways import frame_template
from build_circulation_construction import native_write
from compose_room_terrain import split_native
from append_static_glb import append
from review_roster_macroterrain import records,sha
from export_roster_collision import face_keys,native_faces
from build_deliberate_entrances import write
B=ROOT/'development/levels/rebuild';O=B/'dam-contained-stair-pass';RAIL=ROOT/'development/levels/primitive-props-v1/models/level_blockout_guardrail.glb'
def rect(a,b,c,d):return [(a,b),(c,b),(c,d),(a,d)]
def bb(o):
 p=[o.matrix_world@v.co for v in o.data.vertices];return min(v.x for v in p),min(-v.y for v in p),max(v.x for v in p),max(-v.y for v in p),min(v.z for v in p),max(v.z for v in p)
def main():
 row=json.loads((O/'program.json').read_text());s=row['source'];meta=json.loads((ROOT/s['construction']).read_text());data=json.loads((ROOT/s['layout']).read_text());world=ROOT/row['world'];sourceworld=ROOT/row['source_world'];assert all(sha(sourceworld/p)==h for p,h in row['source_world_files_sha256'].items());folder=O/'wld-09';folder.mkdir(exist_ok=True)
 bpy.ops.wm.read_factory_settings(use_empty=True);bpy.context.preferences.filepaths.save_version=0;bpy.ops.import_scene.gltf(filepath=str(ROOT/s['glb']));old=[o for o in bpy.context.scene.objects if o.type=='MESH'];removed=set();built=[];steps=[];portals={};mat=material('Dam contained stair reinforced concrete',(.39,.40,.38));steel=material('Dam contained stair structural steel',(.25,.26,.27))
 terrain=next(o for o in old if o.get('role','').startswith('saved editor terrain'));assert face_keys(records(terrain))==face_keys(native_faces(sourceworld/'terrain-physics.hcs2'));removed.add(terrain.name)
 for o in old:
  role=o.get('role','');a,b,c,d,lo,hi=bb(o)
  if o.get('link') in ['L07','L09','L11','L12','L18'] and any(k in role for k in ['stair','landing','guard','support','stringer','waist']):removed.add(o.name)
  if any(k in o.name for k in ['Wall turbine E','Header turbine E','Wall turbine W','Header turbine W','Wall office E','Header office E','Wall return E','Header return E']):removed.add(o.name)
  if o.get('zone')=='return' and role in ['floor','ceiling-cap']:removed.add(o.name)
  if o.get('zone')=='west-stair' and ('wall' in role or role in ['floor','ceiling-cap','constructed full landing with rectangular stair opening']):removed.add(o.name)
  if o.get('zone')=='turbine' and role=='ceiling-cap':removed.add(o.name)
  if o.get('zone')=='workshop' and role=='floor':removed.add(o.name)
  if o.get('zone')=='east-stair' and role in ['floor','ceiling-cap']:removed.add(o.name)
  if a>=65 and c>=69 and b<49 and d>32 and abs(hi)<1e-4 and role=='constructed full landing with rectangular stair opening':removed.add(o.name)
  if a>=65 and b<49 and d>32 and role=='mounted indexed landing or aperture rim guard':removed.add(o.name)
  if a>=26 and c<=50 and b>=33 and d<=37 and role in ['mounted indexed stair edge guard','mounted indexed landing or aperture rim guard','constructed full landing with rectangular stair opening','stair mid-landing floor-founded pier']:removed.add(o.name)
 def slab(name,polys,y,depth,role,zone=None,mt=None):
  out=plate('WLD-09 '+name,polys,y,depth,mt or mat) or []
  for o in out:
   o['role']=role
   if zone:o['zone']=zone
   built.append(o)
  return out
 def floor(name,foot,y,depth,cuts=(),zone=None):
  polys=[foot]
  for cut in cuts:polys=[q for p in polys for q in difference(p,cut)]
  return slab(name,polys,y,depth,'contained stair hall floor or roof',zone)
 def box(name,a,b,c,d,lo,hi,role='contained stair structural bearing',mt=None):return slab(name,[rect(a,b,c,d)],hi,hi-lo,role,mt=mt)
 well=rect(65.9,25,68.1,45)
 floor('east hall lower full floor',rect(64,22,70.5,48),-6,.30,zone='east-lower-stair')
 floor('east hall upper floor and rectangular well',rect(64,22,70.5,48),0,.30,[well],'east-stair')
 floor('east hall enclosed roof',rect(64,22,70.5,48),3,.30,zone='east-stair')
 floor('turbine enclosed roof',rect(36,22,64,48),2,.30,zone='turbine')
 floor('retained external service catwalk',rect(70.5,33.75,73.2,48.2),0,.30,zone='east-service-catwalk')
 floor('restored workshop full floor',rect(36,48,54,62),-6,.30,zone='workshop')
 floor('restored return full floor',rect(50,32,64,48),-10,.30,zone='return')
 floor('closed old return escape roof',rect(50,32,64,48),-7,.30,zone='return')
 floor('west hall lower full floor',rect(26,22,36,48),-6,.30,zone='west-lower-stair')
 floor('west hall upper floor and rectangular well',rect(26,22,36,48),0,.30,[rect(29.9,25,32.1,45)],'west-stair')
 floor('west hall enclosed roof',rect(26,22,36,48),3,.30,zone='west-stair')
 # Wall schedules refer to complete physical faces and real floor datums.
 schedules=[('hall',rect(64,22,70.5,48),-6,3,{'W':[(35,-6,1.8,2.3)],'S':[(68,0,1.8,2.3)],'E':[(47,0,1.05,2.1)]}),('turbine-east',rect(36,22,64,48),-6,2,{'E':[(35,-6,1.8,2.3)]}),('return-east',rect(50,32,64,48),-10,-7,{'E':[]})]
 schedules += [('west-hall',rect(26,22,36,48),-6,3,{'W':[(28,0,1.5,2.3)],'S':[(31,0,2,2.3)],'E':[(35,-6,1.8,2.3)]}),('turbine-west',rect(36,22,64,48),-6,2,{'W':[(35,-6,1.8,2.3)]}),('office-east',rect(8,22,26,34),0,3,{'E':[(28,0,1.5,2.3)]})]
 for zone,poly,lo,hi,doors in schedules:
  a,b=poly[0];c,d=poly[2]
  for side,start,end,fixed in [('N',a,c,b),('S',a,c,d),('W',b,d,a),('E',b,d,c)]:
   if zone not in ['hall','west-hall'] and side not in doors:continue
   def wall(u,v,y0,y1,label):
    if v-u<1e-6 or y1-y0<1e-6:return
    foot=rect(u,fixed,v,fixed+.24) if side=='N' else rect(u,fixed-.24,v,fixed) if side=='S' else rect(fixed,u,fixed+.24,v) if side=='W' else rect(fixed-.24,u,fixed,v)
    slab(zone+' '+side+' '+label,[foot],y1,y1-y0,'contained complete stair wall',zone)
   cursor=start
   for j,(along,sill,width,height) in enumerate(doors.get(side,[])):
    left,right=along-.62*width,along+.62*width;head=sill+height*2.275/2.125;assert start<left<right<end and head<hi
    wall(cursor,left,lo,hi,'pier'+str(j));wall(left,right,lo,sill,'sill'+str(j));wall(left,right,head,hi,'header'+str(j));cursor=right
    axis='z' if side in ['N','S'] else 'x';key=(axis,fixed,along,sill);span=[fixed,fixed+.24] if side in ['N','W'] else [fixed-.24,fixed]
    if key in portals:portals[key]['frame_span']=[min(portals[key]['frame_span'][0],span[0]),max(portals[key]['frame_span'][1],span[1])]
    else:portals[key]=dict(id='DAM-HALL-'+axis+'-'+str(fixed)+'-'+str(along)+'-'+str(sill),axis=axis,boundary=fixed,along=along,sill_y=sill,clear_width=width,clear_height=height,frame_span=span,side=side)
   wall(cursor,end,lo,hi,'end')
 template=frame_template()
 for p in portals.values():
  o=template.copy();o.data=template.data.copy();o.name=p['id']+' indexed frame';bpy.context.collection.objects.link(o);mid=sum(p['frame_span'])/2;depth=p['frame_span'][1]-p['frame_span'][0]
  for v in o.data.vertices:
   u=v.co.x*p['clear_width'];n=v.co.y*depth/.18;y=p['sill_y']+v.co.z*p['clear_height']/2.125;v.co=(p['along']-u,-mid-n,y) if p['axis']=='z' else (mid+n,-p['along']-u,y)
  o['role']='indexed contained hall doorway';o['source_asset']='level_blockout_doorway';o['portal_id']=p['id'];built.append(o);p['frame_depth']=depth
 bpy.data.objects.remove(template,do_unlink=True)
 flight_start=len(built)
 # Two axial flights and a real intermediate landing, entirely inside the stair room. The underside remains open above useful headroom.
 def extrude_profile(name,profile,x0,x1,role):
  vs=[(x,-z,y) for x in [x0,x1] for z,y in profile];n=len(profile);fs=[tuple(range(n-1,-1,-1)),tuple(range(n,2*n))]+[(i,(i+1)%n,(i+1)%n+n,i+n) for i in range(n)];mesh=bpy.data.meshes.new(name);mesh.from_pydata(vs,[],fs);mesh.materials.append(mat);mesh.update();o=bpy.data.objects.new('WLD-09 '+name,mesh);bpy.context.collection.objects.link(o);o['role']=role;built.append(o);return o
 for flight,z0,z1,y0,y1 in [(0,25,34.1,-6,-3),(1,35.9,45,-3,0)]:
  extrude_profile('east hall flight'+str(flight)+' inclined waist',[(z0,y0-.4),(z1,y1-.4),(z1,y1-.2),(z0,y0-.2)],66.1,67.9,'continuous contained stair waist')
  for i in range(17):
   za,zb=z0+(z1-z0)*i/17,z0+(z1-z0)*(i+1)/17;top=y0+(y1-y0)*(i+1)/17;bottom=[y0+(y1-y0)*(z-z0)/(z1-z0)-.2 for z in [za,za,zb,zb]];o=prism('WLD-09 contained main tread '+str(flight*17+i),rect(66.1,za,67.9,zb),top,bottom,mat);o['role']='structural-stair-step';o['link']='L09';built.append(o);steps.append(o.name)
 extrude_profile('closed low-head under-stair plinth',[(25,-6.5),(32.75,-6.5),(32.75,-6+3*(32.75-25)/9.1-.4),(25,-6.4)],66.1,67.9,'closed low-head under-stair infill')
 floor('east hall real intermediate landing',rect(66.1,34.1,67.9,35.9),-3,.30)
 for x in [65.78,68.22]:
  box('middle landing side bearing '+str(x),x-.12,34.1,x+.12,35.9,-3.6,-3.3)
  for z in [34.25,35.75]:box('middle landing column '+str(x)+' '+str(z),x-.12,z-.12,x+.12,z+.12,-6.3,-3.3)
 box('middle landing transverse bearing',65.78,34.85,68.22,35.15,-3.6,-3.3)
 for x in [65.78,68.22]:
  box('upper floor rim girder '+str(x),x-.12,22,x+.12,48,-.7,-.3)
  for z in [31.5,38.5]:box('rim column '+str(x)+' '+str(z),x-.12,z-.15,x+.12,z+.15,-6.3,-.3)
 for z in [31.5,38.5]:
  y=(-6+3*(z-25)/9.1-.4) if z<34.1 else (-3+3*(z-35.9)/9.1-.4);box('waist bearing '+str(z),65.78,z-.15,68.22,z+.15,y-.3,y)
 for z in [34,36,44,47]:box('catwalk cantilever '+str(z),70.26,z-.15,73.2,z+.15,-.7,-.3,mt=steel)
 oldset=set(bpy.context.scene.objects);bpy.ops.import_scene.gltf(filepath=str(RAIL));rail=next(o for o in set(bpy.context.scene.objects)-oldset if o.type=='MESH').data.copy()
 for o in set(bpy.context.scene.objects)-oldset:bpy.data.objects.remove(o,do_unlink=True)
 def guard(name,p,q):
  run=math.hypot(q[0]-p[0],q[2]-p[2]);ux,uz=(q[0]-p[0])/run,(q[2]-p[2])/run;n=math.ceil(run/2)
  for i in range(n):
   mesh=rail.copy()
   for v in mesh.vertices:
    t=(v.co.x+1)*run/n/2+i*run/n;off=v.co.y;v.co=(p[0]+ux*t-uz*off,-(p[2]+uz*t+ux*off),p[1]+(q[1]-p[1])*t/run+v.co.z)
   o=bpy.data.objects.new('WLD-09 contained '+name+' guard '+str(i),mesh);bpy.context.collection.objects.link(o);o['role']='indexed supported contained hall guard';o['source_asset']='level_blockout_guardrail';built.append(o)
 for x in [66.17,67.83]:
  guard('lower flight '+str(x),(x,-6,25),(x,-3,34.1));guard('middle landing '+str(x),(x,-3,34.1),(x,-3,35.9));guard('upper flight '+str(x),(x,-3,35.9),(x,0,45))
 # Enclose the upper well with supported partitions instead of redundant railing rings.
 for x in [65.7,68.1]:box('east well enclosed side '+str(x),x,24.8,x+.2,45,0,2.7,'supported upper stairwell partition')
 box('east well closed north end',65.7,24.8,68.3,25,0,2.7,'supported upper stairwell partition')
 for original in list(built[flight_start:]):
  if 'catwalk' in original.name:continue
  o=original.copy();o.data=original.data.copy();o.name=original.name.replace('east hall','west hall').replace('east well','west well').replace('contained main tread','west contained main tread')+' west';bpy.context.collection.objects.link(o)
  for v in o.data.vertices:v.co.x-=36
  if o.get('link')=='L09':o['link']='L07';steps.append(o.name)
  built.append(o)
 for name,p,q in [('catwalk outside',(73.06,0,33.89),(73.06,0,48.06)),('catwalk north',(70.64,0,33.89),(73.06,0,33.89)),('catwalk south',(70.64,0,48.06),(73.06,0,48.06))]:guard(name,p,q)
 # Only now author routes through the built hall and its normal lower/upper doors.
 ascent=[[50,-6,35],[69.2,-6,35],[69.2,-6,23.5],[67,-6,23.5],[67,-6,25],[67,-3,34.1],[67,-3,35.9],[67,0,45],[67,0,46.5]]
 by={l['id']:l for l in data['links']};approach=list(reversed(by['L08']['points']));by['L09']['points']=approach+ascent[1:];by['L11']['points']=ascent;by['L12']['points']=list(reversed(ascent))+by['L08']['points'][1:]+[[43.75,-6,56],[43.75,-6,58],[70,-6,58],[70,-6,55]];by['L10']['points']=[[67,0,46.5],[68,0,46.5],[68,0,52],[40,0,52]]
 west_ascent=[[50,-6,35],[34.7,-6,35],[34.7,-6,23.5],[31,-6,23.5],[31,-6,25],[31,-3,34.1],[31,-3,35.9],[31,0,45],[31,0,46.5]]
 by['L07']['points']=list(reversed(west_ascent));by['L05']['points']=[[17,0,28],[26,0,28],[28.5,0,28],[28.5,0,46.5],[31,0,46.5]];by['L06']['points']=[[31,0,46.5],[31,0,52],[40,0,52]]
 by['L18']['construction_status']='INVALID_ROOM_CROSSING_GEOMETRY_REMOVED_ESCAPE_REPLACEMENT_OPEN'
 data['links'].append(dict(id='EAST-CATWALK',a='east-stair',b='east-service-catwalk',kind='level',width=1.05,points=[[67,0,46.5],[68,0,46.5],[68,0,47],[72,0,47]],player_access='OPEN_ROUTE'))
 for z in data['zones']:
  if z['id']=='west-stair':z.update(label_point=[31,0,46.5],purpose='Enclosed axial west stair with actual lower door and independent floor well')
  if z['id']=='east-stair':z.update(bounds=[64,22,70.5,48],kind='inside',label_point=[67,0,46.5],purpose='Enclosed main stair hall; actual floor, well, landing and upper exit')
 data['zones'].append(dict(id='west-lower-stair',name='West stair lower hall',bounds=[26,22,36,48],y=-6,height=6,kind='circulation',label_point=[34.7,-6,35],parent='west-stair'))
 data['zones'].extend([dict(id='east-lower-stair',name='East stair lower hall',bounds=[64,22,70.5,48],y=-6,height=6,kind='circulation',label_point=[69.2,-6,35],parent='east-stair'),dict(id='east-service-catwalk',name='East service catwalk',bounds=[70.5,33.75,73.2,48.2],y=0,height=3,kind='outside',label_point=[72,0,47],parent='east-stair')]);assert len(data['zones'])<=40
 data['authored_doorways']=data.get('authored_doorways',[])+list(portals.values())
 # Publish the actual saved terrain, without leaving the previous collider beneath it.
 mats=list(terrain.data.materials);vs=[];fs=[]
 for path in sorted((world/'scene-package/meshes').glob('site*.obj')):
  off=len(vs)
  for line in path.read_text().splitlines():
   p=line.split()
   if p and p[0]=='v':x,y,z=map(float,p[1:]);vs.append((x,-z,y))
   elif p and p[0]=='f':fs.append(tuple(off+int(i.split('/')[0])-1 for i in p[1:]))
 mesh=bpy.data.meshes.new('Saved enclosed hall terrain');mesh.from_pydata(vs,[],fs);mesh.update()
 for mt in mats:mesh.materials.append(mt)
 tree=BVHTree.FromPolygons([terrain.matrix_world@v.co for v in terrain.data.vertices],[list(f.vertices) for f in terrain.data.polygons]);uv=mesh.uv_layers.new(name='World metre terrain UV')
 for f in mesh.polygons:
  centre=sum((mesh.vertices[i].co for i in f.vertices),Vector())/len(f.vertices);hit=tree.find_nearest(centre)
  if hit[2] is not None:f.material_index=terrain.data.polygons[hit[2]].material_index
  for li in f.loop_indices:
   point=mesh.vertices[mesh.loops[li].vertex_index].co;uv.data[li].uv=(point.x/8,point.y/8)
 ground=bpy.data.objects.new('WLD-09 saved contained hall terrain',mesh);bpy.context.collection.objects.link(ground);ground['role']='saved editor terrain / contained stair hall';assert face_keys(records(ground))==face_keys(native_faces(world/'terrain-physics.hcs2'))
 for o in old:
  if o.name in removed:bpy.data.objects.remove(o,do_unlink=True)
 for o in built:
  bm=bmesh.new();bm.from_mesh(o.data);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=1e-6);bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces));assert all(e.is_manifold for e in bm.edges) and bm.calc_volume(signed=True)>0,o.name;bm.to_mesh(o.data);bm.free()
 receipt=json.loads((world/'stair-cell-punch-verification.json').read_text());assert receipt['physics_sha256']==sha(world/'terrain-physics.hcs2') and receipt['disk_reopen_verified']
 write(world/'verification.json',dict(site='WLD-09',status='SAVED_CONTAINED_STAIR_HALL_PUNCH_RUNTIME_OPEN',**receipt, saved_topology_sha256=sha(world/'adaptive-topology.bin'),saved_cuts_sha256=sha(world/'authored-cuts.tsv')))
 write(world/'source-provenance.json',dict(source_world=row['source_world'],source_files_sha256=row['source_world_files_sha256'],construction='Contained east and west stair hall footprints',cut_schedule='development/levels/rebuild/dam-contained-stair-pass/stair-cell-cuts.tsv',cut_schedule_sha256=sha(O/'stair-cell-cuts.tsv'),prior_holes_preserved=True))
 worldfiles={p.relative_to(ROOT).as_posix():sha(p) for p in world.rglob('*') if p.is_file()};data['authoritative_saved_terrain']=dict(world=row['world'],files_sha256=worldfiles,source_world=row['source_world']);layout=folder/'authored-layout.json';write(layout,data);bpy.ops.wm.save_as_mainfile(filepath=str(folder/'contained-hall.blend'),compress=True);bpy.ops.object.select_all(action='DESELECT')
 for o in built+[ground]:o.select_set(True)
 add=ROOT/'build/dam-contained-addition.glb';bpy.ops.export_scene.gltf(filepath=str(add),export_format='GLB',export_extras=True,use_selection=True);append(ROOT/s['glb'],add,folder/'contained-hall.glb',remove_mesh_names=sorted(removed));parts=native_write(ROOT/s['native'],folder/'contained-hall.hcs2',removed,built);kept=[p for p in split_native(folder/'contained-hall.hcs2') if not p[0].startswith('terrain-')];raw=(world/'terrain-physics.hcs2').read_bytes();magic,count,total=struct.unpack_from('>III',raw);(folder/'contained-hall.hcs2').write_bytes(struct.pack('>III',magic,len(kept)+count,sum(p[1] for p in kept)+total)+b''.join(p[2] for p in kept)+raw[12:])
 mapping=copy.deepcopy(meta.get('source_segment_map',{}));mapping['L07']={'1':0,'3':1};mapping['L09']={str(len(approach)-1+4):0,str(len(approach)-1+6):1};mapping['L11']={'4':0,'6':1};mapping['L12']={'1':0,'3':1}
 oldsteps=meta['stair_components'];meta.update(status='CONTAINED_PRIMARY_HALL_CONSTRUCTED_EMERGENCY_REPLACEMENT_OPEN',source_glb=s['glb'],source_glb_sha256=s['glb_sha256'],source_physics=s['native'],source_physics_sha256=s['native_sha256'],before_layout=s['layout'],before_layout_sha256=sha(ROOT/s['layout']),before_segment_map=meta.get('source_segment_map',{}),source_segment_map=mapping,before_stair_components=oldsteps,stair_components=[n for n in oldsteps if n.split(':',1)[-1] not in removed]+steps,layout=layout.relative_to(ROOT).as_posix(),layout_sha256=sha(layout),removed_source_meshes=sorted(removed),new_interface_parts=[o.name for o in built],hall_bounds=[64,22,70.5,48],west_hall_bounds=[26,22,36,48],main_flight=dict(low=[67,-6,25],middle_landing=[[67,-3,34.1],[67,-3,35.9]],high=[67,0,45],risers_per_flight=17,width=1.8),doorways=meta.get('doorways',[])+list(portals.values()),world=row['world'],world_files_sha256=worldfiles,saved_world=row['world'],saved_world_files_sha256=worldfiles,hall_builder='tools/build_dam_contained_stair.py',hall_builder_sha256=sha(Path(__file__)),glb_sha256=sha(folder/'contained-hall.glb'),physics_sha256=sha(folder/'contained-hall.hcs2'),blend_sha256=sha(folder/'contained-hall.blend'))
 meta['interface_added_flights']=meta.get('interface_added_flights',[])+[dict(link='L09',segment=1,layout_segment=len(approach)-1+6,points=[[67,-3,35.9],[67,0,45]]),dict(link='L11',segment=1,layout_segment=6,points=[[67,-3,35.9],[67,0,45]]),dict(link='L12',segment=1,layout_segment=3,points=[[67,-3,34.1],[67,-6,25]])]
 write(folder/'construction.json',meta);r=json.loads((B/'stair-interface-construction-pass/roster.json').read_text());entry=dict(id='WLD-09',construction=(folder/'construction.json').relative_to(ROOT).as_posix(),layout=layout.relative_to(ROOT).as_posix(),glb=(folder/'contained-hall.glb').relative_to(ROOT).as_posix(),native=(folder/'contained-hall.hcs2').relative_to(ROOT).as_posix(),blend=(folder/'contained-hall.blend').relative_to(ROOT).as_posix(),glb_sha256=meta['glb_sha256'],native_sha256=meta['physics_sha256']);r['active_sources']=[entry if v['id']=='WLD-09' else v for v in r['active_sources']]
 for v in r['sites']:
  if v['id']=='WLD-09':v['decision']='CONTAINED_MAIN_STAIR_HALL_CONSTRUCTED_EMERGENCY_REBUILD_OPEN'
 r['baseline']='development/levels/rebuild/stair-interface-construction-pass/roster.json';write(O/'roster.json',r);print('DAM_CONTAINED_HALL_CONSTRUCTED',flush=True)
if __name__=='__main__':main()
