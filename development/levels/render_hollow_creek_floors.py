"""Actual saved-scene views plus clearly labelled native component diagnostics."""
import bpy,bmesh,json,hashlib,sys
from pathlib import Path
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[2];sys.path.insert(0,str(ROOT/'development/assets'))
import rebuild_support as s
OUT=ROOT/'development/levels/hollow-creek';PRE=ROOT/'development/levels/previews/hollow-creek'
r=json.loads((OUT/'fitted-floors.json').read_text(encoding='utf-8'));source=ROOT/r['scene'];assert hashlib.sha256(source.read_bytes()).hexdigest()==r['scene_sha256']
bpy.ops.wm.open_mainfile(filepath=str(source));captures=[];scene=bpy.context.scene;fill=bpy.data.objects.get('Boundary inspection fill only')
for name,pos,target,lens in [('fitted-west-native-fitout',(13,-40,2),(8.8,-32.8,.7),30),('fitted-east-native-fitout',(46,-49,2),(51,-43,.6),32),('fitted-site-native-fitout',(105,-125,100),(31,-42,1),40)]:
 scene.camera.location=pos;scene.camera.rotation_euler=(Vector(target)-scene.camera.location).to_track_quat('-Z','Y').to_euler();scene.camera.data.lens=lens
 scene.cycles.samples=24;scene.render.resolution_x=1600;scene.render.resolution_y=1066
 if fill:fill.location=pos;fill.rotation_euler=scene.camera.rotation_euler
 path=PRE/(name+'.png');scene.render.filepath=str(path);bpy.ops.render.render(write_still=True)
 captures.append(dict(path=path.relative_to(ROOT).as_posix(),sha256=hashlib.sha256(path.read_bytes()).hexdigest(),kind='Native scene overview'))

for id,name,view in [('level_camp_tent_fitted_east_dome','fitted-floor-east-studio-diagnostic',(3,-4,2)),('level_camp_tent_fitted_west_ridge','fitted-floor-west-studio-diagnostic',(3,-4,.6))]:
 s.reset()
 with bpy.data.libraries.load(str(ROOT/f'development/levels/blender/{id}.blend'),link=False) as (src,dst):dst.objects=[id]
 o=dst.objects[0];bpy.context.scene.collection.objects.link(o);o.data=o.data.copy();gids={g.index for g in o.vertex_groups if 'groundsheet' in g.name.lower() or 'physical floor' in g.name.lower()}
 bm=bmesh.new();bm.from_mesh(o.data);deform=bm.verts.layers.deform.active;bmesh.ops.delete(bm,geom=[v for v in bm.verts if not any(i in v[deform] for i in gids)],context='VERTS');bm.to_mesh(o.data);bm.free()
 wire=o.copy();wire.data=o.data.copy();wire.name='Diagnostic mesh edges only';bpy.context.scene.collection.objects.link(wire)
 mat=bpy.data.materials.new('Inspection overlay gold, not game material');mat.diffuse_color=(.8,.45,.12,1);mat.use_nodes=True;mat.node_tree.nodes['Principled BSDF'].inputs['Base Color'].default_value=(.8,.45,.12,1)
 wire.data.materials.clear();wire.data.materials.append(mat)
 for p in wire.data.polygons:p.material_index=0
 mod=wire.modifiers.new('Diagnostic edge overlay only','WIREFRAME');mod.thickness=.0018;mod.use_replace=True
 path=PRE/(name+'.png');s.render_family(name,[o,wire],path=path,view=view)
 captures.append(dict(path=path.relative_to(ROOT).as_posix(),sha256=hashlib.sha256(path.read_bytes()).hexdigest(),kind='Native component diagnostic',note='Isolated actual floor geometry with gold diagnostic edges; not its in-game material/appearance'))
assert hashlib.sha256(source.read_bytes()).hexdigest()==r['scene_sha256']
report=dict(status='five actual saved-scene/component captures',scene=r['scene'],scene_sha256=r['scene_sha256'],captures=captures,runtime_capture=False)
(ROOT/'development/levels/verification/hollow-creek-fitted-captures.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8');print('CAPTURED3 native site views and2 actual floor mesh diagnostics; source unchanged',flush=True)
