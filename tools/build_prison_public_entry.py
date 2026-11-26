"""Bind retained Prison public frames and export nonphysical arrival metadata."""
from pathlib import Path
import hashlib,json
base=Path('development/levels/rebuild')
roster=json.loads((base/'school-electrical-pass/roster.json').read_bytes())
source=next(row for row in roster['active_sources'] if row['id']=='PRISON')
layout=json.loads(Path(source['layout']).read_bytes())
construction=json.loads(Path(source['construction']).read_bytes())
def sha(path):return hashlib.sha256(Path(path).read_bytes()).hexdigest()
assert sha(source['layout'])==construction['layout_sha256']
assert sha(source['glb'])==source['glb_sha256'] and sha(source['native'])==source['native_sha256']
zones={r['id']:r for r in layout['zones']}
out=Path('development/levels/rebuild/prison-public-entry-pass')
out.mkdir(exist_ok=True)
def table(path,rows):
    (out/path).write_bytes(''.join('\t'.join(str(x) for x in row)+'\n' for row in rows).encode())
staging=layout['staging']
rows=[('SOURCE',source['glb'],source['glb_sha256'],source['native'],source['native_sha256']),
    ('DOCK',*staging['point'],staging['yaw'])]
for r in layout['zones']:
    if r['kind']=='staging':
        continue
    rows.append(('SPACE',r['id'],r['owner'],*r['bounds'],r['y'],r['height'],
        str(r['kind']=='outside').lower(),str(r['gameplay_eligible']).lower()))
rows.extend(('RITUAL',r['rite'].lower(),zones[r['zone']]['owner'],*r['point']) for r in layout['rituals'])
arrival=next(row for row in layout['links'] if row['id']=='L066')
rows.extend(('POINT',*r) for r in arrival['points'][1:])
table('arrival.tsv',rows)
choices=[(450,'PRISON-z-68-14','reception','entry',2,1,1),
         (451,'PRISON-x-18-65','reception','records',1,1,1)]
fitted=[]
for id,key,a,b,leaves,hinge,swing in choices:
    door=next(row for row in layout['authored_doorways'] if row['id']==key)
    x,z=(door['along'],door['boundary']) if door['axis']=='z' else (door['boundary'],door['along'])
    fitted.append((id,key,a,b,zones[a]['owner'],zones[b]['owner'],x,door['sill_y'],z,
        door['clear_width'],door['axis'],*door['frame_span'],door['clear_height'],leaves,
        hinge,swing,sha('assets/runtime/models/door_1_loose.glb')))
table('door-survey.tsv',fitted)
print('PRISON public frames',[(r[1],r[6:14]) for r in fitted])
assert 'L111 support 0' in construction['study_solid_removal']
assert 'L066 support 0' in construction['study_solid_removal']
