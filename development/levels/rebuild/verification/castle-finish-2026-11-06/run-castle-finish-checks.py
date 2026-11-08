import subprocess,json,pathlib,time
root=pathlib.Path.cwd();out=root/'build/castle-finish-verification';out.mkdir(exist_ok=True)
tests=list(json.loads(pathlib.Path('build/training-verification/results.json').read_text(encoding='utf-8')))+['CastleFinishPlacementTest'];results={}
for name in tests:
    start=time.monotonic()
    with (out/(name+'.log')).open('w',encoding='utf-8') as log:
        r=subprocess.run([r'C:\Program Files\Java\jdk-17\bin\java.exe','-Djava.awt.headless=true','-ea','-cp','build/beckenburg-game.jar;build/test','org.beckenburg.'+name],stdout=log,stderr=subprocess.STDOUT,timeout=150)
    results[name]={'exit':r.returncode,'seconds':round(time.monotonic()-start,3)};(out/'results.json').write_text(json.dumps(results,indent=2)+'\n',encoding='utf-8')
    print(name,r.returncode,flush=True)
    if r.returncode:print((out/(name+'.log')).read_text(encoding='utf-8')[-1600:]);raise RuntimeError(name)
print('PASS40 targeted final-source Castle/rite checks; no round-duration test')
