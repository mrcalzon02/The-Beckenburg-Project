"""Reopen the first Blackpine graybox and verify it against exact runtime exports."""
import bpy,json,hashlib
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
source=ROOT/'build/level-verification/scenes/blackpine'
path=ROOT/'development/levels/blender/blackpine-graybox.blend'
definition=json.loads((source/'definition.json').read_text())
bpy.ops.wm.open_mainfile(filepath=str(path))
scene=bpy.context.scene
assert scene['level_id']=='blackpine' and scene['revision']==definition['revision']
assert scene.unit_settings.system=='METRIC' and scene.unit_settings.scale_length==1
triangles=sum(len(o.data.polygons) for o in scene.objects if o.type=='MESH' and o.get('source','').startswith('Exact runtime'))
assert triangles==len(json.loads((source/'geometry.json').read_text()))
for socket in definition['sockets']:
    obj=bpy.data.objects[socket['id']]
    assert all(abs(a-b)<1e-5 for a,b in zip(obj.location,(socket['x'],-socket['z'],socket['y'])))
assert sum(1 for s in definition['sockets'] if s['type']=='outdoor-hide')==3
assert len({s['room'] for s in definition['sockets'] if s['type']=='ritual'})==3
assert all(image.packed_file for image in bpy.data.images if image.source=='FILE')
result={'status':'graybox-source-checks-pass','level':'blackpine','revision':definition['revision'],'runtime_triangles':triangles,'space_count':len(definition['rooms']),'outdoor_refuges':3,'distinct_rite_rooms':3,'all_socket_transforms_match':True,'all_diffuse_images_packed':True,'blend_sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'limitations':['Not a selectable production map.','No native interactive Windows walkthrough or target-hardware performance acceptance.']}
out=ROOT/'development/levels/verification/blackpine-scene.json';out.write_text(json.dumps(result,indent=2)+'\n')
print('PASS Blackpine editable scene matches exact runtime geometry and socket coordinates')
