"""Compile and verify optical evidence against real map resources and TCP sockets.
Bootstrap resources once with verify_mill_lighting.py if this build is absent.
"""
from pathlib import Path
import hashlib,json,subprocess,time,shutil
ROOT=Path(__file__).resolve().parents[2];BASE=ROOT/'build/mill-lighting-check';C=BASE/'classes'
JDK=Path('C:/Program Files/Java/jdk-17/bin')
java=str(JDK/'java.exe') if JDK.exists() else shutil.which('java')
javac=str(JDK/'javac.exe') if JDK.exists() else shutil.which('javac')
if not (C/'org/beckenburg/models/level_utility_light.glb').exists():
    subprocess.run([shutil.which('python') or 'python',str(ROOT/'development/levels/verify_mill_lighting.py')],cwd=ROOT,check=True)
args=BASE/'optical-sources.txt';args.write_text('\n'.join('"'+p.as_posix()+'"' for p in (ROOT/'src').rglob('*.java')),encoding='utf-8')
subprocess.run([javac,'--release','17','-encoding','UTF-8','-d',str(C),'@'+str(args)],cwd=ROOT,check=True,timeout=120)
tests=['OpticalEvidenceTest','OpticalEvidenceCapture','LanOpticalEvidenceNetworkTest','TieredMediaAndPowerTest','TieredUvAndFirelightTest','FieldEquipmentTest','EquipmentStoreDescriptionsTest','AdditionalEvidenceTest','LanDotsProjectorsTest','LanFieldEquipmentIntegrationTest','LanFieldSensorNetworkTest','VanSensorStationsTest','GpuWorldRendererContractTest','RendererVisibilityTest','LanCaseTest','LanRitualDefenseTest','LanWorldRenderTest','BlackpineDeckDepthTest','BlackpineLightingTest','SoloEquipmentUseTest','SoloEnvironmentalEvidenceTest']
checks=[];report=dict(status='running',base_commit=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),java_17_compile=True,checks=checks,limitations=['Controlled optics screenshots are test fixtures, not completed mission/human-play acceptance.','GPU RGB submission is checked headlessly; actual native OpenGL hardware, multi-PC LAN and performance acceptance remain pending.','UV cubes support six cardinal normals; current genuine passage producer places floor residue, not invented handprints.','Native laser/DOTS visual power and occlusion require per-site reviews; shared logic does not certify old maps.'])
out=ROOT/'development/levels/verification/optical-evidence.json'
for test in tests:
    start=time.monotonic();p=subprocess.run([java,'-Xmx3g','-Djava.awt.headless=true','-Djava.util.prefs.PreferencesFactory=org.beckenburg.VerificationPreferencesFactory','-Djava.io.tmpdir='+str(BASE/'tmp'),'-cp',str(C),'org.beckenburg.VerificationMain',test],cwd=ROOT,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=120)
    checks.append(dict(test=test,status='pass' if p.returncode==0 else 'fail',seconds=round(time.monotonic()-start,2),output=p.stdout.strip()))
    report['status']='pass' if all(c['status']=='pass' for c in checks) else 'fail';out.write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    print(test,p.returncode,p.stdout[-1300:],flush=True)
    if p.returncode:raise SystemExit(p.returncode)
report['captures']={p.relative_to(ROOT).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted((ROOT/'development/levels/previews/optical-evidence').glob('*.png'))}
changed=subprocess.check_output(['git','ls-files','--modified','--others','--exclude-standard','src'],cwd=ROOT,text=True).splitlines()
report['source_hashes']={p:hashlib.sha256((ROOT/p).read_bytes()).hexdigest() for p in sorted(set(changed)) if p.endswith('.java')}
out.write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
