"""Build constant flights, complete landings and an enclosed tunnel elbow."""
from pathlib import Path
import sys,json,math,struct
import bpy,bmesh
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[1];sys.path[:0]=[str(ROOT/'tools'),str(ROOT/'development/levels/blockouts')]
from build_roster_foundations import prism,material
from planar_structure import plate
from build_roster_floor_closures import difference
from build_architectural_doorways import frame_template,TYPES
from append_static_glb import append
from build_circulation_construction import native_write
from build_deliberate_entrances import write
from review_roster_macroterrain import records,sha
from export_roster_collision import face_keys
TYPES={**TYPES,'low-service':(1.8,2.0)}
BASE=ROOT/'development/levels/rebuild';OUT=BASE/'stair-cell-construction-pass';RAIL=ROOT/'development/levels/primitive-props-v1/models/level_blockout_guardrail.glb'
def rect(a,b,c,d):return [(a,b),(c,b),(c,d),(a,d)]
def bounds(o):
 v=[o.matrix_world@p.co for p in o.data.vertices];return min(p.x for p in v),min(-p.y for p in v),max(p.x for p in v),max(-p.y for p in v),min(p.z for p in v),max(p.z for p in v)
ROOMS={
 'SCHOOL':{'main-stair':{'W':[(21,'pedestrian')],'E':[(21,'pedestrian'),(26,'pedestrian')],'N':[(28.6,'pedestrian')]},'upper-stair':{'W':[(29,'pedestrian')],'S':[(26.5,'service')]},'upper-lobby':{'N':[(26.5,'service')]},'service-stair':{'N':[(29.1,'pedestrian')],'S':[(28.6,'pedestrian')]},'lower-stair':{'W':[(13,'pedestrian')]},'plant':{'E':[(13,'pedestrian')]}},
 'PRISON':{'plant-stair':{'N':[(30,'pedestrian')],'W':[(31,'service')]},'lower-stair':{'E':[(31,'service')]},'plant':{'W':[(31,'service')]}},
 'WLD-08':{'tunnel':{'N':[(22,'low-service')],'W':[]},'tunnel-n':{'E':[(23,'low-service')],'S':[]},'tunnel-elbow':{'W':[(23,'low-service')],'S':[(22,'low-service')]}}}
PARTIAL={'SCHOOL':{'plant','upper-lobby'},'PRISON':{'plant'},'WLD-08':{'tunnel','tunnel-n'}}
FLIGHTS={'SCHOOL':[('L43',25,22,27.76,0,3.6,1.8,20),('L44',28.04,13.5,18.3,-3,0,1.2,17)],'PRISON':[('L112',29.4,24,28.8,-3,0,1.8,17)],'WLD-08':[]}
FLOORS={'SCHOOL':[('main-stair',0,rect(23,20,30,30),[]),('upper-stair',3.6,rect(23,22,30,30),[rect(24,22,26,27.76)]),('service-stair',0,rect(27.2,12,30,20),[rect(27.4,13.3,28.7,18.3)]),('lower-stair',-3,rect(27.2,12,30,20),[])], 'PRISON':[('plant-stair',0,rect(28,22,32,34),[rect(28.4,23.8,30.4,28.8)]),('lower-stair',-3,rect(28,22,32,34),[])], 'WLD-08':[('tunnel-elbow',-2.5,rect(20,21,24,26),[])]}
GUIDES={
 'SCHOOL':{'L08':[[26.5,0,25],[28.6,0,25],[28.6,0,19.2]],'L27':[[21.5,3.6,24],[21.5,3.6,29],[26.5,3.6,29]],'L33':[[28.6,-3,12.75],[28.6,-3,13],[25.1,-3,13],[25.1,-3,16]],'L36':[[28.6,0,19.2],[29.2,0,19.2],[29.2,0,13],[29.1,0,13],[29.1,0,10],[29.1,0,6],[52,0,6],[52,0,26]],'L43':[[26.5,0,25],[27.5,0,25],[27.5,0,21],[25,0,21],[25,0,22],[25,3.6,27.76],[25,3.6,29],[26.5,3.6,29]],'L44':[[28.6,-3,12.75],[28.04,-3,12.75],[28.04,-3,13.5],[28.04,0,18.3],[28.04,0,19.2],[28.6,0,19.2]]},
 'PRISON':{'L084':[[34,0,40],[26,0,40],[26,0,31],[31,0,31]],'L085':[[31,0,31],[31.2,0,31],[31.2,0,23],[30,0,23],[30,0,19],[34,0,19]],'L105':[[29.4,-3,23],[31,-3,23],[31,-3,31],[39,-3,31],[39,-3,28]],'L112':[[29.4,-3,23],[29.4,-3,24],[29.4,0,28.8],[29.4,0,31],[31,0,31]]},
 'WLD-08':{'L19':[[32,-2.5,31],[22,-2.5,31],[22,-2.5,23],[11,-2.5,23],[11,-2.5,22]]}}

