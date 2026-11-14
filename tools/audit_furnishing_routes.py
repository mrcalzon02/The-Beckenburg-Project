"""Read-only before/after route regression audit using actual native geometry."""
from pathlib import Path
import argparse,csv,hashlib,json,math,os,subprocess

ROOT=Path(__file__).resolve().parents[1]

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--output',type=Path,required=True);parser.add_argument('--authored-stairs',action='store_true')
    parser.add_argument('--sites',nargs='+');parser.add_argument('--revision-ref',help='Compare earlier furnished scene to current scene on unchanged authored links')
    args=parser.parse_args()
    plans=json.loads((ROOT/'development/levels/rebuild/furnishings/room-programs.json').read_text())['sites']
    if args.sites:
        assert set(args.sites)<=set(plans),'Unknown site';plans={s:plans[s] for s in args.sites}
    build=ROOT/'build/furnishing-route-probe';build.mkdir(parents=True,exist_ok=True);rows=[];hashes={};names=[]
    revision=subprocess.check_output(['git','rev-parse',args.revision_ref],cwd=ROOT,text=True).strip() if args.revision_ref else None
    skipped={}
    def historical(path):return subprocess.check_output(['git','show',revision+':'+path],cwd=ROOT)
    for site in plans:
        base=ROOT/f'development/levels/rebuild/structures/{site.lower()}';new=ROOT/f'development/levels/rebuild/furnishings/{site.lower()}'
        b=json.loads((base/'physics-contract.json').read_text());n=json.loads((new/'physics-contract.json').read_text())
        report=json.loads((base/'implementation.json').read_text());layout=json.loads((ROOT/report['authoritative_input']).read_text());before_mesh='rough-shell.glb';before_physics='structure-physics.hcs2'
        if revision:
            previous=build/'revision'/revision/site.lower();previous.mkdir(parents=True,exist_ok=True)
            for filename in ['physics-contract.json','furnished.glb','furnished-physics.hcs2']:
                (previous/filename).write_bytes(historical(f'development/levels/rebuild/furnishings/{site.lower()}/{filename}'))
            b=json.loads((previous/'physics-contract.json').read_text());base=previous;before_mesh='furnished.glb';before_physics='furnished-physics.hcs2'
            old_report=json.loads(historical(f'development/levels/rebuild/structures/{site.lower()}/implementation.json'))
            old_layout=json.loads(historical(old_report['authoritative_input']));current={l['id']:l for l in layout['links']}
            selected=[];skipped[site]=[]
            for link in old_layout['links']:
                if link['id'] in current and all(link.get(k)==current[link['id']].get(k) for k in ['points','width','player_access']):selected.append(link)
                else:skipped[site].append(link['id'])
            layout=dict(old_layout,links=selected)
        for folder,contract,mesh,physics in [(base,b,before_mesh,before_physics),(new,n,'furnished.glb','furnished-physics.hcs2')]:
            assert hashlib.sha256((folder/mesh).read_bytes()).hexdigest()==contract['source_glb_sha256']
            assert hashlib.sha256((folder/physics).read_bytes()).hexdigest()==contract['physics_sha256']
        hashes[site]=dict(before=b['physics_sha256'],after=n['physics_sha256'])
        for phase,contract in [('before',b),('after',n)]:
            for name in contract['stair_components']:names.append(f'{site}:{phase}\t{name}')
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
                        rows.append('\t'.join(str(v) for v in [site,base/before_physics,b['physics_sha256'],new/'furnished-physics.hcs2',n['physics_sha256'],link['id']+':'+str(segment),x,p[1],z,lane]))
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
    report=dict(scope=f'{len(plans)} candidate maps; flat authored routes at centre/side lanes, with actual actor capsule and baseline-qualified movement sweeps',
        revision_ref=revision,changed_links_excluded=skipped,
        physics=hashes,kernel_jar_sha256=hashlib.sha256(jar.read_bytes()).hexdigest(),sites=summary,
        kernel_source_sha256=hashlib.sha256(kernel.read_bytes()).hexdigest(),authored_stairs=args.authored_stairs,
        grounding='Each scene uses its own measured support Y; after support remains within the original0.02m budget of before feet. Exact before/after heights recorded; body/footprint and actual sweeps unchanged.',
        limitation='Preserving previously valid paths does not accept pre-existing failures, stairs, complete navigation, gameplay or finished models.')
    (args.output/'route-regressions.json').write_bytes((json.dumps(report,indent=2)+'\n').encode())
    print({k:sum(v[k] for v in summary.values()) for k in ['samples','baseline_valid','new_block','baseline_sweep','new_sweep_block']})
    assert all(r['new_block']==0 and r['new_sweep_block']==0 for r in summary.values()),'Revision or placed source prop obstructs a previously valid unchanged native route'

if __name__=='__main__':main()
