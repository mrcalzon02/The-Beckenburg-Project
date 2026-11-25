"""Construct institutional entrance blocks and adjoining corridor door faces from architectural schedules."""
from pathlib import Path
import sys,json,math,copy
import bpy,bmesh
from mathutils import Vector
from mathutils.bvhtree import BVHTree
ROOT=Path(__file__).resolve().parents[1];sys.path[:0]=[str(ROOT/'tools'),str(ROOT/'development/levels/blockouts')]
from append_static_glb import append
from build_circulation_construction import native_write
from build_roster_foundations import prism
from review_roster_macroterrain import records,sha
from export_roster_collision import face_keys
from build_deliberate_entrances import write
BASE=ROOT/'development/levels/rebuild';OUT=BASE/'institution-doorway-pass';MODEL=ROOT/'development/levels/primitive-props-v1/models/level_blockout_doorway.glb'
TYPES={'pedestrian':(1.05,2.10),'service':(1.80,2.30),'loading':(2.40,2.60)}
# Positions are door datums on actual wall faces, not swept path intersections.
SCHEDULE={
 'SCHOOL':{
  'foyer':{'N':[(21.5,'service')],'S':[(22.5,'service')],'E':[(31.5,'pedestrian'),(34.5,'pedestrian')]},
  'interview':{'W':[(31.5,'pedestrian')],'S':[(28.75,'pedestrian')]},
  'visitor-wc':{'W':[(34.5,'pedestrian')]},
  'office':{'N':[(28.75,'pedestrian')]},
  'corridor':{'S':[(21.5,'service')]}},
 'ASYLUM':{
  'admission':{'N':[(27.5,'service')],'S':[(30,'service')],'E':[(52,'pedestrian')]},
  'interview':{'W':[(52,'pedestrian')],'S':[(39.5,'pedestrian')]},
  'records':{'N':[(39.5,'pedestrian')]},
  'spine':{'S':[(27.5,'service')]}},
 'PRISON':{
  'reception':{'N':[(14,'service')],'S':[(14,'service')],'E':[(65,'pedestrian')]},
  'intake':{'N':[(14,'service')],'S':[(14,'service')]},
  'records':{'W':[(65,'pedestrian')]},
  'a-front-link':{'S':[(14,'service')]}}}
PORCHES={'SCHOOL':(20,36,25,39),'ASYLUM':(26,58,35,61),'PRISON':(10,68,18,71)}
FACE_ONLY={'SCHOOL':{'corridor'},'ASYLUM':{'spine'},'PRISON':{'a-front-link'}}
GUIDES={
 'SCHOOL':{'L01':[[22.5,0,33],[23.5,0,33],[23.5,0,31.5],[27.5,0,31.5]],'L02':[[22.5,0,33],[22.5,0,34.5],[26.25,0,34.5]],'L03':[[27.5,0,31.5],[28.75,0,31.5],[28.75,0,34.5]],'L04':[[22.5,0,33],[21.5,0,33],[21.5,0,21]]},
 'ASYLUM':{'L22':[[30.5,0,54],[32,0,54],[32,0,52],[38,0,52]],'L23':[[38,0,52],[39.5,0,52],[39.5,0,56],[41,0,56]],'L24':[[30.5,0,54],[27.5,0,54],[27.5,0,31]]}}

def selected_wall(o,site,schedule):
 # Headers lack zone extras in the original blockout. Match the original
 # explicit wall/side identity through later flat/profile repair prefixes.
 for zone,faces in schedule.items():
  sides=faces.keys() if zone in FACE_ONLY[site] else ['N','S','E','W']
  for side in sides:
   for prefix in ['Wall ','Header ']:
    needle=prefix+zone+' '+side
    if needle in o.name:
     suffix=o.name.split(needle,1)[1]
     if not suffix or suffix[0] in '. ':return True
 return False

WALL_ROLES={'rough shell wall','rough opening header','deliberate entrance wall','aligned service-aisle wall','deliberate courtyard doorway'}
def frame_template():
 before=set(bpy.context.scene.objects);bpy.ops.import_scene.gltf(filepath=str(MODEL));source=next(o for o in set(bpy.context.scene.objects)-before if o.type=='MESH');template=source.copy();template.data=source.data.copy();template.name='Measured indexed jamb and header template';bpy.context.collection.objects.link(template)
 # Select the original three catalog boxes, preserving their faces, materials, UVs and pivot.
 boxes=[(-.62,-.50,-.09,.09,0,2.16),(.50,.62,-.09,.09,0,2.16),(-.62,.62,-.10,.10,2.125,2.275)];bm=bmesh.new();bm.from_mesh(template.data)
 reject=[]
 for f in bm.faces:
  ps=[template.matrix_world@v.co for v in f.verts]
  if not any(all(a-1e-5<=p.x<=b+1e-5 and c-1e-5<=p.y<=d+1e-5 and e-1e-5<=p.z<=g+1e-5 for p in ps) for a,b,c,d,e,g in boxes):reject.append(f)
 bmesh.ops.delete(bm,geom=reject,context='FACES');loose=[v for v in bm.verts if not v.link_faces]
 if loose:bmesh.ops.delete(bm,geom=loose,context='VERTS')
 bm.to_mesh(template.data);bm.free();assert len(template.data.polygons)==36,(len(template.data.polygons),'Expected three original closed frame boxes')
 for o in set(bpy.context.scene.objects)-before:
  if o!=template:bpy.data.objects.remove(o,do_unlink=True)
 return template

