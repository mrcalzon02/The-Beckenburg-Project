"""Compile exact committed Java plus the scoped mill change, preserving unrelated WIP.
Resources are packaged from actual model files; historical classpaths supply sprites/audio.
This is scoped environment acceptance, not a full working-tree/release build.
"""
from pathlib import Path
import subprocess, zipfile, io, shutil, json, os, hashlib, time
ROOT=Path(__file__).resolve().parents[2]
BASE=ROOT/'build/mill-expansion-check';BASE.mkdir(parents=True,exist_ok=True)
def git(*args):return subprocess.check_output(['git',*args],cwd=ROOT)
tree=git('rev-parse','HEAD').decode().strip()
with zipfile.ZipFile(io.BytesIO(git('archive','--format=zip',tree,'src/main/java','src/test/java','development/levels/build_scene.py'))) as archive:archive.extractall(BASE)
overlays=['src/main/java/org/beckenburg/BlackpineProductionAssets.java','src/test/java/org/beckenburg/BlackpineMillExpansionCapture.java','src/test/java/org/beckenburg/BlackpineMillExpansionTest.java']
hashes={}
for name in overlays:
    shutil.copyfile(ROOT/name,BASE/name);hashes[name]=hashlib.sha256((ROOT/name).read_bytes()).hexdigest()
classes=BASE/'classes';classes.mkdir(exist_ok=True)
sources=BASE/'sources.txt';sources.write_text('\n'.join('"'+str(p).replace('\\','/')+'"' for p in (BASE/'src').rglob('*.java')))
jdk=Path('C:/Program Files/Java/jdk-17/bin')
subprocess.run([str(jdk/'javac.exe'),'--release','17','-encoding','UTF-8','-d',str(classes),'@'+str(sources)],cwd=ROOT,check=True)
resources=classes/'org/beckenburg';resources.mkdir(parents=True,exist_ok=True)
for directory in ['models','level-textures']:
    shutil.copytree(ROOT/'assets/runtime'/directory,resources/directory,dirs_exist_ok=True)
for path in (ROOT/'assets/runtime').glob('*.zip'):shutil.copyfile(path,resources/path.name)
classpath=os.pathsep.join(map(str,[classes,ROOT/'build/dressing-source-snapshot/classes',ROOT/'build/blackpine-dressing-check',ROOT/'build/posture-classes',ROOT/'build/classes']))
checks=[]
tasks=[('BlackpineMillExpansionTest',[]),('ProductionAssetTest',[]),('BlackpineStoryTest',[]),('BlackpineDressingTest',[]),('TerrainAndRecoveryTest',[]),('BlackpineGrayboxTest',[]),('AuthoredLevelPhysicsTest',[]),('CrouchAndOutdoorHidingTest',[]),('LevelSurfaceIntegrityTest',[]),('InteractionFocusTest',[]),('AuthoredLevelReachabilityTest',['BLACKPINE']),('BlackpineMillExpansionCapture',[]),('LevelSceneSource',['BLACKPINE'])]
for test,args in tasks:
    start=time.monotonic()
    result=subprocess.run([str(jdk/'java.exe'),'-Xmx3g','-Djava.awt.headless=true','-Djava.util.prefs.PreferencesFactory=org.beckenburg.VerificationPreferencesFactory','-cp',classpath,'org.beckenburg.VerificationMain',test,*args],cwd=ROOT,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=120)
    print(result.stdout,flush=True)
    checks.append(dict(test=test,status='pass' if result.returncode==0 else 'fail',seconds=round(time.monotonic()-start,2),output=result.stdout.strip()))
    report=dict(status='pass' if all(c['status']=='pass' for c in checks) else 'fail',base_commit=tree,scoped_java_overlays=hashes,checks=checks,limitations=['Unrelated uncommitted lighting and castle edits excluded.','No native interactive hunt, LAN map acceptance or target-hardware performance certification.'])
    (ROOT/'development/levels/verification/blackpine-mill-expansion.json').write_text(json.dumps(report,indent=2)+'\n')
    if result.returncode:raise SystemExit(result.returncode)
scene_source=(BASE/'development/levels/build_scene.py').read_text()
scene_source=scene_source.replace("'-dressed.blend' if name=='blackpine'", "'-mill-expanded.blend' if name=='blackpine'")
scene_source=scene_source.replace("suffix='-dressed' if name=='blackpine'", "suffix='-mill-expanded' if name=='blackpine'")
(ROOT/'development/levels/_mill_scene.py').write_text(scene_source)
print('PASS scoped Java 17 compile, model packaging, physical checks, swept routes, runtime captures and exact scene export',flush=True)
