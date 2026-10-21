"""Native construction checks and conservative unsupported-island audit; inspect flags visually."""
import bpy,json,math
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
reports=[]
def union(boxes):return [[min(b[0][i] for b in boxes) for i in range(3)],[max(b[1][i] for b in boxes) for i in range(3)]]
def gap(a,b):return math.sqrt(sum(max(0,a[0][i]-b[1][i],b[0][i]-a[1][i])**2 for i in range(3)))
def bounds(obj):
    groups={g.index:g.name for g in obj.vertex_groups};points={n:[] for n in groups.values()}
    for v in obj.data.vertices:
        for g in v.groups:
            if g.weight>.5:points[groups[g.group]].append(v.co)
    return {name:[[min(v[i] for v in pp) for i in range(3)],[max(v[i] for v in pp) for i in range(3)]] for name,pp in points.items() if pp}
def part(groups,prefix):
    found=[b for n,b in groups.items() if n.startswith(prefix)];assert found,prefix
    return union(found)
assets=[json.loads(p.read_text()) for p in sorted((ROOT/'development/levels/production').glob('*.json'))]
assets += [a for a in json.loads((ROOT/'development/levels/story-detail-assets.json').read_text())['assets'] if a['id']=='level_trace_bedroll']
for a in assets:
    bpy.ops.wm.open_mainfile(filepath=str(ROOT/a['editable']));o=bpy.data.objects[a['id']];g=bounds(o);names=list(g);edges={n:set() for n in names}
    for i,n in enumerate(names):
        for other in names[i+1:]:
            if gap(g[n],g[other])<=.006:edges[n].add(other);edges[other].add(n)
    unseen=set(names);islands=[]
    while unseen:
        found=set();queue=[next(iter(unseen))]
        while queue:
            n=queue.pop()
            if n in found:continue
            found.add(n);queue.extend(edges[n]-found)
        unseen-=found;islands.append(found)
    islands.sort(key=len,reverse=True)
    floor_islands=[s for s in islands[1:] if min(g[n][0][2] for n in s)<.004]
    flags=[dict(parts=sorted(s),nearest_gap_m=round(min(gap(g[n],g[m]) for n in s for m in names if m not in s),5)) for s in islands[1:] if s not in floor_islands]
    checks=[]
    if a['id']=='level_work_camp_sign':
        assert gap(part(g,'Bolted direction arrow'),part(g,'Arrow bearing crossrail'))<.009
        assert gap(part(g,'Arrow bearing crossrail'),part(g,'Old sign bearing post'))==0
        checks.append('arrow bolted to bearing rail attached to posts')
    if a['id']=='level_work_survivor_cookbox':
        bowl=part(g,'Upward concave spoon bowl');handle=part(g,'Connected stamped spoon handle')
        assert gap(bowl,handle)<.001 and bowl[1][2]-bowl[0][2]<.025
        assert abs(bowl[0][2]-part(g,'Loose supported lid')[1][2])<.002
        checks.append('upward concave utensil bowl connected to handle and supported on lid')
    if a['id']=='level_trace_bedroll':
        pillow=part(g,'Separate exposed pillow');bag=part(g,'Sleeping bag quilted shell');mat=part(g,'Recent sleeping mat')
        assert pillow[0][1]>bag[1][1]+.08 and abs(pillow[0][2]-mat[1][2])<.006
        checks.append('pillow supported on mat with head area clear of quilt geometry')
    if a['id']=='level_work_crew_footlocker':
        assert gap(part(g,'Hinge supported open lid'),part(g,'Lid hinge'))<.001
        assert abs(part(g,'Trunk bottom')[0][2])<.001
        checks.append('lid intersects its real hinges and trunk rests at native floor')
    reports.append(dict(asset=a['id'],named_parts=len(g),floor_supported_islands=[sorted(s) for s in floor_islands],explicit_checks=checks,disconnected_islands_to_inspect=flags))
(ROOT/'development/levels/verification/model-construction-audit.json').write_text(json.dumps(dict(status='explicit-checks-pass; conservative island flags require visual inspection',models=reports),indent=2)+'\n')
print('CONSTRUCTION_AUDIT_COMPLETE',len(reports),flush=True)
