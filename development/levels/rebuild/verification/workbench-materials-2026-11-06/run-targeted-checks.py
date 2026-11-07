import subprocess,json,pathlib,time
root=pathlib.Path.cwd();out=root/'build/workbench-verification';out.mkdir(exist_ok=True)
tests=list(json.loads(pathlib.Path('development/levels/rebuild/verification/castle-imagegen-2026-11-03/results.json').read_text()))+['CastleTapestryTest','CastleHeritageSurfacesTest','CastleHeraldicDisplayTest','CastlePlantFitTest','WorkbenchMaterialTest'];results={}
for name in tests:
 start=time.monotonic()
 with (out/(name+'.log')).open('w',encoding='utf-8') as log:
  r=subprocess.run([r'C:\Program Files\Java\jdk-17\bin\java.exe','-Djava.awt.headless=true','-ea','-cp','build/beckenburg-game.jar;build/test','org.beckenburg.'+name],stdout=log,stderr=subprocess.STDOUT,timeout=150)
 results[name]={'exit':r.returncode,'seconds':round(time.monotonic()-start,3)};(out/'results.json').write_text(json.dumps(results,indent=2)+'\n')
 print(name,r.returncode,flush=True)
 if r.returncode:print((out/(name+'.log')).read_text()[-1400:]);raise RuntimeError(name)
print('PASS37 targeted current-main Castle checks; no round-duration test')
