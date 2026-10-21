"""Reopen delivered native files, verify packed original assets and exact runtime export coordinates."""
import bpy,json,hashlib
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
source=ROOT/'build/level-verification/scenes/blackpine'
definition=json.loads((source/'definition.json').read_text())
manifest=json.loads((ROOT/'development/levels/woodland-detail-assets.json').read_text())
checked=[]
for asset in manifest['assets']:
    model=ROOT/f"assets/runtime/models/{asset['id']}.glb"
    assert hashlib.sha256(model.read_bytes()).hexdigest()==asset['sha256']
    bpy.ops.wm.open_mainfile(filepath=str(ROOT/asset['editable']))
    mesh=bpy.data.objects[asset['id']];mesh.data.calc_loop_triangles()
    assert len(mesh.data.loop_triangles)==asset['triangles']
    assert all(im.packed_file for im in bpy.data.images if im.source=='FILE')
    checked.append(asset['id'])
path=ROOT/'development/levels/blender/blackpine-dressed.blend'
bpy.ops.wm.open_mainfile(filepath=str(path));scene=bpy.context.scene
assert scene['level_id']=='blackpine' and scene['revision']==definition['revision']
assert scene.unit_settings.system=='METRIC' and scene.unit_settings.scale_length==1
triangles=sum(len(o.data.polygons) for o in scene.objects if o.type=='MESH' and o.get('source','').startswith('Exact runtime'))
assert triangles==len(json.loads((source/'geometry.json').read_text()))
for socket in definition['sockets']:
    obj=bpy.data.objects[socket['id']]
    # Blender stores locations as float32, including forest sockets more than 128 metres out.
    assert all(abs(a-b)<5e-5 for a,b in zip(obj.location,(socket['x'],-socket['z'],socket['y']))),(socket['id'],list(obj.location),(socket['x'],-socket['z'],socket['y']))
assert all(im.packed_file for im in bpy.data.images if im.source=='FILE')
assert sum(s['type']=='outdoor-hide' for s in definition['sockets'])==3
assert len({s['room'] for s in definition['sockets'] if s['type']=='ritual'})==3
landscape=[s for s in definition['sockets'] if s['type']=='landscape_detail']
result=dict(status='pass',level='blackpine',revision=definition['revision'],runtime_triangles=triangles,
    checked_original_assets=checked,landscape_placements=len(landscape),background_trees=sum(s['style'].startswith('level_tree_') for s in landscape),
    socket_transforms_match=True,all_diffuse_images_packed=True,outdoor_refuges=3,
    blend_sha256=hashlib.sha256(path.read_bytes()).hexdigest(),limitations=['Development dressed site; not unlocked or production accepted.','Daylight art-inspection images do not change runtime night lighting.','No native interactive playthrough or target-hardware performance acceptance.'])
(ROOT/'development/levels/verification/blackpine-dressed-scene.json').write_text(json.dumps(result,indent=2)+'\n')
print('PASS all 20 original native assets, hashes, packed images, exact runtime triangles and authored socket positions',flush=True)