def build(s):
 site=s['id'];schedule=SCHEDULE[site];folder=OUT/site.lower();folder.mkdir(parents=True,exist_ok=True);source=ROOT/s['glb'];native=ROOT/s['native'];meta=json.loads((ROOT/s['construction']).read_text());data=json.loads((ROOT/s['layout']).read_text());bpy.ops.wm.read_factory_settings(use_empty=True);bpy.context.preferences.filepaths.save_version=0;bpy.ops.import_scene.gltf(filepath=str(source));old=[o for o in bpy.context.scene.objects if o.type=='MESH'];removed=set();built=[];portals={}
 wallmat=next(o.data.materials[0] for o in old if o.get('role') in WALL_ROLES and o.data.materials)
 for o in old:
  if selected_wall(o,site,schedule) or o.get('role','').startswith('rough route support') or o.get('role') in ['structural-rooted-circulation-bed','structural-arrival-end-bearing']:removed.add(o.name)
 zones={z['id']:z for z in data['zones']}
 for zone,faces in schedule.items():
  z=zones[zone];a,b,c,d=z['bounds'];h=z['height'];assert z['y']==0
  for side,lo,hi,fixed in [('N',a,c,b),('S',a,c,d),('W',b+.24,d-.24,a),('E',b+.24,d-.24,c)]:
   if zone in FACE_ONLY[site] and side not in faces:continue
   def wall(start,end,bottom,top,label):
    if end-start<.000001:return
    footprint=[(start,fixed),(end,fixed),(end,fixed+.24),(start,fixed+.24)] if side=='N' else [(start,fixed-.24),(end,fixed-.24),(end,fixed),(start,fixed)] if side=='S' else [(fixed,start),(fixed+.24,start),(fixed+.24,end),(fixed,end)] if side=='W' else [(fixed-.24,start),(fixed,start),(fixed,end),(fixed-.24,end)]
    o=prism(site+' clean '+zone+' '+side+' '+label,footprint,top,[bottom]*4,wallmat);o['role']='authored doorway masonry';o['zone']=zone;built.append(o)
   cursor=lo
   for i,(center,kind) in enumerate(sorted(faces.get(side,[]))):
    width,head=TYPES[kind];outerhalf=.62*width;outerhead=head*2.275/2.125;left,right=center-outerhalf,center+outerhalf;assert lo+.05<left<right<hi-.05,(site,zone,side,'door too near corner');wall(cursor,left,0,h,'pier'+str(i));wall(left,right,outerhead,h,'lintel'+str(i));cursor=right
    axis='z' if side in ['N','S'] else 'x';key=(axis,fixed,center);span=[fixed,fixed+.24] if side in ['N','W'] else [fixed-.24,fixed]
    if key not in portals:portals[key]=dict(id=site+'-'+axis+'-'+str(fixed)+'-'+str(center),axis=axis,boundary=fixed,along=center,door_type=kind,clear_width=width,clear_height=head,sill_y=0,frame_span=span,rooms=[zone],side=side)
    else:
     p=portals[key];assert p['door_type']==kind,(site,key,'adjacent door types disagree');p['frame_span']=[min(p['frame_span'][0],span[0]),max(p['frame_span'][1],span[1])];p['rooms'].append(zone)
   wall(cursor,hi,0,h,'end')
 # The entry is a complete architectural landing, not a strip derived from a guide.
 terrain=next(o for o in old if o.get('role','').startswith('saved editor terrain'))
 tree=BVHTree.FromPolygons([terrain.matrix_world@v.co for v in terrain.data.vertices],[tuple(f.vertices) for f in terrain.data.polygons])
 a,b,c,d=PORCHES[site];footprint=[(a,b),(c,b),(c,d),(a,d)];receivers=[]
 for x,z in [(a+.1,d-.1),(c-.1,d-.1),((a+c)/2,d-.1),((a+c)/2,b+(d-b)*.67)]:
  hit=tree.ray_cast(Vector((x,-z,20)),Vector((0,0,-1)),100)[0]
  assert hit is not None,(site,'No actual soil beneath entrance landing',x,z)
  receivers.append(dict(x=x,z=z,soil_y=hit.z))
 bottom=min(p['soil_y'] for p in receivers)-.35
 assert bottom<-.25,(site,'Landing plinth must have real depth')
 landing=prism(site+' architectural entrance landing and soil-bearing plinth',footprint,0,[bottom]*4,wallmat);landing['role']='soil-bearing entrance landing';landing['support']='Foundation below sampled original saved terrain; footprint belongs to entrance block';built.append(landing)
 template=frame_template()
 for p in portals.values():
  o=template.copy();o.data=template.data.copy();o.name=p['id']+' indexed jambs and head';bpy.context.collection.objects.link(o);depth=p['frame_span'][1]-p['frame_span'][0];middle=sum(p['frame_span'])/2;side=p['side']
  for v in o.data.vertices:
   original=template.matrix_world@v.co;u=original.x*p['clear_width'];normal=original.y*depth/.18;y=original.z*p['clear_height']/2.125;center=p['along']
   if side=='N':v.co=(center+u,-middle+normal,y)
   elif side=='S':v.co=(center-u,-middle-normal,y)
   elif side=='W':v.co=(middle-normal,-center+u,y)
   else:v.co=(middle+normal,-center-u,y)
  o.matrix_world.identity();o['role']='fitted indexed doorway jambs and head';o['source_asset']='level_blockout_doorway';o['source_asset_sha256']=sha(MODEL);o['portal_id']=p['id'];o['clear_width']=p['clear_width'];o['clear_height']=p['clear_height'];o['sill_y']=0;built.append(o);p['frame_depth']=depth;p['frame_center']=[p['along'],0,middle] if p['axis']=='z' else [middle,0,p['along']];p['boundary_center']=[p['along'],0,p['boundary']] if p['axis']=='z' else [p['boundary'],0,p['along']]
 bpy.data.objects.remove(template,do_unlink=True)
 for o in old:
  if o.name in removed:bpy.data.objects.remove(o,do_unlink=True)
 for o in built:
  bm=bmesh.new();bm.from_mesh(o.data);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=.000001);bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces));assert all(e.is_manifold for e in bm.edges) and bm.calc_volume(signed=True)>0,(site,o.name,'open or inverted');bm.to_mesh(o.data);bm.free()
 for link in data['links']:
  if link['id'] in GUIDES.get(site,{}):link['points']=GUIDES[site][link['id']]
 data['authored_doorways']=list(portals.values());data['doorway_types']={k:dict(clear_width=v[0],clear_height=v[1],sill_y=0) for k,v in TYPES.items()};data['doorway_notes']='Explicit architectural door schedule. Original indexed frame jambs/header only; flush floor, no raised threshold. Door leaf/hinge/service binding remains runtime work. Existing navigation widths describe broader spaces; door clearance is the authored portal width.'
 layout=folder/'authored-layout.json';write(layout,data);bpy.ops.wm.save_as_mainfile(filepath=str(folder/'clean-doorways.blend'),compress=True);bpy.ops.object.select_all(action='DESELECT')
 for o in built:o.select_set(True)
 add=ROOT/'build'/('doorway-addition-'+site+'.glb');bpy.ops.export_scene.gltf(filepath=str(add),export_format='GLB',export_extras=True,use_selection=True);append(source,add,folder/'clean-doorways.glb',remove_mesh_names=sorted(removed));parts=native_write(native,folder/'clean-doorways.hcs2',removed,built)
 bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(folder/'clean-doorways.glb'));objs={o.name:o for o in bpy.context.scene.objects if o.type=='MESH'}
 for name,ts in parts:assert face_keys(records(objs[name]))==face_keys(ts)
 # Inspect final source: no old fragmented wall/header meshes survive selected rooms.
 assert not any(selected_wall(o,site,schedule) or o.get('role','').startswith('rough route support') or o.get('role') in ['structural-rooted-circulation-bed','structural-arrival-end-bearing'] for o in objs.values())
 for o in objs.values():
  if o.get('role')=='enclosed pitched roof' or o.get('role')=='ceiling-cap' and o.get('zone') not in ['tunnel','tunnel-n','undercroft']:o.hide_render=True
 scene=bpy.context.scene;scene.render.engine='BLENDER_WORKBENCH';scene.display.shading.color_type='MATERIAL';scene.display.shading.show_cavity=True;scene.render.resolution_x=1400;scene.render.resolution_y=1100;scene.render.resolution_percentage=100;scene.world=bpy.data.worlds.new('Door construction review');bpy.ops.object.camera_add();cam=bpy.context.object;scene.camera=cam;a,b,c,d=data['site_bounds'];span=max(c-a,d-b);target=Vector(((a+c)/2,-(b+d)/2,0));cam.location=target+Vector((span*.45,-span*.6,span));cam.rotation_euler=(target-cam.location).to_track_quat('-Z','Y').to_euler();cam.data.type='ORTHO';cam.data.ortho_scale=span*1.15;cap=BASE/'captures/institution-doorway-batch'/(site.lower()+'.png');cap.parent.mkdir(parents=True,exist_ok=True);scene.render.filepath=str(cap);bpy.ops.render.render(write_still=True)
 # Standing eye-level view of the institutional entrance, looking inward.
 p=next(p for p in portals.values() if len(p['rooms'])==1 and p['side']=='S')
 target=Vector((p['along'],-p['boundary'],1.15));cam.location=Vector((p['along']+1.8,-p['boundary']-5,1.7));cam.rotation_euler=(target-cam.location).to_track_quat('-Z','Y').to_euler();cam.data.ortho_scale=4.5;scene.render.resolution_x=1100;scene.render.resolution_y=1000;close=cap.with_name(site.lower()+'-entry-close.png');scene.render.filepath=str(close);bpy.ops.render.render(write_still=True)
 meta.update(status='ARCHITECTURAL_DOORWAYS_REBUILT_RUNTIME_OPEN',source_contract=s['construction'],entrance_landing=dict(bounds=list(PORCHES[site]),top_y=0,foundation_bottom_y=bottom,soil_receivers=receivers),study_solid_removal=[o for o in sorted(removed) if 'Wall ' not in o and 'Header ' not in o],source_glb=s['glb'],source_glb_sha256=s['glb_sha256'],source_physics=s['native'],source_physics_sha256=s['native_sha256'],before_layout=s['layout'],before_layout_sha256=sha(ROOT/s['layout']),before_segment_map=meta.get('source_segment_map',{}),before_stair_components=meta['stair_components'],stair_components=[n for n in meta['stair_components'] if n.split(':',1)[-1] not in removed],layout=layout.relative_to(ROOT).as_posix(),layout_sha256=sha(layout),glb_sha256=sha(folder/'clean-doorways.glb'),physics_sha256=sha(folder/'clean-doorways.hcs2'),blend_sha256=sha(folder/'clean-doorways.blend'),doorways=list(portals.values()),door_types=data['doorway_types'],doorway_builder='tools/build_institution_doorways.py',doorway_builder_sha256=sha(Path(__file__)),doorframe_source=MODEL.relative_to(ROOT).as_posix(),doorframe_source_sha256=sha(MODEL),doorframe_parts='Original catalog left jamb, right jamb and header; source master/UVs/materials preserved. Threshold/hardware studies retained in library, not placed as a blocked/raised opening.',rooms_rebuilt=[z for z in schedule if z not in FACE_ONLY[site]],corridor_faces={z:list(schedule[z]) for z in FACE_ONLY[site]},sealed_context_policy='Existing SEALED_CONTEXT walls/access metadata retained; no new context-room openings',removed_fragmented_walls=sorted(removed),captures=[dict(path=cap.relative_to(ROOT).as_posix(),sha256=sha(cap),type='native exported doorway construction with roof cutaway, not gameplay')]);write(folder/'construction.json',meta);print('DOORWAYS_CONSTRUCTED',site,len(portals),flush=True)
 return dict(id=site,construction=(folder/'construction.json').relative_to(ROOT).as_posix(),layout=layout.relative_to(ROOT).as_posix(),glb=(folder/'clean-doorways.glb').relative_to(ROOT).as_posix(),native=(folder/'clean-doorways.hcs2').relative_to(ROOT).as_posix(),blend=(folder/'clean-doorways.blend').relative_to(ROOT).as_posix(),glb_sha256=meta['glb_sha256'],native_sha256=meta['physics_sha256'])
def main():
 r=json.loads((BASE/'doorway-construction-pass/roster.json').read_text());rows=[build(s) if s['id'] in SCHEDULE else s for s in r['active_sources']];write(OUT/'roster.json',dict(active_sources=rows,sites=[dict(id=s['id'],decision='GROUND_ROOM_DOORWAYS_REBUILT_RUNTIME_OPEN' if s['id'] in SCHEDULE else s['decision'],doorway_cleanup='ENTRANCE_BLOCK_CONSTRUCTED_REMAINING_ROOMS_OPEN' if s['id'] in SCHEDULE else s['doorway_cleanup']) for s in r['sites']],baseline='development/levels/rebuild/doorway-construction-pass/roster.json'));print('DOORWAY_CONSTRUCTION_BATCH_COMPLETE',flush=True)
if __name__=='__main__':main()
