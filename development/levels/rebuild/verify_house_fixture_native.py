"""Reopen the actual fitted candidate and compare every runtime triangle and UV."""
import bpy,collections,json,hashlib
from pathlib import Path
ROOT=Path(__file__).resolve().parents[3]
source=ROOT/'build/level-verification/scenes/briar-rebuild-fit'
native=ROOT/'development/levels/blender/briar-rebuild-fit-construction.blend'
digest=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
before=digest(native)
bpy.ops.wm.open_mainfile(filepath=str(native))
scene=bpy.context.scene
definition=json.loads((source/'definition.json').read_text(encoding='utf-8'))
faces=json.loads((source/'geometry.json').read_text(encoding='utf-8'))
assert scene['revision']==definition['revision']
assert 'HouseRebuildFittings.java' in scene['source_of_truth']
assert 'candidate' in scene['production_status']
assert scene.unit_settings.system=='METRIC' and scene.unit_settings.scale_length==1
groups=collections.defaultdict(list)
for face in faces:groups[(face['room'],face['material'],face['color'])].append(face)
meshes=[o for o in scene.objects if o.type=='MESH' and o.get('source','').startswith('Exact runtime')]
assert len(meshes)==len(groups)
def close(a,b):assert len(a)==len(b) and all(abs(x-y)<1e-5 for x,y in zip(a,b)),(a,b)
count=0
for obj in meshes:
 expected=groups[(obj['zone'],obj['runtime_material'],obj['runtime_color'])]
 assert len(obj.data.polygons)==len(expected)
 uv=obj.data.uv_layers.active;assert uv is not None
 for polygon,face in zip(obj.data.polygons,expected):
  assert len(polygon.vertices)==3
  for loop,corner in zip(polygon.loop_indices,['a','b','c']):
   p=face[corner];vertex=obj.matrix_world@obj.data.vertices[obj.data.loops[loop].vertex_index].co
   close(vertex,(p['x'],-p['z'],p['y']));close(uv.data[loop].uv,(face[corner+'u'],face[corner+'v']))
  count+=1
assert count==len(faces)
images=[im for im in bpy.data.images if im.source=='FILE']
assert images and all(im.packed_file for im in images)
assert len(definition['props'])==5
assert sum(o.name.startswith('Portal ') for o in scene.objects)==len(definition['portals'])
assert sum(o.name.startswith('Spawn ') for o in scene.objects)==15
for socket in definition['sockets']:
 close(scene.objects[socket['id']].location,(socket['x'],-socket['z'],socket['y']))
assert digest(native)==before
report=dict(level=definition['id'],revision=definition['revision'],triangles=count,packed_images=len(images),props=5,spawns=15,
 native_sha256=before,definition_sha256=digest(source/'definition.json'),geometry_sha256=digest(source/'geometry.json'),
 result='PASS every source vertex/UV; packed images; original sockets; native file unchanged',acceptance='construction candidate only; terrain/boundary and complete map gates open')
(ROOT/'development/levels/rebuild/house/fixture-fit-native-verification.json').write_bytes((json.dumps(report,indent=2)+'\n').encode('utf-8'))
print('PASS House fitted native parity',count,'triangles',len(images),'packed images; no native resave')
