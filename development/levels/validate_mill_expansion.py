"""Reopen original sources, verify construction supports, then exact expanded level.
Run after verify_mill_expansion.py and Blender --python _mill_scene.py -- blackpine.
"""
import bpy, json, hashlib, collections
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
def part(obj,prefix):
    groups={g.index for g in obj.vertex_groups if g.name.startswith(prefix)}
    points=[v.co for v in obj.data.vertices if any(g.group in groups and g.weight>.5 for g in v.groups)]
    assert points,prefix
    return [[min(v[i] for v in points) for i in range(3)],[max(v[i] for v in points) for i in range(3)]]
def gap(a,b):return sum(max(0,a[0][i]-b[1][i],b[0][i]-a[1][i])**2 for i in range(3))**.5
checks=[]
runtime=json.loads((ROOT/'development/levels/verification/blackpine-mill-expansion.json').read_text())
for name in ['dust_extractor','timber_rack','safety_station','circular_headrig','log_carriage','bandsaw','repair_bench','crew_footlocker','hearth_tools','boot_rack','survivor_cookbox','camp_sign']:
    asset=json.loads((ROOT/f'development/levels/production/{name}.json').read_text())
    glb=ROOT/f"assets/runtime/models/{asset['id']}.glb"
    assert hashlib.sha256(glb.read_bytes()).hexdigest()==asset['sha256']
    assert runtime['runtime_models'][asset['id']]==asset['sha256'], 'runtime package must match native asset'
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
    elif name=='safety_station':
        assert gap(part(obj,'Station shelf'),part(obj,'Shelf support strut'))<.001
        assert gap(part(obj,'First aid cabinet'),part(obj,'Station back panel'))<.001
        assert gap(part(obj,'Folded old blanket'),part(obj,'Station shelf'))<.005
        assert gap(part(obj,'Expired inspection card'),part(obj,'Station back panel'))<.001
    elif name=='circular_headrig':
        assert gap(part(obj,'Head saw arbor'),part(obj,'Split arbor bearing'))<.001
        assert gap(part(obj,'Arbor bearing pedestal'),part(obj,'Arbor bearing crosshead'))<.001
        blade=part(obj,'Two metre circular head saw')
        assert blade[1][2]-blade[0][2]>2.0 and blade[1][0]-blade[0][0]<.025
        assert abs((blade[0][0]+blade[1][0])/2+.95)<.0001
        arbor_top=part(obj,'Head saw arbor')[1][2]
    elif name=='log_carriage':
        stock=part(obj,'Partly milled log')
        assert abs(stock[0][0]+.95)<.0001
        assert arbor_top<stock[0][2]-.05
        assert part(obj,'Oak cross bearer')[0][0]>-.95+.1
        assert abs(part(obj,'Oak cross bearer')[1][2]-stock[0][2])<.001
    elif name=='bandsaw':
        assert max(v.co.z for v in obj.data.vertices)<2.5
    # Check actual packed pixels: metal must not contain recurring gray rows.
    metal_checks=[]
    for mat in obj.data.materials:
        if not any(mat.name.startswith('Rebuild Original '+key) for key in ['iron','steel','rust','brass']):continue
        im=next(n.image for n in mat.node_tree.nodes if n.type=='TEX_IMAGE')
        pixels=list(im.pixels);w,h=im.size
        row=[sum(sum(pixels[(y*w+x)*4:(y*w+x)*4+3])/3 for x in range(w))/w for y in range(h)]
        mean=sum(row)/len(row)
        contrast=(max(row)-min(row))/mean
        assert contrast<.025,(name,mat.name,'striped metal',contrast)
        metal_checks.append(dict(material=mat.name,relative_row_contrast=round(contrast,5)))
    checks.append(dict(id=asset['id'],triangles=asset['triangles'],hash=asset['sha256'],native_floor_pivot=True,uv_and_packed_images=True,metal_pixel_checks=metal_checks))
asset=json.loads((ROOT/'development/levels/mill-lighting-assets.json').read_text())['assets'][0]
glb=ROOT/'assets/runtime/models/level_mill_suspension.glb'
assert runtime['runtime_models'][asset['id']]==asset['sha256']==hashlib.sha256(glb.read_bytes()).hexdigest()
bpy.ops.wm.open_mainfile(filepath=str(ROOT/asset['editable']))
obj=bpy.data.objects[asset['id']];obj.data.calc_loop_triangles()
assert len(obj.data.loop_triangles)==asset['triangles']
assert abs(min(v.co.z for v in obj.data.vertices)+1.4)<.0001 and abs(max(v.co.z for v in obj.data.vertices))<.0001
assert gap(part(obj,'Fixture bearing crossbar'),part(obj,'Threaded suspension drop'))<.001
assert all(im.packed_file for im in bpy.data.images if im.source=='FILE')
checks.append(dict(id=asset['id'],triangles=asset['triangles'],hash=asset['sha256'],ceiling_origin_preserved=True,fixture_mount_below_ceiling=1.4))
source=ROOT/'build/level-verification/scenes/blackpine'
definition=json.loads((source/'definition.json').read_text())
path=ROOT/'development/levels/blender/blackpine-mill-expanded.blend'
bpy.ops.wm.open_mainfile(filepath=str(path))
assert bpy.context.scene['revision']==definition['revision']
triangles=sum(len(o.data.polygons) for o in bpy.context.scene.objects if o.type=='MESH' and o.get('source','').startswith('Exact runtime'))
assert triangles==len(json.loads((source/'geometry.json').read_text()))
groups=collections.defaultdict(list)
for f in json.loads((source/'geometry.json').read_text()):groups[(f['room'],f['material'],f['color'])].append(f)
for obj in bpy.context.scene.objects:
    if obj.type!='MESH' or not obj.get('source','').startswith('Exact runtime'):continue
    tokens=obj.data.materials[0].name.split();mid=int(tokens[2]);color=int(tokens[4].split('.')[0],16)
    faces=groups[(obj['zone'],mid,color)]
    assert len(obj.data.vertices)==len(faces)*3
    for i,face in enumerate(faces):
        for j,key in enumerate(['a','b','c']):
            p=face[key];expected=(p['x'],-p['z'],p['y'])
            assert all(abs(a-b)<5e-5 for a,b in zip(obj.data.vertices[i*3+j].co,expected)),obj.name
            uv=obj.data.uv_layers.active.data[i*3+j].uv
            assert abs(uv.x-face[key+'u'])<1e-5 and abs(uv.y-face[key+'v'])<1e-5
for socket in definition['sockets']:
    obj=bpy.data.objects[socket['id']]
    assert all(abs(a-b)<5e-5 for a,b in zip(obj.location,(socket['x'],-socket['z'],socket['y'])))
assert all(im.packed_file for im in bpy.data.images if im.source=='FILE')
result=dict(status='pass',assets=checks,level_revision=definition['revision'],exact_runtime_triangles=triangles,all_triangle_vertices_and_uvs_match=True,all_socket_transforms_match=True,packed_scene_images=True,scene_sha256=hashlib.sha256(path.read_bytes()).hexdigest())
(ROOT/'development/levels/verification/blackpine-mill-native.json').write_text(json.dumps(result,indent=2)+'\n')
print('PASS twelve working-place meshes plus ceiling suspension, non-striped metal pixels, native mounting and exact expanded scene',flush=True)
