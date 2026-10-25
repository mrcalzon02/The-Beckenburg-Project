"""Vary headings, unfit/refit actual parts and associated bedding to terrain.
Floor clipping is a subsequent required step; this intermediate is not accepted.
"""
import bpy,json,math,hashlib
from pathlib import Path
from mathutils import Vector,Matrix
from mathutils.bvhtree import BVHTree
ROOT=Path(__file__).resolve().parents[2];OUT=ROOT/'development/levels/hollow-creek'
source=OUT/'hollow-creek-siteart.blend';protected=hashlib.sha256(source.read_bytes()).hexdigest();bpy.ops.wm.open_mainfile(filepath=str(source));bpy.context.preferences.filepaths.save_version=0
terrain=bpy.data.objects['level_camp_terrain_amenities'];terrain.data.calc_loop_triangles();tree=BVHTree.FromPolygons([terrain.matrix_world@v.co for v in terrain.data.vertices],[tuple(t.vertices) for t in terrain.data.loop_triangles],all_triangles=True)
def h(p):
 hit=tree.ray_cast(Vector((p.x,p.y,20)),Vector((0,0,-1)),100)[0];assert hit is not None,tuple(p);return hit.z
names=['west-pitched-ridge','east-pitched-dome']+['Additional pitched tent '+str(i) for i in range(1,7)]
degrees=[-23,31,14,-38,42,-18,29,-33];records=[]
for i,(name,angle) in enumerate(zip(names,degrees)):
 tent=bpy.data.objects[name];pivot=tent.location.copy();rotation=Matrix.Rotation(math.radians(angle),4,'Z');before=tuple(tent.rotation_euler)
 attached=['west-rolled-bedding','west-laid-bedding'] if i==0 else ['east-laid-bedding'] if i==1 else ['Additional camp rolled bedding '+str(i-2)]
 for item in [name]+attached:
  o=bpy.data.objects[item];o.data=o.data.copy();matrix=o.matrix_world.copy();old=[matrix@v.co for v in o.data.vertices]
  # Rotate actual geometry around the tent centre, including world-baked bedding.
  target=Matrix.Translation(pivot)@rotation@Matrix.Translation(-pivot)@matrix
  o.matrix_world=target;bpy.context.view_layer.update();inverse=o.matrix_world.inverted()
  for v,p in zip(o.data.vertices,old):
   q=o.matrix_world@v.co;q.z+=h(q)-h(p);v.co=inverse@q
  o['siteart_heading_delta_degrees']=angle
 records.append(dict(name=name,heading_delta_degrees=angle,previous_rotation=list(before),new_rotation=list(tent.rotation_euler),bedding=attached))
target=OUT/'hollow-creek-siteart-oriented.blend';bpy.ops.wm.save_as_mainfile(filepath=str(target),compress=True)
assert hashlib.sha256(source.read_bytes()).hexdigest()==protected
(OUT/'siteart-headings.json').write_text(json.dumps(dict(status='reoriented and refitted; clipping and independent clearance checks required',source=source.relative_to(ROOT).as_posix(),source_sha256=protected,scene=target.relative_to(ROOT).as_posix(),scene_sha256=hashlib.sha256(target.read_bytes()).hexdigest(),instances=records),indent=2)+'\n',encoding='utf-8')
