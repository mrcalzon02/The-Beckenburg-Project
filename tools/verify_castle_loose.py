"""Verify Castle loose bodies plus existing hub, lighting, clutter and network contracts."""
from pathlib import Path
import argparse,json,os,shutil,subprocess,time
p=argparse.ArgumentParser();p.add_argument('--java',default=shutil.which('java'));p.add_argument('--resume',action='store_true');args=p.parse_args()
if not args.java:raise SystemExit('Java17+ is required')
tests=list(json.loads(Path('development/levels/rebuild/verification/castle-story-2026-11-06/results.json').read_text(encoding='utf-8')))
for name in ['CastleLooseClutterTest','LooseClutterTest','LooseClutterNetworkTest','DynamicGeometryRetentionTest','PhysicalLightingTest','HandheldSurfaceOcclusionTest']:
 if name not in tests:tests.append(name)
out=Path('build/castle-loose-verification');out.mkdir(parents=True,exist_ok=True);results=json.loads((out/'results.json').read_text(encoding='utf-8')) if args.resume and (out/'results.json').exists() else {}
for name in tests:
 if name in results and results[name]['exit']==0:continue
 start=time.monotonic()
 with (out/(name+'.log')).open('w',encoding='utf-8') as log:
  result=subprocess.run([args.java,'-Djava.awt.headless=true','-ea','-cp','build/beckenburg-game.jar'+os.pathsep+'build/test','org.beckenburg.'+name],stdout=log,stderr=subprocess.STDOUT,timeout=180)
 results[name]=dict(exit=result.returncode,seconds=round(time.monotonic()-start,3));(out/'results.json').write_bytes((json.dumps(results,indent=2)+'\n').encode('utf-8'));print(name,result.returncode,flush=True)
 if result.returncode:raise SystemExit((out/(name+'.log')).read_text(encoding='utf-8')[-2400:])
subprocess.run([args.java,'-cp','build/beckenburg-game.jar','org.beckenburg.BeckenburgRegressionSuite','--validate'],check=True)
print('PASS',len(tests),'actual packaged Castle/body/network/lighting bodies; manifest preflight is distinct')
