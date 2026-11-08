"""Run the unchanged 90-second per-entry deadline against the packaged House slices."""
from pathlib import Path
import subprocess,time,json
root=Path(__file__).resolve().parents[5]
java=Path('C:/Program Files/Java/jdk-17/bin/java.exe')
tests=['HouseRebuildPhysicsTest','HouseRebuildFittingsTest','HouseRebuildLandscapeTest','HouseRebuildEnvelopeTest','HouseRebuildFunctionsTest','HouseRebuildPowerTest','HouseRebuildFitoutTest','HouseRebuildStoryTest','HouseRebuildFitoutPowerTest','HouseRebuildTextureTest','BeckenburgRegressionManifestTest']
results=[]
for name in tests:
 start=time.monotonic()
 with (Path(__file__).parent/(name+'.log')).open('wb') as log:
  result=subprocess.run([str(java),'-Djava.awt.headless=true','-ea','-cp','build/beckenburg-game.jar;build/test','org.beckenburg.'+name],cwd=root,stdout=log,stderr=subprocess.STDOUT,timeout=90)
 row=dict(test=name,exit_code=result.returncode,seconds=round(time.monotonic()-start,3),deadline=90);results.append(row)
 (Path(__file__).parent/'scoped-results.json').write_bytes((json.dumps(results,indent=2)+'\n').encode())
 print(row,flush=True)
 if result.returncode:raise SystemExit(result.returncode)
