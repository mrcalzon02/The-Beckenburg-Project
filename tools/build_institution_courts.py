"""Construct real institutional courts and normal exterior door interfaces."""
from pathlib import Path
import sys,json,math
import bpy,bmesh
from mathutils import Vector
from mathutils.bvhtree import BVHTree
ROOT=Path(__file__).resolve().parents[1];sys.path[:0]=[str(ROOT/'tools'),str(ROOT/'development/levels/blockouts')]
from build_roster_foundations import prism,material
from build_roster_floor_closures import difference
from planar_structure import plate
from structural_pass import deck_openings
from build_circulation_construction import native_write
from build_deliberate_entrances import write
from build_architectural_doorways import frame_template,TYPES
from review_roster_macroterrain import records,sha
from export_roster_collision import face_keys
from append_static_glb import append
BASE=ROOT/'development/levels/rebuild';OUT=BASE/'institution-courts-pass'
def rect(a,b,c,d):return [(a,b),(c,b),(c,d),(a,d)]
AREAS={
 'ASYLUM':[('enclosed patient exercise court',rect(10,29,26,40))],
 'PRISON':[('secure exercise yard',rect(22,22,46,58))],
 'SCHOOL':[('north school playground',rect(5,2,55,12)),('east receiving and gym service court',rect(46,12,55,36)),('west teaching-room court',rect(5,12,10,36)),('south school forecourt',rect(5,36,55,42))]}
DOORS={
 'ASYLUM':{'day':{'N':[(18,'service')]},'ground-south-a':{'S':[(13,'pedestrian')]}},
 'PRISON':{'sally':{'N':[(34,'loading')]}},
 'SCHOOL':{'science':{'W':[(16,'pedestrian')]},'class-a':{'W':[(28,'pedestrian')]},'gym':{'N':[(44,'service')]},'receiving':{'E':[(32,'service')]}}}
GUIDES={
 'SCHOOL':{
  'L36':[[28.6,0,16],[28.5,0,12],[28.5,0,6],[52,0,6],[52,0,26]],
  'L37':[[15,0,16.5],[12,0,16.5],[12,0,16],[8,0,16],[8,0,6],[52,0,6],[52,0,26]],
  'L38':[[15,0,29],[12,0,29],[12,0,28],[8,0,28],[8,0,6],[52,0,6],[52,0,26]],
  'L39':[[44,0,16],[44,0,6],[52,0,6],[52,0,26]],
  'L40':[[44,0,32],[52,0,32],[52,0,26]],
  'L41':[[52,0,26],[52,0,39],[28,0,39],[28,0,47.2]]}}

def selected_wall(o,schedule):
 for zone,faces in schedule.items():
  for side in faces:
   for prefix in ['Wall ','Header ']:
    needle=prefix+zone+' '+side
    if needle in o.name:
     suffix=o.name.split(needle,1)[1]
     if not suffix or suffix[0] in '. ':return True
 return False

def clipped(polys,cuts):
 for cut in cuts:polys=[p for poly in polys for p in difference(poly,cut)]
 return polys

