"""Render current saved second-pass scene, recording actual capture/source hashes."""
import bpy,json,hashlib
from pathlib import Path
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[2];OUT=ROOT/'development/levels/hollow-creek';PRE=ROOT/'development/levels/previews/hollow-creek';source=OUT/'hollow-creek-refined.blend';bpy.ops.wm.open_mainfile(filepath=str(source));scene=bpy.context.scene;fill=bpy.data.objects.get('Boundary inspection fill only');captures=[]
for name,pos,target,lens in [('refined-expanded-site-native-fitout',(105,-125,100),(31,-42,1),40),('refined-hearth-native-fitout',(33,-43,2.4),(25.9,-46.1,1.6),28),('refined-culvert-native-fitout',(44,-21,.7),(48.65,-25.5,-.7),34),('refined-toilets-native-fitout',(20,-25,1.8),(12.8,-17.4,0),30),('refined-dock-native-fitout',(36,-.5,3),(27,6.8,0),32),('refined-memorial-native-fitout',(10,-57,2.7),(5,-50.7,1.8),34)]:
 if name=='refined-culvert-native-fitout':target=(json.loads((OUT/'amenities.json').read_text(encoding='utf-8'))['creek']['culvert_center'][0],-25.5,-.7)
 scene.camera.location=pos;scene.camera.rotation_euler=(Vector(target)-scene.camera.location).to_track_quat('-Z','Y').to_euler();scene.camera.data.lens=lens
 if fill:fill.location=pos;fill.rotation_euler=scene.camera.rotation_euler
 path=PRE/(name+'.png');scene.render.filepath=str(path);bpy.ops.render.render(write_still=True);captures.append(dict(path=path.relative_to(ROOT).as_posix(),sha256=hashlib.sha256(path.read_bytes()).hexdigest(),kind='Native scene overview'))
report=dict(status='six actual native captures',scene=source.relative_to(ROOT).as_posix(),scene_sha256=hashlib.sha256(source.read_bytes()).hexdigest(),captures=captures,runtime_acceptance=False);(ROOT/'development/levels/verification/hollow-creek-refinement-captures.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8');print('Rendered six current native views from unchanged saved source',flush=True)
