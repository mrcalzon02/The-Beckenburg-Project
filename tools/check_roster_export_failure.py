"""Blender negative control: a rejected terrain export preserves the previous pair."""
from pathlib import Path
import argparse,hashlib,importlib.util,json,subprocess,sys

ROOT=Path(__file__).resolve().parents[1]
def main():
    parser=argparse.ArgumentParser();parser.add_argument('--legacy-ref');args=parser.parse_args(sys.argv[sys.argv.index('--')+1:] if '--' in sys.argv else [])
    work=ROOT/'build/roster-export-failure/wld-07';work.mkdir(parents=True,exist_ok=True)
    previous=ROOT/'development/levels/rebuild/structures/wld-07'
    for name in ['structure-physics.hcs2','physics-contract.json']:(work/name).write_bytes((previous/name).read_bytes())
    # This actual earlier export lacks the now-required saved editor terrain.
    (work/'rough-shell.glb').write_bytes(subprocess.check_output(['git','show','66942b34382507f77abbafac8e15ec9a9c2d2a53:development/levels/rebuild/structures/wld-07/rough-shell.glb'],cwd=ROOT))
    before={name:hashlib.sha256((work/name).read_bytes()).hexdigest() for name in ['structure-physics.hcs2','physics-contract.json']}
    exporter=ROOT/'tools/export_roster_collision.py'
    if args.legacy_ref:
        exporter=work.parent/'legacy-exporter.py';exporter.write_bytes(subprocess.check_output(['git','show',args.legacy_ref+':tools/export_roster_collision.py'],cwd=ROOT))
    spec=importlib.util.spec_from_file_location('tested_roster_exporter',exporter);module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module);module.ROOT=ROOT
    try:module.export(work);raise AssertionError('Accepted missing saved terrain')
    except AssertionError as error:
        assert 'GLB terrain differs from saved editor physical triangles/flags' in str(error),str(error)
    after={name:hashlib.sha256((work/name).read_bytes()).hexdigest() for name in before}
    result=dict(expected_terrain_rejection=True,prior_pair_preserved=before==after,before=before,after=after,exporter_sha256=hashlib.sha256(exporter.read_bytes()).hexdigest(),legacy_ref=args.legacy_ref)
    (work.parent/('legacy-result.json' if args.legacy_ref else 'current-result.json')).write_bytes((json.dumps(result,indent=2)+'\n').encode())
    assert before==after,'Rejected export overwrote the prior native/contract pair'
    print('PASS rejected terrain export leaves both prior artifact bytes unchanged')

if __name__=='__main__':main()
