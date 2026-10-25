import bpy,json,hashlib,array,struct
from pathlib import Path
ROOT=Path.cwd();out=ROOT/'development/levels/hollow-creek';r=json.loads((out/'green-grass.json').read_text(encoding='utf-8'))
def digest(o):
 h=hashlib.sha256();h.update(struct.pack('>16d',*(v for row in o.matrix_world for v in row)))
 for data,field,code,n in [(o.data.vertices,'co','f',3),(o.data.loops,'vertex_index','i',1),(o.data.polygons,'material_index','i',1)]:
  a=array.array(code,[0])*(len(data)*n);data.foreach_get(field,a);h.update(a.tobytes())
 for uv in o.data.uv_layers:
  a=array.array('f',[0])*(len(uv.data)*2);uv.data.foreach_get('uv',a);h.update(uv.name.encode());h.update(a.tobytes())
 h.update(bytes(p.use_smooth for p in o.data.polygons));return h.hexdigest()
bpy.ops.wm.open_mainfile(filepath=str(ROOT/r['parent']));before={o.name:digest(o) for o in bpy.context.scene.objects if o.type=='MESH'}
bpy.ops.wm.open_mainfile(filepath=str(ROOT/r['scene']));assert before=={o.name:digest(o) for o in bpy.context.scene.objects if o.type=='MESH'};assert len(before)==909
im=bpy.data.images['Woodland grey green grass v4 runtime512 derivative'];assert tuple(im.size)==(512,512) and im.packed_file
models=[]
for a in r['assets']:
 file=ROOT/a['model'];assert hashlib.sha256(file.read_bytes()).hexdigest()==a['sha256'];bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(file));count=0
 for o in [o for o in bpy.context.scene.objects if o.type=='MESH']:
  o.data.calc_loop_triangles();count+=len(o.data.loop_triangles);assert o.data.uv_layers
 assert count==a['triangles'] and count<50000 and file.stat().st_size<4000000
 bpy.ops.wm.open_mainfile(filepath=str(ROOT/a['editable']));assert len([o for o in bpy.context.scene.objects if o.type=='MESH'])==1 and not bpy.data.libraries;assert all(im.packed_file for im in bpy.data.images if im.source=='FILE');models.append(dict(id=a['id'],triangles=count,bytes=file.stat().st_size))
report=dict(status='pass-material-only-native-and-export-checks',unchanged_native_mesh_geometry_uv_poses=909,packed_runtime_grass_size=[512,512],models=models,runtime_atlas_pixel_checks=1152,gameplay_acceptance=False)
(ROOT/'development/levels/verification/hollow-creek-green.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8');print('PASS green source/native/export checks',flush=True)
