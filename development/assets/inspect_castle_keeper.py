"""Independent shipped export/contact/UV and fresh editable-source inspection."""
from pathlib import Path
import bpy,bmesh,json,hashlib
ROOT=Path(__file__).resolve().parents[2];out=ROOT/'development/assets/castle-museum'
path=ROOT/'assets/runtime/models/level_castle_keeper_statue.glb';digest=hashlib.sha256(path.read_bytes()).hexdigest()
assert path.read_bytes()==(out/'exports'/path.name).read_bytes()
bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(path))
objects=[o for o in bpy.data.objects if o.type=='MESH'];assert len(objects)==1
o=objects[0];mesh=o.data.copy();bm=bmesh.new();bm.from_mesh(mesh);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=1e-6);bm.to_mesh(mesh);bm.free()
def audit(m):
    bm=bmesh.new();bm.from_mesh(m)
    assert all(e.is_manifold for e in bm.edges),'Open carving joins'
    remaining=set(bm.verts);groups=0
    while remaining:
        q=[remaining.pop()];groups+=1
        while q:
            for e in q.pop().link_edges:
                for v in e.verts:
                    if v in remaining:remaining.remove(v);q.append(v)
    bm.free();assert groups==1,'Detached carved pieces'
    assert abs(min(v.co.z for v in m.vertices))<1e-5,'Floating stone plinth'
    m.calc_loop_triangles();assert len(m.loop_triangles)<=1100
    return {'connected_components':groups,'triangles':len(m.loop_triangles),'nonmanifold_edges':0}
positive=audit(mesh);negative=mesh.copy()
for v in negative.vertices:v.co.z+=.15
try:audit(negative)
except AssertionError as e:assert 'Floating stone plinth' in str(e)
else:raise AssertionError('Actual lifted carving accepted')
assert len(o.data.uv_layers)==1,'Inherited UV map precedes authored UVs'
for uv in o.data.uv_layers.active.data:assert 0<=uv.uv.x<=1 and 0<=uv.uv.y<=1
bpy.ops.wm.open_mainfile(filepath=str(out/'keeper_finished.blend'))
packed=[]
for o in bpy.context.scene.objects:
    if o.type!='MESH':continue
    for m in o.data.materials:
        for node in m.node_tree.nodes:
            if node.type=='TEX_IMAGE':
                image=node.image;assert image.packed_file and image.packed_file.size>0 and len(image.pixels)==512*512*4;packed.append(image.name)
assert len(packed)==1 and hashlib.sha256(path.read_bytes()).hexdigest()==digest
report={'status':'PASS','runtime_sha256':digest,'positive':positive,'actual_raised_15cm_negative':'REJECTED','fresh_packed_images':packed,'one_authored_uv_layer':True}
(out/'keeper-independent-review.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8');print('PASS',report)
