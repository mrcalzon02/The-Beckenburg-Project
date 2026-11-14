"""Headless map probe; never starts the game or an editor window.

Loads hash-bound HCS2 through the real native kernel. Support/headroom and full
movement results are separate: a clear centre ray is not a capsule sweep.
"""
from pathlib import Path
import argparse,csv,hashlib,json,os,subprocess

ROOT=Path(__file__).resolve().parents[1]

def main():
    parser=argparse.ArgumentParser()
    selection=parser.add_mutually_exclusive_group(required=True)
    selection.add_argument('--sites',nargs='+');selection.add_argument('--remaining',action='store_true')
    parser.add_argument('--output',type=Path,required=True)
    parser.add_argument('--furnished',action='store_true')
    parser.add_argument('--authored-stairs',action='store_true')
    parser.add_argument('--require-clear-spines',action='store_true',help='Fail if any tread lacks support or a clear head-height centre ray; does not accept capsule movement')
    args=parser.parse_args()
    if args.remaining:
        roster=json.loads((ROOT/'development/levels/rebuild/structures/roster.json').read_text(encoding='utf-8'))
        args.sites=[row['id'] for row in roster['sites'] if row['status']=='NATIVE_STRUCTURE_BLOCKING_RUNTIME_GATES_OPEN']
    build=ROOT/'build/roster-route-probe';build.mkdir(parents=True,exist_ok=True)
    rows=[];sources={};names=[]
    for site in args.sites:
        folder=ROOT/('development/levels/rebuild/furnishings' if args.furnished else 'development/levels/rebuild/structures')/site.lower()
        contract=json.loads((folder/'physics-contract.json').read_text(encoding='utf-8'))
        mesh='furnished.glb' if args.furnished else 'rough-shell.glb';physics='furnished-physics.hcs2' if args.furnished else 'structure-physics.hcs2'
        for filename,key in [(mesh,'source_glb_sha256'),(physics,'physics_sha256')]:
            assert hashlib.sha256((folder/filename).read_bytes()).hexdigest()==contract[key],(site,filename)
        sources[site]={key:contract[key] for key in ['source_glb_sha256','physics_sha256']}
        sources[site]['physics_contract_sha256']=hashlib.sha256((folder/'physics-contract.json').read_bytes()).hexdigest()
        for name in contract['stair_components']:names.append(f'{site}\t{name}')
        for pose in contract['tread_centres']:
            rows.append('\t'.join(str(v) for v in [site,folder/physics,contract['physics_sha256'],
                pose['link'],pose['component'],pose['x'],pose['y'],pose['z']]))
    manifest=build/'input.tsv';manifest.write_bytes(('\n'.join(rows)+'\n').encode('utf-8'))
    java=Path(os.environ.get('JAVA_HOME','C:/Program Files/Java/jdk-17'))/'bin'
    jar=ROOT/'build/beckenburg-game.jar';assert jar.exists(),'Build the game jar first'
    kernel=ROOT/'src/main/java/org/beckenburg/AuthoredSitePhysics.java'
    subprocess.run([str(java/'javac.exe'),'-cp',str(jar),'-d',str(build),str(kernel),str(ROOT/'tools/java/RosterRouteProbe.java')],check=True)
    (build/'stairs.tsv').write_bytes(('\n'.join(names)+'\n').encode())
    output=build/'results.tsv'
    subprocess.run([str(java/'java.exe'),'-Djava.awt.headless=true','-cp',str(build)+os.pathsep+str(jar),
        'RosterRouteProbe',str(manifest),str(output),*([str(build/'stairs.tsv')] if args.authored_stairs else [])],check=True)
    probes=list(csv.DictReader(output.read_text(encoding='utf-8').splitlines(),delimiter='\t'))
    summary={}
    for site in args.sites:
        selected=[p for p in probes if p['site']==site]
        summary[site]=dict(treads=len(selected),**{key:sum(p[key]=='true' for p in selected)
            for key in ['supported','spine_clear','body_clear','traversable']})
    args.output.mkdir(parents=True,exist_ok=True)
    (args.output/'tread-probes.tsv').write_bytes(output.read_bytes().replace(b'\r\n',b'\n'))
    report=dict(status='MEASURED_CANDIDATE_NOT_RUNTIME_ACCEPTED',kernel='Current compiled AuthoredSitePhysics; JAR supplies dependencies',
        kernel_source_sha256=hashlib.sha256(kernel.read_bytes()).hexdigest(),authored_stairs=args.authored_stairs,
        kernel_jar_sha256=hashlib.sha256(jar.read_bytes()).hexdigest(),sources=sources,sites=summary,
        limitation='Tread-centre support and spine rays do not prove swept movement. Full body/traversable failures remain blockers; no editor UI or playable unlock.')
    (args.output/'route-probes.json').write_bytes((json.dumps(report,indent=2)+'\n').encode('utf-8'))
    print(json.dumps(summary,indent=2))
    if args.require_clear_spines:
        assert all(v['supported']==v['treads'] and v['spine_clear']==v['treads'] for v in summary.values()),'Native support/head-height obstruction remains'

if __name__=='__main__':main()
