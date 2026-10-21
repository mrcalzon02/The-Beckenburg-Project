"""Package only the reviewed planning deliverables; no unrelated project files."""
from pathlib import Path
import hashlib
import json
import zipfile

ROOT=Path(__file__).resolve().parent
def main():
    files=[ROOT/name for name in ('README.md','MAP_LIST.md','REUSE_BUILD_ORDER.md','WORKFLOW.md','REVIEW.html','ATLAS.png','catalog.json','verification.json','build_review.py','validate_layouts.py','render_previews.cjs','checkpoint_layout.py','package_atlas.py')]
    maps=json.loads((ROOT/'catalog.json').read_text(encoding='utf-8'))['maps']
    assert len(maps)==23
    for m in maps:
        files += [ROOT/m['folder']/n for n in ('README.md','layout.json','plan.svg','plan.png','blockout.obj')]
    assert all(f.is_file() for f in files)
    manifest={'version':1,'date':'2026-10-21','status':'rough-layout-planning-only','map_count':23,'files':[{'path':f.relative_to(ROOT).as_posix(),'bytes':f.stat().st_size,'sha256':hashlib.sha256(f.read_bytes()).hexdigest()} for f in files]}
    mp=ROOT/'package-manifest.json';mp.write_text(json.dumps(manifest,indent=2)+'\n',encoding='utf-8');files.append(mp)
    target=ROOT/'map-layout-atlas.zip'
    with zipfile.ZipFile(target,'w',zipfile.ZIP_DEFLATED) as archive:
        for f in files:archive.write(f,f.relative_to(ROOT).as_posix())
    with zipfile.ZipFile(target) as archive:
        assert archive.testzip() is None
        for f in manifest['files']:assert hashlib.sha256(archive.read(f['path'])).hexdigest()==f['sha256']
    print(f'Packaged and hash-verified {len(files)} files for all 23 layouts.')

if __name__=='__main__':main()
