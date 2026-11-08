"""Read-only diagnosis of every still-invalid flat-route sample after connections.
Does not alter routes, thresholds, collision or classify a map accepted.
"""
from pathlib import Path
import collections,csv,hashlib,json,subprocess
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'development/levels/rebuild/stair-connection-pass/verification';BUILD=ROOT/'build/roster-stair-probe'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
 files={}
 for line in (ROOT/'build/macroterrain-fit/input.tsv').read_text().splitlines():
  p=line.split('\t');files[p[0]]=(p[3],p[4])
 names=ROOT/'build/macroterrain-fit/stairs.tsv';names_out=OUT/'remaining-flat-stair-names.tsv';names_out.write_bytes(names.read_bytes());rows=[]
 for r in csv.DictReader((OUT/'native-route-probes.tsv').open(),delimiter='\t'):
  if r['after_valid']=='true':continue
  file,digest=files[r['site']];assert sha(Path(file))==digest
  y=r['after_ground_y'] if r['after_ground_y']!='-' else r['y'];rows.append('\t'.join(map(str,[r['site'],'after',file,digest,r['link']+'/'+r['lane'],len(rows),r['x'],y,r['z'],r['x'],y,r['z']])))
 source=OUT/'remaining-flat-input.tsv';source.write_bytes(('\n'.join(rows)+'\n').encode());target=OUT/'remaining-flat-contacts.tsv'
 subprocess.run(['C:/Program Files/Java/jdk-17/bin/java.exe','-Djava.awt.headless=true','-Xmx2g','-cp',str(BUILD)+';'+str(ROOT/'build/beckenburg-game.jar'),'RosterFailureContacts',str(names_out),str(source),str(target),'fixed-poses','after'],check=True)
 rs=list(csv.DictReader(target.open(),delimiter='\t'));assert len(rs)==len(rows);sites={}
 for site in sorted(files):
  selected=[r for r in rs if r['site']==site];sites[site]=dict(invalid_flat_samples=len(selected),reasons=dict(collections.Counter(r['reason'] for r in selected)),components=collections.Counter(r['component'] for r in selected).most_common(20),segments=collections.Counter(r['link'] for r in selected).most_common(20))
 report=dict(status='ACTUAL_REMAINING_FLAT_ROUTE_DIAGNOSIS_NOT_ACCEPTANCE',invalid_samples=len(rs),sites=sites,source_bindings={site:dict(path=file,sha256=digest) for site,(file,digest) in files.items()},inputs_sha256=sha(source),names_sha256=sha(names_out),contacts_sha256=sha(target),diagnostic_sha256=sha(ROOT/'tools/java/RosterFailureContacts.java'),kernel_sha256=sha(ROOT/'src/main/java/org/beckenburg/AuthoredSitePhysics.java'),driver_sha256=sha(Path(__file__)),scope='Every after_valid=false flat sample from final110409comparison; exact native actual-support-height pose diagnoses, no route or tolerance changes. Clear stairs do not prove remaining flat lanes or arbitrary movement.')
 (OUT/'remaining-flat-routes.json').write_bytes((json.dumps(report,indent=2)+'\n').encode());print('REMAINING_FLAT_SAMPLES',len(rs),flush=True)
if __name__=='__main__':main()
