"""Match actual terrain collision geometry to persistent worlds before sculpting.
No terrain is changed by this inventory. Never guesses a replacement world.
"""
from pathlib import Path
import collections,csv,hashlib,json,struct
ROOT=Path(__file__).resolve().parents[1];BASE=ROOT/'development/levels/rebuild';OUT=BASE/'flat-terrain-pass'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def parts(path):
 raw=path.read_bytes();magic,count,total=struct.unpack_from('>III',raw);assert magic==0x48435332;i=12;out=[]
 for _ in range(count):
  start=i;n=struct.unpack_from('>H',raw,i)[0];i+=2;name=raw[i:i+n].decode();i+=n;closed,size=struct.unpack_from('>BI',raw,i);i+=5;ts=[struct.unpack_from('>9fB',raw,i+37*j) for j in range(size)];i+=37*size;out.append(dict(name=name,closed=closed,count=size,raw=raw[start:i],triangles=ts))
 assert i==len(raw) and sum(p['count'] for p in out)==total;return out
def faces(ps):return collections.Counter((tuple(sorted(tuple(t[a:a+3]) for a in [0,3,6])),t[9]) for p in ps for t in p['triangles'])
def write(p,d):p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes((json.dumps(d,indent=2)+'\n').encode())
def main():
 contacts=list(csv.DictReader((BASE/'arrival-bearing-pass/verification/current-flat-failure-contacts.tsv').open(),delimiter='\t'));sources=json.loads((BASE/'arrival-bearing-pass/roster.json').read_text())['active_sources'];records=[];worlds=list(BASE.rglob('terrain-physics.hcs2'))
 for source in sources:
  site=source['id'];hits=[r for r in contacts if r['site']==site and r['reason']=='PHYSICAL_COMPONENT' and 'terrain' in r['component'].lower()]
  if not hits:records.append(dict(id=site,decision='RETAIN_SOURCE_NO_DIAGNOSED_TERRAIN_BODY_CONTACT',source=source));continue
  native=ROOT/source['native'];assert sha(native)==source['native_sha256'];soil=[p for p in parts(native) if 'terrain' in p['name'].lower()];target=faces(soil);matches=[]
  for world in worlds:
   if world.parent.parent.name!=site.lower():continue
   if faces(parts(world))==target:matches.append(world.parent)
  def priority(p):
   s=p.as_posix();return next((n for n,k in enumerate(['stair-connection-pass','stair-terrain-pass','terrain-occupancy-pass','terrain-overall-pass']) if k in s),10)
  matches.sort(key=lambda p:(priority(p),p.as_posix()));record=dict(id=site,source=source,terrain_parts=[p['name'] for p in soil],terrain_triangles=sum(p['count'] for p in soil),body_contacts=len(hits),links=dict(collections.Counter(r['link'].split(':')[0] for r in hits)),actual_failed_contacts=hits)
  if not matches:record.update(decision='DEFER_NO_EXACT_PERSISTENT_TERRAIN_MATCH',reason='Current terrain may be an original authored mesh; do not replace it with a merely similar world.')
  else:
   world=matches[0];sample_root=world.parents[2]/'samples'/site.lower();settings=sample_root/'settings.tsv'
   if not settings.exists():
    options=[p for p in BASE.rglob('settings.tsv') if p.parent.name==site.lower()]+[ROOT/'build/terrain-samples'/site.lower()/'settings.tsv'];settings=next((p for p in options if p.exists()),settings)
   record.update(decision='AUTHOR_SAVED_LOWER_ONLY_TERRAIN_REPAIR' if settings.exists() else 'DEFER_SETTINGS_SOURCE_MISSING',source_world=world.relative_to(ROOT).as_posix(),equivalent_worlds=[p.relative_to(ROOT).as_posix() for p in matches],settings=settings.relative_to(ROOT).as_posix(),prior_grades=(sample_root/'grades.tsv').relative_to(ROOT).as_posix() if (sample_root/'grades.tsv').exists() else None,source_height_sha256=sha(world/'tile_0_0.png'),source_native_sha256=sha(world/'terrain-physics.hcs2'))
  records.append(record);print('TERRAIN_SOURCE_MATCH',site,record['decision'],len(hits),record.get('source_world'),flush=True)
 for site in ['CASTLE','HOUSE','WLD-01','WLD-02']:records.append(dict(id=site,decision='RETAIN_MATURE_SOURCE_INDIVIDUAL_TERRAIN_ACCEPTANCE_OPEN'))
 write(OUT/'terrain-repair-plan.json',dict(status='EXACT_SOURCE_MATCH_INVENTORY_NOT_TERRAIN_ACCEPTANCE',sites=records,planner_sha256=sha(Path(__file__))))
if __name__=='__main__':main()
