"""Author saved editor terrain without opening or controlling a local editor.

The seed ref is explicit: source GLBs are immutable inputs from a checkpoint,
not recursively resampled from their own derived terrain exports.
"""
from pathlib import Path
import argparse,hashlib,json,os,subprocess

ROOT=Path(__file__).resolve().parents[1]

def main():
    p=argparse.ArgumentParser();p.add_argument('--sites',nargs='+',required=True)
    p.add_argument('--seed-ref',required=True);p.add_argument('--blender',default='C:/Program Files/Blender Foundation/Blender 5.0/blender.exe')
    p.add_argument('--terrain-source',nargs='*',default=[],help='Explicit same-world source mapping, e.g. WLD-07=WLD-07-MORTUARY; recorded through actual seed paths')
    args=p.parse_args();sites=[s.lower() for s in args.sites]
    seed=subprocess.check_output(['git','rev-parse',args.seed_ref],cwd=ROOT,text=True).strip()
    subprocess.run([args.blender,'--background','--factory-startup','--python-exit-code','1','--python','tools/sample_roster_terrain.py','--','--sites',*sites,'--source-ref',seed,'--terrain-source',*args.terrain_source],cwd=ROOT,check=True)
    java=Path(os.environ.get('JAVA_HOME','C:/Program Files/Java/jdk-17'))/'bin';build=ROOT/'build/roster-terrain';build.mkdir(parents=True,exist_ok=True)
    jar=ROOT/'build/beckenburg-game.jar';assert jar.exists(),'Build the game JAR first'
    subprocess.run([str(java/'javac.exe'),'-cp',str(jar),'-d',str(build),str(ROOT/'tools/java/RosterTerrainBuild.java')],cwd=ROOT,check=True)
    subprocess.run([str(java/'java.exe'),'-Djava.awt.headless=true','-cp',str(build)+os.pathsep+str(jar),'org.beckenburg.RosterTerrainBuild',*sites],cwd=ROOT,check=True)
    for site in sites:
        world=ROOT/f'development/levels/rebuild/{site}/terrain-world';layout=ROOT/f'development/levels/rebuild/{site}/rough-layout.json'
        if not layout.exists():
            layout.parent.mkdir(parents=True,exist_ok=True)
            layout.write_bytes((ROOT/f'development/levels/layout-expansions/{site}/layout.json').read_bytes())
        data=json.loads(layout.read_text(encoding='utf-8'))
        files={f.relative_to(ROOT).as_posix():hashlib.sha256(f.read_bytes()).hexdigest() for f in world.rglob('*') if f.is_file() and not f.name.endswith('.lock')}
        data['saved_editor_terrain']=dict(status='PERSISTED_EDITOR_PUNCH_CANDIDATE_NOT_TERRAIN_ACCEPTED',world=world.relative_to(ROOT).as_posix(),
            mesh=(world/'scene-package/meshes/site.obj').relative_to(ROOT).as_posix(),native_physics=(world/'terrain-physics.hcs2').relative_to(ROOT).as_posix(),files_sha256=files,
            source_seed_commit=seed,builder_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
            kernel_jar_sha256=hashlib.sha256(jar.read_bytes()).hexdigest(),
            remaining='Retaining/foundation rims, guards, arrival/canonical van, source-specific terrain finish and complete movement/visual acceptance')
        layout.write_bytes((json.dumps(data,indent=2)+'\n').encode())
        print('BOUND_SAVED_EDITOR_TERRAIN',site,flush=True)

if __name__=='__main__':main()