def wall_identity(o,schedule,partial):
 for zone,faces in schedule.items():
  for side in (faces if zone in partial else ['N','S','W','E']):
   for prefix in ['Wall ','Header ']:
    needle=prefix+zone+' '+side
    if needle in o.name:
     suffix=o.name.split(needle,1)[1]
     if not suffix or suffix[0] in '. ':return True
 return False

def build(s):
 site=s['id'];folder=OUT/site.lower();folder.mkdir(parents=True,exist_ok=True);source=ROOT/s['glb'];native=ROOT/s['native'];meta=json.loads((ROOT/s['construction']).read_text());data=json.loads((ROOT/s['layout']).read_text());bpy.ops.wm.read_factory_settings(use_empty=True);bpy.context.preferences.filepaths.save_version=0;bpy.ops.import_scene.gltf(filepath=str(source));old=[o for o in bpy.context.scene.objects if o.type=='MESH'];removed=set();built=[];steps=[];portals={};selected_links={f[0] for f in FLIGHTS[site]};schedule=ROOMS[site];partial=PARTIAL[site]
 concrete=material(site+' engineered stair-cell concrete',(.39,.40,.38));steel=material(site+' supported stair and opening steel',(.23,.25,.26));floorzones={f[0] for f in FLOORS[site]}
 if site=='WLD-08':data['zones'].append(dict(id='tunnel-elbow',name='Enclosed service tunnel elbow',bounds=[20,21,24,26],y=-2.5,height=2.2,kind='circulation',purpose='Orthogonal enclosed connection between existing tunnel branches',player_access='OPEN_SEARCH',gameplay_eligible=True))
 zones={z['id']:z for z in data['zones']}
 for o in old:
  if wall_identity(o,schedule,partial) or o.get('zone') in floorzones and o.get('role')=='floor' or o.get('link') in selected_links:removed.add(o.name)
  if o.get('zone') in ['main-stair','lower-stair'] and o.get('role')=='ceiling-cap' and any(abs(bounds(o)[5]-f[1])<.01 for f in FLOORS[site]):removed.add(o.name)
  if any(link in o.name for link in selected_links) and any(t in o.get('role','') for t in ['guard','stringer','landing','stair']):removed.add(o.name)
 def make(name,polys,top,depth,role,zone=None,link=None):
  result=plate(site+' '+name,polys,top,depth,concrete) or []
  for o in result:
   o['role']=role
   if zone:o['zone']=zone
   if link:o['link']=link
   built.append(o)
   if role=='structural-stair-step':steps.append(o.name)
  return result
 for zone,y,foot,cuts in FLOORS[site]:
  polys=[foot]
  for cut in cuts:polys=[q for p in polys for q in difference(p,cut)]
  make('cell continuous floor '+zone,polys,y,.30,'floor',zone)
 # Trim old shallow soil foundations to the actual new stair/underground cells.
 openings=[(rect(27.4,13.3,28.7,18.3),-3,0)] if site=='SCHOOL' else [(rect(28.4,23.8,30.4,28.8),-3,0)] if site=='PRISON' else [(rect(20,21,24,26),-2.5,-.3)]
 for o in old:
  if o.name in removed or not any(k in o.get('role','') for k in ['foundation','bearing']):continue
  a,b,c,d,low,high=bounds(o);active=[p for p,y0,y1 in openings if low<y1-.001 and high>y0+.001 and a<max(q[0] for q in p) and c>min(q[0] for q in p) and b<max(q[1] for q in p) and d>min(q[1] for q in p)]
  if not active:continue
  polys=[];o.data.calc_loop_triangles()
  for t in o.data.loop_triangles:
   ps=[o.matrix_world@o.data.vertices[i].co for i in t.vertices]
   if max(abs(p.z-high) for p in ps)>1e-5 or (ps[1]-ps[0]).cross(ps[2]-ps[0]).z<=0:continue
   pieces=[[(p.x,-p.y) for p in ps]]
   for cut in active:pieces=[q for p in pieces for q in difference(p,cut)]
   polys+=pieces
  for clone in plate(site+' retained footing trimmed to structural cell '+o.name,polys,high,high-low,o.data.materials[0]) or []:clone['role']='retained footing around authored stair cell';built.append(clone)
  removed.add(o.name)
 for zone,faces in schedule.items():
  z=zones[zone];a,b,c,d=z['bounds'];base=z['y'];top=base+z['height'] if zone!='tunnel-elbow' else -.16
  for side in (faces if zone in partial else ['N','S','W','E']):
   along=side in ['N','S'];lo,hi=(a,c) if along else (b,d) if zone in partial else (b+.24,d-.24);fixed={'N':b,'S':d,'W':a,'E':c}[side]
   def wall(start,end,low,high,label):
    if end-start<1e-6:return
    assert high>low,(site,zone,side,'Inverted wall/header datum',low,high)
    foot=rect(start,fixed,end,fixed+.24) if side=='N' else rect(start,fixed-.24,end,fixed) if side=='S' else rect(fixed,start,fixed+.24,end) if side=='W' else rect(fixed-.24,start,fixed,end)
    make('cell '+zone+' '+side+' '+label,[foot],high,high-low,'stair-cell structural wall',zone)
   if site=='WLD-08' and zone=='tunnel' and side=='W':
    wall(lo,33.8,base,top,'solid return closing obsolete diagonal cut');continue
   cursor=lo
   for i,(center,kind) in enumerate(faces.get(side,[])):
    w,h=TYPES[kind];left,right=center-.62*w,center+.62*w;assert lo<left<right<hi,(site,zone,side,lo,left,right,hi);wall(cursor,left,base,top,'pier'+str(i));wall(left,right,base+h*2.275/2.125,top,'lintel'+str(i));cursor=right;axis='z' if along else 'x';key=(axis,fixed,center,base);span=[fixed,fixed+.24] if side in ['N','W'] else [fixed-.24,fixed]
    if key not in portals:portals[key]=dict(id=site+'-cell-'+zone+'-'+side+'-'+str(center),axis=axis,boundary=fixed,along=center,door_type=kind,clear_width=w,clear_height=h,sill_y=base,frame_span=span,rooms=[zone],side=side)
    else:portals[key]['frame_span']=[min(portals[key]['frame_span'][0],span[0]),max(portals[key]['frame_span'][1],span[1])];portals[key]['rooms'].append(zone)
   wall(cursor,hi,base,top,'end')
 # Shared openings also span the actual thickness of retained adjoining walls.
 for p in portals.values():
  if len(p['rooms'])!=1:continue
  opposite={'N':'S','S':'N','W':'E','E':'W'}[p['side']]
  for zid,z in zones.items():
   if zid in p['rooms'] or z['kind'] in ['outside','staging'] or abs(z['y']-p['sill_y'])>.001:continue
   a,b,c,d=z['bounds'];face={'N':b,'S':d,'W':a,'E':c}[opposite];lo,hi=(a,c) if p['axis']=='z' else (b,d)
   if abs(face-p['boundary'])>.001 or not lo<p['along']<hi:continue
   neighbours=[o for o in old if o.name not in removed and wall_identity(o,{zid:{opposite:[]}}, {zid})]
   if not neighbours:continue
   limits=[bounds(o) for o in neighbours];axis=1 if p['axis']=='z' else 0
   p['frame_span']=[min(p['frame_span'][0],min(v[axis] for v in limits)),max(p['frame_span'][1],max(v[axis+2] for v in limits))];p['rooms'].append(zid)
 template=frame_template()
 for p in portals.values():
  middle=sum(p['frame_span'])/2;depth=p['frame_span'][1]-p['frame_span'][0];o=template.copy();o.data=template.data.copy();o.name=p['id']+' indexed frame';bpy.context.collection.objects.link(o)
  for v in o.data.vertices:
   q=template.matrix_world@v.co;u=q.x*p['clear_width'];normal=q.y*depth/.18;y=p['sill_y']+q.z*p['clear_height']/2.125;c=p['along'];side=p['side'];v.co=(c+u,-middle+normal,y) if side=='N' else (c-u,-middle-normal,y) if side=='S' else (middle-normal,-c+u,y) if side=='W' else (middle+normal,-c-u,y)
  o.matrix_world.identity();o['role']='indexed structural cell doorway';o['portal_id']=p['id'];o['source_asset']='level_blockout_doorway';built.append(o);p['frame_depth']=depth;p['frame_center']=[p['along'],p['sill_y'],middle] if p['axis']=='z' else [middle,p['sill_y'],p['along']]
 bpy.data.objects.remove(template,do_unlink=True)
 for link,x,z0,z1,y0,y1,width,n in FLIGHTS[site]:
  for i in range(n):make('constant '+link+' tread '+str(i),[rect(x-width/2,z0+(z1-z0)*i/n,x+width/2,z0+(z1-z0)*(i+1)/n)],y0+(y1-y0)*(i+1)/n,y0+(y1-y0)*(i+1)/n-(y0-.16),'structural-stair-step',link=link)
 if site=='WLD-08':make('tunnel elbow roof underside thickening',[rect(20,21,24,26)],-.16,.14,'enclosed tunnel roof underside','tunnel-elbow')
 # Reuse indexed rails on real tread/landing/shaft edges. No flares or arbitrary cuts.
 before=set(bpy.context.scene.objects);bpy.ops.import_scene.gltf(filepath=str(RAIL));railmesh=next(o for o in set(bpy.context.scene.objects)-before if o.type=='MESH').data.copy()
 for o in set(bpy.context.scene.objects)-before:bpy.data.objects.remove(o,do_unlink=True)
 def rail(name,p,q,link=None):
  length=math.hypot(q[0]-p[0],q[2]-p[2]);ux,uz=(q[0]-p[0])/length,(q[2]-p[2])/length;n=math.ceil(length/2)
  for i in range(n):
   mesh=railmesh.copy();o=bpy.data.objects.new(site+' cell guard '+name+' '+str(i),mesh);bpy.context.collection.objects.link(o)
   for v in mesh.vertices:
    t=(v.co.x+1)*length/n/2+i*length/n;off=v.co.y;v.co=(p[0]+ux*t-uz*off,-(p[2]+uz*t+ux*off),p[1]+(q[1]-p[1])*t/length+v.co.z)
   o['role']='indexed supported stair-cell guard';o['source_asset']='level_blockout_guardrail'
   if link:o['link']=link
   built.append(o)
 for link,x,z0,z1,y0,y1,width,n in FLIGHTS[site]:
  for xx in [x-width/2+.07,x+width/2-.07]:rail(link+' flight '+str(xx),(xx,y0,z0),(xx,y1,z1),link)
 if site=='SCHOOL':
  for x in [23.94,26.06]:rail('upper shaft '+str(x),(x,3.6,22),(x,3.6,27.7))
  rail('service bypass rim',(28.76,0,13.3),(28.76,0,18.25));rail('service north shaft return',(27.45,0,13.24),(28.76,0,13.24))
 if site=='PRISON':
  for x in [28.34,30.46]:rail('plant shaft '+str(x),(x,0,23.8),(x,0,28.75))
  rail('plant north shaft return',(28.34,0,23.74),(30.46,0,23.74))
 for o in old:
  if o.name in removed:bpy.data.objects.remove(o,do_unlink=True)
 for o in built:
  bm=bmesh.new();bm.from_mesh(o.data);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=1e-6);bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces));assert all(e.is_manifold for e in bm.edges) and bm.calc_volume(signed=True)>0,(site,o.name);bm.to_mesh(o.data);bm.free()
 for link in data['links']:
  if link['id'] in GUIDES[site]:link['points']=GUIDES[site][link['id']]
 if site=='SCHOOL':data['links'].append(dict(id='L45',a='upper-stair',b='upper-lobby',kind='level',width=1.2,points=[[26.5,3.6,29],[26.5,3.6,33]],threshold=[26.5,3.6,30],access='public',player_access='OPEN_ROUTE',note='Actual full-depth doorway and continuous landing connect stair head to upper lobby'))
 data.setdefault('doorway_types',{}).update({k:dict(clear_width=v[0],clear_height=v[1]) for k,v in TYPES.items()})
 data['authored_doorways']=data.get('authored_doorways',[])+list(portals.values());data['structural_stair_cells']=dict(flights=[dict(link=f[0],center_x=f[1],z0=f[2],z1=f[3],lower_y=f[4],upper_y=f[5],gross_width=f[6],risers=f[7]) for f in FLIGHTS[site]],floor_openings=[dict(zone=f[0],y=f[1],footprint=f[2],rectangular_holes=f[3]) for f in FLOORS[site]])
 if site=='WLD-08':
  data['authored_stair_passages']=[dict(zone='tunnel',side='W',axis='x',boundary=20,along_min=33.8,along_max=36,base_y=-2.5,top_y=0,purpose='Existing actual west stair cell; separate from closed obsolete diagonal tunnel corner')]
  world=OUT/'worlds/wld-08/terrain-world';data.setdefault('terrain_cutouts',[]).append(dict(bounds=[20,21,24,26],method='Actual saved editor tunnel elbow footprint; existing punched leaves already cover it; disk reopen/hole controls verified'))
  data['authoritative_saved_terrain']=dict(world=world.relative_to(ROOT).as_posix(),source_world='development/levels/rebuild/site-earthworks-pass/worlds/wld-08/terrain-world',files_sha256={p.relative_to(ROOT).as_posix():sha(p) for p in world.rglob('*') if p.is_file() and not p.name.endswith('.lock')})
 layout=folder/'authored-layout.json';write(layout,data);bpy.ops.wm.save_as_mainfile(filepath=str(folder/'constructed-cells.blend'),compress=True);bpy.ops.object.select_all(action='DESELECT')
 for o in built:o.select_set(True)
 addition=ROOT/'build'/('three-cell-addition-'+site+'.glb');bpy.ops.export_scene.gltf(filepath=str(addition),export_format='GLB',export_extras=True,use_selection=True);append(source,addition,folder/'constructed-cells.glb',remove_mesh_names=sorted(removed));parts=native_write(native,folder/'constructed-cells.hcs2',removed,built)
 bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(folder/'constructed-cells.glb'));objs={o.name:o for o in bpy.context.scene.objects if o.type=='MESH'}
 for name,ts in parts:assert face_keys(records(objs[name]))==face_keys(ts)
 for o in objs.values():
  if o.get('role') in ['enclosed pitched roof','ceiling-cap'] or site=='WLD-08' and o.get('zone')=='radio' and o.get('role')=='floor':o.hide_render=True
 scene=bpy.context.scene;scene.render.engine='BLENDER_WORKBENCH';scene.display.shading.color_type='MATERIAL';scene.display.shading.show_cavity=True;scene.render.resolution_x=1400;scene.render.resolution_y=1100;scene.render.resolution_percentage=100;scene.world=bpy.data.worlds.new('Structural cell inspection');bpy.ops.object.camera_add();cam=bpy.context.object;scene.camera=cam;target=Vector((27,-23,1.2)) if site=='SCHOOL' else Vector((31,-28,-.8)) if site=='PRISON' else Vector((21,-25,-1.5));cam.location=target+Vector((15,-18,24));cam.rotation_euler=(target-cam.location).to_track_quat('-Z','Y').to_euler();cam.data.type='ORTHO';cam.data.ortho_scale=26;cap=BASE/'captures/stair-cell-construction-batch'/(site.lower()+'.png');cap.parent.mkdir(parents=True,exist_ok=True);scene.render.filepath=str(cap);bpy.ops.render.render(write_still=True)
 oldsteps=meta['stair_components'];meta.update(status='STRUCTURAL_STAIR_CELLS_AND_TUNNEL_ELBOW_CONSTRUCTED_RUNTIME_OPEN',source_contract=s['construction'],source_glb=s['glb'],source_glb_sha256=s['glb_sha256'],source_physics=s['native'],source_physics_sha256=s['native_sha256'],before_layout=s['layout'],before_layout_sha256=sha(ROOT/s['layout']),before_segment_map=meta.get('source_segment_map',{}),before_stair_components=oldsteps,stair_components=[n for n in oldsteps if n.split(':',1)[-1] not in removed]+steps,layout=layout.relative_to(ROOT).as_posix(),layout_sha256=sha(layout),glb_sha256=sha(folder/'constructed-cells.glb'),physics_sha256=sha(folder/'constructed-cells.hcs2'),blend_sha256=sha(folder/'constructed-cells.blend'),structural_stair_cells=data['structural_stair_cells'],doorways=meta.get('doorways',[])+list(portals.values()),removed_cell_parts=sorted(removed),cell_builder='tools/build_three_stair_cells.py',cell_builder_sha256=sha(Path(__file__)),captures=[dict(path=cap.relative_to(ROOT).as_posix(),sha256=sha(cap),type='Native stair/landing/tunnel construction cutaway, ceilings/context hidden, not gameplay')])
 for link in selected_links:
  meta.setdefault('source_segment_map',{})[link]={str(i):0 for i,(a,b) in enumerate(zip(GUIDES[site][link],GUIDES[site][link][1:])) if abs(a[1]-b[1])>.01}
 if site=='WLD-08':
  meta['saved_world']=data['authoritative_saved_terrain']['world'];meta['saved_world_files_sha256']=data['authoritative_saved_terrain']['files_sha256'];meta['saved_hole_coverage_note']='Existing saved punched leaves already covered the new enclosed elbow; height/layers and terrain geometry are unchanged. No new cutout leaves or terrain construction claimed.'
 meta['frame_library_sha256']=sha(ROOT/'development/levels/primitive-props-v1/models/level_blockout_doorway.glb');meta['guard_library_sha256']=sha(RAIL);meta['frame_helper_sha256']=sha(ROOT/'tools/build_architectural_doorways.py')
 write(folder/'construction.json',meta);print('STRUCTURAL_CELLS_CONSTRUCTED',site,len(built),flush=True)
 return dict(id=site,construction=(folder/'construction.json').relative_to(ROOT).as_posix(),layout=layout.relative_to(ROOT).as_posix(),glb=(folder/'constructed-cells.glb').relative_to(ROOT).as_posix(),native=(folder/'constructed-cells.hcs2').relative_to(ROOT).as_posix(),blend=(folder/'constructed-cells.blend').relative_to(ROOT).as_posix(),glb_sha256=meta['glb_sha256'],native_sha256=meta['physics_sha256'])

def main():
 r=json.loads((BASE/'institution-earthworks-pass/roster.json').read_text());rows=[build(s) if s['id'] in ROOMS else s for s in r['active_sources']]
 for s in r['sites']:
  if s['id'] in ROOMS:s['decision']='STRUCTURAL_CELLS_CONSTRUCTED_REMAINING_ARCHITECTURE_AND_RUNTIME_OPEN'
 write(OUT/'roster.json',dict(active_sources=rows,sites=r['sites'],baseline='development/levels/rebuild/institution-earthworks-pass/roster.json'));print('THREE_STAIR_CELL_BATCH_COMPLETE',flush=True)
if __name__=='__main__':main()
