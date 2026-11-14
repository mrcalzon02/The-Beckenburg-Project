"""Actual current flat lanes and continuous complete flat links.
Original valid-route regression remains a separate unchanged-coordinate check.
"""
from pathlib import Path
import argparse,collections,csv,hashlib,json,math,os,subprocess
from roster_candidate_sources import arguments as candidate_arguments,select as candidate_select
ROOT=Path(__file__).resolve().parents[1];BASE=ROOT/'development/levels/rebuild';OUT=BASE/'flat-circulation-pass/verification';BUILD=ROOT/'build/current-flat-probe'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def latest(site):
 return next((BASE/f/site.lower(),stem) for f,stem in [('stair-connection-pass','connected-stairs'),('stair-terrain-pass/assemblies','benched-terrain'),('stair-bay-pass','bayed-stairs'),('stair-opening-pass','opened-stairs'),('stair-landing-pass','supported-stairs'),('floor-closure-pass','closed-floors')] if (BASE/f/site.lower()/(stem+'.hcs2')).exists())
def main():
 global OUT,BUILD
 p=argparse.ArgumentParser();p.add_argument('--sites',nargs='+');p.add_argument('--arrival-pass',action='store_true');candidate_arguments(p,output=True);args=p.parse_args()
 if args.arrival_pass:OUT=BASE/'arrival-bearing-pass/verification';BUILD=ROOT/'build/arrival-flat-probe'
 if args.source_manifest:
  assert args.output;OUT=ROOT/args.output;BUILD=ROOT/'build/manifest-flat-probe'
 OUT.mkdir(parents=True,exist_ok=True)
 BUILD.mkdir(parents=True,exist_ok=True);sites=json.loads((BASE/'furnishings/room-programs.json').read_text())['sites'];paths=[];names=[];lane_names=[];lanes=[];bindings={}
 if args.sites:sites=[s for s in sites if s in args.sites]
 for site in sites:
  before,stem=(BASE/'flat-circulation-pass'/site.lower(),'open-circulation') if args.arrival_pass else latest(site);current=BASE/'flat-circulation-pass'/site.lower();variants=[('before',before,stem),('after',current,'open-circulation')] if (current/'construction.json').exists() else [('before',before,stem),('after',before,stem)];bindings[site]={}
  if args.arrival_pass:
   current=BASE/'arrival-bearing-pass'/site.lower();variants=[('before',before,stem),('after',current,'supported-arrival')] if (current/'construction.json').exists() else [('before',before,stem),('after',before,stem)]
  if args.source_manifest:
   old,new,selected,current=candidate_select(args,site);variants=[('before',old[0],Path(old[2]).stem),('after',new[0],Path(new[2]).stem)]
  for phase,folder,name in variants:
   c=folder/'construction.json';meta=json.loads(c.read_text());layout=ROOT/(json.loads((current/'construction.json').read_text())['before_layout'] if phase=='before' and (current/'construction.json').exists() else meta['layout']);data=json.loads(layout.read_text());native=folder/(name+'.hcs2');digest=sha(native);assert digest==meta['physics_sha256'];stairs=meta.get('stair_components',json.loads((BASE/'furnishings'/site.lower()/'physics-contract.json').read_text())['stair_components']);names.extend(site+':'+phase+'\t'+n for n in stairs);bindings[site][phase]=dict(contract=c.relative_to(ROOT).as_posix(),contract_sha256=sha(c),layout=layout.relative_to(ROOT).as_posix(),layout_sha256=sha(layout),native=native.relative_to(ROOT).as_posix(),native_sha256=digest)
   if phase=='after':
    for mode in ['before','after']:lane_names.extend(site+':'+mode+'\t'+n for n in stairs)
   for link in data['links']:
    if link.get('player_access')=='SEALED_NO_ROUTE':continue
    flat=all(abs(a[1]-b[1])<.01 for a,b in zip(link['points'],link['points'][1:]))
    for index,(a,b) in enumerate(zip(link['points'],link['points'][1:])):
     if flat:paths.append('\t'.join(map(str,[site,phase,native,digest,link['id'],index,*a,*b])))
     if phase!='after' or abs(a[1]-b[1])>.01:continue
     run=math.hypot(b[0]-a[0],b[2]-a[2]);assert run>.001,(site,link['id'])
     for lane,offset in [('centre',0),('left',max(0,link['width']/2-.27)),('right',-max(0,link['width']/2-.27))]:
      n=math.ceil(run/.25)
      for i in range(n+1):
       x=a[0]+(b[0]-a[0])*i/n-(b[2]-a[2])/run*offset;z=a[2]+(b[2]-a[2])*i/n+(b[0]-a[0])/run*offset;lanes.append('\t'.join(map(str,[site,native,digest,native,digest,link['id']+':'+str(index),x,a[1],z,lane])))
 for file,rows in [('current-flat-paths.tsv',paths),('current-flat-names.tsv',names),('current-flat-lane-names.tsv',lane_names),('current-flat-lane-input.tsv',lanes)]:
  (OUT/file).write_bytes(('\n'.join(rows)+'\n').encode())
 empty=BUILD/'empty-flights.tsv';empty.write_bytes(b'');java=Path('C:/Program Files/Java/jdk-17/bin');jar=ROOT/'build/beckenburg-game.jar';sources=[ROOT/'src/main/java/org/beckenburg/AuthoredSitePhysics.java',ROOT/'tools/java/RosterStairMovementProbe.java',ROOT/'tools/java/FurnishingRouteProbe.java',ROOT/'tools/java/RosterFailureContacts.java'];subprocess.run([str(java/'javac.exe'),'-cp',str(jar),'-d',str(BUILD),*[str(p) for p in sources]],check=True);cp=str(BUILD)+os.pathsep+str(jar)
 subprocess.run([str(java/'java.exe'),'-Djava.awt.headless=true','-Xmx2g','-cp',cp,'RosterStairMovementProbe',str(OUT/'current-flat-names.tsv'),str(empty),str(BUILD/'empty-results.tsv'),str(OUT/'current-flat-links.tsv'),str(OUT/'current-flat-paths.tsv')],check=True)
 subprocess.run([str(java/'java.exe'),'-Djava.awt.headless=true','-Xmx2g','-cp',cp,'FurnishingRouteProbe',str(OUT/'current-flat-lane-input.tsv'),str(OUT/'current-flat-lanes.tsv'),str(OUT/'current-flat-lane-names.tsv'),'--strict-footprints'],check=True)
 rows=list(csv.DictReader((OUT/'current-flat-links.tsv').open(),delimiter='\t'));lane_rows=list(csv.DictReader((OUT/'current-flat-lanes.tsv').open(),delimiter='\t'));diagnoses=[];bad=[r for r in lane_rows if r['after_valid']!='true']
 for r in bad:
  b=bindings[r['site']]['after'];y=r['after_ground_y'] if r['after_ground_y']!='-' else r['y'];diagnoses.append('\t'.join(map(str,[r['site'],'after',ROOT/b['native'],b['native_sha256'],r['link']+'/'+r['lane'],len(diagnoses),r['x'],y,r['z'],r['x'],y,r['z']])))
 source=OUT/'current-flat-failure-input.tsv';source.write_bytes(('\n'.join(diagnoses)+'\n').encode());contacts=OUT/'current-flat-failure-contacts.tsv'
 if diagnoses:subprocess.run([str(java/'java.exe'),'-Djava.awt.headless=true','-Xmx2g','-cp',cp,'RosterFailureContacts',str(OUT/'current-flat-lane-names.tsv'),str(source),str(contacts),'fixed-poses','after'],check=True)
 else:contacts.write_bytes(b'site\tlink\tsegment\tdirection\tx\ty\tz\treason\tcomponent\n')
 findings=list(csv.DictReader(contacts.open(),delimiter='\t'));summary={}
 for site in sites:
  result={}
  for phase in ['before','after']:
   selected=[r for r in rows if r['site']==site and r['variant']==phase];result[phase]=dict(flat_links=len(selected),pass_both_directions=sum(r['authored_forward']=='PASS' and r['authored_reverse']=='PASS' for r in selected))
  selected=[r for r in lane_rows if r['site']==site];failed=[r for r in findings if r['site']==site];result['current_lanes']=dict(samples=len(selected),valid_standing=sum(r['after_valid']=='true' for r in selected),invalid_standing=len(failed),reasons=dict(collections.Counter(r['reason'] for r in failed)),physical_components=collections.Counter(r['component'] for r in failed if r['reason']=='PHYSICAL_COMPONENT').most_common(25));summary[site]=result
 before={(r['site'],r['link']):r for r in rows if r['variant']=='before'};after={(r['site'],r['link']):r for r in rows if r['variant']=='after'};assert before.keys()<=after.keys(),'An existing active flat link was removed';added={key:after[key] for key in after.keys()-before.keys()};added_failures=[dict(site=key[0],link=key[1],forward=row['authored_forward'],reverse=row['authored_reverse']) for key,row in added.items() if row['authored_forward']!='PASS' or row['authored_reverse']!='PASS'];regressions=[dict(site=key[0],link=key[1],field=field,before=before[key][field],after=after[key][field]) for key in before for field in ['authored_forward','authored_reverse'] if before[key][field]=='PASS' and after[key][field]!='PASS']
 report=dict(status='CURRENT_AUTHORED_FLAT_MOVEMENT_MEASURED_NOT_MAP_ACCEPTANCE',sites=summary,source_bindings=bindings,complete_link_regressions=regressions,new_complete_links=[dict(site=key[0],link=key[1]) for key in sorted(added)],new_complete_link_failures=added_failures,source_hashes={p.relative_to(ROOT).as_posix():sha(p) for p in sources+[Path(__file__)]+([ROOT/'tools/roster_candidate_sources.py',ROOT/args.source_manifest] if args.source_manifest else [])},evidence_hashes={p.relative_to(ROOT).as_posix():sha(p) for p in OUT.glob('current-flat-*.tsv')},scope='Every active fully-flat link continuously forward/reverse with original endpoints and actual authored current points; all flat segments including vertical-link approaches sampled in centre/side lanes with strict production traversable full footprints. Original unchanged-coordinate body/sweep regression remains separate. Native candidate evidence, not runtime unlock, arbitrary movement, visual or multiplayer acceptance.')
 (OUT/'current-flat-movement.json').write_bytes((json.dumps(report,indent=2)+'\n').encode());print('CURRENT_FLAT',len(lane_rows),'samples',len(bad),'invalid',sum(r['after']['pass_both_directions'] for r in summary.values()),'complete links passing both ways',len(regressions),'regressions',flush=True)
 assert not regressions,'New authored flat routes regress previously passing complete link directions'
 assert not added_failures,'A new physical connection fails continuous native movement'
if __name__=='__main__':main()
