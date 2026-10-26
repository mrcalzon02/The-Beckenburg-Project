"""Publish only hash-matched completed actual GPU captures, with explicit scope."""
from pathlib import Path
import csv, hashlib, html, json, subprocess
ROOT=Path(__file__).resolve().parents[1]
VERIFY=ROOT/'development/levels/verification'
PREVIEW=ROOT/'development/levels/previews/lighting-camera-audit'
titles=dict(house='The Briar House',farmhouse='Hollowmere Farm',school='Ravenshade School',mansion='Blackthorn Manor',prison='Beckenburg Penitentiary',asylum='St. Dymphna Hospital',academy='St. Alban Academy',college='Beckenburg College',church='St. Brigid Parish',**{'hollow-creek':'Camp Hollow Creek','castle':'Castle Beckenburg'})
def rows(suffix):
    with (VERIFY/f'map-lighting-camera-audit{suffix}.tsv').open(encoding='utf-8') as f:return list(csv.DictReader(f,delimiter='\t'))
first=rows(''); coverage=rows('-coverage'); native=rows('-native'); castle=rows('-castle')
replaced={r['map'] for r in coverage+native+castle}
final=[r for r in first if r['map'] not in replaced]+coverage+native+castle
seen=set()
for r in final:
    key=(r['map'],r['view'])
    if key in seen:raise ValueError('Duplicate capture '+str(key))
    seen.add(key);p=PREVIEW/f"{r['map']}-{r['view']}.png"
    if hashlib.sha256(p.read_bytes()).hexdigest()!=r['sha256']:raise ValueError('Stale capture '+str(p))
    r['path']=p.relative_to(ROOT).as_posix()
for id in titles:
    expected=['lit','blackout','torch','pan']+([] if id=='castle' else ['opening','approach'])
    if any((id,v) not in seen for v in expected):raise ValueError('Incomplete map '+id)
baseline=[]
for p in sorted((PREVIEW/'before-coverage').glob('*.png')):
    id,view=p.stem.rsplit('-',1)
    baseline.append(dict(map=id,view=view,path=p.relative_to(ROOT).as_posix(),sha256=hashlib.sha256(p.read_bytes()).hexdigest()))
catalog=json.loads((ROOT/'development/levels/layouts/catalog.json').read_text(encoding='utf-8'))
report=dict(date='2026-10-26',engine_baseline='5b4761a8f6d72f6fb824325bb33807b3cc7aff5a',scope='64 scripted actual OpenGL static camera/light samples. Not whole-map, LAN, human or real-time performance acceptance.',captures=final,before_coverage=baseline,planned_layouts=[dict(id=m['id'],title=m['title'],status=m['status'],next='Match native geometry to current runtime/camera/lighting contracts before claiming compatibility') for m in catalog['maps']],remaining=['All-room and route camera pans, door/window open/closed and floor/wall/roof/corner light-leak inspection','Per-map texture/UV/normal and metal stripe audit in current renderer','Native aerial water CPU cost and fog composition; no real-time pass inferred','Hollow Creek full mission, ghost power, LAN and player acceptance','Sound-volume final editor/listening acceptance','Intermittent release LanSoundIntegrationTest; local rerun passed, CI failed244/245'])
(VERIFY/'map-compatibility-2026-10-26.json').write_text(json.dumps(report,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
# Keep the public main TSV synchronized with the currently delivered files.
with (VERIFY/'map-lighting-camera-audit.tsv').open('w',encoding='utf-8',newline='') as f:
    w=csv.DictWriter(f,fieldnames=list(final[0]),delimiter='\t');w.writeheader();w.writerows(final)
parts=['<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width"><title>Map lighting and camera audit</title><style>body{background:#10181a;color:#dce7df;font:16px system-ui;margin:24px}a{color:#9edbd1}section{border-top:1px solid #537065;margin-top:28px}.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(280px,1fr));gap:12px}figure{margin:0;background:#1c292c;padding:8px}img{width:100%;image-rendering:pixelated}figcaption{font-size:14px}p{max-width:1000px}</style><h1>Existing-map lighting and camera compatibility</h1><p>26 October 2026. Actual completed OpenGL frames from the current shared lighting engine, with repaired outdoor opening framing and physical fittings in long prototype halls. Static scripted samples; these do not unlock missions or establish full map fitness, LAN or real-time performance acceptance.</p><p><a href="../../../beckenburg-development-gallery.html">Shared gallery</a> · <a href="../verification/map-compatibility-2026-10-26.json">Measured evidence and remaining gates</a></p><p>House, Farmhouse and School keep their refined fixture layouts. Six prototype buildings receive repeated fittings instead of a single out-of-range central lamp. Outdoor opening framing uses site ownership anchors without inventing rectangular buildings. Hollow Creek native captures use320×200; other captures480×300. Native aerial fog and CPU water cost remain open.</p>']
for id,title in titles.items():
    parts.append('<section><h2>'+html.escape(title)+'</h2><div class="grid">')
    for r in final:
        if r['map']!=id:continue
        name=Path(r['path']).name
        parts.append(f'<figure><a href="../previews/lighting-camera-audit/{name}"><img loading="lazy" src="../previews/lighting-camera-audit/{name}" alt="{html.escape(title)} {r["view"]}"></a><figcaption>{r["view"]} · frame{r["frame"]} · luma{r["mean_luma"]}</figcaption></figure>')
    before=[r for r in baseline if r['map']==id and r['view']=='lit']
    for r in before:parts.append(f'<figure><img loading="lazy" src="../previews/lighting-camera-audit/before-coverage/{id}-lit.png" alt="Before fixture coverage repair"><figcaption>Before coverage repair · lights on</figcaption></figure>')
    parts.append('</div></section>')
parts.append('<section><h2>Next review gates</h2><ul>'+''.join('<li>'+html.escape(x)+'</li>' for x in report['remaining'])+'</ul></section></html>')
folder=ROOT/'development/levels/lighting-camera-audit';folder.mkdir(exist_ok=True)
(folder/'REVIEW.html').write_text(''.join(parts),encoding='utf-8')
print('PASS',len(final),'hash-matched actual GPU captures;',len(baseline),'before-images;',len(catalog['maps']),'existing planned layouts tracked separately')
