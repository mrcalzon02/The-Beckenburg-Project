"""Compile current unified sources and verify one map-specific fit-out iteration.
Run the asset generator first; this check uses actual runtime resources.
JDK 17 must be on PATH or provided through JAVA_HOME.
"""
from pathlib import Path
import hashlib, json, os, shutil, subprocess, time

ROOT=Path(__file__).resolve().parents[2]
BASE=ROOT/'build/mill-lighting-check'
CLASSES=BASE/'classes'
CLASSES.mkdir(parents=True,exist_ok=True)
(BASE/'tmp').mkdir(exist_ok=True)
jdk=Path(os.environ['JAVA_HOME'])/'bin' if os.environ.get('JAVA_HOME') else None
def tool(name):
    candidate=(jdk or Path('C:/Program Files/Java/jdk-17/bin'))/(name+('.exe' if os.name=='nt' else ''))
    if candidate.exists():return str(candidate)
    found=shutil.which(name)
    if found:return found
    raise FileNotFoundError('JDK 17 required: '+name)
argfile=BASE/'sources.txt'
argfile.write_text('\n'.join('"'+p.as_posix()+'"' for p in (ROOT/'src').rglob('*.java')),encoding='utf-8')
subprocess.run([tool('javac'),'--release','17','-encoding','UTF-8','-d',str(CLASSES),'@'+str(argfile)],cwd=ROOT,check=True,timeout=120)
resources=CLASSES/'org/beckenburg'
for p in (ROOT/'assets/runtime').iterdir():
    if p.is_dir():shutil.copytree(p,resources/p.name,dirs_exist_ok=True)
    elif p.suffix=='.zip':shutil.copyfile(p,resources/p.name)
shutil.copyfile(ROOT/'assets/runtime/ui/psx-horror-ui-free-assets.zip',resources/'psx-horror-ui-free-assets.zip')
(resources/'ghost-sprite-expansion-v3.zip').write_bytes(b''.join(p.read_bytes() for p in sorted((ROOT/'assets/runtime/ghost-sprite-parts').glob('*.part*'))))
java=[tool('java'),'-Xmx3g','-Djava.awt.headless=true','-Djava.io.tmpdir='+str(BASE/'tmp'),'-Djava.util.prefs.PreferencesFactory=org.beckenburg.VerificationPreferencesFactory','-cp',str(CLASSES)]
subprocess.run(java+['org.beckenburg.TierModelForge',str(resources/'models')],cwd=ROOT,check=True,stdout=subprocess.DEVNULL)
# Sparse checkouts can retrieve this tracked source without modifying the checkout.
kit=ROOT/'assets/source/kenney_retro-medieval-kit.zip'
if not kit.exists():
    kit=BASE/'kenney_retro-medieval-kit.zip'
    kit.write_bytes(subprocess.check_output(['git','show','HEAD:assets/source/kenney_retro-medieval-kit.zip'],cwd=ROOT))
subprocess.run(java+['org.beckenburg.KenneyMedievalAssets',str(kit),str(resources/'models')],cwd=ROOT,check=True,stdout=subprocess.DEVNULL)
checks=[]
tasks=[('BlackpineLightingTest',[]),('BlackpineMillExpansionTest',[]),('ProductionAssetTest',[]),('BlackpineStoryTest',[]),('BlackpineGrayboxTest',[]),('TerrainAndRecoveryTest',[]),('AuthoredLevelPhysicsTest',[]),('LevelSurfaceIntegrityTest',[]),('LightingOcclusionRepairTest',[]),('PhysicalLightingTest',[]),('MergedMaterialMetadataTest',[]),('AuthoredLevelReachabilityTest',['BLACKPINE']),('BlackpineLightingCapture',[]),('LevelSceneSource',['BLACKPINE'])]
paths=['src/main/java/org/beckenburg/BlackpineLighting.java','src/main/java/org/beckenburg/BlackpineLevel.java','src/test/java/org/beckenburg/BlackpineLightingTest.java','assets/runtime/models/level_mill_suspension.glb']
report=dict(base_commit=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),source_hashes={p:hashlib.sha256((ROOT/p).read_bytes()).hexdigest() for p in paths},java_17_compile=True,checks=checks,limitations=['Headless runtime captures; native-window and actual OpenGL performance acceptance remain pending.','No change to engine lighting, map unlocks, protocol IDs or other site layouts.'])
for name,args in tasks:
    start=time.monotonic()
    p=subprocess.run(java+['org.beckenburg.VerificationMain',name,*args],cwd=ROOT,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=180)
    checks.append(dict(test=name,status='pass' if p.returncode==0 else 'fail',seconds=round(time.monotonic()-start,2),output=p.stdout.strip()))
    report['status']='pass' if all(c['status']=='pass' for c in checks) else 'fail'
    (ROOT/'development/levels/verification/blackpine-worklights.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    print(name,p.returncode,p.stdout[-2000:],flush=True)
    if p.returncode:raise SystemExit(p.returncode)
report['capture_hashes']={p.relative_to(ROOT).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted((ROOT/'development/levels/previews/mill-lighting').glob('*.png'))}
(ROOT/'development/levels/verification/blackpine-worklights.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
