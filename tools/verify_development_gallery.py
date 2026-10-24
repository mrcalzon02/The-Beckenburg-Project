"""Read back the deployed gallery, manifest and every PNG header.
Full download/hash verification covers new worklight and defect evidence.
"""
from pathlib import Path
from urllib.request import Request,urlopen
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime,timezone
import hashlib,json,struct,subprocess
ROOT=Path(__file__).resolve().parents[1]
BASE='https://mrcalzon02.github.io/The-Beckenburg-Project/'
def get(path):
    with urlopen(Request(BASE+path,headers={'User-Agent':'Beckenburg-gallery-verifier/1.0'}),timeout=45) as response:
        assert response.status==200,path
        return response.read()
local=json.loads((ROOT/'development/gallery/index.json').read_text(encoding='utf-8'))
live=json.loads(get('development/gallery/index.json'))
assert live==local,'Deployed gallery manifest is stale'
assert get('beckenburg-development-gallery.html')==(ROOT/'beckenburg-development-gallery.html').read_bytes(),'Deployed gallery HTML differs'
def check(item):
    full=any(s in item['path'] for s in ['/mill-lighting/','/defects/','/hollow-creek/','/optical-evidence/','level_camp_picnic_table.png','level_camp_fire_ring.png','level_camp_pavilion.png','level_camp_terrain.png','level_camp_pavilion_services.png'])
    with urlopen(Request(BASE+item['url'],headers={'User-Agent':'Beckenburg-gallery-verifier/1.0'}),timeout=45) as response:
        assert response.status==200,item['path']
        data=response.read() if full else response.read(32)
    assert data[:8]==b'\x89PNG\r\n\x1a\n' and struct.unpack('>II',data[16:24])==(item['width'],item['height']),item['path']
    if full:assert hashlib.sha256(data).hexdigest()==item['sha256'],item['path']
    return dict(path=item['path'],http=200,png_dimensions_match=True,full_sha256_verified=full)
with ThreadPoolExecutor(max_workers=4) as pool:checks=list(pool.map(check,live['images']))
report=dict(status='pass',verified_at=datetime.now(timezone.utc).isoformat(),published_commit=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),gallery=BASE+'beckenburg-development-gallery.html',image_count=len(checks),checks=checks)
out=ROOT/'development/verification/live-development-gallery.json';out.write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
print('PASS deployed gallery + manifest,',len(checks),'PNG URLs/dimensions;',sum(i['full_sha256_verified'] for i in checks),'full evidence hashes')
