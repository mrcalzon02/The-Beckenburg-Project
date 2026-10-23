"""Reopen the authored suspension and the exact exported worklight scene.
Blender --background --factory-startup --python-exit-code 1 --python this.py
"""
import bpy, collections, hashlib, json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
asset=json.loads((ROOT/'development/levels/mill-lighting-assets.json').read_text(encoding='utf-8'))['assets'][0]
meshpath=ROOT/f"assets/runtime/models/{asset['id']}.glb"
assert hashlib.sha256(meshpath.read_bytes()).hexdigest()==asset['sha256']
bpy.ops.wm.open_mainfile(filepath=str(ROOT/asset['editable']))
obj=bpy.data.objects[asset['id']]
obj.data.calc_loop_triangles()
assert len(obj.data.loop_triangles)==asset['triangles'] and obj.data.uv_layers
assert abs(max(v.co.z for v in obj.data.vertices))<1e-5
assert abs(min(v.co.z for v in obj.data.vertices)+1.4)<1e-5
def bounds(prefix):
    groups={g.index for g in obj.vertex_groups if g.name.startswith(prefix)}
    points=[v.co for v in obj.data.vertices if any(g.group in groups and g.weight>.5 for g in v.groups)]
    assert points,prefix
    return [[min(v[i] for v in points) for i in range(3)],[max(v[i] for v in points) for i in range(3)]]
def touch(a,b):
    a,b=bounds(a),bounds(b)
    assert all(max(0,a[0][i]-b[1][i],b[0][i]-a[1][i])<.001 for i in range(3))
for a,b in [('Roof attachment plate','Threaded suspension drop'),('Threaded suspension drop','Fixture bearing crossbar'),('Ceiling junction enclosure','Protected electrical drop'),('Protected electrical drop','Fixture cable terminal'),('Fixture cable terminal','Fixture bearing crossbar')]:touch(a,b)
metal=[]
for mat in obj.data.materials:
    if not any(mat.name.startswith('Rebuild Original '+key) for key in ['iron','steel','brass']):continue
    im=next(n.image for n in mat.node_tree.nodes if n.type=='TEX_IMAGE')
    assert im.packed_file
    w,h=im.size;pixels=list(im.pixels)
    rows=[sum(sum(pixels[(y*w+x)*4:(y*w+x)*4+3])/3 for x in range(w))/w for y in range(h)]
    contrast=(max(rows)-min(rows))/(sum(rows)/len(rows));assert contrast<.025
    metal.append(dict(material=mat.name,relative_row_contrast=round(contrast,5)))
source=ROOT/'build/level-verification/scenes/blackpine'
definition=json.loads((source/'definition.json').read_text(encoding='utf-8'))
faces=json.loads((source/'geometry.json').read_text(encoding='utf-8'))
path=ROOT/'development/levels/blender/blackpine-worklights.blend'
bpy.ops.wm.open_mainfile(filepath=str(path))
assert bpy.context.scene['revision']==definition['revision']
groups=collections.defaultdict(list)
for f in faces:groups[(f['room'],f['material'],f['color'])].append(f)
count=0
for obj in bpy.context.scene.objects:
    if obj.type!='MESH' or not obj.get('source','').startswith('Exact runtime'):continue
    tokens=obj.data.materials[0].name.split();mid=int(tokens[2]);color=int(tokens[4].split('.')[0],16)
    expected=groups.pop((obj['zone'],mid,color))
    assert len(obj.data.vertices)==len(expected)*3 and len(obj.data.polygons)==len(expected)
    count+=len(expected)
    for i,face in enumerate(expected):
        for j,key in enumerate(['a','b','c']):
            p=face[key];position=(p['x'],-p['z'],p['y'])
            assert all(abs(a-b)<5e-5 for a,b in zip(obj.data.vertices[i*3+j].co,position))
            uv=obj.data.uv_layers.active.data[i*3+j].uv
            assert abs(uv.x-face[key+'u'])<1e-5 and abs(uv.y-face[key+'v'])<1e-5
assert not groups and count==len(faces)
for socket in definition['sockets']:
    obj=bpy.data.objects[socket['id']]
    assert all(abs(a-b)<5e-5 for a,b in zip(obj.location,(socket['x'],-socket['z'],socket['y'])))
assert all(im.packed_file for im in bpy.data.images if im.source=='FILE')
report=dict(status='pass',asset_sha256=asset['sha256'],native_triangles=asset['triangles'],ceiling_origin_retained=True,physical_supports_and_electrical_drop_connected=True,metal_pixel_checks=metal,level_revision=definition['revision'],exact_runtime_triangles=count,all_vertices_and_uvs_match=True,all_socket_transforms_match=True,packed_images=True,scene_sha256=hashlib.sha256(path.read_bytes()).hexdigest())
(ROOT/'development/levels/verification/blackpine-worklights-native.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
print('PASS native ceiling pivot, connected hardware/cable, metal pixels and exact runtime scene:',count,'triangles',flush=True)
