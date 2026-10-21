"""Rebuild, planning-check and commit one completed map without staging other work."""
import argparse
import json
from pathlib import Path
import subprocess
import sys

ROOT=Path(__file__).resolve().parent
REPO=ROOT.parents[2]

def run(*args):subprocess.run(args,cwd=REPO,check=True)

def main():
    ap=argparse.ArgumentParser();ap.add_argument('slug');args=ap.parse_args()
    folder=ROOT/args.slug
    if not (folder/'layout.json').exists():raise SystemExit('Author the map JSON before checkpointing it.')
    data=json.loads((folder/'layout.json').read_text(encoding='utf-8'))
    run(sys.executable,str(ROOT/'build_review.py'))
    run(sys.executable,str(ROOT/'validate_layouts.py'),'--partial')
    log=ROOT/'WORKFLOW.md';old=log.read_text(encoding='utf-8')
    marker=f'- **{data["id"]} /'
    if marker not in old:
        log.write_text(old+f'{marker} {data["title"]}**: {len(data["zones"])} space reservations authored. Planning validation passed; runtime geometry and physical access remain pending.\n',encoding='utf-8')
    # Explicit allowlist is important: scene/library/lighting work may be in progress.
    files=['README.md','build_review.py','validate_layouts.py','checkpoint_layout.py','WORKFLOW.md','REVIEW.html','catalog.json','verification.json']
    run('git','add','--',str(folder),*(str(ROOT/f) for f in files))
    run('git','commit','-m',f'Plan rough layout for {data["title"]}')

if __name__=='__main__':main()
