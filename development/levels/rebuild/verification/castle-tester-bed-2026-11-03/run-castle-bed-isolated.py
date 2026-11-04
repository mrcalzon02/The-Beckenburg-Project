import pathlib,subprocess,json,time,shutil
root=pathlib.Path.cwd();out=root/'build/castle-bed-isolated';shutil.copy2(out/'source/regression-tests.txt',out/'regression-tests.txt')
tests=list(json.loads((root/'build/museum-final-results.json').read_text()))+['CastleTesterBedTest','PreparedLightBatchParityTest']
cp=str(out/'castle-bed-verified.jar')+';'+str(out/'build/test');results={}
for test in tests:
 start=time.monotonic()
 with (out/(test+'.log')).open('w',encoding='utf-8') as log:
  p=subprocess.run([r'C:\Program Files\Java\jdk-17\bin\java.exe','-Djava.awt.headless=true','-ea','-cp',cp,'org.beckenburg.'+test],cwd=out if test=='BeckenburgRegressionManifestTest' else root,stdout=log,stderr=subprocess.STDOUT,timeout=150)
 results[test]={'exit':p.returncode,'seconds':round(time.monotonic()-start,3)};(out/'results.json').write_text(json.dumps(results,indent=2)+'\n')
 print(test,p.returncode,flush=True)
 if p.returncode:print((out/(test+'.log')).read_text()[-1000:]);raise RuntimeError(test)
print('PASS31 named isolated-package checks; no suite timeout altered')
