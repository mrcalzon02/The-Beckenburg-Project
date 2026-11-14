"""Actual exported bay overviews and two-sided closeups, native only."""
from pathlib import Path
import json,bpy
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[1];BASE=ROOT/'development/levels/rebuild/stair-bay-pass';CAP=ROOT/'development/levels/rebuild/captures/stair-bay-batch';CAP.mkdir(parents=True,exist_ok=True)
for p in sorted(BASE.glob('*/construction.json')):
 r=json.loads(p.read_text());data=json.loads((ROOT/r['layout']).read_text());links={l['id']:l for l in data['links']};bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(p.parent/'bayed-stairs.glb'));scene=bpy.context.scene;scene.render.engine='BLENDER_WORKBENCH';scene.display.shading.color_type='MATERIAL';scene.display.shading.show_cavity=True;scene.display.shading.show_shadows=True;scene.render.resolution_x=1000;scene.render.resolution_y=750;scene.render.resolution_percentage=100;scene.render.image_settings.file_format='PNG';scene.world=bpy.data.worlds.new('Native stair bay review');a,b,c,d=data['site_bounds'];span=max(c-a,d-b);center=Vector(((a+c)/2,-(b+d)/2,0));bpy.ops.object.camera_add();cam=bpy.context.object;cam.data.type='ORTHO';cam.data.ortho_scale=span*1.5;cam.location=center+Vector((span*.9,-span*.9,span*.9));cam.rotation_euler=(center-cam.location).to_track_quat('-Z','Y').to_euler();scene.camera=cam;scene.render.filepath=str(CAP/(r['id'].lower()+'-whole-site.png'));bpy.ops.render.render(write_still=True)
 targets=list(r['design']['build']) or sorted(set(r['design']['shared'].values()))
 for key in targets:
  link=links[key]
  for i,(a,b) in enumerate(zip(link['points'],link['points'][1:])):
   if abs(a[1]-b[1])<.01:continue
   center=Vector(((a[0]+b[0])/2,-(a[2]+b[2])/2,(a[1]+b[1])/2+1));direction=Vector((b[0]-a[0],a[2]-b[2],0)).normalized()
   for side in [-1,1]:
    cam.data.type='ORTHO';cam.data.ortho_scale=max(10,((a[0]-b[0])**2+(a[2]-b[2])**2)**.5*.9);cam.location=center+direction*side*8+Vector((5,2,5));cam.rotation_euler=(center-cam.location).to_track_quat('-Z','Y').to_euler();scene.render.filepath=str(CAP/(r['id'].lower()+'-'+key+'-native-context-'+str(side)+'.png'));bpy.ops.render.render(write_still=True)
   # Deliberate cutaway separates shaft/plate topology from occluded context.
   for obj in bpy.context.scene.objects:
    if obj.type=='MESH':obj.hide_render=obj.get('role') not in ['structural-stair-step','structural-stair-landing','floor'] or obj.get('role')=='floor' and min((obj.matrix_world@Vector(v)).z for v in obj.bound_box)>max(a[1],b[1])+.01
   cam.data.type='ORTHO';cam.data.ortho_scale=18;cam.location=center+Vector((9,-10,9));cam.rotation_euler=(center-cam.location).to_track_quat('-Z','Y').to_euler();scene.render.filepath=str(CAP/(r['id'].lower()+'-'+key+'-cutaway.png'));bpy.ops.render.render(write_still=True)
   cam.data.ortho_scale=max(10,((a[0]-b[0])**2+(a[2]-b[2])**2)**.5+3);cam.location=center+Vector((0,0,25));cam.rotation_euler=(center-cam.location).to_track_quat('-Z','Y').to_euler();scene.render.filepath=str(CAP/(r['id'].lower()+'-'+key+'-shaft-plan.png'));bpy.ops.render.render(write_still=True)
   for obj in bpy.context.scene.objects:obj.hide_render=False
