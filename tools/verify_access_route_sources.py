"""Bind real authored route changes to exact source, native replay and captures."""
from pathlib import Path
import collections,copy,csv,hashlib,json
ROOT=Path(__file__).resolve().parents[1];BASE=ROOT/'development/levels/rebuild';OUT=BASE/'access-route-pass';V=OUT/'verification'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def write(p,d):p.write_bytes((json.dumps(d,indent=2)+'\n').encode())
def main():
 roster=json.loads((OUT/'roster.json').read_text());old=json.loads((BASE/'flat-terrain-pass/roster.json').read_text());before={s['id']:s for s in old['active_sources']};movement=json.loads((V/'current-flat-movement.json').read_text());stairs=json.loads((V/'stair-movement.json').read_text());captures=json.loads((V/'captures.json').read_text());sheets=json.loads((V/'sheets.json').read_text());rows=[];changes=[]
 assert len(roster['sites'])==29 and len(roster['active_sources'])==25 and not movement['complete_link_regressions']
 assert set(movement['sites'])==set(before)
 passed={r['site']+':'+r['link'] for r in csv.DictReader((V/'current-flat-links.tsv').open(),delimiter='\t') if r['variant']=='after' and r['authored_forward']==r['authored_reverse']=='PASS'}
 for source in roster['active_sources']:
  site=source['id'];prior=before[site];meta=json.loads((ROOT/source['construction']).read_text());data=json.loads((ROOT/source['layout']).read_text());previous=json.loads((ROOT/prior['layout']).read_text());revisions=meta.get('access_route_changes',[]) if source!=prior else []
  assert sum(z.get('player_access')!='SEALED_CONTEXT' for z in data['zones'])<=40
  assert sha(ROOT/source['layout'])==meta['layout_sha256']
  for key in ['glb','native','blend']:assert sha(ROOT/source[key])==sha(ROOT/prior[key]),(site,key,'Changed physical source')
  assert sha(ROOT/source['glb'])==source['glb_sha256']==meta['glb_sha256']
  assert sha(ROOT/source['native'])==source['native_sha256']==meta['physics_sha256']
  assert meta['stair_components']==json.loads((ROOT/prior['construction']).read_text())['stair_components']
  restored=copy.deepcopy(data)
  for revision in revisions:
   a=next(l for l in previous['links'] if l['id']==revision['link']);b=next(l for l in restored['links'] if l['id']==revision['link']);assert a['points']==revision['before'] and b['points']==revision['after'];assert site+':'+revision['link'] in passed
   assert b['width']==a['width'] and b.get('threshold')==a.get('threshold');b['points']=revision['before'];changes.append(dict(site=site,link=revision['link']))
  assert restored==previous,(site,'Unexpected non-route change')
  binding=movement['source_bindings'][site]['after'];assert sha(ROOT/binding['contract'])==binding['contract_sha256'] and sha(ROOT/binding['layout'])==binding['layout_sha256'] and sha(ROOT/binding['native'])==binding['native_sha256']
  assert binding['layout']==source['layout'] and binding['native']==source['native']
  levels={p['after'][0][1] for p in revisions}
  for y in levels:
   frames=[r for r in captures['captures'] if r['site']==site and r['floor_y']==y];assert {(r['phase'],r['view']) for r in frames}=={(p,v) for p in ['before','after'] for v in ['floor-plan','route-detail']};assert {p['link'] for p in revisions if p['after'][0][1]==y}==set(frames[0]['shown_links'])
  rows.append(dict(id=site,changed_links=len(revisions),geometry_byte_identical=True,non_link_fields_identical=True,open_search_spaces=sum(z.get('player_access')!='SEALED_CONTEXT' for z in data['zones']),source=source))
 for row in captures['captures']:
  for path,key in [('path','sha256'),('source_glb','source_glb_sha256'),('layout','layout_sha256')]:assert sha(ROOT/row[path])==row[key]
 for row in sheets:assert sha(ROOT/row['path'])==row['sha256']
 for field in ['checker','prior_checker']:assert sha(ROOT/captures[field+'_source'])==captures[field+'_sha256']
 assert len(changes)==51 and sum(bool(r['changed_links']) for r in rows)==14
 assert all(r['authored_forward']==r['authored_reverse']=='PASS' for r in stairs['authored_routes'])
 assert not any(stairs['bearing_regressions'].values())
 flights=stairs['sites']['after_bearing'];assert sum(r['flights'] for r in flights.values())==221 and all(r['flights']==r['authored_up']==r['authored_down'] for r in flights.values())
 count=dict(complete_flat_before=sum(r['before']['pass_both_directions'] for r in movement['sites'].values()),complete_flat_after=sum(r['after']['pass_both_directions'] for r in movement['sites'].values()),complete_flat_total=sum(r['after']['flat_links'] for r in movement['sites'].values()),current_full_foot_samples=sum(r['current_lanes']['samples'] for r in movement['sites'].values()),current_invalid_full_foot=sum(r['current_lanes']['invalid_standing'] for r in movement['sites'].values()),flights=221,complete_vertical_links=stairs['complete_routes']['after_bearing'],changed_links=len(changes),changed_sites=14,capture_frames=len(captures['captures']),review_sheets=len(sheets))
 write(V/'source-preservation.json',dict(status='PASS_SCOPED_SOURCE_NATIVE_REPLAY_AND_CAPTURE_BINDINGS',sites=rows,counts=count,checker_source=Path(__file__).relative_to(ROOT).as_posix(),checker_sha256=sha(Path(__file__)),scope='Only declared link point arrays change. All actual mesh/native/editable bytes, remaining layout, original route widths, doorway positions, sealed context, functional sockets and staging persist. All51revisions pass independent continuous native replay. This does not prove connectivity between room sockets, spawns, runtime, visual join/light, service, networking or hardware acceptance. Current footprint sample coordinates changed intentionally; invalid totals must not be subtracted as equal-coordinate recovery.'))
 remaining=[];manifest={r['id']:r for r in roster['active_sources']}
 for row in csv.DictReader((V/'current-flat-links.tsv').open(),delimiter='\t'):
  if row['variant']!='after' or row['authored_forward']==row['authored_reverse']=='PASS':continue
  source=manifest[row['site']];data=json.loads((ROOT/source['layout']).read_text());link=next(l for l in data['links'] if l['id']==row['link']);remaining.append(dict(site=row['site'],link=row['link'],rooms=[link['a'],link['b']],points=link['points'],width=link['width'],threshold=link.get('threshold'),forward=row['authored_forward'],reverse=row['authored_reverse'],consume_source=source))
 assert len(remaining)==122
 write(V/'remaining-routes.json',dict(status='ACTUAL_CURRENT_UNFINISHED_FLAT_DESIGN_WORKLIST',complete_flat_links_remaining=len(remaining),links=remaining,room_connection_worklist='development/levels/rebuild/access-route-pass/verification/room-socket-connectivity.json',scope='Failed current flat links. Invalid flat approaches inside vertical routes and unresolved room socket joins are additional obligations; no route-ID-only global graph acceptance.'))
 print('ACCESS_SOURCE_CHECK',json.dumps(count))
if __name__=='__main__':main()
