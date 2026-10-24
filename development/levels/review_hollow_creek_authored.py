"""Independent source/scene invariance, portable atlas and visual review."""
import bpy, json, hashlib, math, struct
from pathlib import Path
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[2];OUT=ROOT/'development/levels/hollow-creek'
r=json.loads((OUT/'authored-surfaces.json').read_text(encoding='utf-8'))

def signature(o):
    return dict(vertices=[tuple(v.co) for v in o.data.vertices],faces=[tuple(p.vertices) for p in o.data.polygons],
                smooth=[p.use_smooth for p in o.data.polygons],matrix=[tuple(row) for row in o.matrix_world],
                uvs={layer.name:[tuple(v.uv) for v in layer.data] for layer in o.data.uv_layers if layer.name!='AuthoredWearAtlas'})
checks=[]
for a in r['assets']:
    previous=ROOT/f"development/levels/blender/{a['previous_source']}.blend"
    bpy.ops.wm.open_mainfile(filepath=str(previous));old=signature(bpy.data.objects[a['previous_source']])
    bpy.ops.wm.open_mainfile(filepath=str(ROOT/a['editable']));o=bpy.data.objects[a['id']];new=signature(o)
    assert old==new,('Geometry/normals/source UVs changed',a['id'])
    tex=next(q for q in o.data.materials[0].node_tree.nodes if q.type=='TEX_IMAGE')
    assert tex.image.packed_file and list(tex.image.size)==[1024,1024]
    assert all(math.isfinite(c) and -.0001<=c<=1.0001 for uv in o.data.uv_layers['AuthoredWearAtlas'].data for c in uv.uv)
    file=ROOT/f"assets/runtime/models/{a['id']}.glb";data=file.read_bytes();length=struct.unpack_from('<I',data,12)[0];gl=json.loads(data[20:20+length])
    assert gl['materials'][0]['pbrMetallicRoughness']['baseColorTexture'].get('texCoord',0)==0
    assert all('TEXCOORD_0' in p['attributes'] and 'TEXCOORD_1' not in p['attributes'] for m in gl['meshes'] for p in m['primitives'])
    assert len(data)<4000000 and hashlib.sha256(data).hexdigest()==a['sha256']
    bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(file))
    count=0
    for o in bpy.context.scene.objects:
        if o.type!='MESH':continue
        o.data.calc_loop_triangles();count+=len(o.data.loop_triangles)
        assert all(math.isfinite(c) for v in o.data.vertices for c in v.co)
    assert count==a['triangles'] and count<50000
    checks.append(dict(id=a['id'],triangles=count,bytes=len(data),geometry_normals_pivots_and_source_uvs_unchanged=True,packed_atlas=[1024,1024],runtime_uv_channel=0))

bpy.ops.wm.open_mainfile(filepath=str(OUT/'hollow-creek-refined.blend'))
old={o.name:signature(o) for o in bpy.context.scene.objects if o.type=='MESH'}
bpy.ops.wm.open_mainfile(filepath=str(ROOT/r['scene']))
new={o.name:signature(o) for o in bpy.context.scene.objects if o.type=='MESH'}
assert old==new,'Native geometry/support/normal/source UV or placement changed'
for p,h in r['protected_sources'].items():assert hashlib.sha256((ROOT/p).read_bytes()).hexdigest()==h,p

scene=bpy.context.scene;scene.cycles.samples=32;scene.render.resolution_x=1600;scene.render.resolution_y=1066
fill=bpy.data.objects.get('Boundary inspection fill only');captures=[]
for name,pos,target,lens in [
 ('authored-expanded-site-native-fitout',(105,-125,100),(31,-42,1),40),
 ('authored-hearth-native-fitout',(33,-43,2.4),(25.9,-46.1,1.6),28),
 ('authored-dock-native-fitout',(36,-.5,3),(27,6.8,0),32),
 ('authored-toilets-native-fitout',(20,-25,1.8),(12.8,-17.4,0),30),
 ('authored-memorial-native-fitout',(10,-57,2.7),(5,-50.7,1.8),34)]:
    scene.camera.location=pos;scene.camera.rotation_euler=(Vector(target)-scene.camera.location).to_track_quat('-Z','Y').to_euler();scene.camera.data.lens=lens
    if fill:fill.location=pos;fill.rotation_euler=scene.camera.rotation_euler
    path=ROOT/f'development/levels/previews/hollow-creek/{name}.png';scene.render.filepath=str(path);bpy.ops.render.render(write_still=True)
    captures.append(dict(path=path.relative_to(ROOT).as_posix(),sha256=hashlib.sha256(path.read_bytes()).hexdigest(),kind='Native scene overview'))
report=dict(status='pass-source-and-native-surface-only',families=len(checks),fitted_instances=len(r['instances']),unchanged_native_mesh_objects=len(old),assets=checks,
            protected_sources=len(r['protected_sources']),scene_sha256=r['scene_sha256'],captures=captures,
            limitations=['No runtime level unlock, movement, circuits, layered water or human/performance acceptance.','New atlas is diffuse color only; no shader-dependent fake detail.','Tent, shrub, creek-engineering and dock-cable families retain previous refined sources.'])
(ROOT/'development/levels/verification/hollow-creek-authored-surfaces.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
print('PASS',len(checks),'actual source/GLB families;',len(old),'unchanged native meshes; five saved-scene captures',flush=True)
