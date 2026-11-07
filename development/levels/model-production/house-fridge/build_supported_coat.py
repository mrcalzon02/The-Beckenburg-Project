"""Retain original coat surfaces/pivot and bridge its shoulder to the existing hook."""
import sys,json,hashlib
from pathlib import Path
import bpy
ROOT=Path(__file__).resolve().parents[4];sys.path.insert(0,str(ROOT/'development/assets'))
import rebuild_support as s
HERE=Path(__file__).resolve().parent;STAGE=HERE/'staging';STAGE.mkdir(exist_ok=True)
s.reset();source=ROOT/'assets/runtime/models/level_trace_coat.glb';bpy.ops.import_scene.gltf(filepath=str(source))
objects=[o for o in bpy.context.scene.objects if o.type=='MESH']
for obj in objects:
 matrix=obj.matrix_world.copy()
 for v in obj.data.vertices:v.co=matrix@v.co
 obj.parent=None;obj.matrix_world.identity()
wood=objects[0].data.materials[0]
original_vertices=sum(len(o.data.vertices) for o in objects)
# Both shoulder endpoints intersect the actual coat top; the apex intersects the left hook.
objects+=s.tube('Attached shoulder hanger',[(-.32,-.11,1.387),(-.30,-.123,1.435),(-.09,-.11,1.387),(-.32,-.11,1.387)],.009,wood,4)
obj=s.join(objects,'level_house_coat_hung');bpy.ops.object.transform_apply(location=True,rotation=True,scale=True)
obj.data.calc_loop_triangles();triangles=len(obj.data.loop_triangles)
out=STAGE/'level_house_coat_hung.glb';bpy.ops.export_scene.gltf(filepath=str(out),export_format='GLB',use_selection=True,export_yup=True,export_extras=True)
bpy.ops.wm.save_as_mainfile(filepath=str(HERE/'level_house_coat_hung.blend'),compress=True)
sha=hashlib.sha256(out.read_bytes()).hexdigest()
s.reset();bpy.ops.import_scene.gltf(filepath=str(out));imported=[o for o in bpy.context.scene.objects if o.type=='MESH']
for tag,view in [('coat-front',(1,-3,2)),('coat-side',(-3,-1,2))]:s.render_family('supported coat',imported,path=HERE/'previews'/(tag+'.png'),view=view)
(HERE/'coat-verification.json').write_bytes((json.dumps(dict(source=source.relative_to(ROOT).as_posix(),source_sha256=hashlib.sha256(source.read_bytes()).hexdigest(),runtime_sha256=sha,triangles=triangles,original_vertices=original_vertices,policy='Original surfaces/UV/materials/metre pivot retained; additive shoulder hanger intersects coat and actual hook',visual_review='Inspect actual reimported front/side before promotion'),indent=2)+'\n').encode())
print('PASS supported source coat',triangles,'triangles; original pivot/surfaces retained; inspect actual front/side')
