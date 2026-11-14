"""Rebuild ground-floor doorway shells from explicit architectural door schedules."""
from pathlib import Path
import sys,json,math,copy
import bpy,bmesh
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[1];sys.path[:0]=[str(ROOT/'tools'),str(ROOT/'development/levels/blockouts')]
from append_static_glb import append
from build_circulation_construction import native_write
from build_roster_foundations import prism
from review_roster_macroterrain import records,sha
from export_roster_collision import face_keys
from planar_structure import plate
from build_roster_floor_closures import difference
from build_three_stair_cells import wall_identity
from build_cinder_galleries import construct_galleries
from build_deliberate_entrances import write
BASE=ROOT/'development/levels/rebuild';OUT=BASE/'service-portal-construction-pass';MODEL=ROOT/'development/levels/primitive-props-v1/models/level_blockout_doorway.glb'
TYPES={'pedestrian':(1.05,2.10),'service':(1.80,2.30),'loading':(2.40,2.60)}
# Positions are door datums on actual wall faces, not swept path intersections.
SCHEDULE={
'WLD-12':{'signal':{'N':[(14,'pedestrian')],'S':[(20,'pedestrian')],'E':[(69,'pedestrian')]},'coach-a':{'W':[(54,'pedestrian')],'E':[(54,'pedestrian')]},'coach-b':{'W':[(54,'pedestrian')],'E':[(54,'pedestrian')]},'store':{'S':[(58,'service')]},'tunnel':{'S':[(34,'service')],'E':[(6,'service')]},'refuge':{'W':[(6,'service')],'S':[(46,'pedestrian')]}},
'WLD-16':{'office':{'N':[(18,'pedestrian')],'S':[(18,'pedestrian')],'W':[(45,'pedestrian')]},'manager':{'E':[(45,'pedestrian')]},'laundry':{'W':[(43,'service')],'S':[(42.5,'service')]},'plant':{'N':[(42.5,'service')],'E':[(49,'service')]},**{'room'+str(i):{'E' if i<=4 else 'W':[(22.5+5*((i-1)%4),'pedestrian')]} for i in range(1,9)}},
'WLD-18':{'servicehouse':{'S':[(19,'pedestrian')],'W':[(56,'pedestrian')]},'head':{'S':[(34,'service')],'E':[(48,'service')],'W':[(48,'service')]},'shop':{'W':[(48,'service')],'S':[(47,'service')]},'exit':{'E':[(56,'pedestrian')]},'west':{'E':[(28,'service')]},'exhibit':{'W':[(28,'service')]},'east':{'W':[(28,'service')]},'crew':{'E':[(28,'service')]}}}
PARTIAL={'WLD-12':set(),'WLD-16':set(),'WLD-18':{'exit','west','exhibit','east','crew'}}
GUIDES={
'WLD-12':{'L04':[[36,0,82],[57,0,82],[57,0,53]],'L02':[[36,0,82],[20,0,82],[20,0,69]],'L05':[[34,0,59],[30,0,59],[30,0,54],[20,0,54]],'L08':[[34,0,59],[38,0,59],[38,0,54],[48,0,54]],'L14':[[51,0,17],[48,0,17],[48,0,34],[57,0,34],[57,0,53]],'L15':[[57,0,53],[58,0,53],[58,0,23]]},
'WLD-16':{'route-02':[[26,0,57],[18,0,57],[18,0,46]],'route-05':[[18,0,46],[18,0,45],[9.5,0,45]],'route-04':[[18,0,46],[18,0,41],[14.5,0,41],[14.5,0,34]],'route-10':[[37.5,0,34],[37.5,0,43],[42.5,0,43]],'route-12':[[42.5,0,49],[51,0,49],[51,0,37]]},
'WLD-18':{'route-02':[[32,0,77],[19,0,77],[19,0,62]],'route-04':[[40,0,62],[34,0,62],[34,0,48]],'route-05':[[40,0,62],[47,0,62],[47,0,48]],'route-09':[[7,0,52],[8.8,0,52],[8.8,0,56],[19,0,56],[19,0,62]],'route-11':[[18,-4,32],[20.5,-4,32],[20.5,-4,28],[23,-4,28]],'route-12':[[41,-4,28],[42.5,-4,28],[42.5,-4,26.5],[48,-4,26.5]],'route-13':[[36,0,48],[52,0,48],[52,0,47.2],[52,-4,40.5],[52,-4,37],[54,-4,37],[54,-4,26.5],[48,-4,26.5]],'route-14':[[48,-4,26.5],[51,-4,26.5],[51,-4,28],[51,-6,34],[51,-6,35],[45,-6,35],[45,-6,34],[45,-8,28],[45,-8,26.5],[48,-8,26.5],[48,-8,28]],'route-15':[[48,-8,28],[48,-8,26],[42.5,-8,26],[42.5,-8,28],[41,-8,28]],'route-19':[[49,-8,28],[49,-8,26],[54,-8,26],[54,-8,37],[49,-8,37],[49,-8,45]],'route-16':[[23,-8,28],[20.5,-8,28],[20.5,-8,32],[18,-8,32]]}}

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
  if wall_identity(o,schedule,PARTIAL[site]):removed.add(o.name)
  if site=='WLD-16' and (o.get('role','').startswith('rough route support') or o.get('role') in ['structural-rooted-circulation-bed','structural-arrival-end-bearing']):removed.add(o.name)
  if site=='WLD-18' and o.get('link')=='route-13' and o.get('role')=='structural-stair-landing':removed.add(o.name)
  if site=='WLD-18' and (o.get('zone') in ['exhibit','west','shop'] and o.get('role')=='floor' or o.get('zone')=='east' and o.get('role')=='ceiling-cap' or (o.get('link')=='route-14' or 'route-14' in o.name) and any(k in o.get('role','') for k in ['stair','landing','guard','stringer'])):removed.add(o.name)
 zones={z['id']:z for z in data['zones']}
 for zone,faces in schedule.items():
  z=zones[zone];a,b,c,d=z['bounds'];h=z['height'];y0=z['y']
  for side,lo,hi,fixed in [('N',a,c,b),('S',a,c,d),('W',b if zone in PARTIAL[site] else b+.24,d if zone in PARTIAL[site] else d-.24,a),('E',b if zone in PARTIAL[site] else b+.24,d if zone in PARTIAL[site] else d-.24,c)]:
   if zone in PARTIAL[site] and side not in faces:continue
   def wall(start,end,bottom,top,label):
    if end-start<.000001:return
    footprint=[(start,fixed),(end,fixed),(end,fixed+.24),(start,fixed+.24)] if side=='N' else [(start,fixed-.24),(end,fixed-.24),(end,fixed),(start,fixed)] if side=='S' else [(fixed,start),(fixed+.24,start),(fixed+.24,end),(fixed,end)] if side=='W' else [(fixed-.24,start),(fixed,start),(fixed,end),(fixed-.24,end)]
    o=prism(site+' clean '+zone+' '+side+' '+label,footprint,y0+top,[y0+bottom]*4,wallmat);o['role']='authored doorway masonry';o['zone']=zone;built.append(o)
   cursor=lo
   for i,(center,kind) in enumerate(sorted(faces.get(side,[]))):
    width,head=TYPES[kind];outerhalf=.62*width;outerhead=head*2.275/2.125;left,right=center-outerhalf,center+outerhalf;assert lo+.05<left<right<hi-.05,(site,zone,side,'door too near corner');wall(cursor,left,0,h,'pier'+str(i));wall(left,right,outerhead,h,'lintel'+str(i));cursor=right
    axis='z' if side in ['N','S'] else 'x';key=(axis,fixed,center,y0);span=[fixed,fixed+.24] if side in ['N','W'] else [fixed-.24,fixed]
    if key not in portals:portals[key]=dict(id=site+'-'+axis+'-'+str(fixed)+'-'+str(center)+'-y'+str(y0),axis=axis,boundary=fixed,along=center,door_type=kind,clear_width=width,clear_height=head,sill_y=y0,frame_span=span,rooms=[zone],side=side)
    else:
     p=portals[key];assert p['door_type']==kind,(site,key,'adjacent door types disagree');p['frame_span']=[min(p['frame_span'][0],span[0]),max(p['frame_span'][1],span[1])];p['rooms'].append(zone)
   wall(cursor,hi,0,h,'end')
 template=frame_template()
 for p in portals.values():
  o=template.copy();o.data=template.data.copy();o.name=p['id']+' indexed jambs and head';bpy.context.collection.objects.link(o);depth=p['frame_span'][1]-p['frame_span'][0];middle=sum(p['frame_span'])/2;side=p['side']
  for v in o.data.vertices:
   original=template.matrix_world@v.co;u=original.x*p['clear_width'];normal=original.y*depth/.18;y=p['sill_y']+original.z*p['clear_height']/2.125;center=p['along']
   if side=='N':v.co=(center+u,-middle+normal,y)
   elif side=='S':v.co=(center-u,-middle-normal,y)
   elif side=='W':v.co=(middle-normal,-center+u,y)
   else:v.co=(middle+normal,-center-u,y)
  o.matrix_world.identity();o['role']='fitted indexed doorway jambs and head';o['source_asset']='level_blockout_doorway';o['source_asset_sha256']=sha(MODEL);o['portal_id']=p['id'];o['clear_width']=p['clear_width'];o['clear_height']=p['clear_height'];o['sill_y']=p['sill_y'];built.append(o);p['frame_depth']=depth;p['frame_center']=[p['along'],p['sill_y'],middle] if p['axis']=='z' else [middle,p['sill_y'],p['along']];p['boundary_center']=[p['along'],p['sill_y'],p['boundary']] if p['axis']=='z' else [p['boundary'],p['sill_y'],p['along']]
 bpy.data.objects.remove(template,do_unlink=True)
 new_steps=[]
 if site=='WLD-18':new_steps=construct_galleries(built,wallmat)
 for link in data['links']:
  if link['id'] in GUIDES[site]:link['points']=GUIDES[site][link['id']]
 for o in old:
  if o.name in removed:bpy.data.objects.remove(o,do_unlink=True)
 for o in built:
  bm=bmesh.new();bm.from_mesh(o.data);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=.000001);bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces));assert all(e.is_manifold for e in bm.edges) and bm.calc_volume(signed=True)>0,(site,o.name,'open or inverted');bm.to_mesh(o.data);bm.free()
 retained_portals=meta.get('doorways',[]);data['authored_doorways']=retained_portals+list(portals.values());data['doorway_types']={k:dict(clear_width=v[0],clear_height=v[1],sill_y=0) for k,v in TYPES.items()};data['doorway_notes']='Explicit architectural door schedule. Original indexed frame jambs/header only; flush floor, no raised threshold. Door leaf/hinge/service binding remains runtime work. Existing navigation widths describe broader spaces; door clearance is the authored portal width.'

 if site=='WLD-18':next(z for z in data['zones'] if z['id']=='exhibit')['label_point']=[48,-4,26.5]
 layout=folder/'authored-layout.json';write(layout,data);bpy.ops.wm.save_as_mainfile(filepath=str(folder/'clean-doorways.blend'),compress=True);bpy.ops.object.select_all(action='DESELECT')
 for o in built:o.select_set(True)
 add=ROOT/'build'/('doorway-addition-'+site+'.glb');bpy.ops.export_scene.gltf(filepath=str(add),export_format='GLB',export_extras=True,use_selection=True);append(source,add,folder/'clean-doorways.glb',remove_mesh_names=sorted(removed));parts=native_write(native,folder/'clean-doorways.hcs2',removed,built)
 bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(folder/'clean-doorways.glb'));objs={o.name:o for o in bpy.context.scene.objects if o.type=='MESH'}
 for name,ts in parts:assert face_keys(records(objs[name]))==face_keys(ts)
 # Inspect final source: no old fragmented wall/header meshes survive selected rooms.
 assert not any(wall_identity(o,schedule,PARTIAL[site]) for o in objs.values())
 for o in objs.values():
  if o.get('role')=='enclosed pitched roof' or o.get('role')=='ceiling-cap' and o.get('zone') not in ['tunnel','tunnel-n','undercroft']:o.hide_render=True
 scene=bpy.context.scene;scene.render.engine='BLENDER_WORKBENCH';scene.display.shading.color_type='MATERIAL';scene.display.shading.show_cavity=True;scene.render.resolution_x=1400;scene.render.resolution_y=1100;scene.render.resolution_percentage=100;scene.world=bpy.data.worlds.new('Door construction review');bpy.ops.object.camera_add();cam=bpy.context.object;scene.camera=cam;a,b,c,d=data['site_bounds'];span=max(c-a,d-b);target=Vector(((a+c)/2,-(b+d)/2,0));cam.location=target+Vector((span*.45,-span*.6,span));cam.rotation_euler=(target-cam.location).to_track_quat('-Z','Y').to_euler();cam.data.type='ORTHO';cam.data.ortho_scale=span*1.15;cap=BASE/'captures/service-portal-construction-batch'/(site.lower()+'.png');cap.parent.mkdir(parents=True,exist_ok=True);scene.render.filepath=str(cap);bpy.ops.render.render(write_still=True)
 meta.update(stair_components=[n for n in meta['stair_components'] if n.split(':',1)[-1] not in removed]+new_steps,gallery_builder='tools/build_cinder_galleries.py',gallery_builder_sha256=sha(ROOT/'tools/build_cinder_galleries.py'),status='ARCHITECTURAL_DOORWAYS_REBUILT_RUNTIME_OPEN',source_contract=s['construction'],source_glb=s['glb'],source_glb_sha256=s['glb_sha256'],source_physics=s['native'],source_physics_sha256=s['native_sha256'],before_layout=s['layout'],before_layout_sha256=sha(ROOT/s['layout']),before_segment_map=meta.get('source_segment_map',{}),before_stair_components=meta['stair_components'],layout=layout.relative_to(ROOT).as_posix(),layout_sha256=sha(layout),glb_sha256=sha(folder/'clean-doorways.glb'),physics_sha256=sha(folder/'clean-doorways.hcs2'),blend_sha256=sha(folder/'clean-doorways.blend'),doorways=data['authored_doorways'],door_types=data['doorway_types'],doorway_builder='tools/build_service_portals.py',doorway_builder_sha256=sha(Path(__file__)),doorframe_source=MODEL.relative_to(ROOT).as_posix(),doorframe_source_sha256=sha(MODEL),doorframe_parts='Original catalog left jamb, right jamb and header; source master/UVs/materials preserved. Threshold/hardware studies retained in library, not placed as a blocked/raised opening.',removed_fragmented_walls=sorted(removed),captures=[dict(path=cap.relative_to(ROOT).as_posix(),sha256=sha(cap),type='native exported doorway construction with roof cutaway, not gameplay')]);
 if site=='WLD-18':meta['source_segment_map']=dict(meta.get('source_segment_map',{}),**{'route-14':{'2':1,'6':3}})
 write(folder/'construction.json',meta);print('DOORWAYS_CONSTRUCTED',site,len(portals),flush=True)
 return dict(id=site,construction=(folder/'construction.json').relative_to(ROOT).as_posix(),layout=layout.relative_to(ROOT).as_posix(),glb=(folder/'clean-doorways.glb').relative_to(ROOT).as_posix(),native=(folder/'clean-doorways.hcs2').relative_to(ROOT).as_posix(),blend=(folder/'clean-doorways.blend').relative_to(ROOT).as_posix(),glb_sha256=meta['glb_sha256'],native_sha256=meta['physics_sha256'])
def main():
 r=json.loads((BASE/'arrival-grounds-construction-pass/roster.json').read_text());rows=[build(s) if s['id'] in SCHEDULE else s for s in r['active_sources']];write(OUT/'roster.json',dict(active_sources=rows,sites=[dict(s,decision='GROUND_ROOM_DOORWAYS_REBUILT_RUNTIME_OPEN' if s['id'] in SCHEDULE else s['decision'],doorway_cleanup='CONSTRUCTED_REVIEW_OPEN' if s['id'] in SCHEDULE else s.get('doorway_cleanup','PRESERVE_MATURE_AUTHORITY' if s['id'] not in {a['id'] for a in r['active_sources']} else 'PENDING_INDIVIDUAL_CONSTRUCTION')) for s in r['sites']],baseline='development/levels/rebuild/arrival-grounds-construction-pass/roster.json'));print('DOORWAY_CONSTRUCTION_BATCH_COMPLETE',flush=True)
if __name__=='__main__':main()
