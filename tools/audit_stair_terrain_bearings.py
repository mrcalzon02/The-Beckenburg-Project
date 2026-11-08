"""Measure preserved foundation feet against the actual changed terrain."""
from pathlib import Path
import json,sys
from mathutils import Vector
from mathutils.bvhtree import BVHTree
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
from export_roster_collision import native_faces
from check_roster_decks import read_parts
from review_roster_macroterrain import sha
BASE=ROOT/(sys.argv[sys.argv.index('--root')+1] if '--root' in sys.argv else 'development/levels/rebuild/stair-terrain-pass')
def tree(path):
    points=[];faces=[]
    for xyz,flags in native_faces(path):
        start=len(points);points.extend(tuple(xyz[i:i+3]) for i in [0,3,6]);faces.append((start,start+1,start+2))
    return BVHTree.FromPolygons(points,faces)
rows=[]
for p in sorted(BASE.glob('*-world.json')):
    w=json.loads(p.read_text());a=tree(ROOT/w['source_world']/'terrain-physics.hcs2');b=tree(ROOT/w['world']/'terrain-physics.hcs2');program=json.loads((ROOT/'development/levels/rebuild/terrain-fit-pass'/w['id'].lower()/'construction.json').read_text());assembly=BASE/'assemblies'/w['id'].lower();meta=json.loads((assembly/'construction.json').read_text());extensions={e['source_bearing']:e for e in meta.get('bearing_extensions',[])};parts=read_parts(assembly/'benched-terrain.hcs2');samples=[];new_gaps=[]
    for part in program['construction']:
        points=part.get('terrain_probe_points',[]);bottom=part.get('bottom_y')
        if bottom is None:continue
        for i,(x,z) in enumerate(points):
            foot=bottom[min(i,len(bottom)-1)] if isinstance(bottom,list) else bottom;old=a.ray_cast(Vector((x,40,z)),Vector((0,-1,0)),80)[0];new=b.ray_cast(Vector((x,40,z)),Vector((0,-1,0)),80)[0];prior=old is not None and old.y>=foot-.02;original_foot=foot
            if part['name'] in extensions:
                extension=extensions[part['name']];rs=parts[extension['name']][1];values=[v[j+1] for v,flags in rs for j in [0,3,6] if abs(v[j]-x)<.0001 and abs(v[j+2]-z)<.0001];assert values,(w['id'],part['name'],'extension has no actual probe-edge vertex');foot=min(values)
            gap=prior and (new is None or new.y<foot-.02);row=dict(part=part['name'],point=[x,foot,z],original_foot_y=original_foot,before_terrain_y=None if old is None else old.y,after_terrain_y=None if new is None else new.y,new_exposed_footing=gap)
            samples.append(row)
            if gap:new_gaps.append(row)
    rows.append(dict(id=w['id'],samples=samples,new_exposed_footing_samples=len(new_gaps),affected_parts=sorted({r['part'] for r in new_gaps})));print('TERRAIN_BEARING_FIT',w['id'],len(samples),len(new_gaps),flush=True)
(BASE/'verification/bearing-contacts.json').write_bytes((json.dumps(dict(status='MEASURED_BEARING_FIT_FINDINGS_NOT_TERRAIN_ACCEPTANCE',sites=rows,auditor_sha256=sha(Path(__file__)),scope='Actual before/after soil rays at prior authored footing probes; original source feet retained; actual new native extension vertices used where present. New gaps require individual load-path/support revisions, not a gameplay or terrain acceptance claim.'),indent=2)+'\n').encode())
