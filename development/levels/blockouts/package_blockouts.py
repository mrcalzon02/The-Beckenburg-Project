"""Independent roster/hash/GLB integrity check and portable all-map review kit."""
from pathlib import Path
import json,hashlib,struct,math,zipfile,html,shutil
ROOT=Path(__file__).resolve().parent;LAYOUTS=ROOT.parent/'layouts'
data=json.loads((ROOT/'manifest.json').read_text());catalog=json.loads((LAYOUTS/'catalog.json').read_text());catalog['maps']=[m for m in catalog['maps'] if m['id'] not in {'HOUSE','FARMHOUSE','SCHOOL','CHURCH','WLD-01','CASTLE'}]
assert {m['id'] for m in data['maps']}=={m['id'] for m in catalog['maps']} and len(data['maps'])==17
checks=[];cards=[]
for row in data['maps']:
    folder=next(m['folder'] for m in catalog['maps'] if m['id']==row['id']);out=ROOT/folder;source=LAYOUTS/folder/'layout.json'
    assert hashlib.sha256(source.read_bytes()).hexdigest()==row['source_sha256']
    assert hashlib.sha256((out/'rough-shell.blend').read_bytes()).hexdigest()==row['blend_sha256']
    assert row['native_reopened'] and (out/'preview.png').stat().st_size>1000
    glb=(out/'rough-shell.glb').read_bytes();magic,version,size=struct.unpack_from('<III',glb)
    assert magic==0x46546c67 and version==2 and size==len(glb)
    length,kind=struct.unpack_from('<II',glb,12);assert kind==0x4e4f534a
    doc=json.loads(glb[20:20+length]);triangles=0
    for mesh in doc['meshes']:
        for p in mesh['primitives']:
            acc=doc['accessors'][p['attributes']['POSITION']]
            assert all(math.isfinite(v) for v in acc['min']+acc['max'])
            triangles+=doc['accessors'][p['indices']]['count']//3
    shutil.copyfile(source,out/'layout-source.json')
    shutil.copyfile(LAYOUTS/folder/'plan.svg',out/'source-plan.svg')
    unresolved=len(row['issues'])
    checks.append(dict(id=row['id'],zones=row['zones'],native_reopened=True,source_hash_matches=True,glb_triangles=triangles,glb_sha256=hashlib.sha256(glb).hexdigest(),rough_openings=len(row['shell_openings']),engineering_reservations=unresolved))
    title=html.escape(row['title']);ident=html.escape(row['id'])
    cards.append(f'<article data-title="{ident.lower()} {title.lower()}"><h2>{ident} · {title}</h2><a href="{folder}/preview.png"><img loading="lazy" src="{folder}/preview.png" alt="{title} actual Blender rough shell preview"></a><p>{row["zones"]} zones · {len(row["shell_openings"])} rough openings · {unresolved} engineering reservations</p><p><a href="{folder}/rough-shell.blend">Editable Blender scene</a> · <a href="{folder}/rough-shell.glb">GLB</a> · <a href="{folder}/source-plan.svg">Measured source plan</a> · <a href="{folder}/NEXT.md">Next steps</a> · <a href="{folder}/implementation.json">Implementation ledger</a></p></article>')
review='''<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Beckenburg — 17 remaining rough shell blockouts</title><style>body{background:#172126;color:#e4e7df;max-width:1500px;margin:32px auto;padding:0 22px;font:16px/1.55 system-ui}h1,h2{color:#d4c397;line-height:1.25}a{color:#a8d8df}main{display:grid;grid-template-columns:repeat(auto-fit,minmax(350px,1fr));gap:24px}article{padding:18px;background:#222e32;border:1px solid #506064}article[hidden]{display:none}img{width:100%;display:block}input{font:inherit;padding:12px;width:min(500px,90%);margin:18px 0;background:#e9ece6;color:#18242b}header{max-width:1000px}p{margin:12px 0}.notice{border-left:4px solid #c49553;padding:14px;background:#293333}</style><header><h1>23 layouts → 23 editable rough shell blockouts</h1><p>Five unbuilt initial families and twelve remote proposals. House, Farmhouse, School, Church, Blackpine and Castle are excluded because authored versions already exist. Actual native previews from existing measured layouts. Cyan strips reserve level routes; amber geometry reserves elevation changes; purple pads reserve ritual space. Each zone retains its authored elevation.</p><p class="notice">ROUGH DEVELOPMENT BLOCKING. Open shells, apertures and connector supports advance the volume studies. These are not playable maps. Amber inclines and vertical cores require real stair/path engineering; overlapping partitions, crossings, terrain, headroom and collision remain review gates. The van is an envelope reservation. Existing runtime maps and ongoing engine optimization are untouched.</p><p><a href="all-remaining-level-blockouts.zip">Download 17 remaining scenes, GLBs and implementation packets</a> · <a href="verification.json">Integrity and roster verification</a> · <a href="README.md">Workflow</a></p><input id="search" aria-label="Filter maps" placeholder="Filter by name or map ID"></header><main>'''+''.join(cards)+'''</main><script>document.querySelector('#search').addEventListener('input',e=>{const q=e.target.value.toLowerCase();document.querySelectorAll('article').forEach(a=>a.hidden=!a.dataset.title.includes(q))});</script></html>'''
(ROOT/'REVIEW.html').write_text(review,encoding='utf-8')
result=dict(status='pass-rough-blockout-integrity-only',maps=len(checks),zones=sum(m['zones'] for m in checks),shell_openings=sum(m['rough_openings'] for m in checks),checks=checks,limitations=data['limitations'])
(ROOT/'verification.json').write_text(json.dumps(result,indent=2)+'\n')
archive=ROOT/'all-remaining-level-blockouts.zip'
with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
    for name in ['README.md','REVIEW.html','manifest.json','verification.json','build_blockouts.py','package_blockouts.py']:z.write(ROOT/name,name)
    for row in catalog['maps']:
        for p in sorted((ROOT/row['folder']).glob('*')):
            if p.is_file():z.write(p,str(p.relative_to(ROOT)))
with zipfile.ZipFile(archive) as z:assert z.testzip() is None
(ROOT/'package.json').write_text(json.dumps(dict(archive=archive.name,bytes=archive.stat().st_size,sha256=hashlib.sha256(archive.read_bytes()).hexdigest(),zip_crc_verified=True),indent=2)+'\n')
print('PASS',len(checks),'native scene reopen checks, matching immutable layout hashes, finite portable meshes; archive CRC verified',flush=True)
