"""After build.cmd package + test compilation, reproduce this checkpoint's40 checks."""
from pathlib import Path
import subprocess,json,shutil,time
root=Path(__file__).resolve().parents[5]
java=shutil.which('java');assert java,'Java17 must be on PATH'
tests=json.loads(Path(__file__).with_name('results.json').read_text(encoding='utf-8'))
out=root/'build/recheck-castle-finish';out.mkdir(parents=True,exist_ok=True)
for name in tests:
    with (out/(name+'.log')).open('w',encoding='utf-8') as log:
        r=subprocess.run([java,'-Djava.awt.headless=true','-ea','-cp','build/beckenburg-game.jar;build/test','org.beckenburg.'+name],cwd=root,stdout=log,stderr=subprocess.STDOUT,timeout=150)
    assert r.returncode==0,name
    print('PASS',name,flush=True)
