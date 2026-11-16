"""Reimport the actual Lookout export, verify sources, and render eye-level views."""
from pathlib import Path
import sys,json
import bpy,bmesh
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
from append_static_glb import read
from compose_room_terrain import split_native
from review_roster_macroterrain import records,sha
from export_roster_collision import face_keys,native_faces
from check_roster_decks import read_parts
from build_deliberate_entrances import write
B=ROOT/'development/levels/rebuild';O=B/'service-ground-bay-pass/wld-04';CAP=B/'captures/service-ground-bay-batch';CAP.mkdir(parents=True,exist_ok=True)
m=json.loads((O/'construction.json').read_text());glb=O/'authored-service-bay.glb';native=O/'authored-service-bay.hcs2'
assert sha(glb)==m['glb_sha256'] and sha(native)==m['physics_sha256']
a,blob=read(ROOT/m['source_glb']);b,after=read(glb);assert after[:len(blob)]==blob
for key in ['meshes','accessors','bufferViews','images','textures','materials']:assert b.get(key,[])[:len(a.get(key,[]))]==a.get(key,[])
removed=set(m['removed_source_meshes'])
for i,n in enumerate(a['nodes']):
 expected=dict(n)
 if n.get('name') in removed:expected.pop('mesh',None)
 assert b['nodes'][i]==expected
old=split_native(ROOT/m['source_physics']);new=split_native(native);keep=[v for v in old if not v[0].startswith('terrain-') and v[0].split(':',1)[-1] not in removed];assert new[:len(keep)]==keep
sourceworld=ROOT/m['source_terrain'];world=ROOT/m['saved_world']
for name in ['tile_0_0.png','adaptive-topology.bin','authored-cuts.tsv']:assert sha(sourceworld/name)==sha(world/name)
bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(glb));objs={o.name:o for o in bpy.context.scene.objects if o.type=='MESH'};parts=read_parts(native)
for name in m['new_interface_parts']:
 o=objs[name];assert face_keys(records(o))==face_keys(parts[name][1]);bm=bmesh.new();bm.from_mesh(o.data);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=1e-6);assert all(e.is_manifold for e in bm.edges) and bm.calc_volume(signed=True)>0,name;bm.free()
assert face_keys(records(objs[m['terrain_mesh']]))==face_keys(native_faces(world/'terrain-physics.hcs2'))
sc=bpy.context.scene;sc.render.engine='BLENDER_WORKBENCH';sc.display.shading.color_type='MATERIAL';sc.display.shading.show_cavity=True;sc.render.resolution_x=1400;sc.render.resolution_y=1050;sc.render.resolution_percentage=100;sc.view_settings.view_transform='Standard';sc.world=bpy.data.worlds.new('Actual exported Lookout architecture');sc.world.color=(.07,.07,.07)
bpy.ops.object.camera_add();cam=bpy.context.object;sc.camera=cam;captures=[]
views=[('whole-site',(29,-37,5),(75,-100,65),105,'ORTHO',True),('upper-structure',(28,-26,6),(62,-64,40),62,'ORTHO',True),('west-stair-standing',(28,-33,4.5),(28,-37,4.62),24,'PERSP',False),('east-stair-standing',(45,-43,4.8),(45,-47.4,4.62),24,'PERSP',False),('clearing-standing',(30.8,-15,7),(30,-23.5,7.27),24,'PERSP',False),('ruin-porch-standing',(40,-22,7.3),(35.8,-22,7.27),24,'PERSP',False),('lower-stair-standing',(20,-54,2),(20,-59.3,1.62),24,'PERSP',False)]
for name,target,pos,scale,kind,cut in views:
 hidden=[]
 for o in objs.values():
  o.hide_render=cut and ('roof' in o.get('role','') or o.get('role')=='ceiling-cap')
  if o.hide_render:hidden.append(o.name)
 target=Vector(target);cam.data.type=kind;cam.data.ortho_scale=scale;cam.data.lens=scale;cam.data.clip_start=.02;cam.data.clip_end=300;cam.location=Vector(pos);cam.rotation_euler=(target-cam.location).to_track_quat('-Z','Y').to_euler();p=CAP/('wld-04-'+name+'.png');sc.render.filepath=str(p);bpy.ops.render.render(write_still=True);captures.append(dict(site='WLD-04',path=p.relative_to(ROOT).as_posix(),sha256=sha(p),glb_sha256=sha(glb),view=name,hidden=hidden,type='Native export overview with roofs hidden' if cut else 'Native exported ordinary standing-height view; nothing hidden; runtime acceptance open'))
write(O/'captures.json',captures);write(O/'export-verification.json',dict(new_closed_positive_solids=len(m['new_interface_parts']),treads=72,retained_native_records=len(keep),source_buffers_assets_nodes_pivots_uv_exact=True,terrain_source_height_topology_holes_exact=True,saved_editor_terrain_export_native_parity=True,new_geometry_export_native_parity=True,runtime_acceptance=False))
print('LOOKOUT_EXPORT_AND_SOURCE_CHECKS_PASS',flush=True)
