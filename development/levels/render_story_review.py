"""Bright temporary native inspection lighting; saved runtime scene remains unchanged."""
import bpy,json,math
from pathlib import Path
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[2]
bpy.ops.wm.open_mainfile(filepath=str(ROOT/'development/levels/blender/blackpine-dressed.blend'))
scene=bpy.context.scene
definition=json.loads((ROOT/'build/level-verification/scenes/blackpine/definition.json').read_text())
for o in scene.objects:
    if o.type=='LIGHT':o.data.energy=0
views=[('01-crew-life',4,(11.8,21.6),(14.8,18),.9),('02-final-payroll',9,(20,7.5),(19.7,5.7),.9),
       ('03-foreman-belongings',6,(27.8,14.5),(26.2,13.5),.9),('04-recent-occupation',7,(36.5,9.2),(38.7,10.4),.3),
       ('05-washing-and-retired-heat',5,(10.5,40.6),(8.5,36.7),1),('06-repair-and-maintenance-power',8,(47.2,51.6),(45.1,47.8),.8),
       ('07-kitchen-services',3,(18.4,34.8),(20.6,31),1)]
scene.render.engine='CYCLES';scene.cycles.samples=20;scene.cycles.use_denoising=True
scene.render.resolution_x=1280;scene.render.resolution_y=800;scene.render.resolution_percentage=100
scene.view_settings.view_transform='Standard';scene.view_settings.look='None';scene.view_settings.exposure=0
out=ROOT/'development/levels/previews/blackpine-story-native';out.mkdir(exist_ok=True)
for name,room,eye,target,ty in views:
    r=definition['rooms'][room];floor=r['y']
    bpy.ops.object.light_add(type='AREA',location=((r['x0']+r['x1'])/2,-(r['z0']+r['z1'])/2,floor+2.7));lamp=bpy.context.object;lamp.data.energy=350;lamp.data.shape='DISK';lamp.data.size=4
    bpy.ops.object.camera_add(location=(eye[0],-eye[1],floor+1.62));cam=bpy.context.object
    cam.rotation_euler=(Vector((target[0],-target[1],floor+ty))-cam.location).to_track_quat('-Z','Y').to_euler();cam.data.lens=24;scene.camera=cam
    scene.render.filepath=str(out/(name+'.png'));bpy.ops.render.render(write_still=True);lamp.data.energy=0
print('STORY_NATIVE_REVIEW_COMPLETE',len(views),flush=True)
