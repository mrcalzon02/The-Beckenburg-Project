import subprocess,json,pathlib,time
out=pathlib.Path('build/castle-native-verification');out.mkdir(exist_ok=True)
tests=list(json.loads(pathlib.Path('build/castle-finish-verification/results.json').read_text(encoding='utf-8')))
for name in ['CastleRebuildUpgradeTest','CastleDocumentRecoveryTest','CastleSnapshotContractTest','EditorAgentServiceTest','EditorDiskRevisionTest']:
    if name not in tests:tests.append(name)
results={}
for name in tests:
    start=time.monotonic();args=[]
    if name=='CastleRebuildUpgradeTest':args=['development/levels/rebuild/captures/castle-native-upgrade']
    with (out/(name+'.log')).open('w',encoding='utf-8') as log:
        r=subprocess.run([r'C:\Program Files\Java\jdk-17\bin\java.exe','-Djava.awt.headless=true','-ea','-cp','build/beckenburg-game.jar;build/test','org.beckenburg.'+name,*args],stdout=log,stderr=subprocess.STDOUT,timeout=150)
    results[name]={'exit':r.returncode,'seconds':round(time.monotonic()-start,3)};(out/'results.json').write_text(json.dumps(results,indent=2)+'\n',encoding='utf-8');print(name,r.returncode,flush=True)
    if r.returncode:print((out/(name+'.log')).read_text(encoding='utf-8')[-2000:]);raise RuntimeError(name)
subprocess.run([r'C:\Program Files\Java\jdk-17\bin\java.exe','-cp','build/beckenburg-game.jar','org.beckenburg.BeckenburgRegressionSuite','--validate'],check=True)
print('PASS',len(tests),'actual packaged Castle/editor/rite checks; manifest preflight distinct')
