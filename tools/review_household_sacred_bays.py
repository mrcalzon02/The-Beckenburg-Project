"""Independent final-export geometry checks and ordinary standing-height views."""
from pathlib import Path
import json,sys
import bpy,bmesh
from mathutils import Vector
from mathutils.bvhtree import BVHTree
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
from append_static_glb import read
from compose_room_terrain import split_native
from review_roster_macroterrain import records,sha
from export_roster_collision import face_keys
from check_roster_decks import read_parts
from build_deliberate_entrances import write
B=ROOT/'development/levels/rebuild';O=B/'household-sacred-bay-pass';CAP=B/'captures/household-sacred-bay-batch';captures=[];proof=[]
for site in ['FARMHOUSE','MANSION','CHURCH']:
 out=O/site.lower();m=json.loads((out/'construction.json').read_text());glb=out/'authored-bays.glb';native=out/'authored-bays.hcs2'
 assert sha(glb)==m['glb_sha256'] and sha(native)==m['physics_sha256'] and sha(ROOT/m['layout'])==m['layout_sha256']
 a,blob=read(ROOT/m['source_glb']);b,after=read(glb);assert after[:len(blob)]==blob
 for key in ['meshes','accessors','bufferViews','images','textures','materials']:assert b.get(key,[])[:len(a.get(key,[]))]==a.get(key,[])
 removed=set(m['removed_source_meshes'])
 for i,n in enumerate(a['nodes']):
  expected=dict(n)
  if n.get('name') in removed:expected.pop('mesh',None)
  assert b['nodes'][i]==expected,(site,'Changed retained source node',i)
 old=split_native(ROOT/m['source_physics']);new=split_native(native);keep=[v for v in old if v[0].split(':',1)[-1] not in removed];assert new[:len(keep)]==keep
 old_terrain=[v for v in old if 'terrain' in v[0].lower()];new_terrain=[v for v in new if 'terrain' in v[0].lower()]
 assert old_terrain and old_terrain==new_terrain,(site,'Saved terrain owner changed or missing')
 bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(glb));objs={o.name:o for o in bpy.context.scene.objects if o.type=='MESH'};parts=read_parts(native);treads=0
 for name in m['new_interface_parts']:
  o=objs[name];assert face_keys(records(o))==face_keys(parts[name][1]),(site,name,'GLB/native parity')
  bm=bmesh.new();bm.from_mesh(o.data);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=1e-6);assert all(e.is_manifold for e in bm.edges) and bm.calc_volume(signed=True)>0,(site,name,'Non-manifold or inverted');bm.free()
  if o.get('role')=='structural-stair-step':
   link=o['link'];f=next(f for f in m['architectural_plan']['flights'] if f[0]==link);_,foot,x,z0,z1,y0,y1,width,n=f;vs=[o.matrix_world@v.co for v in o.data.vertices]
   assert all(foot[0]-.001<=p.x<=foot[2]+.001 and foot[1]-.001<=-p.y<=foot[3]+.001 for p in vs)
   assert abs(max(p.x for p in vs)-min(p.x for p in vs)-width)<2e-5;treads+=1
 expected=sum(f[-1] for f in m['architectural_plan']['flights']);assert treads==expected
 sc=bpy.context.scene;sc.render.engine='BLENDER_WORKBENCH';sc.display.shading.color_type='MATERIAL';sc.display.shading.show_cavity=True;sc.render.resolution_x=1400;sc.render.resolution_y=1050;sc.render.resolution_percentage=100;sc.view_settings.view_transform='Standard';sc.world=bpy.data.worlds.new('Native authored architecture inspection');sc.world.color=(.07,.07,.07)
 bpy.ops.object.camera_add();cam=bpy.context.object;sc.camera=cam
 views={
 'FARMHOUSE':[('house-cutaway',(23,-17,1),(38,-36,24),23,'ORTHO',2.7),('barn-cutaway',(40,-20,2),(54,-34,19),17,'ORTHO',5.7),('house-standing',(24.5,-18,2),(24.5,-12.8,1.62),24,'PERSP',None),('cellar-standing',(21.5,-18,-1),(21.5,-12.8,-1.38),24,'PERSP',None)],
 'MANSION':[('service-cutaway',(43,-8,2.2),(59,-26,22),23,'ORTHO',10),('grand-cutaway',(24,-30,1.7),(40,-47,23),23,'ORTHO',3.3),('grand-standing',(24,-31,2),(24,-25,1.62),24,'PERSP',None),('service-standing',(44.5,-9,2),(44.5,-4.1,1.62),24,'PERSP',None)],
 'CHURCH':[('choir-cutaway',(20,-30,2),(34,-47,22),21,'ORTHO',2.7),('belfry-cutaway',(15,-30,4.5),(30,-47,24),22,'ORTHO',5.7),('choir-standing',(19.5,-31,2),(19.5,-26.8,1.62),24,'PERSP',None),('crypt-standing',(31.5,-31,-1),(31.5,-26.8,-1.38),24,'PERSP',None)]}[site]
 if site=='FARMHOUSE':views.extend([('upper-door-standing',(22,-21,4.4),(24.5,-20,4.62),24,'PERSP',None),('loft-door-standing',(37,-23,4.5),(41,-23,4.62),24,'PERSP',None)])
 elif site=='MANSION':views.extend([('upper-service-door-standing',(41.5,-14,5),(43,-12,5.22),24,'PERSP',None),('grand-head-standing',(24,-37,5),(24,-33,5.22),24,'PERSP',None)])
 else:views.extend([('choir-door-standing',(23,-33.1,4.62),(19.5,-33.1,4.62),24,'PERSP',None),('belfry-head-standing',(16.5,-32.4,7.3),(14,-33.1,7.62),24,'PERSP',None)])
 for name,target,position,scale,kind,cut in views:
  hidden=[]
  for o in objs.values():
   o.hide_render=False
   if cut is not None:
    vs=[o.matrix_world@v.co for v in o.data.vertices];low=min(p.z for p in vs);role=o.get('role','')
    o.hide_render=('terrain' in role or 'roof' in role or low>cut)
    # Review-only east/south face removal exposes load paths. Standing views hide nothing.
    if role=='complete reserved bay wall':
     xs=[p.x for p in vs];zs=[-p.y for p in vs]
     if max(xs)-min(xs)<.3 or max(zs)-min(zs)<.3 and max(zs)>13:o.hide_render=True
   if o.hide_render:hidden.append(o.name)
  cam.data.type=kind;cam.data.ortho_scale=scale;cam.data.lens=scale;cam.data.clip_start=.02;cam.data.clip_end=250;target=Vector(target);cam.location=Vector(position);cam.rotation_euler=(target-cam.location).to_track_quat('-Z','Y').to_euler();path=CAP/(site.lower()+'-'+name+'.png');sc.render.filepath=str(path);bpy.ops.render.render(write_still=True)
  captures.append(dict(site=site,path=path.relative_to(ROOT).as_posix(),sha256=sha(path),glb_sha256=sha(glb),view=name,hidden=hidden,type='Native source export; standing views hide nothing; labeled cutaways hide terrain, overhead layers and selected faces; not runtime'))
 proof.append(dict(site=site,closed_positive_new_solids=len(m['new_interface_parts']),treads=treads,all_treads_inside_reserved_bays=True,constant_flight_widths=True,retained_native_records=len(keep),retained_source_buffer_and_asset_arrays_exact=True,export_native_triangles_match=True,terrain_and_saved_holes_unchanged=True,runtime_acceptance=False))
 write(out/'export-verification.json',proof[-1]);print('VERIFIED_BAY_EXPORT',site,treads,flush=True)
base=json.loads((B/'architectural-restart-pass/roster.json').read_text());current=json.loads((O/'roster.json').read_text());assert len(current['sites'])==29;assert [s for s in base['active_sources'] if s['id'] not in ['FARMHOUSE','MANSION','CHURCH']]==[s for s in current['active_sources'] if s['id'] not in ['FARMHOUSE','MANSION','CHURCH']]
write(O/'captures.json',captures);write(O/'export-verification.json',dict(sites=proof,unchanged_other_source_rows=True,packets=29));print('THREE_MAP_EXPORT_CHECKS_PASS',flush=True)
