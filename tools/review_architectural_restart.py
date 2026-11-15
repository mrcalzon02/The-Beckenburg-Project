"""Inspect exact published model/collider outputs, including standing-height views."""
from pathlib import Path
import sys,json
import bpy,bmesh
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
from append_static_glb import read
from compose_room_terrain import split_native
from review_roster_macroterrain import records,sha
from export_roster_collision import face_keys
from check_roster_decks import read_parts
from build_deliberate_entrances import write
B=ROOT/'development/levels/rebuild';O=B/'architectural-restart-pass';CAP=B/'captures/architectural-restart-batch';CAP.mkdir(exist_ok=True)
proof=[]
for site in ['WLD-04','WLD-09']:
 out=O/site.lower();m=json.loads((out/'construction.json').read_text());glb=out/'reconstructed.glb';native=out/'reconstructed.hcs2'
 assert sha(glb)==m['glb_sha256'] and sha(native)==m['physics_sha256'] and sha(ROOT/m['layout'])==m['layout_sha256']
 a,blob=read(ROOT/m['source_glb']);b,after=read(glb);assert after[:len(blob)]==blob
 for key in ['meshes','accessors','bufferViews','images','textures','materials']:assert b.get(key,[])[:len(a.get(key,[]))]==a.get(key,[])
 removed=set(m['removed_source_meshes'])
 for i,n in enumerate(a['nodes']):
  expected=dict(n)
  if n.get('name') in removed:expected.pop('mesh',None)
  assert b['nodes'][i]==expected
 old=split_native(ROOT/m['source_physics']);new=split_native(native);keep=[v for v in old if v[0].split(':',1)[-1] not in removed];assert new[:len(keep)]==keep
 bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(glb));objs={o.name:o for o in bpy.context.scene.objects if o.type=='MESH'};parts=read_parts(native)
 for name in m['new_interface_parts']:
  o=objs[name];assert face_keys(records(o))==face_keys(parts[name][1]),name
  bm=bmesh.new();bm.from_mesh(o.data);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=1e-6);assert all(e.is_manifold for e in bm.edges) and bm.calc_volume(signed=True)>0,name;bm.free()
 if site=='WLD-04':
  assert len([n for n in m['new_interface_parts'] if ' tread ' in n])==54
  # Cabin floor has no old route-derived cut; its stairs are outside its footprint.
  assert all(f['low'][2]>=10 and f['high'][2]>=10 for f in m['flights'] if f['link']!='TOWER-ENTRY')
 scenes=[('overview',Vector((30,-10,10)),Vector((57,-42,30)),34,'ORTHO') ,('entry-standing',Vector((30.8,-12,8)),Vector((30.8,-18,7.62)),24,'PERSP'),('top-standing',Vector((25.5,-7.9,16.2)),Vector((25.5,-12,16.62)),23,'PERSP')] if site=='WLD-04' else [('workshop-standing',Vector((45,-48,-4.5)),Vector((45,-58,-4.38)),23,'PERSP'),('workshop-section',Vector((45,-55,-4.5)),Vector((64,-70,14)),30,'ORTHO')]
 sc=bpy.context.scene;sc.render.engine='BLENDER_WORKBENCH';sc.display.shading.color_type='MATERIAL';sc.display.shading.show_cavity=True;sc.render.resolution_x=1400;sc.render.resolution_y=1050;sc.render.resolution_percentage=100;sc.view_settings.view_transform='Standard';sc.world=bpy.data.worlds.new('Native architectural review');sc.world.color=(.08,.08,.08)
 bpy.ops.object.camera_add();cam=bpy.context.object;sc.camera=cam
 for name,target,position,scale,kind in scenes:
  hidden=[]
  for o in objs.values():
   o.hide_render=False
   if name=='workshop-section':
    pts=[o.matrix_world@v.co for v in o.data.vertices]
    o.hide_render='terrain' in o.get('role','') or min(v.z for v in pts)>-3.31
   if o.hide_render:hidden.append(o.name)
  cam.data.type=kind;cam.data.ortho_scale=scale;cam.data.lens=scale;cam.data.clip_start=.02;cam.data.clip_end=300;cam.location=position;cam.rotation_euler=(target-position).to_track_quat('-Z','Y').to_euler();path=CAP/(site.lower()+'-'+name+'.png');sc.render.filepath=str(path);bpy.ops.render.render(write_still=True)
  proof.append(dict(site=site,path=path.relative_to(ROOT).as_posix(),sha256=sha(path),glb_sha256=sha(glb),view=name,hidden=hidden,type='Native source export; not runtime. Section explicitly hides terrain and overhead layers.'))
 write(out/'export-verification.json',dict(closed_positive_new_solids=len(m['new_interface_parts']),retained_native_records=len(keep),retained_source_buffer_and_asset_arrays_exact=True,export_native_triangles_match=True,terrain_and_saved_holes_unchanged=True,runtime_acceptance=False))
write(O/'captures.json',proof)
print('RESTART_EXPORT_REVIEW_PASS',flush=True)
