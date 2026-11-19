"""Compile exact current sources and inspect shared atmosphere without editor UI."""
from pathlib import Path
import argparse,hashlib,json,os,subprocess,time
ROOT=Path(__file__).resolve().parents[1]
def main():
    p=argparse.ArgumentParser();p.add_argument('--gpu',action='store_true');p.add_argument('--courtyard',action='store_true');args=p.parse_args()
    build=ROOT/'build/atmosphere-classes';build.mkdir(exist_ok=True);home=ROOT/'build/atmosphere-home';home.mkdir(exist_ok=True);temp=ROOT/'build/atmosphere-temp';temp.mkdir(exist_ok=True)
    evidence=ROOT/'development/levels/rebuild/verification/atmosphere-all-levels';evidence.mkdir(parents=True,exist_ok=True)
    java=Path('C:/Program Files/Java/jdk-17/bin');jar=ROOT/'build/beckenburg-game.jar'
    tests=['LevelAtmosphereTest','AllLevelAtmosphereCapture','VerificationPreferencesFactory','WeatherSkyWorldAnchorTest','MissionWeatherTest','RecordedWeatherTest','WeatherAssetsIntegrationTest','GpuReadbackTest','GpuWorldRendererContractTest','SafeAreaHazeTest','WaterReviewRegressionTest','CastleHubIntegrationTest']
    sources=[f for base in ['src/main/java','water-module/src/main/java','lighting-module/src/main/java'] for f in (ROOT/base).rglob('*.java')]+[ROOT/f'src/test/java/org/beckenburg/{t}.java' for t in tests]
    argfile=ROOT/'build/atmosphere-sources.txt';argfile.write_bytes(('\n'.join('"'+f.as_posix()+'"' for f in sources)).encode())
    subprocess.run([str(java/'javac.exe'),'--release','17','-encoding','UTF-8','-d',str(build),'@'+str(argfile)],check=True)
    cp=str(build)+os.pathsep+str(jar)
    flags=['-Djava.awt.headless=true','-Djava.util.prefs.PreferencesFactory=org.beckenburg.VerificationPreferencesFactory','-Duser.home='+str(home),'-Djava.io.tmpdir='+str(temp),'-Xmx2g','-ea']
    runs=[]
    def run(name,command):
        start=time.monotonic();r=subprocess.run(command,cwd=ROOT,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,encoding='utf-8');(evidence/(name+'.log')).write_bytes(r.stdout.encode())
        runs.append(dict(name=name,exit_code=r.returncode,seconds=round(time.monotonic()-start,3)));print(name,r.returncode,r.stdout[-1000:],flush=True)
        (evidence/(('courtyard-' if args.courtyard else '')+('gpu-checks.json' if args.gpu else 'software-checks.json'))).write_bytes((json.dumps(runs,indent=2)+'\n').encode())
        if r.returncode:raise SystemExit(r.returncode)
    if not args.gpu:
        for test in ([] if args.courtyard else tests):
            if test in ['AllLevelAtmosphereCapture','VerificationPreferencesFactory']:continue
            run(test,[str(java/'java.exe'),*flags,'-cp',cp,'org.beckenburg.'+test])
        if not args.courtyard:run('synthetic-gpu-composite-control',[str(java/'java.exe'),*flags,'-cp',cp,'org.beckenburg.LevelAtmosphereTest','--gpu-composite-control'])
        run('courtyard-software' if args.courtyard else 'runtime-software',[str(java/'java.exe'),*flags,'-cp',cp,'org.beckenburg.AllLevelAtmosphereCapture',str(ROOT/'development/levels/rebuild/captures/atmosphere-all-levels/software'),*(['--courtyard-only'] if args.courtyard else [])])
    else:
        m2=Path('C:/Users/Admin/.m2/repository');jars=[]
        for pattern in ['com/badlogicgames/gdx/*/1.14.2/*.jar','com/badlogicgames/gdx/gdx-jnigen-loader/*/*.jar','org/lwjgl/*/3.3.3/*.jar','com/badlogicgames/jlayer/*/*/*.jar','org/jcraft/jorbis/*/*.jar']:jars.extend(sorted(m2.glob(pattern)))
        cp+=os.pathsep+os.pathsep.join(str(f) for f in jars);gpu=ROOT/'build/atmosphere-gpu-classes';gpu.mkdir(exist_ok=True)
        subprocess.run([str(java/'javac.exe'),'--release','17','-encoding','UTF-8','-cp',cp,'-d',str(gpu),str(ROOT/'gpu-client/src/main/java/org/beckenburg/gpu/OpenGlWorldRenderer.java'),str(ROOT/'gpu-client/src/test/java/org/beckenburg/gpu/LevelAtmosphereGpuCapture.java')],check=True)
        run('courtyard-actual-opengl' if args.courtyard else 'runtime-actual-opengl',[str(java/'java.exe'),*flags,'-cp',str(gpu)+os.pathsep+cp,'org.beckenburg.gpu.LevelAtmosphereGpuCapture',str(ROOT/'development/levels/rebuild/captures/atmosphere-all-levels/opengl'),*(['--courtyard-only'] if args.courtyard else [])])
    print('PASS scoped atmosphere verification; full gameplay/CI/hardware performance acceptance remains separate')
if __name__=='__main__':main()
