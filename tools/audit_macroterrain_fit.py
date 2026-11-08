"""Measure actual new terrain against preserved native route movement.

Findings intentionally remain failures to resolve, never terrain acceptance.
Runs current production physics headlessly and retains raw before/after probes.
"""
from pathlib import Path
import collections,csv,hashlib,json,math,os,subprocess
ROOT=Path(__file__).resolve().parents[1]
BASE=ROOT/'development/levels/rebuild/terrain-overall-pass'
OUT=BASE/'verification';OUT.mkdir(exist_ok=True)
BUILD=ROOT/'build/macroterrain-fit';BUILD.mkdir(exist_ok=True)
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
    rows=[];names=[];bindings={}
    roster={r['id']:r for r in json.loads((BASE/'terrain-envelopes.json').read_text())['sites']}
    catalogue=json.loads((BASE/'macroterrain-specs.json').read_text())
    for site in catalogue['sites']:
        old=ROOT/f'development/levels/rebuild/furnishings/{site.lower()}';new=BASE/'assemblies'/site.lower();review=json.loads((new/'review.json').read_text());contract=json.loads((old/'physics-contract.json').read_text())
        layout=ROOT/roster[site]['layout'];data=json.loads(layout.read_text());before=old/'furnished-physics.hcs2';after=new/'terrain-assembly.hcs2'
        assert sha(before)==contract['physics_sha256'] and sha(after)==review['native_physics_sha256']
        before_hash=sha(before);after_hash=sha(after)
        bindings[site]=dict(before=before_hash,after=after_hash,layout_sha256=sha(layout))
        for phase in ['before','after']:
            names.extend(f'{site}:{phase}\t{name}' for name in contract['stair_components'])
        for link in data['links']:
            if link.get('player_access')=='SEALED_NO_ROUTE':continue
            for index,(p,q) in enumerate(zip(link['points'],link['points'][1:])):
                run=math.hypot(q[0]-p[0],q[2]-p[2])
                if abs(p[1]-q[1])>.01 or run<.01:continue
                side=max(0,link['width']/2-.27)
                for lane,offset in [('centre',0),('left',side),('right',-side)]:
                    n=math.ceil(run/.25)
                    for i in range(n+1):
                        x=p[0]+(q[0]-p[0])*i/n-(q[2]-p[2])/run*offset;z=p[2]+(q[2]-p[2])*i/n+(q[0]-p[0])/run*offset
                        rows.append('\t'.join(map(str,[site,before,before_hash,after,after_hash,link['id']+':'+str(index),x,p[1],z,lane])))
    source=BUILD/'input.tsv';source.write_bytes(('\n'.join(rows)+'\n').encode());stairs=BUILD/'stairs.tsv';stairs.write_bytes(('\n'.join(names)+'\n').encode())
    java=Path('C:/Program Files/Java/jdk-17/bin');jar=ROOT/'build/beckenburg-game.jar'
    kernel=ROOT/'src/main/java/org/beckenburg/AuthoredSitePhysics.java';probe=ROOT/'tools/java/FurnishingRouteProbe.java'
    subprocess.run([str(java/'javac.exe'),'-cp',str(jar),'-d',str(BUILD),str(kernel),str(probe)],check=True)
    results=BUILD/'results.tsv';subprocess.run([str(java/'java.exe'),'-Djava.awt.headless=true','-Xmx2g','-cp',str(BUILD)+os.pathsep+str(jar),'FurnishingRouteProbe',str(source),str(results),str(stairs)],check=True)
    raw=results.read_bytes().replace(b'\r\n',b'\n');(OUT/'native-route-probes.tsv').write_bytes(raw)
    probes=list(csv.DictReader(raw.decode().splitlines(),delimiter='\t'));summary={}
    for site in catalogue['sites']:
        selected=[r for r in probes if r['site']==site];failed=collections.Counter(r['link'] for r in selected if r['new_block']=='true' or r['new_sweep_block']=='true')
        summary[site]=dict(samples=len(selected),**{key:sum(r[key]=='true' for r in selected) for key in ['baseline_valid','new_block','baseline_sweep','new_sweep_block']},affected_segments=dict(failed))
    report=dict(status='MEASURED_FIT_FINDINGS_NOT_TERRAIN_ACCEPTANCE',sites=summary,source_bindings=bindings,kernel_sha256=sha(kernel),probe_sha256=sha(probe),auditor_sha256=sha(Path(__file__)),scope='All18new macroterrain composites; unchanged authored flat centre/side routes with production capsules and qualified sweeps. Vertical routes, moved blockouts, foundations, terrain edges and runtime remain open.',next_action='Revise blockout/terrain relationships at actual failed contacts. Original route coordinates are review inputs, not fixed constraints. Do not bury failures by increasing tolerances or flattening land around reservation pads.')
    (OUT/'native-route-fit.json').write_bytes((json.dumps(report,indent=2)+'\n').encode())
    print(json.dumps({site:{key:v for key,v in row.items() if key!='affected_segments'} for site,row in summary.items()},indent=2))
if __name__=='__main__':main()
