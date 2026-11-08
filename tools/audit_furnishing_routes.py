"""Read-only before/after route regression audit using actual native geometry."""
from pathlib import Path
import argparse,csv,hashlib,json,math,os,subprocess

ROOT=Path(__file__).resolve().parents[1]

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--output',type=Path,required=True);parser.add_argument('--authored-stairs',action='store_true');args=parser.parse_args()
    plans=json.loads((ROOT/'development/levels/rebuild/furnishings/room-programs.json').read_text())['sites']
    build=ROOT/'build/furnishing-route-probe';build.mkdir(parents=True,exist_ok=True);rows=[];hashes={};names=[]
    for site in plans:
        base=ROOT/f'development/levels/rebuild/structures/{site.lower()}';new=ROOT/f'development/levels/rebuild/furnishings/{site.lower()}'
        b=json.loads((base/'physics-contract.json').read_text());n=json.loads((new/'physics-contract.json').read_text())
        for folder,contract,mesh,physics in [(base,b,'rough-shell.glb','structure-physics.hcs2'),(new,n,'furnished.glb','furnished-physics.hcs2')]:
            assert hashlib.sha256((folder/mesh).read_bytes()).hexdigest()==contract['source_glb_sha256']
            assert hashlib.sha256((folder/physics).read_bytes()).hexdigest()==contract['physics_sha256']
        hashes[site]=dict(before=b['physics_sha256'],after=n['physics_sha256'])
        for phase,contract in [('before',b),('after',n)]:
            for name in contract['stair_components']:names.append(f'{site}:{phase}\t{name}')
        report=json.loads((base/'implementation.json').read_text());layout=json.loads((ROOT/report['authoritative_input']).read_text())
        for link in layout['links']:
            if link.get('player_access')=='SEALED_NO_ROUTE':continue
            for segment,(p,q) in enumerate(zip(link['points'],link['points'][1:])):
                run=math.hypot(q[0]-p[0],q[2]-p[2])
                if abs(p[1]-q[1])>.01 or run<.01:continue
                lateral=max(0,link['width']/2-.22-.05)
                for lane,offset in [('centre',0),('left',lateral),('right',-lateral)]:
                    count=math.ceil(run/.25)
                    for i in range(count+1):
                        x=p[0]+(q[0]-p[0])*i/count-(q[2]-p[2])/run*offset
                        z=p[2]+(q[2]-p[2])*i/count+(q[0]-p[0])/run*offset
                        rows.append('\t'.join(str(v) for v in [site,base/'structure-physics.hcs2',b['physics_sha256'],new/'furnished-physics.hcs2',n['physics_sha256'],link['id']+':'+str(segment),x,p[1],z,lane]))
    source=build/'input.tsv';source.write_bytes(('\n'.join(rows)+'\n').encode())
    java=Path(os.environ.get('JAVA_HOME','C:/Program Files/Java/jdk-17'))/'bin';jar=ROOT/'build/beckenburg-game.jar'
    # Always compile current source explicitly; avoid loading an older loose
    # class from a previous opt-in audit ahead of the dependency JAR.
    kernel=ROOT/'src/main/java/org/beckenburg/AuthoredSitePhysics.java'
    subprocess.run([str(java/'javac.exe'),'-cp',str(jar),'-d',str(build),str(kernel),str(ROOT/'tools/java/FurnishingRouteProbe.java')],check=True)
    (build/'stairs.tsv').write_bytes(('\n'.join(names)+'\n').encode())
    output=build/'results.tsv';subprocess.run([str(java/'java.exe'),'-Djava.awt.headless=true','-cp',str(build)+os.pathsep+str(jar),'FurnishingRouteProbe',str(source),str(output),*([str(build/'stairs.tsv')] if args.authored_stairs else [])],check=True)
    probes=list(csv.DictReader(output.read_text(encoding='utf-8').splitlines(),delimiter='\t'));summary={}
    for site in plans:
        selected=[r for r in probes if r['site']==site]
        summary[site]=dict(samples=len(selected),**{k:sum(r[k]=='true' for r in selected) for k in ['baseline_valid','new_block','baseline_sweep','new_sweep_block']})
    args.output.mkdir(parents=True,exist_ok=True);(args.output/'before-after-routes.tsv').write_bytes(output.read_bytes().replace(b'\r\n',b'\n'))
    report=dict(scope='25 candidate maps; flat authored routes at centre/side lanes, with actual actor capsule and baseline-qualified movement sweeps',
        physics=hashes,kernel_jar_sha256=hashlib.sha256(jar.read_bytes()).hexdigest(),sites=summary,
        kernel_source_sha256=hashlib.sha256(kernel.read_bytes()).hexdigest(),authored_stairs=args.authored_stairs,
        limitation='Preserving previously valid paths does not accept pre-existing failures, stairs, complete navigation, gameplay or finished models.')
    (args.output/'route-regressions.json').write_bytes((json.dumps(report,indent=2)+'\n').encode())
    print({k:sum(v[k] for v in summary.values()) for k in ['samples','baseline_valid','new_block','baseline_sweep','new_sweep_block']})
    assert all(r['new_block']==0 and r['new_sweep_block']==0 for r in summary.values()),'Placed source prop obstructs a previously valid native route'

if __name__=='__main__':main()
