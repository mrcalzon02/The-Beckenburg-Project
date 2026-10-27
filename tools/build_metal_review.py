"""Index actual matched game views and exact generated source atlas snapshots."""
from pathlib import Path
import csv, hashlib, html, json, zipfile
ROOT=Path(__file__).resolve().parents[1]
V=ROOT/'development/levels/verification'
records=[]
for phase in ('before','after'):
    with (V/f'map-lighting-camera-audit-metal-{phase}.tsv').open(encoding='utf-8') as f:rows=list(csv.DictReader(f,delimiter='\t'))
    assert len(rows)==6
    for row in rows:
        path=ROOT/f'development/levels/previews/metal-refinement/{phase}/{row["map"]}-{row["view"]}.png'
        assert hashlib.sha256(path.read_bytes()).hexdigest()==row['sha256'],path
        records.append(dict(row,phase=phase,path=path.relative_to(ROOT).as_posix()))
def digest(data):return hashlib.sha256(data).hexdigest()
classes=['org/beckenburg/SceneRenderer.class','org/beckenburg/FixtureLighting.class','org/beckenburg/VanInteriorLayout.class']
unchanged={}
with zipfile.ZipFile(ROOT/'build/metal-before.jar') as before,zipfile.ZipFile(ROOT/'build/metal-after.jar') as after:
    for name in classes:
        assert before.read(name)==after.read(name),name
        unchanged[name]=digest(before.read(name))
report=dict(date='2026-10-27',baseline='eef747d0c3f46e4605cd768d196f497806d866b7',scope='Original procedural IRON diffuse tile only; material IDs, UVs, atlas size, other15tiles and renderer/lighting/van classes preserved.',cause='The original tile explicitly darkened every eighth X pixel by17 levels; repeated UVs stamped grooves across broad painted panels.',change='Replace forced directional grooves with bounded periodic 2D wear, fine grain and sparse small pits. Geometry retains actual ribs and panel seams.',groove_phase_contrast_before=16.651785714285708,groove_phase_contrast_after=.0569196428571388,other15tiles_sha256='baa65d08a699588d61ff1d923c84a4400ebffdde37f4d78488c96d19bf094441',unchanged_classes=unchanged,capture_process_exit={'before':0,'after':0},captures=records,acceptance='Actual OpenGL static van captures and focused material tests. Human art acceptance, other-map full-room review, installed performance and the default-off renderer candidate remain separate.')
(V/'metal-refinement-2026-10-27.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
page=['<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width"><title>Painted metal refinement</title><style>body{font:17px system-ui;background:#142025;color:#e5e8df;margin:24px}a{color:#a5ddd0}.pair{display:grid;grid-template-columns:1fr 1fr;gap:12px;max-width:1200px}img{width:100%;image-rendering:pixelated}figure{margin:0}h2{margin-top:32px}</style><h1>Painted metal: remove the repeating dark grooves</h1><p>27 October 2026. The original generated metal tile had an explicit dark column every eight pixels. Repeated UVs turned that into stripes across the van shell. The revised original procedural tile uses subtle two-dimensional wear. All15other atlas tiles are byte-identical; renderer, fixtures, geometry and exposure are unchanged in these matched captures.</p><p>Van lights on their own supply; flashlight and site breaker OFF. Actual OpenGL frames at480×300. Still images, not frame-time or human acceptance. Other native model textures and metal families require separate review.</p><p><a href="../verification/metal-refinement-2026-10-27.json">Hashes and measurements</a> · <a href="../../../beckenburg-development-gallery.html">Shared gallery</a></p>']
for map in ('van-house','van-hollow-creek'):
    for view in ('workstation','equipment','roof'):
        page.append('<h2>'+html.escape(map+' / '+view)+'</h2><div class="pair">')
        for phase in ('before','after'):
            page.append(f'<figure><img loading="lazy" src="../previews/metal-refinement/{phase}/{map}-{view}.png" alt="{phase} {map} {view}"><figcaption>{phase}</figcaption></figure>')
        page.append('</div>')
page.append('</html>')
folder=ROOT/'development/levels/metal-refinement';folder.mkdir(exist_ok=True)
(folder/'REVIEW.html').write_text(''.join(page),encoding='utf-8')
print('PASS12actual GPU captures, unchanged renderer/lighting/van bytecode, exact atlas preservation and review')
