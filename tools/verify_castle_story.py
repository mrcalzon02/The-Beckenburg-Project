"""Run the existing Castle contracts and new museum interaction against the packaged JAR."""
from pathlib import Path
import argparse,json,os,shutil,subprocess,time
p=argparse.ArgumentParser();p.add_argument('--java',default=shutil.which('java'));args=p.parse_args()
if not args.java:raise SystemExit('Java17+ is required')
out=Path('build/castle-story-verification');out.mkdir(parents=True,exist_ok=True)
tests=list(json.loads(Path('development/levels/rebuild/verification/castle-native-upgrade-2026-11-06/results.json').read_text(encoding='utf-8')))+['CastleStoryTest'];results={}
for name in tests:
 start=time.monotonic()
 with (out/(name+'.log')).open('w',encoding='utf-8') as log:
  result=subprocess.run([args.java,'-Djava.awt.headless=true','-ea','-cp',str(Path('build/beckenburg-game.jar'))+os.pathsep+'build/test','org.beckenburg.'+name],stdout=log,stderr=subprocess.STDOUT,timeout=180)
 results[name]=dict(exit=result.returncode,seconds=round(time.monotonic()-start,3));(out/'results.json').write_bytes((json.dumps(results,indent=2)+'\n').encode('utf-8'));print(name,result.returncode,flush=True)
 if result.returncode:raise SystemExit((out/(name+'.log')).read_text(encoding='utf-8')[-2400:])
subprocess.run([args.java,'-cp','build/beckenburg-game.jar','org.beckenburg.BeckenburgRegressionSuite','--validate'],check=True)
print('PASS',len(tests),'actual packaged Castle/editor/rite/story bodies; manifest preflight is distinct')
