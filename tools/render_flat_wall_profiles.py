"""Inspect actual exported reconstructed asylum wall profiles, both sides."""
from pathlib import Path
import json,sys,bpy
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'));from review_roster_macroterrain import sha
BASE=ROOT/'development/levels/rebuild/flat-circulation-pass';CAP=ROOT/'development/levels/rebuild/captures/flat-circulation-batch';source=BASE/'asylum/open-circulation.glb';r=json.loads((BASE/'asylum/construction.json').read_text());bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(source));scene=bpy.context.scene;scene.render.engine='BLENDER_WORKBENCH';scene.display.shading.color_type='MATERIAL';scene.display.shading.show_cavity=True;scene.display.shading.show_shadows=True;scene.render.resolution_x=800;scene.render.resolution_y=600;scene.render.resolution_percentage=100;scene.render.image_settings.file_format='PNG';bpy.ops.object.camera_add();cam=bpy.context.object;cam.data.type='ORTHO';scene.camera=cam;rows=json.loads((BASE/'verification/captures.json').read_text())
for i,p in enumerate(r['wall_profile_reconstruction']['parts']):
 for obj in scene.objects:
  if obj.type=='MESH':obj.hide_render=obj.name not in p['after']
 targets=[bpy.data.objects[n] for n in p['after']];points=[o.matrix_world@Vector(v) for o in targets for v in o.bound_box];centre=sum(points,Vector())/len(points);span=max(max(v[k] for v in points)-min(v[k] for v in points) for k in range(3));cam.data.ortho_scale=span*1.35;normal=Vector((1,0,0)) if p['wall_normal_axis']==0 else Vector((0,1,0));along=Vector((0,1,0)) if p['wall_normal_axis']==0 else Vector((1,0,0))
 for side in [-1,1]:
  cam.location=centre+normal*side*span*1.5+along*span*.25+Vector((0,0,span*.15));cam.rotation_euler=(centre-cam.location).to_track_quat('-Z','Y').to_euler();path=CAP/('asylum-profile-'+str(i)+'-'+str(side)+'.png');scene.render.filepath=str(path);bpy.ops.render.render(write_still=True);rows.append(dict(site='ASYLUM',phase='after',path=path.relative_to(ROOT).as_posix(),sha256=sha(path),source_glb_sha256=sha(source),type='Native isolated actual exported wall profile, other map context hidden; not runtime/fixture/support acceptance'))
(BASE/'verification/captures.json').write_bytes((json.dumps(rows,indent=2)+'\n').encode())
