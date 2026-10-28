"""Run bounded, isolated real Java checks against a freshly packaged Castle build.
Usage: python tools/review_castle_rebuild.py --java <JDK17/java.exe> [--all]
Packaging and the actual GPU survey are separate gates.
"""
import argparse,json,os,subprocess,time
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
ap=argparse.ArgumentParser();ap.add_argument('--java',required=True);ap.add_argument('--all',action='store_true');args=ap.parse_args()
folder=ROOT/'build/castle-review';folder.mkdir(parents=True,exist_ok=True)
temp=ROOT/'build/castle-temp';temp.mkdir(exist_ok=True)
if args.all:
    tests=[s.strip() for s in (ROOT/'regression-tests.txt').read_text(encoding='utf-8').splitlines() if s.strip() and not s.startswith('#')]
else:
    tests=sorted(p.stem for p in (ROOT/'src/test/java/org/beckenburg').glob('Castle*Test.java') if 'Visual' not in p.stem)
    tests+=['LightingOcclusionRepairTest','VanLightingAndCastleDoorTest','EquipmentStoreDescriptionsTest','CampaignProgressionAndStoreTest','SurroundingClearanceTest']
report=folder/('full-regressions.json' if args.all else 'focused-tests.json');results=[]
for test in tests:
    if test=='SmokeTest' and not (ROOT/'build/test/org/beckenburg/SmokeTest.class').exists():
        results.append(dict(test=test,status='not-present-optional',exit=0));continue
    command=[args.java,'-Djava.io.tmpdir='+str(temp),'-Duser.home='+str(folder/'home'),
        '-Djava.util.prefs.PreferencesFactory=org.beckenburg.VerificationPreferencesFactory',
        '-Djava.awt.headless=true','-ea','-cp',os.pathsep.join(['build/test','build/classes']),
        'org.beckenburg.VerificationMain',test]
    start=time.monotonic()
    try:
        p=subprocess.run(command,cwd=ROOT,capture_output=True,text=True,timeout=90)
        result=dict(test=test,exit=p.returncode,status='pass' if p.returncode==0 else 'fail',output=p.stdout+p.stderr)
    except subprocess.TimeoutExpired as e:
        result=dict(test=test,exit=124,status='timeout',output='Process exceeded90seconds; no pass inferred.')
    result['seconds']=round(time.monotonic()-start,3);results.append(result)
    report.write_text(json.dumps(results,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
    print(test,result['status'],flush=True)
    if result['exit']:print(result['output'][-2200:],flush=True)
print('RESULT',sum(r['status']=='pass' for r in results),'passed;',sum(r['exit']!=0 for r in results),'failed;',len(results),'entries',flush=True)
raise SystemExit(any(r['exit'] for r in results))
