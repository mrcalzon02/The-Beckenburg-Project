
"""Capture the same standing view of both exact House source revisions."""
from argparse import ArgumentParser
from pathlib import Path
import sys

import bpy
from mathutils import Vector

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools"))

from build_measured_approaches import BASE, relative, sha, write
from build_surveyed_thresholds import OUT

parser = ArgumentParser()
parser.add_argument("--before", type=Path, required=True)
parser.add_argument("--after", type=Path, default=OUT/"house/briar-rebuild-furnishings-flush-porch.blend")
args = parser.parse_args(sys.argv[sys.argv.index("--")+1:])
captures = BASE/"captures/flush-porch-and-threshold-batch"
captures.mkdir(parents=True,exist_ok=True)
position, focus = (22.3,-25.8,1.62), (19.8,-21.67,1.07)
results = {}
for label, path in (("before", args.before), ("after", args.after)):
    model = path.resolve()
    before = sha(model)
    bpy.ops.wm.open_mainfile(filepath=str(model))
    scene = bpy.context.scene
    scene.render.engine = "CYCLES"
    scene.cycles.device = "CPU"
    scene.cycles.samples = 12
    scene.cycles.use_denoising = True
    scene.render.resolution_x,scene.render.resolution_y = 1040,700
    scene.render.resolution_percentage = 100
    scene.world.node_tree.nodes.get("Background").inputs["Strength"].default_value = .8
    for light in (obj for obj in scene.objects if obj.type=="LIGHT" and obj.data.type=="SUN"):
        light.data.energy = 2
    fill = bpy.data.lights.new("Porch inspection fill","AREA")
    fill.energy = 2600
    fill.shape = "DISK"
    fill.size = 6
    lamp = bpy.data.objects.new(fill.name,fill)
    scene.collection.objects.link(lamp)
    lamp.location = (20,-25.6,4.8)
    lamp.rotation_euler = (Vector((19.8,-21.2,1.15))-lamp.location).to_track_quat("-Z","Y").to_euler()
    scene.view_settings.exposure = 1.2
    camera = scene.camera
    camera.location = position
    camera.rotation_euler = (Vector(focus)-camera.location).to_track_quat("-Z","Y").to_euler()
    camera.data.lens = 31
    png = captures/f"house-porch-{label}-standing.png"
    scene.render.filepath = str(png)
    bpy.ops.render.render(write_still=True)
    assert sha(model)==before
    results[label] = dict(native_sha256=before, revision=scene["revision"], capture=relative(png), capture_sha256=sha(png))

write(OUT/"house/standing-review.json",dict(camera=list(position),focus=list(focus),lighting="Native inspection daylight with identical front fill in both views",views=results))
