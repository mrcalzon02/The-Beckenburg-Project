"""Headless opt-in native stair movement audit; does not unlock candidate maps.

Exact saved component identities come from the hash-bound physics contracts.
Compare default and explicit stair movement through complete authored flights.
"""
from pathlib import Path
import argparse,csv,hashlib,json,math,os,subprocess

ROOT=Path(__file__).resolve().parents[1]
def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def main():
    parser=argparse.ArgumentParser();parser.add_argument('--output',type=Path,required=True);args=parser.parse_args()
    build=ROOT/'build/roster-stair-probe';build.mkdir(parents=True,exist_ok=True)
    sites=json.loads((ROOT/'development/levels/rebuild/furnishings/room-programs.json').read_text(encoding='utf-8'))['sites']
    rows=[];paths=[];names=[];bindings={};chain_bindings=[]
    for site in sites:
        base=ROOT/f'development/levels/rebuild/structures/{site.lower()}'
        implementation=json.loads((base/'implementation.json').read_text(encoding='utf-8'))
        layout_path=ROOT/implementation['authoritative_input'];layout=json.loads(layout_path.read_text(encoding='utf-8'))
        bindings[site]={'layout':sha(layout_path),'variants':{}}
        for variant,folder,mesh,physics in [('structure',base,'rough-shell.glb','structure-physics.hcs2'),('furnished',ROOT/f'development/levels/rebuild/furnishings/{site.lower()}','furnished.glb','furnished-physics.hcs2')]:
            contract_path=folder/'physics-contract.json';contract=json.loads(contract_path.read_text(encoding='utf-8'))
            assert sha(folder/mesh)==contract['source_glb_sha256'] and sha(folder/physics)==contract['physics_sha256'],(site,variant)
            bindings[site]['variants'][variant]={'contract':sha(contract_path),'physics':sha(folder/physics),'glb':sha(folder/mesh)}
            for name in contract['stair_components']:
                assert '\t' not in name and '\n' not in name
                names.append(f'{site}:{variant}\t{name}')
            for link in layout['links']:
                if link.get('player_access')=='SEALED_NO_ROUTE':continue
                vertical=any(abs(p[1]-q[1])>=.01 for p,q in zip(link['points'],link['points'][1:]))
                for i,(p,q) in enumerate(zip(link['points'],link['points'][1:])):
                    row='\t'.join(map(str,[site,variant,folder/physics,contract['physics_sha256'],link['id'],i,*p,*q]))
                    if vertical:paths.append(row)
                    if abs(p[1]-q[1])<.01:continue
                    assert math.hypot(q[0]-p[0],q[2]-p[2])>.01,(site,link['id'])
                    rows.append(row)
            # Join only unique, exact authored endpoints. No inferred connector
            # or reset at a floor boundary: carry the actual feet position from
            # one full source link into the next.
            active=[]
            for link in layout['links']:
                if link.get('player_access')=='SEALED_NO_ROUTE' or not any(abs(p[1]-q[1])>=.01 for p,q in zip(link['points'],link['points'][1:])):continue
                points=link['points'] if link['points'][-1][1]>link['points'][0][1] else list(reversed(link['points']))
                active.append((link['id'],points))
            ends={tuple(points[-1]) for _,points in active}
            for identity,points in active:
                if tuple(points[0]) in ends:continue
                used=[identity];joined=list(points)
                while True:
                    following=[(key,p) for key,p in active if key not in used and p[0]==joined[-1]]
                    if len(following)!=1:break
                    key,p=following[0];used.append(key);joined.extend(p[1:])
                if len(used)<2:continue
                chain='CHAIN/'+'/'.join(used);chain_bindings.append(dict(site=site,variant=variant,id=chain,links=used))
                for i,(p,q) in enumerate(zip(joined,joined[1:])):
                    paths.append('\t'.join(map(str,[site,variant,folder/physics,contract['physics_sha256'],chain,i,*p,*q])))
    for filename,data in [('names.tsv',names),('flights.tsv',rows),('full-paths.tsv',paths)]:
        (build/filename).write_bytes(('\n'.join(data)+'\n').encode())
    java=Path(os.environ.get('JAVA_HOME','C:/Program Files/Java/jdk-17'))/'bin';jar=ROOT/'build/beckenburg-game.jar'
    sources=['src/main/java/org/beckenburg/AuthoredSitePhysics.java','tools/java/RosterStairMovementProbe.java','tools/java/RosterFailureContacts.java']
    subprocess.run([str(java/'javac.exe'),'-cp',str(jar),'-d',str(build),*[str(ROOT/s) for s in sources]],check=True)
    subprocess.run([str(java/'java.exe'),'-Djava.awt.headless=true','-cp',str(build)+os.pathsep+str(jar),'RosterStairMovementProbe',str(build/'names.tsv'),str(build/'flights.tsv'),str(build/'results.tsv'),str(build/'chains.tsv'),str(build/'full-paths.tsv')],check=True)
    subprocess.run([str(java/'java.exe'),'-Djava.awt.headless=true','-cp',str(build)+os.pathsep+str(jar),'RosterFailureContacts',str(build/'names.tsv'),str(build/'flights.tsv'),str(build/'failure-contacts.tsv')],check=True)
    result=list(csv.DictReader((build/'results.tsv').read_text(encoding='utf-8').splitlines(),delimiter='\t'))
    summary={}
    for variant in ['structure','furnished']:
        summary[variant]={}
        for site in sites:
            selected=[r for r in result if r['site']==site and r['variant']==variant]
            summary[variant][site]={'flights':len(selected),**{key:sum(r[key]=='PASS' for r in selected) for key in ['legacy_up','legacy_down','authored_up','authored_down']}}
    report={'status':'CANDIDATE_STAIR_MOVEMENT_AUDIT_NOT_MAP_ACCEPTANCE','bindings':bindings,'source_hashes':{s:sha(ROOT/s) for s in sources+['tools/audit_roster_stairs.py']},'dependency_jar_sha256':sha(jar),'sites':summary,'limitation':'Explicit stair sets are not yet connected to playable level ownership or replication. Failed complete flights remain blockers; centre support is not complete movement acceptance.'}
    chains=list(csv.DictReader((build/'chains.tsv').read_text(encoding='utf-8').splitlines(),delimiter='\t'))
    report['complete_routes']={v:{'routes':len([r for r in chains if r['variant']==v and not r['link'].startswith('CHAIN/')]),**{k:sum(r[k]=='PASS' for r in chains if r['variant']==v and not r['link'].startswith('CHAIN/')) for k in ['legacy_forward','legacy_reverse','authored_forward','authored_reverse']}} for v in ['structure','furnished']}
    report['cross_link_chains']={'bindings':chain_bindings,'results':[r for r in chains if r['link'].startswith('CHAIN/')],'scope':'Maximal unique exact source-endpoint chains; ascending normalization, no invented inter-room segment. Other links retain separate complete-path checks.'}
    report['chain_scope']='Every active vertically changing link, with all intermediate flat landing segments and forward/reverse continuous position; no reinitialization at each segment. This does not cover the separate flat-only route network.'
    args.output.mkdir(parents=True,exist_ok=True)
    (args.output/'flight-sweeps.tsv').write_bytes((build/'results.tsv').read_bytes().replace(b'\r\n',b'\n'))
    (args.output/'complete-route-sweeps.tsv').write_bytes((build/'chains.tsv').read_bytes().replace(b'\r\n',b'\n'))
    (args.output/'failure-contacts.tsv').write_bytes((build/'failure-contacts.tsv').read_bytes().replace(b'\r\n',b'\n'))
    (args.output/'stair-movement.json').write_bytes((json.dumps(report,indent=2)+'\n').encode())
    print({v:{k:sum(s[k] for s in per.values()) for k in ['flights','legacy_up','legacy_down','authored_up','authored_down']} for v,per in summary.items()})
    print(report['complete_routes'])

if __name__=='__main__':main()
