"""Native assembly inspection at the same relative transforms as the runtime logway."""
import bpy,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'development/assets'))
import rebuild_support as s
s.reset();objects=[]
for name,y in [('circular_headrig',0),('log_carriage',3.4)]:
    path=ROOT/f'development/levels/blender/level_work_{name}.blend'
    with bpy.data.libraries.load(str(path)) as (source,target):target.objects=['level_work_'+name]
    obj=target.objects[0];bpy.context.collection.objects.link(obj);obj.location.y=y;objects.append(obj)
scene=bpy.context.scene;scene.unit_settings.system='METRIC';scene.unit_settings.scale_length=1
scene['source']='Original native meshes; headrig Z34.0, carriage Z30.6, equal X43.2 and Y0 before shared terrain grade'
bpy.ops.wm.save_as_mainfile(filepath=str(ROOT/'development/levels/blender/blackpine-logway-assembly.blend'),compress=True)
# render_family normally lays assets side by side; bake the assembly transforms
# into independent copies so that the inspection retains the authored alignment.
for obj in objects:
    for v in obj.data.vertices:v.co+=obj.location
    obj.location=(0,0,0)
assembly=s.join(objects,'Circular head saw and aligned logway')
s.render_family('blackpine-logway',[assembly],path=ROOT/'development/levels/previews/mill-expansion/logway-assembly-studio.png',view=(5,-8,4))
