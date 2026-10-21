"""Compile/check current authored source without changing game preferences or an installed build."""
from pathlib import Path
import subprocess,os,json,shutil,time
ROOT=Path(__file__).resolve().parents[2]
BUILD=ROOT/'build/blackpine-dressing-check';BUILD.mkdir(parents=True,exist_ok=True)
JDK=Path(os.environ.get('JAVA_HOME','C:/Program Files/Java/jdk-17'))/'bin'
sources=list((ROOT/'src/main/java').rglob('*.java'))+list((ROOT/'src/test/java').rglob('*.java'))
argfile=BUILD/'sources.txt';argfile.write_text('\n'.join('"'+str(p).replace('\\','/')+'"' for p in sources))
subprocess.run([str(JDK/'javac.exe'),'-encoding','UTF-8','-d',str(BUILD),'@'+str(argfile)],cwd=ROOT,check=True)
resources=BUILD/'org/beckenburg'
for folder in ['models','level-textures']:
    shutil.copytree(ROOT/'assets/runtime'/folder,resources/folder,dirs_exist_ok=True)
classpath=os.pathsep.join([str(BUILD),str(ROOT/'build/posture-classes'),str(ROOT/'build/classes')])
tests=['BlackpineDressingTest','FoliageVariationTest','SceneryInstancesTest','BlackpineGrayboxTest','CrouchAndOutdoorHidingTest','AuthoredLevelPhysicsTest','LevelSurfaceIntegrityTest','InteractionFocusTest','LevelEnclosureTest','GraphicsOptionsTest','WorldViewHandednessTest','HeldDeviceReadoutTest','LanCaseFileHudTest','LanGhostHearingHudTest','LanIncapacitationHudTest','LanTelegraphedHudTest','LanSoundHudTest','ManualEvidenceHudTest']
results=[]
for test in tests:
    start=time.monotonic()
    p=subprocess.run([str(JDK/'java.exe'),'-Xmx2g','-Djava.awt.headless=true','-Djava.util.prefs.PreferencesFactory=org.beckenburg.VerificationPreferencesFactory','-cp',classpath,'org.beckenburg.'+test],cwd=ROOT,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
    print(p.stdout,flush=True);results.append(dict(test=test,status='pass' if p.returncode==0 else 'fail',seconds=round(time.monotonic()-start,2),output=p.stdout.strip()))
    if p.returncode:raise SystemExit(p.returncode)
out=ROOT/'development/levels/verification/blackpine-dressing.json'
out.write_text(json.dumps({'source':'current workspace Java source; isolated classes and current runtime meshes','checks':results,'limitations':['No native interactive walkthrough, hunt balance or target-hardware performance acceptance.']},indent=2)+'\n')
print('PASS compilation and dressing checks',flush=True)
