import bpy,json,hashlib,collections
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
r=json.loads((ROOT/'development/levels/hollow-creek/green-grass.json').read_text(encoding='utf-8'))
source=ROOT/r['scene'];assert hashlib.sha256(source.read_bytes()).hexdigest()==r['scene_sha256']
bpy.ops.wm.open_mainfile(filepath=str(source))
rows=['WATER_HULL_SECTIONS_1'];sections=[]
for LEVEL in [-2.0849]+[round(-2.08+step*.01,5) for step in range(37)]:
 boat=bpy.data.objects['West moored rowing boat'];boat.data.calc_loop_triangles();segments=[];level=LEVEL
 hull_groups={g.index for g in boat.vertex_groups if g.name.startswith('Continuous double-skin hull')}
 hull_ids={v.index for v in boat.data.vertices if any(a.group in hull_groups and a.weight>.5 for a in v.groups)}
 assert hull_ids,'No actual inner/outer hull source group'
 for tri in boat.data.loop_triangles:
  if not all(i in hull_ids for i in tri.vertices):continue
  points=[boat.matrix_world@boat.data.vertices[i].co for i in tri.vertices];hits=[]
  for a,b in zip(points,points[1:]+points[:1]):
   if (a.z<level)!=(b.z<level):
    t=(level-a.z)/(b.z-a.z);p=a+(b-a)*t;hits.append((round(p.x,4),round(-p.y,4)))
  if len(hits)==2 and hits[0]!=hits[1]:segments.append(tuple(hits))
 # Weld floating-point section endpoints at 0.25mm, far below 45mm hull skin.
 welded=[]
 def weld(p):
  for q in welded:
   if (p[0]-q[0])**2+(p[1]-q[1])**2<.00025**2:return q
  welded.append(p);return p
 segments=[(weld(a),weld(b)) for a,b in segments];segments=[(a,b) for a,b in segments if a!=b]
 neighbors=collections.defaultdict(set)
 for a,b in segments:neighbors[a].add(b);neighbors[b].add(a)
 assert all(len(v)==2 for v in neighbors.values()),('Native hull waterline is not closed',LEVEL,[(k,len(v)) for k,v in neighbors.items() if len(v)!=2])
 remaining=set(neighbors);loops=[]
 while remaining:
  first=min(remaining);loop=[first];previous=None;at=first
  while True:
   nxt=next(p for p in neighbors[at] if p!=previous)
   if nxt==first:break
   assert nxt not in loop;loop.append(nxt);previous,at=at,nxt
  remaining.difference_update(loop);loops.append(loop)
 def area(loop):return abs(sum(a[0]*b[1]-b[0]*a[1] for a,b in zip(loop,loop[1:]+loop[:1]))/2)
 assert len(loops)==2,('Expected outer and inner native hull waterlines',len(loops));interior=min(loops,key=area)
 rows.append('\t'.join(map(str,[LEVEL]+[v for p in interior for v in p])))
 sections.append(dict(y=LEVEL,vertices=len(interior),inner_area_m2=area(interior),outer_area_m2=max(map(area,loops))))
p=ROOT/'assets/runtime/sites/hollow-creek-water-hull.tsv';p.write_text('\n'.join(rows)+'\n',encoding='utf-8')
report=dict(source=r['scene'],source_sha256=r['scene_sha256'],hull_object=boat.name,section_step_m=.01,conservative_mask='Union adjacent actual inner sections; <=10mm vertical bracket; retained inside actual double-skin shell.',profile_sha256=hashlib.sha256(p.read_bytes()).hexdigest(),sections=sections)
(ROOT/'development/levels/hollow-creek/water-hull.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
assert hashlib.sha256(source.read_bytes()).hexdigest()==r['scene_sha256'];print('PASS actual inner hull profiles',len(sections))
