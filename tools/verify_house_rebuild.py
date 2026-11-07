"""Run actual packaged construction and shared runtime regressions; no map acceptance shortcut."""
from pathlib import Path
import argparse,json,os,shutil,subprocess,time
p=argparse.ArgumentParser();p.add_argument('--java',default=shutil.which('java'));args=p.parse_args()
if not args.java:raise SystemExit('Java17+ is required')
out=Path('build/house-envelope-verification');out.mkdir(parents=True,exist_ok=True);results={}
tests=['HouseRebuildPhysicsTest','AuthoredLevelPhysicsTest','AuthoredLevelReachabilityTest','AuthoredUtilitiesTest','LevelSurfaceIntegrityTest','DynamicGeometryRetentionTest','PhysicalLightingTest','HandheldSurfaceOcclusionTest','PreparedLightBatchParityTest','CastleLooseClutterTest','CastlePortalSlotTest','CastleRebuildUpgradeTest']
for name in tests:
 start=time.monotonic()
 with (out/(name+'.log')).open('w',encoding='utf-8') as log:r=subprocess.run([args.java,'-Djava.awt.headless=true','-ea','-cp','build/beckenburg-game.jar'+os.pathsep+'build/test','org.beckenburg.'+name],stdout=log,stderr=subprocess.STDOUT,timeout=240)
 results[name]=dict(exit=r.returncode,seconds=round(time.monotonic()-start,3));(out/'results.json').write_bytes((json.dumps(results,indent=2)+'\n').encode('utf-8'));print(name,r.returncode,flush=True)
 if r.returncode:raise SystemExit((out/(name+'.log')).read_text(encoding='utf-8')[-2200:])
print('PASS12 actual packaged checks, including all House construction candidates; complete mission/LAN/hardware acceptance remains separate')
