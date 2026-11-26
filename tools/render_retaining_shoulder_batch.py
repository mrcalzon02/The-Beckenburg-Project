from pathlib import Path
import json,bpy
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[1];BASE=ROOT/'development/levels/rebuild/retaining-shoulder-pass';CAP=ROOT/'development/levels/rebuild/captures/retaining-shoulder-batch';CAP.mkdir(parents=True,exist_ok=True)
for p in BASE.glob('*/construction.json'):
 r=json.loads(p.read_text());bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(p.parent/'retained-cuts.glb'));sc=bpy.context.scene;sc.render.engine='BLENDER_WORKBENCH';sc.display.shading.color_type='MATERIAL';sc.display.shading.show_cavity=True;sc.display.shading.show_shadows=True;sc.render.resolution_x=1100;sc.render.resolution_y=850;sc.render.resolution_percentage=100;sc.world=bpy.data.worlds.new('Native terrain retaining construction');bpy.ops.object.camera_add();cam=bpy.context.object;sc.camera=cam;cam.data.type='ORTHO';cam.data.clip_end=500
 target=max(r['retaining_shoulders'],key=lambda q:q['top_y']-min(q['actual_soil_y']));points=target['footprint'];centre=Vector((sum(v[0] for v in points)/4,-sum(v[1] for v in points)/4,(target['top_y']+min(target['actual_soil_y']))/2));cam.data.ortho_scale=10;cam.location=centre+Vector((8,-10,7));cam.rotation_euler=(centre-cam.location).to_track_quat('-Z','Y').to_euler()
 for view in ['terrain-interface','isolated-construction']:
  if view=='isolated-construction':
   allpoints=[v for q in r['retaining_shoulders'] for v in q['footprint']];centre=Vector(((min(v[0] for v in allpoints)+max(v[0] for v in allpoints))/2,-(min(v[1] for v in allpoints)+max(v[1] for v in allpoints))/2,0));cam.data.ortho_scale=max(10,max(v[0] for v in allpoints)-min(v[0] for v in allpoints)+10,max(v[1] for v in allpoints)-min(v[1] for v in allpoints)+10);cam.location=centre+Vector((30,-40,40));cam.rotation_euler=(centre-cam.location).to_track_quat('-Z','Y').to_euler()
   for o in sc.objects:
    if o.type=='MESH':o.hide_render=o.get('role')!='terrain-rooted retaining shoulder'
  sc.render.filepath=str(CAP/(r['id'].lower()+'-'+view+'.png'));bpy.ops.render.render(write_still=True)
