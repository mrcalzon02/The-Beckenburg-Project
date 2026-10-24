"""Read back the deployed gallery, manifest and every PNG header.
Full download/hash verification covers new worklight and defect evidence.
"""
from pathlib import Path
from urllib.request import Request,urlopen
from urllib.error import HTTPError,URLError
import time
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime,timezone
import hashlib,json,struct,subprocess
ROOT=Path(__file__).resolve().parents[1]
BASE='https://mrcalzon02.github.io/The-Beckenburg-Project/'
request_retries=[]

def read_url(path,full=True):
    for attempt in range(4):
        try:
            with urlopen(Request(BASE+path,headers={'User-Agent':'Beckenburg-gallery-verifier/1.0'}),timeout=45) as response:
                assert response.status==200,path
                return response.read() if full else response.read(32)
        except (HTTPError,URLError,TimeoutError,ConnectionError) as error:
            code=getattr(error,'code',None)
            if code is not None and code not in (429,500,502,503,504):raise RuntimeError(f'Non-retryable gallery response for {path}: {error}') from error
            if attempt==3:raise RuntimeError(f'Gallery response exhausted four attempts for {path}: {error}') from error
            wait=[1,3,7][attempt]
            header=error.headers.get('Retry-After') if isinstance(error,HTTPError) and error.headers else None
            if header:
                try:wait=max(wait,float(header))
                except ValueError:
                    from email.utils import parsedate_to_datetime
                    try:wait=max(wait,(parsedate_to_datetime(header)-datetime.now(timezone.utc)).total_seconds())
                    except (TypeError,ValueError):pass
            # Do not ignore a long server backoff or block this worker indefinitely.
            if wait>30:raise RuntimeError(f'Gallery server requires {wait}s backoff for {path}; defer verification rather than ignoring Retry-After') from error
            request_retries.append(dict(path=path,attempt=attempt+1,status=code,delay_seconds=wait))
            if isinstance(error,HTTPError):error.close()
            time.sleep(wait)

def get(path):return read_url(path)
local=json.loads((ROOT/'development/gallery/index.json').read_text(encoding='utf-8'))
live=json.loads(get('development/gallery/index.json'))
assert live==local,'Deployed gallery manifest is stale'
assert get('beckenburg-development-gallery.html')==(ROOT/'beckenburg-development-gallery.html').read_bytes(),'Deployed gallery HTML differs'
def check(item):
    full=any(s in item['path'] for s in ['/mill-lighting/','/defects/','/hollow-creek/','/optical-evidence/','/terrain-foundations/','/edge-sky-plans/','/blockouts/wld-14/','/blockouts/wld-15/','/blockouts/wld-16/','/blockouts/wld-17/','/blockouts/wld-18/','/blockouts/wld-07-mortuary/','new-location-blockouts.png','new-location-layer-studies.png','level_site_','level_camp_tent_','level_camp_bedroll_rolled.png','level_camp_picnic_table.png','level_camp_fire_ring.png','level_camp_pavilion.png','level_camp_terrain.png','level_camp_pavilion_services.png'])
    data=read_url(item['url'],full)
    assert data[:8]==b'\x89PNG\r\n\x1a\n' and struct.unpack('>II',data[16:24])==(item['width'],item['height']),item['path']
    if full:assert hashlib.sha256(data).hexdigest()==item['sha256'],item['path']
    return dict(path=item['path'],http=200,png_dimensions_match=True,full_sha256_verified=full)
with ThreadPoolExecutor(max_workers=4) as pool:checks=list(pool.map(check,live['images']))
report=dict(status='pass',verified_at=datetime.now(timezone.utc).isoformat(),published_commit=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),gallery=BASE+'beckenburg-development-gallery.html',image_count=len(checks),checks=checks,request_retries=request_retries)
out=ROOT/'development/verification/live-development-gallery.json';out.write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
print('PASS deployed gallery + manifest,',len(checks),'PNG URLs/dimensions;',sum(i['full_sha256_verified'] for i in checks),'full evidence hashes')
