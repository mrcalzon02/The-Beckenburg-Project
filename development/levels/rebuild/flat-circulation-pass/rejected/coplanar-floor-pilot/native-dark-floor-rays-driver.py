from pathlib import Path
import bpy,json,sys
from mathutils import Vector
from mathutils.bvhtree import BVHTree
ROOT=Path.cwd();sys.path.insert(0,str(ROOT/'tools'));from review_roster_macroterrain import sha
source=ROOT/'development/levels/rebuild/flat-circulation-pass/wld-08/open-circulation.glb';bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(source));eye=Vector((23,-19,1.62));target=Vector((18,-19,1.62));rot=(target-eye).to_track_quat('-Z','Y');trees=[]
for obj in bpy.context.scene.objects:
 if obj.type!='MESH':continue
 vertices=[obj.matrix_world@v.co for v in obj.data.vertices];obj.data.calc_loop_triangles();faces=[tuple(t.vertices) for t in obj.data.loop_triangles];trees.append((obj,BVHTree.FromPolygons(vertices,faces)))
rows=[]
for x,y in [(400,450),(500,450),(300,450),(400,500),(500,500),(300,500)]:
 direction=(rot@Vector(((x+.5-400)*36/(25*800),(300-y-.5)*36/(25*800),-1))).normalized();hits=[]
 for obj,tree in trees:
  point,normal,index,distance=tree.ray_cast(eye,direction,100)
  if point is None:continue
  tri=obj.data.loop_triangles[index];mat=obj.data.materials[tri.material_index] if obj.data.materials else None;colour=None
  if mat and mat.use_nodes:
   bsdf=next((n for n in mat.node_tree.nodes if n.type=='BSDF_PRINCIPLED'),None)
   if bsdf:colour=list(bsdf.inputs['Base Color'].default_value)
  hits.append(dict(object=obj.name,role=obj.get('role'),zone=obj.get('zone'),distance=distance,point=list(point),normal=list(normal),material=None if mat is None else mat.name,base_colour=colour))
 hits.sort(key=lambda r:r['distance']);rows.append(dict(pixel=[x,y],ray=list(direction),nearest_hits=hits[:8]))
p=ROOT/'development/levels/rebuild/flat-circulation-pass/verification/native-dark-floor-rays.json';p.write_bytes((json.dumps(dict(source=source.relative_to(ROOT).as_posix(),source_sha256=sha(source),eye=list(eye),target=list(target),pixels=rows,scope='Actual exported mesh intersection/material diagnosis for native dark pixels; no runtime or full optical acceptance'),indent=2)+'\n').encode())
for r in rows:print('DARK_FLOOR_RAY',r['pixel'],[(h['object'],round(h['distance'],6),h['base_colour']) for h in r['nearest_hits'][:3]],flush=True)
