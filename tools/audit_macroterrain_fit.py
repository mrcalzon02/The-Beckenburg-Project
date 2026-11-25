"""Measure actual new terrain against preserved native route movement.

Findings intentionally remain failures to resolve, never terrain acceptance.
Runs current production physics headlessly and retains raw before/after probes.
"""
from pathlib import Path
import argparse,collections,csv,hashlib,json,math,os,subprocess
from roster_candidate_sources import arguments as candidate_arguments,select as candidate_select
ROOT=Path(__file__).resolve().parents[1]
BASE=ROOT/'development/levels/rebuild/terrain-overall-pass'
OUT=BASE/'verification';OUT.mkdir(exist_ok=True)
BUILD=ROOT/'build/macroterrain-fit';BUILD.mkdir(exist_ok=True)
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
    parser=argparse.ArgumentParser();parser.add_argument('--sites',nargs='+');parser.add_argument('--bearing-pass',action='store_true');parser.add_argument('--deck-pass',action='store_true');parser.add_argument('--occupancy-pass',action='store_true');parser.add_argument('--closure-pass',action='store_true');parser.add_argument('--stair-end-pass',action='store_true');parser.add_argument('--opening-pass',action='store_true');parser.add_argument('--bay-pass',action='store_true');parser.add_argument('--bench-pass',action='store_true');parser.add_argument('--connection-pass',action='store_true');parser.add_argument('--flat-pass',action='store_true');parser.add_argument('--arrival-pass',action='store_true');candidate_arguments(parser,output=True);args=parser.parse_args();args.flat_pass=args.flat_pass or args.arrival_pass;assert sum([args.bearing_pass,args.deck_pass,args.occupancy_pass,args.closure_pass,args.stair_end_pass,args.opening_pass,args.bay_pass,args.bench_pass,args.connection_pass,args.flat_pass])<=1
    output=ROOT/'development/levels/rebuild/arrival-bearing-pass/verification' if args.arrival_pass else ROOT/'development/levels/rebuild/flat-circulation-pass/verification' if args.flat_pass else ROOT/'development/levels/rebuild/stair-connection-pass/verification' if args.connection_pass else ROOT/'development/levels/rebuild/stair-terrain-pass/verification' if args.bench_pass else ROOT/'development/levels/rebuild/stair-bay-pass/verification' if args.bay_pass else ROOT/'development/levels/rebuild/stair-opening-pass/verification' if args.opening_pass else ROOT/'development/levels/rebuild/stair-landing-pass/verification' if args.stair_end_pass else ROOT/'development/levels/rebuild/floor-closure-pass/verification' if args.closure_pass else ROOT/'development/levels/rebuild/terrain-occupancy-pass/verification' if args.occupancy_pass else ROOT/'development/levels/rebuild/deck-fit-pass/verification' if args.deck_pass else ROOT/'development/levels/rebuild/terrain-fit-pass/verification' if args.bearing_pass else OUT;output.mkdir(exist_ok=True)
    if args.source_manifest:
        assert args.output;output=ROOT/args.output;output.mkdir(parents=True,exist_ok=True)
    rows=[];names=[];bindings={}
    roster={r['id']:r for r in json.loads((BASE/'terrain-envelopes.json').read_text())['sites']}
    catalogue=json.loads(((ROOT/'development/levels/rebuild/terrain-fit-pass/support-policies.json') if args.bearing_pass or args.deck_pass or args.occupancy_pass or args.closure_pass or args.stair_end_pass or args.opening_pass or args.bay_pass or args.bench_pass or args.connection_pass or args.flat_pass or args.source_manifest else (BASE/'macroterrain-specs.json')).read_text())
    if args.sites:catalogue=dict(catalogue,sites=[s for s in catalogue['sites'] if s in args.sites])
    for site in catalogue['sites']:
        old=ROOT/f'development/levels/rebuild/furnishings/{site.lower()}';new=BASE/'assemblies'/site.lower();contract=json.loads((old/'physics-contract.json').read_text())
        layout=ROOT/roster[site]['layout'];alias_file=ROOT/'development/levels/rebuild/flat-circulation-pass/verification/source-layout-aliases.json'
        if (args.flat_pass or args.source_manifest) and alias_file.exists():layout=ROOT/json.loads(alias_file.read_text())['files'].get(layout.relative_to(ROOT).as_posix(),{}).get('frozen_path',layout.relative_to(ROOT).as_posix())
        data=json.loads(layout.read_text())
        if args.source_manifest:
            old_source,new_source,selected,desired=candidate_select(args,site);before=old_source[0]/old_source[2];after=new_source[0]/new_source[2]
        elif args.arrival_pass:
            before=ROOT/f'development/levels/rebuild/flat-circulation-pass/{site.lower()}/open-circulation.hcs2';candidate=ROOT/f'development/levels/rebuild/arrival-bearing-pass/{site.lower()}/supported-arrival.hcs2';after=candidate if candidate.exists() else before
        elif args.flat_pass:
            options=[ROOT/f'development/levels/rebuild/stair-connection-pass/{site.lower()}/connected-stairs.hcs2',ROOT/f'development/levels/rebuild/stair-terrain-pass/assemblies/{site.lower()}/benched-terrain.hcs2',ROOT/f'development/levels/rebuild/stair-bay-pass/{site.lower()}/bayed-stairs.hcs2',ROOT/f'development/levels/rebuild/stair-opening-pass/{site.lower()}/opened-stairs.hcs2',ROOT/f'development/levels/rebuild/stair-landing-pass/{site.lower()}/supported-stairs.hcs2',ROOT/f'development/levels/rebuild/floor-closure-pass/{site.lower()}/closed-floors.hcs2'];before=next(p for p in options if p.exists());candidate=ROOT/f'development/levels/rebuild/flat-circulation-pass/{site.lower()}/open-circulation.hcs2';after=candidate if candidate.exists() else before
        elif args.connection_pass:
            options=[ROOT/f'development/levels/rebuild/stair-terrain-pass/assemblies/{site.lower()}/benched-terrain.hcs2',ROOT/f'development/levels/rebuild/stair-bay-pass/{site.lower()}/bayed-stairs.hcs2',ROOT/f'development/levels/rebuild/stair-opening-pass/{site.lower()}/opened-stairs.hcs2',ROOT/f'development/levels/rebuild/stair-landing-pass/{site.lower()}/supported-stairs.hcs2',ROOT/f'development/levels/rebuild/floor-closure-pass/{site.lower()}/closed-floors.hcs2'];before=next(p for p in options if p.exists());candidate=ROOT/f'development/levels/rebuild/stair-connection-pass/{site.lower()}/connected-stairs.hcs2';after=candidate if candidate.exists() else before
        elif args.bench_pass:
            options=[ROOT/f'development/levels/rebuild/stair-bay-pass/{site.lower()}/bayed-stairs.hcs2',ROOT/f'development/levels/rebuild/stair-opening-pass/{site.lower()}/opened-stairs.hcs2',ROOT/f'development/levels/rebuild/stair-landing-pass/{site.lower()}/supported-stairs.hcs2',ROOT/f'development/levels/rebuild/floor-closure-pass/{site.lower()}/closed-floors.hcs2'];before=next(p for p in options if p.exists());candidate=ROOT/f'development/levels/rebuild/stair-terrain-pass/assemblies/{site.lower()}/benched-terrain.hcs2';after=candidate if candidate.exists() else before
        elif args.bay_pass:
            options=[ROOT/f'development/levels/rebuild/stair-opening-pass/{site.lower()}/opened-stairs.hcs2',ROOT/f'development/levels/rebuild/stair-landing-pass/{site.lower()}/supported-stairs.hcs2',ROOT/f'development/levels/rebuild/floor-closure-pass/{site.lower()}/closed-floors.hcs2'];before=next(p for p in options if p.exists());candidate=ROOT/f'development/levels/rebuild/stair-bay-pass/{site.lower()}/bayed-stairs.hcs2';after=candidate if candidate.exists() else before
        elif args.opening_pass:
            before=ROOT/f'development/levels/rebuild/stair-landing-pass/{site.lower()}/supported-stairs.hcs2'
            if not before.exists():before=ROOT/f'development/levels/rebuild/floor-closure-pass/{site.lower()}/closed-floors.hcs2'
            candidate=ROOT/f'development/levels/rebuild/stair-opening-pass/{site.lower()}/opened-stairs.hcs2';after=candidate if candidate.exists() else before
        elif args.stair_end_pass:
            before=ROOT/f'development/levels/rebuild/floor-closure-pass/{site.lower()}/closed-floors.hcs2';candidate=ROOT/f'development/levels/rebuild/stair-landing-pass/{site.lower()}/supported-stairs.hcs2';after=candidate if candidate.exists() else before
        elif args.closure_pass:
            options=[ROOT/f'development/levels/rebuild/terrain-occupancy-pass/assemblies/{site.lower()}/occupied-terrain.hcs2',ROOT/f'development/levels/rebuild/deck-fit-pass/{site.lower()}/deck-terrain.hcs2',ROOT/f'development/levels/rebuild/terrain-fit-pass/{site.lower()}/supported-terrain.hcs2'];before=next(p for p in options if p.exists());candidate=ROOT/f'development/levels/rebuild/floor-closure-pass/{site.lower()}/closed-floors.hcs2';after=candidate if candidate.exists() else before
        elif args.occupancy_pass:
            before=ROOT/f'development/levels/rebuild/deck-fit-pass/{site.lower()}/deck-terrain.hcs2'
            if not before.exists():before=ROOT/f'development/levels/rebuild/terrain-fit-pass/{site.lower()}/supported-terrain.hcs2'
            candidate=ROOT/f'development/levels/rebuild/terrain-occupancy-pass/assemblies/{site.lower()}/occupied-terrain.hcs2';after=candidate if candidate.exists() else before
        elif args.deck_pass:
            before=ROOT/f'development/levels/rebuild/terrain-fit-pass/{site.lower()}/supported-terrain.hcs2';candidate=ROOT/f'development/levels/rebuild/deck-fit-pass/{site.lower()}/deck-terrain.hcs2';after=candidate if candidate.exists() else before
        elif args.bearing_pass:
            new=ROOT/f'development/levels/rebuild/terrain-fit-pass/{site.lower()}';review=json.loads((new/'construction.json').read_text());before=ROOT/review['source_physics'];after=new/'supported-terrain.hcs2';assert sha(before)==review['source_physics_sha256'] and sha(after)==review['physics_sha256']
        else:
            review=json.loads((new/'review.json').read_text());before=old/'furnished-physics.hcs2';after=new/'terrain-assembly.hcs2';assert sha(before)==contract['physics_sha256'] and sha(after)==review['native_physics_sha256']
        if args.source_manifest:
            old_source,new_source,selected,desired=candidate_select(args,site);before=old_source[0]/old_source[2];after=new_source[0]/new_source[2]
        before_hash=sha(before);after_hash=sha(after)
        bindings[site]=dict(before=before_hash,after=after_hash,layout_sha256=sha(layout))
        for phase in ['before','after']:
            components=contract['stair_components']
            if args.stair_end_pass and phase=='after' and candidate.exists():components=json.loads(candidate.with_name('construction.json').read_text())['stair_components']
            if args.opening_pass or args.bay_pass or args.bench_pass or args.connection_pass or args.flat_pass or args.source_manifest:
                meta=json.loads((before if phase=='before' else after).with_name('construction.json').read_text());components=meta.get('stair_components',components)
            names.extend(f'{site}:{phase}\t{name}' for name in components)
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
    raw=results.read_bytes().replace(b'\r\n',b'\n');(output/'native-route-probes.tsv').write_bytes(raw)
    probes=list(csv.DictReader(raw.decode().splitlines(),delimiter='\t'));summary={}
    for site in catalogue['sites']:
        selected=[r for r in probes if r['site']==site];failed=collections.Counter(r['link'] for r in selected if r['new_block']=='true' or r['new_sweep_block']=='true')
        summary[site]=dict(samples=len(selected),**{key:sum(r[key]=='true' for r in selected) for key in ['baseline_valid','new_block','baseline_sweep','new_sweep_block','after_valid','recovered_pose']},affected_segments=dict(failed))
    report=dict(status='MEASURED_FIT_FINDINGS_NOT_TERRAIN_ACCEPTANCE',sites=summary,source_bindings=bindings,kernel_sha256=sha(kernel),probe_sha256=sha(probe),auditor_sha256=sha(Path(__file__)),scope=f'All{len(summary)} '+('explicit exact-manifest candidates compared to their current source geometry' if args.source_manifest else 'small attached arrival-end bearings compared to exact current flat sources' if args.arrival_pass else 'physical flat circulation apertures and indoor bearing thresholds compared to latest completed vertical-source candidates' if args.flat_pass else 'physical level-entry/convergent stair hubs and flat approaches compared to latest saved terrain/bay/shell sources' if args.connection_pass else 'actual saved editor stair-bed layers and basement punches compared to latest bay/shell candidates' if args.bench_pass else 'authored shared/separate stair bays compared to latest physical shell candidates' if args.bay_pass else 'physical structural stair openings compared to latest landing/floor candidates' if args.opening_pass else 'authored stair-end supports/corner trims compared to latest floor/cap candidates; unchanged sites retained' if args.stair_end_pass else 'floor/cap restorations compared to latest occupied-room/deck/bearing sources; unchanged sites retained' if args.closure_pass else 'occupied-room terrain excavations compared to latest bearing/deck sources; unchanged sites retained' if args.occupancy_pass else 'physical deck candidates compared to previous bearing construction, unchanged sites retained' if args.deck_pass else 'bearing construction candidates compared to exact previous terrain composite/furnished scene' if args.bearing_pass else 'new macroterrain composites')+'; unchanged authored flat centre/side routes with production capsules and qualified sweeps. Vertical routes, moved blockouts, terrain edges and runtime remain open.',next_action='Revise blockout/terrain relationships at actual failed contacts. Original route coordinates are review inputs, not fixed constraints. Do not bury failures by increasing tolerances or flattening land around reservation pads.')
    (output/'native-route-fit.json').write_bytes((json.dumps(report,indent=2)+'\n').encode())
    print(json.dumps({site:{key:v for key,v in row.items() if key!='affected_segments'} for site,row in summary.items()},indent=2))
    if args.opening_pass or args.bay_pass or args.bench_pass or args.connection_pass or args.flat_pass or args.source_manifest:
        assert all(row['new_block']==0 and row['new_sweep_block']==0 for row in summary.values()),'Structural apertures regressed an existing flat route; inspect retained raw probes'
if __name__=='__main__':main()
