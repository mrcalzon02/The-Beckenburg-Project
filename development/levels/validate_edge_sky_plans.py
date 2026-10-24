"""Validate planning scope, preserved reservations and source hashes; no runtime acceptance."""
from pathlib import Path
import json,math,hashlib,sys
import xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[2];LEVELS=ROOT/'development/levels'
sys.path.insert(0,str(LEVELS))
from build_edge_sky_plans import point_in
def read(p):return json.loads(p.read_text(encoding='utf-8'))
def digest(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def dist(p,a,b):
 dx=b[0]-a[0];dz=b[1]-a[1];t=max(0,min(1,((p[0]-a[0])*dx+(p[1]-a[1])*dz)/(dx*dx+dz*dz)))
 return math.hypot(p[0]-a[0]-dx*t,p[1]-a[1]-dz*t)
def main():
 cat=read(LEVELS/'edge-sky-plans/catalog.json');assert cat['base_maps']==28 and cat['additive_expansions']==1;assert len(cat['maps'])==29
 results=[]
 for row in cat['maps']:
  folder=LEVELS/'edge-sky-plans'/row['folder'];p=read(folder/'plan.json');source=ROOT/p['source_layout'];d=read(source)
  assert digest(source)==p['source_sha256'];assert d['title']==p['title'];poly=p['edge_envelope'];assert len(poly)==len(p['edges']) and len(poly)>=12
  refinement=read(ROOT/p['refined_terrain_source']) if p['refined_terrain_source'] else None
  zs=refinement['zones'] if refinement else d['zones'];links=refinement['routes'] if refinement else d['links'];st=refinement['staging'] if refinement else d['staging'];rites=refinement['rituals'] if refinement else d['rituals']
  assert p['protected_links']==links and p['protected_staging']==st and p['protected_rituals']==rites
  assert p['protected_zones']==[dict(id=z['id'],bounds=z['bounds'],y=z['y'],kind=z['kind']) for z in zs]
  samples=0
  def contained(q):
   nonlocal samples
   assert point_in(q,poly),(row['folder'],q);samples+=1
  for z in zs:
   a,b,c,e=z['bounds']
   for q in [[a,b],[c,b],[c,e],[a,e]]:contained(q)
  for l in links:
   for a,b in zip(l['points'],l['points'][1:]):
    dx=b[0]-a[0];dz=b[2]-a[2];length=math.hypot(dx,dz);steps=max(1,math.ceil(length));nx=-dz/(length or 1);nz=dx/(length or 1)
    for i in range(steps+1):
     t=i/steps
     for offset in [-l['width']/2,0,l['width']/2]:contained([a[0]+t*dx+nx*offset,a[2]+t*dz+nz*offset])
  for rite in rites:
   x,y,z=rite['point']
   for dx,dz in [(-2.2,-2.2),(2.2,-2.2),(2.2,2.2),(-2.2,2.2)]:contained([x+dx,z+dz])
  for q in st['cargo_route']:contained([q[0],q[2]])
  gate=p['arrival_portal'];q=[gate['center'][0],gate['center'][2]];assert min(dist(q,a,b) for a,b in zip(poly,poly[1:]+poly[:1]))<.0001
  assert gate['clear_width_m']>=8 and gate['center'][2]>st['point'][2]+4
  for i,e in enumerate(p['edges']):
   assert e['points']==[poly[i],poly[(i+1)%len(poly)]];assert e['physical_recipe']['collision'] and e['base_y'].startswith('Sample/refit')
  assert len(p['sky_frames'])==8
  for i,f in enumerate(p['sky_frames']):
   assert f['bearing_range_deg']==[i*45,(i+1)*45];assert f['mid_distance_m'][0]>0 and f['mid_distance_m'][1]>f['mid_distance_m'][0]
   assert -5<=f['horizon_crest_elevation_deg'][0]<f['horizon_crest_elevation_deg'][1]<60
  assert p['review_views'] and all(all(math.isfinite(n) for n in v['position']) for v in p['review_views'])
  assert any(v['id']=='van-arrival' for v in p['review_views']) and any(v['id']=='highest-accessible-layer' for v in p['review_views'])
  ET.parse(folder/'plan.svg');png=LEVELS/'previews/edge-sky-plans'/f'{row["folder"]}-edge-sky-plan.png';assert png.read_bytes()[:8]==b'\x89PNG\r\n\x1a\n'
  results.append(dict(id=p['id'],source_hash_match=True,protected_reservation_samples=samples,connected_edge_spans=len(poly),sky_sectors=8,arrival_and_cargo_preserved=True,views=len(p['review_views']),svg_xml_and_png_pass=True,status='planning contract checks pass; no runtime containment/framing claim'))
 old=read(LEVELS/'verification/new-location-planning.json')
 for rel,h in old['protected_originals'].items():assert digest(ROOT/rel)==h,rel
 out=dict(status='planning checks pass',base_maps=28,expansions=1,maps=results,original_sources_preserved=len(old['protected_originals']),engine_changed=False,limitations=['No new barrier geometry, collision, navigation or renderer changes.','No exact scene horizon coverage or screenshot framing accepted.','All proposed spans need authoritative ground fitting and human/ghost/crouch/upper-view tests.'])
 (LEVELS/'verification/edge-sky-plans.json').write_text(json.dumps(out,indent=2)+'\n',encoding='utf-8')
 print('PASS',len(results),'edge/sky planning packets;',sum(r['protected_reservation_samples'] for r in results),'reservation samples; original sources unchanged.')
if __name__=='__main__':main()
