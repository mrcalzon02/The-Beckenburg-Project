"""Inspect the fitted service shelter in its exact saved Wharf scene."""
from pathlib import Path
import bpy
from mathutils import Vector

folder=Path("development/levels/rebuild/wharf-power-pass").resolve()
bpy.ops.wm.open_mainfile(filepath=str(folder/"wharf-power.blend"))
scene=bpy.context.scene
scene.render.engine="BLENDER_WORKBENCH"
display=scene.display.shading
display.color_type="MATERIAL"
display.show_cavity=True
display.cavity_type="BOTH"
scene.render.resolution_x=1320
scene.render.resolution_y=960
scene.render.resolution_percentage=100
bpy.ops.object.camera_add()
cam=bpy.context.object
cam.data.type="ORTHO"
scene.camera=cam
root=folder/"captures/structure"
root.mkdir(parents=True,exist_ok=True)
target=Vector((37.5,-56.45,1.15))
for name,eye,scale in (
    ("wharf-service-forecourt",(30,-64,6.5),14.5),
    ("wharf-service-framing",(33.8,-61.5,6.5),8.5),
):
    if name.endswith("framing"):
        for ob in scene.objects:
            if ob.name.startswith("Wharf ") and ob.get("role")=="roof seated on four bearing purlins":
                ob.hide_render=True
    cam.location=Vector(eye)
    cam.rotation_euler=(target-cam.location).to_track_quat("-Z","Y").to_euler()
    cam.data.ortho_scale=scale
    scene.render.filepath=str(root/(name+".png"))
    bpy.ops.render.render(write_still=True)
