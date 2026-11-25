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
from build_deliberate_entrances import write
BASE=ROOT/'development/levels/rebuild';OUT=BASE/'doorway-construction-pass';MODEL=ROOT/'development/levels/primitive-props-v1/models/level_blockout_doorway.glb'
TYPES={'pedestrian':(1.05,2.10),'service':(1.80,2.30),'loading':(2.40,2.60)}
# Positions are door datums on actual wall faces, not swept path intersections.
SCHEDULE={
'WLD-06':{'cottage-a':{'S':[(12,'pedestrian')],'E':[(48,'pedestrian')]},'cottage-b':{'S':[(52,'pedestrian')],'W':[(48,'pedestrian')]},'boathouse':{'E':[(19,'loading')],'S':[(16,'service')]},'net':{'W':[(17,'service')]},'fish':{'N':[(32,'loading')]}},
'WLD-08':{'generator':{'E':[(19,'pedestrian')],'S':[(7,'service')]},'lab':{'N':[(7,'service')],'E':[(41,'pedestrian')],'S':[(11,'service')]},'store':{'N':[(11,'service')],'S':[(11,'service')]},'radio':{'W':[(19,'pedestrian')],'E':[(19,'pedestrian')],'S':[(26,'pedestrian')]},'records':{'W':[(19,'pedestrian')],'E':[(16,'service')],'S':[(38,'pedestrian')]},'dorm':{'N':[(53,'pedestrian')],'W':[(41,'pedestrian')],'S':[(53,'pedestrian')]},'mess':{'N':[(53,'pedestrian')],'S':[(53,'pedestrian')]},'shelter':{'W':[(14,'pedestrian')]}},
'WLD-13':{'processing':{'N':[(30,'pedestrian'),(42,'service')],'S':[(36,'loading')],'W':[(35,'service')],'E':[(35,'loading')]},'cold':{'W':[(35,'loading')],'S':[(56,'service')]},'boiler':{'E':[(35,'service')],'W':[(35,'service')],'S':[(16,'service')]},'wash':{'N':[(16,'service')],'S':[(16,'service')],'W':[(50,'pedestrian')]},'office':{'N':[(30,'pedestrian')],'S':[(30,'pedestrian')],'E':[(20,'pedestrian')]},'stores':{'N':[(42,'loading')],'S':[(42,'service')],'W':[(20,'pedestrian')]},'bunk':{'N':[(0,'pedestrian')],'E':[(50,'pedestrian')],'W':[(53,'pedestrian')]},'mess':{'S':[(0,'pedestrian')],'E':[(35,'service')],'W':[(35,'pedestrian')]}}}
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
  if o.get('zone') in schedule and (o.get('role') in WALL_ROLES or o.name.startswith('Header ')):removed.add(o.name)
 zones={z['id']:z for z in data['zones']}
 for zone,faces in schedule.items():
  z=zones[zone];a,b,c,d=z['bounds'];h=z['height'];assert z['y']==0
  for side,lo,hi,fixed in [('N',a,c,b),('S',a,c,d),('W',b+.24,d-.24,a),('E',b+.24,d-.24,c)]:
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
 if site=='WLD-06':
  next(l for l in data['links'] if l['id']=='L11')['points']=[[22,0,23],[22,0,19],[20,0,19],[12,0,19]];next(l for l in data['links'] if l['id']=='L12')['points']=[[42,0,23],[42,0,17],[44,0,17],[50,0,17]]
 data['authored_doorways']=list(portals.values());data['doorway_types']={k:dict(clear_width=v[0],clear_height=v[1],sill_y=0) for k,v in TYPES.items()};data['doorway_notes']='Explicit architectural door schedule. Original indexed frame jambs/header only; flush floor, no raised threshold. Door leaf/hinge/service binding remains runtime work. Existing navigation widths describe broader spaces; door clearance is the authored portal width.'
 layout=folder/'authored-layout.json';write(layout,data);bpy.ops.wm.save_as_mainfile(filepath=str(folder/'clean-doorways.blend'),compress=True);bpy.ops.object.select_all(action='DESELECT')
 for o in built:o.select_set(True)
 add=ROOT/'build'/('doorway-addition-'+site+'.glb');bpy.ops.export_scene.gltf(filepath=str(add),export_format='GLB',export_extras=True,use_selection=True);append(source,add,folder/'clean-doorways.glb',remove_mesh_names=sorted(removed));parts=native_write(native,folder/'clean-doorways.hcs2',removed,built)
 bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(folder/'clean-doorways.glb'));objs={o.name:o for o in bpy.context.scene.objects if o.type=='MESH'}
 for name,ts in parts:assert face_keys(records(objs[name]))==face_keys(ts)
 # Inspect final source: no old fragmented wall/header meshes survive selected rooms.
 assert not any(o.get('zone') in schedule and o.get('role') in WALL_ROLES for o in objs.values())
 for o in objs.values():
  if o.get('role')=='enclosed pitched roof' or o.get('role')=='ceiling-cap' and o.get('zone') not in ['tunnel','tunnel-n','undercroft']:o.hide_render=True
 scene=bpy.context.scene;scene.render.engine='BLENDER_WORKBENCH';scene.display.shading.color_type='MATERIAL';scene.display.shading.show_cavity=True;scene.render.resolution_x=1400;scene.render.resolution_y=1100;scene.render.resolution_percentage=100;scene.world=bpy.data.worlds.new('Door construction review');bpy.ops.object.camera_add();cam=bpy.context.object;scene.camera=cam;a,b,c,d=data['site_bounds'];span=max(c-a,d-b);target=Vector(((a+c)/2,-(b+d)/2,0));cam.location=target+Vector((span*.45,-span*.6,span));cam.rotation_euler=(target-cam.location).to_track_quat('-Z','Y').to_euler();cam.data.type='ORTHO';cam.data.ortho_scale=span*1.15;cap=BASE/'captures/doorway-construction-batch'/(site.lower()+'.png');cap.parent.mkdir(parents=True,exist_ok=True);scene.render.filepath=str(cap);bpy.ops.render.render(write_still=True)
 meta.update(status='ARCHITECTURAL_DOORWAYS_REBUILT_RUNTIME_OPEN',source_contract=s['construction'],source_glb=s['glb'],source_glb_sha256=s['glb_sha256'],source_physics=s['native'],source_physics_sha256=s['native_sha256'],before_layout=s['layout'],before_layout_sha256=sha(ROOT/s['layout']),before_segment_map=meta.get('source_segment_map',{}),before_stair_components=meta['stair_components'],layout=layout.relative_to(ROOT).as_posix(),layout_sha256=sha(layout),glb_sha256=sha(folder/'clean-doorways.glb'),physics_sha256=sha(folder/'clean-doorways.hcs2'),blend_sha256=sha(folder/'clean-doorways.blend'),doorways=list(portals.values()),door_types=data['doorway_types'],doorway_builder='tools/build_architectural_doorways.py',doorway_builder_sha256=sha(Path(__file__)),doorframe_source=MODEL.relative_to(ROOT).as_posix(),doorframe_source_sha256=sha(MODEL),doorframe_parts='Original catalog left jamb, right jamb and header; source master/UVs/materials preserved. Threshold/hardware studies retained in library, not placed as a blocked/raised opening.',removed_fragmented_walls=sorted(removed),captures=[dict(path=cap.relative_to(ROOT).as_posix(),sha256=sha(cap),type='native exported doorway construction with roof cutaway, not gameplay')]);write(folder/'construction.json',meta);print('DOORWAYS_CONSTRUCTED',site,len(portals),flush=True)
 return dict(id=site,construction=(folder/'construction.json').relative_to(ROOT).as_posix(),layout=layout.relative_to(ROOT).as_posix(),glb=(folder/'clean-doorways.glb').relative_to(ROOT).as_posix(),native=(folder/'clean-doorways.hcs2').relative_to(ROOT).as_posix(),blend=(folder/'clean-doorways.blend').relative_to(ROOT).as_posix(),glb_sha256=meta['glb_sha256'],native_sha256=meta['physics_sha256'])
def main():
 r=json.loads((BASE/'wharf-stair-cells-pass/roster.json').read_text());rows=[build(s) if s['id'] in SCHEDULE else s for s in r['active_sources']];write(OUT/'roster.json',dict(active_sources=rows,sites=[dict(id=s['id'],decision='GROUND_ROOM_DOORWAYS_REBUILT_RUNTIME_OPEN' if s['id'] in SCHEDULE else s['decision'],doorway_cleanup='CONSTRUCTED_REVIEW_OPEN' if s['id'] in SCHEDULE else 'PRESERVE_MATURE_AUTHORITY' if s['id'] not in {a['id'] for a in r['active_sources']} else 'PENDING_INDIVIDUAL_CONSTRUCTION') for s in r['sites']],baseline='development/levels/rebuild/wharf-stair-cells-pass/roster.json'));print('DOORWAY_CONSTRUCTION_BATCH_COMPLETE',flush=True)
if __name__=='__main__':main()
