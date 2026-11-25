"""Actual editor lower-only cut layers for eight diagnosed circulation conflicts.
Preserve exact existing source heights, persistent topology and prior grade rows.
"""
from pathlib import Path
import argparse,json,os,shutil,subprocess,sys
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
from plan_flat_terrain_repairs import sha,write
BASE=ROOT/'development/levels/rebuild';OUT=BASE/'flat-terrain-pass'
def main():
 p=argparse.ArgumentParser();p.add_argument('--sites',nargs='+');args=p.parse_args();sys.path.insert(0,str(ROOT/'build/structure-review-python'));from PIL import Image
 java=Path('C:/Program Files/Java/jdk-17/bin');build=ROOT/'build/flat-terrain-builder';build.mkdir(parents=True,exist_ok=True);jar=ROOT/'build/beckenburg-game.jar';subprocess.run([str(java/'javac.exe'),'-cp',str(jar),'-d',str(build),str(ROOT/'tools/java/RosterTerrainBuild.java')],check=True)
 plan=json.loads((OUT/'terrain-repair-plan.json').read_text())
 for row in plan['sites']:
  site=row['id']
  if row['decision']!='AUTHOR_SAVED_LOWER_ONLY_TERRAIN_REPAIR' or args.sites and site not in args.sites:continue
  source=ROOT/row['source_world'];world=OUT/'worlds'/site.lower()/'terrain-world';samples=OUT/'samples'/site.lower();samples.mkdir(parents=True,exist_ok=True);saved={p.relative_to(ROOT).as_posix():sha(p) for p in source.rglob('*') if p.is_file() and not p.name.endswith('.lock')};assert sha(source/'terrain-physics.hcs2')==row['source_native_sha256'];assert sha(source/'tile_0_0.png')==row['source_height_sha256']
  if not world.exists():shutil.copytree(source,world,ignore=shutil.ignore_patterns('*.lock'))
  image=Image.open(source/'tile_0_0.png');assert image.size==(65,65);(samples/'heights.txt').write_bytes(('\n'.join(format(int(v)/65535*64-32,'.12f') for v in image.get_flattened_data())+'\n').encode());(samples/'settings.tsv').write_bytes((ROOT/row['settings']).read_bytes());(samples/'cuts.tsv').write_bytes((source/'authored-cuts.tsv').read_bytes());prior=(ROOT/row['prior_grades']).read_bytes() if row.get('prior_grades') else b''
  assert not (source/'authored-path-grading.sha256').exists() or prior,'Existing grade program must be retained exactly';data=json.loads((ROOT/row['source']['layout']).read_text());program=[];grades=[]
  for link in data['links']:
   if link['id'] not in row['links']:continue
   for i,(a,b) in enumerate(zip(link['points'],link['points'][1:])):
    if abs(a[1]-b[1])>.01:continue
    # The coarse existing two-metre sculpt grid needs a physical shoulder
    # around the original route, not a room-shaped ground pad/material stamp.
    half=link['width']/2+1.5;shoulder=2.5;points=[[p[0],p[2],p[1]-.2] for p in [a,b]];name='Circulation cut '+site+' '+link['id']+' segment'+str(i);grades.append('\t'.join(map(str,[name,half,shoulder,*[v for p in points for v in p]])));program.append(dict(link=link['id'],segment=i,points=[a,b],bed_offset_metres=-.2,half_width_metres=half,shoulder_metres=shoulder,reason='Actual native terrain body contacts on this planned circulation link; no new floor support inferred'))
  (samples/'grades.tsv').write_bytes(prior+('\n'.join(grades)+'\n').encode());provenance=dict(id=site,source_world=row['source_world'],source_height_sha256=row['source_height_sha256'],source_native_sha256=row['source_native_sha256'],source=row['source'],program=program,grading_limit='LOWER_ONLY',prior_grades_retained_exactly=True,new_basement_punches=[],reason='Remove measured soil/body intersections while preserving original landform away from circulation and every saved basement punch');write(samples/'provenance.json',provenance)
  subprocess.run([str(java/'java.exe'),'-Djava.awt.headless=true','-cp',str(build)+os.pathsep+str(jar),'org.beckenburg.RosterTerrainBuild','--cut-only-grades','--input-root',str(OUT/'samples'),'--output-root',str(OUT/'worlds'),'--preview-root',str(BASE/'captures/flat-terrain-batch'),site.lower()],check=True)
  assert all(sha(ROOT/p)==h for p,h in saved.items());assert sha(world/'tile_0_0.png')==row['source_height_sha256'];assert sha(world/'adaptive-topology.bin')==sha(source/'adaptive-topology.bin'),'Existing punched topology bytes changed'
  write(OUT/(site.lower()+'-world.json'),dict(**provenance,source_world_files_preserved=saved,world=world.relative_to(ROOT).as_posix(),world_files_sha256={p.relative_to(ROOT).as_posix():sha(p) for p in world.rglob('*') if p.is_file() and not p.name.endswith('.lock')},author_sha256=sha(Path(__file__)),builder_sha256=sha(ROOT/'tools/java/RosterTerrainBuild.java'),dependency_jar_sha256=sha(jar),status='SAVED_EDITOR_CIRCULATION_CUT_CANDIDATE_NOT_TERRAIN_ACCEPTANCE'))
  print('SAVED_FLAT_TERRAIN',site,len(program),'grade segments; exact source PNG/topology preserved',flush=True)
if __name__=='__main__':main()
