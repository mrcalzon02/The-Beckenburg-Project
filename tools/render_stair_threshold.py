"""Paired intact stair/terrain views and whole-site native construction review."""
from pathlib import Path
import json,sys,bpy
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
from review_roster_macroterrain import sha
BASE=ROOT/'development/levels/rebuild/stair-terrain-pass';CAP=ROOT/'development/levels/rebuild/captures/stair-terrain-batch';CAP.mkdir(parents=True,exist_ok=True)
def point(p):return Vector((p[0],-p[2],p[1]))
captures=json.loads((BASE/'verification/captures.json').read_text());folder=BASE/'assemblies/wld-04';r=json.loads((folder/'construction.json').read_text())
for phase,source in [('before',ROOT/r['source_glb']),('after',folder/'benched-terrain.glb')]:
 bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(source));scene=bpy.context.scene;scene.world=bpy.data.worlds.new('Neutral threshold inspection');scene.world.use_nodes=True;scene.world.node_tree.nodes['Background'].inputs[1].default_value=.2;bpy.ops.object.camera_add();cam=bpy.context.object;cam.data.lens=25;cam.location=point([38,7.2,21.7]);target=point([40.2,6,22]);cam.rotation_euler=(target-cam.location).to_track_quat('-Z','Y').to_euler();scene.camera=cam;bpy.ops.object.light_add(type='POINT');fill=bpy.context.object;fill.location=cam.location;fill.data.energy=150;scene.render.engine='CYCLES';scene.cycles.samples=8;scene.render.resolution_x=1000;scene.render.resolution_y=750;scene.render.resolution_percentage=100;scene.view_settings.view_transform='Standard';scene.render.image_settings.file_format='PNG';path=CAP/('wld-04-threshold-'+phase+'.png');scene.render.filepath=str(path);bpy.ops.render.render(write_still=True);captures.append(dict(site='WLD-04',phase=phase,path=path.relative_to(ROOT).as_posix(),sha256=sha(path),source_glb_sha256=sha(source),type='Native intact paired threshold and terrain inspection with150Wneutral fill; not runtime'))
(BASE/'verification/captures.json').write_bytes((json.dumps(captures,indent=2)+'\n').encode())
