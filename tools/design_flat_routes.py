"""Reroute diagnosed flat links through existing full-width native circulation.
Retains exact source geometry, endpoints, required thresholds, sealed context and
all previously passing routes. Does not unlock maps or manufacture floor support.
"""
from pathlib import Path
import argparse,csv,hashlib,json,os,subprocess
ROOT=Path(__file__).resolve().parents[1];BASE=ROOT/'development/levels/rebuild';OUT=BASE/'route-design-pass';BUILD=ROOT/'build/route-design'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def write(p,data):p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes((json.dumps(data,indent=2)+'\n').encode())
def serial(ps):return ';'.join(','.join(map(str,p)) for p in ps)
def main():
 global OUT,BUILD
 p=argparse.ArgumentParser();p.add_argument('--sites',nargs='+');p.add_argument('--source-manifest',type=Path);p.add_argument('--worklist',type=Path);p.add_argument('--output',type=Path);args=p.parse_args()
 if args.output:OUT=ROOT/args.output;BUILD=ROOT/'build'/('route-design-'+OUT.name)
 BUILD.mkdir(parents=True,exist_ok=True);v=OUT/'verification';v.mkdir(parents=True,exist_ok=True)
 work=json.loads((ROOT/args.worklist if args.worklist else BASE/'flat-circulation-pass/verification/remaining-routes.json').read_text(encoding='utf-8'));manifest={r['id']:r for r in json.loads((ROOT/args.source_manifest).read_text(encoding='utf-8'))['active_sources']} if args.source_manifest else {};inputs=[];names=[];source={}
 for item in work['links']:
  site=item['site']
  if args.sites and site not in args.sites:continue
  c=ROOT/(manifest[site]['construction'] if manifest else item['consume_source']);meta=json.loads(c.read_text());layout=ROOT/meta['layout'];data=json.loads(layout.read_text());link=next(l for l in data['links'] if l['id']==item['link']);native=ROOT/manifest[site]['native'] if manifest else c.parent/'open-circulation.hcs2';glb=ROOT/manifest[site]['glb'] if manifest else c.parent/'open-circulation.glb';assert sha(native)==meta['physics_sha256'];assert sha(layout)==meta['layout_sha256'];assert sha(glb)==meta['glb_sha256']
  source[site]=dict(contract=c.relative_to(ROOT).as_posix(),contract_sha256=sha(c),layout=meta['layout'],layout_sha256=sha(layout),native=native.relative_to(ROOT).as_posix(),native_sha256=sha(native),glb=glb.relative_to(ROOT).as_posix(),glb_sha256=sha(glb))
  names.extend(site+'\t'+n for n in meta['stair_components'])
  anchors=[link['points'][0]]
  if 'threshold' in link and link['threshold'] not in [link['points'][0],link['points'][-1]]:anchors.append(link['threshold'])
  anchors.append(link['points'][-1]);assert all(abs(a[1]-anchors[0][1])<.01 for a in anchors)
  sealed=[[*z['bounds'],z['y'],z['y']+z['height']] for z in data['zones'] if z.get('player_access')=='SEALED_CONTEXT']
  a,b,cx,d=data['site_bounds'];box=[max(a,min(p[0] for p in link['points'])-6),max(b,min(p[2] for p in link['points'])-6),min(cx,max(p[0] for p in link['points'])+6),min(d,max(p[2] for p in link['points'])+6)]
  inputs.append('\t'.join(map(str,[site,native,sha(native),link['id'],link['width'],serial(anchors),';'.join(','.join(map(str,r)) for r in sealed),','.join(map(str,box))])))
 (v/'design-input.tsv').write_bytes(('\n'.join(inputs)+'\n').encode());(v/'stair-names.tsv').write_bytes(('\n'.join(sorted(set(names)))+'\n').encode());write(v/'source-bindings.json',source)
 java=Path('C:/Program Files/Java/jdk-17/bin');jar=ROOT/'build/beckenburg-game.jar';subprocess.run([str(java/'javac.exe'),'-cp',str(jar),'-d',str(BUILD),str(ROOT/'src/main/java/org/beckenburg/AuthoredSitePhysics.java'),str(ROOT/'tools/java/FlatRouteDesigner.java')],check=True)
 with (BUILD/'design.log').open('wb') as log:subprocess.run([str(java/'java.exe'),'-Djava.awt.headless=true','-Xmx2g','-cp',str(BUILD)+os.pathsep+str(jar),'FlatRouteDesigner',str(v/'design-input.tsv'),str(v/'stair-names.tsv'),str(v/'design-results.tsv')],stdout=log,stderr=subprocess.STDOUT,check=True)
 rows=list(csv.DictReader((v/'design-results.tsv').open(),delimiter='\t'));changes=[]
 for site,s in source.items():
  data=json.loads((ROOT/s['layout']).read_text());changed=[];decisions=[r for r in rows if r['site']==site]
  for r in decisions:
   if r['status']!='REROUTED_FULL_WIDTH':continue
   link=next(l for l in data['links'] if l['id']==r['link']);before=link['points'];points=[[float(v) for v in p.split(',')] for p in r['points'].split(';')];assert points[0]==before[0] and points[-1]==before[-1]
   assert 'threshold' not in link or link['threshold'] in points or link['threshold'] in [points[0],points[-1]]
   link['points']=points;changed.append(dict(link=link['id'],before=before,after=points,width=link['width'],required_threshold=link.get('threshold')))
  if not changed:continue
  folder=OUT/site.lower();data['native_circulation_design']=dict(source=s,changes=changed,scope='Rerouted on existing exact native surfaces; unchanged endpoints, required thresholds and route widths. Source-only candidate; native validation, source visualization and runtime integration remain separate.')
  write(folder/'authored-layout.json',data);write(folder/'construction.json',dict(id=site,status='EXISTING_NATIVE_FULL_WIDTH_ROUTE_DESIGN_CANDIDATE',source=s,layout=(folder/'authored-layout.json').relative_to(ROOT).as_posix(),layout_sha256=sha(folder/'authored-layout.json'),changes=changed,decisions=decisions))
  changes.extend(dict(site=site,**c) for c in changed)
 write(v/'design-summary.json',dict(tested_links=len(rows),changed_links=len(changes),changed_sites=len(set(c['site'] for c in changes)),changes=changes,decisions=rows,source_hashes={p.relative_to(ROOT).as_posix():sha(p) for p in [Path(__file__),ROOT/'tools/java/FlatRouteDesigner.java',ROOT/'src/main/java/org/beckenburg/AuthoredSitePhysics.java']},scope='Geometry and production physics unchanged. Required source movement and complete route widths, not runtime map acceptance.'))
 print('DESIGN_RESULT',len(rows),'links examined;',len(changes),'rerouted;',len(set(c['site'] for c in changes)),'sites',flush=True)
if __name__=='__main__':main()
