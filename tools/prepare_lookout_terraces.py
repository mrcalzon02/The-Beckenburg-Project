"""Use saved editor geographic fields; architectural terrain program has no paths."""
from pathlib import Path
import json,shutil,subprocess,hashlib
R=Path(r'C:/Users/Admin/.codex/worktrees/castle-textiles/The-Beckenburg-Project')
B=R/'development/levels/rebuild';O=B/'service-ground-bay-pass';P=O/'program/wld-04';P.mkdir(parents=True,exist_ok=True)
source=B/'stair-terrain-pass/worlds/wld-04/terrain-world';world=O/'worlds/wld-04/terrain-world'
shutil.copytree(source,world,dirs_exist_ok=True)
# Geographic terraces and their connecting landform. These polygons precede
# and are independent of the fixed architectural stair bays.
fields=[
 ('upper west hillside','DIRT',2,'plane:22.525:0:-0.552083333333',[(21,30.7),(34,30.7),(35,36.2),(20,36.2)]),
 ('lower hillside','DIRT',2,'plane:36.15:0:-0.625',[(7,53.2),(51,53.2),(54,58),(6,58)]),
 ('upper lookout clearing','DIRT',4,'plane:5.65:0:0',[(2,0),(38,-1),(56,8),(57,29),(49,32),(36,30.7),(24,30.7),(20,28),(2,28)]),
 ('middle forest bench','DIRT',3,'plane:3:0:0',[(10,36),(30,36),(37,41),(51,42),(52,50),(48,53.2),(13,53.2),(9,44)]),
 ('roadhead and lower clearing','GRAVEL',4,'plane:0:0:0',[(7,58),(15,56),(47,56),(53,62),(43,77),(38,85),(18,85),(12,73),(6,66)])]
(P/'features.tsv').write_text(''.join('\t'.join([n,m,str(f),p]+[str(q) for xy in poly for q in xy])+'\n' for n,m,f,p,poly in fields),encoding='utf-8')
roster=json.loads((B/'household-sacred-bay-pass/roster.json').read_text());row=next(s for s in roster['active_sources'] if s['id']=='WLD-04');data=json.loads((R/row['layout']).read_text())
protected=[z['bounds'] for z in data['zones'] if z['kind']=='inside']+[[24,2,36,16]]
(P/'protected-rooms.tsv').write_text(''.join('\t'.join(map(str,p))+'\n' for p in protected),encoding='utf-8')
(P/'underlays.tsv').write_text('20\t72\t36\t72\t36\t84\t20\t84\n',encoding='utf-8')
src=(R/'tools/java/RebuildInstitutionEarthworks.java').read_text();src=src.replace('RebuildInstitutionEarthworks','RebuildTieredEarthworks').replace('Geographic site earthworks ','Architectural site terraces ')
needle='if(f.profile.equals("woodland"))'
src=src.replace(needle,'if(f.profile.startsWith("plane:")){String[] coefficients=f.profile.split(":");target=Double.parseDouble(coefficients[1])+Double.parseDouble(coefficients[2])*wx+Double.parseDouble(coefficients[3])*wz;}\n    '+needle)
java=R/'tools/java/RebuildTieredEarthworks.java';java.write_text(src,encoding='utf-8')
out=R/'build/tiered-earthworks';out.mkdir(exist_ok=True)
subprocess.run([r'C:/Program Files/Java/jdk-17/bin/javac.exe','-cp',str(R/'build/beckenburg-game.jar'),'-d',str(out),str(java)],check=True)
log=O/'verification';log.mkdir(exist_ok=True)
with (log/'lookout-earthworks.log').open('w',encoding='utf-8') as f:subprocess.run([r'C:/Program Files/Java/jdk-17/bin/java.exe','-Xmx3g','-cp',str(out)+';'+str(R/'build/beckenburg-game.jar'),'RebuildTieredEarthworks',str(O),'WLD-04',str(source)],stdout=f,stderr=subprocess.STDOUT,check=True)
(world/'verification.json').write_bytes((world/'earthworks-verification.json').read_bytes())
(world/'source-provenance.json').write_text(json.dumps(dict(source_world=source.relative_to(R).as_posix(),method='Actual saved editor architectural geographic fields; no route/centerline input',source_height_topology_and_holes_unchanged=True,program='development/levels/rebuild/service-ground-bay-pass/program/wld-04/design.json'),indent=2)+'\n',encoding='utf-8')
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
for p in source.rglob('*'):
 if p.is_file() and p.name in ['tile_0_0.png','adaptive-topology.bin','authored-cuts.tsv']:assert sha(p)==sha(world/p.relative_to(source))
(P/'design.json').write_text(json.dumps(dict(source=row,source_world=source.relative_to(R).as_posix(),world=world.relative_to(R).as_posix(),fields=fields,protected_footprints=protected,source_files_sha256={p.relative_to(source).as_posix():sha(p) for p in source.rglob('*') if p.is_file() and not p.name.endswith('.lock')},author='tools/prepare_lookout_terraces.py',architectural_reason='Three connected geographic levels and two hillside stairs; buildings and saved room punches retained. No path data used by terrain authoring.'),indent=2)+'\n',encoding='utf-8')
print('LOOKOUT_SAVED_GEOGRAPHIC_TERRACES',json.loads((world/'earthworks-verification.json').read_text()))
