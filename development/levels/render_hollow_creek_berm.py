"""Same-camera before/after native berm material reviews; sources immutable."""
import bpy,json,hashlib
from pathlib import Path
from mathutils import Vector
from mathutils.bvhtree import BVHTree
ROOT=Path(__file__).resolve().parents[2];OUT=ROOT/'development/levels/hollow-creek';PRE=ROOT/'development/levels/previews/hollow-creek'
r=json.loads((OUT/'grounded-berm.json').read_text(encoding='utf-8'));captures=[]
views=[('toe',(9,-28,1.4),(3.7,-29,.6),38),('camp',(18,-42,2.1),(13,-34,-.2),30)]
for phase,path,expected in [('before',r['source'],r['source_sha256']),('after',r['scene'],r['scene_sha256'])]:
 source=ROOT/path;assert hashlib.sha256(source.read_bytes()).hexdigest()==expected
 bpy.ops.wm.open_mainfile(filepath=str(source));scene=bpy.context.scene;fill=bpy.data.objects.get('Boundary inspection fill only')
 ground=bpy.data.objects['level_camp_terrain_amenities'];ground.data.calc_loop_triangles();tree=BVHTree.FromPolygons([ground.matrix_world@v.co for v in ground.data.vertices],[tuple(t.vertices) for t in ground.data.loop_triangles],all_triangles=True)
 def eye(x,y,h=1.65):
  p=tree.ray_cast(Vector((x,y,40)),Vector((0,0,-1)),100)[0];assert p is not None;return (x,y,p.z+h)
 rock=r['outcrops'][0]['center'];rock_view=('bedrock',eye(rock[0]+4.5,rock[1]-2.5),rock,40)
 actual_views=[('toe',eye(9,-28),(3.7,-29,.6),38),('toe-crouch',eye(9,-28,.95),(3.7,-29,.6),38),views[1]]
 for name,pos,target,lens in actual_views+([('site',(105,-125,100),(31,-42,1),40),rock_view] if phase=='after' else []):
  scene.camera.location=pos;scene.camera.rotation_euler=(Vector(target)-scene.camera.location).to_track_quat('-Z','Y').to_euler();scene.camera.data.lens=lens
  if fill:fill.location=pos;fill.rotation_euler=scene.camera.rotation_euler
  scene.cycles.samples=24;scene.render.resolution_x=1600;scene.render.resolution_y=1066
  file=PRE/f'berm-{phase}-{name}-native-fitout.png';scene.render.filepath=str(file);bpy.ops.render.render(write_still=True)
  captures.append(dict(path=file.relative_to(ROOT).as_posix(),sha256=hashlib.sha256(file.read_bytes()).hexdigest(),kind='Native scene comparison',phase=phase,camera=list(pos),target=list(target),lens=lens))
 assert hashlib.sha256(source.read_bytes()).hexdigest()==expected
(ROOT/'development/levels/verification/hollow-creek-berm-captures.json').write_text(json.dumps(dict(scene=r['scene'],scene_sha256=r['scene_sha256'],captures=captures,runtime_capture=False),indent=2)+'\n',encoding='utf-8')
