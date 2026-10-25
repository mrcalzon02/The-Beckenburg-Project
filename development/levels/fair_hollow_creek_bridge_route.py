"""Repair an independently found28.4% foot-radius slope on protected L14.
Project local terrain heights onto actual triangle-grade constraints. Preserve
XY/topology/UV/materials and all other models; do not raise the runtime grade cap.
"""
import bpy,json,math,hashlib,sys
from pathlib import Path
from mathutils import Vector
import numpy as np
ROOT=Path(__file__).resolve().parents[2];OUT=ROOT/'development/levels/hollow-creek';sys.path.insert(0,str(ROOT/'development/levels'))
r=json.loads((OUT/'grounded-berm.json').read_text(encoding='utf-8'));source=ROOT/r['scene'];assert hashlib.sha256(source.read_bytes()).hexdigest()==r['scene_sha256'];bpy.ops.wm.open_mainfile(filepath=str(source));bpy.context.preferences.filepaths.save_version=0
o=bpy.data.objects['level_camp_terrain_amenities'];o.data=o.data.copy();o.data.calc_loop_triangles();old=np.array([v.co[:] for v in o.data.vertices]);heights=old[:,2].copy();adjustable={i for i,(x,y,z) in enumerate(old) if 54.3<x<55.8 and -15.8<y< -13.5};assert adjustable
representatives={};aliases=[]
for i,p in enumerate(old):aliases.append(representatives.setdefault(tuple(p),i))
adjustable_representatives={aliases[i] for i in adjustable}
constraints=[];worst=0;affected=0
for t in o.data.loop_triangles:
 native_ids=list(t.vertices)
 if not adjustable.intersection(native_ids):continue
 ids=[aliases[i] for i in native_ids];p=old[native_ids,:2];matrix=np.array([p[1]-p[0],p[2]-p[0]])
 if abs(np.linalg.det(matrix))<1e-12:continue
 inv=np.linalg.inv(matrix);D=inv@np.array([[-1,1,0],[-1,0,1]],dtype=float);original=float(np.linalg.norm(D@heights[ids]))
 protected=min(p[:,0])<=55.30 and max(p[:,0])>=54.70 and min(p[:,1])<=-14.0 and max(p[:,1])>=-15.3 and original<=.35
 limit=.19 if protected else max(.19,original+1e-7);mask=np.array([i in adjustable_representatives for i in ids],dtype=float);constraints.append((ids,D,limit,mask,protected));worst=max(worst,original if protected else 0);affected+=protected
for iteration in range(2000):
 violation=0
 for ids,D,limit,mask,protected in constraints:
  gradient=D@heights[ids];grade=float(np.linalg.norm(gradient));excess=grade-limit
  if excess<=1e-10:continue
  derivative=(gradient/grade)@D;derivative*=mask;den=float(derivative@derivative);assert den>1e-16,'Infeasible fixed boundary'
  heights[ids]-=derivative*(excess/den);violation=max(violation,excess)
 if violation<1e-8:break
else:raise AssertionError(('Local grade projection did not converge',violation))
heights=heights[aliases];delta=heights-old[:,2];assert float(np.max(np.abs(delta)))<.03,'Local route fix spread into broad terrain redesign'
changed=[]
for i,v in enumerate(o.data.vertices):
 if abs(delta[i])>1e-9:changed.append(dict(index=i,before=list(v.co),after=[float(v.co.x),float(v.co.y),float(heights[i])]))
 v.co.z=float(heights[i])
assert changed;o.data.update();o['runtime_geometry_source']='level_camp_terrain_route_graded'
target=OUT/'hollow-creek-route-grade.blend';bpy.ops.wm.save_as_mainfile(filepath=str(target),compress=True)
asset=o.copy();asset.data=o.data.copy();asset.name='level_camp_terrain_route_graded';editable=ROOT/'development/levels/blender/level_camp_terrain_route_graded.blend';bpy.data.libraries.write(str(editable),{asset},compress=True);bpy.context.scene.collection.objects.link(asset)
active=asset.data.uv_layers.active.name
for name in [u.name for u in asset.data.uv_layers if u.name!=active]:asset.data.uv_layers.remove(asset.data.uv_layers[name])
bpy.ops.object.select_all(action='DESELECT');asset.select_set(True);bpy.context.view_layer.objects.active=asset;file=ROOT/'assets/runtime/models/level_camp_terrain_route_graded.glb';bpy.ops.export_scene.gltf(filepath=str(file),export_format='GLB',use_selection=True,export_yup=True,export_extras=True);asset.data.calc_loop_triangles();assert len(asset.data.loop_triangles)<50000 and file.stat().st_size<4_000_000
assets=[dict(id=asset.name,editable=editable.relative_to(ROOT).as_posix(),model=file.relative_to(ROOT).as_posix(),triangles=len(asset.data.loop_triangles),bytes=file.stat().st_size,sha256=hashlib.sha256(file.read_bytes()).hexdigest())]
from finalize_earthen_bank_sources import materialize
materialize(assets)
assert hashlib.sha256(source.read_bytes()).hexdigest()==r['scene_sha256']
report=dict(status='local protected-route grading; independent checks required',parent=source.relative_to(ROOT).as_posix(),parent_sha256=r['scene_sha256'],scene=target.relative_to(ROOT).as_posix(),scene_sha256=hashlib.sha256(target.read_bytes()).hexdigest(),route='L14',source_pre_fix_max_grade=worst,target_grade=.19,protected_triangles=int(affected),projection_iterations=iteration+1,changed_vertices=changed,max_height_change_m=float(np.max(np.abs(delta))),assets=assets,runtime_grade_cap_unchanged=.20)
(OUT/'route-grade.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8');print('FAIRED L14',len(changed),'vertices; max height change',report['max_height_change_m'],'m; native grade',worst,'-> .19',flush=True)
