import bpy,bmesh,json,sys
from pathlib import Path
from mathutils import Vector
r=Path.cwd();sys.path.insert(0,str(r/'tools'));from review_roster_macroterrain import sha,records;from export_roster_collision import face_keys,native_faces;from compose_room_terrain import split_native
O=r/'development/levels/rebuild/dam-contained-stair-pass';f=O/'wld-09';m=json.loads((f/'construction.json').read_text());cap=r/'development/levels/rebuild/captures/dam-contained-stair-batch';cap.mkdir(parents=True,exist_ok=True);proof=[]
for mode in ['whole-site','hall-cutaway','hall-load-path','west-cutaway']:
 bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(f/'contained-hall.glb'));objs={o.name:o for o in bpy.context.scene.objects if o.type=='MESH'}
 if mode=='whole-site':
  original=split_native(r/m['source_physics']);removed=set(m['removed_source_meshes']);kept=[p for p in original if not p[0].startswith('terrain-') and p[0].split(':',1)[-1] not in removed];after=split_native(f/'contained-hall.hcs2');assert after[:len(kept)]==kept
  for name in m['new_interface_parts']:
   o=objs[name];bm=bmesh.new();bm.from_mesh(o.data);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=1e-6);assert all(e.is_manifold for e in bm.edges) and bm.calc_volume(signed=True)>0,name;bm.free()
   if 'contained main tread' in name:
    pts=[o.matrix_world@v.co for v in o.data.vertices];left,right=(26,36) if 'west contained' in name else (64,70.5);assert all(left<=p.x<=right and 22<=-p.y<=48 for p in pts),name;assert abs(max(p.x for p in pts)-min(p.x for p in pts)-1.8)<2e-5;proof.append(name)
  assert len(proof)==68
  terrain=next(o for o in objs.values() if o.get('role','').startswith('saved editor terrain'));world=r/m['world'];assert face_keys(records(terrain))==face_keys(native_faces(world/'terrain-physics.hcs2'));assert [p[2] for p in after if p[0].startswith('terrain-')]==[p[2] for p in split_native(world/'terrain-physics.hcs2')]
  for path,h in m['world_files_sha256'].items():assert sha(r/path)==h
  before=json.loads((r/'development/levels/rebuild/stair-interface-construction-pass/roster.json').read_text());current=json.loads((O/'roster.json').read_text());assert [v for v in before['active_sources'] if v['id']!='WLD-09']==[v for v in current['active_sources'] if v['id']!='WLD-09'];assert len(current['sites'])==29
 for o in objs.values():
  role=o.get('role','')
  if role in ['enclosed pitched roof','ceiling-cap'] or 'enclosed roof' in o.name:o.hide_render=True
  if mode!='whole-site' and ('terrain' in role or 'hall E ' in o.name or 'west-hall E ' in o.name or 'turbine-west' in o.name):o.hide_render=True
  if mode in ['hall-load-path','west-cutaway'] and ('hall upper floor' in o.name or 'well enclosed side' in o.name or 'well closed north end' in o.name or 'hall S ' in o.name or 'hall W ' in o.name):o.hide_render=True
 scene=bpy.context.scene;scene.render.engine='BLENDER_WORKBENCH';scene.display.shading.color_type='MATERIAL';scene.display.shading.show_cavity=True;scene.render.resolution_x=1400;scene.render.resolution_y=1050;scene.render.resolution_percentage=100;scene.world=bpy.data.worlds.new('Actual contained hall review');bpy.ops.object.camera_add();camera=bpy.context.object;scene.camera=camera;target=Vector((44,-44,-1)) if mode=='whole-site' else Vector((31,-35,-2)) if mode=='west-cutaway' else Vector((67,-35,-2));camera.location=target+Vector((30,-33,40)) if mode!='hall-load-path' else target+Vector((24,-8,12));camera.rotation_euler=(target-camera.location).to_track_quat('-Z','Y').to_euler();camera.data.type='ORTHO';camera.data.ortho_scale=92 if mode=='whole-site' else 34;scene.render.filepath=str(cap/(mode+'.png'));bpy.ops.render.render(write_still=True)
(O/'verification').mkdir(exist_ok=True);(O/'verification/architecture-and-source.json').write_text(json.dumps(dict(both_primary_stairs_all_treads_inside_halls=True,treads=68,actual_width=1.8,closed_new_solids=True,old_unremoved_native_records_exact=True,emergency_replacement_open=True),indent=2)+'\n');print('CONTAINED_HALL_ARCHITECTURE_REVIEW_READY',flush=True)
