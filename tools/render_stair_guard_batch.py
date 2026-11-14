from pathlib import Path
import json,bpy
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[1];BASE=ROOT/'development/levels/rebuild/stair-guard-pass';CAP=ROOT/'development/levels/rebuild/captures/stair-guard-batch';CAP.mkdir(parents=True,exist_ok=True)
for p in BASE.glob('*/construction.json'):
 r=json.loads(p.read_text());bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(p.parent/'guarded-stairs.glb'));sc=bpy.context.scene;sc.render.engine='BLENDER_WORKBENCH';sc.display.shading.color_type='MATERIAL';sc.display.shading.show_cavity=True;sc.render.resolution_x=1100;sc.render.resolution_y=850;sc.render.resolution_percentage=100;sc.world=bpy.data.worlds.new('Native structural review');bpy.ops.object.camera_add();cam=bpy.context.object;sc.camera=cam;cam.data.type='ORTHO';ps=[v for points in r['design']['points'].values() for v in points];center=Vector(((min(v[0] for v in ps)+max(v[0] for v in ps))/2,-sum(v[2] for v in ps)/len(ps),-1));cam.data.ortho_scale=max(v[0] for v in ps)-min(v[0] for v in ps)+12;cam.location=center+Vector((10,-20,24));cam.rotation_euler=(center-cam.location).to_track_quat('-Z','Y').to_euler()
 for view in ['context','cutaway']:
  if view=='cutaway':
   for o in sc.objects:
    if o.type=='MESH':o.hide_render=not(o.get('role') in ['structural-stair-step','structural-stair-landing','fitted library stair guard','structural side stringer'] or o.name.startswith(('Orthogonal stair','Bay landing','Restored bay')))
  sc.render.filepath=str(CAP/(r['id'].lower()+'-'+view+'.png'));bpy.ops.render.render(write_still=True)
