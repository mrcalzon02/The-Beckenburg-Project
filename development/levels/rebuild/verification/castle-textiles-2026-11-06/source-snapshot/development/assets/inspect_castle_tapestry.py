"""Independent exported wall-contact audit and an actually detached-rail control."""
from pathlib import Path
import bpy,bmesh,importlib.util,hashlib,json
OUT=Path(__file__).with_name('castle-museum');path=OUT/'exports/level_castle_tapestry.glb'
digest=hashlib.sha256(path.read_bytes()).hexdigest()
bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(path))
obj=next(o for o in bpy.context.scene.objects if o.type=='MESH');data=obj.data.copy()
bm=bmesh.new();bm.from_mesh(data);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=.000001);bm.to_mesh(data);bm.free()
assert max(v.co.y for v in data.vertices)<.00001
for v in data.vertices:v.co=(v.co.x,v.co.z,-v.co.y)
data.calc_loop_triangles()
spec=importlib.util.spec_from_file_location('contact',Path(__file__).with_name('castle_armour_lowpoly.py'));contact=importlib.util.module_from_spec(spec);spec.loader.exec_module(contact)
def audit(mesh,phase):
    result=None
    try:result=contact.audit_contacts(mesh,phase,digest)
    except RuntimeError:
        if phase!='tapestry-detached-rail-negative':raise
        result=json.loads((OUT/('armour-contact-audit-'+phase+'.json')).read_text())
    result['criterion']='Rigid audit coordinates(x,z,-y): actual wall planeY=0 maps to supportZ=0; every component must contact the wall plates through real triangle intersections within1mm'
    result['expected']='FAIL' if phase.endswith('negative') else 'PASS'
    (OUT/(phase+'.json')).write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
    assert result['status']==result['expected'],result
    return result
positive=audit(data,'tapestry-independent-wall-export')
negative=data.copy();links=[set() for v in negative.vertices]
for e in negative.edges:
    a,b=e.vertices;links[a].add(b);links[b].add(a)
remaining=set(range(len(links)));moved=0
while remaining:
    i=remaining.pop();group={i};queue=[i]
    while queue:
        for j in links[queue.pop()]:
            if j in remaining:remaining.remove(j);group.add(j);queue.append(j)
    width=max(negative.vertices[j].co.x for j in group)-min(negative.vertices[j].co.x for j in group)
    if width>1.6:
        for j in group:negative.vertices[j].co.z+=.25
        moved+=len(group)
assert moved>=12,'Actual exported rail was not selected'
negative.calc_loop_triangles();failed=audit(negative,'tapestry-detached-rail-negative')
assert hashlib.sha256(path.read_bytes()).hexdigest()==digest
print('PASS exported tapestry:',positive['triangle_count'],'triangles;',len(positive['components']),'physically supported components; actual rail moved25cm from wall is rejected; source bytes untouched')
