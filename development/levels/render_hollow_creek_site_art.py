"""Photographic native reviews, never labeled runtime gameplay."""
import bpy,json,hashlib
from pathlib import Path
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[2];OUT=ROOT/'development/levels/hollow-creek';PRE=ROOT/'development/levels/previews/hollow-creek'
r=json.loads((OUT/'siteart-fitted-floors.json').read_text(encoding='utf-8'));source=ROOT/r['scene'];assert hashlib.sha256(source.read_bytes()).hexdigest()==r['scene_sha256']
bpy.ops.wm.open_mainfile(filepath=str(source));scene=bpy.context.scene;fill=bpy.data.objects.get('Boundary inspection fill only');captures=[]
views=[('siteart-site-native-fitout',(105,-125,100),(31,-42,1),40),('siteart-ground-native-fitout',(18,-42,2.1),(13,-34,-.2),30),('siteart-shore-native-fitout',(52,-26,2),(43,-12,-1.7),30),('siteart-memorial-native-fitout',(9,-57,4.5),(5,-50.7,2),38)]
for name,pos,target,lens in views:
 scene.camera.location=pos;scene.camera.rotation_euler=(Vector(target)-scene.camera.location).to_track_quat('-Z','Y').to_euler();scene.camera.data.lens=lens
 if fill:fill.location=pos;fill.rotation_euler=scene.camera.rotation_euler
 scene.cycles.samples=24;scene.render.resolution_x=1600;scene.render.resolution_y=1066;path=PRE/(name+'.png');scene.render.filepath=str(path);bpy.ops.render.render(write_still=True)
 captures.append(dict(path=path.relative_to(ROOT).as_posix(),sha256=hashlib.sha256(path.read_bytes()).hexdigest(),kind='Native scene review'))
marks=bpy.data.objects['Cosmetic UV goose footprint carriers'];marks.hide_render=False;marks.hide_set(False)
mat=next(m for m in marks.data.materials if 'outer face' in m.name);p=mat.node_tree.nodes['Principled BSDF'];p.inputs['Emission Color'].default_value=(.08,.65,.20,1);p.inputs['Emission Strength'].default_value=.6
scene.camera.location=(7,-53.4,2.0);scene.camera.rotation_euler=(Vector((5,-50.7,.95))-scene.camera.location).to_track_quat('-Z','Y').to_euler();scene.camera.data.lens=42
bpy.ops.object.light_add(type='SPOT',location=scene.camera.location);light=bpy.context.object;light.rotation_euler=scene.camera.rotation_euler;light.data.energy=110;light.data.color=(.30,.015,1);light.data.spot_size=.8
path=PRE/'siteart-uv-memorial-native-diagnostic.png';scene.render.filepath=str(path);bpy.ops.render.render(write_still=True)
captures.append(dict(path=path.relative_to(ROOT).as_posix(),sha256=hashlib.sha256(path.read_bytes()).hexdigest(),kind='Native UV layer diagnostic; manually revealed, not gameplay trigger'))
assert hashlib.sha256(source.read_bytes()).hexdigest()==r['scene_sha256']
(ROOT/'development/levels/verification/hollow-creek-siteart-captures.json').write_text(json.dumps(dict(scene=r['scene'],scene_sha256=r['scene_sha256'],captures=captures,runtime_capture=False),indent=2)+'\n',encoding='utf-8')
