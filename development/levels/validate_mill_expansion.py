"""Reopen original sources, verify construction supports, then exact expanded level.
Run after verify_mill_expansion.py and Blender --python _mill_scene.py -- blackpine.
"""
import bpy, json, hashlib
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
def part(obj,prefix):
    groups={g.index for g in obj.vertex_groups if g.name.startswith(prefix)}
    points=[v.co for v in obj.data.vertices if any(g.group in groups and g.weight>.5 for g in v.groups)]
    assert points,prefix
    return [[min(v[i] for v in points) for i in range(3)],[max(v[i] for v in points) for i in range(3)]]
def gap(a,b):return sum(max(0,a[0][i]-b[1][i],b[0][i]-a[1][i])**2 for i in range(3))**.5
checks=[]
for name in ['dust_extractor','timber_rack','safety_station']:
    asset=json.loads((ROOT/f'development/levels/production/{name}.json').read_text())
    glb=ROOT/f"assets/runtime/models/{asset['id']}.glb"
    assert hashlib.sha256(glb.read_bytes()).hexdigest()==asset['sha256']
    bpy.ops.wm.open_mainfile(filepath=str(ROOT/asset['editable']))
    obj=bpy.data.objects[asset['id']];obj.data.calc_loop_triangles()
    assert len(obj.data.loop_triangles)==asset['triangles'] and obj.data.uv_layers
    assert abs(min(v.co.z for v in obj.data.vertices))<.0001
    assert all(im.packed_file for im in bpy.data.images if im.source=='FILE')
    if name=='dust_extractor':
        assert gap(part(obj,'Collection sack support shelf'),part(obj,'Heavy dust sack'))<.001
        assert gap(part(obj,'Heavy dust sack'),part(obj,'Separator hopper'))<.001
        assert gap(part(obj,'Fan bearing deck side'),part(obj,'Bolted stand upright'))<.001
        assert gap(part(obj,'Pleated upper filter'),part(obj,'Separator hopper'))<.001
    elif name=='timber_rack':
        assert gap(part(obj,'Old stock label'),part(obj,'Stock label bearing rail'))<.001
        assert gap(part(obj,'Stock label bearing rail'),part(obj,'Rack upright'))<.001
        assert gap(part(obj,'Air drying sticker'),part(obj,'Sorted sawn timber'))<.001
        assert gap(part(obj,'Second separated timber course'),part(obj,'Air drying sticker'))<.001
    else:
        assert gap(part(obj,'Station shelf'),part(obj,'Shelf support strut'))<.001
        assert gap(part(obj,'First aid cabinet'),part(obj,'Station back panel'))<.001
        assert gap(part(obj,'Folded old blanket'),part(obj,'Station shelf'))<.005
        assert gap(part(obj,'Expired inspection card'),part(obj,'Station back panel'))<.001
    checks.append(dict(id=asset['id'],triangles=asset['triangles'],hash=asset['sha256'],native_floor_pivot=True,uv_and_packed_images=True,named_construction_supports=True))
source=ROOT/'build/level-verification/scenes/blackpine'
definition=json.loads((source/'definition.json').read_text())
path=ROOT/'development/levels/blender/blackpine-mill-expanded.blend'
bpy.ops.wm.open_mainfile(filepath=str(path))
assert bpy.context.scene['revision']==definition['revision']
triangles=sum(len(o.data.polygons) for o in bpy.context.scene.objects if o.type=='MESH' and o.get('source','').startswith('Exact runtime'))
assert triangles==len(json.loads((source/'geometry.json').read_text()))
for socket in definition['sockets']:
    obj=bpy.data.objects[socket['id']]
    assert all(abs(a-b)<5e-5 for a,b in zip(obj.location,(socket['x'],-socket['z'],socket['y'])))
assert all(im.packed_file for im in bpy.data.images if im.source=='FILE')
result=dict(status='pass',assets=checks,level_revision=definition['revision'],exact_runtime_triangles=triangles,all_socket_transforms_match=True,packed_scene_images=True,scene_sha256=hashlib.sha256(path.read_bytes()).hexdigest())
(ROOT/'development/levels/verification/blackpine-mill-native.json').write_text(json.dumps(result,indent=2)+'\n')
print('PASS three source meshes and named construction supports; expanded scene triangles, sockets, packed textures and hash',flush=True)
