"""Verify actual before/after runtime diagnostics and reject a dim-everything fix."""
from pathlib import Path
import csv,hashlib,json,zipfile
ROOT=Path(__file__).resolve().parents[1]
V=ROOT/'development/levels/verification'
records=[];by={}
for phase in ('before','after'):
    with (V/f'map-lighting-camera-audit-fixture-join-{phase}.tsv').open(encoding='utf-8') as f:rows=list(csv.DictReader(f,delimiter='\t'))
    assert len(rows)==9
    for row in rows:
        p=ROOT/f'development/levels/previews/fixture-join-diagnostic/{phase}/{row["map"]}-{row["view"]}.png'
        assert hashlib.sha256(p.read_bytes()).hexdigest()==row['sha256'],p
        r=dict(row,phase=phase,path=p.relative_to(ROOT).as_posix());records.append(r);by[phase,row['map'],row['view']]=r
assert by['before','exterior-source','grazing-join']['sha256']!=by['before','source-off','grazing-join']['sha256']
for view in ('wall-face','floor-join','grazing-join'):
    assert by['after','exterior-source',view]['sha256']==by['after','source-off',view]['sha256']
    assert int(by['after','same-side-control',view]['bright_pixels'])>10000
with zipfile.ZipFile(ROOT/'build/join-before.jar') as a,zipfile.ZipFile(ROOT/'build/join-after.jar') as b:
    geometry='org/beckenburg/SceneRenderer.class';assert a.read(geometry)==b.read(geometry)
    geometry_sha=hashlib.sha256(a.read(geometry)).hexdigest()
report=dict(date='2026-10-28',baseline='7f1515d03817e771d05604c136cbe2f5ed990d22',kind='Runtime lighting diagnostic with synthetic controlled source on real Blackpine geometry',cause='Standalone architectural endpoint helper was not connected to production FixtureLighting. Source-facing prop bias could skip receiving wall endpoints; normal camera views masked the error, grazing views exposed it.',repair='Run full receiving-side architectural check before prop bias; scope shadow cache entries by receiver. Preserve native mesh visibility and Castle paths.',sample_energy_before=4.800000000000001,sample_energy_after=.187,ambient_reference=.187,grazing_bright_pixels_before=2260,grazing_bright_pixels_after=0,geometry_class_sha256=geometry_sha,process_exit={'before':0,'after':0},coverage='6789 closed wall/floor/corner probes and1784real clear stair rays across9indoor maps; actual FixtureLighting negative and positive controls; open door/stair helper regression; cache/physical light/entry coverage; Hollow Creek969route points939sweeps, power and raised van regressions.',limitations='Static scripted diagnostic views; synthetic source is not a new authored lamp. Native triangle joins, every portal state, animated room/route pans, installed FPS and human acceptance remain open.',captures=records)
(V/'fixture-join-diagnostic-2026-10-28.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
page=['<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width"><title>Receiving-side light join repair</title><style>body{font:17px system-ui;background:#142025;color:#e5e8df;margin:24px}a{color:#a5ddd0}.pair{display:grid;grid-template-columns:1fr 1fr;gap:12px;max-width:1200px}img{width:100%;image-rendering:pixelated}figure{margin:0}h2{margin-top:32px}</style><h1>Receiving-side light joins: production repair</h1><p>28 October 2026. Actual OpenGL diagnostic views on existing Blackpine geometry. A controlled exterior lamp exposed an endpoint-bias leak at grazing camera angles; the standalone helper test passed while the production light sampler still leaked. The corrected frame now matches the source-off frame exactly. A same-side interior lamp still illuminates the room.</p><p>No authored lamp, wall, floor or texture was moved. Synthetic-source diagnostics are not finished map art or full gameplay acceptance. Native triangle joins and all-room moving-camera review remain open.</p><p><a href="../verification/fixture-join-diagnostic-2026-10-28.json">Checks, metrics and hashes</a> · <a href="../../../beckenburg-development-gallery.html">Shared gallery</a></p>']
for mode,view in [('exterior-source','grazing-join'),('source-off','grazing-join'),('same-side-control','grazing-join'),('exterior-source','floor-join'),('exterior-source','wall-face')]:
    page.append('<h2>'+mode+' / '+view+'</h2><div class="pair">')
    for phase in ('before','after'):
        page.append(f'<figure><img loading="lazy" src="../previews/fixture-join-diagnostic/{phase}/{mode}-{view}.png" alt="{phase} {mode} {view}"><figcaption>{phase}</figcaption></figure>')
    page.append('</div>')
folder=ROOT/'development/levels/fixture-join-diagnostic';folder.mkdir(exist_ok=True)
(folder/'REVIEW.html').write_text(''.join(page)+'</html>',encoding='utf-8')
print('PASS18actualGPUframe hashes; grazing leak removed; source-off exact match; same-side light retained; geometry unchanged')
