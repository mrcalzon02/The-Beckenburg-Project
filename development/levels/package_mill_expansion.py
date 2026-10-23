"""Package the original mill kit and build a review from verified real images."""
from pathlib import Path
import json,hashlib,zipfile,html
ROOT=Path(__file__).resolve().parents[2];LEVELS=ROOT/'development/levels'
assets=[json.loads(p.read_text()) for p in sorted((LEVELS/'production').glob('*.json'))]
native=json.loads((LEVELS/'verification/blackpine-mill-native.json').read_text())
runtime=json.loads((LEVELS/'verification/blackpine-mill-expansion.json').read_text())
assert native['status']==runtime['status']=='pass'
for asset in assets:
    path=ROOT/f"assets/runtime/models/{asset['id']}.glb"
    assert hashlib.sha256(path.read_bytes()).hexdigest()==asset['sha256']
archive=ROOT/'assets/libraries/industrial/blackpine-mill-refinement.zip'
archive.parent.mkdir(parents=True,exist_ok=True)
with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
    z.writestr('README.txt','Beckenburg original working-place kit, 22 October 2026.\n12 static scenery models. CC0-1.0 original geometry and procedural textures.\nGLBs use metre dimensions, Y up and a floor pivot. Editable Blender sources are Z up.\nCircular headrig and raised log carriage are aligned in the included original assembly.\nNo powered cutting, moving log carriage, inventory or healing simulation.\nThe complete map uses separately licensed scenery and is not included in this CC0 archive.\n')
    z.writestr('LICENSE.txt','CC0 1.0 Universal\nhttps://creativecommons.org/publicdomain/zero/1.0/\nOriginal Beckenburg geometry and procedural textures.\n')
    z.writestr('manifest.json',json.dumps(dict(assets=assets),indent=2))
    for a in assets:
        for path,entry in [(ROOT/f"assets/runtime/models/{a['id']}.glb",f"GLB/{a['id']}.glb"),(ROOT/a['editable'],f"Blender/{a['id']}.blend"),(LEVELS/f"previews/{a['id']}.png",f"Previews/{a['id']}.png")]:z.write(path,entry)
    z.write(LEVELS/'blender/blackpine-logway-assembly.blend','Blender/blackpine-logway-assembly.blend')
with zipfile.ZipFile(archive) as z:assert z.testzip() is None
cards=[]
for name,title,view in [('circular_headrig','Circular sawmill headrig','mill-working-floor'),('dust_extractor','Isolated dust extractor','dust-extractor'),('timber_rack','Supported dry-stock rack','timber-rack'),('safety_station','2009 first-aid station','safety-station'),('bandsaw','Small bandsaw relocated to workshop','workshop-bandsaw')]:
    a=next(a for a in assets if a['id']=='level_work_'+name)
    cards.append(f'<article><h2>{title}</h2><p>{a["triangles"]:,} source triangles · static scenery · packed diffuse</p><div class="pair"><figure><a href="previews/{a["id"]}.png"><img src="previews/{a["id"]}.png" alt="{title} bright native studio view"></a><figcaption>Bright Blender inspection</figcaption></figure><figure><a href="previews/mill-expansion/blackpine-{view}.png"><img src="previews/mill-expansion/blackpine-{view}.png" alt="{title} in the game"></a><figcaption>Actual game renderer / flashlight</figcaption></figure></div><p><a href="../../assets/runtime/models/{a["id"]}.glb">GLB</a> · <a href="blender/{a["id"]}.blend">Editable Blender source</a> · <a href="production/{name}.json">Hash and dimensions</a></p></article>')
page='''<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Blackpine — circular head saw and mill refinement</title><style>
body{max-width:1200px;margin:40px auto;padding:0 24px;background:#151b1b;color:#e5e7df;font:17px/1.6 system-ui}h1,h2{line-height:1.2;color:#e4d6a5}a{color:#b6d7d1}img{width:100%;display:block}.pair{display:grid;grid-template-columns:1fr 1fr;gap:14px}figure{margin:0}figcaption{font-size:14px;color:#bec8c0}article{border-top:1px solid #4b5a52;padding:22px 0}header p{max-width:880px}.status{border-left:4px solid #beaa75;padding:12px 20px;background:#222b29}@media(max-width:720px){.pair{grid-template-columns:1fr}}</style>
<header><p>BECKENBURG / ENVIRONMENT DEVELOPMENT</p><h1>Blackpine: the logway now ends at a circular head saw</h1><p>The two metre circular blade shares the log's vertical cut plane. Raised carriage knees keep the shaft below the stock and the blade outside the cradle bearers. Continuous rails connect the carriage to the headrig. The smaller bandsaw belongs in the repair shed.</p><p>Four new reusable models; twelve original working-place assets refreshed with subtle, non-directional metal patina. The repeating black/gray stripe pattern has been removed from this kit.</p></header>
<a href="previews/mill-expansion/logway-assembly-studio.png"><img src="previews/mill-expansion/logway-assembly-studio.png" alt="Original circular headrig and aligned raised log carriage"></a><p>Native assembly inspection: exact relative placement from the authored map.</p>
<p><a href="../../assets/libraries/industrial/blackpine-mill-refinement.zip">Download 12-model CC0 kit</a> · <a href="blender/blackpine-mill-expanded.blend">Complete editable development scene</a> · <a href="MILL_REFINEMENT.md">Workflow and remaining gates</a></p>
<p class="status">Scoped Java 17 compilation, physical checks, swept van routes and native source checks passed. Development map remains locked: hunts, full LAN map acceptance and target-hardware performance are unfinished. Studio lighting is for inspection.</p>
'''+''.join(cards)+'''<article><h2>Verification</h2><p><a href="verification/blackpine-mill-expansion.json">Java, collision, routes and capture report</a> · <a href="verification/blackpine-mill-native.json">Native geometry, metal pixel and scene checks</a> · <a href="verification/blackpine-mill-package.json">Archive integrity</a></p></article></html>'''
(LEVELS/'BLACKPINE_MILL_REVIEW.html').write_text(page,encoding='utf-8')
result=dict(status='pass',archive=str(archive.relative_to(ROOT)).replace('\\','/'),sha256=hashlib.sha256(archive.read_bytes()).hexdigest(),bytes=archive.stat().st_size,models=len(assets),source_triangles=sum(a['triangles'] for a in assets),zip_crc_verified=True)
(LEVELS/'verification/blackpine-mill-package.json').write_text(json.dumps(result,indent=2)+'\n')
print(json.dumps(result),flush=True)
