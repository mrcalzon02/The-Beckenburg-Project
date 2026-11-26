"""Adopt measured circulation on unchanged native assets, with explicit provenance.
This authors layout source, not a runtime unlock or a geometry acceptance claim.
"""
from pathlib import Path
import collections,copy,hashlib,json,math,shutil
ROOT=Path(__file__).resolve().parents[1]
BASE=ROOT/'development/levels/rebuild';OUT=BASE/'access-route-pass'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def write(p,d):p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes((json.dumps(d,indent=2)+'\n').encode())
def samples(points):
 for a,b in zip(points,points[1:]):
  n=max(1,math.ceil(math.dist(a,b)/.1))
  for i in range(n+1):yield [a[k]+(b[k]-a[k])*i/n for k in range(3)]
def inside(p,z,margin=.3):
 a,b,c,d=z['bounds'];return a-margin<=p[0]<=c+margin and b-margin<=p[2]<=d+margin
def main():
 baseline=BASE/'flat-terrain-pass/roster.json';roster=json.loads(baseline.read_text());sources={r['id']:r for r in roster['active_sources']}
 reports=[BASE/'circulation-layout-pass/verification/design-summary.json',BASE/'room-access-pass/verification/summary.json']
 proposals=json.loads(reports[0].read_text())['changes']+json.loads(reports[1].read_text())['proposals'];by_site=collections.defaultdict(list);rejected=[]
 for proposal in proposals:
  site=proposal['site'];source=sources[site];data=json.loads((ROOT/source['layout']).read_text());link=next(l for l in data['links'] if l['id']==proposal['link']);zones={z['id']:z for z in data['zones']};new=proposal['after']
  assert proposal['before']==link['points'] and proposal['width']==link['width']
  assert all(abs(p[1]-new[0][1])<1e-8 for p in new)
  assert all(math.dist(a,b)>.001 for a,b in zip(new,new[1:]))
  assert link.get('threshold') is None or link['threshold'] in new
  assert all(zones[k].get('player_access')!='SEALED_CONTEXT' for k in [link['a'],link['b']])
  for key,p,old in [(link['a'],new[0],link['points'][0]),(link['b'],new[-1],link['points'][-1])]:
   assert inside(p,zones[key])
   if zones[key]['kind']=='staging':assert p==old
  outside=[p for p in samples(new) if not any(inside(p,zones[k]) for k in [link['a'],link['b']])]
  if outside:
   rejected.append(dict(site=site,link=link['id'],reason='PATH_LEAVES_INTENDED_ROOM_PAIR',outside_samples=len(outside),proposal=proposal));continue
  by_site[site].append(proposal)
 active=[];decisions=[];accepted=[]
 for site,source in sources.items():
  if site not in by_site:
   active.append(source);decisions.append(dict(id=site,decision='RETAIN_CURRENT_SOURCE_PENDING_CONSTRUCTION',active_source=source));continue
  meta=json.loads((ROOT/source['construction']).read_text());data=json.loads((ROOT/source['layout']).read_text());old=copy.deepcopy(data);folder=OUT/'assemblies'/site.lower()
  folder.mkdir(parents=True,exist_ok=True)
  for key,ext in [('glb','glb'),('native','hcs2'),('blend','blend')]:
   p=ROOT/source[key];assert p.exists()
   if key!='blend':assert sha(p)==source[key+'_sha256']
   shutil.copyfile(p,folder/('access-routes.'+ext));assert sha(p)==sha(folder/('access-routes.'+ext))
  for change in by_site[site]:
   link=next(l for l in data['links'] if l['id']==change['link']);link['points']=change['after'];accepted.append(change)
  # Only the declared link points change. Functional sockets/rooms/spawns remain
  # separate authored contracts; this does not claim every socket is reachable.
  restored=copy.deepcopy(data)
  for change in by_site[site]:next(l for l in restored['links'] if l['id']==change['link'])['points']=change['before']
  assert restored==old
  write(folder/'authored-layout.json',data)
  meta.update(status='AUTHORED_NATIVE_ROOM_ACCESS_ROUTES_NOT_RUNTIME_ACCEPTANCE',source_contract=source['construction'],source_contract_sha256=sha(ROOT/source['construction']),source_glb=source['glb'],source_glb_sha256=sha(ROOT/source['glb']),source_physics=source['native'],source_physics_sha256=sha(ROOT/source['native']),before_layout=source['layout'],before_layout_sha256=sha(ROOT/source['layout']),before_segment_map=meta.get('source_segment_map',{}),before_stair_components=meta['stair_components'],layout=(folder/'authored-layout.json').relative_to(ROOT).as_posix(),layout_sha256=sha(folder/'authored-layout.json'),glb_sha256=sha(folder/'access-routes.glb'),physics_sha256=sha(folder/'access-routes.hcs2'),blend_sha256=sha(folder/'access-routes.blend'),access_route_changes=by_site[site],geometry_unchanged=True,scope='Existing exact exported mesh, native collision, editable scene, terrain holes, UVs, pivots and all non-link source fields retained. Doorways, widths and staging retained. Room access endpoints may move inside their own room. Independent complete routes and room connection checks remain separate; no runtime unlock.')
  write(folder/'construction.json',meta)
  row=dict(id=site,construction=(folder/'construction.json').relative_to(ROOT).as_posix(),layout=meta['layout'],glb=(folder/'access-routes.glb').relative_to(ROOT).as_posix(),native=(folder/'access-routes.hcs2').relative_to(ROOT).as_posix(),blend=(folder/'access-routes.blend').relative_to(ROOT).as_posix(),glb_sha256=meta['glb_sha256'],native_sha256=meta['physics_sha256']);active.append(row);decisions.append(dict(id=site,decision='AUTHORED_CIRCULATION_REVISED_ON_EXACT_EXISTING_GEOMETRY',changed_links=len(by_site[site]),active_source=row))
 for site in ['CASTLE','HOUSE','WLD-01','WLD-02']:decisions.append(dict(id=site,decision='RETAIN_MATURE_SOURCE_INDIVIDUAL_ACCEPTANCE_OPEN'))
 assert len(decisions)==29 and len(active)==25
 write(OUT/'roster.json',dict(status='AUTHORED_ACCESS_SOURCE_BATCH_PENDING_INDEPENDENT_ACCEPTANCE',sites=decisions,active_sources=active,baseline=baseline.relative_to(ROOT).as_posix(),baseline_sha256=sha(baseline),changed_links=len(accepted),changed_sites=len(by_site)))
 write(OUT/'verification/authoring.json',dict(status='AUTHORED_LAYOUT_SOURCES_ON_BYTE_IDENTICAL_GEOMETRY',accepted=accepted,rejected=rejected,source_hashes={p.relative_to(ROOT).as_posix():sha(p) for p in reports+[Path(__file__)]},scope='Source changes are limited to stated link points. Domain sampled at <=0.1m against own two zones with0.3m wall/door tolerance. Not full graph, visual, runtime or map acceptance.'))
 print('AUTHORED_ACCESS',len(accepted),'links;',len(by_site),'sites;',len(rejected),'domain rejections')
if __name__=='__main__':main()
