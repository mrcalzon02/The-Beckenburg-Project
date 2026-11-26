"""Blender driver for measured structure passes, preserving existing native scenes.

Run Blender --background --factory-startup --python tools/build_roster_structures.py -- --sites FARMHOUSE
Then inspect the pilot before --remaining. Every output is intermediate blocking.
"""
from pathlib import Path
import argparse,hashlib,json,sys

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'development/levels/blockouts'))
import build_blockouts

def main():
    parser=argparse.ArgumentParser()
    selection=parser.add_mutually_exclusive_group(required=True)
    selection.add_argument('--sites',nargs='+')
    selection.add_argument('--remaining',action='store_true')
    parser.add_argument('--replace',action='store_true')
    args=parser.parse_args(sys.argv[sys.argv.index('--')+1:] if '--' in sys.argv else [])
    roster=json.loads((ROOT/'development/levels/rebuild/stage-register.json').read_text(encoding='utf-8'))['sites']
    retained={'CASTLE','HOUSE','WLD-01','WLD-02'}
    ids={s['id'] for s in roster if s['id'] not in retained} if args.remaining else set(args.sites)
    assert ids and ids<={s['id'] for s in roster},'Unknown/missing roster IDs'
    assert not(ids&retained),'Mature native scenes require individual retrofit; never regenerate them from old roughs'
    destination=ROOT/'development/levels/rebuild/structures';reports=[];failures=[]
    for site in roster:
        if site['id'] not in ids:continue
        folder=site['folder'];options=[ROOT/f'development/levels/rebuild/{folder}/rough-layout.json',
            ROOT/f'development/levels/layouts/{folder}/layout.json',ROOT/f'development/levels/layout-expansions/{folder}/layout.json']
        source=next((p for p in options if p.exists()),None)
        assert source is not None,site['id']
        preserved=[source]+[p for p in [ROOT/f'development/levels/blockouts/{folder}/rough-shell.blend',ROOT/f'development/levels/blockouts/{folder}/rough-shell.glb'] if p.exists()]
        original_hashes={p.relative_to(ROOT).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in preserved}
        output=destination/folder
        if (output/'rough-shell.blend').exists() and not args.replace:
            print('RETAIN existing structural checkpoint',site['id'],flush=True);continue
        try:
            report=build_blockouts.build(source,output_root=destination,structural=True)
        except Exception as error:
            output.mkdir(parents=True,exist_ok=True)
            failure=dict(id=site['id'],status='REVISE_FAILED_STRUCTURAL_CHECK',source=source.relative_to(ROOT).as_posix(),
                source_sha256=hashlib.sha256(source.read_bytes()).hexdigest(),error=str(error))
            (output/'failure.json').write_text(json.dumps(failure,indent=2)+'\n',encoding='utf-8')
            failures.append(failure);print('STRUCTURE_REVISE',site['id'],str(error),flush=True);continue
        (output/'failure.json').unlink(missing_ok=True)
        report['authoritative_input']=source.relative_to(ROOT).as_posix()
        report['original_scene_preserved']=True
        assert all(hashlib.sha256((ROOT/name).read_bytes()).hexdigest()==digest for name,digest in original_hashes.items()),'Original source changed during derivative build'
        report['preserved_sources']=original_hashes
        report['generator_sha256']={name:hashlib.sha256((ROOT/name).read_bytes()).hexdigest() for name in [
            'tools/build_roster_structures.py','development/levels/blockouts/build_blockouts.py','development/levels/blockouts/structural_pass.py','development/levels/blockouts/proposal_landforms.py']}
        (output/'implementation.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
        reports.append(report)
    rows=[]
    for site in roster:
        path=destination/site['folder']/'implementation.json'
        if (path.parent/'failure.json').exists():status='REVISE_FAILED_STRUCTURAL_CHECK'
        elif site['id'] in retained:status='RETAIN_MATURE_NATIVE_INDIVIDUAL_STRUCTURE_AUDIT_REQUIRED'
        elif path.exists():status='NATIVE_STRUCTURE_BLOCKING_RUNTIME_GATES_OPEN'
        else:status='STRUCTURE_PASS_PENDING'
        rows.append(dict(id=site['id'],status=status,evidence=path.relative_to(ROOT).as_posix() if path.exists() else None))
    destination.mkdir(parents=True,exist_ok=True)
    (destination/'roster.json').write_text(json.dumps(dict(scope=29,stage='BREADTH_BY_STAGE_STRUCTURAL_BLOCKING',sites=rows),indent=2)+'\n',encoding='utf-8')
    assert len(rows)==29
    print('STRUCTURE_PASS_GENERATED',len(reports),'packets; all29 decisions recorded; no playable unlock',flush=True)
    if failures:raise SystemExit('Structural checks failed for '+','.join(row['id'] for row in failures)+'; other packets preserved')

if __name__=='__main__':main()