def build(s):
 site=s['id'];folder=OUT/site.lower();folder.mkdir(parents=True,exist_ok=True);meta=json.loads((ROOT/s['construction']).read_text());data=json.loads((ROOT/s['layout']).read_text());source=ROOT/s['glb'];native=ROOT/s['native'];schedule=DOORS[site]
 bpy.ops.wm.read_factory_settings(use_empty=True);bpy.context.preferences.filepaths.save_version=0;bpy.ops.import_scene.gltf(filepath=str(source));old=[o for o in bpy.context.scene.objects if o.type=='MESH'];removed={o.name for o in old if selected_wall(o,schedule)};built=[];proof=[];portals=[]
 wallmat=next(o.data.materials[0] for o in old if o.get('role')=='authored doorway masonry');paving=material(site+' real exterior court paving',(.38,.37,.34));foundation=material(site+' compacted court foundation and perimeter stem',(.29,.28,.25))
 terrain=next(o for o in old if o.get('role','').startswith('saved editor terrain'));soil=BVHTree.FromPolygons([terrain.matrix_world@v.co for v in terrain.data.vertices],[tuple(f.vertices) for f in terrain.data.polygons])
 # Ground rooms and retained basement roof decks own their actual footprints.
 # Never bury a basement or cover an existing authored stair opening.
 occupied=[rect(*z['bounds']) for z in data['zones'] if z['kind'] not in ['outside','staging'] and z['y']<=0]
 cuts=occupied+[h['polygon'] for h in deck_openings(0,data)]
 for o in old:
  if o.get('role')=='soil-bearing entrance landing':
   vs=[o.matrix_world@v.co for v in o.data.vertices];cuts.append(rect(min(v.x for v in vs),min(-v.y for v in vs),max(v.x for v in vs),max(-v.y for v in vs)))
 prior=[]
 for name,foot in AREAS[site]:
  polys=clipped([foot],cuts+prior);prior.append(foot);assert polys
  hits=[];missing=0
  # Sample only the actual clipped architectural area, not rooms or terrain holes.
  def inside(x,z,poly):
   c=False
   for p,q in zip(poly,poly[1:]+poly[:1]):
    if (p[1]>z)!=(q[1]>z) and x<(q[0]-p[0])*(z-p[1])/(q[1]-p[1])+p[0]:c=not c
   return c
  for x in range(math.floor(min(p[0] for p in foot)),math.ceil(max(p[0] for p in foot))):
   for z in range(math.floor(min(p[1] for p in foot)),math.ceil(max(p[1] for p in foot))):
    if not any(inside(x+.5,z+.5,p) for p in polys):continue
    h=soil.ray_cast(Vector((x+.5,-z-.5,20)),Vector((0,0,-1)),100)[0]
    if h is None:missing+=1
    else:hits.append(h.z)
  assert hits and max(hits)<-.01,(site,name,'Soil intersects planned court',min(hits),max(hits))
  bottom=min(hits)-.35
  for o in plate(site+' '+name+' pavement',polys,0,.20,paving) or []:o['role']='authored institutional court pavement';o['area']=name;built.append(o)
  for o in plate(site+' '+name+' soil-bearing foundation',polys,-.20,-.20-bottom,foundation) or []:o['role']='soil-bearing court foundation';o['area']=name;built.append(o)
  proof.append(dict(area=name,architectural_footprint=foot,constructed_polygons=polys,surface_y=0,pavement_depth=.20,foundation_bottom_y=bottom,soil_range=[min(hits),max(hits)],soil_samples=len(hits),saved_hole_samples=missing,excluded='Actual ground/below-grade rooms, retained basement roof footprints, saved stair apertures and existing entrance landings'))
 zones={z['id']:z for z in data['zones']};template=frame_template()
 for zone,faces in schedule.items():
  z=zones[zone];a,b,c,d=z['bounds'];h=z['height'];assert z['y']==0
  for side,openings in faces.items():
   along=side in ['N','S'];lo,hi=(a,c) if along else (b+.24,d-.24);fixed={'N':b,'S':d,'W':a,'E':c}[side]
   def wall(start,end,low,high,label):
    if end-start<.000001:return
    foot=rect(start,fixed,end,fixed+.24) if side=='N' else rect(start,fixed-.24,end,fixed) if side=='S' else rect(fixed,start,fixed+.24,end) if side=='W' else rect(fixed-.24,start,fixed,end)
    o=prism(site+' court-door '+zone+' '+side+' '+label,foot,high,[low]*4,wallmat);o['role']='architectural exterior doorway wall';o['zone']=zone;built.append(o)
   cursor=lo
   for i,(center,kind) in enumerate(openings):
    w,head=TYPES[kind];left,right=center-.62*w,center+.62*w;assert lo+.05<left<right<hi-.05
    wall(cursor,left,0,h,'pier'+str(i));wall(left,right,head*2.275/2.125,h,'lintel'+str(i));cursor=right
    span=[fixed,fixed+.24] if side in ['N','W'] else [fixed-.24,fixed];middle=sum(span)/2;p=dict(id=site+'-court-'+zone+'-'+side,axis='z' if along else 'x',boundary=fixed,along=center,door_type=kind,clear_width=w,clear_height=head,sill_y=0,frame_span=span,frame_depth=.24,rooms=[zone],side=side,frame_center=[center,0,middle] if along else [middle,0,center],boundary_center=[center,0,fixed] if along else [fixed,0,center]);portals.append(p)
    o=template.copy();o.data=template.data.copy();o.name=p['id']+' indexed exterior jambs and head';bpy.context.collection.objects.link(o)
    for v in o.data.vertices:
     original=template.matrix_world@v.co;u=original.x*w;normal=original.y*.24/.18;y=original.z*head/2.125
     v.co=(center+u,-middle+normal,y) if side=='N' else (center-u,-middle-normal,y) if side=='S' else (middle-normal,-center+u,y) if side=='W' else (middle+normal,-center-u,y)
    o.matrix_world.identity();o['role']='fitted indexed exterior doorway';o['portal_id']=p['id'];o['source_asset']='level_blockout_doorway';built.append(o)
   wall(cursor,hi,0,h,'end')
 bpy.data.objects.remove(template,do_unlink=True)
 for o in old:
  if o.name in removed:bpy.data.objects.remove(o,do_unlink=True)
 for o in built:
  bm=bmesh.new();bm.from_mesh(o.data);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=.000001);bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces));assert all(e.is_manifold for e in bm.edges) and bm.calc_volume(signed=True)>0,(site,o.name,'Open/inverted solid');bm.to_mesh(o.data);bm.free()
 for link in data['links']:
  if link['id'] in GUIDES.get(site,{}):link['points']=GUIDES[site][link['id']]
 if site=='SCHOOL':data['authored_exterior_anchors']={'grounds':dict(point=[52,0,26],reason='Actual east service court outside gym/health rooms; historical outdoor envelope center was inside the building. Routes use normal exterior doors and completed courts.')}
 data['authored_doorways']=data.get('authored_doorways',[])+portals;data['institutional_courts']=proof
 layout=folder/'authored-layout.json';write(layout,data);bpy.ops.wm.save_as_mainfile(filepath=str(folder/'constructed-courts.blend'),compress=True);bpy.ops.object.select_all(action='DESELECT')
 for o in built:o.select_set(True)
 addition=ROOT/'build'/('institution-court-addition-'+site+'.glb');bpy.ops.export_scene.gltf(filepath=str(addition),export_format='GLB',export_extras=True,use_selection=True);append(source,addition,folder/'constructed-courts.glb',remove_mesh_names=sorted(removed));parts=native_write(native,folder/'constructed-courts.hcs2',removed,built)
 bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(folder/'constructed-courts.glb'));objs={o.name:o for o in bpy.context.scene.objects if o.type=='MESH'}
 for name,ts in parts:assert face_keys(records(objs[name]))==face_keys(ts)
 for o in objs.values():
  assert not o.get('role','').startswith('rough route support')
  if o.get('role')=='enclosed pitched roof' or o.get('role')=='ceiling-cap':o.hide_render=True
 scene=bpy.context.scene;scene.render.engine='BLENDER_WORKBENCH';scene.display.shading.color_type='MATERIAL';scene.display.shading.show_cavity=True;scene.render.resolution_x=1400;scene.render.resolution_y=1100;scene.render.resolution_percentage=100;scene.world=bpy.data.worlds.new('Institutional court inspection');bpy.ops.object.camera_add();cam=bpy.context.object;scene.camera=cam;a,b,c,d=data['site_bounds'];span=max(c-a,d-b);target=Vector(((a+c)/2,-(b+d)/2,0));cam.location=target+Vector((span*.45,-span*.6,span));cam.rotation_euler=(target-cam.location).to_track_quat('-Z','Y').to_euler();cam.data.type='ORTHO';cam.data.ortho_scale=span*1.15;cap=BASE/'captures/institution-courts-batch'/(site.lower()+'.png');cap.parent.mkdir(parents=True,exist_ok=True);scene.render.filepath=str(cap);bpy.ops.render.render(write_still=True)
 meta.update(status='INSTITUTIONAL_COURTS_CONSTRUCTED_RUNTIME_AND_REMAINING_GEOMETRY_OPEN',source_contract=s['construction'],source_glb=s['glb'],source_glb_sha256=s['glb_sha256'],source_physics=s['native'],source_physics_sha256=s['native_sha256'],before_layout=s['layout'],before_layout_sha256=sha(ROOT/s['layout']),before_segment_map=meta.get('source_segment_map',{}),before_stair_components=meta['stair_components'],layout=layout.relative_to(ROOT).as_posix(),layout_sha256=sha(layout),glb_sha256=sha(folder/'constructed-courts.glb'),physics_sha256=sha(folder/'constructed-courts.hcs2'),blend_sha256=sha(folder/'constructed-courts.blend'),court_construction=proof,doorways=meta.get('doorways',[])+portals,exterior_doorways=portals,removed_exterior_walls=sorted(removed),court_builder='tools/build_institution_courts.py',court_builder_sha256=sha(Path(__file__)),doorframe_helper_sha256=sha(ROOT/'tools/build_architectural_doorways.py'),captures=[dict(path=cap.relative_to(ROOT).as_posix(),sha256=sha(cap),type='Native exported court construction, roof cutaway, not gameplay')]);write(folder/'construction.json',meta);print('INSTITUTION_COURTS_BUILT',site,len(proof),len(portals),flush=True)
 return dict(id=site,construction=(folder/'construction.json').relative_to(ROOT).as_posix(),layout=layout.relative_to(ROOT).as_posix(),glb=(folder/'constructed-courts.glb').relative_to(ROOT).as_posix(),native=(folder/'constructed-courts.hcs2').relative_to(ROOT).as_posix(),blend=(folder/'constructed-courts.blend').relative_to(ROOT).as_posix(),glb_sha256=meta['glb_sha256'],native_sha256=meta['physics_sha256'])

def main():
 source=BASE/'institution-doorway-pass/roster.json';r=json.loads(source.read_text());rows=[build(s) if s['id'] in AREAS else s for s in r['active_sources']]
 for s in r['sites']:
  if s['id'] in AREAS:s['decision']='COURTS_AND_NORMAL_EXTERIOR_DOORS_CONSTRUCTED_RUNTIME_OPEN'
 write(OUT/'roster.json',dict(active_sources=rows,sites=r['sites'],baseline=source.relative_to(ROOT).as_posix()));print('INSTITUTION_COURT_BATCH_COMPLETE',flush=True)
if __name__=='__main__':main()
