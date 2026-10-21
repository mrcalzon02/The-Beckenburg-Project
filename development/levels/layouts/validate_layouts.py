"""Planning checks only. Does not load the game or certify navigation/collision."""
import argparse
import json
import math
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parent
EXPECTED={'house','farmhouse','school','mansion','prison','asylum','academy','college','church','castle'}|{f'wld-{i:02}' for i in range(1,14)}

def contains(zone, p, margin=0):
    a,b,c,d=zone['bounds']
    return abs(p[1]-zone['y'])<.001 and a+margin<=p[0]<=c-margin and b+margin<=p[2]<=d-margin

def validate(d):
    checks=[]
    def check(ok, message):
        if not ok: raise ValueError(d['id']+': '+message)
        checks.append(message)
    zones={z['id']:z for z in d['zones']}
    check(len(zones)==len(d['zones']), 'unique zone IDs')
    check(len(zones)>=12,'at least twelve purposeful spaces')
    for z in zones.values():
        a,b,c,e=z['bounds']
        check(all(math.isfinite(v) for v in [a,b,c,e,z['y'],z['height']]) and c>a and e>b and z['height']>0, 'finite positive volume '+z['id'])
        check(d['site_bounds'][0]<=a<c<=d['site_bounds'][2] and d['site_bounds'][1]<=b<e<=d['site_bounds'][3], 'zone inside site '+z['id'])
    solids=[z for z in zones.values() if z['kind'] not in ('outside','staging')]
    for i,a in enumerate(solids):
        for b in solids[i+1:]:
            if a['y']!=b['y']:continue
            aa,az,ac,ad=a['bounds']; ba,bz,bc,bd=b['bounds']
            check(min(ac,bc)-max(aa,ba)<.001 or min(ad,bd)-max(az,bz)<.001,'no same-level overlap '+a['id']+'/'+b['id'])
    graph={z:set() for z in zones}; ids=set()
    for l in d['links']:
        check(l['id'] not in ids,'unique link '+l['id']); ids.add(l['id'])
        check(l['a'] in zones and l['b'] in zones and l['a']!=l['b'],'valid linked zones '+l['id'])
        pts=l['points']
        check(len(pts)>=2 and all(len(p)==3 and all(math.isfinite(v) for v in p) for p in pts),'finite route '+l['id'])
        check(contains(zones[l['a']],pts[0]) and contains(zones[l['b']],pts[-1]),'route endpoints inside zones '+l['id'])
        check(l['width']>=1.05,'clear route width '+l['id'])
        check((l['kind']=='level')==(zones[l['a']]['y']==zones[l['b']]['y']),'elevation kind '+l['id'])
        if l['kind']=='level':check(all(p[1]==pts[0][1] for p in pts),'level route stays level '+l['id'])
        else:check(l.get('construction') and l.get('rise_m',0)>0,'vertical construction reservation '+l['id'])
        graph[l['a']].add(l['b']);graph[l['b']].add(l['a'])
    reached=set(); pending=[d['staging']['zone']]
    while pending:
        n=pending.pop()
        if n in reached:continue
        reached.add(n);pending.extend(graph[n]-reached)
    check(reached==set(zones),'all spaces connected from staging')
    check(len(d['loops'])>=2,'two declared circulation cycles')
    for loop in d['loops']:
        check(len(loop)>=4 and loop[0]==loop[-1] and len(set(loop[:-1]))==len(loop)-1,'simple closed circulation cycle')
        check(all(b in graph[a] for a,b in zip(loop,loop[1:])),'cycle uses actual graph edges')
    check({r['rite'] for r in d['rituals']}=={'Banishment','Cleansing','Absolution'} and len(d['rituals'])==3,'three ritual types')
    check(len({r['zone'] for r in d['rituals']})==3,'rituals occupy distinct zones')
    for r in d['rituals']:check(contains(zones[r['zone']],r['point'],2.2),'4.4 metre ritual area inside '+r['zone'])
    st=d['staging'];check(contains(zones[st['zone']],st['point'],4),'van staging reserve')
    check(st['point'][1]==0 and math.isfinite(st['yaw']),'ground placement matches existing van interface')
    check(len(st['cargo_route'])>=2 and contains(zones[st['zone']],st['cargo_route'][0]),'cargo ramp route start')
    # Existing transform: world X=x+cos(yaw)*localX-sin(yaw)*localZ, Z=z+sin(yaw)*localX+cos(yaw)*localZ.
    expected=[st['point'][0]+math.sin(st['yaw'])*3.628,0,st['point'][2]-math.cos(st['yaw'])*3.628]
    check(math.dist(expected,st['cargo_route'][0])<.002,'ramp toe follows shared van transform')
    check(all(d[k] for k in ('intent','circulation','systems','campaign','refine','sources')),'complete authoring brief')
    return len(checks)

def main():
    ap=argparse.ArgumentParser();ap.add_argument('--partial',action='store_true');args=ap.parse_args()
    results=[]; folders=set(); map_ids=set()
    for f in sorted(ROOT.glob('*/layout.json')):
        folders.add(f.parent.name);d=json.loads(f.read_text(encoding='utf-8'));count=validate(d)
        assert d['id']==f.parent.name.upper() and d['id'] not in map_ids
        map_ids.add(d['id'])
        ET.parse(f.parent/'plan.svg')
        obj=(f.parent/'blockout.obj').read_text(encoding='utf-8')
        assert obj.count('\no ')==len(d['zones'])
        results.append({'id':d['id'],'zones':len(d['zones']),'checks':count,'result':'pass'})
    missing=sorted(EXPECTED-folders);extra=sorted(folders-EXPECTED)
    assert not extra,extra
    if not args.partial:assert not missing,missing
    catalog=json.loads((ROOT/'catalog.json').read_text(encoding='utf-8'))
    assert len(catalog['maps'])==len(results)
    assert {m['id'] for m in catalog['maps']}==map_ids
    roster=(ROOT.parents[2]/'docs/environments/WILDERNESS_AND_REMOTE_SITE_ROSTER.md').read_text(encoding='utf-8')
    assert all(f'WLD-{i:02}' in roster for i in range(1,14))
    report={'status':'partial-pass' if missing else 'planning-checks-pass','maps':results,'missing':missing,
      'limitations':['No runtime geometry or collision loaded.','Route lines are circulation reservations; shell openings, stair geometry, terrain support and clearance require refinement.','No human/AI ghost traversal, game timings, LAN replication, campaign persistence or performance verified.']}
    (ROOT/'verification.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    print(f'{len(results)} layouts passed planning checks; {len(missing)} still to author. Not runtime acceptance.')

if __name__=='__main__':main()
