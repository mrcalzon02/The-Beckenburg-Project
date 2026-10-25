"""Matched native green-grass review; no lighting/exposure changes."""
import bpy,json,hashlib
from pathlib import Path
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[2];OUT=ROOT/'development/levels/hollow-creek';PRE=ROOT/'development/levels/previews/hollow-creek';r=json.loads((OUT/'green-grass.json').read_text(encoding='utf-8'));captures=[]
for phase,path in [('before',r['parent']),('after',r['scene'])]:
 bpy.ops.wm.open_mainfile(filepath=str(ROOT/path));scene=bpy.context.scene
 for label,pos,target,lens in [('camp',(18,-42,2.1),(13,-34,-.2),30),('site',(105,-125,100),(31,-42,1),40)]:
  if phase=='before' and label=='site':continue
  scene.camera.location=pos;scene.camera.rotation_euler=(Vector(target)-scene.camera.location).to_track_quat('-Z','Y').to_euler();scene.camera.data.lens=lens
  fill=bpy.data.objects.get('Boundary inspection fill only')
  if fill:fill.location=pos;fill.rotation_euler=scene.camera.rotation_euler
  scene.cycles.samples=24;scene.render.resolution_x=1600;scene.render.resolution_y=1066;file=PRE/f'grass-green-{phase}-{label}-native-fitout.png';scene.render.filepath=str(file);bpy.ops.render.render(write_still=True);captures.append(dict(path=file.relative_to(ROOT).as_posix(),sha256=hashlib.sha256(file.read_bytes()).hexdigest(),phase=phase,kind='Native Blender material review',camera=list(pos),target=list(target)))
(ROOT/'development/levels/verification/hollow-creek-green-captures.json').write_text(json.dumps(dict(captures=captures,runtime_capture=False),indent=2)+'\n',encoding='utf-8')
