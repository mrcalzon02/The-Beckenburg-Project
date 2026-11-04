"""Independent GLB reimport contact audit, including a detached-helmet negative."""
from pathlib import Path
import bpy,bmesh,importlib.util,hashlib

ROOT=Path(__file__).resolve().parent
spec=importlib.util.spec_from_file_location('armour',ROOT/'castle_armour_lowpoly.py')
armour=importlib.util.module_from_spec(spec);spec.loader.exec_module(armour)
path=ROOT/'castle-museum/exports/level_castle_armour_display.glb'
digest=hashlib.sha256(path.read_bytes()).hexdigest()
bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.ops.import_scene.gltf(filepath=str(path))
objects=[o for o in bpy.context.scene.objects if o.type=='MESH']
assert len(objects)==1,'Expected one atlas mesh; audit all transformed objects if export changes'
mesh=objects[0].data.copy()
# UV/normal seams duplicate vertices in glTF. Merge exact positions on an audit
# copy only; source bytes and the visible UV/normal data are never modified.
bm=bmesh.new();bm.from_mesh(mesh);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=.000001);bm.to_mesh(mesh);bm.free()
mesh.calc_loop_triangles()
positive=armour.audit_contacts(mesh,'export',digest)
negative=mesh.copy();links=[set() for v in negative.vertices]
for e in negative.edges:
    a,b=e.vertices;links[a].add(b);links[b].add(a)
remaining=set(range(len(links)));moved=0
while remaining:
    i=remaining.pop();group={i};queue=[i]
    while queue:
        for j in links[queue.pop()]:
            if j in remaining:remaining.remove(j);group.add(j);queue.append(j)
    if min(negative.vertices[j].co.z for j in group)>1.74:
        for j in group:negative.vertices[j].co.z+=.15
        moved+=len(group)
assert moved>20,'Negative control did not select actual helmet components'
negative.calc_loop_triangles()
try:
    armour.audit_contacts(negative,'negative-detached-helmet',digest)
except RuntimeError as expected:
    assert 'Detached armour components' in str(expected)
else:raise AssertionError('Contact audit accepted an actually detached helmet')
assert hashlib.sha256(path.read_bytes()).hexdigest()==digest
print('PASS exported GLB:',positive['triangle_count'],'triangles;',len(positive['components']),'components supported within1mm; actual detached-helmet negative rejected; original artifact unchanged')
