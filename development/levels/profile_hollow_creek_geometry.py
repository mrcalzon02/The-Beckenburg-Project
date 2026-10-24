"""Native geometry cost inventory, not a runtime FPS or LOD acceptance test."""
import bpy,json,hashlib,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2];fitted='--fitted' in sys.argv;source=ROOT/('development/levels/hollow-creek/hollow-creek-fitted.blend' if fitted else 'development/levels/hollow-creek/hollow-creek-tailored.blend')
bpy.ops.wm.open_mainfile(filepath=str(source));rows=[];floor_total=0
for o in bpy.context.scene.objects:
 if o.type!='MESH':continue
 o.data.calc_loop_triangles();floor=set()
 if o.get('tailored_source','').startswith('level_camp_tent_'):
  ids={g.index for g in o.vertex_groups if 'groundsheet' in g.name.lower() or 'physical floor' in g.name.lower()}
  floor={v.index for v in o.data.vertices if any(a.group in ids and a.weight>.5 for a in v.groups)}
 floor_triangles=sum(all(i in floor for i in t.vertices) for t in o.data.loop_triangles) if floor else 0;floor_total+=floor_triangles
 rows.append(dict(name=o.name,triangles=len(o.data.loop_triangles),vertices=len(o.data.vertices),hidden_in_native_render=o.hide_render,tent_floor_triangles=floor_triangles))
rows.sort(key=lambda r:r['triangles'],reverse=True)
report=dict(status='measured-native-topology-only',scene=source.relative_to(ROOT).as_posix(),scene_sha256=hashlib.sha256(source.read_bytes()).hexdigest(),mesh_objects=len(rows),total_native_triangles=sum(r['triangles'] for r in rows),render_visible_native_triangles=sum(r['triangles'] for r in rows if not r['hidden_in_native_render']),retained_tent_floor_triangles=floor_total,objects=rows,runtime_fps_measured=False,limitations=['Native triangle counts are not runtime performance measurements.','Current groundsheet vertices are intentionally retained exactly in this silhouette milestone.','Before runtime acceptance, profile actual renderer/culling/LOD and replace excessive floor tessellation only with a separately validated terrain-conforming mesh, retaining actual support and shell depth.'])
(ROOT/('development/levels/verification/hollow-creek-fitted-geometry-cost.json' if fitted else 'development/levels/verification/hollow-creek-tailored-geometry-cost.json')).write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
print('NATIVE COST',report['total_native_triangles'],'total;',report['render_visible_native_triangles'],'visible;',floor_total,'retained floor triangles; runtime FPS unmeasured',flush=True)
